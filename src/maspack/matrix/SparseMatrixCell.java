/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

public class SparseMatrixCell {
   public int i;
   public int j;
   public double value;

   public SparseMatrixCell next = null;
   public SparseMatrixCell down = null;

   protected SparseMatrixCell() {
   }

   public SparseMatrixCell (int i, int j, double value) {
      this.i = i;
      this.j = j;
      this.value = value;
   }

   public SparseMatrixCell (SparseMatrixCell cell) {
      this.i = cell.i;
      this.j = cell.j;
      this.value = cell.value;
   }

   public void transpose() {
      int itmp;
      SparseMatrixCell ctmp;

      // swap i and j
      itmp = i;
      i = j;
      j = itmp;

      // swap next and down
      ctmp = next;
      next = down;
      down = ctmp;
   }

   public String toString() {
      return "(" + i + "," + j + "," + value + ")";
   }
}
