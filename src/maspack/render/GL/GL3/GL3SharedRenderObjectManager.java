package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.RenderInstances;
import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.GL.GLDeferredDeleteQueue;
import maspack.render.GL.GLGarbageSource;
import maspack.util.DisposeObserver;

public class GL3SharedRenderObjectManager implements GLGarbageSource {

   /**
    * Global switch for reference-count-based disposal of cached shared render
    * objects. When {@code true} (the default), this manager holds its own
    * reference to each cached object and releases it via
    * {@link GL3ResourceBase#releaseDispose} on eviction, so the object's GL
    * resources are freed only once nothing references it any more (neither this
    * cache nor any per-viewer wrapper). When {@code false}, the original
    * behaviour is restored: the manager holds no reference and force-disposes
    * cached objects the moment their backing {@link RenderObject} dies, even if
    * a wrapper still references them.
    *
    * <p>Captured once per manager at construction (see {@link #refCounted}):
    * toggling at runtime is unsafe because it would mismatch the acquire done at
    * cache-insert time against the release done at eviction time. Set at startup
    * via the system property {@code maspack.render.refCountedSharedDisposal}.
    */
   public static boolean useReferenceCountedDisposal =
      Boolean.parseBoolean (
         System.getProperty ("maspack.render.refCountedSharedDisposal", "true"));

   // per-manager snapshot of the flag, so acquire/release stay paired even if
   // the static flag is toggled while this manager is live
   private final boolean refCounted;

   GL3VertexAttributeMap attributeMap;
   // queue used to defer GL buffer deletes to a render thread (may be null)
   private final GLDeferredDeleteQueue deferredDeletes;
   HashMap<RenderObjectIdentifier,GL3SharedRenderObjectPrimitives> indexedMap;
   HashMap<RenderObjectIdentifier,GL3SharedRenderObjectLines> lineMap;
   HashMap<RenderObjectIdentifier,GL3SharedRenderObjectPoints> pointMap;
   HashMap<DisposeObserver,GL3SharedRenderInstances> instanceMap;

   public GL3SharedRenderObjectManager(GL3VertexAttributeMap attributeMap) {
      this (attributeMap, null);
   }

   public GL3SharedRenderObjectManager(
      GL3VertexAttributeMap attributeMap, GLDeferredDeleteQueue deferredDeletes) {
      this.attributeMap = attributeMap;
      this.deferredDeletes = deferredDeletes;
      this.refCounted = useReferenceCountedDisposal;
      indexedMap = new HashMap<> ();
      lineMap = new HashMap<> ();
      pointMap = new HashMap<> ();
      instanceMap = new HashMap<>();
   }

   // ------------------------------------------------------------------
   // Reference-count helpers. These centralize the acquire/release policy so
   // that the four getXXX/garbage paths below stay parallel.
   // ------------------------------------------------------------------

   /**
    * Records this manager's reference to a freshly cached shared object.
    * In reference-counted mode this is an {@code acquire()}; otherwise a no-op.
    */
   private void managerAcquire (GL3ResourceBase gro) {
      if (refCounted) {
         gro.acquire ();
      }
   }

   /**
    * Drops this manager's hold on a shared object being evicted from a cache.
    * In reference-counted mode this is a {@code releaseDispose()} (the object is
    * disposed only if nothing else references it); otherwise it force-disposes,
    * matching the original behaviour.
    */
   private void managerRelease (GL3ResourceBase gro, GL3 gl) {
      if (refCounted) {
         gro.releaseDispose (gl);
      } else {
         gro.dispose (gl);
      }
   }

   /**
    * Returns true if a cached lookup result must be (re)generated: either it was
    * absent, or it is no longer valid. An invalid entry is evicted here (this
    * manager's reference dropped) before the caller regenerates a replacement.
    */
   private boolean invalidOrNull (GL3ResourceBase gro, GL3 gl) {
      if (gro == null) {
         return true;
      }
      if (!gro.isValid ()) {
         managerRelease (gro, gl);
         return true;
      }
      return false;
   }

   public GL3SharedRenderObjectPrimitives getPrimitives(GL3 gl, RenderObject robj) {

      GL3SharedRenderObjectPrimitives gro = null;
      synchronized (indexedMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = indexedMap.get (rid);
         if (invalidOrNull (gro, gl)) {
            gro = GL3SharedRenderObjectPrimitives.generate(gl, robj, attributeMap.getPosition (),
               attributeMap.getNormal (), attributeMap.getColor (), attributeMap.getTexcoord ());
            gro.setDeferredDeleteQueue (deferredDeletes);
            managerAcquire (gro);
            indexedMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }

      return gro;
   }

   public GL3SharedRenderObjectLines getLines(GL3 gl, RenderObject robj) {

      GL3SharedRenderObjectLines gro = null;
      synchronized (lineMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = lineMap.get (rid);
         if (invalidOrNull (gro, gl)) {
            gro = GL3SharedRenderObjectLines.generate(gl, robj, attributeMap.getPosition (),
               attributeMap.getNormal (), attributeMap.getColor (), attributeMap.getTexcoord (),
               attributeMap.get ("line_bottom_position"),
               attributeMap.get ("line_top_position"),
               attributeMap.get ("line_bottom_color"),
               attributeMap.get ("line_top_color"));
            gro.setDeferredDeleteQueue (deferredDeletes);
            managerAcquire (gro);
            lineMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }

      return gro;
   }

   public GL3SharedRenderObjectPoints getPoints(GL3 gl, RenderObject robj) {

      GL3SharedRenderObjectPoints gro = null;
      synchronized (pointMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = pointMap.get (rid);
         if (invalidOrNull (gro, gl)) {
            gro = GL3SharedRenderObjectPoints.generate(gl, robj, attributeMap.getPosition (),
               attributeMap.getNormal (), attributeMap.getColor (), attributeMap.getTexcoord (),
               attributeMap.get ("instance_position"),
               attributeMap.get ("instance_color"));
            gro.setDeferredDeleteQueue (deferredDeletes);
            managerAcquire (gro);
            pointMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }

      return gro;
   }

   public GL3SharedRenderInstances getInstances(GL3 gl, RenderInstances rinst) {

      GL3SharedRenderInstances gro = null;
      // NB: original code synchronized on pointMap here; instanceMap is correct
      synchronized (instanceMap) {
         DisposeObserver rid = rinst.getDisposeObserver();
         gro = instanceMap.get (rid);
         if (invalidOrNull (gro, gl)) {
            gro = GL3SharedRenderInstances.generate(gl, rinst,
               attributeMap.get("instance_position"),
               attributeMap.get("instance_orientation"),
               attributeMap.get("instance_affine_matrix"),
               attributeMap.get("instance_normal_matrix"),
               attributeMap.get("instance_scale"),
               attributeMap.get("instance_color"));
            managerAcquire (gro);
            instanceMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, rinst);
         }
      }

      return gro;
   }

   @Override
   public void garbage (GL gl) {
      GL3 gl3 = (GL3)gl;

      // dispose dead RenderObjects: drop this manager's reference (which
      // disposes only if no wrapper still references the shared object)
      synchronized(indexedMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3SharedRenderObjectPrimitives>> it = indexedMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3SharedRenderObjectPrimitives> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               managerRelease (entry.getValue (), gl3);
            }
         }
      }

      synchronized(lineMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3SharedRenderObjectLines>> it = lineMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3SharedRenderObjectLines> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               managerRelease (entry.getValue (), gl3);
            }
         }
      }

      synchronized(pointMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3SharedRenderObjectPoints>> it = pointMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3SharedRenderObjectPoints> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               managerRelease (entry.getValue (), gl3);
            }
         }
      }

      synchronized(instanceMap) {
         Iterator<Entry<DisposeObserver,GL3SharedRenderInstances>> it = instanceMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<DisposeObserver,GL3SharedRenderInstances> entry = it.next ();
            if (entry.getKey ().isDisposed()) {
               it.remove ();
               managerRelease (entry.getValue (), gl3);
            }
         }
      }
   }

   public void dispose(GL gl) {
      // Full teardown (e.g. context loss): discard all GL resources regardless
      // of references, per the GLResource.dispose contract. Force-dispose in
      // both modes; the (now idempotent) shared-object dispose makes a later
      // stray wrapper release harmless.

      GL3 gl3 = (GL3)gl;

      synchronized(indexedMap) {
         for (GL3SharedRenderObjectPrimitives gro : indexedMap.values ()) {
            gro.dispose (gl3);
         }
         indexedMap.clear ();
      }

      synchronized(lineMap) {
         for (GL3SharedRenderObjectLines gro : lineMap.values ()) {
            gro.dispose (gl3);
         }
         lineMap.clear ();
      }

      synchronized(pointMap) {
         for (GL3SharedRenderObjectPoints gro : pointMap.values ()) {
            gro.dispose (gl3);
         }
         pointMap.clear ();
      }

      synchronized(instanceMap) {
         for (GL3SharedRenderInstances gro : instanceMap.values ()) {
            gro.dispose (gl3);
         }
         instanceMap.clear ();
      }
   }

   public GL3VertexAttributeInfo getAttribute(String str) {
      return attributeMap.get (str);
   }


}
