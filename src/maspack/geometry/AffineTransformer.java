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
   boolean myReflectingP = false;
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
      myReflectingP = myX.A.determinant() < 0;
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
    * Returns <code>true</code> if this transformer is reflecting.
    */
   public boolean isReflecting() {
      return myReflectingP;
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
    * Transforms a point <code>p</code> and returns the result in
    * <code>pr</code>. The transform is computed according to
    * <pre>
    * pr = F p + pf
    * </pre>
    * This method provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * 
    * @param pr transformed point
    * @param p point to be transformed
    */
   public void computeTransformPnt (Point3d pr, Point3d p) {
      pr.transform (myX, p);
   }

   /**
    * Transforms a vector <code>v</code>, and returns the result in
    * <code>vr</code>. 
    * The reference position <code>r</code> is ignored since affine transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    * vr = F v
    * </pre>
    *
    * This method provides the low level implementation for vector
    * transformations and does not do any saving or restoring of data.
    *
    * @param vr transformed vector
    * @param v vector to be transformed
    * @param r reference position of the vector (ignored)
    */
   public void computeTransformVec (Vector3d vr, Vector3d v, Vector3d r) {
      vr.transform (myX, v);
   }
   
   /**
    * Computes the matrices <code>PL</code> and <code>N</code> that transform
    * points <code>xl</code> local to a coordinate frame <code>T</code> after
    * that frame is itself transformed.  The updated local coordinates are
    * given by
    * <pre>
    * xl' = N PL xl
    * </pre>
    * where <code>PL</code> is symmetric positive definite and
    * <code>N</code> is a diagonal matrix that is either the identity,
    * or a reflection that flips a single axis. See the documentation
    * for {@link GeometryTransformer#computeLocalTransforms}.
    * The quantities <code>F</code> and <code>f(p)</code>
    * described there correspond to <code>F</code> and
    * <code>pf</code> for this transformer.
    *
    * @param PL primary transformation matrix
    * @param Ndiag if non-null, returns the diagonal components of N
    * @param T rigid transform for which the local transforms are computed
    */
   public void computeLocalTransforms (
      Matrix3d PL, Vector3d Ndiag, RigidTransform3d T) {
      
      if (Ndiag != null) {
         myPolarD.getN (Ndiag);
      }
      PL.mulTransposeLeft (myPolarD.getQ(), myX.A);
      PL.inverseTransform (T.R);
   }

   /**
    * Transforms a normal vector <code>n</code>, and returns the result in
    * <code>nr</code>. 
    * The reference position <code>r</code> is ignored since affine transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    *       -1 T
    * nr = F     n
    * </pre>
    * The result is <i>not</i> normalized since the unnormalized form could be
    * useful in some contexts.
    *
    * This method provides the low level implementation for normal
    * transformations and does not do any saving or restoring of data.
    *
    * @param nr transformed normal
    * @param n normal to be transformed
    * @param r reference position of the normal (ignored)
    */
   public void computeTransformNormal (Vector3d nr, Vector3d n, Vector3d r) {
      updateInverse();
      myInvX.A.mulTranspose (nr, n);
   }
 
   /**
    * Transforms an affine transform <code>X</code> and returns the result in
    * <code>XR</code>. If
    * <pre>
    *     [  A  p ]
    * X = [       ]
    *     [  0  1 ]
    * </pre>
    * the transform is computed according to
    * <pre>
    *      [  F A   F p + pf ]
    * XR = [                 ]
    *      [   0       1     ]
    * </pre>
    *
    * This method provides the low level implementation for the transformation
    * of affine transforms and does not do any saving or restoring of data.
    * 
    * @param XR transformed transform
    * @param X transform to be transformed
    */
   public void computeTransform (AffineTransform3d XR, AffineTransform3d X) {
      XR.mul (myX, X);
   }
   
   /**
    * Transforms a rotation matrix <code>R</code> and returns the result in
    * <code>RR</code>. The reference position <code>r</code> is ignored since
    * affine transforms are position invariant. This transform takes the form
    * <pre>
    * RR = Q R N 
    * </pre>
    * where <code>Q</code> is the orthogonal matrix from the left polar
    * decomposition <code>F = P Q</code>, and <code>N</code> is matrix that
    * flips an axis to ensure that <code>Q R N</code> remains right-handed.
    * For additional details, see the documentation for {@link
    * #transform(RotationMatrix3d,RotationMatrix3d,Vector3d) transform(RR,R,r)}.
    * 
    * This method provides the low level implementation for the transformation
    * of rotation matrices and does not do any saving or restoring of data.
    *
    * @param RR transformed rotation
    * @param R rotation to be transformed
    * @param r reference position of the rotation (ignored)
    */
   public void computeTransform (
      RotationMatrix3d RR, Vector3d Ndiag, RotationMatrix3d R, Vector3d r) {

      RR.mul (myPolarD.getQ(), R);
      if (Ndiag != null) {
         myPolarD.getN (Ndiag);
      }
      if (!myPolarD.isRightHanded()) {
         RR.negateColumn (myPolarD.getMaxQDiagIndex());
      }
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M</code> and returns the result
    * in <code>MR</code>.
    * The reference position <code>r</code> is ignored since affine transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    * MR = F M
    * </pre>
    * 
    * This method provides the low level implementation for the transformation
    * of 3 X 3 matrices and does not do any saving or restoring of data.
    *
    * @param MR transformed matrix
    * @param M matrix to be transformed
    * @param r reference position of the matrix (ignored)
    */
   public void computeTransform (Matrix3d MR, Matrix3d M, Vector3d r) {
      MR.mul (myX.A, M);
   }

   /**
    * Transforms a plane <code>p</code> and returns the result in <code>pr</code>.
    * The reference position <code>r</code> is ignored since affine transforms
    * are position invariant.
    * Assume that <code>p</code> is defined by a normal <code>n</code>
    * and offset <code>o</code> such that all planar points <code>x</code>
    * satisfy
    * <pre>
    * n^T x = o
    * </pre>
    * Then the transformed normal <code>nr</code> and offset <code>or</code>
    * are computed according to
    * <pre>
    * nr = inv(F)^T n
    * or = o + nr^T pf
    * mag = ||nr||
    * nr = nr/mag, or = or/mag
    * </pre>
    *
    * This method provides the low level implementation for the transformation
    * of planes and does not do any saving or restoring of data.
    *
    * @param pr transformed plane
    * @param p plane to be transformed
    * @param r reference position of the plane (ignored)
    */
   public void computeTransform (Plane pr, Plane p, Vector3d r) {
      pr.transform (myX, p);
   }

}
