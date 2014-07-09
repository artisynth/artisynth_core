/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

public class SparseVectorCell {
   public int i;
   public double value;

   SparseVectorCell next = null;

   public SparseVectorCell next() {
      return next;
   }
   
   public SparseVectorCell (int i, double value) {
      this.i = i;
      this.value = value;
   }

   public SparseVectorCell (SparseVectorCell cell) {
      this.i = cell.i;
      this.value = cell.value;
   }

   public String toString() {
      return "(" + i + "," + value + ")";
   }
}
