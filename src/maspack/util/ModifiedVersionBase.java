package maspack.util;

/**
 * Simple class to track a "modification" version
 * @author Antonio
 */
public abstract class ModifiedVersionBase implements Versioned {

   int version;
   boolean modified;
   
   protected ModifiedVersionBase() {
      version = 0;
      modified = false;
   }
   
   public void notifyModified() {
      modified = true;
   }
   
   public int getVersion() {
      if (modified) {
         ++version;
         modified = false;
      }
      return version;
   }
   
}
