/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ComponentState;
import maspack.matrix.*;
import maspack.util.*;

public class SolveMatrixTest {

   int myVsize;
   int myQsize;
   SparseNumberedBlockMatrix myS;
   MatrixNd myK;
   MatrixNd myKnumeric;

   public double testStiffness (MechSystemBase sys, double h) {
      return testStiffness (sys, h, null);
   }         

   public double testStiffness (
      MechSystemBase sys, double h, boolean printMatrices) {
      return testStiffness (sys, h, printMatrices ? "%8.3f" : null);
   }

   public double testStiffness (
      MechSystemBase sys, double h, String fmtStr) {

      myS = new SparseNumberedBlockMatrix();
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
      sys.buildSolveMatrix (myS);

      ComponentState savestate = sys.createState (null);
      savestate.setAnnotated(true);
      // save old state, because we are going to zero the velocity and advance
      // the state and we will need to restore
      sys.getState (savestate);
      sys.setActiveVelState (u0);

      // build numeric stiffness matrix
      sys.getActivePosState (q0);
      sys.updateForces (0);
      sys.getActiveForces (f0);
      //System.out.println ("f0=  " + f0.toString("%16.6f"));

      // the aux state code is necessary to handle situations involving
      // state-bearing force effectors like viscous materials
      sys.advanceState (0, h);

      for (int i=0; i<myVsize; i++) {
         // increment position by a small amount
         uimp.setZero();
         uimp.set (i, 1);
         q.set (q0);
         sys.addActivePosImpulse (q, h, uimp);
         sys.setActivePosState (q);

         sys.updateForces (h);
         sys.getActiveForces (f);
         //System.out.println ("f["+i+"]=" + f.toString("%16.6f"));
         f.sub (f0);
         f.scale (1/h);
         //System.out.println ("df=" + f);
         myKnumeric.setColumn (i, f);
      }
      sys.setActivePosState (q0);
      sys.updateForces (0);

      boolean saveIgnoreCoriolis = PointSpringBase.myIgnoreCoriolisInJacobian;
      PointSpringBase.myIgnoreCoriolisInJacobian = false;

      sys.addPosJacobian (myS, null, 1);

      // restore entire system state
      sys.setState (savestate);

      MatrixNd Sdense = new MatrixNd (myS);
      Sdense.getSubMatrix (0, 0, myK);

      if (fmtStr != null) {
         NumberFormat fmt = new NumberFormat (fmtStr);
         System.out.println ("K=\n" + myK.toString (fmt));
         System.out.println ("Knumeric=\n" + myKnumeric.toString (fmt));
         MatrixNd E = new MatrixNd (myK);
         E.sub (myKnumeric);
         System.out.println ("Err=\n" + E.toString (fmt));
         MatrixNd ET = new MatrixNd ();
         ET.transpose (E);
         E.sub (ET);
         System.out.println ("SymErr=\n" + E.toString (fmt));
      }

      PointSpringBase.myIgnoreCoriolisInJacobian = saveIgnoreCoriolis;

      double norm = Math.max (myK.infinityNorm(), myKnumeric.infinityNorm());
      return getKerror().infinityNorm()/norm;
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
