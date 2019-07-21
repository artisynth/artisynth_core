package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;

/**
 * A muscle material that does nothing
 */
public class InactiveMuscle extends MuscleMaterial {

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Vector3d dir0, double excitation, MaterialStateObject state) {
      sigma.setZero();
      if (D != null) {
         D.setZero();
      }
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
