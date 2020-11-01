/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class Vector4dTest extends VectorTest {

   boolean equals (Vector vr, Vector v1) {
      return ((Vector4d)vr).equals ((Vector4d)v1);
   }

   boolean epsilonEquals (Vector vr, Vector v1, double tol) {
      return ((Vector4d)vr).epsilonEquals ((Vector4d)v1, tol);
   }

   void add (Vector vr, Vector v1) {
      ((Vector4d)vr).add ((Vector4d)v1);
   }

   void add (Vector vr, Vector v1, Vector v2) {
      ((Vector4d)vr).add ((Vector4d)v1, (Vector4d)v2);
   }

   void sub (Vector vr, Vector v1) {
      ((Vector4d)vr).sub ((Vector4d)v1);
   }

   void sub (Vector vr, Vector v1, Vector v2) {
      ((Vector4d)vr).sub ((Vector4d)v1, (Vector4d)v2);
   }

   void negate (Vector vr, Vector v1) {
      ((Vector4d)vr).negate ((Vector4d)v1);
   }

   void negate (Vector vr) {
      ((Vector4d)vr).negate();
   }

   void scale (Vector vr, double s, Vector v1) {
      ((Vector4d)vr).scale (s, (Vector4d)v1);
   }

   void scale (Vector vr, double s) {
      ((Vector4d)vr).scale (s);
   }

   void setZero (Vector vr) {
      ((Vector4d)vr).setZero();
   }

   void interpolate (Vector vr, double s, Vector v1) {
      ((Vector4d)vr).interpolate (s, (Vector4d)v1);
   }

   void interpolate (Vector vr, Vector v1, double s, Vector v2) {
      ((Vector4d)vr).interpolate ((Vector4d)v1, s, (Vector4d)v2);
   }

   void scaledAdd (Vector vr, double s, Vector v1) {
      ((Vector4d)vr).scaledAdd (s, (Vector4d)v1);
   }

   void scaledAdd (Vector vr, double s, Vector v1, Vector v2) {
      ((Vector4d)vr).scaledAdd (s, (Vector4d)v1, (Vector4d)v2);
   }

   void combine (Vector vr, double a, Vector v1, double b, Vector v2) {
      ((Vector4d)vr).combine (a, (Vector4d)v1, b, (Vector4d)v2);
   }

   double dot (Vector v1, Vector v2) {
      return ((Vector4d)v1).dot ((Vector4d)v2);
   }

   double angle (Vector v1, Vector v2) {
      return ((Vector4d)v1).angle ((Vector4d)v2);
   }

   void normalize (Vector vr) {
      ((Vector4d)vr).normalize();
   }

   void normalize (Vector vr, Vector v1) {
      ((Vector4d)vr).normalize ((Vector4d)v1);
   }

   void set (Vector vr, Vector v1) {
      ((Vector4d)vr).set ((Vector4d)v1);
   }

   public void execute() {
      Vector4d vr = new Vector4d();
      Vector4d v1 = new Vector4d();
      Vector4d v2 = new Vector4d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1);
      testSetZero (vr);

      for (int i = 0; i < 100; i++) {
         v1.setRandom();
         v2.setRandom();
         vr.setRandom();

         testEquals (vr, v1);

         testAdd (vr, v1, v2);
         testAdd (vr, vr, vr);

         testSub (vr, v1, v2);
         testSub (vr, vr, vr);

         testNegate (vr, v1);
         testNegate (vr, vr);

         testScale (vr, 1.23, v1);
         testScale (vr, 1.23, vr);

         testSet (vr, v1);
         testSet (vr, vr);

         testNormalize (vr, v1);
         testNormalize (vr, vr);

         testCombine (vr, 0.123, v1, 0.677, v2);
         testCombine (vr, 0.123, vr, 0.677, vr);

         testNorms (v1);
         testDotAndAngle (v1, v2);
      }
   }

   public static void main (String[] args) {
      Vector4dTest test = new Vector4dTest();

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
