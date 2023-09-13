package artisynth.core.materials;

public class ConstantAxialMuscle extends AxialMuscleMaterial {

   /**
    * Creates a new ConstantAxialMuscle with default values. The (deprecated)
    * {@code forceScaling} term is set to 1.
    *
    * @return created material 
    */
   public static ConstantAxialMuscle create() {
      ConstantAxialMuscle mus = new ConstantAxialMuscle();
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Creates a new ConstantAxialMuscle with a specified maximum force. The
    * {@code passiveFraction} and {@code damping} properties are both set to 0,
    * and the deprecated {@code forceScaling} property is set to 1.
    *
    * @param fmax maximum contractile force
    * @return created material 
    */
   public static ConstantAxialMuscle create(double fmax) {
      ConstantAxialMuscle mus = new ConstantAxialMuscle(fmax);
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Constructs a new ConstantAxialMuscle.
    *
    * <p>Important: for historical reasons, this constructor sets the
    * deprecated {@code forceScaling} property to 1000, thus scaling the
    * effective values of the {@code maxForce} and {@code damping} properties.
    */
   public ConstantAxialMuscle () {
      super();
   }
   
   /**
    * Constructs a new ConstantAxialMuscle with a specified maximum force. The
    * {@code passiveFraction} and {@code damping} properties are both set to 0,
    *
    * @deprecated For historical reasons, this constructor sets the deprecated
    * {@code forceScaling} property to 1000, thuis scaling the effective value
    * of {@code fmax}.
    *
    * @param fmax maximum contractile force
    */
   public ConstantAxialMuscle (double fmax) {
      super();
      setMaxForce (fmax);
   }
   
   /**
    * Constructs a new ConstantAxialMuscle with a specified values.  The {@code
    * damping} property is set to 0, and the deprecated {@code forceScaling}
    * property is set to 1.
    *
    * @param fmax maximum contractile force
    * @param pfrac passive fraction: the proportion of fmax to apply as passive
    * tension
    */
   public ConstantAxialMuscle (double fmax, double pfrac) {
      this (fmax, pfrac, /*damping=*/0);
   }
   
   /**
    * Constructs a new ConstantAxialMuscle with a specified values.  The
    * deprecated {@code forceScaling} property is set to 1.
    *
    * @param fmax maximum contractile force
    * @param pfrac passive fraction: the proportion of fmax to apply as passive
    * tension
    * @param damping damping term
    */
   public ConstantAxialMuscle (double fmax, double pfrac, double damping) {
      super();
      setMaxForce (fmax);
      setPassiveFraction (pfrac);
      setDamping (damping);
      setForceScaling (1);
   }
   
   public double computeF(double l, double ldot, double l0, double ex) {
      return forceScaling * ( myMaxForce * ( ex + myPassiveFraction ) + myDamping * ldot );
   }

   public double computeDFdl(double l, double ldot, double l0, double ex) {
      return 0;
   }

   public double computeDFdldot(double l, double ldot, double l0, double excitation) {
      return forceScaling * myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }
}
