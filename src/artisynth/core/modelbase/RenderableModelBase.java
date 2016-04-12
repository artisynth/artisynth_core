/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.properties.*;
import maspack.render.*;
import maspack.matrix.*;

import java.util.*;

public abstract class RenderableModelBase extends ModelBase
   implements RenderableComponent, HasRenderMappings {
   protected RenderProps myRenderProps = null;
   protected RenderMappings myRenderMappings = null;

   public static PropertyList myProps =
      new PropertyList (RenderableModelBase.class, ModelBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();
   static private RenderMappings defaultRenderMappings = new RenderMappings();

   static {
      myProps.add (
         "renderProps * *", "render properties for this model",
         defaultRenderProps);
      myProps.add (
         "renderMappings", "render mappings for this model",
         defaultRenderMappings);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderableModelBase (String name) {
      super (name);
      setRenderProps (createRenderProps());
      setRenderMappings (new RenderMappings());
   }

   public RenderableModelBase() {
      this (null);
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public RenderMappings getRenderMappings() {
      return myRenderMappings;
   }

   public void setRenderMappings (RenderMappings mappings) {
      myRenderMappings =
         RenderMappings.updateRenderMappings (this, myRenderMappings, mappings);
   }

   protected void recursivelyPrerender (
      CompositeComponent comp, RenderList list) {

       for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof Renderable) {
            list.addIfVisible ((Renderable)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyPrerender ((CompositeComponent)c, list);
         }
      }     
   }

   public void prerender (RenderList list) {

      recursivelyPrerender (this, list);
   }

   protected void recursivelyUpdateBounds (
      CompositeComponent comp, Point3d pmin, Point3d pmax) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof Renderable) {
            ((Renderable)c).updateBounds (pmin, pmax);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyUpdateBounds ((CompositeComponent)c, pmin, pmax);
         }
      }

   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      recursivelyUpdateBounds (this, pmin, pmax);
   }

   public abstract void render (Renderer renderer, int flags);

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.getAlpha() != 1) {
         code |= TRANSPARENT;
      }
      return code;
   }

}


