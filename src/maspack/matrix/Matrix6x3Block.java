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
public class Matrix6x3Block extends Matrix6x3 implements MatrixBlock {
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
    * Creates a new Matrix6x3Block.
    */
   public Matrix6x3Block() {
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
      double x2 = x[xIdx + 2];

      y[yIdx + 0] += m00 * x0 + m01 * x1 + m02 * x2;
      y[yIdx + 1] += m10 * x0 + m11 * x1 + m12 * x2;
      y[yIdx + 2] += m20 * x0 + m21 * x1 + m22 * x2;
      y[yIdx + 3] += m30 * x0 + m31 * x1 + m32 * x2;
      y[yIdx + 4] += m40 * x0 + m41 * x1 + m42 * x2;
      y[yIdx + 5] += m50 * x0 + m51 * x1 + m52 * x2;
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];
      double x2 = x[xIdx + 2];
      double x3 = x[xIdx + 3];
      double x4 = x[xIdx + 4];
      double x5 = x[xIdx + 5];

      y[yIdx + 0] +=
         m00 * x0 + m10 * x1 + m20 * x2 + m30 * x3 + m40 * x4 + m50 * x5;
      y[yIdx + 1] +=
         m01 * x0 + m11 * x1 + m21 * x2 + m31 * x3 + m41 * x4 + m51 * x5;
      y[yIdx + 2] +=
         m02 * x0 + m12 * x1 + m22 * x2 + m32 * x3 + m42 * x4 + m52 * x5;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSIndices (
      int[] rowIdxs, int colOff, int[] offsets, Partition part) {
      return MatrixBlockBase.getBlockCRSIndices (
         this, rowIdxs, colOff, offsets, part);
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
         offsets[0] = off + 3;

         off = offsets[1];
         vals[off + 0] = m10;
         vals[off + 1] = m11;
         vals[off + 2] = m12;
         offsets[1] = off + 3;

         off = offsets[2];
         vals[off + 0] = m20;
         vals[off + 1] = m21;
         vals[off + 2] = m22;
         offsets[2] = off + 3;

         off = offsets[3];
         vals[off + 0] = m30;
         vals[off + 1] = m31;
         vals[off + 2] = m32;
         offsets[3] = off + 3;

         off = offsets[4];
         vals[off + 0] = m40;
         vals[off + 1] = m41;
         vals[off + 2] = m42;
         offsets[4] = off + 3;

         off = offsets[5];
         vals[off + 0] = m50;
         vals[off + 1] = m51;
         vals[off + 2] = m52;
         offsets[5] = off + 3;

         return 18;
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
         vals[off + 2] = m20;
         vals[off + 3] = m30;
         vals[off + 4] = m40;
         vals[off + 5] = m50;
         offsets[0] = off + 6;

         off = offsets[1];
         vals[off + 0] = m01;
         vals[off + 1] = m11;
         vals[off + 2] = m21;
         vals[off + 3] = m31;
         vals[off + 4] = m41;
         vals[off + 5] = m51;
         offsets[1] = off + 6;

         off = offsets[2];
         vals[off + 0] = m02;
         vals[off + 1] = m12;
         vals[off + 2] = m22;
         vals[off + 3] = m32;
         vals[off + 4] = m42;
         vals[off + 5] = m52;
         offsets[2] = off + 6;

         return 18;
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
   public Matrix3x6Block createTranspose() {
      Matrix3x6Block M = new Matrix3x6Block();
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix6x3Block clone() {
      Matrix6x3Block blk = (Matrix6x3Block)super.clone();
      blk.initBlockVariables();
      return blk;
   }
}
