package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class WrapSphereFactory extends WrapObjectFactoryBase<WrapSphere> {

   public WrapSphereFactory () {
      super (WrapSphere.class);
   }
   
   protected WrapSphereFactory(Class<? extends WrapSphere> wrapClass) {
      super(wrapClass);
   }
   
   @Override
   protected boolean parseChild (WrapSphere comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("radius".equals(name)) {
         comp.setRadius (parseDoubleValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
