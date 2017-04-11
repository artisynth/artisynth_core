/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVIntersector;
import maspack.geometry.BVNode;
import maspack.geometry.Boundable;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.Intersector2d;
import maspack.geometry.OBBTree;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;

/**
 * A basic display probe that shows the intersection of a rigid body
 * with the display probe's plane
 * 
 * @author Antonio
 *
 */
public class MeshIntersectingProbe extends CutPlaneProbe {
   
   protected static boolean myDefaultDrawIntersections = false;
   protected static double myDefaultTolerance = 1e-10;   // used for intersections
   
   protected boolean clipped = false;
   protected boolean drawIntersections = myDefaultDrawIntersections;
   protected static double myIntersectionTolerance = myDefaultTolerance;
   
   protected PolygonalMesh myIntersectingMesh = null;
   protected HashMap<Vertex3d,Boolean> vtxIndicatorMap = 
      new HashMap<Vertex3d,Boolean>(myPlaneSurface.numVertices());
   protected ArrayList<LinkedList<Point3d>> myIntersections = null;
   
   public static PropertyList myProps =
      new PropertyList (MeshIntersectingProbe.class,CutPlaneProbe.class);
   
   static {
      myProps.add( "drawIntersections", "draw intersection with mesh", myDefaultDrawIntersections);
      myProps.add( "clipped isClipped clip", "clip to intersection mesh", false);
   }
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public MeshIntersectingProbe() {
      super();
   }
   
   /**
    * Creates a probe with a display plane
    * @param center centre of plane
    * @param orientation orientation of plane (originally x-y)
    * @param size size of plane
    */
   public MeshIntersectingProbe(Point3d center, AxisAngle orientation, Vector2d size) {
      super(center,orientation, size);
   }
      
   public MeshIntersectingProbe (PolygonalMesh mesh) {
      setIntersectingMesh(mesh);
   }
   
   /**
    * Sets the mesh to compute intersections with
    */
   public void setIntersectingMesh(PolygonalMesh mesh) {
      myIntersectingMesh = mesh;
      updateMeshDisplay();
   }
   
   /**
    * Gets the associated mesh
    */
   public PolygonalMesh getIntersectingMesh() {
      return myIntersectingMesh;
   }
   
   /**
    * Enable/disable drawing of intersections of mesh with the plane
    */
   public void setDrawIntersections(boolean enable) {
      if (drawIntersections != enable) {
         drawIntersections = enable;
         if (drawIntersections) {
            updateIntersections();
         }
      }
   }
   
   /**
    * Returns whether we are drawing intersections with the plane
    */
   public boolean getDrawIntersections() {
      return drawIntersections;
   }
   
   /**
    * Returns whether we are clipped to a mesh
    */
   public boolean isClipped() {
      return clipped;
   }
   
   /**
    * Clips the display plane to lie solely within the associated mesh
    * @param enable if <code>true</code>, enables display plane clipping
    */
   public void clip(boolean enable) {
      
      if (clipped == enable) {
         return;  // prevent excessive setting
      }
      
      if (clipped == false) {
         clipToMesh();
      } else {
         rebuildMesh();
      }
      updateMeshDisplay();
      clipped = enable;
   }
   
   @Override
   protected void rebuildMesh() {
      super.rebuildMesh();
      clipped = false;
   }
   
   /**
    * Updates the display orientation and indicator colors
    */
   @Override
   protected void updateMeshDisplay() {
      super.updateMeshDisplay();      
      
      if (myIntersectingMesh != null) {
         updateVertexIndicators();
         if (drawIntersections) {
            updateIntersections();
         }
      }
   }
   
   /**
    * Computes vertex indicators, determining if vertex is inside or outside mesh
    */
   protected void updateVertexIndicators() {
      
      BVFeatureQuery query = new BVFeatureQuery();
      vtxIndicatorMap = new HashMap<Vertex3d,Boolean>(myPlaneSurface.numVertices());
      if (myIntersectingMesh != null) {
         for (Vertex3d vtx : myPlaneSurface.getVertices()) {
            boolean inside = query.isInsideOrientedMesh (
               myIntersectingMesh, vtx.getWorldPoint(), 1e-10);
            if (inside) {
               vtxIndicatorMap.put(vtx, true);
            } else {
               vtxIndicatorMap.put(vtx, false);
            }
         }
      }
      
   }
   
   /**
    * Computes intersection of the mesh with a plane
    */
   protected void updateIntersections() {
      if (myIntersectingMesh != null) {
         getPlane(myPlane);
         BVIntersector intersector = new BVIntersector();
         myIntersections = intersector.intersectMeshPlane (
            myIntersectingMesh, myPlane, myIntersectionTolerance);
      } 
   }
   
   @Override
   public void render (Renderer renderer, int flags) {
     
      if (myPlaneSurface != null) {
         if (drawIntersections) {
            for (LinkedList<Point3d> contour : myIntersections) {
               drawContour(contour, renderer, false);
            }
         }
      }    
      if (isSelected()) {
         drawBoundary(renderer, true);
      }
      
   }
   
   /**
    *  clips the plane mesh to lie within the designated surface
    */
   protected void clipToMesh() {
      // start at one vertex, follow contour
      //    hit edge, corner, next point on contour, or edge
      //      recursive?  each vertex should be visited from both sides (except endpoints)
      
      rebuildMesh();
      updateIntersections();
      
      for (LinkedList<Point3d> contour : myIntersections) {
         clipMesh(myPlaneSurface, contour, 1e-10);
      }
      updateVertexIndicators();
      myPlaneSurface = trimFaces(myPlaneSurface, vtxIndicatorMap);
      myPlaneSurface.setMeshToWorld(XGridToWorld);
      mySurfaceBoundaries = extractBoundaries(myPlaneSurface);
      
   }  
   
   protected static PolygonalMesh trimFaces(PolygonalMesh mesh, HashMap<Vertex3d, Boolean> vtxIndicatorMap) {
      
      PolygonalMesh out = new PolygonalMesh();
      HashMap<Vertex3d, Vertex3d> vtxMap = new HashMap<Vertex3d,Vertex3d>(mesh.numVertices());
      
      for (Vertex3d vtx : mesh.getVertices()) {
         if (vtxIndicatorMap.get(vtx)) {
            Vertex3d nvtx = new Vertex3d(new Point3d(vtx.getPosition()));
            vtxMap.put(vtx, nvtx);
            out.addVertex(nvtx);
         }
      }
      
      for (Face face : mesh.getFaces()) {
         boolean add = true;
         for (Vertex3d vtx : face.getVertices()) {
            if (vtxIndicatorMap.get(vtx) == false) {
               add = false;
               break;
            }
         }
         if (add) {
            Vertex3d [] oldVtxs = face.getVertices();
            Vertex3d [] vtxs = new Vertex3d[face.numVertices()];
            for (int i=0; i<vtxs.length; i++) {
               vtxs[i] = vtxMap.get(oldVtxs[i]);
            }
            out.addFace(vtxs);
         }
      }
      
      return out;
   }

   private static class SplitStorage {
      public Face face = null;
      public Vertex3d vtx = null;
      public int idx = 0;
   }
   
   // does the clipping of a planar surface based on a contour
   private void clipMesh(PolygonalMesh surface, LinkedList<Point3d> contour, double tol) {
      
      SplitStorage info = new SplitStorage();
      
      Intersector2d ti = new Intersector2d();
      ti.setEpsilon(tol);
      
      // coordinate system
      Vector3d vx = new Vector3d();
      Vector3d vy = new Vector3d();
      Point3d o = getPosition();
      XGridToWorld.R.getColumn(0, vx);
      XGridToWorld.R.getColumn(1, vy);
      
      OBBTree obbt = new OBBTree (surface, 2, tol);
      
      ArrayList<Face> faceList = null;
      
      while (info.idx+1 < contour.size()) {
         
         if (info.vtx == null) {
            
            Face face = findNextFace(contour.get(info.idx), 
               contour, obbt, ti, vx, vy, o, info);
            
            // so now I have a vertex and a face it lies on
            if (face != null) {
               // we may be on multiple faces
               faceList = findFaces(info.vtx.getWorldPoint(), obbt, 
                  vx, vy, o, tol);
               for (Face f : faceList) {
                  splitFace(surface,f,info.vtx, tol);   
               }
               
            }

         } else {
            // find all faces this vertex is on, project in direction of contour
            faceList = getFaces(info.vtx);
            HalfEdge he = findNextEdge(contour, faceList, ti, info, vx, vy, o);
            
            if (he != null) {
               // we landed on an edge
               Face oppFace = null;
               if (he.opposite != null) {
                  oppFace = he.opposite.getFace();
               } 
               
               splitFace(surface, he.getFace(), info.vtx, tol);
               if (oppFace != null) {
                  splitFace(surface, oppFace, info.vtx, tol);
               } 
               
            } else if (info.face != null) {
               splitFace(surface, info.face, info.vtx, tol);
            } else {
               info.idx++; // move to next point
               info.vtx = null;
            }
            
            
         }
      }
      
   }
   
   /**
    * Returns a list of all faces that use this vertex
    */
   public ArrayList<Face> getFaces(Vertex3d vtx) {
      Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges();
      ArrayList<Face> faces = new ArrayList<Face>();
      
      while (hit.hasNext()) {
         HalfEdge he = hit.next();
         faces.add(he.getFace());
      }
      return faces;
   }
   
   // finds faces near a points
   private ArrayList<Face> findFaces(Point3d p, OBBTree obbt, Vector3d vx, Vector3d vy, Point3d o, double tol) {
      
      ArrayList<Face> faces = new ArrayList<Face>();
      
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      obbt.intersectPoint(nodes, p);
      
      Point2d p2d = Intersector2d.get2dCoordinate(p, vx, vy, o);
      Point3d uvw = new Point3d();
      
      for (BVNode obbn : nodes) {
         for (int i=0; i<obbn.getNumElements(); i++) {
            Boundable ps = obbn.getElements()[i];
            if (ps instanceof Face) {
               Face f = (Face)ps;
               Vertex3d[] vtxs = f.getVertices();

               Point2d p0 = Intersector2d.get2dCoordinate(
                  vtxs[0].getWorldPoint(), vx, vy, o);
               Point2d p1 = Intersector2d.get2dCoordinate(
                  vtxs[1].getWorldPoint(), vx, vy, o);
               Point2d p2 = Intersector2d.get2dCoordinate(
                  vtxs[2].getWorldPoint(), vx, vy, o);

               Intersector2d.getBarycentric(p2d, p0, p1, p2, uvw);
               if (uvw.x > -tol && uvw.y > -tol && uvw.z > -tol) {
                  faces.add(f);
               }
            }
         }
      }
      
      return faces;
   }
   
   // NOTE: only works for convex faces (so, triangular is okay)
   // finds the next edge while clipping around in a circle
   private static HalfEdge findNextEdge(LinkedList<Point3d> contour, ArrayList<Face> faceList, Intersector2d ti, SplitStorage info, 
      Vector3d vx, Vector3d vy, Point3d o) {
      
      Vector3d dir = new Vector3d();
      Point3d vtxp = info.vtx.getWorldPoint();
      Point2d vtx2d = Intersector2d.get2dCoordinate(vtxp, vx, vy, o);
      dir.sub(contour.get(info.idx+1), contour.get(info.idx));
      
      for (Face face : faceList) {
         HalfEdge he0 = face.getEdge(0);
         HalfEdge he = he0;
         do {
            if (info.vtx != he.head && info.vtx != he.tail) {
               
               Point2d p1 = Intersector2d.get2dCoordinate(he.head.getWorldPoint(), vx, vy, o);
               Point2d p2 = Intersector2d.get2dCoordinate(he.tail.getWorldPoint(), vx, vy, o);
               ArrayList<Point2d> pnts = new ArrayList<Point2d>();
               Vector2d lineDir = Intersector2d.get2dVector(dir, vx, vy);
               
               int npoints = ti.intersectLineLineSegment(vtx2d, lineDir, p1, p2, pnts);
               
               if (npoints == 1) {
                  Point3d p = Intersector2d.get3dCoordinate(pnts.get(0), vx, vy, o);
                  Vector3d ldir = new Vector3d(p.x-vtxp.x,p.y-vtxp.y,p.z-vtxp.z);
                  
                  // check direction
                  if (ldir.dot(dir) > -ti.epsilon) {
                     // check if we passed the next point
                     Point3d pNext = contour.get(info.idx+1);
                     if (ldir.norm() < pNext.distance(vtxp)+ti.epsilon) {
                        
                        if (p.distance(pNext)<ti.epsilon) {
                           info.idx++; // move to next point
                        }
                        info.vtx = createOrGetVertex(p, face.getVertices(), ti.epsilon);
                        return he;
                     } else {
                        // advance to next vertex
                        info.vtx = createOrGetVertex(pNext, face.getVertices(), ti.epsilon);
                        info.face = face;
                        info.idx++; // move to next point
                        return null;
                     }
                     
                  }
                  
               }               
            }
            he = he.getNext();
         } while (he != he0);
         
      }
      info.face = null;
      return null;
   }
   
   // either creates a new vertex at pos, or takes the closest one from vtxs
   private static Vertex3d createOrGetVertex(Point3d pos, Vertex3d[] vtxs, double tol) {
      //  check if we have to make a new vertex
      for (Vertex3d vtx : vtxs) {
         if (vtx.getWorldPoint().distance(pos) < tol) {
            return vtx;
         }
      }
      
      return new Vertex3d(new Point3d(pos));
   }
   
   // splits a face at a point
   private static ArrayList<Face> splitFace(PolygonalMesh mesh, Face face, Vertex3d vtx, double tol) {
      ArrayList<Face> newFaces = new ArrayList<Face>();
      
      Vertex3d[] verts = face.getVertices();
      
      // if the vtx is one of the corners, don't do anything
      for (Vertex3d v : verts) {
         if (v == vtx) {
            newFaces.add(face);
            return newFaces;
         }
      }
      
      if (!mesh.containsVertex(vtx)) {
         // transform to mesh coordinate system,
         // then add
         vtx.pnt.inverseTransform(mesh.getMeshToWorld());
         mesh.addVertex(vtx);
      }
      mesh.removeFace(face);
      
      Vertex3d v1,v2,v3;
      v1 = vtx;
      for (int i=0; i<verts.length; i++) {
         int j = (i+1)%verts.length;
         v2 = verts[i];
         v3 = verts[j];
         
         if (getSignedTriangleArea(v1.getWorldPoint(), 
            v2.getWorldPoint(), 
            v3.getWorldPoint(), face.getWorldNormal()) > tol) {
            newFaces.add(mesh.addFace(v1, v2, v3));
         }
      }
      
      return newFaces;
      
   }
   
   // 3D signed triangle area based on supplied normal
   protected static double getSignedTriangleArea(Point3d p1, Point3d p2, Point3d p3, Vector3d n) {
      
      // [(p3-p1)x(p3-p2)]/2, length gives area, direction gives normal
      // dot with n to get sign
      
      double x = ((p3.y-p1.y)*(p3.z-p2.z)-(p3.z-p1.z)*(p3.y-p2.y));
      double y = ((p3.z-p1.z)*(p3.x-p2.x)-(p3.x-p1.x)*(p3.z-p2.z));
      double z = ((p3.x-p1.x)*(p3.y-p2.y)-(p3.y-p1.y)*(p3.x-p2.x));
      
      double d = Math.sqrt((x*x+y*y+z*z))/2;
      if (n.x*x+n.y*y+n.z*z < 0) {
         d = -d;
      }
      
      return d;
   }
   
   // does not add the vertex, goes in a loop around contour until we hit a face
   private Face findNextFace(Point3d pnt, LinkedList<Point3d> contour, OBBTree obbt, 
      Intersector2d ti, Vector3d vx, Vector3d vy, Point3d o, SplitStorage info) {
      
      Face face = null;
      Vector3d dir = new Vector3d();
      if (info.idx <contour.size()-1) {
         dir.sub(contour.get(info.idx+1), contour.get(info.idx));
      } else {
         dir.sub(contour.get(info.idx), contour.get(info.idx-1));
      }
      dir.normalize();
      
      while (face == null && info.idx < contour.size()-1) {
       
         Point3d pntNext = contour.get(info.idx+1);
         Point2d pnt2d = Intersector2d.get2dCoordinate(pnt, vx, vy, o);
         Point2d pnt2dNext = Intersector2d.get2dCoordinate(
            pntNext, vx, vy, o);
         Vector2d diff2d = new Vector2d();
         
         ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(); 
         obbt.intersectLineSegment(bvNodes, pnt, pntNext);  // get close nodes
         double minDist = Double.MAX_VALUE;
         Point2d minPnt = null;
         
         for (BVNode node : bvNodes) {
            for (int i=0; i<node.getNumElements(); i++) {
               Boundable ps = node.getElements()[i];
               if (ps instanceof Face) {
                  Face f = (Face)ps;

                  Vertex3d[] vtxs = f.getVertices();

                  Point2d p0 = Intersector2d.get2dCoordinate(
                     vtxs[0].getWorldPoint(), vx, vy, o);
                  Point2d p1 = Intersector2d.get2dCoordinate(
                     vtxs[1].getWorldPoint(), vx, vy, o);
                  Point2d p2 = Intersector2d.get2dCoordinate(
                     vtxs[2].getWorldPoint(), vx, vy, o);


                  ArrayList<Point2d> points = new ArrayList<Point2d>();
                  ti.intersectTriangleLineSegment(p0, p1, p2, pnt2d, pnt2dNext, points);

                  // check points
                  for (Point2d p : points) {
                     diff2d.sub(p, pnt2d);
                     if (diff2d.norm() < minDist) {
                        face = f;
                        minDist = diff2d.norm();
                        minPnt = p;
                     }
                  }
               }
            }
         }
         
         if (face == null || minDist >=  pnt2dNext.distance(pnt2d)) {
            face = null;
            
            // move to next point
            info.idx++;
            pnt = contour.get(info.idx);
            if (info.idx < contour.size()-1) {
               dir.sub(contour.get(info.idx+1), pnt);
            }
         } else {
            
            // snap to edge if within tolerance
            Point3d pos = Intersector2d.get3dCoordinate(minPnt, vx, vy, o); 
            
            
            if (pos.distance(pnt) < ti.epsilon) {
               pos.set(pnt);
            }
            
            // check if we have to make a new vertex
            info.vtx = createOrGetVertex(pos, face.getVertices(), ti.epsilon);
         }
      }
      
      return face;
   }
   
}
