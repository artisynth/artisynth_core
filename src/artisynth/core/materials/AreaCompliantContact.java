package artisynth.core.materials;

import artisynth.core.materials.ContactForceBehavior;
import artisynth.core.modelbase.ContactPoint;
import maspack.matrix.Vector3d;
import maspack.properties.*;


/**
 * Linear compliant contact whose stiffness is scaled by the area associated
 * with each contact. Each contact behaves as a linear spring of stiffness
 * {@code K = Ka*A} in parallel with a damper of coefficient
 * <pre>
 *   D = max (stiffnessDamping, stepDamping*h) * K
 * </pre>
 * where {@code Ka} is a stiffness per unit area (with units of
 * force/length^3), {@code A} is the area associated with the contact, {@code
 * h} is the simulation step size, {@code stepDamping} is a dimensionless
 * damping factor, and {@code stiffnessDamping} is a damping time constant.
 * Specifying stiffness per unit area, instead of per contact, makes the
 * contact pressure for a given penetration independent of the mesh
 * resolution.
 *
 * <p>This behavior is intended for vertex-based contacts (e.g., {@link
 * artisynth.core.mechmodels.CollisionBehavior.Method#VERTEX_PENETRATION
 * VERTEX_PENETRATION}). The area {@code A} is given by the {@code
 * contactArea} argument of {@link #computeResponse}, which for a vertex
 * contact is one third of the area of each face surrounding the penetrating
 * vertex, projected onto the plane perpendicular to the contact normal
 * (and halved for two-way contact).
 *
 * <p><b>Choosing Ka.</b> Adding contact compliance is roughly equivalent to
 * adding springs to each contact, so that the resulting local deformation
 * {@code delta} (i.e., the penetration) at each contact point is about
 * {@code p/Ka}, where {@code p} is the contact pressure. If the maximum
 * contact pressure {@code pmax} and a maximum allowable {@code delta} are
 * known, one can choose {@code Ka = pmax/deltaMax}.
 *
 * <p>Alternatively, for contact involving a deformable body, {@code Ka} can
 * be set to try to control the ratio {@code deltaB/delta}, where {@code
 * deltaB} is the deformation in the contact direction that the body itself
 * experiences under pressure {@code p}. If the body is assumed to be linearly
 * elastic with Young's modulus {@code E} and a Poisson's ratio of 0, then
 * {@code deltaB} is about {@code p*Le/E}, where {@code Le} is the
 * <i>effective thickness</i> of the body along the contact direction; i.e.,
 * the depth over which the contact stress stays concentrated over the contact
 * patch. {@code Le} lies between the true body thickness {@code L} and the
 * minimal width {@code dp} of the contact patch. With {@code Ap} the area of
 * the patch and {@code AB} the cross-sectional area of the body in the
 * contact plane, the following rules of thumb can be useful:
 *
 * <table border="1">
 * <caption>Effective thickness rules of thumb</caption>
 * <tr><th>body type</th><th>condition</th><th>Le</th></tr>
 * <tr><td>thin layer</td><td>L &lt;&lt; dp</td><td>~ L</td></tr>
 * <tr><td>compact</td><td>L &gt;&gt; dp, AB &gt;&gt; Ap</td><td>~ dp</td></tr>
 * <tr><td>slender</td><td>L &gt;&gt; dp, AB ~ Ap</td><td>~ L</td></tr>
 * </table>
 *
 * <p>A nonzero Poisson's ratio will make the body stiffer, particularly for
 * a thin, nearly incompressible layer whose lateral expansion is constrained
 * (by a factor of about 3.8 for a Poisson's ratio of 0.45), in which case a
 * smaller value of {@code Ka} can be used. Given an estimate for {@code Le},
 * {@code Ka} can then be chosen from
 * <pre>
 *   Ka = rho * E / Le
 * </pre>
 * where {@code rho} is a stiffness ratio that equals {@code deltaB/delta}
 * under the assumptions given above. {@code rho} should be large enough that
 * {@code delta} is small relative to {@code deltaB}, but not so large that
 * the noisy, unevenly distributed contact pressures that can arise with
 * non-compliant vertex contact are reintroduced. Tests, while not
 * exhaustive, suggest that values of {@code rho} between 10 and 30 are
 * usually a good compromise. The default value of {@code Ka} corresponds to
 * {@code rho = 20} for a cartilage-like layer with {@code E = 5e6} and a
 * thickness of 2 mm (in SI units). It should usually be adjusted for other
 * materials, geometries and units.
 *
 * <p><b>Choosing the damping.</b> Compliant contact forces are integrated
 * using the penetration estimated at the middle of the time step. As a
 * result, the damping time constant {@code D/K} must be at least {@code
 * h/2}. If it is smaller, stiff contacts oscillate from one step to the
 * next, with contacts possibly switching on and off at every step, and the
 * oscillation becomes more persistent as {@code Ka} increases. The {@code
 * stepDamping} property provides this minimum damping automatically, by
 * setting {@code D/K} to {@code stepDamping*h}, where {@code h} is the
 * current step size. This does not depend on the masses or units of the
 * model, and adjusts automatically if the step size is changed. A value of
 * 0.5 damps the oscillation within a single step, while larger values give a
 * smoother but more sluggish response. The default value of 1 is suitable
 * for most models. Values less than 0.5 should be avoided unless {@code
 * stiffnessDamping} is set to at least {@code h/2}.
 *
 * <p>Step damping is a numerical device and does not model physical
 * damping. In particular, when a light rigid body strikes a stiff compliant
 * contact, it rebounds with a coefficient of restitution of roughly {@code
 * 0.5/(0.5+stepDamping)} (or about 1/3 for the default value), regardless
 * of the step size; increasing {@code stepDamping} reduces this rebound.
 * The {@code stiffnessDamping} property provides damping that is
 * independent of the step size, and is needed only when such damping is
 * required; for example, when the oscillation of a rigid body of mass
 * {@code M} on a soft contact, with frequency {@code w = sqrt(Ktot/M)}
 * (where {@code Ktot} is the total stiffness of all its contacts), is
 * resolved by the step size (i.e., {@code w*h} is less than about 1). A
 * damping ratio of about {@code zeta} can then be obtained by setting {@code
 * stiffnessDamping} to {@code 2*zeta/w}. FEM models rarely require {@code
 * stiffnessDamping}.
 */
public class AreaCompliantContact extends ContactForceBehavior {

   protected static double DEFAULT_KA = 5e10;
   double myKa = DEFAULT_KA;   // stiffness per unit area (N/m^3)

   protected static double DEFAULT_STEP_DAMPING = 1.0;
   double myStepDamping = DEFAULT_STEP_DAMPING;  // step damping factor

   protected static double DEFAULT_STIFFNESS_DAMPING = 0;
   double myStiffnessDamping = DEFAULT_STIFFNESS_DAMPING; // time constant (s)

   public static PropertyList myProps =
      new PropertyList (
         AreaCompliantContact.class, ContactForceBehavior.class);

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   static {
      myProps.add (
         "Ka", "stiffness per unit area (force/length^3)",
         DEFAULT_KA, "[0,inf)");
      myProps.add (
         "stepDamping",
         "damping time constant as a multiple of the step size",
         DEFAULT_STEP_DAMPING, "[0,inf)");
      myProps.add (
         "stiffnessDamping",
         "step size independent damping time constant",
         DEFAULT_STIFFNESS_DAMPING, "[0,inf)");
   }

   /**
    * Need no-args constructor for scanning
    */
   public AreaCompliantContact () {
   }

   /**
    * Queries the contact stiffness per unit area. See {@link #setKa}.
    *
    * @return stiffness per unit area
    */
   public double getKa() {
      return myKa;
   }

   /**
    * Sets the contact stiffness per unit area, such that the stiffness of
    * each contact is {@code Ka} times its associated area. See the class
    * documentation for advice on choosing this value.
    *
    * @param ka stiffness per unit area (force/length^3)
    */
   public void setKa (double ka) {
      myKa = ka;
   }

   /**
    * Queries the step damping factor. See {@link #setStepDamping}.
    *
    * @return step damping factor
    */
   public double getStepDamping() {
      return myStepDamping;
   }

   /**
    * Sets the step damping factor, such that the damping coefficient of each
    * contact is at least {@code stepDamping*h} times its stiffness, where
    * {@code h} is the current step size. The default value is 1, and values
    * less than 0.5 should usually be avoided; see the class documentation
    * for details.
    *
    * @param damping step damping factor
    */
   public void setStepDamping (double damping) {
      myStepDamping = damping;
   }

   /**
    * Queries the stiffness damping time constant. See {@link
    * #setStiffnessDamping}.
    *
    * @return stiffness damping time constant
    */
   public double getStiffnessDamping() {
      return myStiffnessDamping;
   }

   /**
    * Sets the stiffness damping time constant, such that the damping
    * coefficient of each contact is at least {@code stiffnessDamping} times
    * its stiffness, independently of the step size. The default value is 0;
    * see the class documentation for when a nonzero value is needed.
    *
    * @param tau stiffness damping time constant
    */
   public void setStiffnessDamping (double tau) {
      myStiffnessDamping = tau;
   }

   /**
    * Creates a new AreaCompliantContact with a specified stiffness per unit
    * area and default damping properties.
    *
    * @param kA stiffness per unit area (force/length^3)
    */
   public AreaCompliantContact (double kA) {
      myKa = kA;
   }

   /**
    * Creates a new AreaCompliantContact with specified properties.
    *
    * @param kA stiffness per unit area (force/length^3)
    * @param stepDamping step damping factor
    * @param stiffnessDamping stiffness damping time constant
    */
   public AreaCompliantContact (
      double kA, double stepDamping, double stiffnessDamping) {
      myKa = kA;
      myStepDamping = stepDamping;
      myStiffnessDamping = stiffnessDamping;
   }

   /**
    * {@inheritDoc}
    */
   public void computeResponse (
      double[] fres, double dist, ContactPoint cpnt0, ContactPoint cpnt1,
      Vector3d normal, double contactArea, int flags) {

      double k = myKa*contactArea;
      fres[0] = k*dist;
      fres[1] = 1/k;
      fres[2] = myStiffnessDamping*k;
      fres[3] = myStepDamping;
   }
}
