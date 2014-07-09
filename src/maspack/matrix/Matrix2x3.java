/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;
import maspack.util.InternalErrorException;

/**
 * Implements a 2 x 3 matrix
 */
public class Matrix2x3 extends DenseMatrixBase {
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
}
