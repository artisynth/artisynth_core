package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

import maspack.render.RenderInstances;
import maspack.render.RenderInstances.InstanceTransformType;

/**
 * VAO-based object associated with RenderInstances, CANNOT be shared between multiple contexts
 */
public class GL3RenderInstances extends GL3ResourceBase {

   static int INSTANCE_POINT_IDX = 0;
   static int INSTANCE_FRAME_IDX = 1;
   static int INSTANCE_AFFINE_IDX = 2;
   static int INSTANCE_SIZE = 3;
   static InstanceTransformType[] INSTANCE_TYPES = {
      InstanceTransformType.POINT, InstanceTransformType.FRAME, InstanceTransformType.AFFINE};
   
   private static class InstanceInfo {
      VertexArrayObject vao;
      GL3SharedDrawable lastGlo;
      int lastGloBindVersion;
      int lastInstanceBindVersion;

      public InstanceInfo(VertexArrayObject vao) {
         this.vao = vao.acquire ();
         this.lastGlo = null;
         lastInstanceBindVersion = -1;
         lastGloBindVersion = -1;
      }
      
      public boolean bind (GL3 gl, GL3SharedDrawable glo) {
         vao.bind (gl);
         
         // instance
         if (!glo.equals(lastGlo) ) {
            if (lastGlo != null) {
               lastGlo.releaseDispose (gl);
            }
            lastGlo = glo.acquire ();
            lastGloBindVersion = -1;
         } 
         
         int bv = glo.getBindVersion();
         if (bv != lastGloBindVersion) {
            glo.bind (gl);
            lastGloBindVersion = bv;
            return true;
         }
         
         return false;
      }
      
      public void unbind(GL3 gl) {
         vao.unbind (gl);
      }
      
      public boolean isBound() {
         return vao.isBound();
      }
      
      public void dispose(GL3 gl) {
         if (vao != null) {
            vao.releaseDispose (gl);
            vao = null;
         }
         if (lastGlo != null) {
            lastGlo.releaseDispose (gl);
            lastGlo = null;
            lastInstanceBindVersion = -1;
            lastGloBindVersion = -1;
         }
      }
   }
   
   InstanceInfo[] instances;
   GL3SharedRenderInstances glinst;
   
   private GL3RenderInstances(VertexArrayObject[] vao, 
      GL3SharedRenderInstances glinst) {
      
      this.instances = new InstanceInfo[INSTANCE_SIZE];
      for (int i=0; i<instances.length; ++i) {
         instances[i] = new InstanceInfo(vao[i]);
      }
      
      this.glinst = glinst.acquire();
      
   }
   
   private void bind(GL3 gl, GL3SharedDrawable glo, int idx) {
      // check if correct instances
      instances[idx].bind (gl, glo);
      
      // extra instance attributes
      int bv = glinst.getBindVersion ();
      if (bv != instances[idx].lastInstanceBindVersion) {        
         glinst.bindInstanceAttributes(gl, INSTANCE_TYPES[idx]);
         instances[idx].lastInstanceBindVersion = bv;
      }
   }
   
   /**
    * Bind points attributes to the VAO to prepare for drawing
    * @param gl context handle
    */
   public void bindPoints(GL3 gl, GL3SharedDrawable glo) {
      bind(gl, glo, INSTANCE_POINT_IDX);
   }
   
   /**
    * Bind frame attributes to the VAO to prepare for drawing
    * @param gl context handle
    */
   public void bindFrames(GL3 gl, GL3SharedDrawable glo) {
      bind(gl, glo, INSTANCE_FRAME_IDX);
   }
   
   public void bindAffines(GL3 gl, GL3SharedDrawable glo) {
      bind(gl, glo,  INSTANCE_AFFINE_IDX);
   }
   
   /**
    * Bind points attributes to the VAO to prepare for drawing
    * @param gl context handle
    */
   public void unbindPoints(GL3 gl) {
      instances[INSTANCE_POINT_IDX].unbind(gl);
   }
   
   /**
    * Bind frame attributes to the VAO to prepare for drawing
    * @param gl context handle
    */
   public void unbindFrames(GL3 gl) {
      instances[INSTANCE_FRAME_IDX].unbind(gl);
   }
   
   public void unbindAffines(GL3 gl) {
      instances[INSTANCE_AFFINE_IDX].unbind(gl);
   }
   
   public void unbind(GL3 gl) {
      for (InstanceInfo v : instances) {
         if (v.isBound()) {
            v.unbind(gl);
         }
      }
   }
   
   @Override
   public void dispose (GL3 gl) {
      for (InstanceInfo v : instances) {
         v.dispose (gl);
      }
      instances = null;
      
      glinst.releaseDispose(gl);
      glinst = null;
   }
   
   @Override
   public boolean isDisposed () {
      return (instances == null);
   }
   
   @Override
   public boolean isValid () {
      if (instances == null) {
         return false;
      }
      if (!glinst.isValid()) {
         return false;
      }
      return true;
   }
   
   
   @Override
   public GL3RenderInstances acquire () {
      return (GL3RenderInstances)super.acquire ();
   }
   
   public boolean maybeUpdate(GL3 gl, RenderInstances rinst) {
      return glinst.maybeUpdate (gl, rinst);
   }

   boolean hasPoints() {
      return glinst.numPoints() > 0;
   }
   
   boolean hasFrames() {
      return glinst.numFrames() > 0;
   }
   
   boolean hasAffines() {
      return glinst.numAffines() > 0;
   }
   
   public void drawPoints (GL3 gl, GL3SharedDrawable glo) {
      bindPoints (gl, glo);
      glo.drawInstanced(gl, glinst.numPoints());
      unbindPoints(gl);
   }
   
   public void drawFrames (GL3 gl, GL3SharedDrawable glo) {
      bindFrames (gl, glo);
      glo.drawInstanced(gl, glinst.numFrames());
      unbindFrames(gl);
   }
   
   public void drawAffines (GL3 gl, GL3SharedDrawable glo) {
      bindAffines (gl, glo);
      glo.drawInstanced(gl, glinst.numAffines());
      unbindAffines(gl);
   }
   
   /**
    * Draw points, frames and affines (in order)
    * @param gl context handle
    */
   public void draw (GL3 gl, GL3SharedDrawable glo) {
      if (glinst.numPoints() > 0) {
         drawPoints(gl, glo);
      }
      if (glinst.numFrames() > 0) {
         drawFrames(gl, glo);
      }
      if (glinst.numAffines() > 0) {
         drawAffines(gl, glo);
      }
   }
  
   public static GL3RenderInstances generate(GL3 gl, GL3SharedRenderInstances glinst) {
      VertexArrayObject[] vao = new VertexArrayObject[INSTANCE_SIZE];
      for (int i=0; i<vao.length; ++i) {
         vao[i] = VertexArrayObject.generate (gl);
      }
      GL3RenderInstances out = new GL3RenderInstances (vao, glinst);
      return out;
   }
}
