/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * General interface for matrices. It specifies methods which allow one to set
 * and get various matrix components (e.g., individual elements, rows, columns,
 * submatrics), and do various non-modifying queries such as finding out its
 * size, computing it's determinant, or comparing it with other matrices. There
 * is also a method {@link #setSize setSize} for resizing, which can be used
 * unless the matrix size is fixed (which can be determined using {@link
 * #isFixedSize isFixedSize}).
 * 
 * <p>
 * This interface does not support operations such as multiplication or
 * addition. The reason for this is that specific implementations may have a
 * specialized structure which could be compromised by arbitrary operations. For
 * instance, if an implementing class represents orthogonal matrices, then it
 * does not make sense for that class to allow an <code>add</code> operation.
 * Similarly, for an implementation representing symmetric matrices, any
 * muliplication methods should not be open to general matrices.
 * 
 * <p>
 * Of course, it is possible to corrupt any special implementation structure
 * using the <code>set</code> methods provided in this base class, but it was
 * felt that not including such routines would be overly restrictive. It is
 * therefore up to the user to safeguard implementation integrity against misuse
 * of the <code>set</code> methods.
 * 
 * <p>
 * Note that indices for matrix elements, rows, and columns are zero-based. The
 * range of valid indices for a matrix of size m X n is
 * {@code [0, ... , m-1]} and {@code [0, ... , n-1]}.
 */
public interface Matrix extends LinearTransformNd {
   /**
    * Identifies a matrix as regular indefinite.
    */
   public static final int INDEFINITE = 0x0;

   /**
    * Identifies a matrix as symmetric.
    */
   public static final int SYMMETRIC = 0x1;

   /**
    * Identifies a matrix as positive definite.
    */
   public static final int POSITIVE_DEFINITE = 0x2;

   /**
    * Identifies a matrix as symmetric positive definite.
    */
   public static final int SPD = (SYMMETRIC | POSITIVE_DEFINITE);

   /**
    * Describes different partitions of a matrix object.
    */
   public enum Partition {
      /**
       * No partition specified.
       */
      None,

      /**
       * Specfies the entire matrix.
       */
      Full,

      /**
       * Specifices the upper triangular portion of this matrix.
       */
      UpperTriangular,

      /**
       * Specifices the lower triangular portion of this matrix.
       */
      LowerTriangular
   }

   /**
    * Describes the general format for writing matrix values.
    */
   public enum WriteFormat {
      /**
       * Usual dense matrix format, as in
       * 
       * <pre>
       * 1.00  0.00  0.50
       * 2.30  4.10  0.00
       * 0.00  2.00  3.00
       * </pre>
       */
      Dense,

      /**
       * Sparse format consisting of the non-zero entries, in row-major order,
       * each written as a 3-tuples containing the i and j indices (0-based) and
       * the associated value. The example shown for
       * {@link WriteFormat#Dense Dense} would be output as
       * 
       * <pre>
       * ( 0 0 1 )
       * ( 0 2 0.50 )
       * ( 1 0 2.30 )
       * ( 1 1 4.10 )
       * ( 2 1 2.00 )
       * ( 2 2 3.00 )
       * </pre>
       */
      Sparse,

      /**
       * <a href="http://math.nist.gov/MatrixMarket/formats.html">MatrixMarket</a>
       * format. The example shown for {@link WriteFormat#Dense Dense} would be
       * output as
       * 
       * <pre>
       * %%MatrixMarket matrix coordinate real general
       * 3 3 6
       * 1 1 1
       * 1 3 0.50
       * 2 1 2.30
       * 2 2 4.10
       * 3 2 2.00
       * 3 3 3.00
       * </pre>
       */
      MatrixMarket,

      /**
       * Compressed row storage (CRS) format. Row one gives the number of rows.
       * Row two gives the starting offsets (one-based) for the
       * first non-zero element in each row, followed by total number of
       * non-zero values plus one. Rows three and four give, respectively, the
       * column index (one-based) and value for each non-zero value, in
       * row-major order. The example shown for {@link WriteFormat#Dense Dense}
       * would be output as
       * 
       * <pre>
       * 3
       * 1 3 5 7
       * 1 3 1 2 2 3
       * 1.00 0.50 2.30 4.10 2.00 3.00
       * </pre>
       */
      CRS,

      /**
       * Same as CRS, except that the matrix is assumed to be symmetric
       * and only the upper triangular values are stored.
       */
      SYMMETRIC_CRS, 

   };

   /**
    * Returns the number of rows in this matrix.
    * 
    * @return number of rows
    */
   public int rowSize();

   /**
    * Returns the number of columns in this matrix.
    * 
    * @return number of columns
    */
   public int colSize();

   /**
    * Gets a single element of this matrix.
    * 
    * @param i
    * element row index
    * @param j
    * element column index
    * @return element value
    */
   public double get (int i, int j);

   /**
    * Copies the elements of this matrix into an array of doubles. The elements
    * are stored using row-major order, so that element <code>(i,j)</code> is
    * stored at location <code>i*colSize()+j</code>.
    * 
    * @param values
    * array into which values are copied
    */
   public void get (double[] values);

   /**
    * Copies a column of this matrix into an array of doubles.
    * 
    * @param j
    * column index
    * @param values
    * array into which the column is copied
    */
   public void getColumn (int j, double[] values);

   /**
    * Copies a column of this matrix into an array of doubles, starting at a
    * specified offset.
    * 
    * @param j
    * column index
    * @param values
    * array into which the column is copied
    * @param off
    * offset in values where copying should begin
    */
   public void getColumn (int j, double[] values, int off);

   /**
    * Copies a row of this matrix into an array of doubles.
    * 
    * @param i
    * row index
    * @param values
    * array into which the row is copied
    */
   public void getRow (int i, double[] values);

   /**
    * Copies a row of this matrix into an array of doubles, starting at a
    * specified offset.
    * 
    * @param i
    * row index
    * @param values
    * array into which the row is copied
    * @param off
    * offset in values where copying should begin
    */
   public void getRow (int i, double[] values, int off);

   /**
    * Copies a column of this matrix into a {@link maspack.matrix.Vector
    * Vector}.
    * 
    * @param j
    * column index
    * @param v
    * vector into which the column is copied
    * @throws ImproperSizeException
    * vector's size not equal to the number of matrix rows and the vector cannot
    * be resized
    */
   public void getColumn (int j, Vector v);

   /**
    * Copies a row of this matrix into a {@link maspack.matrix.Vector Vector}.
    * 
    * @param i
    * row index
    * @param v
    * vector into which the row is copied
    * @throws ImproperSizeException
    * vector's size not equal to the number of matrix columns and the vector
    * cannot be resized
    */
   public void getRow (int i, Vector v);

   /**
    * Sets the size and values of this matrix to those of another matrix.
    * 
    * @param M
    * matrix whose size and values are copied
    * @throws ImproperSizeException
    * matrices have different sizes and this matrix cannot be resized
    * accordingly
    */
   public void set (Matrix M);

   /**
    * Returns true if this matrix is of fixed size. If this matrix is not of
    * fixed size, then it can be resized dynamically, either explicitly using
    * {@link #setSize setSize}, or implicitly when used as a result for various
    * matrix operations.
    * 
    * @return true if this matrix is of fixed size
    * @see Matrix#setSize
    */
   public boolean isFixedSize();

   /**
    * Sets the size of this matrix. This operation is only supported if
    * {@link #isFixedSize isFixedSize} returns false.
    * 
    * @param numRows
    * new row size
    * @param numCols
    * new column size
    * @throws UnsupportedOperationException
    * if this operation is not supported
    * @see Matrix#isFixedSize
    */
   public void setSize (int numRows, int numCols);

   /**
    * Returns the determinant of this matrix, which must be square
    * 
    * @return matrix determinant
    * @throws ImproperSizeException
    * if the matrix is not square
    */
   public double determinant();

   /**
    * Returns the trace of this matrix, which must be square
    * 
    * @return matrix trace
    * @throws ImproperSizeException
    * if the matrix is not square
    */   
   public double trace();
   
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
   public boolean epsilonEquals (Matrix M1, double epsilon);

   /**
    * Returns true if the elements of this matrix exactly equal those of matrix
    * <code>M1</code>.
    * 
    * @param M1
    * matrix to compare with
    * @return false if the matrices are not equal or have different sizes
    */
   public boolean equals (Matrix M1);

   /**
    * Returns the 1 norm of this matrix. This is equal to the maximum of the
    * vector 1-norm of each column.
    * 
    * @return 1 norm of this matrix
    */
   public double oneNorm();

   /**
    * Returns the infinity norm of this matrix. This is equal to the maximum of
    * the vector 1-norm of each row.
    * 
    * @return infinity norm of this matrix
    */
   public double infinityNorm();

   /**
    * Returns the Frobenius norm of this matrix. This is equal to the square
    * root of the sum of the squares of each element.
    * 
    * @return Frobenius norm of this matrix
    */
   public double frobeniusNorm();
   
   /**
    * Returns the squared Frobenius norm of this matrix. This is equal to the 
    * sum of the squares of each element.
    * 
    * @return Frobenius norm squared of this matrix
    */
   public double frobeniusNormSquared();
   
   /**
    * Returns the max-norm of this matrix.  This is equal to the maximum of the
    * absolute values of all entries (i.e. element-wise, with p=q=inf)
    * 
    * @return max norm of this matrix
    */
   public double maxNorm();

   /**
    * Writes the contents of this matrix to a PrintWriter. Element values are
    * written in a dense-format row-major order, one row per line, separated by
    * spaces. Each value is formatted using a C <code>printf</code> style as
    * described by the parameter <code>NumberFormat</code>.
    * 
    * @param pw
    * PrintWriter to write this matrix to
    * @param fmt
    * numeric format
    */
   public void write (PrintWriter pw, NumberFormat fmt) throws IOException;

   /**
    * Writes the contents of this matrix to a PrintWriter. The overall write
    * format is specified by <code>wfmt</code>, while the values themselves
    * are formatted using a C <code>printf</code> style as decribed by the
    * parameter <code>NumberFormat</code>.
    * 
    * @param pw
    * PrintWriter to write this matrix to
    * @param fmt
    * numeric format
    * @param wfmt
    * specifies the general output format
    */
   public void write (PrintWriter pw, NumberFormat fmt, WriteFormat wfmt)
      throws IOException;

   /**
    * Writes the contents of a principal sub-matrix of this matrix delimited by
    * the first <code>numRows</code> rows and the first <code>numCols</code>
    * columns. The overall write format is specified by <code>wfmt</code>,
    * while the values themselves are formatted using a C <code>printf</code>
    * style as decribed by the parameter <code>NumberFormat</code>.
    * 
    * @param pw
    * PrintWriter to write this matrix to
    * @param fmt
    * numeric format
    * @param wfmt
    * specifies the general output format
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    */
   public void write (
      PrintWriter pw, NumberFormat fmt, WriteFormat wfmt, int numRows,
      int numCols) throws IOException;

   /**
    * Sets the contents of this matrix to values read from a ReaderTokenizer.
    * Matrix elements may be specified in either a dense or sparse format.
    * 
    * <p>
    * For the dense format, the input should consist of a sequence of numbers,
    * arranged in row-major order, separated by white space, and optionally
    * surrounded by square brackets <code>[ ]</code>.
    * 
    * <p>
    * If the input is not surrounded by square brackets, then the number of
    * values should equal the current number of elements in this matrix, as
    * defined by the product of {@link #rowSize rowSize} and
    * {@link #colSize colSize}.
    * 
    * <p>
    * If the input is surrounded by square brackets, then the matrix dimensions
    * are determined by the input itself: rows should be separated by either
    * semicolons or a newline, the number of values in each row should be the
    * same, and rows are read until a closing square bracket is detected. The
    * resulting input should either match this matrix's dimensions, or this
    * matrix should be resizable to accomodate those dimensions. For example,
    * 
    * <pre>
    * [ 1.2  4   5
    *   6    3.1 0 ]
    * </pre>
    * 
    * defines a 2 x 3 matrix.
    * 
    * <p>
    * For the sparse format, the input should be surrounded by square brackets,
    * and each element value is specified as a triple <code>( i j value )</code>,
    * enclosed in parentheses. No particular ordering is required for the i or j
    * indices, and unspecified elements are set to zero. The entire set of
    * elements should be surrounded by square brackets. For example,
    * 
    * <pre>
    * [ ( 0 0 1.2 )
    *   ( 0 1 4 )
    *   ( 0 2 5 )
    *   ( 1 0 6 )
    *   ( 1 1 3.1) ]
    * </pre>
    * 
    * defines that same 2 x 3 matrix given in the previous example.
    * 
    * @param rtok
    * Tokenizer from which matrix values are read. Number parsing should be
    * enabled
    * @throws ImproperSizeException
    * if this matrix has a fixed size which is incompatible with the input, or
    * if the sizes of the specified rows are inconsistent
    */
   public void scan (ReaderTokenizer rtok) throws IOException;

   /**
    * Multiplies this matrix by the column vector v1 and places the result in
    * the vector vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = M v1
    * </pre>
    * 
    *
    * The vector vr is resized if it is not sufficiently large.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @throws ImproperSizeException
    * if the size of v1 does not equal the number of columns of this matrix.
    */
   public void mul (VectorNd vr, VectorNd v1);

   /**
    * Multiplies a submatrix of this matrix by the column vector <code>v1
    * </code>and places the result in the vector <code>vr</code>.  The
    * submatrix is defined by the first <code>nr</code> rows and
    * <code>nc</code> columns of this matrix. If M represents this matrix, this
    * is equivalent to computing
    * 
    * <pre>
    *  vr = M[0:nr-1][0:nc-1] v1
    * </pre>
    * 
    * The vector vr is resized if it is not sufficiently large.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param nr number of initial rows of this matrix to use
    * @param nc number of initial columns of this matrix to use
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>, or if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mul (VectorNd vr, VectorNd v1, int nr, int nc);

   /**
    * Multiplies a submatrix of this matrix by the column vector <code>v1
    * </code>and places the result in the vector <code>vr</code>.  The submatrix
    * is defined by the <code>nr</code> rows and <code>nc</code> columns of
    * this matrix starting at <code>r0</code> and <code>c0</code>,
    * respectively. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = M[r0:r0+nr-1][c0:c0+nc-1] v1
    * </pre>
    * 
    * The vector vr is resized if it is not sufficiently large.
    *
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param r0 initial row of the submatrix
    * @param nr number of rows in the submatrix
    * @param c0 initial column of the submatrix
    * @param nc number of columns in the submatrix
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>, or
    * if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mul (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc);


   /**
    * Multiplies this matrix by the column vector v1 and adds the result to
    * the vector vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr += M v1
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @throws ImproperSizeException
    * if the size of v1 does not equal the number of columns of this matrix,
    * or if the size of vr does not equal the number of rows.
    */
   public void mulAdd (VectorNd vr, VectorNd v1);

   /**
    * Multiplies a submatrix of this matrix by the column vector <code>v1
    * </code>and adds the result to the vector <code>vr</code>.  The submatrix
    * is defined by the first <code>nr</code> rows and <code>nc</code> columns
    * of this matrix. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr += M[0:nr-1][0:nc-1] v1
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param nr number of initial rows of this matrix to use
    * @param nc number of initial columns of this matrix to use
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>,
    * if the size of vr is less than <code>nr</code>, or
    * if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mulAdd (VectorNd vr, VectorNd v1, int nr, int nc);

   /**
    * Multiplies a submatrix of this matrix by the column vector <code>v1
    * </code>and adds the result to the vector <code>vr</code>.  The submatrix
    * is defined by the <code>nr</code> rows and <code>nc</code> columns of
    * this matrix starting at <code>r0</code> and <code>c0</code>,
    * respectively. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr += M[r0:r0+nr-1][c0:c0+nc-1] v1
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param r0 initial row of the submatrix
    * @param nr number of rows in the submatrix
    * @param c0 initial column of the submatrix
    * @param nc number of columns in the submatrix
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>,
    * if the size of vr is less than <code>nr</code>, or
    * if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mulAdd (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc);

   /**
    * Multiplies the transpose of this matrix by the vector v1 and places the
    * result in vr. If M represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr = v1 M
    * </pre>
    * 
    * The vector vr is resized if it is not sufficiently large.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @throws ImproperSizeException
    * if the size of v1 does not equal the number of rows of this matrix
    */
   public void mulTranspose (VectorNd vr, VectorNd v1);

   /**
    * Multiplies a submatrix of the transpose of this matrix by the column
    * vector <code>v1 </code>and places the result in the vector
    * <code>vr</code>.  The submatrix is defined by the first <code>nr</code>
    * rows and <code>nc</code> columns of the transpose of this matrix. If M
    * represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr = v1 M[0:nc-1][0:nr-1]
    * </pre>
    * 
    * The vector vr is resized if it is not sufficiently large.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param nr number of initial rows of the transpose of this matrix to use
    * @param nc number of initial columns of the transpose of this matrix to use
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>,
    * or if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mulTranspose (VectorNd vr, VectorNd v1, int nr, int nc);


   /**
    * Multiplies a submatrix of the transpose of this matrix by the column
    * vector <code>v1 </code>and places the result in the vector
    * <code>vr</code>.  The submatrix is defined by the <code>nr</code> rows
    * and <code>nc</code> columns of the transpose of this matrix starting at
    * <code>r0</code> and <code>c0</code>, respectively. If M represents this
    * matrix, this is equivalent to
    * 
    * <pre>
    *  vr = v1 M[c0:c0+nc-1][r0:r0+nr-1]
    * </pre>
    * 
    * The vector vr is resized if it is not sufficiently large.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param r0 initial row of the submatrix
    * @param nr number of rows in the submatrix
    * @param c0 initial column of the submatrix
    * @param nc number of columns in the submatrix
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>,
    * or if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mulTranspose (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc);

   /**
    * Multiplies the transpose of this matrix by the vector v1 and adds the
    * result to vr. If M represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr += v1 M
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @throws ImproperSizeException
    * if the size of v1 does not equal the number of rows of this matrix
    */
   public void mulTransposeAdd (VectorNd vr, VectorNd v1);

   /**
    * Multiplies a submatrix of the transpose of this matrix by the column
    * vector <code>v1 </code>and adds the result to the vector
    * <code>vr</code>.  The submatrix is defined by the first <code>nr</code>
    * rows and <code>nc</code> columns of the transpose of this matrix. If M
    * represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr += v1 M[0:nc-1][0:nr-1]
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param nr number of initial rows of the transpose of this matrix to use
    * @param nc number of initial columns of the transpose of this matrix to use
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>,
    * or if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mulTransposeAdd (VectorNd vr, VectorNd v1, int nr, int nc);


   /**
    * Multiplies a submatrix of the transpose of this matrix by the column
    * vector <code>v1 </code>and adds the result to the vector <code>vr</code>.
    * The submatrix is defined by the <code>nr</code> rows and <code>nc</code>
    * columns of the transpose of this matrix starting at <code>r0</code> and
    * <code>c0</code>, respectively. If M represents this matrix, this is
    * equivalent to
    * 
    * <pre>
    *  vr += v1 M[c0:c0+nc-1][r0:r0+nr-1]
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param r0 initial row of the submatrix
    * @param nr number of rows in the submatrix
    * @param c0 initial column of the submatrix
    * @param nc number of columns in the submatrix
    * @throws ImproperSizeException
    * if the size of v1 is less than <code>nc</code>,
    * or if <code>nr</code>
    * or <code>nc</code> exceed the dimensions of this matrix.
    */
   public void mulTransposeAdd (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc);

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
    * @param numValues
    * number of explicit values
    */
   public void set (double[] values, int[] indices, int numValues);

   /**
    * Gets the compressed row storage (CRS) indices for this matrix.  This is a
    * convenience wrapper for {@link #getCRSIndices(int[],int[],Partition)
    * getCRSIndices(int[],int[],Partition)} with the Partition set to
    * <code>Full</code>. For a detailed detailed decsription of the CRS format,
    * see {@link #setCRSValues setCRSValues()}.
    * 
    * @param colIdxs returns the column indices of each non-zero element,
    * in row-major order. This array must have a length equal
    * at least to the number of non-zero elements.
    * @param rowOffs returns the row start offsets into
    * <code>colIdxs</code>, followed by nvals+1, where nvals is the number of
    * non-zero elements. This array must have a length equal at least to
    * nrows+1, where nrows is the number of rows in this matrix.
    * @return number of non-zero elements
    */
   public int getCRSIndices (int[] colIdxs, int[] rowOffs);

   /**
    * Gets the compressed row storage (CRS) indices for this matrix. Indices
    * are 1-based and the matrix is traversed in row-major order. For a
    * detailed decsription of the CRS format, see {@link #setCRSValues
    * setCRSValues()}.
    * 
    * @param colIdxs returns the column indices of each non-zero element,
    * in row-major order. This array must have a length equal
    * at least to the number of non-zero elements.
    * @param rowOffs returns the row start offsets into
    * <code>colIdxs</code>, followed by nvals+1, where nvals is the number of
    * non-zero elements. This array must have a length equal at least to
    * nrows+1, where nrows is the number of rows in this matrix.
    * @param part
    * specifies what portion of the matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#UpperTriangular UpperTriangular}
    * @return number of non-zero elements
    */
   public int getCRSIndices (int[] colIdxs, int[] rowOffs, Partition part);

   /**
    * Gets the compressed row storage (CRS) indices for a principal sub-matrix
    * of this matrix delimited by the first <code>numRows</code> rows and the
    * first <code>numCols</code> columns. Indices are 1-based and supplied in
    * row-major order. For a detailed decsription of the CRS format, see {@link
    * #setCRSValues setCRSValues()}. Some matrix types may place restrictions
    * on the sub-matrix; for instance, a block-structured matrix may require
    * that the sub-matrix be block aligned.
    * 
    * @param colIdxs returns the column indices of each non-zero element, 
    * in row-major order. This array must have a length equal
    * at least to the number of non-zero elements.
    * @param rowOffs returns the row start offsets into <code>colIdxs</code>,
    * followed by nvals+1, where nvals is the number of non-zero elements in
    * the sub-matrix. This array must have a length equal at least to nrows+1,
    * where nrows is the number of rows in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#UpperTriangular UpperTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCRSIndices (
      int[] colIdxs, int[] rowOffs, Partition part, int numRows, int numCols);

   /**
    * Gets the compressed row storage (CRS) values for this matrix, in
    * row-major order. This is a convenience wrapper for {@link
    * #getCRSValues(double[],Partition) getCRSValues(double[],Partition)} with
    * the Partition set to <code>Full</code>. For a detailed decsription of the
    * CRS format, see {@link #setCRSValues setCRSValues()}.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements.
    * @return number of non-zero elements
    */
   public int getCRSValues (double[] vals);

   /**
    * Gets the compressed row storage (CRS) values for this matrix, in
    * row-major order. For a detailed decsription of the CRS format, see {@link
    * #setCRSValues setCRSValues()}.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements.
    * @param part
    * specifies what portion of the matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#UpperTriangular UpperTriangular}
    * @return number of non-zero elements
    */
   public int getCRSValues (double[] vals, Partition part);

   /**
    * Gets the compressed row storage (CRS) values for a principal sub-matrix
    * of this matrix delimited by the first <code>numRows</code> rows and the
    * first <code>numCols</code> columns. Values are supplied in row-major
    * order. For a detailed decsription of the CRS format, see {@link
    * #setCRSValues setCRSValues()}. Some matrix types may place restrictions
    * on the sub-matrix; for instance, a block-structured matrix may require
    * that the sub-matrix be block aligned.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#UpperTriangular UpperTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCRSValues (
      double[] vals, Partition part, int numRows, int numCols);


   /**
    * Sets the contents of this matrix given a set of values in compressed row
    * storage (CRS). The argument <code>part</code> specifies what portion of
    * the matrix is supplied. If it is {@link Partition#Full Full}, then the
    * entire matrix is supplied. If it is {@link Partition#UpperTriangular},
    * then only the upper triangular part is assumed to be supplied, and the
    * lower triangular part is set from its transpose (matrix bounds
    * permitting).  For Partition.UpperTriangular, supplied entries which are
    * not upper triangular will either be ignored or generate an exception.
    *
    * <p>All specified data must fit within the current matrix
    * bounds; the matrix is not resized.
    *
    * <p>
    * CRS data takes the form of three arrays:
    *
    * <dl>
    * <dt>vals</dt>
    * <dd>An array giving the values of all the non-zero elements in
    * the sparse matrix, in row-major order.</dd>
    * <dt>colIdxs</dt>
    * <dd>An array giving the column indices of all the
    * non-zero elements in the sparse matrix, in row-major order. The indices
    * are 1-based, so that 1 denotes the first column.</dd>
    * <dt>rowOffs</dt>
    * <dd>An array of size nrows+1, where nrows is the number
    * of matrix rows, giving the offsets into <code>vals</code> and
    * <code>colIdxs</code> corresponding to the first non-zero element in each
    * row. All values are 1-based, so that first offset value is
    * 1, the second offset value is n+1, where n is the number of non-zero
    * elements in the first row, etc. The final value is set to nvals+1, where
    * nvals is the number of non-zero elements in the matrix.</dd>
    * </dl>
    *
    * CRS index data is 1-based because this is the most standard form
    * used by solvers and matrix formats.
    * 
    * @param vals
    * non-zero element values. This array must have a length equal at
    * least to <code>nvals</code>.
    * @param colIdxs
    * column indices for each non-zero element.
    * This array must have a length equal at
    * least to <code>nvals</code>.
    * @param rowOffs row start offsets into <code>vals</code> and
    * <code>colIdxs</code>. This array must have a length equal at least to
    * nrows+1, where nrows is the number of rows in this matrix.
    * @param nvals
    * number of non-zero values
    * @param nrows
    * number of specified rows. Indicates the maximum value of
    * <code>rowOffs</code> that will be used; will not resize matrix.
    * @param part
    * must be either {@link Partition#Full Full} or
    * {@link Partition#UpperTriangular UpperTriangular}.
    */
   public void setCRSValues (
      double[] vals, int[] colIdxs, int[] rowOffs, int nvals, int nrows,
      Partition part);

   /**
    * Gets the compressed column storage (CCS) indices for this matrix. This is
    * a convenience wrapper for {@link #getCCSIndices(int[],int[],Partition)
    * getCCSIndices(int[],int[],Partition)} with the Partition set to
    * <code>Full</code>.  For a detailed decsription of the CCS format, see
    * {@link #setCCSValues setCCSValues()}.
    * 
    * @param rowIdxs returns the row indices of each non-zero element,
    * in column-major order. This array must have a length equal
    * at least to the number of non-zero elements.
    * @param colOffs returns the column start offsets into
    * <code>rowIdxs</code>, followed by nvals+1, where nvals is the number of
    * non-zero elements. This array must have a length equal at least to
    * ncols+1, where ncols is the number of columns in this matrix.
    * @return number of non-zero elements
    */
   public int getCCSIndices (int[] rowIdxs, int[] colOffs);

   /**
    * Gets the compressed column storage (CCS) indices for this matrix. Indices
    * are 1-based and the matrix is traversed in column-major order. For a
    * detailed decsription of the CCS format, see {@link #setCCSValues
    * setCCSValues()}.
    * 
    * @param rowIdxs returns the row indices of each non-zero element,
    * in column-major order. This array must have a length equal
    * at least to the number of non-zero elements.
    * @param colOffs returns the column start offsets into
    * <code>rowIdxs</code>, followed by nvals+1, where nvals is the number of
    * non-zero elements. This array must have a length equal at least to
    * ncols+1, where ncols is the number of columns in this matrix.
    * @param part
    * specifies what portion of the matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#LowerTriangular LowerTriangular}
    * @return number of non-zero elements
    */
   public int getCCSIndices (int[] rowIdxs, int[] colOffs, Partition part);

   /**
    * Gets the compressed column storage (CCS) indices for a principal
    * sub-matrix of this matrix delimited by the first <code>numRows</code>
    * rows and the first <code>numCols</code> columns. Indices are 1-based and
    * supplied in column-major order. For a detailed decsription of the CCS
    * format, see {@link #setCCSValues setCCSValues()}. Some matrix types may
    * place restrictions on the sub-matrix; for instance, a block-structured
    * matrix may require that the sub-matrix be block aligned. 
    * 
    * @param rowIdxs returns the row indices of each non-zero element, 
    * in column-major order. This array must have a length equal
    * at least to the number of non-zero elements.
    * @param colOffs returns the column start offsets into <code>rowIdxs</code>,
    * followed by nvals+1, where nvals is the number of non-zero elements in
    * the sub-matrix. This array must have a length equal at least to ncol+1,
    * where ncols is the number of columns in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#LowerTriangular LowerTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCCSIndices (
      int[] rowIdxs, int[] colOffs, Partition part, int numRows, int numCols);

   /**
    * Gets the compressed column storage (CCS) values for this matrix, in
    * column-major order. This is a convenience wrapper for {@link
    * #getCCSValues(double[],Partition) getCCSValues(double[],Partition)} with
    * the Partition set to <code>Full</code>. For a detailed decsription of the
    * CCS format, see {@link #setCCSValues setCCSValues()}.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements.
    * @return number of non-zero elements
    */
   public int getCCSValues (double[] vals);

   /**
    * Gets the compressed column storage (CCS) values for this matrix, in
    * column-major order. For a detailed decsription of the CCS format, see
    * {@link #setCCSValues setCCSValues()}.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements.
    * @param part
    * specifies what portion of the matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#LowerTriangular LowerTriangular}
    * @return number of non-zero elements
    */
   public int getCCSValues (double[] vals, Partition part);

   /**
    * Gets the compressed column storage (CCS) values for a principal
    * sub-matrix of this matrix delimited by the first <code>numRows</code>
    * rows and the first <code>numCols</code> columns. Values are supplied in
    * column-major order. For a detailed decsription of the CCS format, see
    * {@link #setCCSValues setCCSValues()}. Some matrix types may place
    * restrictions on the sub-matrix; for instance, a block-structured matrix
    * may require that the sub-matrix be block aligned.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link Partition#Full Full} or
    * {@link Partition#LowerTriangular LowerTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCCSValues (
      double[] vals, Partition part, int numRows, int numCols);

   /**
    * Sets the contents of this matrix given a set of values in compressed
    * column storage (CCS). The argument <code>part</code> specifies what
    * portion of the matrix is supplied. If it is {@link Partition#Full Full},
    * then the entire matrix is supplied. If it is {@link
    * Partition#LowerTriangular}, then only the lower triangular part is
    * assumed to be supplied, and the upper triangular part is set from its
    * transpose (matrix bounds permitting).  For Partition.LowerTriangular,
    * supplied entries which are not lower triangular will either be ignored or
    * generate an exception.
    *
    * <p>All specified data must fit within the current matrix
    * bounds; the matrix is not resized.
    *
    * <p>
    * CCCS data takes the form of three arrays:
    *
    * <dl>
    * <dt>vals</dt>
    * <dd>An array giving the values of all the non-zero elements in
    * the sparse matrix, in column-major order.</dd>
    * <dt>rowIdxs</dt>
    * <dd>An array giving the row indices of all the
    * non-zero elements in the sparse matrix, in column-major order. The indices
    * are 1-based, so that 1 denotes the first row.</dd>
    * <dt>colOffs</dt>
    * <dd>An array of size ncols+1, where ncols is the number of matrix
    * columns, giving the offsets into <code>vals</code> and
    * <code>colIdxs</code> corresponding to the first non-zero element in each
    * column. All values are 1-based, so that first offset value is 1, the
    * second offset value is n+1, where n is the number of non-zero elements in
    * the first column, etc. The final value is set to nvals+1, where nvals is
    * the number of non-zero elements in the matrix.</dd>
    * </dl>
    *
    * CCS index data is 1-based because this is the most standard form
    * used by solvers and matrix formats.
    * 
    * @param vals
    * non-zero element values. This array must have a length equal at
    * least to <code>nvals</code>.
    * @param rowIdxs
    * row indices for each non-zero element.
    * This array must have a length equal at
    * least to <code>nvals</code>.
    * @param colOffs column start offsets into <code>vals</code> and
    * <code>rowIdxs</code>. This array must have a length equal at least to
    * ncols+1, where ncols is the number of columns in this matrix.
    * @param nvals
    * number of non-zero values
    * @param ncols
    * number of specified columns. Indicates the maximum value of
    * <code>colOffs</code> that will be used; will not resize matrix.
    * @param part
    * must be either {@link Partition#Full Full} or
    * {@link Partition#LowerTriangular LowerTriangular}.
    */
   public void setCCSValues (
      double[] vals, int[] rowIdxs, int[] colOffs, int nvals, int ncols,
      Partition part);

   /**
    * Gets a submatrix of this matrix object. The first row and column of the
    * submatrix are given by <code>baseRow</code> and <code>baseCol</code>,
    * and the values are written into the matrix object <code>Mdest</code>.
    * The size of the submatrix is determined by the dimensions of
    * <code>Mdest</code>.
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
   public void getSubMatrix (int baseRow, int baseCol, DenseMatrix Mdest)
      throws ImproperSizeException;

   /**
    * Returns true if this matrix is symmetric within a given absolute 
    * tolerance. Specifically, each off-diagonal element must equal it's 
    * transposed counterpart within the given tolerance.
    * 
    * @param tol
    * absolute tolerance for checking equality of off-diagonal elements
    */
   public boolean isSymmetric (double tol);

   /**
    * Returns the number of non-zero values in this matrix object. For dense
    * matrices, this will simply be the product of the number of rows times the
    * number of columns.
    * 
    * @return number of non-zero values
    */
   public int numNonZeroVals();

   /**
    * Returns the number of non-zero values for a specified partition of a
    * principal sub-matrix of this matrix delimited by the first
    * <code>numRows</code> rows and the first <code>numCols</code> columns.
    * If the matrix is dense and the partition is {@link Partition#Full Full},
    * then this will simply be the product of the number of rows times the
    * number of columns.
    * 
    * @param part
    * matrix parition to be examined
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero values
    */
   public int numNonZeroVals (Partition part, int numRows, int numCols);

   /**
    * Returns a String representation of this matrix, in which each element is
    * formatted using a C <code>printf</code> style format string. The
    * exact format for this string is described in the documentation for
    * {@link maspack.util.NumberFormat#set(String)}{NumberFormat.set(String)}.
    * 
    * @param fmtStr
    * numeric printf style format string
    * @return String representation of this vector
    * @see maspack.util.NumberFormat
    */
   public String toString (String fmtStr);

   /**
    * Returns a String representation of this matrix, in which each element is
    * formatted using a C <code>printf</code> style format as decribed by the
    * parameter <code>NumberFormat</code>.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this vector
    */
   public String toString (NumberFormat fmt);

   /** 
    * Returns a string indicating the size of this matrix.
    * @return size of the matrix, in string form
    */   
   public String getSize();

   /** 
    * Check that the internal structures of this matrix are consistent.
    * Used for testing. 
    *
    * @throws IllegalStateException if the matrix structure is inconsistent.
    */
   public void checkConsistency();

}
