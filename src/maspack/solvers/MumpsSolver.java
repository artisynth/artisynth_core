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
 * JNI interface to the MUMPS sparse solver. Usage of MUMPS
 * is usually dividing into three phases:
 *
 * <ul>
 * <li>An <i>analyze</i> phase that reorder the matrix to reduce
 * fill-in and performs a symbolic factorization;
 * <li>A <i>factor</i> phase that numerically factors the matrix into
 * a sutiable decomposition;
 * <li>A <i>solve</i> phase that uses the factorization to solve
 * M x = b for some given right-hand side b.
 * </ul>
 *
 * Typical usage of this solver is exemplified by 
 * the following call sequence:
 * <pre>
 *    Matrix M;                 // matrix to be solved
 *    VectorNd x, b;            // solution vector and right-hand side
 *    
 *    MumpsSolver solver = new MumpsSolver();
 *    solver.analyze (M, M.rowSize(), Matrix.SYMMETRIC); // symbolic factorization
 *    solver.factor();          // numeric factorization
 *    solver.solve (x, b);      // solution using factorization
 *    
 *    solver.dispose();         // release resources when we are done
 * </pre>
 * It is <i>very</i> important to call <code>dispose()</code> when the solver
 * is no longer required, in order to release internal native resources that
 * have been allocated for the MUMPS native code.
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
 * <p>MUMPS itself uses an assembled coordinate format, in which the row and
 * column index of every non-zero element is given explicitly. The conversion
 * from CRS is done inside the native layer, once per <code>analyze()</code>
 * call, by expanding the row offsets into row indices. For symmetric
 * matrices, only the upper triangular portion is supplied, which MUMPS
 * accepts directly.
 */
public class MumpsSolver extends DirectSolverBase {

   public static boolean printThreadInfo = true;
   
   public static boolean supportsMultipleRhs = true;
   static String nativeLibrary = "MumpsJNI.5.9.1";

   /**
    * Describes the reorder methods that can be used during the analyze phase
    * to reduce factorization fill-in.
    */
   public enum ReorderMethod {
      AMD,
      AMF,
      SCOTCH,
      PORD,
      METIS,
      QAMD,
      DEFAULT,
   };

   // Error codes returned by MUMPS in INFOG(1). Only those which are likely
   // to arise in this context are named explicitly; all others are reported
   // numerically by getErrorMessage(). Additional information about an error
   // is contained in INFOG(2), which is available from getLastErrorInfo().

   private static final int ERR_PROCESSOR = -1;

   private static final int ERR_NNZ_RANGE = -2;

   private static final int ERR_BAD_JOB = -3;

   private static final int ERR_BAD_PERM = -4;

   private static final int ERR_ANALYSIS_REAL_ALLOC = -5;

   private static final int ERR_STRUCTURALLY_SINGULAR = -6;

   private static final int ERR_ANALYSIS_INT_ALLOC = -7;

   private static final int ERR_INT_WORKSPACE = -8;

   private static final int ERR_REAL_WORKSPACE = -9;

   private static final int ERR_SINGULAR = -10;

   private static final int ERR_SOLVE_REAL_WORKSPACE = -11;

   private static final int ERR_REFINE_REAL_WORKSPACE = -12;

   private static final int ERR_ALLOC = -13;

   private static final int ERR_SOLVE_INT_WORKSPACE = -14;

   private static final int ERR_REFINE_INT_WORKSPACE = -15;

   private static final int ERR_N_RANGE = -16;

   private static final int ERR_SEND_BUFFER = -17;

   private static final int ERR_MAX_MEMORY = -19;

   private static final int ERR_RECV_BUFFER = -20;

   private static final int ERR_BAD_POINTER = -22;

   private static final int ERR_NOT_POSITIVE_DEFINITE = -40;

   private static final int ERR_NO_FACTORS = -44;

   private static final int ERR_ORDERING = -50;

   private static final int ERR_ORDERING_OVERFLOW = -51;

   private static final int ERR_ORDERING_INT_SIZE = -52;

   private static final int ERR_INCONSISTENT_INPUT = -53;

   private static final int ERR_CANT_LOAD_LIBRARIES = -1000;

   /**
    * Warning bit (returned in INFOG(1)) indicating that entries with
    * out-of-range indices were found and ignored.
    */
   public static final int WARN_INDEX_OUT_OF_RANGE = 0x1;

   /**
    * Warning bit indicating that the max-norm of the solution is close
    * to zero.
    */
   public static final int WARN_ZERO_SOLUTION = 0x2;

   /**
    * Warning bit indicating insufficient memory to compact the internal
    * work array at the end of the factorization.
    */
   public static final int WARN_COMPACT_MEMORY = 0x4;

   /**
    * Warning bit indicating that iterative refinement did not converge
    * within the allowed number of steps.
    */
   public static final int WARN_REFINEMENT = 0x8;

   /**
    * Warning bit returned by the rank revealing feature.
    */
   public static final int WARN_RANK_REVEALING = 0x10;

   private static final int INIT_UNKNOWN = 0;

   private static final int INIT_LIBRARIES_LOADED = 1;

   private static final int INIT_OK = 2;

   static int myInitStatus = INIT_UNKNOWN;
   
   public static boolean DEFAULT_SHOW_PERTURBED_PIVOTS = true;
   static boolean myShowPerturbedPivots = DEFAULT_SHOW_PERTURBED_PIVOTS;

   /**
    * Returns a message corresponding to a MUMPS error code, as returned in
    * INFOG(1).
    *
    * @param code MUMPS error code
    * @return message describing the error
    */
   static String getErrorMessage (int code) {
      return getErrorMessage (code, 0);
   }

   /**
    * Returns a message corresponding to a MUMPS error code, as returned in
    * INFOG(1), together with the auxiliary information contained in INFOG(2).
    * Most MUMPS error codes are not fully descriptive without the latter.
    *
    * @param code MUMPS error code, from INFOG(1)
    * @param info auxiliary error information, from INFOG(2)
    * @return message describing the error
    */
   static String getErrorMessage (int code, int info) {
      switch (code) {
         case ERR_PROCESSOR: {
            return "Error occurred on processor " + info;
         }
         case ERR_NNZ_RANGE: {
            return "Number of non-zeros out of range: " + info;
         }
         case ERR_BAD_JOB: {
            return "Invalid or out-of-sequence MUMPS job value " + info;
         }
         case ERR_BAD_PERM: {
            return "Error in user permutation at position " + info;
         }
         case ERR_ANALYSIS_REAL_ALLOC: {
            return "Real workspace allocation failed during analysis, size "
               + memSize(info);
         }
         case ERR_STRUCTURALLY_SINGULAR: {
            return "Matrix is structurally singular; structural rank " + info;
         }
         case ERR_ANALYSIS_INT_ALLOC: {
            return "Integer workspace allocation failed during analysis, size "
               + memSize(info);
         }
         case ERR_INT_WORKSPACE: {
            return "Internal integer workspace too small for factorization; "
               + "increase workspace increase (ICNTL(14))";
         }
         case ERR_REAL_WORKSPACE: {
            return "Internal real workspace too small for factorization "
               + "(missing " + memSize(info) + " entries); "
               + "increase workspace increase (ICNTL(14))";
         }
         case ERR_SINGULAR: {
            return "Numerically singular matrix, or zero pivot encountered "
               + "after " + info + " eliminated pivots";
         }
         case ERR_SOLVE_REAL_WORKSPACE: {
            return "Internal real workspace too small for solve "
               + "(missing " + memSize(info) + " entries)";
         }
         case ERR_REFINE_REAL_WORKSPACE: {
            return "Internal real workspace too small for iterative refinement";
         }
         case ERR_ALLOC: {
            return "Workspace allocation failed during factor or solve, size "
               + memSize(info);
         }
         case ERR_SOLVE_INT_WORKSPACE: {
            return "Internal integer workspace too small for solve; "
               + "increase workspace increase (ICNTL(14))";
         }
         case ERR_REFINE_INT_WORKSPACE: {
            return "Internal integer workspace too small for "
               + "iterative refinement";
         }
         case ERR_N_RANGE: {
            return "Matrix size out of range: " + info;
         }
         case ERR_SEND_BUFFER: {
            return "Internal send buffer too small; "
               + "increase workspace increase (ICNTL(14))";
         }
         case ERR_MAX_MEMORY: {
            return "Maximum working memory (ICNTL(23)) too small for "
               + "factorization; missing " + memSize(info) + " entries";
         }
         case ERR_RECV_BUFFER: {
            return "Internal receive buffer too small; "
               + "increase workspace increase (ICNTL(14))";
         }
         case ERR_BAD_POINTER: {
            return "Invalid array supplied to MUMPS: " + pointerName(info);
         }
         case ERR_NOT_POSITIVE_DEFINITE: {
            return "Matrix declared SPD but a negative or null pivot "
               + "was encountered";
         }
         case ERR_NO_FACTORS: {
            return "Solve requested but factors are not available";
         }
         case ERR_ORDERING: {
            return "Fill reducing ordering failed";
         }
         case ERR_ORDERING_OVERFLOW: {
            return "Graph too large for 32 bit ordering package";
         }
         case ERR_ORDERING_INT_SIZE: {
            return "Ordering package not built with matching integer size";
         }
         case ERR_INCONSISTENT_INPUT: {
            return "Inconsistent input data between consecutive calls";
         }
         case ERR_CANT_LOAD_LIBRARIES: {
            return "Unable to load MUMPS library";
         }
         default: {
            if (code < 0) {
               return "MUMPS error " + code + " (INFOG(2)=" + info + ")";
            }
            else if (code > 0) {
               return getWarningMessage (code, info);
            }
            else {
               return "No error";
            }
         }
      }
   }

   /**
    * Returns a message corresponding to a positive (warning) value of
    * INFOG(1). Warnings are bit-coded and may be combined.
    *
    * @param code warning code, from INFOG(1)
    * @param info auxiliary information, from INFOG(2)
    * @return message describing the warning(s)
    */
   static String getWarningMessage (int code, int info) {
      StringBuilder sb = new StringBuilder();
      if ((code & WARN_INDEX_OUT_OF_RANGE) != 0) {
         sb.append (info + " matrix entries had out-of-range indices");
      }
      if ((code & WARN_ZERO_SOLUTION) != 0) {
         appendWarning (sb, "solution max-norm is close to zero");
      }
      if ((code & WARN_COMPACT_MEMORY) != 0) {
         appendWarning (sb, "insufficient memory to compact work array");
      }
      if ((code & WARN_REFINEMENT) != 0) {
         appendWarning (sb, "iterative refinement did not converge");
      }
      if ((code & WARN_RANK_REVEALING) != 0) {
         appendWarning (sb, "inertia may be inconsistent with deficiency");
      }
      if (sb.length() == 0) {
         sb.append ("MUMPS warning " + code + " (INFOG(2)=" + info + ")");
      }
      return sb.toString();
   }

   private static void appendWarning (StringBuilder sb, String msg) {
      if (sb.length() > 0) {
         sb.append ("; ");
      }
      sb.append (msg);
   }

   /**
    * Formats a MUMPS size quantity. MUMPS returns such quantities as
    * negative numbers when they should be multiplied by one million.
    */
   private static String memSize (int info) {
      if (info < 0) {
         return (-(long)info*1000000) + "";
      }
      else {
         return info + "";
      }
   }

   /**
    * Returns the name of the MUMPS array indicated by INFOG(2) for
    * error -22.
    */
   private static String pointerName (int info) {
      switch (info) {
         case 1: return "IRN";
         case 2: return "JCN";
         case 3: return "PERM_IN";
         case 4: return "A";
         case 5: return "ROWSCA";
         case 6: return "COLSCA";
         case 7: return "RHS";
         default: return "array " + info;
      }
   }

   private native long doInit();

   private native int doGetInitError (long handle);

   /**
    * Returns the auxiliary error information INFOG(2) associated with the
    * most recent MUMPS call.
    */
   private native int doGetLastErrorInfo (long handle);

   private native int doGetNumThreads (long handle);
   private native int doSetNumThreads (long handle, int num);

   private native long doGetNumNonZerosInFactors (long handle);
   private native int doGetNumNegEigenvalues (long handle);
   private native int doGetNumPosEigenvalues (long handle);

   private native int doGetNumTinyPivots (long handle);
   private native int doGetNumNullPivots (long handle);
   private native int doGetNumDelayedPivots (long handle);
   private native int doGetFirstNullPivot (long handle);

   private native int doGetAnalysisMemoryUsage (long handle);
   private native int doGetPeakAnalysisMemoryUsage (long handle);
   private native int doGetFactorSolveMemoryUsage (long handle);

   private native int doGetMaxRefinementSteps (long handle);
   private native int doSetMaxRefinementSteps (long handle, int nsteps);
   private native int doGetNumRefinementSteps (long handle);

   // values for ICNTL(7), which selects the reorder method

   private static final int AMD_REORDER = 0;
   private static final int USER_REORDER = 1;
   private static final int AMF_REORDER = 2;
   private static final int SCOTCH_REORDER = 3;
   private static final int PORD_REORDER = 4;
   private static final int METIS_REORDER = 5;
   private static final int QAMD_REORDER = 6;
   private static final int AUTO_REORDER = 7;

   private native int doGetReorderMethod (long handle);
   private native int doSetReorderMethod (long handle, int method);
   private native int doGetReorderMethodUsed (long handle);

   private native double doGetStaticPivotTolerance (long handle);
   private native int doSetStaticPivotTolerance (long handle, double tol);

   private native int doGetNullPivotDetection (long handle);
   private native int doSetNullPivotDetection (long handle, int enable);

   private native double doGetNullPivotThreshold (long handle);
   private native int doSetNullPivotThreshold (long handle, double thresh);

   private native int doGetApplyScaling (long handle);
   private native int doSetApplyScaling (long handle, int apply);

   private native int doGetApplyWeightedMatchings (long handle);
   private native int doSetApplyWeightedMatchings (long handle, int apply);

   private native int doGetSymOrderingStrategy (long handle);
   private native int doSetSymOrderingStrategy (long handle, int strategy);

   private native int doGetWorkspaceIncrease (long handle);
   private native int doSetWorkspaceIncrease (long handle, int percent);

   private native int doGetMaxWorkingMemory (long handle);
   private native int doSetMaxWorkingMemory (long handle, int mbytes);

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

   private native int doSolve (long handle, double[] x, double[] b);

   private native int doSolve (
      long handle, double[] xvecs, double[] bvecs, int nrhs);

   private native void doRelease (long handle);


   /**
    * Attempts to load the native libraries needed for MUMPS.
    */
   private static void doLoadLibraries() {
      try {
         NativeLibraryManager.setFlags (NativeLibraryManager.VERBOSE);
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
         NativeLibraryManager.load (nativeLibrary);
         myInitStatus = INIT_LIBRARIES_LOADED;
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
         myInitStatus = ERR_CANT_LOAD_LIBRARIES;
      }
   }

   /**
    * Creates a new MumpsSolver object.
    */
   public MumpsSolver() {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }
      if (myInitStatus < 0) {
         throw new UnsupportedOperationException (
            "MUMPS not available: " + getInitErrorMessage());
      }
      myVals = new double[0];
      myColIdxs = new int[0];
      myRowOffs = new int[0];

      // create the handle here because earlier JNI implementations of
      // setNumThreads required this internally:
      myHandle = doInit();
      setNumThreads (myDefaultNumThreads);
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
         System.out.println ("MUMPS: max threads=" + getNumThreads());
      }
      return err;
   }

   /**
    * Returns true if the MUMPS solver is available. A solver might
    * <i>not</i> be available if the MUMPS native libraries cannot
    * be loaded for some reason.
    *
    * @return true if MUMPS is available.
    */
   public static boolean isAvailable () {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }     
      if (myInitStatus == INIT_LIBRARIES_LOADED) {
         MumpsSolver test = new MumpsSolver();
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






   /**
    * Enables/disables the "num perturbed pivots" message (which usually
    * indicates a singular or ill-conditioned solve). See {@link
    * #getNumPerturbedPivots}.
    *
    * @param enable if {@code true}, enables the message
    */
   public static void setShowPerturbedPivots (boolean enable) {
      myShowPerturbedPivots = enable;
   }

   /**
    * Queries whether the "num perturbed pivots" message is enabled.
    *
    * @return {@code true} if the message is enabled
    */
   public static boolean getShowPerturbedPivots () {
      return myShowPerturbedPivots;
   }

   /**
    * Sets the maximum number of iterative refinement steps that MUMPS should
    * perform after a solve (MUMPS parameter ICNTL(10)). Setting this to 0
    * (the MUMPS default) disables iterative refinement. Positive values
    * specify a maximum number of steps, with a convergence test performed at
    * each step, while negative values specify a fixed number of steps
    * ({@code -nsteps}) with no convergence test. More iterative refinement
    * steps will increase solution accuracy but slow down the solve.
    *
    * <p><b>Note:</b> MUMPS silently disables iterative refinement whenever
    * more than one right hand side is solved for.
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
    * Returns the maximum number of iterative refinement steps that MUMPS
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
    * Returns the number of iterative refinement steps that MUMPS
    * actually performed during the most recent call to {@link #solve solve()}
    * (MUMPS parameter INFOG(15)).
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

   private ReorderMethod methodFromCode (int m) {
      switch (m) {
         case AMD_REORDER: {
            return ReorderMethod.AMD;
         }
         case AMF_REORDER: {
            return ReorderMethod.AMF;
         }
         case SCOTCH_REORDER: {
            return ReorderMethod.SCOTCH;
         }
         case PORD_REORDER: {
            return ReorderMethod.PORD;
         }
         case METIS_REORDER: {
            return ReorderMethod.METIS;
         }
         case QAMD_REORDER: {
            return ReorderMethod.QAMD;
         }
         case USER_REORDER:
         case AUTO_REORDER: {
            return ReorderMethod.DEFAULT;
         }
         default:
            throw new UnsupportedOperationException (
               "MUMPS returned unknown reorder method: " + m);
      }
   }

   /**
    * Gets the reorder method that is used during the analyze phase to
    * reduced factorization fill-in (MUMPS parameter ICNTL(7)).
    *
    * @return current reorder method
    * @see #setReorderMethod
    */
   public synchronized ReorderMethod getReorderMethod () {
      if (myHandle == 0) {
         initialize();
      }
      return methodFromCode (doGetReorderMethod (myHandle));
   }

   /**
    * Returns the reorder method that MUMPS actually used during the most
    * recent analyze phase (MUMPS parameter INFOG(7)). This may differ from
    * the value returned by {@link #getReorderMethod}, either because
    * <code>DEFAULT</code> was requested, or because the requested method was
    * not available or not compatible with other settings.
    *
    * @return reorder method used by the most recent analyze
    */
   public synchronized ReorderMethod getReorderMethodUsed () {
      if (myHandle == 0) {
         initialize();
      }
      return methodFromCode (doGetReorderMethodUsed (myHandle));
   }

   /**
    * Sets the reorder method that should be used during the analyze phase to
    * reduced factorization fill-in. Setting the method to <code>DEFAULT</code>
    * will cause MUMPS to choose the method automatically. For the symmetric
    * indefinite systems typically encountered in ArtiSynth,
    * <code>METIS</code> is usually the best choice.
    *
    * @param method reorder method
    * @see #getReorderMethod
    */
   public synchronized void setReorderMethod (ReorderMethod method) {
      if (myHandle == 0) {
         initialize();
      }
      int m = AUTO_REORDER;
      switch (method) {
         case AMD: {
            m = AMD_REORDER;
            break;
         }
         case AMF: {
            m = AMF_REORDER;
            break;
         }
         case SCOTCH: {
            m = SCOTCH_REORDER;
            break;
         }
         case PORD: {
            m = PORD_REORDER;
            break;
         }
         case METIS: {
            m = METIS_REORDER;
            break;
         }
         case QAMD: {
            m = QAMD_REORDER;
            break;
         }
         case DEFAULT: {
            m = AUTO_REORDER;
            break;
         }
         default:
            throw new UnsupportedOperationException (
               "Unknown reorder method: " + method);
      }
      doSetReorderMethod (myHandle, m);
   }

   /**
    * Sets the threshold used for static pivoting (MUMPS parameter CNTL(4)).
    * Static pivoting replaces pivots whose magnitude is smaller than the
    * threshold by the threshold itself, thus perturbing the matrix instead of
    * postponing the pivot. The number of pivots so modified is returned by
    * {@link #getNumTinyPivots}. Values are interpreted as follows:
    *
    * <dl>
    * <dt>{@code tol < 0}</dt>
    * <dd>static pivoting is disabled (the MUMPS default);
    * <dt>{@code tol == 0}</dt>
    * <dd>static pivoting is enabled, with the threshold determined
    * automatically as {@code sqrt(eps)*norm(M)};
    * <dt>{@code tol > 0}</dt>
    * <dd>static pivoting is enabled, using <code>tol</code> as an
    * <i>absolute</i> threshold.
    * </dl>
    *
    * <p><b>Note:</b> unlike the pivot perturbation used by Pardiso, this is
    * an absolute quantity rather than a factor applied to a norm of the
    * matrix. Also, MUMPS ignores static pivoting when null pivot detection is
    * enabled (see {@link #setNullPivotDetection}), which is the default for
    * this solver.
    *
    * @param tol static pivoting threshold
    * @see #getStaticPivotTolerance
    */
   public synchronized void setStaticPivotTolerance (double tol) {
      if (myHandle == 0) {
         initialize();
      }
      doSetStaticPivotTolerance (myHandle, tol);
   }

   /**
    * Returns the threshold used for static pivoting.
    *
    * @return static pivoting threshold
    * @see #setStaticPivotTolerance
    */
   public synchronized double getStaticPivotTolerance () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetStaticPivotTolerance (myHandle);
   }

   /**
    * Enables or disables the detection of null pivot rows (MUMPS parameter
    * ICNTL(24)). When enabled, pivots whose magnitude falls below the
    * threshold described for {@link #setNullPivotThreshold} are treated as
    * null and the factorization continues, with the number of such pivots
    * returned by {@link #getNumNullPivots}. When disabled, a null pivot
    * instead causes the factorization to fail with a
    * <code>NumericalException</code>.
    *
    * <p>This is enabled by default, since it most closely matches the
    * behavior of Pardiso, which perturbs zero pivots and continues.
    *
    * @param enable if {@code true}, enables null pivot detection
    * @see #getNullPivotDetection
    */
   public synchronized void setNullPivotDetection (boolean enable) {
      if (myHandle == 0) {
         initialize();
      }
      doSetNullPivotDetection (myHandle, enable ? 1 : 0);
   }

   /**
    * Queries whether null pivot detection is enabled.
    *
    * @return {@code true} if null pivot detection is enabled
    * @see #setNullPivotDetection
    */
   public synchronized boolean getNullPivotDetection () {
      if (myHandle == 0) {
         initialize();
      }
      return (doGetNullPivotDetection (myHandle) != 0);
   }

   /**
    * Sets the threshold used to identify null pivots (MUMPS parameter
    * CNTL(3)), when null pivot detection is enabled. Values are interpreted
    * as follows:
    *
    * <dl>
    * <dt>{@code thresh > 0}</dt>
    * <dd>the threshold is {@code thresh*norm(M)};
    * <dt>{@code thresh == 0}</dt>
    * <dd>the threshold is determined automatically (the default);
    * <dt>{@code thresh < 0}</dt>
    * <dd>the threshold is the absolute value {@code -thresh}.
    * </dl>
    *
    * @param thresh null pivot threshold
    * @see #getNullPivotThreshold
    */
   public synchronized void setNullPivotThreshold (double thresh) {
      if (myHandle == 0) {
         initialize();
      }
      doSetNullPivotThreshold (myHandle, thresh);
   }

   /**
    * Returns the threshold used to identify null pivots.
    *
    * @return null pivot threshold
    * @see #setNullPivotThreshold
    */
   public synchronized double getNullPivotThreshold () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNullPivotThreshold (myHandle);
   }

   /**
    * Sets whether or not MUMPS should apply matrix scaling to its
    * factorizations (MUMPS parameter ICNTL(8)). Scaling is controlled by
    * <code>enable</code> as follows: {@code enable > 0} enables scaling,
    * {@code enable = 0} disables scaling, and {@code enable < 0} causes MUMPS
    * to choose automatically (the default). The scaling actually applied can
    * be queried using {@link #getApplyScaling}, which reads it back from
    * MUMPS after the analyze and factor phases.
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
    * Sets whether or not MUMPS should compute a weighted matching (maximum
    * transversal) for the matrix (MUMPS parameter ICNTL(6)). For unsymmetric
    * matrices this permutes the matrix to a zero-free diagonal; for symmetric
    * matrices it is used to identify 1 x 1 and 2 x 2 pivots, and is
    * recommended for highly indefinite systems such as the saddle point
    * problems arising in ArtiSynth. Weighted matchings are controlled by
    * <code>enable</code> as follows: {@code enable > 0} enables them, {@code
    * enable = 0} disables them, and {@code enable < 0} causes MUMPS to choose
    * automatically (the default). Weighted matchings are always disabled for
    * SPD matrices.
    *
    * @param enable enables/disables weighted matchings
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
    * Sets the ordering strategy that MUMPS uses for symmetric indefinite
    * matrices (MUMPS parameter ICNTL(12)). Possible values are 0 (automatic
    * choice, the default), 1 (usual ordering), 2 (ordering on the compressed
    * graph) and 3 (constrained ordering, available only with the
    * <code>AMF</code> reorder method). Setting this to 2 can significantly
    * reduce the number of delayed pivots (see {@link #getNumDelayedPivots})
    * for augmented systems, at the cost of increased fill-in.
    *
    * @param strategy symmetric ordering strategy
    * @see #getSymOrderingStrategy
    */
   public synchronized void setSymOrderingStrategy (int strategy) {
      if (myHandle == 0) {
         initialize();
      }
      doSetSymOrderingStrategy (myHandle, strategy);
   }

   /**
    * Returns the ordering strategy used for symmetric indefinite matrices.
    *
    * @return symmetric ordering strategy
    * @see #setSymOrderingStrategy
    */
   public synchronized int getSymOrderingStrategy () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetSymOrderingStrategy (myHandle);
   }

   /**
    * Sets the percentage by which MUMPS should increase the working space
    * estimated during the analyze phase (MUMPS parameter ICNTL(14)). Setting
    * this to a value {@code < 0} causes MUMPS to use its default value
    * (between 20 and 35 percent). Numerical pivoting can cause significantly
    * more fill-in than was predicted by the analysis, in which case the
    * factorization will fail because the working space is too small; this
    * solver responds by automatically increasing the workspace and retrying,
    * but setting a larger value here in advance will avoid the wasted
    * factorizations.
    *
    * @param percent percentage increase in the estimated working space
    * @see #getWorkspaceIncrease
    */
   public synchronized void setWorkspaceIncrease (int percent) {
      if (myHandle == 0) {
         initialize();
      }
      doSetWorkspaceIncrease (myHandle, percent);
   }

   /**
    * Returns the percentage by which MUMPS increases the working space
    * estimated during the analyze phase.
    *
    * @return percentage increase in the estimated working space
    * @see #setWorkspaceIncrease
    */
   public synchronized int getWorkspaceIncrease () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetWorkspaceIncrease (myHandle);
   }

   /**
    * Sets the maximum size of the working memory, in Mbytes, that MUMPS is
    * allowed to allocate (MUMPS parameter ICNTL(23)). Setting this to a value
    * {@code <= 0} (the default) places no limit other than that implied by
    * {@link #setWorkspaceIncrease}.
    *
    * @param mbytes maximum working memory, in Mbytes
    * @see #getMaxWorkingMemory
    */
   public synchronized void setMaxWorkingMemory (int mbytes) {
      if (myHandle == 0) {
         initialize();
      }
      doSetMaxWorkingMemory (myHandle, mbytes);
   }

   /**
    * Returns the maximum size of the working memory, in Mbytes, that MUMPS is
    * allowed to allocate.
    *
    * @return maximum working memory, in Mbytes
    * @see #setMaxWorkingMemory
    */
   public synchronized int getMaxWorkingMemory () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetMaxWorkingMemory (myHandle);
   }

   /**
    * Returns the number of non-zeros elements in the factorization. After a
    * <code>factor()</code> call, this is the actual number (MUMPS parameter
    * INFOG(29)); otherwise, it is the number estimated by the most recent
    * <code>analyze()</code> call (MUMPS parameter INFOG(20)).
    *
    * @return number non-zero elements in the factorization.
    */
   public synchronized long getNumNonZerosInFactors () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumNonZerosInFactors (myHandle);
   }

   /**
    * Returns the number of negative eigenvalues that were detected during the
    * most recent numeric factorization of a symmetric indefinite matrix (i.e.,
    * during the last <code>factor()</code> call, MUMPS parameter INFOG(12)).
    * For matrices that are not symmetric indefinite, -1 is returned.
    *
    * <p>Note that when null pivot detection is enabled (the default), null
    * pivots are excluded from this count, even if their sign is negative.
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
    * during the last <code>factor()</code> call). MUMPS does not report this
    * directly; it is inferred from the matrix size and the numbers of
    * negative and null pivots. For matrices that are not symmetric
    * indefinite, -1 is returned.
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
    * Returns the number of pivots that were perturbed during the most recent
    * numeric factorization (i.e., during the last <code>factor()</code>
    * call). This is the sum of the null pivots detected (see {@link
    * #getNumNullPivots}) and the tiny pivots modified by static pivoting (see
    * {@link #getNumTinyPivots}); only one of these mechanisms can be active
    * at a time. As with the perturbed pivots reported by Pardiso, a non-zero
    * value generally indicates a singular, or very nearly singular, matrix.
    *
    * @return number of perturbed pivots
    */      
   public synchronized int getNumPerturbedPivots () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumNullPivots (myHandle) + doGetNumTinyPivots (myHandle);
   }

   /**
    * Returns the number of null pivot rows that were detected during the most
    * recent numeric factorization (MUMPS parameter INFOG(28)), which
    * corresponds to the deficiency of the matrix. This will be zero unless
    * null pivot detection is enabled (see {@link #setNullPivotDetection}).
    *
    * @return number of null pivots
    */
   public synchronized int getNumNullPivots () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumNullPivots (myHandle);
   }

   /**
    * Returns the number of tiny pivots that were modified by static pivoting
    * during the most recent numeric factorization (MUMPS parameter
    * INFOG(25)). This will be zero unless static pivoting is enabled (see
    * {@link #setStaticPivotTolerance}).
    *
    * @return number of tiny pivots
    */
   public synchronized int getNumTinyPivots () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumTinyPivots (myHandle);
   }

   /**
    * Returns the number of delayed pivots that occurred during the most
    * recent numeric factorization (MUMPS parameter INFOG(13)). Delayed pivots
    * arise from numerical pivoting and increase both fill-in and
    * factorization time; a number exceeding 10 percent of the matrix size
    * indicates numerical problems, which may be alleviated by adjusting
    * {@link #setApplyWeightedMatchings setApplyWeightedMatchings()}, {@link
    * #setApplyScaling setApplyScaling()}, or {@link #setSymOrderingStrategy
    * setSymOrderingStrategy()}.
    *
    * @return number of delayed pivots
    */
   public synchronized int getNumDelayedPivots () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetNumDelayedPivots (myHandle);
   }

   /**
    * If the solver detected null pivots during the most recent
    * factorization, this method returns the row number of the first such
    * pivot, as identified by the MUMPS PIVNUL_LIST array. Otherwise, 0 is
    * returned. For an SPD matrix, this indicates the row at which the matrix
    * was found not to be positive definite.
    * 
    * @return location of first null pivot, if any
    */
   public synchronized int getSPDZeroPivot () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetFirstNullPivot (myHandle);
   }

   /**
    * Returns the maximum amount of memory (in kbytes) that MUMPS estimated,
    * during the most recent <code>analyze()</code> call, would be needed by a
    * single process for an in-core factorization (MUMPS parameter INFOG(16)).
    * MUMPS reports memory in Mbytes, so the returned value is a multiple of
    * 1024.
    *
    * @return estimated peak memory usage for factorization.
    */
   public synchronized int getPeakAnalysisMemoryUsage () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetPeakAnalysisMemoryUsage (myHandle);
   }

   /**
    * Returns the total amount of memory (in kbytes), summed over all
    * processes, that MUMPS estimated during the most recent
    * <code>analyze()</code> call would be needed for an in-core
    * factorization (MUMPS parameter INFOG(17)). MUMPS reports memory in
    * Mbytes, so the returned value is a multiple of 1024.
    *
    * @return estimated total memory usage for factorization.
    */
   public synchronized int getAnalysisMemoryUsage () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetAnalysisMemoryUsage (myHandle);
   }

   /**
    * Returns the memory (in kbytes) actually used during the most recent
    * <code>factor()</code> call (MUMPS parameter INFOG(21)). MUMPS reports
    * memory in Mbytes, so the returned value is a multiple of 1024.
    *
    * @return memory actually used during factor and solve steps.
    */
   public synchronized int getFactorSolveMemoryUsage () {
      if (myHandle == 0) {
         initialize();
      }
      return doGetFactorSolveMemoryUsage (myHandle);
   }

   



   /**
    * {@inheritDoc}
    */
   public boolean hasMultipleRhsSolves() {
      return true;
   }


   /**
    * {@inheritDoc}
    */
   public boolean hasIterativeSolves() {
      return false;
   }



   // ------------------------------------------------------------------
   // hook methods required by DirectSolverBase
   // ------------------------------------------------------------------

   protected String getSolverName() {
      return "MUMPS";
   }

   protected long createSolverHandle() {
      return doInit();
   }

   protected void releaseSolverHandle (long handle) {
      doRelease (handle);
   }

   protected int setMatrixNative (
      double[] vals, int[] rowOffs, int[] colIdxs,
      int size, int numVals, int type) {

      if ((type & Matrix.SYMMETRIC) != 0) {
         if ((type & Matrix.POSITIVE_DEFINITE) != 0) {
            return doSetSPDMatrix (
               myHandle, vals, rowOffs, colIdxs, size, numVals);
         }
         else {
            return doSetSymmetricMatrix (
               myHandle, vals, rowOffs, colIdxs, size, numVals);
         }
      }
      else {
         return doSetMatrix (myHandle, vals, rowOffs, colIdxs, size, numVals);
      }
   }

   protected int factorMatrixNative (double[] vals) {
      return doFactorMatrix (myHandle, vals);
   }

   protected int solveNative (double[] x, double[] b, int nrhs) {
      if (nrhs == 1) {
         return doSolve (myHandle, x, b);
      }
      else {
         return doSolve (myHandle, x, b, nrhs);
      }
   }

   /**
    * Returns the message for a MUMPS return code, using the auxiliary
    * information (INFOG(2)) associated with the most recent MUMPS call.
    */
   protected String errorMessage (int rcode) {
      return getErrorMessage (rcode, doGetLastErrorInfo (myHandle));
   }

   protected boolean showPerturbedPivots() {
      return myShowPerturbedPivots;
   }

   protected int setNumThreadsNative (int num) {
      return doSetNumThreads (myHandle, num);
   }

   protected int getNumThreadsNative() {
      return doGetNumThreads (myHandle);
   }

}
