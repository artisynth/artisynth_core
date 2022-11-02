package maspack.geometry; 


import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class Polygon2dTest extends UnitTest {

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

   public void test() {
      RandomGenerator.setSeed (0x1234);
      //testSpecial();
      testSimpleConvexHull();
      testConvexHull();
      testNearestEdge();
   }

   public static void main (String[] args) {
      Polygon2dTest tester = new Polygon2dTest();

      tester.runtest();
   }

}
