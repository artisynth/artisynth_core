/**
 * Copyright (c) 2026, by the Authors: Alexander Denk
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.util.ArrayList;
import java.util.List;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.solvers.DantzigQPSolver;
import maspack.solvers.DantzigQPSolver.Status;
import maspack.util.UnitTest;

/**
 * Tests the solve status of {@link QPSolver} and its optional retry of a
 * failed equality-constrained solve. The programs are small: a cost
 * 1/2 x'Qx + p'x, box bounds and, where present, equality constraints
 * Aeq x = beq. The failing ones reproduce the ways {@link DantzigQPSolver}
 * fails when the equality rows are dependent or cannot be met within the
 * bounds. Failed solves and retries are reported on the console, as they
 * are in a simulation.
 */
public class QPSolverTest extends UnitTest {

   static final double EPS = 1e-10;
   static final double T1 = 0.01; // end time handed to each solve

   /**
    * Cost term with a fixed quadratic matrix and linear term.
    */
   static class QuadraticTerm extends QPCostTermBase {
      MatrixNd myQ;
      VectorNd myP;

      QuadraticTerm (MatrixNd Q, VectorNd p) {
         myQ = new MatrixNd (Q);
         myP = new VectorNd (p);
      }

      public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
         Q.add (myQ);
         p.add (myP);
      }
   }

   /**
    * Equality constraint Aeq x = beq. As the cost term the retry turns it
    * into, it contributes 1/2 |Aeq x - beq|^2.
    */
   static class EqualityTerm extends QPConstraintTermBase
      implements QPCostTerm {

      MatrixNd myAeq;
      VectorNd myBeq;

      EqualityTerm (MatrixNd Aeq, VectorNd beq) {
         myAeq = new MatrixNd (Aeq);
         myBeq = new VectorNd (beq);
      }

      public Type getType() {
         return Type.EQUALITY;
      }

      public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
         MatrixNd AtA = new MatrixNd (Q.rowSize(), Q.colSize());
         AtA.mulTransposeLeft (myAeq, myAeq);
         Q.add (AtA);
         VectorNd Atb = new VectorNd (p.size());
         myAeq.mulTranspose (Atb, myBeq);
         p.sub (Atb);
      }

      public int getTerm (
         MatrixNd A, VectorNd b, int rowoff, double t0, double t1) {
         for (int i=0; i<myAeq.rowSize(); i++) {
            for (int j=0; j<myAeq.colSize(); j++) {
               A.set (rowoff+i, j, myAeq.get (i, j));
            }
            b.set (rowoff+i, myBeq.get (i));
         }
         return rowoff + myAeq.rowSize();
      }

      public int numConstraints (int qpsize) {
         return myAeq.rowSize();
      }
   }

   /**
    * Bounds {@code lo <= x <= hi}, written as the inequalities
    * {@code x >= lo} and {@code -x >= -hi}.
    */
   static class BoxTerm extends QPConstraintTermBase {
      VectorNd myLo;
      VectorNd myHi;

      BoxTerm (VectorNd lo, VectorNd hi) {
         myLo = new VectorNd (lo);
         myHi = new VectorNd (hi);
      }

      public int getTerm (
         MatrixNd A, VectorNd b, int rowoff, double t0, double t1) {
         MatrixNd Abox = boxMatrix (myLo.size());
         VectorNd bbox = boxOffsets (myLo, myHi);
         for (int i=0; i<Abox.rowSize(); i++) {
            for (int j=0; j<Abox.colSize(); j++) {
               A.set (rowoff+i, j, Abox.get (i, j));
            }
            b.set (rowoff+i, bbox.get (i));
         }
         return rowoff + Abox.rowSize();
      }

      public int numConstraints (int qpsize) {
         return 2*myLo.size();
      }
   }

   /**
    * Matrix A of the bounds written as A x >= b: the identity above its
    * negative.
    */
   static MatrixNd boxMatrix (int n) {
      MatrixNd A = new MatrixNd (2*n, n);
      for (int i=0; i<n; i++) {
         A.set (i, i, 1);
         A.set (n+i, i, -1);
      }
      return A;
   }

   /**
    * Offsets b of the bounds written as A x >= b.
    */
   static VectorNd boxOffsets (VectorNd lo, VectorNd hi) {
      int n = lo.size();
      VectorNd b = new VectorNd (2*n);
      for (int i=0; i<n; i++) {
         b.set (i, lo.get (i));
         b.set (n+i, -hi.get (i));
      }
      return b;
   }

   /**
    * A program: cost, box bounds and, once set, equality constraints.
    */
   static class Program {
      MatrixNd Q;
      VectorNd p;
      VectorNd lo;
      VectorNd hi;
      MatrixNd Aeq;
      VectorNd beq;

      Program (double[][] Q, double[] p, double[] lo, double[] hi) {
         this.Q = new MatrixNd (Q);
         this.p = new VectorNd (p);
         this.lo = new VectorNd (lo);
         this.hi = new VectorNd (hi);
      }

      Program setEqualities (double[][] Aeq, double[] beq) {
         this.Aeq = new MatrixNd (Aeq);
         this.beq = new VectorNd (beq);
         return this;
      }

      int size() {
         return p.size();
      }

      List<QPCostTerm> costTerms() {
         List<QPCostTerm> terms = new ArrayList<>();
         terms.add (new QuadraticTerm (Q, p));
         return terms;
      }

      List<QPConstraintTerm> constraintTerms() {
         List<QPConstraintTerm> terms = new ArrayList<>();
         if (Aeq != null) {
            terms.add (new EqualityTerm (Aeq, beq));
         }
         terms.add (new BoxTerm (lo, hi));
         return terms;
      }

      /**
       * Largest excursion of x past its bounds, or zero.
       */
      double boundsViolation (VectorNd x) {
         double worst = 0;
         for (int i=0; i<size(); i++) {
            worst = Math.max (worst, lo.get (i) - x.get (i));
            worst = Math.max (worst, x.get (i) - hi.get (i));
         }
         return worst;
      }
   }

   static double[][] identity (int n) {
      double[][] I = new double[n][n];
      for (int i=0; i<n; i++) {
         I[i][i] = 1;
      }
      return I;
   }

   static double[] filled (int n, double value) {
      double[] v = new double[n];
      for (int i=0; i<n; i++) {
         v[i] = value;
      }
      return v;
   }

   static VectorNd vec (double... values) {
      return new VectorNd (values);
   }

   /**
    * Program with Q = I, p = 0 and bounds {@code -1 <= x <= 1}.
    */
   static Program unitProgram (int n) {
      return new Program (
         identity (n), filled (n, 0), filled (n, -1), filled (n, 1));
   }

   QPSolver createSolver (boolean fallbackEnabled) {
      QPSolver solver = new QPSolver();
      solver.setFallbackEnabled (fallbackEnabled);
      return solver;
   }

   VectorNd solve (QPSolver solver, Program prog) {
      return solver.solve (
         prog.costTerms(), prog.constraintTerms(), prog.size(), 0, T1);
   }

   /**
    * With the retry off, QPSolver must return exactly the status and the
    * solution that DantzigQPSolver returns for the assembled program.
    */
   void checkMatchesPlainSolver (String name, Program prog) {
      DantzigQPSolver dantzig = new DantzigQPSolver();
      VectorNd xplain = new VectorNd (prog.size());
      MatrixNd A = boxMatrix (prog.size());
      VectorNd b = boxOffsets (prog.lo, prog.hi);
      Status plainStatus;
      if (prog.Aeq == null) {
         plainStatus = dantzig.solve (xplain, prog.Q, prog.p, A, b);
      }
      else {
         plainStatus = dantzig.solve (
            xplain, prog.Q, prog.p, A, b, prog.Aeq, prog.beq);
      }
      QPSolver solver = createSolver (/*fallbackEnabled=*/false);
      VectorNd x = solve (solver, prog);
      checkEquals (name+", no retry: status", solver.getStatus(), plainStatus);
      checkEquals (name+", no retry: solution", x, xplain);
      checkEquals (name+", no retry: retries", solver.getFallbackCount(), 0);
   }

   /**
    * A feasible program: the status is SOLVED and the retry stays idle.
    */
   public void testFeasibleProgram() {
      Program prog = unitProgram (2).setEqualities (
         new double[][] {{ 1, 1 }}, new double[] { 1 });
      checkMatchesPlainSolver ("feasible", prog);
      QPSolver solver = createSolver (true);
      VectorNd x = solve (solver, prog);
      checkEquals ("feasible: status", solver.getStatus(), Status.SOLVED);
      checkEquals ("feasible: solution", x, vec (0.5, 0.5), EPS);
      checkEquals ("feasible: solves", solver.getSolveCount(), 1);
      checkEquals ("feasible: retries", solver.getFallbackCount(), 0);
   }

   /**
    * Equality rows that are linearly dependent but consistent: the pivoting
    * method reports SINGULAR_SYSTEM and leaves x at zero. The retry minimizes
    * 1/2 |x|^2 + 1/2 |Aeq x - beq|^2, whose minimum (5/11, 5/11, 0) lies
    * inside the bounds.
    */
   public void testDependentEqualities() {
      Program prog = unitProgram (3).setEqualities (
         new double[][] {{ 1, 1, 0 }, { 2, 2, 0 }}, new double[] { 1, 2 });
      checkMatchesPlainSolver ("dependent", prog);
      QPSolver solver = createSolver (false);
      VectorNd x = solve (solver, prog);
      checkEquals (
         "dependent, no retry: status", solver.getStatus(),
         Status.SINGULAR_SYSTEM);
      checkEquals ("dependent, no retry: solution", x, new VectorNd (3));

      solver = createSolver (true);
      x = solve (solver, prog);
      checkEquals (
         "dependent: status", solver.getStatus(), Status.SINGULAR_SYSTEM);
      checkEquals ("dependent: solution", x, vec (5/11.0, 5/11.0, 0), EPS);
      checkEquals ("dependent: retries", solver.getFallbackCount(), 1);
      checkEquals ("dependent: frozen triggers", solver.getFrozenTriggers(), 1);
      checkEquals (
         "dependent: violation triggers", solver.getViolationTriggers(), 0);
      checkEquals ("dependent: unrecovered", solver.getUnrecoveredCount(), 0);
      checkEquals (
         "dependent: worst residual", solver.getWorstEqualityResidual(),
         2.0, EPS);
      checkEquals (
         "dependent: retry times", solver.getFallbackTimes(),
         new double[] { T1 });
      checkEquals (
         "dependent: retry share", solver.getFallbackPercent(), 100.0, EPS);
   }

   /**
    * An equality that cannot be met within the bounds: the pivoting method
    * reports NO_SOLUTION and leaves x at zero. The retry gives the point of
    * the box closest to meeting it, the corner (1, 1); with the cost terms
    * weighted twice as strongly, (0.75, 0.75).
    */
   public void testUnattainableEquality() {
      Program prog = unitProgram (2).setEqualities (
         new double[][] {{ 1, 1 }}, new double[] { 3 });
      checkMatchesPlainSolver ("unattainable", prog);
      QPSolver solver = createSolver (true);
      VectorNd x = solve (solver, prog);
      checkEquals (
         "unattainable: status", solver.getStatus(), Status.NO_SOLUTION);
      checkEquals ("unattainable: solution", x, vec (1, 1), EPS);
      checkEquals (
         "unattainable: frozen triggers", solver.getFrozenTriggers(), 1);
      checkEquals ("unattainable: unrecovered", solver.getUnrecoveredCount(), 0);

      solver = createSolver (true);
      solver.setFallbackCostScale (2);
      x = solve (solver, prog);
      checkEquals (
         "unattainable, cost scale 2: solution", x, vec (0.75, 0.75), EPS);
   }

   /**
    * An equality that cannot be met, with a small coefficient: the pivoting
    * method reports SOLVED for x = (-1, 500), far outside the bounds. The
    * retry triggers on the violation and returns the minimum of
    * 1/2 |x|^2 + 1/2 (-x1 + 0.001 x2 - 1.5)^2, which lies inside the bounds.
    */
   public void testSolvedOutsideBounds() {
      Program prog = unitProgram (2).setEqualities (
         new double[][] {{ -1, 0.001 }}, new double[] { 1.5 });
      checkMatchesPlainSolver ("outside", prog);
      QPSolver solver = createSolver (false);
      VectorNd x = solve (solver, prog);
      checkEquals ("outside, no retry: status", solver.getStatus(), Status.SOLVED);
      check (
         "outside, no retry: violation above 400",
         prog.boundsViolation (x) > 400);

      solver = createSolver (true);
      x = solve (solver, prog);
      double c = 1 + 1 + 0.001*0.001; // 1 + |a|^2 for the row a = (-1, 0.001)
      checkEquals ("outside: solution", x, vec (-1.5/c, 0.0015/c), EPS);
      checkEquals (
         "outside: violation triggers", solver.getViolationTriggers(), 1);
      checkEquals ("outside: frozen triggers", solver.getFrozenTriggers(), 0);
      checkEquals ("outside: unrecovered", solver.getUnrecoveredCount(), 0);
      check (
         "outside: worst violation above 400",
         solver.getWorstBoundsViolation() > 400);
   }

   /**
    * An equality that cannot be met, for which the pivoting method reports
    * SOLVED but leaves x at zero while the equality still asks for 1.67. The
    * zero test catches it.
    */
   public void testSolvedAtZero() {
      Program prog = new Program (
         new double[][] {{ 1.37, 0.11 }, { 0.11, 0.12 }},
         new double[] { -0.66, 1.52 }, filled (2, -1), filled (2, 1));
      prog.setEqualities (
         new double[][] {{ -1.18, 0.0002 }}, new double[] { 1.67 });
      checkMatchesPlainSolver ("solved at zero", prog);
      QPSolver solver = createSolver (true);
      VectorNd x = solve (solver, prog);
      checkEquals (
         "solved at zero: status", solver.getStatus(), Status.SOLVED);
      checkEquals (
         "solved at zero: frozen triggers", solver.getFrozenTriggers(), 1);
      checkEquals (
         "solved at zero: unrecovered", solver.getUnrecoveredCount(), 0);
      check (
         "solved at zero: admissible",
         prog.boundsViolation (x) <= solver.getBoundsTolerance());
   }

   /**
    * Equality rows that agree only to rounding, as for several markers on one
    * rigid body, with L2 regularization as the only cost and the bounds
    * shifted by the current excitations. The program is feasible, yet the
    * pivoting method reports SOLVED for a point 0.155 outside the bounds
    * (found by a random search). Below the default threshold of 1 nothing
    * is retried; with a threshold of 0 the retry returns an admissible point.
    */
   public void testBoundsThreshold() {
      double w = 0.0003978380793764832;
      Program prog = new Program (
         new double[][] {{ w, 0, 0 }, { 0, w, 0 }, { 0, 0, w }},
         new double[] {
            3.1592328941781134e-05, 0.0003006975958648504,
            0.00038127601840571093 },
         new double[] {
            -0.07941001774212919, -0.7558290959380322, -0.9583698448455981 },
         new double[] {
            0.9205899822578708, 0.24417090406196784, 0.04163015515440194 });
      prog.setEqualities (
         new double[][] {
            { 1.2991655648651421, 1.8052431693213005, -3.9806897608177607 },
            { 0.0006741794839254479, 0.0009367996975653438,
              -0.0020657100535865958 } },
         new double[] { 2.8374814455421755, 0.001472461885027126 });
      checkMatchesPlainSolver ("threshold", prog);

      QPSolver solver = createSolver (true);
      VectorNd x = solve (solver, prog);
      double violation = prog.boundsViolation (x);
      checkEquals ("threshold 1: status", solver.getStatus(), Status.SOLVED);
      check (
         "threshold 1: violation between 0.1 and 1",
         violation > 0.1 && violation < 1);
      checkEquals ("threshold 1: retries", solver.getFallbackCount(), 0);

      solver = createSolver (true);
      solver.setBoundsThreshold (0);
      x = solve (solver, prog);
      checkEquals (
         "threshold 0: violation triggers", solver.getViolationTriggers(), 1);
      checkEquals ("threshold 0: unrecovered", solver.getUnrecoveredCount(), 0);
      check (
         "threshold 0: admissible",
         prog.boundsViolation (x) <= solver.getBoundsTolerance());
   }

   /**
    * A zero solution is legitimate when the equalities ask for nothing, and
    * then no retry is taken.
    */
   public void testLegitimateZero() {
      Program prog = unitProgram (2).setEqualities (
         new double[][] {{ 1, -1 }}, new double[] { 0 });
      QPSolver solver = createSolver (true);
      VectorNd x = solve (solver, prog);
      checkEquals ("zero: status", solver.getStatus(), Status.SOLVED);
      checkEquals ("zero: solution", x, new VectorNd (2));
      checkEquals ("zero: retries", solver.getFallbackCount(), 0);
   }

   /**
    * Without equality constraints the status is kept too: bounds that
    * contradict each other give NO_SOLUTION and a zero solution. With the
    * retry on, nothing is retried, since there is nothing to demote.
    */
   public void testStatusWithoutEqualities() {
      Program prog = new Program (
         identity (2), filled (2, 0), new double[] { 1, -1 },
         new double[] { 0, 1 });
      checkMatchesPlainSolver ("no equalities", prog);
      QPSolver solver = createSolver (true);
      VectorNd x = solve (solver, prog);
      checkEquals (
         "no equalities: status", solver.getStatus(), Status.NO_SOLUTION);
      checkEquals ("no equalities: solution", x, new VectorNd (2));
      checkEquals ("no equalities: retries", solver.getFallbackCount(), 0);
      checkEquals (
         "no equalities: unrecovered", solver.getUnrecoveredCount(), 0);
   }

   /**
    * A solution containing NaN counts as a solve that returned nothing, and
    * leaves the worst bounds violation usable for later solves. Injected
    * through a subclass, on the first solve only.
    */
   public void testNaNSolution() {
      QPSolver solver = new QPSolver() {
         boolean injected = false;

         protected Status solveProgram (VectorNd x, boolean withEqualities) {
            Status status = super.solveProgram (x, withEqualities);
            if (withEqualities && !injected) {
               x.set (0, Double.NaN);
               injected = true;
            }
            return status;
         }
      };
      solver.setFallbackEnabled (true);
      Program prog = unitProgram (2).setEqualities (
         new double[][] {{ 1, 1 }}, new double[] { 1 });
      VectorNd x = solve (solver, prog);
      checkEquals ("NaN: frozen triggers", solver.getFrozenTriggers(), 1);
      checkEquals ("NaN: solution", x, vec (1/3.0, 1/3.0), EPS);
      check (
         "NaN: worst violation stays 0", solver.getWorstBoundsViolation() == 0);

      // the program of testSolvedOutsideBounds, whose violation must register
      Program outside = unitProgram (2).setEqualities (
         new double[][] {{ -1, 0.001 }}, new double[] { 1.5 });
      solve (solver, outside);
      check (
         "NaN, then outside: worst violation above 400",
         solver.getWorstBoundsViolation() > 400);
   }

   /**
    * reset() clears the status and the counters.
    */
   public void testReset() {
      Program prog = unitProgram (2).setEqualities (
         new double[][] {{ 1, 1 }}, new double[] { 3 });
      QPSolver solver = createSolver (true);
      solve (solver, prog);
      solve (solver, prog);
      checkEquals ("reset: retries before", solver.getFallbackCount(), 2);
      solver.reset();
      checkEquals ("reset: status", solver.getStatus(), Status.SOLVED);
      checkEquals ("reset: solves", solver.getSolveCount(), 0);
      checkEquals ("reset: retries", solver.getFallbackCount(), 0);
      checkEquals ("reset: frozen triggers", solver.getFrozenTriggers(), 0);
      checkEquals ("reset: retry times", solver.getFallbackTimes().length, 0);
      checkEquals (
         "reset: worst residual", solver.getWorstEqualityResidual(), 0.0, 0);
   }

   public void test() {
      testFeasibleProgram();
      testDependentEqualities();
      testUnattainableEquality();
      testSolvedOutsideBounds();
      testSolvedAtZero();
      testBoundsThreshold();
      testLegitimateZero();
      testStatusWithoutEqualities();
      testNaNSolution();
      testReset();
   }

   public static void main (String[] args) {
      QPSolverTest tester = new QPSolverTest();
      tester.runtest();
   }
}
