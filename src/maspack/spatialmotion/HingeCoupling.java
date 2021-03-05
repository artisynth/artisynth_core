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
public class HingeCoupling extends RigidBodyCoupling {

   public static final int THETA_IDX = 0;

   protected int myThetaSign = 1; // -1 for clockwise, 1 for counter-clockwise

   public HingeCoupling() {
      super();
   }

   public void setThetaClockwise (boolean enable) {
      myThetaSign = (enable ? -1 : 1);
      initializeConstraintInfo();
   }

   public boolean isThetaClockwise () {
      return myThetaSign == -1;
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.R.set (TCD.R);
      TGD.R.rotateZDirection (Vector3d.Z_UNIT);
      TGD.p.setZero();
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   public void initializeConstraints () {
      if (myThetaSign == 0) {
         // not yet set because we are in superclass constructor
         myThetaSign = 1; 
      }
      addConstraint (BILATERAL|LINEAR, new Wrench (1, 0, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench (0, 1, 0, 0, 0, 0));
      addConstraint (BILATERAL|LINEAR, new Wrench (0, 0, 1, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 1, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench (0, 0, 0, 0, 1, 0));
      addConstraint (ROTARY, new Wrench (0, 0, 0, 0, 0, myThetaSign));

      addCoordinate (-INF, INF, 0, getConstraint(5));
   }

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      // nothing to do - all constraints are constant
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      double theta;
      if (isThetaClockwise()) {
         theta = Math.atan2 (TCD.R.m01, TCD.R.m00);
      }
      else {
         theta = Math.atan2 (TCD.R.m10, TCD.R.m00);
      }
      coords.set (THETA_IDX, getCoordinate(THETA_IDX).nearestAngle(theta));
   }

   public void coordinatesToTCD (RigidTransform3d TCD, double theta) {
      TCD.setIdentity();
      double c = Math.cos (theta);
      double s = Math.sin (theta);
      TCD.R.m00 = c;
      TCD.R.m11 = c;
      if (isThetaClockwise()) {
         TCD.R.m01 = s;
         TCD.R.m10 = -s;
      }
      else {
         TCD.R.m01 = -s;
         TCD.R.m10 = s;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0));
   }

}
