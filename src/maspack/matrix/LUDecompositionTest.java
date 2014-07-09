/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.TestException;

class LUDecompositionTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;

   LUDecomposition lu = new LUDecomposition();

   private void timingTests() {
      // Random rand = RandomGenerator.get();
      int baseTimingCnt = 100000;
      int numTrials = 10;

      int[] matsizes = new int[] { 4, 8, 16, 32, 4, 8, 16, 32, 64 };

      NumberFormat ifmt = new NumberFormat ("%3d");
      NumberFormat ffmt = new NumberFormat ("%7.2f");

      System.out.println ("matsize    time");
      // XXX XXXXX.XX

      for (int k = 0; k < matsizes.length; k++) {
         int n = matsizes[k];
         int timingCnt = baseTimingCnt / (n * n);
         MatrixNd M = new MatrixNd (n, n);
         LUDecomposition lu = new LUDecomposition (n);

         FunctionTimer timer = new FunctionTimer();

         double ludTime = 0;

         for (int cnt = 0; cnt < numTrials; cnt++) {
            M.setRandom();

            timer.start();
            for (int i = 0; i < timingCnt; i++) {
               lu.factor (M);
            }
            timer.stop();
            ludTime += timer.getTimeUsec() / timingCnt;
         }

         ludTime /= numTrials;

         System.out.println ("  " + ifmt.format (n) + "    "
         + ffmt.format (ludTime));
      }
   }

   public void testDecomposition (int nrows, int ncols) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandom();

      Exception eActual = null;
      Exception eExpected = null;
      if (nrows != ncols) {
         eExpected = new ImproperSizeException ("Matrix not square");
      }
      try {
         lu.factor (M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      MatrixTest.checkExceptions (eActual, eExpected);
      if (eActual == null) {
         int n = nrows;
         MatrixNd L = new MatrixNd (n, n);
         MatrixNd U = new MatrixNd (n, n);
         MatrixNd PM = new MatrixNd (n, n);
         MatrixNd LU = new MatrixNd (n, n);
         int[] perm = new int[n];
         double[] row = new double[n];

         lu.get (L, U, perm);

         for (int i = 0; i < n; i++) {
            M1.getRow (perm[i], row);
            PM.setRow (i, row);
         }
         LU.mul (L, U);
         if (!LU.epsilonEquals (PM, EPSILON)) {
            throw new TestException ("LU=\n" + LU.toString ("%9.4f")
            + "expected:\n" + PM.toString ("%9.4f"));
         }

         double condEst = lu.conditionEstimate (M1);

         // check vector solver
         VectorNd b = new VectorNd (n);
         for (int i = 0; i < n; i++) {
            b.set (i, RandomGenerator.get().nextDouble() - 0.5);
         }
         VectorNd x = new VectorNd (n);
         VectorNd Mx = new VectorNd (n);
         lu.solve (x, b);
         Mx.mul (M1, x);
         if (!Mx.epsilonEquals (b, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "Mx="
            + Mx.toString ("%9.4f") + "b=" + b.toString ("%9.4f"));
         }

         // check matrix solver
         MatrixNd B = new MatrixNd (n, 3);
         B.setRandom();
         MatrixNd X = new MatrixNd (n, 3);
         MatrixNd MX = new MatrixNd (n, 3);

         lu.solve (X, B);
         MX.mul (M1, X);
         if (!MX.epsilonEquals (B, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "MX="
            + MX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }

         // check determinant
         if (n <= 3) {
            double det;

            if (n == 1) {
               det = M1.get (0, 0);
            }
            else if (n == 2) {
               det =
                  M1.get (0, 0) * M1.get (1, 1) - M1.get (0, 1) * M1.get (1, 0);
            }
            else // n == 3
            {
               det =
                  M1.get (0, 0) * M1.get (1, 1) * M1.get (2, 2) + M1.get (0, 1)
                  * M1.get (1, 2) * M1.get (2, 0) + M1.get (0, 2)
                  * M1.get (1, 0) * M1.get (2, 1) - M1.get (0, 2)
                  * M1.get (1, 1) * M1.get (2, 0) - M1.get (0, 0)
                  * M1.get (1, 2) * M1.get (2, 1) - M1.get (0, 1)
                  * M1.get (1, 0) * M1.get (2, 2);
            }
            if (Math.abs (det - lu.determinant()) > Math.abs (det * condEst
            * EPSILON)) {
               throw new TestException ("determinant failed: got "
               + lu.determinant() + " expected " + det + "\nM=\n"
               + M1.toString ("%9.4f"));
            }
         }

         // check inverse
         MatrixNd MI = new MatrixNd (n, n);
         MatrixNd IMI = new MatrixNd (n, n);
         lu.inverse (MI);
         IMI.mul (M1, MI);
         MatrixNd I = new MatrixNd (n, n);
         I.setIdentity();

         if (!IMI.epsilonEquals (I, EPSILON * condEst)) {
            throw new TestException ("failed inverse:\n"
            + MI.toString ("%9.4f") + "M1=\n" + M1.toString ("%9.4f"));
         }

      }
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      testDecomposition (4, 3);
      testDecomposition (3, 4);
      for (int i = 0; i < 10; i++) {
         testDecomposition (4, 4);
         testDecomposition (3, 3);
         testDecomposition (2, 2);
         testDecomposition (1, 1);
      }
   }

   public static void main (String[] args) {
      LUDecompositionTest tester = new LUDecompositionTest();
      boolean doTiming = false;

      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.err.println (
               "Usage: java maspack.matrix.LUDecompositionTest [-timing]");
            System.exit (1);
         }
      }

      if (doTiming) {
         tester.timingTests();
      }
      else {
         tester.execute();
      }

      System.out.println ("\nPassed\n");
   }
}
