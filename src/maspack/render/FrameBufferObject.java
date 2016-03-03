/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.jogamp.common.nio.Buffers;


public class FrameBufferObject {

   public static int defaultMultiSamples = 8;

   public int FBOhandle = -1; // Main FrameBuffer Object
   public int DBhandle = -1;  // Depth RenderBuffer Object
   public int CBhandle = -1;  // Color buffer handle

   // intermediate fbo for blitting (no depth required)
   public int FBNhandle = -1;
   public int CBNhandle = -1;

   public int x;
   public int y;
   public int width; // dimensions of framebuffer
   public int height;
   public int nMultiSamples;
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
      this.nMultiSamples = nsamples;
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

   //   public void reconfigure(int w, int h) {
   //      reconfigure(x, y, w, h, -1);
   //   }
   //   
   //   public void reconfigure(int w, int h, int nsamples) {
   //      reconfigure(x, y, w, h, nsamples);
   //   }
   //   
   //   public void reconfigure(int x, int y, int w, int h) {
   //      reconfigure(x, y, w, h, -1);
   //   }
   
   public void reconfigure(int w, int h, int nsamples, boolean gammaCorrected) {
      reconfigure(x, y, w, h, nsamples, gammaCorrected);
   }
   
   public void reconfigure(int x, int y, int w, int h, int nsamples, boolean gammaCorrected) {
      if (nsamples <= 0) {
         nsamples = this.nMultiSamples;
      }
      if (x != this.x || y != this.y || w != this.width || h != this.height
         || nsamples != this.nMultiSamples || gammaCorrected != this.gammaCorrected) {
         
         // clean up old buffer, create new
         set(x,y,w,h,nsamples, gammaCorrected);
         initialized = false;
      }
   }
   
   

   /**
    * Prepare framebuffer for use. Width and height should be set to rational
    * values before calling this function. Creates framebuffer with depth buffer
    * and one texture.
    */
   private void init (GL2GL3 gl) {
      dispose(gl);  // maybe clean FBO
      
      int[] buff = new int[1];
      // create the single-sample frame buffer object (for final image)
      gl.glGenFramebuffers(1, buff, 0);
      FBOhandle = buff[0];

      // generate multisampled FBO
      if (nMultiSamples > 1) {
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
   private void addDepthBuffer (GL2GL3 gl) {
      // IntBuffer numSamps = allocInts (1);
      // gl.glGetIntegerv(GL.GL_MAX_SAMPLES, numSamps);
      // System.out.println ("numSamps=" + numSamps.get(0));

      // make renderbuffer for depth
      // System.out.println("adding depth buffer");

      int[] buff = new int[1];
      gl.glGenRenderbuffers (1, buff, 0);
      DBhandle = buff[0];

      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBOhandle);
      gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, DBhandle);
      
      // either bind depth buffer to the single FBO or to the multisampled
      if (nMultiSamples > 1) {
         gl.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, nMultiSamples, 
            GL2GL3.GL_DEPTH_COMPONENT, width, height);
      } else {
         gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, 
            GL2GL3.GL_DEPTH_COMPONENT, width, height);
      }

      // attach the renderbuffer to depth attachment point
      gl.glFramebufferRenderbuffer (
         GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
         GL.GL_RENDERBUFFER, DBhandle);

      gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, 0); // detach for safety
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
      
   }

   /**
    * Create a renderBuffer configured as an RGB buffer and attach it to the
    * FBO
    */
   private void addRgbBuffer (GL2GL3 gl) {
      // IntBuffer numSamps = allocInts (1);
      // gl.glGetIntegerv(GL.GL_MAX_SAMPLES, numSamps);
      // System.out.println ("numSamps=" + numSamps.get(0));

      // make renderbuffer for depth
      // System.out.println ("adding rgb buffer");

      int[] buff = new int[1];
      gl.glGenRenderbuffers (1, buff, 0);
      CBhandle = buff[0];

      // bind color buffer to non-multisampled FBO
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBOhandle);
      gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, CBhandle);
      if (nMultiSamples > 1) {
         if (gammaCorrected) {
            gl.glRenderbufferStorageMultisample (
               GL.GL_RENDERBUFFER, nMultiSamples, GL.GL_RGBA8, width, height);   
         } else {
            gl.glRenderbufferStorageMultisample (
               GL.GL_RENDERBUFFER, nMultiSamples, GL.GL_SRGB8_ALPHA8, width, height);  
         }
      } else {
         if (gammaCorrected) {
            gl.glRenderbufferStorage (
               GL.GL_RENDERBUFFER, GL.GL_SRGB8_ALPHA8, width, height);  
         } else {
            gl.glRenderbufferStorage (
               GL.GL_RENDERBUFFER, GL.GL_RGBA8, width, height);
         }
      }
      gl.glFramebufferRenderbuffer (
         GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
         GL.GL_RENDERBUFFER, CBhandle);
      gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, 0); // detach for safety

      // Create secondary FBO
      if (nMultiSamples > 1) {
         gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBNhandle);
         gl.glGenRenderbuffers (1, buff, 0);
         CBNhandle = buff[0];
         gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, CBNhandle);
         if (gammaCorrected) {
            gl.glRenderbufferStorage (
               GL.GL_RENDERBUFFER, GL.GL_SRGB8_ALPHA8, width, height);   
         } else {
            gl.glRenderbufferStorage (
               GL.GL_RENDERBUFFER, GL.GL_RGBA8, width, height);   
         }
         
         gl.glFramebufferRenderbuffer (
            GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
            GL.GL_RENDERBUFFER, CBNhandle);
         gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, 0); // detach for safety
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
   public void activate (GL2GL3 gl) {
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
      if (DBhandle != -1) {
         int[] buff = new int[]{DBhandle};
         gl.glDeleteRenderbuffers (1, buff, 0);
         DBhandle = -1;
      }
      if (CBhandle != -1) {
         int[] buff = new int[]{CBhandle};
         gl.glDeleteRenderbuffers (1, buff, 0);
         CBhandle = -1;
      }

      // clean up multisample
      if (FBNhandle != -1) {
         int[] buff = new int[]{FBNhandle};
         gl.glDeleteFramebuffers (1, buff, 0);
         FBNhandle = -1;
      }
      if (CBNhandle != -1) {
         int[] buff = new int[]{CBNhandle};
         gl.glDeleteRenderbuffers (1, buff, 0);
         CBNhandle = -1;
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
   public int[] getPixelsARGB (GL2GL3 gl) {

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
   private ByteBuffer getPixelsBGRA (GL2GL3 gl) {
      return getPixels(gl, GL.GL_BGRA);
   }
   
   public ByteBuffer getPixels(GL2GL3 gl, int format) {
      int size = width * height * 4; // 4 bytes per RGBA pixel
      ByteBuffer pixels = Buffers.newDirectByteBuffer(size);

      if (nMultiSamples > 1) {
         // System.out.println ("blitting");
         // read from multisample, write to single sample
         gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, FBOhandle);
         gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, FBNhandle);
         // Blit the multisampled FBO to the normal FBO
         gl.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 
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

      // detach
      gl.glBindFramebuffer (GL.GL_FRAMEBUFFER, 0);

      return pixels;
   }

   public int getWidth() {
      return width;
    }
   
   public int getHeight() {
     return height;
   }

}
