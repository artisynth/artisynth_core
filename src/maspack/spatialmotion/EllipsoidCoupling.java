/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.geometry.QuadraticUtils;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.GimbalCoupling.AxisSet;

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
      TGD.R.rotateZDirection (n); // TODO: update for additional rotation DOFs     

      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   /** 
    * TODO - add additional rotational DOFs
    */
   @Override
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

      System.out.println("updateCon");
      
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
   
   static void setROD(RotationMatrix3d ROD, Vector3d ndir) {
      Vector3d bitangent = new Vector3d();
      bitangent.cross (Vector3d.X_UNIT, ndir); // x-axis is up-direction for ellipsoid (D-frame)
      ROD.setZXDirections (ndir, bitangent);
   }
   
   static void setTheta(RotationMatrix3d RCD, Vector3d ndir, double theta) {

      // O frame is co-incident with C and at neutral orientation
      RotationMatrix3d RCO = new RotationMatrix3d ();
      RotationMatrix3d ROD = new RotationMatrix3d ();
      setROD (ROD, ndir);
      
      GimbalCoupling.setRpy (RCO, 0, 0, theta, AxisSet.XYZ);     
      RCD.mul (ROD, RCO);
   }
   
   static double getTheta(Vector3d ndir, RotationMatrix3d RCD) {
      
      // O frame is co-incident with C and at neutral orientation
      RotationMatrix3d RCO = new RotationMatrix3d ();
      RotationMatrix3d ROD = new RotationMatrix3d ();
      setROD (ROD, ndir);
      
      RCO.mulInverseLeft (ROD, RCD);
      double[] rpy = new double[3];
      GimbalCoupling.getRpy (rpy, RCO, AxisSet.XYZ);
      return rpy[2]; // theta
   }
 
   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
//      QuadraticUtils.nearestPointEllipsoid (ptmp, a, b, c, TCD.p);
      p.set (TCD.p);
      n.x = p.x/(a*a);
      n.y = p.y/(b*b);
      n.z = p.z/(c*c);
      
      double u1, u2;
      double cos2 = Math.sqrt(n.y*n.y + n.z*n.z);
      double sin2 = n.x;

      if (Math.abs(cos2) < Math.abs(sin2)) { 
         u2 = Math.acos (cos2);
      }
      else {
         u2 = Math.asin (sin2);
      }
         
      u1 = Math.atan2 (-n.y/cos2, n.z/cos2);
      
      coords.set (X_IDX, u1);
      coords.set (Y_IDX, u2);
      coords.set (THETA_IDX, getTheta (n, TCD.R));
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double u1, double u2, double theta) {

      TCD.setIdentity();
      
      double cos1 = Math.cos (u1);
      double sin1 = Math.sin (u1);
      double cos2 = Math.cos (u2);
      double sin2 = Math.sin (u2);
      
      // Seth Paper eq (6)
      p.x = a*sin2;
      p.y = -b*sin1*cos2;
      p.z = c*cos1*cos2;
      
      n.x = p.x/(a*a);
      n.y = p.y/(b*b);
      n.z = p.z/(c*c);
      
      TCD.p.set (p);
      setTheta (TCD.R, n, theta);
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1), coords.get(2));
   }

}
