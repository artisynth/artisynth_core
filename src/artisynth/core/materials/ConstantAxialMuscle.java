package artisynth.core.materials;

public class ConstantAxialMuscle extends AxialMuscleMaterial {
   
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
