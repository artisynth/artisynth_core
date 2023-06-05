/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public interface Function3x1 extends FunctionNx1 {

   /**
    * {@inheritDoc}
    */
   default double eval (VectorNd in) {
      if (in.size() != 3) {
         throw new IllegalArgumentException (
            "Argument 'in' has size "+in.size()+"; expected 3");
      }
      Vector3d vec = new Vector3d (in.get(0), in.get(1), in.get(2));
      return eval (vec);      
   }

   /**
    * {@inheritDoc}
    */
   default int inputSize() {
      return 3;
   }

   /**
    * Evaluates this function for the specified inputs.
    * 
    * @return function output value
    */
   double eval(Vector3d in);

   default double eval(double x, double y, double z) {
      return eval (new Vector3d (x, y, z));
   }
}
