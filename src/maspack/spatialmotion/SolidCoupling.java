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

//   public SolidCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXDB (XDB);
//      setXFA (TCA);
//   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.R.setIdentity();
      TGD.p.setZero();
   }

   public void initializeConstraints () {
      addConstraint (BILATERAL|LINEAR, new Wrench (1, 0, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench (0, 1, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench (0, 0, 1, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 1, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 0, 1, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 0, 0, 1));
   }

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {
      // nothing to do - constraints are constant, and distances will
      // have been computed automatically
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {
      TCD.setIdentity();
   }


}
