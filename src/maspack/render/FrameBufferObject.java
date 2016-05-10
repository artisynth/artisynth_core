/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import com.jogamp.common.nio.Buffers;


public class FrameBufferObject {
   
   // This static var keeps track of which FBO has been activated
   public static int ActiveFBO = 0;
   public static int defaultSamples = 8;
   
   public int FBOhandle = -1; // FrameBuffer Object
   public int DBhandle = -1; // RenderBuffer Object (provides depth buffer)
   public int CBhandle = -1; // Texture handle

   public int FBNhandle = -1; // Multisample FrameBuffer Object
   public int CBNhandle = -1; // Multisample RGB buffer handle

   public int x;
   public int y;
   public int width; // dimensions of framebuffer
   public int height;
   public final static int SIZE_INT = 4;
   public GL2 gl;

   public File file;
   public String format;

   // status returned by OpenGL after FBO init
   // if this isn't == GL_FRAMEBUFFER_COMPLETE then init() had a problem
   int status = 0;
   public boolean setup = false;
   public int samples = defaultSamples;
   
   /**
    * Create a framebuffer with the given dimensions
    */
   public FrameBufferObject (int w, int h, File file, String format, GL2 gl) {
      this(0, 0, w, h, defaultSamples, file, format, gl);
   }
   
   /**
    * Create a framebuffer with the given dimensions
    */
   public FrameBufferObject (int x, int y, int w, int h, int nsamples, File file, String format, GL2 gl) {
      width = w;
      height = h;
      this.x = x;
      this.y = y;
      this.gl = gl;
      this.file = file;
      this.format = format;
      this.samples = nsamples;
      this.setup = false;
   }

   /**
    * Prepare framebuffer for use. Width and height should be set to rational
    * values before calling this function. Creates framebuffer with depth buffer
    * and one texture.
    */
   public void setupFBO () {
      
      IntBuffer handle = allocInts(1);
      // create the frame buffer object
      gl.glGenFramebuffers(1, handle);
      FBOhandle = handle.get(0);

      if (samples > 1) {
         gl.glGenFramebuffers(1, handle);
         FBNhandle = handle.get(0);
      }
      

      addDepthBuffer ();
      addRgbBuffer ();
      // CBhandle = makeTexture();
      // attachTexture(CBhandle);
      
      checkStatus ();
      setup = true;
   }
   
   /**
    *  Return the error code from the FBO
    */
   public int checkStatus() {
      gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, FBOhandle);
      status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
      gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, ActiveFBO);
      if (status != GL2.GL_FRAMEBUFFER_COMPLETE) {
         System.err.println("Framebuffer status is " + status + 
            " = " + framebuffer_status_string(status));
      }
      return status;
   }

   /**
    * Return a string representing the given fbo status code
    */
   public static String framebuffer_status_string(int statcode)
   {
      switch(statcode) {
         case GL2.GL_FRAMEBUFFER_COMPLETE:
            return ("complete!");
         case GL2.GL_FRAMEBUFFER_UNSUPPORTED:
            return ("GL_FRAMEBUFFER_UNSUPPORTED");
         case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
            return ("INCOMPLETE_ATTACHMENT");
         case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
            return ("FRAMEBUFFER_MISSING_ATTACHMENT");
         case GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
            return ("FRAMEBUFFER_DIMENSIONS");
         case GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
            return ("INCOMPLETE_FORMATS");
         case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
            return ("INCOMPLETE_DRAW_BUFFER");
         case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
            return ("INCOMPLETE_READ_BUFFER");
         case GL2.GL_FRAMEBUFFER_BINDING:
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
   //    gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, FBOhandle);
   //    gl.glFramebufferTexture2DEXT(GL2.GL_FRAMEBUFFER, 
   //       GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, textureId, 0);
   //    gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, ActiveFBO);
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
      
   //    gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);

   //    gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
   //    gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
   //    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_GENERATE_MIPMAP, GL2.GL_TRUE);

   //    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
   //    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);

   //    // make the texture
   //    gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8, 
   //       width, height, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, null);
   //    gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
   //    return textureId;
   // }

   private IntBuffer allocInts(int howmany) {
      return ByteBuffer.allocateDirect(howmany * SIZE_INT).order(
         ByteOrder.nativeOrder()).asIntBuffer();
  }

   // ---------------------------------------------------------------------------
   // Render Buffer Object functions
   // ---------------------------------------------------------------------------

   /**
    * Create a renderBuffer configured as a depth buffer and attach it to the
    * FBO
    */
   private void addDepthBuffer () {
      // IntBuffer numSamps = allocInts (1);
      // gl.glGetIntegerv(GL2.GL_MAX_SAMPLES, numSamps);
      // System.out.println ("numSamps=" + numSamps.get(0));

      // make renderbuffer for depth
      // System.out.println("adding depth buffer");

      IntBuffer rboId = allocInts (1);
      gl.glGenRenderbuffers (1, rboId);
      DBhandle = rboId.get (0);
      gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, FBOhandle);
      gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, DBhandle);
      
      if (samples > 1) {
         gl.glRenderbufferStorageMultisample(GL2.GL_RENDERBUFFER, samples, GL2.GL_DEPTH_COMPONENT, width, height);
      } else {
         gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, width, height);
      }
      
      // gl.glRenderbufferStorageMultisampleEXT (
      //    GL2.GL_RENDERBUFFER, /*samps=*/8,
      //    GL2.GL_DEPTH_COMPONENT, width, height);

      // attach the renderbuffer to depth attachment point
      gl.glFramebufferRenderbuffer (
         GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
         GL2.GL_RENDERBUFFER, DBhandle);
      
      // XXX Why detach?
      gl.glBindRenderbuffer (GL2.GL_RENDERBUFFER, 0);
      gl.glBindFramebuffer (GL2.GL_FRAMEBUFFER, ActiveFBO);
   }

   /**
    * Create a renderBuffer configured as an RGB buffer and attach it to the
    * FBO
    */
   private void addRgbBuffer () {
      // IntBuffer numSamps = allocInts (1);
      // gl.glGetIntegerv(GL2.GL_MAX_SAMPLES, numSamps);
      // System.out.println ("numSamps=" + numSamps.get(0));

      // make renderbuffer for depth
      // System.out.println ("adding rgb buffer");

      IntBuffer rboId = allocInts (1);
      gl.glGenRenderbuffers (1, rboId);
      CBhandle = rboId.get (0);
      
      gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, FBOhandle);
      gl.glBindRenderbuffer (GL2.GL_RENDERBUFFER, CBhandle);
      
      if (samples > 1) {
         gl.glGenRenderbuffers (1, rboId);
         CBNhandle = rboId.get (0);
         gl.glRenderbufferStorageMultisample (
             GL2.GL_RENDERBUFFER, samples, GL2.GL_RGBA8, width, height);
      }
      else {
          gl.glRenderbufferStorage (
             GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, width, height);
      }

      // attach the renderbuffer to depth attachment point
      gl.glFramebufferRenderbuffer (
         GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
         GL2.GL_RENDERBUFFER, CBhandle);
      
      // XXX Why disconnect?
      gl.glBindRenderbuffer (GL2.GL_RENDERBUFFER, 0);
      gl.glBindFramebuffer (GL2.GL_FRAMEBUFFER, ActiveFBO);
   }

   // ---------------------------------------------------------------------------
   // Frame Buffer Object functions
   // ---------------------------------------------------------------------------

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
   public void activate () {
      // Select the framebuffer for subsequent rendering operations
      gl.glBindFramebuffer (GL2.GL_FRAMEBUFFER, FBOhandle);

      // Set viewport to match fbo dimensions
      gl.glPushAttrib (GL2.GL_VIEWPORT_BIT);
      gl.glViewport (x, y, width, height);

      // keep track of the FBO that is active
      FrameBufferObject.ActiveFBO = FBOhandle;
   }

   /**
    * Once deactivated all further rendering goes to the screen. The viewport is
    * returned to its previous setting (before activate() was called).
    * 
    * @see #activate
    */
   public void deactivate () {
      // return viewport to previous state
      gl.glPopAttrib ();

      // Stop rendering to FBO
      gl.glBindFramebuffer (GL2.GL_FRAMEBUFFER, 0);

      // 0 means no FBO is active
      FrameBufferObject.ActiveFBO = 0;
   }

   /**
    * delete the framebufferobject and renderbufferobject
    */
   public void cleanup () {
      // XXX huge hack here, every delete buffer seems to fail
      if (FBOhandle != -1) {
         IntBuffer fboHandleBuff = allocInts (1).put (FBOhandle);
         try {
            gl.glDeleteFramebuffers (1, fboHandleBuff);
         } catch (Exception e) {
            System.out.println("Cannot delete FBOhandle");
         }
         FBOhandle = -1;
      }
      if (DBhandle != -1) {
         IntBuffer dbHandleBuff = allocInts (1).put (DBhandle);
         try {
            gl.glDeleteRenderbuffers (1, dbHandleBuff);
         } catch (Exception e) {
            System.out.println("Cannot delete DBhandle");
         }
         DBhandle = -1;
      }
      if (CBhandle != -1) {
         IntBuffer cbHandleBuff = allocInts (1).put (CBhandle);
         try {
            gl.glDeleteRenderbuffers (1, cbHandleBuff);
         } catch (Exception e) {
            System.out.println("Cannot delete CBhandle");
         }
         CBhandle = -1;
      }

      if (FBNhandle != -1) {
         IntBuffer fbnHandleBuff = allocInts (1).put (FBNhandle);
         try {
            gl.glDeleteFramebuffers (1, fbnHandleBuff);
         } catch (Exception e) {
            System.out.println("Cannot delete FBNhandle");
         }
         FBNhandle = -1;
      }
       if (CBNhandle != -1) {
         IntBuffer cbnHandleBuff = allocInts (1).put (CBNhandle);
         try {
            gl.glDeleteRenderbuffers (1, cbnHandleBuff);
         } catch (Exception e) {
            System.out.println("Cannot delete CBNhandle");
         }
         CBNhandle = -1;
      }
      setup = false;
      
   }

   /**
    * Captures an image of the canvas and saves it to the specified file.
    */
   public void capture () {
      gl.glBindFramebuffer (GL2.GL_FRAMEBUFFER, FBOhandle);
      
      BufferedImage image = null;

      // Get the ARGB pixels as integers.
      int[] pixelsARGB = getPixelsARGB ();
      // JPG for some reason breaks if we turn on the alpha layer.
      // JPG doesn't support alpha anyways.
      if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
         image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
      } else {
         image = new BufferedImage (width, height, BufferedImage.TYPE_INT_ARGB);
      }
      
      image.setRGB (0, 0,
                    width, height,
                    pixelsARGB,
                    0, width);
      
      try {
         ImageIO.write (image, format, file);
      }
      catch (IOException io_e) {
         io_e.printStackTrace();
      }
      
      gl.glBindFramebuffer (GL2.GL_FRAMEBUFFER, ActiveFBO);
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
    * @param width   the width of the canvas.
    * @param height  the height of the canvas.
    * @return  The ARGB pixels as integers.
    */
   private int[] getPixelsARGB () {
      // Get the canvas RGB pixels as bytes and set up counters.
      ByteBuffer pixelsBGRA = getPixelsBGRA ();
      int byteRow = width * height * 4;
      int currentByte = byteRow;
      int byteRowWidth = width * 4;

      int[] pixelsARGB = new int[width * height];
      
      // grab back-color
      float[] bkColor = new float[4];
      gl.glGetFloatv(GL2.GL_COLOR_CLEAR_VALUE, bkColor,0);
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
            
            // compute final dest alpha
            float fAlpha = 1-(1-alpha/255f)*(1-bkAlpha);
            alpha = (int)(fAlpha*bkAlpha*255);
            
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
   private ByteBuffer getPixelsBGRA () {
      int size = width * height * 4; // 4 bytes per RGBA pixel
      ByteBuffer pixelsBGRA = Buffers.newDirectByteBuffer(size);
      
      // gl.glReadBuffer (GL2.GL_FRONT); // XXX shouldn't need this, and regardless should use GL_BACK

      if (samples > 1) {
         System.out.println ("blitting");
         gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, FBNhandle);
         gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, CBNhandle);
         gl.glRenderbufferStorage (
            GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, width, height);
         gl.glFramebufferRenderbuffer (
            GL2.GL_FRAMEBUFFER,
            GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, CBNhandle);
         // Blit the multisampled FBO to the normal FBO
         gl.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 
            GL2.GL_COLOR_BUFFER_BIT, GL2.GL_LINEAR);
         
         // Bind the normal FBO for reading
         gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, FBNhandle);
       } 

      gl.glReadPixels (0,
                       0,
                       width,
                       height,
                       GL2.GL_BGRA, 
                       GL2.GL_UNSIGNED_BYTE,
                       pixelsBGRA);
      return pixelsBGRA;
   }
   
   public void getPixelsRGBA(ByteBuffer pixels) {
      gl.glReadPixels(0, 0, width, height,
         GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, pixels);
   }
   
}
