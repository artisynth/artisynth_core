package maspack.solvers;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;
import maspack.util.*;
import maspack.solvers.LCPSolver.Status;
import maspack.spatialmotion.FrictionInfo;

public class MurtyMechSolver {

   public boolean debug = false;
   public boolean showAStructureChange = false;

   // Pivot types
   protected static int Z = LCPSolver.Z_VAR;          // activate constraint
   protected static int W_HI = LCPSolver.W_VAR_UPPER; // deactivate, high bound
   protected static int W_LO = LCPSolver.W_VAR_LOWER; // deactivate, low bound
   
   public static int CONTACT_SOLVE = 0x0001; // indicates a contact solve
   public static int NT_INACTIVE = 0x0002; // NT state should not be changed
   public static int REBUILD_A = 0x0004; // indicates that A should be rebuilt

   enum ConstraintType {
      N,
      D
   };

   protected static final ConstraintType TYPE_N = ConstraintType.N;
   protected static final ConstraintType TYPE_D = ConstraintType.D;
 
   /**
    * Base descriptor for a new row to be added to the J matrix
    */
   static abstract class JRowData {
      Pivot myPivot;

      JRowData (Pivot piv) {
         myPivot = piv;
      }

      abstract void getVec (VectorNd vec);
   }

   /**
    * Provides information about the columns of the A matrix
    */
   static class AConstraintData {
      ConstraintType myType; // constraint type (N or D)
      int myCol;             // corresponding column of N or D
      int[] myBlockRowIdxs;  // block row indices (used to set values if masked)
      boolean myContactMask; // true if the constraint is masked for a contact solve
      boolean myRemovedMask; // true if the constraint is masked because it was removed
      int myIdx;             // constraint index within A

      AConstraintData (ConstraintType type, int col, int idx) {
         myType = type;
         myCol = col;
         myIdx = idx;
      }

      // number of non zeros in this constraint
      int numNonZeros (SparseBlockMatrix M) {
         int nnz = 0;
         for (int bi : myBlockRowIdxs) {
            nnz += M.getBlockRowSize(bi);
         }
         return nnz;
      }

      void addNonZerosByRow (int[] localOffs, SparseBlockMatrix M) {
         for (int bi : myBlockRowIdxs) {
            int i0 = M.getBlockRowOffset (bi);
            int iend = i0 + M.getBlockRowSize(bi);
            for (int i=i0; i<iend; i++) {
               localOffs[i]++;
            }
         }
      }

      void getCRSIndices (
         int[] colIdxs, int[] localOffs, SparseBlockMatrix M, int j) {
         for (int bi : myBlockRowIdxs) {
            int i0 = M.getBlockRowOffset (bi);
            int iend = i0 + M.getBlockRowSize(bi);
            for (int i=i0; i<iend; i++) {
               colIdxs[localOffs[i]++] = j;
            }
         }
      }

      void getCRSZeros (
         double[] values, int[] localOffs, SparseBlockMatrix M) {
         for (int bi : myBlockRowIdxs) {
            int i0 = M.getBlockRowOffset (bi);
            int iend = i0 + M.getBlockRowSize(bi);
            for (int i=i0; i<iend; i++) {
               values[localOffs[i]++] = 0;
            }
         }
      }

      void getCRSValues (
         double[] values, int[] localOffs, SparseBlockMatrix XT, int blkSizeM) {
         int bj = XT.getBlockCol(myCol);
         MatrixBlock blk = XT.firstBlockInCol(bj);
         while (blk != null) {
            int bi = blk.getBlockRow();
            if (bi >= blkSizeM) {
               break;
            }
            int i0 = XT.getBlockRowOffset (bi);
            if (blk instanceof Matrix3x1) {
               Matrix3x1Block mblk = (Matrix3x1Block)blk;
               values[localOffs[i0+0]++] = mblk.m00;
               values[localOffs[i0+1]++] = mblk.m10;
               values[localOffs[i0+2]++] = mblk.m20;
            }
            else if (blk instanceof Matrix3x2) {
               Matrix3x2Block mblk = (Matrix3x2Block)blk;
               if (myCol == XT.getBlockColOffset(bj)) {
                  // first column
                  values[localOffs[i0+0]++] = mblk.m00;
                  values[localOffs[i0+1]++] = mblk.m10;
                  values[localOffs[i0+2]++] = mblk.m20;
               }
               else {
                  // second column
                  values[localOffs[i0+0]++] = mblk.m01;
                  values[localOffs[i0+1]++] = mblk.m11;
                  values[localOffs[i0+2]++] = mblk.m21;
               }
            }
            else {
               int j = myCol- XT.getBlockColOffset(bj); // block local j
               for (int i=0; i<blk.rowSize(); i++) {
                  values[localOffs[i0+i]++] = blk.get (i, j);
               }
            }
            blk = blk.down();
         }
      }

      boolean isMasked() {
         return myContactMask || myRemovedMask;
      }

      void setContactMask (boolean enable) {
         myContactMask = enable;
      }

      boolean getContactMask () {
         return myContactMask;
      }

      void setRemovedMask (boolean enable) {
         myRemovedMask = enable;
      }

      boolean getRemovedMask () {
         return myRemovedMask;
      }
   }

   /**
    * Describes a new row to be added to the H matrix
    */
   static class HRowData extends JRowData {
      DynamicIntArray myColIdxs;
      DynamicDoubleArray myValues;
      double myRh;
      double myBh;

      int numVals() {
         return myValues.size();
      }

      double dot (VectorNd vec) {
         double sum = 0;
         for (int k=0; k<numVals(); k++) {
            int j = myColIdxs.get(k);
            sum += myValues.get(k)*vec.get(j);
         }
         return sum;
      }

      void getVec (VectorNd vec) {
         for (int k=0; k<numVals(); k++) {
            int j = myColIdxs.get(k);
            vec.set (j, myValues.get(k));
         }
      }

      HRowData (Pivot piv) {
         super (piv);
         myColIdxs = new DynamicIntArray();
         myValues = new DynamicDoubleArray();
      }
   }

   /**
    * Describes a new row to be added to the E matrix
    */
   static class ERowData extends JRowData {
      int myAIdx; // index of the removed constraint wrt A
      double myBe;

      void getVec (VectorNd vec) {
         vec.set (myAIdx, 1);
      }

      ERowData (Pivot piv) {
         super (piv);
      }
   }

   /**
    * Multiplies vec by the current E matrix and places the result in res,
    * starting at the index indicated by {@code rowIdx}.
    */
   void mulE (VectorNd res, VectorNd vec, int rowIdx) {
      for (int i=0; i<myE.size(); i++) {
         res.set (rowIdx++, vec.get(myE.get(i)));
      }
   }        

   /**
    * Multiplies vec, starting at the index specified by {@code colIdx}, by the
    * transpose of the current E matrix and places the result in res, starting
    * at {@code rowIdx}.
    */
   void mulTransposeAddE (VectorNd res, VectorNd vec, int rowIdx, int colIdx) {
      //      for (int i=rowIdx; i<res.size(); i++) {
      //         res.set (i, 0);
      //      }
      for (int j=0; j<myE.size(); j++) {
         res.add (myE.get(j), vec.get(colIdx++));
      }
   }

   /**
    * Information about a specific pivot request.
    */
   static class Pivot {
      ConstraintType myType; // constraint type (N or D matrix)
      int myColIdx;          // constraint index within N or D
      int myAColIdx;         // if basic in A, constraint index within A, or -1
      int myNewState;        // requested new state for the constraint
      int myCurState;        // current (pre-pivot) state for the constraint

      Pivot (
         int colIdx, int acolIdx,
         int curState, int newState, ConstraintType type) {

         myColIdx = colIdx;
         myAColIdx = acolIdx;
         myNewState = newState;
         myCurState = curState;
         myType = type;
      }

      boolean requiresNewJRow() {
         if (myAColIdx != -1) {
            // constraint is basic in A
            return myNewState != Z;            
         }
         else {
            // constraint is not basic in A
            return myNewState == Z;
         }
      }

      public String toString() {
         return (myType + " " + myColIdx + " to " +
                 LCPSolver.stateToChar(myNewState));
      }
   }

   // variables associated with the augmenting constraints H
   protected SignedCholeskyDecomp myHCholesky; // factorization of H
   protected SparseCRSMatrix myH;    // H submatrix of J
   protected VectorNd myBh;          // right hand side for H 
   protected ArrayList<Pivot> myHPivots; // pivots associated with H
   protected VectorNi myE;           // E submatrix of J. Lists E indices wrt A
   protected VectorNd myBe;          // right hand side for E
   protected ArrayList<Pivot> myEPivots; // pivots associated with E
   protected VectorNd myY;           // y value for the A-J system
   protected VectorNd myX;           // solve vector for the A-J system

   // variables controlling the pivoting and iterations
   protected double myDefaultTol = 1e-10; // default solve tolerance
   protected boolean myDefaultBlockPivoting = true; // enable block pivots
   protected boolean myBlockPivoting = true; // current block pivot setting
   protected int myBlockPivotFailCnt = 0; // number of times block pivoting failed
   protected double myTol;           // current solve tolerance
   protected int myNumFailedPivots;  // number of pivots that failed
   protected int myIterationLimit = 10; // default iteration limit
   protected int myMaxIterations;    // current iteration limit
   protected int myIterationCnt;     // number of iterations for last solve
   protected int myPivotCnt;         // number of pivots for last solve
   protected int mySolveCnt;         // number of solves of A for last solve

   // sparse solver used to solve the A system
   private SparseSolverId mySolverType = SparseSolverId.Pardiso;
   DirectSolver myMatrixSolver;      // current sparse solver
   UmfpackSolver myUmfpack;          // Umfpack solver, if used
   PardisoSolver myPardiso;          // Pardiso solver, if used
   int mySavedMaxRefinementSteps;    // saved value of Pardiso refinement steps
   boolean myAMatrixFactored;        // A matrix factored and ready for solution
   
   // control variables related to hybrid solves
   int myHybridCnt = 0;
   boolean myFakeHybridFail = false; // for testing only
   FunctionTimer myTimer = new FunctionTimer();

   double myTimingWeight = 0.25;
   double myAvgHybridTime = 0.0;
   double myAvgDirectTime = 0.0;
   double myHybridRatio = 0.8; // refactor when hybrid/direct time exceeds this

   // A system attributes
   protected int mySizeA;            // size of the system A
   protected VectorNd myB;           // right hand side of the system A
   protected boolean myAStructureChanged; // A structure changed from last solve
   protected double[] myValuesA;     // used to pass A values to solver
   protected int[] myRowOffsA;       // row offsets for A
   protected int myNumValsA;         // number of non-zero values in A
   ArrayList<AConstraintData> myAConsData; // A matrix constraint information

   // mass/stiffness matrix and bilateral constraints
   protected SparseBlockMatrix myM;  // system matrix M
   protected VectorNd myBm;          // right hand side for M
   protected int mySizeM;            // number of rows/cols of M being used
   protected int myBlkSizeM;         // number of blocks of M being used
   protected int myVersionM;         // most recent version number for M

   protected int mySizeG;            // number of G constraints
   protected SparseBlockMatrix myGT; // tranpose of G matrix
   protected SparseBlockSignature myGTSignature; // signature of Gt matrix
   protected VectorNd myRg;          // regularization coefficients for G
   protected VectorNd myBg;          // right hand side for G

   protected int mySizeMG;           // sizeM + sizeG

   // unilateral constraints
   protected int mySizeN;            // number of N constraints
   protected SparseBlockMatrix myNT; // transpose of N matrix
   protected SparseBlockSignature myNTSignature; // signature of NT matrix
   protected int[] myStateN;         // current state of N constraints
   protected VectorNd myRn;          // regularization coefficients for N    
   protected VectorNd myBn;          // right hand side for N
   protected VectorNd myWn;          // w vector for N

   protected int mySizeNA;           // number of active N constraints in A
   protected AConstraintData[] myAConsN; // A constraints (if any) for N
   protected int[] myJRowN;          // index of J constraint for N, or -1 if none
   
   protected boolean myNTActivityFrozen = false;

   // friction constraints
   protected int mySizeD;            // number of D constraints
   protected SparseBlockMatrix myDT; // transpose of D matrix
   protected SparseBlockMatrix myLastDTFull; // DT for last full solve
   protected SparseBlockSignature myDTSignature; // signature of DT matrix
   protected int[] myStateD;         // current state of D constraints
   protected VectorNd myRd;          // regularization coefficients for N  
   protected VectorNd myLastRdFull;  // Rd for last full solve
   protected VectorNd myBd;          // right hand side for D
   protected VectorNd myWd;          // w vector for D
   protected VectorNd myFlim;        // maximum force values for D constraints

   protected int mySizeDA;           // number of active N constraints in A
   protected AConstraintData[] myAConsD; // A constraints (if any) for D
   protected AConstraintData[] myAConsDFull; // AConsD for last full solve
   protected int[] myJRowD;          // index of J constraint for D, or -1 if none

   protected int mySizeND;           // sizeN + sizeD
   protected ArrayList<FrictionInfo> myFrictionInfo; // information for each D constraint
   
   // timers and counters for assessing computational costs with respect to the
   // A system
   protected FunctionTimer myAnalyzeTimer;
   protected int myTotalAnalyzeCnt;
   protected FunctionTimer myFactorTimer;
   protected int myTotalFactorCnt;
   protected FunctionTimer mySolveTimer;
   protected int myTotalSolveCnt;

   public static final int SHOW_NONE = 0x00;
   public static final int SHOW_PIVOTS = 0x01;
   public static final int SHOW_TOL_UPDATE = 0x02;
   public static final int SHOW_BLOCK_PIVOT_CHANGE = 0x04;
   public static final int SHOW_VARIABLES = 0x08;
   public static final int SHOW_RETURNS = 0x10;
   public static final int SHOW_ALL = (SHOW_PIVOTS | SHOW_TOL_UPDATE);

   protected boolean myHybridSolves; // use hybrid solves when N and D are null
   private static int myHybridSolveTol = 10;
   protected boolean myNTFrictionActivity = true;

   protected boolean mySilentP = false;
   protected int myDebug = SHOW_NONE;
   protected boolean myAdaptivelyRebuildA = true;
   protected boolean myUpdateABetweenSolves = true;
   protected boolean myContactSolveP = false; // flag indicating contact solve
   // rebuild A if sizeND/myAconsD.size() <= rebuildARatio;
   protected double myRebuildARatio = 0.1;

   int[] myARowOffs;
   int[] myAColIdxs;

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

   public void setAdaptivelyRebuildA (boolean enable) {
      myAdaptivelyRebuildA = enable;
   }

   public boolean getAdaptivelyRebuildA () {
      return myAdaptivelyRebuildA;
   }

   public void setUpdateABetweenSolves (boolean enable) {
      myUpdateABetweenSolves = enable;
   }

   public boolean getUpdateABetweenSolves () {
      return myUpdateABetweenSolves;
   }

   /**
    * Rebuild the A matrix if sizeND/myAconsData.size() {@code <=} r.
    */
   public void setRebuildARatio (double r) {
      myRebuildARatio = r;
   }

   public double getRebuildARatio () {
      return myRebuildARatio;
   }

   public MurtyMechSolver() {
      myHCholesky = new SignedCholeskyDecomp();
      myHCholesky.ensureCapacity(100);
      myH = new SparseCRSMatrix();
      myBh = new VectorNd();
      myHPivots = new ArrayList<>(); 
      myE = new VectorNi();
      myBe = new VectorNd();
      myEPivots = new ArrayList<>(); 
      // myInitialToPivotedIdxs = new int[0];
      myB = new VectorNd();
      myX = new VectorNd();      
      myY = new VectorNd();

      myAnalyzeTimer = new FunctionTimer();
      myFactorTimer = new FunctionTimer();
      mySolveTimer = new FunctionTimer();
      
      myStateN = new int[0];
      myJRowN = new int[0];
      myAConsN = new AConstraintData[0];
      myWn = new VectorNd();

      myStateD = new int[0];
      myJRowD = new int[0];
      myAConsD = new AConstraintData[0];
      myAConsDFull = myAConsD;
      myWd = new VectorNd();
      
      myAConsData = new ArrayList<>();
      myVersionM = -1;
   }

   public void setSolverType (SparseSolverId solverType) {
      switch (solverType) {
         case Pardiso: {
            myPardiso = new PardisoSolver();
            myMatrixSolver = myPardiso;
            break;
         }
         case Umfpack: {
            myUmfpack = new UmfpackSolver();
            myMatrixSolver = myUmfpack;
            break;
         }
         default: {
            throw new IllegalArgumentException (
               "Solver type " + solverType + " not supported");
         }
      }
      mySolverType = solverType;
   }

   public void setSolver (DirectSolver solver) {
      if (solver instanceof PardisoSolver) {
         myPardiso = (PardisoSolver)solver;
         mySolverType = SparseSolverId.Pardiso;
      }
      else if (solver instanceof UmfpackSolver) {
         myUmfpack = (UmfpackSolver)solver;
         mySolverType = SparseSolverId.Umfpack;
      }
      else {
         throw new UnsupportedOperationException ("Unsupported solver "+solver);
      }
      myMatrixSolver = solver;
   }

   void initializeSolverIfNecessary() {
      if (myMatrixSolver == null) {
         setSolverType (SparseSolverId.Pardiso);
      }
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

   public boolean getBlockPivoting() {
      return myDefaultBlockPivoting;
   }

   public void setBlockPivoting (boolean enable) {
      myDefaultBlockPivoting = enable;
   }

   public boolean getLastBlockPivoting() {
      return myBlockPivoting;
   }

   public double getLastSolveTol() {
      return myTol;
   }
   
   public int getLastVersionM() {
      return myVersionM;
   }

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
    * Returns the number of iterations that were used in the most recent
    * solution operation.
    * 
    * @return iteration count for last solution
    */
   public int getIterationCount() {
      return myIterationCnt;
   }

   /**
    * Returns the number of pivots that were used in the most recent solution
    * operation. If block pivoting is enabled, this will usually exceed the
    * number of iterations. Otherwise, this number should be one less than the
    * number of iterations.
    * 
    * @return pivot count for last solution
    */
   public int getPivotCount() {
      return myPivotCnt;
   }

   /**
    * Returns the number of times that block pivoting failed during the most
    * recent solve. This may be more than 1 if attempts to retart block
    * pivoting also fail.
    * 
    * @return block pivot failure count
    */
   public int getBlockPivotFailCount() {
      return myBlockPivotFailCnt;
   }

   /**
    * Returns the number of solves of A that were used in the most recent
    * solution operation.
    * 
    * @return solve count for last solution
    */
   public int getSolveCount() {
      return mySolveCnt;
   }

   /**
    * Queries whether hybrid solves are enabled. See {@link #setHybridSolves}.
    *
    * @return {@code true} if hybrid solves are enabled.
    */
   public boolean getHybridSolves() {
      return myHybridSolves;
   }

   /**
    * Returns the current count of hybrid solve solutions. This count is
    * increased whenever a solution is computed using hybrid solves, and reset
    * to 0 whenever it is not.
    *
    * @return current hybrid solve count
    */
   public int getHybridSolveCount() {
      return myHybridCnt;
   }

   /**
    * Enables hybrid solves. If enabled, the sparse solver will attempt to use
    * a hybrid direct/iterative method for solving the A matrix, which can
    * result in a significant performance improvement. However, hybrid solves
    * can only be called in limited situtations where no pivoting is
    * anticipated (i.e., the N and D matrices are null).
    *
    * @param enable if {@code true}, enables hybrid solves
    */
   public void setHybridSolves (boolean enable) {
      myHybridSolves = enable;
   }

   /**
    * Queries whether the activity of {@code NT} is fixed during friciton
    * solves. See {@link #setNTFrictionActivity}.
    *
    * @return {@code true} if the activity of {@code NT} is enabled
    * during friction solves
    */
   public boolean getNTFrictionActivity() {
      return myNTFrictionActivity;
   }

   /**
    * Enables or disables activity of the {@code NT} matrix during friction
    * solves, which occur in the friction iterations associated with {@code
    * solve()} calls. If actvity is disabled, the activity state of {@code NT}
    * will not be changed, and so those constraints will be treated as
    * bilateral. This will typically reduce the number of required pivots and
    * allow a faster solution, at the expense of some accuracy. The resulting
    * values of {@code the} may also be negative. When updating friction
    * limits, negative values of {@code NT} will be clipped to {@code 0}.
    *
    * <p>The default value of this setting is {@code true}.
    *
    * @param enable if {@code false}, disables the activity of {@code NT}
    * during friction solves
    */
   public void setNTFrictionActivity (boolean enable) {
      myNTFrictionActivity = enable;
   }

   public boolean isBLCPSupported() {
      return true;
   }

   int[] getBlockRowIndices (SparseBlockMatrix XT, int j, int blkSizeM) {
      int bj = XT.getBlockCol(j);
      MatrixBlock blk = XT.firstBlockInCol(bj);
      DynamicIntArray blockRowIdxs = new DynamicIntArray();
      while (blk != null) {
         int bi = blk.getBlockRow();
         if (bi >= blkSizeM) {
            break;
         }
         blockRowIdxs.add (bi);
         blk = blk.down();
      }
      return blockRowIdxs.getArray();      
   }

   protected void initializeState (VectorNi state) {
      int basisChangeCnt = 0;

      myAConsN = Arrays.copyOf (myAConsN, mySizeN);
      if (mySizeN > 0) {
         for (int i=0; i<mySizeN; i++) {
            int sval = (state != null ? state.get(i) : W_LO);
            if ((myAConsN[i] == null) != (sval != Z)) {
               basisChangeCnt++;
            }
            myStateN[i] = sval;
         }
      }
      myAConsD = Arrays.copyOf (myAConsD, mySizeD); // XXX
      if (!myContactSolveP) {
         myAConsDFull = myAConsD;
        if (myUpdateABetweenSolves) {
           myAConsDFull = myAConsD;
        }
        else {
           myAConsDFull = new AConstraintData[0];
        }
        myLastDTFull = myDT;
        myLastRdFull = myRd;
      }
      if (mySizeD > 0) {
         for (int i=0; i<mySizeD; i++) {
            int sval = (state != null ? state.get(mySizeN+i) : W_LO);
            if (myFlim.get(i) == 0 && sval != W_LO) {
               sval = W_LO;
            }
            if ((myAConsD[i] == null) != (sval != Z)) {
               basisChangeCnt++;
            }
            myStateD[i] = sval;
         }
      }
      if (basisChangeCnt > 0) {
         if (!myAStructureChanged) {
            if (showAStructureChange) System.out.println ("rebuild: basis");
            myAStructureChanged = true;
         }
      }
   }

   protected void initializeState (VectorNi stateN, VectorNi stateD) {
      int basisChangeCnt = 0;

      myAConsN = Arrays.copyOf (myAConsN, mySizeN);
      if (mySizeN > 0) {
         for (int i=0; i<mySizeN; i++) {
            int sval = (stateN != null ? stateN.get(i) : W_LO);
            if ((myAConsN[i] == null) != (sval != Z)) {
               basisChangeCnt++;
            }
            myStateN[i] = sval;
         }
      }
      myAConsD = Arrays.copyOf (myAConsD, mySizeD); // XXX
      if (!myContactSolveP) {
         myAConsDFull = myAConsD;
        if (myUpdateABetweenSolves) {
           myAConsDFull = myAConsD;
        }
        else {
           myAConsDFull = new AConstraintData[0];
        }
        myLastDTFull = myDT;
        myLastRdFull = myRd;
      }
      if (mySizeD > 0) {
         for (int i=0; i<mySizeD; i++) {
            int sval = (stateD != null ? stateD.get(i) : W_LO);
            if (myFlim.get(i) == 0 && sval != W_LO) {
               sval = W_LO;
            }
            if ((myAConsD[i] == null) != (sval != Z)) {
               basisChangeCnt++;
            }
            myStateD[i] = sval;
         }
      }
      if (basisChangeCnt > 0) {
         if (!myAStructureChanged) {
            if (showAStructureChange) System.out.println ("rebuild: basis");
            myAStructureChanged = true;
         }
      }
   }

   /**
    * Solves the A system and places the solution in {@code myY}. This needs to
    * be done when the A system is first built, and then whenever the set of D
    * constraints for which stateD[i] != Z and stateDA[i] != Z changes, which
    * happens whenever we add or remove an H row involving D.
    */
   void solveForY() {
      boolean hasFrictionOffset = false;
      if (mySizeD > 0) {
         // initialize phi with non-basic values so we can solve for the
         // friction force offset
         VectorNd phi = new VectorNd(mySizeD);
         for (int i=0; i<mySizeD; i++) {
            if (myStateD[i] != Z && myAConsD[i] == null) {
               if (myStateD[i] == W_LO) {
                  phi.set (i, -myFlim.get(i));
               }
               else { // state == W_HI
                  phi.set (i, myFlim.get(i));
               }
               hasFrictionOffset = true;
            }
         }
         if (hasFrictionOffset) {
            myX.setZero();
            myDT.mul (myX, phi, mySizeM, myDT.colSize());
         }
      }
      if (hasFrictionOffset) {
         if (debug) {
            System.out.println ("O = " + myX.toString("%11.8f"));
         }
         myX.add (myB, myX);
         solveA (myY, myX); 
         if (debug) {
            System.out.println ("X = " + myX.toString("%11.8f"));
            System.out.println ("Y = " + myY.toString("%11.8f"));
         }
     }
      else {
         if (debug) {
            System.out.println ("B = " + myB.toString("%11.8f"));
            System.out.println ("Y = " + myY.toString("%11.8f"));
         }
         solveA (myY, myB);
      }
   }

   private void analyzeA (int[] colIdxs) {
      if (mySolverType == SparseSolverId.Pardiso) {
         int[] rowOffs = Arrays.copyOf (myRowOffsA, mySizeA+1);
         for (int i=0; i<rowOffs.length; i++) {
            rowOffs[i]++;
         }
         for (int i=0; i<colIdxs.length; i++) {
            colIdxs[i]++;
         }
         myAColIdxs = colIdxs;
         myARowOffs = rowOffs;
         //getAValues (null, true);
         myAnalyzeTimer.restart();
         myPardiso.analyze (
            myValuesA, colIdxs, rowOffs, mySizeA, Matrix.SYMMETRIC);
         if (myPardiso.getState() == PardisoSolver.UNSET) {
            throw new NumericalException (
               "Pardiso: unable to analyze matrix: " +
               myPardiso.getErrorMessage());
         }
         myAnalyzeTimer.stop();
         //getAValues (null, false);
         myTotalAnalyzeCnt++;
         myHybridCnt = 0;
         myAvgDirectTime = 0;
         myAMatrixFactored = false;
      }
      else {
         throw new UnsupportedOperationException (
            "Solver " + mySolverType + " is not supported");
      }
   }

   public MatrixNd getA() {
      if (myValuesA != null && myAColIdxs != null && myARowOffs != null) {
         MatrixNd A = new MatrixNd(mySizeA, mySizeA);
         A.setCRSValues (
            myValuesA, myAColIdxs, myARowOffs, myNumValsA, mySizeA,
            Partition.UpperTriangular);
         return A;
      }
      else {
         return null;
      }
   }

   public void writeA (String filename, Matrix.WriteFormat wfmt) {
      PrintWriter pw = null;
      try {
         pw = new PrintWriter (
            new BufferedWriter (new FileWriter (filename)));
         MatrixNd A = getA();
         if (A != null) {
            A.write (pw, "SYMMETRIC", wfmt);
         }
         pw.close();
      }
      catch (IOException e) {
         e.printStackTrace(); 
      }
      finally {
         if (pw != null) {
            pw.close ();
         }
      }
   }

   private void factorA () {
      if (mySolverType == SparseSolverId.Pardiso) {
         myFactorTimer.restart();

         myPardiso.factor (myValuesA);
         if (myPardiso.getState() != PardisoSolver.FACTORED) {
            throw new NumericalException (
               "Pardiso: unable to factor matrix: size="+mySizeA+", nnz=" +
               myNumValsA + ", error=" + myPardiso.getErrorMessage());
         }
         myFactorTimer.stop();
         myTotalFactorCnt++;
         myAMatrixFactored = true;
      }
      else {
         throw new UnsupportedOperationException (
            "Solver " + mySolverType + " is not supported");
      }
   }

   private void solveA (VectorNd y, VectorNd x) {
      if (mySolverType == SparseSolverId.Pardiso) {
         mySolveTimer.restart();
         myPardiso.solve (y, x);
         mySolveTimer.stop();
         mySolveCnt++;
         myTotalSolveCnt++;
      }
      else {
         throw new UnsupportedOperationException (
            "Solver " + mySolverType + " is not supported");
      }
   }

   private void solveA (MatrixNd Y, MatrixNd X) {
      if (mySolverType == SparseSolverId.Pardiso) {
         mySolveTimer.restart();
         int nrows= Y.rowSize(); 
         myPardiso.solve (Y.getBuffer(), X.getBuffer(), nrows);
         mySolveTimer.stop();
         mySolveCnt += nrows;
         myTotalSolveCnt += nrows;
      }
      else {
         throw new UnsupportedOperationException (
            "Solver " + mySolverType + " is not supported");
      }
   }

   private boolean canDoHybridSolve() {
      if (myHybridSolves && myPardiso != null &&
          mySizeND == 0 && myAvgDirectTime > 0 && !myAStructureChanged) {
         return (myAvgHybridTime < myHybridRatio*myAvgDirectTime);
      }
      return false;
   }

   private double updateAvgTime (double tnew, double tavg) {
      if (tavg == 0) {
         return tnew;
      }
      else {
         double s = myTimingWeight;
         return s*tnew + (1-s)*tavg;
      }
   }

   boolean hybridSolveA() {
      myTimer.start();
      int status = myPardiso.iterativeSolve (
         myValuesA, myY.getBuffer(), myB.getBuffer(), myHybridSolveTol);
      myTimer.stop();
      if (status > 0 && !myFakeHybridFail) {
         myAvgHybridTime = updateAvgTime (myTimer.getTimeUsec(), myAvgHybridTime);
         myHybridCnt++;
         //System.out.println ("hybrid");
         return true;
      }
      else {
         return false;
      }
   }

   void extractMGSolution (VectorNd vel, VectorNd lam) {
      // extract vel and lam from x
      int iy = 0;
      for (int i=0; i<mySizeM; i++) {
         vel.set (i, myY.get(iy++));
      }
      if (lam != null) {
         for (int i=0; i<mySizeG; i++) {
            lam.set (i, -myY.get(iy++));
         }
      }
   }

   void startDirectSolveTiming() {
      myTimer.start();
   }

   void stopDirectSolveTiming () {
      myTimer.stop();
      myHybridCnt = 0;
      myAvgHybridTime = 0;
      myAvgDirectTime = updateAvgTime (myTimer.getTimeUsec(), myAvgDirectTime);
   }

   int myCnt = 0;

   FunctionTimer idxTimer = new FunctionTimer();
   FunctionTimer altTimer = new FunctionTimer();
   int idxCnt = 0;

   protected int[] rebuildOrUpdateA(ArrayList<Pivot> pivots, VectorNi state) {

      myCnt++;

      //myHasPrevNIdxs = (prevNIdxsVec != null);

      boolean rebuildA = !myUpdateABetweenSolves;
      if (state == null) {
         //System.out.println ("state==null");
         rebuildA = true;
      }
      else if (myTotalAnalyzeCnt == 0) {
         rebuildA = true;
      }
      else if (myAStructureChanged) {
         //System.out.println ("Achanged");
         rebuildA = true;
      }
      else if (!myContactSolveP && mySizeND/(double)myAConsData.size() <= myRebuildARatio) {
         rebuildA = true;
      }
      

      int[] prevDIdxs = null;
      int[] prevNIdxs = null;
      AConstraintData[] prevAConsD = null;
      if (!rebuildA && mySizeD > 0) {
         prevAConsD = myAConsDFull;
         myAConsD = new AConstraintData[mySizeD];
         if (!myContactSolveP) {
            myAConsDFull = myAConsD;
            myLastDTFull = myDT;
            myLastRdFull = myRd;
         }
      }

      if (state != null) {
         if (state.size() == 0) {
            state.setSize (mySizeND);
            LCPSolver.clearState (state);
         }
         else if (state.size() < mySizeND) {
            throw new IllegalArgumentException (
               "state size="+state.size()+" when ND size="+mySizeND);
         }
      }

      SparseBlockSignature prevNTSignature = myNTSignature;
      myNTSignature =
         (myNT != null ? new SparseBlockSignature (myNT, /*vertical=*/true) : null);

      
      if (!rebuildA) {
         if (mySizeN > 0 && prevNTSignature != null) {
            prevNIdxs = myNTSignature.computePrevColIdxs(prevNTSignature);
         }
      }
      else {
         if (!myAStructureChanged &&
             !signaturesEqual (myNTSignature, prevNTSignature)) {
            if (showAStructureChange) System.out.println ("rebuild: N changed");
            myAStructureChanged = true;
         }         
      }
      
      if (!myContactSolveP) {
         SparseBlockSignature prevDTSignature = myDTSignature;
         myDTSignature =
            (myDT != null ? new SparseBlockSignature (myDT, /*vertical=*/true) : null);

         if (!rebuildA) {
            if (mySizeD > 0 && prevDTSignature != null) {
               prevDIdxs = myDTSignature.computePrevColIdxs (prevDTSignature);
            }
         }
         else {
            if (!myAStructureChanged &&
                !signaturesEqual (myDTSignature, prevDTSignature)) {
               if (showAStructureChange) System.out.println ("rebuild: D change");
               myAStructureChanged = true;
            }
         }
      }
      
      if (!rebuildA) {
         updateA (pivots, state, prevNIdxs, prevDIdxs, prevAConsD);
         int nump = pivots.size();
         // decide whether to update or rebuild
         //System.out.println ("nump=" + nump);
         if (!myAdaptivelyRebuildA ||
            nump*getAvgSolveTime() < 0.5*getAvgAnalyzeTime()) {
            //System.out.println ("UPDATE");
            return null;
         }
      }

      initializeState (state);
      pivots.clear();

      return buildA();
   }

   protected int[] rebuildOrUpdateA (
      ArrayList<Pivot> pivots, VectorNi stateN, VectorNi stateD) {

      myCnt++;

      //myHasPrevNIdxs = (prevNIdxsVec != null);

      boolean rebuildA = !myUpdateABetweenSolves;
      if (stateN == null && stateD == null) {
         //System.out.println ("stateN and stateD are null");
         rebuildA = true;
      }
      else if (myTotalAnalyzeCnt == 0) {
         rebuildA = true;
      }
      else if (myAStructureChanged) {
         //System.out.println ("Achanged");
         rebuildA = true;
      }
      else if (!myContactSolveP && mySizeND/(double)myAConsData.size() <= myRebuildARatio) {
         rebuildA = true;
      }
      

      int[] prevDIdxs = null;
      int[] prevNIdxs = null;
      AConstraintData[] prevAConsD = null;
      if (!rebuildA && mySizeD > 0) {
         prevAConsD = myAConsDFull;
         myAConsD = new AConstraintData[mySizeD];
         if (!myContactSolveP) {
            myAConsDFull = myAConsD;
            myLastDTFull = myDT;
            myLastRdFull = myRd;
         }
      }

      if (stateN != null) {
         if (stateN.size() == 0) {
            stateN.setSize (mySizeN);
            LCPSolver.clearState (stateN);
         }
         else if (stateN.size() < mySizeN) {
            throw new IllegalArgumentException (
               "stateN size="+stateN.size()+" when N size="+mySizeN);
         }
      }
      if (stateD != null) {
         if (stateD.size() == 0) {
            stateD.setSize (mySizeD);
            LCPSolver.clearState (stateD);
         }
         else if (stateD.size() < mySizeD) {
            throw new IllegalArgumentException (
               "stateD size="+stateD.size()+" when D size="+mySizeD);
         }
      }

      SparseBlockSignature prevNTSignature = myNTSignature;
      myNTSignature =
         (myNT != null ? new SparseBlockSignature (myNT, /*vertical=*/true) : null);

      
      if (!rebuildA) {
         if (mySizeN > 0 && prevNTSignature != null) {
            prevNIdxs = myNTSignature.computePrevColIdxs(prevNTSignature);
         }
      }
      else {
         if (!myAStructureChanged &&
             !signaturesEqual (myNTSignature, prevNTSignature)) {
            if (showAStructureChange) System.out.println ("rebuild: N changed");
            myAStructureChanged = true;
         }         
      }
      
      if (!myContactSolveP) {
         SparseBlockSignature prevDTSignature = myDTSignature;
         myDTSignature =
            (myDT != null ? new SparseBlockSignature (myDT, /*vertical=*/true) : null);

         if (!rebuildA) {
            if (mySizeD > 0 && prevDTSignature != null) {
               prevDIdxs = myDTSignature.computePrevColIdxs (prevDTSignature);
            }
         }
         else {
            if (!myAStructureChanged &&
                !signaturesEqual (myDTSignature, prevDTSignature)) {
               if (showAStructureChange) System.out.println ("rebuild: D change");
               myAStructureChanged = true;
            }
         }
      }
      
      if (!rebuildA) {
         updateA (pivots, stateN, prevNIdxs, stateD, prevDIdxs, prevAConsD);
         int nump = pivots.size();
         // decide whether to update or rebuild
         //System.out.println ("nump=" + nump);
         if (!myAdaptivelyRebuildA ||
            nump*getAvgSolveTime() < 0.5*getAvgAnalyzeTime()) {
            //System.out.println ("UPDATE");
            return null;
         }
      }

      initializeState (stateN, stateD);
      pivots.clear();

      return buildA();
   }

   protected void updateA (
      ArrayList<Pivot> pivots, VectorNi state,
      int[] prevNIdxs, int[] prevDIdxs,
      AConstraintData[] prevAConsD) {

      if (prevNIdxs != null && prevNIdxs.length < mySizeN) {
         throw new IllegalArgumentException (
            "prevNIdxs.length="+prevNIdxs.length+" when N size="+mySizeN);
      }
      
      for (AConstraintData acons : myAConsData) {
         if (acons.myType == TYPE_D) {
            if (myContactSolveP) {
               acons.setContactMask (true);
            }
            else {
               acons.setContactMask (false);
               acons.setRemovedMask (true);
            }
         }
         else {
            acons.setRemovedMask(true); // will unmask below if used
         }
      }

      int prevSizeN = myAConsN.length;
      AConstraintData[] prevAConsN = Arrays.copyOf (myAConsN, prevSizeN);
      myAConsN = new AConstraintData[mySizeN];

      int AIdxMax = mySizeA - mySizeMG;
      
      pivots.clear();
      for (int i=0; i<mySizeN; i++) {
         int previ = prevNIdxs[i];
         if (previ >= prevSizeN) {
            throw new IllegalArgumentException (
               "prevNIdxs["+i+"]="+previ+
               "; exceeds previous N size "+prevSizeN);
         }
         int sval = state.get(i);         
         if (previ == -1) {
            // constraint is new, and so is not in A
            if (sval == Z) {
               pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_N));
            }
            myAConsN[i] = null;
         }
         else {
            AConstraintData acons = prevAConsN[previ];
            if (acons != null) {
               // update A matrix constraint
               acons.myCol = i;
               acons.setRemovedMask (false);
               myAConsN[i] = acons;
            }
            else {
               myAConsN[i] = null;
            }
            if (sval == Z) {
               if (acons == null) {
                  // constraint is not in A
                  pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_N));
               }
               else {
                  // use existing A constraint
               }
            }
            else {
               if (acons == null) {
                  // constraint is not in A; nothing to do
               }
               else {
                  // pivot to remove constraint
                  pivots.add (new Pivot (i, acons.myIdx, Z, W_LO, TYPE_N));
               }
            }
         }
         myStateN[i] = sval;
         myJRowN[i] = -1;
      }

      for (int i=0; i<mySizeD; i++) {
         int previ = prevDIdxs[i];
         int sval = state.get(mySizeN+i);   
         if (myFlim.get(i) == 0 && sval != W_LO) {
            sval = W_LO;
         }
         if (previ == -1) {
            // constraint is new, and so is not in A
            if (sval == Z) {
               pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_D));
            }
            myAConsD[i] = null;
         }
         else {
            AConstraintData acons;
            if (previ >= prevAConsD.length ){
               acons = null;
            }
            else {
               acons = prevAConsD[previ];
            }
            if (acons != null) {
               // update A matrix constraint
               acons.myCol = i;
               acons.setRemovedMask(false);
               myAConsD[i] = acons;
            }
            else {
               myAConsD[i] = null;
            }
            if (sval == Z) {
               if (acons == null) {
                  // constraint is not in A
                  pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_D));
               }
               else {
                  // use existing A constraint
               }
            }
            else {
               if (acons == null) {
                  // constraint is not in A; nothing to do
               }
               else {
                  if (acons.myIdx >= AIdxMax) {
                     throw new IndexOutOfBoundsException (
                        "acons.myIdx=" + acons.myIdx +", max=" + AIdxMax);
                  }
                  // pivot to remove constraint
                  pivots.add (new Pivot (i, acons.myIdx, Z, sval, TYPE_D));
               }
            }
         }
         myStateD[i] = sval;
         myJRowD[i] = -1;
      }
   }

   protected void updateA (
      ArrayList<Pivot> pivots,
      VectorNi stateN, int[] prevNIdxs,
      VectorNi stateD, int[] prevDIdxs,
      AConstraintData[] prevAConsD) {

      if (prevNIdxs != null && prevNIdxs.length < mySizeN) {
         throw new IllegalArgumentException (
            "prevNIdxs.length="+prevNIdxs.length+" when N size="+mySizeN);
      }
      
      for (AConstraintData acons : myAConsData) {
         if (acons.myType == TYPE_D) {
            if (myContactSolveP) {
               acons.setContactMask (true);
            }
            else {
               acons.setContactMask (false);
               acons.setRemovedMask (true);
            }
         }
         else {
            acons.setRemovedMask(true); // will unmask below if used
         }
      }

      int prevSizeN = myAConsN.length;
      AConstraintData[] prevAConsN = Arrays.copyOf (myAConsN, prevSizeN);
      myAConsN = new AConstraintData[mySizeN];

      int AIdxMax = mySizeA - mySizeMG;
      
      pivots.clear();
      for (int i=0; i<mySizeN; i++) {
         int previ = prevNIdxs[i];
         if (previ >= prevSizeN) {
            throw new IllegalArgumentException (
               "prevNIdxs["+i+"]="+previ+
               "; exceeds previous N size "+prevSizeN);
         }
         int sval = stateN.get(i);         
         if (previ == -1) {
            // constraint is new, and so is not in A
            if (sval == Z) {
               pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_N));
            }
            myAConsN[i] = null;
         }
         else {
            AConstraintData acons = prevAConsN[previ];
            if (acons != null) {
               // update A matrix constraint
               acons.myCol = i;
               acons.setRemovedMask (false);
               myAConsN[i] = acons;
            }
            else {
               myAConsN[i] = null;
            }
            if (sval == Z) {
               if (acons == null) {
                  // constraint is not in A
                  pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_N));
               }
               else {
                  // use existing A constraint
               }
            }
            else {
               if (acons == null) {
                  // constraint is not in A; nothing to do
               }
               else {
                  // pivot to remove constraint
                  pivots.add (new Pivot (i, acons.myIdx, Z, W_LO, TYPE_N));
               }
            }
         }
         myStateN[i] = sval;
         myJRowN[i] = -1;
      }

      for (int i=0; i<mySizeD; i++) {
         int previ = prevDIdxs[i];
         int sval = stateD.get(i);   
         if (myFlim.get(i) == 0 && sval != W_LO) {
            sval = W_LO;
         }
         if (previ == -1) {
            // constraint is new, and so is not in A
            if (sval == Z) {
               pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_D));
            }
            myAConsD[i] = null;
         }
         else {
            AConstraintData acons;
            if (previ >= prevAConsD.length ){
               acons = null;
            }
            else {
               acons = prevAConsD[previ];
            }
            if (acons != null) {
               // update A matrix constraint
               acons.myCol = i;
               acons.setRemovedMask(false);
               myAConsD[i] = acons;
            }
            else {
               myAConsD[i] = null;
            }
            if (sval == Z) {
               if (acons == null) {
                  // constraint is not in A
                  pivots.add (new Pivot (i, -1, W_LO, Z, TYPE_D));
               }
               else {
                  // use existing A constraint
               }
            }
            else {
               if (acons == null) {
                  // constraint is not in A; nothing to do
               }
               else {
                  if (acons.myIdx >= AIdxMax) {
                     throw new IndexOutOfBoundsException (
                        "acons.myIdx=" + acons.myIdx +", max=" + AIdxMax);
                  }
                  // pivot to remove constraint
                  pivots.add (new Pivot (i, acons.myIdx, Z, sval, TYPE_D));
               }
            }
         }
         myStateD[i] = sval;
         myJRowD[i] = -1;
      }
   }

   protected int numMaskedA () {
      int n = 0;
      for (AConstraintData acons : myAConsData) {
         if (acons.isMasked()) {
            n++;
         }
      }
      return n;
   }

   protected int numMaskedNA () {
      int n = 0;
      for (AConstraintData acons : myAConsData) {
         if (acons.isMasked() && acons.myType == TYPE_N) {
            n++;
         }
      }
      return n;
   }

   protected int numMaskedDA () {
      int n = 0;
      for (AConstraintData acons : myAConsData) {
         if (acons.isMasked() && acons.myType == TYPE_D) {
            n++;
         }
      }
      return n;
   }

   protected void updateAndSolveA (VectorNi stateN, VectorNi stateD) {
      initializeSolverIfNecessary();

      ArrayList<Pivot> pivots = new ArrayList<>();
      int[] colIdxs = rebuildOrUpdateA (pivots, stateN, stateD);

      //System.out.println ("numCons=" + myAConsData.size());
      
      checkConsistency();
      buildRhs();
      // getAValues (null, /*forAnalyze=*/false); 

      boolean solved = false;
      boolean valuesUpdated = false;
      if (canDoHybridSolve()) {
         getAValues (null, /*forAnalyze=*/false); 
         valuesUpdated = true;
         solved = hybridSolveA();
      }
      if (!solved) {
         if (myAStructureChanged) {
            getAValues (null, /*forAnalyze=*/true); 
            analyzeA (colIdxs);
            //System.out.println ("analyze " + mySizeA);
         }
         if (!valuesUpdated) {
            getAValues (null, /*forAnalyze=*/false); 
         }
         startDirectSolveTiming();
         factorA();
         solveForY();
         stopDirectSolveTiming();
         //System.out.println ("direct");
      }
      if (mySizeND > 0) {
         // clear J
         myH.setSize (0, mySizeMG);
         myBh.setSize (0);
         myHPivots.clear();
         myE.setSize (0);
         myBe.setSize (0);
         myEPivots.clear();
         myHCholesky.clear();
      }

      if (debug) {
         System.out.println ("A=\n" + getA().toString ("%11.8f"));
         System.out.println ("b=\n" + myB.toString ("%11.8f"));
         if (myNT != null) {
            if (myNT.numBlockCols() == 0) {
               System.out.println ("NT=[]\n");
            }
            else {
               System.out.println ("NT=\n" + myNT.toString ("%11.8f"));
            }
         }
         if (myDT != null) {
            System.out.println ("DT=\n" + myDT.toString ("%11.8f"));
         }
      }
      
      if (pivots.size() > 0) {
         double estTime = pivots.size()*getAvgSolveTime();
         // System.out.println ("initial pivots:");
         // for (Pivot piv : pivots) {
         //    System.out.println ("  " + piv);
         // }
         applyBlockPivots (pivots);
      }
   }

   /**
    * Get column indices for A
    */
   protected int[] getAColumnIndices (int[] localOffs) {
      
      int[] colIdxs = new int[myNumValsA];

      // Reset localOffs so we tranverse the rows of [M GT NT DT]
      for (int i=0; i<mySizeM; i++) {
         localOffs[i] = myRowOffsA[i];
      }
      // Get column indices for [ M GT NT DT ]
      int colOffset = 0;
      myM.getBlockCRSIndices (
         colIdxs, colOffset, localOffs, Partition.UpperTriangular,
         mySizeM, mySizeM);
      colOffset += mySizeM;
      if (mySizeG > 0) {
         myGT.getBlockCRSIndices (
            colIdxs, colOffset, localOffs, Partition.Full, mySizeM, mySizeG);
      }
      int j = mySizeMG;
      for (AConstraintData acons : myAConsData) {
         acons.getCRSIndices (colIdxs, localOffs, myM, j++);
      }
      // Get column indices for diagonal blocks
      for (int i=mySizeM; i<mySizeA; i++) {
         colIdxs[myRowOffsA[i]] = i;
      }
      return colIdxs;
   }

   void getAValues (int[] localOffs, boolean forAnalyze) {
      
      if (localOffs == null) {
         localOffs = new int[mySizeM];
      }
      // set localOffs so we tranverse the rows of [M GT NT DT]
      for (int i = 0; i < mySizeM; i++) {
         localOffs[i] = myRowOffsA[i];
      }
      myM.getBlockCRSValues (
         myValuesA, localOffs, Partition.UpperTriangular, mySizeM, mySizeM);
      int doff = mySizeM; // offset for lower diagonal elements
      if (mySizeG > 0) {
         myGT.getBlockCRSValues (
            myValuesA, localOffs, Partition.Full, mySizeM, mySizeG);
         // lower diagonal block
         for (int i=0; i<mySizeG; i++) {
            myValuesA[myRowOffsA[doff++]] = (myRg != null ? -myRg.get(i) : 0);
         }
      }
      for (AConstraintData acons : myAConsData) {
         if (acons.isMasked()) {
            if (acons.myType == TYPE_D && acons.getContactMask() && forAnalyze) {
               //acons.getCRSFixedValue (myValuesA, localOffs, 1, myM);
               acons.getCRSValues (myValuesA, localOffs, myLastDTFull,myBlkSizeM);
               myValuesA[myRowOffsA[doff++]] =
                  (myLastRdFull != null ? -myLastRdFull.get(acons.myCol) : 0);
            }
            else {
               acons.getCRSZeros (myValuesA, localOffs, myM);
               myValuesA[myRowOffsA[doff++]] = 1.0;
            }
         }
         else if (acons.myType == TYPE_N) {
            acons.getCRSValues (myValuesA, localOffs, myNT, myBlkSizeM);
            myValuesA[myRowOffsA[doff++]] =
               (myRn != null ? -myRn.get(acons.myCol) : 0);
         }
         else if (acons.myType == TYPE_D) {
            acons.getCRSValues (myValuesA, localOffs, myDT, myBlkSizeM);
            myValuesA[myRowOffsA[doff++]] =
               (myRd != null ? -myRd.get(acons.myCol) : 0);
         }
      }
   }

   private AConstraintData addAConstraint (
      ConstraintType type, SparseBlockMatrix XT, int col) {
      
      AConstraintData acons = 
         new AConstraintData (type, col, myAConsData.size());
      acons.myBlockRowIdxs = getBlockRowIndices (XT, col, myBlkSizeM);
      myAConsData.add (acons);   
      return acons;
   }
   
   protected int[] buildA () {
      // A is the whole system composed of M, GT, NT, DT and the optional lower
      // diagonal blocks. We first need to analyze and factor A.
      // find number of active columns in NT and DT;
      mySizeNA = 0;
      int s;
      for (int j=0; j<mySizeN; j++) {
         if ((s=myStateN[j]) == Z) {
            mySizeNA++;
         }
         myJRowN[j] = -1;
      }
      ArrayList<AConstraintData> dConsData = null;
      if (myContactSolveP && myUpdateABetweenSolves/* && myHasPrevNIdxs*/) {
         dConsData = new ArrayList<>();
         for (AConstraintData acons : myAConsData) {
            if (acons.myType == TYPE_D && !acons.getRemovedMask()) {
               acons.setContactMask (true);
               dConsData.add (acons);
            }
         }
         mySizeDA = dConsData.size();
      }
      else {
         mySizeDA = 0;
         for (int j=0; j<mySizeD; j++) {
            if ((s=myStateD[j]) == Z) {
               mySizeDA++;
            }
            myJRowD[j] = -1;
         }
      }
      mySizeA = mySizeMG + mySizeNA + mySizeDA;

      myX.setSize (mySizeA);
      myY.setSize (mySizeA);    

      // localOffs stores indice offsets as we collect column indices and
      // values across the rows of [ M GT NT DT ]
      int[] localOffs = new int[mySizeM];
      int[] colIdxs = null; // needed only if structure changed

      if (myAStructureChanged || (dConsData != null && dConsData.size() > 0)) {
         // allocate space for row offsets
         if (myRowOffsA == null || myRowOffsA.length < mySizeA + 1) {
            myRowOffsA = new int[mySizeA + 1];
         }
         myAConsData.clear();
         for (int j=0; j<mySizeN; j++) {
            if (myStateN[j] == Z) {
               myAConsN[j] = addAConstraint (TYPE_N, myNT, j);
            }
            else {
               myAConsN[j] = null;
            }
         }
         for (int j=0; j<mySizeD; j++) {
            if (myStateD[j] == Z) {
               myAConsD[j] = addAConstraint (TYPE_D, myDT, j);
            }
            else {
               myAConsD[j] = null;
            }
         }
         if (dConsData != null) {
            for (AConstraintData acons : dConsData) {
               acons.myIdx = myAConsData.size();
               myAConsData.add (acons);
            }
            if (dConsData.size() > 0) {
               //System.out.println ("dconsdata.size=" + dConsData.size());
            }
         }
               
         // Find number of non zeros (nzz) in the upper triangular portion of A.
         // This includes the lower diagonal blocks, which must be set even if
         // they are zero.
         int nnz = myM.numNonZeroVals(Partition.UpperTriangular,mySizeM,mySizeM);
         if (mySizeG > 0) {
            nnz += myGT.numNonZeroVals(Partition.Full, mySizeM, mySizeG)+mySizeG;
         }
         for (AConstraintData acons : myAConsData) {
            nnz += acons.numNonZeros (myM);
         }
         // lower diagonal blocks
         nnz += mySizeNA;
         nnz += mySizeDA;
         myNumValsA = nnz;

         if (myValuesA == null || myValuesA.length < nnz) {
            myValuesA = new double[nnz];
         }

         // Row offsets for A:
 
         // First find the number of non zeros in each
         // row of [ M GT NT DT ] and store this in localOffs.
         myM.addNumNonZerosByRow (
            localOffs, 0, Partition.UpperTriangular, mySizeM, mySizeM);
         if (mySizeG > 0) {
            myGT.addNumNonZerosByRow (
               localOffs, 0, Partition.Full, mySizeM, mySizeG);
         }
         for (AConstraintData acons : myAConsData) {
            acons.addNonZerosByRow (localOffs, myM);
         }
         // Find the row offsets by adding the number of non-zeros in each row:
         int off = 0;
         for (int i=0; i<mySizeM; i++) {
            myRowOffsA[i] = off;
            off += localOffs[i];
         }
         // Compute offsets for the lower diagonal blocks
         for (int i=mySizeM; i<mySizeA; i++) {
            myRowOffsA[i] = off++;
         }
         myRowOffsA[mySizeA] = off; // off should now equal nnz

         // Column indices for A:

         colIdxs = getAColumnIndices (localOffs);
      }
      return colIdxs;
   }

   void buildRhs() {
      // initialize right-hand side b vector
      myB.setSize (mySizeA);
      int ia = 0;
      for (int i=0; i<mySizeM; i++) {
         myB.set (ia++, myBm.get(i));
      }
      for (int i=0; i<mySizeG; i++) {
         myB.set (ia++, myBg.get(i));
      }
      for (AConstraintData acons : myAConsData) {
         if (!acons.isMasked()) {
            if (acons.myType == TYPE_N) {
               myB.set (ia, myBn.get(acons.myCol));
            }
            else {
               myB.set (ia, myBd.get(acons.myCol));
            }
         }
         else {
            myB.set (ia, 0);
         }
         ia++;
      }
   }

   /**
    * Collects the non-zero row indices and values of the j-th column of a
    * constraint matrix XT.
    */
   void getNDTColumn (HRowData rowData, SparseBlockMatrix XT, int j) {

      int bj = XT.getBlockCol (j); // block column index within XT
      int jrel = j - XT.getBlockColOffset(bj); // block-relative column index
      
      DynamicIntArray colIdxs = rowData.myColIdxs;
      DynamicDoubleArray values = rowData.myValues;

      MatrixBlock blk = XT.firstBlockInCol (bj);
      while (blk != null && blk.getBlockRow() < myBlkSizeM) {
         int rowIdx = XT.getBlockRowOffset (blk.getBlockRow());
         if (blk instanceof Matrix3x1Block) {
            Matrix3x1Block mblk = (Matrix3x1Block)blk;

            colIdxs.add (rowIdx++);
            colIdxs.add (rowIdx++);
            colIdxs.add (rowIdx);

            values.add (mblk.m00);
            values.add (mblk.m10);
            values.add (mblk.m20);
         }
         else if (blk instanceof Matrix3x2Block) {
            Matrix3x2Block mblk = (Matrix3x2Block)blk;

            colIdxs.add (rowIdx++);
            colIdxs.add (rowIdx++);
            colIdxs.add (rowIdx);

            if (jrel == 0) {
               values.add (mblk.m00);
               values.add (mblk.m10);
               values.add (mblk.m20);
            }
            else {
               values.add (mblk.m01);
               values.add (mblk.m11);
               values.add (mblk.m21);
            }
         }
         else {
            int rowSize = blk.rowSize();
            for (int i=0; i<rowSize; i++) {
               colIdxs.add (rowIdx++);
               values.add (blk.get(i,jrel));
            }
         }
         blk = blk.down();
      }
   }

   void setMGVariables (
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, 
      VectorNd vel, VectorNd lam) {

      if (versionM == -1 || versionM != myVersionM || mySizeM != sizeM ||
          (myM != null &&
           (M.numBlockRows() != myM.numBlockRows() ||
            M.numBlockCols() != myM.numBlockCols()))) {
         if (!myAStructureChanged) {
            if (showAStructureChange) System.out.println ("rebuild: M changed");
            //System.out.println ("M matrix changed");
            myAStructureChanged = true;
         }
      }
      myM = M;
      mySizeM = sizeM;
      myVersionM = versionM;
      myBlkSizeM = M.getAlignedBlockRow (mySizeM);

      SparseBlockMatrix prevGT = myGT;

      myGT = GT;
      SparseBlockSignature prevGTSignature = myGTSignature;
      if (GT != null) {
         if (!GT.isVerticallyLinked()) {
            GT.setVerticallyLinked (true);
         }
         mySizeG = GT.colSize();
         myGTSignature = new SparseBlockSignature (GT, /*vertical=*/true);
      }
      else {
         mySizeG = 0;
         myGTSignature = null;
      }
      if (!myAStructureChanged &&
          !signaturesEqual (myGTSignature, prevGTSignature)) {
         if (!myAStructureChanged) {
            if (showAStructureChange) System.out.println ("rebuild: G changed");
            //System.out.println ("G matrix changed, size=" + mySizeG);
            myAStructureChanged = true;
         }
      }
      myRg = Rg;
      myBm = bm;
      myBg = bg;

      mySizeMG = mySizeM + mySizeG;

      vel.setSize (mySizeM);
      if (mySizeG > 0) {
         lam.setSize (mySizeG);
      }
   }

   /**
    * Checks to see if two matrix structure signatures are equal.
    *
    * <p>Instead of using signatures, we could also simply compare two matrices
    * using their {@code blockStructureEquals()} method, which takes about the
    * same amount of time. However, signatures give us the option of clearing
    * the matices at the end of each solve, and hence freeing up more memory.
    */
   boolean signaturesEqual (SparseBlockSignature sig0, SparseBlockSignature sig1) {
      if ((sig0 == null) != (sig1 == null)) {
         return false;
      }
      else if (sig0 != null) {
         return sig0.equals (sig1);
      }
      else {
         // both are null
         return true;
      }
   }

   void setZero (int[] array) {
      for (int i=0; i<array.length; i++) {
         array[i] = 0;
      }
   }

   void setNVariables (
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNd the) {

      myNT = NT;
      int sizeN;
      if (NT != null) {
         if (!NT.isVerticallyLinked()) {
            NT.setVerticallyLinked (true);
         }
         sizeN = NT.colSize();
      }
      else {
         sizeN = 0;
      }
      if (sizeN > mySizeN) {
         myStateN = Arrays.copyOf (myStateN, sizeN); // preserve prev state
         //myAConsN = Arrays.copyOf (myAConsN, sizeN);
         myJRowN = new int[sizeN];
      }
      myWn.setSize (sizeN);
      mySizeN = sizeN;

      myRn = Rn;
      myBn = bn;

      mySizeND = mySizeN;
      if (mySizeN > 0) {
         the.setSize (mySizeN);
      }
   }
   
   void setDVariables (
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNd phi,
      ArrayList<FrictionInfo> finfo) {

      myDT = DT;
      int sizeD;
      if (DT != null) {
         if (!DT.isVerticallyLinked()) {
            DT.setVerticallyLinked (true);
         }
         sizeD = DT.colSize();
      }
      else {
         sizeD = 0;
      }
      if (sizeD > mySizeD) {
         myStateD = Arrays.copyOf (myStateD, sizeD); // preserve prev state
         myJRowD = new int[sizeD];
      }
      
      myWd.setSize (sizeD);
      mySizeD = sizeD;

      myRd = Rd;
      myBd = bd;

      if (mySizeD > 0) {
         phi.setSize (mySizeD);
      }

      mySizeND = mySizeN + mySizeD;
   }

   protected void setFrictionLimits (VectorNd flim) {
      if (flim != null && flim.size() < mySizeD) {
         throw new IllegalArgumentException (
            "flim.size=" + flim.size()+"; must be >= "+mySizeD);
      }
      myFlim = flim;
   }   

   protected VectorNd getFrictionLimits () {
      if (myFlim == null) {
         return new VectorNd();
      }
      else {
         return new VectorNd(myFlim);
      }
   }   

   protected void updateFrictionLimits (VectorNd lam, VectorNd the) {
      if (myFlim == null) {
         myFlim = new VectorNd (mySizeD);
      }
      else {
         myFlim.setSize (mySizeD);
      }
      int k = 0;
      for (int bk=0; bk<myDT.numBlockCols(); bk++) {
         FrictionInfo info = myFrictionInfo.get(bk);

         double phiMax;
         if ((info.flags & FrictionInfo.BILATERAL) != 0) {
            phiMax = info.getMaxFriction (lam);
         }
         else {
            phiMax = info.getMaxFriction (the);
         } 
         for (int i=0; i<myDT.getBlockColSize(bk); i++) {
            myFlim.set (k++, phiMax);
         }
      }
      if (debug) {
         System.out.println ("flim=" + myFlim.toString("%11.8f"));
      }

      // need to update myBe for cases where state corresponds to D
      for (k=0; k<myE.size(); k++) {
         Pivot piv = myEPivots.get(k);
         if (piv.myType == TYPE_D) {
            int j = piv.myColIdx;
            if (piv.myNewState == W_LO) {
               myBe.set (k, myFlim.get(j));
            }
            else if (piv.myNewState == W_HI) {
               myBe.set (k, -myFlim.get(j));
            }
            else {
               throw new InternalErrorException (
                  "E pivot for D has Z target state");
            }
         }
      }
   }

   void setFrictionInfo (ArrayList<FrictionInfo> finfo) {
      if (finfo.size() < myDT.numBlockCols()) {
         throw new IllegalArgumentException (
            "finfo.size="+finfo.size()+"; must be >= "+myDT.numBlockCols());
      }
      // validate friction info 
      for (int bk=0; bk<myDT.numBlockCols(); bk++) {
         FrictionInfo info = finfo.get(bk);

         String cname;
         int csize;
         if ((info.flags & FrictionInfo.BILATERAL) != 0) {
            if (mySizeG == 0) {
               throw new IllegalArgumentException (
                  "finfo["+bk+"] references GT, but GT.colSize is 0");
            }
            cname = "GT";
            csize = mySizeG;
         }
         else {
            if (mySizeN == 0) {
               throw new IllegalArgumentException (
                  "finfo["+bk+"] references NT, but NT.colSize is 0");
            } 
            cname = "NT";
            csize = mySizeN;
         } 
         int cidx0 = info.contactIdx0;
         int cidx1 = info.contactIdx1;
         if (cidx0 < 0 || cidx0 >= csize) {
            throw new IllegalArgumentException (
               "finfo["+bk+"]: contact index 0 out of range for "+cname);
         }
         if (cidx1 > 0 && cidx1 >= csize) {
            throw new IllegalArgumentException (
               "finfo["+bk+"]: contact index 1 out of range for "+cname);
         }
      }
      myFrictionInfo = finfo;
   }

   int numJRows() {
      return myH.rowSize() + myE.size();
   }

   void solveForBasicVariables (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi) {

      int numJ = numJRows();
      VectorNd psi = new VectorNd (numJ);
      
      if (numJ > 0) {
         myX.setZero();
         VectorNd tmp = new VectorNd (numJ);
         int hsize = myH.rowSize();
         if (hsize > 0) {
            myH.mul (tmp, myY);
            for (int i=0; i<hsize; i++) {
               tmp.add (i, -myBh.get(i));
            }
         }
         if (myE.size() > 0) {
            mulE (tmp, myY, hsize);
            for (int i=0; i<myBe.size(); i++) {
               tmp.add (hsize+i, -myBe.get(i));
            }
         }

         myHCholesky.solve (psi, tmp);

         if (debug) {
            MatrixNd J = new MatrixNd (numJ, mySizeA);
            if (myH.rowSize() > 0) {
               J.setSubMatrix (0, 0, myH);
            }
            if (myE.size() > 0) {
               for (int i=0; i<myE.size(); i++) {
                  J.set (i+myH.rowSize(), myE.get(i), 1);
               }
            }
            System.out.println ("bh=" + myBh.toString ("%11.8f"));
            System.out.println ("be=" + myBe.toString ("%11.8f"));
            System.out.println ("J=\n" + J.toString ("%11.8f"));

            MatrixNd B = new MatrixNd();
            MatrixNd L = new MatrixNd();
            VectorNd D = new VectorNd();
            myHCholesky.get (L, D);
            B.set (L);
            B.mulDiagonalRight (D);
            B.mulTransposeRight (B, L);
            System.out.println ("B=\n" + B.toString ("%11.8f"));
            System.out.println ("psi=" + psi.toString ("%11.8f"));
         }
         
         // x = b - J^T phi

         if (myH.rowSize() > 0) {
            myH.mulTransposeAdd (myX, psi);
         }
         if (myE.size() > 0) {
            mulTransposeAddE (myX, psi, mySizeM, myH.rowSize());
         }
         //System.out.println ("x=\n" + myX.toString("%12.8f"));
         //System.out.println ("b=\n" + myB.toString("%12.8f"));
         //myX.sub (myB, myX);
         //System.out.println ("x=\n" + myX.toString("%12.8f"));
         // x = inv(A) x

         solveA (myX, myX);

         // System.out.println ("  SOLVE psi=" + psi.toString("%9.5f"));
         // System.out.println ("  SOLVE x=" + myX.toString("%9.5f"));
         myX.sub (myY, myX);
      }
      else {
         myX.set (myY);
      }

      if (debug) {
         System.out.println ("y = " + myY.toString("%11.8f"));
         System.out.println ("x = " + myX.toString("%11.8f"));
      }

      int ix = 0;  // index into x component of solution of the A + H system

      // extract vel and lam from x
      for (int i=0; i<mySizeM; i++) {
         vel.set (i, myX.get(ix++));
      }
      for (int i=0; i<mySizeG; i++) {
         lam.set (i, -myX.get(ix++));
      }
      // extract the and phi: parts that are still active in A

      // start by setting non-basic values, then overwrite with
      // basic values from either A or J
      if (mySizeN > 0) {
         the.setZero();
      }
      for (int i=0; i<mySizeD; i++) {
         if (myStateD[i] == W_LO) {
            phi.set (i, -myFlim.get(i));
         }
         else if (myStateD[i] == W_HI) {
            phi.set (i, myFlim.get(i));
         }
      }
      for (AConstraintData acons : myAConsData) {
         if (!acons.isMasked()) {
            if (acons.myType == TYPE_N) {
               int i = acons.myCol;
               if (myStateN[i] == Z) {
                  the.set (i, -myX.get(ix));
               }
            }
            else if (acons.myType == TYPE_D) {
               int i = acons.myCol;
               if (myStateD[i] == Z) {
                  phi.set (i, -myX.get(ix));
               }
            }
         }
         ix++;
      }     
      for (int i=0; i<myHPivots.size(); i++) {
         Pivot pivot = myHPivots.get(i);
         if (pivot.myNewState == Z) {
            if (pivot.myType == TYPE_N) {
               the.set (pivot.myColIdx, -psi.get(i));
            }
            else {
               phi.set (pivot.myColIdx, -psi.get(i));
            }
         }
      }
      if (debug) {
         System.out.println ("condJ = " + myHCholesky.eigenValueRatio());
         System.out.println (
            "numH=" + myHPivots.size() + " numE=" + myEPivots.size());
         if (lam != null) {
            System.out.println ("lam=" + lam.toString ("%14.10f"));
         }
         if (the != null) {
            System.out.println ("the=" + the.toString ("%14.10f"));
         }
         if (phi != null) {
            System.out.println ("phi=" + phi.toString ("%14.10f"));
         }
      }
   }

   /**
    * Creates and returns a dense matrix containing the values of the row
    * information contained in {@code newHRows} and {@code newERows}.
    */
   MatrixNd getNewRowMatrix (
      ArrayList<HRowData> newHRows, ArrayList<ERowData> newERows) {
      MatrixNd J = new MatrixNd (newHRows.size()+newERows.size(), mySizeA);
      double[] buf = J.getBuffer();

      int w = J.getBufferWidth();
      int rowOff = 0;
      for (int i=0; i<newHRows.size(); i++) {
         HRowData row = newHRows.get(i);
         for (int k=0; k<row.numVals(); k++) {
            int j = row.myColIdxs.get(k);
            buf[rowOff+j] = row.myValues.get(k);
         }
         rowOff += w;
      }
      for (int i=0; i<newERows.size(); i++) {
         ERowData row = newERows.get(i);
         if (row.myAIdx >= mySizeA) {
            throw new IndexOutOfBoundsException (
               " Aidx=" + row.myAIdx + " A size=" + mySizeA);
         }
         buf[rowOff+row.myAIdx] = 1;
         rowOff += w;
      }
      return J;
   }

   //MatrixNd myJSolve;
   //MatrixNd myJ;

   int applyBlockPivots (ArrayList<Pivot> pivotRequests) {

      // store new rows to be added to H
      //SparseCRSMatrix J = new SparseCRSMatrix(0, mySizeA);
      ArrayList<HRowData> newHRows = new ArrayList<>();
      ArrayList<ERowData> newERows = new ArrayList<>();

      ArrayList<Integer> removedJRows = new ArrayList<>();
      boolean resolveY = false;

      for (Pivot piv : pivotRequests) {
         int j = piv.myColIdx;
         if ((myDebug & SHOW_PIVOTS) != 0) {
            System.out.println (" BLK PIVOT " + piv.toString());
         }
         if (piv.requiresNewJRow()) {
            // add row to J
            if (piv.myNewState == Z) {
               // set row to a column from NT or DT
               HRowData newRow = new HRowData(piv);
               if (piv.myType == TYPE_N) {
                  getNDTColumn (newRow, myNT, j);
                  newRow.myRh = (myRn != null ? myRn.get(j) : 0);
                  newRow.myBh = myBn.get(j);

               }
               else { // piv.myType == TYPE_D
                  getNDTColumn (newRow, myDT, j);
                  newRow.myRh = (myRd != null ? myRd.get(j) : 0);
                  newRow.myBh = myBd.get(j);
               }
               newHRows.add (newRow);
            }
            else {
               // add row to nullify the active A
               ERowData newRow = new ERowData(piv);
               if (piv.myType == TYPE_N) {
                  newRow.myAIdx = mySizeMG + piv.myAColIdx;
                  newRow.myBe = 0;
               }
               else { // piv.myType == TYPE_D
                  newRow.myAIdx = mySizeMG + piv.myAColIdx;
                  if (piv.myNewState == W_LO) {
                     newRow.myBe = myFlim.get(j);
                  }
                  else { // piv.myNewState == W_HI
                     newRow.myBe = -myFlim.get(j);
                  }
                  resolveY = true;
               }
               newERows.add (newRow);
            }
         }
         else {
            // remove row from J
            if (piv.myType == TYPE_N) {
               removedJRows.add (myJRowN[j]);
               myStateN[j] = piv.myNewState;
               myJRowN[j] = -1;
            }
            else { // piv.myType == TYPE_D
               removedJRows.add (myJRowD[j]);
               myStateD[j] = piv.myNewState;
               myJRowD[j] = -1;
               if (myAConsD[j] == null) {
                  // must be removing an H entry
                  resolveY = true;
               }
            }
         }
      }
      Collections.sort (removedJRows);

      int reindexFrom = numJRows();
      int[] removeIdxs = ArraySupport.toIntArray (removedJRows);

      if (removeIdxs.length > 0) {
         myHCholesky.deleteRowsAndColumns (removeIdxs);
         //ArraySupport.removeListItems (myJPivots, removeIdxs);

         int hsize = myH.rowSize();
         int[] removeHRows = null;
         // remove highest rows first for greater efficiency
         for (int k=removeIdxs.length-1; k>=0; k--) {
            int idx = removeIdxs[k];
            if (idx >= hsize) {
               myE.remove (idx-hsize);
               myEPivots.remove (idx-hsize);
               myBe.remove (idx-hsize);
            }
            else {
               if (removeHRows == null) {
                  // lazy creation of list of Hrows to remove
                  removeHRows = new int[k+1];
               }
               removeHRows[k] = idx;
               myBh.remove (idx);
            }
         }
         if (removeHRows != null) {
            myH.removeRows (removeHRows);
            ArraySupport.removeListItems (myHPivots, removeHRows);
         }
         reindexFrom = removeIdxs[0];
      }
      int numRowsAdded = 0;
      int numNewRows = newHRows.size() + newERows.size();
      if (numNewRows > 0) {
         // adding rows
         // note: J is newNewRows X Asize
         MatrixNd J = getNewRowMatrix (newHRows, newERows);
         // store the solution of J in J to reduce memory allocation
         MatrixNd Jsolve = J;
         solveA (Jsolve, J);
         VectorNd solVec = new VectorNd (mySizeA);
         if (newHRows.size() > 0) {
            reindexFrom = Math.min (reindexFrom, myH.rowSize());
         }
         for (int i=0; i<newHRows.size(); i++) {
            HRowData rowData = newHRows.get(i);
            Jsolve.getRow (i, solVec);
            if (addHRow (rowData, solVec)) {
               if (rowData.myPivot.myType == TYPE_D) {
                  resolveY = true;
               }
               numRowsAdded++;
            }
            // else {
            //    System.out.println (
            //       "Add H FAILED, B ratio=" + myHCholesky.eigenValueRatio());
            // }
         }
         if (newERows.size() > 0) {
            reindexFrom = Math.min (reindexFrom, numJRows());
         }         
         for (int i=0; i<newERows.size(); i++) {
            ERowData rowData = newERows.get(i);
            Jsolve.getRow (newHRows.size()+i, solVec);
            if (addERow (rowData, solVec)) {
               numRowsAdded++;
            }
            // else {
            //    System.out.println (
            //       "Add E FAILED, B ratio=" + myHCholesky.eigenValueRatio()+
            //       ", "+rowData.myPivot+", idx="+rowData.myAIdx);
            // }
         }
      }
      reindexJRows (reindexFrom);
      if (resolveY) {
         solveForY();
      }
      return removeIdxs.length + numRowsAdded;
   }
   
   void reindexJRows (int jidx) {
      int hsize = myH.rowSize();
      int jsize = hsize + myE.size();
      while (jidx < hsize) {
         Pivot piv = myHPivots.get(jidx);
         if (piv.myType == TYPE_N) {
            myJRowN[piv.myColIdx] = jidx;
         }
         else {
            myJRowD[piv.myColIdx] = jidx;
         }
         jidx++;
      }
      while (jidx < jsize) {
         Pivot piv = myEPivots.get(jidx-hsize);
         if (piv.myType == TYPE_N) {
            myJRowN[piv.myColIdx] = jidx;
         }
         else {
            myJRowD[piv.myColIdx] = jidx;
         }
         jidx++;        
      }
   }
   
   /**
    * Make sure references between J rows to N/D columns are consistent.
    */
   void checkJRowIndices() {
      int hsize = myH.rowSize();
      int esize = myE.size();
      int jsize = hsize + esize;
      boolean[] markedN = new boolean[mySizeN];
      boolean[] markedD = new boolean[mySizeD];
      for (int i=0; i<jsize; i++) {
         int jrowIdx;
         Pivot piv = (i < hsize ? myHPivots.get(i) : myEPivots.get(i-hsize));
         if (piv.myType == TYPE_N) {
            jrowIdx = myJRowN[piv.myColIdx];
            markedN[piv.myColIdx] = true;
         }
         else {
            jrowIdx = myJRowD[piv.myColIdx];
            markedD[piv.myColIdx] = true;
         }            
         if (jrowIdx != i) {
            throw new InternalErrorException (
               "back index for J row "+i+" is " + jrowIdx);
         }
      }
      for (int i=0; i<mySizeN; i++) {
         if (!markedN[i] && myJRowN[i] != -1) {
            throw new InternalErrorException (
               "jrow index for N "+i+" is "+myJRowN[i]+"; should be -1");
         }
      }
      for (int i=0; i<mySizeD; i++) {
         if (!markedD[i] && myJRowD[i] != -1) {
            throw new InternalErrorException (
               "jrow index for D "+i+" is "+myJRowD[i]+"; should be -1");
         }
      }
      if (myBh.size() != hsize) {
         throw new InternalErrorException (
            "Bh size = "+myBh.size()+"; should be "+hsize);
      }
      if (myHPivots.size() != hsize) {
         throw new InternalErrorException (
            "HPivots size = "+myHPivots.size()+"; should be "+hsize);
      }
      if (myBe.size() != esize) {
         throw new InternalErrorException (
            "Bh size = "+myBe.size()+"; should be "+esize);
      }
      if (myEPivots.size() != esize) {
         throw new InternalErrorException (
            "EPivots size = "+myEPivots.size()+"; should be "+esize);
      }
   }

   boolean doAddRow (
      JRowData rowData, Pivot pivot, int rowIdx, VectorNd col, int sgn) {
      boolean success;
      if (sgn == 1) {
         success = myHCholesky.addPosRowAndColumn (col, 0);
      }
      else {
         success = myHCholesky.addNegRowAndColumn (col, 0);
      }
      int j = pivot.myColIdx;
      if (success) {
         if (pivot.myType == TYPE_N) {
            myStateN[j] = pivot.myNewState;
         }
         else { // piv.myType == TYPE_D
            myStateD[j] = pivot.myNewState;
         }
         return true;
      }
      else {
         myNumFailedPivots++;
         return false;
      }
   }      

   boolean addHRow (HRowData rowData, VectorNd solVec) {
      VectorNd col = new VectorNd (numJRows()+1);
      myH.mul (col, solVec);
      col.set (myH.rowSize(), rowData.dot(solVec) + rowData.myRh);
      mulE (col, solVec, myH.rowSize()+1);
      if (doAddRow (rowData, rowData.myPivot, myH.rowSize(), col, /*sgn=*/1)) {
         myBh.append (rowData.myBh);
         myH.addRow (rowData.myColIdxs, rowData.myValues);
         myHPivots.add (rowData.myPivot);
         return true;
      }
      else {
         return false;
      }
   }

   boolean addERow (ERowData rowData, VectorNd solVec) {
      int numJ = numJRows();
      VectorNd col = new VectorNd (numJ+1);
      myH.mul (col, solVec);
      mulE (col, solVec, myH.rowSize());
      col.set (col.size()-1, solVec.get(rowData.myAIdx));

      if (doAddRow (rowData, rowData.myPivot, numJ, col, /*sgn=*/-1)) {
         myE.append (rowData.myAIdx);
         myBe.append (rowData.myBe);
         myEPivots.add (rowData.myPivot);
         return true;
      }      
      else {
         return false;
      }
   }

   boolean applySinglePivot (ArrayList<Pivot> pivotRequests) {
      if ((myDebug & SHOW_PIVOTS) != 0) {
         printState();
      }
      // start from the end of the pivots and apply the first one we can
      for (int i=pivotRequests.size()-1; i>=0; i--) {
         Pivot piv = pivotRequests.get(i);
         int j = piv.myColIdx;
         if ((myDebug & SHOW_PIVOTS) != 0) {
            System.out.println (" PIVOT " + piv.toString());
         }
         if (piv.requiresNewJRow()) {
            // add row to H
            JRowData newJRow = null;
            if (piv.myNewState == Z) {
               HRowData newRow = new HRowData(piv);
               // set row to a column from NT or DT
               if (piv.myType == TYPE_N) {
                  getNDTColumn (newRow, myNT, j);
                  newRow.myRh = (myRn != null ? myRn.get(j) : 0);
                  newRow.myBh = myBn.get(j);
               }
               else { // piv.myType == TYPE_D
                  getNDTColumn (newRow, myDT, j);
                  newRow.myRh = (myRd != null ? myRd.get(j) : 0);
                  newRow.myBh = myBd.get(j);
               }
               newJRow = newRow;
            }
            else {
               // add row to nullify the active A
               ERowData newRow = new ERowData(piv);               
               if (piv.myType == TYPE_N) {
                  newRow.myAIdx = mySizeMG + piv.myAColIdx;
                  newRow.myBe = 0;
               }
               else { // piv.myType == TYPE_D
                  newRow.myAIdx = mySizeMG + piv.myAColIdx;
                  if (piv.myNewState == W_LO) {
                     newRow.myBe = myFlim.get(j);
                  }
                  else { // piv.myNewState == W_HI
                     newRow.myBe = -myFlim.get(j);
                  }
               }
               newJRow = newRow;
            }
            // try to add the new row
            VectorNd jvec = new VectorNd (mySizeA);
            VectorNd jsol = new VectorNd (mySizeA);

            newJRow.getVec (jvec);
            solveA (jsol, jvec);

            if (newJRow instanceof HRowData) {
               if (addHRow ((HRowData)newJRow, jsol)) {
                  reindexJRows (myH.rowSize()-1);
                  if (piv.myType == TYPE_D) {
                     solveForY();
                  }
                  return true;
               }
               // else {
               //    System.out.println (
               //       "Add H FAILED, B ratio=" + myHCholesky.eigenValueRatio());
               //    System.out.println ("row=" + jsol.toString ("%12.8f"));
               // }
            }
            else {
               if (addERow ((ERowData)newJRow, jsol)) {
                  reindexJRows (myE.size()-1);
                  return true;
               }
               // else {
               //    System.out.println (
               //       "Add E FAILED, B ratio=" + myHCholesky.eigenValueRatio());
               // }
            }
         }
         else {
            // remove row from J
            int removeIdx;
            boolean updateY = false;
            if (piv.myType == TYPE_N) {
               removeIdx = myJRowN[j];
               myStateN[j] = piv.myNewState;
               myJRowN[j] = -1;
            }
            else { // piv.myType == TYPE_D
               removeIdx = myJRowD[j];
               myStateD[j] = piv.myNewState;
               myJRowD[j] = -1;
            }
            int sizeH = myH.rowSize();
            if (removeIdx < sizeH) {
               myH.removeRow (removeIdx);
               myBh.remove (removeIdx);
               myHPivots.remove (removeIdx);
               updateY = (piv.myType == TYPE_D);
            }
            else {
               myE.remove (removeIdx-sizeH);
               myBe.remove (removeIdx-sizeH);
               myEPivots.remove (removeIdx-sizeH);
            }
            myHCholesky.deleteRowAndColumn (removeIdx);
            reindexJRows (removeIdx);
            if (updateY) {
               solveForY();
            }
            return true;
         }           
      }
      return false;
   }

   private int getAIndexN (int col) {
      AConstraintData acons = myAConsN[col];
      return acons != null ? acons.myIdx : -1;
   }
   
   private int getAIndexD (int col) {
      AConstraintData acons = myAConsD[col];
      return acons != null ? acons.myIdx : -1;
   }
   
   ArrayList<Pivot> findPivots (
      VectorNd the, VectorNd phi, VectorNd wn, VectorNd wd, VectorNd flim) {

      ArrayList<Pivot> pivots = new ArrayList<>();

      for (int i=0; i<mySizeN; i++) {
         if (!myNTActivityFrozen) {
            if (myStateN[i] == Z) {
               if (the.get(i) < -myTol) {
                  pivots.add (new Pivot (i, getAIndexN(i), Z, W_LO, TYPE_N));
               }
            }
            else {
               if (wn.get(i) < -myTol) {
                  pivots.add (new Pivot (i, getAIndexN(i), W_LO, Z, TYPE_N));
               }
            }
         }
      }
      for (int i=0; i<mySizeD; i++) {
         int curState = myStateD[i];         
         if (curState == Z) {
            double p = phi.get(i);
            if (p + flim.get(i) < -myTol) {
               pivots.add (new Pivot (i, getAIndexD(i), Z, W_LO, TYPE_D));
            }
            else if (flim.get(i) - p < -myTol) {
               pivots.add (new Pivot (i, getAIndexD(i), Z, W_HI, TYPE_D));
            }
         }
         else {
            if (myFlim.get(i) > 0) {
               if ((curState == W_LO && wd.get(i) < -myTol) ||
                   (curState == W_HI && wd.get(i) > myTol)) {
                  pivots.add (
                     new Pivot (i, getAIndexD(i), curState, Z, TYPE_D));
               }
            }
         }       
      }
      return pivots;
   }

   void getState (VectorNi state) {
      if (state != null) {
         for (int i=0; i<mySizeN; i++) {
            state.set (i, myStateN[i]);
         }
         for (int i=0; i<mySizeD; i++) {
            state.set (mySizeN+i, myStateD[i]);
         }
      }
   }      

   void getStateN (VectorNi stateN) {
      if (stateN != null) {
         for (int i=0; i<mySizeN; i++) {
            stateN.set (i, myStateN[i]);
         }
      }
   }      

   void getStateD (VectorNi stateD) {
      if (stateD != null) {
         for (int i=0; i<mySizeD; i++) {
            stateD.set (i, myStateD[i]);
         }
      }
   }      

   String getStateStr() {
      StringBuilder sb = new StringBuilder();
      sb.append (LCPSolver.stateToString(myStateN, mySizeN));
      sb.append ('|');
      sb.append (LCPSolver.stateToString(myStateD, mySizeD));
      return sb.toString();
   }

   void printState() {
      System.out.println (getStateStr());
   }

   protected void updateBasisForFrictionLimits () {
      ArrayList<Pivot> pivots = new ArrayList<>();
      for (int i=0; i<mySizeD; i++) {
         if (myFlim.get(i) == 0 && myStateD[i] == Z) {
            Pivot pivot = new Pivot (i, getAIndexD(i), Z, W_LO, TYPE_D);
            pivots.add (pivot);
         }
      }
      if (pivots.size() > 0) {
         applyBlockPivots (pivots);
      }
      else {
         solveForY();
      }
   }

   /**
    * Freeze NT activity
    */
   protected void setNTActivityFrozen (boolean enable) {
      if (enable != myNTActivityFrozen) {
         myNTActivityFrozen = enable;
      }
   }

   /**
    * Whenever the upper and lower z bounds are equal, the pivoting will allow
    * state to be either L or H.  To strictly adhere to the definition of a
    * BLCP, the corresponding state should be set L or H depending on the sign
    * of w.
    *
    * <p>Upper and lower bounds will be equal for N constraints when
    * NTActivityFrozen is true (with hi = lo = 0), or for D constraints when
    * flim = 0.
    */
   void adjustStateForEqualBounds () {
      // when we update the state, we also update the corresponding A state if
      // it is non-basic. Otherwise, the constraint is basic in A and there
      // must be an E entry to make it non-basic. We could update the pivot
      // entry for E to reflect the changed state, but there is no need the
      // state info in Pivot is not used after the pivot has been applied.
      if (myNTActivityFrozen && mySizeN > 0) {
         // need to adjust state to refect values of w
         for (int j=0; j<mySizeN; j++) {
            double wval = myWn.get(j);
            if (myStateN[j] != Z && wval != 0) {
               int sval = wval > 0 ? W_LO : W_HI;
               myStateN[j] = sval;
            }
         }
      }
      if (mySizeD > 0) {
         for (int j=0; j<mySizeD; j++) {
            if (myFlim.get(j) == 0) {
               double wval = myWd.get(j);
               if (myStateD[j] != Z) {
                  if (wval != 0) {
                     int sval = wval > 0 ? W_LO : W_HI;
                     myStateD[j] = sval;
                  }
               }
               else {
                  throw new InternalErrorException (
                     "D constraint "+j+" in Z state with friction limit of 0");
               }
            }
         }
      }
   }

   protected Status runPivotingLoop (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi) {

      int ninfMin = mySizeND;
      int p = 4;
      int blockPivotIterLimit = p;
      myBlockPivoting = myDefaultBlockPivoting;

      if ((myDebug & SHOW_PIVOTS) != 0) {
         System.out.println (
            "LCP START "+getStateStr()+" blockPivoting=" + myBlockPivoting);
      }      

      while (myIterationCnt < myMaxIterations) {
         myIterationCnt++;
         // System.out.println (
         //    " iter " + myIterationCnt + " state=" +
         //    MurtySparseContactSolverTest.stateToString (curstate, mySizeN));
         solveForBasicVariables (vel, lam, the, phi);
         // use vel to find wn and wd:
         // wn = N vel + Rn the - bn
         // wd = D vel + Rd phi - bd
         // 
         // Note that even though we use -Rn and -Rd in the system matrix, we
         // add Rn*the and Rd*phi since the and phi are themselves negated.
         if (mySizeN > 0) {
            myNT.mulTranspose (myWn, vel, myNT.colSize(), mySizeM);
            if (myRn != null) {
               for (int i=0; i<mySizeN; i++) {
                  myWn.add (i, myRn.get(i)*the.get(i));
               }
            }
            myWn.sub (myBn);
         }
         if (mySizeD > 0) {
            myDT.mulTranspose (myWd, vel, myDT.colSize(), mySizeM);
            if (myRd != null) {
               for (int i=0; i<mySizeD; i++) {
                  myWd.add (i, myRd.get(i)*phi.get(i));
               }
            }
            myWd.sub (myBd);
         }

         ArrayList<Pivot> pivotRequests =
            findPivots (the, phi, myWn, myWd, myFlim);
         int ninf = pivotRequests.size();
         if (ninf == 0) {
            if ((myDebug & SHOW_PIVOTS) != 0) {
               printState();
            }
            return Status.SOLVED;
         }         
         if ((myDebug & SHOW_PIVOTS) != 0) {
            System.out.println (" iter " + myIterationCnt);
         }
         if (myBlockPivoting) {
            int npiv = applyBlockPivots (pivotRequests);
            if (npiv == 0) {
               return Status.NO_SOLUTION;
            }
            else if (ninf < ninfMin) {
               ninfMin = npiv;
               blockPivotIterLimit = myIterationCnt + p;
            }
            else if (myIterationCnt > blockPivotIterLimit) {
               if ((myDebug & SHOW_BLOCK_PIVOT_CHANGE) != 0) {
                  System.out.println ("block pivots OFF");
               }
               myBlockPivotFailCnt++;
               myBlockPivoting = false;
            }
            myPivotCnt += npiv;
            //checkJRowIndices();
         }
         else {
            if (!applySinglePivot (pivotRequests)) {
               return Status.NO_SOLUTION;
            }
            if (ninf < ninfMin-1) {
               // try restarting block pivots
               ninfMin = ninf;
               myBlockPivoting = true;
               blockPivotIterLimit = myIterationCnt + p;
            }
            myPivotCnt++;
            //checkJRowIndices();
         }
         //printState();
      }
      return Status.ITERATION_LIMIT_EXCEEDED;
   }

   public Status contactSolve (
      VectorNd vel, VectorNd lam, VectorNd the, 
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, SparseBlockMatrix NT, 
      VectorNd Rn, VectorNd bn, VectorNi stateN) {
      
      return solve (
         vel, lam, the, null, M, sizeM, bm, versionM, GT, Rg, bg, 
         NT, Rn, bn, stateN, null, null, null, (VectorNi)null,
         null, 0, CONTACT_SOLVE);
   }
   
   public Status contactSolve (
      VectorNd vel, VectorNd lam, VectorNd the, 
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, SparseBlockMatrix NT, 
      VectorNd Rn, VectorNd bn, VectorNi stateN, int flags) {
      
      if ((flags & NT_INACTIVE) != 0) {
         throw new IllegalArgumentException (
            "NT_INACTIVE flag cannot be set for contact solves");
      }      
      return solve (
         vel, lam, the, null, M, sizeM, bm, versionM, GT, Rg, bg, 
         NT, Rn, bn, stateN, null, null, null, (VectorNi)null, 
         null, 0, flags | CONTACT_SOLVE);
   }

   public Status solve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi, 
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, 
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD,
      ArrayList<FrictionInfo> finfo) {
      
      return solve (
         vel, lam, the, phi, M, sizeM, bm, versionM, GT, Rg,
         bg, NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 0, /*flags=*/0);
   }
   
   public Status solve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi, 
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, 
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD,
      ArrayList<FrictionInfo> finfo, int frictionIters, int flags) {

      mySolveCnt = 0;
      myIterationCnt = 0;
      myPivotCnt = 0;
      myBlockPivotFailCnt = 0;
      myNumFailedPivots = 0;
      myAStructureChanged = ((flags & REBUILD_A) != 0); // also updated below
      if (myAStructureChanged) {
         if (showAStructureChange) System.out.println ("rebuild: flag");
      }
      myContactSolveP = ((flags & CONTACT_SOLVE) != 0);

      initializeSolverIfNecessary();

      setMGVariables (M, sizeM, bm, versionM, GT, Rg, bg, vel, lam);
      setNVariables (NT, Rn, bn, the);
      setDVariables (DT, Rd, bd, phi, finfo);

      if (mySizeD > 0) {
         setFrictionInfo (finfo);
         updateFrictionLimits (lam, the);
      }
      else {
         myFrictionInfo = null;
         // ensure there will be only one call to runPivotingLoop()
         frictionIters = 0;
      }

      myTol = myDefaultTol;

      mySavedMaxRefinementSteps = myPardiso.getMaxRefinementSteps();
      myPardiso.setMaxRefinementSteps(0);

      updateAndSolveA (stateN, stateD);
      myNTActivityFrozen = ((flags & NT_INACTIVE) != 0);
      
      Status status = Status.SOLVED;
      if (mySizeND > 0) {
         int numOuterIters = frictionIters + 1;
         myMaxIterations = numOuterIters*myIterationLimit*mySizeND+1;
         for (int k=0; k<numOuterIters && status == Status.SOLVED; k++) {
            if (k > 0) {
               updateFrictionLimits (lam, the);
               updateBasisForFrictionLimits();
               myNTActivityFrozen = 
                 (!myNTFrictionActivity || (flags & NT_INACTIVE) != 0);
               if ((myDebug & SHOW_PIVOTS) != 0) {
                  System.out.println ("FRICTION INTERATION " + k);
               }
            }
            status = runPivotingLoop (vel, lam, the, phi);
         }
         adjustStateForEqualBounds();
      }
      else {
         extractMGSolution (vel, lam);
      }
      myPardiso.setMaxRefinementSteps(mySavedMaxRefinementSteps);
      myNTActivityFrozen = false;
      getStateN (stateN);
      getStateD (stateD);

      if ((myDebug & SHOW_RETURNS) != 0) {
         System.out.println (status);
      }
      return status;
   }
   
   public Status solve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi, 
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, 
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN, 
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD,
      VectorNd flim) {

      return solve (
         vel, lam, the, phi, M, sizeM, bm, versionM, GT, Rg, bg,
         NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim, 0);
   }

   public Status solve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi, 
      SparseBlockMatrix M, int sizeM, VectorNd bm, int versionM, 
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg, 
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD,
      VectorNd flim, int flags) {

      mySolveCnt = 0;
      myIterationCnt = 0;
      myPivotCnt = 0;
      myBlockPivotFailCnt = 0;
      myNumFailedPivots = 0;
      myAStructureChanged = ((flags & REBUILD_A) != 0); // also updated below
      myContactSolveP = false;

      initializeSolverIfNecessary();

      setMGVariables (M, sizeM, bm, versionM, GT, Rg, bg, vel, lam);
      setNVariables (NT, Rn, bn, the);
      setDVariables (DT, Rd, bd, phi, null);

      setFrictionLimits (flim);

      mySavedMaxRefinementSteps = myPardiso.getMaxRefinementSteps();
      myPardiso.setMaxRefinementSteps(0);
      
      myTol = myDefaultTol;

      updateAndSolveA (stateN, stateD);
      myNTActivityFrozen = ((flags & NT_INACTIVE) != 0);

      Status status = Status.SOLVED;
      if (mySizeND > 0) {
         myMaxIterations = myIterationLimit*mySizeND+1;
         status = runPivotingLoop (vel, lam, the, phi);
         adjustStateForEqualBounds();
      }
      else {
         extractMGSolution (vel, lam);      
      }
      
      myPardiso.setMaxRefinementSteps(mySavedMaxRefinementSteps);
      myNTActivityFrozen = false;
      getStateN (stateN);
      getStateD (stateD);
      if ((myDebug & SHOW_RETURNS) != 0) {
         System.out.println (status);
      }
      return status;
   }

   public void resolveMG (
      VectorNd vel, VectorNd lam, VectorNd bm, VectorNd bg) {

      if (!myAMatrixFactored) {
         throw new IllegalStateException (
            "System has not been factored in a previous solve() call");
      }
      if (vel.size() != mySizeM) {
         throw new IllegalArgumentException (
            "vel.size "+vel.size()+" not equal to expected value "+mySizeM);
      }
      if (bm.size() != mySizeM) {
         throw new IllegalArgumentException (
            "bm.size "+bm.size()+" not equal to expected value "+mySizeM);
      }
      int ia = 0;
      for (int i=0; i<mySizeM; i++) {
         myB.set (ia++, bm.get(i));
      }
      if (lam != null) {
         if (lam.size() != mySizeG) {
            throw new IllegalArgumentException (
               "lam.size="+lam.size()+" not equal to expected value "+mySizeG);
         }
      }
      else {
         lam = new VectorNd(mySizeG);
      }
      if (bg != null) {
         if (bg.size() != mySizeG) {
            throw new IllegalArgumentException (
               "bg.size="+bg.size()+" not equal to expected value "+mySizeG);
         }
         for (int i=0; i<mySizeG; i++) {
            myB.set (ia++, bg.get(i));
         }
      }

      mySolveCnt = 0;
      myIterationCnt = 0;
      myPivotCnt = 0;
      myBlockPivotFailCnt = 0;
      myNumFailedPivots = 0;
      
      VectorNd the = new VectorNd(mySizeN);
      VectorNd phi = new VectorNd(mySizeD);

      solveForY();
      solveForBasicVariables (vel, lam, the, phi);
   }
   
   /**
    * For debugging and testing: return a 0-based CRS representation of the
    * most recently constructed A matrix.
    */
   CRSValues getAMatrix() {

      int[] localOffs = new int[mySizeM];

      int[] rowOffs = Arrays.copyOf (myRowOffsA, mySizeA+1);
      int[] colIdxs = getAColumnIndices (localOffs);
      double[] values = Arrays.copyOf (myValuesA, myNumValsA);

      CRSValues crs = new CRSValues (rowOffs, colIdxs, values, mySizeA);
      return crs;      
   }

   public boolean isAMatrixFactored() {
      return myAMatrixFactored;
   }

   /**
    * Returns the w vector associated with the most recent solve. If the solve
    * had no unilateral constraints, this vector will have length 0.
    *
    * @return w vector (may be modified)
    */
   public VectorNd getW () {
      VectorNd w = new VectorNd(mySizeND);
      if (mySizeN > 0) {
         w.setSubVector (0, myWn);
      }
      if (mySizeD > 0) {
         w.setSubVector (mySizeN, myWd);
      }
      return w;
   }

   public void dispose() {
      if (myMatrixSolver != null) {
         myMatrixSolver.dispose();
         myMatrixSolver = null;
         myPardiso = null;
         myUmfpack = null;
      }
   }

   public void finalize() {
      dispose();
   }

   public double getAvgSolveTime() {
      if (myTotalSolveCnt == 0) {
         return 0;
      }
      else {
         return mySolveTimer.getTimeUsec()/myTotalSolveCnt;
      }
   }

   public int getTotalSolveCount() {
      return myTotalSolveCnt;
   }

   public double getAvgAnalyzeTime() {
      if (myTotalAnalyzeCnt == 0) {
         return 0;
      }
      else {
         return myAnalyzeTimer.getTimeUsec()/myTotalAnalyzeCnt;
      }
   }

   public int getTotalAnalyzeCount() {
      return myTotalAnalyzeCnt;
   }

   public double getAvgFactorTime() {
      if (myTotalFactorCnt == 0) {
         return 0;
      }
      else {
         return myFactorTimer.getTimeUsec()/myTotalFactorCnt;
      }
   }

   public int getTotalFactorCount() {
      return myTotalFactorCnt;
   }

   public void resetTimers() {
      mySolveTimer.reset();
      myTotalSolveCnt = 0;
      myFactorTimer.reset();
      myTotalFactorCnt = 0;
      myAnalyzeTimer.reset();
      myTotalAnalyzeCnt = 0;
   }

   public VectorNi getStateA() {
      VectorNi state = new VectorNi (mySizeND);
      for (AConstraintData acons : myAConsData) {
         if (!acons.isMasked()) {
            if (acons.myType == TYPE_N) {
               state.set (acons.myCol, Z);
            }
            else if (mySizeD > 0) {
               state.set (mySizeN+acons.myCol, Z);
            }
         }
      }
      return state;
   }

   void checkConsistency() {
      if (mySizeA != mySizeNA + mySizeDA + mySizeMG) {
         throw new TestException (
            "sizeA=" + mySizeA + " != sizeNA=" + mySizeNA +
            " + sizeDA=" + mySizeDA + " + sizeMG=" + mySizeMG);
      }
      if (myAConsData.size() != mySizeNA + mySizeDA /* + numMaskedDA()*/) {
         throw new TestException (
            "myAConsData.size()=" + myAConsData.size() +
            " != sizeNA=" + mySizeNA + " + sizeDA=" + mySizeDA +
            " + numMaskedA=" + numMaskedA());
      }
      AConstraintData[] aconsNChk = new AConstraintData[mySizeN];
      AConstraintData[] aconsDChk = new AConstraintData[mySizeD];
      int idx = 0;
      for (AConstraintData acons : myAConsData) {
         if (!acons.isMasked()) {
            if (acons.myType == TYPE_N) {
               aconsNChk[acons.myCol] = acons;
            }
            else {
               aconsDChk[acons.myCol] = acons;
            }
         }
         if (acons.myIdx != idx) {
            throw new TestException ("constraint "+idx+" has idx "+acons.myIdx);
         }
         idx++;
      }
      for (int i=0; i<mySizeN; i++) {
         if (aconsNChk[i] != myAConsN[i]) {
            throw new TestException (
               "aconsN["+i+"]="+myAConsN[i]+", expected "+aconsNChk[i]);
         }
      }
      for (int i=0; i<mySizeD; i++) {
         if (aconsDChk[i] != myAConsD[i]) {
            System.out.println ("sizeD=" + mySizeD);
            throw new TestException (
               "aconsD["+i+"]="+myAConsD[i]+", expected "+aconsDChk[i]);
         }
      }
   }

   AConstraintData[] getAconsD() {
      int maxi = -1;
      for (AConstraintData acons : myAConsData) {
         if (acons.myCol > maxi) {
            maxi = acons.myCol;
         }
      }
      AConstraintData[] aconsD = new AConstraintData[maxi+1];
      for (AConstraintData acons : myAConsData) {
         if (acons.myCol > maxi) {
            aconsD[acons.myCol] = acons;
         }
      }
      return aconsD;
   }

   public String getAConsString () {
      StringBuilder sb = new StringBuilder();
      ConstraintType prevType = null;
      sb.append ("| ");
      for (AConstraintData acons : myAConsData) {
         if (prevType != null && prevType != acons.myType) {
            sb.append ("| ");
         }
         if (acons.isMasked()) {
            if (acons.myContactMask) {
               sb.append ('C');
            }
            if (acons.myRemovedMask) {
               sb.append ('R');
            }
            sb.append (" ");
         }
         else {
            sb.append (acons.myCol + " ");
         }
         prevType = acons.myType;
      }
      sb.append ("|");
      return sb.toString();
   }

   String getAConsIdxString () {
      StringBuilder sb = new StringBuilder();
      ConstraintType prevType = null;
      sb.append ("| ");
      for (AConstraintData acons : myAConsData) {
         if (prevType != null && prevType != acons.myType) {
            sb.append ("| ");
         }
         sb.append (acons.myIdx + " ");
         prevType = acons.myType;
      }
      sb.append ("|");
      return sb.toString();
   }

   public void initialize() {
      // reinit hybrid solve stats
      myHybridCnt = 0;
      myAvgDirectTime = 0;
      myAvgHybridTime = 0;
   }

}
