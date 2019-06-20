/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Implements a 4 x 4 matrix block using a single Matrix4d object.
 */
public class Matrix4x4Block extends Matrix4d implements MatrixBlock {
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
    * Creates a new Matrix4x4Block.
    */
   public Matrix4x4Block() {
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
      double x3 = x[xIdx + 3];

      y[yIdx + 0] += m00 * x0 + m01 * x1 + m02 * x2 + m03 * x3;
      y[yIdx + 1] += m10 * x0 + m11 * x1 + m12 * x2 + m13 * x3;
      y[yIdx + 2] += m20 * x0 + m21 * x1 + m22 * x2 + m23 * x3;
      y[yIdx + 3] += m30 * x0 + m31 * x1 + m32 * x2 + m33 * x3;
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];
      double x2 = x[xIdx + 2];
      double x3 = x[xIdx + 3];

      y[yIdx + 0] += m00 * x0 + m10 * x1 + m20 * x2 + m30 * x3;
      y[yIdx + 1] += m01 * x0 + m11 * x1 + m21 * x2 + m31 * x3;
      y[yIdx + 2] += m02 * x0 + m12 * x1 + m22 * x2 + m32 * x3;
      y[yIdx + 3] += m03 * x0 + m13 * x1 + m23 * x2 + m33 * x3;
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix M) {
      if (M instanceof Matrix4dBase) {
         add ((Matrix4dBase)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m03 += M.get (0, 3);

         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);
         m13 += M.get (1, 3);

         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
         m22 += M.get (2, 2);
         m23 += M.get (2, 3);

         m30 += M.get (3, 0);
         m31 += M.get (3, 1);
         m32 += M.get (3, 2);
         m33 += M.get (3, 3);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix M) {
      if (M instanceof Matrix4dBase) {
         scaledAdd (s, (Matrix4dBase)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
         m03 += s * M.get (0, 3);

         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m12 += s * M.get (1, 2);
         m13 += s * M.get (1, 3);

         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);
         m22 += s * M.get (2, 2);
         m23 += s * M.get (2, 3);

         m30 += s * M.get (3, 0);
         m31 += s * M.get (3, 1);
         m32 += s * M.get (3, 2);
         m33 += s * M.get (3, 3);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix M) {
      if (M instanceof Matrix4dBase) {
         sub ((Matrix4dBase)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m03 -= M.get (0, 3);

         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);
         m13 -= M.get (1, 3);

         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
         m22 -= M.get (2, 2);
         m23 -= M.get (2, 3);

         m30 -= M.get (3, 0);
         m31 -= M.get (3, 1);
         m32 -= M.get (3, 2);
         m33 -= M.get (3, 3);
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
         vals[off + 3] = m03;
         offsets[0] = off + 4;

         off = offsets[1];
         vals[off] = m11;
         vals[off + 1] = m12;
         vals[off + 2] = m13;
         offsets[1] = off + 3;

         off = offsets[2];
         vals[off] = m22;
         vals[off + 1] = m23;
         offsets[2] = off + 2;

         off = offsets[3];
         vals[off] = m33;
         offsets[3] = off + 1;

         return 10;
      }
      else {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m01;
         vals[off + 2] = m02;
         vals[off + 3] = m03;
         offsets[0] = off + 4;

         off = offsets[1];
         vals[off] = m10;
         vals[off + 1] = m11;
         vals[off + 2] = m12;
         vals[off + 3] = m13;
         offsets[1] = off + 4;

         off = offsets[2];
         vals[off] = m20;
         vals[off + 1] = m21;
         vals[off + 2] = m22;
         vals[off + 3] = m23;
         offsets[2] = off + 4;

         off = offsets[3];
         vals[off] = m30;
         vals[off + 1] = m31;
         vals[off + 2] = m32;
         vals[off + 3] = m33;
         offsets[3] = off + 4;

         return 16;
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
         vals[off + 3] = m30;
         offsets[0] = off + 4;

         off = offsets[1];
         vals[off] = m11;
         vals[off + 1] = m21;
         vals[off + 2] = m31;
         offsets[1] = off + 3;

         off = offsets[2];
         vals[off] = m22;
         vals[off + 1] = m32;
         offsets[2] = off + 2;

         off = offsets[3];
         vals[off] = m33;
         offsets[3] = off + 1;

         return 10;
      }
      else {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m10;
         vals[off + 2] = m20;
         vals[off + 3] = m30;
         offsets[0] = off + 4;

         off = offsets[1];
         vals[off] = m01;
         vals[off + 1] = m11;
         vals[off + 2] = m21;
         vals[off + 3] = m31;
         offsets[1] = off + 4;

         off = offsets[2];
         vals[off] = m02;
         vals[off + 1] = m12;
         vals[off + 2] = m22;
         vals[off + 3] = m32;
         offsets[2] = off + 4;

         off = offsets[3];
         vals[off] = m03;
         vals[off + 1] = m13;
         vals[off + 2] = m23;
         vals[off + 3] = m33;
         offsets[3] = off + 4;

         return 16;
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
   public Matrix4x4Block createTranspose() {
      Matrix4x4Block M = new Matrix4x4Block();
      M.transpose (this);
      return M;
   }

   /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix4x4Block clone() {
      Matrix4x4Block blk = (Matrix4x4Block)super.clone();
      blk.initBlockVariables();
      return blk;
   }

}
