/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.List;
import maspack.util.Disposable;

/**
 * Basic interface requirements for all Artisynth models
 */
public interface Model extends ModelComponent, Disposable, HasState {
   
   /**
    * Flag for {@link #advance} indicating that the model's state may have been
    * changed or restored immediately prior to the method call. This means that
    * {@link #advance} may wish to perform its computations in a way as to
    * ensure precise numeric repeatability if this is the case.
    */
   public static int STATE_IS_VOLATILE = 0x0001;
   
   /**
    * Causes this model to initialize itself at time t. This method will be
    * called at the very beginning of the simulation (with t = 0), or
    * immediately after the model's state has been reset using {@link
    * #setState}, in which case <code>t</code> may have an arbitrary value.
    * 
    * @param t initialization time (seconds)
    */
   public void initialize (double t);

   /**
    * Prepares this model for advance from time t0 to time t1.
    * Often this method does nothing; it is provided in case a model
    * needs to update internal state at the very beginning a step
    * before input probes and controllers are applied.
    * 
    * <p>If the method determines that the step size should be
    * reduced, it can return a {@link StepAdjustment} object indicating
    * the recommended reduction. Otherwise, the method may return 
    * <code>null</code>
    * 
    * <p>The flags argument gives additional information that may
    * be relevant when advancing the model. Currently implemented
    * flags include {@link #STATE_IS_VOLATILE}.
    * 
    * @param t0
    * current time (seconds)
    * @param t1
    * new time to advance to (seconds)
    * @param flags 
    * additional information relevant to the advance
    * @return null, or a step adjustment recommendation
    */
   public StepAdjustment preadvance (double t0, double t1, int flags);

   /**
    * Advances this model from time t0 to time t1.
    * 
    * <p>If the method determines that the step size should be
    * reduced, it can return a {@link StepAdjustment} object indicating
    * the recommended reduction. Otherwise, the method may return 
    * <code>null</code>
    *
    * <p>The flags argument gives additional information that may
    * be relevant when advancing the model. Currently implemented
    * flags include {@link #STATE_IS_VOLATILE}.
    * 
    * @param t0
    * current time (seconds)
    * @param t1
    * new time to advance to (seconds)
    * @param flags
    * rdditional information relevant to the advance
    * @return null, or a step adjustment recommendation
    */
   public StepAdjustment advance (double t0, double t1, int flags);

//   /**
//    * Called before advancing from t0 to t1.
//    * 
//    * @param t0 time at the step start (seconds)
//    * @param t1 time at the step end (seconds)
//    */
//   public void setDefaultInputs (double t0, double t1);

   /**
    * Returns the maximum step by which this object should be advanced within a
    * simulation loop.
    * 
    * @return maximum step size (seconds)
    */
   public double getMaxStepSize();

   /** 
    * Called when the model is discarded. Disposes of any resources used. 
    */
   public void dispose();


}
