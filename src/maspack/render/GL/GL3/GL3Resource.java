package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

import maspack.render.GL.GLResource;

/**
 * Resource used by GL3
 */
public interface GL3Resource extends GLResource {

   /**
    * Discard data, regardless of references
    */
   public void dispose(GL3 gl);
   
   public boolean disposeInvalid (GL3 gl);
   
   boolean disposeUnreferenced (GL3 gl);
   
   boolean releaseDispose (GL3 gl);

   @Override
   public GL3Resource acquire ();
   
}
