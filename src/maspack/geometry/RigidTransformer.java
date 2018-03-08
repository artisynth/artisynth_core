package maspack.geometry;

import maspack.matrix.*;

/**
 * A GeometryTransformer that implements a linear rigid body transformation.
 * For points, this can be expressed in homogenous coordinates as
 *<pre> 
 * [ p' ]     [ p ]       [  RF   pf  ]
 * [    ] = T [   ],  T = [           ]
 * [ 1  ]     [ 1 ]       [  0     1  ]
 *</pre>
 * where R is a 3 X 3 rotation matrix and pf is an offset vector.
 * 
 * @author John E Lloyd
 *
 */
public class RigidTransformer extends GeometryTransformer {

   RigidTransform3d myT;

  /**
    * Creates a new RigidTransformer from a specified rigid transform.
    *
    * @param T rigid transform defining the transformation
    */
   public RigidTransformer (RigidTransform3d T) {
      myT = new RigidTransform3d (T);
   }

   /**
    * Returns <code>true</code>, since this transformer does implement a linear
    * rigid transform.
    */
   public boolean isRigid() {
      return true;
   }

   /**
    * Returns <code>true</code>, since a rigid transform is a special case of
    * an affine transform.
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
   
   /**
    * Returns <code>false</code>, since a rigid transform cannot be
    * reflecting.
    */
   public boolean isReflecting() {
      return false;
   }

   /**
    * Returns a transformer that implements the inverse operation of this
    * transformer.
    * 
    * @return inverse transformer
    */
   public RigidTransformer getInverse() {
      RigidTransform3d T = new RigidTransform3d (myT);
      T.invert();
      return new RigidTransformer (T);
   }

   /**
    * Transforms a point <code>p</code> and returns the result in
    * <code>pr</code>. The transform is computed according to
    * <pre>
    * pr = RF p + pf
    * </pre>
    * This method provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * 
    * @param pr transformed point
    * @param p point to be transformed
    */
   public void computeTransformPnt (Point3d pr, Point3d p) {
      pr.transform (myT, p);
   }

   /**
    * Transforms a vector <code>v</code>, and returns the result in
    * <code>vr</code>. 
    * The reference position <code>r</code> is ignored since rigid transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    * vr = RF v
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
      vr.transform (myT, v);
   }

   /**
    * Computes the matrices <code>PL</code> and <code>N</code> that transform
    * points <code>xl</code> local to a coordinate frame <code>T</code> after
    * that frame is itself transformed. For rigid transforms, <code>N</code>
    * is always the identity, and the updated local coordinates are
    * given by
    * <pre>
    * xl' = PL xl
    * </pre>
    * where <code>PL</code> is symmetric positive definite. See the
    * documentation for {@link GeometryTransformer#computeLocalTransforms}.
    *
    * @param PL primary transformation matrix
    * @param Ndiag if non-null, returns the diagonal components of N
    * (which will be all ones)
    * @param T rigid transform for which the local transforms are computed
    */
   public void computeLocalTransforms (
      Matrix3d PL, Vector3d Ndiag, RigidTransform3d T) {
      PL.setIdentity();
      if (Ndiag != null) {
         Ndiag.set (Vector3d.ONES);
      }
   }
   
   /**
    * Transforms a normal vector <code>n</code>, and returns the result in
    * <code>nr</code>. 
    * The reference position <code>r</code> is ignored since rigid transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    * nr = RF n
    * </pre>
    *
    * This method provides the low level implementation for normal
    * transformations and does not do any saving or restoring of data.
    *
    * @param nr transformed normal
    * @param n normal to be transformed
    * @param r reference position of the normal (ignored)
    */
   public void computeTransformNormal (Vector3d nr, Vector3d n, Vector3d r) {
      nr.transform (myT, n);
   }

   /**
    * {@inheritDoc}
    */
   public void computeTransform (RigidTransform3d TR, RigidTransform3d T) {
      TR.mul (myT, T);
   }

   /**
    * Transforms an affine transform <code>X</code> and returns the result in
    * <code>XR</code>. If
    * <pre>
    *      [ A   p ]
    * X =  [       ]
    *      [ 0   1 ]
    * </pre>
    * the transform is computed according to
    * <pre>
    *      [  RF A   RF p + pf ]
    * XR = [                   ]
    *      [   0         1     ]
    * </pre>
    *
    * This method provides the low level implementation for the transformation
    * of affine transforms and does not do any saving or restoring of data.
    * 
    * @param XR transformed transform
    * @param X transform to be transformed
    */
   public void computeTransform (AffineTransform3d XR, AffineTransform3d X) {
      XR.mul (myT, X);
   }
   
   /**
    * Transforms a rotation matrix <code>R</code> and returns the result in
    * <code>RR</code>.
    * The reference position <code>r</code> is ignored since rigid transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    * RR = RF R
    * </pre>
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
      RR.mul (myT.R, R);
      if (Ndiag != null) {
         Ndiag.set (Vector3d.ONES);
      }
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M</code>,
    * and returns the result in <code>MR</code>.
    * The reference position <code>r</code> is ignored since rigid transforms
    * are position invariant.
    * The transform is computed according to
    * <pre>
    * MR = RF M
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
      MR.mul (myT.R, M);
   }

   /**
    * Transforms a plane <code>p</code> and returns the result in
    * <code>pr</code>.
    * The reference position <code>r</code> is ignored since rigid transforms
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
    * nr = RF n
    * or = o + nr^T pf
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
      pr.transform (myT, p);
   }

}
