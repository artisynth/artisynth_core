/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import javax.media.opengl.*;

import java.util.*;

public class FemElement3dList extends RenderableComponentList<FemElement3d> {
   protected static final long serialVersionUID = 1;

   private static int DEFAULT_ELEMENT_WIDGET_SIZE = 0;
   private static PropertyMode DEFAULT_ELEMENT_WIDGET_SIZE_MODE = PropertyMode.Inherited;
   double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = DEFAULT_ELEMENT_WIDGET_SIZE_MODE;
   
   static PropertyList myProps = new PropertyList(FemElement3dList.class, RenderableComponentList.class);
   static {
         myProps.addInheritable (
            "elementWidgetSize:Inherited",
            "size of rendered widget in each element's center",
            DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public void setElementWidgetSize (double size) {
      myElementWidgetSize = size;
      myElementWidgetSizeMode = 
         PropertyUtils.propagateValue (
            this, "elementWidgetSize",
            myElementWidgetSize, myElementWidgetSizeMode);
   }

   public double getElementWidgetSize () {
      return myElementWidgetSize;
   }

   public void setElementWidgetSizeMode (PropertyMode mode) {
      myElementWidgetSizeMode =
         PropertyUtils.setModeAndUpdate (
            this, "elementWidgetSize", myElementWidgetSizeMode, mode);
   }

   public PropertyMode getElementWidgetSizeMode() {
      return myElementWidgetSizeMode;
   }
   
   public FemElement3dList () {
      this (null, null);
   }

   public FemElement3dList (String name, String shortName) {
      super (FemElement3d.class, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   public boolean hasParameterizedType() {
      return false;
   }   

   // public FemElement3dList (String name, String shortName,
   // CompositeComponent parent)
   // {
   // super (FemElement3d.class, name, shortName, parent);
   // setRenderProps(createRenderProps());
   // }

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
            FemElement3d elem = get (i);
            if (elem.isSelected() && elem.getRenderProps() != null) {
               list.addIfVisible (elem);
            }
         }
         for (int i = 0; i < size(); i++) {
            FemElement3d elem = get (i);
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
      GL2 gl = renderer.getGL2();
      boolean selecting = renderer.isSelecting();
      if (!addElementsInPrerender) {
         // we render all elements ourselves, taking care to render selected
         // elements first. This provides the maximum visibility for selected
         // elements, but means the transparency won't work properly.
         for (int i = 0; i < size(); i++) {
            FemElement3d elem = get (i);
            if (elem.isSelected() == selected &&
                elem.getRenderProps() != null &&
                elem.getRenderProps().isVisible()) {
               if (selecting) {
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

      if (myRenderProps.getLineWidth() > 0) {
         switch (myRenderProps.getLineStyle()) {
            case LINE: {
               if (!selecting) {
                  renderer.setLightingEnabled (false);
               }
               // draw regular points first
               gl.glLineWidth (myRenderProps.getLineWidth());
               renderer.setColor (myRenderProps.getLineColorArray(), false);
               for (int i = 0; i < size(); i++) {
                  FemElement3d elem = get (i);
                  if (elem.getRenderProps() == null &&
                      elem.isSelected() == selected) {
                     if (selecting) {
                        if (renderer.isSelectable (elem)) {
                           renderer.beginSelectionQuery (i);
                           elem.renderEdges (renderer, myRenderProps);
                           renderer.endSelectionQuery ();
                        }
                        
                     }
                     else {
                        renderer.updateColor (
                           myRenderProps.getLineColorArray(), elem.isSelected());
                        elem.renderEdges (renderer, myRenderProps);
                     }
                  }
               }
               gl.glLineWidth (1);
               if (!selecting) {
                  renderer.setLightingEnabled (true);
               }
               break;
            }
            case CYLINDER: {
               // GLU glu = renderer.getGLU();
               renderer.setMaterialAndShading (
                  myRenderProps, myRenderProps.getLineMaterial(), false);
               for (int i = 0; i < size(); i++) {
                  FemElement3d elem = get (i);
                  if (elem.getRenderProps() == null &&
                      elem.isSelected() == selected) {
                     if (selecting) {
                        if (renderer.isSelectable (elem)) {
                           renderer.beginSelectionQuery (i);
                           elem.renderEdges (renderer, myRenderProps);
                           renderer.endSelectionQuery ();
                        }
                     }
                     else {
                        maspack.render.Material mat = 
                           myRenderProps.getLineMaterial();
                        renderer.updateMaterial (
                           myRenderProps, mat, null, elem.isSelected());
                        elem.renderEdges (renderer, myRenderProps);
                     }
                  }
               }
               renderer.restoreShading (myRenderProps);
               break;
            }
            default:
               break;
         }
      }
      renderer.setMaterialAndShading (
         myRenderProps, myRenderProps.getFaceMaterial(), false);
      for (int i = 0; i < size(); i++) {
         // double widgetSize;
         FemElement3d elem = get (i);
         if (elem.getRenderProps() == null &&
             elem.isSelected() == selected &&
             elem.getElementWidgetSize() > 0) {
            if (selecting) {
               if (renderer.isSelectable (elem)) {
                  renderer.beginSelectionQuery (size()+i);
                  elem.renderWidget (renderer, myRenderProps);
                  renderer.endSelectionQuery ();
               }
            }
            else {
               maspack.render.Material mat = myRenderProps.getFaceMaterial();
               if (elem.isInverted()) {
                  mat = FemModel3d.myInvertedMaterial;
               }
               renderer.updateMaterial (myRenderProps, mat, elem.isSelected());
               elem.renderWidget (renderer, myRenderProps);
            }
         }
      }
      renderer.restoreShading (myRenderProps);
   }

   public void render (GLRenderer renderer, int flags) {
      // GL2 gl = renderer.getGL2().getGL2();
      // if (renderer.isSelecting()) {
      //    gl.glPushName (-1);
      // }
      dorender (renderer, /*selected=*/true);
      dorender (renderer, /*selected=*/false);
      // if (renderer.isSelecting()) {
      //    gl.glPopName();
      // }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      // May two selection queries per element, cases where we are rendering
      // the element's edges and widget in separate passes.
      return 2*size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < 2*size()) {
         CompositeComponent p = getParent();
         if (p instanceof GLRenderable) {
            if (!list.contains(p)) {
               list.addLast((GLRenderable)p);
            }
         }
         list.addLast (get (qid%size()));
      }
   }
   
   public FemElement3d newComponent (String classId)
      throws InstantiationException, IllegalAccessException {
      if (classId == null) {
         return null;
      }
      else {
         return (FemElement3d)ClassAliases.newInstance (
            classId, FemElement3d.class);
      }
   }
}
