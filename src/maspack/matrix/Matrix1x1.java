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
import maspack.util.NumberFormat;

/**
 * Implements a 1 x 1 matrix. 
 */
public class Matrix1x1 extends DenseMatrixBase {
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
    * Adds the contents of a Matrix1x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix1x1 M) {
      m00 += M.m00;
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
}
