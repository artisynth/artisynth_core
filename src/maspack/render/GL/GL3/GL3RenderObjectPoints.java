package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.RenderObject;

/**
 * VAO-based object associated with a RenderObject, CANNOT be shared between multiple contexts
 */
public class GL3RenderObjectPoints extends GL3ResourceBase {
   
   VertexArrayObject vao;
   GL3SharedRenderObjectPoints pointGLO;
   int lastVertexBindVersion;
   GL3PointsVertexBuffer pointVBO;
   
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
      
      public boolean bind (GL3 gl, GL3SharedObject point) {
         vao.bind (gl);
         // instance
         if (lastInstance != point) {
            if (lastInstance != null) {
               lastInstance.releaseDispose (gl);
            }
            lastInstance = point.acquire ();
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
   
   private GL3RenderObjectPoints(VertexArrayObject vao,
      GL3PointsVertexBuffer pointVBO, 
      GL3SharedRenderObjectPoints glo) {
      
      this.pointGLO = glo.acquire ();
      this.vao = vao.acquire ();      
      this.pointVBO = pointVBO.acquire ();
      
      this.instances = null;
      lastVertexBindVersion = -1;
   }
   
   /**
    * Bind attributes to the VAO to prepare for drawing
    */
   public void bind(GL3 gl) {
      // vertex array
      vao.bind (gl);
      int bv = pointGLO.getBindVersion ();
      if (lastVertexBindVersion != bv) {
         pointGLO.bindVertices (gl);
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
      int ngroups = pointGLO.numPointGroups ();
      if (instances == null || instances.length != ngroups) {
         clearInstances (gl);
         instances = new InstanceInfo[ngroups];
         for (int i=0; i<ngroups; ++i) {
            instances[i] = InstanceInfo.generate (gl);
         }
      }
   }
   
   public void bindInstanced(GL3 gl, GL3SharedObject point, int gidx, int offset) {
      // check if correct instances
      maybeUpdateInstances(gl);
      instances[gidx].bind (gl, point);

      // extra instance attributes
      int bv = pointGLO.getBindVersion ();
      if (bv != instances[gidx].lastInstanceBindVersion || offset != instances[gidx].lastOffset) {
         pointGLO.bindInstancedVertices (gl, gidx, offset);
         pointVBO.bind (gl, pointGLO.numPoints (gidx));
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
      
      if (pointGLO != null) {
         pointGLO.releaseDispose (gl);
         pointGLO = null;
      }
      
      if (pointVBO != null) {
         pointVBO.releaseDispose (gl);
         pointVBO = null;
      }
      
      clearInstances (gl);
   }
   
   @Override
   public boolean isDisposed () {
      return (vao == null);
   }
   
   @Override
   public boolean isValid () {
      if (vao == null) {
         return false;
      }
      if (!vao.isValid ()) {
         return false;
      }
      if (!pointGLO.isValid ()) {
         return false;
      }
      if (!pointVBO.isValid()) {
         return false;
      }
      
      return true;
   }
   
   @Override
   public GL3RenderObjectPoints acquire () {
      return (GL3RenderObjectPoints)super.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderObject robj) {
      boolean modified = pointGLO.maybeUpdate (gl, robj);
      return modified;
   }

   public void setRadius (GL3 gl, float rad) {
      pointVBO.maybeUpdate (gl, rad);
   }
   
   public void drawPointGroup (GL3 gl, int mode, int gidx) {
      bind (gl);
      pointGLO.drawPoints (gl, mode, gidx);
      unbind(gl);
   }
   
   public void drawPointGroup (GL3 gl, int mode, int gidx, int offset, int count) {
      bind (gl);
      pointGLO.drawPoints (gl, mode, gidx, offset, count);
      unbind(gl);
   }
   
   public void drawInstancedPointGroup(GL3 gl, GL3SharedObject point, int gidx) {
      bindInstanced (gl, point, gidx, 0);
      pointGLO.drawInstancedPoints (gl, point, gidx);
      unbindInstanced(gl, gidx);
   }
   
   public void drawInstancedPointGroup(GL3 gl, GL3SharedObject point, int gidx, int offset, int count) {
      bindInstanced (gl, point, gidx, offset);
      pointGLO.drawInstancedPoints (gl, point, gidx, count);
      unbindInstanced(gl, gidx);
   }
   
   public void drawInstancedPointGroup(GL3 gl, GL3Object point, int gidx) {
      drawInstancedPointGroup (gl, point.getShared (), gidx);
   }
   
   public void drawInstancedPointGroup(GL3 gl, GL3Object point, int gidx, int offset, int count) {
      drawInstancedPointGroup (gl, point.getShared (), gidx, offset, count);
   }

   public static GL3RenderObjectPoints generate (
      GL3 gl, GL3PointsVertexBuffer pointVBO,
      GL3SharedRenderObjectPoints points) {
      
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      return new GL3RenderObjectPoints (vao, pointVBO, points);
   }

}
