/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;
import maspack.util.Clonable;

/**
 * Base class for 4 x 4 matrices in which the elements are stored as explicit
 * fields. A primary motivation for such objects is computational speed.
 */
public abstract class Matrix4dBase extends DenseMatrixBase implements Clonable {

   /**
    * Matrix element (0,0)
    */
   public double m00;

   /**
    * Matrix element (0,1)
    */
   public double m01;

   /**
    * Matrix element (0,2)
    */
   public double m02;

   /**
    * Matrix element (0,3)
    */
   public double m03;

   /**
    * Matrix element (1,0)
    */
   public double m10;

   /**
    * Matrix element (1,1)
    */
   public double m11;

   /**
    * Matrix element (1,2)
    */
   public double m12;

   /**
    * Matrix element (1,3)
    */
   public double m13;

   /**
    * Matrix element (2,0)
    */
   public double m20;

   /**
    * Matrix element (2,1)
    */
   public double m21;

   /**
    * Matrix element (2,2)
    */
   public double m22;

   /**
    * Matrix element (2,3)
    */
   public double m23;

   /**
    * Matrix element (3,0)
    */
   public double m30;

   /**
    * Matrix element (3,1)
    */
   public double m31;

   /**
    * Matrix element (3,2)
    */
   public double m32;

   /**
    * Matrix element (3,3)
    */
   public double m33;

   /**
    * Returns the number of rows in this matrix (which is always 4).
    * 
    * @return 4
    */
   public final int rowSize() {
      return 4;
   }

   /**
    * Returns the number of columns in this matrix (which is always 4).
    * 
    * @return 4
    */
   public final int colSize() {
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
         case 3: {
            switch (j) {
               case 0:
                  return m30;
               case 1:
                  return m31;
               case 2:
                  return m32;
               case 3:
                  return m33;
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
   public void get (double[] values) {
      values[0] = m00;
      values[1] = m01;
      values[2] = m02;
      values[3] = m03;

      values[4] = m10;
      values[5] = m11;
      values[6] = m12;
      values[7] = m13;

      values[8] = m20;
      values[9] = m21;
      values[10] = m22;
      values[11] = m23;

      values[12] = m30;
      values[13] = m31;
      values[14] = m32;
      values[15] = m33;
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
         case 3: {
            values[0 + off] = m03;
            values[1 + off] = m13;
            values[2 + off] = m23;
            values[3 + off] = m33;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Copies a column of this matrix into a 4-vector.
    * 
    * @param j
    * column index
    * @param col
    * 4-vector into which the column is copied
    */
   public void getColumn (int j, Vector4d col) {
      switch (j) {
         case 0: {
            col.x = m00;
            col.y = m10;
            col.z = m20;
            col.w = m30;
            break;
         }
         case 1: {
            col.x = m01;
            col.y = m11;
            col.z = m21;
            col.w = m31;
            break;
         }
         case 2: {
            col.x = m02;
            col.y = m12;
            col.z = m22;
            col.w = m32;
            break;
         }
         case 3: {
            col.x = m03;
            col.y = m13;
            col.z = m23;
            col.w = m33;
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
         case 3: {
            values[0 + off] = m30;
            values[1 + off] = m31;
            values[2 + off] = m32;
            values[3 + off] = m33;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Copies a row of this matrix into a 4-vector.
    * 
    * @param i
    * row index
    * @param row
    * 4-vector into which the row is copied
    */
   public void getRow (int i, Vector4d row) {
      switch (i) {
         case 0: {
            row.x = m00;
            row.y = m01;
            row.z = m02;
            row.w = m03;
            break;
         }
         case 1: {
            row.x = m10;
            row.y = m11;
            row.z = m12;
            row.w = m13;
            break;
         }
         case 2: {
            row.x = m20;
            row.y = m21;
            row.z = m22;
            row.w = m23;
            break;
         }
         case 3: {
            row.x = m30;
            row.y = m31;
            row.z = m32;
            row.w = m33;
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
   final public void set (int i, int j, double value) {
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
               case 3:
                  m33 = value;
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
   public void set (double[] vals) {
      m00 = vals[0];
      m01 = vals[1];
      m02 = vals[2];
      m03 = vals[3];

      m10 = vals[4];
      m11 = vals[5];
      m12 = vals[6];
      m13 = vals[7];

      m20 = vals[8];
      m21 = vals[9];
      m22 = vals[10];
      m23 = vals[11];

      m30 = vals[12];
      m31 = vals[13];
      m32 = vals[14];
      m33 = vals[15];
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
         case 3: {
            m03 = values[0];
            m13 = values[1];
            m23 = values[2];
            m33 = values[3];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Sets a column of this matrix to the specified 4-vector.
    * 
    * @param j
    * column index
    * @param col
    * 4-vector from which the column is copied
    */
   public void setColumn (int j, Vector4d col) {
      switch (j) {
         case 0: {
            m00 = col.x;
            m10 = col.y;
            m20 = col.z;
            m30 = col.w;
            break;
         }
         case 1: {
            m01 = col.x;
            m11 = col.y;
            m21 = col.z;
            m31 = col.w;
            break;
         }
         case 2: {
            m02 = col.x;
            m12 = col.y;
            m22 = col.z;
            m32 = col.w;
            break;
         }
         case 3: {
            m03 = col.x;
            m13 = col.y;
            m23 = col.z;
            m33 = col.w;
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
         case 3: {
            m30 = values[0];
            m31 = values[1];
            m32 = values[2];
            m33 = values[3];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets a row of this matrix to the specified 4-vector.
    * 
    * @param i
    * row index
    * @param row
    * 4-vector from which the row is copied
    */
   public void setRow (int i, Vector4d row) {
      switch (i) {
         case 0: {
            m00 = row.x;
            m01 = row.y;
            m02 = row.z;
            m03 = row.w;
            break;
         }
         case 1: {
            m10 = row.x;
            m11 = row.y;
            m12 = row.z;
            m13 = row.w;
            break;
         }
         case 2: {
            m20 = row.x;
            m21 = row.y;
            m22 = row.z;
            m23 = row.w;
            break;
         }
         case 3: {
            m30 = row.x;
            m31 = row.y;
            m32 = row.z;
            m33 = row.w;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets the values of this matrix to those of matrix M.
    * 
    * @param M
    * matrix whose values are to be copied
    */
   public void set (Matrix4dBase M) {
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

      m30 = M.m30;
      m31 = M.m31;
      m32 = M.m32;
      m33 = M.m33;
   }

   /**
    * Sets the columns of this matrix to the vectors v0, v1, v2, and v3.
    * 
    * @param v0
    * values for the first column
    * @param v1
    * values for the second column
    * @param v2
    * values for the third column
    * @param v3
    * values for the fourth column
    */
   protected void setColumns (Vector4d v0, Vector4d v1, Vector4d v2, Vector4d v3) {
      m00 = v0.x;
      m10 = v0.y;
      m20 = v0.z;
      m30 = v0.w;

      m01 = v1.x;
      m11 = v1.y;
      m21 = v1.z;
      m31 = v1.w;

      m02 = v2.x;
      m12 = v2.y;
      m22 = v2.z;
      m32 = v2.w;

      m02 = v3.x;
      m12 = v3.y;
      m22 = v3.z;
      m32 = v3.w;
   }

   /**
    * Sets the rows of this matrix to the vectors v0, v1, v2, and v3.
    * 
    * @param v0
    * values for the first row
    * @param v1
    * values for the second row
    * @param v2
    * values for the third row
    * @param v3
    * values for the fourth row
    */
   protected void setRows (Vector4d v0, Vector4d v1, Vector4d v2, Vector4d v3) {
      m00 = v0.x;
      m01 = v0.y;
      m02 = v0.z;
      m03 = v0.w;

      m10 = v1.x;
      m11 = v1.y;
      m12 = v1.z;
      m13 = v1.w;

      m20 = v2.x;
      m21 = v2.y;
      m22 = v2.z;
      m23 = v2.w;

      m30 = v3.x;
      m31 = v3.y;
      m32 = v3.z;
      m33 = v3.w;
   }

   /**
    * Multiplies this matrix by M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mul (Matrix4dBase M1) {
      mul (this, M1);
   }

   /**
    * Multiplies matrix M1 by M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mul (Matrix4dBase M1, Matrix4dBase M2) {
      double x00, x01, x02, x03;
      double x10, x11, x12, x13;
      double x20, x21, x22, x23;
      double x30, x31, x32, x33;

      x00 = M1.m00*M2.m00 + M1.m01*M2.m10 + M1.m02*M2.m20 + M1.m03*M2.m30;
      x01 = M1.m00*M2.m01 + M1.m01*M2.m11 + M1.m02*M2.m21 + M1.m03*M2.m31;
      x02 = M1.m00*M2.m02 + M1.m01*M2.m12 + M1.m02*M2.m22 + M1.m03*M2.m32;
      x03 = M1.m00*M2.m03 + M1.m01*M2.m13 + M1.m02*M2.m23 + M1.m03*M2.m33;

      x10 = M1.m10*M2.m00 + M1.m11*M2.m10 + M1.m12*M2.m20 + M1.m13*M2.m30;
      x11 = M1.m10*M2.m01 + M1.m11*M2.m11 + M1.m12*M2.m21 + M1.m13*M2.m31;
      x12 = M1.m10*M2.m02 + M1.m11*M2.m12 + M1.m12*M2.m22 + M1.m13*M2.m32;
      x13 = M1.m10*M2.m03 + M1.m11*M2.m13 + M1.m12*M2.m23 + M1.m13*M2.m33;

      x20 = M1.m20*M2.m00 + M1.m21*M2.m10 + M1.m22*M2.m20 + M1.m23*M2.m30;
      x21 = M1.m20*M2.m01 + M1.m21*M2.m11 + M1.m22*M2.m21 + M1.m23*M2.m31;
      x22 = M1.m20*M2.m02 + M1.m21*M2.m12 + M1.m22*M2.m22 + M1.m23*M2.m32;
      x23 = M1.m20*M2.m03 + M1.m21*M2.m13 + M1.m22*M2.m23 + M1.m23*M2.m33;

      x30 = M1.m30*M2.m00 + M1.m31*M2.m10 + M1.m32*M2.m20 + M1.m33*M2.m30;
      x31 = M1.m30*M2.m01 + M1.m31*M2.m11 + M1.m32*M2.m21 + M1.m33*M2.m31;
      x32 = M1.m30*M2.m02 + M1.m31*M2.m12 + M1.m32*M2.m22 + M1.m33*M2.m32;
      x33 = M1.m30*M2.m03 + M1.m31*M2.m13 + M1.m32*M2.m23 + M1.m33*M2.m33;

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
   }

   /**
    * Multiplies this matrix by the transpose of M1 and places the result in
    * this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mulTranspose (Matrix4dBase M1) {
      mulTransposeRight (this, M1);
   }

   /**
    * Multiplies the transpose of matrix M1 by M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mulTransposeLeft (Matrix4dBase M1, Matrix4dBase M2) {
      double x00, x01, x02, x03;
      double x10, x11, x12, x13;
      double x20, x21, x22, x23;
      double x30, x31, x32, x33;

      x00 =
         M1.m00 * M2.m00 + M1.m10 * M2.m10 + M1.m20 * M2.m20 + M1.m30 * M2.m30;
      x01 =
         M1.m00 * M2.m01 + M1.m10 * M2.m11 + M1.m20 * M2.m21 + M1.m30 * M2.m31;
      x02 =
         M1.m00 * M2.m02 + M1.m10 * M2.m12 + M1.m20 * M2.m22 + M1.m30 * M2.m32;
      x03 =
         M1.m00 * M2.m03 + M1.m10 * M2.m13 + M1.m20 * M2.m23 + M1.m30 * M2.m33;

      x10 =
         M1.m01 * M2.m00 + M1.m11 * M2.m10 + M1.m21 * M2.m20 + M1.m31 * M2.m30;
      x11 =
         M1.m01 * M2.m01 + M1.m11 * M2.m11 + M1.m21 * M2.m21 + M1.m31 * M2.m31;
      x12 =
         M1.m01 * M2.m02 + M1.m11 * M2.m12 + M1.m21 * M2.m22 + M1.m31 * M2.m32;
      x13 =
         M1.m01 * M2.m03 + M1.m11 * M2.m13 + M1.m21 * M2.m23 + M1.m31 * M2.m33;

      x20 =
         M1.m02 * M2.m00 + M1.m12 * M2.m10 + M1.m22 * M2.m20 + M1.m32 * M2.m30;
      x21 =
         M1.m02 * M2.m01 + M1.m12 * M2.m11 + M1.m22 * M2.m21 + M1.m32 * M2.m31;
      x22 =
         M1.m02 * M2.m02 + M1.m12 * M2.m12 + M1.m22 * M2.m22 + M1.m32 * M2.m32;
      x23 =
         M1.m02 * M2.m03 + M1.m12 * M2.m13 + M1.m22 * M2.m23 + M1.m32 * M2.m33;

      x30 =
         M1.m03 * M2.m00 + M1.m13 * M2.m10 + M1.m23 * M2.m20 + M1.m33 * M2.m30;
      x31 =
         M1.m03 * M2.m01 + M1.m13 * M2.m11 + M1.m23 * M2.m21 + M1.m33 * M2.m31;
      x32 =
         M1.m03 * M2.m02 + M1.m13 * M2.m12 + M1.m23 * M2.m22 + M1.m33 * M2.m32;
      x33 =
         M1.m03 * M2.m03 + M1.m13 * M2.m13 + M1.m23 * M2.m23 + M1.m33 * M2.m33;

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
   }

   /**
    * Multiplies matrix M1 by the transpose of M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mulTransposeRight (Matrix4dBase M1, Matrix4dBase M2) {
      double x00, x01, x02, x03;
      double x10, x11, x12, x13;
      double x20, x21, x22, x23;
      double x30, x31, x32, x33;

      x00 =
         M1.m00 * M2.m00 + M1.m01 * M2.m01 + M1.m02 * M2.m02 + M1.m03 * M2.m03;
      x01 =
         M1.m00 * M2.m10 + M1.m01 * M2.m11 + M1.m02 * M2.m12 + M1.m03 * M2.m13;
      x02 =
         M1.m00 * M2.m20 + M1.m01 * M2.m21 + M1.m02 * M2.m22 + M1.m03 * M2.m23;
      x03 =
         M1.m00 * M2.m30 + M1.m01 * M2.m31 + M1.m02 * M2.m32 + M1.m03 * M2.m33;

      x10 =
         M1.m10 * M2.m00 + M1.m11 * M2.m01 + M1.m12 * M2.m02 + M1.m13 * M2.m03;
      x11 =
         M1.m10 * M2.m10 + M1.m11 * M2.m11 + M1.m12 * M2.m12 + M1.m13 * M2.m13;
      x12 =
         M1.m10 * M2.m20 + M1.m11 * M2.m21 + M1.m12 * M2.m22 + M1.m13 * M2.m23;
      x13 =
         M1.m10 * M2.m30 + M1.m11 * M2.m31 + M1.m12 * M2.m32 + M1.m13 * M2.m33;

      x20 =
         M1.m20 * M2.m00 + M1.m21 * M2.m01 + M1.m22 * M2.m02 + M1.m23 * M2.m03;
      x21 =
         M1.m20 * M2.m10 + M1.m21 * M2.m11 + M1.m22 * M2.m12 + M1.m23 * M2.m13;
      x22 =
         M1.m20 * M2.m20 + M1.m21 * M2.m21 + M1.m22 * M2.m22 + M1.m23 * M2.m23;
      x23 =
         M1.m20 * M2.m30 + M1.m21 * M2.m31 + M1.m22 * M2.m32 + M1.m23 * M2.m33;

      x30 =
         M1.m30 * M2.m00 + M1.m31 * M2.m01 + M1.m32 * M2.m02 + M1.m33 * M2.m03;
      x31 =
         M1.m30 * M2.m10 + M1.m31 * M2.m11 + M1.m32 * M2.m12 + M1.m33 * M2.m13;
      x32 =
         M1.m30 * M2.m20 + M1.m31 * M2.m21 + M1.m32 * M2.m22 + M1.m33 * M2.m23;
      x33 =
         M1.m30 * M2.m30 + M1.m31 * M2.m31 + M1.m32 * M2.m32 + M1.m33 * M2.m33;

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
   }

   /**
    * Multiplies the transpose of matrix M1 by the transpose of M2 and places
    * the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mulTransposeBoth (Matrix4dBase M1, Matrix4dBase M2) {
      double x00, x01, x02, x03;
      double x10, x11, x12, x13;
      double x20, x21, x22, x23;
      double x30, x31, x32, x33;

      x00 =
         M1.m00 * M2.m00 + M1.m10 * M2.m01 + M1.m20 * M2.m02 + M1.m30 * M2.m03;
      x01 =
         M1.m00 * M2.m10 + M1.m10 * M2.m11 + M1.m20 * M2.m12 + M1.m30 * M2.m13;
      x02 =
         M1.m00 * M2.m20 + M1.m10 * M2.m21 + M1.m20 * M2.m22 + M1.m30 * M2.m23;
      x03 =
         M1.m00 * M2.m30 + M1.m10 * M2.m31 + M1.m20 * M2.m32 + M1.m30 * M2.m33;

      x10 =
         M1.m01 * M2.m00 + M1.m11 * M2.m01 + M1.m21 * M2.m02 + M1.m31 * M2.m03;
      x11 =
         M1.m01 * M2.m10 + M1.m11 * M2.m11 + M1.m21 * M2.m12 + M1.m31 * M2.m13;
      x12 =
         M1.m01 * M2.m20 + M1.m11 * M2.m21 + M1.m21 * M2.m22 + M1.m31 * M2.m23;
      x13 =
         M1.m01 * M2.m30 + M1.m11 * M2.m31 + M1.m21 * M2.m32 + M1.m31 * M2.m33;

      x20 =
         M1.m02 * M2.m00 + M1.m12 * M2.m01 + M1.m22 * M2.m02 + M1.m32 * M2.m03;
      x21 =
         M1.m02 * M2.m10 + M1.m12 * M2.m11 + M1.m22 * M2.m12 + M1.m32 * M2.m13;
      x22 =
         M1.m02 * M2.m20 + M1.m12 * M2.m21 + M1.m22 * M2.m22 + M1.m32 * M2.m23;
      x23 =
         M1.m02 * M2.m30 + M1.m12 * M2.m31 + M1.m22 * M2.m32 + M1.m32 * M2.m33;

      x30 =
         M1.m03 * M2.m00 + M1.m13 * M2.m01 + M1.m23 * M2.m02 + M1.m33 * M2.m03;
      x31 =
         M1.m03 * M2.m10 + M1.m13 * M2.m11 + M1.m23 * M2.m12 + M1.m33 * M2.m13;
      x32 =
         M1.m03 * M2.m20 + M1.m13 * M2.m21 + M1.m23 * M2.m22 + M1.m33 * M2.m23;
      x33 =
         M1.m03 * M2.m30 + M1.m13 * M2.m31 + M1.m23 * M2.m32 + M1.m33 * M2.m33;

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
   }

   /**
    * Multiplies matrix M1 by the inverse of M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @return false if M2 is singular
    */
   protected boolean mulInverseRight (Matrix4dBase M1, Matrix4dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix4d Tmp = new Matrix4d();
         nonSingular = Tmp.invert (M2);
         mul (M1, Tmp);
      }
      else {
         nonSingular = invert (M2);
         mul (M1, this);
      }
      return nonSingular;
   }

   /**
    * Multiplies the inverse of matrix M1 by M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @return false if M1 is singular
    */
   protected boolean mulInverseLeft (Matrix4dBase M1, Matrix4dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix4d Tmp = new Matrix4d();
         nonSingular = Tmp.invert (M1);
         mul (Tmp, M2);
      }
      else {
         nonSingular = invert (M1);
         mul (this, M2);
      }
      return nonSingular;
   }

   /**
    * Multiplies the inverse of matrix M1 by the inverse of M2 and places the
    * result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @return false if M1 or M2 is singular
    */
   protected boolean mulInverseBoth (Matrix4dBase M1, Matrix4dBase M2) {
      mul (M2, M1);
      return invert();
   }

   /**
    * Multiplies this matrix by the column vector v1 and places the result in
    * the vector vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = M v1
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    */
   public void mul (Vector4d vr, Vector4d v1) {

      double x = m00*v1.x + m01*v1.y + m02*v1.z + m03*v1.w;
      double y = m10*v1.x + m11*v1.y + m12*v1.z + m13*v1.w;
      double z = m20*v1.x + m21*v1.y + m22*v1.z + m23*v1.w;
      double w = m30*v1.x + m31*v1.y + m32*v1.z + m33*v1.w;

      vr.x = x;
      vr.y = y;
      vr.z = z;
      vr.w = w;
   }

   /**
    * Multiplies this matrix by the column vector v1 and places the result in
    * the vector vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = M v1
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    */
   public void mul (VectorNd vr, VectorNd v1) {
      if (v1.size() < 4) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size 4");
      }
      if (vr.size() < 4) {
         vr.setSize(4);
      }
      double[] res = vr.getBuffer();
      double[] buf = v1.getBuffer();

      double b0 = buf[0];
      double b1 = buf[1];
      double b2 = buf[2];
      double b3 = buf[3];

      res[0] = m00*b0 + m01*b1 + m02*b2 + m03*b3;
      res[1] = m10*b0 + m11*b1 + m12*b2 + m13*b3;
      res[2] = m20*b0 + m21*b1 + m22*b2 + m23*b3;
      res[3] = m30*b0 + m31*b1 + m32*b2 + m33*b3;
   }

   /**
    * Multiplies this matrix by the column vector vr and places the result back
    * into vr. If M represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr = M vr
    * </pre>
    * 
    * @param vr
    * vector to multiply (in place)
    */
   public void mul (Vector4d vr) {
      mul (vr, vr);
   }

   /**
    * Multiplies the transpose of this matrix by the vector v1 and places the
    * result in vr. If M represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr = v1 M
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    */
   public void mulTranspose (Vector4d vr, Vector4d v1) {
      double x = m00 * v1.x + m10 * v1.y + m20 * v1.z + m30 * v1.w;
      double y = m01 * v1.x + m11 * v1.y + m21 * v1.z + m31 * v1.w;
      double z = m02 * v1.x + m12 * v1.y + m22 * v1.z + m32 * v1.w;
      double w = m03 * v1.x + m13 * v1.y + m23 * v1.z + m33 * v1.w;

      vr.x = x;
      vr.y = y;
      vr.z = z;
      vr.w = w;
   }

   /**
    * Multiplies the transpose of this matrix by the vector vr and places the
    * result back in vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = vr M
    * </pre>
    * 
    * @param vr
    * vector to multiply by (in place)
    */
   public void mulTranspose (Vector4d vr) {
      mulTranspose (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse of this matrix and places
    * the result in vr.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return false if this matrix is singular
    */
   public boolean mulInverse (Vector4d vr, Vector4d v1) {
      Matrix4d Tmp = new Matrix4d();
      boolean nonSingular = Tmp.invert (this);
      Tmp.mul (vr, v1);
      return nonSingular;
   }

   /**
    * Multiplies the column vector vr by the inverse of this matrix and places
    * the result back in vr.
    * 
    * @param vr
    * vector to multiply by (in place)
    * @return false if this matrix is singular
    */
   public boolean mulInverse (Vector4d vr) {
      return mulInverse (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse transpose of this matrix
    * and places the result in vr.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return false if this matrix is singular
    */
   public boolean mulInverseTranspose (Vector4d vr, Vector4d v1) {
      Matrix4d Tmp = new Matrix4d();
      boolean nonSingular = Tmp.invert (this);
      Tmp.mulTranspose (vr, v1);
      return nonSingular;
   }

   /**
    * Multiplies the column vector vr by the inverse transpose of this matrix
    * and places the result back in vr.
    * 
    * @param vr
    * vector to multiply by (in place)
    * @return false if this matrix is singular
    */
   public boolean mulInverseTranspose (Vector4d vr) {
      return mulInverseTranspose (vr, vr);
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd4x4 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd4x4 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd4x4 (this, M1, M2);
   }

   /**
    * Adds matrix M1 to M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void add (Matrix4dBase M1, Matrix4dBase M2) {
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

      m30 = M1.m30 + M2.m30;
      m31 = M1.m31 + M2.m31;
      m32 = M1.m32 + M2.m32;
      m33 = M1.m33 + M2.m33;
   }

   /**
    * Adds this matrix to M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void add (Matrix4dBase M1) {
      m00 += M1.m00;
      m01 += M1.m01;
      m02 += M1.m02;
      m03 += M1.m03;

      m10 += M1.m10;
      m11 += M1.m11;
      m12 += M1.m12;
      m13 += M1.m13;

      m20 += M1.m20;
      m21 += M1.m21;
      m22 += M1.m22;
      m23 += M1.m23;

      m30 += M1.m30;
      m31 += M1.m31;
      m32 += M1.m32;
      m33 += M1.m33;
   }

   /**
    * Subtracts matrix M1 from M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void sub (Matrix4dBase M1, Matrix4dBase M2) {
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

      m30 = M1.m30 - M2.m30;
      m31 = M1.m31 - M2.m31;
      m32 = M1.m32 - M2.m32;
      m33 = M1.m33 - M2.m33;
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void sub (Matrix4dBase M1) {
      m00 -= M1.m00;
      m01 -= M1.m01;
      m02 -= M1.m02;
      m03 -= M1.m03;

      m10 -= M1.m10;
      m11 -= M1.m11;
      m12 -= M1.m12;
      m13 -= M1.m13;

      m20 -= M1.m20;
      m21 -= M1.m21;
      m22 -= M1.m22;
      m23 -= M1.m23;

      m30 -= M1.m30;
      m31 -= M1.m31;
      m32 -= M1.m32;
      m33 -= M1.m33;
   }

   /**
    * Scales the elements of matrix M1 by <code>s</code> and places the
    * results in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    */
   protected void scale (double s, Matrix4dBase M1) {
      m00 = s * M1.m00;
      m01 = s * M1.m01;
      m02 = s * M1.m02;
      m03 = s * M1.m03;

      m10 = s * M1.m10;
      m11 = s * M1.m11;
      m12 = s * M1.m12;
      m13 = s * M1.m13;

      m20 = s * M1.m20;
      m21 = s * M1.m21;
      m22 = s * M1.m22;
      m23 = s * M1.m23;

      m30 = s * M1.m30;
      m31 = s * M1.m31;
      m32 = s * M1.m32;
      m33 = s * M1.m33;
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
   protected void scaledAdd (double s, Matrix4dBase M1, Matrix4dBase M2) {
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

      m30 = s * M1.m30 + M2.m30;
      m31 = s * M1.m31 + M2.m31;
      m32 = s * M1.m32 + M2.m32;
      m33 = s * M1.m33 + M2.m33;
   }

   /**
    * Computes s M1 and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled and added
    */
   protected void scaledAdd (double s, Matrix4dBase M1) {
      m00 += s * M1.m00;
      m01 += s * M1.m01;
      m02 += s * M1.m02;
      m03 += s * M1.m03;

      m10 += s * M1.m10;
      m11 += s * M1.m11;
      m12 += s * M1.m12;
      m13 += s * M1.m13;

      m20 += s * M1.m20;
      m21 += s * M1.m21;
      m22 += s * M1.m22;
      m23 += s * M1.m23;

      m30 += s * M1.m30;
      m31 += s * M1.m31;
      m32 += s * M1.m32;
      m33 += s * M1.m33;
   }

   /**
    * Sets this matrix to the negative of M1.
    * 
    * @param M1
    * matrix to negate
    */
   protected void negate (Matrix4dBase M1) {
      m00 = -M1.m00;
      m01 = -M1.m01;
      m02 = -M1.m02;
      m03 = -M1.m03;

      m10 = -M1.m10;
      m11 = -M1.m11;
      m12 = -M1.m12;
      m13 = -M1.m13;

      m20 = -M1.m20;
      m21 = -M1.m21;
      m22 = -M1.m22;
      m23 = -M1.m23;

      m30 = -M1.m30;
      m31 = -M1.m31;
      m32 = -M1.m32;
      m33 = -M1.m33;
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      negate (this);
   }

   /**
    * Transposes this matrix in place.
    */
   public void transpose() {
      double tmp01 = m01;
      double tmp02 = m02;
      double tmp03 = m03;
      double tmp12 = m12;
      double tmp13 = m13;
      double tmp23 = m23;

      m01 = m10;
      m02 = m20;
      m03 = m30;
      m12 = m21;
      m13 = m31;
      m23 = m32;

      m10 = tmp01;
      m20 = tmp02;
      m30 = tmp03;
      m21 = tmp12;
      m31 = tmp13;
      m32 = tmp23;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   protected void transpose (Matrix4dBase M) {
      double tmp01 = M.m01;
      double tmp02 = M.m02;
      double tmp03 = M.m03;
      double tmp12 = M.m12;
      double tmp13 = M.m13;
      double tmp23 = M.m23;

      m00 = M.m00;
      m11 = M.m11;
      m22 = M.m22;
      m33 = M.m33;

      m01 = M.m10;
      m02 = M.m20;
      m03 = M.m30;
      m12 = M.m21;
      m13 = M.m31;
      m23 = M.m32;

      m10 = tmp01;
      m20 = tmp02;
      m30 = tmp03;
      m21 = tmp12;
      m31 = tmp13;
      m32 = tmp23;
   }

   /**
    * Sets this matrix to the identity.
    */
   public void setIdentity() {
      m00 = 1.0;
      m01 = 0.0;
      m02 = 0.0;
      m03 = 0.0;

      m10 = 0.0;
      m11 = 1.0;
      m12 = 0.0;
      m13 = 0.0;

      m20 = 0.0;
      m21 = 0.0;
      m22 = 1.0;
      m23 = 0.0;

      m30 = 0.0;
      m31 = 0.0;
      m32 = 0.0;
      m33 = 1.0;
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   protected void setZero() {
      m00 = 0.0;
      m01 = 0.0;
      m02 = 0.0;
      m03 = 0.0;

      m10 = 0.0;
      m11 = 0.0;
      m12 = 0.0;
      m13 = 0.0;

      m20 = 0.0;
      m21 = 0.0;
      m22 = 0.0;
      m23 = 0.0;

      m30 = 0.0;
      m31 = 0.0;
      m32 = 0.0;
      m33 = 0.0;
   }

   /**
    * Returns true if the elements of this matrix equal those of matrix
    * <code>M1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param M1
    * matrix to compare with
    * @param epsilon
    * comparison tolerance
    * @return false if the matrices are not equal within the specified tolerance
    */
   public boolean epsilonEquals (Matrix4dBase M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && abs (m03 - M1.m03) <= epsilon &&

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon && abs (m13 - M1.m13) <= epsilon &&

          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m22 - M1.m22) <= epsilon && abs (m23 - M1.m23) <= epsilon &&

          abs (m30 - M1.m30) <= epsilon && abs (m31 - M1.m31) <= epsilon &&
          abs (m32 - M1.m32) <= epsilon && abs (m33 - M1.m33) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if the elements of this matrix exactly equal those of matrix
    * <code>M1</code>.
    * 
    * @param M1
    * matrix to compare with
    * @return false if the matrices are not equal
    */
   public boolean equals (Matrix4dBase M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m03 == M1.m03) && (m10 == M1.m10) && (m11 == M1.m11) &&
          (m12 == M1.m12) && (m13 == M1.m13) && (m20 == M1.m20) &&
          (m21 == M1.m21) && (m22 == M1.m22) && (m23 == M1.m23) &&
          (m30 == M1.m30) && (m31 == M1.m31) && (m32 == M1.m32) &&
          (m33 == M1.m33)) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Returns the infinity norm of this matrix. This is equal to the maximum of
    * the vector 1-norm of each row.
    * 
    * @return infinity norm of this matrix
    */
   public double infinityNorm() {
      // returns the largest row sum of the absolute value\
      double abs0, abs1, abs2, abs3;
      double max, sum;

      abs0 = (m00 >= 0 ? m00 : -m00);
      abs1 = (m01 >= 0 ? m01 : -m01);
      abs2 = (m02 >= 0 ? m02 : -m02);
      abs3 = (m03 >= 0 ? m03 : -m03);
      max = abs0 + abs1 + abs2 + abs3;

      abs0 = (m10 >= 0 ? m10 : -m10);
      abs1 = (m11 >= 0 ? m11 : -m11);
      abs2 = (m12 >= 0 ? m12 : -m12);
      abs3 = (m13 >= 0 ? m13 : -m13);
      sum = abs0 + abs1 + abs2 + abs3;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m20 >= 0 ? m20 : -m20);
      abs1 = (m21 >= 0 ? m21 : -m21);
      abs2 = (m22 >= 0 ? m22 : -m22);
      abs3 = (m23 >= 0 ? m23 : -m23);
      sum = abs0 + abs1 + abs2 + abs3;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m30 >= 0 ? m30 : -m30);
      abs1 = (m31 >= 0 ? m31 : -m31);
      abs2 = (m32 >= 0 ? m32 : -m32);
      abs3 = (m33 >= 0 ? m33 : -m33);
      sum = abs0 + abs1 + abs2 + abs3;
      if (sum > max) {
         max = sum;
      }
      return max;
   }

   /**
    * Returns the 1 norm of this matrix. This is equal to the maximum of the
    * vector 1-norm of each column.
    * 
    * @return 1 norm of this matrix
    */
   public double oneNorm() {
      // returns the largest column sum of the absolute value
      double abs0, abs1, abs2, abs3;
      double max, sum;

      abs0 = (m00 >= 0 ? m00 : -m00);
      abs1 = (m10 >= 0 ? m10 : -m10);
      abs2 = (m20 >= 0 ? m20 : -m20);
      abs3 = (m30 >= 0 ? m30 : -m30);
      max = abs0 + abs1 + abs2 + abs3;

      abs0 = (m01 >= 0 ? m01 : -m01);
      abs1 = (m11 >= 0 ? m11 : -m11);
      abs2 = (m21 >= 0 ? m21 : -m21);
      abs3 = (m31 >= 0 ? m31 : -m31);
      sum = abs0 + abs1 + abs2 + abs3;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m02 >= 0 ? m02 : -m02);
      abs1 = (m12 >= 0 ? m12 : -m12);
      abs2 = (m22 >= 0 ? m22 : -m22);
      abs3 = (m32 >= 0 ? m32 : -m32);
      sum = abs0 + abs1 + abs2 + abs3;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m03 >= 0 ? m03 : -m03);
      abs1 = (m13 >= 0 ? m13 : -m13);
      abs2 = (m23 >= 0 ? m23 : -m23);
      abs3 = (m33 >= 0 ? m33 : -m33);
      sum = abs0 + abs1 + abs2 + abs3;
      if (sum > max) {
         max = sum;
      }
      return max;
   }

   /**
    * Returns the Frobenius norm of this matrix. This is equal to the square
    * root of the sum of the squares of each element.
    * 
    * @return Frobenius norm of this matrix
    */
   public double frobeniusNorm() {
      // returns sqrt(sum (diag (M'*M))
      double sum =
         (m00 * m00 + m01 * m01 + m02 * m02 + m03 * m03 +
          m10 * m10 + m11 * m11 + m12 * m12 + m13 * m13 +
          m20 * m20 + m21 * m21 + m22 * m22 + m23 * m23 +
          m30 * m30 + m31 * m31 + m32 * m32 + m33 * m33);
      return Math.sqrt (sum);
   }

   /**
    * Inverts this matrix in place, returning false if the matrix is detected to
    * be singular. The inverse is computed using an LU decomposition with
    * partial pivoting.
    */
   public boolean invert() {
      return invert (this);
   }

   /**
    * Inverts the matrix M and places the result in this matrix, return false if
    * M is detected to be singular. The inverse is computed using an LU
    * decomposition with partial pivoting.
    * 
    * @param M1
    * matrix to invert
    * @return false if M is singular
    */
   protected boolean invert (Matrix4dBase M1) {
      LUDecomposition lu = new LUDecomposition();
      lu.factor (M1);
      boolean singular = false;
      try {
         singular = lu.inverse (this);
      }
      catch (ImproperStateException e) { // can't happen
      }
      return singular;
   }

   /**
    * Returns the determinant of this matrix
    * 
    * @return matrix determinant
    */
   public double determinant() throws ImproperSizeException {
      LUDecomposition lu = new LUDecomposition();
      lu.factor (this);
      return (lu.determinant());
   }

   public Object clone() {
      try {
         return super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for Matrix4dBase");
      }
   }

}
