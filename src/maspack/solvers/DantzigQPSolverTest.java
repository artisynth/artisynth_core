/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.solvers.DantzigQPSolver.Status;
import maspack.matrix.*;
import maspack.util.*;

/**
 * Testing program for the QPSolver
 */
public class DantzigQPSolverTest extends UnitTest {

   DantzigQPSolver mySolver = new DantzigQPSolver();

   public void checkSolution (
      MatrixNd H, VectorNd f, MatrixNd A, VectorNd b,
      MatrixNd Aeq, VectorNd beq, VectorNd xcheck) {

      VectorNd x = new VectorNd (xcheck.size());
      Status status;
      if (Aeq != null && Aeq.rowSize() > 0) {
         status = mySolver.solve (x, H, f, A, b, Aeq, beq);
      }
      else {
         status = mySolver.solve (x, H, f, A, b);
      }
      if (status != Status.SOLVED) {
         throw new TestException (
            "Unexpected solution status: " + status);
      }
      if (status == Status.SOLVED) {
         if (!x.epsilonEquals (xcheck, 1e-10)) {
            System.out.println ("H.size=" + H.rowSize());
            throw new TestException (
               "Solution:\n" + x + "\nExpected:\n" + xcheck);
         }
      }
   }

   private double clipsol (double x, double w, boolean eq) {
      if (eq) {
         return 1;
      }
      else if (x > w) {
         return w;
      }
      else if (x < -w) {
         return -w;
      }
      else {
         return x;
      }
   }

   /**
    * Basic sanity test: exhaustive testing on a very simple problem H = identity
    * and unit constraints.
    */
   public void simpleCubeTest() {
      MatrixNd H = new MatrixNd(3, 3);
      H.setIdentity();
      MatrixNd A = new MatrixNd(6, 3);
      A.set (0, 0, -1);
      A.set (1, 1, -1);
      A.set (2, 2, -1);
      A.set (3, 0,  1);
      A.set (4, 1,  1);
      A.set (5, 2,  1);
      VectorNd b = new VectorNd (6);
      double w = 1.0001;
      b.set (new double[] {-w, -w, -w, -w, -w, -w});

      for (int i=-1; i<2; i++) {
         for (int j=-1; j<2; j++) {
            for (int k=-1; k<2; k++) {
               VectorNd f = new VectorNd(3);
               f.set (0, -1.5*i);
               f.set (1, -1.5*j);
               f.set (2, -1.5*k);
               // try 8 equality cases
               for (int n=0; n<8; n++) {
                  boolean[] eq = new boolean[3];
                  eq[0] = ((n & 0x1) == 0x1);
                  eq[1] = ((n & 0x2) == 0x2);
                  eq[2] = ((n & 0x4) == 0x4);
                  int neq = 0;
                  for (int l=0; l<3; l++) {
                     if (eq[l]) {
                        neq++;
                     }
                  }
                  MatrixNd Aeq = new MatrixNd ();
                  VectorNd beq = new VectorNd ();
                  if (neq > 0) {
                     Aeq.setSize (neq, 3);
                     beq.setSize (neq);
                     int m = 0;
                     for (int l=0; l<3; l++) {
                        if (eq[l]) {
                           Aeq.set (m, l, 1);
                           beq.set (m, 1);
                           m++;
                        }
                     }
                  }
                  VectorNd xcheck = new VectorNd(3);
                  xcheck.set (0, clipsol (-f.get(0), w, eq[0]));
                  xcheck.set (1, clipsol (-f.get(1), w, eq[1]));
                  xcheck.set (2, clipsol (-f.get(2), w, eq[2]));
                  checkSolution (H, f, A, b, Aeq, beq, xcheck);
               }
            }
         }
      }
   }

   private MatrixNd myH1 = new MatrixNd (3, 3, new double[] {
         1.500359369953654, 1.329321196633154, 0.843850710771907,
         1.329321196633154, 1.243647801471904, 0.693594738867917,
         0.843850710771907, 0.693594738867917, 1.293459993863473
      });      
   private VectorNd myb1 = new VectorNd (new double[] {-0.5, -1, -2, -0.5, -1});
   private MatrixNd myA1 = new MatrixNd (5, 3, new double[] {
         -0.325044337540851,   0.597973606926693,   0.732648445061961,
         -0.557792927156398,  -0.445918337620505,  -0.700017061640688,
         -0.432340846184460,   0.832500014966999,  -0.346446414038956,
          0.349973206566297,   0.724402145389556,   0.593936264628380,
         -0.236324157330205,   0.689812960279756,  -0.684331040134999,
      });

   //
   // Test data that was validated by matlab quadprog. Each row in this table gives:
   //
   // number of inequality constraints
   // number of equality constraints
   // f value (3 numbers)
   // expected solution (3 numbers)

   private double[] tests1 = new double[] {
      5, 0,   0, 0, 0,              0, 0, 0,
      5, 0,  1, 1, 1,  -0.100766115615096,  -0.299341612069609,  -0.417369452212965,
      5, 0,  2, 0, -1, -31.128734368217518, 30.695757820693039,   4.621394183363338,

      0, 0, -1, 1, 1,  31.214945241765911, -31.929630474865252,  -4.015995888741307,
      0, 0,  0, 0, 0,    0, 0, 0,

      1, 0, -1, 1, 1,  0.765947483326566,  -0.820627713777774,   0.327142721555080,
      1, 0,  2, 0,-1,  -31.128734368217152,  30.695757820692666,   4.621394183363297,
      1, 0,  1, 1, 1,  -0.116274555427033,  -0.385507482594294,  -0.419397718030386,

      2, 0,  1, 1, 1,  -0.116274555427033,  -0.385507482594294,  -0.419397718030386,
      2, 0,  3, -1, 0, -59.013468116675284,  60.505973391494116,   6.055024691993587,

      5, 1,  0, 0, 0,   -0.725077370179283,   0.750239204175996,   0.070204230264415,
      5, 1,  1, 1, 1,  -0.890856648117006,   0.440282584378572,  -0.429961019671936,
      5, 1,  2, 0, -1, -1.289816335194294,  -0.176010274532455,   1.729280889812024,
      5, 1,  -1, 0, 1,  -0.315302277670019,   1.414441827173497,  -1.338159457240978,
      5, 1,  1, 0, 0,   -0.881026954081263,  0.486584001532966,   0.324029738871701,
      
      5, 2,  1, 0, 0,   -0.648324461702401,   0.876514438395238,  -0.145163882447850,
      5, 2,  0, 0, 0,    -0.643521773783014,   0.886898717386475,  -0.094234771745678,
      5, 2,  -1, 1, 1,   -0.700032475557037,  0.764712370269624,  -0.693490820611411,

      2, 1,  3, -1, 0,  -0.929270919242190,  0.406122433755771,  0.431133335731803,
      2, 1,  0, 0, 0,  -0.725077370179284,  0.750239204175996,   0.070204230264415,
      2, 1,  -1, -1, -1,  -0.559298092241561,  1.060195823973420,   0.570369480200766,

      1, 1,  0, 0, 0,  -0.725077370179284,  0.750239204175996,  0.070204230264415,
      1, 1,  -1, -1, -1, -0.559298092241561,   1.060195823973420,   0.570369480200766,
      1, 1,  1, 0, -2, -1.388677971295834,  -0.314510839175974,   2.633106096667915,

      0, 1,  1, 0, -2,  -1.388677971295834, -0.314510839175974,  2.633106096667915,
      0, 1,  0, 0, 0,   -0.725077370179284,  0.750239204175996,   0.070204230264415,
      0, 2,  0, 0, 0,   -0.643521773783015,   0.886898717386475,  -0.094234771745678,
      0, 2,  -1, -1, -1, -0.577405696170219,   1.029853622485801,   0.606879498524399,
      0, 2,  2, .1, 2,  -0.756023798925255, 0.643649013103074,  -1.287239228595712,
   };

   private MatrixNd myAeq1 = new MatrixNd (3, 3, new double[] {
         -0.868497615324686,   0.495325434237954,  -0.019094668688283,
         -0.199946975839055,   0.963573756951330,  -0.177614249899934,
         -0.324181609011007,  -0.857048467873305,   0.400467486938771
      });
   private VectorNd mybeq1 = new VectorNd (new double[] { 1, 1, 1 });

   public void matlabTests() {
      for (int k=0; k<tests1.length-6; ) {
         int n = (int)tests1[k++];
         int neq = (int)tests1[k++];
         VectorNd f = new VectorNd(3);
         VectorNd xcheck = new VectorNd(3);
         f.set (0, tests1[k++]);
         f.set (1, tests1[k++]);
         f.set (2, tests1[k++]);
         xcheck.set (0, tests1[k++]);
         xcheck.set (1, tests1[k++]);
         xcheck.set (2, tests1[k++]);

         MatrixNd A = new MatrixNd (n, 3);
         VectorNd b = new VectorNd (n);
         for (int i=0; i<n; i++) {
            for (int j=0; j<3; j++) {
               A.set (i, j, myA1.get(i,j));
            }
            b.set (i, myb1.get(i));
         }

         MatrixNd Aeq = new MatrixNd (neq, 3);
         VectorNd beq = new VectorNd (neq);
         for (int i=0; i<neq; i++) {
            for (int j=0; j<3; j++) {
               Aeq.set (i, j, myAeq1.get(i,j));
            }
            beq.set (i, mybeq1.get(i));
         }
         checkSolution (myH1, f, A, b, Aeq, beq, xcheck);
         if (A.rowSize() == 0) {
            checkSolution (myH1, f, null, null, Aeq, beq, xcheck);
         }
      }
   }

   /**
    * Checks that a warm started solve produced the same result as a cold
    * started one. Warm starting changes only the pivot sequence, and so must
    * not change the solution for problems with a unique minimum.
    */
   private void checkWarmAgainstCold (
      Status coldStatus, VectorNd xcold,
      Status warmStatus, VectorNd xwarm, String msg) {

      if (coldStatus != Status.SOLVED || warmStatus != Status.SOLVED) {
         throw new TestException (
            "Unexpected status ("+msg+"): cold=" + coldStatus +
            ", warm=" + warmStatus);
      }
      if (!xwarm.epsilonEquals (xcold, 1e-10)) {
         throw new TestException (
            "Warm started solution ("+msg+"):\n" + xwarm +
            "\nDiffers from cold started solution:\n" + xcold);
      }
   }

   private MatrixNd createSPD (int size) {
      MatrixNd B = new MatrixNd (size, size);
      B.setRandom();
      MatrixNd H = new MatrixNd (size, size);
      H.mulTransposeLeft (B, B);
      // add to the diagonal to keep H well conditioned
      for (int i=0; i<size; i++) {
         H.add (i, i, 1.0);
      }
      return H;
   }

   /**
    * Tests warm starting, as controlled by {@link
    * DantzigQPSolver#setWarmStartEnabled}. Solutions are compared against
    * those produced by a second, cold started solver.
    */
   public void warmStartTest() {
      int size = 10;    // problem size
      int nineq = 6;    // number of inequality constraints
      int neq = 2;      // number of equality constraints

      DantzigQPSolver cold = new DantzigQPSolver();
      DantzigQPSolver warm = new DantzigQPSolver();

      check ("warm starts should be disabled by default",
             !cold.getWarmStartEnabled());
      warm.setWarmStartEnabled (true);
      check ("getWarmStartEnabled() should return true",
             warm.getWarmStartEnabled());

      MatrixNd H = createSPD (size);
      VectorNd f = new VectorNd (size);
      MatrixNd A = new MatrixNd (nineq, size);
      VectorNd b = new VectorNd (nineq);
      MatrixNd Aeq = new MatrixNd (neq, size);
      VectorNd beq = new VectorNd (neq);
      f.setRandom();
      A.setRandom();
      b.setRandom();
      Aeq.setRandom();
      beq.setRandom();

      VectorNd xcold = new VectorNd (size);
      VectorNd xwarm = new VectorNd (size);
      VectorNd df = new VectorNd (size);

      // A sequence of slowly varying problems, as arises when a QP is solved
      // at each step of a simulation. This is the case warm starting is
      // intended for, so also check that it reduces the pivot count.
      int coldPivots = 0;
      int warmPivots = 0;
      for (int k=0; k<100; k++) {
         df.setRandom (-0.02, 0.02);
         f.add (df);

         Status cstat = cold.solve (xcold, H, f, A, b);
         coldPivots += cold.myLcp.getPivotCount();
         Status wstat = warm.solve (xwarm, H, f, A, b);
         warmPivots += warm.myLcp.getPivotCount();
         checkWarmAgainstCold (cstat, xcold, wstat, xwarm, "inequalities only");

         cstat = cold.solve (xcold, H, f, A, b, Aeq, beq);
         wstat = warm.solve (xwarm, H, f, A, b, Aeq, beq);
         checkWarmAgainstCold (cstat, xcold, wstat, xwarm, "with equalities");
      }
      check ("warm starting should reduce the pivot count, but cold=" +
             coldPivots + " and warm=" + warmPivots, warmPivots < coldPivots);

      // Changing the number of inequality constraints invalidates the retained
      // state, which must then be discarded instead of being used to seed the
      // solve. The retained state is checked directly, since a stale state
      // still yields the right answer: the LCP solver falls back to a cold
      // solve when a warm start fails.
      for (int nc : new int[] { 6, 3, 9, 0, 5, 6 }) {
         MatrixNd Anc = new MatrixNd (nc, size);
         VectorNd bnc = new VectorNd (nc);
         Anc.setRandom();
         bnc.setRandom();

         Status cstat = cold.solve (xcold, H, f, Anc, bnc);
         Status wstat = warm.solve (xwarm, H, f, Anc, bnc);
         checkWarmAgainstCold (cstat, xcold, wstat, xwarm, "nineq=" + nc);
         if (nc > 0) {
            // nc == 0 is solved without an LCP, and so leaves the state alone
            checkEquals (
               "retained state size for nineq=" + nc,
               warm.myLcpState.size(), nc);
         }

         cstat = cold.solve (xcold, H, f, Anc, bnc, Aeq, beq);
         wstat = warm.solve (xwarm, H, f, Anc, bnc, Aeq, beq);
         checkWarmAgainstCold (
            cstat, xcold, wstat, xwarm, "nineq=" + nc + " with equalities");
         if (nc > 0) {
            checkEquals (
               "retained state size for nineq=" + nc + " with equalities",
               warm.myLcpState.size(), nc);
         }
      }

      // disabling warm starts should restore cold start behavior
      warm.setWarmStartEnabled (false);
      check ("getWarmStartEnabled() should return false",
             !warm.getWarmStartEnabled());
      checkEquals ("retained state size after disabling warm starts",
                   warm.myLcpState.size(), 0);
      Status cstat = cold.solve (xcold, H, f, A, b);
      Status wstat = warm.solve (xwarm, H, f, A, b);
      checkWarmAgainstCold (cstat, xcold, wstat, xwarm, "warm starts disabled");
   }

   public void test() {
      // Run the tests both cold and warm started. Each problem here has a
      // unique minimum, so enabling warm starts must not change any solution,
      // even though the state is being carried over between unrelated problems.
      for (boolean warmStart : new boolean[] { false, true }) {
         mySolver.setWarmStartEnabled (warmStart);
         simpleCubeTest();
         matlabTests();
      }
      mySolver.setWarmStartEnabled (false);
      warmStartTest();
   }

   public static void main (String[] args) {
      DantzigQPSolverTest tester = new DantzigQPSolverTest();
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
