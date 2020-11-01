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

class SVDecomposition3dTest {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 200 * DOUBLE_PREC;

   SVDecomposition3d svd = new SVDecomposition3d();

   static boolean isOrthogonal (Matrix3d M) {
      Matrix3d P = new Matrix3d();
      Matrix3d I = new Matrix3d();
      P.mulTransposeRight (M, M);
      I.setIdentity();
      return I.epsilonEquals (P, EPSILON);
   }

   public void testDecomposition() {
      Matrix3d M1 = new Matrix3d();
      M1.setRandom();
      testDecomposition (M1, null);
   }

   public void testDecomposition (double[] svals) {
      MatrixNd MR = new MatrixNd (3, 3);
      MR.setRandomSvd (svals);
      Matrix3d M1 = new Matrix3d();
      M1.set (MR);
      testDecomposition (M1, null);
   }

   public void testPolarDecomposition (Matrix3d M) {

      RotationMatrix3d R = new RotationMatrix3d();
      Matrix3d P = new Matrix3d();
      Matrix3d Psym = new Matrix3d();
      Matrix3d T = new Matrix3d();

      svd.polarDecomposition (R, P, M);
      T.mul (R, P);
      if (!T.epsilonEquals (M, EPSILON)) {
         throw new TestException (
            "Right polar decomposition failed: R=\n" + R + "P=\n" + P);
      }
      svd.polarDecomposition (R, Psym, M);
      T.mul (R, Psym);
      if (!T.epsilonEquals (M, EPSILON)) {
         throw new TestException (
            "Right polar decomposition failed: R=\n" + R + "Psym=\n" + Psym);
      }
      
      svd.leftPolarDecomposition (P, R, M);
      T.mul (P, R);
      if (!T.epsilonEquals (M, EPSILON)) {
         throw new TestException (
            "Left polar decomposition failed: P=\n" + P + "R=\n" + R);
      }
      svd.leftPolarDecomposition (Psym, R, M);
      T.mul (Psym, R);
      if (!T.epsilonEquals (M, EPSILON)) {
         throw new TestException (
            "Left polar decomposition failed: Psym=\n" + Psym + "R=\n" + R);
      }
      
   }

   public void testDecomposition (Matrix3d M1, double[] svals) {
      svd.factor (M1);
      int nrows = 3;
      int ncols = 3;
      int mind = 3;

      Matrix3d U = svd.getU();
      Matrix3d V = svd.getV();
      Vector3d sig = new Vector3d();
      svd.getS (sig);

      //System.out.println ("sig="+sig);

      if (!isOrthogonal (U)) {
         throw new TestException ("U not orthogonal:\n" + U.toString ("%9.4f"));
      }
      if (!isOrthogonal (V)) {
         throw new TestException ("V not orthogonal:\n" + V.toString ("%9.4f"));
      }

      // verify product

      double cond = svd.condition();

      Matrix3d US = new Matrix3d();
      US.set (U);
      US.mulCols (sig);
      Matrix3d MP = new Matrix3d();
      MP.mulTransposeRight (US, V);

      if (!MP.epsilonEquals (M1, EPSILON * svd.norm())) {
         Matrix3d ER = new Matrix3d();
         ER.sub (MP, M1);
         throw new TestException ("U S V' = \n" + MP.toString ("%9.4f")
         + "expecting:\n" + M1.toString ("%9.4f") + "error="
         + ER.infinityNorm());
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
         Vector3d b = new Vector3d();
         for (int i = 0; i < nrows; i++) {
            b.set (i, RandomGenerator.get().nextDouble() - 0.5);
         }
         Vector3d x = new Vector3d();
         Vector3d Mx = new Vector3d();
         if (svd.solve (x, b)) {
            Mx.mul (M1, x);
            if (!Mx.epsilonEquals (b, EPSILON * cond)) {
               throw new TestException (
                  "solution failed:\n" + "Mx="
                  + Mx.toString ("%9.4f") + "b=" + b.toString ("%9.4f") + "x="
                  + x.toString ("%9.4f"));
            }
         }
      }

      // // check matrix solver
      // if (nrows == ncols)
      // {
      // MatrixNd B = new MatrixNd(nrows,3);
      // B.setRandom();
      // MatrixNd X = new MatrixNd(ncols,3);
      // MatrixNd MX = new MatrixNd(nrows,3);

      // svd.solve (X, B);
      // MX.mul (M1, X);
      // if (!MX.epsilonEquals (B, EPSILON*cond))
      // { throw new TestException (
      // "solution failed:\n" +
      // "MX=" + MX.toString("%9.4f") +
      // "B=" + B.toString("%9.4f"));
      // }
      // }

      // check determinant
      {
         double det =
            M1.get (0, 0) * M1.get (1, 1) * M1.get (2, 2) + M1.get (0, 1)
            * M1.get (1, 2) * M1.get (2, 0) + M1.get (0, 2) * M1.get (1, 0)
            * M1.get (2, 1) - M1.get (0, 2) * M1.get (1, 1) * M1.get (2, 0)
            - M1.get (0, 0) * M1.get (1, 2) * M1.get (2, 1) - M1.get (0, 1)
            * M1.get (1, 0) * M1.get (2, 2);

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
         Matrix3d MI = new Matrix3d();
         Matrix3d IMI = new Matrix3d();
         if (svd.inverse (MI)) {
            IMI.mul (M1, MI);
            Matrix3d I = new Matrix3d();
            I.setIdentity();
            
            if (!IMI.epsilonEquals (I, EPSILON * cond)) {
               throw new TestException (
                  "failed inverse:\n"
                  + MI.toString ("%9.4f") + "M1=\n" + M1.toString ("%9.4f"));
            }
         }
      }
   }

   public void execute() {
      RandomGenerator.setSeed (0x1234);

      testDecomposition (new Matrix3d (new double[] { 0.46914823730452665,
                                                     -2.009927412169458,
                                                     0.8481049304975272,
                                                     0.41645167964478735,
                                                     -1.639935747542862,
                                                     -1.090233210860603, 0.0,
                                                     0.0, 0.0 }), null);

      testDecomposition (new Matrix3d (
         new double[] { 0, 1, 0, 0, 2, 2, 0, 0, 3 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 1, 0, 0, 0, 2, 0, 0, 0, 3 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 1, 0, 0, 0, 2, 0, 0, 0, 0 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 1, 0, 0, 0, 0, 0, 0, 0, 0 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 0, 0, 0, 0, 2, 3, 0, 0, 4 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 0, 1, 1, 1, 0, 0, 0, 0, 0 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 0, 1, 0, 0, 2, 2, 0, 0, 3 }), null);
      testDecomposition (new Matrix3d (
         new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }), null);

      testDecomposition (new Matrix3d (new double[] { 0.46914823730452665,
                                                     -2.009927412169458,
                                                     0.8481049304975272,
                                                     0.41645167964478735,
                                                     -1.639935747542862,
                                                     -1.090233210860603, 0.0,
                                                     0.0, 0.0 }), null);
      testDecomposition (new Matrix3d (new double[] { 2.2773016205484105,
                                                     -0.31338266946551563,
                                                     0.6778778092657844,
                                                     -0.8388186812067157,
                                                     -1.104692980287434,
                                                     -0.3719235007583789, 0, 0,
                                                     0 }), null);
      testDecomposition (new Matrix3d (new double[] { 1.7271458646290014,
                                                     -0.3070131535540012,
                                                     -0.3861866812734839,
                                                     -0.6113434028645098,
                                                     -1.8807249610714911,
                                                     -0.011968911164261908, 0,
                                                     0, 0 }), null);
      testDecomposition (new Matrix3d (new double[] { 2.033319309140049,
                                                     -0.25028611817089313,
                                                     -0.3385028472987217,
                                                     -0.5266982813470501,
                                                     -2.05763587270708,
                                                     -0.0968065248015208, 0.0,
                                                     0.0, 0.0 }), null);

      testDecomposition (new Matrix3d (new double[] { 1.9885969788993956,
                                                     -0.5060050726003058,
                                                     0.48307368305754705,
                                                     -0.3752012250425878,
                                                     -1.6807108448715908,
                                                     -0.17451750023680307, 0.0,
                                                     0.0, 0.0 }), null);

      testDecomposition (new Matrix3d (new double[] {
               0.10226, -0.0126665, -0.07014 ,
               -0.0126665, 0.10226, 0.07014,
               -0.07014, 0.07014, -0.20556 }), null);

//       for (int i = 0; i < 1000; i++) {
//          testDecomposition (new double[] { 1, 0.0001, 0 });
//          testDecomposition (new double[] { 1.243, 0, 0 });
//          testDecomposition (new double[] { 5, 0.00000001, 0.001 });
//          testDecomposition();
//       }
      for (int i=0; i<100; i++) {
         Matrix3d M = new Matrix3d();
         M.setRandom();
         testPolarDecomposition (M);
      }
      
   }

   public void checkTiming (int nrand) {
      Matrix3d M1 = new Matrix3d();
      M1.setRandom();

      SVDecomposition3d svd = new SVDecomposition3d();

      int cnt = 20000;
      FunctionTimer timer = new FunctionTimer();
      for (int k = 0; k < nrand; k++) {
         M1.setRandom();
         timer.restart();
         for (int i = 0; i < cnt; i++) {
            svd.factor (M1);
         }
         timer.stop();
      }
      System.out.println (timer.result (cnt * nrand));
   }

   public static void main (String[] args) {
      SVDecomposition3dTest tester = new SVDecomposition3dTest();

      boolean doTiming = false;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println ("Usage: SVDecomposition3dTest [-timing]");
            System.exit (1);
         }
      }

      if (doTiming) {
         System.out.println ("warming up ...");
         tester.checkTiming (20);
         System.out.println ("real answer ...");
         tester.checkTiming (20);
      }
      else {
         tester.execute();
         System.out.println ("\nPassed\n");
      }
   }
}
