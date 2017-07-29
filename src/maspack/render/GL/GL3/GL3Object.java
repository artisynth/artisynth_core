package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.GL.GLShaderProgram;

/**
 * Standard VAO-based object, CANNOT be shared between multiple contexts
 */
public class GL3Object extends GL3ResourceBase implements GL3Drawable {

   VertexArrayObject vao;
   GL3SharedObject glo;
   
   /**
    * VAO should either already have attributes bound, or be sure
    * to call {@link #bind(GL3)} before drawing
    * @param vao vertex array object
    * @param glo potentially shared object
    */
   protected GL3Object(VertexArrayObject vao, GL3SharedObject glo) {
      // hold a reference to this object
      this.vao = vao.acquire ();
      this.glo = glo.acquire ();
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    */
   public void bind(GL3 gl) {
      vao.bind (gl);
      glo.bindAttributes (gl);
      vao.unbind (gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (vao != null) {
         vao.releaseDispose (gl);
         glo.releaseDispose (gl);
         vao = null;
         glo = null;
      }
   }
   
   @Override
   public boolean isDisposed () {
      return (vao == null);
   }
   
   @Override
   public boolean isValid () {
      if (vao == null || glo == null) {
         return false;
      }
      if (!vao.isValid ()) {
         return false;
      }
      if (!glo.isValid ()) {
         return false;
      }
      return true;
   }
   
   public GL3SharedObject getShared () {
      return glo;
   }
   
   @Override
   public GL3Object acquire () {
      return (GL3Object)super.acquire ();
   }

   // @Override
   public void draw (GL3 gl) {
      vao.bind (gl);
      glo.draw(gl);
      vao.unbind (gl);
   }   
   
   public void drawArrays(GL3 gl, int mode) {
      vao.bind (gl);
      glo.drawArrays(gl, mode);
      vao.unbind (gl);
   }
   
   public void drawArrays(GL3 gl, int mode, int start, int count) {
      vao.bind (gl);
      glo.drawArrays (gl, mode, start, count);
      vao.unbind (gl);
   }
   
   public void drawElements(GL3 gl, int mode, int start, int count, int indexType) {
      vao.bind (gl);
      glo.drawElements (gl, mode, start, count, indexType);
      vao.unbind (gl);
   }
   
   public void drawInstancedArray(GL3 gl, GLShaderProgram prog, int mode, int start, int count, int instances) {
      vao.bind (gl);
      glo.drawInstancedArray (gl, mode, start, count, instances);
      vao.unbind (gl);
   }

   public void drawInstancedElements(GL3 gl, int mode, int start, int count, int instances) {
      vao.bind (gl);
      glo.drawInstancedElements (gl, mode, start, count, instances);
      vao.unbind (gl);
   }
   
   public void draw(GL3 gl, int start, int count) {
      vao.bind (gl);
      glo.draw (gl, start, count);
      vao.unbind (gl);
   }
   
   public void draw(GL3 gl, int mode, int start, int count) {
      vao.bind (gl);
      glo.draw (gl, mode, start, count);
      vao.unbind (gl);
   }

   public static GL3Object generate(GL3 gl, GL3SharedObject glo) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      GL3Object out = new GL3Object (vao, glo);
      out.bind (gl);
      return out;
   }
   
}
