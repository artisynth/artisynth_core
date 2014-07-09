/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.FunctionTimer;

class Matrix3dTest extends MatrixTest {
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
      }
   }

   public void timing() {

      Matrix3d M1 = new Matrix3d();
      Matrix3d M2 = new Matrix3d();
      Matrix3d MR = new Matrix3d();
      Vector3d vr = new Vector3d();
      Vector3d v1 = new Vector3d();

      RandomGenerator.setSeed (0x1234);
      M1.setRandom();
      M2.setRandom();
      v1.setRandom();
      FunctionTimer timer = new FunctionTimer();

      int cnt = 100000000;
      timer.start();
      for (int i=0; i<cnt; i++) {
         MR.mul (M1, M2);
      }
      timer.stop();
      System.out.println ("matrix mul: " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         MR.mul (vr, v1);
      }
      timer.stop();
      System.out.println ("vector mul: " + timer.result(cnt));
   }

   public static void main (String[] args) {
      Matrix3dTest test = new Matrix3dTest();
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
