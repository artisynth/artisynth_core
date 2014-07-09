/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Line;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;

public class TriLineIntersection {

   public Face face;
   public Point3d[] points;
   public Vector2d[] faceCoords;
   public Line line;

   public TriLineIntersection (Face face, Line l,
      ArrayList<Point3d> points) {
      this.face = face;
      this.line = l;
      this.points = points.toArray(new Point3d[points.size()]);
      this.faceCoords = null;
   }

   public TriLineIntersection (Face face, Line l,
      ArrayList<Point3d> points, ArrayList<Vector2d> coords) {
      this.face = face;
      this.line = l;
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
