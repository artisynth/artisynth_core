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
public class SliderCoupling extends RigidBodyCoupling {

   public static final int Z_IDX = 0;

   public SliderCoupling() {
      super();
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.R.setIdentity();
      TGD.p.x = 0;
      TGD.p.y = 0;
      TGD.p.z = TCD.p.z;
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }      
   }

   public void initializeConstraints () {
      
      addConstraint (BILATERAL|LINEAR, new Wrench (1, 0, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench (0, 1, 0, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 1, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 0, 1, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 0, 0, 1));
      addConstraint (LINEAR, new Wrench (0, 0, 1, 0, 0, 0));

      addCoordinate (-INF, INF, 0, getConstraint(5));
   }

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      // nothing to do - all constraints are constant
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      coords.set(Z_IDX, TCD.p.z);
   }

   public void coordinatesToTCD (RigidTransform3d TCD, double z) {
      TCD.setIdentity();
      TCD.p.z = z;
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0));
   }
}
