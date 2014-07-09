/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.LinkedList;

import javax.media.opengl.GL2;

import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.ClassAliases;

public class MFreeElement3dList extends RenderableComponentList<MFreeElement3d> {
   protected static final long serialVersionUID = 1;

   public MFreeElement3dList () {
      this (null, null);
   }
   
   public MFreeElement3dList (String name, String shortName) {
      super (MFreeElement3d.class, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   public boolean hasParameterizedType() {
      return false;
   }
   
   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   boolean addElementsInPrerender = true;

   public void prerender (RenderList list) {
      if (addElementsInPrerender) {
         // add elements with their own renderProps inside prerender, as
         // usual. We add the selected elements first, so that they
         // will render first and be more visible.
         for (int i = 0; i < size(); i++) {
            MFreeElement3d elem = get (i);
            if (elem.isSelected() && elem.getRenderProps() != null) {
               list.addIfVisible (elem);
            }
         }
         for (int i = 0; i < size(); i++) {
            MFreeElement3d elem = get (i);
            if (!elem.isSelected() && elem.getRenderProps() != null) {
               list.addIfVisible (elem);
            }
         }
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   private void dorender (GLRenderer renderer, boolean selected) {
      GL2 gl = renderer.getGL2().getGL2();
      if (!addElementsInPrerender) {
         // we render all elements ourselves, taking care to render selected
         // elements first. This provides the maximum visibility for selected
         // elements, but means the transparency won't work properly.
         for (int i = 0; i < size(); i++) {
            MFreeElement3d elem = get (i);
            if (elem.isSelected() == selected &&
                elem.getRenderProps() != null &&
                elem.getRenderProps().isVisible()) {
               if (renderer.isSelecting()) {
                  if (renderer.isSelectable (elem)) {
                     renderer.beginSelectionQuery (i);
                     elem.render (renderer, 0);
                     renderer.endSelectionQuery ();
                  }
               }
               else {
                  elem.render (renderer, 0);
               }
            }
         }
      }      
   }

   public void render (GLRenderer renderer, int flags) {
      dorender (renderer, /*selected=*/true);
      dorender (renderer, /*selected=*/false);
      
      renderer.setMaterialAndShading (
         myRenderProps, myRenderProps.getFaceMaterial(), false);
      for (int i = 0; i < size(); i++) {
         double widgetSize;
         MFreeElement3d elem = get (i);
         if (elem.getRenderProps() == null &&
             elem.isSelected() == isSelected() &&
             elem.getElementWidgetSize() > 0) {
            if (renderer.isSelecting()) {
               if (renderer.isSelectable (elem)) {
                  renderer.beginSelectionQuery (i);
                  elem.renderWidget (renderer, myRenderProps, 0);
                  renderer.endSelectionQuery ();
               }
            }
            else {
               maspack.render.Material mat = myRenderProps.getFaceMaterial();
               renderer.updateMaterial (myRenderProps, mat, elem.isSelected());
               elem.renderWidget (renderer, myRenderProps, 0);
            }
         }
      }
      renderer.restoreShading (myRenderProps);
      
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
   
   public MFreeElement3d newComponent (String classId)
      throws InstantiationException, IllegalAccessException {
      if (classId == null) {
         return null;
      }
      else {
         return (MFreeElement3d)ClassAliases.newInstance (
            classId, MFreeElement3d.class);
      }
   }
}
