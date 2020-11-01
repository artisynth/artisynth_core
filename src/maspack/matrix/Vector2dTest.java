/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class Vector2dTest extends VectorTest {

   boolean equals (Vector vr, Vector v1) {
      return ((Vector2d)vr).equals ((Vector2d)v1);
   }

   boolean epsilonEquals (Vector vr, Vector v1, double tol) {
      return ((Vector2d)vr).epsilonEquals ((Vector2d)v1, tol);
   }

   void add (Vector vr, Vector v1) {
      ((Vector2d)vr).add ((Vector2d)v1);
   }

   void add (Vector vr, Vector v1, Vector v2) {
      ((Vector2d)vr).add ((Vector2d)v1, (Vector2d)v2);
   }

   void sub (Vector vr, Vector v1) {
      ((Vector2d)vr).sub ((Vector2d)v1);
   }

   void sub (Vector vr, Vector v1, Vector v2) {
      ((Vector2d)vr).sub ((Vector2d)v1, (Vector2d)v2);
   }

   void negate (Vector vr, Vector v1) {
      ((Vector2d)vr).negate ((Vector2d)v1);
   }

   void negate (Vector vr) {
      ((Vector2d)vr).negate();
   }

   void scale (Vector vr, double s, Vector v1) {
      ((Vector2d)vr).scale (s, (Vector2d)v1);
   }

   void scale (Vector vr, double s) {
      ((Vector2d)vr).scale (s);
   }

   void setZero (Vector vr) {
      ((Vector2d)vr).setZero();
   }

   void interpolate (Vector vr, double s, Vector v1) {
      ((Vector2d)vr).interpolate (s, (Vector2d)v1);
   }

   void interpolate (Vector vr, Vector v1, double s, Vector v2) {
      ((Vector2d)vr).interpolate ((Vector2d)v1, s, (Vector2d)v2);
   }

   void scaledAdd (Vector vr, double s, Vector v1) {
      ((Vector2d)vr).scaledAdd (s, (Vector2d)v1);
   }

   void scaledAdd (Vector vr, double s, Vector v1, Vector v2) {
      ((Vector2d)vr).scaledAdd (s, (Vector2d)v1, (Vector2d)v2);
   }

   void combine (Vector vr, double a, Vector v1, double b, Vector v2) {
      ((Vector2d)vr).combine (a, (Vector2d)v1, b, (Vector2d)v2);
   }

   double dot (Vector v1, Vector v2) {
      return ((Vector2d)v1).dot ((Vector2d)v2);
   }

   double angle (Vector v1, Vector v2) {
      return ((Vector2d)v1).angle ((Vector2d)v2);
   }

   void normalize (Vector vr) {
      ((Vector2d)vr).normalize();
   }

   void normalize (Vector vr, Vector v1) {
      ((Vector2d)vr).normalize ((Vector2d)v1);
   }

   void set (Vector vr, Vector v1) {
      ((Vector2d)vr).set ((Vector2d)v1);
   }

   public void execute() {
      Vector2d vr = new Vector2d();
      Vector2d v1 = new Vector2d();
      Vector2d v2 = new Vector2d();

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

         testScaledAdd (vr, 7.3, v1, v2);
         testScaledAdd (vr, -4.5, vr, vr);

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
      Vector2dTest test = new Vector2dTest();

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
