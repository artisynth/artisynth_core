/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;

public class TriPlaneIntersection {

   public Face face;
   public Point3d[] points;
   public Vector2d[] faceCoords;
   public Plane plane;

   public TriPlaneIntersection (Face face, Plane p,
      ArrayList<Point3d> points) {
      this.face = face;
      this.plane = p;
      this.points = points.toArray(new Point3d[points.size()]);
      this.faceCoords = null;
   }

   public TriPlaneIntersection (Face face, Plane p,
      ArrayList<Point3d> points, ArrayList<Vector2d> coords) {
      this.face = face;
      this.plane = p;
      this.points = points.toArray(new Point3d[points.size()]);
      this.faceCoords = coords.toArray(new Vector2d[coords.size()]);
   }

   public int numPoints() {
      return points.length;
   }

   public Vector2d[] getFaceCoords() {
      if (faceCoords == null) {
         // compute coords
         faceCoords = new Vector2d[points.length];
         for (int i=0; i<faceCoords.length; i++) {
            faceCoords[i] = new Vector2d();
            face.computeCoords(points[i], faceCoords[i]);
         }
      }
      return faceCoords;
   }

}
