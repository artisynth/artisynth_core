/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Implements a 3 x 6 matrix block using two Matrix3d objects.
 */
public class Matrix2x6Block extends Matrix2x6 implements MatrixBlock {
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
    * Creates a new Matrix2x6Block.
    */
   public Matrix2x6Block() {
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
      double x2 = x[xIdx + 2];
      double x3 = x[xIdx + 3];
      double x4 = x[xIdx + 4];
      double x5 = x[xIdx + 5];

      y[yIdx + 0] +=
         m00 * x0 + m01 * x1 + m02 * x2 + m03 * x3 + m04 * x4 + m05 * x5;
      y[yIdx + 1] +=
         m10 * x0 + m11 * x1 + m12 * x2 + m13 * x3 + m14 * x4 + m15 * x5;
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];

      y[yIdx + 0] += m00 * x0 + m10 * x1;
      y[yIdx + 1] += m01 * x0 + m11 * x1;
      y[yIdx + 2] += m02 * x0 + m12 * x1;
      y[yIdx + 3] += m03 * x0 + m13 * x1;
      y[yIdx + 4] += m04 * x0 + m14 * x1;
      y[yIdx + 5] += m05 * x0 + m15 * x1;
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
         vals[off + 0] = m00;
         vals[off + 1] = m01;
         vals[off + 2] = m02;
         vals[off + 3] = m03;
         vals[off + 4] = m04;
         vals[off + 5] = m05;
         offsets[0] = off + 6;

         off = offsets[1];
         vals[off + 0] = m10;
         vals[off + 1] = m11;
         vals[off + 2] = m12;
         vals[off + 3] = m13;
         vals[off + 4] = m14;
         vals[off + 5] = m15;
         offsets[1] = off + 6;

         return 12;
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
         vals[off + 0] = m00;
         vals[off + 1] = m10;
         offsets[0] = off + 2;

         off = offsets[1];
         vals[off + 0] = m01;
         vals[off + 1] = m11;
         offsets[1] = off + 2;

         off = offsets[2];
         vals[off + 0] = m02;
         vals[off + 1] = m12;
         offsets[2] = off + 2;

         off = offsets[3];
         vals[off + 0] = m03;
         vals[off + 1] = m13;
         offsets[3] = off + 2;

         off = offsets[4];
         vals[off + 0] = m04;
         vals[off + 1] = m14;
         offsets[4] = off + 2;

         off = offsets[5];
         vals[off + 0] = m05;
         vals[off + 1] = m15;
         offsets[5] = off + 2;

         return 12;
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
   public Matrix6x2Block createTranspose() {
      Matrix6x2Block M = new Matrix6x2Block();
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix2x6Block clone() {
      Matrix2x6Block blk = (Matrix2x6Block)super.clone();
      blk.initBlockVariables();
      return blk;
   }
}
