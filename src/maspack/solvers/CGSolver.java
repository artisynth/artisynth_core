/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.ImproperSizeException;
import maspack.matrix.LinearTransformNd;
import maspack.matrix.Matrix;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.VectorNd;

/**
 * Solves linear systems using the conjugate gradient algorithm
 */
public class CGSolver implements IterativeSolver {
   private int myLastIterationCnt;
   private double myLastResidualSquared;
   private double myTol = 0.001;
   private int myMaxIter = 100;
   private ToleranceType myTolType = ToleranceType.RelativeResidual;

   public boolean debug = false;

   VectorNd res = new VectorNd (0); // residual
   VectorNd P_res = new VectorNd (0); // res with preconditioner applied
   VectorNd dir = new VectorNd (0); // direction
   VectorNd A_dir = new VectorNd (0); // direction multiplied by A

   // public enum ToleranceType
   // {
   // RelativeResidual,
   // AbsoluteResidual,
   // AbsoluteError
   // };

   public double getTolerance() {
      return myTol;
   }

   public void setTolerance (double tol) {
      myTol = tol;
   }

   public ToleranceType getToleranceType() {
      return myTolType;
   }

   public void setToleranceType (ToleranceType type) {
      myTolType = type;
   }

   public int getMaxIterations() {
      return myMaxIter;
   }

   public void setMaxIterations (int max) {
      myMaxIter = max;
   }

   /**
    * Solves a linear system A x = b using the conjugate gradient method. The
    * matrix associated with the linear system is represented implicitly by a
    * {@link maspack.matrix.LinearTransformNd LinearTransformNd}. The method
    * will iterate while relative residual ||A x - b||/||b|| is greater than a
    * supplied tolerance and the number of iterations is less than a specified
    * maximum.
    * 
    * @param x
    * result vector, as well as initial guess of the solution
    * @param A
    * linear transform for the system to be solved
    * @param b
    * input vector
    * @return true if a solution was found within the specified tolerance
    */
   public boolean solve (VectorNd x, LinearTransformNd A, VectorNd b) {
      return solve (x, A, b, myTol, myMaxIter, null);
   }

   /**
    * Solves a linear system A x = b using the conjugate gradient method. The
    * matrix associated with the linear system is represented implicitly by a
    * {@link maspack.matrix.LinearTransformNd LinearTransformNd}. The method
    * will iterate while relative residual ||A x - b||/||b|| is greater than a
    * supplied tolerance and the number of iterations is less than a specified
    * maximum.
    * 
    * @param x
    * result vector, as well as initial guess of the solution
    * @param A
    * linear transform for the system to be solved
    * @param b
    * input vector
    * @param tol
    * solution tolerance
    * @param maxIter
    * maximum number of iferations
    * @return true if a solution was found within the specified tolerance
    */
   public boolean solve (
      VectorNd x, LinearTransformNd A, VectorNd b, double tol, int maxIter) {
      return solve (x, A, b, tol, maxIter, null);
   }

   /**
    * Solves a linear system A x = b using the conjugate gradient method with a
    * preconditioner. The matrix associated with the linear system and the
    * preconditioner are each represented implicitly by a {@link
    * maspack.matrix.LinearTransformNd LinearTransformNd}. The {@link
    * maspack.matrix.LinearTransformNd#mul mul} method of the preconditioner
    * should implement the transformation y = inv(M) x, where M is a a
    * preconditioning matrix that approximates A. The method will iterate while
    * relative residual ||A x - b||/||b|| is greater than a supplied tolerance
    * and the number of iterations is less than a specified maximum.
    * 
    * @param x
    * result vector, as well as initial guess of the solution
    * @param A
    * linear transform for the system to be solved
    * @param b
    * input vector
    * @param tol
    * solution tolerance
    * @param maxIter
    * maximum number of iferations
    * @param P
    * preconditioner (optional, may be specified as null)
    * @return true if a solution was found within the specified tolerance
    */
   public boolean solve (
      VectorNd x, LinearTransformNd A, VectorNd b, double tol, int maxIter,
      LinearTransformNd P) {

      // System.out.println ("CGSolve: " + tol+" "+maxIter+" "+myTolType);
      if (A.rowSize() != A.colSize()) {
         throw new ImproperSizeException ("Matrix must be square");
      }
      if (b.size() != A.rowSize() || x.size() != b.size()) {
         throw new ImproperSizeException ("Inconsistent argument sizes");
      }
      double dnew;

      int xsize = x.size();

      if (xsize != res.size()) {
         res.setSize (xsize);
         dir.setSize (xsize);
         A_dir.setSize (xsize);
      }
      if (P != null && xsize != P_res.size()) {
         P_res.setSize (xsize);
      }

      A.mul (res, x);
      res.sub (b, res);
      if (P != null) {
         P.mul (dir, res);
      }
      else {
         dir.set (res);
      }

      dnew = res.dot (dir);
      double resLimit;
      switch (myTolType) {
         case RelativeResidual: {
            resLimit = tol * tol * b.dot (b);
            break;
         }
         case AbsoluteResidual:
         case AbsoluteError: {
            resLimit = tol * tol;
            break;
         }
         default: {
            throw new IllegalStateException (
               "Unhandled tolerance type " + myTolType);
         }
      }

      int cnt = 0;
      if (debug) {
         System.out.println ("limit=" + resLimit);
      }
      while (cnt < maxIter) {
         if (myTolType != ToleranceType.AbsoluteError && dnew <= resLimit) {
            break;
         }
         A.mul (A_dir, dir);
         if (debug) {
            System.out.println ("  " + cnt + " " + dnew);
         }
         double alpha = dnew / dir.dot (A_dir);
         x.scaledAdd (alpha, dir, x);
         if (myTolType == ToleranceType.AbsoluteError) {
            if (alpha * alpha * dir.normSquared() <= resLimit) {
               break;
            }
         }
         if (cnt > 0 && ((cnt % 100) == 0)) {
            A.mul (res, x);
            res.sub (b, res);
         }
         else {
            res.scaledAdd (-alpha, A_dir, res);
         }
         double dold = dnew;
         if (P != null) {
            P.mul (P_res, res);
            dnew = res.dot (P_res);
            dir.scaledAdd (dnew / dold, dir, P_res);
         }
         else {
            dnew = res.dot (res);
            dir.scaledAdd (dnew / dold, dir, res);
         }
         cnt++;
      }
      myLastIterationCnt = cnt + 1;
      myLastResidualSquared = dnew;
      return cnt < maxIter;
   }

   /**
    * Returns the number of iterations associated with the last call to
    * {@link #solve solve}.
    * 
    * @return number of iterations
    */
   public int getNumIterations() {
      return myLastIterationCnt;
   }

   /**
    * Returns the relative residual ||A x - b||/||b|| at the end of the last
    * call to {@link #solve solve}.
    * 
    * @return last relative residual
    */
   public double getRelativeResidual() {
      return Math.sqrt (myLastResidualSquared);
   }

   public boolean isCompatible (int matrixType) {
      return ((matrixType & Matrix.SYMMETRIC) != 0);
   }

   public boolean solveTransformed (
      VectorNd x, SparseMatrixNd A, VectorNd b, double tol, int maxIter,
      IncompleteCholeskyDecomposition icd) {
      // icd.factor(A);
      // icd.L.setIdentity();

      int n = x.size();

      VectorNd d = new VectorNd (n);
      VectorNd r = new VectorNd (n);
      VectorNd dnew = new VectorNd (n);
      VectorNd rnew = new VectorNd (n);
      VectorNd xnew = new VectorNd (n);
      VectorNd EAEv = new VectorNd (n);

      // x.setZero();

      icd.solveTranspose (EAEv, x);
      A.mul (EAEv, EAEv);
      icd.solve (EAEv, EAEv);

      icd.solve (d, b);
      d.sub (EAEv);

      // d.set(b);

      r.set (d);

      double rnorm2 = r.normSquared();
      double rnorm2tol = Math.sqrt (r.norm()) * tol;

      int i = 0;
      while (i < maxIter) {
         rnorm2 = r.normSquared();

         if (Math.sqrt (rnorm2) < rnorm2tol) {
            System.out.println ("error tolerance reached at " + i);
            break;
         }

         icd.solveTranspose (EAEv, d);
         A.mul (EAEv, EAEv);
         icd.solve (EAEv, EAEv);
         // A.mul(tmp, d);

         double alpha = rnorm2 / d.dot (EAEv);
         xnew.scaledAdd (alpha, d, x);
         rnew.scaledAdd (-alpha, EAEv, r);

         double beta = rnew.normSquared() / rnorm2;
         dnew.scaledAdd (beta, d, rnew);

         x.set (xnew);
         r.set (rnew);
         d.set (dnew);

         // System.out.println(rnorm2);

         i++;
      }

      myLastIterationCnt = i;
      myLastResidualSquared = rnorm2;

      icd.solveTranspose (x, x);

      return true;
   }
}
