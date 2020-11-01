/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 2 x 3 matrix
 */
public class Matrix2x3 extends DenseMatrixBase
   implements VectorObject<Matrix2x3> {

   public double m00;
   public double m01;
   public double m02;

   public double m10;
   public double m11;
   public double m12;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix2x3 ZERO = new Matrix2x3();

   /**
    * Creates a new Matrix2x3Block.
    */
   public Matrix2x3() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return 2;
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
      switch (i) {
         case 0: {
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
         case 1: {
            switch (j) {
               case 0:
                  return m10;
               case 1:
                  return m11;
               case 2:
                  return m12;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
      switch (i) {
         case 0: {
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
         case 1: {
            switch (j) {
               case 0:
                  m10 = value;
                  return;
               case 1:
                  m11 = value;
                  return;
               case 2:
                  m12 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
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
      m01 = values[1];
      m02 = values[2];
      m10 = values[3];
      m11 = values[4];
      m12 = values[5];
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            m00 = values[0];
            m10 = values[1];
            break;
         }
         case 1: {
            m01 = values[0];
            m11 = values[1];
            break;
         }
         case 2: {
            m02 = values[0];
            m12 = values[1];
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
         case 1: {
            m10 = values[0];
            m11 = values[1];
            m12 = values[2];
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
      m10 = 0;
      m11 = 0;
      m12 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix2x3) {
         set ((Matrix2x3)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);
         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
         m12 = M.get (1, 2);
      }
   }

   /**
    * Sets the contents of this Matrix2x3Block to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix2x3 M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;

      m10 = M.m10;
      m11 = M.m11;
      m12 = M.m12;
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

      m10 *= s;
      m11 *= s;
      m12 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix2x3 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m02 = s * M.m02;

      m10 = s * M.m10;
      m11 = s * M.m11;
      m12 = s * M.m12;
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
      if (M instanceof Matrix2x3) {
         add ((Matrix2x3)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);
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
      if (M instanceof Matrix2x3) {
         scaledAdd (s, (Matrix2x3)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m12 += s * M.get (1, 2);
      }
   }

   /**
    * Adds the contents of a Matrix2x3 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix2x3 M) {
      m00 += M.m00;
      m01 += M.m01;
      m02 += M.m02;

      m10 += M.m10;
      m11 += M.m11;
      m12 += M.m12;
   }


   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix2x3 M1, Matrix2x3 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m12 = M1.m12 + M2.m12;
   }

   /**
    * Adds the scaled contents of a Matrix2x3 to this matrix block.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix2x3 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m02 += s * M.m02;

      m10 += s * M.m10;
      m11 += s * M.m11;
      m12 += s * M.m12;
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
   public void scaledAdd (double s, Matrix2x3 M1, Matrix2x3 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix2x3 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m02 = -M.m02;

      m10 = -M.m10;
      m11 = -M.m11;
      m12 = -M.m12;
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
      if (M instanceof Matrix2x3) {
         sub ((Matrix2x3)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);
      }
   }

   /**
    * Subtracts the contents of a Matrix2x3 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix2x3 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m02 -= M.m02;

      m10 -= M.m10;
      m11 -= M.m11;
      m12 -= M.m12;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix2x3 M1, Matrix2x3 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m12 = M1.m12 - M2.m12;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd2x3 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd2x3 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd2x3 (this, M1, M2);
   }

   /**
    * Creates a transpose of this matrix block.
    */
   public Matrix3x2Block createTranspose() {
      Matrix3x2Block M = new Matrix3x2Block();
      M.transpose (this);
      return M;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix3x2 M) {
      m00 = M.m00;
      m10 = M.m01;
      m01 = M.m10;
      m11 = M.m11;
      m02 = M.m20;
      m12 = M.m21;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix2x3 clone() {
      try {
         return (Matrix2x3)super.clone();
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
   public void addObj (Matrix2x3 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix2x3 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix2x3 M1) {
      return (m00 == M1.m00 && m01 == M1.m01 && m02 == M1.m02 &&
              m10 == M1.m10 && m11 == M1.m11 && m12 == M1.m12);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix2x3 M1, double tol) {
      return (abs (m00 - M1.m00) <= tol &&
              abs (m01 - M1.m01) <= tol &&
              abs (m02 - M1.m02) <= tol &&
              abs (m10 - M1.m10) <= tol &&
              abs (m11 - M1.m11) <= tol &&
              abs (m12 - M1.m12) <= tol);
   }
}


