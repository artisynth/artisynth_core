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
         if (err > 2e-6) {
            throw new TestException (
               "tendonForce derivative: error "+err+" at ltn=" + ltn);
         }
      }
      
   }

   private double computeFAtState (
      EquilibriumAxialMuscle mat, DataBuffer state,
      double l, double ldot, double a) {
      state.resetOffsets();
      mat.setState (state);
      return mat.computeF (l, ldot, 0, a);
   }

   private void checkDeriv (
      String msg, double d, double dnum, double fmax) {
      double err = Math.abs(d-dnum)/Math.max(fmax, Math.abs(dnum));
      if (err > 1e-5) {
         throw new TestException (
            msg + ": computed "+d+", numeric "+dnum+", rel error "+err);
      }
   }

   /**
    * Checks computeDFdl() and computeDFdldot() for a compliant tendon against
    * numeric derivatives of computeF(), holding the muscle state from the
    * previous step fixed. With computeLmDotFromLDot false, the force depends
    * on l only (vm = (lm-lmprev)/h), so the backward Euler stiffness K + D/h
    * must match dF/dl. With computeLmDotFromLDot true, K and D must match
    * dF/dl and dF/dldot separately.
    */
   public void testCompliantDerivatives (
      EquilibriumAxialMuscle mat, boolean lmDotFromLDot,
      double lnorm, double a) {

      double h = 0.01;
      double ldot = 0.05;
      mat.setRigidTendon (false);
      mat.setFibreDamping (0.1);
      mat.setComputeLmDotFromLDot (lmDotFromLDot);
      double T = mat.getTendonSlackLength();
      double lo = mat.getOptFibreLength();
      double fmax = mat.getMaxIsoForce();
      double l0 = 1.02*T + lnorm*lo;

      // initialize at rest, take one step, then evaluate at l with ldot
      mat.computeF (l0, 0, 0, a);
      mat.advanceState (0, h);
      double l = l0 + ldot*h;
      if (lmDotFromLDot) {
         // let the stored Dm, Dt (used to estimate vm) settle at l
         mat.computeF (l, ldot, 0, a);
         mat.computeF (l, ldot, 0, a);
      }
      DataBuffer state = new DataBuffer();
      mat.getState (state);

      double f0 = computeFAtState (mat, state, l, ldot, a);
      double K = mat.computeDFdl (l, ldot, 0, a);
      double D = mat.computeDFdldot (l, ldot, 0, a);

      double dl = 1e-7*l;
      double dFdl = (computeFAtState (mat, state, l+dl, ldot, a)-f0)/dl;
      String msg = mat.getClass().getSimpleName()+" lmDotFromLDot="
         +lmDotFromLDot+" lnorm="+lnorm+" a="+a;
      if (lmDotFromLDot) {
         double dv = 1e-7;
         double dFdldot =
            (computeFAtState (mat, state, l, ldot+dv, a)-f0)/dv;
         checkDeriv (msg+" dF/dl", K, dFdl, fmax);
         checkDeriv (msg+" dF/dldot", D, dFdldot, fmax*h);
      }
      else {
         checkDeriv (msg+" K+D/h", K+D/h, dFdl, fmax);
      }
   }

   void testCompliantDerivatives() {
      for (boolean lmDotFromLDot : new boolean[] { false, true }) {
         // near optimal length, and on the descending limb at full activation
         testCompliantDerivatives (
            new Millard2012AxialMuscle (1000, 0.1, 0.2, 0.1),
            lmDotFromLDot, 1.0, 0.5);
         testCompliantDerivatives (
            new Millard2012AxialMuscle (1000, 0.1, 0.2, 0.1),
            lmDotFromLDot, 1.3, 1.0);
      }
   }

   void testDerivatives() {
      testDerivatives (new Millard2012AxialMuscle());
      testDerivatives (new Thelen2003AxialMuscle());
   }

   public void test() {
      testCompliantDerivatives();
      testDerivatives();
   }

   public static void main (String[] args) {
      EquilibriumAxialMuscleTest tester = new EquilibriumAxialMuscleTest();
      tester.runtest();
   }
}
