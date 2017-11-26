package maspack.numerics;

import maspack.util.*;
import maspack.matrix.*;

/**
 * Fits a polynomial of specified degree to a set of time data.
 */
public class PolynomialFit {

   private final double sqr (double x) {
      return x*x;
   }         

   public void fit (
      VectorNd coefs, VectorNd yv, double xlo, double xhi, int degree) {

      fit (coefs, yv.getBuffer(), yv.size(), xlo, xhi, degree);
   }

   public void fit (
      VectorNd coefs, double[] yv, int numy, double xlo, double xhi, int degree) {

      if (yv.length < numy) {
         throw new IllegalArgumentException (
            "length of yv is less that numy ("+numy+")");
      }
      if (degree < 1) {
         throw new IllegalArgumentException ("degree must be at least 1");
      }
      if (numy < degree+1) {
         throw new IllegalArgumentException (
            "number of data points must be >= degree+1 ("+(degree+1)+")");
      }
      if (xlo == xhi) {
         throw new IllegalArgumentException (
            "xlo equals xhi ("+xhi+")");
      }
      coefs.setSize (degree+1);
      MatrixNd A = new MatrixNd (numy, degree+1);
      VectorNd b = new VectorNd(yv);
      for (int i=0; i<numy; i++) {
         double x = xlo + i*(xhi-xlo)/(numy-1.0);
         double pow = 1;
         for (int j=0; j<degree+1; j++) {
            A.set (i, degree-j, pow);
            pow *= x;
         }
      }
      QRDecomposition QR = new QRDecomposition();
      QR.factor (A);
      QR.solve (coefs, b);
   }

   public double evalPoly (VectorNd coefs, double x) {
      double val = coefs.get(0);
      for (int j=1; j<coefs.size(); j++) {
         val = val*x + coefs.get(j);
      }
      return val;
   }

   public double variance (
      VectorNd coefs, double[] yv, int numy, double xlo, double xhi) {

      double var = 0;
      for (int i=0; i<numy; i++) {
         double x = xlo + i*(xhi-xlo)/(numy-1.0);
         double y = evalPoly (coefs, x);
         var += sqr(y-yv[i]);
      }
      return var/numy;
   }

   public double variance (
      VectorNd coefs, VectorNd yv, double xlo, double xhi) {

      return variance (coefs, yv.getBuffer(), yv.size(), xlo, xhi);
   }

   public double standardDeviation (
      VectorNd coefs, double[] yv, int numy, double xlo, double xhi) {

      if (numy <= 1) {
         throw new IllegalArgumentException (
            "numy must be > 1");
      }
      double std = 0;
      for (int i=0; i<numy; i++) {
         double x = xlo + i*(xhi-xlo)/(numy-1.0);
         double y = evalPoly (coefs, x);
         std += sqr(y-yv[i]);
      }
      return Math.sqrt(std/(numy-1));
   }

   public double standardDeviation (
      VectorNd coefs, VectorNd yv, double xlo, double xhi) {

      return standardDeviation (coefs, yv.getBuffer(), yv.size(), xlo, xhi);
   }

   public double maximumDeviation (
      VectorNd coefs, double[] yv, int numy, double xlo, double xhi) {

      double max = 0;
      for (int i=0; i<numy; i++) {
         double x = xlo + i*(xhi-xlo)/(numy-1.0);
         double y = evalPoly (coefs, x);
         double dev = Math.abs(y-yv[i]);
         if (dev > max) {
            max = dev;
         }
      }
      return max;
   }

   public double maximumDeviation (
      VectorNd coefs, VectorNd yv, double xlo, double xhi) {

      return maximumDeviation (coefs, yv.getBuffer(), yv.size(), xlo, xhi);
   }

   public static void main (String[] args) {
   }

}
