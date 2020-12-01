/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.*;

/**
 * Reports changes in a component's dynamic state
 */
public class DynamicActivityChangeEvent extends ComponentChangeEvent {
   /**
    * Convenience class for reporting activity changes.
    */
   public static DynamicActivityChangeEvent defaultEvent =
      new DynamicActivityChangeEvent();

   public DynamicActivityChangeEvent (ModelComponent comp) {
      super (Code.DYNAMIC_ACTIVITY_CHANGED, comp, /*stateChanged=*/true);
   }

   public DynamicActivityChangeEvent (
      ModelComponent comp, boolean stateChanged) {
      super (Code.DYNAMIC_ACTIVITY_CHANGED, comp, stateChanged);
   }

   public DynamicActivityChangeEvent() {
      super (Code.DYNAMIC_ACTIVITY_CHANGED, null, /*stateChanged=*/true);
   }
}
