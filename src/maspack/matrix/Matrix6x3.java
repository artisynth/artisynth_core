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
public class Matrix6x3 extends DenseMatrixBase
   implements VectorObject<Matrix6x3> {

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

   public double m40;
   public double m41;
   public double m42;

   public double m50;
   public double m51;
   public double m52;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix6x3 ZERO = new Matrix6x3();

   /**
    * Creates a new Matrix6x3.
    */
   public Matrix6x3() {
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
         case 4: {
            switch (j) {
               case 0:
                  return m40;
               case 1:
                  return m41;
               case 2:
                  return m42;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 5: {
            switch (j) {
               case 0:
                  return m50;
               case 1:
                  return m51;
               case 2:
                  return m52;
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
         case 4: {
            values[0 + off] = m40;
            values[1 + off] = m41;
            values[2 + off] = m42;
            break;
         }
         case 5: {
            values[0 + off] = m50;
            values[1 + off] = m51;
            values[2 + off] = m52;
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
         case 4: {
            row.x = m40;
            row.y = m41;
            row.z = m42;
            break;
         }
         case 5: {
            row.x = m50;
            row.y = m51;
            row.z = m52;
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
         case 2: {
            values[0 + off] = m02;
            values[1 + off] = m12;
            values[2 + off] = m22;
            values[3 + off] = m32;
            values[4 + off] = m42;
            values[5 + off] = m52;
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
         case 4: {
            switch (j) {
               case 0:
                  m40 = value;
                  return;
               case 1:
                  m41 = value;
                  return;
               case 2:
                  m42 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 5: {
            switch (j) {
               case 0:
                  m50 = value;
                  return;
               case 1:
                  m51 = value;
                  return;
               case 2:
                  m52 = value;
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
         case 2: {
            m02 = values[0];
            m12 = values[1];
            m22 = values[2];
            m32 = values[3];
            m42 = values[4];
            m52 = values[5];
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
         case 4: {
            m40 = values[0];
            m41 = values[1];
            m42 = values[2];
            break;
         }
         case 5: {
            m50 = values[0];
            m51 = values[1];
            m52 = values[2];
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
         case 4: {
            m40 = row.x;
            m41 = row.y;
            m42 = row.z;
            break;
         }
         case 5: {
            m50 = row.x;
            m51 = row.y;
            m52 = row.z;
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

      m40 = 0;
      m41 = 0;
      m42 = 0;

      m50 = 0;
      m51 = 0;
      m52 = 0;
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M instanceof Matrix6x3) {
         set ((Matrix6x3)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 3) {
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

         m40 = M.get (4, 0);
         m41 = M.get (4, 1);
         m42 = M.get (4, 2);

         m50 = M.get (5, 0);
         m51 = M.get (5, 1);
         m52 = M.get (5, 2);
      }
   }

   /**
    * Sets the contents of this Matrix6x3 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix6x3 M) {
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

      m40 = M.m40;
      m41 = M.m41;
      m42 = M.m42;

      m50 = M.m50;
      m51 = M.m51;
      m52 = M.m52;
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

      m40 *= s;
      m41 *= s;
      m42 *= s;

      m50 *= s;
      m51 *= s;
      m52 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix6x3 M) {
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

      m40 = s * M.m40;
      m41 = s * M.m41;
      m42 = s * M.m42;

      m50 = s * M.m50;
      m51 = s * M.m51;
      m52 = s * M.m52;
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
      if (M instanceof Matrix6x3) {
         add ((Matrix6x3)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 3) {
            throw new ImproperSizeException (
               "matrix sizes do not conform; M is "+M.getSize());
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

         m40 += M.get (4, 0);
         m41 += M.get (4, 1);
         m42 += M.get (4, 2);

         m50 += M.get (5, 0);
         m51 += M.get (5, 1);
         m52 += M.get (5, 2);
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
      if (M instanceof Matrix6x3) {
         scaledAdd (s, (Matrix6x3)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 3) {
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

         m40 += s * M.get (4, 0);
         m41 += s * M.get (4, 1);
         m42 += s * M.get (4, 2);

         m50 += s * M.get (5, 0);
         m51 += s * M.get (5, 1);
         m52 += s * M.get (5, 2);
      }
   }

   /**
    * Adds the contents of a Matrix6x3 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix6x3 M) {
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

      m40 += M.m40;
      m41 += M.m41;
      m42 += M.m42;

      m50 += M.m50;
      m51 += M.m51;
      m52 += M.m52;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix6x3 M1, Matrix6x3 M2) {
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

      m40 = M1.m40 + M2.m40;
      m41 = M1.m41 + M2.m41;
      m42 = M1.m42 + M2.m42;

      m50 = M1.m50 + M2.m50;
      m51 = M1.m51 + M2.m51;
      m52 = M1.m52 + M2.m52;
   }

   /**
    * Computes s M and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix6x3 M) {
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

      m40 += s * M.m40;
      m41 += s * M.m41;
      m42 += s * M.m42;

      m50 += s * M.m50;
      m51 += s * M.m51;
      m52 += s * M.m52;
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
   public void scaledAdd (double s, Matrix6x3 M1, Matrix6x3 M2) {
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

      m40 = s * M1.m40 + M2.m40;
      m41 = s * M1.m41 + M2.m41;
      m42 = s * M1.m42 + M2.m42;

      m50 = s * M1.m50 + M2.m50;
      m51 = s * M1.m51 + M2.m51;
      m52 = s * M1.m52 + M2.m52;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix6x3 M) {
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

      m40 = -M.m40;
      m41 = -M.m41;
      m42 = -M.m42;

      m50 = -M.m50;
      m51 = -M.m51;
      m52 = -M.m52;
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
      if (M instanceof Matrix6x3) {
         sub ((Matrix6x3)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 3) {
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

         m40 -= M.get (4, 0);
         m41 -= M.get (4, 1);
         m42 -= M.get (4, 2);

         m50 -= M.get (5, 0);
         m51 -= M.get (5, 1);
         m52 -= M.get (5, 2);
      }
   }

   /**
    * Subtracts the contents of a Matrix6x3 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix6x3 M) {
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

      m40 -= M.m40;
      m41 -= M.m41;
      m42 -= M.m42;

      m50 -= M.m50;
      m51 -= M.m51;
      m52 -= M.m52;
   }

   /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix6x3 M1, Matrix6x3 M2) {
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

      m40 = M1.m40 - M2.m40;
      m41 = M1.m41 - M2.m41;
      m42 = M1.m42 - M2.m42;

      m50 = M1.m50 - M2.m50;
      m51 = M1.m51 - M2.m51;
      m52 = M1.m52 - M2.m52;
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
    * Gets the 3x3 sub-matrix of this matrix starting at (3, 0).
    * 
    * @param M returns the sub matrix
    */
   public void getSubMatrix30 (Matrix3dBase M) {
      M.m00 = m30; M.m01 = m31; M.m02 = m32; 
      M.m10 = m40; M.m11 = m41; M.m12 = m42; 
      M.m20 = m50; M.m21 = m51; M.m22 = m52; 
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
    * Sets the 3x3 sub-matrix of this matrix starting at (3, 0).
    *
    * @param M new sub matrix value
    */
   public void setSubMatrix30 (Matrix3dBase M) {
      m30 = M.m00; m31 = M.m01; m32 = M.m02; 
      m40 = M.m10; m41 = M.m11; m42 = M.m12; 
      m50 = M.m20; m51 = M.m21; m52 = M.m22; 
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
    * Adds to the 3x3 sub-matrix of this matrix starting at (3, 0).
    *
    * @param M sub matrix to add
    */
   public void addSubMatrix30 (Matrix3dBase M) {
      m30 += M.m00; m31 += M.m01; m32 += M.m02; 
      m40 += M.m10; m41 += M.m11; m42 += M.m12; 
      m50 += M.m20; m51 += M.m21; m52 += M.m22; 
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd6x3 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd6x3 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd6x3 (this, M1, M2);
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix3x6 M) {
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
      m40 = M.m04;
      m41 = M.m14;
      m42 = M.m24;
      m50 = M.m05;
      m51 = M.m15;
      m52 = M.m25;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix6x3 clone() {
      try {
         return (Matrix6x3)super.clone();
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
   public void addObj (Matrix6x3 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix6x3 M1) {
      scaledAdd (s, M1);
   }

   /**
     * {@inheritDoc}
     */
   public boolean epsilonEquals (Matrix6x3 M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && 

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon &&
          
          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m22 - M1.m22) <= epsilon && 
          
          abs (m30 - M1.m30) <= epsilon && abs (m31 - M1.m31) <= epsilon &&
          abs (m32 - M1.m32) <= epsilon && 

          abs (m40 - M1.m40) <= epsilon && abs (m41 - M1.m41) <= epsilon &&
          abs (m42 - M1.m42) <= epsilon && 
          
          abs (m50 - M1.m50) <= epsilon && abs (m51 - M1.m51) <= epsilon &&
          abs (m52 - M1.m52) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
     * {@inheritDoc}
     */
   public boolean equals (Matrix6x3 M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m10 == M1.m10) && (m11 == M1.m11) && (m12 == M1.m12) &&
          (m20 == M1.m20) && (m21 == M1.m21) && (m22 == M1.m22) &&
          (m30 == M1.m30) && (m31 == M1.m31) && (m32 == M1.m32) &&
          (m40 == M1.m40) && (m41 == M1.m41) && (m42 == M1.m42) &&
          (m50 == M1.m50) && (m51 == M1.m51) && (m52 == M1.m52)) {
         return true;
      }
      else {
         return false;
      }
   }
}
