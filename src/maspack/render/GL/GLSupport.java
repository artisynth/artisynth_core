/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Scanner;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import javax.swing.JFrame;
import javax.swing.JPanel;

import jogamp.opengl.glu.error.Error;
import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix2dBase;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.BufferUtilities;

public class GLSupport {

   // sizes of elements in bytes (to circumvent Java 7/8 problems)
   public static final int BYTE_SIZE = 1;
   public static final int SHORT_SIZE = 2;
   public static final int INTEGER_SIZE = 4;
   public static final int FLOAT_SIZE = 4;

   /**
    * Java 8 version of float hashcode
    * @return the computed hashcode
    */
   public static int hashCode(float f) {
      return Float.floatToIntBits (f);
   }

   /**
    * Java 8 version of double hashcode
    * @return the computed hashcode
    */
   // NOT USED
   public static int hashCode(double d) {
      long bits = Double.doubleToLongBits(d);
      return (int)(bits ^ (bits >>> 32));
   }

   /**
    * Converts a 2D affine transform to a 4D matrix expected by opengl
    */
   public static void transformToGLMatrix (double[] mat, AffineTransform2dBase T) {
      Matrix2dBase M = T.getMatrix ();
      Vector2d p = T.getOffset ();

      mat[0] = M.m00;
      mat[1] = M.m10;
      mat[2] = 0;
      mat[3] = 0;

      mat[4] = M.m01;
      mat[5] = M.m11;
      mat[6] = 01;
      mat[7] = 0;

      mat[8] = 0;
      mat[9] = 0;
      mat[10] = 0;
      mat[11] = 0;

      mat[12] = p.x;
      mat[13] = p.y;
      mat[14] = 0;
      mat[15] = 1;
   }

   public static void transformToGLMatrix (double[] mat, Matrix T) {
      int nr = T.rowSize ();
      int nc = T.colSize ();
      int idx = 0;
      for (int c=0; c<nc; ++c) {
         for (int r=0; r<nr; ++r) {
            mat[idx++] = T.get (r, c);
         }
      }
   }

   //   public static void GLMatrixToTransform (DenseMatrix T, double[] mat) {
   //      T.set (0, 0, mat[0]);
   //      T.set (1, 0, mat[1]);
   //      T.set (2, 0, mat[2]);
   //      T.set (3, 0, mat[3]);
   //
   //      T.set (0, 1, mat[4]);
   //      T.set (1, 1, mat[5]);
   //      T.set (2, 1, mat[6]);
   //      T.set (3, 1, mat[7]);
   //
   //      T.set (0, 2, mat[8]);
   //      T.set (1, 2, mat[9]);
   //      T.set (2, 2, mat[10]);
   //      T.set (3, 2, mat[11]);
   //
   //      T.set (0, 3, mat[12]);
   //      T.set (1, 3, mat[13]);
   //      T.set (2, 3, mat[14]);
   //      T.set (3, 3, mat[15]);
   //   }
   //
   //   public static void transformToGLMatrix (double[] mat, AffineTransform3d T) {
   //      mat[0] = T.A.m00;
   //      mat[1] = T.A.m10;
   //      mat[2] = T.A.m20;
   //      mat[3] = 0;
   //
   //      mat[4] = T.A.m01;
   //      mat[5] = T.A.m11;
   //      mat[6] = T.A.m21;
   //      mat[7] = 0;
   //
   //      mat[8] = T.A.m02;
   //      mat[9] = T.A.m12;
   //      mat[10] = T.A.m22;
   //      mat[11] = 0;
   //
   //      mat[12] = T.p.x;
   //      mat[13] = T.p.y;
   //      mat[14] = T.p.z;
   //      mat[15] = 1;
   //   }
   //   
   //   public static void transformToGLMatrix (double[] mat, int offset, AffineTransform3dBase T) {
   //      Matrix3dBase M = T.getMatrix();
   //      Vector3d b = T.getOffset();
   //      int idx = offset;
   //      mat[idx++] = M.m00;
   //      mat[idx++] = M.m10;
   //      mat[idx++] = M.m20;
   //      mat[idx++] = 0;
   //
   //      mat[idx++] = M.m01;
   //      mat[idx++] = M.m11;
   //      mat[idx++] = M.m21;
   //      mat[idx++] = 0;
   //
   //      mat[idx++] = M.m02;
   //      mat[idx++] = M.m12;
   //      mat[idx++] = M.m22;
   //      mat[idx++] = 0;
   //
   //      mat[idx++] = b.x;
   //      mat[idx++] = b.y;
   //      mat[idx++] = b.z;
   //      mat[idx++] = 1;
   //   }

   //   public static void transformToGLMatrix (float[] mat, int offset, AffineTransform3dBase T) {
   //      Matrix3dBase M = T.getMatrix();
   //      Vector3d b = T.getOffset();
   //      int idx = offset;
   //      mat[idx++] = (float)M.m00;
   //      mat[idx++] = (float)M.m10;
   //      mat[idx++] = (float)M.m20;
   //      mat[idx++] = 0f;
   //
   //      mat[idx++] = (float)M.m01;
   //      mat[idx++] = (float)M.m11;
   //      mat[idx++] = (float)M.m21;
   //      mat[idx++] = 0f;
   //
   //      mat[idx++] = (float)M.m02;
   //      mat[idx++] = (float)M.m12;
   //      mat[idx++] = (float)M.m22;
   //      mat[idx++] = 0f;
   //
   //      mat[idx++] = (float)b.x;
   //      mat[idx++] = (float)b.y;
   //      mat[idx++] = (float)b.z;
   //      mat[idx++] = 1f;
   //   }
   //   
   //   public static void transformToGLMatrixTranspose (float[] mat, int offset, AffineTransform3dBase T) {
   //      Matrix3dBase M = T.getMatrix();
   //      Vector3d b = T.getOffset();
   //      int idx = offset;
   //      mat[idx++] = (float)M.m00;
   //      mat[idx++] = (float)M.m01;
   //      mat[idx++] = (float)M.m02;
   //      mat[idx++] = (float)b.x;
   //
   //      mat[idx++] = (float)M.m10;
   //      mat[idx++] = (float)M.m11;
   //      mat[idx++] = (float)M.m12;
   //      mat[idx++] = (float)b.y;
   //
   //      mat[idx++] = (float)M.m20;
   //      mat[idx++] = (float)M.m21;
   //      mat[idx++] = (float)M.m22;
   //      mat[idx++] = (float)b.z;
   //
   //      mat[idx++] = 0f;
   //      mat[idx++] = 0f;
   //      mat[idx++] = 0f;
   //      mat[idx++] = 1f;
   //   }

   //   public static void transformToGLMatrixTranspose (double[] mat, int offset, AffineTransform3dBase T) {
   //      Matrix3dBase M = T.getMatrix();
   //      Vector3d b = T.getOffset();
   //      int idx = offset;
   //      mat[idx++] = M.m00;
   //      mat[idx++] = M.m01;
   //      mat[idx++] = M.m02;
   //      mat[idx++] = b.x;
   //
   //      mat[idx++] = M.m10;
   //      mat[idx++] = M.m11;
   //      mat[idx++] = M.m12;
   //      mat[idx++] = b.y;
   //
   //      mat[idx++] = M.m20;
   //      mat[idx++] = M.m21;
   //      mat[idx++] = M.m22;
   //      mat[idx++] = b.z;
   //
   //      mat[idx++] = 0.0;
   //      mat[idx++] = 0.0;
   //      mat[idx++] = 0.0;
   //      mat[idx++] = 1.0;
   //   }

   public static void GLMatrixToTransform (
      AffineTransform3dBase X, double[] mat) {
      Matrix3dBase M = X.getMatrix();
      Vector3d b = X.getOffset();
      M.m00 = mat[0];
      M.m10 = mat[1];
      M.m20 = mat[2];

      M.m01 = mat[4];
      M.m11 = mat[5];
      M.m21 = mat[6];

      M.m02 = mat[8];
      M.m12 = mat[9];
      M.m22 = mat[10];

      b.x = mat[12];
      b.y = mat[13];
      b.z = mat[14];
   }

   /**
    * Simple class to help debug storage
    */
   public static class ImagePanel extends JPanel {
      private static final long serialVersionUID = 1L;
      private BufferedImage image;

      public ImagePanel (BufferedImage image) {
         this.image = image;
         setOpaque (false);
         setLayout (null);
      }

      public void setImage(BufferedImage im) {
         image = im;
         repaint ();
      }

      @Override
      protected void paintComponent (Graphics g) {
         super.paintComponent (g);
         g.drawImage (image, 0, 0, null); // see javadoc for more info on the
         // parameters
      }
   }

   static JFrame textureFrame = null;
   static ImagePanel textureImage = null;

   public static void showTexture(GL2GL3 gl, int target, int level) {
      BufferedImage image = downloadTexture (gl, target, level);

      if (textureFrame == null) {
         textureFrame = new JFrame ("GLSupport texture");
         textureImage = new ImagePanel (image);
         textureFrame.getContentPane().setBackground (Color.BLACK);
         textureFrame.getContentPane ().add (textureImage);
         textureFrame.setSize (image.getWidth ()+30, image.getHeight ()+70);
         textureFrame.setVisible (true);
      } else {
         textureImage.setImage (image);
         textureFrame.setSize (image.getWidth ()+30, image.getHeight ()+70);
         if (!textureFrame.isVisible ()) {
            textureFrame.setVisible (true);
         }
      }

      textureFrame.repaint ();
   }
   
   static JFrame imageFrame = null;
   static ImagePanel imageImage = null;

   public static void showImage(ByteBuffer buff, int width, int height) {
      showImage(getImageRGBA (buff, width, height));
   }
   
   public static void showImage(BufferedImage image) {

      if (imageFrame == null) {
         imageFrame = new JFrame ("GLSupport image");
         imageImage = new ImagePanel (image);
         imageFrame.getContentPane().setBackground (Color.BLACK);
         imageFrame.getContentPane ().add (imageImage);
         imageFrame.setSize (image.getWidth ()+30, image.getHeight ()+70);
         imageFrame.setVisible (true);
      } else {
         imageImage.setImage (image);
         imageFrame.setSize (image.getWidth ()+30, image.getHeight ()+70);
         if (!imageFrame.isVisible ()) {
            imageFrame.setVisible (true);
         }
      }

      imageFrame.repaint ();
   }

   public static BufferedImage downloadTexture(GL2GL3 gl, int target) {
      return downloadTexture(gl, target, 0);
   }

   public static BufferedImage getImageRGBA(ByteBuffer buff, int width, int height) {
      final ComponentColorModel RGBA_COLOR =
      new ComponentColorModel (
         ColorSpace.getInstance (ColorSpace.CS_sRGB),
         new int[] { 8, 8, 8, 8 }, true, false,
         ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
      // sRGBA color model
      WritableRaster raster = Raster.createInterleavedRaster (
         DataBuffer.TYPE_BYTE, width, height, 4, null);
      BufferedImage image = new BufferedImage (RGBA_COLOR, raster, false, null);

      // flip vertically
      int scanline =4*width;
      int pos = width*height*4-scanline;
      for (int i=0; i<height; ++i) {
         for (int j=0; j<scanline; ++j) {
            byte bb = buff.get ();
            raster.getDataBuffer ().setElem (pos+j, bb);   
         }
         pos -= scanline;
      }
      return image;
   }

   public static BufferedImage downloadTexture(GL2GL3 gl, int target, int level) {
      int[] v = new int[2];
      gl.glGetTexLevelParameteriv (target, 0, GL2GL3.GL_TEXTURE_WIDTH, v, 0);
      gl.glGetTexLevelParameteriv (target, 0, GL2GL3.GL_TEXTURE_HEIGHT, v, 1);

      if (v[0]*v[1] == 0) {
         return null;
      }

      ByteBuffer buff = BufferUtilities.newNativeByteBuffer (v[0]*v[1]*4);
      gl.glGetTexImage (target, level, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buff);

      return getImageRGBA(buff, v[0], v[1]);

   }

   private static void printErr(String msg) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      String fullClassName = trace[3].getClassName();            
      String className = 
      fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
      String methodName = trace[3].getMethodName();
      int lineNumber = trace[3].getLineNumber();
      System.err.println (
         className + "." + methodName + "():" + lineNumber + ": " + msg);
   }

   public static boolean checkAndPrintGLError (GL gl) {
      int err = gl.glGetError();
      if (err != GL.GL_NO_ERROR) {
         String msg = Error.gluErrorString(err);
         printErr(msg + " (" +err + ")");
         return false;
      }
      return true;
   }
   
   public static void printGLError (int err) {
      String msg = Error.gluErrorString(err);
      printErr(msg + " (" +err + ")");
   }
   
   public static class GLVersionInfo {
      String renderer;
      int majorVersion;
      int minorVersion;
      String version;
      
      public GLVersionInfo(String renderer, String version, int major, int minor) {
         this.renderer = renderer;
         this.version = version;
         this.majorVersion = major;
         this.minorVersion = minor;
      }
      
      public String getVersionString() {
         return version;
      }
      
      public int getMajorVersion() {
         return majorVersion;
      }
      
      public int getMinorVersion() {
         return minorVersion;
      }
      
      public String getRenderer() {
         return renderer;
      }
   }
   
   
   public static class GLVersionListener implements GLEventListener {

      volatile boolean valid = false;
      
      GLVersionInfo vinfo = null;
      
      @Override
      public void init(GLAutoDrawable drawable) {
         GL gl = drawable.getGL();
         // XXX JOGL 2.4.0 on MacOS gives an invalid framebuffer operation error 1286
         // seems to be benign, so just clear it:
         int err = gl.glGetError();
         if (err != GL.GL_NO_ERROR && err != 1286) {
            printGLError (err);
         }
         
         String renderer = gl.glGetString(GL.GL_RENDERER);
         GLSupport.checkAndPrintGLError (gl);
         String version = gl.glGetString(GL.GL_VERSION);
         GLSupport.checkAndPrintGLError (gl);
         
         // XXX on older machines, must parse version string
         Scanner scanf = new Scanner (version.trim ().split (" ", 2)[0]);
         scanf.useDelimiter ("\\.");
         int major = scanf.nextInt ();
         int minor = scanf.nextInt ();
         scanf.close ();
         
         // int[] buff = new int[2];
         // gl.glGetIntegerv(GL3.GL_MAJOR_VERSION, buff, 0);
         // GLSupport.checkAndPrintGLError (gl);
         // gl.glGetIntegerv(GL3.GL_MINOR_VERSION, buff, 1);
         // GLSupport.checkAndPrintGLError (gl);
         // int major = buff[0];
         // int minor = buff[1];
         
         vinfo = new GLVersionInfo(renderer, version, major, minor);
         valid = true;
      }

      @Override
      public void dispose(GLAutoDrawable drawable) {
      }

      @Override
      public void display(GLAutoDrawable drawable) {}

      @Override
      public void reshape(
         GLAutoDrawable drawable, int x, int y, int width, int height) {}
      
      public GLVersionInfo getVersionInfo() {
         return vinfo;
      }
      
      public boolean isValid() {
         return valid;
      }
         
   }
   
   public static GLVersionInfo getMinGLVersionSupported() {
      // get minimum profile
      GLProfile glp = GLProfile.get (GLProfile.GL_PROFILE_LIST_MIN, true);
      return getVersionInfo(glp);
   }
   
   public static GLVersionInfo getMaxGLVersionSupported() {
      // get maximum profile
      GLProfile glp = GLProfile.get (GLProfile.GL_PROFILE_LIST_MAX, true);
      return getVersionInfo(glp);
   }
   
      
   public static GLVersionInfo getVersionInfo(GLProfile glp) {
      
      GLCapabilities glc = new GLCapabilities(glp);
      GLAutoDrawable dummy = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, glc, null);
      GLVersionListener listener = new GLVersionListener();
      dummy.addGLEventListener (listener);
      dummy.display(); // triggers GLContext object creation and native realization.

      while (!listener.isValid()) {
         Thread.yield(); // let other threads do stuff
      }
      GLVersionInfo vinfo = listener.getVersionInfo();
      dummy.disposeGLEventListener(listener, true);
      dummy.destroy(); // XXX should be auto-destroyed.  We have reports that manually calling destroy sometimes crashes the JVM.

      return vinfo;
   }
   
   public static void main (String[] args) {
      GLVersionInfo info = getMaxGLVersionSupported();
      System.out.println (info.getVersionString());      
   }
}
