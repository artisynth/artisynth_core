/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * A Range object which inspects a number to make sure it lies within a
 * prescibed interval.
 */
public class IntegerInterval extends NumericInterval {
   /**
    * Creates a IntRange which accepts any integer value.
    */
   public IntegerInterval() {
      myUpper = Integer.MAX_VALUE;
      myUpperClosed = true;
      myLower = Integer.MIN_VALUE;
      myLowerClosed = true;
   }

   /**
    * Creates a new IntRange that accepts any value in the closed interval
    * [lower, upper].
    * 
    * @param lower
    * interval lower bound
    * @param upper
    * interval upper bound
    */
   public IntegerInterval (int lower, int upper) {
      this();
      set (lower, upper);
   }

   /**
    * Creates a new IntRange which is a copy of an existing one.
    * 
    * @param rng
    * range to copy
    */
   public IntegerInterval (IntegerInterval rng) {
      this();
      set (rng);
   }
   
   /**
    * Creates a new IntegerRange which is a copy of an existing NumericRange one.
    * 
    * @param rng
    * range to copy
    */
   public IntegerInterval (NumericInterval rng) {
      this();
      set (rng);
   }

   /**
    * Creates a new IntRange from a specification string. For the string syntax,
    * see {@link #scan scan}.
    * 
    * @param str
    * specification string for this IntRange
    * @throws IllegalArgumentException
    * if the string is not in the correct format.
    */
   public IntegerInterval (String str) throws IllegalArgumentException {
      this();
      parse (str);
   }

   /**
    * Sets the lower bound for the interval of this IntRange.
    * 
    * @param lower
    * new lower interval bound
    */
   public void setLowerBound (int lower) {
      myLower = lower;
   }

   /**
    * Sets the upper bound for the interval of this IntRange.
    * 
    * @param upper
    * new upper interval bound
    */
   public void setUpperBound (int upper) {
      myUpper = upper;
   }

   /**
    * Sets this IntRange so that it accepts any value in the closed interval
    * [lower, upper].
    * 
    * @param lower
    * interval lower bound
    * @param upper
    * interval upper bound
    */
   public void set (int lower, int upper) {
      myUpper = upper;
      myLower = lower;
   }

   /**
    * Sets this IntRange from the value of an existing NumericInterval.
    * 
    * @param rng
    * numeric range to copy
    */
   public void set (NumericInterval rng) {
      myUpper = rng.myUpper;
      myLower = rng.myLower;
   }

//   /** 
//    * {@inheritDoc}
//    */
//   public Object projectToRange (Object obj) {
//      if (!withinRange (obj)) {
//         if (obj instanceof Number) {
//            return new Integer ((int)clipToRange (((Number)obj).doubleValue()));
//         }
//         else {
//            return null;
//         }
//      }
//      else {
//         return obj;
//      }
//   }

//   /** 
//    * {@inheritDoc}
//    */
//   public String getDescription() {
//      return "Value must be an integer in the range " + toString();
//   }

   public String toString() {
      StringBuffer buf = new StringBuffer (64);
      buf.append ('[');
      buf.append (Integer.toString ((int)myLower));
      buf.append (',');
      buf.append (Integer.toString ((int)myUpper));
      buf.append (']');
      return buf.toString();
   }
   
   protected void rawscan (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      myLower = (int)rtok.scanInteger();
      rtok.scanToken (',');
      myUpper = (int)rtok.scanInteger();
      rtok.scanToken (']');     
   }
   
   /**
    * Scans this element from a ReaderTokenizer. The input should consist of
    * five tokens, which specifiy the interval as follows:
    * 
    * <ul>
    * <li>A '<code>[</code>' character;
    * <li>An integer describing the lower bound;
    * <li>A '<code>,</code>' character;
    * <li>A integer described the upper bound;
    * <li>A '<code>]</code>' character;
    * </ul>
    * 
    * @param rtok
    * Tokenizer from which to scan the element
    * @param ref
    * reference object (not used, may be null)
    * @throws IOException
    * if an I/O or formatting error occured
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      rawscan (rtok);
      rtok.scanToken (']');
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.print ("[ [");
      pw.print (Integer.toString ((int)myLower));
      pw.print (',');
      pw.print (Integer.toString ((int)myUpper));
      pw.println ("] ]");
   }

   protected Number getNumber (double num) {
      return new Integer ((int)num);
   }

   /**
    * Validates an array of integers by checking that its elements lie within
    * this range interval. If all elements are within range, the array is
    * returned unchanged. Otherwise, the method returns either a new clipped
    * array (if <code>clip</code> is <code>true</code>), or the special
    * value {@link Range#IllegalValue}.
    * In the latter two cases, an error message will
    * also be returned if the variable <code>errMsg</code> is non-null.
    * 
    * @param values
    * array to validate
    * @param clip
    * if true, clip the array to the range
    * @param errMsg
    * if non-null, is used to return an error message if one or more elements
    * are out of range
    * @return either the original array, a clipped version of it, or
    * Range.IllegalValue
    */
   public Object validate (int[] values, boolean clip, StringHolder errMsg) {
      boolean arrayCopied = false;
      for (int i = 0; i < values.length; i++) {
         int clippedNum = (int)clipToRange (values[i]);
         if (clippedNum != values[i]) {
            if (!clip) {
               if (errMsg != null) {
                  errMsg.value =
                     "elements should be within the range " + toString();
               }
               return Range.IllegalValue;
            }
            else {
               if (!arrayCopied) {
                  values = ArraySupport.copy (values);
                  arrayCopied = true;
               }
               values[i] = clippedNum;
            }
         }
      }
      if (arrayCopied) {
         if (errMsg != null) {
            errMsg.value = "array was clipped to range";
         }
      }
      return values;
   }

   public IntegerInterval clone() {
      return (IntegerInterval)super.clone();
   }


}
