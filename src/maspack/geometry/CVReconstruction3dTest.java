package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.AxisAngle;
import maspack.matrix.DenseMatrix;
import maspack.matrix.ImproperSizeException;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x4;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.util.RandomGenerator;
import maspack.util.TestException;

public class CVReconstruction3dTest {

   /**
    * Computes and estimates fundamental matrix given a set of points
    */
   public static void testFundamentalEstimation() {
      
      int n = 100;
      
      RandomGenerator.setSeed (10);
      
      // 3D points
      ArrayList<Point3d> p3d = new ArrayList<> (n);
      for (int i=0; i<n; ++i) {
         Point3d p = new Point3d();
         p.x = RandomGenerator.nextDouble (-1, 1);
         p.y = RandomGenerator.nextDouble (-1, 1);
         p.z = RandomGenerator.nextDouble (-1, 1);
         p3d.add (p);
      }
      
      // cameras
      RigidTransform3d T1 = new RigidTransform3d(new Point3d(0, 0, 2), AxisAngle.IDENTITY);
      Matrix3x4 P1 = CVReconstruction3d.computeCameraMatrix (3, Point2d.ZERO, T1);
      
      RigidTransform3d T2 = new RigidTransform3d(new Point3d(0, 0, 2), new AxisAngle(0, 1, 0, -Math.PI/2));
      Matrix3x4 P2 = CVReconstruction3d.computeCameraMatrix (3, Point2d.ZERO, T2);
      
      // projections
      Point3d x = new Point3d();
      Vector4d X = new Vector4d();
      
      // image 1
      ArrayList<Point2d> p1 = new ArrayList<Point2d>(n);
      for (Point3d p : p3d) {
         X.set (p.x, p.y, p.z, 1);
         P1.mul (x, X);
         p1.add (new Point2d(x.x/x.z, x.y/x.z));
      }

      // image 2
      ArrayList<Point2d> p2 = new ArrayList<Point2d>(n);
      for (Point3d p : p3d) {
         X.set (p.x, p.y, p.z, 1);
         P2.mul (x, X);
         p2.add (new Point2d(x.x/x.z, x.y/x.z));
      }
      
      // reconstruct
      Matrix3d F = CVReconstruction3d.computeFundamentalMatrix (P1, P2);
      Matrix3d F8 = CVReconstruction3d.linearNormalizedEightPointAlgorithm (p1, p2);
      
      // account for difference in scale
      rescale(F, F8);
      
      // double check condition
      Point3d x1 = new Point3d();
      Point3d x2 = new Point3d();
      Point3d z = new Point3d();
      for (int i=0; i<p1.size (); ++i) {
         Point2d y1 = p1.get (i);
         Point2d y2 = p2.get (i);
         
         x1.set(y1.x, y1.y, 1);
         x2.set(y2.x, y2.y, 1);
         
         F.mul (z, x1);
         double d = z.dot (x2);
         if (Math.abs (d) > 1e-10*F.frobeniusNorm ()) {
            throw new TestException ("F not fundamental");
         }
         
         F8.mul (z, x1);
         d = z.dot (x2);
         if (Math.abs (d) > 1e-10*F.frobeniusNorm ()) {
            throw new TestException ("F8 not fundamental");
         }
      }

      
      if (!F.epsilonEquals (F8, 1e-10)) {
         throw new TestException ("Fundamental matrices do not match!");
      }
      
   }
   
   /**
    * Scales G to best match uniform scaling of F
    * @param F comparison matrix (unmodified)
    * @param G target matrix (modified)
    */
   private static void rescale(Matrix3d F, Matrix3d G) {
      double s = (  F.m00*G.m00 + F.m01*G.m01 + F.m02*G.m02
      + F.m10*G.m10 + F.m11*G.m11 + F.m12*G.m12
      + F.m20*G.m20 + F.m21*G.m21 + F.m22*G.m22) / G.frobeniusNormSquared ();
      G.scale(s);
   }
   
   /**
    * Scales G to best match uniform scaling of F
    * @param F comparison vector (unmodified)
    * @param G target vector (modified)
    */
   private static void rescale(Vector3d F, Vector3d G) {
      double s = (  F.dot (G) / G.normSquared () );
      G.scale(s);
   }
   
   /**
    * Estimates relative transforms given a set of camera matrices
    */
   public static void testEssentialRelativeTransformEstimation() {
      
      RandomGenerator.setSeed (0x9871231);
      
      RigidTransform3d T1 = new RigidTransform3d();
      RigidTransform3d T2 = new RigidTransform3d();
      
      for (int i=0; i<100; ++i) {
         
         // initialize camera calibration
         Matrix3d K1 = CVReconstruction3d.computeCameraCalibration (RandomGenerator.nextDouble (1, 100), Point2d.ZERO);
         Matrix3d K2 = CVReconstruction3d.computeCameraCalibration (RandomGenerator.nextDouble (1, 100), Point2d.ZERO);
         
         // random transforms
         T1.setRandom ();
         // T1.setIdentity ();  // reset
         T2.setRandom ();
         
         // compute camera matrices
         Matrix3x4 P1 = CVReconstruction3d.computeCameraMatrix (K1, T1);
         Matrix3x4 P2 = CVReconstruction3d.computeCameraMatrix (K2, T2);
         Matrix3d F = CVReconstruction3d.computeFundamentalMatrix (P1, P2);
         Matrix3d E = CVReconstruction3d.computeEssentialMatrix(F, K1, K2);
         
         Matrix3d E2 = CVReconstruction3d.computeEssentialMatrix (T1, T2);
         rescale (E, E2);
         if (!E.epsilonEquals (E2, 1e-10*E2.frobeniusNorm ())) {
            throw new TestException("Essential matrix computation failed");
         }

         // recover possible transforms
         RigidTransform3d[] transforms = CVReconstruction3d.computeRelativeTransform (F, K1, K2);
       
         // ensure all produce same essential and fundamental matrix
         Matrix3x4 Q1 = CVReconstruction3d.computeCameraMatrix (K1, RigidTransform3d.IDENTITY); //P1;
         Matrix3x4 Q2;
         Matrix3d G;
         for (RigidTransform3d trans : transforms) {
            
            // essential matrices
            E2 = CVReconstruction3d.computeEssentialMatrix (RigidTransform3d.IDENTITY, trans);
            rescale(E, E2);
            if (!E.epsilonEquals (E2, 1e-10)) {
               throw new TestException("Fundamental matrices do not match!");
            }
            
            // fundamental matrices
            Q2 = CVReconstruction3d.computeCameraMatrix (K2, trans);
            G = CVReconstruction3d.computeFundamentalMatrix (Q1, Q2);
            rescale(F, G);
            if (!F.epsilonEquals (G, 1e-10)) {
                throw new TestException("Fundamental matrices do not match!");
            }
         }
         
         // ensure one of transforms is true relative transform
         RigidTransform3d U2 = new RigidTransform3d();
         U2.mulInverseRight (T2, T1);
         U2.p.normalize ();
         
         boolean success = false;
         for (RigidTransform3d trans : transforms) {
            RigidTransform3d strans = new RigidTransform3d(trans);
            rescale(U2.p, strans.p);
            if (strans.epsilonEquals (U2, 1e-10)) {
               success = true;
               break;
            }
         }
         
         if (!success) {
            System.err.println (U2);
            System.err.println ("\n  vs  \n");
            System.err.println("Transforms: ");
            for (RigidTransform3d T : transforms) {
               System.err.println (T);
            }
            
            throw new TestException("Relative transform not found (" + i + ").");
         }
         
      }
      
   }
   
   /**
    * Multiply L' by R and place result in O
    * @param O output matrix
    * @param L left matrix
    * @param R right matrix
    */
   private static void mulTransposeLeft(DenseMatrix O, Matrix L, Matrix R) {
      
      if (L.rowSize () != R.rowSize ()) {
         throw new ImproperSizeException ("Matrices of incorrect size");
      }
      
      if (O.isFixedSize ()) {
         if (O.rowSize () != L.colSize () || O.colSize () != R.colSize ()) {
            throw new ImproperSizeException ("Output matrix is of incorrect size");
         }
      }
      
      // create temporary storage
      DenseMatrix C;
      if (O == L || O == R) {
         C = new MatrixNd(L.colSize (), R.colSize ());
      } else {
         C = O;
         if (O.rowSize () != L.colSize () || O.colSize () != R.colSize ()) {
            O.setSize (L.colSize (), R.colSize ());
         }
      }
      
      // do multiplication
      for (int r = 0; r < L.colSize (); ++r) {
         for (int c=0; c < R.colSize (); ++c) {
            double o = 0;
            for (int i=0; i<L.rowSize (); ++i) {
               o += L.get (i, r)*R.get (i, c);
            }
            C.set (r, c, o);
         }
      }
      
      // copy back result
      if (C != O) {
         O.set (C);
      }
   }
   
   /**
    * Multiply L by R and place result in O
    * @param O output matrix
    * @param L left matrix
    * @param R right matrix
    */
   private static void mul(DenseMatrix O, Matrix L, Matrix R) {
      
      if (L.colSize () != R.rowSize ()) {
         throw new ImproperSizeException ("Matrices of incorrect size");
      }
      
      if (O.isFixedSize ()) {
         if (O.rowSize () != L.rowSize () || O.colSize () != R.colSize ()) {
            throw new ImproperSizeException ("Output matrix is of incorrect size");
         }
      }
      
      // create temporary storage
      DenseMatrix C;
      if (O == L || O == R) {
         C = new MatrixNd(L.rowSize (), R.colSize ());
      } else {
         C = O;
         if (O.rowSize () != L.rowSize () || O.colSize () != R.colSize ()) {
            O.setSize (L.rowSize (), R.colSize ());
         }
      }
      
      // do multiplication
      for (int r = 0; r < L.rowSize (); ++r) {
         for (int c=0; c < R.colSize (); ++c) {
            double o = 0;
            for (int i=0; i<L.colSize (); ++i) {
               o += L.get (r, i)*R.get (i, c);
            }
            C.set (r, c, o);
         }
      }
      
      // copy back result
      if (C != O) {
         O.set (C);
      }
      
   }
   
   /**
    * Tests whether or not a dense matrix is skew-symmetric
    * @param M dense matrix
    * @param eps error tolerance
    * @return true if M' = -M
    */
   private static boolean isSkewSymmetric(DenseMatrix M, double eps) {
      
      int n = M.rowSize ();
      if (n != M.colSize ()) {
         return false;
      }
      
      for (int r = 0; r < n; ++r) {
         for (int c = r+1; c < n; ++c) {
            if ( Math.abs (M.get (r, c) + M.get (c, r)) > eps) {
               return false;
            }
         }
      }
      
      return true;
   }
   
   /**
    * Tests construction of the fundamental matrix
    */
   public static void testFundamentalMatrix() {
      
      RandomGenerator.setSeed (0x98324231);
      
      RigidTransform3d T1 = new RigidTransform3d();
      RigidTransform3d T2 = new RigidTransform3d();
      RigidTransform3d TR = new RigidTransform3d();  // relative transform
      MatrixNd C = new MatrixNd ();
      
      for (int i=0; i<100; ++i) {
         
         // initialize camera calibration
         Matrix3d K1 = CVReconstruction3d.computeCameraCalibration (RandomGenerator.nextDouble (1, 100), Point2d.ZERO);
         Matrix3d K2 = CVReconstruction3d.computeCameraCalibration (RandomGenerator.nextDouble (1, 100), Point2d.ZERO);
         
         // random transforms
         T1.setRandom ();
         T2.setRandom ();
         
         // compute camera matrices
         Matrix3x4 P1 = CVReconstruction3d.computeCameraMatrix (K1, T1);
         Matrix3x4 P2 = CVReconstruction3d.computeCameraMatrix (K2, T2);
         Matrix3d F = CVReconstruction3d.computeFundamentalMatrix (P1, P2);
         
         // verify fundamental matrix
         mulTransposeLeft (C, P2, F);
         mul (C, C, P1);
         
         if (!isSkewSymmetric(C, 1e-10 * C.frobeniusNorm ())) {
            throw new TestException("Fundamental matrix not valid");
         }
         
         // relative transform, ensure consistent fundamental matrix
         TR.setRandom ();
         RigidTransform3d T1B = new RigidTransform3d();
         T1B.mul (T1, TR);
         RigidTransform3d T2B = new RigidTransform3d();
         T2B.mul (T2, TR);
         
         Matrix3x4 P1B = CVReconstruction3d.computeCameraMatrix (K1, T1B);
         Matrix3x4 P2B = CVReconstruction3d.computeCameraMatrix (K2, T2B);
         Matrix3d FB = CVReconstruction3d.computeFundamentalMatrix (P1B, P2B);
         rescale(F, FB);
         
         Matrix3x4 P1C = new Matrix3x4();
         Matrix3x4 P2C = new Matrix3x4();
         mul(P1C, P1, TR);
         mul(P2C, P2, TR);
         if (!P1C.epsilonEquals (P1B, 1e-10*P1B.frobeniusNorm ())) {
            throw new TestException("Bad camera matrix transform");
         }
         if (!P2C.epsilonEquals (P2B, 1e-10*P2B.frobeniusNorm ())) {
            throw new TestException("Bad camera matrix transform");
         }
         
         // verify new fundamental matrix F2
         mulTransposeLeft (C, P2, FB);
         mul (C, C, P1);
         if (!isSkewSymmetric(C, 1e-10 * C.frobeniusNorm ())) {
            throw new TestException("Fundamental matrix not valid");
         }
         
         // verify original fundamental matrix F
         mulTransposeLeft (C, P2, F);
         mul (C, C, P1);
         if (!isSkewSymmetric(C, 1e-10 * C.frobeniusNorm ())) {
            throw new TestException("Fundamental matrix not valid");
         }
         
         // check equality of fundamental matrices
         if (!F.epsilonEquals (FB, 1e-10 * F.frobeniusNorm ())) {
            throw new TestException("Fundamental matrix not consistent");
         }
      }
   }
   
   public static void testHomogeneousReconstruction() {
      int n = 100;
      
      RandomGenerator.setSeed (0x87110);
      
      // 3D points
      ArrayList<Point3d> p3d = new ArrayList<> (n);
      for (int i=0; i<n; ++i) {
         Point3d p = new Point3d();
         p.x = RandomGenerator.nextDouble (-1, 1);
         p.y = RandomGenerator.nextDouble (-1, 1);
         p.z = RandomGenerator.nextDouble (-1, 1);
         p3d.add (p);
      }
      
      // cameras
      RigidTransform3d T1 = new RigidTransform3d(new Point3d(0, 0, 2), AxisAngle.IDENTITY);
      Matrix3d K1 = CVReconstruction3d.computeCameraCalibration (3, null);
      Matrix3x4 P1 = CVReconstruction3d.computeCameraMatrix (K1, T1);
      
      RigidTransform3d T2 = new RigidTransform3d(new Point3d(0, 0, 2), new AxisAngle(0, 1, 0, -Math.PI/2));
      Matrix3d K2 = CVReconstruction3d.computeCameraCalibration (2, Point2d.ZERO);
      Matrix3x4 P2 = CVReconstruction3d.computeCameraMatrix (K2, T2);
      
      RigidTransform3d T21 = new RigidTransform3d();
      T21.mulInverseRight (T2, T1);
      
      // projections
      Point3d x = new Point3d();
      Vector4d X = new Vector4d();
      
      // image 1
      ArrayList<Point2d> p1 = new ArrayList<Point2d>(n);
      for (Point3d p : p3d) {
         X.set (p.x, p.y, p.z, 1);
         P1.mul (x, X);
         p1.add (new Point2d(x.x/x.z, x.y/x.z));
      }

      // image 2
      ArrayList<Point2d> p2 = new ArrayList<Point2d>(n);
      for (Point3d p : p3d) {
         X.set (p.x, p.y, p.z, 1);
         P2.mul (x, X);
         p2.add (new Point2d(x.x/x.z, x.y/x.z));
      }
      
      // reconstruct given known cameras
      for (int i=0; i<p2.size (); ++i) {
         Point2d y1 = p1.get (i);
         Point2d y2 = p2.get (i);
         Point3d Y = p3d.get (i);
         Vector4d W = new Vector4d(Y.x, Y.y, Y.z, 1);
         
         X = CVReconstruction3d.homogeneousReconstruction (P1, P2, y1, y2);
         if (X.w != 0) {
            X.scale(1.0/X.w);
            if (!X.epsilonEquals (W, 1e-10)) {
               throw new TestException ("Invalid point reconstruction");
            }
            
            Point3d z1 = new Point3d(X.get (0), X.get (1), X.get (2));
            Point3d z2 = new Point3d(z1);
            z1.transform (T1);
            z2.transform (T2);
            
            if (z1.z < 0 || z2.z < 0) {
               throw new TestException("z-coordinate is behind image");
            }
            
         }
         
      }
   }
   
   /**
    * Generates two cameras and a set of points, then attempts to reconstruct the relative
    * transform between them.
    */
   public static void testReconstructionTransformEstimation() {
      
      int n = 100;
      
      RandomGenerator.setSeed (0x87110);
      
      // 3D points
      ArrayList<Point3d> p3d = new ArrayList<> (n);
      for (int i=0; i<n; ++i) {
         Point3d p = new Point3d();
         p.x = RandomGenerator.nextDouble (-1, 1);
         p.y = RandomGenerator.nextDouble (-1, 1);
         p.z = RandomGenerator.nextDouble (-1, 1);
         p3d.add (p);
      }
      
      // cameras
      RigidTransform3d T1 = new RigidTransform3d(new Point3d(0, 0, 2), AxisAngle.IDENTITY);
      Matrix3d K1 = CVReconstruction3d.computeCameraCalibration (3, null);
      Matrix3x4 P1 = CVReconstruction3d.computeCameraMatrix (K1, T1);
      
      RigidTransform3d T2 = new RigidTransform3d(new Point3d(0, 0, 2), new AxisAngle(0, 1, 0, -Math.PI/2));
      Matrix3d K2 = CVReconstruction3d.computeCameraCalibration (2, Point2d.ZERO);
      Matrix3x4 P2 = CVReconstruction3d.computeCameraMatrix (K2, T2);
      
      RigidTransform3d T21 = new RigidTransform3d();
      T21.mulInverseRight (T2, T1);
      
      // projections
      Point3d x = new Point3d();
      Vector4d X = new Vector4d();
      
      // image 1
      ArrayList<Point2d> p1 = new ArrayList<Point2d>(n);
      for (Point3d p : p3d) {
         X.set (p.x, p.y, p.z, 1);
         P1.mul (x, X);
         p1.add (new Point2d(x.x/x.z, x.y/x.z));
      }

      // image 2
      ArrayList<Point2d> p2 = new ArrayList<Point2d>(n);
      for (Point3d p : p3d) {
         X.set (p.x, p.y, p.z, 1);
         P2.mul (x, X);
         p2.add (new Point2d(x.x/x.z, x.y/x.z));
      }
      
      // reconstruct
      RigidTransform3d TR = CVReconstruction3d.linearEightPointAlgorithm (p1, p2, K1, K2);
      
      // compare TR to T21
      RigidTransform3d T21R = new RigidTransform3d(T21);
      T21R.p.normalize ();
      
      if (TR == null || !TR.epsilonEquals (T21R, 1e-10)) {
         throw new TestException ("Relative transform reconstruction failed!");
      }
      
   }
   
   public static void main (String[] args) {
      
      testFundamentalMatrix ();
      testFundamentalEstimation ();
      testEssentialRelativeTransformEstimation ();
      testHomogeneousReconstruction ();
      testReconstructionTransformEstimation();
      
      System.out.println ("All tests passed!");
      
   }
   
}
