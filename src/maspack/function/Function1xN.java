/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Single-input, multi-output functions
 */
public interface Function1xN {

   /**
    * Evaluates this function. The output vector size must equal the value
    * returned by {@link #outputSize}.
    *
    * @param out function output
    * @param in function input
    */
   void eval (VectorNd out, double in);

   /**
    * Queries the output size of this function
    * 
    * @return function output size
    */ 
   int outputSize();
}
