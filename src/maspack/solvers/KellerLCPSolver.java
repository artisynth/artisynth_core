/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Solves linear complementarity problems (LCPs) for symmetric positive
 * semi-definite (SPSD) matrices using Kellers's method. Details on Keller's
 * method can be found in Claude Lacoursiere's Ph.D. thesis. <i>Ghosts and
 * Machines: Regularized Variational Methods for Interactive Simulations of
 * Multibodies with Dry Frictional Contact</i>, as well as ``Algorithms for
 * Linear Complementarity Problems'', by Joaquim Judice (1994).
 */
public class KellerLCPSolver implements LCPSolver {
   
   protected double[] myMbuf;
   protected double[] myQbuf;
   protected double[] myRbuf;
   protected double[] myZbuf;
   protected boolean[] myZBasic;
   protected int[] myZState;
   protected boolean[] myPivotOK;
   protected int mySize;

   protected double myTol = 1e-12;
   protected int myIterationLimit = 10;
   protected int myIterationCnt;
   protected int myPivotCnt;

   public static final int SHOW_NONE = 0x00;
   public static final int SHOW_PIVOTS = 0x01;
   public static final int SHOW_MIN_RATIO = 0x02;
   public static final int SHOW_QM = 0x04;
   public static final int SHOW_ALL = (SHOW_PIVOTS | SHOW_MIN_RATIO | SHOW_QM);

   protected int myDebug = SHOW_NONE;

   public static final int Z_FREE = 0x1;
   public static final int Z_UPPER_BOUNDED = 0x2;
   public static final int Z_LOWER_BOUNDED = 0x2;

   public int getDebug() {
      return myDebug;
   }

   public void setDebug (int code) {
      myDebug = code;
   }

   /**
    * Creates a new Keller solver.
    */
   public KellerLCPSolver() {
      myMbuf = new double[0];
      myQbuf = new double[0];
      myRbuf = new double[0];
      myZbuf = new double[0];
      myZBasic = new boolean[0];
      myZState = new int[0];
      myPivotOK = new boolean[0];
      mySize = 0;
   }

   /**
    * Returns the numeric tolerance for this solver.
    * 
    * @return numeric tolerance
    * @see #setTolerance
    */
   public double getTolerance() {
      return myTol;
   }

   /**
    * Sets the numeric tolerance for this solver. This is used to determine when
    * q' {@code >=} 0, as described in the class documentation. In particular, a
    * solution will be considered found whenever q' {@code >=} -tol.
    * 
    * @param tol
    * new numeric tolerance. Negative numbers will be truncated to 0.
    * @see #getTolerance
    */
   public void setTolerance (double tol) {
      myTol = Math.max (tol, 0);
   }

   /**
    * Gets the iteration limit for this solver. This value multiplied by the
    * size of the LCP matrix gives the maximum number of iterations allowed for
    * the solver.
    * 
    * @return iteration limit
    */
   public int getIterationLimit() {
      return myIterationLimit;
   }

   /**
    * Sets the iteration limit for this solver. This value multiplied by the
    * size of the LCP matrix gives the maximum number of iterations allowed for
    * the solver. The maximum number of iterations is specified in this way
    * because the expected number of iterations for Keller's algorithm is
    * proportional to the size of the LCP matrix.
    * 
    * @param limit
    * new iteration limit
    */
   public void setIterationLimit (int limit) {
      myIterationLimit = limit;
   }

   /**
    * {@inheritDoc}
    */
   public int getIterationCount() {
      return myIterationCnt;
   }

   /**
    * {@inheritDoc}
    */
   public int getPivotCount() {
      return myPivotCnt;
   }

   /**
    * {@inheritDoc}
    */
   public double getLastSolveTol() {
      return myTol;
   }

   /**
    * {@inheritDoc}
    */
   public int numFailedPivots() {
      return 0;
   }

   /**
    * Returns the current pivoted value of q in an array of doubles. Subclasses
    * can override this method if they have a particularly efficient way of
    * determining q.
    * 
    * @return current pivoted value of q.
    */
   protected double[] getQv() {
      return myQbuf;
   }

   /**
    * Returns the r-th column of the current pivoted value of M, in an array of
    * doubles. Subclasses can override this method if they have a particularly
    * efficient way of performining the computation.
    * 
    * @param r
    * column to return
    * @return r-th column of the current pivoted value of M
    */
   protected double[] getMv (int r) {
      for (int i = 0; i < mySize; i++) {
         myRbuf[i] = myMbuf[i * mySize + r];
      }
      return myRbuf;
   }

   protected boolean principalPivot (int s) {
      pivot (myMbuf, myQbuf, s, s, mySize, mySize);
      myPivotCnt++;
      return true;
   }

   protected void copyBooleanArray (boolean[] dest, boolean[] src, int n) {
      for (int i = 0; i < n; i++) {
         dest[i] = src[i];
      }
   }

   /**
    * {@inheritDoc}
    */
   public Status solve (
      VectorNd z, VectorNi state, MatrixNd M, VectorNd q) {
      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException ("Matrix is not square");
      }
      mySize = z.size();
      if (M.rowSize() != mySize) {
         throw new IllegalArgumentException (
            "z and M do not have the same size");
      }
      if (q.size() != mySize) {
         throw new IllegalArgumentException (
            "z and q do not have the same sizes");
      }
      if (state != null && state.size() < mySize) {
         throw new IllegalArgumentException ("state has size less than z");
      }
      // allocate storage space
      if (myMbuf.length < mySize * mySize) {
         myMbuf = new double[mySize * mySize];
      }
      if (myQbuf.length < mySize) {
         myQbuf = new double[mySize];
      }
      if (myRbuf.length < mySize) {
         myRbuf = new double[mySize];
      }
      if (myZbuf.length < mySize) {
         myZbuf = new double[mySize];
      }
      if (myZBasic.length < mySize) {
         myZBasic = new boolean[mySize];
      }
      if (myZState.length < mySize) {
         myZState = new int[mySize];
      }
      M.get (myMbuf);
      q.get (myQbuf);
      for (int i = 0; i < mySize; i++) {
         myZBasic[i] = false;
      }
      Status status = dosolve (myZbuf, myZBasic, mySize);
      z.set (myZbuf);
      if (state != null) {
         for (int i=0; i<mySize; i++) {
            state.set (i, myZBasic[i] ? Z_VAR : W_VAR_LOWER);
         }
      }
      return status;
   }

   /**
    * Returns {@link Status#UNIMPLEMENTED} since BLCP problems are not
    * supported by this solver class.
    */
   public Status solve (
      VectorNd z, VectorNd w, VectorNi state, MatrixNd M, VectorNd q, 
      VectorNd lo, VectorNd hi, int nub) {
      
      myIterationCnt = 0;
      myPivotCnt = 0;
      return Status.UNIMPLEMENTED;
   }

   /**
    * {@inheritDoc}
    */  
   public boolean isBLCPSupported() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */  
   public boolean isWarmStartSupported() {
      return false;
   }

   /**
    * Multiplies the pivoted system matrix, produced by the last solve, by a
    * vector.
    * 
    * @param res
    * used to store result
    * @param vec
    * vector to be multiplied
    */
   public void mulPivotedMatrix (double[] res, double[] vec) {
      if (myMbuf == null) {
         throw new ImproperStateException ("No call has been made to solve");
      }
      if (res.length < mySize || vec.length < mySize) {
         throw new ImproperSizeException (
            "res and/or vec are not as large as the matrix");
      }
      double[] ivec = vec;
      if (vec == res) {
         ivec = new double[mySize];
         for (int i = 0; i < mySize; i++) {
            ivec[i] = vec[i];
         }
      }
      for (int i = 0; i < mySize; i++) {
         double sum = 0;
         for (int j = 0; j < mySize; j++) {
            sum += myMbuf[i * mySize + j] * ivec[j];
         }
         res[i] = sum;
      }
   }

   /**
    * Multiplies the pivoted system matrix, produced by the last solve, by
    * another matrix.
    * 
    * @param MR
    * matrix result
    * @param M1
    * matrix to be multiplied
    */
   public void mulPivotedMatrix (MatrixNd MR, MatrixNd M1) {
      if (myMbuf == null) {
         throw new ImproperStateException ("No call has been made to solve");
      }
      if (M1.colSize() != MR.colSize()) {
         throw new ImproperSizeException ("MR and M1 have incompatible sizes");
      }
      if (M1.rowSize() != mySize || MR.rowSize() != mySize) {
         throw new ImproperSizeException (
            "MR and/or M1 incompatible with system matrix");
      }
      double[] buf = M1.getBuffer();
      int base = M1.getBufferBase();
      int width = M1.getBufferWidth();
      if (M1 == MR) { // need to store M1 in a temp
         MatrixNd M = new MatrixNd (M1);
         buf = M.getBuffer();
         base = M.getBufferBase(); // should be 0
         width = M.getBufferWidth(); // should equal M1.colSize()
      }
      for (int i = 0; i < MR.rowSize(); i++) {
         for (int j = 0; j < MR.colSize(); j++) {
            double sum = 0;
            for (int k = 0; k < mySize; k++) {
               sum += myMbuf[i * mySize + k] * buf[width * k + j + base];
            }
            MR.set (i, j, sum);
         }
      }
   }

   private void printVec (String msg, double[] buf, int n) {
      VectorNd v = new VectorNd (n);
      v.set (buf);
      System.out.println (msg + " " + v.toString ("%g"));
   }


   private double getMinZ (double[] vec, boolean[] zBasic, int n) {
      double min = Double.POSITIVE_INFINITY;
      for (int i = 0; i < n; i++) {
         if (zBasic[i]) {
            double x = vec[i];
            if (x < min) {
               min = x;
            }
         }
      }
      return min;
   }

   protected Status dosolve (double[] zsol, boolean[] zBasic, int n) {
      int maxIterations = myIterationLimit * n;
      myIterationCnt = 0;
      myPivotCnt = 0;

      if (myPivotOK.length < mySize) {
         myPivotOK = new boolean[mySize];
      }

      double[] qv;
      double[] mv;

      boolean mixed = false;

      qv = getQv();

      while (myIterationCnt < maxIterations) {
         int s = -1;

         // printVec ("Q ", qv, n);
         double minVal = 0;
         for (int i = 0; i < n; i++) {
            if (!zBasic[i]) {
               if (s == -1 || qv[i] < minVal) {
                  s = i;
                  minVal = qv[i];
               }
            }
         }

         if (minVal >= -myTol) { // solution found
            double minq = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
               zsol[i] = (zBasic[i] ? qv[i] : 0);
               if (zsol[i] < minq) {
                  minq = zsol[i];
               }
            }
            if (minq < -myTol) {
               System.out.println ("ERROR: min Q=" + minq);
               System.out.println ("minZ = " + getMinZ (qv, zBasic, n));
               return Status.NUMERIC_ERROR;

            }
            else {
               return Status.SOLVED;
            }
         }
         // s has now been changed, so get mv_s
         mv = getMv (s);

         double theta, theta1, theta2;
         do {
            // printVec ("R ", mv, n);

            double mss = mv[s];

            if (mss > myTol) {
               theta1 = -qv[s] / mss;
            }
            else {
               theta1 = Double.POSITIVE_INFINITY;
            }
            theta2 = Double.POSITIVE_INFINITY;
            int r = -1;
            for (int i = 0; i < n; i++) {
               if (zBasic[i] && mv[i] < -myTol) {
                  double ratio = -qv[i] / mv[i];
                  if (ratio < theta2) {
                     theta2 = ratio;
                     r = i;
                  }
               }
            }
            theta = Math.min (theta1, theta2);
            if (theta == Double.POSITIVE_INFINITY) {
               return Status.NO_SOLUTION;
            }
            else if (theta == theta1) {
               principalPivot (s);
               zBasic[s] = !zBasic[s];
               qv = getQv();
            }
            else {
               principalPivot (r);
               zBasic[r] = !zBasic[r];
               qv = getQv();
               mv = getMv (s);
            }
            double minZ = getMinZ (qv, zBasic, n);
            // if (minZ < 0) {
            //    System.out.println ("minZ=" + minZ + " "
            //    + (theta == theta1 ? "s" : "r"));
            // }
            myIterationCnt++;
         }
         while (theta != theta1);
      }
      return Status.ITERATION_LIMIT_EXCEEDED;
   }

   private void estimateCondition (boolean[] zbasic) {
      int nzb = 0;
      for (int i = 0; i < mySize; i++) {
         if (zbasic[i]) {
            nzb++;
         }
      }
      MatrixNd MaaInv = new MatrixNd (nzb, nzb);
      int ia = 0;
      for (int i = 0; i < mySize; i++) {
         if (zbasic[i]) {
            int ja = 0;
            for (int j = 0; j < mySize; j++) {
               if (zbasic[j]) {
                  MaaInv.set (ia, ja, myMbuf[i * mySize + j]);
                  ja++;
               }
            }
            ia++;
         }
      }
      LUDecomposition LU = new LUDecomposition();
      LU.factor (MaaInv);
   }

   /**
    * A general purpose pivot routine that transforms Mv and qv to reflect the
    * exchange of row variable s with column variable r.
    * 
    * @param Mv
    * storage buffer for Mv
    * @param qv
    * storage buffer for qv
    * @param r
    * column variable
    * @param s
    * row variable
    * @param nr
    * number of rows
    * @param nc
    * number of columns
    */
   protected void pivot (double[] Mv, double[] qv, int r, int s, int nr, int nc) {
      // transform Mv and qv to reflect and exchange of variables
      // w(r) with z(s)

      double q_r = qv[r];
      double m_rs = Mv[r * nc + s];

      // update qv
      for (int i = 0; i < nr; i++) {
         if (i == r) {
            qv[i] = -q_r / m_rs;
         }
         else {
            qv[i] = qv[i] - q_r * (Mv[i * nc + s] / m_rs);
         }
      }

      // update Mv(i,j) for i != r and j != s
      for (int i = 0; i < nr; i++) {
         if (i != r) {
            double m_is = Mv[i * nc + s];
            for (int j = 0; j < nc; j++) {
               if (j != s) {
                  Mv[i * nc + j] -= (m_is / m_rs) * Mv[r * nc + j];
               }
            }
         }
      }
      // update Mv(r,j)
      for (int j = 0; j < nc; j++) {
         if (j != s) {
            Mv[r * nc + j] /= -m_rs;
         }
      }
      // update Mv(i,s)
      for (int i = 0; i < nr; i++) {
         if (i != r) {
            Mv[i * nc + s] /= m_rs;
         }
      }
      Mv[r * nc + s] = 1 / m_rs;
   }

}
