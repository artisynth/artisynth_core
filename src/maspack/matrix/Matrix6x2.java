/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 6 x 2 matrix
 */
public class Matrix6x2 extends DenseMatrixBase 
   implements VectorObject<Matrix6x2> {

   public double m00;
   public double m01;

   public double m10;
   public double m11;

   public double m20;
   public double m21;

   public double m30;
   public double m31;

   public double m40;
   public double m41;

   public double m50;
   public double m51;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix6x2 ZERO = new Matrix6x2();

   /**
    * Creates a new Matrix6x2.
    */
   public Matrix6x2() {
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
            }
            break;
         }
         case 1: {
            switch (j) {
               case 0:
                  return m10;
               case 1:
                  return m11;
            }
            break;
         }
         case 2: {
            switch (j) {
               case 0:
                  return m20;
               case 1:
                  return m21;
            }
            break;
         }
         case 3: {
            switch (j) {
               case 0:
                  return m30;
               case 1:
                  return m31;
            }
            break;
         }
         case 4: {
            switch (j) {
               case 0:
                  return m40;
               case 1:
                  return m41;
            }
            break;
         }
         case 5: {
            switch (j) {
               case 0:
                  return m50;
               case 1:
                  return m51;
            }
            break;
         }
      }
      throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
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
            break;
         }
         case 1: {
            values[0 + off] = m10;
            values[1 + off] = m11;
            break;
         }
         case 2: {
            values[0 + off] = m20;
            values[1 + off] = m21;
            break;
         }
         case 3: {
            values[0 + off] = m30;
            values[1 + off] = m31;
            break;
         }
         case 4: {
            values[0 + off] = m40;
            values[1 + off] = m41;
            break;
         }
         case 5: {
            values[0 + off] = m50;
            values[1 + off] = m51;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
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
            values[2 + off] = m20;
            values[3 + off] = m30;
            values[4 + off] = m40;
            values[5 + off] = m50;
            break;
         }
         case 1: {
            values[0 + off] = m01;
            values[1 + off] = m11;
            values[2 + off] = m21;
            values[3 + off] = m31;
            values[4 + off] = m41;
            values[5 + off] = m51;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Copies a column of this Matrix6x2 into two specified vectors.
    *
    * @param j column index
    * @param v1 vector to recieve the first three values.
    * @param v2 vector to recieve the second three values.
    */
   public void getColumn (int j, Vector3d v1, Vector3d v2) {
      if (j == 0) {
         v1.x = m00;
         v1.y = m10;
         v1.z = m20;
         v2.x = m30;
         v2.y = m40;
         v2.z = m50;
      }
      else if (j == 1) {
         v1.x = m01;
         v1.y = m11;
         v1.z = m21;
         v2.x = m31;
         v2.y = m41;
         v2.z = m51;
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("j=" + j);
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
            }
            break;
         }
         case 2: {
            switch (j) {
               case 0:
                  m20 = value;
                  return;
               case 1:
                  m21 = value;
                  return;
            }
            break;
         }
         case 3: {
            switch (j) {
               case 0:
                  m30 = value;
                  return;
               case 1:
                  m31 = value;
                  return;
            }
            break;
         }
         case 4: {
            switch (j) {
               case 0:
                  m40 = value;
                  return;
               case 1:
                  m41 = value;
                  return;
            }
            break;
         }
         case 5: {
            switch (j) {
               case 0:
                  m50 = value;
                  return;
               case 1:
                  m51 = value;
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
      m10 = values[2];
      m11 = values[3];
      m20 = values[4];
      m21 = values[5];
      m30 = values[6];
      m31 = values[7];
      m40 = values[8];
      m41 = values[9];
      m50 = values[10];
      m51 = values[11];
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
         case 1: {
            m01 = values[0];
            m11 = values[1];
            m21 = values[2];
            m31 = values[3];
            m41 = values[4];
            m51 = values[5];
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
         case 3: {
            m30 = values[0];
            m31 = values[1];
            break;
         }
         case 4: {
            m40 = values[0];
            m41 = values[1];
            break;
         }
         case 5: {
            m50 = values[0];
            m51 = values[1];
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

      m30 = 0;
      m31 = 0;

      m40 = 0;
      m41 = 0;

      m50 = 0;
      m51 = 0;
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M instanceof Matrix6x2) {
         set ((Matrix6x2)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);

         m10 = M.get (1, 0);
         m11 = M.get (1, 1);

         m20 = M.get (2, 0);
         m21 = M.get (2, 1);

         m30 = M.get (3, 0);
         m31 = M.get (3, 1);

         m40 = M.get (4, 0);
         m41 = M.get (4, 1);

         m50 = M.get (5, 0);
         m51 = M.get (5, 1);
      }
   }

   /**
    * Sets the contents of this Matrix6x2 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix6x2 M) {
      m00 = M.m00;
      m01 = M.m01;

      m10 = M.m10;
      m11 = M.m11;

      m20 = M.m20;
      m21 = M.m21;

      m30 = M.m30;
      m31 = M.m31;

      m40 = M.m40;
      m41 = M.m41;

      m50 = M.m50;
      m51 = M.m51;
   }

   /**
    * Sets a column of this Matrix6x2 from two specified vectors.
    *
    * @param j index of the column
    * @param v1 vector specifying first three values.
    * @param v2 vector specifying second three values.
    */
   public void setColumn (int j, Vector3d v1, Vector3d v2) {
      if (j == 0) {
         m00 = v1.x;
         m10 = v1.y;
         m20 = v1.z;
         m30 = v2.x;
         m40 = v2.y;
         m50 = v2.z;
      }
      else if (j == 1) {
         m01 = v1.x;
         m11 = v1.y;
         m21 = v1.z;
         m31 = v2.x;
         m41 = v2.y;
         m51 = v2.z;
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("j=" + j);
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

      m10 *= s;
      m11 *= s;

      m20 *= s;
      m21 *= s;

      m30 *= s;
      m31 *= s;

      m40 *= s;
      m41 *= s;

      m50 *= s;
      m51 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix6x2 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;

      m10 = s * M.m10;
      m11 = s * M.m11;

      m20 = s * M.m20;
      m21 = s * M.m21;

      m30 = s * M.m30;
      m31 = s * M.m31;

      m40 = s * M.m40;
      m41 = s * M.m41;

      m50 = s * M.m50;
      m51 = s * M.m51;
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
      if (M instanceof Matrix6x2) {
         add ((Matrix6x2)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);

         m10 += M.get (1, 0);
         m11 += M.get (1, 1);

         m20 += M.get (2, 0);
         m21 += M.get (2, 1);

         m30 += M.get (3, 0);
         m31 += M.get (3, 1);

         m40 += M.get (4, 0);
         m41 += M.get (4, 1);

         m50 += M.get (5, 0);
         m51 += M.get (5, 1);
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
      if (M instanceof Matrix6x2) {
         scaledAdd (s, (Matrix6x2)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);

         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);

         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);

         m30 += s * M.get (3, 0);
         m31 += s * M.get (3, 1);

         m40 += s * M.get (4, 0);
         m41 += s * M.get (4, 1);

         m50 += s * M.get (5, 0);
         m51 += s * M.get (5, 1);
      }
   }

   /**
    * Adds the contents of a Matrix6x2 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix6x2 M) {
      m00 += M.m00;
      m01 += M.m01;

      m10 += M.m10;
      m11 += M.m11;

      m20 += M.m20;
      m21 += M.m21;

      m30 += M.m30;
      m31 += M.m31;

      m40 += M.m40;
      m41 += M.m41;

      m50 += M.m50;
      m51 += M.m51;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix6x2 M1, Matrix6x2 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;

      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;

      m30 = M1.m30 + M2.m30;
      m31 = M1.m31 + M2.m31;

      m40 = M1.m40 + M2.m40;
      m41 = M1.m41 + M2.m41;

      m50 = M1.m50 + M2.m50;
      m51 = M1.m51 + M2.m51;
   }

   /**
    * Computes s M and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix6x2 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;

      m10 += s * M.m10;
      m11 += s * M.m11;

      m20 += s * M.m20;
      m21 += s * M.m21;

      m30 += s * M.m30;
      m31 += s * M.m31;

      m40 += s * M.m40;
      m41 += s * M.m41;

      m50 += s * M.m50;
      m51 += s * M.m51;
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
   public void scaledAdd (double s, Matrix6x2 M1, Matrix6x2 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;

      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;

      m30 = s * M1.m30 + M2.m30;
      m31 = s * M1.m31 + M2.m31;

      m40 = s * M1.m40 + M2.m40;
      m41 = s * M1.m41 + M2.m41;

      m50 = s * M1.m50 + M2.m50;
      m51 = s * M1.m51 + M2.m51;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix6x2 M) {
      m00 = -M.m00;
      m01 = -M.m01;

      m10 = -M.m10;
      m11 = -M.m11;

      m20 = -M.m20;
      m21 = -M.m21;

      m30 = -M.m30;
      m31 = -M.m31;

      m40 = -M.m40;
      m41 = -M.m41;

      m50 = -M.m50;
      m51 = -M.m51;
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
      if (M instanceof Matrix6x2) {
         sub ((Matrix6x2)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         
         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);

         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);

         m30 -= M.get (3, 0);
         m31 -= M.get (3, 1);

         m40 -= M.get (4, 0);
         m41 -= M.get (4, 1);

         m50 -= M.get (5, 0);
         m51 -= M.get (5, 1);
      }
   }

   /**
    * Subtracts the contents of a Matrix6x2 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix6x2 M) {
      m00 -= M.m00;
      m01 -= M.m01;

      m10 -= M.m10;
      m11 -= M.m11;

      m20 -= M.m20;
      m21 -= M.m21;

      m30 -= M.m30;
      m31 -= M.m31;

      m40 -= M.m40;
      m41 -= M.m41;

      m50 -= M.m50;
      m51 -= M.m51;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix6x2 M1, Matrix6x2 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;

      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;

      m30 = M1.m30 - M2.m30;
      m31 = M1.m31 - M2.m31;

      m40 = M1.m40 - M2.m40;
      m41 = M1.m41 - M2.m41;

      m50 = M1.m50 - M2.m50;
      m51 = M1.m51 - M2.m51;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd6x2 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd6x2 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd6x2 (this, M1, M2);
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix2x6 M) {
      m00 = M.m00;
      m01 = M.m10;
      m10 = M.m01;
      m11 = M.m11;
      m20 = M.m02;
      m21 = M.m12;
      m30 = M.m03;
      m31 = M.m13;
      m40 = M.m04;
      m41 = M.m14;
      m50 = M.m05;
      m51 = M.m15;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix6x2 clone() {
      try {
         return (Matrix6x2)super.clone();
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
   public void addObj (Matrix6x2 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix6x2 M1) {
      scaledAdd (s, M1);
   }

   /**
     * {@inheritDoc}
     */
   public boolean epsilonEquals (Matrix6x2 M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m30 - M1.m30) <= epsilon && abs (m31 - M1.m31) <= epsilon &&
          abs (m40 - M1.m40) <= epsilon && abs (m41 - M1.m41) <= epsilon &&
          abs (m50 - M1.m50) <= epsilon && abs (m51 - M1.m51) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
     * {@inheritDoc}
     */
   public boolean equals (Matrix6x2 M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && 
          (m10 == M1.m10) && (m11 == M1.m11) && 
          (m20 == M1.m20) && (m21 == M1.m21) && 
          (m30 == M1.m30) && (m31 == M1.m31) && 
          (m40 == M1.m40) && (m41 == M1.m41) && 
          (m50 == M1.m50) && (m51 == M1.m51)) {
         return true;
      }
      else {
         return false;
      }
   }

}
