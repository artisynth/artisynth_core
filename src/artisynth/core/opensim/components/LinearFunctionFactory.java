package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class LinearFunctionFactory extends OpenSimObjectFactory<LinearFunction> {

   public LinearFunctionFactory () {
      super(LinearFunction.class);
   }
   
   protected LinearFunctionFactory(Class<? extends LinearFunction> lfClass) {
      super(lfClass);
   }
   
   @Override
   protected boolean parseChild (LinearFunction comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("coefficients".equals (name)) {
         double[] vals = parseDoubleArrayValue (child);
         comp.setCoefficients (vals);
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
