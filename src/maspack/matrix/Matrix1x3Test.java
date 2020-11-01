/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class Matrix1x3Test extends MatrixTest {

    boolean equals (Matrix MR, Matrix M1) {
      return ((Matrix1x3)M1).equals ((Matrix1x3)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((Matrix1x3)M1).epsilonEquals ((Matrix1x3)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((Matrix1x3)MR).add ((Matrix1x3)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix1x3)MR).add ((Matrix1x3)M1, (Matrix1x3)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix1x3)MR).sub ((Matrix1x3)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix1x3)MR).sub ((Matrix1x3)M1, (Matrix1x3)M2);
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix1x3)MR).scale (s, (Matrix1x3)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix1x3)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix1x3)MR).scaledAdd (s, (Matrix1x3)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix1x3)MR).scaledAdd (s, (Matrix1x3)M1, (Matrix1x3)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix1x3)MR).setZero();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix1x3)MR).set ((Matrix1x3)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix1x3)MR).mulAdd (M1, M2);
   }

   public void execute() {
      Matrix1x3 MR = new Matrix1x3();
      Matrix1x3 M1 = new Matrix1x3();
      Matrix1x3 M2 = new Matrix1x3();

      RandomGenerator.setSeed (0x1234);

      testGeneric (M1);
      testSetZero (MR);

      for (int i = 0; i < 10; i++) {
         M1.setRandom();
         M2.setRandom();
         MR.setRandom();

         testEquals (M1, MR);

         testAdd (MR, M1, M2);
         testAdd (MR, MR, MR);

         testSub (MR, M1, M2);
         testSub (MR, MR, MR);

         testScale (MR, 1.23, M1);
         testScale (MR, 1.23, MR);

         testScaledAdd (MR, 1.23, M1, M2);
         testScaledAdd (MR, 1.23, MR, MR);

         testSet (MR, M1);
         testSet (MR, MR);

         testNorms (M1);

         testMulAdd (MR);
      }
   }

   public static void main (String[] args) {
      Matrix1x3Test test = new Matrix1x3Test();

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
