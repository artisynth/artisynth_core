/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import maspack.matrix.Matrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.util.*;
import java.util.*;

public abstract class DynamicMechComponentBase extends RenderableComponentBase
implements DynamicMechComponent {
   DynamicAttachment myAttachment;
   LinkedList<DynamicAttachment> myMasterAttachments;

   boolean myDynamicP;
   // Activity myActivity;
   int mySolveIdx;

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
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
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
         DynamicMechComponent[] masters = getAttachment().getMasters();
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

   public void addMasterAttachment (DynamicAttachment a) {
      if (myMasterAttachments == null) {
         myMasterAttachments = new LinkedList<DynamicAttachment>();
      }
      myMasterAttachments.add (a);
   }

   public void removeMasterAttachment (DynamicAttachment a) {
      if (myMasterAttachments == null || !myMasterAttachments.remove (a)) {
         throw new InternalErrorException (
            "attempt to remove non-existent master attachment");
      }
      if (myMasterAttachments.size() == 0) {
         myMasterAttachments = null;
      }
   }

   public LinkedList<DynamicAttachment> getMasterAttachments() {
      return myMasterAttachments;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      DynamicMechComponentBase comp =
         (DynamicMechComponentBase)super.copy (flags, copyMap);
      comp.myAttachment = null;
      comp.myMasterAttachments = null;
      comp.mySolveIdx = -1;
      comp.myDynamicP = myDynamicP;
      return comp;
   }

   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
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
