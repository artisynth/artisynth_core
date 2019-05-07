package artisynth.demos.fem;

import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.mechmodels.SolveMatrixTest;
import artisynth.core.materials.*;

import maspack.matrix.*;

public class TetBeam3d extends FemBeam3d {

   public void build (String[] args) {
      //Normal:
      build ("tet", 4, 2, /*options=*/ADD_MUSCLES);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      //SolveMatrixTest tester = new SolveMatrixTest();
      //System.out.println ("error=" + tester.testStiffness (myMechMod, 1e-8));

      return super.advance (t0, t1, flags);
   }


}
