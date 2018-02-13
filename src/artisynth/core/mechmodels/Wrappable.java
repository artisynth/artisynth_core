package artisynth.core.mechmodels;

import maspack.matrix.Matrix3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public interface Wrappable extends PointAttachable {

   public final double OUTSIDE = Double.MAX_VALUE;

   /**
    * Computes the point <code>pt</code> on the surface of this Wrappable such
    * that the line segment (pa,pt) is both tangent to the surface and as near
    * as possible to the line defined by the two points pa and p1.
    *
    * <p>To assist with the computation, <code>pKcheck[kmax]t</code> can be assumed to lie
    * fairly close to <code>p1</code>, and its projection onto the line can be
    * assumed to lie between <code>p1</code> and another point p0 defined by
    * <pre>
    * p0 = (1-lam0) pa + lam0 p1
    * </pre>
    * where <code>lam0</code> is a parameter between 0 and 1.
    * 
    * @param pt returns the tangent point
    * @param pa first point of the line 
    * @param p1 second point of the line
    * @param lam0 parameter defining point p0 as defined above
    * @param sideNrm a normalized vector perpendicular to the plane defined by
    * <code>pa</code>, <code>p1</code> and <code>pt</code>.  Can be used
    * to help compute <code>pt</code>
    */
   public void surfaceTangent (
      Point3d pt, Point3d pa, Point3d p1, double lam0, Vector3d sideNrm);

   /**
    * Computes the penetration distance of a point <code>p0</code> into this
    * Wrappable, along with the normal <code>nrm</code> that points from
    * <code>p0</code> to its nearest point on the surface. The distance should
    * be negative if the point is inside the wrappable, and positive otherwise.
    * If the point is outside and further than some minimum distance,
    * then the method should return the constant {@link #OUTSIDE}.
    * 
    * <p>If possible, the method should also compute the derivative of the
    * normal with respect to changes in <code>p0</code>, and return this in
    * <code>dnrm</code>. If this is not possible, <code>dnrm</code> should be
    * set to 0.
    * 
    * @param nrm returns the normal (should be normalized)
    * @param dnrm returns the derivative of the normal with respect to 
    * changes in p0, or zero if this cannot be determined.
    * @param p0 point to determine penetration for
    * @return returns the penetration distance, or {@link #OUTSIDE} if the
    * point is further than some minimum distance from the wrappable.
    */
   public double penetrationDistance (Vector3d nrm, Matrix3d dnrm, Point3d p0);

   /**
    * Returns a typical "radius" that should be expected for this wrappable,
    * defined as the distance for a surface point to the center.
    */
   public double getCharacteristicRadius();
   
   public RigidTransform3d getPose();
}
