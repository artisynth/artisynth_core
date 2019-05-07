package maspack.geometry;

import maspack.util.*;
import maspack.matrix.*;


public class RobustPredsTest extends UnitTest {

   boolean verbose;
   
   void segmentTriangleTest() {

      Vector3d ps0 = new Vector3d (0.0, 0.0, -1.75);
      Vector3d ps1 = new Vector3d (0.0, 0.0, 2.05);
      Vector3d pt10 = new Vector3d (0.0, 0.0, 0.8999999999999999);
      Vector3d pt5 = new Vector3d (-1.2, -1.4695761589768238E-16, 0.6);
      Vector3d pt4 = new Vector3d (7.347880794884119E-17, -1.2, 0.6);
      Vector3d pt7 = new Vector3d (1.2, 0.0, 0.6);
      Vector3d pt9 = new Vector3d (7.347880794884119E-17, 1.2, 0.6);
      Vector3d pt11 = new Vector3d (0.0, 0.0, -0.6);
      Vector3d pt2 = new Vector3d (-1.2, -1.4695761589768238E-16, -0.6);
      Vector3d pt8 = new Vector3d (7.347880794884119E-17, 1.2, -0.6);
      Point3d ipnt = new Point3d();
      int rc;

      rc = RobustPreds.intersectSegmentTriangle (
         ipnt, 0, ps0, 1, ps1, 10, pt10, 5, pt5, 4, pt4);
      checkEquals ("intersectSegmentTriangle result", rc, 0);
      if (verbose) {
         System.out.println ("0 rc=" + rc);
      }
      rc = RobustPreds.intersectSegmentTriangle (
         ipnt, 0, ps0, 1, ps1, 10, pt10, 4, pt4, 7, pt7);
      checkEquals ("intersectSegmentTriangle result", rc, 0);
      if (verbose) {
         System.out.println ("1 rc=" + rc);
      }
      rc = RobustPreds.intersectSegmentTriangle (
         ipnt, 0, ps0, 1, ps1, 10, pt10, 7, pt7, 9, pt9);
      checkEquals ("intersectSegmentTriangle result", rc, 0);
      if (verbose) {
         System.out.println ("2 rc=" + rc);
      }
      rc = RobustPreds.intersectSegmentTriangle (
         ipnt, 0, ps0, 1, ps1, 10, pt10, 9, pt9, 5, pt5);
      //checkEquals ("intersectSegmentTriangle result=", rc, 0x51);
      if (verbose) {
         System.out.println ("3 rc=" + rc);
      }
   }

   void specialTest() {
      
      Point3d pnt = new Point3d();
      int res;

      double[] vals0 = new double[] {
         91, -2.220446049250313E-16, 2.5, 1.1796119636642288E-16,
         79, 0.0, 1.5, 7.632783294297951E-17,
         27, -0.7142857142857144, 2.0, 1.5662074811676317E-16,
         19, -0.7142857142857142, 0.6666666666666665, 1.0110959688550534E-16,
         28, 0.7142857142857142, 2.0, 3.766828119263923E-17,
      };

      double[] vals1 = new double[] {
         102, -1.0000000000000016, 1.5000000000000009, 0.4999999999999998,
         85, -1.000000000000001, 0.5000000000000006, -0.5000000000000003,
         48, -1.0000000000000018, 2.0000000000000013, -3.608224830031759E-16,
         37, -1.0000000000000013, 1.0000000000000007, -2.498001805406602E-16,
         49, -7.771561172376096E-16, 2.000000000000001, -1.6653345369377348E-16
      };

      double[] vals2 = new double[] {
         78, 1.5000000000000004, -5.134781488891349E-16, -5.551115123125783E-17,
         66, 0.5000000000000003, -3.191891195797325E-16, -5.551115123125783E-17,
         27, 1.6653345369377348E-16, -2.220446049250313E-16, -5.551115123125783E-17,
         17, 1.0000000000000007, -1.0000000000000009, -1.5265566588595902E-16,
         28, 1.0000000000000004, -4.163336342344337E-16, -5.551115123125783E-17,
      };

      double[] vals3 = new double[] {
         43, -1.9617481518875979, 1.8009297843138277, -3.484728483100079,
         49, -0.5334841619041738, 2.390022449434669, -2.214662537728642,
         20, -1.965916506803723, 1.802750915928569, -3.480857516088048,
         13, 2.347757815478261, 9.00049825608733, -2.2220298596860473,
         14, 2.331384503194007, 3.5645637796602996, 0.3177237273055801,
      };

      Point3d chk = new Point3d (
         -1.1102230246251565E-16, 2.0, 9.71445146547012E-17);

      for (double[] vals : new double[][] { vals0, vals0, vals0, vals0 }) {
         res = RobustPreds.jniIntersectSegmentTriangle (
            (int)vals[0], vals[1], vals[2], vals[3], 
            (int)vals[4], vals[5], vals[6], vals[7], 
            (int)vals[8], vals[9], vals[10], vals[11], 
            (int)vals[12], vals[13], vals[14], vals[15], 
            (int)vals[16], vals[17], vals[18], vals[19], pnt);

         checkEquals (
            "jniIntersectSegmentTriangle: result", res, 0x41);
         checkEquals (
            "jniIntersectSegmentTriangle: intersection point", pnt, chk);

         if (verbose) {
            System.out.printf ("res=%x\n", res);
            System.out.println ("pnt=" + pnt);
         }
      }

   }

   void directJniTests() {
      Vector3d p0 = new Vector3d (1, 0, 0);
      Vector3d p1 = new Vector3d (-1, 1, 0);
      Vector3d p2 = new Vector3d (-1, -1, 0);
      double volume[] = new double[1];

      Vector3d p3 = new Vector3d (-1, 0, 0);
      int result =
         RobustPreds.jniOrient3d (
            1, p0.x, p0.y, p0.z, 2, p1.x, p1.y, p1.z, 3, p2.x, p2.y, p2.z, 4,
            p3.x, p3.y, p3.z);
      checkEquals ("orient result", result, 1);
      if (verbose) {
         System.out.println ("orient result=" + result);
      }
      
      result =
         RobustPreds.jniOrient3d (
            1, 0, 0, 0, 
            2, 1, 0, 0, 
            3, 0, 1, 0, 
            4, 0, 0, 1e-10);
      checkEquals ("orient result", result, 0);
      checkEquals ("orient volume", volume[0], 0.0);
      if (verbose) {
         System.out.println ("orient result=" + result + " volume=" + volume[0]);
      }

      result =
         RobustPreds.jniOrient3d (
            1, 1, 0, 0,  2, 0, 1, 0,  3, 0, 0, 1,  4, 0, 0, 0);

      checkEquals ("simple orient result", result, 1);
      if (verbose) {
         System.out.println ("simple orient result=" + result);
      }

      Vector3d a = new Vector3d (0, 0, 1);
      Vector3d b = new Vector3d (0, 0, -2);
      Vector3d c0 = new Vector3d (1, 0, 0);
      Vector3d c1 = new Vector3d (-1, 1, 0);
      Vector3d c2 = new Vector3d (-1, -1, 0);
      Vector3d d0 = new Vector3d (1, 0, -1);
      Vector3d d1 = new Vector3d (-1, 1, -1);
      Vector3d d2 = new Vector3d (-1, -1, -1);

      Point3d ipnt = new Point3d();
      Point3d ichk = new Point3d (0, 0, 1.1102230246251565E-16);
      result =
         RobustPreds.jniIntersectSegmentTriangle (
            1, a.x, a.y, a.z, 2, b.x, b.y, b.z, 3, c0.x, c0.y, c0.z, 4, c1.x,
            c1.y, c1.z, 5, c2.x, c2.y, c2.z, ipnt);
      checkEquals ("jniIntersectSegmentTriangle result", result, 3);
      checkEquals ("jniIntersectSegmentTriangle ipnt", ipnt, ichk);
      if (verbose) {
         System.out.println (
            "jniIntersectSegmentTriangle result=" + result + " ipnt=" + ipnt);
      }
      ichk = new Point3d (0, 0, -0.9999999999999999);
      result =
         RobustPreds.jniIntersectSegmentTriangle (
            1, a.x, a.y, a.z, 2, b.x, b.y, b.z, 6, d0.x, d0.y, d0.z, 7, d1.x,
            d1.y, d1.z, 8, d2.x, d2.y, d2.z, ipnt);
      checkEquals ("jniIntersectSegmentTriangle result", result, 3);
      checkEquals ("jniIntersectSegmentTriangle ipnt", ipnt, ichk);
      if (verbose) {
         System.out.println (
            "jniIntersectSegmentTriangle result=" + result + " ipnt=" + ipnt);
      }

      result =
         RobustPreds.jniClosestIntersection (
            a.x, a.y, a.z, b.x, b.y, b.z, c0.x, c0.y, c0.z, c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z, d0.x, d0.y, d0.z, d1.x, d1.y, d1.z, d2.x, d2.y,
            d2.z);
      checkEquals ("jniClosestIntersection result", result, -1);
      if (verbose) {
         System.out.println ("jniClosestIntersection result=" + result);
      }
   }

   public void fail(String msg) {
      throw new RuntimeException(msg);
   }

   boolean intersectSegmentTriangle (Point3d s0, Point3d s1, Face face) {
      return RobustPreds.intersectSegmentTriangle (
         null, s0, s1, face, 0, /*worldCoords=*/false) > 0;
   }

   /**
    *   1: __ \ __
    *         /
    */
   public void testEdgeThrough() {
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d v0 = mesh.addVertex(-1, 0, 0);
      Vertex3d v1 = mesh.addVertex(0, 1, 0);
      Vertex3d v2 = mesh.addVertex(0, -1, 0);
      Vertex3d v3 = mesh.addVertex(1, 0, 0);
      mesh.addFace(v0, v1, v2);
      mesh.addFace(v2, v1, v3);
      
      
      // segment for intersection
      Point3d s0 = new Point3d(0, 0, -1);
      Point3d s1 = new Point3d(0, 0, 1);
      
      int isects = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects;
         }
      }
      if (verbose) {
         System.out.println("Testing through edge");
         System.out.println("    intersections: " + isects);
      }
      if (isects != 1) {
         throw new TestException("through edge test failed!");
      }
      
   }
   
   /**
    *   0: __ \/ __,  ______ 
    *                   /\
    *         
    */
   public void testEdgeTangent() {
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d v0 = mesh.addVertex(-1, 0, -1);
      Vertex3d v1 = mesh.addVertex(0, 1, 0);
      Vertex3d v2 = mesh.addVertex(0, -1, 0);
      Vertex3d v3 = mesh.addVertex(-1, 0, 1);
      mesh.addFace(v0, v1, v2);
      mesh.addFace(v2, v1, v3);
      
      // segment for intersection
      Point3d s0 = new Point3d(0, 0, -1);
      Point3d s1 = new Point3d(0, 0, 1);
      
      int isects = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects;
         }
      }
      if (verbose) {
         System.out.println("Testing tangent edge");
         System.out.println("    intersections: " + isects);
      }
      if( (isects%2) != 0) {
         throw new TestException("tangent edge test failed");
      }
      
      // flip around
      v0.getPosition().set(1, 0, 1);
      v3.getPosition().set(1, 0, -1);
      
      int isects2 = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects2;
         }
      }
      if (verbose) {
         System.out.println("    intersections: " + isects2);
      }
      if ((isects2%2) != 0) {
         throw new TestException("tangent edge test failed");
      }
      
      
   }
   
   /**
    * 1: __|_____
    *         |
    * 
    */
   public void testCoplanarFaceThrough() {
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d v0 = mesh.addVertex(-1, 0, 0);
      Vertex3d v1 = mesh.addVertex(0, 1, 0);
      Vertex3d v2 = mesh.addVertex(0, -1, 0);
      Vertex3d v3 = mesh.addVertex(0, 1, 1);
      Vertex3d v4 = mesh.addVertex(0, -1, 1);
      Vertex3d v5 = mesh.addVertex(1, 0, 1);
      
      mesh.addFace(v0, v1, v2);
      mesh.addFace(v3, v2, v1);
      mesh.addFace(v2, v3, v4);
      mesh.addFace(v4, v3, v5);
      
      // segment for intersection
      Point3d s0 = new Point3d(0, 0, -1);
      Point3d s1 = new Point3d(0, 0, 1);
      
      int isects = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects;
         }
      }
      if (verbose) {
         System.out.println("Testing coplanar face through");
         System.out.println("    intersections: " + isects);
      }
      if( (isects%2) != 1) {
         throw new TestException("coplanar face through test failed");
      }
      
   }
   
   /**
    * 1: __|__|___
    *         
    * 
    */
   public void testCoplanarFaceTangent() {
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d v0 = mesh.addVertex(-1, 0, 0);
      Vertex3d v1 = mesh.addVertex(0, 1, 0);
      Vertex3d v2 = mesh.addVertex(0, -1, 0);
      Vertex3d v3 = mesh.addVertex(0, 1, 1);
      Vertex3d v4 = mesh.addVertex(0, -1, 1);
      Vertex3d v5 = mesh.addVertex(-1, 0, 1);
      
      mesh.addFace(v0, v1, v2);
      mesh.addFace(v3, v2, v1);
      mesh.addFace(v2, v3, v4);
      mesh.addFace(v4, v3, v5);
      
      // segment for intersection
      Point3d s0 = new Point3d(0, 0, -2);
      Point3d s1 = new Point3d(0, 0, 2);
      
      int isects = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects;
         }
      }
      if (verbose) {
         System.out.println("Testing coplanar face tangent");
         System.out.println("    intersections: " + isects);
      }
      if ( (isects%2) != 0) {
         throw new TestException("coplanar face tangent test failed");
      }
      
   }
   
   public void testVertexTangent() {
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d v0 = mesh.addVertex(-1, -1, -1);
      Vertex3d v1 = mesh.addVertex(-1,  1, -1);
      Vertex3d v2 = mesh.addVertex(-1,  1,  1);
      Vertex3d v3 = mesh.addVertex(-1, -1,  1);
      Vertex3d v4 = mesh.addVertex( 0,  0,  0);
      
      mesh.addFace(v4, v0, v1);
      mesh.addFace(v4, v1, v2);
      mesh.addFace(v4, v2, v3);
      mesh.addFace(v4, v3, v0);
      
      // segment for intersection
      Point3d s0 = new Point3d(0, 0,-2);
      Point3d s1 = new Point3d(0, 0, 2);
      
      int isects = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects;
            System.out.println("intersects face " + face.getIndex());
         }
      }
      if (verbose) {
         System.out.println("Testing vertex tangent");
         System.out.println("    intersections: " + isects);
      }
      if ((isects % 2) != 0) {
         // throw new TestException("vertex tangent test failed!");
      }
      
   }
   
   public void testVertexThrough() {
      
      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d v0 = mesh.addVertex(-1, -1, 0);
      Vertex3d v1 = mesh.addVertex(-1, 1, 0);
      Vertex3d v2 = mesh.addVertex(1, 1, 0);
      Vertex3d v3 = mesh.addVertex(1, -1, 0);
      Vertex3d v4 = mesh.addVertex(0, 0, 0);
      
      mesh.addFace(v4, v0, v1);
      mesh.addFace(v4, v1, v2);
      mesh.addFace(v4, v2, v3);
      mesh.addFace(v4, v3, v0);
      
      // segment for intersection
      Point3d s0 = new Point3d(0, 0, -2);
      Point3d s1 = new Point3d(0, 0, 2);
      
      int isects = 0;
      for (Face face : mesh.getFaces()) {
         if (intersectSegmentTriangle(s0, s1, face)) {
            ++isects;
         }
      }
      if (verbose) {
         System.out.println("Testing vertex through");
         System.out.println("    intersections: " + isects);
      }
      if( (isects%2) != 1) {
         throw new TestException("vertex through test failed");
      }
      
   }
   
   void degeneracyTests() {

      // create several kinds of degeneracies and count intersections
      testEdgeThrough();
      testEdgeTangent();
      testCoplanarFaceThrough();
      testCoplanarFaceTangent();
      testVertexThrough();
      testVertexTangent();
   }

   public void test() {
      // load in the native library
      RobustPreds.initialize(); 
      degeneracyTests();
      specialTest();
      directJniTests();
      segmentTriangleTest();
   }

   public static void main (String[] args) {
      RobustPredsTest tester = new RobustPredsTest();

      tester.verbose = false;
      tester.runtest();
   }   

}

