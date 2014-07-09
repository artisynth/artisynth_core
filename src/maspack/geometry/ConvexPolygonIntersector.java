/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

import java.util.Vector;
import java.util.Iterator;

public class ConvexPolygonIntersector {
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private double tol;
   private double distanceTol = AUTOMATIC;

   private static final int UNKNOWN = 0;
   private static final int POLY_P = 1;
   private static final int POLY_Q = 2;

   private int pcnt;
   private int qcnt;

   private double ah1;
   private double at1;
   private double ah2;
   private double at2;

   private Point2d ipnt;
   private Vector2d vtmp;

   private double lamp;
   private double lamq;

   private boolean colinear;
   private int lastAdvance;
   private boolean polygonArgumentsSwitched;

   private Vertex2d p;
   private Vertex2d q;

   private ConvexPolygon2d resultPolygon;
   private int inflag;

   private Vector listeners = new Vector (1);

   public interface Listener {
      public void output (
         Point2d pnt, Vertex2d vp, Vertex2d vq, double lamp, double lamq);
   }

   public void addListener (Listener l) {
      listeners.add (l);
   }

   public void removeListener (Listener l) {
      listeners.remove (l);
   }

   private void fireListeners (
      Point2d pnt, Vertex2d vp, Vertex2d vq, double lamp, double lamq) {
      if (!listeners.isEmpty()) {
         for (Iterator it = listeners.iterator(); it.hasNext();) {
            ((Listener)it.next()).output (pnt, vp, vq, lamp, lamq);
         }
      }
   }

   private void fireListeners (Polygon2d poly, int polyID) {
      if (!listeners.isEmpty()) {
         for (Iterator it = listeners.iterator(); it.hasNext();) {
            Listener l = (Listener)it.next();
            Vertex2d vtx = poly.firstVertex;
            if (vtx != null) {
               do {
                  if (polyID == POLY_P) {
                     l.output (vtx.pnt, vtx, null, 1, -1);
                  }
                  else {
                     l.output (vtx.pnt, null, vtx, -1, 1);
                  }
                  vtx = vtx.next;
               }
               while (vtx != poly.firstVertex);
            }
         }
      }
   }

   /**
    * Specfies that distance tolerances should be computed automatically.
    */
   public static final double AUTOMATIC = -1;

   /**
    * Sets the distance tolerance. This should be the smallest distance value
    * that can be reliably computed. If tol is set to {@link #AUTOMATIC
    * AUTOMATIC}, then the distance tolerance is computed automatically from the
    * data.
    * 
    * @param tol
    * distance tolerance
    */
   public void setDistanceTolerance (double tol) {
      distanceTol = tol;
   }

   /**
    * Gets the distance tolerance.
    * 
    * @return distance tolerance
    * @see #setDistanceTolerance
    */
   public double getDistanceTolerance() {
      return distanceTol;
   }

   public ConvexPolygonIntersector() {
      ipnt = new Point2d();
      vtmp = new Vector2d();
   }

   private double withinTol (double x) {
      if (x <= tol && x >= -tol) {
         return (0);
      }
      else {
         return (x);
      }
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
    * Computes (b-a) . (c-a)
    */
   private double dotSegs (Point2d a, Point2d b, Point2d c) {
      double dbx = b.x - a.x;
      double dby = b.y - a.y;
      double dcx = c.x - a.x;
      double dcy = c.y - a.y;

      return dbx * dcx + dby * dcy;
   }

   private int outputIntersectionPoint (
      Point2d pnt, Vertex2d p, Vertex2d q, double lamp, double lamq) {
      Vertex2d firstVtx = resultPolygon.firstVertex;
      if (firstVtx != null) {
         Vertex2d lastVtx = firstVtx.prev;
         if (lastVtx.pnt.epsilonEquals (pnt, tol) ||
             firstVtx.pnt.epsilonEquals (pnt, tol)) {
            return 0;
         }
      }
      resultPolygon.appendVertex (new Vertex2d (pnt));
      if (polygonArgumentsSwitched) {
         fireListeners (pnt, q, p, lamq, lamp);
      }
      else {
         fireListeners (pnt, p, q, lamp, lamq);
      }
      return 1;
   }

   private void outputCompletePolygon (ConvexPolygon2d poly, int polyID) {
      if (polygonArgumentsSwitched) {
         fireListeners (poly, polyID == POLY_P ? POLY_Q : POLY_P);
      }
      else {
         fireListeners (poly, polyID);
      }
   }

   private void advanceP (String msg) {
      if (inflag == POLY_P) {
         outputIntersectionPoint (p.pnt, p, null, 1, -1);
      }
      p = p.next;

      at1 = ah1;
      ah1 = withinTol (area (q.prev.pnt, q.pnt, p.pnt));
      ah2 = withinTol (area (p.prev.pnt, p.pnt, q.pnt));
      at2 = withinTol (area (p.prev.pnt, p.pnt, q.prev.pnt));

      pcnt++;
      lastAdvance = POLY_P;
   }

   private void advanceQ (String msg) {
      if (inflag == POLY_Q) {
         outputIntersectionPoint (q.pnt, null, q, -1, 1);
      }
      q = q.next;

      at2 = ah2;
      ah2 = withinTol (area (p.prev.pnt, p.pnt, q.pnt));
      ah1 = withinTol (area (q.prev.pnt, q.pnt, p.pnt));
      at1 = withinTol (area (q.prev.pnt, q.pnt, p.prev.pnt));

      qcnt++;
      lastAdvance = POLY_Q;
   }

   private boolean isectLineSegs (Point2d pnt) {
      double ah2at2;
      double ah1at1;

      colinear = false;

      lamp = -1;
      lamq = -1;

      if ((ah2at2 = ah2 * at2) > 0 || (ah1at1 = ah1 * at1) > 0) {
         return (false);
      }
      else if (ah2at2 < 0 && ah1at1 < 0) {
         lamp = at1 / (at1 - ah1);
         lamq = at2 / (at2 - ah2);
         // pnt.interpolate (p.pnt, -ah1/(at1-ah1), p.prev.pnt);
         pnt.interpolate (p.prev.pnt, lamp, p.pnt);
         inflag = ah1 > 0 ? POLY_P : POLY_Q;
         return (true);
      }
      else if ((ah1 == 0 && at1 == 0) || (ah2 == 0 && at2 == 0)) {
         colinear = true;
         return (false);
      }
      else if (ah2at2 < 0) { // pnt.set (ah1 == 0 ? p.pnt : p.prev.pnt);
         lamp = (ah1 == 0 ? 1 : 0);
         lamq = at2 / (at2 - ah2);
      }
      else if (ah1at1 < 0) { // pnt.set (ah2 == 0 ? q.pnt : q.prev.pnt);
         lamq = (ah2 == 0 ? 1 : 0);
         lamp = at1 / (at1 - ah1);
      }
      else if (at1 == 0) {
         if (at2 == 0) {
            // used to output q.prev if (p0-q0).(q1-q0) >= 0, else p.prev
            lamp = 0;
            lamq = 0;
         }
         else // ah2 == 0
         { // used to output q if -(p0-q1).(q0-q1) >= 0, else p.prev
            lamp = 0;
            lamq = 1.0;
         }
      }
      else if (ah1 == 0) {
         if (at2 == 0) {
            // used to output q.prev if (p1-q0).(q1-q0) >= 0, else p
            lamp = 1.0;
            lamq = 0;
         }
         else // ah2 == 0
         { // used to output q if -(p1-q1).(q0-q1) >=, else p
            lamp = 1.0;
            lamq = 1.0;
         }
      }
      else {
         throw new InternalErrorException (
            "ConvexPolygon.isectLineSegs: help! we forgot a case!\n");
      }

      if (lamp == 0) {
         pnt.set (p.prev.pnt);
      }
      else if (lamp == 1) {
         pnt.set (p.pnt);
      }
      else if (lamq == 0) {
         pnt.set (q.prev.pnt);
      }
      else // lamq == 1
      {
         pnt.set (q.pnt);
      }

      if (ah1 > 0) {
         if (ah2at2 < 0) {
            inflag = POLY_P;
         }
         else if (at2 == 0) { // (q0-q*) X (p1-q0)
            if (area (q.prev.prev.pnt, q.prev.pnt, p.pnt) > tol) {
               inflag = POLY_P;
            }
         }
      }
      else if (ah2 > 0) {
         if (ah1at1 < 0) {
            inflag = POLY_Q;
         }
         else if (at1 == 0) { // (p0-p*) X (q1-p0)
            if (area (p.prev.prev.pnt, p.prev.pnt, q.pnt) > tol) {
               inflag = POLY_Q;
            }
         }
      }
      return true;
   }

   public ConvexPolygon2d intersect (
      ConvexPolygon2d polyP, ConvexPolygon2d polyQ) {
      int numVertsP = polyP.numVertices();
      int numVertsQ = polyQ.numVertices();

      polygonArgumentsSwitched = false;

      if (numVertsP < 2) {
         throw new IllegalArgumentException (
            "Polygon polyP has less than 2 vertices");
      }
      if (numVertsQ < 2) {
         throw new IllegalArgumentException (
            "Polygon polyQ has less than 2 vertices");
      }

      if (numVertsQ == 2) {
         return intersect2VertexPolygon (polyP, polyQ);
      }
      else if (numVertsP == 2) {
         polygonArgumentsSwitched = true;
         return intersect2VertexPolygon (polyQ, polyP);
      }

      resultPolygon = new ConvexPolygon2d();

      if (distanceTol == AUTOMATIC) { // XXX maybe think about this some more

         tol =
            100
            * Math.max (polyP.getMaxCoordinate(), polyQ.getMaxCoordinate())
            * DOUBLE_PREC;
      }
      else {
         tol = distanceTol;
      }

      inflag = UNKNOWN;
      pcnt = 0;
      qcnt = 0;
      colinear = false;

      p = polyP.firstVertex;
      q = polyQ.firstVertex;

      ah2 = withinTol (area (p.prev.pnt, p.pnt, q.pnt));
      at2 = withinTol (area (p.prev.pnt, p.pnt, q.prev.pnt));
      ah1 = withinTol (area (q.prev.pnt, q.pnt, p.pnt));
      at1 = withinTol (area (q.prev.pnt, q.pnt, p.prev.pnt));
      lastAdvance = UNKNOWN;

      do {
         double cross = ah2 - at2;

         /* Check for intersection of both segments */

         if (isectLineSegs (ipnt)) {
            Vertex2d firstVtx = resultPolygon.getFirstVertex();
            if (firstVtx == null) { // then result polygon is empty
               pcnt = qcnt = 0;
            }
            else if (firstVtx.next != firstVtx) {
               // then result has more than 1 vertex
               if (ipnt.epsilonEquals (firstVtx.pnt, tol)) {
                  return resultPolygon;
               }
            }
            outputIntersectionPoint (ipnt, p, q, lamp, lamq);
         }

         if (colinear) { // (p1-p0) . (q1-q0)
            vtmp.sub (p.pnt, p.prev.pnt);
            if (vtmp.dot (q.pnt) - vtmp.dot (q.prev.pnt) < 0) // well defined
            {
               if (lastAdvance == POLY_P) {
                  advanceQ ("A");
               }
               else {
                  advanceP ("B");
               }
            }
            else { // (p1-p0) . (q1-p1) = -(p0-p1) . (q1-p1)
               if (-dotSegs (p.pnt, p.prev.pnt, q.pnt) >= 0) {
                  advanceP ("AA");
               }
               else {
                  advanceQ ("BB");
               }
            }
         }

         // These next two rules say: If the head of X lies on the line
         // of Y, is behind Y, and X is pointing away from Y, advance Y

         else if (ah1 == 0 && cross * at2 > 0) {
            advanceQ ("JJ");
         }
         else if (ah2 == 0 && cross * at1 < 0) {
            advanceP ("KK");
         }
         else if (cross >= 0) {
            if (ah2 > 0) {
               advanceP ("C");
            }
            else {
               advanceQ ("D");
            }
         }
         else {
            if (ah1 > 0) {
               advanceQ ("E");
            }
            else {
               advanceP ("F");
            }
         }
      }
      while ((pcnt < numVertsP || qcnt < numVertsQ) &&
             (pcnt < 2 * numVertsP && qcnt < 2 * numVertsQ));

      if (inflag == UNKNOWN) {
         // then we know that either (1) there is no intersection, (2)
         // poly1 is contained in poly2, or (3) poly2 is contained in
         // poly1.
         Point2d centroid = new Point2d();
         boolean insideTheOther = false;

         polyP.getCentroid (centroid);
         if (polyQ.pointIsInside (centroid) == 1) {
            insideTheOther = true;
         }
         else {
            polyQ.getCentroid (centroid);
            if (polyP.pointIsInside (centroid) == 1) {
               insideTheOther = true;
            }
         }
         if (insideTheOther) {
            if (polyP.area() > polyQ.area()) {
               if (resultPolygon.numVertices() == 0) {
                  outputCompletePolygon (polyQ, POLY_Q);
               }
               return polyQ;
            }
            else {
               if (resultPolygon.numVertices() == 0) {
                  outputCompletePolygon (polyP, POLY_P);
               }
               return polyP;
            }
         }
      }
      return resultPolygon;
   }

   // public ConvexPolygon2d intersectHalfPlane (
   // ConvexPolygon2d poly, Point2d q0, Vector2d u)
   // {
   // resultPolygon = new ConvexPolygon2d();
   // Edge e1, e2;
   // Edge firstNotIn = null;
   // Edge lastNotIn = null;
   // boolean insideVtxExists = false;
   // double side1, side2;
   // Point2d qnew = new Point2d();

   // if (polyP.isEmpty())
   // { return (resultPolygon);
   // }

   // Point2d q1 = new Point2d();
   // q1.add (q0, u);
   // Vertex2d p = poly.getFirstVertex();

   // if (distanceTol = AUTOMATIC)
   // { tol = 2*poly.getMaxCoordinate()*DOUBLE_PREC;
   // }
   // else
   // { tol = distanceTol;
   // }

   // side1 = withinTol (q0, q1, p.pnt);
   // do
   // { p = p.next;

   // if (side1 > 0)
   // { insideVtxExists = true;
   // }

   // side2 = withinTol (q0, q1, p.pnt);
   // if (side1 > 0 && side2 <= 0)
   // { if (side2 < 0)
   // { e2.intersect (qnew, hp, eps);
   // firstNotIn = new Edge();
   // firstNotIn.vtx.set (qnew);
   // firstNotIn.u.sub (qnew, e1.vtx);
   // addEdgeAfter (e1, firstNotIn);
   // numv++;
   // }
   // else
   // { firstNotIn = e2;
   // }
   // }
   // else if (side1 != 1 && side2 == 1)
   // { if (side1 == -1)
   // { e2.intersect (qnew, hp, eps);
   // lastNotIn = new Edge();
   // e2.u.sub (e2.vtx, qnew);
   // lastNotIn.vtx.set (qnew);
   // addEdgeAfter (e1, lastNotIn);
   // numv++;
   // }
   // else
   // { lastNotIn = e1;
   // }
   // }
   // e1 = e2;
   // side1 = side2;
   // }
   // while (e1!=elistHead && (firstNotIn==null||lastNotIn==null));

   // if (firstNotIn == null && lastNotIn == null)
   // { if (!insideVtxExists)
   // { clear();
   // }
   // }
   // else if (firstNotIn != null && lastNotIn != null)
   // { if (firstNotIn != lastNotIn)
   // { numv -= removeEdgesBetween (firstNotIn, lastNotIn);
   // lastNotIn.u.sub (lastNotIn.vtx, firstNotIn.vtx);
   // }
   // }
   // else
   // { System.err.println (
   // "Internal error: ConvexPolygon.intersect(Line2d):");
   // System.err.println ("firstNotIn=" + firstNotIn +
   // "lastNotIn=" + lastNotIn);
   // System.exit(1);
   // }
   // return (this);
   // }

   // This is used by intersect2VertexPolygon
   Vertex2d[] pverts = new Vertex2d[2];
   double[] plams = new double[2];

   private void updateLam (double[] lam, double l, Vertex2d p, double plam) {
      if (l < lam[0]) {
         lam[0] = l;
         pverts[0] = p; // used by intersect2VertexPolygon
         plams[0] = plam;
      }
      if (l > lam[1]) {
         lam[1] = l;
         pverts[1] = p; // used by intersect2VertexPolygon
         plams[1] = plam;
      }
   }

   public int intersectLine (
      double[] lam, ConvexPolygon2d poly, Point2d q0, Vector2d u) {
      double at, ah;

      Vertex2d p = poly.firstVertex;

      lam[0] = Double.POSITIVE_INFINITY;
      lam[1] = -Double.POSITIVE_INFINITY;

      if (p == null) { // empty polygon, so no intersection
         return 0;
      }
      else {
         if (distanceTol == AUTOMATIC) {
            tol = 100 * poly.getMaxCoordinate() * DOUBLE_PREC;
         }
         else {
            tol = distanceTol;
         }

         Point2d q1 = new Point2d();
         q1.add (q0, u);

         at = withinTol (area (q0, q1, p.pnt));
         do {
            ah = withinTol (area (q0, q1, p.next.pnt));
            if (ah == 0 || at == 0) {
               if (ah == 0) {
                  vtmp.sub (p.next.pnt, q0);
                  updateLam (lam, vtmp.dot (u) / u.normSquared(), p.next, 1);
               }
               if (at == 0) {
                  vtmp.sub (p.pnt, q0);
                  updateLam (lam, vtmp.dot (u) / u.normSquared(), p.next, 0);
               }
            }
            else {
               if (ah * at < 0) {
                  ipnt.interpolate (p.next.pnt, ah / (ah - at), p.pnt);
                  vtmp.sub (ipnt, q0);
                  updateLam (lam, vtmp.dot (u) / u.normSquared(), p.next, at
                  / (at - ah));
               }
               else if (lam[0] != Double.POSITIVE_INFINITY && lam[0] != lam[1]) {
                  // short-cut: we have found a finite interval, and we didn't
                  // add to it this time, so was are done.
                  break;
               }
            }
            p = p.next;
            at = ah;
         }
         while (p != poly.firstVertex);
      }
      if (lam[0] == Double.POSITIVE_INFINITY) {
         double tmp = lam[0];
         lam[0] = lam[1];
         lam[1] = tmp;
         return (0);
      }
      else if (lam[0] != lam[1]) {
         return (2);
      }
      else {
         return (1);
      }
   }

   /**
    * Handles special case when polyQ has less than three vertices.
    */
   private ConvexPolygon2d intersect2VertexPolygon (
      ConvexPolygon2d polyP, ConvexPolygon2d polyQ) {
      resultPolygon = new ConvexPolygon2d();

      int numVertsQ = polyQ.numVertices();

      if (numVertsQ != 2) { // handle this case later
         throw new InternalErrorException ("polyP does not have two vertices");
      }

      Vertex2d q = polyQ.firstVertex;

      double[] lam = new double[2];
      Vector2d u = new Vector2d();
      u.sub (q.next.pnt, q.pnt);

      int n;

      // tol with be updated in this routine
      n = intersectLine (lam, polyP, q.pnt, u);

      if (n == 1) {
         if (lam[0] < -tol || lam[0] > 1 + tol) {
            n = 0;
         }
         lam[0] = Math.max (0.0, Math.min (1.0, lam[0]));
      }
      else if (n == 2) {
         if (lam[0] > 1 + tol || lam[1] < -tol) {
            n = 0;
         }
         else {
            lam[0] = Math.max (0.0, Math.min (1.0, lam[0]));
            lam[1] = Math.max (0.0, Math.min (1.0, lam[1]));
            if (Math.abs (lam[0] - lam[1]) <= 1.0 * tol) {
               n = 1;
            }
         }
      }

      if (n == 2) {
         if (lam[0] == 0 && lam[1] == 1) { // EDGE_WITHIN;
            outputCompletePolygon (polyQ, POLY_Q);
            return polyQ;
         }
         else if (lam[0] > 0 && lam[1] < 1 && polyP.numVertices() == 2) {
            outputCompletePolygon (polyP, POLY_P);
            return polyP;
         }
      }

      for (int i = 0; i < n; i++) {
         if (lam[i] == 0) {
            outputIntersectionPoint (q.pnt, null, q, -1, 1);
         }
         else if (lam[i] == 1) {
            outputIntersectionPoint (q.next.pnt, null, q.next, -1, 1);
         }
         else {
            ipnt.interpolate (q.pnt, lam[i], q.next.pnt);
            outputIntersectionPoint (ipnt, pverts[i], q.next, plams[i], lam[i]);
         }
      }
      return resultPolygon;
   }
}
