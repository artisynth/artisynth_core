package maspack.render.GL.GL2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;

import maspack.render.GL.GLResources;
import maspack.render.GL.GL2.DisplayListManager.DisplayListPassport;

public class GL2Resources extends GLResources {

   // Shared VBOs/VAOs
   DisplayListManager dlManager;
   GL2PrimitiveManager primManager;
   
   public GL2Resources(GLCapabilities cap) {
      super(cap);
      
      dlManager = new DisplayListManager();
      primManager = new GL2PrimitiveManager();
   }

   @Override
   public void init(GL gl) {
   }

   @Override
   public synchronized void dispose(GL gl) {
      // clear shared info
      GL2 gl2 = gl.getGL2 ();
      dlManager.dispose(gl2);
      primManager.dispose(gl2);
   }

   /**
    * Release cache not referenced since previous call
    */
   public synchronized void releaseUnused(GL2 gl) {
      dlManager.releaseUnused(gl);
      primManager.releaseUnused(gl);
   }

   /**
    * Clear all cached objects (hard reset of resources)
    */
   public synchronized void clearCached(GL2 gl) {
      dlManager.clear(gl);
      primManager.clear(gl);
   }
   
   //   public GL2PrimitiveManager getPrimitiveManager() {
   //      return primManager;
   //   }
   //   
   //   public DisplayListManager getDisplayListManager() {
   //      return dlManager;
   //   }
   
   public int getDisplayList(GL2 gl, Object key) {
      DisplayListPassport list = dlManager.getDisplayList(gl, key);
      return list.getList ();
   }
   
   public int allocateDisplayList(GL2 gl, Object key) {
      DisplayListPassport list = dlManager.allocateDisplayList(gl, key);
      return list.getList();
   }
   
   public void freeDisplayList(GL2 gl, Object key) {
      dlManager.freeDisplayList(gl, key);
   }
   
   public int getSphereDisplayList(GL2 gl, int slices) {
      int list = primManager.getSphereDisplayList(gl, slices, slices/2);
      return list;
   }

   public int getCylinderDisplayList(GL2 gl, int slices, boolean capped) {
      int list = primManager.getCylinderDisplayList(gl, slices, capped);
      return list;
   }

   public int getSpindleDisplayList(GL2 gl, int slices) {
      int list = primManager.getSpindleDisplayList(gl, slices, slices/2);
      return list;
   }

   public DisplayListPassport getDisplayListPassport(GL2 gl, Object key) {
      DisplayListPassport list = dlManager.getDisplayList(gl, key);
      return list;
   }

   public DisplayListPassport allocateDisplayListPassport(GL2 gl, Object key, Object fingerPrint) {
      DisplayListPassport list = dlManager.allocateDisplayList(gl, key, fingerPrint);
      return list;
   }
      
}
