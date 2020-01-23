package artisynth.demos.tutorial;

import java.io.IOException;

import maspack.matrix.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;

public class SimpleMuscleWithInlineProbe extends SimpleMuscleWithProbes {

   public void createInputProbe() throws IOException {

      NumericInputProbe p1probe =
         new NumericInputProbe (
            mech, "particles/p1:targetPosition", 0, 5);
      p1probe.addData (
         new double[] {
            0.0,  0.0, 0.0, 0.0,
            1.0,  0.5, 0.0, 0.5,
            2.0,  0.0, 0.0, 1.0,
            3.0, -0.5, 0.0, 0.5,
            4.0,  0.0, 0.0, 0.0 },
            NumericInputProbe.EXPLICIT_TIME);
      
      addInputProbe (p1probe);
   }
}
