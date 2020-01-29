package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class MovingPathPointFactory extends PathPointFactoryBase<MovingPathPoint>{
   
   public MovingPathPointFactory () {
      super(MovingPathPoint.class);
   }
   
   protected MovingPathPointFactory(Class<? extends MovingPathPoint> cppClass) {
      super(cppClass);
   }

   @Override
   protected boolean parseChild (MovingPathPoint comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("x_coordinate".equals(name)) {
         comp.setXCoordinate (parseTextValue(child));
      } else if ("x_location".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setXLocation (func);
         } else {
            success = false;
         }
      } else if ("y_coordinate".equals(name)) {
         comp.setYCoordinate (parseTextValue(child));
      } else if ("y_location".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setYLocation (func);
         } else {
            success = false;
         }
      } else if ("z_coordinate".equals(name)) {
         comp.setZCoordinate (parseTextValue(child));
      } else if ("z_location".equals(name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setZLocation (func);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
