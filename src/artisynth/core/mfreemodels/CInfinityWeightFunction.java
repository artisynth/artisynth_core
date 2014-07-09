/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;

public class CInfinityWeightFunction extends RadialWeightFunction {
   
   public static double EPSILON = 1e-10;
   public double DEFAULT_RADIUS = 1;
   private double rho2;
   
   public CInfinityWeightFunction(Point3d x) {
      setCenter(x);
      setRadius(DEFAULT_RADIUS);
   }
   
   public CInfinityWeightFunction(Point3d x, double rho) {
      setCenter(x);
      setRadius(rho);
   }

   @Override
   public double eval(double r2) {
      double q2 = r2/rho2;
      if (q2 >= 1) {
         return 0;
      }
      return (Math.exp(1/(q2-1)));
   }
   
   public void setRadius(double rho) {
      rho2 = rho*rho;
      super.setRadius(rho);
   }
   
   public CInfinityWeightFunction clone() {
      CInfinityWeightFunction w = new CInfinityWeightFunction(center);
      w.rho2 = this.rho2;
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

      if (q2 > 1-EPSILON) {
         return 0;
      }
      
      double f = eval(r2);
      double q2m1= q2-1;
      double q2m12 = q2m1*q2m1;
      double rho4 = rho2*rho2;

      if (dx == 0 && dy == 0 && dz == 0) {
         return f;
      } else if (dx == 1 && dy == 0 && dz == 0) {
         return -2*delx/(rho2*q2m12)*f;
      } else if (dx == 0 && dy == 1 && dz == 0) {
         return -2*dely/(rho2*q2m12)*f;
      } else if (dx == 0 && dy == 0 && dz == 1) {
         return -2*delz/(rho2*q2m12)*f;
      } else if (dx == 1 && dy == 1 && dz == 0) {
         return 4*delx*dely*(2.0/(q2m1*q2m12)+1.0/(q2m12*q2m12))/(rho4)*f;
      } else if (dx == 1 && dy == 0 && dz == 1) {
         return 4*delx*delz*(2.0/(q2m1*q2m12)+1.0/(q2m12*q2m12))/(rho4)*f;
      } else if (dx == 0 && dy == 1 && dz == 1) {
         return 4*dely*delz*(2.0/(q2m1*q2m12)+1.0/(q2m12*q2m12))/(rho4)*f;
      } else if (dx == 2 && dy == 0 && dz == 0) {
         return (8*delx*delx/(q2m1*q2m12*rho4)-2.0/(q2m12*rho2)+4*delx*delx/(q2m12*q2m12*rho4))*f;
      } else if (dx == 0 && dy == 2 && dz == 0) {
         return (8*dely*dely/(q2m1*q2m12*rho4)-2.0/(q2m12*rho2)+4*dely*dely/(q2m12*q2m12*rho4))*f;
      } else if (dx == 0 && dy == 0 && dz == 0) {
         return (8*delz*delz/(q2m1*q2m12*rho4)-2.0/(q2m12*rho2)+4*delz*delz/(q2m12*q2m12*rho4))*f;
      } else {
         throw new IllegalArgumentException(
            "Only up to 2nd order derivatives supported");
      }
      
   }

   @Override
   public RadialWeightFunctionType getType() {
      return RadialWeightFunctionType.C_INFINITY;
   }

}
