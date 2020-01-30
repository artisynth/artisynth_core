package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class WrapEllipsoidFactory extends WrapObjectFactoryBase<WrapEllipsoid> {

   public WrapEllipsoidFactory () {
      super (WrapEllipsoid.class);
   }
   
   protected WrapEllipsoidFactory(Class<? extends WrapEllipsoid> wrapClass) {
      super(wrapClass);
   }
   
   @Override
   protected boolean parseChild (WrapEllipsoid comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("dimensions".equals(name) ) {
         comp.setDimensions (parseVector3dValue(child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
