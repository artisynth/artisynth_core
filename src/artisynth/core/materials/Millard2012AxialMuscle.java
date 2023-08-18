package artisynth.core.materials;

import java.io.*;

import artisynth.core.modelbase.HasNumericState;
import maspack.util.DataBuffer;
import maspack.util.*;
import maspack.properties.*;
import maspack.numerics.*;
import maspack.function.*;
import maspack.interpolation.CubicHermiteSpline1d;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

/**
 * Implements the Millard2012EquilibriumMuscle from OpenSim. One should consult
 * the OpenSim API documentation for a detailed explanation of the muscle
 * model's parameters and behavior. The names of this class's muscle parameter
 * properties should match the OpenSim equivalents closely enough to be
 * unambiguous.
 *
 * <p>Equilbrium mus1cle models contain a muscle component and tendon component
 * in series. The muscle and tendon lengths sum to the combined length {@code
 * l} that is supplied the the method {@link #computeF}, which computes the
 * muacle tension, while the equilibrum condition implies that the tension from
 * the muscle and tendon components must both equal the value returned by
 * {@link #computeF}. To implement this, we store the muscle length as a state
 * variable which is updated at each time step to ensure that the equilbrium
 * condition is satisfied. We do this differently from OpenSim: rather than
 * solving for and then integrating the muscle velocity, the velocity inferred
 * by dividing the cnange in muscle length by the time step size.  This avoids
 * the singularity that arises in the OpenSim method at zero activation, and so
 * removes the need for always ensuring that the activation is non-zero.
 *
 * <p>As with the OpenSim model, we also a support, via the {@code rigidTendon}
 * property, a mode where the tendon is considered to be rigid (via the {@code
 * rigidTendon} property), which simplifies the computation in situations where
 * this approximation is acceptable.
 */
public class Millard2012AxialMuscle
   extends EquilibriumAxialMuscle implements HasNumericState {

   // from OpenSim Millard2012EquilibriumMuscle:

   public static double DEFAULT_MAX_PENNATION_ANGLE = Math.acos(0.1);
   protected double myMaxPennationAngle = DEFAULT_MAX_PENNATION_ANGLE;

   public static double DEFAULT_DEFAULT_MIN_FLC_VALUE = 0.0;
   protected static double myDefaultMinFlcValue = DEFAULT_DEFAULT_MIN_FLC_VALUE;

   double myMinCos; // minimum cosine of the pennation angle

   public static PropertyList myProps =
      new PropertyList(Millard2012AxialMuscle.class,
                       EquilibriumAxialMuscle.class);

   // default force curves
   protected static CubicHermiteSpline1d myDefaultActiveForceLengthCurve;
   protected static CubicHermiteSpline1d myDefaultPassiveForceLengthCurve;
   protected static CubicHermiteSpline1d myDefaultForceVelocityCurve;
   protected static CubicHermiteSpline1d myDefaultTendonForceLengthCurve;

   static {
      myProps.add (
         "maxPennationAngle",
         "maximum pennation angle", DEFAULT_MAX_PENNATION_ANGLE);
      myProps.add (
         "muscleLength",
         "length of the muscle section", DEFAULT_MUSCLE_LENGTH);      
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Millard2012AxialMuscle() {
      super();
      setActiveForceLengthCurve (getDefaultActiveForceLengthCurve());
      setPassiveForceLengthCurve (getDefaultPassiveForceLengthCurve());
      setForceVelocityCurve (getDefaultForceVelocityCurve());
      setTendonForceLengthCurve (getDefaultTendonForceLengthCurve());
      
      myMinCos = Math.cos(DEFAULT_MAX_PENNATION_ANGLE);
      myMuscleLength = DEFAULT_MUSCLE_LENGTH;
      myMuscleLengthPrev = DEFAULT_MUSCLE_LENGTH;
      myH = -1;
   }

   public Millard2012AxialMuscle (
      double maxIsoForce, double optFibreLen,
      double tendonSlackLen, double optPennationAng) {
      
      this();
      setMaxIsoForce (maxIsoForce);
      setOptFibreLength (optFibreLen);
      setTendonSlackLength (tendonSlackLen);
      setOptPennationAngle (optPennationAng);
      myMuscleLength = myOptFibreLength*Math.cos(optPennationAng);
      myMuscleLengthPrev = myMuscleLength;
   }

   /**
    * {@inheritDoc}
    */
   protected double computeActiveForceLength (double ln) {
      return myActiveForceLengthCurve.evalY (ln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeDActiveForceLength (double ln) {
      return myActiveForceLengthCurve.evalDy (ln);
   }
   
   /**
    * {@inheritDoc}
    */
   protected double computePassiveForceLength (double ln) {
      return myPassiveForceLengthCurve.evalY (ln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeDPassiveForceLength (double ln) {
      return myPassiveForceLengthCurve.evalDy (ln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeForceVelocity (double vn, double a) {
      if (vn < -1) {
         return 0;
      }
      else {
         return myForceVelocityCurve.evalY (vn);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected double computeDForceVelocity (double vn, double a) {
      if (vn < -1) {
         return 0;
      }
      else {
         return myForceVelocityCurve.evalDy (vn);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected double computeTendonForce (double tln) {
      return myTendonForceLengthCurve.evalY (tln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeDTendonForce (double tln) {
      return myTendonForceLengthCurve.evalDy (tln);
   }

   public static CubicHermiteSpline1d getDefaultActiveForceLengthCurve() {
      if (myDefaultActiveForceLengthCurve == null) {
         myDefaultActiveForceLengthCurve =
            createActiveForceLengthCurve (
               0.4441, 0.73, 1.8123, 0.8616, getDefaultMinFlcValue());
      }
      return myDefaultActiveForceLengthCurve;
   }

   public static CubicHermiteSpline1d getDefaultPassiveForceLengthCurve() {
      if (myDefaultPassiveForceLengthCurve == null) {
         double strainAtZeroForce = 0.0;
         double strainAtOneNormForce = 0.7;
         myDefaultPassiveForceLengthCurve =
            createPassiveForceLengthCurve (
               strainAtZeroForce, strainAtOneNormForce, 0.2,
               2.0/(strainAtOneNormForce-strainAtZeroForce), 0.75);
      }
      return myDefaultPassiveForceLengthCurve;
   }

   public static CubicHermiteSpline1d getDefaultForceVelocityCurve() {
      if (myDefaultForceVelocityCurve == null) {
         myDefaultForceVelocityCurve =
            createForceVelocityCurve (
               0.01, 0.25, 5.0, 0.01, 0.15, 1.4, 0.6, 0.9);
      }
      return myDefaultForceVelocityCurve;
   }

   public static CubicHermiteSpline1d getDefaultTendonForceLengthCurve() {
      if (myDefaultTendonForceLengthCurve == null) {
         double strainAtOneNormForce = 0.049;
         myDefaultTendonForceLengthCurve =
            createTendonForceLengthCurve (
               strainAtOneNormForce, 1.375/strainAtOneNormForce, 2.0/3.0, 0.5);
      }
      return myDefaultTendonForceLengthCurve;
   }

   public double getMaxPennationAngle() {
      return myMaxPennationAngle;
   }

   public void setMaxPennationAngle (double ang) {
      myMaxPennationAngle = ang;
      myMinCos = Math.cos(ang);
   }

   public static double getDefaultMinFlcValue() {
      return myDefaultMinFlcValue;
   }

   public static void setDefaultMinFlcValue (double min) {
      myDefaultMinFlcValue = min;
      myDefaultActiveForceLengthCurve = null;
   }

   static int nsame = 0;

   static int ninterval = 0;
   static int nsolved = 0;
   static int nclippedL = 0;
   static int nclipped0 = 0;
   static int maxiters = 0;
   static int nshort = 0;

   static int nreduce;

   double getMaxTendonStiffness() {
      return
         getTendonForceLengthCurve().getLastKnot().getDy()/myTendonSlackLength;
   }

   double getMaxPassiveStiffness() {
      return getPassiveForceLengthCurve().getLastKnot().getDy()/myOptFibreLength;
   }

   public Millard2012AxialMuscle clone() {
      Millard2012AxialMuscle mat = (Millard2012AxialMuscle)super.clone();
      mat.myLengthValid = false;
      return mat;
   }

   public static void main (String[] args) throws IOException {
      Millard2012AxialMuscle m = new Millard2012AxialMuscle();

      m.writeActiveForceLengthCurve ("MillardAFLC.txt", 0, 2, 400, "%g");
      m.writePassiveForceLengthCurve ("MillardPFLC.txt", 0, 2, 400, "%g");
      m.writeTendonForceLengthCurve ("MillardTFLC.txt", 0.9, 1.1, 100, "%g");
      m.writeForceVelocityCurve ("MillardFVC.txt", 1, -1, 1, 400, "%g");
   }

}
