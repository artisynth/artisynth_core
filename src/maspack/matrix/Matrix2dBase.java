/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.Clonable;
import maspack.util.InternalErrorException;

/**
 * Base class for 2 x 2 matrices in which the elements are stored as explicit
 * fields. A primary motivation for such objects is computational speed.
 */
public abstract class Matrix2dBase extends DenseMatrixBase implements Clonable {
   /**
    * Matrix element (0,0)
    */
   public double m00;

   /**
    * Matrix element (0,1)
    */
   public double m01;

   /**
    * Matrix element (1,0)
    */
   public double m10;

   /**
    * Matrix element (1,1)
    */
   public double m11;

   /**
    * Returns the number of rows in this matrix (which is always 2).
    * 
    * @return 2
    */
   public final int rowSize() {
      return 2;
   }

   /**
    * Returns the number of columns in this matrix (which is always 2).
    * 
    * @return 2
    */
   public final int colSize() {
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
      values[2] = m10;
      values[3] = m11;
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            values[0] = m00;
            values[1] = m10;
            break;
         }
         case 1: {
            values[0] = m01;
            values[1] = m11;
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
            break;
         }
         case 1: {
            values[0 + off] = m01;
            values[1 + off] = m11;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Copies a column of this matrix into a 2-vector.
    * 
    * @param j
    * column index
    * @param col
    * 2-vector into which the column is copied
    */
   public void getColumn (int j, Vector2d col) {
      switch (j) {
         case 0: {
            col.x = m00;
            col.y = m10;
            break;
         }
         case 1: {
            col.x = m01;
            col.y = m11;
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
            break;
         }
         case 1: {
            values[0] = m10;
            values[1] = m11;
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
            break;
         }
         case 1: {
            values[0 + off] = m10;
            values[1 + off] = m11;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Copies a row of this matrix into a 2-vector.
    * 
    * @param i
    * row index
    * @param row
    * 2-vector into which the row is copied
    */
   public void getRow (int i, Vector2d row) {
      switch (i) {
         case 0: {
            row.x = m00;
            row.y = m01;
            break;
         }
         case 1: {
            row.x = m10;
            row.y = m11;
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
      m10 = vals[2];
      m11 = vals[3];
   }
   
   /**
    * Sets the matrix elements
    * @param m00 top-left
    * @param m01 top-right
    * @param m10 bottom-left
    * @param m11 bottom-right
    */
   public void set(double m00, double m01, double m10, double m11) {
      this.m00 = m00;
      this.m01 = m01;
      this.m10 = m10;
      this.m11 = m11;
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            m00 = values[0];
            m10 = values[1];
            break;
         }
         case 1: {
            m01 = values[0];
            m11 = values[1];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * Sets a column of this matrix to the specified 2-vector.
    * 
    * @param j
    * column index
    * @param col
    * 2-vector from which the column is copied
    */
   public void setColumn (int j, Vector2d col) {
      switch (j) {
         case 0: {
            m00 = col.x;
            m10 = col.y;
            break;
         }
         case 1: {
            m01 = col.x;
            m11 = col.y;
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
            break;
         }
         case 1: {
            m10 = values[0];
            m11 = values[1];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets a row of this matrix to the specified 2-vector.
    * 
    * @param i
    * row index
    * @param row
    * 2-vector from which the row is copied
    */
   public void setRow (int i, Vector2d row) {
      switch (i) {
         case 0: {
            m00 = row.x;
            m01 = row.y;
            break;
         }
         case 1: {
            m10 = row.x;
            m11 = row.y;
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
      if (M instanceof Matrix2dBase) {
         set ((Matrix2dBase)M);
      }
      else {
         if (M.rowSize() != 2 || M.colSize() != 2) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
      }
   }

   /**
    * Sets the values of this matrix to those of matrix M.
    * 
    * @param M
    * matrix whose values are to be copied
    */
   public void set (Matrix2dBase M) {
      m00 = M.m00;
      m01 = M.m01;
      m10 = M.m10;
      m11 = M.m11;
   }

   /**
    * Sets the columns of this matrix to the vectors v0 and v1.
    * 
    * @param v0
    * values for the first column
    * @param v1
    * values for the second column
    */
   protected void setColumns (Vector2d v0, Vector2d v1) {
      m00 = v0.x;
      m10 = v0.y;
      m01 = v1.x;
      m11 = v1.y;
   }

   /**
    * Sets the rows of this matrix to the vectors v0 and v1.
    * 
    * @param v0
    * values for the first row
    * @param v1
    * values for the second row
    */
   protected void setRows (Vector2d v0, Vector2d v1) {
      m00 = v0.x;
      m01 = v0.y;
      m10 = v1.x;
      m11 = v1.y;
   }

   /**
    * Multiplies this matrix by M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mul (Matrix2dBase M1) {
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
   protected void mul (Matrix2dBase M1, Matrix2dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m01 * M2.m10;
      double tmp01 = M1.m00 * M2.m01 + M1.m01 * M2.m11;
      double tmp10 = M1.m10 * M2.m00 + M1.m11 * M2.m10;
      double tmp11 = M1.m10 * M2.m01 + M1.m11 * M2.m11;

      m00 = tmp00;
      m01 = tmp01;
      m10 = tmp10;
      m11 = tmp11;
   }

   /**
    * Multiplies this matrix by the transpose of M1 and places the result in
    * this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mulTranspose (Matrix2dBase M1) {
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
   protected void mulTransposeLeft (Matrix2dBase M1, Matrix2dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m10 * M2.m10;
      double tmp01 = M1.m00 * M2.m01 + M1.m10 * M2.m11;
      double tmp10 = M1.m01 * M2.m00 + M1.m11 * M2.m10;
      double tmp11 = M1.m01 * M2.m01 + M1.m11 * M2.m11;

      m00 = tmp00;
      m01 = tmp01;
      m10 = tmp10;
      m11 = tmp11;
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
   protected void mulTransposeRight (Matrix2dBase M1, Matrix2dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m01 * M2.m01;
      double tmp01 = M1.m00 * M2.m10 + M1.m01 * M2.m11;
      double tmp10 = M1.m10 * M2.m00 + M1.m11 * M2.m01;
      double tmp11 = M1.m10 * M2.m10 + M1.m11 * M2.m11;

      m00 = tmp00;
      m01 = tmp01;
      m10 = tmp10;
      m11 = tmp11;
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
   protected void mulTransposeBoth (Matrix2dBase M1, Matrix2dBase M2) {
      double tmp00 = M1.m00 * M2.m00 + M1.m10 * M2.m01;
      double tmp01 = M1.m00 * M2.m10 + M1.m10 * M2.m11;
      double tmp10 = M1.m01 * M2.m00 + M1.m11 * M2.m01;
      double tmp11 = M1.m01 * M2.m10 + M1.m11 * M2.m11;

      m00 = tmp00;
      m01 = tmp01;
      m10 = tmp10;
      m11 = tmp11;
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
   protected boolean mulInverseRight (Matrix2dBase M1, Matrix2dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix2d Tmp = new Matrix2d();
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
   protected boolean mulInverseLeft (Matrix2dBase M1, Matrix2dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix2d Tmp = new Matrix2d();
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
   protected boolean mulInverseBoth (Matrix2dBase M1, Matrix2dBase M2) {
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
   public void mul (Vector2d vr, Vector2d v1) {
      double x = m00 * v1.x + m01 * v1.y;
      double y = m10 * v1.x + m11 * v1.y;

      vr.x = x;
      vr.y = y;
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
      if (v1.size() < 2) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size 2");
      }
      if (vr.size() < 2) {
         vr.setSize(2);
      }
      double[] res = vr.getBuffer();
      double[] buf = v1.getBuffer();

      double b0 = buf[0];
      double b1 = buf[1];

      res[0] = m00*b0 + m01*b1;
      res[1] = m10*b0 + m11*b1;
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
   public void mulAdd (Vector2d vr, Vector2d v1, Vector2d v2) {
      double x = m00 * v1.x + m01 * v1.y;
      double y = m10 * v1.x + m11 * v1.y;

      vr.x = x + v2.x;
      vr.y = y + v2.y;
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
   public void mul (Vector2d vr) {
      double x = m00 * vr.x + m01 * vr.y;
      double y = m10 * vr.x + m11 * vr.y;

      vr.x = x;
      vr.y = y;
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
   public void mulTranspose (Vector2d vr, Vector2d v1) {
      double x = m00 * v1.x + m10 * v1.y;
      double y = m01 * v1.x + m11 * v1.y;

      vr.x = x;
      vr.y = y;
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
   public void mulTranspose (Vector2d vr) {
      double x = m00 * vr.x + m10 * vr.y;
      double y = m01 * vr.x + m11 * vr.y;

      vr.x = x;
      vr.y = y;
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
   public boolean mulInverse (Vector2d vr, Vector2d v1) {
      Matrix2d Tmp = new Matrix2d();
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
   public boolean mulInverse (Vector2d vr) {
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
   public boolean mulInverseTranspose (Vector2d vr, Vector2d v1) {
      Matrix2d Tmp = new Matrix2d();
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
   public boolean mulInverseTranspose (Vector2d vr) {
      return mulInverseTranspose (vr, vr);
   }

   /**
    * Multiplies M1 by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulAdd (Matrix M1, Matrix M2) {
      MatrixMulAdd.mulAdd2x2 (this, M1, M2);
   }

   /**
    * Multiplies M1 by M2^T and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeRightAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeRightAdd.mulTransposeRightAdd2x2 (this, M1, M2);
   }

   /**
    * Multiplies M1^T by M2 and places the result in this matrix.
    *
    * @param M1 left matrix term
    * @param M2 right matrix term
    */
   public void mulTransposeLeftAdd (Matrix M1, Matrix M2) {
      MatrixMulTransposeLeftAdd.mulTransposeLeftAdd2x2 (this, M1, M2);
   }

   /**
    * Adds matrix M1 to M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void add (Matrix2dBase M1, Matrix2dBase M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
   }

   /**
    * Adds this matrix to M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void add (Matrix2dBase M1) {
      m00 += M1.m00;
      m01 += M1.m01;
      m10 += M1.m10;
      m11 += M1.m11;
   }

   /**
    * Subtracts matrix M1 from M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void sub (Matrix2dBase M1, Matrix2dBase M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void sub (Matrix2dBase M1) {
      m00 -= M1.m00;
      m01 -= M1.m01;
      m10 -= M1.m10;
      m11 -= M1.m11;
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
   protected void scale (double s, Matrix2dBase M1) {
      m00 = s * M1.m00;
      m01 = s * M1.m01;
      m10 = s * M1.m10;
      m11 = s * M1.m11;
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
   protected void scaledAdd (double s, Matrix2dBase M1, Matrix2dBase M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
   }

   /**
    * Computes s M1 and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled and added
    */
   protected void scaledAdd (double s, Matrix2dBase M1) {
      m00 += s * M1.m00;
      m01 += s * M1.m01;
      m10 += s * M1.m10;
      m11 += s * M1.m11;
   }

   /**
    * Sets this matrix to the negative of M1.
    * 
    * @param M1
    * matrix to negate
    */
   protected void negate (Matrix2dBase M1) {
      m00 = -M1.m00;
      m01 = -M1.m01;
      m10 = -M1.m10;
      m11 = -M1.m11;
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
      m01 = m10;
      m10 = tmp01;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   protected void transpose (Matrix2dBase M) {
      double tmp01 = M.m01;

      m00 = M.m00;
      m11 = M.m11;
      m01 = M.m10;
      m10 = tmp01;
   }

   /**
    * Sets this matrix to the identity.
    */
   public void setIdentity() {
      m00 = 1.0;
      m01 = 0.0;
      m10 = 0.0;
      m11 = 1.0;
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   protected void setZero() {
      m00 = 0.0;
      m01 = 0.0;
      m10 = 0.0;
      m11 = 0.0;
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
   public boolean epsilonEquals (Matrix2dBase M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon) {
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
   public boolean equals (Matrix2dBase M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) &&
          (m10 == M1.m10) && (m11 == M1.m11)) {
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
      max = abs (m00) + abs (m01);
      sum = abs (m10) + abs (m11);
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
      max = abs (m00) + abs (m10);
      sum = abs (m01) + abs (m11);
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
      double sum = (m00 * m00 + m01 * m01 + m10 * m10 + m11 * m11);
      return Math.sqrt (sum);
   }

   /**
    * Inverts this matrix in place, returning false if the matrix is detected to
    * be singular.
    */
   public boolean invert() {
      return invert (this);
   }

   /**
    * Inverts the matrix M and places the result in this matrix, return false if
    * M is detected to be singular.
    * 
    * @param M1
    * matrix to invert
    * @return false if M is singular
    */
   protected boolean invert (Matrix2dBase M1) {
      double x00, x01, x10, x11;

      double abs00 = (M1.m00 >= 0 ? M1.m00 : -M1.m00);
      double abs10 = (M1.m10 >= 0 ? M1.m10 : -M1.m10);
      double dinv;
      if (abs00 >= abs10) {
         dinv = 1 / (M1.m00 * (M1.m11 - (M1.m10 * M1.m01) / M1.m00));
      }
      else {
         dinv = 1 / (M1.m10 * ((M1.m11 * M1.m00) / M1.m10 - M1.m01));
      }
      x00 = M1.m11 * dinv;
      x01 = -M1.m01 * dinv;
      x10 = -M1.m10 * dinv;
      x11 = M1.m00 * dinv;

      m00 = x00;
      m01 = x01;
      m10 = x10;
      m11 = x11;

      return dinv != 0;
   }

   /**
    * Returns the determinant of this matrix
    * 
    * @return matrix determinant
    */
   public double determinant() throws ImproperSizeException {
      return (m00 * m11 - m10 * m01);
   }

   public Matrix2dBase clone() {
      try {
         return (Matrix2dBase)(super.clone ());
      } catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for Matrix2dBase");
      }
   }

}
