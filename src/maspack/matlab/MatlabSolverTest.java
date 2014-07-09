/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matlab;

import maspack.util.*;
import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;

public class MatlabSolverTest
{
   private static int[] incIndices(int[] idxs)
   {
      int[] newIdxs = new int[idxs.length];
      for (int i = 0; i < idxs.length; i++)
      {
	 newIdxs[i] = idxs[i] + 1;
      }
      return newIdxs;
   }

   public static void main(String[] args)
   {
      MatlabSolver solver = new MatlabSolver();
      int i;

      // set test symmetric matrix:
      // M = [3 1 2 0 0
      //      1 0 1 2 0
      //      2 1 4 1 0
      //      0 2 1 0 6
      //      0 0 0 6 2]
      NumberFormat fmt = new NumberFormat("%8.3f");
      double[] vals3 = new double[] { 3, 1, 2, 0, 1, 2, 4, 1, 0, 6, 2 };
      int[] rows3 = new int[] { 0, 3, 6, 8, 10, 11 };
      int[] cols3 = new int[] { 0, 1, 2, 1, 2, 3, 2, 3, 3, 4, 4 };
      double[] b3 = new double[] { 1, 2, 3, 4, 5 };
      double[] x3 = new double[5];
      MatrixNd M = new MatrixNd(5, 5);
      int[] rows3_1 = incIndices(rows3);
      int[] cols3_1 = incIndices(cols3);
      M.setCRSValues(vals3, cols3_1, rows3_1, 11, 5, Partition.UpperTriangular);
      solver.analyze(M, 5, Matrix.SYMMETRIC);

      // solver.setSymmetricMatrix (vals3, rows3_1, cols3_1, 5, 11);
      // solver.factorMatrix(vals3);
      solver.factor();
      solver.solve(x3, b3);
      System.out.println("Sparse symmetric:");
      for (i = 0; i < 5; i++)
      {
	 System.out.println(fmt.format(x3[i]));
      }
      System.exit(0);

      // Now change matrix but keep topology:
      // M = [3 1 2 0 0
      //      1 10 1 2 0
      //      2 1 4 1 0
      //      0 2 1 10 5
      //      0 0 0 5 2]
      double[] vals4 = { 3, 1, 2, 10, 1, 2, 4, 1, 10, 5, 2 };
      M.setCRSValues(vals4, cols3_1, rows3_1, 11, 5, Partition.UpperTriangular);
      // solver.factorMatrix(vals4);
      solver.factor();
      solver.solve(x3, b3);
      System.out.println("Sparse symmetric, different values:");
      for (i = 0; i < 5; i++)
      {
	 System.out.println(fmt.format(x3[i]));
      }

      double[] vals = new double[] { 1, 2, 3, 0, 4, 0, 5, 0, 6 };
      int[] rows = new int[] { 0, 3, 6, 9 };
      int[] cols = new int[] { 0, 1, 2, 0, 1, 2, 0, 1, 2 };
      int[] rows_1 = incIndices(rows);
      int[] cols_1 = incIndices(cols);
      double x[] = new double[3];
      double[] b1 = new double[] { 1, 2, 3 };

      M = new MatrixNd(3, 3);
      M.setCRSValues(vals, cols_1, rows_1, 9, 3, Partition.Full);
      
      solver.analyze(M, 3, 0);
      solver.factor();
      // solver.setMatrix (vals, rows_1, cols_1, 3, 9);
      // solver.factorMatrix();
      solver.solve(x, b1);
      System.out.println("Dense unsymmetric:");
      for (i = 0; i < 3; i++)
      {
	 System.out.println(fmt.format(x[i]));
      }

      double[] b2 = new double[] { 4, 5, 6 };

      solver.solve(x, b2);
      System.out.println("Dense unsymmetric, second solution:");
      for (i = 0; i < 3; i++)
      {
	 System.out.println(fmt.format(x[i]));
      }

      double[] vals2 = new double[] { 26, 2, 33, 20, 6, 45 };
      int[] rows2 = new int[] { 0, 3, 5, 6 };
      int[] cols2 = new int[] { 0, 1, 2, 1, 2, 2 };
      int[] rows2_1 = incIndices(rows);
      int[] cols2_1 = incIndices(cols);
      M.setCRSValues(vals2, cols2_1, rows2_1, 6, 3, Partition.UpperTriangular);

      solver.analyze(M, 3, Matrix.SYMMETRIC);
      solver.factor();
      // solver.setSymmetricMatrix (vals2, rows2_1, cols2_1, 3, 6);
      // solver.factorMatrix();
      solver.solve(x, b1);
      System.out.println("Dense symmetric:");
      for (i = 0; i < 3; i++)
      {
	 System.out.println(fmt.format(x[i]));
      }
   }
}
