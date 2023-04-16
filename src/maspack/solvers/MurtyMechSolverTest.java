package maspack.solvers;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.matrix.Matrix.Partition;
import maspack.solvers.MurtyMechSolver.HRowData;
import maspack.solvers.MurtyMechSolver.Pivot;
import maspack.solvers.MurtyMechSolver.ConstraintType;
import maspack.solvers.LCPSolver.Status;

public class MurtyMechSolverTest extends UnitTest {

   static final double INF = Double.POSITIVE_INFINITY;

   static final int Z = LCPSolver.Z_VAR;
   static final int W_LO = LCPSolver.W_VAR_LOWER;
   static final int W_HI = LCPSolver.W_VAR_UPPER;

   protected static final ConstraintType TYPE_N = ConstraintType.N;
   protected static final ConstraintType TYPE_D = ConstraintType.D;

   private static int REBUILD_A = MurtyMechSolver.REBUILD_A;
   private static int USE_THE = 0x0010;
   private static int DEBUG = 0x0020;

   boolean printTestProblemStates = false;

   VectorNi packState (VectorNi stateN, VectorNi stateD) {
      VectorNi state = new VectorNi (stateN.size()+stateD.size());
      state.setSubVector (0, stateN);
      state.setSubVector (stateN.size(), stateD);
      return state;
   }

   String stateToString (VectorNi stateN, VectorNi stateD) {
      return LCPSolver.stateToString (packState (stateN, stateD));
   }                               

   void unpackState (VectorNi stateN, VectorNi stateD, VectorNi state) {
      state.getSubVector (0, stateN);
      state.getSubVector (stateN.size(), stateD);
   }

   class TestProblem {
      boolean debug;

      SparseBlockMatrix myM;
      VectorNd myBm;
      int mySizeM;
      int myVersionM = 0;

      SparseBlockMatrix myGT;
      int mySizeG;
      VectorNd myRg;
      VectorNd myBg;
      int[] myGidxs;
      VectorNi myPrevG;
      int myNumFixedG; // number of G that are not contact associated

      SparseBlockMatrix myNT;
      int mySizeN;
      VectorNd myRn;
      VectorNd myBn;
      int[] myNidxs;
      VectorNi myPrevN;

      SparseBlockMatrix myDT;
      int mySizeD;
      VectorNd myRd;
      VectorNd myBd;
      ArrayList<FrictionInfo> myFinfo;

      VectorNd myVel;
      VectorNd myLam;
      VectorNd myThe;
      VectorNd myPhi;
      VectorNi myStateN;
      VectorNi myStateD;

      VectorNd myVelChk;
      VectorNd myTheChk;
      VectorNd myLamChk;
      VectorNd myPhiChk;
      VectorNi myStateNChk;
      VectorNi myStateDChk;

      // lambda and theta values in the contact solution
      VectorNd myConVel;
      VectorNd myConVelChk;
      VectorNd myConLam;
      VectorNd myConLamChk;
      VectorNd myConThe;
      VectorNd myConTheChk;

      double myMu;

      int myFrictionIters = 1;

      void setFrictionIters (int n) {
         myFrictionIters = n;
      }

      VectorNd getTheta() {
         return new VectorNd (myThe);
      }
      
      void setTheta (VectorNd the) {
         myThe.set (the);
      }

      VectorNd getLambda() {
         return new VectorNd (myLam);
      }
      
      void setLambda (VectorNd lam) {
         myLam.set (lam);
      }

      VectorNd getPhi() {
         return new VectorNd (myPhi);
      }
      
      void setPhi (VectorNd phi) {
         myPhi.set (phi);
      }

      void setFixedGT (TestProblem prob) {
         if (myNumFixedG != prob.myNumFixedG) {
            throw new IllegalArgumentException (
               "fixed G sizes differ");
         }
         VectorNd col = new VectorNd (myGT.rowSize());
         for (int j=0; j<myNumFixedG; j++) {
            prob.myGT.getColumn (j, col);
            for (int i=0; i<myGT.rowSize(); i++) {
               myGT.set (i, j, col.get(i));
            }
         }
      }

      void createSolutionCheck (String stateStr) {
         createSolutionCheck (stateStr, 0);
      }

      void createSolutionCheck (String stateStr, int flags) {
         MurtyMechSolver murty = new MurtyMechSolver();
         murty.debug = ((flags & DEBUG) != 0);

         myStateNChk.setSize (mySizeN);
         myStateDChk.setSize (mySizeD);
         if (stateStr != null && stateStr.length() > 0) {
            initStateFromStr (myStateNChk, myStateDChk, stateStr);
         }
         if ((flags & USE_THE) == 0) {
            myTheChk.setZero();
            myLamChk.setZero();
         }
         else {
            myTheChk.set (myThe);
            myLamChk.set (myLam);
         }
         Status status = murty.solve (
            myVelChk, myLamChk, myTheChk, myPhiChk, myM, mySizeM, myBm,
            -1, myGT, myRg, myBg, myNT, myRn, myBn, myStateNChk,
            myDT, myRd, myBd, myStateDChk,
            myFinfo,  myFrictionIters, 0);
         check ("status != SOLVED", status == Status.SOLVED);     
      }

      void createContactSolutionCheck (String stateStr) {
         MurtyMechSolver murty = new MurtyMechSolver();

         myStateNChk = new VectorNi (mySizeN);
         if (stateStr != null && stateStr.length() > 0) {
            initStateFromStr (myStateNChk, null, stateStr);
         }
         myConTheChk.setZero();
         myConLamChk.setZero();

         Status status = murty.solve (
            myConVelChk, myConLamChk, myConTheChk, null, myM, mySizeM, myBm, -1,
            myGT, myRg, myBg, myNT, myRn, myBn, myStateNChk,
            null, null, null, (VectorNi)null, null, 0, 0);
         check ("status != SOLVED", status == Status.SOLVED);     
      }

      void checkSolve (
         String label, MurtyMechSolver murty,
         String stateStr, int flags, double tol) {

         if ((flags & USE_THE) == 0) {
            myThe.setZero();
            myLam.setZero();
         }
         myStateN.setSize (mySizeN);
         myStateD.setSize (mySizeD);
         if (stateStr != null) {
            LCPSolver.clearState (myStateN);
            LCPSolver.clearState (myStateD);
            initStateFromStr (myStateN, myStateD, stateStr);
         }
         if ((flags & REBUILD_A) != 0) {
            myVersionM++;
         }         
         if (printTestProblemStates) {
            System.out.println (
               "state in: " + stateToString(myStateN, myStateD));
         }
         Status status = murty.solve (
            myVel, myLam, myThe, myPhi, myM, mySizeM, myBm, myVersionM, 
            myGT, myRg, myBg, myNT, myRn, myBn, myStateN,
            myDT, myRd, myBd, myStateD, myFinfo,
            myFrictionIters, 0);

         check ("status != SOLVED", status == Status.SOLVED);
         checkEquals ("stateN "+label, myStateN, myStateNChk);         
         checkEquals ("stateD "+label, myStateD, myStateDChk);         
         checkNormedEquals ("lam "+label, myLam, myLamChk, tol);
         checkNormedEquals ("the "+label, myThe, myTheChk, tol);
         if (myPhi != null) {
            checkNormedEquals ("phi "+label, myPhi, myPhiChk, tol);
         }
         checkNormedEquals ("vel "+label, myVel, myVelChk, tol);

         if (printTestProblemStates) {
            System.out.println ("> " + label);
            System.out.println (
               "stateA=" + LCPSolver.stateToString(murty.getStateA()));
            System.out.println (
               "Astring=" + murty.getAConsString());
            System.out.println (
               "state=" + murty.getStateStr());
            System.out.println ("");
         }

         for (int i=0; i<mySizeN; i++) {
            myPrevN.set (i, i);
         }
      }

      void initStateFromStr (VectorNi stateN, VectorNi stateD, String stateStr) {
         int sizeN = stateN.size();
         int sizeD = (stateD != null ? stateD.size() : 0);
         if (stateStr.length() > sizeN + sizeD) {
            throw new IllegalArgumentException (
               "stateStr too large");
         }
         String stateStrN =
            stateStr.substring (0, Math.min(sizeN, stateStr.length()));
         stateN.setSubVector (0, LCPSolver.stringToState (stateStrN));
         if (sizeD > 0) {
            if (stateStr.length() > sizeN) {
               String stateStrD = stateStr.substring (sizeN);
               stateD.setSubVector (0, LCPSolver.stringToState (stateStrD));
            }
         }
      }

      void checkContactSolve (
         String label, MurtyMechSolver murty, String stateStr,
         int flags, double tol) {

         myConThe.setZero();
         myConLam.setZero();
         myStateN.setSize (mySizeN);
         if (stateStr != null) {
            initStateFromStr (myStateN, null, stateStr);
            // if (stateStr.length() > myStateN.size()) {
            //    throw new IllegalArgumentException (
            //       "stateStr too large");
            // }
            // VectorNi startState = LCPSolver.stringToState (stateStr);
            // LCPSolver.clearState (myStateN);         
            // myStateN.setSubVector (0, startState);
         }
         if (printTestProblemStates) {
            System.out.println (
               "contact state in: " + LCPSolver.stateToString(myStateN)); 
            System.out.println ("prevN: " + myPrevN);
         }

         if ((flags & REBUILD_A) != 0) {
            myVersionM++;
         }
         // Status status = murty.solve (
         //    myVel, myConLam, myConThe, null, myM, mySizeM, myBm, 
         //    myGT, myRg, myBg, prevG, myNT, myRn, myBn, prevN, 
         //    null, null, null, null, myStateN, 0);

         Status status = murty.contactSolve (
            myConVel, myConLam, myConThe, myM, mySizeM, myBm, myVersionM,
            myGT, myRg, myBg, myNT, myRn, myBn, myStateN);

         check ("status != SOLVED", status == Status.SOLVED);
         checkEquals ("state N "+label, myStateN, myStateNChk);         
         checkNormedEquals ("lam "+label, myConLam, myConLamChk, tol);
         checkNormedEquals ("the "+label, myConThe, myConTheChk, tol);
         checkNormedEquals ("vel "+label, myConVel, myConVelChk, tol);

         if (printTestProblemStates) {
            System.out.println ("> " + label);
            System.out.println (
               "stateA=" + LCPSolver.stateToString(murty.getStateA()));
            System.out.println (
               "Astring=" + murty.getAConsString());
            System.out.println (
               "state=" + LCPSolver.stateToString(myStateN)); 
            System.out.println ("");
         }

         for (int i=0; i<mySizeN; i++) {
            myPrevN.set (i, i);
         }
      }
   }

   Random myRand;
   boolean myBlockPivoting = true;

   void setBlockPivoting (boolean enable) {
      myBlockPivoting= enable; 
   }
   
   public MurtyMechSolverTest() {
      myRand = RandomGenerator.get();
      myRand.setSeed (0x1234);
   }

   MatrixBlock createRandomBlock (int m, int n) {
      MatrixBlock blk = MatrixBlockBase.alloc (m, n);
      MatrixNd M = new MatrixNd (m, n);
      M.setRandom();
      blk.set (M);
      return blk;
   }

   MatrixBlock createRandomSymmetricBlock (int m, int n) {
      MatrixBlock blk = MatrixBlockBase.alloc (m, n);
      MatrixNd M = new MatrixNd (m, n);
      M.setRandom();
      M.mulTranspose (M);
      blk.set (M);
      return blk;
   }

   MatrixBlock createTransposedBlock (MatrixBlock blk) {
      MatrixBlock blkT = MatrixBlockBase.alloc (blk.colSize(), blk.rowSize());
      MatrixNd MT = new MatrixNd (blk);
      MT.transpose();
      blkT.set (MT);
      return blkT;
   }

   void addRandomBlock (SparseBlockMatrix M, int bi, int bj) {
      int m = M.getBlockRowSize (bi);
      int n = M.getBlockColSize (bj);
      MatrixBlock blk = createRandomBlock (m, n);
      M.addBlock (bi, bj, blk);
   }

   void addRandomSymmetric (SparseBlockMatrix M, int bi, int bj) {
      addRandomSymmetric (M, bi, bj, 1.0);
   }

   void addRandomSymmetric (SparseBlockMatrix M, int bi, int bj, double scale) {
      int m = M.getBlockRowSize (bi);
      int n = M.getBlockColSize (bj);

      if (bi == bj) {
         MatrixBlock blk = createRandomSymmetricBlock (m, n);
         M.addBlock (bi, bj, blk);
      }
      else {
         MatrixBlock blk = createRandomBlock (m, n);
         MatrixBlock blkT = createTransposedBlock (blk);
         M.addBlock (bi, bj, blk);
         M.addBlock (bj, bi, blkT);
      }
      M.scale (scale);
   }

   void addRandomBlocks (SparseBlockMatrix M) {
      addRandomBlocks (M, M.numBlockRows()-1);
   }

   void perturbBlocks (SparseBlockMatrix M, double amp) {
      for (int bi=0; bi<M.numBlockRows(); bi++) {
         MatrixBlock blk;
         for (blk=M.firstBlockInRow(bi); blk!=null; blk=blk.next()) {
            MatrixNd Mnoise = new MatrixNd (blk.rowSize(), blk.colSize());
            Mnoise.setRandom ();
            blk.scaledAdd (amp, Mnoise);
         }
      }
   }

   void perturbBlocksSymmetric (SparseBlockMatrix M, double amp) {
      for (int bi=0; bi<M.numBlockRows(); bi++) {
         MatrixBlock blk;
         for (blk=M.firstBlockInRow(bi); blk!=null; blk=blk.next()) {

            MatrixNd perturb = new MatrixNd (blk.rowSize(), blk.colSize());
            perturb.setRandom();
            MatrixNd perturbT = new MatrixNd (blk.colSize(), blk.rowSize());
            perturbT.transpose (perturb);

            int bj = blk.getBlockCol();
            if (bj == bi) {
               perturb.add (perturbT);
               blk.scaledAdd (amp/2, perturb);
            }
            else if (bj > bi) {
               blk.scaledAdd (amp, perturb);
               MatrixBlock blkT = M.getBlock (bj, bi);
               blkT.scaledAdd (amp, perturbT);               
            }
         }
      }
   }

   void perturbVector (VectorNd vec, double amp) {
      for (int k=0; k<vec.size(); k++) {
         vec.add (k, RandomGenerator.nextDouble (-amp, amp));
      }
   }

   void addRandomBlocks (SparseBlockMatrix M, int minBlkRow) {
      for (int bj=0; bj<M.numBlockCols(); bj++) {
         int[] blockRows;
         do {
            blockRows = RandomGenerator.randomSubsequence (M.numBlockRows());
         }
         while (blockRows.length < 2 || blockRows[0] > minBlkRow);
         for (int bi : blockRows) {
            addRandomBlock (M, bi, bj);
         }
      }
   }

   void addFullRandomBlocks (SparseBlockMatrix M) {
      for (int bj=0; bj<M.numBlockCols(); bj++) {
         for (int bi=0; bi<M.numBlockRows(); bi++) {
            addRandomBlock (M, bi, bj);
         }
      }
   }

   void checkEquals (CRSValues crs, CRSValues chk) {
      checkEquals (
         "CRS values for A, colSize", crs.colSize(), chk.colSize());
      checkEquals (
         "CRS values for A, rowSize", crs.rowSize(), chk.rowSize());
      checkEquals (
         "CRS values for A, numNonZeros",
         crs.numNonZeros(), chk.numNonZeros());
      for (int i=0; i<=crs.rowSize(); i++) {
         if (crs.getRowOffs()[i] != chk.getRowOffs()[i]) {
            throw new TestException (
               "CRS values for A, rowOff["+i+"]=" + crs.getRowOffs()[i] +
               ", expected "+chk.getRowOffs()[i]);
         }
      }
      for (int i=0; i<crs.numNonZeros(); i++) {
         if (crs.getColIdxs()[i] != chk.getColIdxs()[i]) {
            throw new TestException (
               "CRS values for A, colIdxs["+i+"]=" + crs.getColIdxs()[i] +
               ", expected "+chk.getColIdxs()[i]);
         }
      }
      for (int i=0; i<crs.numNonZeros(); i++) {
         if (crs.getValues()[i] != chk.getValues()[i]) {
            throw new TestException (
               "CRS values for A, rowOff["+i+"]=" + crs.getValues()[i] +
               ", expected "+chk.getValues()[i]);
         }
      }
   }

   int numZVarsInBlockCol (SparseBlockMatrix HT, int bj, int state[], int off) {
      int nz = 0;
      int j = HT.getBlockColOffset(bj);
      for (int k=0; k<HT.getBlockColSize(bj); k++) {
         if (state[off+j+k] == Z) {
            nz++;
         }
      }
      return nz;
   }

   protected void testAssembleA (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg, 
      SparseBlockMatrix NT, VectorNd Rn, SparseBlockMatrix DT, VectorNd Rd)  {

      int numtests = 10;

      int sizeG = (GT != null ? GT.colSize() : 0);
      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      int blkSizeM = M.getAlignedBlockRow (sizeM);

      for (int k=0; k<numtests; k++) {
         //VectorNi state = createRandomState (sizeN + sizeD);
         VectorNi stateN = createRandomNState (sizeN);
         VectorNi stateD = createRandomDState (sizeD);
         VectorNi state = new VectorNi (stateN);
         state.append (stateD);
         
         SparseBlockMatrix HT;
         VectorNd Rh = new VectorNd();
         if (GT != null) {
            HT = GT.clone();            
            if (Rg != null) {
               Rh.set (Rg);
            }
            else {
               Rh.setSize (GT.colSize());
            }
         }
         else {
            int[] rowSizes = new int[blkSizeM];
            for (int bi=0; bi<rowSizes.length; bi++) {
               rowSizes[bi] = M.getBlockRowSize(bi);
            }
            HT = new SparseBlockMatrix (rowSizes, new int[0]);
         }

         // add extra columns for active parts of NT and DT
         int colHT = HT.numBlockCols();
         for (int j=0; j<state.size(); j++) {
            if (state.get(j) == Z) {
               SparseBlockMatrix XT;
               int jx;
               if (j < sizeN) {
                  XT = NT;
                  jx = j;
                  Rh.append (Rn != null ? Rn.get(j) : 0);
               }
               else {
                  XT = DT;
                  jx = j-sizeN;
                  Rh.append (Rd != null ? Rd.get(j-sizeN) : 0);
               }
               int bj = XT.getBlockCol (jx);
               for (int bi=0; bi<XT.numBlockRows(); bi++) {
                  MatrixBlock blk = XT.getBlock (bi, bj);
                  if (blk != null) {
                     MatrixBlock subblk =
                        MatrixBlockBase.alloc (blk.rowSize(), 1);
                     int jj = jx - XT.getBlockColOffset (bj);
                     for (int i=0; i<blk.rowSize(); i++) {
                        subblk.set (i, 0, blk.get(i, jj));
                     }
                     HT.addBlock (bi, colHT, subblk);
                  }
               }
               colHT++;
            }
         }

         int sizeA = sizeM + HT.colSize();
         int ncolsH = HT.colSize();
         int nvals = M.numNonZeroVals (
            Partition.UpperTriangular, sizeM, sizeM);
         nvals += HT.numNonZeroVals (Partition.Full, sizeM, ncolsH);
         nvals += sizeA - sizeM;  // diagonal block

         int[] rowOffs = new int[sizeA+1];
         int[] colIdxs = new int[nvals];
         double[] values = new double[nvals];
         int[] localOffs = new int[sizeM];

         M.addNumNonZerosByRow (
            localOffs, 0, Partition.UpperTriangular, sizeM, sizeM);
         HT.addNumNonZerosByRow (
            localOffs, 0, Partition.Full, sizeM, ncolsH);
         int off = 0;
         for (int i=0; i<sizeM; i++) {
            rowOffs[i] = off;
            off += localOffs[i];
         }
         for (int i=sizeM; i<=sizeA; i++) {
            rowOffs[i] = off++;
         }

         for (int i=0; i<sizeM; i++) {
            localOffs[i] = rowOffs[i];
         }
         M.getBlockCRSIndices (
            colIdxs, 0, localOffs, Partition.UpperTriangular, sizeM, sizeM);
         HT.getBlockCRSIndices (
            colIdxs, sizeM, localOffs, Partition.Full, sizeM, ncolsH);
         for (int i=sizeM; i<sizeA; i++) {
            colIdxs[rowOffs[i]] = i;
         }

         for (int i=0; i<sizeM; i++) {
            localOffs[i] = rowOffs[i];
         }
         M.getBlockCRSValues (
            values, localOffs, Partition.UpperTriangular, sizeM, sizeM);
         HT.getBlockCRSValues (
            values, localOffs, Partition.Full, sizeM, ncolsH);
         for (int i=sizeM; i<sizeA; i++) {
            values[rowOffs[i]] = -Rh.get(i-sizeM);
         }

         // stubs
         VectorNd vel = new VectorNd(sizeM);
         VectorNd lam = new VectorNd(sizeG);
         VectorNd the = new VectorNd(sizeN);
         VectorNd phi = new VectorNd(sizeD);
         VectorNd flim = new VectorNd(sizeD);
         for (int i=0; i<sizeD; i++) {
            flim.set (i, 1);
         }

         VectorNd bm = new VectorNd(sizeM);
         VectorNd bg = new VectorNd(sizeG);
         VectorNd bn = new VectorNd(sizeN);
         VectorNd bd = new VectorNd(sizeD);

         MurtyMechSolver murty = new MurtyMechSolver();
         murty.setMGVariables (M, sizeM, bm, -1, GT, Rg, bg, vel, lam);
         murty.setNVariables (NT, Rn, bn, the);
         murty.setDVariables (DT, Rd, bd, phi, null);
         murty.setFrictionLimits (flim);
         murty.rebuildOrUpdateA (new ArrayList<>(), stateN, stateD);
         murty.getAValues (null, /*forAnalyze=*/false);
         CRSValues crs = murty.getAMatrix();
         CRSValues chk = new CRSValues (rowOffs, colIdxs, values, sizeA);

         checkEquals (crs, chk);
      }
   }

   void createSystemA (
      MatrixNd A, VectorNd b, 
      SparseBlockMatrix M, int sizeM, VectorNd bm,
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg,
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD) {
      
      int sizeG = (GT != null ? GT.colSize() : 0);
      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      int numBasic = 0;
      for (int i=0; i<stateN.size(); i++) {
         if (stateN.get(i) == Z) {
            numBasic++;
         }
      }
      for (int i=0; i<stateD.size(); i++) {
         if (stateD.get(i) == Z) {
            numBasic++;
         }
      }
      int sizeMG = sizeM + sizeG;
      int sizeA = sizeMG + numBasic;

      A.setSize (sizeA, sizeA);
      b.setSize (sizeA);
      if (sizeM < M.rowSize()) {
         MatrixNd Msub = new MatrixNd (sizeM, sizeM);
         M.getSubMatrix (0, 0, Msub);
         A.setSubMatrix (0, 0, Msub);
      }
      else {
         A.setSubMatrix (0, 0, M);
      }
      for (int i=0; i<sizeM; i++) {
         b.set (i, bm.get(i));
      }
      if (sizeG > 0) {
         MatrixNd X = new MatrixNd(GT);
         X.setSize (sizeM, sizeG);
         A.setSubMatrix (0, sizeM, X);
         X.transpose();
         A.setSubMatrix (sizeM, 0, X);
         for (int i=0; i<sizeG; i++) {
            if (Rg != null) {
               A.set (sizeM+i, sizeM+i, -Rg.get(i));
            }
            b.set (sizeM+i, bg.get(i));
         }
      }
      VectorNd col = new VectorNd();
      int ia = sizeMG;
      if (sizeN > 0) {
         for (int i=0; i<sizeN; i++) {
            if (stateN.get(i) == Z) {
               NT.getColumn (i, col);
               col.setSize (sizeA);
               for (int j=sizeM; j<sizeA; j++) {
                  col.set (j, 0);
               }
               if (Rn != null) {
                  col.set (ia, -Rn.get(i));
               }
               A.setColumn (ia, col);
               A.setRow (ia, col);
               b.set (ia, bn.get(i));
               ia++;
            }
         }
      }
      if (sizeD > 0) {
         for (int i=0; i<sizeD; i++) {
            if (stateD.get(i) == Z) {
               DT.getColumn (i, col);
               col.setSize (sizeA);
               for (int j=sizeM; j<sizeA; j++) {
                  col.set (j, 0);
               }
               if (Rd != null) {
                  col.set (ia, -Rd.get(i));
               }
               A.setColumn (ia, col);
               A.setRow (ia, col);
               b.set (ia, bd.get(i));
               ia++;
            }
         }
      }
      // System.out.println ("A=\n" + A.toString("%12.8f"));
      // MatrixNd D = new MatrixNd (DT);
      // D.transpose();
      // System.out.println ("D=\n" + D.toString("%12.8f"));
   }

   double solveBasic (
      MatrixNd A, VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi, 
      SparseBlockMatrix M, int sizeM, VectorNd bm,
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg,
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD,
      VectorNd flim) {

      int sizeG = (GT != null ? GT.colSize() : 0);
      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      VectorNd b = new VectorNd();
      createSystemA (
         A, b, M, sizeM, bm, GT, Rg, bg, NT, Rn, bn, stateN, DT, Rd, bd, stateD);

      int sizeA = A.rowSize();

      VectorNd x = new VectorNd (sizeA);
      LUDecomposition lud = new LUDecomposition();

      if (sizeD > 0) {
         // compute force offset
         for (int i=0; i<sizeD; i++) {
            if (stateD.get(i) == W_LO) {
               phi.set (i, -flim.get(i));
            }
            else if (stateD.get(i) == W_HI) {
               phi.set (i, flim.get(i));
            }
            else {
               phi.set (i, 0);
            }
         }
         VectorNd foffset = new VectorNd(sizeA);
         DT.mul (foffset, phi, sizeM, DT.colSize());
         b.add (foffset);
      }
      lud.factor (A);
      lud.solve (x, b);
      //System.out.println ("x=\n" + x.toString("%12.8f"));
      int ia = 0;
      for (int i=0; i<sizeM; i++) {
         vel.set (i, x.get(ia++));
      }
      for (int i=0; i<sizeG; i++) {
         lam.set (i, -x.get(ia++));
      }
      the.setZero();
      for (int i=0; i<sizeN; i++) {
         if (stateN.get(i) == Z) {
            the.set (i, -x.get(ia++));
         }
      }
      phi.setZero();
      for (int i=0; i<sizeD; i++) {
         if (stateD.get(i) == Z) {
            phi.set (i, -x.get(ia++));
         }
         else if (stateD.get(i) == W_LO) {
            phi.set (i, -flim.get(i));
         }
         else {
            phi.set (i, flim.get(i));
         }
      }
      return lud.conditionEstimate(A);
   }

   ArrayList<Pivot> createPivots (
      VectorNi newstateN, VectorNi curstateN, VectorNi stateNA,
      VectorNi newstateD, VectorNi curstateD, VectorNi stateDA) {

      ArrayList<Pivot> pivots = new ArrayList<>();

      int sizeN = newstateN.size();
      int sizeD = newstateD.size();
      int acnt = 0;
      for (int i=0; i<sizeN; i++) {
         int ia = -1;
         if (stateNA.get(i) == Z) {
            ia = acnt++;
         }
         if (newstateN.get(i) != curstateN.get(i)) { 
            pivots.add (
               new Pivot (i, ia, curstateN.get(i), newstateN.get(i), TYPE_N));
         }
      }
      for (int i=0; i<sizeD; i++) {
         int ia = -1;
         if (stateDA.get(i) == Z) {
            ia = acnt++;
         }
         if (newstateD.get(i) != curstateD.get(i)) { 
            pivots.add (
               new Pivot (i, ia, curstateD.get(i), newstateD.get(i), TYPE_D));
         }
      }
      return pivots;
   }

   protected void testBasisSolves (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg, 
      SparseBlockMatrix NT, VectorNd Rn, SparseBlockMatrix DT, VectorNd Rd)  {

      int sizeG = (GT != null ? GT.colSize() : 0);
      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      int numtests = 10;
      VectorNd bm = new VectorNd (sizeM);
      VectorNd bg = new VectorNd (sizeG);
      VectorNd bn = new VectorNd (sizeN);
      VectorNd bd = new VectorNd (sizeD);
      VectorNd flim = new VectorNd (sizeD);
      for (int i=0; i<sizeD; i++) {
         flim.set (i, 0.5);
      }
      

      bm.setRandom();
      bg.setRandom();
      bn.setRandom();
      bd.setRandom();

      VectorNd vel = new VectorNd (sizeM);
      VectorNd lam = new VectorNd (sizeG);
      VectorNd the = new VectorNd (sizeN);
      VectorNd phi = new VectorNd (sizeD);

      VectorNd velChk = new VectorNd (sizeM);
      VectorNd lamChk = new VectorNd (sizeG);
      VectorNd theChk = new VectorNd (sizeN);
      VectorNd phiChk = new VectorNd (sizeD);

      for (int k0=0; k0<numtests; k0++) {
         VectorNi stateNA = createRandomNState (sizeN);
         VectorNi stateDA = createRandomDState (sizeD);

         MurtyMechSolver murty = new MurtyMechSolver();
         murty.setMGVariables (M, sizeM, bm, -1, GT, Rg, bg, vel, lam);
         murty.setNVariables (NT, Rn, bn, the);
         murty.setDVariables (DT, Rd, bd, phi, null);
         murty.setFrictionLimits (flim);

         murty.updateAndSolveA(stateNA, stateDA);
         CRSValues crs = murty.getAMatrix();
         crs.incrementIndices();

         MatrixNd Achk = new MatrixNd();

         double cond = solveBasic (
            Achk, velChk, lamChk, theChk, phiChk, M, sizeM, bm,
            GT, Rg, bg, NT, Rn, bn, stateNA, DT, Rd, bd, stateDA, flim);

         MatrixNd A = new MatrixNd(Achk.rowSize(), Achk.colSize());
         A.setCRSValues (
            crs.getValues(), crs.getColIdxs(), crs.getRowOffs(),
            crs.numNonZeros(), crs.rowSize(), Matrix.Partition.UpperTriangular);

         if (!A.equals(Achk)) {
            System.out.println ("A=\n" + A.toString("%6.3f"));
            System.out.println ("A expected=\n" + Achk.toString("%6.3f"));
            throw new TestException ("A matrix differs from expected value");
         }
         MatrixNd AT = new MatrixNd();
         AT.transpose (A);
         if (!A.equals(AT)) {
            throw new TestException ("A matrix is not symmetric");
         }

         //crs.write ("A.txt");
         murty.solveForBasicVariables (vel, lam, the, phi);

         double eps = 1e-12*cond;
         checkNormedEquals ("vel", vel, velChk, eps);
         checkNormedEquals ("lam", lam, lamChk, eps);
         checkNormedEquals ("the", the, theChk, eps);
         checkNormedEquals ("phi", phi, phiChk, eps);

         VectorNi curstateN = new VectorNi(sizeN);
         VectorNi curstateD = new VectorNi(sizeD);
         murty.getStateN (curstateN);
         murty.getStateD (curstateD);

         for (int k1=0; k1<2; k1++) {
            VectorNi newstateN = createRandomStateChange (curstateN);
            VectorNi newstateD = createRandomStateChange (curstateD);

            ArrayList<Pivot> pivots = createPivots (
               newstateN, curstateN, stateNA,
               newstateD, curstateD, stateDA);

            if ((k0 % 2) == 0) {
               murty.applyBlockPivots (pivots);
            }
            else {
               while (pivots.size() > 0) {
                  murty.applySinglePivot (pivots);
                  pivots.remove (pivots.size()-1);
               }
            }
            
            murty.getStateN (curstateN);
            murty.getStateD (curstateD);

            // MatrixNd Jchk = new MatrixNd();
            // Jchk.mulTransposeRight (A, murty.myJSolve);
            // Jchk.transpose();
            // Jchk.sub (murty.myJ);
            // System.out.println ("Jsolve err=" + Jchk.frobeniusNorm());

            cond = solveBasic (
               Achk, velChk, lamChk, theChk, phiChk, M, sizeM, bm,
               GT, Rg, bg, NT, Rn, bn, newstateN, DT, Rd, bd, newstateD, flim);

            murty.solveForBasicVariables (vel, lam, the, phi);

            eps = 1e-11*cond;
            checkNormedEquals ("vel", vel, velChk, eps);
            checkNormedEquals ("lam", lam, lamChk, eps);
            checkNormedEquals ("the", the, theChk, eps);
            checkNormedEquals ("phi", phi, phiChk, eps);
         }
      }
   }

   static String stateToString (int[] state, int sizeN) {
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<state.length; i++) {
         if (i == sizeN) {
            sb.append ("| ");
         }
         if (state[i] == Z) {
            sb.append ("Z ");
         }
         else if (state[i] == W_LO) {
            sb.append ("L ");
         }
         else if (state[i] == W_HI) {
            sb.append ("H ");
         }
      }
      return sb.toString();
   }

   protected void testColumnExtraction (SparseBlockMatrix XT) {
      MurtyMechSolver murty = new MurtyMechSolver();
      // myBlkSizeM needs to be set for getNDTColumn() to work
      murty.myBlkSizeM = XT.numBlockRows();
      for (int j=0; j<XT.colSize(); j++) {
         VectorNd chk = new VectorNd(XT.rowSize());
         VectorNd col = new VectorNd(XT.rowSize());
         XT.getColumn (j, chk);
         HRowData rowData = new HRowData(null);
         murty.getNDTColumn (rowData, XT, j);
         rowData.getVec (col);
         checkEquals ("Extracted column "+j, col, chk);
      }
   }

   protected void testAssembleA() {
      int[] rowSizes = new int[] { 3, 3, 3, 3, 3, 6, 6 };

      SparseBlockMatrix M =
         new SparseBlockMatrix (rowSizes, rowSizes);

      for (int i=0; i<M.numBlockRows(); i++) {
         addRandomSymmetric (M, i, i);
      }
      addRandomSymmetric (M, 0, 1);
      addRandomSymmetric (M, 0, 3);
      addRandomSymmetric (M, 0, 5);
      addRandomSymmetric (M, 1, 2);
      addRandomSymmetric (M, 1, 4);
      addRandomSymmetric (M, 1, 6);
      addRandomSymmetric (M, 3, 5);
      addRandomSymmetric (M, 4, 6);

      for (int k=0; k<10; k++) {
         SparseBlockMatrix GT =
            new SparseBlockMatrix(rowSizes, new int[] {1, 2, 3});
         SparseBlockMatrix NT =
            new SparseBlockMatrix(rowSizes, new int[] {1, 2, 3, 1, 1, 1});
         SparseBlockMatrix DT =
            new SparseBlockMatrix(rowSizes, new int[] {1, 2, 2, 3, 1, 2});

         //GT.setVerticallyLinked(true);
         NT.setVerticallyLinked(true);
         DT.setVerticallyLinked(true);

         VectorNd Rg = new VectorNd(GT.colSize());
         VectorNd Rn = new VectorNd(NT.colSize());
         VectorNd Rd = new VectorNd(DT.colSize());

         Rg.setRandom(0, 1.0);
         Rn.setRandom(0, 1.0);
         Rd.setRandom(0, 0.0001);

         int minBlkRow = M.numBlockRows()-3;
         addRandomBlocks (GT, minBlkRow);
         addRandomBlocks (NT, minBlkRow);
         addRandomBlocks (DT, minBlkRow);

         int sizeM = M.colSize();
      
         testAssembleA (M, sizeM, GT, Rg, NT, Rn, DT, Rd);
         testAssembleA (M, sizeM, GT, null, NT, null, DT, Rd);
         testAssembleA (M, sizeM, null, null, NT, null, DT, Rd);
         testAssembleA (M, sizeM, null, null, NT, Rn, null, null);
         testAssembleA (M, sizeM, null, null, null, null, DT, Rd);
         testAssembleA (M, sizeM, null, null, null, null, null, null);
         testAssembleA (M, sizeM, GT, Rg, null, null, null, null);
         testAssembleA (M, sizeM, GT, Rg, NT, Rn, null, null);
         testAssembleA (M, sizeM-6, GT, Rg, NT, Rn, DT, Rd);
         testAssembleA (M, sizeM-6, GT, null, NT, null, null, null);
         testAssembleA (M, sizeM-12, GT, Rg, NT, Rn, DT, Rd);

         testColumnExtraction (NT);
         testColumnExtraction (DT);
      }
   }

   void setContactProblemFromState (
      VectorNd vel, VectorNd the, VectorNd phi,
      VectorNd bm, VectorNd bn, VectorNd bd,
      SparseBlockMatrix M, int sizeM,
      SparseBlockMatrix NT, VectorNd Rn, SparseBlockMatrix DT, VectorNd Rd, 
      String stateStr, double mu) {

      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      if (sizeN > 0) {
         the.setSize (sizeN);
         bn.setSize (sizeN);
      }
      if (sizeD > 0) {
         phi.setSize (sizeD);
         bd.setSize (sizeD);
      }

      VectorNi state = LCPSolver.stringToState (stateStr);
      if (state.size() != sizeN + sizeD) {
         throw new IllegalArgumentException (
            "stateStr size="+state.size()+"; sizeN+sizeD=" + (sizeN + sizeD));
      }

      for (int i=0; i<sizeN; i++) {
         if (state.get(i) == Z) {
            the.set (i, RandomGenerator.nextDouble (0.1, 1.0));
         }
         else {
            the.set (i, 0);
         }
      }
      for (int i=0; i<sizeD; i++) {
         if (state.get(sizeN+i) == Z) {
            phi.set (i, mu*RandomGenerator.nextDouble (0.5, 0.99));
         }
         else if (state.get(sizeN+i) == W_LO) {
            phi.set (i, -mu);
         }
         else {
            phi.set (i, mu);
         }
      }
      VectorNd fm = new VectorNd (sizeM);
      bm.setSize (sizeM);
      bm.setZero();
      fm.set (bm);
      if (sizeN > 0) {
         NT.mulAdd (fm, the, sizeM, sizeN);
      }
      if (sizeD > 0) {
         DT.mulAdd (fm, phi, sizeM, sizeD);
      }
      fm.sub (bm, fm);
      CholeskyDecomposition chol = new CholeskyDecomposition(M);
      chol.solve (vel, fm);
      if (sizeN > 0) {
         NT.mulTranspose (bn, vel);
         bn.scaledAdd (Rn.get(0), the);
      }
      if (sizeD > 0) {
         DT.mulTranspose (bd, vel);
         bd.scaledAdd (Rd.get(0), phi);
      }
   }

   TestProblem createContactProblem (
      double mass, double stiffness, RotationMatrix3d[] C,
      int nparts, String cstrN,
      int[] cidxsN, double mu, TestProblem prevProb) {
      return createContactProblem (
         mass, stiffness, C, nparts, 0, null, null, cidxsN, cstrN, mu,
         prevProb);
   }

   VectorNi createPrevIdxs (int[] cidxs, int[] prevCidxs, int maxi) {
      VectorNi pidxs = new VectorNi (cidxs.length);
      if (prevCidxs == null) {
         pidxs.setAll (-1);
      }
      else {
         int[] backIdxs = new int[maxi];
         for (int i=0; i<maxi; i++) {
            backIdxs[i] = -1;
         }
         for (int i=0; i<prevCidxs.length; i++) {
            backIdxs[prevCidxs[i]] = i;
         }
         for (int i=0; i<pidxs.size(); i++) {
            pidxs.set (i, backIdxs[cidxs[i]]);
         }
      }
      return pidxs;
   }

   void setPrevIdxs (
      VectorNi pidxs, int nfixed, int[] cidxs, int[] prevCidxs, int maxi) {
      int[] backIdxs = new int[maxi];
      for (int i=0; i<maxi; i++) {
         backIdxs[i] = -1;
      }
      for (int i=0; i<prevCidxs.length; i++) {
         backIdxs[prevCidxs[i]] = i;
      }
      int k = 0;
      for (int i=0; i<nfixed; i++) {
         pidxs.set (k++, i);
      }
      for (int i=0; i<cidxs.length; i++) {
         int previ = backIdxs[cidxs[i]];
         pidxs.set (k++, previ==-1 ? -1 : previ+nfixed);
      }
   }

   void setFromPrev (
      VectorNd lam, VectorNd prevLam, VectorNi pidxs) {

      for (int i=0; i<lam.size(); i++) {
         int previ = pidxs.get(i);
         if (previ != -1) {
            lam.set (i, prevLam.get(previ));
         }
         else {
            lam.set (i, 0);
         }
      }
   }

   TestProblem createContactProblem (
      double mass, double stiffness, RotationMatrix3d[] C,
      int nparts, int nfixedG, int[] cidxsG, String cstrG,
      int[] cidxs, String cstrN, double mu, TestProblem prevProb) {

      if (cidxs == null) {
         cidxs = new int[0];
      }
      if (cstrN == null) {
         cstrN = new String("");
      }
      if (cidxsG == null) {
         cidxsG = new int[0];
      }
      if (cstrG == null) {
         cstrG = new String("");
      }
      

      TestProblem prob = new TestProblem();
      if (nparts > C.length) {
         throw new IllegalArgumentException (
               "nparts "+nparts+" exceeds contact supply");
      }
      
      // maximum contact index
      for (int i : cidxsG) {
         if (i >= nparts) {
            throw new IllegalArgumentException (
               "G contact index "+i+" exceeds contact supply");
         }
      }
      for (int i : cidxs) {
         if (i >= nparts) {
            throw new IllegalArgumentException (
               "N contact index "+i+" exceeds contact supply");
         }
      }

      // create M as a mass/stiffness matrix, assuming particles connected into
      // a 1D linear spring system
      int[] rowSizes = new int[nparts];
      for (int bi=0; bi<nparts; bi++) {
         rowSizes[bi] = 3;
      }
      prob.myM = new SparseBlockMatrix (rowSizes, rowSizes);
      for (int bi=0; bi<nparts; bi++) {
         Matrix3x3Block blk00 = new Matrix3x3Block();
         blk00.setDiagonal (mass+2*stiffness);
         prob.myM.addBlock (bi, bi, blk00);
         if (bi < nparts-1) {
            Matrix3x3Block blk01 = new Matrix3x3Block();
            Matrix3x3Block blk10 = new Matrix3x3Block();
            blk01.setDiagonal (-stiffness);
            blk10.setDiagonal (-stiffness);
            prob.myM.addBlock (bi, bi+1, blk01);
            prob.myM.addBlock (bi+1, bi, blk10);
         }
      }
      int sizeM = 3*nparts;
      prob.myBm = new VectorNd (sizeM);
      prob.myVel = new VectorNd (sizeM);
      prob.myVelChk = new VectorNd (sizeM);
      prob.myConVel = new VectorNd (sizeM);
      prob.myConVelChk = new VectorNd (sizeM);
      prob.mySizeM = sizeM;

      int sizeG = nfixedG + cidxsG.length;
      prob.myGT = new SparseBlockMatrix (rowSizes, new int[0]);
      for (int bj=0; bj<nfixedG; bj++) {
         for (int bi=0; bi<nparts; bi++) {
            Matrix3x1Block gblk = new Matrix3x1Block ();
            gblk.setRandom();
            prob.myGT.addBlock (bi, bj, gblk);
         }
      }
      for (int k=0; k<cidxsG.length; k++) {
         int ci = cidxsG[k];
         int bj = k+nfixedG;
         RotationMatrix3d R = C[ci];
         Vector3d col = new Vector3d();
         Matrix3x1Block gblk = new Matrix3x1Block ();
         R.getColumn (2, col);
         gblk.set (col);
         prob.myGT.addBlock (ci, bj, gblk);
      }
      prob.myRg = new VectorNd (sizeG);    
      prob.myRg.setAll (0.001);
      prob.myBg = new VectorNd (sizeG);
      prob.myBg.setRandom ();
      prob.myBg.scale (0.0005);
      prob.myLam = new VectorNd (sizeG);
      prob.myLamChk = new VectorNd (sizeG);
      prob.myConLam = new VectorNd (sizeG);
      prob.myConLamChk = new VectorNd (sizeG);
      prob.mySizeG = sizeG;
      prob.myGidxs = cidxsG;
      prob.myNumFixedG = nfixedG;
      prob.myPrevG = new VectorNi (sizeG);
      if (prevProb != null) {
         setPrevIdxs (prob.myPrevG, nfixedG, cidxsG, prevProb.myGidxs, nparts);
         setFromPrev (prob.myLam, prevProb.myLam, prob.myPrevG);
      }
      else {
         prob.myPrevG.setAll (-1);
      }

      prob.myNT = new SparseBlockMatrix (rowSizes, new int[0]);
      int sizeN = cidxs.length;
      for (int k=0; k<sizeN; k++) {
         int ci = cidxs[k];
         int bj = k;
         RotationMatrix3d R = C[ci];
         Vector3d col = new Vector3d();
         Matrix3x1Block nblk = new Matrix3x1Block ();
         R.getColumn (2, col);
         nblk.set (col);
         prob.myNT.addBlock (ci, bj, nblk);
      }
      prob.myRn = new VectorNd (sizeN);     
      prob.myRn.setAll (0.001);
      prob.myBn = new VectorNd (sizeN);
      prob.myBn.setRandom (-0.0001, 0.0001);
      prob.myThe  = new VectorNd (sizeN);
      prob.myTheChk = new VectorNd (sizeN);
      prob.myConThe = new VectorNd (sizeN);
      prob.myConTheChk = new VectorNd (sizeN);
      prob.mySizeN = sizeN;
      prob.myNidxs = cidxs;
      prob.myPrevN = new VectorNi (sizeN);
      if (prevProb != null) {
         setPrevIdxs (prob.myPrevN, 0, cidxs, prevProb.myNidxs, nparts);
         setFromPrev (prob.myThe, prevProb.myThe, prob.myPrevN);
      }
      else {
         prob.myPrevN.setAll (-1);
      }
      int numc = cidxsG.length + cidxs.length;

      int sizeD = 0;
      if (mu != -1) {
         prob.myDT = new SparseBlockMatrix (rowSizes, new int[0]);
         sizeD = 2*(numc);
         prob.myFinfo = new ArrayList<FrictionInfo>();
         for (int cnt=0; cnt<2; cnt++) {
            int[] idxs = (cnt == 0 ? cidxsG : cidxs);
            for (int k=0; k<idxs.length; k++) {
               int ci = idxs[k];
               int bj;
               RotationMatrix3d R = C[ci];
               Vector3d col = new Vector3d();
               Matrix3x2Block dblk = new Matrix3x2Block ();
               R.getColumn (0, col);
               dblk.setColumn (0, col);
               R.getColumn (1, col);
               dblk.setColumn (1, col);
               FrictionInfo fi = new FrictionInfo();
               fi.mu = mu;
               fi.blockSize = 2;

               if (cnt == 0){
                  fi.contactIdx0 = k+nfixedG;
                  fi.flags = FrictionInfo.BILATERAL;
//                  int pi = prob.myPrevG.get(k+nfixedG);
//                  if (pi != -1) {
//                     pi -= prob.myNumFixedG;
//                  }
                  //fi.prevFrictionIdx = (pi == -1 ? -1 : 2*pi);
                  bj = k;
               }
               else {
                  fi.contactIdx0 = k;
//                  int pi = prob.myPrevN.get(k);
//                  fi.prevFrictionIdx = 
//                     (pi == -1 ? -1 : 2*(pi+prevProb.myGidxs.length));
                  fi.flags = 0;
                  bj = k + cidxsG.length;
               }
               fi.blockIdx = bj;
               prob.myDT.addBlock (ci, bj, dblk);
               prob.myFinfo.add (fi);                  
            }
         }
         prob.myRd = new VectorNd (sizeD);
         prob.myRd.setAll (0.0001);
         prob.myBd = new VectorNd (sizeD);
         prob.myBd.setRandom (-0.0001, 0.0001);
         prob.myPhi = new VectorNd (sizeD);
         prob.myPhiChk = new VectorNd (sizeD);
         prob.mySizeD = sizeD;
      }
      prob.myMu = mu;
      prob.myStateN = new VectorNi (sizeN);
      if (prevProb != null) {
         VectorNi stateN = prob.myStateN;
         for (int i=0; i<stateN.size(); i++) {
            int previ = prob.myPrevN.get(i);
            if (previ != -1) {
               stateN.set (i, prevProb.myStateN.get(previ));
            }
            else {
               stateN.set (i, Z); // assume new contact active
            }
         }
      }
      prob.myStateNChk = new VectorNi (sizeN);
      prob.myStateD = new VectorNi (sizeD);
      prob.myStateDChk = new VectorNi (sizeD);

      if (prevProb != null && prob.mySizeD > 0 && prevProb.mySizeD > 0) {
         prob.myDT.setVerticallyLinked (true);
         prevProb.myDT.setVerticallyLinked (true);
         SparseBlockSignature sig = 
            new SparseBlockSignature (prob.myDT, /*vert=*/true);
         SparseBlockSignature prevSig = 
            new SparseBlockSignature (prevProb.myDT, /*vert=*/true);
         int[] prevDIdxs = sig.computePrevColIdxs (prevSig);
         for (int i=0; i<prevDIdxs.length; i++) {
            int pi = prevDIdxs[i];
            if (pi != -1) {
               prob.myStateD.set (i, prevProb.myStateD.get (pi));
            }
         }
      }

      for (int cnt=0; cnt<2; cnt++) {
         int[] idxs = (cnt == 0 ? cidxsG : cidxs);
         String stateStr = (cnt == 0 ? cstrG : cstrN);

         if (idxs.length != stateStr.length()) {
            throw new IllegalArgumentException (
               "index and state strings have different lengths");
         }
         for (int k=0; k<idxs.length; k++) {
            int ci = idxs[k];
            RotationMatrix3d R = C[ci];
            Vector3d z = new Vector3d(); // z (normal) vector
            Vector3d x = new Vector3d(); // x tangent vector
            Vector3d y = new Vector3d(); // y tangent vector
            Vector3d t = new Vector3d(); // tangent vector
            Vector3d f = new Vector3d(); // applied force vector

            R.getColumn (0, x);
            R.getColumn (1, y);
            R.getColumn (2, z);
            // find a random tangent vector
            double s = RandomGenerator.nextDouble (0,1);
            t.combine (s, x, 1-s, y);
            t.normalize();
            switch (stateStr.charAt(k)) {
               case 'Z': {
                  f.scaledAdd (0.5*mu, t, z);
                  f.negate();
                  break;
               }
               case 'S': {
                  f.scaledAdd (2.0*mu, t, z);
                  f.negate();
                  break;
               }
               case ' ': {
                  f.set (z);
                  break;
               }
               default: {
                  throw new IllegalArgumentException (
                     "stateStr: unrecognized character "+stateStr.charAt(k));
               }
            }
            prob.myBm.setSubVector (ci*3, f);
         }
      }
      return prob;
   }

   /**
    * Create contact problem matrices and vectors for contact indices described
    * by {@code cidxs}, using the normal and friction directions described by
    * {@code C}.
    */
   void createContactProblem (
      int sizeM, SparseBlockMatrix NT, VectorNd Rn, VectorNd bn,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd,
      ArrayList<FrictionInfo> finfo, 
      VectorNd bm, String stateStr,
      RotationMatrix3d[] C, double mu, int[] cidxs) {

      NT.removeAllCols();
      for (int bi=0; bi<cidxs.length; bi++) {
         int ci = cidxs[bi];
         RotationMatrix3d R = C[ci];
         Vector3d col = new Vector3d();
         Matrix3x1Block nblk = new Matrix3x1Block ();
         R.getColumn (2, col);
         nblk.set (col);
         NT.addBlock (ci, NT.numBlockCols(), nblk);
      }
      Rn.setSize (cidxs.length);     
      Rn.setAll (0.001);
      bn.setSize (cidxs.length);
      bn.setZero();

      if (DT != null) {
         DT.removeAllCols();
         finfo.clear();
         for (int bi=0; bi<cidxs.length; bi++) {
            int ci = cidxs[bi];
            RotationMatrix3d R = C[ci];
            Vector3d col = new Vector3d();
            Matrix3x2Block dblk = new Matrix3x2Block ();
            R.getColumn (0, col);
            dblk.setColumn (0, col);
            R.getColumn (1, col);
            dblk.setColumn (1, col);
            DT.addBlock (ci, DT.numBlockCols(), dblk);

            FrictionInfo fi = new FrictionInfo ();
            fi.set2D (bi, bi, mu);
            finfo.add (fi);
         }
         Rd.setSize (2*cidxs.length);
         Rd.setAll (0.0001);
         bd.setSize (2*cidxs.length);
         bd.setZero();
      }

      if (stateStr != null) {
         bm.setSize (sizeM);
         bm.setZero();
         if (stateStr.length() != NT.colSize()) {
            throw new IllegalArgumentException (
               "stateStr.length="+stateStr.length()+
               "; NT.colSize()=" + NT.colSize());
         }
         for (int bi=0; bi<stateStr.length(); bi++) {
            int ci = cidxs[bi];
            RotationMatrix3d R = C[ci];
            Vector3d z = new Vector3d(); // z (normal) vector
            Vector3d x = new Vector3d(); // x tangent vector
            Vector3d y = new Vector3d(); // y tangent vector
            Vector3d t = new Vector3d(); // tangent vector
            Vector3d f = new Vector3d(); // applied force vector

            R.getColumn (0, x);
            R.getColumn (1, y);
            R.getColumn (2, z);
            // find a random tangent vector
            double s = RandomGenerator.nextDouble (0,1);
            t.combine (s, x, 1-s, y);
            t.normalize();
            switch (stateStr.charAt(bi)) {
               case 'Z': {
                  f.scaledAdd (0.5*mu, t, z);
                  f.negate();
                  break;
               }
               case 'S': {
                  f.scaledAdd (2.0*mu, t, z);
                  f.negate();
                  break;
               }
               case ' ': {
                  f.set (z);
                  break;
               }
               default: {
                   throw new IllegalArgumentException (
                      "stateStr: unrecognized character "+stateStr.charAt(bi));
               }
            }
            bm.setSubVector (ci*3, f);
         }
      }
   }

   

   boolean testSPD (Matrix M) {
      try {
         MatrixNd MM = new MatrixNd (M);
         new CholeskyDecomposition(MM);
      }
      catch (IllegalArgumentException e) {
         return false;
      }
      return true;
   }

   protected void testBasisSolves() {
      //int[] rowSizes = new int[] { 3, 3, 3, 3, 3, 3 };
      int[] rowSizes = new int[] { 3, 3, 3 };

      SparseBlockMatrix M =
         new SparseBlockMatrix (rowSizes, rowSizes);

      for (int i=0; i<M.numBlockRows(); i++) {
         addRandomSymmetric (M, i, i);
      }
      addRandomSymmetric (M, 0, 1);
      
      while (!testSPD(M)) {
         // make sure matrix is SPD by scaling up the diagonal if needed.  This
         // will work because diagonal blocks are already SPD
         for (int i=0; i<M.numBlockRows(); i++) {
            MatrixBlock blk = M.getBlock(i, i);
            MatrixNd MB = new MatrixNd(blk);
            for (int k=0; k<MB.rowSize(); k++) {
               MB.set (k, k, 2*MB.get(k,k));
            }
            blk.set (MB);
         }
      }

      for (int k=0; k<10; k++) {
         SparseBlockMatrix GT =
            new SparseBlockMatrix(rowSizes, new int[] {1, 1});
         SparseBlockMatrix NT =
            new SparseBlockMatrix(rowSizes, new int[] {1, 1 });
         //new SparseBlockMatrix(rowSizes, new int[] {1, 1, 1, 1});
         SparseBlockMatrix DT =
           new SparseBlockMatrix(rowSizes, new int[] {2});
         // new SparseBlockMatrix(rowSizes, new int[] {2, 2, 2});

         GT.setVerticallyLinked(true);
         NT.setVerticallyLinked(true);
         DT.setVerticallyLinked(true);

         VectorNd Rg = new VectorNd(GT.colSize());
         VectorNd Rn = new VectorNd(NT.colSize());
         VectorNd Rd = new VectorNd(DT.colSize());

         Rg.setRandom(0, 1.0);
         Rn.setRandom(0, 1.0);
         Rd.setRandom(0, 0.0001);

         addFullRandomBlocks (GT);
         addFullRandomBlocks (NT);
         addFullRandomBlocks (DT);

         int sizeM = M.colSize();

         testBasisSolves (M, sizeM, null, null, null, null, null, null);
         testBasisSolves (M, sizeM, GT, Rg, null, null, null, null);
         testBasisSolves (M, sizeM, null, null, NT, Rn, null, null);      
         testBasisSolves (M, sizeM, null, null, null, null, DT, Rd);
         testBasisSolves (M, sizeM, GT, Rg, NT, Rn, null, null);
         testBasisSolves (M, sizeM, null, null, NT, Rn, DT, Rd);
         testBasisSolves (M, sizeM, GT, Rg, null, null, DT, Rd);
         testBasisSolves (M, sizeM, GT, Rg, NT, Rn, DT, Rd);
         testBasisSolves (M, sizeM, GT, null, NT, null, DT, Rd);

         testBasisSolves (M, sizeM-3, GT, Rg, null, null, null, null);
         testBasisSolves (M, sizeM-3, null, null, NT, Rn, null, null);
         testBasisSolves (M, sizeM-3, null, null, null, null, DT, Rd);
         testBasisSolves (M, sizeM-3, GT, Rg, NT, Rn, DT, Rd);

      }
   }

   VectorNi createRandomNState (int sizeN) {
      VectorNi state = new VectorNi(sizeN);
      for (int j=0; j<sizeN; j++) {
         if (myRand.nextBoolean()) {
            state.set (j, Z);
         }
         else {
            state.set (j, W_LO);
         }
      }
      return state;
   }

   VectorNi createRandomDState (int sizeD) {
      VectorNi state = new VectorNi(sizeD);
      for (int j=0; j<sizeD; j++) {
         if (myRand.nextBoolean()) {
            state.set (j, Z);
         }
         else {
            state.set (j, myRand.nextBoolean() ? W_LO : W_HI);
         }
      }
      return state;
   }

   int changeState (int state) {
      if (state == Z) {
         return myRand.nextBoolean() ? W_LO : W_HI;
      }
      else {
         return Z;
      }
   }

   VectorNi createRandomStateChange (VectorNi state) {
      VectorNi newstate = new VectorNi(state);
      boolean changed = false;
      for (int j=0; j<newstate.size(); j++) {
         if (myRand.nextBoolean()) {
            newstate.set(j, changeState (newstate.get(j)));
            changed = true;
         }
      }
      if (!changed && newstate.size() > 0) {
         newstate.set (0, changeState(newstate.get(0)));
      }
      return newstate;
   }

   void createSyntheticSolution (
      MatrixNd A, VectorNd b,
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi, 
      SparseBlockMatrix M, int sizeM, VectorNd bm,
      SparseBlockMatrix GT, VectorNd Rg, VectorNd bg,
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn, VectorNi stateN,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNi stateD,
      VectorNd flim) {

      int sizeG = (GT != null ? GT.colSize() : 0);
      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      createSystemA (
         A, b, M, sizeM, bm, GT, Rg, bg, NT, Rn, bn, stateN, DT, Rd, bd, stateD);

      int sizeA = A.rowSize();
      VectorNd x = new VectorNd(sizeA);
      VectorNd y = new VectorNd(sizeA);

      VectorNd wn = new VectorNd(sizeN);
      VectorNd wd = new VectorNd(sizeD);

      vel.setRandom();
      lam.setRandom();
      int ia = 0;
      for (int i=0; i<sizeM; i++) {
         x.set (ia++, vel.get(i));
      }
      for (int i=0; i<sizeG; i++) {
         x.set (ia++, -lam.get(i));
         
      }
      the.setZero();
      for (int i=0; i<sizeN; i++) {
         if (stateN.get(i) == Z) {
            the.set (i, RandomGenerator.nextDouble (0.1, 1.0));
            x.set (ia++, -the.get(i));
         }
      }
      phi.setZero();
      for (int i=0; i<sizeD; i++) {
         if (stateD.get(i) == W_LO) {
            phi.set (i, -flim.get(i));
         }
         else if (stateD.get(i) == W_HI) {
            phi.set (i, flim.get(i));
         }
      }

      VectorNd dforce = new VectorNd (sizeM);
      if (sizeD > 0) {
         DT.mul (dforce, phi, sizeM, DT.colSize());
      }

      for (int i=0; i<sizeD; i++) {
         if (stateD.get(i) == Z) {
            phi.set (i, RandomGenerator.nextDouble (-flim.get(i), flim.get(i)));
            x.set (ia++, -phi.get(i));
         }
      }

      A.mul (y, x);
      if (sizeN > 0) {
         NT.mulTranspose (wn, vel, NT.colSize(), sizeM);
      }
      if (sizeD > 0) {
         DT.mulTranspose (wd, vel, DT.colSize(), sizeM);
      }

      ia = 0;
      for (int i=0; i<sizeM; i++) {
         double bval = y.get(ia) - dforce.get(i);
         bm.set (i, bval);
         b.set (ia++, bval);
      }
      for (int i=0; i<sizeG; i++) {
         double bval = y.get(ia);
         bg.set (i, bval);
         b.set (ia++, bval);         
      }
      for (int i=0; i<sizeN; i++) {
         if (stateN.get(i) == Z) {
            double bval = y.get(ia);
            bn.set (i, bval);
            b.set (ia++, bval);
         } 
         else {
            bn.set (i, wn.get(i)-RandomGenerator.nextDouble (0.1, 1.0));
         }
      }
      for (int i=0; i<sizeD; i++) {
         if (stateD.get(i) == Z) {
            double bval = y.get(ia);
            bd.set (i, bval);
            b.set (ia++, bval);
         }
         else if (stateD.get(i) == W_LO) {
            bd.set (
               i, wd.get(i)-RandomGenerator.nextDouble (0.1, 1.0));
         }
         else {
            bd.set (
               i, wd.get(i)+RandomGenerator.nextDouble (0.1, 1.0));
         }
      }
   }

   protected void testSolve (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg, 
      SparseBlockMatrix NT, VectorNd Rn, SparseBlockMatrix DT, VectorNd Rd)  {

      int sizeG = (GT != null ? GT.colSize() : 0);
      int sizeN = (NT != null ? NT.colSize() : 0);
      int sizeD = (DT != null ? DT.colSize() : 0);

      int numtests = 10;
      VectorNd bm = new VectorNd (sizeM);
      VectorNd bg = new VectorNd (sizeG);
      VectorNd bn = new VectorNd (sizeN);
      VectorNd bd = new VectorNd (sizeD);

      // bm.setRandom();
      // bg.setRandom();
      // bn.setRandom();
      // bd.setRandom();

      VectorNd vel = new VectorNd (sizeM);
      VectorNd lam = (sizeG > 0 ? new VectorNd (sizeG) : null);
      VectorNd the = (sizeN > 0 ? new VectorNd (sizeN) : null);
      VectorNd phi = (sizeD > 0 ? new VectorNd (sizeD) : null);

      VectorNd velChk = new VectorNd (sizeM);
      VectorNd lamChk = new VectorNd (sizeG);
      VectorNd theChk = new VectorNd (sizeN);
      VectorNd phiChk = new VectorNd (sizeD);

      VectorNd flim = new VectorNd (sizeD);
      for (int i=0; i<sizeD; i++) {
         flim.set (i, 0.5);
      }

      for (int k0=0; k0<numtests; k0++) {
         // compute an initial A state
         VectorNi stateNA = createRandomNState (sizeN);
         VectorNi stateDA = createRandomDState (sizeD);
         VectorNi stateN = new VectorNi (stateNA);
         VectorNi stateD = new VectorNi (stateDA);
         int versionM = 0;

         VectorNi stateNChk = createRandomNState (sizeN);
         VectorNi stateDChk = createRandomDState (sizeD);

         MatrixNd Achk = new MatrixNd();
         VectorNd bchk = new VectorNd();

         createSyntheticSolution (
            Achk, bchk, velChk, lamChk, theChk, phiChk, M, sizeM, bm,
            GT, Rg, bg, NT, Rn, bn, stateNChk, DT, Rd, bd, stateDChk, flim);

         LUDecomposition lud = new LUDecomposition(Achk);
         double cond = lud.conditionEstimate(Achk);

         MurtyMechSolver murty = new MurtyMechSolver();
         murty.setUpdateABetweenSolves (false);
         murty.setBlockPivoting (myBlockPivoting);

         murty.solve (
            vel, lam, the, phi, M, sizeM, bm, versionM, 
            GT, Rg, bg, NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim);

         double eps = 1e-10*cond;

         for (int i=0; i<sizeN; i++) {
            if (stateN.get(i) != stateNChk.get(i)) {
               throw new TestException (
                  "computed N state=" + LCPSolver.stateToString (stateN) +
                  "\nexpected " + LCPSolver.stateToString (stateNChk));
            }
         }
         for (int i=0; i<sizeD; i++) {
            if (stateD.get(i) != stateDChk.get(i)) {
               throw new TestException (
                  "computed D state=" + LCPSolver.stateToString (stateD) +
                  "\nexpected " + LCPSolver.stateToString (stateDChk));
            }
         }

         VectorNd b = new VectorNd();
         MatrixNd A = new MatrixNd();
         createSystemA (
            A, b, M, sizeM, bm, GT, Rg, bg, 
            NT, Rn, bn, stateN, DT, Rd, bd, stateD);

         checkNormedEquals ("vel", vel, velChk, eps);
         if (lam != null) {
            checkNormedEquals ("lam", lam, lamChk, 1e-8);
         }
         if (the != null) {
            checkNormedEquals ("the", the, theChk, eps);
         }
         if (phi != null) {
            checkNormedEquals ("phi", phi, phiChk, eps);
         }

         // check warm start:
         stateNChk.set (stateN);
         stateDChk.set (stateD);

         for (int i=0; i<2; i++) {
            if (i == 0) {
               versionM++;
            }
            murty.solve (
               vel, lam, the, phi, M, sizeM, bm, versionM,
               GT, Rg, bg, NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim);
            checkEquals (
               "A structure changed", murty.myAStructureChanged, i==0);
            int niters = murty.getIterationCount();
            int npivs = murty.getPivotCount();
            if (niters > 1 || npivs > 0) {
               throw new TestException (
                  "Warm resolve required "+niters+
                  " iterations and "+npivs+" pivots");
            }
            if (!stateN.equals (stateNChk)) {
               throw new TestException ("state N changed after warm start");
            }
            if (!stateD.equals (stateDChk)) {
               throw new TestException ("state D changed after warm start");
            }
            checkNormedEquals ("vel", vel, velChk, eps);
            if (lam != null) {
               checkNormedEquals ("lam", lam, lamChk, eps);
            }
            if (the != null) {
               checkNormedEquals ("the", the, theChk, eps);
            }
            if (phi != null) {
               checkNormedEquals ("phi", phi, phiChk, eps);
            }
         }

         // check MGResolve with existing b values
         murty.resolveMG (vel, lam, bm, bg);

         checkNormedEquals ("vel", vel, velChk, eps);
         if (lam != null) {
            checkNormedEquals ("lam", lam, lamChk, eps);
         }

         // check MGResolve with changed b values
         bm.setRandom();
         bg.setRandom();

         // compute new check results
         VectorNd xm = new VectorNd (bm);
         // add friction offsets
         VectorNd colD = new VectorNd(sizeM);
         for (int j=0; j<sizeD; j++) {
            if (stateDChk.get(j) != Z) {
               DT.getColumn (j, colD);
               xm.scaledAdd (phiChk.get(j), colD);
            }
         }
         b.setSubVector (0, xm);
         b.setSubVector (sizeM, bg);
         SignedCholeskyDecomp chol = new SignedCholeskyDecomp(A,sizeM);
         VectorNd y = new VectorNd();
         chol.solve (y, b);
         y.getSubVector (0, velChk);
         y.getSubVector (sizeM, lamChk);
         lamChk.negate();

         murty.resolveMG (vel, lam, bm, bg);

         checkNormedEquals ("vel", vel, velChk, eps);
         if (lam != null) {
            checkNormedEquals ("lam", lam, lamChk, eps);
         }
      }
   }

   public void testSolves() {
      int[] rowSizes = new int[] { 3, 3, 3, 3, 3, 3 };
      //int[] rowSizes = new int[] { 3, 3 };

      SparseBlockMatrix M =
         new SparseBlockMatrix (rowSizes, rowSizes);

      for (int i=0; i<M.numBlockRows(); i++) {
         addRandomSymmetric (M, i, i);
      }
      addRandomSymmetric (M, 0, 1);
      
      while (!testSPD(M)) {
         // make sure matrix is SPD by scaling up the diagonal if needed.  This
         // will work because diagonal blocks are already SPD
         for (int i=0; i<M.numBlockRows(); i++) {
            MatrixBlock blk = M.getBlock(i, i);
            MatrixNd MB = new MatrixNd(blk);
            for (int k=0; k<MB.rowSize(); k++) {
               MB.set (k, k, 2*MB.get(k,k));
            }
            blk.set (MB);
         }
      }

      for (int k=0; k<10; k++) {
         SparseBlockMatrix GT =
            new SparseBlockMatrix(rowSizes, new int[] {1});
         SparseBlockMatrix NT =
            //new SparseBlockMatrix(rowSizes, new int[] {1, 1 });
            new SparseBlockMatrix(rowSizes, new int[] {1, 1, 1, 1});
         SparseBlockMatrix DT =
            //new SparseBlockMatrix(rowSizes, new int[] {2});
            new SparseBlockMatrix(rowSizes, new int[] {2, 2, 2});

         GT.setVerticallyLinked(true);
         NT.setVerticallyLinked(true);
         DT.setVerticallyLinked(true);

         VectorNd Rg = new VectorNd(GT.colSize());
         VectorNd Rn = new VectorNd(NT.colSize());
         VectorNd Rd = new VectorNd(DT.colSize());

         Rg.setRandom(0, 1.0);
         Rn.setRandom(0, 1.0);
         Rd.setRandom(0, 0.0001);

         //System.out.println ("k=" + k);

         addRandomBlocks (GT);
         addRandomBlocks (NT);
         addRandomBlocks (DT);

         int sizeM = M.colSize();

         testSolve (M, sizeM, null, null, null, null, null, null);
         testSolve (M, sizeM, GT, Rg, null, null, null, null);
         testSolve (M, sizeM, null, null, NT, Rn, null, null);      
         testSolve (M, sizeM, null, null, null, null, DT, Rd);
         testSolve (M, sizeM, GT, Rg, NT, Rn, null, null);
         testSolve (M, sizeM, null, null, NT, Rn, DT, Rd);
         testSolve (M, sizeM, GT, Rg, null, null, DT, Rd);
         testSolve (M, sizeM, GT, null, NT, null, DT, Rd);
      }
   }

   public void testHybridSolves() {
      // create a random matrix big enough that the hybrid solve is actually
      // faster than the the direct solve:
      int numBlkRows = 50;
      int[] rowSizes = new int[numBlkRows];
      for (int bi=0; bi<numBlkRows; bi++) {
         rowSizes[bi] = 3;
      }
      SparseBlockMatrix M =
         new SparseBlockMatrix (rowSizes, rowSizes);
      for (int bi=0; bi<M.numBlockRows(); bi++) {
         addRandomSymmetric (M, bi, bi);
      }
      int[] blockIdxs = RandomGenerator.randomSubsequence (numBlkRows*numBlkRows);
      for (int k=0; k<blockIdxs.length; k++) {
         int bi = blockIdxs[k]/numBlkRows;
         int bj = blockIdxs[k]%numBlkRows;
         if (bj > bi) {
            addRandomSymmetric (M, bi, bj, 0.5);
         }
      }
      VectorNd bm = new VectorNd (M.rowSize());
      bm.setRandom();
      
      while (!testSPD(M)) {
         // make sure matrix is SPD by scaling up the diagonal if needed.  This
         // will work because diagonal blocks are already SPD
         for (int i=0; i<M.numBlockRows(); i++) {
            MatrixBlock blk = M.getBlock(i, i);
            MatrixNd MB = new MatrixNd(blk);
            for (int k=0; k<MB.rowSize(); k++) {
               MB.set (k, k, 2*MB.get(k,k));
            }
            blk.set (MB);
         }
      }

      SparseBlockMatrix GT =
         new SparseBlockMatrix(rowSizes, new int[] {2, 2});
      
      GT.setVerticallyLinked(true);
      VectorNd Rg = new VectorNd(GT.colSize());
      VectorNd bg = new VectorNd(GT.colSize());
      Rg.setRandom(0, 1.0);
      addRandomBlocks (GT);

      VectorNd vel = new VectorNd();
      VectorNd lam = new VectorNd();
      MurtyMechSolver solver = new MurtyMechSolver();
      solver.setHybridSolves (true);
      int versionM = 0;
      solver.solve (
         vel, lam, null, null,
         M, M.rowSize(), bm, versionM, GT, Rg, bg,
         null, null, null, (VectorNi)null,
         null, null, null, (VectorNi)null,
         (VectorNd)null);

      VectorNd velChk = new VectorNd (vel);
      VectorNd lamChk = new VectorNd (lam);

      check (
         "hydrid solve count != 0 after first call",
         solver.getHybridSolveCount()==0);

      for (int cnt=1; cnt<=5; cnt++) {
         solver.solve (
            vel, lam, null, null,
            M, M.rowSize(), bm, versionM, GT, Rg, bg, 
         null, null, null, (VectorNi)null,
         null, null, null, (VectorNi)null,
         (VectorNd)null);

         check (
            "hydrid solve count != "+cnt+" after second call",
            solver.getHybridSolveCount()==cnt);
         checkNormedEquals ("hybrid solve of vel", vel, velChk, 1e-10);
         checkNormedEquals ("hybrid solve of lam", lam, lamChk, 1e-10);
      }

      solver.myFakeHybridFail = true;
      solver.solve (
         vel, lam, null, null,
         M, M.rowSize(), bm, versionM, GT, Rg, bg, 
         null, null, null, (VectorNi)null,
         null, null, null, (VectorNi)null,
         (VectorNd)null);

      check (
         "hydrid solve count != 0 after fake failed call",
         solver.getHybridSolveCount()==0);
      checkNormedEquals ("failed hybrid solve of vel", vel, velChk, 1e-10);
      checkNormedEquals ("failed hybrid solve of lam", lam, lamChk, 1e-10);
   }

   ArrayList<FrictionInfo> createFrictionInfo (
      int dim, int size, double mu, int flags) {

      ArrayList<FrictionInfo> finfo = new ArrayList<>(size);
      for (int i=0; i<size; i++) {
         FrictionInfo info = new FrictionInfo();
         info.mu = mu;
         info.blockSize = dim;
         info.blockIdx = i;
         info.contactIdx0 = i;
         info.flags = flags;
         finfo.add (info);
      }
      return finfo;
   }

   ArrayList<FrictionInfo> createFrictionInfo (
      VectorNi cidxs, double mu) {

      ArrayList<FrictionInfo> finfo = new ArrayList<>(cidxs.size());
      for (int i=0; i<cidxs.size(); i++) {
         FrictionInfo info = new FrictionInfo();
         info.mu = mu;
         info.blockSize = 2;
         info.blockIdx = i;
         info.contactIdx0 = i;
         //info.prevFrictionIdx = (cidxs.get(i) == -1 ? -1 : 2*cidxs.get(i));
         finfo.add (info);
      }
      return finfo;
   }

   void testFrictionResolve () {
      // test friction resolve for a simple case of a box on a plane with four
      // contact points.

      double mass = 4.0;
      SpatialInertia Inertia =
         SpatialInertia.createBoxInertia (mass, 1.0, 1.0, 1.0);
      Point3d[] pnts =
         new Point3d[] {
            new Point3d ( 0.5,  0.5, -0.5), 
            new Point3d (-0.5,  0.5, -0.5), 
            new Point3d (-0.5, -0.5, -0.5), 
            new Point3d ( 0.5, -0.5, -0.5)
      };
      SparseBlockMatrix M = new SparseBlockMatrix (new int[]{6}, new int[]{6});
      SparseBlockMatrix NT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix DT = new SparseBlockMatrix (new int[]{6}, new int[0]);

      Matrix6dBlock Mblk = new Matrix6dBlock();
      Mblk.set (Inertia);
      M.addBlock (0, 0, Mblk);
      Wrench wr = new Wrench();
      
      for (int j=0; j<pnts.length; j++) {
         Matrix6x1Block Nblk = new Matrix6x1Block();
         wr.f.set (Vector3d.Z_UNIT);
         wr.m.cross (pnts[j], wr.f);
         Nblk.setColumn (0, wr);
         NT.addBlock (0, j, Nblk);

         Matrix6x2Block Dblk = new Matrix6x2Block();
         wr.f.set (Vector3d.X_UNIT);
         wr.m.cross (pnts[j], wr.f);
         Dblk.setColumn (0, wr);

         wr.f.set (Vector3d.Y_UNIT);
         wr.m.cross (pnts[j], wr.f);
         Dblk.setColumn (1, wr);
         DT.addBlock (0, j, Dblk);
      }

      VectorNd Rn = new VectorNd (4);
      for (int i=0; i<Rn.size(); i++) {
         Rn.set (i, 1e-6);
      }
      VectorNd bn = new VectorNd (4);
      

      VectorNd Rd = new VectorNd (8);
      VectorNd bd = new VectorNd (8);      
      ArrayList<FrictionInfo> finfo =
         createFrictionInfo (2, 4, /*mu=*/1.0, 0);
      
      for (int i=0; i<Rd.size(); i++) {
         Rd.set (i, 1e-6);
      }
      VectorNd vel = new VectorNd();
      VectorNd the = new VectorNd();
      VectorNd phi = new VectorNd();

      Wrench fm = new Wrench (1, 0, -1, 0, 0, 0);
      VectorNd bm = new VectorNd(6);
      bm.set (fm);

      VectorNi stateN = new VectorNi(4);
      VectorNi stateD = new VectorNi(8);

      // figure out bn values required to account for regularization
      // solve redundant unregularized contact problem using
      //
      // N inv(M) N^T the = N inv(M) bm
      // 
      // and then set bn to  -Rn the

      MatrixNd N = new MatrixNd(NT);
      N.transpose();
      MatrixNd MI = new MatrixNd (Inertia);
      MI.invert();
      VectorNd b = new VectorNd (NT.colSize());
      MI.mul (b, bm);
      N.mul (b, b);
      b.setSize (4);
      MatrixNd A = new MatrixNd();
      A.mulTransposeRight (MI, N);
      A.mul (N, A);

      SVDecomposition svd = new SVDecomposition(A);
      svd.solve (the, b, 1e-12);
      bn.scale (-Rn.get(0), the);

      Status status = null;

      VectorNd theChk =
         new VectorNd(new double[] { 0.25, 0.25, 0.25, 0.25 });
      VectorNd velChk =
         new VectorNd(new double[] { 0.25, 0.0, 0.0, 0.0, 0.0, 0.0 });
      VectorNi stateNChk = LCPSolver.stringToState ("ZZZZ");
      VectorNi stateDChk = LCPSolver.stringToState ("LLLLLLLL");

      MurtyMechSolver solver = new MurtyMechSolver();
      
      // start with a contact only solve
      status = solver.contactSolve (
         vel, null, the, M, 6, bm, -1, null, null, null, 
         NT, Rn, bn, stateN);  

      checkEquals ("status for contact solve", status, Status.SOLVED);
      checkEquals ("velocity for contact solve", vel, velChk, 1e-6);
      checkEquals ("the for contact solve", the, theChk, 1e-6);
      checkEquals ("state N for contact solve", stateN, stateNChk);

      // now apply a two-step friction solve

      VectorNd theChkF =
         new VectorNd(new double[] { 0.5, 0.0, 0.0, 0.5 });
      VectorNd velChkF = new VectorNd (6); // zero
      VectorNd phiChkF =
         new VectorNd(new double[] { -0.5, 0.0, 0.0, 0.0, 0.0, 0.0, -0.5, 0.0});

      status = solver.solve (
         vel, null, the, phi, M, 6, bm, -1, null, null, null, NT, Rn,
         bn, stateN, DT, Rd, bd, stateD, finfo, /*frictionIters=*/1, 0);

      checkEquals ("status for friction solve", status, Status.SOLVED);
      checkEquals ("velocity for friction solve", vel, velChkF, 1e-6);
      checkEquals ("the for friction solve", the, theChkF, 2e-6);
      checkEquals ("phi for friction solve", phi, phiChkF, 5e-6);

      // now repeat the same thing, using a two step friction solve and
      // unitialized state data:
      the.setZero();
      LCPSolver.clearState (stateN);
      LCPSolver.clearState (stateD);
      status = solver.solve (
         vel, null, the, phi, M, 6, bm, -1, null, null, null, NT, Rn,
         bn, stateN, DT, Rd, bd, stateD, finfo, /*frictionIters=*/2, 0);

      checkEquals ("status for friction solve", status, Status.SOLVED);
      checkEquals ("velocity for friction solve", vel, velChkF, 1e-6);
      checkEquals ("the for friction solve", the, theChkF, 2e-6);
      checkEquals ("phi for friction solve", phi, phiChkF, 5e-6);
   }

   public void testPegInHole (int nz, int nr, int ntests) {
      for (int i=0; i<ntests; i++) {
         Vector3d force = new Vector3d();
         force.setRandom();
         double mu = RandomGenerator.nextDouble (0.2, 1.0);
         testPegInHole (nz, nr, force, mu);
      }
   }

   void checkContactSolution (
      VectorNd vel, VectorNd the,
      SparseBlockMatrix M, VectorNd bm, 
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn,
      VectorNi state, double tol) {

      MatrixNd A = new MatrixNd();
      VectorNd q = new VectorNd();
      LCPSolverTestBase.buildLCP (A, q, M, bm, NT, Rn, bn, null, null, null);

      LCPSolverTestBase.checkLCPSolution (the, A, q, state, tol);
   }

   void checkFrictionSolution (
      VectorNd vel, VectorNd the, VectorNd phi,
      SparseBlockMatrix M, VectorNd bm, 
      SparseBlockMatrix NT, VectorNd Rn, VectorNd bn,
      SparseBlockMatrix DT, VectorNd Rd, VectorNd bd, VectorNd flim,
      VectorNi state, VectorNi state0, boolean isNTActive, double tol) {

      MatrixNd A = new MatrixNd();
      VectorNd q = new VectorNd();
      LCPSolverTestBase.buildLCP (A, q, M, bm, NT, Rn, bn, DT, Rd, bd);

      int sizeN = NT.colSize();
      int sizeND = sizeN + DT.colSize();

      int n = A.rowSize();
      VectorNd lo = new VectorNd (n);
      VectorNd hi = new VectorNd (n);
      for (int i=0; i<sizeN; i++) {
         if (isNTActive) {
            hi.set (i, INF);
            lo.set (i, 0);
         }
         else if (state0.get(i) == Z) {
            hi.set (i, INF);
            lo.set (i, -INF);
         }
         else  {
            hi.set (i, 0);
            lo.set (i, 0);
         }
      }
      for (int i=sizeN; i<sizeND; i++) {
         hi.set (i,  flim.get(i-sizeN));
         lo.set (i, -flim.get(i-sizeN));
      }

      VectorNd z = new VectorNd (sizeND);
      VectorNd w = new VectorNd (sizeND);

      z.setSubVector (0, the);
      z.setSubVector (sizeN, phi);

      A.mul (w, z);
      w.add (q);

      LCPSolverTestBase.checkBLCPSolution (
         z, w, A, q, lo, hi, 0, state, tol);

      // check against the regular MurtySolver

      VectorNd zchk = new VectorNd(n);
      VectorNd wchk = new VectorNd(n);
      VectorNi stateChk = new VectorNi(n);
      stateChk.set (state0);

      MurtyLCPSolver murtyLCP = new MurtyLCPSolver();
      murtyLCP.setTolerance (tol);
      murtyLCP.setBlockPivoting(true);
      murtyLCP.solve (zchk, wchk, stateChk, A, q, lo, hi, 0);
      // if (!murtyLCP.getLastBlockPivoting()) {
      //    System.out.println ("Murty pivots OFF");
      // }

      checkNormedEquals ("z vs. MurtyLCPSolver", z, zchk, 1e-10);
      checkNormedEquals ("w vs. MurtyLCPSolver", w, wchk, 1e-10);
      checkEquals ("state vs. MurtyLCPSolver", state, stateChk);
   }

   /**
    * Check the pivoting efficiency using the redundant contacts
    * generated by a pegInHole simulation.
    */
   void checkBlockPivots () {

      int nz = 5;
      int nr = 7;
      int ntests = 1000;

      Vector3d force = new Vector3d();
      SparseBlockMatrix M = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix NT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix DT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      VectorNd bm = new VectorNd();
      VectorNd Rn = new VectorNd();
      VectorNd Rd = new VectorNd();

      LCPSolverTestBase.createPegInHoleProblem (
         M, bm, NT, Rn, DT, Rd, nz, nr, force, /*regularize=*/true);

      int numf = DT.numBlockCols();
      ArrayList<FrictionInfo> finfo = createFrictionInfo (2, numf, /*mu=*/0, 0);

      MurtyMechSolver solver = new MurtyMechSolver();
      int numc = NT.colSize();
      VectorNd bn = new VectorNd(numc);
      VectorNd bd = new VectorNd(2*numc);
      VectorNd vel = new VectorNd();
      VectorNd the = new VectorNd();
      VectorNd phi = new VectorNd();
      VectorNi stateN = new VectorNi(numc);
      VectorNi stateD = new VectorNi(2*numc);

      System.out.println (
         "Contact problem with " +NT.colSize() + " contacts");

      for (int cnt=0; cnt<2; cnt++) {
         if (cnt > 0) {
            solver.setNTFrictionActivity (false);
         }
         
         // first try a problem without warm starts 

         int npivs = 0;
         int niters = 0;
         int blockFails = 0;
         for (int i=0; i<ntests; i++) {
            // update force and mu
            force.setRandom();
            double mu = RandomGenerator.nextDouble (0.2, 1.0);
            for (int j=0; j<numf; j++) {
               finfo.get(j).mu = mu;
            }
            bm.set (new Wrench (force, new Vector3d()));
            LCPSolver.clearState (stateN);
            LCPSolver.clearState (stateD);
            Status status = solver.solve (
               vel, null, the, phi, M, M.rowSize(), bm, -1, null, null,
               null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 1, 0);
            npivs += solver.getPivotCount();
            niters += solver.getIterationCount();
            blockFails += solver.getBlockPivotFailCount();
            if (status != Status.SOLVED) {
               System.out.println ("no solution at i=" + i + ": " + status);
            }
         }
         if (cnt == 0) {
            System.out.printf ("fresh start:\n");
         }
         else {
            System.out.printf ("fresh start, NT fixed:\n");
         }
         System.out.printf (
            " npivs=%g niters=%g blockFails=%g\n",
            npivs/(double)ntests,
            niters/(double)ntests,
            blockFails/(double)ntests);

         // try with warm starts and force and mu slowly varying

         npivs = 0;
         niters = 0;
         blockFails = 0;
         double mu = 0.6;
         Vector3d deltaForce = new Vector3d();
         for (int i=0; i<ntests; i++) {
            deltaForce.setRandom(-0.3, 0.3);
            force.add (deltaForce);
            mu += RandomGenerator.nextDouble (0.0, 0.01);
            for (int j=0; j<numf; j++) {
               finfo.get(j).mu = mu;
            }
            bm.set (new Wrench (force, new Vector3d()));
            Status status = solver.solve (
               vel, null, the, phi, M, M.rowSize(), bm, -1, null, null,
               null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 1, 0);
            npivs += solver.getPivotCount();
            niters += solver.getIterationCount();
            blockFails += solver.getBlockPivotFailCount();
            if (status != Status.SOLVED) {
               System.out.println ("no solution at i=" + i + ": " + status);
            }
         } 
         if (cnt == 0) {
            System.out.printf ("warm start:\n");
         }
         else {
            System.out.printf ("warm start, NT fixed:\n");
         }
         System.out.printf (
            " npivs=%g niters=%g blockFails=%g\n",
            npivs/(double)ntests,
            niters/(double)ntests,
            blockFails/(double)ntests);
      }
   }

   /**
    * Create a test case involving peg-in-hole contact with many contact
    * points and friction.
    * @param nz number of rings of contact points along z
    * @param nr number of contact points about each ring
    * @param force to apply to the center of the peg
    * @param mu friction coefficient
    */
   public void testPegInHole (
      int nz, int nr, Vector3d force, double mu) {

      SparseBlockMatrix M = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix NT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      SparseBlockMatrix DT = new SparseBlockMatrix (new int[]{6}, new int[0]);
      VectorNd bm = new VectorNd();
      VectorNd Rn = new VectorNd();
      VectorNd Rd = new VectorNd();

      LCPSolverTestBase.createPegInHoleProblem (
         M, bm, NT, Rn, DT, Rd, nz, nr, force, /*regularize=*/true);

      MurtyMechSolver solver = new MurtyMechSolver();
      int numc = NT.colSize();
      VectorNd bn = new VectorNd(numc);
      VectorNd bd = new VectorNd(2*numc);
      VectorNd vel = new VectorNd();
      VectorNd the = new VectorNd();
      VectorNd phi = new VectorNd();
      VectorNi stateN = new VectorNi(numc);
      VectorNi stateD = new VectorNi(2*numc);
      int versionM = 0;

      Status status;
      boolean isNTActive = true;

      int numf = DT.numBlockCols();
      ArrayList<FrictionInfo> finfo = createFrictionInfo (2, numf, mu, 0);

      // first solve a contact only problem to estimate contact force

      status = solver.contactSolve (
         vel, null, the, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN);
      check ("status != SOLVED", status == Status.SOLVED);
      checkContactSolution (
         vel, the, M, bm, NT, Rn, bn, stateN, solver.getLastSolveTol());

      // now apply a solve without a friction step

      versionM++;
      VectorNi state0 = packState (stateN, stateD);
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 0, 0);
      check ("status != SOLVED", status == Status.SOLVED);

      VectorNd flim = solver.getFrictionLimits();

      VectorNi state = packState (stateN, stateD);
      checkFrictionSolution (
         vel, the, phi, M, bm, NT, Rn, bn, DT, Rd, bd, flim,
         state, state0, isNTActive, solver.getLastSolveTol());

      // do same calculation using a friction iteration:

      VectorNd velChk = new VectorNd(vel);
      VectorNd theChk = new VectorNd(the);
      VectorNd phiChk = new VectorNd(phi);
      VectorNi stateNChk = new VectorNi(stateN);
      VectorNi stateDChk = new VectorNi(stateD);

      LCPSolver.clearState(stateN);
      LCPSolver.clearState(stateD);
      the.setZero();

      versionM++;
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 1, 0);

      check ("status != SOLVED", status == Status.SOLVED);
      String testLabel = "for friction step solve";
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // redo to make sure warm start works:

      flim = solver.getFrictionLimits();
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim, REBUILD_A);

      check ("status != SOLVED", status == Status.SOLVED);
      testLabel = "for warm start";
      checkEquals ("iterations "+testLabel, solver.getIterationCount(), 1);
      checkEquals ("pivots "+testLabel, solver.getPivotCount(), 0);
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // redo with a random start:

      flim = solver.getFrictionLimits();
      stateN = createRandomNState (numc);
      stateD = createRandomDState (2*numc);
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim, REBUILD_A);

      check ("status != SOLVED", status == Status.SOLVED);
      testLabel = "for random start";
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // do same calculation using contactSolve() and frictonSolve():

      //solver.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      LCPSolver.clearState (stateN);
      LCPSolver.clearState (stateD);


      versionM = -1; // renanalyze from here on
      status = solver.contactSolve (
         vel, null, the, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN);

      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo);

      check ("status != SOLVED", status == Status.SOLVED);
      testLabel = "for contact/friction solve";
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // check frictonSolve() warm start:

      state = packState (stateN, stateD);
      VectorNi stateChk = packState (stateNChk, stateDChk);

      LCPSolver.clearState (stateN);
      LCPSolver.clearState (stateD);
      status = solver.contactSolve (
         vel, null, the, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN);
      // get "before" and "after" contact states, since warm start will have
      // zero pivots only if there are the same
      VectorNi contactState = new VectorNi(stateN);
      VectorNi contactStateChk = new VectorNi(stateNChk);

      stateN.set (stateNChk);
      stateD.set (stateDChk);

      flim = solver.getFrictionLimits();
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim, REBUILD_A);

      check ("status != SOLVED", status == Status.SOLVED);
      testLabel = "for contact/friction warm start";
      if (contactState.equals (contactStateChk)) {
         checkEquals ("iterations "+testLabel, solver.getIterationCount(), 1);
         checkEquals ("pivots "+testLabel, solver.getPivotCount(), 0);
      }
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // check frictonSolve() with random start:

      LCPSolver.clearState (stateN);
      status = solver.contactSolve (
         vel, null, the, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN);

      stateN.set (createRandomNState (numc)); // random start
      stateD.set (createRandomDState (2*numc)); // random start

      flim = solver.getFrictionLimits();
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, flim, REBUILD_A);

      check ("status != SOLVED", status == Status.SOLVED);
      testLabel = "for contact/friction random start";
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // check frictonSolve() with contact activity fixed:

      isNTActive = false;
      LCPSolver.clearState (stateN);
      solver.setNTFrictionActivity (false);
      status = solver.contactSolve (
         vel, null, the, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN);
      check ("status != SOLVED", status == Status.SOLVED);
      checkContactSolution (
         vel, the, M, bm, NT, Rn, bn, stateN, solver.getLastSolveTol());

      VectorNi contactState0 = new VectorNi (stateN);
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 0, MurtyMechSolver.NT_INACTIVE);

      check ("status != SOLVED", status == Status.SOLVED);
      VectorNi contactState1 = new VectorNi (stateN);

      // check that Z values in contact state are unchanged
      for (int i=0; i<numc; i++) {
         if ((contactState0.get(i) == Z) != (contactState1.get(i) == Z)) {
            throw new TestException (
               "Z values differ in contactState:\n"+contactState1+
               "\nexpected:\n" + contactState0);
         }
      }
            
      state = packState (stateN, stateD);
      checkFrictionSolution (
         vel, the, phi, M, bm, NT, Rn, bn, DT, Rd, bd, flim,
         state, state0, isNTActive, solver.getLastSolveTol());

      velChk.set (vel);
      theChk.set (the);
      phiChk.set (phi);
      stateNChk.set (stateN);
      stateDChk.set (stateD);

      // repeat using a random starting friction state:

      LCPSolver.clearState (stateN);
      LCPSolver.clearState (stateD);
      status = solver.contactSolve (
         vel, null, the, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN);

      stateD = createRandomDState (2*numc);

      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null, null,
         NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo,
         0, MurtyMechSolver.NT_INACTIVE);

      check ("status != SOLVED", status == Status.SOLVED);

      testLabel = "for friction solve, warm start, NTFrictionActivity false";
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);

      // repeat using a solve with a friction iteration:

      LCPSolver.clearState(stateN);
      LCPSolver.clearState(stateD);
      the.setZero();
      status = solver.solve (
         vel, null, the, phi, M, M.rowSize(), bm, versionM, null, null,
         null, NT, Rn, bn, stateN, DT, Rd, bd, stateD, finfo, 1, 0);

      check ("status != SOLVED", status == Status.SOLVED);
      testLabel = "for friction step solve with NTFrictionActivity false";
      checkNormedEquals ("vel "+testLabel, vel, velChk, 1e-10);
      checkNormedEquals ("the "+testLabel, the, theChk, 1e-10);
      checkNormedEquals ("phi "+testLabel, phi, phiChk, 1e-10);
      checkEquals ("stateN "+testLabel, stateN, stateNChk);
      checkEquals ("stateD "+testLabel, stateD, stateDChk);
   }

   protected void testVaryingContactSmall() {

      int maxparts = 10;
      double mass = 2.0;
      double stiffness = 0.5;
      double mu = 0.5;

      // create orientations for potential contacts and tangents
      RotationMatrix3d[] C = new RotationMatrix3d[maxparts];
      for (int bi=0; bi<maxparts; bi++) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.setRandom();
         C[bi] = R;
      }

      int[] cidxs = new int[] {0, 1, 2};
      TestProblem prob, probN, probG = null;
      int nfixedG = 0;
      MurtyMechSolver murty = new MurtyMechSolver();

      prob = createContactProblem (
         mass, stiffness, C, 10, "Z S", cidxs, mu, null);
      prob.createSolutionCheck ("");

      // do an initial check to form the contact basis:
      prob.debug = true;
      prob.checkSolve (
         "initialize", murty, "", REBUILD_A, 1e-10);
      prob.debug = false;

      // start by just specifying the (unchanged) contact indices:

      prob.checkSolve (
         "with unchanged cidx", murty, "", 0, 1e-10);

      // initialze with N state ZLZ to get H pivots:

      prob.checkSolve (
         "with N state ZLZ", murty, "ZLZ", 0, 1e-10);

      // resolve with N state=ZLZ and prevc=null to initialize A

      prob.checkSolve (
         "with N state ZLZ, prevc=null", murty, "ZLZ", REBUILD_A, 1e-10);

      // resolve with state clear and prevc to get E pivots

      prob.checkSolve (
         "with state clear to get E pivots", murty, "", 0, 1e-10);

      // permute the contacts 

      cidxs = new int[] {2, 0, 1};
      prob = createContactProblem (
         mass, stiffness, C, 10, "SZ ", cidxs, mu, prob);
      prob.createSolutionCheck ("ZZL");
      prob.checkSolve (
         "with permuted contacts", murty, "ZZL", 0, 1e-10);

      // remove an active constraint

      cidxs = new int[] {0, 1};
      prob = createContactProblem (
         mass, stiffness, C, 10, "S ", cidxs, mu, prob);
      prob.createSolutionCheck ("ZL");
      prob.checkSolve (
         "with active contact removed", murty, "ZL", 0, 1e-10);

      // turn the constraint back on again - should result in an H pivot

      cidxs = new int[] {2, 0, 1};
      prob = createContactProblem (
         mass, stiffness, C, 10, "SS ", cidxs, mu, prob);
      prob.createSolutionCheck ("ZZL");
      prob.checkSolve (
         "with active contact restored", murty, "ZZL", 0, 1e-10);

      // permute the constraints again

      cidxs = new int[] {2, 1, 0};
      prob = createContactProblem (
         mass, stiffness, C, 10, "S S", cidxs, mu, prob);
      prob.createSolutionCheck ("ZLZ");
      prob.checkSolve (
         "with permuted contact", murty, "ZLZ", 0, 1e-10);

      // add more constraints, one active, one not

      cidxs = new int[] {2, 1, 0, 4, 5};
      prob = createContactProblem (
         mass, stiffness, C, 10, "Z ZS ", cidxs, mu, prob);
      prob.createSolutionCheck ("ZLZZL");
      prob.checkSolve (
         "with new contacts", murty, "ZLZZL", 0, 1e-10);

      // expand checks to D: resolve with prevc=null to reinitialize A

      cidxs = new int[] {2, 1, 0, 3};
      prob = createContactProblem (
         mass, stiffness, C, 5, "Z ZS", cidxs, mu, null);
      prob.createSolutionCheck ("ZLZZ");
      prob.checkSolve (
         "contact solve, clear state A",
         murty, /*state=*/null, REBUILD_A, 1e-10);

      prob.setFrictionIters (0);
      prob.createSolutionCheck ("ZLZZ", USE_THE);
      prob.checkSolve (
         "full solve, stateA set to prev state",
         murty, /*state=*/null, REBUILD_A|USE_THE, 1e-10);

      // permute contacts

      cidxs = new int[] {1, 3, 0, 2};
      prob = createContactProblem (
         mass, stiffness, C, 5, " SZZ", cidxs, mu, prob);
      prob.createSolutionCheck ("LZZZ", USE_THE);
      prob.checkSolve (
          "permuted contacts", murty, "LZZZ", USE_THE, 1e-10);

      // remove active contact

      cidxs = new int[] {1, 3, 2};
      prob = createContactProblem (
         mass, stiffness, C, 5, " SZ", cidxs, mu, prob);
      prob.createSolutionCheck ("LZZ", USE_THE);
      prob.checkSolve (
          "removed active contact", murty, "LZZ", USE_THE, 1e-10);

      // remove all active contacts
      cidxs = new int[] {};
      prob = createContactProblem (
         mass, stiffness, C, 5, "", cidxs, mu, prob);
      VectorNd theSave = prob.getTheta();
      prob.createContactSolutionCheck ("");
      prob.checkContactSolve (
          "contact solve removing all active contacts", 
          murty, "", USE_THE, 1e-10);
      
      // full solve - make sure D gets reactivated
      prob.setTheta (theSave);
      prob.createSolutionCheck ("", USE_THE);
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      prob.checkSolve (
         "full solve with contacts removed",
         murty, /*state=*/null, USE_THE, 1e-10);      
      
      // add new contacts

      if (false) {
         
      cidxs = new int[] {4, 1, 3, 2, 0};
      prob = createContactProblem (
         mass, stiffness, C, 5, "Z SZ ", cidxs, mu, prob);
      theSave = prob.getTheta();
      prob.createSolutionCheck ("ZLZZL", USE_THE);
      // murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      prob.checkSolve (
         "added new contacts", murty, "ZLZZL", USE_THE, 1e-10);

      // test disabling of friction constraints: contact solve followed by a
      // full solve:

      prob.createContactSolutionCheck ("ZLZZL");
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      prob.checkContactSolve (
         "contact solve", murty, "ZLZZL", 0, 1e-10);
      
      // full solve - make sure D gets reactivated
      prob.setTheta (theSave);
      prob.createSolutionCheck ("ZLZZL", USE_THE);
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      prob.checkSolve (
         "full solve to reactivate D",
         murty, /*state=*/null, REBUILD_A|USE_THE, 1e-10);
      
      // now remove all contacts in the contact phase
      cidxs = new int[] {};
      prob = createContactProblem (
         mass, stiffness, C, 5, "", cidxs, mu, prob);
      
      prob.createContactSolutionCheck ("");
      prob.checkContactSolve (
         "contact solve", murty, "", 0, 1e-10);    
      
      // now do this with G based contact constraints instead of N

      nfixedG = 2;
      cidxs = new int[] {4, 1, 3, 2, 0};
      probG = createContactProblem (
         mass, stiffness, C, 10, nfixedG, cidxs, "ZSSZZ", null, null, mu, null);
      probG.createSolutionCheck ("", 0);
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      probG.checkSolve (
         "G based contact 'ZSSZZ'", murty, "", 0, 1e-10);      
      VectorNd lamG = probG.getLambda();
      VectorNd phiG = probG.getPhi();

      // compare this to a fully contacting N-based problem

      probN = createContactProblem (
         mass, stiffness, C, 10, nfixedG, null, null, cidxs, "ZSSZZ", mu, null);
      probN.myBm.set (probG.myBm);
      
      probG.myBg.getSubVector (0, probN.myBg);
      probG.myBg.getSubVector (nfixedG, probN.myBn);
      probN.myBd.set (probG.myBd);
      probN.setFixedGT (probG);
      probN.createSolutionCheck ("", 0);
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      probN.checkSolve (
         "N based contact 'ZSSZZ'", murty, "", 0, 1e-10);      
      VectorNd lamN = new VectorNd (lamG);
      lamN.setSubVector (nfixedG, probN.getTheta());
      VectorNd phiN = probN.getPhi();

      checkEquals ("lam for G based contact", lamG, lamN, 1e-10);
      checkEquals ("phi for G based contact", phiG, phiN, 1e-10);

      // resolve with new the
      probG.setFrictionIters (0);
      probG.createSolutionCheck (null, USE_THE);
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      probG.checkSolve (
         "G based contact reusing lam", murty, null, REBUILD_A|USE_THE, 1e-10);
      lamG = probG.getLambda();
      phiG = probG.getPhi();     

      probN.setFrictionIters (0);
      probN.createSolutionCheck (null, USE_THE);
      //murty.setDebug (MurtySparseContactSolver.SHOW_PIVOTS);
      probN.checkSolve (
         "N based contact resuing the", murty, null, REBUILD_A|USE_THE, 1e-10);
      lamN = new VectorNd (lamG);
      lamN.setSubVector (nfixedG, probN.getTheta());
      phiN = probN.getPhi();

      checkEquals ("lam for G based contact", lamG, lamN, 1e-10);
      checkEquals ("phi for G based contact", phiG, phiN, 1e-10);
      
      // premute the constraints 

      cidxs = new int[] {3, 2, 4, 0, 1};   

      probG = createContactProblem (
         mass, stiffness, C, 10, nfixedG, cidxs, "SSZSS", null, null, mu, probG);
      probG.createSolutionCheck ("ZZZZZ", USE_THE);
      probG.checkSolve (
         "G solve with permutation", murty, "ZZZZZ", USE_THE, 1e-10);      
      lamG = probG.getLambda();
      phiG = probG.getPhi();

      probN = createContactProblem (
         mass, stiffness, C, 10, nfixedG, null, null, cidxs, "SSZSS", mu, probN);
      probN.myBm.set (probG.myBm);
      probG.myBg.getSubVector (0, probN.myBg);
      probG.myBg.getSubVector (nfixedG, probN.myBn);
      probN.myBd.set (probG.myBd);
      probN.setFixedGT (probG);
      probN.createSolutionCheck ("ZZZZZ", USE_THE);
      probN.checkSolve (
         "N solve with permutation", murty, "ZZZZZZZZZZLLLLL", USE_THE, 1e-10);
      lamN = new VectorNd (lamG);
      lamN.setSubVector (nfixedG, probN.getTheta());
      phiN = probN.getPhi();

      checkEquals ("lam for G based contact", lamG, lamN, 1e-10);
      checkEquals ("phi for G based contact", phiG, phiN, 1e-10);

      // resolve G with state clear to get E pivots

      probG.createSolutionCheck ("ZZZZZ", USE_THE);
      probG.checkSolve (
         "G resolve with E pivots", murty, "ZZZZZ", USE_THE, 1e-10);

      // remove an active constraint

      cidxs = new int[] {3, 2, 1};   
      probG = createContactProblem (
         mass, stiffness, C, 10, nfixedG, cidxs, "SSS", null, null, mu, probG);
      probG.createSolutionCheck ("ZZZ", USE_THE);
      probG.checkSolve (
         "G solve with constraints removed", murty, "ZZZ", USE_THE, 1e-10);

      // restore the constraints - should result in an H pivot

      cidxs = new int[] {3, 2, 4, 0, 1};
      probG = createContactProblem (
         mass, stiffness, C, 10, nfixedG, cidxs, "SSZSS", null, null, mu, probG);
      probG.createSolutionCheck ("ZZZZZ", USE_THE);
      probG.checkSolve (
         "G solve with constraints restored", murty, "ZZZZZ", USE_THE, 1e-10);
      
      // do a solve/contact solve cycle with changing bilateral constraints
         
      nfixedG = 1;
      cidxs = new int[] {0, 1, 2};
      probG = createContactProblem (
         mass, stiffness, C, 5, nfixedG, cidxs, "ZZZ", null, null, mu, null);
      probG.createSolutionCheck ("ZZZZZZ", 0);
      probG.checkSolve (
         "G solve with 3 contacts", murty, "ZZZZZZ", 0, 1e-10);
      VectorNd lamSave = probG.getLambda();
      probG.checkSolve (
         "G resolve with 3 contacts", murty, "ZZZZZZ", USE_THE|REBUILD_A, 1e-10);

      // do a contact solve with a rebuild, and then solve again
      probG.createContactSolutionCheck ("");      
      probG.checkContactSolve (
         "G contact solve with rebuild and 3 contacts ",
         murty, null, REBUILD_A, 1e-10);

      probG.setLambda (lamSave);
      probG.checkSolve (
         "G resolve with 3 contacts", murty, null, USE_THE, 1e-10);

      // now remove constraints and do a contact solve

      cidxs = new int[] {1};
      probG = createContactProblem (
         mass, stiffness, C, 5, nfixedG, cidxs, "Z", null, null, mu, probG);
      probG.createContactSolutionCheck ("");
      probG.checkContactSolve (
         "G contact solve with 1 contact", murty, "", 0, 1e-10);

      probG.createSolutionCheck ("ZZ", USE_THE);
      probG.checkSolve (
         "G full solve with 1 contact", murty, "ZZ", USE_THE, 1e-10);

      }
      
   }

   String createStateFromPrevious (VectorNi state, VectorNi prevIdxs) {
      StringBuilder sb = new StringBuilder();
      // N part first
      for (int i=0; i<prevIdxs.size(); i++) {
         if (prevIdxs.get(i) == -1) {
            sb.append ('Z');
         }
         else {
            int sval = state.get(prevIdxs.get(i));
            if (sval == Z) {
               sb.append ('Z');
            }
            else {
               sb.append ('L');
            }
         }
      }
      // D part next
      int nsize = state.size()/3;
      for (int i=0; i<prevIdxs.size(); i++) {
         if (prevIdxs.get(i) == -1) {
            sb.append ('L');
            sb.append ('L');
         }
         else {
            for (int j=0; j<2; j++) {
               int sval = state.get(nsize+2*prevIdxs.get(i)+j);
               if (sval == Z) {
                  sb.append ('Z');
               }
               else if (sval == W_LO) {
                  sb.append ('L');
               }
               else {
                  sb.append ('H');
               }
            }
         }
      }
      return sb.toString();
   }

   String randomContactSpec (int numc) {
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<numc; i++) {
         double a = RandomGenerator.nextDouble (0, 1);
         if (a < 0.3) {
            sb.append (' ');
         }
         else if (a < 0.6) {
            sb.append ('S');
         }
         else {
            sb.append ('Z');
         }
      }
      return sb.toString();
   }   

   protected void testVaryingContactSpecial() {

      int maxparts = 10;
      double mass = 2.0;
      double stiffness = 0.5;
      double mu = 0.5;

      // create orientations for potential contacts and tangents
      RotationMatrix3d[] C = new RotationMatrix3d[maxparts];
      for (int bi=0; bi<maxparts; bi++) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.setRandom();
         C[bi] = R;
      }

 
      TestProblem prob, probN, probG = null;
      int nfixedG = 0;
      MurtyMechSolver murty = new MurtyMechSolver();

      // create a contact problem without friction

      int[] cidxs = new int[] {0, 1, 2};
      prob = createContactProblem (
         mass, stiffness, C, 10, "ZZZ", cidxs, -1, null);
      prob.createSolutionCheck ("");

      // do an initial check to form the contact basis:
      prob.checkSolve (
         "initialize special", murty, "ZZZ", REBUILD_A, 1e-10);

      // now remove all contacts and do a contact solve followed by a full
      // solve
      cidxs = new int[] {};
      prob = createContactProblem (
         mass, stiffness, C, 10, "", cidxs, -1, null);
      prob.createContactSolutionCheck ("");
      prob.myVersionM = 1; // prevent rebuild
      prob.checkContactSolve (
          "frictionless contact solve removing all active contacts", 
          murty, "", USE_THE, 1e-10);

      prob.createSolutionCheck ("");
      prob.checkSolve (
          "frictionless full solve removing all active contacts", 
          murty, "", USE_THE, 1e-10);
   }         

   /**
    * Test varying contact in a setting that tries to emulate a real simulation
    * loop.
    */
   protected void testVaryingContactLarge() {
      int nparts = 20;
      double mass = 2.0;
      double stiffness = 0.5;
      double mu = 0.5;

      // create orientations for potential contacts and tangents
      RotationMatrix3d[] C = new RotationMatrix3d[nparts];
      for (int bi=0; bi<nparts; bi++) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.setRandom();
         C[bi] = R;
      }

      int ntests = 1000;
      MurtyMechSolver murty = new MurtyMechSolver();
      murty.setAdaptivelyRebuildA (true);

      TestProblem prob = null;
      int[] cidxs = null;
      int nfixedG = 2;
      for (int cnt=0; cnt<ntests; cnt++) {
         // create a random contact sequence and likely solution state
         int numc = RandomGenerator.nextInt (nparts/2, 3*nparts/4);
         cidxs = RandomGenerator.randomSequence (0, nparts-1, numc);
         String cstr = randomContactSpec (numc);

         int[] cidxsG = null;
         String cstrG = null;
         int[] cidxsN = null;
         String cstrN = null;

         if (cnt < ntests/2) {
            cidxsN = cidxs;
            cstrN = cstr;
         }
         else {
            if (cnt == ntests/2) {
               prob = null;
            }
            cidxsG = cidxs;
            cstrG = cstr;
         }

         prob = createContactProblem (
            mass, stiffness, C, nparts, nfixedG,
            cidxsG, cstrG, cidxsN, cstrN, mu, prob);

         prob.createContactSolutionCheck (null);
         if ((cnt%10) == 0) {
            prob.checkContactSolve (
               "contact solve, setting A", murty, null, REBUILD_A, 1e-10);
         }
         else {
            prob.checkContactSolve ("contact solve", murty, null, 0, 1e-10);
         }
         prob.createSolutionCheck (null, USE_THE);
         if ((cnt%12) == 0) {
            prob.checkSolve (
               "full solve, setting A", murty, null, REBUILD_A|USE_THE,
               1e-10);
         }
         else {
            prob.checkSolve ("full solve", murty, null, USE_THE, 1e-10);
            murty.debug = false;
         }
      }
   }

   public void test () {
      testAssembleA();
      testBasisSolves();
      testSolves();
      setBlockPivoting (false);
      testSolves();
      setBlockPivoting (true);
      testFrictionResolve();
      testPegInHole (5, 5, 100);
      testHybridSolves();
      testVaryingContactSmall();
      testVaryingContactSpecial();
      testVaryingContactLarge();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      MurtyMechSolverTest tester = new MurtyMechSolverTest();

      boolean checkBlockPivots = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-checkBlockPivots")) {
            checkBlockPivots = true;
         }
         else {
            tester.printUsageAndExit ("[-checkBlockPivots]");
         }
      }
      if (checkBlockPivots) {
         tester.checkBlockPivots();
      }
      else {
         tester.runtest();
      }
   }

}
