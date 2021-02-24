/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;
import maspack.solvers.*;
import maspack.solvers.*;
import maspack.solvers.DantzigLCPSolver.Status;
import maspack.util.*;

import java.io.*;
import java.util.Arrays;

public class KKTSolver {

   private SparseSolverId mySolverType = SparseSolverId.Pardiso;

   public static boolean computeResidualMG = false;

   boolean myTimeSolves = false;
   boolean myMDiagonalP = false;
   int mySizeM;
   int myTypeM = Matrix.SYMMETRIC;
   Partition myPartitionM;
   int myNumG;
   int myNumN;
   int myNumD;
   SparseBlockMatrix myNT;
   SparseBlockMatrix myDT;
   SparseBlockMatrix myM;
   SparseBlockMatrix myGT;
   UmfpackSolver myUmfpack;
   PardisoSolver myPardiso;
   DirectSolver myMatrixSolver;
   boolean myIndices1Based = false;
   boolean myLastSolveWasIterative = false;

   DantzigLCPSolver myDantzig = new DantzigLCPSolver();

   int myNumVals = 0;
   int[] myColIdxs = new int[0];
   int[] myRowOffs = new int[0];

   // for umfpack
   int[] myRowIdxs = new int[0];
   int[] myColOffs = new int[0];
   double[] myUmfpackVals = new double[0];

   int[] myLocalOffs = new int[0];
   double[] myVals = new double[0];

   VectorNd myMGx = new VectorNd();
   VectorNd myMGy = new VectorNd();

   MatrixNd myLcpM = new MatrixNd();
   VectorNd myQ = new VectorNd();
   VectorNd myZ = new VectorNd();
   boolean[] myZBasic = new boolean[0];

   // if myDT != null, used zbasic, along with hi, lo, and W.
   int[] myZState = new int[0];
   VectorNd myLo = new VectorNd();
   VectorNd myHi = new VectorNd();
   VectorNd myW = new VectorNd();

   private enum State {
      NULL, ANALYZED, FACTORED
   };

   private State myState = State.NULL;

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
      NUMERIC_ERROR
   };

   public KKTSolver (SparseSolverId solverType) {
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

   public KKTSolver() {
      this (SparseSolverId.Pardiso);
   }

   /**
    * Performs symbolic analysis on the equality portion of the KKT system
    * defined by M and G. This step must be called before any calls are made to
    * factor().
    */
   public void analyzeMG (
      Object M, int sizeM, SparseBlockMatrix GT, VectorNd Rg, int typeM) {
      int numG = (GT != null ? GT.colSize() : 0);
      int numVals = 0;

      myTypeM = typeM;
      if ((typeM & Matrix.SYMMETRIC) != 0) {
         myPartitionM = Partition.UpperTriangular;
      }
      else {
         myPartitionM = Partition.Full;
      }      
      if (M instanceof SparseBlockMatrix) {
         numVals =
            ((SparseBlockMatrix)M).numNonZeroVals (myPartitionM, sizeM, sizeM);
         myM = (SparseBlockMatrix)M; // save only for possible debugging
      }
      else {
         numVals = sizeM; // M is a vector
      }
      if (GT != null) {
         int numGTnz = GT.numNonZeroVals(Partition.Full, sizeM, numG);
         if ((typeM & Matrix.SYMMETRIC) != 0) {
            numVals += numGTnz + numG;
         }
         else {
            numVals += 2*numGTnz + numG;
         }
         myGT = GT; // save only for possible debugging
      }
      // allocate buffers ...
      int sizeMG = sizeM + numG;
      if (sizeMG > myLocalOffs.length) {
         myLocalOffs = new int[sizeMG];
      }
      myMGx.setSize (sizeMG);
      myMGy.setSize (sizeMG);
      if (sizeMG + 1 > myRowOffs.length) {
         // the last entry stores the total number of non-zeros
         myRowOffs = new int[sizeMG + 1];
      }
      if (numVals > myVals.length) {
         myColIdxs = new int[numVals];
         myVals = new double[numVals];
      }
      // store for later ...
      mySizeM = sizeM;
      myNumG = numG;
      myNumVals = numVals;

      // At this point, all indices are 0-based, and so we set myIndices1Based
      // false so that getCRSIndices and getCRSValues will work properly. If we
      // are using Pardiso, then the indices will incremented later and
      // myIndices1Based will be set to true.
      myIndices1Based = false;

      getCRSRowOffsets (M, sizeM, GT);
      getCRSIndices (M, sizeM, numVals, GT);
      // get values as well, since pardiso seems to need legitimate
      // values in some cases
      getCRSValues (M, sizeM, numVals, GT, Rg);

      if (mySolverType == SparseSolverId.Umfpack) {
         setUmfpackIndices (sizeMG, numVals);
         if (myUmfpack.analyze (
            myColOffs, myRowIdxs, sizeMG, myUmfpackVals.length) !=
             UmfpackSolver.UMFPACK_OK) {
            throw new NumericalException ("Unable to analyze matrix");
         }
      }
      else { // add 1 to indices, since Pardiso indices are 1-based
         // XXX
         for (int i = 0; i < numVals; i++) {
            myColIdxs[i]++;
         }
         for (int i = 0; i < sizeMG+1; i++) {
            myRowOffs[i]++;
         }
         myIndices1Based = true;
         if ((myTypeM & Matrix.SYMMETRIC) != 0) {
            // even if myTypeM is SPD, the KKT system won't be, so
            // we need a symmetric solve regardless
            myPardiso.analyze (
               myVals, myColIdxs, myRowOffs, sizeMG, Matrix.SYMMETRIC);
         }
         else {
            myPardiso.analyze (
               myVals, myColIdxs, myRowOffs, sizeMG, Matrix.INDEFINITE);
         }
         if (myPardiso.getState() == PardisoSolver.UNSET) {
            throw new NumericalException (
               "Pardiso: unable to analyze matrix: "+myPardiso.getErrorMessage());
         }
      }
      myMDiagonalP = (M instanceof VectorNd);
      myDirectCnt = 0;
      myDirectTimeMsec = 0;
      myIterativeCnt = 0;
      myIterativeTimeMsec = 0;
      myState = State.ANALYZED;
   }

   private void getCRSRowOffsets (Object M, int sizeM, SparseBlockMatrix GT) {
      // start by finding the number of non-zeros in each row, and
      // accumulate this into myLocalOffs
      if (M instanceof SparseBlockMatrix) {
         for (int i = 0; i < sizeM; i++) {
            myLocalOffs[i] = 0;
         }
         ((SparseBlockMatrix)M).addNumNonZerosByRow (
            myLocalOffs, 0, myPartitionM, sizeM, sizeM);
      }
      else {
         for (int i = 0; i < sizeM; i++) {
            myLocalOffs[i] = 1;
         }
      }
      int numG = (GT != null ? GT.colSize() : 0);
      int sizeMG = sizeM + numG;
      if (numG != 0) {
         GT.addNumNonZerosByRow (myLocalOffs, 0, Partition.Full, sizeM, numG);
         // if (GT.colSize() == 5) {
         //    System.out.println ("rowOffs:");
         //    for (int i=0; i<sizeM; i++) {
         //       System.out.print (myLocalOffs[i]+" "); 
         //    }
         //    System.out.println ("");
         // }
         for (int i=sizeM; i<sizeMG; i++) {
            myLocalOffs[i] = 0;
         }
         if (myPartitionM == Partition.Full) {
            GT.addNumNonZerosByCol (
               myLocalOffs, sizeM, Partition.Full, sizeM, numG);
         }
         for (int i=sizeM; i<sizeMG; i++) {
            myLocalOffs[i]++;
         }        
      }
      int off = 0;
      for (int i=0; i<sizeMG; i++) {
         myRowOffs[i] = off; // XXX
         off += myLocalOffs[i];;
      }
      myRowOffs[sizeMG] = off; // XXX
   }

   private void getCRSIndices (
      Object M, int sizeM, int numVals, SparseBlockMatrix GT) {
      for (int i = 0; i < sizeM; i++) {
         myLocalOffs[i] = myRowOffs[i];
         if (myIndices1Based) {
            myLocalOffs[i]--;
         }
      }
      if (M instanceof SparseBlockMatrix) {
         ((SparseBlockMatrix)M).getBlockCRSIndices (
            myColIdxs, 0, myLocalOffs, myPartitionM, sizeM, sizeM);
      }
      else {
         for (int i = 0; i < sizeM; i++) {
            myColIdxs[myLocalOffs[i]++] = i;
         }
      }
      if (GT != null) {
         int numG = GT.colSize();
         GT.getBlockCRSIndices (
            myColIdxs, sizeM, myLocalOffs, Partition.Full, sizeM, numG);
         // now do the lower block(s). Reset localOffs to the cober rows sizeM
         // to sizeM+numG
         for (int i=0; i<numG; i++) {
            myLocalOffs[i] = myRowOffs[sizeM+i];
            // XXX
            if (myIndices1Based) {
               myLocalOffs[i]--;
            }
         } 
         if (myPartitionM == Partition.Full) {
            GT.getBlockCCSIndices (
               myColIdxs, 0, myLocalOffs, Partition.Full, sizeM, numG);
         }
         for (int i=0; i<numG; i++) {
            myColIdxs[myLocalOffs[i]++] = i + sizeM;
         }
      }
   }

   private void getCRSValues (
      Object M, int sizeM, int numVals, SparseBlockMatrix GT, VectorNd Rg) {
      for (int i = 0; i < sizeM; i++) {
         myLocalOffs[i] = myRowOffs[i];
         // XXX
         if (myIndices1Based) {
            myLocalOffs[i]--;
         }
      }
      if (M instanceof SparseBlockMatrix) {
         ((SparseBlockMatrix)M).getBlockCRSValues (
            myVals, myLocalOffs, myPartitionM, sizeM, sizeM);
      }
      else {
         double[] diag = ((VectorNd)M).getBuffer();
         for (int i = 0; i < sizeM; i++) {
            myVals[myLocalOffs[i]++] = diag[i];
         }
      }
      // if (GT != null) {
      //    int numG = GT.colSize();
      //    GT.getBlockCRSValues (myVals, myLocalOffs, Partition.Full, sizeM, numG);
      //    if (Rg != null) {
      //       double[] Rgbuf = Rg.getBuffer();
      //       for (int i = 0; i < numG; i++) {
      //          myVals[numVals - numG + i] = -Rgbuf[i];
      //       }
      //    }
      //    else {
      //       for (int i = 0; i < numG; i++) {
      //          myVals[numVals - numG + i] = 0;
      //       }
      //    }
      // }
      if (GT != null) {
         int numG = GT.colSize();
         GT.getBlockCRSValues (myVals, myLocalOffs, Partition.Full, sizeM, numG);
         // now do the lower block(s). Reset localOffs to the cober rows sizeM
         // to sizeM+numG
         for (int i=0; i<numG; i++) {
            myLocalOffs[i] = myRowOffs[sizeM+i];
            // XXX
            if (myIndices1Based) {
               myLocalOffs[i]--;
            }
         }    
         if (myPartitionM == Partition.Full) {
            GT.getBlockCCSValues (
               myVals, myLocalOffs, Partition.Full, sizeM, numG);
         }        
         if (Rg != null) {
            double[] Rgbuf = Rg.getBuffer();
            for (int i=0; i<numG; i++) {
               myVals[myLocalOffs[i]++] = -Rgbuf[i];
            }
         }
         else {
            for (int i=0; i<numG; i++) {
               myVals[myLocalOffs[i]++] = 0;
            }
         }
      }      
   }

   /**
    * Performs symbolic analysis on the equality portion of the KKT system
    * defined by a diagonal M matrix and G. This step must be called before any
    * calls are made to factor().
    * 
    * @param Mdiag vector defining the diagonal entries of M
    * @param sizeM size of M 
    * @param GT Sparse matrix defining the transpose of G
    * @param Rg if non-null, supplies the diagonal regularization matrix R
    */
   public void analyze (
      VectorNd Mdiag, int sizeM, SparseBlockMatrix GT, VectorNd Rg) {
      analyzeMG (Mdiag, sizeM, GT, Rg, Matrix.SYMMETRIC);
   }

   /**
    * Performs symbolic analysis on the equality portion of the KKT system
    * defined by matrices M and GT. This step must be called before any calls
    * are made to factor().
    * 
    * @param M Sparse matrix defining M
    * @param sizeM size of M 
    * @param GT Sparse matrix defining the transpose of G
    * @param Rg if non-null, supplies the diagonal regularization matrix R
    * @param typeM describes the type of M, to be used in determining 
    * how the resulting KKT system should be factored. Should be
    * either {@link Matrix#INDEFINITE}, {@link Matrix#SYMMETRIC}, or
    * {@link Matrix#SPD}. 
    */
   public void analyze (
      SparseBlockMatrix M, int sizeM,
      SparseBlockMatrix GT, VectorNd Rg, int typeM) {
      analyzeMG (M, sizeM, GT, Rg, typeM);
   }

   private void checkMGStructure (Object M, int sizeM, SparseBlockMatrix GT) {
      if (myState == State.NULL) {
         throw new ImproperStateException ("Analyze has not been called");
      }
      if (sizeM != mySizeM) {
         throw new ImproperStateException ("" + sizeM + "x" + sizeM
         + " M matrix different from " + mySizeM + "x" + mySizeM
         + " matrix supplied to analyze()");
      }
      int GTcolSize = (GT != null ? GT.colSize() : 0);
      if (GTcolSize != myNumG) {
         throw new ImproperStateException ("" + sizeM + "x" + GTcolSize
         + " GT matrix different from " + mySizeM + "x" + myNumG
         + " matrix supplied to analyze()");
      }
      if (myMDiagonalP && M instanceof SparseBlockMatrix) {
         throw new ImproperStateException (
            "M is a SparseBlockMatrix but was analyzed as a diagonal vector");
      }
      else if (!myMDiagonalP && M instanceof VectorNd) {
         throw new ImproperStateException (
            "M is a diagonal vector but was analyzed as a SparseBlockMatrix");
      }
   }

   /**
    * Does a numeric factorization of a KKT system containg only equality
    * constraints. analyze() must have been previously called with M and G
    * matrices having the same symbolic structure as the ones supplied to this
    * method.
    */
   public void factor (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg) {
      checkMGStructure (M, sizeM, GT);
      factorMG (M, sizeM, GT, Rg);
      myState = State.FACTORED;
   }

   private boolean warningGiven = false;

   private void warnAboutUnsymmetricUnilateralSolves () {
      if (!warningGiven) {
         System.out.println (
"WARNING: KKTSolver: unilateral constraints have been specified with a\n" +
"non-symmetric system matrix; solution may fail");
         warningGiven = true;
      }
   }

   /**
    * Does a numeric factorization of a KKT system containing equality and
    * inequality constraints. analyze() must have been previously called with M
    * and G matrices having the same symbolic structure as the ones supplied to
    * this method.
    */
   public void factor (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg,
      SparseBlockMatrix NT, VectorNd Rn) {
      long t0 = System.nanoTime();
      checkMGStructure (M, sizeM, GT);
      if (myTimeSolves) timerStart();
      factorMG (M, sizeM, GT, Rg);

      if (myTimeSolves) timerStop ("factorMG:");
      if (NT != null && NT.colSize() != 0) {
         if ((myTypeM & Matrix.SYMMETRIC) == 0) {
            warnAboutUnsymmetricUnilateralSolves();
         }
         if (myTimeSolves) timerStart();
         buildLCP (NT, Rn);
         if (myTimeSolves) {
            timerStop ("buildLCP m=" + NT.colSize() + ":");
         }
      }
      // System.out.println ("M=\n" + M.toString ("%10.6f"));
      // System.out.println ("GT=\n" + GT.toString ("%10.6f"));
      // System.out.println ("NT=\n" + NT.toString ("%10.6f"));
      myState = State.FACTORED;
      long t1 = System.nanoTime();
      //System.out.println ("factor " + (t1-t0)*1e-6);
   }

   /**
    * Does a numeric factorization of a KKT system containg equality,
    * inequality, and friction constraints. analyze() must have been previously
    * called with M and G matrices having the same symbolic structure as the
    * ones supplied to this method.
    */
   public void factor (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg,
      SparseBlockMatrix NT, VectorNd Rn, SparseBlockMatrix DT) {
      long t0 = System.nanoTime();
      checkMGStructure (M, sizeM, GT);
      factorMG (M, sizeM, GT, Rg);
      if (NT != null && NT.colSize() != 0) {
         if ((myTypeM & Matrix.SYMMETRIC) == 0) {
            warnAboutUnsymmetricUnilateralSolves();
         }
         buildLCP (NT, Rn, DT);
      }
      myState = State.FACTORED;
      long t1 = System.nanoTime();
      // System.out.println ("factor " + (t1-t0)*1e-6);
   }

   /**
    * Does a numeric factorization of a KKT system containg only equality
    * constraints. analyze() must have been previously called with M and GT
    * matrices having the same symbolic structure as the ones supplied to this
    * method.
    */
   public void factor (
      VectorNd Mdiag, int sizeM, SparseBlockMatrix GT, VectorNd Rg) {
      checkMGStructure (Mdiag, sizeM, GT);
      factorMG (Mdiag, sizeM, GT, Rg);
      myState = State.FACTORED;
   }

   /**
    * Does a numeric factorization of a KKT system containg equality and
    * inequality constraints. analyze() must have been previously called with M
    * and G matrices having the same symbolic structure as the ones supplied to
    * this method.
    */
   public void factor (
      VectorNd Mdiag, int sizeM, SparseBlockMatrix GT, VectorNd Rg,
      SparseBlockMatrix NT, VectorNd Rn) {
      checkMGStructure (Mdiag, sizeM, GT);
      factorMG (Mdiag, sizeM, GT, Rg);
      if (NT != null && NT.colSize() != 0) {
         if ((myTypeM & Matrix.SYMMETRIC) == 0) {
            warnAboutUnsymmetricUnilateralSolves();
         }
         buildLCP (NT, Rn);
      }
      myState = State.FACTORED;
   }

   public boolean isFactored() {
      return myState == State.FACTORED;
   }

   /**
    * Solves the equality, inequality, and frictional parts of a factored
    * system.
    */
   public Status solve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi,
      VectorNd bm, VectorNd bg, VectorNd bn, VectorNd bd, VectorNd flim) {
      myLastSolveWasIterative = false;
      return dosolve (vel, lam, the, phi, bm, bg, bn, bd, flim);
   }

   /**
    * Solves the equality and inequality parts of a factored system.
    */
   public Status solve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd bm, VectorNd bg,
      VectorNd bn) {
      myLastSolveWasIterative = false;
      return dosolve (vel, lam, the, null, bm, bg, bn, null, null);
   }

   public double residual (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg,
      SparseBlockMatrix NT, VectorNd Rn,
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd bm, VectorNd bg,
      VectorNd bn) {

      VectorNd resm = new VectorNd (sizeM);
      VectorNd tmpm = new VectorNd (sizeM);
      VectorNd resg = new VectorNd (GT.colSize());
      VectorNd resn = new VectorNd (0);
      M.mul (resm, vel, sizeM, sizeM);
      GT.mul (tmpm, lam, sizeM, GT.colSize());
      resm.sub (tmpm);
      if (NT != null) {
         NT.mul (tmpm, the, sizeM, NT.colSize());
         resm.sub (tmpm);
      }
      resm.sub (bm);
      GT.mulTranspose (resg, vel, GT.colSize(), sizeM);
      resg.sub (bg);
      if (Rg != null) {
         for (int i=0; i<GT.colSize(); i++) {
            resg.add (i, Rg.get(i)*lam.get(i));
         }
      }
      if (NT != null) {
         NT.mulTranspose (resn, vel, NT.colSize(), sizeM);
         resn.sub (bn);
         for (int i=0; i<NT.colSize(); i++) {
            if (Rn != null) {
               resn.add (i, Rn.get(i)*the.get(i));
            }
            // residual for an LCP is different - base it on
            // the complemenatarity conditions
            double w = resn.get(i);
            double t = the.get(i);
            double r = w < 0 ? w : 0;
            if (t < 0 && t < r) {
               r = t;
            }
            if (Math.abs(w*t) > Math.abs(r)) {
               r = w*t;
            }
            resn.set (i, r);               
         }
      }
      return Math.sqrt (resm.dot(resm) + resg.dot(resg) + resn.dot(resn));
   }
   
   /**
    * Solves the equality parts of a factored system.
    */
   public Status solve (VectorNd vel, VectorNd lam, VectorNd bm, VectorNd bg) {
      myLastSolveWasIterative = false;
      return dosolve (vel, lam, null, null, bm, bg, null, null, null);
   }

   double myDirectTimeMsec = 0;
   int myDirectCnt = 0;
   double myIterativeTimeMsec = 0;
   double myFirstIterativeTimeMsec = 0;
   int myIterativeCnt = 0;

   private int estimateOptimalCount () {
      // Estimates the optimal count (myIterativeCnt+1), after which we
      // should do a refactor. Should only be called if myIterativeCnt > 0.
      // 
      // We assume that after an initial factor, the iterative time starts out
      // small and increases linearly with each step.  Let ft be the factor
      // time, it0 the initial iterative time, and dt the increase in the
      // factor time per step. Let n be the total number of steps since (and
      // including) the last factor step. Total average compute time at step n
      // is then
      //
      // (ft + it0 (n-1) + 1/2 dt (n^2 - 3n + 2)) / n
      //
      // Pretending n is continuous, we obtain a minimum at
      //
      // n = sqrt (2(ft-it0+dt)/dt)
      //
      double ft = myDirectTimeMsec/myDirectCnt;
      double it0 = myFirstIterativeTimeMsec;
      double itAvg = myIterativeTimeMsec/myIterativeCnt;

      if (itAvg >= ft) {
         return myIterativeCnt+1; // refactor right away
      }
      double dt = 2*(itAvg-it0)/myIterativeCnt;
      if (dt <= 0) {
         // will happen if iterativeCnt == 0, or if solution has gone static
         // set n to an arbitrary value of 20
         return myIterativeCnt+19;
      }
      else {
         return (int)Math.ceil (Math.sqrt (2*(ft-it0+dt)/dt));
      }
   }

   /**
    * Does a numeric factorization of a KKT system containg only equality
    * constraints. analyze() must have been previously called with M and G
    * matrices having the same symbolic structure as the ones supplied to this
    * method.
    */
   public Status factorAndSolve (
      SparseBlockMatrix M, int sizeM, SparseBlockMatrix GT, VectorNd Rg,
      VectorNd vel, VectorNd lam, VectorNd bm, VectorNd bg, int tolExp) {

      myLastSolveWasIterative = false;
      checkMGStructure (M, sizeM, GT);
      if (myState != State.FACTORED) {
         throw new ImproperStateException ("Factor has not been called");
      }
      if (vel.size() != mySizeM || bm.size() != mySizeM) {
         throw new IllegalArgumentException (
            "size of vel and/or bm incompatible with factored M size of "
            + mySizeM);
      }
      if (lam.size() != myNumG || bg.size() != myNumG) {
         throw new IllegalArgumentException (
            "size of lam and/or bg incompatible with factored GT size of "
            + myNumG);
      }
      int iterStatus = 0;
      myNumN = 0;
      myNT = null;
      myNumD = 0;
      myDT = null;
      
      if (myPardiso != null && myDirectCnt > 0 &&
          (myIterativeCnt == 0 || myIterativeCnt+1 < estimateOptimalCount())) {
         long t0 = System.nanoTime();
         getCRSValues (M, sizeM, myNumVals, GT, Rg);

         double[] bbuf;
         double[] xbuf = myMGx.getBuffer();
         double[] ybuf = myMGy.getBuffer();

         bbuf = bm.getBuffer();
         for (int i = 0; i < mySizeM; i++) {
            xbuf[i] = bbuf[i];
         }
         bbuf = bg.getBuffer();
         for (int i = 0; i < myNumG; i++) {
            xbuf[i + mySizeM] = bbuf[i];
         }

         iterStatus = myPardiso.iterativeSolve (myVals, ybuf, xbuf, tolExp);

         if (iterStatus > 0) {
            bbuf = vel.getBuffer();
            for (int i = 0; i < mySizeM; i++) {
               bbuf[i] = ybuf[i];
            }
            bbuf = lam.getBuffer();
            for (int i = 0; i < myNumG; i++) {
               bbuf[i] = -ybuf[i + mySizeM];
            }
            long t1 = System.nanoTime();
            myIterativeTimeMsec += (t1 - t0) * 1e-6;
            if (myIterativeCnt++ == 0) {
               myFirstIterativeTimeMsec = myIterativeTimeMsec;
            }
            myLastSolveWasIterative = true;
            return Status.SOLVED;
         }
      }
      long t0 = System.nanoTime();
      factorMG (M, sizeM, GT, Rg);
      solveMG (vel, lam, bm, bg);

      long t1 = System.nanoTime();

      myDirectTimeMsec += (t1 - t0) * 1e-6;
      myDirectCnt++;
      myIterativeCnt = 0;
      myIterativeTimeMsec = 0;
      return Status.SOLVED;
   }

   private Status dosolve (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi,
      VectorNd bm, VectorNd bg, VectorNd bn, VectorNd bd, VectorNd flim) {
      long t0 = System.nanoTime();
      if (myState != State.FACTORED) {
         throw new ImproperStateException ("Factor has not been called");
      }
      if (vel.size() != mySizeM || bm.size() != mySizeM) {
         throw new IllegalArgumentException (
            "size of vel and/or bm incompatible with factored M size of "
            + mySizeM);
      }
      if (lam.size() != myNumG || bg.size() != myNumG) {
         throw new IllegalArgumentException ("Bad dimensions: lam size="
         + lam.size() + ", bg size=" + bg.size() + ", factored GT size="
         + myNumG);
      }
      if ((the == null) != (bn == null)) {
         throw new IllegalArgumentException (
            "'the' and 'bn' must both be null or non-null");
      }
      if ((phi == null) != (flim == null)) {
         throw new IllegalArgumentException (
            "'phi' and 'flim' must both be null or non-null");
      }
      if (the != null) {
         if (the.size() != myNumN) {
            throw new IllegalArgumentException (
               "'the' size "+the.size()+" incompatible with NT size " + myNumN);
         }
         else if (bn.size() != myNumN) {
            throw new IllegalArgumentException (
               "'bn' size "+bn.size()+" incompatible with NT size " + myNumN);
         }
      }
      if (phi != null) {
         if (the == null) {
            throw new IllegalArgumentException (
               "'the' must be specified if 'phi' is");
         }
         if (phi.size() != myNumD) {
            throw new IllegalArgumentException (
               "'phi' size "+phi.size()+" incompatible with DT size " + myNumD);
         }
         else if (flim.size() != myNumD) {
            throw new IllegalArgumentException (
               "'flim' size "+flim.size()+" incompatible with DT size " + myNumD);
         }
      }
      Status status;
      if (myNumN == 0 || the == null) {
         if (myTimeSolves) timerStart();
         solveMG (vel, lam, bm, bg);
         if (myTimeSolves) timerStop ("solveMG:");
         status = Status.SOLVED;
      }
      else if (the != null && phi == null) {
         status = solveLCP (vel, lam, the, bm, bg, bn);
      }
      else {
         status = solveLCP (vel, lam, the, phi, bm, bg, bn, bd, flim);
      }
      long t1 = System.nanoTime();
      return status;
   }

   public boolean[] getZBasic() {
      return Arrays.copyOf (myZBasic, myNumN);
   }

   public int[] getZState() {
      return Arrays.copyOf (myZState, myNumN+myNumD);
   }

   private Status solveLCP (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd bm, VectorNd bg,
      VectorNd bn) {
      double[] xbuf = myMGx.getBuffer();
      double[] ybuf = myMGy.getBuffer();
      double[] bbuf;

      for (int i = 0; i < mySizeM; i++) {
         xbuf[i] = bm.get(i);
      }
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         xbuf[i] = bg.get (i - mySizeM);
      }
      if (myTimeSolves) timerStart();
      solveMG (myMGy, myMGx);
      if (myTimeSolves) timerStop ("solveMG:");
      myNT.mulTranspose (myQ, myMGy, myNT.colSize(), mySizeM);
      double[] qbuf = myQ.getBuffer();
      for (int i = 0; i < myNumN; i++) {
         qbuf[i] -= bn.get(i);
      }
      // don't currently need this:
      for (int i = 0; i < myQ.size(); i++) {
         myZBasic[i] = false;
      }
      //System.out.println ("LCP M=[\n" + myLcpM + "]");
      //System.out.println ("Q=" + myQ);

      myDantzig.setComputeResidual (true);
      if (myTimeSolves) timerStart();
      DantzigLCPSolver.Status status =
         myDantzig.solve (myZ, myLcpM, myQ, myZBasic);
      if (myTimeSolves) timerStop("solveLCP:");
      myDantzig.setComputeResidual (false);
      //System.out.println ("status=" + status + " res=" + myDantzig.getResidual());
      // System.out.println ("M=\n" + myLcpM);
      // System.out.println ("q=\n" + myQ);
      // System.out.println ("z=\n" + myZ);
      if (status != DantzigLCPSolver.Status.SOLVED) {
         switch (status) {
            case NO_SOLUTION: {
               return Status.NO_SOLUTION;
            }
            case ITERATION_LIMIT_EXCEEDED: {
               return Status.ITERATION_LIMIT_EXCEEDED;
            }
            case NUMERIC_ERROR: {
               return Status.NUMERIC_ERROR;
            }
            default: {
               throw new InternalErrorException ("Unknown LCP solver status: "
               + status);
            }
         }
      }
      //System.out.println ("kktN= " + myQ.toString("%12.8f"));
      // System.out.println ("myM11=\n" + myLcpM);
      // System.out.println ("myQ=" + myQ);
      // System.out.println ("myZ=" + myZ);
      the.set (myZ);
      myNT.mul (myMGx, myZ, mySizeM, myNumN);

      // reset size of MGx because myNT.mul & myDT.mul will have set it to mySizeM
      myMGx.setSize (mySizeM+myNumG); 
      for (int i = 0; i < mySizeM; i++) {
         xbuf[i] += bm.get(i);
      }
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         // use '=' instead of '+=' because this range of x is unset
         xbuf[i] = bg.get (i - mySizeM);
      }
      solveMG (myMGy, myMGx);
      for (int i = 0; i < mySizeM; i++) {
         vel.set (i, ybuf[i]);
      }
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         lam.set (i - mySizeM, ybuf[i]);
      }
      return Status.SOLVED;
   }

   private Status solveLCP (
      VectorNd vel, VectorNd lam, VectorNd the, VectorNd phi,
      VectorNd bm, VectorNd bg, VectorNd bn, VectorNd bd, VectorNd flim) {

      double[] xbuf = myMGx.getBuffer();
      double[] ybuf = myMGy.getBuffer();
      double[] bbuf;

      VectorNd nmul = new VectorNd (myNumN);
      VectorNd dmul = new VectorNd (myNumD);
      for (int i = 0; i < mySizeM; i++) {
         xbuf[i] = bm.get(i);
      }
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         xbuf[i] = bg.get (i - mySizeM);
      }
      solveMG (myMGy, myMGx);
      myNT.mulTranspose (nmul, myMGy, myNumN, mySizeM);
      myDT.mulTranspose (dmul, myMGy, myNumD, mySizeM);

      myQ.setSubVector (0, nmul);      
      myQ.setSubVector (myNumN, dmul);
      double[] qbuf = myQ.getBuffer();
      for (int i = 0; i < myNumN; i++) {
         //         System.out.println (""+i+" "+qbuf[i]+" "+bn.get(i));
         qbuf[i] -= bn.get(i);
         myLo.set (i, 0);
         myHi.set (i, Double.POSITIVE_INFINITY);
      }
      for (int i = 0; i < myNumD; i++) {
         // System.out.println (""+(myNumN+i)+" "+qbuf[myNumN+i]+" "+bd.get(i));
         qbuf[myNumN+i] -= bd.get(i);
         double fmax = flim.get(i);
         myLo.set (myNumN+i, -fmax);
         myHi.set (myNumN+i, +fmax);
      }
      DantzigLCPSolver.Status status =
         myDantzig.solve (myZ, myW, myLcpM, myQ, myLo, myHi, 0, myZState);
      if (status != DantzigLCPSolver.Status.SOLVED) {
         switch (status) {
            case NO_SOLUTION: {
               return Status.NO_SOLUTION;
            }
            case ITERATION_LIMIT_EXCEEDED: {
               return Status.ITERATION_LIMIT_EXCEEDED;
            }
            case NUMERIC_ERROR: {
               return Status.NUMERIC_ERROR;
            }
            default: {
               throw new InternalErrorException ("Unknown LCP solver status: "
               + status);
            }
         }
      }
      //System.out.println ("kktF= " + myQ.toString("%12.8f"));
      //System.out.println ("kktQ= " + myQ);
      // System.out.println ("myM11=\n" + myLcpM);
      // System.out.println ("myQ=" + myQ);
      // System.out.println ("myLo=" + myLo);
      // System.out.println ("myHi=" + myHi);
      // System.out.println ("myZ=" + myZ);
      // System.out.println ("myW=" + myW);
      for (int i=0; i<myNumN; i++) {
         the.set (i, myZ.get(i));
      }
      for (int i=0; i<myNumD; i++) {
         phi.set (i, myZ.get(myNumN+i));
      }
      myNT.mul (myMGx, the, mySizeM, myNumN);
      myDT.mulAdd (myMGx, phi, mySizeM, myNumD);

      // reset size of MGx because myNT.mul will have set it to mySizeM
      myMGx.setSize (mySizeM+myNumG); 
      for (int i = 0; i < mySizeM; i++) {
         xbuf[i] += bm.get(i);
      }
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         // use '=' instead of '+=' because this range of x is unset
         xbuf[i] = bg.get (i - mySizeM);
      }
      solveMG (myMGy, myMGx);
      for (int i = 0; i < mySizeM; i++) {
         vel.set (i, ybuf[i]);
      }
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         lam.set (i - mySizeM, ybuf[i]);
      }
      return Status.SOLVED;
   }

   private void buildLCP (SparseBlockMatrix NT, VectorNd Rn) {
      int n = NT.colSize();
      myLcpM.setSize (n, n);
      if (myZBasic.length < n) {
         myZBasic = new boolean[n];
      }
      myQ.setSize (n);
      myZ.setSize (n);
      double[] xbuf = myMGx.getBuffer();
      double[] ybuf = myMGy.getBuffer();
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         xbuf[i] = 0;
      }
      VectorNd mcol = new VectorNd (n);
      for (int j = 0; j < n; j++) {
         NT.getColumn (j, xbuf, 0, mySizeM);
         solveMG (myMGy, myMGx);
         NT.mulTranspose (mcol, myMGy, NT.colSize(), mySizeM);
         myLcpM.setColumn (j, mcol);
      }
      if (Rn != null) {
         for (int i = 0; i < n; i++) {
            myLcpM.set (i, i, myLcpM.get (i, i) + Rn.get(i));
         }
      }
      myNumN = n;
      myNT = NT;
      myNumD = 0;
      myDT = null;
   }

   private void buildLCP (
      SparseBlockMatrix NT, VectorNd Rn, SparseBlockMatrix DT) {
      
      int Nsize = NT.colSize();
      int Dsize = DT.colSize();
      int n = Nsize + Dsize;
      myLcpM.setSize (n, n);
      if (myZState.length < n) {
         myZState = new int[n];
      }
      myQ.setSize (n);
      myZ.setSize (n);
      myW.setSize (n);
      myHi.setSize (n);
      myLo.setSize (n);
      double[] xbuf = myMGx.getBuffer();
      double[] ybuf = myMGy.getBuffer();
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         xbuf[i] = 0;
      }
      VectorNd nmul = new VectorNd (Nsize);
      VectorNd dmul = new VectorNd (Dsize);
      VectorNd mcol = new VectorNd (n);
      for (int j = 0; j < Nsize; j++) {
         NT.getColumn (j, xbuf, 0, mySizeM);
         solveMG (myMGy, myMGx);
         NT.mulTranspose (nmul, myMGy, Nsize, mySizeM);
         mcol.setSubVector (0, nmul);
         DT.mulTranspose (dmul, myMGy, Dsize, mySizeM);
         mcol.setSubVector (Nsize, dmul);
         myLcpM.setColumn (j, mcol);
      }
      for (int j = 0; j < Dsize; j++) {
         DT.getColumn (j, xbuf, 0, mySizeM);
         solveMG (myMGy, myMGx);
         NT.mulTranspose (nmul, myMGy, Nsize, mySizeM);
         mcol.setSubVector (0, nmul);
         DT.mulTranspose (dmul, myMGy, Dsize, mySizeM);
         mcol.setSubVector (Nsize, dmul);
         myLcpM.setColumn (Nsize+j, mcol);
      }
      if (Rn != null) {
         for (int i = 0; i < Nsize; i++) {
            myLcpM.set (i, i, myLcpM.get (i, i) + Rn.get(i));
         }
      }
      myNumN = Nsize;
      myNT = NT;
      myNumD = Dsize;
      myDT = DT;
   }

   /**
    * Umfpack is an unsymmetric solver, so we need to map the symmetric CRS
    * indices onto a full set of CCS indices.
    */
   private void setUmfpackIndices (int size, int numVals) {
      // first, compute number of entries in each column
      int[] numColVals = new int[size];
      for (int i = 0; i < size; i++) {
         numColVals[i] = myRowOffs[i+1] - myRowOffs[i];
      }
      int i = -1;
      for (int k = 0; k < numVals; k++) {
         if (k == myRowOffs[i+1]) {
            i++;
         }
         int j = myColIdxs[k];
         if (j > i) {
            numColVals[j]++;
         }
      }
      myColOffs = new int[size + 1];
      int accum = 0;
      for (i = 0; i < size; i++) {
         myColOffs[i] = accum;
         myLocalOffs[i] = accum;
         accum += numColVals[i];
      }
      myColOffs[size] = accum;
      int fullNumVals = accum;
      myRowIdxs = new int[fullNumVals];
      i = -1;
      for (int k = 0; k < numVals; k++) {
         if (k == myRowOffs[i+1]) {
            i++;
         }
         int j = myColIdxs[k];
         if (j > i) {
            myRowIdxs[myLocalOffs[j]++] = i;
         }
      }
      i = -1;
      for (int k = 0; k < numVals; k++) {
         if (k == myRowOffs[i+1]) {
            i++;
         }
         myRowIdxs[myLocalOffs[i]++] = myColIdxs[k];
      }
      myUmfpackVals = new double[fullNumVals];
   }

   private void loadUmfpackValues (int size, int numVals) {
      for (int i = 0; i < size; i++) {
         myLocalOffs[i] = myColOffs[i];
      }
      int i = -1;
      for (int k = 0; k < numVals; k++) {
         if (k == myRowOffs[i+1]) {
            i++;
         }
         int j = myColIdxs[k];
         if (j > i) {
            myUmfpackVals[myLocalOffs[j]++] = myVals[k];
         }
      }
      i = -1;
      for (int k = 0; k < numVals; k++) {
         if (k == myRowOffs[i+1]) {
            i++;
         }
         myUmfpackVals[myLocalOffs[i]++] = myVals[k];
      }
   }

   public void printStructure (PrintStream ps) {
      printStructure (ps, myRowOffs, myColIdxs, myNumVals);
   }

   private void printStructure (
      PrintStream ps, int[] rowOffs, int[] colIdxs, int numVals) {
      NumberFormat fmt = new NumberFormat ("%2d");
      int i = 0;
      int curCol = 0;
      for (int k = 0; k < numVals; k++) {
         int rowOff = rowOffs[i];
         if (myIndices1Based) {
            rowOff--;
         }
         if (k == rowOff) {
            curCol = 0;
            if (i > 0) {
               System.out.println ("");
            }
            i++;
         }
         int col = colIdxs[k];
         if (myIndices1Based) {
            col--;
         }
         while (curCol < col) {
            System.out.print ("   ");
            curCol++;
         }
         System.out.print (" " + fmt.format (col));
         curCol++;
      }
      System.out.println ("");
   }

   public void printValues (PrintStream ps, String fmtStr) {
      printValues (
         ps, fmtStr, myVals, myRowOffs, myColIdxs, mySizeM + myNumG, myNumVals);
   }


   // for debugging
   public MatrixNd getLinearMatrix() {
      int size = mySizeM+myNumG;
      MatrixNd KKT = new MatrixNd(size,size);
      KKT.setCRSValues (
         myVals, myColIdxs, myRowOffs, myNumVals, size,
         Matrix.Partition.UpperTriangular);
      return KKT;
   }

   public void printLinearProblem (
      PrintWriter pw, VectorNd bf, VectorNd bg,
      String fmtStr, boolean omitLowerRightDiagonal) throws IOException {
      
      NumberFormat fmt = new NumberFormat (fmtStr);      
      int size = mySizeM + myNumG;
      int nnz = myNumVals;
      if (omitLowerRightDiagonal) {
         nnz -= myNumG;
      }
      if (myPartitionM == Matrix.Partition.UpperTriangular) {
         pw.print ("SYMMETRIC ");
      }
      pw.println (size);
      for (int i=0; i<size+1; i++) {
         // if we are omitting the lower right diagonal, then the row offsets
         // should max out at nnz+1.
         pw.print (Math.min(nnz+1,myRowOffs[i]));
         if (i<size) {
            pw.print (" ");
         }
      }
      pw.println ("");
      for (int i=0; i<nnz; i++) {
         pw.print (myColIdxs[i]);
         if (i<nnz-1) {
            pw.print (" ");
         }
      }
      pw.println ("");
      for (int i=0; i<nnz; i++) {
         pw.print (fmt.format(myVals[i]));
         if (i<nnz-1) {
            pw.print (" ");
         }
      }
      pw.println ("");
      bf.write (pw, fmt);
      pw.print (" ");
      bg.write (pw, fmt);
      pw.println ("");
      pw.flush();
   }

   public void printValues (
      PrintStream ps, String fmtStr, double[] vals, int[] rowOffs,
      int[] colIdxs, int size, int numVals) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      String blank = "";
      for (int i = 0; i < fmt.getFieldWidth(); i++) {
         blank += " ";
      }
      blank += "0";
      int i = 0;
      int curCol = 0;
      for (int k = 0; k < numVals; k++) {
         int rowOff = rowOffs[i];
         if (myIndices1Based) {
            rowOff--;
         }
         if (k == rowOff) {
            if (i > 0) {
               while (curCol < size) {
                  System.out.print (blank);
                  curCol++;
               }
               System.out.println ("");
            }
            curCol = 0;
            i++;
         }
         int col = colIdxs[k];
         if (myIndices1Based) {
            col--;
         }
         while (curCol < col) {
            System.out.print (blank);
            curCol++;
         }
         System.out.print (" " + fmt.format (vals[k]));
         curCol++;
      }
      while (curCol < size) {
         System.out.print (blank);
         curCol++;
      }
      System.out.println ("");
   }

   /**
    * Solve the system
    * <pre>
    * [ M -G ] [ x ]   [ b ]
    * [      ] [   ] = [   ]
    * [ G Rg ] [lam]   [ a ]
    * </pre>
    * using the factorization of
    * <pre>
    * [ M  G ]
    * [      ] 
    * [ G -Rg ]
    * </pre>
    * 
    * This requires negating the value of lam from the original solve
    */
   public void solveMG (VectorNd x, VectorNd b) {
      myMatrixSolver.solve (x, b);
      if (computeResidualMG) {
         double res = 
            myPardiso.residual (
               myRowOffs, myColIdxs, myVals, mySizeM+myNumG, 
               x.getBuffer(), b.getBuffer(),(myTypeM & Matrix.SYMMETRIC) != 0);
         System.out.println ("solveRes=" + res + " size="+(mySizeM+myNumG));
      }
      // negate lam:
      double[] xbuf = x.getBuffer();
      for (int i = mySizeM; i < mySizeM + myNumG; i++) {
         xbuf[i] = -xbuf[i];
      }
   }

   public static boolean myDebug = false;

   /**
    * Solve the system
    * <pre>
    * [ M -G ] [ x ]   [ b ]
    * [      ] [   ] = [   ]
    * [ G Rg ] [lam]   [ a ]
    * </pre>
    * using the factorization of
    * <pre>
    * [ M  G ]
    * [      ] 
    * [ G -Rg ]
    * </pre>
    * 
    * This requires negating the value of lam from the original solve
    */
   public void solveMG (VectorNd xm, VectorNd xg, VectorNd bm, VectorNd bg) {
      double[] bbuf;
      double[] xbuf = myMGx.getBuffer();
      double[] ybuf = myMGy.getBuffer();

      bbuf = bm.getBuffer();
      for (int i = 0; i < mySizeM; i++) {
         xbuf[i] = bbuf[i];
      }
      bbuf = bg.getBuffer();
      for (int i = 0; i < myNumG; i++) {
         xbuf[i + mySizeM] = bbuf[i];
      }
      myMatrixSolver.solve (myMGy, myMGx);
      bbuf = xm.getBuffer();
      for (int i = 0; i < mySizeM; i++) {
         bbuf[i] = ybuf[i];
      }
      bbuf = xg.getBuffer();
      for (int i = 0; i < myNumG; i++) {
         bbuf[i] = -ybuf[i + mySizeM];
      }
   }

   FunctionTimer timer = new FunctionTimer();

   private void timerStart() {
      timer.start();
   }

   private void timerStop (String msg) {
      timer.stop();
      System.out.println (msg + " " + timer.result(1));
   }

   private void factorMG (
      Object M, int sizeM, SparseBlockMatrix GT, VectorNd Rg) {
      getCRSValues (M, sizeM, myNumVals, GT, Rg);
      if (mySolverType == SparseSolverId.Umfpack) {
         loadUmfpackValues (mySizeM + myNumG, myNumVals);
         int status = myUmfpack.factor (myUmfpackVals);
         if (status < 0) {
            throw new NumericalException ("Unable to factor matrix");
         }
         else if (status == UmfpackSolver.UMFPACK_WARNING_singular_matrix) {
            System.out.println (
               "Umfpack: Matrix is near singular, solve could fail");
         }
      }
      else {
         myPardiso.factor (myVals);
         if (myPardiso.getState() != PardisoSolver.FACTORED) {
            throw new NumericalException (
               "Pardiso: unable to factor matrix: size="+(mySizeM+myNumG)+
               ", nnz=" + myNumVals + ", error=" + myPardiso.getErrorMessage());
         }
      }
      myNumN = 0;
      myNT = null;
      myDT = null;
   }

   public int getNumNonZerosInFactors() {
      return myPardiso.getNumNonZerosInFactors();
   }

   public boolean lastSolveWasIterative() {
      return myLastSolveWasIterative;
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

}
