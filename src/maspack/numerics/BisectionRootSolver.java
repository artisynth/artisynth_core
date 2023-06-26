package maspack.numerics;

import maspack.function.*;
import maspack.matrix.*;
import maspack.util.*;

public class BisectionRootSolver {

   private static int myIterCnt;

   public static int MAX_ITER = 100;

   public static void clearIterationCount() {
      myIterCnt = 0;
   }

   public static int getIterationCount() {
      return myIterCnt;
   }

   /**
    * Finds a root by bisection, given {@code a} and {@code b} that bracket the
    * root.
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
      Function1x1 fxn, double a, double b, double eps, double feps) {

      double fa = fxn.eval (a);
      double fb = fxn.eval (b);
      return findRoot (fxn, a, fa, b, fb, eps, feps);
   }

   /**
    * Finds a root by bisection, given {@code a} and {@code b} that bracket the
    * root.
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
      Function1x1 fxn, double a, double fa, double b, double fb,
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

      int iter = 0;
      for ( ; iter<MAX_ITER; iter++) {
         double x = (lo+hi)/2;
         double f = fxn.eval (x);
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
         if (Math.abs (hi-lo) <= eps) {
            return x;
         }
         myIterCnt++;
      }
      throw new NumericalException ("iteration limit "+MAX_ITER+" exceeded");
   }
}
