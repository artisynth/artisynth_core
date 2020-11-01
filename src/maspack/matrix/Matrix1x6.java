/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 1 x 6 matrix
 */
public class Matrix1x6 extends DenseMatrixBase
   implements VectorObject<Matrix1x6> {

   public double m00;
   public double m01;
   public double m02;
   public double m03;
   public double m04;
   public double m05;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix1x6 ZERO = new Matrix1x6();

   /**
    * Creates a new Matrix1x6.
    */
   public Matrix1x6() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return 1;
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return 6;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (i != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (j) {
         case 0: {
            return m00;
         }
         case 1: {
            return m01;
         }
         case 2: {
            return m02;
         }
         case 3: {
            return m03;
         }
         case 4: {
            return m04;
         }
         case 5: {
            return m05;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * Gets the contents of this Matrix1x6 into two Vector3ds.
    * 
    * @param v1
    * vector to return first three elements in
    * @param v2
    * vector to return second three elements in
    */
   public void get (Vector3d v1, Vector3d v2) {
      v1.x = m00;
      v1.y = m01;
      v1.z = m02;
      v2.x = m03;
      v2.y = m04;
      v2.z = m05;
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
      if (i != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (j) {
         case 0: {
            m00 = value;
            return;
         }
         case 1: {
            m01 = value;
            return;
         }
         case 2: {
            m02 = value;
            return;
         }
         case 3: {
            m03 = value;
            return;
         }
         case 4: {
            m04 = value;
            return;
         }
         case 5: {
            m05 = value;
            return;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   public void set (double[] vals) {
      m00 = vals[0];
      m01 = vals[1];
      m02 = vals[2];
      m03 = vals[3];
      m04 = vals[4];
      m05 = vals[5];
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            m00 = values[0];
            break;
         }
         case 1: {
            m01 = values[0];
            break;
         }
         case 2: {
            m02 = values[0];
            break;
         }
         case 3: {
            m03 = values[0];
            break;
         }
         case 4: {
            m04 = values[0];
            break;
         }
         case 5: {
            m05 = values[0];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setRow (int i, double[] values) {
      switch (i) {
         case 0: {
            m00 = values[0];
            m01 = values[1];
            m02 = values[2];
            m03 = values[3];
            m04 = values[4];
            m05 = values[5];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   public void setZero() {
      m00 = 0;
      m01 = 0;
      m02 = 0;
      m03 = 0;
      m04 = 0;
      m05 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix1x6) {
         set ((Matrix1x6)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);
         m03 = M.get (0, 3);
         m04 = M.get (0, 4);
         m05 = M.get (0, 5);
      }
   }

   /**
    * Sets the contents of this Matrix1x6 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix1x6 M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;
      m03 = M.m03;
      m04 = M.m04;
      m05 = M.m05;
   }

   /**
    * Sets the contents of this Matrix1x6 from two Vector3ds.
    * 
    * @param v1
    * vector providing first three values
    * @param v2
    * vector providing second three values
    */
   public void set (Vector3d v1, Vector3d v2) {
      m00 = v1.x;
      m01 = v1.y;
      m02 = v1.z;
      m03 = v2.x;
      m04 = v2.y;
      m05 = v2.z;
   }

   /**
    * Sets the contents of this Matrix6x1 to the wrench produced from nrm and
    * pnt. The first three elements are set to nrm, and the last three elements
    * are set to pnt x nrm.
    * 
    * @param nrm
    * normal vector used to form the wrench
    * @param pnt
    * point used to form the wrench
    */
   public void setWrench (Vector3d nrm, Vector3d pnt) {
      m00 = nrm.x;
      m01 = nrm.y;
      m02 = nrm.z;
      m03 = pnt.y * nrm.z - pnt.z * nrm.y;
      m04 = pnt.z * nrm.x - pnt.x * nrm.z;
      m05 = pnt.x * nrm.y - pnt.y * nrm.x;
   }

   /** 
    * Transforms the first three and last three elements of this Matrix1x6
    * into a new coordinate system specified by a rotation matrix R.
    * 
    * @param R Rotation matrix specifying the rotation
    */
   public void transform (RotationMatrix3d R) {

      double x = m00;
      double y = m01;
      double z = m02;

      m00 = R.m00*x + R.m01*y + R.m02*z;
      m01 = R.m10*x + R.m11*y + R.m12*z;
      m02 = R.m20*x + R.m21*y + R.m22*z;

      x = m03;
      y = m04;
      z = m05;

      m03 = R.m00*x + R.m01*y + R.m02*z;
      m04 = R.m10*x + R.m11*y + R.m12*z;
      m05 = R.m20*x + R.m21*y + R.m22*z;
   }


    /**
    * Forms the product of a 1x6 matrix with a 6x6 matrix and places
    * the result in this matrix.
    *
    * @param M1 1x6 matrix  
    * @param M2 6x6 matrix
    */
   public void mul (Matrix1x6 M1, Matrix6d M2) {
      
      double x = M1.m00;
      double y = M1.m01;
      double z = M1.m02;
      double w = M1.m03;
      double u = M1.m04;
      double v = M1.m05;

      m00 = M2.m00*x + M2.m10*y + M2.m20*z + M2.m30*w + M2.m40*u + M2.m50*v;
      m01 = M2.m01*x + M2.m11*y + M2.m21*z + M2.m31*w + M2.m41*u + M2.m51*v;
      m02 = M2.m02*x + M2.m12*y + M2.m22*z + M2.m32*w + M2.m42*u + M2.m52*v;
      m03 = M2.m03*x + M2.m13*y + M2.m23*z + M2.m33*w + M2.m43*u + M2.m53*v;
      m04 = M2.m04*x + M2.m14*y + M2.m24*z + M2.m34*w + M2.m44*u + M2.m54*v;
      m05 = M2.m05*x + M2.m15*y + M2.m25*z + M2.m35*w + M2.m45*u + M2.m55*v;
   }

  /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      m00 *= s;
      m01 *= s;
      m02 *= s;
      m03 *= s;
      m04 *= s;
      m05 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix1x6 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m02 = s * M.m02;
      m03 = s * M.m03;
      m04 = s * M.m04;
      m05 = s * M.m05;
   }

   /**
    * Adds this matrix to M and places the result in this matrix.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M have different sizes
    */
   public void add (Matrix M) {
      if (M instanceof Matrix1x6) {
         add ((Matrix1x6)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m03 += M.get (0, 3);
         m04 += M.get (0, 4);
         m05 += M.get (0, 5);
      }
   }

   /**
    * Scales the matrix M and add the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    * @throws ImproperSizeException
    * if this matrix and M have different sizes
    */
   public void scaledAdd (double s, Matrix M) {
      if (M instanceof Matrix1x6) {
         scaledAdd (s, (Matrix1x6)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
         m03 += s * M.get (0, 3);
         m04 += s * M.get (0, 4);
         m05 += s * M.get (0, 5);
      }
   }

   /**
    * Adds the contents of a Matrix1x6 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix1x6 M) {
      m00 += M.m00;
      m01 += M.m01;
      m02 += M.m02;
      m03 += M.m03;
      m04 += M.m04;
      m05 += M.m05;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix1x6 M1, Matrix1x6 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;
      m03 = M1.m03 + M2.m03;
      m04 = M1.m04 + M2.m04;
      m05 = M1.m05 + M2.m05;
   }

   /**
    * Adds the scaled contents of a Matrix1x6 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix1x6 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m02 += s * M.m02;
      m03 += s * M.m03;
      m04 += s * M.m04;
      m05 += s * M.m05;
   }

   /**
    * Computes s M1 + M2 and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    * @param M2
    * matrix to be added
    */
   public void scaledAdd (double s, Matrix1x6 M1, Matrix1x6 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;
      m03 = s * M1.m03 + M2.m03;
      m04 = s * M1.m04 + M2.m04;
      m05 = s * M1.m05 + M2.m05;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix1x6 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m02 = -M.m02;
      m03 = -M.m03;
      m04 = -M.m04;
      m05 = -M.m05;
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      negate (this);
   }

   /**
    * Subtracts this matrix from M and places the result in this matrix.
    * 
    * @param M
    * right-hand matrix
    * @throws ImproperSizeException
    * if this matrix and M have different sizes
    */
   public void sub (Matrix M) {
      if (M instanceof Matrix1x6) {
         sub ((Matrix1x6)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m03 -= M.get (0, 3);
         m04 -= M.get (0, 4);
         m05 -= M.get (0, 5);
      }
   }

   /**
    * Subtracts the contents of a Matrix1x6 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix1x6 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m02 -= M.m02;
      m03 -= M.m03;
      m04 -= M.m04;
      m05 -= M.m05;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix1x6 M1, Matrix1x6 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;
      m03 = M1.m03 - M2.m03;
      m04 = M1.m04 - M2.m04;
      m05 = M1.m05 - M2.m05;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd1x6 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd1x6 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd1x6 (this, M1, M2);
   }

   /**
    * Returns the dot product of the elements of this matrix with a
    * 6-element vector formed from two 3-element vectors.
    * 
    * @param v1 first three vector elements
    * @param v2 second three vector elements
    * @return dot product of this with [v1 v2]
    */
   public double dot (Vector3d v1, Vector3d v2) {
      return m00*v1.x + m01*v1.y + m02*v1.z + m03*v2.x + m04*v2.y + m05*v2.z;
   }

   /**
    * Returns the dot product of the elements of this matrix with a
    * another 1 x 6 matrix.
    * 
    * @param M matrix to take dot product with
    * @return dot product of this with M
    */
   public double dot (Matrix1x6 M) {
      return m00*M.m00 + m01*M.m01 + m02*M.m02 + m03*M.m03 + m04*M.m04 + m05*M.m05;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix6x1 M) {
      m00 = M.m00;
      m01 = M.m10;
      m02 = M.m20;
      m03 = M.m30;
      m04 = M.m40;
      m05 = M.m50;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix1x6 clone() {
      try {
         return (Matrix1x6)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for " + getClass());
      }
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
      scale (s, this);
   }

   /**
    * {@inheritDoc}
    */
   public void addObj (Matrix1x6 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix1x6 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix1x6 M1) {
      return (m00 == M1.m00 && m01 == M1.m01 && m02 == M1.m02 &&
              m03 == M1.m03 && m04 == M1.m04 && m05 == M1.m05);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix1x6 M1, double tol) {
      return (abs (m00 - M1.m00) <= tol &&
              abs (m01 - M1.m01) <= tol &&
              abs (m02 - M1.m02) <= tol &&
              abs (m03 - M1.m03) <= tol &&
              abs (m04 - M1.m04) <= tol &&
              abs (m05 - M1.m05) <= tol);
   }

}
