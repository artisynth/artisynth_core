package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class ForceBaseFactory<E extends ForceBase> extends HasVisibleObjectFactory<E> {

   protected ForceBaseFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      if ("isDisabled".equals(name)) {
         comp.setDisabled (parseBooleanValue (child));
      } else if ("appliesForce".equals(name)) {
         comp.setAppliesForce (parseBooleanValue (child));
      } else {
         success =  super.parseChild (comp, child);
      }
      
      return success;
   }

}
