package maspack.geometry;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;

/**
 * Performs some basic query and speed test for KDTree3d.
 */
public class KDTree3dTest extends UnitTest {

   ArrayList<Point3d> randomPoints (int num) {
      ArrayList<Point3d> pnts = new ArrayList<>();
      for (int i=0; i<num; i++) {
         Point3d p = new Point3d();
         p.setRandom();
         pnts.add (p);
      }
      return pnts;
   }

   protected void testSingleQuery (Point3d pnt, ArrayList<Point3d> pnts) {
      Point3d nearest;
      double usecDirect = 0;
      double usecTree = 0;

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      nearest = getNearest (pnt, pnts);
      timer.stop();
      usecDirect = timer.getTimeUsec();

      timer.start();
      KDTree3d tree = new KDTree3d (pnts);
      nearest = tree.nearestNeighbourSearch (pnt, 1e-16);
      timer.stop();
      usecTree = timer.getTimeUsec();
      System.out.println (" n=" + pnts.size()+ " " +usecDirect+" "+usecTree);
   }

   protected void testMultipleQuery (
      ArrayList<Point3d> pnts0, ArrayList<Point3d> pnts1) {

      ArrayList<Point3d> nearest;
      double usecDirect = 0;
      double usecTree = 0;

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      nearest = getNearest (pnts0, pnts1);
      timer.stop();
      usecDirect = timer.getTimeUsec();

      timer.start();
      KDTree3d tree = new KDTree3d (pnts1);
      nearest = new ArrayList<>();
      for (Point3d pnt : pnts0) {
         nearest.add (tree.nearestNeighbourSearch (pnt, 1e-16));
      }
      timer.stop();
      usecTree = timer.getTimeUsec();
      System.out.printf (
         " n=%d, %g %g, ratio=%g\n", pnts1.size(),
         usecDirect, usecTree, usecTree/usecDirect);

   }

   protected void speedtest() {
      int maxexp = 14;
      // warmup
      for (int k=0; k<100; k++) {
         int n = 1000;
         Point3d pnt = new Point3d();
         pnt.setRandom();
         ArrayList<Point3d> pnts = randomPoints (n);
         KDTree3d tree = new KDTree3d (pnts);
         getNearest (pnts, pnts);
         tree.nearestNeighbourSearch (pnt, 1e-16);
      }

      System.out.println ("Multiple queries, direct vs. tree (usec): ");
      for (int k=1; k<=maxexp; k++) {
         int n = (1 << k);
         ArrayList<Point3d> pnts0 = randomPoints (n);
         ArrayList<Point3d> pnts1 = randomPoints (n);
         testMultipleQuery (pnts0, pnts1);
      }
   }

   protected Point3d getNearest (Point3d pnt, ArrayList<Point3d> pnts) {
      Point3d nearest = pnts.get(0);
      double mindist = nearest.distance (pnt);
      for (int i=1; i<pnts.size(); i++) {
         Point3d p = pnts.get(i);
         double d = p.distance (pnt);
         if (d < mindist) {
            mindist = d;
            nearest = p;
         }
      }
      return nearest;
   }

   protected ArrayList<Point3d> getNearest (
      ArrayList<Point3d> pnts0, ArrayList<Point3d> pnts1) {
      ArrayList<Point3d> nearest = new ArrayList<>();
      for (Point3d pnt : pnts0) {
         nearest.add (getNearest (pnt, pnts1));
      }
      return nearest;
   }

   public void test() {
      ArrayList<Point3d> pnts = randomPoints (100);
      KDTree3d tree = new KDTree3d (pnts);
      int ntests = 100;
      for (int i=0; i<ntests; i++) {
         Point3d pnt = new Point3d();
         pnt.setRandom();
         Point3d nearest = tree.nearestNeighbourSearch (pnt, 1e-16);
         Point3d nearChk = getNearest (pnt, pnts);
         if (!nearest.equals (nearChk)) {
            throw new TestException (
               "Nearest point is "+nearest+"; expecting "+nearChk);
         }
      }
   }

   public static void main (String[] args) {
      KDTree3dTest tester = new KDTree3dTest();

      RandomGenerator.setSeed (0x1234);
      boolean speedTest = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-speed")) {
            speedTest = true;
         }
         else {
            System.out.println (
               "Usage: java maspack.geometry.KDTree3dTest [-speed]");
            System.exit(1); 
         }
      }
      if (speedTest) {
         tester.speedtest();
      }
      else {
         tester.runtest();
      }
   }
}
