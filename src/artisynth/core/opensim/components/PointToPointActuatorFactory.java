package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class PointToPointActuatorFactory
   extends ForceBaseFactory<PointToPointActuator>{
   
   public PointToPointActuatorFactory () {
      super(PointToPointActuator.class);
   }
   
   protected PointToPointActuatorFactory(Class<? extends PointToPointActuator> clfClass) {
      super(clfClass);
   }

   @Override
   protected boolean parseChild (PointToPointActuator comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("bodyA".equals(name)) {
         comp.bodyA = parseTextValue (child);
      }
      else if ("bodyB".equals(name)) {
         comp.bodyB = parseTextValue (child);
      }
      else if ("pointA".equals(name)) {
         comp.pointA = parsePoint3dValue (child);
      }
      else if ("pointB".equals(name)) {
         comp.pointB = parsePoint3dValue (child);
      }
      else if ("points_are_global".equals(name)) {
         comp.points_are_global = parseBooleanValue (child);
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
