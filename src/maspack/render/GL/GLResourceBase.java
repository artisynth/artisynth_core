package maspack.render.GL;

import javax.media.opengl.GL;

import maspack.util.ReferenceCountedBase;

/**
 * Resource used by GL
 * @param <T>
 */
public abstract class GLResourceBase extends ReferenceCountedBase implements GLResource {
   
   volatile boolean disposed;
   
   public GLResourceBase() {
      super();
      disposed = false;
   }
      
   /**
    * Must dispose of the resource.  Assumption is that after this is called,
    * all GL resources have been appropriately freed.
    * @param gl
    */
   protected abstract void internalDispose(GL gl);
   
   @Override
   public final void dispose (GL gl) {
      internalDispose (gl);
      disposed = true;
   }
   
   @Override
   public boolean releaseDispose (GL gl) {
      long r = releaseAndCount();
      if (r == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   public final boolean isDisposed() {
      return disposed;
   }
      
   public boolean isValid() {
      return !disposed;
   }
   
   @Override
   public boolean disposeInvalid (GL gl) {
      if (!isValid ()) {
         dispose(gl);
         return false;
      }
      return true;
   }
   
   @Override
   public boolean disposeUnreferenced (GL gl) {
      if (getReferenceCount () == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   @Override
   public GLResourceBase acquire () {
      return (GLResourceBase)super.acquire ();
   }
}
