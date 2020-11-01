/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 1 x 3 matrix
 */
public class Matrix1x3 extends DenseMatrixBase
   implements VectorObject<Matrix1x3> {

   public double m00;
   public double m01;
   public double m02;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix1x3 ZERO = new Matrix1x3();

   /**
    * Creates a new Matrix1x3Block.
    */
   public Matrix1x3() {
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
      return 3;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (i != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (j) {
         case 0:
            return m00;
         case 1:
            return m01;
         case 2:
            return m02;
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * Gets the contents of this Matrix1x3 into a Vector3d.
    * 
    * @param v
    * vector to return contents in
    */
   public void get (Vector3d v) {
      v.x = m00;
      v.y = m01;
      v.z = m02;
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
      if (i != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (j) {
         case 0:
            m00 = value;
            return;
         case 1:
            m01 = value;
            return;
         case 2:
            m02 = value;
            return;
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] vals) {
      m00 = vals[0];
      m01 = vals[1];
      m02 = vals[2];
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
   }

   public void set (Matrix M) {
      if (M instanceof Matrix1x3) {
         set ((Matrix1x3)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);
      }
   }

   /**
    * Sets the contents of this Matrix1x3Block to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix1x3 M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;
   }

   /**
    * Sets the contents of this Matrix1x3 from a Vector3d.
    * 
    * @param v
    * vector providing new values
    */
   public void set (Vector3d v) {
      m00 = v.x;
      m01 = v.y;
      m02 = v.z;
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
   }

   /**
    * Scales the elements of vector v1 by <code>s</code> and places the
    * results in this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    */
   public void scale (double s, Vector3d v1) {
      m00 = s * v1.x;
      m01 = s * v1.y;
      m02 = s * v1.z;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix1x3 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m02 = s * M.m02;
   }

   /**
    * Computes <code>s v1</code> and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    */
   public void scaledAdd (double s, Vector3d v1) {
      m00 += s * v1.x;
      m01 += s * v1.y;
      m02 += s * v1.z;
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
      if (M instanceof Matrix1x3) {
         add ((Matrix1x3)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
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
      if (M instanceof Matrix1x3) {
         scaledAdd (s, (Matrix1x3)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
      }
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
   public void scaledAdd (double s, Matrix1x3 M1, Matrix1x3 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;
   }

   /**
    * Adds the contents of a Matrix1x3 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix1x3 M) {
      m00 += M.m00;
      m01 += M.m01;
      m02 += M.m02;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix1x3 M1, Matrix1x3 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;
   }

   /**
    * Adds the scaled contents of a Matrix1x3 to this matrix block.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix1x3 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m02 += s * M.m02;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix1x3 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m02 = -M.m02;
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
      if (M instanceof Matrix1x3) {
         sub ((Matrix1x3)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
      }
   }

   /**
    * Subtracts the contents of a Matrix1x3 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix1x3 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m02 -= M.m02;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix1x3 M1, Matrix1x3 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd1x3 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd1x3 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd1x3 (this, M1, M2);
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix3x1 M) {
      m00 = M.m00;
      m01 = M.m10;
      m02 = M.m20;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix1x3 clone() {
      try {
         return (Matrix1x3)super.clone();
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
   public void addObj (Matrix1x3 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix1x3 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix1x3 M1) {
      return m00 == M1.m00 && m01 == M1.m01 && m02 == M1.m02;
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix1x3 M1, double tol) {
      return (abs (m00 - M1.m00) <= tol &&
              abs (m01 - M1.m01) <= tol &&
              abs (m02 - M1.m02) <= tol);
   }

}
