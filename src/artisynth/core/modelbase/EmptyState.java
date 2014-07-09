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

public class EmptyState implements ComponentState {
   private static final long serialVersionUID = 1L;

   /**
    * Writes this state (in binary format) to a data output stream. For the
    * EmptyState, nothing is written.
    * 
    * @param dos
    * output stream to write model to
    * @throws IOException
    * if an I/O error occurred
    */
   public void writeBinary (DataOutputStream dos) throws IOException {
   }

   /**
    * Read this state (in binary format) from a data input stream. For the
    * EmptyState, nothing is read.
    * 
    * @param dis
    * input stream to read model from
    * @throws IOException
    * if an I/O error occurred or if the input is incompatible with the current
    * state configuration.
    */
   public void readBinary (DataInputStream dis) throws IOException {
   }

   /**
    * Sets this state by copying the value from an existing state object.
    * 
    * @param state
    * state object to copy
    * @throws IllegalArgumentException
    * if the state objects are incompatible
    */
   public void set (ComponentState state) {
      if (state != null && !(state instanceof EmptyState)) {
         throw new IllegalArgumentException (
            "new state is not null or the EmptyState");
      }
   }

   public boolean equals (ComponentState state) {
      if (state instanceof EmptyState) {
         return true;
      }
      else {
         return false;
      }
   }            

   /** 
    * {@inheritDoc}
    */
   public ComponentState duplicate() {
      return new EmptyState();
   }
}
