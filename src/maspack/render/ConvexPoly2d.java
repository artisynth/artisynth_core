/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.*;
import maspack.util.*;

public class ConvexPoly2d {

   private static final double DEFAULT_TOL = 1e-12;
   protected double myTol = DEFAULT_TOL;

   public double getTolerance() {
      return myTol;
   }

   public void setTolerance (double tol) {
      myTol = tol;
   }

   public class Vertex2d {
      public Point2d pnt;
      public Vertex2d next;
      public Vertex2d prev;

      Vertex2d (Point2d p) {
         pnt = new Point2d(p);
      }

      Vertex2d (double x, double y) {
         pnt = new Point2d(x, y);
      }

      boolean isOutsideHalfPlane (double nx, double ny, double d) {
         return pnt.x*nx + pnt.y*ny - d < 0;
      }
   }

   Vertex2d head;

   public ConvexPoly2d() {
      head = null;
   }

   public ConvexPoly2d (ConvexPoly2d poly) {
      if (poly.head != null) {
         Vertex2d v = poly.head;
         do {
            addVertex (v.pnt);
            v = v.next;
         }
         while (v != poly.head);
      }
   }

   public ConvexPoly2d(Double... coords) {
      int numv = coords.length/2;
      for (int i=0; i<numv; i++) {
         addVertex (coords[i*2], coords[i*2+1]);
      }
   }

   public void clear() {
      head = null;
   }

   public Vertex2d addVertex (double x, double y) {
      Vertex2d v = new Vertex2d (x, y);
      addVertex (v);
      return v;
   }

   public Vertex2d addVertex (Point2d p) {
      Vertex2d v = new Vertex2d (p);
      addVertex (v);
      return v;
   }

   public Vertex2d firstVertex() {
      return head;
   }

   private void addVertex (Vertex2d v) {
      if (head == null) {
         v.next = v;
         v.prev = v;
         head = v;
      }
      else {
         v.next = head;
         v.prev = head.prev;
         head.prev.next = v;
         head.prev = v;
      }
   }

   private void addVertexAfter (Vertex2d vprev, Vertex2d vnew) {
      vnew.prev = vprev;
      vnew.next = vprev.next;
      vprev.next.prev = vnew;
      vprev.next = vnew;
   }

   public void removeVertex (Vertex2d v) {
      if (v.next == v) {
         head = null;
      }
      else {
         v.prev.next = v.next;
         v.next.prev = v.prev;
         if (v == head) {
            head = v.next;
         }
      }
   }

   public boolean isEmpty() {
      return head == null;
   }

   public int numVertices() {
      int numv = 0;
      if (head != null) {
         Vertex2d v = head;
         do {
            numv++;
            v = v.next;
         }
         while (v != head);
      }
      return numv;
   }

   private double intersectLineSegments (
      Point2d pr, Point2d p1, Point2d p2, double nx, double ny, double d) {

      double ux = p2.x - p1.x;
      double uy = p2.y - p1.y;
      double denom = nx*ux + ny*uy;
      double lam;
      if (Math.abs (denom) < myTol) {
         // assume line segments are parallel
         lam = 0;
      }
      else {
         lam = (d - nx*p1.x - ny*p1.y)/denom;
         if (lam < 0) {
            lam = 0;
         }
         else if (lam > 1) {
            lam = 1;
         }
      }
      pr.x = ux*lam + p1.x;
      pr.y = uy*lam + p1.y;
      return lam;
   }

   /**
    * Intersects this convex polygon with a half plane defined by
    * {@code n^T x - d >= 0}.
    */
   public void intersectHalfPlane (double nx, double ny, double d) {
      // if plane is empty do nothing
      if (head == null) {
         return;
      }
      Vertex2d v = head;
      Vertex2d firstOut = null;
      Vertex2d lastOut = null;
      // see if any vertices lie outside the half plane
      if (v.isOutsideHalfPlane(nx, ny, d)) {
         do {
            v = v.prev;
            boolean outside = v.isOutsideHalfPlane(nx, ny, d);
            if (!outside && firstOut == null) {
               firstOut = v.next;
            }
            else if (outside && firstOut != null) {
               lastOut = v;
               break;
            }
         }
         while (v != head);
         if (firstOut == null) {
            // all vertices are outside
            clear();
            return;
         }
      }
      else {
         do {
            v = v.next;
            boolean outside = v.isOutsideHalfPlane(nx, ny, d);
            if (outside && firstOut == null) {
               firstOut = v;
            }
            else if (!outside && firstOut != null) {
               lastOut = v.prev;
               break;
            }
         }
         while (v != head);
         if (firstOut == null) {
            // all vertices are inside
            return;
         }
      }

      boolean singleOut = false;
      if (firstOut == lastOut) {
         // add an additional vertex 
         lastOut = new Vertex2d (firstOut.pnt);
         addVertexAfter (firstOut, lastOut);
         singleOut = true;
      }
      else if (firstOut.next != lastOut) {
         // remove vertices between first and last out
         for (v = firstOut.next; v != lastOut; v = v.next) {
            if (v == head) {
               head = lastOut;
               break;
            }
         }
         firstOut.next = lastOut;
         lastOut.prev = firstOut;
      }
      double lam1 = intersectLineSegments (
         firstOut.pnt, firstOut.pnt, firstOut.prev.pnt, nx, ny, d);
      if (1-lam1 < myTol) {
         removeVertex (firstOut);
      }
      double lam2 = intersectLineSegments (
         lastOut.pnt, lastOut.pnt, lastOut.next.pnt, nx, ny, d);
      if (1-lam2 < myTol) {
         removeVertex (lastOut);
      }
      if (singleOut && lam1 < myTol && lam2 < myTol) {
         removeVertex (lastOut);
      }
   }

   public boolean epsilonEquals (ConvexPoly2d poly, double eps) {
      if (numVertices() != poly.numVertices()) {
         return false;
      }
      if (head != null) {
         Vertex2d v1 = head;
         Vertex2d start1 = null;
         do {
            if (v1.pnt.epsilonEquals (poly.head.pnt, eps)) {
               start1 = v1;
               break;
            }
            v1 = v1.next;
         }
         while (v1 != head);
         if (start1 == null) {
            return false;
         }
         v1 = start1;
         Vertex2d v2 = poly.head;
         while (v1.next != start1) {
            v1 = v1.next;
            v2 = v2.next;
            if (!v1.pnt.epsilonEquals (v2.pnt, eps)) {
               return false;
            }
         }
      }
      return true;
   }

   public void transform (AffineTransform2dBase T) {
      if (head != null) {
         Vertex2d v = head;
         do {
            v.pnt.transform (T);
            v = v.next;
         }
         while (v != head);
      }
   }

   boolean checkConsistency() {
      if (head != null) {
         Vertex2d v = head;
         do {
            if (v.next == null || v.next.prev != v ||
                v.prev == null || v.prev.next != v) {
               return false;
            }
            v = v.next;
         }
         while (v != head);
      }
      return true;
   }

   double computeArea (Point2d p1, Point2d p2, Point2d p3) {
      double d2x = p2.x - p1.x;
      double d2y = p2.y - p1.y;
      double d3x = p3.x - p1.x;
      double d3y = p3.y - p1.y;
      return (d2x*d3y - d2y*d3x)/2;
   }

   public void computeCentroid (Vector2d cent) {
      int numv = numVertices();
      switch (numv) {
         case 0: {
            cent.setZero();
            break;
         }
         case 1: {
            cent.set (head.pnt);
            break;
         }
         case 2: {
            cent.add (head.pnt, head.next.pnt);
            cent.scale (0.5);
            break;
         }
         default: {
            double totalArea = 0;
            Vector2d c = new Vector2d();
            Vertex2d v1 = head;
            Vertex2d v2 = v1.next;
            Vertex2d v3 = v2.next;
            cent.setZero();
            do {
               double a = computeArea (v1.pnt, v2.pnt, v3.pnt);
               c.add (v1.pnt, v2.pnt);
               c.add (v3.pnt);
               c.scale (1/3.0);
               cent.scaledAdd (a, c);
               v2 = v3;
               v3 = v3.next;
               totalArea += a;
            }
            while (v3 != head);
            cent.scale (1/totalArea);
         }
      }
   }

   public String toString (String fmtStr) {
      StringBuilder strb = new StringBuilder();
      if (head != null) {
         Vertex2d v = head;
         NumberFormat fmt = new NumberFormat (fmtStr);
         do {
            if (v != head) {
               strb.append (", ");
            }
            strb.append (fmt.format (v.pnt.x));
            strb.append (" ");
            strb.append (fmt.format (v.pnt.y));
            v = v.next;
         }
         while (v != head);
      }
      return strb.toString();
   }
}
