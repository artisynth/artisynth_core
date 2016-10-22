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

public class SurfaceMeshCollider implements AbstractCollider {
   // If true, use this class as a collider.
   // If false, use maspack.collision.MeshCollider
   private static boolean useAjlCollision = false;
   //public static boolean doEdgeEdgeContacts = false;
   //public static boolean renderContours = false;
   
//   public long sumTime = 0;
//   public long averageTime = 0;
//   public long startTime = 0;
//   public long finishTime = 0;
//   public long maxTime = 0;
//   public long minTime = Long.MAX_VALUE;
//   public int runCount = 0;
//   boolean warmup = false;
   
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
    * Method getContacts returns this ContactInfo populated with all of the
    * information about the collision.
    */
   //public ContactInfo contactInfo;

   /*
    * Currently set false for rigid body-rigid body collisions,
    * =doEdgeEdgeContacts otherwise.
    */
   //public boolean doEdgeEdgeContactsThisTime;

//   /*
//    * Remember the maximum distance of vertex-face contacts, for use in
//    * determining validity of edge-edge contacts.
//    */
//   double maxVertexFaceDistance;

//   /* Work item for determining validity of candidate edge-edge contacts. */
//   EdgeEdgeContact eec = new EdgeEdgeContact ();

//   /*
//    * Used for edge-edge contacts only. Used for determining whether correction
//    * of an edge-edge contact is necessary. Penetrating vertex corrections are
//    * applied first, and if they will un-collide the edges, no correction is
//    * needed. This hashtable allows the ContactPenetratingPoint for a given
//    * vertex to be determined. It can then be determined where the vertices of a
//    * colliding edge will be after they are corrected.
//    */
//   private Hashtable<Vertex3d,ContactPenetratingPoint> vertexCpps;

//   /*
//    * For convenience, points to the list of edgeEdgeContacts in the current
//    * contactInfo.
//    */
//   ArrayList<EdgeEdgeContact> edgeEdgeContacts;

   /*
    * The intersector is used to find the nearest point on an opposing face for
    * each inside vertex.
    */
   private TriangleIntersector faceIntersector = new TriangleIntersector();
   /* Working variables for the intersector */
   //Point3d nearesttmp = new Point3d();
   //Vector2d coordstmp = new Vector2d();
   //NearestPoint np = new NearestPoint();

   /* Debugging and performance testing items. */
//   public static CollisionMetrics collisionMetrics =
//      new CollisionMetrics ("ajl");
   //public static ContactInfo lastContactInfo;
   //public static String debugPath = "/artisynth/";

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
      
      ContactInfo contactInfo = new ContactInfo (mesh0, mesh1);
      boolean intersected = 
         meshIntersector.findContoursAndRegions (contactInfo, mesh0, mesh1);
      
      if (intersected) {
         // John Lloyd, August 2016. Not sure what this is doing here.
         // It takes a lot of time, so has been commented out
         // for (MeshIntersectionContour c : meshIntersector.contours) {
         //    for (Vertex3d v1 : c.insideVertices0) {
         //       for (Face f1 : c.insideFaces1) {
         //          if (v1.getMesh() == f1.getMesh()) {
         //             throw new RuntimeException ("wrong mesh");
         //          }
         //       }
         //    }
         //    for (Vertex3d v1 : c.insideVertices1) {
         //       for (Face f1 : c.insideFaces0) {
         //          if (v1.getMesh() == f1.getMesh()) {
         //             throw new RuntimeException ("wrong mesh");
         //          }
         //       }
         //    }
         // }
//         contactInfo.myPointTol = pointTolerance;
//         contactInfo.myRegionTol = regionTolerance;
         return contactInfo;
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
      for (Vertex3d vtx : region.myInsideVertices) {
         vtx.getWorldPoint (wpnt);
         Face face = query.nearestFaceToPoint (nearPnt, uv, otherMesh, wpnt);
         disp.sub (nearPnt, wpnt);
         cpp = new PenetratingPoint (vtx, face, uv, nearPnt, disp, region);
         //maxVertexFaceDistance =
         //   Math.max (maxVertexFaceDistance, disp.norm());
         cpps.add (cpp);
//         if (doEdgeEdgeContactsThisTime) {
//            vertexCpps.put (vtx, cpp);
//         }
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
      /*
       * double maxD = 2 * maxVertexFaceDistance; for (MeshIntersectionContour
       * contour: contours) { if (contour.hasInterior()) { MeshIntersectionPoint
       * mipB = contour.get(contour.size() - 1); for (MeshIntersectionPoint
       * mipA: contour) { if (interlocking(mipA, mipB)) { boolean keep; if
       * (mipA.edgeRegion) keep = eec.calculate(mipA, mipB, maxD); else keep =
       * eec.calculate(mipB, mipA, maxD); if (keep) { edgeEdgeContacts.add(eec);
       * eec = new EdgeEdgeContact(this); } } mipB = mipA; } } }
       */
      HashMap<Vertex3d,PenetratingPoint> vertexCpps = 
         new HashMap<Vertex3d,PenetratingPoint>();
      for (PenetratingPoint cpp : cinfo.getPenetratingPoints0()) {
         vertexCpps.put (cpp.vertex, cpp);
      }
      for (PenetratingPoint cpp : cinfo.getPenetratingPoints1()) {
         vertexCpps.put (cpp.vertex, cpp);
      }
      ArrayList<EdgeEdgeContact> contacts = new ArrayList<EdgeEdgeContact>();
      
      HashMap<PenetrationRegion,PenetrationRegion> opposingRegions = 
         cinfo.findMatchingRegions ();

      for (PenetrationRegion r0 : opposingRegions.keySet()) {
         PenetrationRegion r1 = opposingRegions.get(r0);
         for (HalfEdge edge0 : r0.myInsideEdges) {
            if (needsEdgeEdgeCorrection (edge0, vertexCpps)) {
               for (HalfEdge edge1 : r1.myInsideEdges) {
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

//   /*
//    * The following code does nothing and is merely for compatibility with the
//    * old collision code.
//    */
//   private double pointTolerance = 1e-6; // min distance between contact points
//   private double regionTolerance = 1e-2; // min distance between regions

   public SurfaceMeshCollider() {
//      setPointTolerance (1e-6);
//      setRegionTolerance (1e-2);
   }

//   public double getPointTolerance() {
//      return pointTolerance;
//   }
//
//   public void setPointTolerance (double tolerance) {
//      this.pointTolerance = tolerance;
//   }
//
//   public double getRegionTolerance() {
//      return regionTolerance;
//   }
//
//   public void setRegionTolerance (double regionTolerance) {
//      this.regionTolerance = regionTolerance;
//   }

   // /* Debugging items. */
   // static boolean addedAsRenderable = false;
   // public static ContactInfoRenderer contactInfoRenderer;

   // void addAsRenderable() {
   //    if (!addedAsRenderable) {
   //       addedAsRenderable = true;
   //       Main.getMain().getViewer().addRenderable (
   //          contactInfoRenderer = new ContactInfoRenderer());
   //    }
   // }

//   public class ContactInfoRenderer implements IsRenderable {
//
//      IdentityHashMap<PolygonalMesh,IdentityHashMap<PolygonalMesh,ContactInfo>> renderableContactInfos =
//         new IdentityHashMap<PolygonalMesh,IdentityHashMap<PolygonalMesh,ContactInfo>>();
//      public Face face;
//      public Face face1;
//      public HalfEdge edge;
//      public Point3d point;
//
//      void removeAllRenderables() {
//         renderableContactInfos.clear(); 
//      }
//
//      void removeContactInfo (PolygonalMesh m0, PolygonalMesh m1) {
//         ContactInfo ci;
//         IdentityHashMap<PolygonalMesh,ContactInfo> rs;
//         synchronized (renderableContactInfos) {
//            rs = renderableContactInfos.get (m0);
//            if (rs != null) {
//               ci = rs.get (m1);
//               if (ci != null) {
//                  if (m0 != ci.mesh0 | m1 != ci.mesh1)
//                     throw new RuntimeException ("no such contact info");
//                  // System.out.println("removing "+m0.name+" "+m1.name+"
//                  // "+ci.contours.size());
//                  rs.remove (m1);
//               }
//            }
//         }
//      }
//
//      int countInfos() {
//         int ans = 0;
//         synchronized (renderableContactInfos) {
//            for (PolygonalMesh m0 : renderableContactInfos.keySet()) {
//               IdentityHashMap<PolygonalMesh,ContactInfo> rs =
//                  renderableContactInfos.get (m0);
//               if (rs != null) {
//                  for (PolygonalMesh m1 : rs.keySet()) {
//                     ContactInfo ci = rs.get (m1);
//                     if (ci != null) {
//                        // System.out.println(" "+m0.name+" "+m1.name+"
//                        // "+ci.contours.size());
//                        ans += ci.contours.size();
//                     }
//                  }
//               }
//            }
//         }
//         return ans;
//      }
//
//      void addContactInfo (ContactInfo info) {
//         IdentityHashMap<PolygonalMesh,ContactInfo> rs;
//         synchronized (renderableContactInfos) {
//            rs = renderableContactInfos.get (info.mesh0);
//            if (rs == null) {
//               rs = new IdentityHashMap<PolygonalMesh,ContactInfo>();
//               renderableContactInfos.put (info.mesh0, rs);
//            }
//            rs.put (info.mesh1, info);
//            // System.out.println("adding "+info.mesh0.name+"
//            // "+info.mesh1.name+" "+info.contours.size());
//         }
//      }
//
//      public void render (Renderer renderer, int flags) {
//         
//         renderer.setShading (Shading.NONE);
//         //gl.glDisable (GL2.GL_LINE_STIPPLE);
//         //gl.glEnable (GL2.GL_LINE_SMOOTH);
//         renderer.setLineWidth (6);
//         synchronized (renderableContactInfos) {
//            for (PolygonalMesh m0 : renderableContactInfos.keySet()) {
//               IdentityHashMap<PolygonalMesh,ContactInfo> rs;
//               rs = renderableContactInfos.get (m0);
//               for (PolygonalMesh m1 : rs.keySet()) {
//                  // renderMeshNormals(renderer, m0);
//                  // renderMeshNormals(renderer, m1);
//                  rs.get (m1).render (renderer, flags);
//               }
//            }
//         }
//         if (edge != null) {
//            renderer.setColor (1, 1, 1f);
//            renderer.setLineWidth (26);
//            Point3d p0 = edge.tail.getWorldPoint();
//            Point3d p1 = edge.head.getWorldPoint();
//            renderer.drawLine (p0, p1);
//         }
//         if (face != null) {
//            renderer.setColor (1, 0.2f, 0.6f);
//            renderFace (renderer, face);
//         }
//         if (face1 != null) {
//            renderer.setColor (0.5f, 0.2f, 0.9f);
//            renderFace (renderer, face1);
//         }
//
//         if (point != null) {
//            renderer.setColor (1, 1, 1f);
//            renderer.setPointSize (40);
//            renderer.drawPoint (point);
//            renderer.setPointSize (1);
//         }
//         renderer.setLineWidth (1);
//         renderer.setShading (Shading.FLAT);
//      }
//
//      void renderFace (Renderer renderer, Face aFace) {
//         
//         renderer.setLineWidth (26);
//         renderer.beginDraw (DrawMode.LINE_LOOP);
//         Point3d p = aFace.getEdge (0).tail.getWorldPoint();
//         renderer.addVertex (p);
//         p = aFace.getEdge (0).head.getWorldPoint();
//         renderer.addVertex (p);
//         p = aFace.getEdge (1).head.getWorldPoint();
//         renderer.addVertex (p);
//         renderer.endDraw();
//      }
//
//      void renderMeshNormals (Renderer renderer, PolygonalMesh mesh) {
//         
//         Point3d p = new Point3d();
//         renderer.setLineWidth (20);
//         renderer.beginDraw (DrawMode.LINES);
//         for (Face f : mesh.getFaces()) {
//            renderer.setColor (1, 1, 0.5f);
//            f.computeWorldCentroid (p);
//            renderer.addVertex (p);
//            p.scaledAdd (0.3, f.getWorldNormal());
//            renderer.addVertex (p);
//         }
//         renderer.endDraw();
//         renderer.setLineWidth (1);
//      }
//
//      public int getRenderHints() {
//         return 0;
//      }
//
//      public void prerender (RenderList list) {
//      }
//
//      public void updateBounds (Vector3d pmin, Vector3d pmax) {
//      }
//   }

}
