package maspack.solvers;

import java.util.*;

import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;
import maspack.util.*;

/**
 * Solves linear complementarity problems (LCPs) and bounded linear
 * complementarity problems (BLCPs) for symmetric positive definite (SPD)
 * matrices using Murty's method. Details on Murty's method can be found in
 * Claude Lacoursiere's Ph.D. thesis. <i>Ghosts and Machines: Regularized
 * Variational Methods for Interactive Simulations of Multibodies with Dry
 * Frictional Contact</i>, as well as in ``Algorithms for Linear
 * Complementarity Problems'', by Joaquim Judice (1994).
 */
public class MurtyLCPSolver implements LCPSolver {

   protected double EPS = 1e-10;

   protected double[] myMvBuf;
   protected double[] myQvBuf;
   protected double[] myMcolBuf;
   protected double[] myZBuf;
   protected double[] myWBuf;

   protected double[] myLo;
   protected double[] myHi;

   protected boolean[] myZBasic;
   protected int mySize;
   protected int myCapacity;
   protected int[] myState;

   protected CholeskyDecomposition myCholesky;
   protected VectorNd myX;
   protected int[] myPivotedToInitialIdxs;
   protected int[] myBlockPivots;
   // protected int[] myInitialToPivotedIdxs;
   protected int myNumZBasic;
   protected MatrixNd myM;
   protected VectorNd myQ;

   protected double myDefaultTol = -1;
   protected boolean myDefaultBlockPivoting = false;
   protected boolean myBlockPivoting = false;
   protected double myTol;
   protected int myNumFailedPivots;
   protected int myIterationLimit = 100;
   protected int myIterationCnt;
   protected int myPivotCnt;
   protected boolean myComputeResidual = false;
   protected double myResidual = 0;
   protected boolean mySilentP = false;

   public static final int SHOW_NONE = 0x00;
   public static final int SHOW_PIVOTS = 0x01;
   public static final int SHOW_TOL_UPDATE = 0x02;
   public static final int SHOW_BLOCK_PIVOT_CHANGE = 0x04;
   public static final int SHOW_VARIABLES = 0x08;
   public static final int SHOW_RETURNS = 0x10;
   public static final int SHOW_ALL = (SHOW_PIVOTS | SHOW_TOL_UPDATE);

   protected class Pivot {
      int myStateReq;
      int myIdx;
      double myTestValue;

      Pivot (int idx, int state, double testValue) {
         myIdx = idx;
         myStateReq = state;
         myTestValue = testValue;
      }
   }

   protected int myDebug = SHOW_NONE; // 

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

   public MurtyLCPSolver() {
      myMvBuf = new double[0];
      myQvBuf = new double[0];
      myMcolBuf = new double[0];
      myZBasic = new boolean[0];
      myState = new int[0];
      mySize = 0;

      myCholesky = new CholeskyDecomposition();
      myPivotedToInitialIdxs = new int[0];
      myBlockPivots = new int[0];
      // myInitialToPivotedIdxs = new int[0];
      myX = new VectorNd (0);      
   }

   protected void setProblemSize (int size) {
      if (size > myCapacity) {
         myMvBuf = new double[size * size];
         myQvBuf = new double[size];
         myMcolBuf = new double[size];
         myZBasic = new boolean[size];
         myState = new int[size];

         // myInitialToPivotedIdxs = new int[size];
         myPivotedToInitialIdxs = new int[size];
         myBlockPivots = new int[size];

         myCholesky.ensureCapacity (size);
         myCapacity = size;
      }
      mySize = size;
      myX.setSize (size);
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
    * <p>Setting a negative value will cause the solver to choose its
    * own tolerance based on the input data. This can then be retrieved,
    * after the solve, using {@link #getLastSolveTol}.
    * 
    * @param tol
    * new numeric tolerance. Negative numbers will be truncated to 0.
    * @see #getTolerance
    */
   public void setTolerance (double tol) {
      myDefaultTol = tol;
   }

   public boolean getBlockPivoting() {
      return myDefaultBlockPivoting;
   }

   public void setBlockPivoting (boolean enable) {
      myDefaultBlockPivoting = enable;
   }

   public boolean getLastBlockPivoting() {
      return myBlockPivoting;
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

   void clearBasis () {
      myCholesky.clear();
      for (int i=0; i<mySize; i++) {
         myPivotedToInitialIdxs[i] = i;
      }
      myNumZBasic = 0;
   }

   boolean initializeBasis (int numz, int nub, double[] lo, double[] hi) {
      clearBasis();
      if (numz > 0) {
         double[] Mbuf = myM.getBuffer();
         int mw = myM.getBufferWidth();

         MatrixNd B = new MatrixNd(numz, numz);
         // populate basic columns of B from M:
         int jp = 0;
         for (int j=0; j<mySize; j++) {
            if (myState[j] == Z_VAR) {
               int ip = 0;
               for (int i=0; i<=j; i++) {
                  if (myState[i] == Z_VAR) {
                     double val = Mbuf[i*mw + j];
                     B.set (ip, jp, val);
                     if (i != j) {
                        B.set (jp, ip, val);
                     }
                     ip++;
                  }
               }
               myPivotedToInitialIdxs[j] = myPivotedToInitialIdxs[jp];
               myPivotedToInitialIdxs[jp] = j;
               jp++;
            }
         }
         // form the basis by factoring B:
         myNumZBasic = numz;
         try {
            myCholesky.factor (B);
         }
         catch (Exception e) {
            System.out.println ("EXCEPTION");
            // can only occur if M is not positive definite. Try to initialize
            // the basis one column at a time, since the solver may still be
            // able to find a solution
            clearBasis();
            for (int j=0; j<mySize; j++) {
               if (myState[j] == Z_VAR) {
                  if (!addToZBasis (j)) {
                     if (j < nub) {
                        return false;
                     }
                     else {
                        if (lo != null) {
                           if (lo[j] == -INF) {
                              myState[j] = W_VAR_UPPER;
                              myZBuf[j] = hi[j];
                           }
                           else {
                              myState[j] = W_VAR_LOWER;
                              myZBuf[j] = lo[j];
                           }
                        }
                        else {
                           myState[j] = W_VAR_LOWER;
                           myZBuf[j] = 0;
                        }
                     }
                  }
               }
            }
         }
      }
      return true;
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

      // initialize state
      int numz = 0;
      if (state != null) {
         for (int i=0; i<size; i++) {
            myState[i] = state.get (i);
            if (myState[i] == Z_VAR) {
               numz++;
            }
            else { // myState[i] = W_VAR_LOWER
               myZBuf[i] = 0;
            }
         }
      } 
      else {
         for (int i=0; i<size; i++) {
            myState[i] = W_VAR_LOWER;
            myZBuf[i] = 0;
         }
      }
      initializeBasis (numz, /*nub=*/0, /*lo=*/null, /*hi=*/null);

      if (myDefaultTol < 0) {
         // initialize initial tolerance based on q
         double maxq = 0;
         double[] qbuf = myQ.getBuffer();
         for (int i = 0; i < mySize; i++) {
            maxq = Math.max (Math.abs(qbuf[i]), maxq);
         }
         myTol = EPS*maxq;
      }
      else {
         myTol = myDefaultTol;
      }

      Status status = dosolve();
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

   protected Status dosolve () {
      int maxIterations = myIterationLimit * mySize;
      myIterationCnt = 0;
      myPivotCnt = 0;
      myNumFailedPivots = 0;

      double[] qbuf = myQ.getBuffer();
      double[] Mbuf = myM.getBuffer();
      
      int ninfPrev = mySize;
      int p = 4;
      int blockPivotIterLimit = p;
      myBlockPivoting = myDefaultBlockPivoting;

      if ((myDebug & SHOW_PIVOTS) != 0) {
         System.out.println ("LCP START blockPivoting=" + myBlockPivoting);
      }      

      while (myIterationCnt < maxIterations) {
         myIterationCnt++;
         int mw = myM.getBufferWidth();
         myX.setSize (myNumZBasic);
         double[] xbuf = myX.getBuffer();
         for (int ip=0; ip<myNumZBasic; ip++) {
            xbuf[ip] = -qbuf[myPivotedToInitialIdxs[ip]];
         }
         if (myNumZBasic > 0) {
            myCholesky.solve (myX, myX);
         }
         // update z and w 
         for (int ip=0; ip<mySize; ip++) {
            int i = myPivotedToInitialIdxs[ip];
            if (ip < myNumZBasic) {
               myWBuf[i] = 0;
               myZBuf[i] = xbuf[ip];
            }
            else {
               double w = 0;
               for (int jp=0; jp<myNumZBasic; jp++) {
                  w += Mbuf[i * mw + myPivotedToInitialIdxs[jp]] * xbuf[jp];
               }
               w += qbuf[i];
               myWBuf[i] = w;
               myZBuf[i] = 0;
            }
         }
         ArrayList<Pivot> pivotRequests = findPivots();
         int ninf = pivotRequests.size();
         if (ninf == 0) {
            if ((myDebug & SHOW_PIVOTS) != 0) {
               printState();
            }
            if ((myDebug & SHOW_RETURNS) != 0) {
               System.out.println ("SOLVED");
            }
            return Status.SOLVED;
         }
         if (myBlockPivoting) {
            int npiv = applyBlockPivots (pivotRequests);
            if (npiv == 0) {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NO_SOLUTION");
               }
               return Status.NO_SOLUTION;
            }
            else if (ninf < ninfPrev) {
               ninfPrev = ninf;
               blockPivotIterLimit = myIterationCnt + p;
            }
            else if (myIterationCnt > blockPivotIterLimit) { 
               if ((myDebug & SHOW_BLOCK_PIVOT_CHANGE) != 0) {
                  System.out.println ("block pivots OFF");
               }
              myBlockPivoting = false;
            }
            myPivotCnt += npiv;
         }
         else {
            if (!applySinglePivot(pivotRequests)) {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NO_SOLUTION");
               }
               return Status.NO_SOLUTION;
            }
            myPivotCnt++;
         }
         //checkBasis();
      }
      if ((myDebug & SHOW_RETURNS) != 0) {
         System.out.println ("ITERATION LIMIT EXCEEDED");
      }
      return Status.ITERATION_LIMIT_EXCEEDED;
   }

   double estimateUpdatedBasisCondition (MatrixNd BX, int s) {
      int numz = myCholesky.getSize();
      BX.setSize (numz+1, numz+1);

      int ip = 0;
      for (int i=0; i<mySize; i++) {
         if (myState[i] == Z_VAR || i == s) {
            int jp = 0;
            for (int j=0; j<mySize; j++) {
               if (myState[j] == Z_VAR || j == s) {
                  BX.set (ip, jp, myM.get(i,j));
                  jp++; 
               }
            }
            ip++;
         }
      }
      CholeskyDecomposition chol = new CholeskyDecomposition();
      try {
         chol.factor (BX);
      }
      catch (Exception e) {
         return Double.MAX_VALUE;
      }
      return chol.conditionEstimate(BX);
   }

   void checkBasis (MatrixNd BX) {

      MatrixNd L = new MatrixNd();
      myCholesky.get(L);
      MatrixNd B = new MatrixNd();
      B.mulTransposeRight (L, L);

      System.out.println ("B=\n" + B.toString("%14.10f"));
      System.out.println ("BX=\n" + BX.toString("%14.10f"));

      int nz = myCholesky.getSize();
      MatrixNd C = new MatrixNd (nz, nz);
      for (int ip=0; ip<nz; ip++) {
         int i = myPivotedToInitialIdxs[ip];
         for (int jp=0; jp<nz; jp++) {
            int j = myPivotedToInitialIdxs[jp];
            C.set (ip, jp, myM.get(i,j));            
         }
      }
      System.out.println ("C=\n" + C.toString("%14.10f"));
   }

   double estimateBasisCondition () {
      int numz = myCholesky.getSize();
      MatrixNd B = new MatrixNd(numz, numz);

      int ip = 0;
      for (int i=0; i<mySize; i++) {
         if (myState[i] == Z_VAR) {
            int jp = 0;
            for (int j=0; j<mySize; j++) {
               if (myState[j] == Z_VAR) {
                  B.set (ip, jp, myM.get(i,j));
                  jp++; 
               }
            }
            ip++;
         }
      }
      CholeskyDecomposition chol = new CholeskyDecomposition();
      try {
         chol.factor (B);
      }
      catch (Exception e) {
         return Double.MAX_VALUE;
      }
      return chol.conditionEstimate(B);
   }

   ArrayList<Pivot> findPivots () {
      ArrayList<Pivot> requests = new ArrayList<Pivot>();

      for (int i=0; i<mySize; i++) {
         if (myState[i] == Z_VAR) {
            if (myZBuf[i] < -myTol) {
               requests.add (new Pivot (i, W_VAR_LOWER, myZBuf[i]));
            }
         }
         else { // state = W_VAR_LOWER
            if (myWBuf[i] < -myTol) {
               requests.add (new Pivot (i, Z_VAR, myWBuf[i]));
            }
         }
      }
      return requests;
   }

   boolean applySinglePivot (ArrayList<Pivot> pivotRequests) {
      if ((myDebug & SHOW_PIVOTS) != 0) {
         printState();
      }
      for (int k=pivotRequests.size()-1; k>=0; k--) {
         Pivot piv = pivotRequests.get(k);
         if (piv.myTestValue < -myTol) {
            int i = piv.myIdx;
            if (piv.myStateReq == W_VAR_LOWER) {
               updateZBasis (i);
               myState[i] = W_VAR_LOWER;
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println (" PIVOT "+i+" to L");
               }
               return true;
            }
            else { // stateReq = Z_VAR
               if (updateZBasis (i)) {
                  myState[i] = Z_VAR;
                  if ((myDebug & SHOW_PIVOTS) != 0) {
                     System.out.println (" PIVOT "+i+" to Z");
                  }
                  return true;
               }
               else {
                  if ((myDebug & SHOW_TOL_UPDATE) != 0) {
                     System.out.println (
                        "SS setting tol " +  2*Math.abs(myWBuf[i]));
                  }
                  //myTol = 2*Math.abs(myWBuf[i]);
                  myNumFailedPivots++;
               }
            }
         }
      }
      return false;
   }

   int applyBlockPivots (ArrayList<Pivot> pivotRequests) {
      int npivs = 0;
      if ((myDebug & SHOW_PIVOTS) != 0) {
         printState();
      }
      boolean hasAddedToZ = false;
      for (Pivot piv : pivotRequests) {
         if (piv.myTestValue < -myTol) {
            int i = piv.myIdx;
            if (piv.myStateReq == W_VAR_LOWER) {
               updateZBasis (i);
               myState[i] = W_VAR_LOWER;
               myZBuf[i] = 0;
               npivs++;
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println (" PIVOT "+i+" to L");
               }
            }
            else { // stateReq = Z_VAR
               if (updateZBasis(i)) {
                  myState[i] = Z_VAR;
                  hasAddedToZ = true;
                  npivs++;
                  if ((myDebug & SHOW_PIVOTS) != 0) {
                     System.out.println (" PIVOT "+i+" to Z");
                  }
               }
               else {
                  if (!hasAddedToZ) {
                     if ((myDebug & SHOW_TOL_UPDATE) != 0) {
                        System.out.println (
                           "SB setting tol " +  2*Math.abs(myWBuf[i]));
                     }
                     //myTol = 2*Math.abs(myWBuf[i]);
                  }
                  myNumFailedPivots++;
               }
            }
         }
      }
      return npivs;
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
      if (lo.size() < size) {
         throw new IllegalArgumentException (
            "lo has size "+lo.size()+"; must be at least "+size);
      }
      if (hi.size() < size) {
         throw new IllegalArgumentException (
            "hi has size "+hi.size()+"; must be at least "+size);
      }
      if (nub > size) {
         throw new IllegalArgumentException (
            "nub=" + nub + " is greater than size=" + size);
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

      for (int i=0; i<nub; i++) {
         if (myLo[i] != -INF || myHi[i] != INF) {
            throw new IllegalArgumentException ("unbounded variable " + i
            + " must have lo/hi settings -inf/+inf");
         }
      }
      for (int i=nub; i<size; i++) {
         // if (myLo[i] == -INF && myHi[i] == INF) {
         //    throw new IllegalArgumentException (
         //       "unbounded variables detected outside range indicated by nub");
         // }
         if (myLo[i] == INF) {
            throw new IllegalArgumentException ("lo[" + i
            + "] set to +infinity");
         }
         if (myHi[i] == -INF) {
            throw new IllegalArgumentException ("hi[" + i
            + "] set to -infinity");
         }
      }

      // initialize state
      int numz = nub;
      for (int i=0; i<nub; i++) {
         myState[i] = Z_VAR;
         myWBuf[i] = 0; // XXX need this?
      }
      if (state != null) {
         for (int i=nub; i<size; i++) {
            int s = state.get (i);
            if (s == W_VAR_LOWER && myLo[i] == -INF) {
               // sanity override
               s = W_VAR_UPPER;
            }
            myState[i] = s;
            if (s == W_VAR_LOWER) {
               myZBuf[i] = myLo[i];
            }
            else if (s == W_VAR_UPPER) {
               myZBuf[i] = myHi[i];
            }
            else { // s == Z_VAR
               numz++;
            }
         }
      } 
      else {
         for (int i=nub; i<size; i++) {
            if (myLo[i] == -INF) {
               myState[i] = W_VAR_UPPER;
               myZBuf[i] = myHi[i];
            }
            else {
               myState[i] = W_VAR_LOWER;
               myZBuf[i] = myLo[i];
            }
         }
      }
      if (!initializeBasis (numz, nub, myLo, myHi)) {
         // no solution because matrix for the unbounded variables is singular
         return Status.NO_SOLUTION;
      }
      
      if (myDefaultTol < 0) {
         // initialize initial tolerance based on q
         double[] qbuf = q.getBuffer();
         double maxq = 0;
         for (int i = 0; i < mySize; i++) {
            maxq = Math.max (Math.abs(qbuf[i]), maxq);
         }
         myTol = EPS*maxq;
      }
      else {
         myTol = myDefaultTol;
      }

      Status status = dosolveBLCP (nub);
      if (state != null) {
         for (int i=0; i<size; i++) {
            state.set (i, myState[i]);
         }
      }
      return status;
   }

   void printState() {
      System.out.println (LCPSolver.stateToString (myState, mySize));
   }

   protected Status dosolveBLCP (int nub) {
      int maxIterations = myIterationLimit * mySize;
      myPivotCnt = 0;
      myIterationCnt = 0;
      myNumFailedPivots = 0;

      double[] qbuf = myQ.getBuffer();
      double[] Mbuf = myM.getBuffer();
      double[] xbuf;

      int mw = myM.getBufferWidth();

      int ninfPrev = mySize;
      int p = 4;
      int blockPivotIterLimit = p;
      myBlockPivoting = myDefaultBlockPivoting;

      if ((myDebug & SHOW_PIVOTS) != 0) {
         System.out.println ("BLCP START blockPivoting=" + myBlockPivoting);
      }      

      while (myIterationCnt < maxIterations) {
         myIterationCnt++;
         //printState();
         // solve for za from Maa za = -qa - Mab zb
         myX.setSize (myNumZBasic);
         xbuf = myX.getBuffer();
         for (int ip=0; ip<myNumZBasic; ip++) {
            int i = myPivotedToInitialIdxs[ip];
            double Mz = 0;
            for (int jp=myNumZBasic; jp<mySize; jp++) {
               int j = myPivotedToInitialIdxs[jp];
               Mz += Mbuf[i*mw + j]*myZBuf[j];
            }
            xbuf[ip] = -qbuf[i]-Mz;
         }
         if (myNumZBasic > 0) {
            myCholesky.solve (myX, myX);
         }
         // set za components of z, and set wa = 0, wb = Mba za + Mbb zb
         for (int ip=0; ip<mySize; ip++) {
            int i = myPivotedToInitialIdxs[ip];
            if (ip < myNumZBasic) {
               myWBuf[i] = 0;
               myZBuf[i] = xbuf[ip];
            }
            else {
               double w = 0;
               for (int j=0; j<mySize; j++) {
                  w += Mbuf[i*mw + j]*myZBuf[j];
               }
               myWBuf[i] = w + qbuf[i];
            }
         }
         ArrayList<Pivot> pivotRequests = findBLCPPivots();
         int ninf = pivotRequests.size();
         if (ninf == 0) {
            if ((myDebug & SHOW_PIVOTS) != 0) {
               printState();
            }
            if ((myDebug & SHOW_RETURNS) != 0) {
               System.out.println ("SOLVED");
            }
            return Status.SOLVED;
         }         
         if (myBlockPivoting) {
            int npiv = applyBlockBLCPPivots(pivotRequests);
            if (npiv == 0) {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NO_SOLUTION");
               }
               return Status.NO_SOLUTION;
            }
            else if (ninf < ninfPrev) {
               ninfPrev = ninf;
               blockPivotIterLimit = myIterationCnt + p;
            }
            else if (myIterationCnt > blockPivotIterLimit) {
               if (myBlockPivoting) {
                  if ((myDebug & SHOW_BLOCK_PIVOT_CHANGE) != 0) {
                     System.out.println (" Block pivoting OFF");
                  }
                  myBlockPivoting = false;
               }
            }
            myPivotCnt += npiv;
         }
         else {
            if (!applySingleBLCPPivot(pivotRequests)) {
               if ((myDebug & SHOW_RETURNS) != 0) {
                  System.out.println ("NO_SOLUTION");
               }
               return Status.NO_SOLUTION;
            }
            myPivotCnt++;
         }
      }
      if ((myDebug & SHOW_RETURNS) != 0) {
         System.out.println ("ITERATION LIMIT EXCEEDED");
      }
      return Status.ITERATION_LIMIT_EXCEEDED;
   }

   boolean applySingleBLCPPivot (ArrayList<Pivot> pivotRequests) {
      if ((myDebug & SHOW_PIVOTS) != 0) {
         printState();
      }
      for (int k=pivotRequests.size()-1; k>=0; k--) {
         Pivot piv = pivotRequests.get(k);
         if (piv.myTestValue < -myTol) {
            int i = piv.myIdx;
            if (piv.myStateReq == W_VAR_LOWER) {
               updateZBasis (i);
               myState[i] = W_VAR_LOWER;
               myZBuf[i] = myLo[i];
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println (" PIVOT "+i+" to L");
               }
               return true;
            }
            else if (piv.myStateReq == W_VAR_UPPER) {
               updateZBasis (i);
               myState[i] = W_VAR_UPPER;
               myZBuf[i] = myHi[i];
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println (" PIVOT "+i+" to H");
               }
               return true;
            }
            else { // stateReq = Z_VAR
               if (updateZBasis (i)) {
                  myState[i] = Z_VAR;
                  if ((myDebug & SHOW_PIVOTS) != 0) {
                     System.out.println (" PIVOT "+i+" to Z");
                  }
                  return true;
               }
               else {
                  if ((myDebug & SHOW_TOL_UPDATE) != 0) {
                     System.out.println (
                        "BS setting tol " + (2*Math.abs(myWBuf[i])));
                  }
                  //myTol = 2*Math.abs(myWBuf[i]);
                  myNumFailedPivots++;             
               }
            }
         }
      }
      return false;
   }

   ArrayList<Pivot> findBLCPPivots () {
      ArrayList<Pivot> requests = new ArrayList<Pivot>();

      for (int i=0; i<mySize; i++) {
         int curState = myState[i];
         if (curState == Z_VAR) {
            if (myZBuf[i] - myLo[i] < -myTol) {
               requests.add (
                  new Pivot (i, W_VAR_LOWER, myZBuf[i] - myLo[i]));
            }
            else if (myHi[i] - myZBuf[i] < -myTol) {
               requests.add (
                  new Pivot (i, W_VAR_UPPER, myHi[i] - myZBuf[i]));
            }
         }
         else {
            if (curState == W_VAR_LOWER) {
               if (myWBuf[i] < -myTol) {
                  requests.add (new Pivot (i, Z_VAR, myWBuf[i]));
               }
            }
            else if (curState == W_VAR_UPPER) {
               if (myWBuf[i] > myTol) {
                  requests.add (new Pivot (i, Z_VAR, -myWBuf[i]));
               }
            }
         }
      }
      return requests;
   }

   int applyBlockBLCPPivots (ArrayList<Pivot> pivotRequests) {
      int npivs = 0;
      if ((myDebug & SHOW_PIVOTS) != 0) {
         printState();
      }
      boolean hasAddedToZ = false;
      for (Pivot piv : pivotRequests) {
         if (piv.myTestValue < -myTol) {
            int i = piv.myIdx;
            if (piv.myStateReq == W_VAR_LOWER) {
               updateZBasis (i);
               myState[i] =W_VAR_LOWER;
               myZBuf[i] = myLo[i];
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println (" PIVOT "+i+" to L");
               }
               npivs++;
            }
            else if (piv.myStateReq == W_VAR_UPPER) {
               updateZBasis (i);
               myState[i] = W_VAR_UPPER;
               myZBuf[i] = myHi[i];
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println (" PIVOT "+i+" to H");
               }
               npivs++;
            }
            else { // stateReq = Z_VAR
               if (updateZBasis(i)) {
                  myState[i] = Z_VAR;
                  hasAddedToZ = true;
                  if ((myDebug & SHOW_PIVOTS) != 0) {
                     System.out.println (" PIVOT "+i+" to Z");
                  }
                  npivs++;
               }
               else {
                  if (!hasAddedToZ) {
                     if ((myDebug & SHOW_TOL_UPDATE) != 0) {
                        System.out.println (
                           "BB setting tol " +  2*Math.abs(myWBuf[i]));
                     }
                     //myTol = 2*Math.abs(myWBuf[i]);
                  }
                  myNumFailedPivots++;
               }
            }
         }
      }
      return npivs;
   }

   protected boolean addToZBasis (int s) {
      double[] xbuf = myX.getBuffer();
      double[] Mbuf = myM.getBuffer();
      int mw = myM.getBufferWidth();
      
      for (int ip = 0; ip < myNumZBasic; ip++) {
         xbuf[ip] = Mbuf[mw * myPivotedToInitialIdxs[ip] + s];
      }
      xbuf[myNumZBasic] = Mbuf[mw * s + s];
      myX.setSize (myNumZBasic + 1);
      MatrixNd BX = new MatrixNd();
      //double condest = estimateUpdatedBasisCondition(BX, s);
      if (!myCholesky.addRowAndColumn (myX, 1e-10)) {
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
      return true;
   }

   protected boolean updateZBasis (int s) {
      if (myState[s] != Z_VAR) {
         return addToZBasis (s);
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
         myX.setSize (myNumZBasic);
         return true;
      }
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
      return true;
   }
}
