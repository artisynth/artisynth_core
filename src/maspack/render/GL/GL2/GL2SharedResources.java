package maspack.render.GL.GL2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;

import maspack.render.RenderKey;
import maspack.render.GL.GLGarbageSource;
import maspack.render.GL.GLSharedResources;

public class GL2SharedResources extends GLSharedResources implements GLGarbageSource {

   // Shared primitives
   GL2PrimitiveManager primManager;
   HashMap<RenderKey,GL2VersionedObject> sharedObjectMap;
   
   public GL2SharedResources(GLCapabilities cap) {
      super(cap);
      
      primManager = new GL2PrimitiveManager();
      sharedObjectMap = new HashMap<>();
      addGarbageSource (primManager);
      addGarbageSource (this);  // track shared render objects
   }

   @Override
   public synchronized void dispose(GL gl) {
      // clear shared info
      GL2 gl2 = gl.getGL2 ();
      primManager.disposeResources(gl2);
   }
   
   public GL2Primitive getSphere(GL2 gl, int resolution) {
      return primManager.getSphere (gl, resolution);
   }

   public GL2Primitive getCone(GL2 gl, int slices, boolean capped) {
      return primManager.getCone (gl, slices, capped);
   }
   
   public GL2Primitive getCylinder(GL2 gl, int slices, boolean capped) {
      return primManager.getCylinder (gl, slices, capped);
   }

   public GL2Primitive getSpindle(GL2 gl, int slices) {
      return primManager.getSpindle (gl, slices);
   }
   
   /**
    * Creates a new display list whose resources are tracked for garbage collection
    * @param gl
    * @param count number of lists to generate
    * @return
    */
   public GL2DisplayList allocateDisplayList(GL2 gl, int count) {
      GL2DisplayList list = GL2DisplayList.allocate (gl, count);
      track (list);
      return list;
   }
   
   //   public GL2VersionedObject allocateVersionedObject(GL2 gl, Object key, Object fingerPrint) {
   //      GL2DisplayList list = allocateDisplayList (gl, 1);
   //      GL2VersionedObject object = new GL2VersionedObject (list, fingerPrint);
   //      synchronized(sharedObjectMap) {
   //         sharedObjectMap.put (key, object);
   //      }
   //      return object;
   //   }
   
   public GL2VersionedObject allocateVersionedObject(GL2 gl, RenderKey key, Object fingerPrint) {
      GL2DisplayList list = allocateDisplayList (gl, 1);
      GL2VersionedObject object = new GL2VersionedObject (list, fingerPrint);
      synchronized(sharedObjectMap) {
         sharedObjectMap.put (key, object);
      }
      return object;
   }
   
   public GL2VersionedObject getVersionedObject(RenderKey key) {
      GL2VersionedObject object = null;
      synchronized(sharedObjectMap) {
         object = sharedObjectMap.get(key);
      }
      if (object == null) {
         System.out.println ("Unknown object");
      }
      return object;
   }

   @Override
   public void garbage (GL gl) {
      synchronized(sharedObjectMap) {
         // check if any render objects are valid
         Iterator<Entry<RenderKey,GL2VersionedObject>> it = sharedObjectMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderKey,GL2VersionedObject> entry = it.next ();
            RenderKey key = entry.getKey ();
            if (!key.isValid ()) {
               GL2VersionedObject obj = entry.getValue ();
               obj.dispose (); // release hold on resources
               it.remove ();   // remove entry
            }
         }
      }
      
   }
      
}
