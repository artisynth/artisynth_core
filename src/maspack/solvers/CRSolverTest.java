/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.LUDecomposition;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.*;

import java.util.Random;
import java.io.*;

/**
 * Unit test for the class CGSolver
 */
public class CRSolverTest {
   Random randGen = new Random();

   CRSolverTest() {
      randGen.setSeed (0x1234);
   }

   public SparseMatrixNd createBlockDiagonal (int nblocks) {
      SparseMatrixNd M = new SparseMatrixNd (5 * nblocks, 5 * nblocks);
      MatrixNd block = new MatrixNd (5, 5);
      for (int k = 0; k < nblocks; k++) {
         block.setRandom (-0.5, 0.5, randGen);
         block.mulTranspose (block);
         for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
               M.set (5 * k + i, 5 * k + j, block.get (i, j));
            }
         }
      }
      return M;
   }

   public SparseMatrixNd createConstraintProblem (int nbodies) {
      SparseMatrixNd M = new SparseMatrixNd (5 * nbodies, 5 * nbodies);
      Matrix3d mass = new Matrix3d();
      for (int k = 0; k < nbodies; k++) {
         mass.setRandom (-0.5, 0.5, randGen);
         mass.mulTranspose (mass);
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               M.set (3 * k + i, 3 * k + j, mass.get (i, j));
            }
         }
      }
      int[] connectionCount = new int[nbodies * nbodies];

      Vector3d n = new Vector3d();
      for (int k = 0; k < 2 * nbodies; k++) {
         // find a random connection between two bodies
         int bod1, bod2;
         do {
            bod1 = randGen.nextInt (nbodies);
            bod2 = randGen.nextInt (nbodies);
            if (bod1 < bod2) {
               int l = bod1;
               bod1 = bod2;
               bod2 = l;
            }
         }
         while (bod1 == bod2 || connectionCount[bod1 * nbodies + bod2] >= 2);
         connectionCount[bod1 * nbodies + bod2]++;
         n.setRandom (-0.5, 0.5, randGen);
         for (int i = 0; i < 3; i++) {
            M.set (bod1 * 3 + i, 3 * nbodies + k, n.get (i));
            M.set (bod2 * 3 + i, 3 * nbodies + k, -n.get (i));

            M.set (3 * nbodies + k, bod1 * 3 + i, n.get (i));
            M.set (3 * nbodies + k, bod2 * 3 + i, -n.get (i));
         }
      }
      return M;
   }

   public void timing() {
      CGSolver solver = new CGSolver();
      FunctionTimer timer = new FunctionTimer();

      for (int nbod = 10; nbod <= 50; nbod += 10) {
         SparseMatrixNd Sc = createBlockDiagonal (nbod);
         VectorNd xc = new VectorNd (Sc.rowSize());
         VectorNd bc = new VectorNd (Sc.rowSize());
         bc.setRandom (-0.5, 0.5, randGen);

         int loopCnt = 10;
         boolean solved = true;
         timer.start();
         for (int i = 0; i < loopCnt; i++) {
            xc.setZero();
            if (!solver.solve (xc, Sc, bc, 1e-8, 10 * xc.size())) {
               solved = false;
               break;
            }
         }
         timer.stop();
         if (!solved) {
            System.out.println ("" + nbod + " bodies: No convergence");
         }
         else {
            System.out.println ("" + nbod + " bodies: "
            + timer.resultMsec (loopCnt));
            System.out.println ("         iterations="
            + solver.getNumIterations() + " error="
            + solver.getRelativeResidual());
         }
      }
   }

   public void test() {
      // use a small matrix size for now
      int size = 20;
      double eps = 1e-9;

      CRSolver solver = new CRSolver();
      LUDecomposition LU = new LUDecomposition();

      MatrixNd M = new MatrixNd (size, size);
      VectorNd x = new VectorNd (size);
      VectorNd b = new VectorNd (size);
      VectorNd xcheck = new VectorNd (size);

      // identity matrix to try as trivial pre-conditioner
      MatrixNd I = new MatrixNd (size, size);
      I.setIdentity();
      // inverse matrix to try as pre-conditioner
      MatrixNd Minv = new MatrixNd (size, size);

      for (int i = 0; i < 10; i++) {
         int numIter;
         M.setRandom (-0.5, 0.5, randGen);
         M.mulTranspose (M); // make sure matrix is SPD
         b.setRandom (-0.5, 0.5, randGen);
         x.setZero();

         if (!solver.solve (x, M, b, eps, size * size)) {
            throw new TestException ("No convergence: error="
            + solver.getRelativeResidual());
         }

         numIter = solver.getNumIterations();
         LU.factor (M);
         LU.solve (xcheck, b);

         if (!xcheck.epsilonEquals (x, x.norm() * eps)) {
            throw new TestException (
               "Solver gave wrong answer. Expected\n"
               + xcheck.toString ("%8.3f") + "\nGot\n" + x); //.toString ("%8.3f"));
         }
         //System.out.println ("condEst=" + LU.conditionEstimate (M));

         SparseMatrixNd Sc = createBlockDiagonal (10);
         VectorNd bc = new VectorNd (Sc.colSize());
         VectorNd xc = new VectorNd (Sc.colSize());
         bc.setRandom (-0.5, 0.5, randGen);

         if (!solver.solve (xc, Sc, bc, eps, 10 * xc.size())) {
            throw new TestException (
               "No convergence, constraint problem: error="
               + solver.getRelativeResidual());
         }

         // x.setZero();
         // if(!solver.solve(x, M, b, eps, size * size, I))
         // {
         // throw new TestException("No convergence, identity preconditioner:
         // error=" + solver.getRelativeResidual());
         // }
         //            
         // if(numIter != solver.getNumIterations())
         // {
         // throw new TestException("Different iteration count with identity
         // preconditioner: " + solver.getNumIterations() + " vs. " + numIter);
         // }
         //            
         // LU.inverse(Minv);
         // x.setZero();
         // if(!solver.solve(x, M, b, eps, size * size, Minv))
         // {
         // throw new TestException("No convergence, inverse preconditioner:
         // error=" + solver.getRelativeResidual());
         // }
         //            
         // if(solver.getNumIterations() > 2)
         // {
         // throw new TestException("Num iterations > 2 with inverse
         // preconditioner");
         // }

         // System.out.println ("num iter=" + solver.getNumIterations());

         // try
         // { PrintWriter pw =
         // new PrintWriter (new FileWriter ("mat.m"));
         // pw.println ("M=[\n" + Sc.toString ("%12.9f") + "]");
         // pw.println ("b=[\n" + bc.toString ("%12.9f") + "]");
         // pw.println ("x=[\n" + xc.toString ("%12.9f") + "]");
         // pw.close();
         // }
         // catch (Exception e)
         // {
         // }
      }
   }

   public static void main (String[] args) {
      //System.out.println ("Conjugate Residual Solver Test");

      boolean dotiming = false;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            dotiming = true;
         }
         else {
            System.err.println ("Usage: java maspack.matrix.CGSolver [-timing]");
         }
      }
      CRSolverTest tester = new CRSolverTest();
      try {
         if (dotiming) {
            tester.timing();
         }
         else {
            tester.test();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
