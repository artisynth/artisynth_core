/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.render.RenderList;
import maspack.render.*;
import artisynth.core.modelbase.*;

public class CollisionHandlerList
   extends RenderableComponentList<CollisionHandler> {

   protected static final long serialVersionUID = 1;

   public CollisionHandlerList() {
      this (null, null);
   }
   
   public CollisionHandlerList (String name, String shortName) {
      super (CollisionHandler.class, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   public boolean hasParameterizedType() {
      return false;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createRenderProps (this);
      return props;
   }

   public void prerender (RenderList list) {
      for (int i = 0; i < size(); i++) {
         CollisionHandler ch = get (i);
         if (ch.getRenderProps() != null) {
            list.addIfVisible (ch);
         }
         else {
            ch.prerender (myRenderProps);
         }
      }
   }

   // Structure changes to this list are handled internally by MechModel,
   // so we don't need to notify the hierarchy when they occur
   @Override
   protected void notifyStructureChanged (Object comp, boolean stateIChanged) {
   }   

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {
      for (CollisionHandler ch : this) {
         if (ch.getRenderProps() == null) {
            ch.render (renderer, myRenderProps, flags);
         }
      }
   }
}
