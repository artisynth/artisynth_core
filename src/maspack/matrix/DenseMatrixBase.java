/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;

/**
 * Base implementation of {@link maspack.matrix.Matrix Matrix}.
 */
public abstract class DenseMatrixBase extends MatrixBase implements DenseMatrix {

   /**
    * {@inheritDoc}
    */
   public abstract void set (int i, int j, double value);

   /**
    * {@inheritDoc}
    */
   public void set (double[] values) {
      int ncols = colSize();
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < ncols; j++) {
            set (i, j, values[i * ncols + j]);
         }
      }
   }

   /**
    * Sets the elements of this matrix from a 2-dimensional array of doubles.
    * 
    * @param values
    * array from which values are copied
    * @throws IllegalArgumentException
    * <code>values</code> has inconsistent row sizes
    * @throws ImproperSizeException
    * dimensions of <code>values</code> do not match the size of this matrix
    */
   public void set (double[][] values) {
      int nrows = values.length;
      int ncols = 0;
      if (nrows > 0) {
         ncols = values[0].length;
         for (int i = 1; i < nrows; i++) {
            if (values[i].length != ncols) {
               throw new IllegalArgumentException (
                  "inconsistent rows sizes in input");
            }
         }
      }
      if (nrows != rowSize() || ncols != colSize()) {
         if (isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            setSize (nrows, ncols);
         }
      }
      for (int i = 0; i < nrows; i++) {
         setRow (i, values[i]);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M.rowSize() != rowSize() || M.colSize() != colSize()) {
         if (isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            setSize (M.rowSize(), M.colSize());
         }
      }
      for (int i = 0; i < rowSize(); i++) {
         for (int j = 0; j < colSize(); j++) {
            set (i, j, M.get (i, j));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (Vector v) {
      int size = v.size();
      if (v instanceof VectorBase && ((VectorBase)v).isRowVector()) {
         if (rowSize() != 1 || colSize() != size) {
            if (isFixedSize()) {
               throw new ImproperSizeException();
            }
            else {
               setSize (1, size);
            }
         }
         for (int j = 0; j < size; j++) {
            set (0, j, v.get (j));
         }
      }
      else {
         if (rowSize() != size || colSize() != 1) {
            if (isFixedSize()) {
               throw new ImproperSizeException();
            }
            else {
               setSize (size, 1);
            }
         }
         for (int i = 0; i < size; i++) {
            set (i, 0, v.get (i));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] values, int[] indices, int numValues) {
      if (values.length < numValues) {
         throw new IllegalArgumentException (
            "numValues exceeds length of values array");
      }
      if (indices.length < 2 * numValues) {
         throw new IllegalArgumentException ("insufficient index values");
      }
      setZero();
      for (int k = 0; k < numValues; k++) {
         set (indices[2 * k], indices[2 * k + 1], values[k]);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      for (int i = 0; i < rowSize(); i++) {
         set (i, j, values[i]);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setRow (int i, double[] values) {
      for (int j = 0; j < colSize(); j++) {
         set (i, j, values[j]);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, Vector v) {
      if (v.size() != rowSize()) {
         throw new ImproperSizeException();
      }
      for (int i = 0; i < rowSize(); i++) {
         set (i, j, v.get (i));
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setRow (int i, Vector v) {
      if (v.size() != colSize()) {
         throw new ImproperSizeException();
      }
      for (int j = 0; j < colSize(); j++) {
         set (i, j, v.get (j));
      }
   }


   /**
    * {@inheritDoc}
    */
   public void setSubMatrix (int baseRow, int baseCol, Matrix Msrc)
      throws ImproperSizeException {
      if (baseRow < 0 || baseCol < 0) {
         throw new ImproperSizeException ("Negative row or column index");
      }
      int numSrcRows = Msrc.rowSize();
      int numSrcCols = Msrc.colSize();
      if (baseRow + numSrcRows > rowSize() ||
          baseCol + numSrcCols > colSize()) {
         throw new ImproperSizeException ("Dimensions out of bounds");
      }
      if (Msrc == this) { // nothing to do, since in this case baseRow and
                           // baseCol must
         // both equal 0 (otherwise a bounds exception would have been
         // tripped)
         return;
      }
      for (int i = 0; i < numSrcRows; i++) {
         for (int j = 0; j < numSrcCols; j++) {
            set (i + baseRow, j + baseCol, Msrc.get (i, j));
         }
      }
   }

   protected void setZero() {
      int nrows = rowSize();
      int ncols = colSize();
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            set (i, j, 0);
         }
      }
   }


   /**
    * {@inheritDoc}
    */
   protected void setRandom (
      double lower, double upper, Random generator) {
      double range = upper - lower;
      int nrows = rowSize();
      int ncols = colSize();
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            set (i, j, generator.nextDouble() * range + lower);
         }
      }
   }

   void setRandomEntryToNaN () {
      int i = RandomGenerator.nextInt (0, rowSize()-1);
      int j = RandomGenerator.nextInt (0, colSize()-1);
      set (i, j, 0.0/0.0);
   }

   /**
    * {@inheritDoc}
    */
   public void setCRSValues (
      double[] vals, int[] colIdxs, int[] rowOffs, int nvals, int nrows,
      Partition part) {

      checkSetCRSValuesArgs (vals, colIdxs, rowOffs, nvals, nrows, part);
      setZero();
      int ncols = colSize();
      for (int i=0; i<nrows; i++) {
         int max = (i < nrows-1 ? rowOffs[i+1]-1 : nvals);
         for (int off=rowOffs[i]-1; off<max; off++) {
            int j = colIdxs[off]-1;
            if (j >= ncols) {
               throw new ArrayIndexOutOfBoundsException (
                  "Column "+j+" in row "+i+" is out of range");
            }
            if (part == Partition.UpperTriangular) {
               if (j >= i) {
                  set (i, j, vals[off]);
                  if (i != j && j < nrows) {
                     set (j, i, vals[off]);
                  }
               }
            }
            else {
               set (i, j, vals[off]);
            }
         }
      }
   }


   /**
    * {@inheritDoc}
    */
   public void setCCSValues (
      double[] vals, int[] rowIdxs, int[] colOffs, int nvals, int ncols,
      Partition part) {

      checkSetCCSValuesArgs (vals, rowIdxs, colOffs, nvals, ncols, part);
      setZero();
      int nrows = rowSize();
      for (int j=0; j<ncols; j++) {
         int max = (j < ncols-1 ? colOffs[j+1]-1 : nvals);
         for (int off = colOffs[j]-1; off<max; off++) {
            int i = rowIdxs[off]-1;
            if (i >= nrows) {
               throw new ArrayIndexOutOfBoundsException (
                  "Row "+i+" in column "+j+" is out of range");
            }
            if (part == Partition.LowerTriangular) {
               if (i >= j) {
                  set (i, j, vals[off]);
                  if (i != j && i < ncols) {
                     set (j, i, vals[off]);
                  }
               }
            }
            else {
               set (i, j, vals[off]);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void checkConsistency() {
   }
 

   /**
    * Adds matrix M to this matrix provided dimensions match
    * @param M
    * matrix to add to this
    */
   public void add(DenseMatrixBase M) {
      
      if (rowSize() != M.rowSize() || colSize() != M.colSize()) {
         throw new ImproperSizeException ("Improper dimensions");
      }
      
      for (int i=0; i<rowSize(); i++) {
         for (int j=0; j<colSize(); j++) {
            set(i,j,  get(i,j)+M.get(i, j)  );
         }
      }
      
   }
   
}
