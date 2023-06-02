/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Once-differentiable muliple-input, single output function
 */
public interface Diff1FunctionNx1 extends FunctionNx1 {
   
   /**
    * Evaluates the derivative of this function at the specified input value.
    * Both the input and output arguments should have a length {@code >=}
    * {@link #getInputSize}.
    *
    * @param deriv returns the derivatives values
    * @param in input values at which the derivative should be evaluated
    */
   void evalDeriv (double[] deriv, double[] in);
   
   /**
    * Evaluates the derivative of this function at the specified input value.
    * Both the input and output arguments should have a length {@code >=}
    * {@link #getInputSize}.
    *
    * @param deriv returns the derivatives values
    * @param in input values at which the derivative should be evaluated
    */
   default void evalDeriv (VectorNd deriv, VectorNd in) {
      evalDeriv (deriv.getBuffer(), in.getBuffer());
   }
}
