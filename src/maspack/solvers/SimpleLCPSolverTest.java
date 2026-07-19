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

public class SimpleLCPSolverTest extends LCPSolverTestBase {

   private SimpleLCPSolver mySolver;

   public SimpleLCPSolverTest() {
      super();
      mySolver = new SimpleLCPSolver();
   }

   public void execute() {
      simpleContactTests(/*regularize=*/false);
      randomTests (1000, 100, /*semiDefinite=*/false);
      randomTests (1000, 100, /*semiDefinite=*/true);
   }

   public void test() {
      execute();
   }

   public LCPSolver getSolver() {
      return mySolver;
   }

   public static void main (String[] args) {
      SimpleLCPSolverTest tester = new SimpleLCPSolverTest();

      tester.parseArgs (args);
      if (tester.isTimingRequested()) {
         tester.runtiming();
      }
      else {
         tester.runtest();
      }

   }
}
