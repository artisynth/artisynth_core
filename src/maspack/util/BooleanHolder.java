/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Wrapper class which ``holds'' a boolean value, enabling methods to return
 * boolean values through arguments.
 */
public class BooleanHolder implements java.io.Serializable {

   private static final long serialVersionUID = 1L;
   
   /**
    * Value of the boolean, set and examined by the application as needed.
    */
   public boolean value;

   /**
    * Constructs a new <code>BooleanHolder</code> with an initial value of
    * <code>false</code>.
    */
   public BooleanHolder() {
      value = false;
   }

   /**
    * Constructs a new <code>BooleanHolder</code> with a specific initial
    * value.
    * 
    * @param b
    * Initial boolean value.
    */
   public BooleanHolder (boolean b) {
      value = b;
   }
}
