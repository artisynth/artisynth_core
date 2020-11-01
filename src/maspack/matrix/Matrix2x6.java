/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 2 x 6 matrix
 */
public class Matrix2x6 extends DenseMatrixBase
   implements VectorObject<Matrix2x6> {

   public double m00;
   public double m01;
   public double m02;
   public double m03;
   public double m04;
   public double m05;

   public double m10;
   public double m11;
   public double m12;
   public double m13;
   public double m14;
   public double m15;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix2x6 ZERO = new Matrix2x6();

   /**
    * Creates a new Matrix2x6.
    */
   public Matrix2x6() {
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
      return 6;
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
               case 3:
                  return m03;
               case 4:
                  return m04;
               case 5:
                  return m05;
            }
            break;
         }
         case 1: {
            switch (j) {
               case 0:
                  return m10;
               case 1:
                  return m11;
               case 2:
                  return m12;
               case 3:
                  return m13;
               case 4:
                  return m14;
               case 5:
                  return m15;
            }
            break;
         }
      }
      throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values) {
      getColumn (j, values, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values, int off) {
      switch (j) {
         case 0: {
            values[0 + off] = m00;
            values[1 + off] = m10;
            break;
         }
         case 1: {
            values[0 + off] = m01;
            values[1 + off] = m11;
            break;
         }
         case 2: {
            values[0 + off] = m02;
            values[1 + off] = m12;
            break;
         }
         case 3: {
            values[0 + off] = m03;
            values[1 + off] = m13;
            break;
         }
         case 4: {
            values[0 + off] = m04;
            values[1 + off] = m14;
            break;
         }
         case 5: {
            values[0 + off] = m05;
            values[1 + off] = m15;
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
   public void getRow (int i, double[] values) {
      getRow (i, values, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void getRow (int i, double[] values, int off) {
      switch (i) {
         case 0: {
            values[0 + off] = m00;
            values[1 + off] = m01;
            values[2 + off] = m02;
            values[3 + off] = m03;
            values[4 + off] = m04;
            values[5 + off] = m05;
            break;
         }
         case 1: {
            values[0 + off] = m10;
            values[1 + off] = m11;
            values[2 + off] = m12;
            values[3 + off] = m13;
            values[4 + off] = m14;
            values[5 + off] = m15;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Copies a row of this Matrix6x2 into two specified vectors.
    *
    * @param i row index
    * @param v1 vector to recieve the first three values.
    * @param v2 vector to recieve the second three values.
    */
   public void getRow (int i, Vector3d v1, Vector3d v2) {
      if (i == 0) {
         v1.x = m00;
         v1.y = m01;
         v1.z = m02;
         v2.x = m03;
         v2.y = m04;
         v2.z = m05;
      }
      else if (i == 1) {
         v1.x = m10;
         v1.y = m11;
         v1.z = m12;
         v2.x = m13;
         v2.y = m14;
         v2.z = m15;
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("i=" + i);
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
               case 3:
                  m03 = value;
                  return;
               case 4:
                  m04 = value;
                  return;
               case 5:
                  m05 = value;
                  return;
            }
            break;
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
               case 3:
                  m13 = value;
                  return;
               case 4:
                  m14 = value;
                  return;
               case 5:
                  m15 = value;
                  return;
            }
            break;
         }
      }
      throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] values) {
      m00 = values[0];
      m01 = values[1];
      m02 = values[2];
      m03 = values[3];
      m04 = values[4];
      m05 = values[5];
      m10 = values[6];
      m11 = values[7];
      m12 = values[8];
      m13 = values[9];
      m14 = values[10];
      m15 = values[11];
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
         case 3: {
            m03 = values[0];
            m13 = values[1];
            break;
         }
         case 4: {
            m04 = values[0];
            m14 = values[1];
            break;
         }
         case 5: {
            m05 = values[0];
            m15 = values[1];
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
         case 1: {
            m10 = values[0];
            m11 = values[1];
            m12 = values[2];
            m13 = values[3];
            m14 = values[4];
            m15 = values[5];
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

      m10 = 0;
      m11 = 0;
      m12 = 0;
      m13 = 0;
      m14 = 0;
      m15 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix2x6) {
         set ((Matrix2x6)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);
         m03 = M.get (0, 3);
         m04 = M.get (0, 4);
         m05 = M.get (0, 5);

         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
         m12 = M.get (1, 2);
         m13 = M.get (1, 3);
         m14 = M.get (1, 4);
         m15 = M.get (1, 5);
      }
   }

   /**
    * Sets the contents of this Matrix2x6 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix2x6 M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;
      m03 = M.m03;
      m04 = M.m04;
      m05 = M.m05;

      m10 = M.m10;
      m11 = M.m11;
      m12 = M.m12;
      m13 = M.m13;
      m14 = M.m14;
      m15 = M.m15;
   }

   /**
    * Sets a row of this Matrix6x2 from two specified vectors.
    * 
    * @param i index of the row
    * @param v1 vector specifying first three values.
    * @param v2 vector specifying second three values.
    */
   public void setRow (int i, Vector3d v1, Vector3d v2) {
      if (i == 0) {
         m00 = v1.x;
         m01 = v1.y;
         m02 = v1.z;
         m03 = v2.x;
         m04 = v2.y;
         m05 = v2.z;
      }
      else if (i == 1) {
         m10 = v1.x;
         m11 = v1.y;
         m12 = v1.z;
         m13 = v2.x;
         m14 = v2.y;
         m15 = v2.z;
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("i=" + i);
      }
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

      m10 *= s;
      m11 *= s;
      m12 *= s;
      m13 *= s;
      m14 *= s;
      m15 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix2x6 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m02 = s * M.m02;
      m03 = s * M.m03;
      m04 = s * M.m04;
      m05 = s * M.m05;

      m10 = s * M.m10;
      m11 = s * M.m11;
      m12 = s * M.m12;
      m13 = s * M.m13;
      m14 = s * M.m14;
      m15 = s * M.m15;
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
      if (M instanceof Matrix2x6) {
         add ((Matrix2x6)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m03 += M.get (0, 3);
         m04 += M.get (0, 4);
         m05 += M.get (0, 5);

         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);
         m13 += M.get (1, 3);
         m14 += M.get (1, 4);
         m15 += M.get (1, 5);
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
      if (M instanceof Matrix2x6) {
         scaledAdd (s, (Matrix2x6)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
         m03 += s * M.get (0, 3);
         m04 += s * M.get (0, 4);
         m05 += s * M.get (0, 5);

         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m12 += s * M.get (1, 2);
         m13 += s * M.get (1, 3);
         m14 += s * M.get (1, 4);
         m15 += s * M.get (1, 5);
      }
   }

   /**
    * Adds the contents of a Matrix2x6 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix2x6 M) {
      m00 += M.m00;
      m01 += M.m01;
      m02 += M.m02;
      m03 += M.m03;
      m04 += M.m04;
      m05 += M.m05;

      m10 += M.m10;
      m11 += M.m11;
      m12 += M.m12;
      m13 += M.m13;
      m14 += M.m14;
      m15 += M.m15;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix2x6 M1, Matrix2x6 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;
      m03 = M1.m03 + M2.m03;
      m04 = M1.m04 + M2.m04;
      m05 = M1.m05 + M2.m05;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m12 = M1.m12 + M2.m12;
      m13 = M1.m13 + M2.m13;
      m14 = M1.m14 + M2.m14;
      m15 = M1.m15 + M2.m15;
   }

   /**
    * Computes s M and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix2x6 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m02 += s * M.m02;
      m03 += s * M.m03;
      m04 += s * M.m04;
      m05 += s * M.m05;

      m10 += s * M.m10;
      m11 += s * M.m11;
      m12 += s * M.m12;
      m13 += s * M.m13;
      m14 += s * M.m14;
      m15 += s * M.m15;
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
   public void scaledAdd (double s, Matrix2x6 M1, Matrix2x6 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;
      m03 = s * M1.m03 + M2.m03;
      m04 = s * M1.m04 + M2.m04;
      m05 = s * M1.m05 + M2.m05;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;
      m13 = s * M1.m13 + M2.m13;
      m14 = s * M1.m14 + M2.m14;
      m15 = s * M1.m15 + M2.m15;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix2x6 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m02 = -M.m02;
      m03 = -M.m03;
      m04 = -M.m04;
      m05 = -M.m05;

      m10 = -M.m10;
      m11 = -M.m11;
      m12 = -M.m12;
      m13 = -M.m13;
      m14 = -M.m14;
      m15 = -M.m15;
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
      if (M instanceof Matrix2x6) {
         sub ((Matrix2x6)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m03 -= M.get (0, 3);
         m04 -= M.get (0, 4);
         m05 -= M.get (0, 5);

         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);
         m13 -= M.get (1, 3);
         m14 -= M.get (1, 4);
         m15 -= M.get (1, 5);
      }
   }

   /**
    * Subtracts the contents of a Matrix2x6 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix2x6 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m02 -= M.m02;
      m03 -= M.m03;
      m04 -= M.m04;
      m05 -= M.m05;

      m10 -= M.m10;
      m11 -= M.m11;
      m12 -= M.m12;
      m13 -= M.m13;
      m14 -= M.m14;
      m15 -= M.m15;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix2x6 M1, Matrix2x6 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;
      m03 = M1.m03 - M2.m03;
      m04 = M1.m04 - M2.m04;
      m05 = M1.m05 - M2.m05;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m12 = M1.m12 - M2.m12;
      m13 = M1.m13 - M2.m13;
      m14 = M1.m14 - M2.m14;
      m15 = M1.m15 - M2.m15;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd2x6 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd2x6 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd2x6 (this, M1, M2);
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix6x2 M) {
      m00 = M.m00;
      m10 = M.m01;
      m01 = M.m10;
      m11 = M.m11;
      m02 = M.m20;
      m12 = M.m21;
      m03 = M.m30;
      m13 = M.m31;
      m04 = M.m40;
      m14 = M.m41;
      m05 = M.m50;
      m15 = M.m51;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix2x6 clone() {
      try {
         return (Matrix2x6)super.clone();
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
   public void addObj (Matrix2x6 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix2x6 M1) {
      scaledAdd (s, M1);
   }

   /**
     * {@inheritDoc}
     */
   public boolean epsilonEquals (Matrix2x6 M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && abs (m03 - M1.m03) <= epsilon &&
          abs (m04 - M1.m04) <= epsilon && abs (m05 - M1.m05) <= epsilon &&

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon && abs (m13 - M1.m13) <= epsilon &&
          abs (m14 - M1.m14) <= epsilon && abs (m15 - M1.m15) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
     * {@inheritDoc}
     */
   public boolean equals (Matrix2x6 M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m03 == M1.m03) && (m04 == M1.m04) && (m05 == M1.m05) &&

          (m10 == M1.m10) && (m11 == M1.m11) && (m12 == M1.m12) &&
          (m13 == M1.m13) && (m14 == M1.m14) && (m15 == M1.m15)) {
         return true;
      }
      else {
         return false;
      }
   }

}
