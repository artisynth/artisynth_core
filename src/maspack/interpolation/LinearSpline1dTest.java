package maspack.interpolation;

import java.util.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.interpolation.LinearSpline1d.Knot;

public class LinearSpline1dTest extends UnitTest {

   double computeDy (Knot knot, LinearSpline1d curve) {
      int numk = curve.numKnots();
      if (numk > 1) {
         int idx = knot.getIndex();         
         Knot next;
         if (idx < numk-1) {
            next = curve.getKnot (idx+1);
         }
         else {
            knot = curve.getKnot (idx-1);
            next = curve.getKnot (idx);
         }
         return (next.getY0()-knot.getY0())/(next.getX0()-knot.getX0());
      }
      else {
         return 0;
      }
   }

   double checkY (LinearSpline1d curve, double x) { 
      Knot knot = curve.getPreceedingKnot (x);
      if (knot == null) {
         knot = curve.getFirstKnot();
         if (knot != null) {
            double dydx = computeDy (knot, curve);
            return (x-knot.myX0)*dydx + knot.myY0;
         }
         else {
            return 0;
         }
      }
      else {
         double dydx = computeDy (knot, curve);
         return (x-knot.myX0)*dydx + knot.myY0;
      }
   }
   
   double checkDy (LinearSpline1d curve, double x) {
      Knot knot = curve.getPreceedingKnot (x);
      if (knot == null) {
         knot = curve.getFirstKnot();
         if (knot != null) {
            return computeDy (knot, curve);
         }
         else {
            return 0;
         }
      }
      else {
         return computeDy (knot, curve);
      }
   }

   void testWriteAndScan (LinearSpline1d spline) {
      LinearSpline1d newspline =
         (LinearSpline1d)ScanTest.testWriteAndScanWithClass (
            spline, null, "%g");
      if (!spline.equals (newspline)) {
         throw new TestException (
            "written-scanned spline not equal to original");
      }
   }

   public void testRegular() {
      // create known splines and check their behavior
      LinearSpline1d curve = new LinearSpline1d();

      double tol = 1e-14;

      testWriteAndScan (curve);      

      // check with no knots
      checkEquals ("y at s=2", curve.eval(2), 0.0, tol);
      checkEquals ("dydx at s=2", curve.evalDy(2), 0.0, tol);

      // check with one knot
      DynamicDoubleArray yVals = new DynamicDoubleArray();
      yVals.add (-0.5);

      curve.set (new double[] {1}, yVals.getArray());
      testWriteAndScan (curve);      

      checkEquals ("y at s=0", curve.eval(0), -0.5, tol);
      checkEquals ("y at s=1", curve.eval(1), -0.5, tol);
      checkEquals ("y at s=2", curve.eval(2), -0.5, tol);

      checkEquals ("dydx at s=0", curve.evalDy(0), 0.0, tol);
      checkEquals ("dydx at s=1", curve.evalDy(1), 0.0, tol);
      checkEquals ("dydx at s=2", curve.evalDy(2), 0.0, tol);

      // check with four knots
      yVals.add (0.5);
      yVals.add (0.0);
      yVals.add (4.0);
      curve.set (new double[] {0, 1, 3, 6}, yVals.getArray());
      testWriteAndScan (curve);      

      // check x values
      checkEquals ("y at s=-1", curve.eval(-1), -1.5, tol);
      checkEquals ("y at s=0", curve.eval(0), -0.5, tol);
      checkEquals ("y at s=1", curve.eval(1), 0.5, tol);
      checkEquals ("y at s=2", curve.eval(2), 0.25, tol);
      checkEquals ("y at s=3", curve.eval(3), 0.0, tol);
      checkEquals ("y at s=6", curve.eval(6), 4.0, tol);
      checkEquals ("y at s=8", curve.eval(8), 20/3.0, tol);

      // check dydx values
      checkEquals ("dydx at s=-1", curve.evalDy(-1), 1.0, tol);
      checkEquals ("dydx at s=0", curve.evalDy(0), 1.0, tol);
      checkEquals ("dydx at s=1", curve.evalDy(1), -0.25, tol);
      checkEquals ("dydx at s=2", curve.evalDy(2), -0.25, tol);
      checkEquals ("dydx at s=3", curve.evalDy(3), 4.0/3, tol);
      checkEquals ("dydx at s=6", curve.evalDy(6), 4.0/3, tol);
      checkEquals ("dydx at s=8", curve.evalDy(7), 4.0/3, tol);

      // general value check
      int npts = 200;
      IntHolder lastIdx = new IntHolder();
      for (int i=0; i<=npts; i++) {
         double x = -0.5 + (i*7.0/npts);
         checkEquals (
            "y at x=" + x, curve.eval(x), checkY (curve, x), tol);
         checkEquals (
            "dydx at x=" + x, curve.evalDy(x), checkDy (curve, x), tol);
         checkEquals (
            "y at x=" + x, curve.eval(x, lastIdx), checkY (curve, x), tol);
         checkEquals (
            "dydx at x=" + x, curve.evalDy(x, lastIdx), checkDy (curve, x), tol);
      }

      // test copy
      LinearSpline1d check = new LinearSpline1d(curve);
      if (!curve.equals (check)) {
         throw new TestException ("copy failed");
      }

      // test save and load
      
      String str = ScanTest.writeToString (curve, "%g", null);
      check = new LinearSpline1d();
      ScanTest.scanFromString (check, null, str);
      if (!curve.equals (check)) {
         throw new TestException ("write/scan failed");
      }
   }

   public void test() {
      testRegular();
   }

   public void timing() {
      // examine the timing difference between eval(s) and eval(s,lastIdx)
      // NOTE: break even seems to be at around 10-15 knots

      // speed up is about 27 for 1000 knots
      // speed up is about 254 for 10000 knots

      int numk = 10000;
      double[] yvals = new double[numk];
      double[] xvals = new double[numk];
      for (int i=0; i<numk; i++) {
         yvals[i] = RandomGenerator.nextDouble();
         xvals[i] = i;
      }
      LinearSpline1d curve = new LinearSpline1d (xvals, yvals);

      FunctionTimer timer = new FunctionTimer();
      int cnt = 1000;
      double nsamps = 1234;
      IntHolder lastIdx = new IntHolder();

      // warmup
      for (int i=0; i<cnt; i++) {
         for (int k=0; k<=nsamps; k++) {
            double x = k*numk/(double)nsamps;
            curve.eval (x);
            curve.evalDy (x);
            curve.eval (x, lastIdx);
            curve.evalDy (x, lastIdx);
         }
      }

      // time without lastIdx
      System.out.println ("Testing with "+numk+" knots:");
      timer.start();
      for (int i=0; i<cnt; i++) {
         for (int k=0; k<=nsamps; k++) {
            double x = k*numk/(double)nsamps;
            curve.eval (x);
            curve.evalDy (x);
         }
      }
      timer.stop();
      System.out.println (" without lastIdx: " + timer.resultUsec(cnt));

      // time without lastIdx
      timer.start();
      for (int i=0; i<cnt; i++) {
         for (int k=0; k<=nsamps; k++) {
            double x = k*numk/(double)nsamps;
            curve.eval (x, lastIdx);
            curve.evalDy (x, lastIdx);
         }
      }
      timer.stop();
      System.out.println (" with lastIdx:    "+timer.resultUsec(cnt));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      LinearSpline1dTest tester = new LinearSpline1dTest ();

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
