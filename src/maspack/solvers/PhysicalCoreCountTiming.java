/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

/**
 * Quick timing check for {@link DirectSolverBase#getPhysicalCoreCountNative},
 * backed by {@code hybridGetPhysicalCoreCount()} in {@code hybridSolve.cc}.
 * Run after a native rebuild to confirm the physical-core query -- added so
 * solver initialization doesn't need to spawn an external process -- is
 * cheap enough to do unconditionally every time a solver is constructed.
 *
 * <p>Calls {@code getPhysicalCoreCountNative()} directly (not the cached
 * {@code getPhysicalCoreCount()} wrapper), on a freshly constructed solver,
 * so the first call measured is genuinely uncached at the native level too
 * -- see {@code hybridSolve.cc}'s own {@code thePhysicalCoreCount} cache,
 * which is per-DLL (MumpsJNI and PardisoJNI each have their own copy of
 * hybridSolve.obj, hence their own copy of that cache), so each solver's
 * first call here is a real OS query, not served by the other's cache.
 *
 * <p>Usage: {@code java maspack.solvers.PhysicalCoreCountTiming}
 */
public class PhysicalCoreCountTiming {

   static void time (String label, DirectSolverBase solver, int reps) {
      long t0 = System.nanoTime();
      int first = solver.getPhysicalCoreCountNative();
      double firstMsec = (System.nanoTime()-t0)/1e6;

      // repeated calls: native-level cache (see hybridSolve.cc) should make
      // these cheap -- this is the JNI round-trip + cache-check floor
      int last = -1;
      t0 = System.nanoTime();
      for (int i=0; i<reps; i++) {
         last = solver.getPhysicalCoreCountNative();
      }
      double avgUsec = (System.nanoTime()-t0)/1e3/reps;

      System.out.printf (
         "%-8s physicalCores=%d  first call=%.4f msec  " +
         "avg of %d subsequent calls=%.4f usec%n",
         label, first, firstMsec, reps, avgUsec);
      if (last != first) {
         System.out.println (
            "   WARNING: result changed between calls (" +
            first + " vs " + last + ")");
      }
   }

   public static void main (String[] args) {
      MumpsSolver.printThreadInfo = false;
      PardisoSolver.printThreadInfo = false;

      try {
         MumpsSolver mumps = new MumpsSolver();
         time ("Mumps", mumps, 100000);
         mumps.dispose();
      }
      catch (Exception e) {
         System.out.println ("Mumps unavailable: " + e);
      }

      try {
         PardisoSolver pardiso = new PardisoSolver();
         time ("Pardiso", pardiso, 100000);
         pardiso.dispose();
      }
      catch (Exception e) {
         System.out.println ("Pardiso unavailable: " + e);
      }
   }
}
