package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class MarkerFactory extends HasVisibleObjectOrAppearanceFactory<Marker> {

   public MarkerFactory() {
      super(Marker.class);
   }
   
   protected MarkerFactory (Class<? extends Marker> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (Marker comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("body".equals(name)) {
         comp.setBody (parseTextValue (child));
      } else if ("socket_parent_frame".equals(name)) {
         comp.setSocketParentFrame (parseTextValue(child));
      } else if ("location".equals(name)) {
         comp.setLocation (parsePoint3dValue (child));
      } else if ("fixed".equals(name)) {
         comp.setFixed (parseBooleanValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
