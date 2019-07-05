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
public class DoubleInterval extends NumericInterval {

   public static DoubleInterval Positive = new DoubleInterval ("(0,inf]");
   public static DoubleInterval NonNegative = new DoubleInterval ("[0,inf]");

   /**
    * Creates a DoubleRange which accepts any double value.
    */
   public DoubleInterval() {
      myUpper = Double.POSITIVE_INFINITY;
      myUpperClosed = true;
      myLower = Double.NEGATIVE_INFINITY;
      myLowerClosed = true;
   }

   /**
    * Creates a new DoubleRange that accepts any value in the closed interval
    * [lower, upper].
    * 
    * @param lower
    * interval lower bound
    * @param upper
    * interval upper bound
    */
   public DoubleInterval (double lower, double upper) {
      this();
      set (lower, upper);
   }

   /**
    * Creates a new DoubleRange which is a copy of an existing NumericRange one.
    * 
    * @param rng
    * range to copy
    */
   public DoubleInterval (NumericInterval rng) {
      this();
      set (rng);
   }

   /**
    * Creates a new DoubleRange from a specification string. For the string
    * syntax, see {@link #scan scan}.
    * 
    * @param str
    * specification string for this DoubleRange
    * @throws IllegalArgumentException
    * if the string is not in the correct format.
    */
   public DoubleInterval (String str) throws IllegalArgumentException {
      this();
      parse (str);
   }

   /**
    * Sets the lower bound for the interval of this DoubleRange.
    * 
    * @param lower
    * new lower interval bound
    */
   public void setLowerBound (double lower) {
      myLower = lower;
   }

   /**
    * Sets the lower bound for the interval of this DoubleRange to be closed or
    * open.
    * 
    * @param closed
    * if true, sets the lower interval bound to be closed
    */
   public void setLowerBoundClosed (boolean closed) {
      myLowerClosed = closed;
   }

   /**
    * Sets the upper bound for the interval of this DoubleRange.
    * 
    * @param upper
    * new upper interval bound
    */
   public void setUpperBound (double upper) {
      myUpper = upper;
   }

   /**
    * Sets the upper bound for the interval of this DoubleRange to be closed or
    * open.
    * 
    * @param closed
    * if true, sets the upper interval bound to be closed
    */
   public void setUpperBoundClosed (boolean closed) {
      myUpperClosed = closed;
   }

   /**
    * Sets this DoubleRange so that it accepts any value in the closed interval
    * [lower, upper].
    * 
    * @param lower
    * interval lower bound
    * @param upper
    * interval upper bound
    */
   public void set (double lower, double upper) {
      myUpper = upper;
      myUpperClosed = true;
      myLower = lower;
      myLowerClosed = true;
   }

   /**
    * Sets this DoubleRange to the value of any NumericRange.
    * 
    * @param rng
    * numeric range to copy
    */
   public void set (NumericInterval rng) {
      myUpper = rng.myUpper;
      myUpperClosed = rng.myUpperClosed;
      myLower = rng.myLower;
      myLowerClosed = rng.myLowerClosed;
   }

   /**
    * Sets this DoubleRange so that it accepts any value within a specified
    * interval. The lower and upper bounds of the interval may be either open or
    * closed, as specified by a string argument which should be set to one of
    * four values:
    * 
    * <ul>
    * <li>"[]" lower and upper bounds both closed
    * <li>"[)" lower bound closed, upper bound open
    * <li>"(]" lower bound open, upper bound closed
    * <li>"()" lower and upper bounds both open
    * </ul>
    * 
    * @param lower
    * interval lower bound
    * @param upper
    * interval upper bound
    * @param closure
    * specifies whether the lower and upper bounds are open or closed
    * @throws IllegalArgumentException
    * if the closure specification string is not one of "[]", "[)", "(]", or
    * "()".
    */
   public void set (double lower, double upper, String closure) {
      if (closure.equals ("[]")) {
         myLowerClosed = true;
         myUpperClosed = true;
      }
      else if (closure.equals ("[)")) {
         myLowerClosed = true;
         myUpperClosed = false;
      }
      else if (closure.equals ("(]")) {
         myLowerClosed = false;
         myUpperClosed = true;
      }
      else if (closure.equals ("()")) {
         myLowerClosed = false;
         myUpperClosed = false;
      }
      else {
         throw new IllegalArgumentException (
            "closure string not one of \"[]\", \"[)\", \"(]\", or \"()\"");
      }
      myUpper = upper;
      myLower = lower;
   }

//   /** 
//    * {@inheritDoc}
//    */
//   public Object projectToRange (Object obj) {
//      if (!withinRange (obj)) {
//         if (obj instanceof Number) {
//            return new Double (clipToRange (((Number)obj).doubleValue()));
//         }
//         else {
//            return null;
//         }
//      }
//      else {
//         return obj;
//      }
//   }

   private String doubleToString (double x) {
      if (x == Double.NEGATIVE_INFINITY) {
         return "-inf";
      }
      else if (x == Double.POSITIVE_INFINITY) {
         return "inf";
      }
      else {
         return Double.toString (x);
      }
   }

//   /** 
//    * {@inheritDoc}
//    */   
//   public String getDescription() {
//      return "Value must lie in the range " + toString();
//   }

   public String toString() {
      StringBuffer buf = new StringBuffer (256);
      buf.append (myLowerClosed ? '[' : '(');
      buf.append (doubleToString (myLower));
      buf.append (',');
      buf.append (doubleToString (myUpper));
      buf.append (myUpperClosed ? ']' : ')');
      return buf.toString();
   }      

   protected void rawscan (ReaderTokenizer rtok) throws IOException {
      int token;
      if ((token = rtok.nextToken()) == '[') {
         myLowerClosed = true;
      }
      else if (token == '(') {
         myLowerClosed = false;
      }
      else {
         throw new IOException ("Either '[' or '(' expected, got " + rtok);
      }
      if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
         throw new IOException ("Lower bound number expected, got " + rtok);
      }
      myLower = rtok.nval;
      if (rtok.nextToken() != ',') {
         throw new IOException ("',' expected, got " + rtok);
      }
      if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
         throw new IOException ("Upper bound number expected, got " + rtok);
      }
      myUpper = rtok.nval;
      if ((token = rtok.nextToken()) == ']') {
         myUpperClosed = true;
      }
      else if (token == ')') {
         myUpperClosed = false;
      }
      else {
         throw new IOException ("Either ']' or ')' expected, got " + rtok);
      }     
   }
   
   /**
    * Scans this element from a ReaderTokenizer. The input should consist of
    * five tokens, which specifiy the interval as follows:
    * 
    * <ul>
    * <li>A '<code>[</code>' or '<code>(</code>' character, depending
    * on whether the lower bound is closed or open;
    * 
    * <li>A floating point number describing the lower bound;
    * 
    * <li>A '<code>,</code>' character
    * 
    * <li>A floating point number described the upper bound;
    * 
    * <li>A '<code>]</code>' or '<code>)</code>' character, depending
    * on whether the upper bound is closed or open;
    * 
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
      pw.print ("[ ");
      pw.print (myLowerClosed ? '[' : '(');
      pw.print (fmt.format (myLower));
      pw.print (',');
      pw.print (fmt.format (myUpper));
      pw.print (myUpperClosed ? ']' : ')');
      pw.println (" ]");
   }

   protected Number getNumber (double num) {
      return new Double (num);
   }

   public DoubleInterval clone() {
      return (DoubleInterval)super.clone();
   }

}
