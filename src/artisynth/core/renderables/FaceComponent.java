/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.ArrayList;

import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentBase;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;

public class FaceComponent extends RenderableComponentBase {

   Face myFace;
   MeshBase myMesh;
   boolean useVertexColouring = false;
   
   public static PropertyList myProps =
      new PropertyList (FaceComponent.class, ModelComponentBase.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FaceComponent(Face f, MeshBase mesh) {
      myFace = f;
      myMesh = mesh;
   }

   public Face getFace() {
      return myFace;
   }
  
   @Override
   public void prerender(RenderList list) {
      HalfEdge he0 = myFace.firstHalfEdge();
      HalfEdge he = he0;
      do {
         he.getHead ().saveRenderInfo ();
         he = he.getNext();
      } while (he != he0);
      myFace.computeRenderNormal ();
   }

   @Override
   public void render(Renderer renderer, int flags) {

      RenderProps props = getRenderProps();
      
      if (props == null) {
         return;
      }
      
      if (props.getDrawEdges()) {

         float savedLineWidth = renderer.getLineWidth();
         Shading savedShadeModel = renderer.getShading();

         renderer.setLineWidth (props.getLineWidth());

         if (props.getLineColor() != null && !renderer.isSelecting()) {
            renderer.setShading (Shading.NONE);
            renderer.setLineColoring (props, isSelected());
         }
         if (useVertexColouring && !renderer.isSelecting()) {
            renderer.setShading (Shading.SMOOTH);
         }
         else {
            renderer.setShading (Shading.FLAT);
         }

         drawEdges(renderer, props);

         renderer.setLineWidth (savedLineWidth);
         renderer.setShading (savedShadeModel);
      }

      
      if (props.getFaceStyle() != Renderer.FaceStyle.NONE) {
         Shading savedShadeModel = renderer.getShading();
         Shading shading = props.getShading();
         if (shading != Shading.NONE) {
            renderer.setFaceColoring (props, isSelected());
         }
         
         if (shading == Shading.NONE) {
            renderer.setColor (props.getFaceColorF(), isSelected());
         }
         renderer.setShading (shading);

         if (props.getDrawEdges()) {
            renderer.setDepthOffset (1);
         }

         drawFaces (renderer, shading, props);

         if (props.getDrawEdges()) {
            renderer.setDepthOffset (0);
         }
         renderer.setShading (savedShadeModel);
      }

      
   }

   private void drawEdges(Renderer renderer, RenderProps props) {
      
      Face face = myFace;
      HalfEdge he = face.firstHalfEdge();

      ArrayList<float[]> colors;
      int[] colorIndices;
      int faceOff;
      if (useVertexColouring) {
         colors = myMesh.getColors();
         colorIndices = myMesh.getColorIndices();
         faceOff = myMesh.getFeatureIndexOffsets()[face.getIndex()];
      }
      else {
         colors = null;
         colorIndices = null;
         faceOff = -1;
      }

      int k = 0;
      renderer.beginDraw (DrawMode.LINE_LOOP);
      do {
         if (useVertexColouring) {
            int cidx = colorIndices[faceOff+k];
            if (cidx != -1) {
               float[] color = colors.get(cidx);
               renderer.setColor (color);
            }
         }
         Point3d pnt = he.head.myRenderPnt;
         renderer.addVertex (pnt);        
         he = he.getNext();
         k++;
      } while (he != face.firstHalfEdge());
      renderer.endDraw ();
      
   }

   void drawFaces(Renderer renderer, Shading shading, RenderProps props) {

      boolean useVertexColors = useVertexColouring;
      if (renderer.isSelecting()) {
         useVertexColors = false;
      }

      Face face = myFace;
      
      boolean useVertexNormals = false; 
      ArrayList<Vector3d> normals = null;
      int[] normalIndices = null;
      int normalFaceOff = -1;
      if ((shading == Shading.SMOOTH || shading == Shading.METAL)
         && myMesh.hasNormals ()) {
         useVertexNormals = true;
         normals = myMesh.getNormals();
         normalIndices = myMesh.getNormalIndices();
         normalFaceOff = myMesh.getFeatureIndexOffsets()[face.getIndex()];
      }
      Vector3d faceNrm = face.getNormal();

      ArrayList<float[]> colors = null;
      int[] colorIndices = null;
      int colorFaceOff = -1;
      if (useVertexColouring) {
         colors = myMesh.getColors();
         colorIndices = myMesh.getColorIndices();
         colorFaceOff = myMesh.getFeatureIndexOffsets()[face.getIndex()];
      }
      
      renderer.beginDraw (DrawMode.TRIANGLE_FAN);
      HalfEdge he = face.firstHalfEdge();
      int k = 0;
      do {
         Vertex3d vtx = he.head;
         Point3d pnt = vtx.myRenderPnt;

         if (useVertexColors) {
            int cidx = colorIndices[colorFaceOff+k];
            if (cidx != -1) {
               float[] color = colors.get(cidx);
               renderer.setColor (color);
            }
         }
         if (useVertexNormals) {
            int nidx = normalIndices[normalFaceOff+k];
            if (nidx != -1) {
               Vector3d normal = normals.get (nidx);
               renderer.setNormal (normal);
            }
         } else {
            renderer.setNormal (faceNrm);
         }
         renderer.addVertex (pnt.x, pnt.y, pnt.z);

         he = he.getNext();
         k++;
      } while (he != face.firstHalfEdge());
      renderer.endDraw ();

   }

}
