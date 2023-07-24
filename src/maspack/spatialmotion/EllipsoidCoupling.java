/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.geometry.QuadraticUtils;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6x3;
import maspack.matrix.MatrixNd;
import maspack.matrix.QRDecomposition;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3x4;

/** 
 * Constraints a rigid body to 2D motion (with rotation) on the surface of an
 * ellipsoid.
 */
public class EllipsoidCoupling extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;
   public static final int THETA_IDX = 2;
   public static final int PHI_IDX = 3;

   // OpenSim approximation for ellipsoid joints: assumes orientation is given
   // directy by the x, y, theta angles. This simplifies the calculations but
   // means that the z axis of C is generally not parallel to the ellipsoid
   // surface normal.
   public boolean myUseOpenSimApprox = true;

   static double EPS = 2e-15;
   
   // ellipsoid radii
   double myA, myB, myC;
   // wing joint angle with respect to y axis in frame 3
   private double myAlpha;
   double myCosA; // cos alpha
   double mySinA; // sin alpha
   
   protected QRDecomposition myQRD; 
   protected MatrixNd myQ;
   Wrench[] myG;

   private static final double INF = Double.POSITIVE_INFINITY; 

   public EllipsoidCoupling () {
      this (1.0, 1.0, 1.0, 0, false);
   }

   public EllipsoidCoupling (double a, double b, double c, double alpha) {
      this (a, b, c, alpha, false);
   }

   public EllipsoidCoupling (
      double a, double b, double c, double alpha, boolean useOpenSimApprox) {

      super();
      this.myA = a;
      this.myB = b;
      this.myC = c;
      setAlpha (alpha);
      this.myUseOpenSimApprox = useOpenSimApprox;
   }

   public boolean getUseOpenSimApprox() {
      return myUseOpenSimApprox;
   }

   public void setUseOpenSimApprox (boolean enable) {
      myUseOpenSimApprox = enable;
   }

   public void setSemiAxisLengths (Vector3d lengths) {
      myA = lengths.x;
      myB = lengths.y;
      myC = lengths.z;
   }

   public Vector3d getSemiAxisLengths () {
      return new Vector3d (myA, myB, myC);
   }

   public void setAlpha (double alpha) {
      myAlpha = alpha;
      myCosA = Math.cos (alpha);
      mySinA = Math.sin (alpha);
   }

   public double getAlpha() {
      return myAlpha;
   }

   private void mulRotAlpha (RotationMatrix3d R1, RotationMatrix3d R0) {
      double m00 = R0.m00;
      double m10 = R0.m10;
      double m20 = R0.m20;

      double m01 = R0.m01;
      double m11 = R0.m11;
      double m21 = R0.m21;

      R1.m02 = R0.m02;
      R1.m12 = R0.m12;
      R1.m22 = R0.m22;

      R1.m00 = myCosA*m00 + mySinA*m01;
      R1.m10 = myCosA*m10 + mySinA*m11;
      R1.m20 = myCosA*m20 + mySinA*m21;

      R1.m01 = -mySinA*m00 + myCosA*m01;
      R1.m11 = -mySinA*m10 + myCosA*m11;
      R1.m21 = -mySinA*m20 + myCosA*m21;
   }

   private void mulInvRotAlpha (RotationMatrix3d R1, RotationMatrix3d R0) {
      double m00 = R0.m00;
      double m10 = R0.m10;
      double m20 = R0.m20;

      double m01 = R0.m01;
      double m11 = R0.m11;
      double m21 = R0.m21;

      R1.m02 = R0.m02;
      R1.m12 = R0.m12;
      R1.m22 = R0.m22;

      R1.m00 = myCosA*m00 - mySinA*m01;
      R1.m10 = myCosA*m10 - mySinA*m11;
      R1.m20 = myCosA*m20 - mySinA*m21;

      R1.m01 = mySinA*m00 + myCosA*m01;
      R1.m11 = mySinA*m10 + myCosA*m11;
      R1.m21 = mySinA*m20 + myCosA*m21;
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      Vector3d p = new Vector3d();
      Vector3d n = new Vector3d();
      Vector3d zdir = new Vector3d();
      Vector3d xdir = new Vector3d();

      double dist = QuadraticUtils.nearestPointEllipsoid (
         p, myA, myB, myC, TCD.p);
      TGD.p.set (p);

      n.x = p.x/myA;
      n.y = p.y/myB;
      n.z = p.z/myC;

      double c2 = Math.sqrt(n.y*n.y + n.z*n.z);

      // align z axis with surface normal
      if (c2 <= EPS) {
         zdir.set (n.x, 0, 0);
         if (n.x > 0) {
            xdir.set (0, 0, -1);
         }
         else {
            xdir.set (0, 0, 1);
         }
      }
      else if (myUseOpenSimApprox) {
         // use n as an approximate normal
         zdir.x = n.x;
         zdir.y = n.y;
         zdir.z = n.z;

         xdir.x = c2;
         xdir.y = -p.x*p.y/(c2*myA*myB);
         xdir.z = -p.x*p.z/(c2*myA*myC);
      }
      else {
         zdir.x = n.x/myA;
         zdir.y = n.y/myB;
         zdir.z = n.z/myC;

         xdir.x = myA*c2;
         xdir.y = -p.x*p.y/(c2*myA);
         xdir.z = -p.x*p.z/(c2*myA);
      }

      RotationMatrix3d R2D = new RotationMatrix3d();
      R2D.setZXDirections (zdir, xdir);
      RotationMatrix3d RC2 = new RotationMatrix3d();
      RC2.mulInverseLeft (R2D, TCD.R);
      // post mult R2D by RotZ(alpha) so that y corrseponds to the pitch axis
      RC2.mulRotZ (myCosA, mySinA);
      // project the pitch axis (y axis) onto the x-y plane
      double yx = RC2.m01;
      double yy = RC2.m11;
      double yz = RC2.m21;
      double r = Math.sqrt(yx*yx+yy*yy); // length of projection into x/y plane
      RotationMatrix3d RP = new RotationMatrix3d();
      if (r == 0) {
         // unlikely to happen. Just rotate about x by PI/2
         RP.setRotX (Math.PI/2);
      }
      else {
         double ang = Math.atan2 (yz, r);
         Vector3d axis = new Vector3d (yy, -yx, 0);
         RP.setAxisAngle (new AxisAngle (axis, -ang));
      }  
      RC2.mul (RP, RC2);
      // post mult R2D by RotZ(-alpha) 
      RC2.mulRotZ (myCosA, -mySinA);
      TGD.R.mul (R2D, RC2);
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   @Override
   public void initializeConstraints () {
      
      // regular constraints
      addConstraint (BILATERAL|COUPLED);
      addConstraint (BILATERAL|COUPLED); 
      // coordinate limit constraints
      addConstraint (ROTARY);
      addConstraint (ROTARY);
      addConstraint (ROTARY);
      addConstraint (ROTARY);

      addCoordinate ("x", -INF, INF, 0, getConstraint(2));
      addCoordinate ("y", -INF, INF, 0, getConstraint(3));
      addCoordinate ("theta", -INF, INF, 0, getConstraint(4));
      addCoordinate ("phi", -INF, INF, 0, getConstraint(5));
      
      // initializations for temporary variables used in updateConstraints

      int numc = myCoordinates.size ();
      myQRD = new QRDecomposition (); 
      myQ = new MatrixNd (6, 6);
      myG = new Wrench[numc];
      for (int i=0; i<numc; i++) {
         myG[i] = new Wrench();
      }
   }

   /**
    * Computes and returns a matrix to project vectors onto the space
    * perpendicular to the unit vector of vec.
    */
   Matrix3d computeUnitPerpProjector (Vector3d vec) {
      double mag = vec.norm();
      Vector3d uvec = new Vector3d();
      uvec.scale (1/mag, vec);
      Matrix3d P = new Matrix3d();
      P.outerProduct (uvec, uvec);
      P.m00 -= 1;
      P.m11 -= 1;
      P.m22 -= 1;
      P.scale (-1/mag);
      return P;
   }

   /**
    * Sets a column entry of the rotational Jacobian. Entries are given
    * in frame 2, and then converted to frame C using the inverse of RC2.
    */
   private void setRotationalJacobianCol (
      MatrixNd J, RotationMatrix3d RC2, int j,
      double x, double y, double z) {

      Vector3d col = new Vector3d (x, y, z);
      col.inverseTransform (RC2);
      J.set (3, j, col.x);
      J.set (4, j, col.y);
      J.set (5, j, col.z);
   }

   /**
    * Computes the coordinate Jacobian. This is a 6 x numc matrix that maps
    * coordinate speeds onto spatial velocities in the C frame.
    */
   public MatrixNd computeCoordinateJacobian (RigidTransform3d TGD) {
      int numc = numCoordinates();
      MatrixNd J = new MatrixNd (6, numc);

      VectorNd coords = new VectorNd (4);
      TCDToCoordinates (coords, TGD);
      double x = coords.get (X_IDX);
      double y = coords.get (Y_IDX);
      double theta = coords.get (THETA_IDX);
      double phi = coords.get (PHI_IDX);
      double c1 = Math.cos (x);
      double s1 = Math.sin (x);
      double c2 = Math.cos (y);
      double s2 = Math.sin (y);
      double c3 = Math.cos (theta);
      double s3 = Math.sin (theta);

      // Set the translational Jacobian, which depends only on the
      // first two angles.

      Vector3d dpd1 = new Vector3d (0, -myB*c1*c2, -myC*s1*c2);
      Vector3d dpd2 = new Vector3d (myA*c2, myB*s1*s2, -myC*c1*s2);

      dpd1.inverseTransform (TGD.R);
      dpd2.inverseTransform (TGD.R);

      J.set (0, 0, dpd1.x);
      J.set (1, 0, dpd1.y);
      J.set (2, 0, dpd1.z);

      J.set (0, 1, dpd2.x);
      J.set (1, 1, dpd2.y);
      J.set (2, 1, dpd2.z);

      // transforms from frame C to frame 2
      RotationMatrix3d RC2 = new RotationMatrix3d();
      RC2.setRotZ (theta+myAlpha);
      RC2.mulRotY (phi);
      RC2.mulRotZ (myCosA, -mySinA);

      // Set the first two columns of the rotational Jacobian

      if (myUseOpenSimApprox) {
         // First three columns of rotational Jacobian are quite simple, and
         // are the same as for an XYZ GimbalCoupling
 
         setRotationalJacobianCol (
            J, RC2, 0, 
            c2, 0, s2);

         setRotationalJacobianCol (
            J, RC2, 1, 
            0, 1, 0);

         // setRotationalJacobianCol (
         //    J, RC2, 1,
         //    -ydir.dot (dzd2),
         //    xdir.dot (dzd2),
         //    ydir.dot (dxd2));
        
         // J.set (3, 0, c2*c3);
         // J.set (4, 0, -c2*s3);
         // J.set (5, 0, s2);

         // J.set (3, 1, s3);
         // J.set (4, 1, c3);
         // J.set (5, 1, 0);

         // last 2 columns set below since they are unaffected by OpenSim
         // approximation
      }
      else {
         // for simplicity, do the rotation jacobian calculations for x and y
         // in frame 2, which is the intermediate frame on the ellipsoid
         // surface after x and y have been applied but before the theta
         // rotation. The orientation of this frame is given by R2.
         RotationMatrix3d R2 = new RotationMatrix3d();
         R2.mulInverseRight (TGD.R, RC2);

         // extract the x and y direction vectors from R2
         Vector3d xdir = new Vector3d();
         Vector3d ydir = new Vector3d();
         R2.getColumn (0, xdir);
         R2.getColumn (1, ydir);

         // We know that the x and z directions of the R2 are given by
         // normalizing the vectors
         // 
         // xvec = (A*c2, B*s1*s2, -C*c1*s2)'
         //
         // zvec = (s2/A, -s1*c2/B, c1*c2/C)'
         //
         // such that
         //
         // xdir = xvec/||xvec||  and  zdir = zvec/||zvec||
         //
         // Then, if dxdj and dzdj are the derivatives of xdir and zdir with
         // respect to joint coordinate j, it is possible to show that the
         // angular velocity (in frame 2) imparted by j is:
         //
         // [ wx ] = [ -ydir . dzdj ]
         // [ wy ] = [  xdir . dzdj ] dotj
         // [ wz ] = [  ydir . dxdj ]
         //
         // where dotj is the coordinate speed of j. The column of R2
         // corresponding to j is then the right side vector.
         
         // To compute dxdj and dzdj, we take the derivatives of xvec and zvec
         // with respect to j, and then use the following formula for
         // differentiating a unit vector u that is formed from a non-unit
         // vector v::
         //
         // dudj = PU vdj, where PU = (I - u*u^T)/||v||
         //
         // We call PU the unit perpendicular projector for the vector v.
         //

         // compute unit perpendicular projectors PX and PZ for xvec and zvec:
         Matrix3d PX =
            computeUnitPerpProjector (
               new Vector3d (myA*c2, myB*s1*s2, -myC*c1*s2));
         Matrix3d PZ =
            computeUnitPerpProjector (
               new Vector3d (s2/myA, -s1*c2/myB, c1*c2/myC));

         // compute the derivatives of xdir with respect to coordinates 1 and
         // 2, by multiplying the derivatives of xvec and zvec by PX
         Vector3d dxd1 = new Vector3d(0, myB*s2*c1, myC*s1*s2);
         PX.mul (dxd1, dxd1);
         Vector3d dxd2 = new Vector3d(-myA*s2, myB*s1*c2, -myC*c1*c2);
         PX.mul (dxd2, dxd2);

         // compute the derivatives of zdir with respect to coordinates 1 and 2
         Vector3d dzd1 = new Vector3d(0, -c1*c2/myB, -s1*c2/myC);
         PZ.mul (dzd1, dzd1);
         Vector3d dzd2 = new Vector3d(c2/myA, s1*s2/myB, -c1*s2/myC);
         PZ.mul (dzd2, dzd2);

         // compute the first two columns of JR, corresponding to dot1 and
         // dot2, using the angular velocity formula given above:

         setRotationalJacobianCol (
            J, RC2, 0, 
            -ydir.dot (dzd1),
            xdir.dot (dzd1),
            ydir.dot (dxd1));

         setRotationalJacobianCol (
            J, RC2, 1,
            -ydir.dot (dzd2),
            xdir.dot (dzd2),
            ydir.dot (dxd2));

      }

      // Set the last two columns of the rotational Jacobian. Third column is
      // (0, 0, 1), since dot3 imparts an angular velocity about z in frame 2.

      setRotationalJacobianCol (
         J, RC2, 2,
         0, 0, 1);

      if (myAlpha == 0) {
         setRotationalJacobianCol (
            J, RC2, 3,
            -s3, c3, 0);
      }
      else {
         setRotationalJacobianCol (
            J, RC2, 3,
            -c3*mySinA - s3*myCosA, 
            -s3*mySinA + c3*myCosA,
            0);
      }
      return J;
   }

   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
      Twist velGD, boolean updateEngaged) {
      
      // from OpenSimCustomJoint

      int numc = numCoordinates();
      VectorNd gcol = new VectorNd(numc);

      MatrixNd C = computeCoordinateJacobian (TGD);
      
      // QR decomposition to compute pseudoinverse A* = inv(R) Q^T
      myQRD.factorWithPivoting (C);
      int[] perm = new int[numc];
      myQRD.get (myQ, null, perm);
      if (myQRD.rank (1e-8) < numc) {
         System.out.println (
            "WARNING: joint has rank "+myQRD.rank(1e-8)+" vs. " + numc);
         System.out.println ("coupling=" + this);
      }
      for (int i=0; i<6; i++) {
         for (int j=0; j<numc; j++) {
            gcol.set (j, myQ.get(i, j));
         }
         myQRD.solveR (gcol, gcol);
         for (int j=0; j<numc; j++) {
            myG[j].set (i, gcol.get(j));
         }
      }
      // set joint coordinate wrenches from myG
      for (int j=0; j<numc; j++) {
         RigidBodyConstraint con = myCoordinates.get (j).limitConstraint;
         con.wrenchG.set (myG[j]);
         con.dotWrenchG.setZero ();
      }      
      // non-coordinate constraints are given by the orthogonal complement
      // of H, which is given by the last numc-6 columns of Q
      Wrench wr = new Wrench();
      for (int i=0; i<6-numc; i++) {
         RigidBodyConstraint cons = getConstraint(i);
         myQ.getColumn (numc+i, wr);
         cons.setWrenchG (wr);
      }
   } 
 
   private void setToNearestAngle (VectorNd coords, int idx, double ang) {
      coords.set (idx, getCoordinateInfo(idx).nearestAngle(ang));
   }

   public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {

      Vector3d p = new Vector3d();
      Vector3d n = new Vector3d();

      // compute n from Seth eq (6), note this is not the surface normal
      p.set (TCD.p);
      n.x = p.x/myA;
      n.y = p.y/myB;
      n.z = p.z/myC;
      
      double x, y;
      double c2 = Math.sqrt(n.y*n.y + n.z*n.z);
      double s2 = n.x;

      y = Math.atan2 (s2, c2);
      if (Math.abs(n.y) < EPS && Math.abs(n.z) < EPS) {
         x = 0;
      }
      else {
         x = Math.atan2 (-n.y, n.z);
      }
      double c1 = Math.cos (x);
      double s1 = Math.sin (x);
      
      setToNearestAngle (coords, X_IDX, x);
      setToNearestAngle (coords, Y_IDX, y);

      Vector3d zdir = new Vector3d();
      Vector3d xdir = new Vector3d();

      if (myUseOpenSimApprox) {
         // set z direction to an approximation of the surface normal
         zdir.x = p.x/myA;
         zdir.y = p.y/myB;
         zdir.z = p.z/myC;

         // set x direction to that imparted by the first two joints of an XYZ
         // GimbalCoupling
         xdir.x = c2;
         xdir.y = s1*s2;
         xdir.z = -c1*s2;
      }
      else {
         // scale n to get surface normal direction at p
         zdir.x = n.x/myA; // nrml.x = p.x/(a*a)
         zdir.y = n.y/myB; // nrml.y = p.y/(b*b)
         zdir.z = n.z/myC; // nrml.z = p.z/(c*c)
            
         xdir.x = myA*c2;
         xdir.y = myB*s1*s2;
         xdir.z = -myC*c1*s2;
      }
      RotationMatrix3d RC2 = new RotationMatrix3d();
      RC2.setZXDirections (zdir, xdir);
      RC2.mulInverseLeft (RC2, TCD.R);
      if (myAlpha != 0) {
         RC2.mulRotZ (myCosA, mySinA);
      }
      double theta = Math.atan2 (-RC2.m01, RC2.m11) - myAlpha;
      double phi = Math.atan2 (-RC2.m20, RC2.m22);
      setToNearestAngle (coords, THETA_IDX, theta);
      setToNearestAngle (coords, PHI_IDX, phi);
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double x, double y, double theta, double phi) {

      Vector3d p = new Vector3d();
      Vector3d zdir = new Vector3d();
      Vector3d xdir = new Vector3d();
      
      double c1 = Math.cos (x);
      double s1 = Math.sin (x);
      double c2 = Math.cos (y);
      double s2 = Math.sin (y);
      
      p.x = myA*s2;
      p.y = -myB*s1*c2;
      p.z = myC*c1*c2;

      TCD.p.set (p);

      if (myUseOpenSimApprox) {
         // Note: this is equivalent to coordinatesToTCD for an XYZ
         // GimbalCoupling

         // set z direction to an approximation of the surface normal
         zdir.x = p.x/myA;
         zdir.y = p.y/myB;
         zdir.z = p.z/myC;

         // set x direction to that imparted by the first two joints of an XYZ
         // GimbalCoupling
         xdir.x = c2;
         xdir.y = s1*s2;
         xdir.z = -c1*s2;
      }
      else {
         // set z direction to the ellipsoid surface normal at p
         zdir.x = p.x/(myA*myA);
         zdir.y = p.y/(myB*myB);
         zdir.z = p.z/(myC*myC);

         // set x direction to the surface tangent direction imparted by y
         xdir.x = myA*c2;
         xdir.y = myB*s1*s2;
         xdir.z = -myC*c1*s2;
      }
      RotationMatrix3d R = new RotationMatrix3d();
      R.setZXDirections (zdir, xdir);
      R.mulRotZ (theta + myAlpha);
      R.mulRotY (phi);
      if (myAlpha != 0) {
         R.mulRotZ (myCosA, -mySinA);         
      }
      TCD.R.set (R);
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {
      coordinatesToTCD (
         TCD, coords.get(0), coords.get(1), coords.get(2), coords.get(3));
   }

}
