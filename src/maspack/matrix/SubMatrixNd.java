/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

public class SubMatrixNd extends MatrixNd {
   private static final long serialVersionUID = 1L;
   MatrixNd Mroot;
   int rowBase; // base row index WRT the root matrix
   int colBase; // base column index WRT the root matrix

   public boolean isSubMatrix() {
      return true;
   }

   public SubMatrixNd() {
      Mroot = null;
      rowBase = 0;
      colBase = 0;
   }

   public SubMatrixNd (int row0, int col0, int numRows, int numCols,
   MatrixNd Mparent) {
      // subMatrix = true;
      fixedSize = true;
      setDimensions (row0, col0, numRows, numCols, Mparent);
   }

   void resetDimensions (int row0, int col0, int numRows, int numCols) {
      rowBase += row0;
      colBase += col0;
      base = rowBase * width + colBase;
      this.nrows = numRows;
      this.ncols = numCols;
      storageFilled = (buf.length == numRows * numCols);
   }

   public void setDimensions (
      int row0, int col0, int numRows, int numCols, MatrixNd Mparent)
      throws ImproperSizeException {
      int newRowBase, newColBase;
      MatrixNd newMroot;

      if (numRows < 0 || numCols < 0) {
         throw new ImproperSizeException ("Negative dimensions");
      }
      if (Mparent.isSubMatrix()) {
         SubMatrixNd Sparent = (SubMatrixNd)Mparent;
         newRowBase = row0 + Sparent.rowBase;
         newColBase = col0 + Sparent.colBase;
         newMroot = Sparent.Mroot;
      }
      else {
         newRowBase = row0;
         newColBase = col0;
         newMroot = Mparent;
      }
      if (newRowBase + numRows > newMroot.nrows ||
          newColBase + numCols > newMroot.ncols ||
          newRowBase < 0 ||
          newColBase < 0) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      if (newMroot != Mroot) {
         if (Mroot != null) {
            Mroot.dereferenceSubMatrix();
         }
         newMroot.referenceSubMatrix();
         Mroot = newMroot;
      }
      colBase = newColBase;
      rowBase = newRowBase;
      buf = Mroot.buf;
      width = Mroot.width;
      base = rowBase * width + colBase;
      this.nrows = numRows;
      this.ncols = numCols;
      storageFilled = (buf.length == numRows * numCols);
   }

   public void clear() {
      if (Mroot != null) {
         Mroot.dereferenceSubMatrix();
         Mroot = null;
         nrows = 0;
         ncols = 0;
         buf = null;
      }
   }

   public void finalize() {
      clear();
   }

   public MatrixNd clone() throws CloneNotSupportedException {
      throw new CloneNotSupportedException();
   }
}
