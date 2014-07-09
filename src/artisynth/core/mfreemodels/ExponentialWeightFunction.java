/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;

public class ExponentialWeightFunction extends RadialWeightFunction {

   public double DEFAULT_VARIANCE = 1;
   public int DEFAULT_EXPONENT = 1;
   public double DEFAULT_RADIUS = 1;

   private double rho2 = 1;
   private double oc2 = 1;
   private int k = 1;
   private double a;

   public ExponentialWeightFunction (Point3d x) {
      setCenter(x);
      setRadius(DEFAULT_RADIUS);
      setExponent(DEFAULT_EXPONENT);
      setVariance(DEFAULT_VARIANCE);
   }

   public ExponentialWeightFunction (Point3d x, double rho) {
      setCenter(x);
      setRadius(rho);
      setExponent(DEFAULT_EXPONENT);
      setVariance(DEFAULT_VARIANCE);
   }

   public ExponentialWeightFunction (Point3d x, double rho, int k, double s2) {
      setCenter(x);
      setRadius(rho);
      setExponent(k);
      setVariance(s2);
   }

   @Override
   public double eval(double r2) {
      double q2 = r2 / rho2;
      if (q2 >= 1) {
         return 0;
      }
      return (Math.exp(-Math.pow(q2 * oc2, k)) - a) / (1 - a);
   }

   public void setRadius(double rho) {
      rho2 = rho * rho;
      super.setRadius(rho);
   }

   public void setExponent(int k) {
      this.k = k;
      a = Math.exp(-Math.pow(1.0 * oc2, k));
   }

   public int getExponent() {
      return k;
   }

   public void setVariance(double c2) {
      this.oc2 = 1.0 / c2;
      a = Math.exp(-Math.pow(1.0 * oc2, k));
   }

   public double getVariance() {
      return 1.0 / oc2;
   }

   public ExponentialWeightFunction clone() {
      ExponentialWeightFunction w = new ExponentialWeightFunction(center);
      w.a = this.a;
      w.k = this.k;
      w.rho2 = this.rho2;
      w.oc2 = this.oc2;
      return w;
   }

   public double evalDerivative(Point3d in, int[] derivatives) {
      return evalDerivative(in.x, in.y, in.z, 
         derivatives[0], derivatives[1], derivatives[2]);
   }

   public double evalDerivative(double x, double y, double z, int dx, int dy,
      int dz) {

      double delx = x - center.x;
      double dely = y - center.y;
      double delz = z - center.z;

      double r2 = (delx * delx + dely * dely + delz * delz);
      double q2 = r2 / rho2;

      if (q2 > 1) {
         return 0;
      } else if (q2 < 1e-10 && k<2) {
         q2 = q2+1e-10;
      }

      double f = Math.exp(-Math.pow(q2 * oc2, k)) / (1 - a);

      if (dx == 0 && dy == 0 && dz == 0) {
         return f - (a / (1 - a));

      } else if (dx == 1 && dy == 0 && dz == 0) {
         return -2 * delx * k * Math.pow(q2 * oc2, k - 1) * f * oc2 / rho2;
      } else if (dx == 0 && dy == 1 && dz == 0) {
         return -2 * dely * k * Math.pow(q2 * oc2, k - 1) * f * oc2 / rho2;
      } else if (dx == 0 && dy == 0 && dz == 1) {
         return -2 * delz * k * Math.pow(q2 * oc2, k - 1) * f * oc2 / rho2;
      } else if (dx == 1 && dy == 1 && dz == 0) {
         return 4
            * delx
            * dely
            * ((k - k * k) * Math.pow(q2 * oc2, k - 2) + k * k
               * Math.pow(q2 * oc2, 2 * k - 2)) * oc2 * oc2 / (rho2 * rho2) * f;
      } else if (dx == 1 && dy == 0 && dz == 1) {
         return 4
            * delx
            * delz
            * ((k - k * k) * Math.pow(q2 * oc2, k - 2) + k * k
               * Math.pow(q2 * oc2, 2 * k - 2)) * oc2 * oc2 / (rho2 * rho2) * f;
      } else if (dx == 0 && dy == 1 && dz == 1) {
         return 4
            * dely
            * delz
            * ((k - k * k) * Math.pow(q2 * oc2, k - 2) + k * k
               * Math.pow(q2 * oc2, 2 * k - 2)) * oc2 * oc2 / (rho2 * rho2) * f;
      } else if (dx == 2 && dy == 0 && dz == 0) {

         return (4 * oc2 * oc2 / (rho2 * rho2) * delx * delx * (k - k * k)
            * Math.pow(q2 * oc2, k - 2)
            - 2 * k * oc2 / rho2 * Math.pow(q2 * oc2, k - 1)
            + 4 * oc2 * oc2 / (rho2 * rho2) * delx * delx * k * k
            * Math.pow(q2 * oc2, 2 * k - 2))
            * f;

      } else if (dx == 0 && dy == 2 && dz == 0) {
         return (4 * oc2 * oc2 / (rho2 * rho2) * dely * dely * (k - k * k)
            * Math.pow(q2 * oc2, k - 2)
            - 2 * k * oc2 / rho2 * Math.pow(q2 * oc2, k - 1)
            + 4 * oc2 * oc2 / (rho2 * rho2) * dely * dely * k * k
            * Math.pow(q2 * oc2, 2 * k - 2))
            * f;
      } else if (dx == 0 && dy == 0 && dz == 0) {
         return (4 * oc2 * oc2 / (rho2 * rho2) * delz * delz * (k - k * k)
            * Math.pow(q2 * oc2, k - 2)
            - 2 * k * oc2 / rho2 * Math.pow(q2 * oc2, k - 1)
            + 4 * oc2 * oc2 / (rho2 * rho2) * delz * delz * k * k
            * Math.pow(q2 * oc2, 2 * k - 2))
            * f;
      } else {
         throw new IllegalArgumentException(
            "Only up to 2nd order derivatives supported");
      }

   }

   @Override
   public RadialWeightFunctionType getType() {
      return RadialWeightFunctionType.EXPONENTIAL;
   }

}
