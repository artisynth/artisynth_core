/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

class NumericIntervalTest {
   private static boolean C = true;
   private static boolean O = false;

   public void test (
      double lower, double upper, boolean lowerClosed, boolean upperClosed,
      String testStr) {
      DoubleInterval rng = new DoubleInterval (lower, upper);
      rng.setLowerBoundClosed (lowerClosed);
      rng.setUpperBoundClosed (upperClosed);
      String str = rng.toString();
      if (!str.equals (testStr)) {
         throw new TestException ("DoubleRange string is " + str
         + ", expecting " + testStr);
      }
      rng.parse (str);
      DoubleInterval strRng = new DoubleInterval (testStr);
      if (!rng.equals (strRng)) {
         throw new TestException ("DoubleRange created from string is "
         + strRng + ", expecting " + rng);
      }
      if (rng.isUpperBoundClosed() != rng.myUpperClosed) {
         throw new TestException ("isUpperBoundClosed returns "
         + rng.isUpperBoundClosed() + ", expecting " + rng.myUpperClosed);
      }
      if (rng.isLowerBoundClosed() != rng.myLowerClosed) {
         throw new TestException ("isLowerBoundClosed returns "
         + rng.isLowerBoundClosed() + ", expecting " + rng.myLowerClosed);
      }
      if (rng.getUpperBound() != rng.myUpper) {
         throw new TestException ("getUpperBound returns "
         + rng.getUpperBound() + ", expecting " + rng.myUpper);
      }
      if (rng.getLowerBound() != rng.myLower) {
         throw new TestException ("getLowerBound returns "
         + rng.getLowerBound() + ", expecting " + rng.myLower);
      }
   }

   public void testIntersection (
      String rng1, String rng2, String rngr, boolean empty) {

      DoubleInterval r1 = new DoubleInterval (rng1);
      DoubleInterval r2 = new DoubleInterval (rng2);
      DoubleInterval rr = new DoubleInterval (rngr);

      DoubleInterval ck = new DoubleInterval (r1);
      ck.intersect (r2);
      if (!ck.equals (rr)) {
         throw new TestException (
            "intersection of "+rng1+" and "+rng2+" = "+ck+", expecting "+rngr);
      }
      ck = new DoubleInterval (r2);
      ck.intersect (r1);
      if (!ck.equals (rr)) {
         throw new TestException (
            "intersection of "+rng2+" and "+rng1+" = "+ck+", expecting "+rngr);
      }
      if (empty != ck.isEmpty()) {
         throw new TestException (
            ck+" empty="+ck.isEmpty()+", expecting "+empty);
      }
   }         

   public static void main (String[] args) {
      NumericIntervalTest tester = new NumericIntervalTest();

      try {
         tester.test (-1, 1, C, C, "[-1.0,1.0]");
         tester.test (1.7e-4, 1.9, C, C, "[1.7E-4,1.9]");
         tester.test (1.7e-4, 1.9, C, O, "[1.7E-4,1.9)");
         tester.test (1.7e-4, 1.9, O, C, "(1.7E-4,1.9]");
         tester.test (1.7e-4, 1.9, O, O, "(1.7E-4,1.9)");
         tester.test (Double.NEGATIVE_INFINITY, 1.56, C, C, "[-inf,1.56]");

         tester.testIntersection ("[4,5]", "[1,4]", "[4,4]", false);
         tester.testIntersection ("(4,5]", "[1,4)", "(4,4)", true);
         tester.testIntersection ("[3,5]", "[1,4]", "[3,4]", false);

         tester.testIntersection ("[1,6]", "[7,8]", "[7,6]", true);
         tester.testIntersection ("(1,6)", "[7,8]", "[7,6)", true);
         tester.testIntersection ("(1,6]", "[7,8)", "[7,6]", true);
         tester.testIntersection ("[1,6)", "(7,8]", "(7,6)", true);

         tester.testIntersection ("[1,6]", "[8,7]", "[8,6]", true);
         tester.testIntersection ("(1,6)", "[8,7]", "[8,6)", true);
         tester.testIntersection ("(1,6]", "[8,7)", "[8,6]", true);
         tester.testIntersection ("[1,6)", "(8,7]", "(8,6)", true);

         tester.testIntersection ("[1,6]", "(1,6)", "(1,6)", false);
         tester.testIntersection ("(1,6]", "[1,6)", "(1,6)", false);
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
