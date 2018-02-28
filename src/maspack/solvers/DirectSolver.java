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
    * Factors a previously analyzed matrix.
    * 
    * @throws IllegalStateException
    * if no previous call to {@link #analyze analyze} has been made.
    * @throws NumericalException
    * if the factor failed for numeric reasons.
    */
   public void factor();

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
    * Factors a previously analyzed matrix M and then solves the system
    * 
    * <pre>
    * M x = b
    * </pre>
    * 
    * This is equivalent to
    * 
    * <pre>
    * factor()
    * solve (x, b) 
    * </pre>
    * 
    * but is included because it may be faster depending on the underlying
    * implementation.
    * 
    * If auto-iterative solving is available (as determined by {@link
    * #hasAutoIterativeSolving hasAutoIterativeSolving}), this method also
    * allows the solver to automatically employ iterative solving using a recent
    * direct factorization as a preconditioner. To enable auto-iterative
    * solving, the argument tolExp should be set to a positive value giving the
    * (negative) exponent of the desired relative residual.
    * 
    * @param x
    * vector in which result is returned
    * @param b
    * right hand vector of matrix equation
    * @param tolExp
    * if positive, enables auto-iterative solving with the specified value
    * giving the (negative) exponent of the desired relative residual.
    * @throws NumericalException
    * if the factoring or solving failed for numeric reasons
    */
   public void autoFactorAndSolve (VectorNd x, VectorNd b, int tolExp);

   /**
    * Returns true if this solver supports automatic iterative solving using a
    * recent directly-factored matrix as a preconditioner. If this feature is
    * available, it may be invoked using the
    * {@link #autoFactorAndSolve autoFactorAndSolve} method.
    * 
    * @return true if auto-iterative solving is available
    */
   public boolean hasAutoIterativeSolving();

   /**
    * Releases all internal resources allocated by this solver.
    */
   public void dispose();
}
