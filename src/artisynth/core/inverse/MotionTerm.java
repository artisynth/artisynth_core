/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.util.NumberFormat;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.util.TimeBase;

public class MotionTerm
{
   public static final boolean DEFAULT_NORMALIZE = false;

   boolean debug = false;
   NumberFormat fmt = new NumberFormat("%g");

   TrackingController myController;
   MechSystemSolver myMechSysSolver;

   double weight = 1;

   public MotionTerm (TrackingController opt)
   {
      myController = opt;
      myMechSysSolver = new MechSystemSolver(opt.getMech());
   }

   public void dispose() {
      System.out.println("motion term dispose()");
      myMechSysSolver.dispose();
   }

   SparseBlockMatrix Jm;
   VectorNd Lambda_j = new VectorNd();
   VectorNd Hu_j = new VectorNd();
   VectorNd Hm_j = new VectorNd();
   MatrixNd Hm = new MatrixNd();
   VectorNd v = new VectorNd();
   VectorNd ex = new VectorNd();
   VectorNd vbar = new VectorNd();
   VectorNd f_passive = new VectorNd();
   VectorNd f = new VectorNd();
   VectorNd ftmp = new VectorNd();

   VectorNd lam = new VectorNd(0);
   VectorNd the = new VectorNd(0);

   VectorNd curEx = new VectorNd(0);

   // VectorNd curVel = new VectorNd(0);

   protected int getTerm(MatrixNd H, VectorNd b, int rowoff,
      double t0, double t1, VectorNd targetVel, VectorNd curVel, VectorNd wgts,
      SparseBlockMatrix Jm) {
      return getTerm(
         H, b, rowoff, t0, t1, targetVel, curVel, wgts, Jm, DEFAULT_NORMALIZE);
   }

   protected int getTerm(MatrixNd H, VectorNd b, int rowoff,
      double t0, double t1, VectorNd targetVel, VectorNd curVel,
      VectorNd wgts, SparseBlockMatrix Jm, boolean normalizeH) {

      // round because results are very sensitive to h and we want to keep them
      // identical to earlier results when t0, t1 where given as nsec integers
      double h = TimeBase.round(t1 - t0);

      int tvelSize = targetVel.size();
      int velSize = curVel.size();
      int exSize = myController.numExcitations();

      if (Jm == null) 
         assert velSize == tvelSize;

      Lambda_j.setSize(velSize);
      Hu_j.setSize(velSize);
      Hm_j.setSize(tvelSize);
      Hm.setSize(tvelSize, exSize);

      v.setSize(velSize);
      ex.setSize(exSize);
      vbar.setSize(tvelSize);
      f_passive.setSize(velSize);
      f.setSize(velSize);
      ftmp.setSize(velSize);

      curEx.setSize(exSize);
      myController.getExcitations(curEx, 0);

      if (debug) {
         System.out.println("Integrator = " + myController.getIntegrator());
         System.out.println("h = " + h);
         System.out.println("targetVel = " + targetVel.toString(fmt));
         System.out.println("exPrev = " + curEx.toString(fmt));
      }

      // Jm = motionTarget.getVelocityJacobian();
      // System.out.println("Jm = ["+Jm.toString(fmt)+" ]");

      // b = M v
      myMechSysSolver.updateStateSizes();
      myMechSysSolver.updateMassMatrix(t0);
      myMechSysSolver.mulActiveInertias(f, curVel);

      // passive forces with act = 0
      ex.setZero();
      myController.getForces(f_passive, ex);
      myController.updateConstraints(t1);
      myMechSysSolver.addMassForces(f_passive, t0);

      f.scaledAdd(h, f_passive, f);

      boolean useTrapezoidal = true;
      double hscale = 1;

      double hold = h;
      h *= hscale;

      if (useTrapezoidal) {
         // trapezoidal
         myMechSysSolver.KKTFactorAndSolve(
            v, null, f, /* tmp= */ftmp, curVel, t1, h,
            -h / 2, -h * h / 4, -h / 2, h * h / 4, /* velocitySolve= */true);
      }
      else {
         // backward euler
         myMechSysSolver.KKTFactorAndSolve(
            v, null, f, /*tmp=*/ftmp, curVel, t1,
            h, -h, -h*h, -h, 0, /*velocitySolve=*/true);
      }
      h = hold;

      // vbar = v* - Jm u0
      if (Jm != null) {
         Jm.mul(vbar, v, tvelSize, velSize);
      }
      else {
         vbar.set(v);
      }
      //System.out.println ("f_passive=" + f_passive);

      // vbar.mul(Jm, v);
      vbar.sub(targetVel, vbar);
      
      // Hm = Jm Hu
      // get column j of Hu by solving with RHS = Lambda * ej
      for (int j = 0; j < exSize; j++)
      {
         if (j > 0) {
            ex.set(j - 1, 0.0);
         }
         ex.set(j, 1.0);
         myController.getForces(Lambda_j, ex);
         Lambda_j.sub(f_passive); // subtract passive forces
         Lambda_j.scale(h);
         // if (j == 0) {
         // trapezoidal
         // myMechSysSolver.KKTFactorAndSolve(Hu_j, Lambda_j, /*tmp=*/ftmp,
         // null/*zero vel*/, t1,
         // -h/2, -h*h/4, -h/2, h*h/4, /*velocitySolve=*/true);
         // backward euler
         // myMechSysSolver.KKTFactorAndSolve(v, f, /*tmp=*/ftmp, curVel, t1,
         // -h, -h*h, -h, 0, /*velocitySolve=*/true);
         // }
         // else {

         myMechSysSolver.KKTSolve(Hu_j, lam, the, Lambda_j);
         // }

         // Jm.mul (Hm_j, Hu_j);
         if (Jm != null)
            Jm.mul(Hm_j, Hu_j, tvelSize, velSize);
         else
            Hm_j.set(Hu_j);

         Hm.setColumn(j, Hm_j.getBuffer());
      }

      if (debug) {
         System.out.println("vCurrent = [" + curVel.toString(fmt) + "]';");
         System.out.println("vTarget = [" + targetVel.toString(fmt) + "]';");

         System.out.println("vbar = [" + vbar.toString(fmt) + "]';");
         System.out.println("Hm_j = [" + Hm_j.toString(fmt) + "]';");
      }
      
      if (normalizeH) {
          double fn = 1.0/Hm.frobeniusNorm();
          Hm.scale(fn);
          vbar.scale(fn);
       }

      // apply weights
      if (wgts != null) {
         diagMul(wgts,Hm,Hm);
         pointMul(wgts,vbar,vbar);
      }
      if (weight > 0) {
          Hm.scale(weight);
          vbar.scale(weight);
       }

      H.setSubMatrix(rowoff, 0, Hm);
      b.setSubVector(rowoff, vbar);

      // reset excitations
      myController.setExcitations(curEx, 0);

      return rowoff + Hm.rowSize();
   }

   private void pointMul(VectorNd v1, VectorNd v2, VectorNd out) {
      assert(v1.size() == v2.size() && v2.size() == out.size());
      
      double [] v1buff = v1.getBuffer();
      double [] v2buff = v2.getBuffer();
      double [] outbuff = out.getBuffer();
      
      for (int i=0; i<v1.size(); i++) {
         outbuff[i] = v1buff[i]*v2buff[i];
      }
      
   }
   
   private void diagMul(VectorNd diag, MatrixNd m1, MatrixNd out) {
      assert(diag.size() == m1.rowSize() && 
         m1.rowSize() == out.rowSize() && 
         m1.colSize() == out.colSize());
      
      for (int i=0; i<diag.size(); i++) {
         double c = diag.get(i);
         for (int j=0; j<m1.colSize(); j++) {
            out.set(i,j, c*m1.get(i, j));
         }
      }
      
   }
   
   private void fprime(VectorNd ex, VectorNd currForce, int col, double da, VectorNd out) {

      double exi = ex.get(col);
      double exj = exi+da;
      if (exj > 1) {
         exj=1;
         da=exj-exi;
      }

      ex.set(col,exj);
      myController.getForces(out, ex);
      ex.set(col,exi);
      out.sub(currForce);
      out.scale((1.0/da));
   }

   public void setWeight(double w) {
      weight = w;
   }

}
