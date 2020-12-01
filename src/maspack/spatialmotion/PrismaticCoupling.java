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
 * Implements a one DOF prismatic coupling. Frames C and D share a common
 * orientation and slide with respect to each other about a common z axis. The
 * distance between the two frames along to z axis is
 * described by the variable <i>z</i>. Range limits can be placed on z.
 */
public class PrismaticCoupling extends RigidBodyCoupling {
   private double myMinZ = Double.NEGATIVE_INFINITY;
   private double myMaxZ = Double.POSITIVE_INFINITY;

   /**
    * Sets the maximum value of z for this prismatic coupling. The default
    * value is +infinity.
    * 
    * @param max
    * maximum value for z
    */
   public void setMaximumZ (double max) {
      myMaxZ = max;
   }

   /**
    * Returns the maximum value of z for this prismatic coupling.
    * 
    * @return maximum value for z
    */
   public double getMaximumZ() {
      return myMaxZ;
   }

   /**
    * Sets the minimum value of z for this prismatic coupling. The default
    * value is -infinity.
    * 
    * @param min
    * minimum value for z
    */
   public void setMinimumZ (double min) {
      myMinZ = min;
   }

   /**
    * Returns the minimum value of z for this prismatic coupling.
    * 
    * @return minimum value for z
    */
   public double getMinimumZ() {
      return myMinZ;
   }

   /**
    * Returns true if this coupling has a range restriction; this will be true
    * if the range of z is inside the default range [-infinity, infinity].
    * 
    * @return true if this coupling has a range restriction.
    */
   public boolean hasRestrictedRange() {
      return (myMinZ != Double.NEGATIVE_INFINITY ||
              myMaxZ != Double.POSITIVE_INFINITY);
   }

   public PrismaticCoupling() {
      super();
   }

//   public PrismaticCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXDB (XDB);
//      setXFA (TCA);
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
   public void projectToConstraint (RigidTransform3d TGD, RigidTransform3d TCD) {
      TGD.R.setIdentity();
      TGD.p.x = 0;
      TGD.p.y = 0;
      TGD.p.z = TCD.p.z;
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
         TGD.setIdentity();
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
      info[5].flags = (BILATERAL|ROTARY);

      info[0].wrenchC.set (1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set (0, 1, 0, 0, 0, 0);
      info[3].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[4].wrenchC.set (0, 0, 0, 0, 1, 0);
      info[5].wrenchC.set (0, 0, 0, 0, 0, 1);
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
      setDistanceAndZeroDerivative (info[5], myErr);

      double z = doGetZ(TGD);
      if (hasRestrictedRange()) {
         if (setEngaged) {
            maybeSetEngaged (info[2], z, myMinZ, myMaxZ);
         }
         if (info[2].engaged != 0) {
            info[2].distance = getDistance (z, myMinZ, myMaxZ);
            info[2].dotWrenchC.setZero();
            if (info[2].engaged == 1) {
               info[2].wrenchC.set (0, 0, 1, 0, 0, 0);
            }
            else if (info[2].engaged == -1) {
               info[2].wrenchC.set (0, 0, -1, 0, 0, 0);
            }
         }
      }
   }
}
