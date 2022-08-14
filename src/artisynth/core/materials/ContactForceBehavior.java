package artisynth.core.materials;

import artisynth.core.modelbase.ContactPoint;
import maspack.collision.PenetrationRegion;
import maspack.matrix.*;
import maspack.geometry.Vertex3d;
import maspack.util.Clonable;
import maspack.util.DynamicArray;

public abstract class ContactForceBehavior extends MaterialBase {

   /**
    * Flag indicating that vertex penetrations are being calculated for
    * <i>both</i> collidables, which means that the effective area should be
    * divided by two in order to produce the same contact pressures as for
    * one-way contact.
    */
   public static final int TWO_WAY_CONTACT = 0x0001;
   
   static DynamicArray<Class<?>> mySubclasses = new DynamicArray<>(
      new Class<?>[] {
      LinearElasticContact.class,
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
    * three values: force, compliance, and damping, all of which should be
    * placed into <code>fres</code>. The force is defined as the restoring
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
    *
    * <p>Other information the can be used includes the two contact
    * points associated the contact, and the contact normal, which
    * is facing outward from the contact surface.
    *
    * @param fres returns the force, compliance and damping for a given
    * contact distance.
    * @param dist contact interpenetration distance. This value is usually
    * negative, with greater negativity indicating greater interpenetration.
    * @param cpnt0 first contact point
    * @param cpnt1 second contact point
    * @param normal contact normal, directed from cpnt0 to cpnt1
    * @param contactArea average area associated with the contact, or
    * -1 if this information is not available
    * @param flags flags providing additional information to the calculation.
    * These include {@link #TWO_WAY_CONTACT}, which indicates two-way vertex
    * penetration contact.
    */
   public abstract void computeResponse (
      double[] fres, double dist, ContactPoint cpnt0, ContactPoint cpnt1,
      Vector3d normal, double contactArea, int flags);

   /**
    * Compute the local contact area for a contact described by contact points
    * {@code cpnt0} and {@code cpnt1} and a normal {@code normal}.  This methid
    * required that the contact was produced by vertex penetration contact;
    * otherwise, it returns -1.
    *
    * @param cpnt0 first contact point
    * @param cpnt1 second contact point
    * @param normal contact normal, directed from cpnt0 to cpnt1
    * @return local contact area, or -1 if the contact was not produced by
    * vertex penetration contact
    */
   public static double computeContactArea (
      ContactPoint cpnt0, ContactPoint cpnt1, Vector3d normal) {
      // just use the area of cpnt0
      return cpnt0.computeContactArea (normal);
   }

 
}
