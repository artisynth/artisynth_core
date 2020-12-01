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
 * Implements a one DOF cylindrical coupling. Frames C and D share a common origin
 * and rotate about a common z axis. The distance from the x axis of C to the x
 * axis of D, about the z axis, is described by the angle <i>theta</i>. Range
 * limits can be placed on theta.
 */
public class CylindricalCoupling extends RigidBodyCoupling {
   private static final double INF = Double.POSITIVE_INFINITY; 

   private double myMinTheta = -Math.PI;
   private double myMaxTheta = Math.PI;

   private double myMinZ = -INF;
   private double myMaxZ = INF;

   /**
    * Sets the maximum value of theta for this cylindrical coupling. The default
    * value is PI.
    * 
    * @param max
    * maximum value for theta
    */
   public void setMaximumTheta (double max) {
      myMaxTheta = max;
   }

   /**
    * Returns the maximum value of theta for this cylindrical coupling.
    * 
    * @return maximum value for theta
    */
   public double getMaximumTheta() {
      return myMaxTheta;
   }

   /**
    * Sets the minimum value of theta for this cylindrical coupling. The default
    * value is -PI.
    * 
    * @param min
    * minimum value for theta
    */
   public void setMinimumTheta (double min) {
      myMinTheta = min;
   }

   /**
    * Returns the minimum value of theta for this cylindrical coupling.
    * 
    * @return minimum value for theta
    */
   public double getMinimumTheta() {
      return myMinTheta;
   }

   /**
    * Sets the maximum value of z for this cylindrical coupling. The default
    * value is +infinity.
    * 
    * @param max
    * maximum value for z
    */
   public void setMaximumZ (double max) {
      myMaxZ = max;
   }

   /**
    * Returns the maximum value of z for this cylindrical coupling.
    * 
    * @return maximum value for z
    */
   public double getMaximumZ() {
      return myMaxZ;
   }

   /**
    * Sets the minimum value of z for this cylindrical coupling. The default
    * value is -infinity.
    * 
    * @param min
    * minimum value for z
    */
   public void setMinimumZ (double min) {
      myMinZ = min;
   }

   /**
    * Returns the minimum value of z for this cylindrical coupling.
    * 
    * @return minimum value for z
    */
   public double getMinimumZ() {
      return myMinZ;
   }

   public CylindricalCoupling() {
      super();
   }

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
      TGD.p.x = 0;
      TGD.p.y = 0;
      TGD.p.z = TCD.p.z;
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
         TGD.R.setIdentity();
         double c = Math.cos (theta);
         double s = Math.sin (theta);
         TGD.R.m00 = c;
         TGD.R.m01 = s;
         TGD.R.m10 = -s;
         TGD.R.m11 = c;
      }
      myConstraintInfo[5].coordinate = theta;
   }

   private double doGetZ (RigidTransform3d TGD) {
      checkConstraintStorage();
      return -TGD.p.z;
   }

   public double getZ (RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectToConstraint (TGD, TGD);
         return doGetZ (TGD);
      }
      else {
         // simply read back coordinate settings
         checkConstraintStorage();
         return myConstraintInfo[2].coordinate;
      }
   }

   public void setZ (
      RigidTransform3d TGD, double z) {
      checkConstraintStorage();
      if (TGD != null) {
         TGD.p.x = 0;
         TGD.p.y = 0;
         TGD.p.z = -z;
      }
      myConstraintInfo[2].coordinate = z;
   }
   

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|LINEAR);
      info[2].flags = (LINEAR);
      info[3].flags = (BILATERAL|ROTARY);
      info[4].flags = (BILATERAL|ROTARY);
      info[5].flags = (ROTARY);

      info[0].wrenchC.set (1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set (0, 1, 0, 0, 0, 0);
      info[3].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[4].wrenchC.set (0, 0, 0, 0, 1, 0);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {

      myErr.set (XERR);
      setDistanceAndZeroDerivative (info[0], myErr);
      setDistanceAndZeroDerivative (info[1], myErr);
      setDistanceAndZeroDerivative (info[3], myErr);
      setDistanceAndZeroDerivative (info[4], myErr);

      if (myMaxZ != INF || myMinZ != -INF) {
         double z = doGetZ(TGD);
         ConstraintInfo liminfo = info[2];
         if (setEngaged) {
            maybeSetEngaged (liminfo, z, myMinZ, myMaxZ);
         }
         if (liminfo.engaged != 0) {
            liminfo.distance = getDistance (z, myMinZ, myMaxZ);
            liminfo.dotWrenchC.setZero();
            if (liminfo.engaged == 1) {
               liminfo.wrenchC.set (0, 0, 1, 0, 0, 0);
            }
            else if (liminfo.engaged == -1) {
               liminfo.wrenchC.set (0, 0, -1, 0, 0, 0);
            }
         }
      }
      
      if (myMinTheta > -Math.PI || myMaxTheta < Math.PI) {
         double theta = doGetTheta(TGD);
         ConstraintInfo liminfo = info[5];         
         if (setEngaged) {
            maybeSetEngaged (liminfo, theta, myMinTheta, myMaxTheta);
         }
         if (liminfo.engaged != 0) {
            liminfo.distance = getDistance (theta, myMinTheta, myMaxTheta);
            liminfo.dotWrenchC.setZero();
            if (liminfo.engaged == 1) {
               liminfo.wrenchC.set (0, 0, 0, 0, 0, 1);
            }
            else if (liminfo.engaged == -1) {
               liminfo.wrenchC.set (0, 0, 0, 0, 0, -1);
            }
         }
      }
   }
}
