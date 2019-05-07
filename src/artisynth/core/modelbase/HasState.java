/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Interface for a model component that can contain state and can save and
 * restore this state to a ComponentState object.
 */
public interface HasState {

   /**
    * Set the state of this component.
    * 
    * @param state
    * state to be copied
    * @throws IllegalArgumentException
    * if the supplied state object is incompatible with this component
    */
   public void setState (ComponentState state);

   /**
    * Get the current state of this component.
    * 
    * @param state
    * receives the state information
    * @throws IllegalArgumentException
    * if the supplied state object is incompatible with this component
    */
   public void getState (ComponentState state);

   /**
    * Gets an initial state for this component and returns the value in
    * <code>state</code>. If <code>prevstate</code> is non-null, then it is
    * assumed to contain a previous initial state value returned by this
    * method, and <code>state</code> should be set to be as consistent with
    * this previous state as possible. For example, suppose that this component
    * currently contains subcomponents A, B, and C, while the
    * <code>prevstate</code> contains the state from a previous time when it
    * had components B, C, and D. Then <code>state</code> should contain
    * substate values for B and C that are taken from
    * <code>prevstate</code>. To facilitate this, the information returned in
    * <code>state</code> should contain additional information such as the
    * identities of all the (current) sub-components.
    *
    * @param state
    * receives the state information
    * @param prevstate
    * previous state information; may be <code>null</code>.
    * @throws IllegalArgumentException
    * if the supplied state object is incompatible with this component
    */
   public void getInitialState (
      ComponentState state, ComponentState prevstate);
   

   /**
    * Factory routine to create a state object for this component, which can
    * then be used as an argument for {@link #setState} and {@link
    * #getState}. The state object does not have to be set to the component's
    * current state.  If the component does not have any state information,
    * this method should return an instance of
    * {@link artisynth.core.modelbase.EmptyState EmptyState}.
    * 
    * @param prevState If non-null, supplies a previous state that
    * was created by this component and which can be used to provide
    * pre-sizing hints.
    * @return new object for storing this component's state
    */
   public ComponentState createState (ComponentState prevState);

}
