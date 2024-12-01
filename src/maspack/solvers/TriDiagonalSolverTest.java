package maspack.solvers;

import maspack.matrix.*;
import maspack.util.*;
import maspack.interpolation.*;

/**
 * Test class for TriDiagonalSolver.
 */
public class TriDiagonalSolverTest extends UnitTest {

   private static double EPS = 1e-12;
   
   VectorNd computeCheck (VectorNd a, VectorNd b, VectorNd c, VectorNd r) {
      int n = b.size();
      MatrixNd M = new MatrixNd (n, n);
      for (int i=0; i<n; i++) {
         M.set (i, i, b.get(i));
         if (i > 0) {
            M.set (i-1, i, c.get(i-1));
            M.set (i, i-1, a.get(i-1));
         }
      }
      LUDecomposition lud = new LUDecomposition (M);
      VectorNd x = new VectorNd(n);
      lud.solve (x, r);
      return x;
   }         

   VectorNd cyclicalComputeCheck (
      VectorNd a, VectorNd b, VectorNd c, VectorNd r) {
      int n = b.size();
      MatrixNd M = new MatrixNd (n, n);
      for (int i=0; i<n; i++) {
         M.set (i, i, b.get(i));
         if (i > 0) {
            M.set (i-1, i, c.get(i-1));
            M.set (i, i-1, a.get(i-1));
         }
      }
      M.set (0, n-1, a.get(n-1));
      M.set (n-1, 0, c.get(n-1));
      LUDecomposition lud = new LUDecomposition (M);
      VectorNd x = new VectorNd(n);
      lud.solve (x, r);
      return x;
   }         

   void getElements (VectorNd v, Vector3d[] vecs, int idx) {
      for (int i=0; i<vecs.length; i++) {
         v.set (i, vecs[i].get(idx));
      }
   }

   void setElements (Vector3d[] vecs, VectorNd v, int idx) {
      for (int i=0; i<vecs.length; i++) {
         vecs[i].set(idx, v.get(i));
      }
   }

   void testSolve (int n) {
      VectorNd x = new VectorNd();
      VectorNd a = new VectorNd(n); // only first n-1 used for regular solves
      VectorNd b = new VectorNd(n);
      VectorNd r = new VectorNd(n);
      VectorNd c = new VectorNd(n); // only first n-1 used for regular solves

      a.setRandom();
      b.setRandom();
      c.setRandom();
      r.setRandom();

      // vectorized right hand sides and solutions
      Vector3d[] rvals = new Vector3d[n];
      Vector3d[] xchks = new Vector3d[n];
      for (int i=0; i<n; i++) {
         rvals[i] = new Vector3d();
         rvals[i].setRandom();
         xchks[i] = new Vector3d();
      }

      // test standard solve
      TriDiagonalSolver.solve (x, a, b, c, r);
      VectorNd xchk = computeCheck (a, b, c, r);
      checkNormedEquals ("solve for n=" + n, x, xchk, EPS);
 
      // test standard 3-vector solve
      Vector3d[] xvals = TriDiagonalSolver.solve (a, b, c, rvals);
      // compute check values
      for (int j=0; j<3; j++) {
         getElements (r, rvals, j);
         xchk = computeCheck (a, b, c, r);
         setElements (xchks, xchk, j);
      }
      for (int i=0; i<n; i++) {
         checkNormedEquals (
            "vec solve for n="+n+", i="+i, xvals[i], xchks[i], EPS);
      }

      if (n < 3) {
         // cyclical solves undefined for n < 3
         return;
      }

      // test cyclical solve
      TriDiagonalSolver.solveCyclical (x, a, b, c, r);
      xchk = cyclicalComputeCheck (a, b, c, r);
      checkNormedEquals ("cyclical solve for n=" + n, x, xchk, EPS);
 
      // test cyclical 2-vector solve
      xvals = TriDiagonalSolver.solveCyclical (a, b, c, rvals);
      // compute check values
      for (int j=0; j<3; j++) {
         getElements (r, rvals, j);
         xchk = cyclicalComputeCheck (a, b, c, r);
         setElements (xchks, xchk, j);
      }
      for (int i=0; i<n; i++) {
         checkNormedEquals (
            "vec cyclical solve for n="+n+", i="+i, xvals[i], xchks[i], EPS);
      }
   }

   public void test() {
      for (int n=1; n<15; n++) {
         testSolve (n);
      }
   }

   public static void main (String[] args) {
      TriDiagonalSolverTest tester = new TriDiagonalSolverTest();

      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }

}
