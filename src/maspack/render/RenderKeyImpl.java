package maspack.render;

/**
 * Basic implementation of {@link RenderKey}
 * @author Antonio
 *
 */
public class RenderKeyImpl implements RenderKey {

   private volatile boolean valid;
   
   public RenderKeyImpl () {
      valid = true;
   }
   
   @Override
   public boolean isValid () {
      return valid;
   }
   
   /**
    * To be called when the associated renderable is no longer valid.
    * Signals that any stored resources can be free.  It may be a
    * good idea to call this in the renderable's dispose() or finalize()
    * method.
    */
   public void invalidate() {
      valid = false;
   }
   

}
