/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.ArrayList;

/**
 * Implements a matrix based on an array of 3x3 sub matrices. Support is
 * provided to assemble these into a sparse matrix.
 */
public abstract class MatrixBlockBase extends DenseMatrixBase implements MatrixBlock {
   protected MatrixBlock myNext;
   protected MatrixBlock myDown;

   protected int myBlkRow;
   protected int myBlkCol;
   protected int myRowOff;
   protected int myColOff;

   protected int myNumber;

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

   MatrixBlockBase() {
      super();
      myBlkCol = -1;
      myBlkRow = -1;
      myRowOff = -1;
      myColOff = -1;
      myNumber = -1;
   }

   /**
    * {@inheritDoc}
    */
   public abstract void setZero();

   /**
    * {@inheritDoc}
    */
   public void scale (double s) {
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            set (i, j, s * get (i, j));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix M) {
      int nrows = rowSize();
      int ncols = colSize();
      if (M.rowSize() != nrows || M.colSize() != ncols) {
         throw new ImproperSizeException ("Matrix sizes do not conform");
      }
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            set (i, j, get (i, j) + M.get (i, j));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix M) {
      int nrows = rowSize();
      int ncols = colSize();
      if (M.rowSize() != nrows || M.colSize() != ncols) {
         throw new ImproperSizeException ("Matrix sizes do not conform");
      }
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            set (i, j, get (i, j) + s * M.get (i, j));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix M) {
      int nrows = rowSize();
      int ncols = colSize();
      if (M.rowSize() != nrows || M.colSize() != ncols) {
         throw new ImproperSizeException ("Matrix sizes do not conform");
      }
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            set (i, j, get (i, j) - M.get (i, j));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public abstract void mulAdd (double[] y, int yIdx, double[] x, int xidx);

   /**
    * {@inheritDoc}
    */
   public abstract void mulTransposeAdd (
      double[] y, int yIdx, double[] x, int xidx);

   // static boolean isUpperTriangular (Partition part)
   // {
   // if (part == Partition.UpperTriangular)
   // { return true;
   // }
   // else if (part == Partition.Full)
   // { return false;
   // }
   // else
   // { throw new IllegalArgumentException (
   // "Invalid partition " + part);
   // }
   // }

   // static boolean isLowerTriangular (Partition part)
   // {
   // if (part == Partition.LowerTriangular)
   // { return true;
   // }
   // else if (part == Partition.Full)
   // { return false;
   // }
   // else
   // { throw new IllegalArgumentException (
   // "Invalid partition " + part);
   // }
   // }
   static int numNonZeros (int nrows, int ncols, Partition part) {
      if (part == Partition.Full) {
         return nrows*ncols;
      }
      else if (part == Partition.UpperTriangular) {
         if (nrows >= ncols) {
            return ncols*(ncols+1)/2;
         }
         else {
            return nrows*ncols - nrows*(nrows-1)/2;
         }
      }
      else { // part == Partition.UpperTriangular
         if (ncols >= nrows) {
            return nrows*(nrows+1)/2;
         }
         else {
            return nrows*ncols - ncols*(ncols-1)/2;
         }
      }
   }

   public static int getBlockCRSIndices (
      MatrixBlock M, int[] colIdxs, int colOff, int[] offsets, Partition part) {
      boolean upperTriangular = MatrixBase.checkUpperTriangular (part);
      int numRows = M.rowSize();
      int numCols = M.colSize();
      // int colOff = M.getColOffset();
      for (int i = 0; i < numRows; i++) {
         int off = offsets[i];
         for (int j = upperTriangular ? i : 0; j < numCols; j++) {
            colIdxs[off] = colOff + j;
            off++;
         }
         offsets[i] = off;
      }
      return numNonZeros (numRows, numCols, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part) {
      return getBlockCRSIndices (this, colIdxs, colOff, offsets, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part) {
      boolean upperTriangular = MatrixBase.checkUpperTriangular (part);
      int numRows = rowSize();
      int numCols = colSize();
      for (int i = 0; i < numRows; i++) {
         int off = offsets[i];
         for (int j = upperTriangular ? i : 0; j < numCols; j++) {
            vals[off++] = get (i, j);
         }
         offsets[i] = off;
      }
      return numNonZeros (numRows, numCols, part);
   }

   /**
    * Dense implementation method for {@link
    * maspack.matrix.MatrixBlock#addNumNonZerosByRow(int[],int,Matrix.Partition)
    * addNumNonZerosByRow(int[],int,Partition)}.
    */
   public static void addNumNonZerosByRow (
      Matrix M, int[] offsets, int idx, Partition part) {
      boolean upperTriangular = MatrixBase.checkUpperTriangular (part);
      int numCols = M.colSize();
      for (int i = 0; i < M.rowSize(); i++) {
         offsets[idx + i] += numCols;
         if (upperTriangular) {
            numCols--;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByRow (int[] offsets, int idx, Partition part) {
      addNumNonZerosByRow (this, offsets, idx, part);
   }

   /**
    * Dense implementation method for {@link
    * maspack.matrix.MatrixBlock#getBlockCCSIndices(int[],int,int[],Matrix.Partition)
    * getBlockCCSIndices(int[],int[],Partition)}.
    */
   public static int getBlockCCSIndices (
      MatrixBlock M, int[] rowIdxs, int rowOff, int[] offsets, Partition part) {
      boolean lowerTriangular = MatrixBase.checkLowerTriangular (part);
      int numRows = M.rowSize();
      int numCols = M.colSize();
      // int rowOff = M.getRowOffset();
      for (int j = 0; j < numCols; j++) {
         int off = offsets[j];
         for (int i = lowerTriangular ? j : 0; i < numRows; i++) {
            rowIdxs[off] = rowOff + i;
            off++;
         }
         offsets[j] = off;
      }
      return numNonZeros (numRows, numCols, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part) {
      return getBlockCCSIndices (this, rowIdxs, rowOff, offsets, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSValues (double[] vals, int[] offsets, Partition part) {
      boolean lowerTriangular = MatrixBase.checkLowerTriangular (part);
      int numRows = rowSize();
      int numCols = colSize();
      for (int j = 0; j < numCols; j++) {
         int off = offsets[j];
         for (int i = lowerTriangular ? j : 0; i < numRows; i++) {
            vals[off++] = get (i, j);
         }
         offsets[j] = off;
      }
      return numNonZeros (numRows, numCols, part);
   }

   /**
    * Dense implementation method for {@link
    * maspack.matrix.MatrixBlock#addNumNonZerosByCol(int[],int,Matrix.Partition)
    * addNumNonZerosByColgetBlockCCSIndices(int[],int,Partition)}.
    */
   public static void addNumNonZerosByCol (
      Matrix M, int[] offsets, int idx, Partition part) {
      boolean lowerTriangular = MatrixBase.checkLowerTriangular (part);
      int numRows = M.rowSize();
      for (int j = 0; j < M.colSize(); j++) {
         offsets[idx + j] += numRows;
         if (lowerTriangular) {
            numRows--;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByCol (int[] offsets, int idx, Partition part) {
      addNumNonZerosByCol (this, offsets, idx, part);
   }

   /**
    * {@inheritDoc}
    */
   public boolean valueIsNonZero (int i, int j) {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public int numNonZeroVals() {
      return rowSize() * colSize();
   }

   // public void set (MatrixBlock M)
   // {
   // super.set (M);
   // }

   /**
    * {@inheritDoc}
    */
   public MatrixBlockBase clone() {
      try {
         return (MatrixBlockBase)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new UnsupportedOperationException (
            "Clone not supported for " + getClass());
      }
   }


   /**
    * List the dimenions of the different fixed-size MatrixBlocks currently
    * defined by the system.
    */
   static protected String[] mySizes = new String[] {
      "1x1", "1x3", "1x6",
      "2x2", "2x3", "2x6", 
      "3x1", "3x2", "3x3", "3x4", "3x6",
      "4x3", "4x4",
      "6x1", "6x2", "6x3", "6x6"
   };

   static String[] getDefinedSizes() {
      return mySizes;
   }

   static int numRows (String dimen) {
      return dimen.charAt(0)-'0';
   }

   static int numCols (String dimen) {
      return dimen.charAt(2)-'0';
   }

   /**
    * Returns an array of integers giving the possible middle
    * dimensions for a fixed-size block matrix multiple whose
    * result is nr x nc.
    */
   static int[] getMulDimensions (int nr, int nc) {
      ArrayList<Integer> middleDims = new ArrayList<Integer>();

      for (String dimr : mySizes) {
         if (numRows(dimr) == nr) {
            for (String dimc : mySizes) {
               if (numCols(dimc) == nc && numCols(dimr) == numRows(dimc)) {
                  middleDims.add (numCols(dimr));
               }
            }
         }
      }
      int[] mids = new int[middleDims.size()];
      for (int k=0; k<mids.length; k++) {
         mids[k] = middleDims.get(k);
      }
      return mids;
   }

   static public MatrixBlock alloc (int nrows, int ncols) {
      switch (nrows) {
         case 1: {
            switch (ncols) {
               case 1: return new Matrix1x1Block();
               case 3: return new Matrix1x3Block();
               case 6: return new Matrix1x6Block();
            }
            break;
         }
         case 2: {
            switch (ncols) {
               case 2: return new Matrix2x2Block();
               case 3: return new Matrix2x3Block();
               case 6: return new Matrix2x6Block();
            }
            break;
         }
         case 3: {
            switch (ncols) {
               case 1: return new Matrix3x1Block();
               case 2: return new Matrix3x2Block();
               case 4: return new Matrix3x4Block();
               case 3: return new Matrix3x3Block();
               case 6: return new Matrix3x6Block();
            }
            break;
         }
         case 4: {
            switch (ncols) {
               case 3: return new Matrix4x3Block();
               case 4: return new Matrix4x4Block();
            }
            break;
         }
         case 6: {
            switch (ncols) {
               case 1: return new Matrix6x1Block();
               case 2: return new Matrix6x2Block();
               case 3: return new Matrix6x3Block();
               case 6: return new Matrix6dBlock();
            }
            break;
         }
      }
      return new MatrixNdBlock (nrows, ncols);
   }

}
