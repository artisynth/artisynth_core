/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class VectorNdTest extends VectorTest {

   boolean equals (Vector vr, Vector v1) {
      return ((VectorNd)vr).equals ((VectorNd)v1);
   }

   boolean epsilonEquals (Vector vr, Vector v1, double tol) {
      return ((VectorNd)vr).epsilonEquals ((VectorNd)v1, tol);
   }

   void add (Vector vr, Vector v1) {
      ((VectorNd)vr).add ((VectorNd)v1);
   }

   void add (Vector vr, Vector v1, Vector v2) {
      ((VectorNd)vr).add ((VectorNd)v1, (VectorNd)v2);
   }

   void sub (Vector vr, Vector v1) {
      ((VectorNd)vr).sub ((VectorNd)v1);
   }

   void sub (Vector vr, Vector v1, Vector v2) {
      ((VectorNd)vr).sub ((VectorNd)v1, (VectorNd)v2);
   }

   void negate (Vector vr, Vector v1) {
      ((VectorNd)vr).negate ((VectorNd)v1);
   }

   void negate (Vector vr) {
      ((VectorNd)vr).negate();
   }

   void scale (Vector vr, double s, Vector v1) {
      ((VectorNd)vr).scale (s, (VectorNd)v1);
   }

   void scale (Vector vr, double s) {
      ((VectorNd)vr).scale (s);
   }

   void setZero (Vector vr) {
      ((VectorNd)vr).setZero();
   }

   void interpolate (Vector vr, double s, Vector v1) {
      ((VectorNd)vr).interpolate (s, (VectorNd)v1);
   }

   void interpolate (Vector vr, Vector v1, double s, Vector v2) {
      ((VectorNd)vr).interpolate ((VectorNd)v1, s, (VectorNd)v2);
   }

   void scaledAdd (Vector vr, double s, Vector v1) {
      ((VectorNd)vr).scaledAdd (s, (VectorNd)v1);
   }

   void scaledAdd (Vector vr, double s, Vector v1, Vector v2) {
      ((VectorNd)vr).scaledAdd (s, (VectorNd)v1, (VectorNd)v2);
   }

   void combine (Vector vr, double a, Vector v1, double b, Vector v2) {
      ((VectorNd)vr).combine (a, (VectorNd)v1, b, (VectorNd)v2);
   }

   double dot (Vector v1, Vector v2) {
      return ((VectorNd)v1).dot ((VectorNd)v2);
   }

   double angle (Vector v1, Vector v2) {
      return ((VectorNd)v1).angle ((VectorNd)v2);
   }

   void normalize (Vector vr) {
      ((VectorNd)vr).normalize();
   }

   void normalize (Vector vr, Vector v1) {
      ((VectorNd)vr).normalize ((VectorNd)v1);
   }

   void set (Vector vr, Vector v1) {
      ((VectorNd)vr).set ((VectorNd)v1);
   }

   void testMul (VectorNd vr, MatrixNd M, VectorNd v1) {
      saveResult (vr);
      eExpected = MatrixTest.mulVectorCheck (vr, M, vr, v1);
      saveExpectedResult (vr);
      try {
         vr.mul (M, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = MatrixTest.mulTransposeVectorCheck (vr, M, vr, v1);
      saveExpectedResult (vr);
      try {
         vr.mulTranspose (M, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testSparseMul (VectorNd vr, SparseMatrixNd M, VectorNd v1) {
      saveResult (vr);
      MatrixNd Mdense = new MatrixNd();
      Mdense.set (M);
      eExpected = MatrixTest.mulVectorCheck (vr, Mdense, vr, v1);
      saveExpectedResult (vr);
      try {
         vr.mul (M, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = MatrixTest.mulTransposeVectorCheck (vr, Mdense, vr, v1);
      saveExpectedResult (vr);
      try {
         vr.mulTranspose (M, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

//    Exception mulCheck (VectorNd vr, MatrixNd M, VectorNd v1) {
//       if (M.colSize() != v1.size()) {
//          return new ImproperSizeException ("Incompatible dimensions");
//       }
//       else if (M.rowSize() != vr.size()) {
//          if (vr.isFixedSize()) {
//             return new ImproperSizeException ("Incompatible dimensions");
//          }
//       }
//       VectorNd res = new VectorNd (M.rowSize());
//       for (int i = 0; i < M.rowSize(); i++) {
//          double sum = 0;
//          for (int j = 0; j < M.colSize(); j++) {
//             sum += M.get (i, j) * v1.get (j);
//          }
//          res.set (i, sum);
//       }
//       vr.set (res);
//       return null;
//    }

//    Exception mulTransposeCheck (VectorNd vr, MatrixNd M, VectorNd v1) {
//       if (M.rowSize() != v1.size()) {
//          return new ImproperSizeException ("Incompatible dimensions");
//       }
//       else if (M.colSize() != vr.size()) {
//          if (vr.isFixedSize()) {
//             return new ImproperSizeException ("Incompatible dimensions");
//          }
//       }
//       VectorNd res = new VectorNd (M.colSize());
//       for (int j=0; j<M.colSize(); j++) {
//          double sum = 0;
//          for (int i=0; i<M.rowSize(); i++) {
//             sum += M.get (i,j)*v1.get(i);
//          }
//          res.set (j, sum);
//       }
//       vr.set (res);
//       return null;
//    }

   private void testHermiteInterpolation (
      VectorNd pos0, VectorNd vel0, VectorNd pos1, VectorNd vel1) {
      
      int vsize = pos0.size();

      VectorNd pos = new VectorNd(vsize);
      VectorNd vel = new VectorNd(vsize);
      VectorNd pchk = new VectorNd(vsize);
      VectorNd vchk = new VectorNd(vsize);

      for (double h=0.5; h<2; h+=0.5) {
         for (double s=0; s<=1.0; s+=0.2) {
            VectorNd.hermiteInterpolate (pos, pos0, vel0, pos1, vel1, s, h);
            VectorNd.hermiteVelocity (vel, pos0, vel0, pos1, vel1, s, h);

            for (int k=0; k<vsize; k++) {
               double p0 = pos0.get(k);
               double v0 = vel0.get(k);
               double p1 = pos1.get(k);
               double v1 = vel1.get(k);

               double a0 = p0;
               double a1 = v0;
               double a2 = (3*(p1-p0)/h - (2*v0 + v1))/h;
               double a3 = (2*(p0-p1)/h + v0 + v1)/(h*h);
               double t = s*h;
               pchk.set (k, ((a3*t + a2)*t + a1)*t + a0);
               vchk.set (k, (3*a3*t + 2*a2)*t + a1);
            }
            checkEquals ("hermiteInterpolate", pos, pchk, 50*DOUBLE_PREC);
            checkEquals ("hermiteVelocity", vel, vchk, 50*DOUBLE_PREC);
         }
      }
   }

   public void execute() {
      VectorNd vr_0 = new VectorNd (0);
      VectorNd vr_2 = new VectorNd (2);
      VectorNd vr_9 = new VectorNd (9);
      VectorNd vr_11 = new VectorNd (11);
      VectorNd v1_3 = new VectorNd (3);
      VectorNd v1_9 = new VectorNd (9);
      VectorNd v2_9 = new VectorNd (9);
      VectorNd v1_11 = new VectorNd (11);
      VectorNd v2_11 = new VectorNd (11);

      MatrixNd M3x2 = new MatrixNd (3, 2);
      MatrixNd M9x9 = new MatrixNd (9, 9);
      MatrixNd M11x9 = new MatrixNd (11, 9);

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1_9);
      testGeneric (vr_2);
      testGeneric (vr_0);
      testSetZero (vr_9);

      for (int i = 0; i < 100; i++) {
         v1_3.setRandom();
         v1_9.setRandom();
         v2_9.setRandom();
         vr_9.setRandom();
         v1_11.setRandom();
         v2_11.setRandom();
         vr_11.setRandom();

         M3x2.setRandom();
         M9x9.setRandom();
         M11x9.setRandom();

         testEquals (vr_9, v1_9);

         testAdd (vr_9, v1_9, v2_9);
         testAdd (vr_9, v1_9, v2_11);
         testAdd (vr_9, v1_11, v2_9);
         testAdd (vr_9, v1_11, v2_11);
         testAdd (vr_9, vr_9, vr_9);

         testSub (vr_9, v1_9, v2_9);
         testSub (vr_9, v1_9, v2_11);
         testSub (vr_9, v1_11, v2_9);
         testSub (vr_9, v1_11, v2_11);
         testSub (vr_9, vr_9, vr_9);

         testNegate (vr_9, v1_9);
         testNegate (vr_9, v1_11);
         testNegate (vr_9, vr_9);

         testScale (vr_9, 1.23, v1_9);
         testScale (vr_9, 1.23, v1_11);
         testScale (vr_9, 1.23, vr_9);

         testScaledAdd (vr_9, 3.0, v1_9, v2_9);
         testScaledAdd (vr_9, -6.7, v1_9, v2_11);
         testScaledAdd (vr_9, 11.0, v1_11, v2_9);
         testScaledAdd (vr_9, 9.0, v1_11, v2_11);
         testScaledAdd (vr_9, 12.5, vr_9, vr_9);

         testSet (vr_9, v1_9);
         testSet (vr_9, v1_11);
         testSet (vr_9, vr_9);

         testNormalize (vr_9, v1_9);
         testNormalize (vr_9, v1_11);
         testNormalize (vr_9, vr_9);

         testCombine (vr_9, 0.123, v1_9, 0.677, v2_9);
         testCombine (vr_9, 0.123, v1_11, 0.677, v2_9);
         testCombine (vr_9, 0.123, v1_9, 0.677, v2_11);
         testCombine (vr_9, 0.123, v1_11, 0.677, v2_11);
         testCombine (vr_9, 0.123, vr_9, 0.677, vr_9);

         testMul (vr_2, M3x2, v1_3);
         testMul (vr_9, M9x9, v1_9);
         testMul (vr_9, M9x9, vr_9);
         testMul (vr_11, M11x9, v1_9);
         testMul (vr_11, M11x9, vr_9);
         testMul (vr_9, M11x9, v1_11);
         testMul (vr_9, M11x9, vr_11);

         SparseMatrixNd S11x9 = new SparseMatrixNd (11, 9);
         SparseMatrixNd S9x9 = new SparseMatrixNd (9, 9);

         S11x9.setRandom();
         S9x9.setRandom();

         testSparseMul (vr_9, S9x9, v1_9);
         testSparseMul (vr_9, S9x9, vr_9);
         testSparseMul (vr_11, S11x9, v1_9);
         testSparseMul (vr_11, S11x9, vr_9);
         testSparseMul (vr_9, S11x9, v1_11);
         testSparseMul (vr_9, S11x9, vr_11);

         VectorNd p0 = new VectorNd(3);
         VectorNd v0 = new VectorNd(3);
         VectorNd p1 = new VectorNd(3);
         VectorNd v1 = new VectorNd(3);
         p0.setRandom();
         v0.setRandom();
         p1.setRandom();
         v1.setRandom();
         testHermiteInterpolation (p0, v0, p1, v1);

         testNorms (v1_9);
         testDotAndAngle (v1_9, v2_9);
      }
   }

   public static void main (String[] args) {
      VectorNdTest test = new VectorNdTest();

      try {
         test.execute();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");
   }
}
