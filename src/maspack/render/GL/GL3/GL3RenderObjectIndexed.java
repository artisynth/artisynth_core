package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

import maspack.render.RenderObject;

/**
 * VAO-based object associated with a RenderObject, CANNOT be shared between multiple contexts
 */
public class GL3RenderObjectIndexed extends GL3ResourceBase implements GL3Drawable {

   VertexArrayObject vao;
   GL3SharedRenderObjectIndexed glo;
   int lastVertexVersion;
   GL3VertexAttributeMap attrMap;
   
   private GL3RenderObjectIndexed(VertexArrayObject vao,
      GL3SharedRenderObjectIndexed glo) {
      this.glo = glo.acquire ();
      this.vao = vao.acquire ();
      
      lastVertexVersion = -1;
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    * @param gl
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
   public GL3RenderObjectIndexed acquire () {
      return (GL3RenderObjectIndexed)super.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      return glo.maybeUpdate (gl, robj);
   }

   public void drawVertices (GL3 gl, int mode) {
      bind (gl);
      glo.drawVertices(gl, mode);
   }
   
   public void drawPointGroup (GL3 gl, int mode, int gidx) {
      bind (gl);
      glo.drawPointGroup (gl, mode, gidx);
   }
   
   public void drawLineGroup (GL3 gl, int mode, int gidx) {
      bind(gl);
      glo.drawLineGroup (gl, mode, gidx);
   }
   
   public void drawTriangleGroup (GL3 gl, int mode, int gidx) {
      bind(gl);
      glo.drawTriangleGroup (gl, mode, gidx);
   }
   
   public static GL3RenderObjectIndexed generate(GL3 gl, GL3SharedRenderObjectIndexed glo) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      GL3RenderObjectIndexed out = new GL3RenderObjectIndexed (vao, glo);
      return out;
   }
}
