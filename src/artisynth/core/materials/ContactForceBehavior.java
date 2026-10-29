package artisynth.core.materials;

import artisynth.core.modelbase.ContactPoint;
import maspack.collision.PenetrationRegion;
import maspack.matrix.*;
import maspack.geometry.Vertex3d;
import maspack.util.Clonable;
import maspack.util.DynamicArray;

public abstract class ContactForceBehavior extends MaterialBase {

   /**
    * Flag formerly used to indicate that vertex penetrations are being
    * calculated for <i>both</i> collidables, which means that the effective
    * area should be divided by two in order to produce the same contact
    * pressures as for one-way contact.
    *
    * @deprecated This flag is no longer set. Instead, for two-way contact,
    * the {@code contactArea} argument of {@link #computeResponse} is
    * halved for vertex contacts, so that no further scaling is required.
    */
   @Deprecated
   public static final int TWO_WAY_CONTACT = 0x0001;
   
   static DynamicArray<Class<?>> mySubclasses = new DynamicArray<>(
      new Class<?>[] {
      LinearElasticContact.class,
      AreaCompliantContact.class,
   });

   /**
    * Allow adding of classes (for use in control panels)
    * @param cls class to register
    */
   public static void registerSubclass(Class<? extends FemMaterial> cls) {
      if (!mySubclasses.contains(cls)) {
         mySubclasses.add(cls);
      }
   }

   public static Class<?>[] getSubClasses() {
      return mySubclasses.getArray();
   }

   /**
    * Computes the force response for a given contact. The response consists of
    * four values: force, compliance, damping, and step damping, which should
    * be placed into the first four entries of <code>fres</code>. Entries
    * that are not set default to 0. The force is defined as the restoring
    * force
    * <pre>
    * force = f(dist)
    * </pre>
    * computed in response to the interpenetration distance <code>dist</code>.
    * Note that in general, <code>dist</code> will be negative, and a
    * <i>negative</i> force will be expected in response. The compliance is the
    * reciprocal of the derivative of <code>f(dist)</code> evaluated at
    * <code>dist</code>. The damping is a positive damping parameter used to
    * generate damping forces in response to motion in the contact direction.
    * The step damping is an optional dimensionless factor that specifies a
    * minimum damping proportional to the step size {@code h}, such that the
    * effective damping {@code d} is given by
    * <pre>
    * d = max (damping, stepDamping*h/compliance)
    * </pre>
    * This allows the damping time constant {@code d*compliance} to be kept at
    * or above {@code h/2}, which is the minimum required for stable compliant
    * contact, without the behavior needing to know {@code h}. Damping and step
    * damping are applied only if the compliance is greater than 0.
    *
    * <p>Other information the can be used includes the two contact
    * points associated the contact, and the contact normal, which
    * is facing outward from the contact surface.
    *
    * @param fres returns the force, compliance, damping and step damping for
    * a given contact distance.
    * @param dist contact interpenetration distance. This value is usually
    * negative, with greater negativity indicating greater interpenetration.
    * @param cpnt0 first contact point
    * @param cpnt1 second contact point
    * @param normal contact normal, directed from cpnt0 to cpnt1
    * @param contactArea area associated with the contact. For vertex
    * penetration contacts, this is one third of the area of each face
    * surrounding the penetrating vertex; for edge-edge contacts, it is one
    * third of the area of each face adjacent to the first edge; in both
    * cases, the area is projected onto the plane perpendicular to the
    * normal. For contour region contacts, it is the area of the contact
    * region divided by the number of contact points. For two-way contact
    * (when vertex penetrations are computed for both collidables), the area
    * of vertex contacts is halved, since the vertices of both collidables
    * then cover the same contact patch.
    * @param flags flags providing additional information to the calculation.
    * No flags are currently defined.
    */
   public abstract void computeResponse (
      double[] fres, double dist, ContactPoint cpnt0, ContactPoint cpnt1,
      Vector3d normal, double contactArea, int flags);

   /**
    * Compute the local contact area for a contact described by contact points
    * {@code cpnt0} and {@code cpnt1} and a normal {@code normal}, using
    * {@link ContactPoint#computeContactArea}.
    *
    * @param cpnt0 first contact point
    * @param cpnt1 second contact point
    * @param normal contact normal, directed from cpnt0 to cpnt1
    * @return local contact area, or -1 if {@code cpnt0} is not associated
    * with a vertex or edge
    * @deprecated The contact area is now supplied directly to {@link
    * #computeResponse} via its {@code contactArea} argument, which should be
    * used instead. Note that for two-way vertex contact, the value returned
    * by this method is twice the effective area supplied by {@code
    * contactArea}.
    */
   @Deprecated
   public static double computeContactArea (
      ContactPoint cpnt0, ContactPoint cpnt1, Vector3d normal) {
      // just use the area of cpnt0
      return cpnt0.computeContactArea (normal);
   }

 
}
