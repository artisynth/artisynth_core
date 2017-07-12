/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;
import java.util.*;

class SymmetricMatrix3dTest {
   Random randGen = new Random();

   public SymmetricMatrix3dTest() {
      randGen.setSeed (0x1234);
   }

   private double orthogonalityError (Matrix3d U) {
      Matrix3d P = new Matrix3d();
      P.mulTransposeRight (U, U);
      P.m00 -= 1;
      P.m11 -= 1;
      P.m22 -= 1;
      return P.infinityNorm();
   }

   public void doTest (SymmetricMatrix3d M) {
      Matrix3d U = new Matrix3d();
      Matrix3d V = new Matrix3d();
      Vector3d s = new Vector3d();
      Vector3d e = new Vector3d();
      Matrix3d P = new Matrix3d();
      Matrix3d S = new Matrix3d();
      Matrix3d eV = new Matrix3d();

      double maxError = 0;
      double maxOrthoError = 0;

      M.getSVD (U, s, V);

      S.setDiagonal (s);
      P.mulTransposeRight (S, V);
      P.mul (U, P);

      if (!P.epsilonEquals (M, 1e-9)) {
         throw new TestException ("Bad svd for M=\n" + M.toString ("%12.9f")
         + "U=\n" + U.toString ("%12.9f") + "s=\n" + s.toString ("%12.9f")
         + "V=\n" + V.toString ("%12.9f"));
      }
      P.sub (M);
      double err = P.infinityNorm();
      if (err > maxError) {
         maxError = err;
      }
      err = orthogonalityError (U);
      if (err > maxOrthoError) {
         maxOrthoError = err;
      }
      err = orthogonalityError (V);
      if (err > maxOrthoError) {
         maxOrthoError = err;
      }

      M.getEigenValues (e, V);
      P.mul (M, V);
      eV.set (V);
      eV.mulCols (e);
      if (!P.epsilonEquals (eV, 1e-9)) {
         throw new TestException ("Bad eig values for M=\n"
         + M.toString ("%12.9f") + "e=\n" + e.toString ("%12.9f") + "V=\n"
         + V.toString ("%12.9f"));
      }
   }

   public void test() {
      // test the SVD stuff
      SymmetricMatrix3d M = new SymmetricMatrix3d();

      M.setIdentity();
      doTest (M);
      M.set (0, 0, 0, 0, 0, 0);
      doTest (M);
      for (int i = 0; i < 10; i++) {
         M.setRandom (-0.5, 0.5, randGen);
         M.m01 = 0;
         M.m02 = 0;
         M.m10 = 0;
         M.m20 = 0;
         doTest (M);
      }
      for (int i = 0; i < 10; i++) {
         M.setRandom (-0.5, 0.5, randGen);
         M.m02 = 0;
         M.m12 = 0;
         M.m20 = 0;
         M.m21 = 0;
         doTest (M);
      }
      for (int i = 0; i < 20; i++) {
         MatrixNd R = new MatrixNd (3, 3);
         Matrix3d A = new Matrix3d();
         R.setRandomSvd (new double[] { 4, 1, 0.0000001 });
         A.set (R);
         M.mulTransposeLeft (A);
         doTest (M);
         R.setRandomSvd (new double[] { 4, 0.000001, 0 });
         A.set (R);
         M.mulTransposeLeft (A);
         doTest (M);
         R.setRandomSvd (new double[] { 2, 0, 0 });
         A.set (R);
         M.mulTransposeLeft (A);
         doTest (M);
      }

      int numtests = 100;
      for (int i = 0; i < numtests; i++) {
         M.setRandom (-0.5, 0.5, randGen);
         doTest (M);
      }
   }

   public void timing() {
      // test the SVD stuff
      SymmetricMatrix3d M = new SymmetricMatrix3d();
      Matrix3d U = new Matrix3d();
      Matrix3d V = new Matrix3d();
      Vector3d s = new Vector3d();
      SVDecomposition svd = new SVDecomposition ();

      FunctionTimer timer1 = new FunctionTimer();
      FunctionTimer timer2 = new FunctionTimer();
      int loopCnt = 10000;
      int caseCnt = 20;
      for (int i = 0; i < caseCnt; i++) {
         M.setRandom (-0.5, 0.5, randGen);
         timer1.restart();
         for (int k = 0; k < loopCnt; k++) {
            M.getSVD (U, s, V);
         }
         timer1.stop();

         timer2.restart();
         for (int k = 0; k < loopCnt; k++) {
            svd.factor (M);
         }
         timer2.stop();
      }
      System.out.println (
         "fast symmetric 3x3 SVD: " + timer1.resultUsec (loopCnt * caseCnt));
      System.out.println (
         "regular svd for 3x3: " + timer2.resultUsec (loopCnt * caseCnt));
   }

   public static void main (String[] args) {
      boolean dotiming = false;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            dotiming = true;
         }
         else {
            System.err.println (
               "Usage: java maspack.matrix.SymmetricMatrix3dTest [-timing]");
         }
      }
      SymmetricMatrix3dTest tester = new SymmetricMatrix3dTest();
      try {
         if (dotiming) {
            tester.timing();
         }
         else {
            tester.test();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
