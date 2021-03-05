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

/**
 * Implements a constraint acting on one or more dynamic compnents. It provides
 * the constraint matrices and other information that is used to assemble the
 * KKT system or mixed linear complementarity problem (MLCP) used for
 * constrained dynamic solves.
 *
 * <p>The bilateral constraints are associated with a <i>velocity constraint
 * matrix</i> {@code Gc}, which is a sparse block matrix for which
 * <pre>
 * Gc vel = 0
 * </pre>
 * where {@code vel} is the composite velocity vector for all dynamic
 * components. The transpose of {@code Gc} is known as the bilateral
 * <i>force constraint matrix</i>.
 *
 * <p>Likewise, the unilateral constraints are associated with a
 * velocity constraint matrix {@code Nc}, for which
 * <pre>
 * Nc vel &gt;= 0,
 * </pre>
 * and the transpose of {@code Nc} is the unilateral force constraint
 * matrix.
 */
public interface Constrainer {
   
   /**
    * Returns the sizes of each block column in the bilateral force constraint
    * matrix. The size for each block should be appended to the vector {@code
    * sizes}.
    *
    * @param sizes vector to which the block column sizes are appended
    */
   public void getBilateralSizes (VectorNi sizes);

   /**
    * Appends the current bilateral force constraint matrix {@code Gc^T} to the
    * matrix {@code GT}, by appending block columns to it.  If the argument
    * {@code dg} is non-{@code null}, it should be used to return the
    * velocity constraint time derivative, defined by
    * <pre>
    * \dot Gc vel
    * </pre>
    * starting at the location {@code numb}. In all cases, the method must
    * return an updated value of {@code numb}, incremented by the total row
    * size of {@code Gc}.
    * 
    * @param GT matrix to which the bilateral force contraint matrix
    * is appended.
    * @param dg if non-{@code null}, returns the velocity constraint time
    * derivative
    * @param numb starting index for time derivative in {@code dg}
    * @return updated value of {@code numb}
    */
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb);

   /**
    * Returns constraint information for each row of the bilateral
    * constraint system
    * <pre>
    * Gc vel = 0.
    * </pre>
    * This information is placed in pre-allocated {@link ConstraintInfo}
    * structures in {@code ginfo}, starting at {@code idx}. The method must
    * return an updated value of {@code idx}, incremented by the number of
    * rows of {@code Gc}.
    *
    * <p>
    * The constraint information to be set in {@code ConstraintInfo} includes:
    * <pre>
    *   dist        // distance to the constraint surface.
    *   compliance  // if &gt; 0, gives constraint compliance value
    *   damping     // damping; only used if compliance &gt; 0
    *   force       // used for computing non-linear compliance
    * </pre>
    *
    * @param ginfo returns the constraint information
    * @param idx starting location in {@code ginfo} for returning constraint
    * info
    * @return updated value of {@code idx}
    */
   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx);

   /**
    * Sets the bilateral forces that were computed to enforce this
    * constraint. These are the Lagrange multipliers for the columns of the
    * transposed bilateral constraint matrix. The forces are
    * supplied in {@code lam}, starting at the index {@code idx}, and should be
    * scaled by {@code s}. (In practice, {@code s} is used to convert from
    * impulses to forces.) The method must return an updated value of {@code
    * idx}, incremented by the number of forces associated with this constraint.
    * 
    * @param lam supplies the force impulses, which should be scaled by {@code
    * s}
    * @param s scaling factor for the force values
    * @param idx starting index of forces in {@code lam}
    * @return updated value of {@code idx}
    */
   public int setBilateralForces (VectorNd lam, double s, int idx);   

   /**
    * Returns the bilateral forces that were most recently set for this
    * constrainer using {@link #setBilateralForces}. The forces are returned in
    * {@code lam}, starting at the index {@code idx}.  The method must return
    * an updated value of {@code idx}, incremented by the number of forces
    * associated with this constraint.
    *
    * @param lam returns the forces
    * @param idx starting index for forces in {@code lam}
    * @return updated value of {@code idx}
    */
   public int getBilateralForces (VectorNd lam, int idx);
   
   /**
    * Zeros all bilateral and unilateral constraint forces in this constraint.
    */
   public void zeroForces();

   /**
    * Returns the sizes of each block column in the unilateral force constraint
    * matrix. The size for each block should be appended to the vector {@code
    * sizes}.
    *
    * @param sizes vector to which the block column sizes are appended
    */
   public void getUnilateralSizes (VectorNi sizes);

   /**
    * Appends the current unilateral force constraint matrix {@code Nc^T} to the
    * matrix {@code NT}, by appending block columns to it.  If the argument
    * {@code dn} is non-{@code null}, it should be used to return the
    * velocity constraint time derivative, defined by
    * <pre>
    * \dot Nc vel
    * </pre>
    * starting at the location {@code numu}. In all cases, the method must
    * return an updated value of {@code numu}, incremented by the total row
    * size of {@code Nc}.
    * 
    * @param NT matrix to which the unilateral force contraint matrix
    * is appended.
    * @param dn if non-{@code null}, returns the velocity constraint time
    * derivative
    * @param numu starting index for time derivative in {@code dn}
    * @return updated value of {@code numu}
    */
   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu);

   /**
    * Returns constraint information for each row of the unilateral
    * constraint system
    * <pre>
    * Nc vel &gt; 0.
    * </pre>
    * This information is placed in pre-allocated {@link ConstraintInfo}
    * structures in {@code ninfo}, starting at {@code idx}. The method must
    * return an updated value of {@code idx}, incremented by the number of
    * rows of {@code Nc}.
    *
    * <p>
    * The constraint information to be set in {@code ConstraintInfo} includes:
    * <pre>
    *   dist        // distance to the constraint surface.
    *   compliance  // if &gt; 0, gives constraint compliance value
    *   damping     // damping; only used if compliance &gt; 0
    *   force       // used for computing non-linear compliance
    * </pre>
    *
    * @param ninfo returns the constraint information
    * @param idx starting location in {@code ninfo} for returning constraint
    * info
    * @return updated value of {@code idx}
    */
   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx);

   /**
    * Sets the unilateral forces that were computed to enforce this
    * constraint. These are the Lagrange multipliers for the columns of the
    * transposed unilateral constraint matrix. The forces are
    * supplied in {@code the}, starting at the index {@code idx}, and should be
    * scaled by {@code s}. (In practice, {@code s} is used to convert from
    * impulses to forces.) The method must return an updated value of {@code
    * idx}, incremented by the number of forces associated with this constraint.
    * 
    * @param the supplies the force impulses, which should be scaled by {@code
    * s}
    * @param s scaling factor for the force values
    * @param idx starting index of forces in {@code the}
    * @return updated value of {@code idx}
    */
   public int setUnilateralForces (VectorNd the, double s, int idx);

   /**
    * Returns the unilateral forces that were most recently set for this
    * constrainer using {@link #setUnilateralForces}. The forces are returned in
    * {@code the}, starting at the index {@code idx}.  The method must return
    * an updated value of {@code idx}, incremented by the number of forces
    * associated with this constraint.
    *
    * @param the returns the forces
    * @param idx starting index for forces in {@code the}
    * @return updated value of {@code idx}
    */
   public int getUnilateralForces (VectorNd the, int idx);

   /**
    * Returns the maximum number of friction constraint sets that can be
    * expected for this constraint. There should be one constraint set for each
    * bilateral or unilateral constraint that may be associated with friction.
    * This method is used for presizing the {@code finfo} argument that is
    * passed to {@link #addFrictionConstraints}.
    * 
    * @return maximum number of friction constraint sets
    */
   public int maxFrictionConstraintSets();
   
   /**
    * Appends the friction force constraint matrix {@code Dc^T} to the matrix
    * {@code DT}, by appending block columns to it. Each block column in {@code
    * Dc^T} corresponds to a <i>friction constraint set</i>, for which
    * information should be supplied in the pre-allocated {@link FrictionInfo}
    * structures in {@code finfo}, starting at {@code idx}.  The method must
    * return an updated value of {@code idx}, incremented by the number of
    * friction constraint sets.

    * @param DT matrix to which the friction force contraint matrix
    * is appended.
    * @param finfo returns friction constraint information for each
    * block column in {@code Dc^T}
    * @param idx starting index for friction information in {@code finfo}
    * @return updated value of {@code idx}
    */
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int idx);

   /**
    * Updates the current set of constraints, and returns the maximum
    * penetration {@code >} 0 associated with all of them. If no constraints are
    * presently active, returns -1.
    */
   public double updateConstraints (double t, int flags);
   
   /**
    * Collected all the dynamic components constrained by this constrainer.
    * Not currently used.
    * 
    * @param list list to which constrained components should be appended
    */
   public void getConstrainedComponents (List<DynamicComponent> list);
   
}
