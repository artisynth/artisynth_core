package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class CoordinateFactory extends OpenSimObjectFactory<Coordinate> {

   public CoordinateFactory () {
      super (Coordinate.class);
   }
   
   protected CoordinateFactory(Class<? extends Coordinate> coordinateClass) {
      super(coordinateClass);
   }
   
   @Override
   protected boolean parseChild (Coordinate comp, Element child) {
     
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("motion_type".equals(name) ) {
         comp.setMotionType (parseTextValue (child));
      } else if ("default_value".equals(name)) {
         comp.setDefaultValue (parseDoubleValue(child));
      } else if ("initial_value".equals(name)) {
         comp.setDefaultValue (parseDoubleValue(child));
      } else if ("default_speed_value".equals(name)) {
         comp.setDefaultSpeedValue (parseDoubleValue(child));
      } else if ("range".equals(name)) {
         double[] range = parseDoubleArrayValue (child);
         comp.setRange (range[0], range[1]);
      } else if ("clamped".equals(name)) {
         comp.setClamped (parseBooleanValue(child));
      } else if ("locked".equals(name)) {
         comp.setLocked (parseBooleanValue(child));
      } else if ("prescribed".equals(name)) {
         comp.setPrescribed (parseBooleanValue(child));
      } else if ("prescribed_function".equals (name)) {
         // allowed to be empty
         if (child.hasChildNodes ()) {
            FunctionBase func = parseFunctionValue(child);
            if (func != null) {
               comp.setPrescribedFunction (func);
            } else {
               success = false;
            }
         }
      } else if ("is_free_to_satisfy_constraints".equals(name)) {
         comp.setFreeToSatisfyConstraints (parseBooleanValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
      
   }

}
