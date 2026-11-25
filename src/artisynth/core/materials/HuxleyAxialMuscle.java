/**
 * Copyright (c) 2026, by the Authors: Jeff M. Barrett
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.materials;

import artisynth.core.modelbase.HasNumericState;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.util.DataBuffer;

/**
 * Point-to-point muscle material based on Huxley's (1957) cross-bridge model,
 * as reduced to a system of ordinary differential equations using Zahalak's
 * (1981) distribution-moment (DM) approximation. Unlike Hill-type materials,
 * which prescribe force-velocity behavior directly, the force produced by this
 * material emerges from the attachment and detachment kinetics of a
 * population of cross-bridges. This results in history-dependent behavior
 * such as short-range stiffness, force transients following length changes,
 * and a force-velocity relationship that is a consequence of the kinetics.
 *
 * <h2>Cross-bridge model</h2>
 *
 * Let {@code n(xi,t)} be the fraction of cross-bridges attached with a
 * normalized strain {@code xi = x/h}, where {@code x} is the displacement of a
 * cross-bridge from its unstrained position and {@code h} is the attachment
 * range (the maximum displacement at which a cross-bridge can attach). The
 * distribution evolves according to
 * <pre>
 *   dn/dt - u dn/dxi = a f(xi) - (f(xi) + g(xi)) n
 * </pre>
 * where {@code a} is the activation (given by the muscle excitation), {@code
 * u} is the normalized shortening velocity of the half-sarcomere, and {@code
 * f(xi)} and {@code g(xi)} are the attachment and detachment rate functions:
 * <pre>
 *   f(xi) = f1 xi                  for 0 &lt;= xi &lt;= 1, and 0 otherwise,
 *
 *   g(xi) = g0                     for xi &lt; 0,
 *           g1 xi                  for 0 &lt;= xi &lt;= 1,
 *           g1 + g2 (xi - 1)       for xi &gt; 1.
 * </pre>
 * The detachment rate is continuous at {@code xi = 1}, and increases
 * linearly beyond it.
 *
 * <p>Activation scales only the attachment source term, so that under isometric
 * conditions the steady-state distribution is uniform on {@code [0, 1]} with
 * height {@code a f1/(f1+g1)}.
 *
 * <h2>Distribution-moment approximation</h2>
 *
 * Rather than solving for {@code n(xi,t)} directly, the material tracks its
 * first three moments
 * <pre>
 *   Q_k(t) = integral xi^k n(xi,t) dxi,     k = 0, 1, 2,
 * </pre>
 * which evolve according to
 * <pre>
 *   dQ_k/dt = a beta_k - phi_k - psi_k - k u Q_{k-1}
 * </pre>
 * where {@code beta_k}, {@code phi_k} and {@code psi_k} are the moments of
 * {@code f}, {@code f n} and {@code g n}. To evaluate {@code phi_k} and {@code
 * psi_k}, {@code n} is assumed to be a uniform distribution of height {@code
 * N} on the interval {@code [xi0-Delta, xi0+Delta]}, with
 * <pre>
 *   xi0 = Q1/Q0,  Delta = sqrt(3 (Q0 Q2 - Q1^2))/Q0,  N = Q0/(2 Delta).
 * </pre>
 * This assumption is exact for the isometric steady state.
 *
 * <p>The muscle force and short-range stiffness are proportional to {@code
 * Q1} and {@code Q0}, respectively:
 * <pre>
 *   F = Gamma1 Q1 + d ldot,     dF/dl = Gamma0 Q0,
 *
 *   Gamma1 = A m s k h^2 / (2 ell),  Gamma0 = Gamma1 s / (2 h X)
 * </pre>
 * where {@code A} is the cross-sectional area, {@code m} is the number density
 * of cross-bridges, {@code s} is the sarcomere length, {@code k} is the
 * cross-bridge stiffness, {@code ell} is the spacing between actin binding
 * sites, {@code X} is the fibre length, and {@code d} is an optional linear
 * damping coefficient. The fibre length change associated with a unit change
 * in {@code xi} is {@code gamma = 2 X h / s}.
 *
 * <h2>Numerical treatment</h2>
 *
 * The moments are integrated using operator splitting. At the start of each
 * time step (of size {@code h_t}), the kinetic terms are advanced explicitly
 * from the start-of-step moments,
 * <pre>
 *   Qk_k = Q_k + h_t (a beta_k - phi_k - psi_k),
 * </pre>
 * and the convection term is then applied exactly, by shifting the
 * distribution by {@code delta = (l - l_s)/gamma}, where {@code l_s} is the
 * length at the start of the step:
 * <pre>
 *   Q0 = Qk0,  Q1 = Qk1 + delta Qk0,  Q2 = Qk2 + 2 delta Qk1 + delta^2 Qk0.
 * </pre>
 * Within a time step the force is therefore linear in the length {@code l},
 * and {@code computeDFdl()} returns its exact derivative {@code Gamma0
 * Qk0}. Because the kinetics are explicit, the step size should be small
 * compared with the inverse of the largest detachment rate. The default rate
 * constants work well with step sizes of 1 msec or less.
 *
 * <p>The state is committed using the activation supplied at the first force
 * evaluation of each step. Additional evaluations, such as those made between
 * steps for rendering, or by controllers probing the response to other
 * excitations, therefore do not alter the simulation results.
 *
 * <p>The muscle is assumed to be tendonless (or equivalently, to have a rigid
 * tendon), so that the fibre length change equals the change in the overall
 * length. A compliant tendon can be added by connecting the muscle in series
 * with a tendon material (such as {@link Millard2012AxialTendon}) using an
 * intermediate point. Force-length effects are not currently modeled.
 *
 * <p>If the material's state has not been explicitly set using {@link
 * #setIsometricSteadyState}, it is initialized to the isometric steady state
 * corresponding to the excitation supplied at the first force evaluation.
 *
 * <p>The default parameter values assume SI units, and give a maximum
 * isometric stress of approximately 34.5 N/cm^2.
 *
 * <p>References:
 * <ul>
 * <li>Huxley AF (1957). Muscle structure and theories of contraction.
 * <i>Progress in Biophysics and Biophysical Chemistry</i>, 7:255-318.
 * <li>Zahalak GI (1981). A distribution-moment approximation for kinetic
 * theories of muscular contraction. <i>Mathematical Biosciences</i>,
 * 55:89-114.
 * </ul>
 */
public class HuxleyAxialMuscle extends AxialMuscleMaterialBase
   implements HasNumericState {

   // Smallest value of Q0 for which the distribution is considered non-empty
   protected static double Q0_MIN = 1e-10;
   // Smallest half-width allowed for the uniform distribution
   protected static double DELTA_MIN = 1e-6;

   // Fibre length at which sarcomeres are at their reference length
   public static double DEFAULT_OPT_FIBRE_LENGTH = 0.2;
   protected double myOptFibreLength = DEFAULT_OPT_FIBRE_LENGTH;

   // Physiological cross-sectional area
   public static double DEFAULT_CROSS_SECTIONAL_AREA = 1.5e-3;
   protected double myCrossSectionalArea = DEFAULT_CROSS_SECTIONAL_AREA;

   // Linear damping coefficient
   public static double DEFAULT_DAMPING = 0;
   protected double myDamping = DEFAULT_DAMPING;

   // Attachment rate constant f1
   public static double DEFAULT_ATTACHMENT_RATE = 15.0;
   protected double myF1 = DEFAULT_ATTACHMENT_RATE;

   // Detachment rate constant g1, for 0 <= xi <= 1
   public static double DEFAULT_DETACHMENT_RATE = 8.0;
   protected double myG1 = DEFAULT_DETACHMENT_RATE;

   // Detachment rate g0 for compressed cross-bridges (xi < 0)
   public static double DEFAULT_COMPRESSED_DETACHMENT_RATE = 170.0;
   protected double myG0 = DEFAULT_COMPRESSED_DETACHMENT_RATE;

   // Detachment rate slope g2 for over-extended cross-bridges (xi > 1)
   public static double DEFAULT_EXTENDED_DETACHMENT_RATE = 25.0;
   protected double myG2 = DEFAULT_EXTENDED_DETACHMENT_RATE;

   // Sarcomere length s
   public static double DEFAULT_SARCOMERE_LENGTH = 2.2e-6;
   protected double mySarcomereLength = DEFAULT_SARCOMERE_LENGTH;

   // Attachment range h (maximum cross-bridge attachment displacement)
   public static double DEFAULT_ATTACHMENT_RANGE = 5e-9;
   protected double myAttachmentRange = DEFAULT_ATTACHMENT_RANGE;

   // Distance ell between actin binding sites
   public static double DEFAULT_ACTIN_SITE_SPACING = 36e-9;
   protected double myActinSiteSpacing = DEFAULT_ACTIN_SITE_SPACING;

   // Cross-bridge stiffness k
   public static double DEFAULT_CROSS_BRIDGE_STIFFNESS = 5e-3;
   protected double myCrossBridgeStiffness = DEFAULT_CROSS_BRIDGE_STIFFNESS;

   // Number of cross-bridges per unit volume m. The default corresponds to a
   // myosin concentration of 0.46 mmol/L.
   public static double DEFAULT_CROSS_BRIDGE_DENSITY = 4.6e-4*6.022e23*1e3;
   protected double myCrossBridgeDensity = DEFAULT_CROSS_BRIDGE_DENSITY;

   // Derived quantities, updated by updateDerivedValues():
   protected double myGamma1; // force per unit Q1
   protected double myGamma0; // stiffness per unit Q0
   protected double myGamma;  // fibre length change per unit xi
   protected double myVarphi; // f1/(f1+g1)

   // State:

   // Moments at the start of the current step, referenced to myLengthStart
   protected double[] myQStart = new double[3];
   // Length associated with myQStart
   protected double myLengthStart;
   // Start moments advanced by the kinetic terms over the current step
   protected double[] myQKin = new double[3];
   // Current moments, at the length of the most recent evaluation
   protected double[] myQ = new double[3];
   // Current step size; 0 before the first step
   protected double myH;
   // Activation used for the kinetic update of the current step. This is the
   // activation at the first evaluation of the step, so that evaluations made
   // with other activations (such as those used by controllers to probe the
   // excitation response) do not alter the state.
   protected double myStepActivation;
   // True if the moments have been initialized
   protected boolean myMomentsValid = false;
   // True if myLengthStart has been set
   protected boolean myLengthStartValid = false;
   // True if a step has been started but the start moments have not yet been
   // shifted to the length at the start of the step
   protected boolean myStepStartPending = false;

   // Workspace for the kinetic rates and partial moments
   private double[] myDQ = new double[3];
   private double[] myJneg = new double[4];
   private double[] myJmid = new double[4];
   private double[] myJext = new double[4];

   public static PropertyList myProps =
      new PropertyList (HuxleyAxialMuscle.class, AxialMuscleMaterialBase.class);

   static {
      myProps.add (
         "optFibreLength",
         "fibre length at which the sarcomeres have their reference length",
         DEFAULT_OPT_FIBRE_LENGTH);
      myProps.add (
         "crossSectionalArea",
         "physiological cross-sectional area",
         DEFAULT_CROSS_SECTIONAL_AREA);
      myProps.add (
         "damping", "linear damping coefficient", DEFAULT_DAMPING);
      myProps.add (
         "attachmentRate",
         "cross-bridge attachment rate constant f1", DEFAULT_ATTACHMENT_RATE);
      myProps.add (
         "detachmentRate",
         "cross-bridge detachment rate constant g1", DEFAULT_DETACHMENT_RATE);
      myProps.add (
         "compressedDetachmentRate",
         "detachment rate g0 for compressed cross-bridges",
         DEFAULT_COMPRESSED_DETACHMENT_RATE);
      myProps.add (
         "extendedDetachmentRate",
         "detachment rate slope g2 for over-extended cross-bridges",
         DEFAULT_EXTENDED_DETACHMENT_RATE);
      myProps.add (
         "sarcomereLength", "sarcomere length", DEFAULT_SARCOMERE_LENGTH);
      myProps.add (
         "attachmentRange",
         "maximum displacement h at which cross-bridges attach",
         DEFAULT_ATTACHMENT_RANGE);
      myProps.add (
         "actinSiteSpacing",
         "distance between actin binding sites", DEFAULT_ACTIN_SITE_SPACING);
      myProps.add (
         "crossBridgeStiffness",
         "stiffness of a single cross-bridge", DEFAULT_CROSS_BRIDGE_STIFFNESS);
      myProps.add (
         "crossBridgeDensity",
         "number of cross-bridges per unit volume",
         DEFAULT_CROSS_BRIDGE_DENSITY);
      myProps.addReadOnly (
         "maxIsoForce", "maximum isometric force");
      myProps.addReadOnly (
         "maxIsoStiffness", "short-range stiffness at maximum activation");
      myProps.addReadOnly (
         "stiffness", "current short-range stiffness");
      myProps.addReadOnly (
         "moments", "current distribution moments Q0, Q1, Q2");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new HuxleyAxialMuscle with default parameter values.
    */
   public HuxleyAxialMuscle() {
      updateDerivedValues();
   }

   /**
    * Creates a new HuxleyAxialMuscle with a specified fibre length and
    * cross-sectional area, and other parameters set to their defaults.
    *
    * @param optFibreLength fibre length at which the sarcomeres have their
    * reference length
    * @param area physiological cross-sectional area
    */
   public HuxleyAxialMuscle (double optFibreLength, double area) {
      this();
      setOptFibreLength (optFibreLength);
      setCrossSectionalArea (area);
   }

   /**
    * Creates a new HuxleyAxialMuscle with a specified fibre length,
    * cross-sectional area and damping, and other parameters set to their
    * defaults.
    *
    * @param optFibreLength fibre length at which the sarcomeres have their
    * reference length
    * @param area physiological cross-sectional area
    * @param damping linear damping coefficient
    */
   public HuxleyAxialMuscle (
      double optFibreLength, double area, double damping) {
      this (optFibreLength, area);
      setDamping (damping);
   }

   // property accessors

   public double getOptFibreLength() {
      return myOptFibreLength;
   }

   public void setOptFibreLength (double len) {
      myOptFibreLength = len;
      updateDerivedValues();
   }

   public double getCrossSectionalArea() {
      return myCrossSectionalArea;
   }

   public void setCrossSectionalArea (double area) {
      myCrossSectionalArea = area;
      updateDerivedValues();
   }

   public double getDamping() {
      return myDamping;
   }

   public void setDamping (double d) {
      myDamping = d;
      notifyHostOfPropertyChange();
   }

   public double getAttachmentRate() {
      return myF1;
   }

   public void setAttachmentRate (double f1) {
      myF1 = f1;
      updateDerivedValues();
   }

   public double getDetachmentRate() {
      return myG1;
   }

   public void setDetachmentRate (double g1) {
      myG1 = g1;
      updateDerivedValues();
   }

   public double getCompressedDetachmentRate() {
      return myG0;
   }

   public void setCompressedDetachmentRate (double g0) {
      myG0 = g0;
   }

   public double getExtendedDetachmentRate() {
      return myG2;
   }

   public void setExtendedDetachmentRate (double g2) {
      myG2 = g2;
   }

   public double getSarcomereLength() {
      return mySarcomereLength;
   }

   public void setSarcomereLength (double s) {
      mySarcomereLength = s;
      updateDerivedValues();
   }

   public double getAttachmentRange() {
      return myAttachmentRange;
   }

   public void setAttachmentRange (double h) {
      myAttachmentRange = h;
      updateDerivedValues();
   }

   public double getActinSiteSpacing() {
      return myActinSiteSpacing;
   }

   public void setActinSiteSpacing (double ell) {
      myActinSiteSpacing = ell;
      updateDerivedValues();
   }

   public double getCrossBridgeStiffness() {
      return myCrossBridgeStiffness;
   }

   public void setCrossBridgeStiffness (double k) {
      myCrossBridgeStiffness = k;
      updateDerivedValues();
   }

   public double getCrossBridgeDensity() {
      return myCrossBridgeDensity;
   }

   public void setCrossBridgeDensity (double m) {
      myCrossBridgeDensity = m;
      updateDerivedValues();
   }

   /**
    * Returns the maximum isometric force, which is the force at the isometric
    * steady state for an activation of 1.
    *
    * @return maximum isometric force
    */
   public double getMaxIsoForce() {
      return myGamma1*myVarphi/2;
   }

   /**
    * Returns the short-range stiffness at the isometric steady state for an
    * activation of 1.
    *
    * @return maximum isometric short-range stiffness
    */
   public double getMaxIsoStiffness() {
      return myGamma0*myVarphi;
   }

   /**
    * Returns the current short-range stiffness {@code Gamma0 Q0}.
    *
    * @return current short-range stiffness
    */
   public double getStiffness() {
      return myGamma0*myQ[0];
   }

   /**
    * Returns the current distribution moments {@code Q0}, {@code Q1} and
    * {@code Q2}. {@code Q0} gives the fraction of attached cross-bridges.
    *
    * @return current distribution moments
    */
   public Vector3d getMoments() {
      return new Vector3d (myQ[0], myQ[1], myQ[2]);
   }

   /**
    * Returns the constant {@code Gamma1} relating the moment {@code Q1} to
    * the muscle force.
    *
    * @return force per unit {@code Q1}
    */
   public double getForceCoefficient() {
      return myGamma1;
   }

   /**
    * Returns the constant {@code Gamma0} relating the moment {@code Q0} to
    * the short-range stiffness.
    *
    * @return stiffness per unit {@code Q0}
    */
   public double getStiffnessCoefficient() {
      return myGamma0;
   }

   /**
    * Returns the fibre length change {@code gamma} corresponding to a unit
    * change in the normalized cross-bridge strain {@code xi}.
    *
    * @return fibre length change per unit {@code xi}
    */
   public double getStrainScale() {
      return myGamma;
   }

   protected void updateDerivedValues() {
      double A = myCrossSectionalArea;
      double m = myCrossBridgeDensity;
      double s = mySarcomereLength;
      double k = myCrossBridgeStiffness;
      double h = myAttachmentRange;
      double ell = myActinSiteSpacing;
      double X = myOptFibreLength;

      myGamma1 = A*m*s*k*h*h/(2*ell);
      myGamma = 2*X*h/s;
      myGamma0 = myGamma1/myGamma;
      myVarphi = myF1/(myF1+myG1);
   }

   // state initialization

   /**
    * Sets the moments of this material to the isometric steady state for a
    * given activation level {@code a}. The resulting cross-bridge
    * distribution is uniform on {@code [0, 1]} with height {@code a
    * f1/(f1+g1)}. The length associated with this state is taken to be the
    * length supplied at the next force evaluation.
    *
    * @param a activation level
    */
   public void setIsometricSteadyState (double a) {
      computeIsometricMoments (myQStart, a);
      copy (myQKin, myQStart);
      copy (myQ, myQStart);
      myMomentsValid = true;
      myLengthStartValid = false;
      // no kinetic update for the remainder of the current step
      myH = 0;
   }

   /**
    * Clears the state of this material, so that it will be reinitialized to
    * the isometric steady state at the next force evaluation.
    */
   public void clearState() {
      myMomentsValid = false;
      myLengthStartValid = false;
      myStepStartPending = false;
      myH = 0;
   }

   /**
    * Computes the isometric steady-state moments for a given activation.
    *
    * @param Q returns the moments
    * @param a activation level
    */
   public void computeIsometricMoments (double[] Q, double a) {
      double N = a*myVarphi;
      Q[0] = N;
      Q[1] = N/2;
      Q[2] = N/3;
   }

   // core computations

   private static void copy (double[] dst, double[] src) {
      dst[0] = src[0];
      dst[1] = src[1];
      dst[2] = src[2];
   }

   /**
    * Shifts the moments {@code Q} to account for all cross-bridges being
    * displaced by {@code delta}.
    */
   private static void shiftMoments (
      double[] Qr, double[] Q, double delta) {
      double q0 = Q[0];
      double q1 = Q[1];
      double q2 = Q[2];
      Qr[0] = q0;
      Qr[1] = q1 + delta*q0;
      Qr[2] = q2 + delta*(2*q1 + delta*q0);
   }

   /**
    * Computes the partial moments
    * <pre>
    *   J_k = integral_{a}^{b} xi^k n(xi) dxi,   k = 0, ... 3
    * </pre>
    * for a uniform distribution of height {@code N} on {@code [lo, hi]}.
    * {@code (u^(k+1) - w^(k+1))/(k+1)} is evaluated in factored form, as
    * {@code (u-w)} times a polynomial in {@code u} and {@code w}, to reduce
    * cancellation when the interval is narrow.
    */
   static void computePartialMoments (
      double[] J, double N, double lo, double hi, double a, double b) {
      double u = Math.min (b, hi);
      double w = Math.max (a, lo);
      if (u <= w) {
         J[0] = J[1] = J[2] = J[3] = 0;
      }
      else {
         double nd = N*(u-w);
         double uu = u*u;
         double ww = w*w;
         J[0] = nd;
         J[1] = nd*(u+w)/2;
         J[2] = nd*(uu+u*w+ww)/3;
         J[3] = nd*(uu*u+uu*w+u*ww+ww*w)/4;
      }
   }

   /**
    * Computes the kinetic contributions {@code a beta_k - phi_k - psi_k} to
    * the time derivatives of the moments {@code Q_k}, for {@code k = 0, 1,
    * 2}. The convection term {@code -k u Q_(k-1)} is not included.
    *
    * @param dQ returns the kinetic rates
    * @param Q current moments
    * @param a activation level
    */
   public void computeKineticRates (double[] dQ, double[] Q, double a) {
      double f1 = myF1;
      for (int k=0; k<3; k++) {
         // beta_k = f1/(k+2)
         dQ[k] = a*f1/(k+2);
      }
      double q0 = Q[0];
      if (q0 > Q0_MIN) {
         double xi0 = Q[1]/q0;
         double var = Q[2]/q0 - xi0*xi0;
         double delta = Math.sqrt (Math.max (3*var, DELTA_MIN*DELTA_MIN));
         double N = q0/(2*delta);
         double lo = xi0-delta;
         double hi = xi0+delta;
         double inf = Double.POSITIVE_INFINITY;
         computePartialMoments (myJneg, N, lo, hi, -inf, 0);
         computePartialMoments (myJmid, N, lo, hi, 0, 1);
         computePartialMoments (myJext, N, lo, hi, 1, inf);
         double g0 = myG0;
         double g1 = myG1;
         double g2 = myG2;
         for (int k=0; k<3; k++) {
            // phi_k + psi_k
            double loss =
               (f1+g1)*myJmid[k+1] + g0*myJneg[k] +
               g2*myJext[k+1] + (g1-g2)*myJext[k];
            dQ[k] -= loss;
         }
      }
   }

   /**
    * Advances the moments {@code Q} by the kinetic terms over a time {@code
    * h}, using a forward Euler step, and places the result in {@code Qr}.
    * {@code Qr} and {@code Q} may be the same array.
    */
   private void advanceKinetics (double[] Qr, double[] Q, double h, double a) {
      computeKineticRates (myDQ, Q, a);
      for (int k=0; k<3; k++) {
         Qr[k] = Q[k] + h*myDQ[k];
      }
      if (Qr[0] <= 0) {
         // all cross-bridges have detached
         Qr[0] = Qr[1] = Qr[2] = 0;
      }
   }

   /**
    * Updates the moments for the current step at length {@code l} and
    * activation {@code a}. The kinetic update is computed from the
    * start-of-step moments, and the result is then shifted by the change in
    * length since the start of the step.
    */
   protected void updateMoments (double l, double a) {
      if (!myMomentsValid) {
         computeIsometricMoments (myQStart, a);
         myMomentsValid = true;
      }
      if (!myLengthStartValid) {
         myLengthStart = l;
         myLengthStartValid = true;
      }
      if (myStepStartPending) {
         // first evaluation of this step: l is the length at the start of the
         // step, so shift the start moments to account for the length change
         // that occurred during the previous step
         shiftMoments (myQStart, myQStart, (l-myLengthStart)/myGamma);
         myLengthStart = l;
         myStepActivation = a;
         myStepStartPending = false;
      }
      advanceKinetics (myQKin, myQStart, myH, a);
      shiftMoments (myQ, myQKin, (l-myLengthStart)/myGamma);
   }

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {
      updateMoments (l, excitation);
      return myGamma1*myQ[1] + myDamping*ldot;
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {
      updateMoments (l, excitation);
      // Q0 is not changed by the shift, and so dQ1/dl = Q0/gamma
      return myGamma0*myQKin[0];
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {
      return myDamping;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDFdldotZero() {
      return myDamping == 0;
   }

   // implementation of HasNumericState

   /**
    * {@inheritDoc}
    */
   public boolean hasState() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean requiresAdvance() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void advanceState (double t0, double t1) {
      if (myMomentsValid && !myStepStartPending) {
         // apply the kinetic update for the previous step. This is recomputed,
         // rather than taken from myQKin, since myQKin may reflect evaluations
         // made with other activations. The shift for the length change is
         // applied at the first evaluation of the new step.
         advanceKinetics (myQStart, myQStart, myH, myStepActivation);
      }
      myH = t1 - t0;
      myStepStartPending = true;
   }

   /**
    * {@inheritDoc}
    */
   public void getState (DataBuffer data) {
      data.zputBool (myMomentsValid);
      data.zputBool (myLengthStartValid);
      data.zputBool (myStepStartPending);
      for (int k=0; k<3; k++) {
         data.dput (myQStart[k]);
      }
      for (int k=0; k<3; k++) {
         data.dput (myQKin[k]);
      }
      for (int k=0; k<3; k++) {
         data.dput (myQ[k]);
      }
      data.dput (myLengthStart);
      data.dput (myH);
      data.dput (myStepActivation);
   }

   /**
    * {@inheritDoc}
    */
   public void setState (DataBuffer data) {
      myMomentsValid = data.zgetBool();
      myLengthStartValid = data.zgetBool();
      myStepStartPending = data.zgetBool();
      for (int k=0; k<3; k++) {
         myQStart[k] = data.dget();
      }
      for (int k=0; k<3; k++) {
         myQKin[k] = data.dget();
      }
      for (int k=0; k<3; k++) {
         myQ[k] = data.dget();
      }
      myLengthStart = data.dget();
      myH = data.dget();
      myStepActivation = data.dget();
   }

   // other material methods

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof HuxleyAxialMuscle)) {
         return false;
      }
      HuxleyAxialMuscle hmat = (HuxleyAxialMuscle)mat;
      if (myOptFibreLength != hmat.myOptFibreLength ||
          myCrossSectionalArea != hmat.myCrossSectionalArea ||
          myDamping != hmat.myDamping ||
          myF1 != hmat.myF1 ||
          myG0 != hmat.myG0 ||
          myG1 != hmat.myG1 ||
          myG2 != hmat.myG2 ||
          mySarcomereLength != hmat.mySarcomereLength ||
          myAttachmentRange != hmat.myAttachmentRange ||
          myActinSiteSpacing != hmat.myActinSiteSpacing ||
          myCrossBridgeStiffness != hmat.myCrossBridgeStiffness ||
          myCrossBridgeDensity != hmat.myCrossBridgeDensity) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public HuxleyAxialMuscle clone() {
      HuxleyAxialMuscle mat = (HuxleyAxialMuscle)super.clone();
      mat.myQStart = myQStart.clone();
      mat.myQKin = myQKin.clone();
      mat.myQ = myQ.clone();
      mat.myDQ = new double[3];
      mat.myJneg = new double[4];
      mat.myJmid = new double[4];
      mat.myJext = new double[4];
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myOptFibreLength *= s;
      myCrossSectionalArea *= (s*s);
      mySarcomereLength *= s;
      myAttachmentRange *= s;
      myActinSiteSpacing *= s;
      myCrossBridgeDensity /= (s*s*s);
      // cross-bridge stiffness and damping (force/length and
      // force/velocity) are unchanged by distance scaling
      myLengthStart *= s;
      updateDerivedValues();
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myCrossBridgeStiffness *= s;
      myDamping *= s;
      updateDerivedValues();
   }
}
