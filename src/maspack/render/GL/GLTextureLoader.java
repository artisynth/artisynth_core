package maspack.render.GL;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

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
   private HashMap<String,GLTexture> table = new HashMap<String,GLTexture>();
   HashSet<GLTexture> toRemove = new HashSet<>();

   /** The GL context used to load textures */
   /** The colour model including alpha for the GL image */
   private ColorModel glAlphaColorModel;
   /** The colour model for the GL image */
   private ColorModel glColorModel;

   /**
    * Create a new texture loader based on the game panel
    */
   public GLTextureLoader () {
      glAlphaColorModel =
      new ComponentColorModel (
         ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] { 8, 8, 8, 8 }, true, false,
         ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);

      glColorModel =
      new ComponentColorModel (
         ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] { 8, 8, 8, 0 }, false, false, ComponentColorModel.OPAQUE,
         DataBuffer.TYPE_BYTE);
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
    * @param resourceName
    * The location of the resource to load
    * @return The loaded texture
    * @throws IOException
    * Indicates a failure to access the resource
    */
   public GLTexture getTextureAcquired (GL gl, String resourceName) throws IOException {
      maybeClearTextures(gl);
      GLTexture tex = null;
      synchronized(table) {
         tex = table.get (resourceName);
         if (tex != null) {
            return tex.acquire ();
         }

         // this is an acquired texture
         tex = getTexture (gl, resourceName, GL2.GL_TEXTURE_2D, // target
            GL2.GL_RGBA,    // dst pixel format
            GL2.GL_LINEAR,  // min filter (unused)
            GL2.GL_LINEAR).acquire ();

         // potential for duplicate load, check and prevent
         table.put (resourceName, tex);

      }
      return tex;
   }

   /**
    * Load a texture with one held reference to prevent garbage collection
    * 
    * @param resourceName
    * key for referring to loaded texture
    * @param resourceFileName
    * filename for loading texture
    * @return The loaded texture
    * @throws IOException
    * Indicates a failure to access the resource
    */
   public GLTexture getTextureAcquired (GL gl, String resourceName, String resourceFileName) throws IOException {
      maybeClearTextures(gl);

      GLTexture tex = null;
      synchronized(table) {
         tex = table.get (resourceName);

         if (tex != null) {
            return tex.acquire ();
         }

         // acquired texture
         tex = getTexture (gl, resourceFileName, 
            GL2.GL_TEXTURE_2D, // target
            GL2.GL_RGBA,    // dst pixel format
            GL2.GL_LINEAR,  // min filter (unused)
            GL2.GL_LINEAR).acquire ();

         table.put (resourceName, tex);

      }

      return tex;
   }

   /**
    * Load a texture into OpenGL from a image reference on disk.
    * 
    * @param gl the opengl context
    * @param resourceName
    * The location of the resource to load
    * @param target
    * The GL target to load the texture against
    * @param dstPixelFormat
    * The pixel format of the screen
    * @param minFilter
    * The minimizing filter
    * @param magFilter
    * The magnification filter
    * @return The loaded texture
    * @throws IOException
    * Indicates a failure to access the resource
    */
   public GLTexture getTexture (GL gl,
      String resourceName, int target, int dstPixelFormat, 
      int minFilter, int magFilter) throws IOException {
      maybeClearTextures(gl);

      int srcPixelFormat = 0;

      // load image
      BufferedImage bufferedImage = loadImagePow2 (resourceName);
      // convert that image into a byte buffer of texture data
      ByteBuffer textureBuffer = convertImageData (bufferedImage);
      if (bufferedImage.getColorModel().hasAlpha()) {
         srcPixelFormat = GL2.GL_RGBA;
      } else {
         srcPixelFormat = GL2.GL_RGB;
      }

      // create the texture ID for this texture
      int textureID = createTextureId(gl);
      GLTexture texture = new GLTexture (target, textureID);
      texture.setWidth (bufferedImage.getWidth());
      texture.setHeight (bufferedImage.getHeight());

      // bind this texture
      gl.glBindTexture (target, textureID);
      if (target == GL2.GL_TEXTURE_2D) {
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MIN_FILTER, minFilter);
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MAG_FILTER, magFilter);
      }

      // produce a texture from the byte buffer
      gl.glTexImage2D (
         target, 0, dstPixelFormat, bufferedImage.getWidth(), bufferedImage.getHeight(), 
         0, srcPixelFormat,
         GL2.GL_UNSIGNED_BYTE, textureBuffer);

      return texture;
   }

   public GLTexture getTextureAcquired (GL gl, String resourceName,
      int target,
      byte[] buffer, int width, int height, 
      int srcPixelFormat, int dstPixelFormat) {

      maybeClearTextures(gl);
      GLTexture tex = null;
      synchronized(table) {
         tex = table.get (resourceName);

         if (tex != null) {
            return tex.acquire ();
         }

         tex = getTexture (gl, 
            resourceName, buffer,
            width, height,
            srcPixelFormat, 
            target, // target
            dstPixelFormat,    // dst pixel format
            GL2.GL_LINEAR,     // min filter (unused)
            GL2.GL_LINEAR).acquire ();

         GLTexture oldtex = table.put (resourceName, tex);
         if (oldtex != null) {
            tex.dispose (gl);
            tex = oldtex;
            table.put (resourceName, tex);
         }
      }

      return tex;
   }

   public GLTexture getTexture (
      GL gl,
      String name, 
      byte[] bytes, int width, int height, int srcPixelFormat,
      int target, int dstPixelFormat, int minFilter,
      int magFilter) {
      maybeClearTextures(gl);
      //    if (width != get2Fold(width) || height != get2Fold(height)) {
      // rescale to be power of two?
      //       
      //         DataBufferByte buffer = new DataBufferByte(bytes, bytes.length);
      //         BufferedImage bi = new BufferedImage(width, height, imageType);
      //         TYPE_INT_RGB
      //         TYPE_INT_ARGB
      //         TYPE_INT_ARGB_PRE
      //         TYPE_INT_BGR
      //         TYPE_3BYTE_BGR
      //         TYPE_4BYTE_ABGR
      //         TYPE_4BYTE_ABGR_PRE
      //         TYPE_BYTE_GRAY
      //         TYPE_USHORT_GRAY
      //         TYPE_BYTE_BINARY
      //         TYPE_BYTE_INDEXED
      //         TYPE_USHORT_565_RGB
      //         TYPE_USHORT_555_RGB
      //         switch (imageType) {
      //            
      //         }
      //         bi.setData(Raster.createInterleavedRaster(buffer, width, height, bands, location));
      //         
      //         //return new BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, new int[]{0, 1, 2}, null), false, null);
      //         
      //         Image sampled = new BufferedImage(width, height, imageType);
      //         sampled.
      //         Image sampled = bufferedImage.getScaledInstance(
      //            get2Fold(bufferedImage.getWidth()), 
      //            get2Fold(bufferedImage.getHeight()), 
      //            Image.SCALE_SMOOTH);
      //         bufferedImage = new BufferedImage(get2Fold(biw), get2Fold(bih), BufferedImage.TYPE_INT_ARGB);
      //         bufferedImage.getGraphics().drawImage(sampled, 0, 0 , null);
      //      }

      // create the texture ID for this texture
      int textureID = createTextureId(gl);
      GLTexture texture = new GLTexture (target, textureID);

      // bind this texture
      gl.glBindTexture (target, textureID);
      texture.setWidth (width);
      texture.setHeight (height);

      if (target == GL2.GL_TEXTURE_2D) {
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MIN_FILTER, minFilter);
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MAG_FILTER, magFilter);
      }

      ByteBuffer buffer = BufferUtilities.newNativeByteBuffer(bytes.length);
      buffer.put(bytes);
      buffer.flip ();

      // if pixels don't align to 4 bytes, adjust unpack
      if (width % 4 != 0) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
      }

      // produce a texture from the byte buffer
      gl.glTexImage2D (
         target, 0, dstPixelFormat, width,
         height, 0, srcPixelFormat,
         GL2.GL_UNSIGNED_BYTE, buffer);

      // restore pixel alignment
      if (width % 4 != 0) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 4);
      }

      return texture;
   }

   /**
    * Get the closest greater power of 2 to the fold number
    * 
    * @param fold
    * The target number
    * @return The power of 2
    */
   private int get2Fold (int fold) {
      int ret = 2;
      while (ret < fold) {
         ret *= 2;
      }
      return ret;
   }

   /**
    * Convert the buffered image to a texture
    * 
    * @param bufferedImage
    * The image to convert to a texture
    * @param texture
    * The texture to store the data into
    * @return A buffer containing the data
    */
   private ByteBuffer convertImageData (BufferedImage bufferedImage) {

      ByteBuffer imageBuffer = null;
      WritableRaster raster;
      BufferedImage texImage;
      int texWidth = bufferedImage.getWidth();
      int texHeight = bufferedImage.getHeight();

      // create a raster that can be used by OpenGL as a source
      // for a texture
      // XXX maybe check other color spaces?
      if (bufferedImage.getColorModel().hasAlpha()) {
         raster = Raster.createInterleavedRaster (
            DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
         texImage = new BufferedImage (
            glAlphaColorModel, raster, false, new Hashtable<String,Object>());
      } else {
         raster = Raster.createInterleavedRaster (
            DataBuffer.TYPE_BYTE, texWidth, texHeight, 3, null);
         texImage = new BufferedImage (glColorModel, raster, false, new Hashtable<String,Object>());
      }

      // copy the source image into the produced image
      Graphics g = texImage.getGraphics();
      g.setColor (new Color (0f, 0f, 0f, 0f));
      g.fillRect (0, 0, texWidth, texHeight);
      g.drawImage (bufferedImage, 0, 0, null);

      // build a byte buffer from the temporary image
      // that be used by OpenGL to produce a texture.
      byte[] data = ((DataBufferByte)texImage.getRaster().getDataBuffer()).getData();

      imageBuffer = BufferUtilities.newNativeByteBuffer (data.length);
      // flip rows
      int rowlength = data.length/texHeight;
      int pos = data.length-rowlength;
      for (int i=0; i<texHeight; ++i) {
         imageBuffer.put (data, pos, rowlength);
         pos -= rowlength;
      }
      imageBuffer.flip ();

      return imageBuffer;
   }

   /**
    * Load a given resource as a buffered image
    * 
    * @param ref
    * The location of the resource to load
    * @return The loaded buffered image
    * @throws IOException
    * Indicates a failure to find a resource
    */
   private BufferedImage loadImagePow2 (String ref) throws IOException {

      BufferedImage bufferedImage = ImageIO.read (new File (ref));

      // rescale to power-of-two width/height
      int biw = bufferedImage.getWidth();
      int bih = bufferedImage.getHeight();
      int biwp2 = get2Fold (biw);
      int bihp2 = get2Fold (bih);

      // scale to power-of-two
      if (biw != biwp2 || bih != bihp2) {
         // rescale to be power of two
         Image sampled = bufferedImage.getScaledInstance(biwp2, bihp2, Image.SCALE_SMOOTH);
         bufferedImage = new BufferedImage(biwp2, bihp2, bufferedImage.getType());
         bufferedImage.getGraphics().drawImage(sampled, 0, 0 , null);
      }
      return bufferedImage;
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
         for (Entry<String,GLTexture> entry : table.entrySet()) {
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
         Iterator<Entry<String,GLTexture>> it = table.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<String,GLTexture> entry = it.next ();
            GLTexture tex = entry.getValue ();
            if (tex.disposeUnreferenced (gl)) {
               it.remove ();
            }
         }
      }
   }
}
