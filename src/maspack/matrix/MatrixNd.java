/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.InternalErrorException;
import maspack.util.Clonable;
import maspack.util.RandomGenerator;

/**
 * Implements general m x n matrices, along with most the commonly used
 * operations associated with such matrices.
 * 
 * <p>
 * Normally, these matrices can be resized, either explicitly through a call to
 * {@link #setSize setSize}, or implicitly through operations that require the
 * matrix size to be modified. The exception is when this matrix is either a
 * submatrix, or is being referenced by a submatrix (see {@link
 * maspack.matrix.SubMatrixNd SubMatrixNd}).
 */
public class MatrixNd extends DenseMatrixBase
   implements java.io.Serializable, LinearTransformNd, Clonable,
              VectorObject<MatrixNd> {
   
   private static final long serialVersionUID = 1L;
   int nrows;
   int ncols;
   int width;
   int base;
   double[] buf = new double[0];
   double[] tmp = new double[0]; // scratch storgae space
   double[] res; // result buffer
   // boolean subMatrix = false;
   boolean fixedSize = false;
   boolean storageFilled = true;
   boolean explicitBuffer = false;
   // int spaceMargin = 0;
   int subMatrixRefCnt = 0;

   void referenceSubMatrix() {
      subMatrixRefCnt++;
      fixedSize = true;
   }

   void dereferenceSubMatrix() {
      subMatrixRefCnt--;
      if (subMatrixRefCnt == 0 && !explicitBuffer) {
         fixedSize = false;
      }
   }

   /**
    * Creates a matrix will initial row and column sizes of zero.
    */
   public MatrixNd() {
      fixedSize = false;
      nrows = 0;
      ncols = 0;
      width = 0;
      base = 0;
   }

   /**
    * Creates a matrix of a specific size, and initializes its elements to 0. It
    * is legal to create a matrix with zero rows and columns.
    * 
    * @param numRows
    * number of rows
    * @param numCols
    * number of columns
    * @throws ImproperSizeException
    * if numRows or numCols are negative
    */
   public MatrixNd (int numRows, int numCols) {
      // subMatrix = false;
      fixedSize = false;
      setSize (numRows, numCols);
   }

   /**
    * Creates a matrix whose size and elements are the same as an existing
    * Matrix.
    * 
    * @param M
    * matrix object to be copied.
    */
   public MatrixNd (Matrix M) {
      // subMatrix = false;
      fixedSize = false;
      if (M instanceof MatrixNd) { // faster to use MatrixNd type
         set ((MatrixNd)M);
      }
      else {
         set (M);
      }
   }

   /**
    * Creates a matrix whose size and elements correspond to a Vector.
    * 
    * @param v
    * vector object to be copied.
    */
   public MatrixNd (Vector v) {
      // subMatrix = false;
      fixedSize = false;
      set (v);
   }

   /**
    * Creates a matrix which is a copy of an existing one.
    * 
    * @param M
    * matrix to be copied.
    */
   public MatrixNd (MatrixNd M) {
      set (M);
   }

   /**
    * Creates a matrix from a two dimensional array of doubles. The matrix size
    * will be determined by the size of this array.
    * 
    * @param values
    * element values for the new matrix
    */
   public MatrixNd (double[][] values) {
      // subMatrix = false;
      fixedSize = false;
      set (values);
   }

   /**
    * Creates a matrix of specific size and initializes its elements from an
    * array of values.
    * 
    * @param numRows
    * number of rows
    * @param numCols
    * number of columns
    * @param values
    * element values for the matrix, with element (i,j) stored at location
    * <code>i*numCols+j</code>
    */
   public MatrixNd (int numRows, int numCols, double[] values) {
      setSize (numRows, numCols);
      set (values);
   }

   private final double[] allocScratchSpace (int size) {
      if (tmp.length < size) {
         tmp = new double[size];
      }
      return tmp;
   }

   /**
    * {@inheritDoc}
    */
   public final int rowSize() {
      return nrows;
   }

   /**
    * {@inheritDoc}
    */
   public final int colSize() {
      return ncols;
   }

   /**
    * Returns true if this matrix is of fixed size. This will be true if the
    * matrix is either a submatrix or is being referenced by a submatrix (see
    * {@link maspack.matrix.SubMatrixNd SubMatrixNd}). If the matrix is not of
    * fixed size, then it can be resized dynamically, either explicitly using
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
    * {@link #isFixedSize isFixedSize} returns false. If necessary, this
    * operation will enlarge the internal buffer associated with this matrix,
    * invalidating buffers previously returned by {@link #getBuffer getBuffer}.
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
    * @see MatrixNd#isFixedSize
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
      if (numCols > width || numRows * width > buf.length) {
         if (explicitBuffer) {
            throw new ImproperSizeException (
               "Requested matrix size too large for explicit internal buffer");
         }
         // need to resize the buffer
         int newWidth, newLength;

         newWidth = Math.max (width, numCols);
         if (buf != null && width != 0) {
            newLength = Math.max (buf.length / width, numRows);
         }
         else {
            newLength = numRows;
         }

         double[] newBuf = new double[newWidth * newLength];
         if (buf != null) {
            int idx0 = 0;
            int idx1 = base; // paranoid
            int nr = Math.min (nrows, numRows);
            int nc = Math.min (ncols, numCols);
            for (int i = 0; i < nr; i++) {
               for (int j = 0; j < nc; j++) {
                  newBuf[idx0 + j] = buf[idx1 + j];
               }
               idx0 += newWidth;
               idx1 += width;
            }
         }
         buf = newBuf;
         this.width = newWidth;
         this.base = 0;
      }
      this.nrows = numRows;
      this.ncols = numCols;
      storageFilled = (buf.length == numRows * numCols);
   }

   /**
    * Returns true if the this matrix is a submatrix; i.e., is a member of the
    * subclass {@link maspack.matrix.SubMatrixNd SubMatrixNd}.
    * 
    * @return true if this matrix is a submatrix
    */
   public boolean isSubMatrix() {
      return false;
   }

   /**
    * Returns the internal buffer used to store the elements in this matrix.
    * When possible, applications should access the matrix elements using the
    * various set and get methods. However, if efficiency requires it, this
    * buffer can be used directly.
    * 
    * <p>
    * Roughly speaking, elements are stored in the buffer in row-column order.
    * However, the buffer may be larger than the matrix, and there may be extra
    * space at the beginning of the buffer. The buffer index which access the
    * element (i,j) is computed as <code>i*w + j + b</code>, where
    * <code>w</code> and <code>b</code> are the width and base values
    * returned by {@link #getBufferWidth getBufferWidth} and
    * {@link #getBufferBase getBufferBase}, respectively.
    * 
    * <p>
    * Note also that if this matrix is a submatrix, the buffer will actually
    * belong to the root matrix, and so modifying values in the buffer
    * (including those outside the nominal range of this matrix) will change the
    * root matrix as well.
    * 
    * <p>
    * If this matrix is resized, then the internal buffer may change and the
    * buffer previously returned by this routine may no longer be valid.
    * 
    * @return internal buffer for this matrix
    * @see MatrixNd#getBufferWidth
    * @see MatrixNd#getBufferBase
    * @see MatrixNd#setBuffer
    */
   public double[] getBuffer() {
      return buf;
   }

   /**
    * Returns the internal buffer width of this matrix, used for computing the
    * index necessary to reference a particular element.
    * 
    * @return internal buffer width
    * @see MatrixNd#getBuffer
    */
   public int getBufferWidth() {
      return width;
   }

   /**
    * Returns the internal buffer base index of this matrix, used for computing
    * the index necessary to reference a particular element.
    * 
    * @return internal buffer base index
    * @see MatrixNd#getBuffer
    */
   public int getBufferBase() {
      return base;
   }

   /**
    * Explicitly sets the size and internal buffer associated with this matrix.
    * Any previous values will be discarded, and the matrix will assume new
    * values with each element (i,j) given by the buffer contents at index
    * <code>[i*bufWidth+j]</code>. In particular, <code>bufWidth</code> must
    * satisfy {@code bufWidth >= numCols}, and the length of
    * <code>buffer</code> must satisfy
    * {@code buffer.length >= numRows*bufWidth}.
    * The matrix can continue to be resized as long as
    * requested sizes stay do not violate these bounds.  The value of
    * <code>bufWidth</code> will become the value returned by {@link
    * #getBufferWidth getBufferWidth}, while the value returned by {@link
    * #getBufferBase getBufferBase} will be 0.
    * 
    * @param numRows
    * new row size
    * @param numCols
    * new column size
    * @param buffer
    * explicit buffer for this matrix
    * @param bufWidth
    * width for the explicit buffer
    * @throws IllegalStateException
    * if this matrix has referring submatrices or is itself a submatrix
    * @throws ImproperSizeException
    * if the specified buffer and/or width is too small for the requested size
    * @see MatrixNd#getBufferWidth
    * @see MatrixNd#getBufferBase
    * @see MatrixNd#unsetBuffer
    */
   public void setBuffer (
      int numRows, int numCols, double[] buffer, int bufWidth) {
      if (subMatrixRefCnt > 0) {
         throw new IllegalStateException (
            "Can't assign buffer: matrix has referring submatrices");
      }
      else if (isSubMatrix()) {
         throw new IllegalStateException (
            "Can't assign buffer: matrix is a submatrix");
      }
      else if (bufWidth < numCols || buffer.length < numRows * bufWidth) {
         throw new ImproperSizeException (
            "Buffer and/or width too small for requested matrix size");
      }
      explicitBuffer = true;
      nrows = numRows;
      ncols = numCols;
      width = bufWidth;
      base = 0;
      buf = buffer;
      storageFilled = (buf.length == numRows * numCols);
   }

   /**
    * Removes an explicit buffer provided for this matrix and replaces it with a
    * default implicit buffer. The matrix retains its present size but all
    * values are replaced with zero.
    * 
    * @throws IllegalStateException
    * if this matrix does not have an explicit buffer given by
    * {@link #setBuffer setBuffer}
    * @throws IllegalStateException
    * if this matrix has referring submatrices
    * @see MatrixNd#setBuffer
    */
   public void unsetBuffer() {
      if (!explicitBuffer) {
         throw new IllegalStateException (
            "Matrix does not have an explicit buffer");
      }
      else if (isSubMatrix()) {
         throw new IllegalStateException (
            "Can't remove explicit buffer: matrix has referring submatrices");
      }
      explicitBuffer = false;
      buf = new double[nrows * ncols];
      width = ncols;
      base = 0;
      storageFilled = true;
   }

   private void doCopySubMatrix (
      int row0, int col0, int numRows, int numCols,
      Matrix Msrc, int rowDest, int colDest) {
      int idx0 = rowDest * width + colDest + base;

      if (Msrc instanceof MatrixNd) {
         MatrixNd MsrcNd = (MatrixNd)Msrc;
         int idx1 = row0 * MsrcNd.width + col0 + MsrcNd.base;
         if (idx0 <= idx1) {
            for (int i = 0; i < numRows; i++) {
               for (int j = 0; j < numCols; j++) {
                  buf[idx0 + j] = MsrcNd.buf[idx1 + j];
               }
               idx0 += width;
               idx1 += MsrcNd.width;
            }
         }
         else {
            idx0 += (numRows - 1) * width;
            idx1 += (numRows - 1) * MsrcNd.width;
            for (int i = numRows - 1; i >= 0; i--) {
               for (int j = numCols - 1; j >= 0; j--) {
                  buf[idx0 + j] = MsrcNd.buf[idx1 + j];
               }
               idx0 -= width;
               idx1 -= MsrcNd.width;
            }
         }
      }
      else {
         for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
               buf[idx0 + j] = Msrc.get (row0 + i, col0 + j);
            }
            idx0 += width;
         }
      }
   }

   /**
    * Copies a sub matrix from a specified source matrix <code>Msrc</code> to
    * a region of this matrix. The base row and column of the source and
    * destination may be different.
    * 
    * @param baseRowSrc
    * starting submatrix row in the source matrix
    * @param baseColSrc
    * starting submatrix column in the source matrix
    * @param numRows
    * number of rows in the submatrix
    * @param numCols
    * number of columns in the submatrix
    * @param Msrc
    * source matrix
    * @param baseRowDest
    * starting submatrix row in this matrix
    * @param baseColDest
    * starting submatrix column in this matrix
    * @throws ImproperSizeException
    * if the specified submatrix dimensions are infeasible
    * @see MatrixNd#getSubMatrix(int,int,MatrixNd)
    * @see MatrixNd#setSubMatrix(int,int,Matrix)
    */
   public void copySubMatrix (
      int baseRowSrc, int baseColSrc, int numRows, int numCols, Matrix Msrc,
      int baseRowDest, int baseColDest) throws ImproperSizeException {
      if (numRows < 0 || numCols < 0) {
         throw new ImproperSizeException ("Negative dimensions");
      }
      if (numRows + baseRowSrc > Msrc.rowSize() ||
          numCols + baseColSrc > Msrc.colSize()) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      if (numRows + baseRowDest > nrows || numCols + baseColDest > ncols) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      doCopySubMatrix (
         baseRowSrc, baseColSrc, numRows, numCols, Msrc, baseRowDest,
         baseColDest);
   }

   /**
    * {@inheritDoc}
    */
   public final double get (int i, int j) {
      try {
         return buf[i * width + j + base];
      }
      catch (Exception e) {
         throw new ArrayIndexOutOfBoundsException ("(" + i + "," + j + ")");
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void get (double[] values) {
      get (values, ncols);
   }

   public final void get (double[] values, int w) {
      if (storageFilled && w == ncols) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            values[i] = buf[i];
         }
      }
      else {
         int idx1 = base;
         int idx0 = 0;
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               values[idx0 + j] = buf[idx1 + j];
            }
            idx0 += w;
            idx1 += width;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void getColumn (int j, double[] values) {
      int idx = j + base;
      for (int i = 0; i < nrows; i++) {
         values[i] = buf[idx];
         idx += width;
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void getColumn (int j, double[] values, int off) {
      int idx = j + base;
      for (int i = 0; i < nrows; i++) {
         values[i + off] = buf[idx];
         idx += width;
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void getRow (int i, double[] values) {
      int idx = i * width + base;
      for (int j = 0; j < ncols; j++) {
         values[j] = buf[idx++];
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void getRow (int i, double[] values, int off) {
      int idx = i * width + base;
      for (int j = 0; j < ncols; j++) {
         values[j + off] = buf[idx++];
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void getColumn (int j, Vector v) {
      if (v.size() != rowSize()) {
         if (v.isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            v.setSize (rowSize());
         }
      }
      int idx = j + base;
      for (int i = 0; i < nrows; i++) {
         v.set (i, buf[idx]);
         idx += width;
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void getRow (int i, Vector v) {
      if (v.size() != colSize()) {
         if (v.isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            v.setSize (colSize());
         }
      }
      int idx = i * width + base;
      for (int j = 0; j < ncols; j++) {
         v.set (j, buf[idx++]);
      }
   }

   /**
    * Gets a submatrix of this matrix. The first row and column of the submatrix
    * are given by <code>baseRow</code> and <code>baseCol</code>, and the
    * values are written into the matrix <code>Mdest</code>. The size of the
    * submatrix is determined by the dimensions of <code>Mdest</code>.
    * 
    * @param baseRow
    * first row of the submatrix
    * @param baseCol
    * first column of the submatrix
    * @param Mdest
    * destination for submatrix values
    * @throws ImproperSizeException
    * if <code>baseRow</code> or <code>baseCol</code> are negative, or if
    * the submatrix exceeds the current matrix bounds.
    * @see MatrixNd#copySubMatrix
    */
   public void getSubMatrix (int baseRow, int baseCol, MatrixNd Mdest)
      throws ImproperSizeException {
      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      if (baseRow + Mdest.nrows > nrows || baseCol + Mdest.ncols > ncols) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }

      Mdest.doCopySubMatrix (
         baseRow, baseCol, Mdest.nrows, Mdest.ncols, this, 0, 0);
   }

   /**
    * Gets a submatrix of this matrix. The row and column indices of the
    * submatrix are specified by <code>rows</code> and the <code>cols</code>,
    * respectively, and the values are written into the matrix
    * <code>Mdest</code>. The size of the submatrix is determined by the
    * existing dimensions of <code>Mdest</code>.
    * 
    * @param rows
    * row indices of the submatrix
    * @param cols
    * column indices of the submatrix
    * @param Mdest
    * destination for submatrix values
    * @throws ImproperSizeException
    * if the number of rows or columns exceed the dimensions of
    * <code>Mdest</code>, or if any of the specified indices lie outside the
    * bounds of this matrix.
    */
   public void getSubMatrix (int[] rows, int[] cols, MatrixNd Mdest)
      throws ImproperSizeException {
      double[] dbuf;
      double[] sbuf = buf;
      int sw = width;
      int sb = base;
      int dw, db;

      if (rows.length > Mdest.nrows || cols.length > Mdest.ncols) {
         throw new ImproperSizeException (
            "Number of rows or columns exceeds destination matrix size");
      }

      if (Mdest == this) {
         dbuf = new double[Mdest.nrows * Mdest.ncols];
         Mdest.get (dbuf);
         dw = Mdest.ncols;
         db = 0;
      }
      else {
         dbuf = Mdest.buf;
         dw = Mdest.width;
         db = Mdest.base;
      }
      for (int i = 0; i < rows.length; i++) {
         for (int j = 0; j < cols.length; j++) {
            int srci = rows[i];
            int srcj = cols[j];
            if (srci < 0 || srci >= nrows || srcj < 0 || srcj >= ncols) {
               throw new ImproperSizeException (
                  "Submatrix indices exceed matrix bounds");
            }
            dbuf[i * dw + j + db] = sbuf[srci * sw + srcj + sb];
         }
      }
      if (Mdest == this) {
         Mdest.set (dbuf);
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void set (int i, int j, double value) {
      buf[i * width + j + base] = value;
   }

   /**
    * Adds a value to the indicated entry.
    */
   public final void add (int i, int j, double value) {
      buf[i * width + j + base] += value;
   }

   /**
    * {@inheritDoc}
    */
   public final void set (double[] values) {
      if (storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = values[i];
         }
      }
      else {
         int idx0 = base;
         int k = 0;
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               buf[idx0 + j] = values[k++];
            }
            idx0 += width;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void setColumn (int j, double[] values) {
      int idx = j + base;
      for (int i = 0; i < nrows; i++) {
         buf[idx] = values[i];
         idx += width;
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void setRow (int i, double[] values) {
      int idx = i * width + base;
      for (int j = 0; j < ncols; j++) {
         buf[idx++] = values[j];
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, Vector v) {
      if (v.size() != rowSize()) {
         throw new ImproperSizeException();
      }
      int idx = j + base;
      for (int i = 0; i < nrows; i++) {
         buf[idx] = v.get (i);
         idx += width;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setRow (int i, Vector v) {
      if (v.size() != colSize()) {
         throw new ImproperSizeException();
      }
      int idx = i * width + base;
      for (int j = 0; j < ncols; j++) {
         buf[idx++] = v.get (j);
      }
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
    * @see MatrixNd#copySubMatrix
    */
   public void setSubMatrix (int baseRow, int baseCol, Matrix Msrc)
      throws ImproperSizeException {
      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      if (baseRow + Msrc.rowSize() > nrows ||
          baseCol + Msrc.colSize() > ncols) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      doCopySubMatrix (
         0, 0, Msrc.rowSize(), Msrc.colSize(), Msrc, baseRow, baseCol);
   }

   /**
    * Sets a submatrix of this matrix. The row and column indices of the
    * submatrix are specified by <code>rows</code> and the <code>cols</code>,
    * respectively, and the new values are given by the matrix <code>Msrc</code>.
    * The size of the submatrix is determined by the existing dimensions of
    * <code>Msrc</code>.
    * 
    * @param rows
    * row indices of the submatrix
    * @param cols
    * column indices of the submatrix
    * @param Msrc
    * new values for the submatrix
    * @throws ImproperSizeException
    * if the number of rows or columns exceed the dimensions of
    * <code>Msrc</code>, or if any of the specified indices lie outside the
    * bounds of this matrix.
    */
   public void setSubMatrix (int[] rows, int[] cols, MatrixNd Msrc)
      throws ImproperSizeException {
      double[] dbuf;
      double[] sbuf = Msrc.buf;
      int sw = Msrc.width;
      int sb = Msrc.base;
      int dw, db;

      if (rows.length > Msrc.nrows || cols.length > Msrc.ncols) {
         throw new ImproperSizeException (
            "Number of rows or columns exceeds destination matrix size");
      }

      if (Msrc == this) {
         dbuf = new double[nrows * ncols];
         get (dbuf);
         dw = ncols;
         db = 0;
      }
      else {
         dbuf = buf;
         dw = width;
         db = base;
      }
      for (int i = 0; i < rows.length; i++) {
         for (int j = 0; j < cols.length; j++) {
            int desti = rows[i];
            int destj = cols[j];
            if (desti < 0 || desti >= nrows || destj < 0 || destj >= ncols) {
               throw new ImproperSizeException (
                  "Submatrix indices exceed matrix bounds");
            }
            dbuf[desti * dw + destj + db] = sbuf[i * sw + j + sb];
         }
      }
      if (Msrc == this) {
         set (dbuf);
      }
   }

   /**
    * Adds a submatrix to this matrix. The starting row and column indices of
    * the submatrix are specified by <code>baseRow</code> and
    * <code>baseCol</code>, respectively, and the values to add are given by
    * the matrix <code>Msrc</code>.  The size of the submatrix is determined by
    * the existing dimensions of <code>Msrc</code>.
    * 
    * @param baseRow
    * index of the first row of the submatrix
    * @param baseCol
    * index of the first column of the submatrix
    * @param Msrc
    * values to add to the submatrix.
    * @throws ImproperSizeException
    * if <code>baseRow</code> or <code>baseCol</code> are negative, or if
    * the submatrix exceeds the current matrix bounds
    * @throws IllegalArgumentException if <code>Msrc</code> equals this
    * this matrix.
    */
   public void addSubMatrix (int baseRow, int baseCol, MatrixNd Msrc)
      throws ImproperSizeException {

      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      if (baseRow + Msrc.rowSize() > nrows ||
          baseCol + Msrc.colSize() > ncols) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }

      if (Msrc == this) {
         throw new IllegalArgumentException (
            "Msrc can not be the same as this matrix");
      }

      double[] sbuf = Msrc.buf;
      int sw = Msrc.width;
      int sb = Msrc.base;
      int dw = width;
      int db = baseRow*dw + baseCol + base;

      for (int i=0; i<Msrc.rowSize(); i++) {
         for (int j=0; j<Msrc.colSize(); j++) {
            buf[i*dw+j+db] += sbuf[i*sw+j+sb];
         }
      }
   }

   /**
    * Adds a scaled submatrix to this matrix. The starting row and column
    * indices of the submatrix are specified by <code>baseRow</code> and
    * <code>baseCol</code>, respectively, and the values to add are given by
    * the matrix <code>Msrc</code>.  The size of the submatrix is determined by
    * the existing dimensions of <code>Msrc</code>.
    * 
    * @param baseRow
    * index of the first row of the submatrix
    * @param baseCol
    * index of the first column of the submatrix
    * @param s
    * scale factor for the added submatrix
    * @param Msrc
    * values to add to the submatrix.
    * @throws ImproperSizeException
    * if <code>baseRow</code> or <code>baseCol</code> are negative, or if
    * the submatrix exceeds the current matrix bounds
    * @throws IllegalArgumentException if <code>Msrc</code> equals this
    * this matrix.
    */
   public void addScaledSubMatrix (
      int baseRow, int baseCol, double s, MatrixNd Msrc)
      throws ImproperSizeException {

      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      if (baseRow + Msrc.rowSize() > nrows ||
          baseCol + Msrc.colSize() > ncols) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }

      if (Msrc == this) {
         throw new IllegalArgumentException (
            "Msrc can not be the same as this matrix");
      }

      double[] sbuf = Msrc.buf;
      int sw = Msrc.width;
      int sb = Msrc.base;
      int dw = width;
      int db = baseRow*dw + baseCol + base;

      for (int i=0; i<Msrc.rowSize(); i++) {
         for (int j=0; j<Msrc.colSize(); j++) {
            buf[i*dw+j+db] += s*sbuf[i*sw+j+sb];
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M instanceof MatrixNd) {
         set ((MatrixNd)M);
      }
      else {
         super.set (M);
      }
   }

   /**
    * Sets the size and values of this matrix to those of matrix M.
    * 
    * @param M
    * matrix to be copied
    * @throws ImproperSizeException
    * if matrices have different sizes and this matrix cannot be resized
    * accordingly
    */
   public void set (MatrixNd M) throws ImproperSizeException {
      if (M == this) {
         return;
      }
      if (nrows != M.nrows || ncols != M.ncols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M.nrows, M.ncols);
         }
      }
      if (storageFilled & M.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = M.buf[i];
         }
      }
      else {
         scaleCopy (1, M, base, M.base);
      }
   }

   /**
    * Sets this matrix to the identity matrix. If this matrix matrix is not
    * square, then element (i,j) is set to 1 if i and j are equal, and 0
    * otherwise.
    */
   public void setIdentity() {
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            buf[idx + j] = (i == j ? 1 : 0);
         }
         idx += width;
      }
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   public void setZero() {
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            buf[idx + j] = 0;
         }
         idx += width;
      }
   }

   /**
    * Sets this matrix to a diagonal matrix whose diagonal elements are
    * specified by an array.
    * 
    * @param diag
    * diagonal elements for this matrix
    * @throws ImproperSizeException
    * if the size of <code>diag</code> does not equal the minimum matrix
    * dimension
    */
   public void setDiagonal (VectorNd diag) {
      if (diag.size() != Math.min (nrows, ncols)) {
         throw new ImproperSizeException ("Imcompatible dimensions");
      }
      setDiagonal (diag.buf);
   }

   /**
    * Sets this matrix to a diagonal matrix whose diagonal elements are
    * specified by the leading elements of an array of doubles
    * 
    * @param diag
    * diagonal elements for this matrix
    * @throws ImproperSizeException
    * if the length of <code>diag</code> is less than the minimum matrix
    * dimension
    */
   public void setDiagonal (double[] diag) {
      if (diag.length < Math.min (nrows, ncols)) {
         throw new ImproperSizeException ("Insufficient diagonal values");
      }
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            buf[idx + j] = (i == j ? diag[i] : 0);
         }
         idx += width;
      }
   }

   /**
    * Adds {@code d} to the diagonal of this matrix.
    * 
    * @param d value to add to the diagonal
    */
   public void addDiagonal (double d) {
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         buf[idx + i] += d;
         idx += width;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom() {
      super.setRandom();
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper) {
      super.setRandom (lower, upper);
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper, Random generator) {
      super.setRandom (lower, upper, generator);
   }

   /**
    * Sets this matrix to a random orthogonal matrix. This is a matrix whose
    * rows and columns are randomly selected while still being orthonormal.
    */
   public void setRandomOrthogonal() {
      setRandomOrthogonal (RandomGenerator.get());
   }


//   /**
//    * Sets this matrix to a random orthogonal matrix, using a supplied random
//    * number generator.
//    * 
//    * @param generator
//    * random number generator
//    * @see #setRandomOrthogonal()
//    */
//   public void setRandomOrthogonalOld (Random generator) {
//      setIdentity();
//
//      if (ncols >= nrows) {
//         double[] w = new double[nrows];
//         double[] v = new double[ncols];
//
//         int nr = nrows;
//         SubMatrixNd SubMat =
//            new SubMatrixNd (nr - 1, nr - 1, 1, ncols - nr + 1, this);
//         // makeSubMatrix (nr-1, nr-1, 1, ncols-nr+1);
//         for (int i = 0; i < nr; i++) {
//            for (int j = 0; j < ncols; j++) {
//               v[j] = generator.nextDouble() - 0.5;
//            }
//            houseVector (v, 0, ncols);
//            // colHouseMul (v, w);
//            double beta = 0;
//            for (int k=0; k<SubMat.ncols; k++) {
//               beta += v[k]*v[k];
//            }
//            beta = 2/beta;
//            SubMat.colHouseMul (v, w, beta);
//
//            if (i < nr - 1) { // makeSubMatrix (-1, -1, nrows+1, ncols+1);
//               SubMat.resetDimensions (
//                  -1, -1, SubMat.nrows + 1, SubMat.ncols + 1);
//            }
//         }
//      }
//      else {
//         double[] w = new double[ncols];
//         double[] v = new double[nrows];
//
//         int nc = ncols;
//         SubMatrixNd SubMat =
//            new SubMatrixNd (nc - 1, nc - 1, nrows - nc + 1, 1, this);
//         // makeSubMatrix (nc-1, nc-1, nrows-nc+1, 1);
//         for (int j = 0; j < nc; j++) {
//            for (int i = 0; i < nrows; i++) {
//               v[i] = generator.nextDouble() - 0.5;
//            }
//            houseVector (v, 0, nrows);
//            double beta = 0;
//            for (int k=0; k<SubMat.nrows; k++) {
//               beta += v[k]*v[k];
//            }
//            beta = 2/beta;
//            // rowHouseMul (v, w);
//            SubMat.rowHouseMul (v, w, beta);
//
//            if (j < nc - 1) { // makeSubMatrix (-1, -1, nrows+1, ncols+1);
//               SubMat.resetDimensions (
//                  -1, -1, SubMat.nrows + 1, SubMat.ncols + 1);
//            }
//         }
//      }
//   }

   /** 
    * Sets this matrix to a random orthogonal matrix, using a supplied random
    * number generator.
    * 
    * @param generator
    * random number generator
    * @see #setRandomOrthogonal()
    */
   public void setRandomOrthogonal (Random generator) {
      setIdentity();

      if (ncols >= nrows) {
         double[] w = new double[nrows];
         double[] v = new double[ncols];

         int nr = nrows;
         // SubMatrixNd SubMat =
         //    new SubMatrixNd (nr - 1, nr - 1, 1, ncols - nr + 1, this);
         // makeSubMatrix (nr-1, nr-1, 1, ncols-nr+1);
         for (int i = nr-1; i >= 0; i--) {
            for (int j = i; j < ncols; j++) {
               v[j] = generator.nextDouble() - 0.5;
            }
            double beta = QRDecomposition.houseVector (v, i, ncols);
            // colHouseMul (v, w);
            QRDecomposition.housePostMul (
               buf, width, nrows, ncols, i, i, beta, v, w);
         }
      }
      else {
         double[] w = new double[ncols];
         double[] v = new double[nrows];

         int nc = ncols;
         // SubMatrixNd SubMat =
         //    new SubMatrixNd (nc - 1, nc - 1, nrows - nc + 1, 1, this);
         // makeSubMatrix (nc-1, nc-1, nrows-nc+1, 1);
         for (int j = nc-1; j >= 0; j--) {
            for (int i = j; i < nrows; i++) {
               v[i] = generator.nextDouble() - 0.5;
            }
            double beta = QRDecomposition.houseVector (v, j, nrows);
            QRDecomposition.housePreMul (
               buf, width, nrows, ncols, j, j, beta, v, w);
         }
      }
   }

   /**
    * Sets this matrix to a random matrix with specified singular values. The
    * size of the matrix is unchanged, and the number of supplied singular
    * values must equal the minimum matrix dimension. This routine is useful for
    * creating random matrices with a prescribed degeneracy.
    * 
    * @param singularValues
    * singular values for this matrix
    * @throws ImproperSizeException
    * if the length of <code>singularValues</code> is less than the minimum
    * matrix dimension
    */
   public void setRandomSvd (double[] singularValues) {
      setRandomSvd (singularValues, RandomGenerator.get());
   }

   /**
    * Sets this matrix to a random matrix with specified singular values, using
    * a supplied random number generator.
    * 
    * @param singularValues
    * singular values for this matrix
    * @param generator
    * random number generator
    * @throws ImproperSizeException
    * if the length of <code>singularValues</code> is less than the minimum
    * matrix dimension
    * @see #setRandomSvd(double[])
    */
   public void setRandomSvd (double[] singularValues, Random generator) {
      int mind = Math.min (nrows, ncols);
      if (singularValues.length < mind) {
         throw new ImproperSizeException (
            "Not enough singular values specified");
      }
      setRandomOrthogonal (generator);
      MatrixNd O = new MatrixNd (mind, mind);
      O.setRandomOrthogonal (generator);
      if (nrows >= ncols) {
         O.mulDiagonalLeft (singularValues);
         mul (O);
      }
      else {
         O.mulDiagonalRight (singularValues);
         mul (O, this);
      }
   }

   private boolean mulInit (int nrows1, int ncols2, MatrixNd m1, MatrixNd m2) {
      boolean resizeLater = false;
      boolean needScratchSpace = (buf == m1.buf || buf == m2.buf);

      if (nrows != nrows1 || ncols != ncols2) {
         if (fixedSize) {
            throw new ImproperSizeException (
               "Incompatible dimensions");
         }
         else if (!needScratchSpace) {
            setSize (nrows1, ncols2);
         }
         else {
            resizeLater = true;
         }
      }
      if (needScratchSpace) {
         res = allocScratchSpace (nrows1 * ncols2);
      }
      else {
         res = buf;
      }
      return resizeLater;
   }
   
   private boolean mulInit (int nrows1, int ncols2, DenseMatrixBase m1, DenseMatrixBase m2) {
      
      boolean resizeLater = false;
      boolean needScratchSpace = (this==m1 || this==m2);

      if (nrows != nrows1 || ncols != ncols2) {
         if (fixedSize) {
            throw new ImproperSizeException (
               "Incompatible dimensions");
         }
         else if (!needScratchSpace) {
            setSize (nrows1, ncols2);
         }
         else {
            resizeLater = true;
         }
      }
      if (needScratchSpace) {
         res = allocScratchSpace (nrows1 * ncols2);
      }
      else {
         res = buf;
      }
      return resizeLater;
   }

   private void copyBackResult() {
      int idx0 = base;
      int idx1 = 0;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            buf[idx0 + j] = res[idx1 + j];
         }
         idx0 += width;
         idx1 += ncols;
      }
   }

   /**
    * Multiplies this matrix by M1 and places the result in this matrix. This
    * matrix is resized if necessary.
    * 
    * @param M1
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M1 do not conform, or if this matrix needs resizing but
    * is of fixed size
    */
   public void mul (MatrixNd M1) {
      mul (this, M1);
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
   public void mul (MatrixNd M1, MatrixNd M2) throws ImproperSizeException {
      if (M1.ncols != M2.nrows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      boolean resizeLater = mulInit (M1.nrows, M2.ncols, M1, M2);

      int idx0 = base;
      int rwidth = width;
      if (res != buf) {
         idx0 = 0;
         rwidth = M2.ncols;
      }
      int idx1 = M1.base;
      int ncols1 = M1.ncols;
      for (int i = 0; i < M1.nrows; i++) {
         for (int j = 0; j < M2.ncols; j++) {
            double sum = 0;
            int idx2 = j + M2.base;
            for (int k = 0; k < ncols1; k++) {
               sum += M1.buf[idx1 + k] * M2.buf[idx2];
               idx2 += M2.width;
            }
            res[idx0 + j] = sum;
         }
         idx0 += rwidth;
         idx1 += M1.width;
      }

      if (resizeLater) {
         setSize (M1.nrows, M2.ncols);
      }
      if (res != buf) {
         copyBackResult();
      }
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
   public void mul (DenseMatrixBase M1, DenseMatrixBase M2) throws ImproperSizeException {
      if (M1.colSize() != M2.rowSize()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      
      int nRows = M1.rowSize();
      int nCols = M2.colSize();
      int nMul = M2.rowSize();
      
      // create workspace if necessary and resize matrix
      boolean resizeLater = mulInit(nRows, nCols, M1, M2);
    
      for (int i = 0; i < nRows; i++) {
         for (int j = 0; j < nCols; j++) {
            double sum = 0;
            for (int k = 0; k < nMul; k++) {
               sum += M1.get(i, k)*M2.get(k, j);
            }
            res[i*nCols + j] = sum;
         }
      }

      if (resizeLater) {
         setSize (M1.rowSize(), M2.colSize());
      }
      if (res != buf) {
         copyBackResult();
      }
   }

   /**
    * Multiplies this matrix by the transpose of M1 and places the result in
    * this matrix. This matrix is resized if necessary.
    * 
    * @param M1
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and the transpose of M1 do not conform, or if this matrix
    * needs resizing but is of fixed size
    */
   public void mulTranspose (MatrixNd M1) {
      mulTransposeRight (this, M1);
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
   public void mulTransposeRight (MatrixNd M1, MatrixNd M2)
      throws ImproperSizeException {
      if (M1.ncols != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      boolean resizeLater = mulInit (M1.nrows, M2.nrows, M1, M2);

      int idx0 = base;
      int rwidth = width;
      if (res != buf) {
         idx0 = 0;
         rwidth = M2.nrows;
      }
      int idx1 = M1.base;
      int ncols1 = M1.ncols;
      for (int i = 0; i < M1.nrows; i++) {
         int idx2 = M2.base;
         for (int j = 0; j < M2.nrows; j++) {
            double sum = 0;
            for (int k = 0; k < ncols1; k++) {
               sum += M1.buf[idx1 + k] * M2.buf[idx2 + k];
            }
            idx2 += M2.width;
            res[idx0 + j] = sum;
         }
         idx0 += rwidth;
         idx1 += M1.width;
      }

      if (resizeLater) {
         setSize (M1.nrows, M2.nrows);
      }
      if (res != buf) {
         copyBackResult();
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
   public void mulTransposeLeft (MatrixNd M1, MatrixNd M2)
      throws ImproperSizeException {
      if (M1.nrows != M2.nrows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      boolean resizeLater = mulInit (M1.ncols, M2.ncols, M1, M2);

      int idx0 = base;
      int rwidth = width;
      if (res != buf) {
         idx0 = 0;
         rwidth = M2.ncols;
      }
      int ncols1 = M1.nrows;
      for (int i = 0; i < M1.ncols; i++) {
         for (int j = 0; j < M2.ncols; j++) {
            double sum = 0;
            int idx1 = i + M1.base;
            int idx2 = j + M2.base;
            for (int k = 0; k < ncols1; k++) {
               sum += M1.buf[idx1] * M2.buf[idx2];
               idx1 += M1.width;
               idx2 += M2.width;
            }
            res[idx0 + j] = sum;
         }
         idx0 += rwidth;
      }

      if (resizeLater) {
         setSize (M1.ncols, M2.ncols);
      }
      if (res != buf) {
         copyBackResult();
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
   public void mulTransposeBoth (MatrixNd M1, MatrixNd M2)
      throws ImproperSizeException {
      if (M1.nrows != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      boolean resizeLater = mulInit (M1.ncols, M2.nrows, M1, M2);

      int idx0 = base;
      int rwidth = width;
      if (res != buf) {
         idx0 = 0;
         rwidth = M2.nrows;
      }
      int ncols1 = M1.nrows;
      for (int i = 0; i < M1.ncols; i++) {
         int idx2 = M2.base;
         for (int j = 0; j < M2.nrows; j++) {
            double sum = 0;
            int idx1 = i + M1.base;
            for (int k = 0; k < ncols1; k++) {
               sum += M1.buf[idx1] * M2.buf[idx2 + k];
               idx1 += M1.width;
            }
            idx2 += M2.width;
            res[idx0 + j] = sum;
         }
         idx0 += rwidth;
      }

      if (resizeLater) {
         setSize (M1.ncols, M2.nrows);
      }
      if (res != buf) {
         copyBackResult();
      }
   }

   /**
    * Pre-multiplies, in place, this matrix by a diagonal matrix whose
    * (diagonal) elements are specified by a vector. This is the same as
    * multiplying the rows of this matrix by the elements of the vector.
    * 
    * @param diag
    * specifies the diagonal elements of the implied left-hand matrix
    * @throws ImproperSizeException
    * if the size of <code>diag</code> does not equal the row size of this
    * matrix
    */
   public void mulDiagonalLeft (VectorNd diag) {
      if (diag.size != nrows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      mulDiagonalLeft (diag.buf);
   }

   /**
    * Pre-multiplies, in place, this matrix by a diagonal matrix whose
    * (diagonal) elements are specified by the leading elements of an array of
    * doubles. This is the same as multiplying the rows of this matrix by the
    * elements of the array.
    * 
    * @param diag
    * specifies the diagonal elements of the implied left-hand matrix
    * @throws ImproperSizeException
    * if the length of <code>diag</code> is less than the row size of this
    * matrix
    */
   public void mulDiagonalLeft (double[] diag) {
      if (diag.length < nrows) {
         throw new ImproperSizeException ("Insufficient diagonal values");
      }
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         double s = diag[i];
         for (int j = 0; j < ncols; j++) {
            buf[idx + j] *= s;
         }
         idx += width;
      }
   }

   /**
    * Post-multiplies, in place, this matrix by a diagonal matrix whose
    * (diagonal) elements are specified by a vector. This is the same as
    * multiplying the columns of this matrix by the elements of the vector.
    * 
    * @param diag
    * specifies the diagonal elements of the implied right-hand matrix
    * @throws ImproperSizeException
    * if the size of <code>diag</code> does not equal the column size of this
    * matrix
    */
   public void mulDiagonalRight (VectorNd diag) {
      if (diag.size != ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      mulDiagonalRight (diag.buf);
   }

   /**
    * Post-multiplies, in place, this matrix by a diagonal matrix whose
    * (diagonal) elements are specified by the leading elements of an array of
    * doubles. This is the same as multiplying the columns of this matrix by the
    * elements of the array.
    * 
    * @param diag
    * specifies the diagonal elements of the implied right-hand matrix
    * @throws ImproperSizeException
    * if the length of <code>diag</code> is less than the column size of this
    * matrix
    */
   public void mulDiagonalRight (double[] diag) {
      if (diag.length < ncols) {
         throw new ImproperSizeException ("Insufficient diagonal values");
      }
      for (int j = 0; j < ncols; j++) {
         double s = diag[j];
         int idx = j + base;
         for (int i = 0; i < nrows; i++) {
            buf[idx] *= s;
            idx += width;
         }
      }
   }

   /**
    * Sets the elements of this matrix to their absolute values.
    */
   public void absolute() {
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            double x = buf[idx + j];
            if (x < 0) {
               buf[idx + j] = -x;
            }
         }
         idx += width;
      }
   }

   /**
    * Returns the maximum element value of this matrix.
    * 
    * @return maximum element value
    */
   public double maxElement() {
      double max = Double.NEGATIVE_INFINITY;
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            double x = buf[idx + j];
            if (x > max) {
               max = x;
            }
         }
         idx += width;
      }
      return max;
   }

   /**
    * Returns the minimum element value of this matrix.
    * 
    * @return minimum element value
    */
   public double minElement() {
      double min = Double.POSITIVE_INFINITY;
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            double x = buf[idx + j];
            if (x < min) {
               min = x;
            }
         }
         idx += width;
      }
      return min;
   }

   private static final int ADD = 1;
   private static final int SUB = 2;

   private void addOp (int op, MatrixNd M1, MatrixNd M2) {
      res = buf;
      int idx0 = base;
      int rwidth = width;

      if ((buf == M1.buf && base != M1.base) ||
          (buf == M2.buf && base != M2.base)) {
         res = allocScratchSpace (nrows * ncols);
         idx0 = 0;
         rwidth = ncols;
      }

      int idx1 = M1.base;
      int idx2 = M2.base;
      if (op == ADD) {
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               res[idx0 + j] = M1.buf[idx1 + j] + M2.buf[idx2 + j];
            }
            idx0 += rwidth;
            idx1 += M1.width;
            idx2 += M2.width;
         }
      }
      else if (op == SUB) {
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               res[idx0 + j] = M1.buf[idx1 + j] - M2.buf[idx2 + j];
            }
            idx0 += rwidth;
            idx1 += M1.width;
            idx2 += M2.width;
         }
      }

      if (res != buf) {
         copyBackResult();
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
   public void add (MatrixNd M) throws ImproperSizeException {
      if (nrows != M.nrows || ncols != M.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (storageFilled & M.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] += M.buf[i];
         }
      }
      else {
         addOp (ADD, this, M);
      }
   }

   /**
    * Scales the matrix M and add the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    * @throws ImproperSizeException
    * if this matrix and M have different sizes
    */
   public void scaledAdd (double s, MatrixNd M) throws ImproperSizeException {
      if (nrows != M.nrows || ncols != M.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (storageFilled & M.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] += s * M.buf[i];
         }
      }
      else {
         res = buf;
         int idx0 = base;
         int rwidth = width;

         if ((buf == M.buf && base != M.base)) {
            res = allocScratchSpace (nrows * ncols);
            idx0 = 0;
            rwidth = ncols;
         }

         int idx1 = M.base;
         int idx2 = base;
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               res[idx0 + j] = s * M.buf[idx1 + j] + buf[idx2 + j];
            }
            idx0 += rwidth;
            idx1 += M.width;
            idx2 += width;
         }
         if (res != buf) {
            copyBackResult();
         }
      }
   }

   /**
    * Computes s M1 + M2 and places the result in this matrix. This matrix is
    * resized if necessary.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    * @param M2
    * matrix to be added
    * @throws ImproperSizeException
    * if matrices M1 and M2 have different sizes, or if this matrix needs
    * resizing but is of fixed size
    */
   public void scaledAdd (double s, MatrixNd M1, MatrixNd M2)
      throws ImproperSizeException {
      if (M1.nrows != M2.nrows || M1.ncols != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (nrows != M1.nrows || ncols != M1.ncols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.nrows, M1.ncols);
         }
      }
      if (storageFilled & M1.storageFilled & M2.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = s * M1.buf[i] + M2.buf[i];
         }
      }
      else {
         res = buf;
         int idx0 = base;
         int rwidth = width;

         if ((buf == M1.buf && base != M1.base) ||
             (buf == M2.buf && base != M2.base)) {
            res = allocScratchSpace (nrows * ncols);
            idx0 = 0;
            rwidth = ncols;
         }

         int idx1 = M1.base;
         int idx2 = M2.base;
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               res[idx0 + j] = s * M1.buf[idx1 + j] + M2.buf[idx2 + j];
            }
            idx0 += rwidth;
            idx1 += M1.width;
            idx2 += M2.width;
         }
         if (res != buf) {
            copyBackResult();
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
   public void add (MatrixNd M1, MatrixNd M2) throws ImproperSizeException {
      if (M1.nrows != M2.nrows || M1.ncols != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (nrows != M1.nrows || ncols != M1.ncols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.nrows, M1.ncols);
         }
      }
      if (storageFilled & M1.storageFilled & M2.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = M2.buf[i] + M1.buf[i];
         }
      }
      else {
         addOp (ADD, M1, M2);
      }
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M1 have different sizes
    */
   public void sub (MatrixNd M1) throws ImproperSizeException {
      if (nrows != M1.nrows || ncols != M1.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (storageFilled & M1.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] -= M1.buf[i];
         }
      }
      else {
         addOp (SUB, this, M1);
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
   public void sub (MatrixNd M1, MatrixNd M2) throws ImproperSizeException {
      if (M1.nrows != M2.nrows || M1.ncols != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (nrows != M1.nrows || ncols != M1.ncols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.nrows, M1.ncols);
         }
      }
      if (storageFilled & M1.storageFilled & M2.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = M1.buf[i] - M2.buf[i];
         }
      }
      else {
         addOp (SUB, M1, M2);
      }
   }

   private void scaleCopy (double scale, MatrixNd M1, int idx0, int idx1) {
      if (idx0 <= idx1) {
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               buf[idx0 + j] = scale * M1.buf[idx1 + j];
            }
            idx0 += width;
            idx1 += M1.width;
         }
      }
      else {
         idx0 += (nrows - 1) * width;
         idx1 += (nrows - 1) * M1.width;
         for (int i = nrows - 1; i >= 0; i--) {
            for (int j = ncols - 1; j >= 0; j--) {
               buf[idx0 + j] = scale * M1.buf[idx1 + j];
            }
            idx0 -= width;
            idx1 -= M1.width;
         }
      }
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      negate (this);
   }

   /**
    * Sets this matrix to the negative of M1. This matrix is resized if
    * necessary.
    * 
    * @param M1
    * matrix to negate
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void negate (MatrixNd M1) throws ImproperSizeException {
      if (nrows != M1.nrows || ncols != M1.ncols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.nrows, M1.ncols);
         }
      }
      if (storageFilled & M1.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = -M1.buf[i];
         }
      }
      else {
         scaleCopy (-1, M1, base, M1.base);
      }
   }

   /**
    * Scales the elements of matrix M1 by <code>s</code> and places the
    * results in this matrix. This matrix is resized if necessary.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void scale (double s, MatrixNd M1) throws ImproperSizeException {
      if (nrows != M1.nrows || ncols != M1.ncols) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            setSize (M1.nrows, M1.ncols);
         }
      }
      if (storageFilled & M1.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            buf[i] = s * M1.buf[i];
         }
      }
      else {
         scaleCopy (s, M1, base, M1.base);
      }
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      scale (s, this);
   }

   /**
    * Replaces this matrix by its tranpose. The matrix is resized if necessary.
    * 
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void transpose() {
      if (nrows == ncols) { // fast transpose
         int idx0 = base;
         for (int i = 0; i < nrows; i++) {
            int idx1 = idx0 + width + i;
            for (int j = i + 1; j < ncols; j++) {
               double tmp = buf[idx0 + j];
               buf[idx0 + j] = buf[idx1];
               buf[idx1] = tmp;
               idx1 += width;
            }
            idx0 += width;
         }
      }
      else {
         transpose (this);
      }
   }

   /**
    * Takes the transpose of matrix M1 and places the result in this matrix. The
    * matrix is resized if necessary.
    * 
    * @param M1
    * matrix to take the transpose of
    * @throws ImproperSizeException
    * if this matrix needs resizing but is of fixed size
    */
   public void transpose (MatrixNd M1) throws ImproperSizeException {
      boolean resizeLater = false;
      int ridx, rwidth;

      if (nrows != M1.ncols || ncols != M1.nrows) {
         if (fixedSize) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else if (buf != M1.buf) {
            setSize (M1.ncols, M1.nrows);
         }
         else {
            resizeLater = true;
         }
      }
      if (buf == M1.buf) {
         res = allocScratchSpace (M1.ncols * M1.nrows);
         ridx = 0;
         rwidth = M1.nrows;
      }
      else {
         res = buf;
         ridx = base;
         rwidth = width;
      }

      for (int i = 0; i < M1.ncols; i++) {
         int idx1 = i + M1.base;
         for (int j = 0; j < M1.nrows; j++) {
            res[ridx + j] = M1.buf[idx1];
            idx1 += M1.width;
         }
         ridx += rwidth;
      }

      if (resizeLater) {
         setSize (M1.ncols, M1.nrows);
      }
      if (res != buf) {
         copyBackResult();
      }
   }

   /**
    * Returns true if the elements of this matrix equal those of matrix
    * <code>M1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param M1
    * matrix to compare with
    * @param epsilon
    * comparison tolerance
    * @return false if the matrices are not equal within the specified
    * tolerance, or have different sizes
    */
   public boolean epsilonEquals (MatrixNd M1, double epsilon) {
      if (M1.nrows != nrows || M1.ncols != ncols) {
         return false;
      }
      if (storageFilled & M1.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            // use ! abs(diff) <= epsilon to catch NaN
            if (!(Math.abs (buf[i] - M1.buf[i]) <= epsilon)) {
               return false;
            }
         }
      }
      else {
         int idx0 = base;
         int idx1 = M1.base;
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               // use ! abs(diff) <= epsilon to catch NaN
               if (!(Math.abs (buf[idx0 + j] - M1.buf[idx1 + j]) <= epsilon)) {
                  return false;
               }
            }
            idx0 += width;
            idx1 += M1.width;
         }
      }
      return true;
   }

   /**
    * Returns true if the elements of this matrix exactly equal those of matrix
    * <code>M1</code>.
    * 
    * @param M1
    * matrix to compare with
    * @return false if the matrices are not equal or have different sizes
    */
   public boolean equals (MatrixNd M1) {
      if (M1.nrows != nrows || M1.ncols != ncols) {
         return false;
      }
      if (storageFilled & M1.storageFilled) {
         int size = nrows * ncols;
         for (int i = 0; i < size; i++) {
            if (buf[i] != M1.buf[i]) {
               return false;
            }
         }
      }
      else {
         int idx0 = base;
         int idx1 = M1.base;
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               if (buf[idx0 + j] != M1.buf[idx1 + j]) {
                  return false;
               }
            }
            idx0 += width;
            idx1 += M1.width;
         }
      }
      return true;
   }

   /**
    * Returns the infinity norm of this matrix. This is equal to the maximum of
    * the vector 1-norm of each row.
    * 
    * @return infinity norm of this matrix
    */
   public double infinityNorm() {
      // returns the largest row sum of the absolute value
      double max = 0;
      int idx = base;
      for (int i = 0; i < nrows; i++) {
         double sum = 0;
         for (int j = 0; j < ncols; j++) {
            sum += Math.abs (buf[idx + j]);
         }
         idx += width;
         if (sum > max) {
            max = sum;
         }
      }
      return max;
   }

   /**
    * Returns the 1 norm of this matrix. This is equal to the maximum of the
    * vector 1-norm of each column.
    * 
    * @return 1 norm of this matrix
    */
   public double oneNorm() {
      // returns the largest column sum of the absolute value
      double max = 0;
      for (int j = 0; j < ncols; j++) {
         double sum = 0;
         int idx = j + base;
         for (int i = 0; i < nrows; i++) {
            sum += Math.abs (buf[idx]);
            idx += width;
         }
         if (sum > max) {
            max = sum;
         }
      }
      return max;
   }

   /**
    * Returns the Frobenius norm of this matrix. This is equal to the square
    * root of the sum of the squares of each element.
    * 
    * @return Frobenius norm of this matrix
    */
   public double frobeniusNorm() {
      // returns sqrt(sum (diag (M'*M))
      double sum = 0;
      for (int j = 0; j < ncols; j++) {
         double diagElem = 0;
         int idx = j + base;
         for (int i = 0; i < nrows; i++) {
            double m = buf[idx];
            diagElem += m * m;
            idx += width;
         }
         sum += diagElem;
      }
      return Math.sqrt (sum);
   }

   /**
    * Inverts this matrix in place, returning false if the matrix is detected to
    * be singular. The inverse is computed using LU decomposition with partial
    * pivoting.
    * 
    * @throws ImproperSizeException
    * if this matrix is not square
    */
   public boolean invert() throws ImproperSizeException {
      return invert (this);
   }

   /**
    * Inverts the matrix M1 and places the result in this matrix, returning
    * false if the matrix is detected to be singular. This matrix is resized if
    * necessary. The inverse is computed using LU decomposition with partial
    * pivoting.
    * 
    * @param M1
    * matrix to take the inverse of
    * @throws ImproperSizeException
    * if matrix M1 is not square, or if this matrix needs resizing but is of
    * fixed size
    */
   public boolean invert (MatrixNd M1) throws ImproperSizeException {
      if (M1.nrows != M1.ncols) {
         throw new ImproperSizeException ("matrix must be square");
      }
      LUDecomposition lu = new LUDecomposition (M1);
      boolean singular = false;
      try {
         singular = lu.inverse (this);
      }
      catch (ImproperStateException e) { // can't happen
      }
      return singular;
   }

//   private double dotArray (double[] v1, double[] v2, int n) {
//      double sum = 0;
//      for (int i = 0; i < n; i++) {
//         sum += v1[i] * v2[i];
//      }
//      return sum;
//   }
//
//   /**
//    * Overwrites v with its householder vector and returns beta
//    */
//   double houseVector (double[] v, int i0, int iend) {
//      double x0 = v[i0];
//      double sigma = 0;
//      for (int i=i0+1; i<iend; i++) {
//         sigma += v[i]*v[i];
//      }
//      v[i0] = 1;
//      if (sigma == 0) {
//         //return x0 >= 0 ? 0 : -2;
//         return 0;
//      }
//      else {
//         double mu = Math.sqrt(x0*x0 + sigma);
//         double v0;
//         if (x0 <= 0) {
//            v0 = x0-mu;
//         }
//         else {
//            v0 = -sigma/(x0+mu);
//         }
//         double beta = 2*v0*v0/(sigma+v0*v0);
//         for (int i=i0+1; i<iend; i++) {
//            v[i] /= v0;
//         }
//         return beta;
//      }
//   }

//   /**
//    * Premultiples this matrix by the Householder transform produced by the
//    * Householder vector v, whose length should be equal to the number of rows.
//    * w provides scratch space; it's length should equal or exceed the number of
//    * columns.
//    */
//   void rowHouseMul (double[] v, double[] w, double beta) {
//      //double beta = -2 / dotArray (v, v, nrows);
//
//      int idx;
//      for (int j = 0; j < ncols; j++) {
//         double sum = 0;
//         idx = j + base;
//         for (int i = 0; i < nrows; i++) {
//            sum += buf[i * width + j + base] * v[i];
//            idx += width;
//         }
//         w[j] = beta * sum;
//      }
//      idx = base;
//      for (int i = 0; i < nrows; i++) {
//         for (int j = 0; j < ncols; j++) {
//            buf[idx + j] -= w[j] * v[i];
//         }
//         idx += width;
//      }
//   }

//   /**
//    * Applies a Householder transform to zero out the trailing elements of the
//    * first column of this matrix. If "store" is true, the zeroed matrix entries
//    * are used to store the significant elements of the Householder vector.
//    */
//   double rowHouseReduce (double[] v, double[] w, boolean store) {
//      int idx = base;
//      for (int i = 0; i < nrows; i++) {
//         v[i] = buf[idx];
//         idx += width;
//      }
//      double beta = houseVector (v, 0, nrows);
//      rowHouseMul (v, w, beta);
//
//      if (store) { // place the Householder vector in the first column
//         idx = base + width;
//         for (int i = 1; i < nrows; i++) {
//            buf[idx] = v[i];
//            idx += width;
//         }
//      }
//      return beta;
//   }

//   /**
//    * Postmultiples this matrix by the Householder transform produced by the
//    * Householder vector v, whose length should be equals to the number of
//    * columns. w provides scratch space; it's length should equal or exceed the
//    * number of rows.
//    */
//   void colHouseMul (double[] v, double[] w, double beta) {
//      //double beta = -2 / dotArray (v, v, ncols);
//
//      int idx = base;
//      for (int i = 0; i < nrows; i++) {
//         double sum = 0;
//         for (int j = 0; j < ncols; j++) {
//            sum += buf[idx + j] * v[j];
//         }
//         idx += width;
//         w[i] = beta * sum;
//      }
//      idx = base;
//      for (int i = 0; i < nrows; i++) {
//         for (int j = 0; j < ncols; j++) {
//            buf[idx + j] -= w[i] * v[j];
//         }
//         idx += width;
//      }
//   }

//   /**
//    * Applies a Householder transform to zero out the trailing elements of the
//    * first row of this matrix. If "store" is true, the zeroed matrix entries
//    * are used to store the significant elements of the Householder vector.
//    */
//   double colHouseReduce (double[] v, double[] w, boolean store) {
//      for (int j = 0; j < ncols; j++) {
//         v[j] = buf[base + j];
//      }
//      double beta = houseVector (v, 0, ncols);
//      colHouseMul (v, w, beta);
//
//      if (store) { // store the Householder vector in the first row
//         for (int j = 1; j < ncols; j++) {
//            buf[base + j] = v[j];
//         }
//      }
//      return beta;
//   }

   /**
    * Rearrange the columns of this matrix according to the specified
    * permutation, such that each column j is replaced by column permutation[j].
    * 
    * @param permutation
    * describes the column exchanges
    * @throws ImproperSizeException
    * if the length of <code>permutation</code> is less than the column size
    * of this matrix.
    */
   public void permuteColumns (int[] permutation) {
      if (permutation.length < ncols) {
         throw new ImproperSizeException ("permutation argument too short");
      }
      // bit of a hack: use buf as temporay column storage,
      // and use buf[nrows+k] to mark exchanges that have
      // been made
      double[] tmp = allocScratchSpace (nrows + ncols);

      for (int j = 0; j < ncols; j++) {
         tmp[nrows + j] = 1;
      }
      for (int j = 0; j < ncols; j++) {
         int k = permutation[j];
         if (k < 0 || k >= ncols || tmp[nrows + k] == 0) {
            throw new IllegalArgumentException ("malformed permutation");
         }
         tmp[nrows + k] = 0;
      }
      for (int j = 0; j < ncols; j++) {
         if (tmp[nrows + j] == 0 && j != permutation[j]) {
            int idx = j + base; // tmp = M(:,j)
            for (int i = 0; i < nrows; i++) {
               tmp[i] = buf[idx];
               idx += width;
            }
            int tmp_j = j;
            int next_k = j;
            while (true) {
               int k = next_k;
               next_k = permutation[k];
               tmp[nrows + k] = 1; // mark element as done
               if (next_k == tmp_j) {
                  idx = k + base; // M(:,k) = tmp;
                  for (int i = 0; i < nrows; i++) {
                     buf[idx] = tmp[i];
                     idx += width;
                  }
                  break;
               }
               else {
                  idx = base; // M(:,k) = M(:,next_k)
                  for (int i = 0; i < nrows; i++) {
                     buf[idx + k] = buf[idx + next_k];
                     idx += width;
                  }
               }
            }
         }
      }
   }

   /**
    * Rearrange the rows of this matrix according to the specified permutation,
    * such that each row i is replaced by row permutation[i].
    * 
    * @param permutation
    * describes the row exchanges
    * @throws ImproperSizeException
    * if the length of <code>permutation</code> is less than the row size of
    * this matrix.
    */
   public void permuteRows (int[] permutation) {
      if (permutation.length < nrows) {
         throw new ImproperSizeException ("permutation argument too short");
      }
      // bit of a hack: use buf as temporay row storage,
      // and use buf[ncols+k] to mark exchanges that have
      // been made
      double[] tmp = allocScratchSpace (ncols + nrows);

      for (int i = 0; i < nrows; i++) {
         tmp[ncols + i] = 1;
      }
      for (int i = 0; i < nrows; i++) {
         int k = permutation[i];
         if (k < 0 || k >= nrows || tmp[ncols + k] == 0) {
            throw new IllegalArgumentException ("malformed permutation");
         }
         tmp[ncols + k] = 0;
      }
      for (int i = 0; i < nrows; i++) {
         if (tmp[ncols + i] == 0 && i != permutation[i]) {
            int idx = i * width + base; // tmp = M(i,:)
            for (int j = 0; j < ncols; j++) {
               tmp[j] = buf[idx + j];
            }
            int tmp_i = i;
            int next_k = i;
            while (true) {
               int k = next_k;
               next_k = permutation[k];
               tmp[ncols + k] = 1; // mark element as done
               if (next_k == tmp_i) {
                  idx = k * width + base; // M(k,:) = tmp;
                  for (int j = 0; j < ncols; j++) {
                     buf[idx + j] = tmp[j];
                  }
                  break;
               }
               else {
                  idx = k * width + base; // M(k,:) = M(next_k,:)
                  int idx2 = next_k * width + base;
                  for (int j = 0; j < ncols; j++) {
                     buf[idx + j] = buf[idx2 + j];
                  }
               }
            }
         }
      }
   }

   /**
    * Exchange two columns of this matrix.
    * 
    * @param col0
    * index of first column
    * @param col1
    * index of second column
    */
   protected void exchangeColumns (int col0, int col1) {
      if (col0 == col1) {
         return;
      }
      if (col0 >= ncols) {
         throw new IllegalArgumentException ("col0 out of range: " + col0);
      }
      if (col1 >= ncols) {
         throw new IllegalArgumentException ("col1 out of range: " + col1);
      }
      for (int i = 0; i < nrows; i++) {
         int baseIdx = i * width + base;
         double tmp = buf[baseIdx + col0];
         buf[baseIdx + col0] = buf[baseIdx + col1];
         buf[baseIdx + col1] = tmp;
      }
   }

   /**
    * Exchange two rows of this matrix.
    * 
    * @param row0
    * index of first row
    * @param row1
    * index of second row
    */
   protected void exchangeRows (int row0, int row1) {
      if (row0 == row1) {
         return;
      }
      if (row0 >= nrows) {
         throw new IllegalArgumentException ("row0 out of range: " + row0);
      }
      if (row1 >= nrows) {
         throw new IllegalArgumentException ("row1 out of range: " + row1);
      }
      for (int j = 0; j < ncols; j++) {
         int baseIdx = base + j;
         double tmp = buf[row0 * width + baseIdx];
         buf[row0 * width + baseIdx] = buf[row1 * width + baseIdx];
         buf[row1 * width + baseIdx] = tmp;
      }
   }

   @Override
   protected void mulVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int idx = base + r0*width + c0;
      for (int i=0; i<nr; i++) {
         double sum = 0;
         for (int j=0; j<nc; j++) {
            sum += buf[idx+j]*vec[j];
         }
         idx += width;
         res[i] = sum;
      }
   }

   @Override
   protected void mulAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int idx = base + r0*width + c0;
      for (int i=0; i<nr; i++) {
         double sum = 0;
         for (int j=0; j<nc; j++) {
            sum += buf[idx+j]*vec[j];
         }
         idx += width;
         res[i] += sum;
      }
   }

   @Override
   protected void mulTransposeVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc){

      for (int j=0; j<nr; j++) {
         double sum = 0;
         int idx = base + c0*width + r0 + j;
         for (int i=0; i<nc; i++) {
            sum += vec[i]*buf[idx];
            idx += width;
         }
         res[j] = sum;
      }
   }

   @Override
   protected void mulTransposeAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc){

      for (int j=0; j<nr; j++) {
         double sum = 0;
         int idx = base + c0*width + r0 + j;
         for (int i=0; i<nc; i++) {
            sum += vec[i]*buf[idx];
            idx += width;
         }
         res[j] += sum;
      }
   }

   public void mulAdd (Matrix M1, Matrix M2) {
      if (M1.rowSize() != rowSize() ||
          M2.colSize() != colSize() ||
          M1.colSize() != M2.rowSize()) {
         throw new ImproperSizeException (
            "matrix sizes "+M1.getSize()+" and "+M2.getSize()+
            " do not conform to "+getSize());
      }
      MatrixNd R = this;
      if (M1 == this || M2 == this) {
         R = new MatrixNd (this);
      }
      for (int i=0; i<rowSize(); i++) {
         for (int j=0; j<colSize(); j++) {
            double sum = 0;
            for (int k=0; k<M1.colSize(); k++) {
               sum += M1.get(i,k)*M2.get(k,j);
            }
            R.add (i, j, sum);
         }
      }
      if (R != this) {
         set (R);
      }
   }

   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      if (M1.rowSize() != rowSize() ||
          M2.rowSize() != colSize() ||
          M1.colSize() != M2.colSize()) {
         throw new ImproperSizeException (
            "matrix sizes "+M1.getSize()+" and "+M2.getSize()+
            " do not conform to "+getSize());
      }
      MatrixNd R = this;
      if (M1 == this || M2 == this) {
         R = new MatrixNd (this);
      }
      for (int i=0; i<rowSize(); i++) {
         for (int j=0; j<colSize(); j++) {
            double sum = 0;
            for (int k=0; k<M1.colSize(); k++) {
               sum += M1.get(i,k)*M2.get(j,k);
            }
            R.add (i, j, sum);
         }
      }
      if (R != this) {
         set (R);
      }
   }

   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      if (M1.colSize() != rowSize() ||
          M2.colSize() != colSize() ||
          M1.rowSize() != M2.rowSize()) {
         throw new ImproperSizeException (
            "matrix sizes "+M1.getSize()+" and "+M2.getSize()+
            " do not conform to "+getSize());
      }
      MatrixNd R = this;
      if (M1 == this || M2 == this) {
         R = new MatrixNd (this);
      }
      for (int i=0; i<rowSize(); i++) {
         for (int j=0; j<colSize(); j++) {
            double sum = 0;
            for (int k=0; k<M1.rowSize(); k++) {
               sum += M1.get(k,i)*M2.get(k,j);
            }
            R.add (i, j, sum);
         }
      }
      if (R != this) {
         set (R);
      }
   }

   public static void main (String[] args) {
      MatrixNd M3x4 = new MatrixNd (3, 4);
      MatrixNd M4x3 = new MatrixNd (4, 3);

      double[] svals = new double[] { 10, 3, 0.1 };
      RandomGenerator.setSeed (0x1234);
      M4x3.setRandomSvd (svals);
      RandomGenerator.setSeed (0x1234);
      M3x4.setRandomSvd (svals);

      System.out.println ("M4x3=\n" + M4x3.toString ("%8.4f"));
      System.out.println ("M3x4=\n" + M3x4.toString ("%8.4f"));
   }

   public MatrixNd clone() throws CloneNotSupportedException {
      MatrixNd M = null;
      try {
         M = (MatrixNd)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for " + getClass());
      }
      M.tmp = new double[0];
      M.subMatrixRefCnt = 0;
      M.fixedSize = false;
      M.explicitBuffer = false;
      M.buf = new double[0];
      // nrows, ncols, and width will be reset by the set routine
      M.nrows = 0;
      M.ncols = 0;
      M.width = 0;
      M.base = 0;
      M.set (this);
      M.res = null;
      return M;
   }

   public void checkConsistency () {
      // not much to do - just make sure we have enough space
      if (width < ncols) {
         throw new IllegalStateException (
            "width "+width+" less than ncols "+ncols);
      }
      if ((nrows-1)*width+(ncols-1)+base > buf.length) {
         throw new IllegalStateException (
            "buffer size not large enough for matrix");
      }
   }

   /* VectorObject implementation. It is currently necessary to define the
    * scale and add methods as scaleObj(), addObj(), and scaledAddObj(), since
    * the corresponding scale(), add() and scaledAdd() methods have
    * incompatible return types across different classes (some return a
    * reference to their object, while others return {@code void}).
    */

   /**
    * {@inheritDoc}
    */
   public void scaleObj (double s) {
      scale (s, this);
   }

   /**
    * {@inheritDoc}
    */
   public void addObj (MatrixNd M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, MatrixNd M1) {
      scaledAdd (s, M1);
   }
}
