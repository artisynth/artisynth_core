package maspack.collision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.media.opengl.GL2;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.GL.GL2.GL2Viewer;

public class ContactRegion {
   // triangle triangle intersections
   public ArrayList<TriTriIntersection> intersections =
      new ArrayList<TriTriIntersection>();

   // contact points
   // public ArrayList<Vertex3d> vertices0 = new ArrayList<Vertex3d>();
   // public ArrayList<Vertex3d> vertices1 = new ArrayList<Vertex3d>();

   /*
    * contour is a list of edge-face intersection points representing a closed
    * loop intersection between two opposing regions of mesh.
    */
   public MeshIntersectionContour contour;

   /* points is a subset of the contour points, reduced to a pseudo-convex hull. */
   public ArrayList<Point3d> points; // = new ArrayList<Point3d>();

   /*
    * Use mPoints temporarily while SurfaceMeshCollider and MeshCollider
    * coexist. The former requires points to be ArrayList<MeshIntersectionPoint>
    * and the latter requires points to be ArrayList<Point3d>
    */
   public ArrayList<MeshIntersectionPoint> mPoints =
      new ArrayList<MeshIntersectionPoint>();

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
   private double sTotal0To1, sTotal;

   /*
    * This is set to false in compute() if the region is degenerate, ie. does
    * not have enough information on which to base a collision response.
    */
   public boolean isValid = true;
   public ContactInfo contactInfo;

   public ContactRegion() {
   }

   public ContactRegion (MeshIntersectionContour aContour,
   ContactInfo aContactInfo, double pointTol) {
      contour = aContour;
      contactInfo = aContactInfo;
      pointTolerance = pointTol;
      compute();
   }

   MeshIntersectionPoint getPoint (int i) { // Allow index wraparound.
      if (i >= mPoints.size())
         return mPoints.get (i - mPoints.size());
      else
         return mPoints.get (i);
   }

   MeshIntersectionPoint removePoint (int i) { // Allow index wraparound.
      if (i >= mPoints.size())
         return mPoints.remove (i - mPoints.size());
      else
         return mPoints.remove (i);
   }

   void compute() {
      /*
       * Remove contour points that are closer than pointTolerance to a
       * neighbour
       */
      MeshIntersectionPoint p1;
      Vector3d dist = new Vector3d();
      p1 = contour.get (contour.size() - 1); // last point -- it's a
                                             // neighbour of the first.
      for (MeshIntersectionPoint mip : contour) {
         dist.sub (p1, mip);
         if (dist.norm() >= epsilonPointTolerance) {
            mPoints.add (mip);
            p1 = mip;
         }
      }

      if (mPoints.isEmpty()) {
         points = new ArrayList<Point3d>();
         return;
      }

      /* Compute the centroid of all the points. */
      for (MeshIntersectionPoint mip : mPoints)
         centroid.add (mip);
      centroid.scale (1.0d / mPoints.size());

      normal = new Vector3d();
      int v0s = contour.insideVertices0.size();
      int v1s = contour.insideVertices1.size();
      int vTot = v0s + v1s;

      if (mPoints.size() >= 3) {
         // Fit a plane to the contour.
         normalContact();
      }
      else {
         if ((v0s == 0 | v1s == 0) & (vTot == 1 | vTot == 2)) {
            vertexFaceContact();
         }
         else {
            if (vTot == 0) {
               edgeEdgeContact();
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
                  " v1s=" + v1s + " f0s=" + contour.insideFaces0.size() +
                  " f1s=" + contour.insideFaces1.size());
               isValid = false;
               // throw new InternalErrorException("non-edge-edge contact with
               // v0s="+v0s+" v1s="+v1s+" f0s="+faces0.size()+"
               // f1s="+faces1.size());
            }
         }
      }

      /*
       * depth was computed to be the max distance between opposing features
       * along the normal. Divide by 2 so it is now the distance the bodies
       * should each move in order to separate.
       */
      depth *= 0.5;

      /* Filter the points to remove one of any pair closer than pointTolerance. */
      points = new ArrayList<Point3d>();
      p1 = mPoints.get (mPoints.size() - 1); // last point -- it's a
                                             // neighbour of the first.
      for (MeshIntersectionPoint mip : mPoints) {
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
   }

   /*
    * Handle the case where there are insufficient contact points in the contour
    * to define a plane. and there are no penetrating vertices in either region.
    */
   void edgeEdgeContact() {
      HashSet<HalfEdge> edges = new HashSet<HalfEdge>();
      for (MeshIntersectionPoint mip : contour)
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
         isValid = false;
         return;
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
      checkNormalDirection (contour.insideFaces0, contour.insideFaces1);
      return;
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
      LinkedHashSet<Face> faces0, LinkedHashSet<Face> faces1) {
      Vector3d tmp = new Vector3d(), tot = new Vector3d();
      MeshIntersectionPoint c0 = contour.get (contour.size() - 1);
      /*
       * Find each pair of interlocking triangles, one from each region, and add
       * its contribution to the total.
       */
      for (MeshIntersectionPoint c1 : contour) {
         if (c0.edgeRegion != c1.edgeRegion) {
            tmp.sub (c1, c0);
            // edgeRegion=true ==> edge is mesh0, face is mesh1,
            // c0 is in interior of a face in mesh1, c1 is in interior of a face
            // in mesh0
            if (c0.edgeRegion)
               tot.add (tmp);
            else
               tot.sub (tmp);
         }
         c0 = c1;
      }
      if (normal.dot (tot) < 0)
         negateNormal();
   }

   /*
    * Handle the case where there are insufficient contact points in the contour
    * to define a plane. and one region has 1 or 2 penetrating vertices while
    * the other region has none.
    */
   void vertexFaceContact() {
      LinkedHashSet<Face> fs;
      ArrayList<Vertex3d> vs;

      if (contour.insideVertices0.size() > 0) {
         vs = contour.insideVertices0;
         fs = contour.insideFaces1;
      }
      else {
         vs = contour.insideVertices1;
         fs = contour.insideFaces0;
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
   }

   /*
    * Handle the case where there are enough contact points in the contour to
    * define a plane.
    */
   void normalContact() {
      /* For each point calculate a radius vector from the centroid to the point */
      for (MeshIntersectionPoint mip : mPoints) {
         mip.radiusVector = new Vector3d();
         mip.radiusVector.sub (mip, centroid);
      }
      /*
       * Fit a plane to the points. Plane goes through the centroid. Plane
       * normal is the average of the cross products of vectors from the
       * centroid to neighbouring points.
       */
      Vector3d cp = new Vector3d();
      int pSize = mPoints.size();
      MeshIntersectionPoint pLast = mPoints.get (pSize - 1);
      for (int i = 0; i < pSize; i++) {
         MeshIntersectionPoint pThis = mPoints.get (i);
         cp.cross (pThis.radiusVector, pLast.radiusVector);
         normal.add (cp);
         pThis.radiusArea = cp.norm(); // Use this area later to detect &
                                       // remove concavities in the contour.
         pLast = pThis;
      }
      normal.normalize();

      /*
       * Project each point into the plane. Calculate the radius of the
       * projected point from the centroid. Also calculate the maximum distance
       * of a mesh intersection point from either side of the plane. This
       * maximum depth includes all points, even concave ones which may later be
       * removed from the contour.
       */
      Point3d proj = new Point3d();
      for (MeshIntersectionPoint mip : mPoints) {
         proj.sub (mip, centroid);
         double s = proj.dot (normal); // signed distance from plane to the mip
                                       // along normal
         if (s < minProjectedDistance)
            minProjectedDistance = s;
         if (s > maxProjectedDistance)
            maxProjectedDistance = s;
         proj.scaledAdd (-s, normal);
         mip.radius = proj.norm();
      }

      /*
       * Distance between two planes parallel to the fitted plane which contain
       * all the intersection points between them.
       */
      depth = maxProjectedDistance - minProjectedDistance;

      /*
       * Remove concave points to make the points a convex hull. A point is
       * concave if the area of the triangle formed by the centroid and the
       * point's two neighbours is larger than the sum of the areas of the two
       * triangles formed by the point with the centroid and each of its two
       * neighbours.
       */
      boolean removedPoint;
      do {
         removedPoint = false;
         MeshIntersectionPoint p1;
         MeshIntersectionPoint p2 = mPoints.get (0);
         MeshIntersectionPoint p3 = mPoints.get (1);
         int i = 2;
         for (int k = 1; k <= mPoints.size(); k++) {
            p1 = p2;
            p2 = p3;
            p3 = getPoint (i);
            cp.cross (p3.radiusVector, p1.radiusVector);
            double combinedArea = cp.norm();
            if (combinedArea > (p3.radiusArea + p2.radiusArea)) {
               removePoint (i - 1);
               p3.radiusArea = combinedArea;
               removedPoint = true;
               p2 = p1;
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
       * Also try to figure out which way the normal should point. It's required
       * by RigidBodyContact to point in the direction of the force to be
       * applied to mesh0 to stop it from penetrating mesh1. For pathological
       * contours this may be ambiguous, but in the simple case where the
       * contour fits well to a plane, the ContactRegion normal should point
       * from the penetrating vertices of mesh0 to the opposing faces of mesh1.
       * 
       * If the contour is too confusing to distinguish a direction then throw
       * an error.
       */
      if (contour.insideVertices0.size() + contour.insideVertices1.size() == 0) {
         checkNormalDirection (contour.insideFaces0, contour.insideFaces1);
      }
      else {
         sTotal = 0;
         for (Vertex3d v : contour.insideVertices0)
            for (Face f : contour.insideFaces1)
               checkDistanceToFace (v.getWorldPoint(), f);
         sTotal0To1 = sTotal;
         if (sTotal0To1 < 0)
            negateNormal();
         sTotal = 0;
         for (Vertex3d v : contour.insideVertices1)
            for (Face f : contour.insideFaces0)
               checkDistanceToFace (v.getWorldPoint(), f);
         if (sTotal > 0) {
            // 0 to 1 direction should now be opposite to the normal,
            // ie. sTotal < 0
            if (sTotal0To1 < 0) {
               System.out.println (
                  "ambiguous collision normal - 0 to 1: " + sTotal0To1 +
                  " - 1 to 0: " + sTotal);
               isValid = false;
               /*
                * contour.collider.dumpScenario(contour);
                * contour.collider.dumpMeshes(); throw new
                * InternalErrorException("ambiguous collision normal - 0 to 1:
                * "+sTotal0To1+" - 1 to 0: "+sTotal);
                */
            }
            negateNormal(); // normal should point from mesh1 to mesh0
         }
      }
   }

   void negateNormal() {
      normal.negate();
      minProjectedDistance = -minProjectedDistance;
      maxProjectedDistance = -maxProjectedDistance;
   }

   /*
    * Calculate the distance from an origin, along the normal, to a face. Do
    * nothing if the ray from the origin along the normal does not intersect the
    * face. Set depth to this distance if it is greater than the current depth.
    */
   void checkDistanceToFace (Point3d origin, Face face) {
      Vector3d n = face.getWorldNormal();
      double s = normal.dot (n);
      if (Math.abs (s) < 1e-7)
         return;
      s = (face.getPoint0DotNormal() - origin.dot (n)) / s;
      double x = normal.x * s + origin.x;
      double y = normal.y * s + origin.y;
      double z = normal.z * s + origin.z;
      if (face.isPointInside (x, y, z)) {
         sTotal = sTotal + s; /*
                               * Use the total of s as an average measure of
                               * which direction the faces are relative to the
                               * vertices, along the normal.
                               */
         if (s < 0)
            s = -s;
         if (depth < s)
            depth = s;
         // System.out.println(contactInfo.mesh0.name+"
         // "+contactInfo.mesh1.name+" newDepth="+depth);
      }
   }

   void render (Renderer renderer, int flags) {

      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();
      
      renderer.setColor (0f, 0f, 1f);
      if (contour != null) {
         contour.render (renderer, flags); // draw the contour
      }

      renderer.setColor (0f, 1f, 1f);
      gl.glLineWidth (4);
      gl.glBegin (GL2.GL_LINE_LOOP);
      for (Point3d p : points)
         gl.glVertex3d (p.x, p.y, p.z); // draw convex hull with a heavier line
      gl.glEnd();
      gl.glLineWidth (1);

      gl.glPointSize (10);
      gl.glBegin (GL2.GL_POINTS); // emphasize the convex-hull points in case
                                 // they are same as contour.
      for (Point3d p : points)
         gl.glVertex3d (p.x, p.y, p.z);
      gl.glEnd();
      gl.glPointSize (1);

      gl.glLineWidth (4);
      renderer.setColor (0f, 1f, 0f); // draw a line through centroid along the normal
      Point3d n1 = new Point3d();
      gl.glBegin (GL2.GL_LINES);
      n1.set (centroid);
      n1.scaledAdd (minProjectedDistance, normal);
      gl.glVertex3d (n1.x, n1.y, n1.z);
      n1.set (centroid);
      n1.scaledAdd (0.5, normal);
      gl.glVertex3d (n1.x, n1.y, n1.z);
      gl.glEnd();
      gl.glLineWidth (1);

      gl.glPointSize (15);
      gl.glBegin (GL2.GL_POINTS);
      n1.set (centroid);
      gl.glVertex3d (n1.x, n1.y, n1.z);
      n1.scaledAdd (0.5, normal);
      gl.glVertex3d (n1.x, n1.y, n1.z);
      n1.set (centroid);
      n1.scaledAdd (minProjectedDistance, normal);
      gl.glVertex3d (n1.x, n1.y, n1.z);
      n1.set (centroid);
      n1.scaledAdd (maxProjectedDistance, normal);
      gl.glVertex3d (n1.x, n1.y, n1.z);
      if (depth > maxProjectedDistance - minProjectedDistance) {
         n1.set (centroid);
         n1.scaledAdd (depth, normal);
         gl.glVertex3d (n1.x, n1.y, n1.z);
      }
      gl.glEnd();
      gl.glPointSize (1);
   }
}
