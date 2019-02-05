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
 * Implements general sparse matrix using CRS (compressed row storage) format.
 * The structure of the matrix, and the number of nonzero entries, is fixed.
 *
 * This code uses some ideas from Csparse, the sparse matrix package by Tim
 * Davis.
 *
 * IMPORTANT: the internal values of the column indices and row offsets, stored
 * as myCols and myRowOffs, are ZERO-based, as opposed to the convention used
 * by getCRSIndices() and setCRSValues(), in which they are ONE-based.
 */
public class SparseMatrixCRS extends SparseMatrixBase implements LinearTransformNd {
   protected int nrows = 0;
   protected int ncols = 0;
   protected int nvals = 0;
   protected boolean isSorted = true;

   protected double[] myVals = new double[0];
   protected int[] myCols = new int[0];    // column indices (ZERO based)
   protected int[] myRowOffs = new int[0]; // row offsets (ZERO based)

   /** 
    * Creates an empty sparse matrix with 0 size.
    */   
   public SparseMatrixCRS () {
      this (0, 0);
   }
   
   /**
    * Returns the value at provided index within compressed storage format
    * @param idx value index
    * @return value
    */
   public double getValue(int idx) {
      return myVals[idx];
   }
   
   /**
    * Returns the offset of the row within the compressed storage format
    * @param ridx index of row
    * @return row offset
    */
   public int getRowOffset(int ridx) {
      return myRowOffs[ridx];
   }
   
   /**
    * Returns the column at the provided index within compressed storage format 
    * @param idx index in compressed storage
    * @return column
    */
   public int getColumn(int idx) {
      return myCols[idx];
   }
   
   /**
    * Returns the row at the provided index within compressed storage format
    * @param idx index in compressed storage
    * @return row
    */
   public int getRow(int idx) {
      // binary search offsets
      int low = 0;
      int hi = nrows;
      
      while (hi != low) {
         int row = (hi+low)/2;
         int v = myRowOffs[row];
         if (idx < v) {
            hi = row;
         } else if (idx < myRowOffs[row+1]) {
            return row;
         } else {
            low = row+1;
         }
      }
      return low;
   }

   /** 
    * Creates an empty sparse matrix with a specified size.
    * 
    * @param numRows number of rows
    * @param numCols number of columns
    */   
   public SparseMatrixCRS (int numRows, int numCols) {
      setNumRowsCols (numRows, numCols);
      myRowOffs = new int[nrows+1];
   }

   /** 
    * Creates a sparse matrix from an existing general matrix.
    * 
    * @param M matrix to copy
    */   
   public SparseMatrixCRS (Matrix M) {
      this (0, 0);
      set (M);
   }

   /**
    * Creates a sparse matrix from CRS data. The number
    * of columns is determined from the column data.
    * 
    * @param vals
    * if non-null, gives all the nonzero values, in row order
    * @param cols
    * column index for each value, in row order.
    * @param rowOffs
    * gives the starting offset of each row into the cols and vals arrays
    * @throws IllegalArgumentException
    * if the specified storgae format is inconsistent
    */
   public SparseMatrixCRS (
      double[] vals, int[] cols, int[] rowOffs) {

      nrows = rowOffs.length-1;
      myRowOffs = new int[nrows+1];
      int maxcol = -1;
      for (int off=0; off<cols.length; off++) {
         if (cols[off] > maxcol) {
            maxcol = cols[off];
         }
      }
      ncols = maxcol+1;
      setCRSValues (vals, cols, rowOffs);
   }

   private void setCapacity (int newcap) {
      int cap = myVals.length;
      if (cap != newcap) {
         double[] newVals = new double[newcap];
         int[] newCols = new int[newcap];
         for (int i=0; i<Math.min(cap,newcap); i++) {
            newVals[i] = myVals[i];
            newCols[i] = myCols[i];
         }               
         myVals = newVals;
         myCols = newCols;
      }
   }

   private void setNumRowsCols (int numRows, int numCols) {
      nrows = numRows;
      ncols = numCols;
   }

   public void setSize (int numRows, int numCols) {
      if (numRows < 0 || numCols < 0) {
         throw new ImproperSizeException ("Negative dimension");
      }
      resetSize (numRows, numCols);
   }

   private void resetSize (int numRows, int numCols) {

      if (numCols < ncols) {
         int i = 0;
         int newoff = 0;
         int rowstart = myRowOffs[1];
         for (int off=0; off<nvals; off++) {
            if (off == rowstart) {
               i++;
               myRowOffs[i] = newoff;
               rowstart = myRowOffs[i+1];
            }
            if (myCols[off] < numCols) {
               myCols[newoff] = myCols[off];
               myVals[newoff] = myVals[off];
               newoff++;
            }
         }
         nvals = newoff;
         myRowOffs[nrows] = nvals;
      }
      if (numRows != nrows) {
         int[] newRowOffs = new int[numRows+1];
         for (int i=0; i<Math.min(nrows,numRows); i++) {
            newRowOffs[i] = myRowOffs[i];
         }
         if (numRows > nrows) {
            for (int i=nrows; i<=numRows; i++) {
               newRowOffs[i] = nvals;
            }
         }
         else { // numRows < nrows
            nvals = myRowOffs[numRows];
            newRowOffs[numRows] = nvals;
         }
         myRowOffs = newRowOffs;
      }
      setNumRowsCols (numRows, numCols);
   }

   /** 
    * Sort only the columns numbers. Used when the values have been explicitly
    * supplied by other means.
    */   
   private void sortColumns () {
      if (!isSorted) {
         for (int i=0; i<nrows; i++) {
            ArraySort.sort (myCols, myRowOffs[i], myRowOffs[i+1]-1);
         }
         isSorted = true;
      }
   }

   private void sortMatrix () {
      if (!isSorted) {
         for (int i=0; i<nrows; i++) {
            ArraySort.sort (myCols, myVals, myRowOffs[i], myRowOffs[i+1]-1);
         }
         isSorted = true;
      }
   }

   /** 
    * Ensures that the nonzero storage capacity of this matrix is at least n.
    * 
    * @param newcap desired storage capacity
    */   
   public void ensureCapacity (int newcap) {
      if (newcap > getCapacity()) {
         setCapacity (newcap);
      }
   }

   /** 
    * Returns the nonzero storage capacity of this matrix
    * 
    * @return nonzero storage capacity
    */   
   public int getCapacity (){
      return myVals.length;
   }

   /** 
    * Trims the nonzero storage capacity of this matrix to fit
    * the exact number of nonzeros.
    */   
   public void trimCapacity () {
      setCapacity (nvals);
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return nrows;
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return ncols;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isFixedSize() {
      return false;
   }

   // Gets the offset for the (i,j) element. Assumes that (i,j)
   // are within bounds. Returns -1 if (i,j) doesn't correspond to
   // a nonzero element.
   private int getOffset (int i, int j) {
      // the offset must lie in the range offLo <= off < offHi;
      int offLo = myRowOffs[i];
      int offHi = myRowOffs[i+1];
      if (isSorted) {
         // binary search
         while (offHi != offLo) {
            int off = (offHi+offLo)/2;
            int col = myCols[off];
            if (col == j) {
               return off;
            }
            else if (col < j) {
               offLo = off+1;
            }
            else {
               offHi = off;
            }
         }
      }
      else {
         // brute force
         for (int off=offLo; off<offHi; off++) {
            if (myCols[off] == j) {
               return off;
            }
         }
      }
      return -1;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (i >= nrows || j >= ncols) {
         throw new ArrayIndexOutOfBoundsException ("(i,j)=("+i+","+j+")");
      }
      int off = getOffset (i, j);
      if (off == -1) {
         return 0;
      }
      else {
         return myVals[off];
      }
   }

   /**
    * Returns the nonzero element values in row-major order. The values are
    * placed into an array of doubles. It is assumed that i and j values of
    * these elements are known.
    * 
    * @param values
    * collects the nonzero element values
    * @return number of nonzero elements
    * @throws ArrayIndexOutOfBoundsException
    * if the array is not large enough to store all the values
    */
   public int getNonzeroElements (double[] values) {
      if (!isSorted) {
         sortMatrix();
      }
      for (int off = 0; off < nvals; off++) {
         values[off] = myVals[off];
      }
      return nvals;
   }

   /**
    * Sets the existing nonzero element values, in row-major order. New values
    * are supplied by an array of doubles.
    * 
    * @param values
    * new values for the nonzero elements
    * @throws ArrayIndexOutOfBoundsException
    * if the array is not large enough to provide all the values
    */
   public void setNonzeroElements (double[] values) {
      if (values.length < nvals) {
         throw new ArrayIndexOutOfBoundsException (
            "Number of supplied values "+values.length+
            " less than number of non-zeros "+nvals);
      }
      for (int off=0; off<nvals; off++) {
         myVals[off] = values[off];
      }
      if (!isSorted) {
         sortColumns();
      }
   }

   /**
    * Returns the number of nonzero elements in this matrix.
    * 
    * @return number of nonzero elements
    */
   public int numNonzeroElements() {
      return nvals;
   }

   public void set (int i, int j, double value) {
      if (i >= nrows || j >= ncols) {
         throw new ArrayIndexOutOfBoundsException ("(i,j)=("+i+","+j+")");
      }
      int off = getOffset (i, j);
      if (off != -1) {
         myVals[off] = value;
      }
   }

   // XXX
   /**
    * Sets the elements of this matrix from an array of doubles and a list of
    * indices. Each value should be associated with two (i,j) index values,
    * which, for the k-th value, are given by <code>indices[k*2]</code> and
    * <code>indices[k*2+1]</code>, respectively. All non-specified elements
    * are set to zero.
    * 
    * @param values
    * nonzero values for specific matrix locations
    * @param indices
    * i and j indices for each explicit value
    * @param numVals
    * number of explicit values
    */
   public void set (double[] values, int[] indices, int numVals) {
      if (values.length < numVals) {
         throw new IllegalArgumentException (
            "numVals exceeds length of values array");
      }
      if (indices.length < 2 * numVals) {
         throw new IllegalArgumentException ("insufficient index values");
      }
      if (numVals > getCapacity()) {
         myCols = new int[numVals];
         myVals = new double[numVals];
      }
      int[] locs = new int[nrows]; // used to mark row locations
      for (int off=0; off<numVals; off++) {
         locs[indices[2*off]]++;
      }
      countsToOffsets (locs, locs, nrows);
      for (int i=0; i<nrows; i++) {
         myRowOffs[i] = locs[i];
      }
      myRowOffs[nrows] = numVals;

      isSorted = true;
      for (int off=0; off<numVals; off++) {
         int i = indices[2*off];
         int j = indices[2*off+1];
         int loc = locs[i];
         if (i < 0 || i >= nrows || j < 0 || j >= ncols) {
            throw new ArrayIndexOutOfBoundsException (
               "entry "+i+" "+j+" "+values[off]+" out of bounds, offset "+off);
         }
         myCols[loc] = j;
         myVals[loc] = values[off];
         if (locs[i] > myRowOffs[i]) {
            if (j < myCols[loc-1]) {
               isSorted = false;
            }
         }
         locs[i]++;
      }
      nvals = numVals;
   }

   /** 
    * Sets the structure of this matrix to that on another SparseMatrixCRS.
    */   
   private void setStructure (SparseMatrixCRS M) {
      if (M == this) {
         return;
      }
      setNumRowsCols (M.nrows, M.ncols);
      nvals = M.nvals;
      isSorted = M.isSorted;
      if (myRowOffs.length != nrows) {
         myRowOffs = new int[nrows+1];
      }
      for (int i=0; i<nrows; i++) {
         myRowOffs[i] = M.myRowOffs[i];
      }
      myRowOffs[nrows] = nvals;
      if (myVals.length < nvals) {
         myCols = new int[nvals];
         myVals = new double[nvals];
      }
      for (int off=0; off<nvals; off++) {
         myCols[off] = M.myCols[off];
      }
   }

   /**
    * Sets the values of this matrix from an existing general matrix M,
    * assuming the supplied partition type
    * @param M
    * matrix used to set values
    * @param part
    * partition type
    */
   public void set (Matrix M, Partition part) {
      if (M == this) {
         return;
      }
      nrows = M.rowSize();
      ncols = M.colSize();
      nvals = M.numNonZeroVals(part, nrows, ncols);
      isSorted = true;
      if (myRowOffs.length != nrows+1) {
         myRowOffs = new int[nrows+1];
      }
      if (myCols.length < nvals) {
         myCols = new int[nvals];
         myVals = new double[nvals];
      }
      M.getCRSIndices (myCols, myRowOffs, part, nrows, ncols);
      // myCols and myRowOffs are stored internally as zero-based,
      // so we decrement the values returned by getCRSIndices
      for (int i=0; i<nvals; i++) {
         myCols[i]--;
      }
      for (int i=0; i<nrows+1; i++) {
         myRowOffs[i]--;
      }
      M.getCRSValues (myVals, part, nrows, ncols);
      myRowOffs[nrows] = nvals;
   }
   
   /**
    * Sets the values of this matrix from an existing general matrix M.
    * 
    * @param M
    * matrix used to set values
    */
   public void set (Matrix M) {
      if (M == this) {
         return;
      }
      nrows = M.rowSize();
      ncols = M.colSize();
      nvals = M.numNonZeroVals();
      isSorted = true;
      if (myRowOffs.length != nrows+1) {
         myRowOffs = new int[nrows+1];
      }
      if (myCols.length < nvals) {
         myCols = new int[nvals];
         myVals = new double[nvals];
      }
      M.getCRSIndices (myCols, myRowOffs, Partition.Full, nrows, ncols);
      // myCols and myRowOffs are stored internally as zero-based,
      // so we decrement the values returned by getCRSIndices
      for (int i=0; i<nvals; i++) {
         myCols[i]--;
      }
      for (int i=0; i<nrows+1; i++) {
         myRowOffs[i]--;
      }
      M.getCRSValues (myVals, Partition.Full, nrows, ncols);
      myRowOffs[nrows] = nvals;
   }

   /**
    * Sets this matrix to the identity matrix. If this matrix matrix is not
    * square, then element (i,j) is set to 1 if i and j are equal, and 0
    * otherwise.
    *
    * <p>This method does not trim matrix capacity.
    */
   public void setIdentity() {
      nvals = Math.min(nrows, ncols);
      if (myCols.length < nvals) {
         myCols = new int[nvals];
         myVals = new double[nvals];
      }
      for (int i=0; i<=nrows; i++) {
         myRowOffs[i] = Math.min(i, nvals);
      }
      for (int off=0; off<nvals; off++) {
         myCols[off] = off;
         myVals[off] = 1;
      }
      isSorted = true;
   }

   /**
    * Sets this matrix to a diagonal matrix whose diagonal elements are
    * specified by an array.
    * 
    * <p>This method does not trim matrix capacity.
    * @param vals
    * diagonal elements for this matrix
    * @throws ImproperSizeException
    * if the length of <code>diag</code> does not equal the minimum matrix
    * dimension
    */
   public void setDiagonal (double[] vals) {
      nvals = Math.min (nrows, ncols);
      if (vals.length < nvals) {
         throw new IllegalArgumentException ("Insufficient values");
      }
      if (myCols.length < nvals) {
         myCols = new int[nvals];
         myVals = new double[nvals];
      }
      for (int i=0; i<=nrows; i++) {
         myRowOffs[i] = Math.min(i, nvals);
      }
      for (int off=0; off<nvals; off++) {
         myCols[off] = off;
         myVals[off] = vals[off];
      }
      isSorted = true;
   }

   /**
    * Sets the elements of this matrix to zero. This method
    * does not trim matrix capacity.
    */
   public void setZero() {
      nvals = 0;
      for (int i=0; i<=nrows; i++) {
         myRowOffs[i] = 0;
      }
   }

   /** 
    * Sets this matrix to have a random sparsity structure and values,
    * with between one and four nonzero elements per row.
    */
   public void setRandom () {
      setRandom (-0.5, 0.5, RandomGenerator.get());
   }

   /**
    * Sets the nonzero elements of this matrix to random values
    * in the range -0.5 (inclusive) to 0.5 (exclusive).
    */
   public void setRandomValues () {
      setRandomValues (-0.5, 0.5, RandomGenerator.get());
   }

   /**
    * Sets the nonzero elements of this matrix to random values
    * in a specified range.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    */
   public void setRandomValues (
      double lower, double upper, Random generator) {
      double range = upper - lower;
      for (int off=0; off<nvals; off++) {
         myVals[off] = generator.nextDouble() * range + lower;
      }
   }

   /** 
    * Forms the product
    * <pre>
    * x = x + beta B(i,:)
    * </pre>
    * and ensures that cols contains column entries for
    * all nonzeros of B(i,:). This is done by inspecting the
    * marks array: if marks[j] < mark, an entry for the column
    * does not yet exist in cols and so it is added.
    *
    * <p>
    * The method was taken from cs_scatter in the package
    * csparse, by Tim Davis.
    */
   private int accumulateScatterProduct (
      double[] x, double beta, SparseMatrixCRS B, int i,
      int[] cols, int mark, int[] marks, int coff) {

      int max = B.myRowOffs[i+1];
      for (int off=B.myRowOffs[i]; off<max; off++) {
         int j = B.myCols[off];
         if (marks[j] < mark) {
            cols[coff++] = j;
            marks[j] = mark;
            x[j] = beta*B.myVals[off];
         }
         else {
            x[j] += beta*B.myVals[off];
         }
      }
      return coff;
   }

   private int[] realloc (int[] array, int length) {
      int[] newArray = new int[length];
      for (int i=0; i<Math.min(array.length,length); i++){
         newArray[i] = array[i];
      }
      return newArray;
   }

   private double[] realloc (double[] array, int length) {
      double[] newArray = new double[length];
      for (int i=0; i<Math.min(array.length,length); i++){
         newArray[i] = array[i];
      }
      return newArray;
   }

   /**
    * Multiplies this matrix by M and places the result in this matrix. This
    * matrix is resized if necessary.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M do not conform
    */
   public void mul (SparseMatrixCRS M) {
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
    * if this matrix and M1 and M2 do not conform.
    */
   public void mul (SparseMatrixCRS M1, SparseMatrixCRS M2)
      throws ImproperSizeException {
      if (M1.ncols != M2.nrows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      int[] rowOffs = myRowOffs;
      int[] cols = myCols;
      double[] vals = myVals;
      if (M1 == this || M2 == this || M1.nrows != nrows) {
         rowOffs = new int[M1.nrows+1];
      }
      if (M1 == this || M2 == this) {
         cols = new int[M1.nvals+M2.nvals];
         vals = new double[M1.nvals+M2.nvals];
      }
      double[] x = new double[M2.ncols];
      int[] marks = new int[M2.ncols];
      int coff = 0;

      for (int i=0; i<M1.nrows; i++) {
         int neededcap = coff + M2.ncols;
         if (neededcap > cols.length) {
            cols = realloc (cols, 2*neededcap);
            vals = realloc (vals, 2*neededcap);
         }
         int max = M1.myRowOffs[i+1];
         rowOffs[i] = coff;
         for (int off=M1.myRowOffs[i]; off<max; off++) {
            coff = accumulateScatterProduct (
               x, M1.myVals[off], M2, M1.myCols[off], cols, i+1, marks, coff);
         }
         for (int off=rowOffs[i]; off<coff; off++) {
            vals[off] = x[cols[off]];
         }
      }
      nvals = coff;
      setNumRowsCols (M1.nrows, M2.ncols);
      rowOffs[nrows] = nvals;
      if (rowOffs != myRowOffs) {
         myRowOffs = rowOffs;
      }
      if (cols != myCols) {
         myCols = cols;
         myVals = vals;
      }
      isSorted = false;
   }


   /**
    * Multiplies this matrix by the transpose of M and places the result in this
    * matrix. This matrix is resized if necessary.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and the transpose of M do not conform
    */
   public void mulTranspose (SparseMatrixCRS M) {
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
    * if M1 and the transpose M2 do not comform
    */
   public void mulTransposeRight (SparseMatrixCRS M1, SparseMatrixCRS M2)
      throws ImproperSizeException {

      if (M1.ncols != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseMatrixCRS MT = new SparseMatrixCRS(M2.ncols, M2.nrows);
      MT.transpose (M2);
      mul (M1, MT);
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
    * if M1 and the transpose M2 do not comform
    */
   public void mulTransposeLeft (SparseMatrixCRS M1, SparseMatrixCRS M2)
      throws ImproperSizeException {

      if (M1.nrows != M2.nrows) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseMatrixCRS MT = new SparseMatrixCRS(M1.ncols, M1.nrows);
      MT.transpose (M1);
      mul (MT, M2);
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
    * if M1 and the transpose M2 do not comform
    */
   public void mulTransposeBoth (SparseMatrixCRS M1, SparseMatrixCRS M2)
      throws ImproperSizeException {

      if (M1.nrows != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseMatrixCRS MT1 = new SparseMatrixCRS(M1.ncols, M1.nrows);
      MT1.transpose (M1);
      SparseMatrixCRS MT2 = new SparseMatrixCRS(M2.ncols, M2.nrows);
      MT2.transpose (M2);
      mul (MT1, MT2);
   }

   /** 
    * Returns the dot product of the i-th row with itself.
    * 
    * @param i row index
    * @return dot product of row i with itself
    */   
   public double dotRowSelf (int i) {
      if (i < 0 || i >= nrows) {
         throw new ArrayIndexOutOfBoundsException (
            "Row "+i+" is out of range");
      }
      double dot = 0;
      int end = myRowOffs[i+1];
      for (int off=myRowOffs[i]; off<end; off++) {
         dot += myVals[off]*myVals[off];
      }
      return dot;
   }

   /** 
    * Returns the dot product of the i-th row with a vector v.
    * 
    * @param i row index
    * @param v vector to take dot product with
    * @return dot product of row i with v
    */   
   public double dotRow (int i, VectorNd v) {
      if (i < 0 || i >= nrows) {
         throw new ArrayIndexOutOfBoundsException (
            "Row "+i+" is out of range");
      }
      double[] buf = v.getBuffer();
      double dot = 0;
      int end = myRowOffs[i+1];
      for (int off=myRowOffs[i]; off<end; off++) {
         dot += myVals[off]*buf[myCols[off]];
      }
      return dot;
   }

   /** 
    * Adds s times the i-th row to the vector v.
    * 
    * @param v vector to add scaled row to
    * @param s scaling factor
    * @param i row index
    */   
   public void addScaledRow (VectorNd v, double s, int i) {
      if (i < 0 || i >= nrows) {
         throw new ArrayIndexOutOfBoundsException (
            "Row "+i+" is out of range");
      }
      double[] buf = v.getBuffer();
      int end = myRowOffs[i+1];
      for (int off=myRowOffs[i]; off<end; off++) {
         buf[myCols[off]] += s*myVals[off];
      }
   }

   /**
    * Adds this matrix to M and places the result in this matrix.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException if this matrix and M have different sizes.
    */
   public void add (SparseMatrixCRS M) {
      add (this, M);
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
    * if matrices M1 and M2 have different sizes.
    */
   public void add (SparseMatrixCRS M1, SparseMatrixCRS M2) {
      combine (1, M1, 1, M2);
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
    * if matrices M1 and M2 have different sizes
    */
   public void scaledAdd (double s, SparseMatrixCRS M1, SparseMatrixCRS M2) {
      combine (s, M1, 1, M2);
   }

   /**
    * Subtracts M from this matrix.
    * 
    * @param M
    * matrix to subtract
    * @throws ImproperSizeException if this matrix and M have different sizes.
    */
   public void sub (SparseMatrixCRS M) {
      sub (this, M);
   }

   /**
    * Subtracts M1 from M2 and places the result in this matrix. This matrix is
    * resized if necessary.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if matrices M1 and M2 have different sizes.
    */
   public void sub (SparseMatrixCRS M1, SparseMatrixCRS M2) {
      combine (1, M1, -1, M2);
   }

   /**
    * Forms the combination a M1 + b M2 and places the result in this
    * matrix. This matrix is resized if necessary.
    *
    * @param a
    * scaling factor for M1
    * @param M1
    * left-hand matrix
    * @param b
    * scaling factor for M2
    * @param M2
    * right-hand matrix
    * @throws ImproperSizeException
    * if matrices M1 and M2 have different sizes.
    */
   public void combine (
      double a, SparseMatrixCRS M1, double b, SparseMatrixCRS M2) {
      if (M1.nrows != M2.nrows || M1.ncols != M2.ncols) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      int[] rowOffs = myRowOffs;
      int[] cols = myCols;
      double[] vals = myVals;
      if (M1 == this || M2 == this || M1.nrows != nrows) {
         rowOffs = new int[M1.nrows+1];
      }
      if (M1 == this || M2 == this || myCols.length < M1.nvals+M2.nvals) {
         cols = new int[M1.nvals+M2.nvals];
         vals = new double[M1.nvals+M2.nvals];
      }
      double[] x = new double[M1.ncols];
      int[] marks = new int[M1.ncols];
      int coff = 0;
      for (int i=0; i<M1.nrows; i++) {
         rowOffs[i] = coff;
         coff = accumulateScatterProduct (x, a, M1, i, cols, i+1, marks, coff);
         coff = accumulateScatterProduct (x, b, M2, i, cols, i+1, marks, coff);
         for (int off=rowOffs[i]; off<coff; off++) {
            vals[off] = x[cols[off]];
         }
      }
      nvals = coff;
      setNumRowsCols (M1.nrows, M1.ncols);
      rowOffs[nrows] = nvals;
      if (rowOffs != myRowOffs) {
         myRowOffs = rowOffs;
      }
      if (cols != myCols) {
         myCols = cols;
         myVals = vals;
      }
      isSorted = false;
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      for (int off=0; off<nvals; off++) {
         myVals[off] = -myVals[off];
      }
   }

   /** 
    * Sets this matrix to the negative of another matrix M. This matrix is
    * resized if necessary.
    * 
    * @param M matrix to be 
    */
   public void negate (SparseMatrixCRS M) {
      if (M == this) {
         negate();
      }
      else {
         setStructure (M);
         for (int off=0; off<nvals; off++) {
            myVals[off] = -M.myVals[off];
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
      for (int off=0; off<nvals; off++) {
         myVals[off] *= s;
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
    */
   public void scale (double s, SparseMatrixCRS M) {
      if (M == this) {
         scale(s);
      }
      else {
         setStructure (M);
         for (int off=0; off<nvals; off++) {
            myVals[off] = s*M.myVals[off];
         }
      }
   }

   /**
    * Sets this matrix its transpose, changing its size and
    * sparsity structure if necessary.
    */
   public void transpose () {
      transpose (this);
   }

   /**
    * Sets this matrix to the transpose of M, changing the size and
    * sparsity structure of this matrix if necessary.
    * 
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (SparseMatrixCRS M) {

      int[] rowOffs = myRowOffs;
      int[] cols = myCols;
      double[] vals = myVals;

      if (M == this || M.ncols != nrows) {
         rowOffs = new int[M.ncols+1];
      }
      if (M == this || M.nvals > nvals) {
         cols = new int[M.nvals];
         vals = new double[M.nvals];
      }
      M.getColOffsets (rowOffs, Partition.Full, M.nrows, M.ncols);
      
      // now place entries in appropriate rows of the result. rowOffs is used
      // as a location counter for each row and will be reset after.
      for (int i=0; i<M.nrows; i++) {
         int max = M.myRowOffs[i+1];
         for (int off=M.myRowOffs[i]; off<max; off++) {
            int j = M.myCols[off];
            int idx = rowOffs[j]++;
            cols[idx] = i;
            vals[idx] = M.myVals[off];
         }
      }
      // reset row offs
      for (int i=M.ncols; i>0; i--) {
         rowOffs[i] = rowOffs[i-1];
      }
      rowOffs[0] = 0;
      setNumRowsCols (M.ncols, M.nrows);
      nvals = rowOffs[nrows];
      if (rowOffs != myRowOffs) {
         myRowOffs = rowOffs;
      }
      if (cols != myCols) {
         myCols = cols;
         myVals = vals;
      }
      isSorted = false;
   }

   // protected void mulVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int i=0; i<nr; i++) {
   //       double sum = 0;
   //       int end = myRowOffs[i+1];
   //       for (int off=myRowOffs[i]; off<end; off++) {
   //          int j = myCols[off];
   //          if (j < nc) {
   //             sum += myVals[off]*vec[j];
   //          }
   //       }
   //       res[i] = sum;
   //    }
   // }

   // protected void mulAddVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int i=0; i<nr; i++) {
   //       double sum = 0;
   //       int end = myRowOffs[i+1];
   //       for (int off=myRowOffs[i]; off<end; off++) {
   //          int j = myCols[off];
   //          if (j < nc) {
   //             sum += myVals[off]*vec[j];
   //          }
   //       }
   //       res[i] += sum;
   //    }
   // }

   // protected void mulTransposeVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int j=0; j<nr; j++) {
   //       res[j] = 0;
   //    }
   //    for (int i=0; i<nc; i++) {
   //       int end = myRowOffs[i+1];
   //       for (int off=myRowOffs[i]; off<end; off++) {
   //          int j = myCols[off];
   //          if (j < nr) {
   //             res[j] += myVals[off]*vec[i];
   //          }
   //       }
   //    }
   // }

   protected void mulVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {
      int rowf = r0+nr;
      int colf = c0+nc;
      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         int end = myRowOffs[i+1];
         for (int off=myRowOffs[i]; off<end; off++) {
            int j = myCols[off];
            if (j >= c0 && j < colf) {
               sum += myVals[off]*vec[j-c0];
            }
         }
         res[i-r0] = sum;
      }
   }

   protected void mulAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {
      int rowf = r0+nr;
      int colf = c0+nc;
      for (int i=r0; i<rowf; i++) {
         double sum = 0;
         int end = myRowOffs[i+1];
         for (int off=myRowOffs[i]; off<end; off++) {
            int j = myCols[off];
            if (j >= c0 && j < colf) {
               sum += myVals[off]*vec[j-c0];
            }
         }
         res[i-r0] += sum;
      }
   }

   protected void mulTransposeVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      for (int j=0; j<nr; j++) {
         res[j] = 0;
      }
      mulTransposeAddVec (res, vec, r0, nr, c0, nc);
   }

   protected void mulTransposeAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      // rowf and colf are with respect to the *transposed* matrix
      int rowf = r0+nr;
      int colf = c0+nc;

      for (int i=c0; i<colf; i++) {
         int end = myRowOffs[i+1];
         for (int off=myRowOffs[i]; off<end; off++) {
            int j = myCols[off];
            if (j >= r0 && j < rowf) {
               res[j-r0] += myVals[off]*vec[i-c0];
            }
         }
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
      StringBuffer buf = new StringBuffer (20 * nvals);
      if (!isSorted) {
      System.out.println ("CRS  93");
         sortMatrix();
      }
      int i = -1;
      for (int off=0; off<nvals; off++) {
         while (myRowOffs[i+1] == off) {
            i++;
         }
         buf.append (
               "("+i+" "+myCols[off]+" "+fmt.format(myVals[off])+")\n");
      }
      return buf.toString();
   }

   @Override
   public int numNonZeroVals (Partition part, int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
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
            if (numRows == rowSize() && numCols == colSize()) {
               return nvals;
            }
            for (int i=0; i<numRows; i++) {
               int max = myRowOffs[i+1];
               for (int off=myRowOffs[i]; off<max; off++) {
                  if (myCols[off] < numCols) {
                     cnt++;
                  }
               }
            }
            break;
         }
         case UpperTriangular: {
            for (int i=0; i<numRows; i++) {
               int max = myRowOffs[i+1];
               for (int off=myRowOffs[i]; off<max; off++) {
                  if (myCols[off] < numCols && myCols[off] >= i) {
                     cnt++;
                  }
               }
            }
            break;
         }
         case LowerTriangular: {
            for (int i=0; i<numRows; i++) {
               int max = myRowOffs[i+1];
               for (int off=myRowOffs[i]; off<max; off++) {
                  if (myCols[off] < numCols && myCols[off] <= i) {
                     cnt++;
                  }
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
   public void setCRSValues (
      double[] vals, int[] colIdxs, int[] rowOffs, int numVals, int numRows,
      Partition part) {

      if (part == Partition.UpperTriangular) {
         super.setCRSValues (vals, colIdxs, rowOffs, numVals, numRows, part);
         return;
      }
      checkSetCRSValuesArgs (vals, colIdxs, rowOffs, numVals, numRows, part);

      if (myCols.length < numVals) {
         myCols = new int[numVals];
         myVals = new double[numVals];
      }
      for (int i=0; i<=nrows; i++) {
         if (i<numRows) {
            int off = rowOffs[i]-1;
            int max = (i+1 < numRows ? rowOffs[i+1]-1 : numVals);
            if (off > max) {
               throw new IllegalArgumentException (
                  "rowOffs "+i+","+(i+1)+" = "+(off+1)+","+(max+1));
            }
            myRowOffs[i] = off;
         }
         else {
            myRowOffs[i] = numVals;
         }
      }
      isSorted = true;
      for (int i=0; i<nrows; i++) {
         int max = myRowOffs[i+1];
         for (int off=myRowOffs[i]; off<max; off++) {
            int j = colIdxs[off]-1;
            if (j >= ncols) {
               throw new ArrayIndexOutOfBoundsException (
                  "Column "+j+" in row "+i+" is out of range");
            }
            if (off+1 < max && colIdxs[off+1]-1 < j) {
               isSorted = false;
            }
            myCols[off] = j;
            myVals[off] = vals[off];
         }
      }
      nvals = numVals;
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
      if (!isSorted) {
         sortMatrix();
      }
      if (numRows == nrows && numCols == ncols && part == Partition.Full) {
         for (int i = 0; i < numRows; i++) {
            rowOffs[i] = myRowOffs[i]+1;
         }
         for (int off=0; off<nvals; off++) {
            colIdxs[off] = myCols[off]+1;
         }
         rowOffs[numRows] = nvals+1;
         return nvals;
      }
      else {
         int idx = 0;
         for (int i = 0; i < numRows; i++) {
            rowOffs[i] = idx+1;
            int jbegin = (part == Partition.UpperTriangular ? i : 0);
            int max = myRowOffs[i+1];
            for (int off=myRowOffs[i]; off<max; off++) {
               int j = myCols[off];
               if (j >= numCols) {
                  break;
               }
               if (j >= jbegin) {
                  colIdxs[idx++] = j+1;
               }
            }
         }
         rowOffs[numRows] = idx+1;
         return idx;
      }
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
      if (!isSorted) {
         sortMatrix();
      }
      if (numRows == nrows && numCols == ncols && part == Partition.Full) {
         for (int off=0; off<nvals; off++) {
            vals[off] = myVals[off];
         }
         return nvals;
      }
      else {
         int idx = 0;
         for (int i = 0; i < numRows; i++) {
            int jbegin = (part == Partition.UpperTriangular ? i : 0);
            int max = myRowOffs[i+1];
            for (int off=myRowOffs[i]; off<max; off++) {
               int j = myCols[off];
               if (j >= numCols) {
                  break;
               }
               if (j >= jbegin) {
                  vals[idx++] = myVals[off];
               }
            }
         }
         return idx;
      }
   }

   private int getColOffsets (
      int[] colOffs, Partition part, int numRows, int numCols) {
      
      for (int j=0; j<numCols; j++) {
         colOffs[j] = 0;
      }
      if (numRows == nrows && numCols == ncols && part == Partition.Full) {
         for (int off=0; off<nvals; off++) {
            colOffs[myCols[off]]++;
         }
      }
      else {
         for (int i=0; i<numRows; i++) {
            int max = myRowOffs[i+1];
            int maxj = numCols-1;
            if (part == Partition.LowerTriangular) {
               maxj = Math.min (i, maxj);
            }
            for (int off=myRowOffs[i]; off<max; off++) {
               int j = myCols[off];
               if (j <= maxj) {
                  colOffs[j]++;
               }
            }
         }
      }
      return countsToOffsets (colOffs, colOffs, numCols);
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
      if (!isSorted) {
         sortMatrix();
      }
      int[] locOffs = new int[numCols];
      int nnz = getColOffsets (colOffs, part, numRows, numCols);

      for (int i = 0; i < numRows; i++) {
         int max = myRowOffs[i+1];
         int jend = numCols-1;
         if (part == Partition.LowerTriangular) {
            jend = Math.min (i, jend);
         }
         for (int off=myRowOffs[i]; off<max; off++) {
            int j = myCols[off];
            if (j > jend) {
               break;
            }
            int idx = colOffs[j] + locOffs[j]++;
            rowIdxs[idx] = i+1;
         }
      }
      colOffs[numCols] = nnz;
      for (int i=0; i<=numCols; i++) {
         // XXX increment colOffs because they have been computed 0-based
         // and they need to be returned 1-based
         colOffs[i]++;
      }
      return nnz;
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
      if (!isSorted) {
         sortMatrix();
      }
      int[] locOffs = new int[numCols];
      int[] colOffs = new int[numCols];
      int nnz = getColOffsets (colOffs, part, numRows, numCols);

      for (int i = 0; i < numRows; i++) {
         int max = myRowOffs[i+1];
         int jend = numCols-1;
         if (part == Partition.LowerTriangular) {
            jend = Math.min (i, jend);
         }
         for (int off=myRowOffs[i]; off<max; off++) {
            int j = myCols[off];
            if (j > jend) {
               break;
            }
            int idx = colOffs[j] + locOffs[j]++;
            vals[idx] = myVals[off];
         }
      }
      return nnz;
   }

   /** 
    * {@inheritDoc}
    */
   public void checkConsistency () {
      if (myRowOffs.length != nrows+1) {
         throw new TestException (
            "number of row offsets not equal to nrows+1");
      }
      if (myRowOffs[0] != 0) {
         throw new TestException (
            "first row offset is not 0");
      }
      if (myRowOffs[nrows] != nvals) {
         throw new TestException (
            "last row offset not equals to nvals");
      }
      for (int i=0; i<nrows; i++) {
         if (myRowOffs[i] > nvals) {
            throw new TestException (
               "rowOffs at "+i+" exceeds nvals");
         }
         if (myRowOffs[i] > myRowOffs[i+1]) {
            throw new TestException (
               "rowOffs at "+i+","+(i+1)+" = "+myRowOffs[i]+","+myRowOffs[i+1]);
         }
      }
      for (int i=0; i<nrows; i++) {
         int end = (i+1 < nrows ? myRowOffs[i+1] : nvals);
         int lastj = -1;
         for (int off=myRowOffs[i]; off<end; off++) {
            int j = myCols[off];
            if (j <= lastj) {
               throw new TestException (
                  "columns not increasing for row "+i);
            }
            if (j < 0 || j >+ ncols) {
               throw new TestException (
                  "column "+j+" out of range in row "+i);
            }
         }
      }
   }
   
   public void zeroRow(int i) {
      // find row i
      int offLow = myRowOffs[i];
      int offHigh = myRowOffs[i+1];
      for (int off = offLow; off<offHigh; off++) {
         myVals[off] = 0;
      }
   }
   
   public void zeroColumn(int j) {
      
      for (int i = 0; i<rowSize(); i++) {
         // find column j
         int off = getOffset(i, j);
         if (off >= 0) {
            myVals[off] = 0;
         }
      }
   }
   
   @Override
   public double maxNorm() {
      double m = 0;
      for (int i=0; i<myVals.length; ++i) {
         double a = Math.abs(myVals[i]);
         if (a > m) {
            m = a;
         }
      }
      return m;
   }
   
   public void add(int i, int j, double v) {
      int offset = getOffset(i, j);
      if (offset >= 0) {
         myVals[offset] += v;
      } else {
         set(i, j, v);
      }
   }
}
