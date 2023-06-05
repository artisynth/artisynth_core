/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Multi-input, single output
 */
public interface FunctionNx1 {

   /**
    * Queries the number of inputs expected by this function.
    *
    * @return number of expected inputs
    */
   int inputSize();
   
   /**
    * Evaluates this function for the specified inputs. The input vector size
    * must the value returned by {@link #inputSize}.
    * 
    * @return function output value
    */
   double eval (VectorNd in);
}
