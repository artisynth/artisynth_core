/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

public class PolygonVertex3d {
   public PolygonVertex3d next;
   public PolygonVertex3d prev;

   public Point3d pnt;

   public PolygonVertex3d() {
      pnt = new Point3d();
   }

   public PolygonVertex3d (double x, double y, double z) {
      this();
      pnt.set (x, y, z);
   }

   public PolygonVertex3d (Point3d p) {
      this();
      pnt.set (p);
   }
}
