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
 * Constraints a rigid body to 2D motion (with rotation) in a plane. This is
 * really just revolute coupling, relaxed to allow planar translation.
 */
public class FullPlanarCoupling extends RigidBodyCoupling {

   public FullPlanarCoupling() {
      super();
   }

//   public FullPlanarCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXFA (TCA);
//      setXDB (XDB);
//   }

   @Override
   public int maxUnilaterals() {
      return 0;
   }

   @Override
   public int numBilaterals() {
      return 3;
   }

   @Override
   public void projectToConstraint (RigidTransform3d TGD, RigidTransform3d TCD) {
      TGD.set (TCD);
      TGD.R.rotateZDirection (Vector3d.Z_UNIT);
      TGD.p.z = 0;
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|ROTARY);
      info[2].flags = (BILATERAL|ROTARY);     

      info[0].wrenchC.set (0, 0, 1, 0, 0, 0);
      info[1].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[2].wrenchC.set (0, 0, 0, 0, 1, 0);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {
      //projectToConstraint (TGD, TCD);

      //myXFC.mulInverseLeft (TGD, TCD);
      myErr.set (XERR);
      setDistancesAndZeroDerivatives (info, 3, myErr);
   }

//    /**
//     * For planar couplings, we do not change F relative to A when D changes
//     * relative to the world. This method hence becomes a noop.
//     * 
//     * @param XAW
//     * new pose of body A in world coordinates
//     * @param XBW
//     * pose of body B in world coordinates
//     */
//    public void updateXFA (RigidTransform3d XAW, RigidTransform3d XBW) {
//    }
}
