/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import maspack.util.Scannable;

/**
 * Object containing state information for an Artisynth model or component.
 */
public interface ComponentState extends Serializable, Scannable {
   
   /**
    * Writes this state (in binary format) to a data output stream.
    * 
    * @param dos
    * output stream to write model to
    * @throws IOException
    * if an I/O error occurred
    */
   public void writeBinary (DataOutputStream dos) throws IOException;

   /**
    * Read this state (in binary format) from a data input stream.
    * 
    * @param dis
    * input stream to read model from
    * @throws IOException
    * if an I/O error occurred or if the input is incompatible with the current
    * state configuration.
    */
   public void readBinary (DataInputStream dis) throws IOException;

   /**
    * Sets this state by copying the value from an existing state object.
    * 
    * @param state
    * state object to copy
    * @throws IllegalArgumentException
    * if the state objects are incompatible
    */
   public void set (ComponentState state);

   /**
    * Returns true if this state equals another component state.
    * 
    * @param state
    * state to compare to
    */
   default public boolean equals (ComponentState state) {
      return equals (state, /*msg=*/null);
   }
   
   /**
    * Returns true if this state equals another component state.
    * 
    * @param state
    * state to compare to
    * @param msg
    * If not {@code null}, can be used to append diagnostic
    * information if the states are not equal.
    */
   public boolean equals (ComponentState state, StringBuilder msg);

   /**
    * Queries whether or not this state is annotated. Annotation means that 
    * the state may contain additional information about its contents. 
    * In particular, this can be used to give details about how two states 
    * differ (via the {@code msg} argument to 
    * {@link #equals(ComponentState,StringBuilder) equals(state,msg)}.
    * 
    * @return {@code true} if this state is annotated.
    */
   default public boolean isAnnotated() {
      return false;
   }
   
   /**
    * Requests that this state be annotated. See {@link #isAnnotated}
    * for more details.
    * 
    * <p>Support for this method is optional. Applications should
    * check {@code isAnnotated()} to determine if the request has been
    * honored.
    * 
    * @param annotated if {@code true}, requests that this state be annotated.
    */
   default public void setAnnotated (boolean annotated) {
   }
   
   /**
    * Create a duplicate of this state which can be used for storing this
    * state's values.
    * 
    * @return duplicate of this state
    */
   public ComponentState duplicate();
}
