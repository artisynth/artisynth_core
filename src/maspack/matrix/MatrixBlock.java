/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.Clonable;

/**
 * An extension to matrix that supports inclusion in a block matrix.
 */
public interface MatrixBlock extends DenseMatrix, Clonable {
   /**
    * Returns the next matrix block in a block matrix column.
    * 
    * @return next matrix block in a column
    */
   public MatrixBlock down();

   /**
    * Sets the next matrix block in a block matrix column. Reserved for use by
    * <code>maspack.matrix</code>.
    * 
    * @param blk
    * next matrix block in a column
    */
   public void setDown (MatrixBlock blk);

   /**
    * Returns the next matrix block in a block matrix row.
    * 
    * @return next matrix block in a row
    */
   public MatrixBlock next();

   /**
    * Sets the next matrix block in a block matrix row. Reserved for use by
    * <code>maspack.matrix</code>.
    * 
    * @param blk
    * next matrix block in a row
    */
   public void setNext (MatrixBlock blk);

   /**
    * Sets the contents of this matrix block to zero.
    */
   public void setZero();

   /**
    * Scales the elements of this matrix block by a scale factor.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s);

   /**
    * Adds the contents of a Matrix to this matrix block.
    * 
    * @param M
    * matrix to add
    * @throws ImproperSizeException
    * if the matrix sizes do not conform
    */
   public void add (Matrix M);

   /**
    * Adds the scaled contents of a Matrix to this matrix block.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to add
    * @throws ImproperSizeException
    * if the matrix sizes do not conform
    */
   public void scaledAdd (double s, Matrix M);

   /**
    * Subtract the contents of a Matrix from this matrix block.
    * 
    * @param M
    * matrix to subtract
    * @throws ImproperSizeException
    * if the matrix sizes do not conform
    */
   public void sub (Matrix M);

   /**
    * Negates this matrix in place.
    */
   public void negate();

   /**
    * Gets the number of the block column of this matrix block within a
    * {@link maspack.matrix.SparseBlockMatrix SparseBlockMatrix}.
    * 
    * @return block column number for this block
    */
   public int getBlockCol();

   /**
    * Gets the number of the block row of this matrix block within a
    * {@link maspack.matrix.SparseBlockMatrix SparseBlockMatrix}.
    * 
    * @return block row number for this block
    */
   public int getBlockRow();

   /**
    * Sets the number of the block column of this matrix block within a
    * {@link maspack.matrix.SparseBlockMatrix SparseBlockMatrix}. Use of this
    * method is reserved to <code>maspack.matrix</code>.
    * 
    * @param blkCol
    * column number for this block
    */
   public void setBlockCol (int blkCol);

   /**
    * Sets the number of the block row of this matrix block within a
    * {@link maspack.matrix.SparseBlockMatrix SparseBlockMatrix}. Use of this
    * method is reserved to <code>maspack.matrix</code>.
    * 
    * @param blkRow
    * row number for this block
    */
   public void setBlockRow (int blkRow);

   /**
    * Gets the number of this block within a {@link
    * maspack.matrix.SparseBlockMatrix
    * SparseBlockMatrix}. Block numbers are used for fast access. If
    * the block does not belong to a SparseBlockMatrix, then -1 is returned.
    * 
    * @return number for this block
    */
   public int getBlockNumber();

   /**
    * Sets the number of this block within a {@link
    * maspack.matrix.SparseBlockMatrix SparseBlockMatrix}. Use
    * of this method is reserved to <code>maspack.matrix</code>.
    * 
    * @param num
    * number for this block
    */
   public void setBlockNumber (int num);

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2);

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2);


   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2);

   /**
    * Pre-multiplies a column vector by this matrix block and adds the result to
    * a set of existing values. The column vector values are supplied by the
    * argument <code>x</code>, starting at location <code>xIdx</code>, and
    * the result is added to values in <code>y</code>, starting at location
    * <code>yIdx</code>.
    * 
    * @param y
    * accumulates resulting values
    * @param yIdx
    * starting index for accumulating values
    * @param x
    * supplies column vector values
    * @param xidx
    * starting index for column vector values
    */
   public void mulAdd (double[] y, int yIdx, double[] x, int xidx);

   /**
    * Post-multiplies a row vector by this matrix block and adds the result to a
    * set of existing values. The row vector values are supplied by the argument
    * <code>x</code>, starting at location <code>xIdx</code>, and the
    * result is added to values in <code>y</code>, starting at location
    * <code>yIdx</code>.
    * 
    * @param y
    * accumulates resulting values
    * @param yIdx
    * starting index for accumulating values
    * @param x
    * supplies row vector values
    * @param xidx
    * starting index for row vector values
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xidx);

   /**
    * Stores the column indices of the non-zero entries for this matrix block.
    * The indices for each row i are stored contiguously within the array
    * <code>colIdxs</code>, starting at a location given by
    * <code>offsets[i]</code>. Indices are 0-based. Upon return,
    * <code>offsets[i]</code> should be incremented by the number of non-zero
    * values in row i.
    * 
    * @param colIdxs
    * stores column indices of non-zero entries
    * @param colOff
    * starting column index for the first column of this block
    * @param offsets
    * offsets within <code>colIdxs</code> for storing each row's indices; upon
    * return, should be incremented by the number of non-zero entries in each
    * row.
    * @param part
    * specifies whether to store column indices for the entire block or a
    * specified sub-portion.
    * @return number of non-zero entries
    */
   /*
    * This was removed from the above documentation because the method it linked
    * to has been commented out, and this was leading to conflicts
    *  , and are assumed to start (for this block) at the value returned by
    * {@link #getColOffSet getColOffSet}.
    */
   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part);

   /**
    * Stores the values of non-zero entries for this matrix block. The values
    * for each row i are stored contiguously within the array <code>vals</code>,
    * starting at a location given by <code>offsets[i]</code>. Upon return,
    * <code>offsets[i]</code> should be incremented by the number of non-zero
    * values in row i.
    * 
    * @param vals
    * stores values of non-zero entries
    * @param offsets
    * offsets within <code>vals</code> for storing each row's values; upon
    * return, should be incremented by the number of non-zero entries in each
    * row.
    * @param part
    * specifies whether to store the entire block or a specified sub-portion.
    * @return number of non-zero entries
    */
   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part);

   /**
    * Adds the number of non-zero entries in each row of this block matrix to
    * the current values stored in the array <code>offsets</code>, starting
    * at the location specified by <code>idx</code>.
    * 
    * @param offsets
    * values to be incremented by the number of non-zero entries in each row
    * @param idx
    * starting location within <code>offsets</code>
    * @param part
    * specifies whether to consider the entire block or a specified sub-portion.
    */
   public void addNumNonZerosByRow (int[] offsets, int idx, Partition part);

   /**
    * Stores the row indices of the non-zero entries for this matrix block. The
    * indices for each column j are stored contiguously within the array
    * <code>rowIdxs</code>, starting at a location given by
    * <code>offsets[i]</code>. Indices are 0-based. Upon return,
    * <code>offsets[j]</code> should be incremented by the number of non-zero
    * values in column j.
    */
   /*
    * This was removed from the javadocs` because getRowOffset is also commented
    * out, and this was causing conflicts.
    *  , and are assumed to start (for this block) at the value returned by
    * {@link #getRowOffset getRowOffset}.
    */
   /**
    * @param rowIdxs
    * stores row indices of non-zero entries
    * @param rowOff
    * starting row index for the first row of this block
    * @param offsets
    * offsets within <code>rowIdxs</code> for storing each column's indices;
    * upon return, should be incremented by the number of non-zero entries in
    * each column.
    * @param part
    * specifies whether to store row indices for the entire block or a specified
    * sub-portion.
    * @return number of non-zero entries
    */
   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part);

   /**
    * Stores the values of non-zero entries for this matrix block. The values
    * for each column j are stored contiguously within the array
    * <code>vals</code>, starting at a location given by
    * <code>offsets[j]</code>. Upon return, <code>offsets[j]</code> should
    * be incremented by the number of non-zero values in column j.
    * 
    * @param vals
    * stores values of non-zero entries
    * @param offsets
    * offsets within <code>vals</code> for storing each columns's values; upon
    * return, should be incremented by the number of non-zero entries in each
    * column.
    * @param part
    * specifies whether to store the entire block or a specified sub-portion.
    * @return number of non-zero entries
    */
   public int getBlockCCSValues (double[] vals, int[] offsets, Partition part);

   /**
    * Adds the number of non-zero entries in each column of this block matrix to
    * the current values stored in the array <code>offsets</code>, starting
    * at the location specified by <code>idx</code>.
    * 
    * @param offsets
    * values to be incremented by the number of non-zero entries in each column
    * @param idx
    * starting location within <code>offsets</code>
    * @param part
    * specifies whether to consider the entire block or a specified sub-portion.
    */
   public void addNumNonZerosByCol (int[] offsets, int idx, Partition part);

   /** 
    * Returns true if the value at the specified location is structurally
    * non-zero. For dense blocks, this methid will always return true.
    * 
    * @param i row index
    * @param j column index
    * @return true if the value at (i,j) is structurally non-zero
    */
   public boolean valueIsNonZero (int i, int j);

   // /**
   // * Gets the starting column of this matrix block within a {@link
   // * maspack.matrix.SparseBlockMatrix SparseBlockMatrix}.
   // *
   // * @return starting column for this block
   // */
   // public int getColOffset();
   //
   // /**
   // * Gets the starting row of this matrix block within a {@link
   // * maspack.matrix.SparseBlockMatrix SparseBlockMatrix}.
   // *
   // * @return starting row for this block
   // */
   // public int getRowOffset();
   //
   // /**
   // * Sets the starting column of this matrix block within a {@link
   // * maspack.matrix.SparseBlockMatrix SparseBlockMatrix}.
   // * Use of this method is reserved to <code>maspack.matrix</code>.
   // *
   // * @param off starting column for this block
   // */
   // public void setColOffset(int off);
   //
   // /**
   // * Sets the starting row of this matrix block within a {@link
   // * maspack.matrix.SparseBlockMatrix SparseBlockMatrix}.
   // * Use of this method is reserved to <code>maspack.matrix</code>.
   // *
   // * @param off starting row for this block
   // */
   // public void setRowOffset(int off);

   // /**
   // * Returns the number of non-zero values contained within
   // * this matrix block. This method is used for integrating
   // * this block into a sparse matrix structure.
   // *
   // * @return number of non-zero values in the block
   // */
   // public int numNonZeroVals();

   /**
    * Creates a new MatrixBlock which is the transpose of this one.
    * If the MatrixBlock has a fixed size, the transpose should have
    * a fixed size as well.
    */
   public MatrixBlock createTranspose();

   /**
    * Creates a clone of this MatrixBlock, which duplicates the size, storage
    * capacity, and values.
    * 
    * @return clone of this MatrixBlock
    */
   public MatrixBlock clone();

}
