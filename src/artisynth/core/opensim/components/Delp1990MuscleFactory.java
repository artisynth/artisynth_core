package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class Delp1990MuscleFactory extends MuscleBaseFactory<Delp1990Muscle> {

   public Delp1990MuscleFactory() {
      super(Delp1990Muscle.class);
   }
   
   protected Delp1990MuscleFactory (Class<? extends Delp1990Muscle> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      
      // allow muscle or deprecated
      boolean valid = false;
      if ("Delp1990Muscle".equals (name)) {
         valid = true;
      } else if ("Delp1990Muscle_Deprecated".equals(name)) {
         valid = true;
      }
      
      return valid;
   }

   @Override
   protected boolean parseChild (Delp1990Muscle comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("time_scale".equals (name)) {
         comp.setTimeScale (parseDoubleValue (child));
      } else if ("activation1".equals(name)) {
         comp.setActivation1 (parseDoubleValue (child));
      } else if ("activation2".equals(name)) {
         comp.setActivation2 (parseDoubleValue (child));
      } else if ("tendon_force_length_curve".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setTendonForceLengthCurve (func);
         } else {
            success = false;
         }
      } else if ("active_force_length_curve".equals(name)) {
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
