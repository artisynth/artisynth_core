package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class Model3Factory extends ModelFactoryBase<Model3> {

   public static final int MIN_SUPPORTED_VERSION = 00000;
   public static final int MAX_SUPPORTED_VERSION = 39999;
   
   public Model3Factory() {
      super (Model3.class);
   }
   
   protected Model3Factory (Class<? extends Model3> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      if (!"Model".equals(name)) {
         return false;
      }
      
      int version = getModelVersion (elem);
      if (version >= MIN_SUPPORTED_VERSION && version < MAX_SUPPORTED_VERSION) {
         return true;
      }
      
      return false;
   }

   
   
}
