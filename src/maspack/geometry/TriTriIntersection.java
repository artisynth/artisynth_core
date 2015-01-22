package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;

/**
 * A generic representation of an intersection between two triangles.
 * 
 * @author elliote, antonio
 * 
 */
public class TriTriIntersection {

   public Face face0;
   public Face face1;
   public Point3d[] points;

//   /**
//    * The two faces.
//    */
//   public TriTriIntersection (Face f0, Face f1,
//      ArrayList<Point3d> _points) {
//      face0 = f0;
//      face1 = f1;
//      points = _points.toArray(new Point3d[2]);
//   }
   
   /**
    * The two faces.
    */
   public TriTriIntersection (Face f0, Face f1,
      Point3d[] _points) {
      face0 = f0;
      face1 = f1;
      points = _points;
   }

   public Vector2d[] getFace0Coords() {
      // compute coords
      Vector2d[] faceCoords = new Vector2d[points.length];
      for (int i=0; i<faceCoords.length; i++) {
         faceCoords[i] = new Vector2d();
         face0.computeCoords(points[i], faceCoords[i]);
      }
      return faceCoords;
   }
   
   public Vector2d[] getFace1Coords() {
      // compute coords
      Vector2d[] faceCoords = new Vector2d[points.length];
      for (int i=0; i<faceCoords.length; i++) {
         faceCoords[i] = new Vector2d();
         face1.computeCoords(points[i], faceCoords[i]);
      }
      return faceCoords;
   }

}
