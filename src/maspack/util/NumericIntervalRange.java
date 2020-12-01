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
 * A base class for creating Range objects.
 */
public class NumericIntervalRange extends RangeBase {

   NumericInterval myMaxRange;
   boolean myAllowEmpty = true;

   public NumericIntervalRange () {
      this (null, /*allowEmpty=*/true);
   }

   public NumericIntervalRange (NumericInterval maxRange) {
      this (maxRange, /*allowEmpty=*/true);
   }

   public NumericIntervalRange (NumericInterval maxRange, boolean allowEmpty) {
      if (maxRange != null) {
         myMaxRange = maxRange.clone();
      }
      else {
         myMaxRange = null;
      }
      myAllowEmpty = allowEmpty;
   }

   /** 
    * {@inheritDoc}
    */   
   public boolean isValid (Object obj, StringHolder errMsg) {
      if (obj instanceof NumericInterval) {
         NumericInterval rng = (NumericInterval)obj;
         if (myAllowEmpty) {
            if (rng.myUpper < rng.myLower) {
               setError (errMsg, "lower bound must not exceed upper bound");
               return false;
            }
         }
         else {
            if (rng.myUpper <= rng.myLower) {
               setError (errMsg, "lower bound must be less then upper bound");
               return false;
            }
         }
         if (myMaxRange != null) {
            if (!myMaxRange.contains (rng)) {
               setError (errMsg, "range must lie within "+myMaxRange);
               return false;
            }
         }
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isEmpty() {
      if (myEmptyP) {
         return true;
      }
      else if (myMaxRange != null) {
         return myMaxRange.isEmpty();
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void intersect (Range r) {
      if (r instanceof NumericIntervalRange) {
         NumericIntervalRange nrr = (NumericIntervalRange)r;
         if (nrr.myMaxRange != null) {
            if (myMaxRange == null) {
               myMaxRange = nrr.myMaxRange.clone();
            }
            else {
               myMaxRange.intersect (nrr.myMaxRange);
            }
         }
      }
      else {
         myEmptyP = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myMaxRange = null;
      rtok.scanToken ('[');
      if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         if (rtok.sval.equals ("maxRange")) {
            rtok.scanToken ('=');
            myMaxRange = (NumericInterval)Scan.scanClassAndObject (rtok, ref);
         }
         else if (rtok.sval.equals ("allowEmpty")) {
            rtok.scanToken ('=');
            myAllowEmpty = rtok.scanBoolean();
         }
         else {
            throw new IOException ("Unrecognized keyword: "+rtok);
         }
      }
      else {
         rtok.pushBack();
      }
      rtok.scanToken (']');
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      if (myMaxRange == null) {
         pw.println ("[ ]");
      }
      else {
         IndentingPrintWriter.addIndentation (pw, 2);
         pw.println ("[");
         System.out.print ("maxRange="+myMaxRange.getClass().getName()+" ");
         myMaxRange.write (pw, fmt, ref);
         if (!myAllowEmpty) {
            System.out.println ("allowEmpty=false");
         }
         pw.println ("]");
         IndentingPrintWriter.addIndentation (pw, -2);
      }
   }

   public NumericIntervalRange clone() {
      NumericIntervalRange nrr = (NumericIntervalRange)super.clone();
      if (myMaxRange != null) {
         nrr.myMaxRange = myMaxRange.clone();
      }
      return nrr;
   }
}
