/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;

public class ConvexPolygon3d extends Polygon3d {
   private double myTol = 1e-8;

   public double getTolerance() {
      return myTol;
   }

   public void setTolerance (double tol) {
      myTol = tol;
   }

   public ConvexPolygon3d() {
      firstVertex = null;
   }

   public ConvexPolygon3d (double[] coords) {
      set (coords, coords.length / 3);
   }

   public ConvexPolygon3d (Point3d[] pnts) {
      set (pnts, pnts.length);
   }

   public double area() {
      if (numVertices() < 3) {
         return 0;
      }
      Vector3d del01 = new Vector3d();
      Vector3d del02 = new Vector3d();
      Vector3d xprod = new Vector3d();

      Vector3d xprod0 = null;

      double area = 0;
      Point3d pnt0 = firstVertex.pnt;
      PolygonVertex3d vtx = firstVertex.next;
      del01.sub (vtx.pnt, pnt0);
      vtx = vtx.next;

      while (vtx != firstVertex) {
         del02.sub (vtx.pnt, pnt0);
         xprod.cross (del01, del02);
         double a = xprod.norm();

         if (xprod0 == null) {
            xprod0 = new Vector3d (xprod);
         }
         else {
            if (xprod0.dot (xprod) < 0) {
               a = -a;
            }
         }
         area += a / 2;

         vtx = vtx.next;
         del01.set (del02);
      }
      return area;
   }

   public void computeCentroid (Vector3d centroid) {
      if (numVertices() == 1) {
         centroid.set (firstVertex.pnt);
         return;
      }
      else if (numVertices() == 2) {
         centroid.add (firstVertex.pnt, firstVertex.next.pnt);
         centroid.scale (0.5);
         return;
      }
      Vector3d del01 = new Vector3d();
      Vector3d del02 = new Vector3d();
      Vector3d xprod = new Vector3d();

      Vector3d xprod0 = null;

      centroid.setZero();
      double area = 0;
      Point3d pnt0 = firstVertex.pnt;
      PolygonVertex3d vtx = firstVertex.next;
      del01.sub (vtx.pnt, pnt0);
      vtx = vtx.next;

      while (vtx != firstVertex) {
         del02.sub (vtx.pnt, pnt0);
         xprod.cross (del01, del02);
         double a = xprod.norm();

         if (xprod0 == null) {
            xprod0 = new Vector3d (xprod);
         }
         else {
            if (xprod0.dot (xprod) < 0) {
               a = -a;
            }
         }
         centroid.scaledAdd (a, del01);
         centroid.scaledAdd (a, del02);

         area += a / 2;

         vtx = vtx.next;
         del01.set (del02);
      }
      centroid.scale (1 / (6 * area));
      centroid.add (firstVertex.pnt);
   }

   public void intersectHalfSpace (Plane plane) {
      if (firstVertex == null) {
         return;
      }
      PolygonVertex3d firstOut = null;
      PolygonVertex3d lastOut = null;

      // see if there is at least one clipped vertex
      PolygonVertex3d vtx = firstVertex;
      do {
         if (plane.distance (vtx.pnt) < 0) {
            firstOut = vtx;
            break;
         }
         vtx = vtx.next;
      }
      while (vtx != firstVertex);

      if (firstOut == null) { // everything is inside
         return;
      }
      if (firstOut == firstVertex) { // back up to find real first out
         for (vtx = firstOut.prev; vtx != firstVertex; vtx = vtx.prev) {
            if (plane.distance (vtx.pnt) >= 0) {
               break;
            }
         }
         if (vtx == firstVertex) { // everything is outside
            clear();
            return;
         }
         firstOut = vtx.next;
      }
      // look for last out
      for (vtx = firstOut.next; plane.distance (vtx.pnt) < 0; vtx = vtx.next)
         ;
      lastOut = vtx.prev;

      Vector3d utmp = new Vector3d();
      Point3d rpnt = new Point3d();

      double s;
      s = intersectSegment (rpnt, firstOut.prev.pnt, firstOut.pnt, utmp, plane);
      if (s > myTol) {
         addVertexAfter (new PolygonVertex3d (rpnt), firstOut.prev);
      }
      s = intersectSegment (rpnt, lastOut.next.pnt, lastOut.pnt, utmp, plane);
      if (s > myTol) {
         addVertexAfter (new PolygonVertex3d (rpnt), lastOut);
      }

      PolygonVertex3d vtxPrev = firstOut.prev;
      PolygonVertex3d vtxNext = lastOut.next;

      removeVertices (firstOut, lastOut);

      if (vtxPrev.pnt.distance (vtxNext.pnt) < myTol) {
         removeVertex (vtxPrev);
      }
      if (numVertices() < 3) {
         clear();
      }
   }

   public void computeNormal (Vector3d nrm) {
      if (numVertices() < 3) {
         return;
      }
      Vector3d del01 = new Vector3d();
      Vector3d del02 = new Vector3d();
      Vector3d xprod = new Vector3d();

      nrm.setZero();
      Point3d pnt0 = firstVertex.pnt;
      PolygonVertex3d vtx = firstVertex.next;
      del01.sub (vtx.pnt, pnt0);
      vtx = vtx.next;

      while (vtx != firstVertex) {
         del02.sub (vtx.pnt, pnt0);
         xprod.cross (del01, del02);
         nrm.add (xprod);

         vtx = vtx.next;
         del01.set (del02);
      }
      nrm.normalize();

      vtx = firstVertex;
      do {
         System.out.println (" dist=" + nrm.dot (vtx.pnt));
         vtx = vtx.next;
      }
      while (vtx != firstVertex);
   }

   private double intersectSegment (
      Point3d pnt, Point3d p1, Point3d p2, Vector3d utmp, Plane plane) {
      utmp.sub (p2, p1);
      double len = utmp.norm();
      if (len == 0) {
         pnt.set (p1);
         return 0;
      }
      utmp.scale (1 / len);
      double d1 = plane.distance (p1);
      double d2 = plane.distance (p2);

      double s = -d1 * len / (d2 - d1);

      if (s < 0) {
         s = 0;
      }
      else if (s > len) {
         s = len;
      }
      pnt.scaledAdd (s, utmp, p1);
      return s;
   }

   private static class LexicalComparator<P extends Point3d>
      implements Comparator<P> {
      RotationMatrix3d myRPW;

      LexicalComparator (RotationMatrix3d RPW) {
         myRPW = new RotationMatrix3d (RPW);
      }
      
      public int compare (P p1, P p2) {
         Vector3d pp1 = new Vector3d();
         pp1.inverseTransform (myRPW, p1);
         Vector3d pp2 = new Vector3d();
         pp2.inverseTransform (myRPW, p2);
         if (pp1.x == pp2.x) {
            if (pp1.y < pp2.y) {
               return -1;
            }
            else {
               return pp1.y > pp2.y ? 1 : 0;
            }
         }
         else {
            return pp1.x > pp2.x ? 1 : -1;
         }
      }
   }

   /**
    * Computes the convex hull, with respect to a given normal direction, of a
    * set of input points. In other words, the points are culled such their
    * projection into the plane defined by the normal forms a convex hull in
    * that plane. Te algorithm is based on a 3D version of the Graham Scan
    * algorithm, which is O(n log n).
    */
   public static <P extends Point3d> ArrayList<P> computeOld (
      Collection<P> input, Vector3d nrml, double tol) {
      ArrayList<P> points = new ArrayList<>();
      ArrayList<P> upper = new ArrayList<>();
      ArrayList<P> lower = new ArrayList<>();

      if (input.size() < 3) {
         throw new IllegalArgumentException (
            "Number of input points must be at least 3");
      }
      points.addAll (input);
      
      RotationMatrix3d RPW = new RotationMatrix3d();
      RPW.setZDirection (nrml);
      Collections.sort (points, new LexicalComparator(RPW));
      System.out.println ("sorted:");
      for (Point3d p : points) {
         Point3d pp = new Point3d();
         pp.inverseTransform (RPW, p);
         System.out.println ("  "+pp.toString ("%8.3f"));
      }
      for (int k=0; k<points.size(); k++) {
         P p = points.get(k);
         int nu;
         while ((nu=lower.size()) > 1 &&
                counterClockwise (
                   lower.get(nu-2), lower.get(nu-1), p, nrml, 0) <= 0) {
            System.out.println (" remove "+(nu-1));
            lower.remove (nu-1);
         }
         System.out.println (" add "+lower.size());
         lower.add (p);
      }
      for (int k=points.size()-1; k>=0; k--) {
         P p = points.get(k);
         int nl;
         while ((nl=upper.size()) > 1 &&
                counterClockwise (
                   upper.get(nl-2), upper.get(nl-1), p, nrml, 0) <= 0) {
            upper.remove (nl-1);
         }
         upper.add (p);
      }
      lower.remove (lower.size()-1);
      upper.remove (upper.size()-1);
      lower.addAll (upper);
      return lower;
   }

   /**
    * Computes the indices of the convex hull, with respect to a given normal
    * direction, of a set of input points. See {@link #computeHull} for
    * details.
    *
    * @param input points from which to compute the hull
    * @param nrml normal vector for the plane with respect to which
    * the hull should be computed
    * @param angTol if {@code > 0}, specifies an angular tolerance which should
    * be used to remove nearly colinear points
    * @return list of the indices of the point in {@code input} which
    * form the hull, in counter-clockwise order
    */
   public static <P extends Point3d> int[] computeHullIndices (
      List<P> input, Vector3d nrml, double angTol) {

      if (input.size() < 3) {
         throw new IllegalArgumentException (
            "Number of input points must be at least 3");
      }
      RotationMatrix3d RPW = new RotationMatrix3d();
      RPW.setZDirection (nrml);
      ArrayList<Point2d> pnts2d = new ArrayList<>();
      Point3d pp = new Point3d();
      for (P p : input) {
         pp.inverseTransform (RPW, p);
         pnts2d.add (new Point2d (pp.x, pp.y));
      }
      return ConvexPolygon2d.computeHullIndices (pnts2d, angTol);     
   }

   /**
    * Computes the convex hull, with respect to a given normal direction, of a
    * set of input points. The algorithm works by projecting the points into a
    * plane associated with the normal, and then computing the indices of the
    * 2D convex hull in that plane, using {@link
    * ConvexPolygon2d#computeHullIndices}.
    * Point values <i>along</i> the normal are
    * ignored.
    *
    * @param input points from which to compute the hull
    * @param nrml normal vector for the plane with respect to which
    * the hull should be computed
    * @param angTol if {@code > 0}, specifies an angular tolerance which should
    * be used to remove nearly colinear points
    * @return a list of points, copied from {@code input}, which form the hull
    * in counter-clockwise order
    */
   public static <P extends Point3d> ArrayList<P> computeHull (
      List<P> input, Vector3d nrml, double angTol) {

      int[] indices = computeHullIndices (input, nrml, angTol);
      ArrayList<P> hull = new ArrayList<>();
      for (int i : indices) {
         hull.add (input.get(i));
      }
      return hull;
   }

   /**
    * Computes the convex hull, with respect to a given normal direction, of a
    * set of input points, and places its contents in this {@code Polygon3d}.
    * See {@link #computeHull(List,Vector3d,double)} for details.
    *
    * @param input points from which to compute the hull
    * @param nrml normal vector for the plane with respect to which
    * the hull should be computed
    * @param angTol if {@code > 0}, specifies an angular tolerance which should
    * be used to remove nearly colinear points
    */
   public <P extends Point3d> void computeAndSetHull (
      List<P> input, Vector3d nrml, double angTol) {
      set (computeHull (input, nrml, angTol));
   }
}
