/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import maspack.util.Scannable;

/**
 * Base class for tokens used in postscaning. Post scannning is the "second
 * pass" used to read in ArtiSynth model components, in which items (such as
 * component references) that could not be resolved by {@link Scannable#scan
 * Scannable.scan()} during the first pass are stored in a queue of tokens that
 * are then processed in a second pass by a <code>postscan()</code> method.
 */
public class ScanToken {

   int myLineno;

   private static class EndToken extends ScanToken {
      public String toString () {
         return "EndToken";
      }
   }            

   private static class BeginToken extends ScanToken {
      public String toString () {
         return "BeginToken";
      }
   }            

   // special tokens used as delimiters
   public static final ScanToken END = new EndToken ();

   public static final ScanToken BEGIN = new BeginToken ();
   
   public ScanToken () {
      this (-1);
   }

   public ScanToken (int lineno) {
      myLineno = lineno;
   }
   
   public final int lineno() {
      return myLineno;
   }

   public Object value() {
      return null;
   }

   public String toString() {
      return (getClass().getName()+",value="+
              value()+",lineno="+lineno()+")");
   }
}


