package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class PhysicalFrameFactory<E extends PhysicalFrame> extends FrameFactory<E> {
   
   protected PhysicalFrameFactory(Class<? extends E> instanceClass) {
      super(instanceClass);
   }
   
   @Override
   protected boolean parseChild (E body, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      if ("WrapObjectSet".equals(cname)) {
         OpenSimObjectFactory<? extends WrapObjectSet> factory = getFactory (WrapObjectSet.class);
         if (factory != null) {
            WrapObjectSet wos = factory.parse(child);
            body.setWrapObjectSet (wos);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (body, child);
      }

      return success;
   }
   
}
