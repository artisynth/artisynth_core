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
 * Special reference component for source points that also returns the
 * associated target point as a hard reference. This ensures that the
 * reference will be removed if the target point is removed via the GUI.
 */
public class SourcePointReference extends ReferenceComp<Point> {

   public SourcePointReference () {
      this (null);
   }

   public SourcePointReference (Point ref) {
      super (ref);
   }

   private TargetPoint getTargetPoint() {
      CompositeComponent ccomp = getGrandParent();
      if (ccomp instanceof MotionTargetTerm) {
         MotionTargetTerm motionTerm = (MotionTargetTerm)ccomp;
         int idx = getParent().indexOf(this);
         if (motionTerm.myTargetPoints.size() > idx) {
            return motionTerm.myTargetPoints.get (idx);
         }
      }
      return null;

   }
   
   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      TargetPoint targetPoint = getTargetPoint();
      if (targetPoint != null) {
         refs.add (targetPoint);
      }
   }  
}

