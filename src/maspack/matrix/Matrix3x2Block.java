/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Implements a 3 x 2 matrix block
 */
public class Matrix3x2Block extends Matrix3x2 implements MatrixBlock {
   protected MatrixBlock myNext;
   protected MatrixBlock myDown;

   protected int myBlkRow;
   protected int myBlkCol;
   protected int myRowOff;
   protected int myColOff;

   protected int myNumber;

   private void initBlockVariables() {
      myNext = null;
      myDown = null;
      myBlkRow = -1;
      myBlkCol = -1;
      myRowOff = -1;
      myColOff = -1;
      myNumber = -1;
   }

   /**
    * Creates a new Matrix3x2Block.
    */
   public Matrix3x2Block() {
      super();
      initBlockVariables();
   }

   /**
    * {@inheritDoc}
    */
   public MatrixBlock next() {
      return myNext;
   }

   /**
    * {@inheritDoc}
    */
   public void setNext (MatrixBlock blk) {
      myNext = blk;
   }

   /**
    * {@inheritDoc}
    */
   public MatrixBlock down() {
      return myDown;
   }

   /**
    * {@inheritDoc}
    */
   public void setDown (MatrixBlock blk) {
      myDown = blk;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockRow() {
      return myBlkRow;
   }

   /**
    * {@inheritDoc}
    */
   public void setBlockRow (int blkRow) {
      myBlkRow = blkRow;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCol() {
      return myBlkCol;
   }

   /**
    * {@inheritDoc}
    */
   public void setBlockCol (int blkCol) {
      myBlkCol = blkCol;
   }

   // /**
   // * {@inheritDoc}
   // */
   // public int getRowOffset()
   // {
   // return myRowOff;
   // }
   //
   // /**
   // * {@inheritDoc}
   // */
   // public void setRowOffset(int off)
   // {
   // myRowOff = off;
   // }
   //
   // /**
   // * {@inheritDoc}
   // */
   // public int getColOffset()
   // {
   // return myColOff;
   // }
   //
   // /**
   // * {@inheritDoc}
   // */
   // public void setColOffset(int off)
   // {
   // myColOff = off;
   // }

   /**
    * {@inheritDoc}
    */
   public int getBlockNumber() {
      return myNumber;
   }

   /**
    * {@inheritDoc}
    */
   public void setBlockNumber (int num) {
      myNumber = num;
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];

      y[yIdx + 0] += m00 * x0 + m01 * x1;
      y[yIdx + 1] += m10 * x0 + m11 * x1;
      y[yIdx + 2] += m20 * x0 + m21 * x1;
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];
      double x2 = x[xIdx + 2];

      y[yIdx + 0] += m00 * x0 + m10 * x1 + m20 * x2;
      y[yIdx + 1] += m01 * x0 + m11 * x1 + m21 * x2;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part) {
      return MatrixBlockBase.getBlockCRSIndices (
         this, colIdxs, colOff, offsets, part);
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByRow (int[] offsets, int idx, Partition part) {
      MatrixBlockBase.addNumNonZerosByRow (this, offsets, idx, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part) {
      int off;

      if (part == Partition.Full) {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m01;
         offsets[0] = off + 2;

         off = offsets[1];
         vals[off] = m10;
         vals[off + 1] = m11;
         offsets[1] = off + 2;

         off = offsets[2];
         vals[off] = m20;
         vals[off + 1] = m21;
         offsets[2] = off + 2;

         return 6;
      }
      else {
         throw new UnsupportedOperationException (
            "partition " + part + " not supported");
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part) {
      return MatrixBlockBase.getBlockCCSIndices (
         this, rowIdxs, rowOff, offsets, part);
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByCol (int[] offsets, int idx, Partition part) {
      MatrixBlockBase.addNumNonZerosByCol (this, offsets, idx, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSValues (double[] vals, int[] offsets, Partition part) {
      int off;

      if (part == Partition.Full) {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m10;
         vals[off + 2] = m20;
         offsets[0] = off + 3;

         off = offsets[1];
         vals[off] = m01;
         vals[off + 1] = m11;
         vals[off + 2] = m21;
         offsets[1] = off + 3;

         return 6;
      }
      else {
         throw new UnsupportedOperationException (
            "partition " + part + " not supported");
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean valueIsNonZero (int i, int j) {
      return true;
   }

   /**
    * Creates a transpose of this matrix block.
    */
   public Matrix2x3Block createTranspose() {
      Matrix2x3Block M = new Matrix2x3Block();
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix3x2Block clone() {
      Matrix3x2Block blk = (Matrix3x2Block)super.clone();
      blk.initBlockVariables();
      return blk;
   }
}
