/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.LinkedList;

import artisynth.core.modelbase.RenderableComponentList;
import maspack.geometry.PolygonalMesh;
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
   
   PolygonalMesh myMesh;
   
   private PolygonalMeshRenderer myMeshRenderer;
   FeatureIndexArray[] myFaces;
   FeatureIndexArray[] myEdges;
   DynamicIntArray[] myFaceIdxs;
   int[] myFaceIdxsVersions;

   public static PropertyList myProps =
      new PropertyList (FaceList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public FaceList (
      Class<P> type, String name, String shortName, PolygonalMesh mesh) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
      
      myMesh = mesh;
      myMeshRenderer = null;
      myFaceIdxs = null;
      myFaceIdxsVersions = null;
      myFaces = null;
      myEdges = null;
      
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createMeshProps (this);
   }

   public void prerender (RenderList list) {
      
      // create stored copy of render information
      if (myMeshRenderer == null) {
         myMeshRenderer = new PolygonalMeshRenderer (myMesh);
      }
      myMeshRenderer.prerender (getRenderProps());
      
      if (myFaceIdxs == null) {
         myFaceIdxs = new DynamicIntArray[2];
         myFaceIdxs[0] = new DynamicIntArray (size());
         myFaceIdxs[1] = new DynamicIntArray (size());
         myFaceIdxsVersions = new int[]{-1, -1};
      }
      
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
            myFaceIdxs[gidx].set (nFaces[gidx], faceIdx);
            nFaces[gidx]++;
         }
      }
      myFaceIdxs[REG_GRP].resize (nFaces[REG_GRP]);
      myFaceIdxs[SEL_GRP].resize (nFaces[SEL_GRP]);
      
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {
      
      RenderProps props = getRenderProps();
      
      if (myFaces == null) {
         myFaces = new FeatureIndexArray[2];
         myEdges = new FeatureIndexArray[2];
         
         for (int i=0; i<2; ++i) {
            int size = myFaceIdxs[i].size ();
            myFaces[i] = new FeatureIndexArray (size, 3*size);
            myEdges[i] = new FeatureIndexArray (size, 6*size);
         }
      }
      
      if ( (flags & Renderer.SORT_FACES) != 0) {
         Vector3d zdir = renderer.getEyeZDirection ();
         myMeshRenderer.sortFaces (myFaceIdxs[REG_GRP].getArray (), 0, myFaceIdxs[REG_GRP].size(), zdir);
         myMeshRenderer.sortFaces (myFaceIdxs[SEL_GRP].getArray (), 0, myFaceIdxs[SEL_GRP].size(), zdir);
         myFaceIdxs[REG_GRP].notifyModified ();
         myFaceIdxs[SEL_GRP].notifyModified ();
      }
      
      for (int i=0; i<2; ++i) {
         if (myFaceIdxsVersions[i] != myFaceIdxs[i].getVersion ()) {
            int[] faceIdxs = myFaceIdxs[i].getArray ();
            int len = myFaceIdxs[i].size ();
            myMeshRenderer.updateFaceTriangles (faceIdxs, 0, len, myFaces[i]);
            myMeshRenderer.updateFaceLines (faceIdxs, 0, len, myEdges[i]);
            myFaceIdxsVersions[i] = myFaceIdxs[i].getVersion ();
         }
      }

      // first draw selected
      boolean highlight = false;
      if (renderer.getHighlightStyle () == HighlightStyle.COLOR) {
         highlight = true;
      }
      myMeshRenderer.render (renderer, props, highlight, myFaces[SEL_GRP], myEdges[SEL_GRP], true);
      myMeshRenderer.render (renderer, props, false, myFaces[REG_GRP], myEdges[REG_GRP], true);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return size ();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      // faces and edges
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }

}
