package maspack.geometry;

import maspack.matrix.*;

/**
 * Base class for GeometryTransformers that implement general deformation
 * fields. Subclasses must implement a single abstract method that for a given
 * reference point <code>r</code> returns its deformed position
 * <code>f(r)</code> and the deformation gradient <code>F</code> at that
 * location.
 * 
 * @author John E Lloyd
 *
 */
public abstract class DeformationTransformer extends GeometryTransformer {

   Matrix3d myF;
   Vector3d myPos;
   PolarDecomposition3d myPolarD;

   public DeformationTransformer () {
      myF = new Matrix3d();
      myPos = new Vector3d();
      myPolarD = new PolarDecomposition3d();
   }

   /**
    * Returns <code>false</code> since this transformer does not implement a
    * linear rigid transform.
    */
   public boolean isRigid() {
      return false;
   }

   /**
    * Returns <code>false</code> since this transformer does not implement a
    * linear affine transform.
    */
   public boolean isAffine() {
      return false;
   }

   /**
    * Returns <code>false</code> since this transformer is not by default
    * invertible; subclasses my override this.
    */
   public boolean isInvertible() {
      return false;
   }

   /**
    * Returns <code>null</code> by since this transformer is not by default
    * invertible; subclasses my override this.
    */
   public DeformationTransformer getInverse() {
      return null;
   }

   /**
    * Computes the deformed position <code>f(r)</code> and deformation
    * gradient <code>F</code> for a given reference point <code>r</code> in
    * undeformed coordinates.
    * 
    * @param p if non-<code>null</code>, returns the deformed position
    * @param F if non-<code>null</code>, returns the deformation gradient
    * @param r reference point in undeformed coordinates
    */
   public abstract void getDeformation (Vector3d p, Matrix3d F, Vector3d r);

   /**
    * Transforms a point <code>p</code> and returns the result in
    * <code>pr</code>. The transform is computed according to
    * <pre>
    * pr = f(p)
    * </pre>
    * This method provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * @param pr transformed point
    * @param p point to be transformed
    */
   public void computeTransformPnt (Point3d pr, Point3d p) {
      getDeformation (pr, null, p);
   }

   /**
    * Transforms a vector <code>v</code>, located at a reference
    * position <code>r</code>, and returns the result in
    * <code>vr</code>. 
    * The transform is computed according to
    * <pre>
    * vr = F v
    * </pre>
    * where F is the deformation gradient at the reference position.
    *
    * This method provides the low level implementation for vector
    * transformations and does not do any saving or restoring of data.
    *
    * @param vr transformed vector
    * @param v vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */
   public void computeTransformVec (Vector3d vr, Vector3d v, Vector3d r) {
      getDeformation (null, myF, r);
      myF.mul (vr, v);
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
    *
    * @param PL primary transformation matrix
    * @param Ndiag if non-null, returns the diagonal components of N
    * @param T rigid transform for which the local transforms are computed
    */
   public void computeLocalTransforms (
      Matrix3d PL, Vector3d Ndiag, RigidTransform3d T) {
      
      getDeformation (null, myF, T.p);
      myPolarD.factorLeft (myF);
      if (Ndiag != null) {
         myPolarD.getN (Ndiag);
      }
      PL.mulTransposeLeft (myPolarD.getQ(), myF);
      PL.inverseTransform (T.R);
   }

   /**
    * Transforms a normal vector <code>n</code>, located at a reference
    * position <code>r</code>, and returns the result in
    * <code>nr</code>. 
    * The transform is computed according to
    * <pre>
    *       -1 T
    * nr = F     n
    * </pre>
    * where F is the deformation gradient at the reference position.
    * 
    * The result is <i>not</i> normalized since the unnormalized form could be
    * useful in some contexts.
    *
    * This method provides the low level implementation for normal
    * transformations and does not do any saving or restoring of data.
    *
    * @param nr transformed normal
    * @param n normal to be transformed
    * @param r reference position of the normal, in original coordinates
    */
   public void computeTransformNormal (Vector3d nr, Vector3d n, Vector3d r) {
      getDeformation (null, myF, r);
      myF.mulInverseTranspose (nr, n);
   }

   /**
    * Transforms an affine transform <code>X</code> and returns the result in
    * <code>XR</code>. If
    * <pre>
    *     [  A   p ]
    * X = [        ]
    *     [  0   1 ]
    * </pre>
    * the transform is computed according to
    * <pre>
    *      [  F A    f(p) ]
    * XR = [              ]
    *      [   0      1   ]
    * </pre>
    * where f(p) and F are the deformation and deformation gradient at p.
    *
    * This method provides the low level implementation for the transformation
    * of affine transforms and does not do any saving or restoring of data.
    * 
    * @param XR transformed transform
    * @param X transform to be transformed
    */
   public void computeTransform (AffineTransform3d XR, AffineTransform3d X) {
      getDeformation (myPos, myF, X.p);
      XR.A.mul (myF, X.A);
      XR.p.set (myPos);
   }
   
   /**
    * Transforms a rotation matrix <code>R</code>, located at reference
    * position <code>r</code>, and returns the result in <code>RR</code>.
    * The transform is computed according to
    * <pre>
    * RR = RF R
    * </pre>
    * where PF RF = F is the left polar decomposition of the deformation
    * gradient at the reference position.
    * 
    * This method provides the low level implementation for the transformation
    * of rotation matrices and does not do any saving or restoring of data.
    *
    * @param RR transformed rotation
    * @param R rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    */
   public void computeTransform (
      RotationMatrix3d RR, Vector3d Ndiag, RotationMatrix3d R, Vector3d r) {

      getDeformation (null, myF, r);
      myPolarD.factorLeft (myF);
      RR.mul (myPolarD.getQ(), R);
      if (Ndiag != null) {
         myPolarD.getN (Ndiag);
      }
      if (!myPolarD.isRightHanded()) {
         RR.negateColumn (myPolarD.getMaxQDiagIndex());
      }
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M</code>, located at reference
    * position <code>r</code>, and returns the result in <code>MR</code>.
    * The transform is computed according to
    * <pre>
    * MR = F M
    * </pre>
    * where F is the deformation gradient at the reference position.
    * 
    * This method provides the low level implementation for the transformation
    * of 3 X 3 matrices and does not do any saving or restoring of data.
    *
    * @param MR transformed matrix
    * @param M matrix to be transformed
    * @param r reference position of the matrix, in original coordinates
    */
   public void computeTransform (Matrix3d MR, Matrix3d M, Vector3d r) {
      getDeformation (null, myF, r);
      MR.mul (myF, M);
   }

   /**
    * Transforms a plane <code>p</code>, located at reference position
    * <code>r</code>, and returns the result in <code>pr</code>.
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
    * nr = nr / ||nr||
    * or = nr^T f(r)
    * </pre>
    * where F is the deformation gradient at the reference position.
    *
    * This method provides the low level implementation for the transformation
    * of planes and does not do any saving or restoring of data.
    *
    * @param pr transformed plane
    * @param p plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public void computeTransform (Plane pr, Plane p, Vector3d r) {
      // rp = r projected on to the project r onto the plane
      getDeformation (myPos, myF, r);
      Vector3d nrm = new Vector3d();
      myF.mulInverseTranspose (nrm, p.normal);
      nrm.normalize();
      pr.set (nrm, nrm.dot(myPos));
   }

}
