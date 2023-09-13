package artisynth.core.materials;

import java.io.*;
import maspack.function.*;

public class PeckAxialMuscle extends AxialMuscleMaterial {
   
   /**
    * Creates a new PeckAxialMuscle with default values. The (deprecated)
    * {@code forceScaling} property is set to 1.
    *
    * @return created material
    */
   public static PeckAxialMuscle create () {
      PeckAxialMuscle mus = new PeckAxialMuscle();
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Creates a new PeckAxialMuscle with specified values. The damping property
    * is set to 0 and the (deprecated) {@code forceScaling} property is set to
    * 1.
    * 
    * @param fmax maximum contractile force
    * @param optL length at which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    * @return created material
    */
   public static PeckAxialMuscle create (
      double fmax, double optL, double maxL, double tratio, double pfrac) {
      PeckAxialMuscle mus = new PeckAxialMuscle (
         fmax, optL, maxL, tratio, pfrac);
      mus.setForceScaling (1);
      return mus;
   }
   
   /**
    * Creates a new PeckAxialMuscle with specified values. The (deprecated)
    * {@code forceScaling} property is set to 1.
    * 
    * @param fmax maximum contractile force
    * @param optL length at which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    * @param damping damping parameter
    * @return created material
    */
   public static PeckAxialMuscle create (
      double fmax, double optL, double maxL, 
      double tratio, double pfrac, double damping) {
      PeckAxialMuscle mus = new PeckAxialMuscle (
         fmax, optL, maxL, tratio, pfrac, damping);
      mus.setForceScaling (1);
      return mus;
   }

   /**
    * Constructs a new PeckAxialMuscle.
    *
    * <p>Important: for historical reasons, this constructor sets the
    * deprecated {@code forceScaling} property to 1000, thus scaling the
    * effective values of the {@code maxForce} and {@code damping} properties.
    */
   public PeckAxialMuscle () {
   }
   
   /**
    * Constructs a new PeckAxialMuscle with specified values. The damping
    * parameter is set to 0.
    *
    * @deprecated For historical reasons, this constructor sets the deprecated
    * {@code forceScaling} property to 1000, thus scaling the effective value
    * of {@code fmax}.
    *
    * @param fmax maximum contractile force
    * @param optL length at which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    */
   public PeckAxialMuscle (
      double fmax, double optL, double maxL, 
      double tratio, double pfrac) {
      this (fmax, optL, maxL, tratio, pfrac, /*damping=*/0);
   }

   /**
    * Constructs a new PeckAxialMuscle with specified values.
    * 
    * @deprecated For historical reasons, this constructor sets the deprecated
    * {@code forceScaling} property to 1000, thus scaling the effective values
    * of {@code fmax} and {@code damping}.
    *
    * @param fmax maximum contractile force
    * @param optL length at which maximum active force is generated
    * @param maxL length at which maximum passive force is generated
    * @param tratio tendon to fibre length ratio
    * @param pfrac passive fraction (of {@code fmax}) that forms the maximum
    * passive force
    * @param damping damping parameter
    */
   public PeckAxialMuscle (
      double fmax, double optL, double maxL, 
      double tratio, double pfrac, double damping) {
      setMaxForce (fmax);
      setOptLength (optL);
      setMaxLength (maxL);
      setTendonRatio (tratio);
      setPassiveFraction (pfrac);
      setDamping (damping);
   }
   
   public double computeF(double l, double ldot, double l0, double ex) {
      double passive = 0, active = 0;

      // active part
      double normFibreLen = (l - myOptLength * myTendonRatio)
	    / (myOptLength * (1 - myTendonRatio));
      if (normFibreLen > minStretch && normFibreLen < maxStretch) {
	 active = 0.5 * (1 + Math.cos(2 * Math.PI * normFibreLen));
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
      if (normFibreLen > minStretch && normFibreLen < maxStretch) {
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

   public void writeForceLengthCurve (
      String fileName, double x0, double x1,
      int npnts, String fmtStr) throws IOException {
      Function1x1 fxn = new Function1x1() {
            public double eval(double l) {
               return computeF (l, 0, 0, /*excitation=*/1.0);
            }
         };
      FunctionUtils.writeValues (new File(fileName), fxn, x0, x1, npnts, fmtStr);
   }

   public static void main (String[] args) throws IOException {
      PeckAxialMuscle peck =
         new PeckAxialMuscle (
            /*maxf*/1, /*optL*/1, /*maxL*/2,
            /*tratio*/0, /*passivefrac*/1, /*damping*/0);
      peck.setForceScaling (1.0);

      peck.writeForceLengthCurve ("peckFLC.txt", 0, 2.5, 400, "%g");
   }
}

