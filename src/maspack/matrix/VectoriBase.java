/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;

/**
 * Base implementation of {@link maspack.matrix.Vectori Vectori}.
 */
public abstract class VectoriBase implements Vectori {
   protected static NumberFormat myDefaultFmt = new NumberFormat ("%d");

   /**
    * Sets the default format string used in {@link #toString() toString}. For
    * a description of the format string syntax, see {@link
    * maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * new format string
    * @throws IllegalArgumentException
    * if the format string is invalid
    * @see #getDefaultFormat
    */
   public static void setDefaultFormat (String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      myDefaultFmt = fmt;
   }

   /**
    * Returns the default format string used in {@link #toString() toString}. If
    * unset, this string is "%d". For a description of the format string syntax,
    * see {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @return Default format string
    */
   public static String getDefaultFormat() {
      return myDefaultFmt.toString();
   }

   /**
    * {@inheritDoc}
    */
   public abstract int size();

   /**
    * {@inheritDoc}
    */
   public abstract int get (int i);

   /**
    * {@inheritDoc}
    */
   public void get (int[] values) {
       if (values.length < size()) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= "+size());
      }      
      for (int i = 0; i < size(); i++) {
         values[i] = get(i);
      }     
   }

   /**
    * {@inheritDoc}
    */
   public abstract void set (int i, int value);

   /**
    * {@inheritDoc}
    */
   public void set (int[] values) {
      if (isFixedSize()) {
         if (values.length < size()) {
            throw new IllegalArgumentException (
               "argument 'values' must have a length of at least "+size());
         }
      }
      else {
         if (values.length != size()) {
            setSize (values.length);
         }
      }
      for (int i = 0; i < size(); i++) {
         set (i, values[i]);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (Vectori v) {
      if (v.size() != size()) {
         if (isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            setSize (v.size());
         }
      }
      for (int i = 0; i < size(); i++) {
         set (i, v.get (i));
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isFixedSize() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void setSize (int n) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public double norm() {
      return Math.sqrt (normSquared());
   }

   static double computeNormSquared (Vectori v) {
      double sum = 0;
      for (int i = 0; i < v.size(); i++) {
         double x = v.get (i);
         sum += x * x;
      }
      return sum;
   }

   /**
    * {@inheritDoc}
    */
   public double normSquared() {
      return computeNormSquared (this);
   }

   static int computeOneNorm (Vectori v) {
      int sum = 0;
      for (int i = 0; i < v.size(); i++) {
         int x = v.get (i);
         sum += (x >= 0 ? x : -x);
      }
      return sum;
   }

   /**
    * {@inheritDoc}
    */
   public int oneNorm() {
      return computeOneNorm (this);
   }

   static int computeInfinityNorm (Vectori v) {
      int max = 0;
      for (int i = 0; i < v.size(); i++) {
         int x = v.get (i);
         if (x < 0) {
            x = -x;
         }
         if (x > max) {
            max = x;
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public int infinityNorm() {
      return computeInfinityNorm (this);
   }

   static int getMaxElement (Vectori v) {
      int max = 0;
      if (v.size() > 0) {
         max = v.get(0);
      }
      for (int i = 1; i < v.size(); i++) {
         int x = v.get (i);
         if (x > max) {
            max = x;
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public int maxElement() {
      return getMaxElement (this);
   }

   static int getMinElement (Vectori v) {
      int min = 0;
      if (v.size() > 0) {
         min = v.get(0);
      }
      for (int i = 1; i < v.size(); i++) {
         int x = v.get (i);
         if (x < min) {
            min = x;
         }
      }
      return min;
   }

   /**
    * {@inheritDoc}
    */
   public int minElement() {
      return getMinElement (this);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Vectori v1) {
      if (v1.size() != size()) {
         return false;
      }
      for (int i = 0; i < size(); i++) {
         if (get (i) != v1.get (i)) {
            return false;
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt) throws IOException {
      write (pw, fmt, /* withBrackets= */false);
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, boolean withBrackets)
      throws IOException {
      if (withBrackets) {
         pw.print ("[ ");
      }
      String str;
      if (fmt.isFloatingPoint()) {
         for (int i = 0; i < size(); i++) {
            if (i < size() - 1) {
               str = fmt.format (get (i)) + " ";
            }
            else {
               str = fmt.format (get (i));
            }
            pw.print (str);
         }
      }
      else {
         for (int i = 0; i < size(); i++) {
            if (i < size() - 1) {
               str = get(i) + " ";
            }
            else {
               str = get(i) + "";
            }
            pw.print (str);
         }
      }
      if (withBrackets) {
         pw.print (" ]");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      if (rtok.nextToken() == '[') {
         if (isFixedSize()) {
            for (int i = 0; i < size(); i++) {
               set (i, rtok.scanInteger());
            }
            rtok.scanToken (']');
         }
         else {
            ArrayList<Integer> valueList = new ArrayList<Integer> (64);
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               valueList.add (new Integer (rtok.scanInteger()));
            }
            if (valueList.size() != size()) {
               setSize (valueList.size());
            }
            for (int i=0; i<valueList.size(); i++) {
               set (i, valueList.get(i));
            }
         }
      }
      else {
         rtok.pushBack();
         for (int i = 0; i < size(); i++) {
            set (i, rtok.scanInteger());
         }
      }
   }

   /**
    * Returns a String representation of this vector, using the default format
    * returned by {@link #getDefaultFormat getDefaultFormat}.
    * 
    * @return String representation of this vector
    * @see #toString(String)
    */
   public String toString() {
      return toString (new NumberFormat(myDefaultFmt));
   }

   /**
    * {@inheritDoc}
    */
   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }
   
   /**
    * {@inheritDoc}
    */
   public String toString (NumberFormat fmt) {
      StringBuffer buf = new StringBuffer (20 * size());
      int size = size();
      for (int i = 0; i < size; i++) {
         // debugging added because of race condition in widget
         String str = null;
         try {
            str = fmt.format (get (i));
         }
         catch (Exception e) {
            System.out.println ("Vector3i.toString() failed");
            System.out.println (e.getMessage());
            System.out.println ("fmt=" + fmt + " i=" + " get(i)=" + get(i));
            str = "0";
         }
         buf.append (str);
         if (i < size - 1) {
            buf.append (' ');
         }
      }
      return buf.toString();
   }

   /**
    * Sets the elements of this vector to uniformly distributed random values in
    * the range -1000 (inclusive) to 1000 (exclusive).
    */
   void setRandom() {
      setRandom (-1000, 1000, RandomGenerator.get());
   }

   /**
    * Sets the elements of this vector to uniformly distributed random values in
    * a specified range.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    */
   void setRandom (int lower, int upper) {
      setRandom (lower, upper, RandomGenerator.get());
   }

   /**
    * Sets the elements of this vector to uniformly distributed random values in
    * a specified range, using a supplied random number generator.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    */
   void setRandom (int lower, int upper, Random generator) {
      int rng = upper - lower;
      for (int i=0; i<size(); i++) {
         set (i, generator.nextInt (rng) + lower);
      }
   }

   public VectoriBase clone() {
      try {
         return (VectoriBase)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for "+getClass());
      }      
   }

}
