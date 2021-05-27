package maspack.interpolation;

import maspack.util.*;
import maspack.matrix.*;

public class CubicHermiteSpline1dTest extends UnitTest {

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

      for (int i=0; i<=20; i++) {
         xchk = i*0.1-1.0;
         y = curve.evalY (xchk);
         x = curve.solveX (y);
         checkEquals ("multi-knot inverse", x, xchk, 1e-10);
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
      }
   }

   public void test() {
      testInverse();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      CubicHermiteSpline1dTest tester = new CubicHermiteSpline1dTest ();
      tester.runtest();
   }

}
