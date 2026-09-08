/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.util.*;
import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;

import java.io.*;

/**
 * Base class for testing {@link DirectSolver} implementations. It provides the
 * tests which are common to all such solvers; subclasses supply the solver
 * itself, through {@link #createSolver}, along with any solver specific tests.
 *
 * <p>Tests which depend on optional functionality are guarded by the
 * corresponding capability queries ({@link DirectSolver#hasMultipleRhsSolves}
 * and {@link DirectSolver#hasIterativeSolves}), while the few expectations
 * which legitimately differ between solvers are supplied by overridable
 * methods such as {@link #expectedNumNegEigenvaluesSPD}.
 */
public abstract class DirectSolverTestBase extends UnitTest {

   protected static double EPS = 1e-12;

   protected boolean verbose = false;

   /**
    * Creates a new instance of the solver to be tested.
    *
    * @return new solver instance
    */
   protected abstract DirectSolver createSolver();

   /**
    * Number of negative eigenvalues that the solver should report after
    * factoring an SPD matrix. Solvers which compute the inertia only for
    * symmetric indefinite matrices return -1.
    *
    * @return expected number of negative eigenvalues for an SPD matrix
    */
   protected int expectedNumNegEigenvaluesSPD() {
      return -1;
   }

   /**
    * Number of positive eigenvalues that the solver should report after
    * factoring an SPD matrix of size 5. See {@link
    * #expectedNumNegEigenvaluesSPD}.
    *
    * @return expected number of positive eigenvalues for an SPD matrix
    */
   protected int expectedNumPosEigenvaluesSPD() {
      return -1;
   }

   /**
    * Enables or disables any "num perturbed pivots" message which the solver
    * may print, and returns the setting's previous value. Called around tests
    * which deliberately factor singular matrices, so that the test output
    * stays clean. Solvers without such a message need not override this.
    *
    * @param enable if {@code true}, enables the message
    * @return previous value of the setting
    */
   protected boolean setShowPerturbedPivots (boolean enable) {
      return enable;
   }

   // Increments the values of an integer array. We need this because CRS
   // indices were originally zero-based
   protected static int[] incIndices (int[] idxs) {
      int[] newIdxs = new int[idxs.length];
      for (int i=0; i<idxs.length; i++) {
	 newIdxs[i] = idxs[i]+1;
      }
      return newIdxs;
   }

   // test symmetric matrix, in upper triangular CRS format:
   //
   // M = [3 1 2 0 0
   //      1 0 1 2 0
   //      2 1 4 1 0
   //      0 2 1 0 6
   //      0 0 0 6 2]

   protected double[] symVals = new double[] { 3, 1, 2, 0, 1, 2, 4, 1, 0, 6, 2 };
   protected int[] symRowOffs = incIndices (new int[] { 0, 3, 6, 8, 10, 11 });
   protected int[] symColIdxs =
      incIndices (new int[] { 0, 1, 2, 1, 2, 3, 2, 3, 3, 4, 4 });
   protected double[] symB = new double[] { 1, 2, 3, 4, 5 };
   protected double[] symXchk = new double[] {
      0.111111111111111,
      -0.88888888888888,
      0.777777777777777,
      0.555555555555555,
      0.833333333333333
   };

   // SPD test matrix: tridiagonal and diagonally dominant
   protected double[] spdVals = new double[] { 4, 1, 4, 1, 4, 1, 4, 1, 4 };
   protected int[] spdColIdxs = incIndices (new int[] { 0, 1, 1, 2, 2, 3, 3, 4, 4 });
   protected int[] spdRowOffs = incIndices (new int[] { 0, 2, 4, 6, 8, 9 });

   protected void checkSolution (double[] x, double[] xchk) {
      checkEquals (
         "solution", new VectorNd(x), new VectorNd(xchk), EPS);
   }

   /**
    * Checks that M x = b to within a tolerance, where M is given in CRS
    * format.
    */
   protected void checkResidual (
      int[] rowOffs, int[] colIdxs, double[] vals, int size,
      double[] x, double[] b, boolean symmetric, double tol) {

      double res = DirectSolver.residual (
         rowOffs, colIdxs, vals, size, x, b, symmetric);
      if (res > tol) {
         throw new TestException (
            "residual is " + res + "; expected less than " + tol);
      }
   }

   /**
    * Tests the analyze/factor/solve sequence for a symmetric indefinite
    * matrix supplied as a Matrix object.
    */
   public void testSymmetric() {
      DirectSolver solver = createSolver();

      MatrixNd M = new MatrixNd (5, 5);
      M.setCRSValues (
         symVals, symColIdxs, symRowOffs, 11, 5, Partition.UpperTriangular);
      double[] x = new double[5];

      solver.analyze (M, 5, Matrix.SYMMETRIC);
      check ("state == ANALYZED", solver.getState() == DirectSolver.ANALYZED);
      solver.factor();
      check ("state == FACTORED", solver.getState() == DirectSolver.FACTORED);

      checkEquals ("getNumPerturbedPivots()", solver.getNumPerturbedPivots(), 0);
      check ("getNumNonZerosInFactors() > 0",
             solver.getNumNonZerosInFactors() > 0);
      check ("getErrorMessage() == null", solver.getErrorMessage() == null);

      solver.solve (x, symB);
      checkSolution (x, symXchk);

      // check the VectorNd version of solve
      VectorNd xvec = new VectorNd(5);
      solver.solve (xvec, new VectorNd(symB));
      checkEquals (
         "symmetric solution (VectorNd)", xvec, new VectorNd(symXchk), EPS);

      if (solver.hasMultipleRhsSolves()) {
         // check with multiple right hand sides
         int nrhs = 3;
         MatrixNd B = new MatrixNd (nrhs, 5);
         MatrixNd X = new MatrixNd (nrhs, 5);
         MatrixNd Xchk = new MatrixNd (nrhs, 5);
         VectorNd b = new VectorNd (symB);
         VectorNd xchk = new VectorNd (symXchk);
         for (int i=0; i<nrhs; i++) {
            B.setRow (i, b);
            Xchk.setRow (i, xchk);
            b.scale (2);
            xchk.scale (2);
         }
         solver.solve (X.getBuffer(), B.getBuffer(), nrhs);
         checkEquals ("solve with multiple rhs", X, Xchk, EPS);
      }

      // now change the values but keep the topology:
      //
      // M = [3 1 2 0 0
      //      1 10 1 2 0
      //      2 1 4 1 0
      //      0 2 1 10 5
      //      0 0 0 5 2]
      double[] vals4 = new double[] { 3, 1, 2, 10, 1, 2, 4, 1, 10, 5, 2 };
      M.setCRSValues (
         vals4, symColIdxs, symRowOffs, 11, 5, Partition.UpperTriangular);
      solver.factor();
      solver.solve (x, symB);
      checkSolution (
         x, new double[] {
            0.6032064128257,
            -0.4368737474950,
            -0.1863727454910,
            2.9759519038076,
            -4.9398797595190
         });
      solver.dispose();
   }

   /**
    * Tests the inertia reported for a symmetric indefinite matrix.
    */
   public void testInertia() {
      DirectSolver solver = createSolver();

      solver.analyze (symVals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      solver.factor (symVals);
      checkEquals ("getNumNegEigenvalues()", solver.getNumNegEigenvalues(), 2);
      checkEquals ("getNumPosEigenvalues()", solver.getNumPosEigenvalues(), 3);
      solver.dispose();
   }

   /**
    * Tests the analyze/factor/solve sequence using the CRS interface, which
    * is the one used by KKTSolver and MurtyMechSolver.
    */
   public void testCRSInterface() {
      DirectSolver solver = createSolver();
      double[] x = new double[5];

      solver.analyze (symVals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      check ("state == ANALYZED", solver.getState() == DirectSolver.ANALYZED);
      solver.factor (symVals);
      check ("state == FACTORED", solver.getState() == DirectSolver.FACTORED);

      solver.solve (x, symB);
      checkSolution (x, symXchk);
      checkResidual (
         symRowOffs, symColIdxs, symVals, 5, x, symB, true, EPS);

      // reanalyze with a different structure, to check that the solver
      // properly reinitializes itself
      double[] vals = new double[] { 4, 1, 5, 1, 6 };
      int[] colIdxs = incIndices (new int[] { 0, 1, 1, 2, 2 });
      int[] rowOffs = incIndices (new int[] { 0, 2, 4, 5 });
      double[] b = new double[] { 1, 2, 3 };
      double[] x3 = new double[3];

      solver.analyze (vals, colIdxs, rowOffs, 3, Matrix.SYMMETRIC);
      solver.factor (vals);
      solver.solve (x3, b);
      checkResidual (rowOffs, colIdxs, vals, 3, x3, b, true, EPS);
      solver.dispose();
   }

   /**
    * Tests an unsymmetric matrix:
    *
    * M = [1 2 3
    *      0 4 0
    *      5 0 6]
    */
   public void testUnsymmetric() {
      DirectSolver solver = createSolver();

      double[] vals = new double[] { 1, 2, 3, 0, 4, 0, 5, 0, 6 };
      int[] rowOffs = incIndices (new int[] { 0, 3, 6, 9 });
      int[] colIdxs = incIndices (new int[] { 0, 1, 2, 0, 1, 2, 0, 1, 2 });
      double[] x = new double[3];
      double[] b1 = new double[] { 1, 2, 3 };
      double[] b2 = new double[] { 4, 5, 6 };

      MatrixNd M = new MatrixNd (3, 3);
      M.setCRSValues (vals, colIdxs, rowOffs, 9, 3, Partition.Full);
      solver.analyze (M, 3, Matrix.INDEFINITE);
      solver.factor();

      // eigenvalue counts are only available for symmetric matrices
      checkEquals ("getNumNegEigenvalues()", solver.getNumNegEigenvalues(), -1);
      checkEquals ("getNumPosEigenvalues()", solver.getNumPosEigenvalues(), -1);

      solver.solve (x, b1);
      checkSolution (x, new double[] { 1, 0.5, -1/3.0 });
      solver.solve (x, b2);
      checkSolution (x, new double[] { 1, 1.25, 1/6.0 });

      // analyzeAndFactor uses the unsymmetric path as well
      solver.analyzeAndFactor (M);
      solver.solve (x, b1);
      checkSolution (x, new double[] { 1, 0.5, -1/3.0 });
      solver.dispose();
   }

   /**
    * Tests an SPD matrix, for which solvers normally use a Cholesky
    * factorization.
    */
   public void testSPD() {
      DirectSolver solver = createSolver();

      double[] b = new double[] { 1, 2, 3, 4, 5 };
      double[] x = new double[5];

      solver.analyze (spdVals, spdColIdxs, spdRowOffs, 5, Matrix.SPD);
      solver.factor (spdVals);
      solver.solve (x, b);
      checkResidual (spdRowOffs, spdColIdxs, spdVals, 5, x, b, true, EPS);
      checkEquals ("getNumPerturbedPivots()", solver.getNumPerturbedPivots(), 0);
      checkEquals (
         "getNumNegEigenvalues()", solver.getNumNegEigenvalues(),
         expectedNumNegEigenvaluesSPD());
      checkEquals (
         "getNumPosEigenvalues()", solver.getNumPosEigenvalues(),
         expectedNumPosEigenvaluesSPD());
      solver.dispose();
   }

   /**
    * Tests that a singular matrix is detected and reported through the
    * perturbed pivot count, rather than causing the factorization to fail.
    */
   public void testSingularMatrix() {
      DirectSolver solver = createSolver();
      boolean showPivots = setShowPerturbedPivots (false);

      // the symmetric test matrix, with its last row and column zeroed
      double[] vals = new double[] { 3, 1, 2, 0, 1, 2, 4, 1, 0, 0, 0 };
      double[] b = new double[] { 1, 2, 3, 4, 0 };
      double[] x = new double[5];

      solver.analyze (vals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      solver.factor (vals);
      check ("getNumPerturbedPivots() > 0", solver.getNumPerturbedPivots() > 0);
      check ("state == FACTORED", solver.getState() == DirectSolver.FACTORED);
      solver.solve (x, b);
      solver.dispose();
      setShowPerturbedPivots (showPivots);
   }

   /**
    * Tests state handling and reuse of the solver after a call to dispose().
    */
   public void testStateAndReuse() {
      DirectSolver solver = createSolver();

      MatrixNd M = new MatrixNd (5, 5);
      M.setCRSValues (
         symVals, symColIdxs, symRowOffs, 11, 5, Partition.UpperTriangular);
      double[] x = new double[5];

      solver.analyze (M, 5, Matrix.SYMMETRIC);
      solver.factor();
      solver.solve (x, symB);
      checkSolution (x, symXchk);

      solver.dispose();
      check ("state == UNSET after dispose",
             solver.getState() == DirectSolver.UNSET);

      // reuse the solver after disposing it
      solver.analyze (M, 5, Matrix.SYMMETRIC);
      solver.factor();
      solver.solve (x, symB);
      checkSolution (x, symXchk);
      solver.dispose();
   }

   /**
    * Loads the symmetric 2529 x 2529 test matrix used by the larger tests.
    *
    * @return test matrix
    */
   protected SparseMatrixNd loadTestMatrix() throws IOException {
      SparseMatrixNd S = new SparseMatrixNd (2529, 2529);
      ReaderTokenizer rtok =
         new ReaderTokenizer (
            new BufferedReader (
               new FileReader (
                  PathFinder.getSourceRelativePath (
                     DirectSolverTestBase.class, "testMatrix.mat"))));
      try {
         S.scan (rtok);
      }
      finally {
         rtok.close();
      }
      return S;
   }

   /**
    * Tests the solver on a larger matrix, read from testMatrix.mat.
    */
   public void testLargeMatrix() throws IOException {
      DirectSolver solver = createSolver();

      SparseMatrixNd S = loadTestMatrix();
      int size = S.rowSize();
      VectorNd bvec = new VectorNd (size);
      VectorNd xvec = new VectorNd (size);
      VectorNd check = new VectorNd (size);
      for (int i=0; i<size; i++) {
         bvec.set (i, 1);
      }
      double tol = 1e-10;

      solver.analyze (S, size, Matrix.SYMMETRIC);
      solver.factor();
      solver.solve (xvec, bvec);

      check ("getNumNonZerosInFactors() > 0",
             solver.getNumNonZerosInFactors() > 0);
      checkLargeMatrixSolution (S, xvec, bvec, tol, "large matrix");
      if (verbose) {
         System.out.println (
            "nnz in factors=" + solver.getNumNonZerosInFactors());
      }

      // refactor and solve with iterative refinement enabled
      int savedRefinementSteps = solver.getMaxRefinementSteps();
      solver.setMaxRefinementSteps (2);
      solver.factor();
      solver.solve (xvec, bvec);
      checkLargeMatrixSolution (
         S, xvec, bvec, tol, "large matrix with refinement");
      solver.setMaxRefinementSteps (savedRefinementSteps);

      if (solver.hasIterativeSolves()) {
         // perturb the matrix slightly and check that an iterative solve,
         // preconditioned by the existing factorization, still works
         int numVals = S.numNonZeroVals (
            Partition.UpperTriangular, size, size);
         double[] vals = new double[numVals];
         int[] colIdxs = new int[numVals];
         int[] rowOffs = new int[size+1];
         S.getCRSIndices (colIdxs, rowOffs, Partition.UpperTriangular);
         S.getCRSValues (vals, Partition.UpperTriangular);
         double norm = 0;
         for (int k=0; k<numVals; k++) {
            norm += vals[k]*vals[k];
         }
         norm = Math.sqrt (norm/numVals);
         java.util.Random rand = new java.util.Random (0x1234);
         for (int k=0; k<numVals; k++) {
            vals[k] += (rand.nextDouble()-0.5)*norm/10000;
         }
         S.setCRSValues (
            vals, colIdxs, rowOffs, numVals, size,
            Partition.UpperTriangular);
         int iterCnt = solver.iterativeSolve (
            vals, xvec.getBuffer(), bvec.getBuffer(), 10);
         check ("iterativeSolve() > 0", iterCnt > 0);
         checkLargeMatrixSolution (S, xvec, bvec, 1e-9, "iterative solve");
         if (verbose) {
            System.out.println ("iterative solve: " + iterCnt);
         }
      }
      solver.dispose();
   }

   private void checkLargeMatrixSolution (
      SparseMatrixNd S, VectorNd xvec, VectorNd bvec, double tol, String msg) {

      VectorNd check = new VectorNd (S.rowSize());
      S.mul (check, xvec);
      check.sub (bvec);
      if (check.infinityNorm() > tol) {
         throw new TestException (
            msg + " error is " + check.infinityNorm() +
            "; expected less than " + tol);
      }
      if (verbose) {
         System.out.println (msg + " error=" + check.infinityNorm());
      }
   }

   /**
    * Runs all the solver independent tests. Subclasses should call this from
    * their own <code>test()</code> method, along with any solver specific
    * tests.
    */
   public void testBasics() throws IOException {
      testSymmetric();
      testInertia();
      testCRSInterface();
      testUnsymmetric();
      testSPD();
      testSingularMatrix();
      testStateAndReuse();
      testLargeMatrix();
   }

   public void test() throws IOException {
      testBasics();
   }
}
