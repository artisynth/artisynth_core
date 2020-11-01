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

class Matrix3dTest extends MatrixTest {

   boolean equals (Matrix MR, Matrix M1) {
      return ((Matrix3d)M1).equals ((Matrix3d)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((Matrix3d)M1).epsilonEquals ((Matrix3d)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).add ((Matrix3d)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).add ((Matrix3d)M1, (Matrix3d)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).sub ((Matrix3d)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).sub ((Matrix3d)M1, (Matrix3d)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).mul ((Matrix3d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mul ((Matrix3d)M1, (Matrix3d)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).mulTranspose ((Matrix3d)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulTransposeRight ((Matrix3d)M1, (Matrix3d)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulTransposeLeft ((Matrix3d)M1, (Matrix3d)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulTransposeBoth ((Matrix3d)M1, (Matrix3d)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).transpose ((Matrix3d)M1);
   }

   void transpose (Matrix MR) {
      ((Matrix3d)MR).transpose();
   }

   void invert (Matrix MR) {
      ((Matrix3d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).invert ((Matrix3d)M1);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).mulInverse ((Matrix3d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulInverseRight ((Matrix3d)M1, (Matrix3d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulInverseLeft ((Matrix3d)M1, (Matrix3d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulInverseBoth ((Matrix3d)M1, (Matrix3d)M2);
   }

   void negate (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).negate ((Matrix3d)M1);
   }

   void negate (Matrix MR) {
      ((Matrix3d)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix3d)MR).scale (s, (Matrix3d)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix3d)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix3d)MR).scaledAdd (s, (Matrix3d)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).scaledAdd (s, (Matrix3d)M1, (Matrix3d)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix3d)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((Matrix3d)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((Matrix3d)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix3d)MR).set ((Matrix3d)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3d)MR).mulAdd (M1, M2);
   }

   public void testTransform (Matrix3d M1) {
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRandom();

      Matrix3d MR = new Matrix3d();
      Matrix3d MRCheck = new Matrix3d();

      MR.transform (R, M1);
      MRCheck.mulTransposeRight (M1, R);
      MRCheck.mul (R, MRCheck);
      if (!MRCheck.epsilonEquals (MR, 10 * EPSILON)) {
         throw new TestException ("transform failed");
      }
      MR.set (M1);
      MR.transform (R);
      if (!MRCheck.epsilonEquals (MR, 10 * EPSILON)) {
         throw new TestException ("self transform failed");
      }

      MR.inverseTransform (R, M1);
      MRCheck.mul (M1, R);
      MRCheck.mulTransposeLeft (R, MRCheck);
      if (!MRCheck.epsilonEquals (MR, 10 * EPSILON)) {
         throw new TestException ("inverse transform failed");
      }
      MR.set (M1);
      MR.inverseTransform (R);
      if (!MRCheck.epsilonEquals (MR, 10 * EPSILON)) {
         throw new TestException ("self inverse transform failed");
      }
   }

   public void testSolve (Matrix3d M1) {

      Matrix3d Minv = new Matrix3d();
      Vector3d b = new Vector3d();
      Vector3d x = new Vector3d();
      Vector3d chk = new Vector3d();

      LUDecomposition lud = new LUDecomposition (M1);
      double cond = lud.conditionEstimate (M1);

      b.setRandom();
      M1.solve (x, b);
      Minv.invert (M1);
      Minv.mul (chk, b);
      if (!chk.epsilonEquals (x, cond*EPSILON*chk.norm())) {
         System.out.println ("solve: x = " + x);
         System.out.println ("expected:  " + chk);
         System.out.println ("eps=" + cond*EPSILON*chk.norm());
         throw new TestException ("solve failed");
      }

      M1.solveTranspose (x, b);
      Minv.transpose ();
      Minv.mul (chk, b);
      if (!chk.epsilonEquals (x, cond*EPSILON*chk.norm())) {
         System.out.println ("solveTranspose: x = " + x);
         System.out.println ("expected:           " + chk);
         System.out.println ("eps=" + cond*EPSILON*chk.norm());
         throw new TestException ("solveTranspose failed");
      }
   }

   public void execute() {
      Matrix3d MR = new Matrix3d();
      Matrix3d M1 = new Matrix3d();
      Matrix3d M2 = new Matrix3d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (M1);
      testSetZero (MR);
      testSetIdentity (MR);
      testSetDiagonal (MR, new double[] { 1, 2, 3 });

      for (int i = 0; i < 100; i++) {
         M1.setRandom();
         M2.setRandom();
         MR.setRandom();

         testEquals (M1, MR);

         testAdd (MR, M1, M2);
         testAdd (MR, MR, MR);

         testSub (MR, M1, M2);
         testSub (MR, MR, MR);

         testMul (MR, M1, M2);
         testMul (MR, MR, MR);

         testMulTranspose (MR, M1, M2);
         testMulTranspose (MR, MR, MR);

         testMulInverse (MR, M1, M2);
         testMulInverse (MR, MR, MR);

         testNegate (MR, M1);
         testNegate (MR, MR);

         testScale (MR, 1.23, M1);
         testScale (MR, 1.23, MR);

         testScaledAdd (MR, 1.23, M1, M2);
         testScaledAdd (MR, 1.23, MR, MR);

         testSet (MR, M1);
         testSet (MR, MR);

         testTranspose (MR, M1);
         testTranspose (MR, MR);

         testInvert (MR, M1);
         testInvert (MR, MR);

         testNorms (M1);

         testTransform (M1);

         testMulAdd (MR);
      }

      for (int i=0; i<1000; i++) {
         M1.setRandom();
         testSolve (M1);
      }
      
   }

   public void timing() {

      int nsamps = 100000;
      RandomGenerator.setSeed (0x1234);

      FunctionTimer timer = new FunctionTimer();

      Matrix3d[] MR = new Matrix3d[nsamps];
      Matrix3d[] M1 = new Matrix3d[nsamps];
      Matrix3d[] M2 = new Matrix3d[nsamps];
      Vector3d[] vr = new Vector3d[nsamps];
      Vector3d[] v1 = new Vector3d[nsamps];

      for (int i=0; i<nsamps; i++) {
         MR[i] = new Matrix3d();
         M1[i] = new Matrix3d();
         M1[i].setRandom();
         M2[i] = new Matrix3d();
         M2[i].setRandom();
         vr[i] = new Vector3d();
         v1[i] = new Vector3d();
         v1[i].setRandom();
      }

      int cnt = 1000;
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<nsamps; i++) {
            MR[i].mul (M1[i], M2[i]);
            MR[i].mul (vr[i], v1[i]);
            MR[i].solve (vr[i], v1[i]);
            MR[i].mulInverse (vr[i], v1[i]);
         }
      }

      timer.start();
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<nsamps; i++) {
            MR[i].mul (M1[i], M2[i]);
         }
      }
      timer.stop();
      System.out.println ("matrix mul: " + timer.result(cnt*nsamps));

      timer.start();
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<nsamps; i++) {
            MR[i].mul (vr[i], v1[i]);
         }
      }
      timer.stop();
      System.out.println ("vector mul: " + timer.result(cnt*nsamps));

      timer.start();
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<nsamps; i++) {
            MR[i].solve (vr[i], v1[i]);
         }
      }
      timer.stop();
      System.out.println ("vector solve: " + timer.result(cnt*nsamps));

      timer.start();
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<nsamps; i++) {
            MR[i].mulInverse (vr[i], v1[i]);
         }
      }
      timer.stop();
      System.out.println ("vector mulInverse: " + timer.result(cnt*nsamps));
   }

   public static void main (String[] args) {
      boolean dotiming = false;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            dotiming = true;
         }
         else {
            System.err.println (
               "Usage: java maspack.matrix.Matrix3dTest [-timing]");
         }
      }
      Matrix3dTest tester = new Matrix3dTest();
      try {
         if (dotiming) {
            tester.timing();
         }
         else {
            tester.execute();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");
   }
}
