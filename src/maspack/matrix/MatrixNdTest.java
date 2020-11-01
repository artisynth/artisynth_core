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

class MatrixNdTest extends MatrixTest {

    boolean equals (Matrix MR, Matrix M1) {
      return ((MatrixNd)M1).equals ((MatrixNd)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((MatrixNd)M1).epsilonEquals ((MatrixNd)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).add ((MatrixNd)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).add ((MatrixNd)M1, (MatrixNd)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).sub ((MatrixNd)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).sub ((MatrixNd)M1, (MatrixNd)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).mul ((MatrixNd)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).mul ((MatrixNd)M1, (MatrixNd)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).mulTranspose ((MatrixNd)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).mulTransposeRight ((MatrixNd)M1, (MatrixNd)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).mulTransposeLeft ((MatrixNd)M1, (MatrixNd)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).mulTransposeBoth ((MatrixNd)M1, (MatrixNd)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).transpose ((MatrixNd)M1);
   }

   void transpose (Matrix MR) {
      ((MatrixNd)MR).transpose();
   }

   void negate (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).negate ((MatrixNd)M1);
   }

   void negate (Matrix MR) {
      ((MatrixNd)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((MatrixNd)MR).scale (s, (MatrixNd)M1);
   }

   void scale (Matrix MR, double s) {
      ((MatrixNd)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((MatrixNd)MR).scaledAdd (s, (MatrixNd)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).scaledAdd (s, (MatrixNd)M1, (MatrixNd)M2);
   }

   void setZero (Matrix MR) {
      ((MatrixNd)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((MatrixNd)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((MatrixNd)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((MatrixNd)MR).set ((MatrixNd)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((MatrixNd)MR).mulAdd (M1, M2);
   }

   void testCopySubMatrix (
      MatrixNd MR, int row0, int col0, int nrows, int ncols, MatrixNd MT,
      int rowDest, int colDest) {
      saveResult (MR);
      eExpected =
         copySubMatrixCheck (
            MR, row0, col0, nrows, ncols, MT, rowDest, colDest);
      MX.set (MR);
      MR.set (MRsave);
      try {
         MR.copySubMatrix (row0, col0, nrows, ncols, MT, rowDest, colDest);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   Exception copySubMatrixCheck (
      MatrixNd MR, int row0, int col0, int nrows, int ncols, 
      MatrixNd MT, int rowDest, int colDest) {
      if (nrows < 0 || ncols < 0) {
         return new ImproperSizeException ("Negative dimensions");
      }
      if (row0 + nrows > MT.rowSize() || col0 + ncols > MT.colSize()) {
         return new ImproperSizeException ("Dimensions out of bounds");
      }
      if (rowDest + nrows > MR.rowSize() || colDest + ncols > MR.colSize()) {
         return new ImproperSizeException ("Dimensions out of bounds");
      }
      double[] buf = new double[nrows * ncols];
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            buf[i * ncols + j] = MT.get (row0 + i, col0 + j);
         }
      }
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            MR.set (rowDest + i, colDest + j, buf[i * ncols + j]);
         }
      }
      return null;
   }

   void testSetSize (MatrixNd MR, int nrows, int ncols) {
      saveResult (MR);
      eExpected = setSizeCheck (MX, MR, nrows, ncols);
      try {
         MR.setSize (nrows, ncols);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
   }

   Exception setSizeCheck (MatrixNd MX, MatrixNd MR, int nrows, int ncols) {
      if (MR.isFixedSize()) {
         return new UnsupportedOperationException ("Matrix has fixed size");
      }
      if (nrows < 0 || ncols < 0) {
         return new ImproperSizeException ("Negative dimension");
      }
      MatrixNd Mnew = new MatrixNd (nrows, ncols);
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            if (i < MR.nrows && j < MR.ncols) {
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
         throw new TestException ("setRandomOrthogonal failed:\n"
         + MR.toString ("%9.4f"));
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

      MatrixNd M1_9x9 = new MatrixNd (9, 9);
      MatrixNd M2_9x9 = new MatrixNd (9, 9);
      MatrixNd MR_9x9 = new MatrixNd (9, 9);

      MatrixNd M1_3x3 = new MatrixNd (3, 3);
      MatrixNd M1_3x4 = new MatrixNd (3, 4);
      MatrixNd M1_4x3 = new MatrixNd (4, 3);
      MatrixNd M1_4x4 = new MatrixNd (4, 4);

      // MatrixNd M1sub_3x3 = new MatrixNd (M1_9x9, 2, 1, 3, 3);
      // MatrixNd M1sub_3x4 = new MatrixNd (M1_9x9, 1, 2, 3, 4);
      // MatrixNd M1sub_4x3 = new MatrixNd (M1_9x9, 1, 2, 4, 3);
      // MatrixNd M1sub_4x4 = new MatrixNd (M1_9x9, 2, 2, 4, 4);

      SubMatrixNd M1sub_3x3 = new SubMatrixNd (2, 1, 3, 3, M1_9x9);
      SubMatrixNd M1sub_3x4 = new SubMatrixNd (1, 2, 3, 4, M1_9x9);
      SubMatrixNd M1sub_4x3 = new SubMatrixNd (1, 2, 4, 3, M1_9x9);
      // SubMatrixNd M1sub_4x4 = new SubMatrixNd (2, 2, 4, 4, M1_9x9);

      MatrixNd M2_3x3 = new MatrixNd (3, 3);
      MatrixNd M2_3x4 = new MatrixNd (3, 4);
      MatrixNd M2_4x3 = new MatrixNd (4, 3);
      MatrixNd M2_4x4 = new MatrixNd (4, 4);

      // MatrixNd M2sub_3x3 = new MatrixNd (M2_9x9, 2, 1, 3, 3);
      // MatrixNd M2sub_3x4 = new MatrixNd (M2_9x9, 1, 2, 3, 4);
      // MatrixNd M2sub_4x3 = new MatrixNd (M2_9x9, 1, 2, 4, 3);
      // MatrixNd M2sub_4x4 = new MatrixNd (M2_9x9, 2, 2, 4, 4);

      SubMatrixNd M2sub_3x3 = new SubMatrixNd (2, 1, 3, 3, M2_9x9);
      SubMatrixNd M2sub_3x4 = new SubMatrixNd (1, 2, 3, 4, M2_9x9);
      SubMatrixNd M2sub_4x3 = new SubMatrixNd (1, 2, 4, 3, M2_9x9);
      // SubMatrixNd M2sub_4x4 = new SubMatrixNd (2, 2, 4, 4, M2_9x9);

      MatrixNd MR_3x3 = new MatrixNd (3, 3);
      MatrixNd MR_3x4 = new MatrixNd (3, 4);
      MatrixNd MR_4x3 = new MatrixNd (4, 3);
      MatrixNd MR_4x4 = new MatrixNd (4, 4);

      // MatrixNd MRsub_3x3 = new MatrixNd (MR_9x9, 2, 1, 3, 3);
      // MatrixNd MRsub_3x4 = new MatrixNd (MR_9x9, 1, 2, 3, 4);
      // MatrixNd MRsub_4x3 = new MatrixNd (MR_9x9, 1, 2, 4, 3);
      // MatrixNd MRsub_4x4 = new MatrixNd (MR_9x9, 2, 2, 4, 4);

      // MatrixNd MAsub_3x3 = new MatrixNd (MR_9x9, 1, 0, 3, 3);
      // MatrixNd MAsub_3x4 = new MatrixNd (MR_9x9, 0, 1, 3, 4);
      // MatrixNd MAsub_4x3 = new MatrixNd (MR_9x9, 0, 1, 4, 3);
      // MatrixNd MAsub_4x4 = new MatrixNd (MR_9x9, 1, 1, 4, 4);

      // MatrixNd MBsub_3x3 = new MatrixNd (MR_9x9, 3, 2, 3, 3);
      // MatrixNd MBsub_3x4 = new MatrixNd (MR_9x9, 2, 3, 3, 4);
      // MatrixNd MBsub_4x3 = new MatrixNd (MR_9x9, 2, 3, 4, 3);
      // MatrixNd MBsub_4x4 = new MatrixNd (MR_9x9, 3, 3, 4, 4);

      SubMatrixNd MRsub_3x3 = new SubMatrixNd (2, 1, 3, 3, MR_9x9);
      SubMatrixNd MRsub_3x4 = new SubMatrixNd (1, 2, 3, 4, MR_9x9);
      SubMatrixNd MRsub_4x3 = new SubMatrixNd (1, 2, 4, 3, MR_9x9);
      SubMatrixNd MRsub_4x4 = new SubMatrixNd (2, 2, 4, 4, MR_9x9);

      SubMatrixNd MAsub_3x3 = new SubMatrixNd (1, 0, 3, 3, MR_9x9);
      SubMatrixNd MAsub_3x4 = new SubMatrixNd (0, 1, 3, 4, MR_9x9);
      SubMatrixNd MAsub_4x3 = new SubMatrixNd (0, 1, 4, 3, MR_9x9);
      // SubMatrixNd MAsub_4x4 = new SubMatrixNd (1, 1, 4, 4, MR_9x9);

      SubMatrixNd MBsub_3x3 = new SubMatrixNd (3, 2, 3, 3, MR_9x9);
      SubMatrixNd MBsub_3x4 = new SubMatrixNd (2, 3, 3, 4, MR_9x9);
      SubMatrixNd MBsub_4x3 = new SubMatrixNd (2, 3, 4, 3, MR_9x9);
      // SubMatrixNd MBsub_4x4 = new SubMatrixNd (3, 3, 4, 4, MR_9x9);

      M1_3x3.set (new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

      testScan (MR_3x3, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", M1_3x3);
      testScan (MR_3x3, "[ (0 0 1) (1 1 5) (2 2 9)\n"
      + "  (0 1 2) (0 2 3) (1 0 4)\n" + "  (1 2 6) (2 0 7) (2 1 8) ]", M1_3x3);
      testScan (MR_3x4, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", M1_3x3);
      testScan (MR_3x3, "[ 1 2 3 \n 4 5 6\n\n 7 8 9 ] ", M1_3x3);
      testScan (MR_3x3, "[ 1 2 3 \n 4 5 6 ;; 7 8 9 ] ", M1_3x3);
      testScan (
         MR_3x3, "[ 1 2 3 \n 4 5 6 7 \n 7 8 9 ] ", new ImproperSizeException (
            "Inconsistent row size, line 2"));
      testScan (MR_3x3, "[ ( 0 0 1 ) 1 ", new IOException (
         "Token '(' expected for sparse matrix input, line 1"));
      testScan (MR_3x3, "[ ( 0 0 1 ) \n ( 1 1 1 1 ", new IOException (
         "Token ')' expected for sparse matrix input, line 2"));
      testScan (MR_3x3, "[ ( 0 0 1 ) \n ( -1 0 3) ]", new IOException (
"Expected non-negative integer for row index, got Token[n=-1], line 2"));
      testScan (MR_3x3, "[ ( 0 0 1 ) \n ( 1 -3 3) ]", new IOException (
"Expected non-negative integer for column index, got Token[n=-3], line 2"));
      testScan (
         MAsub_3x4, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", new ImproperSizeException (
            "Matrix size incompatible with input, line 1"));
      testScan (
         MAsub_3x4, "[ (0 0 1) (1 1 4) \n (3 1 1) ]",
         new ImproperSizeException (
            "Matrix size incompatible with row index 3, line 2"));
      testScan (
         MAsub_3x4, "[ (0 0 1) (1 5 4) \n (2 1 1) ]",
         new ImproperSizeException (
            "Matrix size incompatible with column index 5, line 1"));

      M1_3x3.set (new double[] { 1, 0, 0, 0, 5, 0, 0, 0, 9 });
      testScan (MR_3x3, "[ (0 0 1) (1 1 5) (2 2 9) ]", M1_3x3);

      testGeneric (MRsub_3x4);
      testGeneric (MRsub_4x3);
      testGeneric (MR_3x3);
      testGeneric (MR_4x3);
      testGeneric (MR_3x4);

      testSetZero (MRsub_3x4);
      testSetZero (MRsub_4x3);
      testSetZero (MR_3x3);

      testSetIdentity (MRsub_3x4);
      testSetIdentity (MRsub_4x3);
      testSetIdentity (MR_3x3);

      testSetDiagonal (MRsub_3x4, new double[] { 1, 2, 3 });
      testSetDiagonal (MRsub_4x3, new double[] { 1, 2, 3, 4 });
      testSetDiagonal (MR_3x3, new double[] { 1, 2, 3, 4 });

      MatrixNd M0 = new MatrixNd (4, 5);
      M0.setRandom();
      MatrixNd MR = new MatrixNd (M0);
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

      for (int n = 0; n < 10; n++) {
         testEquals (M0, MR);
      }

      int n = 0;
      try {
         for (n = 0; n < 4; n++) {
            // basic tests with result different from args

            testRandomOrthogonal (1, 1);
            testRandomOrthogonal (3, 3);
            testRandomOrthogonal (3, 5);
            testRandomOrthogonal (5, 3);

            MR_9x9.setRandom();

            M1_3x3.setRandom();
            M1_3x4.setRandom();
            M1_4x3.setRandom();
            M1_4x4.setRandom();
            M1_9x9.setRandom();

            M2_3x3.setRandom();
            M2_3x4.setRandom();
            M2_4x3.setRandom();
            M2_4x4.setRandom();
            M2_9x9.setRandom();

            testCopySubMatrix (MR_9x9, 2, 3, 4, 4, M1_9x9, 3, 1);
            testCopySubMatrix (MR_3x3, 2, 3, 4, 4, M1_9x9, 3, 1);
            testCopySubMatrix (MR_9x9, 2, 3, 4, 4, M1_3x3, 3, 1);
            testCopySubMatrix (MR_9x9, -1, 3, 4, 4, M1_3x3, 3, 1);
            testCopySubMatrix (MR_9x9, 2, 3, 4, 4, MR_9x9, 3, 1);

            testAdd (MR_3x3, M1_3x3, M2_3x3);
            testAdd (MR_3x3, M1_4x4, M2_4x4);
            testAdd (MR_3x3, M1_3x4, M2_4x4);
            testAdd (MR_3x3, M1_3x4, M2_3x4);
            testAdd (MR_3x3, M1_3x4, M2_4x3);

            testAdd (MRsub_3x3, M1sub_3x3, M2sub_3x3);
            testAdd (MRsub_4x4, M1sub_3x3, M2sub_3x3);
            testAdd (MRsub_3x4, M1sub_3x4, M2sub_3x4);

            testAdd (MRsub_3x3, MAsub_3x3, MBsub_3x3);
            testAdd (MRsub_4x4, MAsub_3x3, MBsub_3x3);
            testAdd (MRsub_3x4, MAsub_3x4, MBsub_3x4);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testSub (MR_3x3, M1_3x3, M2_3x3);
            testSub (MR_3x3, M1_3x4, M2_4x4);
            testSub (MR_3x3, M1_4x4, M2_4x4);
            testSub (MR_3x3, M1_3x4, M2_3x4);
            testSub (MR_3x3, M1_3x4, M2_4x3);

            testSub (MRsub_3x3, M1sub_3x3, M2sub_3x3);
            testSub (MRsub_4x4, M1sub_3x3, M2sub_3x3);
            testSub (MRsub_3x4, M1sub_3x4, M2sub_3x4);

            testSub (MRsub_3x3, MAsub_3x3, MBsub_3x3);
            testSub (MRsub_4x4, MAsub_3x3, MBsub_3x3);
            testSub (MRsub_3x4, MAsub_3x4, MBsub_3x4);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testMul (MR_3x3, M1_3x3, M2_3x3);
            testMul (MR_3x3, M1_3x4, M2_4x3);
            testMul (MR_4x4, M1_3x4, M2_4x3);
            testMul (MR_4x4, M1_3x4, M2_3x4);

            testMul (MRsub_3x3, M1sub_3x3, M2sub_3x3);
            testMul (MRsub_3x3, M1sub_3x4, M2sub_4x3);
            testMul (MRsub_4x4, M1sub_3x4, M2sub_4x3);
            testMul (MRsub_4x4, M1sub_3x4, M2sub_3x4);

            testMul (MRsub_3x3, MAsub_3x3, MBsub_3x3);
            testMul (MRsub_3x3, MAsub_3x4, MBsub_4x3);
            testMul (MRsub_4x4, MAsub_3x4, MBsub_4x3);
            testMul (MRsub_4x4, MAsub_3x4, MBsub_3x4);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testMulTranspose (MR_3x3, M1_3x3, M2_3x3);
            testMulTranspose (MR_3x3, M1_3x4, M2_4x3);
            testMulTranspose (MR_3x3, M1_4x3, M2_4x3);
            testMulTranspose (MR_3x3, M1_3x4, M2_3x4);
            testMulTranspose (MR_4x4, M1_3x4, M2_4x3);
            testMulTranspose (MR_3x3, M1_4x3, M2_3x4);

            testMulTranspose (MRsub_3x3, M1sub_3x3, M2sub_3x3);
            testMulTranspose (MRsub_3x3, M1sub_3x4, M2sub_4x3);
            testMulTranspose (MRsub_3x3, M1sub_4x3, M2sub_4x3);
            testMulTranspose (MRsub_3x3, M1sub_3x4, M2sub_3x4);
            testMulTranspose (MRsub_4x4, M1sub_3x4, M2sub_4x3);
            testMulTranspose (MRsub_3x3, M1sub_4x3, M2sub_3x4);

            testMulTranspose (MRsub_3x3, MAsub_3x3, MBsub_3x3);
            testMulTranspose (MRsub_3x3, MAsub_3x4, MBsub_4x3);
            testMulTranspose (MRsub_3x3, MAsub_4x3, MBsub_4x3);
            testMulTranspose (MRsub_3x3, MAsub_3x4, MBsub_3x4);
            testMulTranspose (MRsub_4x4, MAsub_3x4, MBsub_4x3);
            testMulTranspose (MRsub_3x3, MAsub_4x3, MBsub_3x4);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testNegate (MR_3x3, M1_3x3);
            testNegate (MR_3x3, M1_4x3);

            testNegate (MRsub_3x3, M1sub_3x3);
            testNegate (MRsub_3x3, M1sub_4x3);

            testNegate (MRsub_3x3, MAsub_3x3);
            testNegate (MRsub_3x3, MAsub_4x3);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testScale (MR_3x3, 1.23, M1_3x3);
            testScale (MR_3x3, 1.23, M1_4x3);

            testScale (MRsub_3x3, 1.23, M1sub_3x3);
            testScale (MRsub_3x3, 1.23, M1sub_4x3);

            testScale (MRsub_3x3, 1.23, MAsub_3x3);
            testScale (MRsub_3x3, 1.23, MAsub_4x3);

            testScaledAdd (MR_3x3, 1.23, M1_3x3, M2_3x3);
            testScaledAdd (MR_3x3, 1.23, MR_3x3, MR_3x3);

            testScaledAdd (MRsub_3x3, 1.23, M1sub_3x3, M2sub_3x3);
            testScaledAdd (MRsub_3x3, 1.23, M1sub_4x3, M2sub_4x3);

            testScaledAdd (MRsub_3x3, 1.23, MAsub_3x3, MBsub_3x3);
            testScaledAdd (MR_3x3, 1.23, MAsub_4x3, MBsub_4x3);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testSet (MR_3x3, M1_3x3);
            testSet (MR_3x3, M1_4x3);

            testSet (MRsub_3x3, M1sub_3x3);
            testSet (MRsub_3x3, M1sub_4x3);

            testSet (MRsub_3x3, MAsub_3x3);
            testSet (MRsub_3x3, MAsub_4x3);

            MR_3x3 = new MatrixNd (3, 3);
            MR_3x3.setRandom();

            testTranspose (MR_3x3, M1_3x3);
            testTranspose (MR_3x3, M1_4x3);
            testTranspose (MR_3x3, M1_3x4);

            testTranspose (MRsub_3x3, M1sub_3x3);
            testTranspose (MRsub_3x3, M1sub_4x3);
            testTranspose (MRsub_3x3, M1sub_3x4);

            testTranspose (MRsub_3x3, MAsub_3x3);
            testTranspose (MRsub_3x3, MAsub_4x3);
            testTranspose (MRsub_3x3, MAsub_3x4);

            int[] perm = new int[] { 0, 1, 2, 3 };
            testColumnPermutation (MRsub_4x4, perm);
            testRowPermutation (MRsub_4x4, perm);
            perm = new int[] { 0, 1, 3, 1 };
            testColumnPermutation (MRsub_4x4, perm);
            testRowPermutation (MRsub_4x4, perm);
            perm = new int[] { 0, 4, 1, 2 };
            testColumnPermutation (MRsub_4x4, perm);
            testRowPermutation (MRsub_4x4, perm);
            for (int i = 0; i < 10; i++) {
               perm = randomPermutation (9);
               testRowPermutation (MR_9x9, perm);
               testColumnPermutation (MR_9x9, perm);
               perm = randomPermutation (3);
               testColumnPermutation (MAsub_4x3, perm);
               testRowPermutation (MAsub_3x4, perm);
            }

            testNorms (M1_3x3);
            testNorms (M1sub_4x3);
            testNorms (M1sub_3x4);
            testNorms (M1_4x4);
         }

         for (int nr=1; nr<=8; nr++) {
            for (int nc=1; nc<=8; nc++) {
               testMulAdd (new MatrixNd (nr, nc));
            }
         }
      }
      catch (Exception e) {
         System.out.println ("n=" + n);
         e.printStackTrace();
         System.exit (1);
      }
   }

   public static void main (String[] args) {
      MatrixNdTest test = new MatrixNdTest();

      test.execute();

      System.out.println ("\nPassed\n");
   }
}
