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
    * Transforms a point <code>p1</code> and returns the result in
    * <code>pr</code>. The transform is computed according to
    * <pre>
    * pr = f(p1)
    * </pre>
    * This method provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * 
    * @param pr transformed point
    * @param p1 point to be transformed
    */
   public void computeTransformPnt (Point3d pr, Point3d p1) {
      getDeformation (pr, null, p1);
   }

   /**
    * Transforms a vector <code>v1</code>, and returns the result in
    * <code>vr</code>. 
    * The transform is computed according to
    * <pre>
    * vr = F v1
    * </pre>
    * where F is the deformation gradient at the reference position.
    *
    * This method provides the low level implementation for vector
    * transformations and does not do any saving or restoring of data.
    *
    * @param vr transformed vector
    * @param v1 vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */

   public void computeTransformVec (Vector3d vr, Vector3d v1, Vector3d r) {
      getDeformation (null, myF, r);
      myF.mul (vr, v1);
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
    *      [  RF R1    f(p1) ]
    * TR = [                 ]
    *      [    0       1    ]
    * </pre>
    * where PF RF = F is the left polar decomposition of the deformation
    * gradient at the reference position.
    * 
    * This method provides the low level implementation for the transformation
    * of rigid transforms and does not do any saving or restoring of data.
    *
    * @param TR transformed transform
    * @param T1 transform to be transformed
    */
   public void computeTransform (RigidTransform3d TR, RigidTransform3d T1) {
      getDeformation (myPos, myF, T1.p);
      myPolarD.factor (myF);
      TR.R.mul (myPolarD.getR(), T1.R);
      TR.p.set (myPos);
      //TR.mulAffineLeft (myX, myPolarD.getR());
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
    *      [  F A1   f(pf) ]
    * XR = [               ]
    *      [   0       1   ]
    * </pre>
    * where F is the deformation gradient at the reference position.
    *
    * This method provides the low level implementation for the transformation
    * of affine transforms and does not do any saving or restoring of data.
    * 
    * @param XR transformed transform
    * @param X1 transform to be transformed
    */
   public void computeTransform (AffineTransform3d XR, AffineTransform3d X1) {
      getDeformation (myPos, myF, X1.p);
      XR.A.mul (myF, X1.A);
      XR.p.set (myPos);
   }
   
   /**
    * Transforms a rotation matrix <code>R1</code>, located at reference
    * position <code>r</code>, and returns the result in <code>RR</code>.
    * The transform is computed according to
    * <pre>
    * RR = RF R1
    * </pre>
    * where PF RF = F is the left polar decomposition of the deformation
    * gradient at the reference position.
    * 
    * This method provides the low level implementation for the transformation
    * of rotation matrices and does not do any saving or restoring of data.
    *
    * @param RR transformed rotation
    * @param R1 rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    */
   public void computeTransform (
      RotationMatrix3d RR, RotationMatrix3d R1, Vector3d r) {
      getDeformation (null, myF, r);
      myPolarD.factor (myF);
      RR.mul (myPolarD.getR(), R1);
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M1</code>, located at reference
    * position <code>r</code>, and returns the result in <code>MR</code>.
    * The transform is computed according to
    * <pre>
    * MR = F M1
    * </pre>
    * where F is the deformation gradient at the reference position.
    * 
    * This method provides the low level implementation for the transformation
    * of 3 X 3 matrices and does not do any saving or restoring of data.
    *
    * @param MR transformed matrix
    * @param M1 matrix to be transformed
    * @param r reference position of the matrix, in original coordinates
    */
   public void computeTransform (Matrix3d MR, Matrix3d M1, Vector3d r) {
      getDeformation (null, myF, r);
      MR.mul (myF, M1);
   }

   /**
    * Transforms a plane <code>p1</code>, located at reference position
    * <code>r</code>, and returns the result in <code>pr</code>.
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
    * nr = nr / ||nr||
    * or = nr^T f(r)
    * </pre>
    * where F is the deformation gradient at the reference position.
    *
    * This method provides the low level implementation for the transformation
    * of planes and does not do any saving or restoring of data.
    *
    * @param pr transformed plane
    * @param p1 plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public void computeTransform (Plane pr, Plane p1, Vector3d r) {
      getDeformation (myPos, myF, r);
      Vector3d nrm = new Vector3d();
      myF.mulInverseTranspose (nrm, p1.normal);
      nrm.normalize();
      pr.set (nrm, nrm.dot(myPos));
   }

}
