/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.LinkedList;

import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.ModelComponent;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMeshRenderer;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.FeatureIndexArray;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.HighlightStyle;
import maspack.util.DynamicIntArray;

public class FaceList<P extends FaceComponent> extends RenderableComponentList<P> {
   
   protected static final long serialVersionUID = 1;

   private final int REG_GRP = 0;
   private final int SEL_GRP = 1;
   
   public static boolean DEFAULT_SELECTABLE = true;
   protected boolean mySelectable = DEFAULT_SELECTABLE;

   private class RenderData {
      PolygonalMeshRenderer myMeshRenderer;
      FeatureIndexArray[] myFaces;
      FeatureIndexArray[] myEdges;
      DynamicIntArray[] myFaceIdxs;
      int[] myFaceIdxsVersions;
   };

   RenderData myRenderData = null;

   public static PropertyList myProps =
      new PropertyList (FaceList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
      myProps.add (
         "selectable isSelectable", 
         "true if faces in this list are selectable", DEFAULT_SELECTABLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public FaceList (
      Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());

      myRenderData = null;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createMeshProps (this);
   }

   protected PolygonalMesh getMesh() {
      ModelComponent comp = getParent();
      if (comp instanceof EditablePolygonalMeshComp) {
         MeshBase mesh = ((EditablePolygonalMeshComp)comp).getMesh();
         if (mesh instanceof PolygonalMesh) {
            return (PolygonalMesh)mesh;
         }
      }
      return null;
   }

   public void prerender (RenderList list) {
      PolygonalMesh pmesh = getMesh();
      if (pmesh == null) {
         myRenderData = null;
         return;
      }
      
      // create stored copy of render information
      RenderData rd = myRenderData;
      if (rd == null) {
         rd = new RenderData();
         rd.myMeshRenderer = new PolygonalMeshRenderer (pmesh);

         rd.myFaceIdxs = new DynamicIntArray[2];
         rd.myFaceIdxs[0] = new DynamicIntArray (size());
         rd.myFaceIdxs[1] = new DynamicIntArray (size());
         rd.myFaceIdxsVersions = new int[]{-1, -1};
      }

      rd.myMeshRenderer.prerender (getRenderProps());
      
      // assign faces in a way that does not trigger a list
      // modification if it indeed does not change
      int nFaces[] = {0, 0};
      for (int i = 0; i < size(); i++) {
         FaceComponent fc = get (i);
         
         if (fc.getRenderProps() != null) {
            list.addIfVisible (fc);
         }
         else {
            int gidx = fc.isSelected () ? SEL_GRP : REG_GRP;
            int faceIdx = fc.getFace ().getIndex ();
            rd.myFaceIdxs[gidx].set (nFaces[gidx], faceIdx);
            nFaces[gidx]++;
         }
      }
      rd.myFaceIdxs[REG_GRP].resize (nFaces[REG_GRP]);
      rd.myFaceIdxs[SEL_GRP].resize (nFaces[SEL_GRP]);
      myRenderData = rd;
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {
      
      RenderData rd = myRenderData;
      if (rd == null) { 
         return;
      }
      RenderProps props = getRenderProps();
      
      if (rd.myFaces == null) {
         rd.myFaces = new FeatureIndexArray[2];
         rd.myEdges = new FeatureIndexArray[2];
         
         for (int i=0; i<2; ++i) {
            int size = rd.myFaceIdxs[i].size ();
            rd.myFaces[i] = new FeatureIndexArray (size, 3*size);
            rd.myEdges[i] = new FeatureIndexArray (size, 6*size);
         }
      }
      
      if ( (flags & Renderer.SORT_FACES) != 0) {
         Vector3d zdir = renderer.getEyeZDirection ();
         DynamicIntArray[] faceIdxs = rd.myFaceIdxs;

         rd.myMeshRenderer.sortFaces (
            faceIdxs[REG_GRP].getArray (), 0, faceIdxs[REG_GRP].size(), zdir);
         rd.myMeshRenderer.sortFaces (
            faceIdxs[SEL_GRP].getArray (), 0, faceIdxs[SEL_GRP].size(), zdir);
         faceIdxs[REG_GRP].notifyModified ();
         faceIdxs[SEL_GRP].notifyModified ();
      }
      
      for (int i=0; i<2; ++i) {
         if (rd.myFaceIdxsVersions[i] != rd.myFaceIdxs[i].getVersion ()) {
            int[] faceIdxs = rd.myFaceIdxs[i].getArray ();
            int len = rd.myFaceIdxs[i].size ();
            rd.myMeshRenderer.updateFaceTriangles (
               faceIdxs, 0, len, rd.myFaces[i]);
            rd.myMeshRenderer.updateFaceLines (
               faceIdxs, 0, len, rd.myEdges[i]);
            rd.myFaceIdxsVersions[i] = rd.myFaceIdxs[i].getVersion ();
         }
      }

      // first draw selected
      boolean highlight = false;
      if (renderer.getHighlightStyle () == HighlightStyle.COLOR) {
         highlight = true;
      }
      rd.myMeshRenderer.render (
         renderer, props, highlight,
         rd.myFaces[SEL_GRP], rd.myEdges[SEL_GRP], true);
      rd.myMeshRenderer.render (
         renderer, props, false, rd.myFaces[REG_GRP],
         rd.myEdges[REG_GRP], true);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return mySelectable;
   }

   public void setSelectable (boolean enable) {
      mySelectable = enable;
   }

   public int numSelectionQueriesNeeded() {
      return mySelectable ? size() : -1;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      // faces and edges
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }

}
