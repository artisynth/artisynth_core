package maspack.interpolation;

import maspack.util.*;
import maspack.matrix.*;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

public class CubicHermiteSpline1dTest extends UnitTest {

   double EPS = 1e-12;

   public static double numericDerivativeError (CubicHermiteSpline1d curve) {

      int nx = 1;
      double xmin = curve.getKnot(0).x0;
      double xmax = xmin;
      double xinc = 0;
      if (curve.numKnots() > 1) {
         xmax = curve.getKnot(curve.numKnots()-1).x0;
         nx = 1000;
         xinc = (xmax-xmin)/(nx-1);
      }
      double dyMag = 0;
      // first find an overall magnitude for dy
      for (int i=0; i<nx; i++) {
         double x = xmin + i*xinc;
         double dy = curve.evalDy(x);
         if (Math.abs(dy) > dyMag) {
            dyMag = Math.abs(dy);
         }
      }
      // now check the derivative numerically using this magnitude
      double h = 1e-8*(xmax-xmin);
      double maxErr = 0;
      for (int i=0; i<nx; i++) {
         double x = xmin + i*xinc;
         double dy = curve.evalDy(x);
         double dyNum = (curve.evalY(x+h)-curve.evalY(x))/h;
         double err = Math.abs(dy-dyNum)/dyMag;
         if (err > maxErr) {
            maxErr = err;
         }
      }
      return maxErr;
   }

   void testInverse() {
      CubicHermiteSpline1d curve = new CubicHermiteSpline1d();
      check ("empty curve is not invertible", curve.isInvertible() == false);

      curve.addKnot (1.2, 4.3, 0);
      check ("curve should not be invertible", curve.isInvertible() == false);

      curve.clearKnots();
      check ("empty curve is not invertible", curve.isInvertible() == false);

      curve.addKnot (1.2, 4.3, 2.3);
      check ("curve should be invertible", curve.isInvertible() == true);

      double xchk = 0.9;
      double y = curve.evalY(xchk);
      double x = curve.solveX (y);
      checkEquals ("single knot inverse", x, xchk, 1e-10);

      curve.clearKnots();

      curve.addKnot (-1, 0, 0.001);
      curve.addKnot (-0.1, 0.5, -0.9);
      curve.addKnot ( 0.5,-0.5, -0.001);
      check ("curve should not be invertible", curve.isInvertible() == false);

      curve.clearKnots();
      curve.addKnot (-1, -.65, 0.000); // 0 slope not allowed
      curve.addKnot (0.01, 0.1, -0.1);
      curve.addKnot ( 0.7, 0.75, 0.9);
      check ("curve should not be invertible", curve.isInvertible() == false);

      curve.clearKnots();
      curve.addKnot (-0.8, -.65, 0.001);
      curve.addKnot (0.01, 0.1, 0.1);
      curve.addKnot ( 0.7, 0.75, 0.9);
      check ("curve should be invertible", curve.isInvertible() == true);

      double alpha = 2.3;
      for (int i=0; i<=20; i++) {
         xchk = i*0.1-1.0;
         y = curve.evalY (xchk);
         x = curve.solveX (y);
         checkEquals ("multi-knot inverse", x, xchk, 1e-10);
         y = curve.evalY (xchk, alpha);
         x = curve.solveX (y, alpha);
         checkEquals ("multi-knot inverse with alpha", x, xchk, 1e-10);
      }

      // try same curve but with y going the other way
      curve.clearKnots();
      curve.addKnot (-0.8, 0.65, -0.001);
      curve.addKnot (0.01, -0.1, -0.1);
      curve.addKnot ( 0.7, -0.75, -0.9);
      check ("curve should be invertible", curve.isInvertible() == true);

      for (int i=0; i<=20; i++) {
         xchk = i*0.1-1.0;
         y = curve.evalY (xchk);
         x = curve.solveX (y);
         checkEquals ("multi-knot inverse", x, xchk, 1e-10);
         y = curve.evalY (xchk, -alpha);
         x = curve.solveX (y, -alpha);
         checkEquals ("multi-knot inverse with alpha", x, xchk, 1e-10);
      }

      // try some semi-invertable curves

      curve.clearKnots();
      curve.addKnot (-0.5, 0.55, 0.0);
      curve.addKnot ( 0.2, 2.00, 2.0);
      check ("curve should be invertible", curve.isInvertible() == true);

      checkEquals ("inverse at zero deriv point", curve.solveX(0.55), -0.5);
      checkForIllegalArgumentException (
         ()->curve.solveX(0.54));

      curve.addKnot (3.0, 3.50, 0.0);
      check ("curve should be invertible", curve.isInvertible() == true);

      checkEquals ("inverse at zero deriv point", curve.solveX(3.5), 3.0);
      checkForIllegalArgumentException (
         ()->curve.solveX(3.51));

      curve.addKnot (4.0, 4.50, 0.0);
      check ("curve should be invertible", curve.isInvertible() == true);
      checkEquals ("inverse at zero deriv point", curve.solveX(3.5), 3.0);
      checkEquals ("inverse at zero deriv point", curve.solveX(4.5), 4.0);
      checkForIllegalArgumentException (
         ()->curve.solveX(4.51));
   }

   /**
    * Checks that a spline is in fact a natural spline corresponding to the
    * specified x and y inputs.
    */
   private void validateNatural (
      CubicHermiteSpline1d spline,
      double[] x, double[] y, double ddy0, double ddyL) {

      int numk = spline.numKnots();
      if (numk != x.length) {
         throw new TestException (
            "incorrect number of knots: "+spline.numKnots()+
            ", expected "+x.length);
      }
      for (int k=0; k<numk; k++) {
         Knot knot = spline.getKnot(k);
         if (knot.x0 != x[k]) {
            throw new TestException (
               "incorrect x value at k="+k+": "+knot.x0+", expected "+x[k]);
         }
         if (knot.y0 != y[k]) {
            throw new TestException (
               "incorrect y value at k="+k+": "+knot.y0+", expected "+y[k]);
         }
      }
      if (numk == 2) {
         Knot knot0 = spline.getKnot(0);
         Knot knot1 = spline.getKnot(1);
         double dx = knot0.x0 - knot1.x0;
         double dy = knot0.y0 - knot1.y0;
         
         checkEquals ("knot0.dy0", knot0.dy0, dy/dx, EPS);
         checkEquals ("knot1.dy0", knot1.dy0, dy/dx, EPS);

         checkEquals ("knot0.a2", knot0.a2, 0.0);
         checkEquals ("knot0.a3", knot0.a3, 0.0);
         checkEquals ("knot1.a2", knot1.a2, 0.0);
         checkEquals ("knot1.a3", knot1.a3, 0.0);
      }
      else if (numk > 2) {
         Knot knot = spline.getKnot(0);
         checkEquals ("ddy0", 2*knot.a2, ddy0, EPS);
         knot = spline.getLastKnot();
         checkEquals ("ddyL", 2*knot.a2, ddyL, EPS);

         for (int k=0; k<numk-1; k++) {
            knot = spline.getKnot(k);
            Knot next = spline.getKnot(k+1);
            double hk = next.x0 - knot.x0;
            
            double dy = knot.dy0 + (3*knot.a3*hk + 2*knot.a2)*hk;
            if (Math.abs(dy-next.dy0) > EPS) {
               throw new TestException (
                  "discontinous dy at k="+(k+1)+
                  ": "+next.dy0+" vs. "+dy+" computed from previous");
            }
            double ddy = 2*(knot.a2 + 3*knot.a3*hk);
            if (Math.abs(ddy-2*next.a2) > EPS) {
               throw new TestException (
                  "discontinous ddy at k="+(k+1)+
                  ": "+2*next.a2+" vs. "+ddy+" computed from previous");
            }
         }
      }
   }

   void testWriteAndScan (CubicHermiteSpline1d spline) {
      CubicHermiteSpline1d newspline =
         (CubicHermiteSpline1d)ScanTest.testWriteAndScanWithClass (
            spline, null, "%g");
      if (!spline.equals (newspline)) {
         throw new TestException (
            "written-scanned spline not equal to original");
      }
   }

   void testNatural() {
      CubicHermiteSpline1d spline = new CubicHermiteSpline1d();

      double[] x = { 0.9, 1.3, 1.9, 2.1 };
      double[] y = { 1.3, 1.5, 1.85, 2.1 };
      spline.setNatural (x, y, 0, 0);
      validateNatural (spline, x, y, 0, 0);
      testWriteAndScan (spline);

      x = new double[] { -1, 1, 3 };
      y = new double[] {  4, 0, 4 };
      spline.setNatural (x, y, 0, 0);
      validateNatural (spline, x, y, 0, 0);
      testWriteAndScan (spline);

      x = new double[] { -1, 0, 1.2, 3.4, 4 };
      y = new double[] { 2.3, -1.2, 3.4, 4.5, 2 };
      spline.setNatural (x, y, 0, 0);
      validateNatural (spline, x, y, 0, 0);
      testWriteAndScan (spline);

      x = new double[] { -1, 0 };
      y = new double[] { 2.3, 7.3 };
      spline.setNatural (x, y, 0, 0);
      validateNatural (spline, x, y, 0, 0);
      testWriteAndScan (spline);
   }

   public void test() {
      testInverse();
      testNatural();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      CubicHermiteSpline1dTest tester = new CubicHermiteSpline1dTest ();
      tester.runtest();
   }

}
