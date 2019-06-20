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
public class Matrix1x3Block extends Matrix1x3 implements MatrixBlock {
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
    * Creates a new Matrix1x3Block.
    */
   public Matrix1x3Block() {
      super();
      initBlockVariables();
   }

   /**
    * Creates a new Matrix1x3Block initialized by a 3-vector.
    *
    * @param vec vector to initialized this matrix.
    */
   public Matrix1x3Block(Vector3d vec) {
      this();
      set (vec);
   }

   /**
    * {@inheritDoc}
    */
   public final MatrixBlock next() {
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
   public final MatrixBlock down() {
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

      y[yIdx + 0] += m00 * x0 + m01 * x1 + m02 * x2;
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];

      y[yIdx + 0] += m00 * x0;
      y[yIdx + 1] += m01 * x0;
      y[yIdx + 2] += m02 * x0;
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
         vals[off++] = m00;
         vals[off++] = m01;
         vals[off++] = m02;
         offsets[0] = off;
         return 3;
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
      if (part == Partition.Full) {
         vals[offsets[0]++] = m00;
         vals[offsets[1]++] = m01;
         vals[offsets[2]++] = m02;
         return 3;
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
   public Matrix3x1Block createTranspose() {
      Matrix3x1Block M = new Matrix3x1Block();
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix1x3Block clone() {
      Matrix1x3Block blk = (Matrix1x3Block)super.clone();
      blk.initBlockVariables();
      return blk;
   }
}
