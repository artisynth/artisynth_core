/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.*;

import maspack.util.*;

/**
 * Implements general sparse m x n matrices, along with most the commonly used
 * operations associated with such matrices. A sparse matrix is implemented as a
 * linked list of cells representing the explicit elements, with links running
 * in both the row and column directions. In general, explicit elements are
 * non-zero and non-explicit elements are zero, although zero-valued explicit
 * elements may arise computationally.
 * 
 * <p>
 * Normally, these matrices can be resized, either explicitly through a call to
 * {@link #setSize setSize}, or implicitly through operations that require the
 * matrix size to be modified. However, specialized sub-classes (such as those
 * implementing sub-matrices) may have a fixed size.
 */
public class SparseMatrixNd extends SparseMatrixBase implements LinearTransformNd {
   protected int myNumRows;
   protected int myNumCols;

   protected SparseMatrixCell[] myRows = new SparseMatrixCell[0];
   protected SparseMatrixCell[] myCols = new SparseMatrixCell[0];

   // list of the last items that were added to each column
   protected SparseMatrixCell[] colTails = new SparseMatrixCell[0];

   protected boolean fixedSize = false;

   /**
    * Creates a sparse matrix of a specific size, and initializes its elements
    * to 0. It is legal to create a matrix with zero rows and columns.
    * 
    * @param numRows
    * number of rows
    * @param numCols
    * number of columns
    * @throws ImproperSizeException
    * if numRows or numCols are negative
    */
   public SparseMatrixNd (int numRows, int numCols) {
      setSize (numRows, numCols);
   }

   /**
    * Creates a sparse matrix whose size and elements are the same as an
    * existing Matrix.
    * 
    * @param M
    * matrix object to be copied.
    */
   public SparseMatrixNd (Matrix M) {
      set (M);
   }

   /**
    * Creates a sparse matrix which is a copy of an existing one.
    * 
    * @param M
    * matrix object to be copied.
    */
   public SparseMatrixNd (SparseMatrixNd M) {
      set (M);
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return myNumRows;
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return myNumCols;
   }

   /**
    * Returns true if this matrix is of fixed size. The size of a SparseMatrixNd
    * is normally not fixed, but the size of subclasses might be. A matrix not
    * of fixed size can be resized dynamically, either explicitly using
    * {@link #setSize setSize}, or implicitly when being used as a result for
    * various matrix operations.
    * 
    * @return true if this matrix is of fixed size
    * @see MatrixNd#setSize
    */
   public boolean isFixedSize() {
      return fixedSize;
   }

   /**
    * Sets the size of this matrix. This operation is only supported if
    * {@link #isFixedSize isFixedSize} returns false.
    * 
    * <p>
    * If a matrix is resized, then any previous element values which are still
    * within the new matrix dimensions are preserved. Other (new) element values
    * are undefined.
    * 
    * @param numRows
    * new row size
    * @param numCols
    * new column size
    * @throws ImproperSizeException
    * if this matrix has an explicit internal buffer and that buffer is too
    * small to support the requested size
    * @throws UnsupportedOperationException
    * if this matrix has fixed size
    * @see SparseMatrixNd#isFixedSize
    */
   public void setSize (int numRows, int numCols) {
      if (fixedSize) {
         throw new UnsupportedOperationException ("Matrix has fixed size");
      }
      if (numRows < 0 || numCols < 0) {
         throw new ImproperSizeException ("Negative dimension");
      }
      resetSize (numRows, numCols);
   }

   void resetSize (int numRows, int numCols) throws ImproperSizeException {
      if (numRows > myRows.length) { // need to resize rows
         SparseMatrixCell[] newRows = new SparseMatrixCell[numRows];
         for (int i = 0; i < myRows.length; i++) {
            newRows[i] = myRows[i];
         }
         myRows = newRows;
      }
      else if (numRows < myNumRows) {
         for (int i = numRows; i < myNumRows; i++) {
            myRows[i] = null;
         }
         for (int j = 0; j < myNumCols; j++) {
            SparseMatrixCell prev = prevColEntry (numRows, j);
            if (prev != null) {
               prev.down = null;
            }
            else {
               myCols[j] = null;
            }
         }
      }
      if (numCols > myCols.length) { // need to resize cols
         SparseMatrixCell[] newCols = new SparseMatrixCell[numCols];
         for (int i = 0; i < myCols.length; i++) {
            newCols[i] = myCols[i];
         }
         myCols = newCols;
         colTails = new SparseMatrixCell[numCols];
      }
      else if (numCols < myNumCols) {
         for (int j = numCols; j < myNumCols; j++) {
            myCols[j] = null;
         }
         for (int i = 0; i < myNumRows; i++) {
            SparseMatrixCell prev = prevRowEntry (i, numCols);
            if (prev != null) {
               prev.next = null;
            }
            else {
               myRows[i] = null;
            }
         }
      }
      myNumRows = numRows;
      myNumCols = numCols;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (myRows[i] != null && myCols[j] != null) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            if (cell.j == j) {
               return cell.value;
            }
         }
      }
      return 0;
   }

   public SparseMatrixCell getCell (int i, int j) {
      if (myRows[i] != null && myCols[j] != null) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            if (cell.j == j) {
               return cell;
            }
         }
      }
      return null;
   }

//   /**
//    * Gets the explicit elements of this matrix. The element values are stored
//    * in the array <code>values</code> and the corresponding i and j indices
//    * are stored in the array <code>indices</code>, such that for the k-th
//    * element, i and j are given by <code>indices[k*2]</code> and
//    * <code>indices[k*2+1]</code>, respectively. Elements are returned in
//    * row-major order. The supplied arrays must be long enough to accomodate all
//    * explicit values, otherwise an exception is thrown. The number of explicit
//    * elements is returned.
//    * 
//    * @param values
//    * values for explicit elements
//    * @param indices
//    * i and j indices for explicit elements
//    * @return number of explicit elements
//    * @throws ArrayIndexOutOfBoundsException
//    * if <code>values</code> or <code>indices</code> are not long enough to
//    * store all explicit elements.
//    */
//   public int get (double[] values, int[] indices) {
//      int k = 0;
//      for (int i = 0; i < nrows; i++) {
//         SparseMatrixCell cell;
//         for (cell = rows[i]; cell != null; cell = cell.next) {
//            values[k] = cell.value;
//            indices[k * 2] = cell.i;
//            indices[k * 2 + 1] = cell.j;
//            k++;
//         }
//      }
//      return k;
//   }

   /**
    * Returns the explicit element values in row-major order. The values are
    * placed into an array of doubles. It is assumed that i and j values of
    * these elements are known.
    * 
    * @param values
    * collects the explicit element values
    * @return number of explicit elements
    * @throws ArrayIndexOutOfBoundsException
    * if the array is not large enough to store all the values
    */
   public int getExplicitElements (double[] values) {
      int k = 0;
      SparseMatrixCell cell;
      for (int i = 0; i < myNumRows; i++) {
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            values[k++] = cell.value;
         }
      }
      return k;
   }

   /**
    * Sets the existing explicit element values, in row-major order. New values
    * are supplied by an array of doubles.
    * 
    * @param values
    * new values for the explicit elements
    * @throws ArrayIndexOutOfBoundsException
    * if the array is not large enough to provide all the values
    */
   public void setExplicitElements (double[] values) {
      int k = 0;
      SparseMatrixCell cell;
      for (int i = 0; i < myNumRows; i++) {
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            cell.value = values[k++];
         }
      }
   }

   /**
    * Returns the number of explicit elements in this matrix.
    * 
    * @return number of explicit elements
    */
   public int numExplicitElements() {
      int k = 0;
      SparseMatrixCell cell;
      for (int i = 0; i < myNumRows; i++) {
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            k++;
         }
      }
      return k;
   }

   private class CellIterator implements Iterator<SparseMatrixCell> {
      SparseMatrixCell myCell;

      CellIterator() {
         myCell = null;
         for (int i = 0; i < myNumRows; i++) {
            if (myRows[i] != null) {
               myCell = myRows[i];
               break;
            }
         }
      }

      public boolean hasNext() {
         return myCell != null;
      }

      public SparseMatrixCell next() throws NoSuchElementException {
         if (myCell == null) {
            throw new NoSuchElementException();
         }
         else {
            SparseMatrixCell cell = myCell;
            if (myCell.next != null) {
               myCell = myCell.next;
            }
            else {
               for (int i = myCell.i + 1; i < myNumRows; i++) {
                  if (myRows[i] != null) {
                     myCell = myRows[i];
                     return cell;
                  }
               }
               myCell = null;
            }
            return cell;
         }
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Returns an iterator over explicit elements of this matrix. Element value
    * fields may be set or read as desired, but i and j fields should not be set
    * as this will cause inconsistencies in the matrix structure. The purpose of
    * this routine is to allow fast reading or writing of values for a matrix
    * whose explicit structure is set.
    * 
    * @return iterator over explicit elements.
    */
   Iterator<SparseMatrixCell> getExplicitElements() {
      return new CellIterator();
   }

   public void set (int i, int j, double value) {
      if (i >= myNumRows || j >= myNumCols) {
         throw new ArrayIndexOutOfBoundsException (
            "Indices ("+i+","+j+") out of bounds for "+getSize()+" matrix");
      }
      SparseMatrixCell rowPrev = prevRowEntry (i, j);
      // System.out.println ("(i,j)=("+i+","+j+")");
      // System.out.println ("rowPrev=" + rowPrev);
      // System.out.println ("colPrev=" + colPrev);
      // System.out.println ("value=" + value);

      SparseMatrixCell cell = (rowPrev == null ? myRows[i] : rowPrev.next);
      if (cell != null && cell.j == j) { // then a cell already exists at this
                                          // spot
         if (value != 0) {
            cell.value = value;
         }
         else { // delete the cell
            removeEntry (cell, rowPrev, prevColEntry (i, j));
         }
      }
      else { // no cell exists at this spot
         if (value != 0) {
            cell = new SparseMatrixCell (i, j, value);
            addEntry (cell, rowPrev, prevColEntry (i, j));
         }
      }
   }

   public void setZero (int i, int j) {
      SparseMatrixCell rowPrev = prevRowEntry (i, j);
      SparseMatrixCell cell = (rowPrev == null ? myRows[i] : rowPrev.next);
      if (cell != null && cell.j == j) {
         cell.value = 0;
      }
      else { // no cell exists at this spot
         cell = new SparseMatrixCell (i, j, 0);
         addEntry (cell, rowPrev, prevColEntry (i, j));
      }
   }

   /**
    * Sets the elements of this matrix from an array of doubles and a list of
    * indices. Each value should be associated with two (i,j) index values,
    * which, for the k-th value, are given by <code>indices[k*2]</code> and
    * <code>indices[k*2+1]</code>, respectively. All non-specified elements
    * are set to zero.
    * 
    * @param values
    * explicit values for specific matrix locations
    * @param indices
    * i and j indices for each explicit value
    * @param numNonZeroEntries
    * number of non-zero entries
    */
   public void set (double[] values, int[] indices, int numNonZeroEntries) {
      if (values.length < numNonZeroEntries) {
         throw new IllegalArgumentException (
            "numNonZeroEntries exceeds length of values array");
      }
      if (indices.length < 2 * numNonZeroEntries) {
         throw new IllegalArgumentException ("insufficient index values");
      }
      setZero();
      for (int k = 0; k < numNonZeroEntries; k++) {
         set (indices[2 * k], indices[2 * k + 1], values[k]);
      }
   }

   /**
    * Sets the elements of this matrix from two index arrays and an array of
    * doubles that contain, respectively, the row index, column index, and
    * numeric value for each non-zero entry. The total number of
    * non-zero entries is given by the argument <code>numNonZeroEntries</code>,
    * and each of the arrays should have at least this many elements.
    * 
    * @param rowIdxs
    * row index values for each non-zero entry
    * @param colIdxs
    * column index values for each non-zero entry
    * @param values
    * numeric values for each non-zero entry
    * @param numNonZeroEntries
    * number of non-zero entres
    */
   public void set (
      int[] rowIdxs, int[] colIdxs, double[] values, int numNonZeroEntries) {

      if (values.length < numNonZeroEntries) {
         throw new IllegalArgumentException (
            "numNonZeroEntries exceeds length of values array");
      }
      if (rowIdxs.length < numNonZeroEntries) {
         throw new IllegalArgumentException (
            "numNonZeroEntries exceeds length of rowIdxs array");
      }
      if (colIdxs.length < numNonZeroEntries) {
         throw new IllegalArgumentException (
            "numNonZeroEntries exceeds length of colIdxs array");
      }
      setZero();
      for (int k = 0; k < numNonZeroEntries; k++) {
         set (rowIdxs[k], colIdxs[k], values[k]);
      }
   }

   public void addEntry (
      SparseMatrixCell cell, SparseMatrixCell rowPrev, SparseMatrixCell colPrev) {
      if (rowPrev == null) {
         cell.next = myRows[cell.i];
         myRows[cell.i] = cell;
      }
      else {
         cell.next = rowPrev.next;
         rowPrev.next = cell;
      }
      if (colPrev == null) {
         cell.down = myCols[cell.j];
         myCols[cell.j] = cell;
      }
      else {
         cell.down = colPrev.down;
         colPrev.down = cell;
      }
   }

   private void clearColumnTails() {
      for (int j = 0; j < myNumCols; j++) {
         colTails[j] = null;
      }
   }

   private void addEntryAtColumnTail (
      SparseMatrixCell cell, SparseMatrixCell rowPrev) {
      int j = cell.j;
      addEntry (cell, rowPrev, colTails[j]);
      colTails[j] = cell;
   }

   private void duplicateRowElements (
      SparseMatrixCell rowCell, SparseMatrixCell rowPrev) {
      while (rowCell != null) {
         SparseMatrixCell newCell = new SparseMatrixCell (rowCell);
         addEntryAtColumnTail (newCell, rowPrev);
         rowPrev = newCell;
         rowCell = rowCell.next;
      }
   }

   private void duplicateAndScaleRowElements (
      SparseMatrixCell rowCell, SparseMatrixCell rowPrev, double s) {
      while (rowCell != null) {
         SparseMatrixCell newCell = new SparseMatrixCell (rowCell);
         newCell.value *= s;
         addEntryAtColumnTail (newCell, rowPrev);
         rowPrev = newCell;
         rowCell = rowCell.next;
      }
   }

   public void removeEntry (
      SparseMatrixCell cell, SparseMatrixCell rowPrev, SparseMatrixCell colPrev) {
      if (rowPrev == null) {
         myRows[cell.i] = cell.next;
      }
      else {
         rowPrev.next = cell.next;
      }
      if (colPrev == null) {
         myCols[cell.j] = cell.down;
      }
      else {
         colPrev.down = cell.down;
      }
   }

   public SparseMatrixCell prevColEntry (int i, int j) {
      SparseMatrixCell prev = null;
      SparseMatrixCell cell;
      for (cell = myCols[j]; cell != null; cell = cell.down) {
         if (cell.i >= i) {
            return prev;
         }
         prev = cell;
      }
      return prev;
   }

   public SparseMatrixCell prevRowEntry (int i, int j) {
      SparseMatrixCell prev = null;
      SparseMatrixCell cell;
      for (cell = myRows[i]; cell != null; cell = cell.next) {
         if (cell.j >= j) {
            return prev;
         }
         prev = cell;
      }
      return prev;
   }

   /**
    * Sets the size and values of this matrix to those of matrix M.
    * 
    * @param M
    * matrix tp be copied
    * @throws ImproperSizeException
    * if matrices have different sizes and this matrix cannot be resized
    * accordingly
    */
   public void set (SparseMatrixNd M) {
      if (M == this) {
         return;
      }
      if (myNumRows != M.myNumRows || myNumCols != M.myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M.myNumRows, M.myNumCols);
         }
      }
      setZero();
      clearColumnTails();
      for (int i = 0; i < myNumRows; i++) {
         duplicateRowElements (M.myRows[i], null);
      }
   }

   /**
    * Sets this matrix to the identity matrix. If this matrix matrix is not
    * square, then element (i,j) is set to 1 if i and j are equal, and 0
    * otherwise.
    */
   public void setIdentity() {
      setZero();
      int size = Math.min (myNumRows, myNumCols);
      for (int i = 0; i < size; i++) {
         addEntry (new SparseMatrixCell (i, i, 1), null, null);
      }
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   public void setZero() {
      for (int i = 0; i < myNumRows; i++) {
         myRows[i] = null;
      }
      for (int j = 0; j < myNumCols; j++) {
         myCols[j] = null;
      }
   }

   /**
    * Sets this matrix to a diagonal matrix whose diagonal elements are
    * specified by an array.
    * 
    * @param vals
    * diagonal elements for this matrix
    * @throws ImproperSizeException
    * if the size of <code>diag</code> does not equal the minimum matrix
    * dimension
    */
   public void setDiagonal (double[] vals) {
      int size = Math.min (myNumRows, myNumCols);
      if (vals.length < size) {
         throw new IllegalArgumentException ("Insufficient values");
      }
      setZero();
      for (int i = 0; i < size; i++) {
         addEntry (new SparseMatrixCell (i, i, vals[i]), null, null);
      }
   }

   /**
    * Sets some randomly selected elements of this matrix to random values. The
    * number of selected elements is equal to the sum of the number of rows and
    * columns, and the values are uniformly distributed in the range -0.5
    * (inclusive) to 0.5 (exclusive). Other elements are set to zero.
    */
   public void setRandom() {
      setRandom (-0.5, 0.5, myNumRows + myNumCols, RandomGenerator.get());
   }

   /**
    * Sets some randomly selected elements of this matrix to random values. The
    * number of selected elements is given by the argument
    * <code>numElements</code>, and the values are uniformly distributed in in
    * a specified range. Other elements are set to zero.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param numElements
    * number of explicit elements to randomly select
    */
   public void setRandom (double lower, double upper, int numElements) {
      setRandom (lower, upper, numElements, RandomGenerator.get());
   }

   /**
    * Sets some randomly selected elements of this matrix to random values. The
    * number of selected elements is given by the argument
    * <code>numElements</code>, and the values are uniformly distributed in in
    * a specified range. Other elements are set to zero. Random numbers are
    * generated by a supplied random number generator.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param numElements
    * number of explicit values to randomly select
    * @param generator
    * random number generator
    */
   public void setRandom (
      double lower, double upper, int numElements, Random generator) {
      setZero();
      double range = upper - lower;
      for (int k = 0; k < numElements; k++) {
         int i, j;
         do {
            i = generator.nextInt (myNumRows);
            j = generator.nextInt (myNumCols);
         }
         while (get (i, j) != 0);
         set (i, j, generator.nextDouble() * range + lower);
      }
   }

   /**
    * Multiplies this matrix by M and places the result in this matrix. This
    * matrix is resized if necessary.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M do not conform, or if this matrix needs resizing but
    * is of fixed size
    */
   public void mul (SparseMatrixNd M) {
      mul (this, M);
   }

   /**
    * Multiplies matrix M1 by M2 and places the result in this matrix. This
    * matrix is resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if matrices M1 and M2 do not comform, or if this matrix needs resizing but
    * is of fixed size
    */
   public void mul (SparseMatrixNd M1, SparseMatrixNd M2)
      throws ImproperSizeException {
      if (M1.myNumCols != M2.myNumRows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (M1.myNumRows != myNumRows || M2.myNumCols != myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      SparseMatrixCell[] rows1 = M1.myRows;
      SparseMatrixCell[] cols2 = M2.myCols;

      myNumRows = M1.myNumRows;
      myNumCols = M2.myNumCols;
      myRows = new SparseMatrixCell[myNumRows];
      myCols = new SparseMatrixCell[myNumCols];
      if (colTails.length < myNumCols) {
         colTails = new SparseMatrixCell[myNumCols];
      }
      else {
         clearColumnTails();
      }
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell rowTail = null;
         for (int j = 0; j < myNumCols; j++) {
            double value = rowColProduct (rows1[i], cols2[j]);
            if (value != 0) {
               SparseMatrixCell newCell = new SparseMatrixCell (i, j, value);
               addEntryAtColumnTail (newCell, rowTail);
               rowTail = newCell;
            }
         }
      }
   }

   /**
    * Multiplies this matrix by the transpose of M and places the result in this
    * matrix. This matrix is resized if necessary.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and the transpose of M do not conform, or if this matrix
    * needs resizing but is of fixed size
    */
   public void mulTranspose (SparseMatrixNd M) {
      mulTransposeRight (this, M);
   }

   /**
    * Multiplies matrix M1 by the transpose of M2 and places the result in this
    * matrix. This matrix is resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if M1 and the transpose of M2 do not comform, or if this matrix needs
    * resizing but is of fixed size
    */
   public void mulTransposeRight (SparseMatrixNd M1, SparseMatrixNd M2)
      throws ImproperSizeException {
      if (M1.myNumCols != M2.myNumCols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (M1.myNumRows != myNumRows || M2.myNumRows != myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      SparseMatrixCell[] rows1 = M1.myRows;
      SparseMatrixCell[] rows2 = M2.myRows;

      myNumRows = M1.myNumRows;
      myNumCols = M2.myNumRows;
      myRows = new SparseMatrixCell[myNumRows];
      myCols = new SparseMatrixCell[myNumCols];
      if (colTails.length < myNumCols) {
         colTails = new SparseMatrixCell[myNumCols];
      }
      else {
         clearColumnTails();
      }
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell rowTail = null;
         for (int j = 0; j < myNumCols; j++) {
            double value = rowRowProduct (rows1[i], rows2[j]);
            if (value != 0) {
               SparseMatrixCell newCell = new SparseMatrixCell (i, j, value);
               addEntryAtColumnTail (newCell, rowTail);
               rowTail = newCell;
            }
         }
      }
   }

   /**
    * Multiplies the transpose of matrix M1 by M2 and places the result in this
    * matrix. This matrix is resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if the transpose of M1 and M2 do not comform, or if this matrix needs
    * resizing but is of fixed size
    */
   public void mulTransposeLeft (SparseMatrixNd M1, SparseMatrixNd M2)
      throws ImproperSizeException {
      if (M1.myNumRows != M2.myNumRows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (M1.myNumCols != myNumRows || M2.myNumCols != myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      SparseMatrixCell[] cols1 = M1.myCols;
      SparseMatrixCell[] cols2 = M2.myCols;

      myNumRows = M1.myNumCols;
      myNumCols = M2.myNumCols;
      myRows = new SparseMatrixCell[myNumRows];
      myCols = new SparseMatrixCell[myNumCols];
      if (colTails.length < myNumCols) {
         colTails = new SparseMatrixCell[myNumCols];
      }
      else {
         clearColumnTails();
      }
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell rowTail = null;
         for (int j = 0; j < myNumCols; j++) {
            double value = colColProduct (cols1[i], cols2[j]);
            if (value != 0) {
               SparseMatrixCell newCell = new SparseMatrixCell (i, j, value);
               addEntryAtColumnTail (newCell, rowTail);
               rowTail = newCell;
            }
         }
      }
   }

   /**
    * Multiplies the transpose of matrix M1 by the transpose of M2 and places
    * the result in this matrix. This matrix is resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if the transpose of M1 and the transpose of M2 do not comform, or if this
    * matrix needs resizing but is of fixed size
    */
   public void mulTransposeBoth (SparseMatrixNd M1, SparseMatrixNd M2)
      throws ImproperSizeException {
      if (M1.myNumRows != M2.myNumCols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (M1.myNumCols != myNumRows || M2.myNumRows != myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
      }
      SparseMatrixCell[] cols1 = M1.myCols;
      SparseMatrixCell[] rows2 = M2.myRows;

      myNumRows = M1.myNumCols;
      myNumCols = M2.myNumRows;
      myRows = new SparseMatrixCell[myNumRows];
      myCols = new SparseMatrixCell[myNumCols];
      if (colTails.length < myNumCols) {
         colTails = new SparseMatrixCell[myNumCols];
      }
      else {
         clearColumnTails();
      }
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell rowTail = null;
         for (int j = 0; j < myNumCols; j++) {
            double value = rowColProduct (rows2[j], cols1[i]);
            if (value != 0) {
               SparseMatrixCell newCell = new SparseMatrixCell (i, j, value);
               addEntryAtColumnTail (newCell, rowTail);
               rowTail = newCell;
            }
         }
      }
   }

   /**
    * Adds this matrix to M and places the result in this matrix.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M have different sizes
    */
   public void add (SparseMatrixNd M) {
      if (myNumRows != M.myNumRows || myNumCols != M.myNumCols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (M == this) { // just iterate through and double everything
         for (int i = 0; i < myNumRows; i++) {
            SparseMatrixCell cell;
            for (cell = myRows[i]; cell != null; cell = cell.next) {
               cell.value += cell.value;
            }
         }
      }
      else {
         clearColumnTails();
         for (int i = 0; i < myNumRows; i++) {
            SparseMatrixCell cell1 = myRows[i];
            SparseMatrixCell cell2 = M.myRows[i];
            SparseMatrixCell rowTail = null;
            SparseMatrixCell curCell = null;
            while (cell1 != null && cell2 != null) {
               if (cell1.j < cell2.j) { // leave cell where it is
                  curCell = cell1;
                  cell1 = cell1.next;
               }
               else if (cell1.j > cell2.j) {
                  curCell = new SparseMatrixCell (cell2);
                  addEntry (curCell, rowTail, colTails[curCell.j]);
                  cell2 = cell2.next;
               }
               else {
                  curCell = cell1;
                  cell1.value += cell2.value;
                  cell1 = cell1.next;
                  cell2 = cell2.next;
               }
               rowTail = curCell;
               colTails[curCell.j] = curCell;
            }
            if (cell2 != null) {
               duplicateRowElements (cell2, rowTail);
            }
            else if (cell1 != null) {
               while (cell1 != null) {
                  colTails[cell1.j] = cell1;
                  cell1 = cell1.next;
               }
            }
         }
      }
   }

   /**
    * Adds matrix M1 to M2 and places the result in this matrix. This matrix is
    * resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if matrices M1 and M2 have different sizes, or if this matrix needs
    * resizing but is of fixed size
    */
   public void add (SparseMatrixNd M1, SparseMatrixNd M2) {
      if (M1.myNumRows != M2.myNumRows || M1.myNumCols != M2.myNumCols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (myNumRows != M1.myNumRows || myNumCols != M1.myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.myNumRows, M1.myNumCols);
         }
      }
      if (M1 == this) {
         add (M2);
      }
      else if (M2 == this) {
         add (M1);
      }
      else {
         setZero();
         clearColumnTails();
         for (int i = 0; i < myNumRows; i++) {
            SparseMatrixCell cell1 = M1.myRows[i];
            SparseMatrixCell cell2 = M2.myRows[i];
            SparseMatrixCell rowTail = null;
            SparseMatrixCell newCell = null;
            while (cell1 != null && cell2 != null) {
               if (cell1.j < cell2.j) {
                  newCell = new SparseMatrixCell (cell1);
                  cell1 = cell1.next;
               }
               else if (cell1.j > cell2.j) {
                  newCell = new SparseMatrixCell (cell2);
                  cell2 = cell2.next;
               }
               else {
                  newCell =
                     new SparseMatrixCell (
                        cell1.i, cell1.j, cell1.value + cell2.value);
                  cell1 = cell1.next;
                  cell2 = cell2.next;
               }
               addEntryAtColumnTail (newCell, rowTail);
               rowTail = newCell;
            }
            if (cell1 != null) {
               duplicateRowElements (cell1, rowTail);
            }
            else if (cell2 != null) {
               duplicateRowElements (cell2, rowTail);
            }
         }
      }
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M have different sizes
    */
   public void sub (SparseMatrixNd M) {
      if (myNumRows != M.myNumRows || myNumCols != M.myNumCols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (M == this) { // just zero everything
         setZero();
      }
      else {
         clearColumnTails();
         for (int i = 0; i < myNumRows; i++) {
            SparseMatrixCell cell1 = myRows[i];
            SparseMatrixCell cell2 = M.myRows[i];
            SparseMatrixCell rowTail = null;
            SparseMatrixCell curCell = null;
            while (cell1 != null && cell2 != null) {
               if (cell1.j < cell2.j) { // leave cell where it is
                  curCell = cell1;
                  cell1 = cell1.next;
               }
               else if (cell1.j > cell2.j) {
                  curCell = new SparseMatrixCell (cell2);
                  curCell.value = -curCell.value;
                  addEntry (curCell, rowTail, colTails[curCell.j]);
                  cell2 = cell2.next;
               }
               else {
                  curCell = cell1;
                  cell1.value -= cell2.value;
                  cell1 = cell1.next;
                  cell2 = cell2.next;
               }
               rowTail = curCell;
               colTails[curCell.j] = curCell;
            }
            if (cell2 != null) {
               duplicateAndScaleRowElements (cell2, rowTail, -1);
            }
            else if (cell1 != null) {
               while (cell1 != null) {
                  colTails[cell1.j] = cell1;
                  cell1 = cell1.next;
               }
            }
         }
      }
   }

   /**
    * Subtracts matrix M1 from M2 and places the result in this matrix. This
    * matrix is resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if matrices M1 and M2 have different sizes, or if this matrix needs
    * resizing but is of fixed size
    */
   public void sub (SparseMatrixNd M1, SparseMatrixNd M2) {
      if (M1.myNumRows != M2.myNumRows || M1.myNumCols != M2.myNumCols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (myNumRows != M1.myNumRows || myNumCols != M1.myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.myNumRows, M1.myNumCols);
         }
      }
      if (M1 == this) {
         sub (M2);
      }
      else if (M2 == this) {
         sub (M1);
         negate();
      }
      else {
         setZero();
         clearColumnTails();
         for (int i = 0; i < myNumRows; i++) {
            SparseMatrixCell cell1 = M1.myRows[i];
            SparseMatrixCell cell2 = M2.myRows[i];
            SparseMatrixCell rowTail = null;
            SparseMatrixCell newCell = null;
            while (cell1 != null && cell2 != null) {
               if (cell1.j < cell2.j) {
                  newCell = new SparseMatrixCell (cell1);
                  cell1 = cell1.next;
               }
               else if (cell1.j > cell2.j) {
                  newCell = new SparseMatrixCell (cell2);
                  newCell.value = -newCell.value;
                  cell2 = cell2.next;
               }
               else {
                  newCell =
                     new SparseMatrixCell (cell1.i, cell1.j, cell1.value
                     - cell2.value);
                  cell1 = cell1.next;
                  cell2 = cell2.next;
               }
               addEntryAtColumnTail (newCell, rowTail);
               rowTail = newCell;
            }
            if (cell1 != null) {
               duplicateRowElements (cell1, rowTail);
            }
            else if (cell2 != null) {
               duplicateAndScaleRowElements (cell2, rowTail, -1);
            }
         }
      }
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            cell.value = -cell.value;
         }
      }
   }

   /**
    * Sets this matrix to the negative of M1. This matrix is resized if
    * necessary.
    * 
    * @param M
    * matrix to negate
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void negate (SparseMatrixNd M) {
      if (myNumRows != M.myNumRows || myNumCols != M.myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M.myNumRows, M.myNumCols);
         }
      }
      if (M == this) {
         negate();
      }
      else {
         setZero();
         clearColumnTails();
         for (int i = 0; i < myNumRows; i++) {
            duplicateAndScaleRowElements (M.myRows[i], null, -1);
         }
      }
   }

   /**
    * Scales the elements of matrix M1 by <code>s</code> and places the
    * results in this matrix. This matrix is resized if necessary.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void scale (double s, SparseMatrixNd M) throws ImproperSizeException {
      if (myNumRows != M.myNumRows || myNumCols != M.myNumCols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M.myNumRows, M.myNumCols);
         }
      }
      if (M == this) {
         scale (s);
      }
      else {
         setZero();
         clearColumnTails();
         for (int i = 0; i < myNumRows; i++) {
            duplicateAndScaleRowElements (M.myRows[i], null, s);
         }
      }
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            cell.value *= s;
         }
      }
   }

   /**
    * Replaces this matrix by its tranpose. The matrix is resized if necessary.
    * 
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void transpose() {
      if (myNumRows != myNumCols && fixedSize) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseMatrixCell[] rowsTmp;
      int nrowsTmp;

      rowsTmp = myRows;
      myRows = myCols;
      myCols = rowsTmp;

      nrowsTmp = myNumRows;
      myNumRows = myNumCols;
      myNumCols = nrowsTmp;

      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            cell.transpose();
         }
      }
   }

   /**
    * Takes the transpose of matrix M and places the result in this matrix. The
    * matrix is resized if necessary.
    * 
    * @param M
    * matrix to take the transpose of
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void transpose (SparseMatrixNd M) throws ImproperSizeException {
      if (M == this) {
         transpose();
         return;
      }
      if (myNumRows != M.myNumCols || myNumCols != M.myNumRows) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M.myNumCols, M.myNumRows);
         }
      }
      setZero();
      clearColumnTails();
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell rowPrev = null;
         SparseMatrixCell colCell = M.myCols[i];
         while (colCell != null) {
            SparseMatrixCell newCell =
               new SparseMatrixCell (colCell.j, colCell.i, colCell.value);
            addEntryAtColumnTail (newCell, rowPrev);
            rowPrev = newCell;
            colCell = colCell.down;
         }
      }
   }

   private double rowColProduct (
      SparseMatrixCell rowCell, SparseMatrixCell colCell) {
      double prod = 0;
      while (rowCell != null && colCell != null) {
         if (rowCell.j < colCell.i) {
            rowCell = rowCell.next;
         }
         else if (rowCell.j > colCell.i) {
            colCell = colCell.down;
         }
         else {
            prod += rowCell.value * colCell.value;
            rowCell = rowCell.next;
            colCell = colCell.down;
         }
      }
      return prod;
   }

   private double rowRowProduct (
      SparseMatrixCell row1Cell, SparseMatrixCell row2Cell) {
      double prod = 0;
      while (row1Cell != null && row2Cell != null) {
         if (row1Cell.j < row2Cell.j) {
            row1Cell = row1Cell.next;
         }
         else if (row1Cell.j > row2Cell.j) {
            row2Cell = row2Cell.next;
         }
         else {
            prod += row1Cell.value * row2Cell.value;
            row1Cell = row1Cell.next;
            row2Cell = row2Cell.next;
         }
      }
      return prod;
   }

   private double colColProduct (
      SparseMatrixCell col1Cell, SparseMatrixCell col2Cell) {
      double prod = 0;
      while (col1Cell != null && col2Cell != null) {
         if (col1Cell.i < col2Cell.i) {
            col1Cell = col1Cell.down;
         }
         else if (col1Cell.i > col2Cell.i) {
            col2Cell = col2Cell.down;
         }
         else {
            prod += col1Cell.value * col2Cell.value;
            col1Cell = col1Cell.down;
            col2Cell = col2Cell.down;
         }
      }
      return prod;
   }

   // protected void mulVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int i=0; i<nr; i++) {
   //       double sum = 0;
   //       SparseMatrixCell rowCell;
   //       for (rowCell = rows[i]; rowCell != null; rowCell = rowCell.next) {
   //          if (rowCell.j >= nc) {
   //             break;
   //          }
   //          sum += rowCell.value*vec[rowCell.j];
   //       }
   //       res[i] = sum;
   //    }
   // }
   
   // @Override
   // protected void mulAddVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int i=0; i<nr; i++) {
   //       double sum = 0;
   //       SparseMatrixCell rowCell;
   //       for (rowCell = rows[i]; rowCell != null; rowCell = rowCell.next) {
   //          if (rowCell.j >= nc) {
   //             break;
   //          }
   //          sum += rowCell.value*vec[rowCell.j];
   //       }
   //       res[i] += sum;
   //    }
   // }

   // protected void mulTransposeVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int j=0; j<nr; j++) {
   //       double sum = 0;
   //       SparseMatrixCell colCell;
   //       for (colCell = cols[j]; colCell != null; colCell = colCell.down) {
   //          if (colCell.i >= nc) {
   //             break;
   //          }
   //          sum += colCell.value * vec[colCell.i];
   //       }
   //       res[j] = sum;
   //    }
   // }

   protected void mulVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;

      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         SparseMatrixCell rowCell;
         for (rowCell = myRows[i]; rowCell != null; rowCell = rowCell.next) {
            if (rowCell.j >= colf) {
               break;
            }
            else if (rowCell.j >= c0) {
               sum += rowCell.value*vec[rowCell.j-c0];
            }
         }
         res[i-r0] = sum;
      }
   }
   
   @Override
   protected void mulAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int rowf = r0+nr;
      int colf = c0+nc;

      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         SparseMatrixCell rowCell;
         for (rowCell = myRows[i]; rowCell != null; rowCell = rowCell.next) {
            if (rowCell.j >= colf) {
               break;
            }
            else if (rowCell.j >= c0) {
               sum += rowCell.value*vec[rowCell.j-c0];
            }
         }
         res[i-r0] += sum;
      }
   }

   protected void mulTransposeVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      // rowf and colf are with respect to the *transposed* matrix
      int rowf = r0+nr;
      int colf = c0+nc;

      for (int j=r0; j<rowf; j++) {
         double sum = 0;
         SparseMatrixCell colCell;
         for (colCell = myCols[j]; colCell != null; colCell = colCell.down) {
            if (colCell.i >= colf) {
               break;
            }
            else if (colCell.i >= c0) {
               sum += colCell.value * vec[colCell.i-c0];
            }
         }
         res[j-r0] = sum;
      }
   }

   protected void mulTransposeAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      // rowf and colf are with respect to the *transposed* matrix
      int rowf = r0+nr;
      int colf = c0+nc;

      for (int j=r0; j<rowf; j++) {
         double sum = 0;
         SparseMatrixCell colCell;
         for (colCell = myCols[j]; colCell != null; colCell = colCell.down) {
            if (colCell.i >= colf) {
               break;
            }
            else if (colCell.i >= c0) {
               sum += colCell.value * vec[colCell.i-c0];
            }
         }
         res[j-r0] += sum;
      }
   }

   /**
    * Returns a String representation of this sparse matrix. Each explicit
    * element is output in row-major order as a tuple <code>( i j
    * value)</code>,
    * one tuple per line. Elements which are not printed are zero.
    * 
    * @return String representation of this matrix
    */
   public String toString() {
      return toString ("%g");
   }

   /**
    * Returns a String representation of this sparse matrix. The output format
    * is the same as that produced by {@link #toString() toString}, except that
    * elements values are themselves formatted using a C <code>printf</code>
    * style format string. For a description of the format string syntax, see
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * numeric format specification
    * @return String representation of this matrix
    */
   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   /**
    * Returns a String representation of this sparse matrix. The output format
    * is the same as that produced by {@link #toString() toString}, except that
    * elements values are themselves formatted using a C <code>printf</code>
    * style as decribed by the parameter <code>NumberFormat</code>. When
    * called numerous times, this routine can be more efficient than
    * {@link #toString(String) toString(String)}, because the
    * {@link maspack.util.NumberFormat NumberFormat} does not need to be
    * recreated each time from a specification string.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this matrix
    */
   public String toString (NumberFormat fmt) {
      StringBuffer buf = new StringBuffer (20 * myNumRows * myNumCols);
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            buf.append (
               "("+cell.i+" "+cell.j+" " + fmt.format (cell.value) + ")\n");
         }
      }
      return buf.toString();
   }

   @Override
   public int numNonZeroVals (Partition part, int nrows, int ncols) {
      if (nrows > rowSize() || ncols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      if (part == Partition.None) {
         return 0;
      }
      int cnt = 0;
      switch (part) {
         case None: {
            return 0;
         }
         case Full: {
            for (int i = 0; i < nrows; i++) {
               SparseMatrixCell cell = myRows[i];
               while (cell != null && cell.j < ncols) {
                  cell = cell.next;
                  cnt++;
               }
            }
            break;
         }
         case UpperTriangular: {
            for (int i = 0; i < nrows; i++) {
               SparseMatrixCell cell = myRows[i];
               while (cell != null && cell.j < ncols) {
                  if (cell.j >= i) {
                     cnt++;
                  }
                  cell = cell.next;
               }
            }
            break;
         }
         case LowerTriangular: {
            for (int j = 0; j < ncols; j++) {
               SparseMatrixCell cell = myCols[j];
               while (cell != null && cell.i < nrows) {
                  if (cell.i >= j) {
                     cnt++;
                  }
                  cell = cell.down;
               }
            }
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented partition: "+part);
         }
      }
      return cnt;
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSIndices (
      int[] colIdxs, int[] rowOffs, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      if (part != Partition.UpperTriangular && part != Partition.Full) {
         throw new UnsupportedOperationException (
            "Matrix partition " + part + " not supported");
      }
      int idx = 0;
      for (int i = 0; i < numRows; i++) {
         SparseMatrixCell cell = myRows[i];
         if (rowOffs != null) {
            rowOffs[i] = idx+1;
         }
         int jbegin = (part == Partition.UpperTriangular ? i : 0);
         while (cell != null && cell.j < numCols) {
            if (cell.j >= jbegin) {
               colIdxs[idx++] = cell.j+1;
            }
            cell = cell.next;
         }
      }
      if (rowOffs != null) {
         rowOffs[numRows] = idx+1;
      }
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int getCRSValues (
      double[] vals, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified block matrix is out of bounds");
      }
      if (part != Partition.UpperTriangular && part != Partition.Full) {
         throw new UnsupportedOperationException (
            "Matrix partition " + part + " not supported");
      }
      int idx = 0;
      for (int i = 0; i < numRows; i++) {
         SparseMatrixCell cell = myRows[i];
         int jbegin = (part == Partition.UpperTriangular ? i : 0);
         while (cell != null && cell.j < numCols) {
            if (cell.j >= jbegin) {
               vals[idx++] = cell.value;
            }
            cell = cell.next;
         }
      }
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSIndices (
      int[] rowIdxs, int[] colOffs, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified sub-matrix is out of bounds");
      }
      if (part != Partition.LowerTriangular && part != Partition.Full) {
         throw new UnsupportedOperationException (
            "Matrix partition " + part + " not supported");
      }
      int idx = 0;
      for (int j = 0; j < numCols; j++) {
         SparseMatrixCell cell = myCols[j];
         if (colOffs != null) {
            colOffs[j] = idx+1;
         }
         int ibegin = (part == Partition.LowerTriangular ? j : 0);
         while (cell != null && cell.i < numRows) {
            if (cell.i >= ibegin) {
               rowIdxs[idx++] = cell.i+1;
            }
            cell = cell.down;
         }
      }
      if (colOffs != null) {
         colOffs[numCols] = idx+1;
      }
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int getCCSValues (
      double[] vals, Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "specified block matrix is out of bounds");
      }
      if (part != Partition.LowerTriangular && part != Partition.Full) {
         throw new UnsupportedOperationException (
            "Matrix partition " + part + " not supported");
      }
      int idx = 0;
      for (int j = 0; j < numCols; j++) {
         SparseMatrixCell cell = myCols[j];
         int ibegin = (part == Partition.LowerTriangular ? j : 0);
         while (cell != null && cell.i < numRows) {
            if (cell.i >= ibegin) {
               vals[idx++] = cell.value;
            }
            cell = cell.down;
         }
      }
      return idx;
   }

   /**
    * Sets a submatrix of this matrix. The first row and column of the submatrix
    * are given by <code>baseRow</code> and <code>baseCol</code>, and the
    * new values are given by the matrix <code>Msrc</code>. The size of the
    * submatrix is determined by the dimensions of <code>Msrc</code>.
    * 
    * @param baseRow
    * index of the first row of the submatrix
    * @param baseCol
    * index of the first column of the submatrix
    * @param Msrc
    * new values for the submatrix.
    * @throws ImproperSizeException
    * if <code>baseRow</code> or <code>baseCol</code> are negative, or if
    * the submatrix exceeds the current matrix bounds
    */
   public void setSubMatrix (int baseRow, int baseCol, SparseMatrixNd Msrc)
      throws ImproperSizeException {
      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      if (baseRow + Msrc.myNumRows > myNumRows || baseCol + Msrc.myNumCols > myNumCols) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      for (int i = baseRow; i < baseRow + Msrc.myNumRows; i++) {
         SparseMatrixCell rowPrev = prevRowEntry (i, baseCol);
         SparseMatrixCell srcCell = Msrc.myRows[i - baseRow];

         while (srcCell != null) {
            SparseMatrixCell cell = (rowPrev == null ? myRows[i] : rowPrev.next);
            int j = srcCell.j + baseCol;

            // remove any cells between previous cell and (i, j)
            while (cell != null && cell.j < j) {
               removeEntry (cell, rowPrev, prevColEntry (i, cell.j));
               cell = cell.next;
            }
            // test for existing cell at (i,j)
            if (cell != null && cell.j == j) {
               cell.value = srcCell.value; // cell exists
            }
            else { // no cell; create one
               cell = new SparseMatrixCell (i, j, srcCell.value);
               addEntry (cell, rowPrev, prevColEntry (i, j));
            }
            rowPrev = cell;
            srcCell = srcCell.next;
         }
         // delete all remaining cells up to column baseCol+Msrc.nrows
         SparseMatrixCell cell = (rowPrev == null ? myRows[i] : rowPrev.next);
         while (cell != null && cell.j < baseCol + Msrc.myNumRows) {
            removeEntry (cell, rowPrev, prevColEntry (i, cell.j));
            cell = cell.next;
         }
      }
   }

   public SparseMatrixCell getRow (int i) {
      return myRows[i];
   }

   public SparseMatrixCell getCol (int j) {
      return myCols[j];
   }

   /** 
    * {@inheritDoc}
    */
   public void checkConsistency () {
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell cell;
         SparseMatrixCell prev = null;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            if (cell.i != i) {
               throw new TestException ("row "+i+" contains cell "+cell);
            }
            if (prev != null && prev.j >= cell.j) {
               throw new TestException (
                  "row "+i+": cell "+cell+" follows "+prev);
            }
            if (cell.j >= myNumCols) {
               throw new TestException (
                  "row "+i+" contains cell "+cell+" with ncols="+myNumCols);
            }
            prev = cell;
         }
      }
      for (int j = 0; j < myNumCols; j++) {
         SparseMatrixCell cell;
         SparseMatrixCell prev = null;
         for (cell = myCols[j]; cell != null; cell = cell.down) {
            if (cell.j != j) {
               throw new TestException ("col "+j+" contains cell "+cell);
            }
            if (prev != null && prev.i >= cell.i) {
               throw new TestException (
                  "col "+j+": cell "+cell+" follows "+prev);
            }
            if (cell.i >= myNumRows) {
               throw new TestException (
                  "col "+j+" contains cell "+cell+" with nrows="+myNumRows);
            }
            prev = cell;
         }
      }
      for (int i = myNumRows; i < myRows.length; i++) {
         if (myRows[i] != null) {
            throw new TestException ("non-null row at i="+i);
         }
      }
      for (int j = myNumCols; j < myCols.length; j++) {
         if (myCols[j] != null) {
            throw new TestException ("non-null col at j="+j);
         }
      }

      for (int i = 0; i < myNumRows; i++) {
         for (int j = 0; j < myNumCols; j++) {
            SparseMatrixCell rowCell = myRows[i];
            while (rowCell != null && rowCell.j != j) {
               rowCell = rowCell.next;
            }
            SparseMatrixCell colCell = myCols[j];
            while (colCell != null && colCell.i != i) {
               colCell = colCell.down;
            }
            if (get (i, j) == 0) {
               if (colCell != null) {
                  throw new TestException (
                     "column cell found at zero location ("+i+","+j+")");
               }
               if (rowCell != null) {
                  throw new TestException (
                     "row cell found at zero location ("+i+","+j+")");
               }
            }
            else {
               if (rowCell != colCell) {
                  throw new TestException (
                     "row and columns cells not identical at  location (" +
                     i+","+j+")");
               }
               if (rowCell.i != i || rowCell.j != j) {
                  throw new TestException (
                     "cell at location ("+i+","+j +
                     ") has indices ("+rowCell.i+","+rowCell.j+")");
               }
               if (rowCell.value != get (i, j)) {
                  throw new TestException (
                     "cell at location ("+i+","+j +
                     ") has value "+rowCell.value+" vs. "+get (i, j));
               }
            }
         }
      }
   }

   @Override
   public double maxNorm() {
      double m = 0;
      
      for (int i = 0; i < myNumRows; i++) {
         SparseMatrixCell cell;
         for (cell = myRows[i]; cell != null; cell = cell.next) {
            double a = Math.abs(cell.value);
            if (a > m) {
               m = a;
            }
         }
      }
      
      return m;
   }
   
}
