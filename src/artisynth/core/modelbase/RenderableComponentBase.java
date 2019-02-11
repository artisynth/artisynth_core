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
import maspack.util.*;

import java.util.*;

public abstract class RenderableComponentBase extends ModelComponentBase
implements RenderableComponent {
   protected RenderProps myRenderProps;

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = updateRenderProps (this, myRenderProps, props);
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

   public static RenderProps updateRenderProps (
      ModelComponent comp, RenderProps props, RenderProps setProps) {

      //System.out.println ("updating RP in " + comp.getClass());
      if (setProps != null) {
         if (props == null ||
             props == setProps ||
             props.getClass() != setProps.getClass()) {
            if (!(comp instanceof Renderable)) {
               throw new InternalErrorException ("component type "
               + comp.getClass() + " is not renderable");
            }
            props = ((Renderable)comp).createRenderProps();
            props.set (setProps);
            PropertyUtils.updateCompositeProperty (
               comp, "renderProps", null, props);
         }
         else {
            props.set (setProps);
            PropertyUtils.updateCompositeProperty (props);
         }
      }
      else {
         if (props != null) {
            PropertyUtils.updateCompositeProperty (
               comp, "renderProps", props, null);
            props = null;
         }
      }
      return props;
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

   public static boolean isVisible (RenderableComponent rcomp) {
      RenderProps props = rcomp.getRenderProps();
      if (props != null) {
         return props.isVisible();
      }
      CompositeComponent parent = rcomp.getParent();
      if (parent instanceof RenderableComponentList) {
         RenderableComponentList<?> rparent = (RenderableComponentList<?>)parent;
         props = rparent.getRenderProps();
         return props != null && props.isVisible();
      }
      return false;
   }

   public static void setVisible (RenderableComponent rcomp, boolean enable) {
      if (isVisible (rcomp) != enable) {
         // One option would be to climb hierarchy to see there is an ancestor
         // with RenderProps, and then if the "visible" value of those props ==
         // enable, set the "visible" mode for rcomp to inherited, but that
         // could lead to other problems - like if we set rcomp to be visible,
         // we might expect it to stay visible even if an ancestor is made
         // invisible. So we just explicitly set the visible property for now.
         RenderProps.setVisible (rcomp, enable);
      }
   }

}
