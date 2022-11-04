package maspack.interpolation;

import java.util.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.interpolation. CubicHermiteSpline3d.Knot;

public class CubicHermiteSpline3dTest extends UnitTest {

   void computeA2A3Check (Vector3d a2, Vector3d a3, Knot knot, Knot next) {
      double h = next.myS0-knot.myS0;
      a2.scale (-3/h, knot.myA0);
      a2.scaledAdd (-2, knot.myA1);
      a2.scaledAdd (3/h, next.myA0);
      a2.scaledAdd (-1, next.myA1);
      a2.scale (1/h);

      a3.scale (2/h, knot.myA0);
      a3.scaledAdd (1, knot.myA1);
      a3.scaledAdd (-2/h, next.myA0);
      a3.scaledAdd (1, next.myA1);
      a3.scale (1/(h*h));
   }

   Vector3d checkX (CubicHermiteSpline3d curve, double s) { 
      Vector3d xval = new Vector3d();
      Knot knot = curve.getPreceedingKnot (s);
      if (knot == null) {
         knot = curve.getFirstKnot();
         if (knot != null) {
            xval.scaledAdd (s-knot.myS0, knot.myA1, knot.myA0);
         }
      }
      else {
         int idx = knot.getIndex();
         if (idx+1 >= curve.numKnots()) {
            xval.scaledAdd (s-knot.myS0, knot.myA1, knot.myA0);
         }
         else {
            Knot next = curve.getKnot (idx+1);
            Vector3d a0 = knot.myA0;
            Vector3d a1 = knot.myA1;
            Vector3d a2 = new Vector3d();
            Vector3d a3 = new Vector3d();
            computeA2A3Check (a2, a3, knot, next);
            double ds = s-knot.myS0;
            xval.scaledAdd (ds, a3, a2);
            xval.scaledAdd (ds, xval, a1);
            xval.scaledAdd (ds, xval, a0);
         }  
      }
      return xval;
  }

   Vector3d checkDx (CubicHermiteSpline3d curve, double s) {
      Vector3d dxval = new Vector3d();
      Knot knot = curve.getPreceedingKnot (s);
      if (knot == null) {
         knot = curve.getFirstKnot();
         if (knot != null) {
            dxval.set (knot.myA1);
         }
      }
      else {
         int idx = knot.getIndex();
         if (idx+1 >= curve.numKnots()) {
            dxval.set (knot.myA1);
         }
         else {
            Knot next = curve.getKnot (idx+1);
            Vector3d a1 = knot.myA1;
            Vector3d a2 = new Vector3d();
            Vector3d a3 = new Vector3d();
            computeA2A3Check (a2, a3, knot, next);
            double ds = s-knot.myS0;
            dxval.scale (3*ds, a3);
            dxval.scaledAdd (2, a2);
            dxval.scaledAdd (ds, dxval, a1);
         }
      }
      return dxval;
   }

   private Vector3d vec3d (double val) {
      return new Vector3d (val, val, val);
   }

   /**
    * Create a list of Vector3d objects, where the x,y,z values
    * of each vector is the same.
    */
   ArrayList<Vector3d> createVec3dList (double... vals) {
      ArrayList<Vector3d> vlist = new ArrayList<>();
      for (double val : vals) {
         vlist.add (vec3d(val));
      }
      return vlist;
   }

   public void testRegular() {
      // create a known spline and check its behavior

      ArrayList<Vector3d> xVals = createVec3dList (-0.5, 0.5, 0.5, -0.5);
      ArrayList<Vector3d> dxdsVals = createVec3dList (-0.1, 2.0, 2.0, -0.1);

      CubicHermiteSpline3d curve = new CubicHermiteSpline3d();
      curve.set (
         new double[] {0, 1, 3, 6}, xVals, dxdsVals);

      double tol = 1e-14;

      // check x values
      checkEquals ("x at s=-1", curve.eval(-1), vec3d(-0.4), tol);
      checkEquals ("x at s=0", curve.eval(0), vec3d(-0.5), tol);
      checkEquals ("x at s=1", curve.eval(1), vec3d(0.5), tol);
      checkEquals ("x at s=2", curve.eval(2), vec3d(0.5), tol);
      checkEquals ("x at s=3", curve.eval(3), vec3d(0.5), tol);
      checkEquals ("x at s=6", curve.eval(6), vec3d(-0.5), tol);
      checkEquals ("x at s=8", curve.eval(7), vec3d(-0.6), tol);

      // check dxds values
      checkEquals ("dxds at s=-1", curve.evalDx(-1), vec3d(-0.1), tol);
      checkEquals ("dxds at s=0", curve.evalDx(0), vec3d(-0.1), tol);
      checkEquals ("dxds at s=1", curve.evalDx(1), vec3d(2.0), tol);
      checkEquals ("dxds at s=2", curve.evalDx(2), vec3d(-1.0), tol);
      checkEquals ("dxds at s=3", curve.evalDx(3), vec3d(2.0), tol);
      checkEquals ("dxds at s=6", curve.evalDx(6), vec3d(-0.1), tol);
      checkEquals ("dxds at s=8", curve.evalDx(7), vec3d(-0.1), tol);

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
            "x at s=" + s, curve.eval(s,lastIdx), checkX (curve, s), tol);
         checkEquals (
            "dxdx at s=" + s, curve.evalDx(s,lastIdx), checkDx (curve, s), tol);
      }

      // test copy
      CubicHermiteSpline3d check = new CubicHermiteSpline3d(curve);
      if (!curve.equals (check)) {
         throw new TestException ("copy failed");
      }

      // test save and load
      
      String str = ScanTest.writeToString (curve, "%g", null);
      check = new CubicHermiteSpline3d();
      ScanTest.scanFromString (check, null, str);
      if (!curve.equals (check)) {
         throw new TestException ("write/scan failed");
      }
   }

   void printSpline (String msg, CubicHermiteSpline3d spline) {
      System.out.println (msg + ScanTest.writeToString (spline, "%g", null));
   }

   public void testMultiSegment() {

      // start by with one segments special case

      double[] svals = new double[] { 1, 3 };
      ArrayList<Vector3d> pnts = createVec3dList (0, 1, -1, 0);

      CubicHermiteSpline3d spline = new CubicHermiteSpline3d();
      spline.setMultiSegment (pnts, svals);
      CubicHermiteSpline3d splineChk = new CubicHermiteSpline3d();
      splineChk.setSingleSegment (pnts, svals[0], svals[1]);

      if (!spline.epsilonEquals (splineChk, 1e-10)) {
         printSpline ("spline", spline);
         printSpline ("splineChk", splineChk);
         throw new TestException (
            "single segment and multisegment spline differ");
      }

      // now try to reconstruct known splines

      ArrayList<Vector3d> xVals = createVec3dList (-0.5, 0.5, 0.5, -0.5);
      ArrayList<Vector3d> dxdsVals = createVec3dList (-0.1, 2.0, 2.0, -0.1);

      CubicHermiteSpline3d curve = new CubicHermiteSpline3d();
      svals = new double [] { 1, 2, 4, 7 };
      curve.set (svals, xVals, dxdsVals);

      int npnts = 10;
      double slen = svals[svals.length-1]-svals[0];
      pnts.clear();
      for (int i=0; i<npnts; i++) {
         double s = svals[0] + i*slen/(double)(npnts-1);
         Vector3d p = curve.eval(s);
         pnts.add (p);
      }
      spline.setMultiSegment (pnts, svals);
      if (!spline.epsilonEquals (curve, 1e-10)) {
         printSpline ("spline", spline);
         printSpline ("curve", curve);
         throw new TestException (
            "multisegment call does not reproduce spline");
      }
   }

   double[] testPnts = new double[] {
      -5.790109701418277,-0.40413900140877795,-95.51778916625788,
      -5.858089408811325,-0.345048947303665,-91.69707759960757,
      -5.9415927212011175,0.0949108948745013,-87.87636603295725,
      -5.839714792847682,0.18813076215817495,-84.05565446630695,
      -6.383579166501213,0.24749426478917497,-80.23494289965663,
      -6.16766703944191,0.4730462781190479,-76.41423133300631,
      -6.2973418836963555,0.6219940258728125,-72.593519766356,
      -6.604916197274285,0.5167126408081895,-68.77280819970568,
      -6.2472021054611515,0.609420319102813,-64.95209663305536,
      -6.47163011890169,0.798302759744324,-61.13138506640505,
      -6.6029351807244145,0.6934582398076794,-57.310673499754735,
      -6.573298588753738,0.6673270109788324,-53.48996193310442,
      -6.66207383899891,0.629303948188886,-49.66925036645411,
      -6.939657048615982,0.5741021455101036,-45.8485387998038,
      -6.997350977704658,0.4918469487404932,-42.02782723315348,
      -7.096176091461781,0.6425063716787286,-38.207115666503164,
      -6.929307613533561,0.7341485243827719,-34.38640409985285,
      -6.928167795286697,0.682193556583469,-30.565692533202537,
      -6.365260314119892,0.6605996763614664,-26.74498096655222,
      -6.601661526307616,0.6197657066610254,-22.924269399901902,
      -6.433241482628366,0.5661940575241199,-19.103557833251585,
      -6.450666060869214,0.5116705625326233,-15.282846266601283,
      -6.406572225576416,0.8206225899320893,-11.462134699950951,
      -6.468405954977701,0.5747558707472873,-7.641423133300634,
      -6.492924040049117,0.7078101866411721,-3.8207115666503313,
      -6.468109086166808,0.7553121632892773,-2.8421709430404007E-14,
      -6.445745533820265,0.8648266861207469,3.8207115666502887,
      -6.348311957946551,0.8094017553720563,7.641423133300606,
      -6.452298765265033,0.7465117086580534,11.462134699950923,
      -6.545058027888685,0.766538790601578,15.28284626660124,
      -6.267775633305944,0.8500176758961573,19.103557833251557,
      -6.164437966749881,0.7998352185702743,22.924269399901874,
      -6.466385085025583,0.7847816681739548,26.74498096655219,
      -6.663239219833353,0.7994859801633627,30.56569253320251,
      -7.120168696772976,0.7804213363004474,34.38640409985281,
      -7.483186925745671,0.8119199505499535,38.20711566650313,
      -7.158510892634126,0.7195958583266775,42.027827233153445,
      -7.174506558875442,0.6657291047547556,45.84853879980376,
      -7.1207144934578475,0.8914748946109172,49.66925036645408,
      -7.126450596847796,0.9586270967781397,53.4899619331044,
      -6.6128913839386465,1.2852012604202023,57.310673499754714,
      -6.442159765474306,1.1888166689014625,61.131385066405,
      -5.93921410685743,1.2860817273691092,64.95209663305532,
      -5.943423619208598,1.4753848223160606,68.77280819970566,
      -5.714437390025073,1.279974421024228,72.59351976635598,
      -5.6087623371246265,1.3075382317128117,76.4142313330063,
      -5.269367472916887,1.4155061835409557,80.23494289965662,
      -5.270958948344881,1.5278915826916915,84.05565446630693,
      -5.239297740011321,1.439816127108665,87.87636603295722,
      -5.186404423068966,1.352848779606336,91.69707759960751,
      -4.717999560383231,1.4848843011226278,95.51778916625783
   };

   void testSpecial() {
      ArrayList<Point3d> xvals = new ArrayList<>();
      for (int i=0; i<testPnts.length; i+=3) {
         xvals.add (
            new Point3d(testPnts[i], testPnts[i+1], testPnts[i+2]));
      }
      CubicHermiteSpline3d spline0 = new CubicHermiteSpline3d();
      spline0.setSingleSegment (xvals, 0, 1);
      double zmin = xvals.get(0).z;
      double zmax = xvals.get(xvals.size()-1).z;
      CubicHermiteSpline3d spline1 = new CubicHermiteSpline3d();
      spline1.setSingleSegment (xvals, zmin, zmax);
      int ntests = 33;
      double maxErr = 0;
      double avgMag = 0;
      for (int i=0; i<=ntests; i++) {
         double s = i/(double)ntests;
         Vector3d eval0 = spline0.eval (s);
         Vector3d eval1 = spline1.eval (zmin + s*(zmax-zmin));
         avgMag += eval0.norm();
         double err = eval0.distance (eval1);
         if (err > maxErr) {
            maxErr = err;
         }
      }
      avgMag /= (double)(ntests+1);
      maxErr /= avgMag;
      if (maxErr > 1e-14) {
         throw new TestException (
            "Curve values change with different interval ranges");
      }
   }

   public void test() {
      testSpecial();
      testRegular();
      testMultiSegment();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      CubicHermiteSpline3dTest tester = new CubicHermiteSpline3dTest ();
      tester.runtest();
   }

}
