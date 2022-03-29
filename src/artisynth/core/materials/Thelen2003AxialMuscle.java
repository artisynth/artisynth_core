package artisynth.core.materials;

import maspack.properties.*;
import maspack.interpolation.CubicHermiteSpline1d;

/**
 * Implements the Thelen2003Muscle from OpenSim. At present, only the rigid
 * tendon version is supported. The names of the various properties
 * should match those in OpenSim closely enough to be unambiguous.
 *
 * <p>The force velocity curve has been modified to remove a derivative
 * discontinuity at vn = 0, where vn is the normalized velocity.
 */
public class Thelen2003AxialMuscle extends AxialMuscleMaterialBase {

   
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

   // generic muscle properties

   // Maximum isometric force that the fibers can generate
   public static double DEFAULT_MAX_ISO_FORCE = 1000.0;
   protected double myMaxIsoForce = DEFAULT_MAX_ISO_FORCE;

   // Optimal length of the muscle fibers
   public static double DEFAULT_OPT_FIBRE_LENGTH = 0.1;
   protected double myOptFibreLength = DEFAULT_OPT_FIBRE_LENGTH;

   // Resting length of the tendo
   public static double DEFAULT_TENDON_SLACK_LENGTH = 0.2;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;

   // Angle between tendon and fibers at optimal fiber length, in radians
   public static double DEFAULT_OPT_PENNATION_ANGLE = 0.0;
   protected double myOptPennationAngle = DEFAULT_OPT_PENNATION_ANGLE;

   // aximum contraction velocity of the fibers, in optimal fiberlengths/second
   public static double DEFAULT_MAX_CONTRACTION_VELOCITY = 10;
   protected double myMaxContractionVelocity = DEFAULT_MAX_CONTRACTION_VELOCITY;

   // Compute muscle dynamics ignoring tendon compliance (rigid tendon)
   public static boolean DEFAULT_IGNORE_TENDON_COMPLIANCE = true;
   protected boolean myIgnoreTendonCompliance = DEFAULT_IGNORE_TENDON_COMPLIANCE;

   // Compute muscle dynamics ignoring the force velocity curve
   public static boolean DEFAULT_IGNORE_FORCE_VELOCITY = true;
   protected boolean myIgnoreForceVelocity = DEFAULT_IGNORE_FORCE_VELOCITY;

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
   public static double DEFAULT_FV_LINEAR_EXTRAP_THRESHOLD = 0.95;
   protected double myFvLinearExtrapThreshold =
      DEFAULT_FV_LINEAR_EXTRAP_THRESHOLD;

   // maximum pennation angle, in radians
   public static double DEFAULT_MAX_PENNATION_ANGLE = Math.acos(0.1);
   protected double myMaxPennationAngle = DEFAULT_MAX_PENNATION_ANGLE;

   // lower bound on activation
   public static double DEFAULT_MINIMUM_ACTIVATION = 0.01;
   protected double myMinimumActivation = DEFAULT_MINIMUM_ACTIVATION;

   double myH;
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
                       AxialMuscleMaterialBase.class);

   static {
      myProps.add (
         "maxIsoForce",
         "maximum isometric force", DEFAULT_MAX_ISO_FORCE);
      myProps.add (
         "optFibreLength",
         "optimal fibre length", DEFAULT_OPT_FIBRE_LENGTH);
      myProps.add (
         "tendonSlackLength",
         "resting length of the tendon", DEFAULT_TENDON_SLACK_LENGTH);
      myProps.add (
         "optPennationAngle",
         "pennation angle at optimal length", DEFAULT_OPT_PENNATION_ANGLE);
      myProps.add (
         "maxContractionVelocity",
         "maximum fiber contraction velocity", DEFAULT_MAX_CONTRACTION_VELOCITY);
      myProps.add(
         "ignoreTendonCompliance",
         "if true, assume that the tendon is rigid",
         DEFAULT_IGNORE_TENDON_COMPLIANCE);
      myProps.add(
         "ignoreForceVelocity",
         "if true, ignore the force velocity curve",
         DEFAULT_IGNORE_FORCE_VELOCITY);
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
         "force velocity threshold where linear extrpolation is used",
         DEFAULT_FV_LINEAR_EXTRAP_THRESHOLD);
      myProps.add (
         "maxPennationAngle",
         "maximum pennation angle", DEFAULT_MAX_PENNATION_ANGLE);
      myProps.add (
         "minimumActivation",
         "minimum activation value (to prevent equilibrium singularities)",
         DEFAULT_MINIMUM_ACTIVATION);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Thelen2003AxialMuscle() {
      myH = DEFAULT_OPT_FIBRE_LENGTH*Math.sin(DEFAULT_OPT_PENNATION_ANGLE);
      myMinCos = Math.cos(DEFAULT_MAX_PENNATION_ANGLE);
      setFmaxTendonStrain (DEFAULT_FMAX_TENDON_STRAIN);
   }

   public Thelen2003AxialMuscle (
      double maxIsoForce, double optFibreLen,
      double tendonSlackLen, double optPennationAng) {
      
      this();
      setMaxIsoForce (maxIsoForce);
      setOptFibreLength (optFibreLen);
      setTendonSlackLength (tendonSlackLen);
      setOptPennationAngle (optPennationAng);
   }

   // generic muscle properties

   public double getMaxIsoForce() {
      return myMaxIsoForce;
   }

   public void setMaxIsoForce (double maxf) {
      myMaxIsoForce = maxf;
   }

   public double getOptFibreLength() {
      return myOptFibreLength;
   }

   public void setOptFibreLength (double l) {
      myOptFibreLength = l;
      myH = myOptFibreLength*Math.sin(myOptPennationAngle);
   }

   public double getTendonSlackLength() {
      return myTendonSlackLength;
   }

   public void setTendonSlackLength (double l) {
      myTendonSlackLength = l;
   }

   public double getOptPennationAngle() {
      return myOptPennationAngle;
   }

   public void setOptPennationAngle (double ang) {
      myOptPennationAngle = ang;
      myH = myOptFibreLength*Math.tan(myOptPennationAngle);
   }

   public double getMaxContractionVelocity() {
      return myMaxContractionVelocity;
   }

   public void setMaxContractionVelocity (double maxv) {
      myMaxContractionVelocity = maxv;
   }

  public boolean getIgnoreTendonCompliance () {
      return myIgnoreTendonCompliance;
   }

   public void setIgnoreTendonCompliance (boolean enable) {
      // elastic tendons not implemented yet
      //myIgnoreTendonCompliance = enable;
   }

   public boolean getIgnoreForceVelocity () {
      return myIgnoreForceVelocity;
   }

   public void setIgnoreForceVelocity (boolean enable) {
      myIgnoreForceVelocity = enable;
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

   // util methods

   private final double sqr (double x) {
      return x*x;
   }

   private double cosPennationAngle (double fl) {
      double c = fl/Math.sqrt(myH*myH+fl*fl);
      return Math.max (c, myMinCos);
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
   double computeTendonForce (double tln) {
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
   double computeDTendonForce (double tln) {
      double x = tln-1;

      //Compute tendon force
      double dft = 0;
      if (x > myEToe) {
         dft = myKlin;
      }
      else if (x > 0.0) { 
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
   double computeActiveForceLength (double ln) {
      return Math.exp(-sqr(ln-1.0)/myKShapeActive);
   }

   /**
    * Computes the derivate of the active force length curve.
    */
   double computeDActiveForceLength (double ln) {
      double x = ln - 1.0;
      return -2*x/myKShapeActive * Math.exp(-x*x/myKShapeActive);
   }

   /**
    * Computes the passive force length curve. Adapated from OpenSim code.
    */
   double computePassiveForceLength (double ln) {
      double fpe = 0;
      double e0 = myFmaxMuscleStrain;
      double kpe = myKShapePassive;
      
      //Compute the passive force developed by the muscle
      if (ln > 1.0) {
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
   double computeDPassiveForceLength (double ln) {
      double dfpe = 0;
      double e0 = myFmaxMuscleStrain;
      double kpe = myKShapePassive;

      if(ln > 1.0){
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
         double alphax = alpha*(myFlen-1)/(1+1/myAf); // 2 
         fv = (alphax + vn*myFlen)/(alphax + vn);
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
         double alphax = alpha*(myFlen-1)/(1+1/myAf); // 2
         dfv = alphax*(myFlen-1)/sqr(alphax + vn);
      }
      return dfv;
   }

   /**
    * Computes the force velocity curve, using linear extrapolation
    * for the zone where fv {@code > Flen*FvLinearExtrapThreshold},
    * and not allowing fv to become negative.
    */
   double computeForceVelocity (double vn, double a) {
      double alpha = 0.25 + 0.75*a;
      double fvt = myFlen*myFvLinearExtrapThreshold;
      double vThreshLo = -alpha;
      double vThreshHi =
         alpha*(fvt-1)*(myFlen-1)/((1+1/myAf)*(myFlen-fvt)); // 2
      if (vn <= vThreshLo) {
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
   double computeDForceVelocity (double vn, double a) {
      double alpha = 0.25 + 0.75*a;
      double fvt = myFlen*myFvLinearExtrapThreshold;
      double vThreshLo = -alpha;
      double vThreshHi =
         alpha*(fvt-1)*(myFlen-1)/((1+1/myAf)*(myFlen-fvt)); // 2
      if (vn <= vThreshLo) {
         return 0;
      }
      else if (vn > vThreshHi) {
         return computeDForceVelocityRaw (vThreshHi, a);
      }
      else {
         return computeDForceVelocityRaw (vn, a);
      }
   }

   /**
    * {@inheritDoc}
    */
   public double computeF (
      double l, double ldot, double l0, double excitation) {

      if (myIgnoreTendonCompliance) {
         // rigid tendon case
         double calm = l-myTendonSlackLength; // calm = cos(alpha)*lm
         double lm = Math.sqrt (myH*myH + calm*calm); // muscle length
         double ca = calm/lm; // ca = cos(alpha)

         if (ca < 0) {
            // XXX does this happen?
            return 0;
         }

         double ln = lm/myOptFibreLength; // normalized muscle length
         // normalized muscle velocity:
         double vn = ldot*ca/(myOptFibreLength*myMaxContractionVelocity); 
         double fa = computeActiveForceLength (ln);
         double fp = computePassiveForceLength (ln);
         double fm;
         if (myIgnoreForceVelocity) {
            fm = myMaxIsoForce*(fa*excitation+fp)*ca;
         }
         else {
            double fv = computeForceVelocity (vn, excitation);
            if (fv < 0) {
               fv = 0;
            }
            fm = myMaxIsoForce*(fa*fv*excitation+fp)*ca;
         }
         return fm;
      }
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      if (myIgnoreTendonCompliance) {
         // rigid tendon case
         double calm = l-myTendonSlackLength; // calm = cos(alpha)*lm
         double lmSqr = myH*myH + calm*calm;
         double lm = Math.sqrt (lmSqr); // muscle length
         double ca = calm/lm; // ca = cos(alpha)

         if (ca < 0) {
            return 0;
         }

         double ln = lm/myOptFibreLength; // normalized muscle length
         // normalized muscle velocity:
         double vn = ldot*ca/(myOptFibreLength*myMaxContractionVelocity); 
         double fa = computeActiveForceLength (ln);
         double fp = computePassiveForceLength (ln);

         double dca = myH*myH/(lmSqr*lm);
         double dvn = ldot*dca/(myOptFibreLength*myMaxContractionVelocity);
         double dln = ca/myOptFibreLength;

         double dfa = computeDActiveForceLength(ln)*dln;
         double dfp = computeDPassiveForceLength(ln)*dln;

         if (myIgnoreForceVelocity) {
            double fm = myMaxIsoForce*(fa*excitation+fp);
            return myMaxIsoForce*(
               dfa*excitation + dfp)*ca + fm*dca;
         }
         else {
            double fv = computeForceVelocity (vn, excitation);
            double dfv = computeDForceVelocity(vn, excitation)*dvn;
            if (fv < 0) {
               fv = 0;
               dfv = 0;
            }
            double fm = myMaxIsoForce*(fa*fv*excitation+fp);
            return myMaxIsoForce*(
               (dfa*fv+fa*dfv)*excitation + dfp)*ca + fm*dca;
         }
      }
      return 0;
   }
   
   /**
    * {@inheritDoc}
    */
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      if (myIgnoreTendonCompliance) {
         // rigid tendon case
         if (myIgnoreForceVelocity) {
            return 0;
         }
         double calm = l-myTendonSlackLength; // calm = cos(alpha)*lm
         double lm = Math.sqrt (myH*myH + calm*calm); // muscle length
         double ca = calm/lm; // ca = cos(alpha)
         
         if (ca < 0) {
            return 0;
         }

         double ftmp;
         double ln = lm/myOptFibreLength; // normalized muscle length
         double fa = computeActiveForceLength(ln);
         // normalized muscle velocity:
         double vn = ldot*ca/(myOptFibreLength*myMaxContractionVelocity); 
         ftmp = fa*computeDForceVelocity(vn,excitation)*excitation;

         return (myMaxIsoForce*ftmp*ca*ca/
                 (myOptFibreLength*myMaxContractionVelocity));
      }
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDFdldotZero() {
      return myIgnoreForceVelocity;
   }

}
