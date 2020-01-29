package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class RigidTendonMuscleFactory extends MuscleBaseFactory<RigidTendonMuscle> {

   public RigidTendonMuscleFactory() {
      super(RigidTendonMuscle.class);
   }
   
   protected RigidTendonMuscleFactory (Class<? extends RigidTendonMuscle> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      
      // allow muscle or deprecated
      boolean valid = false;
      if ("RigidTendonMuscle".equals (name)) {
         valid = true;
      } else if ("RigidTendonMuscle_Deprecated".equals(name)) {
         valid = true;
      }
      
      return valid;
   }

   @Override
   protected boolean parseChild (RigidTendonMuscle comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("active_force_length_curve".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setActiveForceLengthCurve (func);
         } else {
            success = false;
         }
      } else if ("passive_force_length_curve".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setPassiveForceLengthCurve (func);
         } else {
            success = false;
         }
      } else if ("force_velocity_curve".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setForceVelocityCurve (func);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
