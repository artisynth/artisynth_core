package maspack.numerics;

import maspack.function.Function1x1;

public class GoldenSectionSearch {

   /**
    * Performs a Golden Section Search to find the minimum of a function f
    * in the range [a, b]
    * 
    * @param f function to minimize
    * @param a lower bound of minimizer
    * @param b upper bound of minimizer
    * @param tol   tolerance for answer (
    * iterations will stop if b-a {@code <} tol)
    * @param ftol  function tolerance 
    *   (iterations will stop if f(c) doesn't seem to vary outside this range within bounds)
    * @return minimizing input c
    */
   public static double minimize (
      Function1x1 f, double a, double b, double tol, double ftol) {

      final double g = (Math.sqrt(5)+1)/2; 
      
      double c = b - (b-a)/g;
      double fc = f.eval(c);
      double d = a + (b-a) / g;
      double fd = f.eval(d);
      
      double s = c;
      double fs = fc;
      if (fd < fs) {
         s = d;
         fs = fd;
      }
      
      while ((d-c) > tol && fs > ftol) {
         if (fc < fd) {
            b = d;
         } else {
            a = c;
         }
         
         c = b - (b-a)/g;
         fc = f.eval(c);
         d = a + (b-a) / g;
         fd = f.eval(d);
         
         s = c;
         fs = fc;
         if (fd < fs) {
            s = d;
            fs = fd;
         }
      }
      
      return s;
   }

   /**
    * Implementation of a modified Golden section search for minimizing |f(s)|, 
    * guaranteeing that the final call to func is at the returned minimum 
    * 
    * @param a   left-side of search interval
    * @param fa  f(a)
    * @param b   right-side of search interval
    * @param fb  f(b)
    * @param eps   tolerance for interval [a,b]
    * @param feps
    * tolerance for function evaluation, {@code |f(s)| <} feps considered a root
    * @param func  function to evaluate
    * @return function minimizer
    */
   public static double minimize (
      Function1x1 func, double a, double fa, double b, double fb, 
      double eps, double feps) {
      
      // absolute values
      double afa = Math.abs(fa);
      double afb = Math.abs(fb);
      
      // initial solution guess as minimum of end-points
      double s;
      double fs;
      double afs;
      if (afa <= afb) {
         s = a;
         fs = fa;
         afs = afa;
      } else {
         s = b;
         fs = fb;
         afs = afb;
      }
      
      // intermediate values
      double c, d, fc, fd, afc, afd;
      final double g = 1.61803398875;   // golden ratio
      
      boolean callNeeded = true;        // whether we need to make a final call to the function 
      
      while ( b-a > eps && afs > feps) {
         if (fa*fb <= 0) {
            // return brentRootFinder (a, fa, b, fb, eps, feps, func);
            return BrentRootSolver.findRoot (func, a, fa, b, fb, eps, feps);
         }
         
         c = b + (a-b)/g;

         fc = func.eval(c);
         afc = Math.abs(fc);
         
         if ( fc*fa < 0) {
            //return brentRootFinder(a, fa, c, fc, eps, feps, func);
            return BrentRootSolver.findRoot (func, a, fa, c, fc, eps, feps);
         } else if (afc >= afa) {
            if (afa > afb) {
               // start again in range [c,b]
               a = c;
               fa = fc;
               afa = afc;
            } else {
               // start again in range [a,c]
               b = c;
               fb = fc;
               afb = afc;
            }
            callNeeded = true;  // c is not minimum
         } else if (afc >= afb) {
            // start again in range [c, b]
            a = c;
            fa = fc;
            afa = afc;
            callNeeded = true;  // c is not minimum
         } else {
   
            // fc guaranteed to be below both fa & fb
            d = a + (b-a)/g;
            fd = func.eval(d);
            afd = Math.abs(fd);
            
            if ( fc * fd < 0 ) {
               //return brentRootFinder(a, fa, c, fc, eps, feps, func);
               return BrentRootSolver.findRoot (func, a, fa, c, fc, eps, feps);
            } else if ( afc <= afd ) {
               // c below a and d
               b = d; 
               fb = fd;
               afb = afd;
               s = c;
               fs = fc;
               afs = afc;
               callNeeded = true;  // will need to re-call at c
            } else {
               // d below c and b
               a = c;
               fa = fc;
               afa = afc;
               s = d;
               fs = fd;
               afs = afd;
               callNeeded = false;  // d is minimum
            }
         }
      }
      // final function call, if needed
      if (callNeeded) {
         fs = func.eval(s);
      }
      
      return s;
   }   
   
}
