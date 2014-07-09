package artisynth.demos.fem;

import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.mechmodels.SolveMatrixTest;
import artisynth.core.materials.*;

import maspack.matrix.*;

public class TetBeam3d extends FemBeam3d {

   public TetBeam3d () {
   }    

   public TetBeam3d (String name) {
      //Normal:
      super (name, "tet", 4, 2, /*options=*/ADD_MUSCLES);

      //super (name, "tet", 4, 2, 0);
      //LinearMaterial lmat = new LinearMaterial (100000, 0.33);
      //myFemMod.setMaterial (lmat);

      // 40,000 tet example: 
      //super (name, "tet", 5, 5, 5, 20, 20, 20, /*options=*/0);
      //myMechMod.setProfiling (true);

      
      // 135,000 tet example: 
      //super (name, "tet", 5, 5, 5, 30, 30, 30, /*options=*/0);
      //myMechMod.setProfiling (true);

      // MooneyRivlinMaterial monMat = new MooneyRivlinMaterial();
      // monMat.setBulkModulus (5000000);
      // monMat.setC10 (50000);
      // myFemMod.setMaterial (monMat);

      // myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);

      // SolveMatrixTest tester = new SolveMatrixTest();
      // System.out.println ("error=" + tester.testStiffness (myMechMod, 1e-8));
      // System.out.println ("K=\n" + tester.getK().toString ("%12.1f"));
      // System.out.println ("N=\n" + tester.getKnumeric().toString ("%12.1f"));
      // System.out.println ("E=\n" + tester.getKerror().toString ("%12.1f"));

      // int size = myMechMod.getActivePosStateSize();

      // VectorNd vel0 = new VectorNd (size);
      // VectorNd vel = new VectorNd (size);
      // RandomGenerator.setSeed (0x1234);
      // vel.setRandom();

      // myMechMod.getActiveVelState (vel0);
      // myMechMod.setActiveVelState (vel);

      // VectorNd dg = new VectorNd();
      // SparseBlockMatrix GT = new SparseBlockMatrix();
      // myMechMod.updateConstraints (0, null, 0);
      // myMechMod.getBilateralConstraints (GT, dg);
      // MatrixNd GTdense = new MatrixNd (GT);
      // System.out.println ("GT=\n" + GTdense.toString ("%8.5f"));
      // System.out.println ("dg=" + dg.toString ("%8.5f"));


      // VectorNd q0 = new VectorNd (size);
      // VectorNd q = new VectorNd (size);
      // VectorNd col0 = new VectorNd (GT.rowSize());
      // VectorNd col = new VectorNd (GT.rowSize());
      // MatrixNd DGT = new MatrixNd (GT.rowSize(), size);
      // double h = 1e-8;
      // myMechMod.getActivePosState (q0);
      
      // for (int k=0; k<4; k++) {

      //    GT.getColumn (k, col0);
      //    for (int i=0; i<size; i++) {
      //       q.set (q0);
      //       q.add (i, h);
      //       myMechMod.setActivePosState (q);
      //       myMechMod.updateConstraints (i+1, null, 0);
      //       GT = new SparseBlockMatrix();
      //       myMechMod.getBilateralConstraints (GT, dg);
      //       GT.getColumn (k, col);
      //       col.sub (col0);
      //       col.scale (1/h);
      //       DGT.setColumn (i, col);
      //    }      
      //    myMechMod.getActivePosState (q0);

      //    //System.out.println ("DGT=\n" + DGT.toString ("%11.8f"));

      //    VectorNd tmp = new VectorNd (size);
      //    DGT.mul (tmp, vel);
      //    tmp.setSize (size);
      //    System.out.println ("dg("+k+")=" + tmp.dot(vel));
      // }
      
      // myMechMod.setActiveVelState (vel0);     
      

   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      //SolveMatrixTest tester = new SolveMatrixTest();
      //System.out.println ("error=" + tester.testStiffness (myMechMod, 1e-8));

      return super.advance (t0, t1, flags);
   }


}
