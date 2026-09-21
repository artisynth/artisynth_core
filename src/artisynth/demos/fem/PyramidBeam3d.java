package artisynth.demos.fem;

import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.demos.fem.FemBeam3d;

public class PyramidBeam3d extends FemBeam3d {

   public void build (String[] args) {
      //super (name, "tet", 1, 1, 
      //       /*options=*/ADD_MUSCLES | VERTICAL | ADD_DISPLACEMENT);
      super.build ("pyramid", 4, 2, /*options=*/0);
   }
}
