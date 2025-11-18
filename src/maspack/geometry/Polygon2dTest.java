package maspack.geometry; 


import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class Polygon2dTest extends UnitTest {

   static final double INF = Double.POSITIVE_INFINITY;

   protected Point2d[] doubleToPoint2d (double[] coords) {
      int nump = coords.length/2;
      Point2d[] pnts = new Point2d[nump];
      for (int i=0; i<nump; i++) {
         pnts[i] = new Point2d (coords[2*i], coords[2*i+1]);
      }
      return pnts;
   }      

   protected void testSimpleConvexHull (double[] pcoords, double[] hcoords) {

      Polygon2d poly = new Polygon2d (doubleToPoint2d(pcoords));
      Polygon2d chk = new Polygon2d (doubleToPoint2d(hcoords));
      int numv = poly.numVertices();
      for (int i=0; i<numv; i++) {
         if (i > 0) {
            poly.shiftFirstVertex();
         }
         Polygon2d hull = poly.simpleConvexHull();
         if (!hull.equals (chk)) {
            System.out.println ("computed hull=\n" + hull.toString());
            System.out.println ("expected hull=\n" + chk.toString());
            throw new TestException ("Incorrect simpleConvexHull");
         }
      }
   }

   private ArrayList<Point2d> addNoise (ArrayList<Point2d> points, double eps) {
      ArrayList<Point2d> newpoints = new ArrayList<>();
      for (Point2d p : points) {
         Point2d newp = new Point2d(p);
         newp.x += RandomGenerator.nextDouble (-eps, eps);
         newp.y += RandomGenerator.nextDouble (-eps, eps);
         newpoints.add (newp);
      }
      return newpoints;
   }

   private Polygon2d extractHull (ArrayList<Point2d> points, int[] hidxs) {
      ArrayList<Point2d> hpnts = new ArrayList<>();
      for (int idx : hidxs) {
         hpnts.add (new Point2d (points.get(idx)));
      }
      return new Polygon2d (hpnts);
   }
   protected void testConvexHull (double[] pcoords, int[] hidxs) {
      double tol = 1e-10;
      Point2d[] parray = doubleToPoint2d(pcoords);
      ArrayList<Point2d> pnts = new ArrayList<>();
      for (Point2d p : parray) {
         pnts.add (p);
      }
      Polygon2d chk = extractHull (pnts, hidxs);
      ConvexPolygon2d hull = new ConvexPolygon2d();
      hull.computeAndSetHull (pnts, tol);
      if (!hull.equals (chk)) {
         System.out.println ("computed hull=\n" + hull.toString());
         System.out.println ("expected hull=\n" + chk.toString());
         throw new TestException ("Incorrect convexHull");
      }
      for (int k=0; k<pnts.size(); k++) {
         ArrayList<Point2d> newpnts = addNoise (pnts, 0.01*tol);
         chk = extractHull (newpnts, hidxs);
         Collections.shuffle (newpnts, RandomGenerator.get());
         hull.computeAndSetHull (newpnts, 0);
         if (!hull.isConvex()) {
            System.out.println ("computed hull=\n" + hull.toString());
            throw new TestException (
               "Computed hull is not convex, shuffle "+k);
         }
      }
   }

   public void testSimpleConvexHull() {
      // trivial cases
      testSimpleConvexHull (
         new double[] { 1.2, 2.3,  4.5, 6.0 },
         new double[] { 4.5, 6.0,  1.2, 2.3 });
      testSimpleConvexHull (
         new double[] { 0.0, 0.0,  1.0, 1.0,  2.0, 0.0 },
         new double[] { 2.0, 0.0,  1.0, 1.0,  0.0, 0.0 });
      testSimpleConvexHull (
         new double[] { 0.0, 0.0,  2.0, 0.0,  1.0, 1.0 },
         new double[] { 2.0, 0.0,  1.0, 1.0,  0.0, 0.0 });

      testSimpleConvexHull (
         new double[] {
            0,0, 0,3, 4,3, 4,1, 3,1, 4,0 },
         new double[] {
            4,0, 4,3, 0,3, 0,0 });

      testSimpleConvexHull (
         new double[] {
            0,0, 1,2, 0,3, 2,3, 2,2, 3,2, 3,3, 2,4, 4,3, 4,1, 3,1, 4,0 },
         new double[] {
            4,0, 4,3, 2,4, 0,3, 0,0 });
      
      testSimpleConvexHull (
         new double[] {
            4,0, 3,1, 4,1, 4,3, 2,4, 3,3, 3,2, 2,2, 2,3, 0,3, 1,2, 0,0 },
         new double[] {
            4,0, 4,3, 2,4, 0,3, 0,0 });

      testSimpleConvexHull (
         new double[] { 0,0, 1,2, 3,2, 3,3, 2,4, 4,5, 5,4, 4,2, 3,1, 4,0 },
         new double[] { 0,0, 4,0, 5,4, 4,5, 2,4 });
   }

   public void testConvexHull () {

      testConvexHull (
         new double[] { 0.0, 0.0,  1.0, 1.0,  2.0, 0.0 },
         new int[] { 0, 2, 1 });

      testConvexHull (
         new double[] { 0,0, 0,3, 4,3, 4,1, 3,1, 4,0 },
         // indices:      0    1    2    3    4    5
         new int[] { 0, 5, 2, 1 });

      testConvexHull (
         new double[] {
            0,0, 1,2, 0,3, 2,3, 2,2, 3,2, 3,3, 2,4, 4,3, 4,1, 3,1, 4,0 },
         //   0    1    2    3    4    5    6    7    8    9   10   11
         new int[] {
            0, 11, 8, 7, 2 });

      testConvexHull (
         new double[] {
            4,0, 3,1, 4,1, 4,3, 2,4, 3,3, 3,2, 2,2, 2,3, 0,3, 1,2, 0,0 },
         //   0    1    2    3    4    5    6    7    8    9   10   11
         new int[] {
            11, 0, 3, 4, 9 });

      testConvexHull (
         new double[] {
            0,0, 1,2, 3,2, 3,3, 2,4, 4,5, 5,4, 4,2, 3,1, 4,0 },
         //   0    1    2    3    4    5    6    7    8    9
         new int[] { 0, 9, 6, 5, 4 });

      testConvexHull (
         new double[] {
            0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2, 2,2, 3,2, 
         //   0    1    2    3    4    5    6    7    8    9   10   11
            0,3, 1,3, 2,3, 3,3 },
         //  12   13   14   15
         new int[] { 0, 3, 15, 12 });
   }

   private void testSpecial() {
      testConvexHull (
         new double[] {
            0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2, 2,2, 3,2, 
         //   0    1    2    3    4    5    6    7    8    9   10   11
            0,3, 1,3, 2,3, 3,3 },
         //  12   13   14   15
         new int[] { 0, 3, 15, 12 });
   }

   void testNearestEdge (
      Polygon2d poly, double x, double y, double nearx, double neary) {
      
      double tol = 1e-14;
      Point2d nearChk = new Point2d (nearx, neary);
      Point2d nearPnt = new Point2d ();
      Vertex2d vtx = poly.nearestEdge (nearPnt, new Point2d (x, y));
      checkEquals ("nearPnt", nearPnt, nearChk, tol);
   }

   private void testNearestEdge() {
      Polygon2d poly = new Polygon2d(
            new double[] { 0,0, 2,0, 2,2, 1,1, -1,1 });
      
      testNearestEdge (poly, 0,0,  0,0);
      testNearestEdge (poly, 2,0,  2,0);
      testNearestEdge (poly, 2,2,  2,2);
      testNearestEdge (poly, 1,1,  1,1);
      testNearestEdge (poly, -1,1,  -1,1);

      testNearestEdge (poly, 1,0,   1,0);
      testNearestEdge (poly, 1,-1,  1,0);
      testNearestEdge (poly, 1,0.1, 1,0);      
      testNearestEdge (poly, 3,-1,  2,0);
      testNearestEdge (poly, 2,1,  2,1);
      testNearestEdge (poly, 2.1,1,  2,1);
      testNearestEdge (poly, 1.9,1,  2,1);

      testNearestEdge (poly, -2,2,  -1,1);
      testNearestEdge (poly, -1,2,  -1,1);
      testNearestEdge (poly, -2,1,  -1,1);
      testNearestEdge (poly,  0,2,  0,1);
   }

   public void testComputeAreaIntegrals (double... xycoords) {
      if (xycoords.length%2 != 0) {
         throw new InternalErrorException ("uneven number of xycoords specified");
      }
      
      Polygon2d poly = new Polygon2d(xycoords);
      Vector2d moa1 = new Vector2d();
      Vector3d moa2 = new Vector3d();
      double area = poly.computeAreaIntegrals (moa1, moa2);

      PolygonalMesh prism = MeshFactory.createPrism (xycoords, 1.0);

      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();
      double areaChk = prism.computeVolumeIntegrals (mov1, mov2, pov);

      double TOL = 1e-10;
      Vector2d moa1Chk = new Vector2d (mov1.x, mov1.y);
      Vector3d moa2Chk = new Vector3d (mov2.x, mov2.y, pov.z);

      checkEquals ("area", area, areaChk, TOL);
      checkEquals ("moa1", moa1, moa1Chk, TOL);
      checkEquals ("moa2", moa2, moa2Chk, TOL);

      // System.out.printf (
      //    "area=%g moa1=%g %g  moa2=%g %g %g\n",
      //    area, moa1.x, moa1.y, moa2.x, moa2.y, moa2.z);

      double[] revc = new double[xycoords.length];
      for (int i=0; i<revc.length; i += 2) {
         revc[i  ] = xycoords[revc.length-1-i-1];
         revc[i+1] = xycoords[revc.length-1-i];
      }
      poly = new Polygon2d (revc);
      area = poly.computeAreaIntegrals (moa1, moa2);

      checkEquals ("reverse area", area, areaChk, TOL);
      checkEquals ("reverse moa1", moa1, moa1Chk, TOL);
      checkEquals ("reverse moa2", moa2, moa2Chk, TOL);
   }

   public void testComputeAreaIntegrals() {
      testComputeAreaIntegrals (0,0, 1,0, 1,3, 0,3);
      testComputeAreaIntegrals (-0.5,-1.5, 0.5,-1.5, 0.5,1.5, -0.5,1.5);
      testComputeAreaIntegrals (-1,0, 0,-1, 1,0, 0,1, 0,2, -1,3);
      testComputeAreaIntegrals (0,0, 2,0, 1,1, -1,1, -1,2, -2,2, -2,2);
   }

   public void testIntersectRay() {
      double[] xycoords = new double[] {
         0.0,0.0, 1.0,0.0, 1.0,1.0 };
      Polygon2d poly = new Polygon2d (xycoords);

      double EPS = 1e-10;

      Vertex2d vtx = poly.getFirstVertex();
      Point2d pi = new Point2d ();
      Point2d p2 = new Point2d (0.5, -0.5);
      Vector2d u = new Vector2d (0, 0.5+EPS);
      double s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect", pi, new Point2d(0.5, 0), 2*EPS);

      u.negate();
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect null", s==INF, true);
      s = poly.intersectLine (pi, vtx, p2, u);
      checkEquals ("line intersect null", s==INF, false);
      checkEquals ("line intersect", pi, new Point2d(0.5, 0), 2*EPS);


      u.set (0.0, 0.5-EPS);
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect", pi, new Point2d(0.5, 0), 2*EPS);

      u.negate();
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect null", s==INF, true);
      s = poly.intersectLine (pi, vtx, p2, u);
      checkEquals ("line intersect null", s==INF, false);
      checkEquals ("line intersect", pi, new Point2d(0.5, 0), 2*EPS);

      u.set (1-EPS, 1);
      poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect", pi, new Point2d(1.0, 0), 2*EPS);

      u.negate();
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect null", s==INF, true);
      s = poly.intersectLine (pi, vtx, p2, u);
      checkEquals ("line intersect null", s==INF, false);
      checkEquals ("line intersect", pi, new Point2d(1.0, 0), 2*EPS);

      u.set (1+EPS, 1);
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect null", s==INF, true);

      u.set (-1+EPS, 1);
      poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect", pi, new Point2d(0, 0), 2*EPS);

      u.negate();
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect null", s==INF, true);
      s = poly.intersectLine (pi, vtx, p2, u);
      checkEquals ("line intersect null", s==INF, false);
      checkEquals ("line intersect", pi, new Point2d(0, 0), 2*EPS);

      u.set (-1-EPS, 1);
      s = poly.intersectRay (pi, vtx, p2, u);
      checkEquals ("ray intersect null", s==INF, true);
   }

   void testCpolyLineIntersect (
      ConvexPolygon2d cpoly, Point2d p0, Vector2d u, double... svals) {

      Vector2d schk = svals.length==0 ? null : new Vector2d(svals[0], svals[1]);

      double[] srng = cpoly.intersectLine (p0, u);
      Vector2d svec = srng==null ? null : new Vector2d(srng[0], srng[1]);

      if (svec==null) {
         if (schk != null) {
            throw new TestException ("No range found, expected " + schk);
         }
      }
      else {
         if (schk == null) {
            throw new TestException ("Found range "+svec+"; expected none");
         }
         checkEquals ("Intersection range", svec, schk, 1e-8);
         Vector2d uneg = new Vector2d (u);
         uneg.negate();
         schk.set (-schk.y, -schk.x);
         srng = cpoly.intersectLine (p0, uneg);
         if (srng == null) {
            throw new TestException ("No range found, expected " + schk);
         }
         svec.set (srng[0], srng[1]);
         checkEquals ("Intersection range", svec, schk, 1e-8);         
      }
   }

   void testConvexPolyLineIntersect() {
      ConvexPolygon2d cpoly = new ConvexPolygon2d (
         new double[] { 0.0,0.0, 0.5,0.0, 1.0,1.0, 0.0,1.0 });

      Point2d p0 = new Point2d();
      Vector2d u = new Vector2d();

      double EPS = 1e-10;

      p0.set (0.5, -0.5);
      u.set (0.0, 1);
      testCpolyLineIntersect (cpoly, p0, u, 0.5, 1.5);

      u.set (1.0, 0.0);
      testCpolyLineIntersect (cpoly, p0, u);

      u.set (-0.5+EPS, 0.5);
      testCpolyLineIntersect (cpoly, p0, u, 1.0, 1.0);

      u.set (-0.5-EPS, 0.5);
      testCpolyLineIntersect (cpoly, p0, u);

      u.set (1.0, 1.0);
      testCpolyLineIntersect (cpoly, p0, u);

      u.set (-0.25, 0.5);
      testCpolyLineIntersect (cpoly, p0, u, 1.0, 2.0);

      p0.set (0.5, 0.5);
      u.set (1.0, 0);
      testCpolyLineIntersect (cpoly, p0, u, -0.5, 0.25);

      u.set (1.0, 1.0);
      testCpolyLineIntersect (cpoly, p0, u, -0.5, 0.5);
   }

   public void test() {
      RandomGenerator.setSeed (0x1234);
      //testSpecial();
      testSimpleConvexHull();
      testConvexHull();
      testNearestEdge();
      testComputeAreaIntegrals();
      testIntersectRay();
      testConvexPolyLineIntersect();
   }

   public static void main (String[] args) {
      Polygon2dTest tester = new Polygon2dTest();

      tester.runtest();
   }

}
