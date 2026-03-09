package maspack.matrix;

import maspack.util.*;
import maspack.matrix.*;

public class LineTest extends UnitTest {

   public double[] computeNearestPointParams (Line line, Point3d p0, Point3d p1) {
      double s, t;      

      Vector3d u = line.getDirection();
      Vector3d v = new Vector3d();
      v.sub (p1, p0);
      double len = v.norm();

      Vector3d w = new Vector3d();
      w.sub (line.getOrigin(), p0);

      if (len == 0) {
         s = -w.dot(u);
         t = 0;
      }
      else {
         double a = u.dot(u);
         double b = u.dot(v);
         double c = v.dot(v);
         double d = u.dot(w);
         double e = v.dot(w);

         double denom = a*c-b*b;
         if (Math.abs(denom) < 1e-15*b*b) {
            s = -w.dot(u);
            t = 0;
         }
         else {
            s = (b*e-c*d)/denom;
            t = (a*e-b*d)/denom;
         }
      }
      return new double[] {s, t};      
   }

   public void testDistanceToSegment (Line line, Point3d p0, Point3d p1) {
      double EPS = 1e-14;
      double[] params = computeNearestPointParams (line, p0, p1);
      Point3d pline = new Point3d();
      Point3d nearChk = new Point3d();
      double distChk;
      pline.scaledAdd (params[0], line.getDirection(), line.getOrigin());
      if (params[1] < 0) {
         nearChk.set (p0);
         distChk = line.distance (nearChk);
         params[1] = 0.0;
      }
      else if (params[1] > 1) {
         nearChk.set (p1);
         distChk = line.distance (nearChk);
         params[1] = 1.0;
      }
      else {
         nearChk.combine (1-params[1], p0, params[1], p1);
         distChk = nearChk.distance (pline);
      }
      Point3d near = new Point3d();
      double dist = line.distanceToSegment (near, p0, p1);
      DoubleHolder segParam = new DoubleHolder();

      checkEquals ("distanceToSegment distance", dist, distChk, EPS);
      checkEquals ("distanceToSegment near", near, nearChk, EPS);

      dist = line.distanceToSegment (segParam, p0, p1);
      checkEquals ("distanceToSegment distance", dist, distChk, EPS);
      checkEquals ("distanceToSegment segParam", segParam.value, params[1], EPS);

   }

   public void testDistanceToSegment() {
      Line line = new Line();
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();

      int ntests = 100;
      for (int i=0; i<ntests; i++) {
         line.setRandom();
         p0.setRandom();
         p1.setRandom();
         testDistanceToSegment (line, p0, p1);
      }
      // parallel case
      for (int i=0; i<ntests; i++) {
         line.setRandom();
         p0.setRandom();
         p1.scaledAdd (1.0, line.getDirection(), p0);
         testDistanceToSegment (line, p0, p1);
      }
      // point case
      for (int i=0; i<ntests; i++) {
         line.setRandom();
         p0.setRandom();
         p1.set (p0);
         testDistanceToSegment (line, p0, p1);
      }

   }

   public void test() {
      testDistanceToSegment();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      LineTest tester = new LineTest();
      tester.runtest();
   }

}
