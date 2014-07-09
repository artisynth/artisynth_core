/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;

public class GaussianFunction3x1 implements Function3x1 {

   private Point3d m;
   private Matrix3d A;
   private double s;

   
   public GaussianFunction3x1(Point3d mean, Matrix3d A) {
      setMean(mean);
      setVarianceMatrix(A);
      setScaleFactor(1);
   }
   
   public void setMean(Point3d mean) {
      m = mean;
   }
   public Point3d getMean() {
      return m;
   }
   public Matrix3d getVarianceMatrix() {
      return A;
   }
   public void setVarianceMatrix(Matrix3d A) {
      this.A = A;
   }
   
   public void setScaleFactor(double a) {
      this.s = a;
   }
   
   public double getScaleFactor() {
      return s;
   }

   public double eval(double[] in) {
      return eval(new Point3d(in[0],in[1],in[2]));
   }

   public int getInputSize() {
      return 3;
   }

   public double eval(double x, double y, double z) {
      return eval(new Point3d(x,y,z));
   }

   public double eval(Point3d in) {
      Point3d tmp = new Point3d(in);
      tmp.mul(A, in);
      return s*Math.exp(tmp.dot(in)+m.dot(in));
   }
   
      
}
