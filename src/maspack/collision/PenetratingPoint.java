package maspack.collision;

import java.util.Comparator;
import java.util.ArrayList;

import maspack.geometry.Face;
import maspack.geometry.Vertex3d;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.DataBuffer;

public class PenetratingPoint implements Comparable<PenetratingPoint> {
   // the vertex projected
   public Vertex3d vertex;

   // target position = (1-coords.x-coords.y) * face.v0 + coords.x * face.v1 +
   // coords.y * face.v2
   public Face face;
   public Vector2d coords;// = new Vector2d();
   public Point3d position; // point on the face that is closest to vertex, 
                            // in world coordinates.

   public Vector3d normal; // normal from interpenetrating point to surface
   public double distance; // distance from interpenetrating point to surface,
                           // in world coordinates
   public PenetrationRegion region; // region associated with vertex, 
                           // if available

   public Face getFace() {
      return face;
   }

   public Vector3d getNormal() {
      return normal;
   }

   public Point3d getPosition() {
      return position;
   }
   
   PenetratingPoint () {
       position = new Point3d();
       normal = new Vector3d();
   }

   public PenetratingPoint (
      Vertex3d aVertex, Face opposingFace,
      Vector2d pointBarycentricCoords, Point3d nearestFacePoint,
      Vector3d dispToNearestFace,
      PenetrationRegion r) {
      this();
      vertex = aVertex;
      face = opposingFace;
      coords = new Vector2d (pointBarycentricCoords);
      position.set (nearestFacePoint);
      distance = dispToNearestFace.norm();
      normal.set (dispToNearestFace);
      if (distance != 0) {
         normal.normalize();
      }
      region = r;
   }

   public PenetratingPoint (
      Vertex3d vertex, Point3d vpos, Vector3d normal, double distToSurface) {
      this();
      this.vertex = vertex;
      face = null;
      coords = null;
      this.normal.set (normal);
      distance = distToSurface;
      position.scaledAdd (distToSurface, normal, vpos);
   }
   
   /**
    * Returns the average area associated with this contact, or -1
    * if this information is not available.
    * 
    * @return average contact area, or -1
    */
   public double getContactArea() {
      if (region != null) {
         return region.getArea()/region.numVertices();
      }
      else {
         return -1;
      }
   }
   
   @Override
   public int compareTo(PenetratingPoint o) {
      if (this.distance < o.distance) {
         return -1;
      } else if (this.distance == o.distance) {
         return 0;
      }
      return 1;
   }
   
   public static class DistanceComparator implements Comparator<PenetratingPoint> {

      private int one = 1;
      
      public DistanceComparator () {
      }
      
      public DistanceComparator(boolean reverse) {
         setReverse(reverse);
      }
      
      public void setReverse(boolean set) {
         if (set) {
            one = -1;
         } else {
            one = 1;
         }
      }
      
      @Override
      public int compare(PenetratingPoint o1, PenetratingPoint o2) {
         if (o1.distance > o2.distance) {
            return one;
         } else if (o1.distance < o2.distance) {
            return -one;
         } else {
            return 0;
         }
      }
   }

   public static class IndexComparator
      implements Comparator<PenetratingPoint> {

      @Override
      public int compare (
         PenetratingPoint p0, PenetratingPoint p1) {

         int idx0 = p0.vertex.getIndex();
         int idx1 = p1.vertex.getIndex();
         if (idx0 < idx1) {
            return -1;
         }
         else if (idx0 == idx1) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }  
   
   public static DistanceComparator createMaxDistanceComparator() {
      return new DistanceComparator(true);
   }
   
   public static DistanceComparator createMinDistanceComparator() {
      return new DistanceComparator();
   }

   boolean equals (PenetratingPoint p, StringBuilder msg) {
      if (vertex != p.vertex) {
         if (msg != null) msg.append ("vertex differs\n");
         return false;
      }
      if (!position.equals(p.position)) {
         if (msg != null) msg.append ("position differs\n");
         return false;
      }
      if (!normal.equals(p.normal)) {
         if (msg != null) msg.append ("normal differs\n");
         return false;
      }
      if (distance != p.distance) {
         if (msg != null) msg.append ("distance differs\n");
         return false;
      }
      if (face != p.face) {
         if (msg != null) msg.append ("face differs\n");
         return false;
      }
      if (face != null) {
         if (!coords.equals (p.coords)) {
            if (msg != null) msg.append ("face differs\n");
            return false;
         }
      }
      return true;
   }

   static boolean equals (
      ArrayList<PenetratingPoint> points0, 
      ArrayList<PenetratingPoint> points1,
      StringBuilder msg) {

      if ((points0 != null) != (points1 != null)) {
         if (msg != null) msg.append ("points != null differs\n");
         return false;
      }
      if (points0 != null) {
         if (points0.size() != points1.size()) {
            if (msg != null) msg.append ("points size differs\n");
            return false;
         }
         for (int i=0; i<points0.size(); i++) {
            if (!points0.get(i).equals (points1.get(i), msg)) {
               if (msg != null) msg.append ("points "+i+" differs\n");
               return false;
            }
         }
      }
      return true;
   }

   static DataBuffer.Offsets getStateSize (boolean hasFaces) {
      if (hasFaces) {
         return new DataBuffer.Offsets (2, 9, 0);
      }
      else {
         return new DataBuffer.Offsets (1, 7, 0);
      }
   }

   void getState (DataBuffer data, boolean hasFaces) {
      data.zput (vertex.getIndex());
      data.dput (position);
      data.dput (normal);
      data.dput (distance);
      if (hasFaces) {
         data.zput (face.getIndex());
         data.dput (coords.x);
         data.dput (coords.y);
      }
   }

   void setState (
      DataBuffer data, boolean hasFaces,
      PolygonalMesh meshv, PolygonalMesh meshf) {
      
      vertex = meshv.getVertex (data.zget());
      data.dget (position);
      data.dget (normal);
      distance = data.dget();
      if (hasFaces) {
         face = meshf.getFace (data.zget());
         coords = new Vector2d ();
         coords.x = data.dget();
         coords.y = data.dget();
      }
   }

   static void getState (
      DataBuffer data, ArrayList<PenetratingPoint> points) {
      
      if (points == null) {
         data.zput (-1);
         return;
      }
      int size = points.size();
      data.zput (size);
      // no faces if points were created using SignedDistanceGrid. Assume the
      // first point indicates whether all point have faces or not.
      boolean hasFaces = (size > 0 && points.get(0).face != null);
      data.zputBool (hasFaces);
      for (int i=0; i<size; i++) {
         points.get(i).getState (data, hasFaces);
      }
   }

   static ArrayList<PenetratingPoint> setState (
      DataBuffer data, PolygonalMesh meshv, PolygonalMesh meshf) {
      
      int size = data.zget();
      if (size == -1) {
         return null;
      }
      boolean hasFaces = data.zgetBool();
      ArrayList<PenetratingPoint> points = new ArrayList<PenetratingPoint>(size);
      for (int i=0; i<size; i++) {
         PenetratingPoint p = new PenetratingPoint();
         p.setState (data, hasFaces, meshv, meshf);
         points.add (p);
      }
      return points;
   }
   
}
