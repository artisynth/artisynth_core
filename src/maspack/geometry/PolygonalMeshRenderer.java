/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Vector3d;
import maspack.render.BumpMapProps;
import maspack.render.NormalMapProps;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.ColorMapProps;

/**
 * Utility class for rendering {@link PolygonalMesh} objects.
 */
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

   protected void buildRenderObject (MeshBase mesh, RenderProps props) {

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
      addTextureCoords (r, pmesh);     

      int[] nidxs = useVertexNormals ? pmesh.getNormalIndices() : null;
      int[] cidxs = pmesh.hasColors() ? pmesh.getColorIndices() : null;
      int[] tidxs = pmesh.hasTextureCoords() ? pmesh.getTextureIndices() : null;
      
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
      myRob = r;
   }

   protected void updateRenderObject (MeshBase mesh, RenderProps props) {

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
      float[] color = props.getEdgeColorF();
      if (color == null) {
         color = props.getLineColorF();
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
    * <p>If faces <i>are</i> also being drawn and there <i>is</i> vertex
    * coloring, then edges should render using the edge color, with whatever
    * shading is selected, and should respond to highlighting.
    * 
    * <p>If faces <i>are</i> also being drawn and there <i>is no</i> vertex
    * coloring, then (a) edges should not respond to highlighting (the faces
    * will instead), and (b) edges should be rendered with whatever shading is
    * selected, <i>unless</i> the edge color is the same as the face color, in
    * which case shading is turned off so that the edges can be seen.
    *
    * <p>If faces <i>are not</i> also being drawn and there <i>is</i> vertex
    * coloring, then edges should render using the vertex coloring, with
    * whatever shading is selected, unless the mesh being highlighted, in which
    * case it should they should be rendered with the highlight color.
    *
    * <p>If faces <i>are not</i> also being drawn and there <i>is no</i> vertex
    * coloring, then edges should be rendered with whatever shading is
    * selected, and should respond to highlighting.
    */
   private void drawEdges (
      Renderer renderer, PolygonalMesh mesh,
      RenderProps props, boolean highlight, boolean alsoDrawingFaces) {

      float savedLineWidth = renderer.getLineWidth();
      Shading savedShadeModel = renderer.getShading();

      boolean disableColors = false;

      renderer.setLineWidth (props.getEdgeWidth());

      Shading shading = props.getShading();
      if (!mesh.hasNormals() && shading != Shading.FLAT) {
         shading = Shading.NONE;
      }

      float[] edgeColor = getEffectiveEdgeColor(props);

      if (alsoDrawingFaces) {
         highlight = false;
         if (mesh.hasColors()) {
            disableColors = true;
         }
         else {
            if (colorsEqual (edgeColor, props.getFaceColorF())) {
               // turn off shading so we can see edges
               shading = Shading.NONE;
            }
         }
      }
      else {
         if (mesh.hasColors()) {
            if (highlight) {
               disableColors = true;
            }
         }
      }
      renderer.setEdgeColoring (props, highlight);
      renderer.setShading (shading);

      ColorInterpolation savedColorInterp = null;
      if (usingHSV(mesh)) {
         savedColorInterp =
            renderer.setColorInterpolation (ColorInterpolation.HSV);
      }
      ColorMixing savedColorMixing = null;
      if (disableColors) {
         savedColorMixing = renderer.getVertexColorMixing();
         renderer.setVertexColorMixing (ColorMixing.NONE);
      }
      renderer.drawLines (myRob);
      if (savedColorInterp != null) {
         renderer.setColorInterpolation (savedColorInterp);
      }
      if (disableColors) {
         renderer.setVertexColorMixing (savedColorMixing);
      }
      renderer.setLineWidth (savedLineWidth);
      renderer.setShading (savedShadeModel);
   }

   private void drawFaces (
      Renderer renderer, PolygonalMesh mesh, RenderProps props, 
      boolean highlight) {
      
      boolean useTextures = mesh.hasTextureCoords ();

      Renderer.FaceStyle savedFaceStyle = renderer.getFaceStyle();
      Shading savedShadeModel = renderer.getShading();

      Shading shading = props.getShading();
      if (!mesh.hasNormals() && shading != Shading.FLAT) {
         shading = Shading.NONE;
      }

      renderer.setShading (shading);
      renderer.setFaceColoring (props, highlight);
      renderer.setFaceStyle (props.getFaceStyle());

      //int i = 0; // i is index of face
      ColorInterpolation savedColorInterp = null;
      if (usingHSV(mesh)) {
         savedColorInterp =
            renderer.setColorInterpolation (ColorInterpolation.HSV);
      }

      if (props.getDrawEdges()) { 
         // FINISH: add setPolygonalOffset() to renderer
         ////gl.glEnable (GL2.GL_POLYGON_OFFSET_FILL);
         ////gl.glPolygonOffset (1f, 1f);
      }
      
      ColorMapProps oldtprops = null;
      NormalMapProps oldnprops = null;
      BumpMapProps oldbprops = null;
      if (useTextures) {
         ColorMapProps dtprops = props.getColorMap ();
         oldtprops = renderer.setColorMap(dtprops);
         
         NormalMapProps ntprops = props.getNormalMap ();
         oldnprops = renderer.setNormalMap (ntprops);
         
         BumpMapProps btprops = props.getBumpMap ();
         oldbprops = renderer.setBumpMap (btprops);
      }
      renderer.drawTriangles (myRob);
      if (useTextures) {
         // restore diffuse texture properties
         renderer.setColorMap (oldtprops);
         renderer.setNormalMap (oldnprops);
         renderer.setBumpMap (oldbprops);
      }
      
      if (props.getDrawEdges()) {
         // FINISH: add setPolygonalOffset() to renderer
         //rendererer.setPolygonalOffset (0f);
         ////gl.glDisable (GL2.GL_POLYGON_OFFSET_FILL);
      }
      
      if (savedColorInterp != null) {
         renderer.setColorInterpolation (savedColorInterp);
      }
      renderer.setFaceStyle (savedFaceStyle);
      renderer.setShading (savedShadeModel);
   }

   public void renderEdges (
      Renderer renderer, PolygonalMesh mesh, RenderProps props, 
      boolean highlight) {
      
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

      drawEdges (renderer, mesh, props, highlight, false);

      renderer.popModelMatrix();
   }
   
   public void render (
      Renderer renderer, PolygonalMesh mesh, RenderProps props, 
      int flags) {
      
      if (mesh.numVertices() == 0) {
         return;
      }
      // if mesh is transparent, and we are drawing faces, then sort
      // the mesh faces if SORT_FACES is requested:
      if (props.getAlpha() < 1 && 
          props.getFaceStyle() != FaceStyle.NONE && 
          (flags & Renderer.SORT_FACES) != 0) {
         mesh.sortFaces(renderer.getEyeZDirection());
      }
      boolean highlight = ((flags & Renderer.HIGHLIGHT) != 0);
      
      renderer.pushModelMatrix();
      if (mesh.isRenderBuffered()) {
         renderer.mulModelMatrix (mesh.getXMeshToWorldRender());
      }
      else {
         renderer.mulModelMatrix (mesh.XMeshToWorld);
      }

      boolean drawFaces = (props.getFaceStyle() != Renderer.FaceStyle.NONE);

      if (props.getDrawEdges()) {
         drawEdges (renderer, mesh, props, highlight, drawFaces);
      }
      
      if (drawFaces) {
         drawFaces (renderer, mesh, props, highlight);
      }

      renderer.popModelMatrix();
   }
   
   
//   protected boolean isTransparent (RenderProps props) {
//      return props.getAlpha() < 1.0;
//   }

}
