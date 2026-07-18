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
 * Solves linear complementarity problems (LCPs) and bounded linear
 * complementarity problems (BLCPs) for symmetric positive semi-definite (SPSD)
 * matrices using a principal pivoting method.
 *
 * <p>The algorithm implemented here is the <i>simple</i> principal pivoting
 * method attributed to Dantzig. At the start of each major cycle the most
 * infeasible variable (the one whose residual is most negative, or for a BLCP
 * most violates its bound) is selected as the driving variable; a
 * minimum-ratio test then determines a sequence of single (1x1) principal
 * pivots that drive it to feasibility while keeping all other variables
 * feasible. This corresponds to Algorithm 4.2.11 ("Dantzig; van de Panne and
 * Whinston") in <i>The Linear Complementarity Problem</i> by Cottle, Pang, and
 * Stone. This implementation generalizes the method to the bounded (BLCP)
 * case, and maintains an incrementally updated Cholesky factorization of the
 * basis matrix, which makes it considerably faster than a dense tableau
 * implementation such as {@link SimpleLCPSolver}.
 *
 * <p>Warm starting is supported and may be enabled with
 * {@link #setWarmStartEnabled}; when enabled, the {@code state} argument to the
 * {@code solve} methods is used to seed the initial basis, which can greatly
 * reduce the number of pivots when a sequence of related problems is solved
 * with a slowly changing active set.
 *
 * <p>Note that {@link SimpleLCPSolver} implements the <i>same</i> simple
 * principal pivoting method (for LCPs only), so for nondegenerate SPSD problems
 * the two classes produce the same sequence of pivots. They would differ only
 * at zero-diagonal degeneracies: robustly handling those requires the 2x2
 * block pivots and negative lower-bound device of Keller's general (symmetric)
 * principal pivoting method (Algorithm 4.3.2 in Cottle, Pang and Stone; E.
 * Keller, "The general quadratic optimization problem", Mathematical
 * Programming 5, 1973), <i>neither</i> of which is implemented here or in
 * {@link SimpleLCPSolver}.
 *
 * <p>The method is also described in Claude Lacoursiere's Ph.D. thesis,
 * <i>Ghosts and Machines: Regularized Variational Methods for Interactive
 * Simulations of Multibodies with Dry Frictional Contact</i> (2007). Be aware,
 * however, that Lacoursiere uses the name "Cottle-Dantzig" for an
 * <i>incremental</i> variant that introduces the variables one at a time in a
 * fixed index order; that is not the method implemented here (which, like
 * Algorithm 4.2.11 above, selects the most infeasible variable from the full
 * set at each cycle).
 */
public class DantzigLCPSolver implements LCPSolver {
   
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
   protected int myNumFailedPivots;
   protected int myIterationLimit = 10;
   protected int myIterationCnt;
   protected int myPivotCnt;
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

   protected int myDebug = SHOW_NONE; // 
   // protected int myDebug = SHOW_PIVOTS | SHOW_VARIABLES | SHOW_RETURNS;
   protected FunctionTimer timerA = new FunctionTimer();
   protected FunctionTimer timerB = new FunctionTimer();

   // when true, enables warm starting, assuming that state information for a
   // previous solve is supplied to the solve() methods.
   protected boolean myWarmStartEnabled = false;

   // internal variable indicating the warm starts should be used. If true,
   // dosolve()/dosolveBLCP() assume that the basis, along with the myZBuf and
   // myWBuf values, have been seeded by one of the seedBasis methods
   protected boolean myWarmStart = false;

   // tolerance used while warm starting. Because warm starting recomputes z and
   // w from a freshly factored principal submatrix, the resulting values may
   // differ from those of a cold (incremental) solve by an amount that exceeds
   // the default tolerance, particularly for ill-conditioned or rank-deficient
   // matrices. We therefore use a tolerance scaled by the problem magnitude
   // (as MurtyLCPSolver also does) so that a seeded solution is recognized
   // instead of being needlessly pivoted away from.
   protected double myWarmTol = 0;
   protected static double WARM_EPS = 1e-10;

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
      return myNumFailedPivots;
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

         myCholesky.ensureCapacity (size);
         myCapacity = size;
      }
      mySize = size;
      myX.setSize (size);
   }

   /**
    * {@inheritDoc}
    */
   public Status solve (VectorNd z, VectorNi state, MatrixNd M, VectorNd q) {
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
      if (state != null && state.size() < size) {
         throw new IllegalArgumentException ("state has size less than z");
      }
      // allocate storage space
      myZBuf = z.getBuffer();
      setProblemSize (size);
      myWBuf = new double[size];
      myM = M;
      myQ = q;
      M.get (myMvBuf);
      q.get (myQvBuf);

      // set internal warm start variable
      myWarmStart = (myWarmStartEnabled && state != null);

      if (myWarmStart) {
         seedBasisLCP (state);
      }
      else {
         seedCold (size);
      }
      Status status = dosolve (size);
      if (status != Status.SOLVED) {
         // warm start failed; try again with a cold solve
         myWarmStart = false;
         seedCold (size);
         status = dosolve (size);
      }
      if (state != null) {
         for (int i=0; i<size; i++) {
            state.set (i, myState[i]);
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
    * @param state
    * (optional) if specified, returns which z variables are basic in the
    * solution.
    */
   public void resolve (VectorNd z, VectorNd q, int[] state) {
      if (q.size() != mySize) {
         throw new IllegalArgumentException (
            "q does not have the size of the currently loaded M");
      }
      if (z.size() != mySize) {
         throw new IllegalArgumentException (
            "q does not have the size of the currently loaded M");
      }
      if (state != null && state.length < mySize) {
         throw new IllegalArgumentException ("state size " + state.length
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
      if (state != null) {
         for (int i = 0; i < mySize; i++) {
            state[i] = myState[i];
         }
      }
   }

   private void initializeState (int size) {
      if (myLocalStateBuf.length < mySize) {
         myLocalStateBuf = new int[mySize];
      }
      myState = myLocalStateBuf;     
      for (int i=0; i<size; i++) {
         myState[i] = W_VAR_LOWER;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Status solve (
      VectorNd z, VectorNd w, VectorNi state, MatrixNd M, VectorNd q, 
      VectorNd lo, VectorNd hi, int nub) {
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
      if (state != null && state.size() < size) {
         throw new IllegalArgumentException ("state has size less than z");
      }

      // allocate storage space
      myZBuf = z.getBuffer();
      myWBuf = w.getBuffer();
      setProblemSize (size);
      myM = M;
      myQ = q;
      myLo = lo.getBuffer();
      myHi = hi.getBuffer();
      M.get (myMvBuf);

      // set internal warm start variable
      myWarmStart = (myWarmStartEnabled && state != null);
      if (myWarmStart) {
         seedBasisBLCP (state, nub);
      }
      else {
         seedCold (size);
      }
      Status status = dosolveBLCP (size, nub);
      if (status != Status.SOLVED) {
         // warm start failed; try again with a cold solve
         myWarmStart = false;
         seedCold (size);
         status = dosolveBLCP (size, nub);
      }
      if (state != null) {
         for (int i=0; i<size; i++) {
            state.set (i, myState[i]);
         }
      }
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
            if (i == r || myState[i] == Z_VAR) 
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
      myPivotCnt = 0;

      if (myPivotOK.length < mySize) {
         myPivotOK = new boolean[mySize];
      }
      myNumFailedPivots = 0;

      double[] mv;

      myTol = (myWarmStart ? myWarmTol : myDefaultTol);

      double[] qbuf = myQ.getBuffer();
      if (!myWarmStart) {
         for (int i = 0; i < n; i++) {
            myPivotOK[i] = true;
            myWBuf[i] = qbuf[i];
            myZBuf[i] = 0;
         }
      }
      else {
         // z, w and the basis have already been seeded
         for (int i = 0; i < n; i++) {
            myPivotOK[i] = true;
         }
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
               if (false) {
                  System.out.printf (
                     "%s n=%d npivs=%d niters=%d\n", myWarmStart ? "warm":"cold",
                     n, myPivotCnt, myIterationCnt);
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

         if (Math.abs (mv[r]) < myTol) {
            int i;
            for (i = 0; i < n; i++) {
               if (mv[i] < -myTol) {
                  break;
               }
            }
            if (i == n) { // no solution
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
               myNumFailedPivots++;
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
               myPivotCnt++;
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
      myPivotCnt = 0;

      if (myPivotOK.length < mySize) {
         myPivotOK = new boolean[mySize];
      }
      myNumFailedPivots = 0;

      if (!myWarmStart) {
         double[] Mbuf = myM.getBuffer();
         double[] xbuf;
         double[] qbuf = myQ.getBuffer();
         int mw = myM.getBufferWidth();

         for (int j = 0; j < nub; j++) {
            myX.setSize (j + 1);
            xbuf = myX.getBuffer();
            for (int i = 0; i <= j; i++) {
               xbuf[i] = Mbuf[i * mw + j];
            }
            if (myLo[j] != Double.NEGATIVE_INFINITY ||
                myHi[j] != Double.POSITIVE_INFINITY) {
               throw new IllegalArgumentException (
                  "unbounded variable " + j +
                  " must have lo/hi settings -inf/+inf");
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
               throw new IllegalArgumentException (
                  "lo[" + j + "] set to +infinity");
            }
            if (myHi[j] == Double.NEGATIVE_INFINITY) {
               throw new IllegalArgumentException (
                  "hi[" + j  + "] set to -infinity");
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
            xbuf = myX.getBuffer();
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
      }
      
      myTol = (myWarmStart ? myWarmTol : myDefaultTol);

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
               theta0 = -INF;
               theta1 = -INF;
               theta2 = -INF;
               theta3 = -INF;

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
            if (theta != theta1 || Math.abs (theta1) == INF) {
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
                  }
                  else {
                     myZBuf[r] = myLo[r];
                     myState[r] = W_VAR_LOWER;
                  }
               }
               else if (theta == theta0) {
                  myWBuf[r] = 0;
                  myState[r] = Z_VAR;
               }
               else if (theta == theta2) {
                  myZBuf[s2] = myLo[s2];
                  myState[s2] = W_VAR_LOWER;
               }
               else if (theta == theta3) {
                  myZBuf[s3] = myHi[s3];
                  myState[s3] = W_VAR_UPPER;
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
               myIterationCnt++;
               myPivotCnt++;
            }
            else {
               myPivotOK[s] = false;
               myNumFailedPivots++;
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
    * {@inheritDoc}
    */
   public boolean isBLCPSupported() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */  
   public boolean isWarmStartSupported() {
      return myWarmStartEnabled;
   }

   /**
    * Enables or disables warm starts. When warm starts are enabled, the state
    * argument of the {@code solve()} methods, when non-{@code null}, is used
    * to supply an initial guess for the LCP solution.  Warm starts are
    * disabled by default. Enabling them causes {@link #isWarmStartSupported}
    * to return {@code true}.
    */
   public void setWarmStartEnabled (boolean enable) {
      myWarmStartEnabled = enable;
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
         myX.set (myNumZBasic, Mbuf[mw * s + s]);
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

   /**
    * Initialize the basis for a cold LCP solve.
    */
   protected void seedCold (int size) {
      myCholesky.clear();
      initializeState (size);
      for (int i = 0; i < size; i++) {
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
   }

   /**
    * Initialize the basis for a cold BLCP solve.
    */
   protected void seedColdLCP (int size) {
      myCholesky.clear();
      initializeState (size);
      for (int i = 0; i < size; i++) {
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
   }

   /**
    * Computes the tolerance to use while warm starting, scaled by the magnitude
    * of q but never smaller than the default tolerance.
    */
   protected double computeWarmTol () {
      double maxq = 0;
      double[] qbuf = myQ.getBuffer();
      for (int i = 0; i < mySize; i++) {
         double a = Math.abs (qbuf[i]);
         if (a > maxq) {
            maxq = a;
         }
      }
      return Math.max (myDefaultTol, WARM_EPS*maxq);
   }

   // ------------------------------------------------------------------
   // Warm-start basis seeding for plain LCPs
   // ------------------------------------------------------------------

   /**
    * Seeds the Z-basis for a plain LCP from the supplied state vector. On
    * return, myState, myPivotedToInitialIdxs, myNumZBasic and myCholesky reflect
    * a complementary basis whose basic variables are all feasible, and myZBuf
    * and myWBuf contain the corresponding z and w values.
    */
   protected void seedBasisLCP (VectorNi state) {
      int n = mySize;
      if (myLocalStateBuf.length < n) {
         myLocalStateBuf = new int[n];
      }
      myState = myLocalStateBuf;
      for (int i = 0; i < n; i++) {
         myState[i] = (state.get(i) == Z_VAR ? Z_VAR : W_VAR_LOWER);
      }
      rebuildBasisLCP();
   }

   /**
    * Rebuilds the Cholesky factor and pivot ordering for a plain LCP from the Z
    * entries of myState, processing variables in index order, then removes any
    * basic variables that are infeasible and recomputes the z and w values.
    * Because the basis is always rebuilt in the same canonical (index) order,
    * two solves that reach the same set of basic variables produce identical
    * results regardless of the pivot order used to get there.
    */
   protected void rebuildBasisLCP () {
      int n = mySize;
      myWarmTol = computeWarmTol();
      myTol = myWarmTol;
      // start with an empty basis and all variables non-basic at their lower
      // bound (z = 0)
      for (int i = 0; i < n; i++) {
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
      myCholesky.clear();
      // add the basic variables one at a time, dropping any that would make the
      // basis matrix non positive definite
      for (int i = 0; i < n; i++) {
         if (myState[i] == Z_VAR) {
            myState[i] = W_VAR_LOWER;
            if (updateZBasis (i)) {
               myState[i] = Z_VAR;
            }
            else {
               myZBuf[i] = 0;
            }
         }
      }
      // remove any basic variables that are infeasible (z < 0), so that the
      // starting point satisfies the precondition for the drive loop
      projectFeasibleLCP();
      // compute the w values consistent with the basis
      computeWLCP();
   }

   /**
    * Computes the basic z values for a plain LCP from the current basis,
    * storing them in myZBuf (non-basic z values are set to 0).
    */
   protected void computeBasicZLCP () {
      myX.setSize (myNumZBasic);
      double[] xbuf = myX.getBuffer();
      double[] qbuf = myQ.getBuffer();
      for (int ip = 0; ip < myNumZBasic; ip++) {
         xbuf[ip] = -qbuf[myPivotedToInitialIdxs[ip]];
      }
      if (myNumZBasic > 0) {
         myCholesky.solve (myX, myX);
      }
      for (int i = 0; i < mySize; i++) {
         myZBuf[i] = 0;
      }
      for (int ip = 0; ip < myNumZBasic; ip++) {
         myZBuf[myPivotedToInitialIdxs[ip]] = xbuf[ip];
      }
   }

   /**
    * Repeatedly removes the most infeasible (most negative) basic variable from
    * the basis until all basic z values are feasible. This always terminates
    * since the basis can only shrink.
    */
   protected void projectFeasibleLCP () {
      while (true) {
         computeBasicZLCP();
         int worst = -1;
         double minz = -myTol;
         for (int ip = 0; ip < myNumZBasic; ip++) {
            int i = myPivotedToInitialIdxs[ip];
            if (myZBuf[i] < minz) {
               minz = myZBuf[i];
               worst = i;
            }
         }
         if (worst == -1) {
            break;
         }
         updateZBasis (worst);
         myState[worst] = W_VAR_LOWER;
         myZBuf[worst] = 0;
      }
   }

   /**
    * Computes the w values for a plain LCP that are consistent with the current
    * basis and the current z values.
    */
   protected void computeWLCP () {
      double[] qbuf = myQ.getBuffer();
      double[] Mbuf = myM.getBuffer();
      int mw = myM.getBufferWidth();
      for (int i = 0; i < mySize; i++) {
         if (myState[i] == Z_VAR) {
            myWBuf[i] = 0;
         }
         else {
            double w = qbuf[i];
            for (int ip = 0; ip < myNumZBasic; ip++) {
               int j = myPivotedToInitialIdxs[ip];
               w += Mbuf[i * mw + j] * myZBuf[j];
            }
            myWBuf[i] = w;
         }
      }
   }

   // ------------------------------------------------------------------
   // Warm-start basis seeding for BLCPs
   // ------------------------------------------------------------------

   /**
    * Seeds the Z-basis for a BLCP from the supplied state vector. The first
    * {@code nub} variables are unbounded and are forced to be basic. Returns
    * {@code false} if the basis matrix for the unbounded variables is singular,
    * in which case the caller should fall back to a cold solve.
    */
   protected boolean seedBasisBLCP (VectorNi state, int nub) {
      int n = mySize;
      if (myLocalStateBuf.length < n) {
         myLocalStateBuf = new int[n];
      }
      myState = myLocalStateBuf;

      // determine the desired state for each variable, and set the non-basic
      // variables to their bounds
      for (int i = 0; i < nub; i++) {
         myState[i] = Z_VAR;
      }
      for (int i = nub; i < n; i++) {
         int s = state.get(i);
         if (s != Z_VAR && s != W_VAR_UPPER) {
            s = W_VAR_LOWER;
         }
         // sanity overrides for variables that cannot be at the requested bound
         if (s == W_VAR_LOWER && myLo[i] == -INF) {
            s = W_VAR_UPPER;
         }
         else if (s == W_VAR_UPPER && myHi[i] == INF) {
            s = W_VAR_LOWER;
         }
         myState[i] = s;
         if (s == W_VAR_LOWER) {
            myZBuf[i] = myLo[i];
         }
         else if (s == W_VAR_UPPER) {
            myZBuf[i] = myHi[i];
         }
      }
      return rebuildBasisBLCP (nub);
   }

   /**
    * Rebuilds the Cholesky factor and pivot ordering for a BLCP from the Z
    * entries of myState (processing variables in index order), removes any
    * basic variables that are out of bounds, and recomputes the z and w values.
    * The first {@code nub} variables are unbounded and must remain basic;
    * returns {@code false} if their basis matrix is singular. See
    * {@link #rebuildBasisLCP} for why the basis is rebuilt in a canonical
    * order.
    */
   protected boolean rebuildBasisBLCP (int nub) {
      int n = mySize;
      myWarmTol = computeWarmTol();
      myTol = myWarmTol;

      // start with an empty basis
      for (int i = 0; i < n; i++) {
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
      myCholesky.clear();

      // build the basis, processing the unbounded variables first
      for (int i = 0; i < n; i++) {
         if (myState[i] == Z_VAR) {
            myState[i] = W_VAR_LOWER;
            if (updateZBasis (i)) {
               myState[i] = Z_VAR;
            }
            else if (i < nub) {
               // unbounded basis is singular; warm start not possible
               return false;
            }
            else {
               // could not add to basis; place at a finite bound instead
               myState[i] = (myLo[i] == -INF ? W_VAR_UPPER : W_VAR_LOWER);
               myZBuf[i] = (myState[i] == W_VAR_UPPER ? myHi[i] : myLo[i]);
            }
         }
      }
      // remove any basic variables that are out of bounds
      projectFeasibleBLCP (nub);
      // compute the w values consistent with the basis
      computeWBLCP();
      return true;
   }

   /**
    * Computes the basic z values for a BLCP from the current basis (taking into
    * account the non-basic z values which are held at their bounds), storing
    * them in myZBuf.
    */
   protected void computeBasicZBLCP () {
      double[] Mbuf = myM.getBuffer();
      double[] qbuf = myQ.getBuffer();
      int mw = myM.getBufferWidth();
      myX.setSize (myNumZBasic);
      double[] xbuf = myX.getBuffer();
      for (int ip = 0; ip < myNumZBasic; ip++) {
         int i = myPivotedToInitialIdxs[ip];
         double Mz = 0;
         for (int jp = myNumZBasic; jp < mySize; jp++) {
            int j = myPivotedToInitialIdxs[jp];
            Mz += Mbuf[i * mw + j] * myZBuf[j];
         }
         xbuf[ip] = -qbuf[i] - Mz;
      }
      if (myNumZBasic > 0) {
         myCholesky.solve (myX, myX);
      }
      for (int ip = 0; ip < myNumZBasic; ip++) {
         myZBuf[myPivotedToInitialIdxs[ip]] = xbuf[ip];
      }
   }

   /**
    * Repeatedly removes the most out-of-bounds basic variable from the basis
    * (placing it at the violated bound) until all basic z values lie within
    * their bounds. Unbounded variables (index {@code < nub}) are never removed.
    */
   protected void projectFeasibleBLCP (int nub) {
      while (true) {
         computeBasicZBLCP();
         int worst = -1;
         double maxviol = myTol;
         boolean below = false;
         for (int ip = 0; ip < myNumZBasic; ip++) {
            int i = myPivotedToInitialIdxs[ip];
            if (i < nub) {
               continue; // unbounded; never infeasible
            }
            double viol = 0;
            boolean lo = false;
            if (myZBuf[i] < myLo[i]) {
               viol = myLo[i] - myZBuf[i];
               lo = true;
            }
            else if (myZBuf[i] > myHi[i]) {
               viol = myZBuf[i] - myHi[i];
               lo = false;
            }
            if (viol > maxviol) {
               maxviol = viol;
               worst = i;
               below = lo;
            }
         }
         if (worst == -1) {
            break;
         }
         updateZBasis (worst);
         if (below) {
            myState[worst] = W_VAR_LOWER;
            myZBuf[worst] = myLo[worst];
         }
         else {
            myState[worst] = W_VAR_UPPER;
            myZBuf[worst] = myHi[worst];
         }
      }
   }

   /**
    * Computes the w values for a BLCP that are consistent with the current basis
    * and the current z values.
    */
   protected void computeWBLCP () {
      double[] qbuf = myQ.getBuffer();
      double[] Mbuf = myM.getBuffer();
      int mw = myM.getBufferWidth();
      for (int i = 0; i < mySize; i++) {
         if (myState[i] == Z_VAR) {
            myWBuf[i] = 0;
         }
         else {
            double w = qbuf[i];
            for (int j = 0; j < mySize; j++) {
               double z = myZBuf[j];
               if (z != 0) {
                  w += Mbuf[i * mw + j] * z;
               }
            }
            myWBuf[i] = w;
         }
      }
   }

}
