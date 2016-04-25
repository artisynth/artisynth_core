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
   implements RenderableComponent {
   protected RenderProps myRenderProps = null;

   public static PropertyList myProps =
      new PropertyList (RenderableModelBase.class, ModelBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this model",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderableModelBase (String name) {
      super (name);
      setRenderProps (createRenderProps());
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
      CompositeComponent comp, Vector3d pmin, Vector3d pmax) {

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

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
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
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

}


