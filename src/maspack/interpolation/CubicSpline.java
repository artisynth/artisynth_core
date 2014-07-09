/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import maspack.matrix.Matrix4d;
import maspack.matrix.Vector4d;

/**
 * A cardinal hermation Spline Interpolation method.
 * 
 * @author chad
 * 
 */
public class CubicSpline {

   private static double[] pt = new double[4];

   private static double alpha = 0.5;

   static Matrix4d hermite = null;

   static {
      hermite = new Matrix4d();
      double values[] = { 2, -2, 1, 1, -3, 3, -2, -1, 0, 0, 1, 0, 1, 0, 0, 0 };
      hermite.set (values);
   }

   /**
    * The eval method evaluates a point on a curve given a parametric value "t".
    * This value should not be changed. The dimension of the point to evaluate
    * is p.length - 1. The result of the evaluation is placed in index locations
    * 0 .. p.length - 2 (inclusive).
    * 
    * The eval method should remain protected except for those curves that do no
    * need any preparation to be done in the appendTo method.
    */

   public static void printHermite() {
      System.out.println ("Time Matrix");
      System.out.println (hermite.toString());
   }

   public static double interpolate (double[] p, double[] ptimes, double t) {
      if (p.length != 4) {
         System.out.println ("Error: not enough knots for interpolation");
         return Double.NaN;
      }

      double dt21 = ptimes[2] - ptimes[1];

      double tRel = (t - ptimes[1]) / dt21;
      if (tRel < 0 || tRel > 1) {
         System.out.println ("Error: Time bounds exception: " + t + "<>" +
                             ptimes[1] + " " + ptimes[2]);
         return Double.NaN;
      }
      // double result;
      // double boundCond[] = new double[4];
      // // add to two midpoints to the boundary condition
      // boundCond[0] = p[1];
      // boundCond[1] = p[2];
      // boundCond[2] = (p[2] - p[0])/(ptimes[2] - ptimes[0]);
      // boundCond[3] = (p[3] - p[1])/(ptimes[3] - ptimes[1]);
      double dP1 = (p[2] - p[0]) / (ptimes[2] - ptimes[0]) * dt21;
      double dP2 = (p[3] - p[1]) / (ptimes[3] - ptimes[1]) * dt21;
      //      
      // // System.out.println("slope at point 1: " + boundCond[2] + "=" + (p[2]
      // - p[0]) + "/" + (ptimes[2] - ptimes[0]) );
      // // System.out.println("slope at point 2: " + boundCond[3]);
      // Vector4d xVector = new Vector4d();
      // xVector.set(boundCond);
      //
      // Vector4d tVector = new Vector4d();
      // double trow[] = {tRel*tRel*tRel, tRel*tRel, tRel, 1};
      // tVector.set(trow);
      // xVector.mul(hermite, xVector);
      // result = tVector.dot(xVector);

      double t3 = (tRel * tRel * tRel);
      double t2 = (tRel * tRel);
      double validate =
         ((2 * t3 - 3 * t2 + 1) * p[1]) + ((-2 * t3 + 3 * t2) * p[2]) +
         (dP1 * (t3 - 2 * t2 + tRel)) + (dP2 * (t3 - t2));

      // if(validate!= result)
      // System.out.println("Error: Internal Validation failed, " + validate + "
      // != " + result);
      return validate;

   }

   public static double interpolate (
      int firstDerivative, double[] p, double[] ptimes, double t) {
      if (p.length != 4) {
         System.out.println ("Error: not enough knots for interpolation");
         return Double.NaN;
      }

      double dt21 = ptimes[2] - ptimes[1];

      double tRel = (t - ptimes[1]) / dt21;
      if (tRel < 0 || tRel > 1) {
         System.out.println ("Error: Time bounds exception: " + t + "<>" +
                             ptimes[1] + " " + ptimes[2]);
         return Double.NaN;
      }

      double dP1 = (firstDerivative);
      double dP2 = (p[3] - p[1]) / (ptimes[3] - ptimes[1]) * dt21;

      double t3 = (tRel * tRel * tRel);
      double t2 = (tRel * tRel);
      double validate =
         ((2 * t3 - 3 * t2 + 1) * p[1]) + ((-2 * t3 + 3 * t2) * p[2]) +
         (dP1 * (t3 - 2 * t2 + tRel)) + (dP2 * (t3 - t2));

      return validate;

   }

   // public static void main (String[] args)
   // {
   // double times[] = {0.0, 0.5, 4.5, 5.0};
   // double xvalues[] = {0,5,5,0};
   // // CubicSpline.printHermite();
   // double referenceTime = 0;
   // System.out.println("Interpolated value " +
   // CubicSpline.interpolate(xvalues, times, (float) 4.0));
   // }
   //	

}
