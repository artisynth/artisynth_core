/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.modelbase.*;

public class FemModelState extends NumericState {
   private static final long serialVersionUID = 1L;

   public FemModelState (int dsize, int zsize) {
      super (dsize, zsize);
   }

   public FemModelState() {
      super();
   }

   public void set (ComponentState state) {
      try {
         set ((FemModelState)state);
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException (
            "state to copy is not a FemModelState");
      }
   }
}
