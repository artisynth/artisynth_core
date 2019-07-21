/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.Iterator;

import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.util.DoubleHolder;

/**
 * Worker class for nearest feature queries using distance/signed distance fields
 */
public class DistanceGridFeatureQuery {

   TriangleIntersector myIntersector;
   Vector3d myTmp1;
   Vector3d myTmp2;
   Face lastFace;
   Point3d lastNear;
   Vector2d lastUV;
   
   public enum InsideQuery {
      INSIDE,
      ON,
      OUTSIDE
   }

   public DistanceGridFeatureQuery() {
      myTmp1 = new Vector3d();
      myTmp2 = new Vector3d();
   }
   
   private static DistanceGrid getOrCreateFaceGrid(PolygonalMesh mesh) {
      DistanceGrid sdGrid = mesh.getSignedDistanceGrid();
      if (sdGrid == null) {
         Vector3i cellDivisions = new Vector3i (20, 20, 20);
         double gridMargin = 0.1;
         sdGrid = mesh.getSignedDistanceGrid (gridMargin, cellDivisions);
      }
      return sdGrid;
   }

   /**
    * Returns the nearest triangular mesh face to a point. This method
    * uses the default signed distance field produced by the mesh.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param uv if not <code>null</code>, returns the UV coordinates
    * of the nearest face point. These are the barycentric coordinates
    * with respect to the second and third vertices.
    * @param mesh mesh containing the faces.
    * @param pnt point for which the nearest face should be found.
    * @return the nearest face to the point, or <code>null</code>
    * if the mesh contains no faces.
    */
   public Face nearestFaceToPoint (
      Point3d nearPnt, Vector2d uv, PolygonalMesh mesh, Point3d pnt) {

      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      DistanceGrid sdgrid = getOrCreateFaceGrid(mesh);
      Face face = nearestFaceToPoint (nearPnt, uv, sdgrid, pnt);
      return face;
   }

   /**
    * Find the nearest feature to a point
    * @param nearPnt populates with location of nearest point on set of features
    * @param featureGrid distance grid containing desired features
    * @param pnt point from which to find the nearest feature
    * @return the nearest feature, or null
    */
   public Feature nearestFeatureToPoint(Point3d nearPnt, DistanceGrid featureGrid, Point3d pnt) {
      if (nearPnt == null) {
         nearPnt = new Point3d();
      }
      return featureGrid.getNearestWorldFeature(nearPnt, pnt);
   }

   public boolean debug = false;

   /**
    * Returns the nearest triangular face to a point, using a specified
    * distance grid. The faces contained within the grid are
    * all assumed to be triangular.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param uv if not <code>null</code>, returns the UV coordinates
    * of the nearest face point. These are the barycentric coordinates
    * with respect to the second and third vertices.
    * @param sdgrid distance grid containing the faces.
    * @param pnt point for which the nearest face should be found.
    * @return the nearest face to the point, or <code>null</code>
    * if <code>sdgrid</code> contains no faces.
    */
   public Face nearestFaceToPoint (
      Point3d nearPnt, Vector2d uv, DistanceGrid sdgrid, Point3d pnt) {

      if (nearPnt == null) {
         nearPnt = new Point3d();
      }
      Feature feature = sdgrid.getNearestWorldFeature(nearPnt, pnt);
      
      if (feature != null && feature instanceof Face) {
         // compute barycentric
         Face face = (Face)feature;
         if (uv != null) {
            // local point
            Point3d lnear = nearPnt;
            if (sdgrid.hasLocalToWorld()) {
               lnear = new Point3d(nearPnt);
               lnear.inverseTransform(sdgrid.getLocalToWorld());
            }
            face.computeCoords(lnear, uv);
         }
         return face;
      }
      
      return null;
   }

   /**
    * Returns the nearest triangular mesh face to a point. This method
    * uses the default distance grid produced by the mesh.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param uv if not <code>null</code>, returns the UV coordinates
    * of the nearest face point. These are the barycentric coordinates
    * with respect to the second and third vertices.
    * @param mesh mesh containing the faces.
    * @param pnt point for which the nearest face should be found.
    * @return the nearest face to the point, or <code>null</code>
    * if the mesh contains no faces.
    */
   public static Face getNearestFaceToPoint (
      Point3d nearPnt, Vector2d uv, PolygonalMesh mesh, Point3d pnt) {
      DistanceGridFeatureQuery query = new DistanceGridFeatureQuery();
      DistanceGrid sdgrid = getOrCreateFaceGrid(mesh);
      return query.nearestFaceToPoint (nearPnt, uv, sdgrid, pnt);
   }

   /**
    * Returns true if a point is on or inside an oriented triangular
    * mesh. "Oriented" means that all face normals are assumed to point
    * outwards.  Whether or not the point is on the mesh is determined using a
    * numerical tolerance computed from the meshes overall dimensions.
    *
    * <p> The method works by inspecting the nearest face, edge or vertex to
    * the point. Hence the mesh does not need to be closed, and the method is
    * faster, though possibly less numerically robust, than {@link
    * #isInsideMesh(PolygonalMesh,Point3d)}, which uses ray casting.
    *
    * @param mesh mesh which point may be inside.
    * @param pnt point to check.
    * @return true if <code>pnt</code> is on or inside the mesh.
    */
   public static boolean isInsideOrientedMesh (
      PolygonalMesh mesh, Point3d pnt) {
      DistanceGridFeatureQuery query = new DistanceGridFeatureQuery();
      return query.isInsideOrientedMesh (getOrCreateFaceGrid(mesh), pnt, -1);
   }

   /**
    * Returns true if a point is on or inside an oriented triangular
    * mesh. "Oriented" means that all face normals are assumed to point
    * outwards. The method works by inspecting the nearest face, edge or vertex
    * to the point.
    *
    * <p> The method works by inspecting the nearest face, edge or vertex to
    * the point. Hence the mesh does not need to be closed, and the method is
    * faster, though possibly less numerically robust, than {@link
    * #isInsideMesh(PolygonalMesh,Point3d,double)}, which uses ray casting.
    *
    * @param mesh mesh which point may be inside.
    * @param pnt point to check (in world coordinates)
    * @param tol tolerance within which the point is considered to be on the
    * mesh surfaces. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return true if <code>pnt</code> is on or inside the mesh.
    */
   public boolean isInsideOrientedMesh (
      PolygonalMesh mesh, Point3d pnt, double tol) {
      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      return isInsideOrientedMesh (getOrCreateFaceGrid(mesh), pnt, tol);
   }

   private boolean vertexPointsOutward (Vertex3d vtx) {
      Vector3d fnrm = new Vector3d();
      Vector3d enrm = new Vector3d();
      Vector3d u = new Vector3d();

      vtx.computeAngleWeightedNormal (fnrm);
      int nume = 0;
      
      for (Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges(); hit.hasNext();){
         HalfEdge he = hit.next();
         u.sub (he.head.pnt, he.tail.pnt);
         u.normalize();
         enrm.add (u);
         nume++;
         he = he.getNext().opposite;
      }
      enrm.scale (1.0/nume);
      return enrm.dot (fnrm) > 0;
   }

   public String lastCase = null;

   int numEdgeCases;
   int numVertexCases;
   int numFaceCases;

   private final int SGN (double x) {
      if (x == 0) {
         return 0;
      }
      else {
         return x > 0 ? 1 : -1;
      }
   }

   /**
    * Returns true if a point is on or inside an oriented triangular mesh, the
    * faces of which are contained within a specified distance grid. 
    * "Oriented" means that all face normals are assumed to point
    * outwards.
    *
    * <p> The method works by inspecting the nearest face, edge or vertex to
    * the point. Hence the mesh does not need to be closed, and the method is
    * faster, though possibly less numerically robust, than {@link
    * #isInsideMesh(PolygonalMesh,DistanceGrid,Point3d,double)}.
    *
    * @param grid distance grid containing the faces.
    * @param pnt point to check (in world coordinates)
    * @param tol tolerance within which the point is considered to be on the
    * mesh surface. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return true if <code>pnt</code> is on or inside the mesh.
    */
   public boolean isInsideOrientedMesh (
      DistanceGrid grid, Point3d pnt, double tol) {
      Point3d lpnt;
      if (grid.hasLocalToWorld()) {
         lpnt = new Point3d (pnt);
         lpnt.inverseTransform (grid.getLocalToWorld());
      }
      else {
         lpnt = pnt;
      }
      if (tol < 0) {
         tol = 1e-12*grid.getRadius();
      }

      lastFace = null;
      lastNear = null;
      lastUV = null;
      
      double d = grid.getLocalDistance(lpnt);
      if (d == DistanceGrid.OUTSIDE_GRID) {
         lastCase = "Culled";
         return false;
      }
      
      
      Point3d nearest = new Point3d();
      Feature f = grid.getNearestLocalFeature(nearest, lpnt);
      if (f == null || !(f instanceof Face)) {
         // must be no faces in the mesh
         lastCase = "No Faces";
         return false;
      }
      
      Face face = (Face)f;
      Vector3d diff = new Vector3d();
      Vector2d uv = new Vector2d();
      face.computeCoords(nearest, uv);
      
      // diff is the vector from the nearest face point to pnt, in face coords
      diff.sub (lpnt, nearest);
      if (diff.norm() <= tol) {
         lastCase = "Tol";
         return true;
      }

      // w0, w1, w2 are the barycentric weights of the intersection point
      // with respect to the face vertices
      double w0 = 1 - uv.x - uv.y;
      double w1 = uv.x;
      double w2 = uv.y;

      // tolerence to determine if the intersection point is near an edge or
      // vertex
      double eps = 1e-12;

      Vertex3d nearVertex = null;
      HalfEdge nearEdge = null;

      if (Math.abs (w0-1) < eps) {
         // intersection is near vertex 0
         nearVertex = face.getVertex(0);
      }
      else if (Math.abs (w1-1) < eps) {
         // intersection is near vertex 1
         nearVertex = face.getVertex(1);
      }
      else if (Math.abs (w2-1) < eps) {
         // intersection is near vertex 2
         nearVertex = face.getVertex(2);
      }
      else if (Math.abs (w0) < eps) {
         // intersection is near edge 1-2
         nearEdge = face.getEdge (2);
      }
      else if (Math.abs (w1) < eps) {
         // intersection is near edge 0-2
         nearEdge = face.getEdge (0);
      }
      else if (Math.abs (w2) < eps) {
         // intersection is near edge 0-1
         nearEdge = face.getEdge (1);
      }

      boolean inside;
   
      if (nearVertex != null) {
         numVertexCases++;
         
         Iterator<HalfEdge> it = nearVertex.getIncidentHalfEdges();
         HalfEdge he = it.next();
         double dot = diff.dot(he.getFace().getNormal());
         int uniformSign = SGN(dot);
         while (uniformSign != 0 && it.hasNext()) {
            he = it.next();
            dot = diff.dot(he.getFace().getNormal());          
            if (uniformSign != SGN(dot)) {
               uniformSign = 0;
            }
         }

         // HalfEdgeNode node = nearVertex.incidentHedges;
         // double dot = diff.dot(node.he.face.getNormal());
         // int uniformSign = SGN(dot);
         // node = node.next;
         // while (uniformSign != 0 && node != null) {
         //    dot = diff.dot(node.he.face.getNormal());          
         //    if (uniformSign != SGN(dot)) {
         //       uniformSign = 0;
         //    }
         //    node = node.next;
         // }

         if (uniformSign != 0) {
            lastCase = "Vertex uniform sign check";
            inside = (uniformSign < 0);
         }
         else {
            lastCase = "Vertex outward pointing check";
            inside = !vertexPointsOutward (nearVertex);
         }
      }
      else if (nearEdge != null && nearEdge.opposite != null) {
         numEdgeCases++;
         Face oppface = nearEdge.opposite.getFace();
         double dot0 = diff.dot(face.getNormal());
         double dot1 = diff.dot(oppface.getNormal());
         if (Math.abs(dot0) > Math.abs(dot1)) {
            inside = dot0 < 0;
         }
         else {
            inside = dot1 < 0;
         }
         lastCase = "Edge";
      }
      else {
         numFaceCases++;
         inside = diff.dot(face.getNormal()) < 0;
         lastCase = "Face";
      }
      
      // save in case of query in getFaceForInsideOrientedTest
      lastFace = face;
      lastNear = new Point3d(nearest);
      lastUV = uv;
      
      return inside;
   }      

   /**
    * If called immediately after either
    * {@link #isInsideOrientedMesh(PolygonalMesh,Point3d,double)} or
    * {@link #isInsideOrientedMesh(DistanceGrid,Point3d,double)},
    * returns the nearest face
    * that was used to resolve whether or not the query point is actually
    * inside the mesh. If the point is outside the mesh's distance grid, then
    * no nearest face will have been computed and this method will return
    * <code>null</code>.
    *
    * @param nearLoc if not <code>null</code>, and if the returned face is not
    * <code>null</code>, returns the nearest point (to the query point) on the
    * face in mesh local coordinates.
    * @param uv if not <code>null</code>, and if the returned face is not
    * <code>null</code>, returns the UV coordinates of the nearest point on the
    * face. These are the barycentric coordinates with respect to the second
    * and third vertices.
    * @return the nearest face, if any, used to resolve whether 
    * the query point is actually inside the mesh.
    */
   public Face getFaceForInsideOrientedTest (Point3d nearLoc, Vector2d uv) {
      if (lastFace == null) {
         return null;
      }
      else {
         if (nearLoc != null) {
            nearLoc.set (lastNear);
         }
         if (uv != null) {
            uv.set (lastUV);
         }
         return lastFace;
      }
   }

   /**
    * Determines if a point is on or inside a closed triangular mesh. The mesh
    * normals are assumed to be pointing outwards.  The method works using
    * a signed distance grid, which does a series of ray-casts to determine
    * inside/outside distance values.
    * 
    * @param mesh mesh which point may be inside.
    * @param pnt point to check.
    * @return InsideQuery.INSIDE if <code>pnt</code> is inside, InsideQuery.ON if on,
    *  InsideQuery.OUTSIDE if it is outside.
    */
   public static InsideQuery isInsideMesh (
      PolygonalMesh mesh, Point3d pnt) {
      DistanceGridFeatureQuery query = new DistanceGridFeatureQuery();
      return query.isInsideMesh (mesh, getOrCreateFaceGrid(mesh), pnt, -1);
   }

   /**
    * Determines if a point is on or inside a closed triangular mesh. The mesh
    * normals are assumed to be pointing outwards.  The method works using
    * a signed distance grid, which does a series of ray-casts to determine
    * inside/outside distance values.
    *
    * @param mesh mesh which point may be inside.
    * @param pnt point to check.
    * @param tol tolerance within which the point is considered to be on the
    * mesh surface. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return InsideQuery.INSIDE if <code>pnt</code> is inside, InsideQuery.ON if on,
    *  InsideQuery.OUTSIDE if it is outside.
    */
   public InsideQuery isInsideMesh (PolygonalMesh mesh, Point3d pnt, double tol) {
      return isInsideMesh (mesh,getOrCreateFaceGrid(mesh), pnt, tol);
   }

   /**
    * Determines if a point is on or inside a closed triangular mesh, the faces
    * of which are contained within a specified signed distance grid. The method works using
    * a signed distance grid, which does a series of ray-casts to determine
    * inside/outside distance values.
    *
    * @param mesh mesh which point may be inside.
    * @param sdgrid signed distance grid containing the mesh.
    * @param pnt point to check.
    * @param tol tolerance within which the point is considered to be on the
    * mesh surface. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return InsideQuery.INSIDE if <code>pnt</code> is inside or on,
    *  InsideQuery.OUTSIDE if it is outside.
    */
   public InsideQuery isInsideMesh(
      PolygonalMesh mesh, DistanceGrid sdgrid, Point3d pnt, double tol) {
      InsideQuery q = isInsideOrOnMesh(mesh, sdgrid, pnt, tol);
      if (q == InsideQuery.ON) {
         return InsideQuery.INSIDE;
      }
      return q;
   }
   
   public InsideQuery isInsideOrOnMesh (
      PolygonalMesh mesh, DistanceGrid sdgrid, Point3d pnt, double tol) {

      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      if (!mesh.isClosed()) {
         throw new IllegalArgumentException ("mesh is not closed");
      }
      if (myIntersector == null) {
         myIntersector = new TriangleIntersector();
      }
      Point3d lpnt;
      if (tol < 0) {
         tol = 1e-12*sdgrid.getRadius();
      }

      if (sdgrid.hasLocalToWorld()) {
         lpnt = new Point3d (pnt);
         lpnt.inverseTransform (sdgrid.getLocalToWorld());
      }
      else {
         lpnt = pnt;
      }
      
      double d = sdgrid.getLocalDistance(lpnt);
      if (Math.abs(d) <= tol) {
         return InsideQuery.ON;
      }
      
      if (d < 0) {
         return InsideQuery.INSIDE;
      }
      
      return InsideQuery.OUTSIDE;
   }

   /**
    * Returns the nearest mesh vertex to a point, using a specified distance grid
    *
    * @param dgrid distance-grid containing the vertices.
    * @param pnt point for which the nearest vertex should be found.
    * @return the nearest vertex to the point, or <code>null</code> if
    * <code>dgrid</code> contains no vertices.
    */
   public Vertex3d nearestVertexToPoint (DistanceGrid dgrid, Point3d pnt) {
      Point3d nearest = new Point3d();
      Feature feat = dgrid.getNearestWorldFeature(nearest, pnt);
      if (feat instanceof Vertex3d) {
         return (Vertex3d)feat;
      }
      return null;
   }

   /**
    * Returns the nearest edge to a point, using a specified distance grid. 
    * An edge may be either a HalfEdge or a LineSegment. The former
    * are found in PolygonalMeshes, while the latter are found in PolylineMeshes.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest point on
    * the edge.
    * @param sval if not <code>null</code>, returns a coordinate in the
    * range [0,1] giving the location of the nearest point along the edge.
    * @param dgrid distance grid containing the features.
    * @param pnt point for which the nearest edge should be found.
    * @return the nearest edge to the point, or <code>null</code> if
    * <code>bvh</code> contains no edges.
    */
   public Feature nearestEdgeToPoint (
      Point3d nearPnt, DoubleHolder sval, DistanceGrid dgrid, Point3d pnt) {

      if (nearPnt == null) {
         nearPnt = new Point3d();
      }
      Feature feat = dgrid.getNearestWorldFeature(nearPnt, pnt);
      if (feat == null) {
         return null;
      }
      
      if (sval != null) {
         if (feat instanceof HalfEdge) {
            HalfEdge he = (HalfEdge)feat;
            sval.value = he.getProjectionParameter(nearPnt);
         } else if (feat instanceof LineSegment) {
            LineSegment seg = (LineSegment)feat;
            sval.value = seg.getProjectionParameter(nearPnt);
         }
      }
      return feat;
   }

}
