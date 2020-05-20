/**
 * Copyright (c) 2020, by the Authors: Fabien Pean
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.Arrays;

public class SplineBasis {
   
   static public double computeGrevilleAbscissae(int controlPointIndex, int p, double... knots) {
      double GrevilleAbscissae = 0;
      for(int i = 1; i <= p; i++) {
         GrevilleAbscissae += knots[controlPointIndex + i];
      }
      GrevilleAbscissae /= p;
      return GrevilleAbscissae;
   }
   
   static public SplineBasis generateSplineBasisUniformOpen(int p, int n_cp) {
      return new SplineBasis(p,generateKnotVectorUniform(p, n_cp, true));
   }
   
   static public double[] generateKnotVectorUniformOpen(int p, int n_cp) {
      return generateKnotVectorUniform(p, n_cp, true);
   }
   
   static public double[] generateKnotVectorUniformClose(int p, int n_cp) {
      return generateKnotVectorUniform(p, n_cp, false);
   }
   
   static public double[] generateKnotVectorUniform(int p, int n, boolean open) {
      double[] knots = new double[n+p+1];
      int N = n+p+1;
      int sta = open?p:0;
      int end = open?n:N;
      
      for(int i=0; i<sta; ++i) {
         knots[i]=0;
      }
      for(int i=sta; i<end; ++i) {
         knots[i]=(double)(i-p)/(n-p);
      }
      for(int i=N-1; i>=end; --i) {
         knots[i]=1;
      }
      return knots;
   }

   static int findKnotSpan(double u, double[] knotVector, int p) {
      int m = knotVector.length - 1;
      int upper = m - p;
      int lower = p;
      int mid = (int)(m / 2);
      do {
         double knot = knotVector[mid];

         if(u < knot) {
            upper = mid;
            mid = (int)(mid * 0.5);
         }
         else {
            lower = mid;
            mid = (int)(mid + (upper - lower) / 2);
         }
      }
      while((upper - lower) > 1);
      return lower;
   }

   static boolean isOpen(double[] knotVector, int p) {
      int m = knotVector.length - 1;
      if(knotVector[0] == knotVector[p] && knotVector[m] == knotVector[m - p])
         return true;
      else
         return false;
   }

   static double[] computeBasisFunction(double x, SplineBasis u) {
      return computeBasisFunction(x, u.getKnotVector(), u.getDegree());
   }

   static double[] computeBasisFunction(double x, double[] knotVector, int degree) {
      int p = degree;
      int numberNonZeroBasis = p + 1;
      double[] basisValues = new double[numberNonZeroBasis];
      basisValues[0] = 1.;
      int knotSpanIndex = findKnotSpan(x, knotVector, p);

      // Initialize auxiliary variables
      double saved = 0.0;
      double temp = 0.0;

      // Initialize auxiliary arrays
      double[] left = new double[numberNonZeroBasis];
      double[] right = new double[numberNonZeroBasis];

      for(int j = 1; j <= p; j++) {
         left[j - 1] = x - knotVector[knotSpanIndex + 1 - j];
         right[j - 1] = knotVector[knotSpanIndex + j] - x;
         saved = 0.0;
         for(int r = 0; r < j; r++) {
            temp = basisValues[r] / (right[r] + left[j - r - 1]);
            basisValues[r] = saved + right[r] * temp;
            saved = left[j - r - 1] * temp;
         }
         basisValues[j] = saved;
      }
      return basisValues;
   }

   static double[] computeBasisFunctionGradient(double x, SplineBasis u) {
      return computeBasisFunctionGradient(x, u.getKnotVector(), u.getDegree());
   }

   static double[] computeBasisFunctionGradient(double parametricCoordinate, double[] knotVector, int p) {
      int ks = findKnotSpan(parametricCoordinate, knotVector, p);
      double[] basisDerivatives = computeBasisFunction(parametricCoordinate, Arrays.copyOfRange(knotVector, 1, knotVector.length-1), p - 1);

      int numberBasis = p + 1;
      int numberDerivatives = p - 1 + 1;
      double[] basisValues = new double[numberBasis];

      basisValues[0] = (double)-1. * p * basisDerivatives[0] / (knotVector[ks + 1] - knotVector[ks - p + 1]);
      for(int i = 1; i < numberDerivatives; i++) {
         basisValues[i] =
            (double)p * (basisDerivatives[i - 1] / (knotVector[ks + i] - knotVector[ks + i - p])
            - basisDerivatives[i] / (knotVector[ks + i + 1] - knotVector[ks + i - p + 1]));
      }
      basisValues[basisValues.length - 1] =
         (double)p * (double)basisDerivatives[basisDerivatives.length - 1]
         / (knotVector[ks + numberDerivatives] - knotVector[ks + numberDerivatives - p]);

      return basisValues;
   }
   
   static double[] computeBasisFunctionHessian(double parametricCoordinate, double[] knotVector, int p) {
      if(p==1) return new double[2];
      
      int ks = findKnotSpan(parametricCoordinate, knotVector, p);
      double[] basisDerivatives = computeBasisFunction(parametricCoordinate, Arrays.copyOfRange(knotVector, 2, knotVector.length-2), p - 2);

      int numberBasis = p + 1;
      int numberDerivatives = p - 2 + 1;
      double[] basisValues = new double[numberBasis];

      basisValues[0] = (double)-1. * p * basisDerivatives[0] / (knotVector[ks + 1] - knotVector[ks - p + 1]);
      for(int i = 1; i < numberDerivatives; i++) {
         basisValues[i] =
            (double)p * (basisDerivatives[i - 1] / (knotVector[ks + i] - knotVector[ks + i - p])
            - basisDerivatives[i] / (knotVector[ks + i + 1] - knotVector[ks + i - p + 1]));
      }
      basisValues[basisValues.length - 1] =
         (double)p * (double)basisDerivatives[basisDerivatives.length - 1]
         / (knotVector[ks + numberDerivatives] - knotVector[ks + numberDerivatives - p]);

      return basisValues;
   }

   static int getNumberZeroKnotSpan(double[] knotVector) {
      int zeroKnotSpan = 0;
      for(int i = 0; i < knotVector.length - 1; i++) {
         if(knotVector[i + 1] == knotVector[i])
            zeroKnotSpan++;
      }
      return zeroKnotSpan;
   }

   public SplineBasis(int degree, double... knotVector) {
      p = degree;
      this.knotVector = knotVector;
      dirty = true;
      updateProperties();
   }
   
   public SplineBasis(SplineBasis s) {
      p = s.p;
      this.knotVector = s.knotVector.clone();
      dirty = true;
      updateProperties();
   }

   void setBasis(int p, double[] knotVector) {
      this.p = p;
      this.knotVector = knotVector;
      dirty = true;
      updateProperties();
   }

   void setBasis(double[] knotVector, int p) {
      this.p = p;
      this.knotVector = knotVector;
      dirty = true;
      updateProperties();
   }

   public double[] computeBasisFunction(double x) {
      return computeBasisFunction(x, knotVector, p);
   }

   public double[] computeBasisFunctionGradient(double x) {
      return computeBasisFunctionGradient(x, knotVector, p);
   }
   
   public double[] computeBasisFunctionHessian(double x) {
      return computeBasisFunctionHessian(x, knotVector, p);
   }

   public int getDegree() {
      return p;
   }

   void setDegree(int p) {
      this.p = p;
      dirty = true;
   }

   double[] getKnotVector() {
      return knotVector;
   }

   public int getKnotVectorSize() {
      return knotVector.length;
   }

   void setKnotVector(double[] knotVector) {
      this.knotVector = knotVector;
      dirty = true;
   }
   
   double getFirst() {
      return knotVector[0];
   }
   
   double getFirstKnotValid() {
      return knotVector[p];
   }

   double get(int i) {
      return knotVector[i];
   }
   
   double getLast() {
      return knotVector[knotVector.length-1];
   }
   
   double getLastKnotValid() {
      return knotVector[knotVector.length-p-1];
   }

   double set(int i, double u) {
      dirty = true;
      return knotVector[i] = u;
   }

   int findKnotSpan(double u) {
      int m = knotVector.length - 1;
      int upper = m - p;
      int lower = p ;
      int mid = (int)(m / 2);
      do {
         double knot = knotVector[mid];
         if(u < knot) {
            upper = mid;
            mid = (int)(mid * 0.5);
         }
         else {
            lower = mid;
            mid = (int)(mid + (upper - lower) / 2);
         }
      }
      while((upper - lower) > 1);
      return lower;
   }

   void normalizeKnotVector() {
      if(isNormalized()) {
         return;
      }
      translate = getFirstKnotValid();
      if(translate != 0)
         for(int i = 0; i < knotVector.length; i++)
            knotVector[i] -= translate;
      scale = getLastKnotValid() - getFirstKnotValid();
      if(scale != 1)
         for(int i = 0; i < knotVector.length; i++)
            knotVector[i] /= scale;
   }
   
   public double unnormalize(double u) {
      u*=scale;
      u+=translate;
      return u;
   }
   
   public double normalize(double u) {
      u-=translate;
      u/=scale;
      return u;
   }

   public int getNumberNonZeroBasisPerKnotSpan() {
      return p + 1;
   }
   
   public int getNumberBasis() {
      return knotVector.length - p - 1;
   }

   int getNumberNonZeroKnotSpan() {
      return numberNonZeroKnotSpan;
   }
   
   int getNumberZeroKnotSpan() {
      int zeroKnotSpan = 0;
      for(int i = 0; i < knotVector.length - 1; i++) {
         if(knotVector[i + 1] == knotVector[i])
            zeroKnotSpan++;
      }
      return zeroKnotSpan;
   }
   
   public int getNumberZeroKnotSpanInRange(int ku) {
      int zeroKnotSpan = 0;
      for(int i = ku-p; i < ku + p + 1; i++) {
         if(knotVector[i + 1] == knotVector[i])
            zeroKnotSpan++;
      }
      return zeroKnotSpan;
   }
   
   public int getNumberZeroKnotSpanInRange(double u) {
      int ku = findKnotSpan(u);
      return getNumberZeroKnotSpanInRange(ku);
   }

   boolean isNormalized() {
      return (knotVector[0] == 0) && (knotVector[knotVector.length - 1] == 1);
   }
   
   boolean isOpenRight() {
      return openRight;
   }
   
   boolean isOpenLeft() {
      return openLeft;
   }

   boolean isOpen() {
      return openRight || openLeft;
   }

   boolean isUniform() {
      return uniform;
   }

   boolean checkOpen() {
      int m = knotVector.length - 1;
      if(knotVector[0] == knotVector[p])
         openLeft = true;
      else
         openLeft = false;
      if(knotVector[m] == knotVector[m - p])
         openRight = true;
      else
         openRight = false;
      return isOpen();
   }

   boolean checkUniform() {
      checkOpen();
      int m = knotVector.length - 1;
      double interval = knotVector[p + 1] - knotVector[p];
      int begin = openLeft ? p : 0;
      int end = openRight ? m - p : m + 1;
      for(int i = begin; i < end-1; ++i) {
         if(knotVector[i + 1] - knotVector[i] != interval)
            return (uniform = false);
      }
      return (uniform = true);
   }

   int checkNumberNonZeroKnotSpan() {
      numberNonZeroKnotSpan = 0;
      for(int i = p; i < knotVector.length - p - 1; i++) {
         if(knotVector[i + 1] != knotVector[i])
            numberNonZeroKnotSpan++;
      }
      return numberNonZeroKnotSpan;
   }

   void updateProperties() {
      if(dirty) {
         // First isOpen and then isUniform, order matters.
         checkOpen();
         checkUniform();
         checkNumberNonZeroKnotSpan();
         normalizeKnotVector();
         dirty = false;
      }
   }

   double computeGrevilleAbscissae(int controlPointIndex) {
      double GrevilleAbscissae = 0;
      for(int i = 1; i <= p; i++) {
         GrevilleAbscissae += knotVector[controlPointIndex + i];
      }
      GrevilleAbscissae /= p;
      return GrevilleAbscissae;
   }
   
   public double clamp(double u) {
      u=Math.max(u, getFirstKnotValid());
      u=Math.min(u, getLastKnotValid());
      return u;
   }

   private int p;

   private double[] knotVector;

   private int numberNonZeroKnotSpan;

   private Boolean openRight, openLeft;

   private Boolean uniform;

   private Boolean dirty;
   
   private double translate = 0;
   
   private double scale = 1;

   public static void main(String[] args) {
   {
         SplineBasis u = new SplineBasis(2, new double[] { 0, 0, 0, 0.25, 0.5, 0.75, 1, 1, 1 });
         System.out
            .println("Basis u contains degree" + u.getDegree() + " and knot vector " + Arrays.toString(u.getKnotVector()));
         int ks;
         ks = u.findKnotSpan(0.3);
         System.out.println("Knot span index for coord 0.3 is " + ks + " from expected 3");
         int nks = u.getNumberNonZeroKnotSpan();
         System.out.println("Number of knot spans is " + nks + " from expected 4");
         int nnz = u.getNumberNonZeroBasisPerKnotSpan();
         System.out.println("Number of knot spans is " + nnz + " from expected 3");
         System.out.println("Knot span index for coord 0.3 is " + ks + " from expected 3");
         double greville;
         greville = u.computeGrevilleAbscissae(0);
         System.out.println("Greville coordinates for index 0 is " + greville + " from expected 0");
         greville = u.computeGrevilleAbscissae(1);
         System.out.println("Greville coordinates for index 1 is " + greville + " from expected 0.125");
      }
      {
         SplineBasis u = new SplineBasis(3, new double[] { 0, 0, 0, 0, 0.5, 1, 1, 1, 1 });
         System.out
            .println("Basis u contains degree" + u.getDegree() + " and knot vector " + Arrays.toString(u.getKnotVector()));
         int ks;
         ks = u.findKnotSpan(0.3);
         System.out.println("Knot span index for coord 0.3 is " + ks + " from expected 3");
         int nks = u.getNumberNonZeroKnotSpan();
         System.out.println("Number of knot spans is " + nks + " from expected 2");
         int nnz = u.getNumberNonZeroBasisPerKnotSpan();
         System.out.println("Number of basis is " + nnz + " from expected 4");
         double greville;
         greville = u.computeGrevilleAbscissae(0);
         System.out.println("Greville coordinates for index 0 is " + greville + " from expected 0");
         greville = u.computeGrevilleAbscissae(1);
         System.out.println("Greville coordinates for index 1 is " + greville + " from expected 0.125");
         
         double[] N = u.computeBasisFunction(0.75);
         System.out.println("Basis function at 0.75 are [" + Arrays.toString(N) + "] from expected ~[0.04 0.25 0.59 0.14]");
         N = u.computeBasisFunction(0.5);
         System.out.println("Basis function at 0.5 are [" + Arrays.toString(N) + "] from expected [0.25 0.5 0.25 0.0]");
         N = u.computeBasisFunction(0);
         System.out.println("Basis function at 0.0 are [" + Arrays.toString(N) + "] from expected [1 0 0 0]");
         N = u.computeBasisFunctionGradient(0.5);
         System.out.println("Gradient basis at 0.5 are [" + Arrays.toString(N) + "] from expected [-1.5 0 1.5 0]");
         N = u.computeBasisFunctionGradient(1.0);
         System.out.println("Gradient basis at 1.0 are [" + Arrays.toString(N) + "] from expected [0 0 -6 6]");
      }
   }
}
