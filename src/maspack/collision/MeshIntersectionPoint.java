package maspack.collision;

import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.geometry.RobustPreds;
import maspack.geometry.Vertex3d;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;

public class MeshIntersectionPoint extends Point3d {
   private static final long serialVersionUID = 1L;
   public MeshIntersectionContour contour;
   public Face face;
   public HalfEdge edge;
   public boolean edgeRegion;
   public double radius; // used for calculating contact regions
   public Vector3d radiusVector;
   public double radiusArea; /*
                               * Area of the parallelogram defined by this
                               * radius vector and the previous point's radius
                               * vector. Used for reducing the contour to a
                               * pseudo-convex hull.
                               */
   public boolean isCoincident; // set to true if this mip is found to be
                                // exactly coincident with another mip with the
                                // same edge.

   public MeshIntersectionPoint() {
   }

   public boolean matches (MeshIntersectionPoint p) {
      return face == p.face && edge == p.edge;
   }

   public boolean isVertexInsideFace (Vertex3d v) {
      HalfEdge e = face.getEdge (0);
      Vertex3d v0 = e.tail;
      Vertex3d v1 = e.head;
      Vertex3d v2 = e.getNext().head;
      boolean in = RobustPreds.orient3d (v0, v1, v2, v);
      return contour.isInverting ? !in : in;
   }

//   /*
//    * Return the barycentric coordinate of the intersection with respect to the
//    * edge's head.
//    */
//   double lambda() {
//      Point3d t = edge.tail.getWorldPoint();
//      return t.distance (this) / t.distance (edge.head.getWorldPoint());
//   }
}
