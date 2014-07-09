package maspack.collision;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;

import javax.media.opengl.GL2;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriangleIntersector;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.util.InternalErrorException;

public class SurfaceMeshCollider implements AbstractCollider {
   // If true, use this class as a collider.
   // If false, use maspack.collision.MeshCollider
   public static boolean useAjlCollision = false;
   public static boolean doEdgeEdgeContacts = false;
   public static boolean renderContours = false;

   public PolygonalMesh mesh0;
   public PolygonalMesh mesh1;
   
//   public long sumTime = 0;
//   public long averageTime = 0;
//   public long startTime = 0;
//   public long finishTime = 0;
//   public long maxTime = 0;
//   public long minTime = Long.MAX_VALUE;
//   public int runCount = 0;
//   boolean warmup = false;
   
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
   public ContactInfo contactInfo;

   /*
    * Currently set false for rigid body-rigid body collisions,
    * =doEdgeEdgeContacts otherwise.
    */
   public boolean doEdgeEdgeContactsThisTime;

   /*
    * Remember the maximum distance of vertex-face contacts, for use in
    * determining validity of edge-edge contacts.
    */
   double maxVertexFaceDistance;

   /* Work item for determining validity of candidate edge-edge contacts. */
   EdgeEdgeContact eec = new EdgeEdgeContact (this);

   /*
    * Used for edge-edge contacts only. Used for determining whether correction
    * of an edge-edge contact is necessary. Penetrating vertex corrections are
    * applied first, and if they will un-collide the edges, no correction is
    * needed. This hashtable allows the ContactPenetratingPoint for a given
    * vertex to be determined. It can then be determined where the vertices of a
    * colliding edge will be after they are corrected.
    */
   private Hashtable<Vertex3d,ContactPenetratingPoint> vertexCpps;

   /*
    * For convenience, points to the list of edgeEdgeContacts in the current
    * contactInfo.
    */
   ArrayList<EdgeEdgeContact> edgeEdgeContacts;

   /*
    * The intersector is used to find the nearest point on an opposing face for
    * each inside vertex.
    */
   private TriangleIntersector faceIntersector = new TriangleIntersector();
   /* Working variables for the intersector */
   Point3d nearesttmp = new Point3d();
   Vector2d coordstmp = new Vector2d();
   NearestPoint np = new NearestPoint();

   /* Debugging and performance testing items. */
   public static SurfaceMeshCollider the;
   public static CollisionMetrics collisionMetrics =
      new CollisionMetrics ("ajl");
   public static ContactInfo lastContactInfo;
   public static long renderTime = -1;
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

   /*
    * Main interface for collision -- called by CollisionPair. Returns a
    * ContactInfo populated with the results of the collision analysis. Returns
    * null if there is no collision. mesh0, mesh1 are the candidate colliding
    * surface meshes, which can be the same mesh.
    * 
    * isRigidBodyRigidBody should be specified as true only if mesh0 and mesh1
    * both represent rigid bodies. This controls the type of data returned in
    * ContactInfo.
    */
   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1, boolean isRigidBodyRigidBody) {
      
      // if (renderContours)
      //    addAsRenderable();

      // Collect timing data.
      collisionMetrics.getContactsCalls++;
      long time = System.nanoTime();
      collisionMetrics.totalTime -= time;
      if (!collisionMetrics.started) {
         collisionMetrics.elapsedRealTime = -time;
         collisionMetrics.started = true;
      }
      
//      startTime = System.nanoTime ();
      the = this; // static var used for debugging and performance reporting.
      doEdgeEdgeContactsThisTime = doEdgeEdgeContacts & !isRigidBodyRigidBody;
      this.mesh0 = mesh0;
      this.mesh1 = mesh1;
      boolean intersected = meshIntersector.findContours (mesh0, mesh1);
      contactInfo = null;
      
      if (intersected) {
         for (MeshIntersectionContour c : meshIntersector.contours) {
            for (Vertex3d v1 : c.insideVertices0) {
               for (Face f1 : c.insideFaces1) {
                  if (v1.getMesh() == f1.getMesh()) {
                     throw new RuntimeException ("wrong mesh");
                  }
               }
            }
            for (Vertex3d v1 : c.insideVertices1) {
               for (Face f1 : c.insideFaces0) {
                  if (v1.getMesh() == f1.getMesh()) {
                     throw new RuntimeException ("wrong mesh");
                  }
               }
            }
         }
         contactInfo = new ContactInfo (mesh0, mesh1);
         contactInfo.contours = meshIntersector.contours;
         
         if (!isRigidBodyRigidBody) {
            /*
             * For FemBody to RigidBody, FemBody to FemBody collisions return a
             * set of interpenetrating FEM vertices (over all regions), and the
             * nearest point on opposite mesh, with associate face and
             * barycentric coordinates.
             */
            collisionMetrics.femTime -= System.nanoTime();
            maxVertexFaceDistance = 0;
            if (doEdgeEdgeContactsThisTime) {
               vertexCpps = new Hashtable<Vertex3d,ContactPenetratingPoint>();
            }
            for (MeshIntersectionContour contour : meshIntersector.contours) {
               collideVerticesWithFaces (
                  contactInfo.points0, contour.insideVertices0,
                  contour.insideFaces1);
               collideVerticesWithFaces (
                  contactInfo.points1, contour.insideVertices1,
                  contour.insideFaces0);
            }
            edgeEdgeContacts = contactInfo.edgeEdgeContacts;
            if (doEdgeEdgeContactsThisTime)
               findEdgeEdgeContacts();
            collisionMetrics.femTime += System.nanoTime();
         }
         else {

            /*
             * For RigidBody to RigidBody collisions, the contactInfo will
             * contain: - Set of contact regions, with for each region: - normal
             * based on planar fit, which must point from mesh1 to mesh0
             *  - separation distance - the min distance we need to move along
             * the normal to separate the bodies, which we approximate by taking
             * the maximum of:
             *  - range distance of contour points WRT to the plane
             *  - distance of each internal vertex to the opposite mesh along
             * the normal
             *  - contour points, culled so that points closer than
             * pointTolerance are merged, and *possibly* reduced to a convex
             * hull WRT the plane
             */
            collisionMetrics.rigidTime -= System.nanoTime();
            for (MeshIntersectionContour contour : meshIntersector.contours) {
               if (contour.isClosed & contour.size() > 1) {
                  ContactRegion cr =
                     new ContactRegion (contour, contactInfo, pointTolerance);
                  if (cr.isValid)
                     contactInfo.regions.add (cr);
               }
               ;
            }
            collisionMetrics.rigidTime += System.nanoTime();
         }
         // if (renderContours)
         //    contactInfoRenderer.addContactInfo (contactInfo);
      }
      else {
         // if (renderContours)
         //    contactInfoRenderer.removeContactInfo (mesh0, mesh1);
      }

      collisionMetrics.report (contactInfo);
      lastContactInfo = contactInfo;
      ContactInfo tmp = contactInfo;
      contactInfo = null;
      /*
       * if (mesh0.name.equals("box2") & mesh1.name.equals("box3")){
       * System.out.println(" "); }
       */
      return tmp;
   }

   /*
    * Return true if the edge has no penetrating vertices, or if when the edge's
    * penetrating vertices are moved to their nearest opposing point, the edge
    * will still point inside from an opposing face.
    */
   boolean needsEdgeEdgeCorrection (HalfEdge edge) {
      ContactPenetratingPoint cppTail = vertexCpps.get (edge.getTail());
      ContactPenetratingPoint cppHead = vertexCpps.get (edge.getHead());
      if (cppHead == null & cppTail == null)
         return true;
      Point3d newTailPosition =
         cppTail == null ? edge.getTail().getWorldPoint() : cppTail.position;
      Point3d newHeadPosition =
         cppHead == null ? edge.getHead().getWorldPoint() : cppHead.position;
      if (cppTail != null) {
         nearesttmp.sub (newHeadPosition, cppTail.position);
         if (nearesttmp.dot (cppTail.face.getWorldNormal()) < 0)
            return true;
      }
      if (cppHead != null) {
         nearesttmp.sub (newTailPosition, cppHead.position);
         if (nearesttmp.dot (cppHead.face.getWorldNormal()) < 0)
            return true;
      }
      return false;
   }

   /*
    * Create an edge-edge contact for each unique pair of HalfEdges where - the
    * edges are from opposite regions, - both edges are in need of edge-edge
    * correction - the geometry is suitable (see EdgeEdgeContact.calculate())
    */
   void findEdgeEdgeContacts() {

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

      for (MeshIntersectionContour contour : meshIntersector.contours) {
         for (HalfEdge edge0 : contour.insideEdges0) {
            if (needsEdgeEdgeCorrection (edge0)) {
               for (HalfEdge edge1 : contour.insideEdges1) {
                  if (needsEdgeEdgeCorrection (edge1)) {
                     if (eec.calculate (edge0, edge1)) {
                        // Test the geometry to see if it is a valid edge-edge
                        // contact.
                        edgeEdgeContacts.add (eec);
                        eec = new EdgeEdgeContact (this);
                     }
                  }
               }
            }
         }

      }
   }

   boolean interlocking (MeshIntersectionPoint mipA, MeshIntersectionPoint mipB) {
      if (mipA.edgeRegion == mipB.edgeRegion)
         return false;
      return ((mipA.edge.getFace() == mipB.face |
               mipA.edge.opposite.getFace() == mipB.face) &
              (mipB.edge.getFace() == mipA.face |
               mipB.edge.opposite.getFace() == mipA.face));
   }

   /*
    * For each penetrating vertex, find the closest opposing face and add a
    * corresponding new element to the list of ContactPenetratingPoints
    */
   public void collideVerticesWithFaces (
      ArrayList<ContactPenetratingPoint> cpps,
      ArrayList<Vertex3d> penetratingVertices,
      LinkedHashSet<Face> opposingFaces) {
      ContactPenetratingPoint cpp;
      for (Vertex3d penetratingVertex : penetratingVertices) {
         np.face = null;
         for (Face f : opposingFaces) {
            if (f.getMesh() == penetratingVertex.getMesh())
               throw new RuntimeException ("self intersection");
            Point3d wpnt = penetratingVertex.getWorldPoint();
            double tmpdist =
               getNearestPoint (f, wpnt);
            if (np.face == null | tmpdist < np.dist) {
               np.face = f;
               np.dist = tmpdist;
               np.point.set (nearesttmp);
               np.uv.set (coordstmp);
               np.disp.sub (nearesttmp, wpnt);
            }
         }
         if (np.face == null) {
            throw new InternalErrorException (
               "null face for ContactPenetratingPoint");
         }
         cpp =
            new ContactPenetratingPoint (
               penetratingVertex, np.face, np.uv, np.point, np.disp);
         maxVertexFaceDistance = Math.max (maxVertexFaceDistance, np.dist);
         cpps.add (cpp);
         if (doEdgeEdgeContactsThisTime)
            vertexCpps.put (penetratingVertex, cpp);
      }
   }

   /* Helper class used to find nearest opposing face to a vertex. */
   static class NearestPoint {
      Face face = null;
      Point3d point = new Point3d();
      Vector2d uv = new Vector2d();
      double dist = Double.POSITIVE_INFINITY;
      Vector3d disp = new Vector3d();
   }

   public double getNearestPoint (Face f, Point3d p) {
      Point3d p0, p1, p2;
      p0 = f.getVertex (0).getWorldPoint();
      p1 = f.getVertex (1).getWorldPoint();
      p2 = f.getVertex (2).getWorldPoint();
      return faceIntersector.nearestpoint (p0, p1, p2, p, nearesttmp, coordstmp);
   }

   /*
    * The following code does nothing and is merely for compatibility with the
    * old collision code.
    */
   private double epsilon = 0;
   private double pointTolerance = 0; // min distance between contact points
   private double regionTolerance = 0; // min distance between regions

   public SurfaceMeshCollider() {
      setEpsilon (1e-12);
      setPointTolerance (1e-6);
      setRegionTolerance (1e-2);
   }

   public double getEpsilon() {
      return epsilon;
   }

   public void setEpsilon (double epsilon) {
      this.epsilon = epsilon;
   }

   public double getPointTolerance() {
      return pointTolerance;
   }

   public void setPointTolerance (double tolerance) {
      this.pointTolerance = tolerance;
   }

   public double getRegionTolerance() {
      return regionTolerance;
   }

   public void setRegionTolerance (double regionTolerance) {
      this.regionTolerance = regionTolerance;
   }

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

   public class ContactInfoRenderer implements GLRenderable {

      IdentityHashMap<PolygonalMesh,IdentityHashMap<PolygonalMesh,ContactInfo>> renderableContactInfos =
         new IdentityHashMap<PolygonalMesh,IdentityHashMap<PolygonalMesh,ContactInfo>>();
      public Face face;
      public Face face1;
      public HalfEdge edge;
      public Point3d point;

      void removeAllRenderables() {
         renderableContactInfos.clear(); 
      }

      void removeContactInfo (PolygonalMesh m0, PolygonalMesh m1) {
         ContactInfo ci;
         IdentityHashMap<PolygonalMesh,ContactInfo> rs;
         synchronized (renderableContactInfos) {
            rs = renderableContactInfos.get (m0);
            if (rs != null) {
               ci = rs.get (m1);
               if (ci != null) {
                  if (m0 != ci.mesh0 | m1 != ci.mesh1)
                     throw new RuntimeException ("no such contact info");
                  // System.out.println("removing "+m0.name+" "+m1.name+"
                  // "+ci.contours.size());
                  rs.remove (m1);
               }
            }
         }
      }

      int countInfos() {
         int ans = 0;
         synchronized (renderableContactInfos) {
            for (PolygonalMesh m0 : renderableContactInfos.keySet()) {
               IdentityHashMap<PolygonalMesh,ContactInfo> rs =
                  renderableContactInfos.get (m0);
               if (rs != null) {
                  for (PolygonalMesh m1 : rs.keySet()) {
                     ContactInfo ci = rs.get (m1);
                     if (ci != null) {
                        // System.out.println(" "+m0.name+" "+m1.name+"
                        // "+ci.contours.size());
                        ans += ci.contours.size();
                     }
                  }
               }
            }
         }
         return ans;
      }

      void addContactInfo (ContactInfo info) {
         IdentityHashMap<PolygonalMesh,ContactInfo> rs;
         synchronized (renderableContactInfos) {
            rs = renderableContactInfos.get (info.mesh0);
            if (rs == null) {
               rs = new IdentityHashMap<PolygonalMesh,ContactInfo>();
               renderableContactInfos.put (info.mesh0, rs);
            }
            rs.put (info.mesh1, info);
            // System.out.println("adding "+info.mesh0.name+"
            // "+info.mesh1.name+" "+info.contours.size());
         }
      }

      public void render (GLRenderer renderer, int flags) {
         GL2 gl = renderer.getGL2().getGL2();
         renderer.setLightingEnabled (false);
         gl.glDisable (GL2.GL_LINE_STIPPLE);
         gl.glEnable (GL2.GL_LINE_SMOOTH);
         gl.glLineWidth (6.0f);
         synchronized (renderableContactInfos) {
            for (PolygonalMesh m0 : renderableContactInfos.keySet()) {
               IdentityHashMap<PolygonalMesh,ContactInfo> rs;
               rs = renderableContactInfos.get (m0);
               for (PolygonalMesh m1 : rs.keySet()) {
                  // renderMeshNormals(renderer, m0);
                  // renderMeshNormals(renderer, m1);
                  rs.get (m1).render (renderer, flags);
               }
            }
         }
         if (edge != null) {
            renderer.setColor (1, 1, 1f);
            gl.glLineWidth (26.0f);
            gl.glBegin (GL2.GL_LINES);
            Point3d p = edge.tail.getWorldPoint();
            gl.glVertex3d (p.x, p.y, p.z);
            p = edge.head.getWorldPoint();
            gl.glVertex3d (p.x, p.y, p.z);
            gl.glEnd();
         }
         if (face != null) {
            renderer.setColor (1, 0.2f, 0.6f);
            renderFace (renderer, face);
         }
         if (face1 != null) {
            renderer.setColor (0.5f, 0.2f, 0.9f);
            renderFace (renderer, face1);
         }

         if (point != null) {
            renderer.setColor (1, 1, 1f);
            gl.glPointSize (40);
            gl.glBegin (GL2.GL_POINTS);
            gl.glVertex3d (point.x, point.y, point.z);
            gl.glEnd();
            gl.glPointSize (1);
         }
         gl.glLineWidth (1.0f);
         renderer.setLightingEnabled (true);
      }

      void renderFace (GLRenderer renderer, Face aFace) {
         GL2 gl = renderer.getGL2().getGL2();
         gl.glLineWidth (26.0f);
         gl.glBegin (GL2.GL_LINE_LOOP);
         Point3d p = aFace.getEdge (0).tail.getWorldPoint();
         gl.glVertex3d (p.x, p.y, p.z);
         p = aFace.getEdge (0).head.getWorldPoint();
         gl.glVertex3d (p.x, p.y, p.z);
         p = aFace.getEdge (1).head.getWorldPoint();
         gl.glVertex3d (p.x, p.y, p.z);
         gl.glEnd();
      }

      void renderMeshNormals (GLRenderer renderer, PolygonalMesh mesh) {
         GL2 gl = renderer.getGL2().getGL2();
         Point3d p = new Point3d();
         gl.glLineWidth (20f);
         gl.glBegin (GL2.GL_LINES);
         for (Face f : mesh.getFaces()) {
            renderer.setColor (1, 1, 0.5f);
            f.computeWorldCentroid (p);
            gl.glVertex3d (p.x, p.y, p.z);
            p.scaledAdd (0.3, f.getWorldNormal());
            gl.glVertex3d (p.x, p.y, p.z);
         }
         gl.glEnd();
         gl.glLineWidth (1);
      }

      public int getRenderHints() {
         return 0;
      }

      public void prerender (RenderList list) {
      }

      public void updateBounds (Point3d pmin, Point3d pmax) {
      }
   }

}
