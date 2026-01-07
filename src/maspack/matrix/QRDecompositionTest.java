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
import maspack.util.UnitTest;

class QRDecompositionTest extends UnitTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 100 * DOUBLE_PREC;
   private static double INF = Double.POSITIVE_INFINITY;

   QRDecomposition qr = new QRDecomposition();

   private void timingTests() {
      int numTrials = 10;

      // first entry 256 is just to allow warmup
      int[] matsizes = new int[] { 256, 16, 32, 64, 128, 256 };

      NumberFormat ifmt = new NumberFormat ("%3d");
      NumberFormat ffmt = new NumberFormat ("%8.2f");

      System.out.println ("matsize   time");
      // XXX XXXXX.XX

      for (int k = 0; k < matsizes.length; k++) {
         int n = matsizes[k];
         MatrixNd M = new MatrixNd (n, n);

         QRDecomposition qr = new QRDecomposition();
         FunctionTimer timer = new FunctionTimer();
         double qrdTime = 0;

         for (int cnt = 0; cnt < numTrials; cnt++) {

            M.setRandom();
            timer.start();
            qr.factor (M);
            timer.stop();
            qrdTime += timer.getTimeUsec();
         }

         qrdTime /= numTrials;

         if (k > 0) {
            System.out.println ("  " + ifmt.format (n) + "   "
                                + ffmt.format (qrdTime));
         }
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
      // test with zero
      M1.setZero();
      testDecomposition (M1);
   }

   private boolean hasPermutation (int[] perm) {
      for (int i=0; i<perm.length; i++) {
         if (i != perm[i]) {
            return true;
         }
      }
      return false;
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
            System.out.println ("R=\n" + R.toString("%18.12f"));
            throw new TestException ("QR=\n" + QR.toString ("%18.12f")
            + "expected:\n" + MP.toString ("%18.12f"));
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

      double condEst = qr.conditionEstimate();
      if (condEst == INF) {
         // The matrix is zero, so solve tests are not meaningfull.  Check that
         // Q is the identity, R is zero, and the permutation is 0, 1, 2 ...
         MatrixNd QfullChk = new MatrixNd (m, m);
         QfullChk.setIdentity();
         MatrixNd QChk = new MatrixNd (m, mind);
         QChk.setIdentity();
         
         checkEquals ("Q for zero matrix", qr.getQ(), QChk);
         MatrixNd Qfull = new MatrixNd (m, m);
         qr.get (Qfull, null, null);
         checkEquals ("full Q for zero matrix", Qfull, QfullChk);

         MatrixNd RChk = new MatrixNd (mind,n);
         checkEquals ("R for zero matrix", qr.getR(), RChk);
         if (usePivoting) {
            VectorNi permChk = new VectorNi(n);
            for (int j=0; j<n; j++) {
               permChk.set (j, j);
            }
            checkEquals (
               "col permutation for zero matrix", 
               new VectorNi(qr.getColumnPermutation()),
               permChk);
         }
         return;
      }

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
            throw new TestException (
               "leftSolveR failed:\n" + "xR=" + xR.toString ("%9.4f") +
               "\n" + "bperm=" + bperm.toString ("%9.4f"));
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
            throw new TestException (
               "leftSolveR failed:\n" + "XR=\n" + XR.toString ("%9.4f") +
               "BP=\n" + BP.toString ("%9.4f"));
         }
         if (n > 1) {
            // check with fewer columns 

            int nc = n-1;
            //b.setSize (nc);
            xR.setSize (nc);
            bperm.setSize (nc);
            MatrixNd Rsub = new MatrixNd(R);
            Rsub.setSize (nc, nc);


            qr.leftSolveR (x, b, nc);
            xR.mulTranspose (Rsub, x);
            if (!xR.epsilonEquals (bperm, EPSILON * condEst)) {
               throw new TestException (
                  "leftSolveR w/ncols failed:\n" + "xR=" + xR.toString ("%9.4f") +
                  "\n" + "bperm=" + bperm.toString ("%9.4f"));
            }

            //BT.setSize (BT.rowSize(), nc);
            BP.setSize (BP.rowSize(), nc);
            qr.leftSolveR (X, BT, nc);
            XR.mul (X, Rsub);
            if (!XR.epsilonEquals (BP, EPSILON * condEst)) {
               throw new TestException (
                  "leftSolveR w/ncols failed:\n" + "XR=\n" +
                  XR.toString ("%9.4f") + "BP=\n" + BP.toString ("%9.4f"));
            }
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

      if (usePivoting) {
         // check rank constrained leftSolve 
         VectorNd brow = new VectorNd (ncols);
         brow.setRandom();
         VectorNd xrow = new VectorNd ();
         VectorNd xM = new VectorNd ();
         double rankTol = 1e-10;
         int rank = qr.rank (rankTol);
         qr.leftSolve (xrow, brow, rank);
         MatrixNd Qreduced = qr.getQ (rank);
         MatrixNd Rreduced = qr.getR();
         Rreduced.setSize (rank, rank);
         MatrixNd QRreduced = new MatrixNd();
         QRreduced.mul (Qreduced, Rreduced);
         VectorNd bperm = new VectorNd(brow);
         bperm.permute (cperm);
         bperm.setSize (rank);
         xM.mulTranspose (QRreduced, xrow);
         if (!xM.epsilonEquals (bperm, EPSILON * condEst)) {
            System.out.println ("M size=" + M1.getSize());
            throw new TestException (
               "left rank constrained solution failed:\n" + "   xM="
               + xM.toString ("%9.4f") + "\nbperm=" + bperm.toString ("%9.4f"));
         }
         if (M1.colSize() > 1 && M1.rowSize() > 1) {
            // make M1 rank deficient
            SVDecomposition svd = new SVDecomposition(M1);
            VectorNd sig = new VectorNd (svd.getS());
            // zero some of the sig values
            int nz = RandomGenerator.nextInt (1, sig.size()-1);
            for (int i=sig.size()-1; i>=sig.size()-nz; i--) {
               sig.set (i, 0);
            }
            M1.set (svd.getU());
            M1.mulDiagonalRight (sig);
            M1.mulTransposeRight (M1, svd.getV());
            qr.factorWithPivoting (M1);

            rank = qr.rank (rankTol);
            // make sure R is ordered properly
            MatrixNd RR = qr.getR();
            for (int j=1; j<Math.min(M1.colSize(),M1.rowSize()); j++) {
               if ((RR.get(j-1,j-1) - RR.get(j,j)) < -1e15) {
                  System.out.println ("R=\n" + RR.toString ("%10.7f"));
                  throw new TestException (
                     "R matrix incorrectly ordered");
               }
            }
            rank = qr.rank (rankTol);
            cperm = qr.getColumnPermutation();
            qr.leftSolve (xrow, brow, rank);
            Qreduced = qr.getQ (rank);
            Rreduced = qr.getR();
            Rreduced.setSize (rank, rank);

            QRreduced = new MatrixNd();
            QRreduced.mul (Qreduced, Rreduced);
            bperm = new VectorNd(brow);
            bperm.permute (qr.getColumnPermutation());
            bperm.setSize (rank);
            xM.setSize (rank);
            xM.mulTranspose (QRreduced, xrow);
            if (!xM.epsilonEquals (bperm, EPSILON * condEst)) {
               throw new TestException (
                  "left rank constrained solution failed:\n" + "   xM="
                  + xM.toString ("%9.4f") + "\nbperm=" +
                  bperm.toString ("%9.4f"));
            }

            qr.leftSolveR (xrow, brow, rank);
            VectorNd xR = new VectorNd();
            xR.mulTranspose (Rreduced, xrow);
            if (!xR.epsilonEquals (bperm, EPSILON * condEst)) {
               throw new TestException (
                  "left rank constrained solution w/ncols failed:\n" + "   xR="
                  + xR.toString ("%9.4f") + "\nbperm=" +
                  bperm.toString ("%9.4f"));
            }

            MatrixNd Bperm = new MatrixNd (3, n);
            MatrixNd B1 = new MatrixNd (3, n);

            B1.setRandom();
            Bperm.set (B1);
            Bperm.permuteColumns (cperm);
            Bperm.setSize (Bperm.rowSize(), rank);
            qr.leftSolveR (X, B1, rank);
            MatrixNd XR = new MatrixNd();
            XR.mul (X, Rreduced);
            if (!XR.epsilonEquals (Bperm, EPSILON * condEst)) {
               throw new TestException (
                  "left rank constrained solution w/ncols failed:\n" + "XR=\n"
                  + XR.toString ("%9.4f") + "Bperm=\n" +
                  Bperm.toString ("%9.4f"));
            }
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
      
      MatrixNd Q = new MatrixNd (qr.myNumRows, qr.myNumCols);
      qr.get (Q, null);

      MatrixNd M1 = new MatrixNd (Q.rowSize(), Q.colSize());
      MatrixNd MR = new MatrixNd (Q.rowSize(), Q.colSize());

      M1.setIdentity();
      qr.preMulQ (MR, M1);
      checkMatrix ("testPreMulQ failed", MR, Q, M);
      MR.setIdentity();
      qr.preMulQ (MR, MR);
      checkMatrix ("testPreMulQ failed", MR, Q, M);

      Q = new MatrixNd (qr.myNumRows, qr.myNumRows);
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

      MatrixNd QT = new MatrixNd (qr.myNumRows, qr.myNumRows);
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

      MatrixNd MX = new MatrixNd (qr.myNumRows, qr.myNumCols+2);
      MX.setRandom();
      MatrixNd Mchk =  new MatrixNd();
      Mchk.mul (QT, MX);
      qr.preMulQTranspose (MR, MX);
      checkMatrix ("testPreMulQ failed", MR, Mchk, M);     
   }

   protected void testPostMulQ (QRDecomposition qr, MatrixNd M) {

      MatrixNd Q = new MatrixNd (qr.myNumRows, qr.myNumRows);
      qr.get (Q, null);

      MatrixNd M1 = new MatrixNd (Q.rowSize(), Q.colSize());
      MatrixNd MR = new MatrixNd (Q.rowSize(), Q.colSize());

      M1.setIdentity();
      qr.postMulQ (MR, M1);
      checkMatrix ("testPostMulQ failed", MR, Q, M);
      MR.setIdentity();
      qr.postMulQ (MR, MR);
      checkMatrix ("testPostMulQ failed", MR, Q, M);

      MatrixNd MX = new MatrixNd (qr.myNumCols+2, qr.myNumRows);
      MX.setRandom();
      MatrixNd Mchk =  new MatrixNd();
      Mchk.mul (MX, Q);
      qr.postMulQ (MR, MX);
      checkMatrix ("testPreMulQ failed", MR, Mchk, M);     
   }

   protected void testPostMulQTranspose (
      QRDecomposition qr, MatrixNd M) {

      MatrixNd QT = new MatrixNd (qr.myNumRows, qr.myNumCols);
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

      QT = new MatrixNd (qr.myNumRows, qr.myNumRows);
      qr.get (QT, null);      
      QT.transpose();
      MatrixNd MX = new MatrixNd (qr.myNumCols+2, qr.myNumRows);
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
      testDecomposition (6, 6);
      testDecomposition (5, 5);
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

      // special case that gave some trouble
      MatrixNd H = new MatrixNd(6,6);
      H.set (0, 3, 1);
      H.set (1, 4, 1);
      H.set (2, 5, 1);
      H.set (3, 0, 1);
      H.set (3, 2, -1);
      H.set (4, 1, 1);
      H.set (5, 2, -0.000000003205000);

      testDecomposition (H);
   }

   void testSpecial() {
      double[] vals = new double[] {
-0.007910678987498442, 4.982996185814992E-10, -0.007910678987498424, 0.015821357476697245, 
1.263283252575368E-11, -2.6055228139052163E-11, 1.2632868854896859E-11, 7.895267584016228E-13, 
0.060087571545670494, -4.33910249397762E-9, 0.06008757154567044, -0.12017513875223843, 
2.7037178882033367E-12, -5.576184334087363E-12, 2.7035653317133644E-12, 1.689011141706614E-13, 
 0.48484850128627077, -3.499805576942516E-8, 0.4848485012862708, -0.969696967574486, 
// -2.099358712703328E-12, 4.3298907028653894E-12, -2.0993592303077874E-12, -1.3117275985427345E-13, 
// 0.007910679060304907, -6.484944525819714E-10, 0.007910679060305226, -0.015821357472115678, 
// 1.3849983862342824E-11, -2.856591137542923E-11, 1.3850245524438594E-11, 8.656819886478093E-13, 
// -0.060087571561343374, 4.371663933633307E-9, -0.060087571561343096, 0.12017513875102252, 
// 2.7036168222527025E-12, -5.576121426258073E-12, 2.7035470765985258E-12, 1.6895752740684462E-13, 
// 1.749977768250588E-8, 4.855560398198122E-11, 1.7499777626994728E-8, -3.5048110771235264E-8, 
// -2.0995281350246004E-12, 4.3298743782473145E-12, -2.09918114497621E-12, -1.3116509824650441E-13, 
// 0.007910679058786577, -6.453256037181054E-10, 0.007910679058786611, -0.01582135747224758, 
// 1.2632841880449165E-11, -2.6055228887696784E-11, 1.2632861922092132E-11, 7.895250851554882E-13, 
// -0.060087571549677206, 4.347593604570044E-9, -0.06008757154967732, 0.1201751387517609, 
// 2.7035283251623695E-12, -5.5761151355825754E-12, 2.7036191768818E-12, 1.6896763353840678E-13, 
// -1.754683554011649E-8, 4.855738033882062E-11, -1.7546835262560734E-8, 3.50451134223384E-8, 
// -2.0994307426982353E-12, 4.329901886952912E-12, -2.0993036473192335E-12, -1.311674969354428E-13, 
// -0.007910678985949795, 4.951307749218037E-10, -0.007910678985949715, 0.01582135747676874, 
// 1.3850019584710997E-11, -2.856588500192645E-11, 1.385022153745668E-11, 8.656438797587688E-13, 
// 0.06008757153377513, -4.315032053892054E-9, 0.06008757153377532, -0.12017513875251841, 
// 2.7036237908811823E-12, -5.576161944466623E-12, 2.7034683082583925E-12, 1.6906984532704777E-13, 
// -0.4848485013333286, 3.5095168424148504E-8, -0.4848485013333287, 0.9696969675714888, 
// -2.09927748225735E-12, 4.329897099274799E-12, -2.099317928859166E-12, -1.3130168815828362E-13
      };
      MatrixNd M = new MatrixNd (vals.length/4, 4);
      M.set(vals);
      QRDecomposition qrd = new QRDecomposition();
      qrd.factorWithPivoting (M);
      System.out.println ("R=\n" + qrd.getR().toString("%10.7f"));
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
         //tester.testSpecial();
         tester.execute();
      }

      System.out.println ("\nPassed\n");
   }
}
