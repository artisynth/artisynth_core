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
}
