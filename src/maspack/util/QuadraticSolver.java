package maspack.util;

/**
 * Finds the real roots of a quadratic equation, attempting to handle numerical
 * issues as best as possible.
 */
public class QuadraticSolver {

   /**
    * Find the real roots of the quadratic equation
    * <pre>
    *    a x^2 + b x + c = 0
    * </pre>
    * There will be either 0, 1, or 2 roots, where the single
    * root case occurs when a = 0 or when there is a repeated root.
    * When there are two roots, they are sorted in increasing order.
    *
    * @param roots returns the roots, must have length {@code >=} 2
    * @param a first coefficient
    * @param b second coefficient
    * @param c third coefficient
    * @return number of roots found
    */
   public static int getRoots (
      double[] roots, double a, double b, double c) {

      if (a == 0) {
         // solve b x + c = 0 instead
         if (b == 0) {
            return 0;
         }
         else {
            roots[0] = -c/b;
            return 1;
         }
      }
      else {
         double disc = b*b - 4*a*c;
         if (disc < 0) {
            return 0;
         }
         else if (disc == 0) {
            roots[0] = -b/(2*a);
            return 1;
         }
         else {
            double droot = Math.sqrt(disc);
            double r0, r1;
            if (b >= 0) {
               r0 = (-b-droot)/(2*a);
               r1 = (2*c)/(-b-droot);
            }
            else {
               r0 = (2*c)/(-b+droot);
               r1 = (-b+droot)/(2*a);
            }
            if (a > 0) {
               roots[0] = r0;
               roots[1] = r1;
            }
            else {
               roots[0] = r1;
               roots[1] = r0;
            }
            return 2;            
         }
      }
   }

   /**
    * Find the real roots of the quadratic equation
    * <pre>
    *    a x^2 + b x + c = 0
    * </pre>
    * that lie within the interval [xlo, xhi].
    * There will be either 0, 1, or 2 roots, where the single
    * root case occurs when a = 0 or when there is a repeated root.
    * When there are two roots, they are sorted in increasing order.
    *
    * @param roots returns the roots, must have length {@code >=} 2
    * @param a first coefficient
    * @param b second coefficient
    * @param c third coefficient
    * @param xlo lower bound on x
    * @param xhi upper bound on x
    * @return number of roots found
    */
   public static int getRoots (
      double[] roots, double a, double b, double c, double xlo, double xhi) {

      int numr = 0;

      if (a == 0) {
         // solve b x + c = 0 instead
         if (b != 0) {
            double r0 = -c/b;
            if (r0 >= xlo && r0 <= xhi) {
               roots[numr++] = r0;
            }
         }
      }
      else {
         double disc = b*b - 4*a*c;
         if (disc == 0) {
            double r0 = -b/(2*a);
            if (r0 >= xlo && r0 <= xhi) {
               roots[numr++] = r0;
            }
         }
         else if (disc > 0) {
            double droot = Math.sqrt(disc);
            double r0, r1;
            if (b >= 0) {
               r0 = (-b-droot)/(2*a);
               r1 = (2*c)/(-b-droot);
            }
            else {
               r0 = (2*c)/(-b+droot);
               r1 = (-b+droot)/(2*a);
            }
            if (a > 0) {
               if (r0 >= xlo && r0 <= xhi) {
                  roots[numr++] = r0;
               }
               if (r1 >= xlo && r1 <= xhi) {
                  roots[numr++] = r1;
               }
            }
            else {
               if (r1 >= xlo && r1 <= xhi) {
                  roots[numr++] = r1;
               }
               if (r0 >= xlo && r0 <= xhi) {
                  roots[numr++] = r0;
               }
            }
         }
      }
      return numr;
   }

}

