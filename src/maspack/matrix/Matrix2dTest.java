/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;

class Matrix2dTest extends MatrixTest {

   boolean equals (Matrix MR, Matrix M1) {
      return ((Matrix2d)M1).equals ((Matrix2d)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((Matrix2d)M1).epsilonEquals ((Matrix2d)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).add ((Matrix2d)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).add ((Matrix2d)M1, (Matrix2d)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).sub ((Matrix2d)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).sub ((Matrix2d)M1, (Matrix2d)M2);
   }

   void mul (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).mul ((Matrix2d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mul ((Matrix2d)M1, (Matrix2d)M2);
   }

   void mulTranspose (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).mulTranspose ((Matrix2d)M1);
   }

   void mulTransposeRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulTransposeRight ((Matrix2d)M1, (Matrix2d)M2);
   }

   void mulTransposeLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulTransposeLeft ((Matrix2d)M1, (Matrix2d)M2);
   }

   void mulTransposeBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulTransposeBoth ((Matrix2d)M1, (Matrix2d)M2);
   }

   void transpose (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).transpose ((Matrix2d)M1);
   }

   void transpose (Matrix MR) {
      ((Matrix2d)MR).transpose();
   }

   void invert (Matrix MR) {
      ((Matrix2d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).invert ((Matrix2d)M1);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).mulInverse ((Matrix2d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulInverseRight ((Matrix2d)M1, (Matrix2d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulInverseLeft ((Matrix2d)M1, (Matrix2d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulInverseBoth ((Matrix2d)M1, (Matrix2d)M2);
   }

   void negate (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).negate ((Matrix2d)M1);
   }

   void negate (Matrix MR) {
      ((Matrix2d)MR).negate();
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix2d)MR).scale (s, (Matrix2d)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix2d)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix2d)MR).scaledAdd (s, (Matrix2d)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).scaledAdd (s, (Matrix2d)M1, (Matrix2d)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix2d)MR).setZero();
   }

   void setDiagonal (Matrix MR, double[] diagValues) {
      ((Matrix2d)MR).setDiagonal (diagValues);
   }

   void setIdentity (Matrix MR) {
      ((Matrix2d)MR).setIdentity();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix2d)MR).set ((Matrix2d)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix2d)MR).mulAdd (M1, M2);
   }

   public void execute() {
      Matrix2d MR = new Matrix2d();
      Matrix2d M1 = new Matrix2d();
      Matrix2d M2 = new Matrix2d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (M1);
      testSetZero (MR);
      testSetIdentity (MR);
      testSetDiagonal (MR, new double[] { 1, 2 });

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
      }
   }

   public static void main (String[] args) {
      Matrix2dTest test = new Matrix2dTest();

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
