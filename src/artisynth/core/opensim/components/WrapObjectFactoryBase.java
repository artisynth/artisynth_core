package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class WrapObjectFactoryBase<F extends WrapObject> extends HasVisibleObjectOrAppearanceFactory<F> {

   protected WrapObjectFactoryBase (Class<? extends F> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (F comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("xyz_body_rotation".equals (name)) {
         comp.setXYZBodyRotation (parsePoint3dValue (child));
      } else if ("translation".equals(name)) {
         comp.setTranslation (parsePoint3dValue (child));
      } else if ("active".equals(name)) {
         comp.setActive (parseBooleanValue (child));
      } else if ("quadrant".equals(name)) {
         comp.setQuadrant (parseTextValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
