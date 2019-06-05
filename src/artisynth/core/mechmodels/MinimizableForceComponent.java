/**
 * Copyright (c) 2019, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;

/** 
 * Indicates a force effector whose force can be minimized by the the inverse
 * controller.
 */
public interface MinimizableForceComponent extends ForceEffector {

   public int getMinForceSize();

   public void getMinForce (VectorNd minf, boolean staticOnly);

   /** 
    * Add a row to the force Jacobian for this minimum force component.
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
   public int addMinForcePosJacobian (
      SparseBlockMatrix J, double h, boolean staticOnly, int bi);
   
   public int addMinForceVelJacobian (
      SparseBlockMatrix J, double h, int bi);
}
