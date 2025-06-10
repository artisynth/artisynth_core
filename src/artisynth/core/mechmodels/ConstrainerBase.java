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
import maspack.spatialmotion.FrictionInfo;

public abstract class ConstrainerBase
   extends ModelComponentBase implements Constrainer, HasNumericState {

   public abstract void getBilateralSizes (VectorNi sizes);

   public abstract int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb);

   public abstract int getBilateralInfo (
      ConstraintInfo[] ginfo, int idx);

   public abstract int setBilateralForces (VectorNd lam, double s, int idx);   

   public abstract int getBilateralForces (VectorNd lam, int idx);
   
   public abstract void zeroForces();

   public void getUnilateralSizes (VectorNi sizes) {
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      return idx;
   }

   public int setUnilateralForces (VectorNd the, double s, int idx) {
      return idx;
   }

   public int getUnilateralForces (VectorNd the, int idx) {
      return idx;
   }

   public int setUnilateralState (VectorNi state, int idx) {
      return idx;
   }
   
   public int getUnilateralState (VectorNi state, int idx) {
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return 0;
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, ArrayList<FrictionInfo> finfo, 
      boolean prune, int idx) {
      return idx;
   }
   
   /**
    * {@inheritDoc}
    */
   public int setFrictionForces (VectorNd phi, double s, int idx) {
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int getFrictionForces (VectorNd phi, int idx) {
      return idx;
   }
   
   public int setFrictionState (VectorNi state, int idx) {
      return idx;
   }
   
   public int getFrictionState (VectorNi state, int idx) {
      return idx;
   }

   public abstract double updateConstraints (double t, int flags);
   
   public abstract void getConstrainedComponents (HashSet<DynamicComponent> comps);
   
   public void getState (DataBuffer data) {
      //data.zput (myBaseIdx);
   }
   
   public void setState (DataBuffer data) {
      //myBaseIdx = data.zget();
   }
 
   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }
}
