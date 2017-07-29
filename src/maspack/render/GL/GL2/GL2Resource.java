package maspack.render.GL.GL2;

import com.jogamp.opengl.GL2;

import maspack.render.GL.GLResource;

/**
 * Resource used by GL2
 */
public interface GL2Resource extends GLResource {

   /**
    * Discard data, regardless of references
    */
   public void dispose(GL2 gl);
   
   public boolean disposeInvalid(GL2 gl);
   
   boolean disposeUnreferenced (GL2 gl);
   
   boolean releaseDispose (GL2 gl);

   @Override
   public GL2Resource acquire ();
   
}
