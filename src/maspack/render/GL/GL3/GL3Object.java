package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

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
   private GL3Object(VertexArrayObject vao, GL3SharedObject glo) {
      // hold a reference to this object
      this.vao = vao.acquire ();
      this.glo = glo.acquire ();
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    * @param gl
    */
   public void bind(GL3 gl) {
      vao.bind (gl);
      glo.bindAttributes (gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      vao.releaseDispose (gl);
      glo.releaseDispose (gl);
   }
   
   @Override
   public boolean isValid () {
      if (!vao.isValid ()) {
         return false;
      }
      if (!glo.isValid ()) {
         return false;
      }
      return true;
   }
   
   @Override
   public boolean disposeInvalid (GL3 gl) {
      if (!isValid ()) {
         dispose(gl);
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
   }   
   
   public void drawArrays(GL3 gl, int mode) {
      vao.bind (gl);
      glo.drawArrays(gl, mode);
   }
   
   public void drawArrays(GL3 gl, int mode, int start, int count) {
      vao.bind (gl);
      glo.drawArrays (gl, mode, start, count);
   }
   
   public void drawElements(GL3 gl, int mode, int start, int count, int indexType) {
      vao.bind (gl);
      glo.drawElements (gl, mode, start, count, indexType);
   }
   
   public void drawInstancedArray(GL3 gl, GLShaderProgram prog, int mode, int start, int count, int instances) {
      vao.bind (gl);
      glo.drawInstancedArray (gl, mode, start, count, instances);
   }

   public void drawInstancedElements(GL3 gl, int mode, int start, int count, int instances) {
      vao.bind (gl);
      glo.drawInstancedElements (gl, mode, start, count, instances);
   }
   
   public void draw(GL3 gl, int start, int count) {
      vao.bind (gl);
      glo.draw (gl, start, count);
   }
   
   public void draw(GL3 gl, int mode, int start, int count) {
      vao.bind (gl);
      glo.draw (gl, mode, start, count);
   }

   public static GL3Object generate(GL3 gl, GL3SharedObject glo) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      GL3Object out = new GL3Object (vao, glo);
      out.bind (gl);
      return out;
   }
   
}
