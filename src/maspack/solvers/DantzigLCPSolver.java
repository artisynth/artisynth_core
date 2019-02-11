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
 * semi-definite (SPSD) matrices using Dantzig's method. An LCP is defined by
 * the linear system
 * 
 * <pre>
 * {@code
 * w = M z + q
 * }
 * </pre>
 * 
 * and solving it entails finding w and z subject to the constraints
 * 
 * <pre>
 * {@code
 *                  T
 * w >= 0, z >= 0, w z = 0
 * }
 * </pre>
 * 
 * Dantig's method does this by a series of <i>pivoting</i> operations. Each
 * pivot corresponds to one solver iteration and entails the exchange of a
 * single variable w_i with its complementary counterpart z_i. A sequence of
 * pivots results in the pivoted system
 * 
 * <pre>
 * {@code
 * w' = M' z' + q'
 * }
 * </pre>
 * 
 * where w' and z' contain complementary combinations of the w and z variables.
 * Any variable (w or z) contained in w' is called a <i>basic</i> variable.
 * When a pivoted system is found for which q' {@code >=} 0, this provides a
 * solution to the LCP in which the z and w variables comprising z' are 0 and
 * the z and w variables comprising w' are equal to the corresponding entries
 * in q'. As mentioned above, Dantzig's method only works when M is SPSD.
 * 
 * <p>
 * Full details on the solution of LCPs can be found in <i>The Linear
 * Complementarity Problem</i>, by Cottle, Pang, and Stone.
 */
public class DantzigLCPSolver {
   protected double[] myMvBuf;
   protected double[] myQvBuf;
   protected double[] myMcolBuf;
   protected double[] myZBuf;
   protected double[] myWBuf;

   protected double[] myLo;
   protected double[] myHi;

   protected boolean[] myZBasic;
   protected boolean[] myPivotOK;
   protected int mySize;
   protected int myCapacity;

   protected int[] myState;
   protected int[] myLocalStateBuf;

   protected static final boolean myIncrementalSolve = true;
   protected CholeskyDecomposition myCholesky;
   protected VectorNd myX;
   protected int[] myPivotedToInitialIdxs;
   // protected int[] myInitialToPivotedIdxs;
   protected int myNumZBasic;
   protected MatrixNd myM;
   protected VectorNd myQ;

   protected double myDefaultTol = 1e-12;
   protected double myTol;
   protected int myIterationLimit = 10;
   protected int myIterationCnt;
   protected boolean myComputeResidual = false;
   protected double myResidual = 0;
   protected boolean mySilentP = false;

   public static final int SHOW_NONE = 0x00;
   public static final int SHOW_PIVOTS = 0x01;
   public static final int SHOW_MIN_RATIO = 0x02;
   public static final int SHOW_QM = 0x04;
   public static final int SHOW_VARIABLES = 0x08;
   public static final int SHOW_RETURNS = 0x10;
   public static final int SHOW_ALL = (SHOW_PIVOTS | SHOW_MIN_RATIO | SHOW_QM);

   public static int Z_VAR = 0x01;
   public static int W_VAR_LOWER = 0x02;
   public static int W_VAR_UPPER = 0x06;

   protected int myDebug = SHOW_NONE; // 
   // protected int myDebug = SHOW_PIVOTS | SHOW_VARIABLES | SHOW_RETURNS;
   protected FunctionTimer timerA = new FunctionTimer();
   protected FunctionTimer timerB = new FunctionTimer();

   private static double inf = Double.POSITIVE_INFINITY;

   /**
    * Described whether or not a solution was found.
    */
   public enum Status {
      /**
       * A solution was found.
       */
      SOLVED,
      /**
       * No solution appears to be possible.
       */
      NO_SOLUTION,
      /**
       * Iteration limit was exceeded, most likely due to numerical
       * ill-conditioning.
       */
      ITERATION_LIMIT_EXCEEDED,
      /**
       * A numeric error was detected in the solution.
       */
      NUMERIC_ERROR,
   }

   public int getDebug() {
      return myDebug;
   }

   public void setDebug (int code) {
      myDebug = code;
   }

   public boolean getSilent() {
      return mySilentP;
   }

   public void setSilent (boolean code) {
      mySilentP = code;
   }

   public boolean getComputeResidual() {
      return myComputeResidual;
   }

   public void setComputeResidual (boolean enable) {
      myComputeResidual = enable;
   }

   public double getResidual() {
      return myResidual;
   }

   /**
    * Creates a new Dantzig solver.
    */
   public DantzigLCPSolver() {
      myMvBuf = new double[0];
      myQvBuf = new double[0];
      myMcolBuf = new double[0];
      myZBasic = new boolean[0];
      myLocalStateBuf = new int[0];
      myPivotOK = new boolean[0];
      mySize = 0;

      myCholesky = new CholeskyDecomposition();
      myPivotedToInitialIdxs = new int[0];
      // myInitialToPivotedIdxs = new int[0];
      myX = new VectorNd (0);
   }

   /**
    * Returns the numeric tolerence for this solver.
    * 
    * @return numeric tolerance
    * @see #setTolerance
    */
   public double getTolerance() {
      return myDefaultTol;
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
      myDefaultTol = Math.max (tol, 0);
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
    * because the expected number of iterations for Dantzig's algorithm is
    * proportional to the size of the LCP matrix.
    * 
    * @param limit
    * new iteration limit
    */
   public void setIterationLimit (int limit) {
      myIterationLimit = limit;
   }

   /**
    * Returns the number of iterations, or pivots, that were used in the most
    * recent solution operation.
    * 
    * @return iteration count for last solution
    */
   public int getIterationCount() {
      return myIterationCnt;
   }

   /**
    * Returns the current pivoted value of q in an array of doubles. Subclasses
    * can override this method if they have a particularly efficient way of
    * determining q.
    * 
    * @return current pivoted value of q.
    */
   protected double[] getQv() {
      return myQvBuf;
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
      if (myIncrementalSolve) {
         // timerA.restart();

         double[] xbuf = myX.getBuffer();
         double[] Mbuf = myM.getBuffer();
         int mw = myM.getBufferWidth();

         for (int ip = 0; ip < myNumZBasic; ip++) {
            xbuf[ip] = -Mbuf[mw * myPivotedToInitialIdxs[ip] + r];
         }
         if (myNumZBasic > 0) {
            myCholesky.solve (myX, myX);
         }

         for (int ip = 0; ip < myNumZBasic; ip++) {
            myMcolBuf[myPivotedToInitialIdxs[ip]] = xbuf[ip];
         }

         int ip = myNumZBasic;
         while (ip < mySize - 5) {
            int i0 = myPivotedToInitialIdxs[ip++];
            int i1 = myPivotedToInitialIdxs[ip++];
            int i2 = myPivotedToInitialIdxs[ip++];
            int i3 = myPivotedToInitialIdxs[ip++];
            int i4 = myPivotedToInitialIdxs[ip++];
            int i5 = myPivotedToInitialIdxs[ip++];
            double sum0 = Mbuf[i0 * mw + r];
            double sum1 = Mbuf[i1 * mw + r];
            double sum2 = Mbuf[i2 * mw + r];
            double sum3 = Mbuf[i3 * mw + r];
            double sum4 = Mbuf[i4 * mw + r];
            double sum5 = Mbuf[i5 * mw + r];
            for (int jp = 0; jp < myNumZBasic; jp++) {
               int j = myPivotedToInitialIdxs[jp];
               double x = xbuf[jp];
               sum0 += Mbuf[i0 * mw + j] * x;
               sum1 += Mbuf[i1 * mw + j] * x;
               sum2 += Mbuf[i2 * mw + j] * x;
               sum3 += Mbuf[i3 * mw + j] * x;
               sum4 += Mbuf[i4 * mw + j] * x;
               sum5 += Mbuf[i5 * mw + j] * x;
            }
            myMcolBuf[i0] = sum0;
            myMcolBuf[i1] = sum1;
            myMcolBuf[i2] = sum2;
            myMcolBuf[i3] = sum3;
            myMcolBuf[i4] = sum4;
            myMcolBuf[i5] = sum5;
         }
         while (ip < mySize) {
            int i = myPivotedToInitialIdxs[ip++];
            double sum = Mbuf[i * mw + r];
            for (int jp = 0; jp < myNumZBasic; jp++) {
               sum += Mbuf[i * mw + myPivotedToInitialIdxs[jp]] * xbuf[jp];
            }
            myMcolBuf[i] = sum;
         }
         // timerB.stop();
      }
      else {
         for (int i = 0; i < mySize; i++) {
            myMcolBuf[i] = myMvBuf[i * mySize + r];
         }
      }
      return myMcolBuf;
   }

   protected boolean principalPivot (int s) {
      pivot (myMvBuf, myQvBuf, s, s, mySize, mySize);
      if (myState[s] == Z_VAR) {
         myZBuf[s] = 0;
         myState[s] = W_VAR_LOWER;
      }
      else {
         myWBuf[s] = 0;
         myState[s] = Z_VAR;
      }
      for (int i = 0; i < mySize; i++) {
         if (myState[i] == Z_VAR) {
            myZBuf[i] = myQvBuf[i];
            myWBuf[i] = 0;
         }
         else {
            myWBuf[i] = myQvBuf[i];
            myZBuf[i] = 0;
         }
      }
      return true;
   }

   protected void copyBooleanArray (boolean[] dest, boolean[] src, int n) {
      for (int i = 0; i < n; i++) {
         dest[i] = src[i];
      }
   }

   protected void setProblemSize (int size) {
      if (size > myCapacity) {
         myMvBuf = new double[size * size];
         myQvBuf = new double[size];
         myMcolBuf = new double[size];
         myZBasic = new boolean[size];

         // myInitialToPivotedIdxs = new int[size];
         myPivotedToInitialIdxs = new int[size];

         myCholesky.setCapacity (size);
         myCapacity = size;
      }
      mySize = size;
      myX.setSize (size);
   }

   /**
    * Solves the LCP
    * 
    * <pre>
    * w = M z + q
    * </pre>
    * 
    * where M is SPSD. It is possible to use this routine to solve a mixed LCP,
    * by pre-pivoting M and q to make the relevant non-LCP z variables basic and
    * presetting the corresponding entries for these variables in zBasic to
    * true.
    * 
    * @param z
    * returns the solution for z
    * @param M
    * system matrix
    * @param q
    * system vector
    * @param zBasic
    * On output, identifies which z variables are basic in the solution. On
    * input, identifies z variables which have been made basic as part of
    * solving a mixed LCP. If the LCP is not mixed, then all entries in this
    * array should be set to false.
    * @return Status of the solution.
    */
   public Status solve (VectorNd z, MatrixNd M, VectorNd q, boolean[] zBasic) {
      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException ("Matrix is not square");
      }
      int size = z.size();
      if (M.rowSize() != size) {
         throw new IllegalArgumentException (
            "z and M do not have the same size");
      }
      if (q.size() != size) {
         throw new IllegalArgumentException (
            "z and q do not have the same sizes");
      }
      if (zBasic != null && zBasic.length < size) {
         throw new IllegalArgumentException ("zBasic has size less than z");
      }
      // allocate storage space
      myZBuf = z.getBuffer();
      setProblemSize (size);
      myWBuf = new double[size];
      myCholesky.clear();
      myM = M;
      myQ = q;
      M.get (myMvBuf);
      q.get (myQvBuf);
      if (myLocalStateBuf.length < mySize) {
         myLocalStateBuf = new int[mySize];
      }
      myState = myLocalStateBuf;
      for (int i = 0; i < size; i++) {
         myState[i] = W_VAR_LOWER;
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
      Status status = dosolve (size);
      // z.set (myZBuf);
      if (zBasic != null) {
         for (int i = 0; i < size; i++) {
            zBasic[i] = (myState[i] == Z_VAR);
         }
      }
      if (myComputeResidual) {
         myResidual = computeResidual (z, M, q);
      }
      return status;
   }

   protected double computeResidual (VectorNd z, MatrixNd M, VectorNd q) {
      VectorNd w = new VectorNd (z.size());
      M.mul (w, z);
      w.add (q);
      double res = 0;
      for (int i=0; i<M.rowSize(); i++) {
         double wv = w.get(i);
         double zv = z.get(i);
         if (wv < 0) {
            res = Math.max (res, -wv);
         }
         if (zv < 0) {
            res = Math.max (res, -zv);            
         }
         res = Math.max (res, Math.abs(wv*zv));
      }
      return res;
   }

   /**
    * Solves the system
    * 
    * <pre>
    * w = M z + q
    * </pre>
    * 
    * using the same matrix M and active set that was determined in the previous
    * call to solve. Non-active z variables are set to 0.
    * 
    * @param z
    * returns the solution for z
    * @param q
    * system vector
    * @param zBasic
    * (optional) if specified, returns which z variables are basic in the
    * solution.
    */
   public void resolve (VectorNd z, VectorNd q, boolean[] zBasic) {
      if (q.size() != mySize) {
         throw new IllegalArgumentException (
            "q does not have the size of the currently loaded M");
      }
      if (z.size() != mySize) {
         throw new IllegalArgumentException (
            "q does not have the size of the currently loaded M");
      }
      if (zBasic != null && zBasic.length < mySize) {
         throw new IllegalArgumentException ("zBasic size " + zBasic.length
         + " less than current problem size " + mySize);
      }
      // System.out.println ("M=\n" + M.toString("%12.8f"));
      // System.out.println ("q=" + q.toString("%12.8f"));

      double[] xbuf = myX.getBuffer();
      double[] qbuf = q.getBuffer();
      double[] zbuf = z.getBuffer();

      for (int ip = 0; ip < myNumZBasic; ip++) {
         xbuf[ip] = -qbuf[myPivotedToInitialIdxs[ip]];
      }
      if (myNumZBasic > 0) {
         myCholesky.solve (myX, myX);
      }
      // set all z[i] to 0; overwrite the active ones in the next loop
      for (int i = 0; i < mySize; i++) {
         zbuf[i] = 0;
      }
      for (int ip = 0; ip < myNumZBasic; ip++) {
         zbuf[myPivotedToInitialIdxs[ip]] = xbuf[ip];
      }
      if (zBasic != null) {
         for (int i = 0; i < mySize; i++) {
            zBasic[i] = (myState[i] == Z_VAR);
         }
      }
   }

   public Status solve (
      VectorNd z, VectorNd w, MatrixNd M, VectorNd q, VectorNd lo, VectorNd hi,
      int nub, int[] state) {
      if (M.rowSize() != M.colSize()) {
         throw new IllegalArgumentException ("Matrix is not square");
      }
      int size = z.size();
      if (M.rowSize() != size) {
         throw new IllegalArgumentException (
            "z and M do not have the same size");
      }
      if (q.size() != size) {
         throw new IllegalArgumentException (
            "z and q do not have the same sizes");
      }
      if (state != null) {
         if (state.length < size) {
            throw new IllegalArgumentException (
               "state array insufficiently large");
         }
         myState = state;
      }
      else {
         if (myLocalStateBuf.length < mySize) {
            myLocalStateBuf = new int[mySize];
         }
         myState = myLocalStateBuf;
      }

      // allocate storage space
      myZBuf = z.getBuffer();
      myWBuf = w.getBuffer();
      setProblemSize (size);
      myCholesky.clear();
      myM = M;
      myQ = q;
      myLo = lo.getBuffer();
      myHi = hi.getBuffer();
      M.get (myMvBuf);
      for (int i = 0; i < size; i++) {
         myState[i] = W_VAR_LOWER;
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
      Status status = dosolveBLCP (size, nub);
      return status;
   }

   private void printVec (String msg, double[] buf, int n) {
      VectorNd v = new VectorNd (n);
      v.set (buf);
      System.out.println (msg + " " + v.toString ("%g"));
   }

   protected int minRatioTest (double[] mv, int r, int n) {
      double minStep = 0;
      double rstep = 0;
      int s = -1;

      if ((myDebug & SHOW_MIN_RATIO) != 0) {
         System.out.println ("minRatioTest, r=" + r + ", tol=" + myTol + ":");
      }
      for (int i = 0; i < n; i++) {
         if (myPivotOK[i]) {
            if (i == r || myState[i] == Z_VAR) // && !zBasicOrig[i]))
            {
               double step = Double.POSITIVE_INFINITY;
               if (i == r) {
                  if (mv[i] > myTol) {
                     step = -myWBuf[i] / mv[i];
                     if ((myDebug & SHOW_MIN_RATIO) != 0) {
                        System.out.println ("step" + i + " = " + (-myWBuf[i])
                        + "/" + mv[i] + " = " + step);
                     }
                  }
                  rstep = step;
               }
               else // i is a z variable
               {
                  if (mv[i] < -myTol) {
                     step = -myZBuf[i] / mv[i];
                     if ((myDebug & SHOW_MIN_RATIO) != 0) {
                        System.out.println ("step=" + i + " = " + (-myZBuf[i])
                        + "/" + mv[i] + " = " + step);
                     }
                  }
               }
               if (s == -1 || step < minStep) {
                  s = i;
                  minStep = step;
               }
            }
         }
      }
      if (minStep == rstep) {
         if (myPivotOK[r]) {
            s = r;
         }
         else {
            s = -1; // no pivots possible
         }
      }
      return s;
   }

   protected Status dosolve (int n) {
      int maxIterations = myIterationLimit * n;
      myIterationCnt = 0;

      if (myPivotOK.length < mySize) {
         myPivotOK = new boolean[mySize];
      }

      double[] mv;

      myTol = myDefaultTol;

      boolean mixed = false;

      double[] qbuf = myQ.getBuffer();
      for (int i = 0; i < n; i++) {
         myPivotOK[i] = true;
         myWBuf[i] = qbuf[i];
         myZBuf[i] = 0;
      }
      while (myIterationCnt < maxIterations) {
         int r = -1;

         // printVec ("Q ", qv, n);
         double minVal = 0;
         for (int i = 0; i < n; i++) {
            if (myState[i] != Z_VAR && myPivotOK[i]) {
               if (r == -1 || myWBuf[i] < minVal) {
                  r = i;
                  minVal = myWBuf[i];
               }
            }
         }

         if (minVal >= -myTol) { // solution found
            double minq = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
               if (myZBuf[i] < minq) {
                  minq = myZBuf[i];
               }
            }
            if (minq < -myTol) {
               System.out.println ("ERROR: min Q=" + minq);
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NUMERIC_ERROR 1");
               }
               return Status.NUMERIC_ERROR;
            }
            else {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("SOLVED");
               }
               return Status.SOLVED;
            }
         }
         if (r == -1) {
            throw new InternalErrorException (
               "r not initialized; n=" + n);
         }
         // r has now been changed, so get mv_r
         mv = getMv (r);
         // printVec ("R ", mv, n);

         if (Math.abs (mv[r]) < myTol) {
            int i;
            for (i = 0; i < n; i++) {
               if (mv[i] < -myTol) {
                  break;
               }
            }
            if (i == n) { // no solution
            // System.out.println ("NO SOLUTION");
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NO SOLUTION");
               }
               return Status.NO_SOLUTION;
            }
         }

         int s;
         do {
            // minimum ratio test to determine blocking variable

            s = minRatioTest (mv, r, n);
            // System.out.println (
            // "Pivoting " + s + " to " + (myZBasic[s] ? "W" : "Z"));
            if (s == -1) {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NUMERIC ERROR 2");
               }
            }
            boolean pivotOK;
            if ((myDebug & SHOW_VARIABLES) != 0) {
               printVariables (s, r, mv);
            }

            if (myIncrementalSolve) {
               pivotOK = updateZBasis (s);

               if (pivotOK) {
                  if (myState[s] == Z_VAR) {
                     updateVariables (s, r, mv, -myZBuf[s] / mv[s]);
                     myZBuf[s] = 0;
                     myState[s] = W_VAR_LOWER;
                  }
                  else {
                     updateVariables (s, r, mv, -myWBuf[s] / mv[s]);
                     myWBuf[s] = 0;
                     myState[s] = Z_VAR;
                  }
               }
            }
            else {
               pivotOK = principalPivot (s);
            }

            if (!pivotOK) {
               myPivotOK[s] = false;
               myTol = 2 * Math.abs (mv[s]);
            }
            else {
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  if (s != r) {
                     System.out.println ("pivot " + s + ", drive=" + r);
                  }
                  else {
                     System.out.println ("pivot " + s);
                  }
               }
               for (int i = 0; i < n; i++) {
                  myPivotOK[i] = true;
               }
               myIterationCnt++;

               if (s != r) {
                  mv = getMv (r);
               }
            }
         }
         while (s != r);
      }
      if ((myDebug & SHOW_RETURNS) != 0) {
         System.out.println ("ITERATION LIMIT EXCEEDED");
      }
      return Status.ITERATION_LIMIT_EXCEEDED;
   }

   private void printVariables (int s, int r, double[] mv) {
      NumberFormat ffmt = new NumberFormat ("%13.8f");
      NumberFormat rfmt = new NumberFormat ("%7.2f");
      NumberFormat dfmt = new NumberFormat ("%2d");
      System.out.print ("           Z             W        ");
      // XX X X XXXX.XXXXXXXX XXXX.XXXXXXXX
      if (myLo != null && myHi != null) {
         System.out.print ("    lo      hi  ");
         // XXXX.XX XXXX.XX
      }
      if (mv != null) {
         System.out.println ("     mv");
         // XXXX.XXXXXXXX
      }
      else {
         System.out.println ("");
      }
      for (int i = 0; i < mySize; i++) {
         String anotation = "";
         if (myState[i] == Z_VAR) {
            anotation = " z ";
         }
         else if (myState[i] == W_VAR_LOWER) {
            anotation = "   ";
         }
         else {
            anotation = " u ";
         }
         if (i == r && r == s) {
            anotation += "R ";
         }
         else if (i == r) {
            anotation += "r ";
         }
         else if (i == s) {
            anotation += "s ";
         }
         else {
            anotation += "  ";
         }
         System.out.print (dfmt.format(i) + anotation
         + ffmt.format (myZBuf[i]) + " " + ffmt.format (myWBuf[i]));
         if (myLo != null && myHi != null) {
            System.out.print (" " + rfmt.format (myLo[i]) + " "
            + rfmt.format (myHi[i]));
         }
         if (mv != null) {
            System.out.println (" " + ffmt.format (mv[i]));
         }
         else {
            System.out.println ("");
         }
      }
   }

   private void checkWValues() {
      double[] wcheck = new double[mySize];
      double[] Mbuf = myM.getBuffer();
      int mw = myM.getBufferWidth();

      for (int i = 0; i < mySize; i++) {
         wcheck[i] = myQ.get(i);
      }
      for (int j = 0; j < mySize; j++) {
         double z = myZBuf[j];
         if (z != 0) {
            for (int i = 0; i < mySize; i++) {
               wcheck[i] += Mbuf[i * mw + j] * z;
            }
         }
      }
      for (int i = 0; i < mySize; i++) {
         VectorNd zvec = new VectorNd (mySize, myZBuf);
         VectorNd wvec = new VectorNd (mySize, myWBuf);
         if (Math.abs (wcheck[i] - myWBuf[i]) > 1e-8) {
            System.out.println ("M=[\n" + myM.toString ("%g") + "]");
            System.out.println ("q=[" + myQ.toString ("%g") + "]");
            System.out.println ("z=[" + zvec.toString ("%g") + "]");
            System.out.println ("w=[" + wvec.toString ("%g") + "]");
            System.out.println ("lo=[" + (new VectorNd (myLo)).toString ("%g")
            + "]");
            System.out.println ("hi=[" + (new VectorNd (myHi)).toString ("%g")
            + "]");
            throw new InternalErrorException ("w[" + i + "]=" + myWBuf[i]
            + ", expected " + wcheck[i]);
         }
      }
   }

   protected Status dosolveBLCP (int n, int nub) {
      int maxIterations = myIterationLimit * n;
      myIterationCnt = 0;

      if (myPivotOK.length < mySize) {
         myPivotOK = new boolean[mySize];
      }

      double[] Mbuf = myM.getBuffer();
      double[] xbuf = myX.getBuffer();
      double[] qbuf = myQ.getBuffer();
      int mw = myM.getBufferWidth();

      for (int j = 0; j < nub; j++) {
         myX.setSize (j + 1);
         for (int i = 0; i <= j; i++) {
            xbuf[i] = Mbuf[i * mw + j];
         }
         if (myLo[j] != Double.NEGATIVE_INFINITY ||
             myHi[j] != Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException ("unbounded variable " + j
            + " must have lo/hi settings -inf/+inf");
         }
         if (!myCholesky.addRowAndColumn (myX, 0)) {
            throw new IllegalArgumentException (
               "unbounded variable basis is not positive definite");
         }
         myState[j] = Z_VAR;
         myWBuf[j] = 0;
      }
      myNumZBasic = nub;
      for (int j = nub; j < mySize; j++) {
         if (myLo[j] == Double.NEGATIVE_INFINITY &&
             myHi[j] == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException (
               "unbounded variables detected outside range indicated by nub");
         }
         if (myLo[j] == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException ("lo[" + j
            + "] set to +infinity");
         }
         if (myHi[j] == Double.NEGATIVE_INFINITY) {
            throw new IllegalArgumentException ("hi[" + j
            + "] set to -infinity");
         }
         if (myLo[j] == Double.NEGATIVE_INFINITY) {
            myState[j] = W_VAR_UPPER;
            myZBuf[j] = myHi[j];
         }
         else {
            myState[j] = W_VAR_LOWER;
            myZBuf[j] = myLo[j];
         }
      }
      if (nub > 0) {
         myX.setSize (nub);
         for (int i = 0; i < nub; i++) {
            xbuf[i] = qbuf[i];
         }
         for (int j = nub; j < mySize; j++) {
            double z = myZBuf[j];
            if (z != 0) {
               for (int i = 0; i < nub; i++) {
                  xbuf[i] += Mbuf[i * mw + j] * z;
               }
            }
         }
         myX.negate();
         myCholesky.solve (myX, myX);
         for (int i = 0; i < nub; i++) {
            myZBuf[i] = xbuf[i];
         }
      }
      for (int i = nub; i < mySize; i++) {
         myWBuf[i] = qbuf[i];
      }
      for (int j = 0; j < mySize; j++) {
         double z = myZBuf[j];
         if (z != 0) {
            for (int i = nub; i < mySize; i++) {
               myWBuf[i] += Mbuf[i * mw + j] * z;
            }
         }
      }
      checkWValues();
      myTol = myDefaultTol;

      for (int i = 0; i < n; i++) {
         myPivotOK[i] = true;
      }
      while (myIterationCnt < maxIterations) {

         int rl = -1;
         int ru = -1;

         double minl = Double.POSITIVE_INFINITY;
         double maxu = Double.NEGATIVE_INFINITY;

         boolean wWithinBounds = true;
         for (int i = 0; i < mySize; i++) {
            if (myState[i] == W_VAR_LOWER) {
               double w = myWBuf[i];
               if (w < -myTol) {
                  if (myPivotOK[i] && w < minl) {
                     rl = i;
                     minl = w;
                     wWithinBounds = false;
                  }
               }
            }
            else if (myState[i] == W_VAR_UPPER) {
               double w = myWBuf[i];
               if (w > myTol) {
                  if (myPivotOK[i] && w > maxu) {
                     ru = i;
                     maxu = w;
                     wWithinBounds = false;
                  }
               }
            }
         }

         if (wWithinBounds) { // solution found
            if ((myDebug & SHOW_VARIABLES) != 0) {
               printVariables (-1, -1, null);
            }
            if ((myDebug & SHOW_RETURNS) != 0) {
               System.out.println ("SOLVED");
            }
            return Status.SOLVED;
         }
         else if (rl == -1 && ru == -1) {
            if ((myDebug & SHOW_RETURNS) != 0) {
               System.out.println ("NUMERIC ERROR");
            }
            return Status.NUMERIC_ERROR;
         }

         int r = -1;

         if (rl != -1) {
            r = rl;
         }
         if (ru != -1) {
            if (r == -1 || (r == rl && -maxu < minl)) {
               r = ru;
            }
         }
         int p = (myWBuf[r] < 0 ? 1 : -1);

         double theta, theta0, theta1, theta2, theta3;
         theta = 0;
         theta0 = 0;

         double[] mv = getMv (r);

         do {
            double mrr = mv[r];

            int s2 = -1;
            int s3 = -1;
            int s;

            if (p == 1) {
               theta0 = Double.POSITIVE_INFINITY;
               theta1 = Double.POSITIVE_INFINITY;
               theta2 = Double.POSITIVE_INFINITY;
               theta3 = Double.POSITIVE_INFINITY;

               if (mrr > myTol) {
                  theta0 = -myWBuf[r] / mrr;
               }
               theta1 = myHi[r] - myZBuf[r];

               for (int i = 0; i < mySize; i++) {
                  if (myPivotOK[i] && myState[i] == Z_VAR) {
                     double mi = mv[i];
                     if (mi < -myTol) {
                        double x = (myLo[i] - myZBuf[i]) / mi;
                        if (x < theta2) {
                           theta2 = x;
                           s2 = i;
                        }
                     }
                     else if (mi > myTol) {
                        double x = (myHi[i] - myZBuf[i]) / mi;
                        if (x < theta3) {
                           theta3 = x;
                           s3 = i;
                        }
                     }
                  }
               }
               s = r;
               theta = Math.min (theta0, theta1);
               if (theta2 < theta) {
                  theta = theta2;
                  s = s2;
               }
               if (theta3 < theta) {
                  theta = theta3;
                  s = s3;
               }
            }
            else {
               theta0 = -inf;
               theta1 = -inf;
               theta2 = -inf;
               theta3 = -inf;

               if (mrr > myTol) {
                  theta0 = -myWBuf[r] / mrr;
               }
               theta1 = myLo[r] - myZBuf[r];

               for (int i = 0; i < mySize; i++) {
                  if (myPivotOK[i] && myState[i] == Z_VAR) {
                     double mi = mv[i];
                     if (mi > myTol) {
                        double x = (myLo[i] - myZBuf[i]) / mi;
                        if (x > theta2) {
                           theta2 = x;
                           s2 = i;
                        }
                     }
                     else if (mi < -myTol) {
                        double x = (myHi[i] - myZBuf[i]) / mi;
                        if (x > theta3) {
                           theta3 = x;
                           s3 = i;
                        }
                     }
                  }
               }
               s = r;
               theta = Math.max (theta0, theta1);
               if (theta2 > theta) {
                  theta = theta2;
                  s = s2;
               }
               if (theta3 > theta) {
                  theta = theta3;
                  s = s3;
               }
            }

            if ((myDebug & SHOW_VARIABLES) != 0) {
               printVariables (s, r, mv);
               System.out.println ("p=" + p);
               System.out.println ("theta=" + theta);
               System.out.println ("theta0=" + theta0);
               System.out.println ("theta1=" + theta1);
               System.out.println ("theta2=" + theta2);
               System.out.println ("theta3=" + theta3);
            }

            if (Math.abs (theta) == Double.POSITIVE_INFINITY && theta != theta0) {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NO SOLUTION");
               }
               return Status.NO_SOLUTION;
            }
            boolean pivotOK = true;
            if (theta != theta1 || Math.abs (theta1) == inf) {
               pivotOK = updateZBasis (s);
               if (pivotOK) {
                  updateVariables (s, r, mv, theta);
               }
            }
            else {
               updateVariables (s, r, mv, theta);
            }
            if (pivotOK) {
               if (theta == theta1) {
                  if (p == 1) {
                     myZBuf[r] = myHi[r];
                     myState[r] = W_VAR_UPPER;
                     // System.out.println ("1 "+r+" <- W_UPPER");
                  }
                  else {
                     myZBuf[r] = myLo[r];
                     myState[r] = W_VAR_LOWER;
                     // System.out.println ("1 "+r+" <- W_LOWER");
                  }
               }
               else if (theta == theta0) {
                  myWBuf[r] = 0;
                  myState[r] = Z_VAR;
                  // System.out.println ("0 "+r+" <- Z");
               }
               else if (theta == theta2) {
                  myZBuf[s2] = myLo[s2];
                  myState[s2] = W_VAR_LOWER;
                  // System.out.println ("2 "+s2+" <- W_LOWER");
               }
               else if (theta == theta3) {
                  myZBuf[s3] = myHi[s3];
                  myState[s3] = W_VAR_UPPER;
                  // System.out.println ("3 "+s3+" <- W_UPPER");
               }
               for (int i = 0; i < n; i++) {
                  myPivotOK[i] = true;
               }
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  if (theta == theta1) {
                     System.out.println ("no pivot");
                  }
                  else if (s != r) {
                     System.out.println ("pivot " + s + ", drive=" + r);
                  }
                  else {
                     System.out.println ("pivot " + s);
                  }
               }
               if (s != r) {
                  mv = getMv (r);
               }
               checkWValues();
               myIterationCnt++;
            }
            else {
               myPivotOK[s] = false;
               myTol = 2 * Math.abs (mv[s]);
               if (!mySilentP) {
                  System.out.println (
                     "pivot " + s + " REJECTED, r=" + r + " tol=" + myTol);
               }
            }
         }
         while (theta != theta0 && theta != theta1);
      }
      if ((myDebug & SHOW_RETURNS) != 0) {
         System.out.println ("ITERATION LIMIT EXCEEDED");
      }
      return Status.ITERATION_LIMIT_EXCEEDED;
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
      // timerA.restart();

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
      // timerA.stop();
   }

   protected boolean updateZBasis (int s) {
      double[] xbuf = myX.getBuffer();
      double[] qbuf = myQ.getBuffer();
      double[] Mbuf = myM.getBuffer();
      int mw = myM.getBufferWidth();

      boolean addToZBasis = (myState[s] != Z_VAR);

      // timerA.restart();
      if (addToZBasis) {
         for (int ip = 0; ip < myNumZBasic; ip++) {
            xbuf[ip] = Mbuf[mw * myPivotedToInitialIdxs[ip] + s];
         }
         xbuf[myNumZBasic] = Mbuf[mw * s + s];
         myX.setSize (myNumZBasic + 1);
         if (!myCholesky.addRowAndColumn (myX, 0)) {
            myX.setSize (myNumZBasic);
            return false;
         }
         int sp = -1;
         for (int ip = myNumZBasic; ip < mySize; ip++) {
            if (myPivotedToInitialIdxs[ip] == s) {
               sp = ip;
               break;
            }
         }
         myPivotedToInitialIdxs[sp] = myPivotedToInitialIdxs[myNumZBasic];
         myPivotedToInitialIdxs[myNumZBasic] = s;
         myNumZBasic++;
      }
      else {
         int sp = -1;
         for (int ip = 0; ip < myNumZBasic; ip++) {
            if (myPivotedToInitialIdxs[ip] == s) {
               sp = ip;
               break;
            }
         }
         myCholesky.deleteRowAndColumn (sp);
         for (int ip = sp + 1; ip < myNumZBasic; ip++) {
            myPivotedToInitialIdxs[ip - 1] = myPivotedToInitialIdxs[ip];
         }
         myPivotedToInitialIdxs[myNumZBasic - 1] = s;
         myNumZBasic--;
      }
      myX.setSize (myNumZBasic);
      return true;
   }

   protected void updateVariables (int s, int r, double[] mv, double step) {
      for (int i = 0; i < mySize; i++) {
         if (i == r) {
            myZBuf[i] += step;
            myWBuf[i] += step * mv[i];
         }
         else if (myState[i] == Z_VAR) {
            myZBuf[i] += step * mv[i];
         }
         else {
            myWBuf[i] += step * mv[i];
         }
      }
   }

   double getUsecA() {
      return timerA.getTimeUsec();
   }

   double getUsecB() {
      return timerB.getTimeUsec();
   }
}
