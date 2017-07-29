package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;

/**
 * VAO-based object associated with a RenderObject, CANNOT be shared between multiple contexts
 */
public class GL3RenderObjectLines extends GL3ResourceBase {
   
   VertexArrayObject vao;
   GL3SharedRenderObjectLines lineGLO;
   int lastVertexBindVersion;
   GL3LinesVertexBuffer lineVBO;
   
   private static class InstanceInfo {
      VertexArrayObject vao;
      GL3SharedObject lastInstance;
      int lastInstanceBindVersion;
      int lastOffset;

      public InstanceInfo(VertexArrayObject vao) {
         this.vao = vao.acquire ();
         this.lastInstance = null;
         lastInstanceBindVersion = -1;
         lastOffset = -1;
      }
      
      public boolean bind (GL3 gl, GL3SharedObject line) {
         vao.bind (gl);
         
         // instance
         if (lastInstance != line) {
            if (lastInstance != null) {
               lastInstance.releaseDispose (gl);
            }
            lastInstance = line.acquire ();
            lastInstance.bindAttributes (gl);
            return true;
         }
         return false;
      }
      
      public void unbind(GL3 gl) {
         vao.unbind (gl);
      }
      
      public void dispose(GL3 gl) {
         if (vao != null) {
            vao.releaseDispose (gl);
            vao = null;
         }
         if (lastInstance != null) {
            lastInstance.releaseDispose (gl);
            lastInstance = null;
         }
      }
      
      public static InstanceInfo generate(GL3 gl) {
         VertexArrayObject vao = VertexArrayObject.generate (gl);
         return new InstanceInfo (vao);
      }

   }
   
   InstanceInfo[] instances;
   
   private GL3RenderObjectLines(VertexArrayObject vao,
      GL3LinesVertexBuffer lineVBO, 
      GL3SharedRenderObjectLines glo) {
      
      this.lineGLO = glo.acquire ();
      this.vao = vao.acquire ();      
      this.lineVBO = lineVBO.acquire ();
      
      this.instances = null;
      lastVertexBindVersion = -1;
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    */
   public void bind(GL3 gl) {
      // vertex array
      vao.bind (gl);
      int bv = lineGLO.getBindVersion ();
      if (lastVertexBindVersion != bv) {
         lineGLO.bindVertices (gl);
         lastVertexBindVersion = bv;
      }
   }
   
   public void unbind(GL3 gl) {
      vao.unbind (gl);
   }
   
   private void clearInstances(GL3 gl) {
      if (instances != null) {
         for (InstanceInfo ii : instances) {
            ii.dispose (gl);
         }
         instances = null;
      }
   }
   
   private void maybeUpdateInstances(GL3 gl) {
      int ngroups = lineGLO.numLineGroups ();
      if (instances == null || instances.length != ngroups) {
         clearInstances (gl);
         instances = new InstanceInfo[ngroups];
         for (int i=0; i<ngroups; ++i) {
            instances[i] = InstanceInfo.generate (gl);
         }
      }
   }
   
   public void bindInstanced(GL3 gl, GL3SharedObject line, int gidx, int offset) {
      // check if correct instances
      maybeUpdateInstances(gl);
      instances[gidx].bind (gl, line);
      
      // extra instance attributes
      int bv = lineGLO.getBindVersion ();
      if (bv != instances[gidx].lastInstanceBindVersion 
         || offset != instances[gidx].lastOffset) {
         lineGLO.bindInstancedVertices (gl, gidx, offset);
         lineVBO.bind (gl, lineGLO.numLines(gidx));
         instances[gidx].lastInstanceBindVersion = bv;
         instances[gidx].lastOffset = offset;
      }
      
   }
   
   public void unbindInstanced(GL3 gl, int gidx) {
      instances[gidx].unbind(gl);
   }
   
   @Override
   public void dispose (GL3 gl) {
      if (vao != null) {
         vao.releaseDispose (gl);
         vao = null;
      }
      
      if (lineGLO != null) {
         lineGLO.releaseDispose (gl);
         lineGLO = null;
      }
      
      if (lineVBO != null) {
         lineVBO.releaseDispose (gl);
         lineVBO = null;
      }
      
      clearInstances (gl);
   }
   
   @Override
   public boolean isDisposed () {
      return vao == null;
   }
   
   @Override
   public boolean isValid () {
      if (vao == null) {
         return false;
      }
      if (!vao.isValid ()) {
         return false;
      }
      if (!lineGLO.isValid ()) {
         return false;
      }
      if (!lineVBO.isValid()) {
         return false;
      }
      
      return true;
   }
   
   @Override
   public GL3RenderObjectLines acquire () {
      return (GL3RenderObjectLines)super.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      boolean modified = lineGLO.maybeUpdate (gl, robj);
      return modified;
   }
   
   public void setRadius(GL3 gl, float r) {
      lineVBO.maybeUpdate(gl, r, null, null);
   }
   
   public void setRadiusOffsets(GL3 gl, float r, float[] bottomScaleOffset, float[] topScaleOffset) {
      lineVBO.maybeUpdate(gl, r, bottomScaleOffset, topScaleOffset);
   }

   public void drawLineGroup (GL3 gl, int mode, int gidx) {
      bind (gl);
      lineGLO.drawLines (gl, mode, gidx);
      unbind(gl);
   }
   
   public void drawLineGroup (GL3 gl, int mode, int gidx, int offset, int count) {
      bind (gl);
      lineGLO.drawLines (gl, mode, gidx, offset, count);
      unbind(gl);
   }
   
   public void drawInstancedLineGroup(GL3 gl, GL3SharedObject line, int gidx) {
      bindInstanced (gl, line, gidx, 0);
      lineGLO.drawInstancedLines (gl, line, gidx);
      unbindInstanced(gl, gidx);
   }
   
   public void drawInstancedLineGroup(GL3 gl, GL3SharedObject line, int gidx, 
      int offset, int count) {
      bindInstanced (gl, line, gidx, offset);
      lineGLO.drawInstancedLines (gl, line, gidx, count);
      unbindInstanced(gl, gidx);
   }
   
   public void drawInstancedLineGroup(GL3 gl, GL3Object line, int gidx) {
      GL3SharedObject sline = line.getShared();
      drawInstancedLineGroup(gl, sline, gidx);
   }
   
   public void drawInstancedLineGroup(GL3 gl, GL3Object line, int gidx, int offset, int count) {
      GL3SharedObject sline = line.getShared();
      drawInstancedLineGroup(gl, sline, gidx, offset, count);
   }

   public static GL3RenderObjectLines generate (
      GL3 gl, GL3LinesVertexBuffer lineVBO, GL3SharedRenderObjectLines lines) {
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      return new GL3RenderObjectLines (vao, lineVBO, lines);
   }
   
}
