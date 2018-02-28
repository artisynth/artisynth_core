package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

public class SphereIntersector {

   /**
    * Intersects two spheres, finding the distance along
    * the line from c1 to c2 for the intersection plane
    * @param c1  center of circle 1
    * @param r1  radius of circle 1
    * @param c2  center of circle 2
    * @param r2  radias of circle 2
    * @param axis from c1 to c2
    * @param xdv x populated with distance along axis, d is the distance from c1 to c2, v is the volume of the intersection
    * @return intersection volume
    */
   public static double intersect(Point3d c1, double r1, Point3d c2, double r2, Vector3d axis, Vector3d xdv) {
            
      // c2 -> c1
      if (axis == null) {
         axis = new Vector3d();
      }
      axis.sub(c2, c1);
      double d = axis.norm();
      
      // zero overlap
      if (d > r1 + r2) {
         if (xdv != null) {
            xdv.x = (r1 + d - r2)/2;
            xdv.y = d;
            xdv.z = 0;
         }
         return 0;
      }
      
      if (d > (r1+r2)*1e-10/2) {
         axis.normalize();
      } else {
         axis.set(0,0,1);
      }
      
      if (d <= r2 - r1) {
         // r1 is inside r2
         double v = 4*Math.PI*r2*r2*r2/3;
         if (xdv != null) {
            xdv.x = -r2;
            xdv.y = d;
            xdv.z = v;
         }
         return v;
      } else if (d <= r1-r2) {
         // r2 is inside r1
         double v = 4*Math.PI*r1*r1*r1/3;
         if (xdv != null) {
            xdv.x = r1;
            xdv.y = d;
            xdv.z = v;
         }
         return v;
      }
      
      double x = (d*d-r2*r2+r1*r1)/2*d;
      // double rr = Math.sqrt(r1*r1-x*x); // intersection circle
      double d1 = x;
      double d2 = d-x;
      // h1 is height of cap 1
      // h2 is height of cap 2
      double h1 = r1-d1;
      double h2 = r2-d2;
      
      double v1 = Math.PI*h1*h1*(3*r1-h1);
      double v2 = Math.PI*h2*h2*(3*r2-h2);
      double v = v1+v2;
      
      if (xdv != null) {
         xdv.x = x;
         xdv.y = d;
         xdv.z = v;
      }
      
      return v;
   }
   
   /**
    * Bounds the intersection of two spheres
    * @param c1 center of sphere 1
    * @param r1 radius of sphere 1
    * @param c2 center of sphere 2
    * @param r2 radius of sphere 2
    * @return oriented bounding box
    * 
    */
   public static OBB bound(Point3d c1, double r1, Point3d c2, double r2) {
      Vector3d axis = new Vector3d();
      Vector3d xdv = new Vector3d();
      
      double v = intersect(c1, r1, c2, r2, axis, xdv);
      if (v <= 0) {
         return null;
      }
      
      RigidTransform3d trans = new RigidTransform3d();
      
      Vector3d widths = new Vector3d();
      if (xdv.y <= r2-r1) {
         // inside r2
         trans.setTranslation(c2);
         widths.set(r2, r2, r2);
      } else if (xdv.y <= r1-r2) {
         // inside r1
         trans.setTranslation(c1);
         widths.set(r1, r1, r1);
      } else {
         trans.R.rotateZDirection(axis);
         double x = xdv.x;
         double d = xdv.y;
         
         double d1 = x;
         double d2 = d-x;
         // h1 is height of cap 1
         // h2 is height of cap 2
         double h1 = r1-d1;
         double h2 = r2-d2;
         
         if (h1 > r1) {
            // r1 sticks out
            widths.x = r1;
            widths.y = r1;            
         } else if (h2 > r2) {
            // r2 sticks out
            widths.x = r2;
            widths.y = r2;
         } else {
            // bounded by intersection circle
            double r = Math.sqrt(r1*r1-x*x);
            widths.x = r;
            widths.y = r;
         }
         widths.z = h1+h2;
         
         // center-z is half-way between (x+h1) and (x-h2)
         Vector3d c = new Vector3d(0,0, x + (h1-h2)/2);
         c.transform(trans.R);
         c.add(c1);
         trans.setTranslation(c);
      }
      
      return new OBB(widths, trans);
   }
   
   /**
    * modifies min1, max1, returns volume
    */
   private static double boundIntersection(Vector3d min1, Vector3d max1, 
      Vector3d min2, Vector3d max2) {
      
      if (min2.x > min1.x) {
         min1.x = min2.x;
      }
      if (min2.y > min1.y) {
         min1.y = min2.y;
      }
      if (min2.z > min1.z) {
         min1.z = min2.z;
      }
      if (max2.x < max1.x) {
         max1.x = max2.x;
      }
      if (max2.y < max1.y) {
         max1.y = max2.y;
      }
      if (max2.z < max1.z) {
         max1.z = max2.z;
      }
      
      double dx = max1.x-min1.x;
      if (dx < 0) {
         return -1;
      }
      double dy = max1.y-min1.y;
      if (dy < 0) {
         return -1;
      }
      double dz = max1.z-min1.z;
      if (dz < 0) {
         return -1;
      }

      return dx*dy*dz;
   }
   
   /**
    * Bound the intersection of N spheres with a bounding box
    * @param c sphere centers
    * @param r sphere radii
    * @return axis-aligned bounding volume
    */
   public static AABB bound(Point3d[] c, double[] r) {
      
      if (c.length == 0) {
         return null;
      } else if (c.length == 1) {
         return new AABB(c[0].x-r[0], c[0].y-r[0], c[0].z-r[0], 
            c[0].x+r[0], c[0].y+r[0], c[0].z+r[0]);
      }
      
      Vector3d amax = new Vector3d();
      Vector3d amin = new Vector3d();
      Vector3d omax = new Vector3d();
      Vector3d omin = new Vector3d();
      
      amin.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      amax.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      
      // find circle with smallest radius
      int imin = 0;
      double rmin = r[0];
      for (int i=1; i<r.length; ++i) {
         if (r[i] < rmin) {
            rmin = r[i];
            imin = i;
         }
      }
      
      for (int i=0; i<c.length; ++i) {
         if (i != imin) {
            OBB o0 = bound(c[imin], r[imin], c[i], r[i]);
            if (o0 == null) {
               return null;  // no intersection between spheres
            }
            omax.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            omin.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            o0.updateBounds(omax, omin);
            double v = boundIntersection(amin, amax, omin, omax);
            if (v < 0) {
               return null;
            }
         }
      }
      
      return new AABB(amin.x, amin.y, amin.z, amax.x, amax.y, amax.z);
   }
   
}
