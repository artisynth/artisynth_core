package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class CoordinateLimitForceFactory
   extends ForceBaseFactory<CoordinateLimitForce>{
   
   public CoordinateLimitForceFactory () {
      super(CoordinateLimitForce.class);
   }
   
   protected CoordinateLimitForceFactory(Class<? extends CoordinateLimitForce> clfClass) {
      super(clfClass);
   }

   @Override
   protected boolean parseChild (CoordinateLimitForce comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("coordinate".equals(name)) {
         comp.setCoordinate (parseTextValue (child));
      }
      else if ("upper_limit".equals (name)) {
         comp.setUpperLimit (parseDoubleValue (child));
      }
      else if ("upper_stiffness".equals (name)) {
         comp.setUpperStiffness (parseDoubleValue (child));
      }
      else if ("lower_limit".equals (name)) {
         comp.setLowerLimit (parseDoubleValue (child));
      }
      else if ("lower_stiffness".equals (name)) {
         comp.setLowerStiffness (parseDoubleValue (child));
      }
      else if ("damping".equals (name)) {
         comp.setDamping (parseDoubleValue (child));
      }
      else if ("transition".equals (name)) {
         comp.setTransition (parseDoubleValue (child));
      }
      else {
         success = super.parseChild (comp, child);
      }
      return success;
   }
   
}
