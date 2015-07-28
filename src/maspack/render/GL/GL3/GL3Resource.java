package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

/**
 * Resource used by GL3
 */
public interface GL3Resource {

   /**
    * Prepare resource for use
    */
   public void init(GL3 gl);
   
   /**
    * Signal the resource that something is holding a reference to it
    */
   public void acquire();
   
   /**
    * Signal the resource that a previous hold is complete, disposing the resource if there
    * are no more holds on it.
    */
   public void release(GL3 gl);
   
   /**
    * Discard data, regardless of references
    */
   public void dispose(GL3 gl);
   
   /**
    * Returns true if resource is initialized and has not yet been disposed
    */
   public boolean isValid();
}
