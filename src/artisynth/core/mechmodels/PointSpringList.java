/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;

import maspack.properties.PropertyList;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.ScalableUnits;

public class PointSpringList<S extends PointSpringBase> extends
RenderableComponentList<S> implements ScalableUnits {

   protected static final long serialVersionUID = 1;

   public static PropertyList myProps =
      new PropertyList (PointSpringList.class, RenderableComponentList.class);

   protected AxialMaterial myMaterial;

   static {
      myProps.add (
         "material", "spring material parameters", createDefaultMaterial(), "CE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      setMaterial (createDefaultMaterial());
   }

   public PointSpringList (Class<S> type) {
      this (type, null, null);
   }
   
   public PointSpringList (Class<S> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   public static AxialMaterial createDefaultMaterial() {
      // allow null materials
      //return new LinearAxialMaterial();
      return null; 
   }

   public AxialMaterial getMaterial() {
      return myMaterial;
   }

   public void setMaterial (AxialMaterial mat) {
      myMaterial = (AxialMaterial)MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      // issue DynamicActivityChange in case solve matrix symmetry has changed:
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineProps (this);
   }

   // public boolean rendersSubComponents() {
   //    return true;
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public boolean isSelectable() {
   //    return true;
   // }

   // public int numSelectionQueriesNeeded() {
   //    return size();
   // }

   // public void getSelection (LinkedList<Object> list, int qid) {
   //    if (qid >= 0 && qid < size()) {
   //       list.addLast (get (qid));
   //    }
   // }
   
   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      if (myMaterial != null) {
         myMaterial.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleMass (s);
      }
      if (myMaterial != null) {
         myMaterial.scaleMass (s);
      }
   }
}
