/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.selectionManager;

import java.util.List;

import artisynth.core.modelbase.ModelComponent;

public class NoChildFilter implements SelectionFilter {
   /**
    * Returns true is compA is an ancestors of compB
    */
   protected boolean isAncestor (ModelComponent compA, ModelComponent compB) {
      for (ModelComponent parent = compB.getParent(); parent != null; parent =
         parent.getParent()) {
         if (parent == compA) {
            return true;
         }
      }
      return false;
   }

   public boolean objectIsValid (
      ModelComponent comp, List<ModelComponent> currentSelections) {
      for (ModelComponent c : currentSelections) {
         if (isAncestor (c, comp)) {
            return false;
         }
      }
      return true;
   }
}
