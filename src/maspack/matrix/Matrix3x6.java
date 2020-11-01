/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 3 x 6 matrix
 */
public class Matrix3x6 extends DenseMatrixBase
   implements VectorObject<Matrix3x6> {
   
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

   public double m20;
   public double m21;
   public double m22;
   public double m23;
   public double m24;
   public double m25;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix3x6 ZERO = new Matrix3x6();

   /**
    * Creates a new Matrix3x6.
    */
   public Matrix3x6() {
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
               case 4:
                  return m14;
               case 5:
                  return m15;
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
               case 4:
                  return m24;
               case 5:
                  return m25;
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
         case 4: {
            values[0 + off] = m04;
            values[1 + off] = m14;
            values[2 + off] = m24;
            break;
         }
         case 5: {
            values[0 + off] = m05;
            values[1 + off] = m15;
            values[2 + off] = m25;
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
         case 4: {
            col.x = m04;
            col.y = m14;
            col.z = m24;
            break;
         }
         case 5: {
            col.x = m05;
            col.y = m15;
            col.z = m25;
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
         case 2: {
            values[0 + off] = m20;
            values[1 + off] = m21;
            values[2 + off] = m22;
            values[3 + off] = m23;
            values[4 + off] = m24;
            values[5 + off] = m25;
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
               case 4:
                  m04 = value;
                  return;
               case 5:
                  m05 = value;
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
               case 4:
                  m14 = value;
                  return;
               case 5:
                  m15 = value;
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
               case 4:
                  m24 = value;
                  return;
               case 5:
                  m25 = value;
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
         case 2: {
            m20 = values[0];
            m21 = values[1];
            m22 = values[2];
            m23 = values[3];
            m24 = values[4];
            m25 = values[5];
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
         case 4: {
            m04 = values[0];
            m14 = values[1];
            m24 = values[2];
            break;
         }
         case 5: {
            m05 = values[0];
            m15 = values[1];
            m25 = values[2];
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
         case 4: {
            m04 = col.x;
            m14 = col.y;
            m24 = col.z;
            break;
         }
         case 5: {
            m05 = col.x;
            m15 = col.y;
            m25 = col.z;
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
      m04 = 0;
      m05 = 0;

      m10 = 0;
      m11 = 0;
      m12 = 0;
      m13 = 0;
      m14 = 0;
      m15 = 0;

      m20 = 0;
      m21 = 0;
      m22 = 0;
      m23 = 0;
      m24 = 0;
      m25 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix3x6) {
         set ((Matrix3x6)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 6) {
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

         m20 = M.get (2, 0);
         m21 = M.get (2, 1);
         m22 = M.get (2, 2);
         m23 = M.get (2, 3);
         m24 = M.get (2, 4);
         m25 = M.get (2, 5);
      }
   }

   /**
    * Sets the contents of this Matrix3x6 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix3x6 M) {
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

      m20 = M.m20;
      m21 = M.m21;
      m22 = M.m22;
      m23 = M.m23;
      m24 = M.m24;
      m25 = M.m25;
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

      m20 *= s;
      m21 *= s;
      m22 *= s;
      m23 *= s;
      m24 *= s;
      m25 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix3x6 M) {
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

      m20 = s * M.m20;
      m21 = s * M.m21;
      m22 = s * M.m22;
      m23 = s * M.m23;
      m24 = s * M.m24;
      m25 = s * M.m25;
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
      if (M instanceof Matrix3x6) {
         add ((Matrix3x6)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 6) {
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

         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
         m22 += M.get (2, 2);
         m23 += M.get (2, 3);
         m24 += M.get (2, 4);
         m25 += M.get (2, 5);
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
      if (M instanceof Matrix3x6) {
         scaledAdd (s, (Matrix3x6)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 6) {
            throw new ImproperSizeException (
               "matrix of size "+M.getSize()+" does not conform");
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

         m20 += s * M.get (2, 0);
         m21 += s * M.get (2, 1);
         m22 += s * M.get (2, 2);
         m23 += s * M.get (2, 3);
         m24 += s * M.get (2, 4);
         m25 += s * M.get (2, 5);
      }
   }

   /**
    * Adds the contents of a Matrix3x6 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix3x6 M) {
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

      m20 += M.m20;
      m21 += M.m21;
      m22 += M.m22;
      m23 += M.m23;
      m24 += M.m24;
      m25 += M.m25;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix3x6 M1, Matrix3x6 M2) {
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

      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;
      m22 = M1.m22 + M2.m22;
      m23 = M1.m23 + M2.m23;
      m24 = M1.m24 + M2.m24;
      m25 = M1.m25 + M2.m25;
   }

   /**
    * Computes s M and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix3x6 M) {
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

      m20 += s * M.m20;
      m21 += s * M.m21;
      m22 += s * M.m22;
      m23 += s * M.m23;
      m24 += s * M.m24;
      m25 += s * M.m25;
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
   public void scaledAdd (double s, Matrix3x6 M1, Matrix3x6 M2) {
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

      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;
      m22 = s * M1.m22 + M2.m22;
      m23 = s * M1.m23 + M2.m23;
      m24 = s * M1.m24 + M2.m24;
      m25 = s * M1.m25 + M2.m25;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix3x6 M) {
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

      m20 = -M.m20;
      m21 = -M.m21;
      m22 = -M.m22;
      m23 = -M.m23;
      m24 = -M.m24;
      m25 = -M.m25;
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
      if (M instanceof Matrix3x6) {
         sub ((Matrix3x6)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 6) {
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

         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
         m22 -= M.get (2, 2);
         m23 -= M.get (2, 3);
         m24 -= M.get (2, 4);
         m25 -= M.get (2, 5);
      }
   }

   /**
    * Subtracts the contents of a Matrix3x6 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix3x6 M) {
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

      m20 -= M.m20;
      m21 -= M.m21;
      m22 -= M.m22;
      m23 -= M.m23;
      m24 -= M.m24;
      m25 -= M.m25;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix3x6 M1, Matrix3x6 M2) {
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

      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;
      m22 = M1.m22 - M2.m22;
      m23 = M1.m23 - M2.m23;
      m24 = M1.m24 - M2.m24;
      m25 = M1.m25 - M2.m25;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd3x6 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd3x6 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd3x6 (this, M1, M2);
   }

   /** 
    * Gets the 3x3 sub-matrix of this matrix starting at (0, 0).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix00 (Matrix3dBase M) {
      M.m00 = m00; M.m01 = m01; M.m02 = m02; 
      M.m10 = m10; M.m11 = m11; M.m12 = m12; 
      M.m20 = m20; M.m21 = m21; M.m22 = m22; 
   }
   
   /** 
    * Gets the 3x3 sub-matrix of this matrix starting at (0, 3).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix03 (Matrix3dBase M) {
      M.m00 = m03; M.m01 = m04; M.m02 = m05; 
      M.m10 = m13; M.m11 = m14; M.m12 = m15; 
      M.m20 = m23; M.m21 = m24; M.m22 = m25; 
   }
   
   /** 
    * Sets the 3x3 sub-matrix of this matrix starting at (0, 0).
    * 
    * @param M new sub matrix value
    */
   public void setSubMatrix00 (Matrix3dBase M) {
      m00 = M.m00; m01 = M.m01; m02 = M.m02; 
      m10 = M.m10; m11 = M.m11; m12 = M.m12; 
      m20 = M.m20; m21 = M.m21; m22 = M.m22; 
   }
   
   /** 
    * Sets the 3x3 sub-matrix of this matrix starting at (0, 3).
    * 
    * @param M new sub matrix value
    */
   public void setSubMatrix03 (Matrix3dBase M) {
      m03 = M.m00; m04 = M.m01; m05 = M.m02; 
      m13 = M.m10; m14 = M.m11; m15 = M.m12; 
      m23 = M.m20; m24 = M.m21; m25 = M.m22; 
   }
   
   /** 
    * Adds to 3x3 sub-matrix of this matrix starting at (0, 0).
    * 
    * @param M sub matrix to add
    */
   public void addSubMatrix00 (Matrix3dBase M) {
      m00 += M.m00; m01 += M.m01; m02 += M.m02; 
      m10 += M.m10; m11 += M.m11; m12 += M.m12; 
      m20 += M.m20; m21 += M.m21; m22 += M.m22; 
   }
   
   /** 
    * Adds to the 3x3 sub-matrix of this matrix starting at (0, 3).
    * 
    * @param M sub matrix to add
    */
   public void addSubMatrix03 (Matrix3dBase M) {
      m03 += M.m00; m04 += M.m01; m05 += M.m02; 
      m13 += M.m10; m14 += M.m11; m15 += M.m12; 
      m23 += M.m20; m24 += M.m21; m25 += M.m22; 
   }
   
   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix6x3 M) {
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
      m04 = M.m40;
      m14 = M.m41;
      m24 = M.m42;
      m05 = M.m50;
      m15 = M.m51;
      m25 = M.m52;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix3x6 clone() {
      try {
         return (Matrix3x6)super.clone();
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
   public void addObj (Matrix3x6 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix3x6 M1) {
      scaledAdd (s, M1);
   }
 
   /**
     * {@inheritDoc}
     */
   public boolean epsilonEquals (Matrix3x6 M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && abs (m03 - M1.m03) <= epsilon &&
          abs (m04 - M1.m04) <= epsilon && abs (m05 - M1.m05) <= epsilon &&

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon && abs (m13 - M1.m13) <= epsilon &&
          abs (m14 - M1.m14) <= epsilon && abs (m15 - M1.m15) <= epsilon &&

          
          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m22 - M1.m22) <= epsilon && abs (m23 - M1.m23) <= epsilon &&
          abs (m24 - M1.m24) <= epsilon && abs (m25 - M1.m25) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
     * {@inheritDoc}
     */
   public boolean equals (Matrix3x6 M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m03 == M1.m03) && (m04 == M1.m04) && (m05 == M1.m05) &&

          (m10 == M1.m10) && (m11 == M1.m11) && (m12 == M1.m12) &&
          (m13 == M1.m13) && (m14 == M1.m14) && (m15 == M1.m15) &&

          (m20 == M1.m20) && (m21 == M1.m21) && (m22 == M1.m22) &&
          (m23 == M1.m23) && (m24 == M1.m24) && (m25 == M1.m25)) {

         return true;
      }
      else {
         return false;
      }
   }
}
