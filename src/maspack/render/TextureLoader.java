package maspack.render;

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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

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
 * @author Kevin Glass (Modified by Kees van den Doel)
 */
public class TextureLoader {
   /** The table of textures that have been loaded in this loader */
   private HashMap<String,Texture> table = new HashMap<String,Texture>();
   /** The GL context used to load textures */
   private GL2 gl;
   /** The colour model including alpha for the GL image */
   private ColorModel glAlphaColorModel;
   /** The colour model for the GL image */
   private ColorModel glColorModel;

   /**
    * Create a new texture loader based on the game panel
    * 
    * @param gl
    * The GL content in which the textures should be loaded
    */
   public TextureLoader (GL2 gl) {
      this.gl = gl;

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
    * @return A new texture ID
    */
   private int createTextureID() {
      int[] tmp = new int[1];
      gl.glGenTextures (1, tmp, 0);
      return tmp[0];
   }

   /**
    * Load a texture
    * 
    * @param resourceName
    * The location of the resource to load
    * @return The loaded texture
    * @throws IOException
    * Indicates a failure to access the resource
    */
   public Texture getTexture (String resourceName) throws IOException {
      Texture tex = (Texture)table.get (resourceName);

      if (tex != null) {
         return tex;
      }

      tex = getTexture (resourceName, GL2.GL_TEXTURE_2D, // target

      GL2.GL_RGBA, // dst pixel format

         GL2.GL_LINEAR, // min filter (unused)

         GL2.GL_LINEAR);

      table.put (resourceName, tex);

      return tex;
   }

   /**
    * Load a texture into OpenGL from a image reference on disk.
    * 
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
   public Texture getTexture (
      String resourceName, int target, int dstPixelFormat, int minFilter,
      int magFilter) throws IOException {
      int srcPixelFormat = 0;

      // create the texture ID for this texture

      int textureID = createTextureID();
      Texture texture = new Texture (target, textureID);

      // bind this texture

      gl.glBindTexture (target, textureID);

      BufferedImage bufferedImage = loadImage (resourceName);
      int biw = bufferedImage.getWidth();
      int bih = bufferedImage.getHeight();
      
      if (biw != get2Fold(biw) || bih != get2Fold(bih)) {
         // rescale to be power of two
         Image sampled = bufferedImage.getScaledInstance(
            get2Fold(bufferedImage.getWidth()), 
            get2Fold(bufferedImage.getHeight()), 
            Image.SCALE_SMOOTH);
         bufferedImage = new BufferedImage(get2Fold(biw), get2Fold(bih), BufferedImage.TYPE_INT_ARGB);
         bufferedImage.getGraphics().drawImage(sampled, 0, 0 , null);
      }
      texture.setWidth (bufferedImage.getWidth());
      texture.setHeight (bufferedImage.getHeight());

      if (bufferedImage.getColorModel().hasAlpha()) {
         srcPixelFormat = GL2.GL_RGBA;
      }
      else {
         srcPixelFormat = GL2.GL_RGB;
      }

      // convert that image into a byte buffer of texture data

      ByteBuffer textureBuffer = convertImageData (bufferedImage, texture);

      if (target == GL2.GL_TEXTURE_2D) {
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MIN_FILTER, minFilter);
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MAG_FILTER, magFilter);
      }
      // required for new version of JOGL: reset the buffer postion
      textureBuffer.position (0);

      // produce a texture from the byte buffer
      gl.glTexImage2D (
         target, 0, dstPixelFormat, get2Fold (bufferedImage.getWidth()),
         get2Fold (bufferedImage.getHeight()), 0, srcPixelFormat,
         GL2.GL_UNSIGNED_BYTE, textureBuffer);

      return texture;
   }

   public Texture getTexture (String resourceName,
      byte[] buffer, int width, int height, 
      int srcPixelFormat, int dstPixelFormat) {
      
      Texture tex = (Texture)table.get (resourceName);
      if (tex != null) {
         return tex;
      }

      tex = getTexture (resourceName, buffer,
         width, height,
         srcPixelFormat, 
         GL2.GL_TEXTURE_2D, // target
         dstPixelFormat, // dst pixel format
         GL2.GL_LINEAR,  // min filter (unused)
         GL2.GL_LINEAR);

      table.put (resourceName, tex);

      return tex;
   }
   
   public Texture getTexture (
      String name, 
      byte[] bytes, int width, int height, int srcPixelFormat,
      int target, int dstPixelFormat, int minFilter,
      int magFilter) {

      // create the texture ID for this texture

      int textureID = createTextureID();
      Texture texture = new Texture (target, textureID);

      // bind this texture
      gl.glBindTexture (target, textureID);

      
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
      texture.setWidth (width);
      texture.setHeight (height);


      if (target == GL2.GL_TEXTURE_2D) {
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MIN_FILTER, minFilter);
         gl.glTexParameteri (target, GL2.GL_TEXTURE_MAG_FILTER, magFilter);
      }
      
      ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
      buffer.put(bytes);
      buffer.rewind();
      
      // if pixels don't align to 4 bytes, adjust unpack
      if (width % 4 != 0) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
      }
      
      // produce a texture from the byte buffer
      gl.glTexImage2D (
         target, 0, dstPixelFormat, width,
         height, 0, srcPixelFormat,
         GL2.GL_UNSIGNED_BYTE, buffer);

      if (width % 4 != 0) {
         // restore pixel alignment
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
   private ByteBuffer convertImageData (
      BufferedImage bufferedImage, Texture texture) {
      ByteBuffer imageBuffer = null;
      WritableRaster raster;
      BufferedImage texImage;

      int texWidth = 2;
      int texHeight = 2;

      // find the closest power of 2 for the width and height

      // of the produced texture

      while (texWidth < bufferedImage.getWidth()) {
         texWidth *= 2;
      }
      while (texHeight < bufferedImage.getHeight()) {
         texHeight *= 2;
      }

      texture.setTextureHeight (texHeight);
      texture.setTextureWidth (texWidth);

      // create a raster that can be used by OpenGL as a source

      // for a texture

      if (bufferedImage.getColorModel().hasAlpha()) {
         raster =
            Raster.createInterleavedRaster (
               DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
         texImage =
            new BufferedImage (
               glAlphaColorModel, raster, false, new Hashtable());
      }
      else {
         raster =
            Raster.createInterleavedRaster (
               DataBuffer.TYPE_BYTE, texWidth, texHeight, 3, null);
         texImage =
            new BufferedImage (glColorModel, raster, false, new Hashtable());
      }

      // copy the source image into the produced image

      Graphics g = texImage.getGraphics();
      g.setColor (new Color (0f, 0f, 0f, 0f));
      g.fillRect (0, 0, texWidth, texHeight);
      g.drawImage (bufferedImage, 0, 0, null);

      // build a byte buffer from the temporary image

      // that be used by OpenGL to produce a texture.

      byte[] data =
         ((DataBufferByte)texImage.getRaster().getDataBuffer()).getData();

      imageBuffer = ByteBuffer.allocateDirect (data.length);
      imageBuffer.order (ByteOrder.nativeOrder());
      imageBuffer.put (data, 0, data.length);

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
   private BufferedImage loadImage (String ref) throws IOException {
      BufferedImage bufferedImage = ImageIO.read (new java.io.File (ref));

      return bufferedImage;
   }
   
   public GL2 getGL2() {
      return gl;
   }
   
   public void clearTexture(String id) {
      Texture tex = table.get (id);
      if (tex != null) {
         tex.delete(gl);
         table.remove(id);
      }
   }
   
   public void clearAllTextures() {
      for (Entry<String,Texture> entry : table.entrySet()) {
         entry.getValue().delete(gl);
      }
      table.clear();
   }
   
   public Texture getTextureByName (String name) {
      Texture tex = table.get(name);
      return tex;
   }
   
   public boolean isTextureValid(String id) {
      Texture tex = table.get(id);
      if (tex == null) {
         return false;
      }
      if (tex.getTextureId() <= 0) {
         return false;
      }
      return true;
   }
}
