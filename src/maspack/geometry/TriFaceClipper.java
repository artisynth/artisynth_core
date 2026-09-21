package maspack.geometry;

import maspack.util.*;
import maspack.matrix.*;

/**
 * Worker class that clips line segments to the upper or lower Voronoi regions
 * of an oriented 3D triangular face. Here, <i>upper</i> and <i>lower</i>
 * define the regions directly above or below the face, as defined by the
 * direction of the normal. The regions are bounded on the sides by three
 * planes, each perpendicular to the face and containing one of the triangle
 * edges.
 *
 * <p>The class can also calculate the maximum (unsigned) distance to the face
 * of the portion of either a line segment or triangle that is clipped to one
 * of these regions. Triangles are clipped as areas, and so the distance
 * returned for them is exact.
 *
 * <p>The clipping is done with respect to a coordinate frame attached to the
 * face, in which the face lies in the x-y plane so that the distance of a
 * point from the face is simply the absolute value of its z coordinate and the
 * planes bounding the region from the side are perpendicular to the x-y plane.
 *
 * <p>When a sequence of adjacent triangles is tested using {@link
 * #maxClippedDistance(Face,int,boolean)}, the coordinates computed for the
 * vertices of one triangle can be reused for those it shares with the next.
 */
public class TriFaceClipper { 

   private static double INF = Double.POSITIVE_INFINITY;

   // coordinate frame for the triangular face, centered on the first vertex
   // and oriented so that the face lies in the x-y plane
   private RigidTransform3d myTFW = new RigidTransform3d();

   // plane of the triangle itself
   private Plane myFacePlane = new Plane(); 

   // Triangle edges defined with respect to the triangle coordinate frame.
   // These bound the sides of the Voronoi region, with their normals facing
   // outward from the triangle.
   private Edge2d myEdge0 = new Edge2d();
   private Edge2d myEdge1 = new Edge2d();
   private Edge2d myEdge2 = new Edge2d();

   // Maximum number of vertices produced by clipping a triangle to the three
   // edge planes. Clipping a convex polygon to a half space adds at most one
   // vertex, so the count goes 3 -> 4 -> 5 -> 6.
   private static final int MAX_CLIPPED_VTXS = 6;

   // coordinates, in the triangle coordinate frame, of the triangle currently
   // being clipped
   private Point3d[] myTriPnts = allocPoints (3);

   // General purpose point work space, sized to hold a clipped polygon. These
   // supply the alternating buffers used to clip a triangle in the triangle
   // coordinate frame, and, at times when no triangle is being clipped, the
   // individual work points needed by the segment based methods.
   private Point3d[] myWorkPntsA = allocPoints (MAX_CLIPPED_VTXS);
   private Point3d[] myWorkPntsB = allocPoints (MAX_CLIPPED_VTXS);

   // work space for clipping line segments
   private double[] mySvals = new double[2];
   private double[] myZvals = new double[2];

   private static Point3d[] allocPoints (int num) {
      Point3d[] pnts = new Point3d[num];
      for (int i=0; i<num; i++) {
         pnts[i] = new Point3d();
      }
      return pnts;
   }

   // Vertices of the triangle most recently tested by the method
   // maxClippedDistance(Face,int,boolean), together with their coordinates in
   // the triangle coordinate frame. Entries are null if the corresponding
   // coordinates are not available. Used to avoid recomputing the coordinates
   // of vertices shared with a subsequently tested adjacent triangle.
   private Vertex3d[] myPrevVtxs = new Vertex3d[3];
   private Point3d[] myPrevPnts = allocPoints (3);

   private boolean myInitialized = false; // indicates clipper is initialized

   /**
    * Represents a triangle directed edge projected into the plane of the
    * triangle.  Assume that the edges are arranged around the triangle
    * counter-clockwise.
    */
   private static class Edge2d {
      // outward facing normal vector associated with the edge (normalized)
      double myNx;
      double myNy;

      // offset such that plane associated with this edge is defined by
      //
      // myNx x + myNy y = myD
      //
      double myD;

      /**
       * Sets an Edge2d defined by two 3d points in the triangle coordinate
       * system (such that their z values are assumed to be 0). Returns {@code
       * false} if the edge has 0 length.
       */
      boolean set (Point3d p0, Point3d p1) {
         double ux = p1.x - p0.x;
         double uy = p1.y - p0.y;
         // Math.sqrt() is used instead of Math.hypot(), which is far slower
         // and whose protection against intermediate overflow is of no use at
         // the magnitudes involved here.
         double mag = Math.sqrt (ux*ux + uy*uy);
         if (mag == 0) {
            return false;
         }
         // determine outward facing normal and plane offset
         myNx = uy/mag;
         myNy = -ux/mag;
         myD = myNx*p0.x + myNy*p0.y;
         return true;
      }

      /**
       * Computes the signed distance of the 2d plane associated with this edge
       * to a point in the triangle coordinate frame (such that its z value is
       * assumed to be 0).
       *
       * @param p point to test
       * @return signed distance to the plane
       */
      double planeDistance (Point3d p) {
         return myNx*p.x + myNy*p.y - myD;
      }
   }

   /**
    * Creates an uninitialized TriFaceClipper.
    */
   public TriFaceClipper() {
   }

   /**
    * Creates a TriFaceClipper initialized to a triangle specified by three
    * points oriented counter-clockwise.
    *
    * @param p0 first triangle point
    * @param p1 second triangle point
    * @param p2 third triangle point
    */
   public TriFaceClipper (Point3d p0, Point3d p1, Point3d p2) {
      set (p0, p1, p2);
   }

   /**
    * Creates a TriFaceClipper initialized to a triangle specified by vertices
    * (in world coordinates) of a {@link Face}. The first vertex corresponds to
    * the tail of the face's first half edge.
    *
    * @param tri face defining the triangle
    */
   public TriFaceClipper (Face tri) {
      set (tri);
   }

   /**
    * Sets this TriFaceClipper to correspond to a triangle specified by three
    * points oriented counter-clockwise. If the triangle is degenerate, the
    * method returns {@code false} and the clipper will be set so that {@link
    * #isInitialized} returns {@code false}.
    *
    * @param p0 first triangle point
    * @param p1 second triangle point
    * @param p2 third triangle point
    * @return {@code false} if the triangle is degenerate
    */
   public boolean set (Point3d p0, Point3d p1, Point3d p2) {
      Vector3d v01 = new Vector3d();
      Vector3d v12 = new Vector3d();

      // find triangle edge directions
      v01.sub (p1, p0);
      v12.sub (p2, p1);

      // compute the normal and use it to initialize the face plane
      Vector3d xprod = new Vector3d();
      xprod.cross (v01, v12);
      double mag = xprod.norm();
      if (mag == 0) {
         // degenerate triangle
         myInitialized = false;
         return false;
      }
      myFacePlane.set (xprod, p0);

      // set up coordinate frame
      Vector3d nrm = myFacePlane.normal;
      myTFW.R.setZDirection (nrm);
      myTFW.p.set (p0);

      // The frame has changed, so any previously saved vertex coordinates are
      // no longer valid.
      clearPrevVertices();

      // Find the triangle points with respect to the coordinate frame, using
      // myTriPnts to hold the result since no clipping is in progress. The
      // frame is centered on p0, so the first point is the origin, while the
      // displacements of the others are supplied by the edge directions. All
      // three points lie in the x-y plane and so their z values are 0.
      myTriPnts[0].setZero();
      myTriPnts[1].set (v01);
      computeTriangleCoords (myTriPnts[1], 0);
      myTriPnts[2].add (v01, v12);
      computeTriangleCoords (myTriPnts[2], 0);
      if (!myEdge0.set (myTriPnts[0], myTriPnts[1]) ||
          !myEdge1.set (myTriPnts[1], myTriPnts[2]) ||
          !myEdge2.set (myTriPnts[2], myTriPnts[0])) {
         // triangle is degenerate within its own plane
         myInitialized = false;
         return false;
      }

      myInitialized = true;
      return true;
   }

   /**
    * Sets the TriFaceClipper to correspond to a triangle specified by the
    * world coordinates of a {@link Face}. The first vertex corresponds to the
    * tail of the face's first half edge.
    *
    * @param tri face defining the triangle
    */
   public void set (Face tri) {
      HalfEdge he0 = tri.firstHalfEdge();
      HalfEdge he1 = he0.getNext();
      HalfEdge he2 = he1.getNext();
      if (he2.getNext() != he0) {
         throw new IllegalArgumentException (
            "Face does not correspond to a triangle");
      }
      // Work points to receive the world coordinates of the vertices. The A
      // work space is used because set(p0,p1,p2) stores its result in
      // myTriPnts.
      Point3d p0 = myWorkPntsA[0];
      Point3d p1 = myWorkPntsA[1];
      Point3d p2 = myWorkPntsA[2];
      he0.getTail().getWorldPoint (p0);
      he1.getTail().getWorldPoint (p1);
      he2.getTail().getWorldPoint (p2);
      set (p0, p1, p2);
   }

   /**
    * Queries whether if this clipper is initialized to a non-degenerate
    * triangle.
    *
    * @return {@code true} if clipper is initialzed
    */
   public boolean isInitialized() {
      return myInitialized;
   }

   protected void checkInitialized() {
      if (!myInitialized) {
         throw new ImproperStateException (
            "TriFaceClipper has not been initialized to a specific face");
      }
   }

   protected void checkSide (int side) {
      if (side == 0) {
         throw new IllegalArgumentException ("side must be non-zero");
      }
   }

   /**
    * Intersects the parameter interval {@code svals} with the set of {@code s}
    * values for which
    * <pre>
    * (1-s) d0 + s d1 &gt;= 0
    * </pre>
    * where {@code d0} and {@code d1} are the signed distances of the segment
    * end points from a plane, with positive values indicating the side which
    * is being kept. Points lying exactly on the plane are retained.
    *
    * @param svals parameter interval to be intersected
    * @param d0 signed distance of the first end point
    * @param d1 signed distance of the second end point
    * @return {@code false} if the resulting interval is empty, in which case
    * {@code svals} is set to {@code [1,0]}
    */
   protected boolean clipInterval (double[] svals, double d0, double d1) {
      if (d0 < 0) {
         if (d1 < 0) {
            // completely clipped
            svals[0] = 1;
            svals[1] = 0;
            return false;
         }
         // the segment enters the half space where it crosses the plane
         double s = d0/(d0-d1);
         if (s > svals[0]) {
            svals[0] = s;
         }
      }
      else if (d1 < 0) {
         // the segment leaves the half space where it crosses the plane
         double s = d0/(d0-d1);
         if (s < svals[1]) {
            svals[1] = s;
         }
      }
      if (svals[0] > svals[1]) {
         // the remaining interval is empty
         svals[0] = 1;
         svals[1] = 0;
         return false;
      }
      return true;
   }

   /**
    * Worker method which clips a line segment to the region indicated by
    * {@code side}. The clipping is done entirely in parameter space, using the
    * distances of the segment end points from each of the four planes bounding
    * the region.
    *
    * <p>The z values of the end points with respect to the triangle coordinate
    * frame are determined first, since these are also their signed distances
    * from the face and so allow segments lying entirely on the far side of the
    * face plane to be rejected before their x-y values are computed.
    *
    * @param svals returns the parameter range of the clipped segment, or
    * {@code [1,0]} if the segment is completely clipped
    * @param zvals returns the z values of {@code p0} and {@code p1} with
    * respect to the triangle coordinate frame
    * @param p0 first point of the segment
    * @param p1 last point of the segment
    * @param side indicates the upper region if positive, or the lower region
    * if negative
    * @return {@code false} if the segment is completely clipped
    */
   protected boolean clipSegmentToRegion (
      double[] svals, double[] zvals, Point3d p0, Point3d p1, int side) {

      // work points for the end points with respect to the triangle frame; no
      // triangle is being clipped, so the work space is free
      Point3d q0 = myWorkPntsA[0];
      Point3d q1 = myWorkPntsA[1];

      Vector3d nrm = myFacePlane.normal;
      q0.sub (p0, myTFW.p);
      q1.sub (p1, myTFW.p);
      double z0 = nrm.dot (q0);
      double z1 = nrm.dot (q1);
      zvals[0] = z0;
      zvals[1] = z1;

      svals[0] = 0;
      svals[1] = 1;

      // clip to the face plane, whose inside is where side*z >= 0
      if (!clipInterval (svals, side*z0, side*z1)) {
         return false;
      }
      // complete the transformation into the triangle coordinate frame and
      // clip to the three edge planes. These have outward facing normals, and
      // so their inside is where the plane distance is negative.
      computeTriangleCoords (q0, z0);
      computeTriangleCoords (q1, z1);
      if (!clipInterval (
             svals, -myEdge0.planeDistance(q0),
             -myEdge0.planeDistance(q1))) {
         return false;
      }
      if (!clipInterval (
             svals, -myEdge1.planeDistance(q0),
             -myEdge1.planeDistance(q1))) {
         return false;
      }
      if (!clipInterval (
             svals, -myEdge2.planeDistance(q0),
             -myEdge2.planeDistance(q1))) {
         return false;
      }
      return true;
   }

   /**
    * Returns the maximum unsigned distance from the face of a clipped segment,
    * given the parameter range {@code svals} of the clipped segment and the z
    * values {@code zvals} of the end points of the original segment. Since the
    * z value varies affinely along the segment, and the clipped segment lies
    * on the side of the face indicated by {@code side}, this is simply the
    * larger of the heights at the two ends of the clipped segment.
    */
   private double maxClippedHeight (
      double[] svals, double[] zvals, int side) {
      double z0 = zvals[0];
      double z1 = zvals[1];
      double s0 = svals[0];
      double s1 = svals[1];
      double h0 = side*((1-s0)*z0 + s0*z1);
      double h1 = side*((1-s1)*z0 + s1*z1);
      double maxh = (h0 > h1 ? h0 : h1);
      // guard against round-off making the height slightly negative, since -1
      // is reserved to indicate no intersection
      return (maxh < 0 ? 0 : maxh);
   }

   /**
    * Clips a line segment to either the upper or lower Voronoi region
    * depending on whether {@code side} is {@code 1} or {@code -1}. The segment
    * is assumed to lie between the points {@code p0} and {@code p1}, such that
    * points on it are defined by
    * <pre>
    * p = (1-s) p0 + s p1
    * </pre>
    * where {@code s} is a scalar parameter in the range {@code [0, 1]}.
    * 
    * @param svals If non-null, must be of length {@code >= 2} and will be used
    * to return the parameter range of the segment after clipping. If all of
    * the segment lies in the region, {@code svals} will contain {@code [0,1]}.
    * If none of the segment lies in the region, then it will be set to
    * contain {@code [1,0]}.
    * @param ps0 if non-null and the segment is not completely clipped, returns
    * the clipped segment point corresponding to {@code svals[0]}
    * @param ps1 if non-null and the segment is not completely clipped,
    * returns the clipped segment point corresponding to {@code svals[1]}
    * @param p0 first point of the segment
    * @param p1 last point of the segment
    * @param side clips to the upper region if positive or the lower region if
    * negative. Must not be 0.
    * @return {@code false} if no part of the segment lies in the region and so
    * any values returned by {@code ps0} and {@code ps1} are undefined
    */
   public boolean clipSegment (
      double[] svals, Point3d ps0, Point3d ps1,
      Point3d p0, Point3d p1, int side) {
      
      checkInitialized();
      checkSide (side);

      if (svals == null) {
         svals = mySvals;
      }
      if (!clipSegmentToRegion (svals, myZvals, p0, p1, side)) {
         return false;
      }
      if (ps0 != null) {
         ps0.combine (1-svals[0], p0, svals[0], p1);
      }
      if (ps1 != null) {
         ps1.combine (1-svals[1], p0, svals[1], p1);
      }
      return true;
   }

   /**
    * Computes the maximum unsigned distance from the face of an edge that is
    * clipped to either the upper or lower region, depending on whether {@code
    * side} is positive or negative. The edge is specified by the world
    * coordinates of the vertices of a half-edge.
    *
    * @param he edge to check
    * @param side clips to the upper region if positive or the lower region if
    * negative. Must not be 0.
    * @return non-signed maximum distance of the clipped half edge from the
    * face, or -1 if no part of the half-edge is in the region.
    */
   public double maxClippedDistance (HalfEdge he, int side) {
      // work points for the world coordinates of the end points. These are
      // taken from the B work space, since the segment methods called below
      // use the A work space.
      Point3d p0 = myWorkPntsB[0];
      Point3d p1 = myWorkPntsB[1];
      he.getTail().getWorldPoint (p0);
      he.getHead().getWorldPoint (p1);
      return maxClippedDistance (p0, p1, side);
   }

   /**
    * Computes the maximum unsigned distance from the face of a line segment
    * that is clipped to either the upper or lower region, depending on whether
    * {@code side} is positive or negative. The segment is specified by
    * the points {@code p0} and {@code p1}.
    *
    * @param p0 first segment point
    * @param p1 second segment point
    * @param side clips to the upper region if positive or the lower region if
    * negative. Must not be 0.
    * @return non-signed maximum distance of the clipped segment from the
    * face, or -1 if no part of the segment is in the region
    */
   public double maxClippedDistance (Point3d p0, Point3d p1, int side) {
      checkInitialized();
      checkSide (side);

      if (!clipSegmentToRegion (mySvals, myZvals, p0, p1, side)) {
         return -1; // segment is completely clipped
      }
      return maxClippedHeight (mySvals, myZvals, side);
   }

   /**
    * Computes the maximum unsigned distance from the face of a triangle {@code
    * tri} that is clipped to either the upper or lower region, depending on
    * whether {@code side} is positive or negative. The triangle is evaluated
    * in world coordinates.
    *
    * <p>As with {@link #maxClippedDistance(Point3d,Point3d,Point3d,int)}, the
    * triangle is clipped as an area and so the distance returned is exact.
    *
    * <p>The argument {@code checkAdjacent} is used to help efficiently compute
    * the maximum distance for a clipped triangle strip. If set {@code true},
    * then for each vertex of {@code tri} which is shared with the triangle
    * most recently tested by this method, the coordinates computed for that
    * vertex are reused instead of being recomputed. This does not affect the
    * value returned; it is purely an optimization, and callers should set it
    * {@code false} if the vertex positions may have changed since the previous
    * call.
    *
    * @param tri triangle to check
    * @param side clips to the upper region if positive or the lower region if
    * negative. Must not be 0.
    * @param checkAdjacent if {@code true}, reuses coordinates for vertices
    * shared with the most recent triangle tested with this method
    * @return non-signed maximum distance of the clipped triangle from the
    * face, or -1 if no part of the triangle is in the region.
    */
   public double maxClippedDistance (
      Face tri, int side, boolean checkAdjacent) {

      checkInitialized();
      checkSide (side);

      HalfEdge he0 = tri.firstHalfEdge();
      Vertex3d v0 = he0.getTail();
      HalfEdge he1 = he0.getNext();
      Vertex3d v1 = he1.getTail();
      HalfEdge he2 = he1.getNext();
      Vertex3d v2 = he2.getTail();
      if (he2.getNext() != he0) {
         throw new IllegalArgumentException (
            "Argument 'tri' is not a triangle");
      }

      // Find the triangle frame coordinates of the vertices. The lookups read
      // myPrevVtxs/myPrevPnts while the results are stored in myTriPnts, so
      // the buffers being read are not disturbed.
      computeVertexCoords (myTriPnts[0], v0, checkAdjacent);
      computeVertexCoords (myTriPnts[1], v1, checkAdjacent);
      computeVertexCoords (myTriPnts[2], v2, checkAdjacent);

      double maxd = maxClippedTriangleDistance (side);

      // Save the vertices and their coordinates for the next call. The
      // coordinate buffers are exchanged instead of being copied.
      myPrevVtxs[0] = v0;
      myPrevVtxs[1] = v1;
      myPrevVtxs[2] = v2;
      Point3d[] tmp = myPrevPnts;
      myPrevPnts = myTriPnts;
      myTriPnts = tmp;

      return maxd;
   }

   /**
    * Clears the vertices saved by {@link
    * #maxClippedDistance(Face,int,boolean)}, so that no coordinates will be
    * reused by the next call to that method.
    */
   private void clearPrevVertices() {
      myPrevVtxs[0] = null;
      myPrevVtxs[1] = null;
      myPrevVtxs[2] = null;
   }

   /**
    * Sets {@code p} to the coordinates of the vertex {@code vtx} with respect
    * to the triangle coordinate frame. If {@code useSaved} is {@code true} and
    * coordinates for {@code vtx} were computed by the previous call to {@link
    * #maxClippedDistance(Face,int,boolean)}, those are used directly.
    */
   private void computeVertexCoords (
      Point3d p, Vertex3d vtx, boolean useSaved) {
      if (useSaved) {
         for (int i=0; i<3; i++) {
            if (myPrevVtxs[i] == vtx) {
               p.set (myPrevPnts[i]);
               return;
            }
         }
      }
      // work point for the world coordinates of the vertex; the triangle
      // clipping which uses the work space has not yet begun
      Point3d pw = myWorkPntsB[0];
      vtx.getWorldPoint (pw);
      p.sub (pw, myTFW.p);
      computeTriangleCoords (p, myFacePlane.normal.dot (p));
   }

   /**
    * Clips a convex polygon, described with respect to the triangle coordinate
    * frame, to the inside of the plane associated with one of the triangle
    * edges. Since that plane is perpendicular to the face, and hence parallel
    * to the z axis, the clipping is determined entirely by the x-y values of
    * the polygon points, while their z values are simply interpolated. This
    * keeps the computation exact even when the polygon is itself perpendicular
    * to the face and so projects onto a line segment.
    *
    * <p>Points lying exactly on the plane are retained.
    *
    * @param polyr returns the clipped polygon. Must be a different buffer from
    * {@code poly} and have a length of at least {@code num+1}.
    * @param poly polygon to be clipped
    * @param num number of points in {@code poly}
    * @param edge edge whose plane is used for the clipping
    * @return number of points in the clipped polygon, or 0 if the polygon is
    * completely clipped
    */
   protected int clipPolyToEdge (
      Point3d[] polyr, Point3d[] poly, int num, Edge2d edge) {

      int numr = 0;
      Point3d pa = poly[num-1];
      double da = edge.planeDistance (pa);
      for (int i=0; i<num; i++) {
         Point3d pb = poly[i];
         double db = edge.planeDistance (pb);
         if (da <= 0) {
            // pa is inside the plane, so keep it
            polyr[numr++].set (pa);
         }
         if ((da < 0 && db > 0) || (da > 0 && db < 0)) {
            // segment pa-pb crosses the plane, so add the crossing point
            double s = da/(da-db);
            polyr[numr++].combine (1-s, pa, s, pb);
         }
         pa = pb;
         da = db;
      }
      return numr;
   }

   /**
    * Completes the transformation of a point into the triangle coordinate
    * frame, given that {@code p} currently contains the displacement of the
    * point from the frame's origin, and that its z value with respect to the
    * frame has already been determined.
    *
    * @param p on input, displacement of the point from the frame origin; on
    * output, the point with respect to the frame
    * @param z z value of the point with respect to the frame
    */
   private void computeTriangleCoords (Point3d p, double z) {
      RotationMatrix3d R = myTFW.R;
      double x = R.m00*p.x + R.m10*p.y + R.m20*p.z;
      double y = R.m01*p.x + R.m11*p.y + R.m21*p.z;
      p.set (x, y, z);
   }

   /**
    * Computes the maximum unsigned distance from the face of a triangle that
    * is clipped to either the upper or lower region, depending on whether
    * {@code side} is positive or negative. The triangle is defined by three
    * points specified in counter-clockwise order.
    *
    * <p>The triangle is clipped as an area, and so the distance returned is
    * exact. In particular, the method detects the case in which the triangle's
    * interior intersects the region while none of its edges do.
    *
    * <p>The computation is done in the triangle coordinate frame, where the
    * face lies in the x-y plane and the distance of a point from the face is
    * simply the absolute value of its z coordinate. The three planes bounding
    * the region from the side are then perpendicular to the x-y plane, and the
    * triangle is clipped to them using the (2d) edge descriptions.
    *
    * @param p0 first triangle point
    * @param p1 second triangle point
    * @param p2 third triangle point
    * @param side clips to the upper region if positive or the lower region if
    * negative. Must not be 0.
    * @return non-signed maximum distance of the clipped triangle from the
    * face, or -1 if no part of the triangle is in the region.
    */
   public double maxClippedDistance (
      Point3d p0, Point3d p1, Point3d p2, int side) {

      checkInitialized();
      checkSide (side);

      // Transform the points into the triangle frame. Their z values are
      // found from the displacements of the points from the frame origin;
      // since the z axis of the frame is the face normal, these are simply the
      // signed distances of the points from the face plane.
      Vector3d nrm = myFacePlane.normal;
      myTriPnts[0].sub (p0, myTFW.p);
      myTriPnts[1].sub (p1, myTFW.p);
      myTriPnts[2].sub (p2, myTFW.p);
      double z0 = nrm.dot (myTriPnts[0]);
      double z1 = nrm.dot (myTriPnts[1]);
      double z2 = nrm.dot (myTriPnts[2]);

      // reject the triangle before computing the x-y values if it lies
      // entirely on the opposite side of the face from the region
      if (allPointsOutside (z0, z1, z2, side)) {
         return -1;
      }
      computeTriangleCoords (myTriPnts[0], z0);
      computeTriangleCoords (myTriPnts[1], z1);
      computeTriangleCoords (myTriPnts[2], z2);

      return maxClippedTriangleDistance (side);
   }

   /**
    * Returns {@code true} if all three points of a triangle lie on the
    * opposite side of the face from the region indicated by {@code side},
    * given their z values with respect to the triangle coordinate frame. The
    * whole triangle then lies outside the region.
    */
   private boolean allPointsOutside (
      double z0, double z1, double z2, int side) {
      return side*z0 < 0 && side*z1 < 0 && side*z2 < 0;
   }

   /**
    * Computes the maximum unsigned distance from the face of the triangle
    * whose coordinates, with respect to the triangle coordinate frame, are
    * given by {@code myTriPnts}. Clipping is to the upper or lower region
    * depending on whether {@code side} is positive or negative.
    *
    * @param side clips to the upper region if positive or the lower region if
    * negative
    * @return non-signed maximum distance of the clipped triangle from the
    * face, or -1 if no part of the triangle is in the region.
    */
   private double maxClippedTriangleDistance (int side) {

      if (allPointsOutside (
             myTriPnts[0].z, myTriPnts[1].z, myTriPnts[2].z, side)) {
         return -1;
      }

      // Clip to the three edge planes, alternating between the buffers. The
      // result is a convex polygon lying in the plane of the triangle.
      int num;
      num = clipPolyToEdge (myWorkPntsB, myTriPnts, 3, myEdge0);
      if (num == 0) {
         return -1;
      }
      num = clipPolyToEdge (myWorkPntsA, myWorkPntsB, num, myEdge1);
      if (num == 0) {
         return -1;
      }
      num = clipPolyToEdge (myWorkPntsB, myWorkPntsA, num, myEdge2);
      if (num == 0) {
         return -1;
      }

      // Only the face plane remains to be clipped to. Since the height side*z
      // is an affine function over the polygon, its maximum is attained at one
      // of the vertices, and clipping to the face plane would only introduce
      // vertices for which z = 0 and which hence cannot increase this
      // maximum. It is therefore enough to maximize the height over the
      // existing vertices, and reject the triangle if the result is negative.
      double maxd = -INF;
      for (int i=0; i<num; i++) {
         double h = side*myWorkPntsB[i].z;
         if (h > maxd) {
            maxd = h;
         }
      }
      return (maxd < 0 ? -1 : maxd);
   }

}
