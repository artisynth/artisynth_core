/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Base class for 4 x 4 matrices representing 3D affine transformations. A 3D
 * affine transformation applied to a 3-vector v has the form <br>
 * A v + b <br>
 * In homogeneous coordinates, this is represented by a 4 x 4 matrix of the form
 * 
 * <pre>
 *     [  A   p  ]
 * M = [         ]
 *     [  0   1  ]
 * </pre>
 */
public abstract class AffineTransform3dBase extends DenseMatrixBase
   implements VectorTransformer3d , java.io.Serializable {
   private static final long serialVersionUID = 1L;
   protected Matrix3dBase M;
   protected Vector3d b;

   /**
    * Returns the matrix assiciated with this affine transform.
    * 
    * @return matrix
    */
   public Matrix3dBase getMatrix() {
      return M;
   }

   /**
    * Returns the offset vector assiciated with this affine transform.
    * 
    * @return offset vector
    */
   public Vector3d getOffset() {
      return b;
   }

   /**
    * Returns the number of columns in this matrix (which is always 4).
    * 
    * @return 4
    */
   public int colSize() {
      return 4;
   }

   /**
    * Returns the number of rows in this matrix (which is always 4).
    * 
    * @return 4
    */
   public int rowSize() {
      return 4;
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
                  return M.m02;
               case 3:
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
                  return M.m12;
               case 3:
                  return b.y;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
                  return M.m20;
               case 1:
                  return M.m21;
               case 2:
                  return M.m22;
               case 3:
                  return b.z;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            switch (j) {
               case 0:
               case 1:
               case 2:
                  return 0;
               case 3:
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
      values[2] = M.m02;
      values[3] = b.x;

      values[4] = M.m10;
      values[5] = M.m11;
      values[6] = M.m12;
      values[7] = b.y;

      values[8] = M.m20;
      values[9] = M.m21;
      values[10] = M.m22;
      values[11] = b.z;

      values[12] = 0;
      values[13] = 0;
      values[14] = 0;
      values[15] = 1;
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
            values[2 + off] = M.m20;
            values[3 + off] = 0;
            break;
         }
         case 1: {
            values[0 + off] = M.m01;
            values[1 + off] = M.m11;
            values[2 + off] = M.m21;
            values[3 + off] = 0;
            break;
         }
         case 2: {
            values[0 + off] = M.m02;
            values[1 + off] = M.m12;
            values[2 + off] = M.m22;
            values[3 + off] = 0;
            break;
         }
         case 3: {
            values[0 + off] = b.x;
            values[1 + off] = b.y;
            values[2 + off] = b.z;
            values[3 + off] = 1;
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
            values[2 + off] = M.m02;
            values[3 + off] = b.x;
            break;
         }
         case 1: {
            values[0 + off] = M.m10;
            values[1 + off] = M.m11;
            values[2 + off] = M.m12;
            values[3 + off] = b.y;
            break;
         }
         case 2: {
            values[0 + off] = M.m20;
            values[1 + off] = M.m21;
            values[2 + off] = M.m22;
            values[3 + off] = b.z;
            break;
         }
         case 3: {
            values[0 + off] = 0;
            values[1 + off] = 0;
            values[2 + off] = 0;
            values[3 + off] = 1;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   public void set (AffineTransform3dBase A) {
      M.set (A.M);
      b.set (A.b);
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
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
                  M.m02 = value;
                  return;
               case 3:
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
                  M.m12 = value;
                  return;
               case 3:
                  b.y = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
                  M.m20 = value;
                  return;
               case 1:
                  M.m21 = value;
                  return;
               case 2:
                  M.m22 = value;
                  return;
               case 3:
                  b.z = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            if (j < 0 || j > 3) {
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
      M.m02 = vals[2];
      b.x = vals[3];

      M.m10 = vals[4];
      M.m11 = vals[5];
      M.m12 = vals[6];
      b.y = vals[7];

      M.m20 = vals[8];
      M.m21 = vals[9];
      M.m22 = vals[10];
      b.z = vals[11];
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            M.m00 = values[0];
            M.m10 = values[1];
            M.m20 = values[2];
            break;
         }
         case 1: {
            M.m01 = values[0];
            M.m11 = values[1];
            M.m21 = values[2];
            break;
         }
         case 2: {
            M.m02 = values[0];
            M.m12 = values[1];
            M.m22 = values[2];
            break;
         }
         case 3: {
            b.x = values[0];
            b.y = values[1];
            b.z = values[2];
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
            M.m02 = values[2];
            b.x = values[3];
            break;
         }
         case 1: {
            M.m10 = values[0];
            M.m11 = values[1];
            M.m12 = values[2];
            b.y = values[3];
            break;
         }
         case 2: {
            M.m20 = values[0];
            M.m21 = values[1];
            M.m22 = values[2];
            b.z = values[3];
            break;
         }
         case 3: { // ignore
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * Sets the translation component of this affine transform.
    * 
    * @param p
    * translation vector
    */
   public void setTranslation (Vector3d p) {
      b.set (p);
   }
   
   /**
    * Sets the translation component of this affine transform.
    * 
    * @param p
    * translation vector
    */
   public void addTranslation (Vector3d p) {
      b.add (p);
   }
   
   /**
    * Sets the translation component of this affine transform.
    * 
    * @param tx x-component of translation
    * @param ty y-component of translation
    * @param tz z-component of translation
    * 
    */
   public void setTranslation (double tx, double ty, double tz) {
      b.set (tx, ty, tz);
   }
   
   /**
    * Sets the translation component of this affine transform.
    * 
    * @param tx x-component of translation
    * @param ty y-component of translation
    * @param tz z-component of translation
    * 
    */
   public void addTranslation (double tx, double ty, double tz) {
      b.add (tx, ty, tz);
   }

   /**
    * Sets the matrix component of this affine transform to an explicit
    * rotation.
    * 
    * @param axisAng
    * axis-angle describing the rotation
    */
   public void setRotation (AxisAngle axisAng) {
      M.set (new RotationMatrix3d (axisAng));
   }
   
   /**
    * Sets the matrix component of this affine transform to an explicit
    * rotation.
    * 
    * @param quat
    * quaternion describing the rotation
    */
   public void setRotation (Quaternion quat) {
      M.set (new RotationMatrix3d (quat));
   }

   /**
    * Sets the matrix component of this affine transform to an explicit
    * rotation.
    * 
    * @param R
    * rotation matrix
    */
   public void setRotation (RotationMatrix3d R) {
      M.set (R);
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
   protected void mul (AffineTransform3dBase X1, AffineTransform3dBase X2) {
      double b1x = X1.b.x; // save b1
      double b1y = X1.b.y;
      double b1z = X1.b.z;
      X1.M.mul (b, X2.b);
      M.mul (X1.M, X2.M);
      b.x += b1x;
      b.y += b1y;
      b.z += b1z;
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
      AffineTransform3dBase X1, AffineTransform3dBase X2) {
      //            
      // compute M1 inv(M2) and -M1 inv(M2) b2 + b1
      //
      boolean nonSingular = M.mulInverseRight (X1.M, X2.M); // M = M1 inv(M2)
      double b1x = X1.b.x; // save b1
      double b1y = X1.b.y;
      double b1z = X1.b.z;
      M.mul (b, X2.b); // compute M1 inv(M2) b2
      b.x = b1x - b.x; // b = b1 - M1 inv(M2) b2
      b.y = b1y - b.y;
      b.z = b1z - b.z;
      return nonSingular;
   }

   /**
    * Post-multiplies this transformation by the inverse of the rigid 
    * transformation T and places the result in this transformation.
    * 
    * @param T
    * right-hand rigid transformation
    */
   public void mulInverse (RigidTransform3d T) {
      mulInverseRight (this, T);
   }

   /**
    * Multiplies transformation X1 by the inverse of the rigid transformation 
    * T2 and places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param T2
    * right-hand rigid transformation
    */
   public void mulInverseRight (
      AffineTransform3dBase X1, RigidTransform3d T2) {
      //            
      // compute M1 inv(M2) and -M1 inv(M2) b2 + b1
      //
      M.mulTransposeRight (X1.M, T2.R); // M = M1 R2^T
      double b1x = X1.b.x; // save b1
      double b1y = X1.b.y;
      double b1z = X1.b.z;
      M.mul (b, T2.p);     // compute M1 R2^T b2
      b.x = b1x - b.x;     // b = b1 - M1 R2^T b2
      b.y = b1y - b.y;
      b.z = b1z - b.z;
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
      AffineTransform3dBase X1, AffineTransform3dBase X2) {
      //            
      // compute inv(M1) M2 and inv(M1) b2 - inv(M1) b1
      //
      Matrix3d Mtmp = new Matrix3d();
      boolean nonSingular = Mtmp.invert (X1.M); // compute inv(M1)
      b.sub (X2.b, X1.b); // compute b2 - b1
      Mtmp.mul (b); // b = inv(M1) (b2 - b1)
      M.mul (Mtmp, X2.M); // M = inv(M1) M2
      return nonSingular;
   }

   /**
    * Multiplies the inverse of the rigid transformation T1 by the 
    * transformation X2 and places the result in this transformation.
    * 
    * @param T1
    * left-hand rigid transformation
    * @param X2
    * right-hand transformation
    */
   public void mulInverseLeft (RigidTransform3d T1, AffineTransform3dBase X2) {

      b.sub (X2.b, T1.p);               // compute b2 - b1
      T1.R.mulTranspose (b, b);         // b = R1^T (b2 - b1)
      M.mulTransposeLeft (T1.R, X2.M);  // M = R1^T M2
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
      AffineTransform3dBase X1, AffineTransform3dBase X2) {
      //            
      // compute inv(M1) inv(M2) and -inv(M1) (inv(M2) b2 - b1)
      //
      Matrix3d Mtmp = new Matrix3d();
      double b1x = X1.b.x; // save b1
      double b1y = X1.b.y;
      double b1z = X1.b.z;
      boolean nonSingular1 = Mtmp.invert (X1.M); // compute inv(M1)
      boolean nonSingular2 = M.invert (X2.M); // compute inv(M2)
      M.mul (b, X2.b); // compute inv(M2) b2
      b.x += b1x; // compute inv(M2) b2 + b1
      b.y += b1y;
      b.z += b1z;
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
   public void mul (Vector4d vr, Vector4d v1) {
      double x = M.m00 * v1.x + M.m01 * v1.y + M.m02 * v1.z + b.x * v1.w;
      double y = M.m10 * v1.x + M.m11 * v1.y + M.m12 * v1.z + b.y * v1.w;
      double z = M.m20 * v1.x + M.m21 * v1.y + M.m22 * v1.z + b.z * v1.w;
      vr.x = x;
      vr.y = y;
      vr.z = z;
      vr.w = v1.w;
   }

   /**
    * Multiplies the column vector vr by this transform and places the result
    * back in vr.
    * 
    * @param vr
    * vector to multiply (in place)
    */
   public void mul (Vector4d vr) {
      mul (vr, vr);
   }
   
   
   public void mul(RigidTransform3d X2) {
      mul(this, X2);
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
   public boolean mulInverse (Vector4d vr, Vector4d v1) {
      Matrix3d Mtmp = new Matrix3d();
      boolean nonSingular = Mtmp.invert (M);
      double x = v1.x - b.x * v1.w;
      double y = v1.y - b.y * v1.w;
      double z = v1.z - b.z * v1.w;

      vr.x = Mtmp.m00 * x + Mtmp.m01 * y + Mtmp.m02 * z;
      vr.y = Mtmp.m10 * x + Mtmp.m11 * y + Mtmp.m12 * z;
      vr.z = Mtmp.m20 * x + Mtmp.m21 * y + Mtmp.m22 * z;
      vr.w = v1.w;

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
   public boolean mulInverse (Vector4d vr) {
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
   protected boolean invert (AffineTransform3dBase X) {
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
   public boolean epsilonEquals (AffineTransform3dBase X, double epsilon) {
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
   public boolean equals (AffineTransform3dBase X) {
      return (M.equals (X.M) && b.equals (X.b));
   }

   /**
    * Returns the shear, scaling, and rotational components of this
    * AffineTransform. This is done by computing the SVD of the transform:
    * 
    * <pre>
    *     M = U S V'
    * </pre>
    * 
    * The rotation is then set to U, the scale factors are set to the diagonal
    * elements of S, and the shear is set to U'.
    * 
    * @param R
    * returns the rotation matrix
    * @param s
    * returns the three scaling factors
    * @param X
    * returns the shearing transform
    */
   public void getMatrixComponents (RotationMatrix3d R, Vector3d s, Matrix3d X) {
      Matrix3d Mtmp = new Matrix3d();
      SVDecomposition3d svd = new SVDecomposition3d (Mtmp, X, M);
      svd.getS (s);
      R.set (Mtmp);
      X.transpose();
   }

//   /**
//    * Factors this affine transform into the product of a stretch-shear
//    * transform (with no translation) and a rigid transform:
//    * 
//    * <pre>
//    *    this = XS XR
//    * </pre>
//    * 
//    * @param XS
//    * returns the stretch-shear transform (optional argument)
//    * @param XR
//    * returns the rigid transform (optional argument)
//    */
//   public void leftRigidFactor (AffineTransform3d XS, RigidTransform3d XR) {
//      SVDecomposition3d SVD = new SVDecomposition3d();
//      SVD.factor (M);
//      Matrix3d U = SVD.getU();
//      Matrix3d V = SVD.getV();
//      Vector3d sig = new Vector3d();
//      SVD.getS (sig);
//
//      if (sig.z / sig.x < 1e-16) {
//         throw new IllegalArgumentException (
//            "Transform is singular to working precision");
//      }
//
//      double detU = U.orthogonalDeterminant();
//      double detV = V.orthogonalDeterminant();
//      if (detV * detU < 0) { /* then one is negative and the other positive */
//         if (detV < 0) { /* negative last column of V */
//            V.m02 = -V.m02;
//            V.m12 = -V.m12;
//            V.m22 = -V.m22;
//            sig.z = -sig.z;
//         }
//         else /* detU < 0 */
//         { /* negative last column of U */
//            U.m02 = -U.m02;
//            U.m12 = -U.m12;
//            U.m22 = -U.m22;
//            sig.z = -sig.z;
//         }
//      }
//      // Now set S = U diag(sig) U'
//      Matrix3d S = (XS == null ? new Matrix3d() : XS.A);
//      S.set (U);
//      S.mulDiagonalRight (sig);
//      S.mulTransposeRight (S, U);
//
//      if (XR != null) { // set R = U * V'
//         V.mulTransposeRight (U, V);
//         XR.R.set (V);
//
//         // set p = inv(S) b = U diag(1/sig) U' b
//         Vector3d p = XR.p;
//         p.mulTranspose (U, b);
//         p.x /= sig.x;
//         p.y /= sig.y;
//         p.z /= sig.z;
//         p.mul (U, p);
//      }
//
//      if (XS != null) {
//         XS.b.setZero();
//      }
//
//   }

   /**
    * {@inheritDoc}
    */
   public void transformPnt (Vector3d pr, Vector3d p0) {
      M.mul (pr, p0);
      pr.add (b);
   }

   /**
    * {@inheritDoc}
    */
   public void transformVec (Vector3d vr, Vector3d v0) {
      M.mul (vr, v0);
   }

   /**
    * {@inheritDoc}
    */
   public void transformCovec (Vector3d nr, Vector3d n0) {
      M.mulInverseTranspose (nr, n0);
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformPnt (Vector3d pr, Vector3d p0) {
      pr.sub (p0, b);
      M.mulInverse (pr);
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformVec (Vector3d vr, Vector3d v0) {
      M.mulInverse (vr, v0);
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformCovec (Vector3d nr, Vector3d n0) {
      M.mulTranspose (nr, n0);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isAffine() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isRigid() {
      return false;
   }
   
   /**
    * Returns true if this transform equals the identity.
    * 
    * @return true if this transform equals the identity
    */
   public boolean isIdentity() {
      return (M.isIdentity() && b.equals (Vector3d.ZERO));
   }

   public abstract void setRandom();

   /**
    * @return deep copy of transform
    */
   public abstract AffineTransform3dBase copy();
   
   public AffineTransform3dBase clone() throws CloneNotSupportedException {
      throw new CloneNotSupportedException("Use copy, clone not supported");
   }
}
