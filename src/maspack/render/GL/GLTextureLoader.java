package maspack.render.GL;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;

import maspack.render.TextureContent;
import maspack.render.TextureContent.ContentFormat;
import maspack.util.BufferUtilities;

/**
 * A utility class to load textures for JOGL. This source is based on a texture
 * that can be found in the Java Gaming (www.javagaming.org) Wiki. It has been
 * simplified slightly for explicit 2D graphics use.
 * 
 * OpenGL uses a particular image format. Since the images that are loaded from
 * disk may not match this format this loader introduces a intermediate image
 * which the source image is copied into. In turn, this image is used as source
 * for the OpenGL texture.
 * 
 * @author Kevin Glass, Kees van den Doel, Antonio Sanchez
 */
public class GLTextureLoader implements GLGarbageSource {

   /** The table of textures that have been loaded in this loader */
   private HashMap<Object,GLTexture> table = new HashMap<Object,GLTexture>();
   HashSet<GLTexture> toRemove = new HashSet<>();

   /**
    * Create a new texture loader based on the game panel
    */
   public GLTextureLoader () {
   }

   /**
    * Create a new texture ID
    * 
    * @param gl
    * The GL content
    * @return A new texture ID
    */
   private int createTextureId(GL gl) {
      int[] tmp = new int[1];
      gl.glGenTextures(1, tmp, 0);
      return tmp[0];
   }

   /**
    * Load a texture with one held reference to prevent garbage collection
    * 
    * @return The loaded texture
    */
   public GLTexture getAcquiredTexture (GL gl, TextureContent content) {
      maybeClearTextures(gl);

      GLTexture tex = null;
      Object key = content.getKey ();
      synchronized(table) {
         tex = table.get (content.getKey ());

         if (tex != null) {
            return tex.acquire ();
         }

         tex = createTexture(gl, GL.GL_TEXTURE_2D, content).acquire ();

         table.put (key, tex);

      }

      return tex;
   }
   
   public static final int[] SWIZZLE_GRAY_ALPHA = {GL3.GL_RED, GL3.GL_RED, GL3.GL_RED, GL3.GL_GREEN};
   public static final int[] SWIZZLE_GRAY = {GL3.GL_RED, GL3.GL_RED, GL3.GL_RED, GL3.GL_ALPHA};
   public static final int[] SWIZZLE_RGBA = {GL3.GL_RED, GL3.GL_GREEN, GL3.GL_BLUE, GL3.GL_ALPHA};

   public GLTexture createTexture(GL gl, int target, TextureContent content) {
      
      // create the texture ID for this texture
      int textureID = createTextureId(gl);
      GLTexture texture = new GLTexture (target, textureID);
      
      int width = content.getWidth ();
      int height = content.getHeight ();
      int pixelSize = content.getPixelSize ();
      texture.setWidth (width);
      texture.setHeight (height);
      
      int glFormat = 0;
      int glType = 0;
      int[] swizzle = null;
      
      ContentFormat format = content.getFormat ();
      switch(format) {
         case GRAYSCALE_ALPHA_BYTE_2:
            if (gl.isGL3 ()) {
               glFormat = GL3.GL_RG;
               swizzle = SWIZZLE_GRAY_ALPHA;
            } else if (gl.isGL2 ()) {
               glFormat = GL2.GL_LUMINANCE_ALPHA;
            }
            glType = GL.GL_UNSIGNED_BYTE;
            break;
         case GRAYSCALE_ALPHA_SHORT_2: 
            if (gl.isGL3()) {
               glFormat = GL3.GL_RG;
               swizzle = SWIZZLE_GRAY_ALPHA;
            } else if (gl.isGL2()) {
               glFormat = GL2.GL_LUMINANCE_ALPHA;
            }
            glType = GL.GL_UNSIGNED_SHORT;
            break;
         case GRAYSCALE_BYTE:
            if (gl.isGL3()) {
               glFormat = GL3.GL_RED;
               swizzle = SWIZZLE_GRAY;
            } else if (gl.isGL2()) {
               glFormat = GL2.GL_LUMINANCE;
            }
            glType = GL.GL_BYTE;
            break;
         case GRAYSCALE_UBYTE:
            if (gl.isGL3()) {
               glFormat = GL3.GL_RED;
               swizzle = SWIZZLE_GRAY;
            } else if (gl.isGL2()) {
               glFormat = GL2.GL_LUMINANCE;
            }
            glType = GL.GL_UNSIGNED_BYTE;
            break;
         case GRAYSCALE_SHORT:
            if (gl.isGL3()) {
               glFormat = GL3.GL_RED;
               swizzle = SWIZZLE_GRAY;
            } else if (gl.isGL2()) {
               glFormat = GL2.GL_LUMINANCE;
            }
            glType = GL.GL_SHORT;
            break;
         case GRAYSCALE_USHORT:
            if (gl.isGL3()) {
               glFormat = GL3.GL_RED;
               swizzle = SWIZZLE_GRAY;
            } else if (gl.isGL2()) {
               glFormat = GL2.GL_LUMINANCE;
            }
            glType = GL.GL_UNSIGNED_SHORT;
            break;
         case RGBA_BYTE_4:
            glFormat = GL.GL_RGBA;
            glType = GL.GL_UNSIGNED_BYTE;
            break;
         case RGBA_INTEGER:
            glFormat = GL2GL3.GL_RGBA_INTEGER;
            glType = GL2GL3.GL_UNSIGNED_INT_8_8_8_8;
            break;
         case RGB_BYTE_3:
            glFormat = GL.GL_RGB;
            glType = GL.GL_UNSIGNED_BYTE;
            break;
         default:
            break;
         
      }

      ByteBuffer buff = BufferUtilities.newNativeByteBuffer (width*height*pixelSize);
      content.getData (buff);
      buff.flip ();
      texture.fill(gl, width, height, pixelSize, glFormat, glType, swizzle, buff);
      buff = BufferUtilities.freeDirectBuffer (buff);
      content.markClean ();
      
      return texture;
   }
  
   public boolean clearTexture(GL gl, String id) {
      maybeClearTextures(gl);
      
      synchronized(table) {
         GLTexture tex = table.remove (id);
         if (tex != null) {
            tex.dispose(gl);
            return true;
         }
      }
      return false;
   }

   public boolean clearTexture(String id) {
      synchronized (table) {
         GLTexture tex = table.remove(id);
         if (tex != null) {
            synchronized(toRemove) {
               toRemove.add (tex);
            }
            return true;
         }
      }
      return false;
   }

   public void clearAllTextures() {
      synchronized(table) {
         synchronized(toRemove) {
            toRemove.addAll (table.values());
         }
         table.clear();
      }
   }

   public void clearAllTextures(GL gl) {
      maybeClearTextures(gl);
      synchronized(table) {
         for (Entry<Object,GLTexture> entry : table.entrySet()) {
            entry.getValue().dispose(gl);
         }
         table.clear();
      }
   }

   private void maybeClearTextures(GL gl) {
      synchronized(toRemove) {
         if (toRemove.size () > 0) {
            for (GLTexture tex : toRemove) {
               tex.dispose (gl);
            }
            toRemove.clear ();
         }
      }
   }

   public GLTexture getTextureByNameAcquired (String name) {
      GLTexture tex = null;
      synchronized(table) {
         tex = table.get(name);
         if (tex != null) {
            return tex.acquire ();
         }
      }
      return tex;
   }

   public boolean isTextureValid(String id) {
      GLTexture tex = null;
      synchronized(table) {
         tex = table.get(id);

         if (tex == null) {
            return false;
         }
         if (tex.getTextureId() <= 0) {
            return false;
         }
      }
      return true;
   }

   public void dispose(GL gl) {
      maybeClearTextures (gl);
   }

   @Override
   public synchronized void garbage (GL gl) {
      maybeClearTextures (gl);
      
      // go through table
      synchronized(table) {
         Iterator<Entry<Object,GLTexture>> it = table.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<Object,GLTexture> entry = it.next ();
            GLTexture tex = entry.getValue ();
            if (tex.disposeUnreferenced (gl)) {
               it.remove ();
            }
         }
      }
   }
}
