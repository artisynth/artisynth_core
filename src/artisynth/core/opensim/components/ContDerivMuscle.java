package artisynth.core.opensim.components;

public class ContDerivMuscle extends MuscleBase {
   
   // ActiveFibreLengthMuscle
   private double default_activation;           // "Assumed activation level if none is assigned."
   private double default_fiber_length;         // "Assumed fiber length, unless otherwise assigned."
   
   // ContDerivMuscle_Deprecated
   private double activation_time_constant;     // "time constant for ramping up muscle activation"   
   private double deactivation_time_constant;   // "time constant for ramping down of muscle activation"
   private double Vmax;                         // "maximum contraction velocity at full activation in fiber lengths/second"
   private double Vmax0;                        // "maximum contraction velocity at low activation in fiber lengths/second"
   private double FmaxTendonStrain;             // "tendon strain at maximum isometric muscle force"
   private double FmaxMuscleStrain;             // "passive muscle strain at maximum isometric muscle force"
   private double KshapeActive;                 // "shape factor for Gaussian active muscle force-length relationship"
   private double KshapePassive;                // "exponential shape factor for passive force-length relationship"
   private double damping;                      // "passive damping in the force-velocity relationship"
   private double Af;                           // "force-velocity shape factor"
   private double Flen;                         // "maximum normalized lengthening force"
   
   public ContDerivMuscle() {
      default_activation = 0;
      default_fiber_length = 0;
      activation_time_constant = 0;
      deactivation_time_constant = 0;
      Vmax = 0;
      Vmax0 = 0;
      FmaxTendonStrain = 0;
      FmaxMuscleStrain = 0;
      KshapeActive = 0;
      KshapePassive = 0;
      damping = 0;
      Af = 0;
      Flen = 0;
   }
   
   public double getDefaultActivation () {
      return default_activation;
   }
   
   public void setDefaultActivation (double activation) {
      this.default_activation = activation;
   }

   public double getDefaultFiberLength () {
      return default_fiber_length;
   }

   public void setDefaultFiberLength (double len) {
      this.default_fiber_length = len;
   }
   
   public double getActivationTimeConstant () {
      return activation_time_constant;
   }

   public void setActivationTimeConstant (double tc) {
      this.activation_time_constant = tc;
   }

   public double getDeactivationTimeConstant () {
      return deactivation_time_constant;
   }

   public void setDeactivationTimeConstant (double tc) {
      this.deactivation_time_constant = tc;
   }

   public double getVmax () {
      return Vmax;
   }

   public void setVmax (double vmax) {
      Vmax = vmax;
   }

   public double getVmax0 () {
      return Vmax0;
   }

   public void setVmax0 (double vmax0) {
      Vmax0 = vmax0;
   }

   public double getFmaxTendonStrain () {
      return FmaxTendonStrain;
   }

   public void setFmaxTendonStrain (double fmaxTendonStrain) {
      FmaxTendonStrain = fmaxTendonStrain;
   }

   public double getFmaxMuscleStrain () {
      return FmaxMuscleStrain;
   }

   public void setFmaxMuscleStrain (double fmaxMuscleStrain) {
      FmaxMuscleStrain = fmaxMuscleStrain;
   }

   public double getKshapeActive () {
      return KshapeActive;
   }

   public void setKshapeActive (double kshapeActive) {
      KshapeActive = kshapeActive;
   }

   public double getKshapePassive () {
      return KshapePassive;
   }

   public void setKshapePassive (double kshapePassive) {
      KshapePassive = kshapePassive;
   }

   public double getDamping () {
      return damping;
   }

   public void setDamping (double damping) {
      this.damping = damping;
   }

   public double getAf () {
      return Af;
   }

   public void setAf (double af) {
      Af = af;
   }

   public double getFlen () {
      return Flen;
   }

   public void setFlen (double flen) {
      Flen = flen;
   }
   
   @Override
   public ContDerivMuscle clone () {
      return (ContDerivMuscle)super.clone ();
   }
   
}
