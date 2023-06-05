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
    * Evaluates both the value and first derivative of this function at a
    * specified input value.
    *
    * @param dval if non-null, returns the derivative value 
    * @param x input value
    * @return function value
    */
   double eval (DoubleHolder dval, double x);
   
   /**
    * Evaluates first derivative of this function at a
    * specified input value.
    *
    * @param x input value
    * @return derivative value
    */  
   default double evalDeriv (double x) {
      DoubleHolder dval = new DoubleHolder();
      eval (dval, x);
      return dval.value;
   }

   default double eval (VectorNd deriv, VectorNd in) {
      if (in.size() != 1) {
         throw new IllegalArgumentException (
            "argument 'in' has size "+in.size()+"; should be 1");
      }
      if (deriv != null) {
         if (deriv.size() != 1) {
            deriv.setSize (1);
         }
         DoubleHolder dval = new DoubleHolder();
         double val = eval (dval, in.get(0));
         deriv.set (0, dval.value);
         return val;
      }
      else {
         return eval (null, in.get(0));
      }
   }
   
   default void evalDeriv (VectorNd deriv, VectorNd in) {
      deriv.set (0, evalDeriv(in.get(0)));
   }
     
}
