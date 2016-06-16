package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.demos.fem.FemBeam3d;
import maspack.matrix.*;

public class WedgeBeam3d extends FemBeam3d {

   public void build (String[] args) {
      super.build ("wedge", 1.0, 0.2, 6, 3, /*flags=*/0);
   }
   
}

