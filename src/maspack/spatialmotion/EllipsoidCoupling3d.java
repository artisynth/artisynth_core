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

/** 
 * Constraints a rigid body to 2D motion (with rotation) on the surface of an
 * ellipsoid.
 */
public class EllipsoidCoupling3d extends RigidBodyCoupling {

   public static final int X_IDX = 0;
   public static final int Y_IDX = 1;
   public static final int THETA_IDX = 2;

   // OpenSim approximation for ellipsoid joints: assumes orientation is given
   // directy by the x, y, theta angles. This simplifies the calculations but
   // means that the z axis of C is generally not parallel to the ellipsoid
   // surface normal.
   public boolean myUseOpenSimApprox = true;

   static double EPS = 2e-15;
   
   // ellipsoid radii
   private double myA, myB, myC;
   
   protected QRDecomposition myQRD; 
   protected MatrixNd myQ;
   Wrench[] myG;

   private static final double INF = Double.POSITIVE_INFINITY; 

   public EllipsoidCoupling3d () {
      this (1.0, 1.0, 1.0, false);
   }

   public EllipsoidCoupling3d (double a, double b, double c) {
      this (a, b, c, false);
   }

   public EllipsoidCoupling3d (
      double a, double b, double c, boolean useOpenSimApprox) {

      super();
      this.myA = a;
      this.myB = b;
      this.myC = c;
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

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {

      TGD.set (TCD);
      Vector3d p = new Vector3d();
      Vector3d n = new Vector3d();

      double dist = QuadraticUtils.nearestPointEllipsoid (
         p, myA, myB, myC, TGD.p);
      TGD.p.set (p);

      // align z axis with surface normal
      if (!myUseOpenSimApprox) {
         n.x = p.x/(myA*myA);
         n.y = p.y/(myB*myB);
         n.z = p.z/(myC*myC);
      }
      else {
         // normal is approximated
         n.x = p.x/myA;
         n.y = p.y/myB;
         n.z = p.z/myC;
      }
      n.normalize();
      TGD.R.rotateZDirection (n);
      if (coords != null) {
         TCDToCoordinates (coords, TGD);
      }     
   }

   @Override
   public void initializeConstraints () {
      
      // regular constraints
      addConstraint (BILATERAL|COUPLED);
      addConstraint (BILATERAL|COUPLED); 
      addConstraint (BILATERAL|COUPLED);
      // coordinate limit constraints
      addConstraint (ROTARY);
      addConstraint (ROTARY);
      addConstraint (ROTARY);

      addCoordinate (-INF, INF, 0, getConstraint(3));
      addCoordinate (-INF, INF, 0, getConstraint(4));
      addCoordinate (-INF, INF, 0, getConstraint(5));
      
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
    * Computes the coordinate Jacobian. This is a 6 x numc matrix that maps
    * coordinate speeds onto spatial velocities in the C frame.
    */
   public MatrixNd computeCoordinateJacobian (RigidTransform3d TGD) {
      int numc = numCoordinates();
      MatrixNd J = new MatrixNd (6, numc);

      VectorNd coords = new VectorNd (3);
      TCDToCoordinates (coords, TGD);
      double x = coords.get (X_IDX);
      double y = coords.get (Y_IDX);
      double theta = coords.get (THETA_IDX);
      double c1 = Math.cos (x);
      double s1 = Math.sin (x);
      double c2 = Math.cos (y);
      double s2 = Math.sin (y);
      double c3 = Math.cos (theta);
      double s3 = Math.sin (theta);

      // rotation Jacobian - maps coordinate speeds onto angular velocities in
      // the C frame
      Matrix3d JR = new Matrix3d();

      if (myUseOpenSimApprox) {
         // rotational Jacobian is quite simple, and is the same as for
         // an XYZ GimbalCoupling
         JR.m00 = c2*c3;
         JR.m10 = -c2*s3;
         JR.m20 = s2;
     
         JR.m01 = s3;
         JR.m11 = c3;
         JR.m21 = 0;
     
         JR.m02 = 0;
         JR.m12 = 0;
         JR.m22 = 1;
      }
      else {
         RotationMatrix3d RC2 = new RotationMatrix3d();
         RC2.setRotZ (theta); // transforms from last frame 3 to frame 2
         
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

         JR.m00 = -ydir.dot (dzd1);
         JR.m10 =  xdir.dot (dzd1);
         JR.m20 =  ydir.dot (dxd1);

         JR.m01 = -ydir.dot (dzd2);
         JR.m11 =  xdir.dot (dzd2);
         JR.m21 =  ydir.dot (dxd2);

         // The last column is (0, 0, 1), since dot3 imparts an angular
         // velocity about z in frame 2.

         JR.m02 = 0;
         JR.m12 = 0;
         JR.m22 = 1;

         // transform J2 from frame 2 to frame C
         JR.mulTransposeLeft (RC2, JR);
      }

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

      J.setSubMatrix (3, 0, JR);

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
      
      double x, y, theta;
      double c2 = Math.sqrt(n.y*n.y + n.z*n.z);
      double s2 = n.x;

      y = Math.atan2 (s2, c2);
      if (c2 < EPS) {
         x = 0;
      }
      else {
         x = Math.atan2 (-n.y, n.z);
      }
      double c1 = Math.cos (x);
      double s1 = Math.sin (x);
      
      setToNearestAngle (coords, X_IDX, x);
      setToNearestAngle (coords, Y_IDX, y);

      if (myUseOpenSimApprox) {
         RotationMatrix3d R = TCD.R;
         theta = Math.atan2 (c1*R.m10 + s1*R.m20, c1*R.m11 + s1*R.m21);
      }
      else {
         Vector3d zdir = new Vector3d();
         Vector3d xdir = new Vector3d();
      
         // scale n to get surface normal direction at p
         zdir.x = n.x/myA; // nrml.x = p.x/(a*a)
         zdir.y = n.y/myB; // nrml.y = p.y/(b*b)
         zdir.z = n.z/myC; // nrml.z = p.z/(c*c)
            
         xdir.x = myA*c2;
         xdir.y = myB*s1*s2;
         xdir.z = -myC*c1*s2;
            
         RotationMatrix3d R = new RotationMatrix3d();
         R.setZXDirections (zdir, xdir);
         R.mulInverseLeft (R, TCD.R);

         theta = Math.atan2 (R.m10, R.m00);
      }
      setToNearestAngle (coords, THETA_IDX, theta);
   }

   public void coordinatesToTCD (
      RigidTransform3d TCD, double x, double y, double theta) {

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
      TCD.R.setZXDirections (zdir, xdir);
      TCD.R.mulRotZ (theta);

      /**
         checking way to compute x without s1 and c1

      Vector3d xchk = new Vector3d();
      Vector3d xraw = new Vector3d();
      double c2x = Math.sqrt (sqr(p.y/myB) + sqr(p.z/myC));
      if (c2x <= EPS) {
         xchk.set (0, 0, -1);
         xraw.set (xchk);
      }
      else {
         if (myUseOpenSimApprox) {
            xchk.set (c2x, -p.x*p.y/(c2x*myA*myB), -p.x*p.z/(c2x*myA*myC));
         }
         else {
            xchk.set (myA*c2x, -p.x*p.y/(c2x*myA), -p.x*p.z/(c2x*myA));
         }
         xraw.set (xchk);
         xchk.normalize();
      }
      xdir.normalize();
      if (xdir.distance (xchk) > maxXerr) {
         maxXerr = xdir.distance (xchk);
         System.out.println ("maxXerr=" + maxXerr);
      }
      */
   }

   static double maxXerr = 0;

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {
      coordinatesToTCD (TCD, coords.get(0), coords.get(1), coords.get(2));
   }

}
