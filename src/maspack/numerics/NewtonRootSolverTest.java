package maspack.numerics;

import maspack.util.*;
import maspack.function.*;

public class NewtonRootSolverTest extends RootSolverTestBase {

   void test (
      Diff1Function1x1 func, double a, double b, double xchk,
      double eps, double feps) {

      int icnt = NewtonRootSolver.getIterationCount();
      double x = NewtonRootSolver.findRoot (func, a, b, eps, feps);
      icnt = NewtonRootSolver.getIterationCount() - icnt;
      System.out.println ("x=" + x + ", iters=" + icnt);
      if (Math.abs(x-xchk) > eps) {
         throw new TestException (
            "Computed root is "+x+", expected "+xchk);
      }
   }

   public void test() {
      Diff1Function1x1 fxn = new CubicFxn (1, 0, -2, -5);
      test (fxn, 1, 3, 2.094551481542, 1e-10, 0);
      test (new CubicFxn (-1, 0, 2, 5), 1, 3, 2.094551481542, 1e-10, 0);
      test (new CosFxn(), 1, 2, Math.PI/2, 1e-10, 0);
      fxn = new QuarticFxn (1, -1, -7, 1, 16);
      test (fxn,    1, 2.5, 1.780019009641143, 1e-14, 0);
      test (fxn, -2, 2.5, 1.780019009641143, 1e-14, 0);
      test (fxn, -6.5, 2.5, 1.780019009641143, 1e-14, 0);
      test (fxn, -10, 2.5, 1.780019009641143, 1e-14, 0);
      // try with non-zero feps
      test (fxn, -6.5, 2.5, 1.780019009641143, 1e-14, 1e-10);
      test (fxn, -6.5, 2.5, 1.780019009641143, 1e-7, 1e-6);
      test (fxn, -6.5, 2.5, 1.780019009641143, 1e-7, 1e-4);

      fxn = new QuarticFxn (-1, 1, 7, -1, -16);
      test (fxn, -6.5, 2.5, 1.780019009641143, 1e-14, 0);
      fxn = new CubicFxn (0, 1, 0, -1);
      // test when the root is on the end points
      test (fxn, 0, 1, 1, 1e-14, 0);
      test (fxn, -1, 0, -1, 1e-14, 0);
      test (fxn, -2, 0, -1, 1e-14, 0);
      test (fxn, -1.9, 0, -1, 1e-14, 0);
   }

   public static void main (String[] args) {
      NewtonRootSolverTest tester = new NewtonRootSolverTest();
      tester.runtest();
   }

}
