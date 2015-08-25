/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;

public abstract class ConstrainerBase
   extends RenderableComponentBase implements Constrainer, HasAuxState {

   public abstract void getBilateralSizes (VectorNi sizes);

   public abstract int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb);

   public abstract int getBilateralInfo (ConstraintInfo[] ginfo, int idx);

   public abstract int setBilateralImpulses (VectorNd lam, double h, int idx);   

   public abstract int getBilateralImpulses (VectorNd lam, int idx);
   
   public abstract void zeroImpulses();

   public void getUnilateralSizes (VectorNi sizes) {
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      return idx;
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      return idx;
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return 0;
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int idx) {
      return idx;
   }

   public abstract double updateConstraints (double t, int flags);

   public void advanceAuxState (double t0, double t1) {
   }

   /** 
    * {@inheritDoc}
    */
   public void skipAuxState (DataBuffer data) {
   }

   public void getAuxState (DataBuffer data) {
   }

   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {
   }
   
   public void setAuxState (DataBuffer data) {
   }
 
   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }

}
