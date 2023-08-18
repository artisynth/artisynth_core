package artisynth.core.materials;

import maspack.properties.*;
import maspack.interpolation.CubicHermiteSpline1d;

/**
 * Base class for point-to-point muscle materials that implement
 * muscle tendons.
 */
public abstract class AxialTendonBase extends AxialMuscleMaterialBase {
   
   // generic tendon properties

   // Maximum isometric force that the fibers can generate
   public static double DEFAULT_MAX_ISO_FORCE = 1000.0;
   protected double myMaxIsoForce = DEFAULT_MAX_ISO_FORCE;

   // Resting length of the tendon
   public static double DEFAULT_TENDON_SLACK_LENGTH = 0.2;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;

   // Thelen2003 specific properties

   // tendon strain at maximum isometric muscle force
   public static double DEFAULT_FMAX_TENDON_STRAIN = 0.04;
   protected double myFmaxTendonStrain = DEFAULT_FMAX_TENDON_STRAIN;

   // fixed tendon parameters
   double myKToe = 3.0;
   double myExpKToe = Math.exp(myKToe);
   double myFToe = 0.33;
   double myEToe; // computed from FmaxTendonStrain
   double myKlin; // computed from FmaxTendonStrain

   public static PropertyList myProps =
      new PropertyList(AxialTendonBase.class,
                       AxialMuscleMaterialBase.class);

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

   public AxialTendonBase() {
   }

   // property accessors

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
   protected abstract double computeDTendonForce (double tln);   

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      double tln = l/myTendonSlackLength; // normalized tendon length
      return myMaxIsoForce*computeTendonForce (tln);
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      double tln = l/myTendonSlackLength; // normalized tendon length
      double dy = computeDTendonForce (tln);
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
