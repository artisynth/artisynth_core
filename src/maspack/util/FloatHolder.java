/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Wrapper class which ``holds'' a float value, enabling methods to return float
 * values through arguments.
 */
public class FloatHolder implements java.io.Serializable {
   
   private static final long serialVersionUID = 1L;
   /**
    * Value of the float, set and examined by the application as needed.
    */
   public float value;

   /**
    * Constructs a new <code>FloatHolder</code> with an initial value of 0.
    */
   public FloatHolder() {
      value = 0;
   }

   /**
    * Constructs a new <code>FloatHolder</code> with a specific initial value.
    * 
    * @param f
    * Initial float value.
    */
   public FloatHolder (float f) {
      value = f;
   }
}
