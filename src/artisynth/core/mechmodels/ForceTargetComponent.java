/**
 * Copyright (c) 2019, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ModelComponent;
import maspack.matrix.*;

/** 
 * Indicates a force effector whose force can be controlled by the the inverse
 * controller.
 */
public interface ForceTargetComponent extends ForceEffector, ModelComponent {
   public int getForceSize();

   public void getForce (VectorNd minf, boolean staticOnly);

   /** 
    * Add a row to the force Jacobian for this force target component.
    * The Jacobian maps state velocities u into the forces fm that should
    * be minimized 
    * <pre>
    * fm = J u
    * </pre>
    * 
    * @param J force Jacobian
    * @param h current time step
    * @param bi block row index for the row to be added
    * @param staticOnly use only static forces
    * @return bi + 1
    */   
   public int addForcePosJacobian (
      SparseBlockMatrix J, double h, boolean staticOnly, int bi);
   
   public int addForceVelJacobian (
      SparseBlockMatrix J, double h, int bi);
}
