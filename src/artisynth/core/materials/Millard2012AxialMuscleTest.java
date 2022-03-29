package artisynth.core.materials;

import maspack.matrix.*;
import maspack.util.*;
import maspack.interpolation.*;

/**
 * Tests AxialMaterials by checking that their computed derivatives are
 * consistent with the values determined via numeric differentiation.
 */
public class Millard2012AxialMuscleTest extends UnitTest {

   Millard2012AxialMuscleTest() {
   }

   void testCurveDerivatives (
      String name, CubicHermiteSpline1d curve) {
      double tol = 2e-7;
      double err = CubicHermiteSpline1dTest.numericDerivativeError(curve);
      if (err > tol) {
         throw new TestException (
            "Numeric derivative check of "+name+" curve has error "+err+
            "; exceeds tolerance of "+tol);
      }
   }

   public void test() {
   
      Millard2012AxialMuscle mat = new Millard2012AxialMuscle();
   
      testCurveDerivatives (
         "activeForceLength", mat.getActiveForceLengthCurve());
      testCurveDerivatives (
         "passiveForceLength", mat.getPassiveForceLengthCurve());
      testCurveDerivatives (
         "tendonForceLength", mat.getTendonForceLengthCurve());
      testCurveDerivatives (
         "forceVelocity", mat.getForceVelocityCurve());
         
   }

   public static void main (String[] args) {
      Millard2012AxialMuscleTest tester = new Millard2012AxialMuscleTest();
      tester.runtest();
   }

}
