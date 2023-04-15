package maspack.numerics;

import maspack.function.*;
import maspack.matrix.*;
import maspack.util.*;

public class NewtonRootSolver {

   private static int myIterCnt;

   public static int MAX_ITER = 100;

   public static void clearIterationCount() {
      myIterCnt = 0;
   }

   public static int getIterationCount() {
      return myIterCnt;
   }

   /**
    * Safe implementation of Newton's root-finding, which switchs back to
    * bisection when the Newton's method diverges or doesn't converge fast
    * enough.
    * 
    * @param fxn function to evaluate
    * @param a   left-side of interval
    * @param b   right-side of interval
    * @param eps absolute interval tolerance for the root
    * @param feps absolute tolerance for function value, such that
    * a root is found if {@code |f(s)| <= feps}
    * @return root
    * @throws NumericalException if [a,b] does not bracket the root
    */
   public static double findRoot (
      Diff1Function1x1 fxn, double a, double b, double eps, double feps) {

      double fa = fxn.eval (a);
      double fb = fxn.eval (b);
      return findRoot (fxn, a, fa, b, fb, eps, feps);
   }

   /**
    * Safe implementation of Newton's root-finding, which switchs back to
    * bisection when the Newton's method diverges or doesn't converge fast
    * enough.
    * 
    * @param fxn function to evaluate
    * @param a   left-side of interval
    * @param fa  function evaluated at a
    * @param b   right-side of interval
    * @param fb  function evaluated at b
    * @param eps absolute interval tolerance for the root
    * @param feps absolute tolerance for function value, such that
    * a root is found if {@code |f(s)| <= feps}
    * @return root
    * @throws NumericalException if [a,b] does not bracket the root
    */
   public static double findRoot (
      Diff1Function1x1 fxn, double a, double fa, double b, double fb,
      double eps, double feps) {

      if (fa*fb > 0) {
         throw new NumericalException ("root interval not bounded");
      }
      if (Math.abs(fa) <= feps) {
         return a;
      }
      else if (Math.abs(fb) <= feps) {
         return b;
      }
      double lo = a;
      double hi = b;
      if (fa > 0) {
         // make sure f(lo) < 0
         lo = b;
         hi = a;
      }
      
      double x = 0.5*(lo+hi);
      double dxprev = Math.abs(hi-lo);
      double dx = dxprev;
      DoubleHolder deriv = new DoubleHolder();
      int iter = 0;
      for ( ; iter<MAX_ITER; iter++) {
         double f = fxn.eval (deriv, x);
         double df = deriv.value;
         // update bracket
         if (f > feps) {
            hi = x;
         }
         else if (f < -feps) {
            lo = x;
         }
         else {
            return x;
         }
         if ((df*(x-lo)-f < 0 || df*(x-hi)-f > 0) ||
             2*Math.abs(f) > Math.abs(df*dxprev)) {
            // result out of bounds or not decreasing fast enough, so bisect
            dxprev = dx;
            dx = 0.5*(hi-lo);
            x = lo+dx;
            if (lo == x) {
               return x;
            }
         }
         else {
            dxprev = dx;
            // take Newton step
            dx = -f/df;
            double xold = x;
            x += dx;
            if (x == xold) {
               return x;
            }
         }
         if (Math.abs(dx) < eps) {
            return x;
         }
         myIterCnt++;
      }
      throw new NumericalException ("iteration limit "+MAX_ITER+" exceeded");
   }
}
