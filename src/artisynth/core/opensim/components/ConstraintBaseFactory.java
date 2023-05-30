package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class ConstraintBaseFactory<E extends ConstraintBase>
   extends OpenSimObjectFactory<E> {

   protected ConstraintBaseFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      // opensim 4
      if ("isEnforced".equals(name)) {
         comp.setIsEnforced (parseBooleanValue (child));
      }
      // opensim 3
      else if ("isDisabled".equals(name)) {
         comp.setIsEnforced (!parseBooleanValue (child));
      }
      else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
