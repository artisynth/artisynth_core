package maspack.numerics;

import maspack.util.*;
import maspack.function.*;

public class BrentMinimizerTest extends RootSolverTestBase {

   boolean evalAtEnds = false;

   void test (
      Function1x1 func, double a, double b, double xchk, double eps) {

      BrentMinimizer brent = new BrentMinimizer();
      FunctionValuePair pair = brent.findMinimum (func, a, b, eps, evalAtEnds);
      int icnt = brent.getIterationCount();
      System.out.println ("x=" + pair.x + ", iters=" + icnt);
      double tol = eps*(b-a);
      if (!evalAtEnds) {
         tol *= 10;
      }
      if (Math.abs(pair.x-xchk) > tol) {
         throw new TestException (
            "Computed minimum is "+pair.x+", expected "+xchk);
      } 
      if (brent.iterationLimitExceeded()) {
         throw new TestException (
            "Iteration limit exceeded");
      }
   }   

   public void test(){
      Function1x1 cubic = new CubicFxn (1, 0, -2, -5);
      Function1x1 quartic = new QuarticFxn (1, -1, -7, 1, 16);
      test (cubic, -0.5, 2, 0.816496580927726, 1e-8);
      test (cubic, 0.5, 3, 0.816496580927726, 1e-8);

      test (quartic, -3, 0, -1.574213798563528, 1e-8);      
      test (quartic, -2.5, 0.5, -1.574213798563528, 1e-8);      
      test (quartic, 1, 3, 2.253749245862283, 1e-8);      
      test (new CosFxn(), 0, 5, Math.PI, 1e-8);

      // cubic near the minimum
      test (cubic, 0.5, 1, 0.816496580927726, 1e-8);

      // easy case: parabola at x = 1
      test (new CubicFxn (0, 1, -2, 1), 0.5, 2, 1.00, 1e-8);

      // cases where mins are  on the ends
      test (cubic, -1, 0.5, 0.5, 1e-8);
      test (cubic, 1, 1.5, 1.0, 1e-8);
      // inverted parabola at x = 1
      test (new CubicFxn (0, -1, 2, -1), 0, 1.5, 0.00, 1e-8);
   }

   public void testSpecial(){
      test (new CubicFxn (0, 1, -2, 1), 0.5, 2, 1.00, 1e-8);
   }

   public static void main (String[] args) {
      BrentMinimizerTest tester = new BrentMinimizerTest();
      //tester.testSpecial();
      tester.runtest();
   }
}
