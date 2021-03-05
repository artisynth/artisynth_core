/**
 regr* Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/** 
 * Constraints a rigid body to 2D translational motion (without rotation) in a
 * plane.
 */
public class PlanarTranslationCoupling extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;

   private static final double INF = Double.POSITIVE_INFINITY; 

   public PlanarTranslationCoupling() {
      super();
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.set (TCD);
      TGD.R.setIdentity();
      TGD.p.z = 0;
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }      
   }

   public void initializeConstraints () {
      addConstraint (BILATERAL|LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 1, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 0, 1, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 0, 0, 1));
      addConstraint (LINEAR, new Wrench(1, 0, 0, 0, 0, 0));
      addConstraint (LINEAR, new Wrench(0, 1, 0, 0, 0, 0));

      addCoordinate (-INF, INF, 0, getConstraint(4));
      addCoordinate (-INF, INF, 0, getConstraint(5));
   }

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {
                                                        
      // nothing to do - all constraints are constant
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      coords.set(X_IDX, TCD.p.x);
      coords.set(Y_IDX, TCD.p.y);
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double x, double y) {

      TCD.setIdentity();
      TCD.p.x = x;
      TCD.p.y = y;
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1));
   }

}
