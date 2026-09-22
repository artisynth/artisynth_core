/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import maspack.matrix.Matrix;
import maspack.solvers.FemMatrixGenerator.CRSMatrix;
import maspack.solvers.FemMatrixGenerator.ElemType;

/**
 * Measures how the factor and solve time of a direct sparse solver varies with
 * thread count, across a range of matrix sizes, and then fits a rule that
 * predicts a good thread count from the matrix size or non-zero count alone.
 *
 * <p>Some solvers, MUMPS in particular, get <i>slower</i> beyond a certain
 * thread count, and the count at which that happens depends on the problem
 * size: small matrices are fastest single threaded, since there is not enough
 * work to cover the cost of starting and synchronizing a thread team. The
 * intent here is to determine, in advance and from cheaply available
 * quantities, how many threads a given matrix should be factored with.
 *
 * <p>The available test cases, each swept over a range of sizes, are:
 * <ul>
 * <li>{@code slab} - uniform hex slab, {@code n x n x n/4} nodes</li>
 * <li>{@code slabC} - the same, with constraints added at a ratio of 0.1</li>
 * <li>{@code grid} - uniform hex grid, {@code n x n x n} nodes</li>
 * <li>{@code gridC} - the same, with constraints added at a ratio of 0.1</li>
 * <li>{@code sheet} - uniform hex sheet, {@code n x n x 2} nodes (one element
 * thick)</li>
 * <li>{@code sheetC} - the same, with constraints added at a ratio of 0.1</li>
 * </ul>
 * By default, only {@code slab} and {@code slabC} are run. The constrained
 * cases are symmetric indefinite KKT systems, of the kind arising from
 * contact, while the unconstrained ones are SPD.
 *
 * <p>Usage, to run the sweep and write the raw timings to a CSV file:
 * <pre>
 *  java maspack.solvers.ThreadCountSweep -out sweep.csv \
 *     [-solver Mumps|Pardiso] [-cases grid,gridC,sheet,sheetC] \
 *     [-threads n,n,...] [-verbose]
 * </pre>
 * and then, to analyze the results (which may also be done on another
 * machine):
 * <pre>
 *  java maspack.solvers.ThreadCountSweep -analyze sweep.csv
 * </pre>
 * Running with {@code -out} performs the analysis automatically once the sweep
 * finishes.
 */
public class ThreadCountSweep {

   /**
    * Fraction by which a thread count may exceed the fastest observed time and
    * still be considered optimal. Used to locate the "knee" of the curve,
    * which is more stable than the raw minimum.
    */
   public static final double KNEE_TOL = 0.03;

   SparseSolverId mySolverId = SparseSolverId.Mumps;
   int[] myThreadCounts = null;
   LinkedHashSet<String> myCaseNames = new LinkedHashSet<>();
   boolean myVerbose = false;
   // fixed rep count overriding the ~400msec auto-tuned budget; <=0 means auto
   int myFixedReps = -1;
   // explicit Pardiso iparm overrides, as {index, value} pairs
   ArrayList<int[]> myPardisoIParams = new ArrayList<>();
   // if non-null, overrides the resolutions of every case
   int[] myResolutions = null;
   // if true, declare all matrices as symmetric indefinite
   boolean myForceSymmetric = false;

   // grid resolutions giving roughly 100 to 40000 nodes
   private static final int[] GRID_RES = {5, 7, 9, 12, 15, 19, 24, 29, 34};
   private static final int[] SHEET_RES =
      {7, 10, 14, 20, 28, 40, 56, 80, 112, 141};
   // slab resolutions giving roughly 400 to 150000 dofs
   private static final int[] SLAB_RES =
      {8, 12, 16, 20, 24, 32, 40, 48, 56};

   private enum Geometry { GRID, SHEET, SLAB };

   /**
    * Defines one of the test cases.
    */
   private static class Case {
      String name;
      Geometry geometry;
      double consRatio;
      boolean isDefault;

      Case (String name, Geometry geometry, double consRatio, boolean dflt) {
         this.name = name;
         this.geometry = geometry;
         this.consRatio = consRatio;
         this.isDefault = dflt;
      }

      int[] resolutions() {
         switch (geometry) {
            case GRID: return GRID_RES;
            case SHEET: return SHEET_RES;
            default: return SLAB_RES;
         }
      }

      /**
       * Number of nodes along z for a given x and y resolution.
       */
      int depth (int res) {
         switch (geometry) {
            case GRID: return res;
            case SHEET: return 2;
            default: return Math.max (2, res/4);
         }
      }
   }

   private static final Case[] CASES = new Case[] {
      new Case ("slab",   Geometry.SLAB,  0.0, true),
      new Case ("slabC",  Geometry.SLAB,  0.1, true),
      new Case ("grid",   Geometry.GRID,  0.0, false),
      new Case ("gridC",  Geometry.GRID,  0.1, false),
      new Case ("sheet",  Geometry.SHEET, 0.0, false),
      new Case ("sheetC", Geometry.SHEET, 0.1, false),
   };

   void applyIParams (DirectSolver solver) {
      if (!myPardisoIParams.isEmpty()) {
         if (!(solver instanceof PardisoSolver)) {
            throw new IllegalArgumentException (
               "-iparm requires -solver Pardiso");
         }
         for (int[] kv : myPardisoIParams) {
            ((PardisoSolver)solver).setIParam (kv[0], kv[1]);
         }
      }
   }

   /**
    * One (case, size) problem, together with the times measured for it at each
    * thread count.
    */
   public static class Problem {
      String caseName;
      int res;
      int size;
      long nnz;
      long nnzL;
      int[] threads;
      double[] costs;   // factor + solve, msec

      double bestCost() {
         double best = Double.MAX_VALUE;
         for (double c : costs) {
            best = Math.min (best, c);
         }
         return best;
      }

      /**
       * Returns the thread count giving the fastest time.
       */
      int optimalThreads() {
         return threads[indexOfMin()];
      }

      private int indexOfMin() {
         int imin = 0;
         for (int i=1; i<costs.length; i++) {
            if (costs[i] < costs[imin]) {
               imin = i;
            }
         }
         return imin;
      }

      /**
       * Returns the smallest thread count whose time is within {@link
       * #KNEE_TOL} of the fastest.
       */
      int kneeThreads() {
         double lim = bestCost()*(1+KNEE_TOL);
         for (int i=0; i<costs.length; i++) {
            if (costs[i] <= lim) {
               return threads[i];
            }
         }
         return threads[indexOfMin()];
      }

      /**
       * Returns the fractional time penalty incurred by using {@code num}
       * threads instead of the best available count. The nearest measured
       * thread count is used.
       */
      double penalty (double num) {
         int t = (int)Math.round (num);
         int inear = 0;
         for (int i=1; i<threads.length; i++) {
            if (Math.abs (threads[i]-t) < Math.abs (threads[inear]-t)) {
               inear = i;
            }
         }
         return costs[inear]/bestCost() - 1;
      }
   }

   // ------------------------------------------------------------------
   // sweep
   // ------------------------------------------------------------------

   /**
    * Returns the thread counts to test: every value up to 8, and then even
    * values, up to the number of processors reported by the runtime.
    */
   int[] defaultThreadCounts() {
      int maxThreads = Runtime.getRuntime().availableProcessors();
      ArrayList<Integer> list = new ArrayList<>();
      for (int t=1; t<=Math.min(8,maxThreads); t++) {
         list.add (t);
      }
      for (int t=10; t<=maxThreads; t+=2) {
         list.add (t);
      }
      int[] threads = new int[list.size()];
      for (int i=0; i<threads.length; i++) {
         threads[i] = list.get(i);
      }
      return threads;
   }

   void runCase (Case cs, PrintWriter pw) {
      int[] resolutions =
         (myResolutions != null ? myResolutions : cs.resolutions());
      for (int res : resolutions) {
         FemMatrixGenerator gen = new FemMatrixGenerator();
         CRSMatrix M = gen.create (
            ElemType.HEX, res, res, cs.depth (res), 3);
         int numNodes = gen.numNodes();
         int numCons = (int)Math.round (cs.consRatio*M.size);
         if (numCons > 0) {
            M = gen.addConstraints (M, numCons, 4, 3);
         }
         if (myForceSymmetric) {
            // ArtiSynth often declares matrices as symmetric indefinite even
            // when they have no constraints
            M.type = Matrix.SYMMETRIC;
         }
         double[] b = new double[M.size];
         double[] x = new double[M.size];
         for (int i=0; i<M.size; i++) {
            b[i] = 1.0;
         }
         System.out.println (
            cs.name+" res="+res+" nodes="+numNodes+" size="+M.size+
            " nnz="+M.numFullVals());
         for (int nt : myThreadCounts) {
            DirectSolver solver = mySolverId.createDirectSolver();
            ((DirectSolverBase)solver).setNumThreads (nt);
            applyIParams (solver);
            double[] vals = M.copyVals();

            double abest = Double.MAX_VALUE;
            double fbest = Double.MAX_VALUE;
            double sbest = Double.MAX_VALUE;
            try {
               // analyze is also repeated, since it is redone whenever the
               // matrix structure changes (e.g., with contact), and so its
               // cost matters when comparing reordering methods. The last
               // analyze is the one used by the factor reps below.
               int reps = (myFixedReps > 0 ? myFixedReps : 1);
               for (int rep=0; rep<reps; rep++) {
                  long t0 = System.nanoTime();
                  solver.analyze (vals, M.colIdxs, M.rowOffs, M.size, M.type);
                  double amsec = (System.nanoTime()-t0)/1e6;
                  abest = Math.min (abest, amsec);
                  if (rep == 0 && myFixedReps <= 0) {
                     reps = (int)Math.round (400/amsec);
                     reps = Math.max (2, Math.min (40, reps));
                  }
               }
               reps = (myFixedReps > 0 ? myFixedReps : 1);
               for (int rep=0; rep<reps; rep++) {
                  long t0 = System.nanoTime();
                  solver.factor (vals);
                  double fmsec = (System.nanoTime()-t0)/1e6;
                  t0 = System.nanoTime();
                  solver.solve (x, b);
                  double smsec = (System.nanoTime()-t0)/1e6;
                  fbest = Math.min (fbest, fmsec);
                  sbest = Math.min (sbest, smsec);
                  if (rep == 0 && myFixedReps <= 0) {
                     // choose a rep count spending roughly 400 msec in total;
                     // the 6-rep cap this used to have defeated that budget
                     // for fast (small/thin) problems, which is exactly where
                     // noise needs the most averaging, so the cap is much
                     // looser here
                     reps = (int)Math.round (400/(fmsec+smsec));
                     reps = Math.max (2, Math.min (40, reps));
                  }
               }
            }
            catch (Exception e) {
               System.out.println ("   nt=" + nt + " FAILED: " + e);
               solver.dispose();
               continue;
            }
            double amsec = abest;
            long nnzL = solver.getNumNonZerosInFactors();
            // Pardiso memory use in KB: peak analysis, permanent analysis,
            // and factor/solve; -1 if not available
            int[] mem = new int[] { -1, -1, -1 };
            // iparm[1] and iparm[23] as actually used; -1 if not available
            int[] ip = new int[] { -1, -1 };
            if (solver instanceof PardisoSolver) {
               PardisoSolver psolver = (PardisoSolver)solver;
               mem[0] = psolver.getPeakAnalysisMemoryUsage();
               mem[1] = psolver.getAnalysisMemoryUsage();
               mem[2] = psolver.getFactorSolveMemoryUsage();
               try {
                  ip[0] = psolver.getIParam (1);
                  ip[1] = psolver.getIParam (23);
               }
               catch (UnsatisfiedLinkError e) {
                  // native library predates getIParam()
               }
            }
            double resid = M.residual (x, b);
            solver.dispose();

            pw.printf (
               "%s,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%d,%.3g,%d,%d,%d%n",
               cs.name, res, numNodes, M.size, M.numFullVals(), nt,
               amsec, fbest, sbest, nnzL, resid, mem[0], mem[1], mem[2]);
            pw.flush();
            System.out.printf (
               "   nt=%2d analyze=%10.2f factor=%10.2f solve=%9.2f " +
               "nnzL=%d resid=%.2g iparm1=%d iparm23=%d%n",
               nt, amsec, fbest, sbest, nnzL, resid, ip[0], ip[1]);
         }
      }
   }

   public void runSweep (String fileName) throws IOException {
      if (myThreadCounts == null) {
         myThreadCounts = defaultThreadCounts();
      }
      System.out.println (
         "solver=" + mySolverId +
         " processors=" + Runtime.getRuntime().availableProcessors() +
         " threadCounts=" + Arrays.toString (myThreadCounts));
      if (!myPardisoIParams.isEmpty()) {
         StringBuilder sb = new StringBuilder ("iparm overrides:");
         for (int[] kv : myPardisoIParams) {
            sb.append (" iparm[" + kv[0] + "]=" + kv[1]);
         }
         System.out.println (sb.toString());
      }
      String omp = System.getenv ("OMP_NUM_THREADS");
      if (omp != null) {
         System.out.println (
            "note: OMP_NUM_THREADS is set to " + omp +
            "; it is overridden here by explicit setNumThreads() calls");
      }
      PrintWriter pw = new PrintWriter (fileName);
      pw.println (
         "case,res,nodes,size,nnz,threads,analyze,factor,solve,nnzL,resid," +
         "peakMemKB,permMemKB,factorMemKB");
      // discarded warmup, to absorb library loading and thread pool creation
      FemMatrixGenerator gen = new FemMatrixGenerator();
      CRSMatrix W = gen.create (ElemType.HEX, 6, 6, 6, 3);
      DirectSolver solver = mySolverId.createDirectSolver();
      double[] wvals = W.copyVals();
      solver.analyze (wvals, W.colIdxs, W.rowOffs, W.size, W.type);
      solver.factor (wvals);
      solver.dispose();

      for (Case cs : CASES) {
         if (myCaseNames.isEmpty() ?
             cs.isDefault : myCaseNames.contains (cs.name)) {
            runCase (cs, pw);
         }
      }
      pw.close();
      System.out.println ("\nwrote " + fileName);
   }

   // ------------------------------------------------------------------
   // analysis
   // ------------------------------------------------------------------

   public static ArrayList<Problem> readProblems (String fileName)
      throws IOException {

      LinkedHashMap<String,Problem> map = new LinkedHashMap<>();
      LinkedHashMap<String,ArrayList<double[]>> times = new LinkedHashMap<>();
      BufferedReader reader = new BufferedReader (new FileReader (fileName));
      String line = reader.readLine();  // header
      while ((line = reader.readLine()) != null) {
         line = line.trim();
         if (line.length() == 0) {
            continue;
         }
         String[] f = line.split (",");
         String key = f[0]+":"+f[1];
         Problem prob = map.get (key);
         if (prob == null) {
            prob = new Problem();
            prob.caseName = f[0];
            prob.res = Integer.parseInt (f[1]);
            prob.size = Integer.parseInt (f[3]);
            prob.nnz = Long.parseLong (f[4]);
            map.put (key, prob);
            times.put (key, new ArrayList<double[]>());
         }
         prob.nnzL = Long.parseLong (f[9]);
         times.get(key).add (
            new double[] { Double.parseDouble (f[5]),
                           Double.parseDouble (f[7])+Double.parseDouble (f[8]) });
      }
      reader.close();
      ArrayList<Problem> probs = new ArrayList<>();
      for (String key : map.keySet()) {
         Problem prob = map.get (key);
         ArrayList<double[]> tl = times.get (key);
         // a thread count may appear more than once if a sweep was rerun; the
         // last entry wins
         LinkedHashMap<Integer,Double> byThread = new LinkedHashMap<>();
         for (double[] tc : tl) {
            byThread.put ((int)tc[0], tc[1]);
         }
         int[] threads = new int[byThread.size()];
         double[] costs = new double[byThread.size()];
         int i = 0;
         for (Integer t : byThread.keySet()) {
            threads[i] = t;
            costs[i] = byThread.get(t);
            i++;
         }
         // sort by thread count
         for (int a=0; a<threads.length; a++) {
            for (int bb=a+1; bb<threads.length; bb++) {
               if (threads[bb] < threads[a]) {
                  int ti = threads[a]; threads[a] = threads[bb]; threads[bb] = ti;
                  double td = costs[a]; costs[a] = costs[bb]; costs[bb] = td;
               }
            }
         }
         prob.threads = threads;
         prob.costs = costs;
         probs.add (prob);
      }
      return probs;
   }

   /**
    * A rule predicting a thread count from a problem's size or non-zero count.
    */
   public interface Rule {
      double numThreads (Problem prob);
   }

   static double meanPenalty (ArrayList<Problem> probs, Rule rule) {
      double sum = 0;
      for (Problem p : probs) {
         sum += p.penalty (rule.numThreads (p));
      }
      return sum/probs.size();
   }

   static double maxPenalty (ArrayList<Problem> probs, Rule rule) {
      double max = 0;
      for (Problem p : probs) {
         max = Math.max (max, p.penalty (rule.numThreads (p)));
      }
      return max;
   }

   /**
    * Rule of the form {@code T = clamp (a*log2(x) + b, 1, maxThreads)}, where
    * {@code x} is either the matrix size or its non-zero count.
    */
   static class LogRule implements Rule {
      double a, b;
      int maxThreads;
      boolean useNnz;

      LogRule (double a, double b, int maxThreads, boolean useNnz) {
         this.a = a;
         this.b = b;
         this.maxThreads = maxThreads;
         this.useNnz = useNnz;
      }

      public double numThreads (Problem p) {
         double x = (useNnz ? p.nnz : p.size);
         double t = a*(Math.log(x)/Math.log(2)) + b;
         return Math.min (Math.max (t, 1), maxThreads);
      }

      public String toString() {
         return String.format (
            "T = clamp (%.2f*log2(%s) %s %.2f, 1, %d)",
            a, (useNnz ? "nnz" : "size"), (b < 0 ? "-" : "+"),
            Math.abs(b), maxThreads);
      }
   }

   /**
    * Searches for the {@code LogRule} coefficients minimizing the mean
    * measured time penalty. Fitting to the measured time rather than to the
    * optimal thread count matters: the penalty surface is flat near the
    * optimum, so a fit which minimizes thread count error spends its accuracy
    * where it is worth nothing.
    */
   static LogRule fitLogRule (
      ArrayList<Problem> probs, int[] threadCounts, boolean useNnz) {

      LogRule best = null;
      double bestPen = Double.MAX_VALUE;
      // The upper clamp is fitted rather than assumed: the largest useful
      // thread count is a property of the machine (typically its physical core
      // count, not its logical one), and is exactly what this sweep is meant
      // to discover.
      for (int maxThreads : threadCounts) {
         for (double a=0; a<=2.5001; a+=0.02) {
            for (double b=-32; b<=5.001; b+=0.25) {
               LogRule rule = new LogRule (a, b, maxThreads, useNnz);
               double pen = meanPenalty (probs, rule);
               if (pen < bestPen) {
                  bestPen = pen;
                  best = rule;
               }
            }
         }
      }
      return best;
   }

   /**
    * Rule which selects one of three thread counts based on two non-zero count
    * thresholds.
    */
   static class StepRule implements Rule {
      long nnz1, nnz2;
      int t0, t1, t2;

      StepRule (long nnz1, long nnz2, int t0, int t1, int t2) {
         this.nnz1 = nnz1;
         this.nnz2 = nnz2;
         this.t0 = t0;
         this.t1 = t1;
         this.t2 = t2;
      }

      public double numThreads (Problem p) {
         return (p.nnz < nnz1 ? t0 : (p.nnz < nnz2 ? t1 : t2));
      }

      public String toString() {
         // collapse the middle level when the fit did not need it
         if (t0 == t1) {
            return String.format (
               "T = %d if nnz < %d, else %d", t0, nnz2, t2);
         }
         else if (t1 == t2) {
            return String.format (
               "T = %d if nnz < %d, else %d", t0, nnz1, t1);
         }
         else {
            return String.format (
               "T = %d if nnz < %d, %d if nnz < %d, else %d",
               t0, nnz1, t1, nnz2, t2);
         }
      }
   }

   static StepRule fitStepRule (ArrayList<Problem> probs, int[] threadCounts) {
      StepRule best = null;
      double bestPen = Double.MAX_VALUE;
      for (int t2 : threadCounts) {
         for (int t1 : threadCounts) {
            if (t1 > t2) {
               continue;
            }
            for (int k1=12; k1<=22; k1++) {
               for (int k2=k1+1; k2<=24; k2++) {
                  StepRule rule = new StepRule (1L<<k1, 1L<<k2, 1, t1, t2);
                  double pen = meanPenalty (probs, rule);
                  if (pen < bestPen) {
                     bestPen = pen;
                     best = rule;
                  }
               }
            }
         }
      }
      return best;
   }

   public static void analyze (String fileName) throws IOException {
      ArrayList<Problem> probs = readProblems (fileName);
      if (probs.isEmpty()) {
         System.out.println ("No data found in " + fileName);
         return;
      }
      LinkedHashSet<String> caseNames = new LinkedHashSet<>();
      for (Problem p : probs) {
         caseNames.add (p.caseName);
      }
      System.out.println ("=== measured optima (cost = factor + solve) ===");
      System.out.printf (
         "%-8s%8s%11s%13s%5s%7s%11s%9s%n",
         "case", "size", "nnz", "nnzL", "T*", "Tknee", "best(ms)", "speedup");
      for (Problem p : probs) {
         System.out.printf (
            "%-8s%8d%11d%13d%5d%7d%11.2f%9.2f%n",
            p.caseName, p.size, p.nnz, p.nnzL, p.optimalThreads(),
            p.kneeThreads(), p.bestCost(), p.costs[0]/p.bestCost());
      }
      System.out.println (
         "\n=== constant thread counts (baseline) ===");
      for (int t : probs.get(0).threads) {
         final int tf = t;
         Rule rule = new Rule() {
            public double numThreads (Problem p) { return tf; }
         };
         System.out.printf (
            "  T=%-3d  meanPenalty=%6.1f%%  maxPenalty=%7.1f%%%n",
            t, 100*meanPenalty (probs, rule), 100*maxPenalty (probs, rule));
      }
      System.out.println (
         "\n=== fitted rules (coefficients chosen to minimize measured time) ===");
      LogRule byNnz = fitLogRule (probs, probs.get(0).threads, true);
      LogRule bySize = fitLogRule (probs, probs.get(0).threads, false);
      StepRule step = fitStepRule (probs, probs.get(0).threads);
      for (Rule rule : new Rule[] {byNnz, bySize, step}) {
         System.out.printf (
            "  %-46s meanPenalty=%5.1f%%  maxPenalty=%6.1f%%%n",
            rule, 100*meanPenalty (probs, rule), 100*maxPenalty (probs, rule));
      }
      Rule oracle = new Rule() {
         public double numThreads (Problem p) { return p.kneeThreads(); }
      };
      System.out.printf (
         "  %-46s meanPenalty=%5.1f%%  maxPenalty=%6.1f%%%n",
         "oracle (per-problem knee)", 100*meanPenalty (probs, oracle),
         100*maxPenalty (probs, oracle));

      if (caseNames.size() > 1) {
         System.out.println (
            "\n=== leave-one-case-out cross validation (nnz rule) ===");
         for (String name : caseNames) {
            ArrayList<Problem> train = new ArrayList<>();
            ArrayList<Problem> test = new ArrayList<>();
            for (Problem p : probs) {
               (p.caseName.equals (name) ? test : train).add (p);
            }
            LogRule rule = fitLogRule (train, probs.get(0).threads, true);
            System.out.printf (
               "  held out %-8s %-40s meanPenalty=%5.1f%%  maxPenalty=%6.1f%%%n",
               name, rule, 100*meanPenalty (test, rule),
               100*maxPenalty (test, rule));
         }
      }
      System.out.println (
         "\n=== penalty surface: cost/best at each thread count ===");
      StringBuilder sb = new StringBuilder (String.format ("%-8s%8s", "case", "size"));
      for (int t : probs.get(0).threads) {
         sb.append (String.format ("%6d", t));
      }
      System.out.println (sb.toString());
      for (Problem p : probs) {
         sb = new StringBuilder (
            String.format ("%-8s%8d", p.caseName, p.size));
         for (double c : p.costs) {
            sb.append (String.format ("%6.2f", c/p.bestCost()));
         }
         System.out.println (sb.toString());
      }
      System.out.println (
         "\nRecommended rule for this machine:\n  " + step +
         "\n  (" + byNnz + ")");
   }

   // ------------------------------------------------------------------

   private static int[] parseIntList (String str) {
      String[] strs = str.split (",");
      int[] vals = new int[strs.length];
      for (int i=0; i<strs.length; i++) {
         vals[i] = Integer.parseInt (strs[i].trim());
      }
      return vals;
   }

   private static void printUsageAndExit() {
      System.out.println (
         "Usage: java maspack.solvers.ThreadCountSweep\n" +
         "  -out <file.csv> [-solver Mumps|Pardiso]\n" +
         "  [-cases slab,slabC,grid,gridC,sheet,sheetC] [-threads n,n,...]\n" +
         "              (default cases are slab,slabC)\n" +
         "  [-res n,n,...]  (x/y resolutions to run, overriding the default\n" +
         "              range for each case; slab 32 and 48 give ~25k and\n" +
         "              ~85k dofs)\n" +
         "  [-symmetric]  (declare all matrices symmetric indefinite, even\n" +
         "              unconstrained ones)\n" +
         "  [-iparm k=v,k=v,...]  (Pardiso only: override iparm[k], using\n" +
         "              Intel's 0-based C indexing)\n" +
         "  [-reps n]  (fixed rep count per (size,threads), overriding the\n" +
         "              ~400msec auto-tuned budget; use to denoise fast cases)\n" +
         "  [-pardisoLib name]  (Pardiso native library to load, e.g.\n" +
         "              PardisoJNI.2021.1.1; default is PardisoSolver's)\n" +
         "or, to analyze results produced earlier:\n" +
         "  java maspack.solvers.ThreadCountSweep -analyze <file.csv>");
      System.exit (1);
   }

   public static void main (String[] args) throws IOException {
      ThreadCountSweep sweep = new ThreadCountSweep();
      String outFile = null;
      String analyzeFile = null;
      for (int i=0; i<args.length; i++) {
         String arg = args[i];
         if (arg.equals ("-out") && i+1 < args.length) {
            outFile = args[++i];
         }
         else if (arg.equals ("-analyze") && i+1 < args.length) {
            analyzeFile = args[++i];
         }
         else if (arg.equals ("-solver") && i+1 < args.length) {
            sweep.mySolverId = SparseSolverId.valueOf (args[++i]);
         }
         else if (arg.equals ("-cases") && i+1 < args.length) {
            for (String str : args[++i].split (",")) {
               sweep.myCaseNames.add (str.trim());
            }
         }
         else if (arg.equals ("-threads") && i+1 < args.length) {
            sweep.myThreadCounts = parseIntList (args[++i]);
         }
         else if (arg.equals ("-res") && i+1 < args.length) {
            sweep.myResolutions = parseIntList (args[++i]);
         }
         else if (arg.equals ("-symmetric")) {
            sweep.myForceSymmetric = true;
         }
         else if (arg.equals ("-verbose")) {
            sweep.myVerbose = true;
         }
         else if (arg.equals ("-reps") && i+1 < args.length) {
            sweep.myFixedReps = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-iparm") && i+1 < args.length) {
            for (String kv : args[++i].split (",")) {
               String[] strs = kv.split ("=");
               if (strs.length != 2) {
                  printUsageAndExit();
               }
               sweep.myPardisoIParams.add (
                  new int[] { Integer.parseInt (strs[0].trim()),
                              Integer.parseInt (strs[1].trim()) });
            }
         }
         else if (arg.equals ("-pardisoLib") && i+1 < args.length) {
            // must be set before the first PardisoSolver loads its library
            PardisoSolver.nativeLibrary = args[++i];
         }
         else {
            printUsageAndExit();
         }
      }
      if (analyzeFile != null) {
         analyze (analyzeFile);
      }
      else if (outFile != null) {
         sweep.runSweep (outFile);
         System.out.println ("");
         analyze (outFile);
      }
      else {
         printUsageAndExit();
      }
   }
}
