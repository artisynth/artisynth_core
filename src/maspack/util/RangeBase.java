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
public abstract class RangeBase implements Range {

   protected boolean myEmptyP = false;

   public static void setError (StringHolder errMsg, String msg) {
      if (errMsg != null) {
         errMsg.value = msg;
      }
   }

//   /**
//    * {@inheritDoc}
//    */
//   public boolean withinRange (Object obj) {
//      // about to get rid of this method
//      return false;
//   }

   /**
    * {@inheritDoc}
    */
   public abstract boolean isValid (Object obj, StringHolder errMsg);

   /**
    * {@inheritDoc}
    */
   public Object makeValid (Object obj) {
      // assume that by default a value cannot be projected
      return Range.IllegalValue;
   }

   // /**
   //  * {@inheritDoc}
   //  */
   // public boolean isTypeCompatible (Object obj);ak

//   /**
//    * Returns a string representation of this Range.
//    */
//   public String getDescription() {
//      return null;
//   }

   /**
    * {@inheritDoc}
    */
   public boolean isEmpty() {
      return myEmptyP;
   }

   /**
    * {@inheritDoc}
    */
   public void intersect (Range r) {
      if (!(getClass().isAssignableFrom(r.getClass()))) {
         myEmptyP = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myEmptyP = false;
      rtok.scanToken ('[');
      rtok.scanToken (']');
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.println ("[ ]");
   }

   public RangeBase clone() {
      try {
         return (RangeBase)super.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException ("Can't clone "+getClass());
      }
   }
}