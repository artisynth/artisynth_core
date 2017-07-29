package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;


public class GL3VertexAttributeArray {

   private VertexBufferObject vbo;                   // pointer to VBO
   private GL3VertexAttributeArrayInfo info;   // info
   
   public GL3VertexAttributeArray(VertexBufferObject vbo, GL3VertexAttributeArrayInfo info) {
      this.vbo = vbo;           
      this.info = info;
   }
  
   public VertexBufferObject getVBO() {
      return vbo;
   }
   
   public GL3VertexAttributeArrayInfo getInfo() {
      return info;
   }
   
   public int getType() {
      return info.getType ();
   }
   
   public int getSize() {
      return info.getSize ();
   }
   
   public boolean isNormalized() {
      return info.isNormalized ();
   }
   
   public int getOffset() {
      return info.getOffset ();
   }
   
   public int getStride() {
      return info.getStride ();
   }
   
   public int getCount() {
      return info.getCount ();
   }
   
   public int getDivisor() {
      return info.getDivisor ();
   }

   public void bind(GL3 gl) {
      vbo.bind (gl);
      info.bind (gl);
   }
   
   public void bind(GL3 gl, int location) {
      if (location >= 0) {
         vbo.bind(gl);
         info.bind (gl, location);
      }
   }
   
   public boolean isValid () {
      return vbo.isValid ();
   }
   
   @Override
   public String toString () {
      return info.toString ();
   }
   
}
