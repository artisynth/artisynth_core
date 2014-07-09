package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;

/**
 * A muscle material that does nothing
 */
public class InactiveMuscle extends MuscleMaterial {

   public void computeStress (
      SymmetricMatrix3d sigma, double excitation, Vector3d dir0,
      SolidDeformation def, FemMaterial baseMat) {
      sigma.setZero();
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, double excitation, Vector3d dir0, 
      SolidDeformation def, FemMaterial baseMat) {
      D.setZero();
   }

   public boolean equals (MuscleMaterial mat) {
      if (!(mat instanceof InactiveMuscle)) {
         return false;
      }
      return super.equals (mat);
   }

   @Override 
      public boolean isInvertible() {
      return true;
   }

   public InactiveMuscle clone() {
      InactiveMuscle mat = (InactiveMuscle)super.clone();
      return mat;
   }

}
