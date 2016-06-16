package maspack.render;

/**
 * Used as a key for storing objects for rendering.  If the key returns
 * invalid, this will signal the renderer that it can free any associated
 * resources.
 * @author Antonio
 *
 */
public interface RenderKey {
   
   /**
    * Whether the renderable this key is associated with is still valid.
    * This should run in a thread-safe way (e.g. with a volatile boolean)
    * @return <code>true</code> if key is still valid
    */
   public boolean isValid();
   
}
