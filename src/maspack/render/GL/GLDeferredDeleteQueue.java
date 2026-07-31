package maspack.render.GL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import com.jogamp.opengl.GL;

/**
 * Thread-safe queue of {@link GLResource}s that a background sweep has found to
 * be unreferenced and that are awaiting deletion.
 *
 * <p>The point of this class is to move the actual GL delete calls
 * (glDeleteBuffers, glDeleteTextures, ...) <i>off</i> the background garbage
 * collector thread and onto a render thread that owns a live GL context. A
 * sweep running on the collector thread only needs to do cheap, context-free
 * reference-count bookkeeping and {@link #enqueue} the dead resources here;
 * each viewer then calls {@link #drain} during its own frame to perform the GL
 * deletes on its own context. This avoids issuing GL operations against a
 * context shared with the renderer, which on Windows forces driver-level
 * synchronization and produces rendering stalls.
 *
 * <p>A {@link LinkedHashSet} backs the queue so that repeated enqueues of the
 * same resource (which happen when several sweeps run before a drain) collapse
 * to a single entry while preserving insertion order.
 */
public class GLDeferredDeleteQueue {

   /**
    * Global switch for deferred deletion of GL resources. When {@code true}
    * (the default), background garbage sweeps only enqueue unreferenced
    * resources and the actual GL deletes happen on a render thread (see class
    * comment). When {@code false}, the original behaviour is restored and
    * sweeps delete resources inline on whatever thread runs them.
    *
    * <p>Consulted dynamically at each sweep/drain, so it may be toggled at
    * runtime. May also be set at startup via the system property
    * {@code maspack.render.deferredDelete=false}.
    */
   public static boolean enabled =
      Boolean.parseBoolean (
         System.getProperty ("maspack.render.deferredDelete", "true"));

   private final LinkedHashSet<GLResource> myPending = new LinkedHashSet<>();

   /**
    * Marks a resource for deferred deletion. Does no GL work, so this is safe
    * to call from the garbage collector thread. Resources that are already
    * disposed are ignored.
    */
   public synchronized void enqueue (GLResource resource) {
      if (resource != null && !resource.isDisposed ()) {
         myPending.add (resource);
      }
   }

   /**
    * Number of resources currently awaiting deletion.
    */
   public synchronized int size() {
      return myPending.size ();
   }

   /**
    * Disposes all pending resources that are still unreferenced, using the
    * supplied context. Must be called on a thread that has {@code gl} current
    * (i.e. from within a viewer's render callback).
    *
    * <p>Each resource's reference count is re-checked immediately before
    * disposal: a resource that has been re-acquired since it was enqueued is
    * simply dropped from the queue (its owning list is still tracking it), so
    * a resurrected resource is never deleted out from under a live user. The
    * GL deletes themselves are performed outside the lock so that the
    * collector thread can keep enqueueing while a drain is in progress.
    *
    * @return the number of resources actually disposed
    */
   public int drain (GL gl) {
      ArrayList<GLResource> toDelete;
      synchronized (this) {
         toDelete = new ArrayList<> (myPending.size ());
         Iterator<GLResource> it = myPending.iterator ();
         while (it.hasNext ()) {
            GLResource res = it.next ();
            if (res.isDisposed ()) {
               it.remove ();                 // already gone
            }
            else if (res.getReferenceCount () == 0) {
               toDelete.add (res);           // dead: delete below, outside lock
               it.remove ();
            }
            else {
               it.remove ();                 // resurrected: owner still tracks it
            }
         }
      }
      int disposed = 0;
      for (GLResource res : toDelete) {
         // re-check: the resource may have been re-acquired between the
         // partition above and now
         if (!res.isDisposed () && res.getReferenceCount () == 0) {
            res.dispose (gl);
            disposed++;
         }
      }
      return disposed;
   }

   /**
    * Discards all pending entries without disposing them. Intended for
    * teardown paths where a blanket dispose has already run.
    */
   public synchronized void clear() {
      myPending.clear ();
   }
}
