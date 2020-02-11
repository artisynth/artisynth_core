package artisynth.core.mechmodels;

import maspack.collision.PenetrationRegion;
import maspack.matrix.*;
import maspack.util.Clonable;

public interface ContactForceBehavior extends Clonable {

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
    * @param cpnt1 first contact point
    * @param cpnt2 second contact point
    * @param normal contact normal, facing outward from the surface
    * @param contactArea average area associated with the contact, or
    * -1 if this information is not available
    */
   public void computeResponse (
      double[] fres, double dist, ContactPoint cpnt1, ContactPoint cpnt2,
      Vector3d normal, double contactArea);
}
