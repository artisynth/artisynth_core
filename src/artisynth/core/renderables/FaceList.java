/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.LinkedList;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Material;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Shading;
import maspack.render.Renderer;
import maspack.render.GL.GL2.DisplayListKey;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GL2.DisplayListManager.DisplayListPassport;
import artisynth.core.modelbase.RenderableComponentList;

import javax.media.opengl.GL2;

public class FaceList<P extends FaceComponent> extends RenderableComponentList<P> {

   protected static final long serialVersionUID = 1;
   DisplayListKey faceKey;
   DisplayListKey edgeKey;
   int faceListVersion;
   int edgeListVersion;
   boolean facesChanged;
   boolean edgesChanged;
   
   float[] myColorBuf = new float[4];
   boolean useVertexColouring = false;

   public static PropertyList myProps =
      new PropertyList (FaceList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   private void notifyRenderChanged() {
      facesChanged = true;
      edgesChanged = true;
   }
   
   private int getFaceVersion() {
      if (facesChanged) {
         faceListVersion++;
         facesChanged = false;
      }
      return faceListVersion;
   }
   
   private int getEdgeVersion() {
      if (edgesChanged) {
         edgeListVersion++;
         edgesChanged = false;
      }
      return edgeListVersion;
   }
   
   @Override
   protected void notifyStructureChanged(Object comp, boolean stateIsChanged) {
      super.notifyStructureChanged(comp, stateIsChanged);
      notifyRenderChanged();
   }
   
   public FaceList (Class<P> type) {
      this (type, null, null);
   }
   
   public FaceList (
      Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
      
      faceKey = new DisplayListKey(this, 0);
      edgeKey = new DisplayListKey(this, 1);
      faceListVersion = 0;
      edgeListVersion = 0;
      facesChanged = true;
      edgesChanged = true;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createMeshProps (this);
   }

   public void prerender (RenderList list) {
      for (int i = 0; i < size(); i++) {
         FaceComponent p = get (i);
         if (p.getRenderProps() != null) {
            list.addIfVisible (p);
         }
         else {
            p.prerender (list);
         }
      }
      notifyRenderChanged();
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {
      
      RenderProps props = getRenderProps();
      Material faceMat = props.getFaceMaterial();
      if (isSelected()) {
         faceMat = renderer.getSelectionMaterial();
      }

      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();
      
      gl.glPushMatrix();

      Shading shading = props.getShading();
      if (!renderer.isSelecting()) {
         if (shading != Shading.NONE) {
            if (isSelected()) {
               renderer.getSelectionMaterial().apply (gl, GL2.GL_FRONT_AND_BACK);
            } else {
               faceMat.apply (gl, GL2.GL_FRONT_AND_BACK);
               gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
            }
         }
      }

      if (props.getFaceStyle() != RenderProps.Faces.NONE) {
         int[] savedShadeModel = new int[1];
         gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

         if (shading == Shading.NONE) {
            renderer.setLightingEnabled (false);

            if (isSelected()) {
               renderer.getSelectionColor().getColorComponents(myColorBuf);
               renderer.setColor(myColorBuf, false);
            } else {
               renderer.setColor (
                  props.getFaceColorArray(), false);
            }
         }
         else if (((shading != Shading.FLAT) || useVertexColouring) &&
            !renderer.isSelecting()) {
            gl.glShadeModel (GL2.GL_SMOOTH);
         }
         else { // shading == Shading.FLAT
            gl.glShadeModel (GL2.GL_FLAT);
         }

         if (props.getDrawEdges()) {
            gl.glEnable (GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset (1f, 1f);
         }
         if (useVertexColouring) {
            renderer.setLightingEnabled (false);
         }

         boolean useDisplayList = !renderer.isSelecting();
         DisplayListPassport facePP = null;
         int facePrint = getFaceVersion();
         boolean compile = true;
         
         if (useDisplayList) {
            facePP = viewer.getDisplayListPassport(gl, faceKey);
            if (facePP == null) {
               facePP = viewer.allocateDisplayListPassport(gl, faceKey, facePrint);
               compile = true;
            } else {
               compile = !(facePP.compareExchangeFingerPrint(facePrint));
            }
         }
         
         if (!useDisplayList || compile) {
            if (facePP != null) {
               gl.glNewList(facePP.getList(), GL2.GL_COMPILE);
            }
            drawFaces (gl, renderer, props, faceMat);
            if (facePP != null) {
               gl.glEndList();
               gl.glCallList(facePP.getList());
            }
         } else {
            gl.glCallList(facePP.getList());
         }

         if (useVertexColouring) {
            renderer.setLightingEnabled (true);
         }
         if (props.getDrawEdges()) {
            gl.glDisable (GL2.GL_POLYGON_OFFSET_FILL);
         }
         if (shading == Shading.NONE) {
            renderer.setLightingEnabled (true);
         }
         gl.glShadeModel (savedShadeModel[0]);
      }

      if (!renderer.isSelecting()) {
         if (props.getBackMaterial() != null) {
            gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1f);
         }
      }

      if (props.getDrawEdges()) {

         boolean reenableLighting = false;
         int[] savedLineWidth = new int[1];
         gl.glGetIntegerv (GL2.GL_LINE_WIDTH, savedLineWidth, 0);
         int[] savedShadeModel = new int[1];
         gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

         gl.glLineWidth (props.getLineWidth());

         if (props.getLineColor() != null && !renderer.isSelecting()) {
            reenableLighting = renderer.isLightingEnabled();
            renderer.setLightingEnabled (false);
            float[] color;
            if (isSelected()) {
               color = myColorBuf;
               renderer.getSelectionColor().getRGBColorComponents (color);
            }
            else {
               color = props.getLineColorArray();
            }
            renderer.setColor (color);
         }
         if (useVertexColouring && !renderer.isSelecting()) {
            gl.glShadeModel (GL2.GL_SMOOTH);
         }
         else {
            gl.glShadeModel (GL2.GL_FLAT);
         }

         boolean useDisplayList = !renderer.isSelecting();
         DisplayListPassport edgePP = null;
         int edgePrint = getEdgeVersion();
         boolean compile = true;
         
         if (useDisplayList) {
            edgePP = viewer.getDisplayListPassport(gl, edgeKey);
            if (edgePP == null) {
               edgePP = viewer.allocateDisplayListPassport(gl, edgeKey, edgePrint);
               compile = true;
            } else {
               compile = !(edgePP.compareExchangeFingerPrint(edgePrint));
            }
         }
         
         if (!useDisplayList || compile) {
            if (edgePP != null) {
               gl.glNewList(edgePP.getList(), GL2.GL_COMPILE);
            }
            drawEdges(gl, props);
            if (edgePP != null) {
               gl.glEndList();
               gl.glCallList(edgePP.getList());
            }
         } else {
            gl.glCallList(edgePP.getList());
         }

         if (reenableLighting) {
            renderer.setLightingEnabled (true);
         }
         gl.glLineWidth (savedLineWidth[0]);
         gl.glShadeModel (savedShadeModel[0]);
      }
      
      gl.glPopMatrix();

   }

   private void drawEdges(GL2 gl, RenderProps props) {
      int[] shadingModel = new int[1];

      gl.glGetIntegerv (GL2.GL_SHADE_MODEL, shadingModel, 0);

      gl.glBegin (GL2.GL_LINES);
      for (FaceComponent fc : this) {

         if (fc.getRenderProps() == null) {
            Face face = fc.getFace();
            HalfEdge he = face.firstHalfEdge();

            do {
               if (useVertexColouring) {
                  float[] color = he.head.getColorArray();
                  gl.glColor4f (color[0], color[1], color[2], color[3]);
               }
               Point3d pnt = he.head.myRenderPnt;
               gl.glVertex3d (pnt.x, pnt.y, pnt.z);
               pnt = he.tail.myRenderPnt;
               gl.glVertex3d (pnt.x, pnt.y, pnt.z);

               he = he.getNext();
            } while (he != face.firstHalfEdge());

         }
      }
      gl.glEnd();
   }

   private void drawFaces(GL2 gl, Renderer renderer, RenderProps props, Material faceMat) {

      byte[] savedCullFaceEnabled = new byte[1];
      int[] savedCullFaceMode = new int[1];

      gl.glGetBooleanv (GL2.GL_CULL_FACE, savedCullFaceEnabled, 0);
      gl.glGetIntegerv (GL2.GL_CULL_FACE_MODE, savedCullFaceMode, 0);

      RenderProps.Faces faces = props.getFaceStyle();
      if (props.getDrawEdges() && faces == RenderProps.Faces.NONE) {
         faces = RenderProps.Faces.FRONT_AND_BACK;
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
      
      // if selecting faces, disable face culling
      if (renderer.isSelecting()) {
         gl.glDisable (GL2.GL_CULL_FACE);
      }

      drawFacesRaw (renderer, gl, props, faceMat);

      if (savedCullFaceEnabled[0] != 0) {
         gl.glEnable (GL2.GL_CULL_FACE);
      }
      else {
         gl.glDisable (GL2.GL_CULL_FACE);
      }
      gl.glCullFace (savedCullFaceMode[0]);

   }

   void drawFacesRaw(Renderer renderer, GL2 gl, RenderProps props, Material faceMaterial) {

      int[] shadingModel = new int[1];

      gl.glGetIntegerv (GL2.GL_SHADE_MODEL, shadingModel, 0);

      boolean useVertexColors = useVertexColouring;
      if (renderer.isSelecting()) {
         useVertexColors = false;
      }

      int type = -1;
      int lastType = -1;
      // 0 for triangle
      // 1 for quad
      // 2 for polygon
      
      boolean lastSelected = false;
      
      
      int i = 0;
      for (FaceComponent fc : this) {
         if (fc.getRenderProps() == null) {
            Face face = fc.getFace();
            
            // determine face type
            if (face.isTriangle()) {
               type = 0;
            } else if (face.numEdges() == 4) {
               type = 1;
            } else {
               type = 2;
            }
            
            if (renderer.isSelecting()) {
               renderer.beginSelectionQuery(i);
            } else {
               if (fc.isSelected() && !lastSelected) {
                  renderer.updateMaterial(props, renderer.getSelectionMaterial(), true);
                  lastSelected = true;
               } else if (!fc.isSelected() && lastSelected){
                  renderer.updateMaterial(props, faceMaterial, false);
                  lastSelected = false;
               }
            }
            
            
            if (lastType == -1) {
               switch(type) {
                  case 0:
                     gl.glBegin(GL2.GL_TRIANGLES);
                     break;
                  case 1:
                     gl.glBegin(GL2.GL_QUADS);
                     break;
                  default:
                     gl.glBegin(GL2.GL_POLYGON);
               }
            } else if (type == 0 && lastType != 0) {
               gl.glEnd();
               gl.glBegin(GL2.GL_TRIANGLES);
            } else if (type == 1 && lastType != 1) {
               gl.glEnd();
               gl.glBegin(GL2.GL_QUADS);
            } else if (type == 2 && lastType != 2){
               gl.glEnd();
               gl.glBegin(GL2.GL_POLYGON);
            }
            
            Vector3d faceNrm = face.getNormal();
            gl.glNormal3d (faceNrm.x, faceNrm.y, faceNrm.z);

            HalfEdge he = face.firstHalfEdge();
            do {
               Vertex3d vtx = he.head;
               Point3d pnt = vtx.myRenderPnt;

               if (useVertexColors) {
                  float[] color = he.head.getColorArray();
                  gl.glColor4f (color[0], color[1], color[2], color[3]);
               }
               gl.glVertex3d (pnt.x, pnt.y, pnt.z);

               he = he.getNext();
            } while (he != face.firstHalfEdge());

            if (renderer.isSelecting()) {
               gl.glEnd();
               renderer.endSelectionQuery();
               lastType = -1;
            } else {
               lastType = type;
            }
         }
         i++;
      }
      
      if (lastType != -1) {
         gl.glEnd();
      }

      
   }


   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }

}
