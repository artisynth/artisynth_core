/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 3 x 3 matrix block using a single Matrix3d object.
 */
public class MatrixNdBlock extends MatrixNd implements MatrixBlock {
   
   private static final long serialVersionUID = 1L;
   
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
    * Creates a new MatrixNdBlock.
    */
   public MatrixNdBlock() {
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
    * Creates a new MatrixNdBlock with a specificed number of rows and columns.
    */
   public MatrixNdBlock (int nrows, int ncols) {
      super (nrows, ncols);
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double[] buf = getBuffer();
      int w = getBufferWidth();
      int b = getBufferBase();

      for (int i = 0; i < rowSize(); i++) {
         double sum = 0;
         for (int j = 0; j < colSize(); j++) {
            sum += buf[i * w + j + b] * x[xIdx + j];
         }
         y[yIdx + i] += sum;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double[] buf = getBuffer();
      int w = getBufferWidth();
      int b = getBufferBase();

      for (int j = 0; j < colSize(); j++) {
         double sum = 0;
         for (int i = 0; i < rowSize(); i++) {
            sum += buf[i * w + j + b] * x[xIdx + i];
         }
         y[yIdx + j] += sum;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix M) {
      if (M instanceof MatrixNd) {
         add ((MatrixNd)M);
      }
      else {
         if (M.rowSize() != rowSize() || M.colSize() != colSize()) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         double[] buf = getBuffer();
         int w = getBufferWidth();
         int b = getBufferBase();
         for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < colSize(); j++) {
               buf[i * w + j + b] += M.get (i, j);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix M) {
      if (M instanceof MatrixNd) {
         scaledAdd (s, (MatrixNd)M);
      }
      else {
         if (M.rowSize() != rowSize() || M.colSize() != colSize()) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         double[] buf = getBuffer();
         int w = getBufferWidth();
         int b = getBufferBase();
         for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < colSize(); j++) {
               buf[i * w + j + b] += s * M.get (i, j);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix M) {
      if (M instanceof MatrixNd) {
         sub ((MatrixNd)M);
      }
      else {
         if (M.rowSize() != rowSize() || M.colSize() != colSize()) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         double[] buf = getBuffer();
         int w = getBufferWidth();
         int b = getBufferBase();
         for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < colSize(); j++) {
               buf[i * w + j + b] -= M.get (i, j);
            }
         }
      }
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
      boolean upperTriangular = MatrixBase.checkUpperTriangular (part);
      double[] buf = getBuffer();
      int w = getBufferWidth();
      int b = getBufferBase();
      for (int i = 0; i < rowSize(); i++) {
         int off = offsets[i];
         for (int j = upperTriangular ? i : 0; j < colSize(); j++) {
            vals[off++] = buf[i * w + j + b];
         }
         offsets[i] = off;
      }
      return MatrixBlockBase.numNonZeros (rowSize(), colSize(), part);
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
      boolean lowerTriangular = MatrixBase.checkLowerTriangular (part);
      double[] buf = getBuffer();
      int w = getBufferWidth();
      int b = getBufferBase();
      for (int j = 0; j < colSize(); j++) {
         int off = offsets[j];
         for (int i = lowerTriangular ? j : 0; i < rowSize(); i++) {
            vals[off++] = buf[i * w + j + b];
         }
         offsets[j] = off;
      }
      return MatrixBlockBase.numNonZeros (rowSize(), colSize(), part);      
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
   public MatrixNdBlock createTranspose() {
      MatrixNdBlock M = new MatrixNdBlock (colSize(), rowSize());
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public MatrixNdBlock clone() {
      MatrixNdBlock blk;
      try {
         blk = (MatrixNdBlock)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for " + getClass());
      }
      blk.initBlockVariables();
      return blk;
   }

}
