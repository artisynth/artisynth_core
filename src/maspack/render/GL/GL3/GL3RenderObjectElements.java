package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.VertexIndexArray;

/**
 * VAO-based object associated with a RenderObject, CANNOT be shared between multiple contexts
 */
public class GL3RenderObjectElements extends GL3ResourceBase {

   VertexArrayObject vao;
   GL3SharedRenderObjectVertices glo;
   GL3SharedVertexIndexArray ibo;
   
   int lastBindVersion;
   int lastElementsVersion;
   
   private GL3RenderObjectElements(VertexArrayObject vao,
      GL3SharedVertexIndexArray ibo,
      GL3SharedRenderObjectVertices glo) {
      this.glo = glo.acquire ();
      this.vao = vao.acquire ();
      this.ibo = ibo.acquire ();
      lastBindVersion = -1;
      lastElementsVersion = -1;
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    */
   public void bind(GL3 gl) {
      vao.bind (gl);
      int vv = glo.getBindVersion ();
      if (lastBindVersion != vv) {
         glo.bindVertices (gl);
         ibo.bind (gl);
         lastBindVersion = vv;
      }
   }
   
   public void unbind(GL3 gl) {
      vao.unbind (gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (vao != null) {
         vao.releaseDispose (gl);
         vao = null;
      }
      if (ibo != null) {
         ibo.releaseDispose (gl);
         ibo = null;
      }
      if (glo != null) {
         glo.releaseDispose (gl);
         glo = null;
      }
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
      if (!ibo.isValid ()) {
         return false;
      }
      return true;
   }
   
   
   @Override
   public GL3RenderObjectElements acquire () {
      return (GL3RenderObjectElements)super.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj, VertexIndexArray elements) {
      boolean updated = glo.maybeUpdate (gl, robj);
      updated |= ibo.maybeUpdate (gl, elements);
      return updated;
   }

   public void drawVertices (GL3 gl, int mode) {
      bind (gl);
      glo.drawVertices(gl, mode);
      unbind(gl);
   }
   
   public void drawElements (GL3 gl, int mode) {
      drawElements (gl, mode, 0, ibo.count ());
   }
   
   public void drawElements (GL3 gl, int mode, int start, int count) {
      bind(gl);
      glo.drawElements (gl, mode, count, ibo.type (), start*ibo.stride());
      unbind(gl);
   }
   
   public static GL3RenderObjectElements generate(GL3 gl, 
      GL3SharedRenderObjectPrimitives glo, GL3SharedVertexIndexArray via) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      GL3RenderObjectElements out = new GL3RenderObjectElements (vao, via, glo);
      return out;
   }
}
