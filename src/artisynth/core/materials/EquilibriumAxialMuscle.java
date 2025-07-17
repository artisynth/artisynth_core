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
   public boolean vmFromTendon = false;

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

   // cached quantities:
   protected double myHeight; // "height" of the pennated muscle
   // cached derivatives
   double myDm; // normalized muscle force wrt overall length
   double myDv; // normalized muscle force wrt overall length velocity
   double myDt; // normalized tendon force wrt overall length

   double myH; // most recent time step size

   // muscle length variables

   public static double DEFAULT_MUSCLE_LENGTH = 0;
   double myMuscleLength = DEFAULT_MUSCLE_LENGTH;
   double myLength;
   double myLengthPrev;
   boolean myLengthValid = false;
   double myMuscleLengthPrev;
   double myMuscleVel; // cached value of muscle velocity
   boolean myMuscleLengthValid = false;
   
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
         DEFAULT_IGNORE_FORCE_VELOCITY);
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
         // Compute length regardless of whether length is valid. This ensures
         // that length will be recomputed if updateForces() is called multiple
         // times the same time step (e.g., by the inverse controller). without
         // having to call adbvancestate again.
         //if (!myLengthValid) {
         updateMuscleLength (l, ldot, excitation);
         myLengthValid = true;
         //}
         return getTendonForce (myLength-myMuscleLength);
      }
   }

   public double computePassiveF (
      double l, double ldot, double l0, double excitation) {

      if (myRigidTendonP) {
         return computeF (l, ldot, l0, 0);
      }
      else {
         if (!myLengthValid) {
            updateMuscleLength (l, ldot, excitation);
            myLengthValid = true;
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

   public double computeVmFromLdot (double ldot) {
      double T = myTendonSlackLength;
      double eps = T*1e-8;
      double denom = myDm + myDt;
      double ltprev = myLengthPrev - myMuscleLengthPrev;
      if (myMuscleLengthPrev == 0) {
         return 0;
      }
      else if (ltprev <= T+eps || denom == 0) {
         return ldot;
      }
      else {
         return myDt*ldot/denom;
      }
   }

   public double initializeLmPrevFromLdot (double l, double ldot, double a) {
      int numIters = 3;
      double vm = 0;
      double lmprev = 0;
      double lprev = l - ldot*myH;
      for (int k=0; k<numIters; k++) {
         //vm = computeVmFromLdot (ldot);
         lmprev = computeLmWithConstantVm (lprev, vm, a);
         myMuscleLengthPrev = lmprev;
         myLengthPrev = lprev;
         computeDerivs (lprev, lmprev, vm, a);     
         if (k < numIters-1) {
            vm = computeVmFromLdot (ldot);
         }
      }
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
      if (vmFromTendon) {
         double ltprev = myLengthPrev-myMuscleLengthPrev;
         return ldot + (lm+ltprev-l)/myH;
      }
      else {
         return (lm-myMuscleLengthPrev)/myH;
      }
   }

   void updateMuscleLength (double l, double ldot, double excitation) {
      if (!myMuscleLengthValid && myH != 0) {
         initializeLmPrevFromLdot (l, ldot, excitation);
         myMuscleLengthValid = true;
      }
      double lm;
      double vm;
      //double deltalm = estimateDeltaLm (l-lprev, ldot, lmprev);
      if (myComputeLmDotFromLDot) {
         vm = computeVmFromLdot (ldot);
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
      if (!myIgnoreForceVelocity && vn != 0) {
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
         if (!myLengthValid) {
            updateMuscleLength (l, ldot, excitation);
            myLengthValid = true;
         }
         double denom = myDm + myDt;
         if (vmFromTendon) {
            denom += myDv/myH;
         }
         if (denom != 0) {
            return myMaxIsoForce*myDm*myDt/denom;
         }
         else {
            return 0;
         }
      }
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
         if (!myLengthValid) {
            updateMuscleLength (l, ldot, excitation);
            myLengthValid = true;
         }
         double denom = myDm + myDt;
         if (vmFromTendon) {
            denom += myDv/myH;
         }
         if (denom != 0) {
            return myMaxIsoForce*myDv*myDt/denom;
         }
         else {
            return 0;
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
      if (!myRigidTendonP) {
         myLength = data.dget();
         myMuscleLength = data.dget();
         myMuscleLengthPrev = data.dget();
         // XXX. Need to store Dt and Dm in case computeLmDotFromLdot is true.
         myDt = data.dget();
         myDm = data.dget();
         myH = data.dget();
         myMuscleLengthValid = data.zgetBool(); // XXX need this?
         myLengthValid = data.zgetBool(); // XXX need this?
      }
   }

   public void getState (DataBuffer data) {
      if (!myRigidTendonP) {
         data.dput(myLength);
         data.dput(myMuscleLength);
         data.dput(myMuscleLengthPrev);
         // XXX. Need to store Dt and Dm in case computeLmDotFromLdot is true.
         data.dput(myDt);
         data.dput(myDm);
         data.dput(myH);
         data.zputBool (myMuscleLengthValid); // XXX need this?
         data.zputBool (myLengthValid); // XXX need this?
      }
   }

   public void advanceState (double t0, double t1) {
      if (!myRigidTendonP) {
         myH = (t1 - t0); // store step size for updating muscleLength later
         myMuscleLengthPrev = myMuscleLength;
         myLengthPrev = myLength;
         myLengthValid = false;
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
      myMaxContractionVelocity *= s;
      myDamping *= s;

      myMuscleLength *= s;
      myMuscleLengthPrev *= s;
      myLength *= s;
      myLengthPrev *= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      myMaxIsoForce *= s;
      myDamping *= s;
   }
}

