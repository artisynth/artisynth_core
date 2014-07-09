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

/**
 * Object containing state information for an Artisynth model or component.
 */
public interface ComponentState extends Serializable {
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
   public boolean equals (ComponentState state);

   /**
    * Create a duplicate of this state which can be used for storing this
    * state's values.
    * 
    * @return duplicate of this state
    */
   public ComponentState duplicate();
}
