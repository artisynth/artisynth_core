/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.LinearTransformNd;
import maspack.matrix.VectorNd;

public class CRSolver implements IterativeSolver {
   public int getMaxIterations() {
      return maxIterations;
   }

   public int getNumIterations() {
      return k;
   }

   public double getRelativeResidual() {
      return phi;
   }

   public double getTolerance() {
      return tolerance;
   }

   public ToleranceType getToleranceType() {
      // TODO Auto-generated method stub
      return null;
   }

   public boolean isCompatible (int matrixType) {
      // TODO Auto-generated method stub
      return false;
   }

   int maxIterations = 100;

   public void setMaxIterations (int max) {
      maxIterations = max;
   }

   double tolerance = 1e-20;

   public void setTolerance (double tol) {
      tolerance = tol;
   }

   public void setToleranceType (ToleranceType type) {
      // TODO Auto-generated method stub

   }

   double phi;
   int k;

   public boolean solve (
      VectorNd x, LinearTransformNd A, VectorNd b, double tol, int maxIter,
      LinearTransformNd P) {
      int n = A.colSize();

      VectorNd xkp = new VectorNd (n);
      VectorNd pkp = new VectorNd (n);
      VectorNd rkp = new VectorNd (n);
      VectorNd wkp = new VectorNd (n);
      VectorNd zkp = new VectorNd (n);

      VectorNd xk = new VectorNd (n);
      VectorNd pk = new VectorNd (n);
      VectorNd rk = new VectorNd (n);
      VectorNd wk = new VectorNd (n);
      VectorNd zk = new VectorNd (n);

      xkp.setZero();
      pkp.set (b);
      rkp.set (b);
      A.mul (zkp, rkp);
      A.mul (wkp, pkp);

      double phikp = b.norm();
      double mukp = rkp.dot (zkp);

      double phik = phikp;
      double muk = mukp;

      k = 1;

      while (k < maxIter && phik > tol) {
         // System.out.println("cr iteration " + k + " " + maxIter + " " +
         // phik);

         double alphak = mukp / wkp.normSquared();
         xk.scaledAdd (alphak, pkp, xkp);
         rk.scaledAdd (-alphak, wkp, rk);

         phik = rk.norm();

         A.mul (zk, rk);
         muk = rk.dot (zk);

         double betak = muk / mukp;
         pk.scaledAdd (betak, pkp, rk);
         wk.scaledAdd (betak, wkp, zk);

         k++;

         xkp.set (xk);
         pkp.set (pk);
         rkp.set (rk);
         zkp.set (zk);
         wkp.set (wk);

         phikp = phik;
         mukp = muk;
      }

      x.set (xk);

      phi = phik;

      return k == maxIter;
   }

   public boolean solve (
      VectorNd x, LinearTransformNd A, VectorNd b, double tol, int maxIter) {
      return solve (x, A, b, tol, maxIter, null);
   }

   public boolean solve (VectorNd x, LinearTransformNd A, VectorNd b) {
      return solve (x, A, b, tolerance, maxIterations, null);
   }
}
