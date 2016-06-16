/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.BumpMapProps;
import maspack.render.ColorMapProps;
import maspack.render.FeatureIndexArray;
import maspack.render.NormalMapProps;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.util.SortUtilitities;

/**
 * Utility class for rendering {@link PolygonalMesh} objects.
 */
public class PolygonalMeshRenderer extends MeshRendererBase {

   // feature lists
   FeatureIndexArray myFaceTriangles;
   FeatureIndexArray myFaceLines;
   int myFacePrimitivesVersion;

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

   // used for sorting faces for transparency
   private static class ZOrderComparator<E extends Face> implements Comparator<E> {

      Vector3d myDir = new Vector3d(0,1,0);
      Vector3d pdisp = new Vector3d();
      Point3d centroid1 = new Point3d();
      Point3d centroid2 = new Point3d();

      public ZOrderComparator(Vector3d zDir) {
         myDir.set(zDir);
      }

      public int compare(Face o1, Face o2) {
         o1.computeCentroid(centroid1);
         o2.computeCentroid(centroid2);
         pdisp.sub(centroid1,centroid2);
         double d = pdisp.dot(myDir);
         if (d > 0) {
            return 1;
         } else if (d < 0) {
            return -1;
         }
         return 0;
      }

   }

   public PolygonalMeshRenderer(PolygonalMesh mesh) {
      super(mesh);
      myFacePrimitivesVersion = -1;
   }

   public PolygonalMesh getMesh() {
      return (PolygonalMesh)super.getMesh ();
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

   @Override
   protected RenderObject buildRenderObject (MeshBase mesh, RenderProps props) {

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
      // Shading shadingModel = props.getShading();
      // boolean mergeQuadTriangles = (shadingModel != Shading.FLAT);  

      // ensure capacity assume all faces are triangles
      r.ensureVertexCapacity (pmesh.getFaces().size()*3);
      ArrayList<Face> faces = pmesh.getFaces();
      for (int i=0; i<faces.size(); i++) {
         // Face f = faces.get(i);   // XXX required?
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
         // XXX currently handled using separate index list
         //         // triangle fan for faces, line loop for edges
         //         r.addTriangleFan(vidxs);
         //         if (props.getDrawEdges()) {
         //            r.addLineLoop(vidxs);
         //         }
      } 
      return r;
   }

   @Override
   protected void updateRenderObject (MeshBase mesh, RenderProps props, RenderObject r) {

      PolygonalMesh pmesh = (PolygonalMesh)mesh;

      boolean useRenderData = pmesh.isRenderBuffered() && !pmesh.isFixed();
      boolean useVertexNormals = props.getShading() != Shading.FLAT;

      if (!pmesh.isFixed()) {
         updatePositions (r, pmesh);
         if (!useVertexNormals) {
            if (useRenderData) {
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
      if (!pmesh.isTextureCoordsFixed ()) {
         updateTextureCoords (r, pmesh);
      }
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
   
   @Override
   public void prerender (RenderProps props) {
      super.prerender (props);

      PolygonalMesh mesh = getMesh();
      if (myFaceTriangles == null ||
          mesh.getVersion () != myFacePrimitivesVersion) {
         
         int[] faceOrder = new int[mesh.numFaces ()];
         for (int i=0; i<faceOrder.length; ++i) {
            faceOrder[i] = i;
         }
         myFaceTriangles = new FeatureIndexArray (faceOrder.length, 3*faceOrder.length);
         myFaceLines = new FeatureIndexArray (faceOrder.length, 6*faceOrder.length);
         updateFaceTriangles (faceOrder, myFaceTriangles);
         updateFaceLines (faceOrder, myFaceLines);
         myFacePrimitivesVersion = mesh.getVersion ();
      }
   }

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
      Renderer renderer, RenderProps props, boolean highlight, 
      boolean alsoDrawingFaces,
      RenderObject robj, 
      FeatureIndexArray features,
      boolean featureSelection) {

      float savedLineWidth = renderer.getLineWidth();
      Shading savedShadeModel = renderer.getShading();

      boolean disableColors = false;

      renderer.setLineWidth (props.getEdgeWidth());

      Shading shading = props.getShading();
      if (!robj.hasNormals() && shading != Shading.FLAT) {
         shading = Shading.NONE;
      }

      float[] edgeColor = getEffectiveEdgeColor(props);

      if (alsoDrawingFaces) {
         highlight = false;
         if (robj.hasColors()) {
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
         if (robj.hasColors()) {
            if (highlight) {
               disableColors = true;
            }
         }
      }
      renderer.setEdgeColoring (props, highlight);
      renderer.setShading (shading);

      ColorInterpolation savedColorInterp = null;
      if (usingHSV(getMesh())) {
         savedColorInterp =
         renderer.setColorInterpolation (ColorInterpolation.HSV);
      }
      ColorMixing savedColorMixing = null;
      if (disableColors) {
         savedColorMixing = renderer.getVertexColorMixing();
         renderer.setVertexColorMixing (ColorMixing.NONE);
      }
      
      if (renderer.isSelecting () && featureSelection) {
         for (int i=0; i<features.numFeatures (); ++i) {
            renderer.beginSelectionQuery (features.getFeature (i));
            renderer.drawVertices (robj, features.getVertices (), 
               features.getFeatureOffset (i), features.getFeatureLength (i),
               DrawMode.LINES);
            renderer.endSelectionQuery ();
         }
      } else {
         renderer.drawVertices (robj, features.getVertices (), DrawMode.LINES);
      }
      
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
      Renderer renderer, RenderProps props, 
      boolean highlight, RenderObject robj,
      FeatureIndexArray features,
      boolean featureSelection) {

      boolean useTextures = robj.hasTextureCoords ();

      Renderer.FaceStyle savedFaceStyle = renderer.getFaceStyle();
      Shading savedShadeModel = renderer.getShading();

      Shading shading = props.getShading();
      if (!robj.hasNormals() && shading != Shading.FLAT) {
         shading = Shading.NONE;
      }

      renderer.setShading (shading);
      renderer.setFaceColoring (props, highlight);

      // XXX always front and back when selecting?
      if (renderer.isSelecting ()) {
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      } else {
         renderer.setFaceStyle (props.getFaceStyle());
      }

      //int i = 0; // i is index of face
      ColorInterpolation savedColorInterp = null;
      if (usingHSV(getMesh())) {
         savedColorInterp =
         renderer.setColorInterpolation (ColorInterpolation.HSV);
      }

      if (props.getDrawEdges()) {
         renderer.setDepthOffset (-1);
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
      
      if (renderer.isSelecting () && featureSelection) {
         for (int i=0; i<features.numFeatures (); ++i) {
            renderer.beginSelectionQuery (features.getFeature (i));
            renderer.drawVertices (robj, features.getVertices (), 
               features.getFeatureOffset (i), features.getFeatureLength (i),
               DrawMode.TRIANGLES);
            renderer.endSelectionQuery ();
         }
      } else {
         renderer.drawVertices (robj, features.getVertices (), DrawMode.TRIANGLES);
      }
      
      if (useTextures) {
         // restore diffuse texture properties
         renderer.setColorMap (oldtprops);
         renderer.setNormalMap (oldnprops);
         renderer.setBumpMap (oldbprops);
      }

      if (props.getDrawEdges()) {
         renderer.setDepthOffset (0);
      }

      if (savedColorInterp != null) {
         renderer.setColorInterpolation (savedColorInterp);
      }
      renderer.setFaceStyle (savedFaceStyle);
      renderer.setShading (savedShadeModel);
   }

   /**
    * "Sorts" faces according to the direction provided.  Note that the order of
    * faces is not actually changed.  Instead, an index array is created that
    * holds the sorted order.
    */
   public<E extends Face> int[] sortFaces(List<E> faces, Vector3d zdir) {
      ZOrderComparator<E> zComparator = new ZOrderComparator<E>(zdir);
      int[] faceIndices = SortUtilitities.sortIndices (faces, zComparator);
      return faceIndices;
   }
  
   /**
    * "Sorts" faces according to the direction provided.  Note that the order of
    * faces is not actually changed.  Instead, an index array is created that
    * holds the sorted order.
    */
   public void sortFaces(int[] faceIdxs, int start, int count, Vector3d zdir) {
      ZOrderComparator<Face> zComparator = new ZOrderComparator<Face>(zdir);
      ArrayList<Face> faces = getMesh ().getFaces ();
      SortUtilitities.sortIndices (faceIdxs, start, count, faces, zComparator);
   }
   
   public FeatureIndexArray getFaceTriangles(int[] faceIdxs) {
      FeatureIndexArray fia = new FeatureIndexArray (faceIdxs.length, 3*faceIdxs.length);
      updateFaceTriangles(faceIdxs, fia);
      return fia;
   }
   
   /**
    * Updates a list of features
    * @param faceIdxs face indices
    * @param features feature indices
    * @return whether or not the feature list has been modified
    */
   public boolean updateFaceTriangles(int[] faceIdxs, FeatureIndexArray features) {
      return updateFaceTriangles (faceIdxs, 0, faceIdxs.length, features);
   }
    
   public boolean updateFaceTriangles(int[] faceIdxs, int offset, 
      int len, FeatureIndexArray features) {
      
      int nFaces = len;
      int nFeatures = features.numFeatures ();
      
      PolygonalMesh mesh = getMesh ();
      boolean modified = false;
      
      // find how many features we can keep
      if (nFeatures > 0) {
         for (int i=0; i<nFaces; ++i) {
            if (i >= nFeatures) {
               break;
            }
            if (faceIdxs[offset+i] != features.getFeature (i)) {
               features.chop (0, i);
               nFeatures = i;
               modified = true;
               break;
            }
         }
      }
      if (nFaces < nFeatures) {
         features.chop (0, nFaces);
         nFeatures = nFaces;
         modified = true;
      }
      
      int[] offsets = mesh.getFeatureIndexOffsets ();
      
      for (int i=nFeatures; i<nFaces; ++i) {
         int faceIdx = faceIdxs[offset+i];
         
         int v0 = offsets[faceIdx];
         int nv = offsets[faceIdx+1]-offsets[faceIdx];
         int v1 = v0+1;
         
         // triangle fan
         features.beginFeature (faceIdx);
         for (int j=2; j<nv; ++j) {
            int v2 = v0+j;
            features.addVertex (v0);
            features.addVertex (v1);
            features.addVertex (v2);
            v1 = v2;
         }
         features.endFeature ();
         modified = true;
      }
      
      return modified;
   }
   
   public FeatureIndexArray getFaceLines(int[] faceIdxs) {
      FeatureIndexArray fia = new FeatureIndexArray (faceIdxs.length, 6*faceIdxs.length);
      updateFaceLines(faceIdxs, fia);
      return fia;
   }
   
   public boolean updateFaceLines(int[] faceIdxs, FeatureIndexArray features) {
      return updateFaceLines (faceIdxs, 0, faceIdxs.length, features);
   }
   
   public boolean updateFaceLines(int[] faceIdxs, int offset, int len, FeatureIndexArray features) {
      int nFaces = len;
      int nFeatures = features.numFeatures ();
      
      PolygonalMesh mesh = getMesh ();
      boolean modified = false;
      
      // find how many features we can keep
      if (nFeatures > 0) {
         for (int i=0; i<nFaces; ++i) {
            if (i >= nFeatures) {
               break;
            }
            if (faceIdxs[i+offset] != features.getFeature (i)) {
               features.chop (0, i);
               nFeatures = i;
               modified = true;
               break;
            }
         }
      }
      if (nFaces < nFeatures) {
         features.chop (0, nFaces);
         nFeatures = nFaces;
         modified = true;
      }
      
      int[] offsets = mesh.getFeatureIndexOffsets ();
      
      for (int i=nFeatures; i<nFaces; ++i) {
         int faceIdx = faceIdxs[i+offset];

         int v0 = offsets[faceIdx];
         int nv = offsets[faceIdx+1]-offsets[faceIdx];
         
         // line loop
         features.beginFeature (faceIdx);
         for (int j=1; j<nv; ++j) {
            int v1 = v0+1;
            features.addVertex (v0);
            features.addVertex (v1);
            v0 = v1;
         }
         // close loop
         features.addVertex (v0);
         features.addVertex (offsets[faceIdx]);
         features.endFeature ();
         modified = true;
      }
      
      return modified;
   }
   
   public void renderEdges (
      Renderer renderer, RenderProps props, int flags) {
      boolean highlight = ((flags & Renderer.HIGHLIGHT) != 0);
      boolean sorted = ((flags & Renderer.SORT_FACES) != 0);
      renderEdges(renderer, props, highlight, sorted);
   }
   
   public void renderEdges(
      Renderer renderer, RenderProps props, boolean highlight, 
      boolean sorted) {
      
      if (sorted) {
         PolygonalMesh mesh = getMesh();
         int[] faceOrder = sortFaces(mesh.getFaces (), renderer.getEyeZDirection());
         updateFaceTriangles (faceOrder, myFaceTriangles);
         updateFaceLines (faceOrder, myFaceLines);
      }
      
      renderEdges (renderer, props, highlight, myFaceLines, false);
   }
   
   public void renderEdges(Renderer renderer, RenderProps props, 
      boolean highlight,
      FeatureIndexArray edges,
      boolean featureSelection) {
    
      PolygonalMesh mesh = getMesh();

      RenderObject rob = getRenderObject ();
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

      drawEdges (renderer, props, highlight, false, rob, edges, featureSelection);

      renderer.popModelMatrix();
   }
   
   @Override
   public void render (
      Renderer renderer, RenderProps props, int flags) {
      boolean highlight = ((flags & Renderer.HIGHLIGHT) != 0);
      boolean sorted = ((flags & Renderer.SORT_FACES) != 0);
      render(renderer, props, highlight, sorted);
   }
   
   public void render(
      Renderer renderer, RenderProps props, boolean highlight, 
      boolean sorted) {
      
      if (sorted) {
         PolygonalMesh mesh = getMesh();
         int[] faceOrder = sortFaces(mesh.getFaces (), renderer.getEyeZDirection());
         updateFaceTriangles (faceOrder, myFaceTriangles);
         updateFaceLines (faceOrder, myFaceLines);
      }
      
      render (renderer, props, highlight, myFaceTriangles, myFaceLines, false);
   }
   
   public void render(Renderer renderer, RenderProps props, 
      boolean highlight,
      FeatureIndexArray faces, FeatureIndexArray edges,
      boolean featureSelection) {
      
      PolygonalMesh mesh = getMesh();
      if (mesh.numVertices() == 0) {
         return;
      }
      
      RenderObject rob = getRenderObject ();
     
      renderer.pushModelMatrix();
      if (mesh.isRenderBuffered()) {
         renderer.mulModelMatrix (mesh.getXMeshToWorldRender());
      }
      else {
         renderer.mulModelMatrix (mesh.XMeshToWorld);
      }

      boolean drawFaces = (props.getFaceStyle() != Renderer.FaceStyle.NONE);

      ColorMixing savedColorMixing = null;
      if (mesh.hasColors()) {
         savedColorMixing =
         renderer.setVertexColorMixing (mesh.getVertexColorMixing());
      }
      if (props.getDrawEdges()) {
         drawEdges (renderer, props, highlight, drawFaces, rob, edges, featureSelection);
      }

      if (drawFaces) {
         drawFaces (renderer, props, highlight, rob, faces, featureSelection);
      }
      if (mesh.hasColors()) {
         renderer.setVertexColorMixing (savedColorMixing);
      }

      renderer.popModelMatrix();
   }

}
