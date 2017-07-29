package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.GL.GLResource;

/**
 * Resource used by GL3
 */
public interface GL3Resource extends GLResource {

   /**
    * Discard data, regardless of references
    * @param gl active context
    */
   public void dispose(GL3 gl);
   
   /**
    * Dispose data if the resource is in an invalid state
    * @param gl active context
    * @return true if disposed
    */
   public boolean disposeInvalid (GL3 gl);
   
   /**
    * Dispose if there are currently no held references
    * @param gl active context
    * @return true if disposed
    */
   public boolean disposeUnreferenced (GL3 gl);
   
   /**
    * Release reference and dispose if there are no
    * more references
    * @param gl active context
    * @return true if disposed
    */
   boolean releaseDispose (GL3 gl);

   @Override
   public GL3Resource acquire ();
   
}
