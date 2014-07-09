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

   public void updateBounds (Point3d pmin, Point3d pmax) {
   }

   public abstract void render (GLRenderer renderer, int flags);

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
      if (myRenderProps != null && myRenderProps.getAlpha() != 1) {
         code |= TRANSLUCENT;
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

}
