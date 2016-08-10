package maspack.collision;

import maspack.matrix.Point3d;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;

public class IntersectionPoint extends Point3d {
   private static final long serialVersionUID = 1L;
   public IntersectionContour contour;
   int contourIndex = -1; // index giving the position along the contour
   public Face face;
   public HalfEdge edge;
   public boolean edgeOnMesh0;
   public boolean isCoincident; // set to true if this mip is found to be
                                // exactly coincident with another mip with the
                                // same edge.

   public IntersectionPoint() {
   }

   public boolean matches (IntersectionPoint p) {
      return face == p.face && edge == p.edge;
   }

//   public boolean isVertexInsideFace (Vertex3d v, boolean vertexOnMesh0) {
//      HalfEdge e = face.getEdge (0);
//      Vertex3d v0 = e.tail;
//      Vertex3d v1 = e.head;
//      Vertex3d v2 = e.getNext().head;
//      boolean in = RobustPreds.orient3d (v0, v1, v2, v, vertexOnMesh0);
//      return contour.isInverting ? !in : in;
//   }
   
   public PolygonalMesh getOtherMesh (PolygonalMesh mesh) {
      PolygonalMesh emesh = (PolygonalMesh)edge.getHead().getMesh();
      if (emesh == mesh) {
         return (PolygonalMesh)face.getMesh();
      }
      else {
         return emesh;
      }
   }

//   public MeshIntersectionPoint getNext() {
//      if (contour != null) {
//         return contour.get(contourIndex+1);
//      }
//      else {
//         return null;
//      }
//   }
//
//   public MeshIntersectionPoint getPrev() {
//      if (contour != null) {
//         return contour.get(contourIndex-1);
//      }
//      else {
//         return null;
//      }
//   }

//   /*
//    * Return the barycentric coordinate of the intersection with respect to the
//    * edge's head.
//    */
//   double lambda() {
//      Point3d t = edge.tail.getWorldPoint();
//      return t.distance (this) / t.distance (edge.head.getWorldPoint());
//   }
}
