/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;

import javax.media.opengl.GL2;

import maspack.matrix.Matrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.femmodels.MuscleBundle.DirectionRenderType;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class MuscleElementDescList
   extends RenderableComponentList<MuscleElementDesc> {

   protected static final long serialVersionUID = 1;

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

//    protected float[] myExcitationColor = null;
//    protected PropertyMode myExcitationColorMode = PropertyMode.Inherited;

   static {
//       myProps.addInheritable (
//          "excitationColor", "color of activated muscles", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

//    public Color getExcitationColor() {
//       if (myExcitationColor == null) {
//          return null;
//       }
//       else {
//          return new Color (
//             myExcitationColor[0], myExcitationColor[1], myExcitationColor[2]);
//       }
//    }

//    public void setExcitationColor (Color color) {
//       if (color == null) {
//          myExcitationColor = null;
//       }
//       else {
//          myExcitationColor = color.getRGBColorComponents(null);
//       }
//       myExcitationColorMode =
//          PropertyUtils.propagateValue (
//             this, "excitationColor", color, myExcitationColorMode);
//    }

//    public PropertyMode getExcitationColorMode() {
//       return myExcitationColorMode;
//    }

//    public void setExcitationColorMode (PropertyMode mode) {
//       myExcitationColorMode =
//          PropertyUtils.setModeAndUpdate (
//             this, "excitationColor", myExcitationColorMode, mode);
//    }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   boolean addDescsInPrerender = true;

   public void prerender (RenderList list) {
      // don't add element descs to the render list. Instead, render them
      // ourselves, to make sure that selected elements are rendered
      // first. However, this means that you can't properly render elements
      // transparently.

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
      if (addDescsInPrerender) {
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
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   private void renderDirections (
      GLRenderer renderer, double len, DirectionRenderType type,
      boolean selected) {

      //GL2 gl = renderer.getGL2().getGL2();
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

   private void dorender (GLRenderer renderer, int flags, boolean selected) {
      // This code is taken mostly verbatim from FemElement3dList.
      // Should find a way to avoid duplicate code ...

//      GL2 gl = renderer.getGL2().getGL2();
      boolean selecting = renderer.isSelecting();

      if (!addDescsInPrerender) {
         // we render all descs ourselves, taking care to render selected descs
         // first. This provides the maximum visibility for selected descs,
         // but means the transparency won't work properly.
         for (int i = 0; i < size(); i++) {
            MuscleElementDesc desc = get (i);
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
      double directionLength = 0;
      ModelComponent parent = getParent();
      MuscleBundle.DirectionRenderType renderType =
         MuscleBundle.DirectionRenderType.ELEMENT;
      if (parent instanceof MuscleBundle) {
         MuscleBundle bundle = (MuscleBundle)parent;
         widgetSize = bundle.getElementWidgetSize();
         directionLength = bundle.getDirectionRenderLen();
         renderType = bundle.getDirectionRenderType();
      }      
      //renderer.setMaterial (myRenderProps.getFaceMaterial(), false);
      if (widgetSize > 0) {
         renderer.setMaterialAndShading (
            myRenderProps, myRenderProps.getFaceMaterial(), false);
         for (int i = 0; i < size(); i++) {
            MuscleElementDesc desc = get (i);
            if (desc.getRenderProps() == null &&
                desc.isSelected() == selected) {
               if (selecting) {
                  if (renderer.isSelectable (desc)) {
                     renderer.beginSelectionQuery (i);
                     desc.myElement.renderWidget (
                        renderer, widgetSize, myRenderProps);
                     renderer.endSelectionQuery ();
                  }
               }
               else {
                  maspack.render.Material mat = myRenderProps.getFaceMaterial();
                  renderer.updateMaterial (
                     myRenderProps, mat, desc.myWidgetColor, desc.isSelected());
                  desc.myElement.renderWidget (
                     renderer, widgetSize, myRenderProps);
               }               
            }
         }
         renderer.restoreShading (myRenderProps);
      }
      if (directionLength > 0) {
         renderDirections (renderer, directionLength, renderType, selected);
      }
   }

   public void render (GLRenderer renderer, int flags) {
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
      // Need two selection queries per desc, since they are rendered
      // in two passes: widgets and directions.
      return 2*size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < 2*size()) {
         list.addLast (get (qid%size()));
      }
   }
}


