package artisynth.core.materials;

import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.numerics.*;
import maspack.function.*;
import maspack.interpolation.*;

/**
 * Tests AxialMaterials by checking that their computed derivatives are
 * consistent with the values determined via numeric differentiation.
 */
public class Millard2012AxialMuscleTest extends UnitTest {

   private static double GTOL = Millard2012AxialMuscle.GTOL;
   private static double INF = Double.POSITIVE_INFINITY;
   static double RTOD = 180/Math.PI;
   static double DTOR = Math.PI/180;

   Millard2012AxialMuscleTest() {
   }

   void testCurveDerivatives (
      String name, CubicHermiteSpline1d curve) {
      double tol = 2e-7;
      double err = CubicHermiteSpline1dTest.numericDerivativeError(curve);
      if (err > tol) {
         throw new TestException (
            "Numeric derivative check of "+name+" curve has error "+err+
            "; exceeds tolerance of "+tol);
      }
   }
   
   int numMuscleSolves1;
   int numIterations1;
   int numMuscleSolves2;
   int numIterations2;

   int numroots[] = new int[3];

   void clearIterationCount() {
      if (Millard2012AxialMuscle.useNewtonSolver) {
         NewtonRootSolver.clearIterationCount();
      }
      else {
         BrentRootSolver.clearIterationCount();
      }
   }

   int getIterationCount() {
      if (Millard2012AxialMuscle.useNewtonSolver) {
         return NewtonRootSolver.getIterationCount();
      }
      else {
         return BrentRootSolver.getIterationCount();
      }
   }

   int callCnt = 0;

   void testFindMuscleLength (
      Millard2012AxialMuscle mat, double a, double h) {

      //mat.setIgnoreForceVelocity(true);
      //mat.setFibreDamping(0);

      double Vmax = mat.getMaxContractionVelocity();
      double T = mat.getTendonSlackLength();
      double ol = mat.getOptFibreLength();
      double H = mat.myHeight;

      double lm1 = Math.sqrt(ol*ol-H*H);
      double lm2 = Math.sqrt(4*ol*ol-H*H);
      double lm3 = Math.sqrt(9*ol*ol-H*H);
      //double lm4 = Math.sqrt(16*ol*ol-H*H);

      DynamicDoubleArray lmaxVals = new DynamicDoubleArray();
      lmaxVals.add (T+lm2);
      lmaxVals.add (T+lm3);

      DynamicDoubleArray lminVals = new DynamicDoubleArray();
      lminVals.add (T - Math.min(T/2,lm1/2));
      lminVals.add (T);
      lminVals.add (T + lm1/3);
      lminVals.add (T + 0.9*lm1);

      for (int i=0; i<lminVals.size(); i++) {
         for (int j=0; j<lmaxVals.size(); j++) {
            testFindMuscleLengthX (
               mat, lminVals.get(i), lmaxVals.get(j), a, h);
         }
      }
   }

   double maxErr = 0;

   void testFindMuscleLengthX (
      Millard2012AxialMuscle mat, double lmin, double lmax, double a, double h) {

      double Vmax = mat.getMaxContractionVelocity();
      double T = mat.getTendonSlackLength();
      double ol = mat.getOptFibreLength();
      //mat.setFibreDamping (0.0);

      double l0 = T+ol; // start at slack position
      double A = (lmax-lmin)/2; // amplitude for the sinusoid
      double ang0 = Math.asin (1-(l0-lmin)/A);

      double w = 2*(Vmax*ol)/A;
      int npts = (int)(2*Math.PI/(w*h)) + 1;
      mat.myH = h;

      Millard2012AxialMuscle vmat = mat.clone();
      Millard2012AxialMuscle cmat = mat.clone();
      Millard2012AxialMuscle tmat = mat.clone();
      tmat.vmFromTendon = true;
      cmat.setComputeLmDotFromLDot (true);

      double fvprev = 0;
      double fcprev = 0;
      double ftprev = 0;
      double lprev = 0;
      double ldotprev = 0;
      double dFdl = 0;
      double dFdldot = 0;

      for (int j=0; j<=npts; j++) {
         double ang = ang0 + 2*Math.PI*j/npts;
         double l = lmin+A*(1-Math.sin(ang));
         double ldot = -w*A*Math.cos(ang);
         double ldotdot = w*w*A*Math.sin(ang);

         //double dfEst = dFdl*(l-lprev) + dFdldot*(ldot-ldotprev);
         double dfEst = dFdl*ldot*h + dFdldot*ldotdot*h;
         // double Dv = vmat.myDv;
         // double Dm = vmat.myDm;
         double lmvprev = vmat.myMuscleLength;
         // double vmvprev = vmat.myMuscleVel;

         double fc = cmat.computeF (l, ldot, 0, a);
         double fv = vmat.computeF (l, ldot, 0, a);
         double ft = tmat.computeF (l, ldot, 0, a);

         // check the force errors between vmat and cmat
         if (false) {
            double ferr = Math.abs(fc-fv);
            if ((useSpecial && showAllFE) || ferr > myMaxFErr) {
               myMaxFErr = ferr;
               double vmc = cmat.myMuscleVel;
               double lmv = vmat.myMuscleLength;
               double vmv = vmat.myMuscleVel;
               double lmc = cmat.myMuscleLength;
               double lmcprev = cmat.myMuscleLengthPrev;
               double vmcEst = (cmat.myMuscleLength-cmat.myMuscleLengthPrev)/h;
               System.out.printf (
                  "FELM: %g %g %g %g %g %g %g\n", ferr, fc, fv, ft, l, lmc, lmv);
               System.out.printf (
                  "FE: lmc=%g lmv=%g vmc=%g vmcEst=%g vmv=%g lmvprev=%g j=%d ldot=%g\n",
                  lmc, lmv, vmc, vmcEst, vmv, lmvprev, j, ldot);
               System.out.printf (
                  "FE:    mat=%s a=%g h=%g\n", toString(vmat), a, h);
               System.out.printf (
                  "FE:    case=%d npts=%d l=%g lmin,max=%g, %g Dt=%s Dm=%s Dv=%s\n",
                  myCase, npts, l, lmin, lmax, cmat.myDt, cmat.myDm, cmat.myDv);
            }
         }

         // check the force errors between vmat and tmat
         if (false) {
            double ferr = Math.abs(ft-fv);
            if ((useSpecial && showAllFE) || ferr > myMaxFErr) {
               myMaxFErr = ferr;
               double vmt = tmat.myMuscleVel;
               double lmt = tmat.myMuscleLength;
               double lmv = vmat.myMuscleLength;
               double vmv = vmat.myMuscleVel;
               System.out.printf (
                  "FELM: %g %g %g %g %g %g %g\n", ferr, fc, fv, ft, l, lmt, lmv);
               System.out.printf (
                  "FE: lmt=%g lmv=%g vmt=%g vmv=%g j=%d ldot=%g\n",
                  lmt, lmv, vmt, vmv, j, ldot);
               System.out.printf (
                  "FE:    mat=%s a=%g h=%g\n", toString(vmat), a, h);
               System.out.printf (
                  "FE:    case=%d npts=%d l=%g lmin,max=%g, %g Dt=%s Dm=%s Dv=%s\n",
                  myCase, npts, l, lmin, lmax, tmat.myDt, tmat.myDm, tmat.myDv);
            }
         }
         
         if (true && j > 0) {
            double df = ft - ftprev;
            double lmt = tmat.myMuscleLength;
            double vmt = tmat.myMuscleVel;
            //double dfEst = dFdl*(l-lprev) + dFdldot*(ldot-ldotprev);
            //double dfEst = Dm*(lmt-lmtprev) + Dv*(vmt-vmtprev);
            double dferr = Math.abs(df-dfEst);
            
            if ((useSpecial && showAllFE) || dferr > myMaxDFErr) {
               myMaxDFErr = dferr;
               System.out.printf (
                  "DFXX: %g %g %g %g %g %g %g\n", dferr, df, dfEst, ft, fv, ldotdot, (ldot-ldotprev)/h);
               System.out.printf (
                  "DF:   lmt=%g vmt=%g dl=%g dldot=%g j=%d ldot=%g\n",
                  lmt, vmt, (l-lprev), (ldot-ldotprev), j, ldot);
               System.out.printf (
                  "DF:   mat=%s a=%g h=%g\n", toString(tmat), a, h);
               System.out.printf (
                  "DF:   case=%d npts=%d l=%g lmin=%g lmax=%g Dt=%s Dm=%s Dv=%s\n",
                  myCase, npts, l, lmin, lmax, tmat.myDt, tmat.myDm, tmat.myDv);             
            }
         }
         
         dFdl = vmat.computeDFdl (l, ldot, 0, a);
         dFdldot = vmat.computeDFdldot (l, ldot, 0, a);
         fvprev = fv;
         ftprev = ft;
         fcprev = fc;
         lprev = l;
         ldotprev = ldot;
         
         vmat.advanceState (0, h);
         tmat.advanceState (0, h);
         cmat.advanceState (0, h);
      }
   }

//   void testFindMuscleLength (
//      Millard2012AxialMuscle mat,
//      double l0, double lm0, double a, double h) {
//
//      double Vmax = mat.getMaxContractionVelocity();
//      double T = mat.getTendonSlackLength();
//      double ol = mat.getOptFibreLength();
//
//      double maxDeltaL = 2*ol*Vmax*h;
//      //double maxDeltaL = 10*Vmax*h;
//      double lmin = Math.max (T/5, l0-maxDeltaL);
//      double lmax = l0+maxDeltaL;
//
//      // Test solution method 1 (dotlm estimated within time step).
//      // Sample l unformly between lmin and lmax.
//      clearIterationCount();
//      int npts = 11;
//      // for (int j=0; j<=npts; j++) {
//      //    double l = lmin + (lmax-lmin)*j/npts;  
//      //    double lm = 
//      //       // mat.findMuscleLength (l, 0, lm0, a, h, /*staticSolve=*/false);
//      //       mat.computeLmWithVmFromLm (l, lm0, lm0, 0, h, a);
//      //    numMuscleSolves1++;
//      //    mat.computeDerivs (l, lm, (lm-lm0)/h, a);
//      //    checkDerivRatios (mat, l, lm, (lm-lm0)/h, a, h);
//      // }
//      numIterations1 += getIterationCount();
//
//      // Test solution method 2 (dotlm estimated from derivatives).  Starting
//      // at l0, drive l between lmin and lmax using a sinusoid.
//      if (lmin >= l0) {
//         // can't fit sinusoid
//         return;
//      }
//      double A = (lmax-lmin)/2; // amplitude for the sinusoid
//      double ang0 = Math.asin (1-(l0-lmin)/A);
//      mat.setComputeLmDotFromLDot (true);
//      mat.setMuscleLength (lm0);
//      // find angular velocity to get a desired max velocity
//      clearIterationCount();
//      double w = 10*(Vmax*ol)/A;
//      double dt = 2*Math.PI/(w*(npts-1));
//      double lprev = l0;
//      for (int j=0; j<=npts; j++) {
//         double ang = ang0 + 2*Math.PI*j/npts;
//         double l = lmin+A*(1-Math.sin(ang));
//         double ldot = -w*A*Math.cos(ang);
//         double lmprev = mat.getMuscleLength();
//         double vm = mat.computeVmFromLdot (ldot);
//         if (Math.abs(vm) > myMaxVm) {
//            myMaxVm = Math.abs(vm);
//         }
//         if (Math.abs(ldot) > myMaxLdot) {
//            myMaxLdot = Math.abs(ldot);
//         }
//         double lm =
//            //mat.findMuscleLength (l, ldot, lmprev, a, h, /*staticSolve=*/false);
//            mat.computeLmWithConstantVm (l, lmprev, vm, a);
//         double lmsave = lm;
//         mat.myLength = l;
//         mat.setMuscleLength (lm);
//         //mat.computeDerivs (l, lm, (lm-lmprev)/dt, a);
//         mat.computeDerivs (l, lm, vm, a);
//         if (mat.myDt > 0) {
//            double r = mat.myDt/(mat.myDt+mat.myDm);
//            boolean changed = false;
//            if (r < myMinDFRatio) {
//               myMinDFRatio = r;
//               changed = true;
//            }
//            if (r > myMaxDFRatio) {
//               myMaxDFRatio = r;
//               changed = true;
//            }
//            if (changed) {
//               System.out.printf (
//                  "minMaxDF: %g %g\n", myMinDFRatio, myMaxDFRatio);
//               System.out.printf (
//                  "minMaxDF: lm=%g lmprev=%g vm=%g ldot=%g Dm=%g Dt=%g C=%g\n",
//                  lm, lmprev, vm, ldot, mat.myDm, mat.myDt, mat.myDt/(mat.myDm+mat.myDt));
//               System.out.printf (
//                  "minMaxDF:     l=%g l0=%g lm0=%g a=%g mat=%s\n", l, l0, lm0, a, toString(mat));
//            }
//         }
//         if (callCnt == 1000000) {
//            System.out.printf (
//               "## lm=%g lmprev=%g vm=%g ldot=%g Dm=%g Dt=%g C=%g\n",
//               lm, lmprev, vm, ldot, mat.myDm, mat.myDt, mat.myDt/(mat.myDm+mat.myDt));
//            System.out.printf (
//               "#     l=%g l0=%g lm0=%g a=%g mat=%s\n", l, l0, lm0, a, toString(mat));
//         }
//         
//         if (Math.abs((lm-lmprev)/dt) > myMaxVmDel) {
//            myMaxVmDel = Math.abs((lm-lmprev)/dt);
//            System.out.printf (
//               "BV vmdel=%g vm=%g delL=%g dellm=%g dt=%g\n", myMaxVmDel, vm, 2*A,(lm-lmprev), dt); 
//            System.out.printf (
//               "BV lmin=%g lmax=%g lm=%g lmprev=%g dell=%g j=%d l0=%g lm0=%g\n",
//               lmin, lmax, lm, lmprev, l-lprev, j, l0, lm0);
//            System.out.printf (
//               "BV    myDm=%g myDt=%g a=%g h=%g l=%g lm=%g vm=%g\n",
//               mat.myDm, mat.myDt, a, h, l, lm, vm);
//            System.out.printf (
//               "BV    mat=%s\n", toString(mat));
//            System.out.printf (
//               "BV    calCnt=%d\n", callCnt);
//         }
//         lprev = l;
//         // if (checkDerivRatios (mat, l, lm, vm, a, h)) {
//         //    System.out.println ("nsolves=" + numMuscleSolves2);
//         //    System.out.println ("found it");
//         //    System.out.println ("lmprev=" + lmprev + " ldot=" + ldot);
//         //    System.out.println ("lmsave=" + lmsave);
//         // }
//         numMuscleSolves2++;
//      }
//      mat.setComputeLmDotFromLDot (false);
//      numIterations2 += getIterationCount();
//      callCnt++;
//   }

   String toString (Millard2012AxialMuscle mat) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter (sw);
      pw.printf (
         "angx=%g ol=%g T=%g damping=%g H=%g ignoreForceVel=%b",
         Math.toDegrees(mat.getOptPennationAngle()),
         mat.getOptFibreLength(),
         mat.getTendonSlackLength(),
         mat.getFibreDamping(),
         mat.myHeight,
         mat.getIgnoreForceVelocity());
      return sw.toString();
   }

   void testComputeGDeriv (
      Millard2012AxialMuscle mat,
      double l0, double lm0, double a, double h) {

      double maxDeltaL = 10*mat.getMaxContractionVelocity()*h;
      double lmin = Math.max (mat.getTendonSlackLength()/10, l0-maxDeltaL);
      double lmax = l0+maxDeltaL;
      double lo = mat.getOptFibreLength();
      double T = mat.getTendonSlackLength();

      int npts = 11;
      double reflen = T+lo;
      double hl = reflen*1e-8;
      for (int j=0; j<=npts; j++) {
         double l = lmin + (lmax-lmin)*j/npts;   
         for (int k=0; k<=npts; k++) {
            DoubleHolder dg = new DoubleHolder();
            double lm = lmin + (lmax-lmin)*k/npts;  
            double vm = (lm-lm0)/h;
            double g0 = mat.computeG (dg, l, lm, vm, a, h);
            double g1 = mat.computeG (null, l, lm+hl, (lm+hl-lm0)/h, a, h);

            double dchk = (g1-g0)/hl;
            // XXX should find a better way to evaluate error when dg <= 1
            double dmag = Math.max ((Math.abs(dg.value)+Math.abs(dchk))/2, 1);
            double derr = Math.abs(dg.value-dchk)/dmag;
            if (derr > myMaxDGErr) {
               System.out.printf (
                  "derr=%g dg=%g chk=%g\n",
                  derr, dg.value, dchk);
               System.out.printf (
                  "  mat: %s a=%g h=%g l=%g lm=%g lm0=%g\n", 
                  toString(mat), a, h, l, lm, lm0);
               System.out.println (
                  "  g0=" + g0 + " g1=" + g1 + " hl=" + hl);
               myMaxDGErr = derr;
            }
            mat.computeDerivs (l, lm, vm, a);
            double dgsum = mat.myDm + mat.myDv/h + mat.myDt;
            derr = Math.abs(dg.value-dgsum)/dmag;
            if (derr > Math.max(dmag*1e-13, 1e-12)) {
               System.out.printf (
                  "derr=%g dg=%g dgsum=%g\n",
                  derr, dg.value, dgsum);
            }
         }
      }
   }

   private boolean checkDerivRatios (
      Millard2012AxialMuscle mat,
      double l, double lm, double vm, double a, double h) {

      double lo = mat.getOptFibreLength();
      double Vmax = mat.getMaxContractionVelocity();

      double num = mat.myDm+mat.myDt;

      if (num < myMinDNumerator) {
         myMinDNumerator = num;
         System.out.printf (
               " MinDN myDm=%g myDt=%g a=%g h=%g l=%g lm=%g vm=%g\n",
               mat.myDm, mat.myDt, a, h, l, lm, vm);
         System.out.printf (
            "  mat=%s\n", toString(mat));
      }

      if (num != 0 && Math.abs(vm)/(lo*Vmax) < 10) {

         double r = mat.myDt/num;
         if (r > myMaxDFRatio) {
            System.out.printf (
               " myDm=%g myDt=%g a=%g h=%g l=%g lm=%g vm=%g\n",
               mat.myDm, mat.myDt, a, h, l, lm, vm);
            System.out.printf (
               "  mat=%s\n", toString(mat));
            System.out.printf (
               "  maxKt=%g maxKp=%g\n", mat.getMaxTendonStiffness(),
               mat.getMaxPassiveStiffness());
            mat.computeDerivs (l, lm, vm, a);
            myMaxDFRatio = r;
            return true;
         }
         else if (r < myMinDFRatio) {
            myMinDFRatio = r;
         }
      }
      return false;
   }

   private static double MAX_DG_ERR = 5e-5;
   double myMaxDGErr = MAX_DG_ERR;
   double myMinDFRatio = INF;
   double myMinDNumerator = INF;
   double myMaxDFRatio = 0;
   double myMaxLdot = 0;
   double myMaxVm = 0;
   double myMaxVmDel = 0;
   double myMaxFErr = 0;
   double myMaxDFErr = 0;
   double myMinDFm = 0;

   int myCase = 0;
   
   public void testMuscleComputationsX (
      double optPenAng, double a, double damping, double tratio,
      boolean ignoreForceVelocity, double h) {

      double ol = 0.1; // optimal fibre length
      double T = tratio*ol; // tendonSlack

      Millard2012AxialMuscle mat =
         new Millard2012AxialMuscle (
            /*maxIso=*/1.0, ol, T, Math.toRadians(optPenAng));
      mat.setRigidTendon (false);
      mat.setMaxIsoForce (1.0);
      mat.setFibreDamping (damping);
      mat.setIgnoreForceVelocity (ignoreForceVelocity);

      myCase++;
      testFindMuscleLength (mat, a, h);
      //testComputeGDeriv (mat, a, h);
   }      

   private static class GFunctionWithZeroVm implements Function1x1 {
      double myL;
      double myA;
      Millard2012AxialMuscle myMat;
      
      GFunctionWithZeroVm (
         Millard2012AxialMuscle mat, double l, double a) {
         myMat = mat;
         myL = l;
         myA = a;
      }
      
      public double eval (double lm) {
         return myMat.computeG (null, myL, lm, 0, myA, 0);
      }
   }      

   private void testComputeGDeriv (
      Millard2012AxialMuscle mat, double a, double h) {

      double T = mat.getTendonSlackLength();
      double ol = mat.getOptFibreLength();

      double lmin = T/2;
      double lmax = 2*(T+ol);
      int npts = 11;
      for (int i=0; i<=npts; i++) {
         double l0 = lmin + (lmax-lmin)*i/npts;
         double[] roots = findZeroVelRoots (mat, l0, a);
         // use the last root as the equilibrium position
         if (roots.length > 0) {
            double lm0 = roots[roots.length-1];
            testComputeGDeriv (mat, l0, lm0, a, h);
         }
         else {
            throw new TestException (
               "No equilibrium roots for muscle length test case");
         }
         numroots[roots.length]++;
      }
      if (myMaxDGErr > MAX_DG_ERR) {
         throw new TestException (
            "Differentiation of G has error exceeding "+MAX_DG_ERR);
      }
   }

  double maxRootByBisection (Function1x1 gfxn, double lmlo, double lmhi) {
     // assume g at lmlo is 0
     for (int i=0; i<48; i++) {
        double lm = (lmlo+lmhi)/2;
        double g = gfxn.eval (lm);
        if (Math.abs(g) <= GTOL) {
           // max root is in hi half
           lmlo = lm;
        }
        else {
           // max root is in lo half
           lmhi = lm;
        }
     }
     return (lmlo+lmhi)/2;
  }

  double minRootByBisection (Function1x1 gfxn, double lmlo, double lmhi) {
     // assume g at lmhi is 0
     for (int i=0; i<48; i++) {
        double lm = (lmlo+lmhi)/2;
        double g = gfxn.eval (lm);
        if (Math.abs(g) > GTOL) {
           // min root is in hi half
           lmlo = lm;
        }
        else {
           // min root is in lo half
           lmhi = lm;
        }
     }
     return (lmlo+lmhi)/2;
  }

   double[] findZeroVelRoots (
      Millard2012AxialMuscle mat, double l, double a) {
      int nints = 256;
      // brute force search for intervals
      DynamicDoubleArray roots = new DynamicDoubleArray();
      double h = 0.01;
      Function1x1 gfxn = new GFunctionWithZeroVm (mat, l, a);

      for (int i=0; i<nints; i++) {
         double lmA = i*(l/nints);
         double lmB = (i+1)*(l/nints);
         double g0 = gfxn.eval (lmA);
         double g1 = gfxn.eval (lmB);
         //System.out.println ("g0=" + g0 + " g1=" + g1);
         if (Math.abs(g0) > GTOL && Math.abs(g1) > GTOL && g0*g1 < 0) {
            double lm = BisectionRootSolver.findRoot (
               gfxn, lmA, g0, lmB, g1, l*1e-12, GTOL);
            roots.add (lm);               
         }
         else if (Math.abs(g0) <= GTOL && Math.abs(g1) > GTOL) {
            roots.add (maxRootByBisection (gfxn, lmA, lmB));            
         }
         else if (Math.abs(g0) > GTOL && Math.abs(g1) <= GTOL) {
            roots.add (minRootByBisection (gfxn, lmA, lmB));            
         }
         else if (Math.abs(g1) <= GTOL && i==nints-1) {
            roots.add (lmB);
         }
      }
      return roots.getArray();
   }


//   public void testMuscleComputations (
//      double optPenAng, double a, double damping, double tratio,
//      boolean ignoreForceVelocity, double h) {
//
//      double ol = 0.1; // optimal fibre length
//      double T = tratio*ol; // tendonSlack
//
//      Millard2012AxialMuscle mat =
//         new Millard2012AxialMuscle (
//            /*maxIso=*/1.0, ol, T, Math.toRadians(optPenAng));
//      mat.setRigidTendon (false);
//      mat.setMaxIsoForce (1.0);
//      mat.setFibreDamping (damping);
//      mat.setIgnoreForceVelocity (ignoreForceVelocity);
//
//      double lmin = T/2;
//      double lmax = 2*(T+ol);
//      int npts = 11;
//      for (int i=0; i<=npts; i++) {
//         double l0 = lmin + (lmax-lmin)*i/npts;
//         double[] roots = mat.findRoots (l0, 0, 0, a);
//         // use the last root as the equilibrium position
//         if (roots.length > 0) {
//            double lm0 = roots[roots.length-1];
//            testFindMuscleLength (mat, l0, lm0, a, h);
//            testComputeGDeriv (mat, l0, lm0, a, h);
//         }
//         else {
//            throw new TestException (
//               "No equilibrium roots for muscle length test case");
//         }
//         numroots[roots.length]++;
//      }
//      if (myMaxDGErr > MAX_DG_ERR) {
//         throw new TestException (
//            "Differentiation of G has error exceeding "+MAX_DG_ERR);
//      }
//   }      

   void testFmComputations (
      double optPenAng, double a, double damping, double tratio,
      boolean ignoreForceVelocity) {

      double ol = 0.1; // optimal fibre length
      double T = tratio*ol; // tendonSlack

      Millard2012AxialMuscle mat =
         new Millard2012AxialMuscle (
            /*maxIso=*/1.0, ol, T, Math.toRadians(optPenAng));
      mat.setRigidTendon (false);
      mat.setMaxIsoForce (1.0);
      mat.setFibreDamping (damping);
      mat.setIgnoreForceVelocity (ignoreForceVelocity);

      double maxLm = 3*ol;
      DoubleHolder df = new DoubleHolder();
      for (int i=0; i<=20; i++) {
         double vm = (i-10)/10.0;
         int npts = 100;
         for (int j=0; j<=npts; j++) {
            double lm = j*maxLm/npts;
            mat.computeFm (df, lm, vm, a, -1);
            if (vm < 0 && df.value < myMinDFm) {
               myMinDFm = df.value;
               System.out.printf (
                  "DFM: min=%g lm=%g vm=%g a=%g\n", myMinDFm, lm, vm, a);
               System.out.printf (
                  "DFM:   mat=%s\n", toString(mat));
            }
         }
      }
   }   

   public void testMuscleComputations() {
      double[] angs = new double[] {
         0, 0.01, 0.1, 1, 2, 3, 5, 10.0, 20.0, 30, 45, 60 };
      double[] acts = new double[] {
         0, 0.5, 1.0 };
      double[] dampings = new double[] {
         0, 0.01, 0.1 };
      double[] hvals = new double[] {
         0.01, 0.001, 0.0001 };
      double[] tratios = new double[] {
         5, 2, 1, 0.5, 0.2 };
      for (double ang : angs) {
         for (double a : acts) {
            for (double d : dampings) {
               for (double tr : tratios) {
                  for (double h : hvals) {
                     //testMuscleComputations (ang, a, d, tr, false, h);
                     //testMuscleComputations (ang, a, d, tr, true, h);
                  }
                  testMuscleComputationsX (ang, a, d, tr, false, 0.01);
                  testMuscleComputationsX (ang, a, d, tr, true, 0.01);
                  //testFmComputations (ang, a, d, tr, false);
                  //testFmComputations (ang, a, d, tr, true);
               }
            }
         }
      }

      System.out.println (
         "avg solve iters 1: " + numIterations1/(double)numMuscleSolves1);
      System.out.println (
         "avg solve iters 2: " + numIterations2/(double)numMuscleSolves2);
 
      System.out.printf ("min/maxDFRatio: %g %g\n", myMinDFRatio, myMaxDFRatio);
      System.out.printf ("minDNumerator: %g\n", myMinDNumerator);
      System.out.printf (
         "maxVm=%g maxVmDel=%g maxLdot=%g\n", myMaxVm, myMaxVmDel, myMaxLdot);
      System.out.println ("num roots: " + new VectorNi(numroots));
      Millard2012AxialMuscle mat = new Millard2012AxialMuscle();     
      System.out.printf (
         "nsame=%d ninterval=%d nsolved=%d nclip0=%d nclipL=%d maxiters=%d nshort=%d\n",
         mat.nsame, mat.ninterval, mat.nsolved, mat.nclipped0, mat.nclippedL,
         mat.maxiters, mat.nshort);
   }

   static boolean useSpecial = true;
   static boolean showAllFE = true;

   public void testSpecial() {

      Millard2012AxialMuscle mat = new Millard2012AxialMuscle();
      mat.setRigidTendon (false);
      mat.setMaxIsoForce (1.0);
      mat.setOptPennationAngle (DTOR*20.0);
      mat.setOptFibreLength (0.1);
      mat.setTendonSlackLength (0.2);
      mat.setFibreDamping (0.0);
      mat.setIgnoreForceVelocity (false);
      mat.setComputeLmDotFromLDot (false);

      testFindMuscleLengthX (mat, 0.284572, 0.498044, 1.0, 0.01);
   }

   public void test() {
   
      Millard2012AxialMuscle mat = new Millard2012AxialMuscle();
      mat.setMaxIsoForce (1.0);
   
      // testCurveDerivatives (
      //    "activeForceLength", mat.getActiveForceLengthCurve());
      // testCurveDerivatives (
      //    "passiveForceLength", mat.getPassiveForceLengthCurve());
      // testCurveDerivatives (
      //    "tendonForceLength", mat.getTendonForceLe<ngthCurve());
      // testCurveDerivatives (
      //    "forceVelocity", mat.getForceVelocityCurve());

      myMaxDGErr = MAX_DG_ERR;
      testMuscleComputations(); 
   }

   public static void main (String[] args) {
      Millard2012AxialMuscleTest tester = new Millard2012AxialMuscleTest();

      if (useSpecial) {
         tester.testSpecial();
      }
      else {
         tester.runtest();
      }
      //tester.runtest();
      //tester.PrintG();
   }

}
