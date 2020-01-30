package artisynth.core.opensim.components;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.ConstantAxialMuscle;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.MultiPointSpring;

public abstract class MuscleBase extends ForceSpringBase {
   
      
   // properties from OpenSim docs:
   
   // Actuator
   private double min_control; // "Minimum allowed value for control signal. Used primarily when solving ""for control values."
   private double max_control; // "Maximum allowed value for control signal. Used primarily when solving ""for control values."
   
   // Path Actuator
   // OpenSim_DECLARE_UNNAMED_PROPERTY (GeometryPath,"The set of points defining the path of the muscle.")
   private double optimal_force;                // "The maximum force this actuator can produce."
   
   // Muscle
   private double max_isometric_force;          // "Maximum isometric force that the fibers can generate"
   private double optimal_fiber_length;         // "Optimal length of the muscle fibers"
   private double tendon_slack_length;          // "Resting length of the tendon"
   private double pennation_angle_at_optimal;   // "Angle between tendon and fibers at optimal fiber length expressed in radians"
   private double max_contraction_velocity;     // "Maximum contraction velocity of the fibers, in optimal fiberlengths/second"
   private boolean ignore_tendon_compliance;    // "Compute muscle dynamics ignoring tendon compliance. Tendon is assumed to be rigid."
   private boolean ignore_activation_dynamics;  // "Compute muscle dynamics ignoring activation dynamics. Activation is equivalent to excitation."  
   
   protected MuscleBase() {
      super();
      min_control = 0;
      max_control = 1;
      optimal_force = 0;
      max_isometric_force = 0;
      optimal_fiber_length = 0;
      tendon_slack_length = 0;
      pennation_angle_at_optimal = 0;
      max_contraction_velocity = 0;
      ignore_activation_dynamics = false;
      ignore_tendon_compliance = false;

   }
   
   public String getMuscleClassName() {
      return this.getClass ().getCanonicalName ();
   }
   
   public void setMinControl(double control) {
      min_control = control;
   }
   
   public double getMinControl() {
      return min_control;
   }
   
   public void setMaxControl(double control) {
      max_control = control;
   }
   
   public double getMaxControl() {
      return max_control;
   } 
   
   public void setMaxIsometricForce(double force) {
      max_isometric_force = force;
   }
   
   public double getMaxIsometricForce() {
      return max_isometric_force;
   }
   
   public void setOptimalForce(double force) {
      optimal_force = force;
   }
   
   public double getOptimalForce() {
      return optimal_force;
   }
   
   public void setOptimalFiberLength(double len) {
      optimal_fiber_length = len;
   }
   
   public double getOptimalFiberLength() {
      return optimal_fiber_length;
   }
   
   public void setTendonSlackLength(double slack) {
      tendon_slack_length = slack;
   }
   public double getTendonSlackLength() {
      return tendon_slack_length;
   }
   
   public void setPennationAngle(double angle) {
      pennation_angle_at_optimal = angle;
   }
   
   public double getPennationAngle() {
      return pennation_angle_at_optimal;
   }
   
   public void setMaxContractionVelocity(double velocity) {
      max_contraction_velocity = velocity;
   }
   
   public double getMaxContractionVelocity() {
      return max_contraction_velocity;
   }

   public void setIgnoreTendonCompliance(boolean ignore) {
      ignore_tendon_compliance = ignore;
   }
   
   public boolean getIgnoreTendonCompliance() {
      return ignore_tendon_compliance;
   }
   
   public void setIgnoreActivationDynamics(boolean ignore) {
      ignore_activation_dynamics = ignore;
   }
   
   public boolean getIgnoreActivationDynamics() {
      return ignore_activation_dynamics;
   }
   
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
   public AxialMaterial createMaterial () {
      return createMuscleMaterial ();
   }
   
   @Override
   protected MultiPointSpring createDefaultSpring () {
      return new MultiPointMuscle ();
   }
   
   
}
