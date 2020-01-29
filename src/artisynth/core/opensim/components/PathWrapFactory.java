package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class PathWrapFactory extends OpenSimObjectFactory<PathWrap> {

   public PathWrapFactory() {
      super(PathWrap.class);
   }
   
   protected PathWrapFactory (Class<? extends PathWrap> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (PathWrap comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      if ("wrap_object".equals (name)) {
         comp.setWrapObject (parseTextValue (child));
      } else if ("method".equals(name)) {
         comp.setMethod (parseTextValue (child));
      } else if ("range".equals (name)) {
         int[] vals = parseIntegerArrayValue (child);
         comp.setRange (vals[0], vals[1]);
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   

}
