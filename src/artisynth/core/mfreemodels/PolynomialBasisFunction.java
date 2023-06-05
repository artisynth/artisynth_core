/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.function.Diff2Function3x1;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class PolynomialBasisFunction implements Diff2Function3x1{

   private static double EPSILON = 1e-15; 
   private int [] p;
   
   public PolynomialBasisFunction(int xn, int yn, int zn) {
      p = new int[3];
      p[0] = xn;
      p[1] = yn;
      p[2] = zn;
   }
   
   public double eval(VectorNd in) {
      return eval(in.get(0), in.get(1), in.get(2));
   }

   public int inputSize() {
      return 3;
   }

   public double eval(Vector3d in) {
      return eval(in.x, in.y, in.z);
   }

   public double eval (Vector3d deriv, Vector3d in) {
      if (deriv != null) {
         deriv.x = evalDerivative (in.x, in.y, in.z, 1, 0, 0);
         deriv.x = evalDerivative (in.x, in.y, in.z, 0, 1, 0);
         deriv.x = evalDerivative (in.x, in.y, in.z, 0, 0, 1);
      }
      return eval (in);
   }

   public double evalDerivative(Point3d in, int[] derivatives) {
      return evalDerivative(in.x,in.y,in.z,
         derivatives[0], derivatives[1], derivatives[2]);
   }

   public double evalDerivative(double x, double y, double z, int dx, int dy,
      int dz) {

      if (Math.abs(x)<EPSILON && p[0]-dx<0) {
         x = (x<0 ? -EPSILON : EPSILON);
      }
      if (Math.abs(y)<EPSILON && p[1]-dy<0) {
         y = (y<0 ? -EPSILON : EPSILON);
      }
      if (Math.abs(z)<EPSILON && p[2]-dz<0) {
         z = (z<0 ? -EPSILON : EPSILON);
      }
      
      double out = diffcoeff(p[0], dx)*Math.pow(x, p[0]-dx);
      out *= diffcoeff(p[1], dy)*Math.pow(y, p[1]-dy);
      out *= diffcoeff(p[2], dz)*Math.pow(z, p[2]-dz);
      
      return out;
   }

   public double eval(double x, double y, double z) {
      if (Math.abs(x)<EPSILON && p[0]<0) {
         x = (x<0 ? -EPSILON : EPSILON);
      }
      if (Math.abs(y)<EPSILON && p[1]<0) {
         y = (y<0 ? -EPSILON : EPSILON);
      }
      if (Math.abs(z)<EPSILON && p[2]<0) {
         z = (z<0 ? -EPSILON : EPSILON);
      }
      return Math.pow(x,p[0])*Math.pow(y, p[1])*Math.pow(z,p[2]);
   }

   private int diffcoeff(int n, int dn) {
      int k = 1;
      for (int i=n-dn+1; i<=n; i++) {
         k = k*i;
      }
      return k;
   }

}
