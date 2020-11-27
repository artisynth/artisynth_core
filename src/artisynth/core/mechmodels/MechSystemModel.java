/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;

import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.Model;
import artisynth.core.modelbase.StepAdjustment;

public interface MechSystemModel extends Model, MechSystem {

   public void getAttachments (List<DynamicAttachment> list, int level);

   public void getDynamicComponents (
      List<DynamicComponent> active,
      List<DynamicComponent> attached,
      List<DynamicComponent> parametric);
   
   public void getDynamicComponents (List<DynamicComponent> comps);

   public void getCollidables (List<Collidable> list, int level);

   public void getConstrainers (List<Constrainer> list, int level);

   public void getForceEffectors (List<ForceEffector> list, int level);

   public void getAuxStateComponents (List<HasNumericState> list, int level);
   
   public void getSlaveObjectComponents (List<HasSlaveObjects> list, int level);

   public void addGeneralMassBlocks (SparseNumberedBlockMatrix M);

   public void getMassMatrixValues (
      SparseNumberedBlockMatrix M, VectorNd f, double t);

   public void mulInverseMass (SparseBlockMatrix M, VectorNd a, VectorNd f);

   /**
    * Checks the velocity stability of this system. If the velocity of any
    * component appears to be unstable, return that component. Otherwise, return
    * null.
    *
    * @return first component containing an unstable velocity, or
    * <code>null</code> if there is no instability
    */
   public DynamicComponent checkVelocityStability();

   //public void recursivelyUpdateVelState (int flags);

   //public void recursivelyUpdatePosState (int flags);

   public void recursivelyInitialize (double t, int level);

//   public boolean recursivelyCheckStructureChanged ();

   public void recursivelyPrepareAdvance (
      double t0, double t1, int flags, int level);

   public void recursivelyFinalizeAdvance (
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level);

}
