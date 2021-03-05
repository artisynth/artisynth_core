/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/**
 * Implements a three DOF spherical coupling, parameterized by roll, pitch and
 * yaw angles. Frames C and D share a common origin and are otherwise free to
 * rotate about each other. Travel restrictions can be placed on the roll,
 * pitch and yaw angles.
 */
public class GimbalCoupling extends RigidBodyCoupling {

   public static final int ROLL_IDX = 0;
   public static final int PITCH_IDX = 1;
   public static final int YAW_IDX = 2;

   // 1 => use RCD to determine the angles, -1 => use RDC
   protected int myAngleSign = 1;

   public boolean applyEuler = true;

   public GimbalCoupling () {
      super();
   }

   public boolean getUseRDC() {
      return myAngleSign == -1;
   }

   public void setUseRDC (boolean enable) {
      myAngleSign = (enable ? -1 : 1);
   }

   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.R.set(TCD.R);
      TGD.p.setZero();
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }
   }

   private void doGetRpy (double[] rpy, RotationMatrix3d RDC) {

      Vector3d ang1 = new Vector3d();
      Vector3d ang2 = new Vector3d();
      Vector3d ang3 = new Vector3d();

      CoordinateInfo rcoord = myCoordinates.get(0); 
      CoordinateInfo pcoord = myCoordinates.get(1);
      CoordinateInfo ycoord = myCoordinates.get(2);

      ang1.x = rcoord.value; // roll
      ang1.y = pcoord.value; // pitch
      ang1.z = ycoord.value; // yaw
      
      double[] rpyTrimmed = new double[3];
      rpyTrimmed[0] = rcoord.clipToRange (ang1.x);
      rpyTrimmed[1] = pcoord.clipToRange (ang1.y);
      rpyTrimmed[2] = ycoord.clipToRange (ang1.z);

      
      RDC.getRpy(rpy);

      ang2.set (rpy);

      // adjust so that all angles as close as possible to mid-range
      if (applyEuler) {
         // adjust so that all angles as close as possible to original angles
         EulerFilter.filter(rpyTrimmed, rpy, 1e-2, rpy);
         //       EulerFilter.filter(midRange, rpy, EPSILON, rpy);
      } else {
         rpy[0] = findNearestAngle (ang1.x, rpy[0]);
         rpy[1] = findNearestAngle (ang1.y, rpy[1]);
         rpy[2] = findNearestAngle (ang1.z, rpy[2]);
      }
      
      if (Math.abs(rpy[0]-ang1.x) > Math.PI/2 ) {
         System.out.println (
            "SphericalRpyCoupling: roll more that PI/2 from previous value");
      }
      ang3.set (rpy);

      Vector3d diff = new Vector3d();
      diff.sub (ang3, ang1);
      if (diff.norm() > Math.PI/4) {
         ang1.scale (RTOD);
         System.out.println ("deg1=" + ang1.toString ("%10.5f"));
         ang2.scale (RTOD);
         System.out.println ("deg2=" + ang2.toString ("%10.5f"));
         ang3.scale (RTOD);
         System.out.println ("deg3=" + ang3.toString ("%10.5f"));
         System.out.println ("");
      }
   }

   public void initializeConstraints() {
      addConstraint (BILATERAL | LINEAR, new Wrench(1, 0, 0, 0, 0, 0));
      addConstraint (BILATERAL | LINEAR, new Wrench(0, 1, 0, 0, 0, 0));
      addConstraint (BILATERAL | LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
      addConstraint (ROTARY);
      addConstraint (ROTARY);
      addConstraint (ROTARY);

      addCoordinate (-Math.PI, Math.PI, 0, getConstraint(3));
      addCoordinate (-Math.PI/2, Math.PI/2, 0, getConstraint(4));
      addCoordinate (-Math.PI, Math.PI, 0, getConstraint(5));
   }

   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      CoordinateInfo rcoord = myCoordinates.get(ROLL_IDX);
      CoordinateInfo pcoord = myCoordinates.get(PITCH_IDX);
      CoordinateInfo ycoord = myCoordinates.get(YAW_IDX);

      // constraints for enforcing limits:
      RigidBodyConstraint rcons = rcoord.limitConstraint;
      RigidBodyConstraint pcons = pcoord.limitConstraint;
      RigidBodyConstraint ycons = ycoord.limitConstraint;
      
      // only need to do anything if one or more constraints are engaged:
      if (rcons.engaged != 0 || pcons.engaged != 0 || ycons.engaged != 0) {

         Vector3d wDC = new Vector3d(); // FINISH: angular vel D wrt C, in C
         
         double roll = rcoord.value;
         double pitch = pcoord.value;

         double cr = Math.cos(roll);
         double sr = Math.sin(roll);
         double cp = Math.cos(pitch);
         double sp = Math.sin(pitch);

         double denom = cp;
         // keep the derivative from getting too large near
         // the singularity at cp = 0
         if (Math.abs(denom) < 0.0001) {
            denom = (denom >= 0 ? 0.0001 : -0.0001);
         }
         double tp = sp / denom;
         
         Vector3d wvel = new Vector3d(); // angular velocity in angle frame
         if (getUseRDC()) {
            wvel.set (wDC);
         }
         else {
            wvel.negate (wDC);
            wvel.transform (TGD.R);
         }
         double dotp = -sr * wvel.x + cr * wvel.y;
         double doty = (cr * wvel.x + sr * wvel.y) / denom;
         double dotr = wvel.z + sp * doty;

         // update roll limit constraint if necessary
         if (rcons.engaged != 0) {
            rcons.wrenchG.set (0, 0, 0, sp*cr/denom, sp*sr/denom, 1);
            rcons.dotWrenchG.f.setZero();
            double tt = (1 + tp * tp);
            rcons.dotWrenchG.m.set(
               (-sr*tp*dotr+tt*cr*dotp), (cr*tp*dotr+tt*sr*dotp), 0);
            // checkDeriv ("roll", rcons, conR);
            if (getUseRDC()) {
               // negate wrenches
               rcons.wrenchG.negate();
               rcons.dotWrenchG.negate();
            }
            else {
               // transform from D to C
               transformDtoG (rcons.wrenchG.m, rcons.dotWrenchG.m, TGD.R, velGD.w);
            }
         }
         // update pitch limit constraint if necessary
         if (pcons.engaged != 0) {
            pcons.wrenchG.set(0, 0, 0, -sr, cr, 0);
            pcons.dotWrenchG.f.setZero();
            pcons.dotWrenchG.m.set(-cr*dotr, -sr*dotr, 0);
            // checkDeriv ("pitch", pcons, conP);
            if (getUseRDC()) {
               // negate wrenches
               pcons.wrenchG.negate();
               pcons.dotWrenchG.negate();
            }
            else {
               // transform from D to C
               transformDtoG (pcons.wrenchG.m, pcons.dotWrenchG.m, TGD.R, velGD.w);
            }
         }
         // update yaw limit constraint if necessary
         if (ycons.engaged != 0) {
            ycons.wrenchG.set(0, 0, 0, cr/denom, sr/denom, 0);
            ycons.dotWrenchG.f.setZero();
            ycons.dotWrenchG.m.set(
               (-sr*dotr+cr*tp*dotp)/denom, (cr*dotr+sr*tp*dotp)/denom, 0);
            // checkDeriv ("yaw", ycons, conY);
            if (getUseRDC()) {
               // negate wrenches
               ycons.wrenchG.negate();
               ycons.dotWrenchG.negate();
            }
            else {
               // transform from D to C
               transformDtoG (ycons.wrenchG.m, ycons.dotWrenchG.m, TGD.R, velGD.w);
            }
         }
      }
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      double[] rpy = new double[3];
      RotationMatrix3d R = new RotationMatrix3d();
      if (getUseRDC()) {
         R.transpose(TCD.R);
      }
      else {
         R.set(TCD.R);
      }
      doGetRpy(rpy, R);
      coords.set(ROLL_IDX, rpy[0]);
      coords.set(PITCH_IDX, rpy[1]);
      coords.set(YAW_IDX, rpy[2]);
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double roll, double pitch, double yaw) {

      TCD.setIdentity();
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRpy (roll, pitch, yaw);
      if (getUseRDC()) {
         TCD.R.transpose(R);
      }
      else {
         TCD.R.set(R);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1), coords.get(2));
   }

}
