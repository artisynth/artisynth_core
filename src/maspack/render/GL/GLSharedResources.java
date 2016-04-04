package maspack.render.GL;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;

import maspack.render.TextureContent;
import maspack.render.TextureContentFile;

/**
 * Container class for resources tied to a particular context.  There should
 * one set of resources per context.
 * @author antonio
 *
 */
public abstract class GLSharedResources implements GLEventListener, GLGarbageSource {
   
   private static long DEFAULT_GARBAGE_INTERVAL = 20000; // 20 seconds
   
   GLCapabilities glCapabilities;
   GLAutoDrawable masterDrawable;
   HashSet<GLViewer> viewers;
   
   GLResourceList<GLResource> resources;
   HashMap<TextureContent, GLTexture> textureMap;
   
   GLTextureLoader textureLoader;
   
   public long masterRedrawInterval;
   MasterRedrawThread masterRedrawThread;
   GLGarbageCollector garbageman;
   GLGarbageBin<GLResource> garbagebin;
   
   private static class MasterRedrawThread extends Thread {
      
      GLAutoDrawable master;
      private long redrawInterval;
      private volatile boolean terminate;
      
      public MasterRedrawThread(GLAutoDrawable master, long redrawInterval) {
         this.master = master;
         this.redrawInterval = redrawInterval;
         terminate = false;
      }
      
      public void setRedrawInterval(long ms) {
         redrawInterval = ms;
      }
      
      @Override
      public void run () {
         while (!terminate) {
            master.display ();
            try {
               Thread.sleep (redrawInterval);
            } catch (InterruptedException e) {
            }
            
         }
      }
      
      public void terminate() {
         terminate = true;
      }
      
   }
   
   public GLSharedResources(GLCapabilities cap) {
      this.glCapabilities = cap;
      viewers = new HashSet<>();
      masterDrawable = null;
      masterRedrawThread = null;
      masterRedrawInterval = DEFAULT_GARBAGE_INTERVAL;
      textureLoader = new GLTextureLoader();
      
      textureMap = new HashMap<> ();
      
      garbageman = new GLGarbageCollector ();
      garbagebin = new GLGarbageBin<> ();
      resources = new GLResourceList<> ();
      garbageman.addSource (garbagebin);
      garbageman.addSource (resources);
      garbageman.addSource (textureLoader);
      garbageman.addSource (this);  // for textures
   }
   
   protected void addGarbageSource(GLGarbageSource source) {
      garbageman.addSource (source);
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
   
   @Override
   public void garbage (GL gl) {
      
      // check for any expired textures
      synchronized(textureMap) {
         Iterator<Entry<TextureContent,GLTexture>> it = textureMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<TextureContent,GLTexture> entry = it.next ();
            TextureContent key = entry.getKey ();
            if (key.getReferenceCount () == 0) {
               GLTexture tex = entry.getValue ();
               tex.release ();
               it.remove ();
            }
         }
      }
   }
   
   private void maybeCreateMaster() {
      if (masterDrawable == null) {
         // create the master drawable
         final GLProfile glp = glCapabilities.getGLProfile();
         masterDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, glCapabilities, null);
         
         masterDrawable.addGLEventListener (this);
         masterDrawable.addGLEventListener (garbageman);
         
         masterDrawable.display(); // triggers GLContext object creation and native realization.
         
         masterRedrawThread = new MasterRedrawThread (masterDrawable, masterRedrawInterval);
         masterRedrawThread.setName ("GL Garbage Collector - " + masterDrawable.getHandle ());
         masterRedrawThread.start ();
         
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
            masterRedrawThread.terminate ();
            masterRedrawThread = null;
            masterDrawable.destroy ();
            masterDrawable = null;
         }
      }
   }
   
   /**
    * Clears all resources with the associated with the master
    * @param gl
    */
   public void dispose(GL gl) {
      textureMap.clear (); // other sources should be cleared by the garbage man separately
   }
   
   /**
    * Potentially release invalid texture associated with this key
    * @param key for grabbing texture
    * @param tex stored texture object
    * @return true if texture is invalid and released
    */
   private boolean releaseTexture(TextureContent key, GLTexture tex) {
      if (!tex.isValid ()) {
         // release
         tex.release ();
         textureMap.remove (key);
         return true;
      }
      
      return false;
   }
   
   public GLTexture getTexture(TextureContent key) {
      
      GLTexture tex = null;
      // check if texture exists in map
      synchronized(textureMap) {
         tex = textureMap.get (key);
         if (tex == null || releaseTexture(key, tex)) {
            return null; 
         }
      }
      return tex;
   }
   
   public GLTexture getOrLoadTexture(GL gl, TextureContent content) {
      
      GLTexture tex = null;
      synchronized(textureMap) {
         tex = getTexture(content);
         if (tex != null) {
            return tex;
         }
         
         // load texture
         synchronized (textureLoader) {
            try {
               String fileName = ((TextureContentFile)content).getFileName ();
               tex = textureLoader.getTexture (gl, fileName, fileName).acquire ();
               textureMap.put (content, tex);
            } catch (IOException ioe) {
               System.err.println ("Failed to load texture");
               ioe.printStackTrace ();
            }
         }
         
      }
      return tex;
   }
   
   //   public GLTexture loadTexture(GL gl, String name, String filename) throws IOException {
   //      synchronized (textureLoader) {
   //         GLTexture tex = textureLoader.getTexture (gl, name, filename);
   //         return tex;  
   //      }
   //   }
   //
   //   public GLTexture loadTexture(GL gl, String name, 
   //      byte[] buffer, 
   //      int width, int height, 
   //      int srcPixelFormat, int dstPixelFormat) {
   //      synchronized (textureLoader) {
   //         GLTexture tex = textureLoader.getTexture (gl, name, GL.GL_TEXTURE_2D, buffer, width, height, srcPixelFormat, dstPixelFormat);
   //         return tex;
   //      }
   //   }

   @Override
   public void init (GLAutoDrawable drawable) {
      System.out.println("Master drawable initialized");
   }

   @Override
   public void dispose (GLAutoDrawable drawable) {
      System.out.println("Master drawable disposed");
      textureLoader.clearAllTextures (); // clean up
   }
   
   public void track(GLResource res) {
      synchronized (resources) {
         resources.track (res);
      }
   }
   
   public void disposeResource(GLResource res) {
      garbagebin.dispose (res);
   }
   
   
   public void runGarbageCollection(GL gl) {
      garbageman.collect(gl);
   }
   
   public void setGarbageCollectionInterval(long ms) {
      masterRedrawInterval = ms;
      if (masterRedrawThread != null) {
         masterRedrawThread.setRedrawInterval (ms);
      }
   }

   @Override
   public void display (GLAutoDrawable drawable) {
      //System.out.println("Master drawable displayed");
   }

   @Override
   public void reshape (
      GLAutoDrawable drawable, int x, int y, int width, int height) {
      //System.out.println("Master drawable reshaped");
   }
   
}
