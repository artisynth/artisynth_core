/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

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
}
