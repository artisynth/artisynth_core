package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class PhysicalOffsetFrameFactory extends PhysicalFrameFactory<PhysicalOffsetFrame> {
   
   public PhysicalOffsetFrameFactory() {
      super(PhysicalOffsetFrame.class);
   }
   
   protected PhysicalOffsetFrameFactory(Class<? extends PhysicalOffsetFrame> instanceClass) {
      super(instanceClass);
   }
   
   @Override
   protected boolean parseChild (PhysicalOffsetFrame frame, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      if ("translation".equals(cname)) {
         frame.setTranslation (parseVector3dValue (child));
      } else if ("orientation".equals(cname)) {
         frame.setOrientation (parseOrientationValue(child));
      } else if ("socket_parent".equals(cname)) {
         frame.setSocketParent (parseTextValue(child));
      } else {
         success = super.parseChild (frame, child);
      }

      return success;
   }
   
      
}
