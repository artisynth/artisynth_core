package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class WrapTorusFactory extends WrapObjectFactoryBase<WrapTorus> {

   public WrapTorusFactory () {
      super(WrapTorus.class);
   }
   
   protected WrapTorusFactory(Class<? extends WrapTorus> wrapClass) {
      super(wrapClass);
   }
   
   @Override
   protected boolean parseChild (WrapTorus comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("inner_radius".equals(name)) {
         comp.setInnerRadius (parseDoubleValue (child));
      } else if ("outer_radius".equals(name)) {
         comp.setOuterRadius (parseDoubleValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
