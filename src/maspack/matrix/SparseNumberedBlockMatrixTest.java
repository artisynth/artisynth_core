/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

public class SparseNumberedBlockMatrixTest extends UnitTest {

   public void test(int initialCapacity) {
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
   }

   public void test() {
      SparseBlockMatrix.warningLevel = 0;
      test(0);
      test(4);
      test(40);
   }

   public static void main (String[] args) {

      SparseNumberedBlockMatrixTest tester = new SparseNumberedBlockMatrixTest();
      tester.runtest();
   }
}
