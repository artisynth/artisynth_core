package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.demos.fem.FemBeam3d;
import maspack.matrix.*;

public class QuadtetBeam3d extends FemBeam3d {

   public void build (String[] args) {
      super.build ("quadtet", 1.0, 0.2, 2, 1, /*flags=*/0);
      //super (name, "quadtet", 1.0, 1.0, 4, 4,/*flags=*/VERTICAL);

      myFemMod.setMaterial (
         new MooneyRivlinMaterial (150000.0, 0, 0, 0, 0, 15000000.0));
      myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);

      //addBreakPoint (2.64);
   }
   
}
