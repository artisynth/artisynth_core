package artisynth.core.opensim.components;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Factory for creating instance of a given Set
 */
public abstract class SetBaseFactory<F extends SetBase<C>, C extends OpenSimObject> extends OpenSimObjectFactory<F> {

   private Class<? extends C> componentClass;
   
   protected SetBaseFactory(Class<? extends F> setClass, Class<? extends C> componentClass) {
      super (setClass);
      this.componentClass = componentClass;
   }
   
   @Override
   protected boolean parseChild (F comp, Element child) {
      
      // objects and groups
      String name = getNodeName (child);
      
      boolean success = true;
      
      if ("objects".equals(name)) {
         // parse objects
         
         Node grandChild = child.getFirstChild ();
         while (grandChild != null) {
            if (grandChild.getNodeType () == Node.ELEMENT_NODE) {
               
               OpenSimObjectFactory<? extends C> factory = 
                  findFactory (componentClass, (Element)grandChild);
               
               if (factory != null) {
                  C obj = factory.parse ((Element)grandChild);
                  comp.add (obj);
               } else {
                  success = false;
               }
               
            }
            grandChild = grandChild.getNextSibling ();
         }
         
      } else if ("groups".equals (name)) {
         // parse groups
         
         Node grandChild = child.getFirstChild ();
         while (grandChild != null) {
            if (grandChild.getNodeType () == Node.ELEMENT_NODE) {
               
               OpenSimObjectFactory<? extends ObjectGroup> factory = 
                  findFactory (ObjectGroup.class, (Element)grandChild);
               
               if (factory != null) {
                  ObjectGroup group = factory.parse ((Element)grandChild);
                  comp.addGroup (group);
               } else {
                  success = false;
               }
               
            }
            grandChild = grandChild.getNextSibling ();
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
