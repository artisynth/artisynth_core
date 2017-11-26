package artisynth.demos.tutorial;

import maspack.matrix.*;
import maspack.util.Clonable;

import artisynth.core.workspace.RootModel;
import artisynth.core.probes.NumericMonitorProbe;
import artisynth.core.probes.DataFunction;

/**
 * Simple demo using a NumericMonitorProbe to generate sine and cosine waves.
 */
public class SinCosMonitorProbe extends RootModel {

   // Define the DataFunction that generates a sine and a cosine wave
   class SinCosFunction implements DataFunction, Clonable {
      
      public void eval (VectorNd vec, double t, double trel) {
         // vec should have size == 2, one for each wave
         vec.set (0, Math.sin (t));
         vec.set (1, Math.cos (t));
      }

      public Object clone() throws CloneNotSupportedException {
         return (SinCosFunction)super.clone();
      }
   }

   public void build (String[] args) {

      // Create a NumericMonitorProbe with size 2, file name "sinCos.dat", start
      // time 0, stop time 10, and a sample interval of 0.01 seconds:
      NumericMonitorProbe sinCosProbe =
         new NumericMonitorProbe (/*vsize=*/2, "sinCos.dat", 0, 10, 0.01);

      // then set the data function:
      sinCosProbe.setDataFunction (new SinCosFunction());
      addOutputProbe (sinCosProbe);
   }
}
