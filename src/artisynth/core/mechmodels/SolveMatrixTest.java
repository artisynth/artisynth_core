/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;

public class SolveMatrixTest {

   int mySize;
   SparseNumberedBlockMatrix myS;
   MatrixNd myK;
   MatrixNd myKnumeric;

   public double testStiffness (MechSystem sys, double h) {
      myS = new SparseNumberedBlockMatrix();
      mySize = sys.getActivePosStateSize();
      myKnumeric = new MatrixNd (mySize, mySize);
      myK = new MatrixNd (mySize, mySize);

      int velSize = sys.getActiveVelStateSize();
      VectorNd u0 = new VectorNd (velSize);
      VectorNd usave = new VectorNd (velSize);

      VectorNd q0 = new VectorNd (mySize);
      VectorNd f0 = new VectorNd (mySize);
      VectorNd q = new VectorNd (mySize);
      VectorNd f = new VectorNd (mySize);
      sys.buildSolveMatrix (myS);

      // save old velocity, because we are going to zero the velocity
      // and we will need to restore the velocity
      sys.getActiveVelState (usave);
      sys.setActiveVelState (u0);

      // build numeric stiffness matrix
      sys.getActivePosState (q0);
      sys.updateForces (0);
      sys.getActiveForces (f0);
      for (int i=0; i<mySize; i++) {
         q.set (q0);
         q.add (i, h);
         sys.setActivePosState (q);
         sys.updateForces (0);
         sys.getActiveForces (f);
         f.sub (f0);
         f.scale (1/h);
         myKnumeric.setColumn (i, f);
      }
      sys.setActivePosState (q0);
      sys.updateForces (0);

      PointSpringBase.myIgnoreCoriolisInJacobian = true;

      sys.addPosJacobian (myS, null, 1);

      sys.setActiveVelState (usave);
      sys.updateForces (0);

      PointSpringBase.myIgnoreCoriolisInJacobian = true;

      MatrixNd Sdense = new MatrixNd (myS);
      Sdense.getSubMatrix (0, 0, myK);
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
      MatrixNd error = new MatrixNd (mySize, mySize);
      error.sub (myK, myKnumeric);
      return error;
   }
}