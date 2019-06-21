package maspack.geometry;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorTransformer3d;

/**
 * Implements an affine transformation defined in homogeneous coordinates by
 * <pre>
 *  [  R S  p ]
 *  [         ]
 *  [   0   1 ]
 * </pre>
 * where <code>R</code> is a 3x3 rotation, <code>S</code> is a diagonal
 * scaling matrix, and <code>p</code> is a translation offset.
 */
public class ScaledRigidTransformer3d implements VectorTransformer3d {

   Vector3d myScaling = new Vector3d();
   Vector3d myInvScaling = new Vector3d();
   RigidTransform3d myRigidTrans = new RigidTransform3d();

   public ScaledRigidTransformer3d (
      Vector3d scaling, RigidTransform3d T) {
      set (scaling, T.R, T.p);
   }

   public ScaledRigidTransformer3d (
      Vector3d scaling, RotationMatrix3d R, Vector3d origin) {
      set (scaling, R, origin);
   }

   public void set (Vector3d scaling, RotationMatrix3d R, Vector3d p) {
      myScaling.set (scaling);
      myInvScaling.set (1/scaling.x, 1/scaling.y, 1/scaling.z);
      myRigidTrans.R.set (R);
      myRigidTrans.p.set (p);
   }

   public void getScaling (Vector3d scaling) {
      scaling.set (myScaling);
   }

   public void getOrigin (Vector3d p) {
      p.set (myRigidTrans.p);
   }

   public void getRotation (RotationMatrix3d R) {
      R.set (myRigidTrans.R);
   }

   public void getRigidTransform (RigidTransform3d T) {
      T.set (myRigidTrans);
   }

   /**
    * {@inheritDoc}
    */
   public void transformPnt (Vector3d pr, Vector3d p0) {
      pr.x = myScaling.x*p0.x;
      pr.y = myScaling.y*p0.y;
      pr.z = myScaling.z*p0.z;
      myRigidTrans.transformPnt (pr, pr);
   }

   /**
    * {@inheritDoc}
    */
   public void transformVec (Vector3d vr, Vector3d v0) {
      vr.x = myScaling.x*v0.x;
      vr.y = myScaling.y*v0.y;
      vr.z = myScaling.z*v0.z;
      myRigidTrans.transformVec (vr, vr);
   }

   /**
    * {@inheritDoc}
    */
   public void transformCovec (Vector3d nr, Vector3d n0) {
      nr.x = myInvScaling.x*n0.x;
      nr.y = myInvScaling.y*n0.y;
      nr.z = myInvScaling.z*n0.z;
      myRigidTrans.transformVec (nr, nr);
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformPnt (Vector3d pr, Vector3d p0) {
      myRigidTrans.inverseTransformPnt (pr, p0);
      pr.x *= myInvScaling.x;
      pr.y *= myInvScaling.y;
      pr.z *= myInvScaling.z;
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformVec (Vector3d vr, Vector3d v0) {
      myRigidTrans.inverseTransformVec (vr, v0);
      vr.x *= myInvScaling.x;
      vr.y *= myInvScaling.y;
      vr.z *= myInvScaling.z;
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformCovec (Vector3d nr, Vector3d n0) {
      myRigidTrans.inverseTransformVec (nr, n0);
      nr.x *= myScaling.x;
      nr.y *= myScaling.y;
      nr.z *= myScaling.z;
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
    * Scale the distance units associated with this transformer. This
    * is done by scaling both the scaling factors and the origin by {@code s}.
    * 
    * @param s scaling factor
    */
   public void scaleDistance (double s) {
      myRigidTrans.p.scale (s);
      myScaling.scale (s);
      myInvScaling.scale (1/s);
   }  

   /**
    * {@inheritDoc}
    */   
   public ScaledRigidTransformer3d copy() {
      return new ScaledRigidTransformer3d (myScaling, myRigidTrans);      
   }  

   public String toString (String fmtStr) {
      StringBuilder sb = new StringBuilder();
      sb.append ("scaling=" + myScaling.toString(fmtStr));
      sb.append ("\n");
      sb.append ("trans=\n" + myRigidTrans.toString(fmtStr));
      sb.append ("\n");
      return sb.toString();
   }
}
