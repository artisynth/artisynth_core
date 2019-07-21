package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import maspack.matrix.*;

import java.util.*;

public class ViscousBeam extends FemBeam3d {

   public void build (String[] args) {

      // NORMAL:
      //super (name, "hex", 1.0, 0.2, 4, 2, /*options=*/0);
      build ("hex", 1.0, 0.2, 4, 2, /*options=*/0);

      FemMaterial mat = new MooneyRivlinMaterial (50000.0, 0, 0, 0, 0, 2000000.0);
      //mat = new NeoHookeanMaterial (50000.0, 0.49);

      //myMechMod.setGravity (0, 0, 0);

      QLVBehavior qlv = new QLVBehavior();
      qlv.setTau (0.1, 0.0, 0, 0, 0, 0);
      qlv.setGamma (4.0, 0, 0, 0, 0, 0);      
      //qlv.setTau (0.1, 0.01, 0, 0, 0, 0);
      //qlv.setGamma (0.9, 0.2, 0, 0, 0, 0);

      ViscoelasticMaterial vmat = new ViscoelasticMaterial(mat, qlv);

      myFemMod.setMaterial (vmat);

      myFemMod.setStiffnessDamping (0);
      myFemMod.setParticleDamping (0);
      myMechMod.setIntegrator (Integrator.BackwardEuler);
      addWayPoint (1.0);
   }

   // public void build (String string, double d, double e, int i,
   //    int j, int k) {
   //    super.build (string,d,e,i,j,k);
   //    myFemMod.setMaterial (
   //       new MooneyRivlinMaterial (50000.0, 0, 0, 0, 0, 5000000.0));
   //    myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);
   // }

   public StepAdjustment advance (double t0, double t1, int flags) {
      SolveMatrixTest tester = new SolveMatrixTest();
      if (t0 > 0) {
         // System.out.println (
            //   "error=" + tester.testStiffness (myMechMod, 1e-8));
      }
      return super.advance (t0, t1, flags);
   }

//    public void advance (long t0, long t1) {
//       super.advance(t0, t1);
//       FemModel3d femMod = (FemModel3d)findComponent ("models/mech/models/fem");
//       SparseBlockMatrix S = femMod.getActiveStiffnessMatrix();
//       System.out.println ("S=" + S.rowSize()+"x"+S.colSize());
//    }
   
}

