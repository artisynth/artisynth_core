package maspack.render.GL;

import java.util.Iterator;
import java.util.LinkedList;

import com.jogamp.opengl.GL;

public class GLResourceList<T extends GLResource> implements GLGarbageSource {

   public LinkedList<T> resources;

   // if non-null, unreferenced resources are handed to this queue for deletion
   // on a render thread instead of being deleted inline during garbage()
   private GLDeferredDeleteQueue deferredDeletes;

   public GLResourceList() {
      resources = new LinkedList<>();
   }

   /**
    * Installs a deferred-delete queue. Once set, {@link #garbage(GL)} no longer
    * issues GL delete calls itself; instead it enqueues unreferenced resources
    * for later disposal on a render thread (see {@link GLDeferredDeleteQueue}).
    * Passing {@code null} restores the original inline-delete behaviour.
    */
   public void setDeferredDeleteQueue (GLDeferredDeleteQueue queue) {
      deferredDeletes = queue;
   }

   public void track(T resource) {
      synchronized (resources) {
         resources.add (resource);
      }
   }

   @Override
   /**
    * Removes and frees any unused GL resources tracked by this list.
    * "Unused" refers to resources that have no other references, and have
    * not been used since last collection.
    *
    * <p>If a deferred-delete queue has been installed via
    * {@link #setDeferredDeleteQueue}, this sweep performs no GL work: it prunes
    * already-disposed entries and enqueues unreferenced ones for deletion on a
    * render thread. Unreferenced entries are left in the tracking list until
    * they are actually disposed, at which point a later sweep removes them.
    * This lets the method run cheaply on the background collector thread
    * without touching a GL context shared with the renderer.
    */
   public void garbage(GL gl) {
      synchronized(resources) {
         Iterator<T> it = resources.iterator ();
         while (it.hasNext ()) {
            T resource = it.next ();
            if (resource.isDisposed ()) {
               it.remove ();
            } else if (deferredDeletes != null && GLDeferredDeleteQueue.enabled) {
               // context-free: only bookkeeping, actual GL delete is deferred
               if (resource.getReferenceCount () == 0) {
                  deferredDeletes.enqueue (resource);
               }
            } else if (resource.disposeUnreferenced (gl)) {
               it.remove ();
            }
         }
      }
   }
   
   @Override
   public void dispose (GL gl) {
      synchronized(resources) {
         for (GLResource res : resources) {
            res.dispose (gl);
         }
         resources.clear ();
      }
   }

}
