package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

/**
 * Wraps a {@link GL3SharedRenderObjectPrimitives} into a drawable form
 * @author Antonio
 *
 */
public class GL3SharedRenderObjectPrimitivesDrawable extends GL3ResourceBase implements GL3SharedDrawable {
   
   GL3SharedRenderObjectPrimitives glo;
   int pointGroup;
   int lineGroup;
   int triangleGroup;
   
   public GL3SharedRenderObjectPrimitivesDrawable(
      GL3SharedRenderObjectPrimitives glo) {
      this.glo = glo.acquire();
      pointGroup = -1;
      lineGroup = -1;
      triangleGroup = -1;
   }
   
   @Override
   public int getBindVersion() {
      return glo.getBindVersion();
   }

   @Override
   public void bind(GL3 gl) {
      glo.bindVertices(gl);
      glo.bindIndices(gl);
   }

   @Override
   public GL3SharedRenderObjectPrimitivesDrawable acquire() {
      return (GL3SharedRenderObjectPrimitivesDrawable)super.acquire();
   }

   @Override
   public void dispose(GL3 gl) {
      glo.dispose(gl);
      glo = null;
   }

   @Override
   public boolean isDisposed() {
      return (glo == null);
   }

   @Override
   public boolean equals(GL3SharedDrawable other) {
      if (other == null || other.getClass() != this.getClass()) {
         return false;
      }
      
      GL3SharedRenderObjectPrimitivesDrawable oglo = 
         (GL3SharedRenderObjectPrimitivesDrawable)other;
      return oglo.glo == glo;
   }
   
   @Override
   public int hashCode() {
      return glo.hashCode();
   } 
   
   /**
    * Sets draw groups, -1 to deactivate
    * @param pidx point group index
    * @param lidx line group index
    * @param tidx triangle group index
    */
   public void setDrawGroups(int pidx, int lidx, int tidx) {
      pointGroup = pidx;
      lineGroup = lidx;
      triangleGroup = tidx;
   }
   
   @Override
   public void draw(GL3 gl) {
      if (pointGroup >= 0) {
         glo.drawPointGroup(gl, GL3.GL_POINTS, pointGroup);
      }
      if (lineGroup >= 0) {
         glo.drawLineGroup(gl, GL3.GL_LINES, lineGroup);
      }
      if (triangleGroup >= 0) {
         glo.drawTriangleGroup(gl, GL3.GL_TRIANGLES, triangleGroup);
      }
   }

   @Override
   public void drawInstanced(GL3 gl, int instanceCount) {
      if (pointGroup >= 0) {
         glo.drawInstancedPointGroup(gl, GL3.GL_POINTS, lineGroup, instanceCount);
      }
      if (lineGroup >= 0) {
         glo.drawInstancedLineGroup(gl, GL3.GL_LINES, lineGroup, instanceCount);
      }
      if (triangleGroup >= 0) {
         glo.drawInstancedTriangleGroup(gl, GL3.GL_TRIANGLES, triangleGroup, instanceCount);
      }
   }

}
