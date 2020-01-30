package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class MultiplierFunctionFactory extends OpenSimObjectFactory<MultiplierFunction> {

   public MultiplierFunctionFactory () {
      super (MultiplierFunction.class);
   }
   
   protected MultiplierFunctionFactory(Class<? extends MultiplierFunction> constantClass) {
      super(constantClass);
   }
   
   @Override
   protected boolean parseChild (MultiplierFunction comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("scale".equals(name)) {
         comp.setScale (parseDoubleValue (child));
      } else if ("function".equals(name)) {
         
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setFunction (func);
         } else {
            success = false;
         }
         
      } else {
         success = super.parseChild (comp, child);
      }
   
      return success;
   }

}
