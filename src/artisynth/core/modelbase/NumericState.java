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
import java.util.*;
import maspack.matrix.*;
import maspack.util.DataBuffer;

public class NumericState extends DataBuffer implements ComponentState {
   private static final long serialVersionUID = 1L;

   public NumericState() {
      this (0, 0, 0);
   }

   public NumericState (int dcap, int zcap) {
      this (dcap, zcap, 0);
   }

   public NumericState (int dcap, int zcap, int ocap) {
      super (dcap, zcap, ocap);
   }

   public void writeBinary (DataOutputStream dos) throws IOException {
      int zsize = zsize();
      int[] zbuf = zbuffer();
      dos.writeInt (zsize);
      for (int i=0; i<zsize; i++) {
         dos.writeInt (zbuf[i]);
      }
      int dsize = dsize();
      double[] dbuf = dbuffer();
      dos.writeInt (dsize);
      for (int i=0; i<dsize; i++) {
         dos.writeDouble (dbuf[i]);
      }  
   }

   public void readBinary (DataInputStream dis) throws IOException {
      int zsize = dis.readInt();
      zEnsureCapacity (zsize()+zsize);
      for (int i=0; i<zsize; i++) {
         zput (dis.readInt());
      }
      int dsize = dis.readInt();
      dEnsureCapacity (dsize()+dsize);
      for (int i=0; i<dsize; i++) {
         dput (dis.readDouble());
      }
   }

   public void set (NumericState state) {
      super.set (state);
   }

   public void set (ComponentState state) {
      try {
         set ((NumericState)state);
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException (
            "state to copy is not a VectorState");
      }
   }

   private static boolean debugEquals = true;

   /** 
    * {@inheritDoc}
    */
   public boolean equals (ComponentState state) {
      if (state instanceof NumericState) {
         NumericState otherState = (NumericState)state;
         return super.equals (otherState, debugEquals);
      }
      else {
         return false;
      }
   }

   public ComponentState duplicate() {
      NumericState state = new NumericState ();
      state.set (this);
      return state;
   }
}
