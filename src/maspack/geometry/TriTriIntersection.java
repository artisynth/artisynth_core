package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.util.DataBuffer;
import maspack.util.UnitTest;

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

   protected TriTriIntersection() {
   }
   
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
   
   public int numPoints() {
      return points.length;
   }

   public boolean equals (TriTriIntersection isect, StringBuilder msg) {
      if (face0 != isect.face0) {
         if (msg != null) msg.append ("face0 differs\n");
         return false;
      }
      if (face1 != isect.face1) {
         if (msg != null) msg.append ("face1 differs\n");
         return false;
      }
      if (points.length != isect.points.length) {
         if (msg != null) msg.append ("points size differs\n");
         return false;
      }
      for (int i=0; i<points.length; i++) {
         if (!points[i].equals (isect.points[i])) { 
            if (msg != null) msg.append ("points "+i+" differs\n");
            return false;
         }
      }
      return true;         
   }

   protected void getState (DataBuffer data) {
      data.zput (face0.getIndex());
      data.zput (face1.getIndex());
      data.zput (points.length);
      for (int k=0; k<points.length; k++) {
         data.dput (points[k]);
      }
   }

   protected void setState (
      DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      face0 = mesh0.getFace(data.zget());
      face1 = mesh1.getFace(data.zget());
      points = new Point3d[data.zget()];
      for (int k=0; k<points.length; k++) {
         points[k] = new Point3d();
         data.dget (points[k]);
      }
   }

   public static void getState (
      DataBuffer data, ArrayList<TriTriIntersection> isects) {
      
      if (isects == null) {
         data.zput (-1);
         return;
      }
      int size = isects.size();
      data.zput (size);
      for (int i=0; i<size; i++) {
         isects.get(i).getState (data);
      }
   }

   public static ArrayList<TriTriIntersection> setStateArray (
      DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      int size = data.zget();
      if (size == -1) {
         return null;
      }
      ArrayList<TriTriIntersection> isects =
         new ArrayList<TriTriIntersection>(size);
      for (int i=0; i<size; i++) {
         TriTriIntersection isect = new TriTriIntersection();
         isect.setState (data, mesh0, mesh1);
         isects.add (isect);
      }
      return isects;
   }

}
