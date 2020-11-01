/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class Matrix6x2Test extends MatrixTest {

   boolean equals (Matrix MR, Matrix M1) {
      return ((Matrix6x2)M1).equals ((Matrix6x2)MR);
   }

   boolean epsilonEquals (Matrix MR, Matrix M1, double tol) {
      return ((Matrix6x2)M1).epsilonEquals ((Matrix6x2)MR, tol);
   }

   void add (Matrix MR, Matrix M1) {
      ((Matrix6x2)MR).add ((Matrix6x2)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6x2)MR).add ((Matrix6x2)M1, (Matrix6x2)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix6x2)MR).sub ((Matrix6x2)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6x2)MR).sub ((Matrix6x2)M1, (Matrix6x2)M2);
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix6x2)MR).scale (s, (Matrix6x2)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix6x2)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix6x2)MR).scaledAdd (s, (Matrix6x2)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix6x2)MR).scaledAdd (s, (Matrix6x2)M1, (Matrix6x2)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix6x2)MR).setZero();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix6x2)MR).set ((Matrix6x2)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix6x2)MR).mulAdd (M1, M2);
   }

   public void execute() {
      Matrix6x2 MR = new Matrix6x2();
      Matrix6x2 M1 = new Matrix6x2();
      Matrix6x2 M2 = new Matrix6x2();

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
      Matrix6x2Test test = new Matrix6x2Test();

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
