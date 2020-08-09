/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.FunctionTimer;

/**
 * A general 3 x 3 matrix with the elements stored as explicit fields.
 */
public class Matrix3d extends Matrix3dBase implements VectorObject<Matrix3d> {
   
   private static final long serialVersionUID = 1L;

   /**
    * Global identity matrix. Should not be modified.
    */
   public static final Matrix3d IDENTITY =
      new Matrix3d(new double[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 });

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix3d ZERO = new Matrix3d();

   /**
    * Creates a matrix and initializes it to zero.
    */
   public Matrix3d() {
   }

   /**
    * Creates a matrix and initializes its elements to those of the matrix M.
    * 
    * @param M
    * matrix object to be copied.
    */
   public Matrix3d (Matrix M) {
      set (M);
   }

   /**
    * Creates a matrix and initializes its elements from an array of values.
    * 
    * @param vals
    * element values for the matrix, with element (i,j) stored at location
    * <code>i*3+j</code>
    */
   public Matrix3d(double[] vals) {
      set(vals);
   }

   /**
    * Creates a matrix and initializes its elements from the specified values.
    * 
    * @param m00
    * element (0,0)
    * @param m01
    * element (0,1)
    * @param m02
    * element (0,2)
    * @param m10
    * element (1,0)
    * @param m11
    * element (1,1)
    * @param m12
    * element (1,2)
    * @param m20
    * element (2,0)
    * @param m21
    * element (2,1)
    * @param m22
    * element (2,2)
    */
   public Matrix3d (
      double m00, double m01, double m02,
      double m10, double m11, double m12,
      double m20, double m21, double m22) {
      
      set (m00, m01, m02, m10, m11, m12, m20, m21, m22);
   }

   /**
    * Creates a matrix and initializes its elements to those of the matrix M.
    * 
    * @param M
    * matrix object to be copied.
    */
   public Matrix3d(Matrix3dBase M) {
      set(M);
   }

   /**
    * {@inheritDoc}
    */
    public void setColumns(Vector3d v0, Vector3d v1, Vector3d v2) {
       super.setColumns(v0, v1, v2);
   }

   /**
    * Sets the values of this matrix.
    * 
    * @param m00
    * element (0,0)
    * @param m01
    * element (0,1)
    * @param m02
    * element (0,2)
    * @param m10
    * element (1,0)
    * @param m11
    * element (1,1)
    * @param m12
    * element (1,2)
    * @param m20
    * element (2,0)
    * @param m21
    * element (2,1)
    * @param m22
    * element (2,2)
    */
   public void set (
      double m00, double m01, double m02,
      double m10, double m11, double m12,
      double m20, double m21, double m22) {

      this.m00 = m00;
      this.m01 = m01;
      this.m02 = m02;
      this.m10 = m10;
      this.m11 = m11;
      this.m12 = m12;
      this.m20 = m20;
      this.m21 = m21;
      this.m22 = m22;
   }

   /**
    * {@inheritDoc}
    */
   public void setRows(Vector3d v0, Vector3d v1, Vector3d v2) {
      super.setRows(v0, v1, v2);
   }

   /**
    * {@inheritDoc}
    */
   public void mul(Matrix3dBase M1) {
      super.mul(M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mul(Matrix3dBase M1, Matrix3dBase M2) {
      super.mul(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTranspose(Matrix3dBase M1) {
      super.mulTransposeRight(this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeLeft(Matrix3dBase M1, Matrix3dBase M2) {
      super.mulTransposeLeft(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeRight(Matrix3dBase M1, Matrix3dBase M2) {
      super.mulTransposeRight(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeBoth(Matrix3dBase M1, Matrix3dBase M2) {
      super.mulTransposeBoth(M1, M2);
   }

   /**
    * Multiplies this matrix by the inverse of M1 and places the result in this
    * matrix.
    * 
    * @param M1
    * right-hand matrix
    * @return false if M1 is singular
    */
   public boolean mulInverse(Matrix3dBase M1) {
      return mulInverseRight(this, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseRight(Matrix3dBase M1, Matrix3dBase M2) {
      return super.mulInverseRight(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseLeft(Matrix3dBase M1, Matrix3dBase M2) {
      return super.mulInverseLeft(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseBoth(Matrix3dBase M1, Matrix3dBase M2) {
      return super.mulInverseBoth(M1, M2);
   }

   /**
    * Multiples the rows of this matrix by the values specified by
    * <code>diag</code> and places the result in this matrix.  This is
    * equivalent to pre-multiplying this matrix by a diagonal matrix whose
    * (diagonal) elements are specified by <code>diag</code>.
    * 
    * @param diag
    * specifies the row multipliers
    */
   public void mulRows(Vector3d diag) {
      mulRows(diag.x, diag.y, diag.z);
   }

   /**
    * Multiples the rows of this matrix by the values specified by
    * <code>d00</code>, <code>d11</code>, and <code>d22</code> and places the
    * result in this matrix.  This is equivalent to pre-multiplying this matrix
    * by a diagonal matrix with the specified diagonal elements.
    * 
    * @param d00
    * multiplier for the first row
    * @param d11
    * multiplier for the second row
    * @param d22
    * multiplier for the third row
    */
   public void mulRows(double d00, double d11, double d22) {
      m00 *= d00;
      m01 *= d00;
      m02 *= d00;

      m10 *= d11;
      m11 *= d11;
      m12 *= d11;

      m20 *= d22;
      m21 *= d22;
      m22 *= d22;
   }

   /**
    * Multiples the columns of this matrix by the values specified by
    * <code>diag</code> and places the result in this matrix.  This is
    * equivalent to post-multiplying this matrix by a diagonal matrix whose
    * (diagonal) elements are specified by <code>diag</code>.
    * 
    * @param diag
    * specifies the column multipliers
    */
   public void mulCols(Vector3d diag) {
      mulCols(diag.x, diag.y, diag.z);
   }

   /**
    * Multiples the columns of this matrix by the values specified by
    * <code>d00</code>, <code>d11</code>, and <code>d22</code> and places the
    * result in this matrix.  This is equivalent to post-multiplying this matrix
    * by a diagonal matrix with the specified diagonal elements.
    * 
    * @param d00
    * multiplier for the first column
    * @param d11
    * multiplier for the second column
    * @param d22
    * multiplier for the third column
    */
   public void mulCols(double d00, double d11, double d22) {
      m00 *= d00;
      m10 *= d00;
      m20 *= d00;

      m01 *= d11;
      m11 *= d11;
      m21 *= d11;

      m02 *= d22;
      m12 *= d22;
      m22 *= d22;
   }

   /**
    * {@inheritDoc}
    */
   public void add(Matrix3dBase M1, Matrix3dBase M2) {
      super.add(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void add(Matrix3dBase M1) {
      super.add(M1);
   }

   /**
    * {@inheritDoc}
    */
   public void sub(Matrix3dBase M1, Matrix3dBase M2) {
      super.sub(M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void sub(Matrix3dBase M1) {
      super.sub(M1);
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale(double s) {
      super.scale(s, this);
   }

   /**
    * {@inheritDoc}
    */
   public void scale(double s, Matrix3dBase M1) {
      super.scale(s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd(double s, Matrix3dBase M1, Matrix3dBase M2) {
      super.scaledAdd(s, M1, M2);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd(double s, Matrix3dBase M1) {
      super.scaledAdd(s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void negate(Matrix3dBase M1) {
      super.negate(M1);
   }

   /**
    * {@inheritDoc}
    */
   public void transpose(Matrix3dBase M1) {
      super.transpose(M1);
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
   public void setRandom(double lower, double upper) {
      super.setRandom(lower, upper);
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom(double lower, double upper, Random generator) {
      super.setRandom(lower, upper, generator);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert() {
      return invert(this);
   }

   /**
    * {@inheritDoc}
    */
   public double fastInvert (Matrix3dBase M) {
      return super.fastInvert(M);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert(Matrix3dBase M) {
      return super.invert(M);
   }

   /**
    * {@inheritDoc}
    */
   public void setDiagonal(Vector3d diag) {
      super.setDiagonal(diag);
   }

   /**
    * {@inheritDoc}
    */
   public void setDiagonal(double[] vals) {
      super.setDiagonal(vals);
   }
 
   /**
    * {@inheritDoc}
    */
   public void setDiagonal(double m00, double m11, double m22) {
      super.setDiagonal(m00, m11, m22);
   }

   /**
    * Sets the diagonal elements of this matrix to s.
    *
    * @param s value for each diagonal element
    */
   public void setDiagonal (double s) {
      super.setDiagonal (s, s, s);
   }
   
   /**
    * Adds s to the diagonal elements of this matrix.
    *
    * @param s value to add to the diagonal
    */
   public void addDiagonal (double s) {
      m00 += s;
      m11 += s;
      m22 += s;
   }
   
  /**
    * Sets this matrix to the symmetric component of matrix M1
    */
   public void setSymmetric (Matrix3dBase M1) {

      m01 = 0.5*(M1.m01 + M1.m10);
      m02 = 0.5*(M1.m02 + M1.m20);
      m12 = 0.5*(M1.m12 + M1.m21);

      m00 = M1.m00;
      m11 = M1.m11;
      m22 = M1.m22;

      m10 = m01;
      m20 = m02;
      m21 = m12;
   }

   /**
    * Sets this matrix to the skew-symmetric matrix of vector v
    */
   public void setSkewSymmetric (Vector3d v) {
      m00 = 0.0;
      m11 = 0.0;
      m22 = 0.0;

      m01 = -v.z;
      m02 = v.y;
      m12 = -v.x;

      m10 = v.z;
      m20 = -v.y;
      m21 = v.x;
   }

   /**
    * Sets this matrix to the outer product of the two vectors v1 and v2.
    * 
    * @param v1
    * first vector
    * @param v2
    * second vector
    */
   public void outerProduct(Vector3d v1, Vector3d v2) {
      m00 = v1.x * v2.x;
      m11 = v1.y * v2.y;
      m22 = v1.z * v2.z;

      m01 = v1.x * v2.y;
      m02 = v1.x * v2.z;
      m12 = v1.y * v2.z;

      m10 = v1.y * v2.x;
      m20 = v1.z * v2.x;
      m21 = v1.z * v2.y;
   }

   /**
    * {@inheritDoc}
    */
   public void addOuterProduct (
      double x0, double y0, double z0, double x1, double y1, double z1) {
      super.addOuterProduct (x0, y0, z0, x1, y1, z1);
   }

   /**
    * {@inheritDoc}
    */
   public void addOuterProduct (Vector3d v0, Vector3d v1) {
      super.addOuterProduct (v0, v1);
   }

   /**
    * {@inheritDoc}
    */
   public void addScaledOuterProduct (double s, Vector3d v0, Vector3d v1) {
      super.addScaledOuterProduct (s, v0, v1);
   }

   /**
    * {@inheritDoc}
    */
   public void addScaledOuterProduct (double s, Vector3d v) {
      super.addScaledOuterProduct (s, v);
   }

   /**
    * Computes the cross product of v with each column of M and places the
    * result in this matrix. This is equivalent to computing
    * <pre>
    * this = [v] M
    * </pre>
    * 
    * @param v
    * first cross product argument
    * @param M
    * matrix whose columns supply the second cross product argument
    */
   public void crossProduct(Vector3d v, Matrix3dBase M) {
      double x, y, z;

      x = v.y * M.m20 - v.z * M.m10;
      y = v.z * M.m00 - v.x * M.m20;
      z = v.x * M.m10 - v.y * M.m00;
      m00 = x;
      m10 = y;
      m20 = z;

      x = v.y * M.m21 - v.z * M.m11;
      y = v.z * M.m01 - v.x * M.m21;
      z = v.x * M.m11 - v.y * M.m01;
      m01 = x;
      m11 = y;
      m21 = z;

      x = v.y * M.m22 - v.z * M.m12;
      y = v.z * M.m02 - v.x * M.m22;
      z = v.x * M.m12 - v.y * M.m02;
      m02 = x;
      m12 = y;
      m22 = z;
   }

   /**
    * Computes the cross product of each row of M with v and places the result
    * in this matrix. This is equivalent to computing
    * <pre>
    * this = M [v]
    * </pre>
    * 
    * @param M
    * matrix whose rows supply the first cross product argument
    * @param v
    * second cross product argument
    */
   public void crossProduct (Matrix3dBase M, Vector3d v) {
      double x, y, z;

      x = M.m01 * v.z - M.m02 * v.y;
      y = M.m02 * v.x - M.m00 * v.z;
      z = M.m00 * v.y - M.m01 * v.x;
      m00 = x;
      m01 = y;
      m02 = z;

      x = M.m11 * v.z - M.m12 * v.y;
      y = M.m12 * v.x - M.m10 * v.z;
      z = M.m10 * v.y - M.m11 * v.x;
      m10 = x;
      m11 = y;
      m12 = z;

      x = M.m21 * v.z - M.m22 * v.y;
      y = M.m22 * v.x - M.m20 * v.z;
      z = M.m20 * v.y - M.m21 * v.x;
      m20 = x;
      m21 = y;
      m22 = z;
   }

   /**
    * Rearrange the columns of this matrix according to the specified
    * permutation. In particular, column 0 is set to the original value of
    * column col0, column 1 is set to the original value of column col1, etc. If
    * any index value is out of range, the associated column is left unchanged.
    * 
    * @param col0
    * index of the original column which is to replace column 0
    * @param col1
    * index of the original column which is to replace column 1
    * @param col2
    * index of the original column which is to replace column 2
    */
   public void permuteColumns(int col0, int col1, int col2) {
      double x00 = m00;
      double x10 = m10;
      double x20 = m20;

      double x01 = m01;
      double x11 = m11;
      double x21 = m21;

      double x02 = m02;
      double x12 = m12;
      double x22 = m22;

      if (col0 == 1) {
         m00 = x01;
         m10 = x11;
         m20 = x21;
      }
      else if (col0 == 2) {
         m00 = x02;
         m10 = x12;
         m20 = x22;
      }
      if (col1 == 0) {
         m01 = x00;
         m11 = x10;
         m21 = x20;
      }
      else if (col1 == 2) {
         m01 = x02;
         m11 = x12;
         m21 = x22;
      }
      if (col2 == 0) {
         m02 = x00;
         m12 = x10;
         m22 = x20;
      }
      else if (col2 == 1) {
         m02 = x01;
         m12 = x11;
         m22 = x21;
      }
   }

   /**
    * Rearrange the columns of this matrix according to the specified
    * permutation, such that each column j is replaced by column permutation[j].
    * If any index value is out of range, the associated column is left
    * unchanged.
    * 
    * @param permutation
    * describes the column exchanges
    * @throws ImproperSizeException
    * if the length of <code>permutation</code> is less than 3
    */
   public void permuteColumns(int[] permutation) {
      if (permutation.length < 3) {
         throw new ImproperSizeException("permutation argument too short");
      }
      permuteColumns(permutation[0], permutation[1], permutation[2]);
   }

   /**
    * Rearrange the rows of this matrix according to the specified permutation.
    * In particular, row 0 is set to the original value of row row0, row 1 is
    * set to the original value of row row1, etc. If any index value is out of
    * range, the associated row is left unchanged.
    * 
    * @param row0
    * index of the original row which is to replace row 0
    * @param row1
    * index of the original row which is to replace row 1
    * @param row2
    * index of the original row which is to replace row 2
    */
   public void permuteRows(int row0, int row1, int row2) {
      double x00 = m00;
      double x10 = m10;
      double x20 = m20;

      double x01 = m01;
      double x11 = m11;
      double x21 = m21;

      double x02 = m02;
      double x12 = m12;
      double x22 = m22;

      if (row0 == 1) {
         m00 = x10;
         m01 = x11;
         m02 = x12;
      }
      else if (row0 == 2) {
         m00 = x20;
         m01 = x21;
         m02 = x22;
      }
      if (row1 == 0) {
         m10 = x00;
         m11 = x01;
         m12 = x02;
      }
      else if (row1 == 2) {
         m10 = x20;
         m11 = x21;
         m12 = x22;
      }
      if (row2 == 0) {
         m20 = x00;
         m21 = x01;
         m22 = x02;
      }
      else if (row2 == 1) {
         m20 = x10;
         m21 = x11;
         m22 = x12;
      }
   }

   /**
    * Rearrange the rows of this matrix according to the specified permutation,
    * such that each row i is replaced by row permutation[i]. If any index value
    * is out of range, the associated row is left unchanged.
    * 
    * @param permutation
    * describes the row exchanges
    * @throws ImproperSizeException
    * if the length of <code>permutation</code> is less than 3.
    */
   public void permuteRows(int[] permutation) {
      if (permutation.length < 3) {
         throw new ImproperSizeException("permutation argument too short");
      }
      permuteRows(permutation[0], permutation[1], permutation[2]);
   }

   /**
    * Applies a rotational transformation R to this matrix, in place. This is
    * equivalent to forming the product
    * 
    * <pre>
    *    R M R^T
    * </pre>
    * 
    * where M is this matrix.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void transform(RotationMatrix3d R) {
      super.transform(R, this);
   }

   /**
    * Applies a rotational transformation R to a matrix M, and place the result
    * in this matrix. This is equivalent to forming the product
    * 
    * <pre>
    *    R M R^T
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M
    * matrix to transform
    */
   public void transform(RotationMatrix3d R, Matrix3dBase M) {
      super.transform(R, M);
   }

   /**
    * Applies an inverse rotational transformation R to this matrix, in place.
    * This is equivalent to forming the product
    * 
    * <pre>
    *    R^T M R
    * </pre>
    * 
    * where M is this matrix.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void inverseTransform(RotationMatrix3d R) {
      super.inverseTransform(R, this);
   }

   /**
    * Applies an inverse rotational transformation R to a matrix M, and place
    * the result in this matrix. This is equivalent to forming the product
    * 
    * <pre>
    *    R^T M R
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M
    * matrix to transform
    */
   public void inverseTransform(RotationMatrix3d R, Matrix3dBase M) {
      super.inverseTransform(R, M);
   }

   public static void main(String[] args) {
      Matrix3d MX = new Matrix3d(new double[] { 1, 2, 3, 4, 2, 4, 6, 5, 4 });
      Matrix3d M = new Matrix3d();

      LUDecomposition lu = new LUDecomposition();

      M.set(MX);
      M.invert();
      System.out.println("inv=\n" + M.toString("%9.4f"));

      lu.factor(MX);
      lu.inverse(M);
      System.out.println("lu inv=\n" + M.toString("%9.4f"));

      FunctionTimer timer = new FunctionTimer();
      int cnt = 10000000;

      timer.start();
      for (int i = 0; i < cnt; i++) {
         M.set(MX);
         M.invert();
      }
      timer.stop();
      System.out.println("inv time: " + timer.result(cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         lu.factor(MX);
         lu.inverse(M);
      }
      timer.stop();
      System.out.println("lu time: " + timer.result(cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         M.mul(MX, MX);
      }
      timer.stop();
      System.out.println("mul time: " + timer.result(cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         M.set(MX);
      }
      timer.stop();
      System.out.println("set time: " + timer.result(cnt));


   }

   /** 
    * Computes the QR decompostion for this matrix, for which
    * <pre>
    *    M = Q R
    * </pre>
    * where Q is orthogonal and R is upper triangular.
    * 
    * @param Q if non-null, returns Q
    * @param R if non-null, returns R
    *
    * The computation is the same as that performed by the QRDecomposition
    * class, except significantly faster.
    */
   public void factorQR (Matrix3d Q, Matrix3d R) {
      /* The QR reduction can be done with two Householder steps:

         R = this matrix
         v0 = house(R(:,0));
         rowHouseMul (R, v0);
         v1 = house(R(1:2,1));
         rowHouseMul (R(1:2,1:2),v1);
         
         Q = I;
         rowHouseMul (Q(1:2,1:2),v1);
         rowHouseMul (Q,v0);
      */
      if (R == null) {
         R = new Matrix3d();
      }
      R.set (this);
      
      double v01, v02, v12; // components of house vectors v0 and v1
      double w0, w1, w2; // components of temporary vector w
      double len, beta;

      // v0 = house(R(:,0));
      len = Math.sqrt(R.m00*R.m00 + R.m10*R.m10 + R.m20*R.m20);
      if (len != 0) {
         beta = R.m00 + (R.m00 >= 0 ? len : -len);
         v01 = R.m01/beta;
         v02 = R.m02/beta;
      }
      else {
         v01 = v02 = 0;
      }

      // rowHouseMul (R, v0);
      beta = -2/(1+v01*v01+v02*v02);
      w0 = beta*(R.m00 + R.m10*v01 + R.m20*v02);
      w1 = beta*(R.m01 + R.m11*v01 + R.m21*v02);
      w2 = beta*(R.m02 + R.m12*v01 + R.m22*v02);
      R.addOuterProduct (w0, w1, w2, 1, v01, v02);

      //v1 = house(R(1:2,1));
      len = Math.sqrt(R.m11*R.m11 + R.m12*R.m12);
      if (len != 0) {
         beta = R.m11 + (R.m11 >= 0 ? len : -len);
         v12 = R.m12/beta;
      }
      else {
         v12 = 0;
      }

      // rowHouseMul (R, v0);
      beta = -2/(1+v12*v12);
      w1 = beta*(R.m11 + R.m21*v12);
      w2 = beta*(R.m12 + R.m22*v12);
      R.addOuterProduct (0, w1, w2, 0, 1, v12);

      R.m01 = R.m02 = R.m12 = 0;

      if (Q != null) {
         Q.setIdentity();

         // rowHouseMul (Q(1:2,1:2),v1);
         beta = -2/(1+v12*v12);
         w1 = beta*(Q.m11 + Q.m21*v12);
         w2 = beta*(Q.m12 + Q.m22*v12);
         Q.addOuterProduct (0, w1, w2, 0, 1, v12);

         // rowHouseMul (Q,v0);
         beta = -2/(1+v01*v01+v02*v02);
         w0 = beta*(Q.m00 + Q.m10*v01 + Q.m20*v02);
         w1 = beta*(Q.m01 + Q.m11*v01 + Q.m21*v02);
         w2 = beta*(Q.m02 + Q.m12*v01 + Q.m22*v02);
         Q.addOuterProduct (w0, w1, w2, 1, v01, v02);
      }
   }
   
   @Override
   public Matrix3d clone() {
      return (Matrix3d)super.clone ();
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
   public void addObj (Matrix3d M1) {
      super.add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix3d M1) {
      super.scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix3d M1) {
      super.set (M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix3d M1, double tol) {
      return super.epsilonEquals (M1, tol);
   }
}


