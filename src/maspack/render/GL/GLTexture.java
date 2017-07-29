package maspack.render.GL;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;

import maspack.render.TextureMapProps.TextureFilter;
import maspack.render.TextureMapProps.TextureWrapping;

/**
 * A texture to be bound within JOGL. This object is responsible for keeping
 * track of a given OpenGL texture.
 * 
 * @author Antonio Sanchez
 */
public class GLTexture extends GLResourceBase {
   
   /** The GL target type, i.e. where to bind to */
   private int target;
   /** The GL texture ID, i.e. what to bind */
   private int textureID;
   /** The height of the image */
   private int height;
   /** The width of the image */
   private int width;
   
   private int format;
   private int type;

   private TextureWrapping sWrapping;
   private TextureWrapping tWrapping;
   private boolean wrappingChanged;
   private TextureFilter minFilter;
   private TextureFilter magFilter;
   private boolean hasMipmaps;
   private boolean filtersChanged;
   
   private float[] borderColor;
   private boolean borderColorChanged;
   private float[] DEFAULT_BORDER_COLOR = {0f, 0f, 0f, 0f};
   
   /**
    * Create a new texture
    * 
    * @param target
    * The GL target
    * @param textureID
    * The GL texture ID
    */
   public GLTexture (int target, int textureID) {
      this.target = target;
      this.textureID = textureID;
      borderColor = new float[4];
      hasMipmaps = false;
      setFormat(0);
      setType(0);
      
      // default filters
      setWrapping (TextureWrapping.REPEAT, TextureWrapping.REPEAT);
      setFilters (TextureFilter.LINEAR_MIPMAP_LINEAR, TextureFilter.LINEAR);
      setBorderColor(DEFAULT_BORDER_COLOR);
   }

   /** 
    * Get the GL texture ID
	*/
   public int getTextureId() {
      return textureID;
   }
   
   public int getTarget() {
      return target;
   }
   
   public int getFormat() {
      return format;
   }
   
   private void setFormat(int format) {
      this.format = format;
   }
   
   public int getType() {
      return type;
   }
   
   private void setType(int type) {
      this.type = type;
   }
   
   protected static int getGLWrapping(TextureWrapping wrapping) {
      switch (wrapping) {
         case CLAMP_TO_EDGE:
            return GL.GL_CLAMP_TO_EDGE;
         case CLAMP_TO_BORDER:
            return GL2GL3.GL_CLAMP_TO_BORDER;
         case MIRRORED_REPEAT:
            return GL.GL_MIRRORED_REPEAT;
         case REPEAT:
            return GL.GL_REPEAT;
         default:
            break;
      }
      return GL.GL_REPEAT;
   }
   
   protected static int getGLMinFilter(TextureFilter filter) {
      switch (filter) {
         case LINEAR:
            return GL.GL_LINEAR;
         case LINEAR_MIPMAP_LINEAR:
            return GL.GL_LINEAR_MIPMAP_LINEAR;
         case LINEAR_MIPMAP_NEAREST:
            return GL.GL_LINEAR_MIPMAP_NEAREST;
         case NEAREST:
            return GL.GL_NEAREST;
         case NEAREST_MIPMAP_LINEAR:
            return GL.GL_NEAREST_MIPMAP_LINEAR;
         case NEAREST_MIPMAP_NEAREST:
            return GL.GL_NEAREST_MIPMAP_NEAREST;
         
      }
      return GL.GL_LINEAR_MIPMAP_LINEAR;
   }

   protected static int getGLMagFilter(TextureFilter filter) {
      switch (filter) {
         case LINEAR:
         case LINEAR_MIPMAP_LINEAR:
         case NEAREST_MIPMAP_LINEAR:
            return GL.GL_LINEAR;
         case LINEAR_MIPMAP_NEAREST:
         case NEAREST:
         case NEAREST_MIPMAP_NEAREST:
            return GL.GL_NEAREST;
      }
      return GL.GL_LINEAR;
   }
   
   /**
    * Bind the specified GL context to a texture, automatically
    * increases the use count of the texture.
    * 
    * @param gl
    * The GL context to bind to
    */
   public void bind (GL gl) {
      gl.glBindTexture (target, textureID);
      
      // set parameters
      if (wrappingChanged) {
         gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, getGLWrapping (sWrapping));
         gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, getGLWrapping (tWrapping));
         wrappingChanged = false;
      }
      if (filtersChanged) {
         gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, getGLMinFilter (minFilter));
         gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, getGLMagFilter (magFilter));
         filtersChanged = false;
      }
      // if mipmaps requested, make sure they are generated
      if (!hasMipmaps && (minFilter == TextureFilter.LINEAR_MIPMAP_LINEAR ||
           minFilter == TextureFilter.LINEAR_MIPMAP_NEAREST ||
           minFilter == TextureFilter.NEAREST_MIPMAP_LINEAR ||
           minFilter == TextureFilter.NEAREST_MIPMAP_NEAREST) ) {
         gl.glGenerateMipmap (target);
         hasMipmaps = true;
      }
      if (borderColorChanged) {
         gl.glTexParameterfv(target, GL2GL3.GL_TEXTURE_BORDER_COLOR, borderColor, 0);
         borderColorChanged = false;
      }
   }
   
   /**
    * Unbind the texture
    */
   public void unbind(GL gl) {
      gl.glBindTexture (target, 0);
   }
   
   public void setFilters(TextureFilter minFilter, TextureFilter magFilter) {
      this.minFilter = minFilter;
      this.magFilter = magFilter;
      filtersChanged = true;
   }
   
   public void setBorderColor(float[] rgba) {
      borderColor[0] = rgba[0];
      borderColor[1] = rgba[1];
      borderColor[2] = rgba[2];
      if (rgba.length < 4) {
         borderColor[3] = 1.0f;
      } else {
         borderColor[3] = rgba[3];
      }
      borderColorChanged = true;
   }
   
   public void setWrapping(TextureWrapping sWrapping, TextureWrapping tWrapping) {
      this.sWrapping = sWrapping;
      this.tWrapping = tWrapping;
      wrappingChanged = true;
   }

   /**
    * Delete the current texture in a context
    */
   @Override
   public void dispose(GL gl) {
      if (textureID >= 0) {
         gl.glDeleteTextures(1, new int[] {textureID}, 0);
         textureID = -1;
      }
   }
   
   @Override
   public boolean isDisposed () {
      return (textureID <= 0);
   }

   /**
    * Set the height of the image
    * 
    * @param height
    * The height of the image
    */
   public void setHeight (int height) {
      this.height = height;
   }

   /**
    * Set the width of the image
    * 
    * @param width The width of the image
    */
   public void setWidth (int width) {
      this.width = width;
   }

  
   /**
    * Get the width of the physical texture
    * 
    * @return The width of physical texture
    */
   public float getWidth() {
      return width;
   }

   /**
    * Get the height of the physical texture
    * 
    * @return The height of physical texture
    */
   public float getHeight() {
      return height;
   }

        
   
   @Override
   public GLTexture acquire () {
      return (GLTexture)super.acquire ();
   }
   
   public void fill(GL gl, int width, int height, int pixelBytes, 
      int glFormat, int glType, int[] swizzle, ByteBuffer data) {
   
      setWidth (width);
      setHeight (height);
      setFormat (glFormat);
      setType(glType);
      
      gl.glBindTexture (target, textureID); // internal bind so doesn't generate mip maps
      
      // if pixels don't align to 4 bytes, adjust unpack
      boolean unpacked = false;
      int pixelWidth = pixelBytes*width;
      if (pixelWidth % 4 != 0) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
         unpacked = true;
      }
      
      // produce a texture from the byte buffer
      gl.glTexImage2D (target, 0, GL.GL_RGBA, width, height, 
         0, glFormat, glType, data);
      hasMipmaps = false;  // trigger regenerating mipmaps
      if (swizzle != null) {
         gl.glTexParameteriv (GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_SWIZZLE_RGBA, swizzle, 0);
      }
      
      // restore pixel alignment
      if (unpacked) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 4);
      }
   }
   
   public void fill(GL gl, int x, int y, int width, int height, int pixelBytes,
      int glFormat, int glType, ByteBuffer data) {
      
      gl.glBindTexture (target, textureID); // internal bind so doesn't generate mip maps, etc...
      
      // if pixels don't align to 4 bytes, adjust unpack
      boolean unpacked = false;
      int pixelWidth = pixelBytes*width;
      if (pixelWidth % 4 != 0) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
         unpacked = true;
      }
      
      // produce a texture from the byte buffer
      gl.glTexSubImage2D (target, 0, x, y, width, height, glFormat, glType, data);
      hasMipmaps = false;  // trigger regenerating mipmaps
      
      // restore pixel alignment
      if (unpacked) {
         gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 4);
      }
      
   }
   

   public static GLTexture generate(GL gl, int target) {
      int[] v = new int[1];
      gl.glGenTextures (1, v, 0);
      int id = v[0];
      return new GLTexture (target, id);
   }
   
}
