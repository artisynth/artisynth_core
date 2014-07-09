package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import maspack.matrix.*;

public class PlaneConstrainedFem extends FemBeam3d {

   public PlaneConstrainedFem() {
   }

   public PlaneConstrainedFem (String name) {
      // /*flags=*/VERTICAL|ADD_DISPLACEMENT);
      super (name, "hex", 1.0, 0.2, 4, 2, CONSTRAIN_RIGHT_NODES);
   }
}

