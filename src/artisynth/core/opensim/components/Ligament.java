package artisynth.core.opensim.components;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.LinearAxialMuscle;
import artisynth.core.materials.AxialLigament;

import maspack.function.*;

public class Ligament extends ForceSpringBase {
   
   private double resting_length;
   private double pcsa_force;
   FunctionBase force_length_curve;
   
   public Ligament() {
      resting_length = -1;
      pcsa_force = -1;
      force_length_curve = null;
   }

   public void setRestingLength(double l) {
      resting_length = l;
   }
   
   public double getRestingLength() {
      return resting_length;
   }
   
   public void setPCSAForce(double f) {
      pcsa_force = f;
   }
   
   public double getPCSAForce() {
      return pcsa_force;
   }

   public void setForceLengthCurve(FunctionBase flc) {
      force_length_curve = flc;
      force_length_curve.setParent (this);
   }
   
   public FunctionBase getForceLengthCurve() {
      return force_length_curve;
   }
   
   @Override
   public Ligament clone () {
      Ligament lig = (Ligament)super.clone ();
      if (force_length_curve != null) {
         lig.setForceLengthCurve (force_length_curve.clone ());
      }
      return lig;
   }
   
   @Override
   public AxialMaterial createMaterial () {
      AxialLigament mat = new AxialLigament (
         getPCSAForce(), getRestingLength(), /*damping=*/0);
      if (force_length_curve != null &&
          force_length_curve.getFunction() instanceof Diff1Function1x1Base) {
         mat.setForceLengthCurve (
            (Diff1Function1x1Base)force_length_curve.getFunction());
      }
      return mat;
   }
   
}
