/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.UnitTest;

class EigenDecompositionTest extends UnitTest {
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPS = 10000 * DOUBLE_PREC;
   private static int OMIT_V = EigenDecomposition.OMIT_V;

   EigenDecomposition evd = new EigenDecomposition ();

   static boolean isOrthogonal (MatrixNd M) {
      if (M.nrows >= M.ncols) {
         VectorNd col1 = new VectorNd (M.nrows);
         VectorNd col2 = new VectorNd (M.nrows);
         for (int j = 0; j < M.ncols; j++) {
            M.getColumn (j, col1);
            if (Math.abs (col1.norm() - 1) > EPS) {
               return false;
            }
            for (int k = j + 1; k < M.ncols; k++) {
               M.getColumn (k, col2);
               if (Math.abs (col1.dot (col2)) > EPS) {
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
            if (Math.abs (row1.norm() - 1) > EPS) {
               return false;
            }
            for (int k = i + 1; k < M.nrows; k++) {
               M.getRow (k, row2);
               if (Math.abs (row1.dot (row2)) > EPS) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   public void testDecomposition (int n) {
      testDecomposition (n, /*flags=*/0);
   }

   public void testDecomposition (int n, int flags) {
      MatrixNd M1 = new MatrixNd (n, n);
      M1.setRandom();
      testUnsymmetric (M1, flags, null, null);
      //make the matrix symmetric
      for (int i=0; i<n; i++) {
         for (int j=i+1; j<n; j++) {
            M1.set (j, i, M1.get(i,j));
         }
      }
      testDecomposition (M1, flags);
   }

   public void testDecomposition (int n, double[] evals) {
      MatrixNd M1 = new MatrixNd (n, n);
      MatrixNd V = new MatrixNd (n, n);
      V.setRandomOrthogonal (RandomGenerator.get());
      M1.set (V);
      M1.mulDiagonalRight (evals);
      M1.mulTranspose (V);
      testSymmetric (M1, /*flags=*/0, evals);
      testUnsymmetric (M1, /*flags=*/0, evals, new double[n]);
   }

   public void testDecomposition (int n, double[] rvals, double[] ivals) {
      MatrixNd M1 = new MatrixNd (n, n);
      MatrixNd V = new MatrixNd (n, n);
      MatrixNd D = new MatrixNd (n, n);
      V.setRandomOrthogonal (RandomGenerator.get());
      M1.set (V);
      for (int i=0; i<n; i++) {
         if (ivals[i] == 0) {
            D.set (i, i, rvals[i]);
         }
         else {
            D.set (i, i, rvals[i]);
            D.set (i+1, i, ivals[i+1]);
            D.set (i, i+1, ivals[i]);
            D.set (i+1, i+1, rvals[i+1]);
            i++;
         }
      }
      M1.mul (D);
      M1.mulTranspose (V);
      testUnsymmetric (M1, /*flags=*/0, rvals, ivals);
   }

   public void testDecomposition (MatrixNd M1, int flags) {
      testSymmetric (M1, flags, null);
      testUnsymmetric (M1, flags, null, null);
   }

   public void testDecomposition (MatrixNd M1) {
      testDecomposition (M1, /*flags=*/0);
   }

   protected boolean pairIsConjugate (
      VectorNd er, VectorNd ei, int i, double tol) {
      
      double ar = er.get(i);
      double br = er.get(i+1);
      double ai = ei.get(i);
      double bi = ei.get(i+1);
      return (Math.abs(ar-br) <= tol && Math.abs(ai+bi) <= tol);
   }

   public void testUnsymmetric (
      MatrixNd M1, int flags, double[] rvals, double[] ivals) {
      evd.factor (M1, /*flags=*/0);

      int n = M1.nrows;

      MatrixNd V = new MatrixNd (n, n);
      VectorNd er = new VectorNd (n);
      VectorNd ei = new VectorNd (n);

      evd.get (er, ei, V);

      // verify product
      MatrixNd D = new MatrixNd (n, n);
      evd.getD (D);

      MatrixNd MV = new MatrixNd (n, n);
      MV.mul (M1, V);
      MatrixNd VD = new MatrixNd (n, n);
      VD.mul (V, D);

      double tol = Math.max(EPS, Math.abs(evd.getMaxAbsEig())*EPS);
      if (rvals != null && ivals != null) {
         boolean[] taken = new boolean[n];
         for (int i=0; i<n; i++) {
            int j;
            for (j=0; j<n; j++) {
               if (!taken[j] &&
                   Math.abs (er.get(j)-rvals[i]) <= tol &&
                   Math.abs (ei.get(j)-ivals[i]) <= tol) {
                  taken[j] = true;
                  break;
               }
            }
            if (j == n) {
               throw new TestException (
                  "eigenvalue "+rvals[i]+","+ivals[i]+" not computed");
            }
         }
      }

      // System.out.println ("MP=\n" +MP.toString ("%12.7f"));
      // System.out.println ("eps=" + EPS * evd.norm());
      if (!VD.epsilonEquals (MV, tol)) {
         MatrixNd ERR = new MatrixNd (VD);
         ERR.sub (MV);
         System.out.println ("M1=\n" + M1.toString("%12.7f"));
         System.out.println ("V=\n" + V.toString("%12.7f"));
         System.out.println ("D=\n" + D.toString("%12.7f"));
         System.out.println ("er=\n" + er.toString("%12.7f"));
         System.out.println ("ei=\n" + ei.toString("%12.7f"));
         throw new TestException (
            "V D = \n" + VD.toString ("%9.4f")
            + "expecting:\n" + MV.toString ("%9.4f") + "error="
            + ERR.infinityNorm() + " tol=" + tol);
      }
      VD.sub (MV);

      // check determinant
      double det = getDeterminant (M1);
      tol = getDeterminantTol (evd, er, ei);
      if (Math.abs (det-evd.determinant()) > tol) {
         System.out.println ("max=" + evd.getMaxAbsEig());
         System.out.println ("er=\n" + er);
         System.out.println ("ei=\n" + ei);
         System.out.println ("min=" + evd.getMinAbsEig());
         System.out.println ("M=\n" + M1.toString ("%12.7f"));
         throw new TestException (
            "determinant failed: got "
            + evd.determinant() + " expected " + det + " tol=" + tol);
      }

      if ((flags & OMIT_V) != 0) {
         EigenDecomposition evdx = new EigenDecomposition ();
         evdx.factor (M1, OMIT_V);
         tol = 0; // should get exactly the same results
         if (!evdx.getEigReal().epsilonEquals (evd.getEigReal(), tol) ||
             !evdx.getEigImag().epsilonEquals (evd.getEigImag(), tol)) {
            throw new TestException (
               "Got different eigenvalues when computing without V:\n" +
               evdx.getEigReal() + "\n" + evdx.getEigImag() +
               "\nExpected:\n" + evd.getEigReal() + "\n" + evd.getEigImag());
         }
      }
   }

   protected double getDeterminant (MatrixNd M) {
      int n = M.rowSize();
      if (n <= 3) {
         if (n == 1) {
            return M.get (0, 0);
         }
         else if (n == 2) {
            return M.get (0, 0) * M.get (1, 1) - M.get (0, 1) * M.get (1, 0);
         }
         else { // n == 3
            return 
               M.get(0,0)*M.get(1,1)*M.get(2,2) +
               M.get(0,1)*M.get(1,2)*M.get(2,0) +
               M.get(0,2)*M.get(1,0)*M.get(2,1) -
               M.get(0,2)*M.get(1,1)*M.get(2,0) -
               M.get(0,0)*M.get(1,2)*M.get(2,1) -
               M.get(0,1)*M.get(1,0)*M.get(2,2);
         } 
      }
      else {
         LUDecomposition lud = new LUDecomposition (M);
         return lud.determinant();
      }
   }

   protected double getDeterminantTol (
      EigenDecomposition evd, VectorNd er, VectorNd ei) {
      double max = evd.getMaxAbsEig();
      double eps = max*EPS;
      int n = er.size();
      double mag = 1;
      double tol = eps;
      for (int i=0; i<n; i++) {
         double re = er.get(i);
         double im = (ei == null ? 0 : ei.get(i));
         double abs = Math.sqrt (re*re + im*im);
         tol = tol*abs + mag*eps;
         mag *= abs;
      }
      return tol;
   }

   public void testSymmetric (MatrixNd M1, int flags, double[] evals) {
      evd.factorSymmetric (M1, /*flags=*/0);

      int n = M1.nrows;

      // form the symmetric part of M1
      MatrixNd MS = new MatrixNd (M1);
      MS.transpose();
      MS.add (M1);
      MS.scale (0.5);      

      MatrixNd V = new MatrixNd (n, n);
      VectorNd eig = new VectorNd (n);

      evd.get (eig, V);

      if (!isOrthogonal (V)) {
         throw new TestException ("V not orthogonal:\n" + V.toString ("%9.5f"));
      }

      // verify product

      double cond = evd.condition();

      MatrixNd D = new MatrixNd (n, n);
      evd.getD (D);
      MatrixNd MP = new MatrixNd (V);
      MP.mul (D);
      MP.mulTranspose (V);
      // System.out.println ("MP=\n" +MP.toString ("%12.7f"));
      // System.out.println ("eps=" + EPS * evd.norm());

      if (!MP.epsilonEquals (MS, EPS * evd.norm())) {
         MatrixNd ERR = new MatrixNd (MP);
         ERR.sub (MS);
         throw new TestException (
            "V D V' = \n" + MP.toString ("%9.4f")
            + "expecting:\n" + MS.toString ("%9.4f") + "error="
            + ERR.infinityNorm());
      }

      double tol = Math.max(EPS, Math.abs(evd.getMaxAbsEig())*EPS);
      if (evals != null) {
         boolean[] taken = new boolean[n];
         for (int i=0; i<n; i++) {
            int j;
            for (j=0; j<n; j++) {
               if (!taken[j] &&
                   Math.abs (eig.get(j)-evals[i]) <= tol) {
                  taken[j] = true;
                  break;
               }
            }
            if (j == n) {
               throw new TestException (
                  "eigenvalue " + evals[i] + " not computed");
            }
         }
      }

      // check vector solver
      VectorNd b = new VectorNd (n);
      for (int i = 0; i < n; i++) {
         b.set (i, RandomGenerator.get().nextDouble() - 0.5);
      }
      VectorNd x = new VectorNd (n);
      VectorNd Mx = new VectorNd (n);
      if (evd.solve (x, b)) {
         Mx.mul (MS, x);
         if (!Mx.epsilonEquals (b, EPS * cond)) {
            throw new TestException (
               "solution failed:\n" + "Mx="
               + Mx.toString ("%9.4f") + "b=" + b.toString ("%9.4f") + "x="
               + x.toString ("%9.4f"));
         }
      }
      

      // check matrix solver
      MatrixNd B = new MatrixNd (n, 3);
      B.setRandom();
      MatrixNd X = new MatrixNd (n, 3);
      MatrixNd MX = new MatrixNd (n, 3);

      if (evd.solve (X, B)) {
         MX.mul (MS, X);
         if (!MX.epsilonEquals (B, EPS * cond)) {
            throw new TestException (
               "solution failed:\n" + "MX="
               + MX.toString ("%9.4f") + "B=" + B.toString ("%9.4f"));
         }
      }      

      // check determinant
      double det = getDeterminant (MS);
      tol = getDeterminantTol (evd, eig, null);
      if (Math.abs (det-evd.determinant()) > tol) {
         System.out.println ("MS=\n" + MS.toString ("%12.7f"));
         System.out.println ("eig=\n" + eig);
         throw new TestException (
            "determinant failed: got "
            + evd.determinant() + " expected " + det + " tol=" + tol);
      }

      // check inverse
      MatrixNd MI = new MatrixNd (n, n);
      MatrixNd IMI = new MatrixNd (n, n);
      if (evd.inverse (MI)) {
         IMI.mul (MS, MI);
         MatrixNd I = new MatrixNd (n, n);
         I.setIdentity();
         
         if (!IMI.epsilonEquals (I, EPS * cond)) {
            throw new TestException (
               "failed inverse:\n"
               + MI.toString ("%9.4f") + "MS=\n" + MS.toString ("%9.4f"));
         }
      }

      if ((flags & OMIT_V) != 0) {
         EigenDecomposition evdx = new EigenDecomposition ();
         evdx.factorSymmetric (M1, OMIT_V);
         tol = 0; // should get exactly the same result
         if (!evdx.getEigReal().epsilonEquals (evd.getEigReal(), tol)) {
            throw new TestException (
               "Got different eigenvalues when computing without V:\n" +
               evdx.getEigReal() + "\nExpected:\n" + evd.getEigReal());
         }
      }
   }

   public void test() {
      RandomGenerator.setSeed (0x1234);

      testDecomposition (
         new MatrixNd (3, 3, new double[] { 1, 0, 0, 0, 2, 0, 0, 0, 3 }));
      testDecomposition (
         new MatrixNd (3, 3, new double[] {-1, 0, 0, 0, 2, 0, 0, 0,-3 }));

      testDecomposition (
         new MatrixNd (2, 2, new double[] { 1, 2, 2, 3 }));
      testDecomposition (
         new MatrixNd (2, 2, new double[] { 0, 0, 0, 0 }));
      testDecomposition (
         new MatrixNd (2, 2, new double[] { 0, Math.PI, Math.PI, 0 }));

      testDecomposition (
         new MatrixNd (2, 2, new double[] { 1, -2, -2, 5 }));

      evd.debug = true;

      testDecomposition (
         new MatrixNd (3, 3, new double[] { 1, 2, 3, 2, 4, 5, 3, 5, 6 }));
      evd.debug = false;

      testDecomposition (2, new double[] { 1, 0 });
      testDecomposition (2, new double[] { 1, 0.0000000001 });
      testDecomposition (2, new double[] { 1, -0.0000000001 });
      testDecomposition (2, new double[] { -1, -0.0000000001 });

      testDecomposition (
         2, new double[] { 1, 1 }, new double[] { -1, 1 });         
      testDecomposition (
         4,
         new double[] { 3, 2, 2, 0},
         new double[] { 0, -0.01, 0.01, 0});

      testDecomposition (
         5, 
         new double[] { 3, 5, 5, 0, 0},
         new double[] { 0, -0.0001, 0.0001, 0, 0 });         

      testDecomposition (
         7, 
         new double[] { 123, 7, 7, 0.00001, 0.00001, 4, 0},
         new double[] { 0, -0.0001, 0.0001, -5, 5, 0, 0 });         

      testDecomposition (3, new double[] { 1, 0.0001, 0 });
      testDecomposition (10, new double[] {
            123, 45, 2, 1, 1, 1, 0.1, 0.01, 0.01, 0.01});
      testDecomposition (6);
      testDecomposition (6, new double[] { 1.1, 2.2, 3.3, 0.0001, 0, 0 });
      testDecomposition (6, new double[] { 1.1, 2.2, 3.3, 0.0001, 0, 0 });
      testDecomposition (6, new double[] { 1.1, -2.2, 3.3, -0.0001, 0, 0 });

      for (int i = 0; i < 100; i++) {
         testDecomposition (7);
         testDecomposition (5);
         testDecomposition (4);
         testDecomposition (3);
         testDecomposition (2);
         testDecomposition (1);
      }

      int flags = OMIT_V;
      // make sure things work without V
      for (int i = 0; i < 10; i++) {
         testDecomposition (7, flags);
         testDecomposition (4, flags);
         testDecomposition (2, flags);
         testDecomposition (1, flags);
      }

      for (int i = 0; i < 10; i++) {
         testDecomposition (20);
         testDecomposition (100);
      }
      
   }

   public void tryit1() {
      MatrixNd M = new MatrixNd(4, 4);
      M.set (new double[] {
       0.421761282626275, 0.655740699156587, 0.678735154857773, 0.655477890177557,
       0.915735525189067, 0.035711678574190, 0.757740130578333, 0.171186687811562,
       0.792207329559554, 0.849129305868777, 0.743132468124916, 0.706046088019609,
       0.959492426392903, 0.933993247757551, 0.392227019534168, 0.031832846377421
         });
      tryit (M);
   }

   public void tryit2() {
      MatrixNd M = new MatrixNd(4, 4);
      M.set (new double[] {
            1, 0, 0, 0, 
            0, 2, -1, 0,
            0, -1.00000001, 2, 0,
            0, 0, 0, 3
         });
      tryit (M);
   }

   public void tryit (MatrixNd M) {
      int n = M.rowSize();
      EigenDecomposition eigd = new EigenDecomposition();
      eigd.factor (M);
      VectorNd eig = new VectorNd(n);
      VectorNd img = new VectorNd(n);
      MatrixNd V = new MatrixNd(n,n);
      eigd.get (eig, img, V);
      System.out.println ("eig=\n" + eig.toString ("%12.7f"));
      System.out.println ("img=\n" + img.toString ("%12.7f"));
      System.out.println ("V=\n" + V.toString ("%12.7f"));
   }

   public static void main (String[] args) {
      EigenDecompositionTest tester = new EigenDecompositionTest();

      //tester.execute();
      //tester.tryit2();
      tester.runtest();

   }
}
