/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.util.Random;

import maspack.matrix.ImproperStateException;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.solvers.NMFactorization.Init;
import maspack.solvers.NMFactorization.Normalization;
import maspack.solvers.NMFactorization.Update;
import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.UnitTest;

public class NMFactorizationTest extends UnitTest {

   /**
    * Creates a random non-negative matrix with entries in the range [0,1].
    */
   private MatrixNd createRandomMatrix (int nrows, int ncols) {
      MatrixNd M = new MatrixNd (nrows, ncols);
      M.setRandom (0, 1);
      return M;
   }

   /**
    * Creates a random non-negative {@code m X k} matrix which is
    * <i>separable</i>: each column {@code j} has a row {@code j} in which it is
    * the only non-zero entry. Together with a separable {@code H}, this makes
    * the factorization of {@code W H} unique up to permutation and scaling.
    */
   private MatrixNd createSeparableMatrix (int nrows, int ncols) {
      MatrixNd M = new MatrixNd (nrows, ncols);
      Random rand = RandomGenerator.get();
      for (int i=0; i<nrows; i++) {
         for (int j=0; j<ncols; j++) {
            if (i < ncols) {
               // leading ncols X ncols block is diagonal
               M.set (i, j, i == j ? 1.0 : 0.0);
            }
            else {
               M.set (i, j, 0.1 + rand.nextDouble());
            }
         }
      }
      return M;
   }

   private void checkNonNegative (String msg, MatrixNd M) {
      for (int i=0; i<M.rowSize(); i++) {
         for (int j=0; j<M.colSize(); j++) {
            if (M.get(i,j) < 0) {
               throw new TestException (
                  msg + ": entry ("+i+","+j+") is negative: "+M.get(i,j));
            }
         }
      }
   }

   /**
    * Returns the relative residual {@code ||V - W H||/||V||}, computed
    * directly from the factors.
    */
   private double computeResidual (MatrixNd V, MatrixNd W, MatrixNd H) {
      MatrixNd P = new MatrixNd (V.rowSize(), V.colSize());
      P.mul (W, H);
      P.sub (V);
      return P.frobeniusNorm()/V.frobeniusNorm();
   }

   /**
    * Finds, for each column of {@code Wchk}, the column of {@code W} which is
    * most nearly parallel to it, and returns the resulting permutation.
    * Verifies that the result actually is a permutation.
    */
   private int[] matchColumns (String msg, MatrixNd W, MatrixNd Wchk) {
      int k = Wchk.colSize();
      int[] perm = new int[k];
      boolean[] used = new boolean[k];
      VectorNd w = new VectorNd (W.rowSize());
      VectorNd wchk = new VectorNd (W.rowSize());
      for (int j=0; j<k; j++) {
         Wchk.getColumn (j, wchk);
         wchk.normalize();
         double maxDot = -1;
         int maxIdx = -1;
         for (int l=0; l<k; l++) {
            if (!used[l]) {
               W.getColumn (l, w);
               if (w.norm() > 0) {
                  w.normalize();
                  double dot = w.dot (wchk);
                  if (dot > maxDot) {
                     maxDot = dot;
                     maxIdx = l;
                  }
               }
            }
         }
         if (maxIdx == -1) {
            throw new TestException (msg + ": no match found for column "+j);
         }
         used[maxIdx] = true;
         perm[j] = maxIdx;
      }
      return perm;
   }

   /**
    * Checks that {@code W} equals {@code Wchk} up to a permutation and
    * positive scaling of the columns.
    */
   private void checkFactorsEqual (
      String msg, MatrixNd W, MatrixNd Wchk, int[] perm, double eps) {
      VectorNd w = new VectorNd (W.rowSize());
      VectorNd wchk = new VectorNd (W.rowSize());
      for (int j=0; j<perm.length; j++) {
         W.getColumn (perm[j], w);
         Wchk.getColumn (j, wchk);
         double nrm = w.norm();
         double nrmchk = wchk.norm();
         if (nrm > 0) {
            w.scale (1/nrm);
         }
         if (nrmchk > 0) {
            wchk.scale (1/nrmchk);
         }
         if (!w.epsilonEquals (wchk, eps)) {
            throw new TestException (
               msg + ": column "+j+" =\n" + w + "\nexpected\n" + wchk +
               "\neps=" + eps);
         }
      }
   }

   /**
    * Tests that an exactly factorable matrix is recovered, both in terms of
    * its residual and of the factors themselves.
    */
   public void testExactRecovery() {
      int m = 10;
      int n = 24;

      for (int k=1; k<=4; k++) {
         MatrixNd Wchk = createSeparableMatrix (m, k);
         MatrixNd Hchk = createSeparableMatrix (n, k);
         Hchk.transpose();
         MatrixNd V = new MatrixNd (m, n);
         V.mul (Wchk, Hchk);

         NMFactorization nmf = new NMFactorization();
         nmf.setMaxIterations (2000);
         nmf.setTolerance (1e-14);
         nmf.setNormalization (Normalization.NONE);
         nmf.factor (V, k);

         MatrixNd W = nmf.getW();
         MatrixNd H = nmf.getH();
         checkNonNegative ("exact recovery, k="+k+", W", W);
         checkNonNegative ("exact recovery, k="+k+", H", H);
         checkEquals (
            "exact recovery, k="+k+", numFactors", nmf.getNumFactors(), k);

         double res = computeResidual (V, W, H);
         check (
            "exact recovery, k="+k+", residual "+res+" > 1e-8", res < 1e-8);
         checkEquals (
            "exact recovery, k="+k+", residual",
            nmf.getResidual(), res, 1e-8);

         // factors should be recovered up to permutation and scaling
         int[] perm = matchColumns ("exact recovery, k="+k, W, Wchk);
         checkFactorsEqual (
            "exact recovery, k="+k+", W", W, Wchk, perm, 1e-6);
         MatrixNd Ht = new MatrixNd (H);
         Ht.transpose();
         MatrixNd Hchkt = new MatrixNd (Hchk);
         Hchkt.transpose();
         checkFactorsEqual (
            "exact recovery, k="+k+", H", Ht, Hchkt, perm, 1e-6);
      }
   }

   /**
    * Tests the factorization of matrices which are not exactly factorable,
    * checking non-negativity of the factors and consistency of the reported
    * residual and VAF with values computed directly from the factors.
    */
   public void testGeneralFactorization() {
      int[][] sizes = new int[][] {
         {6, 30}, {30, 6}, {12, 12}, {1, 8}, {8, 1}, {25, 40} };

      for (int[] size : sizes) {
         int m = size[0];
         int n = size[1];
         MatrixNd V = createRandomMatrix (m, n);
         for (int k=1; k<=Math.min(m,n); k++) {
            NMFactorization nmf = new NMFactorization();
            nmf.factor (V, k);
            MatrixNd W = nmf.getW();
            MatrixNd H = nmf.getH();
            String msg = "general ("+m+"x"+n+"), k="+k;
            checkNonNegative (msg+", W", W);
            checkNonNegative (msg+", H", H);
            checkEquals (msg+", W size", W.rowSize(), m);
            checkEquals (msg+", W cols", W.colSize(), k);
            checkEquals (msg+", H rows", H.rowSize(), k);
            checkEquals (msg+", H size", H.colSize(), n);

            // reported residual and VAF must match direct computation
            double res = computeResidual (V, W, H);
            checkEquals (msg+", residual", nmf.getResidual(), res, 1e-8);
            checkEquals (msg+", VAF", nmf.getVAF(), 1-res*res, 1e-8);
            check (msg+", VAF out of range", nmf.getVAF() <= 1.0);

            // getReconstruction() must equal W H
            MatrixNd P = new MatrixNd (m, n);
            P.mul (W, H);
            checkEquals (msg+", reconstruction", nmf.getReconstruction(), P,
                         1e-12);
         }
         // a full rank factorization should be essentially exact
         NMFactorization nmf = new NMFactorization();
         nmf.setMaxIterations (2000);
         nmf.factor (V, Math.min (m, n));
         check ("general ("+m+"x"+n+"), full rank VAF = "+nmf.getVAF(),
                nmf.getVAF() > 0.99);
      }
   }

   /**
    * Tests that the residual decreases monotonically with the number of
    * iterations, as it must for the exact block coordinate descent performed
    * by the ANLS update.
    */
   public void testMonotonicity() {
      int m = 15;
      int n = 30;
      int k = 4;
      MatrixNd V = createRandomMatrix (m, n);

      double prevRes = Double.POSITIVE_INFINITY;
      for (int maxi=1; maxi<=15; maxi++) {
         NMFactorization nmf = new NMFactorization();
         // NNDSVD is deterministic, so each run starts from the same place
         nmf.setInitialization (Init.NNDSVD);
         nmf.setMaxIterations (maxi);
         nmf.setTolerance (0); // don't stop early
         nmf.factor (V, k);
         double res = nmf.getResidual();
         if (res > prevRes + 1e-9) {
            throw new TestException (
               "monotonicity: residual increased from "+prevRes+" to "+res+
               " at iteration "+maxi);
         }
         prevRes = res;
      }
   }

   /**
    * Tests the multiplicative update. This converges more slowly than ANLS, so
    * the tolerances are looser.
    */
   public void testMultiplicativeUpdate() {
      int m = 10;
      int n = 24;
      int k = 3;

      MatrixNd Wchk = createSeparableMatrix (m, k);
      MatrixNd Hchk = createSeparableMatrix (n, k);
      Hchk.transpose();
      MatrixNd V = new MatrixNd (m, n);
      V.mul (Wchk, Hchk);

      NMFactorization nmf = new NMFactorization();
      nmf.setUpdate (Update.MULTIPLICATIVE);
      nmf.setMaxIterations (5000);
      nmf.setTolerance (1e-14);
      nmf.factor (V, k);

      checkEquals ("multiplicative, update", nmf.getUpdate(), Update.MULTIPLICATIVE);
      checkNonNegative ("multiplicative, W", nmf.getW());
      checkNonNegative ("multiplicative, H", nmf.getH());
      double res = computeResidual (V, nmf.getW(), nmf.getH());
      checkEquals ("multiplicative, residual", nmf.getResidual(), res, 1e-8);
      check ("multiplicative, residual "+res+" > 1e-3", res < 1e-3);

      // multiplicative updates are also monotone
      double prevRes = Double.POSITIVE_INFINITY;
      MatrixNd Vrand = createRandomMatrix (12, 20);
      for (int maxi=1; maxi<=15; maxi++) {
         NMFactorization nmfi = new NMFactorization();
         nmfi.setUpdate (Update.MULTIPLICATIVE);
         nmfi.setMaxIterations (maxi);
         nmfi.setTolerance (0);
         nmfi.factor (Vrand, 3);
         double resi = nmfi.getResidual();
         if (resi > prevRes + 1e-9) {
            throw new TestException (
               "multiplicative monotonicity: residual increased from "+
               prevRes+" to "+resi+" at iteration "+maxi);
         }
         prevRes = resi;
      }
   }

   /**
    * Tests that regularization is applied and does not break non-negativity.
    */
   public void testRegularization() {
      int m = 12;
      int n = 20;
      int k = 3;
      MatrixNd V = createRandomMatrix (m, n);

      NMFactorization nmf = new NMFactorization();
      nmf.factor (V, k);
      double res0 = nmf.getResidual();

      nmf.setRegularization (0.1);
      checkEquals ("regularization", nmf.getRegularization(), 0.1, 0);
      nmf.factor (V, k);
      checkNonNegative ("regularized W", nmf.getW());
      checkNonNegative ("regularized H", nmf.getH());
      double res1 = computeResidual (V, nmf.getW(), nmf.getH());
      checkEquals ("regularized residual", nmf.getResidual(), res1, 1e-8);
      // regularization trades residual for smaller factors
      check ("regularized residual "+res1+" < unregularized "+res0,
             res1 >= res0 - 1e-10);

      try {
         nmf.setRegularization (-1);
         throw new TestException (
            "negative regularization did not throw an exception");
      }
      catch (IllegalArgumentException e) {
         // expected
      }
   }

   /**
    * Tests factor ordering and normalization. None of these may change the
    * product W H.
    */
   public void testOrderingAndNormalization() {
      int m = 12;
      int n = 20;
      int k = 4;
      MatrixNd V = createRandomMatrix (m, n);

      NMFactorization nmf = new NMFactorization();
      nmf.setNormalization (Normalization.NONE);
      nmf.factor (V, k);
      MatrixNd Pchk = nmf.getReconstruction();
      double resChk = nmf.getResidual();

      VectorNd w = new VectorNd (m);
      VectorNd h = new VectorNd (n);

      // factors must be ordered by decreasing contribution
      double prevMag = Double.POSITIVE_INFINITY;
      for (int j=0; j<k; j++) {
         nmf.getW().getColumn (j, w);
         nmf.getH().getRow (j, h);
         double mag = w.norm()*h.norm();
         check ("ordering: factor "+j+" magnitude "+mag+" > "+prevMag,
                mag <= prevMag + 1e-12);
         prevMag = mag;
      }

      for (Normalization nrm :
              new Normalization[] {
                 Normalization.UNITY_MAX, Normalization.UNITY_NORM }) {
         nmf.setNormalization (nrm);
         checkEquals ("normalization", nmf.getNormalization(), nrm);
         nmf.factor (V, k);
         for (int j=0; j<k; j++) {
            nmf.getW().getColumn (j, w);
            double val =
               (nrm == Normalization.UNITY_MAX ? w.maxElement() : w.norm());
            if (val != 0) {
               checkEquals (nrm+": column "+j+" scale", val, 1.0, 1e-12);
            }
         }
         // normalization must not change the product W H
         checkEquals (nrm+": reconstruction", nmf.getReconstruction(), Pchk,
                      1e-10);
         checkEquals (nrm+": residual", nmf.getResidual(), resChk, 1e-12);
      }
   }

   /**
    * Tests random initialization and restarts.
    */
   public void testInitializationAndRestarts() {
      int m = 14;
      int n = 25;
      int k = 3;
      MatrixNd V = createRandomMatrix (m, n);

      NMFactorization nmf = new NMFactorization();
      nmf.setInitialization (Init.RANDOM);
      checkEquals ("initialization", nmf.getInitialization(), Init.RANDOM);
      nmf.setRandomGenerator (new Random (0x1234));
      nmf.factor (V, k);
      double res0 = nmf.getResidual();
      checkNonNegative ("random init, W", nmf.getW());
      checkNonNegative ("random init, H", nmf.getH());
      checkEquals ("random init, residual",
                   res0, computeResidual (V, nmf.getW(), nmf.getH()), 1e-8);

      // same seed should give the same result
      nmf.setRandomGenerator (new Random (0x1234));
      nmf.factor (V, k);
      checkEquals ("random init repeatability", nmf.getResidual(), res0, 1e-14);

      // restarts keep the best result, so the residual can only improve
      nmf.setNumRestarts (5);
      checkEquals ("num restarts", nmf.getNumRestarts(), 5);
      nmf.setRandomGenerator (new Random (0x1234));
      nmf.factor (V, k);
      check ("restarts: residual "+nmf.getResidual()+" > single "+res0,
             nmf.getResidual() <= res0 + 1e-12);

      try {
         nmf.setNumRestarts (0);
         throw new TestException ("zero restarts did not throw an exception");
      }
      catch (IllegalArgumentException e) {
         // expected
      }
   }

   /**
    * Tests the VAF sweep used to select the number of factors.
    */
   public void testVAFs() {
      int m = 10;
      int n = 30;
      int k = 3;

      // V is exactly generated by k factors, so the VAF should reach 1 there
      MatrixNd Wchk = createSeparableMatrix (m, k);
      MatrixNd Hchk = createSeparableMatrix (n, k);
      Hchk.transpose();
      MatrixNd V = new MatrixNd (m, n);
      V.mul (Wchk, Hchk);

      NMFactorization nmf = new NMFactorization();
      nmf.setMaxIterations (2000);
      double[] vafs = nmf.computeVAFs (V, 1, 5);
      checkEquals ("VAFs length", vafs.length, 5);
      for (int j=0; j<vafs.length; j++) {
         check ("VAF["+j+"] = "+vafs[j]+" out of range",
                vafs[j] >= 0 && vafs[j] <= 1.0+1e-12);
      }
      check ("VAF for k=1 = "+vafs[0]+" >= 1", vafs[0] < 1.0);
      for (int j=k-1; j<vafs.length; j++) {
         check ("VAF["+j+"] = "+vafs[j]+" < 1 at or above true rank",
                vafs[j] > 1.0-1e-8);
      }

      // a sweep starting at minFactors must match the corresponding
      // subrange of the full sweep
      int minFactors = 2;
      double[] subVafs = nmf.computeVAFs (V, minFactors, 5);
      checkEquals ("VAFs length for minFactors="+minFactors,
                   subVafs.length, 5-minFactors+1);
      for (int j=0; j<subVafs.length; j++) {
         checkEquals ("VAF for k="+(j+minFactors),
                      subVafs[j], vafs[j+minFactors-1], 1e-8);
      }

      // minFactors == maxFactors gives a single value
      double[] oneVaf = nmf.computeVAFs (V, 3, 3);
      checkEquals ("VAFs length for minFactors == maxFactors",
                   oneVaf.length, 1);
      checkEquals ("VAF for k=3", oneVaf[0], vafs[2], 1e-8);

      // improper minFactors
      int[][] badRanges = new int[][] { {0, 5}, {-1, 5}, {4, 3} };
      for (int[] range : badRanges) {
         try {
            nmf.computeVAFs (V, range[0], range[1]);
            throw new TestException (
               "minFactors="+range[0]+", maxFactors="+range[1]+
               " did not throw an exception");
         }
         catch (IllegalArgumentException e) {
            // expected
         }
      }
   }

   /**
    * Tests error handling for improper arguments and state.
    */
   public void testErrorHandling() {
      MatrixNd V = createRandomMatrix (8, 12);
      NMFactorization nmf = new NMFactorization();

      try {
         nmf.getW();
         throw new TestException (
            "getW() before factoring did not throw an exception");
      }
      catch (ImproperStateException e) {
         // expected
      }

      int[] badFactors = new int[] { 0, -1, 9, 100 };
      for (int k : badFactors) {
         try {
            nmf.factor (V, k);
            throw new TestException (
               "numFactors="+k+" did not throw an exception");
         }
         catch (IllegalArgumentException e) {
            // expected
         }
      }

      MatrixNd Vneg = new MatrixNd (V);
      Vneg.set (3, 5, -1e-14);
      try {
         nmf.factor (Vneg, 2);
         throw new TestException (
            "negative matrix entry did not throw an exception");
      }
      catch (IllegalArgumentException e) {
         // expected
      }

      MatrixNd Vnan = new MatrixNd (V);
      Vnan.set (3, 5, Double.NaN);
      try {
         nmf.factor (Vnan, 2);
         throw new TestException ("NaN entry did not throw an exception");
      }
      catch (IllegalArgumentException e) {
         // expected
      }

      // a zero matrix is degenerate but should still factor without error
      MatrixNd Vzero = new MatrixNd (8, 12);
      nmf.factor (Vzero, 2);
      checkNonNegative ("zero matrix, W", nmf.getW());
      checkNonNegative ("zero matrix, H", nmf.getH());
   }

   public void test() {
      testErrorHandling();
      testExactRecovery();
      testGeneralFactorization();
      testMonotonicity();
      testMultiplicativeUpdate();
      testRegularization();
      testOrderingAndNormalization();
      testInitializationAndRestarts();
      testVAFs();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      NMFactorizationTest tester = new NMFactorizationTest();
      tester.runtest();
   }
}
