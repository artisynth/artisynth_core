/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.geometry.QuadraticUtils;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/** 
 * Constraints a rigid body to 2D motion (with rotation) on the surface
 *  of an ellipsoid. 
 */
public class EllipsoidCoupling extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;
   public static final int THETA_IDX = 2;
   
   // ellipsoid radii
   private double a, b, c;
   
   // temporary variables
   private Vector3d p = new Vector3d();
   private Vector3d n = new Vector3d();

   private static final double INF = Double.POSITIVE_INFINITY; 

   public EllipsoidCoupling(double a, double b, double c) {
      super();
      this.a = a;
      this.b = b;
      this.c = c;
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      
      TGD.set (TCD);
      QuadraticUtils.nearestPointEllipsoid (p, a, b, c, TGD.p);
      n.x = p.x/(a*a);
      n.y = p.y/(b*b);
      n.z = p.z/(c*c);
      
      TGD.p.set (p);
      TGD.R.rotateZDirection (n);

      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   /** 
    * TODO - add additional rotational dof for u-joint
    */
   public void initializeConstraints () {
      addConstraint (BILATERAL|LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 1, 0, 0));
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 0, 1, 0));
      
      addConstraint (LINEAR);
      addConstraint (LINEAR);
      addConstraint (ROTARY, new Wrench(0, 0, 0, 0, 0, 1));

      addCoordinate (-INF, INF, 0, getConstraint(3));
      addCoordinate (-INF, INF, 0, getConstraint(4));
      addCoordinate (-INF, INF, 0, getConstraint(5));
   }

   /** 
    * TODO - update for ellipsoid
    */
   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {

      Vector3d wDC = new Vector3d(); // FINISH: angular vel D wrt C, in C

      // might be needed for x, y limits:
      double s = TGD.R.m10; // extract sine and cosine of theta
      double c = TGD.R.m00;
      double dotTheta = -wDC.z; // negate because wDC in C      

      // update x limit constraint if necessary
      RigidBodyConstraint xcons = myCoordinates.get(X_IDX).limitConstraint;
      if (xcons.engaged != 0) {
         // constraint wrench along x, transformed to C, is (c, -s, 0)
         xcons.wrenchG.set (c, -s, 0, 0, 0, 0);
         xcons.dotWrenchG.set (-s*dotTheta, -c*dotTheta, 0, 0, 0, 0);
      }
      // update y limit constraint if necessary
      RigidBodyConstraint ycons = myCoordinates.get(Y_IDX).limitConstraint;
      if (ycons.engaged != 0) {
         // constraint wrench along y, transformed to C, is (s, c, 0)
         ycons.wrenchG.set (s, c, 0, 0, 0, 0);
         ycons.dotWrenchG.set (c*dotTheta, -s*dotTheta, 0, 0, 0, 0);
      }
      // theta limit constraint is constant, so no need to update
   }
 
   /** 
    * TODO - update for ellipsoid
    */
   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
      coords.set(X_IDX, TCD.p.x);
      coords.set(Y_IDX, TCD.p.y);
      double theta = Math.atan2 (TCD.R.m10, TCD.R.m00);
      coords.set (THETA_IDX, getCoordinateInfo(THETA_IDX).nearestAngle(theta));
   }

   /** 
    * TODO - update for ellipsoid
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, double x, double y, double theta) {

      TCD.setIdentity();
      double c = Math.cos (theta);
      double s = Math.sin (theta);
      TCD.R.m00 = c;
      TCD.R.m01 = -s;
      TCD.R.m10 = s;
      TCD.R.m11 = c;  
      TCD.p.x = x;
      TCD.p.y = y;
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1), coords.get(2));
   }

}
