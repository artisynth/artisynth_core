/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.*;

public class QRDecomposition3dTest {
   public static void main (String[] args) {
      boolean doTiming = false;

      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println ("Usage: QRDecomposition3dTest [-timing]");
            System.exit (1);
         }
      }

      Matrix3d A = new Matrix3d();
      Matrix3d Q = new Matrix3d();
      Matrix3d R = new Matrix3d();
      Random rand = new Random();
      rand.setSeed (0x1234);
      Matrix3d M = new Matrix3d();
      Matrix3d I = new Matrix3d();
      QRDecomposition3d QRD = new QRDecomposition3d();

      if (doTiming) {
         A.setRandom (-0.5, 0.5, rand);
         FunctionTimer timer = new FunctionTimer();
         timer.start();
         for (int i = 0; i < 10000; i++) {
            QRD.set (A);
         }
         timer.stop();
         System.out.println ("run time: " + timer.result (10000));
      }
      else {
         I.setIdentity();

         for (int i = 0; i < 10000; i++) {
            A.setRandom (-0.5, 0.5, rand);

            QRD.set (A);
            QRD.getQ (Q);
            QRD.getR (R);
            M.mul (Q, R);
            if (!M.epsilonEquals (A, 1e-13)) {
               System.out.println ("Bad decomposition, test " + i);

               System.out.println ("A\n" + A.toString ("%8.5f"));
               System.out.println ("Q\n" + Q.toString ("%8.5f"));
               System.out.println ("R\n" + R.toString ("%8.5f"));
               System.out.println ("M\n" + M.toString ("%8.5f"));
               System.exit (1);
            }
            // if (!QRD.isQRightHanded())
            // { System.out.println ("Q not right handed, test " + i);
            // System.out.println ("A\n" + A.toString("%8.5f"));
            // System.out.println ("Q\n" + Q.toString("%8.5f"));
            // System.out.println ("R\n" + R.toString("%8.5f"));
            // }
            M.mulTransposeRight (Q, Q);
            if (!M.epsilonEquals (I, 1e-13)) {
               System.out.println ("non-orthogonal Q, test " + i);
               System.out.println ("A\n" + A.toString ("%8.5f"));
               System.out.println ("Q\n" + Q.toString ("%8.5f"));
               System.out.println ("R\n" + R.toString ("%8.5f"));
               System.out.println ("M\n" + M.toString ("%8.5f"));
               System.exit (1);
            }
         }
         System.out.println ("\nPassed\n");
      }
   }
}
