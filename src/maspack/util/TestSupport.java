/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Collection;
import java.util.Iterator;

import maspack.matrix.Matrix;
import maspack.matrix.Vector;

public class TestSupport {

   /**
    * Checks an assertion and throws a TestException if it fails.
    */
   public static void doassert (boolean expr, String msg) {
      if (!expr) {
         throw new TestException ("Assertion failed: "+msg);
      }
   }

   /**
    * Returns true if the boolean arrys a1 and a2 are equal, or if they are both
    * null.
    * 
    * @param a1
    * first array
    * @param a2
    * second array
    * @return true if the arrays are equal
    */
   public static boolean equals (boolean[] a1, boolean[] a2) {
      if (a1 == null && a2 == null) {
         return true;
      }
      else if (a1 != null && a2 != null) {
         if (a1.length != a2.length) {
            return false;
         }
         else {
            for (int i = 0; i < a1.length; i++) {
               if (a1[i] != a2[i]) {
                  return false;
               }
            }
            return true;
         }
      }
      return false;
   }

   /**
    * Returns true if the integer arrys a1 and a2 are equal, or if they are both
    * null.
    * 
    * @param a1
    * first array
    * @param a2
    * second array
    * @return true if the arrays are equal
    */
   public static boolean equals (int[] a1, int[] a2) {
      if (a1 == null && a2 == null) {
         return true;
      }
      else if (a1 != null && a2 != null) {
         if (a1.length != a2.length) {
            return false;
         }
         else {
            for (int i = 0; i < a1.length; i++) {
               if (a1[i] != a2[i]) {
                  return false;
               }
            }
            return true;
         }
      }
      return false;
   }

   /**
    * Returns true if the double arrys a1 and a2 are equal, or if they are both
    * null.
    * 
    * @param a1
    * first array
    * @param a2
    * second array
    * @return true if the arrays are equal
    */
   public static boolean equals (double[] a1, double[] a2) {
      if (a1 == null && a2 == null) {
         return true;
      }
      else if (a1 != null && a2 != null) {
         if (a1.length != a2.length) {
            return false;
         }
         else {
            for (int i = 0; i < a1.length; i++) {
               if (a1[i] != a2[i]) {
                  return false;
               }
            }
            return true;
         }
      }
      return false;
   }

   /**
    * Returns true if the Strings s1 and s2 are equal, or if they are both null.
    * 
    * @param s1
    * first string
    * @param s2
    * second string
    * @return true if the strings are equal
    */
   public static boolean equals (String s1, String s2) {
      if (s1 == null && s2 == null) {
         return true;
      }
      else if (s1 != null && s2 != null) {
         return (s1.equals (s2));
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if the double arrys a1 and a2 are equal within a presecribed
    * tolerance, or if they are both null.
    * 
    * @param a1
    * first array
    * @param a2
    * second array
    * @param tol
    * tolerance
    * @return true if the arrays are equal
    */
   public static boolean epsilonEquals (double[] a1, double[] a2, double tol) {
      if (a1 == null && a2 == null) {
         return true;
      }
      else if (a1 != null && a2 != null) {
         if (a1.length != a2.length) {
            return false;
         }
         else {
            for (int i = 0; i < a1.length; i++) {
               if (Math.abs (a1[i] - a2[i]) > tol) {
                  return false;
               }
            }
            return true;
         }
      }
      return false;
   }

   /**
    * Returns true if two exceptions have the same class type and error message,
    * or if they are both null.
    * 
    * @param e1
    * first exception
    * @param e2
    * second exception
    * @return true if both exceptions are equal
    */
   public static boolean equals (Exception e1, Exception e2) {
      if (e1 == null && e2 == null) {
         return true;
      }
      else if (e1 != null && e2 != null) {
         if (!e1.getClass().getName().equals (e2.getClass().getName())) {
            return false;
         }
         else {
            return equals (e1.getMessage(), e2.getMessage());
         }
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if the contents of two collections are the same, or if they
    * are both null. The contents are considered equal if they have the same
    * number of elements and if corresponding elements are themselves equal.
    * 
    * @param c1
    * first collection
    * @param c2
    * second collection
    * @return true if both collections are equal
    */
   public static boolean equals (Collection<?> c1, Collection<?> c2) {
      if (c1 == null && c2 == null) {
         return true;
      }
      else if (c1 != null && c2 != null) {
         if (c1.size() != c2.size()) {
            return false;
         }
         else {
            Iterator<?> it1 = c1.iterator();
            Iterator<?> it2 = c2.iterator();
            while (it1.hasNext()) {
               Object obj1 = it1.next();
               Object obj2 = it2.next();
               if (obj1 instanceof Vector && obj2 instanceof Vector) {
                  if (!((Vector)obj1).epsilonEquals ((Vector)obj2, 0)) {
                     return false;
                  }
               }
               else if (obj1 instanceof Matrix && obj2 instanceof Matrix) {
                  if (!((Matrix)obj1).epsilonEquals ((Matrix)obj2, 0)) {
                     return false;
                  }
               }
               else if (!obj1.equals (obj2)) {
                  return false;
               }
            }
            return true;
         }
      }
      else {
         return false;
      }
   }

   /**
    * Returns a string giving the class name and message for a particular
    * exception.
    */
   public static String exceptionName (Exception e) {
      if (e == null) {
         return "null";
      }
      else {
         return e.getClass().getName() + ":" + e.getMessage();
      }
   }

   /**
    * Checks to see if an actual exception equals an expected exception. If they
    * do not, then throw a TestException.
    * 
    * @param eActual
    * exception that actually occurred.
    * @param eExpected
    * exception that we are expecting.
    */
   public static void checkExceptions (Exception eActual, Exception eExpected)
      throws TestException {
      if (!equals (eActual, eExpected)) {
         String msg;
         if (eActual == null) {
            msg =
               ("Expected exception:\n" + exceptionName (eExpected) + "\nbut got none");
         }
         else if (eExpected == null) {
            msg = ("Unexpected exception:\n" + exceptionName (eActual));
         }
         else {
            msg =
               ("Expected exception:\n" + exceptionName (eExpected)
               + "\nbut got:\n" + exceptionName (eActual));
         }
         throw new TestException (msg);
      }
   }

   /**
    * Converts an array of booleans into a String
    * 
    * @param a
    * boolean array
    * @return string representation
    */
   public static String toString (boolean[] a) {
      String s = "";
      if (a == null) {
         s = "null";
      }
      else {
         s = "[";
         for (int i = 0; i < a.length; i++) {
            s += a[i] + " ";
         }
         s += "]";
      }
      return s;
   }

   /**
    * Converts an array of integers into a String
    * 
    * @param a
    * integer array
    * @return string representation
    */
   public static String toString (int[] a) {
      String s = "";
      if (a == null) {
         s = "null";
      }
      else {
         s = "[";
         for (int i = 0; i < a.length; i++) {
            s += a[i] + " ";
         }
         s += "]";
      }
      return s;
   }

   /**
    * Converts an array of doubles into a String
    * 
    * @param a
    * double array
    * @return string representation
    */
   public static String toString (double[] a) {
      String s = "";
      if (a == null) {
         s = "null";
      }
      else {
         s = "[";
         for (int i = 0; i < a.length; i++) {
            s += a[i] + " ";
         }
         s += "]";
      }
      return s;
   }
}
