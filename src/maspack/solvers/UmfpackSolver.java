/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.fileutil.NativeLibraryManager;
import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;
import maspack.util.*;

import java.io.*;

public class UmfpackSolver implements DirectSolver {

   private static final int INIT_UNKNOWN = 0;
   private static final int INIT_OK = 2;
   private static final int ERR_CANT_LOAD_LIBRARIES = -1000;

   private static int myInitStatus = INIT_UNKNOWN;

   private static int UMFPACK_A = 0, /* Ax=b */
   UMFPACK_At = 1, /* A'x=b */
   UMFPACK_Aat = 2, /* A.'x=b */

   UMFPACK_Pt_L = 3, /* P'Lx=b */
   UMFPACK_L = 4, /* Lx=b */
   UMFPACK_Lt_P = 5, /* L'Px=b */
   UMFPACK_Lat_P = 6, /* L.'Px=b */
   UMFPACK_Lt = 7, /* L'x=b */
   UMFPACK_Lat = 8, /* L.'x=b */

   UMFPACK_U_Qt = 9, /* UQ'x=b */
   UMFPACK_U = 10, /* Ux=b */
   UMFPACK_Q_Ut = 11, /* QU'x=b */
   UMFPACK_Q_Uat = 12, /* QU.'x=b */
   UMFPACK_Ut = 13, /* U'x=b */
   UMFPACK_Uat = 14; /* U.'x=b */

   public static int UMFPACK_OK = 0,

   /* status > 0 means a warning, but the method was successful anyway. */
   /* A Symbolic or Numeric object was still created. */
   UMFPACK_WARNING_singular_matrix = 1,

   /* The following warnings were added in umfpack_*_get_determinant */
   UMFPACK_WARNING_determinant_underflow = 2,
   UMFPACK_WARNING_determinant_overflow = 3,

   /* status < 0 means an error, and the method was not successful. */
   /* No Symbolic of Numeric object was created. */
   UMFPACK_ERROR_out_of_memory = -1, UMFPACK_ERROR_invalid_Numeric_object = -3,
   UMFPACK_ERROR_invalid_Symbolic_object = -4,
   UMFPACK_ERROR_argument_missing = -5, UMFPACK_ERROR_n_nonpositive = -6,
   UMFPACK_ERROR_invalid_matrix = -8, UMFPACK_ERROR_different_pattern = -11,
   UMFPACK_ERROR_invalid_system = -13, UMFPACK_ERROR_invalid_permutation = -15,
   UMFPACK_ERROR_internal_error = -911, UMFPACK_ERROR_file_IO = -17;

   private int[] decIndices(int[] indices) {
      for (int i=0; i<indices.length; i++) {
         indices[i]--;
      }
      return indices;
   }
   
   private native int umfpack_di_symbolic (
      int n_row, int n_col, int[] Ap, int[] Ai, double[] Ax, long[] Symbolic,
      double[] Control, double[] Info);

   private native int umfpack_di_numeric (
      int[] Ap, int[] Ai, double[] Ax, long[] Symbolic, long[] Numeric,
      double[] Control, double[] Info);

   private native int umfpack_di_solve (
      int sys, int[] Ap, int[] Ai, double[] Ax, double[] X, double[] B,
      long[] Numeric, double[] Control, double[] Info);

   private native void umfpack_di_free_symbolic (long[] Sym);

   private native void umfpack_di_free_numeric (long[] Num);

   private native void umfpack_di_defaults (double[] Control);

   long[] symbolic = new long[] { 0 };

   long[] numeric = new long[] { 0 };

   // int[] Ap = null, Ai = null;
   // double[] Ax = null;
   // int mSize, mNumVals;


   private static void doLoadLibraries() {
      try {
         NativeLibraryManager.setFlags (NativeLibraryManager.VERBOSE);
         NativeLibraryManager.load ("UmfpackJNI");
         myInitStatus = INIT_OK;
      }
      catch (UnsatisfiedLinkError e) {
         System.err.println (e.getMessage());
         myInitStatus = ERR_CANT_LOAD_LIBRARIES;
      }
   }

   public UmfpackSolver() {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }
      if (myInitStatus < 0) {
         throw new UnsupportedOperationException (
            "Umfpack not available: " + getInitErrorMessage());
      }
   }

   public static boolean isAvailable () {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }     
      return myInitStatus < 0 ? false : true;
   }

   public static String getInitErrorMessage() {
      if (myInitStatus < 0) {
         return "can't load native libraries";
      }
      else {
         return null;
      }
   }

   public void finalize() {
      dispose();
   }

   private void freeNumeric() {
      if (numeric[0] != 0) {
         umfpack_di_free_numeric (numeric);
         numeric[0] = 0;
      }
   }

   private void freeSymbolic() {
      if (symbolic[0] != 0) {
         umfpack_di_free_symbolic (symbolic);
         symbolic[0] = 0;
      }
   }

   private void free() {
      freeSymbolic();
      freeNumeric();
   }

   public void analyze (Matrix M, int size, int type) {
      if (M instanceof maspack.matrix.SparseBlockMatrix) {
         numVals =
            ((SparseBlockMatrix)M).numNonZeroVals (Partition.Full, size, size);
      }
      else if (M instanceof maspack.matrix.SparseMatrixNd) {
         numVals = ((SparseMatrixNd)M).numNonZeroVals();
      }
      else {
         numVals = size * size;
      }
      free();

      subsetSize = size;
      matrix = M;

      myRowIdxs = new int[numVals];
      myColOffs = new int[size + 1];
      M.getCCSIndices (
         myRowIdxs, myColOffs, SparseBlockMatrix.Partition.Full, size, size);
      // decrement indices because Umfpack seems to want 0-based indices
      decIndices(myRowIdxs);
      decIndices(myColOffs);
      int status =
         umfpack_di_symbolic (
            size, size, myColOffs, myRowIdxs, null, symbolic, null, null);
      if (status != UMFPACK_OK) {
         matrix = null;
         freeSymbolic();
         throw new IllegalArgumentException ("Matrix could not be set");
      }
   }

   public int analyze (int[] colOffs, int[] rowIdxs, int size, int numVals) {
      matrix = null;
      myRowIdxs = new int[numVals];
      myVals = new double[numVals];
      myColOffs = new int[size + 1];
      for (int i = 0; i < size; i++) {
         myColOffs[i] = colOffs[i];
      }
      myColOffs[size] = numVals;
      for (int i = 0; i < numVals; i++) {
         myRowIdxs[i] = rowIdxs[i];
      }
      int status =
         umfpack_di_symbolic (
            size, size, myColOffs, myRowIdxs, null, symbolic, null, null);
      if (status != UMFPACK_OK) {
         freeSymbolic();
      }
      return status;
   }

   public int factor (double[] vals) {
      freeNumeric();
      for (int i = 0; i < myVals.length; i++) {
         myVals[i] = vals[i];
      }
      return umfpack_di_numeric (
         myColOffs, myRowIdxs, myVals, symbolic, numeric, null, null);
   }

   public void factor() {
      if (matrix != null) {
         freeNumeric();

         myVals = new double[numVals];
         matrix.getCCSValues (
            myVals, SparseBlockMatrix.Partition.Full, subsetSize, subsetSize);

         int status =
            umfpack_di_numeric (
               myColOffs, myRowIdxs, myVals, symbolic, numeric, null, null);
         if (status < 0) {
            throw new IllegalArgumentException ("Matrix could not be factored");
         }
         else if (status == UMFPACK_WARNING_singular_matrix) {
            System.out.println ("Matrix is near singular, solve could fail");
         }
      }
   }

   public void analyzeAndFactor (Matrix M) {
      analyze (M, M.colSize(), 0);
      factor();
   }

   public void solve (VectorNd x, VectorNd b) {
      umfpack_di_solve (
         UMFPACK_A, myColOffs, myRowIdxs, myVals,
         x.getBuffer(), b.getBuffer(), numeric, null, null);
   }

   public void autoFactorAndSolve (VectorNd x, VectorNd b, int tolExp) {
      factor();
      solve (x, b);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasAutoIterativeSolving() {
      return false;
   }

   public void solve (double[] x, double[] b) {
      umfpack_di_solve (
         UMFPACK_A, myColOffs, myRowIdxs, myVals, x, b, numeric, null, null);
   }

   Matrix matrix = null;

   int subsetSize, numVals;

   int[] myRowIdxs, myColOffs;

   double[] myVals;

   public static void main (String[] args) {
      if (args.length != 1) {
         System.out.println ("Usage: <matrixAndVectorFileName>");
      }
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (args[0])));
         SparseMatrixNd M = new SparseMatrixNd (0, 0);
         VectorNd b = new VectorNd();
         M.scan (rtok);
         b.scan (rtok);

         int size = M.rowSize();
         UmfpackSolver solver = new UmfpackSolver();

         if (M.rowSize() != M.colSize()) {
            System.out.println ("Error: matrix is not square");
            System.exit (1);
         }
         if (b.size() != size) {
            System.out.println ("Error: matrix and b have different sizes");
         }
         System.out.println ("Scanned system, size=" + size);
         if (M.isSymmetric (0)) {
            System.out.println ("symmetric matrix");
            solver.analyze (M, size, Matrix.SYMMETRIC);
         }
         else {
            System.out.println ("indefinite matrix");
            solver.analyze (M, size, Matrix.INDEFINITE);
         }
         VectorNd x = new VectorNd (size);
         VectorNd bcheck = new VectorNd (size);
         solver.factor();
         solver.solve (x, b);
         M.mul (bcheck, x);
         bcheck.sub (b);
         for (int i = 0; i < size; i++) {
            System.out.println (x.get (i));
         }
         System.out.println ("residual=" + bcheck.infinityNorm());
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
   }

   public void dispose() {
      free();
   }
}
