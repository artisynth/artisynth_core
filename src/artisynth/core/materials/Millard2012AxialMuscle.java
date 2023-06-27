package artisynth.core.materials;

import artisynth.core.modelbase.HasNumericState;
import maspack.util.DataBuffer;
import maspack.util.*;
import maspack.properties.*;
import maspack.numerics.*;
import maspack.function.*;
import maspack.interpolation.CubicHermiteSpline1d;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

public class Millard2012AxialMuscle
   extends AxialMuscleMaterialBase implements HasNumericState {

   public static boolean useNewtonSolver = false;
   public static boolean useFindLm = true;
   public boolean vmFromTendon = false;

   static double GTOL = 2.5e-10;
   static double RTOD = 180/Math.PI;
   static double DTOR = Math.PI/180;

   private static double INF = Double.POSITIVE_INFINITY;

   // from OpenSim Muscle:

   public static double DEFAULT_MAX_ISO_FORCE = 1000;
   protected double myMaxIsoForce = DEFAULT_MAX_ISO_FORCE;

   public static double DEFAULT_OPT_FIBRE_LENGTH = 0.1;
   protected double myOptFibreLength = DEFAULT_OPT_FIBRE_LENGTH;

   public static double DEFAULT_TENDON_SLACK_LENGTH = 0.2;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;

   public static double DEFAULT_OPT_PENNATION_ANGLE = 0.0;
   protected double myOptPennationAngle = DEFAULT_OPT_PENNATION_ANGLE;

   public static double DEFAULT_MAX_CONTRACTION_VELOCITY = 10;
   protected double myMaxContractionVelocity = DEFAULT_MAX_CONTRACTION_VELOCITY;

   public static boolean DEFAULT_RIGID_TENDON = true;
   protected boolean myRigidTendonP = DEFAULT_RIGID_TENDON;

   public static boolean DEFAULT_COMPUTE_LMDOT_FROM_LDOT = false;
   protected boolean myComputeLmDotFromLDot = DEFAULT_COMPUTE_LMDOT_FROM_LDOT;

   // from OpenSim Millard2012EquilibriumMuscle:

   public static double DEFAULT_FIBRE_DAMPING = 0.1;
   protected double myDamping = DEFAULT_FIBRE_DAMPING;

   public static double DEFAULT_MAX_PENNATION_ANGLE = Math.acos(0.1);
   protected double myMaxPennationAngle = DEFAULT_MAX_PENNATION_ANGLE;

   public static boolean DEFAULT_IGNORE_FORCE_VELOCITY = true;
   protected boolean myIgnoreForceVelocity = DEFAULT_IGNORE_FORCE_VELOCITY;

   public static double DEFAULT_DEFAULT_MIN_FLC_VALUE = 0.0;
   protected static double myDefaultMinFlcValue = DEFAULT_DEFAULT_MIN_FLC_VALUE;

   public static double DEFAULT_MUSCLE_LENGTH = 0;
   double myMuscleLength = DEFAULT_MUSCLE_LENGTH;
   double myDm;
   double myDv;
   double myDt;
   
   double myHeight;
   double myMinCos; // minimum cosine of the pennation angle

   double myH;
   double myLength;
   double myLengthPrev;
   boolean myLengthValid = false;
   double myMuscleLengthPrev;
   double myMuscleVel; // computed for diagnostics only
   boolean myMuscleLengthValid = false;

   public static PropertyList myProps =
      new PropertyList(Millard2012AxialMuscle.class,
                       AxialMuscleMaterialBase.class);

   // default force curves
   protected static CubicHermiteSpline1d myDefaultActiveForceLengthCurve;
   protected static CubicHermiteSpline1d myDefaultPassiveForceLengthCurve;
   protected static CubicHermiteSpline1d myDefaultForceVelocityCurve;
   protected static CubicHermiteSpline1d myDefaultTendonForceLengthCurve;

   static {
      myProps.add (
         "maxIsoForce",
         "maximum isometric force", DEFAULT_MAX_ISO_FORCE);
      myProps.add (
         "optFibreLength",
         "optimal fiber length", DEFAULT_OPT_FIBRE_LENGTH);
      myProps.add (
         "optPennationAngle",
         "pennation angle at optimal length", DEFAULT_OPT_PENNATION_ANGLE);
      myProps.add (
         "fibreDamping",
         "damping term for the muscle component", DEFAULT_FIBRE_DAMPING);
      myProps.add (
         "maxContractionVelocity",
         "maximum fiber contraction velocity", DEFAULT_MAX_CONTRACTION_VELOCITY);
      myProps.add (
         "maxPennationAngle",
         "maximum pennation angle", DEFAULT_MAX_PENNATION_ANGLE);
      myProps.add (
         "tendonSlackLength",
         "resting length of the tendon", DEFAULT_TENDON_SLACK_LENGTH);
      myProps.add (
         "muscleLength",
         "length of the muscle section", DEFAULT_MUSCLE_LENGTH);      
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
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Millard2012AxialMuscle() {
      setActiveForceLengthCurve (getDefaultActiveForceLengthCurve());
      setPassiveForceLengthCurve (getDefaultPassiveForceLengthCurve());
      setForceVelocityCurve (getDefaultForceVelocityCurve());
      setTendonForceLengthCurve (getDefaultTendonForceLengthCurve());
      
      myHeight = DEFAULT_OPT_FIBRE_LENGTH*Math.sin(DEFAULT_OPT_PENNATION_ANGLE);
      myMinCos = Math.cos(DEFAULT_MAX_PENNATION_ANGLE);
      myMuscleLength = DEFAULT_MUSCLE_LENGTH;
      myMuscleLengthPrev = DEFAULT_MUSCLE_LENGTH;
      myH = -1;
   }

   public Millard2012AxialMuscle (
      double maxIsoForce, double optFibreLen,
      double tendonSlackLen, double optPennationAng) {
      
      this();
      setMaxIsoForce (maxIsoForce);
      setOptFibreLength (optFibreLen);
      setTendonSlackLength (tendonSlackLen);
      setOptPennationAngle (optPennationAng);
      myMuscleLength = myOptFibreLength*Math.cos(optPennationAng);
      myMuscleLengthPrev = myMuscleLength;
   }

   public static CubicHermiteSpline1d getDefaultActiveForceLengthCurve() {
      if (myDefaultActiveForceLengthCurve == null) {
         myDefaultActiveForceLengthCurve =
            createActiveForceLengthCurve (
               0.4441, 0.73, 1.8123, 0.8616, getDefaultMinFlcValue());
      }
      return myDefaultActiveForceLengthCurve;
   }

   public static CubicHermiteSpline1d getDefaultPassiveForceLengthCurve() {
      if (myDefaultPassiveForceLengthCurve == null) {
         double strainAtZeroForce = 0.0;
         double strainAtOneNormForce = 0.7;
         myDefaultPassiveForceLengthCurve =
            createPassiveForceLengthCurve (
               strainAtZeroForce, strainAtOneNormForce, 0.2,
               2.0/(strainAtOneNormForce-strainAtZeroForce), 0.75);
      }
      return myDefaultPassiveForceLengthCurve;
   }

   public static CubicHermiteSpline1d getDefaultForceVelocityCurve() {
      if (myDefaultForceVelocityCurve == null) {
         myDefaultForceVelocityCurve =
            createForceVelocityCurve (
               0.01, 0.25, 5.0, 0.01, 0.15, 1.4, 0.6, 0.9);
      }
      return myDefaultForceVelocityCurve;
   }

   public static CubicHermiteSpline1d getDefaultTendonForceLengthCurve() {
      if (myDefaultTendonForceLengthCurve == null) {
         double strainAtOneNormForce = 0.049;
         myDefaultTendonForceLengthCurve =
            createTendonForceLengthCurve (
               strainAtOneNormForce, 1.375/strainAtOneNormForce, 2.0/3.0, 0.5);
      }
      return myDefaultTendonForceLengthCurve;
   }

   public double getFibreDamping() {
      return myDamping;
   }

   public void setFibreDamping (double d) {
      myDamping = d;
   }

   public double getMaxIsoForce() {
      return myMaxIsoForce;
   }

   public void setMaxIsoForce (double maxf) {
      myMaxIsoForce = maxf;
   }

   public double getOptFibreLength() {
      return myOptFibreLength;
   }

   public void setOptFibreLength (double l) {
      myOptFibreLength = l;
      myHeight = myOptFibreLength*Math.sin(myOptPennationAngle);
   }

   public double getMaxContractionVelocity() {
      return myMaxContractionVelocity;
   }

   public void setMaxContractionVelocity (double maxv) {
      myMaxContractionVelocity = maxv;
   }

   public double getOptPennationAngle() {
      return myOptPennationAngle;
   }

   public void setOptPennationAngle (double ang) {
      myOptPennationAngle = ang;
      myHeight = myOptFibreLength*Math.sin(myOptPennationAngle);
   }

   public double getMaxPennationAngle() {
      return myMaxPennationAngle;
   }

   public void setMaxPennationAngle (double ang) {
      myMaxPennationAngle = ang;
      myMinCos = Math.cos(ang);
   }

   public double getTendonSlackLength() {
      return myTendonSlackLength;
   }

   public void setTendonSlackLength (double l) {
      myTendonSlackLength = l;
   }

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

   public static double getDefaultMinFlcValue() {
      return myDefaultMinFlcValue;
   }

   public static void setDefaultMinFlcValue (double min) {
      myDefaultMinFlcValue = min;
      myDefaultActiveForceLengthCurve = null;
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

   private double sqr (double x) {
      return x*x;
   }

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      if (myRigidTendonP) {
         // rigid tendon case
         double lm = l-myTendonSlackLength; // lm = cos(alpha)*lf
         myMuscleLength = lm;
         double lf = Math.sqrt (myHeight*myHeight + lm*lm); // fibre length
         double ca = lm/lf; // ca = cos(alpha)

         if (ca < 0) {
            // XXX does this happen?
            return 0;
         }

         double ln = lf/myOptFibreLength; // normalized muscle length
         // normalized muscle velocity:
         double vn = ldot*ca/(myOptFibreLength*myMaxContractionVelocity); 
         //double vn = ldot*ca/(myOptFibreLength); 
         double fa = myActiveForceLengthCurve.evalY (ln);
         double fp = myPassiveForceLengthCurve.evalY (ln);
         double fm;
         if (myIgnoreForceVelocity) {
            fm = myMaxIsoForce*(fa*excitation+fp+myDamping*vn)*ca;
         }
         else {
            double fv = myForceVelocityCurve.evalY (vn);
            if (fv < 0) {
               fv = 0;
            }
            fm = myMaxIsoForce*(fa*fv*excitation+fp+myDamping*vn)*ca;
         }
         return fm;
      }
      else {
         if (!myLengthValid) {
            updateMuscleLength (l, ldot, excitation);
            myLengthValid = true;
         }
         return getTendonForce (myLength-myMuscleLength);
      }
   }

   double getTendonForce (double lt) {
      double ltn = lt/getTendonSlackLength();
      return myMaxIsoForce*myTendonForceLengthCurve.evalY (ltn);
   }

   static int nsame = 0;
   static int ninterval = 0;
   static int nsolved = 0;
   static int nclippedL = 0;
   static int nclipped0 = 0;
   static int maxiters = 0;
   static int nshort = 0;

   static int nreduce;

//   double getLmA () {
//      double S = Math.sin(myOptPennationAngle);
//      double ol = getOptFibreLength();
//      Knot knot = getActiveForceLengthCurve().getKnot(0);
//      if (knot.getY() == 0) {
//         if (knot.getX() > S) {
//            return ol*Math.sqrt(sqr(knot.getX()) - S*S);
//         }
//      }
//      return 0;      
//   }

   // /**
   //  * Returns a tendon stiffness threshold computed as one percent of the
   //  * stiffness observed at an increase from the slack length that would position given by
   //  * 

   //  * 
   // double getTendonStiffnessThreshold() {
   //    return
   //       getTendonForceLengthCurve().getLastKnot().getDy()/myTendonSlackLength;
   // }

   double getMaxTendonStiffness() {
      return
         getTendonForceLengthCurve().getLastKnot().getDy()/myTendonSlackLength;
   }

   double getMaxPassiveStiffness() {
      return getPassiveForceLengthCurve().getLastKnot().getDy()/myOptFibreLength;
   }

//   double[] getLmBC (double lm0, double l, double h) {
//      double Vmax = getMaxContractionVelocity();
//      double H = myHeight;
//      double ol = getOptFibreLength();
//
//      double lmo = ol*Math.cos(myOptPennationAngle);
//      double gamma = h*ol*Vmax;
//      if (H == 0) {
//         if (lm0 - gamma > 0) {
//            return new double[] { 0, Math.min(lm0-gamma,lmo) };
//         }
//      }
//      else {
//         double gammaSqr = gamma*gamma;
//         double[] roots = new double[4];            
//         int nr = QuarticSolver.getRoots (
//            roots, 1.0, -2*lm0, lm0*lm0-gammaSqr, 0.0, -gammaSqr*H*H, 0, l);
//         if (nr > 0) {
//            double maxlm = -INF;
//            double minlm = INF;
//            for (int i=0; i<nr; i++) {
//               double lm = roots[i];
//               if (lm*(lm-lm0) < 0) {
//                  if (lm > maxlm) {
//                     maxlm = lm;
//                  }
//                  if (lm < minlm) {
//                     minlm = lm;
//                  }
//               }
//            }     
//            if (maxlm != -INF) {
//               maxlm = Math.min (maxlm,lmo);
//               if (minlm < maxlm) {
//                  return new double[] { minlm, maxlm };
//               }
//            }
//         }
//      }
//      return null;
//   }
//
//   double[] getLmBC (double vm) {
//      double Vmax = getMaxContractionVelocity();
//      double H = myHeight;
//      double ol = getOptFibreLength();
//
//      if (H == 0) {
//         if (vm <= -ol*Vmax) {
//            return new double[] {0, INF};
//         }
//      }
//      else {
//         if (vm < -ol*Vmax) {
//            double alpha = ol*Vmax/Math.abs(vm); // will be < 1
//            double lmB = alpha*H/Math.sqrt(1-alpha*alpha);
//            return new double[] {lmB, INF};
//         }
//      }
//      return null;
//   }

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

//   private class GFunction implements Function1x1 {
//      double myL;
//      double myA;
//      double myH;
//      double myLm0;
//      double myVm;
//      boolean myVmConstant;
//      
//      GFunction (
//         double l, double vm, double lm0, double a, double h, 
//         boolean vmConstant) {
//         myL = l;
//         myVm = vm;
//         myA = a;
//         myH = h;
//         myLm0 = lm0;
//         myVmConstant = vmConstant;
//      }
//      
//      public double eval (double lm) {
//         if (myVmConstant) {
//            return computeG (null, myL, lm, myVm, myA, -1);            
//         }
//         else {
//            return computeG (null, myL, lm, (lm-myLm0)/myH, myA, myH);
//         }
//      }
//   }
   
//   private double nearestPointOnInterval (double[] ival, double x) {
//      if (x < ival[0]) {
//         return ival[0];
//      }
//      else if (x > ival[1]) {
//         return ival[1];
//      }
//      else {
//         return x;
//      }
//   }

//   private double nearestPointOnIntervals (
//      double[] ival0, double[] ival1, double x) {
//      
//      // assume intervals are in order such that ival0[0] <= ival1[0]
//      if (ival0[1] >= ival1[0]) {
//         // intervals overlap
//         return nearestPointOnInterval (new double[] {ival0[0], ival1[1]}, x);
//      }
//      double center = (ival1[0]-ival0[1])/2;
//      if (x < center) {
//         // x closer to ival0
//         return nearestPointOnInterval (ival0, x);
//      }
//      else {
//         // x closer to ival1
//         return nearestPointOnInterval (ival1, x);
//      }
//   }

//   private double[] clipIntervalMax (double[] ival, double max) {
//      if (ival == null) {
//         return null;
//      }
//      if (max <= ival[0]) {
//         return null;
//      }
//      else if (max < ival[1]) {
//         ival[1] = max;
//         return ival;
//      }
//      else {
//         return ival;
//      }
//   }
//   
//   private double[] clipIntervalMin (double[] ival, double min) {
//      if (ival == null) {
//         return null;
//      }
//      if (min >= ival[1]) {
//         return null;
//      }
//      else if (min > ival[0]) {
//         ival[0] = min;
//         return ival;
//      }
//      else {
//         return ival;
//      }
//   }

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

//   public double computeVmFromLdotOrig (double ldot) {
//      double denom = myDm + myDt;
//      if (denom != 0) {
//         return myDt*ldot/denom;
//      }
//      else {
//         return ldot;
//      }      
//   }
   
//   public double initializeLmFromLdot (double l, double ldot, double a) {
//      int numIters = 3;
//      double vm = 0;
//      double lm = 0;
//      for (int k=0; k<numIters; k++) {
//         //vm = computeVmFromLdot (ldot);
//         lm = computeLmWithConstantVm (l, /*lmprev (not used)=*/0, vm, a);
//         setMuscleLength (lm);
//         myLengthPrev = l;
//         computeDerivs (l, lm, vm, a);     
//         if (k < numIters-1) {
//            vm = computeVmFromLdot (ldot);
//         }
//      }
//      return lm;
//   }

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

//   private double findLmWithoutDamping (
//      Diff1Function1x1 gfxn, double l, double lmprev, double[] lmBC,
//      double a) {
//
//      double T = getTendonSlackLength();
//      double ol = getOptFibreLength();
//
//      double lmP = ol*Math.cos(myOptPennationAngle);
//      double lmT = Math.max(0, l-T); // lm where tendon force starts
//
//      double lmlo = 0;
//      double lmhi = l;
//      double lm = -1;
//
//      if (a != 0) {
//         double[] lmTA = null; // interval from 
//         double lmA = getLmA(); // lowest lm for which F_L is 0
//         double lmh = lmA; // highest point on any F_m zero interval
//         if (lmA > lmT) {
//            lmTA = new double[] { lmT, lmA };
//         }
//         lmBC = clipIntervalMax (lmBC, lmP);
//         if (lmBC != null) {
//            lmh = Math.max (lmh, lmBC[1]);
//         }
//         lmBC = clipIntervalMin (lmBC, lmT);
//
//         if (lmTA != null && lmBC != null) {
//            lm = nearestPointOnIntervals (lmTA, lmBC, lmprev);
//            ninterval++;
//         }
//         else if (lmTA != null) {
//            lm = nearestPointOnInterval (lmTA, lmprev);
//            ninterval++;
//         }
//         else if (lmBC != null) {
//            lm = nearestPointOnInterval (lmBC, lmprev);
//            ninterval++;
//         }
//         else {
//            // find root in bracket between lmh and l-T
//            lmlo = lmh;
//            lmhi = lmT;
//         }
//      }
//      else {
//         // no effective force
//         if (lmT <= lmP) {
//            lm = nearestPointOnInterval (
//               new double[] { lmT, lmP }, lmprev);
//            ninterval++;
//         }
//         else {
//            // find root in bracket between lmP and l-T
//            lmlo = lmP;
//            lmhi = lmT;
//         }
//      }
//      if (lm == -1) {
//         double tol = 1e-8*(T+ol);
//         if (useNewtonSolver) {
//            lm = NewtonRootSolver.findRoot (gfxn, lmlo, lmhi, tol, GTOL);
//         }
//         else {
//            lm = BrentRootSolver.findRoot (gfxn, lmlo, lmhi, tol, GTOL);
//         }
//         nsolved++;
//      }
//      if (l < lm) {
//         System.out.printf ("unclipped lm=%g lmlo=%g lmhi=%g\n", lm, lmlo, lmhi);
//         lm = l;
//         nclippedL++;
//      }
//      return lm;
//   }
//
//   private double findLmWithDamping (
//      Diff1Function1x1 gfxn, double l, double lmprev,
//      double vmguess, double a, boolean vmIsConstant) {
//
//      double T = getTendonSlackLength();
//      double H = myHeight;
//      double ol = getOptFibreLength();
//      double Vmax = getMaxContractionVelocity();
//      double lmP = ol*Math.cos(myOptPennationAngle);      
//
//      double lmA = (a != 0 ? getLmA() : lmP);
//      double lm = -1;
//      double lmlo = 0;
//      double lmhi = l;
//
//      if (l <= lmA + T) {
//         // length <= minimum required for zero force at equilibrium
//         nshort++;
//         lm = Math.min(lmA, l);
//      }
//      else {
//         double lmguess = lmprev;
//         double g0 = gfxn.eval (lmguess); 
//         if (Math.abs(g0) <= GTOL) {
//            nsame++;
//            lm = lmguess;
//         }
//         else if (g0 > GTOL) {
//            // If g(lmguess) > 0, we know that g(0) < 0, since -F_t(0) < 0
//            // (because l > T) and F_m(0) <= 0 (active muscle and passive
//            // muscle forces are 0, and damping part is <= 0 except
//            // when vmIsConstant, which we handle below).
//            lmhi = lmguess;
//            lmlo = 0;
//            if (vmIsConstant && vmguess > 0) {
//               // special case: check if damping force > 0 at lmlo and
//               // adjust lmlo if necessary
//               double c = computeCos (lmlo);
//               double fd = myDamping*vmguess*c*c/(Vmax*ol);
//               double ft = myTendonForceLengthCurve.evalY((l-lmlo)/T);
//               System.out.println ("fd=" + fd + " HH=" + myHeight + " ");
//               if (fd/ft > maxOver) {
//                  maxOver = fd/ft;
//                  System.out.println ("OVER " + maxOver);
//               }
//               if (fd >= ft) {
//                  // adjust lmlo so the tendon force will be enough to
//                  // ensure F_m(0) < 0. Multiply by 1.05 to make sure
//                  lmlo = l-T*myTendonForceLengthCurve.solveX (fd*1.05);
//                  System.out.println ("new LMLO " + lmlo);                  
//               }
//            }
//         }
//         else { // g0 < -GTOL
//            // If g(lmguess) < 0, we know that g(lmhi) > 0, with lmhi =
//            // max(l,lmprev), since F_t(lmhi) = 0 (because l > T), and
//            // F_m(lmhi) > 0 (active and muscle forces are > 0 because l >
//            // lmA + T, and damping component = 0 at lmprev except when
//            // vmIsConstant, which we handle below).
//            lmlo = lmguess;
//            lmhi = Math.max (l,lmprev);
//            if (vmIsConstant && vmguess < 0) {  
//               // special case: check if damping force > 0 at lmhi and
//               // adjust lmhi if necessary. Assume c = 1 to cover worst case
//               double fdmax = -myDamping*vmguess/(Vmax*ol);
//               double ln = Math.sqrt (H*H+lmhi*lmhi)/ol;
//               if (fdmax >= myPassiveForceLengthCurve.evalY(ln)) {
//                  // adjust lmhi so the passive force will be enough to
//                  // ensure F_m(0) < 0. Multiply by 1.05 to make sure.
//                  ln = myPassiveForceLengthCurve.solveX (fdmax*1.05);
//                  lmhi = Math.sqrt (sqr(ln*ol)-H*H);
//               }
//            }
//         }
//      }
//      if (lm == -1) {
//         //System.out.println ("solving "+lmlo+" "+lmhi);
//         double tol = 1e-8*(T+ol);
//         if (useNewtonSolver) {
//            lm = NewtonRootSolver.findRoot (gfxn, lmlo, lmhi, tol, GTOL);
//         }
//         else {
//            lm = BrentRootSolver.findRoot (gfxn, lmlo, lmhi, tol, GTOL);
//         }
//         //computeFm (l, lm, lmprev, a, h);
//         nsolved++;
//      }
//      if (l < lm) {
//         System.out.printf ("unclipped lm=%g lmlo=%g lmhi=%g lmA=%g\n", lm, lmlo, lmhi, lmA);
//         lm = l;
//         nclippedL++;
//      }
//      else if (lm < 0) {
//         System.out.printf ("lm < 0\n");
//         lm = 0;
//         nclipped0++;
//      }
//      if (Math.abs(gfxn.eval(lm)) > GTOL) {
//         System.out.println ("BAD " + gfxn.eval(lm));
//      }
//      return lm;
//   }

//   double rootByBisection (
//      double lmlo, double lmhi, double l, double ldot, double vm, double a) {
//
//      double h = 0.01;
//      double glo = computeG (null, l, lmlo, vm, a, h);
//      for (int i=0; i<48; i++) {
//         double lm = (lmlo+lmhi)/2;
//         double g = computeG (null, l, lm, vm, a, h);
//         if (g == 0) {
//            return lm;
//         }
//         else if (g*glo > 0) {
//            // root is in hi half
//            lmlo = lm;
//         }
//         else {
//            // root is in lo half
//            lmhi = lm;
//         }
//      }
//      return (lmlo+lmhi)/2;
//   }
//
//   double maxRootByBisection (
//      double lmlo, double lmhi, double l, double ldot, double vm, double a) {
//
//      // assume g at lmlo is 0
//      double h = 0.01;
//      for (int i=0; i<48; i++) {
//         double lm = (lmlo+lmhi)/2;
//         double g = computeG (null, l, lm, vm, a, h);
//         if (Math.abs(g) <= GTOL) {
//            // max root is in hi half
//            lmlo = lm;
//         }
//         else {
//            // max root is in lo half
//            lmhi = lm;
//         }
//      }
//      return (lmlo+lmhi)/2;
//   }
//
//   double minRootByBisection (
//      double lmlo, double lmhi, double l, double ldot, double vm, double a) {
//
//      // assume g at lmhi is 0
//      double h = 0.01;
//      for (int i=0; i<48; i++) {
//         double lm = (lmlo+lmhi)/2;
//         double g = computeG (null, l, lm, vm, a, h);
//         if (Math.abs(g) > GTOL) {
//            // min root is in hi half
//            lmlo = lm;
//         }
//         else {
//            // min root is in lo half
//            lmhi = lm;
//         }
//      }
//      return (lmlo+lmhi)/2;
//   }

//   double[] findRoots (double l, double ldot, double vm, double a) {
//      int nints = 256;
//      // brute force search for intervals
//      DynamicDoubleArray roots = new DynamicDoubleArray();
//      double h = 0.01;
//      for (int i=0; i<nints; i++) {
//         double lmA = i*(l/nints);
//         double lmB = (i+1)*(l/nints);
//         double g0 = computeG (null, l, lmA, vm, a, h);
//         double g1 = computeG (null, l, lmB, vm, a, h);
//         if (Math.abs(g0) > GTOL && Math.abs(g1) > GTOL && g0*g1 < 0) {
//            roots.add (rootByBisection (lmA, lmB, l, ldot, vm, a));
//         }
//         else if (Math.abs(g0) <= GTOL && Math.abs(g1) > GTOL) {
//            roots.add (maxRootByBisection (lmA, lmB, l, ldot, vm, a));            
//         }
//         else if (Math.abs(g0) > GTOL && Math.abs(g1) <= GTOL) {
//            roots.add (minRootByBisection (lmA, lmB, l, ldot, vm, a));            
//         }
//         else if (Math.abs(g1) <= GTOL && i==nints-1) {
//            roots.add (lmB);
//         }
//      }
//      return roots.getArray();
//   }

   boolean debug;

//   double estimateDeltaLm (
//      double deltal, double ldot, double lmprev) {
//      double denom = myDt + myDm;
//      if (denom != 0) {
//         return myDt/denom * deltal;
//      }
//      else {
//         return 0;
//      }
//   }
   
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
      double sa; // sin(alpha)
      double dcdlm; // d c / d lm
      if (H == 0) {
         lf = lm;
         ca = 1.0;
         sa = 0.0;
         dcdlm = 0;
      }
      else {
         lf = Math.sqrt (H*H + lm*lm);
         ca = lm/lf;
         sa = H/lf;
         dcdlm = sa*sa/lf;
      }
      double ln = lf/lo; // normalized muscle length
      // normalized muscle velocity:
      double vn = vm*ca/(lo*Vmax);

      double fa = myActiveForceLengthCurve.evalY (ln);
      double fp = myPassiveForceLengthCurve.evalY (ln);
      double fv = 1.0;

      if (!myIgnoreForceVelocity && vn != 0) {
         if (vn <= -1) {
            fv = 0;
         }
         else {
            fv = myForceVelocityCurve.evalY (vn);
         }
      }
      double ff = fa*fv*a+fp+myDamping*vn;

      double dfa = myActiveForceLengthCurve.evalDy (ln);
      double dfp = myPassiveForceLengthCurve.evalDy (ln);
      double vterm = myDamping;
      if (!myIgnoreForceVelocity && vn != 0) {
         double dfv = myForceVelocityCurve.evalDy (vn);
         if (vn <= -1) {
            dfv = 0;
         }
         vterm += a*fa*dfv;
      }
      double dvn = dcdlm*vm/(lo*Vmax);
      myDm = ff*dcdlm + (a*dfa*fv + dfp)*ca*ca/lo + vterm*dvn*ca;
      myDv = ca*ca*vterm/(lo*Vmax);
      double ltn = (l-lm)/myTendonSlackLength;
      myDt = computeDFt (ltn);
      if (debug) {
         System.out.printf (
            "  G=%g ltn=%g fm=%g vn=%g\n",
            (ca*ff - computeFt(ltn)), ltn, ca*ff, vn);
      }
   }

//   private double computeCos (double lm) {
//      double H = myHeight;
//      if (H == 0) {
//         return 1.0;
//      }
//      else {
//         return lm/Math.sqrt (H*H + lm*lm);
//      }
//   }

   double computeFm (
      DoubleHolder dF, double lm, double vm, double a, double h) {

      double H = myHeight;
      double Vmax = myMaxContractionVelocity;
      double lo = myOptFibreLength;

      double lf; // fibre length
      double ca; // cos(alpha)
      double sa; // sin(alpha)
      double dcdlm; // d c / d lm
      if (H == 0) {
         lf = lm;
         ca = 1.0;
         sa = 0.0;
         dcdlm = 0;
      }
      else {
         lf = Math.sqrt (H*H + lm*lm);
         ca = lm/lf;
         sa = H/lf;
         dcdlm = sa*sa/lf;
      }
      double ln = lf/lo; // normalized muscle length
      // normalized muscle velocity:
      double vn = 0;
      if (vm != 0) {
         vn = vm*ca/(lo*Vmax); 
      }

      double fa = myActiveForceLengthCurve.evalY (ln);
      double fp = myPassiveForceLengthCurve.evalY (ln);
      double fv = 1.0;

      if (!myIgnoreForceVelocity && vn != 0) {
         if (vn <= -1) {
            fv = 0;
         }
         else {
            fv = myForceVelocityCurve.evalY (vn);
         }
      }
      double ff = fa*fv*a+fp+myDamping*vn;
      if (dF != null) {
         double dfa = myActiveForceLengthCurve.evalDy (ln);
         double dfp = myPassiveForceLengthCurve.evalDy (ln);
         double vterm = myDamping;
         if (!myIgnoreForceVelocity && vn != 0) {
            double dfv = myForceVelocityCurve.evalDy (vn);
            if (vn <= -1) {
               dfv = 0;
            }
            vterm += a*fa*dfv;
         }
         double dvn = 0;
         if (vm != 0) {
            dvn = dcdlm*vm/(lo*Vmax);
         }
         double Dm = ff*dcdlm + (a*dfa*fv + dfp)*ca*ca/lo + vterm*dvn*ca;
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
      return myTendonForceLengthCurve.evalY (ltn);
   }

   double computeDFt (double ltn) {
      return myTendonForceLengthCurve.evalDy (ltn)/myTendonSlackLength;
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
         double lm = l-myTendonSlackLength; // lm = cos(alpha)*lf
         myMuscleLength = lm;
         double lfSqr = myHeight*myHeight + lm*lm;
         double lf = Math.sqrt (lfSqr); // fibre length
         double ca = lm/lf; // ca = cos(alpha)

         if (ca < 0) {
            return 0;
         }

         double ln = lf/myOptFibreLength; // normalized muscle length
         // normalized muscle velocity:
         double vn = ldot*ca/(myOptFibreLength*myMaxContractionVelocity); 
         //double vn = ldot*ca/(myOptFibreLength); 
         double fa = myActiveForceLengthCurve.evalY (ln);
         double fp = myPassiveForceLengthCurve.evalY (ln);

         double dca = myHeight*myHeight/(lfSqr*lf);
         double dvn = ldot*dca/(myOptFibreLength*myMaxContractionVelocity);
         double dln = ca/myOptFibreLength;

         double dfa = myActiveForceLengthCurve.evalDy(ln)*dln;
         double dfp = myPassiveForceLengthCurve.evalDy(ln)*dln;

         if (myIgnoreForceVelocity) {
            double fm = myMaxIsoForce*(fa*excitation+fp+myDamping*vn);
            return myMaxIsoForce*(
               dfa*excitation + dfp + myDamping*dvn)*ca + fm*dca;
         }
         else {
            double fv = myForceVelocityCurve.evalY (vn);
            double dfv = myForceVelocityCurve.evalDy(vn)*dvn;
            if (fv < 0) {
               fv = 0;
               dfv = 0;
            }
            double fm = myMaxIsoForce*(fa*fv*excitation+fp+myDamping*vn);
            return myMaxIsoForce*(
               (dfa*fv+fa*dfv)*excitation + dfp + myDamping*dvn)*ca + fm*dca;
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
         double lm = l-myTendonSlackLength; // lm = cos(alpha)*lf
         myMuscleLength = lm;
         double lf = Math.sqrt (myHeight*myHeight + lm*lm); // muscle length
         double ca = lm/lf; // ca = cos(alpha)
         
         if (ca < 0) {
            return 0;
         }

         double ftmp;
         if (myIgnoreForceVelocity) {
            ftmp = myDamping;
         }
         else {
            double ln = lf/myOptFibreLength; // normalized muscle length
            double fa = myActiveForceLengthCurve.evalY(ln);
            // normalized muscle velocity:
            //double vn = ldot*ca/myOptFibreLength;
            double vn = ldot*ca/(myOptFibreLength*myMaxContractionVelocity); 
            ftmp = fa*myForceVelocityCurve.evalDy(vn)*excitation + myDamping;
         }
         return (myMaxIsoForce*ftmp*ca*ca/
                 (myOptFibreLength*myMaxContractionVelocity));
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
  
   public static void main (String[] args) {
      Millard2012AxialMuscle m = new Millard2012AxialMuscle();

      for (double l=0.5; l<2.0; l+=0.01) {
         System.out.printf ("%g %g\n", l, m.computeF (l, 0, 0, 1.0));
      }
   }

   // implementation of HasNumericState

   public boolean hasState() {
      return !myRigidTendonP;
   }

   public void setState (DataBuffer data) {
      myLength = data.dget();
      myMuscleLength = data.dget();
      myH = data.dget();
      myMuscleLengthValid = data.zgetBool(); // XXX need this?
      myLengthValid = data.zgetBool(); // XXX need this?
   }

   public void getState (DataBuffer data) {
      data.dput(myLength);
      data.dput(myMuscleLength);
      data.dput(myH);
      data.zputBool (myMuscleLengthValid); // XXX need this?
      data.zputBool (myLengthValid); // XXX need this?
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

   public Millard2012AxialMuscle clone() {
      Millard2012AxialMuscle mat = (Millard2012AxialMuscle)super.clone();
      mat.myLengthValid = false;
      return mat;
   }

 }
