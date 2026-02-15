package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.util.DataBuffer;

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
   double p0s, p0t; // barycentric coordinates for points[0] wrt face0
   double p1s, p1t; // barycentric coordinates for points[1] wrt face0

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
    * Initialize with faces and intersection points.
    */
   public TriTriIntersection (
      Face f0, Face f1, Point3d[] _points) {
      face0 = f0;
      face1 = f1;
      points = _points;
   }

   /**
    * Initialize with faces, intersection points, and point coordinates.
    */
   public TriTriIntersection (
      Face f0, Face f1, Point3d[] _points, double[] coords) {
      face0 = f0;
      face1 = f1;
      points = _points;
      p0s = coords[0];
      p0t = coords[1];
      p1s = coords[2];
      p1t = coords[3];     
   }

   /**
    * Returns the stored barycentric coordinates of the {@code idx}-th point.
    * It is assumed that {@code points} has been initialized; otherwise, {@code
    * null} is returned.
    * 
    * @return barcentric coordinates of point {@code idx}
    */
   public Vector2d getPointCoords (int idx) {
      if (points == null) {
         return null;
      }
      double s, t;
      if (idx == 0) {
         return new Vector2d (p0s, p0t);
      }
      else if (idx == 1) {
         return new Vector2d (p1s, p1t);
      }
      else {
         throw new IllegalArgumentException (
            "point idx must be either 0 or 1");
      }
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
   
   /**
    * Computes the current position of the {@code idx}-th point, in world
    * coordinates, using its barycentric coordinates with respect to
    * face0. This accounts for any possible displacement of the mesh vertices
    * since the intersection point was first computed.  It is assumed that
    * {@code points} has been initialized; otherwise, {@code null} is returned.
    * 
    * @return current position of point {@code idx}
    */
   public Point3d getCurrentPosition (int idx) {
      if (points == null) {
         return null;
      }
      double s, t;
      if (idx == 0) {
         s = p0s;
         t = p0t;
      }
      else if (idx == 1) {
         s = p1s;
         t = p1t;
      }
      else {
         throw new IllegalArgumentException (
            "point idx must be either 0 or 1");
      }

      HalfEdge he = face0.firstHalfEdge();
      Point3d p0 = he.getHead().getPosition();
      he = he.getNext();
      Point3d p1 = he.getHead().getPosition();
      he = he.getNext();
      Point3d p2 = he.getHead().getPosition();
      Point3d pos = new Point3d();
      pos.combine (s, p1, t, p2);
      pos.scaledAdd (1-s-t, p0);
      PolygonalMesh mesh = face0.getMesh();
      if (mesh != null && !mesh.meshToWorldIsIdentity()) {
         pos.transform (mesh.getMeshToWorld());
      }
      return pos;
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
