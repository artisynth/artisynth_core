package maspack.render.GL.GL3;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;

import maspack.render.GL.GLResources;

public class GL3Resources extends GLResources {

   // Shared VBOs/VAOs
   private GL3PrimitiveManager primManager = null;
   private GL3ResourceManager glresManager = null;

   public GL3Resources(GLCapabilities cap) {
      super(cap);

      primManager = new GL3PrimitiveManager();
      glresManager = new GL3ResourceManager();
   }

   @Override
   public void init(GL gl) {
   }

   @Override
   public void dispose(GL gl) {

      GL3 gl3 = gl.getGL3 ();

      // clear shared info
      primManager.dispose(gl3);
      glresManager.dispose(gl3);
   }

   /**
    * Release cache not referenced since previous call
    */
   public void releaseUnused(GL3 gl) {
      primManager.releaseUnused(gl);
      primManager.resetUseCounts();
      glresManager.releaseUnused(gl);
      glresManager.resetUseCounts();
   }

   /**
    * Clear all cached objects (hard reset of resources)
    */
   public void clearCached(GL3 gl) {
      primManager.clear(gl);
      glresManager.clear(gl);
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

   public GL3Object getSphere(GL3 gl, int slices, int levels) {
      return primManager.getSphere(gl, slices, levels);
   }

   public GL3Object getSpindle(GL3 gl, int slices, int levels) {
      return primManager.getSpindle(gl, slices, levels);
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
