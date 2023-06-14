/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.geometry.QuadraticUtils;
import maspack.matrix.Matrix6x3;
import maspack.matrix.MatrixNd;
import maspack.matrix.QRDecomposition;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.GimbalCoupling.AxisSet;
import maspack.spatialmotion.RigidBodyCoupling.CoordinateInfo;

/** 
 * Constraints a rigid body to 2D motion (with rotation) on the surface
 *  of an ellipsoid. 
 */
public class EllipsoidCoupling3d extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;
   public static final int THETA_IDX = 2;
   
   // ellipsoid radii
   private double a, b, c;
   
   // temporary variables
   private Vector3d p = new Vector3d();
   private Vector3d n = new Vector3d();
   Matrix6x3 myH;
   protected QRDecomposition myQRD; 
   protected MatrixNd myQ;
   Wrench[] myG;

   private static final double INF = Double.POSITIVE_INFINITY; 

   public EllipsoidCoupling3d(double a, double b, double c) {
      super();
      this.a = a;
      this.b = b;
      this.c = c;
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      TGD.set (TCD);
  
      double dist = QuadraticUtils.nearestPointEllipsoid (p, a, b, c, TGD.p);
      TGD.p.set (p);
      
      // compute normal direction
      n.x = p.x/(a*a);
      n.y = p.y/(b*b);
      n.z = p.z/(c*c);
      TGD.R.rotateZDirection (n);

      // construct RCO frame in tangent plane of ellipsoid
//      RotationMatrix3d RCO = new RotationMatrix3d ();
//      RotationMatrix3d ROD = new RotationMatrix3d ();
//      getROD (ROD, n);
//      RCO.mulInverseLeft (ROD, TGD.R);
//      RCO.rotateZDirection (Vector3d.Z_UNIT);      
//      TGD.R.mul (ROD, RCO);

      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   @Override
   public void initializeConstraints () {
      
      addConstraint (BILATERAL|LINEAR, new Wrench(0, 0, 1, 0, 0, 0)); // constrain translation in normal
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 1, 0, 0)); // constrain rotations about bi-tangent 
      addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 0, 1, 0)); // constrain rotations about tangent
      addConstraint (LINEAR);
      addConstraint (LINEAR);
      addConstraint (ROTARY, new Wrench(0, 0, 0, 0, 0, 1));

      addCoordinate (-INF, INF, 0, getConstraint(3));
      addCoordinate (-INF, INF, 0, getConstraint(4));
      addCoordinate (-INF, INF, 0, getConstraint(5));
      
      // initializations for temporary variables used in updateConstraints
      int numc = myCoordinates.size ();
      myH = new Matrix6x3 ();
      myQRD = new QRDecomposition (); 
      myQ = new MatrixNd (6, 6);
      myG = new Wrench[numc];
      for (int i=0; i<numc; i++) {
         myG[i] = new Wrench();
      }
   }
   

   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {
      
      // compute n from Seth eq (6), note this is not the surface normal
      TGD.p.set (p);
      n.x = p.x/(a);
      n.y = p.y/(b);
      n.z = p.z/(c);
      
      // compute H matrix - Seth eq (8), with spatial vel V = [ v  w ]^T
      myH.setZero ();
      myH.m01 = a*n.z;
      myH.m02 = -a*n.y;
      myH.m10 = -b*n.z;
      myH.m12 = b*n.x;
      myH.m20 = c*n.y;
      myH.m21 = -c*n.x;
      myH.m30 = 1d;
      myH.m41 = 1d;
      myH.m52 = 1d;
      
      // from OpenSimCustomJoint
      int numc = numCoordinates();
      myQRD.factorWithPivoting (myH);
      int[] perm = new int[numc];
      myQRD.get (myQ, null, perm);
      if (myQRD.rank (1e-8) < numc) {
         System.out.println (
            "WARNING: joint has rank "+myQRD.rank(1e-8)+" vs. " + numc);
         System.out.println ("coupling=" + this);
      }
      VectorNd gcol = new VectorNd(numc);
      for (int i=0; i<6; i++) {
         for (int j=0; j<numc; j++) {
            gcol.set (j, myQ.get(i, j));
         }
         myQRD.solveR (gcol, gcol);
         for (int j=0; j<numc; j++) {
            myG[j].set (i, gcol.get(j));
         }
      }
      // convert to G coords
      for (int j=0; j<numc; j++) {
         myG[j].inverseTransform (TGD.R);
         RigidBodyConstraint con = myCoordinates.get (j).limitConstraint;
         con.wrenchG.set (myG[j]);
         con.dotWrenchG.setZero ();
         // LINEAR/ROTARY flags set at initialization, so no need to update
      }
   }

   // updateConstraints from FullPlanarCoupling, which
   // ignores velocity coupling due to changing surface normal
   // in the case of an ellipsoid surface
   public void updateConstraintsX (
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
         System.out.println("x-limit");
         // constraint wrench along x, transformed to C, is (c, -s, 0)
         xcons.wrenchG.set (c, -s, 0, 0, 0, 0);
//         xcons.dotWrenchG.set (-s*dotTheta, -c*dotTheta, 0, 0, 0, 0);
         xcons.dotWrenchG.setZero ();
      }
      // update y limit constraint if necessary
      RigidBodyConstraint ycons = myCoordinates.get(Y_IDX).limitConstraint;
      if (ycons.engaged != 0) {
         System.out.println("y-limit");
         // constraint wrench along y, transformed to C, is (s, c, 0)
         ycons.wrenchG.set (s, c, 0, 0, 0, 0);
//         ycons.dotWrenchG.set (c*dotTheta, -s*dotTheta, 0, 0, 0, 0);
         ycons.dotWrenchG.setZero ();
      }
      // theta limit constraint is constant, so no need to update
   }
   
   
   static private void getROD(RotationMatrix3d ROD, Vector3d ndir) {
      Vector3d bitangent = new Vector3d();
      bitangent.cross (Vector3d.X_UNIT, ndir); // x-axis is up-direction for ellipsoid (D-frame)
      ROD.setZXDirections (ndir, bitangent);
   }
   
   static void setRot(RotationMatrix3d RCD, Vector3d ndir, double theta) {

      // O frame is co-incident with C and at neutral orientation
      RotationMatrix3d RCO = new RotationMatrix3d ();
      RotationMatrix3d ROD = new RotationMatrix3d ();
      getROD (ROD, ndir);
      
      double sr, cr;

      sr = Math.sin (theta);
      cr = Math.cos (theta);
      RCO.m00 = cr;
      RCO.m01 = -sr;
      RCO.m10 = sr;
      RCO.m11 = cr;
      
      RCD.mul (ROD, RCO);
   }
   
   static double[] getRot(Vector3d ndir, RotationMatrix3d RCD) {
      
      // O frame is co-incident with C and at neutral orientation
      RotationMatrix3d RCO = new RotationMatrix3d ();
      RotationMatrix3d ROD = new RotationMatrix3d ();
      getROD (ROD, ndir);
      
      RCO.mulInverseLeft (ROD, RCD);
      double[] angs = new double[2];
      angs[0] = Math.atan2 (-RCO.m01, RCO.m11);
      angs[1] = Math.atan2 (-RCO.m20, RCO.m22);
      return angs;
   }
 
   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {

      // compute n from Seth eq (6), note this is not the surface normal
      p.set (TCD.p);
      n.x = p.x/a;
      n.y = p.y/b;
      n.z = p.z/c;
      
      double x, y;
      double cos2 = Math.sqrt(n.y*n.y + n.z*n.z);
      double sin2 = n.x;

      if (Math.abs(cos2) < Math.abs(sin2)) { 
         y = Math.acos (cos2);
      }
      else {
         y = Math.asin (sin2);
      }
      x = Math.atan2 (-n.y/cos2, n.z/cos2);
      
      coords.set (X_IDX, x);
      coords.set (Y_IDX, y);
      
      // scale n to get surface normal direction at p
      n.x = n.x/a; // nrml.x = p.x/(a*a)
      n.y = n.y/b; // nrml.y = p.y/(b*b)
      n.z = n.z/c; // nrml.z = p.z/(c*c)
      
      double[] rpy = getRot (n, TCD.R);
      coords.set (THETA_IDX, rpy[0]); // theta about z-axis, ZYX rpy
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double x, double y, double theta) {

      TCD.setIdentity();
      
      double cos1 = Math.cos (x);
      double sin1 = Math.sin (x);
      double cos2 = Math.cos (y);
      double sin2 = Math.sin (y);
      
      p.x = a*sin2;
      p.y = -b*sin1*cos2;
      p.z = c*cos1*cos2;
      
      n.x = p.x/(a*a);
      n.y = p.y/(b*b);
      n.z = p.z/(c*c);
 
      TCD.p.set (p);
      setRot (TCD.R, n, theta);      
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {

      coordinatesToTCD (TCD, coords.get(0), coords.get(1), coords.get(2));
   }

}
