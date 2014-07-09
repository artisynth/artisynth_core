/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import artisynth.core.modelbase.*;
import artisynth.core.probes.OutputProbe;
import artisynth.core.util.*;

public class SleepProbe extends OutputProbe {
   private double cpuLoad;
   private double SEC_TO_MSEC = 1000;

   public SleepProbe (double interval, double cpuLoad) {
      super();
      this.cpuLoad = cpuLoad;
      setStopTime (Double.POSITIVE_INFINITY);
      setUpdateInterval (interval);
   }

   public void apply (double t) {
      try {
         Thread.sleep (
            (int)((1 - cpuLoad) * getUpdateInterval() * SEC_TO_MSEC));
      }
      catch (Exception e) {
      }
   }
}
