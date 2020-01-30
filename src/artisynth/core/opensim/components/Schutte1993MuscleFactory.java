package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class Schutte1993MuscleFactory extends MuscleBaseFactory<Schutte1993Muscle> {

   public Schutte1993MuscleFactory() {
      super(Schutte1993Muscle.class);
   }
   
   protected Schutte1993MuscleFactory (Class<? extends Schutte1993Muscle> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      
      // allow muscle or deprecated
      boolean valid = false;
      if ("Schutte1993Muscle".equals (name)) {
         valid = true;
      } else if ("Schutte1993Muscle_Deprecated".equals(name)) {
         valid = true;
      }
      
      return valid;
   }

   @Override
   protected boolean parseChild (Schutte1993Muscle comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("default_activation".equals (name)) {
         comp.setDefaultActivation (parseDoubleValue (child));
      } else if ("default_fiber_length".equals(name)) {
         comp.setDefaultFiberLength (parseDoubleValue (child));
      } else if ("time_scale".equals (name)) {
         comp.setTimeScale (parseDoubleValue (child));
      } else if ("activation1".equals(name)) {
         comp.setActivation1 (parseDoubleValue (child));
      } else if ("activation2".equals(name)) {
         comp.setActivation2 (parseDoubleValue (child));
      } else if ("damping".equals(name)) {
         comp.setDamping(parseDoubleValue (child));
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
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
