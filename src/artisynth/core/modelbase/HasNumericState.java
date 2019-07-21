/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.util.*;

/**
 * Indicates a model component that has state information that can be stored
 * inside a {@link DataBuffer}.
 */
public interface HasNumericState {

   /** 
    * Called at the very beginning of the time step (in the system's
    * preadvance() method) to perform any required updating of the component's
    * state before the application in input probes or controllers. If no
    * such updating is required, this method may do nothing.
    * 
    * @param t0 beginning time associated with the time step advance
    * @param t1 end time associated with the time step advance
    */
   default public void advanceState (double t0, double t1) {
   }

   /** 
    * Saves state information for this component by adding data to the
    * supplied DataBuffer. Existing data in the buffer should not be disturbed.
    * 
    * @param data buffer for storing the state values.
    */
   public void getState (DataBuffer data);

   /** 
    * Restores the state for this component by reading from the supplied
    * data buffer, starting at the current buffer offsets.
    * 
    * @param data buffer containing the state information
    */
   public void setState (DataBuffer data);

   /**
    * Specifies whether this component currently hold state.
    */
   public boolean hasState();

   /**
    * Returns a version number for this component's state. When the version
    * number changes, any previously saved state should be considered no longer
    * compatible. The default implementation of this method always returns 0,
    * indicating that state compatibility never changes.
    */
   default public int getStateVersion() {
      return 0;
   }

}
