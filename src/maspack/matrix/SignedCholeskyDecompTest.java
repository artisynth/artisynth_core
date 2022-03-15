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
import maspack.util.UnitTest;

class SignedCholeskyDecompTest extends UnitTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;

   SignedCholeskyDecomp chol = new SignedCholeskyDecomp();

   private void timingTests() {
      int numTrials = 10;

      // first entry 512 is just to allow warmup
      int[] matsizes = new int[] { 512, 32, 64, 128, 256, 512 };
      //int[] matsizes = new int[] { 510, 512 };

      NumberFormat ifmt = new NumberFormat ("%4d");
      NumberFormat ffmt = new NumberFormat ("%8.2f");

      System.out.println ("\ntimes are in usec\n");

      System.out.println (
         "matsize    factor   addPos   addNeg   delPos   delNeg");
        // XXXX    XXXXX.XX XXXXX.XX XXXXX.XX XXXXX.XX XXXXX.XX

      FunctionTimer timer = new FunctionTimer();

      for (int k = 0; k < matsizes.length; k++) {
         int n = matsizes[k];
         int r = n/2;
         MatrixNd M = new MatrixNd (n, n);
         VectorNd colp = new VectorNd (n); // positive column to remove
         VectorNd coln = new VectorNd (n); // negative column to remove

         SignedCholeskyDecomp chol = new SignedCholeskyDecomp (n);

         double factorTime = 0;
         double addPosTime = 0;
         double delPosTime = 0;
         double addNegTime = 0;
         double delNegTime = 0;

         //int nt = (k < 0 ? numTrials : 1);
         int nt = numTrials;

         for (int cnt = 0; cnt < nt; cnt++) {
            M.setRandom();
            M.mulTranspose (M);
            if (r < n) {
               MatrixNd MC = new MatrixNd (n-r, n-r);
               M.getSubMatrix (r, r, MC);
               MC.negate();
               M.setSubMatrix (r, r, MC);
            }

            int idxp = r/2;
            int idxn = r+(n-r)/2;

            // extract the colums colp and coln that will be removed from the
            // positive and negative partitions, respectively, and then permute
            // their entries so that they can be readded at the end of the
            // positive and negative partitions
            M.getColumn (idxp, colp);
            M.getColumn (idxn, coln);
            double x00 = colp.get (idxp);
            double xr0 = coln.get (idxp);
            for (int i=idxp; i<r-1; i++) {
               colp.set (i, colp.get (i+1));
               coln.set (i, coln.get (i+1));
            }
            colp.set (r-1, x00);
            coln.set (r-1, xr0);

            double xrr = coln.get (idxn);
            for (int i=idxn; i<n-1; i++) {
               coln.set (i, coln.get (i+1));
            }
            coln.set (n-1, xrr);

            timer.start();
            chol.factor (M, r);
            timer.stop();
            factorTime += timer.getTimeUsec();

            // delete row/col 0
            timer.start();
            if (!chol.deleteRowAndColumn (idxp, /*tol=*/0.0)) {
               throw new TestException (
                  "deleteRowAndColumn failed; result not SPD/SND");
            }

            timer.stop();
            delPosTime += timer.getTimeUsec();

            // add it back
            timer.start();
            if (!chol.addPosRowAndColumn (colp, /*tol=*/0.0)) {
               throw new TestException (
                  "addPosRowAndColumn failed; result not SPD/SND");
            }
            
            timer.stop();
            addPosTime += timer.getTimeUsec();

            // delete row/col r
            timer.start();
            if (!chol.deleteRowAndColumn (idxn, /*tol=*/0.0)) {
               throw new TestException (
                  "deleteRowAndColumn failed; result not SPD/SND");
            }
            
            timer.stop();
            delNegTime += timer.getTimeUsec();

            // add it back
            timer.start();
            if (!chol.addNegRowAndColumn (coln, /*tol=*/0.0)) {
               throw new TestException (
                  "addNegRowAndColumn failed; result not SPD/SND");
            }
            
                  
            timer.stop();
            addNegTime += timer.getTimeUsec();
         }

         factorTime /= numTrials;
         addPosTime /= numTrials;
         delPosTime /= numTrials;
         addNegTime /= numTrials;
         delNegTime /= numTrials;

         if (k > 0) {
            System.out.println (
               " " + ifmt.format(n) + "    " +
               ffmt.format(factorTime) + " " +
               ffmt.format(addPosTime) + " " +
               ffmt.format(addNegTime) + " " +
               ffmt.format(delPosTime) + " " +
               ffmt.format(delNegTime));
         //    System.out.printf (" factor: %g\n", factorTime);
         //    System.out.printf (" add/delPos: %g %g\n", addPosTime, delPosTime);
         //    System.out.printf (" add/delNeg: %g %g\n", addNegTime, delNegTime);
         }
      }
   }

   MatrixNd deleteRowAndColumn (MatrixNd M, int idx) {
      int n = M.rowSize();
      MatrixNd Msub = new MatrixNd (n-1, n-1);
      int isub = 0;
      for (int i=0; i<n; i++) {
         if (i != idx) {
            int jsub = 0;                  
            for (int j=0; j<n; j++) {
               if (j != idx) {
                  Msub.set (isub, jsub, M.get(i,j));
                  jsub++;
               }
            }
            isub++;
         }
      }
      return Msub;
   }

   MatrixNd addPosRowAndColumn (MatrixNd M, VectorNd col, int r) {
      int n = M.rowSize();
      MatrixNd Madd = new MatrixNd (n+1, n+1);
      int iold = 0;
      for (int i=0; i<n+1; i++) {
         if (i != r) {
            int jold = 0;                  
            for (int j=0; j<n+1; j++) {
               if (j != r) {
                  Madd.set (i, j, M.get(iold, jold));
                  jold++;
               }
               else {
                  Madd.set (i, j, col.get(i));
               }
            }
            iold++;
         }
         else {
            for (int j=0; j<n+1; j++) {
               Madd.set (i, j, col.get(j));
            }
         }
      }
      return Madd;
   }

   MatrixNd addNegRowAndColumn (MatrixNd M, VectorNd col) {
      int n = M.rowSize();
      MatrixNd Madd = new MatrixNd (n+1, n+1);
      Madd.setSubMatrix (0, 0, M);
      Madd.setRow (n, col);
      Madd.setColumn (n, col);
      return Madd;
   }

   public void testRowColumnAdd (MatrixNd M, int r) {
      int n = M.rowSize();
      SignedCholeskyDecomp inc = new SignedCholeskyDecomp();
      MatrixNd Madd = new MatrixNd();
      VectorNd col = new VectorNd();

      for (int k=0; k<n; k++) {
         M.getColumn (k, col);
         Madd.setSize (k+1, k+1);
         Madd.setRow (k, col);
         Madd.setColumn (k, col);
         if (k < r) {
            inc.addPosRowAndColumn (col);
         }
         else {
            inc.addNegRowAndColumn (col);
         }
         checkFactorization (inc, Madd);
      }
   }

   public void testRowColumnDelete (MatrixNd M, int r, int idx) {
      int n = M.rowSize();
      SignedCholeskyDecomp inc = new SignedCholeskyDecomp();
      inc.factor (M, r);
      inc.deleteRowAndColumn (idx);

      VectorNd col = new VectorNd(n);
      M.getColumn (idx, col);
      // permute entries to where there should be when we add column back
      double cval = col.get(idx);
      if (idx < r) {
         for (int i=idx; i<r-1; i++) {
            col.set (i, col.get(i+1));
         }
         col.set (r-1, cval);
      }
      else {
         for (int i=idx; i<n-1; i++) {
            col.set (i, col.get(i+1));
         }
         col.set (n-1, cval);
      }

      MatrixNd Msub = deleteRowAndColumn (M, idx);
      checkFactorization (inc, Msub);

      MatrixNd Madd;
      if (idx < r) {
         Madd = addPosRowAndColumn (Msub, col, r-1);
         inc.addPosRowAndColumn (col);
      }
      else {
         Madd = addNegRowAndColumn (Msub, col);
         inc.addNegRowAndColumn (col);
      }
      checkFactorization (inc, Madd);
   }

   public void testDecomposition (int nrows, int ncols, int r) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandom();
      if (nrows == ncols) {
         M1.mulTranspose (M1);
         if (r < nrows) {
            MatrixNd MC = new MatrixNd (nrows-r, nrows-r);
            M1.getSubMatrix (r, r, MC);
            MC.negate();
            M1.setSubMatrix (r, r, MC);
         }
      }

      Exception eActual = null;
      Exception eExpected = null;
      if (nrows != ncols) {
         eExpected = new ImproperSizeException ("Matrix not square");
      }
      try {
         chol.factor (M1, r);
      }
      catch (Exception e) {
         eActual = e;
      }
      MatrixTest.checkExceptions (eActual, eExpected);
      if (eActual == null) {
         int n = nrows;

         MatrixNd L = new MatrixNd();
         checkFactorization (chol, M1);

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
            chol.get (L, null);
            System.out.println ("M=\n" + M1.toString ("%12.8f"));
            System.out.println ("L=\n" + L.toString ("%12.8f"));
            System.out.println ("b=\n" + b.toString ("%12.8f"));
            System.out.println ("x=\n" + x.toString ("%12.8f"));
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

         SignedCholeskyDecomp inc = new SignedCholeskyDecomp();
         // for (int j = 0; j < n; j++) {
         //    Msub.setSize (j + 1, j + 1);
         //    M1.getSubMatrix (0, 0, Msub);
         //    VectorNd col = new VectorNd (j + 1);
         //    Msub.getColumn (j, col);
         //    inc.addRowAndColumn (col);
         //    checkFactorization (inc, Msub);
         // }
         if (n > 3) {
            // check single row/col add
            testRowColumnAdd (M1, r);

            // check single row/col deletion
            for (int idx=0; idx<n-1; idx++) {
               testRowColumnDelete (M1, r, idx);
            }
            // check multiple row/col deletion
            int[] delIdxs = RandomGenerator.randomSubsequence(n);
            inc.factor (M1, r);
            inc.deleteRowsAndColumns (delIdxs);
            // set Msub explicitly
            boolean[] removed = new boolean[n];
            for (int k=0; k<delIdxs.length; k++) {
               removed[delIdxs[k]] = true;
            }
            int subsize = n-delIdxs.length;
            MatrixNd Msub = new MatrixNd (subsize, subsize);
            int isub = 0;
            for (int i=0; i<n; i++) {
               if (!removed[i]) {
                  int jsub = isub;
                  for (int j=i; j<n; j++) {
                     if (!removed[j]) {
                        double mij = M1.get(i,j);
                        Msub.set (isub, jsub, mij);
                        if (isub != jsub) {
                           Msub.set (jsub, isub, mij);
                        }
                        jsub++;
                     }
                  }
                  isub++;
               }
            }
            checkFactorization (inc, Msub);   
         }
      }
   }

   private void checkFactorization (SignedCholeskyDecomp chol, MatrixNd M) {
      MatrixNd L = new MatrixNd (0, 0);
      VectorNd D = new VectorNd();
      MatrixNd LDLT = new MatrixNd (0, 0);
      chol.get (L, D);
      double condEst = chol.conditionEstimate (M);
      LDLT.set (L);
      LDLT.mulDiagonalRight (D);
      LDLT.mulTranspose (L);
      if (!LDLT.epsilonEquals (M, EPSILON * condEst)) {
         throw new TestException ("LDLT=\n" + LDLT.toString ("%9.4f")
         + "expected:\n" + M.toString ("%9.4f"));
      }
   }

   public void test() {

      testDecomposition (4, 3, 3);
      testDecomposition (3, 4, 3);
      for (int i = 0; i < 10; i++) {
         for (int size=1; size<=10; size++) {
            testDecomposition (size, size, 0);
            testDecomposition (size, size, size);
            testDecomposition (size, size, 1);
            testDecomposition (size, size, (size+1)/2);
            testDecomposition (size, size, size-1);
         }
      }
   }

   public static void main (String[] args) {
      SignedCholeskyDecompTest tester = new SignedCholeskyDecompTest();

      RandomGenerator.setSeed (0x1234);
      boolean doTiming = false;

      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.err.println (
               "Usage: java maspack.matrix.SignedCholeskyDecompTest [-timing]");
            System.exit (1);
         }
      }

      if (doTiming) {
         tester.timingTests();
      }
      else {
         tester.runtest();
      }

   }
}
