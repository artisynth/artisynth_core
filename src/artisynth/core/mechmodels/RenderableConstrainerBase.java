/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import maspack.spatialmotion.FrictionInfo;

public abstract class RenderableConstrainerBase
   extends ConstrainerBase implements RenderableComponent {

   protected RenderProps myRenderProps;

   public static PropertyList myProps =
      new PropertyList (RenderableConstrainerBase.class, ConstrainerBase.class);

   static {
      myProps.add (
         "renderProps", "render properties", defaultRenderProps(null));
   }

   public PropertyInfoList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      if (!defaultRenderPropsAreNull()) {
         setRenderProps (createRenderProps());
      }
   }
   
   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = 
         RenderableComponentBase.updateRenderProps (
            this, myRenderProps, props);
   }

   public void prerender (RenderList list) {
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public abstract void render (Renderer renderer, int flags);

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   protected static RenderProps defaultRenderProps (HasProperties host) {
      return RenderProps.createRenderProps (host);
   }
   
   public RenderProps createRenderProps() {
      return defaultRenderProps(this);
   }
   
   public boolean defaultRenderPropsAreNull() {
      return false;
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
   public RenderableConstrainerBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RenderableConstrainerBase comp =
         (RenderableConstrainerBase)super.copy (flags, copyMap);
      if (myRenderProps != null) {
         comp.setRenderProps (myRenderProps);
      }
      return comp;
   }

}
