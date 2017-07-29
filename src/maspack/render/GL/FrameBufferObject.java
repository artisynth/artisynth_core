/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;

import maspack.util.BufferUtilities;


public class FrameBufferObject {

   public static int defaultMultiSamples = 8;

   private int FBOhandle = -1; // Main FrameBuffer Object
   GLRenderBuffer depthBuffer = null;
   GLRenderBuffer colorBuffer = null;

   // intermediate fbo for blitting (no depth required)
   private int FBNhandle = -1;
   GLRenderBuffer outputColorBuffer = null;  // dummy color buffer for capturing bits

   public int x;
   public int y;
   public int width; // dimensions of framebuffer
   public int height;
   public int numMultiSamples;
   private boolean gammaCorrected;
   boolean initialized;
   
   // status returned by OpenGL after FBO init
   // if this isn't == GL_FRAMEBUFFER_COMPLETE then init() had a problem
   int status = 0;

   public FrameBufferObject (int x, int y, int w, int h, int nsamples, boolean gammaCorrection) {
      set(x,y,w,h,nsamples, gammaCorrection);
   }
   
   private void set(int x, int y, int w, int h, int nsamples, boolean gammaCorrection) {
      this.x = x;
      this.y = y;
      this.width = w;
      this.height = h;
      if (nsamples <= 0) {
         nsamples = defaultMultiSamples;
      }
      this.numMultiSamples = nsamples;
      this.gammaCorrected = gammaCorrection;
      this.initialized = false;
   }

   /**
    * Create a framebuffer with the given dimensions
    */
   public FrameBufferObject (int w, int h) {
      this(0,0,w,h,-1, false);
   }

   public FrameBufferObject (int w, int h, int nsamples) {
      this(0,0,w,h,nsamples, false);
   }

   /**
    * Create a framebuffer with the given dimensions
    */
   public FrameBufferObject (int x, int y, int w, int h) {
      this(x,y,w,h,-1, false);
   }

   public void configure(GL gl, int w, int h, int nsamples) {
      configure(gl, x, y, w, h, nsamples, gammaCorrected);
   }
   
   public void configure(GL gl, int w, int h, int nsamples, boolean gammaCorrected) {
      configure(gl, x, y, w, h, nsamples, gammaCorrected);
   }
   
   public void configure(GL gl, int x, int y, int w, int h, int nsamples, boolean gammaCorrected) {
      if (nsamples <= 0) {
         nsamples = this.numMultiSamples;
      }
      if (x != this.x || y != this.y || w != this.width || h != this.height
         || nsamples != this.numMultiSamples || gammaCorrected != this.gammaCorrected) {
         
         // clean up old buffer, create new
         set(x,y,w,h,nsamples, gammaCorrected);
         configureStorage(gl, x, y, w, h, nsamples, gammaCorrected);
      }
   }
   
   private void configureStorage(GL gl, int x, int y, int w, int h, int nsamples, boolean gammaCorrected) {
      if (depthBuffer != null) {
         depthBuffer.configure (gl, w, h, GL2GL3.GL_DEPTH_COMPONENT, nsamples);
      }
      int colorFormat = GL.GL_RGBA8;
      if (gammaCorrected) {
         colorFormat = GL.GL_SRGB8_ALPHA8;
      }
      
      if (colorBuffer != null) {
         colorBuffer.configure (gl, w, h, colorFormat, nsamples);
      }
      if (outputColorBuffer != null) {
         outputColorBuffer.configure (gl, w, h, colorFormat, 1);
      }
   }
   

   /**
    * Prepare framebuffer for use. Width and height should be set to rational
    * values before calling this function. Creates framebuffer with depth buffer
    * and one texture.
    */
   private void init (GL gl) {
      dispose(gl);  // maybe clean FBO
      
      int[] buff = new int[1];
      // create the single-sample frame buffer object (for final image)
      gl.glGenFramebuffers(1, buff, 0);
      FBOhandle = buff[0];

      // generate multisampled FBO
      if (numMultiSamples > 1) {
         gl.glGenFramebuffers(1, buff, 0);
         FBNhandle = buff[0];
      }

      addDepthBuffer (gl);
      addRgbBuffer (gl);
      
      // CBhandle = makeTexture();
      // attachTexture(CBhandle);

      checkStatus (gl);
      initialized = true;
   }

   /**
    *  Return the error code from the FBO
    */
   public int checkStatus(GL gl) {
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBOhandle);
      status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
      if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
         System.out.println("Framebuffer status is " + status + 
            " = " + getFramebufferStatus(status));
      }
      return status;
   }

   /**
    * Return a string representing the given fbo status code
    */
   public static String getFramebufferStatus(int statcode)
   {
      switch(statcode) {
         case GL.GL_FRAMEBUFFER_COMPLETE:
            return ("complete!");
         case GL.GL_FRAMEBUFFER_UNSUPPORTED:
            return ("GL_FRAMEBUFFER_UNSUPPORTED");
         case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
            return ("INCOMPLETE_ATTACHMENT");
         case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
            return ("FRAMEBUFFER_MISSING_ATTACHMENT");
         case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
            return ("FRAMEBUFFER_DIMENSIONS");
         case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
            return ("INCOMPLETE_FORMATS");
         case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
            return ("INCOMPLETE_DRAW_BUFFER");
         case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
            return ("INCOMPLETE_READ_BUFFER");
         case GL.GL_FRAMEBUFFER_BINDING:
            return ("BINDING");
         default:
            return ("Unknown status code");
      }
   }

   // /**
   //  * Attach a texture to framebuffer. Texture must be same dimensions as
   //  * framebuffer. To render to different textures, create one FrameBufferObject
   //  * and attach/detach the textures to render to each. It's faster to swap
   //  * textures than to bind/unbind FrameBuffers.
   //  * 
   //  * @see makeTexture
   //  */
   // private void attachTexture (int textureId) {
   //    // attach the texture to FBO color attachement point
   //    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBOhandle);
   //    gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER, 
   //       GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, textureId, 0);
   //    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, ActiveFBO);
   //    CBhandle = textureId;
   // }

   // /**
   //  * Create an empty texture with the set width and height. The FBO will
   //  * provide the data space, so last arg to glTexImage2D() is null.
   //  */
   // private int makeTexture () {
   //    IntBuffer textureHandle = allocInts (1);
   //    gl.glGenTextures(1, textureHandle);
   //    int textureId = textureHandle.get (0);

   //    gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);

   //    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
   //    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
   //    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_GENERATE_MIPMAP, GL.GL_TRUE);

   //    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
   //    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);

   //    // make the texture
   //    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, 
   //       width, height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
   //    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
   //    return textureId;
   // }

   // ---------------------------------------------------------------------------
   // Render Buffer Object functions
   // ---------------------------------------------------------------------------

   /**
    * Create a renderBuffer configured as a depth buffer and attach it to the
    * FBO
    */
   private void addDepthBuffer (GL gl) {
      // IntBuffer numSamps = allocInts (1);
      // gl.glGetIntegerv(GL.GL_MAX_SAMPLES, numSamps);
      // System.out.println ("numSamps=" + numSamps.get(0));

      // make renderbuffer for depth
      // System.out.println("adding depth buffer");

      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBOhandle);
      depthBuffer = GLRenderBuffer.generate (gl);
      // attach the renderbuffer to depth attachment point
      depthBuffer.configure (gl, width, height, GL2GL3.GL_DEPTH_COMPONENT, numMultiSamples);
      gl.glFramebufferRenderbuffer (
         GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
         GL.GL_RENDERBUFFER, depthBuffer.getId ());
      depthBuffer.unbind (gl);
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
      
   }

   /**
    * Create a renderBuffer configured as an RGB buffer and attach it to the
    * FBO
    */
   private void addRgbBuffer (GL gl) {
      // IntBuffer numSamps = allocInts (1);
      // gl.glGetIntegerv(GL.GL_MAX_SAMPLES, numSamps);
      // System.out.println ("numSamps=" + numSamps.get(0));

      // make renderbuffer for depth
      // System.out.println ("adding rgb buffer");

      int colorFormat = GL.GL_RGBA8;
      if (gammaCorrected) {
         colorFormat = GL.GL_SRGB8_ALPHA8;
      }
      
      // bind color buffer to non-multisampled FBO
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBOhandle);
      colorBuffer = GLRenderBuffer.generate (gl);
      colorBuffer.configure (gl, width, height, colorFormat, numMultiSamples);
      gl.glFramebufferRenderbuffer (
         GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
         GL.GL_RENDERBUFFER, colorBuffer.getId ());
      colorBuffer.unbind (gl);

      // Create secondary FBO
      if (numMultiSamples > 1) {
         gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBNhandle);
         outputColorBuffer = GLRenderBuffer.generate (gl);
         outputColorBuffer.configure (gl, width, height, colorFormat, 1);
         gl.glFramebufferRenderbuffer (
            GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
            GL.GL_RENDERBUFFER, outputColorBuffer.getId ());
         outputColorBuffer.unbind (gl);
      }

      // detach
      gl.glBindFramebuffer (GL.GL_FRAMEBUFFER, 0);
   }

   // ---------------------------------------------------------------------------
   // Frame Buffer Object functions
   // ---------------------------------------------------------------------------

   int[] savedViewport = null;

   /**
    * Once activated() all further rendering will go to the framebuffer object.
    * This function sets the viewport to the width and height of the FBO. The
    * deactivate() function will return the viewport to it's previous setting
    * and turn off the FBO. To use:
    * 
    * <PRE>
    *      FBO.activate();
    *      .....   // draw something here
    *      FBO.deactivate();
    * </PRE>
    * 
    * @see #deactivate
    */
   public void activate (GL gl) {
      if (!initialized) {
         init(gl);
      }
      
      // Select the framebuffer for subsequent rendering operations
      gl.glBindFramebuffer (GL.GL_FRAMEBUFFER, FBOhandle);

      // Set viewport to match fbo dimensions
      savedViewport = new int[4];
      gl.glGetIntegerv(GL.GL_VIEWPORT, savedViewport, 0);
      gl.glViewport (x, y, width, height);

   }

   /**
    * Once deactivated all further rendering goes to the screen. The viewport is
    * returned to its previous setting (before activate() was called).
    * 
    * @see #activate
    */
   public void deactivate (GL gl) {
      // return viewport to previous state
      if (savedViewport != null) {
         gl.glViewport(savedViewport[0], savedViewport[1], 
            savedViewport[2], savedViewport[3]);
      }

      // Stop rendering to FBO
      gl.glBindFramebuffer (GL.GL_FRAMEBUFFER, 0);

   }

   /**
    * delete the framebufferobject and renderbufferobject
    */
   public void dispose (GL gl) {

      if (FBOhandle != -1) {
         int[] buff = new int[]{FBOhandle};
         gl.glDeleteFramebuffers (1, buff, 0);
         FBOhandle = -1;
      }
      if (depthBuffer != null) {
         depthBuffer.dispose (gl);
         depthBuffer = null;
      }
      if (colorBuffer != null) {
         colorBuffer.dispose (gl);
         colorBuffer = null;
      }

      // clean up multisample
      if (FBNhandle != -1) {
         int[] buff = new int[]{FBNhandle};
         gl.glDeleteFramebuffers (1, buff, 0);
         FBNhandle = -1;
      }
      if (outputColorBuffer != null) {
         outputColorBuffer.dispose (gl);
         outputColorBuffer = null;
      }
      initialized = false;

   }

   /**
    * Uses the current GL object to get the RGBA pixels as bytes
    * and converts them to ARGB pixels as integers.
    * 
    * The RGBA bytes from OpenGL must be converted to the ARGB integers 
    * that Java's ImageIO class is expecting. Also, the y-axis must be 
    * flipped because OpengGl has the origin at the bottom left of a canvas
    * and the Java AWT at the top left.
    * 
    * @param gl      the current GL object.
    * @return  The ARGB pixels as integers.
    */
   public int[] getPixelsARGB (GL gl) {

      // Get the canvas RGB pixels as bytes and set up counters.
      ByteBuffer pixelsBGRA = getPixelsBGRA (gl);
      int byteRow = width * height * 4;
      int currentByte = byteRow;
      int byteRowWidth = width * 4;

      int[] pixelsARGB = new int[width * height];

      // grab back-color
      float[] bkColor = new float[4];
      gl.glGetFloatv(GL.GL_COLOR_CLEAR_VALUE, bkColor,0);
      float bkAlpha = bkColor[3];

      // Convert RGBA bytes to ARGB integers.
      for (int row = 0, currentInt = 0; row < height; ++row) {
         byteRow -= byteRowWidth;
         currentByte = byteRow;

         for (int column = 0; column < width; ++column) {
            int blue = pixelsBGRA.get (currentByte++);
            int green = pixelsBGRA.get (currentByte++);
            int red = pixelsBGRA.get (currentByte++);
            int alpha = pixelsBGRA.get (currentByte++);


            // Set alpha to be completely opaque or completely invisible.
            //(alpha != 0) ? 0xff000000 : 0x00000000;

            float fAlpha = alpha/255f;

            // adjust to account for background alpha
            // if either foreground or background has alpha 1, then dest will have value 1
            fAlpha = 1-(1-fAlpha)*(1-bkAlpha);

            alpha = (int)(fAlpha*255);

            pixelsARGB[currentInt++] = (alpha << 24)
            | ((red & 0x000000ff) << 16)
            | ((green & 0x000000ff) << 8)
            | (blue & 0x000000ff);
         }
      }

      BufferUtilities.freeDirectBuffer (pixelsBGRA);
      
      return pixelsARGB;
   }

   /**
    * Uses the current GL object to get the RGBA pixels from the canvas.
    *
    * @param gl      the current GL object.
    * @param width   the width of the canvas.
    * @param height  the height of the canvas.
    * @return  The RGBA pixels as bytes.
    */
   private ByteBuffer getPixelsBGRA (GL gl) {
      return getPixels(gl, GL.GL_BGRA);
   }
   
   public ByteBuffer getPixels(GL gl, int format) {
      int size = width * height * 4; // 4 bytes per RGBA pixel
      ByteBuffer pixels = BufferUtilities.newNativeByteBuffer(size);

      if (numMultiSamples > 1 && gl.isGL2GL3 ()) {
         GL2GL3 gl23 = (GL2GL3)gl;
         // System.out.println ("blitting");
         // read from multisample, write to single sample
         gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, FBOhandle);
         gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, FBNhandle);
         // Blit the multisampled FBO to the normal FBO
         gl23.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 
            GL.GL_COLOR_BUFFER_BIT, GL.GL_LINEAR);
         // Bind the normal FBO for reading
         gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, FBNhandle);
      } else {
         gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, FBOhandle);
      }
      

      gl.glReadPixels (0,
         0,
         width,
         height,
         format, 
         GL.GL_UNSIGNED_BYTE,
         pixels);
      
      // GLSupport.showImage(pixels, width, height);
      
      // rebind as draw
      gl.glBindFramebuffer (GL2GL3.GL_DRAW_FRAMEBUFFER, FBOhandle);

      return pixels;
   }

   public int getWidth() {
      return width;
    }
   
   public int getHeight() {
     return height;
   }

}
