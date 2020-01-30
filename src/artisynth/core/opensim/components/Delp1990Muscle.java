package artisynth.core.opensim.components;

public class Delp1990Muscle extends MuscleBase {

   private double time_scale; // "Scale factor for normalizing time"
   private double activation1; // "Parameter used in time constant of ramping up of muscle force"
   private double activation2; // "Parameter used in time constant of ramping up and ramping down of muscle force"
   private FunctionBase tendon_force_length_curve; // "Function representing force-length behavior of tendon"
   private FunctionBase active_force_length_curve; // "Function representing active force-length behavior of muscle fibers"
   private FunctionBase passive_force_length_curve; // "Function representing passive force-length behavior of muscle fibers"
   private FunctionBase force_velocity_curve; // "Function representing force-velocity behavior of muscle fibers"
 
   public Delp1990Muscle () {
      time_scale = 1;
      activation1 = 0;
      activation2 = 0;
      tendon_force_length_curve = new Constant (0);
      active_force_length_curve = new Constant (0);
      passive_force_length_curve = new Constant (0);
      force_velocity_curve = new Constant(0);
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
   
   public FunctionBase getTendonForceLengthCurve () {
      return tendon_force_length_curve;
   }

   public void setTendonForceLengthCurve (FunctionBase curve) {
      tendon_force_length_curve = curve;
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

   public FunctionBase getForceVelocityCurve () {
      return force_velocity_curve;
   }

   public void setForceVelocityCurve (FunctionBase curve) {
      this.force_velocity_curve = curve;
      force_velocity_curve.setParent (this);
   }
   
   @Override
   public Delp1990Muscle clone () {
      Delp1990Muscle muscle = (Delp1990Muscle)super.clone ();
      
      if (tendon_force_length_curve != null) {
         muscle.setTendonForceLengthCurve (tendon_force_length_curve.clone ());
      }
      if (active_force_length_curve != null) {
         muscle.setActiveForceLengthCurve (active_force_length_curve.clone ());
      }
      if (passive_force_length_curve != null) {
         muscle.setPassiveForceLengthCurve (passive_force_length_curve.clone ());
      }
      if (force_velocity_curve != null) {
         muscle.setForceVelocityCurve (force_velocity_curve.clone ());
      }
      return muscle;
   }
}
