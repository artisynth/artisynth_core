/**
 * Copyright (c) 2014, by the Authors: Andrew Larkin (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.fileutil.NativeLibraryManager;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public class RobustPreds {
   private static boolean nativeSupportLoaded = false;

   public static void initialize() {
//      try {
//	 System.out.println("loading GetCW");
//         System.loadLibrary ("GetCW");
//      }
//      catch (UnsatisfiedLinkError e) {
//         System.out.println (
//            "err=" + e.getMessage() + " java.library.path=" +
//            System.getProperty ("java.library.path"));
//         throw new UnsupportedOperationException (
//            "Can't load native library \"GetCW\".  java.library.path=" +
//            System.getProperty ("java.library.path"));
//      }
//      
//      System.out.println("Initial CW: " + jniGetCW() );
      
      // try loading in the native code
      try {
         NativeLibraryManager.load ("RobustPreds.1.0");
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
      
//      System.out.println("Current CW: " + jniGetCW() );
   }

   public static boolean isInitialized() {
      return nativeSupportLoaded;
   }

   public static boolean orient3d (
      Vertex3d v0, Vertex3d v1, Vertex3d v2, Vertex3d v3) {
      Point3d p0 = v0.getWorldPoint();
      Point3d p1 = v1.getWorldPoint();
      Point3d p2 = v2.getWorldPoint();
      Point3d p3 = v3.getWorldPoint();

      if (!nativeSupportLoaded)
         initialize();
      int result =
         jniOrient3d (
            v0.uniqueIndex, p0.x, p0.y, p0.z, v1.uniqueIndex, p1.x, p1.y, p1.z,
            v2.uniqueIndex, p2.x, p2.y, p2.z, v3.uniqueIndex, p3.x, p3.y, p3.z);
      //System.out.println("Current CW: " + jniGetCW() );
      if (result < 0)
         throw new RuntimeException ("RobustPreds failed with rc=" + result);
      return result == 1;
   }

   public static boolean intersectEdgeFace (
      HalfEdge edge, Face face, Point3d intersectionPoint) {
      // if (!edge.isPrimary()) throw new RuntimeException("non-primary edge");
      Vertex3d seg0, seg1, tri0, tri1, tri2;
      Point3d ps0, ps1, pt0, pt1, pt2;
      seg0 = edge.tail;
      ps0 = seg0.getWorldPoint();
      seg1 = edge.head;
      ps1 = seg1.getWorldPoint();
      HalfEdge e = face.firstHalfEdge();
      tri0 = e.tail;
      pt0 = tri0.getWorldPoint();
      tri1 = e.head;
      pt1 = tri1.getWorldPoint();
      e = e.getNext();
      tri2 = e.head;
      pt2 = tri2.getWorldPoint();
      if (!nativeSupportLoaded)
         initialize();
      int rc =
         jniIntersectSegmentTriangle (
            seg0.uniqueIndex, ps0.x, ps0.y, ps0.z, seg1.uniqueIndex, ps1.x,
            ps1.y, ps1.z, tri0.uniqueIndex, pt0.x, pt0.y, pt0.z,
            tri1.uniqueIndex, pt1.x, pt1.y, pt1.z, tri2.uniqueIndex, pt2.x,
            pt2.y, pt2.z, intersectionPoint);
      //System.out.println("Current CW: " + jniGetCW() );
      /*
       * if (rc == 1) { System.out.println(""); System.out.println("ps0 :=
       * Point3fd x: "+ps0.x+"d y: "+ps0.y+"d z: "+ps0.z+"d.");
       * System.out.println("ps1 := Point3fd x: "+ps1.x+"d y: "+ps1.y+"d z:
       * "+ps1.z+"d."); System.out.println("pt0 := Point3fd x: "+pt0.x+"d y:
       * "+pt0.y+"d z: "+pt0.z+"d."); System.out.println("pt1 := Point3fd x:
       * "+pt1.x+"d y: "+pt1.y+"d z: "+pt1.z+"d."); System.out.println("pt2 :=
       * Point3fd x: "+pt2.x+"d y: "+pt2.y+"d z: "+pt2.z+"d.");
       * System.out.println("int := Point3fd x: "+intersectionPoint.x+"d y:
       * "+intersectionPoint.y+"d z: "+intersectionPoint.z+"d.");
       * SurfaceMeshCollider.contactInfoRenderer.face = face;
       * SurfaceMeshCollider.contactInfoRenderer.edge = edge;
       * SurfaceMeshCollider.contactInfoRenderer.point = intersectionPoint;
       * 
       * SurfaceMeshCollider.contactInfoRenderer.face = null;
       * SurfaceMeshCollider.contactInfoRenderer.edge = null;
       * SurfaceMeshCollider.contactInfoRenderer.point = null; }
       */
      if (rc < 0)
         throw new RuntimeException (
            "error in jni intersectEdgeFace predicate: " + rc);
      return rc == 1;
   }

   /*
    * faceC and faceD must both have previously been found by
    * intersectEdgeFace(...) to intersect with edge. Let c be the intersection
    * of faceC and edge. Let d be the intersection of faceD and edge. Returns: 1
    * if d is closer to edge.tail than c 0 if c and d are equidistant from
    * edge.tail -1 if c is closer to edge.tail than d
    */

   public static int closestIntersection (Face faceC, HalfEdge edge, Face faceD) {
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

      if (!nativeSupportLoaded)
         initialize();
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

//   public static native int jniGetCW();
   public static native int jniInit (Point3d p);

   public static native int jniOrient3d (
      int i0, double p0x, double p0y, double p0z, int i1, double p1x,
      double p1y, double p1z, int i2, double p2x, double p2y, double p2z,
      int i3, double p3x, double p3y, double p3z);

   public static native int jniIntersectSegmentTriangle (
      int is0, double s0x, double s0y, double s0z, int is1, double s1x,
      double s1y, double s1z, int it0, double t0x, double t0y, double t0z,
      int it1, double t1x, double t1y, double t1z, int it2, double t2x,
      double t2y, double t2z, Point3d p);

   public static native int jniClosestIntersection (
      double ax, double ay, double az, double bx, double by, double bz,
      double c0x, double c0y, double c0z, double c1x, double c1y, double c1z,
      double c2x, double c2y, double c2z, double d0x, double d0y, double d0z,
      double d1x, double d1y, double d1z, double d2x, double d2y, double d2z);

   public static void main (String[] args) {
      Vector3d p0 = new Vector3d (1, 0, 0);
      Vector3d p1 = new Vector3d (-1, 1, 0);
      Vector3d p2 = new Vector3d (-1, -1, 0);
      double volume[] = new double[1];

      initialize();

      Vector3d p3 = new Vector3d (-1, 0, 1);
      int result =
         jniOrient3d (
            1, p0.x, p0.y, p0.z, 2, p1.x, p1.y, p1.z, 3, p2.x, p2.y, p2.z, 4,
            p3.x, p3.y, p3.z);
      System.out.println ("orient result=" + result + " volume=" + volume[0]);

      Vector3d a = new Vector3d (0, 0, 1);
      Vector3d b = new Vector3d (0, 0, -2);
      Vector3d c0 = new Vector3d (1, 0, 0);
      Vector3d c1 = new Vector3d (-1, 1, 0);
      Vector3d c2 = new Vector3d (-1, -1, 0);
      Vector3d d0 = new Vector3d (1, 0, -1);
      Vector3d d1 = new Vector3d (-1, 1, -1);
      Vector3d d2 = new Vector3d (-1, -1, -1);

      Point3d p = new Point3d();
      result =
         jniIntersectSegmentTriangle (
            1, a.x, a.y, a.z, 2, b.x, b.y, b.z, 3, c0.x, c0.y, c0.z, 4, c1.x,
            c1.y, c1.z, 5, c2.x, c2.y, c2.z, p);
      System.out.println (
         "jniIntersectSegmentTriangle result=" + result + " p=" + p);
      result =
         jniIntersectSegmentTriangle (
            1, a.x, a.y, a.z, 2, b.x, b.y, b.z, 6, d0.x, d0.y, d0.z, 7, d1.x,
            d1.y, d1.z, 8, d2.x, d2.y, d2.z, p);
      System.out.println (
         "jniIntersectSegmentTriangle result=" + result + " p=" + p);

      result =
         jniClosestIntersection (
            a.x, a.y, a.z, b.x, b.y, b.z, c0.x, c0.y, c0.z, c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z, d0.x, d0.y, d0.z, d1.x, d1.y, d1.z, d2.x, d2.y,
            d2.z);
      System.out.println ("jniClosestIntersection result=" + result);
   }

}
