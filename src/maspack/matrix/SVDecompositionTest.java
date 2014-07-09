/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.FunctionTimer;
import maspack.util.RandomGenerator;
import maspack.util.TestException;

class SVDecompositionTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 100 * DOUBLE_PREC;

   SVDecomposition svd = new SVDecomposition (0);

   static boolean isOrthogonal (MatrixNd M) {
      if (M.nrows >= M.ncols) {
         VectorNd col1 = new VectorNd (M.nrows);
         VectorNd col2 = new VectorNd (M.nrows);
         for (int j = 0; j < M.ncols; j++) {
            M.getColumn (j, col1);
            if (Math.abs (col1.norm() - 1) > EPSILON) {
               return false;
            }
            for (int k = j + 1; k < M.ncols; k++) {
               M.getColumn (k, col2);
               if (Math.abs (col1.dot (col2)) > EPSILON) {
                  return false;
               }
            }
         }
      }
      else {
         VectorNd row1 = new VectorNd (M.ncols);
         VectorNd row2 = new VectorNd (M.ncols);
         for (int i = 0; i < M.nrows; i++) {
            M.getRow (i, row1);
            if (Math.abs (row1.norm() - 1) > EPSILON) {
               return false;
            }
            for (int k = i + 1; k < M.nrows; k++) {
               M.getRow (k, row2);
               if (Math.abs (row1.dot (row2)) > EPSILON) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   public void testDecomposition (int nrows, int ncols) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandom();
      testDecomposition (M1, null);
   }

   public void testDecomposition (int nrows, int ncols, double[] svals) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandomSvd (svals);
      testDecomposition (M1, null);
   }

   public void testDecomposition (MatrixNd M1, double[] svals) {
      svd.factor (M1);

      int nrows = M1.nrows;
      int ncols = M1.ncols;
      int mind = Math.min (nrows, ncols);

      MatrixNd U = new MatrixNd (nrows, mind);
      MatrixNd V = new MatrixNd (ncols, mind);
      VectorNd sig = new VectorNd (mind);

      svd.get (U, sig, V);

      if (!isOrthogonal (U)) {
         throw new TestException ("U not orthogonal:\n" + U.toString ("%9.4f"));
      }
      if (!isOrthogonal (V)) {
         throw new TestException ("V not orthogonal:\n" + V.toString ("%9.4f"));
      }

      // verify product

      double cond = svd.condition();

      MatrixNd US = new MatrixNd (nrows, mind);
      US.set (U);
      US.mulDiagonalRight (sig);
      MatrixNd MP = new MatrixNd (nrows, ncols);
      MP.mulTransposeRight (US, V);

      if (!MP.epsilonEquals (M1, EPSILON * svd.norm())) {
         MP.sub (M1);
         throw new TestException ("U S V' = \n" + MP.toString ("%9.4f")
         + "expecting:\n" + M1.toString ("%9.4f") + "error="
         + MP.infinityNorm());
      }

      if (svals != null) {
         boolean[] taken = new boolean[mind];
         for (int i = 0; i < mind; i++) {
            int j;
            for (j = 0; j < mind; j++) {
               if (!taken[j] &&
                   Math.abs (sig.get (j) - svals[i]) > svals[i] * EPSILON) {
                  taken[j] = true;
                  break;
               }
            }
            if (j == mind) {
               throw new TestException ("singular values " + svals[i]
               + " not computed");
            }
         }
      }

      // check vector solver
      if (nrows == ncols) {
         VectorNd b = new VectorNd (nrows);
         for (int i = 0; i < nrows; i++) {
            b.set (i, RandomGenerator.get().nextDouble() - 0.5);
         }
         VectorNd x = new VectorNd (ncols);
         VectorNd Mx = new VectorNd (nrows);
         svd.solve (x, b);
         Mx.mul (M1, x);
         if (!Mx.epsilonEquals (b, EPSILON * cond)) {
            throw new TestException ("solution failed:\n" + "Mx="
            + Mx.toString ("%9.4f") + "b=" + b.toString ("%9.4f") + "x="
            + x.toString ("%9.4f"));
         }
      }

      // check matrix solver
      if (nrows == ncols) {
         MatrixNd B = new MatrixNd (nrows, 3);
         B.setRandom();
         MatrixNd X = new MatrixNd (ncols, 3);
         MatrixNd MX = new MatrixNd (nrows, 3);

         svd.solve (X, B);
         MX.mul (M1, X);
         if (!MX.epsilonEquals (B, EPSILON * cond)) {
            throw new TestException ("solution failed:\n" + "MX="
            + MX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }
      }

      // check determinant
      if (nrows == ncols && nrows <= 3) {
         double det;

         if (nrows == 1) {
            det = M1.get (0, 0);
         }
         else if (nrows == 2) {
            det = M1.get (0, 0) * M1.get (1, 1) - M1.get (0, 1) * M1.get (1, 0);
         }
         else // nrows == 3
         {
            det =
               M1.get (0, 0) * M1.get (1, 1) * M1.get (2, 2) + M1.get (0, 1)
               * M1.get (1, 2) * M1.get (2, 0) + M1.get (0, 2) * M1.get (1, 0)
               * M1.get (2, 1) - M1.get (0, 2) * M1.get (1, 1) * M1.get (2, 0)
               - M1.get (0, 0) * M1.get (1, 2) * M1.get (2, 1) - M1.get (0, 1)
               * M1.get (1, 0) * M1.get (2, 2);
         }
         double sigsum = 0;
         for (int i = 0; i < nrows; i++) {
            sigsum += Math.abs (sig.get (i));
         }
         if (Math.abs (det - svd.determinant()) > Math.abs (sigsum * EPSILON)) {
            throw new TestException ("determinant failed: got "
            + svd.determinant() + " expected " + det + "\nM=\n"
            + M1.toString ("%9.4f"));
         }
      }

      // check inverse
      if (nrows == ncols) {
         int n = nrows;
         MatrixNd MI = new MatrixNd (n, n);
         MatrixNd IMI = new MatrixNd (n, n);
         svd.inverse (MI);
         IMI.mul (M1, MI);
         MatrixNd I = new MatrixNd (n, n);
         I.setIdentity();

         if (!IMI.epsilonEquals (I, EPSILON * cond)) {
            throw new TestException ("failed inverse:\n"
            + MI.toString ("%9.4f") + "M1=\n" + M1.toString ("%9.4f"));
         }
      }
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      testDecomposition (new MatrixNd (2, 2, new double[] { 1, 2, 6, 5 }), null);
      testDecomposition (new MatrixNd (2, 2, new double[] { 0, 0, 0, 0 }), null);
      testDecomposition (
         new MatrixNd (2, 2, new double[] { 0, Math.PI, 0, 0 }), null);
      testDecomposition (new MatrixNd (2, 2, new double[] { 0, Math.PI,
                                                           Math.PI, 0 }), null);
      testDecomposition (new MatrixNd (2, 2, new double[] { 1, 2, 6, 5 }), null);
      testDecomposition (
         new MatrixNd (2, 2, new double[] { 3.0622370358907016,
                                           -10.219748167188229,
                                           -10.219748167188238,
                                           34.10684782942292 }), null);

      testDecomposition (new MatrixNd (3, 3, new double[] { 1, 0, 0, 0, 2, 0,
                                                           0, 0, 3 }), null);
      testDecomposition (new MatrixNd (3, 3, new double[] { 1, 2, 3, 4, 5, 6,
                                                           7, 8, 9 }), null);
      testDecomposition (
         new MatrixNd (
            3, 4, new double[] { 1, 2, 3, 4, 2, 4, 2, 4, 1, 8, 9, 3 }), null);
      testDecomposition (
         new MatrixNd (
            4, 3, new double[] { 1, 2, 3, 4, 2, 4, 2, 4, 1, 8, 9, 3 }), null);

      testDecomposition (
         new MatrixNd (3, 3, new double[] { 0.46914823730452665,
                                           -2.009927412169458,
                                           0.8481049304975272,
                                           0.41645167964478735,
                                           -1.639935747542862,
                                           -1.090233210860603, 0.0, 0.0, 0.0 }),
         null);
      testDecomposition (
         new MatrixNd (3, 3, new double[] { 2.2773016205484105,
                                           -0.31338266946551563,
                                           0.6778778092657844,
                                           -0.8388186812067157,
                                           -1.104692980287434,
                                           -0.3719235007583789, 0, 0, 0 }),
         null);
      testDecomposition (new MatrixNd (
         3, 3, new double[] { 1.7271458646290014, -0.3070131535540012,
                             -0.3861866812734839, -0.6113434028645098,
                             -1.8807249610714911, -0.011968911164261908, 0, 0,
                             0 }), null);
      testDecomposition (new MatrixNd (
         3, 3, new double[] { 2.033319309140049, -0.25028611817089313,
                             -0.3385028472987217, -0.5266982813470501,
                             -2.05763587270708, -0.0968065248015208, 0.0, 0.0,
                             0.0 }), null);

      testDecomposition (new MatrixNd (
         3, 3, new double[] { 1.9885969788993956, -0.5060050726003058,
                             0.48307368305754705, -0.3752012250425878,
                             -1.6807108448715908, -0.17451750023680307, 0.0,
                             0.0, 0.0 }), null);

      testDecomposition (2, 2, new double[] { 1, 0 });
      testDecomposition (2, 2, new double[] { 1, 0.0000000001 });

      testDecomposition (6, 5, new double[] { 1.1, 2.2, 4.4, 0.0003, 0 });
      testDecomposition (4, 3);
      testDecomposition (3, 4);
      testDecomposition (3, 3, new double[] { 1, 0.0001, 0 });
      testDecomposition (3, 4, new double[] { 1, 2, 0 });
      testDecomposition (4, 5, new double[] { 1, 1, 0, 0 });
      testDecomposition (6, 6);
      testDecomposition (6, 6, new double[] { 1.1, 2.2, 3.3, 0.0001, 0, 0 });
      testDecomposition (6, 6, new double[] { 1.1, 2.2, 3.3, 0.0001, 0, 0 });
      testDecomposition (6, 5, new double[] { 1.1, 2.2, 4.4, 0.0003, 0 });
      testDecomposition (5, 6, new double[] { 12, 13, 14, 0.0003, 0 });

      for (int i = 0; i < 10; i++) {
         testDecomposition (4, 4);
         testDecomposition (3, 3);
         testDecomposition (2, 2);
         testDecomposition (1, 1);
      }
   }

   public void checkTiming (int nr, int nc) {
      int nd = Math.min (nr, nc);

      MatrixNd M1 = new MatrixNd (nr, nc);
      M1.setRandom();

      MatrixNd U = new MatrixNd (nr, nd);
      MatrixNd V = new MatrixNd (nc, nd);
      VectorNd s = new VectorNd (nd);

      SVDecomposition svd = new SVDecomposition (0);

      int cnt = 10000;
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i = 0; i < cnt; i++) {
         svd.factor (M1);
         svd.get (U, s, V);
      }
      timer.stop();
      System.out.println ("our svd: " + timer.result (cnt));

      System.out.println (s);

      // System.out.println ("U=\n" + U.toString("%9.5f"));
      // System.out.println ("V=\n" + V.toString("%9.5f"));
      // System.out.println ("s=" + s.toString("%9.5f"));

      // javax.vecmath.GMatrix GM = new javax.vecmath.GMatrix(nr,nc);
      // for (int i=0; i<nr; i++)
      // { for (int j=0; j<nc; j++)
      // { GM.setElement (i, j, M1.get(i,j));
      // }
      // }
      // javax.vecmath.GMatrix GU = new javax.vecmath.GMatrix(nr,nd);
      // javax.vecmath.GMatrix GV = new javax.vecmath.GMatrix(nc,nd);
      // javax.vecmath.GMatrix GS = new javax.vecmath.GMatrix(nd,nd);

      // timer.start();
      // for (int i=0; i<cnt; i++)
      // { GM.SVD (GU, GS, GV);
      // }
      // timer.stop();
      // System.out.println ("vecmath svd: " + timer.result(cnt));

      // System.out.println (GU);
      // System.out.println (GV);
      // System.out.println (GS);
      // System.out.println (GM);
   }

   public static void main (String[] args) {
      SVDecompositionTest tester = new SVDecompositionTest();

      tester.execute();
      // tester.checkTiming(6, 6);
      // tester.checkTiming(6, 6);
      // tester.checkTiming(6, 6);
      // tester.checkTiming(6, 6);
      // tester.checkTiming(6, 6);
      // tester.checkTiming(6, 6);

      System.out.println ("\nPassed\n");
   }
}
