package maspack.geometry; 

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

public class PolygonIntersectorTest extends UnitTest {

   private double DOUBLE_PREC = 1e-16;
   private double EPS = 1000*DOUBLE_PREC;

   void testIntersection (
      double[] pcoords0, double[] pcoords1, double[]... rcoords) {
      Polygon2d poly0 = new Polygon2d (pcoords0);
      Polygon2d poly1 = new Polygon2d (pcoords1);
      ArrayList<Polygon2d> chk = new ArrayList<>();
      for (int i=0; i<rcoords.length; i++) {
         chk.add (new Polygon2d (rcoords[i]));
      }

      // 2D test
      Polygon2d[] res = PolygonIntersector.intersect (poly0, poly1);
      if (res.length != chk.size()) {
         checkEquals ("num 2d intersection polygons", res.length, chk.size());
      }
      for (int i=0; i<res.length; i++) {
         Polygon2d found = null;
         for (int j=0; j<chk.size(); j++) {
            if (chk.get(j).epsilonEquals (res[i], EPS)) {
               found = chk.get(j);
               break;
            }
         }
         if (found == null) {
            throw new TestException (
               "result polygon "+i+" not found in expected results");
         }
         else {
            chk.remove (found);
         }
      }

      // 3D test
      RigidTransform3d TPW = new RigidTransform3d();
      TPW.setRandom();

      Polygon3d poly3_0 = new Polygon3d (poly0, TPW);
      Polygon3d poly3_1 = new Polygon3d (poly1, TPW);
      ArrayList<Polygon3d> chk3 = new ArrayList<>();
      for (int i=0; i<rcoords.length; i++) {
         chk3.add (new Polygon3d (new Polygon2d (rcoords[i]), TPW));
      }
      Polygon3d[] res3 = PolygonIntersector.intersect (poly3_0, poly3_1, TPW);
      if (res3.length != chk3.size()) {
         checkEquals ("num 3d intersection polygons", res3.length, chk3.size());
      }
      for (int i=0; i<res3.length; i++) {
         Polygon3d found = null;
         for (int j=0; j<chk3.size(); j++) {
            if (chk3.get(j).epsilonEquals (res3[i], EPS)) {
               found = chk3.get(j);
               break;
            }
         }
         if (found == null) {
            throw new TestException (
               "result 3d polygon"+i+" not found in expected results");
         }
         else {
            chk3.remove (found);
         }
      }
      
   }

   void testIntersection() {
      testIntersection (
         new double[] { 0,0, 2,0, 2,1, 0,1 },
         new double[] { 1,-1, 3,-1, 3,2, 1,2 },
         new double[] { 1,0, 2,0, 2,1, 1,1 });

      double E = 1e-8;
      double X = 1+E;
      double Y = 3+E;

      testIntersection (
         new double[] { 0,0, 3,0, 3,1, 1,1, 1,2, 3,2, 3,3, 0,3 },
         new double[] { X,-E, 2,-E, 2,Y, X,Y }, 
         new double[] { 2,3, X,3, X,2, 2,2 }, 
         new double[] { 2,1, X,1, X,0, 2,0 });
   }

   public void test() {
      RandomGenerator.setSeed (0x1234);
      testIntersection();
   }

   public static void main (String[] args) {
      PolygonIntersectorTest tester = new PolygonIntersectorTest();

      tester.runtest();
   }

}
