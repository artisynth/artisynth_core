/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;

/**
 * Implements a 6 x 1 matrix
 */
public class Matrix6x1 extends DenseMatrixBase 
   implements VectorObject<Matrix6x1> {
   
   public double m00;
   public double m10;
   public double m20;
   public double m30;
   public double m40;
   public double m50;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix6x1 ZERO = new Matrix6x1();

   /**
    * Creates a new Matrix6x1.
    */
   public Matrix6x1() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return 6;
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return 1;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (j != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (i) {
         case 0: {
            return m00;
         }
         case 1: {
            return m10;
         }
         case 2: {
            return m20;
         }
         case 3: {
            return m30;
         }
         case 4: {
            return m40;
         }
         case 5: {
            return m50;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * Gets the contents of this Matrix6x1 into two Vector3ds.
    * 
    * @param v1
    * vector to return first three elements in
    * @param v2
    * vector to return second three elements in
    */
   public void get (Vector3d v1, Vector3d v2) {
      v1.x = m00;
      v1.y = m10;
      v1.z = m20;
      v2.x = m30;
      v2.y = m40;
      v2.z = m50;
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
      if (j != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (i) {
         case 0: {
            m00 = value;
            return;
         }
         case 1: {
            m10 = value;
            return;
         }
         case 2: {
            m20 = value;
            return;
         }
         case 3: {
            m30 = value;
            return;
         }
         case 4: {
            m40 = value;
            return;
         }
         case 5: {
            m50 = value;
            return;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] values) {
      m00 = values[0];
      m10 = values[1];
      m20 = values[2];
      m30 = values[3];
      m40 = values[4];
      m50 = values[5];
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            m00 = values[0];
            m10 = values[1];
            m20 = values[2];
            m30 = values[3];
            m40 = values[4];
            m50 = values[5];
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
            break;
         }
         case 1: {
            m10 = values[0];
            break;
         }
         case 2: {
            m20 = values[0];
            break;
         }
         case 3: {
            m30 = values[0];
            break;
         }
         case 4: {
            m40 = values[0];
            break;
         }
         case 5: {
            m50 = values[0];
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
      m10 = 0;
      m20 = 0;
      m30 = 0;
      m40 = 0;
      m50 = 0;
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M instanceof Matrix6x1) {
         set ((Matrix6x1)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m10 = M.get (1, 0);
         m20 = M.get (2, 0);
         m30 = M.get (3, 0);
         m40 = M.get (4, 0);
         m50 = M.get (5, 0);
      }
   }

   /**
    * Sets the contents of this Matrix6x1 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix6x1 M) {
      m00 = M.m00;
      m10 = M.m10;
      m20 = M.m20;
      m30 = M.m30;
      m40 = M.m40;
      m50 = M.m50;
   }

   /**
    * Sets the contents of this Matrix6x1 from two Vector3ds.
    * 
    * @param v1
    * vector providing first three values
    * @param v2
    * vector providing second three values
    */
   public void set (Vector3d v1, Vector3d v2) {
      m00 = v1.x;
      m10 = v1.y;
      m20 = v1.z;
      m30 = v2.x;
      m40 = v2.y;
      m50 = v2.z;
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
      m10 = nrm.y;
      m20 = nrm.z;
      m30 = pnt.y * nrm.z - pnt.z * nrm.y;
      m40 = pnt.z * nrm.x - pnt.x * nrm.z;
      m50 = pnt.x * nrm.y - pnt.y * nrm.x;
   }

   /** 
    * Transforms the first three and last three elements of this Matrix6x1
    * into a new coordinate system specified by a rotation matrix R.
    * 
    * @param R Rotation matrix specifying the rotation
    */
   public void transform (RotationMatrix3d R) {

      double x = m00;
      double y = m10;
      double z = m20;

      m00 = R.m00*x + R.m01*y + R.m02*z;
      m10 = R.m10*x + R.m11*y + R.m12*z;
      m20 = R.m20*x + R.m21*y + R.m22*z;

      x = m30;
      y = m40;
      z = m50;

      m30 = R.m00*x + R.m01*y + R.m02*z;
      m40 = R.m10*x + R.m11*y + R.m12*z;
      m50 = R.m20*x + R.m21*y + R.m22*z;
   }

   /**
    * Forms the product of a 6x6 matrix with a 6x1 matrix and places
    * the result in this matrix.
    *
    * @param M1 6x6 matrix  
    * @param M2 6x1 matrix
    */
   public void mul (Matrix6d M1, Matrix6x1 M2) {
      
      double x = M2.m00;
      double y = M2.m10;
      double z = M2.m20;
      double w = M2.m30;
      double u = M2.m40;
      double v = M2.m50;

      m00 = M1.m00*x + M1.m01*y + M1.m02*z + M1.m03*w + M1.m04*u + M1.m05*v;
      m10 = M1.m10*x + M1.m11*y + M1.m12*z + M1.m13*w + M1.m14*u + M1.m15*v;
      m20 = M1.m20*x + M1.m21*y + M1.m22*z + M1.m23*w + M1.m24*u + M1.m25*v;
      m30 = M1.m30*x + M1.m31*y + M1.m32*z + M1.m33*w + M1.m34*u + M1.m35*v;
      m40 = M1.m40*x + M1.m41*y + M1.m42*z + M1.m43*w + M1.m44*u + M1.m45*v;
      m50 = M1.m50*x + M1.m51*y + M1.m52*z + M1.m53*w + M1.m54*u + M1.m55*v;
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      m00 *= s;
      m10 *= s;
      m20 *= s;
      m30 *= s;
      m40 *= s;
      m50 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix6x1 M) {
      m00 = s * M.m00;
      m10 = s * M.m10;
      m20 = s * M.m20;
      m30 = s * M.m30;
      m40 = s * M.m40;
      m50 = s * M.m50;
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
      if (M instanceof Matrix6x1) {
         add ((Matrix6x1)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m10 += M.get (1, 0);
         m20 += M.get (2, 0);
         m30 += M.get (3, 0);
         m40 += M.get (4, 0);
         m50 += M.get (5, 0);
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
      if (M instanceof Matrix6x1) {
         scaledAdd (s, (Matrix6x1)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m10 += s * M.get (1, 0);
         m20 += s * M.get (2, 0);
         m30 += s * M.get (3, 0);
         m40 += s * M.get (4, 0);
         m50 += s * M.get (5, 0);
      }
   }

   /**
    * Adds the contents of a Matrix6x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix6x1 M) {
      m00 += M.m00;
      m10 += M.m10;
      m20 += M.m20;
      m30 += M.m30;
      m40 += M.m40;
      m50 += M.m50;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix6x1 M1, Matrix6x1 M2) {
      m00 = M1.m00 + M2.m00;
      m10 = M1.m10 + M2.m10;
      m20 = M1.m20 + M2.m20;
      m30 = M1.m30 + M2.m30;
      m40 = M1.m40 + M2.m40;
      m50 = M1.m50 + M2.m50;
   }

   /**
    * Adds the scaled contents of a Matrix6x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix6x1 M) {
      m00 += s * M.m00;
      m10 += s * M.m10;
      m20 += s * M.m20;
      m30 += s * M.m30;
      m40 += s * M.m40;
      m50 += s * M.m50;
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
   public void scaledAdd (double s, Matrix6x1 M1, Matrix6x1 M2) {
      m00 = s * M1.m00 + M2.m00;
      m10 = s * M1.m10 + M2.m10;
      m20 = s * M1.m20 + M2.m20;
      m30 = s * M1.m30 + M2.m30;
      m40 = s * M1.m40 + M2.m40;
      m50 = s * M1.m50 + M2.m50;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix6x1 M) {
      m00 = -M.m00;
      m10 = -M.m10;
      m20 = -M.m20;
      m30 = -M.m30;
      m40 = -M.m40;
      m50 = -M.m50;
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      negate (this);
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd6x1 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd6x1 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd6x1 (this, M1, M2);
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
      if (M instanceof Matrix6x1) {
         sub ((Matrix6x1)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m10 -= M.get (1, 0);
         m20 -= M.get (2, 0);
         m30 -= M.get (3, 0);
         m40 -= M.get (4, 0);
         m50 -= M.get (5, 0);
      }
   }

   /**
    * Subtracts the contents of a Matrix6x1 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix6x1 M) {
      m00 -= M.m00;
      m10 -= M.m10;
      m20 -= M.m20;
      m30 -= M.m30;
      m40 -= M.m40;
      m50 -= M.m50;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix6x1 M1, Matrix6x1 M2) {
      m00 = M1.m00 - M2.m00;
      m10 = M1.m10 - M2.m10;
      m20 = M1.m20 - M2.m20;
      m30 = M1.m30 - M2.m30;
      m40 = M1.m40 - M2.m40;
      m50 = M1.m50 - M2.m50;
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
      return m00*v1.x + m10*v1.y + m20*v1.z + m30*v2.x + m40*v2.y + m50*v2.z;
   }

   /**
    * Returns the dot product of the elements of this matrix with a
    * another 6 x 1 matrix.
    * 
    * @param M matrix to take dot product with
    * @return dot product of this with M
    */
   public double dot (Matrix6x1 M) {
      return m00*M.m00 + m10*M.m10 + m20*M.m20 + m30*M.m30 + m40*M.m40 + m50*M.m50;
   }

   public String transposeToString() {
      return transposeToString ("%g");
   }

   public String transposeToString (String fmtStr) {
      return transposeToString (new NumberFormat (fmtStr));
   }

   public String transposeToString (NumberFormat fmt) {
      return (fmt.format (m00) + " " + fmt.format (m10) + " " +
              fmt.format (m20) + " " + fmt.format (m30) + " " +
              fmt.format (m40) + " " + fmt.format (m50));
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix1x6 M) {
      m00 = M.m00;
      m10 = M.m01;
      m20 = M.m02;
      m30 = M.m03;
      m40 = M.m04;
      m50 = M.m05;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix6x1 clone() {
      try {
         return (Matrix6x1)super.clone();
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
   public void addObj (Matrix6x1 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix6x1 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix6x1 M1) {
      return (m00 == M1.m00 && m10 == M1.m10 && m20 == M1.m20 &&
              m30 == M1.m30 && m40 == M1.m40 && m50 == M1.m50);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix6x1 M1, double tol) {
      return (abs (m00 - M1.m00) <= tol &&
              abs (m10 - M1.m10) <= tol &&
              abs (m20 - M1.m20) <= tol &&
              abs (m30 - M1.m30) <= tol &&
              abs (m40 - M1.m40) <= tol &&
              abs (m50 - M1.m50) <= tol);
   }

}
