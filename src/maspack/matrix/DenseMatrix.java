/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * General interface for dense matrices. 
 */
public interface DenseMatrix extends Matrix {

   /**
    * Sets a single element of this matrix.
    * 
    * @param i
    * element row index
    * @param j
    * element column index
    * @param value
    * element value
    */
   public void set (int i, int j, double value);

   /**
    * Sets the size and values of this matrix to correspond to those of vector
    * object.
    * 
    * @param v
    * vector whose size and values are copied
    * @throws ImproperSizeException
    * if this matrix cannot be resized to correspond to the dimensions of the
    * vector
    */
   public void set (Vector v);

   /**
    * Sets the elements of this matrix from an array of doubles. The elements in
    * the array should be stored using row-major order, so that element
    * <code>(i,j)</code> is stored at location <code>i*colSize()+j</code>.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values);


   /**
    * Sets a submatrix of this matrix object. The first row and column of the
    * submatrix are given by <code>baseRow</code> and <code>baseCol</code>,
    * and the new values are given by the matrix object <code>Msrc</code>.
    * The size of the submatrix is determined by the dimensions of
    * <code>Msrc</code>.
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
      throws ImproperSizeException;

   /**
    * Sets a column of this matrix from an array of doubles.
    * 
    * @param j
    * column index
    * @param values
    * array from which column values are copied
    */
   public void setColumn (int j, double[] values);

   /**
    * Set a row of this matrix from an array of doubles.
    * 
    * @param i
    * row index
    * @param values
    * array from which the row is copied
    */
   public void setRow (int i, double[] values);

   /**
    * Sets a column of this matrix from a {@link maspack.matrix.Vector Vector}.
    * 
    * @param j
    * column index
    * @param v
    * vector from which the column is copied
    * @throws ImproperSizeException
    * vector's size not equal to the number of matrix rows
    */
   public void setColumn (int j, Vector v);

   /**
    * Sets a row of this matrix from a {@link maspack.matrix.Vector Vector}.
    * 
    * @param i
    * row index
    * @param v
    * vector from which the row is copied
    * @throws ImproperSizeException
    * vector's size not equal to the number of matrix columns
    */
   public void setRow (int i, Vector v);


}
