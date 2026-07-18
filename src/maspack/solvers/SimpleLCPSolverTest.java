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

public class KellerLCPSolverTest extends LCPSolverTestBase {

   private KellerLCPSolver mySolver;

   public KellerLCPSolverTest() {
      super();
      mySolver = new KellerLCPSolver();
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
      KellerLCPSolverTest tester = new KellerLCPSolverTest();

      tester.parseArgs (args);
      if (tester.isTimingRequested()) {
         tester.runtiming();
      }
      else {
         tester.runtest();
      }

   }
}
