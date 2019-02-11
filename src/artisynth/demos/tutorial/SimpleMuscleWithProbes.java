package artisynth.demos.tutorial;

import java.io.IOException;
import maspack.matrix.*;
import maspack.util.PathFinder;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.probes.*;

public class SimpleMuscleWithProbes extends SimpleMuscleWithPanel
{
   public void createInputProbe() throws IOException {
      NumericInputProbe p1probe =
         new NumericInputProbe (
            mech, "particles/p1:targetPosition",
            PathFinder.getSourceRelativePath (this, "simpleMuscleP1Pos.txt"));
      p1probe.setName("Particle Position");
      addInputProbe (p1probe);
   }

   public void createOutputProbe() throws IOException {
      NumericOutputProbe mkrProbe =
         new NumericOutputProbe (
            mech, "frameMarkers/0:velocity",
            PathFinder.getSourceRelativePath (this, "simpleMuscleMkrVel.txt"),
            0.01);
      mkrProbe.setName("FrameMarker Velocity");
      mkrProbe.setDefaultDisplayRange (-4, 4);
      mkrProbe.setStopTime (10);
      addOutputProbe (mkrProbe);
   }

   public void build (String[] args) throws IOException {
      super.build (args);

      createInputProbe ();
      createOutputProbe ();
      mech.setBounds (-1, 0, -1, 1, 0, 1);
   }

}
