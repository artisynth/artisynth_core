package maspack.matrix;

/**
 * Vector3dTransform that does nothing.
 */
public class IdentityVector3dTransform implements VectorTransformer3d {

   /**
    * {@inheritDoc}
    */
   public void transformPnt (Vector3d pr, Vector3d p0) {
      if (pr != p0) {
         pr.set (p0);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void transformVec (Vector3d vr, Vector3d v0) {
      if (vr != v0) {
         vr.set (v0);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void transformCovec (Vector3d nr, Vector3d n0) {
      if (nr != n0) {
         nr.set (n0);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformPnt (Vector3d pr, Vector3d p0) {
      if (pr != p0) {
         pr.set (p0);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformVec (Vector3d vr, Vector3d v0) {
      if (vr != v0) {
         vr.set (v0);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformCovec (Vector3d nr, Vector3d n0) {
      if (nr != n0) {
         nr.set (n0);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isRigid() {
      return true;
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
   public IdentityVector3dTransform copy() {
      return new IdentityVector3dTransform ();
   } 

   /**
    * {@inheritDoc}
    */
   public String toString (String fmtStr) {
      return "IdentityTransform";
   }
}
