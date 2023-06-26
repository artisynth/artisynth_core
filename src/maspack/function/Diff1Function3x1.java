/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public interface Diff1Function3x1 extends Function3x1 {

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
    * Evaluates this function for the specified inputs. If {@code deriv} is not
    * {@code null}, the derivative is also computed and returned in this
    * argument.
    *
    * @param deriv if not {@code null}, returns the derivative values
    * @param in function input values
    * @return function output value
    */  
   public double eval (Vector3d deriv, Vector3d in);
   
   /**
    * {@inheritDoc}
    */
   default public double eval (VectorNd deriv, VectorNd in) {
      if (in.size() != 3) {
         throw new IllegalArgumentException (
            "Argument 'in' has size "+in.size()+"; expected 3");
      }
      Vector3d in3 = new Vector3d (in.get(0), in.get(1), in.get(2));
      if (deriv != null) {
         if (deriv.size() != 3) {
            deriv.setSize (3);
         }
         Vector3d deriv3 = new Vector3d();
         double value = eval (deriv3, in3);
         deriv.set (deriv3);
         return value;
      }
      else {
         return eval (null, in3);
      }
   }
}
