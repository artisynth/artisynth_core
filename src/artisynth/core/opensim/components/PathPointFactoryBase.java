package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class PathPointFactoryBase<F extends PathPoint> extends HasVisibleObjectOrAppearanceFactory<F> {

   protected PathPointFactoryBase (Class<? extends F> instanceClass) {
      super (instanceClass);
   }

   @Override
   protected boolean parseChild (F comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("location".equals(name)) {
         comp.setLocation (parsePoint3dValue (child));
      } else if ("body".equals(name)) { // OpenSim 3
         comp.setBody (parseTextValue(child));
      } else if ("socket_parent_frame".equals(name)) { // OpenSim 4
         comp.setSocketParentFrame (parseTextValue(child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
