package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class ForceSpringBaseFactory<E extends ForceSpringBase> extends ForceBaseFactory<E> {

   protected ForceSpringBaseFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      if ("GeometryPath".equals(name)) {
         OpenSimObjectFactory<? extends GeometryPath> factory = getFactory(GeometryPath.class);
         if (factory != null) {
            comp.setGeometryPath (factory.parse (child));
         } else {
            success = false;
         }
      } else {
         success =  super.parseChild (comp, child);
      }
      
      return success;
   }

}
