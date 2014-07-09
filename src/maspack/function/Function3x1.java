/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;

public interface Function3x1 extends MISOFunction {

   double eval(double x, double y, double z);
   double eval(Point3d in);
   
   
}
