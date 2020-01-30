package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class WrapCylinderFactory extends WrapObjectFactoryBase<WrapCylinder> {

   public WrapCylinderFactory () {
      super (WrapCylinder.class);
   }
   
   protected WrapCylinderFactory(Class<? extends WrapCylinder> wrapClass) {
      super(wrapClass);
   }
   
   @Override
   protected boolean parseChild (WrapCylinder comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("length".equals(name)) {
         comp.setLength (parseDoubleValue (child));
      } else if ("radius".equals(name)) {
         comp.setRadius (parseDoubleValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
