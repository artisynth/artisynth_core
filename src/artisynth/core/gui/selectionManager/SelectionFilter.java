/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.selectionManager;

import java.util.List;

import artisynth.core.modelbase.ModelComponent;

public interface SelectionFilter {
   public boolean objectIsValid (
      ModelComponent c, List<ModelComponent> currentSelections);
}
