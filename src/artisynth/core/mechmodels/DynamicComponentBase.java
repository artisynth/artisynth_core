/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.GeometryTransformer;

import java.util.*;

public abstract class DynamicComponentBase extends RenderableComponentBase
   implements DynamicComponent {
   protected DynamicAttachment myAttachment;
   protected LinkedList<DynamicAttachment> myMasterAttachments;
   protected ArrayList<Constrainer> myConstrainers;

   protected boolean myDynamicP;
   // Activity myActivity;
   protected int mySolveIdx = -1;

   public DynamicAttachment getAttachment() {
      return myAttachment;
   }

   protected void setDynamic (boolean dynamic) {
      if (myDynamicP != dynamic) {
         myDynamicP = dynamic;
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }    
   }

   public boolean isDynamic() {
      return myDynamicP;
   }

   public void setAttached (DynamicAttachment attachment) {
      if (myAttachment != attachment) {
         myAttachment = attachment;
         if (MechSystemBase.useAllDynamicComps) {
            notifyParentOfChange (
               new DynamicActivityChangeEvent (this, /*stateChanged=*/false));
         }
         else {
            notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         }
      }
   }

   public boolean isActive() {
//       if (MechModel.myAttachmentsAsConstraints) {
//          return myDynamicP; 
//       }
//       else {
         return myDynamicP && myAttachment == null;
         //      }
   }

   public boolean isAttached() {
      return myAttachment != null;
   }

//   public boolean isDynamicOrAttached() {
//      return myDynamicP || myAttachment != null;
//   }

   public boolean isParametric() {
      return !myDynamicP && myAttachment == null;
   }

   public boolean isControllable() {
      if (isActive()) {
         return true;
      }
      else if (isAttached()) {
         DynamicComponent[] masters = getAttachment().getMasters();
         for (int i=0; i<masters.length; i++) {
            if (masters[i].isControllable()) {
               return true;
            }
         }
      }
      return false;
   }

   public int getSolveIndex() {
      return mySolveIdx;
   }

   public void setSolveIndex (int idx) {
      mySolveIdx = idx;
   }

   public abstract void addSolveBlock (SparseNumberedBlockMatrix S);

//   public abstract MatrixBlock getSolveBlock();

   public void getInverseMass (Matrix Minv, Matrix M) {
      throw new UnsupportedOperationException (
         "Inverse mass does not exist for "+getClass());
   }

   /**
    * {@inheritDoc}
    */
   public void addMasterAttachment (DynamicAttachment a) {
      if (myMasterAttachments == null) {
         myMasterAttachments = new LinkedList<DynamicAttachment>();
      }
      myMasterAttachments.add (a);
   }

   /**
    * {@inheritDoc}
    */
   public void removeMasterAttachment (DynamicAttachment a) {
      if (myMasterAttachments == null || !myMasterAttachments.remove (a)) {
         throw new InternalErrorException (
            "attempt to remove non-existent master attachment");
      }
      if (myMasterAttachments.size() == 0) {
         myMasterAttachments = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public LinkedList<DynamicAttachment> getMasterAttachments() {
      return myMasterAttachments;
   }
   
   public List<Constrainer> getConstrainers() {
      return myConstrainers;
   }
   
   public void addConstrainer (Constrainer c) {
      if (myConstrainers == null) {
         myConstrainers = new ArrayList<Constrainer>();
      }
      if (!myConstrainers.contains(c)) {
         myConstrainers.add (c);
      }
   }

   public boolean removeConstrainer (Constrainer c) {
      boolean removed = false;
      if (myConstrainers != null) {
         removed = myConstrainers.remove (c);
         if (removed && myConstrainers.size() == 0) {
            myConstrainers = null;
         }
      }
      return removed;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      DynamicComponentBase comp =
         (DynamicComponentBase)super.copy (flags, copyMap);
      comp.myAttachment = null;
      comp.myMasterAttachments = null;
      comp.mySolveIdx = -1;
      comp.myDynamicP = myDynamicP;
      comp.myConstrainers = null;
      return comp;
   }

   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }

   private class AttachmentUpdateAction implements TransformGeometryAction {

      DynamicAttachment myAttachment;

      AttachmentUpdateAction (DynamicAttachment a) {
         myAttachment = a;
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         myAttachment.updateAttachment();
      }     

      public int hashCode() {
         return myAttachment.hashCode();
      }

      public boolean equals (Object obj) {
         return (obj instanceof AttachmentUpdateAction &&
                 ((AttachmentUpdateAction)obj).myAttachment == myAttachment);
      }
   }

   private class SlaveUpdateAction extends AttachmentUpdateAction {

      SlaveUpdateAction (DynamicAttachment a) {
         super (a);
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         boolean allMastersTransforming =
            context.containsAll (myAttachment.getMasters());

         DynamicComponent slave = myAttachment.getSlave();
         if (allMastersTransforming && !gtr.isRigid()) {
            if (slave != null) {
               slave.transformGeometry (
                  gtr, context, flags | TransformableGeometry.TG_SIMULATING);
            }
            myAttachment.updateAttachment();
         }
         else if (!allMastersTransforming) {
            myAttachment.updateAttachment();
         }
         if (!context.containsAction (MechSystemBase.myAttachmentsPosAction)) {
            context.addAction (MechSystemBase.myAttachmentsPosAction);
         }
         if (slave != null) {
            context.addParentToNotify (slave.getParent());
         }
      } 
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
   }

   private void addAttachmentUpdateIfNeeded (
      DynamicAttachment a, GeometryTransformer gtr, 
      TransformGeometryContext context) {

      if (context.contains (a.getSlave())) {
         // the attachment slave is being transformed, and so request an update
         // for the attachment if the transform is non-rigid, or if all the
         // masters are not also being transformed.
         if (!gtr.isRigid() || !context.containsAll (a.getMasters())) {
            AttachmentUpdateAction action = new AttachmentUpdateAction (a);
            if (!context.containsAction (action)) {
               context.addAction (action);
            }
         }
      }
      else {
         // the attachment slave is not being transformed, so request an update
         // action for the slave if one has not already been added
         SlaveUpdateAction action = new SlaveUpdateAction (a);
         if (!context.containsAction (action)) {
            context.addAction (action);
         }
      }
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // transforming the geometry of this dynamic component will cause any
      // associated attachments and constraints to be updated if necessary,
      // *unless* we are simulating
      if ((flags & TransformableGeometry.TG_SIMULATING) != 0) {
         return;
      }
      DynamicAttachment as = getAttachment();
      if (as != null) {
         // this is a slave component; maybe update slave attachment 
         addAttachmentUpdateIfNeeded (as, gtr, context);
      }
      if (myMasterAttachments != null) {
         // master component for one of more attachments - update if needed
         for (DynamicAttachment am : myMasterAttachments) {
            addAttachmentUpdateIfNeeded (am, gtr, context);
         }
      }
      // for all TransformableGeometry constrainers involving this component,
      // call their transformGeometry() method before any of the constrained
      // components have been transformed. That will allow the constrainer to
      // request any update actions that are needed, using pretransform data if
      // necessary. The constrainer's transformGeometry() method is called even
      // if the constrainer itself is not being transformed, since it may still
      // have to request update actions. The constrainer can tell if it is
      // actually being transformed by calling context.contains() to see if it
      // is actually contained in the context.
      if (myConstrainers != null) {
         for (Constrainer c : myConstrainers) {
            if (c instanceof TransformableGeometry) {
               TransformableGeometry tgc = (TransformableGeometry)c;
               
               if (!context.isTransformed(tgc)) {
                  tgc.transformGeometry (gtr, context, flags);
                  // mark c as being transformed
                  context.markTransformed (tgc);
               }
            }
         }
      }
   }   

//    /**
//     * {@inheritDoc}
//     */
//    public void updateAttachmentVelStates() {
//    }

//    /**
//     * {@inheritDoc}
//     */
//    public void updateAttachmentPosStates() {
//    }

}
