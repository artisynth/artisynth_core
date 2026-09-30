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
   /**
    * Tests the scaling setting (MUMPS parameter ICNTL(8)), and that repeated
    * factorizations following a single analyze remain accurate as the matrix
    * values change, which is how the solver is used by a simulation.
    *
    * <p>MUMPS may compute its scaling during the analysis phase, in which case
    * it is reused by later factorizations and becomes stale as the values
    * change. Pivots can then appear to be null, and with null pivot detection
    * enabled such a pivot is "fixed", silently corrupting the solution. This
    * is why the default scaling is 7, which rescales for each factorization.
    */
   public void testScaling() {
      MumpsSolver solver = new MumpsSolver();
      boolean showPivots = MumpsSolver.getShowPerturbedPivots();
      MumpsSolver.setShowPerturbedPivots (false);

      // -1 selects the adaptive policy
      checkEquals ("default getScaling()", solver.getScaling(), -1);
      for (int value : new int[] { 0, 1, 7, 8, 77 }) {
         solver.setScaling (value);
         checkEquals ("getScaling()", solver.getScaling(), value);
      }
      solver.setScaling (-1);
      solver.dispose();

      // saddle point system [ M G'; G 0 ], with M symmetric positive definite
      // and O(1), and the constraint entries much smaller, as for the
      // incompressibility constraints of an FEM model
      int sizeM = 60;
      int sizeG = 12;
      int size = sizeM + sizeG;
      int[] rowOffs = new int[size+1];
      int[] colIdxs = new int[(2*sizeM-1) + sizeM*sizeG + sizeG];
      int k = 0;
      for (int i=0; i<sizeM; i++) {
         rowOffs[i] = k+1;
         colIdxs[k++] = i+1;                      // diagonal
         if (i < sizeM-1) {
            colIdxs[k++] = i+2;                   // off diagonal
         }
         for (int j=0; j<sizeG; j++) {
            colIdxs[k++] = sizeM+j+1;             // constraint block
         }
      }
      for (int j=0; j<sizeG; j++) {
         rowOffs[sizeM+j] = k+1;                  // zero diagonal for R
         colIdxs[k++] = sizeM+j+1;
      }
      rowOffs[size] = k+1;
      int numVals = k;

      double[] vals = new double[numVals];
      double[] b = new double[size];
      double[] x = new double[size];
      for (int i=0; i<size; i++) {
         b[i] = 1.0 + 0.1*(i%7);
      }
      // scalings which should all give accurate solves: the adaptive default
      // (-1), the fixed factorization scalings, and 77, for which MUMPS may
      // scale during analysis
      for (int scaling : new int[] { -1, 0, 1, 7, 8, 77 }) {
         solver = new MumpsSolver();
         solver.setScaling (scaling);
         for (int step=0; step<8; step++) {
            RandomGenerator.setSeed (0x1234);
            int p = 0;
            double drift = 1 + 0.05*step;
            for (int i=0; i<sizeM; i++) {
               vals[p++] = (4.0 + RandomGenerator.nextDouble (0, 1))*drift;
               if (i < sizeM-1) {
                  vals[p++] = -1.0 - 0.1*RandomGenerator.nextDouble (0, 1);
               }
               for (int j=0; j<sizeG; j++) {
                  vals[p++] =
                     1e-3*RandomGenerator.nextDouble (-1, 1)*drift;
               }
            }
            for (int j=0; j<sizeG; j++) {
               vals[p++] = 0;
            }
            if (step == 0) {
               solver.analyze (vals, colIdxs, rowOffs, size, Matrix.SYMMETRIC);
            }
            solver.factor (vals);
            solver.solve (x, b);
            checkEquals (
               "num perturbed pivots, scaling "+scaling+" step "+step,
               solver.getNumPerturbedPivots(), 0);
            checkResidual (
               rowOffs, colIdxs, vals, size, x, b,
               /*symmetric=*/true, 1e-8);
         }
         solver.dispose();
      }
      MumpsSolver.setShowPerturbedPivots (showPivots);
   }

   /**
    * Tests that the factorization and solve phases use the same number of
    * threads as the analysis, which MUMPS requires when ICNTL(48)
    * multithreaded tree parallelism is active (its default), failing with
    * error -58 otherwise.
    *
    * <p>The OpenMP thread count is a property of the process, so another
    * solver in the same process can change it between our phases: when it is
    * created, or when its own analysis is throttled for a smaller matrix (see
    * {@code maxThreadsNnz}). The solver therefore re-asserts the thread count
    * for each phase.
    */
   public void testThreadConsistency() {
      MumpsSolver solver = new MumpsSolver();
      int nthreads = Math.min (4, solver.getNumThreads());
      if (nthreads < 2) {
         // nothing to test if the process has only one thread available
         solver.dispose();
         return;
      }
      solver.setNumThreads (nthreads);
      double[] x = new double[5];
      solver.analyze (symVals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      solver.factor (symVals);
      solver.solve (x, symB);

      // another solver changes the process wide thread count
      MumpsSolver other = new MumpsSolver();
      other.setNumThreads (1);

      // this solver's later phases must still work
      solver.factor (symVals);
      solver.solve (x, symB);
      checkSolution (x, symXchk);
      checkEquals (
         "thread count restored for the phase", solver.getNumThreads(),
         nthreads);

      other.dispose();
      solver.dispose();
   }

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

   protected void checkIterativeSolveInfo (DirectSolver solver, Object method) {
      super.checkIterativeSolveInfo (solver, method);
      check ("getLastIterativeTimes() != null",
             ((MumpsSolver)solver).getLastIterativeTimes() != null);
   }

   public void test() throws IOException {
      testThreadConsistency();
      testScaling();
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
