/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import artisynth.core.modelbase.ModelComponent;

public class InputProbe extends Probe {
   // protected ModelComponent myElement;

   public InputProbe() {
      this (null);
   }

   public InputProbe (ModelComponent e) {
      if (e != null) {
         setModelFromComponent (e);
      }
      setStartTime (0);
      setStopTime (0);
      setUpdateInterval (-1);
      setActive (true);
   }

   public void apply (double t) {
   }

   /**
    * {@inheritDoc}
    */
   public boolean isInput() {
      return true;
   }

}
