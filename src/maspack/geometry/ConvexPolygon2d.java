/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.DynamicIntArray;
import java.util.*;

public class ConvexPolygon2d extends Polygon2d {
   public ConvexPolygon2d() {
      firstVertex = null;
   }

   public ConvexPolygon2d (double[] coords) {
      set (coords, coords.length / 2);
   }

   public ConvexPolygon2d (Point2d[] pnts) {
      set (pnts, pnts.length);
   }

   public ConvexPolygon2d (Collection<Point2d> pnts) {
      set (pnts.toArray(new Point2d[0]), pnts.size());
   }

   public double area() {
      if (numVertices() < 3) {
         return 0;
      }
      Vertex2d vtx = firstVertex;
      double a = 0;
      Point2d pnt0 = vtx.pnt;

      vtx = vtx.next;
      double dx2 = vtx.pnt.x - pnt0.x;
      double dy2 = vtx.pnt.y - pnt0.y;

      vtx = vtx.next;
      while (vtx != firstVertex) {
         double dx1 = dx2;
         double dy1 = dy2;

         dx2 = vtx.pnt.x - pnt0.x;
         dy2 = vtx.pnt.y - pnt0.y;

         a += (dx1 * dy2 - dy1 * dx2);

         vtx = vtx.next;
      }
      return a / 2;
   }

   private static class LexicalComparatorOld implements Comparator<Point2d> {

      LexicalComparatorOld () {
      }
      
      public int compare (Point2d p1, Point2d p2) {
         if (p1.x == p2.x) {
            if (p1.y < p2.y) {
               return -1;
            }
            else {
               return p1.y > p2.y ? 1 : 0;
            }
         }
         else {
            return p1.x > p2.x ? 1 : -1;
         }
      }
   }

   /**
    * Computes a 2D convex hull from a set on input points, using the Graham
    * Scan algorithm, which is O(n log n).
    */
   public static ArrayList<Point2d> computeOld (
      Collection<Point2d> input, double tol) {
      ArrayList<Point2d> points = new ArrayList<>();
      ArrayList<Point2d> upper = new ArrayList<>();
      ArrayList<Point2d> lower = new ArrayList<>();

      if (input.size() < 3) {
         throw new IllegalArgumentException (
            "Number of input points must be at least 3");
      }
      points.addAll (input);

      Collections.sort (points, new LexicalComparatorOld());
      System.out.println ("sorted:");
      for (Point2d p : points) {
         System.out.println ("  "+p.toString("%8.3f"));
      }
      for (int k=0; k<points.size(); k++) {
         Point2d p = points.get(k);
         int nu;
         while ((nu=lower.size()) > 1 &&
                counterClockwise (
                   lower.get(nu-2), lower.get(nu-1), p, tol) <= 0) {
            lower.remove (nu-1);
         }
         lower.add (p);
      }
      for (int k=points.size()-1; k>=0; k--) {
         Point2d p = points.get(k);
         int nl;
         while ((nl=upper.size()) > 1 &&
                counterClockwise (
                   upper.get(nl-2), upper.get(nl-1), p, tol) <= 0) {
            upper.remove (nl-1);
         }
         upper.add (p);
      }
      lower.remove (lower.size()-1);
      upper.remove (upper.size()-1);
      lower.addAll (upper);
      return lower;
   }

   private static class LexicalComparator implements Comparator<Integer> {

      ArrayList<Point2d> myPoints;

      LexicalComparator (ArrayList<Point2d> points) {
         myPoints = points;
      }
      
      public int compare (Integer idx1, Integer idx2) {
         Point2d p1 = myPoints.get(idx1);
         Point2d p2 = myPoints.get(idx2);
         if (p1.x == p2.x) {
            if (p1.y < p2.y) {
               return -1;
            }
            else {
               return p1.y > p2.y ? 1 : 0;
            }
         }
         else {
            return p1.x > p2.x ? 1 : -1;
         }
      }
   }

   private static void removeColinearPoints (
      DynamicIntArray idxs, ArrayList<Point2d> points, double angTol) {
      
      DynamicIntArray removeIdxs = new DynamicIntArray();
      if (idxs.size() <= 3) {
         return;
      }
      Point2d p0 = points.get(idxs.get(idxs.size()-1));
      Point2d p1 = points.get(idxs.get (0));
      for (int k=0; k<idxs.size(); k++) {
         Point2d p2 = points.get(idxs.get((k+1)%idxs.size()));
         if (counterClockwise (p0, p1, p2, angTol) <= 0) {
            removeIdxs.add (k);
            p1 = p2;
         }
         else {
            p0 = p1;
            p1 = p2;
         }
      }
      for (int i=removeIdxs.size()-1; i>=0; i--) {
         idxs.remove (removeIdxs.get(i));
      }    
   }


   /**
    * Computes the indices of a 2D convex hull within a set of input points,
    * using the Monotone Chain algorithm, by A. M. Andrew, 1979 (which is a
    * variation on the Graham scan method).
    *
    * @param input points from which the hull should be formed
    * @param angTol if {@code > 0}, specifies an angular tolerance which should
    * be used to remove nearly colinear points
    * @return list of the indices of the point in {@code input} which
    * form the hull, in counter-clockwise order
    */
   public static int[] computeHullIndices (
      Collection<Point2d> input, double angTol) {
      ArrayList<Point2d> points = new ArrayList<>();
      DynamicIntArray upper = new DynamicIntArray();
      DynamicIntArray lower = new DynamicIntArray();

      if (input.size() < 3) {
         throw new IllegalArgumentException (
            "Number of input points must be at least 3");
      }
      points.addAll (input);
      ArrayList<Integer> allidxs = new ArrayList<>(points.size());
      for (int i=0; i<points.size(); i++) {
         allidxs.add (i);
      }
      Collections.sort (allidxs, new LexicalComparator(points));
      for (int k=0; k<allidxs.size(); k++) {
         int pi = allidxs.get(k);
         int nu;
         while ((nu=lower.size()) > 1 &&
                counterClockwise (
                   points.get(lower.get(nu-2)),
                   points.get(lower.get(nu-1)),
                   points.get(pi), 0) <= 0) {
            lower.remove (nu-1);
         }
         lower.add (pi);
      }
      for (int k=allidxs.size()-1; k>=0; k--) {
         int pi = allidxs.get(k);
         int nl;
         while ((nl=upper.size()) > 1 &&
                counterClockwise (
                   points.get(upper.get(nl-2)),
                   points.get(upper.get(nl-1)),
                   points.get(pi), 0) <= 0) {
            upper.remove (nl-1);
         }
         upper.add (pi);
      }
      lower.remove (lower.size()-1);
      upper.remove (upper.size()-1);
      lower.addAll (upper);
      if (angTol > 0) {
         removeColinearPoints (lower, points, angTol);
      }
      return lower.toArray();
   }




   /**
    * Computes a 2D convex hull for a set of input points, using the Monotone
    * Chain algorithm, by A. M. Andrew, 1979 (which is a variation on the
    * Graham scan method).
    *
    * @param input points from which the hull should be formed
    * @param angTol if {@code > 0}, specifies an angular tolerance which should
    * be used to remove nearly colinear points
    * @return a list of points, copied from {@code input}, which form the hull
    * in counter-clockwise order
    */
   public static ArrayList<Point2d> computeHull (
      List<Point2d> input, double angTol) {

      int[] indices = computeHullIndices (input, angTol);
      ArrayList<Point2d> hull = new ArrayList<>();
      for (int i : indices) {
         hull.add (input.get(i));
      }
      return hull;
   }

   public void computeAndSetHull (List<Point2d> input, double angTol) {
      set (computeHull (input, angTol));
   }

   /**
    * Computes (b-a) X (c-a)
    */
   private double area (Point2d a, Point2d b, Point2d c) {
      double dbx = b.x - a.x;
      double dby = b.y - a.y;
      double dcx = c.x - a.x;
      double dcy = c.y - a.y;

      return dbx * dcy - dcx * dby;
   }

   /**
    * Returns 1 or -1 depending on whether pnt is (strictly) inside or outside
    * of poly.
    * 
    * @return 1 for inside and -1 for outside
    */
   public int pointIsInside (Point2d pnt) {
      Vertex2d vtx = firstVertex;

      if (vtx != null) {
         do {
            if (area (pnt, vtx.prev.pnt, vtx.pnt) <= 0) {
               return -1;
            }
            vtx = vtx.next;
         }
         while (vtx != firstVertex);
         return 1;
      }
      else {
         return -1;
      }
   }

   /**
    * Intersects the convex polygon with a line defined by p0 + s u.  Returns
    * null if there is no intersection, and otherwise returns [smin, smax],
    * giving the minimum and maximum values of s associated with the
    * intersection.
    *
    * @param p0 origin point for the line
    * @param u direction of the line
    * @return intersection interval, or null
    */
   public double[] intersectLine (Point2d p0, Vector2d u) {
      double smin = INF;
      double smax = -INF;
      Vertex2d vtx = firstVertex;
      do {
         double s = intersectLine (null, vtx, p0, u);
         if (s != INF) {
            if (s < smin) {
               smin = s;
            }
            if (s > smax) {
               smax = s;
            }
         }
         vtx = vtx.getNext();
      }
      while (vtx != firstVertex);
      if (smin == INF) {
         return null;
      }
      else {
         return new double[] {smin, smax};
      }
   }

}

