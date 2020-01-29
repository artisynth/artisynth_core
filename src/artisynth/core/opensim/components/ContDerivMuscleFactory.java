package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class ContDerivMuscleFactory extends MuscleBaseFactory<ContDerivMuscle> {

   public ContDerivMuscleFactory() {
      super(ContDerivMuscle.class);
   }
   
   protected ContDerivMuscleFactory (Class<? extends ContDerivMuscle> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      
      // allow muscle or deprecated
      boolean valid = false;
      if ("ContDerivMuscle".equals (name)) {
         valid = true;
      } else if ("ContDerivMuscle_Deprecated".equals(name)) {
         valid = true;
      }
      
      return valid;
   }
   
   @Override
   protected boolean parseChild (ContDerivMuscle comp, Element child) {
      
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
