package maspack.geometry;

import java.util.*;
import java.io.*;

import org.poly2tri.*;
import org.poly2tri.triangulation.sets.PointSet;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.Triangulatable;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import maspack.geometry.DelaunayTriangulator.IndexedPoint;
import maspack.geometry.DelaunayTriangulator.Triangle;
import maspack.geometry.MeshFactory.VertexMap;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Test class for DelaunayTriangulator. This does not test the Delaunay
 * triangulations per-se, but enures that the results produced by
 * DelaunayTriangulator are consistent with the results produced by the
 * implementing poly2tri package.
 */
public class DelaunayTriangulatorTest extends UnitTest {

   /**
    * Describes the vertices for a triangular face
    */
   public static class TriangleDesc {

      int[] myVtxIdxs;
      int myHashCode;

      TriangleDesc (Face face) {
         myVtxIdxs = face.getVertexIndices();
         for (int i=0; i<3; i++) {
            myHashCode += myVtxIdxs[i];
         }
      }

      boolean containsVertex (int idx) {
         return (myVtxIdxs[0]==idx || myVtxIdxs[1]==idx || myVtxIdxs[2]==idx);
      }

      /**
       * TriangleDesca are equal if they have the same vertices, regardless of
       * order.
       */
      public boolean equals (Object obj) {
         if (obj instanceof TriangleDesc) {
            TriangleDesc tri = (TriangleDesc)obj;
            return (containsVertex (tri.myVtxIdxs[0]) &&
                    containsVertex (tri.myVtxIdxs[1]) &&
                    containsVertex (tri.myVtxIdxs[2]));
         }
         return false;
      }

      /**
       * Hashcode is the same if the vertices are equal
       */
      public int hashCode() {
         return myHashCode;
      }
      
   }

   public void test (int npnts, int numc) {
      ArrayList<Vector2d> pnts = new ArrayList<>();
      ArrayList<TriangulationPoint> tpnts = new ArrayList<>();
      PolygonalMesh check = new PolygonalMesh();
      for (int i=0; i<npnts; i++) {
         Vector2d pnt = new Vector2d();
         pnt.setRandom();
         pnts.add (pnt);
         tpnts.add (new IndexedPoint (pnt, i));
         check.addVertex (new Point3d (pnt.x, pnt.y, 0));
      }
      int[] constraints = null;
      if (numc > 0) {
         constraints = new int[numc*2];
         for (int i=0; i<numc; i++) {
            constraints[i*2 + 0] = i;
            constraints[i*2 + 1] = i+1;
         }
      }
      Triangulatable tset;
      if (constraints == null) {
         PointSet pset = new PointSet (tpnts);
         Poly2Tri.triangulate (pset);
         tset = pset;
      }
      else {
         ConstrainedPointSet pset = new ConstrainedPointSet (tpnts, constraints);
         Poly2Tri.triangulate (pset);
         tset = pset;
      }

      for (DelaunayTriangle dtri : tset.getTriangles()) {
         int[] faceIdxs = new int[3];
         for (int i=0; i<3; i++) {
            faceIdxs[i] = ((IndexedPoint)dtri.points[i]).getIndex();
         }
         check.addFace (faceIdxs);
      }
      PolygonalMesh mesh = DelaunayTriangulator.createMesh (pnts, constraints);
      List<Triangle> tris = DelaunayTriangulator.triangulate (pnts, constraints);
      if (!check.epsilonEquals (mesh, 0)) {
         throw new TestException (
            "create mesh and check mesh differ, npnts=" +
            npnts + " numc=" + numc);
      }
      for (Face face : mesh.getFaces()) {
         Triangle tri = tris.get (face.getIndex());
         // check adjacents
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         int vidx = 0;
         do {
            HalfEdge next = he.getNext();
            Face opFace = next.getOppositeFace();
            int opIdxCheck = opFace != null ? opFace.getIndex() : -1;
            if (opIdxCheck != tri.getAdjacentTriangleIndex(vidx)) {
               throw new TestException (
                  "op triangle index is " + tri.getAdjacentTriangleIndex(vidx) +
                  ", expected " +opIdxCheck);
            }
            he = next;
            vidx++;
         }
         while (he != he0);
      }
      
   }

   void testSpecial0 () {
      String testMesh = new String (
         "v 0.29257047739466413 0.1815771349229961 0.46404503207543013\n" +
         "v -4.205073082684429 0.22537446782326442 1.0190807627182457\n" +
         "v 5.135651341534684 -3.1495836943054916 -1.7377746801677911\n" +
         "f 1 2 3\n");

      PolygonalMesh mesh = new PolygonalMesh();
      try {
         mesh.read (new StringReader (testMesh), /*zeroIndexed*/false);
      }
      catch (IOException e) {
         System.out.println ("ERROR: can't create mesh from string");
      }
      mesh = MeshFactory.triangulateIsotropically (mesh, 2);
      if (!mesh.isManifold()) {
         throw new TestException ("mesh is not manifold");
      }
   }

   void testSpecial1 () {
      String testMesh = new String (
         "v 50.0 -19.543867324914398 -1.2863937450861025\n" +
         "v 50.0 50.0 20.019651191951322\n" +
         "v -24.969411340923 -50.0 -0.052460553185065845\n" +
         "v -24.96941134092301 50.0 30.58438171141594\n" +
         "f 3 4 2 1\n");

      PolygonalMesh mesh = new PolygonalMesh();
      try {
         mesh.read (new StringReader (testMesh), /*zeroIndexed*/false);
      }
      catch (IOException e) {
         System.out.println ("ERROR: can't create mesh from string");
      }
      mesh = MeshFactory.triangulateIsotropically (mesh, 20);
      if (!mesh.isManifold()) {
         throw new TestException ("mesh is not manifold");
      }
   }

   void testSpecial2 () {
      String testMesh = new String (
         "v 50.0 -19.543867324914398 -1.6843395484340036\n" + 
         "v 50.0 -19.543867324914398 -1.2863937450861025\n" +
         "v 50.0 50.0 20.019651191951322\n" +
         "v 50.0 50.0 6.877441051663269\n" +
         "v -24.969411340923003 -50.0 -24.631323616784517\n" +
         "v -24.969411340923 -50.0 -0.052460553185065845\n" +
         "v -24.96941134092301 50.0 30.58438171141594\n" +
         "v -24.969411340923013 50.0 -12.319985565801591\n" +
         "f 6 7 3 2\n" +
         "f 8 7 6 5\n" +
         "f 4 8 5 1\n");

      PolygonalMesh mesh = new PolygonalMesh();
      try {
         mesh.read (new StringReader (testMesh), /*zeroIndexed*/false);
      }
      catch (IOException e) {
         System.out.println ("ERROR: can't create mesh from string");
      }
      mesh = MeshFactory.triangulateIsotropically (mesh, 20);
      if (!mesh.isManifold()) {
         throw new TestException ("mesh is not manifold");
      }
   }

   PolygonalMesh createRandomOneFaceMesh (int nverts, double s) {
      PolygonalMesh mesh = new PolygonalMesh();
      Point3d[] pnts = new Point3d[nverts];
      if (nverts == 3) {
         pnts[0] = new Point3d(0, 0, 0);
         pnts[1] = new Point3d(s, 0, 0);
         pnts[2] = new Point3d(0, s, 0);
      }
      else if (nverts == 4) {
         pnts[0] = new Point3d(0, 0, 0);
         pnts[1] = new Point3d(s, 0, 0);
         if (RandomGenerator.nextBoolean()) {
            pnts[2] = new Point3d(s, s, 0);
         }
         else {
            pnts[2] = new Point3d(s*0.8, s*0.9, 0);
         }
         pnts[3] = new Point3d(0, s, 0);
      }
      else {
         throw new IllegalArgumentException ("nverts not 3 or 4");
      }
      AffineTransform3d X = new AffineTransform3d();
      // create a random Affine transform, but avoid skinny triangles
      RotationMatrix3d U = new RotationMatrix3d();
      RotationMatrix3d V = new RotationMatrix3d();
      Vector3d sig = new Vector3d();
      sig.x = RandomGenerator.nextDouble (0.5, 1.5);
      sig.y = RandomGenerator.nextDouble (0.5, 1.5);
      sig.z = RandomGenerator.nextDouble (0.5, 1.5);
      U.setRandom();
      V.setRandom();
      X.A.set (U);
      X.A.mulCols (sig);
      X.A.mulTransposeRight (X.A, V);
      X.p.setRandom();

      for (int i=0; i<nverts; i++) {
         pnts[i].transform (X);
         mesh.addVertex (pnts[i]);
      }
      if (nverts == 3) {
         mesh.addFace (new int[] {0, 1, 2});
      }
      else {
         mesh.addFace (new int[] {0, 1, 2, 3});
      }
      return mesh;
   }

   

   private boolean meshesEqual (PolygonalMesh mesh0, PolygonalMesh mesh1) {
      // check that the vertices are equal 
      if (mesh0.numVertices() != mesh1.numVertices()) {
         return false;
      }
      for (int i=0; i<mesh0.numVertices(); i++) {
         Vertex3d v0 = mesh0.getVertex(i);
         Vertex3d v1 = mesh1.getVertex(i);
         if (!v0.getPosition().equals (v1.getPosition())) {
            return false;
         }
      }
      // check that the faces are equal, modulo permutation
      if (mesh0.numFaces() != mesh1.numFaces()) {
         return false;
      }
      HashSet<TriangleDesc> mesh0Tris = new HashSet<>();
      for (int i=0; i<mesh0.numFaces(); i++) {
         mesh0Tris.add (new TriangleDesc(mesh0.getFace(i)));
      }
      for (int i=0; i<mesh1.numFaces(); i++) {
         TriangleDesc tri = new TriangleDesc(mesh1.getFace(i));
         if (!mesh0Tris.contains (tri)) {
            return false;
         }
      }
      return true;
   }

   void testFaceTriangulation() {
      int nfaces = 1000;
      int nchks = 1;
      double size = 10;
      double edgeLen = 2;
      for (int i=0; i<nfaces; i++) {
         PolygonalMesh mesh0 = createRandomOneFaceMesh (i%2 == 0 ? 3 : 4, size);
         edgeLen = RandomGenerator.nextDouble (1, 6);
         // triangulate each face multiple times and make sure the
         // result is the same
         PolygonalMesh lastmesh = null;
         for (int j=0; j<nchks; j++) {
            PolygonalMesh mesh =
               MeshFactory.triangulateIsotropically (mesh0, edgeLen);
            if (!mesh.isManifold()) {
               throw new TestException (
                  "triangulated face not manifold at iteration "+j);
            }
            if (lastmesh != null) {
               if (!meshesEqual (mesh, lastmesh)) {
                  throw new TestException (
                     "triangulated face differs at iteration "+j);
               }
            }
            lastmesh = mesh;
         }
      }
   }

   public void test() {
      testSpecial0();
      testSpecial1();
      testSpecial2();
      testFaceTriangulation();
      for (int numc=0; numc<3; numc++) {
         // can't spec more than 2 constraints because otherwise they may
         // cross, given how the test code constructs then by simply creating a
         // polyline from the first numc+1 points.
         test (3, numc);
         test (4, numc);
         test (10, numc);
         test (12, numc);
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      DelaunayTriangulatorTest tester = new DelaunayTriangulatorTest();
      tester.runtest();
   }

}
