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
      
      //  In ActivationFiberLengthMuscle
      if ("default_activation".equals (name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setDefaultActivation (value);
         }
      }
      else if ("default_fiber_length".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setDefaultFiberLength (value);
         }
      }
      // In Thelen2003Muscle
      else if ("FmaxTendonStrain".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setFmaxTendonStrain (value);
         }
      }
      else if ("FmaxMuscleStrain".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setFmaxMuscleStrain (value);
         }
      } 
      else if ("KshapeActive".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setKshapeActive (value);
         }
      }
      else if ("KshapePassive".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setKshapePassive (value);
         }
      }
      else if ("Af".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setAf (value);
         }
      }
      else if ("Flen".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setFlen (value);
         }
      }
      else if ("fv_linear_extrap_threshold".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setFvLinearExtrapThreshold (value);
         }
      }
      else if ("maximum_pennation_angle".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setMaximumPennationAngle (value);
         }
      }
      else if ("activation_time_constant".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setActivationTimeConstant (value);
         }
      }
      else if ("deactivation_time_constant".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setDeactivationTimeConstant (value);
         }
      }
      else if ("minimum_activation".equals(name)) { 
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setMinimumActivation (value);
         }
      }
      // In deprecated Thelen2003Muscle
      else if ("Vmax".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setVmax (value);
         }
      }
      else if ("Vmax0".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setVmax0 (value);
         }
      }
      else if ("damping".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.setDamping(value);
         }
      }
      else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
