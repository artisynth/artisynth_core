/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;

import artisynth.core.modelbase.Model;
import artisynth.core.modelbase.StepAdjustment;

public interface MechSystemModel extends Model, MechSystem {

   public void getAttachments (List<DynamicAttachment> list, int level);

   public void getDynamicComponents (
      List<DynamicMechComponent> active,
      List<DynamicMechComponent> attached,
      List<DynamicMechComponent> parametric);

   public void getCollidables (List<Collidable> list, int level);

   public void getConstrainers (List<Constrainer> list, int level);

   public void getForceEffectors (List<ForceEffector> list, int level);

   public void getAuxStateComponents (List<HasAuxState> list, int level);
   
   public void getSlaveObjectComponents (List<HasSlaveObjects> list, int level);

   /**
    * Checks the velocity stability of this system. If the velocity of any
    * component appears to be unstable, return that component. Otherwise, return
    * null.
    */
   public DynamicMechComponent checkVelocityStability();

   //public void recursivelyUpdateVelState (int flags);

   //public void recursivelyUpdatePosState (int flags);

   public void recursivelyInitialize (double t, int level);

//   public boolean recursivelyCheckStructureChanged ();

   public void recursivelyFinalizeAdvance (
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level);

}
