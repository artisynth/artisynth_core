/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;

/**
 * Twice differentiable 3 x 1 function.
 *
 * <p>This is currently only used in the propotype meshfree model code.
 * Further usage of this class will probably involve refactoring.
 */
public interface Diff2Function3x1 extends Diff1Function3x1 {

   /**
    * Evaluates 1st and 2nd order derivatives, one at a time.
    */
   public double evalDerivative(Point3d in, int[] derivatives);

   /**
    * Evaluates 1st and 2nd order derivatives, one at a time.
    */
   public double evalDerivative(
      double x, double y, double z, int dx, int dy, int dz);
}
