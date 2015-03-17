/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.*;

/**
 * Reports a change in a component's name.
 */
public class NameChangeEvent extends PropertyChangeEvent {
   private String myOldName;

   public NameChangeEvent (ModelComponent comp, String oldName) {
      super (Code.NAME_CHANGED, comp, "name");
      myOldName = oldName;
   };

   public String getOldName() {
      return myOldName;
   }
}
