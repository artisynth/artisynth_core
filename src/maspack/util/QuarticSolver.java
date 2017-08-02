/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Finds the real roots of a quartic equation on a specified interval.
 *
 * Also, a good guide to the structure of quartics and their solution is the
 * paper "The quartic equation: invariants and Euler's solution revealed", by
 * RWD Nickalls.
 */
public class QuarticSolver {

   public static double RTOL = 1e-10;
   public static double PREC = 1e-16;

   static int iterationCount;
   static int bisectionCount;
   static boolean useNewtonsMethod = true;

   public static int getRoots (
      double[] roots, double a, double b, double c, double d, double e) {

      return getRoots (roots, a, b, c, d, e, 0, 1);
   }

   public static int getRoots (
      double[] roots, double a, double b, double c, double d, double e,
      double x0, double x1) {
      
      int icnt = 0;
      double[] interval_x = new double[5];

      if (x0 > x1) {
         // sanity check; swap
         double tmp = x0; x0 = x1; x1 = tmp;
      }

      if (a == 0) {
         // quartic is just a cubic, so solve the roots of that
         return CubicSolver.getRoots (roots, b, c, d, e, x0, x1);
      }
      double[] droots = new double[3];

      // solve for the roots of the derivative to find extremal points
      int ndr = CubicSolver.getRoots (droots, 4*a, 3*b, 2*c, d, x0, x1);
      
      if (ndr == 0) {
         // polynominal is monotone on the interval, so at most only one root

         double y0 = (((x0*a+b)*x0+c)*x0+d)*x0 + e;
         double y1 = (((x1*a+b)*x1+c)*x1+d)*x1 + e;

         if (y0*y1 > 0) {
            // then y0 and y1 have the same sign so there is no root
            return 0;
         }
         else if (y0 == 0) {
            // root at x0
            roots[0] = x0;
            return 1;
         }
         else if (y1 == 0) {
            // root at x1
            roots[0] = x1;
            return 1;
         }
         else {
            interval_x[icnt++] = x0;
            interval_x[icnt++] = x1;
         }
      }
      else {
         // set up search regions between derivative roots
         interval_x[icnt++] = x0;
         for (int i=0; i<ndr; i++) {
            interval_x[icnt++] = droots[i];
         }
         interval_x[icnt++] = x1;
      }

      // look for roots in each of the indicated intervals
      int nrts = 0;

      double xtol = RTOL*Math.abs (x1-x0);
      double xmax = Math.max (Math.abs (x0), Math.abs (x1));
      double ytol =
         PREC*(((Math.abs(a)*xmax+Math.abs(b))*xmax+
                Math.abs(c))*xmax+Math.abs(d));
      
      double x = interval_x[0];
      double y = (((a*x+b)*x+c)*x+d)*x + e;
      double xnext = 0;
      double ynext = 0;

      for (int i=0; i<icnt-1; i++) {
         xnext = interval_x[i+1];
         ynext = (((a*xnext+b)*xnext+c)*xnext+d)*xnext + e;
         if (y == 0) {
            roots[nrts++] = x;
         }
         else {
            if ((y < 0 && ynext > 0) || (y > 0 && ynext < 0)) {
               // then there is a root in the interval; hunt it down
               double xlo = x;
               double xhi = xnext;
               double ylo = y;
               double yhi = ynext;

               double alpha = 0.5;
               double xnew = (xlo+xhi)/2.0;
               
               int iteration;
               for (iteration=0; iteration<50; iteration++){
                  double ynew = (((a*xnew+b)*xnew + c)*xnew+d)*xnew+e;
                  double dnew = ((4*a*xnew+3*b)*xnew+2*c)*xnew + d;

                  ytol = Math.max (dnew, PREC)*xtol;

                  //System.out.printf (" ymi=%18.15e\n", ynew);
                  //if(Math.abs(ynew)<1e-2*convergence_tol) break;
                  if (Math.abs(ynew) < ytol) {
                     //System.out.println ("ynew=" + ynew + " tol=" + ytol);
                     break;
                  }
                  if((ylo<0 && ynew>0) || (ylo>0 && ynew<0)){
                     // if sign change between lo and mid
                     xhi=xnew;
                     yhi=ynew;
                  } else { // otherwise sign change between hi and mid
                     xlo=xnew;
                     ylo=ynew;
                  }
                  if (xlo > xhi) {
                     throw new InternalErrorException (
                        "interval exchanged, xlo=" + xlo + " xhi=" + xhi);
                  }
                  
                  if (useNewtonsMethod) {
                     double xx = 0;
                     if (dnew != 0) {
                        xx = xnew - ynew/dnew;
                     }
                     // if Newton's method answer not within the interval,
                     // bisect instead
                     if (xx <= xlo || xx >= xhi) {
                        bisectionCount++;
                        xnew = (xlo+xhi)/2;
                     }
                     else {
                        xnew = xx;
                     }
                  }
                  else {
                     if ((iteration%2) != 0) {
                        // sometimes go with bisection to guarantee progress
                        alpha=0.5; 
                     }
                     else {
                        // other times go with secant to hopefully get there fast
                        alpha=yhi/(yhi-ylo); 
                     }
                     xnew = alpha*xlo+(1-alpha)*xhi;
                  }
                  if (xhi-xlo < xtol) {
                     //System.out.println ("xhi-xlo=" + (xhi-xlo) + " tol=" + xtol);
                     break;                     
                  }
               }
               iterationCount += iteration;
               roots[nrts++] = xnew;
            }
         }
         x = xnext;
         y = ynext;
      }
      if (ynext == 0) {
         roots[nrts++] = xnext;
      }
      return nrts;
   }

}
