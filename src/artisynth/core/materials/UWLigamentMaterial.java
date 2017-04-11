package artisynth.core.materials;

import maspack.properties.PropertyList;

/** 
 * The ligament model used in the University of Wisconsin knee model.
 * Based on: Blankevoort, L., and Huiskes, R. (1991). Ligament-bone 
 * interaction in a three-dimensional model of the knee. 
 * Journal of Biomechanical Engineering, 113(3), 263-269.
 * 
 * implementation adapted from UW SimTk plugin: 
 * https://simtk.org/home/uwmodels
 * 
 * Notes:
 * - the restLength property in the associated spring is used as
 * the referenceLength in the UW formulation, where referenceLength
 * is the length of the ligament when the joint is in the reference 
 * position (full extension).
 * 
 */
public class UWLigamentMaterial extends AxialMaterial {

   protected static double DEFAULT_REFERENCE_STRAIN = 0.0; // typ ligament specific
   protected static double DEFAULT_LINEAR_STIFFNESS = 100; // typ constant
   protected static double DEFAULT_LIG_TRANSITION_STRAIN = 0.03; // typ constant
   protected static double DEFAULT_NORMALIZED_DAMPING = 0.003; // typ constant
   protected static double DEFAULT_MAX_FORCE = 0.0; // typ ligament specific
   protected static double DEFAULT_TENDON_SLACK_LENGTH = 0.0; // typ zero
   
   protected double referenceStrain = DEFAULT_REFERENCE_STRAIN;
   protected double linearStiffness = DEFAULT_LINEAR_STIFFNESS;
   protected double ligamentTransitionStrain = DEFAULT_LIG_TRANSITION_STRAIN;
   protected double normalizedDamping = DEFAULT_NORMALIZED_DAMPING;
   protected double maxForce = DEFAULT_MAX_FORCE;
   protected double tendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;
   
   public static PropertyList myProps =
      new PropertyList (UWLigamentMaterial.class, AxialMaterial.class);

   static {
      myProps.add ("referenceStrain", "zero force strain", 
         DEFAULT_REFERENCE_STRAIN, "%.4g");
      myProps.add ("linearStiffness", "linear portion of force-length curve",
         DEFAULT_LINEAR_STIFFNESS, "%.4g");
      myProps.add ("ligamentTransitionStrain", "strain at transition from toe to linear",
         DEFAULT_LIG_TRANSITION_STRAIN, "%.4g");
      myProps.add ("normalizedDamping", "normalized damping coefficient",
         DEFAULT_NORMALIZED_DAMPING, "%.4g");
      myProps.add ("maxForce", "max force exerted by tendon",
         DEFAULT_MAX_FORCE);
      myProps.add ("tendonSlackLength", "slack length",
         DEFAULT_TENDON_SLACK_LENGTH, "%.4g");
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public UWLigamentMaterial () {
   }

   @Override
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      double forceTotal = 0.0;
      double forceSpring = 0.0;
      double forceDamping = 0.0;
      double slackLength = 0.0;
      double strain = 0.0;
      double strainRate = 0.0;
      
      //Compute slack length
      slackLength = l0/(1.0 + referenceStrain);

      if (l <= slackLength) {
         return 0;
      }

      // Compute strain
      strain = ( l * (1 + referenceStrain) - l0)/l0;

      // Compute strain rate
      strainRate = ldot/slackLength;
      
      // Evaluate elastic force
      if (strain >= ligamentTransitionStrain) {
         forceSpring = linearStiffness*(strain - (ligamentTransitionStrain/2));
      }
      else if ((strain > 0) && (strain < (ligamentTransitionStrain))) {
         forceSpring = 0.25*linearStiffness*strain*strain/(ligamentTransitionStrain/2);
      }

      // Evaluate damping force
      forceDamping = linearStiffness*normalizedDamping*strainRate;

      // Total force
      forceTotal = forceSpring + forceDamping;

      // Ensure the ligament is only acting in tension
      if (forceTotal < 0.0)
         forceTotal = 0.0;

      return forceTotal;
   }

   @Override
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      double strain = ( l * (1 + referenceStrain) - l0 ) / l0;
      double strain_dl = (1 + referenceStrain) / l0;
      return linearStiffness * strain / ligamentTransitionStrain * strain_dl;
      
      // XXX zero if not in tension
   }

   @Override
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      double strainRate_dldot =  (1.0 + referenceStrain) / l0;
      return linearStiffness * normalizedDamping * strainRate_dldot;
      
      // XXX zero if not in tension
   }

   @Override
   public boolean isDFdldotZero () {
      return false;
   }

   /* property methods */
   
   public double getReferenceStrain () {
      return referenceStrain;
   }

   public void setReferenceStrain (double referenceStrain) {
      this.referenceStrain = referenceStrain;
   }

   public double getLinearStiffness () {
      return linearStiffness;
   }

   public void setLinearStiffness (double linearStiffness) {
      this.linearStiffness = linearStiffness;
   }

   public double getLigamentTransitionStrain () {
      return ligamentTransitionStrain;
   }

   public void setLigamentTransitionStrain (double ligamentTransitionStrain) {
      this.ligamentTransitionStrain = ligamentTransitionStrain;
   }

   public double getNormalizedDamping () {
      return normalizedDamping;
   }

   public void setNormalizedDamping (double normalizedDamping) {
      this.normalizedDamping = normalizedDamping;
   }

   public double getMaxForce () {
      return maxForce;
   }

   public void setMaxForce (double maxForce) {
      this.maxForce = maxForce;
   }

   public double getTendonSlackLength () {
      return tendonSlackLength;
   }

   public void setTendonSlackLength (double tendonSlackLength) {
      this.tendonSlackLength = tendonSlackLength;
   }

}
