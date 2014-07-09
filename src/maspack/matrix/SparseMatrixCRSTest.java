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

import maspack.matrix.Matrix.Partition;

class SparseMatrixCRSTest extends MatrixTest {
   void add (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).add ((SparseMatrixCRS)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixCRS)MR).add ((SparseMatrixCRS)M1, (SparseMatrixCRS)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).sub ((SparseMatrixCRS)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixCRS)MR).sub ((SparseMatrixCRS)M1, (SparseMatrixCRS)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).mul ((SparseMatrixCRS)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixCRS)MR).mul ((SparseMatrixCRS)M1, (SparseMatrixCRS)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).mulTranspose ((SparseMatrixCRS)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixCRS)MR).mulTransposeRight (
         (SparseMatrixCRS)M1, (SparseMatrixCRS)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixCRS)MR).mulTransposeLeft (
         (SparseMatrixCRS)M1, (SparseMatrixCRS)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((SparseMatrixCRS)MR).mulTransposeBoth (
         (SparseMatrixCRS)M1, (SparseMatrixCRS)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).transpose ((SparseMatrixCRS)M1);
   }

   void transpose (Matrix MR) {
      ((SparseMatrixCRS)MR).transpose();
   }

   void negate (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).negate ((SparseMatrixCRS)M1);
   }

   void negate (Matrix MR) {
      ((SparseMatrixCRS)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((SparseMatrixCRS)MR).scale (s, (SparseMatrixCRS)M1);
   }

   void scale (Matrix MR, double s) {
      ((SparseMatrixCRS)MR).scale (s);
   }

   void setZero (Matrix MR) {
      ((SparseMatrixCRS)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((SparseMatrixCRS)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((SparseMatrixCRS)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((SparseMatrixCRS)MR).set (M1);
   }

   void testSetSize (SparseMatrixCRS MR, int nrows, int ncols) {
      saveResult (MR);
      eExpected = setSizeCheck (MX, MR, nrows, ncols);
      try {
         MR.setSize (nrows, ncols);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (MR);
      if (eActual == null) {
         MR.checkConsistency ();
      }
   }

   void testCapacity (SparseMatrixCRS M) {
      SparseMatrixCRS MC = new SparseMatrixCRS (M);
      M.trimCapacity();
      M.checkConsistency();
      checkResult (MC, M, "trimCapacity");
      M.ensureCapacity(2*M.getCapacity());
      M.checkConsistency();
      checkResult (MC, M, "ensureCapacity");
      M.trimCapacity();
      M.checkConsistency();
      checkResult (MC, M, "trimCapacity");
   }

   Exception setSizeCheck (
      MatrixNd MX, SparseMatrixCRS MR, int nrows, int ncols) {
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

   private void jumbleArrays (int[] idxs, double[] vals, int off, int end) {
      
      int cnt = end-off;
      for (int i=0; i<cnt; i++) {
         int k = off+randGen.nextInt(cnt);
         int l = off+randGen.nextInt(cnt);
         if (k != l) {
            int tmpi = idxs[k];
            idxs[k] = idxs[l];
            idxs[l] = tmpi;
            double tmpv = vals[k];
            vals[k] = vals[l];
            vals[l] = tmpv;
         }
      }
   }

   public void testMisc (SparseMatrixCRS MR) {

      saveResult (MR);

      double[] vals;
      int[] rowOffs;
      int[] cols;
      int[] indices;

      MatrixNd MC = createRandomValues (MR, /*symmetric=*/false);
      // ensure that diagonals are all set
      for (int i=0; i<Math.min(MC.rowSize(), MC.colSize()); i++) {
         MC.set (i, i, MC.get(i,i) + 10);
      }
      
      SparseMatrixCell[] cells;
      cells = extractCells (MC);
      jumbleCells (cells);
      indices = extractIndices (cells);
      vals = extractValues (cells);

      MR.set (vals, indices, vals.length);
      
      int nnz = MR.numNonZeroVals();
      if (nnz != vals.length) {
         throw new TestException ("Inconsistent number of non-zeros");
      }
      rowOffs = new int[MR.rowSize()+1];

      SparseMatrixCRS I = new SparseMatrixCRS (MR.rowSize(), MR.colSize());
      I.setIdentity();

      nnz = MR.numNonZeroVals();
      cols = new int[nnz];
      vals = new double[nnz];

      MR.getCRSIndices (cols, rowOffs, Partition.Full);
      MR.getCRSValues (vals, Partition.Full);

      SparseMatrixCRS MN = new SparseMatrixCRS (vals, cols, rowOffs);

      if (MN.rowSize() == MC.rowSize() && MN.colSize() == MC.colSize()) {
         checkResult (MN, MC, "Construction from CRS");
      }

      MR.add (I); // do this to cause the matrix to be unsorted
      MR.sub (I);
      MR.getNonzeroElements (vals);
      MR.add (I); // do this to cause the matrix to be unsorted
      MR.sub (I);
      MR.setNonzeroElements (vals);
      checkResult (MR, MC, "set/getNonzeroElements");

      for (int i=0; i<MR.rowSize(); i++) {
         for (int j=0; j<MR.colSize(); j++) {
            double val = MR.get (i,j);
            if (val != 0) {
               MR.set (i, j, val);
            }
         }
      }
      checkResult (MR, MC, "set(i,j,val)");

      MN = new SparseMatrixCRS (MR);
      MR.setZero();
      MR.trimCapacity();
      MR.negate (MN);
      MR.negate();

      checkResult (MR, MC, "construct,negate,negate");

      MR.getCRSIndices (cols, rowOffs, Partition.Full);
      MR.getCRSValues (vals, Partition.Full);
      for (int i=0; i<MR.rowSize(); i++) {
         jumbleArrays (cols, vals, rowOffs[i]-1, rowOffs[i+1]-1);
      }
      MR.setCRSValues (vals, cols, rowOffs);
      checkResult (MR, MC, "setting unsorted CRS");

      restoreResult (MR);
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      SparseMatrixCRS M1_3x3 = new SparseMatrixCRS (3, 3);
      SparseMatrixCRS M1_9x9 = new SparseMatrixCRS (9, 9);
      SparseMatrixCRS M2_9x9 = new SparseMatrixCRS (9, 9);
      SparseMatrixCRS MR_9x9 = new SparseMatrixCRS (9, 9);

      SparseMatrixCRS MR_11x9 = new SparseMatrixCRS (11, 9);
      SparseMatrixCRS M1_11x9 = new SparseMatrixCRS (11, 9);
      SparseMatrixCRS M2_11x9 = new SparseMatrixCRS (11, 9);

      SparseMatrixCRS MR_9x11 = new SparseMatrixCRS (9, 11);
      SparseMatrixCRS M1_9x11 = new SparseMatrixCRS (9, 11);
      SparseMatrixCRS M2_9x11 = new SparseMatrixCRS (9, 11);

      M1_3x3.set (
         new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
         new int[] { 0, 0, 0, 1, 0, 2,
                     1, 0, 1, 1, 1, 2,
                     2, 0, 2, 1, 2, 2 }, 9);

      SparseMatrixCRS MR_3x3 = new SparseMatrixCRS (3, 3);
      testScan (MR_3x3, "[ 1 2 3 ; 4 5 6 ; 7 8 9 ] ", M1_3x3);

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

      M1_3x3.set (
         new double[] { 1, 5, 9 }, new int[] { 0, 0, 1, 1, 2, 2 }, 3);

      testScan (MR_3x3, "[ (0 0 1) (1 1 5) (2 2 9) ]", M1_3x3);

      for (int i=0; i<10; i++) {
         testGeneric (MR_3x3);
         testGeneric (MR_9x9);
         testGeneric (M1_11x9);
         testGeneric (M1_9x11);

         testMisc (MR_3x3);
         testMisc (MR_11x9);
         testMisc (MR_9x11);
      }

      // testGeneric (MR_3x3);

      testSetZero (MR_9x9);
      testSetZero (MR_11x9);
      testSetZero (MR_9x11);

      testSetIdentity (MR_9x9);
      testSetIdentity (MR_11x9);
      MR_11x9.setZero();
      MR_11x9.trimCapacity();
      testSetIdentity (MR_11x9);
      testSetIdentity (MR_9x11);

      testSetDiagonal (MR_9x9, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
      MR_11x9.setZero();
      MR_11x9.trimCapacity();
      testSetDiagonal (MR_11x9, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
      testSetDiagonal (MR_9x11, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

      for (int i = 0; i < 10; i++) {
         SparseMatrixCRS M0 = new SparseMatrixCRS (4, 5);
         M0.setRandom();
         M0.checkConsistency();
         SparseMatrixCRS MR = new SparseMatrixCRS (M0);
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

            MR_9x9.setRandom();

            M1_9x9.setRandom();
            M1_11x9.setRandom();
            M1_9x11.setRandom();
            M2_9x9.setRandom();
            M2_11x9.setRandom();
            M2_9x11.setRandom();

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
            SparseMatrixCRS MR_11x11 = new SparseMatrixCRS (11, 11);
            MR_11x11.setRandom();

            M1_9x9.add (M1_9x9); // do this to make unsorted
            M2_11x9.add (M2_11x9); // do this to make unsorted

            testMul (MR_9x9, M1_9x9, M2_9x9);
            testMul (MR_9x9, M1_9x11, M2_11x9);
            testMul (MR_11x11, M1_9x11, M2_11x9);
            testMul (MR_11x11, M1_9x11, M2_9x11);

            SparseMatrixCRS M1_20x20 = new SparseMatrixCRS (20, 20);
            SparseMatrixCRS MR_20x20 = new SparseMatrixCRS (20, 20);
            M1_20x20.setRandom();
            testMul (MR_20x20, M1_20x20, M1_20x20);

            testCapacity (MR_20x20);

            testTranspose (MR_9x9, MR_9x9);
            testTranspose (MR_9x11, MR_9x11);
            testTranspose (MR_9x9, MR_9x11);
            testTranspose (MR_9x9, M1_11x9);
            testTranspose (MR_9x9, M1_9x9);
            testTranspose (MR_9x9, M1_9x11);

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
         }
      }
      catch (Exception e) {
         System.out.println ("n=" + n);
         e.printStackTrace();
         System.exit (1);
      }
   }

   public static void main (String[] args) {
      SparseMatrixCRSTest test = new SparseMatrixCRSTest();

      test.execute();

      System.out.println ("\nPassed\n");
   }
}
