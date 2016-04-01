package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

import maspack.render.RenderObject;

/**
 * VAO-based object associated with a RenderObject, CANNOT be shared between multiple contexts
 */
public class GL3RenderObjectPoints extends GL3ResourceBase implements GL3Drawable {
   
   VertexArrayObject vao;
   GL3SharedRenderObjectPoints pointGLO;
   int lastVertexBindVersion;
   GL3PointsVertexBuffer pointVBO;
   
   private static class InstanceInfo {
      VertexArrayObject vao;
      GL3SharedObject lastInstance;
      int lastInstanceBindVersion;

      public InstanceInfo(VertexArrayObject vao) {
         this.vao = vao.acquire ();
         this.lastInstance = null;
         lastInstanceBindVersion = -1;
      }
      
      public void bind (
         GL3 gl, GL3SharedObject point) {
         vao.bind (gl);
         
         // instance
         if (lastInstance != point) {
            if (lastInstance != null) {
               lastInstance.releaseDispose (gl);
            }
            lastInstance = point.acquire ();
            lastInstance.bindAttributes (gl);
         }
         
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
      
      public static InstanceInfo generate(GL3 gl, int gidx) {
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
    * @param gl
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
            instances[i] = InstanceInfo.generate (gl, i);
         }
      }
   }
   
   public void bindInstanced(GL3 gl, GL3SharedObject point, int gidx) {
      // check if correct instances
      maybeUpdateInstances(gl);
      instances[gidx].bind (gl, point);
      
      // extra instance attributes
      int bv = pointGLO.getBindVersion ();
      if (bv != instances[gidx].lastInstanceBindVersion) {
         pointGLO.bindInstancedVertices (gl, gidx);
         instances[gidx].lastInstanceBindVersion = bv;
      }
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
   }
   
   public void drawInstancedPointGroup(GL3 gl, GL3SharedObject point, int gidx) {
      bindInstanced (gl, point, gidx);
      pointGLO.drawInstancedPoints (gl, point, gidx);
   }
   
   public void drawInstancedPointGroup(GL3 gl, GL3Object point, int gidx) {
      drawInstancedPointGroup (gl, point.getShared (), gidx);
   }

   public static GL3RenderObjectPoints generate (
      GL3 gl, GL3PointsVertexBuffer pointVBO,
      GL3SharedRenderObjectPoints points) {
      
      VertexArrayObject vao = VertexArrayObject.generate (gl);
      return new GL3RenderObjectPoints (vao, pointVBO, points);
   }

}
