/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;
import java.util.*;

import maspack.util.ArraySort;
import maspack.util.ArraySupport;
import maspack.util.DynamicIntArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.Clonable;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;
import maspack.util.TestException;

/**
 * Implements a square sparse matrix composed of MatrixBlocks.
 */
public class SparseCRSMatrix extends SparseMatrixBase implements Clonable {
   
   int myNumCols = 0;
   int myNumVals = 0;
   // true if the columns in each row are currently sorted
   protected boolean isSorted = true;

   class RowData {
      static final int DEFAULT_INITIAL_CAPACITY = 16;

      int[] myColIdxs;
      double[] myValues;
      int myNumVals;

      RowData (int cap) {
         myColIdxs = new int[cap];
         myValues = new double[cap];
         myNumVals = 0;
      }

      /**
       * Creates a new row from a set of column indices and values. Indices are
       * assumed to be sorted and in ascending order.
       */
      RowData (int[] colIdxs, double[] values, int numVals) {
         this (numVals);
         set (colIdxs, values, numVals);
      }

      RowData () {
         this (DEFAULT_INITIAL_CAPACITY);
      }

      RowData (RowData row) {
         set (row);
      }

      void set (RowData row) {
         myColIdxs = Arrays.copyOf (row.myColIdxs, row.myColIdxs.length);
         myValues = Arrays.copyOf (row.myValues, row.myValues.length);
         myNumVals = row.myNumVals;
      }

      void setScaled (double s, RowData row) {
         myColIdxs = Arrays.copyOf (row.myColIdxs, row.myColIdxs.length);
         myValues = new double[row.myValues.length];
         for (int k=0; k<myValues.length; k++) {
            myValues[k] = s*row.myValues[k];
         }
         myNumVals = row.myNumVals;
      }

      int numVals() {
         return myNumVals;
      }         

      int getOffset (int j) {
         int lo = 0;
         int hi = myNumVals-1;
         while (hi != lo) {
            int off = (hi+lo)/2;
            int col = myColIdxs[off];
            if (col == j) {
               return off;
            }
            else if (col < j) {
               lo = off+1;
            }
            else {
               hi = off;
            }
         }
         return -1;
      }

      void removeVals() {
         myNumVals = 0;
      }

      double getValue (int j) {
         // binary search. Assumes columns are sorted
         int lo = 0;
         int hi = myNumVals-1;
         if (hi == -1) {
            return 0;
         }
         while (hi != lo) {
            int k = (hi+lo)/2;
            if (myColIdxs[k] == j) {
               return myValues[k];
            }
            else if (myColIdxs[k] < j) {
               lo = (k == lo ? hi : k);
            }
            else {
               hi = k;
            }
         }
         if (myColIdxs[lo] == j) {
            return myValues[lo];
         }
         else {
            return 0;
         }
      }

      void setMaxCol (int maxCols) {
         int k;
         for (k=0; k<numVals(); k++) {
            if (myColIdxs[k] >= maxCols) {
               break;
            }
         }
         myNumVals = k;
      }

      int getInsertIndex (int j) {
         // binary search. If column j not found, return index at
         // which the data should be inserted

         int k = myNumVals;
         if (k > 0) {
            // Assume we might be inserting close to the end
            if (myColIdxs[k-1] < j) {
               return k;
            }
            else if (myColIdxs[k-1] == j) {
               return k-1;
            }
     
            // binary search
            int lo = 0;
            int hi = k-1;
            while (hi != lo) {
               k = (hi+lo)/2;
               if (myColIdxs[k] == j) {
                  return k;
               }
               else if (myColIdxs[k] < j) {
                  lo = (k == lo ? hi : k);
               }
               else {
                  hi = k;
               }
            }
            k = lo;
         }
         return k;
      }

      /**
       * Sets a value at column location j in this row. Returns
       * {@code true} if a new value was inserted, or {@code false}
       * if an old value was simply overwritten.
       */
      boolean setValue (int j, double val) {
         // optimize on the assumption that we are adding values in increasing
         // column order
         if (myNumVals == 0 || myColIdxs[myNumVals-1] < j) {
            // append new value
            ensureCapacity (myNumVals+1);
            myColIdxs[myNumVals] = j;
            myValues[myNumVals++] = val;
            return true;
         }
         else {
            // location is somewhere inside the values
            int k = getInsertIndex (j);
            if (myColIdxs[k] == j) {
               // value for j already exists at location k; replace
               myValues[k] = val;
               return false;
            }
            else {
               // insert new value for j at location k
               ensureCapacity (myNumVals+1);
               // shift other values right
               for (int l=myNumVals; l > k; l--) {
                  myValues[l] = myValues[l-1];
                  myColIdxs[l] = myColIdxs[l-1];
               }
               myValues[k] = val;
               myColIdxs[k] = j;
               myNumVals++;
               return true;
            }
         }
      }

      /**
       * Adds to the value at column location j in this row. Returns {@code
       * true} if a new value was inserted, or {@code false} if an old value
       * was simply added to.
       */
      boolean addValue (int j, double val) {
         // optimize on the assumption that we are adding values in increasing
         // column order
         if (myNumVals == 0 || myColIdxs[myNumVals-1] < j) {
            // append new value
            ensureCapacity (myNumVals+1);
            myColIdxs[myNumVals] = j;
            myValues[myNumVals++] = val;
            return true;
         }
         else {
            // location is somewhere inside the values
            int k = getInsertIndex (j);
            if (myColIdxs[k] == j) {
               // value for j already exists at location k; replace
               myValues[k] += val;
               return false;
            }
            else {
               // insert new value for j at location k
               ensureCapacity (myNumVals+1);
               // shift other values right
               for (int l=myNumVals; l > k; l--) {
                  myValues[l] = myValues[l-1];
                  myColIdxs[l] = myColIdxs[l-1];
               }
               myValues[k] = val;
               myColIdxs[k] = j;
               myNumVals++;
               return true;
            }
         }
      }

      void ensureCapacity (int cap) {
         if (myValues.length < cap) {
            int oldCap = myValues.length;            
            int newCap = oldCap;

            if (newCap - cap < 0) {  // overflow aware
               newCap = oldCap + (oldCap >> 1);  // 1.5x growth
            }
            if (newCap - cap < 0) {
               newCap = cap;
            }
            if (newCap > oldCap) {
               myValues = Arrays.copyOf (myValues, newCap);
               myColIdxs = Arrays.copyOf (myColIdxs, newCap);
            }
         }
      }

      /**
       * Sets the values of this column from a set of column indices and
       * values. Indices are assumed to be sorted and in ascending order.
       */
      void set (int[] colIdxs, double[] values, int numVals) {
         ensureCapacity (numVals);
         for (int k=0; k<numVals; k++) {
            myColIdxs[k] = colIdxs[k];
            myValues[k] = values[k];
         }
         myNumVals = numVals;         
      }

   }

   ArrayList<RowData> myRows = new ArrayList<>();

   private void initRows (int numRows) {
      myRows = new ArrayList<>(numRows);
      for (int i=0; i<numRows; i++) {
         myRows.add (new RowData());
      }
   }

   /**
    * Creates an empty SparseCRSMatrix with 0 x 0 size.
    */
   public SparseCRSMatrix () {
      myRows = new ArrayList<>();
   }

   /** 
    * Creates an empty sparse matrix with a specified size.
    * 
    * @param numRows number of rows
    * @param numCols number of columns
    */   
   public SparseCRSMatrix (int numRows, int numCols) {
      myNumCols = numCols;
      initRows (numRows);
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
   public SparseCRSMatrix (
      double[] vals, int[] cols, int[] rowOffs) {

      int nrows = rowOffs.length-1;
      initRows (nrows);
      int maxcol = -1;
      for (int off=0; off<cols.length; off++) {
         if (cols[off] > maxcol) {
            maxcol = cols[off];
         }
      }
      myNumCols = maxcol+1;
      setCRSValues (vals, cols, rowOffs);
   }

   /** 
    * Creates a sparse matrix from an existing general matrix.
    * 
    * @param M matrix to copy
    */   
   public SparseCRSMatrix (Matrix M) {
      this (0, 0);
      set (M);
   }

   public void setSize (int numRows, int numCols) {
      if (numRows < 0 || numCols < 0) {
         throw new ImproperSizeException ("Negative dimension");
      }
      resetSize (numRows, numCols);
   }

   /**
    * Sets the number of rows of this matrix, removing or adding rows as
    * necessary.
    */
   private void setRowSize (int nrows) {
      if (nrows < 0) {
         throw new IllegalArgumentException ("nrows is negative");
      }
      if (nrows == 0) {
         myRows.clear();
         myNumVals = -1;
      }
      else if (nrows < rowSize()) {
         // remove trailing rows, starting at the end for best efficency
         for (int idx=rowSize()-1; idx>=nrows; idx--) {
            myRows.remove (idx);
         }
         myNumVals = -1;
      }
      else if (nrows > rowSize()) {
         int nr = nrows-rowSize();
         // add additional (empty) rows
         for (int i=0; i<nr; i++) {
            myRows.add (new RowData());
         }
      }
   }

   private void resetSize (int numRows, int numCols) {

      setRowSize (numRows);
      if (numCols < colSize()) {
         for (RowData row : myRows) {
            row.setMaxCol (numCols);
         }
         myNumVals = -1;
      }
      if (myNumVals == -1) {
         recomputeNumVals();
      }
      myNumCols = numCols;
   }

   private void sortMatrix () {
      if (!isSorted) {
         for (int i=0; i<rowSize(); i++) {
            RowData row = myRows.get(i);
            ArraySort.sort (row.myColIdxs, row.myValues);
         }
         isSorted = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return myRows.size();
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return myNumCols;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isFixedSize() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (i >= rowSize() || j >= colSize()) {
         throw new ArrayIndexOutOfBoundsException ("(i,j)=("+i+","+j+")");
      }
      return myRows.get(i).getValue (j);
   }

   /**
    * Copies the contents of this matrix into a dense {@code MatrixNd}.
    *
    * @param M matrix to receive the contents of this matrix
    */
   public void get (MatrixNd M) {
      M.setSize (rowSize(), colSize());
      M.setZero();
      for (int i=0; i<rowSize(); i++) {
         RowData row = myRows.get(i);
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            M.set (i, j, row.myValues[k]);
         }
      }
   }

   /**
    * Sets a single element of this matrix. The row and column indices must lie
    * within the current matrix dimenions. If the specified location does not
    * currently contain an explicit element, a new element is added.
    * Otherwise, the previous element is replaced.
    * 
    * @param i
    * element row index
    * @param j
    * element column index
    * @param val new element value
    */
   public void set (int i, int j, double val) {
      if (i >= rowSize() || j >= colSize()) {
         throw new ArrayIndexOutOfBoundsException ("(i,j)=("+i+","+j+")");
      }
      if (myRows.get(i).setValue (j, val)) {
         // value was added (instead of replacing old value)
         myNumVals++;
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
      // compute num non-zeros per row
      int[] nnz = new int[rowSize()];
      for (int ki=0; ki<2*numVals; ki+=2) {
         int i = indices[ki];
         if (i < 0 || i >= rowSize()) {
            throw new ArrayIndexOutOfBoundsException (
               "row index "+i+" out of bounds, offset "+ki);
         }
         nnz[i]++;
      }
      // clear entries in each row and set capacity
      for (int i=0; i<rowSize(); i++) {
         myRows.get(i).removeVals();
         myRows.get(i).ensureCapacity(nnz[i]);
      }
      // load the entries
      int ki = 0;
      for (int kv=0; kv<numVals; kv++) {
         int i = indices[ki++];
         int j = indices[ki++];
         if (j < 0 || j >= colSize()) {
            throw new ArrayIndexOutOfBoundsException (
               "row index  "+j+" out of bounds, offset "+ki);
         }
         myRows.get(i).setValue (j, values[kv]);
      }
      myNumVals = numVals;
      // not sorted if col indices not in order and row.setValue() doesn't sort
      // isSorted = false;
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
      else if (M instanceof SparseCRSMatrix) {
         set ((SparseCRSMatrix)M);
      }
      else {
         initRows (M.rowSize());
         myNumCols = M.colSize();
         int nvals = M.numNonZeroVals();

         int[] rowOffs = new int[rowSize()+1];
         int[] colIdxs = new int[nvals];
         double[] values = new double[nvals];

         M.getCRSIndices (colIdxs, rowOffs, Partition.Full, rowSize(), colSize());
         M.getCRSValues (values, Partition.Full, rowSize(), colSize());
         int numVals = rowOffs[rowSize()]-1;
         setCRSValues (
            values, colIdxs, rowOffs, numVals, rowSize(), Partition.Full);
      }
   }

   /**
    * Sets the values of this matrix from an existing SparseCRSMatrix M.
    *
    * @param M
    * matrix used to set values
    */
   public void set (SparseCRSMatrix M) {
      if (M == this) {
         return;
      }
      setRowSize (M.rowSize());
      for (int i=0; i<rowSize(); i++) {
         myRows.get(i).set (M.myRows.get(i));
      }
      myNumVals = M.myNumVals;
      myNumCols = M.myNumCols;
      isSorted = M.isSorted;
   }

   /**
    * Sets this matrix to the identity matrix. If this matrix matrix is not
    * square, then element (i,j) is set to 1 if i and j are equal, and 0
    * otherwise.
    *
    * <p>This method does not trim matrix capacity.
    */
   public void setIdentity() {
      int nvals = Math.min(rowSize(), colSize());
      for (int i=0; i<rowSize(); i++) {
         RowData row = myRows.get(i);
         row.removeVals();
         if (i < nvals) {
            row.setValue (i, 1);
         }
      }
      myNumVals = nvals;
      isSorted = true; // must be sorted by definition
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
      int nvals = Math.min(rowSize(), colSize());
      if (vals.length < nvals) {
         throw new IllegalArgumentException ("Insufficient values");
      }
      for (int i=0; i<rowSize(); i++) {
         RowData row = myRows.get(i);
         row.removeVals();
         if (i < nvals) {
            row.setValue (i, vals[i]);
         }
      }
      myNumVals = nvals;
      isSorted = true; // must be sorted by definition
   }

   /**
    * Sets the elements of this matrix to zero. This method
    * does not trim matrix capacity.
    */
   public void setZero() {
      for (RowData row : myRows) {
         row.removeVals();
      }
      myNumVals = 0;
      isSorted = true; // must be sorted by definition
   }

   /** 
    * Sets this matrix to have a random sparsity structure and values,
    * with between one and ten nonzero elements per row.
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
      for (RowData row : myRows) {
         for (int k=0; k<row.numVals(); k++) {
            row.myValues[k] = generator.nextDouble() * range + lower;
         }
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
      double[] x, double beta, SparseCRSMatrix B, int i,
      int[] cols, int mark, int[] marks, int coff) {

      RowData row = B.myRows.get(i);
      for (int k=0; k<row.numVals(); k++) {
         int j = row.myColIdxs[k];
         if (marks[j] < mark) {
            cols[coff++] = j;
            marks[j] = mark;
            x[j] = beta*row.myValues[k];
         }
         else {
            x[j] += beta*row.myValues[k];
         }
      }
      return coff;
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
   public void mul (SparseCRSMatrix M) {
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
   public void mul (SparseCRSMatrix M1, SparseCRSMatrix M2)
      throws ImproperSizeException {
      if (M1.colSize() != M2.rowSize()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      int initialCap = M1.numNonZeroVals() + M2.numNonZeroVals();
      ArrayList<RowData> resRows;
      if (M1 == this || M2 == this) {
         resRows = new ArrayList<>(M1.rowSize());
         for (int i=0; i<M1.rowSize(); i++) {
            resRows.add (new RowData());
         }
      }
      else {
         setRowSize (M1.rowSize());
         resRows = myRows;
      }

      double[] x = new double[M2.colSize()];
      int[] marks = new int[M2.colSize()];
      int[] cols = new int[M2.colSize()];

      int nvals = 0;
      for (int i=0; i<M1.rowSize(); i++) {
         RowData row = M1.myRows.get(i);
         int coff = 0;
         for (int k=0; k<row.numVals(); k++) {
            coff = accumulateScatterProduct (
               x, row.myValues[k], M2, row.myColIdxs[k], cols, i+1, marks, coff);
         }
         // Set row from the entries of x contained in cols[0,coff-1]. Note
         // that in the csparse version of this code, CRS entries do not need
         // to be sorted, so this can be done faster.
         RowData res = resRows.get(i);
         res.removeVals();
         res.ensureCapacity (coff);
         for (int k=0; k<coff; k++) {
            int j = cols[k];
            res.setValue (j, x[j]);
         }
         nvals += coff;
      }
      if (M1 == this || M2 == this) {
         myRows = resRows;
      }
      myNumVals = nvals;
      myNumCols = M2.colSize();
      // not sorted if we don't use row.setValue() to enforce column order
      // isSorted = false;
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
   public void mulTranspose (SparseCRSMatrix M) {
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
   public void mulTransposeRight (SparseCRSMatrix M1, SparseCRSMatrix M2)
      throws ImproperSizeException {

      if (M1.colSize() != M2.colSize()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseCRSMatrix MT = new SparseCRSMatrix(M2.colSize(), M2.rowSize());
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
   public void mulTransposeLeft (SparseCRSMatrix M1, SparseCRSMatrix M2)
      throws ImproperSizeException {

      if (M1.rowSize() != M2.rowSize()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseCRSMatrix MT = new SparseCRSMatrix(M1.colSize(), M1.rowSize());
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
   public void mulTransposeBoth (SparseCRSMatrix M1, SparseCRSMatrix M2)
      throws ImproperSizeException {

      if (M1.rowSize() != M2.colSize()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseCRSMatrix MT1 = new SparseCRSMatrix(M1.colSize(), M1.rowSize());
      MT1.transpose (M1);
      SparseCRSMatrix MT2 = new SparseCRSMatrix(M2.colSize(), M2.rowSize());
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
      if (i < 0 || i >= rowSize()) {
         throw new ArrayIndexOutOfBoundsException (
            "Row "+i+" is out of range");
      }
      double dot = 0;
      RowData row = myRows.get(i);
      for (int k=0; k<row.numVals(); k++) {
         dot += row.myValues[k]*row.myValues[k];
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
      if (i < 0 || i >= rowSize()) {
         throw new ArrayIndexOutOfBoundsException (
            "Row "+i+" is out of range");
      }
      double[] buf = v.getBuffer();
      double dot = 0;
      RowData row = myRows.get(i);
      for (int k=0; k<row.numVals(); k++) {
         dot += row.myValues[k]*buf[row.myColIdxs[k]];
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
      if (i < 0 || i >= rowSize()) {
         throw new ArrayIndexOutOfBoundsException (
            "Row "+i+" is out of range");
      }
      double[] buf = v.getBuffer();
      RowData row = myRows.get(i);
      for (int k=0; k<row.numVals(); k++) {
         buf[row.myColIdxs[k]] += s*row.myValues[k];
      }
   }

   /**
    * Adds this matrix to M and places the result in this matrix.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException if this matrix and M have different sizes.
    */
   public void add (SparseCRSMatrix M) {
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
   public void add (SparseCRSMatrix M1, SparseCRSMatrix M2) {
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
   public void scaledAdd (double s, SparseCRSMatrix M1, SparseCRSMatrix M2) {
      combine (s, M1, 1, M2);
   }

   /**
    * Subtracts M from this matrix.
    * 
    * @param M
    * matrix to subtract
    * @throws ImproperSizeException if this matrix and M have different sizes.
    */
   public void sub (SparseCRSMatrix M) {
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
   public void sub (SparseCRSMatrix M1, SparseCRSMatrix M2) {
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
      double a, SparseCRSMatrix M1, double b, SparseCRSMatrix M2) {
      if (M1.rowSize() != M2.rowSize() || M1.colSize() != M2.colSize()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      ArrayList<RowData> resRows;
      if (M1 == this || M2 == this) {
         resRows = new ArrayList<>(M1.rowSize());
         for (int i=0; i<M1.rowSize(); i++) {
            resRows.add (new RowData());
         }
      }
      else {
         setRowSize (M1.rowSize());
         resRows = myRows;
      }
      
      int[] cols = new int[M1.colSize()];
      double[] x = new double[M1.colSize()];
      int[] marks = new int[M1.colSize()];

      int nvals = 0;
      for (int i=0; i<M1.rowSize(); i++) {
         int coff;
         coff = accumulateScatterProduct (x, a, M1, i, cols, i+1, marks, 0);
         coff = accumulateScatterProduct (x, b, M2, i, cols, i+1, marks, coff);
         // Set row from the entries of x contained in cols[0,coff-1]. Note
         // that in the csparse version of this code, CRS entries do not need
         // to be sorted, so this can be done faster.
         RowData res = resRows.get(i);
         res.removeVals();
         res.ensureCapacity (coff);
         for (int k=0; k<coff; k++) {
            int j = cols[k];
            res.setValue (j, x[j]);
         }
         nvals += coff;
      }
      if (M1 == this || M2 == this) {
         myRows = resRows;
      }
      myNumVals = nvals;
      myNumCols = M2.colSize();
      // not sorted if we don't use row.setValue() to enforce column order
      //isSorted = false;
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      for (RowData row : myRows) {
         for (int k=0; k<row.numVals(); k++) {
            row.myValues[k] = -row.myValues[k];
         }
      }
   }

   /** 
    * Sets this matrix to the negative of another matrix M. This matrix is
    * resized if necessary.
    * 
    * @param M matrix to be 
    */
   public void negate (SparseCRSMatrix M) {
      if (M == this) {
         negate();
      }
      else {
         setRowSize (M.rowSize());
         for (int i=0; i<rowSize(); i++) {
            myRows.get(i).setScaled (-1, M.myRows.get(i));
         }
         myNumVals = M.myNumVals;
         myNumCols = M.myNumCols;
      }
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      for (RowData row : myRows) {
         for (int k=0; k<row.numVals(); k++) {
            row.myValues[k] *= s;
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
    */
   public void scale (double s, SparseCRSMatrix M) {
      if (M == this) {
         scale(s);
      }
      else {
         setRowSize (M.rowSize());
         for (int i=0; i<rowSize(); i++) {
            myRows.get(i).setScaled (s, M.myRows.get(i));
         }
         myNumCols = M.myNumCols;
         myNumVals = M.myNumVals;
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
   public void transpose (SparseCRSMatrix M) {

      // note that this works even if M == this

      int newColSize = M.rowSize();
      int newRowSize = M.colSize();
      ArrayList<RowData> newRows = new ArrayList<>(newRowSize);
      for (int i=0; i<newRowSize; i++) {
         newRows.add (new RowData());
      }
      for (int i=0; i<M.rowSize(); i++) {
         RowData row = M.myRows.get(i);
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            newRows.get(j).setValue (i, row.myValues[k]);
         }
      }
      myRows = newRows;
      myNumCols = newColSize;
      myNumVals = M.myNumVals;
      // will not be sorted if we don't use row.setValue() to enforce index order
      // isSorted = false;
   }

   /**
    * Computes the product
    * <pre>
    * MR = MS M1
    * </pre>
    * where {@code MS} is this matrix, and stores the result in {@code MR},
    * which is sized appropriately. If {@code MR == M1} and the required
    * size of {@code MR} does not equal that of {@code M1}, then an internal
    * copy of {@code M1} is made for computational purposes and {@code M1}
    * is resized as required.
    */
   public void mul (MatrixNd MR, MatrixNd M1) {
      if (colSize() != M1.rowSize()) {
         throw new ImproperSizeException (
            "M1 row size "+M1.rowSize()+" != sparse matrix col size "+colSize());
      }
      if (MR.rowSize() != rowSize() && MR == M1) {
         // make internal copy for computational purposes
         M1 = new MatrixNd (M1);
      }
      if (MR.colSize() != M1.colSize() || MR.rowSize() != rowSize()) {
         MR.setSize (rowSize(), M1.colSize());
      }

      int m = rowSize();
      int n = M1.colSize();
      int r = colSize();

      double[] buf1 = M1.getBuffer();
      int w1 = M1.getBufferWidth();
      int base1 = M1.getBufferBase();

      double[] bufr = MR.getBuffer();
      int wr = MR.getBufferWidth();
      int baser = MR.getBufferBase();

      double[] col1 = new double[r];
      double[] colr = new double[m];

      for (int j=0; j<n; j++) {
         // col1 = M1 (:,j)
         int off = j + base1;
         for (int i=0; i<r; i++) {
            col1[i] = buf1[off];
            off += w1;
         }

         // colr = MS * col1
         for (int i=0; i<m; i++) {
            RowData row = myRows.get(i);
            double sum = 0;
            for (int k=0; k<row.numVals(); k++) {
               sum += row.myValues[k]*col1[row.myColIdxs[k]];
            }
            colr[i] = sum;
         }

         // MR (:,j) = colr
         off = j + baser;
         for (int i=0; i<m; i++) {
            bufr[off] = colr[i];
            off += wr;
         }
      }
      
      
      // for (int i=0; i<rowSize(); i++) {
      //    RowData row = myRows.get(i);
      //    // clear sum for accumulating
      //    for (int j=0; j<n; j++) {
      //       sum[j] = 0;
      //    }
      //    // for each non-zero in row i of this matrix ...
      //    for (int k=0; k<row.numVals(); k++) {
      //       int j = row.myColIdxs[k];
      //       double v = row.myValues[k];
      //       int off1 = j*w1 + base1;
      //       // sum = v*M1(j,:)
      //       for (int l=0; l<n; l++) {
      //          sum[l] += v*buf1[off1+l];
      //       }
      //    }
      //    // store sum in row i of result
      //    int off = i*wr + baser;
      //    for (int j=0; j<n; j++) {
      //       bufr[off++] = sum[j];
      //    }
      // }
   }

   /**
    * Computes the product
    * <pre>
    * MR = M1 MS
    * </pre>
    * where {@code MS} is this matrix, and stores the result in {@code MR},
    * which is sized appropriately. If {@code MR == M1} and the required
    * size of {@code MR} does not equal that of {@code M1}, then an internal
    * copy of {@code M1} is made for computational purposes and {@code M1}
    * is resized as required.
    */
   public void mulLeft (MatrixNd MR, MatrixNd M1) {
      if (rowSize() != M1.colSize()) {
         throw new ImproperSizeException (
            "M1 col size "+M1.colSize()+" != sparse matrix row size "+rowSize());
      }
      if (MR.colSize() != colSize() && MR == M1) {
         // make internal copy for computational purposes
         M1 = new MatrixNd (M1);
      }
      if (MR.rowSize() != M1.rowSize() || MR.colSize() != colSize()) {
         MR.setSize (M1.rowSize(), colSize());
      }

      int m = M1.rowSize();
      int n = colSize();
      int r = rowSize();

      double[] buf1 = M1.getBuffer();
      int w1 = M1.getBufferWidth();
      int base1 = M1.getBufferBase();

      double[] bufr = MR.getBuffer();
      int wr = MR.getBufferWidth();
      int baser = MR.getBufferBase();

      double[] row1 = new double[r];
      double[] rowr = new double[n];

      for (int i=0; i<m; i++) {
         // row1 = M1 (i,:)
         int off = i*w1 + base1;
         for (int j=0; j<r; j++) {
            row1[j] = buf1[off++];
         }

         // rowr = row1 * this
         for (int j=0; j<n; j++) {
            rowr[j] = 0;
         }
         for (int l=0; l<rowSize(); l++) {
            RowData row = myRows.get(l);
            for (int k=0; k<row.numVals(); k++) {
               int j = row.myColIdxs[k];
               rowr[j] += row.myValues[k]*row1[l];
            }
         }

         // MR (i,:) = rowr
         off = i*wr + baser;
         for (int j=0; j<n; j++) {
            bufr[off++] = rowr[j];
         }
      }
   }

   /**
    * Computes the product
    * <pre>
    * MR = MS M1^T
    * </pre>
    * 
    * here {@code MS} is this matrix, and stores the result in {@code MR},
    * which will be sized appropriately. If {@code MR == M1}, then an internal
    * copy will be made of {@code M1} for computational purposes
    * and {@code M1} will be resized as needed to store the result.
    */
   public void mulTransposeRight (MatrixNd MR, MatrixNd M1) {
      if (colSize() != M1.colSize()) {
         throw new ImproperSizeException (
            "M1 col size "+M1.colSize()+" != sparse matrix col size "+colSize());
      }
      if (MR == M1) {
         // internal copy for computational purposes
         M1 = new MatrixNd (M1);
      }
      if (MR.colSize() != M1.rowSize() || MR.rowSize() != rowSize()) {
         MR.setSize (rowSize(), M1.rowSize());
      }

      int m = rowSize();
      int n = M1.rowSize();
      int r = colSize();

      double[] buf1 = M1.getBuffer();
      int w1 = M1.getBufferWidth();
      int base1 = M1.getBufferBase();

      double[] bufr = MR.getBuffer();
      int wr = MR.getBufferWidth();
      int baser = MR.getBufferBase();

      double[] col1 = new double[r];
      double[] colr = new double[m];

      for (int j=0; j<n; j++) {
         // col1 = M1 (j,:)
         int off = j*w1 + base1;
         for (int i=0; i<r; i++) {
            col1[i] = buf1[off++];
         }

         // colr = MS * col1
         for (int i=0; i<m; i++) {
            RowData row = myRows.get(i);
            double sum = 0;
            for (int k=0; k<row.numVals(); k++) {
               sum += row.myValues[k]*col1[row.myColIdxs[k]];
            }
            colr[i] = sum;
         }

         // MR (:,j) = colr
         off = j + baser;
         for (int i=0; i<m; i++) {
            bufr[off] = colr[i];
            off += wr;
         }
      }
   }

   protected void mulVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {
      int rowf = r0+nr;
      int colf = c0+nc;
      for (int i=r0; i<rowf; i++) {
         RowData row = myRows.get(i);
         double sum = 0;
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j >= colf) {
               break;
            }
            if (j >= c0) {
               sum += row.myValues[k]*vec[j-c0];
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
         RowData row = myRows.get(i);
         double sum = 0;
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j >= colf) {
               break;
            }
            if (j >= c0) {
               sum += row.myValues[k]*vec[j-c0];
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
         RowData row = myRows.get(i);
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j >= rowf) {
               break;
            }
            if (j >= r0) {
               res[j-r0] += row.myValues[k]*vec[i-c0];
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
      StringBuffer buf = new StringBuffer (20 * myNumVals);
      if (!isSorted) {
         sortMatrix();
      }
      for (int i=0; i<rowSize(); i++) {
         RowData row = myRows.get(i);
         for (int k=0; k<row.numVals(); k++) {
            buf.append (
               "("+i+" "+row.myColIdxs[k]+" "+fmt.format(row.myValues[k])+")\n");
         }
      }
      return buf.toString();
   }

   private void recomputeNumVals() {
      int nvals = 0;
      for (RowData row : myRows) {
         nvals += row.numVals();
      }
      myNumVals = nvals;
   }

   public int numNonZeroVals() {
      if (myNumVals == -1) {
         recomputeNumVals();
      }
      return myNumVals;
   }

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
               return myNumVals;
            }
            for (int i=0; i<numRows; i++) {
               RowData row = myRows.get(i);
               for (int k=0; k<row.numVals(); k++) {
                  if (row.myColIdxs[k] >= numCols) {
                     break;
                  }
                  cnt++;
               }
            }
            break;
         }
         case UpperTriangular: {
            for (int i=0; i<numRows; i++) {
               RowData row = myRows.get(i);
               int k0 = row.getInsertIndex (i);
               for (int k=k0; k<row.numVals(); k++) {
                  if (row.myColIdxs[k] >= numCols) {
                     break;
                  }
                  cnt++;
               }
            }
            break;
         }
         case LowerTriangular: {
            for (int i=0; i<numRows; i++) {
               RowData row = myRows.get(i);
               int maxj = Math.min (i, numCols-1);
               for (int k=0; k<row.numVals(); k++) {
                  if (row.myColIdxs[k] > maxj) {
                     break;
                  }
                  cnt++;
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

      for (int i=0; i<rowSize(); i++) {
         RowData row = myRows.get(i);
         if (i<numRows) {
            int off0 = rowOffs[i]-1;
            int off1 = (i+1 < numRows ? rowOffs[i+1]-1 : numVals);
            if (off0 > off1) {
               throw new IllegalArgumentException (
                  "rowOffs "+i+","+(i+1)+" = "+(off0+1)+","+(off1+1));
            }
            row.removeVals();
            row.ensureCapacity (off1-off0);
            for (int k=off0; k<off1; k++) {
               int j = colIdxs[k]-1;
               if (j >= colSize()) {
                  throw new ArrayIndexOutOfBoundsException (
                     "Column "+j+" in row "+i+" is out of range");
               }
               row.setValue (j, vals[k]);
            }
         }
         else {
            row.removeVals();
         }
      }
      myNumVals = numVals;
      // not sorted if col indices not in order and row.setValue() doesn't sort
      // isSorted = false;
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
      if (rowOffs.length < (numRows+1)) {
         throw new IllegalArgumentException (
            "rowOffs must have length >= numRows+1");
      }
      if (!isSorted) {
         sortMatrix();
      }
      int off = 0;
      int j;
      for (int i=0; i<numRows; i++) {
         RowData row = myRows.get(i);
         rowOffs[i] = off+1;
         int k = 0;
         if (part == Partition.UpperTriangular) {
            k = row.getInsertIndex(i);
         }
         while (k<row.numVals() && (j=row.myColIdxs[k]) < numCols) {
            colIdxs[off++] = j+1;
            k++;
         }
      }
      rowOffs[numRows] = off+1;
      return off;
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
      int off = 0;
      for (int i=0; i<numRows; i++) {
         RowData row = myRows.get(i);
         int k = 0;
         if (part == Partition.UpperTriangular) {
            k = row.getInsertIndex(i);
         }
         while (k<row.numVals() && row.myColIdxs[k] < numCols) {
            vals[off++] = row.myValues[k++];
         }
      }
      return off;
   }

   private int getColOffsets (
      int[] colOffs, Partition part, int numRows, int numCols) {
      
      for (int j=0; j<numCols; j++) {
         colOffs[j] = 0;
      }
      // accumulate number of non-zeros per col in colOffs
      for (int i=0; i<numRows; i++) {
         RowData row = myRows.get(i);
         int maxj = numCols-1;
         if (part == Partition.LowerTriangular) {
            maxj = Math.min (i, maxj);
         }
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j > maxj) {
               break;
            }
            colOffs[j]++;
         }
      }
      // convert number of non-zeros to column offsets
      int totalNnz = 0;
      for (int j=0; j<numCols; j++) {
         int nnz = colOffs[j];
         colOffs[j] = totalNnz;
         totalNnz += nnz;
      }
      return totalNnz;
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
      if (colOffs.length < (numCols+1)) {
         throw new IllegalArgumentException (
            "colOffs must have length >= " + (numCols+1));
      }
      if (!isSorted) {
         sortMatrix();
      }
      int[] locOffs = new int[numCols];
      int nnz = getColOffsets (colOffs, part, numRows, numCols);

      for (int j=0; j<numCols; j++) {
         locOffs[j] = colOffs[j];
      }
      for (int i=0; i<numRows; i++) {
         RowData row = myRows.get(i);
         int maxj = numCols-1;
         if (part == Partition.LowerTriangular) {
            maxj = Math.min (i, maxj);
         }
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j > maxj) {
               break;
            }
            rowIdxs[locOffs[j]++] = i+1;
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
      int nnz = getColOffsets (locOffs, part, numRows, numCols);

      for (int i=0; i<numRows; i++) {
         RowData row = myRows.get(i);
         int maxj = numCols-1;
         if (part == Partition.LowerTriangular) {
            maxj = Math.min (i, maxj);
         }
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j > maxj) {
               break;
            }
            vals[locOffs[j]++] = row.myValues[k];
         }
      }
      return nnz;
   }

   /** 
    * {@inheritDoc}
    */
   public void checkConsistency () {
      int numVals = 0;
      for (int i=0; i<rowSize(); i++) {
         RowData row = myRows.get(i);
         if (row.myColIdxs.length < row.numVals()) {
            throw new TestException (
               "row "+i+": column index capacity less than number of values");
         }
         if (row.myValues.length < row.numVals()) {
            throw new TestException (
               "row "+i+": value capacity less than number of values");
         }
         int lastj = -1;
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs[k];
            if (j <= lastj) {
                throw new TestException (
                   "row "+i+": column indices not ascending");
            }
            if (j >= colSize()) {
                throw new TestException (
                   "row "+i+": column index "+j+" out of range");
            }
            lastj = j;
         }
         numVals += row.numVals();
      }
      if (numVals != myNumVals) {
         throw new TestException (
            "cached number of values "+myNumVals+" != actual number "+numVals);
      }
   }
   
   public SparseCRSMatrix clone() {
      SparseCRSMatrix S;
      try {
         S = (SparseCRSMatrix)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "Cannot clone SparseCRSMatrix");
      }
      S.myRows = new ArrayList<>();
      for (RowData row : myRows) {
         S.myRows.add (new RowData(row));
      }
      return S;
   }

   @Override
   public double maxNorm() {
      double m = 0;
      for (RowData row : myRows) {
         for (int k=0; k<row.numVals(); k++) {
            double a = Math.abs(row.myValues[k]);
            if (a > m) {
               m = a;
            }
         }
      }
      return m;
   }

   public void zeroRow(int i) {
      myRows.get(i).removeVals();
   }
   
   public void zeroColumn(int j) {
      for (int i=0; i<rowSize(); i++) {
         myRows.get(i).setValue (j, 0);
      }
   }
   
   public void add (int i, int j, double v) {
      if (myRows.get(i).addValue (j, v)) {
         // value was added (instead of replacing old value)
         myNumVals++;
      }
   }   

   public void removeRow (int idx) {
      if (idx < 0 || idx >= rowSize()) {
         throw new IllegalArgumentException (
            "Row index " + idx +
            " not in range [0,"+(rowSize()-1)+"]");
      }
      RowData row = myRows.remove(idx);
      myNumVals -= row.numVals();
   }
         
   public void removeRows (int[] idxs) {
      idxs = ArraySupport.sortIndexList (idxs, rowSize());
      for (int k=0; k<idxs.length; k++) {
         myNumVals -= myRows.get(idxs[k]).numVals();
      }
      ArraySupport.removeListItems (myRows, idxs);
   }

   public void addRow (int[] colIdxs, double[] values, int numVals) {
      int lastj = -1;
      boolean ascending = true;
      for (int k=0; k<numVals; k++) {
         int j = colIdxs[k];
         if (j < 0 || j >= colSize()) {
            throw new IllegalArgumentException (
               "Column index " + j +
               " not in range [0,"+(colSize()-1)+"]");
         }
         if (j <= lastj) {
            ascending = false;
         }
         lastj = j;
      }
      if (!ascending) {
         colIdxs = Arrays.copyOf (colIdxs, numVals);
         ArraySort.sort (colIdxs);         
         for (int k=0; k<numVals-1; k++) {
            if (colIdxs[k] == colIdxs[k+1]) {
               throw new IllegalArgumentException (
                  "Column index "+colIdxs[k]+" is repeated");
            }
         }
      }
      RowData row = new RowData (colIdxs, values, numVals);
      myRows.add (row);
      myNumVals += numVals;
   }   

   public void addRow (DynamicIntArray colIdxs, DynamicDoubleArray values) {
      if (colIdxs.size() != values.size()) {
         throw new IllegalArgumentException (
            "colIdxs and values must have the same size");
      }
      addRow (colIdxs.getArray(), values.getArray(), values.size());
   }

}
