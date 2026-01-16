package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class CoordinateActuatorFactory
   extends ForceBaseFactory<CoordinateActuator>{
   
   public CoordinateActuatorFactory () {
      super(CoordinateActuator.class);
   }
   
   protected CoordinateActuatorFactory(Class<? extends CoordinateActuator> clfClass) {
      super(clfClass);
   }

   @Override
   protected boolean parseChild (CoordinateActuator comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("coordinate".equals(name)) {
         comp.setCoordinate (parseTextValue (child));
      }
      else if ("optimal_force".equals (name)) {
         comp.optimal_force = parseDoubleValue (child);
      }
      else if ("min_control".equals (name)) {
         comp.min_control = parseDoubleValue (child);
      }
      else if ("max_control".equals (name)) {
         comp.max_control = parseDoubleValue (child);
      }
      else {
         success = super.parseChild (comp, child);
      }
      return success;
   }
   
}
