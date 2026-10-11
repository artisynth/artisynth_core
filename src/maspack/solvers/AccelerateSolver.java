package maspack.solvers;

import maspack.fileutil.NativeLibraryManager;
import maspack.matrix.Matrix;

/**
 * A direct solver based on the sparse factorizations in Apple's Accelerate
 * framework (Sparse/Solve.h), for comparison with MUMPS on Arm-based Macs,
 * where Pardiso is not available.
 *
 * <p>Symmetric matrices are factored with LDL^T with threshold partial
 * pivoting (SparseFactorizationLDLTTPP), which is the only Accelerate LDL^T
 * variant that is reliable for saddle point (KKT) systems: the
 * Bunch-Kaufman variant (SBK) returns wrong solutions without reporting an
 * error, and the unpivoted one fails on the zero diagonal block.
 * Unsymmetric matrices are factored with LU with threshold partial pivoting
 * (SparseFactorizationLUTPP), which Accelerate provides only on macOS 15.5
 * and later; on earlier systems, {@link #isLUAvailable} returns false and
 * analyzing an unsymmetric matrix fails. The fill-reducing ordering
 * defaults to METIS; it can be changed with the environment variable
 * ACCELERATE_ORDER (amd, metis or mtmetis).
 *
 * <p>Each solve is followed by iterative refinement steps (default 1, as
 * for Pardiso with symmetric matrices; see {@link #setMaxRefinementSteps}),
 * which Accelerate does not provide itself: the residual r = b - A x is
 * computed from the factored matrix, A dx = r is solved, and x += dx. The
 * environment variable ACCELERATE_REFINE sets the initial number of steps.
 *
 * <p>Iterative (hybrid) solves are supported, using the most recent
 * factorization as a preconditioner; see {@link #iterativeSolve}.
 *
 * <p>Accelerate parallelizes internally and provides no thread control, so
 * the thread count methods have no effect, and results can differ slightly
 * from run to run.
 */
public class AccelerateSolver extends DirectSolverBase {

   private static final int INIT_UNKNOWN = 0;
   private static final int INIT_LIBRARIES_LOADED = 1;
   private static final int ERR_CANT_LOAD_LIBRARIES = -1000;
   private static int myInitStatus = INIT_UNKNOWN;

   static String nativeLibrary = "AccelerateJNI.1.0";

   // native return codes, besides Accelerate's SparseStatus values (0, -1 to
   // -4), which are returned unchanged
   private static final int ERR_NOT_SYMMETRIC = -100;
   private static final int ERR_NO_MATRIX = -101;
   private static final int ERR_NO_FACTORIZATION = -102;
   private static final int ERR_OUT_OF_MEMORY = -103;
   private static final int ERR_LU_UNAVAILABLE = -104;

   private static native long doInit();
   private native void doRelease (long handle);
   private native int doSetMatrix (
      long handle, double[] vals, int[] rowOffs, int[] colIdxs,
      int size, int numVals, boolean symmetric);
   private native int doFactor (long handle, double[] vals);
   private native int doSolve (long handle, double[] x, double[] b, int nrhs);
   private native long doGetNumNonZerosInFactors (long handle);
   private native int doGetInertia (long handle, int[] posZeroNeg);
   private native String doGetLastError (long handle);
   private native int doSetMaxRefinementSteps (long handle, int nsteps);
   private native int doGetMaxRefinementSteps (long handle);
   private native int doGetNumRefinementSteps (long handle);
   private native int doIterativeSolve (
      long handle, double[] vals, double[] x, double[] b, double tol,
      int method, int maxSolves, int restart, boolean critical);
   private native int doGetLastIterativeSolves (long handle);
   private native double doGetLastIterativeResidual (long handle);
   private native void doGetLastIterativeTimes (long handle, double[] times);
   private static native int doGetPhysicalCoreCount();
   private static native boolean doIsLUAvailable();

   private int myNumThreads = -1;

   private static void doLoadLibraries() {
      try {
         NativeLibraryManager.setFlags (NativeLibraryManager.VERBOSE);
         NativeLibraryManager.load (nativeLibrary);
         myInitStatus = INIT_LIBRARIES_LOADED;
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
         myInitStatus = ERR_CANT_LOAD_LIBRARIES;
      }
      catch (UnsatisfiedLinkError e) {
         System.out.println (e.getMessage());
         myInitStatus = ERR_CANT_LOAD_LIBRARIES;
      }
   }

   /**
    * Creates a new AccelerateSolver.
    */
   public AccelerateSolver() {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }
      if (myInitStatus < 0) {
         throw new UnsupportedOperationException (
            "Accelerate solver not available: can't load native library");
      }
      initialize();
   }

   /**
    * Returns true if the native library for this solver can be loaded.
    *
    * @return true if this solver is available
    */
   public static boolean isAvailable() {
      if (myInitStatus == INIT_UNKNOWN) {
         doLoadLibraries();
      }
      return myInitStatus >= 0;
   }

   /**
    * Returns true if this solver is available and can factor unsymmetric
    * matrices, which requires macOS 15.5 or later.
    *
    * @return true if unsymmetric matrices are supported
    */
   public static boolean isLUAvailable() {
      return isAvailable() && doIsLUAvailable();
   }

   /**
    * {@inheritDoc}
    */
   public synchronized long getNumNonZerosInFactors() {
      if (myHandle == 0) {
         return -1;
      }
      return doGetNumNonZerosInFactors (myHandle);
   }

   /**
    * Returns the number of negative eigenvalues (pivots) of the most recent
    * factorization, or -1 if this is not available (as for unsymmetric
    * matrices).
    */
   public synchronized int getNumNegEigenvalues() {
      int[] inertia = new int[3];
      if (myHandle == 0 || doGetInertia (myHandle, inertia) != 0) {
         return -1;
      }
      return inertia[2];
   }

   /**
    * Returns the number of positive eigenvalues (pivots) of the most recent
    * factorization, or -1 if this is not available (as for unsymmetric
    * matrices).
    */
   public synchronized int getNumPosEigenvalues() {
      int[] inertia = new int[3];
      if (myHandle == 0 || doGetInertia (myHandle, inertia) != 0) {
         return -1;
      }
      return inertia[0];
   }

   /**
    * Returns the number of zero pivots (pivots below Accelerate's zero
    * tolerance) in the most recent factorization, which, as with pivot
    * perturbation in other solvers, indicates a singular or nearly singular
    * matrix. Returns -1 if this is not available (as for unsymmetric
    * matrices).
    */
   public synchronized int getNumPerturbedPivots() {
      int[] inertia = new int[3];
      if (myHandle == 0 || doGetInertia (myHandle, inertia) != 0) {
         return -1;
      }
      return inertia[1];
   }

   /**
    * Sets the maximum number of iterative refinement steps performed after
    * each solve. The default is 1.
    *
    * @param nsteps maximum number of refinement steps
    */
   public synchronized void setMaxRefinementSteps (int nsteps) {
      if (myHandle != 0) {
         doSetMaxRefinementSteps (myHandle, nsteps);
      }
   }

   /**
    * {@inheritDoc}
    */
   public synchronized int getMaxRefinementSteps() {
      return (myHandle != 0 ? doGetMaxRefinementSteps (myHandle) : 0);
   }

   /**
    * Returns the number of iterative refinement steps performed in the most
    * recent solve.
    *
    * @return number of refinement steps in the last solve
    */
   public synchronized int getNumRefinementSteps() {
      return (myHandle != 0 ? doGetNumRefinementSteps (myHandle) : 0);
   }

   public boolean hasMultipleRhsSolves() {
      return true;
   }

   public boolean hasIterativeSolves() {
      return true;
   }

   /**
    * {@inheritDoc}
    *
    * <p>Accelerate does not provide iterative solves itself. Instead, the
    * iteration is performed in the native wrapper, using the most recent
    * factorization as a preconditioner and the supplied values for matrix
    * products.
    */
   public synchronized int iterativeSolve (
      double[] vals, double[] x, double[] b) {
      checkFactored();
      checkSolveArgs (x, b, 1);
      if (vals.length < myNumVals) {
         throw new IllegalArgumentException (
            "vals is too small: length="+vals.length+
            ", expected size is "+myNumVals);
      }
      int iters = doIterativeSolve (
         myHandle, vals, x, b, myIterativeTolerance,
         nativeMethodCode (myIterativeMethod), myIterativeMaxSolves,
         myGmresRestart, getUseCriticalArrayAccess());
      myLastIterativeSolves = doGetLastIterativeSolves (myHandle);
      myLastIterativeResidual = doGetLastIterativeResidual (myHandle);
      return iters;
   }

   /**
    * Returns a time breakdown of the most recent call to {@link
    * #iterativeSolve}, in msec: the total time spent in the native code, the
    * time spent in preconditioner solves, and the time spent in matrix-vector
    * products. The remainder of the total is vector operations and setup.
    * Intended for performance analysis.
    *
    * @return total, preconditioner solve and matrix product times, or
    * {@code null} if not available
    */
   public synchronized double[] getLastIterativeTimes() {
      if (myHandle == 0) {
         return null;
      }
      double[] times = new double[3];
      doGetLastIterativeTimes (myHandle, times);
      for (int i=0; i<times.length; i++) {
         times[i] *= 1000;
      }
      return times;
   }

   // ------------------------------------------------------------------
   // DirectSolverBase hooks
   // ------------------------------------------------------------------

   protected String getSolverName() {
      return "Accelerate";
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
      return doSetMatrix (
         myHandle, vals, rowOffs, colIdxs, size, numVals,
         (type & Matrix.SYMMETRIC) != 0);
   }

   protected int factorMatrixNative (double[] vals) {
      return doFactor (myHandle, vals);
   }

   protected int solveNative (double[] x, double[] b, int nrhs) {
      return doSolve (myHandle, x, b, nrhs);
   }

   protected String errorMessage (int rcode) {
      String detail = (myHandle != 0 ? doGetLastError (myHandle) : null);
      String msg;
      switch (rcode) {
         case -1: msg = "factorization failed (numerical issue)"; break;
         case -2: msg = "matrix is singular"; break;
         case -3: msg = "internal error"; break;
         case -4: msg = "parameter error"; break;
         case ERR_NOT_SYMMETRIC:
            msg = "only symmetric matrices are supported"; break;
         case ERR_NO_MATRIX: msg = "no matrix has been set"; break;
         case ERR_NO_FACTORIZATION: msg = "matrix has not been factored"; break;
         case ERR_OUT_OF_MEMORY: msg = "out of memory"; break;
         case ERR_LU_UNAVAILABLE:
            msg = "unsymmetric matrices are not supported"; break;
         default: msg = "error code " + rcode; break;
      }
      return (detail != null ? msg + ": " + detail : msg);
   }

   // Accelerate has no thread control; the thread count is only recorded

   protected int setNumThreadsNative (int num) {
      int prev = getNumThreadsNative();
      myNumThreads = num;
      return prev;
   }

   protected int getNumThreadsNative() {
      return myNumThreads > 0 ? myNumThreads : getPhysicalCoreCountNative();
   }

   protected int getPhysicalCoreCountNative() {
      return doGetPhysicalCoreCount();
   }
}
