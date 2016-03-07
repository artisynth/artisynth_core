/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Vector3d;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.Shading;

public class PolygonalMeshRenderer extends MeshRendererBase {

   // Use to determine if/when the render object needs to be rebuilt
   protected class PolygonalRobSignature extends RobSignature {
      Shading shading;
      boolean drawEdges;
      boolean drawFaces;
      
      public PolygonalRobSignature (
         PolygonalMesh mesh, RenderProps props) {
         
         super (mesh, props);
         this.shading = props.getShading();
         this.drawEdges = props.getDrawEdges();
         this.drawFaces = (props.getFaceStyle() != Renderer.FaceStyle.NONE);
      }

      public boolean equals (RobSignature other) {
         if (other instanceof PolygonalRobSignature) {
            PolygonalRobSignature pother = (PolygonalRobSignature)other;
            return (super.equals (pother) &&
                    pother.shading == shading &&
                    pother.drawEdges == drawEdges &&
                    pother.drawFaces == drawFaces);
         }
         else {
            return false;
         }
      }
   }

   public PolygonalMeshRenderer() {
   }
   
   protected RobSignature createSignature (
      MeshBase mesh, RenderProps props) {
      return new PolygonalRobSignature ((PolygonalMesh)mesh, props);
   }

   protected void updateFaceNormals (PolygonalMesh mesh) {
      if (mesh.isRenderBuffered() && !mesh.isFixed()) {
         mesh.updateRenderNormals();
      }
      else {
         mesh.updateFaceNormals();
      }
   }

   protected void addFaceNormals (RenderObject r, PolygonalMesh mesh) {
      boolean useRenderData = mesh.isRenderBuffered() && !mesh.isFixed();
      updateFaceNormals (mesh);
      ArrayList<Face> faces = mesh.getFaces();
      for (int i=0; i<faces.size(); i++) {
         Vector3d nrm;
         if (useRenderData) {
            nrm = faces.get(i).getRenderNormal();
         }
         else {
            nrm = faces.get(i).getNormal();
         }
         r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
      }
      if (!mesh.isFixed()) {
         r.setNormalsDynamic (true);
      }
   }  

   protected void updateFaceNormals (RenderObject r, PolygonalMesh mesh) {
      boolean useRenderData = mesh.isRenderBuffered() && !mesh.isFixed();
      updateFaceNormals (mesh);
      ArrayList<Face> faces = mesh.getFaces();
      for (int i=0; i<faces.size(); i++) {
         Vector3d nrm;
         if (useRenderData) {
            nrm = faces.get(i).getRenderNormal();
         }
         else {
            nrm = faces.get(i).getNormal();
         }
         r.setNormal (i, (float)nrm.x, (float)nrm.y, (float)nrm.z);
      }
   }

   public void buildRenderObject (MeshBase mesh, RenderProps props) {

      PolygonalMesh pmesh = (PolygonalMesh)mesh;
      boolean useVertexNormals = props.getShading() != Shading.FLAT;

      RenderObject r = new RenderObject();
      addPositions (r, pmesh);
      if (useVertexNormals) {
         addNormals (r, pmesh);
      }
      else {
         addFaceNormals (r, pmesh);
      }
      addColors (r, pmesh);
      // Texture coordinates currently break GL3 rendering:
      //addTextureCoords (r, pmesh);     

      int[] nidxs = useVertexNormals ? pmesh.getNormalIndices() : null;
      int[] cidxs = pmesh.hasColors() ? pmesh.getColorIndices() : null;
      //int[] tidxs = pmesh.hasTextureCoords() ? pmesh.getTextureIndices() : null;
      int[] tidxs = null; // Texture coordinates currently break GL3 rendering
      
      int[] indexOffs = pmesh.getFeatureIndexOffsets();      
      int[] pidxs = pmesh.createVertexIndices();

      // FINISH Merge Quad Triangles
      Shading shadingModel = props.getShading();
      boolean mergeQuadTriangles = (shadingModel != Shading.FLAT);      

      ArrayList<Face> faces = pmesh.getFaces();
      for (int i=0; i<faces.size(); i++) {
         Face f = faces.get(i);
         int foff = indexOffs[i];
         int numv = indexOffs[i+1] - foff;

         int[] vidxs = new int[numv]; 
         for (int j=0; j<numv; j++) {
            vidxs[j] = r.addVertex(
               pidxs[foff + j],
               nidxs != null ? nidxs[foff + j] : i,
               cidxs != null ? cidxs[foff + j] : -1,
               tidxs != null ? tidxs[foff + j] : -1);
         }
         // triangle fan for faces, line loop for edges
         r.addTriangleFan(vidxs);
         if (props.getDrawEdges()) {
            r.addLineLoop(vidxs);
         }
      } 
      r.commit();
      myRob = r;
   }

   public void updateRenderObject (MeshBase mesh, RenderProps props) {

      PolygonalMesh pmesh = (PolygonalMesh)mesh;

      boolean useRenderData = pmesh.isRenderBuffered() && !pmesh.isFixed();
      boolean useVertexNormals = props.getShading() != Shading.FLAT;

      RenderObject r = myRob;

      if (!pmesh.isFixed()) {
         updatePositions (r, pmesh);
         if (!useVertexNormals) {
            if (pmesh.isRenderBuffered() && !pmesh.isFixed()) {
               pmesh.updateRenderNormals();
            }
            else {
               pmesh.updateFaceNormals();
            }
            updateFaceNormals (r, pmesh);
         }
         else {
            updateNormals (r, pmesh);
         }
      }
      if (!pmesh.isColorsFixed()) {
         updateColors (r, pmesh);
      }
   }

   public void prerender (PolygonalMesh mesh, RenderProps props) {
      super.prerender (mesh, props);
   }

   private boolean colorsEqual (float[] c1, float[] c2) {
      return (c1[0] == c2[0] &&
              c1[1] == c2[1] &&
              c1[2] == c2[2]);
   }

   private float[] getEffectiveEdgeColor (RenderProps props) {
      float[] color = props.getEdgeColorArray();
      if (color == null) {
         color = props.getLineColorArray();
      }
      return color;
   }

//   private Material getEffectiveEdgeMaterial (RenderProps props) {
//      Material mat = props.getEdgeMaterial();
//      if (mat == null) {
//         mat = props.getLineMaterial();
//      }
//      return mat;
//   }

   /**
    * Draws the edges associated with this mesh. Edge drawing is done using
    * edgeWidth and edgeColor (or LineColor if edgeColor is undefined), according
    * to the following rules:
    *
    * <p>If faces <i>are not</i> also being drawn and there <i>is no</i> vertex
    * coloring, then edges should be rendered with whatever shading is
    * selected, and should highlight when selected;
    *
    * <p>If faces <i>are</i> also being drawn and there <i>is no</i> vertex
    * coloring, then (a) edges should not highlight when selected (the faces
    * will instead), and (b) edges should be rendered with whatever shading is
    * selected, <i>unless</i> the edge color is the same as the face color, in
    * which case shading is turned off so that the edges can be seen.
    *
    * <p>If faces <i>are not</i> also being drawn and there <i>is</i> vertex
    * coloring, then edges should render using the vertex coloring, with
    * whatever shading is selected, and not highlight when selected.
    *
    * <p>If faces <i>are</i> also being drawn and there <i>is</i> vertex
    * coloring, then edges should render using the edge color, with whatever
    * shading is selected, and should highlight when selected.
    */
   private void drawEdges (
      Renderer renderer, PolygonalMesh mesh,
      RenderProps props, int flags, boolean drawingFaces) {

      boolean useHSV =
         mesh.hasColors() && ((flags & Renderer.HSV_COLOR_INTERPOLATION) != 0);

      boolean savedLighting = renderer.isLightingEnabled ();
      float savedLineWidth = renderer.getLineWidth();
      Shading savedShadeModel = renderer.getShading();

      boolean selected = ((flags & Renderer.SELECTED) != 0);
      boolean disableColors = false;

      renderer.setLineWidth (props.getEdgeWidth());

      Shading shading = props.getShading();
      float[] edgeColor = getEffectiveEdgeColor(props);

      if (mesh.hasColors()) {
         if (drawingFaces) {
            disableColors = true;
         }
         else {
            // or do we want to disable colors if selected?
            shading = Shading.GOURAUD;
            // smooth shading is needed to get line colors to interpolate
            renderer.setLightingEnabled (false);
         }
      }
      else {
         if (drawingFaces) {
            selected = false;
            if (colorsEqual (edgeColor, props.getFaceColorArray())) {
               // turn off shading so we can see edges
               shading = Shading.NONE;
            }
         }
      }
      
      if (!mesh.hasColors() || disableColors) {
         if (shading == Shading.NONE ||
             (!mesh.hasNormals() && shading != Shading.FLAT)) {
            renderer.setLightingEnabled (false);
            renderer.setColor (edgeColor, selected);
         }
         else {
            renderer.setEdgeColoring (props, selected);
            renderer.setShading (shading);
         }
      }
      else {
         renderer.setShading (shading);
      }

      if (useHSV) {
         //renderer.setColorInterpolation (ColorInterpolation.HSV);
      }
      if (disableColors) {
         //FINISH - implement
      }
      renderer.drawLines (myRob);
      if (useHSV) {
         renderer.setColorInterpolation (ColorInterpolation.RGB);
      }

      renderer.setLineWidth (savedLineWidth);
      renderer.setShading (savedShadeModel);
      renderer.setLightingEnabled (savedLighting);
   }

//   private void setFaceMaterialAndShading (
//      Renderer renderer, PolygonalMesh mesh, RenderProps props, boolean selected) {
//      
//      Shading shading = props.getShading();
//      if (shading != Shading.NONE) {
//         Material faceMat = mesh.getFaceMaterial();
//         if (faceMat == null) {
//            faceMat = props.getFaceMaterial();
//         }
//         Material backMat = mesh.getBackMaterial();
//         if (backMat == null) {
//            backMat = props.getBackMaterial();
//         }
//         if (backMat == null) {
//            backMat = faceMat;
//         }
//         renderer.setMaterialAndShading (
//            props, faceMat, null, backMat, null, selected);
//      }     
//   }      

   private void drawFaces (
      Renderer renderer, PolygonalMesh mesh, RenderProps props, int flags) {
      
      boolean useHSV =
         mesh.hasColors() && ((flags & Renderer.HSV_COLOR_INTERPOLATION) != 0);

      boolean selected = ((flags & Renderer.SELECTED) != 0);

      boolean savedLighting = renderer.isLightingEnabled ();
      Renderer.FaceStyle savedFaceStyle = renderer.getFaceStyle();
      Shading savedShadeModel = renderer.getShading();

      Shading shading = props.getShading();
      if (mesh.hasColors()) {
         // smooth shading is needed to get line colors to interpolate
         // XXX renderer.setShadeModel (Shading.PHONG);
         if (shading == Shading.NONE) {
            renderer.setLightingEnabled (false);
         } else {
            renderer.setShading (shading);
            renderer.setFaceColoring (props, selected);
         }
      }
      else {
         if (shading == Shading.NONE ||
             (!mesh.hasNormals() && shading != Shading.FLAT)) {
            renderer.setLightingEnabled (false);
            renderer.setFaceColoring (props, selected);
         }
         else {
            renderer.setShading (shading);
            renderer.setFaceColoring (props, selected);
         }
      }

      renderer.setFaceStyle (props.getFaceStyle());

      //int i = 0; // i is index of face
      if (useHSV) {
         renderer.setColorInterpolation (ColorInterpolation.HSV);
      }


      if (props.getDrawEdges()) { 
         // FINISH: add setPolygonalOffset() to renderer
         ////gl.glEnable (GL2.GL_POLYGON_OFFSET_FILL);
         ////gl.glPolygonOffset (1f, 1f);
      }
      renderer.drawTriangles (myRob);
      if (props.getDrawEdges()) {
         // FINISH: add setPolygonalOffset() to renderer
         //rendererer.setPolygonalOffset (0f);
         ////gl.glDisable (GL2.GL_POLYGON_OFFSET_FILL);
      }
      
      if (useHSV) {
         // turn off special HSV interpolating shader
         renderer.setColorInterpolation (ColorInterpolation.RGB);
      }
      renderer.setFaceStyle (savedFaceStyle);
      renderer.setShading (savedShadeModel);
      renderer.setLightingEnabled (savedLighting);
   }

   public void renderEdges (
      Renderer renderer, PolygonalMesh mesh, RenderProps props, int flags) {
      
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

      drawEdges (renderer, mesh, props, flags, false);

      renderer.popModelMatrix();
   }
   
   public void render (
      Renderer renderer, PolygonalMesh mesh, RenderProps props,
      int flags) {
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

      boolean translucent = false;
      // boolean saveTransparencyEnabled = false;

      Shading shading = props.getShading();

      boolean drawFaces = (props.getFaceStyle() != Renderer.FaceStyle.NONE);

      if (props.getDrawEdges()) {
         drawEdges (renderer, mesh, props, flags, drawFaces);
      }
      
      if (drawFaces) {
         drawFaces (renderer, mesh, props, flags);
      }

      // if (!renderer.isSelecting()) {
      //    // FINISH: add setLightModel() to renderer
      //    if (props.getBackMaterial() != null) {
      //       //gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1f);
      //    }
      //    if (translucent) {
      //       //renderer.setTransparencyEnabled (saveTransparencyEnabled);
      //    }
      // }
      
      renderer.popModelMatrix();
   }
   
   
   public boolean isTranslucent (RenderProps props) {
      return props.getAlpha() < 1.0;
      // return (myFrontMaterial.isTranslucent() ||
      // myBackMaterial.isTranslucent());
   }

}
