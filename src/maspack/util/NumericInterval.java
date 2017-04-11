/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.IOException;
import java.io.StringReader;

import maspack.matrix.Vector;
import maspack.matrix.VectorNd;
import maspack.matrix.Vectori;
import maspack.matrix.VectorNi;

/**
 * A Range object which inspects a number to make sure it lies within a
 * prescibed interval.
 */
public abstract class NumericInterval extends RangeBase {
   protected double myUpper;
   protected boolean myUpperClosed;
   protected double myLower;
   protected boolean myLowerClosed;

   private static final double INF = Double.POSITIVE_INFINITY;

   /**
    * {@inheritDoc}
    */
   public boolean isEmpty() {
      if (myUpper > myLower) {
         return false;
      }
      else if (myUpper == myLower) {
         return (!myUpperClosed || !myLowerClosed);
      }
      else {
         return true;
      }
   }

   private void setEmpty() {
      myLower = Double.MAX_VALUE;
      myUpper = -Double.MAX_VALUE;
   }

   /** 
    * Returns true if the NumericRange r is a subset of this range.
    * 
    * @param rng Range to check
    * @return true if r is a subset of this range
    */
   public boolean contains (NumericInterval rng) {
      if (rng.myLower < myLower || rng.myUpper > myUpper) {
         return false;
      }
      else if (rng.myLower == myLower && rng.myLowerClosed && !myLowerClosed) {
         return false;
      }
      else if (rng.myUpper == myUpper && rng.myUpperClosed && !myUpperClosed) {
         return false;
      }
      else {
         return true;
      }
   }

   public void intersect (Range r) {
      if (r instanceof NumericInterval) {
         NumericInterval range = (NumericInterval)r;
         if (range.myUpper < myUpper) {
            myUpper = range.myUpper;
            myUpperClosed = range.myUpperClosed;
         }
         else if (range.myUpper == myUpper && !range.myUpperClosed) {
            myUpperClosed = false;
         }
         if (range.myLower > myLower) {
            myLower = range.myLower;
            myLowerClosed = range.myLowerClosed;
         }
         else if (range.myLower == myLower && !range.myLowerClosed) {
            myLowerClosed = false;
         }
      }
      else {
         setEmpty();
      }
   }
   
   /**
    * Returns true if this interval is closed, which is true if
    * both the lower and upper bounds are closed.
    * 
    * @return lower interval bound
    */
   public boolean isClosed() {
      return myLowerClosed && myUpperClosed;
   }

   /**
    * Returns the lower bound for the interval of this NumericRange.
    * 
    * @return lower interval bound
    */
   public double getLowerBound() {
      return myLower;
   }

   /**
    * Returns true if the lower bound for the interval of this NumericRange is
    * closed.
    * 
    * @return true if lower interval bound is closed
    */
   public boolean isLowerBoundClosed() {
      return myLowerClosed;
   }

   /**
    * Returns the upper bound for the interval of this NumericRange.
    * 
    * @return upper interval bound
    */
   public double getUpperBound() {
      return myUpper;
   }

   /**
    * Returns true if the upper bound for the interval of this NumericRange is
    * closed.
    * 
    * @return true if upper interval bound is closed
    */
   public boolean isUpperBoundClosed() {
      return myUpperClosed;
   }

   /**
    * Returns true if this range is bounded; i.e., neither the upper
    * nor the lower bounds are infinite.
    *
    * @return true if the range is bounded
    */
   public boolean isBounded() {
      return (!isEmpty() &&
              myUpper != INF &&
              myLower != -INF);
   }

   /** 
    * Returns the upper bound minus the lower bound of this NumericRange
    * 
    * @return range interval for this range
    */
   public double getRange() {
      return myUpper - myLower;
   }

   public boolean isValid (Object obj, StringHolder errMsg) {
      if (!withinRange (obj)) {
         // create special messages for some special ranges
         if (myLower == 0 && myUpper == INF) {
            if (myLowerClosed) {
               setError (errMsg, "value must be non-negative");
            }
            else {
               setError (errMsg, "value must be positive");
            }
         }
         else {
            setError (errMsg, "value must be in the range "+this);
         }
         return false;
      }
      else {
         return true;
      }
   }
   
   /**
    * Returns true if the specified object is a Number whose value lies within
    * the interval specified by this NumericRange.
    * 
    * @param obj
    * object to be tested
    * @return true if the object is a Number whose value is within range
    */
   private boolean withinRange (Object obj) {
      if (obj instanceof Number) {
         return withinRange (((Number)obj).doubleValue());
      }
      else if (obj instanceof Vector) {
         Vector vec = (Vector)obj;
         for (int i=0; i<vec.size(); i++) {
            if (!withinRange (vec.get(i))) {
               return false;
            }
         }
         return true;
      }
      else if (obj instanceof int[]) {
         int[] array = (int[])obj;
         for (int i=0; i<array.length; i++) {
            if (!withinRange (array[i])) {
               return false;
            }
         }
         return true;
      }
      else if (obj instanceof long[]) {
         long[] array = (long[])obj;
         for (int i=0; i<array.length; i++) {
            if (!withinRange ((double)array[i])) {
               return false;
            }
         }
         return true;
      }
      else if (obj instanceof float[]) {
         float[] array = (float[])obj;
         for (int i=0; i<array.length; i++) {
            if (!withinRange (array[i])) {
               return false;
            }
         }
         return true;
      }
      else if (obj instanceof double[]) {
         double[] array = (double[])obj;
         for (int i=0; i<array.length; i++) {
            if (!withinRange (array[i])) {
               return false;
            }
         }
         return true;
      }
      else {
         return false;
      }
   }

   public double makeValid (double val) {
      return clipToRange (val);
   }

   /** 
    * {@inheritDoc}
    */
   public Object makeValid (Object obj) {  
      double x, y;
      if (obj instanceof Number) {
         x = ((Number)obj).doubleValue();
         if (!withinRange (x)) {
            if (!canClipToRange(x)) {
               return Range.IllegalValue;
            }
            y = clipToRange (x);
            if (obj instanceof Integer) {
               return new Integer ((int)y);
            }
            else if (obj instanceof Long) {
               return new Long ((long)y);
            }
            else if (obj instanceof Float) {
               return new Float ((float)y);
            }
            else if (obj instanceof Double) {
               return new Double (y);
            }
            else {
               return null;
            }
         }
         else {
            return obj;
         }
      }
      else if (obj instanceof Vector) {
         Vector vec = (Vector)obj;
         for (int i=0; i<vec.size(); i++) {
            x = vec.get(i);
            if (!withinRange (x)) {
               if (!canClipToRange (x)) {
                  return Range.IllegalValue;
               }
               y = clipToRange (x);
               if (vec == obj) {
                  try {
                     vec = (Vector)vec.clone();
                  }
                  catch (Exception e) {
                     throw new InternalErrorException (
                        "Can't clone "+vec.getClass());
                  }
               }
               vec.set (i, y);
            }
         }
         return vec;
      }
      else if (obj instanceof int[]) {
         int[] array = (int[])obj;
         for (int i=0; i<array.length; i++) {
            x = array[i];
            if ((y = clipToRange (x)) != x) {
               if (array == obj) {
                  array = ArraySupport.copy (array);
               }
               array[i] = (int)y;
            }
         }
         return array;
      }
      else if (obj instanceof long[]) {
         long[] array = (long[])obj;
         for (int i=0; i<array.length; i++) {
            x = array[i];
            if ((y = clipToRange (x)) != x) {
               if (array == obj) {
                  array = ArraySupport.copy (array);
               }
               array[i] = (long)y;
            }
         }
         return array;
      }
      else if (obj instanceof float[]) {
         float[] array = (float[])obj;
         for (int i=0; i<array.length; i++) {
            x = array[i];
            if (!withinRange (x)) {
               if (!canClipToRange (x)) {
                  return Range.IllegalValue;
               }
               y = clipToRange (x);
               if (array == obj) {
                  array = ArraySupport.copy (array);
               }
               array[i] = (float)y;
            }
         }
         return array;
      }
      else if (obj instanceof double[]) {
         double[] array = (double[])obj;
         for (int i=0; i<array.length; i++) {
            x = array[i];
            if (!withinRange (x)) {
               if (!canClipToRange (x)) {
                  return Range.IllegalValue;
               }
               y = clipToRange (x);
               if (array == obj) {
                  array = ArraySupport.copy (array);
               }
               array[i] = y;
            }
         }
         return array;
      }
      else {
         return null;
      }
   }

   /**
    * Returns true if the value of a specified Number object lies within the
    * interval specified by this NumericRange.
    * 
    * @param num
    * Number to be tested
    * @return returns true if the Number's value is within range
    */
   public boolean withinRange (Number num) {
      return withinRange (num.doubleValue());
   }

   /**
    * Returns true if the specified number lies within the interval specified by
    * this NumericRange.
    * 
    * @param num
    * number to be tested
    * @return returns true if the number is within range
    */
   public boolean withinRange (double num) {
      if (myLowerClosed) {
         if (num < myLower) {
            return false;
         }
      }
      else {
         if (num <= myLower) {
            return false;
         }
      }
      if (myUpperClosed) {
         if (num > myUpper) {
            return false;
         }
      }
      else {
         if (num >= myUpper) {
            return false;
         }
      }
      return true;
   }

   /**
    * Clips a number to lie within the interval specified by this NumericRange.
    * For purposes of clipping, the interval is assumed to be closed.
    * 
    * @param num
    * number to be clipped
    * @return clipped version of the number
    */
   public double clipToRange (double num) {
      if (num <= myLower) {
         return myLower;
      }
      else if (num >= myUpper) {
         return myUpper;
      }
      else {
         return num;
      }
   }

   /**
    * Returns true if it is possible to clip a number to this range.
    * This will be the case if the number is in range, or if the
    * bound that it is outside of is closed.
    */
   public boolean canClipToRange (double num) {
      return (!((num <= myLower && !myLowerClosed) ||
                (num >= myUpper && !myUpperClosed))); 
   }
   
   /**
    * Returns true if a specified object is an instance of Number.
    * 
    * @param obj
    * object to test
    * @return true if the object is a Number
    */
   public boolean isTypeCompatible (Object obj) {
      return obj instanceof Number;
   }

   /** 
    * Merges this NumericRange with another, updating the bounds for
    * this range to enclose both values.
    * 
    * @param range range to merge with this one
    */   
   public void merge (NumericInterval range) {
      if (range.myUpper > myUpper) {
         myUpper = range.myUpper;
         myUpperClosed = range.myUpperClosed;
      }
      else if (range.myUpper == myUpper) {
         myUpperClosed = (myUpperClosed || range.myUpperClosed);
      }
      if (range.myLower < myLower) {
         myLower = range.myLower;
         myLowerClosed = range.myLowerClosed;
      }
      else if (range.myLower == myLower) {
         myLowerClosed = (myLowerClosed || range.myLowerClosed);
      }
   }

   /**
    * Returns true if a specified object is a NumericRange equivalent in value
    * to this one.
    * 
    * @param obj
    * object to compare with
    * @return true if the object is equivalent to this NumericRange
    */
   public boolean equals (Object obj) {
      try {
         return equals ((NumericInterval)obj);
      }
      catch (Exception e) {
         return false;
      }
   }

   /**
    * Returns true if a specified NumericRange is equivalent in value to this
    * one.
    * 
    * @param rng
    * numeric range to compare with
    * @return true if <code>rng</code> is equivalent to this NumericRange
    */
   public boolean equals (NumericInterval rng) {
      return (myUpper == rng.myUpper &&
              myLower == rng.myLower &&
              myLowerClosed == rng.myLowerClosed &&
              myUpperClosed == rng.myUpperClosed);
   }

   /**
    * Sets the value of this NumericRange from a string. The string format
    * should match that specified for {@link #scan scan}.
    * 
    * @param str
    * string specifying the value of this NumericRange
    */
   public void parse (String str) {
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str));
      try {
         rawscan (rtok);
      }
      catch (Exception e) {
         throw new IllegalArgumentException ("Illegal string: " + e.getMessage());
      }
   }

   /** 
    * Raw scan method that doesn't parse the enclosing brackets [ ].
    */
   protected abstract void rawscan (ReaderTokenizer rtok) throws IOException;

   /**
    * Returns a numeric object of the appropriate type (e.g., Double or Integer)
    * for the range sub-class in question.
    * 
    * @param num
    * value that the numeric object should be set to
    */
   protected abstract Number getNumber (double num);

   /**
    * Validates a numeric object by checking that it lies within this range
    * interval. For purposes of this method, the interval is treated as closed.
    * If the object's number is within range, the object is returned unchanged.
    * Otherwise, the method returns either a new clipped version of the number
    * (if <code>clip</code> is <code>true</code>), or the special value
    * {@link Range#IllegalValue}. In
    * the latter two cases, an error message will also be returned if the
    * variable <code>errMsg</code> is non-null.
    * 
    * @param value
    * numeric object to validate
    * @param clip
    * if true, clip the number to the range if possib;e
    * @param errMsg
    * if non-null, is used to return an error message if the number is out of
    * range
    * @return either the original number, a clipped version of it, or
    * Range.IllegalValue
    */
   public Object validate (Number value, boolean clip, StringHolder errMsg) {
            double num = value.doubleValue();
      if (withinRange (num)) {
         return value;
      }
      else if (clip && canClipToRange (num)) {
         setError (errMsg, "value was clipped to range");
         return getNumber (clipToRange(num));
      }
      else {
         setError (errMsg, "value should be within the range " + toString());
         return Range.IllegalValue;
      }
   }

   /**
    * Validates a vector by checking that its elements lie within this range
    * interval. For purposes of this method, the interval is treated as closed.
    * If all elements are within range, the vector is returned unchanged.
    * Otherwise, the method returns either a new clipped vector (if
    * <code>clip</code> is <code>true</code>), or the special value
    * {@link Range#IllegalValue}. In
    * the latter two cases, an error message will also be returned if the
    * variable <code>errMsg</code> is non-null.
    * 
    * @param vec
    * vector to validate
    * @param clip
    * if true, clip the vector to the range if possible
    * @param errMsg
    * if non-null, is used to return an error message if one or more elements
    * are out of range
    * @return either the original vector, a clipped version of it, or
    * Range.IllegalValue
    */
   public Object validate (Vector vec, boolean clip, StringHolder errMsg) {
      boolean vectorCopied = false;
      for (int i = 0; i < vec.size(); i++) {
         double num = vec.get(i);
         if (!withinRange(num)) {
            if (clip && canClipToRange (num)) {
               if (!vectorCopied) {
                  vec = new VectorNd (vec);
                  vectorCopied = true;
               }
               vec.set (i, clipToRange(num));
            }
            else {
               setError (errMsg, "elements should be within the range "+this);
               return Range.IllegalValue;
            }
         }
      }
      if (vectorCopied) {
         setError (errMsg, "vector was clipped to range");
      }
      return vec;
   }

   /**
    * Validates an integer vector by checking that its elements lie within this
    * range interval. For purposes of this method, the interval is treated as
    * closed.  If all elements are within range, the vector is returned
    * unchanged.  Otherwise, the method returns either a new clipped vector (if
    * <code>clip</code> is <code>true</code>), or the special value {@link
    * Range#IllegalValue}. In the latter two cases, an error message will also
    * be returned if the variable <code>errMsg</code> is non-null.
    * 
    * @param vec
    * vector to validate
    * @param clip
    * if true, clip the vector to the range if possible
    * @param errMsg
    * if non-null, is used to return an error message if one or more elements
    * are out of range
    * @return either the original vector, a clipped version of it, or
    * Range.IllegalValue
    */
   public Object validate (Vectori vec, boolean clip, StringHolder errMsg) {
      boolean vectorCopied = false;
      for (int i = 0; i < vec.size(); i++) {
         double num = vec.get(i);
         if (!withinRange(num)) {
            if (clip && canClipToRange (num)) {
               if (!vectorCopied) {
                  vec = new VectorNi (vec);
                  vectorCopied = true;
               }
               vec.set (i, (int)clipToRange(num));
            }
            else {
               setError (errMsg, "elements should be within the range "+this);
               return Range.IllegalValue;
            }
         }
      }
      if (vectorCopied) {
         setError (errMsg, "vector was clipped to range");
      }
      return vec;
   }

//   /**
//    * Validates a NumericRange to ensure that the lower bound does not exceed
//    * the upper bound.
//    */
//   public static Object validate (NumericRange range, StringHolder errMsg) {
//      String err = null;
//      if (range.getLowerBound() > range.getUpperBound()) {
//         return PropertyUtils.illegalValue (
//            "minimum must not be greater than maximum", errMsg);
//      }
//      return PropertyUtils.validValue (range, errMsg);
//   }  

   public NumericInterval clone() {
      return (NumericInterval)super.clone();
   }  

   public void scale (double s) {
      myUpper *= s;
      myLower *= s;
   }
}
