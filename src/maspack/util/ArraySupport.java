/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Collection;

/**
 * A set of static methods to test for equality of arrays
 */
public class ArraySupport {
   public static String[] concat (String[] a1, String[] a2) {
      String[] array = new String[a1.length + a2.length];
      for (int i = 0; i < a1.length; i++) {
         array[i] = a1[i];
      }
      for (int i = 0; i < a2.length; i++) {
         array[i + a1.length] = a2[i];
      }
      return array;
   }

   public static boolean equals (double[] a1, double[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (double[] a1, double[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }
   
   public static boolean equals (float[] a1, float[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
      
   public static boolean equals (float[] a1, float[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (char[] a1, char[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (char[] a1, char[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (byte[] a1, byte[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (byte[] a1, byte[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (short[] a1, short[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (short[] a1, short[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (int[] a1, int[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (int[] a1, int[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (long[] a1, long[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (long[] a1, long[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (boolean[] a1, boolean[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (boolean[] a1, boolean[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (String[] a1, String[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (String[] a1, String[] a2, int size) {
      for (int i = 0; i < size; i++) {
         if (!a1[i].equals (a2[i])) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals (Object[] a1, Object[] a2) {
      if (a1.length != a2.length) {
         return false;
      }
      return equals(a1, a2, a1.length);
   }
   
   public static boolean equals (Object[] a1, Object[] a2, int size) {
      for (int i = 0; i < size; i++) {
         Object o1 = a1[i];
         Object o2 = a2[i];
         if (((o1 == null) != (o2 == null)) ||
             ((o1 != null) && !(o1.equals(o2)))) {
            return false;
         }
      }
      return true;
   }

   public static double[] copy (double[] a1) {
      double[] ar = new double[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static float[] copy (float[] a1) {
      float[] ar = new float[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static char[] copy (char[] a1) {
      char[] ar = new char[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static byte[] copy (byte[] a1) {
      byte[] ar = new byte[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static short[] copy (short[] a1) {
      short[] ar = new short[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static int[] copy (int[] a1) {
      int[] ar = new int[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static long[] copy (long[] a1) {
      long[] ar = new long[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static boolean[] copy (boolean[] a1) {
      boolean[] ar = new boolean[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static String[] copyByReference (String[] a1) {
      String[] ar = new String[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static String[] copyByValue (String[] a1) {
      String[] ar = new String[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = new String (a1[i]);
      }
      return ar;
   }

   public static Object[] copyByReference (Object[] a1) {
      Object[] ar = new Object[a1.length];
      for (int i = 0; i < a1.length; i++) {
         ar[i] = a1[i];
      }
      return ar;
   }

   public static double[] toDoubleArray (Collection<? extends Number> col) {
      double[] array = new double[col.size()];
      int i = 0;
      for (Number n : col) {
         array[i++] = n.doubleValue();
      }
      return array;
   }

   public static float[] toFloatArray (Collection<? extends Number> col) {
      float[] array = new float[col.size()];
      int i = 0;
      for (Number n : col) {
         array[i++] = n.floatValue();
      }
      return array;
   }

   public static int[] toIntArray (Collection<? extends Number> col) {
      int[] array = new int[col.size()];
      int i = 0;
      for (Number n : col) {
         array[i++] = n.intValue();
      }
      return array;
   }

   public static void print (String name, int[] a1) {
      if (name != null) {
         System.out.print (name);
      }
      for (int i=0; i<a1.length; i++) {
         System.out.print (" " + a1[i]);
      }
      System.out.println ("");
   }

   public static void print (String name, boolean[] a1) {
      if (name != null) {
         System.out.print (name);
      }
      for (int i=0; i<a1.length; i++) {
         System.out.print (" " + (a1[i] ? "1" : "0"));
      }
      System.out.println ("");
   }

   public static String toString (int[] a1) {
      StringBuilder builder = new StringBuilder();
      for (int i=0; i<a1.length; i++) {
         if (i > 0) {
            builder.append (" ");
         }
         builder.append (a1[i]);
      }
      return builder.toString();
   }

   public static String toString (long[] a1) {
      StringBuilder builder = new StringBuilder();
      for (int i=0; i<a1.length; i++) {
         if (i > 0) {
            builder.append (" ");
         }
         builder.append (a1[i]);
      }
      return builder.toString();
   }

   public static String toString (NumberFormat fmt, double[] a1) {
      StringBuilder builder = new StringBuilder();
      for (int i=0; i<a1.length; i++) {
         if (i > 0) {
            builder.append (" ");
         }
         builder.append (fmt.format(a1[i]));
      }
      return builder.toString();
   }

   public static String toString (double[] a1) {
      return toString (new NumberFormat ("%g"), a1);
   }

   public static String toString (NumberFormat fmt, float[] a1) {
      StringBuilder builder = new StringBuilder();
      for (int i=0; i<a1.length; i++) {
         if (i > 0) {
            builder.append (" ");
         }
         builder.append (fmt.format(a1[i]));
      }
      return builder.toString();
   }

   public static String toString (float[] a1) {
      return toString (new NumberFormat ("%g"), a1);
   }

}
