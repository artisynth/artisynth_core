package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class Thelen2003MuscleFactory extends MuscleBaseFactory<Thelen2003Muscle> {

   public Thelen2003MuscleFactory() {
      super(Thelen2003Muscle.class);
   }
   
   protected Thelen2003MuscleFactory (Class<? extends Thelen2003Muscle> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      
      // allow muscle or deprecated
      boolean valid = false;
      if ("Thelen2003Muscle".equals (name)) {
         valid = true;
      } else if ("Thelen2003Muscle_Deprecated".equals(name)) {
         valid = true;
      }
      
      return valid;
   }
   
   @Override
   protected boolean parseChild (Thelen2003Muscle comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("default_activation".equals (name)) {
         comp.setDefaultActivation (parseDoubleValue (child));
      } else if ("default_fiber_length".equals(name)) {
         comp.setDefaultFiberLength (parseDoubleValue (child));
      } else if ("activation_time_constant".equals(name)) {
         comp.setActivationTimeConstant (parseDoubleValue (child));
      } else if ("deactivation_time_constant".equals(name)) {
         comp.setDeactivationTimeConstant (parseDoubleValue (child));
      } else if ("FmaxTendonStrain".equals(name)) {
         comp.setFmaxTendonStrain (parseDoubleValue (child));
      } else if ("FmaxMuscleStrain".equals(name)) {
         comp.setFmaxMuscleStrain (parseDoubleValue (child));
      } else if ("KshapeActive".equals(name)) {
         comp.setKshapeActive (parseDoubleValue (child));
      } else if ("KshapePassive".equals(name)) {
         comp.setKshapePassive (parseDoubleValue (child));
      } else if ("Af".equals(name)) {
         comp.setAf (parseDoubleValue (child));
      } else if ("Flen".equals(name)) {
         comp.setFlen (parseDoubleValue (child));
      } else if ("fv_linear_extrap_threshold".equals(name)) {
         comp.setFvLinearExtrapThreshold (parseDoubleValue (child));
      } else if ("Vmax".equals(name)) {
         comp.setVmax (parseDoubleValue (child));
      } else if ("Vmax0".equals(name)) {
         comp.setVmax0 (parseDoubleValue (child));
      } else if ("damping".equals(name)) {
         comp.setDamping(parseDoubleValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
