package artisynth.core.materials;

import artisynth.core.modelbase.HasNumericState;
import maspack.util.DataBuffer;
import maspack.util.*;
import maspack.properties.*;
import maspack.numerics.*;
import maspack.function.*;
import maspack.interpolation.CubicHermiteSpline1d;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

public class EquilibriumAxialMuscleTest extends UnitTest {
   
   public void testDerivatives (EquilibriumAxialMuscle mat) {
      int nsamps = 50;
      double a = 1.0;
      
      for (int i=0; i<=nsamps; i++) {
         double h = 1e-8;

         // force velocity curve
         double vn = -1.1 + 2.0*i/nsamps;
         if (i == nsamps) {
            vn -= 2*h;
         }
         double fv0 = mat.computeForceVelocity (vn, a);
         double fv1 = mat.computeForceVelocity (vn+h, a);
         double dfv = mat.computeForceVelocityDeriv (vn, a);
         double dfvNum = (fv1-fv0)/h;

         double err = Math.abs(dfv-dfvNum)/Math.max(1.0, Math.abs(dfv));
         if (err > 1e-6) {
            throw new TestException (
               "forceVelocity derivative: error "+err+" at vn=" + vn);
         }

         // active force length curve
         double ln = 2.0*i/nsamps;
         if (i == nsamps) {
            ln -= 2*h;
         }
         double fa0 = mat.computeActiveForceLength (ln);
         double fa1 = mat.computeActiveForceLength (ln+h);
         double dfa = mat.computeActiveForceLengthDeriv (ln);
         double dfaNum = (fa1-fa0)/h;

         err = Math.abs(dfa-dfaNum)/Math.max(1.0, Math.abs(dfa));
         if (err > 1e-6) {
            throw new TestException (
               "activeForceLength derivative: error "+err+" at ln=" + ln);
         }

         // active force length curve
         double fp0 = mat.computePassiveForceLength (ln);
         double fp1 = mat.computePassiveForceLength (ln+h);
         double dfp = mat.computePassiveForceLengthDeriv (ln);
         double dfpNum = (fp1-fp0)/h;

         err = Math.abs(dfp-dfpNum)/Math.max(1.0, Math.abs(dfp));
         if (err > 1e-7) {
            throw new TestException (
               "passiveForceLength derivative: error "+err+" at ln=" + ln);
         }

         // tendon force length curve
         h = 1e-9;
         double ltn = 1.0-0.01 + 0.1*i/nsamps;
         double ft0 = mat.computeTendonForce (ltn);
         double ft1 = mat.computeTendonForce (ltn+h);
         double dft = mat.computeTendonForceDeriv (ltn);
         double dftNum = (ft1-ft0)/h;

         err = Math.abs(dft-dftNum)/Math.max(1.0, Math.abs(dft));
         if (err > 1e-6) {
            throw new TestException (
               "tendonForce derivative: error "+err+" at ltn=" + ltn);
         }
      }
      
   }

   void testDerivatives() {
      testDerivatives (new Millard2012AxialMuscle());
      testDerivatives (new Thelen2003AxialMuscle());
   }

   public void test() {
      testDerivatives();
   }

   public static void main (String[] args) {
      EquilibriumAxialMuscleTest tester = new EquilibriumAxialMuscleTest();
      tester.runtest();
   }
}
