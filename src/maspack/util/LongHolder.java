/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Wrapper class which ``holds'' a long value, enabling methods to return long
 * values through arguments.
 */
public class LongHolder implements java.io.Serializable {
   
   private static final long serialVersionUID = 1L;
   /**
    * Value of the long, set and examined by the application as needed.
    */
   public long value;

   /**
    * Constructs a new <code>LongHolder</code> with an initial value of 0.
    */
   public LongHolder() {
      value = 0;
   }

   /**
    * Constructs a new <code>LongHolder</code> with a specific initial value.
    * 
    * @param l
    * Initial long value.
    */
   public LongHolder (long l) {
      value = l;
   }
}
