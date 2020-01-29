package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class Model4Factory extends ModelFactoryBase<Model4> {

   public static final int MIN_SUPPORTED_VERSION = 40000;
   public static final int MAX_SUPPORTED_VERSION = 49999;
   
   public Model4Factory() {
      super (Model4.class);
   }
   
   protected Model4Factory (Class<? extends Model4> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      if (!"Model".equals(name)) {
         return false;
      }
      
      // check version in parents
      int version = getModelVersion (elem);
      if (version >= MIN_SUPPORTED_VERSION && version < MAX_SUPPORTED_VERSION) {
         return true;
      }
      
      return false;
   }
   
   @Override
   protected boolean parseChild (Model4 comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("JointSet".equals(name)) {
         
         OpenSimObjectFactory<? extends JointSet> factory = getFactory (JointSet.class);
         if (factory != null) {
            comp.setJointSet (factory.parse (child));
         } else {
            success = false;
         }
         
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

   
   
}
