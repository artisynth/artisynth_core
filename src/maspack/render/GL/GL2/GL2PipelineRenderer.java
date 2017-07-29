package maspack.render.GL.GL2;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

import maspack.render.GL.GLPipelineRendererBase;

public class GL2PipelineRenderer extends GLPipelineRendererBase {

   int vbo;
   
   private GL2PipelineRenderer (int vbo) {
      this.vbo = vbo;
   }
   
   @Override
   public void bind (
      GL gl, ByteBuffer buff, int normalOffset, int colorOffset,
      int texcoordOffset, int positionOffset, int vertexStride) {
      
      GL2 gl2 = (GL2)gl;
      gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
      
      if (normalOffset >= 0) {
         gl2.glEnableClientState (GL2.GL_NORMAL_ARRAY);
         gl2.glNormalPointer (GL.GL_FLOAT, vertexStride, normalOffset);
      } else {
         gl2.glDisableClientState (GL2.GL_NORMAL_ARRAY);
      }
      
      if (colorOffset >= 0) {
         gl2.glEnableClientState (GL2.GL_COLOR_ARRAY);
         gl2.glColorPointer (4, GL2.GL_UNSIGNED_BYTE, vertexStride, colorOffset);
      } else {
         gl2.glDisableClientState (GL2.GL_COLOR_ARRAY);
      }
      
      if (texcoordOffset >= 0) {
         gl2.glEnableClientState (GL2.GL_TEXTURE_COORD_ARRAY);
         gl2.glTexCoordPointer (2, GL2.GL_FLOAT, vertexStride, texcoordOffset);
      } else {
         gl2.glDisableClientState (GL2.GL_TEXTURE_COORD_ARRAY);
      }
      
      gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
      gl2.glVertexPointer (3, GL2.GL_FLOAT, vertexStride, positionOffset);
      
   }

   
   @Override
   protected void draw (GL gl, int glMode, ByteBuffer vbuff, int count) {
      if (count > 0) {
         GL2 gl2 = (GL2)gl;
         gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
         // orphan and fill
         gl2.glBufferData (GL.GL_ARRAY_BUFFER, vbuff.capacity (), null, GL2GL3.GL_STREAM_DRAW);
         gl2.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, vbuff.limit (), vbuff);
         
         gl2.glDrawArrays(glMode, 0, count);
      }
   }
   
   @Override
   protected void unbind (GL gl) {
      gl.glBindBuffer (GL.GL_ARRAY_BUFFER, 0);
   }
   
   @Override
   public void dispose (GL gl) {
      super.dispose (gl);
      if (vbo != -1) {
         int[] v = {vbo};
         gl.glDeleteBuffers (1, v, 0);
         vbo = v[0];
      }
   }
   
   public static GL2PipelineRenderer generate(GL gl) {
      int[] v = new int[1];
      gl.glGenBuffers (1, v, 0);
      return new GL2PipelineRenderer (v[0]);
   }

}
