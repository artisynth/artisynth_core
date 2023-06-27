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
    * Returns {@code true} if this component currently holds state.  This
    * allows the presence of state to depend on the component's configuration.
    *
    * @return {@code true} if this component holds state
    */
   public boolean hasState();

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
    * Returns a version number for this component's state. When the version
    * number changes, any previously saved state should be considered no longer
    * compatible. The default implementation of this method always returns 0,
    * indicating that state compatibility never changes.
    */
   default public int getStateVersion() {
      return 0;
   }

   /**
    * Returns {@code true} if the state of this component must be
    * updated each time step using {@link #advanceState}.
    *
    * @return {@code true} if {@code advanceState} must be called
    */
   default public boolean requiresAdvance() {
      return false;
   }

   /** 
    * If {@link #requiresAdvance} and {@link #hasState} both return {@code
    * true}, then this method is called each time step by the system integrator
    * to update this component's state. For single step integrators, the method
    * is called once at the start of the integration, with {@code t0} and
    * {@code t1} set to the step's start and end times. For multi-step
    * integrators (such as Runge Kutta), the method is called for each
    * sub-step, with {@code t0} and {@code t1} set to the sub-step's time
    * bounds.  Multi-step integrators will also use {@link #getState} and
    * {@link #setState} to save and restore state as required.
    * 
    * @param t0 beginning time associated with the time step or sub-step
    * @param t1 end time associated with the time step or sub-step
    */
   default public void advanceState (double t0, double t1) {
   }

   /**
    * Returns the number of <i>auxiliary</i> variables associated with this
    * component, or 0 if there are no auxiliary variables.
    *
    * <p>If {@link #hasState} returns {@code true} and the component has
    * auxiliary variables, the variables are updated each time step by the
    * system integrator using explcit integration of the form
    * <pre>
    * w += h dwdt
    * </pre>
    * 
    * where {@code h} is a time increment, {@code w} is a vector of variables
    * accessed using {@link #getAuxVarState} and {@link #setAuxVarState}, and
    * {@code dwdt} is a time derivative obtained using {@link
    * #getAuxVarDerivative}. For single step integrators, this will be done
    * once at the start of the integration, with {@code h} set to the time step
    * size. For multi-step integrators (such as Runge Kutta), this will be done
    * at the start of each sub-step, with {@code h} set to the sub-step size.
    *
    * <p> If present, auxiliary variables are also assumed to be contained
    * within the state accessed using {@link #getState} and {@link #setState}.
    *
    * @return number of auxiliary variables.
    */
   default public int numAuxVars() {
      return 0;
   }

   /**
    * If this component has auxiliary variables, returns their values in {@code
    * buf}, starting at the location {@code idx}.  See {@link #numAuxVars} for
    * a description of auxiliary variables.  The method returns {@code idx +
    * num}, where {@code num} is the number of variables.
    *
    * @param buf returns the variable values
    * @param idx starting point within {@code buf}
    * @return {@code idx} plus the number of variables
    */
   default public int getAuxVarState (double[] buf, int idx) {
      return idx;
   }

   /**
    * If this component has auxiliary variables, sets them from the values in
    * {@code buf}, starting at the location {@code idx}.  See {@link
    * #numAuxVars} for a description of auxiliary variables.  The method
    * returns {@code idx + num}, where {@code num} is the number of variables.
    *
    * @param buf contains the new variable values
    * @param idx starting point within {@code buf}
    * @return {@code idx} plus the number of variables
    */
   default public int setAuxVarState (double[] buf, int idx) {
      return idx;
   }

   /**
    * If this component has auxiliary variables, returns the current values of
    * their derivatives in {@code buf}, starting at the location {@code idx}.
    * See {@link #numAuxVars} for a description of auxiliary variables.  The
    * method returns {@code idx + num}, where {@code num} is the number of
    * variables.
    *
    * @param buf returns the variable derivative values
    * @param idx starting point within {@code buf}
    * @return {@code idx} plus the number of variables
    */
   default public int getAuxVarDerivative (double[] buf, int idx) {
      return idx;
   }
}




