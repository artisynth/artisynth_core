package maspack.interpolation;

import java.util.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.interpolation.LinearSpline3d.Knot;

public class LinearSpline3dTest extends UnitTest {

   Vector3d computeDx (Knot knot, LinearSpline3d curve) {
      Vector3d dxds = new Vector3d();
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
         dxds.sub (next.getA0(), knot.getA0());
         dxds.scale (1/(next.getS0()-knot.getS0()));
      }
      return dxds;
   }

   Vector3d checkX (LinearSpline3d curve, double s) { 
      Vector3d xval = new Vector3d();
      Knot knot = curve.getPreceedingKnot (s);
      if (knot == null) {
         knot = curve.getFirstKnot();
         if (knot != null) {
            Vector3d dxds = computeDx (knot, curve);
            xval.scaledAdd (s-knot.myS0, dxds, knot.myA0);
         }
      }
      else {
         Vector3d dxds = computeDx (knot, curve);
         xval.scaledAdd (s-knot.myS0, dxds, knot.myA0);
      }
      return xval;
   }
   
   Vector3d checkDx (LinearSpline3d curve, double s) {
      Vector3d dxval = new Vector3d();
      Knot knot = curve.getPreceedingKnot (s);
      if (knot == null) {
         knot = curve.getFirstKnot();
         if (knot != null) {
            Vector3d dxds = computeDx (knot, curve);
            dxval.set (dxds);
         }
      }
      else {
         dxval.set (computeDx (knot, curve));
      }
      return dxval;
   }

   private Vector3d vec3d (double val) {
      return new Vector3d (val, val, val);
   }

   public void testRegular() {
      // create known splines and check their behavior
      LinearSpline3d curve = new LinearSpline3d();

      double tol = 1e-14;

      // check with no knots
      checkEquals ("x at s=2", curve.eval(2), vec3d(0), tol);
      checkEquals ("dxds at s=2", curve.evalDx(2), vec3d(0), tol);

      // check with one knot
      ArrayList<Vector3d> xVals = new ArrayList<>();
      xVals.add (vec3d(-0.5));

      curve.set (new double[] {1}, xVals);

      checkEquals ("x at s=0", curve.eval(0), vec3d(-0.5), tol);
      checkEquals ("x at s=1", curve.eval(1), vec3d(-0.5), tol);
      checkEquals ("x at s=2", curve.eval(2), vec3d(-0.5), tol);

      checkEquals ("dxds at s=0", curve.evalDx(0), vec3d(0), tol);
      checkEquals ("dxds at s=1", curve.evalDx(1), vec3d(0), tol);
      checkEquals ("dxds at s=2", curve.evalDx(2), vec3d(0), tol);

      // check with four knots
      xVals.add (vec3d(0.5));
      xVals.add (vec3d(0.0));
      xVals.add (vec3d(4.0));
      curve.set (new double[] {0, 1, 3, 6}, xVals);

      // check x values
      checkEquals ("x at s=-1", curve.eval(-1), vec3d(-1.5), tol);
      checkEquals ("x at s=0", curve.eval(0), vec3d(-0.5), tol);
      checkEquals ("x at s=1", curve.eval(1), vec3d(0.5), tol);
      checkEquals ("x at s=2", curve.eval(2), vec3d(0.25), tol);
      checkEquals ("x at s=3", curve.eval(3), vec3d(0.0), tol);
      checkEquals ("x at s=6", curve.eval(6), vec3d(4.0), tol);
      checkEquals ("x at s=8", curve.eval(8), vec3d(20/3.0), tol);

      // check dxds values
      checkEquals ("dxds at s=-1", curve.evalDx(-1), vec3d(1), tol);
      checkEquals ("dxds at s=0", curve.evalDx(0), vec3d(1), tol);
      checkEquals ("dxds at s=1", curve.evalDx(1), vec3d(-0.25), tol);
      checkEquals ("dxds at s=2", curve.evalDx(2), vec3d(-0.25), tol);
      checkEquals ("dxds at s=3", curve.evalDx(3), vec3d(4.0/3), tol);
      checkEquals ("dxds at s=6", curve.evalDx(6), vec3d(4.0/3), tol);
      checkEquals ("dxds at s=8", curve.evalDx(7), vec3d(4.0/3), tol);

      // general value check
      int npts = 200;
      IntHolder lastIdx = new IntHolder();
      for (int i=0; i<=npts; i++) {
         double s = -0.5 + (i*7.0/npts);
         checkEquals (
            "x at s=" + s, curve.eval(s), checkX (curve, s), tol);
         checkEquals (
            "dxdx at s=" + s, curve.evalDx(s), checkDx (curve, s), tol);
         checkEquals (
            "x at s=" + s, curve.eval(s, lastIdx), checkX (curve, s), tol);
         checkEquals (
            "dxdx at s=" + s, curve.evalDx(s, lastIdx), checkDx (curve, s), tol);
      }

      // test copy
      LinearSpline3d check = new LinearSpline3d(curve);
      if (!curve.equals (check)) {
         throw new TestException ("copy failed");
      }

      // test save and load
      
      String str = ScanTest.writeToString (curve, "%g", null);
      check = new LinearSpline3d();
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
      ArrayList<Point3d> points = new ArrayList<>();
      double[] svals = new double[numk];
      for (int i=0; i<numk; i++) {
         Point3d pnt = new Point3d();
         pnt.setRandom();
         points.add (pnt);
         svals[i] = i;
      }
      LinearSpline3d curve = new LinearSpline3d (svals, points);

      FunctionTimer timer = new FunctionTimer();
      int cnt = 1000;
      double nsamps = 1234;
      IntHolder lastIdx = new IntHolder();

      // warmup
      for (int i=0; i<cnt; i++) {
         for (int k=0; k<=nsamps; k++) {
            double s = k*numk/(double)nsamps;
            curve.eval (s);
            curve.evalDx (s);
            curve.eval (s, lastIdx);
            curve.evalDx (s, lastIdx);
         }
      }

      // time without lastIdx
      System.out.println ("Testing with "+numk+" knots:");
      timer.start();
      for (int i=0; i<cnt; i++) {
         for (int k=0; k<=nsamps; k++) {
            double s = k*numk/(double)nsamps;
            curve.eval (s);
            curve.evalDx (s);
         }
      }
      timer.stop();
      System.out.println (" without lastIdx: " + timer.resultUsec(cnt));

      // time without lastIdx
      timer.start();
      for (int i=0; i<cnt; i++) {
         for (int k=0; k<=nsamps; k++) {
            double s = k*numk/(double)nsamps;
            curve.eval (s, lastIdx);
            curve.evalDx (s, lastIdx);
         }
      }
      timer.stop();
      System.out.println (" with lastIdx:    "+timer.resultUsec(cnt));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      LinearSpline3dTest tester = new LinearSpline3dTest ();

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
