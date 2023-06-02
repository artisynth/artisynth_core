/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.util.DoubleHolder;
import maspack.matrix.VectorNd;

/**
 * Once-differentiable single-input, single output function
 */
public interface Diff1Function1x1 extends Function1x1, Diff1FunctionNx1 {
   
   /**
    * Evaluates both the first derivative of this function at a
    * specified input value.
    *
    * @param x input value
    * @return derivative value
    */  
   double evalDeriv (double x);
   
   /**
    * Evaluates both the value and first derivative of this function at a
    * specified input value.
    *
    * @param dval if non-null, returns the derivative value 
    * @param x input value
    * @return function value
    */
   default double eval (DoubleHolder dval, double x) {
      if (dval != null) {
         dval.value = evalDeriv (x);
      }
      return eval (x);
   }
   
   default void evalDeriv (VectorNd deriv, VectorNd in) {
      deriv.set (0, evalDeriv(in.get(0)));
   }
   
   default void evalDeriv (double[] deriv, double[] in) {
      deriv[0] = evalDeriv(in[0]);
   }
     
}
