package artisynth.core.materials;

public class LinearAxialMuscle extends AxialMuscleMaterial {

   /**
    * Creates a new LinearAxialMuscle with default values. The (deprecated)
    * {@code forceScaling} term is set to 1.
    *
    * @return created material 
    */
   public static LinearAxialMuscle create() {
      LinearAxialMuscle mus = new LinearAxialMuscle();
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Creates a new LinearAxialMuscle with a specified maximum force. The
    * {@code optLength}, {@code maxLength}, {@code passiveFraction} and {@code
    * damping} properties are set to 0, 1, 0, and 0, and the deprecated {@code
    * forceScaling} property is set to 1.
    *
    * @param fmax maximum contractile force
    */
   public static LinearAxialMuscle create(double fmax, double lrest) {
      LinearAxialMuscle mus = new LinearAxialMuscle(fmax, lrest);
      mus.setForceScaling (1);
      return mus;
   }

   /**
    * Constructs a new LinearAxialMuscle.
    *
    * <p>Important: for historical reasons, this constructor sets the
    * deprecated {@code forceScaling} property to 1000, thus scaling the
    * effective values of the {@code maxForce} and {@code damping} properties.
    */
   public LinearAxialMuscle() {
      super ();
   }
   
   /**
    * Constructs a new LinearAxialMuscle with a specified maximum force.  The
    * {@code optLength} and {@code maxLength} properties are set to {@code
    * lrest} and {@code 1.5 lrest}, {@code passiveFraction} is set to 1, and
    * the {@code damping} property is set to 0.
    *
    * @deprecated For historical reasons, this constructor sets the deprecated
    * {@code forceScaling} property to 1000, thuis scaling the effective value
    * of {@code fmax}.
    *
    * @param fmax maximum contractile force
    * @param lrest sets the {@code optLength} and {@code maxLength} properties
    */
   public LinearAxialMuscle(double fmax, double lrest) {
      super();
      setMaxForce (fmax);
      setOptLength (lrest);
      setMaxLength (lrest*1.5);
      setPassiveFraction (1.0);
   }
   
   /**
    * Constructs a new LinearAxialMuscle with specified properties. The {@code
    * damping} property is set to 0, and the deprecated {@code forceScaling}
    * property is set to 1.
    *
    * @param fmax maximum contractile force
    * @param lopt length below which zero active force is generated
    * @param lmax length beyond which maximum active and passive force is
    * generated
    * @param pfrac passive fraction of {@code fmax} that forms the maximum
    * passive force
    */
   public LinearAxialMuscle(
      double fmax, double lopt, double lmax, double pfrac) {
      this (fmax, lopt, lmax, pfrac, /*damping=*/0);
   }
   
   /**
    * Constructs a new LinearAxialMuscle with specified properties.  The
    * deprecated {@code forceScaling} property is set to 1.
    *
    * @param fmax maximum contractile force
    * @param lopt length below which zero active force is generated
    * @param lmax length beyond which maximum active and passive force is
    * generated
    * @param pfrac passive fraction of {@code fmax} that forms the maximum
    * passive force
    * @param damping damping term
    */
   public LinearAxialMuscle(
      double fmax, double lopt, double lmax, double pfrac, double damping) {
      super();
      setMaxForce (fmax);
      setOptLength (lopt);
      setMaxLength (lmax);
      setPassiveFraction (pfrac);
      setDamping (damping);
      setForceScaling (1);
   }
   
   public double computeF(double l, double ldot, double l0, double ex) {
      double passive = 0, active = 0;

      // active part
      if (l > myMaxLength) {
	 active = 1.0;
      } else if (l > myOptLength) {
	 active = (l - myOptLength) / (myMaxLength - myOptLength);
      }

      // passive part
      if (l > myMaxLength) {
	 passive = 1.0;
      } else if (l > myOptLength) {
	 passive = (l - myOptLength) / (myMaxLength - myOptLength);
      }

      return forceScaling * (
	      myMaxForce * (active * ex + passive * myPassiveFraction)
	    + myDamping * ldot);
   }

   public double computeDFdl(double l, double ldot, double l0, double ex) {
      double dFdl = 0;
      if (l > myOptLength && l < myMaxLength) {
	 dFdl = myMaxForce * (myPassiveFraction + ex)
	       / (myMaxLength - myOptLength);
      } else {
	 dFdl = 0.0;
      }
      return forceScaling * dFdl;
   }

   public double computeDFdldot(double l, double ldot, double l0, double excitation) {
      return forceScaling * myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }
}
