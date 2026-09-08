/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.util.*;
import maspack.matrix.*;
import maspack.solvers.MumpsSolver.ReorderMethod;

import java.io.*;

/**
 * Unit test for MumpsSolver. The solver independent tests are inherited from
 * {@link DirectSolverTestBase}; this class adds those which are specific to
 * MUMPS.
 */
public class MumpsSolverTest extends DirectSolverTestBase {

   protected DirectSolver createSolver() {
      return new MumpsSolver();
   }

   /**
    * MUMPS reports the inertia for SPD matrices as well as symmetric
    * indefinite ones.
    */
   protected int expectedNumNegEigenvaluesSPD() {
      return 0;
   }

   protected int expectedNumPosEigenvaluesSPD() {
      return 5;
   }

   protected boolean setShowPerturbedPivots (boolean enable) {
      boolean prev = MumpsSolver.getShowPerturbedPivots();
      MumpsSolver.setShowPerturbedPivots (enable);
      return prev;
   }

   /**
    * Tests the MUMPS specific mechanisms for handling singular matrices: null
    * pivot detection, which is enabled by default, and static pivoting, which
    * is the alternative.
    */
   public void testNullPivotDetection() {
      MumpsSolver solver = new MumpsSolver();
      boolean showPivots = MumpsSolver.getShowPerturbedPivots();
      MumpsSolver.setShowPerturbedPivots (false);

      // the symmetric test matrix, with its last row and column zeroed
      double[] vals = new double[] { 3, 1, 2, 0, 1, 2, 4, 1, 0, 0, 0 };
      double[] b = new double[] { 1, 2, 3, 4, 0 };
      double[] x = new double[5];

      check ("null pivot detection enabled by default",
             solver.getNullPivotDetection());
      solver.analyze (vals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      solver.factor (vals);
      checkEquals ("getNumNullPivots()", solver.getNumNullPivots(), 1);
      checkEquals ("getNumPerturbedPivots()", solver.getNumPerturbedPivots(), 1);
      checkEquals ("getSPDZeroPivot()", solver.getSPDZeroPivot(), 5);

      // with null pivot detection disabled, the factorization should fail
      solver.setNullPivotDetection (false);
      solver.analyze (vals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      try {
         solver.factor (vals);
         throw new TestException (
            "factoring a singular matrix did not throw an exception");
      }
      catch (NumericalException e) {
         check ("error message is set", solver.getErrorMessage() != null);
         if (verbose) {
            System.out.println ("expected error: " + e.getMessage());
         }
      }

      // static pivoting perturbs tiny pivots instead
      solver.setStaticPivotTolerance (0.0);
      solver.analyze (vals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      solver.factor (vals);
      checkEquals ("getNumTinyPivots()", solver.getNumTinyPivots(), 1);
      checkEquals ("getNumPerturbedPivots()", solver.getNumPerturbedPivots(), 1);
      solver.solve (x, b);

      solver.dispose();
      MumpsSolver.setShowPerturbedPivots (showPivots);
   }

   /**
    * Tests the statistics which MUMPS reports for a larger factorization,
    * including the reorder method actually used.
    */
   public void testStatistics() throws IOException {
      MumpsSolver solver = new MumpsSolver();

      SparseMatrixNd S = loadTestMatrix();
      int size = S.rowSize();
      VectorNd b = new VectorNd (size);
      VectorNd x = new VectorNd (size);
      b.setAll (1.0);

      solver.setReorderMethod (ReorderMethod.METIS);
      solver.analyze (S, size, Matrix.SYMMETRIC);
      solver.factor();
      solver.solve (x, b);

      checkEquals (
         "reorder method used", solver.getReorderMethodUsed(),
         ReorderMethod.METIS);
      check ("getPeakAnalysisMemoryUsage() > 0",
             solver.getPeakAnalysisMemoryUsage() > 0);
      check ("getFactorSolveMemoryUsage() > 0",
             solver.getFactorSolveMemoryUsage() > 0);
      check ("getNumDelayedPivots() >= 0", solver.getNumDelayedPivots() >= 0);
      if (verbose) {
         System.out.println (
            "nnz in factors=" + solver.getNumNonZerosInFactors());
         System.out.println (
            "analysis memory (kb)=" + solver.getPeakAnalysisMemoryUsage());
         System.out.println (
            "factor memory (kb)=" + solver.getFactorSolveMemoryUsage());
         System.out.println (
            "delayed pivots=" + solver.getNumDelayedPivots());
      }
      solver.dispose();
   }

   public void testParameterAccessMethods() {
      MumpsSolver solver = new MumpsSolver();

      int nsteps = solver.getMaxRefinementSteps();
      solver.setMaxRefinementSteps (3);
      checkEquals ("getMaxRefinementSteps()", solver.getMaxRefinementSteps(), 3);
      solver.setMaxRefinementSteps (nsteps);

      ReorderMethod[] methods = new ReorderMethod[] {
         ReorderMethod.AMD, ReorderMethod.AMF, ReorderMethod.SCOTCH,
         ReorderMethod.PORD, ReorderMethod.METIS, ReorderMethod.QAMD,
         ReorderMethod.DEFAULT };
      for (ReorderMethod method : methods) {
         solver.setReorderMethod (method);
         checkEquals ("getReorderMethod()", solver.getReorderMethod(), method);
      }
      solver.setReorderMethod (ReorderMethod.DEFAULT);

      solver.setStaticPivotTolerance (1e-8);
      checkEquals (
         "getStaticPivotTolerance()", solver.getStaticPivotTolerance(),
         1e-8, 0);
      solver.setStaticPivotTolerance (-1.0);

      solver.setNullPivotDetection (false);
      check ("getNullPivotDetection()", !solver.getNullPivotDetection());
      solver.setNullPivotDetection (true);
      check ("getNullPivotDetection()", solver.getNullPivotDetection());

      solver.setNullPivotThreshold (1e-10);
      checkEquals (
         "getNullPivotThreshold()", solver.getNullPivotThreshold(), 1e-10, 0);
      solver.setNullPivotThreshold (0.0);

      solver.setApplyScaling (1);
      check ("getApplyScaling()", solver.getApplyScaling());
      solver.setApplyScaling (0);
      check ("getApplyScaling()", !solver.getApplyScaling());
      solver.setApplyScaling (-1);

      solver.setApplyWeightedMatchings (1);
      check ("getApplyWeightedMatchings()", solver.getApplyWeightedMatchings());
      solver.setApplyWeightedMatchings (0);
      check ("getApplyWeightedMatchings()", !solver.getApplyWeightedMatchings());
      solver.setApplyWeightedMatchings (-1);

      solver.setSymOrderingStrategy (2);
      checkEquals (
         "getSymOrderingStrategy()", solver.getSymOrderingStrategy(), 2);
      solver.setSymOrderingStrategy (0);

      solver.setWorkspaceIncrease (50);
      checkEquals ("getWorkspaceIncrease()", solver.getWorkspaceIncrease(), 50);

      solver.setMaxWorkingMemory (1000);
      checkEquals (
         "getMaxWorkingMemory()", solver.getMaxWorkingMemory(), 1000);
      solver.setMaxWorkingMemory (0);

      // thread setting is global to the process, so restore it afterwards
      int numThreads = solver.getNumThreads();
      solver.setNumThreads (1);
      checkEquals ("getNumThreads()", solver.getNumThreads(), 1);
      solver.setNumThreads (numThreads);

      solver.dispose();
   }

   public void test() throws IOException {
      testBasics();
      testNullPivotDetection();
      testStatistics();
      testParameterAccessMethods();
   }

   public static void main (String[] args) {
      MumpsSolverTest tester = new MumpsSolverTest();
      MumpsSolver.printThreadInfo = false;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.verbose = true;
         }
         else {
            tester.printUsageAndExit ("[-verbose]");
         }
      }
      if (!MumpsSolver.isAvailable()) {
         // the MUMPS native library is not built for all platforms
         System.out.println ("MUMPS not available; test skipped");
         return;
      }
      tester.runtest();
   }
}
