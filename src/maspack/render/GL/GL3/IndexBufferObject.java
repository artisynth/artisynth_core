package maspack.render.GL.GL3;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

public class IndexBufferObject extends BufferObject {

   private static final int TARGET = GL.GL_ELEMENT_ARRAY_BUFFER;

   public IndexBufferObject (int vboId) {
      super (TARGET, vboId);
   }

   @Override
   public IndexBufferObject acquire () {
      return (IndexBufferObject)super.acquire ();
   }
   
   public static IndexBufferObject generate(GL3 gl) {
      int[] vbo = new int[1];
      gl.glGenBuffers (1, vbo, 0);
      return new IndexBufferObject (vbo[0]);
   }
   
   
   
}
