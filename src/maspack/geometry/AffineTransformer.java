package maspack.geometry;

import maspack.matrix.*;

/**
 * A GeometryTransformer that implements a linear affine transformation.
 * For points, this can be expressed in homogenous coordinates as
 *<pre> 
 * [ p' ]     [ p ]       [  F   pf  ]
 * [    ] = X [   ],  X = [          ]
 * [ 1  ]     [ 1 ]       [  0    1  ]
 *</pre>
 * where F is a general 3 X 3 matrix and pf is an offset vector.
 * 
 * @author John E Lloyd
 *
 */
public class AffineTransformer extends GeometryTransformer {

   AffineTransform3d myX;
   PolarDecomposition3d myPolarD;
   AffineTransform3d myInvX;
   boolean myInvertibleP;

   /**
    * Creates a new AffineTransformer from a specified affine transform.
    *
    * @param X affine transform defining the transformation
    */
   public AffineTransformer (AffineTransform3d X) {
      myX = new AffineTransform3d (X);
      myPolarD = new PolarDecomposition3d();
      myPolarD.factorLeft (X.A);
   }

   /**
    * Returns <code>false</code>, since this transformer does not implement a
    * linear rigid transform.
    */
   public boolean isRigid() {
      return false;
   }

   /**
    * Returns <code>true</code>, since this transformer does implement a
    * linear affine transform.
    */
   public boolean isAffine() {
      return true;
   }
   
   /**
    * Returns <code>true</code>, since this transformer is invertible.
    */
   public boolean isInvertible() {
      return true;
   }

   private void updateInverse() {
      if (myInvX == null) {
         myInvX = new AffineTransform3d (myX);
         myInvertibleP = myInvX.invert();
      }
   }

   /**
    * Returns a transformer that implements the inverse operation of this
    * transformer.
    * 
    * @return inverse transformer
    */
   public AffineTransformer getInverse() {
      updateInverse();
      return new AffineTransformer (myInvX);
   }

   /**
    * Transforms a point <code>p1</code> and returns the result in
    * <code>pr</code>. The transform is computed according to
    * <pre>
    * pr = F p1 + pf
    * </pre>
    * This method provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * 
    * @param pr transformed point
    * @param p1 point to be transformed
    */
   public void computeTransformPnt (Point3d pr, Point3d p1) {
      pr.transform (myX, p1);
   }

   /**
    * Transforms a vector <code>v1</code>, and returns the result in
    * <code>vr</code>. 
    * The transform is computed according to
    * <pre>
    * vr = F v1
    * </pre>
    * The reference position is ignored since affine transforms are position
    * invariant.
    *
    * This method provides the low level implementation for vector
    * transformations and does not do any saving or restoring of data.
    *
    * @param vr transformed vector
    * @param v1 vector to be transformed
    * @param r reference position of the vector (ignored)
    */
   public void computeTransformVec (Vector3d vr, Vector3d v1, Vector3d r) {
      vr.transform (myX, v1);
   }

   /**
    * Transforms a rigid transform <code>T1</code> and returns the result in
    * <code>TR</code>. If
    * <pre>
    *      [  R1   p1 ]
    * T1 = [          ]
    *      [  0    1  ]
    * </pre>
    * the transform is computed according to
    * <pre>
    *      [  RF R1   F p1 + pf ]
    * TR = [                    ]
    *      [    0          1    ]
    * </pre>
    * where PF RF = F is the left polar decomposition of F.
    * 
    * This method provides the low level implementation for the transformation
    * of rigid transforms and does not do any saving or restoring of data.
    *
    * @param TR transformed transform
    * @param T1 transform to be transformed
    */
   public void computeTransform (RigidTransform3d TR, RigidTransform3d T1) {
      TR.set (T1);
      TR.mulAffineLeft (myX, myPolarD.getR());
   }

   /**
    * Transforms an affine transform <code>X1</code> and returns the result in
    * <code>XR</code>. If
    * <pre>
    *      [  A1   p1 ]
    * X1 = [          ]
    *      [  0    1  ]
    * </pre>
    * the transform is computed according to
    * <pre>
    *      [  F A1   F p1 + pf ]
    * XR = [                   ]
    *      [   0         1     ]
    * </pre>
    *
    * This method provides the low level implementation for the transformation
    * of affine transforms and does not do any saving or restoring of data.
    * 
    * @param XR transformed transform
    * @param X1 transform to be transformed
    */
   public void computeTransform (AffineTransform3d XR, AffineTransform3d X1) {
      XR.mul (myX, X1);
   }
   
   /**
    * Transforms a rotation matrix <code>R1</code>, located at reference
    * position <code>ref</code>, and returns the result in <code>RR</code>.
    * The transform is computed according to
    * <pre>
    * RR = RF R1
    * </pre>
    * where PF RF = F is the left polar decomposition of F.
    * The reference position is ignored since affine transforms are position
    * invariant.
    * 
    * This method provides the low level implementation for the transformation
    * of rotation matrices and does not do any saving or restoring of data.
    *
    * @param RR transformed rotation
    * @param R1 rotation to be transformed
    * @param r reference position of the rotation (ignored)
    */
   public void computeTransform (
      RotationMatrix3d RR, RotationMatrix3d R1, Vector3d r) {

      RR.mul (myPolarD.getR(), R1);
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M1</code>, located at reference
    * position <code>ref</code>, and returns the result in <code>MR</code>.
    * The transform is computed according to
    * <pre>
    * MR = F M1
    * </pre>
    * The reference position is ignored since affine transforms are position
    * invariant.
    * 
    * This method provides the low level implementation for the transformation
    * of 3 X 3 matrices and does not do any saving or restoring of data.
    *
    * @param MR transformed matrix
    * @param M1 matrix to be transformed
    * @param r reference position of the matrix (ignored)
    */
   public void computeTransform (Matrix3d MR, Matrix3d M1, Vector3d r) {

      MR.mul (myX.A, M1);
   }

   /**
    * Transforms a plane <code>p1</code>, located at reference position
    * <code>ref</code>, and returns the result in <code>pr</code>.
    * Assume that <code>p1</code> is defined by a normal <code>n1</code>
    * and offset <code>o1</code> such that all planar points <code>x</code>
    * satisfy
    * <pre>
    * n1^T x = o1
    * </pre>
    * Then the transformed normal <code>nr</code> and offset <code>or</code>
    * are computed according to
    * <pre>
    * nr = inv(F)^T n1
    * or = o1 + nr^T pf
    * mag = ||nr||
    * nr = nr/mag, or = or/mag
    * </pre>
    * The reference position is ignored since affine transforms are position
    * invariant.
    *
    * This method provides the low level implementation for the transformation
    * of planes and does not do any saving or restoring of data.
    *
    * @param pr transformed plane
    * @param p1 plane to be transformed
    * @param r reference position of the plane (ignored)
    */
   public void computeTransform (Plane pr, Plane p1, Vector3d r) {
      pr.transform (myX, p1);
   }

}
