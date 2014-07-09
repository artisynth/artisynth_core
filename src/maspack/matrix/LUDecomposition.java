/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Constructs the LU decomposition of a square matrix. This takes the form <br>
 * P M = L U <br>
 * where P is a permutation matrix, M is the original matrix, L is
 * unit-lower-triangular, and U is upper-triangular. Once an LU decomposition
 * has been constructed, it can be used to perform various computations related
 * to M, such as solving equations, computing the determinant, or estimating the
 * condition number.
 * 
 * <p>
 * Providing a separate class for the LU decomposition allows an application to
 * perform such decompositions repeatedly without having to reallocate temporary
 * storage space.
 */
public class LUDecomposition {
   private double[] buf;
   private double[] sol;
   private int[] perm;
   private int n;
   private boolean initialized = false;

   // these are used only for condition number estimation
   private double[] pvec = new double[0];
   private double[] pneg = new double[0];
   private double[] ppos = new double[0];
   private double[] yvec = new double[0];

   private void setSize (int n) {
      if (perm == null || perm.length < n) {
         buf = new double[n * n];
         sol = new double[n];
         perm = new int[n];
      }
      this.n = n;
   }

   /**
    * Creates an uninitialized LUDecomposition.
    */
   public LUDecomposition() {
   }

   /**
    * Creates an uninitialized LUDecomposition with enough capacity to handle
    * matrices of size <code>n</code>. This capacity will later be increased
    * on demand.
    * 
    * @param n
    * initial maximum matrix size
    */
   public LUDecomposition (int n) {
      setSize (n);
   }

   /**
    * Creates an LUDecomposition for the Matrix specified by M.
    * 
    * @param M
    * matrix to perform the LU decomposition on
    * @throws ImproperSizeException
    * if M is not square
    */
   public LUDecomposition (Matrix M) throws ImproperSizeException {
      factor (M);
   }

   /**
    * Peforms an LU decomposition on the Matrix M.
    * 
    * @param M
    * matrix to perform the LU decomposition on
    * @throws ImproperSizeException
    * if M is not square
    */
   public void factor (Matrix M) throws ImproperSizeException {
      double tmp, max, d;
      int i, j, k, max_i;

      if (M.rowSize() != M.colSize()) {
         throw new ImproperSizeException ("Matrix not square");
      }
      setSize (M.rowSize());

      // Copy the matrix into buf. The LU decomposition will be
      // done in-place
      M.get (buf);

      for (j = 0; j < n; j++) {
         for (k = 0; k < j; k++) {
            tmp = buf[k * n + j];
            buf[k * n + j] = buf[perm[k] * n + j];
            buf[perm[k] * n + j] = tmp;
         }
         for (k = 0; k < j; k++) {
            tmp = buf[k * n + j];
            for (i = k + 1; i < j; i++) {
               buf[i * n + j] -= buf[i * n + k] * tmp;
            }
         }
         for (k = 0; k < j; k++) {
            tmp = buf[k * n + j];
            for (i = j; i < n; i++) {
               buf[i * n + j] -= buf[i * n + k] * tmp;
            }
         }
         // find pivot index
         max_i = j;
         d = buf[j * n + j];
         max = (d >= 0 ? d : -d);
         for (i = j + 1; i < n; i++) {
            d = buf[i * n + j];
            d = (d >= 0 ? d : -d);
            if (d > max) {
               max_i = i;
               max = d;
            }
         }
         perm[j] = max_i;
         if (j != max_i) {
            for (k = 0; k <= j; k++) {
               tmp = buf[j * n + k];
               buf[j * n + k] = buf[max_i * n + k];
               buf[max_i * n + k] = tmp;
            }
         }
         d = buf[j * n + j];
         if (d != 0) {
            for (i = j + 1; i < n; i++) {
               buf[i * n + j] /= d;
            }
         }
      }
      initialized = true;
   }

   /**
    * Gets the matrices associated with the LU decomposition. Each argument is
    * optional; values will be returned into them if they are present.
    * 
    * @param L
    * unit lower triangular matrix
    * @param U
    * upper triangular matrix
    * @param rperm
    * indices of the row permuation matrix P, such that the i-th row of P M is
    * given by row <code>perm[i]</code> of M.
    * @throws ImproperStateException
    * if this LUDecomposition is uninitialized
    * @throws ImproperSizeException
    * if L or U are not of the proper dimension and cannot be resized, or if the
    * length of perm is less than the size of M.
    */
   public void get (MatrixNd L, MatrixNd U, int[] rperm)
      throws ImproperStateException, ImproperSizeException {
      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (rperm != null) {
         if (rperm.length < n) {
            throw new ImproperSizeException ("perm has length less than n");
         }
         for (int i = 0; i < n; i++) {
            rperm[i] = i;
         }
         for (int i = 0; i < n; i++) {
            int k = perm[i];
            if (k != i) {
               int tmp = rperm[k];
               rperm[k] = rperm[i];
               rperm[i] = tmp;
            }
         }
      }
      if (L != null) {
         if (L.nrows != n || L.ncols != n) {
            if (L.isFixedSize()) {
               throw new ImproperSizeException ("Incompatible dimensions");
            }
            else {
               L.resetSize (n, n);
            }
         }
         int idx0 = L.base;
         int idx1 = 0;
         for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
               L.buf[idx0 + j] = buf[idx1 + j];
            }
            L.buf[idx0 + i] = 1;
            for (int j = i + 1; j < n; j++) {
               L.buf[idx0 + j] = 0;
            }
            idx0 += L.width;
            idx1 += n;
         }
      }
      if (U != null) {
         if (U.nrows != n || U.ncols != n) {
            if (U.isFixedSize()) {
               throw new ImproperSizeException ("Incompatible dimensions");
            }
            else {
               U.resetSize (n, n);
            }
         }
         int idx0 = U.base;
         int idx1 = 0;
         for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
               U.buf[idx0 + j] = 0;
            }
            for (int j = i; j < n; j++) {
               U.buf[idx0 + j] = buf[idx1 + j];
            }
            idx0 += U.width;
            idx1 += n;
         }
      }
   }

   private boolean dosolve (double[] sol) {
      boolean nonSingular = true;
      double sum;
      int i, j;
      int i0 = -1;

      for (i = 0; i < n; i++) {
         sum = sol[perm[i]];
         sol[perm[i]] = sol[i];
         if (i0 >= 0) {
            for (j = i0; j < i; j++) {
               sum -= sol[j] * buf[i * n + j];
            }
         }
         else if (sum != 0) {
            i0 = i;
         }
         sol[i] = sum;
      }
      for (i = n - 1; i >= 0; i--) {
         sum = sol[i];
         for (j = i + 1; j < n; j++) {
            sum -= sol[j] * buf[i * n + j];
         }
         double d = buf[i * n + i];
         if (d == 0) {
            nonSingular = false;
         }
         sol[i] = sum / d;
      }
      return nonSingular;
   }

   /**
    * Solves the linear equation <br>
    * M x = b <br>
    * where M is the original matrix associated with this decomposition, and x
    * and b are vectors.
    * 
    * @param x
    * unknown vector to solve for
    * @param b
    * constant vector
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if b does not have a size compatible with M, or if x does not have a size
    * compatible with M and cannot be resized.
    */
   public boolean solve (Vector x, Vector b)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (b.size() != n) {
         throw new ImproperSizeException ("improper size for b");
      }
      if (x.size() != n) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException ("improper size for x");
         }
         else {
            x.setSize (n);
         }
      }
      b.get (sol);
      nonSingular = dosolve (sol);
      x.set (sol);
      return nonSingular;
   }

   /**
    * Solves the linear equation <br>
    * M X = B <br>
    * where M is the original matrix associated with this decomposition, and X
    * and B are matrices.
    * 
    * @param X
    * unknown matrix to solve for
    * @param B
    * constant matrix
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if B has a different number of rows than M, or if X has a different number
    * of rows than M or a different number of columns than B and cannot be
    * resized.
    */
   public boolean solve (DenseMatrix X, Matrix B)
      throws ImproperStateException, ImproperSizeException {
      boolean nonSingular = true;

      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (B.rowSize() != n) {
         throw new ImproperSizeException ("improper size for B");
      }
      if (X.colSize() != B.colSize() || X.rowSize() != n) {
         if (X.isFixedSize()) {
            throw new ImproperSizeException ("improper size for X");
         }
         else {
            X.setSize (n, B.colSize());
         }
      }

      for (int k = 0; k < B.colSize(); k++) {
         B.getColumn (k, sol);
         if (!dosolve (sol)) {
            nonSingular = false;
         }
         X.setColumn (k, sol);
      }
      return nonSingular;
   }

   /**
    * Estimates the condition number of the original matrix M associated with
    * this decomposition. M must also be supplied as an argument. The algorithm
    * for estimating the condition number is given in Section 3.5.4 of Golub and
    * Van Loan, Matrix Computations (Second Edition).
    * 
    * @param M
    * original matrix
    * @return condition number estimate
    * @throws ImproperStateException
    * if this LUDecomposition is uninitialized
    * @throws ImproperSizeException
    * if the size of M does not match the size of the current LU decomposition
    */
   public double conditionEstimate (Matrix M)
      throws ImproperStateException, ImproperSizeException {

      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (M.rowSize() != n || M.colSize() != n) {
         throw new ImproperSizeException ("M does not match decomposition size");
      }

      double ypos, yneg, yval;
      double pposNorm1;
      double pnegNorm1;
      double sum;
      double zNormInf;
      double rNormInf;
      int i, j;

      if (pvec.length < n) {
         pvec = new double[n];
         pneg = new double[n];
         ppos = new double[n];
         yvec = new double[n];
      }

      for (j = 0; j < n; j++) {
         pvec[j] = 0;
      }
      for (j = 0; j < n; j++) {
         pposNorm1 = 0;
         pnegNorm1 = 0;
         ypos = (1 - pvec[j]) / buf[n * j + j];
         for (i = j + 1; i < n; i++) {
            ppos[i] = pvec[i] + ypos * buf[n * j + i];
            pposNorm1 += Math.abs (ppos[i]);
         }
         yneg = (-1 - pvec[j]) / buf[n * j + j];
         for (i = j + 1; i < n; i++) {
            pneg[i] = pvec[i] + yneg * buf[n * j + i];
            pnegNorm1 += Math.abs (pneg[i]);
         }
         if (Math.abs (ypos) + pposNorm1 >= Math.abs (yneg) + pnegNorm1) {
            yvec[j] = ypos;
            for (i = j + 1; i < n; i++) {
               pvec[i] = ppos[i];
            }
         }
         else {
            yvec[j] = yneg;
            for (i = j + 1; i < n; i++) {
               pvec[i] = pneg[i];
            }
         }
      }

      rNormInf = 0;
      zNormInf = 0;

      // Solve L^T r = y

      for (i = n - 1; i >= 0; i--) {
         sum = 0;
         for (j = i + 1; j < n; j++) {
            sum += yvec[j] * buf[n * j + i];
         }
         yval = yvec[i] - sum;
         if (Math.abs (yval) > rNormInf) {
            rNormInf = Math.abs (yval);
         }
         yvec[i] = yval;
      }

      // Solve L w = P r

      for (i = 0; i < n; i++) {
         sum = yvec[perm[i]];
         yvec[perm[i]] = yvec[i];
         for (j = 0; j < i; j++) {
            sum -= yvec[j] * buf[n * i + j];
         }
         yvec[i] = sum;
      }

      // Solve U z = w

      for (i = n - 1; i >= 0; i--) {
         sum = 0;
         for (j = i + 1; j < n; j++) {
            sum += yvec[j] * buf[n * i + j];
         }
         yval = (yvec[i] - sum) / buf[n * i + i];
         if (Math.abs (yval) > zNormInf) {
            zNormInf = Math.abs (yval);
         }
         yvec[i] = yval;
      }

      // Compute the infinity norm of M

      double mNormInf = 0;
      for (i = 0; i < n; i++) {
         M.getRow (i, yvec);
         sum = 0;
         for (j = 0; j < n; j++) {
            sum += Math.abs (yvec[j]);
         }
         if (sum > mNormInf) {
            mNormInf = sum;
         }
      }

      if (mNormInf * zNormInf == 0 && rNormInf == 0) {
         return Double.POSITIVE_INFINITY;
      }
      else {
         return (mNormInf * zNormInf / rNormInf);
      }
   }

   /**
    * Compute the determinant of the original matrix M associated with this
    * decomposition.
    * 
    * @return determinant
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    */
   public double determinant() throws ImproperStateException {
      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      double prod = 1.0;
      for (int i = 0; i < n; i++) {
         prod *= buf[i * n + i];
         if (perm[i] != i) {
            prod = -prod;
         }
      }
      return (prod);
   }

   /**
    * Computes the inverse of the original matrix M associated with this
    * decomposition, and places the result in R.
    * 
    * @param R
    * matrix in which the inverse is stored
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if R does not have the same size as M and cannot be resized.
    */
   public boolean inverse (DenseMatrix R) throws ImproperStateException {
      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (R.rowSize() != n || R.colSize() != n) {
         if (R.isFixedSize()) {
            throw new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            R.setSize (n, n);
         }
      }
      boolean nonSingular = true;
      for (int j = 0; j < n; j++) {
         for (int i = 0; i < n; i++) {
            sol[i] = (i == j ? 1 : 0);
         }
         if (!dosolve (sol)) {
            nonSingular = false;
         }
         R.setColumn (j, sol);
      }
      return nonSingular;
   }
}
