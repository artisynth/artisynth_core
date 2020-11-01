/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.Clonable;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;

/**
 * Base class for 3 x 3 matrices in which the elements are stored as explicit
 * fields. A primary motivation for such objects is computational speed.
 */
public abstract class Matrix3dBase extends DenseMatrixBase implements
   java.io.Serializable, Clonable {

   private static final long serialVersionUID = 1L;

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
    * Returns the number of rows in this matrix (which is always 3).
    * 
    * @return 3
    */
   public final int rowSize() {
      return 3;
   }

   /**
    * Returns the number of columns in this matrix (which is always 3).
    * 
    * @return 3
    */
   public final int colSize() {
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

      values[3] = m10;
      values[4] = m11;
      values[5] = m12;

      values[6] = m20;
      values[7] = m21;
      values[8] = m22;
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            values[0] = m00;
            values[1] = m10;
            values[2] = m20;
            break;
         }
         case 1: {
            values[0] = m01;
            values[1] = m11;
            values[2] = m21;
            break;
         }
         case 2: {
            values[0] = m02;
            values[1] = m12;
            values[2] = m22;
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
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }
   
   /**
    * Scale a column
    */
   public void scaleColumn (int j, double s) {
      switch (j) {
         case 0: {
            m00 *= s;
            m10 *= s;
            m20 *= s;
            break;
         }
         case 1: {
            m01 *= s;
            m11 *= s;
            m21 *= s;
            break;
         }
         case 2: {
            m02 *= s;
            m12 *= s;
            m22 *= s;
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
      switch (i) {
         case 0: {
            values[0] = m00;
            values[1] = m01;
            values[2] = m02;
            break;
         }
         case 1: {
            values[0] = m10;
            values[1] = m11;
            values[2] = m12;
            break;
         }
         case 2: {
            values[0] = m20;
            values[1] = m21;
            values[2] = m22;
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
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }
   
   /**
    * Scale a row by a particular value
    */
   public void scaleRow (int i, double s) {
      switch (i) {
         case 0: {
            m00 *= s;
            m01 *= s;
            m02 *= s;
            break;
         }
         case 1: {
            m10 *= s;
            m11 *= s;
            m12 *= s;
            break;
         }
         case 2: {
            m20 *= s;
            m21 *= s;
            m22 *= s;
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

      m10 = vals[3];
      m11 = vals[4];
      m12 = vals[5];

      m20 = vals[6];
      m21 = vals[7];
      m22 = vals[8];
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
   public void set (Matrix M) {
      if (M instanceof Matrix3dBase) {
         set ((Matrix3dBase)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 3) {
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
      }
   }

   /**
    * Sets the values of this matrix to those of matrix M.
    * 
    * @param M
    * matrix whose values are to be copied
    */
   public void set (Matrix3dBase M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;

      m10 = M.m10;
      m11 = M.m11;
      m12 = M.m12;

      m20 = M.m20;
      m21 = M.m21;
      m22 = M.m22;
   }

   /**
    * Sets the columns of this matrix to the vectors v0, v1, and v2.
    * 
    * @param v0
    * values for the first column
    * @param v1
    * values for the second column
    * @param v2
    * values for the third column
    */
   protected void setColumns (Vector3d v0, Vector3d v1, Vector3d v2) {
      m00 = v0.x;
      m10 = v0.y;
      m20 = v0.z;
      m01 = v1.x;
      m11 = v1.y;
      m21 = v1.z;
      m02 = v2.x;
      m12 = v2.y;
      m22 = v2.z;
   }

   /**
    * Sets the rows of this matrix to the vectors v0, v1, and v2.
    * 
    * @param v0
    * values for the first row
    * @param v1
    * values for the second row
    * @param v2
    * values for the third row
    */
   protected void setRows (Vector3d v0, Vector3d v1, Vector3d v2) {
      m00 = v0.x;
      m01 = v0.y;
      m02 = v0.z;
      m10 = v1.x;
      m11 = v1.y;
      m12 = v1.z;
      m20 = v2.x;
      m21 = v2.y;
      m22 = v2.z;
   }

   /**
    * Multiplies this matrix by M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mul (Matrix3dBase M1) {
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
   public void mul (Matrix3dBase M1, Matrix3dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m01 * M2.m10 + M1.m02 * M2.m20;
      double tmp01 = M1.m00 * M2.m01 + M1.m01 * M2.m11 + M1.m02 * M2.m21;
      double tmp02 = M1.m00 * M2.m02 + M1.m01 * M2.m12 + M1.m02 * M2.m22;

      double tmp10 = M1.m10 * M2.m00 + M1.m11 * M2.m10 + M1.m12 * M2.m20;
      double tmp11 = M1.m10 * M2.m01 + M1.m11 * M2.m11 + M1.m12 * M2.m21;
      double tmp12 = M1.m10 * M2.m02 + M1.m11 * M2.m12 + M1.m12 * M2.m22;

      double tmp20 = M1.m20 * M2.m00 + M1.m21 * M2.m10 + M1.m22 * M2.m20;
      double tmp21 = M1.m20 * M2.m01 + M1.m21 * M2.m11 + M1.m22 * M2.m21;
      double tmp22 = M1.m20 * M2.m02 + M1.m21 * M2.m12 + M1.m22 * M2.m22;

      m00 = tmp00;
      m01 = tmp01;
      m02 = tmp02;

      m10 = tmp10;
      m11 = tmp11;
      m12 = tmp12;

      m20 = tmp20;
      m21 = tmp21;
      m22 = tmp22;
   }

   /**
    * Multiplies this matrix by the transpose of M1 and places the result in
    * this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mulTranspose (Matrix3dBase M1) {
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
   protected void mulTransposeLeft (Matrix3dBase M1, Matrix3dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m10 * M2.m10 + M1.m20 * M2.m20;
      double tmp01 = M1.m00 * M2.m01 + M1.m10 * M2.m11 + M1.m20 * M2.m21;
      double tmp02 = M1.m00 * M2.m02 + M1.m10 * M2.m12 + M1.m20 * M2.m22;

      double tmp10 = M1.m01 * M2.m00 + M1.m11 * M2.m10 + M1.m21 * M2.m20;
      double tmp11 = M1.m01 * M2.m01 + M1.m11 * M2.m11 + M1.m21 * M2.m21;
      double tmp12 = M1.m01 * M2.m02 + M1.m11 * M2.m12 + M1.m21 * M2.m22;

      double tmp20 = M1.m02 * M2.m00 + M1.m12 * M2.m10 + M1.m22 * M2.m20;
      double tmp21 = M1.m02 * M2.m01 + M1.m12 * M2.m11 + M1.m22 * M2.m21;
      double tmp22 = M1.m02 * M2.m02 + M1.m12 * M2.m12 + M1.m22 * M2.m22;

      m00 = tmp00;
      m01 = tmp01;
      m02 = tmp02;

      m10 = tmp10;
      m11 = tmp11;
      m12 = tmp12;

      m20 = tmp20;
      m21 = tmp21;
      m22 = tmp22;
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
   protected void mulTransposeRight (Matrix3dBase M1, Matrix3dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m01 * M2.m01 + M1.m02 * M2.m02;
      double tmp01 = M1.m00 * M2.m10 + M1.m01 * M2.m11 + M1.m02 * M2.m12;
      double tmp02 = M1.m00 * M2.m20 + M1.m01 * M2.m21 + M1.m02 * M2.m22;

      double tmp10 = M1.m10 * M2.m00 + M1.m11 * M2.m01 + M1.m12 * M2.m02;
      double tmp11 = M1.m10 * M2.m10 + M1.m11 * M2.m11 + M1.m12 * M2.m12;
      double tmp12 = M1.m10 * M2.m20 + M1.m11 * M2.m21 + M1.m12 * M2.m22;

      double tmp20 = M1.m20 * M2.m00 + M1.m21 * M2.m01 + M1.m22 * M2.m02;
      double tmp21 = M1.m20 * M2.m10 + M1.m21 * M2.m11 + M1.m22 * M2.m12;
      double tmp22 = M1.m20 * M2.m20 + M1.m21 * M2.m21 + M1.m22 * M2.m22;

      m00 = tmp00;
      m01 = tmp01;
      m02 = tmp02;

      m10 = tmp10;
      m11 = tmp11;
      m12 = tmp12;

      m20 = tmp20;
      m21 = tmp21;
      m22 = tmp22;
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
   protected void mulTransposeBoth (Matrix3dBase M1, Matrix3dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m10 * M2.m01 + M1.m20 * M2.m02;
      double tmp01 = M1.m00 * M2.m10 + M1.m10 * M2.m11 + M1.m20 * M2.m12;
      double tmp02 = M1.m00 * M2.m20 + M1.m10 * M2.m21 + M1.m20 * M2.m22;

      double tmp10 = M1.m01 * M2.m00 + M1.m11 * M2.m01 + M1.m21 * M2.m02;
      double tmp11 = M1.m01 * M2.m10 + M1.m11 * M2.m11 + M1.m21 * M2.m12;
      double tmp12 = M1.m01 * M2.m20 + M1.m11 * M2.m21 + M1.m21 * M2.m22;

      double tmp20 = M1.m02 * M2.m00 + M1.m12 * M2.m01 + M1.m22 * M2.m02;
      double tmp21 = M1.m02 * M2.m10 + M1.m12 * M2.m11 + M1.m22 * M2.m12;
      double tmp22 = M1.m02 * M2.m20 + M1.m12 * M2.m21 + M1.m22 * M2.m22;

      m00 = tmp00;
      m01 = tmp01;
      m02 = tmp02;

      m10 = tmp10;
      m11 = tmp11;
      m12 = tmp12;

      m20 = tmp20;
      m21 = tmp21;
      m22 = tmp22;
   }

   /**
    * Applies a rotational transformation R to M1 and place the result in this
    * matrix. This is equivalent to forming the product
    * 
    * <pre>
    *    R M1 R^T
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M1
    * matrix to transform
    */
   protected void transform (RotationMatrix3d R, Matrix3dBase M1) {
      if (R == this) {
         transform (new RotationMatrix3d (R), M1);
      }
      else {
         double tmp00 = M1.m00 * R.m00 + M1.m01 * R.m01 + M1.m02 * R.m02;
         double tmp01 = M1.m00 * R.m10 + M1.m01 * R.m11 + M1.m02 * R.m12;
         double tmp02 = M1.m00 * R.m20 + M1.m01 * R.m21 + M1.m02 * R.m22;

         double tmp10 = M1.m10 * R.m00 + M1.m11 * R.m01 + M1.m12 * R.m02;
         double tmp11 = M1.m10 * R.m10 + M1.m11 * R.m11 + M1.m12 * R.m12;
         double tmp12 = M1.m10 * R.m20 + M1.m11 * R.m21 + M1.m12 * R.m22;

         double tmp20 = M1.m20 * R.m00 + M1.m21 * R.m01 + M1.m22 * R.m02;
         double tmp21 = M1.m20 * R.m10 + M1.m21 * R.m11 + M1.m22 * R.m12;
         double tmp22 = M1.m20 * R.m20 + M1.m21 * R.m21 + M1.m22 * R.m22;

         m00 = R.m00 * tmp00 + R.m01 * tmp10 + R.m02 * tmp20;
         m01 = R.m00 * tmp01 + R.m01 * tmp11 + R.m02 * tmp21;
         m02 = R.m00 * tmp02 + R.m01 * tmp12 + R.m02 * tmp22;

         m10 = R.m10 * tmp00 + R.m11 * tmp10 + R.m12 * tmp20;
         m11 = R.m10 * tmp01 + R.m11 * tmp11 + R.m12 * tmp21;
         m12 = R.m10 * tmp02 + R.m11 * tmp12 + R.m12 * tmp22;

         m20 = R.m20 * tmp00 + R.m21 * tmp10 + R.m22 * tmp20;
         m21 = R.m20 * tmp01 + R.m21 * tmp11 + R.m22 * tmp21;
         m22 = R.m20 * tmp02 + R.m21 * tmp12 + R.m22 * tmp22;
      }
   }

   /**
    * Applies an inverse rotational transformation R to a matrix M1 and place
    * the result in this matrix. This is equivalent to forming the product
    * 
    * <pre>
    *    R^T M1 R
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M1
    * matrix to transform
    */
   public void inverseTransform (RotationMatrix3d R, Matrix3dBase M1) {
      if (R == this) {
         inverseTransform (new RotationMatrix3d (R), M1);
      }
      else {
         double tmp00 = M1.m00 * R.m00 + M1.m01 * R.m10 + M1.m02 * R.m20;
         double tmp01 = M1.m00 * R.m01 + M1.m01 * R.m11 + M1.m02 * R.m21;
         double tmp02 = M1.m00 * R.m02 + M1.m01 * R.m12 + M1.m02 * R.m22;

         double tmp10 = M1.m10 * R.m00 + M1.m11 * R.m10 + M1.m12 * R.m20;
         double tmp11 = M1.m10 * R.m01 + M1.m11 * R.m11 + M1.m12 * R.m21;
         double tmp12 = M1.m10 * R.m02 + M1.m11 * R.m12 + M1.m12 * R.m22;

         double tmp20 = M1.m20 * R.m00 + M1.m21 * R.m10 + M1.m22 * R.m20;
         double tmp21 = M1.m20 * R.m01 + M1.m21 * R.m11 + M1.m22 * R.m21;
         double tmp22 = M1.m20 * R.m02 + M1.m21 * R.m12 + M1.m22 * R.m22;

         m00 = R.m00 * tmp00 + R.m10 * tmp10 + R.m20 * tmp20;
         m01 = R.m00 * tmp01 + R.m10 * tmp11 + R.m20 * tmp21;
         m02 = R.m00 * tmp02 + R.m10 * tmp12 + R.m20 * tmp22;

         m10 = R.m01 * tmp00 + R.m11 * tmp10 + R.m21 * tmp20;
         m11 = R.m01 * tmp01 + R.m11 * tmp11 + R.m21 * tmp21;
         m12 = R.m01 * tmp02 + R.m11 * tmp12 + R.m21 * tmp22;

         m20 = R.m02 * tmp00 + R.m12 * tmp10 + R.m22 * tmp20;
         m21 = R.m02 * tmp01 + R.m12 * tmp11 + R.m22 * tmp21;
         m22 = R.m02 * tmp02 + R.m12 * tmp12 + R.m22 * tmp22;
      }
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
   protected boolean mulInverseRight (Matrix3dBase M1, Matrix3dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix3d Tmp = new Matrix3d();
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
   protected boolean mulInverseLeft (Matrix3dBase M1, Matrix3dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix3d Tmp = new Matrix3d();
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
   protected boolean mulInverseBoth (Matrix3dBase M1, Matrix3dBase M2) {
      mul (M2, M1);
      return invert();
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd3x3 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd3x3 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd3x3 (this, M1, M2);
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
   public void mul (Vector3d vr, Vector3d v1) {
      double x = m00 * v1.x + m01 * v1.y + m02 * v1.z;
      double y = m10 * v1.x + m11 * v1.y + m12 * v1.z;
      double z = m20 * v1.x + m21 * v1.y + m22 * v1.z;

      vr.x = x;
      vr.y = y;
      vr.z = z;
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
      if (v1.size() < 3) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size 3");
      }
      if (vr.size() < 3) {
         vr.setSize(3);
      }
      double[] res = vr.getBuffer();
      double[] buf = v1.getBuffer();

      double b0 = buf[0];
      double b1 = buf[1];
      double b2 = buf[2];

      res[0] = m00*b0 + m01*b1 + m02*b2;
      res[1] = m10*b0 + m11*b1 + m12*b2;
      res[2] = m20*b0 + m21*b1 + m22*b2;
   }

   /**
    * Multiplies this matrix by the column vector v1, adds the vector v2, and
    * places the result in the vector vr. If M represents this matrix, this is
    * equivalent to computing
    * 
    * <pre>
    *  vr = M v1 + v2
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param v2
    * vector to add
    */
   public void mulAdd (Vector3d vr, Vector3d v1, Vector3d v2) {
      double x = m00 * v1.x + m01 * v1.y + m02 * v1.z;
      double y = m10 * v1.x + m11 * v1.y + m12 * v1.z;
      double z = m20 * v1.x + m21 * v1.y + m22 * v1.z;

      vr.x = x + v2.x;
      vr.y = y + v2.y;
      vr.z = z + v2.z;
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
   public void mul (Vector3d vr) {
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
   public void mulTranspose (Vector3d vr, Vector3d v1) {
      double x = m00 * v1.x + m10 * v1.y + m20 * v1.z;
      double y = m01 * v1.x + m11 * v1.y + m21 * v1.z;
      double z = m02 * v1.x + m12 * v1.y + m22 * v1.z;

      vr.x = x;
      vr.y = y;
      vr.z = z;
   }

   /**
    * Multiplies the transpose of this matrix by the vector v1, adds the vector
    * v2, and places the result in vr. If M represents this matrix, this is
    * equivalent to computing
    * 
    * <pre>
    *  vr = v1 M + v2
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @param v2
    * vector to add
    */
   public void mulTransposeAdd (Vector3d vr, Vector3d v1, Vector3d v2) {
      double x = m00 * v1.x + m10 * v1.y + m20 * v1.z;
      double y = m01 * v1.x + m11 * v1.y + m21 * v1.z;
      double z = m02 * v1.x + m12 * v1.y + m22 * v1.z;

      vr.x = x + v2.x;
      vr.y = y + v2.y;
      vr.z = z + v2.z;
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
   public void mulTranspose (Vector3d vr) {
      mulTranspose (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse of this matrix and places
    * the result in vr. This is equivalent to
    * {@link #solve(Vector3d,Vector3d) solve(vr,v1)}.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return false if this matrix is singular
    */
   public boolean mulInverse (Vector3d vr, Vector3d v1) {
      return solve (vr, v1);
   }

   /**
    * Multiplies the column vector vr by the inverse of this matrix and places
    * the result back in vr.
    * 
    * @param vr
    * vector to multiply by (in place)
    * @return false if this matrix is singular
    */
   public boolean mulInverse (Vector3d vr) {
      return mulInverse (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse transpose of this matrix
    * and places the result in vr. This is equivalent to
    * {@link #solveTranspose(Vector3d,Vector3d) solveTranspose(vr,v1)}.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return false if this matrix is singular
    */
   public boolean mulInverseTranspose (Vector3d vr, Vector3d v1) {
      return solveTranspose (vr, v1);
   }

   /**
    * Multiplies the column vector vr by the inverse transpose of this matrix
    * and places the result back in vr.
    * 
    * @param vr
    * vector to multiply by (in place)
    * @return false if this matrix is singular
    */
   public boolean mulInverseTranspose (Vector3d vr) {
      return mulInverseTranspose (vr, vr);
   }

   /**
    * Adds matrix M1 to M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void add (Matrix3dBase M1, Matrix3dBase M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m12 = M1.m12 + M2.m12;

      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;
      m22 = M1.m22 + M2.m22;
   }

   /**
    * Adds this matrix to M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void add (Matrix3dBase M1) {
      m00 += M1.m00;
      m01 += M1.m01;
      m02 += M1.m02;

      m10 += M1.m10;
      m11 += M1.m11;
      m12 += M1.m12;

      m20 += M1.m20;
      m21 += M1.m21;
      m22 += M1.m22;
   }

   /**
    * Subtracts matrix M1 from M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void sub (Matrix3dBase M1, Matrix3dBase M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m12 = M1.m12 - M2.m12;

      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;
      m22 = M1.m22 - M2.m22;
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void sub (Matrix3dBase M1) {
      m00 -= M1.m00;
      m01 -= M1.m01;
      m02 -= M1.m02;

      m10 -= M1.m10;
      m11 -= M1.m11;
      m12 -= M1.m12;

      m20 -= M1.m20;
      m21 -= M1.m21;
      m22 -= M1.m22;
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
   protected void scale (double s, Matrix3dBase M1) {
      m00 = s * M1.m00;
      m01 = s * M1.m01;
      m02 = s * M1.m02;

      m10 = s * M1.m10;
      m11 = s * M1.m11;
      m12 = s * M1.m12;

      m20 = s * M1.m20;
      m21 = s * M1.m21;
      m22 = s * M1.m22;
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
   protected void scaledAdd (double s, Matrix3dBase M1, Matrix3dBase M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;

      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;
      m22 = s * M1.m22 + M2.m22;
   }

   /**
    * Computes s M1 and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled and added
    */
   protected void scaledAdd (double s, Matrix3dBase M1) {
      m00 += s * M1.m00;
      m01 += s * M1.m01;
      m02 += s * M1.m02;

      m10 += s * M1.m10;
      m11 += s * M1.m11;
      m12 += s * M1.m12;

      m20 += s * M1.m20;
      m21 += s * M1.m21;
      m22 += s * M1.m22;
   }

   /**
    * Sets this matrix to the negative of M1.
    * 
    * @param M1
    * matrix to negate
    */
   protected void negate (Matrix3dBase M1) {
      m00 = -M1.m00;
      m01 = -M1.m01;
      m02 = -M1.m02;

      m10 = -M1.m10;
      m11 = -M1.m11;
      m12 = -M1.m12;

      m20 = -M1.m20;
      m21 = -M1.m21;
      m22 = -M1.m22;
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
      double tmp12 = m12;

      m01 = m10;
      m02 = m20;
      m12 = m21;

      m10 = tmp01;
      m20 = tmp02;
      m21 = tmp12;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   protected void transpose (Matrix3dBase M) {
      double tmp01 = M.m01;
      double tmp02 = M.m02;
      double tmp12 = M.m12;

      m00 = M.m00;
      m11 = M.m11;
      m22 = M.m22;

      m01 = M.m10;
      m02 = M.m20;
      m12 = M.m21;

      m10 = tmp01;
      m20 = tmp02;
      m21 = tmp12;
   }

   /**
    * Sets this matrix to the identity.
    */
   public void setIdentity() {
      m00 = 1.0;
      m01 = 0.0;
      m02 = 0.0;

      m10 = 0.0;
      m11 = 1.0;
      m12 = 0.0;

      m20 = 0.0;
      m21 = 0.0;
      m22 = 1.0;
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   protected void setZero() {
      m00 = 0.0;
      m01 = 0.0;
      m02 = 0.0;

      m10 = 0.0;
      m11 = 0.0;
      m12 = 0.0;

      m20 = 0.0;
      m21 = 0.0;
      m22 = 0.0;
   }

   /**
    * Returns true if the elements of this matrix equal those of matrix
    * <code>M1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param M1
    * matrix to compare with
    * @param eps
    * comparison tolerance
    * @return false if the matrices are not equal within the specified tolerance
    */
   public boolean epsilonEquals (Matrix3dBase M1, double eps) {
      if (abs (m00 - M1.m00) <= eps &&
          abs (m01 - M1.m01) <= eps &&
          abs (m02 - M1.m02) <= eps &&
          abs (m10 - M1.m10) <= eps &&
          abs (m11 - M1.m11) <= eps &&
          abs (m12 - M1.m12) <= eps &&
          abs (m20 - M1.m20) <= eps &&
          abs (m21 - M1.m21) <= eps &&
          abs (m22 - M1.m22) <= eps) {
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
   public boolean equals (Matrix3dBase M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&

      (m10 == M1.m10) && (m11 == M1.m11) && (m12 == M1.m12) &&

      (m20 == M1.m20) && (m21 == M1.m21) && (m22 == M1.m22)) {
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
      double max, sum;
      max = abs (m00) + abs (m01) + abs (m02);
      sum = abs (m10) + abs (m11) + abs (m12);
      if (sum > max) {
         max = sum;
      }
      sum = abs (m20) + abs (m21) + abs (m22);
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
      double max, sum;
      max = abs (m00) + abs (m10) + abs (m20);
      sum = abs (m01) + abs (m11) + abs (m21);
      if (sum > max) {
         max = sum;
      }
      sum = abs (m02) + abs (m12) + abs (m22);
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
         (m00 * m00 + m10 * m10 + m20 * m20 +
          m01 * m01 + m11 * m11 + m21 * m21 +
          m02 * m02 + m12 * m12 + m22 * m22);
      return Math.sqrt (sum);
   }

   /**
    * Inverts this matrix in place, returning false if the matrix is detected to
    * be singular. The inverse is computed using an unrolled LU decomposition
    * with partial pivoting.
    */
   public boolean invert() {
      return invert (this);
   }

   /**
    * Quickly inverts the matrix M using the determinant formula and returns
    * the computed determinant. If the determinant is 0, no inversion
    * is performed.
    *
    * @param M matrix to invert
    * @return determinant of M
    */
   protected double fastInvert (Matrix3dBase M) {

      double d00 = M.m00;
      double d01 = M.m01;
      double d02 = M.m02;
      
      double d10 = M.m10;
      double d11 = M.m11;
      double d12 = M.m12;
      
      double d20 = M.m20;
      double d21 = M.m21;
      double d22 = M.m22;

      double det = (d00*d11*d22 + d10*d21*d02 + d20*d01*d12 -
                    d20*d11*d02 - d00*d21*d12 - d10*d01*d22);

      if (det != 0) {
         double deti = 1.0 / det;

         m00 =  deti*(d11*d22 - d12*d21);
         m10 =  deti*(d12*d20 - d10*d22);
         m20 =  deti*(d10*d21 - d11*d20);
	
         m01 =  deti*(d02*d21 - d01*d22);
         m11 =  deti*(d00*d22 - d02*d20);
         m21 =  deti*(d01*d20 - d00*d21);

         m02 =  deti*(d01*d12 - d11*d02);
         m12 =  deti*(d02*d10 - d00*d12);
         m22 =  deti*(d00*d11 - d01*d10);
      }
      return det;
   }

   /**
    * Inverts the matrix M and places the result in this matrix, return false if
    * M is detected to be singular. The inverse is computed using an unrolled LU
    * decomposition with partial pivoting.
    * 
    * @param M
    * matrix to invert
    * @return false if M is singular
    */
   protected boolean invert (Matrix3dBase M) {
      boolean singular = false;
      double tmp;

      // j == 0

      double u00 = M.m00;
      double u01 = M.m01;
      double u02 = M.m02;

      double l10 = M.m10;
      double u11 = M.m11;
      double u12 = M.m12;

      double l20 = M.m20;
      double l21 = M.m21;
      double u22 = M.m22;

      int piv0 = 0;

      double max = (u00 >= 0 ? u00 : -u00);
      double abs = (l10 >= 0 ? l10 : -l10);
      if (max < abs) {
         max = abs;
         piv0 = 1;
      }
      abs = (l20 >= 0 ? l20 : -l20);
      if (max < abs) {
         piv0 = 2;
      }

      if (piv0 == 1) {
         tmp = u00;
         u00 = l10;
         l10 = tmp;
      }
      else if (piv0 == 2) {
         tmp = u00;
         u00 = l20;
         l20 = tmp;
      }

      // if (u00 != 0)
      // { l10 /= u00;
      // l20 /= u00;
      // }
      double u00_inv = 1 / u00;
      if (u00 != 0) {
         l10 *= u00_inv;
         l20 *= u00_inv;
      }
      else {
         singular = true;
      }

      // j == 1

      if (piv0 == 1) {
         tmp = u01;
         u01 = u11;
         u11 = tmp;
      }
      else if (piv0 == 2) {
         tmp = u01;
         u01 = l21;
         l21 = tmp;
      }

      u11 -= l10 * u01;
      l21 -= l20 * u01;

      max = (u11 >= 0 ? u11 : -u11);
      abs = (l21 >= 0 ? l21 : -l21);
      int piv1 = (max >= abs ? 1 : 2);
      // int piv1 = (ABS(u11) >= ABS(l21) ? 1 : 2);
      // for k = 0:1
      // m(1,k) <-> m(piv1,k)
      if (piv1 == 2) {
         tmp = l10;
         l10 = l20;
         l20 = tmp;
         tmp = u11;
         u11 = l21;
         l21 = tmp;
      }

      double u11_inv = 1 / u11;
      if (u11 != 0) {
         l21 *= u11_inv;
      }
      else {
         singular = true;
      }

      // j == 2

      // for k=0:1
      // A(k,2) <-> A(piv(k),2)

      if (piv0 == 1) {
         tmp = u02;
         u02 = u12;
         u12 = tmp;
      }
      else if (piv0 == 2) {
         tmp = u02;
         u02 = u22;
         u22 = tmp;
      }
      if (piv1 == 2) {
         tmp = u12;
         u12 = u22;
         u22 = tmp;
      }

      // for k=0:1
      // for i=k+1:1
      // A(i,2) -= A(i,k)A(k,2)

      u12 -= l10 * u02;

      // for k=0:1
      // A(2,2) -= A(2,k)A(k,2)

      u22 -= (l20 * u02 + l21 * u12);
      double u22_inv = 1 / u22;
      if (u22 == 0) {
         singular = true;
      }

      m20 = (-l20 + l21 * l10) * u22_inv;
      m10 = (-l10 - u12 * m20) * u11_inv;
      m00 = (1 - u02 * m20 - u01 * m10) * u00_inv;

      m21 = -l21 * u22_inv;
      m11 = (1 - u12 * m21) * u11_inv;
      m01 = (-u02 * m21 - u01 * m11) * u00_inv;

      m22 = u22_inv;
      m12 = -u12 * m22 * u11_inv;
      m02 = (-u02 * m22 - u01 * m12) * u00_inv;

      if (piv0 == 0) {
         if (piv1 == 2) // ex cols 1 and 2
         {
            tmp = m01;
            m01 = m02;
            m02 = tmp;
            tmp = m11;
            m11 = m12;
            m12 = tmp;
            tmp = m21;
            m21 = m22;
            m22 = tmp;
         }
      }
      else if (piv0 == 1) {
         if (piv1 == 1) // exchanges cols 0 and 1
         {
            tmp = m00;
            m00 = m01;
            m01 = tmp;
            tmp = m10;
            m10 = m11;
            m11 = tmp;
            tmp = m20;
            m20 = m21;
            m21 = tmp;
         }
         else // piv1 == 2 // rotate cols left
         {
            tmp = m00;
            m00 = m02;
            m02 = m01;
            m01 = tmp;
            tmp = m10;
            m10 = m12;
            m12 = m11;
            m11 = tmp;
            tmp = m20;
            m20 = m22;
            m22 = m21;
            m21 = tmp;
         }
      }
      else // piv0 == 2
      {
         if (piv1 == 1) // exchanges cols 0 and 2
         {
            tmp = m00;
            m00 = m02;
            m02 = tmp;
            tmp = m10;
            m10 = m12;
            m12 = tmp;
            tmp = m20;
            m20 = m22;
            m22 = tmp;
         }
         else // piv1 == 2 // rotate cols right
         {
            tmp = m00;
            m00 = m01;
            m01 = m02;
            m02 = tmp;
            tmp = m10;
            m10 = m11;
            m11 = m12;
            m12 = tmp;
            tmp = m20;
            m20 = m21;
            m21 = m22;
            m22 = tmp;
         }
      }
      return !singular;
   }

   /**
    * Solves this matrix for <code>x</code> given a right hand side
    * <code>b</code>, returning <code>false</code> if the matrix is detected
    * to be singular. The solution is computed using partial pivoting.
    * 
    * @param x result
    * @param b right hand side
    * @return false if this matrix is singular
    */
   public boolean solve (Vector3d x, Vector3d b) {
      return doSolve (x, b, /*transpose=*/false);
   }

   /**
    * Solves the transpose of this matrix for <code>x</code> given a right hand
    * side <code>b</code>, returning <code>false</code> if the matrix is
    * detected to be singular. The solution is computed using partial pivoting.
    * 
    * @param x result
    * @param b right hand side
    * @return false if this matrix is singular
    */
   public boolean solveTranspose (Vector3d x, Vector3d b) {
      return doSolve (x, b, /*transpose=*/true);
   }

   protected boolean doSolve (Vector3d x, Vector3d b, boolean transpose) {
      boolean singular = false;

      // Ignoring pivoting, this method perfoms an unrolled reduction of the
      // system
      //
      // m00 m01 m02 | b.x
      // m10 m11 m12 | b.y
      // m20 m21 m22 | b.z
      //
      // first to row echelon form
      //
      //  1  t01 t02 | t03
      //  0   1  t12 | t13
      //  0   0   1  | t23
      //
      // and finally to reduced row form
      //
      //  1   0   0  | x.x
      //  0   1   0  | x.y
      //  0   0   1  | x.z
      //
      // In addition, row pivoting is used to ensure stability.

      double t00, t01, t02, t03;
      double t10, t11, t12, t13;
      double t20, t21, t22, t23;

      t00 = m00;
      t11 = m11;
      t22 = m22;
      t03 = b.x;
      t13 = b.y;
      t23 = b.z;

      if (transpose) {
         t10 = m01; t20 = m02; t01 = m10;
         t21 = m12; t02 = m20; t12 = m21;
      }
      else {
         t10 = m10; t20 = m20; t01 = m01;
         t21 = m21; t02 = m02; t12 = m12;
      }
      int piv0 = 0;

      double max = (t00 >= 0 ? t00 : -t00);
      double abs = (t10 >= 0 ? t10 : -t10);
      if (max < abs) {
         max = abs;
         piv0 = 1;
      }
      abs = (t20 >= 0 ? t20 : -t20);
      if (max < abs) {
         piv0 = 2;
      }
      double inv;

      switch (piv0) {
         case 0: {
            if (t00 == 0) {
               singular = true;
            }
            inv = 1/t00;
            t01 *= inv; t02 *= inv; t03 *= inv;
            t11 -= t10*t01; t12 -= t10*t02; t13 -= t10*t03;
            t21 -= t20*t01; t22 -= t20*t02; t23 -= t20*t03;

            if (Math.abs(t11) >= Math.abs(t21)) {
               if (t11 == 0) {
                  singular = true;
               }
               inv = 1/t11;
               t12 *= inv; t13 *= inv;
               t22 -= t21*t12; t23 -= t21*t13;
               if (t22 == 0) {
                  singular = true;
               }
               t23 /= t22;
               t13 -= t23*t12; t03 -= t23*t02; t03 -= t01*t13;
               x.x = t03; x.y = t13; x.z = t23;
            }
            else {
               // exchange rows 1 and 2
               if (t21 == 0) {
                  singular = true;
               }
               inv = 1/t21;
               t22 *= inv; t23 *= inv;
               t12 -= t11*t22; t13 -= t11*t23;
               if (t12 == 0) {
                  singular = true;
               }
               t13 /= t12;
               t23 -= t13*t22; t03 -= t13*t02; t03 -= t01*t23;
               x.x = t03; x.y = t23; x.z = t13;
            }
            break;
         }
         case 1: {
            // exchange rows 0 and 1
            if (t10 == 0) {
               singular = true;
            }
            inv = 1/t10;
            t11 *= inv; t12 *= inv; t13 *= inv;
            t01 -= t00*t11; t02 -= t00*t12; t03 -= t00*t13;
            t21 -= t20*t11; t22 -= t20*t12; t23 -= t20*t13;
            
            if (Math.abs(t01) >= Math.abs(t21)) {
               if (t01 == 0) {
                  singular = true;
               }
               inv = 1/t01;
               t02 *= inv; t03 *= inv;
               t22 -= t21*t02; t23 -= t21*t03;
               if (t22 == 0) {
                  singular = true;
               }
               t23 /= t22;
               t03 -= t23*t02; t13 -= t23*t12; t13 -= t11*t03;
               x.x = t13; x.y = t03; x.z = t23;
            }
            else {
               // exchange rows 0 and 2
               if (t21 == 0) {
                  singular = true;
               }
               inv = 1/t21;
               t22 *= inv; t23 *= inv;
               t02 -= t01*t22; t03 -= t01*t23;
               if (t02 == 0) {
                  singular = true;
               }
               t03 /= t02;
               t23 -= t03*t22; t13 -= t03*t12; t13 -= t11*t23;
               x.x = t13; x.y = t23; x.z = t03;
            }
            break;
         }
         case 2: {
            // exchange rows 0 and 2
            if (t20 == 0) {
               singular = true;
            }
            inv = 1/t20;
            t21 *= inv; t22 *= inv; t23 *= inv;
            t11 -= t10*t21; t12 -= t10*t22; t13 -= t10*t23;
            t01 -= t00*t21; t02 -= t00*t22; t03 -= t00*t23;
            
            if (Math.abs(t11) >= Math.abs(t01)) {
               if (t11 == 0) {
                  singular = true;
               }
               inv = 1/t11;
               t12 *= inv; t13 *= inv;
               t02 -= t01*t12; t03 -= t01*t13;
               if (t02 == 0) {
                  singular = true;
               }
               t03 /= t02;
               t13 -= t03*t12; t23 -= t03*t22; t23 -= t21*t13;
               x.x = t23; x.y = t13; x.z = t03;
            }
            else {
               // exchange rows 1 and 0
               if (t01 == 0) {
                  singular = true;
               }
               inv = 1/t01;
               t02 *= inv; t03 *= inv;
               t12 -= t11*t02; t13 -= t11*t03;
               if (t12 == 0) {
                  singular = true;
               }
               t13 /= t12;
               t03 -= t13*t02; t23 -= t13*t22; t23 -= t21*t03;
               x.x = t23; x.y = t03; x.z = t13;
            }
            break;
         }
      }
      return singular;
   }

   /**
    * Solves this matrix for <code>x</code> given a right hand side
    * <code>b</code>, returning <code>false</code> if the matrix is detected
    * to be singular. The solution is computed using partial pivoting.
    * 
    * @param x result
    * @param b right hand side
    * @return false if this matrix is singular
    */
   protected boolean solveNopivot (Vector3d x, Vector3d b) {
      boolean singular = false;

      double t01 = m01;
      double t11 = m11;
      double t21 = m21;

      double t02 = m02;
      double t12 = m12;
      double t22 = m22;

      double t03 = b.x;
      double t13 = b.y;
      double t23 = b.z;

      double inv;
      if (m00 == 0) {
         singular = true;
      }
      inv = 1/m00;
      t01 *= inv;
      t02 *= inv;
      t03 *= inv;
      t11 -= m10*t01;
      t12 -= m10*t02;
      t13 -= m10*t03;
      t21 -= m20*t01;
      t22 -= m20*t02;
      t23 -= m20*t03;
      
      if (t11 == 0) {
         singular = true;
      }
      inv = 1/t11;
      t12 *= inv;
      t13 *= inv;
      t22 -= t21*t12;
      t23 -= t21*t13;
      if (t22 == 0) {
         singular = true;
      }
      t23 /= t22;
      t13 -= t23*t12;
      t03 -= t23*t02;
      t03 -= t01*t13;

      x.x = t03;
      x.y = t13;
      x.z = t23;

      return singular;
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified by diag.
    * 
    * @param diag
    * diagonal values
    */
   protected void setDiagonal (Vector3d diag) {
      m00 = diag.x;
      m01 = 0;
      m02 = 0;

      m10 = 0;
      m11 = diag.y;
      m12 = 0;

      m20 = 0;
      m21 = 0;
      m22 = diag.z;
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified by the
    * array vals.
    * 
    * @param vals
    * diagonal values
    */
   protected void setDiagonal (double[] vals) {
      m00 = vals[0];
      m01 = 0;
      m02 = 0;

      m10 = 0;
      m11 = vals[1];
      m12 = 0;

      m20 = 0;
      m21 = 0;
      m22 = vals[2];
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified.
    * 
    * @param m00 first diagonal value
    * @param m11 second diagonal value
    * @param m22 third diagonal value
    */
   protected void setDiagonal (double m00, double m11, double m22) {
      this.m00 = m00;
      this.m01 = 0;
      this.m02 = 0;

      this.m10 = 0;
      this.m11 = m11;
      this.m12 = 0;

      this.m20 = 0;
      this.m21 = 0;
      this.m22 = m22;
   }

   /**
    * Returns the determinant of this matrix
    * 
    * @return matrix determinant
    */
   public double determinant() throws ImproperSizeException {
      return (m00 * m11 * m22 + m10 * m21 * m02 + m20 * m01 * m12 -
              m20 * m11 * m02 - m00 * m21 * m12 - m10 * m01 * m22);
   }

   /**
    * Returns true if this matrix equals the identity.
    * 
    * @return true if this matrix equals the identity
    */
   public boolean isIdentity() {
      return (m00 == 1 && m01 == 0 && m02 == 0 &&
              m10 == 0 && m11 == 1 && m12 == 0 &&
              m20 == 0 && m21 == 0 && m22 == 1);
   }

   /**
    * Returns the determinant of this matrix, assuming that it is orthogonal.
    * This is done by computing the cross product of the first
    * 
    * 
    * two columns and then computing its dot product with the third column.
    * 
    * @return orthogonal matrix determinant
    */
   public double orthogonalDeterminant() {
      double cx = m10 * m21 - m20 * m11;
      double cy = m20 * m01 - m00 * m21;
      double cz = m00 * m11 - m10 * m01;
      return cx * m02 + cy * m12 + cz * m22;
   }

   /**
    * Adds an outer product to this matrix. The outer product
    * is formed from two vectors whose coordinates are given
    * as arguments.
    * 
    * @param x0
    * first vector x coordinate
    * @param y0
    * first vector y coordinate
    * @param z0
    * first vector z coordinate
    * @param x1
    * second vector x coordinate
    * @param y1
    * second vector y coordinate
    * @param z1
    * second vector z coordinate
    */
   protected void addOuterProduct (
      double x0, double y0, double z0, double x1, double y1, double z1) {
      
      m00 += x0*x1;
      m10 += y0*x1;
      m20 += z0*x1;

      m01 += x0*y1;
      m11 += y0*y1;
      m21 += z0*y1;

      m02 += x0*z1;
      m12 += y0*z1;
      m22 += z0*z1;
   }

   /**
    * Adds an outer product to this matrix. The outer product
    * is formed from two vectors are given as arguments.
    * 
    * @param v0
    * first vector
    * @param v1
    * second vector
    */
   protected void addOuterProduct (Vector3d v0, Vector3d v1) {

      m00 += v0.x*v1.x;
      m10 += v0.y*v1.x;
      m20 += v0.z*v1.x;

      m01 += v0.x*v1.y;
      m11 += v0.y*v1.y;
      m21 += v0.z*v1.y;

      m02 += v0.x*v1.z;
      m12 += v0.y*v1.z;
      m22 += v0.z*v1.z;
   }

   /**
    * Adds a scaled outer product to this matrix. The outer product
    * is formed from two vectors are given as arguments, and takes
    * the form
    * <pre>
    *        T
    * s v1 v2
    * </pre>
    *
    * @param s scaling factor
    * @param v0 first vector
    * @param v1 second vector
    */
   protected void addScaledOuterProduct (double s, Vector3d v0, Vector3d v1) {

      double v1x = s*v1.x;
      double v1y = s*v1.y;
      double v1z = s*v1.z;

      m00 += v0.x*v1x;
      m10 += v0.y*v1x;
      m20 += v0.z*v1x;

      m01 += v0.x*v1y;
      m11 += v0.y*v1y;
      m21 += v0.z*v1y;

      m02 += v0.x*v1z;
      m12 += v0.y*v1z;
      m22 += v0.z*v1z;
   }
   
   /**
    * Adds a scaled outer product to this matrix. The outer product
    * is formed from the single vector <code>v</code> and takes
    * the form
    * <pre>
    *      T
    * s v v
    * </pre>
    * The product is hence symmetric.
    * 
    * @param s scaling factor
    * @param v vector used to form outer product
    */
   protected void addScaledOuterProduct (double s, Vector3d v) {

      double svx = s*v.x;
      double svy = s*v.y;
      double svz = s*v.z;
      
      double op01 = v.x*svy;
      double op02 = v.x*svz;
      double op12 = v.y*svz;
      
      m00 += v.x*svx;
      m11 += v.y*svy;
      m22 += v.z*svz;

      m01 += op01;
      m02 += op02;
      m12 += op12;
      
      m10 += op01;
      m20 += op02;
      m21 += op12;
   }  

   /**
    * Sets this matrix to the outer product
    * <pre>
    * v0 v1^T
    * </pre>
    * 
    * @param v0
    * first vector
    * @param v1
    * second vector
    */
   protected void outerProduct (Vector3d v0, Vector3d v1) {

      m00 = v0.x*v1.x;
      m10 = v0.y*v1.x;
      m20 = v0.z*v1.x;

      m01 = v0.x*v1.y;
      m11 = v0.y*v1.y;
      m21 = v0.z*v1.y;

      m02 = v0.x*v1.z;
      m12 = v0.y*v1.z;
      m22 = v0.z*v1.z;
   }

   /** 
    * Computes the QR decompostion for this matrix, for which
    * <pre>
    *    M = Q R
    * </pre>
    * where Q is orthogonal and R is upper triangular.
    * 
    * @param Q if non-null, returns Q
    * @param R if non-null, returns R
    *
    * The computation is the same as that performed by the QRDecomposition
    * class, except significantly faster.
    */
   public void factorQR (Matrix3dBase Q, Matrix3dBase R) {
      /* The QR reduction can be done with two Householder steps:

         R = this matrix
         v0 = house(R(:,0));
         rowHouseMul (R, v0);
         v1 = house(R(1:2,1));
         rowHouseMul (R(1:2,1:2),v1);
         
         Q = I;
         rowHouseMul (Q(1:2,1:2),v1);
         rowHouseMul (Q,v0);
      */
      if (R == null) {
         R = new Matrix3d();
      }
      R.set (this);
      
      double v10, v20, v21; // components of house vectors v0 and v1
      double w0, w1, w2; // components of temporary vector w
      double len, beta;

      // v0 = house(R(:,0));
      len = Math.sqrt(R.m00*R.m00 + R.m10*R.m10 + R.m20*R.m20);
      if (len != 0) {
         beta = R.m00 + (R.m00 >= 0 ? len : -len);
         v10 = R.m10/beta;
         v20 = R.m20/beta;
      }
      else {
         v10 = v20 = 0;
      }

      // rowHouseMul (R, v0);
      beta = -2/(1+v10*v10+v20*v20);
      w0 = beta*(R.m00 + R.m10*v10 + R.m20*v20);
      w1 = beta*(R.m01 + R.m11*v10 + R.m21*v20);
      w2 = beta*(R.m02 + R.m12*v10 + R.m22*v20);
      R.addOuterProduct (1, v10, v20, w0, w1, w2);

      //v1 = house(R(1:2,1));
      len = Math.sqrt(R.m11*R.m11 + R.m21*R.m21);
      if (len != 0) {
         beta = R.m11 + (R.m11 >= 0 ? len : -len);
         v21 = R.m21/beta;
      }
      else {
         v21 = 0;
      }

      // rowHouseMul (R, v0);
      beta = -2/(1+v21*v21);
      w1 = beta*(R.m11 + R.m21*v21);
      w2 = beta*(R.m12 + R.m22*v21);
      R.addOuterProduct (0, 1, v21, 0, w1, w2);

      R.m10 = R.m20 = R.m21 = 0;

      if (Q != null) {
         Q.setIdentity();

         // rowHouseMul (Q(1:2,1:2),v1);
         beta = -2/(1+v21*v21);
         w1 = beta*(Q.m11 + Q.m21*v21);
         w2 = beta*(Q.m12 + Q.m22*v21);
         Q.addOuterProduct (0, 1, v21, 0, w1, w2);

         // rowHouseMul (Q,v0);
         beta = -2/(1+v10*v10+v20*v20);
         w0 = beta*(Q.m00 + Q.m10*v10 + Q.m20*v20);
         w1 = beta*(Q.m01 + Q.m11*v10 + Q.m21*v20);
         w2 = beta*(Q.m02 + Q.m12*v10 + Q.m22*v20);
         Q.addOuterProduct (1, v10, v20, w0, w1, w2);
      }
   }

   public Matrix3dBase clone() {
      try {
         return (Matrix3dBase)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for Matrix3dBase");
      }
   }

   /** 
    * Returns the trace of this matrix.
    * 
    * @return matrix trace
    */
   public double trace() {
      return m00 + m11 + m22;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSymmetric (double tol) {
      return ((Math.abs (m01-m10) <= tol) && 
              (Math.abs (m02-m20) <= tol) &&
              (Math.abs (m12-m21) <= tol));
   }

   /**
    * Negates a column of this matrix.
    * 
    * @param colIdx index of the column to negate
    */
   public void negateColumn (int colIdx) {
      switch (colIdx) {
         case 0: m00 = -m00; m10 = -m10; m20 = -m20; break;
         case 1: m01 = -m01; m11 = -m11; m21 = -m21; break;
         case 2: m02 = -m02; m12 = -m12; m22 = -m22; break;
         default: 
            throw new ArrayIndexOutOfBoundsException (
               "column index is "+colIdx+"; must be 0, 1, or 2");
      }
   }

   /**
    * Negates a row of this matrix.
    * 
    * @param rowIdx index of the row to negate
    */
   public void negateRow (int rowIdx) {
      switch (rowIdx) {
         case 0: m00 = -m00; m01 = -m01; m02 = -m02; break;
         case 1: m10 = -m10; m11 = -m11; m12 = -m12; break;
         case 2: m20 = -m20; m21 = -m21; m22 = -m22; break;
         default: 
            throw new ArrayIndexOutOfBoundsException (
               "row index is "+rowIdx+"; must be 0, 1, or 2");
      }
   }

   public static void main (String[] args) {
      FunctionTimer timer = new FunctionTimer();

      Matrix3d M = new Matrix3d();
      M.setRandom ();
      int cnt = 1000000;
      Matrix3d R = new Matrix3d();

      for (int i=0; i<cnt; i++){
         R.fastInvert (M);
         R.invert (M);
      }

      timer.start();
      for (int i=0; i<cnt; i++){
         R.fastInvert (M);
      }
      timer.stop();
      System.out.println ("fast invert=" + timer.result(cnt));
      
      timer.start();
      for (int i=0; i<cnt; i++){
         R.invert (M);
      }
      timer.stop();
      System.out.println ("invert=" + timer.result(cnt));
   }


}
