/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.util.Clonable;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.SolidDeformation;

public interface AuxiliaryMaterial extends Clonable {

   public abstract void computeStress (
      SymmetricMatrix3d sigma,
      SolidDeformation def, IntegrationPoint3d pt,
      IntegrationData3d dt, FemMaterial baseMat);

   public abstract void computeTangent(
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def,
      IntegrationPoint3d pt, IntegrationData3d dt, FemMaterial baseMat);

   public abstract boolean hasSymmetricTangent();

   public abstract boolean isInvertible();
   
   /**
    * Linear stress/stiffness response to deformation, allows tangent
    * to be pre-computed and stored.
    * 
    * @return true if linear response
    */
   public abstract boolean isLinear();
   
   /**
    * Deformation is computed by first removing a rotation component 
    * (either explicit or computed from strain)
    * 
    * @return true if material is corotated
    */
   public abstract boolean isCorotated();

}
