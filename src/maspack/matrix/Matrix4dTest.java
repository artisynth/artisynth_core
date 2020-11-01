/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;

class Matrix4dTest extends MatrixTest {

   boolean equals (Matrix MR, Matrix M1) {
      return ((Matrix4d)M1).equals ((Matrix4d)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((Matrix4d)M1).epsilonEquals ((Matrix4d)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).add ((Matrix4d)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).add ((Matrix4d)M1, (Matrix4d)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).sub ((Matrix4d)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).sub ((Matrix4d)M1, (Matrix4d)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).mul ((Matrix4d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mul ((Matrix4d)M1, (Matrix4d)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).mulTranspose ((Matrix4d)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mulTransposeRight ((Matrix4d)M1, (Matrix4d)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mulTransposeLeft ((Matrix4d)M1, (Matrix4d)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mulTransposeBoth ((Matrix4d)M1, (Matrix4d)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).transpose ((Matrix4d)M1);
   }

   void transpose (Matrix MR) {
      ((Matrix4d)MR).transpose();
   }

   void invert (Matrix MR) {
      ((Matrix4d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).invert ((Matrix4d)M1);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).mulInverse ((Matrix4d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mulInverseRight ((Matrix4d)M1, (Matrix4d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mulInverseLeft ((Matrix4d)M1, (Matrix4d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).mulInverseBoth ((Matrix4d)M1, (Matrix4d)M2);
   }

   void negate (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).negate ((Matrix4d)M1);
   }

   void negate (Matrix MR) {
      ((Matrix4d)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix4d)MR).scale (s, (Matrix4d)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix4d)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix4d)MR).scaledAdd (s, (Matrix4d)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix4d)MR).scaledAdd (s, (Matrix4d)M1, (Matrix4d)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix4d)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((Matrix4d)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((Matrix4d)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix4d)MR).set ((Matrix4d)M1);
   }

   public void execute() {
      Matrix4d MR = new Matrix4d();
      Matrix4d M1 = new Matrix4d();
      Matrix4d M2 = new Matrix4d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (M1);
      testSetZero (MR);
      testSetIdentity (MR);
      testSetDiagonal (MR, new double[] { 1, 2, 3, 4 });

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
      }
   }

   public static void main (String[] args) {
      Matrix4dTest test = new Matrix4dTest();

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
