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

   public int setBilateralForces (VectorNd lam, double s, int idx);   

   public int getBilateralForces (VectorNd lam, int idx);
   
   public void zeroForces();

   public void getUnilateralSizes (VectorNi sizes);

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu);

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx);

   public int setUnilateralForces (VectorNd the, double s, int idx);

   public int getUnilateralForces (VectorNd the, int idx);

   public int maxFrictionConstraintSets();
   
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int idx);

   /**
    * Updates the current set of constraints, and returns the maximum
    * penetration {@code >} 0 associated with all of them. If no constraints are
    * presently active, returns -1.
    */
   public double updateConstraints (double t, int flags);
   
   // Currently not used. Could be useful at some point in the future though
   public void getConstrainedComponents (List<DynamicComponent> list);
   
}
