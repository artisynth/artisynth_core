/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.*;

/**
 * Reports changes in the structure of the component hierarchy.
 */
public class StructureChangeEvent extends ComponentChangeEvent {
 
   public static StructureChangeEvent defaultEvent =
      new StructureChangeEvent();

   public static StructureChangeEvent defaultStateNotChangedEvent =
      new StructureChangeEvent(false);

   public StructureChangeEvent (ModelComponent comp, boolean stateIsChanged) {
      super (Code.STRUCTURE_CHANGED, comp, stateIsChanged);
   }

   public StructureChangeEvent (ModelComponent comp) {
      this (comp, true);
   }

   public StructureChangeEvent (boolean stateIsChanged) {
      this (null, stateIsChanged);
   }

   public StructureChangeEvent() {
      this (null, /*stateIsChanged=*/true);
   }
}
