package maspack.render.GL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2GL3;

import maspack.threads.SimpleThreadManager;

public class GLFrameCapture {

   SimpleThreadManager imageThreadManager;
   
   private File file;
   private String format;
   private FrameBufferObject fbo;
   
   public GLFrameCapture(
      int w, int h, int nsamples, boolean gammaCorrected, 
      File file, String format) {
      this(0,0,w,h, nsamples, gammaCorrected, file,format);
   }
  
   public GLFrameCapture(
      int x, int y, int w, int h, int nsamples, boolean gammaCorrected, 
      File file, String format) {
      this.file = file;
      this.format = format;
      fbo = new FrameBufferObject(x, y, w, h, nsamples, gammaCorrected);
   }
   
   public FrameBufferObject getFBO() {
      return fbo;
   }
   
   //   public void reconfigure(int w, int h, File file, String format) {
   //      this.file = file;
   //      this.format = format;
   //      fbo.reconfigure(w, h, -1);
   //   }
   
   //   public void reconfigure(int x, int y, int w, int h, File file, String format) {
   //      this.file = file;
   //      this.format = format;
   //      fbo.reconfigure(x, y, w, h, -1);
   //   }
   
   public void reconfigure(GL2GL3 gl, int w, int h, int nsamples, boolean gammaCorrection, File file, String format) {
      this.file = file;
      this.format = format;
      fbo.configure(gl, w, h, nsamples, gammaCorrection);
   }
   
   public void reconfigure(GL2GL3 gl, int x, int y, int w, int h, int nsamples, 
      boolean gammaCorrection, File file, String format) {
      this.file = file;
      this.format = format;
      fbo.configure(gl, x, y, w, h, nsamples, gammaCorrection);
   }
   
   public void activateFBO(GL2GL3 gl) {
      fbo.activate(gl);
   }
   
   public void deactivateFBO(GL2GL3 gl) {
      fbo.deactivate(gl);
   }
   
   private static class ImageWriterRunnable implements Runnable {
      
      private int width;
      private int height;
      private String format;
      private File file;
      int[] pixelsARGB;
      
      public ImageWriterRunnable(String format, File file, 
         int width, int height, int[] pixelsARGB) {
         this.width = width;
         this.height = height;
         this.file = file;
         this.format = format;
         this.pixelsARGB = pixelsARGB;
      }
      
      public void run() {
         // JPG for some reason breaks if we turn on the alpha layer.
         // JPG doesn't support alpha anyways.
         BufferedImage image = null;
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
      }
   }
   
   /**
    * Captures an image of the canvas and saves it to the specified file.
    */
   public void capture (GL2GL3 gl) {

      // Get the ARGB pixels as integers.
      int[] pixelsARGB = fbo.getPixelsARGB (gl);

      if (imageThreadManager == null) {
         imageThreadManager = new SimpleThreadManager("GLFrameCapture", 1, 5000);
      }
      
      // write image in separate thread
      synchronized (imageThreadManager) {
         imageThreadManager.execute(new ImageWriterRunnable(format, file, 
            fbo.getWidth(), fbo.getHeight(), pixelsARGB));  
      }
      
   }
   
   public void waitForCompletion() {
      synchronized (imageThreadManager) {
         while (imageThreadManager.hasNextFuture()) {
            Future<?> fut = imageThreadManager.popFuture();
            try {
               fut.get();
            } catch (InterruptedException | ExecutionException e) {
               e.printStackTrace();
            }
         }
      }
   }
   
   public void dispose(GL2GL3 gl) {
      fbo.dispose(gl);
   }
   
}
