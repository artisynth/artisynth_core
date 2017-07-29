package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;

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
         GL3Utilities.activateVertexAttribute(gl3, nloc, 
            GL3AttributeStorage.FLOAT_N_3, vertexStride, normalOffset);
      } else {
         GL3Utilities.deactivateVertexAttribute(gl3, nloc);
      }
      
      if (colorOffset >= 0) {
         GL3Utilities.activateVertexAttribute(gl3, cloc, GL3AttributeStorage.UBYTE_4,
            vertexStride, colorOffset);
      } else {
         GL3Utilities.deactivateVertexAttribute(gl3, nloc);
      }
      
      if (texcoordOffset >= 0) {
         GL3Utilities.activateVertexAttribute(gl3, tloc, GL3AttributeStorage.FLOAT_2,
            vertexStride, texcoordOffset);
      } else {
         GL3Utilities.deactivateVertexAttribute(gl3, tloc);
      }
      
      GL3Utilities.activateVertexAttribute(gl3, ploc, GL3AttributeStorage.FLOAT_3,
         vertexStride, positionOffset);
      
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
