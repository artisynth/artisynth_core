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
public class SlottedHingeCoupling extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int THETA_IDX = 1;

   public SlottedHingeCoupling() {
      super();
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.R.set (TCD.R);
      TGD.R.rotateZDirection (Vector3d.Z_UNIT);
      TGD.p.x = TCD.p.x;
      TGD.p.y = 0;
      TGD.p.z = 0;
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }
   }

   public void initializeConstraints () {
      addConstraint (BILATERAL|LINEAR);
      addConstraint (BILATERAL|LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 1, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 0, 1, 0));
      addConstraint (LINEAR);
      addConstraint (ROTARY, new Wrench(0, 0, 0, 0, 0, 1));

      addCoordinate (-INF, INF, 0, getConstraint(4));
      addCoordinate (-INF, INF, 0, getConstraint(5));
   }

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      Vector3d wDC = new Vector3d(); // FINISH: angular vel D wrt C, in C

      // constraint wrench along y is constant in D but when transformed to D
      // has the form (s, c, 0, 0, 0, 0), where s and c are the sine and cosine
      // of theta.      
      RigidBodyConstraint cinfo = getConstraint(0); // constraint along y axis of D
      double s = TGD.R.m10;  // extract sine and cosine of theta
      double c = TGD.R.m00;
      cinfo.wrenchG.set (s, c, 0, 0, 0, 0);
      // derivative term:
      double dotTheta = -wDC.z; // negate because wDC in C
      cinfo.dotWrenchG.set (c*dotTheta, -s*dotTheta, 0, 0, 0, 0);

      // update x limit constraint if necessary
      RigidBodyConstraint xcons = myCoordinates.get(X_IDX).limitConstraint;
      if (xcons.engaged != 0) {
         // constraint wrench along x, transformed to C, is (-c, s, 0)
         xcons.wrenchG.set (c, -s, 0, 0, 0, 0);
         xcons.dotWrenchG.set (-s*dotTheta, -c*dotTheta, 0, 0, 0, 0);
      }
      // theta limit is constant
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      coords.set(X_IDX, TCD.p.x);
      double theta = Math.atan2 (TCD.R.m10, TCD.R.m00);
      coords.set (THETA_IDX, getCoordinate(THETA_IDX).nearestAngle(theta));
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double x, double theta) {

      TCD.setIdentity();
      TCD.p.x = x;
      double c = Math.cos (theta);
      double s = Math.sin (theta);
      TCD.R.m00 = c;
      TCD.R.m11 = c;
      TCD.R.m01 = -s;
      TCD.R.m10 = s;
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1));
   }

}
