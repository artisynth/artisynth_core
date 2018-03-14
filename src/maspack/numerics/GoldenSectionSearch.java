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
   public static double minimize(Function1x1 f, double a, double b, double tol, double ftol) {

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
   
}
