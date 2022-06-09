package artisynth.core.opensim.components;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.Thelen2003AxialMuscle;

public class Thelen2003Muscle extends MuscleBase {

   // ActiveFibreLengthMuscle
   private double default_activation;   // "Assumed activation level if none is assigned."
   private double default_fiber_length; // "Assumed fiber length, unless otherwise assigned."
   
   private double FmaxTendonStrain;             // "tendon strain at maximum isometric muscle force"
   private double FmaxMuscleStrain;             // "passive muscle strain at maximum isometric muscle force"
   private double KshapeActive;                 // "shape factor for Gaussian active muscle force-length relationship"
   private double KshapePassive;                // "exponential shape factor for passive force-length relationship"
   private double Af;                           // "force-velocity shape factor"
   private double Flen;                         // "maximum normalized lengthening force"
   
   // Thelen2003Muscle
   private double fv_linear_extrap_threshold;   // "fv threshold where linear extrapolation is used"

   private double maximum_pennation_angle;      // "Maximum pennation angle, in radians"

   // Thelen2003Muscle and Deprecated
   private double activation_time_constant;     // "time constant for ramping up muscle activation"   
   private double deactivation_time_constant;   // "time constant for ramping down of muscle activation"

   private double minimum_activation;           // "Lower bound on activation" 
   // Thelen2003Muscle_Deprecated
   private double Vmax;         // "maximum contraction velocity at full activation in fiber lengths/second"
   private double Vmax0;        // "maximum contraction velocity at low activation in fiber lengths/second"
   private double damping;      // "passive damping in the force-velocity relationship"
   
   public Thelen2003Muscle() {
      default_activation = 0.05;
      default_fiber_length = 0.1; // same as optimal fiber length
      FmaxTendonStrain = 0.04;
      FmaxMuscleStrain = 0.6;
      KshapeActive = 0.45;
      KshapePassive = 5.0;
      Af = 0.25;
      Flen = 1.4;
      
      fv_linear_extrap_threshold = 0.95;
      maximum_pennation_angle = Math.acos(0.1);
      activation_time_constant = 0.015;
      deactivation_time_constant = 0.050;
      minimum_activation = 0.01;

      Vmax = 10.0;
      Vmax0 = 5.0;
      damping = 0.05;

   }
   
   @Override
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

   public double getFvLinearExtrapThreshold () {
      return fv_linear_extrap_threshold;
   }

   public void setFvLinearExtrapThreshold (double threshold) {
      this.fv_linear_extrap_threshold = threshold;
   }

   public double getMaximumPennationAngle () {
      return maximum_pennation_angle;
   }

   public void setMaximumPennationAngle (double max) {
      maximum_pennation_angle = max;
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

   public double getMinimumAcivation () {
      return minimum_activation;
   }

   public void setMinimumActivation (double min) {
      minimum_activation = min;
   }

   @Override
   public AxialMaterial createMuscleMaterial() {
      Thelen2003AxialMuscle mat = new Thelen2003AxialMuscle (
         max_isometric_force, optimal_fiber_length,
         tendon_slack_length, pennation_angle_at_optimal);

      mat.setMaxContractionVelocity (max_contraction_velocity);
      mat.setIgnoreTendonCompliance (ignore_tendon_compliance);

      mat.setFmaxTendonStrain (FmaxTendonStrain);
      mat.setFmaxMuscleStrain (FmaxMuscleStrain);
      mat.setKShapeActive (KshapeActive);
      mat.setKShapePassive (KshapePassive);
      mat.setAf (Af);
      mat.setFlen (Flen);
      mat.setFvLinearExtrapThreshold (fv_linear_extrap_threshold);
      mat.setMaxPennationAngle (maximum_pennation_angle);
      mat.setMinimumActivation (minimum_activation);
      return mat;
   }
   
   @Override
   public Thelen2003Muscle clone () {
      return (Thelen2003Muscle)super.clone ();
   }
   
}
