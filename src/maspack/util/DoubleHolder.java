/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Wrapper class which ``holds'' a double value, enabling methods to return
 * double values through arguments.
 */
public class DoubleHolder implements java.io.Serializable {
   
   private static final long serialVersionUID = 1L;
   
   /**
    * Value of the double, set and examined by the application as needed.
    */
   public double value;

   /**
    * Constructs a new <code>DoubleHolder</code> with an initial value of 0.
    */
   public DoubleHolder() {
      value = 0;
   }

   /**
    * Constructs a new <code>DoubleHolder</code> with a specific initial
    * value.
    * 
    * @param d
    * Initial double value.
    */
   public DoubleHolder (double d) {
      value = d;
   }
}
