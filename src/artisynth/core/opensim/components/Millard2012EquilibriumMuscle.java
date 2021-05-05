package artisynth.core.opensim.components;

import artisynth.core.materials.*;

public class Millard2012EquilibriumMuscle extends MuscleBase {

   // ActiveFibreLengthMuscle
   private double fiber_damping;        // "Linear damping of the fiber

   private double default_activation;   // "Assumed activation level if none is assigned."
   private double default_fiber_length; // "Assumed fiber length, unless otherwise assigned."

   private double activation_time_constant;     // "time constant for ramping up muscle activation"   
   private double deactivation_time_constant;   // "time constant for ramping down of muscle activation"
   private double minimum_activation;   // "Activation lower bound"
   private double maximum_pennation_angle;   // "Maximum pennation angle (in radians)

   // Need to implement:
   //  activeForceLengthCurve;
   //  forceVelocityCurve;
   //  fiberForceLengthCurve;
   //  tendonForceLengthCurve;

   public Millard2012EquilibriumMuscle() {
   }
   
   public double getFiber_damping () {
      return fiber_damping;
   }

   public void setFiberDamping (double fd) {
      this.fiber_damping = fd;
   }

   @Override
   public double getDefaultActivation () {
      return default_activation;
   }

   public void setDefaultActivation (double a) {
      this.default_activation = a;
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

   public void setActivationTimeConstant (double atc) {
      this.activation_time_constant = atc;
   }

   public double getDeactivationTimeConstant () {
      return deactivation_time_constant;
   }

   public void setDeactivationTimeConstant (double dtc) {
      this.deactivation_time_constant = dtc;
   }

   public double getMinimumActivation () {
      return minimum_activation;
   }

   public void setMinimumActivation (double min) {
      this.minimum_activation = min;
   }

   public double getMaximumPennationAngle () {
      return maximum_pennation_angle;
   }

   public void setMaximumPennationAngle (double max) {
      this.maximum_pennation_angle = max;
   }

   @Override
   public AxialMuscleMaterial createMuscleMaterial() {
      //      PeckAxialMuscle mat = new PeckAxialMuscle ();
      //      mat.setForceScaling (1);
      //      mat.setMaxForce (max_isometric_force);
      //      mat.setOptLength (optimal_fiber_length+tendon_slack_length);
      //      mat.setTendonRatio (tendon_slack_length/(optimal_fiber_length+tendon_slack_length));
      ConstantAxialMuscle mat = new ConstantAxialMuscle ();
      mat.setMaxForce (max_isometric_force);
      mat.setForceScaling (1.0);
      return mat;
   }
   
   @Override
   public Millard2012EquilibriumMuscle clone () {
      return (Millard2012EquilibriumMuscle)super.clone ();
   }
   
}
