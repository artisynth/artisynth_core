package artisynth.core.materials;

import java.io.*;

import artisynth.core.modelbase.*;
import maspack.properties.*;
import maspack.function.*;
import maspack.numerics.*;
import maspack.util.*;
import maspack.interpolation.CubicHermiteSpline1d;

/**
 * Implements the Thelen2003Muscle from OpenSim. One should consult the OpenSim
 * API documentation for a detailed explanation of the muscle model's
 * parameters and behavior. The names of this class's muscle parameter
 * properties should match the OpenSim equivalents closely enough to be
 * unambiguous.
 *
 * <p>The force velocity curve has been modified to remove a derivative
 * discontinuity at vn = 0, where vn is the normalized velocity.
 *
 * <p>Equilbrium muscle models contain a muscle component and tendon component
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
 * <p>We also a support, via the {@code rigidTendon} property, a mode where the
 * tendon is considered to be rigid (via the {@code rigidTendon} property),
 * which simplifies the computation in situations where this approximation is
 * acceptable.
 */
public class Thelen2003AxialMuscle
   extends EquilibriumAxialMuscle implements HasNumericState {
   
   // from Thelan paper:

   /*
     
    Active force
    ------------
 
    f_l = exp(-(LM-1)^2/gamma)

    where 

    f_l = active force-length scale factor (force divided by a and maxIsoForce)
    LM = normalized muscle fibre length (length divided by myOptFibreLength)
    gamma = shape factor.

    Passive force:
    --------------

              exp(kPE*(LM-1)/eps0M - 1
      F_PE =---------------------------------
              exp(kPE) - 1

    where

    F_PE = normalized passive muscle force (force divided by maxIsoForce)
    kPE =  an exponential shape factor (default 5)
    eps0M = passive muscle strain due to maximum isometric force 
           (defaults: 0.6 young, 0.5 old)

    Tendon force-strain:
    --------------------
 
            FT_toe
    FT = ---------------- ( exp (k_toe epsT/epsT_toe) - 1)   epsT <= epsT_toe
           exp (k_toe) - 1

    FT = k_lin (epsT - epsT_toe) + FT_toe                    epsT > epsT_toe


    where

    FT = tendon force normalized to maximum isometric force,
    epsT = tendon strain
    epsT_toe = strain above which the tendon exhibits linear behavior,
    k_toe = exponential shape factor (default value 3)
    k_lin = linear scale factor

    FT_toe is ???
    
    Force velocity:
    ---------------
                                 FM - a f_l
    VM = (0.25 + 0.75 a) VM_max ------------
                                     b

    b = a f_l + FM /Af                    FM <= a f_l

        (2 + 2/Af)(a f_l FM_len - FM)
    b = ------------------------------     FM > a f_l
                (FM_len - 1)

    where:

    a is the activation
    FM is the active muscle force
    VM_max is the maximum contraction velocity
    f_l is the active force-length scale factor from above

    FM_len = ratio of maximum lengthing muscle muscle force to isometric force
    (defaults: 1.4 young, 1.8 old)

    Af is a force velocity shape factor (default value 0.25)

    Parameter summary in OpenSim:

    FmaxTendonStrain - tendon strain at max isometric muscle force
    FmaxMuscleStrain - passive muscle strain at max isometric muscl force (eps0M)
    KshapeActive - shape factor active force  (gamma above)
    KshapePassive - exponential shape factor for passive force (kPE above)
    Af - force velocity shape factor (Af above)
    Flen - maximum normalized lengthening force (FM_len above)
    fv_linear_extrap_threshold - fv_threshold where linear extrpolation is used
    maximum_pennation_angle
    minimum_activation
    activation_time_constant - for muscle dynamics
    deactivation_time_constant - for muscle dynamics    
   */

   // Thelen2003 specific properties

   // tendon strain at maximum isometric muscle force
   public static double DEFAULT_FMAX_TENDON_STRAIN = 0.04;
   protected double myFmaxTendonStrain = DEFAULT_FMAX_TENDON_STRAIN;

   // passive muscle strain at maximum isometric muscle force
   public static double DEFAULT_FMAX_MUSCLE_STRAIN = 0.6;
   protected double myFmaxMuscleStrain = DEFAULT_FMAX_MUSCLE_STRAIN;

   // shape factor for Gaussian active muscle force-length relationship
   public static double DEFAULT_K_SHAPE_ACTIVE = 0.45;
   protected double myKShapeActive = DEFAULT_K_SHAPE_ACTIVE;

   // exponential shape factor for passive force-length relationship
   public static double DEFAULT_K_SHAPE_PASSIVE = 5.0;
   protected double myKShapePassive = DEFAULT_K_SHAPE_PASSIVE;

   // force-velocity shape factor
   public static double DEFAULT_AF = 0.25;
   protected double myAf = DEFAULT_AF;

   // maximum normalized lengthening force
   public static double DEFAULT_FLEN = 1.4;
   protected double myFlen = DEFAULT_FLEN;

   // fv threshold where beyond which linear extrapolation is used
   public static double DEFAULT_FV_LINEAR_EXTRAP_THRESHOLD = 0.99;
   protected double myFvLinearExtrapThreshold =
      DEFAULT_FV_LINEAR_EXTRAP_THRESHOLD;

   // maximum pennation angle, in radians. Used by OpenSim, but not needed here
   // because of the manner in which we solve for muscle equilibrium.
   public static double DEFAULT_MAX_PENNATION_ANGLE = Math.acos(0.1);
   protected double myMaxPennationAngle = DEFAULT_MAX_PENNATION_ANGLE;

   // lower bound on activation. Used by OpenSim, but not needed here because
   // of the manner in which we solve for muscle equilibrium.
   public static double DEFAULT_MINIMUM_ACTIVATION = 0.01;
   protected double myMinimumActivation = DEFAULT_MINIMUM_ACTIVATION;

   double myFibreLength;
   double myMinCos; // minimun cosine of the pennation angle

   // fixed tendon parameters
   double myKToe = 3.0;
   double myExpKToe = Math.exp(myKToe);
   double myFToe = 0.33;
   double myEToe; // computed from FmaxTendonStrain
   double myKlin; // computed from FmaxTendonStrain

   public static PropertyList myProps =
      new PropertyList(Thelen2003AxialMuscle.class,
                       EquilibriumAxialMuscle.class);

   static {
      myProps.add (
         "fmaxTendonStrain",
         "tendon strain at max isometric muscle force",
         DEFAULT_FMAX_TENDON_STRAIN);
      myProps.add (
         "fmaxMuscleStrain",
         "passive muscle strain at max isometric muscle force",
         DEFAULT_FMAX_MUSCLE_STRAIN);
      myProps.add (
         "kShapeActive",
         "shape factor for the active force curve",
         DEFAULT_K_SHAPE_ACTIVE);
      myProps.add (
         "kShapePassive",
         "shape factor for the passive force curve",
         DEFAULT_K_SHAPE_PASSIVE);
      myProps.add (
         "af", "force velocity shape factor", DEFAULT_AF);
      myProps.add (
         "flen", "maximum normalized lengthening force", DEFAULT_FLEN);
      myProps.add (
         "fvLinearExtrapThreshold", 
         "force velocity threshold where linear extrapolation is used",
         DEFAULT_FV_LINEAR_EXTRAP_THRESHOLD);
      // myProps.add (
      //    "maxPennationAngle",
      //    "maximum pennation angle", DEFAULT_MAX_PENNATION_ANGLE);
      // myProps.add (
      //    "minimumActivation",
      //    "minimum activation value (to prevent equilibrium singularities)",
      //    DEFAULT_MINIMUM_ACTIVATION);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Thelen2003AxialMuscle() {
      super();
      myHeight = DEFAULT_OPT_FIBRE_LENGTH*Math.sin(DEFAULT_OPT_PENNATION_ANGLE);
      myMinCos = Math.cos(DEFAULT_MAX_PENNATION_ANGLE);
      setFmaxTendonStrain (DEFAULT_FMAX_TENDON_STRAIN);

      myMuscleLength = DEFAULT_MUSCLE_LENGTH;
      myMuscleLengthPrev = DEFAULT_MUSCLE_LENGTH;
      myH = -1;
   }

   public Thelen2003AxialMuscle (
      double fmax, double lopt,
      double tslack, double optPenAng) {
      
      this();
      setMaxIsoForce (fmax);
      setOptFibreLength (lopt);
      setTendonSlackLength (tslack);
      setOptPennationAngle (optPenAng);
   }

   // Thelen2003 specific properties

   public double getFmaxTendonStrain() {
      return myFmaxTendonStrain;
   }

   public void setFmaxTendonStrain (double fts) {
      myFmaxTendonStrain = fts;
      updateTendonConstants();
   }

   public double getFmaxMuscleStrain() {
      return myFmaxMuscleStrain;
   }

   public void setFmaxMuscleStrain (double fts) {
      myFmaxMuscleStrain = fts;
   }

   public double getKShapeActive() {
      return myKShapeActive;
   }

   public void setKShapeActive (double kshape) {
      myKShapeActive = kshape;
   }

   public double getKShapePassive() {
      return myKShapePassive;
   }

   public void setKShapePassive (double kshape) {
      myKShapePassive = kshape;
   }

   public double getAf() {
      return myAf;
   }

   public void setAf (double af) {
      myAf = af;
   }

   public double getFlen() {
      return myFlen;
   }

   public void setFlen (double flen) {
      myFlen = flen;
   }

   public double getFvLinearExtrapThreshold() {
      return myFvLinearExtrapThreshold;
   }

   public void setFvLinearExtrapThreshold (double thresh) {
      myFvLinearExtrapThreshold = thresh;
   }

   public double getMaxPennationAngle() {
      return myMaxPennationAngle;
   }

   public void setMaxPennationAngle (double ang) {
      myMaxPennationAngle = ang;
      myMinCos = Math.cos(ang);
   }

   public double getMinimumActivation() {
      return myMinimumActivation;
   }

   public void setMinimumActivation (double mina) {
      myMinimumActivation = mina;
   }

   private void updateTendonConstants() {
      double e0 = getFmaxTendonStrain();

      double exp3   = Math.exp(3.0);
      myEToe = (99.0*e0*exp3) / (166.0*exp3 - 67.0);
      myKlin = 0.67/(e0 - myEToe);
   }

   /**
    * Compute normalized tendon force from normalized tendon length.  Adapted
    * from OpenSim code.
    */
   protected double computeTendonForce (double tln) {
      double x = tln-1;

      //Compute tendon force
      double fse = 0;
      if (x > myEToe) {
         fse = myKlin*(x-myEToe)+myFToe;
      }
      else if (x > 0.0) { 
         fse = (myFToe/(myExpKToe-1.0))*(Math.exp(myKToe*x/myEToe)-1.0);
      }
      else {
         fse = 0.0;
      }
      return fse;    
   }

   /**
    * Compute derivative of normalized tendon force from normalized tendon
    * length.  Adapted from OpenSim code.
    */
   protected double computeTendonForceDeriv (double tln) {
      double x = tln-1;

      //Compute tendon force
      double dft = 0;
      if (x > myEToe) {
         dft = myKlin;
      }
      else if (x >= 0.0) {
         dft = 
            (myFToe/(myExpKToe-1.0)) *
            (myKToe/myEToe) * (Math.exp(myKToe*x/myEToe));
      }
      else{
         dft = 0;
      }

      return dft;
   }

   /**
    * Computes the active force length curve.
    */
   protected double computeActiveForceLength (double ln) {
      return Math.exp(-sqr(ln-1.0)/myKShapeActive);
   }

   /**
    * Computes the derivate of the active force length curve.
    */
   protected double computeActiveForceLengthDeriv (double ln) {
      double x = ln - 1.0;
      return -2*x/myKShapeActive * Math.exp(-x*x/myKShapeActive);
   }

   /**
    * Computes the passive force length curve. Adapated from OpenSim code.
    */
   protected double computePassiveForceLength (double ln) {
      double fpe = 0;
      double e0 = myFmaxMuscleStrain;
      double kpe = myKShapePassive;
      
      //Compute the passive force developed by the muscle
      if (ln >= 1.0) {
         double t5 = Math.exp(kpe * (ln - 0.10e1) / e0);
         double t7 = Math.exp(kpe);
         fpe = (t5 - 0.10e1) / (t7 - 0.10e1);
      }
      return fpe;
   }

   /**
    * Computes the derivative of the passive force length curve. Adapated from
    * OpenSim code.
    */
   protected double computePassiveForceLengthDeriv (double ln) {
      double dfpe = 0;
      double e0 = myFmaxMuscleStrain;
      double kpe = myKShapePassive;

      if(ln >= 1.0){
         double t1 = 0.1e1 / e0;
         double t6 = Math.exp(kpe * (ln - 0.10e1) * t1);
         double t7 = Math.exp(kpe);
         dfpe = kpe * t1 * t6 / (t7 - 0.10e1);
      }
      return dfpe;
   }

   // ====== force velocity curves ======

   /**
    * Computes the force velocity curve without any linear extrapolation at the
    * ends.
    */
   double computeForceVelocityRaw (double vn, double a) {
      double alpha = 0.25 + 0.75*a;
      double fv;
      if (vn < 0) {
         fv = (alpha + vn)/(alpha - vn/myAf);
      }
      else {
         double beta = alpha*(myFlen-1)/(1+1/myAf); // 2 
         fv = (beta + vn*myFlen)/(beta + vn);
      }
      return fv;
   }         
 
   /**
    * Computes the derivative of the force velocity curve without any linear
    * extrapolation at the ends.
    */
   double computeDForceVelocityRaw (double vn, double a) {
      double alpha = 0.25 + 0.75*a;
      double dfv;
      if (vn <= 0) {
         dfv = alpha*(1 + 1/myAf)/sqr(alpha -vn/myAf);
      }
      else {
         double beta = alpha*(myFlen-1)/(1+1/myAf); // 2
         dfv = beta*(myFlen-1)/sqr(beta + vn);
      }
      return dfv;
   }

   /**
    * Computes the force velocity curve, using linear extrapolation
    * for the zone where fv {@code > Flen*FvLinearExtrapThreshold},
    * and not allowing fv to become negative.
    */
   protected double computeForceVelocity (double vn, double a) {
      double alpha = 0.25 + 0.75*a;
      double beta = alpha*(myFlen-1)/(1+1/myAf); // 2 
      double fvt = myFlen*myFvLinearExtrapThreshold;
      double vThreshLo = -alpha;
      double vThreshHi = beta*(fvt-1)/(myFlen-fvt); // 2
      if (vn < vThreshLo) {
         //double dfvt = computeDForceVelocityRaw (vThreshLo, a);
         return 0;
      }
      else if (vn > vThreshHi) {
         double dfvt = computeDForceVelocityRaw (vThreshHi, a);
         return fvt + dfvt*(vn-vThreshHi);
      }
      else {
         return computeForceVelocityRaw (vn, a);
      }
   }         

   /**
    * Computes the derivative of the force velocity curve, for which linear
    * extrapolation is used in the zone where fv {@code >
    * Flen*FvLinearExtrapThreshold}, and where fv is not allowed to become
    * negative.
    */
   protected double computeForceVelocityDeriv (double vn, double a) {
      double alpha = 0.25 + 0.75*a;
      double fvt = myFlen*myFvLinearExtrapThreshold;
      double vThreshLo = -alpha;
      double vThreshHi =
         alpha*(fvt-1)*(myFlen-1)/((1+1/myAf)*(myFlen-fvt)); // 2
      if (vn < vThreshLo) {
         return 0;
      }
      else if (vn > vThreshHi) {
         return computeDForceVelocityRaw (vThreshHi, a);
      }
      else {
         return computeDForceVelocityRaw (vn, a);
      }
   }

   public Thelen2003AxialMuscle clone() {
      Thelen2003AxialMuscle mat = (Thelen2003AxialMuscle)super.clone();
      mat.myLengthValid = false;
      return mat;
   }

   public static void main (String[] args) throws IOException {
      Thelen2003AxialMuscle m = new Thelen2003AxialMuscle();

      m.writeActiveForceLengthCurve ("ThelenAFLC.txt", 0, 2, 400, "%g");
      m.writePassiveForceLengthCurve ("ThelenPFLC.txt", 0, 2, 400, "%g");
      m.writeTendonForceLengthCurve ("ThelenTFLC.txt", 0.9, 1.1, 100, "%g");
      m.writeForceVelocityCurve ("ThelenFVC_1.txt", 1, -1, 1, 400, "%g");
      m.writeForceVelocityCurve ("ThelenFVC_h.txt", 0.5, -1, 1, 400, "%g");
   }
}
