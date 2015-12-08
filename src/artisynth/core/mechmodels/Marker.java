/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import maspack.matrix.*;

public abstract class Marker extends Point implements HasAttachments {

   public abstract void updateState();

   protected void removeBackRefsIfConnected() {
      if (isConnectedToHierarchy()) {
         DynamicAttachment ax = getAttachment();
         ax.removeBackRefs();
      }
   }

   protected void addBackRefsIfConnected() {
      if (isConnectedToHierarchy()) {
         DynamicAttachment ax = getAttachment();
         ax.addBackRefs();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getAttachments (List<DynamicAttachment> list) {
      DynamicAttachment ax = getAttachment();
      if (ax != null) { // paranoid
         list.add (ax);
      }
   }  

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      DynamicAttachment ax = getAttachment();
      if (ax != null) {
         ArrayList<ModelComponent> allrefs = new ArrayList<ModelComponent>();
         ax.getHardReferences (allrefs);
         allrefs.remove (this); // remove this component
         refs.addAll (allrefs);
      }
   }

   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      DynamicAttachment ax = getAttachment();
      if (ax != null) {
         ax.addBackRefs();
      }
      updateState(); // do we need this?
   }

   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      DynamicAttachment ax = getAttachment();
      if (ax != null) {
         ax.removeBackRefs();
      }
   }

}
