package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class LigamentFactory extends ForceSpringBaseFactory<Ligament> {

   public LigamentFactory() {
      super(Ligament.class);
   }
   
   protected LigamentFactory (Class<? extends Ligament> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (Ligament comp, Element child) {
      boolean success = false;
      
      String name = getNodeName (child);
      
      if ("resting_length".equals (name)) {
         comp.setRestingLength (parseDoubleValue (child));
      } else if ("pcsa_force".equals(name)) {
         comp.setPCSAForce (parseDoubleValue (child));
      } 
      // maybe it's the force-length curve
      else if (child.hasAttribute ("name") && child.getAttribute ("name").equals ("force_length_curve")) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setForceLengthCurve (func);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
         
      }
      
      return success;
   }

}
