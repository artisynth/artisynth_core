/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.util.TimeBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.Matrix;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.Vector;
import maspack.util.NumberFormat;

/**
 * Computes the linearized velocity and constraint force response for each of
 * the excitations computed by the controller. More precisely, if 'a' is the
 * vector of excitation values, then the velocity 'u' and constraint forces
 * 'lam' for the next time step are given by the linear relationships
 *
 * <pre>
 * u = u0 + Hu a
 * lam = lam0 + Hlam a
 * </pre>
 * 
 * u0, Hu, lam0 and Hlam are computed and stored by this class, for subsequent
 * use in computing the cost terms for various motion and force targets.
 * 
 * @author John E. Lloyd
 */
public class ExcitationResponse
{
   public static final boolean DEFAULT_NORMALIZE = false;

   boolean debug = false;
   public static boolean debugHu = false;
   NumberFormat fmt = new NumberFormat("%g");

   TrackingController myController;
   MechSystemSolver myMechSysSolver;
   MechSystem myMech;

   public ExcitationResponse (TrackingController opt) {
      myController = opt;
   }
   
   void initializeMech() {
      myMech = myController.getMech ();
      myMechSysSolver = new MechSystemSolver(myMech);
   }

   VectorNd fa = new VectorNd();
   VectorNd[] HuCols = new VectorNd[0];
   VectorNd[] HlamCols = new VectorNd[0];
   VectorNd u0 = new VectorNd();
   VectorNd ex = new VectorNd();
   VectorNd fp = new VectorNd();
   VectorNd bf = new VectorNd();
   VectorNd ftmp = new VectorNd();

   VectorNd lam0 = new VectorNd(0);
   
   VectorNd the = new VectorNd(0);

   VectorNd curEx = new VectorNd(0);
   VectorNd curVel = new VectorNd(0);

   protected double computeError (Vector v0, Vector v1) {
      VectorNd diff = new VectorNd (v0);
      diff.sub (new VectorNd(v1));
      double mag = (v0.norm()+v1.norm())/2;
      double err = diff.norm();
      if (mag > 1e-8) {
         err /= mag;
      }
      return err;
   }
   
   protected double computeError (Matrix M0, Matrix M1) {
      MatrixNd diff = new MatrixNd (M0);
      diff.sub (new MatrixNd(M1));
      double mag = (M0.frobeniusNorm()+M1.frobeniusNorm())/2;
      double err = diff.frobeniusNorm();
      if (mag > 1e-8) {
         err /= mag;
      }
      return err;
   }
   
   protected void update (double t0, double t1) {
      
      if (myMech == null) {
         initializeMech();
      }
      
      // round because results are very sensitive to h and we want to keep them
      // identical to earlier results when t0, t1 where given as nsec integers
      double h = TimeBase.round(t1 - t0);

      boolean useTrapezoidal = myController.useTrapezoidalSolver();

      int velSize = myMech.getActiveVelStateSize ();
//      int conSize = myMech
      int exSize = myController.numExciters();

      boolean incremental = myController.getComputeIncrementally();
      double deltaEx = 0.001; // for incremental computation
      
      fa.setSize(velSize);

      if (HuCols.length != exSize) {
         HuCols = new VectorNd[exSize];
         HlamCols = new VectorNd[exSize];
         for (int i=0; i<exSize; i++) {
            HuCols[i] = new VectorNd (velSize);
            HlamCols[i] = new VectorNd (); // size will be initialized on demand
         }
      }
      else {
         for (int i=0; i<exSize; i++) {
            HuCols[i].setSize (velSize);
         }
      }

      u0.setSize(velSize);
      ex.setSize(exSize);
      fp.setSize(velSize);
      bf.setSize(velSize);
      ftmp.setSize(velSize);
      
      curVel.setSize (velSize);
      myMech.getActiveVelState (curVel);
      
      curEx.setSize(exSize);
      myController.getExcitations(curEx, 0);
      
      VectorNd Mv = new VectorNd (velSize);
      myMechSysSolver.updateStateSizes();
      myMechSysSolver.updateMassMatrix(t0);
      myMechSysSolver.mulActiveInertias(Mv, curVel);

      // fp = passive forces with zero muscle activation
      if (incremental) {
         ex.set (curEx);
      }
      else {
         ex.setZero();
      }

      myController.updateConstraints(t1);
      myController.updateForces(t1, fp, ex);
      myMechSysSolver.addMassForces(fp, t0);

      // bf = M v + h fp
      bf.scaledAdd(h, fp, Mv);
      
      if (useTrapezoidal) {
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
      
      lam0.set (myMechSysSolver.getLambda ());

      // where e_j is elementary unit vector
      for (int j = 0; j < exSize; j++) {
         double dex = deltaEx;
         if (incremental) {
            ex.set (curEx);
            // hack: if current excitation plus dex > 1.0, use -dex instead,
            // since otherwise excitation clipping may cause derivative to
            // evaluate to 0
            double ej = curEx.get(j);
            if (ej + dex > 1.0) {
               dex = -dex;
            }
            ex.set (j, ej+dex);
         }
         else {
            ex.setZero();
            ex.set (j, 1.0);
         }
         myController.updateForces(t1, fa, ex);
         
         
         // XXX scale fa by excitation weight??
         
         if (incremental) {
            myMechSysSolver.addMassForces(fa, t0);
            bf.scaledAdd(h, fa, Mv);
         }
         else {
            bf.sub (fa, fp);
            bf.scale (h);
         }
         
         if (myController.getUseKKTFactorization() || incremental) {        
            if (useTrapezoidal) {
                // use Trapezoidal integration
                myMechSysSolver.KKTFactorAndSolve (
                   HuCols[j], null, bf, /*tmp=*/ftmp, curVel, 
                   h, -h/2, -h*h/4, -h/2, h*h/4);
             }
             else {
                // use ConstrainedBackwardEuler integration
                myMechSysSolver.KKTFactorAndSolve(
                   HuCols[j], null, bf, /*tmp=*/ftmp, curVel, h);
             }     
            HlamCols[j].set(myMechSysSolver.getLambda());
            if (incremental) {
               HuCols[j].sub (u0);
               HuCols[j].scale (1/dex);
               HlamCols[j].sub (lam0);  
               HlamCols[j].scale (1/dex);  
            }
         }
         else {
            // use pre-factored KKT system
            // Note neglecting change in jacobians due to excitation
            myMechSysSolver.KKTSolve(HuCols[j], HlamCols[j], the, bf);
         }
      }

      // XXX rest now done in motion target term

      if (debugHu && myMech instanceof MechSystemModel) {
         MechSystemModel mySys = (MechSystemModel)myMech;
         ComponentState saveState = mySys.createState (null);
         mySys.getState (saveState);

         VectorNd Hucol = new VectorNd (velSize);
         VectorNd u0chk = new VectorNd (velSize);
         MatrixNd Huchk = new MatrixNd (exSize, exSize);
         MatrixNd Hu = new MatrixNd (exSize, exSize);
         
         VectorNd vel = new VectorNd (mySys.getActiveVelStateSize());
         if (incremental) {
            ex.set (curEx);
         }
         else {
            ex.setZero();
         }

         mySys.preadvance (t0, t1, 0);
         myController.setExcitations (ex, 0);
         mySys.advance (t0, t1, 0);

         mySys.getActiveVelState (u0chk);

         mySys.setState (saveState);

         for (int j = 0; j < exSize; j++) {
            double dex = deltaEx;
            if (incremental) {
               ex.set (curEx);
               // hack: if current excitation plus dex > 1.0, use -dex instead,
               // since otherwise excitation clipping may cause derivative to
               // evaluate to 0
               double ej = curEx.get(j);
               if (ej + dex > 1.0) {
                  dex = -dex;
               }
               ex.set (j, ej+dex);
            }
            else {
               ex.setZero();
               ex.set (j, 1.0);
            }

            mySys.preadvance (t0, t1, 0);
            myController.setExcitations (ex, 0);
            mySys.advance (t0, t1, 0);

            mySys.getActiveVelState (Hucol);
            Hucol.sub (u0chk);
            if (incremental) {
               Hucol.scale (1/dex);
            }
            mySys.setState (saveState);
            Hu.setColumn (j, HuCols[j]);
         }

         System.out.println ("u0=\n" + u0.toString("%10.5f"));
         System.out.println ("u0chk=\n" + u0chk.toString("%10.5f"));
         System.out.println ("Hu=\n" + Hu.toString("%10.5f"));
         System.out.println ("Huchk=\n" + Huchk.toString("%10.5f"));
      }

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

   public VectorNd getU0 () {
      return u0;
   }
   
   protected VectorNd getHuCol (int j) {
      return HuCols[j];
   }
   
   public VectorNd getLam0 () {
      return lam0;
   }
   
   protected VectorNd getHlamCol (int j) {
      return HlamCols[j];
   }

}
