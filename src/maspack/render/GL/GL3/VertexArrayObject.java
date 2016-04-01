package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

public class VertexArrayObject extends GL3ResourceBase {

   int vao;
   
   public VertexArrayObject(int vao) {
      this.vao = vao;
   }
   
   public void bind(GL3 gl) {
      gl.glBindVertexArray (vao);
   }
   
   public void unbind(GL3 gl) {
      gl.glBindVertexArray (0);
   }
   
   @Override
   public void dispose (GL3 gl) {
      gl.glDeleteBuffers (1, new int[]{vao}, 0);
   }
   
   @Override
   public VertexArrayObject acquire () {
      return (VertexArrayObject)super.acquire ();
   }
   
   public static VertexArrayObject generate(GL3 gl) {
      int [] vao = new int[1];
      gl.glGenVertexArrays (1, vao, 0);
      return new VertexArrayObject (vao[0]);
   }
   
}
