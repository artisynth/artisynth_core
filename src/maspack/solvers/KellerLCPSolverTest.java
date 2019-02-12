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

public class KellerLCPSolverTest extends UnitTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPS = 1000 * DOUBLE_PREC;

   private KellerLCPSolver mySolver;
   private Random myRandom;

   private FunctionTimer timer = new FunctionTimer();
   private int pivotCnt;

   public KellerLCPSolverTest() {
      mySolver = new KellerLCPSolver();
      maspack.util.RandomGenerator.setSeed (0x1234);
   }

   public void testSolver (
      VectorNd z, MatrixNd M, VectorNd q, boolean[] zBasic,
      KellerLCPSolver.Status expectedStatus) {
      KellerLCPSolver.Status status;
      timer.restart();
      status = mySolver.solve (z, M, q, zBasic);
      timer.stop();
      pivotCnt += mySolver.getIterationCount();
      if (expectedStatus == KellerLCPSolver.Status.SOLVED &&
          status != expectedStatus) { // perturb the problem
         int cnt = 10;
         int k = 0;
         double mag = q.infinityNorm();
         do {
            VectorNd fuzz = new VectorNd (q.size());
            fuzz.setRandom (-mag * 1e-13, mag * 1e-13);
            q.add (fuzz);
            status = mySolver.solve (z, M, q, zBasic);
            mag *= 10;
            k++;
         }
         while (k < cnt && status != KellerLCPSolver.Status.SOLVED);
         if (!mySilentP) {
            System.out.println ("random retry level " + k);
         }
      }
      if (status != expectedStatus) {
         System.out.println ("M=\n" + M.toString ("%10.6f"));
         System.out.println ("q=\n" + q.toString ("%10.6f"));
         throw new TestException ("solver returned " + status + ", expected "
         + expectedStatus);
      }
      if (status == KellerLCPSolver.Status.SOLVED) { // check the solution
         TestException failException = null;
         int n = z.size();
         VectorNd w = new VectorNd (n);
         w.mul (M, z);
         w.add (q);
         double mag = 0;
         for (int i = 0; i < n; i++) {
            mag = Math.max (mag, Math.abs (z.get(i)));
            mag = Math.max (mag, Math.abs (w.get(i)));
         }
         double tol = DOUBLE_PREC * mag * 10000;
         for (int i = 0; i < n; i++) {
            if (z.get(i) < -tol || w.get(i) < -tol) {
               failException =
                  new TestException ("negative values for z and/or w");
            }
            if (Math.abs (z.get(i) * w.get(i)) > tol) {
               failException =
                  new TestException ("w and z are not complementary");
            }
            if (z.get(i) > tol && !zBasic[i]) {
               failException =
                  new TestException ("non-zero value for non-basic z");
            }
         }
         if (failException != null) {
            System.out.println ("M=\n" + M.toString ("%12.8f"));
            System.out.println ("q=\n" + q.toString ("%12.8f"));
            System.out.println ("z=\n" + z.toString ("%12.8f"));
            System.out.println ("w=\n" + w.toString ("%12.8f"));
            System.out.print ("zBasic=");
            for (int i = 0; i < n; i++) {
               System.out.print (zBasic[i] + " ");
            }
            System.out.println ("");
            throw failException;
         }
      }
   }

   public void execute() {
      int numRandomTests = 1000;
      int size = 100;

      MatrixNd M = new MatrixNd (size, size);
      VectorNd q = new VectorNd (size);
      VectorNd z = new VectorNd (size);
      VectorNd x = new VectorNd (size); // seed for cone projection
      boolean[] zBasic = new boolean[size];

      for (int i = 0; i < numRandomTests; i++) {
         M.setRandom();
         x.setRandom();
         q.mul (M, x);
         M.mulTransposeRight (M, M);
         testSolver (z, M, q, zBasic, KellerLCPSolver.Status.SOLVED);
         // System.out.println (
         // "SPD "+i+", pivots=" + mySolver.getIterationCount());
      }

      MatrixNd N = new MatrixNd (size, size - size / 2);
      x.setSize (size - size / 2);

      for (int i = 0; i < numRandomTests; i++) {
         N.setRandom();
         x.setRandom();
         q.mul (N, x);
         M.mulTransposeRight (N, N);
         testSolver (z, M, q, zBasic, KellerLCPSolver.Status.SOLVED);
         // System.out.println (
         // "SPSD "+i+", pivots=" + mySolver.getIterationCount());
      }
      if (!mySilentP) {
         System.out.println (
            "average time: " + timer.result (2 * numRandomTests)
            + ", " + pivotCnt / (2 * numRandomTests) + " pivots");
      }
   }

   public void test() {
      execute();
   }

   private void printUsageAndExit (int code) {
      System.out.println ("Usage: java "+getClass()+" [-verbose]");
      System.exit (code); 
   }   

   public static void main (String[] args) {
      KellerLCPSolverTest tester = new KellerLCPSolverTest();

      tester.setSilent (true);
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.setSilent (false);
         }
         else if (args[i].equals ("-help")) {
            tester.printUsageAndExit (0);
         }
         else {
            tester.printUsageAndExit (1);
         }
      }
      tester.runtest();
   }
}
