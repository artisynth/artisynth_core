package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Test class for GeometryUtils. 
 */
public class GeometryUtilsTest extends UnitTest {

   double EPS = 1e-14;

   void testFindPointAtDistance (
      ArrayList<Point3d> vtxs, boolean closed, double p0x, double p0z,
      double dist, double r0, double rchk) {

      double eps = 1e-7;

      Point3d pr = new Point3d();

      RigidTransform3d T = new RigidTransform3d();
      int ntests = 10;
      for (int i=0; i<ntests; i++) {
         T.setRandom();
         ArrayList<Point3d> xpnts = new ArrayList<>();
         for (Point3d p : vtxs) {
            Point3d xp = new Point3d(p);
            xp.transform (T);
            xpnts.add (xp);
         }
         Point3d p0 = new Point3d(p0x, 0, p0z);
         p0.transform (T);
         
         double r = GeometryUtils.findPointAtDistance (
            pr, xpnts, closed, p0, dist, r0);
         checkEquals ("findPointAtDistance result", r, rchk, eps);
         if (r != -1) {
            int ka = (int)r;
            double s = r-ka;
            Point3d pchk = new Point3d();
            if (s == 0) {
               pchk.set (xpnts.get(ka));
            }
            else {
               int kb = (ka+1)%xpnts.size();
               pchk.combine (1-s, xpnts.get(ka), s, xpnts.get(kb));
            }
            checkEquals ("findPointAtDistance pr", pr, pchk, eps);
         }
      }
   }

   void testNearestPoint (
      ArrayList<Point3d> vtxs, boolean closed, double p0x, double p0z,
      double r0, double rchk) {

      Point3d pr = new Point3d();

      RigidTransform3d T = new RigidTransform3d();
      int ntests = 10;
      for (int i=0; i<ntests; i++) {
         T.setRandom();
         ArrayList<Point3d> xpnts = new ArrayList<>();
         for (Point3d p : vtxs) {
            Point3d xp = new Point3d(p);
            xp.transform (T);
            xpnts.add (xp);
         }
         Point3d p0 = new Point3d(p0x, 0, p0z);
         p0.transform (T);
         
         double r = GeometryUtils.findNearestPoint (pr, xpnts, closed, p0, r0);
         checkEquals ("findNearestPoint result", r, rchk, EPS);
         int ka = (int)r;
         double s = r-ka;
         Point3d pchk = new Point3d();
         if (s > 0) {
            int kb = ka + 1;
            if (closed && kb == vtxs.size()) {
               kb = 0;
            }
            pchk.combine (1-s, xpnts.get(ka), s, xpnts.get(kb));
         }
         else {
            pchk.set (xpnts.get(ka));
         }
         checkEquals ("findNearestPoint pr", pr, pchk, EPS);
      }
   }

   private double sqrt (double x) {
      return Math.sqrt(x);
   }

   void testFindPointAtDistance() {
      ArrayList<Point3d> vtxs = new ArrayList<>();
      vtxs.add (new Point3d (1, 0, 0));
      vtxs.add (new Point3d (1, 0, 2));
      vtxs.add (new Point3d (1, 0, 3));
      vtxs.add (new Point3d (1, 0, 4));

      double x0 = -1;
      double z0 = 0;
      // test open polyline
      testFindPointAtDistance (vtxs, false, x0, z0, 2, 0,  0.0);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(5), 0, 0.5);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(4.25), 0, 0.25);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(5), 1, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, 10, 0, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, 0, 0, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, 2*sqrt(2), 0, 1.0);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(10.25), 0, 1.5);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(20), 0, 3.0);

      // test with non-zero starting point
      testFindPointAtDistance (vtxs, false, x0, z0, 2, 0.1,  -1);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(5), 0.4, 0.5);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(5), 0.5, 0.5);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(5), 0.51, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(4.25), 0.1, 0.25);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(4.25), 0.25, 0.25);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(4.25), 0.3, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, 10, 2.0, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, 0, 2.0, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(10.25), 1.4, 1.5);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(10.25), 1.5, 1.5);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(10.25), 1.6, -1);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(20), 2.9, 3.0);
      testFindPointAtDistance (vtxs, false, x0, z0, sqrt(20), 3.0, 3.0);
      
      // test with a closed loop
      vtxs.clear();
      vtxs.add (new Point3d (-1, 0, 0));
      vtxs.add (new Point3d ( 1, 0, 0));
      vtxs.add (new Point3d ( 0, 0, 1));
      testFindPointAtDistance (vtxs, true, /*x0*/0, /*z0*/-1, 1, 0, 0.5);
      testFindPointAtDistance (vtxs, true, /*x0*/0, /*z0*/-1, sqrt(2), 0, 0.0);
      testFindPointAtDistance (vtxs, true, /*x0*/2, /*z0*/-1, sqrt(2), 0, 1.0);
      testFindPointAtDistance (vtxs, true, /*x0*/0, /*z0*/-1, 3, 0, -1);
      testFindPointAtDistance (vtxs, true, /*x0*/1, /*z0*/0, 0.6, 0, 0.7);
      testFindPointAtDistance (vtxs, true, /*x0*/1, /*z0*/1, sqrt(2)/2, 0, 1.5);
      testFindPointAtDistance (vtxs, true, /*x0*/0.5, /*z0*/1, 0.5, 0, 1.5);
      testFindPointAtDistance (vtxs, true, /*x0*/0.5, /*z0*/1.5, sqrt(2)/2, 0, 2.0);
      testFindPointAtDistance (vtxs, true, /*x0*/0, /*z0*/1.5, 0.5, 0, 2.0);
      testFindPointAtDistance (vtxs, true, /*x0*/-0.5, /*z0*/1.5, sqrt(2)/2, 0, 2.0);
      testFindPointAtDistance (vtxs, true, /*x0*/-1, /*z0*/1, sqrt(2)/2, 0, 2.5);
   }

   void testNearestPoint() {
      ArrayList<Point3d> vtxs = new ArrayList<>();
      vtxs.add (new Point3d (1, 0, 0));
      vtxs.add (new Point3d (1, 0, 2));
      vtxs.add (new Point3d (1, 0, 3));
      vtxs.add (new Point3d (1, 0, 4));

      // test open polyline
      double x0 = -1;
      testNearestPoint (vtxs, false, x0, /*z0*/0,     0, 0.0);
      testNearestPoint (vtxs, false, x0, /*z0*/-1,    0, 0.0);
      testNearestPoint (vtxs, false, x0, /*z0*/1,     0, 0.5);
      testNearestPoint (vtxs, false, x0, /*z0*/2,     0, 1.0);
      testNearestPoint (vtxs, false, x0, /*z0*/2.33,  0, 1.33);
      testNearestPoint (vtxs, false, x0, /*z0*/3.25,  0, 2.25);
      testNearestPoint (vtxs, false, x0, /*z0*/4,     0, 3.00);
      testNearestPoint (vtxs, false, x0, /*z0*/4.5,   0, 3.00);

      // test with non-zero starting point
      testNearestPoint (vtxs, false, x0, /*z0*/0,     0.33, 0.33);
      testNearestPoint (vtxs, false, x0, /*z0*/-1,    0.33, 0.33);
      testNearestPoint (vtxs, false, x0, /*z0*/0.66,  0.33, 0.33);
      testNearestPoint (vtxs, false, x0, /*z0*/0.68,  0.33, 0.34);
      testNearestPoint (vtxs, false, x0, /*z0*/2.5,   2.0,  2.0);
      testNearestPoint (vtxs, false, x0, /*z0*/3.0,   2.0,  2.0);
      testNearestPoint (vtxs, false, x0, /*z0*/3.1,   2.0,  2.1);

      // test with a closed loop
      vtxs.clear();
      vtxs.add (new Point3d (-1, 0, 0));
      vtxs.add (new Point3d ( 1, 0, 0));
      vtxs.add (new Point3d ( 0, 0, 1));
      testNearestPoint (vtxs, true, /*x0*/-1,  /*z0*/-0.5,  0, 0.0);
      testNearestPoint (vtxs, true, /*x0*/0,   /*z0*/-0.5,  0, 0.5);
      testNearestPoint (vtxs, true, /*x0*/1,   /*z0*/-0.5,  0, 1.0);
      testNearestPoint (vtxs, true, /*x0*/1.5, /*z0*/-0.5,  0, 1.0);
      testNearestPoint (vtxs, true, /*x0*/1,   /*z0*/ 0.5,  0, 1.25);
      testNearestPoint (vtxs, true, /*x0*/0.5, /*z0*/ 1.5,  0, 2.0);
      testNearestPoint (vtxs, true, /*x0*/0,   /*z0*/ 1.2,  0, 2.0);
      testNearestPoint (vtxs, true, /*x0*/-0.5,/*z0*/ 1.5,  0, 2.0);
      testNearestPoint (vtxs, true, /*x0*/-1,  /*z0*/ 0.5,  0, 2.75);
      testNearestPoint (vtxs, true, /*x0*/-1.5,/*z0*/ 0.5,  0, 0.00);
      testNearestPoint (vtxs, true, /*x0*/-1.5,/*z0*/ 0.0,  0, 0.00);
   }

   public void test() {
      testFindPointAtDistance();
      testNearestPoint();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      GeometryUtilsTest tester = new GeometryUtilsTest();
      tester.runtest();
   }

}
