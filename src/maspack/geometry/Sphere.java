/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;

public class Sphere {
   private double radius;
   private Point3d center;
   
   public Sphere(double rad, Point3d c) {
      setCenter(c);
      setRadius(rad);
   }
   
   public double getRadius() {
      return radius;
   }
   
   public Point3d getCenter() {
      return center;
   }
   
   public void setCenter(Point3d c) {
      center = c;
   }
   
   public void setRadius(double rad) {
      radius = rad;
   }
   
   public boolean intersects(Sphere sphere) {
      if (center.distance(sphere.center) < radius + sphere.radius) {
         return true;
      }
      return false;
   }
   
   public boolean containsPoint(Point3d pnt) {
      if (center.distance(pnt) < radius) {
         return true;
      }
      return false;
   }

}
