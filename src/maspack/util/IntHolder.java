/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Wrapper class which ``holds'' an integer value, enabling methods to return
 * integer values through arguments.
 */
public class IntHolder implements java.io.Serializable {
   
   private static final long serialVersionUID = 1L;
   
   /**
    * Value of the integer, set and examined by the application as needed.
    */
   public int value;

   /**
    * Constructs a new <code>IntHolder</code> with an initial value of 0.
    */
   public IntHolder() {
      value = 0;
   }

   /**
    * Constructs a new <code>IntHolder</code> with a specific initial value.
    * 
    * @param i
    * Initial integer value.
    */
   public IntHolder (int i) {
      value = i;
   }
}
