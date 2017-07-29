package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

public class VertexArrayObject extends GL3ResourceBase {

   boolean bound;
   
   static final VertexArrayObject DEFAULT = new VertexArrayObject (0);
   public static VertexArrayObject getDefault()  {
      return DEFAULT;
   }
   
   public static void bindDefault(GL3 gl) {
      DEFAULT.bind (gl);
   }
   
   int vao;
   
   public VertexArrayObject(int vao) {
      this.vao = vao;
      this.bound = false;
      // System.out.println ("VAO created: " + vao);
   }
   
   public void bind(GL3 gl) {
      // System.out.println ("VAO bound: " + vao);
      gl.glBindVertexArray (vao);
      bound = true;
   }
   
   public void unbind(GL3 gl) {
      // System.out.println ("VAO unbound: " + vao);
      bound = false;
      gl.glBindVertexArray (0);
   }
   
   public boolean isBound() {
      return bound;
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (vao != 0) {
         // System.out.println ("VAO destroyed: " + vao);
         gl.glDeleteVertexArrays (1, new int[]{vao}, 0);
         vao = 0;
      }
   }
   
   @Override
   public boolean isDisposed () {
      return vao == 0;
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
