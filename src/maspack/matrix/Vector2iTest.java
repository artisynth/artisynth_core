/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class Vector2iTest extends VectoriTest {
   void add (Vectori vr, Vectori v1) {
      ((Vector2i)vr).add ((Vector2i)v1);
   }

   void add (Vectori vr, Vectori v1, Vectori v2) {
      ((Vector2i)vr).add ((Vector2i)v1, (Vector2i)v2);
   }

   void sub (Vectori vr, Vectori v1) {
      ((Vector2i)vr).sub ((Vector2i)v1);
   }

   void sub (Vectori vr, Vectori v1, Vectori v2) {
      ((Vector2i)vr).sub ((Vector2i)v1, (Vector2i)v2);
   }

   void negate (Vectori vr, Vectori v1) {
      ((Vector2i)vr).negate ((Vector2i)v1);
   }

   void negate (Vectori vr) {
      ((Vector2i)vr).negate();
   }

   void scale (Vectori vr, double s, Vectori v1) {
      ((Vector2i)vr).scale (s, (Vector2i)v1);
   }

   void scale (Vectori vr, double s) {
      ((Vector2i)vr).scale (s);
   }

   void setZero (Vectori vr) {
      ((Vector2i)vr).setZero();
   }

   void scaledAdd (Vectori vr, double s, Vectori v1) {
      ((Vector2i)vr).scaledAdd (s, (Vector2i)v1);
   }

   void scaledAdd (Vectori vr, double s, Vectori v1, Vectori v2) {
      ((Vector2i)vr).scaledAdd (s, (Vector2i)v1, (Vector2i)v2);
   }

   void set (Vectori vr, Vectori v1) {
      ((Vector2i)vr).set ((Vector2i)v1);
   }

   public void execute() {
      Vector2i vr = new Vector2i();
      Vector2i v1 = new Vector2i();
      Vector2i v2 = new Vector2i();

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

         testScaledAdd (vr, 7.3, v1, v2);
         testScaledAdd (vr, -4.5, vr, vr);

         testSet (vr, v1);
         testSet (vr, vr);

         testNorms (v1);
      }
   }

   public static void main (String[] args) {
      Vector2iTest test = new Vector2iTest();

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
