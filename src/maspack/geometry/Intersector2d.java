/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

/**
 * Used to perform 2D intersections, or solve intersection problems on the plane
 * @author Antonio
 */
public class Intersector2d {
   public double epsilon = 1e-24;

   public void setEpsilon (double e) {
      epsilon = e;
   }

   public double getEpsilon() {
      return epsilon;
   }

   public int intersectTriangleLineSegment(
      Point2d v0, Point2d v1, Point2d v2, 
      Point2d l0, Point2d l1, ArrayList<Point2d> points) {
      
      ArrayList<Point2d> pnts2d = new ArrayList<Point2d>();
      
      // check if endpoints are inside
      Point3d uvw = new Point3d();
      getBarycentric(l0, v0, v1, v2, uvw);
      if (uvw.x > -epsilon && uvw.y > -epsilon && uvw.z > -epsilon) {
         pnts2d.add(l0);
      }
      
      getBarycentric(l1, v0, v1, v2, uvw);
      if (uvw.x > -epsilon && uvw.y > -epsilon && uvw.z > -epsilon) {
         pnts2d.add(l1);
      }
      
      // check line-line intersections
      intersectLineSegmentLineSegment(v0, v1, l0, l1, pnts2d);
      intersectLineSegmentLineSegment(v1, v2, l0, l1, pnts2d);
      intersectLineSegmentLineSegment(v2, v0, l0, l1, pnts2d);
           
      // reduce
      pnts2d = getUnique(pnts2d, epsilon);
      points.addAll(pnts2d);
      
      return pnts2d.size();

   }
   
   public static ArrayList<Point2d> getUnique(ArrayList<Point2d> pnts, double tol) {
      ArrayList<Point2d> out = new ArrayList<Point2d>(pnts.size());
      for (Point2d pnt : pnts) {
         boolean unique = true;
         for (Point2d p : out) {
            if (p.distance(pnt)<tol) {
               unique = false;
               break;
            }
         }
         if (unique) {
            out.add(pnt);
         }
      }
      return out;
   }
   
   public static Point3d get3dCoordinate(Point2d pnt, Vector3d vx, Vector3d vy, Point3d o) {
      Point3d out = new Point3d(o);
      out.scaledAdd(pnt.x, vx);
      out.scaledAdd(pnt.y, vy);
      return out;
   }
   
   public static Point2d get2dCoordinate(Point3d pnt, Vector3d vx, Vector3d vy, Point3d o) {
      double dx = pnt.x-o.x;
      double dy = pnt.y-o.y;
      double dz = pnt.z-o.z;
      return new Point2d(dx*vx.x+dy*vx.y+dz*vx.z,dx*vy.x+dy*vy.y+dz*vy.z); 
   }
   
   public static Point3d getBarycentric(Point2d p, Point2d p1, Point2d p2, Point2d p3, Point3d uvw) {
      
      if (uvw == null) {
         uvw = new Point3d();
      }
      double d = ((p2.y - p3.y)*(p1.x - p3.x) + (p3.x - p2.x)*(p1.y - p3.y));
      uvw.x = ((p2.y - p3.y)*(p.x - p3.x) + (p3.x - p2.x)*(p.y - p3.y)) / d;
      uvw.y = ((p3.y - p1.y)*(p.x - p3.x) + (p1.x - p3.x)*(p.y - p3.y)) / d;
      uvw.z = 1 - uvw.x - uvw.y;
      return uvw;
      
   }
   
   public static Vector2d get2dVector(Vector3d v, Vector3d vx, Vector3d vy) {
      return new Vector2d(v.dot(vx), v.dot(vy));
   }
   
   public int intersectLineLine(Point2d c1, Vector2d v1, Point2d c2, Vector2d v2, ArrayList<Point2d> points) {
      int nAdded = 0;

      Vector2d n1 = new Vector2d(-v1.y, v1.x);
      Vector2d n2 = new Vector2d(-v2.y, v2.x);
      n1.normalize();
      n2.normalize();
      double b1 = c1.dot(n1);
      double b2 = c2.dot(n2);

      double d = n1.x*n2.y-n1.y*n2.x;  // denominator, if zero lines are parallel

      if (Math.abs(d)<epsilon) {
         d = n1.dot(c2)-b1;
         d = d/n1.norm();     // distance of c2 to line 1

         if (Math.abs(d) < epsilon) {
            // lines are colinear, so add both
            // add both points
            points.add(c1);
            points.add(c2);
            nAdded = 2;

         } else {
            nAdded = 0;
         }

      } else {
         double x = (n2.y*b1-n1.y*b2)/d;
         double y = (-n2.x*b1+n1.x*b2)/d;
         points.add(new Point2d(x,y));
         nAdded++;
      }

      return nAdded;

   }
   
   public int intersectLineLineSegment(Point2d p1, Vector2d v1, Point2d p2a, Point2d p2b, ArrayList<Point2d> points) {
      
      Vector2d v2 = new Vector2d(p2b.x-p2a.x, p2b.y-p2a.y);
      
      // intersect lines
      ArrayList<Point2d> tmpPnts = new ArrayList<Point2d>();
      int nAdded = intersectLineLine(p1, v1, p2a, v2, tmpPnts);
      
      if (nAdded == 1) {
         Point2d p = tmpPnts.get(0);
         if (p.distance(p2a)+p.distance(p2b) > p2a.distance(p2b)+epsilon) {
            return 0;
         }
         points.add(p);
         return 1;
      }
      
      if (nAdded == 2) {
         points.addAll(tmpPnts);
         return 2;
      }
      
      return 0;
      
   }

   public int intersectLineSegmentLineSegment(Point2d p1a, Point2d p1b, Point2d p2a, Point2d p2b, 
      ArrayList<Point2d> points) {

      Vector2d dir1 = new Vector2d(p1b);
      dir1.sub(p1a);
      double l1 = dir1.norm();
      dir1.scale(1.0/l1);  // normalize
      
      Vector2d dir2 = new Vector2d(p2b);
      dir2.sub(p2a);
      double l2 = dir2.norm();
      dir2.scale(1.0/l2);

      // circle test
      Point2d a = new Point2d();
      a.combine(0.5, p1a, 0.5, p1b);

      Point2d b = new Point2d();
      b.combine(0.5, p2a, 0.5, p2b);

      // check if within each other's radii
      if (a.distance(b) > (l1 + l2)/2 + epsilon) {
         return 0;
      }

      // within range, so intersect lines
      ArrayList<Point2d> tmpPnts = new ArrayList<Point2d>();
      int nAdded = intersectLineLine(p1a, dir1, p2a, dir2, tmpPnts);

      // lines intersect at a single points, check that it exists on both segments
      // we already know it lies on both lines, so just check distances
      if (nAdded == 1) {
         Point2d p = tmpPnts.get(0);         
         if (p.distance(p1a)+p.distance(p1b) > l1+epsilon) {
            return 0;
         }
         
         if (p.distance(p2a)+p.distance(p2b) > l2+epsilon) {
            return 0;
         }
         points.add(p);
         return 1;
         
      }
      
      // parallel
      if (nAdded == 0) {
         return 0;
      }

      // colinear
      Vector2d v = dir1;

      // get direction vector
      double tmp;

      // end points of interval
      // segment 1: [t1s, t1e]
      double t1s = p1a.dot(v);
      double t1e = p1b.dot(v);
      if (t1s < t1e) {
         tmp = t1s;
         t1s = t1e;
         t1e = tmp;
      }
      // segment 2: [t2s, t2e]
      double t2s = p2a.dot(v);
      double t2e = p2b.dot(v);
      if (t2s < t2e) {
         tmp = t2s;
         t2s = t2e;
         t2e = tmp;
      }

      double ts = Math.max(t1s, t2s);
      double te = Math.min(t1e, t2e);

      if (te > ts-epsilon) {
         Vector2d n = new Vector2d();
         n.scaledAdd(-p1a.dot(v), v, p1a);

         a.scaledAdd(ts, v, n);
         b.scaledAdd(te, v, n);
         if (a.distance(b)<epsilon) {
            points.add(a);
            return 1;
         } else {
            points.add(a);
            points.add(b);
            return 2;
         }
      }
      return nAdded;
      
   }
   

}
