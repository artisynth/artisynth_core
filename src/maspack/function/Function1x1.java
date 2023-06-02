package maspack.function;

import maspack.matrix.VectorNd;

/**
 * Single input, single output
 *
 */
public interface Function1x1 extends SISOFunction, FunctionNx1 {

   default int inputSize() {
      return 1;
   }
   
   default double eval (double[] in) {
      return eval (in[0]);
   }
   
   default double eval (VectorNd in) {
      return eval (in.get(0));
   }
}
