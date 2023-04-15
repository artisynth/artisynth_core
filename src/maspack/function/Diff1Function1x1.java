/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.util.DoubleHolder;

/**
 * Once-differentiable single-input, single output function
 */
public interface Diff1Function1x1 extends Function1x1 {
   
   /**
    * Evaluates both the value and first derivative of this function at a
    * specified input value.
    *
    * @param dval if non-null, returns the derivative value 
    * @param x input value
    * @return function value
    */
   double eval (DoubleHolder dval, double x);
     
}
