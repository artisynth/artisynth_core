/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.*;
import java.util.*;

import maspack.util.NumberFormat;

/**
 * Base implementation of {@link maspack.matrix.Matrix Matrix}.
 */
public abstract class SparseMatrixBase extends MatrixBase
   implements SparseMatrix {

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M == this) {
         return;
      }
      if (M.rowSize() != rowSize() || M.colSize() != colSize()) {
         if (isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            setSize (M.rowSize(), M.colSize());
         }
      }
      int nrows = rowSize();
      int ncols = colSize();
      int nvals = M.numNonZeroVals();
      int[] rowOffs = new int[nrows+1];
      int[] cols = new int[nvals];
      double[] vals = new double[nvals];
      M.getCRSIndices (cols, rowOffs, Partition.Full, nrows, ncols);
      M.getCRSValues (vals, Partition.Full, nrows, ncols);
      setCRSValues (vals, cols, rowOffs, nvals, nrows, Partition.Full);
   }

   /**
    * {@inheritDoc}
    */
   public void setCRSValues (
      double[] vals, int[] colIdxs, int[] rowOffs, int nvals, int nrows,
      Partition part) {

      checkSetCRSValuesArgs (vals, colIdxs, rowOffs, nvals, nrows, part);

      int ncols = colSize();      
      int[] indices;
      double[] values;
      int nnz;

      if (part == Partition.UpperTriangular) {
         int[] fullOffs = new int[nrows];

         for (int i=0; i<nrows; i++) {
            int lastk = (i < nrows-1 ? rowOffs[i+1]-1 : nvals);
            for (int off=rowOffs[i]-1; off<lastk; off++) {
               int j = colIdxs[off]-1;
               if (j >= ncols) {
                  throw new ArrayIndexOutOfBoundsException (
                     "Column "+j+" in row "+i+" is out of range");
               }
               if (j >= i) {
                  fullOffs[i]++;
                  if (i != j && j < nrows) {
                     fullOffs[j]++;
                  }
               }
            }
         }
         nnz = countsToOffsets (fullOffs, fullOffs, nrows);
         indices = new int[2*nnz];
         values = new double[nnz];
         for (int i=0; i<nrows; i++) {
            int lastk = (i < nrows-1 ? rowOffs[i+1]-1 : nvals);
            for (int off=rowOffs[i]-1; off<lastk; off++) {
               int j = colIdxs[off]-1;
               if (j >= i) {
                  int k = fullOffs[i]++;
                  indices[k*2  ] = i;
                  indices[k*2+1] = j;
                  values[k] = vals[off];
                  if (i != j && j < nrows) {
                     k = fullOffs[j]++;
                     indices[k*2  ] = j;
                     indices[k*2+1] = i;
                     values[k] = vals[off];
                  }
               }
            }
         }
      }
      else {
         indices = new int[2*nvals];
         values = vals;
         nnz = nvals;
         int k = 0;
         for (int i=0; i<nrows; i++) {
            int lastk = (i < nrows-1 ? rowOffs[i+1]-1 : nvals);
            for (int off=rowOffs[i]-1; off<lastk; off++) {
               int j = colIdxs[off]-1;
               if (j >= ncols) {
                  throw new ArrayIndexOutOfBoundsException (
                     "Column "+j+" in row "+i+" is out of range");
               }
               indices[k*2  ] = i;
               indices[k*2+1] = j;
               k++;
            }
         }
      }
      set (values, indices, nnz);
   }


   /**
    * {@inheritDoc}
    */
   public void setCCSValues (
      double[] vals, int[] rowIdxs, int[] colOffs, int nvals, int ncols,
      Partition part) {

      checkSetCCSValuesArgs (vals, rowIdxs, colOffs, nvals, ncols, part);

      int nrows = rowSize();      
      int[] indices;
      double[] values;
      int nnz;

      if (part == Partition.LowerTriangular) {
         int[] fullOffs = new int[nrows];

         for (int j=0; j<ncols; j++) {
            int lastk = (j < ncols-1 ? colOffs[j+1]-1 : nvals);
            for (int off=colOffs[j]-1; off<lastk; off++) {
               int i = rowIdxs[off]-1;
               if (i >= nrows) {
                  throw new ArrayIndexOutOfBoundsException (
                     "Row "+i+" in column "+j+" is out of range");
               }
               if (i >= j) {
                  fullOffs[i]++;
                  if (j != i && i < ncols) {
                     fullOffs[j]++;
                  }
               }
            }
         }
         nnz = countsToOffsets (fullOffs, fullOffs, nrows);
         indices = new int[2*nnz];
         values = new double[nnz];
         for (int j=0; j<ncols; j++) {
            int lastk = (j < ncols-1 ? colOffs[j+1]-1 : nvals);
            for (int off=colOffs[j]-1; off<lastk; off++) {
               int i = rowIdxs[off]-1;
               if (i >= j) {
                  int k = fullOffs[i]++;
                  indices[k*2  ] = i;
                  indices[k*2+1] = j;
                  values[k] = vals[off];
                  if (j != i && i < ncols) {
                     k = fullOffs[j]++;
                     indices[k*2  ] = j;
                     indices[k*2+1] = i;
                     values[k] = vals[off];
                  }
               }
            }
         }
      }
      else {
         indices = new int[2*nvals];
         values = vals;
         nnz = nvals;
         int k = 0;
         for (int j=0; j<ncols; j++) {
            int lastk = (j < ncols-1 ? colOffs[j+1]-1 : nvals);
            for (int off=colOffs[j]-1; off<lastk; off++) {
               int i = rowIdxs[off]-1;
               if (i >= nrows) {
                  throw new ArrayIndexOutOfBoundsException (
                     "Row "+i+" in column "+j+" is out of range");
               }
               indices[k*2  ] = i;
               indices[k*2+1] = j;
               k++;
            }
         }
      }
      set (values, indices, nnz);
   }


   /** 
    * Sets this matrix to have a random sparsity structure and values,
    * with between one and four nonzero elements per row.
    *
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    */
   public void setRandom (
      double lower, double upper, Random generator) {
      setRandom (lower, upper, generator, /*symmetric=*/false);
   }

   /** 
    * Sets this matrix to have a random sparsity structure and values,
    * with between one and four nonzero elements per row.
    *
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    * @param symmetric
    * ensure that the resulting structure and values are symmetric.
    * Ignored if the matrix is not square.
    */
   public void setRandom (
      double lower, double upper, Random generator, boolean symmetric) {

      int ncols = colSize();
      int nrows = rowSize();
      int maxPerRow = Math.min (4, ncols);
      int nvals = 0;
      int[] nonZerosPerRow = new int[nrows];
      for (int i=0; i<nrows; i++) {
         nonZerosPerRow[i] = generator.nextInt(maxPerRow)+1;
         nvals += nonZerosPerRow[i];
      }
      int[] indices;
      double[] values;
      if (symmetric) { 
         // allocate extra space to allow for symmetrical entries
         indices = new int[4*nvals];
         values = new double[2*nvals];
      }
      else {
         indices = new int[2*nvals];
         values = new double[nvals];
      }
      if (nrows != ncols) {
         symmetric = false;
      }

      double range = upper-lower;
      HashSet<Integer> colset = new HashSet<Integer>();           
      int k = 0;
      for (int i=0; i<nrows; i++) {
         colset.clear();
         int numNonZeros = nonZerosPerRow[i];
         for (int cnt=0; cnt<numNonZeros; cnt++) {
            int j = 0;
            do {
               j = generator.nextInt(ncols);
            }
            while (colset.contains(j));
            colset.add (j);
         }
         LinkedList<Integer> collist = new LinkedList<Integer>();
         collist.addAll (colset);
         Collections.sort (collist);
         for (int j : collist) {
            double value = generator.nextDouble()*range + lower;
            if (symmetric) {
               if (j >= i) {
                  indices[2*k  ] = i;
                  indices[2*k+1] = j;
                  values[k++] = value;
                  if (j > i) {
                     indices[2*k  ] = j;
                     indices[2*k+1] = i;
                     values[k++] = value;
                  }
               }
            }
            else {
               indices[2*k  ] = i;
               indices[2*k+1] = j;
               values[k++] = value;
            }
         }
      }
      set (values, indices, k);
   }   

   /** 
    * Create row (or column) offsets from the count of nonzero
    * entries in each row (or column).
    * 
    * @param offs computed offset values
    * @param cnts number of nonzero entries for row (or column)
    * @param n number of rows (or columns)
    * @return total number of non-zero entries
    */
   protected int countsToOffsets (int[] offs, int[] cnts, int n) {
      int nnz = 0;
      for (int j=0; j<n; j++) {
         int cnt = cnts[j];
         offs[j] = nnz;
         nnz += cnt;
      }
      return nnz;
   }

   public void writeToFileCRS (String fileName, String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      try {
         PrintWriter pw =
            new PrintWriter (new BufferedWriter (new FileWriter (fileName)));
         write (pw, fmt, WriteFormat.CRS);
         pw.close();
      }
      catch (Exception e) {
         System.out.println ("Error writing matrix to file "+ fileName + ":");
         System.out.println (e);
      }
   }

   // /**
   //  * {@inheritDoc}
   //  */
   // public void mulSubMatrix (
   //    VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

   //    mulCheckArgs (null, v1, r0, nr, c0, nc);
   //    if (vr.size != nr && vr != v1) {
   //       vr.resetSize (nr);
   //    }
   //    double[] res = ((vr == v1) ? new double[nr] : vr.buf);
   //    mulVec (res, v1.buf, r0, nr, c0, nc);
   //    if (vr.size != nr) {
   //       vr.resetSize (nr); // reset size if needed when vr == v1
   //    }
   //    if (v1 == vr) {
   //       double[] buf = vr.buf;
   //       for (int i=0; i<nr; i++) {
   //          buf[i] = res[i];
   //       }
   //    }
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public void mulAddSubMatrix (
   //    VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

   //    mulCheckArgs (vr, v1, r0, nr, c0, nc);
   //    if (v1 == vr) {
   //       double[] res = new double[nr];
   //       mulVec (res, v1.buf, r0, nr, c0, nc);
   //       double[] buf = vr.buf;
   //       for (int i=0; i<nr; i++) {
   //          buf[i] += res[i];
   //       }
   //    }
   //    else {
   //       mulAddVec (vr.buf, v1.buf, r0, nr, c0, nc);
   //    }
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public void mulSubMatrixTranspose (
   //    VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {

   //    mulTransposeCheckArgs (null, v1, r0, nr, c0, nc);
   //    if (vr.size != nr && vr != v1) {
   //       vr.resetSize (nr);
   //    }
   //    double[] res = ((vr == v1) ? new double[nr] : vr.buf);
   //    mulTransposeVec (res, v1.buf, r0, nr, c0, nc);
   //    if (vr.size != nr) {
   //       vr.resetSize (nr); // reset size if needed when vr == v1
   //    }
   //    if (v1 == vr) {
   //       double[] buf = vr.buf;
   //       for (int i=0; i<nr; i++) {
   //          buf[i] = res[i];
   //       }
   //    }
   // }


   // /** 
   //  * Multiplies a submatrix of this matrix by the data in <code>vec</code> and
   //  * places the result in <code>res</code>. The submatrix is specified by the
   //  * <code>nr</code> rows and <code>nc</code> columns of this matrix,
   //  * beginning at <code>r0</code> and <code>c0</code>, respectively.
   //  *
   //  * <p>
   //  * It is assumed that <code>res</code> and <code>vec</code>
   //  * are different, and that all submatrix dimensions are compatible
   //  * with the dimensions of this matrix.
   //  */
   // protected abstract void mulVec (
   //    double[] res, double[] vec, int r0, int nr, int c0, int nc);

   // /** 
   //  * Multiplies a submatrix of this matrix by the data in <code>vec</code> and
   //  * adds the result to <code>res</code>. The submatrix is specified by the
   //  * <code>nr</code> rows and <code>nc</code> columns of tis matrix, beginning at
   //  * <code>r0</code> and <code>c0</code>, respectively.
   //  *
   //  * <p>
   //  * It is assumed that <code>res</code> and <code>vec</code>
   //  * are different, and that all submatrix dimensions are compatible
   //  * with the dimensions of this matrix.
   //  */
   // protected abstract void mulAddVec (
   //    double[] res, double[] vec, int r0, int nr, int c0, int nc);

   // /** 
   //  * Multiplies a submatrix of the transpose of this matrix by the data in
   //  * <code>vec</code> and places the result in <code>res</code>. The submatrix
   //  * is specified by the <code>nr</code> rows and <code>nc</code> columns of
   //  * the transpose of this matrix, beginning at <code>r0</code> and
   //  * <code>c0</code>, respectively.
   //  *
   //  * <p>
   //  * It is assumed that <code>res</code> and <code>vec</code>
   //  * are different, and that all submatrix dimensions are compatible
   //  * with the dimensions of this matrix.
   //  */
   // protected abstract void mulTransposeVec (
   //    double[] res, double[] vec, int r0, int nr, int c0, int nc);

}
