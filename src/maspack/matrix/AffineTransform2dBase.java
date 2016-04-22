/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Base class for 3 x 3 matrices representing 2D affine transformations. A 2D
 * affine transformation applied to a 2-vector v has the form <br>
 * A v + b <br>
 * In homogeneous coordinates, this is represented by a 3 x 3 matrix of the form
 * 
 * <pre>
 *     [  A   p  ]
 * M = [         ]
 *     [  0   1  ]
 * </pre>
 */
public abstract class AffineTransform2dBase extends DenseMatrixBase {
   protected Matrix2dBase M;
   protected Vector2d b;

   /**
    * Returns the matrix assiciated with this affine transform.
    * 
    * @return matrix
    */
   public Matrix2dBase getMatrix() {
      return M;
   }

   /**
    * Returns the offset vector assiciated with this affine transform.
    * 
    * @return offset vector
    */
   public Vector2d getOffset() {
      return b;
   }
   
   /**
    * Returns the number of columns in this matrix (which is always 3).
    * 
    * @return 3
    */
   public int colSize() {
      return 3;
   }

   /**
    * Returns the number of rows in this matrix (which is always 3).
    * 
    * @return 3
    */
   public int rowSize() {
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
                  return M.m00;
               case 1:
                  return M.m01;
               case 2:
                  return b.x;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 1: {
            switch (j) {
               case 0:
                  return M.m10;
               case 1:
                  return M.m11;
               case 2:
                  return b.y;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
               case 1:
                  return 0;
               case 2:
                  return 1;
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
      values[0] = M.m00;
      values[1] = M.m01;
      values[2] = b.x;

      values[3] = M.m10;
      values[4] = M.m11;
      values[5] = b.y;

      values[6] = 0;
      values[7] = 0;
      values[8] = 1;
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
            values[0 + off] = M.m00;
            values[1 + off] = M.m10;
            values[2 + off] = 0;
            break;
         }
         case 1: {
            values[0 + off] = M.m01;
            values[1 + off] = M.m11;
            values[2 + off] = 0;
            break;
         }
         case 2: {
            values[0 + off] = b.x;
            values[1 + off] = b.y;
            values[2 + off] = 1;
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
            values[0 + off] = M.m00;
            values[1 + off] = M.m01;
            values[2 + off] = b.x;
            break;
         }
         case 1: {
            values[0 + off] = M.m10;
            values[1 + off] = M.m11;
            values[2 + off] = b.y;
            break;
         }
         case 2: {
            values[0 + off] = 0;
            values[1 + off] = 0;
            values[2 + off] = 1;
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
                  M.m00 = value;
                  return;
               case 1:
                  M.m01 = value;
                  return;
               case 2:
                  b.x = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 1: {
            switch (j) {
               case 0:
                  M.m10 = value;
                  return;
               case 1:
                  M.m11 = value;
                  return;
               case 2:
                  b.y = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            if (j < 0 || j > 2) {
               throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
            break;
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] vals) {
      M.m00 = vals[0];
      M.m01 = vals[1];
      b.x = vals[2];

      M.m10 = vals[3];
      M.m11 = vals[4];
      b.y = vals[5];
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            M.m00 = values[0];
            M.m10 = values[1];
            break;
         }
         case 1: {
            M.m01 = values[0];
            M.m11 = values[1];
            break;
         }
         case 2: {
            b.x = values[0];
            b.y = values[1];
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
            M.m00 = values[0];
            M.m01 = values[1];
            b.x = values[2];
            break;
         }
         case 1: {
            M.m10 = values[0];
            M.m11 = values[1];
            b.y = values[2];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets this affine transform to the rigid body transform described by X.
    * 
    * @param X
    * rigid body transform to copy
    */
   public void set (RigidTransform2d X) {
      M.set (X.M);
      b.set (X.b);
   }

   /**
    * Sets the transformation to the identity.
    */
   public void setIdentity() {
      M.setIdentity();
      b.setZero();
   }

   /**
    * Multiplies transformation X1 by transformation X2 and places the result in
    * this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    */
   protected void mul (AffineTransform2dBase X1, AffineTransform2dBase X2) {
      M.mul (X1.M, X2.M);
      M.mul (b, X2.b);
      b.add (X1.b);
   }

   /**
    * Multiplies transformation X1 by the inverse of transformation X2 and
    * places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    * @return false if X2 is singular
    */
   protected boolean mulInverseRight (
      AffineTransform2dBase X1, AffineTransform2dBase X2) {
      //            
      // compute M1 inv(M2) and -M1 inv(M2) b2 + b1
      //
      boolean nonSingular = M.mulInverseRight (X1.M, X2.M); // M = M1 inv(M2)
      double b1x = X1.b.x; // save b1
      double b1y = X1.b.y;
      M.mul (b, X2.b); // compute M1 inv(M2) b2
      b.x = b1x - b.x; // b = b1 - M1 inv(M2) b2
      b.y = b1y - b.y;
      return nonSingular;
   }

   /**
    * Multiplies the inverse of transformation X1 by transformation X2 and
    * places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    * @return false if X1 is singular
    */
   protected boolean mulInverseLeft (
      AffineTransform2dBase X1, AffineTransform2dBase X2) {
      //            
      // compute inv(M1) M2 and inv(M1) b2 - inv(M1) b1
      //
      Matrix2d Mtmp = new Matrix2d();
      boolean nonSingular = Mtmp.invert (X1.M); // compute inv(M1)
      b.sub (X2.b, X1.b); // compute b2 - b1
      Mtmp.mul (b); // b = inv(M1) (b2 - b1)
      M.mul (Mtmp, X2.M); // M = inv(M1) M2
      return nonSingular;
   }

   /**
    * Multiplies the inverse of transformation X1 by the inverse of
    * transformation X2 and places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    * @return false if either X1 or X2 is singular
    */
   protected boolean mulInverseBoth (
      AffineTransform2dBase X1, AffineTransform2dBase X2) {
      //            
      // compute inv(M1) inv(M2) and -inv(M1) (inv(M2) b2 - b1)
      //
      Matrix2d Mtmp = new Matrix2d();
      double b1x = X1.b.x; // save b1
      double b1y = X1.b.y;
      boolean nonSingular1 = Mtmp.invert (X1.M); // compute inv(M1)
      boolean nonSingular2 = M.invert (X2.M); // compute inv(M2)
      M.mul (b, X2.b); // compute inv(M2) b2
      b.x += b1x; // compute inv(M2) b2 + b1
      b.y += b1y;
      Mtmp.mul (b); // compute inv(M1) (inv(M2) b2 + b1)
      b.negate(); // b = -inv(M1) (inv(M2) b2 + b1)
      M.mul (Mtmp, M); // M = inv(M1) inv(M2)
      return nonSingular1 && nonSingular2;
   }

   /**
    * Multiplies the column vector v1 by this transform and places the result in
    * vr.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply
    */
   public void mul (Vector3d vr, Vector3d v1) {
      double x = M.m00 * v1.x + M.m01 * v1.y + b.x * v1.z;
      double y = M.m10 * v1.x + M.m11 * v1.y + b.y * v1.z;
      vr.x = x;
      vr.y = y;
      vr.z = v1.z;
   }

   /**
    * Multiplies the column vector vr by this transform and places the result
    * back in vr.
    * 
    * @param vr
    * vector to multiply (in place)
    */
   public void mul (Vector3d vr) {
      mul (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse of this transform and
    * places the result in vr.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply
    * @return false if this transform is singular
    */
   public boolean mulInverse (Vector3d vr, Vector3d v1) {
      Matrix2d Mtmp = new Matrix2d();
      boolean nonSingular = Mtmp.invert (M);
      double x = v1.x - b.x * v1.z;
      double y = v1.y - b.y * v1.z;

      vr.x = Mtmp.m00 * x + Mtmp.m01 * y;
      vr.y = Mtmp.m10 * x + Mtmp.m11 * y;
      vr.z = v1.z;

      return nonSingular;
   }

   /**
    * Multiplies the column vector vr by the inverse of this transform and
    * places the result back in vr.
    * 
    * @param vr
    * vector to multiply (in place)
    * @return false if this transform is singular
    */
   public boolean mulInverse (Vector3d vr) {
      return mulInverse (vr, vr);
   }

   /**
    * Inverts this transform in place.
    * 
    * @return false if this transform is singular
    */
   public boolean invert() {
      boolean nonSingular = M.invert();
      M.mul (b);
      b.negate();
      return nonSingular;
   }

   /**
    * Inverts transform X and places the result in this transform.
    * 
    * @param X
    * transform to invert
    * @return false if transform X is singular
    */
   protected boolean invert (AffineTransform2dBase X) {
      boolean nonSingular = M.invert (X.M);
      M.mul (b, X.b);
      b.negate();
      return nonSingular;
   }

   /**
    * Returns true if the elements of this transformation equal those of
    * transform <code>X1</code>within a prescribed tolerance
    * <code>epsilon</code>.
    * 
    * @param X
    * transform to compare with
    * @param epsilon
    * comparison tolerance
    * @return false if the transforms are not equal within the specified
    * tolerance
    */
   public boolean epsilonEquals (AffineTransform2dBase X, double epsilon) {
      return (M.epsilonEquals (X.M, epsilon) && b.epsilonEquals (X.b, epsilon));
   }

   /**
    * Returns true if the elements of this transformation exactly equal those of
    * transform <code>X1</code>.
    * 
    * @param X
    * transform to compare with
    * @return false if the transforms are not equal
    */
   public boolean equals (AffineTransform2dBase X) {
      return (M.equals (X.M) && b.equals (X.b));
   }
   
   /**
    * @return a deep copy of the transform
    */
   public abstract AffineTransform2dBase copy();
   
}
