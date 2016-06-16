/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public abstract class RadialWeightFunction extends MFreeWeightFunction {

   public enum RadialWeightFunctionType {
      C_INFINITY, EXPONENTIAL, SPLINE
   }
   
   public Point3d center = new Point3d();
   public double myRadius;
   
   public abstract double eval(double r2);
   public abstract RadialWeightFunction clone();
   public abstract RadialWeightFunctionType getType();
   
   public double eval(Point3d pnt) {
      return eval(pnt.distanceSquared(center));
   }

   public void setRadius(double r) {
      myRadius = r;
   }
   
   public double getRadius() {
      return myRadius;
   }
   
   public double eval(double x, double y, double z) {
      double r2 = (x-center.x)*(x-center.x)+
                  (y-center.y)*(y-center.y)+
                  (z-center.z)*(z-center.z);
      return eval(r2);
   }
   
   public double eval(double[] in) {
      return eval(in[0]-center.x,
         in[1]-center.y,
         in[2]-center.z);
   }
   
   public void setCenter(Point3d c) {
      center.set(c);
   }
   
   public Point3d getCenter() {
      return center;
   }

   public int getInputSize() {
      return 3;
   }
   
   public double getIntersectionVolume(RadialWeightFunction fun) {
      
      double d = fun.getCenter().distance(getCenter());
      double r1 = getRadius();
      double r2 = fun.getRadius();
      
      // make r1 the smaller radius
      if (r1 > r2) {
         double tmp = r1;
         r1 = r2;
         r2 = tmp;
      }
      
      double v = 0;
      
      if (d > r1+r2) {
         // disjoint
         v = 0;
      } else if (d <= r2-r1){
         // completely inside
         v = Math.PI*4.0/3.0*r1*r1*r1;
      } else {
         v = Math.PI/(12.0*d)*(r1+r2-d)*(r1+r2-d)*(d*d+2*d*r1-3*r1*r1+2*d*r2+6*r1*r2-3*r2*r2);
      }
      
      return v;
      
   }
   
   public double getIntersectionVolume(MFreeWeightFunction fun) {
      
      if (fun instanceof RadialWeightFunction) {
         return getIntersectionVolume((RadialWeightFunction)fun);
      }
      throw new IllegalArgumentException("Not yet implemented");
      
   }
   
   public void computeIntersectionCentroid(Point3d centroid, RadialWeightFunction fun) {
      
      Point3d c1 = getCenter();
      Point3d c2 = fun.getCenter();
      double d = c1.distance(c2);
      
      double r1 = getRadius();
      double r2 = fun.getRadius();
      
      if (d <= r2-r1){
         // completely inside
         centroid.set(c1);
      } else if (d <= r1-r2) {
         centroid.set(c2);
      } else {
         centroid.sub(c2, c1);
         centroid.normalize();
         centroid.scale(r1-r2);
         centroid.add(c1);
         centroid.add(c2);
         centroid.scale(0.5);
      }
   }
   
   public void computeIntersectionCentroid(Point3d centroid, MFreeWeightFunction fun) {
      if (fun instanceof RadialWeightFunction) {
         computeIntersectionCentroid(centroid, (RadialWeightFunction)fun);
      } else {
         throw new IllegalArgumentException("Not yet implemented");
      }
   }
   
   public void computeCentroid(Vector3d centroid) {
      centroid.set(center);
   }

   public void updateBounds(Vector3d min, Vector3d max) {
      
      Point3d myMin = new Point3d(center);
      myMin.add(-myRadius, -myRadius, -myRadius);
      Point3d myMax = new Point3d(center);
      myMax.add(myRadius, myRadius, myRadius);
      myMin.updateBounds(min, max);
      myMax.updateBounds(min, max);
      
   }
   
   
   public boolean intersects(RadialWeightFunction fun) {
      if (fun.center.distance(center)<fun.getRadius()+getRadius()) {
         return true;
      }
      return false;
   }
   
   public boolean intersects(MFreeWeightFunction fun) {
      if (fun instanceof RadialWeightFunction) {
         return intersects((RadialWeightFunction)fun);
      }
      //XXX other functions
      throw new IllegalArgumentException("Not yet implemented");
   }
   
   public static RadialWeightFunction createWeightFunction(RadialWeightFunctionType type, Point3d c, double radius) {
      switch(type) {
         case C_INFINITY:
            return new CInfinityWeightFunction(c, radius);
         case EXPONENTIAL:
            return new ExponentialWeightFunction(c, radius);
         case SPLINE:
            return new SplineWeightFunction(c, radius);
      }
      return null;
   }
   
   public boolean isInDomain(Point3d pos, double tol) {
      return pos.distance(center) < getRadius()+tol;
   }
   
}
