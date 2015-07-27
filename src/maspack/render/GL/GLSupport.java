/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import javax.media.opengl.GL;

import jogamp.opengl.glu.error.Error;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.DenseMatrix;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Vector3d;

public class GLSupport {
   public static void transformToGLMatrix (double[] mat, Matrix T) {
      mat[0] = T.get (0, 0);
      mat[1] = T.get (1, 0);
      mat[2] = T.get (2, 0);
      mat[3] = T.get (3, 0);

      mat[4] = T.get (0, 1);
      mat[5] = T.get (1, 1);
      mat[6] = T.get (2, 1);
      mat[7] = T.get (3, 1);

      mat[8] = T.get (0, 2);
      mat[9] = T.get (1, 2);
      mat[10] = T.get (2, 2);
      mat[11] = T.get (3, 2);

      mat[12] = T.get (0, 3);
      mat[13] = T.get (1, 3);
      mat[14] = T.get (2, 3);
      mat[15] = T.get (3, 3);
   }

   public static void GLMatrixToTransform (DenseMatrix T, double[] mat) {
      T.set (0, 0, mat[0]);
      T.set (1, 0, mat[1]);
      T.set (2, 0, mat[2]);
      T.set (3, 0, mat[3]);

      T.set (0, 1, mat[4]);
      T.set (1, 1, mat[5]);
      T.set (2, 1, mat[6]);
      T.set (3, 1, mat[7]);

      T.set (0, 2, mat[8]);
      T.set (1, 2, mat[9]);
      T.set (2, 2, mat[10]);
      T.set (3, 2, mat[11]);

      T.set (0, 3, mat[12]);
      T.set (1, 3, mat[13]);
      T.set (2, 3, mat[14]);
      T.set (3, 3, mat[15]);
   }

   public static void transformToGLMatrix (double[] mat, AffineTransform3d T) {
      mat[0] = T.A.m00;
      mat[1] = T.A.m10;
      mat[2] = T.A.m20;
      mat[3] = 0;

      mat[4] = T.A.m01;
      mat[5] = T.A.m11;
      mat[6] = T.A.m21;
      mat[7] = 0;

      mat[8] = T.A.m02;
      mat[9] = T.A.m12;
      mat[10] = T.A.m22;
      mat[11] = 0;

      mat[12] = T.p.x;
      mat[13] = T.p.y;
      mat[14] = T.p.z;
      mat[15] = 1;
   }
   
   public static void transformToGLMatrix (double[] mat, int offset, AffineTransform3dBase T) {
      Matrix3dBase M = T.getMatrix();
      Vector3d b = T.getOffset();
      int idx = offset;
      mat[idx++] = M.m00;
      mat[idx++] = M.m10;
      mat[idx++] = M.m20;
      mat[idx++] = 0;

      mat[idx++] = M.m01;
      mat[idx++] = M.m11;
      mat[idx++] = M.m21;
      mat[idx++] = 0;

      mat[idx++] = M.m02;
      mat[idx++] = M.m12;
      mat[idx++] = M.m22;
      mat[idx++] = 0;

      mat[idx++] = b.x;
      mat[idx++] = b.y;
      mat[idx++] = b.z;
      mat[idx++] = 1;
   }
   
   public static void transformToGLMatrix (float[] mat, int offset, AffineTransform3dBase T) {
      Matrix3dBase M = T.getMatrix();
      Vector3d b = T.getOffset();
      int idx = offset;
      mat[idx++] = (float)M.m00;
      mat[idx++] = (float)M.m10;
      mat[idx++] = (float)M.m20;
      mat[idx++] = 0f;

      mat[idx++] = (float)M.m01;
      mat[idx++] = (float)M.m11;
      mat[idx++] = (float)M.m21;
      mat[idx++] = 0f;

      mat[idx++] = (float)M.m02;
      mat[idx++] = (float)M.m12;
      mat[idx++] = (float)M.m22;
      mat[idx++] = 0f;

      mat[idx++] = (float)b.x;
      mat[idx++] = (float)b.y;
      mat[idx++] = (float)b.z;
      mat[idx++] = 1f;
   }
   
   public static void transformToGLMatrixTranspose (float[] mat, int offset, AffineTransform3dBase T) {
      Matrix3dBase M = T.getMatrix();
      Vector3d b = T.getOffset();
      int idx = offset;
      mat[idx++] = (float)M.m00;
      mat[idx++] = (float)M.m01;
      mat[idx++] = (float)M.m02;
      mat[idx++] = (float)b.x;

      mat[idx++] = (float)M.m10;
      mat[idx++] = (float)M.m11;
      mat[idx++] = (float)M.m12;
      mat[idx++] = (float)b.y;

      mat[idx++] = (float)M.m20;
      mat[idx++] = (float)M.m21;
      mat[idx++] = (float)M.m22;
      mat[idx++] = (float)b.z;

      mat[idx++] = 0f;
      mat[idx++] = 0f;
      mat[idx++] = 0f;
      mat[idx++] = 1f;
   }
   
   public static void transformToGLMatrixTranspose (double[] mat, int offset, AffineTransform3dBase T) {
      Matrix3dBase M = T.getMatrix();
      Vector3d b = T.getOffset();
      int idx = offset;
      mat[idx++] = M.m00;
      mat[idx++] = M.m01;
      mat[idx++] = M.m02;
      mat[idx++] = b.x;

      mat[idx++] = M.m10;
      mat[idx++] = M.m11;
      mat[idx++] = M.m12;
      mat[idx++] = b.y;

      mat[idx++] = M.m20;
      mat[idx++] = M.m21;
      mat[idx++] = M.m22;
      mat[idx++] = b.z;

      mat[idx++] = 0.0;
      mat[idx++] = 0.0;
      mat[idx++] = 0.0;
      mat[idx++] = 1.0;
   }

   public static void GLMatrixToTransform (AffineTransform3d T, double[] mat) {
      T.A.m00 = mat[0];
      T.A.m10 = mat[1];
      T.A.m20 = mat[2];

      T.A.m01 = mat[4];
      T.A.m11 = mat[5];
      T.A.m21 = mat[6];

      T.A.m02 = mat[8];
      T.A.m12 = mat[9];
      T.A.m22 = mat[10];

      T.p.x = mat[12];
      T.p.y = mat[13];
      T.p.z = mat[14];
   }

   /** 
    * Convert an HSV color to RGB representation.
    */
   public static void HSVtoRGB (float[] hsv, float[] rgb) {
      float f, p, q, t, hRound;
      int hIndex;
      float h, s, v;

      h = hsv[0];
      s = hsv[1];
      v = hsv[2];
    
      if (h < 0) {
         h = 0;
      }
      else if (h >= 1) {
         h = 0;
      }
      hRound = (int)(h*6.0);
      hIndex = ((int)hRound)%6;
      f = (h*6.0f) - hRound;
      p = v*(1.0f - s);
      q = v*(1.0f - f*s);
      t = v*(1.0f - (1.0f - f)*s);

      switch(hIndex) {
         case 0:
            rgb[0] = v; rgb[1] = t; rgb[2] = p;
            break;
         case 1:
            rgb[0] = q; rgb[1] = v; rgb[2] = p;
            break;
         case 2:
            rgb[0] = p; rgb[1] = v; rgb[2] = t;
            break;
         case 3:
            rgb[0] = p; rgb[1] = q; rgb[2] = v;
            break;
         case 4:
            rgb[0] = t; rgb[1] = p; rgb[2] = v;
            break;
         case 5:
            rgb[0] = v; rgb[1] = p; rgb[2] = q;
            break;
      }
   }

   private static int RED = 0;
   private static int GREEN = 1;
   private static int BLUE = 2;
   private static float EPSILON = 1.0e-6f;

   /** 
    * Convert an RGB color to HSV representation. It is assumed that all RBG
    * and HSV values are in the range 0-1 (as opposed to the typical HSV ranges
    * of [0-360], [0-100], [0-100]).
    */
   public static void RGBtoHSV (float[] hsv, float[] rgb) {

      float r = rgb[0];
      float g = rgb[1];
      float b = rgb[2];

      float h, s, v, diff;

      int minIdx = 0;
      int maxIdx = 0;
      float maxCol = r;
      float minCol = r;
      if (g > maxCol) {
         maxCol = g;
         maxIdx = GREEN;
      }
      if (g < minCol) {
         minCol = g;
         minIdx = GREEN;
      }
      if (b > maxCol) {
         maxCol = b;
         maxIdx = BLUE;
      }
      if (b < minCol) {
         minCol = b;
         minIdx = BLUE;
      }

      diff = maxCol - minCol;
      /* H */
      h = 0.0f;
      if (diff >=  EPSILON) {
         if(maxIdx == RED){
            h = ((1.0f/6.0f) * ( (g - b) / diff )) + 1.0f;
            h = h - (int)h;
         }
         else if(maxIdx == GREEN){
            h = ((1.0f/6.0f) * ( (b - r) / diff )) + (1.0f/3.0f);
         }
         else if(maxIdx == BLUE){
            h = ((1.0f/6.0f) * ( (r - g) / diff )) + (2.0f/3.0f);        
         }
      }
      /* S */
      if(maxCol < EPSILON)
         s = 0;
      else
         s = (maxCol - minCol) / maxCol;

      /* V */
      v = maxCol;

      hsv[0] = h; hsv[1] = s; hsv[2] = v;
   }

   public static void interpolateColor (
      float[] result, float[] color0, float[] color1, double s) {
      
      float a = (float)(1-s);
      float b = (float)(s);

      result[0] = a*color0[0] + b*color1[0];
      result[1] = a*color0[1] + b*color1[1];
      result[2] = a*color0[2] + b*color1[2];
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
}
