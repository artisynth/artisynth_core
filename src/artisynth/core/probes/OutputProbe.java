/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

public class OutputProbe extends Probe {
   public OutputProbe() {
      setStartTime (0);
      setStopTime (0);
      setUpdateInterval (-1);
      setActive (true);
   }

   public void apply (double t) {
   }
}
