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
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("resting_length".equals (name)) {
         comp.setRestingLength (parseDoubleValue (child));
      } else if ("pcsa_force".equals(name)) {
         comp.setPCSAForce (parseDoubleValue (child));
      } 
      // maybe it's another force-length curve
      else if (child.hasAttribute ("name") && 
               child.getAttribute ("name").equals ("force_length_curve")) {
         // try to parse function, could be empty
         OpenSimObjectFactory<? extends FunctionBase> factory =
           findFactory (FunctionBase.class, child);
         FunctionBase func = null;
         if (factory != null) {
            func = factory.parse (child);
            if (func != null) {
               comp.setForceLengthCurve (func);
            }
         }
         if (func == null) {
            func = parseFunctionValue (child);
            if (func != null) {
               comp.setForceLengthCurve (func);
            }
         }
         success = func != null;
      } 
      else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
