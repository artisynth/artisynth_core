/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.Matrix;
import maspack.matrix.Matrix.Partition;
import maspack.matrix.NumericalException;
import maspack.matrix.VectorNd;
import maspack.matrix.ImproperStateException;

/**
 * Base class for {@link DirectSolver} implementations which are implemented
 * natively and which accept their matrices in compressed row storage (CRS)
 * format with 1-based indices. It supplies the solver state machine, the
 * buffers used to extract CRS data from {@link Matrix} objects, and the
 * analyze, factor and solve methods which are common to such solvers,
 * leaving subclasses to supply the native calls themselves through a small
 * number of hook methods:
 *
 * <ul>
 * <li>{@link #createSolverHandle} and {@link #releaseSolverHandle}, which
 * create and destroy the native solver instance;
 * <li>{@link #setMatrixNative}, {@link #factorMatrixNative} and
 * {@link #solveNative}, which implement the three solution phases;
 * <li>{@link #errorMessage} and {@link #getSolverName}, which are used to
 * describe failures.
 * </ul>
 *
 * <p>Solver specific settings (reorder methods, pivot perturbation, etc.), and
 * the static methods which load the native libraries and determine solver
 * availability, are left entirely to the subclasses. Note in particular that
 * the fields which record whether a native library has been loaded must be
 * declared in the subclass, since static fields are not polymorphic and
 * sharing them here would cause one solver's availability to be reported for
 * another.
 */
public abstract class DirectSolverBase implements DirectSolver {

   /**
    * Handle to the native solver instance, or 0 if none has been created.
    */
   protected long myHandle;

   protected int myState = UNSET;
   protected String myErrMsg = null;

   /**
    * Size of the matrix most recently set for this solver.
    */
   protected int mySize;

   /**
    * Number of non-zero values in the matrix most recently set for this
    * solver.
    */
   protected int myNumVals;

   /**
    * Type of the matrix most recently set for this solver.
    */
   protected int myType;

   /**
    * Matrix most recently supplied to {@link #analyze(Matrix,int,int)}, or
    * <code>null</code> if the matrix was supplied in CRS format.
    */
   protected Matrix myMatrix;

   // buffers used to extract CRS data from a Matrix object
   protected double[] myVals = new double[0];
   protected int[] myColIdxs = new int[0];
   protected int[] myRowOffs = new int[0];

   // ------------------------------------------------------------------
   // hook methods to be supplied by subclasses
   // ------------------------------------------------------------------

   /**
    * Returns the name of this solver, for use in error messages.
    *
    * @return name of this solver
    */
   protected abstract String getSolverName();

   /**
    * Creates a native solver instance and returns a handle to it.
    *
    * @return handle to the native solver instance
    */
   protected abstract long createSolverHandle();

   /**
    * Releases the native solver instance associated with <code>handle</code>.
    *
    * @param handle handle to the native solver instance
    */
   protected abstract void releaseSolverHandle (long handle);

   /**
    * Passes a matrix, in CRS format with 1-based indices, to the native
    * solver, and performs the analyze phase.
    *
    * @param vals values of the non-zero matrix elements
    * @param rowOffs 1-based row start offsets
    * @param colIdxs 1-based column indices
    * @param size size of the matrix
    * @param numVals number of non-zero values
    * @param type or-ed flags giving the matrix type
    * @return native return code
    */
   protected abstract int setMatrixNative (
      double[] vals, int[] rowOffs, int[] colIdxs,
      int size, int numVals, int type);

   /**
    * Numerically factors the most recently set matrix, using the supplied
    * values.
    *
    * @param vals values of the non-zero matrix elements
    * @return native return code
    */
   protected abstract int factorMatrixNative (double[] vals);

   /**
    * Solves for one or more right hand sides using the most recent
    * factorization. Both <code>x</code> and <code>b</code> are stored in
    * column major order.
    *
    * @param x returns the solution(s)
    * @param b supplies the right hand side(s)
    * @param nrhs number of right hand sides
    * @return native return code
    */
   protected abstract int solveNative (double[] x, double[] b, int nrhs);

   /**
    * Sets the number of threads that the native solver should use, and
    * returns the previous value.
    *
    * @param num number of threads to use
    * @return previous number of threads
    */
   protected abstract int setNumThreadsNative (int num);

   /**
    * Returns the number of threads that the native solver is using.
    *
    * @return number of threads being used
    */
   protected abstract int getNumThreadsNative();

   /**
    * Returns a message describing the indicated native return code.
    *
    * @param rcode native return code
    * @return message describing the return code
    */
   protected abstract String errorMessage (int rcode);

   /**
    * Queries whether the "num perturbed pivots" message should be printed
    * after a factorization. Subclasses which support this should override
    * this method to return the value of their own enabling flag.
    *
    * @return {@code true} if the message should be printed
    */
   protected boolean showPerturbedPivots() {
      return false;
   }

   /**
    * Queries whether a native return code indicates an error. Codes
    * {@code < 0} always indicate errors; positive codes are used by some
    * solvers (such as MUMPS) to indicate warnings.
    *
    * @param rcode native return code
    * @return {@code true} if the code indicates an error
    */
   protected boolean isError (int rcode) {
      return rcode < 0;
   }

   // ------------------------------------------------------------------
   // state
   // ------------------------------------------------------------------

   protected void setState (int state) {
      myState = state;
   }

   /**
    * {@inheritDoc}
    */
   public int getState() {
      return myState;
   }

   /**
    * {@inheritDoc}
    */
   public String getErrorMessage() {
      return myErrMsg;
   }

   /**
    * Creates the native solver instance, if it does not already exist.
    */
   protected void initialize() {
      myHandle = createSolverHandle();
   }

   /**
    * Ensures that a native solver instance exists.
    */
   protected void ensureInitialized() {
      if (myHandle == 0) {
         initialize();
      }
   }

   // ------------------------------------------------------------------
   // thread control
   // ------------------------------------------------------------------

   /**
    * Default number of threads assigned to a solver when it is created.
    * Negative values mean that the number of threads is left at whatever
    * default the underlying thread pool uses.
    */
   protected static int myDefaultNumThreads = -1;

   /**
    * Sets the default number of threads which solvers are assigned when they
    * are created. The results are undefined if this number exceeds the
    * maximum number of threads available on the system. Setting
    * <code>num</code> to a value {@code <=} 0 will reset the number of threads
    * to the default used by OpenMP, which is typically the value stored in the
    * environment variable <code>OMP_NUM_THREADS</code>.
    *
    * <p><b>Note:</b> this setting is shared by all solvers derived from
    * <code>DirectSolverBase</code>, since the thread pools which they use
    * (OpenMP and the threaded BLAS) are properties of the process rather than
    * of an individual solver.
    *
    * @param num default number of threads to use
    * @see #getDefaultNumThreads
    */
   public static void setDefaultNumThreads (int num) {
      if (myDefaultNumThreads != num) {
         System.out.println ("Solvers: setting max threads to " + num);
         myDefaultNumThreads = num;
      }
   }

   /**
    * Returns the default number of threads which solvers are assigned when
    * they are created.
    *
    * @return default number of threads
    * @see #setDefaultNumThreads
    */
   public static int getDefaultNumThreads() {
      return myDefaultNumThreads;
   }

   /**
    * Sets the number of threads that this solver should use. The results are
    * undefined if this number exceeds the maximum number of threads available
    * on the system. Setting <code>num</code> to a value {@code <=} 0 will
    * reset the number of threads to the default used by OpenMP, which is
    * typically the value stored in the environment variable
    * <code>OMP_NUM_THREADS</code>.
    *
    * <p><b>Note:</b> the thread count is a property of the process rather than
    * of an individual solver, so changing it here affects all solvers running
    * in the same process. It should also not be changed in between the
    * analyze, factor and solve phases.
    *
    * @param num number of threads to use
    * @see #getNumThreads
    */
   public synchronized void setNumThreads (int num) {
      ensureInitialized();
      setNumThreadsNative (num);
   }

   /**
    * Returns the number of threads that this solver is using. By default,
    * this is the number used by OpenMP, which is typically the value stored
    * in the environment variable <code>OMP_NUM_THREADS</code>.
    *
    * @return number of threads being used
    * @see #setNumThreads
    */
   public synchronized int getNumThreads() {
      ensureInitialized();
      return getNumThreadsNative();
   }

   // ------------------------------------------------------------------
   // analyze
   // ------------------------------------------------------------------

   /**
    * Returns the matrix partition which should be used to extract CRS data
    * for a matrix of the indicated type.
    *
    * @param type or-ed flags giving the matrix type
    * @return partition to use for CRS extraction
    */
   protected Partition getPartition (int type) {
      if ((type & Matrix.SYMMETRIC) != 0) {
         return Partition.UpperTriangular;
      }
      else {
         return Partition.Full;
      }
   }

   protected void allocateBufferSpace (int size, int numVals) {
      if (myVals.length < numVals) {
         myVals = new double[numVals];
         myColIdxs = new int[numVals];
      }
      if (myRowOffs.length < size+1) {
         myRowOffs = new int[size+1];
      }
   }

   /**
    * Sets the matrix for this solver and performs the analyze phase, updating
    * the solver state accordingly. The matrix is supplied in CRS format with
    * 1-based indices.
    */
   protected void setMatrix (
      double[] vals, int[] rowOffs, int[] colIdxs,
      int size, int numVals, int type) {

      ensureInitialized();
      checkSetArgs (vals, rowOffs, colIdxs, size, numVals);
      int rcode = setMatrixNative (vals, rowOffs, colIdxs, size, numVals, type);
      if (!isError (rcode)) {
         setState (ANALYZED);
         mySize = size;
         myNumVals = numVals;
         myErrMsg = null;
      }
      else {
         setState (UNSET);
         myErrMsg = errorMessage (rcode);
      }
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void analyze (Matrix M, int size, int type) {
      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException ("Matrix is not square");
      }
      int origSize = M.rowSize();
      if (size < 0 || size > origSize) {
         throw new IllegalArgumentException (
            "Requested size " + size + " is out of bounds");
      }
      Partition part = getPartition (type);
      int numVals = M.numNonZeroVals (Partition.Full, size, size);
      if (part == Partition.UpperTriangular) {
         numVals -= (numVals - size) / 2;
      }
      allocateBufferSpace (size, numVals);
      M.getCRSIndices (myColIdxs, myRowOffs, part, size, size);
      M.getCRSValues (myVals, part, size, size);

      myType = type;
      myMatrix = M;
      setMatrix (myVals, myRowOffs, myColIdxs, size, numVals, type);
      if (myState == UNSET) {
         throw new NumericalException (
            getSolverName()+": unable to analyze matrix: "+myErrMsg);
      }
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void analyze (
      double[] vals, int[] colIdxs, int[] rowOffs, int size, int type) {

      ensureInitialized();
      int numVals = rowOffs[size]-1;
      checkSetArgs (vals, rowOffs, colIdxs, size, numVals);

      myType = type;
      myMatrix = null;
      setMatrix (vals, rowOffs, colIdxs, size, numVals, type);
      if (myState == UNSET) {
         throw new NumericalException (
            getSolverName()+": unable to analyze matrix: "+myErrMsg);
      }
   }

   // ------------------------------------------------------------------
   // factor
   // ------------------------------------------------------------------

   /**
    * {@inheritDoc}
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
    * {@inheritDoc}
    */
   public synchronized void factor (double[] vals) {
      if (myState == UNSET) {
         throw new IllegalStateException ("No matrix currently set");
      }
      else if (vals.length < myNumVals) {
         throw new IllegalArgumentException (
            "Not enough values: vals.length=" + vals.length +
            ", expected number is " + myNumVals);
      }
      int rcode = factorMatrixNative (vals);
      if (!isError (rcode)) {
         setState (FACTORED);
         myErrMsg = null;
      }
      else {
         myErrMsg = errorMessage (rcode);
         throw new NumericalException (
            getSolverName()+": unable to factor matrix: "+myErrMsg);
      }
      int nump = getNumPerturbedPivots();
      if (nump > 0 && showPerturbedPivots()) {
         System.out.println (
            getSolverName()+": num perturbed pivots=" + nump);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void analyzeAndFactor (Matrix M) {
      analyze (M, M.rowSize(), Matrix.INDEFINITE);
      factor();
   }

   // ------------------------------------------------------------------
   // solve
   // ------------------------------------------------------------------

   /**
    * {@inheritDoc}
    */
   public synchronized void solve (VectorNd x, VectorNd b) {
      checkFactored();
      checkSolveArgs (x, b, 1);
      checkSolveResult (solveNative (x.getBuffer(), b.getBuffer(), 1));
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void solve (double[] x, double[] b) {
      checkFactored();
      checkSolveArgs (x, b, 1);
      checkSolveResult (solveNative (x, b, 1));
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void solve (double[] X, double[] B, int nrhs) {
      if (!hasMultipleRhsSolves()) {
         throw new UnsupportedOperationException (
            getSolverName()+" does not support multiple right hand sides");
      }
      checkFactored();
      checkSolveArgs (X, B, nrhs);
      checkSolveResult (solveNative (X, B, nrhs));
   }

   /**
    * Computes the norm of the residual {@code M x - b}, where the values of
    * <code>M</code> are given in compressed row storage (CRS) format. This is
    * a convenience wrapper for {@link DirectSolver#residual}.
    *
    * @param rowOffs matrix row offsets (CRS format)
    * @param colIdxs non-zero element column indices (CRS format)
    * @param vals non-zero element values (CRS format)
    * @param size size of the matrix
    * @param x supplies the solution value
    * @param b supplies the right-hand side
    * @param symmetric if <code>true</code>, assumes that the arguments define
    * only the upper triangular portion of a symmetric matrix
    * @return norm of the residual
    */
   public double residual (
      int[] rowOffs, int[] colIdxs, double[] vals, int size,
      double[] x, double[] b, boolean symmetric) {

      return DirectSolver.residual (
         rowOffs, colIdxs, vals, size, x, b, symmetric);
   }

   // ------------------------------------------------------------------
   // argument and result checking
   // ------------------------------------------------------------------

   protected void checkSetArgs (
      double[] vals, int[] rowOffs, int[] colIdxs, int size, int numVals) {
      if (vals.length < numVals) {
         throw new IllegalArgumentException (
            "Not enough values: vals.length=" + vals.length +
            ", numVals=" + numVals);
      }
      if (colIdxs.length < numVals) {
         throw new IllegalArgumentException (
            "Not enough column indices: colIdxs.length=" + colIdxs.length +
            ", numVals=" + numVals);
      }
      if (rowOffs.length < size+1) {
         throw new IllegalArgumentException (
            "Not enough row start indices: rowOffs.length=" + rowOffs.length +
            ", size+1=" + (size+1));
      }
   }

   protected void checkFactored() {
      if (myState != FACTORED) {
         throw new IllegalStateException ("Matrix is not factored");
      }
   }

   protected void checkSolveArgs (double[] x, double[] b, int nrhs) {
      if (x.length < nrhs*mySize) {
         throw new IllegalArgumentException (
            "x is too small: length="+x.length+
            ", expected size is " + nrhs*mySize);
      }
      else if (b.length < nrhs*mySize) {
         throw new IllegalArgumentException (
            "b is too small: length="+b.length+
            ", expected size is " + nrhs*mySize);
      }
   }

   protected void checkSolveArgs (VectorNd x, VectorNd b, int nrhs) {
      if (x.size() < nrhs*mySize) {
         throw new IllegalArgumentException (
            "x is too small: size="+x.size()+
            ", expected size is " + nrhs*mySize);
      }
      else if (b.size() < nrhs*mySize) {
         throw new IllegalArgumentException (
            "b is too small: size="+b.size()+
            ", expected size is " + nrhs*mySize);
      }
   }

   /**
    * Checks the return code from a solve, and throws an exception if it
    * indicates an error.
    */
   protected void checkSolveResult (int rcode) {
      if (isError (rcode)) {
         myErrMsg = errorMessage (rcode);
         throw new NumericalException (
            getSolverName()+": unable to solve matrix: "+myErrMsg);
      }
      myErrMsg = null;
   }

   // ------------------------------------------------------------------
   // disposal
   // ------------------------------------------------------------------

   /**
    * {@inheritDoc}
    */
   public void dispose() {
      long handle = myHandle;
      if (handle != 0) {
         myHandle = 0;
         releaseSolverHandle (handle);
      }
      setState (UNSET);
      mySize = 0;
      myNumVals = 0;
      myMatrix = null;
   }

   public void finalize() {
      dispose();
   }
}
