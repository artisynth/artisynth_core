package maspack.render.GL.GL3;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

public class VertexBufferObject extends BufferObject {

   private static final int TARGET = GL.GL_ARRAY_BUFFER;
   
   public VertexBufferObject (int vboId) {
      super (TARGET, vboId);
   }

   @Override
   public VertexBufferObject acquire () {
      return (VertexBufferObject)super.acquire ();
   }
   
   public static VertexBufferObject generate(GL3 gl) {
      int[] vbo = new int[1];
      gl.glGenBuffers (1, vbo, 0);
      return new VertexBufferObject (vbo[0]);
   }
   
   
   
}
