/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

/**
 * A general 4 x 4 matrix with the elements stored as explicit fields.
 */
public class Matrix4d extends Matrix4dBase implements VectorObject<Matrix4d> {

   /**
    * Global identity matrix. Should not be modified.
    */
   public static final Matrix4d IDENTITY =
      new Matrix4d (new double[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0,
                                  1 });

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix4d ZERO = new Matrix4d();

   /**
    * Creates a matrix and initializes it to zero.
    */
   public Matrix4d() {
   }

   /**
    * Creates a matrix and initializes its elements from an array of values.
    * 
    * @param vals
    * element values for the matrix, with element (i,j) stored at location
    * <code>i*4+j</code>
    */
   public Matrix4d (double[] vals) {
      set (vals);
   }

   /**
    * Creates a matrix and initializes its elements to those of the matrix M.
    * 
    * @param M
    * matrix object to be copied.
    */
   public Matrix4d (Matrix M) {
      set (M);
   }

   /**
    * {@inheritDoc}
    */
   public void setColumns (Vector4d v0, Vector4d v1, Vector4d v2, Vector4d v3) {
      super.setColumns (v0, v1, v2, v3);
   }

   /**
    * {@inheritDoc}
    */
   public void setRows (Vector4d v0, Vector4d v1, Vector4d v2, Vector4d v3) {
      super.setRows (v0, v1, v2, v3);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (Matrix4dBase M1) {
      super.mul (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (Matrix4dBase M1, Matrix4dBase M2) {
      super.mul (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTranspose (Matrix4dBase M1) {
      super.mulTransposeRight (this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeLeft (Matrix4dBase M1, Matrix4dBase M2) {
      super.mulTransposeLeft (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeRight (Matrix4dBase M1, Matrix4dBase M2) {
      super.mulTransposeRight (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeBoth (Matrix4dBase M1, Matrix4dBase M2) {
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
   public boolean mulInverse (Matrix4dBase M1) {
      return mulInverseRight (this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseRight (Matrix4dBase M1, Matrix4dBase M2) {
      return super.mulInverseRight (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseLeft (Matrix4dBase M1, Matrix4dBase M2) {
      return super.mulInverseLeft (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseBoth (Matrix4dBase M1, Matrix4dBase M2) {
      return super.mulInverseBoth (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix4dBase M1, Matrix4dBase M2) {
      super.add (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix4dBase M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix4dBase M1, Matrix4dBase M2) {
      super.sub (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix4dBase M1) {
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
   public void scale (double s, Matrix4dBase M1) {
      super.scale (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix4dBase M1, Matrix4dBase M2) {
      super.scaledAdd (s, M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix4dBase M1) {
      super.scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void negate (Matrix4dBase M1) {
      super.negate (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void transpose (Matrix4dBase M1) {
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
   public boolean invert (Matrix4dBase M) {
      return super.invert (M);
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified by diag.
    * 
    * @param diag
    * diagonal values
    */
   public void setDiagonal (Vector4d diag) {
      m00 = diag.x;
      m01 = 0;
      m02 = 0;
      m03 = 0;

      m10 = 0;
      m11 = diag.y;
      m12 = 0;
      m13 = 0;

      m20 = 0;
      m21 = 0;
      m22 = diag.z;
      m23 = 0;

      m30 = 0;
      m31 = 0;
      m32 = 0;
      m33 = diag.w;
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
      m02 = 0;
      m03 = 0;

      m10 = 0;
      m11 = vals[1];
      m12 = 0;
      m13 = 0;

      m20 = 0;
      m21 = 0;
      m22 = vals[2];
      m23 = 0;

      m30 = 0;
      m31 = 0;
      m32 = 0;
      m33 = vals[3];
   }
   
   /**
    * Sets this matrix to a diagonal matrix whose values are specified.
    * 
    * @param m00 first diagonal value
    * @param m11 second diagonal value
    * @param m22 third diagonal value
    * @param m33 fourth diagonal value
    */
   public void setDiagonal (double m00, double m11, double m22, double m33) {
      this.m00 = m00;
      this.m01 = 0;
      this.m02 = 0;
      this.m03 = 0;

      this.m10 = 0;
      this.m11 = m11;
      this.m12 = 0;
      this.m13 = 0;

      this.m20 = 0;
      this.m21 = 0;
      this.m22 = m22;
      this.m23 = 0;

      this.m30 = 0;
      this.m31 = 0;
      this.m32 = 0;
      this.m33 = m33;
   }

   /**
    * Computes the outer product of quaternions q0 and q1 (each treated simply
    * as a 4 vector), scales it by s, and adds the result to this matrix.
    * 
    * @param s scale factor
    * @param q0 left side quaternion
    * @param q1 right side quaternion
    */
   public void addScaledOuterProduct (
      double s, Quaternion q0, Quaternion q1) {

      m00 += s*(q0.s*q1.s);

      m01 += s*(q0.s*q1.u.x);
      m02 += s*(q0.s*q1.u.y);
      m03 += s*(q0.s*q1.u.z);

      m10 += s*(q1.s*q0.u.x);
      m20 += s*(q1.s*q0.u.y);
      m30 += s*(q1.s*q0.u.z);

      m11 += s*(q0.u.x*q1.u.x);
      m12 += s*(q0.u.x*q1.u.y);
      m13 += s*(q0.u.x*q1.u.z);

      m21 += s*(q0.u.y*q1.u.x);
      m22 += s*(q0.u.y*q1.u.y);
      m23 += s*(q0.u.y*q1.u.z);

      m31 += s*(q0.u.z*q1.u.x);
      m32 += s*(q0.u.z*q1.u.y);
      m33 += s*(q0.u.z*q1.u.z);
   }

   public Matrix4d clone() {
      return (Matrix4d)super.clone();
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
   public void addObj (Matrix4d M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix4d M1) {
      super.scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix4d M1) {
      super.set (M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix4d M1, double tol) {
      return super.epsilonEquals (M1, tol);
   }
}
