package maspack.render.GL.GL3;

import java.util.HashMap;

import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;

import maspack.render.RenderObject;

public class GL3Resources {

   private GLContext context;

   // Shared VBOs/VAOs
   private GL3PrimitiveManager primManager = null;
   private GL3ResourceManager glresManager = null;
   private HashMap<Object,RenderObject> roMap = null;
   
   int version; // used for managing usage between multiple viewers

   public GL3Resources(GLContext context) {
      this.context = context;
      primManager = new GL3PrimitiveManager();
      glresManager = new GL3ResourceManager();
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

   public void init(GL3 gl) {
   }

   public void dispose(GL3 gl) {
      // clear shared info
      primManager.dispose(gl);
      glresManager.dispose(gl);
      version++;      
   }

   /**
    * Release cache not referenced since previous call
    */
   public void releaseUnused(GL3 gl) {
      primManager.releaseUnused(gl);
      primManager.resetUseCounts();
      glresManager.releaseUnused(gl);
      glresManager.resetUseCounts();
      version++;
   }

   /**
    * Clear all cached objects (hard reset of resources)
    */
   public void clearCached(GL3 gl) {
      primManager.clear(gl);
      glresManager.clear(gl);
      version++;
   }

   public void addResource(GL3 gl, Object key, GL3Resource glres) {
      glresManager.add(gl, key, glres);
   }
   
   public void removeResource(GL3 gl, Object key) {
      glresManager.release(gl, key);
   }
   
   public GL3Resource getResource(Object key) {
      return glresManager.get(key);
   }
   
   
   public void addRenderObject(Object key, RenderObject ro) {
      synchronized (roMap) {
         roMap.put(key, ro);  
      }
   }
   
   public synchronized RenderObject getRenderObject(Object key) {
      RenderObject robj;
      synchronized (roMap) {
         robj = roMap.get(key);
      }
      return robj;
   }
   
   public synchronized RenderObject removeRenderObject(Object key) {
      RenderObject robj;
      synchronized (roMap) {
         robj = roMap.remove(key);
      }
      return robj;
   }
   
   public GL3Object getSphere(GL3 gl, int slices, int levels) {
      return primManager.getSphere(gl, slices, levels);
   }

   public GL3Object getTaperedEllipsoid(GL3 gl, int slices, int levels) {
      return primManager.getTaperedEllipsoid(gl, slices, levels);
   }
   
   public GL3Object getCylinder(GL3 gl, int slices, boolean capped) {
      return primManager.getCylinder(gl, slices, capped);
   }
   
   public GL3Object getCone(GL3 gl, int slices, boolean capped) {
      return primManager.getCone(gl, slices, capped);
   }

   public GL3Object getAxes(GL3 gl, boolean drawx, boolean drawy, boolean drawz) {
      return primManager.getAxes(gl, drawx, drawy, drawz);
   }

}
