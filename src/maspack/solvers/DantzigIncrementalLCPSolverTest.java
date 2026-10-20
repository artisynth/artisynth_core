/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;

import maspack.solvers.LCPSolver.Status;

/**
 * Test suite for {@link DantzigIncrementalLCPSolver}. Unlike the other LCP
 * solver tests, this one does not use the standard {@link LCPSolverTestBase}
 * battery directly, because that battery mutates a single {@code M} object in
 * place between problems, which violates the incremental solver's contract that
 * {@code M} not change (without a call to {@link
 * DantzigIncrementalLCPSolver#invalidateFactorization}) between reused solves.
 *
 * <p>Instead it exercises the actual contract: it replays sequences of problems
 * that share a fixed {@code M} while {@code q} (and, for BLCPs, the bounds)
 * vary, with the active set carried between solves, and checks that every
 * solution is valid and matches a reference cold {@link DantzigLCPSolver} solve.
 * It also checks that the {@code invalidateFactorization} path produces correct
 * results when {@code M} does change.
 */
public class DantzigIncrementalLCPSolverTest extends LCPSolverTestBase {

   private DantzigIncrementalLCPSolver mySolver;
   private DantzigLCPSolver myRef;
   private java.util.Random myRandom;

   private static double INF = Double.POSITIVE_INFINITY;

   public DantzigIncrementalLCPSolverTest() {
      super();
      mySolver = new DantzigIncrementalLCPSolver();
      myRef = new DantzigLCPSolver();
      myRandom = maspack.util.RandomGenerator.get();
   }

   public LCPSolver getSolver() {
      return mySolver;
   }

   private void makeMatrix (MatrixNd M, int size, boolean semiDefinite) {
      if (semiDefinite) {
         MatrixNd N = new MatrixNd (size, size - size/2);
         N.setRandom();
         M.mulTransposeRight (N, N);
      }
      else {
         M.setRandom();
         M.mulTransposeRight (M, M);
      }
   }

   // builds an LCP rhs q for which (zsol, 0) is a solution, with zsol >= 0
   private void makeLCPRhs (VectorNd q, MatrixNd M) {
      int n = M.rowSize();
      VectorNd zsol = new VectorNd (n);
      for (int i=0; i<n; i++) {
         zsol.set (i, myRandom.nextBoolean() ? 0 : myRandom.nextDouble());
      }
      q.mul (M, zsol);
      q.negate();
   }

   // builds a BLCP rhs q (with fixed bounds lo/hi) for which a known
   // complementary point is a solution
   private void makeBLCPRhs (
      VectorNd q, MatrixNd M, VectorNd lo, VectorNd hi) {
      int n = M.rowSize();
      VectorNd zsol = new VectorNd (n);
      VectorNd wsol = new VectorNd (n);
      for (int i=0; i<n; i++) {
         double l = lo.get(i);
         double h = hi.get(i);
         boolean canLower = (l != -INF);
         boolean canUpper = (h != INF);
         int pick = myRandom.nextInt (3); // 0=basic, 1=lower, 2=upper
         if (pick == 1 && !canLower) {
            pick = 0;
         }
         if (pick == 2 && !canUpper) {
            pick = 0;
         }
         if (pick == 0) {
            double lc = (l == -INF ? -1 : l);
            double hc = (h == INF ? 1 : h);
            zsol.set (i, lc + (hc-lc)*myRandom.nextDouble());
            wsol.set (i, 0);
         }
         else if (pick == 1) {
            zsol.set (i, l);
            wsol.set (i, myRandom.nextDouble());
         }
         else {
            zsol.set (i, h);
            wsol.set (i, -myRandom.nextDouble());
         }
      }
      // q = wsol - M zsol
      q.mul (M, zsol);
      q.negate();
      q.add (wsol);
   }

   boolean vecsClose (VectorNd a, VectorNd b, double tol) {
      double mag = (a.norm() + b.norm())/2;
      if (mag > 1) {
         tol *= mag;
      }
      return a.epsilonEquals (b, tol);
   }

   /**
    * Replay a sequence of LCPs that share a fixed matrix M while q varies, with
    * the active set carried between solves.
    */
   public void testLCPSequence (int size, int nsteps, boolean semiDefinite) {
      MatrixNd M = new MatrixNd (size, size);
      makeMatrix (M, size, semiDefinite);
      VectorNd q = new VectorNd (size);
      VectorNd z = new VectorNd (size);
      VectorNd zref = new VectorNd (size);
      VectorNi state = new VectorNi (size);
      VectorNi refState = new VectorNi (size);

      LCPSolver.clearState (state);
      mySolver.invalidateFactorization();

      for (int step=0; step<nsteps; step++) {
         makeLCPRhs (q, M);

         Status st = mySolver.solve (z, state, M, q);
         if (st != Status.SOLVED) {
            throw new TestException (
               "incremental solve returned "+st+" at step "+step);
         }
         checkLCPSolution (z, M, q, state, mySolver.getLastSolveTol());

         // for the definite case the solution is unique; compare to a cold solve
         if (!semiDefinite) {
            LCPSolver.clearState (refState);
            myRef.solve (zref, refState, M, q);
            if (!vecsClose (z, zref, 1e-6)) {
               System.out.println ("z   =" + z);
               System.out.println ("zref=" + zref);
               throw new TestException (
                  "incremental result differs from cold solve at step "+step);
            }
         }
      }
   }

   /**
    * Replay a sequence of BLCPs that share a fixed matrix M and fixed bounds
    * while q varies, with the active set carried between solves.
    */
   public void testBLCPSequence (int size, int nsteps, boolean semiDefinite) {
      MatrixNd M = new MatrixNd (size, size);
      makeMatrix (M, size, semiDefinite);
      VectorNd lo = new VectorNd (size);
      VectorNd hi = new VectorNd (size);
      setRandomBounds (lo, hi, 0, size);

      VectorNd q = new VectorNd (size);
      VectorNd z = new VectorNd (size);
      VectorNd w = new VectorNd (size);
      VectorNd zref = new VectorNd (size);
      VectorNd wref = new VectorNd (size);
      VectorNi state = new VectorNi (size);
      VectorNi refState = new VectorNi (size);

      LCPSolver.clearState (state);
      mySolver.invalidateFactorization();

      for (int step=0; step<nsteps; step++) {
         makeBLCPRhs (q, M, lo, hi);

         Status st = mySolver.solve (z, w, state, M, q, lo, hi, 0);
         if (st != Status.SOLVED) {
            throw new TestException (
               "incremental BLCP solve returned "+st+" at step "+step);
         }
         checkBLCPSolution (z, w, M, q, lo, hi, 0, state, 1e-7);

         if (!semiDefinite) {
            LCPSolver.clearState (refState);
            myRef.solve (zref, wref, refState, M, q, lo, hi, 0);
            if (!vecsClose (z, zref, 1e-6)) {
               throw new TestException (
                  "incremental BLCP result differs from cold solve at step "
                  +step);
            }
         }
      }
   }

   /**
    * Check that when M itself changes between solves (each accompanied by an
    * invalidateFactorization call), the solver still produces correct results,
    * matching a cold solve.
    */
   public void testChangingMatrix (int size, int nsteps) {
      MatrixNd M = new MatrixNd (size, size);
      VectorNd q = new VectorNd (size);
      VectorNd z = new VectorNd (size);
      VectorNd zref = new VectorNd (size);
      VectorNi state = new VectorNi (size);
      VectorNi refState = new VectorNi (size);

      LCPSolver.clearState (state);
      for (int step=0; step<nsteps; step++) {
         // change M (same object, new contents) and signal the change
         makeMatrix (M, size, /*semiDefinite=*/false);
         mySolver.invalidateFactorization();
         makeLCPRhs (q, M);

         Status st = mySolver.solve (z, state, M, q);
         if (st != Status.SOLVED) {
            throw new TestException (
               "changing-matrix solve returned "+st+" at step "+step);
         }
         checkLCPSolution (z, M, q, state, mySolver.getLastSolveTol());

         LCPSolver.clearState (refState);
         myRef.solve (zref, refState, M, q);
         if (!vecsClose (z, zref, 1e-6)) {
            throw new TestException (
               "changing-matrix result differs from cold solve at step "+step);
         }
      }
   }

   public void test() {
      int size = 30;
      for (int trial=0; trial<40; trial++) {
         testLCPSequence (size, 50, /*semiDefinite=*/false);
         testLCPSequence (size, 50, /*semiDefinite=*/true);
         testBLCPSequence (size, 50, /*semiDefinite=*/false);
         testBLCPSequence (size, 50, /*semiDefinite=*/true);
      }
      testChangingMatrix (size, 200);
   }

   public static void main (String[] args) {
      DantzigIncrementalLCPSolverTest tester =
         new DantzigIncrementalLCPSolverTest();
      tester.runtest();
   }
}
