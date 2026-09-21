/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import maspack.matrix.Matrix;
import maspack.solvers.FemMatrixGenerator.CRSMatrix;
import maspack.solvers.FemMatrixGenerator.ElemType;

/**
 * Measures the performance of hybrid direct/iterative solves, in which a
 * matrix factored for values {@code V0} is used as a preconditioner for an
 * iterative solve with nearby values {@code V1}, as done by {@link
 * DirectSolver#iterativeSolve}. The perturbed values are
 * <pre>
 *   V1[k] = V0[k] (1 + eps r[k])
 * </pre>
 * where {@code r[k]} is uniformly distributed in [-1,1]. The time of each
 * iterative solve is compared with that of a direct factor and solve.
 *
 * <p>Test matrices are the slab cases of {@link ThreadCountSweep}: {@code
 * slab}, an {@code n x n x n/4} hex grid, and {@code slabC}, the same with
 * constraints added at a ratio of 0.1.
 *
 * <p>Usage:
 * <pre>
 *  java maspack.solvers.HybridSolveTiming -out hybrid.csv
 *     [-solvers Mumps,Pardiso] [-cases slab,slabC] [-symmetric]
 *     [-res 32,48] [-threads 8] [-eps 1e-4,1e-3,1e-2,1e-1]
 *     [-tolExp 10] [-maxSolves 100] [-restart 20] [-reps 3]
 *     [-mumpsMethods GMRES,CGS] [-pardisoMethods CG,CGS,GMRES,HCGS]
 *     [-criticalArrays] [-noCriticalArrays]
 * </pre>
 */
public class HybridSolveTiming {

   String[] mySolvers = new String[] { "Mumps", "Pardiso" };
   String[] myCases = new String[] { "slab", "slabC" };
   boolean mySymmetric = false;
   int[] myResolutions = new int[] { 32, 48 };
   int myNumThreads = 8;
   double[] myEps = new double[] { 1e-4, 1e-3, 1e-2, 1e-1 };
   int myTolExp = 10;
   int myMaxSolves = 100;
   int myRestart = 20;
   int myReps = 3;
   String[] myMumpsMethods = new String[] { "GMRES", "CGS" };
   // CG and CGS are Pardiso's own iterative solves (iparm[3]); GMRES and
   // HCGS are GMRES and CGS performed by the native wrapper
   String[] myPardisoMethods = new String[] { "CG", "CGS", "GMRES", "HCGS" };

   CRSMatrix createMatrix (String caseName, int res) {
      FemMatrixGenerator gen = new FemMatrixGenerator();
      CRSMatrix M = gen.create (ElemType.HEX, res, res, Math.max (2, res/4), 3);
      if (caseName.equals ("slabC")) {
         M = gen.addConstraints (M, (int)Math.round (0.1*M.size), 4, 3);
      }
      else if (!caseName.equals ("slab")) {
         throw new IllegalArgumentException ("Unknown case " + caseName);
      }
      if (mySymmetric) {
         M.type = Matrix.SYMMETRIC;
      }
      return M;
   }

   static double[] perturbValues (double[] vals, double eps, long seed) {
      Random rand = new Random (seed);
      double[] pvals = new double[vals.length];
      for (int k=0; k<vals.length; k++) {
         pvals[k] = vals[k]*(1 + eps*(2*rand.nextDouble()-1));
      }
      return pvals;
   }

   static double norm (double[] v, int n) {
      double sum = 0;
      for (int i=0; i<n; i++) {
         sum += v[i]*v[i];
      }
      return Math.sqrt (sum);
   }

   void configureMethod (DirectSolver solver, String method) {
      solver.setIterativeMaxSolves (myMaxSolves);
      ((DirectSolverBase)solver).setGmresRestart (myRestart);
      if (solver instanceof MumpsSolver) {
         solver.setIterativeMethod (DirectSolver.IterativeMethod.valueOf (method));
      }
      else if (solver instanceof PardisoSolver) {
         PardisoSolver psolver = (PardisoSolver)solver;
         if (isPardisoHybrid (method)) {
            psolver.setUseNativeIterativeSolve (false);
            psolver.setIterativeMethod (
               method.equals ("GMRES") ?
               DirectSolver.IterativeMethod.GMRES :
               DirectSolver.IterativeMethod.CGS);
         }
         else {
            psolver.setUseNativeIterativeSolve (true);
            if (method.equals ("CGS")) {
               // iparm[3] = 10*L+K; K=1 selects CGS instead of CG. The
               // override must be cleared after the solve, since it would
               // otherwise also be applied to the factorization phase.
               psolver.setIParam (3, 10*myTolExp+1);
            }
         }
      }
   }

   /**
    * Returns true if a Pardiso method is performed by the native wrapper
    * (GMRES or HCGS) rather than by Pardiso itself (CG or CGS).
    */
   static boolean isPardisoHybrid (String method) {
      return method.equals ("GMRES") || method.equals ("HCGS");
   }

   void unconfigureMethod (DirectSolver solver) {
      if (solver instanceof PardisoSolver) {
         ((PardisoSolver)solver).clearIParams();
      }
   }

   void runCase (String caseName, int res, PrintWriter pw) {
      CRSMatrix M = createMatrix (caseName, res);
      boolean symmetric = (M.type & Matrix.SYMMETRIC) != 0;
      double[] b = new double[M.size];
      Random rand = new Random (0x1234);
      for (int i=0; i<M.size; i++) {
         b[i] = 2*rand.nextDouble()-1;
      }
      double bnorm = norm (b, M.size);
      double[] x = new double[M.size];
      System.out.println (
         caseName + (mySymmetric ? "-sym" : "") + " res=" + res +
         " size=" + M.size + " nnz=" + M.numFullVals());

      for (String solverName : mySolvers) {
         SparseSolverId id = SparseSolverId.valueOf (solverName);
         DirectSolver solver = id.createDirectSolver();
         ((DirectSolverBase)solver).setNumThreads (myNumThreads);
         solver.setIterativeTolerance (Math.pow (10.0, -myTolExp));
         double[] vals0 = M.copyVals();
         solver.analyze (vals0, M.colIdxs, M.rowOffs, M.size, M.type);

         // direct factor and solve cost, best of reps
         double fbest = Double.MAX_VALUE;
         double sbest = Double.MAX_VALUE;
         for (int rep=0; rep<Math.max(2,myReps); rep++) {
            long t0 = System.nanoTime();
            solver.factor (vals0);
            double fmsec = (System.nanoTime()-t0)/1e6;
            t0 = System.nanoTime();
            solver.solve (x, b);
            double smsec = (System.nanoTime()-t0)/1e6;
            fbest = Math.min (fbest, fmsec);
            sbest = Math.min (sbest, smsec);
         }
         System.out.printf (
            "  %-8s direct: factor=%9.2f solve=%8.2f msec%n",
            solverName, fbest, sbest);

         String[] methods =
            (solver instanceof MumpsSolver ? myMumpsMethods : myPardisoMethods);
         for (double eps : myEps) {
            double[] vals1 = perturbValues (
               vals0, eps, 0x5678 + Double.doubleToLongBits (eps));
            for (String method : methods) {
               double tbest = Double.MAX_VALUE;
               int iters = 0;
               int solves = -1;
               double resid = -1;
               // residual computed by the native code, as a check on its
               // matrix products; -1 if not available
               double nresid = -1;
               // time breakdown of the best rep, in msec: preconditioner
               // solves, matrix products, other native work (vector
               // operations and setup), and JNI overhead; -1 if not available
               double pcMs = -1;
               double mvMs = -1;
               double otherMs = -1;
               double jniMs = -1;
               for (int rep=0; rep<myReps; rep++) {
                  configureMethod (solver, method);
                  long t0 = System.nanoTime();
                  iters = solver.iterativeSolve (vals1, x, b);
                  double msec = (System.nanoTime()-t0)/1e6;
                  unconfigureMethod (solver);
                  boolean isBest = (msec < tbest);
                  tbest = Math.min (tbest, msec);
                  double[] times = null;
                  if (solver instanceof MumpsSolver) {
                     MumpsSolver msolver = (MumpsSolver)solver;
                     solves = msolver.getLastIterativeSolves();
                     nresid = msolver.getLastIterativeResidual();
                     times = msolver.getLastIterativeTimes();
                  }
                  else if (isPardisoHybrid (method)) {
                     PardisoSolver psolver = (PardisoSolver)solver;
                     solves = psolver.getLastIterativeSolves();
                     nresid = psolver.getLastIterativeResidual();
                     times = psolver.getLastIterativeTimes();
                  }
                  else {
                     // Pardiso's own solve reports CG/CGS iterations; CG uses
                     // one preconditioner solve per iteration and CGS two
                     solves = Math.abs(iters)*(method.equals("CGS") ? 2 : 1);
                  }
                  if (isBest && times != null) {
                     pcMs = times[1];
                     mvMs = times[2];
                     otherMs = times[0]-times[1]-times[2];
                     jniMs = msec-times[0];
                  }
                  resid = DirectSolver.residual (
                     M.rowOffs, M.colIdxs, vals1, M.size, x, b, symmetric)/bnorm;
               }
               double ratio = tbest/(fbest+sbest);
               System.out.printf (
                  "  %-8s eps=%-7.0e %-6s iters=%4d solves=%4d time=%9.2f " +
                  "(%.2fx direct) resid=%.2e nresid=%.2e%n",
                  solverName, eps, method, iters, solves, tbest, ratio, resid,
                  nresid);
               if (pcMs >= 0) {
                  System.out.printf (
                     "  %-8s   breakdown: precond=%.2f matvec=%.2f " +
                     "other=%.2f jni=%.2f msec%n",
                     "", pcMs, mvMs, otherMs, jniMs);
               }
               pw.printf (
                  "%s,%s,%d,%d,%d,%g,%s,%d,%d,%.3f,%.3f,%.3f,%.4f,%.3e,%.3e," +
                  "%.3f,%.3f,%.3f,%.3f%n",
                  solverName, caseName + (mySymmetric ? "-sym" : ""), res,
                  M.size, myNumThreads, eps, method, iters, solves, tbest,
                  fbest, sbest, ratio, resid, nresid, pcMs, mvMs, otherMs,
                  jniMs);
               pw.flush();
            }
         }
         solver.dispose();
      }
   }

   private static int[] parseIntList (String str) {
      String[] strs = str.split (",");
      int[] vals = new int[strs.length];
      for (int i=0; i<strs.length; i++) {
         vals[i] = Integer.parseInt (strs[i].trim());
      }
      return vals;
   }

   private static double[] parseDoubleList (String str) {
      String[] strs = str.split (",");
      double[] vals = new double[strs.length];
      for (int i=0; i<strs.length; i++) {
         vals[i] = Double.parseDouble (strs[i].trim());
      }
      return vals;
   }

   private static void printUsageAndExit() {
      System.out.println (
         "Usage: java maspack.solvers.HybridSolveTiming -out <file.csv>\n" +
         "  [-solvers Mumps,Pardiso] [-cases slab,slabC] [-symmetric]\n" +
         "  [-res 32,48] [-threads 8] [-eps 1e-4,1e-3,1e-2,1e-1]\n" +
         "  [-tolExp 10] [-maxSolves 100] [-restart 20] [-reps 3]\n" +
         "  [-mumpsMethods GMRES,CGS] [-pardisoMethods CG,CGS,GMRES,HCGS]\n" +
         "  [-criticalArrays] [-noCriticalArrays]");
      System.exit (1);
   }

   public static void main (String[] args) throws IOException {
      HybridSolveTiming timing = new HybridSolveTiming();
      String outFile = null;
      for (int i=0; i<args.length; i++) {
         String arg = args[i];
         boolean hasValue = (i+1 < args.length);
         if (arg.equals ("-out") && hasValue) {
            outFile = args[++i];
         }
         else if (arg.equals ("-solvers") && hasValue) {
            timing.mySolvers = args[++i].split (",");
         }
         else if (arg.equals ("-cases") && hasValue) {
            timing.myCases = args[++i].split (",");
         }
         else if (arg.equals ("-symmetric")) {
            timing.mySymmetric = true;
         }
         else if (arg.equals ("-res") && hasValue) {
            timing.myResolutions = parseIntList (args[++i]);
         }
         else if (arg.equals ("-threads") && hasValue) {
            timing.myNumThreads = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-eps") && hasValue) {
            timing.myEps = parseDoubleList (args[++i]);
         }
         else if (arg.equals ("-tolExp") && hasValue) {
            timing.myTolExp = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-maxSolves") && hasValue) {
            timing.myMaxSolves = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-restart") && hasValue) {
            timing.myRestart = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-reps") && hasValue) {
            timing.myReps = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-mumpsMethods") && hasValue) {
            timing.myMumpsMethods = args[++i].split (",");
         }
         else if (arg.equals ("-pardisoMethods") && hasValue) {
            timing.myPardisoMethods = args[++i].split (",");
         }
         else if (arg.equals ("-criticalArrays")) {
            DirectSolverBase.setUseCriticalArrayAccess (true);
         }
         else if (arg.equals ("-noCriticalArrays")) {
            DirectSolverBase.setUseCriticalArrayAccess (false);
         }
         else {
            printUsageAndExit();
         }
      }
      if (outFile == null) {
         printUsageAndExit();
      }
      PrintWriter pw = new PrintWriter (outFile);
      pw.println (
         "solver,case,res,size,threads,eps,method,iters,solves,timeMs," +
         "factorMs,solveMs,ratio,relResid,nativeResid,precondMs,matvecMs," +
         "otherMs,jniMs");
      for (String caseName : timing.myCases) {
         for (int res : timing.myResolutions) {
            timing.runCase (caseName, res, pw);
         }
      }
      pw.close();
   }
}
