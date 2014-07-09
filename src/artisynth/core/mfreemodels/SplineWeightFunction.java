/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;

public class SplineWeightFunction extends RadialWeightFunction {

   public int DEFAULT_CONTINUITY = 2;
   public double DEFAULT_RADIUS = 1;
   
   private int k;
   private double rho2;
   
   public SplineWeightFunction(Point3d x) {
      setCenter(x);
      setRadius(DEFAULT_RADIUS);
      setContinuity(DEFAULT_CONTINUITY);
   }
   
   public SplineWeightFunction(Point3d x, double rho) {
      setCenter(x);
      setRadius(rho);
      setContinuity(DEFAULT_CONTINUITY);
   }
   
   public SplineWeightFunction(Point3d x, double rho, int C) {
      setCenter(x);
      setRadius(rho);
      setContinuity(C);
   }

   @Override
   public double eval(double r2) {
      double q2 = r2/rho2;
      if (q2 >= 1) {
         return 0;
      }
      return Math.pow(1-q2, k);
   }
   
   public void setRadius(double rho) {
      rho2 = rho*rho;
      super.setRadius(rho);
   }
   
   public void setContinuity(int C) {
      this.k = C+1;
   }
   
   public int getContinuity() {
      return k-1;
   }
   
   public SplineWeightFunction clone() {
      SplineWeightFunction s = new SplineWeightFunction(center);
      s.rho2 = this.rho2;
      s.k = this.k;
      return s;
   }

   public double evalDerivative(Point3d in, int[] derivatives) {
      return evalDerivative(in.x, in.y, in.z, 
         derivatives[0], derivatives[1], derivatives[2]);
   }

   public double evalDerivative(double x, double y, double z, int dx, int dy,
      int dz) {      
      
      int td = dx+dy+dz; 
      if (td > 2) {
         throw new IllegalArgumentException("Only second-order derivatives supported");
      }
      
      double delx = x-center.x;
      double dely = y-center.y;
      double delz = z-center.z;
      double q2 = (delx*delx + dely*dely+delz*delz)/rho2; 
      
      if (q2 > 1) {
         return 0;
      }
      
      if (td == 0) {
         return Math.pow(1-q2, k);
      }
      
      if (td == 1) {
         double del = 0;
         if (dx==1) {
            del = delx;
         } else if (dy == 1) {
            del = dely;
         } else if (dz==1) {
            del = delz;
         }
         return -2*k/rho2*Math.pow(1-q2, k-1)*del;
      }
         
      if (dx == 2) {
         return (-2*k*(1-q2)/rho2+4*k*(k-1)*delx*delx/(rho2*rho2))*Math.pow(1-q2,k-2);
      } else if (dy==2) {
         return (-2*k*(1-q2)/rho2+4*k*(k-1)*dely*dely/(rho2*rho2))*Math.pow(1-q2,k-2);
      } else if (dz==2) {
         return (-2*k*(1-q2)/rho2+4*k*(k-1)*delz*delz/(rho2*rho2))*Math.pow(1-q2,k-2);
      } else if (dx ==1 && dy == 1) {
         return 4*k*(k-1)/(rho2*rho2)*Math.pow(1-q2, k-2)*delx*dely;
      } else if (dx ==1 && dz == 1) {
         return 4*k*(k-1)/(rho2*rho2)*Math.pow(1-q2, k-2)*delx*delz;
      } else if (dy ==1 && dz == 1) {
         return 4*k*(k-1)/(rho2*rho2)*Math.pow(1-q2, k-2)*dely*delz;
      }
      
      return 0;
   }

   @Override
   public RadialWeightFunctionType getType() {
      return RadialWeightFunctionType.SPLINE;
   }

}
