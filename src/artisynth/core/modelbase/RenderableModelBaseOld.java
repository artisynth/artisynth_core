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

public abstract class RenderableModelBaseOld extends ModelBaseOld implements
RenderableComponent {
   protected RenderProps myRenderProps = null;

   public static PropertyList myProps =
      new PropertyList (RenderableModelBaseOld.class, ModelBaseOld.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderableModelBaseOld (String name) {
      super (name);
      setRenderProps (createRenderProps());
   }

   public RenderableModelBaseOld() {
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

   public void prerender (RenderList list) {
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
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

   public RenderableModelBaseOld copy (
      Map<ModelComponent,ModelComponent> copyMap,int flags) {
      RenderableModelBaseOld rmb = (RenderableModelBaseOld)super.copy (flags, copyMap);

      rmb.setRenderProps (myRenderProps);

      return rmb;
   }


}


