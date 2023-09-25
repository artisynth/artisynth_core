package maspack.numerics;

import maspack.util.*;
import maspack.matrix.*;

/**
 * Fits a polynomial of specified degree to a set of time data.
 */
public class PolynomialFit {

   int myDegree;
   int myNumY;
   double myXhi;
   double myXlo;
   QRDecomposition myQR;

   public PolynomialFit () {
   }

   public PolynomialFit (int degree, int numy, double xlo, double xhi) {
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
      myDegree = degree;
      myNumY = numy;
      MatrixNd A = new MatrixNd (numy, degree+1);
      for (int i=0; i<numy; i++) {
         double x = xlo + i*(xhi-xlo)/(numy-1.0);
         double pow = 1;
         for (int j=0; j<degree+1; j++) {
            A.set (i, degree-j, pow);
            pow *= x;
         }
      }
      myQR = new QRDecomposition();
      myQR.factor (A);
      myDegree = degree;
      myNumY = numy;
      myXhi = xhi;
      myXlo = xlo;
   }

   public void fit (VectorNd coefs, double[] yv) {
      if (myQR == null) {
         throw new IllegalStateException (
            "PolynomialFit is not initialized");
      }
      if (yv.length < myNumY) {
         throw new IllegalArgumentException (
            "length of yv is less then specified numy ("+myNumY+")");
      }
      coefs.setSize (myDegree+1);
      VectorNd b = new VectorNd(yv);
      b.setSize (myNumY);
      myQR.solve (coefs, b);
   }

   public VectorNd getSavitzkyGolayWeights (double s) {
      VectorNd uvec = new VectorNd (myDegree+1);
      VectorNd weights = new VectorNd(myNumY);
      double pow = 1;
      double x = myXlo + s*(myXhi-myXlo);
      for (int j=0; j<myDegree+1; j++) {
         uvec.set (myDegree-j, pow);
         pow *= x;
      }
      myQR.leftSolve (weights, uvec);
      return weights;
   }

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
      PolynomialFit fit = new PolynomialFit (3, 5, 0.0, 1.0);
      for (int i=0; i<5; i++) {
         double s = i/4.0;
         VectorNd wgts = fit.getSavitzkyGolayWeights (s);
         System.out.println (
            "SavitzkyGolay 3/5 "+s+" =" + wgts.toString ("%12.8f"));
      }
   }

}
