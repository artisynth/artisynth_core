/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

/**
 * Decides when hybrid direct/iterative solves should use an iterative solve
 * (preconditioned by the most recent factorization) and when they should
 * refactor.
 *
 * <p>Solves are grouped into cycles, each beginning with a direct solve. If
 * {@code t_d} is the time of the direct solve and {@code t_1, ... t_n} are the
 * times of the following iterative solves, the policy tries to minimize the
 * average solve time
 * <pre>
 * tavg = (t_d + t_1 + ... + t_n) / (n+1)
 * </pre>
 * by requesting a refactor as soon as {@code tavg} stops decreasing, i.e.,
 * when {@code t_n >= tavg_(n-1)}. An iterative solve failure also causes a
 * refactor.
 *
 * <p>If hybrid solves give no benefit in a cycle, because the first iterative
 * solve takes at least as long as the direct solve, or because it fails, the
 * policy backs off by skipping iterative solves for a number of solves. The
 * backoff starts at {@link #getInitialBackoff} and doubles for each
 * consecutive unsuccessful cycle, up to {@link #getMaxBackoff}. It is cleared
 * when a first iterative solve succeeds.
 *
 * <p>Times can be given in any units, provided they are consistent.
 */
public class HybridSolvePolicy {

   public static final int DEFAULT_INITIAL_BACKOFF = 2;
   public static final int DEFAULT_MAX_BACKOFF = 32;

   private int myInitialBackoff = DEFAULT_INITIAL_BACKOFF;
   private int myMaxBackoff = DEFAULT_MAX_BACKOFF;

   // factorization state
   private boolean myFactorValid = false;
   private boolean myRefactorPending = false;

   // current cycle: completed solves since, and including, the direct solve
   private double myCycleTime = 0;
   private int myCycleSolves = 0;

   // backoff
   private int myBackoff = 0;   // length of the most recent backoff
   private int mySkipCnt = 0;   // solves remaining in the current backoff

   // counters
   private int myNumDirectSolves = 0;
   private int myNumIterativeSolves = 0;
   private int myNumFailedSolves = 0;
   private int myNumBackoffs = 0;

   /**
    * Resets the policy to its initial state, clearing all timing, cycle and
    * backoff information. Settings are preserved.
    */
   public void reset() {
      myFactorValid = false;
      myRefactorPending = false;
      myCycleTime = 0;
      myCycleSolves = 0;
      myBackoff = 0;
      mySkipCnt = 0;
      myNumDirectSolves = 0;
      myNumIterativeSolves = 0;
      myNumFailedSolves = 0;
      myNumBackoffs = 0;
   }

   /**
    * Indicates that the current factorization is no longer valid (e.g.,
    * because the matrix structure has been reanalyzed), so that the next
    * solve must be direct.
    */
   public void invalidateFactor() {
      myFactorValid = false;
      myRefactorPending = false;
   }

   /**
    * Queries whether the next solve should be iterative. Should be called
    * only when an iterative solve is otherwise possible, since a negative
    * answer may count toward a backoff.
    *
    * @return {@code true} if the next solve should be iterative
    */
   public boolean useIterative() {
      if (!myFactorValid) {
         return false;
      }
      if (mySkipCnt > 0) {
         mySkipCnt--;
         return false;
      }
      return !myRefactorPending;
   }

   /**
    * Records a direct (factor and) solve, which starts a new cycle.
    *
    * @param time time required for the solve
    */
   public void recordDirect (double time) {
      myCycleTime = time;
      myCycleSolves = 1;
      myFactorValid = true;
      myRefactorPending = false;
      myNumDirectSolves++;
   }

   /**
    * Records a successful iterative solve.
    *
    * @param time time required for the solve
    */
   public void recordIterative (double time) {
      myNumIterativeSolves++;
      if (myCycleSolves > 0) {
         double tavg = myCycleTime/myCycleSolves;
         if (time >= tavg) {
            // average solve time would no longer decrease
            myRefactorPending = true;
            if (myCycleSolves == 1) {
               // first iterative solve no faster than the direct solve
               backoff();
            }
         }
         else if (myCycleSolves == 1) {
            myBackoff = 0;
         }
         myCycleTime += time;
         myCycleSolves++;
      }
   }

   /**
    * Records a failed iterative solve. The caller is expected to follow
    * this with a direct solve. The time spent in the failed solve is not
    * recorded, so that the ensuing direct solve starts a cycle in the same
    * way as when a refactor is requested.
    */
   public void recordIterativeFailure () {
      myRefactorPending = true;
      myNumFailedSolves++;
      if (myCycleSolves <= 1) {
         // failed on the first iterative solve of the cycle
         backoff();
      }
   }

   private void backoff() {
      if (myBackoff == 0) {
         myBackoff = myInitialBackoff;
      }
      else {
         myBackoff = Math.min (2*myBackoff, myMaxBackoff);
      }
      mySkipCnt = myBackoff;
      myNumBackoffs++;
   }

   /**
    * Queries whether a refactor has been requested.
    *
    * @return {@code true} if a refactor is pending
    */
   public boolean isRefactorPending() {
      return myRefactorPending;
   }

   /**
    * Returns the number of completed solves in the current cycle, including
    * the direct solve.
    *
    * @return number of solves in the current cycle
    */
   public int getCycleSolves() {
      return myCycleSolves;
   }

   /**
    * Returns the average solve time for the completed solves in the current
    * cycle, or 0 if there are none.
    *
    * @return average solve time for the current cycle
    */
   public double getCycleAverage() {
      return myCycleSolves > 0 ? myCycleTime/myCycleSolves : 0;
   }

   /**
    * Returns the length of the most recent backoff, or 0 if the backoff has
    * been cleared.
    *
    * @return current backoff length
    */
   public int getBackoff() {
      return myBackoff;
   }

   /**
    * Returns the number of solves remaining in the current backoff.
    *
    * @return remaining backoff solves
    */
   public int getSkipCount() {
      return mySkipCnt;
   }

   public int getInitialBackoff() {
      return myInitialBackoff;
   }

   public void setInitialBackoff (int n) {
      if (n < 1) {
         throw new IllegalArgumentException ("initial backoff must be >= 1");
      }
      myInitialBackoff = n;
   }

   public int getMaxBackoff() {
      return myMaxBackoff;
   }

   public void setMaxBackoff (int n) {
      if (n < 1) {
         throw new IllegalArgumentException ("max backoff must be >= 1");
      }
      myMaxBackoff = n;
   }

   public int numDirectSolves() {
      return myNumDirectSolves;
   }

   public int numIterativeSolves() {
      return myNumIterativeSolves;
   }

   public int numFailedSolves() {
      return myNumFailedSolves;
   }

   public int numBackoffs() {
      return myNumBackoffs;
   }
}
