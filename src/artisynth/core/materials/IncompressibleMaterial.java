package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.InternalErrorException;

public class IncompressibleMaterial extends IncompressibleMaterialBase {

   public IncompressibleMaterial() {
      super();
   }
   
   public IncompressibleMaterial (double kappa) {
      this();
      setBulkModulus (kappa);
   }

   public void computeDevStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      sigma.setZero();
      if (D != null) {
         D.setZero();
      }
   }

   @Override
   public boolean isInvertible() {
      // right now, true only with a quadratic bulk potential and for this raw
      // IncompressibleMaterial specifically; not any of the other
      // materials derived from IncompressibleMaterialBase
      return (myBulkPotential == BulkPotential.QUADRATIC);
   }

}
   
   
