package artisynth.core.opensim.components;

public class RigidTendonMuscle extends MuscleBase {

   // RigidTendonMuscle
   private FunctionBase active_force_length_curve; // "Function representing active force-length behavior of muscle fibers"
   private FunctionBase passive_force_length_curve; // "Function representing passive force-length behavior of muscle fibers"
   private FunctionBase force_velocity_curve; // "Function representing force-velocity behavior of muscle fibers"
   
   public RigidTendonMuscle() {
      active_force_length_curve = new Constant (0);
      passive_force_length_curve = new Constant (0);
      force_velocity_curve = new Constant (0);
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
   public RigidTendonMuscle clone () {
      RigidTendonMuscle muscle = (RigidTendonMuscle)super.clone ();
      
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
