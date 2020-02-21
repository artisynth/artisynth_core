package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class FrameGeometryFactory extends GeometryFactory<FrameGeometry> {
   
   public FrameGeometryFactory() {
      super(FrameGeometry.class);
   }
   
   protected FrameGeometryFactory(Class<? extends FrameGeometry> instanceClass) {
      super(instanceClass);
   }
   
   @Override
   protected boolean parseChild (FrameGeometry fg, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      
      if ("display_radius".equals (cname)) {
         fg.setDisplayRadius (parseDoubleValue (child));
      } else {
         success = super.parseChild (fg, child);
      }

      return success;
   }
   
}
