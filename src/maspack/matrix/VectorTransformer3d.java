package maspack.matrix;

/**
 * Interface for an object that performs forward and inverse transformations
 * on 3D points, vectors, and normals.
 */
public interface VectorTransformer3d {

   /**
    * Transforms point <code>p0</code> and places the result in
    * <code>pr</code>.
    *
    * @param pr returns the transformed point
    * @param p0 point to be transformed
    */
   public void transformPnt (Vector3d pr, Vector3d p0);

   /**
    * Transforms vector <code>v0</code> and places the result in
    * <code>vr</code>.
    *
    * @param vr returns the transformed vector
    * @param v0 vector to be transformed
    */
   public void transformVec (Vector3d vr, Vector3d v0);

   /**
    * Transforms a covector <code>c0</code> and places the result in
    * <code>cr</code>. If a vector is transformed linearly according to
    * <pre>
    *   vr = A v0,
    * </pre>
    * then the covector will be transformed according to
    * <pre>
    *         -1 T 
    *   cr = A     c0,
    * </pre>
    * Normal vectors and gradients are generally transformed as covectors.
    * In the case of normals, the application will need to normalize the
    * result if this transformation is not rigid.
    * 
    * @param cr returns the transformed covector
    * @param c0 normal to be transformed
    */
   public void transformCovec (Vector3d cr, Vector3d c0);

   /**
    * Applies an inverse transform to point <code>p0</code> and places the
    * result in <code>pr</code>.
    *
    * @param pr returns the transformed point
    * @param p0 point to be transformed
    */
   public void inverseTransformPnt (Vector3d pr, Vector3d p0);

   /**
    * Applies an inverse transform to vector <code>v0</code> and places the
    * result in <code>vr</code>.
    *
    * @param vr returns the transformed vector
    * @param v0 vector to be transformed
    */
   public void inverseTransformVec (Vector3d vr, Vector3d v0);

   /**
    * Applies an inverse transform to covector <code>c0</code> and places the
    * result in <code>cr</code>. See {@link #transformCovec} for more
    * details about covector transformation.
    *
    * @param cr returns the transformed normal
    * @param c0 normal to be transformed
    */
   public void inverseTransformCovec (Vector3d cr, Vector3d c0);
   
   /**
    * Returns <code>true</code> if this transformer implements a linear
    * rigid transform.
    */
   public boolean isRigid();

   /**
    * Returns <code>true</code> if this transformer implements a linear affine
    * transform.
    */
   public boolean isAffine();  
   
   /**
    * Creates and returns a copy of this transformer.
    * 
    * @return copy of this transformer
    */
   public VectorTransformer3d copy();

   /**
    * Returns a string representation of this transformer.
    * 
    * @param fmtStr format string describing how floating point
    * numbers should be formatted
    * @return string representation of this transformer.
    */
   public String toString (String fmtStr);

}
