/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.solvers.IterativeSolver.ToleranceType;

import maspack.matrix.ImproperSizeException;
import maspack.matrix.LinearTransformNd;
import maspack.matrix.Matrix;
import maspack.matrix.SparseMatrixCRS;
import maspack.matrix.VectorNd;

/**
 * Solves linear systems using the conjugate gradient algorithm
 */
public class ProjectedCGSolver { 
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
    * Solves a linear system A x = b using the projected conjugate gradient
    * method. The matrix associated with the linear system is represented
    * implicitly by a {@link maspack.matrix.LinearTransformNd
    * LinearTransformNd}. The method will iterate while relative residual ||A x
    * - b||/||b|| is greater than a supplied tolerance and the number of
    * iterations is less than a specified maximum.
    * 
    * @param x
    * result vector, as well as initial guess of the solution
    * @param A
    * linear transform for the system to be solved
    * @param G
    * constraint matrix
    * @param b
    * input vector
    * @param g
    * input vector for G
    * @return true if a solution was found within the specified tolerance
    */
   public boolean solve (
      VectorNd x, VectorNd lam, LinearTransformNd A, SparseMatrixCRS G,
      VectorNd b, VectorNd g) {
      return solve (x, lam, A, G, b, g, myTol, myMaxIter, null);
   }

   /**
    * Solves a linear system A x = b using the projected conjugate gradient
    * method. The matrix associated with the linear system is represented
    * implicitly by a {@link maspack.matrix.LinearTransformNd
    * LinearTransformNd}. The method will iterate while relative residual ||A x
    * - b||/||b|| is greater than a supplied tolerance and the number of
    * iterations is less than a specified maximum.
    * 
    * @param x
    * result vector, as well as initial guess of the solution
    * @param lam
    * result lambda, as well as initial guess of the solution
    * @param A
    * linear transform for the system to be solved
    * @param G
    * constraint matrix
    * @param b
    * input vector
    * @param g
    * input vector for G
    * @param tol
    * solution tolerance
    * @param maxIter
    * maximum number of iferations
    * @return true if a solution was found within the specified tolerance
    */
   public boolean solve (
      VectorNd x, VectorNd lam, LinearTransformNd A, SparseMatrixCRS G,
      VectorNd b, VectorNd g, double tol, int maxIter) {
      return solve (x, lam, A, G, b, g, tol, maxIter, null);
   }

   /**
    * Solves a linear system A x = b using the projected conjugate gradient
    * method with a preconditioner. The matrix associated with the linear
    * system and the preconditioner are each represented implicitly by a {@link
    * maspack.matrix.LinearTransformNd LinearTransformNd}. The {@link
    * maspack.matrix.LinearTransformNd#mul mul} method of the preconditioner
    * should implement the transformation y = inv(M) x, where M is a a
    * preconditioning matrix that approximates A. The method will iterate while
    * relative residual ||A x - b||/||b|| is greater than a supplied tolerance
    * and the number of iterations is less than a specified maximum.
    * 
    * @param x
    * result vector, as well as initial guess of the solution
    * @param lam
    * result lambda, as well as initial guess of the solution
    * @param A
    * linear transform for the system to be solved
    * @param G
    * constraint matrix
    * @param b
    * input vector
    * @param g
    * input vector for G
    * @param tol
    * solution tolerance
    * @param maxIter
    * maximum number of iferations
    * @param P
    * preconditioner (optional, may be specified as null)
    * @return true if a solution was found within the specified tolerance
    */
   public boolean solve (
      VectorNd x, VectorNd lam, LinearTransformNd A, SparseMatrixCRS G,
      VectorNd b, VectorNd g, double tol, int maxIter, LinearTransformNd P) {

      // System.out.println ("CGSolve: " + tol+" "+maxIter+" "+myTolType);
      if (A.rowSize() != A.colSize()) {
         throw new ImproperSizeException ("Matrix must be square");
      }
      if (b.size() != A.rowSize() || x.size() != b.size()) {
         throw new ImproperSizeException ("Inconsistent argument sizes");
      }
      double dnew;

      double[] Gdot = new double[G.rowSize()];
      for (int i=0; i<G.rowSize(); i++) {
         Gdot[i] = G.dotRowSelf (i);
      }
      lam.setZero();

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
      projectConstraints (res, null, G, g, Gdot);
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
         projectConstraints (A_dir, lam, G, g, Gdot);
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
         projectConstraints (res, null, G, g, Gdot);
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

   private void projectConstraints (
      VectorNd v, VectorNd lam, SparseMatrixCRS G, VectorNd b, double[] Gdot) {
      double[] bbuf = b.getBuffer();
      double[] lambuf = lam != null ? lam.getBuffer() : null;
      int icnt = 1000;

      for (int k=0; k<icnt; k++) {
         
         for (int i=0; i<G.rowSize(); i++) {
            double l = (bbuf[i] - G.dotRow (i, v))/Gdot[i];
            G.addScaledRow (v, l, i);
            if (lam != null) {
               lambuf[i] += l;
            }
            
         }
         for (int i=G.rowSize()-2; i>=0; i--) {
            double l = (bbuf[i] - G.dotRow (i, v))/Gdot[i];
            G.addScaledRow (v, l, i);
            if (lam != null) {
               lambuf[i] += l;
            }
         }
      }
      
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

}
