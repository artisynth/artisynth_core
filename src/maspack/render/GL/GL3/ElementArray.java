package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.GL.GLSupport;

public class ElementArray extends GL3ResourceBase {
   
   IndexBufferObject ibo;
   int type;
   int count;
   int stride;
   
   public ElementArray(IndexBufferObject ibo) {
      this.ibo = ibo.acquire ();
      this.type = GL.GL_UNSIGNED_INT;
      this.count = 0;
      this.stride = GLSupport.INTEGER_SIZE;
   }
   
   public int count() {
      return count;
   }
   
   public int type() {
      return type;
   }
   
   public int stride() {
      return stride;
   }
   
   public void fill(GL3 gl, ByteBuffer buff, int type, int stride, int count, int size, int usage) {
      ibo.fill (gl, buff, size, usage);
      this.type = type;
      this.count = count;
      this.stride = stride;
   }
   
   public void bind(GL3 gl) {
      ibo.bind (gl);
   }
   
   public void unbind(GL3 gl) {
      ibo.unbind (gl);
   }
   
   @Override
   public boolean isValid () {
      return ibo.isValid ();
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (ibo != null) {
         ibo.releaseDispose (gl);
         ibo = null;
      }
      this.count = 0;
   }

   @Override
   public boolean isDisposed () {
      return (ibo == null);
   }
   
   @Override
   public ElementArray acquire () {
      return (ElementArray)super.acquire ();
   }
   
   public static ElementArray generate(GL3 gl) {
      IndexBufferObject ibo = IndexBufferObject.generate (gl);
      return new ElementArray(ibo);
   }

}
