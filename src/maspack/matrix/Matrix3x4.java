/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 3 x 4 matrix
 */
public class Matrix3x4 extends DenseMatrixBase
   implements VectorObject<Matrix3x4> {

   public double m00;
   public double m01;
   public double m02;
   public double m03;

   public double m10;
   public double m11;
   public double m12;
   public double m13;

   public double m20;
   public double m21;
   public double m22;
   public double m23;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix3x4 ZERO = new Matrix3x4();

   /**
    * Creates a new Matrix3x4.
    */
   public Matrix3x4() {
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
      return 4;
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
               case 3:
                  return m13;
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
               case 3:
                  return m23;
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
            break;
         }
         case 1: {
            values[0 + off] = m01;
            values[1 + off] = m11;
            values[2 + off] = m21;
            break;
         }
         case 2: {
            values[0 + off] = m02;
            values[1 + off] = m12;
            values[2 + off] = m22;
            break;
         }
         case 3: {
            values[0 + off] = m03;
            values[1 + off] = m13;
            values[2 + off] = m23;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Copies a column of this matrix into a 3-vector.
    * 
    * @param j
    * column index
    * @param col
    * 3-vector into which the column is copied
    */
   public void getColumn (int j, Vector3d col) {
      switch (j) {
         case 0: {
            col.x = m00;
            col.y = m10;
            col.z = m20;
            break;
         }
         case 1: {
            col.x = m01;
            col.y = m11;
            col.z = m21;
            break;
         }
         case 2: {
            col.x = m02;
            col.y = m12;
            col.z = m22;
            break;
         }
         case 3: {
            col.x = m03;
            col.y = m13;
            col.z = m23;
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
            break;
         }
         case 1: {
            values[0 + off] = m10;
            values[1 + off] = m11;
            values[2 + off] = m12;
            values[3 + off] = m13;
            break;
         }
         case 2: {
            values[0 + off] = m20;
            values[1 + off] = m21;
            values[2 + off] = m22;
            values[3 + off] = m23;
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
               case 3:
                  m13 = value;
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
               case 3:
                  m23 = value;
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
   public void setRow (int i, double[] values) {
      switch (i) {
         case 0: {
            m00 = values[0];
            m01 = values[1];
            m02 = values[2];
            m03 = values[3];
            break;
         }
         case 1: {
            m10 = values[0];
            m11 = values[1];
            m12 = values[2];
            m13 = values[3];
            break;
         }
         case 2: {
            m20 = values[0];
            m21 = values[1];
            m22 = values[2];
            m23 = values[3];
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
         case 2: {
            m02 = values[0];
            m12 = values[1];
            m22 = values[2];
            break;
         }
         case 3: {
            m03 = values[0];
            m13 = values[1];
            m23 = values[2];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Sets a column of this matrix to the specified 3-vector.
    * 
    * @param j
    * column index
    * @param col
    * 3-vector from which the column is copied
    */
   public void setColumn (int j, Vector3d col) {
      switch (j) {
         case 0: {
            m00 = col.x;
            m10 = col.y;
            m20 = col.z;
            break;
         }
         case 1: {
            m01 = col.x;
            m11 = col.y;
            m21 = col.z;
            break;
         }
         case 2: {
            m02 = col.x;
            m12 = col.y;
            m22 = col.z;
            break;
         }
         case 3: {
            m03 = col.x;
            m13 = col.y;
            m23 = col.z;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
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

      m10 = 0;
      m11 = 0;
      m12 = 0;
      m13 = 0;

      m20 = 0;
      m21 = 0;
      m22 = 0;
      m23 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix3x4) {
         set ((Matrix3x4)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);
         m03 = M.get (0, 3);

         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
         m12 = M.get (1, 2);
         m13 = M.get (1, 3);

         m20 = M.get (2, 0);
         m21 = M.get (2, 1);
         m22 = M.get (2, 2);
         m23 = M.get (2, 3);
      }
   }

   /**
    * Sets the contents of this Matrix3x4 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix3x4 M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;
      m03 = M.m03;

      m10 = M.m10;
      m11 = M.m11;
      m12 = M.m12;
      m13 = M.m13;

      m20 = M.m20;
      m21 = M.m21;
      m22 = M.m22;
      m23 = M.m23;
   }

   /**
    * Adds v to the 3x1 sub-matrix starting at (0, 0).
    *
    * @param v values to add to the sub-matrix
    */
    public void addSubMatrix00 (Vector3d v) {
      m00 += v.x;
      m10 += v.y;
      m20 += v.z;
   }

   /**
    * Adds M to the 3x3 sub-matrix starting at (0, 1)
    *
    * @param M values to add to the sub-matrix
    */
   public void addSubMatrix01 (Matrix3d M) {
      m01 += M.m00;
      m02 += M.m01;
      m03 += M.m02;

      m11 += M.m10;
      m12 += M.m11;
      m13 += M.m12;

      m21 += M.m20;
      m22 += M.m21;
      m23 += M.m22;
   }  

   /**
    * Sets the 3x1 sub-matrix starting at (0, 0) to v
    *
    * @param v new sub-matrix values
    */
    public void setSubMatrix00 (Vector3d v) {
      m00 = v.x;
      m10 = v.y;
      m20 = v.z;
   }

   /**
    * Sets the 3x3 sub-matrix starting at (0, 1) to M
    *
    * @param M new sub-matrix values
    */
   public void setSubMatrix01 (Matrix3d M) {
      m01 = M.m00;
      m02 = M.m01;
      m03 = M.m02;

      m11 = M.m10;
      m12 = M.m11;
      m13 = M.m12;

      m21 = M.m20;
      m22 = M.m21;
      m23 = M.m22;
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

      m10 *= s;
      m11 *= s;
      m12 *= s;
      m13 *= s;

      m20 *= s;
      m21 *= s;
      m22 *= s;
      m23 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix3x4 M) {
      m00 = s * M.m00;
      m01 = s * M.m01;
      m02 = s * M.m02;
      m03 = s * M.m03;

      m10 = s * M.m10;
      m11 = s * M.m11;
      m12 = s * M.m12;
      m13 = s * M.m13;

      m20 = s * M.m20;
      m21 = s * M.m21;
      m22 = s * M.m22;
      m23 = s * M.m23;
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
      if (M instanceof Matrix3x4) {
         add ((Matrix3x4)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m03 += M.get (0, 3);

         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);
         m13 += M.get (1, 3);

         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
         m22 += M.get (2, 2);
         m23 += M.get (2, 3);
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
      if (M instanceof Matrix3x4) {
         scaledAdd (s, (Matrix3x4)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m01 += s * M.get (0, 1);
         m02 += s * M.get (0, 2);
         m03 += s * M.get (0, 3);

         m10 += s * M.get (1, 0);
         m11 += s * M.get (1, 1);
         m12 += s * M.get (1, 2);
         m13 += s * M.get (1, 3);

         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);
         m22 += s * M.get (2, 2);
         m23 += s * M.get (2, 3);
      }
   }

   /**
    * Adds the contents of a Matrix3x4 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix3x4 M) {
      m00 += M.m00;
      m01 += M.m01;
      m02 += M.m02;
      m03 += M.m03;

      m10 += M.m10;
      m11 += M.m11;
      m12 += M.m12;
      m13 += M.m13;

      m20 += M.m20;
      m21 += M.m21;
      m22 += M.m22;
      m23 += M.m23;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix3x4 M1, Matrix3x4 M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;
      m03 = M1.m03 + M2.m03;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m12 = M1.m12 + M2.m12;
      m13 = M1.m13 + M2.m13;

      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;
      m22 = M1.m22 + M2.m22;
      m23 = M1.m23 + M2.m23;
   }

   /**
    * Computes s M and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix3x4 M) {
      m00 += s * M.m00;
      m01 += s * M.m01;
      m02 += s * M.m02;
      m03 += s * M.m03;

      m10 += s * M.m10;
      m11 += s * M.m11;
      m12 += s * M.m12;
      m13 += s * M.m13;

      m20 += s * M.m20;
      m21 += s * M.m21;
      m22 += s * M.m22;
      m23 += s * M.m23;
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
   public void scaledAdd (double s, Matrix3x4 M1, Matrix3x4 M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;
      m03 = s * M1.m03 + M2.m03;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;
      m13 = s * M1.m13 + M2.m13;

      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;
      m22 = s * M1.m22 + M2.m22;
      m23 = s * M1.m23 + M2.m23;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix3x4 M) {
      m00 = -M.m00;
      m01 = -M.m01;
      m02 = -M.m02;
      m03 = -M.m03;

      m10 = -M.m10;
      m11 = -M.m11;
      m12 = -M.m12;
      m13 = -M.m13;

      m20 = -M.m20;
      m21 = -M.m21;
      m22 = -M.m22;
      m23 = -M.m23;
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
      if (M instanceof Matrix3x4) {
         sub ((Matrix3x4)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 4) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m03 -= M.get (0, 3);

         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);
         m13 -= M.get (1, 3);

         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
         m22 -= M.get (2, 2);
         m23 -= M.get (2, 3);
      }
   }

   /**
    * Subtracts the contents of a Matrix3x4 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix3x4 M) {
      m00 -= M.m00;
      m01 -= M.m01;
      m02 -= M.m02;
      m03 -= M.m03;

      m10 -= M.m10;
      m11 -= M.m11;
      m12 -= M.m12;
      m13 -= M.m13;

      m20 -= M.m20;
      m21 -= M.m21;
      m22 -= M.m22;
      m23 -= M.m23;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix3x4 M1, Matrix3x4 M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;
      m03 = M1.m03 - M2.m03;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m12 = M1.m12 - M2.m12;
      m13 = M1.m13 - M2.m13;

      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;
      m22 = M1.m22 - M2.m22;
      m23 = M1.m23 - M2.m23;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd3x4 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd3x4 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd3x4 (this, M1, M2);
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
      Matrix3d MR, Matrix3x4 M1, double[] D, Matrix3x4 M2) {

      double d0 = D[0];
      double d1 = D[1];
      double d2 = D[2];
      double d3 = D[3];

      double T00 = d0*M2.m00;
      double T01 = d0*M2.m10;
      double T02 = d0*M2.m20;

      double T10 = d1*M2.m01;
      double T11 = d1*M2.m11;
      double T12 = d1*M2.m21;

      double T20 = d2*M2.m02;
      double T21 = d2*M2.m12;
      double T22 = d2*M2.m22;

      double T30 = d3*M2.m03;
      double T31 = d3*M2.m13;
      double T32 = d3*M2.m23;

      MR.m00 += M1.m00*T00 + M1.m01*T10 + M1.m02*T20 + M1.m03*T30;
      MR.m01 += M1.m00*T01 + M1.m01*T11 + M1.m02*T21 + M1.m03*T31;
      MR.m02 += M1.m00*T02 + M1.m01*T12 + M1.m02*T22 + M1.m03*T32;

      MR.m10 += M1.m10*T00 + M1.m11*T10 + M1.m12*T20 + M1.m13*T30;
      MR.m11 += M1.m10*T01 + M1.m11*T11 + M1.m12*T21 + M1.m13*T31;
      MR.m12 += M1.m10*T02 + M1.m11*T12 + M1.m12*T22 + M1.m13*T32;

      MR.m20 += M1.m20*T00 + M1.m21*T10 + M1.m22*T20 + M1.m23*T30;
      MR.m21 += M1.m20*T01 + M1.m21*T11 + M1.m22*T21 + M1.m23*T31;
      MR.m22 += M1.m20*T02 + M1.m21*T12 + M1.m22*T22 + M1.m23*T32;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix4x3 M) {
      m00 = M.m00;
      m10 = M.m01;
      m20 = M.m02;
      m01 = M.m10;
      m11 = M.m11;
      m21 = M.m12;
      m02 = M.m20;
      m12 = M.m21;
      m22 = M.m22;
      m03 = M.m30;
      m13 = M.m31;
      m23 = M.m32;
   }

/**
    * Multiples this matrix by the vector v1 and places the result in vr
    * @param vr result of multiplication
    * @param v1 vector to transform
    */
   public void mul (Point3d vr, Vector4d v1) {
      vr.x = m00*v1.x + m01*v1.y + m02*v1.z + m03*v1.w;
      vr.y = m10*v1.x + m11*v1.y + m12*v1.z + m13*v1.w;
      vr.z = m20*v1.x + m21*v1.y + m22*v1.z + m23*v1.w;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix3x4 clone() {
      try {
         return (Matrix3x4)super.clone();
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
   public void addObj (Matrix3x4 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix3x4 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix3x4 M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && abs (m03 - M1.m03) <= epsilon &&

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon && abs (m13 - M1.m13) <= epsilon &&

          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m22 - M1.m22) <= epsilon && abs (m23 - M1.m23) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix3x4 M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m03 == M1.m03) && (m10 == M1.m10) && (m11 == M1.m11) &&
          (m12 == M1.m12) && (m13 == M1.m13) && (m20 == M1.m20) &&
          (m21 == M1.m21) && (m22 == M1.m22) && (m23 == M1.m23)) {
         return true;
      }
      else {
         return false;
      }
   }

}
