package maspack.render.GL;

import java.util.HashMap;
import java.util.HashSet;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;

import maspack.render.RenderObject;

/**
 * Container class for resources tied to a particular context.  There should
 * one set of resources per context.
 * @author antonio
 *
 */
public abstract class GLResources implements GLEventListener {
   
   GLCapabilities glCapabilities;
   GLAutoDrawable masterDrawable;
   
   HashMap<Object,RenderObject> roMap;
   HashSet<GLViewer> viewers;
   
   public GLResources(GLCapabilities cap) {
      this.glCapabilities = cap;
      this.roMap = new HashMap<>();
      viewers = new HashSet<>();
      masterDrawable = null;
   }
   
   /**
    * Returns the capabilities of the underlying GL context
    * @return the GL capabilities
    */
   public GLCapabilities getGLCapabilities() {
      return glCapabilities;
   }
   
   protected GLAutoDrawable getMasterDrawable() {
      return masterDrawable;
   }
   
   private void maybeCreateMaster() {
      if (masterDrawable == null) {
         // create the master drawable
         final GLProfile glp = glCapabilities.getGLProfile();
         masterDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, glCapabilities, null);
         masterDrawable.addGLEventListener (this);
         masterDrawable.display(); // triggers GLContext object creation and native realization.
      }
   }
   
   /**
    * Creates a canvas with the same capabilities and shared context
    * as other viewers using this set of resources.  This ensures
    * that the sharing of resources is properly initialized.
    * @return the created canvas
    */
   public synchronized GLCanvas createCanvas() {
      
      maybeCreateMaster();
      
      GLCanvas canvas = new GLCanvas (glCapabilities);
      canvas.setSharedAutoDrawable (masterDrawable);
      canvas.setDefaultCloseOperation (WindowClosingMode.DISPOSE_ON_CLOSE);
      return canvas;
   }
   
   /**
    * Creates a canvas with the same capabilities and shared context
    * as other viewers using this set of resources.  This ensures
    * that the sharing of resources is properly initialized.
    * MUST BE CALLED IN THE GLViewer's CONSTRUCTOR!!
    * @return the created canvas
    */
   public synchronized GLJPanel createPanel() {
      maybeCreateMaster();
      GLJPanel panel = new GLJPanel (glCapabilities);
      panel.setSharedAutoDrawable (masterDrawable);
      panel.setDefaultCloseOperation (WindowClosingMode.DISPOSE_ON_CLOSE);
      return panel;
   }
   
   /**
    * Register a particular viewer with this set of resources.
    * MUST BE CALLED IN THE GLViewer's CONSTRUCTOR!!
    * @param viewer the viewer with which to share resources.
    */
   public synchronized void registerViewer(GLViewer viewer) {
      viewers.add (viewer);
   }
   
   /**
    * Unregisters a particular viewer with this set of resources.
    * MUST BE CALLED IN THE GLViewer's dispose() METHOD.
    * @param viewer
    */
   public synchronized void deregisterViewer(GLViewer viewer) {
      viewers.remove (viewer);
      if (viewers.size () == 0) {
         if (masterDrawable != null) {
            masterDrawable.destroy ();
            masterDrawable = null;
         }
      }
   }
   
   public void init(GL gl) {
   }
   
   /**
    * Clears all resources with the associated with the master
    * @param gl
    */
   public abstract void dispose(GL gl);
   
   /**
    * Add a render object resource to this collection of resources, so it can be
    * shared between viewers.
    * @param key with which to refer to the object
    * @param ro the shared object
    */
   public synchronized void addRenderObject(Object key, RenderObject ro) {
      roMap.put(key, ro);
   }
   
   /**
    * Gets a render object resource to this collection of resources, so it
    * can be shared between viewers.
    * @param key used to reference a particular renderable object
    * @return the shared renderable if it exists, null otherwise
    */
   public synchronized RenderObject getRenderObject(Object key) {
      return roMap.get(key);
   }
   
   /**
    * Remove a render object resource.  It will no longer be shared
    * between viewers
    * @param key used to reference the desired renderable object
    * @return the removed renderable, if it exists
    */
   public synchronized RenderObject removeRenderObject(Object key) {
      return roMap.remove(key);
   }

   @Override
   public void init (GLAutoDrawable drawable) {
      System.out.println("Master drawable initialized");
   }

   @Override
   public void dispose (GLAutoDrawable drawable) {
      System.out.println("Master drawable disposed");
   }

   @Override
   public void display (GLAutoDrawable drawable) {
      System.out.println("Master drawable displayed");
   }

   @Override
   public void reshape (
      GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.println("Master drawable reshaped");
   }
   
}
