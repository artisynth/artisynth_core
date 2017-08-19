/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.util.TimeBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.util.NumberFormat;

public class MotionForceInverseData
{
   public static final boolean DEFAULT_NORMALIZE = false;

   boolean debug = false;
   NumberFormat fmt = new NumberFormat("%g");

   TrackingController myController;
   MechSystemSolver myMechSysSolver;
   MechSystem myMech;
   
   public static boolean DEFAULT_USE_TIMESTEP_SCALING = false;
   protected boolean useTimestepScaling = DEFAULT_USE_TIMESTEP_SCALING;
   
   public static boolean DEFAULT_USE_KKT_FACTORANDSOLVE = false;
   protected boolean useKKTFactorAndSolve = DEFAULT_USE_KKT_FACTORANDSOLVE;
   
   public static boolean DEFAULT_USE_TRAPEZOIDAL_SOLVER = false;
   protected boolean useTrapezoidalSolver = DEFAULT_USE_TRAPEZOIDAL_SOLVER;
   
   public static boolean DEFAULT_NORMALIZE_H = false;
   protected boolean normalizeH = DEFAULT_NORMALIZE_H;

   public MotionForceInverseData (TrackingController opt)
   {
      myController = opt;
      myMech = opt.getMech ();
      myMechSysSolver = new MechSystemSolver(myMech);

   }

   SparseBlockMatrix Jm;
   VectorNd fa = new VectorNd();
   VectorNd Hu_j = new VectorNd();
   VectorNd Hm_j = new VectorNd();
   MatrixNd Hv = new MatrixNd();
   MatrixNd Hu = new MatrixNd();
   VectorNd u0 = new VectorNd();
   VectorNd ex = new VectorNd();
   VectorNd v0 = new VectorNd();
   VectorNd fp = new VectorNd();
   VectorNd bf = new VectorNd();
   VectorNd ftmp = new VectorNd();

   VectorNd lam = new VectorNd(0);
   VectorNd lam0 = new VectorNd(0);
   VectorNd c0 = new VectorNd();
   VectorNd Hlam_j = new VectorNd();
   VectorNd Hc_j = new VectorNd();
   MatrixNd Hlam = new MatrixNd();
   MatrixNd Hc = new MatrixNd();
   
   VectorNd the = new VectorNd(0);

   VectorNd curEx = new VectorNd(0);
   VectorNd curVel = new VectorNd(0);
   
   protected void update(double t0, double t1, SparseBlockMatrix Jm, SparseBlockMatrix Jc) {

      // round because results are very sensitive to h and we want to keep them
      // identical to earlier results when t0, t1 where given as nsec integers
      double h = TimeBase.round(t1 - t0);
      
      if (myMech instanceof MechModel) {
         useTrapezoidalSolver = ((MechModel)myMech).getIntegrator ()==Integrator.Trapezoidal;
      }

      int velSize = myMech.getActiveVelStateSize ();
//      int conSize = myMech
      int exSize = myController.numExcitations();
      
      fa.setSize(velSize);
      Hu_j.setSize(velSize);
      Hu.setSize (velSize, exSize);

      u0.setSize(velSize);
      ex.setSize(exSize);
      fp.setSize(velSize);
      bf.setSize(velSize);
      ftmp.setSize(velSize);

      if (Jm != null) {
         int tvelSize = Jm.rowSize ();
         v0.setSize(tvelSize);
         Hm_j.setSize(tvelSize);
         Hv.setSize(tvelSize, exSize);
      } 
      
      if (Jc != null) {
         int tconSize = Jc.rowSize ();
         c0.setSize(tconSize);
         Hc_j.setSize(tconSize);
         Hc.setSize(tconSize, exSize);
      } 
      
      curVel.setSize (velSize);
      myMech.getActiveVelState (curVel);
      
      curEx.setSize(exSize);
      myController.getExcitations(curEx, 0);

      if (debug) {
         System.out.println("Integrator = " + myController.getIntegrator());
         System.out.println("h = " + h);
//         System.out.println("targetVel = " + targetVel.toString(fmt));
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
      myController.updateConstraints(t1);
      myController.updateForces(t1, fp, ex);
      myMechSysSolver.addMassForces(fp, t0);

      // bf = M v + h fp
      bf.scaledAdd(h, fp, bf);
      
      /* debug info */
      if (myController.getDebug ()) {
         System.out.println ("(MotionForceInverseData - PreKKTSolve)");
         System.out.println ("\tbf: " + bf.toString ("%.3f"));
         System.out.println ("\tftmp: " + ftmp.toString ("%.3f"));
         System.out.println ("\tcurVel: " + curVel.toString ("%.3f"));
         System.out.println ("\th: " + h);
      }

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
      
      /* debug info */
      if (myController.getDebug ()) {
         System.out.println ("(MotionForceInverseData - PostKKTSolve)");
         System.out.println ("\tv0: " + v0.toString ("%.3f"));
         System.out.println ("\tu0: " + u0.toString ("%.3f"));
         System.out.println ("\tftmp: " + ftmp.toString ("%.3f"));         
      }
      
      if (Jm != null) {
         if (myController.getDebug ()) {
            System.out.println ("\n(MotionForceInverseData)\nJm:\n" + Jm.toString("%.3f"));
         }
         Jm.mul(v0, u0, Jm.rowSize (), velSize);
      }
      else {
         v0.set(u0);
      }
      // XXX now done in motion target term
//      v0.sub(targetVel, v0);
       
      lam0.set (myMechSysSolver.getLambda ());
      if (Jc != null) {
         
         /* debug info */
         if (myController.getDebug()) {
//            System.out.println ("(MotionForceInverseData)");
//            System.out.println("\tlam0 size = "+lam0.size ());
//            System.out.println("\ttforSize = "+Jc.rowSize ());
//            System.out.println("\tJc:\n"+Jc);
         }
         
         Jc.mul(c0, lam0);
      }
      else {
         c0.set(lam0);
      }
      
      
      
      // Hm = Jm Hu
      // compute Hu: get column j of Hu by solving with RHS = fa(e_j) = f(e_j) - f(0)
      // where e_j is elementary unit vector
      for (int j = 0; j < exSize; j++)
      {
         if (j > 0) {
            ex.set(j - 1, 0.0);
         }
         ex.set(j, 1.0);
         // XXX scale excitation value by weight??
         
         myController.updateForces(t1, fa, ex);
         
         // XXX scale fa by excitation weight??
         
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
            lam = myMechSysSolver.getLambda ();
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
         Hu.setColumn (j, Hu_j.getBuffer ());
         // Hm_j = Jm Hu_j;
         if (Jm != null)
            Jm.mul(Hm_j, Hu_j, Jm.rowSize (), velSize);
         else
            Hm_j.set(Hu_j);

         Hv.setColumn(j, Hm_j.getBuffer());
         
         Hlam.setColumn (j, lam.getBuffer ());
         // Hc_j = Jc H_lambda_j
         if (Jc != null) {
            Jc.mul (Hc_j,lam);
         }
         else {
            Hc_j.set(lam);
         }
         Hc.setColumn(j,Hc_j.getBuffer());    
      }

      // XXX now done in motion target term
//      if (useTimestepScaling) { // makes it independent of the time step
//         Hv.scale(1/h);      
//         v0.scale(1/h); 
//      }
      
       if (myController.getDebug()) {
          System.out.printf("(MotionForceInverseData)\n\tdt = %.3f" + "    |Hv| = %.3f" + "    |vbar| = %.3f\n",
             h, Hv.frobeniusNorm(), v0.norm ());
       }
      /*END EDIT*/
      
      if (debug) {
         System.out.println("vCurrent = [" + curVel.toString(fmt) + "]';");
//         System.out.println("vTarget = [" + targetVel.toString(fmt) + "]';");

         System.out.println("vbar = [" + v0.toString(fmt) + "]';");
         System.out.println("Hm_j = [" + Hm_j.toString(fmt) + "]';"); 
         System.out.println("Hc = [\n" + Hc.toString("%02.1f") + "]';");
         System.out.println("c0 = [\n" + c0.toString("%02.4f") + "]';");
      }
      
      // XXX now done in motion target term
//      if (normalizeH) {
//          double fn = 1.0/Hv.frobeniusNorm();
//          Hv.scale(fn);
//          v0.scale(fn);
//       }
      
      // XXX rest now done in motion target term

      
      // reset excitations
      myController.setExcitations(curEx, 0);
   }
   
   public static void pointMul(VectorNd v1, VectorNd v2, VectorNd out) {
      assert(v1.size() == v2.size() && v2.size() == out.size());
      
      double [] v1buff = v1.getBuffer();
      double [] v2buff = v2.getBuffer();
      double [] outbuff = out.getBuffer();
      
      for (int i=0; i<v1.size(); i++) {
         outbuff[i] = v1buff[i]*v2buff[i];
      }      
   }
   
   public static void diagMul(VectorNd diag, MatrixNd m1, MatrixNd out) {
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
   

   public MatrixNd getHu () {
      return Hu;
   }

   public MatrixNd getHv () {
      return Hv;
   }
   
   public VectorNd getU0 () {
      return u0;
   }
   
   public VectorNd getV0 () {
      return v0;
   }
   
   public MatrixNd getHlam () {
      return Hlam;
   }

   public MatrixNd getHc () {
      return Hc;
   }
   
   public VectorNd getLam0 () {
      return lam0;
   }
   
   public VectorNd getC0 () {
      return c0;
   }

}
