package maspack.solvers;

import java.io.IOException;
import java.util.Random;

import maspack.matrix.Matrix;
import maspack.matrix.Matrix.Partition;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.VectorNd;
import maspack.util.TestException;

/**
 * Test for {@link AccelerateSolver}. The common tests are provided by {@link
 * DirectSolverTestBase}. Unsymmetric matrices require Accelerate's LU
 * factorization (macOS 15.5 or later); without it, the unsymmetric tests
 * instead check that unsymmetric matrices are rejected.
 */
public class AccelerateSolverTest extends DirectSolverTestBase {

   protected DirectSolver createSolver() {
      return new AccelerateSolver();
   }

   /**
    * Accelerate's LDL^T factorization reports the inertia for SPD matrices
    * as well as symmetric indefinite ones.
    */
   protected int expectedNumNegEigenvaluesSPD() {
      return 0;
   }

   protected int expectedNumPosEigenvaluesSPD() {
      return 5;
   }

   /**
    * Runs the base unsymmetric test if LU is available. Otherwise,
    * unsymmetric matrices should be rejected at the analyze stage.
    */
   public void testUnsymmetric() {
      if (AccelerateSolver.isLUAvailable()) {
         super.testUnsymmetric();
         return;
      }
      DirectSolver solver = createSolver();
      double[] vals = new double[] { 1, 2, 3, 0, 4, 0, 5, 0, 6 };
      int[] rowOffs = incIndices (new int[] { 0, 3, 6, 9 });
      int[] colIdxs = incIndices (new int[] { 0, 1, 2, 0, 1, 2, 0, 1, 2 });
      try {
         solver.analyze (vals, colIdxs, rowOffs, 3, Matrix.INDEFINITE);
         throw new TestException (
            "analyze() of unsymmetric matrix did not fail");
      }
      catch (TestException e) {
         throw e;
      }
      catch (RuntimeException e) {
         // expected
      }
      solver.dispose();
   }

   protected void checkIterativeSolveInfo (DirectSolver solver, Object method) {
      super.checkIterativeSolveInfo (solver, method);
      check ("getLastIterativeTimes() != null",
             ((AccelerateSolver)solver).getLastIterativeTimes() != null);
   }

   /**
    * Tests direct and iterative solves of a larger unsymmetric matrix,
    * formed by perturbing the off-diagonal entries of the test matrix by up
    * to 10%.
    */
   public void testLargeUnsymmetricMatrix() throws IOException {
      if (!AccelerateSolver.isLUAvailable()) {
         return;
      }
      SparseMatrixNd S = loadTestMatrix();
      int size = S.rowSize();
      int numVals = S.numNonZeroVals (Partition.Full, size, size);
      double[] vals = new double[numVals];
      int[] colIdxs = new int[numVals];
      int[] rowOffs = new int[size+1];
      S.getCRSIndices (colIdxs, rowOffs, Partition.Full);
      S.getCRSValues (vals, Partition.Full);
      Random rand = new Random (0x1234);
      for (int i=0; i<size; i++) {
         for (int k=rowOffs[i]-1; k<rowOffs[i+1]-1; k++) {
            if (colIdxs[k]-1 != i) {
               vals[k] *= 1 + 0.2*(rand.nextDouble()-0.5);
            }
         }
      }
      S.setCRSValues (vals, colIdxs, rowOffs, numVals, size, Partition.Full);

      DirectSolver solver = createSolver();
      VectorNd bvec = new VectorNd (size);
      VectorNd xvec = new VectorNd (size);
      for (int i=0; i<size; i++) {
         bvec.set (i, 1);
      }
      solver.analyze (vals, colIdxs, rowOffs, size, Matrix.INDEFINITE);
      solver.factor (vals);
      solver.solve (xvec, bvec);
      checkEquals (
         "getNumNegEigenvalues()", solver.getNumNegEigenvalues(), -1);
      checkUnsymmetricSolution (S, xvec, bvec, 1e-10, "unsymmetric matrix");

      // perturb the values slightly and check that iterative solves,
      // preconditioned by the existing factorization, still work
      double norm = 0;
      for (int k=0; k<numVals; k++) {
         norm += vals[k]*vals[k];
      }
      norm = Math.sqrt (norm/numVals);
      for (int k=0; k<numVals; k++) {
         vals[k] += (rand.nextDouble()-0.5)*norm/10000;
      }
      S.setCRSValues (vals, colIdxs, rowOffs, numVals, size, Partition.Full);
      solver.setIterativeTolerance (1e-11);
      for (Object method : getIterativeMethods()) {
         setIterativeMethod (solver, method);
         String name = "unsymmetric iterative solve ("+method+")";
         xvec.setZero();
         int iterCnt = solver.iterativeSolve (
            vals, xvec.getBuffer(), bvec.getBuffer());
         check (name + ": iterativeSolve() > 0", iterCnt > 0);
         checkUnsymmetricSolution (S, xvec, bvec, 1e-9, name);
         checkIterativeSolveInfo (solver, method);
         if (verbose) {
            System.out.println (name + ": " + iterCnt);
         }
      }
      solver.dispose();
   }

   private void checkUnsymmetricSolution (
      SparseMatrixNd S, VectorNd xvec, VectorNd bvec, double tol, String msg) {
      VectorNd res = new VectorNd (S.rowSize());
      S.mul (res, xvec);
      res.sub (bvec);
      if (res.infinityNorm() > tol) {
         throw new TestException (
            msg + " error is " + res.infinityNorm() +
            "; expected less than " + tol);
      }
      if (verbose) {
         System.out.println (msg + " error=" + res.infinityNorm());
      }
   }

   public void test() throws IOException {
      testBasics();
      testLargeUnsymmetricMatrix();
   }

   public static void main (String[] args) {
      AccelerateSolverTest tester = new AccelerateSolverTest();

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.verbose = true;
         }
         else {
            tester.printUsageAndExit ("[-verbose]");
         }
      }
      if (!SparseSolverId.isDirectSolverAvailable (SparseSolverId.Accelerate)) {
         System.out.println (
            "Accelerate solver not available on this platform; test skipped");
         return;
      }
      tester.runtest();
   }
}
