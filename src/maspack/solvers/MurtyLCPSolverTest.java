/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;
import java.util.Random;

public class MurtyLCPSolverTest extends LCPSolverTestBase {
   private static double EPS = 1000 * DOUBLE_PREC;

   private MurtyLCPSolver mySolver;

   public MurtyLCPSolverTest() {
      super();
      mySolver = new MurtyLCPSolver();
      mySolver.setBlockPivoting (false);
   }

   double[] Mvals0 = new double[] {
0.7558516006032541, 0.08621849127977277, 0.19813271591265702, -0.21965711535420918, 0.08998123977300654, 
0.08621849127977277, 0.3445419104396526, -0.013821781051714513, 0.010749064232795397, 0.05695948354406295, 
0.19813271591265702, -0.013821781051714513, 0.28254812504018184, -0.08105747870929013, -0.22859087587792376, 
-0.21965711535420918, 0.010749064232795397, -0.08105747870929013, 0.40268153788839833, -0.2792229690793988, 
0.08998123977300654, 0.05695948354406295, -0.22859087587792376, -0.2792229690793988, 0.5209119724528151 
   };

      double[] qvals0 = new double[] {
-0.18801573881726505, -0.07257468808959365, -0.20999786601717657, 0.3197081053534277, -0.06313348837924496
      };

   double[] lo0 = new double[] {0, 0, 0, 0, 0};
   double[] hi0 = new double[] {INF, INF, INF, INF, INF};
      

   public void execute() {

      // mySolver.setDebug (MurtyLCPSolver.SHOW_PIVOTS);
      // testSpecial (Mvals0, qvals0);
      // //mySolver.setBlockPivoting (true);
      // testSpecial (Mvals0, qvals0, lo0, hi0, 0);
      // mySolver.setDebug (0);

      simpleContactTests (/*regularize=*/true);
      mySolver.setBlockPivoting (true);
      simpleContactTests (/*regularize=*/true);

      int npegTests = 100;
      int nz = 5; // number of contact rings along z for pegInHole
      int nr = 7; // number of contacts about each ring
      clearPivotCount();
      mySolver.setBlockPivoting (false);
      pegInHoleContactTests (nz, nr, npegTests, /*regularize=*/true);
      printAndClearPivotCount("peg-in-hole (single pivots): ", npegTests);

      mySolver.setBlockPivoting (true);
      pegInHoleContactTests (nz, nr, npegTests, /*regularize=*/true);
      printAndClearPivotCount("peg-in-hole (block pivots):  ", npegTests);

      int ntests= 5000;
      int size = 50;

      mySolver.setBlockPivoting (false);
      randomTests (ntests, size, /*semiDefinite=*/false);
      printAndClearPivotCount("LCP,  single pivot, definite:    ", ntests);
      
      mySolver.setBlockPivoting (true);
      randomTests (ntests, size, /*semiDefinite=*/false);
      printAndClearPivotCount("LCP,  block pivot,  definite:    ", ntests);

      mySolver.setBlockPivoting (false);
      randomBLCPTests (ntests, size, /*semiDefinite=*/false);
      printAndClearPivotCount("BLCP, single pivot, definite:    ", ntests);

      mySolver.setBlockPivoting (true);
      randomBLCPTests (ntests, size, /*semiDefinite=*/false);
      printAndClearPivotCount("BLCP, block pivot,  definite:    ", ntests);
   }

   public void test() {
      execute();
   }

   public LCPSolver getSolver() {
      return mySolver;
   }

   protected void runtiming() {
      randomTiming (10000, 50);
      mySolver.setBlockPivoting (true);
      System.out.println ("With block pivoting:");
      randomTiming (10000, 50);
   }

   public static void main (String[] args) {
      MurtyLCPSolverTest tester = new MurtyLCPSolverTest();

      tester.parseArgs (args);
      if (tester.isTimingRequested()) {
         tester.runtiming();
      }
      else {
         tester.runtest();
      }
   }
}
