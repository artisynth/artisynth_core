package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class FrameFactory<E extends Frame> extends HasVisibleObjectOrAppearanceFactory<E> {
   
   protected FrameFactory(Class<? extends E> instanceClass) {
      super(instanceClass);
   }
   
   @Override
   protected boolean parseChild (E body, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      if ("FrameGeometry".equals(cname)) {
         OpenSimObjectFactory<? extends FrameGeometry> factory = getFactory (FrameGeometry.class);
         if (factory != null) {
            FrameGeometry fg = factory.parse(child);
            body.setFrameGeometry(fg);
         } else {
            success = false;
         }
      } else if ("attached_geometry".equals(cname)) {
         OpenSimObjectFactory<? extends GeometryList> factory = getFactory (GeometryList.class);
         if (factory != null) {
            GeometryList set = factory.parse (child);
            body.setAttachedGeometry (set);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (body, child);
      }

      return success;
   }
   
}
