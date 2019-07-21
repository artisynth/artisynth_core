package artisynth.core.materials;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;


public class NullMaterial extends FemMaterial {

   /**
    * {@inheritDoc}
    */
   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      sigma.setZero();
      if (D != null) {
         D.setZero();
      }
   }
   
   @Override
   public boolean isInvertible() {
      return true;
   }
   
   @Override
   public boolean isLinear() {
      return true;
   }
   
   @Override
   public boolean isCorotated() {
      return false;
   }

}
