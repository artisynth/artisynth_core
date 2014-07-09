/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

/**
 * Constructs the QR decomposition of a matrix. This takes the form <br>
 * M = Q R <br>
 * where M is the original matrix, Q is orthogonal, and R is upper-triangular.
 * Nominally, if M has a size m X n, then if m >= n, R is square with size n and
 * Q is m X n. Otherwise, if m < n, Q is square with size m and R has size m X
 * n.
 * 
 * <p>
 * Note that if m > n, then R and M can actually have sizes p X n and m X p,
 * with n <= p <= m, with the additional rows of R set to zero and the
 * additional columns of Q formed by completing the orthogonal basis.
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
   private double[] colMagSqr = new double[0];
   private boolean factoredInPlaceP = true;

   private enum State {
      UNSET, SET, SET_WITH_PIVOTING
   };

   private State state = State.UNSET;

   private SubMatrixNd SubMat = new SubMatrixNd();

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

   private void houseRowAccum (MatrixNd Q, MatrixNd QR, int m, int n) {
      // given a set of householder row transforms Pn-1 Pn-2 ... P0 = Q
      // stored implictly in QR, this routine computes Q^T

      int j, k;
      int firstCol;

      // int numQcols = Math.min(n,m);
      // SubMat.setDimensions (0, 0, m, numQcols, Q);

      // if (m > n)
      // { firstCol = n-1;
      // SubMat.resetDimensions (n, n, m-n, 0);
      // }
      // else
      // { firstCol = m-2;
      // SubMat.resetDimensions (m-1, m-1, 1, 1);
      // }

      int numQcols = Q.colSize();
      SubMat.setDimensions (0, 0, m, numQcols, Q);

      if (m > n) {
         firstCol = n - 1;
         SubMat.resetDimensions (n, n, m - n, numQcols - n);
      }
      else {
         firstCol = m - 2;
         SubMat.resetDimensions (m - 1, m - 1, 1, 1);
      }

      for (j = firstCol; j >= 0; j--) {
         vec[0] = 1;
         int Qbase = j * QR.width + j + QR.base;
         for (k = 1; k < m - j; k++) {
            vec[k] = QR.buf[k * QR.width + Qbase];
         }
         SubMat.resetDimensions (-1, -1, SubMat.nrows + 1, SubMat.ncols + 1);
         SubMat.rowHouseMul (vec, wec);
      }

      SubMat.clear();
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

   private void doFactor() {
      int maxd = Math.max (nrows, ncols);
      if (vec.length < maxd) {
         vec = new double[maxd];
         wec = new double[maxd];
      }

      SubMat.setDimensions (0, 0, nrows, ncols, QR);

      int columnLimit = (nrows > ncols ? ncols : nrows - 1);
      for (int j = 0; j < columnLimit; j++) {
         SubMat.rowHouseReduce (vec, wec, true);
         if (j < columnLimit - 1) {
            SubMat.resetDimensions (1, 1, SubMat.nrows - 1, SubMat.ncols - 1);
         }
      }
      SubMat.clear();
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

      SubMat.setDimensions (0, 0, nrows, ncols, QR);

      int columnLimit = (nrows > ncols ? ncols : nrows - 1);
      for (int j = 0; j < columnLimit; j++) {
         piv[j] = pivotCol;

         // exchange j and the pivotCol ...
         QR.exchangeColumns (j, pivotCol);
         double tmp = colMagSqr[j];
         colMagSqr[j] = colMagSqr[pivotCol];
         colMagSqr[pivotCol] = tmp;

         SubMat.rowHouseReduce (vec, wec, true);
         // System.out.println ("QR=\n" + QR.toString("%10.4f"));

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
         else if (j < columnLimit - 1) {
            SubMat.resetDimensions (1, 1, SubMat.nrows - 1, SubMat.ncols - 1);
         }
      }
      SubMat.clear();
      state = State.SET_WITH_PIVOTING;
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
    * p, where min(n,m) <= p <= m (and so must be square if m <= n). Extra
    * columns of Q are formed by completing the original basis. R is an upper
    * triangular matrix which can be q X n, where min(n,m) <= q <= m (and so
    * must be m X n if m <= n). Extra rows of R are set to zero.
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
         houseRowAccum (Q, QR, nrows, ncols);
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
   private void rowHouseMulVec (double[] u, int base, double[] v, int m) {
      double sum = 0;
      for (int k = 0; k < m; k++) {
         sum += v[k] * v[k];
      }
      double beta = -2 / sum;

      sum = 0;
      for (int k = 0; k < m; k++) {
         sum += u[base + k] * v[k];
      }
      double w = beta * sum;

      for (int k = 0; k < m; k++) {
         u[base + k] += w * v[k];
      }
   }

   private boolean doSolve (double[] sol) {
      // compute sol' = Q^T sol
      int lastCol = (nrows > ncols ? ncols - 1 : ncols - 2);
      for (int j = 0; j <= lastCol; j++) {
         vec[0] = 1;
         int Qbase = j * QR.width + j + QR.base;
         for (int k = 1; k < nrows - j; k++) {
            vec[k] = QR.buf[k * QR.width + Qbase];
         }
         rowHouseMulVec (sol, j, vec, nrows - j);
      }
      return doSolveR (sol);
   }

   /**
    * Solve R x = sol by back substitution
    */
   private boolean doSolveR (double[] sol) {
      boolean nonSingular = true;

      for (int i = ncols - 1; i >= 0; i--) {
         int Qbase = i * QR.width + QR.base;
         double sum = sol[i];
         for (int j = i + 1; j < ncols; j++) {
            sum -= sol[j] * QR.buf[Qbase + j];
         }
         double d = QR.buf[Qbase + i];
         if (d == 0) {
            nonSingular = false;
         }
         sol[i] = sum / d;
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
    * Computes a least-squares solution to the linear equation <br>
    * M x = b <br>
    * where M is the original matrix associated with this decomposition, and x
    * and b are vectors. The number of rows in M must equal or exceed the number
    * of columns.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M does not have full column rank (within working
    * precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if M has fewer rows than columns, if b does not have a size compatible
    * with M, or if x does not have a size compatible with M and cannot be
    * resized.
    */
   public boolean solve (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
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
      x.set (wec);
      return nonSingular;
   }

   /**
    * Computes a least-squares solution to the linear equation <br>
    * M X = B <br>
    * where M is the original matrix associated with this decomposition, and X
    * and B are matrices. The number of rows in M must equal or exceed the
    * number of columns.
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
    * if M has fewer rows than columns, if B has a different number of rows than
    * M, or if X has a different number of rows than M or a different number of
    * columns than B and cannot be resized.
    */
   public boolean solve (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
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
    * Computes a solution to the linear equation <br>
    * R x = b <br>
    * where R is the upper triangular matrix associated with this decomposition,
    * and x and b are vectors. Note that R is a square matrix with a size equal
    * to the the number of columns in the original matrix M.
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
      nonSingular = doSolveR (wec);
      x.set (wec);
      return nonSingular;
   }

   /**
    * Computes a solution to the linear equation <br>
    * R X = B <br>
    * where R is the upper triangular matrix associated with this decomposition,
    * and X and B are matrices. Note that R is a square matrix with a size equal
    * to the the number of columns in the original matrix M.
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
      if (B.rowSize() != ncols) {
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
    * Computes a left solution to the linear equation <br>
    * x R = b <br>
    * where R is the upper triangular matrix associated with this decomposition,
    * and x and b are vectors. Note that R is a square matrix with a size equal
    * to the the number of columns in the original matrix M.
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
   public boolean leftSolveR (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
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
      x.set (wec);
      return nonSingular;
   }

   /**
    * Computes a left solution to the linear equation <br>
    * X R = B <br>
    * where R is the upper triangular matrix associated with this decomposition,
    * and X and B are matrices. Note that R is a square matrix with a size equal
    * to the the number of columns in the original matrix M.
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
   public boolean leftSolveR (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
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
    * this decomposition. The number of rows in the original matrix M should
    * equal or exceed the number of columns. The algorithm for estimating the
    * condition number is given in Section 3.5.4 of Golub and Van Loan, Matrix
    * Computations (Second Edition).
    * 
    * @return condition number estimate
    * @throws ImproperStateException
    * if this QRDecomposition is uninitialized
    * @throws ImproperSizeException
    * if M has fewer rows than columns.
    */
   public double conditionEstimate() throws ImproperStateException {
      if (state == State.UNSET) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (nrows < ncols) {
         throw new ImproperStateException ("M has fewer rows than columns");
      }

      int i, j;

      double[] pvec = new double[ncols];
      double[] ppos = new double[ncols];
      double[] pneg = new double[ncols];
      double[] yvec = new double[ncols];

      for (i = 0; i < ncols; i++) {
         pvec[i] = 0;
      }
      for (j = ncols - 1; j >= 0; j--) {
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
      for (i = 0; i < ncols; i++) {
         double abs = Math.abs (yvec[i]);
         if (abs > yNormInf) {
            yNormInf = abs;
         }
      }

      // Compute the infinity norm of R
      double RNormInf = 0;
      for (i = 0; i < ncols; i++) {
         int Qbase = QR.width * i + QR.base;
         double sum = 0;
         for (j = i; j < ncols; j++) {
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
      // determinant of Q is -1^p, where p is the number of HouseHolder
      // transformations
      int p = (nrows > ncols ? ncols : nrows - 1);
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
