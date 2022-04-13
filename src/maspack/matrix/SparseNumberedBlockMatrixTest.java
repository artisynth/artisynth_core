/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

public class SparseNumberedBlockMatrixTest extends SparseBlockMatrixTest {

   SparseNumberedBlockMatrix createMatrix (int[] rowSizes, int[] colSizes) {
      return new SparseNumberedBlockMatrix (rowSizes, colSizes);
   }

   SparseNumberedBlockMatrix createMatrix () {
      return new SparseNumberedBlockMatrix (new int[0], new int[0]);
   }

   public void specialTest(int initialCapacity) {
      int[] sizes = new int[] {
         3, 3, 3, 3, 3, 3};
      SparseNumberedBlockMatrix M =
         new SparseNumberedBlockMatrix(sizes, sizes, initialCapacity);

      M.checkConsistency();
      M.addBlock (0, 0, new Matrix3x3Block());
      M.addBlock (1, 1, new Matrix3x3Block());
      M.addBlock (3, 3, new Matrix3x3Block());
      M.checkConsistency();
      M.addBlock (3, 4, new Matrix3x3Block());
      M.addBlock (3, 5, new Matrix3x3Block());
      M.addBlock (5, 4, new Matrix3x3Block());
      M.addBlock (5, 5, new Matrix3x3Block());
      M.checkConsistency();
      M.removeBlock (1, 1);
      M.removeBlock (3, 5);
      M.checkConsistency();
      M.addBlock (3, 0, new Matrix3x3Block());
      M.addBlock (3, 1, new Matrix3x3Block());
      M.removeBlock (5, 4);
      M.checkConsistency();
      M.addBlock (3, 0, new Matrix3x3Block());
      M.addBlock (3, 0, new Matrix3x3Block());
      M.checkConsistency();

      SparseNumberedBlockMatrix C = M.clone();
      C.checkConsistency();

      M = new SparseNumberedBlockMatrix(sizes, sizes, initialCapacity);
      M.addBlock (0, 0, new Matrix3x3Block());
      M.addBlock (0, 1, new Matrix3x3Block());
      M.addBlock (1, 1, new Matrix3x3Block());
      M.addBlock (1, 2, new Matrix3x3Block());
      M.addBlock (2, 1, new Matrix3x3Block());
      M.addBlock (2, 2, new Matrix3x3Block());
      M.addBlock (2, 3, new Matrix3x3Block());
      M.addBlock (3, 3, new Matrix3x3Block());
      M.removeCol (2);
      M.checkConsistency();
      M.removeRow (2);
      M.checkConsistency();
   }

   void testAddBlock (
      SparseNumberedBlockMatrix S, int bi, int bj, MatrixBlock blk) {
      S.addBlock (bi, bj, blk);
      S.checkConsistency();
   }

   void testAddBlock() {
      SparseNumberedBlockMatrix S = createMatrix();

      testAddBlock (S, 0, 0, new Matrix2x2Block());
      testAddBlock (S, 0, 1, new Matrix2x3Block());
      testAddBlock (S, 1, 0, new Matrix3x2Block());
      testAddBlock (S, 1, 1, new Matrix3x3Block());
      testAddBlock (S, 1, 2, new Matrix3x6Block());
      testAddBlock (S, 2, 1, new Matrix6x3Block());
      testAddBlock (S, 2, 2, new Matrix6dBlock());
      testAddBlock (S, 2, 3, new MatrixNdBlock (6, 4));
      testAddBlock (S, 3, 2, new MatrixNdBlock (4, 6));
      testAddBlock (S, 3, 3, new MatrixNdBlock (4, 4));
      testAddBlock (S, 4, 1, new Matrix1x3Block());
      testAddBlock (S, 4, 2, new Matrix1x6Block());
      testAddBlock (S, 1, 4, new Matrix3x1Block());
      testAddBlock (S, 2, 4, new Matrix6x1Block());
      testAddBlock (S, 5, 5, new Matrix6dBlock());
      testAddBlock (S, 2, 5, new Matrix6dBlock());
      testAddBlock (S, 5, 2, new Matrix6dBlock());
      testAddBlock (S, 6, 6, new Matrix3x3DiagBlock());
      testAddBlock (S, 6, 1, new Matrix3x3DiagBlock());
      testAddBlock (S, 1, 6, new Matrix3x3DiagBlock());
      testAddBlock (S, 7, 1, new Matrix4x3Block());
      testAddBlock (S, 1, 7, new Matrix3x4Block());
   }

   public void test() {
      testAddBlock ();
      super.test();
      SparseBlockMatrix.warningLevel = 0;
      specialTest (0);
      specialTest (4);
      specialTest (40);
   }

   public static void main (String[] args) {

      SparseNumberedBlockMatrixTest tester = new SparseNumberedBlockMatrixTest();
      tester.runtest();
   }
}
