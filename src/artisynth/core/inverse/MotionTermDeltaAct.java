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

public class MotionTermDeltaAct extends MotionTerm
{

   public MotionTermDeltaAct (TrackingController opt) {
      super (opt);
   }

   VectorNd u_a = new VectorNd();
   VectorNd F = new VectorNd();
   
   double step = 1e-4;

   protected int getTerm(MatrixNd H, VectorNd b, int rowoff,
      double t0, double t1, VectorNd targetVel, VectorNd curVel,
      VectorNd wgts, SparseBlockMatrix Jm) {

      // round because results are very sensitive to h and we want to keep them
      // identical to earlier results when t0, t1 where given as nsec integers
      double h = TimeBase.round(t1 - t0);

      int tvelSize = targetVel.size();
      int velSize = curVel.size();
      int exSize = myController.numExcitations();

      if (Jm == null) 
         assert velSize == tvelSize;

      fa.setSize(velSize);
      Hu_j.setSize(velSize);
      Hm_j.setSize(tvelSize);
      Hm.setSize(tvelSize, exSize);

      u_a.setSize(velSize);
      ex.setSize(exSize);
      vbar.setSize(tvelSize);
      F.setSize(velSize);
      bf.setSize(velSize);
      ftmp.setSize(velSize);

      curEx.setSize(exSize);
      myController.getExcitations(curEx, 0);
      
      // update bounds for delta excitation
      myController.setOffsetBounds (curEx, 0d, 1d); 

      if (debug) {
         System.out.println("Integrator = " + myController.getIntegrator());
         System.out.println("h = " + h);
         System.out.println("targetVel = " + targetVel.toString(fmt));
         System.out.println("exPrev = " + curEx.toString(fmt));
         // Jm = motionTarget.getVelocityJacobian();
         // System.out.println("Jm = ["+Jm.toString(fmt)+" ]");
      }

      VectorNd Mv = new VectorNd (velSize);
      myMechSysSolver.updateStateSizes();
      myMechSysSolver.updateMassMatrix(t0);
      myMechSysSolver.mulActiveInertias(Mv, curVel);

      myController.updateForces(t1, F, ex);
      myController.updateConstraints(t1);
      myMechSysSolver.addMassForces(F, t0);

      // bf = M v + h fp
      bf.scaledAdd(h, F, Mv);

      if (useTrapezoidalSolver) {
         // use Trapezoidal integration
         myMechSysSolver.KKTFactorAndSolve (
            u_a, null, bf, /*tmp=*/ftmp, curVel, 
            h, -h/2, -h*h/4, -h/2, h*h/4);

      }
      else {
         // use ConstrainedBackwardEuler integration
         myMechSysSolver.KKTFactorAndSolve(
            u_a, null, bf, /*tmp=*/ftmp, curVel, h);
      }
      
//      if (TrackingController.isDebugTimestep (t0, t1)) {
//         System.out.println("b' = " + bf);
//         System.out.println("fp = " + fp);
//      }

      // vbar = v* - Jm u_k
      if (Jm != null) {
         Jm.mul(vbar, curVel, tvelSize, velSize);
      }
      else {
         vbar.set(curVel);
      }
      vbar.sub(targetVel, vbar);
            
      double delta = 1e-2;
      VectorNd utmp = new VectorNd (velSize);
      
      // Hm = Jm Hu
      // compute Hu: get column j of Hu by solving with RHS = fa(e_j) = f(e_j) - f(0)
      // where e_j is elementary unit vector
      for (int j = 0; j < exSize; j++)
      {
         double a_j = ex.get (j);
         ex.set(j, a_j + delta);

         myController.updateForces(t1, F, ex);
         myController.updateConstraints(t1);
         myMechSysSolver.addMassForces(F, t0);

         // bf = M v + h fp
         bf.scaledAdd(h, F, Mv);
         
         if (!useKKTFactorAndSolve) {    
            System.err.println("KKTSolve-only unsupported");
         }
            if (useTrapezoidalSolver) {
                // use Trapezoidal integration
                myMechSysSolver.KKTFactorAndSolve (
                   utmp, null, bf, /*tmp=*/ftmp, curVel, 
                   h, -h/2, -h*h/4, -h/2, h*h/4);
             }
             else {
                // use ConstrainedBackwardEuler integration
                myMechSysSolver.KKTFactorAndSolve(
                   utmp, null, bf, /*tmp=*/ftmp, curVel, h);
             }
            
//         }
//         else {
//            // use pre-factored KKT system
//            // Note neglecting change in jacobians due to excitation
////            myMechSysSolver.KKTSolve(Hu_j, lam, the, fa);
//         }
         
         
//         if (TrackingController.isDebugTimestep (t0, t1)) {
//            System.out.println("fa"+j+" = " + fa);            
//            System.out.println("Hu_"+j+" = " + Hu_j);
//         }
//         
           
            Hu_j.sub (utmp, u_a);
            Hu_j.scale (1/delta);

         // Hm_j = Jm Hu_j;
         if (Jm != null)
            Jm.mul(Hm_j, Hu_j, tvelSize, velSize);
         else
            Hm_j.set(Hu_j);

         Hm.setColumn(j, Hm_j.getBuffer());
         
         ex.set (j, a_j); // reset
      }

      if (useTimestepScaling) { // makes it independent of the time step
         Hm.scale(1/h);      
         vbar.scale(1/h); 
      }
      
       if (TrackingController.isDebugTimestep (t0, t1)) {
          System.out.println("dt = " + h + "    |Hm| = " + Hm.frobeniusNorm() + "    |vbar| = " + vbar.norm ());
          System.out.println("    vbar = " + vbar + " equals    targetVel = " + targetVel + "    minus    v0 = " + u_a );
       }
      /*END EDIT*/
      
      if (debug) {
         System.out.println("vCurrent = [" + curVel.toString(fmt) + "]';");
         System.out.println("vTarget = [" + targetVel.toString(fmt) + "]';");

         System.out.println("vbar = [" + vbar.toString(fmt) + "]';");
         System.out.println("Hm = [" + Hm.toString(fmt) + "]';");
      }
      
      if (normalizeH) {
          double fn = 1.0/Hm.frobeniusNorm();
          Hm.scale(fn);
          vbar.scale(fn);
       }
      
//      if (TrackingController.isDebugTimestep (t0, t1)) {
//         System.out.println("dt = " + h + " normalizeH   |Hm| = " + Hm.frobeniusNorm() + "    |vbar| = " + vbar.norm ());
//      }
      
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
   
}
