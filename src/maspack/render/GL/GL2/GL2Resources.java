package maspack.render.GL.GL2;

import java.util.HashMap;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import maspack.render.RenderObject;

public class GL2Resources {

   GLContext context;

   // Shared VBOs/VAOs
   DisplayListManager dlManager;
   GL2PrimitiveManager primManager;
   HashMap<Object,RenderObject> roMap = null;
   
   int version; // used for managing usage between multiple viewers
   
   public GL2Resources(GLContext context) {
      this.context = context;
      dlManager = new DisplayListManager();
      primManager = new GL2PrimitiveManager();
      roMap = new HashMap<>();
      version = 1;
   }
   
   public int incrementVersion() {
      version++;
      return version;
   }
   
   /**
    * Used for detecting an appropriate viewer (e.g. first to call display)
    * If a viewer has previously seen a particular version, then increments
    * the version, other viewers will know they are not the first.
    */
   public int getVersion() {
      return version;
   }

   public GLContext getContext() {
      return context;
   }

   public void init(GL2 gl) {
   }

   public synchronized void dispose(GL2 gl) {
      // clear shared info
      dlManager.dispose(gl);
      primManager.dispose(gl);
      version++;
   }

   /**
    * Release cache not referenced since previous call
    */
   public synchronized void releaseUnused(GL2 gl) {
      dlManager.releaseUnused(gl);
      primManager.releaseUnused(gl);
      version++;
   }

   /**
    * Clear all cached objects (hard reset of resources)
    */
   public synchronized void clearCached(GL2 gl) {
      dlManager.clear(gl);
      primManager.clear(gl);
      version++;
   }
   
   public GL2PrimitiveManager getPrimitiveManager() {
      return primManager;
   }
   
   public DisplayListManager getDisplayListManager() {
      return dlManager;
   }
   
   public synchronized void addRenderObject(Object key, RenderObject ro) {
      roMap.put(key, ro);
   }
   
   public synchronized RenderObject getRenderObject(Object key) {
      return roMap.get(key);
   }
   
   public synchronized RenderObject removeRenderObject(Object key) {
      return roMap.remove(key);
   }
   
}
