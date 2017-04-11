package maspack.geometry;

import maspack.matrix.Point3d;

public class RobustDegeneracyTest {
   
   public static void fail(String msg) {
      throw new RuntimeException(msg);
   }

   static boolean intersectSegmentTriangle (Point3d s0, Point3d s1, Face face) {
      return RobustPreds.intersectSegmentTriangle (
         null, s0, s1, face, 0, /*worldCoords=*/false) > 0;
   }

   /**
    *   1: __ \ __
    *         /
    */
   public static void testEdgeThrough() {
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
      System.out.println("Testing through edge");
      System.out.println("    intersections: " + isects);
      if (isects != 1) {
         fail("through edge test failed!");
      }
      
   }
   
   /**
    *   0: __ \/ __,  ______ 
    *                   /\
    *         
    */
   public static void testEdgeTangent() {
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
      System.out.println("Testing tangent edge");
      System.out.println("    intersections: " + isects);
      if( (isects%2) != 0) {
         fail("tangent edge test failed");
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
      System.out.println("    intersections: " + isects2);
      
      if ((isects2%2) != 0) {
         fail("tangent edge test failed");
      }
      
      
   }
   
   /**
    * 1: __|_____
    *         |
    * 
    */
   public static void testCoplanarFaceThrough() {
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
      System.out.println("Testing coplanar face through");
      System.out.println("    intersections: " + isects);
      if( (isects%2) != 1) {
         fail("coplanar face through test failed");
      }
      
   }
   
   /**
    * 1: __|__|___
    *         
    * 
    */
   public static void testCoplanarFaceTangent() {
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
      System.out.println("Testing coplanar face tangent");
      System.out.println("    intersections: " + isects);
      
      if ( (isects%2) != 0) {
         fail("coplanar face tangent test failed");
      }
      
   }
   
   public static void testVertexTangent() {
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
      System.out.println("Testing vertex tangent");
      System.out.println("    intersections: " + isects);
      
      if ((isects % 2) != 0) {
         // fail("vertex tangent test failed!");
      }
      
   }
   
   public static void testVertexThrough() {
      
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
      System.out.println("Testing vertex through");
      System.out.println("    intersections: " + isects);
      if( (isects%2) != 1) {
         fail("vertex through test failed");
      }
      
   }
   
   public static void main(String[] args) {
      
      // create several kinds of degeneracies and count intersections
      testEdgeThrough();
      testEdgeTangent();
      testCoplanarFaceThrough();
      testCoplanarFaceTangent();
      testVertexThrough();
      testVertexTangent();

   }
   
}
