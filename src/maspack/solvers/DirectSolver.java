/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.Matrix;
import maspack.matrix.VectorNd;
import maspack.matrix.NumericalException;

public interface DirectSolver {

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

   /**
    * Returns the current state of this solver, which is one of {@link #UNSET
    * UNSET}, {@link #ANALYZED ANALYZED}, or {@link #FACTORED FACTORED}.
    *
    * @return state of this solver
    */
   public int getState();

   /**
    * Returns a message describing the reason for the failure of the most
    * recent call to {@link #analyze analyze()} or {@link #factor factor()}, or
    * <code>null</code> if the method succeeded.
    *
    * @return most recent error message, if any
    */
   public String getErrorMessage();

   /**
    * If possible, returns the number of non-zero elements in the
    * factorization. If the factorization has not yet been performed, this may
    * be the number estimated by the most recent call to {@link #analyze
    * analyze()}. If the solver does not report this quantity, -1 is returned.
    *
    * @return number of non-zero elements in the factorization, or -1 if not
    * supported
    */
   public long getNumNonZerosInFactors();

   /**
    * Performs prefactor analysis on a specified matrix. The matrix reference is
    * stored and used by later calls to {@link #factor() factor()}. If
    * <code>size</code> is less than the actual matrix size, then the analysis
    * is done on the principal submatrix of M defined by the first
    * <code>size</code> rows and columns.
    * 
    * @param M
    * matrix to analyze
    * @param size
    * size of the matrix to factor.
    * @param type
    * or-ed flags giving information about the matrix type. Typical flags are
    * {@link maspack.matrix.Matrix#SYMMETRIC SYMMETRIC} or
    * {@link maspack.matrix.Matrix#SYMMETRIC POSITIVE_DEFINITE}
    * @throws IllegalArgumentException
    * if the matrix is not square, or the matrix type is not supported by the
    * solver.
    * @throws NumericalException
    * if the analysis failed for numeric reasons.
    */
   public void analyze (Matrix M, int size, int type);

   /**
    * Performs prefactor analysis on a matrix whose structure and values are
    * supplied in compressed row storage (CRS) format, using <b>1-based</b>
    * indices, as described in the documentation for {@link
    * maspack.matrix.Matrix#setCRSValues Matrix.setCRSValues}. The number of
    * non-zero values is given by {@code rowOffs[size]-1}. If the matrix type
    * is symmetric, only the upper triangular portion should be supplied.
    *
    * <p>Because no matrix object is supplied, {@link #factor() factor()}
    * cannot be used after calling this method; use {@link #factor(double[])
    * factor(vals)} instead. The values supplied here may be used to assist
    * the analysis but are not used for any numeric factorization.
    *
    * @param vals values of the non-zero matrix elements
    * @param colIdxs 1-based column indices of the non-zero matrix elements
    * @param rowOffs 1-based row start offsets into <code>vals</code> and
    * <code>colIdxs</code>
    * @param size size of the matrix to be analyzed
    * @param type or-ed flags giving information about the matrix type, as
    * described for {@link #analyze(Matrix,int,int) analyze(M,size,type)}
    * @throws IllegalArgumentException if the CRS data structures are
    * inconsistent, or the matrix type is not supported by the solver
    * @throws NumericalException if the analysis failed for numeric reasons
    */
   public void analyze (
      double[] vals, int[] colIdxs, int[] rowOffs, int size, int type);

   /**
    * Factors a previously analyzed matrix.
    * 
    * @throws IllegalStateException
    * if no previous call to {@link #analyze analyze} has been made.
    * @throws NumericalException
    * if the factor failed for numeric reasons.
    */
   public void factor();

   /**
    * Factors a previously analyzed matrix, using the numeric values supplied
    * by <code>vals</code>. These must be ordered in the same way as those
    * supplied to the most recent call to {@link #analyze analyze()}.
    * 
    * @param vals non-zero matrix element values
    * @throws IllegalStateException if no previous call to {@link #analyze
    * analyze} has been made
    * @throws IllegalArgumentException if the number of supplied values is
    * less than the number of non-zero elements in the analyzed matrix
    * @throws NumericalException if the factor failed for numeric reasons.
    */
   public void factor (double[] vals);

   /**
    * Factors a matrix. This is equivalent to the two calls
    * 
    * <pre>
    *   analyze (M, M.rowSize(), 0)
    *   factor()
    * </pre>
    * 
    * @param M
    * matrix to factor
    * @throws IllegalArgumentException
    * if the matrix is not square, or general matrices are not supported by the
    * solver.
    * @throws NumericalException
    * if the analysis or factoring failed for numeric reasons.
    */
   public void analyzeAndFactor (Matrix M);

   /**
    * Solves the system
    * 
    * <pre>
    *  M x = b
    * </pre>
    * 
    * where M was specified using previous calls to {@link #analyze analyze} or
    * {@link #analyzeAndFactor(Matrix) factor}.
    * 
    * @param x
    * vector in which result is returned
    * @param b
    * right hand vector of matrix equation
    * @throws NumericalException
    * if the solve failed for numeric reasons.
    * @throws IllegalStateException
    * if no previous call to {@link #analyze analyze} or
    * {@link #analyzeAndFactor(Matrix) factor} has been made.
    */
   public void solve (VectorNd x, VectorNd b);

   /**
    * Solves the system
    * 
    * <pre>
    *  M x = b
    * </pre>
    * 
    * where the solution and right hand side are supplied using
    * <code>double[]</code> objects.
    * 
    * @param x vector in which result is returned
    * @param b right hand vector of matrix equation
    * @throws NumericalException if the solve failed for numeric reasons.
    * @throws IllegalStateException if the matrix has not been factored
    */
   public void solve (double[] x, double[] b);

   /**
    * Returns true if this solver supports iterative solving, using a recent
    * directly-factored matrix as a preconditioner.
    * 
    * @return true if iterative solving is available
    */
   default public boolean hasIterativeSolves() {
      return false;
   }

   /**
    * Solves the system
    * 
    * <pre>
    *  M x = b
    * </pre>
    *
    * iteratively, using the most recent factorization as a preconditioner and
    * the matrix values supplied by <code>vals</code>. If the current values
    * are close to those associated with the factorization, this can be
    * considerably faster than an explicit {@link #factor(double[]) factor()}
    * and {@link #solve(double[],double[]) solve()}.
    *
    * <p>This method is only available if {@link #hasIterativeSolves} returns
    * <code>true</code>; otherwise, it returns 0 and the caller should fall
    * back on factoring and solving directly.
    *
    * @param vals current values of the non-zero matrix elements, in the same
    * ordering as that supplied to {@link
    * #analyze(double[],int[],int[],int,int) analyze()}
    * @param x vector in which the result is returned
    * @param b right hand vector of the matrix equation
    * @param tolExp (negative) exponent of the desired relative residual
    * @return number of iterations performed, or a value {@code <= 0} if the
    * iterative solve was not performed or did not succeed
    */
   default public int iterativeSolve (
      double[] vals, double[] x, double[] b, int tolExp) {
      return 0;
   }

   /**
    * Returns true if this solver can solve for multiple right hand sides,
    * using {@link #solve(double[],double[],int) solve(X,B,nrhs)}.
    *
    * @return true if multiple right hand sides are supported
    */
   default public boolean hasMultipleRhsSolves() {
      return false;
   }

   /**
    * Solves the system
    * 
    * <pre>
    *  M X = B
    * </pre>
    *
    * for a set of right hand sides B, where the number of right hand sides is
    * given by <code>nrhs</code>. Both <code>X</code> and <code>B</code> should
    * be stored in column major order. This method is only available if {@link
    * #hasMultipleRhsSolves} returns <code>true</code>.
    *
    * @param X returns the solutions, in column major order
    * @param B supplies the right hand sides, in column major order
    * @param nrhs number of right hand sides
    * @throws UnsupportedOperationException if multiple right hand sides are
    * not supported
    * @throws IllegalStateException if the matrix has not been factored
    * @throws NumericalException if the solve failed for numeric reasons
    */
   default public void solve (double[] X, double[] B, int nrhs) {
      throw new UnsupportedOperationException (
         "Solver does not support multiple right hand sides");
   }

   /**
    * If possible, returns the number of negative eigenvalues detected during
    * the most recent factorization of a symmetric indefinite matrix. For
    * matrices which are not symmetric indefinite, or for solvers which do not
    * compute the inertia, -1 is returned.
    *
    * @return number of negative eigenvalues, or -1 if not available
    */
   default public int getNumNegEigenvalues() {
      return -1;
   }

   /**
    * If possible, returns the number of positive eigenvalues detected during
    * the most recent factorization of a symmetric indefinite matrix. For
    * matrices which are not symmetric indefinite, or for solvers which do not
    * compute the inertia, -1 is returned.
    *
    * @return number of positive eigenvalues, or -1 if not available
    */
   default public int getNumPosEigenvalues() {
      return -1;
   }

   /**
    * If possible, returns the number of pivot perturbations that were required
    * during the most recent numeric factorization (i.e., during the last
    * {@link #factor() factor()} call). Pivot perturbation generally indicates
    * a singular, or very nearly singular, matrix. If the solver does not
    * report pivot perturbations, -1 is returned.
    *
    * @return number of perturbed pivots, or -1 if not supported
    */
   default public int getNumPerturbedPivots() {
      return -1;
   }

   /**
    * Returns the maximum number of iterative refinement steps that this solver
    * will perform after a solve, or 0 if iterative refinement is not
    * supported.
    *
    * @return maximum number of iterative refinement steps
    * @see #setMaxRefinementSteps
    */
   default public int getMaxRefinementSteps() {
      return 0;
   }

   /**
    * Sets the maximum number of iterative refinement steps that this solver
    * should perform after a solve. Setting this to 0 disables iterative
    * refinement. Solvers which do not support iterative refinement ignore
    * this setting.
    *
    * @param nsteps maximum number of iterative refinement steps
    * @see #getMaxRefinementSteps
    */
   default public void setMaxRefinementSteps (int nsteps) {
   }

   /**
    * Computes the norm of the residual
    * <pre>
    *   M x - b
    * </pre>
    * for given values of M, x, and b, where the values of <code>M</code> are
    * given in compressed row storage (CRS) format with 1-based indices.
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
    * @throws IllegalArgumentException if the dimensions of <code>x</code> or
    * <code>b</code> are incompatible with the matrix size.
    */
   public static double residual (
      int[] rowOffs, int[] colIdxs, double[] vals, int size,
      double[] x, double[] b, boolean symmetric) {

      if (x.length < size) {
         throw new IllegalArgumentException (
            "x is too small: x.length=" + x.length +
            ", expected size is " + size);
      }
      else if (b.length < size) {
         throw new IllegalArgumentException (
            "b is too small: b.length=" + b.length +
            ", expected size is " + size);
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
    * Releases all internal resources allocated by this solver.
    */
   public void dispose();
}
