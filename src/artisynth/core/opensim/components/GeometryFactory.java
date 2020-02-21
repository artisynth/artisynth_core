package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class GeometryFactory<E extends Geometry> extends HasAppearanceFactory<E> {
   
   protected GeometryFactory(Class<? extends E> instanceClass) {
      super(instanceClass);
   }
   
   static boolean gave_input_transform_warning = false;
   
   @Override
   protected boolean parseChild (E fg, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      
      if ("socket_frame".equals(cname)) {
         fg.setSocketFrame (parseTextValue(child));
      } else if ("scale_factors".equals(cname)) {
         fg.setScaleFactors (parseVector3dValue (child));
      } 
      else if ("input_transform".equals(cname)) {
         if (!gave_input_transform_warning) {
            System.out.println ( instanceClass + ": ignoring input_transform");
            gave_input_transform_warning = true;
         }
      } else {
         success = super.parseChild (fg, child);
      }

      return success;
   }
   
}
