package maspack.collision;

import java.util.ArrayList;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.DataBuffer;

public class EdgeEdgeContact {
   public HalfEdge edge0, edge1;

   // Points on line of either edge defining smallest distance.
   public Point3d point0, point1;

   /*
    * The initial vector from point0 to point1. Just before actually applying
    * the correction for this contact, calculate() is called again because the
    * vertex positions may have changed since calculate() was first called. If
    * (point1 - point0).dot(penetration) is positive, assume the edges are still
    * inside each other.
    */
   public Vector3d point1ToPoint0Normal = new Vector3d();
   public double displacement;
   public PenetrationRegion region; // region associated with edge0

   // fraction along line segment from tail to head, of point0 and point1.
   public double s0, s1;

   // work vector
   public Vector3d w = new Vector3d();

   public EdgeEdgeContact () {
      point0 = new Point3d();
      point1 = new Point3d();
   }

   public boolean calculate (
      SurfaceMeshCollider collider, HalfEdge e0, HalfEdge e1) {
      edge0 = e0;
      edge1 = e1;
      return calculate(collider);
   }

   static double insideLim = 0.25;
   
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
   /*
    * return true if the specified point is "opposite" and inside the face,
    * which means close to being inside a volume swept by translating the face
    * along its normal.
    */
   boolean isPointInsideFace (
      SurfaceMeshCollider collider, Point3d p, Face f) {
      Point3d nearest = new Point3d();
      collider.getNearestPoint (nearest, f, p);
      nearest.sub (p);
      if (nearest.dot (nearest) == 0) {
         return true;
      }
      double x = nearest.angle (f.getWorldNormal());
      return x < insideLim;
   }

   /*
    * Evaluate a candidate edge-edge contact of two edges which are not adjacent
    * in the contour. Test the geometry to see if it is valid. Find the closest
    * points on the lines of the edges, which are required to be on the edges
    * (between their vertices). Then at least one of these points must be on the
    * inside of both faces adjacent to the other edge.
    */
   public boolean calculate(SurfaceMeshCollider collider) {
      Point3d p0 = edge0.tail.getWorldPoint();
      point0.sub (edge0.head.getWorldPoint(), p0);
      // temp value of point0 = direction vector of edge0

      Point3d p1 = edge1.tail.getWorldPoint();
      point1.sub (edge1.head.getWorldPoint(), p1);
      // temp value of point1 = direction vector of edge0

      point1ToPoint0Normal.cross (point0, point1);
      point1ToPoint0Normal.normalize();
      w.sub (p0, p1);
      double a = point0.dot (point0), b = point0.dot (point1), c =
         point1.dot (point1), d = point0.dot (w), e = point1.dot (w);
      double f = (a * c) - (b * b);
      if (f == 0) {
         return false;
      }
      s0 = ((b * e) - (c * d)) / f;
      if (s0 < 0 | s0 > 1) {
         return false;
      }
      s1 = ((a * e) - (b * d)) / f;
      if (s1 < 0 | s1 > 1) {
         return false;
      }
      point0.scale (s0);
      point0.add (p0);
      point1.scale (s1);
      point1.add (p1);
      if (!((isPointInsideFace (collider, point0, edge1.getFace()) &&
             isPointInsideFace (collider, point0, edge1.opposite.getFace())) ||
            (isPointInsideFace (collider, point1, edge0.getFace()) &&
             isPointInsideFace (collider, point1, edge0.opposite.getFace())))) {
         return false;
      }
      /*
       * Use the cross product of the two edge directions to set the direction
       * of the point0ToPoint1 vector, instead of point1 - point0, to prevent
       * precision problems when point0 and point1 are close.
       */
      w.sub (point0, point1);
      displacement = w.norm();
      if (w.dot (point1ToPoint0Normal) < 0) {
         point1ToPoint0Normal.negate();
      }
      return true;
   }

   boolean equals (EdgeEdgeContact econ, StringBuilder msg) {
      if (edge0 != econ.edge0) {
         if (msg != null) msg.append ("edge0 differs\n");
         return false;
      }
      if (edge1 != econ.edge1) {
         if (msg != null) msg.append ("edge1 differs\n");
         return false;
      }
      if (!point0.equals (econ.point0)) {
         if (msg != null) msg.append ("point0 differs\n");
         return false;
      }
      if (!point1.equals (econ.point1)) {
         if (msg != null) msg.append ("point1 differs\n");
         return false;
      }
      if (!point1ToPoint0Normal.equals (econ.point1ToPoint0Normal)) {
         if (msg != null) msg.append ("point1ToPoint0Normal differs\n");
         return false;
      }
      if (displacement != econ.displacement) {
         if (msg != null) msg.append ("displacement differs\n");
         return false;
      }
      if (s0 != econ.s0) {
         if (msg != null) msg.append ("s0 differs\n");
         return false;
      }
      if (s1 != econ.s1) {
         if (msg != null) msg.append ("s1 differs\b");
         return false;
      }
      return true;
   }

   static boolean equals (
      ArrayList<EdgeEdgeContact> contacts0,
      ArrayList<EdgeEdgeContact> contacts1,
      StringBuilder msg) {

      if ((contacts0 != null) != (contacts1 != null)) {
         if (msg != null) msg.append ("contacts != null differs\n");
         return false;
      }
      if (contacts0 != null) {
         if (contacts0.size() != contacts1.size()) {
            if (msg != null) msg.append ("contacts size differs\n");
            return false;
         }
         for (int i=0; i<contacts0.size(); i++) {
            if (!contacts0.get(i).equals (contacts1.get(i), msg)) {
               if (msg != null) msg.append ("contacts "+i+" differs\n");
               return false;
            }
         }
      }
      return true;
   }

   static DataBuffer.Offsets getStateSize() {
      return new DataBuffer.Offsets (2, 12, 0);
   }

   void getState (DataBuffer data) {
      data.zput (edge0.getIndex());
      data.zput (edge1.getIndex());
      data.dput (point0);
      data.dput (point1);
      data.dput (point1ToPoint0Normal);
      data.dput (displacement);
      data.dput (s0);
      data.dput (s1);
   }

   void setState (DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      edge0 = mesh0.getHalfEdge (data.zget());
      edge1 = mesh1.getHalfEdge (data.zget());
      data.dget (point0);
      data.dget (point1);
      data.dget (point1ToPoint0Normal);
      displacement = data.dget();
      s0 = data.dget();
      s1 = data.dget();
   }

   static void getState (
      DataBuffer data, ArrayList<EdgeEdgeContact> contacts) {

      if (contacts == null) {
         data.zput (-1);
         return;
      }
      data.zput (contacts.size());
      for (int i=0; i<contacts.size(); i++) {
         contacts.get(i).getState (data);
      }
   }

   static ArrayList<EdgeEdgeContact> setStateArray (
      DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {

      int size = data.zget();
      if (size == -1) {
         return null;
      }
      ArrayList<EdgeEdgeContact> contacts = new ArrayList<EdgeEdgeContact>(size);
      for (int i=0; i<size; i++) {
         EdgeEdgeContact c = new EdgeEdgeContact();
         c.setState (data, mesh0, mesh1);
         contacts.add (c);
      }
      return contacts;
   }

//   /*
//    * Test a candidate edge-edge contact resulting from two mesh intersection
//    * points which neighbour each other on a contour and are from different edge
//    * regions, ie. two interlocking triangles. To be a valid eec, require that
//    * the resulting displacement is less than a specified value which is some
//    * factor times the maximum of displacements already found for vertex-face
//    * contacts. This requirement reflects the fact that a collision can be
//    * resolved by displacements in many different directions, but the direction
//    * that involves the smallest displacements is typically the most physically
//    * plausible, and it is the direction that any vertex-face contacts will
//    * already have found. So any additional edge-edge contacts should involve
//    * displacements of the same order of magnitude.
//    */
//   public boolean calculate (
//      MeshIntersectionPoint mip0, MeshIntersectionPoint mip1,
//      double maxDisplacement) {
//      edge0 = mip0.edge;
//      edge1 = mip1.edge;
//      point1ToPoint0Normal.sub (mip0, mip1);
//      displacement = point1ToPoint0Normal.norm();
//      point1ToPoint0Normal.scale (1.0 / displacement);
//      point0.sub (edge0.tail.getWorldPoint(), edge0.head.getWorldPoint());
//      point1.sub (edge1.tail.getWorldPoint(), edge1.head.getWorldPoint());
//      w.cross (point0, point1);
//      w.normalize();
//      double d = Math.abs (w.dot (point1ToPoint0Normal));
//      point0.set (mip0);
//      point1.set (mip1);
//      s0 = mip0.lambda();
//      s1 = mip1.lambda();
//      return d > 0.9 & displacement <= maxDisplacement;
//
//   }
}
