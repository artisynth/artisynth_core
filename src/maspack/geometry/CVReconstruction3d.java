package maspack.geometry;

import java.util.Collection;
import java.util.Iterator;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x4;
import maspack.matrix.Matrix4d;
import maspack.matrix.Matrix4x3;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.matrix.VectorNd;

/**
 * Computer-Vision 3D reconstruction methods
 */
public class CVReconstruction3d {
   
   //   /**
   //    * Scales G to best match uniform scaling of F
   //    * @param F comparison matrix (unmodified)
   //    * @param G target matrix (modified)
   //    */
   //   private static void rescale(Matrix3d F, Matrix3d G) {
   //      double s = (  F.m00*G.m00 + F.m01*G.m01 + F.m02*G.m02
   //      + F.m10*G.m10 + F.m11*G.m11 + F.m12*G.m12
   //      + F.m20*G.m20 + F.m21*G.m21 + F.m22*G.m22) / G.frobeniusNormSquared ();
   //      G.scale(s);
   //   }
   
   /**
    * Determines the camera's intrinsic calibration matrix
    * @param f focal distance
    * @param c principal point (center, optional - defaults to zero)
    * @return intrinsic calibration matrix
    */
   public static Matrix3d computeCameraCalibration(double f, Point2d c) {
      if (c == null) {
         c = Point2d.ZERO;
      }
      Matrix3d K = new Matrix3d(f, 0, c.x, 0, f, c.y, 0, 0, 1);
      return K;
   }
   
   /**
    * Transforms an intrinsic camera calibration matrix to a full camera matrix P 
    * @param K intrinsic camera calibration
    * @param T extrinsic camera calibration
    * @return full camera matrix
    */
   public static Matrix3x4 computeCameraMatrix(Matrix3d K, RigidTransform3d T) {
      
      Vector3d d = new Vector3d();
      Matrix3d A = K;
      
      // transform
      if (T != null) {
         d.mul (K, T.p);
         A = new Matrix3d();
         A.mul (K, T.R);
      }
      
      Matrix3x4 P = new Matrix3x4();
      P.setSubMatrix (0, 0, A);
      P.setColumn (3, d);
      return P;
   }
   
   /**
    * Computes a camera matrix
    * @param f uniform focal distance
    * @param c principal point (center, optional - defaults to zero)
    * @param T camera-to-world transform (optional - defaults to identity)
    * @return full camera matrix
    */
   public static Matrix3x4 computeCameraMatrix(double f, Point2d c, RigidTransform3d T) {
      
      if (c == null) {
         c = Point2d.ZERO;
      }
      
      // camera calibration matrix
      Matrix3d K = computeCameraCalibration(f, c);
      Vector3d d = new Vector3d();
      
      // transform
      if (T != null) {
         d.mul (K, T.p);
         K.mul (T.R, K);
      }
      
      Matrix3x4 P = new Matrix3x4();
      P.setSubMatrix (0, 0, K);
      P.setColumn (3, d);
      
      return P;
   }
   
   /**
    * Compute fundamental matrix from two general camera matrices
    * @param P1 camera matrix 1
    * @param P2 camera matrix 2
    * @return fundamental matrix
    */
   public static Matrix3d computeFundamentalMatrix(Matrix3x4 P1, Matrix3x4 P2) {
      
      SVDecomposition svd = new SVDecomposition();
      svd.factor (P1, SVDecomposition.FULL_UV);
      
      // camera location 
      VectorNd C = new VectorNd(4);
      svd.getV ().getColumn (3, C);
      
      // epipolar constraint
      VectorNd e2 = new VectorNd(3);
      P2.mul (e2, C);
      
      // pseudo-invert P
      Matrix4x3 P1inv = new Matrix4x3();
      svd.pseudoInverse (P1inv);
      
      // fundamental matrix
      MatrixNd PP = new MatrixNd();
      // e2 cross
      Matrix3d F = new Matrix3d(0, -e2.get(2), e2.get(1), e2.get(2), 0, -e2.get(0), -e2.get(1), e2.get(0), 0 );
      PP.mul (F, P2);
      PP.mul (PP, P1inv);
      F.set (PP);
      
      return F;
   }
   
   /**
    * Computes the essential matrix from intrinsic camera calibration and fundamental matrix
    * @param F fundamental matrix between two cameras
    * @param K1 camera 1 intrinsic calibration matrix
    * @param K2 camera 2 intrinsic calibration matrix
    * @return essential matrix
    */
   public static Matrix3d computeEssentialMatrix(Matrix3d F, Matrix3d K1, Matrix3d K2) {
      Matrix3d E = new Matrix3d();
      E.mulTransposeLeft (K2,  F);
      E.mul (K1);
      return E;
   }
   
   /**
    * Computes the essential matrix from extrinsic camera calibration
    * @param T1 camera 1 world transform
    * @param T2 camera 2 world transform
    * @return essential matrix
    */
   public static Matrix3d computeEssentialMatrix(RigidTransform3d T1, RigidTransform3d T2) {
      // relative transform
      RigidTransform3d trans = new RigidTransform3d();
      trans.mulInverseRight (T2, T1);
     
      Matrix3d E = new Matrix3d ();
      E.setSkewSymmetric (trans.p);
      E.mul (trans.R);
      
      return E;
      
   }
   
   /**
    * Computes the relative transform between two cameras by decomposing the essential matrix. 
    * See Zisserman and Hartley, 2004, pg 257-259.
    * 
    * @param F fundamental matrix
    * @param K1 camera 1 intrinsic calibration matrix
    * @param K2 camera 2 intrinsic calibration matrix
    * @return all four possible relative transforms
    */
   public static RigidTransform3d[] computeRelativeTransform(Matrix3d F, Matrix3d K1, Matrix3d K2) {
      
      // determine essential matrix from fundamental
      Matrix3d E = computeEssentialMatrix(F, K1, K2);
      
      // Correct E with constraint that it has duplicate singular value
      SVDecomposition3d svd = new SVDecomposition3d (E);
      Vector3d s = new Vector3d();
      svd.getS (s);
      double d = (s.x + s.y)/2;  // mid-point singular value
      Matrix3d U = svd.getU ();
      Matrix3d V = svd.getV ();
      
      // construct all possible transform solutions
      return constructEssentialTransforms (U, V);
   }
   
   /**
    * Compute fundamental matrix from a set of corresponding 2D points
    * @param p1 set of points in camera 1
    * @param p2 set of points in camera 2
    * @return fundamental matrix
    */
   public static Matrix3d linearNormalizedEightPointAlgorithm(Collection<? extends Point2d> p1, Collection<? extends Point2d> p2) {
      
      int n = p1.size ();
      
      // normalization transforms
      // points X
      Point2d c1 = new Point2d();
      for (Point2d p : p1) {
         c1.add (p);
      }
      c1.scale (1.0/n);
      double s1 = 0;
      for (Point2d p : p1) {
         s1 += p.distance (c1);
      }
      s1 = Math.sqrt (2) * n / s1;
      Matrix3d T1 = new Matrix3d(s1, 0, -s1*c1.x, 0, s1, -s1*c1.y, 0, 0, 1);
      
      // points X'
      Point2d c2 = new Point2d();
      for (Point2d p : p2) {
         c2.add (p);
      }
      c2.scale (1.0/n);
      double s2 = 0;
      for (Point2d p : p2) {
         s2 += p.distance (c2);
      }
      s2 = Math.sqrt (2) * n / s2;
      Matrix3d T2 = new Matrix3d(s2, 0, -s2*c2.x, 0, s2, -s2*c2.y, 0, 0, 1);
      
      // build constraint matrix
      MatrixNd A = new MatrixNd(n, 9);
      Iterator<? extends Point2d> p1it = p1.iterator ();
      Iterator<? extends Point2d> p2it = p2.iterator ();
      Point3d x1 = new Point3d();
      Point3d x2 = new Point3d();
      for (int i=0; i<n; ++i) {
         Point2d y1 = p1it.next ();
         Point2d y2 = p2it.next ();
         
         // transform
         x1.set(y1.x, y1.y, 1);
         T1.mul (x1);
         x2.set(y2.x, y2.y, 1);
         T2.mul (x2);
         A.set (i, 0, x2.x*x1.x);
         A.set (i, 1, x2.x*x1.y);
         A.set (i, 2, x2.x*x1.z);
         A.set (i, 3, x2.y*x1.x);
         A.set (i, 4, x2.y*x1.y);
         A.set (i, 5, x2.y*x1.z);
         A.set (i, 6, x2.z*x1.x);
         A.set (i, 7, x2.z*x1.y);
         A.set (i, 8, x2.z*x1.z);
      }
      
      // least-squares solution f
      MatrixNd B = new MatrixNd(9, 9);
      B.mulTransposeLeft (A, A);
      VectorNd f = new VectorNd(9);
      SVDecomposition svd = new SVDecomposition (B);
      VectorNd s = svd.getS ();
      MatrixNd V = svd.getV ();
      V.getColumn (8, f);
      
      // extract fundamental matrix
      Matrix3d F = new Matrix3d();
      F.m00 = f.get (0);
      F.m01 = f.get (1);
      F.m02 = f.get (2);
      F.m10 = f.get (3);
      F.m11 = f.get (4);
      F.m12 = f.get (5);
      F.m20 = f.get (6);
      F.m21 = f.get (7);
      F.m22 = f.get (8);
      
      //      // check validity
      //      VectorNd r = new VectorNd(9);
      //      B.mul (r, f);
      //      if (r.infinityNorm () > 1e-10*B.frobeniusNorm ()) {
      //         System.err.println ("Error!  Not a good nullspace");
      //      }
      //      
      //      A.mul (r, f);
      //      if (r.infinityNorm () > 1e-10*A.frobeniusNorm ()) {
      //         System.err.println ("Error!  Not a good nullspace");
      //      }
      
      // fundamental matrix singularity correction
      SVDecomposition3d svd3 = new SVDecomposition3d (F);
      Matrix3d U3 = svd3.getU ();
      Matrix3d V3 = svd3.getV ();
      Vector3d s3 = new Vector3d();
      svd3.getS (s3);
      s3.z = 0;
      Matrix3d D = new Matrix3d(s3.x, 0, 0, 0, s3.y, 0, 0, 0, 0);
      Matrix3d Fhat = new Matrix3d();
      Fhat.set(U3);
      Fhat.mul (D);
      Fhat.mulTranspose (V3);
      
      // un-normalize
      Fhat.mul(T1);
      Fhat.mulTransposeLeft(T2, Fhat);
      
      // scale to have unit Frobenius norm
      Fhat.scale(1.0/Fhat.frobeniusNorm ());
      
      return Fhat;
   }
   
   public static Vector4d homogeneousReconstruction(Matrix3x4 P1, Matrix3x4 P2, Point2d p1, Point2d p2) {
      Matrix4d C = new Matrix4d();
      Vector4d X = new Vector4d();
      
      // homogeneous reconstruction
      C.m00 = p1.x*P1.m20 - P1.m00;
      C.m01 = p1.x*P1.m21 - P1.m01;
      C.m02 = p1.x*P1.m22 - P1.m02;
      C.m03 = p1.x*P1.m23 - P1.m03;
      
      C.m10 = p1.y*P1.m20 - P1.m10;
      C.m11 = p1.y*P1.m21 - P1.m11;
      C.m12 = p1.y*P1.m22 - P1.m12;
      C.m13 = p1.y*P1.m23 - P1.m13;
      
      C.m20 = p2.x*P2.m20 - P2.m00;
      C.m21 = p2.x*P2.m21 - P2.m01;
      C.m22 = p2.x*P2.m22 - P2.m02;
      C.m23 = p2.x*P2.m23 - P2.m03;
      
      C.m30 = p2.y*P2.m20 - P2.m10;
      C.m31 = p2.y*P2.m21 - P2.m11;
      C.m32 = p2.y*P2.m22 - P2.m12;
      C.m33 = p2.y*P2.m23 - P2.m13;
      
      SVDecomposition svd = new SVDecomposition (C);
      svd.getV ().getColumn (3, X);
      
      // rescale to determine coordinate
      if (X.get (3) != 0) {
         X.scale (1.0 / X.get (3));
      }
      
      return X;
   }
   
   private static RigidTransform3d[] constructEssentialTransforms(Matrix3d U, Matrix3d V) {
      
      // construct all possible transform solutions
      RigidTransform3d[] solutions = new RigidTransform3d[4];
      final Matrix3d W = new Matrix3d(0, -1, 0, 1, 0, 0, 0, 0, 1);
      Matrix3d R = new Matrix3d();
      boolean flip = false;
      
      R.mul (U, W);
      R.mulTranspose (V);
      if (R.determinant () < 0) {
         flip = true;
         R.scale (-1);
      }

      RigidTransform3d T1 = new RigidTransform3d();
      T1.R.set (R);
      U.getColumn (2, T1.p);
      solutions[0] = T1;

      // solution 2, flip translation
      solutions[1] = new RigidTransform3d(T1);
      solutions[1].p.negate ();

      // solution 3
      R.mulTransposeRight(U, W);
      R.mulTranspose (V);
      if (flip) {
         R.scale(-1);
      }
      RigidTransform3d T2 = new RigidTransform3d();
      T2.R.set (R);
      U.getColumn (2, T2.p);
      solutions[2] = T2;

      // solution 4, flip translation
      solutions[3] = new RigidTransform3d(T2);
      solutions[3].p.negate ();
      
      return solutions;
   }

   /**
    * Performs the eight-point algorithm for determining the essential matrix and recovering the extrinsic
    * camera transform.
    * 
    * @param p1 set of points in camera 1
    * @param p2 set of points in camera 2
    * @param K1 intrinsic camera calibration for camera 1
    * @param K2 intrinsic camera calibration for camera 2
    * @return extrinsic camera calibration matrix for camera 2 relative to camera 1
    */
   public static RigidTransform3d linearEightPointAlgorithm(
      Collection<? extends Point2d> p1, Collection<? extends Point2d> p2,
      Matrix3d K1, Matrix3d K2) {
      
      Matrix3d K1inv = new Matrix3d();
      Matrix3d K2inv = new Matrix3d();
      K1inv.fastInvert (K1);
      K2inv.fastInvert (K2);

      int n = p1.size ();
      Point3d x = new Point3d();
      
      // build constraint matrix
      MatrixNd A = new MatrixNd(n, 9);
      Iterator<? extends Point2d> p1it = p1.iterator ();
      Iterator<? extends Point2d> p2it = p2.iterator ();
      Point3d x1 = new Point3d();
      Point3d x2 = new Point3d();
      for (int i=0; i<n; ++i) {
         Point2d y1 = p1it.next ();
         Point2d y2 = p2it.next ();
         x1.set(y1.x, y1.y, 1);
         x2.set(y2.x, y2.y, 1);
         
         K1inv.mul (x1);
         K2inv.mul (x2);
         
         A.set (i, 0, x2.x*x1.x);
         A.set (i, 1, x2.x*x1.y);
         A.set (i, 2, x2.x*x1.z);
         A.set (i, 3, x2.y*x1.x);
         A.set (i, 4, x2.y*x1.y);
         A.set (i, 5, x2.y*x1.z);
         A.set (i, 6, x2.z*x1.x);
         A.set (i, 7, x2.z*x1.y);
         A.set (i, 8, x2.z*x1.z);
      }
      
      // least-squares solution f
      MatrixNd B = new MatrixNd(9, 9);
      B.mulTransposeLeft (A, A);
      VectorNd e = new VectorNd(9);
      SVDecomposition svd = new SVDecomposition (B);
      VectorNd s = svd.getS ();
      MatrixNd V = svd.getU ();
      V.getColumn (8, e);
      
      // extract essential matrix
      Matrix3d E = new Matrix3d();
      E.m00 = e.get (0);
      E.m01 = e.get (1);
      E.m02 = e.get (2);
      E.m10 = e.get (3);
      E.m11 = e.get (4);
      E.m12 = e.get (5);
      E.m20 = e.get (6);
      E.m21 = e.get (7);
      E.m22 = e.get (8);
      
      // fundamental matrix singularity correction
      SVDecomposition3d svd3 = new SVDecomposition3d (E);
      Matrix3d U3 = svd3.getU ();
      Matrix3d V3 = svd3.getV ();
      Vector3d s3 = new Vector3d();
      svd3.getS (s3);
      s3.z = 0;
      s3.x = (s3.x + s3.y)/2;
      s3.y = s3.x;
    
      // construct all possible transform solutions
      RigidTransform3d[] solutions = constructEssentialTransforms (U3, V3);
      
      // try all four solutions:
      RigidTransform3d solution = null;
      int maxv = 0;  // number of in-view points
      
      // homogeneous reconstruction, count number of in-view points
      Matrix3x4 P1 = computeCameraMatrix (K1, null);
      for (int i=0; i<solutions.length; ++i) {
         Matrix3x4 P2 = computeCameraMatrix (K2, solutions[i]);
         
         // reconstruct point p0
         p1it = p1.iterator ();
         p2it = p2.iterator ();
         
         // iterate through points
         int v = 0;
         while (p1it.hasNext ()) {
            Point2d y1 = p1it.next ();
            Point2d y2 = p2it.next ();
            
            Vector4d X = homogeneousReconstruction (P1, P2, y1, y2);
            
            // rescale to determine coordinate
            if (X.w != 0) {
               x.set(X.x/X.w, X.y/X.w, X.z/X.w);
               
               // determine which side X is on, we want +z for both images
               if (x.z >= 0) {
                  ++v;
               }
               x.transform (solutions[i]);
               if (x.z >= 0) {
                  ++v;
               }
               
               // current best solution
               if (v > maxv) {
                  solution = solutions[i];
                  maxv = v;
               }
            }
         }
         
         // go to next possible solution
      }
      
      return solution;
   }
   
}
