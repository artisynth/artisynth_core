/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import javax.media.opengl.GL2;

import java.util.ArrayList;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.Vertex3d;
import maspack.geometry.MeshBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GL2.GL2Viewer;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentBase;

public class FaceComponent extends RenderableComponentBase {

   Face myFace;
   MeshBase myMesh;
   float[] myColorBuf = new float[4];
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
      myFace.computeRenderNormal();
   }

   @Override
   public void render(Renderer renderer, int flags) {

      RenderProps props = getRenderProps();
      
      if (props == null) {
         return;
      }
      
      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();
      
      gl.glPushMatrix();

      Shading shading = props.getShading();
      if (shading != Shading.NONE) {
         renderer.setFaceColoring (props, isSelected());
      }

      if (props.getFaceStyle() != Renderer.FaceStyle.NONE) {
         Shading savedShadeModel = renderer.getShading();

         if (shading == Shading.NONE) {
            renderer.setColor (props.getFaceColorF(), isSelected());
         }
         renderer.setShading (shading);

         if (props.getDrawEdges()) {
            gl.glEnable (GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset (1f, 1f);
         }
//         if (useVertexColouring) {
//            renderer.setLightingEnabled (false);
//         }

         drawFaces (gl, renderer, props);

//         if (useVertexColouring) {
//            renderer.setLightingEnabled (true);
//         }
         if (props.getDrawEdges()) {
            gl.glDisable (GL2.GL_POLYGON_OFFSET_FILL);
         }
         renderer.setShading (savedShadeModel);
      }

      if (!renderer.isSelecting()) {
         if (props.getBackColorF() != null) {
            gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1f);
         }
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
            renderer.setShading (Shading.GOURAUD);
         }
         else {
            renderer.setShading (Shading.FLAT);
         }

         drawEdges(gl, props);

         renderer.setLineWidth (savedLineWidth);
         renderer.setShading (savedShadeModel);
      }

      gl.glPopMatrix();

   }

   private void drawEdges(GL2 gl, RenderProps props) {

      gl.glBegin (GL2.GL_LINES);

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
      do {
         if (useVertexColouring) {
            int cidx = colorIndices[faceOff+k];
            if (cidx != -1) {
               float[] color = colors.get(cidx);
               gl.glColor4f (color[0], color[1], color[2], color[3]);
            }
         }
         Point3d pnt = he.head.myRenderPnt;
         gl.glVertex3d (pnt.x, pnt.y, pnt.z);
         pnt = he.tail.myRenderPnt;
         gl.glVertex3d (pnt.x, pnt.y, pnt.z);

         he = he.getNext();
         k++;
      } while (he != face.firstHalfEdge());

      gl.glEnd();
   }

   private void drawFaces(GL2 gl, Renderer renderer, RenderProps props) {

      byte[] savedCullFaceEnabled = new byte[1];
      int[] savedCullFaceMode = new int[1];

      gl.glGetBooleanv (GL2.GL_CULL_FACE, savedCullFaceEnabled, 0);
      gl.glGetIntegerv (GL2.GL_CULL_FACE_MODE, savedCullFaceMode, 0);

      Renderer.FaceStyle faces = props.getFaceStyle();
      if (props.getDrawEdges() && faces == Renderer.FaceStyle.NONE) {
         faces = Renderer.FaceStyle.FRONT_AND_BACK;
      }
      switch (faces) {
         case FRONT_AND_BACK: {
            gl.glDisable (GL2.GL_CULL_FACE);
            break;
         }
         case FRONT: {
            gl.glCullFace (GL2.GL_BACK);
            break;
         }
         case BACK: {
            gl.glCullFace (GL2.GL_FRONT);
            break;
         }
         default:
            break;
      }
      
      // if selecting, disable face culling
      if (renderer.isSelecting()) {
         gl.glDisable (GL2.GL_CULL_FACE);
      }
      
      drawFacesRaw (renderer, gl, props);

      if (savedCullFaceEnabled[0] != 0) {
         gl.glEnable (GL2.GL_CULL_FACE);
      }
      else {
         gl.glDisable (GL2.GL_CULL_FACE);
      }
      gl.glCullFace (savedCullFaceMode[0]);

   }

   void drawFacesRaw(Renderer renderer, GL2 gl, RenderProps props) {

      boolean useVertexColors = useVertexColouring;
      if (renderer.isSelecting()) {
         useVertexColors = false;
      }

      int type = -1;
      int lastType = 0;
      // 0 for triangle
      // 1 for quad
      // 2 for polygon

      gl.glBegin(GL2.GL_TRIANGLES);

      Face face = myFace;

      // determine face type
      if (face.isTriangle()) {
         type = 0;
      } else if (face.numEdges() == 4) {
         type = 1;
      } else {
         type = 2;
      }

      if (isSelected()) {
         // John Lloyd: wasn't this set in the caller?
         renderer.setFaceColoring (props, /*selected=*/true);
      } 

      if (type == 0 && lastType != 0) {
         gl.glEnd();
         gl.glBegin(GL2.GL_TRIANGLES);
      } else if (type == 1 && lastType != 1) {
         gl.glEnd();
         gl.glBegin(GL2.GL_QUADS);
      } else if (type == 2){
         gl.glEnd();
         gl.glBegin(GL2.GL_POLYGON);
      }

      Vector3d faceNrm = face.getNormal();
      gl.glNormal3d (faceNrm.x, faceNrm.y, faceNrm.z);

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
      
      HalfEdge he = face.firstHalfEdge();
      int k = 0;
      do {
         Vertex3d vtx = he.head;
         Point3d pnt = vtx.myRenderPnt;

         if (useVertexColors) {
            int cidx = colorIndices[faceOff+k];
            if (cidx != -1) {
               float[] color = colors.get(cidx);
               gl.glColor4f (color[0], color[1], color[2], color[3]);
            }
         }
         gl.glVertex3d (pnt.x, pnt.y, pnt.z);

         he = he.getNext();
         k++;
      } while (he != face.firstHalfEdge());

      gl.glEnd();


   }

}
