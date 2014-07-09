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
 * Implements a 3 x 2 matrix
 */
public class Matrix3x2 extends DenseMatrixBase {
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
}
