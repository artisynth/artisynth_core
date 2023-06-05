/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class ConstantFunction3x1 implements Diff2Function3x1 {

   double c;
   
   public ConstantFunction3x1(double c) {
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

   public double eval (VectorNd in) {
      return c;
   }

   public double eval(Vector3d in) {
      return c;
   }
   
   public double eval(Vector3d deriv, Vector3d in) {
      if (deriv != null) {
         deriv.setZero();
      }
      return c;
   }
   
   public int inputSize() {
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
