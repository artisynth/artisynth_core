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
import maspack.util.*;

/**
 * Implements a three DOF spherical coupling, parameterized by roll, pitch and
 * yaw angles. These angles describe a sequence of intrinsic rotations about
 * either the Z-Y-X or the X-Y-Z axes, depending on the axis settings as
 * specified by {@link #setAxes} and {@link #getAxes}. Frames C and D share a
 * common origin but are otherwise free to rotate about each other. Travel
 * restrictions can be placed on the roll, pitch and yaw angles.
 */
public class GimbalCoupling extends RigidBodyCoupling {

   public static final int ROLL_IDX = 0;
   public static final int PITCH_IDX = 1;
   public static final int YAW_IDX = 2;

   public enum AxisSet {
      ZYX,
      XYZ;

      /**
       * Same as valueOf but returns null instead of throwing an exception if
       * the value is unrecognized.
       */
      static public AxisSet fromString (String str) {
         try {
            return valueOf(str);
         }
         catch (Exception e) {
            return null;
         }
      }
   }

   // 1 => use RCD to determine the angles, -1 => use RDC
   protected int myAngleSign = 1;
   protected AxisSet myAxes = AxisSet.ZYX;

   private boolean myApplyEuler = true;

   static double EPS = 2e-15;

   public GimbalCoupling () {
      super();
   }

   public GimbalCoupling (AxisSet axes) {
      super();
      myAxes = axes;
   }

   public boolean getUseRDC() {
      return myAngleSign == -1;
   }

   public void setUseRDC (boolean enable) {
      myAngleSign = (enable ? -1 : 1);
   }

   public AxisSet getAxes() {
      return myAxes;
   }

   public void setAxes (AxisSet axes) {
      myAxes = axes;
   }

   public boolean getApplyEuler() {
      return myApplyEuler;
   }

   public void setApplyEuler (boolean enable) {
      myApplyEuler = enable;
   }

   /**
    * Set roll-pitch-yaw angles.
    */
   static void setRpy (
      RotationMatrix3d R, double roll, double pitch, double yaw, AxisSet axes) {

      double sr = Math.sin (roll);
      double cr = Math.cos (roll);
      double sp = Math.sin (pitch);
      double cp = Math.cos (pitch);
      double sy = Math.sin (yaw);
      double cy = Math.cos (yaw);

      switch (axes) {
         case XYZ: {
            R.m00 = cp*cy;
            R.m01 = -cp*sy;
            R.m02 = sp;

            R.m10 = cr*sy + cy*sp*sr;
            R.m11 = cr*cy - sp*sr*sy;
            R.m12 = -cp*sr;

            R.m20 = sr*sy - cr*cy*sp;
            R.m21 = cy*sr + cr*sp*sy;
            R.m22 = cp*cr;
            break;
         }
         case ZYX: {
            R.m00 = cr*cp;
            R.m10 = sr*cp;
            R.m20 = -sp;
            
            R.m01 = cr*sp*sy - sr*cy;
            R.m11 = sr*sp*sy + cr*cy;
            R.m21 = cp*sy;
            
            R.m02 = cr*sp*cy + sr*sy;
            R.m12 = sr*sp*cy - cr*sy;
            R.m22 = cp*cy;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented axis set " + axes);
         }
      }
   }

   /**
    * Compute the X, Y, Z based roll-pitch-yaw angles given a rotation matrix.
    */
   static void getRpy (double[] angs, RotationMatrix3d R, AxisSet axes) {

      double sr, cr, r;

      switch (axes) {
         case XYZ: {
            if (Math.abs(R.m12) < EPS && Math.abs(R.m22) < EPS) {
               angs[0] = 0;
               angs[1] = Math.atan2 (R.m02, R.m22);
               angs[2] = Math.atan2 (R.m10, R.m11);
            }
            else {
               angs[0] = (r = Math.atan2 (-R.m12, R.m22));
               sr = Math.sin (r);
               cr = Math.cos (r);
               angs[1] = Math.atan2 (R.m02, -sr*R.m12 + cr*R.m22);
               angs[2] = Math.atan2 (cr*R.m10 + sr*R.m20, cr*R.m11 + sr*R.m21);
            }
            break;
         }
         case ZYX: {
            if (Math.abs (R.m00) < EPS && Math.abs (R.m10) < EPS) {
               angs[0] = 0.;
               angs[1] = Math.atan2 (-R.m20, R.m00);
               angs[2] = Math.atan2 (-R.m12, R.m11);
            }
            else {
               angs[0] = (r = Math.atan2 (R.m10, R.m00));
               sr = Math.sin (r);
               cr = Math.cos (r);
               angs[1] = Math.atan2 (-R.m20, cr*R.m00 + sr*R.m10);
               angs[2] = Math.atan2 (sr*R.m02 - cr*R.m12, cr*R.m11 - sr*R.m01);
            }
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented axis set " + axes);
         }
      }   
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

   static void doGetRpy (
      double[] rpy, RotationMatrix3d R,
      CoordinateInfo[] coordInfo, AxisSet axes, boolean applyEuler) {

      Vector3d ang1 = new Vector3d();
      Vector3d ang2 = new Vector3d();
      Vector3d ang3 = new Vector3d();

      CoordinateInfo rcoord = coordInfo[0];
      CoordinateInfo pcoord = coordInfo[1];
      CoordinateInfo ycoord = coordInfo[2];

      ang1.x = rcoord.getValue(); // roll
      ang1.y = pcoord.getValue(); // pitch
      ang1.z = ycoord.getValue(); // yaw
      
      double[] rpyTrimmed = new double[3];
      rpyTrimmed[0] = rcoord.clipToRange (ang1.x);
      rpyTrimmed[1] = pcoord.clipToRange (ang1.y);
      rpyTrimmed[2] = ycoord.clipToRange (ang1.z);

      getRpy (rpy, R, axes);

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

      addCoordinate ("roll", -Math.PI, Math.PI, 0, getConstraint(3));
      addCoordinate ("pitch", -Math.PI/2, Math.PI/2, 0, getConstraint(4));
      addCoordinate ("yaw", -Math.PI, Math.PI, 0, getConstraint(5));
   }

   static void updateRpyLimitConstraints (
      RigidBodyCoupling coupling, RigidTransform3d TGD, Twist velGD, 
      CoordinateInfo[] coordInfo, AxisSet axes, boolean useRDC) {

      // assume roll, pitch, yaw coordinate info is stored at indices 0, 1, 2

      // constraints for enforcing limits:
      RigidBodyConstraint rcons = coordInfo[0].limitConstraint;
      RigidBodyConstraint pcons = coordInfo[1].limitConstraint;
      RigidBodyConstraint ycons = coordInfo[2].limitConstraint;
      
      Vector3d wDC = new Vector3d(); // FINISH: angular vel D wrt C, in C
         
      double roll = coordInfo[0].getValue();
      double pitch = coordInfo[1].getValue();

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
      if (useRDC) {
         wvel.set (wDC);
      }
      else {
         wvel.negate (wDC);
         wvel.transform (TGD.R);
      }
      double dotp, doty, dotr;
      if (axes == AxisSet.XYZ) {
         dotp = sr * wvel.z + cr * wvel.y;
         doty = (cr * wvel.z - sr * wvel.y) / denom;
         dotr = wvel.x - sp * doty;
      }
      else {
         dotp = -sr * wvel.x + cr * wvel.y;
         doty = (cr * wvel.x + sr * wvel.y) / denom;
         dotr = wvel.z + sp * doty;
      }

      Twist tw = new Twist();
      // update roll limit constraint and twist
      double tt = (1 + tp * tp);
      if (axes == AxisSet.XYZ) {
         rcons.wrenchG.set (0, 0, 0, 1, sp*sr/denom, -sp*cr/denom);
         rcons.dotWrenchG.m.set(
            0, (cr*tp*dotr+tt*sr*dotp), (sr*tp*dotr-tt*cr*dotp));
         tw.w.set (1, 0, 0);
      }
      else {
         rcons.wrenchG.set (0, 0, 0, sp*cr/denom, sp*sr/denom, 1);
         rcons.dotWrenchG.m.set(
            (-sr*tp*dotr+tt*cr*dotp), (cr*tp*dotr+tt*sr*dotp), 0);
         tw.w.set (0, 0, 1);
      }
      rcons.dotWrenchG.f.setZero();
      if (useRDC) {
         // negate wrench and twist
         rcons.wrenchG.negate();
         rcons.dotWrenchG.negate();
         tw.negate();
      }
      else {
         // transform from D to C
         coupling.transformDtoG (
            rcons.wrenchG.m, rcons.dotWrenchG.m, TGD.R, velGD.w);
         tw.inverseTransform (TGD.R);
      }
      coordInfo[0].setTwistG (tw);

      // update pitch limit constraint and twist
      if (axes == AxisSet.XYZ) {
         pcons.wrenchG.set(0, 0, 0, 0, cr, sr);
         pcons.dotWrenchG.m.set(0, -sr*dotr, cr*dotr);
         tw.w.set (0, cr, sr);            
      }
      else {
         pcons.wrenchG.set(0, 0, 0, -sr, cr, 0);
         pcons.dotWrenchG.m.set(-cr*dotr, -sr*dotr, 0);
         tw.w.set (-sr, cr, 0);
      }
      pcons.dotWrenchG.f.setZero();
      if (useRDC) {
         // negate wrench and twist
         pcons.wrenchG.negate();
         pcons.dotWrenchG.negate();
         tw.negate();
      }
      else {
         // transform from D to C
         coupling.transformDtoG (
            pcons.wrenchG.m, pcons.dotWrenchG.m, TGD.R, velGD.w);
         tw.inverseTransform (TGD.R);
      }
      coordInfo[1].setTwistG (tw);

      // update yaw limit constraint and twist
      if (axes == AxisSet.XYZ) {
         ycons.wrenchG.set(0, 0, 0, 0, -sr/denom, cr/denom);
         ycons.dotWrenchG.m.set(
            0, -(cr*dotr+sr*tp*dotp)/denom, (-sr*dotr+cr*tp*dotp)/denom);
         tw.w.set (sp, -cp*sr, cp*cr);            
      }
      else {
         ycons.wrenchG.set(0, 0, 0, cr/denom, sr/denom, 0);
         ycons.dotWrenchG.m.set(
            (-sr*dotr+cr*tp*dotp)/denom, (cr*dotr+sr*tp*dotp)/denom, 0);
         tw.w.set (cr*cp, sr*cp, -sp);
      }
      ycons.dotWrenchG.f.setZero();
      if (useRDC) {
         // negate wrench and twist
         ycons.wrenchG.negate();
         ycons.dotWrenchG.negate();
         tw.negate();
      }
      else {
         // transform from D to C
         coupling.transformDtoG (
            ycons.wrenchG.m, ycons.dotWrenchG.m, TGD.R, velGD.w);
         tw.inverseTransform (TGD.R);
      }
      coordInfo[2].setTwistG (tw);
   }

   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      CoordinateInfo[] coordInfo = new CoordinateInfo[] {
         myCoordinates.get(0), myCoordinates.get(1), myCoordinates.get(2) };

      // packaged into a static method so that other classes (like
      // FreeCoupling) can use it too      
      updateRpyLimitConstraints (
         this, TGD, velGD, coordInfo, myAxes, getUseRDC());
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
      CoordinateInfo[] coordInfo = new CoordinateInfo[] {
         myCoordinates.get(0), myCoordinates.get(1), myCoordinates.get(2) };
      doGetRpy (rpy, R, coordInfo, myAxes, myApplyEuler);
      coords.set(ROLL_IDX, rpy[0]);
      coords.set(PITCH_IDX, rpy[1]);
      coords.set(YAW_IDX, rpy[2]);
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double roll, double pitch, double yaw) {

      TCD.p.setZero();
      RotationMatrix3d R = new RotationMatrix3d();
      setRpy (R, roll, pitch, yaw, myAxes);
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
