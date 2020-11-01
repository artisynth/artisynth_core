/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.*;

/**
 * Implements a two DOF roll-pitch coupling. Frames C and D share a common
 * origin, and the transform from C to D is given by a <code>roll</code>
 * rotation about the z axis, followed by a <code>pitch</code> rotation about
 * the subsequent <code>pitch</code> axis. The pitch axis is usually the
 * post-roll y-axis, but can be adjusted via the skewAngle to lie along some
 * other direction in the post-roll z/y plane. The angle between the roll and
 * pitch axes is given by PI/2 - skewAngle.
 */
public class RollPitchCoupling extends RigidBodyCoupling {

   private double myMaxRoll = Math.PI;
   private double myMaxPitch = Math.PI; // Math.PI/2

   private double myMinRoll = -Math.PI;
   private double myMinPitch = -Math.PI;  // Math.PI/2

   // skew angle attributes
   private double mySkewAngle = 0;   
   private double mySa = 0; // sin of the skew angle
   private double myCa = 1; // cos of the skew angle
   // skew angle rotation matrix, or null if the skew angle = 0
   private RotationMatrix3d myRa = null;

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
         throw new IllegalArgumentException ("min exceeds max");
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

   public RollPitchCoupling(double skewAngle) {
      super();
      if (skewAngle <= -Math.PI || skewAngle >= Math.PI) {
         throw new IllegalArgumentException (
            "Skew angle must lie within the open interval (-PI,PI)");
      }
      mySa = Math.sin(skewAngle);
      myCa = Math.cos(skewAngle);
      if (skewAngle != 0) {
         myRa = new RotationMatrix3d();
         myRa.setRotX (skewAngle);
      }
      mySkewAngle = skewAngle;
   }

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
   void getRollPitch (double[] angs, RotationMatrix3d R) {
      if (mySkewAngle == 0) {
         // no skew
         angs[0] = Math.atan2 (-R.m01, R.m11);
         angs[1] = Math.atan2 (-R.m20, R.m22);
      }
      else {
         // assumes |myCa| is not too small ...
         angs[1] = Math.atan2 (-R.m20, (R.m22-mySa*mySa)/myCa);
         double sp = Math.sin(angs[1]);
         double cp = Math.cos(angs[1]);
         double vp = 1-cp;
         angs[0] = Math.atan2 (
            -R.m01*(vp*myCa*myCa + cp) - R.m00*mySa*sp - myCa*R.m02*mySa*vp,
            cp*R.m00 + myCa*R.m02*sp - R.m01*mySa*sp);
      }
   }

   void setRollPitch (RotationMatrix3d R, double roll, double pitch) {
      double sr, sp, cr, cp;

      sr = Math.sin (roll);
      cr = Math.cos (roll);
      sp = Math.sin (pitch);
      cp = Math.cos (pitch);

      if (mySkewAngle == 0) { 
         // no skew
         R.m00 = cr*cp;
         R.m10 = sr*cp;
         R.m20 = -sp;

         R.m01 = -sr;
         R.m11 = cr;
         R.m21 = 0;

         R.m02 = cr*sp;
         R.m12 = sr*sp;
         R.m22 = cp;         
      }
      else {
         double vp = 1 - cp;

         R.m00 = cp*cr - mySa*sp*sr;
         R.m10 = cp*sr + mySa*cr*sp;
         R.m20 = -myCa*sp;

         double vcp = vp*myCa*myCa + cp;

         R.m01 = -sr*vcp - mySa*cr*sp;
         R.m11 = cr*vcp - mySa*sp*sr;
         R.m21 = myCa*mySa*vp;

         R.m02 = myCa*(cr*sp - mySa*sr*vp);
         R.m12 = myCa*(sr*sp + mySa*cr*vp);
         R.m22 = vp*mySa*mySa + cp;
      }      
   }

   // FINISH
   @Override
   public void projectToConstraint (RigidTransform3d TGD, RigidTransform3d TCD) {

      // In the non-skew case, projection is done on the matrix
      //
      // R = RDC
      //
      // while for the skew case, it is done with respect to
      //
      // R = RDC * inv(Ra)
      //
      // where Ra is a rotation about the x axis by skewAngle.

      RotationMatrix3d R = new RotationMatrix3d();
      R.transpose (TCD.R); // RDC is the transpose of RCD
      if (myRa != null) {
         R.mul (myRa);
      }
      

      // Now enusre that the y axis (2nd column) of R forms an angle 'skewAngle' with
      // respect to the x/y plane. If it does not, apply a rotation to bring it
      // there.

      double yx = R.m01;
      double yy = R.m11;
      double yz = R.m21;

      double r = Math.sqrt (yx*yx + yy*yy); // length of projection into x/y plane
      RotationMatrix3d RX = new RotationMatrix3d(); // rotation to adjust y
      if (r == 0) {
         // unlikely to happen. Just rotate about x by PI/2-skewAngle
         RX.setRotX (Math.PI/2-mySkewAngle);
      }
      else {
         double ang = Math.atan2 (yz, r);
         Vector3d axis = new Vector3d (yy, -yx, 0);
         RX.setAxisAngle (axis, mySkewAngle-ang);
      }
      R.mul (RX, R);
      // now transform back: RGD = (R Ra)^T
      if (myRa != null) {
         R.mulInverseRight (R, myRa);
      }
      TGD.R.transpose (R);

      // project translation
      TGD.p.setZero();
   }

   private void doGetRollPitch (double[] angs, RotationMatrix3d RDC) {
      getRollPitch (angs, RDC);

      checkConstraintStorage();
      angs[0] = findNearestAngle (myConstraintInfo[5].coordinate, angs[0]);
      angs[1] = findNearestAngle (myConstraintInfo[4].coordinate, angs[1]);
      myConstraintInfo[5].coordinate = angs[0];
      myConstraintInfo[4].coordinate = angs[1];
   }      

   public void getRollPitch (double[] angs, RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectToConstraint (TGD, TGD);
         RotationMatrix3d RDC = new RotationMatrix3d();
         RDC.transpose (TGD.R);
         doGetRollPitch (angs, RDC);
      }
      else {
         // simply read back the coordinate settings
         checkConstraintStorage();
         angs[0] = myConstraintInfo[5].coordinate;
         angs[1] = myConstraintInfo[4].coordinate;
      }
   }

   public void setRollPitch (RigidTransform3d TGD, double[] angs) {
      if (TGD != null) {
         TGD.setIdentity();
         RotationMatrix3d RDC = new RotationMatrix3d();
         setRollPitch (RDC, angs[0], angs[1]);
         TGD.R.transpose (RDC);
      }

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

   // FINISH
   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {
      //projectToConstraint (TGD, TCD);

      //myXFC.mulInverseLeft (TGD, TCD);
      myErr.set (XERR);
      setDistancesAndZeroDerivatives (info, 4, myErr);

      RotationMatrix3d RDC = new RotationMatrix3d();
      double[] angs = new double[2];
      RDC.transpose (TGD.R);
      doGetRollPitch (angs, RDC);

      double roll = angs[0];
      double pitch = angs[1];

      double cr = Math.cos(roll);
      double sr = Math.sin(roll);
      double cp = Math.cos(pitch);
      double sp = Math.sin(pitch);

      // Don't need to transform because vel is now in Frame C
      //      // get angular velocity of B with respect to A in frame C
      //      if (!myComputeVelInFrameC) {
      //         wBA.transform (RDC, myVelBA.w);
      //      }
      Vector3d wBA = new Vector3d();//myVelBA.w;

      info[4].distance = 0;
      info[5].distance = 0;

      // XXX check:
      double dotp = -sr*wBA.x + cr*wBA.y;
      double dotr = wBA.z;

      info[3].wrenchC.set (0, 0, 0, cr, sr, 0);
      info[3].dotWrenchC.f.setZero();
      info[3].dotWrenchC.m.set (-sr*dotr, cr*dotr, 0);

      if (hasRestrictedRange()) {
         if (setEngaged) {
            maybeSetEngaged (info[4], pitch, myMinPitch, myMaxPitch);
            maybeSetEngaged (info[5], roll, myMinRoll, myMaxRoll);
         }
         if (info[4].engaged != 0) {
            // pitch joint constrained
            info[4].distance = getDistance (pitch, myMinPitch, myMaxPitch);
            info[4].wrenchC.set (0, 0, 0, -sr, cr, 0);
            //info[4].wrenchC.set (0, 0, 0, -myCa*sr, myCa*cr, mySa);
            info[4].dotWrenchC.f.setZero();
            info[4].dotWrenchC.m.set (-myCa*cr*dotr, -myCa*sr*dotr, 0);
            if (info[4].engaged == -1) {
               info[4].wrenchC.negate();
               info[4].dotWrenchC.negate();
            }
         }
         if (info[5].engaged != 0) {
            // roll joint constrained 
            info[5].distance = getDistance (roll, myMinRoll, myMaxRoll);
            info[5].wrenchC.set (0, 0, 0, mySa*sr, -mySa*cr, myCa);
            info[5].dotWrenchC.m.set (mySa*cr, mySa*sr, 0);
            info[5].dotWrenchC.f.setZero();
            if (info[5].engaged == -1) {
               info[5].wrenchC.negate();
               info[5].dotWrenchC.negate();
            }
         }
      }
   }
}

