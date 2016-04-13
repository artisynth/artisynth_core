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
public class Matrix2d extends Matrix2dBase {
   
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
   
   @Override
   public Matrix2d clone () {
      return new Matrix2d (this);
   }

}
