package maspack.geometry;

import maspack.matrix.Vector3d;
import maspack.matrix.VectorTransformer3d;

/**
 * Implements an affine transformation defined in homogeneous coordinates by
 * <pre>
 *  [   S   p ]
 *  [         ]
 *  [   0   1 ]
 * </pre>
 * where <code>S</code> is a 3x3 diagonal
 * scaling matrix, and <code>p</code> is a translation offset.
 */
public class ScaledTranslation3d implements VectorTransformer3d {

   Vector3d myScaling = new Vector3d();
   Vector3d myInvScaling = new Vector3d();
   Vector3d myOrigin = new Vector3d();

   public ScaledTranslation3d (Vector3d scale, Vector3d p) {
      set (scale, p);
   }

   public void set (Vector3d scale, Vector3d p) {
      myScaling.set (scale);
      myInvScaling.set (1/scale.x, 1/scale.y, 1/scale.z);
      myOrigin.set (p);
   }

   public void getScaling (Vector3d scaling) {
      scaling.set (myScaling);
   }

   public void getOrigin (Vector3d p) {
      p.set (myOrigin);
   }
   
   /**
    * {@inheritDoc}
    */
   public void transformPnt (Vector3d pr, Vector3d p0) {
      pr.x = myScaling.x*p0.x;
      pr.y = myScaling.y*p0.y;
      pr.z = myScaling.z*p0.z;
      pr.add (myOrigin);
   }

   /**
    * {@inheritDoc}
    */
   public void transformVec (Vector3d vr, Vector3d v0) {
      vr.x = myScaling.x*v0.x;
      vr.y = myScaling.y*v0.y;
      vr.z = myScaling.z*v0.z;
   }

   /**
    * {@inheritDoc}
    */
   public void transformCovec (Vector3d nr, Vector3d n0) {
      nr.x = myInvScaling.x*n0.x;
      nr.y = myInvScaling.y*n0.y;
      nr.z = myInvScaling.z*n0.z;
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformPnt (Vector3d pr, Vector3d p0) {
      pr.sub (p0, myOrigin);
      pr.x *= myInvScaling.x;
      pr.y *= myInvScaling.y;
      pr.z *= myInvScaling.z;
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformVec (Vector3d vr, Vector3d v0) {
      vr.x = myInvScaling.x*v0.x;
      vr.y = myInvScaling.y*v0.y;
      vr.z = myInvScaling.z*v0.z;
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformCovec (Vector3d nr, Vector3d n0) {
      nr.x = myScaling.x*n0.x;
      nr.y = myScaling.y*n0.y;
      nr.z = myScaling.z*n0.z;
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
      myOrigin.scale (s);
      myScaling.scale (s);
      myInvScaling.scale (1/s);
   }
   
   /**
    * {@inheritDoc}
    */
   public ScaledTranslation3d copy() {
      return new ScaledTranslation3d (myScaling, myOrigin);      
   }

   public String toString (String fmtStr) {
      StringBuilder sb = new StringBuilder();
      sb.append ("scaling=" + myScaling.toString(fmtStr));
      sb.append ("\n");
      sb.append ("origin=" + myOrigin.toString(fmtStr));
      sb.append ("\n");
      return sb.toString();
   }

}
