package artisynth.demos.fem;

import artisynth.core.materials.*;
import artisynth.core.femmodels.FemModel3d;
import artisynth.demos.fem.FemBeam3d;

public class QuadwedgeBeam3d extends FemBeam3d {

   public void build (String[] args) {
      super.build ("quadwedge", 4, 2, 0);
      //super (name, "quadwedge", 4, 2, /*options=*/VERTICAL | ADD_DISPLACEMENT);

      //super (name, "quadhex", 4, 2, 0); //  /*options=*/ADD_MUSCLES);

      myFemMod.setMaterial (
         new MooneyRivlinMaterial (150000.0, 0, 0, 0, 0, 15000000.0));
      myFemMod.setIncompressible (FemModel3d.IncompMethod.AUTO);
   }
}
