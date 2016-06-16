package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import maspack.render.GL.GLPipelineRendererBase;
import maspack.render.GL.GLSupport;

public class GL3PipelineRenderer extends GLPipelineRendererBase {

   VertexArrayObject vao;
   VertexBufferObject vbo;
   
   // attribute locations
   int nloc;
   int cloc;
   int tloc;
   int ploc;
   
   private GL3PipelineRenderer (
      VertexArrayObject vao, VertexBufferObject vbo,
      int normalAttribLocation, int colorAttribLocation,
      int texcoordAttribLocation, int positionAttribLocation) {
      this.vao = vao.acquire ();
      this.vbo = vbo.acquire ();
      nloc = normalAttribLocation;
      cloc = colorAttribLocation;
      tloc = texcoordAttribLocation;
      ploc = positionAttribLocation;
   }
   
   @Override
   public void bind (
      GL gl, ByteBuffer buff, int normalOffset, int colorOffset,
      int texcoordOffset, int positionOffset, int vertexStride) {

      GL3 gl3 = (GL3)gl;
      vao.bind (gl3);
      vbo.allocate (gl3, buff.capacity (), GL3.GL_STREAM_DRAW);
      
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
      if (count > 0) {
         GL3 gl3 = (GL3)gl;
         vao.bind (gl3);
         vbo.fill (gl3, vbuff);
         
         gl.glDrawArrays (glMode, 0, count);
      }
   }
   
   @Override
   public void dispose (GL gl) {
      super.dispose (gl);
      
      GL3 gl3 = (GL3)gl;
      if (vao != null) {
         vao.releaseDispose (gl3);
         vao = null;
      }
      if (vbo != null) {
         vbo.releaseDispose (gl3);
         vbo = null;
      }
   }
   
   public static GL3PipelineRenderer generate(GL3 gl,
      int normalAttribLocation, int colorAttribLocation,
      int texcoordAttribLocation, int positionAttribLocation) {
     VertexArrayObject vao = VertexArrayObject.generate (gl);
     VertexBufferObject vbo = VertexBufferObject.generate (gl);
     return new GL3PipelineRenderer (vao, vbo, 
        normalAttribLocation, colorAttribLocation, 
        texcoordAttribLocation, positionAttribLocation);
   }

}
