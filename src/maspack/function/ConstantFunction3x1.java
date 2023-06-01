/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;

public class ConstantFuntion3x1 implements DifferentiableFunction3x1 {

   double c;
   
   public ConstantFuntion3x1(double c) {
      setVal(c);
   }
   
   public double getVal() {
      return c;
   }
   
   public void setVal(double val) {
      c = val;
   }
   
   public double eval(double in) {
      return c;
   }

   public double eval(double[] in) {
      return c;
   }

   public double eval(Point3d in) {
      return c;
   }
   
   public int getInputSize() {
      return 3;
   }

   public double eval(double x, double y, double z) {
      return c;
   }

   public double evalDerivative(Point3d in, int[] derivatives) {
      return 0;
   }

   public double evalDerivative(double x, double y, double z, int dx, int dy,
      int dz) {
      return 0;
   }

}
