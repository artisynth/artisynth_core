/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.Shading;

/**
 * Utility class for rendering {@link PolylineMesh} objects.
 */
public class PolylineMeshRenderer  extends MeshRendererBase {


   protected class PolylineRobSignature extends RobSignature {
      
      // Don't need anything extra at the moment ...

      public PolylineRobSignature (
         PolylineMesh mesh, RenderProps props) {
         
         super (mesh, props);
      }

      public boolean equals (RobSignature other) {
         if (other instanceof PolylineRobSignature) {
            PolylineRobSignature pother = (PolylineRobSignature)other;
            return (super.equals (pother));
         }
         else {
            return false;
         }
      }
   }

   public PolylineMeshRenderer() {
   }

   protected RobSignature createSignature (
      MeshBase mesh, RenderProps props) {
      return new PolylineRobSignature ((PolylineMesh)mesh, props);
   }

   protected void buildRenderObject (MeshBase mesh, RenderProps props) {
      super.buildRenderObject (mesh, props);
      PolylineMesh pmesh = (PolylineMesh)mesh;

      int[] nidxs = pmesh.hasNormals() ? pmesh.getNormalIndices() : null;
      int[] cidxs = pmesh.hasColors() ? pmesh.getColorIndices() : null;

      RenderObject r = myRob;

      int[] indexOffs = mesh.getFeatureIndexOffsets();
      int[] pidxs = mesh.createVertexIndices();
      ArrayList<Polyline> lines = pmesh.getLines();
      for (int i=0; i<pmesh.numLines(); i++) {
         Polyline line = lines.get(i);
         int loff = indexOffs[i];
         int numv = indexOffs[i+1] - loff;

         int[] vidxs = new int[numv]; 
         for (int j=0; j<numv; j++) {
            vidxs[j] = r.addVertex(
               pidxs[loff + j],
               nidxs != null ? nidxs[loff + j] : i,
               cidxs != null ? cidxs[loff + j] : -1,
               -1);
         }
         // triangle fan for faces, line loop for edges
         r.addLineStrip(vidxs);
      }
      r.commit();
   }

   protected void updateRenderObject (MeshBase mesh, RenderProps props) {
      super.updateRenderObject (mesh, props);
   }

   public void prerender (PointMesh mesh, RenderProps props) {
      super.prerender (mesh, props);
   }

   public void render (
      Renderer renderer, PolylineMesh mesh, RenderProps props, 
      boolean selected) {

      if (mesh.numVertices() == 0) {
         return;
      }

      renderer.pushModelMatrix();
      if (mesh.isRenderBuffered()) {
         renderer.mulModelMatrix (mesh.getXMeshToWorldRender());
      }
      else {
         renderer.mulModelMatrix (mesh.XMeshToWorld);
      }

      float savedLineWidth = renderer.getLineWidth();
      Shading savedShadeModel = renderer.getShading();

//      Shading shading = props.getShading();
//      if (renderer.isSelecting()) {
//         shading = Shading.NONE;
//      }

      LineStyle lineStyle = props.getLineStyle();
      Shading savedShading = renderer.getShading();
      if (lineStyle == LineStyle.LINE && !mesh.hasNormals()) {
         renderer.setShading (Shading.NONE);
      }
      else {
         renderer.setShading (props.getShading());
      }
      ColorInterpolation savedColorInterp = null;
      if (usingHSV(mesh)) {
         savedColorInterp =
            renderer.setColorInterpolation (ColorInterpolation.HSV);
      }
      renderer.setLineColoring (props, selected);
      switch (lineStyle) {
         case LINE: {
            int width = props.getLineWidth();
            if (width > 0) {
               renderer.drawLines (myRob, LineStyle.LINE, width);
            }
            break;
         }
         case SPINDLE:
         case SOLID_ARROW:
         case CYLINDER: {
            double rad = props.getLineRadius();
            if (rad > 0) {
               renderer.drawLines (myRob, props.getLineStyle(), rad);
            }
            break;
         }
      }
      if (savedColorInterp != null) {
         renderer.setColorInterpolation (savedColorInterp);
      }
      renderer.setShading (savedShading);
      renderer.setLineWidth (savedLineWidth);
      renderer.setShading (savedShadeModel);

      renderer.popModelMatrix();
   }

}
