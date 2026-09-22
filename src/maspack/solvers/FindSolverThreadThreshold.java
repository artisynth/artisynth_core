/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.util.Arrays;

import maspack.solvers.FemMatrixGenerator.CRSMatrix;
import maspack.solvers.FemMatrixGenerator.ElemType;

/**
 * Finds the single matrix-size threshold below which a {@link
 * DirectSolverBase} subclass should be forced to one thread, and prints the
 * resulting {@code ThreadLimit} table entry -- see {@link
 * DirectSolverBase#maxThreadsNnz}, {@link DirectSolverBase.ThreadLimit} and
 * {@link DirectSolverBase#lookupMaxThreads} -- ready to paste into that
 * solver's override.
 *
 * <p>Some direct solvers incur a large, largely size-independent per-factor
 * cost to set up their thread team, so that for small matrices using more
 * than one thread is slower, often by several times, than staying
 * single-threaded; above some size that overhead becomes negligible against
 * the actual factorization cost, and more threads help (or at least stop
 * hurting). This finds that crossover size, expressed as the matrix nnz at
 * which it occurs, matching the convention {@link
 * DirectSolverBase#maxThreadsNnz} itself uses.
 *
 * <p>Unlike a full sweep that measures every thread count at every size,
 * this only needs to answer a yes/no question at each size it tries --
 * "does the solver's maximum thread count measurably beat one thread here?"
 * -- so it can locate the crossover by marching up an ascending list of
 * candidate sizes and stopping as soon as that answer flips from no to yes.
 * Since the threshold typically lies well below the largest realistic model
 * size, this usually finds it after testing only a handful of the smaller,
 * cheaper candidates, and never pays for the largest, slowest ones at all.
 * That, in turn, affords something a full sweep can't: re-measuring each
 * candidate several times (see {@link #TRIALS_PER_SIZE}) to guard against
 * run-to-run timing noise before trusting a verdict -- a wrong verdict
 * partway through the search misguides everything after it, not just one
 * data point, so it's worth spending time saved elsewhere on making each
 * verdict it does reach more reliable.
 *
 * <p>The search assumes the underlying effect is a single step: sizes below
 * the threshold are all one-thread-only, sizes above it all benefit from
 * (or are at least not hurt by) more threads, with no size above the
 * threshold reverting back below it -- so the first "yes" the march
 * encounters can be trusted as the crossover, with no need to keep
 * checking larger sizes for a reversal. This matches the {@link
 * DirectSolverBase.ThreadLimit} table format, whose entries are range upper
 * bounds and so cannot express such a reversal either.
 *
 * <p>Geometry and matrix generation are a cubic hex-element block with a
 * constrained (indefinite) boundary condition -- see {@link
 * FemMatrixGenerator} -- matching the kind of system ArtiSynth's own solves
 * most often present.
 *
 * <p>Usage:
 * <pre>
 *  java maspack.solvers.FindSolverThreadThreshold -solver Mumps|Pardiso
 *     [-maxThreads n] [-res n,n,...] [-verbose]
 * </pre>
 */
public class FindSolverThreadThreshold {

   // Candidate grid resolutions to search over, ascending, giving sizes from
   // a few hundred to a bit over 100000 dofs -- small enough at the low end
   // to resolve thresholds that lie at only a few thousand non-zeros (or
   // fewer), and large enough at the high end to confirm threading actually
   // pays off at realistic model sizes.
   private static final int[] DEFAULT_RES = { 5, 7, 9, 12, 15, 19, 24, 29, 34 };

   // Fraction of nodes given a constrained degree of freedom, for the
   // indefinite matrix case this class always generates.
   private static final double CONS_RATIO = 0.1;

   // Target time (msec) for a single (size,threads) measurement, and the
   // rep-count range used to hit it by re-running factor()+solve() and
   // keeping the best -- denoises small/fast sizes without paying for many
   // reps at large/slow ones.
   private static final double TARGET_MSEC = 150;
   private static final int MIN_REPS = 2;
   private static final int MAX_REPS = 15;

   // Once a single rep already costs this much (msec), reps is forced to 1
   // regardless of MIN_REPS: at the largest sizes tested, a single factor()
   // can itself take seconds, and a forced second rep buys little precision
   // for real time.
   private static final double MAX_POINT_MSEC = 800;

   // Independent trials per candidate size: each is a fresh (nt=1,
   // nt=maxThreads) comparison -- new solver instances each time, so
   // trial-to-trial variation reflects only timing noise, not anything
   // about the matrix (matrix generation here is deterministic). The
   // median cost across trials, not the mean, is used at each thread count,
   // so that a single slow outlier (a GC pause, a scheduler hiccup) cannot
   // by itself swing the verdict.
   private static final int TRIALS_PER_SIZE = 3;

   // Fraction by which the maxThreads cost must beat the nt=1 cost to count
   // as "threading is effective" at a given size. Looser than a typical
   // noise floor: a size wrongly called effective reports a threshold
   // that's too small (under-protecting small matrices from thread-team
   // overhead), which is the worse of the two possible mistakes, so this
   // errs toward requiring a clear win before believing one.
   private static final double EFFECTIVE_TOL = 0.15;

   private SparseSolverId mySolverId = null;
   private int myMaxThreads = -1;
   private int[] myRes = DEFAULT_RES;
   private boolean myVerbose = false;

   /**
    * Verdict for one candidate resolution: whether {@link #myMaxThreads}
    * threads measurably beat one thread there, and the matrix's full nnz
    * (for reporting -- the threshold this class ultimately prints is
    * expressed in nnz, not resolution).
    */
   private static class SizeVerdict {
      boolean effective;
      long nnz;
   }

   /**
    * Measures the best (lowest) factor+solve cost, in msec, for matrix
    * {@code M} at thread count {@code nt}, using a fresh solver instance.
    * Internally repeats the factor()+solve() pair, keeping the best, until
    * either {@link #TARGET_MSEC} worth of measurement has accumulated or
    * {@link #MAX_REPS} reps have run -- except that once a single rep
    * already costs {@link #MAX_POINT_MSEC} or more, no further reps are
    * attempted.
    */
   private double measureCost (CRSMatrix M, double[] b, double[] x, int nt) {
      double[] vals = M.copyVals();
      DirectSolver solver = mySolverId.createDirectSolver();
      ((DirectSolverBase)solver).setNumThreads (nt);
      double fbest = Double.MAX_VALUE;
      double sbest = Double.MAX_VALUE;
      try {
         solver.analyze (vals, M.colIdxs, M.rowOffs, M.size, M.type);
         int reps = 1;
         for (int rep=0; rep<reps; rep++) {
            long t0 = System.nanoTime();
            solver.factor (vals);
            double fmsec = (System.nanoTime()-t0)/1e6;
            t0 = System.nanoTime();
            solver.solve (x, b);
            double smsec = (System.nanoTime()-t0)/1e6;
            fbest = Math.min (fbest, fmsec);
            sbest = Math.min (sbest, smsec);
            if (rep == 0) {
               double pointMsec = fmsec+smsec;
               if (pointMsec >= MAX_POINT_MSEC) {
                  reps = 1;
               }
               else {
                  reps = (int)Math.round (TARGET_MSEC/pointMsec);
                  reps = Math.max (MIN_REPS, Math.min (MAX_REPS, reps));
               }
            }
         }
      }
      finally {
         solver.dispose();
      }
      return fbest+sbest;
   }

   private static double median (double[] vals) {
      double[] sorted = vals.clone();
      Arrays.sort (sorted);
      return sorted[sorted.length/2];
   }

   /**
    * Returns the verdict for grid resolution {@code res}. See {@link
    * #TRIALS_PER_SIZE} and {@link #EFFECTIVE_TOL} for how the verdict
    * itself is determined.
    *
    * <p>Prints a progress line for {@code res} before running any of the
    * (potentially slow, at the larger candidate sizes) measurements this
    * requires, so that a long pause is legible as "still working on this
    * size" rather than looking stuck.
    */
   private SizeVerdict verdictAt (int res) {
      FemMatrixGenerator gen = new FemMatrixGenerator();
      CRSMatrix M = gen.create (ElemType.HEX, res, res, res, 3);
      int numCons = (int)Math.round (CONS_RATIO*M.size);
      M = gen.addConstraints (M, numCons, 4, 3);
      double[] b = new double[M.size];
      double[] x = new double[M.size];
      Arrays.fill (b, 1.0);

      long nnz = M.numFullVals();
      System.out.printf (
         "res=%3d size=%6d nnz=%8d  measuring nt=1 vs nt=%d (%d trials each) ...%n",
         res, M.size, nnz, myMaxThreads, TRIALS_PER_SIZE);

      double[] c1 = new double[TRIALS_PER_SIZE];
      double[] cn = new double[TRIALS_PER_SIZE];
      for (int i=0; i<TRIALS_PER_SIZE; i++) {
         c1[i] = measureCost (M, b, x, 1);
         cn[i] = measureCost (M, b, x, myMaxThreads);
      }
      double m1 = median (c1);
      double mn = median (cn);

      SizeVerdict v = new SizeVerdict();
      v.nnz = nnz;
      v.effective = mn < m1*(1-EFFECTIVE_TOL);

      System.out.printf (
         "   nt=1 median=%8.2f  nt=%d median=%8.2f  %s%n",
         m1, myMaxThreads, mn, v.effective ? "effective" : "not yet");
      if (myVerbose) {
         System.out.println (
            "   nt=1 trials=" + Arrays.toString (c1) +
            "  nt=" + myMaxThreads + " trials=" + Arrays.toString (cn));
      }
      return v;
   }

   public void run() {
      if (myMaxThreads <= 0) {
         myMaxThreads = Runtime.getRuntime().availableProcessors();
         System.out.println (
            "note: no -maxThreads given; using " + myMaxThreads +
            " logical processors (Runtime.availableProcessors()). If this\n" +
            "machine has hyperthreading, pass its PHYSICAL core count with\n" +
            "-maxThreads instead: hyperthreads typically add little or no\n" +
            "extra factorization throughput, so including them risks\n" +
            "crediting \"threading\" for a benefit that actually plateaued\n" +
            "well below -maxThreads, and so under-reporting the true\n" +
            "threshold.\n");
      }
      System.out.println (
         "solver=" + mySolverId + " maxThreads=" + myMaxThreads +
         " candidate resolutions=" + Arrays.toString (myRes));

      // warmup, discarded: absorbs library loading and thread pool creation
      FemMatrixGenerator gen = new FemMatrixGenerator();
      CRSMatrix W = gen.create (ElemType.HEX, 6, 6, 6, 3);
      DirectSolver warm = mySolverId.createDirectSolver();
      double[] wvals = W.copyVals();
      warm.analyze (wvals, W.colIdxs, W.rowOffs, W.size, W.type);
      warm.factor (wvals);
      warm.dispose();

      System.out.println ("\n=== searching ===");
      SizeVerdict prev = null;
      int prevRes = -1;
      for (int res : myRes) {
         SizeVerdict v = verdictAt (res);
         if (v.effective) {
            if (prev == null) {
               System.out.println (
                  "\nThreading is already effective at the smallest\n" +
                  "resolution tested (res=" + res + ", nnz=" + v.nnz +
                  "). No threshold was found -- either this solver does\n" +
                  "not need throttling at all, or the threshold lies\n" +
                  "below the smallest size tested here; try a smaller\n" +
                  "-res to check the latter.");
               return;
            }
            System.out.println (
               "\nThreshold found between res=" + prevRes + " (nnz=" +
               prev.nnz + ", not yet effective) and res=" + res + " (nnz=" +
               v.nnz + ", effective).");
            printRecommendation (prev.nnz);
            return;
         }
         prev = v;
         prevRes = res;
      }
      System.out.println (
         "\nThreading is still not effective at the largest resolution\n" +
         "tested (res=" + prevRes + ", nnz=" + prev.nnz + "). No\n" +
         "threshold was found within the tested range; try a larger\n" +
         "-res to search further, or, if that size already exceeds\n" +
         "realistic model sizes, just cap this solver to one thread\n" +
         "unconditionally.");
   }

   private void printRecommendation (long nnz) {
      String tableName = mySolverId.toString().toUpperCase() + "_THREAD_LIMITS";
      System.out.println (
         "\nRecommended entry -- paste into " + mySolverId +
         "Solver's maxThreadsNnz() table (" + tableName + "):\n");
      System.out.println ("   new ThreadLimit (" + nnz + ", 1),");
      System.out.println (
         "\nSizes at or below this nnz are capped to one thread; larger\n" +
         "sizes are left uncapped by this entry, which -- via\n" +
         "DirectSolverBase.applyThreadThrottle()'s use of\n" +
         "myNominalNumThreads -- still limits them to this machine's\n" +
         "physical core count rather than to -maxThreads. This is a\n" +
         "starting point, not a final answer: rerun it a few times and\n" +
         "check for run-to-run agreement before committing to it, and\n" +
         "sanity check the threshold against a realistic model size.");
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
         "Usage: java maspack.solvers.FindSolverThreadThreshold\n" +
         "  -solver Mumps|Pardiso\n" +
         "  [-maxThreads n]  (default: Runtime.availableProcessors(), which\n" +
         "              counts logical, not physical, cores)\n" +
         "  [-res n,n,...]  (ascending candidate grid resolutions to search\n" +
         "              over; default is " + Arrays.toString (DEFAULT_RES) + ")\n" +
         "  [-verbose]  (print the per-trial costs behind each resolution's\n" +
         "              median, not just the median itself)");
      System.exit (1);
   }

   public static void main (String[] args) {
      FindSolverThreadThreshold opt = new FindSolverThreadThreshold();
      for (int i=0; i<args.length; i++) {
         String arg = args[i];
         if (arg.equals ("-solver") && i+1 < args.length) {
            opt.mySolverId = SparseSolverId.valueOf (args[++i]);
         }
         else if (arg.equals ("-maxThreads") && i+1 < args.length) {
            opt.myMaxThreads = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-res") && i+1 < args.length) {
            opt.myRes = parseIntList (args[++i]);
         }
         else if (arg.equals ("-verbose")) {
            opt.myVerbose = true;
         }
         else {
            printUsageAndExit();
         }
      }
      if (opt.mySolverId != SparseSolverId.Mumps &&
          opt.mySolverId != SparseSolverId.Pardiso) {
         // other SparseSolverId values either aren't DirectSolverBase
         // subclasses (Umfpack) or aren't thread-controllable (CG), so
         // setNumThreads()/maxThreadsNnz() don't apply to them
         printUsageAndExit();
      }
      if (opt.myRes.length < 2) {
         System.out.println ("-res must give at least two resolutions");
         System.exit (1);
      }
      opt.run();
   }
}
