/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import maspack.matrix.Matrix3d;
import maspack.matrix.Vector3d;

/**
 * Written to provide interpolation of a path based on three knot points.
 * 
 * @author chad
 * 
 */
public class ParabolicInterpolation {
   Matrix3d time = null;
   boolean inverted = false;

   public ParabolicInterpolation() {
      time = new Matrix3d();
   }

   public void setTimeofKnots (double t0, double t1, double t2) {
      if (t0 > t1 || t1 > t2 || t0 > t2) {
         System.out.println ("Error: times are not ordered");
         return;
      }

      double row1[] = { t0 * t0, t0, 1 };
      double row2[] = { t1 * t1, t1, 1 };
      double row3[] = { t2 * t2, t2, 1 };

      time.setRow (0, row1);
      time.setRow (1, row2);
      time.setRow (2, row3);
      inverted = false;
   }

   public void printTime() {
      System.out.println ("Time Matrix");
      System.out.println (time.toString());
   }

   public double interpolate (double t, double x[]) {

      boolean success = false;
      double result;
      if (!inverted) {
         time.invert();
         inverted = true;
      }

      Vector3d xVector = new Vector3d();
      xVector.set (x);

      Vector3d tVector = new Vector3d();
      double trow[] = { t * t, t, 1 };
      tVector.set (trow);

      xVector.mul (time, xVector);
      // System.out.println("Intermediate Vector " +xVector.toString() );
      result = tVector.dot (xVector);

      return result;

   }

   public static void main (String[] args) {
      ParabolicInterpolation cuby = new ParabolicInterpolation();
      cuby.setTimeofKnots (0.0, 1.0, 2.0);
      cuby.printTime();
      double xvalues[] = { 2, 3, 2 };
      System.out.println (
         "Interpolated value " + cuby.interpolate (0.5, xvalues));
   }
}
