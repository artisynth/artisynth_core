/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

/** 
 * Constraints a rigid body to 2D translational motion (without rotation) in a
 * plane.
 */
public class PlanarTranslationCoupling extends RigidBodyCoupling {

   private static final double INF = Double.POSITIVE_INFINITY; 

   private double myMinX = -INF;
   private double myMaxX = INF;

   private double myMinY = -INF;
   private double myMaxY = INF;

   /**
    * Sets the maximum value of x for this planar coupling. The default
    * value is +infinity.
    * 
    * @param max
    * maximum value for x
    */
   public void setMaximumX (double max) {
      myMaxX = max;
   }

   /**
    * Returns the maximum value of x for this planar coupling.
    * 
    * @return maximum value for x
    */
   public double getMaximumX() {
      return myMaxX;
   }

   /**
    * Sets the minimum value of x for this planar coupling. The default
    * value is -infinity.
    * 
    * @param min
    * minimum value for x
    */
   public void setMinimumX (double min) {
      myMinX = min;
   }

   /**
    * Returns the minimum value of x for this planar coupling.
    * 
    * @return minimum value for x
    */
   public double getMinimumX() {
      return myMinX;
   }

   /**
    * Sets the maximum value of y for this planar coupling. The default
    * value is +infinity.
    * 
    * @param max
    * maximum value for y
    */
   public void setMaximumY (double max) {
      myMaxY = max;
   }

   /**
    * Returns the maximum value of y for this planar coupling.
    * 
    * @return maximum value for y
    */
   public double getMaximumY() {
      return myMaxY;
   }

   /**
    * Sets the minimum value of y for this planar coupling. The default
    * value is -infinity.
    * 
    * @param min
    * minimum value for y
    */
   public void setMinimumY (double min) {
      myMinY = min;
   }

   /**
    * Returns the minimum value of y for this planar coupling.
    * 
    * @return minimum value for y
    */
   public double getMinimumY() {
      return myMinY;
   }

   public PlanarTranslationCoupling() {
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
      TGD.set (TCD);
      TGD.R.setIdentity();
      TGD.p.z = 0;
   }

   private double doGetX (RigidTransform3d TGD) {
      checkConstraintStorage();
      return -TGD.p.x;
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
         TGD.p.x = -x;
         TGD.p.z = 0;
      }
      myConstraintInfo[4].coordinate = x;
   }
   
   private double doGetY (RigidTransform3d TGD) {
      checkConstraintStorage();
      return -TGD.p.y;
   }

   public double getY (RigidTransform3d TGD) {
      if (TGD != null) {
         // on entry, TGD is set to TCD. It is then projected to TGD
         projectToConstraint (TGD, TGD);
         return doGetY (TGD);
      }
      else {
         // simply read back coordinate settings
         checkConstraintStorage();
         return myConstraintInfo[5].coordinate;
      }
   }

   public void setY (
      RigidTransform3d TGD, double y) {
      checkConstraintStorage();
      if (TGD != null) {
         TGD.p.y = -y;
         TGD.p.z = 0;
      }
      myConstraintInfo[5].coordinate = y;
   }
   
   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|ROTARY);
      info[2].flags = (BILATERAL|ROTARY);     
      info[3].flags = (BILATERAL|ROTARY);     

      info[4].flags = (LINEAR);
      info[5].flags = (LINEAR);

      info[0].wrenchC.set (0, 0, 1, 0, 0, 0);
      info[1].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[2].wrenchC.set (0, 0, 0, 0, 1, 0);
      info[3].wrenchC.set (0, 0, 0, 0, 0, 1);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {

      myErr.set (XERR);
      setDistancesAndZeroDerivatives (info, 4, myErr);

      if (myMinX != -INF || myMaxX != INF) {
         double x = doGetX(TGD);
         ConstraintInfo liminfo = info[4];
         if (setEngaged) {
            maybeSetEngaged (liminfo, x, myMinX, myMaxX);
         }
         if (liminfo.engaged != 0) {
            liminfo.distance = getDistance (x, myMinX, myMaxX);
            liminfo.dotWrenchC.setZero();
            if (liminfo.engaged == 1) {
               liminfo.wrenchC.set (1, 0, 0, 0, 0, 0);
            }
            else if (liminfo.engaged == -1) {
               liminfo.wrenchC.set (-1, 0, 0, 0, 0, 0);
            }
         }
      }

      if (myMinY != -INF || myMaxY != INF) {
         double y = doGetY(TGD);
         ConstraintInfo liminfo = info[5];
         if (setEngaged) {
            maybeSetEngaged (liminfo, y, myMinY, myMaxY);
         }
         if (liminfo.engaged != 0) {
            liminfo.distance = getDistance (y, myMinY, myMaxY);
            liminfo.dotWrenchC.setZero();
            if (liminfo.engaged == 1) {
               liminfo.wrenchC.set (0, 1, 0, 0, 0, 0);
            }
            else if (liminfo.engaged == -1) {
               liminfo.wrenchC.set (0, -1, 0, 0, 0, 0);
            }
         }
      }
   }
}
