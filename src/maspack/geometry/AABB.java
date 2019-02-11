/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;

public class AABB extends BVNode {

   Point3d myMin = new Point3d();
   Point3d myMax = new Point3d();

   private static final double INF = Double.POSITIVE_INFINITY;

   public AABB() {
   }

   public AABB (double minx, double miny, double minz, 
                    double maxx, double maxy, double maxz) {
      setMinimums (minx, miny, minz);
      setMaximums (maxx, maxy, maxz);
   }

   public double getRadius() {
      return myMin.distance(myMax)/2;
   }

   public void getCenter (Vector3d center) {
      center.add (myMin, myMax);
      center.scale (0.5);
   }
   
   public void getHalfWidths(Vector3d hw) {
      hw.sub(myMax, myMin);
      hw.scale(0.5);
   }

   public void setMinimums (double minx, double miny, double minz) {
      myMin.x = minx;
      myMin.y = miny;
      myMin.z = minz;
   }

   public void setMaximums (double maxx, double maxy, double maxz) {
      myMax.x = maxx;
      myMax.y = maxy;
      myMax.z = maxz;
   }

   public boolean containsPoint (Point3d pnt) {
      boolean status =
         (myMin.x <= pnt.x && pnt.x <= myMax.x &&
          myMin.y <= pnt.y && pnt.y <= myMax.y &&
          myMin.z <= pnt.z && pnt.z <= myMax.z);
      return status;
   }

   public boolean intersectsSphere (Point3d pnt, double r) {
      boolean status =
         (myMin.x <= pnt.x+r && pnt.x-r <= myMax.x && 
          myMin.y <= pnt.y+r && pnt.y-r <= myMax.y && 
          myMin.z <= pnt.z+r && pnt.z-r <= myMax.z);
      return status;
   }
   
   public boolean intersectsLine (
      double[] dists, Point3d origin, Vector3d dir, double min, double max) {

      double dist0, dist1, invdir;
      
      if (dir.x == 0) {
         if (myMin.x - origin.x > 0 || myMax.x - origin.x < 0) {
            return false;
         }
      } else {
         invdir = 1.0 / dir.x;
         dist0 = (myMin.x - origin.x)*invdir;
         dist1 = (myMax.x - origin.x)*invdir;
         if (dir.x > 0) {
            if (dist0 > min ) {
               min = dist0;
            }
            if (dist1 < max) {
               max = dist1;
            }
         } else {
            if (dist1 > min)
               min = dist1;
            if (dist0 < max)
               max = dist0;
         }
         if (min > max)
            return false;
      }

      if (dir.y == 0) {
         if (myMin.y - origin.y > 0 || myMax.y - origin.y < 0) {
            return false;
         }
      }
      else {
         invdir = 1.0 / dir.y;
         dist0 = (myMin.y - origin.y)*invdir;
         dist1 = (myMax.y - origin.y)*invdir;
         if (dir.y > 0) {
            if (dist0 > min ) {
               min = dist0;
            }
            if (dist1 < max) {
               max = dist1;
            }
         } else {
            if (dist1 > min)
               min = dist1;
            if (dist0 < max)
               max = dist0;
         }
         if (min > max)
            return false;
      }

      if (dir.z == 0) {
         if (myMin.z - origin.z > 0 || myMax.z - origin.z < 0) {
            return false;
         }
      }
      else {
         invdir = 1.0 / dir.z;
         dist0 = (myMin.z - origin.z)*invdir;
         dist1 = (myMax.z - origin.z)*invdir;
         if (dir.z > 0) {
            if (dist0 > min ) {
               min = dist0;
            }
            if (dist1 < max) {
               max = dist1;
            }
         } else {
            if (dist1 > min)
               min = dist1;
            if (dist0 < max)
               max = dist0;
         }
         if (min > max)
            return false;
      }
      if (dists != null) {
         dists[0] = min;
         dists[1] = max;
      }
      return true;
   }

   @Override
   public boolean intersectsPlane(Vector3d n, double d) {
      
      // check plane has at least one corner on each side
      boolean up = false;
      boolean down = false;
      double[] x = {myMin.x, myMax.x};
      double[] y = {myMin.y, myMax.y};
      double[] z = {myMin.z, myMax.z};
      double b = 0;
      for (int i=0; i<2; i++) {
         for  (int j=0; j<2; j++) {
            for (int k=0; k<2; k++) {
               b = n.x*x[i] + n.y*y[j] + n.z*z[k] - d;
               up = up | (b >=0);
               down = down | (b<=0);
               if (up && down) {
                  return true;
               }
            }
         }
      }
      
      return false;
   }

   @Override
   public boolean intersectsLineSegment(Point3d p1, Point3d p2) {
      
      if (containsPoint(p1) || containsPoint(p2)) {
         return true;
      }
      
      double[] dir = {p2.x-p1.x, p2.y-p1.y, p2.z-p1.z};
      double[] min = {myMin.x, myMin.y, myMin.z};
      double[] max = {myMax.x, myMax.y, myMax.z};
      double[] p1v = {p1.x, p1.y, p1.z};
      // double[] p2v = {p2.x, p2.y, p2.z};

      double near = Double.NEGATIVE_INFINITY;
      double far = Double.POSITIVE_INFINITY;
      double t1, t2, tMin, tMax;
      
      // check line/plane intersections
      for (int i=0; i<3; i++) {
         if (dir[i] == 0) {
            // outside range of [min[i], max[i]]
            if ( (p1v[i] < min[i]) || (p1v[i] > max[i])) {
               return false;
            }
         } else {
            // intersect with near/far planes
            t1 = (min[i]-p1v[i]) / dir[i];
            t2 = (max[i]-p1v[i]) / dir[i];
            tMin = Math.min(t1, t2);
            tMax = Math.max(t1, t2);
            if (tMin > near) {
               near = tMin;
            }
            if (tMax < far) {
               far = tMax;
            }
            if (near > far) 
               return false;  
         }
      }
      
      if ((near >= 0 && near <= 1) || (far >= 0 && far <=1)){
         return true;
      }
      return false;
   }
   
//   public void set (
//      IndexedPointSource[] elementList, int nelems, Point3d[] points) {
//      set(elementList, nelems, defaultTolerance);
//   }
//   
   public void set (
      Boundable[] elementList, int nelems, double margin) {

      myMin.set (INF, INF, INF);
      myMax.set (-INF, -INF, -INF);
      for (int i = 0; i < nelems; i++) {
         elementList[i].updateBounds (myMin, myMax);
      }
      myMin.add(-margin, -margin, -margin);
      myMax.add(margin, margin, margin);
   }

   public void set (AABB aabb, double margin) {
      myMin.x = aabb.myMin.x - margin;
      myMin.y = aabb.myMin.y - margin;
      myMin.z = aabb.myMin.z - margin;
      myMax.x = aabb.myMax.x + margin;
      myMax.y = aabb.myMax.y + margin;
      myMax.z = aabb.myMax.z + margin;
   }

   public boolean updateForPoint(Point3d pnt, double margin) {
      boolean modified = false;
      double diff = 0;
      if ((diff = pnt.x + margin) > myMax.x) {
         myMax.x = diff;
         modified = true;
      }
      if ((diff = pnt.x - margin) < myMin.x) {
         myMin.x = diff;
         modified = true;
      }
      if ((diff = pnt.y + margin) > myMax.y) {
         myMax.y = diff;
         modified = true;
      }
      if ((diff = pnt.y - margin) < myMin.y) {
         myMin.y = diff;
         modified = true;
      }
      if ((diff = pnt.z + margin) > myMax.z) {
         myMax.z = diff;
         modified = true;
      }
      if ((diff = pnt.z - margin) < myMin.z) {
         myMin.z = diff;
         modified = true;
      }
     
      
      return modified;
   }
   
   public boolean update (double margin) {
      // just reset
      set (myElements, myElements.length, margin);
      return true;
   }

   public boolean isContained (Boundable[] boundables, double tol) {
      AABB aabb = new AABB();
      aabb.set (boundables, boundables.length, 0);
      return (aabb.myMax.x+tol <= myMax.x && 
              aabb.myMax.y+tol <= myMax.y &&
              aabb.myMax.z+tol <= myMax.z &&
              aabb.myMin.x-tol >= myMin.x &&
              aabb.myMin.y-tol >= myMin.y &&
              aabb.myMin.z-tol >= myMin.z);
   }

   /**
    * Grow this AABB if necessary to accommodate anothor aabb.
    *
    * @param aabb AABB that should fit inside this box
    * @param margin additional tolerance margin
    */
   public void updateForAABB (AABB aabb, double margin) {
      Point3d otherMax = aabb.myMax;
      Point3d otherMin = aabb.myMin;

      if (otherMax.x+margin > myMax.x) {
         myMax.x = otherMax.x+margin;
      }
      if (otherMin.x-margin < myMin.x) {
         myMin.x = otherMin.x-margin;
      }
      if (otherMax.y+margin > myMax.y) {
         myMax.y = otherMax.y+margin;
      }
      if (otherMin.y-margin < myMin.y) {
         myMin.y = otherMin.y-margin;
      }
      if (otherMax.z+margin > myMax.z) {
         myMax.z = otherMax.z+margin;
      }
      if (otherMin.z-margin < myMin.z) {
         myMin.z = otherMin.z-margin;
      }
   }

   public double distanceToPoint (Point3d pnt) {
      double x, y, z;
      if ((x = pnt.x-myMax.x) > 0) {
         if ((y = pnt.y-myMax.y) > 0) {
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
         else if ((y = pnt.y-myMin.y) > 0) {
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (x*x + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return x;
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (x*x + z*z);
            }
         }
         else { // pnt.y-myMin.y < 0
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
      }
      else if ((x = pnt.x-myMin.x) >= 0) {
         if ((y = pnt.y-myMax.y) > 0) {
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (y*y + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return y;
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (y*y + z*z);
            }
         }
         else if ((y = pnt.y-myMin.y) > 0) {
            if ((z = pnt.z-myMax.z) > 0) {
               return z;
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return 0;
            }
            else { // pnt.z-myMin.z < 0
               return -z;
            }
         }
         else { // pnt.y-myMin.y < 0
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (y*y + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return -y;
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (y*y + z*z);
            }
         }
      }
      else { // pnt.x-myMin.x < 0
         if ((y = pnt.y-myMax.y) > 0) {
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
         else if ((y = pnt.y-myMin.y) > 0) {
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (x*x + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return -x;
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (x*x + z*z);
            }
         }
         else { // pnt.y-myMin.y < 0
            if ((z = pnt.z-myMax.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pnt.z-myMin.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pnt.z-myMin.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
      }
   }

   /**
    * Code is modified from "An Efficient and Robust Ray-Box Intersection
    * Algorithm", Amy Williams, Steve Barrus, R. Keith Morley, Peter Shirley,
    * University of Utah.
    */
   public double distanceAlongLine (
      Point3d origin, Vector3d dir, double min, double max) {

      double tmin, tmax;

      if (dir.x == 0) {
         if (myMin.x - origin.x > 0 || myMax.x - origin.x < 0) {
            return Double.POSITIVE_INFINITY;
         }
      }
      else {
         double divx = 1/dir.x;
         if (divx >= 0) {
            tmin = (myMin.x - origin.x)*divx;
            tmax = (myMax.x - origin.x)*divx;
         }
         else {
            tmin = (myMax.x - origin.x)*divx;
            tmax = (myMin.x - origin.x)*divx;
         }
         if (tmin > min) {
            min = tmin;
         }
         if (tmax < max) {
            max = tmax;
         }
         if (min > max) {
            return Double.POSITIVE_INFINITY;
         }
      }

      if (dir.y == 0) {
         if (myMin.y - origin.y > 0 || myMax.y - origin.y < 0) {
            return Double.POSITIVE_INFINITY;
         }
      }
      else {
         double divy = 1/dir.y;
         if (divy >= 0) {
            tmin = (myMin.y - origin.y)*divy;
            tmax = (myMax.y - origin.y)*divy;
         }
         else {
            tmin = (myMax.y - origin.y)*divy;
            tmax = (myMin.y - origin.y)*divy;
         }
         if (tmin > min) {
            min = tmin;
         }
         if (tmax < max) {
            max = tmax;
         }
         if (min > max) {
            return Double.POSITIVE_INFINITY;
         }
      }

      if (dir.z == 0) {
         if (myMin.z - origin.z > 0 || myMax.z - origin.z < 0) {
            return Double.POSITIVE_INFINITY;
         }
      }
      else {
         double divz = 1/dir.z;
         if (divz >= 0) {
            tmin = (myMin.z - origin.z)*divz;
            tmax = (myMax.z - origin.z)*divz;
         }
         else {
            tmin = (myMax.z - origin.z)*divz;
            tmax = (myMin.z - origin.z)*divz;
         }
         if (tmin > min) {
            min = tmin;
         }
         if (tmax < max) {
            max = tmax;
         }
         if (min > max) {
            return Double.POSITIVE_INFINITY;
         }
      }
      if (min > 0) {
         return min;
      }
      else if (max < 0) {
         return -max;
      }
      else {
         return 0;
      }
   }

   public void updateBounds (Vector3d min, Vector3d max) {
      myMax.updateBounds (min, max);
      myMin.updateBounds (min, max);
   }

   public void prerender (RenderList list) {
   }
   
   public void render (Renderer renderer, int flags) {
      
      renderer.setShading (Shading.NONE);
      renderer.setColor (0f, 0f, 1f);
      renderer.beginDraw (DrawMode.LINE_LOOP);
      renderer.addVertex (myMin.x, myMin.y, myMin.z);
      renderer.addVertex (myMax.x, myMin.y, myMin.z);
      renderer.addVertex (myMax.x, myMax.y, myMin.z);
      renderer.addVertex (myMin.x, myMax.y, myMin.z);
      renderer.endDraw();
      renderer.beginDraw (DrawMode.LINE_LOOP);
      renderer.addVertex (myMin.x, myMin.y, myMax.z);
      renderer.addVertex (myMax.x, myMin.y, myMax.z);
      renderer.addVertex (myMax.x, myMax.y, myMax.z);
      renderer.addVertex (myMin.x, myMax.y, myMax.z);
      renderer.endDraw();
      renderer.beginDraw (DrawMode.LINES);
      renderer.addVertex (myMin.x, myMin.y, myMin.z);
      renderer.addVertex (myMin.x, myMin.y, myMax.z);
      renderer.addVertex (myMax.x, myMin.y, myMin.z);
      renderer.addVertex (myMax.x, myMin.y, myMax.z);
      renderer.addVertex (myMax.x, myMax.y, myMin.z);
      renderer.addVertex (myMax.x, myMax.y, myMax.z);
      renderer.addVertex (myMin.x, myMax.y, myMin.z);
      renderer.addVertex (myMin.x, myMax.y, myMax.z);
      renderer.endDraw();
      renderer.setShading (Shading.FLAT);
   }

   /**
    * {@inheritDoc}
    */
   public void scale (double s) {
      Vector3d inc = new Vector3d();
      getHalfWidths (inc);
      inc.scale (s-1);
      myMax.add (inc);
      myMin.sub (inc);
   }
  
}
