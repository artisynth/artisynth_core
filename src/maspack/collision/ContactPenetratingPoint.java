package maspack.collision;

import java.util.Comparator;

import maspack.geometry.Face;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

public class ContactPenetratingPoint implements Comparable<ContactPenetratingPoint> {
   // the vertex projected
   public Vertex3d vertex;

   // target position = (1-coords.x-coords.y) * face.v0 + coords.x * face.v1 +
   // coords.y * face.v2
   public Face face;
   public Vector2d coords;// = new Vector2d();
   public Point3d position = new Point3d(); // point on the face that is
                                             // closest to vertex, in world
                                             // coordinates.

   public Vector3d normal; // normal from interpenetrating point to surface
   public double distance; // distance from interpenetrating point to surface,
                           // in world coordinates
   
   public ContactPenetratingPoint (
      Vertex3d aVertex, Face opposingFace,
      Vector2d pointBarycentricCoords, Point3d nearestFacePoint,
      Vector3d dispToNearestFace) {
      
      vertex = aVertex;
      face = opposingFace;
      coords = new Vector2d (pointBarycentricCoords);
      position.set (nearestFacePoint);
      distance = dispToNearestFace.norm();
      normal = new Vector3d(dispToNearestFace);
      if (distance != 0) {
         normal.normalize();
      }
   }

   public ContactPenetratingPoint (
      Vertex3d vertex, Vector3d normal, double distanceToSurface) {
      
      this.vertex = vertex;
      face = null;
      coords = null;
      this.normal = new Vector3d (normal);
      distance = distanceToSurface;
      position.scaledAdd (distance, normal, vertex.pnt);
      
   }

   @Override
   public int compareTo(ContactPenetratingPoint o) {
      if (this.distance < o.distance) {
         return -1;
      } else if (this.distance == o.distance) {
         return 0;
      }
      return 1;
   }
   
   public static class DistanceComparator implements Comparator<ContactPenetratingPoint> {

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
      public int compare(ContactPenetratingPoint o1, ContactPenetratingPoint o2) {
         if (o1.distance > o2.distance) {
            return one;
         } else if (o1.distance < o2.distance) {
            return -one;
         } else {
            return 0;
         }
      }
   }
  
   
   public static DistanceComparator createMaxDistanceComparator() {
      return new DistanceComparator(true);
   }
   
   public static DistanceComparator createMinDistanceComparator() {
      return new DistanceComparator();
   }
   
}
