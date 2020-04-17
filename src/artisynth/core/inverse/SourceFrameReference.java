/**
 * Copyright (c) 2020, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.util.List;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

/**
 * Special reference component for source frames that also returns the
 * associated target frame as a hard reference. This ensures that the
 * reference will be removed if the target frame is removed via the GUI.
 */
public class SourceFrameReference extends ReferenceComp<Frame> {

   public SourceFrameReference () {
      this (null);
   }

   public SourceFrameReference (Frame ref) {
      super (ref);
   }

   private TargetFrame getTargetFrame() {
      CompositeComponent ccomp = getGrandParent();
      if (ccomp instanceof MotionTargetTerm) {
         MotionTargetTerm motionTerm = (MotionTargetTerm)ccomp;
         int idx = getParent().indexOf(this);
         if (motionTerm.myTargetFrames.size() > idx) {
            return motionTerm.myTargetFrames.get (idx);
         }
      }
      return null;

   }
   
   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      TargetFrame targetFrame = getTargetFrame();
      if (targetFrame != null) {
         refs.add (targetFrame);
      }
   }  
}

