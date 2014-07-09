/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.util.*;

/**
 * Indicates a model component that has auxiliary state information
 * that can be stored as a sequence of double and integer values.
 */
public interface HasAuxState {

   public enum StateContext {
      CURRENT,
      INITIAL,
      DATA
   }

   /** 
    * Called at the very beginning of the time step (in the system's
    * preadvance() method) to perform any required updating of the component's
    * state before the application in input probes or controllers. If no
    * such updating is required, this method may do nothing.
    * 
    * @param t0 beginning time associated with the time step advance
    * @param t1 end time associated with the time step advance
    */
   public void advanceAuxState (double t0, double t1);

   /** 
    * Skips over the state information for this component contained
    * in the supplied data buffer, starting at the current buffer offsets.
    * Essentially this a <i>dummy</i> read; the buffer offsets should
    * be advanced over the state information, but that information
    * should not actually be stored in the component.
    * 
    * @param data buffer containing the state information
    */
   public void skipAuxState (DataBuffer data);
   
   /** 
    * Saves state information for this component by adding data to the
    * supplied DataBuffer. Existing data in the buffer should not be disturbed.
    * 
    * @param data buffer for storing the state values.
    */
   public void getAuxState (DataBuffer data);

   /** 
    * Saves initial state information data for this component by adding data
    * to the supplied data buffer. Existing data in the buffer should not 
    * be disturbed.
    *
    * <p>If <code>oldData</code> is non-null, then this contains
    * previously stored initial state information (starting at its current
    * buffer offsets), which should be stored into 
    * <code>newData</code> in place of the current component state data.
    * This may only be partially possible if the component's state structure 
    * has changed since <code>oldData</code> was written.
    * 
    * @param newData buffer for storing the state values.
    * @param oldData if non-null, contains old state information that should be
    * written into <code>newData</code> in place of the current state
    * information.
    */
   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData);

   /** 
    * Restores the state for this component by reading from the supplied
    * data buffer, starting at the current buffer offsets.
    * 
    * @param data buffer containing the state information
    */
   public void setAuxState (DataBuffer data);

}
