package maspack.render.GL;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import maspack.render.TexturePropsBase.TextureFilter;
import maspack.render.TexturePropsBase.TextureWrapping;

/**
 * A texture to be bound within JOGL. This object is responsible for keeping
 * track of a given OpenGL texture.
 * 
 * Since textures need to be powers of 2 the actual texture may be considerably
 * bigger than the source image and hence the texture mapping coordinates need
 * to be adjusted to match-up drawing the sprite against the texture.
 * 
 * @author Kevin Glass, Antonio Sanchez
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
   /** The width of the texture */
   private int texWidth;
   /** The height of the texture */
   private int texHeight;
   /** The ratio of the width of the image to the texture */
   private float widthRatio;
   /** The ratio of the height of the image to the texture */
   private float heightRatio;

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
   public GLTexture (GL gl, int target, int textureID) {
      this.target = target;
      this.textureID = textureID;
      borderColor = new float[4];
      hasMipmaps = false;
      
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
         // if mipmaps requested, make sure they are generated
         if (!hasMipmaps && (minFilter == TextureFilter.LINEAR_MIPMAP_LINEAR ||
              minFilter == TextureFilter.LINEAR_MIPMAP_NEAREST ||
              minFilter == TextureFilter.NEAREST_MIPMAP_LINEAR ||
              minFilter == TextureFilter.NEAREST_MIPMAP_NEAREST) ) {
            gl.glGenerateMipmap (target);
            hasMipmaps = true;
         }
         filtersChanged = false;
      }
      if (borderColorChanged) {
         gl.glTexParameterfv(target, GL2GL3.GL_TEXTURE_BORDER_COLOR, borderColor, 0);
         borderColorChanged = false;
      }
   }
   
   /**
    * Unbind the texture
    * @param gl
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
   protected void internalDispose(GL gl) {
      if (textureID > 0) {
         super.dispose(gl);
         gl.glDeleteTextures(1, new int[] {textureID}, 0);
      }
   }

   /**
    * Set the height of the image
    * 
    * @param height
    * The height of the image
    */
   public void setHeight (int height) {
      this.height = height;
      setHeight();
   }

   /**
    * Set the width of the image
    * 
    * @param width
    * The width of the image
    */
   public void setWidth (int width) {
      this.width = width;
      setWidth();
   }

   /**
    * Get the height of the original image
    * 
    * @return The height of the original image
    */
   public int getImageHeight() {
      return height;
   }

   /**
    * Get the width of the original image
    * 
    * @return The width of the original image
    */
   public int getImageWidth() {
      return width;
   }

   /**
    * Get the height of the physical texture
    * 
    * @return The height of physical texture
    */
   public float getHeight() {
      return heightRatio;
   }

   /**
    * Get the width of the physical texture
    * 
    * @return The width of physical texture
    */
   public float getWidth() {
      return widthRatio;
   }

   /**
    * Set the height of this texture
    * 
    * @param texHeight
    * The height of the texture
    */
   public void setTextureHeight (int texHeight) {
      this.texHeight = texHeight;
      setHeight();
   }

   /**
    * Set the width of this texture
    * 
    * @param texWidth
    * The width of the texture
    */
   public void setTextureWidth (int texWidth) {
      this.texWidth = texWidth;
      setWidth();
   }

   /**
    * Set the height of the texture. This will update the ratio also.
    */
   private void setHeight() {
      if (texHeight != 0) {
         heightRatio = ((float)height) / texHeight;
      }
   }

   /**
    * Set the width of the texture. This will update the ratio also.
    */
   private void setWidth() {
      if (texWidth != 0) {
         widthRatio = ((float)width) / texWidth;
      }
   }
   
   @Override
   public GLTexture acquire () {
      return (GLTexture)super.acquire ();
   }

}
