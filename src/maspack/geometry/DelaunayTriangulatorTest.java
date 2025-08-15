package maspack.geometry;

import java.util.*;

import org.poly2tri.*;
import org.poly2tri.triangulation.sets.PointSet;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.Triangulatable;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import maspack.geometry.DelaunayTriangulator.IndexedPoint;
import maspack.geometry.DelaunayTriangulator.Triangle;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Test class for DelaunayTriangulator. This does not test the Delaunay
 * triangulations per-se, but enures that the results produced by
 * DelaunayTriangulator are consistent with the results produced by the
 * implementing poly2tri package.
 */
public class DelaunayTriangulatorTest extends UnitTest {

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

   public void test() {
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
