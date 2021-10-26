package artisynth.core.materials;

import maspack.properties.*;
import maspack.interpolation.CubicHermiteSpline1d;

public class Millard2012AxialMuscle extends AxialMuscleMaterialBase {

   public static double DEFAULT_FIBRE_DAMPING = 0.1;
   protected double myDamping = DEFAULT_FIBRE_DAMPING;

   public static double DEFAULT_MAX_ISO_FORCE = 1;
   protected double myMaxIsoForce = DEFAULT_MAX_ISO_FORCE;

   public static double DEFAULT_OPT_FIBRE_LENGTH = 1;
   protected double myOptFibreLength = DEFAULT_OPT_FIBRE_LENGTH;

   public static double DEFAULT_OPT_PENNATION_ANGLE = 0.5;
   protected double myOptPennationAngle = DEFAULT_OPT_PENNATION_ANGLE;

   public static double DEFAULT_MAX_PENNATION_ANGLE = Math.toRadians(84.3);
   protected double myMaxPennationAngle = DEFAULT_MAX_PENNATION_ANGLE;

   public static double DEFAULT_TENDON_SLACK_LENGTH = 1;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;

   public static boolean DEFAULT_IGNORE_TENDON_COMPLIANCE = true;
   protected boolean myIgnoreTendonCompliance = DEFAULT_IGNORE_TENDON_COMPLIANCE;

   public static boolean DEFAULT_IGNORE_FORCE_VELOCITY = true;
   protected boolean myIgnoreForceVelocity = DEFAULT_IGNORE_FORCE_VELOCITY;

   double myH;
   double myFibreLength;
   double myMinCos; // minimm cosine of the pennation angle

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
         "optimal fibre length", DEFAULT_OPT_FIBRE_LENGTH);
      myProps.add (
         "optPennationAngle",
         "pennation angle at optimal length", DEFAULT_OPT_PENNATION_ANGLE);
      myProps.add (
         "maxPennationAngle",
         "maximum pennation angle", DEFAULT_MAX_PENNATION_ANGLE);
      myProps.add (
         "tendonSlackLength",
         "resting length of the tendon", DEFAULT_TENDON_SLACK_LENGTH);
      myProps.add(
         "ignoreTendonCompliance",
         "if true, assume that the tendon is rigid",
         DEFAULT_IGNORE_TENDON_COMPLIANCE);
      myProps.add(
         "ignoreForceVelocity",
         "if true, ignore the force velocity curve",
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
      
      myH = DEFAULT_OPT_FIBRE_LENGTH*Math.sin(DEFAULT_OPT_PENNATION_ANGLE);
      myMinCos = Math.cos(DEFAULT_MAX_PENNATION_ANGLE);
   }

   public Millard2012AxialMuscle (
      double maxIsoForce, double optFibreLen,
      double tendonSlackLen, double optPennationAng) {
      
      this();
      setMaxIsoForce (maxIsoForce);
      setOptFibreLength (optFibreLen);
      setTendonSlackLength (tendonSlackLen);
      setOptPennationAngle (optPennationAng);
   }

   public static CubicHermiteSpline1d getDefaultActiveForceLengthCurve() {
      if (myDefaultActiveForceLengthCurve == null) {
         myDefaultActiveForceLengthCurve =
            createActiveForceLengthCurve (
               0.4441, 0.73, 1.8123, 0.8616, 0.1);
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
      myH = myOptFibreLength*Math.sin(myOptPennationAngle);
   }

   public double getOptPennationAngle() {
      return myOptPennationAngle;
   }

   public void setOptPennationAngle (double ang) {
      myOptPennationAngle = ang;
      myH = myOptFibreLength*Math.tan(myOptPennationAngle);
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

   public boolean getIgnoreTendonCompliance () {
      return myIgnoreTendonCompliance;
   }

   public void setIgnoreTendonCompliance (boolean enable) {
      myIgnoreTendonCompliance = enable;
   }

   public boolean getIgnoreForceVelocity () {
      return myIgnoreForceVelocity;
   }

   public void setIgnoreForceVelocity (boolean enable) {
      myIgnoreForceVelocity = enable;
   }

   private double cosPennationAngle (double fl) {
      double c = fl/Math.sqrt(myH*myH+fl*fl);
      return Math.max (c, myMinCos);
   }

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      if (true || myIgnoreTendonCompliance) {
         // rigid tendon case
         double calm = l-myTendonSlackLength; // calm = cos(alpha)*lm
         double lm = Math.sqrt (myH*myH + calm*calm); // muscle length
         double ca = calm/lm; // ca = cos(alpha)

         double ln = lm/myOptFibreLength; // normalized muscle length
         double vn = ldot*ca/myOptFibreLength; // normalized muscle velocity
         double fa = myActiveForceLengthCurve.evalY (ln);
         double fp = myPassiveForceLengthCurve.evalY (ln);
         if (myIgnoreForceVelocity) {
            return myMaxIsoForce*(fa*excitation+fp+myDamping*vn)*ca;
         }
         else {
            double fv = myForceVelocityCurve.evalY (vn);
            return myMaxIsoForce*(fa*fv*excitation+fp+myDamping*vn)*ca;
         }
      }
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      if (true || myIgnoreTendonCompliance) {
         // rigid tendon case
         double calm = l-myTendonSlackLength; // calm = cos(alpha)*lm
         double lmSqr = myH*myH + calm*calm;
         double lm = Math.sqrt (lmSqr); // muscle length
         double ca = calm/lm; // ca = cos(alpha)

         double ln = lm/myOptFibreLength; // normalized muscle length
         double vn = ldot*ca/myOptFibreLength; // normalized muscle velocity
         double fa = myActiveForceLengthCurve.evalY (ln);
         double fp = myPassiveForceLengthCurve.evalY (ln);

         double dca = myH*myH/(lmSqr*lm);
         double dvn = ldot*dca/myOptFibreLength;
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
            double fm = myMaxIsoForce*(fa*fv*excitation+fp+myDamping*vn);
            double dfv = myForceVelocityCurve.evalDy(vn)*dvn;
            return myMaxIsoForce*(
               (dfa*fv+fa*dfv)*excitation + dfp + myDamping*dvn)*ca + fm*dca;
         }
      }
      return 0;
   }
   
   /**
    * {@inheritDoc}
    */
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      if (true || myIgnoreTendonCompliance) {
         // rigid tendon case
         if (myDamping == 0 && myIgnoreForceVelocity) {
            return 0;
         }
         double calm = l-myTendonSlackLength; // calm = cos(alpha)*lm
         double lm = Math.sqrt (myH*myH + calm*calm); // muscle length
         double ca = calm/lm; // ca = cos(alpha)

         double ftmp;
         if (myIgnoreForceVelocity) {
            ftmp = myDamping;
         }
         else {
            double ln = lm/myOptFibreLength; // normalized muscle length
            double fa = myActiveForceLengthCurve.evalY(ln);
            double vn = ldot*ca/myOptFibreLength; // normalized muscle velocity
            ftmp = fa*myForceVelocityCurve.evalDy(vn)*excitation + myDamping;
         }
         return myMaxIsoForce*ftmp*ca*ca/myOptFibreLength;
      }
      return 0;
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

}
