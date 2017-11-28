package maspack.render.GL;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import maspack.render.TextureContent;
import maspack.render.TextureContent.ContentFormat;
import maspack.render.GL.jogl.GLJPanel;
import maspack.util.BufferUtilities;
import maspack.util.Logger;
import maspack.util.Rectangle;

/**
 * Container class for resources tied to a particular context.  There should
 * one set of resources per context.
 * @author antonio
 *
 */
public abstract class GLSharedResources implements GLEventListener, GLGarbageSource {
   
   private static long DEFAULT_GARBAGE_INTERVAL = 20000; // 20 seconds
   private static boolean DEFAULT_GARBAGE_TIMER_ENABLED = true;
   
   GLCapabilities glCapabilities;
   GLAutoDrawable masterDrawable;
   HashSet<Object> viewers;
   
   GLResourceList<GLResource> resources;
   HashMap<TextureContent, GLTexture> textureMap;
   
   GLTextureLoader textureLoader;
   long garbageCollectionInterval;
   boolean garbageTimerEnabled;
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
            try {
               master.display ();
            } catch(Exception e) {
            	e.printStackTrace();
            }
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
      garbageCollectionInterval = DEFAULT_GARBAGE_INTERVAL;
      garbageTimerEnabled = DEFAULT_GARBAGE_TIMER_ENABLED;
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
               tex.releaseDispose (gl);
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
         
         
         if (garbageTimerEnabled) {
            masterRedrawThread = new MasterRedrawThread (masterDrawable, garbageCollectionInterval);
            masterRedrawThread.setName ("GL Garbage Collector - " + masterDrawable.getHandle ());
            masterRedrawThread.start ();
         }
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
      return panel;
   }
   
   /**
    * Register a particular viewer with this set of resources.
    * MUST BE CALLED IN THE GLViewer's CONSTRUCTOR!!
    * @param viewer the viewer with which to share resources.
    */
   public synchronized void registerViewer(Object viewer) {
      viewers.add (viewer);
   }
   
   /**
    * Unregisters a particular viewer with this set of resources.
    * MUST BE CALLED IN THE GLViewer's dispose() METHOD.
    */
   public synchronized void deregisterViewer(Object viewer) {
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
    */
   public void dispose(GL gl) {
      for (GLTexture tex : textureMap.values ()) {
         tex.releaseDispose (gl);
      }
      textureMap.clear (); // other sources should be cleared by the garbage man separately
   }
   
   /**
    * Potentially release invalid texture associated with this key
    * @param key for grabbing texture
    * @param tex stored texture object
    * @return true if texture is invalid and released
    */
   private boolean maybeReleaseTexture(TextureContent key, GLTexture tex) {
      if (!tex.isValid ()) {
         // release
         tex.release ();
         synchronized(textureMap) {
            textureMap.remove (key);
         }
         return true;
      }
      
      return false;
   }
   
   private boolean maybeUpdateTexture(GL gl, GLTexture texture, TextureContent content) {
      boolean update = false;
      Rectangle dirty = content.getDirty ();
      if (dirty != null) {
         
         ContentFormat format = content.getFormat ();
         int glFormat = 0;
         int glType = 0;
         int[] swizzle = null;
         
         switch(format) {
            case GRAYSCALE_ALPHA_BYTE_2:
               if (gl.isGL3()) {
                  glFormat = GL3.GL_RG;
                  swizzle = GLTextureLoader.SWIZZLE_GRAY_ALPHA;
               } else if (gl.isGL2()) {
                  glFormat = GL2.GL_LUMINANCE_ALPHA;
               }
               glType = GL.GL_UNSIGNED_BYTE;
               break;
            case GRAYSCALE_ALPHA_SHORT_2: 
               if (gl.isGL3()) {
                  glFormat = GL3.GL_RG;
                  swizzle = GLTextureLoader.SWIZZLE_GRAY_ALPHA;
               } else if (gl.isGL2()) {
                  glFormat = GL2.GL_LUMINANCE_ALPHA;
               }
               glType = GL.GL_UNSIGNED_SHORT;
               break;
            case GRAYSCALE_BYTE:
               if (gl.isGL3()) {
                  glFormat = GL3.GL_RED;
                  swizzle = GLTextureLoader.SWIZZLE_GRAY;
               } else if (gl.isGL2()) {
                  glFormat = GL2.GL_LUMINANCE;
               }
               glType = GL.GL_BYTE;
               break;
            case GRAYSCALE_UBYTE:
               if (gl.isGL3()) {
                  glFormat = GL3.GL_RED;
                  swizzle = GLTextureLoader.SWIZZLE_GRAY;
               } else if (gl.isGL2()) {
                  glFormat = GL2.GL_LUMINANCE;
               }
               glType = GL.GL_UNSIGNED_BYTE;
               break;
            case GRAYSCALE_SHORT:
               if (gl.isGL3()) {
                  glFormat = GL3.GL_RED;
                  swizzle = GLTextureLoader.SWIZZLE_GRAY;
               } else if (gl.isGL2()) {
                  glFormat = GL2.GL_LUMINANCE;
               }
               glType = GL.GL_SHORT;
               break;
            case GRAYSCALE_USHORT:
               if (gl.isGL3()) {
                  glFormat = GL3.GL_RED;
                  swizzle = GLTextureLoader.SWIZZLE_GRAY;
               } else if (gl.isGL2()) {
                  glFormat = GL2.GL_LUMINANCE;
               }
               glType = GL.GL_UNSIGNED_SHORT;
               break;
            case RGBA_BYTE_4:
               glFormat = GL.GL_RGBA;
               glType = GL.GL_UNSIGNED_BYTE;
               if (gl.isGL3()) {
                  swizzle = GLTextureLoader.SWIZZLE_RGBA;
               }
               break;
            case RGBA_INTEGER:
               glFormat = GL2GL3.GL_RGBA_INTEGER;
               glType = GL2GL3.GL_UNSIGNED_INT_8_8_8_8;
               if (gl.isGL3()) {
                  swizzle = GLTextureLoader.SWIZZLE_RGBA;
               }
               break;
            case RGB_BYTE_3:
               glFormat = GL.GL_RGB;
               glType = GL.GL_UNSIGNED_BYTE;
               if (gl.isGL3()) {
                  swizzle = GLTextureLoader.SWIZZLE_RGBA;
               }
               break;
            default:
               break;
         }
         
         update = true;
         int psize = content.getPixelSize ();
         int width = dirty.width ();
         int height = dirty.height ();
         
         if (texture.getFormat() != glFormat || texture.getType() != glType) {
            // re-create entire texture
            width = content.getWidth();
            height = content.getHeight();
            
            ByteBuffer buff = BufferUtilities.newNativeByteBuffer (width*height*psize);
            content.getData (buff);
            buff.flip ();
            texture.fill(gl, width, height, psize, glFormat, glType, swizzle, buff);
            buff = BufferUtilities.freeDirectBuffer (buff);
         } else {
            ByteBuffer buff = BufferUtilities.newNativeByteBuffer (width*height*psize);
            content.getData (dirty, buff);
            buff.flip ();
            texture.fill (gl, dirty.x (), dirty.y (), width, height, psize, glFormat, glType, buff);
            buff = BufferUtilities.freeDirectBuffer (buff);
         }
         content.markClean ();
      }
      return update;
   }
   
   public GLTexture getTexture(GL gl, TextureContent content) {
      
      GLTexture tex = null;
      // check if texture exists in map
      synchronized(textureMap) {
         tex = textureMap.get (content);
         if (tex == null || maybeReleaseTexture(content, tex)) {
            return null; 
         }
      }
      
      maybeUpdateTexture (gl, tex, content);
      return tex;
   }
   
   public GLTexture getOrLoadTexture(GL gl, TextureContent content) {
      
      GLTexture tex = null;
      synchronized(textureMap) {
         tex = getTexture(gl, content);
         if (tex != null && !tex.disposeInvalid (gl)) {
            maybeUpdateTexture (gl, tex, content);
            return tex;
         }
         
         // load texture
         synchronized (textureLoader) {
            tex = textureLoader.getAcquiredTexture (gl, content);
            textureMap.put (content, tex);
         }
         
      }
      return tex;
   }
   
   @Override
   public void init (GLAutoDrawable drawable) {
      Logger.getSystemLogger().debug("Master drawable initialized");
   }

   @Override
   public void dispose (GLAutoDrawable drawable) {
      Logger.getSystemLogger().debug("Master drawable disposed");
      textureLoader.clearAllTextures (); // clean up
   }
   
   public void track(GLResource res) {
      synchronized (resources) {
         resources.track (res);
      }
   }
   
   public void disposeResource(GLResource res) {
      garbagebin.trash (res);
   }
   
   
   public void runGarbageCollection(GL gl) {
      garbageman.collect(gl);
   }
   
   /**
    * Runs garbage collection if sufficient time has
    * passed since the last collection, as specified
    * by the garbage collection interval
    * @return true if garbage collection run
    */
   public boolean maybeRunGarbageCollection(GL gl) {
      return garbageman.maybeCollect (gl, garbageCollectionInterval);
   }
   
   /**
    * Enables or disables an automatic garbage timer.  This
    * timer runs on a separate thread.
    */
   public void setGarbageTimerEnabled(boolean set) {
      if (set != garbageTimerEnabled) {
         
         if (set) {
            masterRedrawThread = new MasterRedrawThread (masterDrawable, garbageCollectionInterval);
            masterRedrawThread.setName ("GL Garbage Collector - " + masterDrawable.getHandle ());
            masterRedrawThread.start ();
         } else {
            if (masterRedrawThread != null) {
               masterRedrawThread.terminate ();
               masterRedrawThread = null;
            }
         }
         
         garbageTimerEnabled = set;
      }
   }
   
   public boolean isGarbageTimerEnabled() {
      return garbageTimerEnabled;
   }
   
   /**
    * Time interval for running garbage collection, either with a
    * separate timed thread, or by manual calls to {@link #maybeRunGarbageCollection(GL)}.
    * @param ms time interval for collection in ms
    */
   public void setGarbageCollectionInterval(long ms) {
      garbageCollectionInterval = ms;
      if (masterRedrawThread != null) {
         masterRedrawThread.setRedrawInterval (ms);
      }
   }
   
   public long getGarbageCollectionInterval() {
      return garbageCollectionInterval;
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
