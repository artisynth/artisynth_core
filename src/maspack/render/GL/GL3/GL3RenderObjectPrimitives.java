package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;

/**
 * VAO-based object associated with a RenderObject, CANNOT be shared between multiple contexts
 */
public class GL3RenderObjectPrimitives extends GL3ResourceBase {

   VertexArrayObject vao;
   GL3SharedRenderObjectPrimitives glo;
   int lastVertexVersion;
   
   private GL3RenderObjectPrimitives(VertexArrayObject vao,
      GL3SharedRenderObjectPrimitives glo) {
      this.glo = glo.acquire ();
      this.vao = vao.acquire ();
      
      lastVertexVersion = -1;
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    */
   public void bind(GL3 gl) {
      vao.bind (gl);
      int vv = glo.getBindVersion ();
      if (lastVertexVersion != vv) {
         glo.bindVertices (gl);
         glo.bindIndices (gl);
         lastVertexVersion = vv;
      }
   }
   
   public void unbind(GL3 gl) {
      vao.unbind (gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      vao.releaseDispose (gl);
      glo.releaseDispose (gl);
      vao = null;
      glo = null;
   }
   
   @Override
   public boolean isDisposed () {
      return (vao == null);
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
   public GL3RenderObjectPrimitives acquire () {
      return (GL3RenderObjectPrimitives)super.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      return glo.maybeUpdate (gl, robj);
   }

   public void drawVertices (GL3 gl, int mode) {
      bind (gl);
      glo.drawVertices(gl, mode);
      unbind(gl);
   }
   
   public void drawPointGroup (GL3 gl, int mode, int gidx) {
      bind (gl);
      glo.drawPointGroup (gl, mode, gidx);
      unbind(gl);
   }
   
   public void drawPointGroup (GL3 gl, int mode, int gidx, int offset, int count) {
      bind (gl);
      glo.drawPointGroup (gl, mode, gidx, offset, count);
      unbind(gl);
   }
   
   public void drawLineGroup (GL3 gl, int mode, int gidx) {
      bind(gl);
      glo.drawLineGroup (gl, mode, gidx);
      unbind(gl);
   }
   
   public void drawLineGroup (GL3 gl, int mode, int gidx, int offset, int count) {
      bind(gl);
      glo.drawLineGroup (gl, mode, gidx, offset, count);
      unbind(gl);
   }
   
   public void drawTriangleGroup (GL3 gl, int mode, int gidx) {
      bind(gl);
      glo.drawTriangleGroup (gl, mode, gidx);
      unbind(gl);
   }
   
   public void drawTriangleGroup (GL3 gl, int mode, int gidx, int offset, int count) {
      bind(gl);
      glo.drawTriangleGroup (gl, mode, gidx, offset, count);
      unbind(gl);
   }
   
   public static GL3RenderObjectPrimitives generate(GL3 gl, GL3SharedRenderObjectPrimitives glo) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      GL3RenderObjectPrimitives out = new GL3RenderObjectPrimitives (vao, glo);
      return out;
   }
}
