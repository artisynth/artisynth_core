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
   
   public static boolean DEFAULT_USE_TIMESTEP_SCALING = false;
   protected boolean useTimestepScaling = DEFAULT_USE_TIMESTEP_SCALING;
   
   public static boolean DEFAULT_USE_KKT_FACTORANDSOLVE = false;
   protected boolean useKKTFactorAndSolve = DEFAULT_USE_KKT_FACTORANDSOLVE;
   
   public static boolean DEFAULT_USE_TRAPEZOIDAL_SOLVER = false;
   protected boolean useTrapezoidalSolver = DEFAULT_USE_TRAPEZOIDAL_SOLVER;
   
   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;


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
   VectorNd fa = new VectorNd();
   VectorNd Hu_j = new VectorNd();
   VectorNd Hm_j = new VectorNd();
   MatrixNd Hm = new MatrixNd();
   VectorNd u0 = new VectorNd();
   VectorNd ex = new VectorNd();
   VectorNd vbar = new VectorNd();
   VectorNd fp = new VectorNd();
   VectorNd bf = new VectorNd();
   VectorNd ftmp = new VectorNd();

   VectorNd lam = new VectorNd(0);
   VectorNd the = new VectorNd(0);

   VectorNd curEx = new VectorNd(0);

   // VectorNd curVel = new VectorNd(0);

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

      u0.setSize(velSize);
      ex.setSize(exSize);
      vbar.setSize(tvelSize);
      fp.setSize(velSize);
      bf.setSize(velSize);
      ftmp.setSize(velSize);

      curEx.setSize(exSize);
      myController.getExcitations(curEx, 0);

      if (debug) {
         System.out.println("Integrator = " + myController.getIntegrator());
         System.out.println("h = " + h);
         System.out.println("targetVel = " + targetVel.toString(fmt));
         System.out.println("exPrev = " + curEx.toString(fmt));
         // Jm = motionTarget.getVelocityJacobian();
         // System.out.println("Jm = ["+Jm.toString(fmt)+" ]");
      }


      // bf = M v
      myMechSysSolver.updateStateSizes();
      myMechSysSolver.updateMassMatrix(t0);
      myMechSysSolver.mulActiveInertias(bf, curVel);

      // fp = passive forces with zero muscle activation
      ex.setZero();
      myController.getForces(fp, ex);
      myController.updateConstraints(t1);
      myMechSysSolver.addMassForces(fp, t0);

      // bf = M v + h fp
      bf.scaledAdd(h, fp, bf);

      if (useTrapezoidalSolver) {
         // use Trapezoidal integration
         myMechSysSolver.KKTFactorAndSolve (
            u0, null, bf, /*tmp=*/ftmp, curVel, 
            h, -h/2, -h*h/4, -h/2, h*h/4);

      }
      else {
         // use ConstrainedBackwardEuler integration
         myMechSysSolver.KKTFactorAndSolve(
            u0, null, bf, /*tmp=*/ftmp, curVel, h);
      }
      
//      if (TrackingController.isDebugTimestep (t0, t1)) {
//         System.out.println("b' = " + bf);
//         System.out.println("fp = " + fp);
//      }

      // vbar = v* - Jm u0
      if (Jm != null) {
         Jm.mul(vbar, u0, tvelSize, velSize);
      }
      else {
         vbar.set(u0);
      }
      vbar.sub(targetVel, vbar);
            
      // Hm = Jm Hu
      // compute Hu: get column j of Hu by solving with RHS = fa(e_j) = f(e_j) - f(0)
      // where e_j is elementary unit vector
      for (int j = 0; j < exSize; j++)
      {
         if (j > 0) {
            ex.set(j - 1, 0.0);
         }
         ex.set(j, 1.0);
         myController.getForces(fa, ex); 
         fa.sub (fa, fp);
         fa.scale (h);
         
         if (useKKTFactorAndSolve) {        
            if (useTrapezoidalSolver) {
                // use Trapezoidal integration
                myMechSysSolver.KKTFactorAndSolve (
                   Hu_j, null, fa, /*tmp=*/ftmp, curVel, 
                   h, -h/2, -h*h/4, -h/2, h*h/4);
             }
             else {
                // use ConstrainedBackwardEuler integration
                myMechSysSolver.KKTFactorAndSolve(
                   Hu_j, null, fa, /*tmp=*/ftmp, curVel, h);
             }     
         }
         else {
            // use pre-factored KKT system
            // Note neglecting change in jacobians due to excitation
            myMechSysSolver.KKTSolve(Hu_j, lam, the, fa);
         }
         
         
//         if (TrackingController.isDebugTimestep (t0, t1)) {
//            System.out.println("fa"+j+" = " + fa);            
//            System.out.println("Hu_"+j+" = " + Hu_j);
//         }
//         

         // Hm_j = Jm Hu_j);
         if (Jm != null)
            Jm.mul(Hm_j, Hu_j, tvelSize, velSize);
         else
            Hm_j.set(Hu_j);

         Hm.setColumn(j, Hm_j.getBuffer());
      }

      if (useTimestepScaling) { // makes it independent of the time step
         Hm.scale(1/h);      
         vbar.scale(1/h); 
      }
      
       if (TrackingController.isDebugTimestep (t0, t1)) {
          System.out.println("dt = " + h + "    |Hm| = " + Hm.frobeniusNorm() + "    |vbar| = " + vbar.norm ());
          System.out.println("    vbar = " + vbar + " equals    targetVel = " + targetVel + "    minus    v0 = " + u0 );
       }
      /*END EDIT*/
      
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

   public void getJacobian ( MatrixNd H ) {
      H.set (Hm);
   }
   
   public void getVbar ( VectorNd b ) {
      b.set (vbar);
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
