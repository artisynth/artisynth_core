package maspack.collision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import maspack.geometry.*;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.Vertex3d;
import maspack.matrix.*;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;

public class ContactPlane {
   // triangle triangle intersections
   public ArrayList<TriTriIntersection> intersections =
      new ArrayList<TriTriIntersection>();

   // contact points
   // public ArrayList<Vertex3d> vertices0 = new ArrayList<Vertex3d>();
   // public ArrayList<Vertex3d> vertices1 = new ArrayList<Vertex3d>();

   /*
    * contour is a list of edge-face intersection points representing a closed
    * loop intersection between two matching regions of mesh.
    */
   public IntersectionContour contour;
   
   /* points is a subset of the contour points, reduced to a pseudo-convex hull. */
   public ArrayList<Point3d> points; // = new ArrayList<Point3d>();

   /*
    * Use mPoints temporarily while SurfaceMeshCollider and MeshCollider
    * coexist. The former requires points to be ArrayList<IntersectionPoint>
    * and the latter requires points to be ArrayList<Point3d>
    */
   public ArrayList<IntersectionPoint> mPoints =
      new ArrayList<IntersectionPoint>();

   /*
    * normal and centroid define a plane which is an approximate fit to the
    * contour points. It is generated using an area-weighted average of cross
    * products of radius vectors of neighbouring points. Normal should point
    * from mesh1 to mesh0 (which are the args to
    * SurfaceMeshCollider.getContacts(...).
    */
   public Vector3d normal = new Vector3d();
   public Point3d centroid = new Point3d (0, 0, 0);

   /*
    * depth is the maximum of: - distance between two planes parallel to the
    * fitted plane which contain all the intersection points between them. -
    * maximum distance from a penetrating vertex to an opposing face, along a
    * direction parallel to the normal of the fitted plane.
    */
   public double depth = 0;
   public double minProjectedDistance = Double.POSITIVE_INFINITY;
   public double maxProjectedDistance = Double.NEGATIVE_INFINITY;

   /*
    * Point tolerance specified by whoever's using the collider. Points returned
    * in points field exclude one of any pair closer than this value.
    */
   double pointTolerance;

   /*
    * Used to reject points that are too close together to be useful in
    * determining a normal.
    */
   private double epsilonPointTolerance = 1e-8;

   /**
    * Estimate of the contact area per point, or -1 if no estimate is
    * available. This should be approximately the area of the contact region
    * divided by the number of points.
    */
   public double contactAreaPerPoint = -1;

   public ContactPlane() {
   }

   double sTotal;

   public boolean build (
      PenetrationRegion region0, PenetrationRegion region1, 
      PolygonalMesh mesh0, double pointTol) {
      contour = null;
      if (region0.myContours.size() == 1) {
         IntersectionContour c = region0.getFirstContour();
         if (c.isClosed && c.size() > 1) {
            contour = region0.getFirstContour();
         }
      }
      else {
         Vector3d areaVec = new Vector3d();
         Vector3d centroid = new Vector3d();
         double maxa = 0;
         IntersectionContour maxc = null;
         for (IntersectionContour c : region0.myContours) {
            if (c.isClosed && c.size() >= 3) {
               c.fitPlane (areaVec, centroid, pointTol);
               double a = areaVec.norm();
               if (a > maxa) {
                  maxc = c;
                  maxa = a;
               }
            }
         }
         if (maxc == null) {
            for (IntersectionContour c : region0.myContours) {
               if (c.isClosed && c.size() > 1) {
                  maxc = c;
                  break;
               }
            }            
         }
         if (maxc != null) {
            contour = maxc;
         }
      }
      pointTolerance = pointTol;
      if (contour != null) {
         return compute(region0, region1, mesh0);
      }
      else {
         return false;
      }
   }

   double turn (Point3d p0, Point3d p1, Point3d p2, Vector3d nrm) {
      Vector3d del01 = new Vector3d();
      Vector3d del12 = new Vector3d();
      Vector3d xprod = new Vector3d();

      del01.sub (p1, p0);
      del12.sub (p2, p1);
      xprod.cross (del01, del12);
      return xprod.dot (nrm);
   }

   boolean compute (
      PenetrationRegion region0, PenetrationRegion region1, 
      PolygonalMesh mesh0) {

      Vector3d areaVec = new Vector3d();
      mPoints = contour.fitPlane (areaVec, centroid, epsilonPointTolerance);

      if (mPoints.size() == 0) {
         points = new ArrayList<Point3d>();
         return false;
      }
      normal = new Vector3d();

      if (mPoints.size() >= 3) {
         normal.normalize (areaVec);
         //normal.negate();
         // Fit a plane to the contour.
         if (!normalContact (region0, region1, mesh0)) {
            return false;
         }
      }
      else {
         int v0s = region0.numVertices();
         int v1s = region1.numVertices();
         int vTot = v0s + v1s;
    
         if ((v0s == 0 || v1s == 0) & (vTot == 1 || vTot == 2)) {
            if (!vertexFaceContact(region0, region1)) {
               return false;
            }
         }
         else {
            if (vTot == 0) {
               if (!edgeEdgeContact(region0, region1, mesh0)) {
                  return false;
               }
            }
            else {
               /*
                * Insufficient contour points for a plane, and one or both of
                * the following is true: - both regions have penetrating
                * vertices, and/or - there are more than 2 penetrating vertices
                * total.
                */
               System.out.println (
                  "non-edge-edge contact with v0s=" + v0s +
                  " v1s=" + v1s + " f0s=" + region0.numFaces() +
                  " f1s=" + region1.numFaces());
               return false;
            }
         }
      }

      /*
       * depth was computed to be the max distance between opposing features
       * along the normal. Divide by 2 so it is now the distance the bodies
       * should each move in order to separate.
       */
      depth *= 0.5;

      /* Filter the points to remove one of any pair <i>closer</i> than
       * pointTolerance. */
      Vector3d dist = new Vector3d();
      points = new ArrayList<Point3d>();
      Point3d p1 = mPoints.get (mPoints.size() - 1); // last point -- it's a
                                             // neighbour of the first.
      for (IntersectionPoint mip : mPoints) {
         dist.sub (p1, mip);
         if (dist.norm() >= pointTolerance) {
            points.add (mip);
            p1 = mip;
         }
      }
      // if all points are too close just use the centroid
      if (points.size() == 0) {
         points.add (centroid);
      }
      // estimate the contact area. Use the average of the two region
      // areas to get the total area.
      contactAreaPerPoint =
         0.5*(region0.getArea()+region1.getArea())/points.size();
      return true;
   }

   /*
    * Handle the case where there are insufficient contact points in the contour
    * to define a plane. and there are no penetrating vertices in either region.
    */
   boolean edgeEdgeContact (
      PenetrationRegion region0, PenetrationRegion region1, 
      PolygonalMesh mesh0) {
      HashSet<HalfEdge> edges = new HashSet<HalfEdge>();
      for (IntersectionPoint mip : contour)
         edges.add (mip.edge);
      if (edges.size() != 2) {
         /*
          * This can happen if a penetrating vertex goes right through the other
          * object and out the other side, or if an edge is co-planar with a
          * face.
          */
         System.out.println (
            "unknown contact type: edge-edge with " + edges.size() + " edges");
         // printDiagnostics(vertices0, vertices1);
         // throw new InternalErrorException("unknown contact type: edge-edge
         // with "+edges.size()+" edges");
         return false;
      }
      HalfEdge edge0, edge1;
      Iterator<HalfEdge> itr = edges.iterator();
      edge0 = itr.next();
      edge1 = itr.next();
      Vector3d vedge0 = new Vector3d(), vedge1 = new Vector3d();
      vedge0.sub (edge0.getHead().getWorldPoint(),
                  edge0.getTail().getWorldPoint());
      vedge1.sub (edge1.getHead().getWorldPoint(),
                  edge1.getTail().getWorldPoint());
      normal.cross (vedge0, vedge1);
      normal.normalize();

      /* Compute depth=perpendicular distance between the two edges. */
      Vector3d c = new Vector3d();
      c.sub (edge1.getTail().getWorldPoint(),
             edge0.getTail().getWorldPoint());
      vedge0.cross (vedge0, vedge1);
      depth = Math.abs (c.dot (vedge0)) / vedge0.norm();
      minProjectedDistance = maxProjectedDistance = 0;
      checkNormalDirection (
         region0.getFaces(), region1.getFaces(), mesh0);
      return true;
   }

   /*
    * Make sure the normal points the right direction, ie. the direction of a
    * force which will push mesh0 away from mesh1. Find points p0 and p1 which
    * are in the interior of a face in mesh0 and mesh1, respectively. Then the
    * normal must have a positive component in the direction p1 - p0. Use the
    * total of p1 - p0 over all pairs of interlocking triangles in the contour,
    * as some pairs p0, p1 may be on a plane perpendicular to the normal.
    * 
    * Use when there are no penetrating vertices, which provide a better way of
    * determining sense of direction.
    */
   void checkNormalDirection (
      LinkedHashSet<Face> faces0, LinkedHashSet<Face> faces1,
      PolygonalMesh mesh0) {
      
      Vector3d tmp = new Vector3d(), tot = new Vector3d();
      IntersectionPoint c0 = contour.get (contour.size()-1);
      boolean c0EdgeOnMesh0 = 
         SurfaceMeshIntersector.edgeOnMesh (c0.edge, mesh0);
      
      /*
       * Find each pair of interlocking triangles, one from each region, and add
       * its contribution to the total.
       */
      for (IntersectionPoint c1 : contour) {
         boolean c1EdgeOnMesh0 = 
            SurfaceMeshIntersector.edgeOnMesh (c1.edge, mesh0);
         if (c0EdgeOnMesh0 != c1EdgeOnMesh0) {
            tmp.sub (c1, c0);
            // edgeRegion=true ==> edge is mesh0, face is mesh1,
            // c0 is in interior of a face in mesh1, c1 is in interior of a face
            // in mesh0
            if (c0EdgeOnMesh0) {
               tot.add (tmp);
            }
            else {
               tot.sub (tmp);
            }
         }
         c0EdgeOnMesh0 = c1EdgeOnMesh0;
         c0 = c1;
      }
      if (normal.dot (tot) < 0) {
         negateNormal();
      }
   }

   /*
    * Handle the case where there are insufficient contact points in the contour
    * to define a plane. and one region has 1 or 2 penetrating vertices while
    * the other region has none.
    */
   boolean vertexFaceContact(
      PenetrationRegion region0, PenetrationRegion region1) {
      LinkedHashSet<Face> fs;
      LinkedHashSet<Vertex3d> vs;

      if (region0.numVertices() > 0) {
         vs = region0.myVertices;
         fs = region1.getFaces();
      }
      else {
         vs = region1.myVertices;
         fs = region0.getFaces();
      }
      depth = 0;
      for (Vertex3d v : vs) {
         Face f = fs.iterator().next(); /*
                                           * Assume the vertex contacted the
                                           * first face. For more accurate
                                           * results, we could choose the face
                                           * for each vertex more carefully.
                                           */
         normal.set (f.getWorldNormal());
         double d =
            Math.abs (f.getPoint0DotNormal()
            - (normal.dot (v.getWorldPoint()))); // distance from vertex to face
         depth = Math.max (depth, d);
         minProjectedDistance = maxProjectedDistance = 0;
      }
      return true;
   }

   /*
    * Handle the case where there are enough contact points in the contour to
    * define a plane.
    */
   boolean normalContact(
      PenetrationRegion region0, PenetrationRegion region1,
      PolygonalMesh mesh0) {

      /*
       * Project each point into the plane. Calculate the radius of the
       * projected point from the centroid. Also calculate the maximum distance
       * of a mesh intersection point from either side of the plane. This
       * maximum depth includes all points, even concave ones which may later be
       * removed from the contour.
       */
      Point3d proj = new Point3d();
      for (IntersectionPoint mip : mPoints) {
         proj.sub (mip, centroid);
         double s = proj.dot (normal); // signed distance from plane to the mip
                                       // along normal
         if (s < minProjectedDistance) {
            minProjectedDistance = s;
         }
         if (s > maxProjectedDistance) {
            maxProjectedDistance = s;
         }
         proj.scaledAdd (-s, normal);
         //mip.radius = proj.norm();
      }

      /*
       * Distance between two planes parallel to the fitted plane which contain
       * all the intersection points between them.
       */
      depth = maxProjectedDistance - minProjectedDistance;

      /*
       * Remove concave points to make the points a convex hull. A point p1 is
       * concave with respect to the preceding and following points p0 and p2
       * if the point sequence p0, p1, p2 forms a right turn with respect to
       * the normal.
       */
      boolean removedPoint;
      Vector3d xprod = new Vector3d();
      do {
         removedPoint = false;
         IntersectionPoint p0;
         IntersectionPoint p1 = mPoints.get (0);
         IntersectionPoint p2 = mPoints.get (1);
         int i = 2;
         for (int k = 1; k <= mPoints.size(); k++) {
            p0 = p1;
            p1 = p2;
            p2 = mPoints.get (i%mPoints.size());
            if (turn (p0, p1, p2, normal) < 0) {
               mPoints.remove ((i-1)%mPoints.size());
               removedPoint = true;
               p1 = p0;              
            }
            else {
               i++;
            }
         }
      }
      while (removedPoint);
      
      /*
       * Adjust depth so it is greater than or equal to the maximum distance
       * from a vertex of either region to the opposing face of that vertex.
       * 
       * Also try to figure out which way the normal should point. It's
       * required by RigidBodyContact to point in the direction of the force to
       * be applied to mesh0 to stop it from penetrating mesh1. For
       * pathological contours this may be ambiguous, but in the simple case
       * where the contour fits well to a plane, the ContactRegion normal
       * should point from the penetrating vertices of mesh0 to the opposing
       * faces of mesh1.
       * 
       * If the contour is too confusing to distinguish a direction then throw
       * an error.
       */
      if (region0.numVertices() + region1.numVertices() == 0) {
         checkNormalDirection (
            region0.getFaces(), region1.getFaces(), mesh0);
      }
      else {
         double dTotal = 0;
         BVFeatureQuery query = new BVFeatureQuery();
         Point3d wpnt = new Point3d();
         Point3d nearest = new Point3d();
         Vector2d coords = new Vector2d();
         Vector3d diff = new Vector3d();
         
         for (Vertex3d v : region0.myVertices) {
            v.getWorldPoint (wpnt);
            if (query.isInsideOrientedMesh (region1.myMesh, wpnt, 0)) {
               query.getFaceForInsideOrientedTest (nearest, coords);
               region1.myMesh.transformToWorld (nearest);
               diff.sub (wpnt, nearest);
               
               double d = diff.dot(normal);
               dTotal += d;
               if (d < 0) {
                  d = -d;
               }
               if (d > depth) {
                  depth = d;
               }
            }               
         }
         
         for (Vertex3d v : region1.myVertices) {
            v.getWorldPoint (wpnt);
            if (query.isInsideOrientedMesh (region0.myMesh, wpnt, 0)) {
               query.getFaceForInsideOrientedTest (nearest, coords);
               region0.myMesh.transformToWorld (nearest);
               diff.sub (wpnt, nearest);

               double d = diff.dot(normal);
               dTotal -= d;
               if (d < 0) {
                  d = -d;
               }
               if (d > depth) {
                  depth = d;
               }
            }               
         }
         if (dTotal > 0) {
            negateNormal();
         }
      }
      return true;
   }

   void negateNormal() {
      normal.negate();
      minProjectedDistance = -minProjectedDistance;
      maxProjectedDistance = -maxProjectedDistance;
   }

//   /*
//    * Calculate the distance from an origin, along the normal, to a face. Do
//    * nothing if the ray from the origin along the normal does not intersect the
//    * face. Set depth to this distance if it is greater than the current depth.
//    */
//   void checkDistanceToFace (Point3d origin, Face face) {
//      Vector3d n = face.getWorldNormal();
//      double s = normal.dot (n);
//      if (Math.abs (s) < 1e-7)
//         return;
//      s = (face.getPoint0DotNormal() - origin.dot (n)) / s;
//      double x = normal.x * s + origin.x;
//      double y = normal.y * s + origin.y;
//      double z = normal.z * s + origin.z;
//      if (face.isPointInside (x, y, z)) {
//         sTotal = sTotal + s; /*
//                               * Use the total of s as an average measure of
//                               * which direction the faces are relative to the
//                               * vertices, along the normal.
//                               */
//         if (s < 0)
//            s = -s;
//         if (depth < s)
//            depth = s;
//         // System.out.println(contactInfo.mesh0.name+"
//         // "+contactInfo.mesh1.name+" newDepth="+depth);
//      }
//   }

   void render (Renderer renderer, int flags) {

      renderer.setColor (0f, 0f, 1f);
      if (contour != null) {
         contour.render (renderer, flags); // draw the contour
      }

      renderer.setColor (0f, 1f, 1f);
      renderer.setLineWidth (4);
      renderer.beginDraw (DrawMode.LINE_LOOP);
      for (Point3d p : points)
         renderer.addVertex (p); // draw convex hull with a heavier line
      renderer.endDraw();
      renderer.setLineWidth (1);

      renderer.setPointSize (10);
      renderer.beginDraw (DrawMode.POINTS); // emphasize the convex-hull points in case
                                 // they are same as contour.
      for (Point3d p : points)
         renderer.addVertex (p);
      renderer.endDraw();
      renderer.setPointSize (1);

      renderer.setLineWidth (4);
      renderer.setColor (0f, 1f, 0f); // draw a line through centroid along the normal
      Point3d n0 = new Point3d();
      Point3d n1 = new Point3d();
      n0.set (centroid);
      n0.scaledAdd (minProjectedDistance, normal);
      n1.set (centroid);
      n1.scaledAdd (0.5, normal);
      renderer.drawLine (n0, n1);
      renderer.setLineWidth (1);

      renderer.setPointSize (15);
      renderer.beginDraw (DrawMode.POINTS);
      n1.set (centroid);
      renderer.addVertex (n1);
      n1.scaledAdd (0.5, normal);
      renderer.addVertex (n1);
      n1.set (centroid);
      n1.scaledAdd (minProjectedDistance, normal);
      renderer.addVertex (n1);
      n1.set (centroid);
      n1.scaledAdd (maxProjectedDistance, normal);
      renderer.addVertex (n1);
      if (depth > maxProjectedDistance - minProjectedDistance) {
         n1.set (centroid);
         n1.scaledAdd (depth, normal);
         renderer.addVertex (n1);
      }
      renderer.endDraw();
      renderer.setPointSize (1);
   }
}
