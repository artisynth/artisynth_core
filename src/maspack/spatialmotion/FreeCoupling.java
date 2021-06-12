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
import maspack.spatialmotion.GimbalCoupling.AxisSet;

/**
 * Implements a six DOF coupling that allows complete motion in space, but with
 * translational and rotational limits. The motion is parameterized by six
 * coordinates in the form of three translations along and three intrinsic
 * rotations about either the Z-Y-X or the X-Y-Z axes, depending on the axis
 * settings as specified by {@link #setAxes} and {@link #getAxes}. The
 * implementation of the rotation coordinates and their limitsis the same as
 * for {@link GimbalCoupling}.
 */
public class FreeCoupling extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;
   public static final int Z_IDX = 2;

   public static final int ROLL_IDX = 3;
   public static final int PITCH_IDX = 4;
   public static final int YAW_IDX = 5;

   protected AxisSet myAxes = AxisSet.ZYX;
   private boolean myApplyEuler = true;

   static double EPS = 2e-15;

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

   public FreeCoupling () {
      super();
   }

   public FreeCoupling (AxisSet axes) {
      super();
      setAxes (axes);
   }

   @Override
   public void projectToConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.set(TCD);
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }
   }

   public void initializeConstraints() {
      addConstraint (LINEAR);
      addConstraint (LINEAR);
      addConstraint (LINEAR);
      addConstraint (ROTARY);
      addConstraint (ROTARY);
      addConstraint (ROTARY);

      addCoordinate (-INF, INF, 0, getConstraint(0));
      addCoordinate (-INF, INF, 0, getConstraint(1));
      addCoordinate (-INF, INF, 0, getConstraint(2));

      addCoordinate (-INF, INF, 0, getConstraint(3));
      addCoordinate (-INF, INF, 0, getConstraint(4));
      addCoordinate (-INF, INF, 0, getConstraint(5));
   }

   @Override
   public void updateConstraints(
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      // constraints for enforcing limits:

      RigidBodyConstraint xcons = myCoordinates.get(X_IDX).limitConstraint;
      RigidBodyConstraint ycons = myCoordinates.get(Y_IDX).limitConstraint;
      RigidBodyConstraint zcons = myCoordinates.get(Z_IDX).limitConstraint;

      if (xcons.engaged != 0) {
         xcons.wrenchG.set (1, 0, 0, 0, 0, 0);
         xcons.dotWrenchG.setZero();
         transformDtoG (
            xcons.wrenchG.f, xcons.dotWrenchG.f, TGD.R, velGD.w);         
      }
      if (ycons.engaged != 0) {
         ycons.wrenchG.set (0, 1, 0, 0, 0, 0);
         ycons.dotWrenchG.setZero();
         transformDtoG (
            ycons.wrenchG.f, ycons.dotWrenchG.f, TGD.R, velGD.w);         
      }
      if (zcons.engaged != 0) {
         zcons.wrenchG.set (0, 0, 1, 0, 0, 0);
         zcons.dotWrenchG.setZero();
         transformDtoG (
            zcons.wrenchG.f, zcons.dotWrenchG.f, TGD.R, velGD.w);         
      }

      // for rotational limits, use the code from GimbalCoupling
      CoordinateInfo[] coordInfo = new CoordinateInfo[] {
         myCoordinates.get(ROLL_IDX),
         myCoordinates.get(PITCH_IDX),
         myCoordinates.get(YAW_IDX) 
      };

      // packaged into a static method so that other classes (like
      // FreeCoupling) can use it too      
      GimbalCoupling.updateRpyLimitConstraints (
         this, TGD, velGD, coordInfo, myAxes, false);

      // // see if rotational constraints are engaged:
      // if (rcons.engaged != 0 || pcons.engaged != 0 || wcons.engaged != 0) {

      //    Vector3d wDC = new Vector3d(); // FINISH: angular vel D wrt C, in C
         
      //    double roll = myCoordinates.get(ROLL_IDX).getValue();
      //    double pitch = myCoordinates.get(PITCH_IDX).getValue();

      //    double cr = Math.cos(roll);
      //    double sr = Math.sin(roll);
      //    double cp = Math.cos(pitch);
      //    double sp = Math.sin(pitch);

      //    double denom = cp;
      //    // keep the derivative from getting too large near
      //    // the singularity at cp = 0
      //    if (Math.abs(denom) < 0.0001) {
      //       denom = (denom >= 0 ? 0.0001 : -0.0001);
      //    }
      //    double tp = sp / denom;
         
      //    Vector3d wvel = new Vector3d(); // angular velocity in angle frame
      //    wvel.negate (wDC);
      //    wvel.transform (TGD.R);

      //    double dotp = sr * wvel.z + cr * wvel.y;
      //    double doty = (cr * wvel.z - sr * wvel.y) / denom;
      //    double dotr = wvel.x - sp * doty;

      //    // update roll limit constraint if necessary
      //    if (rcons.engaged != 0) {
      //       rcons.wrenchG.set (0, 0, 0, 1, sp*sr/denom, -sp*cr/denom);
      //       rcons.dotWrenchG.f.setZero();
      //       double tt = (1 + tp * tp);
      //       rcons.dotWrenchG.m.set(
      //          0, (cr*tp*dotr+tt*sr*dotp), (sr*tp*dotr-tt*cr*dotp));
      //       // checkDeriv ("roll", rcons, conR);
      //       // transform from D to C
      //       transformDtoG (
      //          rcons.wrenchG.m, rcons.dotWrenchG.m, TGD.R, velGD.w);
      //    }
      //    // update pitch limit constraint if necessary
      //    if (pcons.engaged != 0) {
      //       pcons.wrenchG.set(0, 0, 0, 0, cr, sr);
      //       pcons.dotWrenchG.f.setZero();
      //       pcons.dotWrenchG.m.set(0, -sr*dotr, cr*dotr);
      //       // transform from D to C
      //       transformDtoG (
      //          pcons.wrenchG.m, pcons.dotWrenchG.m, TGD.R, velGD.w);
      //    }
      //    // update yaw limit constraint if necessary
      //    if (wcons.engaged != 0) {
      //       wcons.wrenchG.set(0, 0, 0, 0, -sr/denom, cr/denom);
      //       wcons.dotWrenchG.f.setZero();
      //       wcons.dotWrenchG.m.set(
      //          0, -(cr*dotr+sr*tp*dotp)/denom, (-sr*dotr+cr*tp*dotp)/denom);
      //       // transform from D to C
      //       transformDtoG (
      //          wcons.wrenchG.m, wcons.dotWrenchG.m, TGD.R, velGD.w);
      //    }
      // }
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {

      // translation
      coords.set (X_IDX, TCD.p.x);
      coords.set (Y_IDX, TCD.p.y);
      coords.set (Z_IDX, TCD.p.z);

      // rotation
      CoordinateInfo[] coordInfo = new CoordinateInfo[] {
         myCoordinates.get(3), myCoordinates.get(4), myCoordinates.get(5) };
      double[] rpy = new double[3];
      GimbalCoupling.doGetRpy (rpy, TCD.R, coordInfo, myAxes, myApplyEuler);
      coords.set(ROLL_IDX, rpy[0]);
      coords.set(PITCH_IDX, rpy[1]);
      coords.set(YAW_IDX, rpy[2]);
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      TCD.p.set (coords.get(0), coords.get(1), coords.get(2));
      GimbalCoupling.setRpy (
         TCD.R, coords.get(3), coords.get(4), coords.get(5), myAxes);
   }

}
