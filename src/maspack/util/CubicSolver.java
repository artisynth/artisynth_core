/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Finds the real roots of a cubic equation on a specified interval. Adopted
 * from utility code provided by Robert Bridson's graphics group.
 *
 * Also, a good guide to the structure of cubics and their solution is the
 * paper "A new approach to solving the cubic: Cardan's solution revealed", by
 * RWD Nickalls.
 */
public class CubicSolver {

   public static double RTOL = 1e-10;
   public static double PREC = 1e-16;

   static int iterationCount;
   static int bisectionCount;
   static boolean useNewtonsMethod = true;

   public static int getRoots (
      double[] roots, double a, double b, double c, double d) {

      return getRoots (roots, a, b, c, d, 0, 1);
   }

   public static int getRoots (
      double[] roots, double a, double b, double c, double d,
      double x0, double x1) {
      
      int icnt = 0;
      double[] interval_x = new double[4];

      if (x0 > x1) {
         // sanity check; swap
         double tmp = x0; x0 = x1; x1 = tmp;
      }

      double xn = -b/3; // xn is the inflexion point

      double disc = b*b-3*a*c; // discriminant of derivative, divided by 4
      if (disc <= 0.0) {
         // polynominal is monotone, so only one root
         // check the root at the inflexion point and bracket accordingly

         double y0 = ((x0*a+b)*x0+c)*x0 + d;
         double y1 = ((x1*a+b)*x1+c)*x1 + d;

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
            if (xn <= x0 || xn >= x1) {
               // xn is outside of (x0,x1)
               interval_x[icnt++] = x0;
               interval_x[icnt++] = x1;
            }
            else {
               double yn = ((xn*a+b)*xn+c)*xn + d;

               if (yn == 0) {
                  // root at xn
                  roots[0] = xn;
                  return 1;
               }
               else if (y1*yn > 0) {
                  // root must lie in (x0,xn)
                  interval_x[icnt++] = x0;
                  interval_x[icnt++] = xn;
               }
               else {
                  // root must lie in (xn,x1)
                  interval_x[icnt++] = xn;
                  interval_x[icnt++] = x1;
               }
            }
         }
      }
      else {
         if (a == 0) {
            // cubic is just a quadratic, b x^2 + c x + d,
            // so solve the roots of that
            if (b == 0) {
               // cubic is actually just linear, solve solve roots of *that*
               if (c == 0) {
                  return 0;
               }
               else {
                  double r = -d/c;
                  if (r < x0 || r > x1) {
                     return 0;
                  }
                  else {
                     roots[0] = r;
                     return 1;
                  }
               }
            }
            else {
               disc = c*c-4*b*d; // discriminant of quadratic
               if (disc == 0) {
                  roots[0] = -0.5*c/b;
                  return 1;
               }
               else if (disc < 0) {
                  return 0;
               }
               else {
                  double q;
                  if (c >= 0) {
                     q = -0.5*(c+Math.sqrt(disc));
                  }
                  else {
                     q = -0.5*(c-Math.sqrt(disc));
                  }
                  double r0 = q/b;
                  double r1 = d/q;
                  if (r0 > r1) {
                     // swap
                     double tmp = r0; r0 = r1; r1 = tmp;
                  }
                  else if (r0 == r1) {
                     // invalidate r1 so we will only return at most one root
                     // if r0 is inside the interval
                     r1 = x1+1; 
                  }
                  int numr = 0;
                  if (r0 >= x0 && r0 <= x1) {
                     roots[numr++] = r0;
                  }
                  if (r1 >= x0 && r1 <= x1) {
                     roots[numr++] = r1;
                  }
                  return numr;
               }
            }
         }
         else {
            // divide cubic into monotone regions
            double q;
            if (b >= 0) {
               q = -b-Math.sqrt(disc);
            }
            else {
               q = -b+Math.sqrt(disc);
            }
            double r0 = q/(3*a);
            double r1 = c/q;
            if (r0 > r1) {
               // swap r0 and r1
               double tmp = r0; r0 = r1; r1 = tmp;
            }
            interval_x[icnt++] = x0;
            if (r0 > x0 && r0 < x1) {
               interval_x[icnt++] = r0;
            }
            if (r1 > x0 && r1 < x1) {
               interval_x[icnt++] = r1;
            }
            interval_x[icnt++] = x1;
         }
      }

      // look for roots in each of the indicated intervals
      int nrts = 0;

      double xtol = RTOL*Math.abs (x1-x0);
      double xmax = Math.max (Math.abs (x0), Math.abs (x1));
      double ytol =
         PREC*(((Math.abs(a)*xmax+Math.abs(b))*xmax+
                Math.abs(c))*xmax+Math.abs(d));
      
      double x = interval_x[0];
      double y = ((a*x+b)*x+c)*x + d;
      double xnext = 0;
      double ynext = 0;

      for (int i=0; i<icnt-1; i++) {
         xnext = interval_x[i+1];
         ynext = ((a*xnext+b)*xnext+c)*xnext + d;
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
                  double ynew = ((a*xnew+b)*xnew + c)*xnew + d;
                  double dnew = (3*a*xnew+2*b)*xnew + c;

                  ytol = Math.max (dnew, PREC)*xtol;

                  //System.out.printf (" ymi=%18.15e\n", ynew);
                  //if(Math.abs(ynew)<1e-2*convergence_tol) break;
                  if (Math.abs(ynew) < ytol) {
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

   public static double cuberoot (double x) {
      return Math.pow (x, 1/3.0);
   }

   public static int getRootsAlgebraic (
      double[] roots, double a, double b, double c, double d) {
 
      double A = b*b - 3*a*c;
      double B = 2*b*b*b - 9*a*b*c + 27*d*a*a;

      double Acub = A*A*A;
      double D = 4*Acub - B*B;

      if (D >= 0) {
         // there are three real roots, though some may be repeated

         if (A == 0) {
            // then there is one real root with multiplicity 3
            roots[0] = -b/(3*a);
            return 1;
         }

         double cos = 0.5*B/Math.sqrt(Acub);
         if (cos > 1) {
            cos = 1;
         }
         else if (cos < -1) {
            cos = -1;
         }
         double ang = Math.acos (0.5*B/Math.sqrt(Acub));         

         double sqrRootA = Math.sqrt(A);
         // this calculation should give the three roots in ascending order
         double r0 = -2*sqrRootA * Math.cos(ang/3) - b;
         double r1 = -2*sqrRootA * Math.cos((ang-2*Math.PI)/3) - b;
         double r2 = -2*sqrRootA * Math.cos((ang+2*Math.PI)/3) - b;

         if (cos == 1) {
            // then roots[1] == roots[2] 
            roots[0] = r0/(3*a);
            roots[1] = r1/(3*a);
            return 2;
         }
         else if (cos == -1) {
            // then roots[0] == roots[1] 
            roots[0] = r0/(3*a);
            roots[1] = r2/(3*a);
            return 2;
         }
         else {
            roots[0] = r0/(3*a);
            roots[1] = r1/(3*a);
            roots[2] = r2/(3*a);
            return 3;
         }
      }
      else {
         double C, r;
         if (a*B >= 0) {
            // sign(R) >= 0
            C = cuberoot ((Math.sqrt(-D) + B)/2.0);
            r = -C - A/C - b;
         }
         else {
            // sign(R) < 0
            C = cuberoot ((Math.sqrt(-D) - B)/2.0);
            r = C + A/C - b;
         }
         roots[0] = r/(3*a);
         return 1;
      }
   }     


   public static int getRootsAlgebraic (
      double[] roots, double a, double b, double c, double d,
      double x0, double x1) {

      int nroots = getRootsAlgebraic (roots, a, b, c, d);

      int k = 0;
      for (int i=0; i<nroots; i++) {
         double r = roots[i];
         if (r >= x0 && r <= x1) {
            if (k < i) {
               roots[k] = r;
            }
            k++;
         }
      }
      return k;
   }

}
