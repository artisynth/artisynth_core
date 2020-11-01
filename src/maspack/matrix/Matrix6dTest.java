/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;

class Matrix6dTest extends MatrixTest {

   boolean equals (Matrix MR, Matrix M1) {
      return ((Matrix6d)M1).equals ((Matrix6d)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((Matrix6d)M1).epsilonEquals ((Matrix6d)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).add ((Matrix6d)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).add ((Matrix6d)M1, (Matrix6d)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).sub ((Matrix6d)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).sub ((Matrix6d)M1, (Matrix6d)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).mul ((Matrix6d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mul ((Matrix6d)M1, (Matrix6d)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).mulTranspose ((Matrix6d)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulTransposeRight ((Matrix6d)M1, (Matrix6d)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulTransposeLeft ((Matrix6d)M1, (Matrix6d)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulTransposeBoth ((Matrix6d)M1, (Matrix6d)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).transpose ((Matrix6d)M1);
   }

   void transpose (Matrix MR) {
      ((Matrix6d)MR).transpose();
   }

   void invert (Matrix MR) {
      ((Matrix6d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).invert ((Matrix6d)M1);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).mulInverse ((Matrix6d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulInverseRight ((Matrix6d)M1, (Matrix6d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulInverseLeft ((Matrix6d)M1, (Matrix6d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulInverseBoth ((Matrix6d)M1, (Matrix6d)M2);
   }

   void negate (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).negate ((Matrix6d)M1);
   }

   void negate (Matrix MR) {
      ((Matrix6d)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix6d)MR).scale (s, (Matrix6d)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix6d)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix6d)MR).scaledAdd (s, (Matrix6d)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).scaledAdd (s, (Matrix6d)M1, (Matrix6d)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix6d)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((Matrix6d)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((Matrix6d)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix6d)MR).set ((Matrix6d)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6d)MR).mulAdd (M1, M2);
   }

   public void testTransform (Matrix6d M1) {
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRandom();

      Matrix6d MR = new Matrix6d();
      Matrix6d MRCheck = new Matrix6d();

      Matrix3d M00 = new Matrix3d();
      Matrix3d M03 = new Matrix3d();
      Matrix3d M30 = new Matrix3d();
      Matrix3d M33 = new Matrix3d();

      M1.getSubMatrix00 (M00);
      M1.getSubMatrix03 (M03);
      M1.getSubMatrix30 (M30);
      M1.getSubMatrix33 (M33);

      M00.transform (R);
      M03.transform (R);
      M30.transform (R);
      M33.transform (R);

      MRCheck.setSubMatrix00 (M00);
      MRCheck.setSubMatrix03 (M03);
      MRCheck.setSubMatrix30 (M30);
      MRCheck.setSubMatrix33 (M33);

      MR.transform (R, M1);
      if (!MRCheck.epsilonEquals (MR, 10 * EPSILON)) {
         throw new TestException ("transform failed");
      }
      MR.inverseTransform (R, MR);
      if (!MR.epsilonEquals (M1, 10 * EPSILON)) {
         throw new TestException ("transform-inverseTransform failed");
      }
      MR.set (M1);
      MR.transform (R);
      MR.inverseTransform (R);
      if (!MR.epsilonEquals (M1, 10 * EPSILON)) {
         throw new TestException ("self transform-inverseTransform failed");
      }
   }

   public void execute() {
      Matrix6d MR = new Matrix6d();
      Matrix6d M1 = new Matrix6d();
      Matrix6d M2 = new Matrix6d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (M1);
      testSetZero (MR);
      testSetIdentity (MR);
      testSetDiagonal (MR, new double[] { 1, 2, 3, 4, 5, 6 });

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

         testMulAdd (MR);

         testTransform (M1);
      }
   }

   public static void main (String[] args) {
      Matrix6dTest test = new Matrix6dTest();

      try {
         test.execute();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");
   }
}
