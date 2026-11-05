package artisynth.core.materials;

import java.io.*;
import java.util.*;

import maspack.interpolation.CubicHermiteSpline1d;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import maspack.properties.*;
import maspack.function.*;
import maspack.numerics.*;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Base class for point-to-point muscle materials that implement a pennated
 * muscle and tendon in series, and which solve for the muscle length at each
 * time step to ensure that the muscle and tendon forces are equal.
 */
public abstract class EquilibriumAxialMuscle extends AxialMuscleMaterialBase 
   implements HasNumericState {

   // Useful constants:

   // root solving tolerance for computing muscle lengths
   protected static double GTOL = 2.5e-10; 
   protected static double RTOD = 180/Math.PI; // radians to degrees
   protected static double DTOR = Math.PI/180; // degrees to radians
   protected static double INF = Double.POSITIVE_INFINITY;

   // Parameters for controlling the solve:

   public static boolean useNewtonSolver = false;
   public static boolean useFindLm = true;
   // upper clipping limit for the muscle velocity ratio Rm
   public static double maxRm = 10.0;

   // Generic muscle properties

   // Maximum isometric force that the fibers can generate
   public static double DEFAULT_MAX_ISO_FORCE = 1000;
   protected double myMaxIsoForce = DEFAULT_MAX_ISO_FORCE;

   // Optimal length of the muscle fibers
   public static double DEFAULT_OPT_FIBRE_LENGTH = 0.1;
   protected double myOptFibreLength = DEFAULT_OPT_FIBRE_LENGTH;

   // Resting length of the tendon
   public static double DEFAULT_TENDON_SLACK_LENGTH = 0.2;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;

   // Angle between tendon and fibers at optimal fiber length, in radians
   public static double DEFAULT_OPT_PENNATION_ANGLE = 0.0;
   protected double myOptPennationAngle = DEFAULT_OPT_PENNATION_ANGLE;

   // Maximum contraction velocity of the fibers, in optimal fiberlengths/second
   public static double DEFAULT_MAX_CONTRACTION_VELOCITY = 10;
   protected double myMaxContractionVelocity = DEFAULT_MAX_CONTRACTION_VELOCITY;

   // Damping along the muscle fibre, with respect to the normalized muscle
   // fibre velocity (fibre velocity divided by maxContractionVelocity)
   public static double DEFAULT_FIBRE_DAMPING = 0;
   protected double myDamping = DEFAULT_FIBRE_DAMPING;

   // Compute muscle dynamics ignoring tendon compliance (rigid tendon)
   public static boolean DEFAULT_RIGID_TENDON = false;
   protected boolean myRigidTendonP = DEFAULT_RIGID_TENDON;

   // Compute muscle dynamics ignoring the force velocity curve
   public static boolean DEFAULT_IGNORE_FORCE_VELOCITY = false;
   protected boolean myIgnoreForceVelocity = DEFAULT_IGNORE_FORCE_VELOCITY;

   // Estimate muscle velocity at each time step from the length time
   // derivative 'ldot'
   public static boolean DEFAULT_COMPUTE_LMDOT_FROM_LDOT = false;
   protected boolean myComputeLmDotFromLDot = DEFAULT_COMPUTE_LMDOT_FROM_LDOT;

   // Transient quasi-static response mode, set by setQuasiStaticResponse().
   // When true, the muscle length is computed as when computeLmDotFromLDot is
   // true, and the step start solution is not updated.
   protected boolean myQuasiStaticResponse = false;

   // "height" of muscle, precomputed from opt fibre length and pennation angle
   protected double myHeight;

   // The class keeps track of the both muscle length and the overall length
   // for which the muscle length was derived. One reason we keep both is to
   // compute the tendon length from myLength - myMuscleLength.
   public static double DEFAULT_MUSCLE_LENGTH = 0;
   double myMuscleLength = DEFAULT_MUSCLE_LENGTH;
   double myLength;

   // ======== Attributes used by the compliant tendon case ========

   // Muscle length determined by the previous time step (or before simulation
   // starts), and used to estimate muscle velocity estimate if
   // computeLmDotFromLDot is {@code false}.
   double myMuscleLengthPrev;
   double myH; // most recent time step size

   // Muscle length and associated overall length for the solve performed
   // whenever myStepValid is false. This will be the solve performed either by
   // the integrator, or before simulation starts. We keep track of this
   // because any subsequent solves that happen *after* the integrator has
   // updated positions needs to use myMuscleLengthStep in place of
   // myMuscleLengthPrev in order to maintain an accurate muscle velocity
   // estimate if computeLmDotFromLDot is {@code false}.
   double myMuscleLengthStep;
   double myLengthStep;
   // whether or not 'step' quantities are valid. 
   boolean myStepValid = false;

   // cached quantities computed during muscle length computation
   boolean myCacheValid = false; // true if cached quantities are valid
   double myDm; // derivative of normalized muscle force wrt overall length
   double myDv; // derivative of normalized muscle force wrt overall velocity
   double myDt; // derivative of normalized tendon force wrt overall length
   double myMuscleVel; // muscle velocity

   // The muscle velocity ratio Rm is nominally Dt/(Dt+Dm), with clipping
   // applied to keep it well conditioned, used to estimate muscle velocity vm
   // from vm = Rm ldot. It is used to when computeLmDotFromLDot is true,
   // and three versions are kept:
   double myRm;           // used by the most recent solve (Rm0 in the notes)
   double myRmPrev = 1.0; // for the previous solve step
   double myRmStep;       // for integrator solve, or before simulation starts
   boolean myMuscleLengthValid = false;

   // ======== end compliant tendon attributes ========
   
   int myStateVersion; // changes when we switch to/from rigidTendoon 
   
   public int getStateVersion() {
      return myStateVersion;
   }

   public static PropertyList myProps =
      new PropertyList(EquilibriumAxialMuscle.class,
                       AxialMuscleMaterialBase.class);

   static {
      myProps.add (
         "maxIsoForce",
         "maximum isometric force", DEFAULT_MAX_ISO_FORCE);
      myProps.add (
         "optFibreLength",
         "optimal fiber length", DEFAULT_OPT_FIBRE_LENGTH);
      myProps.add (
         "tendonSlackLength",
         "resting length of the tendon", DEFAULT_TENDON_SLACK_LENGTH);
      myProps.add (
         "optPennationAngle",
         "pennation angle at optimal length", DEFAULT_OPT_PENNATION_ANGLE);
      myProps.add (
         "maxContractionVelocity",
         "maximum fiber contraction velocity", DEFAULT_MAX_CONTRACTION_VELOCITY);
      myProps.add (
         "fibreDamping",
         "damping term for the muscle component", DEFAULT_FIBRE_DAMPING);
      myProps.add(
         "rigidTendon hasRigidTendon",
         "if true, assume that the tendon is rigid",
         DEFAULT_RIGID_TENDON);
      myProps.add(
         "ignoreForceVelocity",
         "if true, ignore the force velocity curve",
         DEFAULT_IGNORE_FORCE_VELOCITY);
      myProps.add(
         "computeLmDotFromLDot",
         "if true, use ldot to compute lmdot",
         DEFAULT_COMPUTE_LMDOT_FROM_LDOT);
      myProps.addReadOnly (
         "length",
         "length of the combined tendon/muscle");
      myProps.addReadOnly (
         "muscleLength",
         "length of the muscle portion of the tendon/muscle");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Computes the active force length curve.
    *
    * @param ln normalized fibre length
    * @return force length curve scale factor
    */
   protected abstract double computeActiveForceLength (double ln);

   /**
    * Computes the derivate of the active force length curve.
    *
    * @param ln normalized fibre length
    * @return force length curve derivative at ln
    */
   protected abstract double computeActiveForceLengthDeriv (double ln);

   /**
    * Computes the passive force length curve.
    *
    * @param ln normalized fibre length
    * @return normalized passive force
    */
   protected abstract double computePassiveForceLength (double ln);

   /**
    * Computes the derivative of the passive force length curve.
    *
    * @param ln normalized fibre length
    * @return passive force length curve derivative at ln
    */
   protected abstract double computePassiveForceLengthDeriv (double ln);

   /**
    * Computes the force velocity curve.
    *
    * @param normalized muscle fibre velocity
    * @param activation
    * @return force velocity scale factor
    */
   protected abstract double computeForceVelocity (double vn, double a);

   /**
    * Computes the derivative of the force velocity curve with respect to the
    * normalized fibre velocity.
    *
    * @param normalized muscle fibre velocity
    * @param activation
    * @return force velocity curve derivative at vn
    */
   protected abstract double computeForceVelocityDeriv (double vn, double a);

   /**
    * Computes the tendon force curve.
    *
    * @param tln normalized tendon length
    * @return normalized tendon force
    */
   protected abstract double computeTendonForce (double tln);

   /**
    * Computes the derivative of the tendon force curve.
    *
    * @param tln normalized tendon length
    * @return tendon force curve derivative at tln
    */
   protected abstract double computeTendonForceDeriv (double tln);   

   protected EquilibriumAxialMuscle() {
      myHeight = DEFAULT_OPT_FIBRE_LENGTH*Math.sin(DEFAULT_OPT_PENNATION_ANGLE);
   }

   // accessors for generic muscle properties

   public double getMaxIsoForce() {
      return myMaxIsoForce;
   }

   public void setMaxIsoForce (double fmax) {
      myMaxIsoForce = fmax;
   }

   public double getOptFibreLength() {
      return myOptFibreLength;
   }

   public void setOptFibreLength (double l) {
      myOptFibreLength = l;
      myHeight = myOptFibreLength*Math.sin(myOptPennationAngle);
   }

   public double getTendonSlackLength() {
      return myTendonSlackLength;
   }

   public void setTendonSlackLength (double l) {
      myTendonSlackLength = l;
   }

   public double getOptPennationAngle() {
      return myOptPennationAngle;
   }
   
   /**
    * Queries the height of the muscle, as determined by the
    * optimum pennation angle and optimum fibre length.  
    * 
    * @return height of the muscle
    */
   public double getHeight() {
      return myHeight;
   }
   
   /**
    * Computes the current pennation angle from the current
    * muscle length.
    * 
    * @return current pennation angle
    */
   public double getPennationAngle() {
      double H = myHeight;
      double lm = getMuscleLength();
      double ca; // cos(alpha)
      if (H == 0) {
         ca = 1.0;
      }
      else {
         ca = lm/Math.sqrt (H*H + lm*lm);
      }
      return Math.acos (ca);
   }

   public void setOptPennationAngle (double ang) {
      myOptPennationAngle = ang;
      myHeight = myOptFibreLength*Math.sin(myOptPennationAngle);
   }

   public double getMaxContractionVelocity() {
      return myMaxContractionVelocity;
   }

   public void setMaxContractionVelocity (double maxv) {
      myMaxContractionVelocity = maxv;
   }

   public double getFibreDamping() {
      return myDamping;
   }

   public void setFibreDamping (double d) {
      myDamping = d;
   }

   /**
    * Covenience method that returns the optimial muscle length, defined as
    * <pre>
    * cos (optPennationAngle) * optFibreLength
    * </pre>
    *
    * @return optimial muscle length
    */
   public double getOptMuscleLength() {
      return Math.cos (myOptPennationAngle) * myOptFibreLength;
   }

   /**
    * @deprecated Use {@link #hasRigidTendon} instead.
    */
   public boolean getIgnoreTendonCompliance () {
      return hasRigidTendon();
   }

   /**
    * @deprecated Use {@link #setRigidTendon} instead.
    */
   public void setIgnoreTendonCompliance (boolean enable) {
      setRigidTendon (enable);
   }

   /**
    * Queries whether the tendon for this muscle material is rigid.
    *
    * @return {@code true} if the tendon is rigid
    */
   public boolean hasRigidTendon() {
      return myRigidTendonP;
   }

   /**
    * Set whether the tendon for this muscle material is rigid.
    *
    * @param enable if {@code true}, sets the tendon rigid
    */
   public void setRigidTendon (boolean enable) {
      if (enable != myRigidTendonP) {
         myRigidTendonP = enable;
         myStateVersion++;
         notifyHostOfStateChange("rigidTendon");
      }
   }

   public boolean getIgnoreForceVelocity () {
      return myIgnoreForceVelocity;
   }

   public void setIgnoreForceVelocity (boolean enable) {
      myIgnoreForceVelocity = enable;
   }

   public boolean getComputeLmDotFromLDot() {
      return myComputeLmDotFromLDot;
   }

   public void setComputeLmDotFromLDot (boolean enable) {
      myComputeLmDotFromLDot = enable;
   }

   /**
    * {@inheritDoc}
    *
    * <p>For this material, quasi-static response means that the muscle
    * length is computed with the muscle velocity estimated from the length
    * time derivative, as when {@link #getComputeLmDotFromLDot
    * computeLmDotFromLDot} is {@code true}. This removes the contraction lag
    * of the compliant tendon model, under which the one-step force response
    * to excitation is attenuated by approximately {@code h/(tau+h)}, where
    * {@code h} is the step size and {@code tau} is the contraction time
    * constant.
    *
    * <p>This feature is still experimental.
    */
   @Override
   public void setQuasiStaticResponse (boolean enable) {
      if (enable != myQuasiStaticResponse) {
         myQuasiStaticResponse = enable;
         // cached quantities depend on the mode
         myCacheValid = false;
      }
   }

   /**
    * Queries whether quasi-static response is enabled, as described in
    * {@link #setQuasiStaticResponse}.
    *
    * @return {@code true} if quasi-static response is enabled
    */
   public boolean getQuasiStaticResponse() {
      return myQuasiStaticResponse;
   }

   /**
    * Returns true if the muscle velocity is estimated from the length time
    * derivative, either because computeLmDotFromLDot is set or because
    * quasi-static response is enabled.
    */
   private boolean estimateVmFromLDot() {
      return myComputeLmDotFromLDot || myQuasiStaticResponse;
   }

   // Muscle and tendon length accessors

   public double getTendonLength() {
      if (myRigidTendonP) {
         return myTendonSlackLength;
      }
      else {
         return myLength - myMuscleLength;
      }
   }

   public double getLength() {
      return myLength;
   }
   
   public void setLength (double l) {
      myLength = l;
   }
   
   public double getMuscleLength() {
      return myMuscleLength;
   }

   public void setMuscleLength (double l) {
      myMuscleLength = l;
      myMuscleLengthPrev = l;
   }

   public double getNormalizedFibreVelocity() {
      double lm = myMuscleLength;
      double vm = myMuscleVel;
      double H = myHeight;
      double Vmax = myMaxContractionVelocity;
      double lo = myOptFibreLength;
      double ca; // cos(alpha)
      if (H == 0) {
         ca = 1.0;
      }
      else {
         double lf = Math.sqrt (H*H + lm*lm);
         ca = lm/lf;
      }
      // normalized muscle velocity:
      double vn = 0;
      if (vm != 0) {
         vn = vm*ca/(lo*Vmax); 
      }
      return vn;
   }

   // Compute methods

   protected double sqr (double x) {
      return x*x;
   }

   /**
    * Computes the muscle length corresponding to a given fibre length.
    * If the fibre length is less that the muscle height, returns 0.
    *
    * @param lf fibre length
    * @return corresponding muscle length
    */
   public double fibreToMuscleLength (double lf) {
      if (lf < myHeight) {
         return 0;
      }
      else {
         return Math.sqrt(lf*lf - myHeight*myHeight);
      }
   }

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      if (myRigidTendonP) {
         // rigid tendon case

         double H = myHeight;
         double Vmax = myMaxContractionVelocity;
         double lo = myOptFibreLength;

         double lm = l-myTendonSlackLength; // lm = cos(alpha)*lf
         myMuscleLength = lm;
         myLength = l; // not needed for computation, but added for completeness

         double lf; // fibre length
         double ca; // cos(alpha)
         if (H == 0) {
            lf = lm;
            ca = 1.0;
         }
         else {
            lf = Math.sqrt (H*H + lm*lm); // muscle length
            ca = lm/lf; // ca = cos(alpha)
         }

         if (ca < 0) {
            // XXX does this happen?
            return 0;
         }

         double ln = lf/lo; // normalized muscle length
         // normalized muscle velocity:
         double vn = ldot*ca/(lo*Vmax); 
         double fa = computeActiveForceLength (ln);
         double fp = computePassiveForceLength (ln);
         double fm;
         if (myIgnoreForceVelocity) {
            fm = myMaxIsoForce*(fa*excitation+fp+myDamping*vn)*ca;
         }
         else {
            double fv = computeForceVelocity (vn, excitation);
            fm = myMaxIsoForce*(fa*fv*excitation+fp+myDamping*vn)*ca;
         }
         return fm;
      }
      else {
         // Compute length regardless of whether the cache is valid. This
         // ensures that length will be recomputed if updateForces() is called
         // multiple times the same time step (e.g., by the inverse
         // controller), without having to call advanceState again.
         updateMuscleLength (l, ldot, excitation);
         return getTendonForce (myLength-myMuscleLength);
      }
   }

   public double computePassiveF (
      double l, double ldot, double l0, double excitation) {

      if (myRigidTendonP) {
         return computeF (l, ldot, l0, 0);
      }
      else {
         if (!myCacheValid || l != myLength) {
            updateMuscleLength (l, ldot, excitation);
         }
         // solve for passive force given muscle length
         double lm = myMuscleLength;
         double vm = myMuscleVel;
         double H = myHeight;
         double Vmax = myMaxContractionVelocity;
         double lo = myOptFibreLength;

         double lf; // fibre length
         double ca; // cos(alpha)
         if (H == 0) {
            lf = lm;
            ca = 1.0;
         }
         else {
            lf = Math.sqrt (H*H + lm*lm);
            ca = lm/lf;
         }
         double ln = lf/lo; // normalized muscle length
         // normalized muscle velocity:
         double vn = 0;
         if (vm != 0) {
            vn = vm*ca/(lo*Vmax); 
         }
         return myMaxIsoForce*(
            ca*(computePassiveForceLength(ln) + myDamping*vn));
      }
   }

   double getTendonForce (double lt) {
      double ltn = lt/getTendonSlackLength();
      return myMaxIsoForce*computeTendonForce (ltn);
   }

   private class GFunctionWithVmFromLm implements Diff1Function1x1 {
      double myL;
      double myLDot;
      double myA;
      double myH;
      
      GFunctionWithVmFromLm (
         double l, double ldot, double h, double a) {
         myL = l;
         myLDot = ldot;
         myA = a;
         myH = h;
      }
      
      public double eval (double lm) {
         double vm = computeVmFromLm (myL, myLDot, lm);
         return computeG (null, myL, lm, vm, myA, myH);
      }

      public double eval (DoubleHolder deriv, double lm) {
         double vm = computeVmFromLm (myL, myLDot, lm);
         return computeG (deriv, myL, lm, vm, myA, myH);
      }
   }      

   private class GFunctionWithVmConstant implements Diff1Function1x1 {
      double myL;
      double myA;
      double myVm;
      
      GFunctionWithVmConstant (
         double l, double vm, double a) {
         myL = l;
         myVm = vm;
         myA = a;
      }
      
      public double eval (double lm) {
         return computeG (null, myL, lm, myVm, myA, -1); 
      }

      public double eval (DoubleHolder deriv, double lm) {
         return computeG (deriv, myL, lm, myVm, myA, -1); 
      }
   }

   /**
    * Returns the ratio vm/ldot used to estimate the muscle velocity from
    * ldot, given the tendon length lt and the current values of Dm and Dt.
    */
   double computeRm (double lt) {
      double T = myTendonSlackLength;
      double eps = T*1e-8;
      if (lt <= T+eps) {
         // slack tendon: muscle takes all of ldot
         return 1.0;
      }
      else {
         return computeClippedRm();
      }
   }

   /**
    * Returns Dt/(Dm+Dt), computed from the current values of Dm and Dt and
    * clipped to [0, maxRm]. Dm+Dt <= 0 gives maxRm. Dm+Dt > 0 and Dm < 0
    * gives a ratio above 1, which blows up as Dm+Dt -> 0. A slack tendon
    * (Dt = 0) gives 0.
    */
   double computeClippedRm() {
      double denom = myDm + myDt;
      if (!(denom > 0)) {
         return maxRm;
      }
      else {
         return Math.min (myDt/denom, maxRm);
      }
   }

   public double initializeLmPrevFromLdot (double l, double ldot, double a) {
      int numIters = 3;
      double vm = 0;
      double lmprev = 0;
      double lprev = l - ldot*myH;
      for (int k=0; k<numIters; k++) {
         lmprev = computeLmWithConstantVm (lprev, vm, a);
         myMuscleLengthPrev = lmprev;
         computeDerivs (lprev, lmprev, vm, a);     
         if (k < numIters-1) {
            vm = computeRm (lprev-lmprev)*ldot;
         }
      }
      myRmPrev = computeRm (lprev-lmprev);
      return lmprev;
   }

   public double computeLmWithConstantVm (double l, double vm, double a) {

      double ol = getOptFibreLength();
      double Vmax = getMaxContractionVelocity();

      double vnx = vm/(ol*Vmax); // approximate normalize vel assuming c = 1
      if (Math.abs (vnx) < GTOL) {
         // clip very small velocities to zero
         vm = 0.0;
      }

      Diff1Function1x1 gfxn = new GFunctionWithVmConstant (l, vm, a);
      return findLm (gfxn, l, a, -1);
   }

   public double computeLmWithVmFromLm (
      double l, double ldot, double a, double h) {
      
      Diff1Function1x1 gfxn = new GFunctionWithVmFromLm (l, ldot, h, a);
      return findLm (gfxn, l, a, h);
   }

   private double findRoot (
      Diff1Function1x1 gfxn, double lmlo, double gl0, double lmhi, double ghi,
      double tol) {
      
      if (useNewtonSolver) {
         return NewtonRootSolver.findRoot (
            gfxn, lmlo, gl0, lmhi, ghi, tol, GTOL);
      }
      else {
         return BrentRootSolver.findRoot (
            gfxn, lmlo, gl0, lmhi, ghi, tol, GTOL);
      }
   }

   private double findLm (
      Diff1Function1x1 gfxn, double l, double a, double h) {

      double T = getTendonSlackLength();
      double ol = getOptFibreLength();
      double tol = 1e-8*(T+ol);
      //System.out.println ("FIND");

      double g0 = gfxn.eval(0);
      if (g0 >= -GTOL) {
         // We either have a root, or g0 > GTOL. The latter can only occur with
         // 0 pennation angle and constant muscle velocity > 0 and not enough
         // countering tendon force. Doesn't happen often. In either case,
         // return lm = 0.
         return 0;
      }

      if (l > T) {
         // Usual case. Tendon will be active; try to find lm in the range [0,
         // l-T] such that the muscle force balances the tendon force.
         double gt = gfxn.eval (l-T);
         if (gt >= -GTOL) {
            // usual case: root bracketed in [0, l-T]
            return findRoot (gfxn, 0, g0, l-T, gt, tol);
         }
         else {
            // g < -GTOL at l-T. Can only be true if there is damping.
            // Move the lower bound up from 0 to l-T
            return l-T;
         }
      }
      return 0;
   }

   private double computeVmFromLm (double l, double ldot, double lm) {
      return (lm-myMuscleLengthPrev)/myH;
   }

   void updateMuscleLength (double l, double ldot, double excitation) {
      if (myStepValid && (l != myLengthStep || myQuasiStaticResponse)) {
         // Solve at a length other than that of the step start (e.g., a
         // force query made after the position update). This is relative to
         // the step start solution, and must not replace it.
         double lmprev = myMuscleLengthPrev;
         double ratio = myRmPrev;
         myMuscleLengthPrev = myMuscleLengthStep;
         myRmPrev = myRmStep;
         doUpdateMuscleLength (l, ldot, excitation);
         myMuscleLengthPrev = lmprev;
         myRmPrev = ratio;
      }
      else if (myQuasiStaticResponse) {
         // Quasi-static solves must not replace the step start solution,
         // which provides the previous values for the next step
         doUpdateMuscleLength (l, ldot, excitation);
      }
      else {
         doUpdateMuscleLength (l, ldot, excitation);
         myLengthStep = l;
         myMuscleLengthStep = myMuscleLength;
         myRmStep = computeRm (l-myMuscleLength);
         myStepValid = true;
      }
      myCacheValid = true;
   }

   void doUpdateMuscleLength (double l, double ldot, double excitation) {
      // myH <= 0 means no step has been taken yet (the constructor sets it
      // to -1), so defer initialization until there is a step size
      if (!myMuscleLengthValid && myH > 0) {
         initializeLmPrevFromLdot (l, ldot, excitation);
         myMuscleLengthValid = true;
      }
      double lm;
      double vm;
      //double deltalm = estimateDeltaLm (l-lprev, ldot, lmprev);
      if (estimateVmFromLDot() || myH <= 0) {
         // Before the first step (myH <= 0), vm = (lm-lmprev)/h is undefined,
         // so estimate vm from ldot, as done for initialization.
         // store dvm/dldot for computeDFdldot(). Use vm = 0 if there is
         // no previous muscle length.
         myRm = (myMuscleLengthPrev == 0 ? 0 : myRmPrev);
         vm = myRm*ldot;
         lm = computeLmWithConstantVm (l, vm, excitation);
         computeDerivs (l, lm, vm, excitation);
      }
      else {
         lm = computeLmWithVmFromLm (l, ldot, excitation, myH);
         vm = computeVmFromLm (l, ldot, lm);
         computeDerivs (l, lm, vm, excitation);
      }
      myLength = l;
      myMuscleLength = lm;
      myMuscleVel = vm;
   }

   void computeDerivs (double l, double lm, double vm, double a) {

      double H = myHeight;
      double Vmax = myMaxContractionVelocity;
      double lo = myOptFibreLength;

      double lf; // fibre length
      double ca; // cos(alpha)
      double dcdlm; // d c / d lm
      if (H == 0) {
         lf = lm;
         ca = 1.0;
         dcdlm = 0;
      }
      else {
         lf = Math.sqrt (H*H + lm*lm);
         ca = lm/lf;
         dcdlm = sqr(H/lf)/lf;
      }
      double ln = lf/lo; // normalized muscle length
      // normalized muscle velocity:
      double vn = vm*ca/(lo*Vmax);

      double fa = computeActiveForceLength (ln);
      double fp = computePassiveForceLength (ln);
      double fv = 1.0;

      if (!myIgnoreForceVelocity && vn != 0) {
         fv = computeForceVelocity (vn, a);
      }
      double ff = fa*fv*a+fp+myDamping*vn;

      double dln = ca/lo;
      double dfa = computeActiveForceLengthDeriv(ln)*dln;
      double dfp = computePassiveForceLengthDeriv(ln)*dln;
      double vterm = myDamping;
      if (!myIgnoreForceVelocity) {
         // include the slope at vn = 0 too, as computeFm() does; otherwise
         // the damping is underestimated whenever vm is exactly 0, e.g. in
         // static holds with computeLmDotFromLDot
         double dfv = computeForceVelocityDeriv (vn, a);
         vterm += a*fa*dfv;
      }
      double dvn = dcdlm*vm/(lo*Vmax);
      myDm = ff*dcdlm + (a*dfa*fv + dfp)*ca + vterm*dvn*ca;
      myDv = ca*ca*vterm/(lo*Vmax);
      double ltn = (l-lm)/myTendonSlackLength;
      myDt = computeDFt (ltn);
   }

   double computeFm (
      DoubleHolder dF, double lm, double vm, double a, double h) {

      double H = myHeight;
      double Vmax = myMaxContractionVelocity;
      double lo = myOptFibreLength;

      double lf; // fibre length
      double ca; // cos(alpha)
      double dcdlm; // d c / d lm
      if (H == 0) {
         lf = lm;
         ca = 1.0;
         dcdlm = 0;
      }
      else {
         lf = Math.sqrt (H*H + lm*lm);
         ca = lm/lf;
         dcdlm = sqr(H/lf)/lf;
      }
      double ln = lf/lo; // normalized muscle length
      // normalized muscle velocity:
      double vn = 0;
      if (vm != 0) {
         vn = vm*ca/(lo*Vmax); 
      }

      double fa = computeActiveForceLength (ln);
      double fp = computePassiveForceLength (ln);
      double fv = 1.0;

      if (!myIgnoreForceVelocity && vn != 0) {
         if (vn <= -1) {
            fv = 0;
         }
         else {
            fv = computeForceVelocity (vn, a);
         }
      }
      double ff = fa*fv*a+fp+myDamping*vn;
      if (dF != null) {
         double dln = ca/lo;
         double dfa = computeActiveForceLengthDeriv(ln)*dln;
         double dfp = computePassiveForceLengthDeriv(ln)*dln;
         double vterm = myDamping;
         if (!myIgnoreForceVelocity) {
            double dfv = computeForceVelocityDeriv (vn, a);
            vterm += a*fa*dfv;
         }
         double dvn = 0;
         if (vm != 0) {
            dvn = dcdlm*vm/(lo*Vmax);
         }
         double Dm = ff*dcdlm + (a*dfa*fv + dfp)*ca + vterm*dvn*ca;
         if (h > 0 && vterm != 0) {
            double Dv = ca*ca*vterm/(lo*Vmax);
            dF.value = Dm + Dv/h;
         }
         else {
            dF.value = Dm;
         }
      }
      return ff*ca;
   }

   double computeFt (double ltn) {
      return computeTendonForce (ltn);
   }

   double computeDFt (double ltn) {
      return computeTendonForceDeriv (ltn)/myTendonSlackLength;
   }

   public double computeG (
      DoubleHolder dG, double l, double lm,
      double vm, double a, double h) {

      double fm = computeFm (dG, lm, vm, a, h);
      double ltn = (l-lm)/myTendonSlackLength;
      if (dG != null) {
         dG.value += computeDFt (ltn);
      }
      return fm - computeFt (ltn);
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      if (myRigidTendonP) {
         // rigid tendon case

         double H = myHeight;
         double Vmax = myMaxContractionVelocity;
         double lo = myOptFibreLength;

         double lm = l-myTendonSlackLength; // lm = cos(alpha)*lf
         myMuscleLength = lm;
         myLength = l; // not needed for computation, but added for completeness

         double lf; // fibre length
         double ca; // cos(alpha)
         double dcdlm; // d c / d lm
         if (H == 0) {
            lf = lm;
            ca = 1.0;
            dcdlm = 0;
         }
         else {
            lf = Math.sqrt (H*H + lm*lm);
            ca = lm/lf;
            dcdlm = sqr(H/lf)/lf;
         }
         double lfSqr = lf*lf;

         if (ca < 0) {
            return 0;
         }

         double ln = lf/lo; // normalized muscle length
         // normalized muscle velocity:
         double vn = ldot*ca/(lo*Vmax); 
         double fa = computeActiveForceLength (ln);
         double fp = computePassiveForceLength (ln);

         double dvn = ldot*dcdlm/(lo*Vmax);
         double dln = ca/lo;

         double dfa = computeActiveForceLengthDeriv(ln)*dln;
         double dfp = computePassiveForceLengthDeriv(ln)*dln;

         if (myIgnoreForceVelocity) {
            double fm = myMaxIsoForce*(fa*excitation+fp+myDamping*vn);
            return myMaxIsoForce*(
               dfa*excitation + dfp + myDamping*dvn)*ca + fm*dcdlm;
         }
         else {
            double fv = computeForceVelocity (vn, excitation);
            double dfv = computeForceVelocityDeriv(vn, excitation)*dvn;
            double fm = myMaxIsoForce*(fa*fv*excitation+fp+myDamping*vn);
            return myMaxIsoForce*(
               (dfa*fv+fa*dfv)*excitation + dfp + myDamping*dvn)*ca + fm*dcdlm;
         }
      }
      else {
         if (!myCacheValid || l != myLength) {
            updateMuscleLength (l, ldot, excitation);
         }
         // When vm = (lm-lmprev)/h, the force depends only on l, and the
         // total derivative dF/dl is split into stiffness Dm*Dt/denom and
         // damping (Dv*Dt/denom)/h, so only K + D/h is exact. K alone can
         // then be smaller than the quasi-static stiffness, or negative.
         double denom = computeStiffnessDenom();
         if (!(denom > 0)) {
            return 0;
         }
         else if (vmDependsOnLm()) {
            // K = Dm Dt/(Dm*+Dt), with Dm* = Dm + Dv/h, so that K + D/h is
            // the exact dF/dl
            return myMaxIsoForce*myDm*myDt/denom;
         }
         else {
            // K = Dm Rm
            return myMaxIsoForce*myDm*computeClippedRm();
         }
      }
   }

   /**
    * Returns true if muscle velocity is computed as {@code (lm-lmprev)/h},
    * which makes the muscle force depend on lm through the force velocity
    * curve and damping, as well as through the force length curves.
    */
   private boolean vmDependsOnLm() {
      // same condition as used to select the solve in doUpdateMuscleLength()
      return myH > 0 && !estimateVmFromLDot();
   }

   /**
    * Returns d(fm - ft)/d lm, normalized by maxIsoForce, for the most recent
    * muscle length solve. This is the denominator of the series stiffness and
    * damping of the muscle-tendon unit. When the muscle velocity is computed
    * from the change in lm, it includes Dv/h; without that term the
    * denominator is wrong and can reach 0 on the descending limb of the
    * active force length curve, where Dm < 0.
    */
   private double computeStiffnessDenom() {
      double denom = myDm + myDt;
      if (vmDependsOnLm()) {
         denom += myDv/myH;
      }
      return denom;
   }
   
   /**
    * {@inheritDoc}
    */
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      if (myRigidTendonP) {
         // rigid tendon case
         if (myDamping == 0 && myIgnoreForceVelocity) {
            return 0;
         }

         double H = myHeight;
         double Vmax = myMaxContractionVelocity;
         double lo = myOptFibreLength;

         double lm = l-myTendonSlackLength; // lm = cos(alpha)*lf
         myMuscleLength = lm;
         myLength = l; // not needed for computation, but added for completeness

         double lf; // fibre length
         double ca; // cos(alpha)
         if (H == 0) {
            lf = lm;
            ca = 1.0;
         }
         else {
            lf = Math.sqrt (H*H + lm*lm);
            ca = lm/lf;
         }
         
         if (ca < 0) {
            return 0;
         }

         double ftmp;
         if (myIgnoreForceVelocity) {
            ftmp = myDamping;
         }
         else {
            double ln = lf/lo; // normalized muscle length
            double fa = computeActiveForceLength (ln);
            // normalized muscle velocity:
            double vn = ldot*ca/(lo*Vmax); 
            ftmp = fa*computeForceVelocityDeriv(vn,excitation)*excitation + myDamping;
         }
         return (myMaxIsoForce*ftmp*ca*ca/(lo*Vmax));
      }
      else {
         if (!myCacheValid || l != myLength) {
            updateMuscleLength (l, ldot, excitation);
         }
         double denom = computeStiffnessDenom();
         if (!(denom > 0)) {
            return 0;
         }
         else if (vmDependsOnLm()) {
            // D = Dv Dt/(Dm*+Dt), with Dm* = Dm + Dv/h, assuming the
            // muscle velocity at the step start equals ldot
            return myMaxIsoForce*myDv*myDt/denom;
         }
         else {
            // D = Dv Rm Rm0, where Rm0 = myRm is the ratio used by the solve
            // to estimate vm from ldot
            return myMaxIsoForce*myDv*computeClippedRm()*myRm;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDFdldotZero() {
      return myDamping == 0 && myIgnoreForceVelocity;
   }

   // implementation of HasNumericState

   public boolean hasState() {
      return !myRigidTendonP;
   }

   public void setState (DataBuffer data) {
      if (myRigidTendonP) {
         // for completeness, store length and muscleLength for rigid tendons
         myLength = data.dget();
         myMuscleLength = data.dget();
      }
      else {
         myMuscleLengthPrev = data.dget();
         myRmPrev = data.dget();
         myH = data.dget();
         myMuscleLengthValid = data.zgetBool();
         myLengthStep = data.dget();
         myMuscleLengthStep = data.dget();
         myRmStep = data.dget();
         myStepValid = data.zgetBool();
         // cached values will be recomputed when needed
         myCacheValid = false;
         if (myStepValid) {
            myLength = myLengthStep;
            myMuscleLength = myMuscleLengthStep;
         }
      }
   }

   public void getState (DataBuffer data) {
      if (myRigidTendonP) {
         // for completeness, store length and muscleLength for rigid tendons
         data.dput(myLength);
         data.dput(myMuscleLength);
      }
      else {
         data.dput(myMuscleLengthPrev);
         data.dput(myRmPrev);
         data.dput(myH);
         data.zputBool (myMuscleLengthValid);
         data.dput(myLengthStep);
         data.dput(myMuscleLengthStep);
         data.dput(myRmStep);
         data.zputBool (myStepValid);
      }
   }

   public void advanceState (double t0, double t1) {
      if (!myRigidTendonP) {
         myH = (t1 - t0); // store step size for updating muscleLength later
         if (myStepValid) {
            // use the step start solution, since myMuscleLength may have
            // been computed at another length since. This was computed
            // (possibly before the first step), so it provides a valid
            // previous value.
            myMuscleLengthPrev = myMuscleLengthStep;
            myRmPrev = myRmStep;
            myMuscleLengthValid = true;
         }
         myStepValid = false;
         myCacheValid = false;
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean requiresAdvance() {
      return !myRigidTendonP;
   }  

   public void writeActiveForceLengthCurve (
      String fileName, double x0, double x1,
      int npnts, String fmtStr) throws IOException {
      Function1x1 fxn = new Function1x1() {
            public double eval(double ln) {
               return computeActiveForceLength (ln);
            }
         };
      FunctionUtils.writeValues (new File(fileName), fxn, x0, x1, npnts, fmtStr);
   }

   public void writePassiveForceLengthCurve (
      String fileName, double x0, double x1,
      int npnts, String fmtStr) throws IOException {
      Function1x1 fxn = new Function1x1() {
            public double eval(double ln) {
               return computePassiveForceLength (ln);
            }
         };
      FunctionUtils.writeValues (new File(fileName), fxn, x0, x1, npnts, fmtStr);
   }

   public void writeTendonForceLengthCurve (
      String fileName, double x0, double x1,
      int npnts, String fmtStr) throws IOException {
      Function1x1 fxn = new Function1x1() {
            public double eval(double ln) {
               return computeTendonForce (ln);
            }
         };
      FunctionUtils.writeValues (new File(fileName), fxn, x0, x1, npnts, fmtStr);
   }

   public void writeForceVelocityCurve (
      String fileName, double a, double x0, double x1,
      int npnts, String fmtStr) throws IOException {
      Function1x1 fxn = new Function1x1() {
            public double eval(double ln) {
               return computeForceVelocity (ln, a);
            }
         };
      FunctionUtils.writeValues (new File(fileName), fxn, x0, x1, npnts, fmtStr);
   }

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      myMaxIsoForce *= s;
      setOptFibreLength (s*myOptFibreLength);
      myTendonSlackLength *= s;
      // maxContractionVelocity (optimal fibre lengths/sec) and fibreDamping
      // (w.r.t. normalized velocity) are independent of distance units

      myMuscleLength *= s;
      myMuscleLengthPrev *= s;
      myMuscleVel *= s;
      myLength *= s;
      myLengthStep *= s;
      myMuscleLengthStep *= s;
      // cached derivatives of normalized force wrt length or velocity
      myDm /= s;
      myDt /= s;
      myDv /= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      myMaxIsoForce *= s;
      // fibreDamping is normalized by maxIsoForce, so is not scaled
   }
}

