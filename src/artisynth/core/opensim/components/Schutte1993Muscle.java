package artisynth.core.opensim.components;

public class Schutte1993Muscle extends MuscleBase {

   // ActiveFibreLengthMuscle
   private double default_activation;   // "Assumed activation level if none is assigned."
   private double default_fiber_length; // "Assumed fiber length, unless otherwise assigned."
   
   // Schutte1993Muscle_Deprecated
   private double time_scale;                     // "Scale factor for normalizing time"
   private double activation1;                    // "Parameter used in time constant of ramping up of muscle force"
   private double activation2;                    // "Parameter used in time constant of ramping up and ramping down of muscle force"
   private double damping;                        // "Damping factor related to maximum contraction velocity"
   private FunctionBase tendon_force_length_curve;  // "Function representing force-length behavior of tendon"
   private FunctionBase active_force_length_curve;  // "Function representing active force-length behavior of muscle fibers"
   private FunctionBase passive_force_length_curve; // "Function representing passive force-length behavior of muscle fibers"
   
   public Schutte1993Muscle() {
      default_activation = 0;
      default_fiber_length = 0;
      time_scale = 1;
      activation1 = 0;
      activation2 = 0;
      damping = 0;
      tendon_force_length_curve = new Constant (0);
      active_force_length_curve = new Constant (0);
      passive_force_length_curve = new Constant (0);
   }
   
   public void setTimeScale(double scale) {
      time_scale = scale;
   }
   
   public double getTimeScale() {
      return time_scale;
   } 
   
   public void setActivation1(double activation) {
      activation1 = activation;
   }
   
   public double getActivation1() {
      return activation1;
   } 

   public void setActivation2(double activation) {
      activation2 = activation;
   }
   
   public double getActivation2() {
      return activation2;
   } 
   
   public void setDamping(double damp) {
      damping = damp;
   }
   
   public double getDamping() {
      return damping;
   }

   public double getDefaultActivation () {
      return default_activation;
   }

   public void setDefaultActivation (double act) {
      this.default_activation = act;
   }

   public double getDefaultFiberLength () {
      return default_fiber_length;
   }

   public void setDefaultFiberLength (double flen) {
      this.default_fiber_length = flen;
   }

   public FunctionBase getTendonForceLengthCurve () {
      return tendon_force_length_curve;
   }

   public void setTendonForceLengthCurve (FunctionBase curve) {
      this.tendon_force_length_curve = curve;
      tendon_force_length_curve.setParent (this);
   }

   public FunctionBase getActiveForceLengthCurve () {
      return active_force_length_curve;
   }

   public void setActiveForceLengthCurve (FunctionBase curve) {
      this.active_force_length_curve = curve;
      active_force_length_curve.setParent (this);
   }

   public FunctionBase getPassiveForceLengthCurve () {
      return passive_force_length_curve;
   }

   public void setPassiveForceLengthCurve (FunctionBase curve) {
      this.passive_force_length_curve = curve;
      passive_force_length_curve.setParent (this);
   }
   
   @Override
   public Schutte1993Muscle clone () {
      Schutte1993Muscle muscle = (Schutte1993Muscle) super.clone ();
      if (tendon_force_length_curve != null) {
         muscle.setTendonForceLengthCurve (tendon_force_length_curve.clone ());
      }
      if (active_force_length_curve != null) {
         muscle.setActiveForceLengthCurve (active_force_length_curve.clone ());
      }
      if (passive_force_length_curve != null) {
         muscle.setPassiveForceLengthCurve (passive_force_length_curve.clone ());
      }
      return muscle;
   }
   
}
