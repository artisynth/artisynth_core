package maspack.geometry;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

import maspack.geometry.TriangleIntersector.NearestFeature;

public class TriangleIntersectorTest extends UnitTest {

   void testClosestPoint() {
      TriangleIntersector tsect = new TriangleIntersector();
      ArrayList<Point3d[]> tris = new ArrayList<>();
      ArrayList<Point3d> pnts = new ArrayList<>();
      int testcnt = 100;
      createPointsAndTris (pnts, tris, testcnt);
      for (int i=0; i<testcnt; i++) {
         Point3d[] tri = tris.get(i);
         Point3d near = new Point3d();
         Vector2d uv = new Vector2d();

         Vector2d uvChk = new Vector2d();
         Point3d nearChk = new Point3d();
         for (int j=0; j<testcnt; j++) {
            Point3d p = pnts.get(j);
            tsect.nearestpoint (tri[0], tri[1], tri[2], p, near, uv);
            NearestFeature feat =
               tsect.closestPoint (nearChk, tri[0], tri[1], tri[2], p, uvChk);
            checkEquals ("nearest point", near, nearChk, 1e-13);
            checkEquals ("nearest uv", uv, uvChk, 1e-13);

            Face.nearestPointTriangle (
               nearChk, tri[0], tri[1], tri[2], p);
            checkEquals ("nearest point", near, nearChk, 1e-13);
         }
      }
   }

   void createPointsAndTris (
      ArrayList<Point3d> pnts, ArrayList<Point3d[]> tris, int num) {

      for (int i=0; i<num; i++) {
         Point3d pnt = new Point3d();
         pnt.setRandom();
         pnts.add (pnt);
         Point3d[] tri = new Point3d[3];
         tri[0] = new Point3d();
         tri[1] = new Point3d();
         tri[2] = new Point3d();
         tri[0].setRandom();
         tri[1].setRandom();
         tri[2].setRandom();
         tris.add (tri);
      }
   }

   void timeClosestPoint() {
      TriangleIntersector tsect = new TriangleIntersector();
      ArrayList<Point3d[]> tris = new ArrayList<>();
      ArrayList<Point3d> pnts = new ArrayList<>();
      int testcnt = 10000;
      createPointsAndTris (pnts, tris, testcnt);

      Point3d near = new Point3d();
      Vector2d uv = new Vector2d();

      int loopcnt = 10000;
      for (int i=0; i<loopcnt; i++) {
         for (int k=0; k<testcnt; k++) {
            Point3d[] tri = tris.get(k);
            Point3d p = pnts.get(k);
            tsect.nearestpoint (tri[0], tri[1], tri[2], p, near, uv);
            tsect.closestPoint (near, tri[0], tri[1], tri[2], p, uv);
         }
      }
      
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<loopcnt; i++) {
         for (int k=0; k<testcnt; k++) {
            Point3d[] tri = tris.get(k);
            Point3d p = pnts.get(k);
            tsect.nearestpoint (tri[0], tri[1], tri[2], p, near, uv);
         }
      }
      
      timer.stop();
      System.out.println ("nearestpoint: "+timer.result(testcnt*loopcnt));
      timer.start();
      for (int i=0; i<loopcnt; i++) {
         for (int k=0; k<testcnt; k++) {
            Point3d[] tri = tris.get(k);
            Point3d p = pnts.get(k);
            tsect.closestPoint (near, tri[0], tri[1], tri[2], p, uv);
         }
      }
      timer.stop();
      System.out.println ("closestPoint: "+timer.result(testcnt*loopcnt));

      timer.start();
      for (int i=0; i<loopcnt; i++) {
         for (int k=0; k<testcnt; k++) {
            Point3d[] tri = tris.get(k);
            Point3d p = pnts.get(k);
            Face.nearestPointTriangle (near, tri[0], tri[1], tri[2], p);
         }
      }
      timer.stop();
      System.out.println ("nearestPointTri: "+timer.result(testcnt*loopcnt));
   }

   public void test() {
      testClosestPoint();
   }

   public void timing() {
      timeClosestPoint();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      TriangleIntersectorTest tester = new TriangleIntersectorTest();

      boolean doTiming = false;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println (
               "Usage: java "+tester.getClass().getName()+" [-timing]");
            System.exit(1);
         }
      }      
      if (doTiming) {
         tester.timing();
      }
      else {
         tester.runtest();
      }
      
   }

}
