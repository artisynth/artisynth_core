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
public class Matrix3x3Block extends Matrix3d implements MatrixBlock {
   
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
    * Creates a new Matrix3x3Block.
    */
   public Matrix3x3Block() {
      super();
      initBlockVariables();
   }

   /**
    * Creates a new Matrix3x3Block and initializes to the value of a Matrix3d
    *
    * @param M matrix to initialize from
    */
   public Matrix3x3Block(Matrix3d M) {
      super();
      initBlockVariables();
      set (M);
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
      y[yIdx + 2] += m02 * x0 + m12 * x1 + m22 * x2;
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix M) {
      if (M instanceof Matrix3dBase) {
         add ((Matrix3dBase)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);
         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
         m22 += M.get (2, 2);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix M) {
      if (M instanceof Matrix3dBase) {
         scaledAdd (s, (Matrix3dBase)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m12 += s * M.get (1, 2);
         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);
         m22 += s * M.get (2, 2);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix M) {
      if (M instanceof Matrix3dBase) {
         sub ((Matrix3dBase)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);
         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
         m22 -= M.get (2, 2);
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
   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part) {
      int off;

      if (part == Partition.UpperTriangular) {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m01;
         vals[off + 2] = m02;
         offsets[0] = off + 3;

         off = offsets[1];
         vals[off] = m11;
         vals[off + 1] = m12;
         offsets[1] = off + 2;

         off = offsets[2];
         vals[off] = m22;
         offsets[2] = off + 1;

         return 6;
      }
      else {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m01;
         vals[off + 2] = m02;
         offsets[0] = off + 3;

         off = offsets[1];
         vals[off] = m10;
         vals[off + 1] = m11;
         vals[off + 2] = m12;
         offsets[1] = off + 3;

         off = offsets[2];
         vals[off] = m20;
         vals[off + 1] = m21;
         vals[off + 2] = m22;
         offsets[2] = off + 3;

         return 9;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSValues (double[] vals, int[] offsets, Partition part) {
      int off;

      if (part == Partition.LowerTriangular) {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m10;
         vals[off + 2] = m20;
         offsets[0] = off + 3;

         off = offsets[1];
         vals[off] = m11;
         vals[off + 1] = m21;
         offsets[1] = off + 2;

         off = offsets[2];
         vals[off] = m22;
         offsets[2] = off + 1;

         return 6;
      }
      else {
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

         off = offsets[2];
         vals[off] = m02;
         vals[off + 1] = m12;
         vals[off + 2] = m22;
         offsets[2] = off + 3;

         return 9;
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
   public Matrix3x3Block createTranspose() {
      Matrix3x3Block M = new Matrix3x3Block();
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix3x3Block clone() {
      Matrix3x3Block blk = (Matrix3x3Block)super.clone();
      blk.initBlockVariables();
      return blk;
   }

}
