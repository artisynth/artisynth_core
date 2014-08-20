/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.*;
import maspack.util.*;

import java.util.ArrayList;

/**
 * Implements a two DOF roll-pitch coupling. Frames C and D share a common
 * origin, and the transform from C to D is given by a <code>roll</code>
 * rotation about the z axis, followed by a <code>pitch</code> rotation about
 * the subsequent <code>y</code> axis.  Range limits can be placed on both the
 * roll and pitch angles.
 */
public class RollPitchCoupling extends RigidBodyCoupling {

   private double myMaxRoll = Math.PI;
   private double myMaxPitch = Math.PI/2;

   private double myMinRoll = -Math.PI;
   private double myMinPitch = -Math.PI/2;


   private double clip (double value, double min, double max) {
      if (value < min) {
         return min;
      }
      else if (value > max) {
         return max;
      }
      else {
         return value;
      }
   }

   /** 
    * Sets the minimum and maximum values for the coupling's roll angle (in
    * radians).  These values are clipped to the range [-PI,PI].
    * 
    * @param min Minimum roll angle
    * @param max Maximum roll angle
    */
   public void setRollRange (double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException ("min exceeeds max");
      }
      myMinRoll = clip (min, -Math.PI, Math.PI);
      myMaxRoll = clip (max, -Math.PI, Math.PI);
   }

   /** 
    * Gets the minimum and maximum values for the coupling's roll angle (in
    * radians).
    * 
    * @param minmax used to return the minimum and maximum values
    * @see #setRollRange
    */
   public void getRollRange (double[] minmax) {
      minmax[0] = myMinRoll;
      minmax[1] = myMaxRoll;
   }

   /** 
    * Sets the minimum and maximum values for the coupling's pitch angle (in
    * radians).  These values are clipped to the range [-PI,PI].
    * 
    * @param min Minimum pitch angle
    * @param max Maximum pitch angle
    */
   public void setPitchRange (double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException ("min exceeeds max");
      }
      myMinPitch = clip (min, -Math.PI, Math.PI);
      myMaxPitch = clip (max, -Math.PI, Math.PI);
   }

   /** 
    * Gets the minimum and maximum values for the coupling's pitch angle (in
    * radians).
    * 
    * @param minmax used to return the minimum and maximum values
    * @see #setPitchRange
    */
   public void getPitchRange (double[] minmax) {
      minmax[0] = myMinPitch;
      minmax[1] = myMaxPitch;
   }

   /**
    * Returns true if this coupling has a range restriction; this will be true
    * if the range of the roll or pitch angles is less than [-PI, PI].
    * 
    * @return true if this coupling has a range restriction.
    */
   public boolean hasRestrictedRange() {
      return (myMinRoll != Double.NEGATIVE_INFINITY ||
              myMaxRoll != Double.POSITIVE_INFINITY ||
              myMinPitch != Double.NEGATIVE_INFINITY ||
              myMaxPitch != Double.POSITIVE_INFINITY);
   }

   public RollPitchCoupling() {
      super();
   }

//   public RollPitchCoupling (RigidTransform3d XFA, RigidTransform3d XDB) {
//      this();
//      setXDB (XDB);
//      setXFA (XFA);
//   }

   @Override
   public int maxUnilaterals() {
      return 2;
   }

   @Override
   public int numBilaterals() {
      return 4;
   }

   /**
    * Gets the roll-pitch angles for this rotation.
    * 
    * @param angs
    * returns the angles (roll, pitch, and yaw, in that order) in radians.
    * @param R
    * rotation for which angles should be obtained
    * @see #setRollPitch(RotationMatrix3d,double,double)
    */
   private void getRollPitch (double[] angs, RotationMatrix3d R) {

      angs[0] = Math.atan2 (-R.m01, R.m11);
      angs[1] = Math.atan2 (-R.m20, R.m22);
   }

   private void setRollPitch (RotationMatrix3d R, double roll, double pitch) {
      double sroll, spitch, croll, cpitch;

      sroll = Math.sin (roll);
      croll = Math.cos (roll);
      spitch = Math.sin (pitch);
      cpitch = Math.cos (pitch);

      R.m00 = croll*cpitch;
      R.m10 = sroll*cpitch;
      R.m20 = -spitch;

      R.m01 = -sroll;
      R.m11 = croll;
      R.m21 = 0;

      R.m02 = croll*spitch;
      R.m12 = sroll*spitch;
      R.m22 = cpitch;

      double[] angs = new double[2];
      getRollPitch (angs, R);

   }

   @Override
   public void projectToConstraint (RigidTransform3d XCD, RigidTransform3d XFD) {
      XCD.R.set (XFD.R);
      // apply a Givens rotation to 0 the m21 entry of XCD.R. This
      // means that we apply a rotation about the x axis (in R coordinates)
      // to remove any residual "yaw" angle.

      double a = XCD.R.m22;
      double b = XCD.R.m12;
      double s, c;
      if (b == 0) {
         c = 1; s = 0;
      }
      else {
         if (Math.abs(b) > Math.abs(a)) {
            double tau = -a/b;
            s = 1/Math.sqrt(1+tau*tau);
            c = s*tau;
         }
         else {
            double tau = -b/a;
            c = 1/Math.sqrt(1+tau*tau);
            s = c*tau;
         }
      }
      RotationMatrix3d RX =
         new RotationMatrix3d (1, 0, 0,  0, c, -s,  0, s, c);
      XCD.R.mulInverseLeft (RX, XCD.R);
      XCD.p.setZero();

   }

   private void doGetRollPitch (double[] angs, RotationMatrix3d RDC) {
      getRollPitch (angs, RDC);

      checkConstraintStorage();
      angs[0] = findNearestAngle (myConstraintInfo[5].coordinate, angs[0]);
      angs[1] = findNearestAngle (myConstraintInfo[4].coordinate, angs[1]);
      myConstraintInfo[5].coordinate = angs[0];
      myConstraintInfo[4].coordinate = angs[1];
   }      

   public void getRollPitch (double[] angs, RigidTransform3d XCD) {
      
      // on entry, XCD is set to XFD. It is then projected to XCD
      projectToConstraint (XCD, XCD);
      RotationMatrix3d RDC = new RotationMatrix3d();
      RDC.transpose (XCD.R);
      doGetRollPitch (angs, RDC);
   }

   public void setRollPitch (RigidTransform3d XCD, double[] angs) {
      XCD.setIdentity();
      RotationMatrix3d RDC = new RotationMatrix3d();
      setRollPitch (RDC, angs[0], angs[1]);
      XCD.R.transpose (RDC);

      checkConstraintStorage();
      myConstraintInfo[5].coordinate = angs[0];
      myConstraintInfo[4].coordinate = angs[1];
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|LINEAR);
      info[2].flags = (BILATERAL|LINEAR);
      info[3].flags = (BILATERAL|ROTARY);
      info[4].flags = (ROTARY);
      info[5].flags = (ROTARY);

      info[0].wrenchC.set (1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set (0, 1, 0, 0, 0, 0);
      info[2].wrenchC.set (0, 0, 1, 0, 0, 0);
      info[3].wrenchC.set (0, 0, 0, 1, 0, 0);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d XCD, RigidTransform3d XFD,
      RigidTransform3d XERR, boolean setEngaged) {
      //projectToConstraint (XCD, XFD);

      //myXFC.mulInverseLeft (XCD, XFD);
      myErr.set (XERR);
      setDistancesAndZeroDerivatives (info, 4, myErr);

      RotationMatrix3d RDC = new RotationMatrix3d();
      Vector3d wBA = new Vector3d();
      double[] angs = new double[2];
      RDC.transpose (XCD.R);
      doGetRollPitch (angs, RDC);

      double roll = angs[0];
      double pitch = angs[1];

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
      double tp = sp/denom;
      
      // Don't need to transform because vel is now in Frame C
//      // get angular velocity of B with respect to A in frame C
//      if (!myComputeVelInFrameC) {
//         wBA.transform (RDC, myVelBA.w);
//      }

      info[4].distance = 0;
      info[5].distance = 0;

      // XXX check:
      double dotp = -sr*wBA.x + cr*wBA.y;
      double dotr = wBA.z;

      // //set a wrench to constraint rotation about the current x
      // //axis of RDC
      // info[3].wrenchC.set (0, 0, 0, RDC.m00, RDC.m10, RDC.m20);
      // info[3].dotWrenchC.f.setZero();
      // info[3].dotWrenchC.m.set (0, 0, 0); // XXX finish

      info[3].wrenchC.set (0, 0, 0, cr/denom, sr/denom, 0);
      info[3].dotWrenchC.f.setZero();
      info[3].dotWrenchC.m.set (
         (-sr*dotr+cr*tp*dotp)/denom, (cr*dotr+sr*tp*dotp)/denom, 0);

      if (hasRestrictedRange()) {
         if (setEngaged) {
            maybeSetEngaged (info[4], pitch, myMinPitch, myMaxPitch);
            maybeSetEngaged (info[5], roll, myMinRoll, myMaxRoll);
         }
         if (info[4].engaged != 0) {
            info[4].distance = getDistance (pitch, myMinPitch, myMaxPitch);
            info[4].wrenchC.set (0, 0, 0, -sr, cr, 0);
            info[4].dotWrenchC.f.setZero();
            // XXX finish dot setting
            info[4].dotWrenchC.m.set (-cr*dotr, -sr*dotr, 0);
            //checkDeriv ("pitch", info[4], conP);
            if (info[4].engaged == -1) {
               info[4].wrenchC.negate();
               info[4].dotWrenchC.negate();
            }
         }
         if (info[5].engaged != 0) {
            info[5].distance = getDistance (roll, myMinRoll, myMaxRoll);
            info[5].wrenchC.set (0, 0, 0, sp*cr/denom, sp*sr/denom, 1);
            info[5].dotWrenchC.f.setZero();
            // XXX finish dot setting
            double tt = (1+tp*tp);
            info[5].dotWrenchC.m.set (
               -sr*tp*dotr + tt*cr*dotp, cr*tp*dotr + tt*sr*dotp, 0);
            //checkDeriv ("roll", info[5], conR);
            if (info[5].engaged == -1) {
               info[5].wrenchC.negate();
               info[5].dotWrenchC.negate();
            }
         }
      }
   }
}
