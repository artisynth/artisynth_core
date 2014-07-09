/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

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

}
