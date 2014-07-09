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

class CholeskyDecompositionTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;

   CholeskyDecomposition chol = new CholeskyDecomposition();

   private void timingTests() {
      int numTrials = 10;
      int timingCnt = 1;

      int[] matsizes = new int[] { 32, 16, 32, 64, 128, 256 };

      NumberFormat ifmt = new NumberFormat ("%3d");
      NumberFormat ffmt = new NumberFormat ("%8.2f");

      System.out.println ("\ntimes are in usec\n");

      System.out.println ("matsize      factor   add row/col  del row/col");
      // XXX XXXXX.XX XXXXX.XX XXXXX.XX

      FunctionTimer timer = new FunctionTimer();

      for (int k = 0; k < matsizes.length; k++) {
         int n = matsizes[k];
         MatrixNd M = new MatrixNd (n, n);
         VectorNd col = new VectorNd (n);

         CholeskyDecomposition chol = new CholeskyDecomposition (n);

         double factorTime = 0;
         double addTime = 0;
         double delTime = 0;

         for (int cnt = 0; cnt < numTrials; cnt++) {
            M.setRandom();
            M.mulTranspose (M);
            M.getColumn (0, col);
            double x0 = col.get (0);
            for (int i = 0; i < n - 1; i++) {
               col.set (i, col.get (i + 1));
            }
            col.set (n - 1, x0);

            timer.start();
            for (int i = 0; i < timingCnt; i++) {
               chol.factor (M);
            }
            timer.stop();
            factorTime += timer.getTimeUsec() / timingCnt;

            timer.start();
            for (int i = 0; i < timingCnt; i++) {
               chol.deleteRowAndColumn (0);
            }
            timer.stop();
            delTime += timer.getTimeUsec() / timingCnt;

            timer.start();
            for (int i = 0; i < timingCnt; i++) {
               chol.addRowAndColumn (col);
            }
            timer.stop();
            addTime += timer.getTimeUsec() / timingCnt;
         }

         factorTime /= numTrials;
         addTime /= numTrials;
         delTime /= numTrials;

         if (k > 0) {
            System.out.println ("  " + ifmt.format (n) + "      "
            + ffmt.format (factorTime) + "    " + ffmt.format (addTime)
            + "    " + ffmt.format (delTime));
         }
      }
   }

   public void testDecomposition (int nrows, int ncols) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandom();
      if (nrows == ncols) {
         M1.mulTranspose (M1);
      }

      Exception eActual = null;
      Exception eExpected = null;
      if (nrows != ncols) {
         eExpected = new ImproperSizeException ("Matrix not square");
      }
      try {
         chol.factor (M1);
      }
      catch (Exception e) {
         eActual = e;
      }
      MatrixTest.checkExceptions (eActual, eExpected);
      if (eActual == null) {
         int n = nrows;
         MatrixNd L = new MatrixNd (n, n);
         MatrixNd LLT = new MatrixNd (n, n);

         chol.get (L);

         LLT.mulTransposeRight (L, L);
         if (!LLT.epsilonEquals (M1, EPSILON)) {
            throw new TestException ("LLT=\n" + LLT.toString ("%9.4f")
            + "expected:\n" + M1.toString ("%9.4f"));
         }

         double condEst = chol.conditionEstimate (M1);

         // check vector solver
         VectorNd b = new VectorNd (n);
         for (int i = 0; i < n; i++) {
            b.set (i, RandomGenerator.get().nextDouble() - 0.5);
         }
         VectorNd x = new VectorNd (n);
         VectorNd Mx = new VectorNd (n);
         chol.solve (x, b);
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

         chol.solve (X, B);
         MX.mul (M1, X);
         if (!MX.epsilonEquals (B, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "MX="
            + MX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }

         // check L solve
         VectorNd Lx = new VectorNd (n);
         chol.solveL (x, b);
         Lx.mul (L, x);
         if (!Lx.epsilonEquals (b, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "Lx="
            + Lx.toString ("%9.4f") + "b=" + b.toString ("%9.4f"));
         }

         // check L matrix solve
         MatrixNd LX = new MatrixNd (n, 3);
         chol.solveL (X, B);
         LX.mul (L, X);
         if (!LX.epsilonEquals (B, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "LX="
            + LX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }

         // check left L solve
         VectorNd xL = new VectorNd (n);
         chol.leftSolveL (x, b);
         xL.mulTranspose (L, x);
         if (!xL.epsilonEquals (b, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "xL="
            + xL.toString ("%9.4f") + "b=" + b.toString ("%9.4f"));
         }

         // check L matrix solve
         MatrixNd XL = new MatrixNd (3, n);
         MatrixNd BT = new MatrixNd (3, n);
         BT.transpose (B);
         chol.leftSolveL (X, BT);
         XL.mul (X, L);
         if (!XL.epsilonEquals (BT, EPSILON * condEst)) {
            throw new TestException ("solution failed:\n" + "XL="
            + XL.toString ("%9.4f") + "BT=" + BT.toString ("%9.4f"));
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
            if (Math.abs (det - chol.determinant()) > Math.abs (det * condEst
            * EPSILON)) {
               throw new TestException ("determinant failed: got "
               + chol.determinant() + " expected " + det + "\nM=\n"
               + M1.toString ("%9.4f"));
            }
         }

         // check inverse
         MatrixNd MI = new MatrixNd (n, n);
         MatrixNd IMI = new MatrixNd (n, n);
         chol.inverse (MI);
         IMI.mul (M1, MI);
         MatrixNd I = new MatrixNd (n, n);
         I.setIdentity();

         if (!IMI.epsilonEquals (I, EPSILON * condEst)) {
            throw new TestException ("failed inverse:\n"
            + MI.toString ("%9.4f") + "M1=\n" + M1.toString ("%9.4f"));
         }

         // check column additional

         CholeskyDecomposition inc = new CholeskyDecomposition();
         MatrixNd Msub = new MatrixNd (0, 0);
         for (int j = 0; j < n; j++) {
            Msub.setSize (j + 1, j + 1);
            M1.getSubMatrix (0, 0, Msub);
            VectorNd col = new VectorNd (j + 1);
            Msub.getColumn (j, col);
            inc.addRowAndColumn (col);
            checkFactorization (inc, Msub);
         }
         if (n > 3) {
            inc.deleteRowAndColumn (0);
            Msub.setSize (n - 1, n - 1);
            M1.getSubMatrix (1, 1, Msub);
            checkFactorization (inc, Msub);

            Msub.setSize (n - 2, n - 2);
            M1.getSubMatrix (1, 1, Msub);
            inc.deleteRowAndColumn (n - 2);
            checkFactorization (inc, Msub);

            MatrixNd Msubsub = new MatrixNd (n - 3, n - 3);
            int delIdx = n / 2 - 1;
            int[] colIdxs = new int[n - 3];
            for (int i = 0; i < n - 3; i++) {
               colIdxs[i] = (i < delIdx ? i + 1 : i + 2);
            }
            M1.getSubMatrix (colIdxs, colIdxs, Msubsub);
            inc.deleteRowAndColumn (delIdx);
            checkFactorization (inc, Msubsub);

         }
      }
   }

   private void checkFactorization (CholeskyDecomposition chol, MatrixNd M) {
      MatrixNd L = new MatrixNd (0, 0);
      MatrixNd LLT = new MatrixNd (0, 0);
      chol.get (L);
      LLT.mulTransposeRight (L, L);
      if (!LLT.epsilonEquals (M, EPSILON)) {
         throw new TestException ("LLT=\n" + LLT.toString ("%9.4f")
         + "expected:\n" + M.toString ("%9.4f"));
      }
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      testDecomposition (4, 3);
      testDecomposition (3, 4);
      for (int i = 0; i < 10; i++) {
         testDecomposition (7, 7);
         testDecomposition (6, 6);
         testDecomposition (5, 5);
         testDecomposition (4, 4);
         testDecomposition (3, 3);
         testDecomposition (2, 2);
         testDecomposition (1, 1);
      }
   }

   public static void main (String[] args) {
      CholeskyDecompositionTest tester = new CholeskyDecompositionTest();

      boolean doTiming = false;

      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.err.println (
               "Usage: java maspack.matrix.CholeskyDecompositionTest [-timing]");
            System.exit (1);
         }
      }

      if (doTiming) {
         tester.timingTests();
      }
      else {
         tester.execute();
         System.out.println ("\nPassed\n");
      }

   }
}
