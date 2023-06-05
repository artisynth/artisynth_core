package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Single input, single output
 *
 */
public interface Function1x1 extends FunctionNx1 {

   /**
    * Evaluates the function at a prescribed input value.
    *
    * @param x input value
    * @return function output value
    */
   double eval (double x);

   default int inputSize() {
      return 1;
   }
   
   default double eval (VectorNd in) {
      return eval (in.get(0));
   }
}
