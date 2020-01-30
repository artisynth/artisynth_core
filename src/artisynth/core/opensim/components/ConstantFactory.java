package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class ConstantFactory extends OpenSimObjectFactory<Constant> {

   public ConstantFactory () {
      super (Constant.class);
   }
   
   protected ConstantFactory(Class<? extends Constant> constantClass) {
      super(constantClass);
   }
   
   @Override
   protected boolean parseChild (Constant comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("value".equals(name)) {
         comp.setValue (parseDoubleValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
   
      return success;
   }

}
