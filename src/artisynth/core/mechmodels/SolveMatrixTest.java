/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ComponentState;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.util.NumberFormat;

public class SolveMatrixTest {

   int myVsize;
   int myQsize;
   SparseNumberedBlockMatrix myS;
   MatrixNd myK;
   MatrixNd myKnumeric;
   boolean myUseYPRStiffness = false;

   public boolean getYPRStiffness() {
      return myUseYPRStiffness;
   }

   public void setYPRStiffness (boolean enable) {
      myUseYPRStiffness = enable;
   }

   public double testStiffness (MechSystemBase sys, double h) {
      return testStiffness (sys, h, null);
   }         

   public double testStiffness (
      MechSystemBase sys, double h, boolean printMatrices) {
      return testStiffness (sys, h, printMatrices ? "%8.3f" : null);
   }

   private void convertToYPRForces (VectorNd f, MechModel mech) {
   }

   private void getActiveForces (VectorNd f, MechSystemBase sys) {
      sys.getActiveForces (f);
      if (myUseYPRStiffness && sys instanceof MechModel) {
         YPRStiffnessUtils.convertActiveForcesToYPR (f, f, (MechModel)sys);
      }
   }

   private void applyActiveImpulse (
      MechSystemBase sys, VectorNd q0, VectorNd uimp, double h) {
      if (myUseYPRStiffness && sys instanceof MechModel) {
         sys.setActivePosState (q0);
         YPRStiffnessUtils.convertActiveVelocitiesFromYPR (
            uimp, uimp, (MechModel)sys); 
      }
      VectorNd q = new VectorNd (q0);
      sys.addActivePosImpulse (q, h, uimp);
      sys.setActivePosState (q);
   }

   public double testStiffness (
      MechSystemBase sys, double h, String fmtStr) {

      SparseBlockMatrix S = new SparseNumberedBlockMatrix();
      myVsize = sys.getActiveVelStateSize();
      myQsize = sys.getActivePosStateSize();
      //int qsize = sys.getActivePosStateSize();
      myKnumeric = new MatrixNd (myVsize, myVsize);
      myK = new MatrixNd (myVsize, myVsize);

      VectorNd u0 = new VectorNd (myVsize);
      VectorNd q0 = new VectorNd (myQsize);
      VectorNd f0 = new VectorNd (myVsize);
      VectorNd uimp = new VectorNd (myVsize);
      VectorNd q = new VectorNd (myQsize);
      VectorNd f = new VectorNd (myVsize);

      ComponentState savestate = sys.createState (null);
      savestate.setAnnotated(true);
      // save old state, because we are going to zero the velocity and advance
      // the state and we will need to restore
      sys.getState (savestate);
      sys.setActiveVelState (u0);

      // build numeric stiffness matrix
      sys.getActivePosState (q0);
      sys.updateForces (0);
      getActiveForces (f0, sys);
      //System.out.println ("f0=  " + f0.toString("%16.6f"));

      // the aux state code is necessary to handle situations involving
      // state-bearing force effectors like viscous materials
      sys.advanceState (0, h);

      for (int i=0; i<myVsize; i++) {
         // increment position by a small amount
         uimp.setZero();
         uimp.set (i, 1);
         applyActiveImpulse (sys, q0, uimp, h);
         // q.set (q0);
         // sys.addActivePosImpulse (q, h, uimp);
         // sys.setActivePosState (q);

         sys.updateForces (h);
         getActiveForces (f, sys);
         //System.out.println ("f["+i+"]=" + f.toString("%16.6f"));
         f.sub (f0);
         f.scale (1/h);
         //System.out.println ("df=" + f);
         myKnumeric.setColumn (i, f);
      }
      sys.setActivePosState (q0);
      sys.updateForces (0);

      boolean saveIgnoreCoriolis = PointSpringBase.myIgnoreCoriolisInJacobian;
      boolean saveSymmetricJacobian = FrameSpring.mySymmetricJacobian;
      boolean saveAddFrameMarkerStiffness = false;
      PointSpringBase.myIgnoreCoriolisInJacobian = false;
      FrameSpring.mySymmetricJacobian = false;
      if (sys instanceof MechModel) {
         saveAddFrameMarkerStiffness =
            ((MechModel)sys).getAddFrameMarkerStiffness();
         ((MechModel)sys).setAddFrameMarkerStiffness(true);
      }
      
      
      if (myUseYPRStiffness && sys instanceof MechModel) {
         S = ((MechModel)sys).getYPRStiffnessMatrix(null);
      }
      else {
         S = sys.getActiveStiffnessMatrix ();
      }

      // restore entire system state
      sys.setState (savestate);

      MatrixNd Sdense = new MatrixNd (S);
      Sdense.getSubMatrix (0, 0, myK);

      double norm = Math.max (myK.infinityNorm(), myKnumeric.infinityNorm());
      if (fmtStr != null) {
         NumberFormat fmt = new NumberFormat (fmtStr);
         System.out.println ("K=\n" + myK.toString (fmt));
         if (myUseYPRStiffness) {
            double symerr = symmetryError(myKnumeric);
            if (symerr >= 1e-6) {
               System.out.println (
                  "WARNING: numeric stiffness is not symmetric, error=" + symerr);
            }
         }
         System.out.println ("Knumeric=\n" + myKnumeric.toString (fmt));
         MatrixNd E = new MatrixNd (myK);
         E.sub (myKnumeric);
         System.out.println ("Err=\n" + E.toString (fmt));
         MatrixNd ET = new MatrixNd ();
         ET.transpose (E);
         E.sub (ET);
         System.out.println (
            "SymErr=" + E.infinityNorm()/norm + "\n" + E.toString (fmt));
      }
      
      if (sys instanceof MechModel) {
         ((MechModel)sys).setAddFrameMarkerStiffness(saveAddFrameMarkerStiffness);
      }
      PointSpringBase.myIgnoreCoriolisInJacobian = saveIgnoreCoriolis;
      FrameSpring.mySymmetricJacobian = saveSymmetricJacobian;

      return getKerror().infinityNorm()/norm;
   }

   private double symmetryError (MatrixNd M) {
      double norm = M.infinityNorm();
      MatrixNd MT = new MatrixNd();
      MT.transpose (M);
      MT.sub (M);
      if (norm != 0) {
         return MT.infinityNorm()/norm;
      }
      else {
         return MT.infinityNorm();
      }
   }

   public MatrixNd getK() {
      return myK;
   }

   public MatrixNd getKnumeric() {
      return myKnumeric;
   }

   public MatrixNd getKerror() {
      MatrixNd error = new MatrixNd (myVsize, myVsize);
      error.sub (myK, myKnumeric);
      return error;
   }
}
