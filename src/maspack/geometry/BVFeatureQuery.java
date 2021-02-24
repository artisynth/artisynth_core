/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeSet;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.DoubleHolder;
import maspack.util.InternalErrorException;
import maspack.util.RandomGenerator;

/**
 * Worker class for nearest feature queries using bounding volume hierarchies.
 */
public class BVFeatureQuery {

   private static final double INF = Double.POSITIVE_INFINITY;

   public PointFaceDistanceCalculator myPointFaceCalc;
   PointVertexDistanceCalculator myPointVertexCalc;
   PointEdgeDistanceCalculator myPointEdgeCalc;
   LineFaceDistanceCalculator myLineFaceCalc;
   TriangleIntersector myIntersector;
   Vector3d myTmp1;
   Vector3d myTmp2;

   public enum InsideQuery {
      INSIDE,
      ON,
      OUTSIDE,
      UNSURE
   }

   public BVFeatureQuery() {
      myTmp1 = new Vector3d();
      myTmp2 = new Vector3d();
   }

   /**
    * Returns the nearest triangular mesh face to a point. This method
    * uses the default bounding volume hierarchy produced by the mesh.
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
      Face face = nearestFaceToPoint (nearPnt, uv, mesh.getBVTree(), pnt);
      return face;
   }

   /**
    * Find the nearest feature to a point
    * @param nearPnt populates with location of nearest point on set of features
    * @param featureTree bounding-volume tree containing desired features
    * @param pnt point from which to find the nearest feature
    * @return the nearest feature, or null
    */
   public Feature nearestFeatureToPoint(Point3d nearPnt, BVTree featureTree, Point3d pnt) {
      PointFeatureDistanceCalculator calc = new PointFeatureDistanceCalculator();
      calc.setPoint(pnt, featureTree.getBvhToWorld ());

      return (Feature)nearestObjectToPoint(nearPnt, featureTree, calc);
   }

   public boolean debug = false;

   /**
    * Returns the nearest triangular face to a point, using a specified
    * bounding volume hierarchy. The faces contained within the hierarchy are
    * all assumed to be triangular.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param uv if not <code>null</code>, returns the UV coordinates
    * of the nearest face point. These are the barycentric coordinates
    * with respect to the second and third vertices.
    * @param bvh bounding volume hierarchy containing the faces.
    * @param pnt point for which the nearest face should be found.
    * @return the nearest face to the point, or <code>null</code>
    * if <code>bvh</code> contains no faces.
    */
   public Face nearestFaceToPoint (
      Point3d nearPnt, Vector2d uv, BVTree bvh, Point3d pnt) {

      if (myPointFaceCalc == null) {
         myPointFaceCalc = new PointFaceDistanceCalculator();
      }
      myPointFaceCalc.setPoint (pnt, bvh.getBvhToWorld());
      Boundable nearest = nearestObject (bvh, myPointFaceCalc);

      if (nearest != null) {
         myPointFaceCalc.nearestDistance (nearest);
         if (uv != null) {
            uv.set (myPointFaceCalc.myUv);
         }
         if (nearPnt != null) {
            nearPnt.set (myPointFaceCalc.myNearest);
            nearPnt.transform (bvh.getBvhToWorld());
         }
         return (Face)nearest;
      }
      else {
         return null;
      }
   }

   /**
    * Returns the nearest triangular mesh face to a point. This method
    * uses the default bounding volume hierarchy produced by the mesh.
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
      BVFeatureQuery query = new BVFeatureQuery();
      return query.nearestFaceToPoint (nearPnt, uv, mesh.getBVTree(), pnt);
   }

   /**
    * Returns the nearest distance from a point to a mesh.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the mesh in world coordinates.
    * @param mesh mesh to which distance should be computed.
    * @param pnt point for which distance should be computed (world coords)
    * @return distance to the mesh, or -1 if the point is too far away
    */
   public static double distanceToMesh (
      Point3d nearPnt, PolygonalMesh mesh, Point3d pnt) {
      BVFeatureQuery query = new BVFeatureQuery();
      if (nearPnt == null) {
         nearPnt = new Point3d();
      }
      if (query.nearestFaceToPoint (
             nearPnt, null, mesh.getBVTree(), pnt) == null) {
         return -1;
      }
      else {
         return nearPnt.distance (pnt);
      }
   }

   /**
    * Returns the nearest triangular mesh face along a directed ray.
    * This method uses the default bounding volume hierarchy produced by the
    * mesh.  Faces in the negative ray direction are ignored. If no face is
    * found, the results returned int <code>nearPnt</code> and <code>duv</code>
    * are undefined.
    * 
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param duv if not <code>null</code>, returns the distance of the point to
    * the face (in <code>x</code>) and the UV coordinates of the nearest face
    * point (in <code>y</code> and <code>z</code>). The UV coordinates are the
    * barycentric coordinates with respect to the second and third vertices.
    * @param mesh mesh containing the faces.
    * @param origin originating point of the ray.
    * @param dir direction of the ray. If not normalized, the distance
    * returned in <code>duv</code> will be scaled by the inverse of
    * the length of <code>dir</code>.
    * @return the nearest face along the ray, or <code>null</code> is no
    * face is found.
    */
   public Face nearestFaceAlongRay (
      Point3d nearPnt, Vector3d duv,
      PolygonalMesh mesh, Point3d origin, Vector3d dir) {

      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      return nearestFaceAlongLine (
         nearPnt, duv, mesh.getBVTree(), origin, dir, 0, INF);
   }

   /**
    * Returns the nearest triangular face along a directed ray, using a
    * specified bounding volume hierarchy. The faces contained within the
    * hierarchy are all assumed to be triangular. Faces in the negative ray
    * direction are ignored. If no face is found, the results returned int
    * <code>nearPnt</code> and <code>duv</code> are undefined.
    * 
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param duv if not <code>null</code>, returns the distance of the point to
    * the face (in <code>x</code>) and the UV coordinates of the nearest face
    * point (in <code>y</code> and <code>z</code>). The UV coordinates are the
    * barycentric coordinates with respect to the second and third vertices.
    * @param bvh bounding volume hierarchy containing the faces.
    * @param origin origininating point of the ray.
    * @param dir direction of the ray. If not normalized, the distance
    * returned in <code>duv</code> will be scaled by the inverse of
    * the length of <code>dir</code>.
    * @return the nearest face along the ray, or <code>null</code> is no
    * face is found.
    */
   public Face nearestFaceAlongRay (
      Point3d nearPnt, Vector3d duv,
      BVTree bvh, Point3d origin, Vector3d dir) {

      return nearestFaceAlongLine (
         nearPnt, duv, bvh, origin, dir, 0, INF);
   }

   /**
    * Returns the nearest point on a polygonal mesh that intersects
    * a given ray, or null if the ray does not intersect the mesh.
    *
    * @param mesh to intersect with the ray 
    * @param origin origininating point of the ray.
    * @param dir direction of the ray.
    * @return nearest intersecting point, or null if no intersection
    */
   public static Point3d nearestPointAlongRay (
      PolygonalMesh mesh, Point3d origin, Vector3d dir) {

      BVFeatureQuery query = new BVFeatureQuery();
      Point3d pnt = new Point3d();
      Face faceHit = query.nearestFaceAlongRay (pnt, null, mesh, origin, dir);
      if (faceHit != null) {
         return pnt;
      }
      else {
         return null; 
      }
   }

   /**
    * Returns the nearest triangular mesh face to a point along a line.  This
    * method uses the default bounding volume hierarchy produced by the mesh.
    * If no face is found, the results returned int <code>nearPnt</code> and
    * <code>duv</code> are undefined. The search can be restricted to a line
    * segment by restricting <code>min</code> and <code>max</code> to finite
    * values. Setting them to negative and positive infinity causes the entire
    * line to be searched.
    * 
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param duv if not <code>null</code>, returns the distance of the point to
    * the face (in <code>x</code>) and the UV coordinates of the nearest face
    * point (in <code>y</code> and <code>z</code>). The UV coordinates are the
    * barycentric coordinates with respect to the second and third vertices.
    * @param mesh mesh containing the faces
    * @param origin originating point of the line
    * @param dir direction of the line. If not normalized, the distance
    * returned in <code>duv</code> will be scaled by the inverse of
    * the length of <code>dir</code>.
    * @param min minimum allowed distance along the line from
    * <code>origin</code>.
    * @param max maximum allowed distance along the line from
    * <code>origin</code>.
    * @return the nearest face along the ray, or <code>null</code> is no
    * face is found.
    */
   public Face nearestFaceAlongLine (
      Point3d nearPnt, Vector3d duv,
      PolygonalMesh mesh, Point3d origin, Vector3d dir, double min, double max) {

      return nearestFaceAlongLine (
         nearPnt, duv, mesh.getBVTree(), origin, dir, min, max);
   }

   /**
    * Returns the nearest triangular face to a point along a line, using a
    * specified bounding volume hierarchy. The faces contained within the
    * hierarchy are all assumed to be triangular. If no face is found, the
    * results returned int <code>nearPnt</code> and <code>duv</code> are
    * undefined. The search can be restricted to a line segment by restricting
    * <code>min</code> and <code>max</code> to finite values. Setting them to
    * negative and positive infinity causes the entire line to be searched.
    * 
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param duv if not <code>null</code>, returns the distance of the point to
    * the face (in <code>x</code>) and the UV coordinates of the nearest face
    * point (in <code>y</code> and <code>z</code>). The UV coordinates are the
    * barycentric coordinates with respect to the second and third vertices.
    * @param bvh bounding volume hierarchy containing the faces.
    * @param origin originating point of the line
    * @param dir direction of the line. If not normalized, the distance
    * returned in <code>duv</code> will be scaled by the inverse of
    * the length of <code>dir</code>.
    * @param min minimum allowed distance along the line from
    * <code>origin</code>.
    * @param max maximum allowed distance along the line from
    * <code>origin</code>.
    * @return the nearest face along the ray, or <code>null</code> is no
    * face is found.
    */
   public Face nearestFaceAlongLine (
      Point3d nearPnt, Vector3d duv,
      BVTree bvh, Point3d origin, Vector3d dir, double min, double max) {

      if (myLineFaceCalc == null) {
         myLineFaceCalc = new LineFaceDistanceCalculator();
      }
      myLineFaceCalc.setLine (origin, dir, min, max, bvh.getBvhToWorld());
      Boundable nearest = nearestObject (bvh, myLineFaceCalc);
      if (nearest != null) {
         Face face = (Face)nearest;
         myLineFaceCalc.nearestDistance (nearest);
         if (duv != null) {
            duv.set (myLineFaceCalc.myDuv);
         }
         if (nearPnt != null) {
            nearPnt.scaledAdd (
               myLineFaceCalc.myDuv.x, dir, origin);
         }
         return face;
      }
      else {
         return null;
      }

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
      BVFeatureQuery query = new BVFeatureQuery();
      return query.isInsideOrientedMesh (mesh.getBVTree(), pnt, -1);
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
      return isInsideOrientedMesh (mesh.getBVTree(), pnt, tol);
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
    * faces of which are contained within a specified bounding volume
    * hierarchy. "Oriented" means that all face normals are assumed to point
    * outwards.
    *
    * <p> The method works by inspecting the nearest face, edge or vertex to
    * the point. Hence the mesh does not need to be closed, and the method is
    * faster, though possibly less numerically robust, than {@link
    * #isInsideMesh(PolygonalMesh,BVTree,Point3d,double)}, which uses ray casting.
    *
    * @param bvh bounding volume hierarchy containing the faces.
    * @param pnt point to check (in world coordinates)
    * @param tol tolerance within which the point is considered to be on the
    * mesh surface. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return true if <code>pnt</code> is on or inside the mesh.
    */
   public boolean isInsideOrientedMesh (
      BVTree bvh, Point3d pnt, double tol) {
      Point3d lpnt;
      if (bvh.getBvhToWorld() != RigidTransform3d.IDENTITY) {
         lpnt = new Point3d (pnt);
         lpnt.inverseTransform (bvh.getBvhToWorld());
      }
      else {
         lpnt = pnt;
      }
      if (tol < 0) {
         tol = 1e-12*bvh.getRadius();
      }
      if (myPointFaceCalc == null) {
         myPointFaceCalc = new PointFaceDistanceCalculator();
      }
      myPointFaceCalc.myFace = null;

      if (!bvh.getRoot().intersectsSphere (lpnt, tol)) {
         lastCase = "Culled " + tol;
         return false;
      }
      // start by finding the nearest face to the point
      myPointFaceCalc.setPoint (lpnt);
      Face face = (Face)nearestObject (bvh, myPointFaceCalc);
      if (face == null) {
         // must be no faces in the mesh
         lastCase = "No Faces";
         return false;
      }
      myPointFaceCalc.nearestDistance (face);
      Vector3d diff = new Vector3d();
      Vector2d uv = myPointFaceCalc.myUv;
      if (myPointFaceCalc.myFace == null) {
         throw new InternalErrorException ("NO");
      }

      // diff is the vector from the nearest face point to pnt, in face coords
      diff.sub (myPointFaceCalc.myPnt, myPointFaceCalc.myNearest);
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
      return inside;
   }      

   /**
    * If called immediately after either
    * {@link #isInsideOrientedMesh(PolygonalMesh,Point3d,double)} or
    * {@link #isInsideOrientedMesh(BVTree,Point3d,double)},
    * returns the nearest face
    * that was used to resolve whether or not the query point is actually
    * inside the mesh. If the point is outside the mesh's bounding tree, then
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
      if (myPointFaceCalc == null || myPointFaceCalc.myFace == null) {
         // System.out.println ("NULL " + myPointFaceCalc);
         return null;
      }
      else {
         if (nearLoc != null) {
            nearLoc.set (myPointFaceCalc.myNearest);
         }
         if (uv != null) {
            uv.set (myPointFaceCalc.myUv);
         }
         return myPointFaceCalc.myFace;
      }
   }

   int myMaxRayCasts = 100;

   /**
    * Returns the maximum number of ray casts that will be attempted by
    * {@link #isInsideMesh}.
    *
    * @return maximum number of ray casts
    */
   public int getMaxRayCasts() {
      return myMaxRayCasts;
   }

   /**
    * Sets the maximum number of ray casts that will be attempted by
    * {@link #isInsideMesh}.
    *
    * @param maxrc maximum number of ray casts
    */
   public void setMaxRayCasts (int maxrc) {
      myMaxRayCasts = maxrc;
   }

   /**
    * Determines if a point is on or inside a closed triangular mesh. The mesh
    * normals are assumed to be pointing outwards.  The method works by
    * counting the number of times a random ray cast from the point intersects
    * the mesh. Degeneracies are detected and result in another ray being cast.
    * Whether or not the point is on the mesh is determined using a
    * numerical tolerance computed from the mesh's overall dimensions.
    * <p>
    * The maximum number of ray casts can be controlled by the methods {@link
    * #getMaxRayCasts()} and {@link #setMaxRayCasts(int)}. If the number of ray casts
    * exceeds this total the method returns InsideQuery.UNSURE.
    *
    * @param mesh mesh which point may be inside.
    * @param pnt point to check.
    * @return InsideQuery.INSIDE if <code>pnt</code> is inside, InsideQuery.ON if on,
    *  InsideQuery.OUTSIDE if it is outside, and InsideQuery.UNSURE if the method did not converge.
    */
   public static InsideQuery isInsideMesh (
      PolygonalMesh mesh, Point3d pnt) {
      BVFeatureQuery query = new BVFeatureQuery();
      return query.isInsideMesh (mesh, mesh.getBVTree(), pnt, -1);
   }

   /**
    * Determines if a point is on or inside a closed triangular mesh. The mesh
    * normals are assumed to be pointing outwards.  The method works by
    * counting the number of times a random ray cast from the point intersects
    * the mesh. Degeneracies are detected and result in another ray being cast.
    * <p>
    * The maximum number of ray casts can be controlled by the methods {@link
    * #getMaxRayCasts} and {@link #setMaxRayCasts(int)}. If the number of ray casts
    * exceeds this total the method returns InsideQuery.UNSURE.
    *
    * @param mesh mesh which point may be inside.
    * @param pnt point to check.
    * @param tol tolerance within which the point is considered to be on the
    * mesh surface. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return InsideQuery.INSIDE if <code>pnt</code> is inside, InsideQuery.ON if on,
    *  InsideQuery.OUTSIDE if it is outside, and InsideQuery.UNSURE if the method did not converge.
    */
   public InsideQuery isInsideMesh (PolygonalMesh mesh, Point3d pnt, double tol) {
      return isInsideMesh (mesh, mesh.getBVTree(), pnt, tol);
   }

   /**
    * Determines if a point is on or inside a closed triangular mesh, the faces
    * of which are contained within a specified bounding volume hierarchy.  The
    * method works by counting the number of times a random ray cast from the
    * point intersects the mesh. Degeneracies are detected and result in
    * another ray being cast.
    * <p>
    * The maximum number of ray casts can be
    * controlled by the methods {@link #getMaxRayCasts()} and {@link
    * #setMaxRayCasts(int)}. If the number of ray casts exceeds this total the
    * method returns InsideQuery.UNSURE.
    *
    * @param mesh mesh which point may be inside.
    * @param bvh bounding volume hierarchy containing the mesh.
    * @param pnt point to check.
    * @param tol tolerance within which the point is considered to be on the
    * mesh surface. A value of -1 will cause the tolerance to be computed
    * automatically.
    * @return InsideQuery.INSIDE if <code>pnt</code> is inside or on,
    *  InsideQuery.OUTSIDE if it is outside, and InsideQuery.UNSURE if the method did not converge.
    */
   public InsideQuery isInsideMesh(
      PolygonalMesh mesh, BVTree bvh, Point3d pnt, double tol) {
      InsideQuery q = isInsideOrOnMesh(mesh, bvh, pnt, tol);
      if (q == InsideQuery.ON) {
         return InsideQuery.INSIDE;
      }
      return q;
   }

   public InsideQuery isInsideOrOnMesh (
      PolygonalMesh mesh, BVTree bvh, Point3d pnt, double tol) {

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
      Vector3d ldir = new Vector3d();
      double eps = 1e-12;
      if (tol < 0) {
         tol = eps*bvh.getRadius();
      }

      if (bvh.getBvhToWorld() != RigidTransform3d.IDENTITY) {
         lpnt = new Point3d (pnt);
         lpnt.inverseTransform (bvh.getBvhToWorld());
      }
      else {
         lpnt = pnt;
      }
      if (!bvh.getRoot().intersectsSphere (lpnt, tol)) {
         return InsideQuery.OUTSIDE;
      }

      // start with outward direction
      Vector3d dir = new Vector3d();
      Point3d nearestPoint = new Point3d();
      Vector2d uv = new Vector2d();
      nearestFaceToPoint(nearestPoint, uv, bvh, pnt);

      if (pnt.distance(nearestPoint) <= tol) {
         return InsideQuery.ON;
      }      
      dir.sub(pnt, nearestPoint);
      dir.normalize();

      ArrayList<BVNode> nodes = new ArrayList<BVNode>();

      Point3d centroid = new Point3d();
      Random rand = RandomGenerator.get();
      ArrayList<Face> rootFaces = mesh.getFaces();
      RigidTransform3d trans = mesh.getMeshToWorld();


      Vector3d duv = new Vector3d();   // for triangle-face intersection
      int iters = 0;
      do {

         int nIntersections = 0;
         boolean unsure = false;
         nodes.clear();

         if (bvh.getBvhToWorld() != RigidTransform3d.IDENTITY) {
            ldir.inverseTransform (bvh.getBvhToWorld(), dir);
         } else {
            ldir.set (dir);
         }

         bvh.intersectLine (nodes, pnt, dir, 0, INF);

         // loop through all potential faces we may intersect
         for (BVNode node : nodes) {
            Boundable[] elems = node.getElements();
            for (int i=0; i< elems.length; i++) {
               if (elems[i] instanceof Face) {
                  Face face = (Face)elems[i];

                  HalfEdge he = face.firstHalfEdge();
                  Point3d p0 = he.head.pnt;
                  he = he.getNext();
                  Point3d p1 = he.head.pnt;
                  he = he.getNext();
                  Point3d p2 = he.head.pnt;

                  int isect =
                  myIntersector.intersect (
                     p0, p1, p2, lpnt, ldir, duv);

                  if (isect == 0) {
                     // no intersection
                     continue;
                  }
                  else if (Math.abs(duv.x) <= tol) {

                     return InsideQuery.ON;
                  }
                  else if (duv.x < 0) {
                     continue;   // wrong direction of ray
                  }

                  // w0, w1, w2 are the barycentric weights of the intersection
                  // point with respect to the face vertices
                  double w0 = 1 - duv.y - duv.z;
                  double w1 = duv.y;
                  double w2 = duv.z;

                  if (w0 > -eps && w1 > -eps && w2 > -eps) {
                     // either close or well inside

                     if (w0 > eps && w1 > eps && w2 > eps &&
                     (1-w0) > eps && (1-w1) > eps && (1-w2) > eps) {
                        nIntersections++;
                     } else {
                        unsure = true;
                        break;
                     }
                  }
               }               
            }
            if (unsure) {
               break;
            }
         }

         if (!unsure) {
            if  (( nIntersections % 2) == 0 ) {
               return InsideQuery.OUTSIDE;
            } else {
               return InsideQuery.INSIDE;
            }
         }
         // queue up next direction
         int iface = rand.nextInt(rootFaces.size());
         rootFaces.get(iface).computeCentroid (centroid);
         centroid.transform(trans);
         dir.sub(pnt, centroid);
         if (dir.norm() <= tol) {
            return InsideQuery.ON;
         }
         dir.normalize();
         
         iters++;
      } while (iters < myMaxRayCasts);

      return InsideQuery.UNSURE;
   }

   /**
    * Returns the nearest mesh vertex to a point. This method uses the default
    * bounding volume hierarchy produced by the mesh.
    *
    * @param mesh mesh containing the vertices.
    * @param pnt point for which the nearest vertex should be found.
    * @return the nearest vertex to the point, or <code>null</code> if the mesh
    * contains no vertices.
    */
   public Vertex3d nearestVertexToPoint (MeshBase mesh, Point3d pnt) {

      return nearestVertexToPoint (mesh.getBVTree(), pnt);
   }

   /**
    * Returns the nearest mesh vertex to a point, using a specified bounding
    * volume hierarchy.
    *
    * @param bvh bounding volume hierarchy containing the vertices.
    * @param pnt point for which the nearest vertex should be found.
    * @return the nearest vertex to the point, or <code>null</code> if
    * <code>bvh</code> contains no vertices.
    */
   public Vertex3d nearestVertexToPoint (BVTree bvh, Point3d pnt) {

      if (myPointVertexCalc == null) {
         myPointVertexCalc = new PointVertexDistanceCalculator();
      }
      myPointVertexCalc.setPoint (pnt, bvh.getBvhToWorld());
      Boundable nearest = nearestObject (bvh, myPointVertexCalc);
      if (nearest != null) {
         return (Vertex3d)nearest;
      }
      else {
         return null;
      }
   }

   /**
    * Returns the nearest mesh edge to a point. This method uses the default
    * bounding volume hierarchy produced by the mesh. An edge may be either a
    * HalfEdge or a LineSegment. The former are found in PolygonalMeshes, while
    * the latter are found the bounding volume hierarchies produced for
    * PolylineMeshes.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest point on
    * the edge.
    * @param sval if not <code>null</code>, returns a coordinate in the
    * range [0,1] giving the location of the nearest point along the edge.
    * @param mesh mesh containing the edges.
    * @param pnt point for which the nearest edge should be found.
    * @return the nearest edge to the point, or <code>null</code> if the mesh
    * contains no edges.
    */
   public Boundable nearestEdgeToPoint (
      Point3d nearPnt, DoubleHolder sval, MeshBase mesh, Point3d pnt) {

      if (mesh instanceof PointMesh) {
         return null;
      }
      return nearestEdgeToPoint (nearPnt, sval, mesh.getBVTree(), pnt);
   }

   /**
    * Returns the nearest edge to a point, using a specified bounding volume
    * hierarchy. An edge may be either a HalfEdge or a LineSegment. The former
    * are found in PolygonalMeshes, while the latter are found the bounding
    * volume hierarchies produced for PolylineMeshes.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest point on
    * the edge.
    * @param sval if not <code>null</code>, returns a coordinate in the
    * range [0,1] giving the location of the nearest point along the edge.
    * @param bvh bounding volume hierarchy containing the faces.
    * @param pnt point for which the nearest edge should be found.
    * @return the nearest edge to the point, or <code>null</code> if
    * <code>bvh</code> contains no edges.
    */
   public Boundable nearestEdgeToPoint (
      Point3d nearPnt, DoubleHolder sval, BVTree bvh, Point3d pnt) {

      if (myPointEdgeCalc == null) {
         myPointEdgeCalc = new PointEdgeDistanceCalculator();
      }
      myPointEdgeCalc.setPoint (pnt, bvh.getBvhToWorld());
      Boundable nearest = nearestObject (bvh, myPointEdgeCalc);
      if (nearest != null) {
         double s = myPointEdgeCalc.computeNearestPoint (nearPnt, nearest);
         if (nearPnt != null) {
            nearPnt.transform (bvh.getBvhToWorld());
         }
         if (sval != null) {
            sval.value = s;
         }
         return nearest;
      }
      else {
         return null;
      }
   }

   private class BVCheckRequest {
      BVNode myNode;
      double myDist;

      BVCheckRequest (BVNode node, double dist) {
         myNode = node;
         myDist = dist;
      }
   }

   private class BVCheckComparator implements Comparator<BVCheckRequest> {

      public int compare (BVCheckRequest req1, BVCheckRequest req2) {
         if (req1.myDist < req2.myDist) {
            return -1;
         }
         else if (req1.myDist == req2.myDist) {
            return 0;
         }
         else { // req1.myDist > req2.myDist
            return 1;
         }
      }
   }

   public interface ObjectDistanceCalculator {

      /**
       * Reset any internal `nearest' cache so calculator can be re-used
       */
      public void reset();

      /**
       * Computes the nearest distance to the bounding node
       * 
       * @param node node to check distance to
       * @return distance to <code>node</code>
       */
      public double nearestDistance (BVNode node);

      /**
       * Computes the nearest distance to an element within the node.
       * This should store the object and distance for later use
       * by {@link #nearestObject()} and {@link #nearestDistance()}
       * @param e element for which distance is to be computed
       * @return nearest distance to <code>e</code>
       */
      public double nearestDistance (Boundable e);

      /**
       * Returns the last computed nearest {@link Boundable} object
       * 
       * @return last computed nearest object
       */
      public Boundable nearestObject();

      /**
       * Returns the last computed nearest distance
       * 
       * @return last computed nearest distance
       */
      public double nearestDistance();

      /**
       * Returns the last computed nearest point, in local coordinates.
       * 
       * @return last computed nearest point
       */
      public Point3d nearestPoint();
   }

   /**
    * Utility class for collecting objects when performing
    * a nearest-distance search until a condition is met
    * (e.g. nearest K objects, or continue collecting objects 
    * until condition is satisfied)
    *
    */
   public static interface ObjectDistanceCollector {

      /**
       * Checks if a request has the potential to satisfy the condition,
       * otherwise we can stop the search.
       * @param bvcr request, containg BVNode and distance
       * @return true if we should continue checking, false to signal
       * quitting
       */
      public boolean check(BVCheckRequest bvcr);

      public boolean add(Boundable b, double distance);

   }

   /**
    * Pair of { Object, Distance}, used by nearest object queries
    *
    * @param <T> object type
    */
   public static class ObjectDistanceEntry<T> 
   implements Comparable<ObjectDistanceEntry<T>>{
      T object;
      double distance;

      public ObjectDistanceEntry(T object, double distance) {
         this.object = object;
         this.distance = distance;
      }

      /**
       * Underlying object
       * @return object
       */
      public T getObject() {
         return object;
      }

      /**
       * Distance
       * @return distance
       */
      public double getDistance() {
         return distance;
      }

      /**
       * Compares by distance (ascending) then by object hashcode (ascending) to enforce
       * uniqueness
       */
      @Override
      public int compareTo (ObjectDistanceEntry<T> o) {
         int c = Double.compare (distance, o.distance);
         if (c != 0) {
            return c;
         }

         // hash-code
         c = Integer.compare (object.hashCode (), o.object.hashCode ());
         return c;
      }
   }

   /**
    * Collects the nearest K boundables, as in a k-nearest-neighbor search
    * @param <T> boundable class
    */
   public static class NearestKCollector<T> 
   implements ObjectDistanceCollector {

      TreeSet<ObjectDistanceEntry<T>> nearest;
      PriorityQueue<Double> thresholdQueue;  // stores exactly the kth limit
      double distanceThreshold;
      int klimit;

      public NearestKCollector (int k) {
         nearest = new TreeSet<>();
         thresholdQueue = new PriorityQueue<>(Collections.reverseOrder ());
         distanceThreshold = Double.POSITIVE_INFINITY;
         klimit = k;
      }

      @Override
      public boolean check(BVCheckRequest bvcr) {
         if (bvcr.myDist > distanceThreshold) {
            return false;
         }
         return true;
      }

      @Override
      public boolean add(Boundable object, double distance) {

         if (distance > distanceThreshold) {
            return false;
         }

         @SuppressWarnings("unchecked")
         ObjectDistanceEntry<T> entry = new ObjectDistanceEntry<> ((T)object, distance);

         nearest.add (entry);

         thresholdQueue.add (distance);
         if (thresholdQueue.size () > klimit) {
            thresholdQueue.poll ();
            distanceThreshold = thresholdQueue.peek ();
         } else if (thresholdQueue.size () == klimit) {
            distanceThreshold = thresholdQueue.peek ();
         }

         // maybe remove from back
         if (nearest.size () > klimit) {
            Iterator<ObjectDistanceEntry<T>> dit = nearest.descendingIterator ();
            while (dit.hasNext () && dit.next ().distance > distanceThreshold) {
               dit.remove ();
            }
         }

         return true;
      }

      /**
       * Retrieves the current set of nearest entries
       * 
       * Note: could be {@code <} k if there are fewer than k bounded elements, 
       * and {@code >} k if the last several elements have the exact same distance
       * 
       * @return (approximately) k nearest elements
       */
      public TreeSet<ObjectDistanceEntry<T>> nearest() {
         return nearest;
      }

   }

   private class PointFaceDistanceCalculator implements ObjectDistanceCalculator {

      Point3d myPnt;
      Point3d myNearest;
      Vector2d myUv;
      Face myFace;
      double myDist;

      public PointFaceDistanceCalculator () {
         if (myIntersector == null) {
            myIntersector = new TriangleIntersector();
         }
         myPnt = new Point3d();
         myNearest = new Point3d();
         myUv = new Vector2d();
         reset();
      }

      @Override
      public void reset() {
         myFace = null;
         myDist = Double.POSITIVE_INFINITY;
      }

      public void setPoint (Point3d pnt, RigidTransform3d XBvhToWorld) {
         if (XBvhToWorld == RigidTransform3d.IDENTITY) {
            myPnt.set (pnt);
         }
         else {
            myPnt.inverseTransform (XBvhToWorld, pnt);
         }
      }          

      public void setPoint (Point3d pnt) {
         myPnt.set (pnt);
      }          

      public double nearestDistance (BVNode node) {
         return node.distanceToPoint (myPnt);
      }

      public double nearestDistance (Boundable e) {
         myFace = null;
         if (e instanceof Face) {
            Face face = (Face)e;
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            Point3d p0 = he.head.pnt;
            he = he.getNext();
            Point3d p1 = he.head.pnt;
            he = he.getNext();
            Point3d p2 = he.head.pnt;
            if (he.getNext() != he0) {
               throw new InternalErrorException (
                  "face "+face.getIndex()+" is not triangular");
            }
            myFace = face;
            myDist = myIntersector.nearestpoint (
               p0, p1, p2, myPnt, myNearest, myUv);
            return myDist;
         }
         else {
            return -1;
         }
      }

      public Point3d nearestPoint() {
         return myNearest;
      }

      public Face nearestObject () {
         return myFace;
      }

      @Override
      public double nearestDistance() {
         return myDist;
      }
   }

   private class PointVertexDistanceCalculator
   implements ObjectDistanceCalculator {

      Point3d myPnt;
      Vertex3d myVertex;
      double myDist;

      public PointVertexDistanceCalculator () {
         if (myIntersector == null) {
            myIntersector = new TriangleIntersector();
         }
         myPnt = new Point3d();
         reset();
      }

      @Override
      public void reset() {
         myVertex = null;
         myDist = Double.POSITIVE_INFINITY;
      }

      public void setPoint (Point3d pnt, RigidTransform3d XBvhToWorld) {
         if (XBvhToWorld == RigidTransform3d.IDENTITY) {
            myPnt.set (pnt);
         }
         else {
            myPnt.inverseTransform (XBvhToWorld, pnt);
         }
      }          

      public void setPoint (Point3d pnt) {
         myPnt.set (pnt);
      }          

      public double nearestDistance (BVNode node) {
         return node.distanceToPoint (myPnt);
      }

      public double nearestDistance (Boundable e) {
         if (e instanceof Vertex3d) {
            myVertex = (Vertex3d)e;
            myDist = myVertex.pnt.distance (myPnt);
            return myDist;
         }
         else if (e instanceof Face) {
            Face face = (Face)e;
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            double dmin = INF;
            do {
               double d = he.head.pnt.distance (myPnt);
               if (d < dmin) {
                  dmin = d;
                  myVertex = he.head;
               }
               he = he.getNext(); 
            }
            while (he != he0);
            myDist = dmin;
            return dmin;
         }
         else if (e instanceof LineSegment) {
            LineSegment seg = (LineSegment)e;
            double d0 = seg.myVtx0.pnt.distance (myPnt);
            double d1 = seg.myVtx1.pnt.distance (myPnt);
            if (d0 <= d1) {
               myDist = d0;
               myVertex = seg.myVtx0;
               return d0;
            }
            else {
               myDist = d1;
               myVertex = seg.myVtx1;
               return d1;
            }
         }
         else {
            myVertex = null;
            myDist = Double.POSITIVE_INFINITY;
            return -1;
         }
      }

      public Vertex3d nearestObject() {
         return myVertex;
      }

      @Override
      public double nearestDistance() {
         return myDist;
      }

      public Point3d nearestPoint() {
         return myVertex.getPosition ();
      }
   }

   private class PointEdgeDistanceCalculator
   implements ObjectDistanceCalculator {

      Point3d myPnt;
      Boundable myEdge;
      double myS = 0;
      double myDist;
      Point3d myNearest;

      public PointEdgeDistanceCalculator () {
         myPnt = new Point3d();
         myNearest = new Point3d();
         reset();
      }

      @Override
      public void reset() {
         myS = 0;
         myEdge = null;
         myDist = Double.POSITIVE_INFINITY;
      }

      public void setPoint (Point3d pnt, RigidTransform3d XBvhToWorld) {
         if (XBvhToWorld == RigidTransform3d.IDENTITY) {
            myPnt.set (pnt);
         }
         else {
            myPnt.inverseTransform (XBvhToWorld, pnt);
         }
      }          

      public void setPoint (Point3d pnt) {
         myPnt.set (pnt);
      }          

      public double nearestDistance (BVNode node) {
         return node.distanceToPoint (myPnt);
      }

      private double distanceToEdge (Point3d p0, Point3d p1) {
         double ux = p1.x - p0.x;
         double uy = p1.y - p0.y;
         double uz = p1.z - p0.z;

         double dx, dy, dz;

         dx = myPnt.x - p1.x;
         dy = myPnt.y - p1.y;
         dz = myPnt.z - p1.z;
         double dot = dx*ux + dy*uy + dz*uz;
         if (dot >= 0) {
            myS = 1;
            return Math.sqrt (dx*dx + dy*dy + dz*dz);
         }
         dx = myPnt.x - p0.x;
         dy = myPnt.y - p0.y;
         dz = myPnt.z - p0.z;
         dot = dx*ux + dy*uy + dz*uz;
         if (dot <= 0) {
            myS = 0;
            return Math.sqrt (dx*dx + dy*dy + dz*dz);
         }
         else {
            double umagSqr = ux*ux + uy*uy + uz*uz;
            double dmagSqr = dx*dx + dy*dy + dz*dz;
            myS = dot/umagSqr;
            return Math.sqrt ((dmagSqr*umagSqr - dot*dot)/umagSqr);
         }
      }

      public double computeNearestPoint (Point3d near, Boundable e) {
         Point3d p0, p1;
         if (e instanceof LineSegment) {
            LineSegment seg = (LineSegment)e;
            p0 = seg.myVtx0.pnt;
            p1 = seg.myVtx1.pnt;
         }
         else if (e instanceof HalfEdge) {
            HalfEdge he = (HalfEdge)e;
            p0 = he.tail.pnt;
            p1 = he.head.pnt;
         }
         else {
            throw new IllegalArgumentException (
               "Feature is "+e.getClass()+", expected LineSegment or HalfEdge");
         }
         distanceToEdge (p0, p1);
         if (near != null) {
            near.combine (1-myS, p0, myS, p1);
         }
         return myS;
      }         

      public double nearestDistance (Boundable e) {
         if (e instanceof LineSegment) {
            LineSegment seg = (LineSegment)e;
            myEdge = seg;
            myDist = distanceToEdge (seg.myVtx0.pnt, seg.myVtx1.pnt);
            return myDist;
         }
         else if (e instanceof Face) {
            Face face = (Face)e;
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            double dmin = INF;
            do {
               double d = distanceToEdge (he.tail.pnt, he.head.pnt);
               if (d < dmin) {
                  dmin = d;
                  myEdge = he;
               }
               he = he.getNext(); 
            }
            while (he != he0);
            myDist = dmin;
            return dmin;
         }
         else {
            myEdge = null;
            myDist = Double.POSITIVE_INFINITY;
            return -1;
         }
      }

      public Boundable nearestObject() {
         return myEdge;
      }

      @Override
      public double nearestDistance() {
         return myDist;
      }

      public Point3d nearestPoint() {
         computeNearestPoint (myNearest, myEdge);
         return myNearest;
      }

   }

   private static class PointFeatureDistanceCalculator implements ObjectDistanceCalculator {

      Point3d myPnt;
      Point3d myNearest;
      Feature myFeature;
      double myDist;

      public PointFeatureDistanceCalculator () {
         myPnt = new Point3d();
         myNearest = new Point3d();
         reset();
      }

      @Override
      public void reset() {
         myFeature = null;
         myDist = Double.POSITIVE_INFINITY;
      }

      public void setPoint (Point3d pnt, RigidTransform3d XBvhToWorld) {
         if (XBvhToWorld == RigidTransform3d.IDENTITY) {
            myPnt.set (pnt);
         }
         else {
            myPnt.inverseTransform (XBvhToWorld, pnt);
         }
      }          

      public void setPoint (Point3d pnt) {
         myPnt.set (pnt);
      }          

      public double nearestDistance (BVNode node) {
         return node.distanceToPoint (myPnt);
      }

      public double nearestDistance (Boundable e) {
         myFeature = null;
         Point3d nearest = new Point3d();
         if (e instanceof Feature) {
            Feature ff = (Feature)e;
            ff.nearestPoint(nearest, myPnt);
            double d = nearest.distance(myPnt);
            if (d < myDist) {
               myDist = d;
               myFeature = ff;
               myNearest.set(nearest);
            }
            return d;
         }
         else {
            return -1;
         }
      }

      public Feature nearestObject () {
         return myFeature;
      }

      @Override
      public double nearestDistance() {
         return myDist;
      }

      public Point3d nearestPoint() {
         return myNearest;
      }
   }

   private class LineFaceDistanceCalculator implements ObjectDistanceCalculator {

      Point3d myOrigin;
      Vector3d myDir;
      double myMin;
      double myMax;
      Vector3d myDuv;
      Face myFace;
      double myDist;
      Point3d myNearest;

      public LineFaceDistanceCalculator () {
         if (myIntersector == null) {
            myIntersector = new TriangleIntersector();
         }
         myOrigin = new Point3d();
         myDir = new Vector3d();
         myDuv = new Vector3d();
         myNearest = new Point3d();
         reset();
      }

      @Override
      public void reset() {
         myMin = 0;
         myMax = INF;
         myFace = null;
         myDist = Double.POSITIVE_INFINITY;
      }

      public void setLine (
         Point3d origin, Vector3d dir, double min, double max,
         RigidTransform3d XBvhToWorld) {

         if (XBvhToWorld == RigidTransform3d.IDENTITY) {
            myOrigin.set (origin);
            myDir.set (dir);
         }
         else {
            myOrigin.inverseTransform (XBvhToWorld, origin);
            myDir.inverseTransform (XBvhToWorld.R, dir);
         }
         myMin = min;
         myMax = max;
      }          

      public double nearestDistance (BVNode node) {
         double d = node.distanceAlongLine (myOrigin, myDir, myMin, myMax);
         if (d == INF) {
            return -1;
         }
         else {
            return d;
         }
      }

      public double nearestDistance (Boundable e) {
         if (e instanceof Face) {
            Face face = (Face)e;
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            Point3d p0 = he.head.pnt;
            he = he.getNext();
            Point3d p1 = he.head.pnt;
            he = he.getNext();
            Point3d p2 = he.head.pnt;
            if (he.getNext() != he0) {
               throw new InternalErrorException (
                  "face "+face.getIndex()+" is not triangular");
            }
            int rcode = myIntersector.intersect (
               p0, p1, p2, myOrigin, myDir, myDuv);
            if (rcode == 0) {
               myFace = null;
               myDist = Double.POSITIVE_INFINITY;
               return -1;
            }
            else if (rcode == 1) {
               double d = myDuv.x;
               if (d > myMax || d < myMin) {
                  myFace = null;
                  myDist = Double.POSITIVE_INFINITY;
                  return -1;
               }
               else {
                  myFace = face;
                  myDist = Math.abs(d);
                  return myDist;
               }               
            }
            else {
               throw new InternalErrorException (
                  "Triangle-ray intersector returns unexpected code " + rcode);
            }
         }
         else {
            myFace = null;
            myDist = Double.POSITIVE_INFINITY;
            return -1;
         }
      }

      public Face nearestObject() {
         return myFace;
      }         

      @Override
      public double nearestDistance() {
         return myDist;
      }

      boolean computeNearestPoint(Point3d nearest) {
         if (myFace == null) {
            return false;
         }

         nearest.setZero ();
         HalfEdge he = myFace.firstHalfEdge ();
         nearest.scale (1-myDuv.y-myDuv.z, he.head.getWorldPoint ());
         he = he.next;
         nearest.scaledAdd (myDuv.y, he.head.getWorldPoint ());
         he = he.next;
         nearest.scaledAdd (myDuv.z, he.head.getWorldPoint ());

         return true;
      }

      public Point3d nearestPoint() {
         if (myFace != null) {
            computeNearestPoint (myNearest);
            return myNearest;
         }
         return null;
      }
   }

   /**
    * Returns the nearest Object to a point, using a specified
    * bounding volume hierarchy. 
    * 
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point in the boundable
    * @param bvh bounding volume hierarchy containing the faces.
    * @param calc distance calculator for which nearest object should be found.
    * @return the nearest object to the point, or <code>null</code>
    * if <code>bvh</code> contains no objects.
    */
   public Boundable nearestObjectToPoint (
      Point3d nearPnt, BVTree bvh, ObjectDistanceCalculator calc) {

      Boundable nearest = nearestObject (bvh, calc);

      if (nearest != null) {
         calc.nearestDistance (nearest);
         if (nearPnt != null) {
            nearPnt.set (calc.nearestPoint ());
            nearPnt.transform (bvh.getBvhToWorld());
         }
         return nearest;
      }
      else {
         return null;
      }
   }

   public Boundable nearestObject (BVTree bvh, ObjectDistanceCalculator dcalc) {

      double nearestDistance = INF;
      Boundable nearestFeature = null;
      dcalc.reset();
      
      PriorityQueue<BVCheckRequest> queue =
      new PriorityQueue<BVCheckRequest> (11, new BVCheckComparator());

      double d = dcalc.nearestDistance (bvh.getRoot());
      if (d != -1) {
         queue.add (new BVCheckRequest (bvh.getRoot(), d));
      }
      while (!queue.isEmpty()) {
         BVCheckRequest req = queue.poll();
         if (req.myDist > nearestDistance) {
            break;
         }
         BVNode node = req.myNode;
         if (node.isLeaf()) {
            Boundable[] elems = node.getElements();
            for (int i=0; i<elems.length; i++) {
               d = dcalc.nearestDistance (elems[i]);
               if (d != -1 && d < nearestDistance) {
                  nearestFeature = dcalc.nearestObject();
                  nearestDistance = d;
               }
            }
         }
         else {
            // process node
            BVNode child;
            for (child=node.myFirstChild; child!=null; child=child.myNext) {
               d = dcalc.nearestDistance (child);
               if (d != -1 && d < nearestDistance) {
                  queue.add (new BVCheckRequest (child, d));
               }
            }
         }
      }

      // trigger storing of nearest feature
      dcalc.reset ();
      if (nearestFeature != null) {
         dcalc.nearestDistance(nearestFeature);
      }
      return nearestFeature;
   }

   /**
    * Find and collect objects starting at the nearest and working outwards 
    * until a given criteria is met
    * 
    * @param bvh bounding object hierarchy
    * @param dcalc distance calculator
    * @param coll distance condition
    */
   public void nearestObjects (BVTree bvh, ObjectDistanceCalculator dcalc, 
      ObjectDistanceCollector coll) {

      //	   double nearestDistance = INF;
      //	   Boundable nearestFeature = null;
      dcalc.reset();

      PriorityQueue<BVCheckRequest> queue =
      new PriorityQueue<BVCheckRequest> (11, new BVCheckComparator());

      double d = dcalc.nearestDistance (bvh.getRoot());
      if (d != -1) {
         queue.add (new BVCheckRequest (bvh.getRoot(), d));
      }
      while (!queue.isEmpty()) {
         BVCheckRequest req = queue.poll();

         // req contains node and distance to node
         if (!coll.check(req)) {
            break;
         }
         BVNode node = req.myNode;
         if (node.isLeaf()) {
            Boundable[] elems = node.getElements();
            for (int i=0; i<elems.length; i++) {
               d = dcalc.nearestDistance (elems[i]);
               if (d != -1) {
                  coll.add(elems[i], d);
               }
            }
         }
         else {
            // process node
            BVNode child;
            for (child=node.myFirstChild; child!=null; child=child.myNext) {
               d = dcalc.nearestDistance (child);
               if (d != -1) {
                  queue.add (new BVCheckRequest (child, d));
               }
            }
         }
      }
   }

   /**
    * Finds the K nearest features in the bounding-volume hierarchy to a point in world coordinates
    * 
    * The resulting tree is ordered by distance (nearest to furthest).  There may be fewer than K
    * entries if the bounding tree contains fewer than K elements, and there may be more than K
    * entries if the last size()-k+1 elements all have exactly the same distance
    *  
    * @param bvh bounding volume hierarchy
    * @param k number of elements to return
    * @param pnt point (world-coordinates)
    * @return set of nearest features
    */
   public TreeSet<ObjectDistanceEntry<Feature>> nearestKFeaturesToPoint(BVTree bvh, int k, Point3d pnt) {

      PointFeatureDistanceCalculator calc = new PointFeatureDistanceCalculator();
      calc.setPoint(pnt, bvh.getBvhToWorld ());

      NearestKCollector<Feature> collector = new NearestKCollector<> (k);
      nearestObjects (bvh, calc, collector);

      return collector.nearest();      
   }

   public BVNode nearestLeafToPoint(BVTree bvh, Point3d pnt) {

      double nearestDistance = INF;
      BVNode nearestNode = null;

      PriorityQueue<BVCheckRequest> queue =
      new PriorityQueue<BVCheckRequest> (11, new BVCheckComparator());


      double d = bvh.getRoot().distanceToPoint (pnt);
      if (d != -1) {
         queue.add (new BVCheckRequest (bvh.getRoot(), d));
      }
      while (!queue.isEmpty()) {
         BVCheckRequest req = queue.poll();
         if (req.myDist > nearestDistance) {
            break;
         }
         BVNode node = req.myNode;
         if (node.isLeaf()) {
            d = node.distanceToPoint (pnt);
            if (d != -1 && d < nearestDistance) {
               nearestDistance = d;
               nearestNode = node;
            }
         }
         else {
            // process node
            BVNode child;
            for (child=node.myFirstChild; child!=null; child=child.myNext) {
               d = child.distanceToPoint (pnt);
               if (d != -1 && d < nearestDistance) {
                  queue.add (new BVCheckRequest (child, d));
               }
            }
         }
      }

      return nearestNode;
   }
}
