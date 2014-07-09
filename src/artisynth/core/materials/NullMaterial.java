package artisynth.core.materials;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;


public class NullMaterial extends FemMaterial {

   @Override
   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {
      sigma.setZero();      
   }

   @Override
   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {
      D.setZero();
   }
   
   public boolean isInvertible() {
      return true;
   }

}
