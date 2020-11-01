/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;

/**
 * Implements a 1 x 1 matrix. 
 */
public class Matrix1x1 extends DenseMatrixBase
   implements VectorObject<Matrix1x1> {

   public double m00;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix1x1 ZERO = new Matrix1x1();

   /**
    * Creates a new Matrix1x1.
    */
   public Matrix1x1() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int rowSize() {
      return 1;
   }

   /**
    * {@inheritDoc}
    */
   public int colSize() {
      return 1;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      if (j != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (i) {
         case 0: {
            return m00;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
      if (j != 0) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      switch (i) {
         case 0: {
            m00 = value;
            return;
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
   }

   public void set (Matrix M) {
      if (M instanceof Matrix1x1) {
         set ((Matrix1x1)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }         
         m00 = M.get (0, 0);
      }
   }

   /**
    * Sets the contents of this Matrix1x1 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix1x1 M) {
      m00 = M.m00;
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      m00 *= s;
   }

   /**
    * Computes s M and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M
    * matrix to scale
    */
   public void scale (double s, Matrix1x1 M) {
      m00 = s * M.m00;
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
      if (M instanceof Matrix1x1) {
         add ((Matrix1x1)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
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
      if (M instanceof Matrix1x1) {
         scaledAdd (s, (Matrix1x1)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
      }
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
   public void scaledAdd (double s, Matrix1x1 M1, Matrix1x1 M2) {
      m00 = s * M1.m00 + M2.m00;
   }

   /**
    * Adds the contents of a Matrix1x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix1x1 M) {
      m00 += M.m00;
   }

   /**
    * Computes M1 + M2 and places the result in this matrix.
    * 
    * @param M1
    * first matrix to add
    * @param M2
    * second matrix to add
    */
   public void add (Matrix1x1 M1, Matrix1x1 M2) {
      m00 = M1.m00 + M2.m00;
   }

   /**
    * Adds the scaled contents of a Matrix1x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix1x1 M) {
      m00 += s * M.m00;
   }

   /**
    * Sets this matrix to the negative of M.
    * 
    * @param M
    * matrix to negate
    */
   public void negate (Matrix1x1 M) {
      m00 = -M.m00;
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
      if (M instanceof Matrix1x1) {
         sub ((Matrix1x1)M);
      }
      else {
         if (M.rowSize() != 1 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
      }
   }

   /**
    * Subtracts the contents of a Matrix1x1 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix1x1 M) {
      m00 -= M.m00;
   }

    /**
    * Computes M1 - M2 places the result in this matrix.
    * 
    * @param M1
    * first matrix
    * @param M2
    * matrix to subtract
    */
   public void sub (Matrix1x1 M1, Matrix1x1 M2) {
      m00 = M1.m00 - M2.m00;
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd1x1 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd1x1 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd1x1 (this, M1, M2);
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   public void transpose (Matrix1x1 M) {
      m00 = M.m00;
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix1x1 clone() {
      try {
         return (Matrix1x1)super.clone();
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
   public void addObj (Matrix1x1 M1) {
      add (M1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Matrix1x1 M1) {
      scaledAdd (s, M1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals (Matrix1x1 M1) {
      return m00 == M1.m00;
   }

   /**
    * {@inheritDoc}
    */
   public boolean epsilonEquals (Matrix1x1 M1, double tol) {
      return (abs (m00 - M1.m00) <= tol);
   }

}
