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
public class RevoluteCoupling extends RigidBodyCoupling {
   private double myMinTheta = -Math.PI;
   private double myMaxTheta = Math.PI;

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
    * Returns the maximum value of theta for this revolute coupling.
    * 
    * @return maximum value for theta
    */
   public double getMaximumTheta() {
      return myMaxTheta;
   }

   /**
    * Sets the minimum value of theta for this revolute coupling. The default
    * value is -PI.
    * 
    * @param min
    * minimum value for theta
    */
   public void setMinimumTheta (double min) {
      myMinTheta = min;
   }

   /**
    * Returns the minimum value of theta for this revolute coupling.
    * 
    * @return minimum value for theta
    */
   public double getMinimumTheta() {
      return myMinTheta;
   }

   /**
    * Returns true if this coupling has a range restriction; this will be true
    * if the range of theta is less than [-PI, PI].
    * 
    * @return true if this coupling has a range restriction.
    */
   public boolean hasRestrictedRange() {
      return (myMinTheta != Double.NEGATIVE_INFINITY ||
              myMaxTheta != Double.POSITIVE_INFINITY);
   }

   public RevoluteCoupling() {
      super();
   }

//   public RevoluteCoupling (RigidTransform3d XCA, RigidTransform3d XDB) {
//      this();
//      setXDB (XDB);
//      setXFA (XCA);
//   }

   @Override
   public int maxUnilaterals() {
      return 1;
   }

   @Override
   public int numBilaterals() {
      return 5;
   }

   @Override
   public void projectToConstraint (RigidTransform3d XGD, RigidTransform3d XCD) {
      XGD.R.set (XCD.R);
      XGD.R.rotateZDirection (Vector3d.Z_UNIT);
      XGD.p.setZero();
   }

   private double doGetTheta (RigidTransform3d XGD) {
      checkConstraintStorage();
      double theta = Math.atan2 (XGD.R.m01, XGD.R.m00);
      theta = findNearestAngle (myConstraintInfo[5].coordinate, theta);
      myConstraintInfo[5].coordinate = theta;
      return theta;
   }

   public double getTheta (RigidTransform3d XGD) {
      // on entry, XGD is set to XCD. It is then projected to XGD
      projectToConstraint (XGD, XGD);
      return doGetTheta (XGD);
   }

   public void setTheta (
      RigidTransform3d XGD, double theta) {
      checkConstraintStorage();
      XGD.setIdentity();
      double c = Math.cos (theta);
      double s = Math.sin (theta);
      XGD.R.m00 = c;
      XGD.R.m01 = s;
      XGD.R.m10 = -s;
      XGD.R.m11 = c;
      myConstraintInfo[5].coordinate = theta;
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|LINEAR);
      info[2].flags = (BILATERAL|LINEAR);
      info[3].flags = (BILATERAL|ROTARY);
      info[4].flags = (BILATERAL|ROTARY);
      info[5].flags = (ROTARY);

      info[0].wrenchC.set (1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set (0, 1, 0, 0, 0, 0);
      info[2].wrenchC.set (0, 0, 1, 0, 0, 0);
      info[3].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[4].wrenchC.set (0, 0, 0, 0, 1, 0);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d XGD, RigidTransform3d XCD,
      RigidTransform3d XERR, boolean setEngaged) {
      //projectToConstraint (XGD, XCD);

      //myXFC.mulInverseLeft (XGD, XCD);
      myErr.set (XERR);
      setDistancesAndZeroDerivatives (info, 5, myErr);

      //NumberFormat fmt = new NumberFormat ("%11.6f");
      double theta = doGetTheta(XGD);
      if (hasRestrictedRange()) {
         if (setEngaged) {
            maybeSetEngaged (info[5], theta, myMinTheta, myMaxTheta);
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
