/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Material;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Shading;
import maspack.render.RenderProps.PointStyle;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.DiffuseTextureProps;
import maspack.render.GL.GLRenderer;
import maspack.util.FunctionTimer;

public class PointMeshRenderer extends MeshRendererBase {

   // with and without normals
   // with and without colors
   // with and without shading
   // change pointStyle, pointColor, pointRadius

   // Use to determine if/when the render object needs to be rebuilt
   protected class PointRobSignature extends RobSignature {
      double normalLen;
      
      public PointRobSignature (
         PointMesh mesh, RenderProps props) {
         
         super (mesh, props);
         this.normalLen = mesh.getNormalRenderLen();
      }

      public boolean equals (RobSignature other) {
         if (other instanceof PointRobSignature) {
            PointRobSignature pother = (PointRobSignature)other;
            return (super.equals (pother) &&
                    pother.normalLen == normalLen);
         }
         else {
            return false;
         }
      }
   }

   public PointMeshRenderer() {
   }

   protected RobSignature createSignature (
      MeshBase mesh, RenderProps props) {
      return new PointRobSignature ((PointMesh)mesh, props);
   }

   public void buildRenderObject (MeshBase mesh, RenderProps props) {
      super.buildRenderObject (mesh, props);
      PointMesh pmesh = (PointMesh)mesh;

      int[] nidxs = pmesh.hasNormals() ? pmesh.getNormalIndices() : null;
      int[] cidxs = pmesh.hasColors() ? pmesh.getColorIndices() : null;

      RenderObject r = myRob;
      int numv = pmesh.numVertices();
      for (int i=0; i<numv; i++) {
         r.addVertex (
            i,
            nidxs != null ? nidxs[i] : -1,
            cidxs != null ? cidxs[i] : -1,
            -1);
         r.addPoint (i);            
      }
      Point3d tip = new Point3d();
      double normalLen = pmesh.getNormalRenderLen();
      if (normalLen > 0 && pmesh.hasNormals()) {
         ArrayList<Vector3d> nrmls = pmesh.getNormals();
         for (int i=0; i<numv; i++) {
            Point3d pnt = pmesh.getVertex(i).pnt;
            tip.scaledAdd (normalLen, nrmls.get(nidxs[i]), pnt);
            r.addPosition ((float)tip.x, (float)tip.y, (float)tip.z);
            r.addVertex (numv+i, nidxs[i], -1, -1);
            r.addLine (i, numv+i);
         }
      }
      r.commit();
   }

   public void updateRenderObject (MeshBase mesh, RenderProps props) {
      super.updateRenderObject (mesh, props);
   }

   public void prerender (PointMesh mesh, RenderProps props) {
      super.prerender (mesh, props);
   }

   public void render (
      Renderer renderer, PointMesh mesh, RenderProps props, int flags) {

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
      Shading savedShadeModel = renderer.getShadeModel();
      boolean savedLightingEnabled = renderer.isLightingEnabled();

      Shading shading = props.getShading();

      if (renderer.isSelecting()) {
         shading = Shading.NONE;
      }

      boolean selected = ((flags & Renderer.SELECTED) != 0);

      switch (props.getPointStyle()) {
         case POINT: {
            int size = props.getPointSize();
            if (size > 0) {
               if (!mesh.hasNormals()) {
                  renderer.setLightingEnabled (false);               
                  renderer.setColor (props.getPointColorArray(), selected);
               }
               else {
                  renderer.setPointLighting (props, selected);
               }
               renderer.drawPoints (myRob, PointStyle.POINT, size);
               if (!mesh.hasNormals()) {
                  renderer.setLightingEnabled (true);
               }
            }
            break;
         }
         case SPHERE: {
            double rad = props.getPointRadius();
            if (rad > 0) {
               renderer.setPointLighting (props, selected);
               renderer.drawPoints (myRob, PointStyle.SPHERE, rad);
            }
            break;
         }
      }

      if (mesh.getNormalRenderLen() > 0) {
         renderer.setLineWidth (props.getLineWidth());
         if (shading == Shading.NONE) {
            renderer.setColor (props.getLineColorArray(), selected);
         }
         else {
            renderer.setMaterial (props.getLineMaterial(), selected);
         }
         renderer.drawLines (myRob);
      }
      
      renderer.setLineWidth (savedLineWidth);
      renderer.setShadeModel (savedShadeModel);
      renderer.setLightingEnabled (savedLightingEnabled);

      renderer.popModelMatrix();
   }
}
