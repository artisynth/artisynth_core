/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 3 x 2 matrix
 */
public class Matrix3x2 extends DenseMatrixBase
   implements VectorObject<Matrix3x2> {

   public double m00;
   public double m01;

   public double m10;
   public double m11;

   public double m20;
   public double m21;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix3x2 ZERO = new Matrix3x2();

   /**
    * Creates a new Matrix3x2.
    */
   public Matrix3x2() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return 3;
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return 2;
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
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
                  return m20;
               case 1:
                  return m21;
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
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
                  m20 = value;
                  return;
               case 1:
                  m21 = value;
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
      m10 = values[2];
      m11 = values[3];
      m20 = values[4];
      m21 = values[5];
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
            break;
         }
         case 1: {
            m01 = values[0];
            m11 = values[1];
            m21 = values[2];
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
            break;
         }
         case 1: {
            m10 = values[0];
            m11 = values[1];
            break;
         }
         case 2: {
            m20 = values[0];
            m21 = values[1];
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
      m10 = 0;
      m11 = 0;
      m20 = 0;
      m21 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix3x2) {
         set ((Matrix3x2)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
         m20 = M.get (2, 0);
         m21 = M.get (2, 1);
      }
   }

   /**
    * Sets the contents of this Matrix3x2 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix3x2 M) {
      m00 = M.m00;
      m01 = M.m01;
      m10 = M.m10;
      m11 = M.m11;
      m20 = M.m20;
      m21 = M.m21;
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
      m10 *= s;
      m11 *= s;
      m20 *= s;
      m21 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix3x2 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m10 = s * M.m10;
      m11 = s * M.m11;
      m20 = s * M.m20;
      m21 = s * M.m21;
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
      if (M instanceof Matrix3x2) {
         add ((Matrix3x2)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
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
      if (M instanceof Matrix3x2) {
         scaledAdd (s, (Matrix3x2)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);
      }
   }

   /**
    * Adds the contents of a Matrix3x2 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix3x2 M) {
      m00 += M.m00;
      m01 += M.m01;
      m10 += M.m10;
      m11 += M.m11;
      m20 += M.m20;
      m21 += M.m21;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix3x2 M1, Matrix3x2 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;
   }

   /**
    * Adds the scaled contents of a Matrix3x2 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix3x2 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m10 += s * M.m10;
      m11 += s * M.m11;
      m20 += s * M.m20;
      m21 += s * M.m21;
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
   public void scaledAdd (double s, Matrix3x2 M1, Matrix3x2 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix3x2 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m10 = -M.m10;
      m11 = -M.m11;
      m20 = -M.m20;
      m21 = -M.m21;
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
      if (M instanceof Matrix3x2) {
         sub ((Matrix3x2)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
      }
   }

   /**
    * Subtracts the contents of a Matrix3x2 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix3x2 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m10 -= M.m10;
      m11 -= M.m11;
      m20 -= M.m20;
      m21 -= M.m21;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix3x2 M1, Matrix3x2 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd3x2 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd3x2 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd3x2 (this, M1, M2);
   }

   /** 
    * Computes
    * <p>
    *   M1 D M2^T
    * <p>
    * where D is a diagonal matrix, and adds the result to matrix MR.
    * 
    * @param MR matrix to add result to
    * @param M1 left matrix
    * @param D diagonal matrix values
    * @param M2 right matrix transpose
    */   
   public static void mulScaledTransposeRightAdd (
      Matrix3d MR, Matrix3x2 M1, double[] D, Matrix3x2 M2) {

      double d0 = D[0];
      double d1 = D[1];

      double T00 = d0*M2.m00;
      double T01 = d0*M2.m10;
      double T02 = d0*M2.m20;

      double T10 = d1*M2.m01;
      double T11 = d1*M2.m11;
      double T12 = d1*M2.m21;

      MR.m00 += M1.m00*T00 + M1.m01*T10;
      MR.m01 += M1.m00*T01 + M1.m01*T11;
      MR.m02 += M1.m00*T02 + M1.m01*T12;

      MR.m10 += M1.m10*T00 + M1.m11*T10;
      MR.m11 += M1.m10*T01 + M1.m11*T11;
      MR.m12 += M1.m10*T02 + M1.m11*T12;

      MR.m20 += M1.m20*T00 + M1.m21*T10;
      MR.m21 += M1.m20*T01 + M1.m21*T11;
      MR.m22 += M1.m20*T02 + M1.m21*T12;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix2x3 M) {
      m00 = M.m00;
      m10 = M.m01;
      m20 = M.m02;
      m01 = M.m10;
      m11 = M.m11;
      m21 = M.m12;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix3x2 clone() {
      try {
         return (Matrix3x2)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for " + getClass());
      }
   }

   public void mul(Vector3d vr, Vector2d v1) {
      vr.x = v1.x*m00+v1.y*m01;
      vr.y = v1.x*m10+v1.y*m11;
      vr.z = v1.x*m20+v1.y*m21;
   }
   
   public void mulTranspose(Vector2d vr, Vector3d v1) {
      vr.x = v1.x*m00+v1.y*m10+v1.z*m20;
      vr.y = v1.x*m01+v1.y*m11+v1.z*m21;
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
   public void addObj (Matrix3x2 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix3x2 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix3x2 M1) {
      return (m00 == M1.m00 && m10 == M1.m10 && m20 == M1.m20 &&
              m01 == M1.m01 && m11 == M1.m11 && m21 == M1.m21);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix3x2 M1, double tol) {
      return (abs (m00 - M1.m00) <= tol &&
              abs (m10 - M1.m10) <= tol &&
              abs (m20 - M1.m20) <= tol &&
              abs (m01 - M1.m01) <= tol &&
              abs (m11 - M1.m11) <= tol &&
              abs (m21 - M1.m21) <= tol);
   }

}
