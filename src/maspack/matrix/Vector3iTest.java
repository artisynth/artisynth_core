/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class Vector3iTest extends VectoriTest {
   void add (Vectori vr, Vectori v1) {
      ((Vector3i)vr).add ((Vector3i)v1);
   }

   void add (Vectori vr, Vectori v1, Vectori v2) {
      ((Vector3i)vr).add ((Vector3i)v1, (Vector3i)v2);
   }

   void sub (Vectori vr, Vectori v1) {
      ((Vector3i)vr).sub ((Vector3i)v1);
   }

   void sub (Vectori vr, Vectori v1, Vectori v2) {
      ((Vector3i)vr).sub ((Vector3i)v1, (Vector3i)v2);
   }

   void negate (Vectori vr, Vectori v1) {
      ((Vector3i)vr).negate ((Vector3i)v1);
   }

   void negate (Vectori vr) {
      ((Vector3i)vr).negate();
   }

   void scale (Vectori vr, double s, Vectori v1) {
      ((Vector3i)vr).scale (s, (Vector3i)v1);
   }

   void scale (Vectori vr, double s) {
      ((Vector3i)vr).scale (s);
   }

   void setZero (Vectori vr) {
      ((Vector3i)vr).setZero();
   }

   void scaledAdd (Vectori vr, double s, Vectori v1) {
      ((Vector3i)vr).scaledAdd (s, (Vector3i)v1);
   }

   void scaledAdd (Vectori vr, double s, Vectori v1, Vectori v2) {
      ((Vector3i)vr).scaledAdd (s, (Vector3i)v1, (Vector3i)v2);
   }

   void set (Vectori vr, Vectori v1) {
      ((Vector3i)vr).set ((Vector3i)v1);
   }

   public void execute() {
      Vector3i vr = new Vector3i();
      Vector3i v1 = new Vector3i();
      Vector3i v2 = new Vector3i();

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1);
      testSetZero (vr);

      for (int i = 0; i < 100; i++) {
         v1.setRandom();
         v2.setRandom();
         vr.setRandom();

         testAdd (vr, v1, v2);
         testAdd (vr, vr, vr);

         testSub (vr, v1, v2);
         testSub (vr, vr, vr);

         testNegate (vr, v1);
         testNegate (vr, vr);

         testScale (vr, 3, v1);
         testScale (vr, -7, vr);

         testScaledAdd (vr, -7.5, v1, v2);
         testScaledAdd (vr, 4.5, vr, vr);

         testSet (vr, v1);
         testSet (vr, vr);

         testNorms (v1);
      }
   }

   public static void main (String[] args) {
      Vector3iTest test = new Vector3iTest();

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
