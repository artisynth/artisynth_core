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

public interface Constrainer {

   public void getBilateralSizes (VectorNi sizes);

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb);

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx);

   public int setBilateralImpulses (VectorNd lam, double h, int idx);   

   public int getBilateralImpulses (VectorNd lam, int idx);
   
   public void zeroImpulses();

   public void getUnilateralSizes (VectorNi sizes);

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu);

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx);

   public int setUnilateralImpulses (VectorNd the, double h, int idx);

   public int getUnilateralImpulses (VectorNd the, int idx);

   public int maxFrictionConstraintSets();
   
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int idx);

   public double updateConstraints (double t, int flags);

}
