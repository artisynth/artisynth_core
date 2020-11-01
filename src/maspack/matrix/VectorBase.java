/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

import maspack.util.ReaderTokenizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import maspack.util.NumberFormat;
import maspack.util.InternalErrorException;
import maspack.util.RandomGenerator;

/**
 * Base implementation of {@link maspack.matrix.Vector Vector}.
 */
public abstract class VectorBase implements Vector {
   protected static NumberFormat myDefaultFmt = new NumberFormat ("%g");
   protected static boolean myColVecsVerticalP = false;

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
    * unset, this string is "%g". For a description of the format string syntax,
    * see {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @return Default format string
    */
   public static String getDefaultFormat() {
      return myDefaultFmt.toString();
   }

   /**
    * Sets the string representation of column vectors to be vertical. By
    * default, this is <code>false</code>.
    * 
    * @param enable
    * if true, column vector strings are arranged vertically.
    * @see #isColumnVectorStringsVertical
    */
   public static void setColumnVectorStringsVertical (boolean enable) {
      myColVecsVerticalP = enable;
   }

   /**
    * Returns true if the string representation of column vectors is vertical.
    * 
    * @return true if column vector strings are arranged vertically.
    * @see #setColumnVectorStringsVertical
    */
   public static boolean isColumnVectorStringsVertical() {
      return myColVecsVerticalP;
   }

   /**
    * {@inheritDoc}
    */
   public abstract int size();

   /**
    * {@inheritDoc}
    */
   public abstract double get (int i);

   /**
    * {@inheritDoc}
    */
   public void get (double[] values) {
      if (values.length < size()) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= "+size());
      }      
      for (int i = 0; i < size(); i++) {
         values[i] = get (i);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void get (Vector v1) throws ImproperSizeException {
      if (v1.size() != size()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size(); i++) {
         v1.set (i, get (i));
      }
   }

   /**
    * {@inheritDoc}
    */
   public abstract void set (int i, double value);

   /**
    * {@inheritDoc}
    */
   public void set (double[] values) {
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
   public void set (Vector v) {
      if (v.size() != size()) {
         if (isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            setSize (v.size());
         }
      }
      if (v instanceof VectorBase) {
         boolean isRowVector = ((VectorBase)v).isRowVector();
         if (!setRowVector (isRowVector)) {
            String type = isRowVector ? "row" : "column";
            throw new ImproperSizeException ("cannot convert vector into a "
            + type + " vector");
         }
      }
      for (int i = 0; i < size(); i++) {
         set (i, v.get (i));
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      boolean isRow;
      int size;

      if (M.colSize() == 1) {
         isRow = false;
         if (!setRowVector (false)) {
            throw new ImproperSizeException (
               "vector cannot be converted to a column vector");
         }
         size = M.rowSize();
      }
      else if (M.rowSize() == 1) {
         isRow = true;
         if (!setRowVector (true)) {
            throw new ImproperSizeException (
               "vector cannot be converted to a row vector");
         }
         size = M.colSize();
      }
      else {
         throw new ImproperSizeException (
            "matrix is not a row or column vector");
      }
      if (size != size()) {
         if (isFixedSize()) {
            throw new ImproperSizeException();
         }
         else {
            setSize (size);
         }
      }
      if (isRow) {
         for (int i = 0; i < size(); i++) {
            set (i, M.get (0, i));
         }
      }
      else {
         for (int i = 0; i < size(); i++) {
            set (i, M.get (i, 0));
         }
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

   static double computeNormSquared (Vector v) {
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

   static double computeOneNorm (Vector v) {
      double sum = 0;
      for (int i = 0; i < v.size(); i++) {
         double x = v.get (i);
         sum += (x >= 0 ? x : -x);
      }
      return sum;
   }

   /**
    * {@inheritDoc}
    */
   public double oneNorm() {
      return computeOneNorm (this);
   }

   static double computeInfinityNorm (Vector v) {
      double max = 0;
      for (int i = 0; i < v.size(); i++) {
         double x = v.get (i);
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
   public double infinityNorm() {
      return computeInfinityNorm (this);
   }

   static double getMaxElement (Vector v) {
      double max = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < v.size(); i++) {
         double x = v.get (i);
         if (x > max) {
            max = x;
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public double maxElement() {
      return getMaxElement (this);
   }

   static double getMinElement (Vector v) {
      double min = Double.POSITIVE_INFINITY;
      for (int i = 0; i < v.size(); i++) {
         double x = v.get (i);
         if (x < min) {
            min = x;
         }
      }
      return min;
   }

   /**
    * {@inheritDoc}
    */
   public double minElement() {
      return getMinElement (this);
   }

   /**
    * {@inheritDoc}
    */
   public double dot (Vector v1) throws ImproperSizeException {
      if (v1.size() != size()) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      double sum = 0;
      for (int i = 0; i < size(); i++) {
         sum += get (i) * v1.get (i);
      }
      return sum;
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Vector v1, double eps) {
      if (v1.size() != size()) {
         return false;
      }
      for (int i = 0; i < size(); i++) {
         if (!(Math.abs (get (i) - v1.get (i)) <= eps)) {
            return false;
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Vector v1) {
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

   // Dec 9, 2008. John Lloyd: removed hashCode/equals override, since it was
   // causing confusion. For now equals (Object obj) should return true only if
   // the objects are identical. If equals based on contents are required, then
   // one should create a subclass.
   // /**
   // * Returns true if this vector and a specified object
   // * have the same class type and if all the elements are
   // * exactly equal.
   // *
   // * @param obj object to compare with
   // * @return false if the objects are not equal
   // */
   // public boolean equals (Object obj)
   // {
   // if (!getClass().isInstance(obj) ||
   // !obj.getClass().isInstance(this))
   // { return false;
   // }
   // Vector vec = (Vector)obj;
   // if (vec.size() != size())
   // { return false;
   // }
   // boolean isEqual = false;
   // try
   // { isEqual = equals(vec);
   // }
   // catch (Exception e)
   // { // can't happen
   // }
   // return isEqual;
   // }

   // /**
   // * Computes hash code based on all vector elements.
   // *
   // * @return hash code based on all vector elements
   // */
   // public int hashCode()
   // {
   // final int PRIME = 31;
   // int result = 1;
   // long temp;
   // for (int i = 0; i < size(); i++)
   // {
   // temp = Double.doubleToLongBits(get(i));
   // result = PRIME * result + (int) (temp ^ (temp >>> 32));
   // }
   // return result;
   // }

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
    * Returns a String representation of this vector, in which each element is
    * formatted using a C <code>printf</code> style format string. For a
    * description of the format string syntax, see {@link
    * maspack.util.NumberFormat NumberFormat}. Note that when called numerous
    * times, {@link #toString(NumberFormat) toString(NumberFormat)} will be more
    * efficient because the {@link maspack.util.NumberFormat NumberFormat} will
    * not need to be recreated each time from a specification string.
    * 
    * @param fmtStr
    * numeric format specification
    * @return String representation of this vector
    * @see #isColumnVectorStringsVertical
    */
   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   /**
    * {@inheritDoc}
    * 
    * @see #isColumnVectorStringsVertical
    */
   public String toString (NumberFormat fmt) {
      StringBuffer buf = new StringBuffer (20 * size());
      boolean vertical = (myColVecsVerticalP && !isRowVector());
      int size = size();
      for (int i = 0; i < size; i++) {
         buf.append (fmt.format (get (i)));
         if (i < size - 1) {
            buf.append (vertical ? '\n' : ' ');
         }
      }
      return buf.toString();
   }

   public void writeToFile (String fileName, String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      try {
         PrintWriter pw =
            new PrintWriter (new BufferedWriter (new FileWriter (fileName)));
         for (int i=0; i<size(); i++) {
            pw.println (fmt.format(get(i)));
         }
         pw.close();
      }
      catch (Exception e) {
         System.out.println ("Error writing vector to file "+ fileName + ":");
         System.out.println (e);
      }
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
      for (int i = 0; i < size(); i++) {
         String str;
         if (i < size() - 1) {
            str = fmt.format (get (i)) + " ";
         }
         else {
            str = fmt.format (get (i));
         }
         pw.print (str);
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
               set (i, rtok.scanNumber());
            }
            rtok.scanToken (']');
         }
         else {
            ArrayList<Double> valueList = new ArrayList<>(64);
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               valueList.add (new Double (rtok.scanNumber()));
            }
            if (valueList.size() != size()) {
               setSize (valueList.size());
            }
            Iterator<Double> it = valueList.iterator();
            for (int i = 0; i < size(); i++) {
               set (i, it.next().doubleValue());
            }
         }
      }
      else {
         rtok.pushBack();
         for (int i = 0; i < size(); i++) {
            set (i, rtok.scanNumber());
         }
      }
   }

   public boolean isWritable() {
      return true;
   }

   public void scan (ReaderTokenizer rtok, Object obj)
      throws IOException {
      scan (rtok);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object obj)
      throws IOException {
      write (pw, fmt);
   }

   /**
    * Sets the elements of this vector to uniformly distributed random values in
    * the range -0.5 (inclusive) to 0.5 (exclusive).
    */
   protected void setRandom() {
      setRandom (-0.5, 0.5, RandomGenerator.get());
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
   protected void setRandom (double lower, double upper) {
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
   protected void setRandom (double lower, double upper, Random generator) {
      double range = upper - lower;

      for (int i = 0; i < size(); i++) {
         set (i, generator.nextDouble() * range + lower);
      }
   }

   /**
    * Returns true if any element of this vector is not a number.
    * 
    * @return true if any element is NaN
    */
   public boolean containsNaN() {
      for (int i = 0; i < size(); i++) {
         if (get (i) != get (i)) {
            return true;
         }
      }
      return false;
   }

   public VectorNd copy() {
      return new VectorNd (this);
   }

   public VectorNd copyAndNegate() {
      VectorNd tmp = new VectorNd (this);
      tmp.negate();
      return tmp;
   }

   public VectorNd copyAndAdd (Vector v2) {
      VectorNd res = new VectorNd (this);
      double[] buf = res.getBuffer();
      for (int i = 0; i < this.size(); i++) {
         buf[i] += v2.get (i);
      }
      return res;
   }

   public VectorNd copyAndSub (Vector v2) {
      VectorNd res = new VectorNd (this);
      double[] buf = res.getBuffer();
      for (int i = 0; i < this.size(); i++) {
         buf[i] -= v2.get (i);
      }
      return res;
   }

   public VectorNd copyAndSubLeft (Vector v2) {
      VectorNd res = new VectorNd (v2);
      double[] buf = res.getBuffer();
      for (int i = 0; i < this.size(); i++) {
         buf[i] -= get (i);
      }
      return res;
   }

   public VectorNd copyAndScale (double s) {
      VectorNd res = new VectorNd (this);
      res.scale (s);
      return res;
   }

   public boolean isRowVector() {
      return false;
   }

   public boolean setRowVector (boolean isRow) {
      return (isRow == false);
   }

   /**
    * Returns true if one or more elements of this vector is NaN.
    * 
    * @return true if one or more elements is NaN
    */
   public boolean hasNaN() {
      for (int i = 0; i < size(); i++) {
         if (Double.isNaN (get (i))) {
            return true;
         }
      }
      return false;
   }

   protected double abs (double x) {
      return x >= 0 ? x : -x;
   }

   public VectorBase clone() {
      try {
         return (VectorBase)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for "+getClass());
      }      
   }
}
