package artisynth.core.materials;

import maspack.properties.PropertyList;

/** 
 * Ligmament material based on : Blankevoort, L., and Huiskes,
 * R. (1991). Ligament-bone interaction in a three-dimensional model of the
 * knee.  Journal of Biomechanical Engineering, 113(3), 263-269.
 * 
 * implementation adapted from UW SimTk plugin: 
 * https://simtk.org/home/uwmodels
 * 
z * Notes:
 * - the restLength property in the associated spring is used as
 * the referenceLength in the UW formulation, where referenceLength
 * is the length of the ligament when the joint is in the reference 
 * position (full extension).
 * 
 */
public class Blankevoort1991AxialLigament extends AxialMaterial {

   public static double DEFAULT_LINEAR_STIFFNESS = 100; // typ constant
   public static double DEFAULT_TRANSITION_STRAIN = 0.06; // typ constant
   public static double DEFAULT_DAMPING = 0.003; // typ constant
   public static double DEFAULT_SLACK_LENGTH = 0.0; // typ zero
   
   protected double linearStiffness = DEFAULT_LINEAR_STIFFNESS;
   protected double transitionStrain = DEFAULT_TRANSITION_STRAIN;
   protected double damping = DEFAULT_DAMPING;
   protected double slackLength = DEFAULT_SLACK_LENGTH;
   
   public static PropertyList myProps =
      new PropertyList (Blankevoort1991AxialLigament.class, AxialMaterial.class);

   static {
      myProps.add ("linearStiffness", "linear portion of force-length curve",
         DEFAULT_LINEAR_STIFFNESS, "%.4g");
      myProps.add ("transitionStrain", "strain at transition from toe to linear",
         DEFAULT_TRANSITION_STRAIN, "%.4g");
      myProps.add ("damping", "damping coefficient",
         DEFAULT_DAMPING, "%.4g");
      myProps.add ("slackLength", "slack length",
         DEFAULT_SLACK_LENGTH, "%.4g");
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public Blankevoort1991AxialLigament () {
   }

   public Blankevoort1991AxialLigament (
      double stiffness, double slackLen, double damping) {
      setLinearStiffness (stiffness);
      setSlackLength (slackLen);
      setDamping (damping);
   }

   @Override
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      double e = (l-slackLength)/slackLength; // strain
      double edot = ldot/slackLength; // strain rate
      double et = transitionStrain;

      double fk; // spring force
      double fd; // damping force

      if (e < 0) {
         // no force if not in tension
         fk = 0;
      }
      else if (0 <= e && e <= et) {
         fk = linearStiffness*(e*e/(2*et));
      }
      else { // e > et
         // linear zone
         fk = linearStiffness*(e-et/2);
      }

      if (e > 0 && edot > 0) {
         fd = damping*edot;
      }
      else {
         fd = 0;
      }
      return fk + fd;
   }

   @Override
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      double e = (l-slackLength)/slackLength; // strain
      double et = transitionStrain;

      if (e < 0) {
         // no force if not in tension
         return 0;
      }
      else if (0 <= e && e <= et) {
         return linearStiffness*e/(et*slackLength);
      }
      else { // e > et
         // linear zone
         return linearStiffness/slackLength;
      }
   }

   @Override
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      double e = (l-slackLength)/slackLength; // strain
      double edot = ldot/slackLength; // strain rate

      if (e > 0 && edot > 0) {
         return damping/slackLength;
      }
      else {
         return 0;
      }
   }

   @Override
   public boolean isDFdldotZero () {
      return damping == 0;
   }

   /* property methods */
   
   public double getLinearStiffness () {
      return linearStiffness;
   }

   public void setLinearStiffness (double linearStiffness) {
      this.linearStiffness = linearStiffness;
   }

   public double getTransitionStrain () {
      return transitionStrain;
   }

   public void setTransitionStrain (double e) {
      this.transitionStrain = e;
   }

   public double getDamping () {
      return damping;
   }

   public void setDamping (double d) {
      this.damping = d;
   }

   public double getSlackLength () {
      return slackLength;
   }

   public void setSlackLength (double sl) {
      this.slackLength = sl;
   }

   public boolean equals(AxialMaterial mat) {
      if (!(mat instanceof Blankevoort1991AxialLigament)) {
         return false;
      }
      Blankevoort1991AxialLigament lmat = (Blankevoort1991AxialLigament)mat;
      if (linearStiffness != lmat.linearStiffness ||
          slackLength != lmat.slackLength ||
          transitionStrain != lmat.transitionStrain ||
          damping != lmat.damping) {
         return false;
      }
      else {
         return super.equals(mat);
      }
   }

   public Blankevoort1991AxialLigament clone() {
      Blankevoort1991AxialLigament mat = (Blankevoort1991AxialLigament)super.clone();
      return mat;
   }

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      slackLength *= s;
      linearStiffness *= s;
      damping *= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      linearStiffness *= s;
      damping *= s;
   }

}
