/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Arrays;
import maspack.util.ArraySort;

/**
 * Constructs the Cholesky decomposition of a symmetric positive definite
 * matrix. This takes the form <br>
 * M = L L' <br>
 * where M is the original matrix, L is a lower-triangular matrix and L' denotes
 * its transpose. Once this decomposition has been constructed, it can be used
 * to perform various computations related to M, such as solving equations,
 * computing the determinant, or estimating the condition number.
 * 
 * <p>
 * Providing a separate class for the Cholesky decomposition allows an
 * application to perform such decompositions repeatedly without having to
 * reallocate temporary storage space.
 */

public class LDLTDecomposition {
   protected double[] buf;
   protected double[] sol;
   protected int n;
   protected int w;
   protected boolean initialized = false;

   public void ensureCapacity (int cap) {
      if (w < cap) {
         // taken from DynamicArray code in maspack.util:
         int oldCap = w;
         int newCap = oldCap;

         if (newCap - cap < 0) {  // overflow aware
            newCap = oldCap + (oldCap >> 1);  // 1.5x growth
         }
         if (newCap - cap < 0) {
            newCap = cap;
         }
         if (newCap > oldCap) { 
            int n = newCap;
            double[] newBuf = new double[n * n];
            double[] newSol = new double[n];
            if (buf != null) {
               int oldn = this.n;
               for (int i = 0; i < oldn; i++) {
                  for (int j = 0; j < oldn; j++) {
                     newBuf[i * n + j] = buf[i * w + j];
                  }
                  newSol[i] = sol[i];
               }
            }
            w = n;
            buf = newBuf;
            sol = newSol;
         }
      }
   }

   protected void setSize (int n) {
      ensureCapacity (n);
      this.n = n;
   }

   public int getSize() {
      return n;
   }

   /**
    * Creates an uninitialized LDLTDecomposition.
    */
   public LDLTDecomposition() {
   }

   /**
    * Creates an uninitialized LDLTDecomposition with enough capacity to
    * handle matrices of size <code>n</code>. This capacity will later be
    * increased on demand.
    * 
    * @param n
    * initial maximum matrix size
    */
   public LDLTDecomposition (int n) {
      setSize (n);
   }

   /**
    * Creates a LDLTDecomposition for the Matrix specified by M.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @throws ImproperSizeException
    * if M is not square
    */
   public LDLTDecomposition (Matrix M) throws ImproperSizeException {
      factor (M);
   }

   /**
    * Peforms a Cholesky decomposition on the Matrix M.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @throws ImproperSizeException
    * if M is not square
    * @throws IllegalArgumentException
    * if M is detected to be not symmetric positive definite
    */
   public void factor (Matrix M) throws ImproperSizeException {
      double tmp, anorm;
      int i, j, k;

      if (M.rowSize() != M.colSize()) {
         throw new ImproperSizeException ("Matrix not square");
      }
      setSize (M.rowSize());

      // Copy the matrix into buf. The decomposition will be
      // done in-place
      if (M instanceof MatrixNd) {
         ((MatrixNd)M).get (buf, w);
      }
      else {
         for (i = 0; i < n; i++) {
            for (j = 0; j < n; i++) {
               buf[i*w + j] = M.get (i, j);
            }
         }
      }

      // get an approximate norm
      anorm = 0;
      for (i = 0; i < n; i++) {
         if (buf[i*w + i] < 0) {
            throw new IllegalArgumentException (
               "Matrix not symmetric positive definite");
         }
         else if (buf[i*w + i] > anorm) {
            anorm = buf[i*w + i];
         }
      }

      // LDL^T from Golub and Van Loan , "Matrix Computations"

      for (j = 0; j < n; j++) {
         for (i = 0; i < j; i++) {
            sol[i] = buf[j*w + i]*buf[i*w + i];
         }
         tmp = 0;
         for (k = 0; k < j; k++) {
            tmp += buf[j*w + k]*sol[k];
         }
         tmp = buf[j*w + j] - tmp;
         if (tmp < 0 || anorm + tmp == anorm) {
            throw new IllegalArgumentException (
               "Matrix not symmetric positive definite");
         }
         sol[j] = tmp;
         buf[j*w + j] = tmp;
         for (i = j + 1; i < n; i++) {
            tmp = 0;
            for (k=0; k<j; k++) {
               tmp += buf[i*w + k]*sol[k];
            }
            buf[i*w + j] = (buf[i*w + j] - tmp)/sol[j];
         }
      }
      initialized = true;
   }

   /**
    * Gets the components associated with this signed Cholesky decomposition.
    * 
    * @param L
    * if non-null, returns the lower triangular matrix
    * @param D
    * if non-null, returns the diagonal
    * @throws ImproperStateException
    * if this LDLTDecomposition is uninitialized
    */
   public void get (MatrixNd L, VectorNd D)
      throws ImproperStateException {
      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (L != null) {
         if (L.nrows != n || L.ncols != n) {
            L.resetSize (n, n);
         }
         int idx0 = L.base;
         int idx1 = 0;
         for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
               L.buf[idx0 + j] = buf[idx1 + j];
            }
            L.buf[idx0 + i] = 1.0;
            for (int j = i + 1; j < n; j++) {
               L.buf[idx0 + j] = 0;
            }
            idx0 += L.width;
            idx1 += w;
         }
      }
      if (D != null) {
         if (D.size != n) {
            D.setSize (n);
         }
         int idx1 = 0;      
         for (int i = 0; i < n; i++) {
            D.set (i, buf[idx1 + i]);
            idx1 += w;
         }
      }
   }

   /**
    * Solves the linear equation <br>
    * M x = b <br>
    * for x, where M is the original matrix associated with this decomposition,
    * and x and b are vectors.
    * 
    * @param x
    * unknown vector to solve for
    * @param xoff
    * starting offset into <code>x</code>
    * @param b
    * constant vector
    * @param boff
    * starting offset into <code>b</code>
    * @return false if M is singular (within working precision)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException if <code>x</code> or <code>b</code> do not
    * have a length compatible with M
    */
   public boolean solve (double[] x, int xoff, double[] b, int boff) {
      double sum;
      int i, j;

      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      if (b.length < n+boff) {
         throw new ImproperSizeException (
            "b must have length >= " + (n+boff) + " with offset "+boff);
      }
      if (x.length < n+xoff) {
         throw new ImproperSizeException (
            "x must have length >= " + (n+xoff) + " with offset "+xoff);
      }
      if (x == b && xoff > boff) {
         throw new ImproperSizeException (
            "if x == b then xoff must be <= boff");
      }
      for (i = 0; i < n; i++) {
         sum = b[i+boff];
         for (j = 0; j < i; j++) {
            sum -= x[j+xoff] * buf[i*w + j];
         }
         x[i+xoff] = sum;
      }
      for (i = 0; i < n; i++) {
         x[i+xoff] /= buf[i*w + i];
      }
      for (i = n - 2; i >= 0; i--) {
         sum = x[i+xoff];
         for (j = i + 1; j < n; j++) {
            sum -= x[j+xoff] * buf[j*w + i];
         }
         x[i+xoff] = sum;
      }
      return true;
   }

   protected boolean doSolve (double[] sol, double[] vec) {
      double sum;
      int i, j;

      for (i = 0; i < n; i++) {
         sum = vec[i];
         for (j = 0; j < i; j++) {
            sum -= sol[j] * buf[i*w + j];
         }
         sol[i] = sum;
      }
      for (i = 0; i < n; i++) {
         sol[i] /= buf[i*w + i];
      }
      for (i = n - 2; i >= 0; i--) {
         sum = sol[i];
         for (j = i + 1; j < n; j++) {
            sum -= sol[j] * buf[j*w + i];
         }
         sol[i] = sum;
      }
      return true;
   }

  protected boolean doSolveL (double[] sol, double[] vec) {
      double sum;
      int i, j;

      for (i = 0; i < n; i++) {
         sum = vec[i];
         for (j = 0; j < i; j++) {
            sum -= sol[j] * buf[i*w + j];
         }
         sol[i] = sum / buf[i*w + i];
      }
      return true;
   }

   // protected boolean doLeftSolveL (double[] sol, double[] vec) {
   //    double sum;
   //    int i, j;
   //    for (i = n - 1; i >= 0; i--) {
   //       sum = vec[i];
   //       for (j = i + 1; j < n; j++) {
   //          sum -= sol[j] * buf[j*w + i];
   //       }
   //       sol[i] = sum / buf[i*w + i];
   //    }
   //    return true;
   // }

   /**
    * Solves the linear equation <br>
    * M x = b <br>
    * for x, where M is the original matrix associated with this decomposition,
    * and x and b are vectors.
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
      if (x instanceof VectorNd && b instanceof VectorNd) {
         nonSingular =
            doSolve (((VectorNd)x).getBuffer(), ((VectorNd)b).getBuffer());
      }
      else {
         b.get (sol);
         nonSingular = doSolve (sol, sol);
         x.set (sol);
      }
      return nonSingular;
   }

   /**
    * Solves the linear equation <br>
    * M X = B <br>
    * for X, where M is the original matrix associated with this decomposition,
    * and X and B are matrices.
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
         if (!doSolve (sol, sol)) {
            nonSingular = false;
         }
         X.setColumn (k, sol);
      }
      return nonSingular;
   }
   /**
    * Estimates the condition number of the original matrix M associated with
    * this decomposition. M must also be supplied as an argument. The algorithm
    * for estimating the condition numberis based on Section 3.5.4 of Golub and
    * Van Loan, Matrix Computations (Second Edition).
    * 
    * @param M
    * original matrix
    * @return condition number estimate
    * @throws ImproperStateException
    * if this LDLTDecomposition is uninitialized
    * @throws ImproperSizeException
    * if the size of M does not match the size of the current Cholesky
    * decomposition
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

      double[] pvec = new double[n];
      double[] pneg = new double[n];
      double[] ppos = new double[n];
      double[] yvec = new double[n];

      // Algorithm 3.5.1 in Golub and van Loan, Matrix Computations, 2nd
      // edition apply this to just the lower triangular part, and ignore D,
      // which seems to give closer results to the condition estimate of the
      // CholeskyDecomposition.
      for (j = 0; j < n; j++) {
         pvec[j] = 0;
      }
      for (j = 0; j < n; j++) {
         pposNorm1 = 0;
         pnegNorm1 = 0;
         // double diag = buf[w * j + j];  Ignore diag term; assume to be 1
         ypos = (1 - pvec[j]);
         for (i = j + 1; i < n; i++) {
            ppos[i] = pvec[i] + ypos * buf[w * i + j];
            pposNorm1 += Math.abs (ppos[i]);
         }
         yneg = (-1 - pvec[j]);
         for (i = j + 1; i < n; i++) {
            pneg[i] = pvec[i] + yneg *buf[w * i + j];
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

      // Solve D L^T r = y

      for (i = n - 1; i >= 0; i--) {
         sum = 0;
         for (j = i + 1; j < n; j++) {
            sum += yvec[j] * buf[w * j + i];
         }
         yval = yvec[i]/buf[i*w + i] - sum;
         if (Math.abs (yval) > rNormInf) {
            rNormInf = Math.abs (yval);
         }
         yvec[i] = yval;
      }

      // Solve L D w = r

      for (i = 0; i < n; i++) {
         sum = yvec[i];
         for (j = 0; j < i; j++) {
            sum -= yvec[j] * buf[w * i + j];
         }
         yvec[i] = sum;
      }
      for (i = 0; i < n; i++) {
         yvec[i] /= buf[i*w + i];
      }

      // Solve L^T z = w

      for (i = n - 1; i >= 0; i--) {
         sum = 0;
         for (j = i + 1; j < n; j++) {
            sum += yvec[j] * buf[w * j + i];
         }
         yval = yvec[i] - sum;
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

      return (mNormInf * zNormInf / rNormInf);
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
         prod *= buf[i*w + i];
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
         if (!doSolve (sol, sol)) {
            nonSingular = false;
         }
         R.setColumn (j, sol);
      }
      return nonSingular;
   }

   public double[] getBuffer() {
      return buf;
   }

   public int getBufferWidth() {
      return w;
   }

   public void clear() {
      n = 0;
      initialized = true;
   }

}
