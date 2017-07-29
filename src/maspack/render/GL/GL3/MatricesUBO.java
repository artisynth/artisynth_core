package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix2dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix4d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

public class MatricesUBO extends UniformBufferObject {
   
   static final String[] PVM_ATTRIBUTES = { "pvm_matrix",    // model to screen
                                  "vm_matrix",      // model to viewer
                                  "m_matrix",       // model to world
                                  "normal_matrix",  // model to viewer normal
                                  "texture_matrix"  // texture coordinate matrix
                                };
   static final int PVM_IDX = 0;
   static final int VM_IDX = 1;
   static final int M_IDX = 2;
   static final int NORMAL_IDX = 3;
   static final int TEXTURE_IDX = 4;
   
   static final String PVM_NAME = "Matrices";
   
   private MatricesUBO(GL3 gl, int progId) {
      super(gl, progId, PVM_NAME, PVM_ATTRIBUTES, GL3.GL_DYNAMIC_DRAW);
   }
   
   public void updateMatrices(GL3 gl,  Matrix4d proj, RigidTransform3d view, 
      AffineTransform3dBase model, AffineTransform2dBase texture) {
      
      ByteBuffer buff = getBuffer ();
      
      // model view
      AffineTransform3d modelview = new AffineTransform3d(view);
      modelview.mul (model);
      
      // pvm matrix
      buff.position (getByteOffset(PVM_IDX));
      mul(proj, modelview, buff);
      
      // model-view
      buff.position(getByteOffset(VM_IDX));
      putMatrix(buff, modelview);
      
      // model matrix
      buff.position(getByteOffset(M_IDX));
      putMatrix(buff, model);
      
      // normal
      buff.position (getByteOffset(NORMAL_IDX));
      if (model instanceof RigidTransform3d) {
         putMatrix(buff, modelview);   
      } else {
         
         // invert model matrix
         Matrix3d A = new Matrix3d(model.getMatrix());
         if (!A.invert()) {
            // deal with non-invertible
            SVDecomposition3d svd3 = new SVDecomposition3d(model.getMatrix());
            svd3.pseudoInverse(A);
         }
         mulTranspose4x4(view.R, A, buff);
      }
      
      buff.position (getByteOffset (TEXTURE_IDX));
      putMatrix(buff, texture);
      
      buff.flip ();
      update(gl, buff);

   }
   
   public void updateMatrices(GL3 gl,  Matrix4d proj, RigidTransform3d view, 
      AffineTransform3dBase model, Matrix3d modelNormal,
      AffineTransform2dBase texture) {
      
      ByteBuffer buff = getBuffer ();
      
      // model view
      AffineTransform3d modelview = new AffineTransform3d(view);
      modelview.mul (model);
      
      // pvm matrix
      buff.position (getByteOffset(PVM_IDX));
      mul(proj, modelview, buff);
      
      // model-view
      buff.position(getByteOffset(VM_IDX));
      putMatrix(buff, modelview);
      
      // model matrix
      buff.position(getByteOffset(M_IDX));
      putMatrix(buff, model);
      
      // normal
      buff.position(getByteOffset(NORMAL_IDX));
      mul4x4(view.R, modelNormal, buff);
      
      // texture
      buff.position (getByteOffset (TEXTURE_IDX));
      putMatrix(buff, texture);
      
      buff.flip ();
      update(gl, buff);

   } 
   
   public static void putMatrix (ByteBuffer buff, AffineTransform3dBase T) {
      Matrix3dBase M = T.getMatrix();
      Vector3d b = T.getOffset();
      buff.putFloat((float) M.m00);
      buff.putFloat ((float)M.m10);
      buff.putFloat ((float)M.m20);
      buff.putFloat ((float)0);

      buff.putFloat ((float)M.m01);
      buff.putFloat ((float)M.m11);
      buff.putFloat ((float)M.m21);
      buff.putFloat ((float)0);

      buff.putFloat ((float)M.m02);
      buff.putFloat ((float)M.m12);
      buff.putFloat ((float)M.m22);
      buff.putFloat ((float)0);

      buff.putFloat ((float)b.x);
      buff.putFloat ((float)b.y);
      buff.putFloat ((float)b.z);
      buff.putFloat ((float)1);
   }
   
   private static void mul4x4(Matrix3dBase T1, Matrix3dBase T2, ByteBuffer buff) {
      
      buff.putFloat((float)(T1.m00*T2.m00 + T1.m01*T2.m10 + T1.m02*T2.m20));
      buff.putFloat((float)(T1.m10*T2.m00 + T1.m11*T2.m10 + T1.m12*T2.m20));
      buff.putFloat((float)(T1.m20*T2.m00 + T1.m21*T2.m10 + T1.m22*T2.m20));
      buff.putFloat(0f);
      
      buff.putFloat((float)(T1.m00*T2.m01 + T1.m01*T2.m11 + T1.m02*T2.m21));
      buff.putFloat((float)(T1.m10*T2.m01 + T1.m11*T2.m11 + T1.m12*T2.m21));
      buff.putFloat((float)(T1.m20*T2.m01 + T1.m21*T2.m11 + T1.m22*T2.m21));
      buff.putFloat(0f);
      
      buff.putFloat((float)(T1.m00*T2.m02 + T1.m01*T2.m12 + T1.m02*T2.m22));
      buff.putFloat((float)(T1.m10*T2.m02 + T1.m11*T2.m12 + T1.m12*T2.m22));
      buff.putFloat((float)(T1.m20*T2.m02 + T1.m21*T2.m12 + T1.m22*T2.m22));
      buff.putFloat(0f);
      
      buff.putFloat(0f);
      buff.putFloat(0f);
      buff.putFloat(0f);
      buff.putFloat(1f);
         
   }
   
   private static void mulTranspose4x4(Matrix3dBase T1, Matrix3dBase T2, ByteBuffer buff) {
      
      buff.putFloat((float)(T1.m00*T2.m00 + T1.m01*T2.m01 + T1.m02*T2.m02));
      buff.putFloat((float)(T1.m10*T2.m00 + T1.m11*T2.m01 + T1.m12*T2.m02));
      buff.putFloat((float)(T1.m20*T2.m00 + T1.m21*T2.m01 + T1.m22*T2.m02));
      buff.putFloat(0f);
      
      buff.putFloat((float)(T1.m00*T2.m10 + T1.m01*T2.m11 + T1.m02*T2.m12));
      buff.putFloat((float)(T1.m10*T2.m10 + T1.m11*T2.m11 + T1.m12*T2.m12));
      buff.putFloat((float)(T1.m20*T2.m10 + T1.m21*T2.m11 + T1.m22*T2.m12));
      buff.putFloat(0f);
      
      buff.putFloat((float)(T1.m00*T2.m20 + T1.m01*T2.m21 + T1.m02*T2.m22));
      buff.putFloat((float)(T1.m10*T2.m20 + T1.m11*T2.m21 + T1.m12*T2.m22));
      buff.putFloat((float)(T1.m20*T2.m20 + T1.m21*T2.m21 + T1.m22*T2.m22));
      buff.putFloat(0f);
      
      buff.putFloat(0f);
      buff.putFloat(0f);
      buff.putFloat(0f);
      buff.putFloat(1f);
         
   }
   
   private static void mul(Matrix4d P, AffineTransform3dBase M, ByteBuffer buff) {
      Matrix3dBase R = M.getMatrix ();
      Vector3d b = M.getOffset ();
      
      buff.putFloat((float)(P.m00*R.m00 + P.m01*R.m10 + P.m02*R.m20));
      buff.putFloat((float)(P.m10*R.m00 + P.m11*R.m10 + P.m12*R.m20));
      buff.putFloat((float)(P.m20*R.m00 + P.m21*R.m10 + P.m22*R.m20));
      buff.putFloat((float)(P.m30*R.m00 + P.m31*R.m10 + P.m32*R.m20));
      
      buff.putFloat((float)(P.m00*R.m01 + P.m01*R.m11 + P.m02*R.m21));
      buff.putFloat((float)(P.m10*R.m01 + P.m11*R.m11 + P.m12*R.m21));
      buff.putFloat((float)(P.m20*R.m01 + P.m21*R.m11 + P.m22*R.m21));
      buff.putFloat((float)(P.m30*R.m01 + P.m31*R.m11 + P.m32*R.m21));
      
      buff.putFloat((float)(P.m00*R.m02 + P.m01*R.m12 + P.m02*R.m22));
      buff.putFloat((float)(P.m10*R.m02 + P.m11*R.m12 + P.m12*R.m22));
      buff.putFloat((float)(P.m20*R.m02 + P.m21*R.m12 + P.m22*R.m22));
      buff.putFloat((float)(P.m30*R.m02 + P.m31*R.m12 + P.m32*R.m22));
      
      buff.putFloat((float)(P.m00*b.x + P.m01*b.y + P.m02*b.z + P.m03));
      buff.putFloat((float)(P.m10*b.x + P.m11*b.y + P.m12*b.z + P.m13));
      buff.putFloat((float)(P.m20*b.x + P.m21*b.y + P.m22*b.z + P.m23));
      buff.putFloat((float)(P.m30*b.x + P.m31*b.y + P.m32*b.z + P.m33));
   }
   
   public static void putMatrix (ByteBuffer buff, AffineTransform2dBase T) {

      Matrix2dBase M = T.getMatrix ();
      Vector2d b = T.getOffset();
      buff.putFloat((float) M.m00);
      buff.putFloat ((float)M.m10);
      buff.putFloat (0);
      buff.putFloat (0);

      buff.putFloat ((float)M.m01);
      buff.putFloat ((float)M.m11);
      buff.putFloat (0);
      buff.putFloat (0);

      buff.putFloat (0);
      buff.putFloat (0);
      buff.putFloat (0);
      buff.putFloat (0);

      buff.putFloat ((float)b.x);
      buff.putFloat ((float)b.y);
      buff.putFloat (0);
      buff.putFloat ((float)1);
   }
   
   @Override
   public MatricesUBO acquire () {
      return (MatricesUBO)super.acquire ();
   }

   public static MatricesUBO generate(GL3 gl, int progId) {
      return new MatricesUBO (gl, progId);
   }
   
}
