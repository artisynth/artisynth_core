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

/**
 * Subclass of Constrainer that applies constraints to rigid bodies and frames
 * and implements versions of {@code addBilateralConstraints} and {@code
 * addUnilateralConstraints} that permit block indices to be reassigned
 * via a solve index map.
 */
public interface BodyConstrainer extends Constrainer {
   
   /**
    * Implementation of {@link
    * #addBilateralConstraints(SparseBlockMatrix,VectorNd,int)} that allows the
    * block row positions in {@code GT} to be reassigned vis a solve matrix
    * map. This enables the creation of constrained system equations for model
    * component subsets.
    * 
    * @param GT matrix to which the bilateral force contraint matrix
    * is appended.
    * @param dg if non-{@code null}, returns the velocity constraint time
    * derivative
    * @param numb starting index for time derivative in {@code dg}
    * @param solveIndexMap if non-{@code null}, maps the solve indices of the
    * constrained components onto the block-row indices of {@code GT}.
    * @return updated value of {@code numb}
    */
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, int[] solveIndexMap);

   /**
    * Implementation of {@link
    * #addUnilateralConstraints(SparseBlockMatrix,VectorNd,int)} that allows the
    * block row positions in {@code NT} to be reassigned vis a solve matrix
    * map. This enables the creation of constrained system equations for model
    * component subsets.
    * 
    * @param NT matrix to which the bilateral force contraint matrix
    * is appended.
    * @param dg if non-{@code null}, returns the velocity constraint time
    * derivative
    * @param numb starting index for time derivative in {@code dg}
    * @param solveIndexMap if non-{@code null}, maps the solve indices of the
    * constrained components onto the block-row indices of {@code NT}.
    * @return updated value of {@code numb}
    */
   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu, int[] solveIndexMap);
}
