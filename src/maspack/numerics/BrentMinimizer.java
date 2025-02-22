package maspack.numerics;

import maspack.function.Function1x1;
import maspack.function.FunctionValuePair;

/**
 * Implements Brent's minimization procedure for a single-valued function of
 * one variable, as described in:
 * 
 * <blockquote> Richard Brent, Algorithms for Minimization without Derivatives,
 * Dover, 2002, ISBN: 0-486-41998-3 </blockquote>
 *
 * The code itself is based on the C implementation by John Burkardt available
 * at https://people.math.sc.edu/Burkardt/c_src/brent/brent.html. An almost
 * identical implementation is available in Numerical Recipes in C.
 */
public class BrentMinimizer {

   private static final double PREC = 1e-16;

   private static final double GOLD = (Math.sqrt(5)+1)/2; 
   private static final double CGOLD = 1-GOLD;

   /*
     C is the square of the inverse of the golden ratio.
   */
   private static double C = 0.5 * ( 3.0 - Math.sqrt ( 5.0 ) );

   public static int DEFAULT_MAX_ITERS = 50;
   protected int myMaxIters = DEFAULT_MAX_ITERS;

   private int myIterCnt;
   private boolean myIterLimitExceeded;

   /**
    * Queries the maximum number of allowed iterations.
    *
    * @return maximum number of iterations
    */
   public int getMaxIterations() {
      return myMaxIters;
   }

   /**
    * Set the maximum number of allowed iterations. The default is given by
    * {@link #DEFAULT_MAX_ITERS}.
    *
    * @param maxi maximum number of iterations
    */
   public void setMaxIterations(int maxi) {
      myMaxIters = maxi;
   }

   /**
    * Clears the cummulatuve iteration count.
    */
   public void clearIterationCount() {
      myIterCnt = 0;
   }

   /**
    * Queries the cummulative number of iterations invoked by this minimizer
    * since it was created or {@link #clearIterationCount} was called.
    *
    * @return cummulative iteration count
    */
   public int getIterationCount() {
      return myIterCnt;
   }

   /**
    * Queries whether the iteration limit was exceeded on the most recent call
    * to one of the {@code findMinimum} methods.
    *
    * @return {@code true} if iteration limit was exceeded.
    */
   public boolean iterationLimitExceeded() {
      return myIterLimitExceeded;
   }

   private static double sign (double a, double b) {
      return Math.abs(a)*Math.signum(b);
   }

   /**
    * Finds a local minimum of a function within an interval {@code [a,b]}.
    * This is a static convenience wrapper for {@link
    * #findMinimum(Function1x1,double,,double,double)}.
    *
    * @param func function to evaluate
    * @param a left interval end point
    * @param b right interval end point
    * @param eps relative tolerance for finding the minimum abscissa
    * @return FunctionValuePair giving the abscissa of the minimum
    * and the minimum value
    */
   public static FunctionValuePair findMin (
      Function1x1 func, double a, double b, double eps) {

      BrentMinimizer brent = new BrentMinimizer();
      return brent.findMinimum (func, a, func.eval(a), b, func.eval(b), eps);
   }

   /**
    * Finds a local minimum of a function within an interval {@code [a,b]}.
    * The minimum is found within a relative abscissa tolerance {@code eps},
    * which generally should not be less than {@code 1.0e-8} (i.e., the square
    * root of machine precision). If no true local mimimum is found, then the
    * method will return the value at the minimum end point.
    *
    * @param func function to evaluate
    * @param a left interval end point
    * @param b right interval end point
    * @param eps relative tolerance for finding the minimum abscissa
    * @return FunctionValuePair giving the abscissa of the minimum
    * and the minimum value
    */
   public FunctionValuePair findMinimum (
      Function1x1 func, double a, double b, double eps) {

      return findMinimum (func, a, func.eval(a), b, func.eval(b), eps);
   }

   /**
    * Finds a local minimum of a function within an interval {@code [a,b]}.
    * The minimum is found within a relative abscissa tolerance {@code eps},
    * which generally should not be less than {@code 1.0e-8} (i.e., the square
    * root of machine precision). If no true local mimimum is found, then the
    * method will return the value at the minimum end point.
    *
    * @param func function to evaluate
    * @param a left interval end point
    * @param b right interval end point
    * @param eps relative tolerance for finding the minimum abscissa
    * @param evalEndPoints if {@code true}, the method starts by evaluating the
    * function at the interval end points, which may result in fewer
    * iterations. This is the default behavior for methods that do not expose
    * this as an option.
    * @return FunctionValuePair giving the abscissa of the minimum
    * and the minimum value
    */
   public FunctionValuePair findMinimum (
      Function1x1 func, double a, double b, double eps, boolean evalEndPoints) {

      if (evalEndPoints) {
         return findMinimum (func, a, b, eps);
      }
      else {
         double x = a + C * (b - a);
         double fx = func.eval(x);
         return doFindMin (func, a, b, x, fx, x, fx, x, fx, eps);
      }
   }

   /**
    * Finds the minimum of a function over an interval {@code [a,b]}, given
    * evaluations of the function at the end points. This is a static
    * convenience wrapper for {@link
    * #findMinimum(Function1x1,double,double,double,double,double)}.
    * 
    * @param func function to evaluate
    * @param a left interval end point
    * @param fa value of the function at {@code a}
    * @param b right interval end point
    * @param fb value of the function at {@code b}
    * @param eps relative tolerance for finding the minimum abscissa
    * @return FunctionValuePair giving the abscissa of the minimum
    * and the minimum value
    */
   public static FunctionValuePair findMin (
      Function1x1 func, double a, double fa, double b, double fb,
      double eps) {
      BrentMinimizer brent = new BrentMinimizer();
      return brent.findMinimum (func, a, fa, b, fb, eps);
   }

   /**
    * Finds the minimum of a function over an interval {@code [a,b]}, given
    * evaluations of the function at the end points. The minimum is found
    * within a relative abscissa tolerance {@code eps}, which generally should
    * not be less than {@code 1.0e-8} (i.e., the square root of machine
    * precision). If no true local mimimum is found, then the method will
    * return the value at the minimum end point.
    *
    * @param func function to evaluate
    * @param a left interval end point
    * @param fa value of the function at {@code a}
    * @param b right interval end point
    * @param fb value of the function at {@code b}
    * @param eps relative tolerance for finding the minimum abscissa
    * @return FunctionValuePair giving the abscissa of the minimum
    * and the minimum value
    */
   public FunctionValuePair findMinimum (
      Function1x1 func, double a, double fa, double b, double fb,
      double eps) {

      double u = a + C * (b - a);
      double fu = func.eval(u);

      // set w, v, x based on a, b, u
      if (fu < fa && fu < fb) {
         // u is minimum
         if (fa < fb) {
            // (x, w, v) := (u, a, b)
            return doFindMin (func, a, b, u, fu, a, fa, b, fb, eps);
         }
         else {
            // (x, w, v) := (u, b, a)
            return doFindMin (func, a, b, u, fu, b, fb, a, fa, eps);
         }
      }
      else if (fb < fa && fb < fu) {
         // b is minimum
         if (fa <= fu) {
            // (x, w, v) := (b, a, u)
            return doFindMin (func, a, b, b, fb, a, fa, u, fu, eps);
         }
         else {
            // (x, w, v) := (b, u, a)
            return doFindMin (func, a, b, b, fb, u, fu, a, fa, eps);
         }
      }
      else {
         // a is minimum
         if (fu < fb) {
            // (x, w, v) := (a, u, b)
            return doFindMin (func, a, b, a, fa, u, fu, b, fb, eps);
         }
         else {
            // (x, w, v) := (a, b, u)
            return doFindMin (func, a, b, a, fa, b, fb, u, fu, eps);
         }
      }
   }

   private FunctionValuePair doFindMin (
      Function1x1 func, double a, double b,
      double x, double fx, double w, double fw, double v, double fv,
      double eps) {

      double d = 0;   // distance moved on the last step
      double e = 0;   // distance moved on the step before last
      double fu;

      double sa, sb;  // current interval bounding the minimum
      double m;       // midpoint of the current interval
      double tol1;
      double tol2;
      double u;       // most recent function evaluation point

      sa = a;
      sb = b;

      myIterLimitExceeded = false;

      if (fx < fw && ((w < x && x < v) || (v < x && x < w))) {
         // Ok to start with a paraboilc fit. Set e and d to artificial
         // non-zero values to allow parabolic interpolation to begin right
         // awau.
         if (x - a > b - x) {
            e = d = x - a;
         }
         else {
            e = d = x - b;
         }
      }

      double t = eps*(b-a);

      int iter = 0;
      while (iter<myMaxIters) {
         m = 0.5*(sa + sb) ;
         tol1 = eps * Math.abs(x) + t;
         tol2 = 2.0 * tol1;

         // Check the stopping criterion.
         if (Math.abs(x-m) + 0.5*(sb-sa) <= tol2) {
            myIterCnt += iter;
            return new FunctionValuePair (x, fx);
         }

         double r, p, q; // coefficients for parabola fit
         r = p = q = 0;
         double eprev = 0;
         if (tol1 < Math.abs(e)) {
            // Find coefficents to find min of parabola through v, x, w.
            // Note that the formula does not depend on the ordering of v, x, w,
            r = (x - w)*(fx - fv);
            q = (x - v)*(fx - fw);
            p = (x - v)*q - (x - w)*r;
            q = 2.0*(q - r);
            if (0.0 < q) {
               p = - p;
            }
            q = Math.abs(q);
            eprev = e;
            e = d;
         }

         if (Math.abs(p) < Math.abs(0.5*q*eprev) &&
             q*(sa - x) < p && p <= q*(sb - x)) {
            // Take a parabolic interpolation step.
            d = p / q;
            u = x + d;
            // F must not be evaluated too close to A or B.
            if ((u - sa) < tol2 || (sb - u) < tol2) {
               if (x < m) {
                  d = tol1;
               }
               else  {
                  d = -tol1;
               }
            }
         }
         else {
            // Take a golden section step
            e = (x < m) ? (sb - x) : (sa - x);
            d = C*e;
         }

         // F must not be evaluated too close to X.
         if (tol1 <= Math.abs(d)) {
            u = x + d;
         }
         else if (0.0 < d) {
            u = x + tol1;
         }

         else {
            u = x - tol1;
         }

         fu = func.eval (u);
         // Update A, B, V, W, and X.
         if (fu <= fx) {
            // New minimum. shrink [sa, ab] and set (x, w, v) := (u, x, w),
            // eliminating v
            if (u < x) {
               sb = x;
            }
            else {
               sa = x;
            }
            v = w; w = x; x = u;
            fv = fw; fw = fx; fx = fu;
         }
         else {
            // Minimum unchanged. shrink [sa, sb]
            if (u < x) {
               sa = u;
            }
            else {
               sb = u;
            }
            if (fu <= fw || w == x) {
               // set (v, w) := (w, u), eliminating v
               v = w; w = u;
               fv = fw; fw = fu;
            }
            else if (fu <= fv || v == x || v == w) {
               // replace v with u
               v = u;
               fv = fu;
            }
         }
         iter++;
      }
      myIterCnt += iter;
      myIterLimitExceeded = true;
      return new FunctionValuePair (x, fx);
   }
}
