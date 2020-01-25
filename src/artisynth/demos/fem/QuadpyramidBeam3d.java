package artisynth.demos.fem;

import artisynth.core.materials.*;

import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.femmodels.FemModel3d;
import artisynth.demos.fem.FemBeam3d;

public class QuadpyramidBeam3d extends FemBeam3d {

   public void build (String[] args) {
      //super (name, "quadpyramid", 4, 2, /*options=*/0);
      super.build ("quadpyramid", 4, 2, VERTICAL|ADD_DISPLACEMENT);

      myFemMod.setMaterial (
         new MooneyRivlinMaterial (150000.0, 0, 0, 0, 0, 15000000.0));
      myFemMod.setIncompressible (FemModel3d.IncompMethod.OFF);
   }
}
