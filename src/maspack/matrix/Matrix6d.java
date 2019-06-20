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
public class Matrix6d extends Matrix6dBase implements VectorObject<Matrix6d> {

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix6d ZERO = new Matrix6d();

   /**
    * Creates a matrix and initializes it to zero.
    */
   public Matrix6d() {
   }

   /**
    * Creates a matrix and initializes its elements from an array of values.
    * 
    * @param vals
    * element values for the matrix, with element (i,j) stored at location
    * <code>i*6+j</code>
    */
   public Matrix6d (double[] vals) {
      set (vals);
   }

   /**
    * Creates a matrix and initializes its elements to those of the matrix M.
    * 
    * @param M
    * matrix object to be copied.
    */
   public Matrix6d (Matrix M) {
      set (M);
   }

   /** 
    * Gets the 3x3 sub-matrix of this matrix starting at (0, 0).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix00 (Matrix3dBase M) {
      M.m00 = m00; M.m01 = m01; M.m02 = m02; 
      M.m10 = m10; M.m11 = m11; M.m12 = m12; 
      M.m20 = m20; M.m21 = m21; M.m22 = m22; 
   }
   
   /** 
    * Gets the 3x3 sub-matrix of this matrix starting at (3, 0).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix30 (Matrix3dBase M) {
      M.m00 = m30; M.m01 = m31; M.m02 = m32; 
      M.m10 = m40; M.m11 = m41; M.m12 = m42; 
      M.m20 = m50; M.m21 = m51; M.m22 = m52; 
   }

   /** 
    * Gets the 3x3 sub-matrix of this matrix starting at (0, 3).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix03 (Matrix3dBase M) {
      M.m00 = m03; M.m01 = m04; M.m02 = m05; 
      M.m10 = m13; M.m11 = m14; M.m12 = m15; 
      M.m20 = m23; M.m21 = m24; M.m22 = m25; 
   }
   
   /** 
    * Gets the 3x3 sub-matrix of this matrix starting at (3, 3).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix33 (Matrix3dBase M) {
      M.m00 = m33; M.m01 = m34; M.m02 = m35; 
      M.m10 = m43; M.m11 = m44; M.m12 = m45; 
      M.m20 = m53; M.m21 = m54; M.m22 = m55; 
   }
   
   /** 
    * Sets the 3x3 sub-matrix of this matrix starting at (0, 0).
    * 
    * @param M new sub matrix value
    */
   public void setSubMatrix00 (Matrix3dBase M) {
      m00 = M.m00; m01 = M.m01; m02 = M.m02; 
      m10 = M.m10; m11 = M.m11; m12 = M.m12; 
      m20 = M.m20; m21 = M.m21; m22 = M.m22; 
   }
   
   /** 
    * Sets the 3x3 sub-matrix of this matrix starting at (3, 0).
    *
    * @param M new sub matrix value
    */
   public void setSubMatrix30 (Matrix3dBase M) {
      m30 = M.m00; m31 = M.m01; m32 = M.m02; 
      m40 = M.m10; m41 = M.m11; m42 = M.m12; 
      m50 = M.m20; m51 = M.m21; m52 = M.m22; 
   }

   /** 
    * Sets the 3x3 sub-matrix of this matrix starting at (0, 3).
    * 
    * @param M new sub matrix value
    */
   public void setSubMatrix03 (Matrix3dBase M) {
      m03 = M.m00; m04 = M.m01; m05 = M.m02; 
      m13 = M.m10; m14 = M.m11; m15 = M.m12; 
      m23 = M.m20; m24 = M.m21; m25 = M.m22; 
   }
   
   /** 
    * Sets the 3x3 sub-matrix of this matrix starting at (3, 3).
    * 
    * @param M new sub matrix value
    */
   public void setSubMatrix33 (Matrix3dBase M) {
      m33 = M.m00; m34 = M.m01; m35 = M.m02; 
      m43 = M.m10; m44 = M.m11; m45 = M.m12; 
      m53 = M.m20; m54 = M.m21; m55 = M.m22; 
   }
   
   /** 
    * Adds to 3x3 sub-matrix of this matrix starting at (0, 0).
    * 
    * @param M sub matrix to add
    */
   public void addSubMatrix00 (Matrix3dBase M) {
      m00 += M.m00; m01 += M.m01; m02 += M.m02; 
      m10 += M.m10; m11 += M.m11; m12 += M.m12; 
      m20 += M.m20; m21 += M.m21; m22 += M.m22; 
   }
   
   /** 
    * Adds to the 3x3 sub-matrix of this matrix starting at (3, 0).
    *
    * @param M sub matrix to add
    */
   public void addSubMatrix30 (Matrix3dBase M) {
      m30 += M.m00; m31 += M.m01; m32 += M.m02; 
      m40 += M.m10; m41 += M.m11; m42 += M.m12; 
      m50 += M.m20; m51 += M.m21; m52 += M.m22; 
   }

   /** 
    * Adds to the 3x3 sub-matrix of this matrix starting at (0, 3).
    * 
    * @param M sub matrix to add
    */
   public void addSubMatrix03 (Matrix3dBase M) {
      m03 += M.m00; m04 += M.m01; m05 += M.m02; 
      m13 += M.m10; m14 += M.m11; m15 += M.m12; 
      m23 += M.m20; m24 += M.m21; m25 += M.m22; 
   }
   
   /** 
    * Adds to the 3x3 sub-matrix of this matrix starting at (3, 3).
    * 
    * @param M sub matrix to add
    */
   public void addSubMatrix33 (Matrix3dBase M) {
      m33 += M.m00; m34 += M.m01; m35 += M.m02; 
      m43 += M.m10; m44 += M.m11; m45 += M.m12; 
      m53 += M.m20; m54 += M.m21; m55 += M.m22; 
   }
   
   /**
    * {@inheritDoc}
    */
   public void mul (Matrix6dBase M1) {
      super.mul (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (Matrix6dBase M1, Matrix6dBase M2) {
      super.mul (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTranspose (Matrix6dBase M1) {
      super.mulTransposeRight (this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeLeft (Matrix6dBase M1, Matrix6dBase M2) {
      super.mulTransposeLeft (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeRight (Matrix6dBase M1, Matrix6dBase M2) {
      super.mulTransposeRight (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeBoth (Matrix6dBase M1, Matrix6dBase M2) {
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
   public boolean mulInverse (Matrix6dBase M1) {
      return mulInverseRight (this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseRight (Matrix6dBase M1, Matrix6dBase M2) {
      return super.mulInverseRight (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseLeft (Matrix6dBase M1, Matrix6dBase M2) {
      return super.mulInverseLeft (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseBoth (Matrix6dBase M1, Matrix6dBase M2) {
      return super.mulInverseBoth (M1, M2);
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd6x6 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd6x6 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd6x6 (this, M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix6dBase M1, Matrix6dBase M2) {
      super.add (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix6dBase M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix6dBase M1, Matrix6dBase M2) {
      super.sub (M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix6dBase M1) {
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
   public void scale (double s, Matrix6dBase M1) {
      super.scale (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void negate (Matrix6dBase M1) {
      super.negate (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void transpose (Matrix6dBase M1) {
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
   public boolean invert (Matrix6dBase M) {
      return super.invert (M);
   }

   public void setDiagonal (VectorNd diag) {
      super.setDiagonal(diag.getBuffer());
   }

   /**
    * {@inheritDoc}
    */
   public void setDiagonal (double[] vals) {
      super.setDiagonal(vals);
   }

   /**
    * {@inheritDoc}
    */
   public void setDiagonal (
      double m00, double m11, double m22, double m33, double m44, double m55) {
      super.setDiagonal (m00, m11, m22, m33, m44, m55);
   }

   /**
    * Applies a rotational transformation R to this matrix, in place.
    * Equivalent to
    * {@link #transform(maspack.matrix.RotationMatrix3d,maspack.matrix.Matrix6d) transform(R,M)}
    * with <code>M</code> equal to this matrix.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void transform(RotationMatrix3d R) {
      super.transform(R, this);
   }

   /**
    * Applies a rotational transformation R to M1 and place the result in this
    * matrix. This is equivalent to applying a rotational transform to
    * each of the 4 3x3 submatrices:
    * 
    * <pre>
    *    [ R  0 ]      [ R^T 0  ]
    *    [      ]  M1  [        ]
    *    [ 0  R ]      [ 0  R^T ]
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M1
    * matrix to transform
    */
   public void transform(RotationMatrix3d R, Matrix6d M1) {
      super.transform(R, M1);
   }

   /**
    * Applies an inverse rotational transformation R to this matrix, in place.
    * Equivalent to
    * {@link #inverseTransform(maspack.matrix.RotationMatrix3d,maspack.matrix.Matrix6d) inverseTransform(R,M)}
    * with <code>M</code> equal to this matrix.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void inverseTransform(RotationMatrix3d R) {
      super.inverseTransform(R, this);
   }

   /**
    * Applies an inverse rotational transformation R to a matrix M1 and place
    * the result in this matrix. This is equivalent to applying an inverse
    * rotational transform to each of the 4 3x3 submatrices:
    * 
    * <pre>
    *    [ R^T  0 ]      [ R  0 ]
    *    [        ]  M1  [      ]
    *    [ 0  R^T ]      [ 0  R ]
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M1
    * matrix to transform
    */
   public void inverseTransform(RotationMatrix3d R, Matrix6d M1) {
      super.inverseTransform(R, M1);
   }

   /** 
    * Makes this matrix symmetric by setting its lower triangular elements
    * equal to the corresponding upper triangular elements.
    */
   public void setLowerToUpper () {
      m10 = m01; m20 = m02; m30 = m03; m40 = m04; m50 = m05;
      m21 = m12; m31 = m13; m41 = m14; m51 = m15;
      m32 = m23; m42 = m24; m52 = m25;
      m43 = m34; m53 = m35;
      m54 = m45;
   }
   
   @Override
   public Matrix6d clone() {
      return (Matrix6d)super.clone();
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
   public void addObj (Matrix6d M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix6d M1) {
      super.scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix6d M1) {
      super.set (M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix6d M1, double tol) {
      return super.epsilonEquals (M1, tol);
   }
}
