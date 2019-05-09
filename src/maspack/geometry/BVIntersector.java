/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

/**
 * Worker class for computing intersections using bounding volume hierarchies.
 */
public class BVIntersector {

   private final double EPS = 1e-13;

   TriangleIntersector myTriIntersector;

   // vectors for storing transformed triangle vertices
   Point3d myP0;
   Point3d myP1;
   Point3d myP2;

   public BVIntersector () {
      myTriIntersector = new TriangleIntersector();
      myP0 = new Point3d();
      myP1 = new Point3d();
      myP2 = new Point3d();
   }

   /**
    * Intersects the faces of two triangular meshes. The process is accelerated
    * using the default bounding volume hierarchy of each mesh.  The results
    * are returned in the array <code>intersections</code>, which contains
    * information on each detected pair of intersecting triangles.
    * <p>
    * This method detects only face (triangle) intersections; it does
    * not detect if one mesh is completely inside the other.
    *
    * @param intersections returns information for each pair of intersecting
    * triangles.
    * @param mesh1 first mesh to be intersected
    * @param mesh2 second mesh to be intersected
    * @return true if intersecting faces are detected
    */
   public boolean intersectMeshMesh (
      ArrayList<TriTriIntersection> intersections,
      PolygonalMesh mesh1, PolygonalMesh mesh2) {

      if (!mesh1.isTriangular()) {
         throw new IllegalArgumentException ("mesh1 is not triangular");
      }
      if (!mesh2.isTriangular()) {
         throw new IllegalArgumentException ("mesh2 is not triangular");
      }
      return intersectMeshMesh (
         intersections, mesh1.getBVTree(), mesh2.getBVTree());
   }

   /**
    * Intersects the faces of two triangular meshes, whose faces are contained
    * within supplied bounding volume hierarchies.  The results are returned in
    * the array <code>intersections</code>, which contains information on each
    * detected pair of intersecting triangles.
    *
    * <p> This method detects only face (triangle) intersections; it does not
    * detect if one mesh is completely inside the other.
    *
    * @param intersections returns information for each pair of intersecting
    * triangles.
    * @param bvh1 bounding volume hierarchy for the first mesh to be intersected
    * @param bvh2 bounding volume hierarchy for the second mesh to be intersected
    * @return true if intersecting faces are detected
    */
   public boolean intersectMeshMesh (
      ArrayList<TriTriIntersection> intersections,
      BVTree bvh1, BVTree bvh2) {

      double tol = Math.min(bvh1.getRadius()*EPS, bvh1.getRadius()*EPS);
      myTriIntersector.setEpsilon(tol);
      
      RigidTransform3d X21 = new RigidTransform3d();
      X21.mulInverseLeft (bvh1.getBvhToWorld(), bvh2.getBvhToWorld());
      if (X21.equals (RigidTransform3d.IDENTITY)) {
         X21 = RigidTransform3d.IDENTITY;
      }
      ArrayList<BVNode> nodes1 = new ArrayList<BVNode>();
      ArrayList<BVNode> nodes2 = new ArrayList<BVNode>();

      bvh1.intersectTree (nodes1, nodes2, bvh2, X21);
      for (int i=0; i<nodes1.size(); i++) {
         intersectBoundingVolumeTriangles (
            intersections, nodes1.get(i), nodes2.get(i), X21);
      }

      RigidTransform3d X1W = bvh1.getBvhToWorld();
      if (X1W != RigidTransform3d.IDENTITY) {
         // convert intersection points to world coordinates
         for (int i=0; i<intersections.size(); i++) {
            TriTriIntersection isect = intersections.get(i);
            for (int k=0; k<isect.points.length; k++) {
               isect.points[k].transform (X1W);
            }
         }
      }
      return intersections.size() != 0;
   }

   /**
    * Intersects the faces of a triangular mesh with a plane. The process is
    * accelerated using the default bounding volume hierarchy of the mesh.
    * The results are returned in the array <code>intersections</code>, which
    * contains information on each detected face-plane intersection.
    *
    * @param intersections returns information for each face-plane intersection.
    * @param mesh the mesh to be intersected
    * @param plane the plane to be intersected
    * @return true if the mesh and the plane intersect
    */
   public boolean intersectMeshPlane (
      ArrayList<TriPlaneIntersection> intersections,
      PolygonalMesh mesh, Plane plane) {

      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      return intersectMeshPlane (
         intersections, mesh.getBVTree(), plane);
   }

   /**
    * Intersects the faces of a triangular mesh with a plane. The faces
    * of the mesh are contained within a supplied bounding volume hierarchy.
    * The results are returned in the array <code>intersections</code>, which
    * contains information on each detected face-plane intersection.
    *
    * @param intersections returns information for each face-plane intersection.
    * @param bvh bounding volume hierarchy containing the mesh faces
    * @param plane the plane to be intersected
    * @return true if the mesh and the plane intersect
    */
   public boolean intersectMeshPlane (
      ArrayList<TriPlaneIntersection> intersections,
      BVTree bvh, Plane plane) {

      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      bvh.intersectPlane (nodes, plane);

      double eps = bvh.getRadius()*EPS;
      myTriIntersector.setEpsilon (eps);

      if (bvh.getBvhToWorld() != RigidTransform3d.IDENTITY) {
         plane = new Plane (plane);
         plane.inverseTransform (bvh.getBvhToWorld());
      }      

      for (int i=0; i<nodes.size(); i++) {
         intersectBoundingVolumeTriangles (intersections, nodes.get(i), plane);
      }
      RigidTransform3d X1W = bvh.getBvhToWorld();
      if (X1W != RigidTransform3d.IDENTITY) {
         // convert intersection points to world coordinates
         for (int i=0; i<intersections.size(); i++) {
            TriPlaneIntersection isect = intersections.get(i);
            for (int k=0; k<isect.points.length; k++) {
               isect.points[k].transform (X1W);
            }
         }
      }
      return intersections.size() != 0;
   }
   
   /**
    * Intersects the faces of a triangular mesh with a line. The process is
    * accelerated using the default bounding volume hierarchy of the mesh.
    * The results are returned in the array <code>intersections</code>, which
    * contains information on each detected face-line intersection.
    *
    * @param intersections returns information for each line-plane intersection.
    * @param mesh the mesh to be intersected
    * @param line the line to be intersected
    * @return true if the mesh and the line intersect
    */
   public boolean intersectMeshLine (
      ArrayList<TriLineIntersection> intersections,
      PolygonalMesh mesh, Line line) {

      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      return intersectMeshLine (
         intersections, mesh.getBVTree(), line);
   }

   /**
    * Intersects the faces of a triangular mesh with a line. The faces
    * of the mesh are contained within a supplied bounding volume hierarchy.
    * The results are returned in the array <code>intersections</code>, which
    * contains information on each detected face-line intersection.
    *
    * @param intersections returns information for each face-line intersection.
    * @param bvh bounding volume hierarchy containing the mesh faces
    * @param line the line to be intersected
    * @return true if the mesh and the line intersect
    */
   public boolean intersectMeshLine (
      ArrayList<TriLineIntersection> intersections,
      BVTree bvh, Line line) {

      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      bvh.intersectLine(nodes, line.getOrigin(), line.getDirection(), 
         Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY); 

      double eps = bvh.getRadius()*EPS;
      myTriIntersector.setEpsilon (eps);

      if (bvh.getBvhToWorld() != RigidTransform3d.IDENTITY) {
         line = new Line (line);
         line.inverseTransform (bvh.getBvhToWorld());
      }      

      for (int i=0; i<nodes.size(); i++) {
         intersectBoundingVolumeTriangles (intersections, nodes.get(i), line);
      }
      RigidTransform3d X1W = bvh.getBvhToWorld();
      if (X1W != RigidTransform3d.IDENTITY) {
         // convert intersection points to world coordinates
         for (int i=0; i<intersections.size(); i++) {
            TriLineIntersection isect = intersections.get(i);
            for (int k=0; k<isect.points.length; k++) {
               isect.points[k].transform (X1W);
            }
         }
      }
      return intersections.size() != 0;
   }

   void intersectBoundingVolumeTriangles (
      ArrayList<TriTriIntersection> intersections,
      BVNode node1, BVNode node2, RigidTransform3d X21) {

      Boundable[] elems1 = node1.getElements();
      Boundable[] elems2 = node2.getElements();

      for (int k0 = 0; k0 < elems1.length; k0++) {

         if (elems1[k0] instanceof Face) {
            Face face0 = (Face)elems1[k0];

            HalfEdge he;
            he = face0.firstHalfEdge();
            Point3d p0 = he.head.pnt;
            he = he.getNext();
            Point3d p1 = he.head.pnt;
            he = he.getNext();
            Point3d p2 = he.head.pnt;

            for (int k1 = 0; k1 < elems2.length; k1++) {
               if (elems2[k1] instanceof Face) {
                  Face face1 = (Face)elems2[k1];
                  he = face1.firstHalfEdge();
                  myP0.transform (X21, he.head.pnt);
                  he = he.getNext();
                  myP1.transform (X21, he.head.pnt);
                  he = he.getNext();
                  myP2.transform (X21, he.head.pnt);

                  Point3d[] points =
                     myTriIntersector.intersectTriangleTriangle (
                        p0, p1, p2, myP0, myP1, myP2);
                  if (points != null) {
                     // intersections are now in coords of first mesh
                     intersections.add (
                        new TriTriIntersection (
                           face0, face1, points));
                  }
               }
            }
         }
      }

   }

   void intersectBoundingVolumeTriangles (
      ArrayList<TriLineIntersection> intersections,
      BVNode node, Line l) {

      Boundable[] elems = node.getElements();
      Vector3d duv = new Vector3d();
      Vector3d dir = l.getDirection();
      Vector3d rdir = new Vector3d(-dir.x, -dir.y, -dir.z);
      Point3d orig = l.getOrigin();
      Point3d p = new Point3d();

      for (int k = 0; k < elems.length; k++) {

         if (elems[k] instanceof Face) {
            Face face = (Face)elems[k];

            HalfEdge he;
            he = face.firstHalfEdge();
            Point3d p0 = he.head.pnt;
            he = he.getNext();
            Point3d p1 = he.head.pnt;
            he = he.getNext();
            Point3d p2 = he.head.pnt;

            int isect = myTriIntersector.intersect(p0, p1, p2, orig, dir, duv);
            int isect2 = myTriIntersector.intersect(p0, p1, p2, orig, rdir, duv);
               //myTriIntersector.intersectTrianglePlane (p0, p1, p2, p);

            if (isect > 0) {
               ArrayList<Point3d> points = new ArrayList<Point3d>(1);
               ArrayList<Vector2d> coords = new ArrayList<Vector2d>(1);
               p.setZero();
               p.scaledAdd(1-(duv.y+duv.z), p0);
               p.scaledAdd(duv.y, p1);
               p.scaledAdd(duv.z, p2);
               points.add(p);
               coords.add(new Vector2d(duv.y, duv.z));
               intersections.add (
                  new TriLineIntersection (face, l, points, coords));
            }
         }
      }
   }
   
   
   void intersectBoundingVolumeTriangles (
      ArrayList<TriPlaneIntersection> intersections,
      BVNode node, Plane p) {

      Boundable[] elems = node.getElements();

      for (int k = 0; k < elems.length; k++) {

         if (elems[k] instanceof Face) {
            Face face = (Face)elems[k];

            HalfEdge he;
            he = face.firstHalfEdge();
            Point3d p0 = he.head.pnt;
            he = he.getNext();
            Point3d p1 = he.head.pnt;
            he = he.getNext();
            Point3d p2 = he.head.pnt;

            ArrayList<Point3d> points =
               myTriIntersector.intersectTrianglePlane (p0, p1, p2, p);

            if (points != null && points.size() > 0) {
               intersections.add (
                  new TriPlaneIntersection (
                     face, p, points));
            }
         }
      }
   }

   
   
   /**
    * Intersects a {@link PolygonalMesh} with a plane, returning the set of
    * intersection contours. 
    * 
    * @param mesh mesh to intersect
    * @param plane plane to intersect with
    * @param tol tolerance within which points are considered identical
    * @return an array of contours, each described by a linked-list of points.  
    * For closed curves, the first and last point in the linked-list are the same
    * object
    */
   public ArrayList<LinkedList<Point3d>> intersectMeshPlane (
      PolygonalMesh mesh, Plane plane, double tol) {
      
      ArrayList<TriPlaneIntersection> intersections = 
         new ArrayList<TriPlaneIntersection>();
      intersectMeshPlane (intersections, mesh, plane);
      return buildContours(intersections, tol);
   }
   
   // merges a set of triangle-plane intersections into a contour(s)
   public ArrayList<LinkedList<Point3d>> buildMeshContours (
      List<TriTriIntersection> intersections, double tol) {
      
      // expecting only one contour
      ArrayList<LinkedList<Point3d>> contours =
         new ArrayList<LinkedList<Point3d>>(1);
      
      LinkedList<TriTriIntersection> remaining =
         new LinkedList<TriTriIntersection>();
      
      // initialize with all 2-point intersection
      for (TriTriIntersection tti : intersections) {
         
         if (tti.numPoints() == 2) {
            addIfUnique(tti, remaining, tol);
         }
      }
      
      // no contours
      while (remaining.size() > 0) {
         LinkedList<Point3d> contour = new LinkedList<Point3d>();
         TriTriIntersection tpi = remaining.getFirst();
         
         contour.addFirst(tpi.points[0]);
         contour.addLast(tpi.points[1]);
         remaining.removeFirst();
         
         Iterator<TriTriIntersection> rit;
         boolean closed = false;
         boolean didAttach = true;
         boolean attach = false;
         while (didAttach && !closed) {
            didAttach = false;
            rit = remaining.iterator();
            
            while (rit.hasNext()) {
               tpi = rit.next();
               attach = attachSegment(contour, tpi.points[0], tpi.points[1], tol);
               if (attach) {
                  rit.remove();
                  didAttach = true;
                  closed = isContourClosed(contour, tol);
               }
            }
         }
         
         if (closed) {
            // replace last with first for better conditioning
            contour.removeLast(); 
            contour.addLast(contour.getFirst());
         }
         
         contours.add(contour);
         
      }
      
      return contours;
   }
   
   // merges a set of triangle-plane intersections into a contour(s)
   public ArrayList<LinkedList<Point3d>> buildContours (
      List<TriPlaneIntersection> intersections, double tol) {
      
      // expecting only one contour
      ArrayList<LinkedList<Point3d>> contours =
         new ArrayList<LinkedList<Point3d>>(1);
      
      LinkedList<TriPlaneIntersection> remaining =
         new LinkedList<TriPlaneIntersection>();
      
      // initialize with all 2-point intersection
      for (TriPlaneIntersection tpi : intersections) {
         if (tpi.numPoints() == 2) {
            addIfUnique(tpi, remaining, tol);
         }
      }
      
      // no contours
      while (remaining.size() > 0) {
         LinkedList<Point3d> contour = new LinkedList<Point3d>();
         TriPlaneIntersection tpi = remaining.getFirst();
         
         contour.addFirst(tpi.points[0]);
         contour.addLast(tpi.points[1]);
         remaining.removeFirst();
         
         Iterator<TriPlaneIntersection> rit;
         boolean closed = false;
         boolean didAttach = true;
         boolean attach = false;
         while (didAttach && !closed) {
            didAttach = false;
            rit = remaining.iterator();
            
            while (rit.hasNext()) {
               tpi = rit.next();
               attach = attachSegment(contour, tpi.points[0], tpi.points[1], tol);
               if (attach) {
                  rit.remove();
                  didAttach = true;
                  closed = isContourClosed(contour, tol);
               }
            }
         }
         
         if (closed) {
            // replace last with first for better conditioning
            contour.removeLast(); 
            contour.addLast(contour.getFirst());
         }
         
         contours.add(contour);
         
      }
      
      return contours;
   }
   
   // only adds to the contour if the section is unique
   private boolean addIfUnique (
      TriPlaneIntersection tpi, List<TriPlaneIntersection> list,
      double tol) {
      
      for (TriPlaneIntersection t : list) {
         if (tpi.points[0].distance(t.points[0]) < tol &&
            tpi.points[1].distance(t.points[1])<tol ) {
            return false;
         } else if (tpi.points[0].distance(t.points[1]) < tol &&
            tpi.points[1].distance(t.points[0])<tol ) {
            return false;
         }
      }
      
      list.add(tpi);
      return true;
      
   }
   
   // only adds to the contour if the section is unique
   private boolean addIfUnique (
      TriTriIntersection tpi, List<TriTriIntersection> list,
      double tol) {
      
      for (TriTriIntersection t : list) {
         if (tpi.points[0].distance(t.points[0]) < tol &&
            tpi.points[1].distance(t.points[1])<tol ) {
            return false;
         } else if (tpi.points[0].distance(t.points[1]) < tol &&
            tpi.points[1].distance(t.points[0])<tol ) {
            return false;
         }
      }
      
      list.add(tpi);
      return true;
      
   }
   
   // checks if contour is closed
   private boolean isContourClosed (
      LinkedList<Point3d> contour, double tol) {
      if (contour.getFirst().distance(contour.getLast()) < tol) {
         return true;
      }
      return false;
   }
   
   // adds to the contour
   private boolean attachSegment(
      LinkedList<Point3d> contour, Point3d a, Point3d b, double tol) {
      
      if (contour.getFirst().distance(a) < tol) {
         contour.addFirst(b);
         return true;
      }
      if (contour.getFirst().distance(b) < tol) {
         contour.addFirst(a);
         return true;
      }
      if (contour.getLast().distance(a) < tol) {
         contour.addLast(b);
         return true;
      }
      if (contour.getLast().distance(b) < tol) {
         contour.addLast(a);
         return true;
      } 
      return false;
   }
}
