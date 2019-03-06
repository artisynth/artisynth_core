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
 * Implements a one DOF revolute coupling. Frames C and D share a common origin
 * and rotate about a common z axis. The distance from the x axis of C to the x
 * axis of D, about the z axis, is described by the angle <i>theta</i>. Range
 * limits can be placed on theta.
 */
public class SlottedRevoluteCoupling extends RigidBodyCoupling {
   private double myMinTheta = -Math.PI;
   private double myMaxTheta = Math.PI;
   private double myMaxX = 1;
   private double myMinX = -1;

   /**
    * Sets the maximum value of theta for this revolute coupling. The default
    * value is PI.
    * 
    * @param max
    * maximum value for theta
    */
   public void setMaximumTheta (double max) {
      myMaxTheta = max;
   }

   /**
    * Returns the maximum value of theta for this coupling.
    * 
    * @return maximum value for theta
    */
   public double getMaximumTheta() {
      return myMaxTheta;
   }

   /**
    * Sets the minimum value of theta for this coupling. The default
    * value is -PI.
    * 
    * @param min
    * minimum value for theta
    */
   public void setMinimumTheta (double min) {
      myMinTheta = min;
   }

   /**
    * Returns the minimum value of theta for this coupling.
    * 
    * @return minimum value for theta
    */
   public double getMinimumTheta() {
      return myMinTheta;
   }

   /**
    * Sets the maximum value of x for this coupling. The default
    * value is 1.
    * 
    * @param max
    * maximum value for theta
    */
   public void setMaximumX (double max) {
      myMaxX = max;
   }

   /**
    * Returns the maximum value of x for this coupling.
    * 
    * @return maximum value for x
    */
   public double getMaximumX() {
      return myMaxX;
   }

   /**
    * Sets the minimum value of x for this coupling. The default
    * value is -1.
    * 
    * @param min
    * minimum value for x
    */
   public void setMinimumX (double min) {
      myMinX = min;
   }

   /**
    * Returns the minimum value of x for this coupling.
    * 
    * @return minimum value for x
    */
   public double getMinimumX() {
      return myMinX;
   }

   /**
    * Returns true if this coupling has a range restriction; this will be true
    * if the range of theta is less than [-PI, PI].
    * 
    * @return true if this coupling has a range restriction.
    */
   public boolean hasRestrictedRange() {
      return (myMinTheta != Double.NEGATIVE_INFINITY ||
              myMaxTheta != Double.POSITIVE_INFINITY ||
              myMinX != Double.NEGATIVE_INFINITY ||
              myMaxX != Double.POSITIVE_INFINITY);
   }

   public SlottedRevoluteCoupling() {
      super();
   }

//   public SlottedRevoluteCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXDB (XDB);
//      setXFA (TCA);
//   }

   @Override
   public int maxUnilaterals() {
      return 2;
   }

   @Override
   public int numBilaterals() {
      return 4;
   }

   @Override
   public void projectToConstraint (RigidTransform3d TGD, RigidTransform3d TCD) {
      TGD.R.set (TCD.R);
      TGD.R.rotateZDirection (Vector3d.Z_UNIT);
      TGD.p.x = TCD.p.x;
      TGD.p.y = 0;
      TGD.p.z = 0;
   }

   private double doGetTheta (RigidTransform3d TGD) {
      checkConstraintStorage();
      double theta = Math.atan2 (TGD.R.m01, TGD.R.m00);
      theta = findNearestAngle (myConstraintInfo[5].coordinate, theta);
      myConstraintInfo[5].coordinate = theta;
      return theta;
   }

   public double getTheta (RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectToConstraint (TGD, TGD);
         return doGetTheta (TGD);
      }
      else {
         // simply read back coordinate settings
         checkConstraintStorage();
         return myConstraintInfo[5].coordinate;
      }     
   }

   public void setTheta (
      RigidTransform3d TGD, double theta) {
      checkConstraintStorage();
      if (TGD != null) {
         TGD.setIdentity();
         double c = Math.cos (theta);
         double s = Math.sin (theta);
         TGD.R.m00 = c;
         TGD.R.m01 = s;
         TGD.R.m10 = -s;
         TGD.R.m11 = c;
      }
      myConstraintInfo[5].coordinate = theta;
   }

   private double doGetX (RigidTransform3d TGD) {
      checkConstraintStorage();
      double x = TGD.p.x;
      myConstraintInfo[4].coordinate = x;
      return x;
   }

   public double getX (RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectToConstraint (TGD, TGD);
         return doGetX (TGD);
      }
      else {
         // simply read back coordinate settings
         checkConstraintStorage();
         return myConstraintInfo[4].coordinate;
      }
   }

   public void setX (
      RigidTransform3d TGD, double x) {
      checkConstraintStorage();
      if (TGD != null) {
         TGD.p.set (x, 0, 0);
      }
      myConstraintInfo[4].coordinate = x;
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|LINEAR);
      info[2].flags = (BILATERAL|ROTARY);
      info[3].flags = (BILATERAL|ROTARY);
      info[4].flags = (LINEAR);
      info[5].flags = (ROTARY);


      info[0].wrenchC.set (0, 1, 0, 0, 0, 0);
      info[1].wrenchC.set (0, 0, 1, 0, 0, 0);
      info[2].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[3].wrenchC.set (0, 0, 0, 0, 1, 0);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {
      //projectToConstraint (TGD, TCD);

      //myXFC.mulInverseLeft (TGD, TCD);
      myErr.set (XERR);

      // wrench forces are along z and y in D, transformed to C, and so are
      // given by the last 2 rows of TGD.R
      info[0].wrenchC.set (TGD.R.m10, TGD.R.m11, TGD.R.m12, 0, 0, 0);
      info[0].wrenchC.m.setZero();
      double d = info[0].wrenchC.dot (myErr);

      info[0].distance = d;
      info[0].coordinate = d;
      info[0].dotWrenchC.setZero(); // FINISH!

      // wrench forces are along z and y in D, transformed to C, and so are
      // given by the last 2 rows of TGD.R
      info[1].wrenchC.set (TGD.R.m20, TGD.R.m21, TGD.R.m22, 0, 0, 0);
      d = info[1].wrenchC.dot (myErr);

      info[1].distance = d;
      info[1].coordinate = d;
      info[1].dotWrenchC.setZero(); // FINISH!

      setDistanceAndZeroDerivative (info[2], myErr);
      setDistanceAndZeroDerivative (info[3], myErr);

      //NumberFormat fmt = new NumberFormat ("%11.6f");
      double theta = doGetTheta(TGD);
      double x = doGetX(TGD);
      if (hasRestrictedRange()) {
         if (setEngaged) {
            maybeSetEngaged (info[4], x, myMinX, myMaxX);
            maybeSetEngaged (info[5], theta, myMinTheta, myMaxTheta);
         }

         if (info[4].engaged != 0) {
            info[4].distance = getDistance (x, myMinX, myMaxX);
            info[4].dotWrenchC.setZero();
            if (info[4].engaged == 1) {
               info[4].wrenchC.set (-TGD.R.m00, -TGD.R.m01, -TGD.R.m02, 0, 0, 0);
            }
            else if (info[4].engaged == -1) {
               info[4].wrenchC.set (TGD.R.m00, TGD.R.m01, TGD.R.m02, 0, 0, 0);
            }
         }

         if (info[5].engaged != 0) {
            info[5].distance = getDistance (theta, myMinTheta, myMaxTheta);
            info[5].dotWrenchC.setZero();
            if (info[5].engaged == 1) {
               info[5].wrenchC.set (0, 0, 0, 0, 0, 1);
            }
            else if (info[5].engaged == -1) {
               info[5].wrenchC.set (0, 0, 0, 0, 0, -1);
            }
         }
      }
   }
}
