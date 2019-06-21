/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Implements a 3 x 3 matrix block using a single Matrix3d object.
 */
public class Matrix3x3DiagBlock extends Matrix3x3Block {

   private static final long serialVersionUID = 1L;

   /**
    * Creates a new Matrix3x3DiagBlock.
    */
   public Matrix3x3DiagBlock() {
      super();
   }

   /**
    * Creates a new Matrix3x3DiagBlock with specufied diagnonal elements.
    */
   public Matrix3x3DiagBlock(double m00, double m11, double m22) {
      super();
      set (m00, m11, m22);
   }

   public void set (double m00, double m11, double m22) {
      this.m00 = m00;
      this.m11 = m11;
      this.m22 = m22;
   }

   public void set (int i, int j, double val) {
      if (i == j) {
         switch (i) {
            case 0:
               m00 = val;
               return;
            case 1:
               m11 = val;
               return;
            case 2:
               m22 = val;
               return;
            default:
               throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean valueIsNonZero (int i, int j) {
      return i == j;
   }

   /**
    * {@inheritDoc}
    */
   public int numNonZeroVals() {
      return 3;
   }

   /**
    * {@inheritDoc}
    */
   public int numNonZeroVals (Partition part, int numRows, int numCols) {
      if (numRows > 3 || numCols > 3) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      if (numRows > numCols) {
         return numCols;
      }
      else {
         return numRows;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (double[] y, int yIdx, double[] x, int xIdx) {
      y[yIdx + 0] += m00 * x[xIdx + 0];
      y[yIdx + 1] += m11 * x[xIdx + 1];
      y[yIdx + 2] += m22 * x[xIdx + 2];
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      y[yIdx + 0] += m00 * x[xIdx + 0];
      y[yIdx + 1] += m11 * x[xIdx + 1];
      y[yIdx + 2] += m22 * x[xIdx + 2];
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part) {

      colIdxs[offsets[0]++] = colOff;
      colIdxs[offsets[1]++] = colOff+1;
      colIdxs[offsets[2]++] = colOff+2;
      return 3;
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByRow (int[] offsets, int idx, Partition part) {
      offsets[idx]++;
      offsets[idx+1]++;
      offsets[idx+2]++;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part) {

      rowIdxs[offsets[0]++] = rowOff;
      rowIdxs[offsets[1]++] = rowOff+1;
      rowIdxs[offsets[2]++] = rowOff+2;
      return 3;
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByCol (int[] offsets, int idx, Partition part) {
      offsets[idx]++;
      offsets[idx+1]++;
      offsets[idx+2]++;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part) {

      vals[offsets[0]++] = m00;
      vals[offsets[1]++] = m11;
      vals[offsets[2]++] = m22;
      return 3;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSValues (double[] vals, int[] offsets, Partition part) {
      vals[offsets[0]++] = m00;
      vals[offsets[1]++] = m11;
      vals[offsets[2]++] = m22;
      return 3;
   }

}
