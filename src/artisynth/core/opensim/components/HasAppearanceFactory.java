package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class HasAppearanceFactory<E extends HasAppearance> extends OpenSimObjectFactory<E> {

   protected HasAppearanceFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E body, Element child) {
      boolean success = true;

      String cname = getNodeName(child);

         if ("Appearance".equals(cname)) {
         OpenSimObjectFactory<? extends Appearance> factory = getFactory (Appearance.class);
         if (factory != null) {
            Appearance a = factory.parse(child);
            body.setAppearance (a);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (body, child);
      }
      
      return success;
   }

}
