package artisynth.core.materials;

public class LinearAxialMuscle extends AxialMuscleMaterial {

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
