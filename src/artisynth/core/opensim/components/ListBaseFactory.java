package artisynth.core.opensim.components;

import org.w3c.dom.Element;

/**
 * Factory for creating instance of a given Set
 */
public abstract class ListBaseFactory<F extends ListBase<C>, C extends OpenSimObject> extends OpenSimObjectFactory<F> {

   private Class<? extends C> componentClass;
   
   protected ListBaseFactory(Class<? extends F> listClass, Class<? extends C> componentClass) {
      super (listClass);
      this.componentClass = componentClass;
   }
   
   @Override
   protected boolean parseChild (F list, Element child) {
      
      boolean success = true;
      
      // parse object in list
      OpenSimObjectFactory<? extends C> factory = 
         findFactory (componentClass, child);
               
      if (factory != null) {
         C obj = factory.parse (child);
         list.add (obj);
      } else {
         success = false;
      }
      
      return success;
   }
   
}
