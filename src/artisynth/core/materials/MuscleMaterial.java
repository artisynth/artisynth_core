package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;

/**
 * Base class for Muscle materials. These are similiar to regular materials,
 * except that they add to the stress and tangent term, and the associated
 * methods also accept an excitation and a direction.
 */
public abstract class MuscleMaterial extends MaterialBase {

   static Class<?>[] mySubClasses = new Class[] {
      GenericMuscle.class,
      SimpleForceMuscle.class,
      BlemkerMuscle.class,
      FullBlemkerMuscle.class,
      InactiveMuscle.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   /** 
    * Hook to notify associated components of change in parameters.
    */
   protected void notifyHostOfPropertyChange() {
   }

   public abstract void computeStress (
      SymmetricMatrix3d sigma, double excitation, Vector3d dir,
      DeformedPoint def, FemMaterial baseMat);

   public abstract void computeTangent (
      Matrix6d D, SymmetricMatrix3d sigma, double excitation, Vector3d dir,
      DeformedPoint def, FemMaterial baseMat);

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Vector3d dir, double excitation) {
      computeStress (sigma, excitation, dir, def, /*baseMat=*/null);
      if (D != null) {
         computeTangent (D, sigma, excitation, dir, def, /*baseMat=*/null);
      }
   }
    
   public boolean equals (MuscleMaterial mat) {
      return true;
   }

   public boolean equals (Object obj) {
      if (obj instanceof MuscleMaterial) {
         return equals ((MuscleMaterial)obj);
      }
      else {
         return false;
      }
   }

   public MuscleMaterial clone() {
      MuscleMaterial mat = (MuscleMaterial)super.clone();
      return mat;
   }

   /**
    * Returns true if this material is defined for a deformation gradient
    * with a non-positive determinant.
    */
   public boolean isInvertible() {
      return false;
   }

}

  
