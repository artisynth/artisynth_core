/**
 * Copyright (c) 2014, by the Authors: Andrew Larkin (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.HashSet;
import maspack.fileutil.NativeLibraryManager;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.InternalErrorException;

/**
 * A set of utility methods for robust intersection queries involving line
 * segments and faces.
 *
 * <p>In general, the methods will first perform the query using regular
 * double-precision arithmetic. If this is insufficient to yield a correct
 * answer, the query is reevaluated using "robust" predicates involving
 * adaptive precision arithmetic (Jonathan Shewchuck, "Adaptive precision
 * floating-point arithmetic and fast robust geometric predicates", Discrete
 * {@code &} Computational Geometry, 1997).
 *
 * <p>If the computation is <i>exactly</i> degenerate (i.e., an edge exactly
 * intersects a triangle vertex or edge, then a whether or not an intersection
 * occurs is determined using tie-breaking rules as described in Edelsbrunner
 * and Peter, "Simulation of simplicity: a technique to cope with degenerate
 * cases in geometric algorithms", ACM Transactions on Graphics, 1990, and as
 * implemented by Aftosmis et al., "Robust and efficient Cartesian mesh
 * generation for component-based geometry", AIAA journal, 1998. These
 * tie-breaking rules require assigning unique indices to all primitive
 * vertices, and making sure that these remain the same across all the
 * calculations. In cases where all queries involve triangles and faces from
 * two triangular meshes, we can determine such unique indices using the
 * mesh-specific vertex indices. For the vertices on the first mesh, we use
 * mesh indices verbatim, while for the second mesh we use the mesh indices
 * <i>plus</i> the number of vertices on the first mesh.
 * 
 * <p>The robust predicates used by this class are supported using
 * a native code library.
 */
public class RobustPreds {
   private static boolean nativeSupportLoaded = false;

   private static double DOUBLE_PREC = 2e-16;
   private static double ORIENT_EPS = (7+56*DOUBLE_PREC)*DOUBLE_PREC;

   /**
    * segment and triangle intersect
    */
   static public final int INTERSECTS = 0x01;
   /**
    * tail segment point is outside the (counterclockwise) triangle, according to
    * tie-breaking rules
    */
   static public final int TAIL_OUTSIDE = 0x02;
   /**
    * tail segment point is on the triangle plane, according to exact arithmetic
    */
   static public final int TAIL_ON_TRIANGLE = 0x04;
   /**
    * head segment point is on the triangle plane, according to exact arithmetic
    */
   static public final int HEAD_ON_TRIANGLE = 0x08;   
   /**
    * triangle edge 01 is on the segment, according to exact arithmetic
    */
   static public final int E01_ON_SEGMENT = 0x10;
   /**
    * triangle edge 12 is on the segment, according to exact arithmetic
    */
   static public final int E12_ON_SEGMENT = 0x20;
   /**
    * triangle edge 20 is on the segment, according to exact arithmetic
    */
   static public final int E20_ON_SEGMENT = 0x40;

   /**
    * triangle vertex 0 is on the segment, according to exact arithmetic
    */
   static public final int V0_ON_SEGMENT = (E20_ON_SEGMENT|E01_ON_SEGMENT);
   /**
    * triangle vertex 1 is on the segment, according to exact arithmetic
    */
   static public final int V1_ON_SEGMENT = (E01_ON_SEGMENT|E12_ON_SEGMENT);
   /**
    * triangle vertex 2 is on the segment, according to exact arithmetic
    */
   static public final int V2_ON_SEGMENT = (E12_ON_SEGMENT|E20_ON_SEGMENT);
   
   static public final int DEGENERACY_MASK = 0x78; 

   private static native int jniInit (Point3d p);

   static native int jniOrient3d (
      int i0, double p0x, double p0y, double p0z,
      int i1, double p1x, double p1y, double p1z,
      int i2, double p2x, double p2y, double p2z,
      int i3, double p3x, double p3y, double p3z);

   static native int jniIntersectSegmentTriangle (
      int is0, double s0x, double s0y, double s0z,
      int is1, double s1x, double s1y, double s1z,
      int it0, double t0x, double t0y, double t0z,
      int it1, double t1x, double t1y, double t1z,
      int it2, double t2x, double t2y, double t2z, Point3d p);

   static native int jniClosestIntersection (
      double ax, double ay, double az, double bx, double by, double bz,
      double c0x, double c0y, double c0z, double c1x, double c1y, double c1z,
      double c2x, double c2y, double c2z, double d0x, double d0y, double d0z,
      double d1x, double d1y, double d1z, double d2x, double d2y, double d2z);

   static void initialize() {
      // try loading in the native code
      try {
         NativeLibraryManager.load ("RobustPreds.1.1");
         nativeSupportLoaded = true;
         jniInit (new Point3d()); // cache the x,y,z fieldIDs
      }
      catch (UnsatisfiedLinkError e) {
         System.out.println (
            "err=" + e.getMessage() + " java.library.path=" +
            System.getProperty ("java.library.path"));
         throw new UnsupportedOperationException (
            "Can't load native library \"RobustPreds\".  java.library.path=" +
            System.getProperty ("java.library.path"));
      }
   }

   private static boolean isInitialized() {
      return nativeSupportLoaded;
   }

   /**
    * Returns <code>true</code> if v3 is "below", or "inside" the plane formed
    * by the counterclockwise triangle v0, v1, v2.
    *
    * <p>The computation first performs a quick numeric calculation to see if
    * the intersection can be determined within regular double precision
    * tolerances. If it cannot, then a more detailed calculation is performed
    * using robust predicates.
    *
    * <p>If tie-breaking is needed, then this is done using "Simulation of
    * Simplicity", as described in the class header, by assigning unique
    * indices to each vertex based on their underlying mesh indices. If the
    * meshes are different, then if <code>v3OnMesh0</code> is
    * <code>true</code>, we increment the triangle vertex indices by the number
    * of vertices on the v3 mesh, while if it is <code>false</code> we
    * increment the v3 vertex index by the number of vertices on the triangle
    * mesh.
    * 
    * @param v0 first triangle vertex
    * @param v1 second triangle vertex
    * @param v2 third vertex
    * @param v3 vertex to test
    * @param v3OnMesh0 used to determine vertex indices for tie-breaking,
    * as described above
    * @param worldCoords if <code>true</code>, computes the intersection in
    * world coordinates. Otherwise, mesh local coordinates are used.
    * @return <code>true</code> if v3 is inside the triangle
    */
   public static boolean orient3d (
      Vertex3d v0, Vertex3d v1, Vertex3d v2, Vertex3d v3, 
      boolean v3OnMesh0, boolean worldCoords) {

      Point3d p0, p1, p2, p3;

      if (worldCoords) {
         p0 = getWorldPoint(v0);
         p1 = getWorldPoint(v1);
         p2 = getWorldPoint(v2);
         p3 = getWorldPoint(v3);
      }
      else {
         p0 = v0.pnt;
         p1 = v1.pnt;
         p2 = v2.pnt;
         p3 = v3.pnt;
      }

      int i0 = v0.getIndex();
      int i1 = v1.getIndex();
      int i2 = v2.getIndex();
      int i3 = v3.getIndex();
      if (v0.getMesh() != v3.getMesh()) {
         if (v3OnMesh0) {
            int numv = v3.getMesh().numVertices();
            i0 += numv;
            i1 += numv;
            i2 += numv;
         }
         else {
            i3 += v0.getMesh().numVertices();
         }
      }

      if (!nativeSupportLoaded) {
         initialize();
      }
      int result =
         jniOrient3d (
            i0, p0.x, p0.y, p0.z, i1, p1.x, p1.y, p1.z,
            i2, p2.x, p2.y, p2.z, i3, p3.x, p3.y, p3.z);
      //System.out.println("Current CW: " + jniGetCW() );
      if (result < 0)
         throw new RuntimeException ("RobustPreds failed with rc=" + result);
      return result == 1;
   }

   /**
    * For debugging - prints out arguments sent to jniIntersectSegmentTriangle().
    */
   public static void printIntersectEdgeFace (
      HalfEdge edge, Face face, boolean edgeOnMesh0) {
      // if (!edge.isPrimary()) throw new RuntimeException("non-primary edge");
      Vertex3d seg0, seg1, tri0, tri1, tri2;

      Point3d ps0 = new Point3d();
      Point3d ps1 = new Point3d();
      Point3d pt0 = new Point3d();
      Point3d pt1 = new Point3d();
      Point3d pt2 = new Point3d();

      seg0 = edge.tail;
      seg0.getWorldPoint(ps0);
      seg1 = edge.head;
      seg1.getWorldPoint(ps1);
      HalfEdge e = face.firstHalfEdge();
      tri0 = e.tail;
      tri0.getWorldPoint(pt0);
      tri1 = e.head;
      tri1.getWorldPoint(pt1);
      e = e.getNext();
      tri2 = e.head;
      tri2.getWorldPoint(pt2);
      if (!nativeSupportLoaded) {
         initialize();
      }

      int is0 = seg0.getIndex();
      int is1 = seg1.getIndex();
      int it0 = tri0.getIndex();
      int it1 = tri1.getIndex();
      int it2 = tri2.getIndex();
      
      if (edgeOnMesh0) {
         int numv0 = seg0.getMesh().numVertices();
         it0 += numv0;
         it1 += numv0;
         it2 += numv0;
      }
      else {
         int numv0 = tri0.getMesh().numVertices();
         is0 += numv0;
         is1 += numv0;
      }

      System.out.println (is0 + " " + ps0);
      System.out.println (is1 + " " + ps1);
      System.out.println (it0 + " " + pt0);
      System.out.println (it1 + " " + pt1);
      System.out.println (it2 + " " + pt2);
   }

   /**
    * Assuming that <code>faceC</code> and <code>faceD</code> both intersect
    * <code>edge</code>, determines which intersection point comes irst.
    * Specifically, let c and d be the intersection of <code>faceC</code> and
    * <code>faceD</code> with <code>edge</code>. Then this method returns: 1 if
    * d is closer to the edge's tail, -1 if c is closer to the edge's tail, and
    * 0 if they are equidistant. This method uses adaptive arithmetic to
    * determine an exact solution.

    * @param faceC first face to test
    * @param edge edge to tect
    * @param faceD second face to test
    * @return 1, 0, or -1 if faceD is closer, equidistant, or farther
    * from the edge's tail
    */
   public static int closestIntersection (
      Face faceC, HalfEdge edge, Face faceD) {
      Point3d a = edge.tail.getWorldPoint();
      Point3d b = edge.head.getWorldPoint();

      HalfEdge e = faceC.firstHalfEdge();
      Point3d c0 = e.tail.getWorldPoint();
      Point3d c1 = e.head.getWorldPoint();
      e = e.getNext();
      Point3d c2 = e.head.getWorldPoint();

      e = faceD.firstHalfEdge();
      Point3d d0 = e.tail.getWorldPoint();
      Point3d d1 = e.head.getWorldPoint();
      e = e.getNext();
      Point3d d2 = e.head.getWorldPoint();

      if (!nativeSupportLoaded) {
         initialize();
      }
      
      int result =
         jniClosestIntersection (
            a.x, a.y, a.z, b.x, b.y, b.z, c0.x, c0.y, c0.z, c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z, d0.x, d0.y, d0.z, d1.x, d1.y, d1.z, d2.x, d2.y,
            d2.z);
      //System.out.println("Current CW: " + jniGetCW() );
      if (result < -1)
         throw new RuntimeException (
            "error in jni closestIntersection predicate: " + result);
      return result;
   }


   private static Point3d getWorldPoint (Vertex3d v) {
      Point3d wpnt = new Point3d();
      v.getWorldPoint (wpnt);
      return wpnt;
   }
   
   /**
    * Tests for the intersection of an edge and a triangle described by a
    * face. Returns an intersection and a non-zero set of flags if there
    * is. Flags may include {@link #INTERSECTS} (always set if there is an
    * intersection), {@link #TAIL_OUTSIDE}, {@link #TAIL_ON_TRIANGLE}, {@link
    * #HEAD_ON_TRIANGLE}, {@link #E01_ON_SEGMENT}, {@link #E12_ON_SEGMENT}, and
    * {@link #E20_ON_SEGMENT}.  If there is an intersection, and
    * <code>ipnt</code> is non-<code>null</code>, then the intersection point
    * is computed and returned in <code>ipnt</code>.
    * 
    * <p>If {@code maxlen > 0}, the method first performs a quick numeric
    * calculation to see if the intersection can be determined within to
    * tolerance based on <code>maxlen</code>. If it cannot, then a more
    * detailed calculation is performed using robust predicates. In order to
    * work properly, <code>maxlen</code> must be {@code >=} the length of the
    * edge and any of the face edges. If <code>maxlen</code> = 0, then the
    * robust predicates are called directly.
    *
    * <p>If tie-breaking is needed, then this is done using "Simulation of
    * Simplicity", as described in the class header, by assigning unique
    * indices to each vertex based on their underlying mesh indices. If the
    * meshes are different, then if <code>edgeOnMesh0</code> is
    * <code>true</code>, we increment the face vertex indices by the number of
    * vertices on the edge mesh, while if it is <code>false</code> we increment
    * the edge vertex indices by the number of vertices on the face mesh.
    *
    * @param ipnt if non-<code>null</code> and if the edge and face intersect,
    * returns the intersection point
    * @param edge edge to test for intersection
    * @param face face to test for intersection
    * @param maxlen if {@code > 0}, specifies an upper bound for the length of
    * the edge and face edges which is used to see if the intersection can be
    * determined using regular double precision
    * @param edgeOnMesh0 used to determine vertex indices for tie-breaking,
    * as described above
    * @param worldCoords if <code>true</code>, computes the intersection
    * in world coordinates. Otherwise, mesh local coordinates are used.
    * 
    * @return code 0 if there is no intersection; flags describing the
    * intersection if there is
    */
   public static int intersectEdgeTriangle (
      Point3d ipnt, HalfEdge edge, Face face, double maxlen,
      boolean edgeOnMesh0, boolean worldCoords) {

      if (!nativeSupportLoaded) {
         initialize();
      }

      Vertex3d vt = edge.getTail();
      Vertex3d vh = edge.getHead();

      Vertex3d v0 = face.he0.getTail();
      Vertex3d v1 = face.he0.getHead();
      Vertex3d v2 = face.he0.getNext().getHead();

      Point3d pt, ph, p0, p1, p2;

      if (worldCoords) {
         pt = getWorldPoint (vt);
         ph = getWorldPoint (vh);
         p0 = getWorldPoint (v0);
         p1 = getWorldPoint (v1);
         p2 = getWorldPoint (v2);
      }
      else {
         pt = vt.pnt;
         ph = vh.pnt;
         p0 = v0.pnt;
         p1 = v1.pnt;
         p2 = v2.pnt;
      }

      int res = -1;
      if (maxlen > 0) {
         res = intersectSegmentTriangleFast (ipnt, pt, ph, p0, p1, p2, maxlen);
      }
      if (res == -1) {
         // collect edge-face indices and call robust pred
         int it = vt.getIndex();
         int ih = vh.getIndex();
         int i0 = v0.getIndex();
         int i1 = v1.getIndex();
         int i2 = v2.getIndex(); 

         if (vt.getMesh() != v0.getMesh()) {
            if (edgeOnMesh0) {
               int numv0 = vt.getMesh().numVertices();
               i0 += numv0;
               i1 += numv0;
               i2 += numv0;
            }
            else {
               int numv0 = v0.getMesh().numVertices();
               it += numv0;
               ih += numv0;
            }        
         }
         res = intersectSegmentTriangle (
            ipnt, it, pt, ih, ph, i0, p0, i1, p1, i2, p2);
      }
      return res;
   }
      
   /**
    * Tests for the intersection of a line segment and a triangle described by
    * a face. Returns 0 is there is no intersection and a non-zero set of flags
    * if there is. Flags may include {@link #INTERSECTS} (always set if there
    * is an intersection), {@link #TAIL_OUTSIDE}, {@link #TAIL_ON_TRIANGLE},
    * {@link #HEAD_ON_TRIANGLE}, {@link #E01_ON_SEGMENT}, {@link
    * #E12_ON_SEGMENT}, and {@link #E20_ON_SEGMENT}.  If there is an
    * intersection, and <code>ipnt</code> is non-<code>null</code>, then the
    * intersection point is computed and returned in <code>ipnt</code>.
    * 
    * <p>If {@code maxlen > 0}, the method first performs a quick numeric
    * calculation to see if the intersection can be determined within to
    * tolerance based on <code>maxlen</code>. If it cannot, then a more
    * detailed calculation is performed using robust predicates. In order to
    * work properly, <code>maxlen</code> must be {@code >=} the length of the
    * segment and any of the face edges. If <code>maxlen</code> = 0, then the
    * robust predicates are called directly.
    *
    * <p>If tie-breaking is needed, then this is done using "Simulation of
    * Simplicity", as described in the class header, by assigning unique
    * indices to each vertex based on their underlying mesh indices. Vertices
    * for the line segment end points (<code>pt</code> and <code>ph</code>) are
    * assigned 0 and 1, while for the face vertices indices are assigned using
    * their underlying mesh vertices plus 2.
    *
    * <p>For tie-breaking purposes, it is assumed that the line segment is a
    * stand-alone primitive and independent of the face mesh. If multiple
    * queries are performed with the same line segment and different faces from
    * the same mesh, then <code>pt</code> and <code>ph</code> should always
    * refer to the same segment end points.
    *
    * @param ipnt if non-<code>null</code> and if the segment and face
    * intersect, returns the intersection point
    * @param pt tail point of the line segment
    * @param ph head point of the line segment
    * @param face face to test for intersection
    * @param maxlen if {@code > 0}, specifies an upper bound for the length of
    * the edge and face edges which is used to see if the intersection can be
    * determined using regular double precision
    * @param worldCoords if <code>true</code>, computes the intersection
    * in world coordinates. Otherwise, mesh local coordinates are used.
    * 
    * @return code 0 if there is no intersection; flags describing the
    * intersection if there is
    */
   public static int intersectSegmentTriangle (
      Point3d ipnt, Point3d pt, Point3d ph, Face face,
      double maxlen, boolean worldCoords) {

      if (!nativeSupportLoaded) {
         initialize();
      }

      Vertex3d v0 = face.he0.getTail();
      Vertex3d v1 = face.he0.getHead();
      Vertex3d v2 = face.he0.getNext().getHead();

      Point3d p0, p1, p2;

      if (worldCoords) {
         p0 = getWorldPoint (v0);
         p1 = getWorldPoint (v1);
         p2 = getWorldPoint (v2);
      }
      else {
         p0 = v0.pnt;
         p1 = v1.pnt;
         p2 = v2.pnt;
      }

      int res = -1;
      if (maxlen > 0) {
         res = intersectSegmentTriangleFast (ipnt, pt, ph, p0, p1, p2, maxlen);
      }
      if (res == -1) {
         // collect segment-face and call robust pred
         int it = 0;
         int ih = 1;
         int i0 = v0.getIndex()+2;
         int i1 = v1.getIndex()+2;
         int i2 = v2.getIndex()+2;
         res = intersectSegmentTriangle (
            ipnt, it, pt, ih, ph, i0, p0, i1, p1, i2, p2);
      }      
      return res;
   }

   /**
    * Fast test to see if p3 is "below", or "inside" the plane formed by the
    * counterclockwise triangle p0, p1, p2. This is done by computing the and
    * returning the volume of the corresponding tetrahedron. p3 is "below" if
    * this volume is negative, and "above" if it is positive. If the volume can
    * not be determined accurately within machine precision, the 0 is returned.
    * To help determine this precision, the argument {@code maxlen} may be
    * used; this should be {@code >=} the maximum length of all edges
    * associated with the calculation. If {@code maxlen} is given as zero, then
    * it is computed automatically from the edges of the tetrahedron.

    * @param p0 first triangle point
    * @param p1 second triangle point
    * @param p2 third triangle point
    * @param p3 point to test
    * @param maxlen optional; if {@code > 0}, should be the maximum length
    * of all edges associated with the calculation
    * @return positive or negative if p3 is above or inside the plane, or 0 if
    * this cannot be determined.
    */
   public static double orient3dFast (
      Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, double maxlen) {

      Vector3d r1 = new Vector3d();
      Vector3d r2 = new Vector3d();
      Vector3d r3 = new Vector3d();

      r1.sub (p1, p0);
      r2.sub (p2, p0);
      r3.sub (p3, p0);

      if (maxlen == 0) {
         double maxSqr = r1.normSquared();
         double nrmSqr = r2.normSquared();
         if (nrmSqr > maxSqr) {
            maxSqr = nrmSqr;
         }
         nrmSqr = r3.normSquared();
         if (nrmSqr > maxSqr) {
            maxSqr = nrmSqr;
         }
         maxlen = Math.sqrt (maxSqr);
      }
      double maxerr = ORIENT_EPS*6*maxlen*maxlen*maxlen;

      return orient3dFast (r1, r2, r3, maxerr);
   }

   /**
    * Computes the volume of the tet formed by the direction vectors a, b, c.
    * Also computes the rounding error associated with this calculation, using
    * the formulation given by equation (5) of Aftosmis, et al. 1998 ``Robust
    * and Efficient Cartesian Mesh Generation for Component-Based Geometry''.
    * If the volume is within this error bound, the method simply returns 0.
    */
   private static double orient3dFast (
      Vector3d a, Vector3d b, Vector3d c, double maxerr) {

      double cybx = c.y*b.x;
      double cxby = c.x*b.y;
      double cxay = c.x*a.y;
      double cyax = c.y*a.x;
      double axby = a.x*b.y;
      double aybx = a.y*b.x;

      double az = a.z;
      double bz = b.z;
      double cz = c.z;

      double res = az*(cybx-cxby) + bz*(cxay-cyax) + cz*(axby-aybx);
      double abs = (res < 0 ? -res : res);

      // check the coarser error bound first
      if (abs > maxerr) {
         return res;
      }

      if (cybx < 0) {
         cybx = -cybx;
      }
      if (cxby < 0) {
         cxby = -cxby;
      }
      if (cxay < 0) {
         cxay = -cxay;
      }
      if (cyax < 0) {
         cyax = -cyax;
      }
      if (axby < 0) {
         axby = -axby;
      }
      if (aybx < 0) {
         aybx = -aybx;
      }
      if (az < 0) {
         az = -az;
      }
      if (bz < 0) {
         bz = -bz;
      }
      if (cz < 0) {
         cz = -cz;
      }

      maxerr = ORIENT_EPS*(az*(cybx+cxby) + bz*(cxay+cyax) + cz*(axby+aybx));
      if (abs > maxerr) {
         return res;
      }
      else {
         return 0;
      }
   }

   /**
    * Tests for the intersection of a line segment and a triangle described by
    * a face using double precision arithmetic. Returns -1 if this cannot be
    * determined using double precision arithmetic. Otherise, returns 0 is
    * there is no intersection and a non-zero set of flags if there is. Flags
    * may include {@link #INTERSECTS} (always set if there is an intersection),
    * and {@link #TAIL_OUTSIDE} if the tail of the segment is outside the
    * triangle (in a counterclockwise sense). If there is an intersection, and
    * <code>ipnt</code> is non-<code>null</code>, then the intersection point
    * is computed and returned in <code>ipnt</code>.
    *
    * @param ipnt if non-<code>null</code> and if the segment and face
    * intersect, returns the intersection point
    * @param pt tail point of the line segment
    * @param ph head point of the line segment
    * @param p0 first face vertex point
    * @param p1 second face vertex point
    * @param p2 third face vertex point
    * @param maxlen maximum length used to determine if the computation can be
    * done using double precision arithmetic. Should be {@code >=} the length
    * of the segment and any of the face edges.
    * @return code 0 if there is no intersection; flags describing the
    * intersection if there is
    */
   public static int intersectSegmentTriangleFast (
      Point3d ipnt, Point3d pt, Point3d ph,
      Point3d p0, Point3d p1, Point3d p2, double maxlen) {

      // course upper error bound for orient3d. This only works if maxlen is >=
      // all segment and edge lengths.
      double maxerr = ORIENT_EPS*6*maxlen*maxlen*maxlen;
      
      Vector3d r0 = new Vector3d();
      Vector3d r1 = new Vector3d();
      Vector3d r2 = new Vector3d();

      r1.sub (p1, p0);
      r2.sub (p2, p0);
      r0.sub (pt, p0);
      double t = orient3dFast (r1, r2, r0, maxerr);
      if (t == 0) {
         return -1;
      }
      r0.sub (ph, p0);
      double h = orient3dFast (r1, r2, r0, maxerr);
      if (h == 0) {
         return -1;
      }
      int coordSign;

      // if (h < -tol && t < -tol) || (h > tol && t > tol) then both
      // the head and tail of the edge are definitely on the same
      // side of the face plane and there is no intersection.
      // Otherwise, if (-tol <= h <= tol || -tol <= t <= tol),
      // then head or tail are too close to the face plane to
      // tell and we return "don't know" (-1).
      if (h < 0) {
         if (t < 0) {
            return 0;
         }
         coordSign = -1;
      }
      else {
         if (t > 0) {
            return 0;
         }
         coordSign = 1;
      }
      
      Vector3d rt = new Vector3d();
      
      r0.sub (p0, ph);
      r1.sub (p1, ph);
      r2.sub (p2, ph);
      rt.sub (pt, ph);
      
      // b0, b1 and b2 are the non-normalized barycentric coordinates
      // of the intersection point with respect to the three triangle
      // vertices. If any bi < -tol, then the intersection point is
      // definitely *outside* the triangle. If any -tol <= bi <= tol,
      // then the intersection point is too close to an edge to tell.
      double b0 = coordSign*orient3dFast (r2, r1, rt, maxerr);
      if (b0 == 0) {
         return -1;
      }
      else if (b0 < 0) {
         return 0;
      }
      double b1 = coordSign*orient3dFast (r0, r2, rt, maxerr);
      if (b1 == 0) {
         return -1;
      }
      else if (b1 < 0) {
         return 0;
      }
      double b2 = coordSign*orient3dFast (r1, r0, rt, maxerr);
      if (b2 == 0) {
         return -1;
      }
      else if (b2 < 0) {
         return 0;
      }
      double s = Math.abs(t)/(Math.abs(t)+Math.abs(h));
      if (ipnt != null) {
         ipnt.combine (1-s, pt, s, ph);
      }
      // pnt.combine (b0, p0, b1, p1);
      // pnt.scaledAdd (b2, p2);
      // pnt.scale (1/(b0 + b1 + b2));
      int rcode = RobustPreds.INTERSECTS;
      if (coordSign == -1) {
         rcode |= RobustPreds.TAIL_OUTSIDE;
      }
      return rcode;
   }

    /**
    * Tests for the intersection of a line segment and a triangle.  Returns 0
    * is there is no intersection and a non-zero set of flags if there
    * is. Flags may include {@link #INTERSECTS} (always set if there is an
    * intersection), {@link #TAIL_OUTSIDE}, {@link #TAIL_ON_TRIANGLE}, {@link
    * #HEAD_ON_TRIANGLE}, {@link #E01_ON_SEGMENT}, {@link #E12_ON_SEGMENT}, and
    * {@link #E20_ON_SEGMENT}.  If there is an intersection, and
    * <code>ipnt</code> is non-<code>null</code>, then the intersection point
    * is computed and returned in <code>ipnt</code>.
    * 
    * <p>The computation first performs a quick numeric calculation to see if
    * the intersection can be determined within regular double precision
    * tolerances. If it cannot, then a more detailed calculation is performed
    * using robust predicates.
    *
    * <p>If tie-breaking is needed, then this is done using "Simulation of
    * Simplicity", as described in the class header, using the indices
    * supplied for each vertex. These should be unique to each vertex
    * across all queries.
    *
    * @param ipnt if non-<code>null</code> and if the edge and triangle
    * intersect, returns the intersection point
    * @param it tie-breaking index for tail segment vertex
    * @param pt position of tail segment vertex
    * @param ih tie-breaking index for head segment vertex
    * @param ph position of head segment vertex
    * @param i0 tie-breaking index for first face vertex
    * @param p0 position of first face vertex
    * @param i1 tie-breaking index for second face vertex
    * @param p1 position of second face vertex
    * @param i2 tie-breaking index for third face vertex
    * @param p2 position of third face vertex
    * @return code 0 if there is no intersection; flags describing the
    * intersection if there is
    */
   public static int intersectSegmentTriangle (
      Point3d ipnt, 
      int it, Vector3d pt, int ih, Vector3d ph, 
      int i0, Vector3d p0, int i1, Vector3d p1, int i2, Vector3d p2) {

      if (!nativeSupportLoaded) {
         initialize();
      }
      if (ipnt == null) {
         // right now, jniMethod needs ipnt regardless
         ipnt = new Point3d();
      }
      int rc =
         jniIntersectSegmentTriangle (
            it, pt.x, pt.y, pt.z,
            ih, ph.x, ph.y, ph.z,
            i0, p0.x, p0.y, p0.z,
            i1, p1.x, p1.y, p1.z,
            i2, p2.x, p2.y, p2.z, ipnt);
      return rc;
   }

  
}
