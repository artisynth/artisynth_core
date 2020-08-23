/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.TestException;

class QRDecompositionTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 100 * DOUBLE_PREC;

   QRDecomposition qr = new QRDecomposition();

   private void timingTests() {
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

         QRDecomposition qr = new QRDecomposition();

         FunctionTimer timer = new FunctionTimer();

         double qrdTime = 0;

         for (int cnt = 0; cnt < numTrials; cnt++) {
            M.setRandom();

            timer.start();
            for (int i = 0; i < timingCnt; i++) {
               qr.factor (M);
            }
            timer.stop();
            qrdTime += timer.getTimeUsec() / timingCnt;
         }

         qrdTime /= numTrials;

         System.out.println ("  " + ifmt.format (n) + "    "
         + ffmt.format (qrdTime));
      }
   }

   public void testDecomposition (MatrixNd M1) {
      testDecomposition (M1, false);
      testDecomposition (M1, true);
   }      

   public void testDecomposition (int nrows, int ncols) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      for (int i=0; i<10; i++) {
         M1.setRandom();
         testDecomposition (M1);
      }
      // test with a (semi) diagonal matrix
      M1.setZero();
      Random rand = RandomGenerator.get();
      for (int i=0; i<Math.min(nrows,ncols); i++) {
         M1.set (i, i, rand.nextDouble()-0.5);
      }
      testDecomposition (M1);
   }

   public void testDecomposition (MatrixNd M1, boolean usePivoting) {

      int nrows = M1.rowSize();
      int ncols = M1.colSize();

      Exception eActual = null;

      if (usePivoting) {
         qr.factorWithPivoting (M1);
      }
      else {
         qr.factor (M1);
      }

      int n = ncols;
      int m = nrows;
      MatrixNd QR = new MatrixNd (0, 0);
      MatrixNd MP = new MatrixNd (0, 0);
      MatrixNd QTQ = new MatrixNd (0, 0);
      int[] cperm = new int[n];

      // check the different possible values of Q and R
      int mind = Math.min (nrows, ncols);
      for (int p = mind - 1; p <= m + 1; p++) {
         MatrixNd R = new MatrixNd (p, n);
         MatrixNd Q = new MatrixNd (m, p);
         qr.get (Q, R, cperm);
         QR.mul (Q, R);
         MP.set (M1);
         MP.permuteColumns (cperm);
         if (!QR.epsilonEquals (MP, EPSILON)) {
            throw new TestException ("QR=\n" + QR.toString ("%9.4f")
            + "expected:\n" + MP.toString ("%9.4f"));
         }
         QTQ.mulTransposeLeft (Q, Q);
         // Q = Q - I
         for (int i = 0; i < QTQ.rowSize(); i++) {
            QTQ.set (i, i, QTQ.get (i, i) - 1);
         }
         if (QTQ.frobeniusNorm() > EPSILON) {
            throw new TestException ("Q not orthogonal:\n"
            + Q.toString ("%12.9f"));
         }

         if (p == mind) {
            testPreMulQ (qr, M1);
            testPreMulQTranspose (qr, M1);
            testPostMulQ (qr, M1);
            testPostMulQTranspose (qr, M1);
         }
         
      }

      // if (nrows < ncols) {
      //    return;
      // }

      double condEst = qr.conditionEstimate();

      // used for computing desired minimum norm solution
      MatrixNd MTM = new MatrixNd (n, n);
      MTM.mulTransposeLeft (M1, M1);
      MTM.invert (MTM);

      // check vector solver
      VectorNd b = new VectorNd (m);
      for (int i = 0; i < m; i++) {
         b.set (i, RandomGenerator.get().nextDouble() - 0.5);
      }
      VectorNd x = new VectorNd (n);
      VectorNd Mx = new VectorNd (m);
      eActual = null;
      try {
         qr.solve (x, b);
      }
      catch (Exception e) {
         eActual = e;
      }
      // if (m < n) {
      //    MatrixTest.checkExceptions (eActual, new ImproperSizeException (
      //       "M has fewer rows than columns"));
      // }
      // else {
      MatrixTest.checkExceptions (eActual, null);

      if (m <= n) {
         Mx.mul (M1, x);
         if (!Mx.epsilonEquals (b, EPSILON * condEst)) {
            throw new TestException (
               "solution failed:\n" + "Mx="
               + Mx.toString ("%9.4f") + "b=" + b.toString ("%9.4f"));
         }
      }
      else {
         VectorNd xcheck = new VectorNd (n);
         M1.mulTranspose (xcheck, b);
         MTM.mul (xcheck, xcheck);

         if (!xcheck.epsilonEquals (x, EPSILON * condEst)) {
            System.out.println ("n=" + n);
            System.out.println ("condEst=" + condEst);
            System.out.println ("tol=" + EPSILON * condEst);
            System.out.println ("M1=\n" + M1.toString ("%10.4f"));
            throw new TestException (
               "solution failed:\n" + "x="
               + x.toString ("%g") + "\nexpected=" + xcheck.toString ("%g"));
         }
         xcheck.set (x);
         qr.solve (x, b); // check repeatability
         if (!x.equals (xcheck)) {
            throw new TestException (
               "non-repeatable solution:\n" + "x="
               + x.toString ("%g") + "\nexpected=" + xcheck.toString ("%g"));
         }
      }

      // check matrix solver
      MatrixNd B = new MatrixNd (m, 3);
      B.setRandom();
      MatrixNd X = new MatrixNd (n, 3);
      MatrixNd MX = new MatrixNd (n, 3);

      eActual = null;
      try {
         qr.solve (X, B);
      }
      catch (Exception e) {
         eActual = e;
      }
      // if (m < n) {
      //    MatrixTest.checkExceptions (eActual, new ImproperSizeException (
      //       "M has fewer rows than columns"));
      // }
      // else {

      MatrixTest.checkExceptions (eActual, null);
      qr.solve (X, B);
      MX.mul (M1, X);
      if (m <= n) {
         if (!MX.epsilonEquals (B, EPSILON * condEst)) {
            throw new TestException (
               "solution failed:\n" + "MX="
               + MX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }
      }
      else {
         MatrixNd Xcheck = new MatrixNd (n, 3);
         Xcheck.mulTransposeLeft (M1, B);
         Xcheck.mul (MTM, Xcheck);

         if (!Xcheck.epsilonEquals (X, EPSILON * condEst)) {
            throw new TestException (
               "solution failed:\n" + "X="
               + X.toString ("%9.4f") + "expected=" + Xcheck.toString ("%9.4f"));
         }
         Xcheck.set (X);
         qr.solve (X, B);
         if (!X.equals (Xcheck)) {
            throw new TestException (
               "non-repeatable solution:\n" + "X="
               +X.toString ("%9.4f") + "expected=" + Xcheck.toString ("%9.4f"));
         }

      }

      // check R solve
      int nr = Math.min (m, n);
      b.setSize (nr);
      B.setSize (nr, 3);

      MatrixNd R = new MatrixNd (nr, n);
      VectorNd Rx = new VectorNd (nr);
      qr.get (null, R);
      qr.solveR (x, b);
      x.permute (cperm);
      Rx.mul (R, x);
      if (!Rx.epsilonEquals (b, EPSILON * condEst)) {
         throw new TestException ("solveR failed:\n" + "Rx="
         + Rx.toString ("%9.4f") + "\n" + "b=" + b.toString ("%9.4f"));
      }

      // check R matrix solve
      MatrixNd RX = new MatrixNd (nr, 3);
      qr.solveR (X, B);
      X.permuteRows (cperm);
      RX.mul (R, X);
      if (!RX.epsilonEquals (B, EPSILON * condEst)) {
         throw new TestException ("solveR failed:\n" + "RX=\n"
         + RX.toString ("%9.4f") + "B=\n" + B.toString ("%9.4f"));
      }

      if (nrows >= ncols) {
         // check left solve

         VectorNd brow = new VectorNd (ncols);
         brow.setRandom();
         VectorNd xrow = new VectorNd ();
         VectorNd xM = new VectorNd ();
         qr.leftSolve (xrow, brow);
         xM.mulTranspose (M1, xrow);
         if (!xM.epsilonEquals (brow, EPSILON * condEst)) {
            throw new TestException (
               "left solution failed:\n" + "xM="
               + xM.toString ("%9.4f") + "brow=" + brow.toString ("%9.4f"));
         }
         // check repeatability
         VectorNd xcheck = new VectorNd (xrow);
         qr.leftSolve (xrow, brow);
         if (!xrow.equals (xcheck)) {
            throw new TestException (
               "non-repeatable left solution:\n" + "x="
               + xrow.toString ("%g") + "\nexpected=" + xcheck.toString ("%g"));
         }

         MatrixNd Brow = new MatrixNd (3, ncols);
         Brow.setRandom();
         MatrixNd Xrow = new MatrixNd ();
         MatrixNd XM = new MatrixNd ();
         qr.leftSolve (Xrow, Brow);
         XM.mul (Xrow, M1);
         if (!XM.epsilonEquals (Brow, EPSILON * condEst)) {
            throw new TestException (
               "left solution failed:\n" + "XM="
               + XM.toString ("%9.4f") + "Brow=" + Brow.toString ("%9.4f"));
         }
         // check repeatability
         MatrixNd Xcheck = new MatrixNd (Xrow);
         qr.leftSolve (Xrow, Brow);
         if (!Xrow.equals (Xcheck)) {
            throw new TestException (
               "non-repeatable left solution:\n" + "X="
               + Xrow.toString ("%g") + "\nexpected=" + Xcheck.toString ("%g"));
         }

         // check left R solve
         VectorNd xR = new VectorNd (n);
         VectorNd bperm = new VectorNd (n);
         bperm.set (b);
         bperm.permute (cperm);
         qr.get (null, R);
         qr.leftSolveR (x, b);
         xR.mulTranspose (R, x);
         if (!xR.epsilonEquals (bperm, EPSILON * condEst)) {
            throw new TestException ("leftSolveR failed:\n" + "xR="
            + xR.toString ("%9.4f") + "\n" + "bperm="
            + bperm.toString ("%9.4f"));
         }

         // check left R matrix solve
         MatrixNd XR = new MatrixNd (3, n);
         MatrixNd BT = new MatrixNd (3, n);
         MatrixNd BP = new MatrixNd (3, n);
         BT.transpose (B);
         BP.set (BT);
         BP.permuteColumns (cperm);
         qr.leftSolveR (X, BT);
         XR.mul (X, R);
         if (!XR.epsilonEquals (BP, EPSILON * condEst)) {
            throw new TestException ("leftSolveR failed:\n" + "XR=\n"
            + XR.toString ("%9.4f") + "BP=\n" + BP.toString ("%9.4f"));
         }
      }

      // check determinant
      if (m == n && n <= 3) {
         double det;

         if (n == 1) {
            det = M1.get (0, 0);
         }
         else if (n == 2) {
            det = M1.get (0, 0) * M1.get (1, 1) - M1.get (0, 1) * M1.get (1, 0);
         }
         else // n == 3
         {
            det =
               M1.get (0, 0) * M1.get (1, 1) * M1.get (2, 2) + M1.get (0, 1)
               * M1.get (1, 2) * M1.get (2, 0) + M1.get (0, 2) * M1.get (1, 0)
               * M1.get (2, 1) - M1.get (0, 2) * M1.get (1, 1) * M1.get (2, 0)
               - M1.get (0, 0) * M1.get (1, 2) * M1.get (2, 1) - M1.get (0, 1)
               * M1.get (1, 0) * M1.get (2, 2);
         }
         if (Math.abs (det - qr.determinant()) > Math.abs (det * condEst
         * EPSILON)) {
            throw new TestException ("determinant failed: got "
            + qr.determinant() + " expected " + det + "\nM=\n"
            + M1.toString ("%9.4f"));
         }
      }

      // check inverse
      MatrixNd MI = new MatrixNd (n, n);
      MatrixNd IMI = new MatrixNd (n, n);

      eActual = null;
      try {
         qr.inverse (MI);
      }
      catch (Exception e) {
         eActual = e;
      }
      if (m != n) {
         MatrixTest.checkExceptions (eActual, new ImproperSizeException (
            "Original matrix not square"));
      }
      else {
         MatrixTest.checkExceptions (eActual, null);

         IMI.mul (M1, MI);
         MatrixNd I = new MatrixNd (n, n);
         I.setIdentity();

         if (!IMI.epsilonEquals (I, EPSILON * condEst)) {
            throw new TestException ("failed inverse:\n"
            + MI.toString ("%9.4f") + "M1=\n" + M1.toString ("%9.4f"));
         }
      }
   }


   protected void checkMatrix (
      String msg, MatrixNd MR, MatrixNd Mchk, MatrixNd M) {
      if (!MR.epsilonEquals (Mchk,EPSILON)) {
         System.out.println ("Got:\n" + MR.toString ("%10.7f"));
         System.out.println ("Expected:\n" + Mchk.toString ("%10.7f"));
         System.out.println ("M=\n" + M.toString ("%10.5f"));
         throw new TestException (msg);
      }      
   }

   protected void testPreMulQ (QRDecomposition qr, MatrixNd M) {
      
      MatrixNd Q = new MatrixNd (qr.nrows, qr.ncols);
      qr.get (Q, null);

      MatrixNd M1 = new MatrixNd (Q.rowSize(), Q.colSize());
      MatrixNd MR = new MatrixNd (Q.rowSize(), Q.colSize());

      M1.setIdentity();
      qr.preMulQ (MR, M1);
      checkMatrix ("testPreMulQ failed", MR, Q, M);
      MR.setIdentity();
      qr.preMulQ (MR, MR);
      checkMatrix ("testPreMulQ failed", MR, Q, M);

      Q = new MatrixNd (qr.nrows, qr.nrows);
      qr.get (Q, null);      
      MatrixNd MX = new MatrixNd (Q.rowSize(), Q.colSize()+2);
      MX.setRandom();
      MatrixNd Mchk =  new MatrixNd();
      Mchk.mul (Q, MX);
      qr.preMulQ (MR, MX);
      checkMatrix ("testPreMulQ failed", MR, Mchk, M);     
   }

   protected void testPreMulQTranspose (
      QRDecomposition qr, MatrixNd M) {

      MatrixNd QT = new MatrixNd (qr.nrows, qr.nrows);
      qr.get (QT, null);
      QT.transpose();

      MatrixNd M1 = new MatrixNd (QT.rowSize(), QT.colSize());
      MatrixNd MR = new MatrixNd (QT.rowSize(), QT.colSize());

      M1.setIdentity();
      qr.preMulQTranspose (MR, M1);
      checkMatrix ("testPreMulQTranpose failed", MR, QT, M);
      MR.setIdentity();
      qr.preMulQTranspose (MR, MR);
      checkMatrix ("testPreMulQTranpose failed", MR, QT, M);

      MatrixNd MX = new MatrixNd (qr.nrows, qr.ncols+2);
      MX.setRandom();
      MatrixNd Mchk =  new MatrixNd();
      Mchk.mul (QT, MX);
      qr.preMulQTranspose (MR, MX);
      checkMatrix ("testPreMulQ failed", MR, Mchk, M);     
   }

   protected void testPostMulQ (QRDecomposition qr, MatrixNd M) {

      MatrixNd Q = new MatrixNd (qr.nrows, qr.nrows);
      qr.get (Q, null);

      MatrixNd M1 = new MatrixNd (Q.rowSize(), Q.colSize());
      MatrixNd MR = new MatrixNd (Q.rowSize(), Q.colSize());

      M1.setIdentity();
      qr.postMulQ (MR, M1);
      checkMatrix ("testPostMulQ failed", MR, Q, M);
      MR.setIdentity();
      qr.postMulQ (MR, MR);
      checkMatrix ("testPostMulQ failed", MR, Q, M);

      MatrixNd MX = new MatrixNd (qr.ncols+2, qr.nrows);
      MX.setRandom();
      MatrixNd Mchk =  new MatrixNd();
      Mchk.mul (MX, Q);
      qr.postMulQ (MR, MX);
      checkMatrix ("testPreMulQ failed", MR, Mchk, M);     
   }

   protected void testPostMulQTranspose (
      QRDecomposition qr, MatrixNd M) {

      MatrixNd QT = new MatrixNd (qr.nrows, qr.ncols);
      qr.get (QT, null);
      QT.transpose();

      MatrixNd M1 = new MatrixNd (QT.rowSize(), QT.colSize());
      MatrixNd MR = new MatrixNd (QT.rowSize(), QT.colSize());

      M1.setIdentity();
      qr.postMulQTranspose (MR, M1);
      checkMatrix ("testPostMulQTranpose failed", MR, QT, M);
      MR.setIdentity();
      qr.postMulQTranspose (MR, MR);
      checkMatrix ("testPostMulQTranpose failed", MR, QT, M);

      QT = new MatrixNd (qr.nrows, qr.nrows);
      qr.get (QT, null);      
      QT.transpose();
      MatrixNd MX = new MatrixNd (qr.ncols+2, qr.nrows);
      MX.setRandom();
      MatrixNd Mchk =  new MatrixNd();
      Mchk.mul (MX, QT);
      qr.postMulQTranspose (MR, MX);
      checkMatrix ("testPreMulQ failed", MR, Mchk, M);     
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      testDecomposition (4, 3);
      testDecomposition (3, 4);
      testDecomposition (6, 3);
      testDecomposition (3, 6);
      testDecomposition (5, 3);
      testDecomposition (3, 5);
      testDecomposition (4, 4);
      testDecomposition (3, 3);
      testDecomposition (2, 2);
      testDecomposition (1, 1);
      testDecomposition (1, 2);
      testDecomposition (2, 1);
      testDecomposition (3, 2);
      testDecomposition (3, 1);
      testDecomposition (2, 3);
      testDecomposition (1, 3);
   }

   public static void main (String[] args) {
      QRDecompositionTest tester = new QRDecompositionTest();
      boolean doTiming = false;

      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.err.println (
               "Usage: java maspack.matrix.QRDecompositionTest [-timing]");
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
