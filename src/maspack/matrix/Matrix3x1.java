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
 * Implements a 3 x 1 matrix. 
 */
public class Matrix3x1 extends DenseMatrixBase {
   public double m00;
   public double m10;
   public double m20;

   /**
    * Global zero matrix. Should not be modified.
    */
   public static final Matrix3x1 ZERO = new Matrix3x1();

   /**
    * Creates a new Matrix3x1.
    */
   public Matrix3x1() {
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
         case 1: {
            return m10;
         }
         case 2: {
            return m20;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * Gets the contents of this Matrix3x1 into a Vector3d.
    * 
    * @param v
    * vector to return contents in
    */
   public void get (Vector3d v) {
      v.x = m00;
      v.y = m10;
      v.z = m20;
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
         case 1: {
            m10 = value;
            return;
         }
         case 2: {
            m20 = value;
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
      m10 = 0;
      m20 = 0;
   }

   public void set (Matrix M) {
      if (M instanceof Matrix3x1) {
         set ((Matrix3x1)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m10 = M.get (1, 0);
         m20 = M.get (2, 0);
      }
   }

   /**
    * Sets the contents of this Matrix3x1 to those of a specified block.
    * 
    * @param M
    * matrix block providing new values
    */
   public void set (Matrix3x1 M) {
      m00 = M.m00;
      m10 = M.m10;
      m20 = M.m20;
   }

   /**
    * Sets the contents of this Matrix3x1 from a Vector3d.
    * 
    * @param v
    * vector providing new values
    */
   public void set (Vector3d v) {
      m00 = v.x;
      m10 = v.y;
      m20 = v.z;
   }

   /**
    * Scales the elements of this matrix by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      m00 *= s;
      m10 *= s;
      m20 *= s;
   }

   /**
    * Scales the elements of vector v1 by <code>s</code> and places the
    * results in this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    */
   public void scale (double s, Vector3d v1) {
      m00 = s * v1.x;
      m10 = s * v1.y;
      m20 = s * v1.z;
   }

   /**
    * Computes <code>s v1</code> and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    */
   public void scaledAdd (double s, Vector3d v1) {
      m00 += s * v1.x;
      m10 += s * v1.y;
      m20 += s * v1.z;
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
      if (M instanceof Matrix3x1) {
         add ((Matrix3x1)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m10 += M.get (1, 0);
         m20 += M.get (2, 0);
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
      if (M instanceof Matrix3x1) {
         scaledAdd (s, (Matrix3x1)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s * M.get (0, 0);
         m10 += s * M.get (1, 0);
         m20 += s * M.get (2, 0);
      }
   }

   /**
    * Adds the contents of a Matrix3x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void add (Matrix3x1 M) {
      m00 += M.m00;
      m10 += M.m10;
      m20 += M.m20;
   }

   /**
    * Adds the scaled contents of a Matrix3x1 to this matrix block.
    * 
    * @param M
    * matrix block to add
    */
   public void scaledAdd (double s, Matrix3x1 M) {
      m00 += s * M.m00;
      m10 += s * M.m10;
      m20 += s * M.m20;
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
      if (M instanceof Matrix3x1) {
         sub ((Matrix3x1)M);
      }
      else {
         if (M.rowSize() != 3 || M.colSize() != 1) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m10 -= M.get (1, 0);
         m20 -= M.get (2, 0);
      }
   }

   /**
    * Subtracts the contents of a Matrix3x1 from this matrix block.
    * 
    * @param M
    * matrix block to subtract
    */
   public void sub (Matrix3x1 M) {
      m00 -= M.m00;
      m10 -= M.m10;
      m20 -= M.m20;
   }

   /**
    * Forms the dot product of this Matrix3x1 with a vector.
    * 
    * @param v
    * vector to take dot product with
    */
   public double dot (Vector3d v) {
      return m00 * v.x + m10 * v.y + m20 * v.z;
   }

   /** 
    * Computes
    * <p>
    *   s M1 * M2^T
    * <p>
    * and adds the result to matrix MR.
    * 
    * @param MR matrix to add result to
    * @param s scale value
    * @param M1 left matrix
    * @param M2 right matrix transpose
    */   
   public static void mulScaledTransposeRightAdd (
      Matrix3d MR, double s, Matrix3x1 M1, Matrix3x1 M2) {

      double M1x = s*M1.m00;
      double M1y = s*M1.m10;
      double M1z = s*M1.m20;    

      double M2x = M2.m00;
      double M2y = M2.m10;
      double M2z = M2.m20;    

      MR.m00 += M1x*M2x; MR.m01 += M1x*M2y; MR.m02 += M1x*M2z;
      MR.m10 += M1y*M2x; MR.m11 += M1y*M2y; MR.m12 += M1y*M2z;
      MR.m20 += M1z*M2x; MR.m21 += M1z*M2y; MR.m22 += M1z*M2z;      
   }

   public String transposeToString() {
      return transposeToString ("%g");
   }

   public String transposeToString (String fmtStr) {
      return transposeToString (new NumberFormat (fmtStr));
   }

   public String transposeToString (NumberFormat fmt) {
      return (fmt.format(m00) + " " + fmt.format(m10) + " " + fmt.format(m20));
   }

   /**
    * Creates a clone of this matrix.
    */
   public Matrix3x1 clone() {
      try {
         return (Matrix3x1)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for " + getClass());
      }
   }
}
