package maspack.collision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriangleIntersector;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.collision.SurfaceMeshIntersector.RegionType;

public class SurfaceMeshCollider implements AbstractCollider {

   private static boolean useAjlCollision = false;
   
   /**
    * Enables the default use of AJL collisions.
    */
   public static void setAjlCollision (boolean enable) {
      useAjlCollision = enable;
   }
   
   /**
    * Queries whether AJL collisions are enabled by default.
    */
   public static boolean getAjlCollision() {
      return useAjlCollision;
   }
   
   /*
    * The meshIntersector generates the contours and feature information that
    * describe the mesh intersection.
    */
   public SurfaceMeshIntersector meshIntersector =
      new SurfaceMeshIntersector();

   /*
    * The intersector is used to find the nearest point on an opposing face for
    * each inside vertex.
    */
   private TriangleIntersector faceIntersector = new TriangleIntersector();

   // Facilitates the useAjlCollision flag
   public static AbstractCollider newCollider() {
      if (SurfaceMeshCollider.useAjlCollision) {
         return new SurfaceMeshCollider();
      }
      else {
         return new MeshCollider();
      }
   }

   HashMap<PenetrationRegion,PenetrationRegion> findOpposingRegions(
      ArrayList<PenetrationRegion> regions0, 
      ArrayList<PenetrationRegion> regions1) {
      
      HashSet<PenetrationRegion> unmatched1 = 
         new HashSet<PenetrationRegion>();
      HashMap<PenetrationRegion,PenetrationRegion> opposingRegions = 
         new HashMap<PenetrationRegion,PenetrationRegion>();
      unmatched1.addAll (regions1);
      for (PenetrationRegion r0 : regions0) {
         PenetrationRegion found = null;
         for (PenetrationRegion r1 : regions1) {
            if (r0.myContours.equals(r1.myContours)) {
               found = r1;
            }
         }
         if (found != null) {
            opposingRegions.put (r0, found);
            regions1.remove (found);
         }
      }
      return opposingRegions;
   }      
      
   /*
    * Main interface for determining contact information.  ContactInfo
    * populated with the results of the collision analysis. Returns null if
    * there is no collision. mesh0, mesh1 are the candidate colliding surface
    * meshes.
    */
   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {

      ContactInfo cinfo = meshIntersector.findContoursAndRegions (
         mesh0, RegionType.INSIDE, mesh1, RegionType.INSIDE);
      if (cinfo != null && cinfo.numContours() > 0) {
         return cinfo;
      }
      else {
         return null;
      }
   }

   public ArrayList<IntersectionContour> getContours (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      ArrayList<IntersectionContour> contours = 
         meshIntersector.findContours (mesh0, mesh1);
      return contours.size() > 0 ? contours : null;
   }
   
   /*
    * For each penetrating vertex, find the closest opposing face and add a
    * corresponding new element to the list of ContactPenetratingPoints
    */
   public static void collideVerticesWithFaces (
      ArrayList<PenetratingPoint> cpps,
      PenetrationRegion region, 
      PolygonalMesh otherMesh) {
      
      BVFeatureQuery query = new BVFeatureQuery();
      Vector2d uv = new Vector2d();
      Point3d nearPnt = new Point3d();
      Vector3d disp = new Vector3d();
      Point3d wpnt = new Point3d();

      PenetratingPoint cpp;
      for (Vertex3d vtx : region.myVertices) {
         vtx.getWorldPoint (wpnt);
         Face face = query.nearestFaceToPoint (nearPnt, uv, otherMesh, wpnt);
         disp.sub (nearPnt, wpnt);
         cpp = new PenetratingPoint (vtx, face, uv, nearPnt, disp, region);
         cpps.add (cpp);
      }
   }

   /*
    * Return true if the edge has no penetrating vertices, or if when the edge's
    * penetrating vertices are moved to their nearest opposing point, the edge
    * will still point inside from an opposing face.
    */
   static boolean needsEdgeEdgeCorrection (
      HalfEdge edge, HashMap<Vertex3d,PenetratingPoint> vertexCpps) {
      PenetratingPoint cppTail = vertexCpps.get (edge.getTail());
      PenetratingPoint cppHead = vertexCpps.get (edge.getHead());

      Vector3d tmp = new Vector3d();
      if (cppHead == null && cppTail == null) {
         return true;
      }
      Point3d newTailPosition =
         cppTail == null ? edge.getTail().getWorldPoint() : cppTail.position;
      Point3d newHeadPosition =
         cppHead == null ? edge.getHead().getWorldPoint() : cppHead.position;
      double eps = 0; // don't know if we care about epsilon here
      if (cppTail != null) {
         tmp.sub (newHeadPosition, cppTail.position);
         if (tmp.dot (cppTail.face.getWorldNormal()) < -eps) {
            return true;
         }
      }
      if (cppHead != null) {
         tmp.sub (newTailPosition, cppHead.position);
         if (tmp.dot (cppHead.face.getWorldNormal()) < -eps) {
            return true;
         }
      }
      return false;
   }

   /*
    * Create an edge-edge contact for each unique pair of HalfEdges where - the
    * edges are from opposite regions, - both edges are in need of edge-edge
    * correction - the geometry is suitable (see EdgeEdgeContact.calculate())
    */
   public ArrayList<EdgeEdgeContact> findEdgeEdgeContacts (
      ContactInfo cinfo) {

      /* First scan the contours for interlocking triangles, */
      HashMap<Vertex3d,PenetratingPoint> vertexCpps = 
         new HashMap<Vertex3d,PenetratingPoint>();
      for (PenetratingPoint cpp : cinfo.getPenetratingPoints(0)) {
         vertexCpps.put (cpp.vertex, cpp);
      }
      for (PenetratingPoint cpp : cinfo.getPenetratingPoints(1)) {
         vertexCpps.put (cpp.vertex, cpp);
      }
      ArrayList<EdgeEdgeContact> contacts = new ArrayList<EdgeEdgeContact>();
      
      HashMap<PenetrationRegion,PenetrationRegion> opposingRegions = 
         cinfo.findMatchingRegions ();

      for (PenetrationRegion r0 : opposingRegions.keySet()) {
         PenetrationRegion r1 = opposingRegions.get(r0);
         for (HalfEdge edge0 : r0.myEdges) {
            if (needsEdgeEdgeCorrection (edge0, vertexCpps)) {
               for (HalfEdge edge1 : r1.myEdges) {
                  if (needsEdgeEdgeCorrection (edge1, vertexCpps)) {
                     EdgeEdgeContact eec = new EdgeEdgeContact ();
                     if (eec.calculate (this, edge0, edge1)) {
                        // Test the geometry to see if it is a valid edge-edge
                        // contact.
                        eec.region = r0;
                        contacts.add (eec);
                     }
                  }
               }
            }
         }        
      }
      return contacts;
   }

   /* Helper class used to find nearest opposing face to a vertex. */
   static class NearestPoint {
      Face face = null;
      Point3d point = new Point3d();
      Vector2d uv = new Vector2d();
      double dist = Double.POSITIVE_INFINITY;
      Vector3d disp = new Vector3d();
   }

   public double getNearestPoint (Point3d nearest, Face f, Point3d p) {
      Point3d p0, p1, p2;
      p0 = f.getVertex (0).getWorldPoint();
      p1 = f.getVertex (1).getWorldPoint();
      p2 = f.getVertex (2).getWorldPoint();
      return faceIntersector.nearestpoint (p0, p1, p2, p, nearest, null);
   }

   public SurfaceMeshCollider() {
   }
}
