/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

public class Vertex2d { // extends Feature {
   public Vertex2d next;
   public Vertex2d prev;

   public Point2d pnt;

   public Point2d getPosition() {
      return pnt;
   }

   public Vertex2d getNext() {
      return next;
   }

   public Vertex2d getPrev() {
      return prev;
   }

   public Vector2d getEdge() {
      if (next != null) {
         Vector2d edge = new Vector2d();
         edge.sub (next.pnt, pnt);
         return edge;
      }
      else {
         return null;
      }
   }

   public Vertex2d() {
      pnt = new Point2d();
   }

   public Vertex2d (double x, double y) {
      this();
      pnt.set (x, y);
   }

   public Vertex2d (Point2d p) {
      this();
      pnt.set (p);
   }

   public double distance (Vector2d p) {
      return pnt.distance (p);
   }

   public double distance (Vertex2d vtx) {
      return pnt.distance (vtx.pnt);
   }

}
