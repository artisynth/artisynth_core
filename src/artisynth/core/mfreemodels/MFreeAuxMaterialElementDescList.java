/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.LinkedList;

import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.GL.GLRenderable;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class MFreeAuxMaterialElementDescList
   extends RenderableComponentList<MFreeAuxMaterialElementDesc> {

   protected static final long serialVersionUID = 1;

   public MFreeAuxMaterialElementDescList () {
      this (null, null);
   }
   
   public MFreeAuxMaterialElementDescList (String name, String shortName) {
      super (MFreeAuxMaterialElementDesc.class, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   public boolean hasParameterizedType() {
      return false;
   }
   
   protected float[] myElementColor; // render color for elements
 
   public static PropertyList myProps =
      new PropertyList (MFreeAuxMaterialElementDescList.class,
                        RenderableComponentList.class);


   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   boolean addDescsInPrerender = true;

   public void prerender (RenderList list) {
      if (addDescsInPrerender) {
         // add desc with their own renderProps inside prerender, as
         // usual. We add the selected elements first, so that they
         // will render first and be more visible.
         for (int i = 0; i < size(); i++) {
            MFreeAuxMaterialElementDesc desc = get(i);
            if (desc.isSelected() && desc.getRenderProps() != null) {
               list.addIfVisible (desc);
            }
         }
         for (int i = 0; i < size(); i++) {
            MFreeAuxMaterialElementDesc desc = get(i);
            if (!desc.isSelected() && desc.getRenderProps() != null) {
               list.addIfVisible (desc);
            }
         }         
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   private void dorender (Renderer renderer, int flags, boolean selected) {
      // This code is taken mostly verbatim from FemElement3dList.
      // Should find a way to avoid duplicate code ...

      boolean selecting = renderer.isSelecting();
      if (!addDescsInPrerender) {
         // we render all descs ourselves, taking care to render selected descs
         // first. This provides the maximum visibility for selected descs,
         // but means the transparency won't work properly.         
         for (int i = 0; i < size(); i++) {
            MFreeAuxMaterialElementDesc desc = get (i);
            if (desc.isSelected() == selected &&
                desc.getRenderProps() != null &&
                desc.getRenderProps().isVisible()) {
               if (selecting) {
                  if (renderer.isSelectable(desc)) {
                     renderer.beginSelectionQuery (i);
                     desc.render (renderer, flags);
                     renderer.endSelectionQuery ();
                  }
               }
               else {
                  desc.render (renderer, flags);
               }
            }
         }      
      }
      double widgetSize = 0;
      ModelComponent parent = getParent();
      
      //renderer.setMaterial (myRenderProps.getFaceMaterial(), false);
      if (widgetSize > 0) {
         renderer.setMaterialAndShading (
            myRenderProps, myRenderProps.getFaceMaterial(), false);
         for (int i = 0; i < size(); i++) {
            MFreeAuxMaterialElementDesc desc = get (i);
            if (desc.getRenderProps() == null &&
                desc.isSelected() == selected) {
               if (selecting) {
                  if (renderer.isSelectable (desc)) {
                     renderer.beginSelectionQuery (i);
                     desc.myElement.renderWidget (renderer, myRenderProps, 0);
                     renderer.endSelectionQuery ();
                  }
               }
               else {
                  maspack.render.Material mat = myRenderProps.getFaceMaterial();
                  renderer.updateMaterial (
                     myRenderProps, mat, desc.myWidgetColor, desc.isSelected());
                  desc.myElement.renderWidget (renderer, myRenderProps, 0);
               }
            }
         }
         renderer.restoreShading (myRenderProps);
      }
     
   }

   public void render (Renderer renderer, int flags) {
      dorender (renderer, flags, /*selected=*/true);
      dorender (renderer, flags, /*selected=*/false);
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
