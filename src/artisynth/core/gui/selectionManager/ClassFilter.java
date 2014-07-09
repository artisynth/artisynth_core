/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.selectionManager;

import artisynth.core.modelbase.ModelComponent;
import java.util.List;

/**
 * A selection filter that accepts objects which are instances of a particular
 * class or interface.
 */
public class ClassFilter implements SelectionFilter {
   Class myClass;

   public ClassFilter (Class cls) {
      if (cls == null) {
         throw new IllegalArgumentException ("null class specified");
      }
      myClass = cls;
   }

   public boolean objectIsValid (
      ModelComponent comp, List<ModelComponent> currentSelections) {
      return myClass.isInstance (comp);
   }
}
