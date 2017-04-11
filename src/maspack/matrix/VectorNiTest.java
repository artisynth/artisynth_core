/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class VectorNiTest extends VectoriTest {

   void add (Vectori vr, Vectori v1) {
      ((VectorNi)vr).add ((VectorNi)v1);
   }

   void add (Vectori vr, Vectori v1, Vectori v2) {
      ((VectorNi)vr).add ((VectorNi)v1, (VectorNi)v2);
   }

   void sub (Vectori vr, Vectori v1) {
      ((VectorNi)vr).sub ((VectorNi)v1);
   }

   void sub (Vectori vr, Vectori v1, Vectori v2) {
      ((VectorNi)vr).sub ((VectorNi)v1, (VectorNi)v2);
   }

   void negate (Vectori vr, Vectori v1) {
      ((VectorNi)vr).negate ((VectorNi)v1);
   }

   void negate (Vectori vr) {
      ((VectorNi)vr).negate();
   }

   void scale (Vectori vr, double s, Vectori v1) {
      ((VectorNi)vr).scale (s, (VectorNi)v1);
   }

   void scale (Vectori vr, double s) {
      ((VectorNi)vr).scale (s);
   }

   void setZero (Vectori vr) {
      ((VectorNi)vr).setZero();
   }

   void scaledAdd (Vectori vr, double s, Vectori v1) {
      ((VectorNi)vr).scaledAdd (s, (VectorNi)v1);
   }

   void scaledAdd (Vectori vr, double s, Vectori v1, Vectori v2) {
      ((VectorNi)vr).scaledAdd (s, (VectorNi)v1, (VectorNi)v2);
   }

   void set (Vectori vr, Vectori v1) {
      ((VectorNi)vr).set ((VectorNi)v1);
   }

   public void execute() {
      VectorNi vr_0 = new VectorNi (0);
      VectorNi vr_2 = new VectorNi (2);
      VectorNi vr_9 = new VectorNi (9);
      VectorNi vr_11 = new VectorNi (11);
      VectorNi v1_3 = new VectorNi (3);
      VectorNi v1_9 = new VectorNi (9);
      VectorNi v2_9 = new VectorNi (9);
      VectorNi v1_11 = new VectorNi (11);
      VectorNi v2_11 = new VectorNi (11);

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1_9);
      testGeneric (vr_2);
      testGeneric (vr_0);
      testSetZero (vr_9);

      for (int i = 0; i < 100; i++) {
         v1_3.setRandom();
         v1_9.setRandom();
         v2_9.setRandom();
         vr_9.setRandom();
         v1_11.setRandom();
         v2_11.setRandom();
         vr_11.setRandom();

         testAdd (vr_9, v1_9, v2_9);
         testAdd (vr_9, v1_9, v2_11);
         testAdd (vr_9, v1_11, v2_9);
         testAdd (vr_9, v1_11, v2_11);
         testAdd (vr_9, vr_9, vr_9);

         testSub (vr_9, v1_9, v2_9);
         testSub (vr_9, v1_9, v2_11);
         testSub (vr_9, v1_11, v2_9);
         testSub (vr_9, v1_11, v2_11);
         testSub (vr_9, vr_9, vr_9);

         testNegate (vr_9, v1_9);
         testNegate (vr_9, v1_11);
         testNegate (vr_9, vr_9);

         testScale (vr_9, 3, v1_9);
         testScale (vr_9, -7, v1_11);
         testScale (vr_9, 11, vr_9);

         testScaledAdd (vr_9, 3.0, v1_9, v2_9);
         testScaledAdd (vr_9, -6.7, v1_9, v2_11);
         testScaledAdd (vr_9, 11.0, v1_11, v2_9);
         testScaledAdd (vr_9, 9.0, v1_11, v2_11);
         testScaledAdd (vr_9, 12.5, vr_9, vr_9);

         testSet (vr_9, v1_9);
         testSet (vr_9, v1_11);
         testSet (vr_9, vr_9);

         testNorms (v1_9);
      }
   }

   public static void main (String[] args) {
      VectorNiTest test = new VectorNiTest();

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
