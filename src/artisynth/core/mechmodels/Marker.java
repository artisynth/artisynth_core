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

public abstract class Marker extends Point 
   implements HasAttachments, AttachingComponent {

   public abstract void updateState();

   /**
    * {@inheritDoc}
    */
   public void getAttachments (List<DynamicAttachment> list) {
      DynamicAttachment at = getAttachment();
      if (at != null) { // paranoid
         list.add (at);
      }
   }  

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      DynamicAttachment at = getAttachment();
      if (at instanceof DynamicAttachmentComp) {
         DynamicAttachmentComp ac = (DynamicAttachmentComp)at;
         ArrayList<ModelComponent> allrefs = new ArrayList<ModelComponent>();
         ac.getHardReferences (allrefs);
         allrefs.remove (this); // remove this component
         refs.addAll (allrefs);
      }
   }

   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      DynamicAttachment at = getAttachment();
      if (at != null) {
         DynamicAttachmentBase.addConnectedMasterRefs (this, hcomp);
      }
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      super.disconnectFromHierarchy(hcomp);
      DynamicAttachment at = getAttachment();
      if (at != null) {
         DynamicAttachmentBase.removeConnectedMasterRefs (this, hcomp);
      }            
   }
   public void connectAttachment (DynamicComponent dcomp) {
      // should we check to see if dcomp is a known master attachment?
      dcomp.addMasterAttachment (getAttachment());
   }
}
