/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Class that supports rounding numbers with respect to k 10^n, where n is an
 * integer and k is 1, 2, or 5, or with respect to 2^n, where n is an
 * integer. These methods are mainly intended for automatic scaling operations.
 *
 * <p> With respect to the rounding up and down operations, if the operand is
 * within machine precision (as defined by the global variable TOLERANCE) of
 * one of the desired ``roundable'' values, then it will be left at that value
 * and not rounded up or down. This is to ensure more predicatable behavior.
 */
public class Round {

   public static final double TOLERANCE = 2e-15;
   private static final double LOG_2 = Math.log(2);

   /** 
    * Rounds a number x down to the nearest value defined by
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5. If x is zero, then
    * zero is returned. See the note about rounding tolerance in the
    * class documentation.
    *
    * @param x number to round down
    * @return rounded number
    */
   public static double down125 (double x) {
      return down125 (null, x);
   }

   /** 
    * Rounds a number x down to the nearest value defined by
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5. If x is zero, then
    * zero is returned. See the note about rounding tolerance in the
    * class documentation.
    *
    * @param factors if not <code>null</code>, returns the values of
    * <code>k</code> and <code>n</code> described above, in
    * <code>factors[0]</code> and <code>factors[1]</code>, respectively.
    * @param x number to round down
    * @return rounded number
    */
   public static double down125 (int[] factors, double x) {
      if (x == 0) {
         return 0;
      }
      else if (x < 0) {
         return -up125 (factors, -x);
      }
      double log10 = Math.log10(x);
      int n = (int)log10;
      if (log10 - n < 0) {
         n--;
      }
      double exp = Math.pow (10, n);
      double man = x/exp;
      int k;
      if (man < 2-TOLERANCE) {
         k = 1;
      }
      else if (man < 5-TOLERANCE) {
         k = 2;
      }
      else if (man < 10-TOLERANCE) {
         k = 5;
      }
      else {
         k = 1;
         // slightly more accurate to do it this way ...
         n++;
         exp = Math.pow (10, n);
      }
      if (factors != null) {
         factors[0] = k;
         factors[1] = n;
      }
      return k*exp;
   }

   /** 
    * Rounds a number x up to the nearest value defined by
    *
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5. If x is zero, then
    * zero is returned. See the note about rounding tolerance in the
    * class documentation.
    * 
    * @param x number to round up
    * @return rounded number
    */
   public static double up125 (double x) {
      return up125 (null, x);
   }

   /** 
    * Rounds a number x up to the nearest value defined by
    *
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5. If x is zero, then
    * zero is returned. See the note about rounding tolerance in the
    * class documentation.
    * 
    * @param factors if not <code>null</code>, returns the values of
    * <code>k</code> and <code>n</code> described above, in
    * <code>factors[0]</code> and <code>factors[1]</code>, respectively.
    * @param x number to round up
    * @return rounded number
    */
   public static double up125 (int[] factors, double x) {
      if (x == 0) {
         return 0;
      }
      else if (x < 0) {
         return -down125 (factors, -x);
      }
      double log10 = Math.log10(x);
      int n = (int)log10;
      if (log10 - n < 0) {
         n--;
      }
      double exp = Math.pow (10, n);
      double man = x/exp;
      int k;
      if (man <= 1+TOLERANCE) {
         k = 1;
      }
      else if (man <= 2+TOLERANCE) {
         k = 2;
      }
      else if (man <= 5+TOLERANCE) {
         k = 5;
      }
      else {
         k = 1;
         // slightly more accurate to do it this way ...
         n++;
         exp = Math.pow (10, n);
      }
      if (factors != null) {
         factors[0] = k;
         factors[1] = n;
      }
      return k*exp;
   }

   /** 
    * Rounds a number x to the nearest value defined by
    *
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5. If x is zero, then
    * zero is returned.
    * @param x number to round
    * @return rounded number
    */
   public static double near125 (double x) {
      return near125 (null, x);
   }
      
   /** 
    * Rounds a number x to the nearest value defined by
    *
    * <pre>
    * sgn(x) k 10^n 
    * </pre>
    * where n is an integer and k is 1, 2, or 5. If x is zero, then
    * zero is returned.
    * 
    * @param factors if not <code>null</code>, returns the values of
    * <code>k</code> and <code>n</code> described above, in
    * <code>factors[0]</code> and <code>factors[1]</code>, respectively.
    * @param x number to round
    * @return rounded number
    */
   public static double near125 (int[] factors, double x) {
      if (x == 0) {
         return 0;
      }
      else if (x < 0) {
         return -near125 (factors, -x);
      }
      double log10 = Math.log10(x);
      int n = (int)log10;
      if (log10 - n < 0) {
         n--;
      }
      double exp = Math.pow (10, n);
      double man = x/exp;
      int k;
      if (man < 1.5) {
         k = 1;
      }
      else if (man < 3.5) {
         k = 2;
      }
      else if (man < 7.5) {
         k = 5;
      }
      else {
         k = 1;
         // slightly more accurate to do it this way ...
         n++;
         exp = Math.pow (10, n);
      }
      if (factors != null) {
         factors[0] = k;
         factors[1] = n;
      }
      return k*exp;
   }


   /** 
    * Rounds a number x down to the nearest value defined by
    *
    * <pre>
    * sgn(x) 2^n 
    * </pre>
    * where n is an integer. If x is zero, then zero is returned.
    * See the note about rounding tolerance in the class documentation.
    * @param x number to round down
    * @return rounded number
    */
   public static double downPow2 (double x) {
      if (x == 0) {
         return 0;
      }
      else if (x < 0) {
         return -upPow2 (-x);
      }
      double log2 = Math.log(x)/LOG_2;
      int pow = (int)log2;
      if (log2 - pow < 0) {
         pow--;
      }
      double exp = Math.pow (2, pow);
      double man = x/exp;
      if (man < 2-TOLERANCE) {
         // nothing
      }
      else {
         exp *= 2.0;
      }
      return exp;
   }

   /** 
    * Rounds a number x up to the nearest value defined by
    *
    * <pre>
    * sgn(x) 2^n 
    * </pre>
    * where n is an integer. If x is zero, then zero is returned.
    * See the note about rounding tolerance in the class documentation.
    * @param x number to round up
    * @return rounded number
    */
   public static double upPow2  (double x) {
      if (x == 0) {
         return 0;
      }
      else if (x < 0) {
         return -downPow2 (-x);
      }
      double log2 = Math.log(x)/LOG_2;
      int pow = (int)log2;
      if (log2 - pow < 0) {
         pow--;
      }
      double exp = Math.pow (2, pow);
      double man = x/exp;
      if (man <= 1+TOLERANCE) {
         // nothing
      }
      else {
         exp *= 2.0;
      }
      return exp;
   }

   /** 
    * Rounds a number x to the nearest value defined by
    *
    * <pre>
    * sgn(x) 2^n 
    * </pre>
    * where n is an integer. If x is zero, then zero is returned.
    * @param x number to round
    * @return rounded number
    */
   public static double nearPow2 (double x) {
      if (x == 0) {
         return 0;
      }
      else if (x < 0) {
         return -nearPow2 (-x);
      }
      double log2 = Math.log(x)/LOG_2;
      int pow = (int)log2;
      if (log2 - pow < 0) {
         pow--;
      }
      double exp = Math.pow (2, pow);
      double man = x/exp;
      if (man < 1.5) {
         // nothing
      }
      else {
         exp *= 2.0;
      }
      return exp;
   }
}
