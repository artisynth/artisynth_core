package maspack.render.GL;

import com.jogamp.opengl.GL;

import maspack.util.ReferenceCountedBase;

/**
 * Resource used by GL
 */
public abstract class GLResourceBase extends ReferenceCountedBase
   implements GLResource {
   
   public GLResourceBase() {
      super();
   }
   
   @Override
   public abstract void dispose (GL gl);
   
   @Override
   public boolean releaseDispose (GL gl) {
      long r = releaseAndCount();
      if (r == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   public abstract boolean isDisposed();
      
   public boolean isValid() {
      return (!isDisposed ());
   }
   
   @Override
   public boolean disposeInvalid (GL gl) {
      if (!isValid ()) {
         dispose(gl);
         return true;
      }
      return false;
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
