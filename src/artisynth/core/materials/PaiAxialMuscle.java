package artisynth.core.materials;

public class PaiAxialMuscle extends AxialMuscleMaterial {
   
   /**
    * Creates a new PaiAxialMuscle with default values. The (deprecated) {@code
    * forceScaling} property is set to 1.
    *
    * @return created material
    */
   public static PaiAxialMuscle create () {
      PaiAxialMuscle mus = new PaiAxialMuscle();
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Creates a new PaiAxialMuscle with specified values. The damping property
    * is set to 0 and the (deprecated) {@code forceScaling} property is set to
    * 1.
    * 
    * @param fmax maximum contractile force
    * @param optL length beyond which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    * @return created material
    */
   public static PaiAxialMuscle create (
      double maxF, double optL, double maxL, 
      double tendonRatio, double passiveFraction) {
      PaiAxialMuscle mus = new PaiAxialMuscle (
         maxF, optL, maxL, tendonRatio, passiveFraction);
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Creates a new PaiAxialMuscle with specified values. The (deprecated)
    * {@code forceScaling} property is set to 1.
    * 
    * @param fmax maximum contractile force
    * @param optL length beyond which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    * @param damping damping parameter
    * @return created material
    */
   public static PaiAxialMuscle create (
      double maxF, double optL, double maxL, 
      double tendonRatio, double passiveFraction, double damping) {
      PaiAxialMuscle mus = new PaiAxialMuscle (
         maxF, optL, maxL, tendonRatio, passiveFraction, damping);
      mus.setForceScaling (1);
      return mus;
   }  

   /**
    * Constructs a new PaiAxialMuscle.
    *
    * <p>Important: for historical reasons, this constructor sets the
    * deprecated {@code forceScaling} property to 1000, thus scaling the
    * effective values of the {@code maxForce} and {@code damping} properties.
    */
   public PaiAxialMuscle() {
      super();
   }
   
   /**
    * Constructs a new PaiAxialMuscle with specified values. The damping
    * parameter is set to 0.
    *
    * @deprecated For historical reasons, this constructor sets the deprecated
    * {@code forceScaling} property to 1000, thus scaling the effective value
    * of {@code fmax}.
    *
    * @param fmax maximum contractile force
    * @param optL length beyond which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    */
   public PaiAxialMuscle (
      double maxF, double optL, double maxL, 
      double tendonRatio, double passiveFraction) {
      this (maxF, optL, maxL, tendonRatio, passiveFraction, /*damping=*/0);
   }

   /**
    * Constructs a new PaiAxialMuscle with specified values.
    * 
    * @deprecated For historical reasons, this constructor sets the deprecated
    * {@code forceScaling} property to 1000, this scaling the effective values
    * of {@code fmax} and {@code damping}.
    *
    * @param fmax maximum contractile force
    * @param optL length beyond which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    * @param damping damping parameter
    */
   public PaiAxialMuscle (
      double maxF, double optL, double maxL, 
      double tendonRatio, double passiveFraction, double damping) {

      setMaxForce (maxF);
      setOptLength (optL);
      setMaxLength (maxL);
      setTendonRatio (tendonRatio);
      setPassiveFraction (passiveFraction);
      setDamping (damping);      
   }
   
   public double computeF(double l, double ldot, double l0, double ex) {
      double passive = 0, active = 0;

      // active part
      double normFibreLen = (l - myOptLength * myTendonRatio)
	    / (myOptLength * (1 - myTendonRatio));
      if (normFibreLen > minStretch && normFibreLen < 1.0) {  // 1.0 instead of maxStretch
	 active = 0.5 * (1 + Math.cos(2 * Math.PI * normFibreLen));
      }
      else if (normFibreLen >= 1.0) {
         active = 1.0;
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
      double active_dFdl = 0.0, passive_dFdl = 0.0;
      double normFibreLen = (l - myOptLength * myTendonRatio)
	    / (myOptLength * (1 - myTendonRatio));
      
      // active part
      if (normFibreLen > minStretch && normFibreLen < 1.0) { // 1.0 instead of maxStretch
	 active_dFdl = -myMaxForce * ex * Math.PI
	       * Math.sin(2 * Math.PI * normFibreLen)
	       / (myOptLength * (1 - myTendonRatio));
      }

      // passive part
      if (l > myOptLength && l < myMaxLength) {
	 passive_dFdl = myMaxForce * myPassiveFraction
	       / (myMaxLength - myOptLength);
      }
      return forceScaling * (passive_dFdl + active_dFdl);
   }

   public double computeDFdldot(double l, double ldot, double l0, double ex) {
      return forceScaling * myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }
}
