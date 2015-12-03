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
import artisynth.core.util.ScalableUnits;

public abstract class ParticleConstraint extends RenderableComponentBase
   implements ScalableUnits, TransformableGeometry {

   public abstract void getBilateralSizes (VectorNi sizes);

   public abstract int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, double t);

   public abstract int getBilateralInfo (ConstraintInfo[] ginfo, int idx);

   // public abstract int getBilateralOffsets (
   //    VectorNd Rg, VectorNd bg, int idx, int mode);

   public abstract int setBilateralImpulses (VectorNd lam, int idx);
   
   public abstract void zeroImpulses();

   public abstract void getUnilateralSizes (VectorNi sizes);

   public abstract int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu, double t);

   public abstract int getUnilateralInfo (ConstraintInfo[] ninfo, int idx);

   // public abstract int getUnilateralOffsets (
   //    VectorNd Rn, VectorNd bn, int idx, int mode);

   public abstract int setUnilateralImpulses (VectorNd the, int idx);

   public abstract void projectPosConstraints (double t);

//   public abstract void projectVelConstraints (double t0, double t1);

   public void projectFrictionConstraints () {
   }

   public abstract int getStateSize();

   public abstract int getState (VectorNd x, int idx);

   public abstract int setState (VectorNd x, int idx);

   // need to override:
   // getReferences (List<ModelComponent> refs)
}     