/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.FunctionTimer;
import maspack.util.UnitTest;
import maspack.util.RandomGenerator;
import maspack.util.TestException;

class SVDecompositionTest extends UnitTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 100 * DOUBLE_PREC;

   SVDecomposition svd = new SVDecomposition ();

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
      testDecomposition (nrows, ncols, 0);
   }
   
   public void testDecomposition (int nrows, int ncols, int flags) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandom();
      testDecomposition (M1, null);
   }

   public void testDecomposition (int nrows, int ncols, double[] svals) {
      testDecomposition (nrows, ncols, svals, 0);
   }
   
   public void testDecomposition (MatrixNd M1, double[] svals) {
      testDecomposition (M1, svals, 0);
   }
   
   public void testDecomposition (
      int nrows, int ncols, double[] svals, int flags) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandomSvd (svals);
      testDecomposition (M1, svals, flags);
   }
   
   public void testInversion(int nrows, int ncols, int flags) {
      MatrixNd M1 = new MatrixNd (nrows, ncols);
      M1.setRandom();
      testInversion(M1, flags);
   }
   
   public void testInversion(MatrixNd M1, int flags) {
      svd.factor (M1, flags);

      int nrows = M1.nrows;
      int ncols = M1.ncols;
      
      MatrixNd R = new MatrixNd(ncols, nrows);
      svd.pseudoInverse (R);
      
      // properties of pseudo-inverse
      MatrixNd A = new MatrixNd();
      A.mul (M1, R);
      A.mul (M1);
      checkNormedEquals ("pseudo inverse, M1*R*M1", M1, A, 1e-12);
      
      A.mul (R, M1);
      A.mul (R);
      checkNormedEquals ("pseudo inverse, R*M1*R", R, A, 1e-12);
      
      A.mul (M1, R);
      if (!A.isSymmetric (EPSILON * svd.norm()*100)) {
         throw new TestException (
            "Invalid pseudo inverse :\n" + R.toString ("%9.4f"));
      }
      
      A.mul (R, M1);
      if (!A.isSymmetric(EPSILON * svd.norm()*100)) {
         throw new TestException (
            "Invalid pseudo inverse :\n" + R.toString ("%9.4f"));
      }
      
      // all passed
   }
   
   public void testDecomposition (MatrixNd M1, double[] svals, int flags) {
      svd.factor (M1, flags);

      int nrows = M1.nrows;
      int ncols = M1.ncols;
      int mind = Math.min (nrows, ncols);
      
      int ucols = mind;
      int vcols = mind;
      if ( (flags & SVDecomposition.FULL_UV) != 0) {
         ucols = nrows;
         vcols = ncols;
      }

      MatrixNd U = new MatrixNd (nrows, ucols);
      MatrixNd V = new MatrixNd (ncols, vcols);
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
      MatrixNd S = new MatrixNd(ucols, vcols);
      S.setDiagonal (sig);
      MatrixNd US = new MatrixNd (U);
      US.mul (S);
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
                     // equality for case of 0 singular value
                     Math.abs (sig.get (j) - svals[i]) >= svals[i] * EPSILON) {
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

      // check vector solve
      VectorNd b = new VectorNd (nrows);
      for (int i = 0; i < nrows; i++) {
         b.set (i, RandomGenerator.get().nextDouble() - 0.5);
      }
      VectorNd x = new VectorNd (ncols);

      if (nrows == ncols && svd.rank (EPSILON) == nrows) {
         // square non-singular system
         svd.solve (x, b);
         VectorNd chk = new VectorNd (nrows);
         chk.mul (M1, x);
         if (!chk.epsilonEquals (b, EPSILON * cond)) {
            throw new TestException (
               "solution failed:\n" + "chk="
               + chk.toString ("%9.4f") + " b=" + b.toString ("%9.4f") + " x="
               + x.toString ("%9.4f"));
         }
      }
      else {
         // non square and/or rank deficient system
         svd.solve (x, b, EPSILON);

         VectorNd chk = new VectorNd (ncols);
         int r = svd.rank(EPSILON);
         for (int k=0; k<r; k++) {
            VectorNd ucol = new VectorNd (nrows);
            VectorNd vcol = new VectorNd (ncols);
            U.getColumn (k, ucol);
            V.getColumn (k, vcol);
            chk.scaledAdd (ucol.dot(b)/sig.get(k), vcol);
         }
         if (!chk.epsilonEquals (x, 1e-12)) {
            throw new TestException (
               "solution failed:\n" + "chk="
               + chk.toString ("%12.7f") + " x=" + x.toString ("%12.7f"));
         }
      }

      // check matrix solver
      MatrixNd B = new MatrixNd (nrows, 3);
      B.setRandom();
      MatrixNd X = new MatrixNd (ncols, 3);

      if (nrows == ncols && svd.rank (EPSILON) == nrows) {
         // square non-singular system
         svd.solve (X, B);
         MatrixNd MX = new MatrixNd (nrows, 3);
         MX.mul (M1, X);
         if (!MX.epsilonEquals (B, EPSILON * cond)) {
            throw new TestException ("solution failed:\n" + "MX="
            + MX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }
      }
      else if (nrows == ncols) {
         // non square and/or rank deficient system
         svd.solve (X, B, EPSILON);
         int r = svd.rank(EPSILON);
         MatrixNd Xchk = new MatrixNd (ncols, 3);
         MatrixNd Ur = new MatrixNd (nrows, r);
         U.getSubMatrix (0, 0, Ur);
         MatrixNd Vr = new MatrixNd (ncols, r);
         V.getSubMatrix (0, 0, Vr);
         MatrixNd Sinv = new MatrixNd (r, r);
         for (int i=0; i<r; i++) {
            Sinv.set (i, i, 1/sig.get(i));
         }
         Xchk.mulTransposeLeft (Ur, B);
         Xchk.mul (Sinv, Xchk);
         Xchk.mul (Vr, Xchk);
         if (!Xchk.epsilonEquals (X, 1e-12)) {
            throw new TestException ("solution failed:\n" + "Xchk=\n"
            + Xchk.toString ("%9.4f") + "X=\n" + X.toString ("%9.4f"));
         }
      }

      // // check left vector solve
      // b = new VectorNd (ncols);
      // for (int i = 0; i < ncols; i++) {
      //    b.set (i, RandomGenerator.get().nextDouble() - 0.5);
      // }
      // x = new VectorNd (nrows);

      // if (nrows == ncols && svd.rank (EPSILON) == nrows) {
      //    // square non-singular system
      //    svd.leftSolve (x, b);
      //    VectorNd chk = new VectorNd (nrows);
      //    chk.mulTranspose (M1, x);
      //    if (!chk.epsilonEquals (b, EPSILON * cond)) {
      //       throw new TestException (
      //          "solution failed:\n" + "chk="
      //          + chk.toString ("%9.4f") + " b=" + b.toString ("%9.4f") + " x="
      //          + x.toString ("%9.4f"));
      //    }
      // }
      // else {
      //    // non square and/or rank deficient system
      //    svd.leftSolve (x, b, EPSILON);
      //    VectorNd chk = new VectorNd (nrows);
      //    int r = svd.rank(EPSILON);
      //    for (int k=0; k<r; k++) {
      //       VectorNd ucol = new VectorNd (nrows);
      //       VectorNd vcol = new VectorNd (ncols);
      //       U.getColumn (k, ucol);
      //       V.getColumn (k, vcol);
      //       chk.scaledAdd (vcol.dot(b)/sig.get(k), ucol);
      //    }
      //    if (!chk.epsilonEquals (x, 1e-12)) {
      //       throw new TestException (
      //          "solution failed:\n" + "chk="
      //          + chk.toString ("%12.7f") + " x=" + x.toString ("%12.7f"));
      //    }
      // }

      // // check left matrix solve
      // B = new MatrixNd (3, ncols);
      // B.setRandom();
      // X = new MatrixNd (3, nrows);

      // if (nrows == ncols && svd.rank (EPSILON) == nrows) {
      //    // square non-singular system
      //    svd.leftSolve (X, B);
      //    MatrixNd XM = new MatrixNd (3, nrows);
      //    XM.mul (X, M1);
      //    if (!XM.epsilonEquals (B, EPSILON * cond)) {
      //       throw new TestException ("solution failed:\n" + "XM="
      //       + XM.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
      //    }
      // }
      // else if (nrows == ncols) {
      //    // non square and/or rank deficient system
      //    svd.leftSolve (X, B, EPSILON);
      //    int r = svd.rank(EPSILON);
      //    MatrixNd Xchk = new MatrixNd (3, nrows);
      //    MatrixNd Ur = new MatrixNd (nrows, r);
      //    U.getSubMatrix (0, 0, Ur);
      //    MatrixNd Vr = new MatrixNd (ncols, r);
      //    V.getSubMatrix (0, 0, Vr);
      //    MatrixNd Sinv = new MatrixNd (r, r);
      //    for (int i=0; i<r; i++) {
      //       Sinv.set (i, i, 1/sig.get(i));
      //    }
      //    Xchk.mul (B, Vr);
      //    Xchk.mul (Sinv);
      //    Xchk.mulTransposeRight (Xchk, Ur);
      //    if (!Xchk.epsilonEquals (X, 1e-12)) {
      //       throw new TestException ("solution failed:\n" + "Xchk=\n"
      //       + Xchk.toString ("%9.4f") + "X=\n" + X.toString ("%9.4f"));
      //    }
      // }

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
         if (svd.inverse (MI)) {
            IMI.mul (M1, MI);
            MatrixNd I = new MatrixNd (n, n);
            I.setIdentity();
            
            if (!IMI.epsilonEquals (I, EPSILON * cond)) {
               throw new TestException (
                  "failed inverse:\n"
                  + MI.toString ("%9.4f") + "M1=\n" + M1.toString ("%9.4f"));
            }
         }
      }
   }

   public void test() {
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
      testDecomposition (5, 3);
      testDecomposition (3, 4);
      testDecomposition (3, 5);
      testDecomposition (3, 3, new double[] { 1, 0.0001, 0 });
      testDecomposition (3, 4, new double[] { 1, 2, 0 });
      testDecomposition (4, 5, new double[] { 1, 1, 0, 0 });
      testDecomposition (4, 6, new double[] { 1.23, 5.3, 0, 0 });
      testDecomposition (4, 6, new double[] { 1.23, 0, 0, 0 });
      testDecomposition (6, 4, new double[] { 1.23, 5.3, 0, 0 });
      testDecomposition (6, 4, new double[] { 1.23, 0, 0, 0 });
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
      
      // full matrices
      testDecomposition (
         6, 5, new double[] { 1.1, 2.2, 4.4, 0.0003, 0 },
         SVDecomposition.FULL_UV);
      testDecomposition (4, 3, SVDecomposition.FULL_UV);
      testDecomposition (3, 4, SVDecomposition.FULL_UV);
      testDecomposition (
         3, 4, new double[] { 1, 2, 0 }, SVDecomposition.FULL_UV);
      testDecomposition (
         4, 5, new double[] { 1, 1, 0, 0 }, SVDecomposition.FULL_UV);
      testDecomposition (
         6, 5, new double[] { 1.1, 2.2, 4.4, 0.0003, 0 },
         SVDecomposition.FULL_UV);
      testDecomposition (
         5, 6, new double[] { 12, 13, 14, 0.0003, 0 }, SVDecomposition.FULL_UV);
      
      for (int i = 1; i < 10; i++) {
         for (int j=1; j < 10; j++) {
            testDecomposition (i, j, SVDecomposition.FULL_UV);
         }
      }
      
      // pseudo-inverse
      for (int i = 1; i < 10; i++) {
         for (int j=1; j < 10; j++) {
            testInversion (i, j, 0);
            testInversion (i, j, SVDecomposition.FULL_UV);
         }
      }
   }

   public void checkTiming (int nr, int nc) {
      int nd = Math.min (nr, nc);

      MatrixNd M1 = new MatrixNd (nr, nc);
      M1.setRandom();

      MatrixNd U = new MatrixNd (nr, nd);
      MatrixNd V = new MatrixNd (nc, nd);
      VectorNd s = new VectorNd (nd);

      SVDecomposition svd = new SVDecomposition ();

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
      tester.runtest();
   }
}
