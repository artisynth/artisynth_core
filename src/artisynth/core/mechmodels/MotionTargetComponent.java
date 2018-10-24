/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;

/** 
 * Indicates a dynamic component for which motion targets can be set.
 */
public interface MotionTargetComponent extends DynamicComponent {

   public TargetActivity getTargetActivity();

   public void setTargetActivity (TargetActivity interp);

   public int getVelStateSize();

   public int getTargetVel (double[] velt, double s, double h, int idx);

   //public int setTargetVel (double[] velt, int idx);

   public int getPosStateSize();

   public int getTargetPos (double[] post, double s, double h, int idx);

   //public int setTargetPos (double[] post, int idx);

   /** 
    * Add a row to the motion target Jacobian for this motion target.
    * The Jacobian maps state velocities u into motion target velocities
    * vt according to
    * <pre>
    * vt = J u
    * </pre>
    * 
    * @param J motion target Jacobian
    * @param bi block row index for the row to be added
    * @return bi + 1
    */   
   public int addTargetJacobian (SparseBlockMatrix J, int bi);

   public void resetTargets();
}