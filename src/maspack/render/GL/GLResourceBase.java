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

   /**
    * Like {@link #releaseDispose(GL)}, but when the reference count reaches zero
    * the actual GL delete is deferred: instead of disposing inline (on whatever
    * thread happens to make the final release, possibly the background garbage
    * collector), the resource is enqueued on {@code queue} to be disposed later
    * on a render thread. Falls back to the inline {@link #releaseDispose(GL)}
    * when {@code queue} is null or deferred deletion is disabled.
    *
    * @return true if this call released the final reference (whether disposed
    * inline or enqueued for deferred disposal)
    */
   public boolean releaseDisposeDeferred (GL gl, GLDeferredDeleteQueue queue) {
      if (queue == null || !GLDeferredDeleteQueue.enabled) {
         return releaseDispose (gl);
      }
      long r = releaseAndCount();
      if (r == 0) {
         queue.enqueue (this);
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
