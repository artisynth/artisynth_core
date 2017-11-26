package maspack.matrix;

/**
 * Interface for an object that performs forward and inverse transforms
 * on 3D points, vectors, and normals.
 */
public interface Vector3dTransform {

   /**
    * Transforms point <code>p0</code> and places the result in
    * <code>pr</code>.
    *
    * @param pr returns the transformed point
    * @param p0 point to be transformed
    */
   public void transformPoint (Vector3d pr, Vector3d p0);

   /**
    * Transforms vector <code>v0</code> and places the result in
    * <code>vr</code>.
    *
    * @param vr returns the transformed vector
    * @param v0 vector to be transformed
    */
   public void transformVector (Vector3d vr, Vector3d v0);

   /**
    * Transforms normal <code>n0</code> and places the result in
    * <code>nr</code>.
    *
    * @param nr returns the transformed normal
    * @param n0 normal to be transformed
    */
   public void transformNormal (Vector3d nr, Vector3d n0);

   /**
    * Applies an inverse transform to point <code>p0</code> and places the
    * result in <code>pr</code>.
    *
    * @param pr returns the transformed point
    * @param p0 point to be transformed
    */
   public void inverseTransformPoint (Vector3d pr, Vector3d p0);

   /**
    * Applies an inverse transform to vector <code>v0</code> and places the
    * result in <code>vr</code>.
    *
    * @param vr returns the transformed vector
    * @param v0 vector to be transformed
    */
   public void inverseTransformVector (Vector3d vr, Vector3d v0);

   /**
    * Applies an inverse transform to normal <code>n0</code> and places the
    * result in <code>nr</code>.
    *
    * @param nr returns the transformed normal
    * @param n0 normal to be transformed
    */
   public void inverseTransformNormal (Vector3d nr, Vector3d n0);

}
