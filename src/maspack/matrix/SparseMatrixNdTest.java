/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;

import java.io.IOException;

class SparseMatrixNdTest extends MatrixTest {
   void add (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).add ((SparseMatrixNd)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixNd)MR).add ((SparseMatrixNd)M1, (SparseMatrixNd)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).sub ((SparseMatrixNd)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixNd)MR).sub ((SparseMatrixNd)M1, (SparseMatrixNd)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).mul ((SparseMatrixNd)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixNd)MR).mul ((SparseMatrixNd)M1, (SparseMatrixNd)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).mulTranspose ((SparseMatrixNd)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixNd)MR).mulTransposeRight (
         (SparseMatrixNd)M1, (SparseMatrixNd)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixNd)MR).mulTransposeLeft (
         (SparseMatrixNd)M1, (SparseMatrixNd)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixNd)MR).mulTransposeBoth (
         (SparseMatrixNd)M1, (SparseMatrixNd)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).transpose ((SparseMatrixNd)M1);
   }

   void transpose (Matrix MR) {
      ((SparseMatrixNd)MR).transpose();
   }

   void negate (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).negate ((SparseMatrixNd)M1);
   }

   void negate (Matrix MR) {
      ((SparseMatrixNd)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((SparseMatrixNd)MR).scale (s, (SparseMatrixNd)M1);
   }

   void scale (Matrix MR, double s) {
      ((SparseMatrixNd)MR).scale (s);
   }

   void setZero (Matrix MR) {
      ((SparseMatrixNd)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((SparseMatrixNd)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((SparseMatrixNd)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((SparseMatrixNd)MR).set ((SparseMatrixNd)M1);
   }

   void testSetSize (SparseMatrixNd MR, int nrows, int ncols) {
      saveResult (MR);
      eExpected = setSizeCheck (MX, MR, nrows, ncols);
      try {
         MR.setSize (nrows, ncols);
      }
      catch (Exception e) {
         eActual = e;
      }
      MR.checkConsistency();
      checkAndRestoreResult (MR);
   }

   Exception setSizeCheck (
      MatrixNd MX, SparseMatrixNd MR, int nrows, int ncols) {
      if (MR.isFixedSize()) {
         return new UnsupportedOperationException ("Matrix has fixed size");
      }
      if (nrows < 0 || ncols < 0) {
         return new ImproperSizeException ("Negative dimension");
      }
      MatrixNd Mnew = new MatrixNd (nrows, ncols);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            if (i < MR.myNumRows && j < MR.myNumCols) {
               Mnew.set (i, j, MR.get (i, j));
            }
         }
      }
      MX.set (Mnew);
      return null;
   }

   void testRandomOrthogonal (int nrows, int ncols) {
      MatrixNd MR = new MatrixNd (nrows, ncols);
      MR.setRandomOrthogonal();
      MatrixNd Mprod = new MatrixNd (1, 1);
      int mind = Math.min (nrows, ncols);
      if (nrows <= ncols) {
         Mprod.mulTransposeRight (MR, MR);
      }
      else {
         Mprod.mulTransposeLeft (MR, MR);
      }
      MatrixNd Mexpected = new MatrixNd (mind, mind);
      Mexpected.setIdentity();
      if (!Mexpected.epsilonEquals (Mprod, EPSILON)) {
         throw new TestException (
            "setRandomOrthogonal failed:\n" + MR.toString ("%9.4f"));
      }
   }

   Exception permuteColumnsCheck (MatrixNd MX, MatrixNd MR, int[] perm) {
      if (perm.length < MR.ncols) {
         return new IllegalArgumentException ("permutation is too short");
      }
      boolean[] used = new boolean[MR.ncols];
      for (int j = 0; j < MR.ncols; j++) {
         if (perm[j] < 0 || perm[j] >= MR.ncols || used[perm[j]]) {
            return new IllegalArgumentException ("malformed permutation");
         }
         used[perm[j]] = true;
      }
      double[] col = new double[MR.nrows];
      MatrixNd MP = new MatrixNd (MR);
      for (int j = 0; j < MR.ncols; j++) {
         MR.getColumn (perm[j], col);
         MP.setColumn (j, col);
      }
      MX.set (MP);
      return null;
   }

   Exception permuteRowsCheck (MatrixNd MX, MatrixNd MR, int[] perm) {
      if (perm.length < MR.nrows) {
         return new IllegalArgumentException ("permutation is too short");
      }
      boolean[] used = new boolean[MR.nrows];
      for (int i = 0; i < MR.nrows; i++) {
         if (perm[i] < 0 || perm[i] >= MR.nrows || used[perm[i]]) {
            return new IllegalArgumentException ("malformed permutation");
         }
         used[perm[i]] = true;
      }
      double[] row = new double[MR.ncols];
      MatrixNd MP = new MatrixNd (MR);
      for (int i = 0; i < MR.nrows; i++) {
         MR.getRow (perm[i], row);
         MP.setRow (i, row);
      }
      MX.set (MP);
      return null;
   }

   void testColumnPermutation (MatrixNd MR, int perm[]) {
      saveResult (MR);
      eExpected = permuteColumnsCheck (MX, MR, perm);
      try {
         MR.permuteColumns (perm);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   void testRowPermutation (MatrixNd MR, int perm[]) {
      saveResult (MR);
      eExpected = permuteRowsCheck (MX, MR, perm);
      try {
         MR.permuteRows (perm);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      SparseMatrixNd M1_3x3 = new SparseMatrixNd (3, 3);
      SparseMatrixNd M1_9x9 = new SparseMatrixNd (9, 9);
      SparseMatrixNd M2_9x9 = new SparseMatrixNd (9, 9);
      SparseMatrixNd MR_9x9 = new SparseMatrixNd (9, 9);

      SparseMatrixNd MR_11x9 = new SparseMatrixNd (11, 9);
      SparseMatrixNd M1_11x9 = new SparseMatrixNd (11, 9);
      SparseMatrixNd M2_11x9 = new SparseMatrixNd (11, 9);

      SparseMatrixNd MR_9x11 = new SparseMatrixNd (9, 11);
      SparseMatrixNd M1_9x11 = new SparseMatrixNd (9, 11);
      SparseMatrixNd M2_9x11 = new SparseMatrixNd (9, 11);

      // M1_3x3.set (new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

      // testScan (MR_3x3, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", M1_3x3);
      // testScan (MR_3x4, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", M1_3x3);
      // testScan (MR_3x3, "[ 1 2 3 \n 4 5 6\n\n 7 8 9 ] ", M1_3x3);
      // testScan (MR_3x3, "[ 1 2 3 \n 4 5 6 ;; 7 8 9 ] ", M1_3x3);
      // testScan (MR_3x3, "[ 1 2 3 \n 4 5 6 7 \n 7 8 9 ] ",
      // new ImproperSizeException (
      // "Inconsistent row size, line 2"));
      // testScan (MAsub_3x4, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ",
      // new ImproperSizeException (
      // "Matrix size incompatible with input, line 1"));

      M1_3x3.set (
         new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, new int[] { 0, 0, 0, 1, 0,
                                                                2, 1, 0, 1, 1,
                                                                1, 2, 2, 0, 2,
                                                                1, 2, 2 }, 9);

      SparseMatrixNd MR_3x3 = new SparseMatrixNd (0, 0);
      testScan (MR_3x3, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", M1_3x3);
      MR_3x3 = new SparseMatrixNd (0, 0);
      testScan (MR_3x3, "[ (0 0 1) (1 1 5) (2 2 9)\n"
      + "  (0 1 2) (0 2 3) (1 0 4)\n" + "  (1 2 6) (2 0 7) (2 1 8) ]", M1_3x3);
      testScan (MR_3x3, "[ 1 2 3 \n 4 5 6\n\n 7 8 9 ] ", M1_3x3);
      testScan (MR_3x3, "[ 1 2 3 \n 4 5 6 ;; 7 8 9 ] ", M1_3x3);
      testScan (MR_3x3, "[ ( 0 0 1 ) 1 ", new IOException (
         "Token '(' expected for sparse matrix input, line 1"));
      testScan (MR_3x3, "[ ( 0 0 1 ) \n ( 1 1 1 1 ", new IOException (
         "Token ')' expected for sparse matrix input, line 2"));
      testScan (MR_3x3, "[ ( 0 0 1 ) \n ( -1 0 3) ]", new IOException (
"Expected non-negative integer for row index, got Token[n=-1], line 2"));
      testScan (MR_3x3, "[ ( 0 0 1 ) \n ( 1 -3 3) ]", new IOException (
"Expected non-negative integer for column index, got Token[n=-3], line 2"));

      M1_3x3.set (new double[] { 1, 5, 9 }, new int[] { 0, 0, 1, 1, 2, 2 }, 3);
      testScan (MR_3x3, "[ (0 0 1) (1 1 5) (2 2 9) ]", M1_3x3);

      testGeneric (MR_9x9);
      testGeneric (M1_11x9);

      // testGeneric (MR_3x3);

      testSetZero (MR_9x9);
      testSetZero (MR_11x9);
      testSetZero (MR_9x11);

      testSetIdentity (MR_9x9);
      testSetIdentity (MR_11x9);
      testSetIdentity (MR_9x11);

      testSetDiagonal (MR_9x9, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
      testSetDiagonal (MR_11x9, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
      testSetDiagonal (MR_9x11, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

      for (int i = 0; i < 10; i++) {
         SparseMatrixNd M0 = new SparseMatrixNd (4, 5);
         M0.setRandom();
         M0.checkConsistency();
         SparseMatrixNd MR = new SparseMatrixNd (M0);
         MR.checkConsistency();
         testSetSize (MR, -1, 5);
         testSetSize (MR, 6, 7);
         MR.set (M0);
         testSetSize (MR, 2, 5);
         MR.set (M0);
         testSetSize (MR, 4, 3);
         MR.set (M0);
         testSetSize (MR, 6, 3);
         MR.set (M0);
         testSetSize (MR, 2, 7);
         MR.set (M0);
         testSetSize (MR, 2, 3);
      }

      int n = 0;
      try {
         for (n = 0; n < 10; n++) {
            // basic tests with result different from args

            // testRandomOrthogonal (1, 1);
            // testRandomOrthogonal (3, 3);
            // testRandomOrthogonal (3, 5);
            // testRandomOrthogonal (5, 3);

            MR_9x9.setRandom();

            M1_9x9.setRandom();
            M1_11x9.setRandom();
            M1_9x11.setRandom();
            M2_9x9.setRandom();
            M2_11x9.setRandom();
            M2_9x11.setRandom();

            // testCopySubMatrix (MR_9x9, 2, 3, 4, 4, M1_9x9, 3, 1);
            // testCopySubMatrix (MR_3x3, 2, 3, 4, 4, M1_9x9, 3, 1);
            // testCopySubMatrix (MR_9x9, 2, 3, 4, 4, M1_3x3, 3, 1);
            // testCopySubMatrix (MR_9x9, -1, 3, 4, 4, M1_3x3, 3, 1);
            // testCopySubMatrix (MR_9x9, 2, 3, 4, 4, MR_9x9, 3, 1);

            testAdd (MR_9x9, M1_9x9, M2_9x9);
            testAdd (MR_9x9, MR_9x9, MR_9x9);
            testAdd (MR_9x9, M1_9x9, MR_9x9);
            testAdd (MR_9x9, MR_9x9, M1_9x9);
            testAdd (MR_9x9, M1_11x9, M2_11x9);
            testAdd (MR_9x9, M1_9x9, M2_11x9);
            testAdd (MR_9x9, M1_9x11, M2_11x9);
            testAdd (MR_9x9, M1_9x11, M2_9x11);
            testAdd (MR_9x11, MR_9x11, MR_9x11);

            MR_9x9.setRandom();
            M1_9x9.setRandom();
            M2_9x9.setRandom();

            testSub (MR_9x9, M1_9x9, M2_9x9);
            testSub (MR_9x9, MR_9x9, MR_9x9);
            testSub (MR_9x9, M1_9x9, MR_9x9);
            testSub (MR_9x9, MR_9x9, M1_9x9);
            testSub (MR_9x9, M1_11x9, M2_11x9);
            testSub (MR_9x9, M1_9x9, M2_11x9);
            testSub (MR_9x9, M1_9x11, M2_11x9);
            testSub (MR_9x9, M1_9x11, M2_9x11);
            testSub (MR_9x11, MR_9x11, MR_9x11);

            MR_9x9.setRandom();
            M1_9x9.setRandom();
            M2_9x9.setRandom();
            SparseMatrixNd MR_11x11 = new SparseMatrixNd (11, 11);
            MR_11x11.setRandom();

            testMul (MR_9x9, M1_9x9, M2_9x9);
            testMul (MR_9x9, M1_9x11, M2_11x9);
            testMul (MR_11x11, M1_9x11, M2_11x9);
            testMul (MR_11x11, M1_9x11, M2_9x11);

            testMulTranspose (MR_9x9, M1_9x9, M2_9x9);
            testMulTranspose (MR_9x9, M1_9x11, M2_11x9);
            testMulTranspose (MR_9x9, M1_11x9, M2_11x9);
            testMulTranspose (MR_9x9, M1_9x11, M2_9x11);
            testMulTranspose (MR_11x11, M1_9x11, M2_11x9);
            testMulTranspose (MR_9x9, M1_11x9, M2_9x11);

            MR_9x9.setRandom();

            testNegate (MR_9x9, MR_9x9);
            testNegate (MR_9x9, M1_11x9);
            testNegate (MR_11x9, M1_11x9);

            MR_9x9.setRandom();

            testScale (MR_9x9, 1.23, MR_9x9);
            testScale (MR_9x9, 1.23, M1_11x9);
            testScale (MR_11x9, 1.23, M1_11x9);

            testSet (MR_9x9, M1_9x9);
            testSet (MR_9x9, M1_11x9);

            MR_9x9.setRandom();
            MR_9x11.setRandom();
            M1_11x9.setRandom();

            testTranspose (MR_9x9, MR_9x9);
            testTranspose (MR_9x11, MR_9x11);
            testTranspose (MR_9x9, MR_9x11);
            testTranspose (MR_9x9, M1_11x9);
            testTranspose (MR_9x9, M1_9x9);
            testTranspose (MR_9x9, M1_9x11);

            // int[] perm = new int[] { 0, 1, 2, 3 };
            // testColumnPermutation (MRsub_4x4, perm);
            // testRowPermutation (MRsub_4x4, perm);
            // perm = new int[] { 0, 1, 3, 1 };
            // testColumnPermutation (MRsub_4x4, perm);
            // testRowPermutation (MRsub_4x4, perm);
            // perm = new int[] { 0, 4, 1, 2 };
            // testColumnPermutation (MRsub_4x4, perm);
            // testRowPermutation (MRsub_4x4, perm);
            // for (int i=0; i<10; i++)
            // { perm = randomPermutation(9);
            // testRowPermutation (MR_9x9, perm);
            // testColumnPermutation (MR_9x9, perm);
            // perm = randomPermutation(3);
            // testColumnPermutation (MAsub_4x3, perm);
            // testRowPermutation (MAsub_3x4, perm);
            // }

            // testNorms (M1_3x3);
            // testNorms (M1sub_4x3);
            // testNorms (M1sub_3x4);
            // testNorms (M1_4x4);
         }
      }
      catch (Exception e) {
         System.out.println ("n=" + n);
         e.printStackTrace();
         System.exit (1);
      }
   }

   public static void main (String[] args) {
      SparseMatrixNdTest test = new SparseMatrixNdTest();

      test.execute();

      System.out.println ("\nPassed\n");
   }
}
