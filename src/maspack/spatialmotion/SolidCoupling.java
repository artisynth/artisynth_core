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
 * Implements a solid coupling in which one rigid body is solidly
 * connected to another, with no degrees of freedom.
 */
public class SolidCoupling extends RigidBodyCoupling {

   public SolidCoupling() {
      super();
   }

//   public SolidCoupling (RigidTransform3d XCA, RigidTransform3d XDB) {
//      this();
//      setXDB (XDB);
//      setXFA (XCA);
//   }

   @Override
   public int numBilaterals() {
      return 6;
   }

   @Override
   public int maxUnilaterals() {
      return 0;
   }

   @Override
   public void projectToConstraint (RigidTransform3d XGD, RigidTransform3d XCD) {
      XGD.R.setIdentity();
      XGD.p.setZero();
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = (BILATERAL|LINEAR);
      info[1].flags = (BILATERAL|LINEAR);
      info[2].flags = (BILATERAL|LINEAR);
      info[3].flags = (BILATERAL|ROTARY);
      info[4].flags = (BILATERAL|ROTARY);
      info[5].flags = (BILATERAL|ROTARY);

      info[0].wrenchC.set (1, 0, 0, 0, 0, 0);
      info[1].wrenchC.set (0, 1, 0, 0, 0, 0);
      info[2].wrenchC.set (0, 0, 1, 0, 0, 0);
      info[3].wrenchC.set (0, 0, 0, 1, 0, 0);
      info[4].wrenchC.set (0, 0, 0, 0, 1, 0);
      info[5].wrenchC.set (0, 0, 0, 0, 0, 1);
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d XGD, RigidTransform3d XCD,
      RigidTransform3d XERR, boolean setEngaged) {
      //projectToConstraint (XGD, XCD);

      //myXFC.mulInverseLeft (XGD, XCD);
      myErr.set (XERR);
      setDistancesAndZeroDerivatives (info, 6, myErr);
   }
}
