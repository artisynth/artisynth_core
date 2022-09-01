package maspack.geometry; 

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class ConvexPolygon3dTest extends UnitTest {

   protected Point3d[] doubleToPoint3d (double[] coords) {
      int nump = coords.length/2;
      Point3d[] pnts = new Point3d[nump];
      for (int i=0; i<nump; i++) {
         pnts[i] = new Point3d (coords[2*i], coords[2*i+1], 0);
      }
      return pnts;
   }      

   private ArrayList<Point3d> addNoise (ArrayList<Point3d> points, double eps) {
      ArrayList<Point3d> newpoints = new ArrayList<>();
      for (Point3d p : points) {
         Point3d newp = new Point3d(p);
         newp.x += RandomGenerator.nextDouble (-eps, eps);
         newp.y += RandomGenerator.nextDouble (-eps, eps);
         newpoints.add (newp);
      }
      return newpoints;
   }

   private Polygon3d extractHull (ArrayList<Point3d> points, int[] hidxs) {
      ArrayList<Point3d> hpnts = new ArrayList<>();
      for (int idx : hidxs) {
         hpnts.add (new Point3d (points.get(idx)));
      }
      return new Polygon3d (hpnts);
   }

   protected void testConvexHull (double[] pcoords, int[] hidxs) {
      double tol = 1e-14;
      RotationMatrix3d RPW = new RotationMatrix3d();
      Vector3d nrml = new Vector3d();
      nrml.setRandom();
      nrml.normalize();
      RPW.setZDirection (nrml);         

      RigidTransform3d TPW = new RigidTransform3d();
      TPW.R.set (RPW);

      Point3d[] parray = doubleToPoint3d(pcoords);
      ArrayList<Point3d> pnts = new ArrayList<>();
      for (Point3d p : parray) {
         p.transform (RPW);
         pnts.add (p);
      }
      Polygon3d chk = extractHull (pnts, hidxs);
      ConvexPolygon3d hull = new ConvexPolygon3d();
      hull.computeAndSetHull (pnts, nrml, tol);
      if (!hull.isConvex(nrml, tol)) {
         hull.inverseTransform (TPW);
         System.out.println ("computed hull=\n" + hull.toString("%8.3f"));
         throw new TestException (
            "Computed hull is not convex");
      }
      if (!hull.equals (chk)) {
         hull.inverseTransform (TPW);
         System.out.println ("computed hull=\n" + hull.toString("%8.3f"));
         chk.inverseTransform (TPW);
         System.out.println ("expected hull=\n" + chk.toString("%8.3f"));
         throw new TestException ("Incorrect convexHull");
      }
      for (int k=0; k<pnts.size(); k++) {
         ArrayList<Point3d> newpnts = addNoise (pnts, tol);
         Collections.shuffle (newpnts, RandomGenerator.get());
         hull.computeAndSetHull (newpnts, nrml, 0);
         if (!hull.isConvex(nrml, tol)) {
            hull.inverseTransform (TPW);
            System.out.println ("computed hull=\n" + hull.toString("%8.3f"));
            throw new TestException (
               "Computed hull is not convex, shuffle "+k);
         }
      }
   }

   protected void testConvexHull (double[] pcoords, Vector3d nrml) {
      double tol = 1e-14;
      RotationMatrix3d RPW = new RotationMatrix3d();
      RPW.setZDirection (nrml);         
      RigidTransform3d TPW = new RigidTransform3d();
      TPW.R.set (RPW);

      Point3d[] parray = doubleToPoint3d(pcoords);
      ArrayList<Point3d> pnts = new ArrayList<>();
      for (Point3d p : parray) {
         p.transform (RPW);
         pnts.add (p);
      }
      ConvexPolygon3d hull = new ConvexPolygon3d();
      hull.computeAndSetHull (pnts, nrml, 0);
      if (!hull.isConvex(nrml, tol)) {
         hull.inverseTransform (TPW);
         System.out.println ("computed hull=\n" + hull.toString("%8.3f"));
         throw new TestException (
            "Computed hull is not convex");
      }
      hull.inverseTransform (TPW);
      System.out.println ("hull2d=\n");
      System.out.println (hull.toString ("%8.3f"));
      System.out.println ("nrml=" + nrml);
   }

   public void testConvexHull () {

      // convex hulls are specified in 2D and then transformed to 3d within the
      // test methods.

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
            0,0, 1,0, 2,0, 0,1, 1,1, 2,1, 0,2, 1,2, 2,2 },
         //   0    1    2    3    4    5    6    7    8 
         new int[] { 0, 2, 8, 6 });

      testConvexHull (
         new double[] {
            0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2, 2,2, 3,2, 
         //   0    1    2    3    4    5    6    7    8    9   10   11
            0,3, 1,3, 2,3, 3,3 },
         //  12   13   14   15
         new int[] { 0, 3, 15, 12 });
   }

   void testSpecial () {

      testConvexHull (
         new double[] {
            0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2, 2,2, 3,2, 
         //   0    1    2    3    4    5    6    7    8    9   10   11
            0,3, 1,3, 2,3, 3,3 },
         //  12   13   14   15
         new Vector3d (
            0.5483625875031178, 0.8360914699624516, 0.015796407278622436));
   }

   public void test() {
      RandomGenerator.setSeed (0x1234);
      //testSpecial();
      testConvexHull();
   }

   public static void main (String[] args) {
      ConvexPolygon3dTest tester = new ConvexPolygon3dTest();
      tester.runtest();
   }

}
