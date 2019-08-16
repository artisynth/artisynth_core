/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;

import maspack.matrix.Matrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderObject;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.femmodels.MuscleBundle.DirectionRenderType;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.RenderableComponentList;

public class MuscleElementDescList
   extends RenderableComponentList<MuscleElementDesc> {

   protected static final long serialVersionUID = 1;

   private RenderObject myWidgetRob = null;   
   private byte[] myRobFlags = null;

   public MuscleElementDescList() {
      this (null, null);
   }
   
   public MuscleElementDescList (String name, String shortName) {
      super (MuscleElementDesc.class, name, shortName);
      setRenderProps (createRenderProps());
   }

   public boolean hasParameterizedType() {
      return false;
   }

   // the following are set if an activation color is specified:
   protected float[] myDirectionColor; // render color for directions
   protected float[] myElementColor; // render color for elements
 
   public static PropertyList myProps =
      new PropertyList (MuscleElementDescList.class,
                        RenderableComponentList.class);

   static {
//       myProps.addInheritable (
//          "excitationColor", "color of activated muscles", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   private static final int REG_GRP = 0;
   private static final int SEL_GRP = 1;
   private static final int GRP_MASK = 0x1;
   private static final int IS_LIST_RENDERED = 0x40;

   private byte getRobFlags (MuscleElementDesc desc) {
      if (desc.getRenderProps() != null) {
         return (byte)0;
      }
      else {
         byte flags = IS_LIST_RENDERED;
         if (desc.isSelected()) {
            flags |= SEL_GRP;
         }
         return flags;
      }
   }

   private static final int BUILD = 1;   

   private double getWidgetSize() {
      ModelComponent parent = getParent();
      if (parent instanceof MuscleBundle) {
         MuscleBundle bundle = (MuscleBundle)parent;
         return bundle.getElementWidgetSize();
      }
      else {
         return 0;
      }
   }

   private int renderObjectsNeedUpdating () {
      if (myRobFlags == null || myRobFlags.length != size()) {
         return BUILD;
      }
      if ((getWidgetSize() != 0) != (myWidgetRob != null)) {
         return BUILD;
      }
      for (int i = 0; i < size(); i++) {
         byte flags = getRobFlags(get(i));
         if (flags != myRobFlags[i]) {
            return BUILD;
         }
      }
      return 0;      
   }

   protected void buildRenderObjects() {

      // allocate per-element flag storage that will be used to determine when
      // the render object needs to be rebuilt
      myRobFlags = new byte[size()];      
      for (int i=0; i<size(); i++) {
         MuscleElementDesc desc = get (i);
         // note: flags default to 0 if elem.getRenderProps() != null
         if (desc.getRenderProps() == null) {
            myRobFlags[i] = getRobFlags(desc); 
         }
      }

      double wsize = getWidgetSize();
      if (wsize > 0) {
         RenderObject r = new RenderObject();
         r.createTriangleGroup();
         r.createTriangleGroup();
         for (int i=0; i<size(); i++) {
            MuscleElementDesc desc = get (i);
            if (desc.getRenderProps() == null) {
               int group = desc.isSelected() ? SEL_GRP : REG_GRP;
               r.triangleGroup (group);
               FemElementRenderer.addWidgetFaces (r, desc.myElement);
            }
         }
         myWidgetRob = r;
      }
      else {
         myWidgetRob = null;
      }
   }

   public void prerender (RenderList list) {

      for (int i=0; i<size(); i++) {
         MuscleElementDesc desc = get(i);
         // Call to getWarpingData is to ensure that the invJ0 in the warping
         // data is updated in the current (simulation) thread.
         desc.myElement.getWarpingData(); 

         if (desc.getRenderProps() != null) {
            desc.setExcitationColors (desc.getRenderProps());
         }
         else {
            desc.setExcitationColors (myRenderProps);
         }
      }

      // add desc with their own renderProps inside prerender, as
      // usual. We add the selected elements first, so that they
      // will render first and be more visible.
      for (int i = 0; i < size(); i++) {
         MuscleElementDesc desc = get(i);
         if (desc.isSelected() && desc.getRenderProps() != null) {
            list.addIfVisible (desc);
         }
      }
      for (int i = 0; i < size(); i++) {
         MuscleElementDesc desc = get(i);
         if (!desc.isSelected() && desc.getRenderProps() != null) {
            list.addIfVisible (desc);
         }
      }         

      if (renderObjectsNeedUpdating () != 0) {
         buildRenderObjects();
      }
      if (myWidgetRob != null) {
         double wsize = getWidgetSize();
         RenderObject r = myWidgetRob;
         int pidx = 0;
         for (int i=0; i<size(); i++) {
            MuscleElementDesc desc = get(i);
            if (desc.getRenderProps() == null) {
               pidx = FemElementRenderer.updateWidgetPositions (
                  r, desc.myElement, wsize, pidx);
            }
         }
         FemElementRenderer.updateWidgetNormals (r, REG_GRP);
         FemElementRenderer.updateWidgetNormals (r, SEL_GRP);
         r.notifyPositionsModified();      
      }      
   }

   public boolean rendersSubComponents() {
      return true;
   }

   private void renderDirections (
      Renderer renderer, double len, DirectionRenderType type,
      boolean selected) {

      Matrix3d F = new Matrix3d();
      Vector3d dir = new Vector3d();
      float[] coords0 = new float[3];
      float[] coords1 = new float[3];

      for (int i=0; i<size(); i++) {
         MuscleElementDesc desc = get(i);

         if (desc.getRenderProps() == null && desc.isSelected() == selected) {
            if (renderer.isSelecting()) {
               if (renderer.isSelectable (desc)) {
                  renderer.beginSelectionQuery (i+size());
                  desc.renderDirection (
                     renderer, myRenderProps, coords0, coords1,
                     F, dir, len, type);
                  renderer.endSelectionQuery ();                  
               }
            }
            else {
               desc.renderDirection (
                  renderer, myRenderProps, coords0, coords1, F, dir, len, type);
            }
         }
      }
   }

   private void dorender (Renderer renderer, int flags, boolean selected) {
      // This code is taken mostly verbatim from FemElement3dList.
      // Should find a way to avoid duplicate code ...

      boolean selecting = renderer.isSelecting();

      double directionLength = 0;
      ModelComponent parent = getParent();
      MuscleBundle.DirectionRenderType renderType =
         MuscleBundle.DirectionRenderType.ELEMENT;
      if (parent instanceof MuscleBundle) {
         MuscleBundle bundle = (MuscleBundle)parent;
         directionLength = bundle.getDirectionRenderLen();
         renderType = bundle.getDirectionRenderType();
      }      
      if (directionLength > 0) {
         renderDirections (renderer, directionLength, renderType, selected);
      }
   }

   protected void drawWidgets (
      Renderer renderer, RenderObject r, RenderProps props, int group) {

      if (r.numTriangles(group) > 0) {
         renderer.setFaceColoring (props, group == SEL_GRP);
         renderer.drawTriangles (r, group);
      }
   }

   public void render (Renderer renderer, int flags) {
      RenderProps props = myRenderProps;
      if (renderer.isSelecting()) {
         for (int i=0; i<size(); i++) {
            MuscleElementDesc desc = get(i);        
            if (desc.getRenderProps() == null && renderer.isSelectable (desc)) {
               renderer.beginSelectionQuery (i);
               desc.render (renderer, myRenderProps, flags);
               renderer.endSelectionQuery ();
            }
         }
      }
      else {
         dorender (renderer, flags, /*selected=*/true);
         dorender (renderer, flags, /*selected=*/false);

         RenderObject r = myWidgetRob;
         if (r != null) {
            drawWidgets (renderer, r, props, SEL_GRP);
            drawWidgets (renderer, r, props, REG_GRP);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      // Need two selection queries per desc, since they are rendered
      // in two passes: widgets and directions.
      return 2*size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < 2*size()) {
         list.addLast (get (qid%size()));
      }
   }

   public void notifyParentOfChange (ComponentChangeEvent e) {
      if (e instanceof StructureChangeEvent) {
         myWidgetRob = null;
      }
      super.notifyParentOfChange (e);
   }

}


