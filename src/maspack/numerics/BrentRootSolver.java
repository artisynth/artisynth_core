/*
 * Brent's algorithm for solving roots in one dimension. Adapted from
 * BrentSolver in the Apache commons library.
 * -------------------------------------------------------------------
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package maspack.numerics;

import maspack.function.*;
import maspack.matrix.*;
import maspack.util.*;

public class BrentRootSolver {

   private static int myIterCnt;

   /**
    * Maximum number of iterations. This should be sufficient since the
    * iteration falls back on bisection if necessary.
    */
   public static final int MAX_ITER = 100;

   private static final double PREC = 1e-16;

   public static void clearIterationCount() {
      myIterCnt = 0;
   }

   public static int getIterationCount() {
      return myIterCnt;
   }
   
   /** Mask used to clear the non-sign part of a long. */
   private static final long MASK_NON_SIGN_LONG = 0x7fffffffffffffffl;

   // From Apache commons. Probably don't need - tests seem to show it no
   // faster than Math.abs().
   private static double fastAbs (double x) {
      return Double.longBitsToDouble(
         MASK_NON_SIGN_LONG & Double.doubleToRawLongBits(x));
   }

   private static double SIGN (double a, double b) {
      return b >= 0 ? (a >= 0 ? a : -a) : (a >= 0 ? -a : a);
   }

   /**
    * Implementation of Brent's root-finding method.
    * 
    * @param fxn function to evaluate
    * @param a   left-side of interval
    * @param b   right-side of interval
    * @param eps absolute tolerance for the root.
    * @param feps absolute tolerance for function value, such that
    * a root is found if {@code |f(s)| <= feps}.
    * @throws NumericalException if [a,b] does not bracket the root, or if the
    * number of iteractions exceeds {@link #MAX_ITER}.
    */
   public static double findRoot (
      Function1x1 fxn, double a, double b, double eps, double feps) {

      double fa = fxn.eval (a);
      double fb = fxn.eval (b);
      return findRoot (fxn, a, fa, b, fb, eps, feps);
   }

   /**
    * Implementation of Brent's root-finding method.
    * 
    * @param fxn function to evaluate
    * @param a   left-side of interval
    * @param fa  function evaluated at a
    * @param b   right-side of interval
    * @param fb  function evaluated at b
    * @param eps absolute tolerance for the root.
    * @param feps absolute tolerance for function value, such that
    * a root is found if {@code |f(s)| <= feps}.
    * @throws NumericalException if [a,b] does not bracket the root, or if the
    * number of iteractions exceeds {@link #MAX_ITER}.
    */
   public static double findRoot (
      Function1x1 fxn, double a, double fa, double b, double fb,
      double eps, double feps) {


      if (fastAbs(fa) <= feps) {
         return a;
      }
      else if (fastAbs(fb) <= feps) {
         return b;
      }
      
      double c = a;
      double fc = fa;
      double d = b - a;
      double e = d;

      while (true) {
         if (fastAbs(fc) < fastAbs(fb)) {
            a = b;
            b = c;
            c = a;
            fa = fb;
            fb = fc;
            fc = fa;
         }

         final double tol = 2 * PREC * fastAbs(b) + 0.5*eps;
         final double m = 0.5 * (c - b);

         if (fastAbs(m) <= tol || fastAbs(fb) <= feps) {
            // Apache library used Precision.equals (fb, 0) instead of fb == 0
            return b;
         }
         if (fastAbs(e) < tol ||
             fastAbs(fa) <= fastAbs(fb)) {
            // Force bisection.
            d = m;
            e = d;
         } else {
            double s = fb / fa;
            double p;
            double q;
            // The equality test (a == c) is intentional,
            // it is part of the original Brent's method and
            // it should NOT be replaced by proximity test.
            if (a == c) {
               // Linear interpolation.
               p = 2 * m * s;
               q = 1 - s;
            } else {
               // Inverse quadratic interpolation.
               q = fa / fc;
               final double r = fb / fc;
               p = s * (2 * m * q * (q - r) - (b - a) * (r - 1));
               q = (q - 1) * (r - 1) * (s - 1);
            }
            if (p > 0) {
               q = -q;
            } else {
               p = -p;
            }
            s = e;
            e = d;
            if (p >= 1.5 * m * q - fastAbs(tol * q) ||
                p >= fastAbs(0.5 * s * q)) {
               // Inverse quadratic interpolation gives a value
               // in the wrong direction, or progress is slow.
               // Fall back to bisection.
               d = m;
               e = d;
            } else {
               d = p / q;
            }
         }
         a = b;
         fa = fb;

         if (fastAbs(d) > tol) {
            b += d;
         } else if (m > 0) {
            b += tol;
         } else {
            b -= tol;
         }
         fb = fxn.eval (b);
         if ((fb > 0 && fc > 0) ||
             (fb <= 0 && fc <= 0)) {
            c = a;
            fc = fa;
            d = b - a;
            e = d;
         }
         myIterCnt++;
      }
   }
}
