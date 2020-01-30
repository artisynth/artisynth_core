package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class HasVisibleObjectFactory<E extends HasVisibleObject> extends VisibleBaseFactory<E> {

   protected HasVisibleObjectFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E body, Element child) {
      boolean success = true;

      String cname = getNodeName(child);

         if ("VisibleObject".equals(cname)) {
         OpenSimObjectFactory<? extends VisibleObject> vof = getFactory (VisibleObject.class);
         if (vof != null) {
            VisibleObject vo = vof.parse(child);
            body.setVisibleObject (vo);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (body, child);
      }
      
      return success;
   }

}
