/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Multi-input, single output
 */
public interface MISOFunction {

   /**
    * Queries the number of inputs expected by this function.
    *
    * @return number of expected inputs
    */
   int getInputSize();
   
   /**
    * Evaluates this function for the specified inputs.
    *
    * @param in function inputs. Should have a length {@code >=} {@link
    * #getInputSize}.
    * @return function output value
    */
   double eval(double[] in);
   
   /**
    * Evaluates this function for the specified inputs.
    *
    * @param in function inputs. Should have a length {@code >=} {@link
    * #getInputSize}.
    * @return function output value
    */
   default double eval (VectorNd in) {
      return eval (in.getBuffer());
   }
     
}
