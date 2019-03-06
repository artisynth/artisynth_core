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
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;

/**
 * Implements a three DOF spherical coupling. Frames C and D share a common
 * origin and are otherwise free to rotate about each other. Travel restrictions
 * can be placed on this coupling by bounding either the maximum tilt of the z
 * axis, or the maximum total rotation. Bounds on the maximum total rotation can
 * be weighted by the rotation direction, so that the maximum rotations about
 * the x, y, and z axes are different.
 */
public class SphericalCoupling extends RigidBodyCoupling {

   private double MAX_DISTANCE = Math.toRadians(50); // prevent unstable velocity

   public boolean applyEuler = true;
   private double myMaxTilt = Math.PI;

   private double myMaxRotX = Math.PI;
   private double myMaxRotY = Math.PI;
   private double myMaxRotZ = Math.PI;

   private double myMaxRoll = Math.PI;
   private double myMaxPitch = Math.PI / 2;
   private double myMaxYaw = Math.PI;

   private double myMinRoll = -Math.PI;
   private double myMinPitch = -Math.PI / 2;
   private double myMinYaw = -Math.PI;

   public static final int TILT_LIMIT = 0x01;
   public static final int ROTATION_LIMIT = 0x02;
   public static final int RPY_LIMIT = 0x04;

   // public enum RangeType {
   // None,
   // Tilt,
   // Rotation,
   // RollPitchYaw,
   // };

   private int myRangeType = 0;

   public int getRangeType() {
      return myRangeType;
   }

   public void setRangeType(int type) {
      if (type == TILT_LIMIT ||
         type == ROTATION_LIMIT ||
         type == RPY_LIMIT ||
         type == 0) {
         myRangeType = type;
      }
      else {
         throw new IllegalArgumentException(
            "Illegal range type " + NumberFormat.formatHex(type));
      }
   }

   private double clip(double value, double min, double max) {
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
    * Sets the maximum total rotation for this coupling. This will only have an
    * effect if the current range type is {@link #ROTATION_LIMIT}. The maximum
    * must be greater than 0, and will be clipped to the range (0, PI]. Setting
    * the maximum to PI will remove the range restriction.
    * 
    * @param max
    * maximum total rotation
    */
   public void setMaximumRotation(double max) {
      setMaximumRotation(max, max, max);
   }

   /**
    * Sets the maximum total rotation about the x, y, and z axes. This will only
    * have an effect if the current range type is {@link #ROTATION_LIMIT}. For a
    * general orientation, the maximum rotation angle will be computed from the
    * length of the rotation axis weighted by the maximum values along each
    * principal axis.
    * 
    * <p>
    * Each maximum must be greater than 0, and will be clipped to the range (0,
    * PI]. Setting all maximums to PI will remove the range restriction.
    * 
    * @param maxx
    * maximum total rotation about x
    * @param maxy
    * maximum total rotation about y
    * @param maxz
    * maximum total rotation about z
    */
   public void setMaximumRotation(double maxx, double maxy, double maxz) {
      if (maxx <= 0 || maxy <= 0 || maxz <= 0) {
         throw new IllegalArgumentException(
            "maximum rotations must be positive");
      }
      myMaxRotX = clip(maxx, 0, Math.PI);
      myMaxRotY = clip(maxy, 0, Math.PI);
      myMaxRotZ = clip(maxz, 0, Math.PI);
   }

   /**
    * Returns the maximum total rotation for each axis.
    * 
    * @param maxRot
    * returns the maximum rotation for each axis
    */
   public void getMaximumRotation(double[] maxRot) {
      maxRot[0] = myMaxRotX;
      maxRot[1] = myMaxRotY;
      maxRot[2] = myMaxRotZ;
   }

   /**
    * Returns the maximum total rotation for this coupling. If separate maximum
    * rotations have been specified for each axis, then the maximum rotation for
    * the x axis is returned.
    * 
    * @return the maximum rotation for this coupling, or for the x axis if
    * separate limits have been specified for each axis
    */
   public double getMaximumRotation() {
      return myMaxRotX;
   }

   /**
    * Sets the minimum and maximum values for the coupling's roll angle (in
    * radians). These only have an effect if the current range type is
    * {@link #RPY_LIMIT}.
    * 
    * @param min
    * Minimum roll angle
    * @param max
    * Maximum roll angle
    */
   public void setRollRange(double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException("min exceeds max");
      }
      myMinRoll = min; // clip(min, -Math.PI, Math.PI);
      myMaxRoll = max; // clip(max, -Math.PI, Math.PI);
   }

   /**
    * Gets the minimum and maximum values for the coupling's roll angle (in
    * radians).
    * 
    * @param minmax
    * used to return the minimum and maximum values
    * @see #setRollRange
    */
   public void getRollRange(double[] minmax) {
      minmax[0] = myMinRoll;
      minmax[1] = myMaxRoll;
   }

   /**
    * Sets the minimum and maximum values for the coupling's pitch angle (in
    * radians). These values only have an effect if the current range type is
    * {@link #RPY_LIMIT}.
    * 
    * @param min
    * Minimum pitch angle
    * @param max
    * Maximum pitch angle
    */
   public void setPitchRange(double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException("min exceeeds max");
      }
      myMinPitch = min; // clip(min, -Math.PI / 2, Math.PI / 2);
      myMaxPitch = max; // clip(max, -Math.PI / 2, Math.PI / 2);
   }

   /**
    * Gets the minimum and maximum values for the coupling's pitch angle (in
    * radians).
    * 
    * @param minmax
    * used to return the minimum and maximum values
    * @see #setPitchRange
    */
   public void getPitchRange(double[] minmax) {
      minmax[0] = myMinPitch;
      minmax[1] = myMaxPitch;
   }

   /**
    * Sets the minimum and maximum values for the coupling's yaw angle (in
    * radians). These only have an effect if the current range type is
    * {@link #RPY_LIMIT}.
    * 
    * @param min
    * Minimum yaw angle
    * @param max
    * Maximum yaw angle
    */
   public void setYawRange(double min, double max) {
      if (min > max) {
         throw new IllegalArgumentException("min exceeeds max");
      }
      myMinYaw = min; // clip(min, -Math.PI, Math.PI);
      myMaxYaw = max; // clip(max, -Math.PI, Math.PI);
   }

   /**
    * Gets the minimum and maximum values for the coupling's yaw angle (in
    * radians).
    * 
    * @param minmax
    * used to return the minimum and maximum values
    * @see #setYawRange
    */
   public void getYawRange(double[] minmax) {
      minmax[0] = myMinYaw;
      minmax[1] = myMaxYaw;
   }

   /**
    * Sets the maximum tilt for this coupling. The tilt is the angle between the
    * initial and current z axis. This will only have an effect if the current
    * range type is {@link #TILT_LIMIT}. The maximum must be greater than 0, and
    * will be clipped to the range (0, PI].
    * 
    * @param max
    * maximum value for theta
    */
   public void setMaximumTilt(double max) {
      if (max <= 0) {
         throw new IllegalArgumentException("maximum must be greater than 0");
      }
      myMaxTilt = clip(max, 0, Math.PI);
   }

   /**
    * Returns the maximum value of theta for this revolute coupling.
    * 
    * @return maximum value for theta
    */
   public double getMaximumTilt() {
      return myMaxTilt;
   }

   public SphericalCoupling () {
      super();
   }

//   public SphericalCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXDB(XDB);
//      setXFA(TCA);
//   }

   @Override
   public int maxUnilaterals() {
      return 3;
   }

   @Override
   public int numBilaterals() {
      return 3;
   }

   @Override
   public void projectToConstraint(RigidTransform3d TGD, RigidTransform3d TCD) {
      TGD.R.set(TCD.R);
      TGD.p.setZero();
   }

   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   private void doGetRpy(double[] rpy, RotationMatrix3d RDC) {

      checkConstraintStorage();

      Vector3d deg1 = new Vector3d();
      Vector3d deg2 = new Vector3d();
      Vector3d deg3 = new Vector3d();

      deg1.x = myConstraintInfo[3].coordinate;
      deg1.y = myConstraintInfo[4].coordinate;
      deg1.z = myConstraintInfo[5].coordinate;
      deg1.scale (180/Math.PI);
      
      double[] midRange = { (myMinRoll + myMaxRoll) / 2,
                           (myMinPitch + myMaxPitch) / 2,
                           (myMinYaw + myMaxYaw) / 2 };
      double[] rpyTrimmed = new double[3];
      rpyTrimmed[0] = clip(myConstraintInfo[3].coordinate, myMinRoll,myMaxRoll);
      rpyTrimmed[1] = clip(myConstraintInfo[4].coordinate, myMinPitch,myMaxPitch);
      rpyTrimmed[2] = clip(myConstraintInfo[5].coordinate, myMinYaw,myMaxYaw);
      
      RDC.getRpy(rpy);

      deg2.set (rpy);
      deg2.scale (180/Math.PI);

      // adjust so that all angles as close as possible to mid-range
      if (applyEuler) {
         // adjust so that all angles as close as possible to original angles
         EulerFilter.filter(rpyTrimmed, rpy, 1e-2, rpy);
         //       EulerFilter.filter(midRange, rpy, EPSILON, rpy);
      } else {
         rpy[0] = findNearestAngle(myConstraintInfo[3].coordinate, rpy[0]);
         rpy[1] = findNearestAngle(myConstraintInfo[4].coordinate, rpy[1]);
         rpy[2] = findNearestAngle(myConstraintInfo[5].coordinate, rpy[2]);
      }
      
      if( Math.abs(rpy[0]-myConstraintInfo[3].coordinate) > Math.PI/2 ) {
         System.out.println("busted");
      }

      myConstraintInfo[5].coordinate = rpy[2];
      myConstraintInfo[4].coordinate = rpy[1];
      myConstraintInfo[3].coordinate = rpy[0];

      deg3.set (rpy);
      deg3.scale (180/Math.PI);

      Vector3d diff = new Vector3d();
      diff.sub (deg3, deg1);
      if (diff.norm() > 45) {
         System.out.println ("deg1=" + deg1.toString ("%10.5f"));
         System.out.println ("deg2=" + deg2.toString ("%10.5f"));
         System.out.println ("deg3=" + deg3.toString ("%10.5f"));
         System.out.println ("");
      }
      


   }

   public void getRpy (Vector3d angs, RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectToConstraint(TGD, TGD);
         double[] rpy = new double[3];
         RotationMatrix3d RDC = new RotationMatrix3d();
         RDC.transpose(TGD.R);
         doGetRpy(rpy, RDC);
         angs.x = rpy[2];
         angs.y = rpy[1];
         angs.z = rpy[0];
      }
      else {
         // no TGD. Simply read back the coordinate settings
         checkConstraintStorage();
         angs.x = myConstraintInfo[5].coordinate;
         angs.y = myConstraintInfo[4].coordinate;
         angs.z = myConstraintInfo[3].coordinate;
      }
   }

   public void setRpy(RigidTransform3d TGD, Vector3d angs) {
      if (TGD != null) {
         TGD.setIdentity();
         RotationMatrix3d RDC = new RotationMatrix3d();
         RDC.setRpy(angs.z, angs.y, angs.x);
         TGD.R.transpose(RDC);
      }
      checkConstraintStorage();
      myConstraintInfo[5].coordinate = angs.x;
      myConstraintInfo[4].coordinate = angs.y;
      myConstraintInfo[3].coordinate = angs.z;
   }

   // Wrench conR = new Wrench();
   // Wrench conP = new Wrench();
   // Wrench conY = new Wrench();
   // int cnt = 0;

   // private void checkDeriv (String msg, ConstraintInfo info, Wrench lastC) {
   // if (cnt > 4 && ((cnt-5)%3 == 0)) {
   // System.out.println ("deriv check for " + msg);
   // Wrench diffC = new Wrench();
   // Wrench estC = new Wrench();
   // diffC.sub (info.wrenchC, lastC);
   // estC.scale (0.01, info.dotWrenchC);
   // System.out.println (" diff: " + diffC.toString ("%10.7f"));
   // System.out.println (" est:  " + estC.toString ("%10.7f"));
   // lastC.set (info.wrenchC);
   // }
   // }

   public void initializeConstraintInfo(ConstraintInfo[] info) {
      info[0].flags = (BILATERAL | LINEAR);
      info[1].flags = (BILATERAL | LINEAR);
      info[2].flags = (BILATERAL | LINEAR);
      info[3].flags = (ROTARY);
      info[4].flags = (ROTARY);
      info[5].flags = (ROTARY);

      info[0].wrenchC.set(1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set(0, 1, 0, 0, 0, 0);
      info[2].wrenchC.set(0, 0, 1, 0, 0, 0);
   }

   @Override
   public void getConstraintInfo(
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {

      //projectToConstraint(TGD, TCD);

      // info[0].bilateral = true;
      // info[1].bilateral = true;
      // info[2].bilateral = true;
      // info[3].bilateral = false;
      // info[4].bilateral = false;
      // info[5].bilateral = false;

      //myXFC.mulInverseLeft(TGD, TCD);
      myErr.set(XERR);
      setDistancesAndZeroDerivatives(info, 3, myErr);

      if (myRangeType == TILT_LIMIT) {
         Vector3d utilt = new Vector3d();

         // Tilt axis is is z(C) x z(D).
         // In C coordinates, z(C) = (0,0,1) and z(D) = last row of TGD.R.
         // In D coordinates, z(D) = (0,0,1) and z(C) = last col of TGD.R.

         utilt.set(TGD.R.m12, -TGD.R.m02, 0); // D coordinates
         // utilt.set (-TGD.R.m21, TGD.R.m20, 0); // in C coordinates
         double ulen = utilt.norm();
         double theta = 0;
         if (ulen > 1e-8) {
            theta = Math.atan2(ulen, TGD.R.m22);
            utilt.scale(1 / ulen);
         }
         if (setEngaged) {
            if (theta > myMaxTilt) {
               info[3].engaged = 1;
            }
         }
         if (info[3].engaged != 0) {
            info[3].distance = myMaxTilt - theta;
            utilt.inverseTransform(TGD.R);
            info[3].wrenchC.set(0, 0, 0, utilt.x, utilt.y, utilt.z);
            info[3].dotWrenchC.setZero();
         }
      }
      else if (myRangeType == ROTATION_LIMIT) {
         Vector3d u = new Vector3d();
         double ang = TGD.R.getAxisAngle(u);
         u.normalize(); // paranoid
         Vector3d a =
            new Vector3d(u.x * myMaxRotX, u.y * myMaxRotY, u.z * myMaxRotZ);

         double maxAng = a.norm();
         if (setEngaged) {
            if (ang > 0 && ang > maxAng) {
               info[3].engaged = 1;
            }
         }
         if (info[3].engaged != 0) {
            info[3].distance = maxAng - ang;
            u.x /= myMaxRotX;
            u.y /= myMaxRotY;
            u.z /= myMaxRotZ;
            u.normalize();
            info[3].wrenchC.set(0, 0, 0, -u.x, -u.y, -u.z);
            info[3].dotWrenchC.setZero(); // TODO set this
         }
      }
      else if (myRangeType == RPY_LIMIT) {
         double roll, pitch, yaw;
         double[] rpy = new double[3];
         RotationMatrix3d RDC = new RotationMatrix3d();
         Vector3d wBA = new Vector3d();
         RDC.transpose(TGD.R);
         doGetRpy(rpy, RDC);

         roll = rpy[0];
         pitch = rpy[1];
         yaw = rpy[2];

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
         
         // Don't need to transform because vel is now in Frame C
         // get angular velocity of B with respect to A in frame C
//         if (!myComputeVelInFrameC) {
//            wBA.transform(RDC, myVelBA.w);
//         }

         info[3].distance = 0;
         info[4].distance = 0;
         info[5].distance = 0;

         double dotp = -sr * wBA.x + cr * wBA.y;
         double doty = (cr * wBA.x + sr * wBA.y) / denom;
         double dotr = wBA.z + sp * doty;

         if (setEngaged) {
            maybeSetEngaged(info[3], roll, myMinRoll, myMaxRoll);
            maybeSetEngaged(info[4], pitch, myMinPitch, myMaxPitch);
            maybeSetEngaged(info[5], yaw, myMinYaw, myMaxYaw);
         }
         if (info[3].engaged != 0) {
            info[3].distance = getDistance(roll, myMinRoll, myMaxRoll);
            info[3].wrenchC.set(0, 0, 0, sp * cr / denom, sp * sr / denom, 1);
            info[3].dotWrenchC.f.setZero();
            double tt = (1 + tp * tp);
            info[3].dotWrenchC.m.set(
               -sr*tp*dotr + tt*cr*dotp, cr*tp*dotr + tt*sr*dotp, 0);
            // checkDeriv ("roll", info[3], conR);
            if (info[3].engaged == -1) {
               info[3].wrenchC.negate();
               info[3].dotWrenchC.negate();
            }
         }
         if (info[4].engaged != 0) {
            info[4].distance = getDistance(pitch, myMinPitch, myMaxPitch);
            info[4].wrenchC.set(0, 0, 0, -sr, cr, 0);
            info[4].dotWrenchC.f.setZero();
            info[4].dotWrenchC.m.set(-cr * dotr, -sr * dotr, 0);
            // checkDeriv ("pitch", info[4], conP);
            if (info[4].engaged == -1) {
               info[4].wrenchC.negate();
               info[4].dotWrenchC.negate();
            }
         }
         if (info[5].engaged != 0) {
            info[5].distance = getDistance(yaw, myMinYaw, myMaxYaw);
            info[5].wrenchC.set(0, 0, 0, cr / denom, sr / denom, 0);
            info[5].dotWrenchC.f.setZero();
            info[5].dotWrenchC.m.set(
               (-sr*dotr+cr*tp*dotp)/denom, (cr*dotr+sr*tp*dotp)/denom, 0);
            // checkDeriv ("yaw", info[5], conY);
            if (info[5].engaged == -1) {
               info[5].wrenchC.negate();
               info[5].dotWrenchC.negate();
            }
         }
         // System.out.println ("xxx:");
         // for (int i=3; i<6; i++) {
         //    System.out.println (
         //       info[i].engaged+" "+Math.toDegrees(info[i].distance));
         // }
      }
      else if (myRangeType != 0) {
         throw new InternalErrorException(
            "Unimplemented range limits " + NumberFormat.formatHex(myRangeType));
      }
   }

}
