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
    * Evaluates this function for the specified inputs. If {@code deriv} is not
    * {@code null}, the derivative is also computed and returned in this
    * argument. The input vector size should equal the value returned by {@link
    * #inputSize}. If present, {@code deriv} will be resized, if necessary, to
    * {@link #inputSize}.
    *
    * @param deriv if not {@code null}, returns the derivative values
    * @param in function input values
    * @return function output value
    */
   double eval (VectorNd deriv, VectorNd in);
   
   /**
    * Evaluates the derivative of this function for the specified inputs. The
    * input vector size should equal the value returned by {@link
    * #inputSize}. The derivative vector will be resized, if necessary, to
    * {@link #inputSize}.
    *
    * @param deriv returns the derivative values
    * @param in function input values
    */
   default void evalDeriv (VectorNd deriv, VectorNd in) {
      eval (deriv, in);
   }
   
}
