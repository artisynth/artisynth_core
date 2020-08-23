/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Constructs the QR decomposition of a matrix. This takes the form
 * <pre>
 * M = Q R
 * </pre>
 * where M is the original matrix, Q is orthogonal, and R is
 * upper-triangular.  Nominally, if M has a size m X n, then if m {@code >=} n,
 * R is square with size n and Q is m X n. Otherwise, if m {@code <} n, Q is
 * square with size m and R has size m X n.
 *
 * <p>If the decomposition is performed with pivoting, using the method {@link
 * #factorWithPivoting}, then it takes the form
 * <pre>
 * M P = Q R
 * </pre>
 * where P is a permutation matrix.
 * 
 * <p> Note that if m {@code >} n, then R and M can actually have sizes p X n
 * and m X p, with n {@code <=} p {@code <=} m, with the additional rows of R
 * set to zero and the additional columns of Q formed by completing the
 * orthogonal basis.
 * 
 * <p>
 * Once constructed, a QR decomposition can be used to perform various
 * computations related to M, such as solving equations (in particular,
 * determining least-squares solutions), computing the determinant, or
 * estimating the condition number.
 * 
 * <p>
 * Providing a separate class for the QR decomposition allows an application to
 * perform such decompositions repeatedly without having to reallocate temporary
 * storage space.
 */
public class QRDecomposition {
   int nrows;
   int ncols;
   MatrixNd QR;
   private double[] vec = new double[0];
   private double[] wec = new double[0];
   private int[] piv = new int[0];
   private double[] beta = new double[0];
   private double[] colMagSqr = new double[0];
   private boolean factoredInPlaceP = true;

   private enum State {
      UNSET, SET, SET_WITH_PIVOTING
   };

   private State state = State.UNSET;

   /**
    * Creates a QRDecomposition for the Matrix specified by M.
    * 
    * @param M
    * matrix to perform the QR decomposition on
    */
   public QRDecomposition (Matrix M) {
      this();
      factor (M);
   }

   /**
    * Creates an uninitialized QRDecomposition.
    */
   public QRDecomposition() {
      nrows = 0;
      ncols = 0;
      QR = new MatrixNd (0, 0);
   }

   /**
    * Peforms a QR decomposition on the Matrix M.
    * 
    * @param M
    * matrix to perform the QR decomposition on
    */
   public void factor (Matrix M) {
      if (factoredInPlaceP) {
         QR = new MatrixNd (0, 0);
         factoredInPlaceP = false;
      }
      QR.set (M);
      nrows = M.rowSize();
      ncols = M.colSize();
      doFactor();
   }

   /**
    * Performs a QR decomposition on the Matrix M, placing the result in M and
    * using M as the storage for subsequent solve operations. Subsequent
    * modification of M will invalidate the decomposition.
    */
   public void factorInPlace (MatrixNd M) {
      QR = M;
      nrows = M.rowSize();
      ncols = M.colSize();
      doFactor();
      factoredInPlaceP = true;
   }

   /**
    * Overwrites v with its householder vector and returns beta
    */
   static double houseVector (double[] v, int i0, int iend) {
      double x0 = v[i0];
      double sigma = 0;
      for (int i=i0+1; i<iend; i++) {
         sigma += v[i]*v[i];
      }
      v[i0] = 1;
      if (sigma == 0) {
         //return x0 >= 0 ? 0 : -2;
         return 0;
      }
      else {
         double mu = Math.sqrt(x0*x0 + sigma);
         double v0;
         if (x0 <= 0) {
            v0 = x0-mu;
         }
         else {
            v0 = -sigma/(x0+mu);
         }
         double beta = 2*v0*v0/(sigma+v0*v0);
         for (int i=i0+1; i<iend; i++) {
            v[i] /= v0;
         }
         return beta;
      }
   }

   protected static double rowHouseReduce (
      MatrixNd M, int j, int k, double[] vec, double[] wec) {

      int m = M.nrows;
      int n = M.ncols;
      int w = M.width;

      for (int i=k; i<m; i++) {
         vec[i] = M.buf[i*w+j];
      }
      double beta = houseVector (vec, k, m);
      housePreMul (M.buf, w, m, n, j, k, beta, vec, wec);
      // store v in the zero-out section of M
      for (int i=k+1; i<m; i++) {
         M.buf[i*w+j] = vec[i];
      }
      return beta;
   }

   protected static double colHouseReduce (
      MatrixNd M, int i, int k, double[] vec, double[] wec) {

      int m = M.nrows;
      int n = M.ncols;
      int w = M.width;
      for (int j=k; j<n; j++) {
         vec[j] = M.buf[i*w+j];
      }
      double beta = houseVector (vec, k, n);
      housePostMul (M.buf, w, m, n, i, k, beta, vec, wec);
      for (int j=k+1; j<n; j++) {
         M.buf[i*w+j] = vec[j];
      }
      return beta;
   }
   
   private void doFactor() {
      int maxd = Math.max (nrows, ncols);
      if (vec.length < maxd) {
         vec = new double[maxd];
         wec = new double[maxd];
      }

      int columnLimit = (nrows > ncols ? ncols : nrows - 1);
      beta = new double[columnLimit];
      for (int j = 0; j < columnLimit; j++) {
         beta[j] = rowHouseReduce (QR, j, j, vec, wec);
      }
      state = State.SET;
   }

   private int getPivotCol (double[] colMagSqr, int col0, int ncols) {
      double maxMagSqr = -1;
      int pivotCol = ncols;
      for (int j = col0; j < ncols; j++) {
         if (colMagSqr[j] > maxMagSqr) {
            maxMagSqr = colMagSqr[j];
            pivotCol = j;
         }
      }
      return maxMagSqr <= 0 ? ncols : pivotCol;
   }

   /**
    * Peforms a QR decomposition, with pivoting, on the Matrix M. Adapted from
    * Algorithm 5.4.1, Golub and van Loan, second edition.
    * 
    * @param M
    * matrix to perform the QR decomposition on
    */
   public void factorWithPivoting (Matrix M) {
      QR.set (M);

      nrows = M.rowSize();
      ncols = M.colSize();
      int maxd = Math.max (nrows, ncols);
      if (vec.length < maxd) {
         vec = new double[maxd];
         wec = new double[maxd];
      }
      if (piv.length < ncols) {
         piv = new int[ncols];
      }
      if (colMagSqr.length < ncols) {
         colMagSqr = new double[ncols];
      }

      // compute magnitude squared for each column, and
      // find the maximum value and associated column
      //
      for (int j = 0; j < ncols; j++) {
         int Qbase = QR.base + j;
         double magSqr = 0;
         for (int i = 0; i < nrows; i++) {
            double elem = QR.buf[i * QR.width + Qbase];
            magSqr += elem * elem;
         }
         colMagSqr[j] = magSqr;
         piv[j] = j;
      }
      int pivotCol = getPivotCol (colMagSqr, 0, ncols);

      int columnLimit = (nrows > ncols ? ncols : nrows - 1);
      beta = new double[columnLimit];
      for (int j = 0; j < columnLimit; j++) {
         piv[j] = pivotCol;

         // exchange j and the pivotCol ...
         QR.exchangeColumns (j, pivotCol);
         double tmp = colMagSqr[j];
         colMagSqr[j] = colMagSqr[pivotCol];
         colMagSqr[pivotCol] = tmp;

         beta[j] = rowHouseReduce (QR, j, j, vec, wec);
         // update colMagSqr
         for (int k = j + 1; k < ncols; k++) {
            double QR_jk = QR.buf[j * QR.width + QR.base + k];
            colMagSqr[k] -= QR_jk * QR_jk;
         }
         pivotCol = getPivotCol (colMagSqr, j + 1, ncols);
         if (pivotCol == ncols) { // zero entries from j+1 on of QR, to remove
                                    // round off error
            for (int i = j + 1; i < nrows; i++) {
               for (int k = j + 1; k < ncols; k++) {
                  QR.buf[i * QR.width + QR.base + k] = 0;
               }
            }
            break;
         }
      }
      state = State.SET_WITH_PIVOTING;
   }

   /**
    * Returns the R matrix associated with this QR decomposition.
    *
    * @return R matrix
    * @throws ImproperStateException
    * if this QRDecomposition is uninitialized
    */
   public MatrixNd getR() {
      MatrixNd R = new MatrixNd();
      get (null, R, null);
      return R;
   }

   /**
    * Returns the Q matrix associated with this QR decomposition.
    *
    * @return Q matrix
    * @throws ImproperStateException
    * if this QRDecomposition is uninitialized
    */
   public MatrixNd getQ() {
      MatrixNd Q = new MatrixNd();
      get (Q, null, null);
      return Q;
   }

   /**
    * Gets the Q and R matrices associated with this QR decomposition. Each
    * argument is optional; values will be returned into them if they are
    * present. Details on the appropriate dimensions for Q and R are described
    * in the documentation for
    * {@link #get(MatrixNd,MatrixNd,int[]) get(Q,R,cperm)}.
    * 
    * @param Q
    * returns the orthogonal matrix
    * @param R
    * returns the upper triangular matrix.
    * @throws ImproperStateException
    * if this QRDecomposition is uninitialized
    * @throws ImproperSizeException
    * if Q or R are not of the proper dimension and cannot be resized.
    */
   public void get (MatrixNd Q, MatrixNd R)
      throws ImproperStateException, ImproperSizeException {
      get (Q, R, null);
   }

   /**
    * Gets the Q and R matrices, and the column permutation, associated with
    * this QR decomposition. The column permutation is the identity unless the
    * decomposition was performed with {@link #factorWithPivoting
    * factorWithPivoting}. Each argument is optional; values will be returned
    * into them if they are present. Q is an orthogonal matrix which can be m X
    * p, where min(n,m) {@code <=} p {@code <=} m (and so must be square if m
    * {@code <=} n). Extra columns of Q are formed by completing the original
    * basis. R is an upper triangular matrix which can be q X n, where min(n,m)
    * {@code <=} q {@code <=} m (and so must be m X n if m {@code <=} n). Extra
    * rows of R are set to zero.
    * 
    * @param Q
    * returns the orthogonal matrix
    * @param R
    * returns the upper triangular matrix
    * @param cperm
    * returns the indices of the column permutation matrix P, such that j-th
    * column of M P is given by column <code>perm[j]</code> of M.
    * @throws ImproperStateException
    * if this QRDecomposition is uninitialized
    * @throws ImproperSizeException
    * if Q or R are not of the proper dimension and cannot be resized.
    */
   public void get (MatrixNd Q, MatrixNd R, int[] cperm)
      throws ImproperStateException, ImproperSizeException {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      int mind = Math.min (nrows, ncols);

      if (Q != null) {
         if (Q.nrows != nrows || Q.ncols < mind || Q.ncols > nrows) {
            if (Q.isFixedSize()) {
               throw new ImproperSizeException ("Incompatible dimensions");
            }
            else if (Q.ncols < mind) {
               Q.resetSize (nrows, mind);
            }
            else {
               Q.resetSize (nrows, nrows);
            }
         }
         Q.setIdentity();
         preMulQ (Q, Q);
      }
      if (R != null) {
         if (R.ncols != ncols || R.nrows < mind || R.nrows > nrows) {
            if (R.isFixedSize()) {
               throw new ImproperSizeException ("Incompatible dimensions");
            }
            else if (R.nrows < mind) {
               R.resetSize (mind, ncols);
            }
            else {
               R.resetSize (nrows, ncols);
            }
         }
         for (int i = 0; i < R.nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               int Rbase = i * R.width + R.base;
               int Qbase = i * QR.width + QR.base;
               if (j >= i) {
                  R.buf[Rbase + j] = QR.buf[Qbase + j];
               }
               else {
                  R.buf[Rbase + j] = 0;
               }
            }
         }
      }
      if (cperm != null) {
         for (int j = 0; j < ncols; j++) {
            cperm[j] = j;
         }
         if (state == State.SET_WITH_PIVOTING) {
            for (int j = 0; j < ncols; j++) {
               int k = piv[j];
               if (k != j) {
                  int tmp = cperm[k];
                  cperm[k] = cperm[j];
                  cperm[j] = tmp;
               }
            }
         }
      }
   }

   /**
    * Given a subvector x defined by u[base:base+m-1] and an m-element vector v,
    * this routine overwrites x with P x, where
    * 
    * P = I - 2 v v^T / (v^T v)
    */
   private void rowHouseMulVec (
      double[] u, int base, double[] v, int m, double beta) {
      // double sum = 0;
      // for (int k = 0; k < m; k++) {
      //    sum += v[k] * v[k];
      // }
      // double beta = -2 / sum;

      double sum = 0;
      for (int k = 0; k < m; k++) {
         sum += u[base + k] * v[k];
      }
      double w = beta * sum;

      for (int k = 0; k < m; k++) {
         u[base + k] -= w * v[k];
      }
   }

   private boolean doSolve (double[] sol) {
      // compute sol' = Q^T sol
      int lastCol = (nrows > ncols ? ncols - 1 : Math.min(ncols,nrows) - 2);
      for (int j = 0; j <= lastCol; j++) {
         vec[0] = 1;
         int Qbase = j * QR.width + j + QR.base;
         for (int k = 1; k < nrows - j; k++) {
            vec[k] = QR.buf[k * QR.width + Qbase];
         }
         rowHouseMulVec (sol, j, vec, nrows - j, beta[j]);
      }
      return doSolveR (sol);
   }

   private boolean doLeftSolve (double[] sol) {
      boolean nonSingular = doLeftSolveR (sol);

      // compute sol' = Q sol

      if (nrows > ncols) {
         // clear trailing part of sol, since sol' is larger than sol and
         // otherwise trailing trash will get folded into the solution
         for (int i=ncols; i<nrows; i++) {
            sol[i] = 0;
         }
      }
      int lastCol = (nrows > ncols ? ncols - 1 : Math.min(ncols,nrows) - 2);
      for (int j = lastCol; j >= 0; j--) {
         vec[0] = 1;
         int Qbase = j * QR.width + j + QR.base;
         for (int k = 1; k < nrows - j; k++) {
            vec[k] = QR.buf[k * QR.width + Qbase];
         }
         rowHouseMulVec (sol, j, vec, nrows - j, beta[j]);
      }
      return nonSingular;
   }

   /**
    * Solve R x = sol by back substitution
    */
   private boolean doSolveR (double[] sol) {
      boolean nonSingular = true;

      int nr = Math.min(ncols,nrows);
      for (int i = nr - 1; i >= 0; i--) {
         int Qbase = i * QR.width + QR.base;
         double sum = sol[i];
         for (int j = i + 1; j < nr; j++) {
            sum -= sol[j] * QR.buf[Qbase + j];
         }
         double d = QR.buf[Qbase + i];
         if (d == 0) {
            nonSingular = false;
         }
         sol[i] = sum / d;
      }
      // if nrows < ncols, pad remainder of sol with 0
      for (int i = nr; i < ncols; i++) {
         sol[i] = 0;
      }

      // if the decomposition contains a permutation, apply
      // that in reverse order
      if (state == State.SET_WITH_PIVOTING) {
         for (int i = ncols - 1; i >= 0; i--) {
            if (piv[i] != i) {
               double tmp = sol[piv[i]];
               sol[piv[i]] = sol[i];
               sol[i] = tmp;
            }
         }
      }
      return nonSingular;
   }

   /**
    * Solve x R = sol by back substitution
    */
   private boolean doLeftSolveR (double[] sol) {
      boolean nonSingular = true;

      // if the decomposition contains a permutation, apply
      // that first to sol
      if (state == State.SET_WITH_PIVOTING) {
         for (int i = 0; i < ncols; i++) {
            if (piv[i] != i) {
               double tmp = sol[piv[i]];
               sol[piv[i]] = sol[i];
               sol[i] = tmp;
            }
         }
      }
      for (int i = 0; i < ncols; i++) {
         int Qbase = QR.base + i;
         double sum = sol[i];
         for (int j = 0; j < i; j++) {
            sum -= sol[j] * QR.buf[Qbase];
            Qbase += QR.width;
         }
         double d = QR.buf[Qbase];
         if (d == 0) {
            nonSingular = false;
         }
         sol[i] = sum / d;
      }
      return nonSingular;
   }

   /**
    * Computes a solution to the linear equation <br> M x = b<br> where M is
    * the original matrix associated with this decomposition, and x and b are
    * vectors. If M has size {@code m X n} with {@code m > n}, then the system
    * is overdetermined and solution with the minimum least square error is
    * computed. Alternatively, if {@code m < n}, then the system is
    * underdetermined and the a solution is found by using only the left-most
    * {@code m X m} block of R (which is the same method used by the MATLAB \
    * operator).
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M does not have full rank (within working
    * precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if b does not have a size compatible
    * with M, or if x does not have a size compatible with M and cannot be
    * resized.
    */
   public boolean solve (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (b.size() != nrows) {
         throw new ImproperSizeException ("improper size for b");
      }
      if (x.size() != ncols) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException ("improper size for x");
         }
         else {
            x.setSize (ncols);
         }
      }
      b.get (wec);
      nonSingular = doSolve (wec);
      x.set (wec, 0);
      return nonSingular;
   }

   /**
    * Computes a least-squares solution to the linear equation <br>
    * M X = B <br>
    * where M is the original matrix associated with this decomposition, and X
    * and B are matrices. If M has size {@code m X n} with {@code m > n}, then
    * the system is overdetermined and solution with the minimum least square
    * error is computed. Alternatively, if {@code m < n}, then the system is
    * underdetermined and the a solution is found by using only the left-most
    * {@code m X m} block of R (which is the same method used by the MATLAB \
    * operator).
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if M does not have full column rank (within working
    * precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * If B has a different number of rows than
    * M, or if X has a different number of rows than M or a different number of
    * columns than B and cannot be resized.
    */
   public boolean solve (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (B.rowSize() != nrows) {
         throw new ImproperSizeException ("improper size for B");
      }
      if (X.colSize() != B.colSize() || X.rowSize() != ncols) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("improper size for X");
         }
         else {
            X.setSize (ncols, B.colSize());
         }
      }
      for (int k = 0; k < B.colSize(); k++) {
         B.getColumn (k, wec);
         if (!doSolve (wec)) {
            nonSingular = false;
         }
         X.setColumn (k, wec);
      }
      return nonSingular;
   }

   /**
    * Computes a left solution to the linear equation <br>x M = b<br>
    * where M is the original matrix associated with this decomposition, and x
    * and b are row vectors.
    *
    * <p>The number of rows {@code m} in M must equal or exceed the number of
    * columns {@code n} (which implies that R is a square matrix with a size
    * equal to {@code n}).
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M does not have full rank (within working
    * precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized or if {@code m < n} for
    * the original matrix
    * @throws ImproperSizeException
    * if b does not have a size compatible
    * with M, or if x does not have a size compatible with M and cannot be
    * resized.
    */
   public boolean leftSolve (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;
            
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
      }
      if (b.size() != ncols) {
         throw new ImproperSizeException ("improper size for b");
      }
      if (x.size() != nrows) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException ("improper size for x");
         }
         else {
            x.setSize (nrows);
         }
      }
      b.get (wec);
      nonSingular = doLeftSolve (wec);
      x.set (wec, 0);
      return nonSingular;
   }

   /**
    * Computes a left solution to the linear equation <br>x M = b<br>
    * where M is the original matrix associated with this decomposition, and X
    * and B are matrices.
    *
    * <p>The number of rows {@code m} in M must equal or exceed the number of
    * columns {@code n} (which implies that R is a square matrix with a size
    * equal to {@code n}).
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if M does not have full rank (within working
    * precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized or if {@code m < n} for
    * the original matrix
    * @throws ImproperSizeException
    * if b does not have a size compatible
    * with M, or if x does not have a size compatible with M and cannot be
    * resized.
    */
   public boolean leftSolve (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;
            
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
      }
      if (B.colSize() != ncols) {
         throw new ImproperSizeException ("improper size for B");
      }
      if (X.rowSize() != B.rowSize() || X.colSize() != nrows) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("improper size for X");
         }
         else {
            X.setSize (B.rowSize(), nrows);
         }
      }
      for (int k = 0; k < B.rowSize(); k++) {
         B.getRow (k, wec);
         if (!doLeftSolve (wec)) {
            nonSingular = false;
         }
         X.setRow (k, wec);
      }
      return nonSingular;
   }

   /**
    * Computes a solution to the linear equation
    * <pre>
    * R P^T x = b
    * </pre>
    * where R is the upper triangular matrix associated with this decomposition,
    * P is the permutation matrix (which is the identity unless the
    * decomposition was formed with {@link #factorWithPivoting}), and
    * x and b are vectors. 
    *
    *<p> 
    * If the original matrix M has size {@code m X n}, and {@code m >= n}, then
    * R is a square matrix with a size equal to {@code n}, and {@code x} and
    * {@code b} should each have size {@code n}.  Otherise, if {@code m < n},
    * then R is {@code m X n}, {@code b} should have size {@code m}, and {@code
    * x} will have size {@code n} and will be solved using only the left-most
    * {@code m X m} block of R with trailing elements set to zero.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if R is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if b does not have a size compatible with R, or if x does not have a size
    * compatible with R and cannot be resized.
    */
   public boolean solveR (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (b.size() != Math.min(nrows,ncols)) {
         throw new ImproperSizeException ("improper size for b");
      }
      if (x.size() != ncols) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException ("improper size for x");
         }
         else {
            x.setSize (ncols);
         }
      }
      b.get (wec);
      nonSingular = doSolveR (wec);
      x.set (wec, 0);
      return nonSingular;
   }

   /**
    * Computes a solution to the linear equation
    * <pre>
    * R P^T X = B
    * </pre>
    * where R is the upper triangular matrix associated with this decomposition,
    * P is the permutation matrix (which is the identity unless the
    * decomposition was formed with {@link #factorWithPivoting}), and
    * X and B are matrices. 
    *
    * <p> 
    * If the original matrix M has size {@code m X n}, and {@code m >= n}, then
    * R is a square matrix with a size equal to {@code n}, and {@code X} and
    * {@code B} should each have {@code n} rows.  Otherise, if {@code m < n},
    * then R is {@code m X n}, {@code B} should have {@code m} rows, and {@code
    * X} will have {@code n} rows and will be solved using only the left-most
    * {@code m X m} block of R with trailing elements set to zero.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if R is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if the size of B is incompatible with R, or if the size of X is
    * incompatible with R or B and X cannot be resized.
    */
   public boolean solveR (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (B.rowSize() != Math.min(nrows,ncols)) {
         throw new ImproperSizeException ("improper size for B");
      }
      if (X.colSize() != B.colSize() || X.rowSize() != ncols) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("improper size for X");
         }
         else {
            X.setSize (ncols, B.colSize());
         }
      }
      for (int k = 0; k < B.colSize(); k++) {
         B.getColumn (k, wec);
         if (!doSolveR (wec)) {
            nonSingular = false;
         }
         X.setColumn (k, wec);
      }
      return nonSingular;
   }

   /**
    * Computes a left solution to the linear equation
    * <pre>
    * x R P^T = b
    * </pre>
    * where R is the upper triangular matrix associated with this decomposition,
    * P is the permutation matrix (which is the identity unless the
    * decomposition was formed with {@link #factorWithPivoting}), and
    * x and b are matrices. 
    *
    * <p> The number of rows {@code m} in the original matrix M must equal or
    * exceed the number of columns {@code n} (which implies that R is a square
    * matrix with a size equal to {@code n}).  {@code x} and {@code b} should
    * each have size {@code n}.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if R is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized or if {@code m < n} for
    * the original matrix
    * @throws ImproperSizeException
    * if b does not have a size compatible with R, or if x does not have a size
    * compatible with R and cannot be resized.
    */
   public boolean leftSolveR (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
      }
      if (b.size() != ncols) {
         throw new ImproperSizeException ("improper size for b");
      }
      if (x.size() != ncols) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException ("improper size for x");
         }
         else {
            x.setSize (ncols);
         }
      }
      b.get (wec);
      nonSingular = doLeftSolveR (wec);
      x.set (wec, 0);
      return nonSingular;
   }

   /**
    * Computes a left solution to the linear equation
    * <pre>
    * X R P^T = B
    * </pre>
    * where R is the upper triangular matrix associated with this decomposition,
    * P is the permutation matrix (which is the identity unless the
    * decomposition was formed with {@link #factorWithPivoting}), and
    * X and B are matrices. 
    *
    * <p> The number of rows {@code m} in the original matrix M must equal or
    * exceed the number of columns {@code n} (which implies that R is a square
    * matrix with a size equal to {@code n}).  {@code X} and {@code B} should
    * each have {@code n} rows.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if R is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized or if {@code m < n} for
    * the original matrix
    * @throws ImproperSizeException
    * if the size of B is incompatible with R, or if the size of X is
    * incompatible with R or B and X cannot be resized.
    */
   public boolean leftSolveR (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {

      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
      }
      if (B.colSize() != ncols) {
         throw new ImproperSizeException ("improper size for B");
      }
      if (X.rowSize() != B.rowSize() || X.colSize() != ncols) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("improper size for X");
         }
         else {
            X.setSize (B.rowSize(), ncols);
         }
      }
      for (int i = 0; i < B.rowSize(); i++) {
         B.getRow (i, wec);
         if (!doLeftSolveR (wec)) {
            nonSingular = false;
         }
         X.setRow (i, wec);
      }
      return nonSingular;
   }

   /**
    * Estimates the condition number of the triangular matrix R associated with
    * this decomposition. If the number of rows in the original matrix M is
    * less than the number of columns, the condition number is estimated for
    * the left-most {@code m X m} block of R. The algorithm for estimating the
    * condition number is given in Section 3.5.4 of Golub and Van Loan, Matrix
    * Computations (Second Edition).
    * 
    * @return condition number estimate
    * @throws ImproperStateException
    * if this QRDecomposition is uninitialized
    */
   public double conditionEstimate() throws ImproperStateException {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      int nr = Math.min(nrows,ncols); // number of rows in R

      int i, j;

      double[] pvec = new double[nr];
      double[] ppos = new double[nr];
      double[] pneg = new double[nr];
      double[] yvec = new double[nr];

      for (i = 0; i < nr; i++) {
         pvec[i] = 0;
      }
      for (j = nr - 1; j >= 0; j--) {
         double pposNorm1 = 0;
         double pnegNorm1 = 0;
         double R_jj = QR.buf[j * QR.width + j + QR.base];
         double ypos = (1 - pvec[j]) / R_jj;
         double yneg = (-1 - pvec[j]) / R_jj;
         for (i = 0; i < j; i++) {
            double R_ij = QR.buf[i * QR.width + j + QR.base];
            ppos[i] = pvec[i] + ypos * R_ij;
            pneg[i] = pvec[i] + yneg * R_ij;
            pposNorm1 += Math.abs (ppos[i]);
            pnegNorm1 += Math.abs (pneg[i]);
         }
         if (Math.abs (ypos) + pposNorm1 >= Math.abs (yneg) + pnegNorm1) {
            yvec[j] = ypos;
            for (i = 0; i < j; i++) {
               pvec[i] = ppos[i];
            }
         }
         else {
            yvec[j] = yneg;
            for (i = 0; i < j; i++) {
               pvec[i] = pneg[i];
            }
         }
      }

      // Compute the infinity norm of y
      double yNormInf = 0;
      for (i = 0; i < nr; i++) {
         double abs = Math.abs (yvec[i]);
         if (abs > yNormInf) {
            yNormInf = abs;
         }
      }

      // Compute the infinity norm of R
      double RNormInf = 0;
      for (i = 0; i < nr; i++) {
         int Qbase = QR.width * i + QR.base;
         double sum = 0;
         for (j = i; j < nr; j++) {
            sum += Math.abs (QR.buf[Qbase + j]);
         }
         if (sum > RNormInf) {
            RNormInf = sum;
         }
      }

      if (RNormInf == 0) {
         return Double.POSITIVE_INFINITY;
      }
      else {
         return (RNormInf * yNormInf);
      }
   }

   /**
    * Returns the determinant of the (square) upper partition of R, times the
    * determinant of Q. This equals the determinant of the original matrix M in
    * the case where M is square.
    * 
    * @return determinant (or product of R diagonals)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public double determinant() throws ImproperStateException {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      double detR = 1.0;
      for (int i = 0; i < Math.min (ncols, nrows); i++) {
         detR *= QR.buf[i * QR.width + i + QR.base];
      }
      // determinant of Q is -1^p, where p is the number of non-zero HouseHolder
      // transformations
      int nhouse = (nrows > ncols ? ncols : nrows - 1);
      int p = nhouse;
      for (int j=0; j<nhouse; j++) {
         if (beta[j] == 0) {
            p--;
         }
      }
      int detQ = ((p % 2) == 0 ? 1 : -1);

      if (state == State.SET_WITH_PIVOTING) { // apply the sign of the
                                                // permutation
         int permSign = 1;
         for (int i = 0; i < ncols; i++) {
            if (piv[i] != i) {
               permSign = -permSign;
            }
         }
         return permSign * detQ * detR;
      }
      else {
         return detQ * detR;
      }
   }

   /**
    * Computes the inverse of the original matrix M associated with this
    * decomposition, and places the result in X. M must be square.
    * 
    * @param X
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if M is not square, or if X does not have the same size as M and cannot be
    * resized.
    */
   public boolean inverse (DenseMatrix X) throws ImproperStateException {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows != ncols) {
         throw new ImproperSizeException ("Original matrix not square");
      }
      if (X.rowSize() != ncols || X.colSize() != ncols) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            X.setSize (ncols, ncols);
         }
      }
      boolean nonSingular = true;
      for (int j = 0; j < ncols; j++) {
         for (int i = 0; i < ncols; i++) {
            wec[i] = (i == j ? 1 : 0);
         }
         if (!doSolve (wec)) {
            nonSingular = false;
         }
         X.setColumn (j, wec);
      }
      return nonSingular;
   }

   public int rank (double tol) {
      if (state != State.SET_WITH_PIVOTING) {
         throw new ImproperStateException (
            "Decomposition must be initialized with pivoting");
      }
      int rank = 0;
      double R00 = Math.abs (QR.buf[QR.base]);
      for (int i = 0; i < Math.min (ncols, nrows); i++) {
         if (Math.abs (QR.buf[i * QR.width + i + QR.base]) <= R00 * tol) {
            break;
         }
         rank++;
      }
      return rank;
   }

   /**
    * Load HouseHolder vector k from its location in QR and store
    * it in v(k:m-1) of m.
    */
   protected void loadHouseVector (double[] v, int m, int k) {
      v[k] = 1;
      for (int i=k+1; i<m; i++) {
         v[i] = QR.buf [i*QR.width+k];
      }      
   }

   /**
    * Computes
    * <pre>
    * A(:,j0:n-1) = P_k A(:,j0:n-1)
    * </pre>
    * where A is an m X n matrix and P_k is a k-th Householder reflection
    * determined from
    * <pre>
    * P_k = I - beta v v^T
    * </pre>
    * Here, "k-th Householder" implies that only elements (k,m-1) of v are
    * non-zero and hence only rows (k:m-1) of A are affected.
    *
    * @param Abuf double buffer holding the values of A
    * @param aw width of A such that A(i,j) = Abuf[i*aw+j]
    * @param m number of rows in A
    * @param n number of columnes in A
    * @param j0 index of first column in A to which reflection should be applied
    * @param k index of first non-zero entry in the Householder vector
    * @param beta beta value associated with the reflection
    * @param v contains Householder vector (with length at least m)
    * @param w work vector (with length at least n)
    */
   protected static void housePreMul (
      double[] Abuf, int aw, int m, int n, int j0,
      int k, double beta, double[] v, double[] w) {

      for (int j=j0; j<n; j++) {
         double sum = 0;
         for (int i=k; i<m; i++) {
            sum += Abuf[i*aw+j]*v[i];
         }
         w[j] = beta*sum;
      }
      for (int j=j0; j<n; j++) {
         for (int i=k; i<m; i++) {
            Abuf[i*aw+j] -= v[i]*w[j];
         }
      }
   }

   /**
    * Computes
    * <pre>
    * A = P_k A
    * </pre>
    * where A is an m X n matrix and P_k is a k-th Householder reflection
    * determined from
    * <pre>
    * P_k = I - beta v v^T
    * </pre>
    * Here, "k-th Householder" implies that only elements (k,m-1) of v are
    * non-zero and hence only rows (k:m-1) of A are affected.
    *
    * @param A matrix to which reflection is applied
    * @param k index of first non-zero entry in the Householder vector
    * @param beta beta value associated with the reflection
    * @param v contains Householder vector (with length at least m)
    * @param w work vector (with length at least n)
    */
   protected static void housePreMul (
      MatrixNd A, int k, double beta, double[] v, double[] w) {

      housePreMul (A.buf, A.width, A.nrows, A.ncols, 0, k, beta, v, w);
   }

   /**
    * Computes
    * <pre>
    * A(i0:m-1,:) = A(i0:m-1,:) P_k
    * </pre>
    * where A is an m X n matrix and P_k is a k-th Householder reflection
    * determined from
    * <pre>
    * P_k = I - beta v v^T
    * </pre>
    * Here, "k-th Householder" implies that only elements (k,m-1) of v are
    * non-zero and hence only columns (k:n-1) of A are affected.
    *
    * @param Abuf double buffer holding the values of A
    * @param aw width of A such that A(i,j) = Abuf[i*aw+j]
    * @param m number of rows in A
    * @param n number of columnes in A
    * @param i0 index of first row in A to which reflection should be applied
    * @param k index of first non-zero entry in the Householder vector
    * @param beta beta value associated with the reflection
    * @param v contains Householder vector (with length at least n)
    * @param w work vector (with length at least m)
    */
   protected static void housePostMul (
      double[] Abuf, int aw, int m, int n, int i0,
      int k, double beta, double[] v, double[] w) {

      for (int i=i0; i<m; i++) {
         double sum = 0;
         for (int j=k; j<n; j++) {
            sum += Abuf[i*aw+j]*v[j];
         }
         w[i] = beta*sum;
      }
      for (int i=i0; i<m; i++) {
         for (int j=k; j<n; j++) {
            Abuf[i*aw+j] -= v[j]*w[i];
         }
      }
   }

   /**
    * Computes
    * <pre>
    * A = A P_k
    * </pre>
    * where A is an m X n matrix and P_k is a k-th Householder reflection
    * determined from
    * <pre>
    * P_k = I - beta v v^T
    * </pre>
    * Here, "k-th Householder" implies that only elements (k,m-1) of v are
    * non-zero and hence only columns (k:n-1) of A are affected.
    *
    * @param A matrix to which reflection is applied
    * @param k index of first non-zero entry in the Householder vector
    * @param beta beta value associated with the reflection
    * @param v contains Householder vector (with length at least n)
    * @param w work vector (with length at least m)
    */
   protected static void housePostMul (
      MatrixNd A, int k, double beta, double[] v, double[] w) {

      housePostMul (A.buf, A.width, A.nrows, A.ncols, 0, k, beta, v, w);
   }

   /**
    * Computes
    * <pre>
    *   MR = Q M1
    * </pre>
    * where Q is the full m X m orthogonal matrix associated with the
    * decomposition.  Before calling this method, the decomposition must be
    * initialized with a factorization. In order to conform with Q, M1 must
    * have m rows.
    *
    * @param MR result matrix
    * @param M1 matrix to pre-multiply by Q
    */
   public void preMulQ (MatrixNd MR, MatrixNd M1) {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (M1.rowSize() != nrows) {
         throw new IllegalArgumentException (
            "M1 has "+M1.rowSize()+" rows; should have "+nrows);
      }
      if (MR != M1) {
         MR.set (M1);
      }
      double[] w = wec;
      if (M1.colSize() > wec.length) {
         w = new double[M1.colSize()];
      }
      for (int k=beta.length-1; k>=0; k--) {
         loadHouseVector (vec, nrows, k);
         housePreMul (MR, k, beta[k], vec, w);
      }
   }

   /**
    * Computes
    * <pre>
    *   MR = Q^T M1
    * </pre>
    * where Q is the full m X m orthogonal matrix associated with the
    * decomposition.  Before calling this method, the decomposition must be
    * initialized with a factorization. In order to conform with Q, M1 must
    * have m rows.
    *
    * @param MR result matrix
    * @param M1 matrix to pre-multiply by Q transpose
    */
   public void preMulQTranspose (MatrixNd MR, MatrixNd M1) {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (M1.rowSize() != nrows) {
         throw new IllegalArgumentException (
            "M1 has "+M1.rowSize()+" rows; should have "+nrows);
      }
      if (MR != M1) {
         MR.set (M1);
      }
      double[] w = wec;
      if (M1.colSize() > wec.length) {
         w = new double[M1.colSize()];
      }
      for (int k=0; k<beta.length; k++) {
         loadHouseVector (vec, nrows, k);
         housePreMul (MR, k, beta[k], vec, w);
      }
   }

   /**
    * Computes
    * <pre>
    *   MR = M1 Q
    * </pre>
    * where Q is the full m X m orthogonal matrix associated with the
    * decomposition.  Before calling this method, the decomposition must be
    * initialized with a factorization. In order to conform with Q, M1 must
    * have m columns.
    *
    * @param MR result matrix
    * @param M1 matrix to post-multiply by Q
    */
   public void postMulQ (MatrixNd MR, MatrixNd M1) {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (M1.colSize() != nrows) {
         throw new IllegalArgumentException (
            "M1 has "+M1.colSize()+" cols; should have "+nrows);
      }
      if (MR != M1) {
         MR.set (M1);
      }
      double[] w = wec;
      if (M1.rowSize() > wec.length) {
         w = new double[M1.rowSize()];
      }
      for (int k=0; k<beta.length; k++) {
         loadHouseVector (vec, nrows, k);
         housePostMul (MR, k, beta[k], vec, w);
      }
   }

   /**
    * Computes
    * <pre>
    *   MR = M1 Q^T
    * </pre>
    * where Q is the full m X m orthogonal matrix associated with the
    * decomposition.  Before calling this method, the decomposition must be
    * initialized with a factorization. In order to conform with Q, M1 must
    * have m columns.
    *
    * @param MR result matrix
    * @param M1 matrix to post-multiply by Q transpose
    */
   public void postMulQTranspose (MatrixNd MR, MatrixNd M1) {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (M1.colSize() != nrows) {
         throw new IllegalArgumentException (
            "M1 has "+M1.colSize()+" cols; should have "+nrows);
      }
      if (MR != M1) {
         MR.set (M1);
      }
      double[] w = wec;
      if (M1.rowSize() > wec.length) {
         w = new double[M1.rowSize()];
      }
      for (int k=beta.length-1; k>=0; k--) {
         loadHouseVector (vec, nrows, k);
         housePostMul (MR, k, beta[k], vec, w);
      }
   }

   public static void main (String[] args) {
      MatrixNd M = new MatrixNd (6, 6);
      MatrixNd Mcheck = new MatrixNd (6, 6);
      MatrixNd R = new MatrixNd (6, 6);
      MatrixNd Q = new MatrixNd (6, 6);

      M.setRandom();

      QRDecomposition qrd = new QRDecomposition();

      qrd.factor (M);
      qrd.get (Q, R);
      System.out.println ("M=\n" + M.toString ("%8.4f"));
      System.out.println ("Q=\n" + Q.toString ("%8.4f"));
      System.out.println ("R=\n" + R.toString ("%8.4f"));
      Mcheck.mul (Q, R);
      System.out.println ("Mcheck=\n" + Mcheck.toString ("%8.4f"));
   }

}
