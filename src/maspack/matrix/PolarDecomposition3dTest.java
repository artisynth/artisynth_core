/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.*;

public class PolarDecomposition3dTest {
   public static void main (String[] args) {
      boolean doTiming = false;

      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println ("Usage: PolarDecomposition3dTest [-timing]");
            System.exit (1);
         }
      }

      Matrix3d A = new Matrix3d();
      Matrix3d R = new Matrix3d();
      Matrix3d H = new Matrix3d();
      Matrix3d Q = new Matrix3d();
      Matrix3d P = new Matrix3d();
      Random rand = new Random();
      rand.setSeed (0x1234);
      Matrix3d M = new Matrix3d();
      Matrix3d I = new Matrix3d();
      PolarDecomposition3d PD = new PolarDecomposition3d();

      if (doTiming) {
         A.setRandom (-0.5, 0.5, rand);
         FunctionTimer timer = new FunctionTimer();
         timer.start();
         for (int i = 0; i < 10000; i++) {
            PD.factor (A);
         }
         timer.stop();
         System.out.println ("run time: " + timer.result (10000));
      }
      else {
         I.setIdentity();

         for (int i = 0; i < 10000; i++) {
            A.setRandom (-0.5, 0.5, rand);

            PD.factor (A);

            PD.getR (R);
            PD.getH (H);
            M.mul (R, H);

            boolean failed = false;

            if (!M.epsilonEquals (A, 1e-13)) {
               System.out.println ("Bad R H decomposition, test " + i);
               failed = true;
            }
            if (!failed) {
               M.mulTransposeRight (R, R);
               if (!M.epsilonEquals (I, 1e-13)) {
                  System.out.println ("non-orthogonal R, test " + i);
                  failed = true;
               }
            }
            if (!failed && Math.abs(1-R.determinant()) > 1e-8) {
               System.out.println (
                  "R is not righthanded, det="+R.determinant()+", test " + i);
            }

            PD.getQ (Q);
            PD.getP (P);
            M.mul (Q, P);
            if (!failed && !M.epsilonEquals (A, 1e-13)) {
               System.out.println ("Bad Q P decomposition, test " + i);
               failed = true;
            }
            if (!failed) {
               M.mulTransposeRight (Q, Q);
               if (!M.epsilonEquals (I, 1e-13)) {
                  System.out.println ("non-orthogonal Q, test " + i);
                  failed = true;
               }
            }
               

            if (!failed) {
               PD.factorLeft (A);
               PD.getR (R);
               PD.getH (H);
               M.mul (H, R);
               if (!M.epsilonEquals (A, 1e-13)) {
                  System.out.println ("Bad H R decomposition, test " + i);
                  failed = true;
               }
               PD.getQ (Q);
               PD.getP (P);
               M.mul (P, Q);
               if (!failed && !M.epsilonEquals (A, 1e-13)) {
                  System.out.println ("Bad P Q decomposition, test " + i);
                  failed = true;
               }
            }
            if (!failed) {
               M.mulTransposeRight (R, R);
               if (!M.epsilonEquals (I, 1e-13)) {
                  System.out.println ("non-orthogonal left R, test " + i);
                  failed = true;
               }
            }
            if (!failed) {
               M.mulTransposeRight (Q, Q);
               if (!M.epsilonEquals (I, 1e-13)) {
                  System.out.println ("non-orthogonal left Q, test " + i);
                  failed = true;
               }
            }
            if (failed) {
               System.out.println ("A\n" + A.toString ("%8.5f"));
               System.out.println ("R\n" + R.toString ("%8.5f"));
               System.out.println ("H\n" + H.toString ("%8.5f"));
               System.out.println ("Q\n" + Q.toString ("%8.5f"));
               System.out.println ("P\n" + P.toString ("%8.5f"));
               System.out.println ("M\n" + M.toString ("%8.5f"));
               System.exit (1);
            }
         }
         System.out.println ("\nPassed\n");
      }
   }
}
