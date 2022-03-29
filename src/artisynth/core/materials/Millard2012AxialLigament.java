package artisynth.core.materials;

import maspack.properties.*;
import maspack.interpolation.CubicHermiteSpline1d;

public class Millard2012AxialLigament extends AxialMuscleMaterialBase {

   public static double DEFAULT_MAX_ISO_FORCE = 1;
   protected double myMaxIsoForce = DEFAULT_MAX_ISO_FORCE;

   public static double DEFAULT_TENDON_SLACK_LENGTH = 1;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;

   public static PropertyList myProps =
      new PropertyList(Millard2012AxialLigament.class,
                       AxialMuscleMaterialBase.class);

   // default force curves
   protected static CubicHermiteSpline1d myDefaultTendonForceLengthCurve;

   static {
      myProps.add (
         "maxIsoForce",
         "maximum isometric force", DEFAULT_MAX_ISO_FORCE);
      myProps.add (
         "tendonSlackLength",
         "resting length of the tendon", DEFAULT_TENDON_SLACK_LENGTH);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Millard2012AxialLigament() {
      setTendonForceLengthCurve (getDefaultTendonForceLengthCurve());
   }

   public Millard2012AxialLigament (
      double maxIsoForce, double tendonSlackLen) {
      
      this();
      setMaxIsoForce (maxIsoForce);
      setTendonSlackLength (tendonSlackLen);
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

   public double getMaxIsoForce() {
      return myMaxIsoForce;
   }

   public void setMaxIsoForce (double maxf) {
      myMaxIsoForce = maxf;
   }

   public double getTendonSlackLength() {
      return myTendonSlackLength;
   }

   public void setTendonSlackLength (double l) {
      myTendonSlackLength = l;
   }

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      double ln = l/myTendonSlackLength; // normalized muscle length
      return myMaxIsoForce*myTendonForceLengthCurve.evalY (ln);
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      double ln = l/myTendonSlackLength; // normalized muscle length
      double dy = myTendonForceLengthCurve.evalDy(ln);
      return myMaxIsoForce*dy/myTendonSlackLength;
   }
   
   /**
    * {@inheritDoc}
    */
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDFdldotZero() {
      return true;
   }

}
