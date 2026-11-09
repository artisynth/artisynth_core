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

   double myDerivTol = 1e-6;
   double myMaxDerivErr = 0;
   boolean myPrintDerivErrs = false;

   private void checkDeriv (
      String msg, double d, double dnum, double fmax) {
      double err = Math.abs(d-dnum)/Math.max(fmax, Math.abs(dnum));
      myMaxDerivErr = Math.max (myMaxDerivErr, err);
      if (myPrintDerivErrs) {
         System.out.printf ("%-70s err=%8.2e\n", msg, err);
      }
      if (err > myDerivTol) {
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
      double lnorm, double a, double ldot) {

      double h = 0.01;
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
      DataBuffer state = new DataBuffer();
      mat.getState (state);

      double f0 = computeFAtState (mat, state, l, ldot, a);
      double K = mat.computeDFdl (l, ldot, 0, a);
      double D = mat.computeDFdldot (l, ldot, 0, a);
      // current Rm, at l, used for dF/dl when vm is estimated from ldot
      double Rm = mat.computeClippedRm();

      // central differences
      double dl = 1e-7*l;
      double dFdl = (computeFAtState (mat, state, l+dl, ldot, a)-
                     computeFAtState (mat, state, l-dl, ldot, a))/(2*dl);
      String msg = mat.getClass().getSimpleName()+" lmDotFromLDot="
         +lmDotFromLDot+" lnorm="+lnorm+" a="+a+" ldot="+ldot;
      if (lmDotFromLDot) {
         double dv = 1e-8; // fv curvature changes at vm = 0, so keep dv small
         double dFdldot =
            (computeFAtState (mat, state, l, ldot+dv, a)-
             computeFAtState (mat, state, l, ldot-dv, a))/(2*dv);
         checkDeriv (msg+" dF/dl", K, dFdl, fmax);
         checkDeriv (msg+" dF/dldot", D, dFdldot, fmax*h);
      }
      else {
         checkDeriv (msg+" K+D/h", K+D/h, dFdl, fmax);
         // D must account for the force change due to lmprev advancing over
         // the step, (dF/dlmprev) h vm, with vm assumed equal to ldot
         double dlm = 1e-7*l;
         double dFdlm0 =
            (computeFAtState (mat, state, dlm, l, ldot, a)-
             computeFAtState (mat, state, -dlm, l, ldot, a))/(2*dlm);
         checkDeriv (msg+" D", D, -h*dFdlm0, fmax*h);
      }
      if (myPrintDerivErrs) {
         System.out.printf ("   Rm=%g\n", Rm);
      }
   }

   /**
    * Computes the force at l for the given state, with the previous muscle
    * length perturbed by dlm.
    */
   double computeFAtState (
      EquilibriumAxialMuscle mat, DataBuffer state, double dlm,
      double l, double ldot, double a) {
      state.resetOffsets();
      mat.setState (state);
      mat.myMuscleLengthPrev += dlm;
      return mat.computeF (l, ldot, 0, a);
   }

   void testCompliantDerivatives (EquilibriumAxialMuscle mat) {
      for (boolean lmDotFromLDot : new boolean[] { false, true }) {
         // near optimal length, and on the descending limb at full activation
         testCompliantDerivatives (
            (EquilibriumAxialMuscle)mat.clone(), lmDotFromLDot, 1.0, 0.5, 0.05);
         testCompliantDerivatives (
            (EquilibriumAxialMuscle)mat.clone(), lmDotFromLDot, 1.3, 1.0, 0.05);
      }
      // static hold: with computeLmDotFromLDot, vm is exactly 0 when ldot
      // is 0, and the damping must still include the force-velocity slope
      testCompliantDerivatives (
         (EquilibriumAxialMuscle)mat.clone(), true, 1.0, 0.5, 0.0);
   }

   void testCompliantDerivatives() {
      // The Brent solver's tolerance on lm (1e-8*(T+lo)) gives errors in the
      // numeric derivatives of up to about 2e-5 (relative), so use the Newton
      // solver, which converges much more tightly.
      boolean saveUseNewton = EquilibriumAxialMuscle.useNewtonSolver;
      double saveMaxRm = EquilibriumAxialMuscle.maxRm;
      EquilibriumAxialMuscle.useNewtonSolver = true;
      // allow Rm > 1, so derivatives on the descending limb are exact
      EquilibriumAxialMuscle.maxRm = 10;
      try {
         testCompliantDerivatives (
            new Millard2012AxialMuscle (1000, 0.1, 0.2, 0.1));
         testCompliantDerivatives (
            new Thelen2003AxialMuscle (1000, 0.1, 0.2, 0.1));
         // long compliant tendons on the descending limb, for which Rm is
         // well above 1 (about 3.2 for Millard and 1.4 for Thelen)
         for (boolean lmDotFromLDot : new boolean[] { false, true }) {
            testCompliantDerivatives (
               new Millard2012AxialMuscle (1000, 0.1, 2.0, 0.1),
               lmDotFromLDot, 1.8, 1.0, 0.05);
            testCompliantDerivatives (
               new Thelen2003AxialMuscle (1000, 0.1, 2.0, 0.1),
               lmDotFromLDot, 1.7, 1.0, 0.05);
         }
      }
      finally {
         EquilibriumAxialMuscle.useNewtonSolver = saveUseNewton;
         EquilibriumAxialMuscle.maxRm = saveMaxRm;
      }
   }

   void testDerivatives() {
      testDerivatives (new Millard2012AxialMuscle());
      testDerivatives (new Thelen2003AxialMuscle());
   }

   private void checkScaledForce (
      String msg, double f, double fscaled, double s) {
      double err = Math.abs(fscaled-s*f)/Math.abs(s*f);
      if (err > 1e-10) {
         throw new TestException (
            msg + ": force scaled to "+fscaled+", expected "+(s*f));
      }
   }

   /**
    * Changing units with scaleDistance() or scaleMass() should scale the
    * force by s, with maxContractionVelocity and fibreDamping unchanged.
    */
   public void testScaling (EquilibriumAxialMuscle mat) {
      double s = 1000;
      double a = 1.0;
      // rigid tendon makes computeF() independent of muscle state
      mat.setRigidTendon (true);
      mat.setFibreDamping (0.1);
      // muscle at 1.1 optimal fibre lengths, with nonzero velocity so that
      // force-velocity and damping terms contribute
      double l = mat.getTendonSlackLength() + 1.1*mat.getOptFibreLength();
      double ldot = 0.3;
      double f = mat.computeF (l, ldot, 0, a);

      EquilibriumAxialMuscle scaled =
         (EquilibriumAxialMuscle)mat.clone();
      scaled.scaleDistance (s);
      checkScaledForce (
         "scaleDistance", f, scaled.computeF (s*l, s*ldot, 0, a), s);

      scaled = (EquilibriumAxialMuscle)mat.clone();
      scaled.scaleMass (s);
      checkScaledForce (
         "scaleMass", f, scaled.computeF (l, ldot, 0, a), s);
   }

   private void checkUnchanged (String msg, double d, double dscaled) {
      double err = Math.abs(dscaled-d)/Math.abs(d);
      if (err > 1e-8) {
         throw new TestException (
            msg + ": changed to "+dscaled+" from "+d+" after scaling");
      }
   }

   /**
    * For a compliant tendon in mid-simulation, scaleDistance(s) should scale
    * the force by s and leave the stiffness and damping unchanged, both from
    * cached values and after recomputing the muscle length.
    */
   public void testCompliantScaling (
      EquilibriumAxialMuscle mat, boolean lmDotFromLDot) {
      double s = 1000;
      double a = 0.5;
      double h = 0.01;
      double ldot = 0.05;
      mat.setRigidTendon (false);
      mat.setFibreDamping (0.1);
      mat.setComputeLmDotFromLDot (lmDotFromLDot);
      double l0 =
         1.02*mat.getTendonSlackLength() + 1.1*mat.getOptFibreLength();
      mat.computeF (l0, 0, 0, a);
      mat.advanceState (0, h);
      double l = l0 + ldot*h;
      double f = mat.computeF (l, ldot, 0, a);
      double K = mat.computeDFdl (l, ldot, 0, a);
      double D = mat.computeDFdldot (l, ldot, 0, a);

      // scale mat itself, since clone() invalidates the cached values
      EquilibriumAxialMuscle scaled = mat;
      scaled.scaleDistance (s);
      String msg = mat.getClass().getSimpleName()+" lmDotFromLDot="
         +lmDotFromLDot;
      checkUnchanged (msg+" cached K", K, scaled.computeDFdl (s*l, s*ldot, 0, a));
      checkUnchanged (
         msg+" cached D", D, scaled.computeDFdldot (s*l, s*ldot, 0, a));
      checkScaledForce (msg, f, scaled.computeF (s*l, s*ldot, 0, a), s);
      checkUnchanged (msg+" K", K, scaled.computeDFdl (s*l, s*ldot, 0, a));
      checkUnchanged (msg+" D", D, scaled.computeDFdldot (s*l, s*ldot, 0, a));
   }

   private void checkEqual (String msg, double d, double dchk) {
      double err = Math.abs(dchk-d)/Math.abs(d);
      if (err > 1e-8) {
         throw new TestException (msg + ": "+dchk+", expected "+d);
      }
   }

   /**
    * For a compliant tendon, a force query at another length between steps
    * (e.g., by an output probe after the position update) should give the
    * same force as the next step's solve at that length, and should not
    * change the next step.
    */
   /**
    * Checks that a query at a length l2 other than that of the step start,
    * made before the next advanceState(), returns the same values as the next
    * step at l2, and does not change the next step. Also checks that repeated
    * solves at the step start length agree, and that the step can be restored
    * with getState()/setState(). lfrac gives the initial muscle length as a
    * fraction of the optimal fibre length; for values {@code <} 1 the muscle
    * is on the ascending limb, and Dm {@code >} 0, so that the vm/ldot ratio
    * Dt/(Dm+Dt) is not clipped to 1.
    */
   public void testSideQuery (
      EquilibriumAxialMuscle mat, boolean lmDotFromLDot, double lfrac) {
      double a = 0.5;
      double h = 0.01;
      double ldot = 0.05;
      mat.setRigidTendon (false);
      mat.setFibreDamping (0.1);
      mat.setComputeLmDotFromLDot (lmDotFromLDot);
      double l0 =
         1.02*mat.getTendonSlackLength() + lfrac*mat.getOptFibreLength();
      double l1 = l0 + ldot*h;
      double l2 = l1 + ldot*h;
      String msg = mat.getClass().getSimpleName()+" lmDotFromLDot="
         +lmDotFromLDot+" lfrac="+lfrac;

      EquilibriumAxialMuscle matq = (EquilibriumAxialMuscle)mat.clone();
      EquilibriumAxialMuscle mats = (EquilibriumAxialMuscle)mat.clone();
      double f1 = 0;
      for (EquilibriumAxialMuscle m :
              new EquilibriumAxialMuscle[] {mat,matq,mats}) {
         m.computeF (l0, 0, 0, a);
         m.advanceState (0, h);
         f1 = m.computeF (l1, ldot, 0, a);
      }
      if (lfrac < 1 && !(mat.myDm > 0)) {
         throw new TestException (msg+": Dm="+mat.myDm+", expected > 0");
      }
      // repeated solves at l1 should give the same result
      checkEqual (msg+" repeated F", f1, mat.computeF (l1, ldot, 0, a));
      checkEqual (msg+" repeated F", f1, mat.computeF (l1, ldot, 0, a));

      // save the state of mats, query it at l2, and then restore it
      DataBuffer state = new DataBuffer();
      mats.getState (state);
      mats.computeF (l2, 2*ldot, 0, 0.8*a);
      mats.computeDFdl (l2, 2*ldot, 0, 0.8*a);
      state.resetOffsets();
      mats.setState (state);

      // query matq at l2 before the next advanceState
      double fq = matq.computeF (l2, ldot, 0, a);
      double Kq = matq.computeDFdl (l2, ldot, 0, a);
      double Dq = matq.computeDFdldot (l2, ldot, 0, a);
      checkEqual (msg+" F at l1 after query", f1, matq.computeF (l1, ldot, 0, a));
      for (EquilibriumAxialMuscle m :
              new EquilibriumAxialMuscle[] {mat,matq,mats}) {
         m.advanceState (h, 2*h);
      }
      double f = mat.computeF (l2, ldot, 0, a);
      double K = mat.computeDFdl (l2, ldot, 0, a);
      double D = mat.computeDFdldot (l2, ldot, 0, a);
      checkEqual (msg+" queried F", f, fq);
      checkEqual (msg+" queried K", K, Kq);
      checkEqual (msg+" queried D", D, Dq);
      checkEqual (msg+" F after query", f, matq.computeF (l2, ldot, 0, a));
      checkEqual (
         msg+" K after query", K, matq.computeDFdl (l2, ldot, 0, a));
      checkEqual (
         msg+" D after query", D, matq.computeDFdldot (l2, ldot, 0, a));
      // derivatives should be recomputed at l2 even if not preceded by F
      checkEqual (
         msg+" K after restore", K, mats.computeDFdl (l2, ldot, 0, a));
      checkEqual (
         msg+" D after restore", D, mats.computeDFdldot (l2, ldot, 0, a));
      checkEqual (msg+" F after restore", f, mats.computeF (l2, ldot, 0, a));
   }

   void testSideQuery() {
      for (boolean lmDotFromLDot : new boolean[] { false, true }) {
         for (double lfrac : new double[] { 0.7, 1.1 }) {
            testSideQuery (new Millard2012AxialMuscle(), lmDotFromLDot, lfrac);
            testSideQuery (new Thelen2003AxialMuscle(), lmDotFromLDot, lfrac);
         }
      }
   }

   void testScaling() {
      testScaling (new Millard2012AxialMuscle());
      testScaling (new Thelen2003AxialMuscle());
      for (boolean lmDotFromLDot : new boolean[] { false, true }) {
         testCompliantScaling (new Millard2012AxialMuscle(), lmDotFromLDot);
         testCompliantScaling (new Thelen2003AxialMuscle(), lmDotFromLDot);
      }
   }

   public void test() {
      testCompliantDerivatives();
      testDerivatives();
      testScaling();
      testSideQuery();
   }

   public static void main (String[] args) {
      EquilibriumAxialMuscleTest tester = new EquilibriumAxialMuscleTest();
      for (String arg : args) {
         if (arg.equals ("-verbose")) {
            tester.myPrintDerivErrs = true;
         }
      }
      tester.runtest();
      if (tester.myPrintDerivErrs) {
         System.out.printf ("max deriv err=%8.2e\n", tester.myMaxDerivErr);
      }
   }
}
