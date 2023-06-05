/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Multi-input, multi-output functions
 */
public interface FunctionNxM {
   
   /**
    * Evaluates this function. The input and output vectors sizes must equal
    * the values returned by {@link #inputSize} and {@link #outputSize},
    * respectively.
    * 
    * @param out function output
    * @param in function input
    * @return function value
    */
   void eval (VectorNd out, VectorNd in);
   
   /**
    * Queries the input size of this function
    * 
    * @return function input size
    */
   int inputSize();
   
   /**
    * Queries the output size of this function
    * 
    * @return function output size
    */  
   int outputSize();
}
