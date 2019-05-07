/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.LinkedList;
import java.util.Map;

import maspack.matrix.Vector3d;
import maspack.render.IsRenderable;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;

public abstract class ControllerMonitorBase extends ModelAgentBase
implements RenderableComponent {
   protected RenderProps myRenderProps;

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public void prerender (RenderList list) {
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public abstract void render (Renderer renderer, int flags);

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
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

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RenderableComponentBase comp =
         (RenderableComponentBase)super.copy (flags, copyMap);
      if (myRenderProps != null) {
         comp.setRenderProps (myRenderProps);
      }
      return comp;
   }

   /**
    * {@inheritDoc}
    */   
   public boolean hasState() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */   
   public ComponentState createState (
      ComponentState prevState) {
      return new EmptyState();
   }
   
   /**
    * {@inheritDoc}
    */   
   public void getState (ComponentState state) {
   }
   
   /**
    * {@inheritDoc}
    */
   public void setState (ComponentState state) {
   }
   
   /**
    * {@inheritDoc}
    */  
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {
      if (oldstate == null) {
         getState (newstate);
      }
      else {
         newstate.set (oldstate);
      }
   }
   
}
