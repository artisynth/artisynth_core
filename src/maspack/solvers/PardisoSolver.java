/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.fileutil.NativeLibraryException;
import maspack.fileutil.NativeLibraryManager;
import maspack.fileutil.NativeLibraryManager.SystemType;
import maspack.matrix.Matrix.Partition;
import maspack.matrix.*;
import maspack.util.*;

import java.io.*;
import java.util.*;

/**
 * JNI interface to the Pardiso sparse solver. Usage of Pardiso
 * is usually dividing into three phases:
 *
 * <ul>
 * <li>An <i>analyze</i> phase that reorder the matrix to reduce
 * fill-in and performs a symbolic factorization;
 * <li>A <i>factor</i> phase that numerically factors the matrix into
 * a sutiable decomposition;
 * <li>A <i>solve</i> phase that uses the factorization to solve
 * M x = b for some given right-hand-side b.
 * </ul>
 *
 * Typical usage of this solver is exemplified by 
 * the following call sequence:
 * <pre>
 *    Matrix M;                 // matrix to be solved
 *    VectorNd x, b;            // solution vector and right-hand-side
 *    
 *    PardisoSolver solver = new PardisoSolver();
 *    solver.analyze (M, M.rowSize(), Matrix.SYMMETRIC); // symbolic factorization
 *    solver.factor();          // numeric factorization
 *    solver.solve (x, b);      // solution using factorization
 *    
 *    solver.dispose();         // release resources when we are done
 * </pre>
 * It is <i>very</i> important to call <code>dispose()</code> when the solver
 * is no longer required, in order to release internal native resources that
 * have been allocated for the Pardiso native code.
 *
 * It is not necessary to call the <code>analyze()</code> and
 * <code>factor()</code> methods every time a solution is
 * required. <code>analyze()</code> needs to be called only when a matrix is
 * first presented to the solver or when its sparsity structure changes.
 * <code>factor()</code> needs to be called only when the numeric values of the
 * matrix change. For a given set of numeric values, once <code>factor()</code>
 * has been called, <code>solve()</code> can be called as many times as desired
 * to generate solutions for different right-hand sides.
 *
 * <p>It is also possible to avoid using a <code>Matrix</code> object, and
 * instead call <code>analyze()</code> and <code>factor()</code> with 
 * compressed row storage (CRS) data structures directly, as in
 * <pre>
 *    solver.analyze (vals, colIdxs, rowOffs, size, matrixType);
 *    solver.factor (vals);
 * </pre>
 * Here <code>vals</code>, <code>colIdxs</code>, and <code>rowOffs</code>
 * descrive the sparse matrix structure using the CRS format as described
 * in the documentation for
 * {@link maspack.matrix.Matrix#setCRSValues Matrix.setCRSValues}.
 *
 * Pardiso also supports iterative solving, using preconditioned CGS iteration.
 * The preconditioner is supplied by the most recent matrix factorization.  If
 * the current matrix values are close to those associated with the
 * factorization, then the resulting iterative solution time can be
 * considerably faster than the alternative combination of a factor() and a
 * solve(). There are several <code>iterativeSolve()</code> methods, which
 * obtain the current matrix values either directly as an input argument, or
 * from a <code>Matrix</code> object supplied through the
 * <code>analyze()</code> methods. When an <code>iterativeSolve()</code> method
 * is successful, it returns a positive number indicating the number of
 * iterations performed.  A possible call sequence is as follows:
 *
 * <pre>
 * {@code
 *    solver.analyze (M, M.rowSize(), Matrix.SYMMETRIC); // symbolic factorization
 *    solver.factor();          // numeric factorization
 *    while (computing) {
 *       ... update matrix values and right-hand side b ...;
 *       if (solver.iterativeSolve(x, b) <= 0) {
 *          // iterative solve failed; do explicit factor and solve
 *          solver.factor();
 *          solver.solve (x, b);
 *       }
 *    }
 * }
 * </pre>
 * 
 * A more sophisticated version of the above code will also call
 * <code>factor()</code> and <code>solve()</code> when the compute time
 * associated with <code>iterativeSolve()</code> exceeds a certain threshold.
 * After refactorization, the time required by <code>iterativeSolve()</code>
 * should be reduced since the next set of matrix values will again
 * (presumably) be close to those associated with the factorization.  This
 * functionality is provided by the <code>autoFactorAndSolve()</code> methods:
 * <pre>
 *    solver.analyze (M, M.rowSize(), Matrix.SYMMETRIC); // symbolic factorization
 *    solver.factor();          // numeric factorization
 *    while (computing) {
 *       ... update matrix values and right-hand side b ...;
 *       // automatically choose between iterative and direct solving
 *       solver.autoFactorAndSolve (x, b);
 *    }
 * </pre>
 */
public class PardisoSolver implements DirectSolver {

   public static boolean printThreadInfo = true;

   /**
    * Describes the reorder methods that can be used during the analyze phase
    * to reduce factorization fill-in.
    */
   public enum ReorderMethod {
      METIS,
      AMD,
      METIS_PARALLEL,
      DEFAULT,
   };

   private static final int ERR_INCONSISTENT_INPUT = -1;

   private static final int ERR_MEMORY = -2;

   private static final int ERR_REORDERING = -3;

   private static final int ERR_NUMERICAL = -4;

   private static final int ERR_INTERNAL = -5;

   private static final int ERR_PREORDERING = -6;

   private static final int ERR_DIAGONAL = -7;

   private static final int ERR_INT_OVERFLOW = -8;

   private static final int ERR_NO_LICENCE = -10;

   private static final int ERR_LICENCE_EXPIRED = -11;

   private static final int ERR_WRONG_USERHOST = -12;

   private static final int ERR_CGS_UNSTABLE_RESIDUAL = -21;

   private static final int ERR_CGS_SLOW_CONVERGENCE = -22;

   private static final int ERR_CGS_ITERATION_LIMIT = -23;

   private static final int ERR_CGS_PERTURBED_PIVOTS = -24;

   private static final int ERR_CGS_FACTORIZATION_TOO_FAST = -25;

   // the following ITERATION codes are not used in the MKL version of Pardiso
   private static final int ERR_ITERATION_LIMIT = -100;

   private static final int ERR_ITERATION_CONVERGENCE = -101;

   private static final int ERR_ITERATION_ERROR = -102;

   private static final int ERR_ITERATION_BREAKDOWN = -103;

   private static final int ERR_CANT_LOAD_LIBRARIES = -1000;

   private static final int INIT_UNKNOWN = 0;

   private static final int INIT_LIBRARIES_LOADED = 1;

   private static final int INIT_OK = 2;

   static int myInitStatus = INIT_UNKNOWN;
   static int myDefaultNumThreads = -1;
   
   /**
    * Returns a message corresponding to a Pardiso error code.
    */
   static String getErrorMessage (int code) {
      switch (code) {
         case ERR_INCONSISTENT_INPUT: {
            return "Inconsitent input";
         }
         case ERR_MEMORY: {
            return "Insufficient memory";
         }
         case ERR_REORDERING: {
            return "Reordering problem";
         }
         case ERR_NUMERICAL: {
            return "Zero pivot, numerical fact. or refinement problem";
         }
         case ERR_INTERNAL: {
            return "Internal error";
         }
         case ERR_PREORDERING: {
            return "Preordering failed";
         }
         case ERR_DIAGONAL: {
            return "Diagonal matrix problem";
         }
         case ERR_INT_OVERFLOW: {
            return "Integer overflow";
         }
         case ERR_NO_LICENCE: {
            return "No licence file pardiso.lic found";
         }
         case ERR_LICENCE_EXPIRED: {
            return "Licence has expired";
         }
         case ERR_WRONG_USERHOST: {
            return "License has wrong user or host name";
         }
         case ERR_CGS_UNSTABLE_RESIDUAL: {
            return "Excessive residual fluctuations";
         }
         case ERR_CGS_SLOW_CONVERGENCE: {
            return "Convergence too slow";
         }
         case ERR_CGS_ITERATION_LIMIT: {
            return "Iteration limit of 150 exceeded";
         }
         case ERR_CGS_PERTURBED_PIVOTS: {
            return "Perturbed pivots present";
         }
         case ERR_CGS_FACTORIZATION_TOO_FAST: {
            return "Inefficient: should factor directly";
         }
         case ERR_ITERATION_LIMIT: {
            return "Krylov-subspace iteration limit exceeded";
         }
         case ERR_ITERATION_CONVERGENCE: {
            return "Insufficient Krylov-subspace convergence in 25 iterations";
         }
         case ERR_ITERATION_ERROR: {
            return "Error in Krylov-subspace iteration";
         }
         case ERR_ITERATION_BREAKDOWN: {
            return "Break-down in Krylov-subspace iteration";
         }
         case ERR_CANT_LOAD_LIBRARIES: {
            return "Unable to load Pardiso library";
         }
         default: {
            if (code < 0) {
               return "??? Unknown error";
            }
            else {
               return "No error";
            }
         }
      }
   }

   private native long doInit();

   private native int doGetInitError (long handle);

   private native int doGetNumThreads (long handle);
   private native int doSetNumThreads (long handle, int num);

   private native int doGetNumNonZerosInFactors (long handle);
   private native int doGetNumNegEigenvalues (long handle);
   private native int doGetNumPosEigenvalues (long handle);
   private native int doGetNumPerturbedPivots (long handle);

   private native int doGetSPDZeroPivot (long handle);
   private native int doGetPeakAnalysisMemoryUsage (long handle);
   private native int doGetAnalysisMemoryUsage (long handle);
   private native int doGetFactorSolveMemoryUsage (long handle);

   private native int doGetMaxRefinementSteps (long handle);
   private native int doSetMaxRefinementSteps (long handle, int nsteps);
   private native int doGetNumRefinementSteps (long handle);

   private static final int AMD_REORDER = 0;
   private static final int METIS_REORDER = 2;
   private static final int METIS_REORDER_PARALLEL = 3;
   private native int doGetReorderMethod (long handle);
   private native int doSetReorderMethod (long handle, int method);

   private native int doGetPivotPerturbation (long handle);
   private native int doSetPivotPerturbation (long handle, int perturb);

   private native int doGetApplyScaling (long handle);
   private native int doSetApplyScaling (long handle, int apply);

   private native int doGetApplyWeightedMatchings (long handle);
   private native int doSetApplyWeightedMatchings (long handle, int apply);

   private native int doGetUse2x2Pivoting (long handle);
   private native int doSetUse2x2Pivoting (long handle, int enable);

   private native int doGetMatrixChecking (long handle);
   private native int doSetMatrixChecking (long handle, int enable);

   private native int doGetMessageLevel (long handle);
   private native int doSetMessageLevel (long handle, int level);

   private native int doSetMatrix (
      long handle, double[] vals, int rowStartIdxs[], int[] elemColIdxs,
      int size, int numVals);

   private native int doSetSPDMatrix (
      long handle, double[] vals, int rowStartIdxs[], int[] elemColIdxs,
      int size, int numVals);

   private native int doSetSymmetricMatrix (
      long handle, double[] vals, int rowStartIdxs[], int[] elemColIdxs,
      int size, int numVals);

   private native int doFactorMatrix (long handle, double[] vals);

   private native int doFactorMatrix (long handle);

   private native int doSolve (long handle, double[] x, double[] b);

   private native int doIterativeSolve (
      long handle, double[] vals, double[] x, double[] b, int tolExp);

   private native int doFactorAndSolve (
      long handle, double[] vals, double[] x, double[] b, int tolExp);

   private native void doRelease (long handle);

   private native void doExit (int code);

   private long myHandle;

   private int mySize;

   private int myNumVals;

   /**
    * Indicates that no matrix is currently set for this solver.
    */
   public static final int UNSET = 0;

   /**
    * Indicates that a matrix has been set and analyzed for this solver.
    */
   public static final int ANALYZED = 1;

   /**
    * Indicates that a matrix has been set, analyzed, and numerically factored
    * for this solver.
    */
   public static final int FACTORED = 2;

   private static final int RET_OK = 0;

   private int myState = UNSET;

   private String myErrMsg = null;

   private double[] myVals;

   private int[] myColIdxs;

   private int[] myRowOffs;

   // private int myNumBlkRows;
   // private int myNumBlkCols;
   private int myType;

   private Matrix myMatrix;

   // timing information for decided when to use iterative vs. direct solves
   private double myLastIterativeTimeMsec;

   private int myDirectCnt = 0;

   private double myDirectTimeMsec;

   private boolean myDirectIterativeDebug = false;

   private void setState (int state) {
      myState = state;
      myDirectCnt = 0;
   }

   /**
    * Returns the current stateface for this solver. Possible states are
    * {@link #UNSET UNSET}, {@link #ANALYZED ANALYZED}, and
    * {@link #FACTORED FACTORED}.
    *
    * @return state for this solver.
    */
   public int getState() {
      return myState;
   }

   /**
    * Attempts to load the native libraries needed for Pardiso.
    */
   private static void doLoadLibraries() {
      try {
         NativeLibraryManager.setFlags (NativeLibraryManager.VERBOSE);
         //String pardisoLibrary = "PardisoJNI.11.1.2.1"; // uses MKL 11.1.2
         String pardisoLibrary = "PardisoJNI.2021.1"; // uses MKL 2021.1
         switch (NativeLibraryManager.getSystemType()) {
            case Linux32:
            case Linux64: {
               //NativeLibraryManager.load ("gomp.1");
               break;
            }
            case Windows32:
            case Windows64: {
               NativeLibraryManager.load ("libiomp5md");
               break;
            }
            case MacOS64: {
               // Advance loading of iomp5 now appears to work on the Mac. This
               // helps solve issues with libPardisoJNI not finding it.
               NativeLibraryManager.load ("iomp5");
               break;
            }
         }
         NativeLibraryManager.load (pardisoLibrary);
         myInitStatus = INIT_LIBRARIES_LOADED;
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
         myInitStatus = ERR_CANT_LOAD_LIBRARIES;
      }
   }

   /**
    * Creates a new PardisoSolver object.
    */
   public PardisoSolver() {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }
      if (myInitStatus < 0) {
         throw new UnsupportedOperationException (
            "Pardiso not available: " + getInitErrorMessage());
      }
      myVals = new double[0];
      myColIdxs = new int[0];
      myRowOffs = new int[0];
      if (myDefaultNumThreads > 0) {
         // create the handle because earlier JNI implementations of
         // setNumThreads required this internally
         myHandle = doInit();
         setNumThreads (myDefaultNumThreads);
      }
   }

   void initialize () {
      myHandle = doInit();
   }

   int checkInitialization() {
      if (myHandle == 0) {
         initialize();
      }
      int err = doGetInitError (myHandle);
      if (err < 0) {
         myInitStatus = err;
         myErrMsg = getErrorMessage (myInitStatus);         
      }
      else {
         myInitStatus = INIT_OK;
         myErrMsg = null;
      }
      if (printThreadInfo){
         System.out.println ("Pardiso: max threads=" + getNumThreads());
      }
      return err;
   }

   /**
    * Returns true if the Pardiso solver is available. A solver might
    * <i>not</i> be available if the Pardiso native libraries cannot
    * be loaded for some reason.
    *
    * @return true if Pardiso is available.
    */
   public static boolean isAvailable () {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }     
      if (myInitStatus == INIT_LIBRARIES_LOADED) {
         PardisoSolver test = new PardisoSolver();
         test.checkInitialization();
         test.dispose();
      }
      return myInitStatus < 0 ? false : true;
   }

   /**
    * Returns a message describing an error that occurred during
    * initialization, or <code>null</code> if no error occurred.
    *
    * @return initialization error message
    */
   public static String getInitErrorMessage() {
      if (myInitStatus < 0) {
         return getErrorMessage (myInitStatus);
      }
      else {
         return null;
      }
   }

   private void allocateBufferSpace (int size, int numVals) {      
      if (myVals.length < numVals) {
         myVals = new double[numVals];
         myColIdxs = new int[numVals];
      }
      if (myRowOffs.length < size+1) {
         myRowOffs = new int[size+1];
      }
   }

   private Partition getPartition (int type) {
      if ((type & Matrix.SYMMETRIC) != 0) {
         return Partition.UpperTriangular;
      }
      else {
         return Partition.Full;
      }
   }

   /**
    * Sets the matrix associated with this solver and performs
    * symbolic analysis on it. The matrix is assumed to be square.
    * After calling this method, the solver's state is set to
    * {@link #ANALYZED ANALYZED}.
    *
    * <p> 
    * Normally the matrix is simply supplied by the argument <code>M</code>,
    * unless the <code>size</code> arugment is less than <code>M.rowSize()</code>,
    * in which case the matrix is taken to be the top-left diagonal sub-matrix
    * of the indicated size. This solver retains a pointer to <code>M</code>
    * until the next call to {@link #analyze analyze()} or
    * {@link #analyzeAndFactor analyzeAndFactor()}.
    *
    * <p>The type of the matrix is given by <code>type</code>:
    *
    * <dl>
    * <dt>Matrix.INDEFINITE</dt>
    * <dd>will produce a general permuted L U decomposition;
    * <dt>Matrix.SYMMETRIC</dt>
    * <dd>will produce an L D L^T decomposition;
    * <dt>Matrix.SPD</dt>
    * <dd>will produce a Cholesky L L^T decomposition;
    * </dl>
    *
    * @param M supples the matrix to be analyzed
    * @param size size of the matrix to be analyzed
    * @param type type of the matrix to be analyzed
    * @throws IllegalArgumentException if the matrix is not square or if
    * <code>size</code> is out of bounds
    * @throws NumericalException if the matrix cannot be analyzed for numeric
    * reasons.
    */
   public synchronized void analyze (Matrix M, int size, int type) {
      int numVals;

      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException ("Matrix is not square");
      }
      int origSize = M.rowSize();
      if (size < 0 || size > origSize) {
         throw new IllegalArgumentException ("Requested size " + size
         + " is out of bounds");
      }
      Partition part = getPartition (type);
      numVals = M.numNonZeroVals (Partition.Full, size, size);

      if (part == Partition.UpperTriangular) {
         numVals -= (numVals - size) / 2;
      }
      allocateBufferSpace (size, numVals);
      M.getCRSIndices (myColIdxs, myRowOffs, part, size, size);
      M.getCRSValues (myVals, part, size, size);

      // // add 1 to indices, since Pardiso indices are 1-based
      // for (int i = 0; i < numVals; i++) {
      //    myColIdxs[i]++;
      // }
      // for (int i = 0; i < size; i++) {
      //    myRowOffs[i]++;
      // }

      myType = type;
      myMatrix = M;
      if ((type & Matrix.SYMMETRIC) != 0) {
         if ((type & Matrix.POSITIVE_DEFINITE) != 0) {
            setSPDMatrix (myVals, myRowOffs, myColIdxs, size, numVals);
         }
         else {
            setSymmetricMatrix (myVals, myRowOffs, myColIdxs, size, numVals);
         }
      }
      else {
         setMatrix (myVals, myRowOffs, myColIdxs, size, numVals);
      }//
      if (myState == UNSET) {
         throw new NumericalException (
            "Pardiso: unable to analyze matrix: "+myErrMsg);
      }
   }

   /**
    * Performs a numeric factorization of the matrix associated with this
    * solver, using the current numeric values contained within
    * the matrix that was supplied by a previous call to 
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)}.
    * After calling this method, the solver's state is set to
    * {@link #FACTORED FACTORED}.
    *
    * @throws ImproperStateException if not preceded by a call to
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)}
    * @throws NumericalException if the matrix cannot be factored for numeric
    * reasons.
    */
   public void factor() {
      if (myMatrix == null) {
         throw new ImproperStateException (
            "analyze(Matrix) or analyzeAndFactor(Matrix) not previously called");
      }
      Partition part = getPartition (myType);
      myMatrix.getCRSValues (myVals, part, mySize, mySize);
      factor (myVals);
   }

   /**
    * Performs a numeric factorization of the most recently analyzed matrix
    * solver using the supplied numeric values.  After calling this method, the
    * solver's state is set to {@link #FACTORED FACTORED}.
    *
    * @param vals non-zero matrix element values
    * @throws ImproperStateException if this solver's state is
    * {@link #UNSET UNSET} or if the number of supplied values is
    * less that the number of non-zero elements in the analyzed matrix.
    * @throws NumericalException if the matrix cannot be factored for numeric
    * reasons.
    */
   public synchronized void factor (double[] vals) {
      if (myState == UNSET) {
         throw new IllegalStateException ("No matrix currently set");
      }
      else if (vals.length < myNumVals) {
         throw new IllegalArgumentException ("Not enough values: vals.length="
         + vals.length + ", expected number is " + myNumVals);
      }
      //System.out.println ("factor "+Thread.currentThread());
      int rcode = doFactorMatrix (myHandle, vals);
      if (rcode == RET_OK) {
         setState (FACTORED);
         myErrMsg = null;
      }
      else {
         myErrMsg = getErrorMessage (rcode);
         throw new NumericalException (
            "Pardiso: unable to factor matrix: "+myErrMsg);
      }
      int nump = getNumPerturbedPivots();
      if (nump > 0) {
         System.out.println ("Pardiso: num perturbed pivots=" + nump);
      }
   }


   /**
    * Convenience method that sets the matrix associated with this solver,
    * performs symbolic analysis on it, and factors it. The matrix is assumed
    * to be square and have a type of <code>Matrix.INDEFINITE</code>, meaning
    * that Pardiso will produce an L U factorization.  After calling this
    * method, the solver's state is set to {@link #ANALYZED
    * ANALYZED}.
    *
    * @throws NumericalException if the matrix cannot be analyzed or factored
    * for numeric reasons.
    */
   public void analyzeAndFactor (Matrix M) {
      analyze (M, M.rowSize(), 0);
      factor();
   }

   /**
    * Calls {@link #factor()} and {@link #solve(double[],double[])} together,
    * or, if <code>tolExp</code> is positive, automatically determines
    * when to call
    * {@link #iterativeSolve(maspack.matrix.VectorNd,maspack.matrix.VectorNd,int)}
    * with the specific <code>tolExp</code>instead,
    * depending on whether the matrix is factored and if it is estimated
    * that <code>iterativeSolve</code> will save time.
    * It is assumed that a matrix was supplied to the solver using a previous
    * call to
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)}.
    *
    * @param x returns the solution value
    * @param b supplies the right-hand side
    * @param tolExp if positive, enables iterative solving and provides
    * the exponent of the stopping criterion
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size, or if
    * <code>topExp</code> is negative.
    * @throws ImproperStateException if not preceded by a call to
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)}
    * @throws NumericalException if the matrix cannot be factored for numeric
    * reasons
    */
   public void autoFactorAndSolve (VectorNd x, VectorNd b, int tolExp) {
      checkSolveArgs (x, b);
      autoFactorAndSolve (x.getBuffer(), b.getBuffer(), tolExp);
   }

   /**
    * Implementation of
    * {@link #autoFactorAndSolve(maspack.matrix.VectorNd,maspack.matrix.VectorNd,int)}
    * that uses <code>double[]</code> objects
    * to store to the result and right-hand side.
    *
    * @param x returns the solution value
    * @param b supplies the right-hand side
    * @param tolExp if positive, enables iterative solving and provides
    * the exponent of the stopping criterion
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size, or if
    * <code>topExp</code> is negative.
    * @throws ImproperStateException if not preceded by a call to
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)}
    * @throws NumericalException if the matrix cannot be factored for numeric
    * reasons
    */
   public void autoFactorAndSolve (double[] x, double[] b, int tolExp) {
      long t0, t1;

      if (myMatrix == null) {
         throw new ImproperStateException (
            "analyze(Matrix) or analyzeAndFactor(Matrix) not previously called");
      }
      NumberFormat fmt = new NumberFormat ("%8.3f");
      boolean tryIterativeSolve = true;
      /*
       * iterative solving disabled for WinXP
       */
      if (!System.getProperty ("os.name").equals ("Windows XP")) {
         tryIterativeSolve = false;
      }
      if (tryIterativeSolve) {
         if (tolExp <= 0 || myDirectCnt == 0 ||
             myLastIterativeTimeMsec >= (0.5 * myDirectTimeMsec / myDirectCnt)) {
            tryIterativeSolve = false;
         }
      }

      int iterCode = 0;
      double iterTimeMsec = 0;
      if (tryIterativeSolve) {
         // try iterative solve

         t0 = System.nanoTime();
         iterCode = iterativeSolve (x, b, tolExp);
         t1 = System.nanoTime();
         if (iterCode > 0) {
            myLastIterativeTimeMsec = (t1 - t0) * 1e-6;
            if (myDirectIterativeDebug) {
               System.out.println (
                  "iterative solve: " + iterCode + ", "
                  + fmt.format (myLastIterativeTimeMsec) + " msec");
            }
         }
         else {
            String errMsg = getErrorMessage (-iterCode%10 - 20);
            System.out.println (
               "PardisoSolver.factorAndSolve: iteration failed ("+errMsg+
               "), using direct solve");
            tryIterativeSolve = false;
         }
      }

      if (!tryIterativeSolve) {
         // do the proper factor and solve
         t0 = System.nanoTime();

         int savedFactorCnt = myDirectCnt; // myDirectCnt is cleared in factor
         factor();
         solve (x, b);
         t1 = System.nanoTime();
         myDirectCnt = savedFactorCnt + 1;
         myDirectTimeMsec += (t1 - t0) * 1e-6;
         myLastIterativeTimeMsec = 0;
         if (myDirectIterativeDebug) {
            System.out.println ("direct solve: "
            + fmt.format ((t1 - t0) * 1e-6) + " msec");
         }
         myState = FACTORED;
      }
   }

   private void checkSetArgs (
      double[] vals, int rowIdxs[], int[] colIdxs, int size, int numVals) {
      if (vals.length < numVals) {
         throw new IllegalArgumentException ("Not enough values: vals.length="
         + vals.length + ", numVals=" + numVals);
      }
      if (colIdxs.length < numVals) {
         throw new IllegalArgumentException (
            "Not enough column indices: colIdxs.length=" + colIdxs.length
            + ", numVals=" + numVals);
      }
      if (rowIdxs.length < size) {
         throw new IllegalArgumentException (
            "Not enough row start indices: rowIdxs.length=" + rowIdxs.length
            + ", size=" + size);
      }
   }


   /**
    * Sets the default number of threads that Pardiso is assigned when a
    * <code>PardisoSolver</code> is created. The results are undefined if this
    * number exceeds the maximum number of threads available on the
    * system. Setting <code>num</code> to a value {@code <=} 0 will reset the
    * number of threads to the default used by OpenMP, which is typically the
    * value stored in the environment variable <code>OMP_NUM_THREADS</code>.
    *
    * @param num default number of threads to use
    * @see #getDefaultNumThreads
    */
   public static void setDefaultNumThreads (int num) {
      if (myDefaultNumThreads != num) {
         System.out.println ("Pardiso: setting max threads to " + num);
         myDefaultNumThreads = num;
      }
   }

   /**
    * Returns the default number of threads that Pardiso is assigned when a
    * <code>PardisoSolver</code> is created.
    *
    * @see #setDefaultNumThreads
    */
   public static int getDefaultNumThreads () {
      return myDefaultNumThreads;
   }

   /**
    * Sets the number of threads that Pardiso should use. The results are
    * undefined if this number exceeds the maximum number of threads available
    * on the system. Setting <code>num</code> to a value {@code <=} 0 will reset the
    * number of threads to the default used by OpenMP, which is typically the
    * value stored in the environment variable <code>OMP_NUM_THREADS</code>.
    *
    * <p><b>Note:</b> under the MKL version of Pardiso, changes to the thread
    * number are applied globally to all instances of Pardiso running in the
    * same process. Therefore, this method is of limited utility and does not
    * allow different numbers of threads to be used by different PardisoSolver
    * instances. Moreover, the thread number should not be changed in between
    * the analyze, factor and solve phases.
    *
    * @param num number of threads to use
    * @see #getNumThreads
    */
   public synchronized void setNumThreads (int num) {
      if (myHandle == 0) {
         initialize();
      }
      doSetNumThreads (myHandle, num);
   }

   /**
    * Returns the number of threads that Pardiso should use. By default, this
    * is the default number used by OpenMP, which is typically the value stored
    * in the environment variable <code>OMP_NUM_THREADS</code>.
    *
    * @return number of threads Pardio should use
    * @see #setNumThreads
    */
   public synchronized int getNumThreads () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumThreads (myHandle);
   }

   /**
    * Sets the maximum number of iterative refinement steps that Pardiso should
    * perform after a solve. Setting this to 0 disables iterative
    * refinement. Setting this to -1 will cause Pardiso to choose a default
    * value appropriate to the matrix type. More iterative refinement steps
    * will increase solution accuracy but slow down the solve.
    *
    * @param nsteps maximum number of iterative refinement steps
    * @see #getMaxRefinementSteps
    * @see #getNumRefinementSteps
    */
   public synchronized void setMaxRefinementSteps (int nsteps) {
      if (myHandle == 0) {
         initialize();
      }
      doSetMaxRefinementSteps (myHandle, nsteps);
   }

   /**
    * Returns the maximum number of iterative refinement steps that Pardiso
    * should perform after a solve. 
    *
    * @return maximum number of iterative refinement steps
    * @see #setMaxRefinementSteps
    * @see #getNumRefinementSteps
    */
   public synchronized int getMaxRefinementSteps () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetMaxRefinementSteps (myHandle);
   }

   /**
    * Returns the number of iterative refinement steps that Pardiso
    * actually performed during the most call to {@link #solve solve()}.
    *
    * @return number of iterative refinement steps actually performed
    * @see #getMaxRefinementSteps
    * @see #setMaxRefinementSteps
    */
   public synchronized int getNumRefinementSteps () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumRefinementSteps (myHandle);
   }

   /**
    * Gets the reorder method that is used during the analyze phase to
    * reduced factorization fill-in. 
    *
    * @return current reorder method
    * @see #setReorderMethod
    */
   public synchronized ReorderMethod getReorderMethod () {
      if (myHandle == 0) {
         initialize();
      }
      int m = doGetReorderMethod (myHandle);
      switch (m) {
         case AMD_REORDER: {
            return ReorderMethod.AMD;
         }
         case METIS_REORDER: {
            return ReorderMethod.METIS;
         }
         case METIS_REORDER_PARALLEL: {
            return ReorderMethod.METIS_PARALLEL;
         }
         default:
            throw new UnsupportedOperationException (
               "Pardiso returned unknown reorder method: " + m);
      }
   }

   /**
    * Sets the reorder method that should be used during the analyze phase to
    * reduced factorization fill-in. Setting the method to <code>DEFAULT</code>
    * will cause Pardiso to choose its default value.
    *
    * @param method reorder method
    * @see #getReorderMethod
    */
   public synchronized void setReorderMethod (ReorderMethod method) {
      if (myHandle == 0) {
         initialize();
      }
      int m = 0;
      switch (method) {
         case AMD: {
            m = AMD_REORDER;
            break;
         }
         case METIS: {
            m = METIS_REORDER;
            break;
         }
         case METIS_PARALLEL: {
            m = METIS_REORDER_PARALLEL;
            break;
         }
         case DEFAULT: {
            m = -1;
            break;
         }
         default:
            throw new UnsupportedOperationException (
               "Unknown reorder method: " + method);
      }
      doSetReorderMethod (myHandle, m);
   }

   /**
    * Sets the size of the perturbation that should be used to resolve
    * zero-pivots, expressed as a negative power-of-ten exponent.
    * The perturbation size is given by
    * <pre>
    *  norm(M) 10^{-n}
    * </pre>
    * where <code>n</code> is the argument to this method and
    * <code>norm(M)</code> is a norm of the matrix.  Setting <code>n</code>
    * this to -1 will cause Pardiso to choose a default value appropriate to
    * the matrix type.
    *
    * @param n perturbation exponent
    * @see #getPivotPerturbation 
    */
   public synchronized void setPivotPerturbation (int n) {
      if (myHandle == 0) {
         initialize();
      }
      doSetPivotPerturbation (myHandle, n);
   }

   /**
    * Returns the size of the perturbation that should be used to resolve
    * zero-pivots, expressed as a negative power-of-ten exponent.
    *
    * @return perturbation exponent
    * @see #setPivotPerturbation
    */
   public synchronized int getPivotPerturbation () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetPivotPerturbation (myHandle);
   }

   /**
    * Sets whether or not Pardiso should apply matrix scaling to its
    * factorizations. Scaling can be applied to unsymmetric matrices, or to
    * symmetric matrices if weighted matchings (see {@link
    * #setApplyWeightedMatchings setApplyWeightedMatchings()}) are also
    * selected. Scaling is controlled by <code>enable</code> as follows:
    * {@code enable > 0} enables scaling, {@code enable = 0} disables
    * scaling, and {@code enable < 0} causes the solver to choose a
    * default value appropriate to the matrix type.
    *
    * @param enable enables/disables matrix scaling
    * @see #getApplyScaling
    */
   public synchronized void setApplyScaling (int enable) {
      if (myHandle == 0) {
         initialize();
      }
      if (enable > 0) {
         enable = 1;
      }
      doSetApplyScaling (myHandle, enable);
   }

   /**
    * Returns whether or not matrix scaling is enabled.
    *
    * @return true if matrix scaling is enabled
    * @see #setApplyScaling
    */
   public synchronized boolean getApplyScaling () {
      if (myHandle == 0) {
         initialize();
      }
      return (doGetApplyScaling (myHandle) != 0);
   }

   /**
    * Sets whether or not Pardiso should apply weighted matching to its
    * factorizations. This, along with scaling (see {@link #setApplyScaling
    * setApplyScaling()}) is recommended for highly indefinite symmetric
    * systems (such as saddle point problems) and is the default for symmetric
    * matrices.  Weighted matchings are controlled by <code>enable</code> as
    * follows: {@code enable > 0} enables them, {@code enable = 0}
    * disables them, and {@code enable < 0} causes the solver to choose a
    * default value appropriate to the matrix type.
    *
    * @param enable enables/disables weight matchings
    * @see #getApplyWeightedMatchings
    */
   public synchronized void setApplyWeightedMatchings (int enable) {
      if (myHandle == 0) {
         initialize();
      }
      if (enable > 0) {
         enable = 1;
      }
      doSetApplyWeightedMatchings (myHandle, enable);
   }

   /**
    * Returns whether or not weighted matchings are enabled.
    *
    * @return true if weighted matchings are enabled
    * @see #setApplyWeightedMatchings
    */
   public synchronized boolean getApplyWeightedMatchings () {
      if (myHandle == 0) {
         initialize();
      }
      return (doGetApplyWeightedMatchings (myHandle) != 0);
   }

   /**
    * Sets whether or not Pardiso should use 2 x 2 Bunch and Kaufman
    * pivoting (in addition to regular 1 x 1 pivoting) for symmetric
    * indefinite matrices. 2 x 2 pivoting is controlled by <code>enable</code> as
    * follows: {@code enable > 0} enables it, {@code enable = 0}
    * disables it, and {@code enable < 0} causes the solver to choose a
    * default value.
    *
    * @param enable enables 2 x 2 pivoting
    * @see #getUse2x2Pivoting
    */
   public synchronized void setUse2x2Pivoting (int enable) {
      if (myHandle == 0) {
         initialize();
      }
      if (enable > 0) {
         enable = 1;
      }
      doSetUse2x2Pivoting (myHandle, enable);
   }

   /**
    * Returns whether or not 2 x 2 pivoting is enabled.
    *
    * @return true if 2 x 2 pivoting is enabled
    * @see #setUse2x2Pivoting
    */
   public synchronized boolean getUse2x2Pivoting () {
      if (myHandle == 0) {
         initialize();
      }
      return (doGetUse2x2Pivoting (myHandle) != 0);
   }

   /**
    * Enables or disables checking of the integrity of the matrix data
    * structures passed to Pardiso. By default this is disabled, but it may be
    * a good idea to enable it if problems arise such as crashes within the
    * Pardiso native code.
    *
    * @param enable enables/disables matrix checking
    * @see #getMatrixChecking
    */
   public synchronized void setMatrixChecking (boolean enable) {
      if (myHandle == 0) {
         initialize();
      }
      doSetMatrixChecking (myHandle, enable ? 1 : 0);
   }

   /**
    * Returns true if matrix checking is enabled.
    *
    * @return true if matrix checking is enabled
    * @see #setMatrixChecking
    */
   public synchronized boolean getMatrixChecking () {
      if (myHandle == 0) {
         initialize();
      }
      return (doGetMatrixChecking (myHandle) != 0);
   }

   /**
    * Sets the message level for the Pardiso native code.  0 disables messages,
    * while 1 causes printing of various stats and information about the solve
    * process. The message level should normally be set to 0.
    *
    * @param level native code message level
    * @see #getMessageLevel
    */
   public synchronized void setMessageLevel (int level) {
      if (level < 0) {
         throw new IllegalArgumentException (
            "Message level must be positive");
      }
      if (myHandle == 0) {
         initialize();
      }
      doSetMessageLevel (myHandle, level);
   }

   /**
    * Returns the message level for the Pardiso native code.
    *
    * @return message level
    * @see #setMessageLevel
    */
   public synchronized int getMessageLevel () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetMessageLevel (myHandle);
   }

   /**
    * Returns the number of non-zeros elements in the factorization.  Pardiso
    * determines this during the analyze phase, and so the number returned by
    * this method reflects that determined during the most recent
    * <code>analyze()</code> call.
    *
    * @return number non-zero elements in the factorization.
    */
   public synchronized int getNumNonZerosInFactors () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumNonZerosInFactors (myHandle);
   }

   /**
    * Returns the number of negative eigenvalues that were detected during the
    * most recent numeric factorization of a symmetric indefinite matrix (i.e.,
    * during the last <code>factor()</code> call). For matrices that are
    * not symmetric indefinite, -1 is returned.
    * 
    * @return number of negative eigenvalues for symmetric indefinite matrices
    */      
   public synchronized int getNumNegEigenvalues () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumNegEigenvalues (myHandle);
   }

   /**
    * Returns the number of positive eigenvalues that were detected during the
    * most recent numeric factorization of a symmetric indefinite matrix (i.e.,
    * during the last <code>factor()</code> call). For matrices that are
    * not symmetric indefinite, -1 is returned.
    * 
    * @return number of positive eigenvalues for symmetric indefinite matrices
    */      
   public synchronized int getNumPosEigenvalues () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumPosEigenvalues (myHandle);
   }

   /**
    * Returns the number of pivot perturbations that were required
    * during the last recent numeric factorization (i.e., during
    * the last <code>factor()</code> call). Pivot perturbation
    * generally indicates a singular, or very nearly singular, matrix.
    *
    * @return number of pivot perturbations
    */      
   public synchronized int getNumPerturbedPivots () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumPerturbedPivots (myHandle);
   }

   /**
    * If the solver detects a zero or negative pivot for an SPD matrix,
    * this method returns the row number where the first zero or negative
    * pivot was detected.
    * 
    * @return location of first zero or negative pivot, if any
    */
   public synchronized int getSPDZeroPivot () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetSPDZeroPivot (myHandle);
   }

   /**
    * Returns the peak amount of memory (in kbytes) that was used
    * during the most recent <code>analyze()</code> call.
    *
    * @return peak memory usage during analyze.
    */
   public synchronized int getPeakAnalysisMemoryUsage () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetPeakAnalysisMemoryUsage (myHandle);
   }

   /**
    * Returns the amount of permanent memory (in kbytes) that was allocated
    * during the most recent <code>analyze()</code> call.
    *
    * @return permanent memory allocated during analyze.
    */
   public synchronized int getAnalysisMemoryUsage () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetAnalysisMemoryUsage (myHandle);
   }

   /**
    * Returns the total memory consumption (in kbytes) required for the
    * <code>factor()</code> and <code>solve()</code> calls.  This number is
    * determined in the most recent <code>factor()</code> call.
    *
    * @return total memory usage during factor and solve steps.
    */
   public synchronized int getFactorSolveMemoryUsage () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetFactorSolveMemoryUsage (myHandle);
   }

   /**
    * Sets the matrix associated with this solver and performs
    * symbolic analysis on it. The matrix is assumed to be square.
    * After calling this method, the solver's state is set to
    * {@link #ANALYZED ANALYZED}.
    *
    * <p> The matrix structure and its initial values are described using a
    * compressed row storage (CRS) format. See {@link
    * maspack.matrix.Matrix#setCRSValues Matrix.setCRSValues()} for a detailed
    * description of this format. The matrix type is the same as that supplied
    * to {@link #analyze(maspack.matrix.Matrix,int,int)
    * analyze(Matrix,int,int)}.  It is not possible to call {@link #factor()
    * factor()} after calling this version of <code>analyze</code> because it
    * does not supply a matrix that can be used to obtain values from.
    *
    * @param vals values of the non-zero matrix elements. These may be used to
    * assist the symbolic factorization, but will not used in any actual
    * numeric factorization.
    * @param colIdxs 1-based column indices of the non-zero matrix elements.
    * @param rowOffs 1-based row start offsets into <code>vals</code> and
    * <code>colIdxs</code>, corresponding to CRS format.
    * @param size size of the matrix to be analyzed
    * @param type type of the matrix to be analyzed
    * @throws IllegalArgumentException if the CRS data structures
    * are inconsistent.
    * @throws NumericalException if the matrix cannot be analyzed for numeric
    * reasons.
    */
   public synchronized void analyze (
      double[] vals, int colIdxs[], int rowOffs[], int size, int type) {
      
      if (myHandle == 0) {
         initialize();
      }
      int numVals = rowOffs[size]-1;
      checkSetArgs (vals, rowOffs, colIdxs, size, numVals);
 
      myType = type;
      myMatrix = null;
      if ((type & Matrix.SYMMETRIC) != 0) {
         if ((type & Matrix.POSITIVE_DEFINITE) != 0) {
            setSPDMatrix (vals, rowOffs, colIdxs, size, numVals);
         }
         else {
            setSymmetricMatrix (vals, rowOffs, colIdxs, size, numVals);
         }
      }
      else {
         setMatrix (vals, rowOffs, colIdxs, size, numVals);
      }//
      if (myState == UNSET) {
         throw new NumericalException (
            "Pardiso: unable to analyze matrix: "+myErrMsg);
      }
   }
   
   void setSymmetricMatrix (
      double[] vals, int rowOffs[], int[] colIdxs, int size, int numVals) {
      // System.out.println ("set symmetric:");
      // for (int k=0; k<numVals; k++)
      // { System.out.println (" " + colIdxs[k] + " " + vals[k]);
      // }
      // for (int i=0; i<size; i++)
      // { System.out.println (" " + rowIdxs[i]);
      // }
      if (myHandle == 0) {
         initialize();
      }
      //System.out.println ("setSymmetric "+Thread.currentThread());
      checkSetArgs (vals, rowOffs, colIdxs, size, numVals);
      int rcode =
         doSetSymmetricMatrix (myHandle, vals, rowOffs, colIdxs, size, numVals);
      if (rcode == RET_OK) {
         setState (ANALYZED);
         mySize = size;
         myNumVals = numVals;
         myErrMsg = null;
      }
      else {
         setState (UNSET);
         myErrMsg = getErrorMessage (rcode);
      }
   }

   void setSPDMatrix (
      double[] vals, int rowIdxs[], int[] colIdxs, int size, int numVals) {

      if (myHandle == 0) {
         initialize();
      }
      //System.out.println ("setSPD "+Thread.currentThread());
      checkSetArgs (vals, rowIdxs, colIdxs, size, numVals);
      int rcode =
         doSetSPDMatrix (myHandle, vals, rowIdxs, colIdxs, size, numVals);
      if (rcode == RET_OK) {
         setState (ANALYZED);
         mySize = size;
         myNumVals = numVals;
         myErrMsg = null;
      }
      else {
         setState (UNSET);
         myErrMsg = getErrorMessage (rcode);
      }
   }

   void setMatrix (
      double[] vals, int rowIdxs[], int[] colIdxs, int size, int numVals) {

      if (myHandle == 0) {
         initialize();
      }
      //System.out.println ("setMatrix "+Thread.currentThread());
      checkSetArgs (vals, rowIdxs, colIdxs, size, numVals);
      int rcode = doSetMatrix (myHandle, vals, rowIdxs, colIdxs, size, numVals);
      if (rcode == RET_OK) {
         setState (ANALYZED);
         mySize = size;
         myNumVals = numVals;
         myErrMsg = null;
      }
      else {
         setState (UNSET);
         myErrMsg = getErrorMessage (rcode);
      }
   }

   private void checkFactored () {
      if (myState != FACTORED) {
         throw new IllegalStateException ("Matrix is not factored");
      }
   }

   private void checkSolveArgs (double[] x, double[] b) {
      if (x.length < mySize) {
         throw new IllegalArgumentException (
            "x is too small: length="+x.length+", expected size is " + mySize);
      }
      else if (b.length < mySize) {
         throw new IllegalArgumentException (
            "b is too small: length="+b.length+", expected size is " + mySize);
      }
   }

   private void checkSolveArgs (VectorNd x, VectorNd b) {
      if (x.size() < mySize) {
         throw new IllegalArgumentException (
            "x is too small: size="+x.size()+", expected size is " + mySize);
      }
      else if (b.size() < mySize) {
         throw new IllegalArgumentException (
            "b is too small: size="+b.size()+", expected size is " + mySize);
      }
   }

   /**
    * Solves the matrix associated with this solver for x, given a
    * specific right-hand-side b. It is assumed that the matrix
    * has been factored and that this solver's state is
    * {@link #FACTORED FACTORED}.
    *
    * @param x returns the solution value
    * @param b supplies the right-hand-side
    * @throws IllegalStateException if this solver's state is not
    * {@link #FACTORED FACTORED}
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size.
    */
   public synchronized void solve (VectorNd x, VectorNd b) {
      checkFactored();
      checkSolveArgs (x, b);
      int rcode = doSolve (myHandle, x.getBuffer(), b.getBuffer());
   }

   /**
    * Solves the matrix associated with this solver for x, given a
    * specific right-hand-side b. It is assumed that the matrix
    * has been factored and that this solver's state is
    * {@link #FACTORED FACTORED}.
    *
    * @param x returns the solution value
    * @param b supplies the right-hand-side
    * @throws IllegalStateException if this solver's state is not
    * {@link #FACTORED FACTORED}
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size.
    */
   public synchronized void solve (double[] x, double[] b) {
      checkFactored();
      checkSolveArgs (x, b);
      int rcode = doSolve (myHandle, x, b);
   }

   /**
    * Computes the norm of the residual
    * <pre>
    *   M x - b
    * </pre>
    * for a given values of M, x, and b. The values of <code>M</code>
    * are given in compressed row storage (CRS) format. 
    *
    * @param rowOffs matrix row offsets (CRS format)
    * @param colIdxs non-zero element column indices (CRS format)
    * @param vals non-zero element value (CRS format)
    * @param size size of the matrix
    * @param x supplies the solution value
    * @param b supplies the right-hand-side
    * @param symmetric if <code>true</code>, assumes that the arguments
    * define only the upper triangular portion of a symmetric matrix.
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size.
    */
   public double residual (
      int[] rowOffs, int[] colIdxs, double[] vals, int size,
      double[] x, double[] b, boolean symmetric) {

      if (x.length < size) {
         throw new IllegalArgumentException ("x is too small: x.length="
         + x.length + ", expected size is " + size);
      }
      else if (b.length < size) {
         throw new IllegalArgumentException ("b is too small: b.length="
         + b.length + ", expected size is " + size);
      }
      double[] check = new double[size];
      for (int i=0; i<size; i++) {
         int end = rowOffs[i+1]-1;
         for (int k=rowOffs[i]-1; k<end; k++) {
            int j = colIdxs[k]-1;
            check[i] += vals[k]*x[j];
            if (symmetric && i != j) {
               check[j] += vals[k]*x[i];
            }
         }
      }
      double sum = 0;
      for (int i=0; i<size; i++) {
         sum += (check[i]-b[i])*(check[i]-b[i]);
      }
      return Math.sqrt(sum);
   }

   /**
    * Implementation of
    * {@link #iterativeSolve(double[],double[],int)} 
    * that uses {@link maspack.matrix.VectorNd VectorNd} objects
    * to store to the result and right-hand side.
    *
    * @param x returns the solution value
    * @param b supplies the right-hand side
    * @param tolExp exponent for the stopping criterion
    * @return number of iterations performed, negated if unsuccessful
    * @throws ImproperStateException if not preceded by a call to
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)},
    * or if the matrix has not previously been factored.
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size, or if
    * <code>topExp</code> is negative.
    */
   public int iterativeSolve (VectorNd x, VectorNd b, int tolExp) {
      checkFactored();
      checkSolveArgs (x, b);
      return iterativeSolve (x.getBuffer(), b.getBuffer(), tolExp);
   }

   /**
    * Attempts to use preconditioned CGS iteration to solve M x = b for a given
    * right-hand side <code>b</code>. Current numeric values for M are obtained
    * from the matrix that was supplied by a previous call to {@link
    * #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or {@link
    * #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)}.  The
    * most recent numeric factorization of this matrix will be used as a
    * preconditioner for the CGS iteration, with a relative stopping criterion
    * given by <code>10^-tolExp</code>. It is assumed that the matrix has
    * been previously factored with a call to <code>factor()</code>.
    *
    * <p>If the CGS iteration is successful, this method returns a positive
    * value giving the number of iterations required. If unsuccessful,
    * it returns a non-postive value giving the negative of the number
    * of iterations that were actually performed, and
    * {@link #getErrorMessage getErrorMessage()} can be used to determine
    * the underlying error.
    *
    * @param x returns the solution value
    * @param b supplies the right-hand side
    * @param tolExp exponent for the stopping criterion
    * @return number of iterations performed, negated if unsuccessful
    * @throws ImproperStateException if not preceded by a call to
    * {@link #analyze(maspack.matrix.Matrix,int,int) analyze(Matrix,int,int)} or
    * {@link #analyzeAndFactor(maspack.matrix.Matrix) analyzeAndFactor(Matrix)},
    * or if the matrix has not previously been factored.
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size, or if
    * <code>topExp</code> is negative.
    */
   public synchronized int iterativeSolve (double[] x, double[] b, int tolExp) {
      if (myMatrix == null) {
         throw new ImproperStateException (
            "analyze(Matrix) or analyzeAndFactor(Matrix) not previously called");
      }  
      Partition part = getPartition (myType);
      myMatrix.getCRSValues (myVals, part, mySize, mySize);
      return iterativeSolve (myVals, x, b, tolExp);
   }

   /**
    * Attempts to use preconditioned CGS iteration to solve M x = b for a given
    * right-hand side <code>b</code>. Current numeric values for M are
    * supplied by the argument <code>vals</code>. Otherwise this
    * method behaves identically to
    * {@link #iterativeSolve(double[],double[],int)}.
    *
    * @param vals supplied the current matrix values 
    * @param x returns the solution value
    * @param b supplies the right-hand side
    * @param tolExp exponent for the stopping criterion
    * @return number of iterations performed, negated if unsuccessful
    * @throws ImproperStateException if the matrix has not previously been
    * factored.
    * @throws IllegalArgumentException if there are insufficient values
    * specified by <code>vals</code>, the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size, or if
    * <code>topExp</code> is negative.
    */
   public synchronized int iterativeSolve (
      double[] vals, double[] x, double[] b, int tolExp) {
      checkFactored();
      checkSolveArgs (x, b);
      if (vals.length < myNumVals) {
         throw new IllegalArgumentException (
            "vals is too small: length="+vals.length+
            ", expected size is "+myNumVals);
      }
      else if (tolExp < 0) {
         throw new IllegalArgumentException ("tolExp should not be negative");
      }   
      
      // Some versions of Pardiso have a bug whereby iterative solves fail when
      // the RHS is zero. So check for rhs == 0 and simple return x = 0 when
      // this is the case.
      boolean rhs0 = true;      // assume RHS is zero
      for (int i=0; i<mySize; i++) {
         if (b[i] != 0) {
            rhs0 = false;  // not zero
            break;
         }
      }
      
      int rcode = 0;
      if (rhs0) {
         // zero out solution
         for (int i=0; i<mySize; i++) {
            x[i] = 0;
         }
      } else {
         rcode = doIterativeSolve (myHandle, vals, x, b, tolExp);
      }
      
      if (rcode < 0) {
         myErrMsg = getErrorMessage (rcode%10 - 20);
      } else {
         myErrMsg = null;
      }
      return rcode;
   }

   /**
    * Releases the native resources used by this solver.
    */
   public void dispose() {
      if (myHandle != 0) {
         doRelease (myHandle);
         myHandle = 0;
      }
   }

   public void finalize() {
      dispose();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasAutoIterativeSolving() {
      /*
       * iterative solving disabled for WinXP
       */
      if (System.getProperty ("os.name").equals ("Windows XP")) {
         return false;
      }
      else {
         return true;
      }
   }

   private static void printUsage() {
      System.out.println ("Usage: [-size n] <matrixAndVectorFileName>");
   }

   private static boolean contains (int idx, int[] list) {
      for (int i = 0; i < list.length; i++) {
         if (list[i] == idx) {
            return true;
         }
      }
      return false;
   }

   private static SparseMatrixNd removeRowsAndCols (
      SparseMatrixNd S, int[] idxs) {

      MatrixNd M = new MatrixNd (S);

      LinkedList<Integer> keepIdxs = new LinkedList<Integer>();
      for (int i = 0; i < M.colSize(); i++) {
         if (!contains (i, idxs)) {
            keepIdxs.add (i);
         }
      }
      int newSize = keepIdxs.size();
      int[] keep = new int[newSize];
      int k = 0;
      for (Integer ii : keepIdxs) {
         keep[k++] = ii;
      }
      System.out.println ("newSize=" + newSize);

      MatrixNd Msub = new MatrixNd (newSize, newSize);
      M.getSubMatrix (keep, keep, Msub);
      SparseMatrixNd Snew = new SparseMatrixNd (Msub);
      for (int i = 0; i < newSize; i++) {
         if (Snew.get (i, i) == 0) {
            Snew.setZero (i, i);
         }
      }
      LUDecomposition lud = new LUDecomposition();
      lud.factor (Msub);
      System.out.println ("cond est = " + lud.conditionEstimate (Msub));
      return Snew;
   }

   public static void main (String[] args) {
      String fileName = null;
      int size = -1;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-size")) {
            if (i == args.length - 1) {
               printUsage();
            }
            i++;
            size = Integer.parseInt (args[i]);
         }
         else if (fileName == null && !args[i].startsWith ("-")) {
            fileName = args[i];
         }
         else {
            printUsage();
         }
      }
      if (fileName == null) {
         printUsage();
      }
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (fileName)));
         SparseMatrixNd M = new SparseMatrixNd (0, 0);
         VectorNd b = new VectorNd();
         M.scan (rtok);
         b.scan (rtok);

         PardisoSolver solver = new PardisoSolver();

         if (M.rowSize() != M.colSize()) {
            System.out.println ("Error: matrix is not square");
            System.exit (1);
         }
         if (size == -1 || size > M.rowSize()) {
            size = M.rowSize();
         }
         if (b.size() < size) {
            System.out.println ("Error: b is smaller than size");
         }

         // for (int i=0; i<size; i++)
         // { for (int j=i+1; j<size; j++)
         // { if (Math.abs(M.get(i,j)) < 1e-10)
         // { M.set(i,j,0);
         // M.set(j,i,0);
         // }
         // }
         // }
         // M = removeRowsAndCols (
         // M, new int[] { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
         // 15, 16, 17, 18, 23, 25, 26, 27, 28, 29, 30});

         // try
         // { PrintWriter pw = new PrintWriter (new FileWriter ("bam.txt"));
         // M.write (pw, new NumberFormat("%a"), Matrix.WriteFormat.Sparse);
         // pw.close();
         // }
         // catch (Exception e)
         // { e.printStackTrace();
         // }

         // size = M.rowSize();

         System.out.println ("numNonZeros=" + M.numNonZeroVals());

         b.setSize (size);
         System.out.println ("Scanned system, solving with size=" + size);

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
         M.mul (bcheck, x, size, size);
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

   /**
    * Returns a message describing the reason for failure of
    * the most recent call to any of the <code>analyze()</code>,
    * <code>factor()</code>, or <code>iterativeSolve()</code> methods,
    * or <code>null</code> if the method succeeded.
    *
    * @return most recent error message, if any
    */
   public String getErrorMessage() {
      return myErrMsg;
   }

   /**
    * This method is a hook that gives us access to _exit(), which is
    * is in turn needed to exit ArtiSynth on the MacBook Pro 8.2
    * version of Ubuntu, since otherwise the JVM exit process encounters
    * a SEGV in XQueryExtension. This method was put into PardisoSolver
    * purely ; it has nothing to do with Pardiso per-se.
    */
   public void systemExit (int code) {
      doExit (code);
   }
}
