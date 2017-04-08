/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;

import artisynth.core.femmodels.AuxMaterialBundle.FractionRenderType;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;

public class AuxMaterialElementDescList
   extends RenderableComponentList<AuxMaterialElementDesc> {

   protected static final long serialVersionUID = 1;

   private RenderObject myWidgetRob = null;   
   private byte[] myRobFlags = null;

   public AuxMaterialElementDescList () {
      this (null, null);
   }

   public AuxMaterialElementDescList (String name, String shortName) {
      super (AuxMaterialElementDesc.class, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   public boolean hasParameterizedType() {
      return false;
   }
   
   protected float[] myElementColor; // render color for elements
 
   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps(this);
   }

   private static final int REG_GRP = 0;
   private static final int SEL_GRP = 1;
   private static final int GRP_MASK = 0x1;
   private static final int IS_LIST_RENDERED = 0x40;

   private byte getRobFlags (AuxMaterialElementDesc desc) {
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
      if (parent instanceof AuxMaterialBundle) {
         AuxMaterialBundle bundle = (AuxMaterialBundle)parent;
         return bundle.getElementWidgetSize();
      }
      else {
         return 0;
      }
   }

   private int renderObjectsNeedUpdating () {
      if (myRobFlags == null) {
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
         AuxMaterialElementDesc desc = get (i);
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
            AuxMaterialElementDesc desc = get (i);
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
      // add desc with their own renderProps inside prerender, as
      // usual. We add the selected elements first, so that they
      // will render first and be more visible.
      for (int i = 0; i < size(); i++) {
         AuxMaterialElementDesc desc = get(i);
         if (desc.isSelected() && desc.getRenderProps() != null) {
            list.addIfVisible (desc);
         }
      }
      for (int i = 0; i < size(); i++) {
         AuxMaterialElementDesc desc = get(i);
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
            AuxMaterialElementDesc desc = get(i);
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

   private void dorender (Renderer renderer, int flags, boolean selected) {
      // This code is taken mostly verbatim from FemElement3dList.
      // Should find a way to avoid duplicate code ...

      double fractionRenderRadius = 0;
      FractionRenderType fractionRenderType =
         AuxMaterialBundle.DEFAULT_FRACTION_RENDER_TYPE;
      ModelComponent parent = getParent();
      if (parent instanceof AuxMaterialBundle) {
         fractionRenderRadius =
            ((AuxMaterialBundle)parent).getFractionRenderRadius();
         fractionRenderType =
            ((AuxMaterialBundle)parent).getFractionRenderType();
      }
      
      if (fractionRenderRadius > 0) {
         Shading savedShading = renderer.getShading ();
         renderer.setShading (myRenderProps.getShading());
         renderer.setPointColoring (myRenderProps, /*highlight=*/false);
         renderFractions(renderer, fractionRenderRadius, fractionRenderType, false);
         renderer.setShading (savedShading);
      }
   }
   
   private void renderFractions (
      Renderer renderer, double len, FractionRenderType type,
      boolean selected) {

      for (int i=0; i<size(); i++) {
         AuxMaterialElementDesc desc = get(i);

         if (desc.getRenderProps() == null && desc.isSelected() == selected) {
//            if (renderer.isSelecting()) {
//               if (renderer.isSelectable (desc)) {
//                  renderer.beginSelectionQuery (i+size());
//                  desc.renderFraction(renderer, getRenderProps(), len, type);
//                  renderer.endSelectionQuery ();                  
//               }
//            }
//            else {
               desc.renderFraction(renderer, getRenderProps(), len, type);
//            }
         }
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
            AuxMaterialElementDesc desc = get(i);        
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
      return size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }
   
}
