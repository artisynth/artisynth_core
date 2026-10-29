/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.util.UnitTest;

/**
 * Tests the refactor and backoff decisions of HybridSolvePolicy using
 * scripted solve times.
 */
public class HybridSolvePolicyTest extends UnitTest {

   // does one skipped (direct) solve, checking that iterative solves are denied
   void skipSolve (HybridSolvePolicy policy, double tdirect) {
      check ("iterative allowed during backoff", !policy.useIterative());
      policy.recordDirect (tdirect);
   }

   // does one iterative solve, checking that iterative solves are allowed
   void iterSolve (HybridSolvePolicy policy, double titer) {
      check ("iterative not allowed", policy.useIterative());
      policy.recordIterative (titer);
   }

   public void testRefactor() {
      HybridSolvePolicy policy = new HybridSolvePolicy();
      check ("iterative allowed before first factor", !policy.useIterative());
      policy.recordDirect (10);
      checkEquals ("cycle solves", policy.getCycleSolves(), 1);
      // average solve times: 10, 6, 5, 4.75
      iterSolve (policy, 2);
      iterSolve (policy, 3);
      iterSolve (policy, 4);
      check ("refactor pending too early", !policy.isRefactorPending());
      checkEquals ("cycle average", policy.getCycleAverage(), 4.75, 1e-14);
      // 5 >= 4.75, so the average would increase
      iterSolve (policy, 5);
      check ("refactor not pending", policy.isRefactorPending());
      check ("iterative allowed when refactor pending", !policy.useIterative());
      checkEquals ("backoff", policy.getBackoff(), 0);
      policy.recordDirect (10);
      check ("refactor still pending", !policy.isRefactorPending());
      checkEquals ("cycle solves", policy.getCycleSolves(), 1);
      iterSolve (policy, 2);
      checkEquals ("cycle solves", policy.getCycleSolves(), 2);

      // invalidating the factor requires a direct solve
      policy.invalidateFactor();
      check ("iterative allowed after invalidate", !policy.useIterative());
      policy.recordDirect (10);
      iterSolve (policy, 2);

      policy.reset();
      check ("iterative allowed after reset", !policy.useIterative());
      checkEquals ("direct solves after reset", policy.numDirectSolves(), 0);
   }

   public void testBackoff() {
      HybridSolvePolicy policy = new HybridSolvePolicy();
      policy.setMaxBackoff (8);
      policy.recordDirect (10);
      int[] expectedBackoffs = new int[] { 2, 4, 8, 8 };
      for (int k=0; k<expectedBackoffs.length; k++) {
         // first iterative solve no faster than the direct solve
         iterSolve (policy, 10);
         int backoff = expectedBackoffs[k];
         checkEquals ("backoff", policy.getBackoff(), backoff);
         for (int i=0; i<backoff; i++) {
            checkEquals ("skip count", policy.getSkipCount(), backoff-i);
            skipSolve (policy, 10);
         }
         checkEquals ("skip count", policy.getSkipCount(), 0);
      }
      checkEquals ("num backoffs", policy.numBackoffs(), 4);
      // successful first iterative solve clears the backoff
      iterSolve (policy, 9);
      checkEquals ("backoff after success", policy.getBackoff(), 0);
      // 10 >= average 9.5
      iterSolve (policy, 10);
      check ("refactor not pending", policy.isRefactorPending());
      checkEquals ("backoff after later refactor", policy.getBackoff(), 0);
   }

   public void testFailures() {
      HybridSolvePolicy policy = new HybridSolvePolicy();
      policy.recordDirect (10);
      // failure in the first iterative solve causes a backoff
      check ("iterative not allowed", policy.useIterative());
      policy.recordIterativeFailure();
      check ("refactor not pending", policy.isRefactorPending());
      policy.recordDirect (10);
      checkEquals ("backoff after first-solve failure", policy.getBackoff(), 2);
      skipSolve (policy, 10);
      skipSolve (policy, 10);
      // failure in a later solve does not
      iterSolve (policy, 2);
      checkEquals ("backoff after success", policy.getBackoff(), 0);
      check ("iterative not allowed", policy.useIterative());
      policy.recordIterativeFailure();
      policy.recordDirect (10);
      checkEquals ("backoff after later failure", policy.getBackoff(), 0);
      checkEquals ("failed solves", policy.numFailedSolves(), 2);
      check ("iterative not allowed", policy.useIterative());
   }

   public void test() {
      testRefactor();
      testBackoff();
      testFailures();
   }

   public static void main (String[] args) {
      HybridSolvePolicyTest tester = new HybridSolvePolicyTest();
      tester.runtest();
   }
}
