package maspack.render.GL.GL3;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix4d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector3d;
import maspack.render.GL.GLSupport;

public class MatricesUBO extends UniformBufferObject {
   
   static final String[] PVM_ATTRIBUTES = { "pvm_matrix",    // model to screen
                                  "vm_matrix",     // model to viewer
                                  "m_matrix",      // model to world
                                  "normal_matrix"  // model to viewer normal
                                };
   static final int PVM_IDX = 0;
   static final int VM_IDX = 1;
   static final int M_IDX = 2;
   static final int NORMAL_IDX = 3;
   
   int[] foffsets;
   int fsize;
   
   static final String PVM_NAME = "Matrices";
   
   public MatricesUBO(GL3 gl, int progId) {
      super(gl, progId, PVM_NAME, PVM_ATTRIBUTES, GL3.GL_DYNAMIC_DRAW);
      foffsets = new int[offsets.length];
      for (int i=0; i<offsets.length; ++i) {
         foffsets[i] = offsets[i]/GLSupport.FLOAT_SIZE;
      }
      fsize = getSize()/GLSupport.FLOAT_SIZE;
   }
   
   public void updateMatrices(GL3 gl,  Matrix4d proj, RigidTransform3d view, AffineTransform3dBase model) {
      
      float[] matrixbuff = new float[fsize];
      
      // model matrix
      GLSupport.transformToGLMatrix(matrixbuff, foffsets[M_IDX], model);
      // model view
      mul(view, model, matrixbuff, foffsets[VM_IDX]);
      // projection-model-view
      mul(proj, matrixbuff, foffsets[VM_IDX], matrixbuff, foffsets[PVM_IDX]);
      
      // normal
      if (model instanceof RigidTransform3d) {
         copy(matrixbuff, foffsets[VM_IDX], matrixbuff, foffsets[NORMAL_IDX], 16);
      } else {
         
         // invert model matrix
         Matrix3d A = new Matrix3d(model.getMatrix());
         if (!A.invert()) {
            // deal with non-invertible
            SVDecomposition3d svd3 = new SVDecomposition3d(model.getMatrix());
            svd3.pseudoInverse(A);
         }
         mulTranspose4x4(view.R, A, matrixbuff, foffsets[NORMAL_IDX]);
         
         //         System.out.println("Model-view:");
         //         printMat(matrixbuff, foffsets[VM_IDX], 4, 4);
         //         System.out.println("Normal:");
         //         printMat(matrixbuff, foffsets[NORMAL_IDX], 4, 4);
         //         System.out.println();
      }
      
      FloatBuffer fb = FloatBuffer.wrap(matrixbuff);
      update(gl, fb);

   }
   
   public void updateMatrices(GL3 gl,  Matrix4d proj, RigidTransform3d view, AffineTransform3dBase model, 
      Matrix3d modelNormalMatrix) {
      
      float[] matrixbuff = new float[fsize];
      
      // model matrix
      GLSupport.transformToGLMatrix(matrixbuff, foffsets[M_IDX], model);
      // model view
      mul(view, model, matrixbuff, foffsets[VM_IDX]);
      // projection-model-view
      mul(proj, matrixbuff, foffsets[VM_IDX], matrixbuff, foffsets[PVM_IDX]);
      
      // normal
      mul4x4(view.R, modelNormalMatrix, matrixbuff, foffsets[NORMAL_IDX]);
      
      FloatBuffer fb = FloatBuffer.wrap(matrixbuff);
      update(gl, fb);

   }
   
   @SuppressWarnings("unused")
   private static void printMat(float[] buff, int offset, int nrows, int ncols) {
      
      for (int i=0; i<nrows; ++i) {
         System.out.print(buff[offset+i]);
         for (int j=1; j<ncols; ++j) {
            System.out.print(" " + buff[offset+i+j*nrows]);
         }
         System.out.println();
      }
      
   }  
   
   private static void mul(AffineTransform3dBase T1, AffineTransform3dBase T2, float[] out, int offset) {
      
      Matrix3dBase TM1 = T1.getMatrix();
      Vector3d Tb1 = T1.getOffset();
      
      Matrix3dBase TM2 = T2.getMatrix();
      Vector3d Tb2 = T2.getOffset();
      
      int idx = offset;
      out[idx++] = (float)(TM1.m00*TM2.m00 + TM1.m01*TM2.m10 + TM1.m02*TM2.m20);
      out[idx++] = (float)(TM1.m10*TM2.m00 + TM1.m11*TM2.m10 + TM1.m12*TM2.m20);
      out[idx++] = (float)(TM1.m20*TM2.m00 + TM1.m21*TM2.m10 + TM1.m22*TM2.m20);
      out[idx++] = 0f;
      
      out[idx++] = (float)(TM1.m00*TM2.m01 + TM1.m01*TM2.m11 + TM1.m02*TM2.m21);
      out[idx++] = (float)(TM1.m10*TM2.m01 + TM1.m11*TM2.m11 + TM1.m12*TM2.m21);
      out[idx++] = (float)(TM1.m20*TM2.m01 + TM1.m21*TM2.m11 + TM1.m22*TM2.m21);
      out[idx++] = 0f;
      
      out[idx++] = (float)(TM1.m00*TM2.m02 + TM1.m01*TM2.m12 + TM1.m02*TM2.m22);
      out[idx++] = (float)(TM1.m10*TM2.m02 + TM1.m11*TM2.m12 + TM1.m12*TM2.m22);
      out[idx++] = (float)(TM1.m20*TM2.m02 + TM1.m21*TM2.m12 + TM1.m22*TM2.m22);
      out[idx++] = 0f;
      
      out[idx++] = (float)(TM1.m00*Tb2.x + TM1.m01*Tb2.y + TM1.m02*Tb2.z + Tb1.x);
      out[idx++] = (float)(TM1.m10*Tb2.x + TM1.m11*Tb2.y + TM1.m12*Tb2.z + Tb1.y);
      out[idx++] = (float)(TM1.m20*Tb2.x + TM1.m21*Tb2.y + TM1.m22*Tb2.z + Tb1.z);
      out[idx++] = 1f;
         
   }
   
   private static void mul4x4(Matrix3dBase T1, Matrix3dBase T2, float[] out, int offset) {
      
      int idx = offset;
      out[idx++] = (float)(T1.m00*T2.m00 + T1.m01*T2.m10 + T1.m02*T2.m20);
      out[idx++] = (float)(T1.m10*T2.m00 + T1.m11*T2.m10 + T1.m12*T2.m20);
      out[idx++] = (float)(T1.m20*T2.m00 + T1.m21*T2.m10 + T1.m22*T2.m20);
      out[idx++] = 0f;
      
      out[idx++] = (float)(T1.m00*T2.m01 + T1.m01*T2.m11 + T1.m02*T2.m21);
      out[idx++] = (float)(T1.m10*T2.m01 + T1.m11*T2.m11 + T1.m12*T2.m21);
      out[idx++] = (float)(T1.m20*T2.m01 + T1.m21*T2.m11 + T1.m22*T2.m21);
      out[idx++] = 0f;
      
      out[idx++] = (float)(T1.m00*T2.m02 + T1.m01*T2.m12 + T1.m02*T2.m22);
      out[idx++] = (float)(T1.m10*T2.m02 + T1.m11*T2.m12 + T1.m12*T2.m22);
      out[idx++] = (float)(T1.m20*T2.m02 + T1.m21*T2.m12 + T1.m22*T2.m22);
      out[idx++] = 0f;
      
      out[idx++] = 0f;
      out[idx++] = 0f;
      out[idx++] = 0f;
      out[idx++] = 1f;
         
   }
   
   private static void mulTranspose4x4(Matrix3dBase T1, Matrix3dBase T2, float[] out, int offset) {
      
      int idx = offset;
      out[idx++] = (float)(T1.m00*T2.m00 + T1.m01*T2.m01 + T1.m02*T2.m02);
      out[idx++] = (float)(T1.m10*T2.m00 + T1.m11*T2.m01 + T1.m12*T2.m02);
      out[idx++] = (float)(T1.m20*T2.m00 + T1.m21*T2.m01 + T1.m22*T2.m02);
      out[idx++] = 0f;
      
      out[idx++] = (float)(T1.m00*T2.m10 + T1.m01*T2.m11 + T1.m02*T2.m12);
      out[idx++] = (float)(T1.m10*T2.m10 + T1.m11*T2.m11 + T1.m12*T2.m12);
      out[idx++] = (float)(T1.m20*T2.m10 + T1.m21*T2.m11 + T1.m22*T2.m12);
      out[idx++] = 0f;
      
      out[idx++] = (float)(T1.m00*T2.m20 + T1.m01*T2.m21 + T1.m02*T2.m22);
      out[idx++] = (float)(T1.m10*T2.m20 + T1.m11*T2.m21 + T1.m12*T2.m22);
      out[idx++] = (float)(T1.m20*T2.m20 + T1.m21*T2.m21 + T1.m22*T2.m22);
      out[idx++] = 0f;
      
      out[idx++] = 0f;
      out[idx++] = 0f;
      out[idx++] = 0f;
      out[idx++] = 1f;
         
   }
   
   private static void mul(Matrix4d P, float[] M, int moff, float[] out, int off) {
      
      int idx = off;
      out[idx++] = (float)(P.m00*M[moff] + P.m01*M[moff+1] + P.m02*M[moff+2] + P.m03*M[moff+3]);
      out[idx++] = (float)(P.m10*M[moff] + P.m11*M[moff+1] + P.m12*M[moff+2] + P.m13*M[moff+3]);
      out[idx++] = (float)(P.m20*M[moff] + P.m21*M[moff+1] + P.m22*M[moff+2] + P.m23*M[moff+3]);
      out[idx++] = (float)(P.m30*M[moff] + P.m31*M[moff+1] + P.m32*M[moff+2] + P.m33*M[moff+3]);
      
      out[idx++] = (float)(P.m00*M[moff+4] + P.m01*M[moff+5] + P.m02*M[moff+6] + P.m03*M[moff+7]);
      out[idx++] = (float)(P.m10*M[moff+4] + P.m11*M[moff+5] + P.m12*M[moff+6] + P.m13*M[moff+7]);
      out[idx++] = (float)(P.m20*M[moff+4] + P.m21*M[moff+5] + P.m22*M[moff+6] + P.m23*M[moff+7]);
      out[idx++] = (float)(P.m30*M[moff+4] + P.m31*M[moff+5] + P.m32*M[moff+6] + P.m33*M[moff+7]);
      
      out[idx++] = (float)(P.m00*M[moff+8] + P.m01*M[moff+9] + P.m02*M[moff+10] + P.m03*M[moff+11]);
      out[idx++] = (float)(P.m10*M[moff+8] + P.m11*M[moff+9] + P.m12*M[moff+10] + P.m13*M[moff+11]);
      out[idx++] = (float)(P.m20*M[moff+8] + P.m21*M[moff+9] + P.m22*M[moff+10] + P.m23*M[moff+11]);
      out[idx++] = (float)(P.m30*M[moff+8] + P.m31*M[moff+9] + P.m32*M[moff+10] + P.m33*M[moff+11]);
      
      out[idx++] = (float)(P.m00*M[moff+12] + P.m01*M[moff+13] + P.m02*M[moff+14] + P.m03*M[moff+15]);
      out[idx++] = (float)(P.m10*M[moff+12] + P.m11*M[moff+13] + P.m12*M[moff+14] + P.m13*M[moff+15]);
      out[idx++] = (float)(P.m20*M[moff+12] + P.m21*M[moff+13] + P.m22*M[moff+14] + P.m23*M[moff+15]);
      out[idx++] = (float)(P.m30*M[moff+12] + P.m31*M[moff+13] + P.m32*M[moff+14] + P.m33*M[moff+15]);
         
   }
   
   private static void copy(float[] src, int srcOffset, float[] dest, int destOffset, int len) {
      for (int i=0; i<len; ++i) {
         dest[destOffset+i] = src[srcOffset+i];
      }
   }

}
