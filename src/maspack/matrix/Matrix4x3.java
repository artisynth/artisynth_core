/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 6 x 3 matrix
 */
public class Matrix4x3 extends DenseMatrixBase
   implements VectorObject<Matrix4x3> {

   public double m00;
   public double m01;
   public double m02;

   public double m10;
   public double m11;
   public double m12;

   public double m20;
   public double m21;
   public double m22;

   public double m30;
   public double m31;
   public double m32;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix4x3 ZERO = new Matrix4x3();

   /**
    * Creates a new Matrix4x3.
    */
   public Matrix4x3() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return 4;
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
         case 2: {
            switch (j) {
               case 0:
                  return m20;
               case 1:
                  return m21;
               case 2:
                  return m22;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            switch (j) {
               case 0:
                  return m30;
               case 1:
                  return m31;
               case 2:
                  return m32;
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
            break;
         }
         case 1: {
            values[0 + off] = m10;
            values[1 + off] = m11;
            values[2 + off] = m12;
            break;
         }
         case 2: {
            values[0 + off] = m20;
            values[1 + off] = m21;
            values[2 + off] = m22;
            break;
         }
         case 3: {
            values[0 + off] = m30;
            values[1 + off] = m31;
            values[2 + off] = m32;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Copies a row of this matrix into a 3-vector.
    * 
    * @param i
    * row index
    * @param row
    * 3-vector into which the row is copied
    */
   public void getRow (int i, Vector3d row) {
      switch (i) {
         case 0: {
            row.x = m00;
            row.y = m01;
            row.z = m02;
            break;
         }
         case 1: {
            row.x = m10;
            row.y = m11;
            row.z = m12;
            break;
         }
         case 2: {
            row.x = m20;
            row.y = m21;
            row.z = m22;
            break;
         }
         case 3: {
            row.x = m30;
            row.y = m31;
            row.z = m32;
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
            break;
         }
         case 1: {
            values[0 + off] = m01;
            values[1 + off] = m11;
            values[2 + off] = m21;
            values[3 + off] = m31;
            break;
         }
         case 2: {
            values[0 + off] = m02;
            values[1 + off] = m12;
            values[2 + off] = m22;
            values[3 + off] = m32;
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
         case 2: {
            switch (j) {
               case 0:
                  m20 = value;
                  return;
               case 1:
                  m21 = value;
                  return;
               case 2:
                  m22 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            switch (j) {
               case 0:
                  m30 = value;
                  return;
               case 1:
                  m31 = value;
                  return;
               case 2:
                  m32 = value;
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
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            m00 = values[0];
            m10 = values[1];
            m20 = values[2];
            m30 = values[3];
            break;
         }
         case 1: {
            m01 = values[0];
            m11 = values[1];
            m21 = values[2];
            m31 = values[3];
            break;
         }
         case 2: {
            m02 = values[0];
            m12 = values[1];
            m22 = values[2];
            m32 = values[3];
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
         case 2: {
            m20 = values[0];
            m21 = values[1];
            m22 = values[2];
            break;
         }
         case 3: {
            m30 = values[0];
            m31 = values[1];
            m32 = values[2];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets a row of this matrix to the specified 3-vector.
    * 
    * @param i
    * row index
    * @param row
    * 3-vector from which the row is copied
    */
   public void setRow (int i, Vector3d row) {
      switch (i) {
         case 0: {
            m00 = row.x;
            m01 = row.y;
            m02 = row.z;
            break;
         }
         case 1: {
            m10 = row.x;
            m11 = row.y;
            m12 = row.z;
            break;
         }
         case 2: {
            m20 = row.x;
            m21 = row.y;
            m22 = row.z;
            break;
         }
         case 3: {
            m30 = row.x;
            m31 = row.y;
            m32 = row.z;
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

      m20 = 0;
      m21 = 0;
      m22 = 0;

      m30 = 0;
      m31 = 0;
      m32 = 0;
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M instanceof Matrix4x3) {
         set ((Matrix4x3)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);

         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
         m12 = M.get (1, 2);

         m20 = M.get (2, 0);
         m21 = M.get (2, 1);
         m22 = M.get (2, 2);

         m30 = M.get (3, 0);
         m31 = M.get (3, 1);
         m32 = M.get (3, 2);
      }
   }

   /**
    * Sets the contents of this Matrix4x3 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix4x3 M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;

      m10 = M.m10;
      m11 = M.m11;
      m12 = M.m12;

      m20 = M.m20;
      m21 = M.m21;
      m22 = M.m22;

      m30 = M.m30;
      m31 = M.m31;
      m32 = M.m32;
   }

   /**
    * Adds v to the 1x3 sub-matrix starting at (0, 0).
    *
    * @param v values to add to the sub-matrix
    */
    public void addSubMatrix00 (Vector3d v) {
      m00 += v.x;
      m01 += v.y;
      m02 += v.z;
   }

   /**
    * Adds M to the 3x3 sub-matrix starting at (1, 0).
    *
    * @param M values to add to the sub-matrix
    */
   public void addSubMatrix10 (Matrix3d M) {
      m10 += M.m00;
      m11 += M.m01;
      m12 += M.m02;

      m20 += M.m10;
      m21 += M.m11;
      m22 += M.m12;

      m30 += M.m20;
      m31 += M.m21;
      m32 += M.m22;
   }  

   /**
    * Sets the 1x3 sub-matrix starting at (0, 0) to v
    *
    * @param v new sub-matrix values
    */
    public void setSubMatrix00 (Vector3d v) {
      m00 = v.x;
      m01 = v.y;
      m02 = v.z;
   }

   /**
    * Sets the 3x3 sub-matrix starting at (1, 0) to M
    *
    * @param M new sub-matrix values
    */
   public void setSubMatrix10 (Matrix3d M) {
      m10 = M.m00;
      m11 = M.m01;
      m12 = M.m02;

      m20 = M.m10;
      m21 = M.m11;
      m22 = M.m12;

      m30 = M.m20;
      m31 = M.m21;
      m32 = M.m22;
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

      m20 *= s;
      m21 *= s;
      m22 *= s;

      m30 *= s;
      m31 *= s;
      m32 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix4x3 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m02 = s * M.m02;

      m10 = s * M.m10;
      m11 = s * M.m11;
      m12 = s * M.m12;

      m20 = s * M.m20;
      m21 = s * M.m21;
      m22 = s * M.m22;

      m30 = s * M.m30;
      m31 = s * M.m31;
      m32 = s * M.m32;
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
      if (M instanceof Matrix4x3) {
         add ((Matrix4x3)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);

         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);

         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
         m22 += M.get (2, 2);

         m30 += M.get (3, 0);
         m31 += M.get (3, 1);
         m32 += M.get (3, 2);
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
      if (M instanceof Matrix4x3) {
         scaledAdd (s, (Matrix4x3)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);

         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m12 += s * M.get (1, 2);

         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);
         m22 += s * M.get (2, 2);

         m30 += s * M.get (3, 0);
         m31 += s * M.get (3, 1);
         m32 += s * M.get (3, 2);
      }
   }

   /**
    * Adds the contents of a Matrix4x3 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix4x3 M) {
      m00 += M.m00;
      m01 += M.m01;
      m02 += M.m02;

      m10 += M.m10;
      m11 += M.m11;
      m12 += M.m12;

      m20 += M.m20;
      m21 += M.m21;
      m22 += M.m22;

      m30 += M.m30;
      m31 += M.m31;
      m32 += M.m32;

   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix4x3 M1, Matrix4x3 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m12 = M1.m12 + M2.m12;

      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;
      m22 = M1.m22 + M2.m22;

      m30 = M1.m30 + M2.m30;
      m31 = M1.m31 + M2.m31;
      m32 = M1.m32 + M2.m32;
   }

   /**
    * Computes s M and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix4x3 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m02 += s * M.m02;

      m10 += s * M.m10;
      m11 += s * M.m11;
      m12 += s * M.m12;

      m20 += s * M.m20;
      m21 += s * M.m21;
      m22 += s * M.m22;

      m30 += s * M.m30;
      m31 += s * M.m31;
      m32 += s * M.m32;
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
   public void scaledAdd (double s, Matrix4x3 M1, Matrix4x3 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;

      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;
      m22 = s * M1.m22 + M2.m22;

      m30 = s * M1.m30 + M2.m30;
      m31 = s * M1.m31 + M2.m31;
      m32 = s * M1.m32 + M2.m32;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix4x3 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m02 = -M.m02;

      m10 = -M.m10;
      m11 = -M.m11;
      m12 = -M.m12;

      m20 = -M.m20;
      m21 = -M.m21;
      m22 = -M.m22;

      m30 = -M.m30;
      m31 = -M.m31;
      m32 = -M.m32;
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
      if (M instanceof Matrix4x3) {
         sub ((Matrix4x3)M);
      }
      else {
         if (M.rowSize() != 4 || M.colSize() != 3) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);

         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);

         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
         m22 -= M.get (2, 2);

         m30 -= M.get (3, 0);
         m31 -= M.get (3, 1);
         m32 -= M.get (3, 2);
      }
   }

   /**
    * Subtracts the contents of a Matrix4x3 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix4x3 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m02 -= M.m02;

      m10 -= M.m10;
      m11 -= M.m11;
      m12 -= M.m12;

      m20 -= M.m20;
      m21 -= M.m21;
      m22 -= M.m22;

      m30 -= M.m30;
      m31 -= M.m31;
      m32 -= M.m32;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix4x3 M1, Matrix4x3 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m12 = M1.m12 - M2.m12;

      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;
      m22 = M1.m22 - M2.m22;

      m30 = M1.m30 - M2.m30;
      m31 = M1.m31 - M2.m31;
      m32 = M1.m32 - M2.m32;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd4x3 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd4x3 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd4x3 (this, M1, M2);
   }

   /**
    * Multiplies the 4x4 matrix M1 by the 4x3 matrix M2 and adds the result to
    * this matrix. This special method has been added to support operations
    * involving quaternions.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix4d M1, Matrix4x3 M2) {
      double x00, x01, x02;
      double x10, x11, x12;
      double x20, x21, x22;
      double x30, x31, x32;

      x00 = M1.m00*M2.m00 + M1.m01*M2.m10 + M1.m02*M2.m20 + M1.m03*M2.m30;
      x01 = M1.m00*M2.m01 + M1.m01*M2.m11 + M1.m02*M2.m21 + M1.m03*M2.m31;
      x02 = M1.m00*M2.m02 + M1.m01*M2.m12 + M1.m02*M2.m22 + M1.m03*M2.m32;

      x10 = M1.m10*M2.m00 + M1.m11*M2.m10 + M1.m12*M2.m20 + M1.m13*M2.m30;
      x11 = M1.m10*M2.m01 + M1.m11*M2.m11 + M1.m12*M2.m21 + M1.m13*M2.m31;
      x12 = M1.m10*M2.m02 + M1.m11*M2.m12 + M1.m12*M2.m22 + M1.m13*M2.m32;

      x20 = M1.m20*M2.m00 + M1.m21*M2.m10 + M1.m22*M2.m20 + M1.m23*M2.m30;
      x21 = M1.m20*M2.m01 + M1.m21*M2.m11 + M1.m22*M2.m21 + M1.m23*M2.m31;
      x22 = M1.m20*M2.m02 + M1.m21*M2.m12 + M1.m22*M2.m22 + M1.m23*M2.m32;

      x30 = M1.m30*M2.m00 + M1.m31*M2.m10 + M1.m32*M2.m20 + M1.m33*M2.m30;
      x31 = M1.m30*M2.m01 + M1.m31*M2.m11 + M1.m32*M2.m21 + M1.m33*M2.m31;
      x32 = M1.m30*M2.m02 + M1.m31*M2.m12 + M1.m32*M2.m22 + M1.m33*M2.m32;

      m00 += x00;
      m01 += x01;
      m02 += x02;

      m10 += x10;
      m11 += x11;
      m12 += x12;

      m20 += x20;
      m21 += x21;
      m22 += x22;

      m30 += x30;
      m31 += x31;
      m32 += x32;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix3x4 M) {
      m00 = M.m00;
      m01 = M.m10;
      m02 = M.m20;
      m10 = M.m01;
      m11 = M.m11;
      m12 = M.m21;
      m20 = M.m02;
      m21 = M.m12;
      m22 = M.m22;
      m30 = M.m03;
      m31 = M.m13;
      m32 = M.m23;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix4x3 clone() {
      try {
         return (Matrix4x3)super.clone();
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
   public void addObj (Matrix4x3 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix4x3 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix4x3 M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && 

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon && 

          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m22 - M1.m22) <= epsilon && 

          abs (m30 - M1.m30) <= epsilon && abs (m31 - M1.m31) <= epsilon &&
          abs (m32 - M1.m32) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix4x3 M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m10 == M1.m10) && (m11 == M1.m11) && (m12 == M1.m12) &&
          (m20 == M1.m20) && (m21 == M1.m21) && (m22 == M1.m22) &&
          (m30 == M1.m30) && (m31 == M1.m31) && (m32 == M1.m32)) {
         return true;
      }
      else {
         return false;
      }
   }
}
