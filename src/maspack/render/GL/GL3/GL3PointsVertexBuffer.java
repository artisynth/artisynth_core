package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.GL.GLSupport;
import maspack.util.BufferUtilities;

public class GL3PointsVertexBuffer extends GL3ResourceBase {

   VertexBufferObject vbo;
   ByteBuffer buff;

   GL3VertexAttributeArrayInfo radAttr;

   // track version numbers so we can detect what has changed since last use
   float lastPointRadius;
   
   public GL3PointsVertexBuffer(VertexBufferObject vbo, GL3VertexAttributeInfo radAttr) {

      this.vbo = vbo.acquire ();  // hold on to VBO

      int FLOAT_SIZE = GL3AttributeStorage.FLOAT.width ();
      int stride = FLOAT_SIZE;

      this.radAttr = new GL3VertexAttributeArrayInfo (radAttr, 
         GL3AttributeStorage.FLOAT, 0, stride, 1);
      
      lastPointRadius = 0;
      buff = BufferUtilities.newNativeByteBuffer(GLSupport.FLOAT_SIZE);
   }

   public boolean maybeUpdate(GL3 gl, float pointRadius) {
      if (pointRadius != lastPointRadius) {
         buff.clear ();
         buff.putFloat (pointRadius);
         buff.flip();
         gl.glBindVertexArray (0); // unbind any existing VAOs
         vbo.fill(gl, buff, GL.GL_DYNAMIC_DRAW);
         lastPointRadius = pointRadius;
         return true;
      }  
      return false;
   }

   public void bind(GL3 gl, int numInstances) {
      vbo.bind (gl);
      radAttr.bind (gl);
      radAttr.bindDivisor (gl, numInstances);
   }

   @Override
   public boolean isValid () {
      if (vbo == null) {
         return false;
      }
      return vbo.isValid ();
   }

   @Override
   public void dispose (GL3 gl) {
      if (vbo != null) {
         vbo.releaseDispose (gl);
         vbo = null;
      }

      lastPointRadius = 0;
      BufferUtilities.freeDirectBuffer (buff);
      buff = null;
   }
   
   @Override
   public boolean isDisposed () {
      return (vbo == null);
   }

   @Override
   public GL3PointsVertexBuffer acquire () {
      return (GL3PointsVertexBuffer)super.acquire ();
   }
   
   public static GL3PointsVertexBuffer generate ( GL3 gl, GL3VertexAttributeInfo radAttr) {
         VertexBufferObject vbo = VertexBufferObject.generate(gl);
         GL3PointsVertexBuffer pbuff = new GL3PointsVertexBuffer (vbo, radAttr);
         pbuff.maybeUpdate (gl, 1);
         return pbuff;
   }



}
