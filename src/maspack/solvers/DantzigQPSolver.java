/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;

/**
 * A dense QP (Quadratic Program) solver that that uses Dantzig's algorithm.
 */
public class DantzigQPSolver {

   protected CholeskyDecomposition myCholD;
   protected LUDecomposition myLUD;
   protected DantzigLCPSolver myLcp;
   protected MatrixNd myM;
   protected MatrixNd myY;
   protected VectorNd myq;
   protected VectorNd myy;
   protected VectorNd myz;

   /**
    * Described whether or not a solution was found. Where appropriate,
    * these are taken directly from DantizLCPSolver.Status.
    */
   public enum Status {
      /**
       * A solution was found.
       */
      SOLVED,

      /**
       * System was completely constrained by equality constraints;
       * only equality constraints were solved for.
       */
      SOLVED_EQUALITIES_ONLY,

      /**
       * No solution appears to be possible.
       */
      NO_SOLUTION,

      /**
       * Iteration limit was exceeded, most likely due to numerical
       * ill-conditioning.
       */
      ITERATION_LIMIT_EXCEEDED,
      /**
       * A numeric error was detected in the solution.
       */
      NUMERIC_ERROR,

      /**
       * H is not symmetric positive definite.
       */
      NOT_SPD,

      /**
       * H, or H combined with equality constraints, is singular
       * within working precision.
       */
      SINGULAR_SYSTEM
   }

   private Status statusFromLCP (DantzigLCPSolver.Status dstatus) {
      switch (dstatus) {
         case SOLVED: {
            return Status.SOLVED;
         }
         case NO_SOLUTION: {
            return Status.NO_SOLUTION;
         }
         case ITERATION_LIMIT_EXCEEDED: {
            return Status.ITERATION_LIMIT_EXCEEDED;
         }
         case NUMERIC_ERROR: {
            return Status.NUMERIC_ERROR;
         }
         default: {
            throw new InternalErrorException (
               "Unknown DantzigLCPSolver status " + dstatus);
         }
      }
   }

   public DantzigQPSolver() {
      myLcp = new DantzigLCPSolver();
      myM = new MatrixNd();
      myY = new MatrixNd();
      myq = new VectorNd();
      myy = new VectorNd();
      myz = new VectorNd();
   }

   private void checkProblemDimensions (
      MatrixNd H, VectorNd f, MatrixNd A, VectorNd b) {

      if (H.rowSize() != H.colSize()) {
         throw new IllegalArgumentException ("H must be square");
      }
      int hsize = H.rowSize();
      if (A.colSize() != hsize) {
         throw new IllegalArgumentException (
            "A column size "+A.colSize()+" does not equal H size "+hsize);
      }
      if (f.size() != hsize) {
         throw new IllegalArgumentException (
            "f size "+f.size()+" does not equal H size "+hsize);
      }
      if (A.rowSize() != b.size()) {
         throw new IllegalArgumentException (
            "A row size "+A.rowSize()+" does not equal b size "+b.size());
      }
   }

   /**
    * Solves a convex quadratic program with inequality constraints:
    * <pre>
    * min 1/2 x^T H x + f^T x,  A x >= b
    * </pre>
    * using Dantzig's LCP pivoting algorithm.
    *
    * @param x computed minimum value
    * @param H quadratic matrix term. Must be symmetric positive definite
    * @param f linear term
    * @param A inequality constraint matrix
    * @param b inequality constraint offsets
    * @return status value.
    */
   public Status solve (
      VectorNd x, MatrixNd H, VectorNd f, MatrixNd A, VectorNd b) {

      checkProblemDimensions (H, f, A, b);
      if (myCholD == null) {
         myCholD = new CholeskyDecomposition();
      }

      MatrixNd AT = new MatrixNd (A.colSize(), A.rowSize());

      myM.setSize (A.rowSize(), A.rowSize());
      myq.setSize (A.rowSize());
      x.setSize (H.rowSize());

      AT.transpose (A);
      try {
         myCholD.factor (H);
      }
      catch (Exception e) {
         return Status.NOT_SPD;
      }
      if (A.rowSize() == 0) {
         if (!myCholD.solve (x, f)) {
            return Status.SINGULAR_SYSTEM;
         }
         x.negate();
         return Status.SOLVED;
      }
      if (!myCholD.solve (myY, AT)) {
         return Status.SINGULAR_SYSTEM;
      }
      myM.mul (A, myY);

      if (!myCholD.solve (myy, f)) {
         return Status.SINGULAR_SYSTEM;
      }
      A.mul (myq, myy);
      myq.add (b);
      myq.negate();

      int n = myq.size();
      myz.setSize (n);
      boolean[] zBasic = new boolean[n];
      Status status = statusFromLCP(myLcp.solve (myz, myM, myq, zBasic));
      if (status == Status.SOLVED) {
         A.mulTranspose (myy, myz);
         myy.sub (f);
         if (! myCholD.solve (x, myy)) {
            return Status.SINGULAR_SYSTEM;
         }
      }
      return status;
   }

   private void setAT (MatrixNd AT, MatrixNd A, int neq) {
      int m = A.colSize()+neq;
      int n = A.rowSize();
      for (int i=0; i<A.colSize(); i++) {
         for (int j=0; j<n; j++) {
            AT.set (i, j, A.get (j, i));
         }
      }
      for (int i=A.colSize(); i<m; i++) {
         for (int j=0; j<n; j++) {
            AT.set (i, j, 0);
         }
      }
   }

   private void setHAeq (MatrixNd HAeq, MatrixNd Q, MatrixNd G) {

      int qsize = Q.rowSize();
      int neq = G.rowSize();
      for (int i=0; i<qsize; i++) {
         for (int j=i; j<qsize; j++) {
            double val = Q.get(i, j);
            HAeq.set (i, j, val);
            if (j > i) {
               HAeq.set (j, i, val);
            }
         }
      }
      for (int i=0; i<neq; i++) {
         for (int j=0; j<qsize; j++) {
            double val = G.get(i, j);
            HAeq.set (qsize+i, j, -val);
            HAeq.set (j, qsize+i, val);
         }
      }
   }

   protected void setCg (VectorNd cg, VectorNd c, VectorNd g) {

      int qsize = c.size();
      for (int i=0; i<qsize; i++) {
         cg.set (i, c.get(i));
      }
      for (int i=0; i<g.size(); i++) {
         cg.set (qsize+i, g.get(i));
      }
   }

   /**
    * Solves a convex quadratic program with both equality and inequality
    * constraints:
    * <pre> min 1/2 x^T H x + f^T x, A x >= b, Aeq x = beq
    * </pre>
    * using Dantzig's LCP pivoting algorithm.
    *
    * @param x computed minimum value
    * @param H quadratic matrix term. Must be symmetric positive definite
    * @param f linear term
    * @param A inequality constraint matrix
    * @param b inequality constraint offsets
    * @param Aeq equality constraint matrix
    * @param beq equality constraint offsets
    * @return status value.
    */
   public Status solve (
      VectorNd x, MatrixNd H, VectorNd f, 
      MatrixNd A, VectorNd b, MatrixNd Aeq, VectorNd beq) {

      if (Aeq.rowSize() == 0) {
         // just solve the inequality problem
         return solve (x, H, f, A, b);
      }
      checkProblemDimensions (H, f, A, b);
      if (Aeq.rowSize() > H.rowSize()) {
         throw new IllegalArgumentException (
            "Number of equality constraints " + Aeq.rowSize() + 
            " exceeds problem size " + H.rowSize());
      }
      if (Aeq.colSize() != H.rowSize()) {
         throw new IllegalArgumentException (
            "Aeq column size " + Aeq.colSize() + 
            " does not equal H size " + H.rowSize());
      }
      if (Aeq.rowSize() != beq.size()) {
         throw new IllegalArgumentException (
            "Aeq row size " + Aeq.rowSize() + 
            " does not equal beq size " + beq.size());
      }
      if (myLUD == null) {
         myLUD = new LUDecomposition();
      }
      int hsize = H.rowSize();
      int neq = Aeq.rowSize();
      x.setSize (hsize);
      if (neq == hsize) {
         // just solve the equality constraints
         myLUD.factor (Aeq);
         if (!myLUD.solve (x, beq)) {
            return Status.SINGULAR_SYSTEM;
         }
         else {
            return Status.SOLVED_EQUALITIES_ONLY;              
         }
      }
      MatrixNd HAeq = new MatrixNd (hsize+neq, hsize+neq);
      MatrixNd AT = new MatrixNd (A.colSize()+neq, A.rowSize());
      VectorNd cg = new VectorNd (hsize+neq);
      VectorNd xlam = new VectorNd (hsize+neq);

      myM.setSize (A.rowSize(), A.rowSize());
      myq.setSize (A.rowSize());
      x.setSize (H.rowSize());

      setAT (AT, A, neq);
      setHAeq (HAeq, H, Aeq);
      myLUD.factor (HAeq);
      if (A.rowSize() == 0) {
         myy.negate (f);
         myy.setSize (hsize+neq);
         for (int i=0; i<neq; i++) {
            myy.set (hsize+i, -beq.get(i));
         }
         if (!myLUD.solve (xlam, myy)) {
            return Status.SINGULAR_SYSTEM;
         }
         else {
            xlam.getSubVector (0, x);
            return Status.SOLVED;
         }
      }

      if (!myLUD.solve (myY, AT)) {
         return Status.SINGULAR_SYSTEM;
      }
      myM.mulTransposeLeft (AT, myY);

      setCg (cg, f, beq);
      if (!myLUD.solve (myy, cg)) {
         return Status.SINGULAR_SYSTEM;
      }
      AT.mulTranspose (myq, myy);
      myq.add (b);
      myq.negate();

      int n = myq.size();
      myz.setSize (n);
      boolean[] zBasic = new boolean[n];
      Status status = statusFromLCP(myLcp.solve (myz, myM, myq, zBasic));
      if (status == Status.SOLVED) {
         A.mulTranspose (myy, myz);
         myy.setSize (hsize+neq);
         for (int i=0; i<neq; i++) {
            myy.set (hsize+i, 0);
         }
         myy.sub (cg);
         if (!myLUD.solve (xlam, myy)) {
            return Status.SINGULAR_SYSTEM;
         }
         xlam.getSubVector (0, x);
      }
      return status;
   }


}
