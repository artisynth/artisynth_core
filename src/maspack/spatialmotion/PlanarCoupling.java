/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.RigidTransform3d;

public class PlanarCoupling extends RigidBodyCoupling {
   private boolean myUnilateral = false;

   public void setUnilateral (boolean unilateral) {
      myUnilateral = unilateral;
   }

   public boolean isUnilateral() {
      return myUnilateral;
   }

   public PlanarCoupling() {
      super();
   }

//   public PlanarCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXFA (TCA);
//      setXDB (XDB);
//   }

   @Override
   public int maxUnilaterals() {
      return myUnilateral ? 1 : 0;
   }

   @Override
   public int numBilaterals() {
      return myUnilateral ? 0 : 1;
   }

   @Override
   public void projectToConstraint (RigidTransform3d TGD, RigidTransform3d TCD) {
      TGD.set (TCD);
      TGD.p.z = 0;
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = LINEAR;
      if (!myUnilateral) {
         info[0].flags |= BILATERAL;
      }
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD, 
      RigidTransform3d XERR, boolean setEngaged) {
      
      //projectToConstraint (TGD, TCD);

      // wrench force is the plane normal, which is (0,0,1) in D
      // coordinates, transformed to C, and so is given by the
      // last column of inv(TGD.R), or the last row of TGD.R
      myErr.set (XERR);
      
      info[0].wrenchC.f.set (TGD.R.m20, TGD.R.m21, TGD.R.m22);
      info[0].wrenchC.m.setZero();
      //double d = TCD.p.z;
      double d = info[0].wrenchC.dot (myErr);

      info[0].distance = d;
      info[0].dotWrenchC.setZero();
      if (setEngaged) {
         if (myUnilateral && d < getContactDistance()) {
            info[0].engaged = 1;
         }
      }
   }

//   /**
//    * For planar couplings, we do not change F relative to A when D changes
//    * relative to the world. This method hence becomes a noop.
//    * 
//    * @param XAW
//    * new pose of body A in world coordinates
//    * @param XBW
//    * pose of body B in world coordinates
//    */
//   public void updateXFA (RigidTransform3d XAW, RigidTransform3d XBW) {
//   }
}
