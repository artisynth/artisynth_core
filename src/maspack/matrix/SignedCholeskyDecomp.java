/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.*;
import maspack.util.*;

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
public class SignedCholeskyDecomp {
   protected double[] buf;
   protected double[] sol;
   protected int n;
   protected int r;  // size of the positive definite portion
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

   protected void setSize (int n, int r) {
      ensureCapacity (n);
      this.n = n;
      this.r = r;
   }

   public int getSize() {
      return n;
   }

   public int getR() {
      return r;
   }


   /**
    * Creates an uninitialized SignedCholeskyDecomp.
    */
   public SignedCholeskyDecomp() {
   }

   /**
    * Creates an uninitialized SignedCholeskyDecomp with enough capacity to
    * handle matrices of size <code>n</code>. This capacity will later be
    * increased on demand.
    * 
    * @param n
    * initial maximum matrix size
    */
   public SignedCholeskyDecomp (int n) {
      setSize (n, n);
   }

   /**
    * Creates a SignedCholeskyDecomp for the Matrix specified by M.  M is
    * assumed to be entirely SPD.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @throws ImproperSizeException
    * if M is not square
    */
   public SignedCholeskyDecomp (Matrix M) throws ImproperSizeException {
      factor (M);
   }

   /**
    * Creates a SignedCholeskyDecomp for the Matrix specified by M.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @param r
    * size of the upper-left positive definite block
    * @throws ImproperSizeException
    * if M is not square
    */
   public SignedCholeskyDecomp (Matrix M, int r) throws ImproperSizeException {
      factor (M, r);
   }

   public void factor (Matrix M) throws ImproperSizeException {
      factor (M, M.rowSize());
   }

   /**
    * Peforms a signed Cholesky decomposition on the Matrix M.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @param r
    * size of the upper-left positive definite block
    * @throws ImproperSizeException
    * if M is not square
    * @throws IllegalArgumentException
    * if M is detected to be not symmetric positive definite
    */
   public void factor (Matrix M, int r) throws ImproperSizeException {
      double tmp, anorm;
      int i, j, k;

      if (M.rowSize() != M.colSize()) {
         throw new ImproperSizeException ("Matrix not square");
      }
      setSize (M.rowSize(), r);
      

      // Copy the matrix into buf. The decomposition will be
      // done in-place
      if (M instanceof MatrixNd) {
         ((MatrixNd)M).get (buf, w);
      }
      else {
         for (i = 0; i < n; i++) {
            for (j = 0; j < n; i++) {
               buf[i * w + j] = M.get (i, j);
            }
         }
      }

      // get an approximate norm
      anorm = 0;
      for (i = 0; i < n; i++) {
         double dmag = Math.abs(buf[i * w + i]);
         if (dmag > anorm) {
            anorm = dmag;
         }
      }

      // Gaxpy Cholesky from Golub and Van Loan , "Matrix Computations"

      for (j = 0; j < n; j++) {
         if (j > 0) {
            for (i = j; i < n; i++) {
               tmp = 0;
               for (k = 0; k < j; k++) {
                  if (k < r) {
                     tmp += buf[i * w + k] * buf[j * w + k];
                  }
                  else {
                     tmp -= buf[i * w + k] * buf[j * w + k];
                  }
               }
               buf[i * w + j] -= tmp;
            }
         }
         tmp = buf[j * w + j];
         if (j >= r) {
            tmp = -tmp;
         }
         if (tmp < 0) {
            throw new IllegalArgumentException (
               "Matrix not SPD/SND");
         }
         tmp = Math.sqrt (tmp);
         if (anorm + tmp == anorm) {
            throw new IllegalArgumentException (
               "Matrix not SPD/SND");
         }
         if (j >= r) {
            tmp = -tmp;
         }
         for (i = j; i < n; i++) {
            buf[i * w + j] /= tmp;
         }
      }
      initialized = true;
   }

   /**
    * Gets the components associated with this decomposition.
    * 
    * @param L
    * if non-null, return the lower triangular matrix
    * @param D
    * if non-null, return the diagonal matrix, whose upper left and
    * lower right elements consist of 1 and -1, respectively
    * @throws ImproperStateException
    * if this SignedCholeskyDecomp is uninitialized
    * @throws ImproperSizeException
    * if L is not of the proper dimension and cannot be resized
    */
   public void get (MatrixNd L, VectorNd D) throws ImproperStateException {
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
            for (int j = 0; j <= i; j++) {
               L.buf[idx0 + j] = buf[idx1 + j];
            }
            for (int j = i + 1; j < n; j++) {
               L.buf[idx0 + j] = 0;
            }
            idx0 += L.width;
            idx1 += w;
         }
      }
      if (D != null) {
         if (D.size() != n) {
            D.setSize (n);
         }
         for (int i = 0; i < n; i++) {
            D.set (i, i < r ? 1 : -1);
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

      // Solve L y = vec
      for (i = 0; i < n; i++) {
         sum = b[i+boff];
         for (j = 0; j < i; j++) {
            sum -= x[j+xoff] * buf[i*w + j];
         }
         x[i+xoff] = sum / buf[i*w + i];
      } 
      // Solve D L^T sol = y

      // negative definite part first:
      for (i = n - 1; i >= r; i--) {
         sum = x[i+xoff];
         for (j = i + 1; j < n; j++) {
            sum += x[j+xoff] * buf[j*w + i];
         }
         x[i+xoff] = -sum / buf[i*w + i];
      }
      // negative definite part last:
      for (i = r - 1; i >= 0; i--) {
         sum = x[i+xoff];
         for (j = i + 1; j < n; j++) {
            sum -= x[j+xoff] * buf[j*w + i];
         }
         x[i+xoff] = sum / buf[i*w + i];
      }
      return true;
   }

   protected boolean doSolve (double[] sol, double[] vec) {
      double sum;
      int i, j;

      doSolveL (sol, vec, n);
      soSolveDLT (sol, sol);
      return true;
   }

   protected boolean doSolveL (double[] sol, double[] vec, int maxi) {
      double sum;
      int i, j;

      for (i = 0; i < maxi; i++) {
         sum = vec[i];
         for (j = 0; j < i; j++) {
            sum -= sol[j] * buf[i * w + j];
         }
         sol[i] = sum / buf[i * w + i];
      }
      return true;
   }

  protected void soSolveDLT (double[] sol, double[] vec) {
      double sum;
      int i, j;

      // negative definite part first:
      for (i = n - 1; i >= r; i--) {
         sum = vec[i];
         for (j = i + 1; j < n; j++) {
            sum += sol[j] * buf[j * w + i];
         }
         sol[i] = -sum / buf[i * w + i];
      }
      // positive definite part last:
      for (i = r - 1; i >= 0; i--) {
         sum = vec[i];
         for (j = i + 1; j < n; j++) {
            sum -= sol[j] * buf[j * w + i];
         }
         sol[i] = sum / buf[i * w + i];
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
      if (b.size() < n) {
         throw new ImproperSizeException (
            "b size "+b.size()+" incompatible with decomposition size "+n);
      }
      if (x.size() < n) {
         if (x.isFixedSize()) {
            throw new ImproperSizeException (
               "x size "+x.size()+" incompatible with decomposition size "+n);
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
    * if this SignedCholeskyDecomp is uninitialized
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

      // Algorithm 3.5.1 in Golub and van Loan, Matrix Computations, 2nd edition
      for (j = 0; j < n; j++) {
         pvec[j] = 0;
      }
      for (j = 0; j < n; j++) {
         pposNorm1 = 0;
         pnegNorm1 = 0;
         ypos = (1 - pvec[j]) / buf[w * j + j];
         for (i = j + 1; i < n; i++) {
            ppos[i] = pvec[i] + ypos * buf[w * i + j];
            pposNorm1 += Math.abs (ppos[i]);
         }
         yneg = (-1 - pvec[j]) / buf[w * j + j];
         for (i = j + 1; i < n; i++) {
            pneg[i] = pvec[i] + yneg * buf[w * i + j];
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
      soSolveDLT (yvec, yvec);

      // Compute infinity norm of r
      for (i=0; i<n; i++) {
         double m = Math.abs (yvec[i]);
         if (m > rNormInf) {
            rNormInf = m;
         }
      }

      // Solve L w = r
      doSolveL (yvec, yvec, n);

      // Solve D L^T z = w
      soSolveDLT (yvec, yvec);

      // Compute the infinity norm of z
      for (i=0; i<n; i++) {
         double m = Math.abs (yvec[i]);
         if (m > zNormInf) {
            zNormInf = m;
         }
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

   public double eigenValueRatio() {
      if (!initialized) {
         throw new ImproperStateException ("Uninitialized decomposition");
      }
      double maxL = 0;
      double minL = Double.MAX_VALUE;
      
      for (int i=0; i<n; i++) {
         double lii = Math.abs(buf[i*w+i]);
         if (lii < minL) {
            minL = lii;
         }
         if (lii > maxL) {
            maxL = lii;
         }
      }
      return maxL/minL;      
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
         prod *= buf[i * w + i];
      }
      if (r == n || ((n-r)%2 == 0)) {
         return (prod * prod);         
      }
      else {
         // negate determinant if (n-r) is odd
         return -(prod * prod);         
      }
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
      r = 0;
      initialized = true;
   }

   public void addPosRowAndColumn (VectorNd col) {
      if (!addPosRowAndColumn (col, 0)) {
         throw new IllegalArgumentException (
            "updated matrix is not SPD/SND");
      }
   }

   public boolean addPosRowAndColumn (VectorNd col, double tol) {
      if (col.size() < n + 1) {
         throw new IllegalArgumentException (
            "new column must have " + (n + 1) + " elements");
      }
      if (r > 0) {
         doSolveL (sol, col.getBuffer(), r);
      }
      double sum = col.get (r);
      for (int j = 0; j < r; j++) {
         sum -= sol[j] * sol[j];
      }
      if (sum < tol) {
         return false;
      }
      double ld =  Math.sqrt (sum);
      int oldr = r;
      int oldn = n;
      setSize (n + 1, r + 1);
      if (oldr < oldn) {
         // shift LB down, and LC down and to the right
         for (int i=oldn-1; i>=oldr; i--) {
            // shift LB down
            int idx0 = (i+1)*w;
            int idx1 = i*w;
            for (int j=0; j<oldr; j++) {
               buf[idx0++] = buf[idx1++];
            }
            idx0 = (i+1)*w + i+1;
            idx1 = i*w + i;
            // shift LC down and to the right
            for (int j=i; j>=oldr; j--) {
               buf[idx0--] = buf[idx1--];
            }
         }
      }
      // insert [ la ld ] at i == oldr
      int i = oldr;
      for (int j = 0; j < oldr; j++) {
         buf[i * w + j] = sol[j];
      }
      buf[i * w + i] = ld;
      if (oldr < oldn) {
         double[] lb = new double[oldn-oldr];
         // lb^T = ( mc^T - LB la^T)/ld
         // use updated location for LB
         for (i=r; i<n; i++) {
            sum = col.get(i);
            for (int j=0; j<oldr; j++) {
               // LB is has been moved down by one
               sum -= buf [i*w + j]*sol[j];
            }
            lb[i-r] = sum / ld;
            buf[i*w + oldr] = lb[i-r];
         }
         // now update LC by folding lb into it
         for (i=r; i<n; i++) {
            double z1 = buf[i*w + i - 1];
            double z2 = buf[i*w + i];
            double p = Math.sqrt (z1 * z1 + z2 * z2);
            double c = z1 / p;
            double s = z2 / p;
            buf[i*w + i - 1] = p;
            int off1 = (i+1)*w + i - 1;
            int off2 = off1 + 1;
            for (int k=i+1; k<n; k++) {
               z1 = buf[off1];
               z2 = buf[off2];
               buf[off1] = c * z1 + s * z2;
               buf[off2] = - s * z1 + c * z2;
               off1 += w;
               off2 = off1 + 1;
            }
         }
         // shift LC to the right and reset lb
         for (i=r; i<n; i++) {
            int ioff = i*w + i;
            for (int j=i; j>=r; j--) {
               buf[ioff] = buf[ioff-1];               
               ioff--;
            }
            buf[i*w + oldr] = lb[i-r];
         }
      }
      initialized = true;
      return true;
   }

   public void addNegRowAndColumn (VectorNd col) {
      if (!addNegRowAndColumn (col, 0)) {
         throw new IllegalArgumentException (
            "updated matrix is not SPD/SND");
      }
   }

   public boolean addNegRowAndColumn (VectorNd col, double tol) {
      if (col.size() < n + 1) {
         throw new IllegalArgumentException (
            "new column must have " + (n + 1) + " elements");
      }
      double[] lb = new double[r];
      // solve for lb and compute its norm squared
      if (r > 0) {
         doSolveL (lb, col.getBuffer(), r);
      }
      double lbnorm2 = 0;
      for (int j=0; j<r; j++) {
         lbnorm2 += lb[j]*lb[j];
      }
      // compute ca = -mb + lb LB^T and place this in sol
      for (int i=r; i<n; i++) {
         double sum = -col.get(i);
         for (int j=0; j<r; j++) {
            sum += buf[i*w + j]*lb[j];
         }
         sol[i-r] = sum;
      }
      // now add [ ca cb ] as a row/column to the factorization (- LC LC^T ),
      // where cb = -mc + lbnorm2:

      // solve sol = LC^-1 ca
      for (int i=r; i<n; i++) {
         double sum = sol[i-r];
         for (int j=r; j<i; j++) {
            sum -= sol[j-r] * buf[i * w + j];
         }
         sol[i-r] = sum / buf[i * w + i];
      }
      double sum = -col.get(n) + lbnorm2;
      for (int j=0; j<n-r; j++) {
         sum -= sol[j]*sol[j];
      }
      if (sum < tol) {
         return false;
      } 

      setSize (n + 1, r);

      int i = n-1;
      // add lb:
      for (int j=0; j<r; j++) {
         buf[i*w + j] = lb[j];
      }
      // add [lc ld}:
      for (int j=r; j<i; j++) {
         buf[i*w + j] = sol[j-r];
      }
      buf[i*w + i] = Math.sqrt (sum);
      initialized = true;
      return true;
   }


   public void deleteRowAndColumn (int idx) {
      if (!deleteRowAndColumn (idx, 0)) {
         throw new IllegalArgumentException (
            "updated matrix is not SPD/SND");
      }
   }

   public boolean deleteRowAndColumn (int idx, double tol) {
      if (idx >= n) {
         throw new IllegalArgumentException ("row/column index is out of range");
      }

      // before removal, L is partitioned into
      //
      //     [ L11         ]
      //     [             ]
      // L = [ l21 l22     ]
      //     [             ]
      //     [ L31 l32 L33 ]
      // 
      // where l21, l22, and l32 are the rows/columns to be removed.

      // Do a givens reduction to fold the diagonal elements of L33 into l32
      // and the left part of L33.

      FunctionTimer timer = new FunctionTimer();

      int i;
      int imax;
      if (idx >= r) {
         imax = n;
      }
      else {
         imax = r;
      }
      boolean timing = false; //= (idx < r && n == 512); 
      if (timing) timer.start();
      for (i=idx+1; i<imax; i++) {
         double z1 = buf[i*w + i - 1];
         double z2 = buf[i*w + i];
         double p = Math.sqrt (z1 * z1 + z2 * z2);
         if (z1 < 0) {
            p = -p;
         }
         double c = z1 / p;
         double s = z2 / p;
         buf[i*w + i - 1] = p;
         int off1 = (i+1)*w + i - 1;
         int off2 = off1 + 1;
         for (int k=i+1; k<n; k++) {
            z1 = buf[off1];
            z2 = buf[off2];
            buf[off1] = c * z1 + s * z2;
            buf[off2] = - s * z1 + c * z2;
            off1 += w;
            off2 = off1 + 1;
         }
      }
      // now shift L31 and L33 upwards
      for (i=idx+1; i<n; i++) {
         int jmax;
         if (idx < r) {
            jmax = Math.min (i, r-1);
         }
         else {
            jmax = i;
         }
         for (int j=0; j<jmax; j++) {
            buf[(i-1)*w + j] = buf[i*w + j];
         }
      }
      if (timing) {
         timer.stop();
         System.out.println ("LP: " + timer.result(1));
         timer.start();
      }
      

      if (idx < r) {


         // need to handle C component
         double[] a = new double[n-r];
         // solve a = inv(LC) buf[r:n-1,r-1]
         double anorm2 = 0;
         for (i=r; i<n; i++) {
            double sum = buf [i*w + r-1];
            int aidx = 0;
            int bidx = i*w+r;
            for (int j=r; j<i; j++) {
               sum -= a[aidx++]*buf[bidx++];
            }
            double ai = sum / buf[bidx];
            anorm2 += ai*ai;
            a[i-r] = ai; 
            buf [i*w + r-1] = 0;
         }
         double alpha2 = 1 - anorm2;
         if (alpha2 <= tol) {
            return false;
         }

         if (timing) {
            timer.stop();
            System.out.println ("LB: " + timer.result(1));
            timer.start();           
         }

         double p = Math.sqrt (alpha2);
         for (int j=n-1; j >= r; j--) {
            double z1 = p;
            double z2 = a[j-r];
            p = Math.sqrt (z1 * z1 + z2 * z2);
            double c = z1 / p;
            double s = z2 / p;

            int off1 = j*w + r-1;
            int off2 = j*w + j;
            for (i=j; i<n; i++) {
               z1 = buf[off1];
               z2 = buf[off2];
               buf[off1] = c * z1 + s * z2;
               buf[off2] = - s * z1 + c * z2;
               off1 += w;
               off2 += w;
            }
         }
         if (timing) {
            timer.stop();
            System.out.println ("LC: " + timer.result(1));
            timer.start();
         }

         // shift LC up and to the left
         for (i=r; i<n; i++) {
            int ioff = i*w;
            for (int j=r; j<=i; j++) {
               buf[(ioff-w) + j-1] = buf[ioff + j];
            }
            ioff += w;
         }

         if (timing) {
            timer.stop();
            System.out.println ("SHIFT: " + timer.result(1));
         }

      }

      if (idx < r) {
         setSize (n - 1, r - 1);
      }
      else {
         setSize (n - 1, r);
      }
      
      return true;
   }

   public void deleteRowsAndColumns (int[] idxs) {
      if (idxs.length > n) {
         throw new IllegalArgumentException (
            "Number of rows/columns to delete exceeds decomposition size");
      }
      // make sure the indices are strictly ascending
      boolean ascending = true;
      int lasti = -1;
      for (int k=0; k<idxs.length; k++) {
         int i = idxs[k];
         if (i < 0 || i >= n) {
            throw new IllegalArgumentException (
               "Row/column "+i+" is out of range");
         }
         if (i <= lasti) {
            ascending = false;
         }
         lasti = i;
      }
      if (!ascending) {
         idxs = Arrays.copyOf (idxs, idxs.length);
         ArraySort.sort (idxs);
         for (int k=0; k<idxs.length-1; k++) {
            if (idxs[k] == idxs[k+1]) {
               throw new IllegalArgumentException (
                  "Repeated row/column index "+idxs[k]);
            }
         }
      }
      for (int k=idxs.length-1; k>=0; k--) {
         deleteRowAndColumn (idxs[k]);
      }
   }

}
