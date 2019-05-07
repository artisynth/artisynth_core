/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;

public class MechModelState extends NumericState {
   
   public MechModelState (int dsize, int zsize) {
      super (zsize, dsize);
   }

   public MechModelState() {
      super();
   }

   public void set (ComponentState state) {
      try {
         set ((MechModelState)state);
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException (
            "state to copy is not a MechModelState");
      }
   }
}
