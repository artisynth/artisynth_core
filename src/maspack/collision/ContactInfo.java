package maspack.collision;

import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.matrix.Point3d;
import maspack.render.Renderer;
import maspack.render.Renderer.VertexDrawMode;

public class ContactInfo {
   /*
    * 
    * Information returned in ContactInfo:
    * 
    * RigidBody to RigidBody: - Set of contact regions, with for each region: -
    * normal based on planar fit - separation distance - the min distance we
    * need to move along the normal to separate the bodies, which we approximate
    * by taking the maximum of: - range distance of contour points WRT to the
    * plane - distance of each internal vertex to the opposite mesh along the
    * normal
    *  - contour points, culled so that points closer than pointTolerance are
    * merged, and *possibly* reduced to a convex hull WRT the plane
    * 
    * FemBody to RigidBody, FemBody to FemBody: - Set of interpenetrating FEM
    * vertices (over all regions), and the nearest point on opposite mesh, with
    * associate face and barycentric coordinates. Currently returned in
    * ContactPenetratingPoint - If there are no penetrating vertices, return a
    * list of edge-edge intersections.
    */

   // the intersections
   public ArrayList<TriTriIntersection> intersections =
      new ArrayList<TriTriIntersection>();

   // the colliding meshes
   public PolygonalMesh mesh0;
   public PolygonalMesh mesh1;

   public ArrayList<MeshIntersectionContour> contours;

   // the set of penetrating points for each mesh
   public ArrayList<ContactPenetratingPoint> points0 =
      new ArrayList<ContactPenetratingPoint>();
   public ArrayList<ContactPenetratingPoint> points1 =
      new ArrayList<ContactPenetratingPoint>();

   public ArrayList<EdgeEdgeContact> edgeEdgeContacts =
      new ArrayList<EdgeEdgeContact>();

   // contact regions with normals facing mesh 0
   public ArrayList<ContactRegion> regions = new ArrayList<ContactRegion>();

   public ContactInfo (PolygonalMesh m0, PolygonalMesh m1) {
      mesh0 = m0;
      mesh1 = m1;
   }

   public void render (Renderer renderer, int flags) {

      /*
       * For fem-fem collisions render lines from each penetrating vertex to the
       * nearest point on an opposing face.
       */
      renderCPPoints (renderer, points0);
      renderCPPoints (renderer, points1);
      if (regions.isEmpty()) {
         for (MeshIntersectionContour contour : contours)
            contour.render (renderer, flags);
      }
      else {
         for (ContactRegion region : regions)
            region.render (renderer, flags);
      }
      ;
   }

   void renderCPPoints (
      Renderer renderer, ArrayList<ContactPenetratingPoint> points) {
      
      renderer.setColor (0.9f, 0.6f, 0.8f);
      renderer.beginDraw (VertexDrawMode.LINES);
      for (ContactPenetratingPoint p : points) {
         Point3d n1 = p.position;
         renderer.addVertex (n1);
         n1 = p.vertex.getWorldPoint();
         renderer.addVertex (n1);
      }
      renderer.endDraw();

      renderer.setColor (1f, 0f, 0f);
      renderer.setPointSize (30);
      renderer.beginDraw (VertexDrawMode.POINTS);
      for (ContactPenetratingPoint p : points) {
         renderer.addVertex (p.position);
      }
      renderer.endDraw();
      renderer.setPointSize (1);
      renderer.setColor (0f, 1f, 0f);

   }

}
