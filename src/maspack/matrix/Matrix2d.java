/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

/**
 * A general 3 x 3 matrix with the elements stored as explicit fields.
 */
public class Matrix2d extends Matrix2dBase implements VectorObject<Matrix2d> {
   
   /**
    * Global identity matrix. Should not be modified.
    */
   public static final Matrix2d IDENTITY =
      new Matrix2d (new double[] { 1, 0, 0, 1 });

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix2d ZERO = new Matrix2d();

   /**
    * Creates a matrix and initializes it to zero.
    */
   public Matrix2d() {
   }

   /**
    * Creates a matrix and initializes to provided values
    * @param m00 top-left
    * @param m01 top-right
    * @param m10 bottom-left
    * @param m11 bottom-right
    */
   public Matrix2d(double m00, double m01, double m10, double m11) {
      set(m00, m01, m10, m11);
   }
   
   /**
    * Creates a matrix and initializes its elements from an array of values.
    * 
    * @param vals
    * element values for the matrix, with element (i,j) stored at location
    * <code>i*2+j</code>
    */
   public Matrix2d (double[] vals) {
      set (vals);
   }

   /**
    * Creates a matrix and initializes its elements to those of the matrix M.
    * 
    * @param M
    * matrix object to be copied.
    */
   public Matrix2d (Matrix M) {
      set (M);
   }

   /**
    * Creates a matrix and initializes its elements to those of the matrix M.
    * 
    * @param M
    * matrix object to be copied.
    */
   public Matrix2d (Matrix2dBase M) {
      set (M);
   }

   /**
    * {@inheritDoc}
    */
   public void setColumns (Vector2d v0, Vector2d v1) {
      super.setColumns (v0, v1);
   }

   /**
    * {@inheritDoc}
    */
   public void setRows (Vector2d v0, Vector2d v1) {
      super.setRows (v0, v1);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (Matrix2dBase M1) {
      super.mul (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (Matrix2dBase M1, Matrix2dBase M2) {
      super.mul (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTranspose (Matrix2dBase M1) {
      super.mulTransposeRight (this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeLeft (Matrix2dBase M1, Matrix2dBase M2) {
      super.mulTransposeLeft (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeRight (Matrix2dBase M1, Matrix2dBase M2) {
      super.mulTransposeRight (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeBoth (Matrix2dBase M1, Matrix2dBase M2) {
      super.mulTransposeBoth (M1, M2);
   }

   /**
    * Multiplies this matrix by the inverse of M1 and places the result in this
    * matrix.
    * 
    * @param M1
    * right-hand matrix
    * @return false if M1 is singular
    */
   public boolean mulInverse (Matrix2dBase M1) {
      return super.mulInverseRight (this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseRight (Matrix2dBase M1, Matrix2dBase M2) {
      return super.mulInverseRight (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseLeft (Matrix2dBase M1, Matrix2dBase M2) {
      return super.mulInverseLeft (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseBoth (Matrix2dBase M1, Matrix2dBase M2) {
      return super.mulInverseBoth (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix2dBase M1, Matrix2dBase M2) {
      super.add (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix2dBase M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix2dBase M1, Matrix2dBase M2) {
      super.sub (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix2dBase M1) {
      super.sub (M1);
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      super.scale (s, this);
   }

   /**
    * {@inheritDoc}
    */
   public void scale (double s, Matrix2dBase M1) {
      super.scale (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix2dBase M1, Matrix2dBase M2) {
      super.scaledAdd (s, M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix2dBase M1) {
      super.scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void negate (Matrix2dBase M1) {
      super.negate (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void transpose (Matrix2dBase M1) {
      super.transpose (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void setZero() {
      super.setZero();
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom() {
      super.setRandom();
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper) {
      super.setRandom (lower, upper);
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper, Random generator) {
      super.setRandom (lower, upper, generator);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert() {
      return super.invert (this);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert (Matrix2dBase M) {
      return super.invert (M);
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified by diag.
    * 
    * @param diag
    * diagonal values
    */
   public void setDiagonal (Vector2d diag) {
      m00 = diag.x;
      m01 = 0;
      m10 = 0;
      m11 = diag.y;
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified by the
    * array vals.
    * 
    * @param vals
    * diagonal values
    */
   public void setDiagonal (double[] vals) {
      m00 = vals[0];
      m01 = 0;
      m10 = 0;
      m11 = vals[1];
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified.
    * 
    * @param m00 first diagonal value
    * @param m11 second diagonal value
    */
   public void setDiagonal (double m00, double m11) {
      this.m00 = m00;
      this.m01 = 0;

      this.m10 = 0;
      this.m11 = m11;
   }
   
   /**
    * Adds an outer product to this matrix. The outer product
    * is formed from two vectors whose coordinates are given
    * as arguments.
    * 
    * @param x0
    * first vector x coordinate
    * @param y0
    * first vector y coordinate
    * @param x1
    * second vector x coordinate
    * @param y1
    * second vector y coordinate
    */
   public void addOuterProduct (
      double x0, double y0, double x1, double y1) {      
      m00 += x0*x1;
      m10 += y0*x1;

      m01 += x0*y1;
      m11 += y0*y1;
   }

   /**
    * Adds an outer product to this matrix. The outer product
    * is formed from two vectors are given as arguments.
    * 
    * @param v0
    * first vector
    * @param v1
    * second vector
    */
   public void addOuterProduct (Vector2d v0, Vector2d v1) {
      m00 += v0.x*v1.x;
      m10 += v0.y*v1.x;

      m01 += v0.x*v1.y;
      m11 += v0.y*v1.y;
   }

   /**
    * Adds a scaled outer product to this matrix. The outer product
    * is formed from two vectors are given as arguments.
    *
    * @param s scaling factor
    * @param v0 first vector
    * @param v1 second vector
    */
   public void addScaledOuterProduct (double s, Vector2d v0, Vector2d v1) {

      double v1x = s*v1.x;
      double v1y = s*v1.y;

      m00 += v0.x*v1x;
      m10 += v0.y*v1x;

      m01 += v0.x*v1y;
      m11 += v0.y*v1y;
   }

   /**
    * Sets this matrix to the outer product
    * <pre>
    * v0 v1^T
    * </pre>
    * 
    * @param v0
    * first vector
    * @param v1
    * second vector
    */
   public void outerProduct (Vector2d v0, Vector2d v1) {

      m00 = v0.x*v1.x;
      m10 = v0.y*v1.x;

      m01 = v0.x*v1.y;
      m11 = v0.y*v1.y;
   }
   
   public static double solve(Matrix2dBase M, Vector2d b, Vector2d x) {
      // solve using Cramer's rule
      double d = M.m00*M.m11-M.m01*M.m10;
      if (d == 0) {
         // singular
         // do 2D SVD
         double ac = M.m00*M.m11;
         double bd = M.m01*M.m10;
         double aa = M.m00*M.m00;
         double bb = M.m01*M.m01;
         double cc = M.m10*M.m10;
         double dd = M.m11*M.m11;
         
         double theta = 0.5*Math.atan2(2*(ac+bd), aa+bb-cc-dd);
         double phi = 0.5*Math.atan2(2*(ac+bd), aa-bb+cc-dd);
         
         double d1 = aa+bb+cc+dd;
         double d21 = aa+bb-cc-dd;
         double d22 = ac+bd;
         double d2 = Math.sqrt(d21*d21+d22*d22);
         
         double s1 = Math.sqrt((d1+d2)/2);
         // double s2 = Math.sqrt((d1-d2)/2);  // should be zero
         
         double ctheta = Math.cos(theta);
         double stheta = Math.sin(theta);
         double cphi = Math.cos(phi);
         double sphi = Math.sin(phi);
         
         double s11 = Math.signum((M.m00*ctheta+M.m10*stheta)*cphi+(M.m01*ctheta+M.m11*stheta)*sphi);
         // double s22 = Math.signum((M.m00*stheta-M.m10*ctheta)*sphi+(-M.m01*stheta+M.m11*ctheta)*cphi);
         // U = [cos(theta), -sin(theta); sin(theta), cos(theta)];
         // E = [s1, 0; 0, s2];
         // V = [s11*cos(phi), -s22*sin(phi); s11*sin(phi), s22*cos(phi)];
         double b11 = (b.x*ctheta-b.y*stheta)/s1;
         x.x = b11*s11*cphi;
         x.y = b11*s11*sphi;
      } else {
         x.x = (b.x*M.m11-b.y*M.m01)/d;
         x.y = (M.m00*b.y-M.m10*b.x)/d;
      }
      return d;
   }
   
   @Override
   public Matrix2d clone () {
      return (Matrix2d)super.clone();
   }

   /* VectorObject implementation. It is currently necessary to define the
    * scale and add methods as scaleObj(), addObj(), and scaledAddObj(), since
    * the corresponding scale(), add() and scaledAdd() methods have
    * incompatible return types across different classes (some return a
    * reference to their object, while others return {@code void}).
    */

   /**
    * {@inheritDoc}
    */
   public void scaleObj (double s) {
      super.scale (s, this);
   }

   /**
    * {@inheritDoc}
    */
   public void addObj (Matrix2d M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix2d M1) {
      super.scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix2d M1) {
      super.set (M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix2d M1, double tol) {
      return super.epsilonEquals (M1, tol);
   }
}
