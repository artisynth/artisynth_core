package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import maspack.render.GL.GLPipelineRendererBase;

public class GL3PipelineRenderer extends GLPipelineRendererBase {

   int vao;
   int vbo;
   
   // attribute locations
   int nloc;
   int cloc;
   int tloc;
   int ploc;
   
   public GL3PipelineRenderer (int normalAttribLocation, int colorAttribLocation,
      int texcoordAttribLocation, int positionAttribLocation) {
      vao = 0;
      vbo = -1;
      nloc = normalAttribLocation;
      cloc = colorAttribLocation;
      tloc = texcoordAttribLocation;
      ploc = positionAttribLocation;
   }
   
   @Override
   public void init (GL gl) {
      GL3 gl3 = (GL3)gl;
      int[] v = new int[1];
      if (vao == 0) {
         gl3.glGenVertexArrays (1, v, 0);
         vao = v[0];
      }
      if (vbo == -1) {
         gl3.glGenBuffers (1, v, 0);
         vbo = v[0];
      }
   }
   
   @Override
   public boolean isInitialized () {
      return (vao != 0 && vbo != -1);
   }
   
   @Override
   public void bind (
      GL gl, ByteBuffer buff, int normalOffset, int colorOffset,
      int texcoordOffset, int positionOffset, int vertexStride) {

      GL2GL3 gl3 = (GL2GL3)gl;
      gl3.glBindVertexArray (vao);
      gl3.glBindBuffer (GL.GL_ARRAY_BUFFER, vbo);
      gl3.glBufferData (GL.GL_ARRAY_BUFFER, buff.capacity (), null, GL2GL3.GL_STREAM_DRAW);
      
      if (normalOffset >= 0) {
         gl3.glEnableVertexAttribArray (nloc);
         gl3.glVertexAttribPointer (nloc, 3, GL.GL_FLOAT, true, vertexStride, normalOffset);
      } else {
         gl3.glDisableVertexAttribArray (nloc);
      }
      
      if (colorOffset >= 0) {
         gl3.glEnableVertexAttribArray (cloc);
         gl3.glVertexAttribPointer (cloc, 4, GL.GL_UNSIGNED_BYTE, false, vertexStride, colorOffset);
      } else {
         gl3.glDisableVertexAttribArray (cloc);
      }
      
      if (texcoordOffset >= 0) {
         gl3.glEnableVertexAttribArray (tloc);
         gl3.glVertexAttribPointer (tloc, 2, GL.GL_FLOAT, false, vertexStride, texcoordOffset);
      } else {
         gl3.glDisableVertexAttribArray (tloc);
      }
      
      gl3.glEnableVertexAttribArray (ploc);
      gl3.glVertexAttribPointer (ploc, 3, GL.GL_FLOAT, false, vertexStride, positionOffset);
      
   }
   
   @Override
   protected void unbind (GL gl) {
      GL2GL3 gl3 = (GL2GL3)gl;
      gl3.glBindVertexArray (0);
   }

   
   @Override
   protected void draw (GL gl, int glMode, ByteBuffer vbuff, int count) {
      GL2GL3 gl3 = (GL2GL3)gl;
      gl3.glBindVertexArray (vao);
      gl3.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
      // orphan and fill
      gl3.glBufferData (GL.GL_ARRAY_BUFFER, vbuff.capacity (), null, GL2GL3.GL_STREAM_DRAW);
      gl3.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, vbuff.limit (), vbuff);
      
      gl.glDrawArrays (glMode, 0, count);
   }
   
   @Override
   public void dispose (GL gl) {
      super.dispose (gl);
      
      GL3 gl3 = (GL3)gl;
      int[] v = new int[1];
      if (vao != 0) {
         v[0] = vao;
         gl3.glDeleteVertexArrays (1, v, 0);
         vao = 0;
      }
      if (vbo != -1) {
         v[0] = vbo;
         gl3.glDeleteBuffers (1, v, 0);
         vbo = -1;
      }
   }

}
