package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class MuscleBaseFactory<E extends MuscleBase> extends ForceSpringBaseFactory<E> {

   protected MuscleBaseFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
    
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("min_control".equals(name)) {
         comp.setMinControl (parseDoubleValue (child));
      } else if ("max_control".equals(name)) {
         comp.setMaxControl (parseDoubleValue (child));
      } else if ("optimal_force".equals(name)) {
         comp.setOptimalForce (parseDoubleValue (child));
      } else if ("max_isometric_force".equals(name)) {
         comp.setMaxIsometricForce (parseDoubleValue (child));
      } else if ("optimal_fiber_length".equals(name)) {
         comp.setOptimalFiberLength (parseDoubleValue (child));
      } else if ("tendon_slack_length".equals(name)) {
         comp.setTendonSlackLength (parseDoubleValue (child));
      } else if ("pennation_angle_at_optimal".equals(name)) {
         comp.setPennationAngle (parseDoubleValue (child));
      } else if ("max_contraction_velocity".equals(name)) {
         comp.setMaxContractionVelocity (parseDoubleValue (child));
      } else if ("ignore_activation_dynamics".equals(name)) {
         comp.setIgnoreActivationDynamics (parseBooleanValue (child));
      } else if ("ignore_tendon_compliance".equals(name)) {
         comp.setIgnoreTendonCompliance (parseBooleanValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
   

}
