/**
 * Copyright (c) 2026, by the Authors: Jeff M. Barrett
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.materials;

import java.util.Random;

import maspack.util.DataBuffer;
import maspack.util.TestException;
import maspack.util.UnitTest;

/**
 * Tests HuxleyAxialMuscle by checking its closed-form moment integrals
 * against numeric quadrature, its isometric steady state and short-range
 * stiffness against analytic values, its derivatives against numeric
 * differentiation, its time integration against a fine-step reference
 * solution of the distribution-moment equations, and its state handling.
 */
public class HuxleyAxialMuscleTest extends UnitTest {

   Random myRand = new Random (0x1234);

   // ---- numeric reference for the kinetic rates ----

   static double fRate (HuxleyAxialMuscle mat, double xi) {
      return (xi > 0 && xi <= 1) ? mat.getAttachmentRate()*xi : 0;
   }

   static double gRate (HuxleyAxialMuscle mat, double xi) {
      if (xi < 0) {
         return mat.getCompressedDetachmentRate();
      }
      else if (xi <= 1) {
         return mat.getDetachmentRate()*xi;
      }
      else {
         return (mat.getDetachmentRate() +
                 mat.getExtendedDetachmentRate()*(xi-1));
      }
   }

   /**
    * Simpson integration of xi^k*(a*f(xi) - (f(xi)+g(xi))*n(xi)) over [w,u],
    * for a uniform distribution n of height N on [lo,hi]. The integrand must
    * be smooth on [w,u].
    */
   static double integrateRate (
      HuxleyAxialMuscle mat, int k, double a,
      double N, double lo, double hi, double w, double u) {

      int nint = 64; // must be even
      double dx = (u-w)/nint;
      double sum = 0;
      for (int i=0; i<=nint; i++) {
         double xi = w + i*dx;
         // evaluate on the interior side of the endpoints, since f, g and n
         // are discontinuous at the breakpoints
         double xe = Math.min (Math.max (xi, w+1e-12*dx), u-1e-12*dx);
         double n = (xe >= lo && xe <= hi) ? N : 0;
         double val = Math.pow (xi, k)*(
            a*fRate(mat,xe) - (fRate(mat,xe) + gRate(mat,xe))*n);
         double wgt = (i == 0 || i == nint) ? 1 : (i%2 == 1 ? 4 : 2);
         sum += wgt*val;
      }
      return sum*dx/3;
   }

   /**
    * Computes the kinetic rates by numeric quadrature, splitting the domain
    * at the breakpoints of f, g and n so that each piece is a polynomial
    * (which Simpson's rule integrates exactly up to degree 3).
    */
   static void numericKineticRates (
      double[] dQ, HuxleyAxialMuscle mat, double a,
      double N, double delta, double xi0) {

      double lo = xi0-delta;
      double hi = xi0+delta;
      double xmin = Math.min (lo, 0);
      double xmax = Math.max (hi, 1);
      double[] brks = new double[] { xmin, lo, 0, 1, hi, xmax };
      java.util.Arrays.sort (brks);
      for (int k=0; k<3; k++) {
         double sum = 0;
         for (int i=0; i<brks.length-1; i++) {
            if (brks[i+1] > brks[i]) {
               sum += integrateRate (mat, k, a, N, lo, hi, brks[i], brks[i+1]);
            }
         }
         dQ[k] = sum;
      }
   }

   static void momentsFromDistribution (
      double[] Q, double N, double delta, double xi0) {
      Q[0] = 2*delta*N;
      Q[1] = 2*delta*N*xi0;
      Q[2] = 2*delta*N*(delta*delta + 3*xi0*xi0)/3;
   }

   /**
    * Checks the closed-form kinetic rates against numeric quadrature over a
    * grid of uniform distributions.
    */
   public void testKineticRates() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle();
      double[] Q = new double[3];
      double[] dQ = new double[3];
      double[] dQchk = new double[3];
      // scale of the rates, used to normalize errors
      double rscale = mat.getCompressedDetachmentRate();
      int ntests = 0;
      for (double N=0.01; N<=1.0; N+=0.11) {
         for (double delta=0.01; delta<=1.0; delta+=0.07) {
            for (double xi0=-5.0; xi0<=5.0; xi0+=0.37) {
               for (double a : new double[] { 0.0, 0.3, 1.0 }) {
                  momentsFromDistribution (Q, N, delta, xi0);
                  mat.computeKineticRates (dQ, Q, a);
                  numericKineticRates (dQchk, mat, a, N, delta, xi0);
                  for (int k=0; k<3; k++) {
                     double mag = rscale*(1 + Math.pow(Math.abs(xi0)+delta,k));
                     double err = Math.abs (dQ[k]-dQchk[k])/mag;
                     if (err > 1e-10) {
                        throw new TestException (
                           "kinetic rate "+k+" = "+dQ[k]+", expected "+
                           dQchk[k]+", N="+N+" delta="+delta+" xi0="+xi0+
                           " a="+a);
                     }
                  }
                  ntests++;
               }
            }
         }
      }
      check ("no kinetic rate tests performed", ntests > 1000);

      // empty distribution: only the attachment source term remains
      Q[0] = Q[1] = Q[2] = 0;
      mat.computeKineticRates (dQ, Q, 0.5);
      double f1 = mat.getAttachmentRate();
      checkEquals ("empty dQ0", dQ[0], 0.5*f1/2, 1e-14);
      checkEquals ("empty dQ1", dQ[1], 0.5*f1/3, 1e-14);
      checkEquals ("empty dQ2", dQ[2], 0.5*f1/4, 1e-14);
   }

   /**
    * Checks the partial moments for narrow distributions, which should
    * approach those of a delta function. (The tolerance accounts for the
    * rounding of xi0 +/- delta.)
    */
   public void testNarrowPartialMoments() {
      double[] J = new double[4];
      double delta = 1e-7;
      double N = 1/(2*delta);
      for (double xi0 : new double[] { -3.0, 0.5, 7.0 }) {
         HuxleyAxialMuscle.computePartialMoments (
            J, N, xi0-delta, xi0+delta,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
         for (int k=0; k<4; k++) {
            // a delta function of unit mass at xi0
            double chk = Math.pow (xi0, k);
            checkEquals (
               "narrow J"+k+" at xi0="+xi0, J[k], chk,
               1e-8*(1+Math.abs(chk)));
         }
      }
   }

   /**
    * Checks that the isometric steady state is a fixed point of the kinetics
    * and gives the expected force and stiffness.
    */
   public void testIsometricSteadyState() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle (0.142, 24e-4);
      double l = mat.getOptFibreLength();
      double[] Q = new double[3];
      double[] dQ = new double[3];
      for (double a : new double[] { 0.0, 0.005, 0.1, 0.5, 1.0 }) {
         mat.computeIsometricMoments (Q, a);
         mat.computeKineticRates (dQ, Q, a);
         for (int k=0; k<3; k++) {
            checkEquals (
               "steady state rate "+k+" for a="+a, dQ[k], 0, 1e-12);
         }
         mat.clearState();
         double F = mat.computeF (l, 0, 0, a);
         double K = mat.computeDFdl (l, 0, 0, a);
         double Fmax = mat.getMaxIsoForce();
         double Kmax = mat.getMaxIsoStiffness();
         checkEquals ("isometric force for a="+a, F, a*Fmax, 1e-10*Fmax);
         checkEquals ("isometric stiffness for a="+a, K, a*Kmax, 1e-10*Kmax);
         // integrating over several steps should not change the force
         for (int i=0; i<100; i++) {
            mat.advanceState (i*1e-3, (i+1)*1e-3);
            F = mat.computeF (l, 0, 0, a);
         }
         checkEquals (
            "isometric force after 100 steps for a="+a, F, a*Fmax, 1e-10*Fmax);
      }
      // regression check against the original implementation, which gave a
      // force of 828.025 N for these parameters at full activation
      checkEquals (
         "max isometric force", mat.getMaxIsoForce(), 828.025, 1e-6);
      // corresponding stiffness, as reported by the original implementation
      checkEquals (
         "max isometric stiffness",
         mat.getMaxIsoStiffness(), 2565711.26, 1e-2);
   }

   /**
    * Checks that an instantaneous stretch produces the expected change in
    * force. At the isometric steady state, the short-range stiffness is
    * related to the force by K = (s/h) F/X.
    */
   public void testShortRangeStiffness() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle();
      double X = mat.getOptFibreLength();
      double q = mat.getSarcomereLength()/mat.getAttachmentRange();
      for (double a=0.0; a<=1.0; a+=0.25) {
         for (double dx=0.0001; dx<=0.01; dx*=3) {
            mat.setIsometricSteadyState (a);
            double F0 = mat.computeF (X, 0, 0, a);
            double K = mat.computeDFdl (X, 0, 0, a);
            double F1 = mat.computeF (X+dx, 0, 0, a);
            double Kchk = (F1-F0)/dx;
            checkEquals (
               "stiffness for a="+a+" dx="+dx, K, Kchk,
               1e-8*mat.getMaxIsoStiffness());
            checkEquals (
               "analytic stiffness for a="+a+" dx="+dx, K, q*F0/X,
               1e-8*mat.getMaxIsoStiffness());
         }
      }
   }

   /**
    * Checks computeDFdl() and computeDFdldot() using numeric differentiation,
    * at a variety of states produced by an imposed motion.
    */
   public void testDerivatives() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle (0.2, 1.5e-3, 40.0);
      double X = mat.getOptFibreLength();
      double gamma = mat.getStrainScale();
      double h = 1e-3;
      double a = 0.7;
      mat.setIsometricSteadyState (a);
      double Kmax = mat.getMaxIsoStiffness();
      for (int i=0; i<200; i++) {
         double t0 = i*h;
         double t1 = t0+h;
         mat.advanceState (t0, t1);
         // motion spanning several units of xi
         double l = X + 3*gamma*Math.sin (2*Math.PI*2*t0);
         double ldot = 2*Math.PI*2*3*gamma*Math.cos (2*Math.PI*2*t0);
         mat.computeF (l, ldot, 0, a);
         // check derivatives at a perturbed length within the step
         double lp = l + gamma*(myRand.nextDouble()-0.5);
         double dl = 1e-6*gamma;
         double F = mat.computeF (lp, ldot, 0, a);
         double dFdl = mat.computeDFdl (lp, ldot, 0, a);
         double Fl = mat.computeF (lp+dl, ldot, 0, a);
         checkEquals (
            "dFdl at step "+i, dFdl, (Fl-F)/dl, 1e-6*Kmax);
         double dv = 1e-3;
         double dFdldot = mat.computeDFdldot (lp, ldot, 0, a);
         double Fv = mat.computeF (lp, ldot+dv, 0, a);
         checkEquals (
            "dFdldot at step "+i, dFdldot, (Fv-F)/dv, 1e-6*mat.getDamping());
         // restore final evaluation at the unperturbed length
         mat.computeF (l, ldot, 0, a);
      }
      check ("damping symmetric", !mat.isDFdldotZero());
      mat.setDamping (0);
      check ("no damping symmetric", mat.isDFdldotZero());
   }

   // ---- comparison with a fine-step reference solution ----

   /**
    * Imposed length trajectory for the integration tests.
    */
   static double length (double X, double gamma, double t) {
      return X + 2.5*gamma*Math.sin (2*Math.PI*1.5*t) - 0.5*gamma*t;
   }

   static double lengthDot (double X, double gamma, double t) {
      return 2.5*gamma*2*Math.PI*1.5*Math.cos (2*Math.PI*1.5*t) - 0.5*gamma;
   }

   /**
    * Imposed activation trajectory for the integration tests.
    */
   static double activation (double t) {
      return 0.5 + 0.4*Math.sin (2*Math.PI*0.7*t);
   }

   /**
    * Evaluates the full distribution-moment equations
    * <pre>
    *   dQ_k/dt = a beta_k - phi_k - psi_k - k u Q_(k-1)
    * </pre>
    * with u = -ldot/gamma.
    */
   static void evalMomentDerivative (
      double[] dQdt, HuxleyAxialMuscle mat, double[] Q, double t) {
      double X = mat.getOptFibreLength();
      double gamma = mat.getStrainScale();
      mat.computeKineticRates (dQdt, Q, activation (t));
      double u = -lengthDot (X, gamma, t)/gamma;
      dQdt[1] -= u*Q[0];
      dQdt[2] -= 2*u*Q[1];
   }

   /**
    * Integrates the distribution-moment equations using RK4 with a small time
    * step, returning the force at each multiple of {@code hout}.
    */
   static double[] computeReferenceForces (
      HuxleyAxialMuscle mat, double tend, double hout) {

      int nout = (int)Math.round (tend/hout);
      int nsub = 100;
      double hs = hout/nsub;
      double[] forces = new double[nout+1];
      double[] Q = new double[3];
      double[] Qtmp = new double[3];
      double[][] K = new double[4][3];
      mat.computeIsometricMoments (Q, activation(0));
      forces[0] = mat.getForceCoefficient()*Q[1];
      for (int i=0; i<nout; i++) {
         for (int j=0; j<nsub; j++) {
            double t = i*hout + j*hs;
            evalMomentDerivative (K[0], mat, Q, t);
            for (int k=0; k<3; k++) Qtmp[k] = Q[k] + hs/2*K[0][k];
            evalMomentDerivative (K[1], mat, Qtmp, t+hs/2);
            for (int k=0; k<3; k++) Qtmp[k] = Q[k] + hs/2*K[1][k];
            evalMomentDerivative (K[2], mat, Qtmp, t+hs/2);
            for (int k=0; k<3; k++) Qtmp[k] = Q[k] + hs*K[2][k];
            evalMomentDerivative (K[3], mat, Qtmp, t+hs);
            for (int k=0; k<3; k++) {
               Q[k] += hs/6*(K[0][k] + 2*K[1][k] + 2*K[2][k] + K[3][k]);
            }
         }
         forces[i+1] = mat.getForceCoefficient()*Q[1];
      }
      return forces;
   }

   /**
    * Simulates the material under the imposed length and activation, using
    * the evaluation pattern of an implicit integrator: advanceState() at the
    * start of each step, an evaluation at the start-of-step length, and a
    * final evaluation at the end-of-step length.
    *
    * @param extraCalls if {@code true}, adds redundant evaluations at
    * random lengths, which should not affect the result
    */
   double[] simulate (
      HuxleyAxialMuscle mat, double tend, double h, boolean extraCalls) {

      double X = mat.getOptFibreLength();
      double gamma = mat.getStrainScale();
      int nsteps = (int)Math.round (tend/h);
      double[] forces = new double[nsteps+1];
      mat.clearState();
      forces[0] = mat.computeF (length (X, gamma, 0), 0, 0, activation(0));
      for (int i=0; i<nsteps; i++) {
         double t0 = i*h;
         double t1 = t0+h;
         double a = activation (t1);
         if (extraCalls) {
            // evaluations between steps, such as those for rendering
            mat.computeF (X + gamma*myRand.nextDouble(), 0, 0, a);
         }
         mat.advanceState (t0, t1);
         mat.computeF (length (X, gamma, t0), 0, 0, a);
         if (extraCalls) {
            // evaluations within a step, such as those for line searches
            mat.computeF (X + gamma*myRand.nextDouble(), 0, 0, a);
            mat.computeDFdl (X + gamma*myRand.nextDouble(), 0, 0, a);
         }
         forces[i+1] = mat.computeF (length (X, gamma, t1), 0, 0, a);
      }
      return forces;
   }

   double maxForceError (
      double[] forces, double hsim, double[] refForces, double href) {
      int stride = (int)Math.round (href/hsim);
      double maxErr = 0;
      for (int i=0; i<refForces.length; i++) {
         double err = Math.abs (forces[i*stride] - refForces[i]);
         maxErr = Math.max (maxErr, err);
      }
      return maxErr;
   }

   /**
    * Checks that the operator-split integration converges, at first order,
    * to a fine-step reference solution of the distribution-moment equations.
    */
   public void testIntegrationAccuracy() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle();
      double tend = 2.0;
      double href = 1e-2;
      double[] ref = computeReferenceForces (mat, tend, href);
      double Fmax = mat.getMaxIsoForce();

      double err1 = maxForceError (simulate (mat, tend, 1e-3, false), 1e-3, ref, href);
      double err2 = maxForceError (simulate (mat, tend, 5e-4, false), 5e-4, ref, href);
      double err3 = maxForceError (simulate (mat, tend, 1e-4, false), 1e-4, ref, href);

      if (err3 > 0.005*Fmax) {
         throw new TestException (
            "force error with h=1e-4 is "+err3/Fmax+" of max force");
      }
      if (err1 > 0.05*Fmax) {
         throw new TestException (
            "force error with h=1e-3 is "+err1/Fmax+" of max force");
      }
      // first order convergence: halving h should roughly halve the error
      double ratio = err1/err2;
      if (ratio < 1.6 || ratio > 2.5) {
         throw new TestException (
            "error ratio for halved step size is "+ratio+", expected ~2");
      }
      if (!getSilent()) {
         System.out.printf (
            "integration errors (fraction of Fmax): h=1e-3: %.3e, "+
            "h=5e-4: %.3e, h=1e-4: %.3e\n",
            err1/Fmax, err2/Fmax, err3/Fmax);
      }
   }

   /**
    * Checks that results do not depend on redundant force evaluations, and
    * that saving and restoring the state reproduces results exactly.
    */
   public void testStateRepeatability() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle();
      double tend = 0.5;
      double h = 1e-3;
      double[] f0 = simulate (mat, tend, h, false);
      double[] f1 = simulate (mat, tend, h, true);
      checkEquals ("forces with redundant evaluations", f1, f0);

      // save state partway through, then restore and repeat
      double X = mat.getOptFibreLength();
      double gamma = mat.getStrainScale();
      int nsteps = (int)Math.round (tend/h);
      int nsave = nsteps/2;
      mat.clearState();
      mat.computeF (length (X, gamma, 0), 0, 0, activation(0));
      DataBuffer state = new DataBuffer();
      double[] fa = new double[nsteps];
      double[] fb = new double[nsteps];
      for (int pass=0; pass<2; pass++) {
         double[] fout = (pass == 0 ? fa : fb);
         int istart = 0;
         if (pass == 1) {
            state.resetOffsets();
            mat.setState (state);
            istart = nsave;
         }
         for (int i=istart; i<nsteps; i++) {
            if (pass == 0 && i == nsave) {
               mat.getState (state);
            }
            double t0 = i*h;
            double t1 = t0+h;
            double a = activation (t1);
            mat.advanceState (t0, t1);
            mat.computeF (length (X, gamma, t0), 0, 0, a);
            fout[i] = mat.computeF (length (X, gamma, t1), 0, 0, a);
         }
      }
      for (int i=nsave; i<nsteps; i++) {
         if (fa[i] != fb[i]) {
            throw new TestException (
               "restored force at step "+i+" = "+fb[i]+", expected "+fa[i]);
         }
      }
   }

   /**
    * Checks the steady-state force-velocity behavior: force decreases with
    * shortening velocity and increases with lengthening velocity.
    */
   public void testForceVelocity() {
      HuxleyAxialMuscle mat = new HuxleyAxialMuscle();
      double X = mat.getOptFibreLength();
      double gamma = mat.getStrainScale();
      double h = 1e-4;
      double tend = 1.0;
      double Fmax = mat.getMaxIsoForce();
      // velocities in units of gamma*f1, from fast lengthening to fast
      // shortening
      double[] vels = new double[] { 0.5, 0.1, 0.0, -0.05, -0.2, -0.5, -1.0 };
      double fprev = Double.POSITIVE_INFINITY;
      for (double v : vels) {
         double ldot = v*gamma*mat.getAttachmentRate();
         mat.setIsometricSteadyState (1.0);
         double F = mat.computeF (X, ldot, 0, 1.0);
         int nsteps = (int)Math.round (tend/h);
         for (int i=0; i<nsteps; i++) {
            double t0 = i*h;
            double t1 = t0+h;
            mat.advanceState (t0, t1);
            mat.computeF (X + ldot*t0, ldot, 0, 1.0);
            F = mat.computeF (X + ldot*t1, ldot, 0, 1.0);
         }
         if (v == 0) {
            checkEquals ("isometric force", F, Fmax, 1e-8*Fmax);
         }
         if (F >= fprev) {
            throw new TestException (
               "force "+F+" at velocity "+v+
               " not less than force at higher velocity "+fprev);
         }
         if (v > 0 && F <= Fmax) {
            throw new TestException (
               "lengthening force "+F+" not greater than isometric force");
         }
         fprev = F;
      }
   }

   /**
    * Checks that scaleDistance() and scaleMass() scale forces and stiffness
    * consistently, including the state.
    */
   public void testScaling() {
      HuxleyAxialMuscle mat0 = new HuxleyAxialMuscle (0.2, 1.5e-3, 40.0);
      double X = mat0.getOptFibreLength();
      double gamma = mat0.getStrainScale();
      double s = 1000.0; // e.g., meters to millimeters
      double h = 1e-3;
      mat0.setIsometricSteadyState (0.5);
      mat0.computeF (X, 0, 0, 0.5);
      for (int i=0; i<50; i++) {
         mat0.advanceState (i*h, (i+1)*h);
         mat0.computeF (length (X, gamma, i*h), 0, 0, 0.5);
      }
      HuxleyAxialMuscle mat1 = mat0.clone();
      mat1.scaleDistance (s);
      checkEquals ("scaled strain scale", mat1.getStrainScale(), s*gamma, 1e-12*s*gamma);
      HuxleyAxialMuscle mat2 = mat0.clone();
      double sm = 0.001; // e.g., kg to grams
      mat2.scaleMass (sm);
      for (int i=50; i<100; i++) {
         double t0 = i*h;
         double t1 = t0+h;
         double l0 = length (X, gamma, t0);
         double l1 = length (X, gamma, t1);
         double ldot = lengthDot (X, gamma, t1);
         mat0.advanceState (t0, t1);
         mat1.advanceState (t0, t1);
         mat2.advanceState (t0, t1);
         mat0.computeF (l0, ldot, 0, 0.5);
         mat1.computeF (s*l0, s*ldot, 0, 0.5);
         mat2.computeF (l0, ldot, 0, 0.5);
         double F0 = mat0.computeF (l1, ldot, 0, 0.5);
         double F1 = mat1.computeF (s*l1, s*ldot, 0, 0.5);
         double F2 = mat2.computeF (l1, ldot, 0, 0.5);
         double K0 = mat0.computeDFdl (l1, ldot, 0, 0.5);
         double K1 = mat1.computeDFdl (s*l1, s*ldot, 0, 0.5);
         double K2 = mat2.computeDFdl (l1, ldot, 0, 0.5);
         double ftol = 1e-9*mat0.getMaxIsoForce();
         double ktol = 1e-9*mat0.getMaxIsoStiffness();
         checkEquals ("distance scaled force", F1, s*F0, s*ftol);
         checkEquals ("distance scaled stiffness", K1, K0, ktol);
         checkEquals ("mass scaled force", F2, sm*F0, sm*ftol);
         checkEquals ("mass scaled stiffness", K2, sm*K0, sm*ktol);
      }
   }

   /**
    * Checks that clones have independent state.
    */
   public void testClone() {
      HuxleyAxialMuscle mat0 = new HuxleyAxialMuscle();
      double X = mat0.getOptFibreLength();
      mat0.setIsometricSteadyState (0.8);
      mat0.computeF (X, 0, 0, 0.8);
      HuxleyAxialMuscle mat1 = mat0.clone();
      check ("clone not equal", mat1.equals (mat0));
      double[] Q0 = new double[3];
      mat0.getMoments().get (Q0);
      for (int i=0; i<10; i++) {
         mat1.advanceState (i*1e-3, (i+1)*1e-3);
         mat1.computeF (X + 1e-4*i, 0, 0, 0.1);
      }
      double[] Q0chk = new double[3];
      mat0.getMoments().get (Q0chk);
      checkEquals ("original moments after stepping clone", Q0chk, Q0);
      check (
         "clone moments unchanged after stepping",
         !mat1.getMoments().equals (mat0.getMoments()));
      mat1.setAttachmentRate (20);
      check ("clone with different property equal", !mat1.equals (mat0));
   }

   public void test() {
      testKineticRates();
      testNarrowPartialMoments();
      testIsometricSteadyState();
      testShortRangeStiffness();
      testDerivatives();
      testIntegrationAccuracy();
      testStateRepeatability();
      testForceVelocity();
      testScaling();
      testClone();
   }

   public static void main (String[] args) {
      HuxleyAxialMuscleTest tester = new HuxleyAxialMuscleTest();
      tester.runtest();
   }
}
