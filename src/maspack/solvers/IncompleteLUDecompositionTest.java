/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import maspack.matrix.SparseMatrixNd;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix.WriteFormat;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class IncompleteLUDecompositionTest {
   static void waitforuser() {
      try {
         System.in.read();
      }
      catch (IOException e) {

      }
   }

   public static void main (String[] args) {
      SparseMatrixNd A = new SparseMatrixNd (1, 1);
      Random random = new Random (51);

      try {
         A.scan (new ReaderTokenizer (new FileReader (
            "/ubc/ece/home/hct/other/elliote/A.matrix")));
         System.out.println ("read matrix " + A.rowSize() + " " + A.colSize());
      }
      catch (IOException e) {
         System.out.println (e.getMessage());
         System.exit (0);
      }
      int n = A.rowSize();
      System.out.println ("loaded matrix");

      // int n = 400;
      // A.setSize(n, n);
      // SVDecomposition svd;
      // do
      // {
      // A.setRandom(1, 2, n*2, random);
      // for(int i = 0; i < n; i++)
      // A.set(i, i, random.nextDouble() + 1);
      //            
      // svd = new SVDecomposition(A);
      // }
      // while(svd.determinant() < 1e-10);
      // A.mulTranspose(A);
      // System.out.println("created spd matrix");

      // waitforuser();

      IncompleteLUDecomposition ilud = new IncompleteLUDecomposition();
      ilud.factor (A, 0.00001);

      if (ilud.L.containsNaN() || ilud.U.containsNaN()) {
         System.out.println ("LU contains NaN");
      }

      System.out.println ("factored matrix");
      // waitforuser();

      SparseMatrixNd tmp = new SparseMatrixNd (n, n);
      tmp.mul (ilud.L, ilud.U);
      // System.out.println(new MatrixNd(tmp));
      tmp.sub (A);
      System.out.println ("residual matrix one norm " + tmp.oneNorm());

      VectorNd x = new VectorNd (n);
      VectorNd b = new VectorNd (n);
      VectorNd xc = new VectorNd (n);

      try {
         System.out.println (
            "writing solve matrix and incomplete cholesky decomposition");

         PrintWriter fwr;

         fwr = new PrintWriter ("/ubc/ece/home/hct/other/elliote/LUA.matrix");
         // fwr.append("[ ");
         A.write (fwr, new NumberFormat(), WriteFormat.Dense);
         // fwr.append(" ]");
         fwr.close();

         fwr = new PrintWriter ("/ubc/ece/home/hct/other/elliote/L.matrix");
         // fwr.append("[ ");
         ilud.L.write (fwr, new NumberFormat(), WriteFormat.Dense);
         // fwr.append(" ]");
         fwr.close();

         fwr = new PrintWriter ("/ubc/ece/home/hct/other/elliote/U.matrix");
         // fwr.append("[ ");
         ilud.U.write (fwr, new NumberFormat(), WriteFormat.Dense);
         // fwr.append(" ]");
         fwr.close();

         System.out.println ("finished writing");
      }
      catch (IOException e) {
         System.out.println (e.getMessage());
         System.exit (0);
      }

      // test backsolves

      System.out.println();

      CGSolver isolver = new CGSolver();
      DirectSolver dsolver = new UmfpackSolver();
      // dsolver.factor(A);

      b.setRandom (-1, 1, random);

      System.out.println ("solving L * x = b");
      ilud.solveL (x, b);
      dsolver.analyzeAndFactor (ilud.L);
      dsolver.solve (xc, b);
      System.out.println ("b " + b);
      System.out.println ("x " + x);
      System.out.println ("xc " + xc);
      if (!x.epsilonEquals (xc, 1e-6)) {
         System.out.println ("backsolve failed");
      }

      System.out.println();

      System.out.println ("solving U * x = b");
      ilud.solveU (x, b);
      dsolver.analyzeAndFactor (ilud.U);
      dsolver.solve (xc, b);
      System.out.println ("b " + b);
      System.out.println ("x " + x);
      System.out.println ("xc " + xc);
      if (!x.epsilonEquals (xc, 1e-6)) {
         System.out.println ("backsolve failed");
      }

      // test upcg solver

      System.out.println();
      System.out.println ("solving A * x = b");

      double tol = 1e-2;
      int maxit = 1500;

      System.out.println ("preconditioned solve untransformed");
      x.setZero();
      isolver.solve (x, A, b, tol, maxit, ilud);
      System.out.println ("iterations " + isolver.getNumIterations());
      System.out.println ("b " + b);
      System.out.println ("x " + x);

      System.out.println();

      // System.out.println("preconditioned solve transformed");
      // x.setZero();
      // isolver.solveTransformed(x, A, b, tol, maxit, icd);
      // System.out.println("iterations " + isolver.getNumIterations());
      // System.out.println("b " + b);
      // System.out.println("x " + x);
      //        
      // System.out.println();

      System.out.println ("unpreconditioned solve");
      x.setZero();
      isolver.solve (x, A, b, tol, maxit);
      System.out.println ("iterations " + isolver.getNumIterations());
      System.out.println ("b " + b);
      System.out.println ("x " + x);

      System.out.println();

      System.out.println ("direct solve");
      x.setZero();
      dsolver.analyzeAndFactor (A);
      dsolver.solve (x, b);
      System.out.println ("b " + b);
      System.out.println ("x " + x);
   }
}
