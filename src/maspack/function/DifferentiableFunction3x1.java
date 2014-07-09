/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;

public interface DifferentiableFunction3x1 extends Function3x1 {

   public double eval(Point3d in);
   public double eval(double x, double y, double z);
   public double evalDerivative(Point3d in, int[] derivatives);
   public double evalDerivative(double x, double y, double z, int dx, int dy, int dz);
}
