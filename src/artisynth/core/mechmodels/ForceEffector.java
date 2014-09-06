/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import maspack.matrix.*;

/**
 * An object that exerts forces within a mechanical model.
 */
public interface ForceEffector {
   /**
    * Adds forces to the components affected by this force effector at a
    * particular time. Component forces should be added and not set, since other
    * forces may be added to the same components by other force effectors.
    * 
    * @param t
    * time (seconds)
    */
   public void applyForces (double t);

   /**
    * Adds any needed blocks to a solve matrix in order to accomodate the
    * Jacobian terms associated with this force effector. In general, blocks
    * will be need to be added at locations given by the block indices (<i>bi</i>,
    * <i>bj</i>), where <i>bi</i> and <i>bj</i> correspond to the solve
    * indices (as returned by {@link
    * artisynth.core.mechmodels.DynamicComponent#getSolveIndex
    * getSolveIndex}) for all dynamic or attached components affected by this
    * force effector.
    * 
    * @param M
    * solve matrix to which blocks should be added
    */
   public void addSolveBlocks (SparseNumberedBlockMatrix M);

   /**
    * Scales the components of the position Jacobian associated with this force
    * effector and adds it to the supplied solve matrix M.
    * 
    * <p>
    * M is guaranteed to be the same matrix supplied in the most recent call to
    * {@link #addSolveBlocks addSolveBlocks}, and so implementations may choose
    * to cache the relevant matrix blocks from that call, instead of retrieving
    * them directly from M.
    * 
    * @param M
    * solve matrix to which scaled position Jacobian is to be added
    * @param s
    * scaling factor for position Jacobian
    */
   public void addPosJacobian (SparseNumberedBlockMatrix M, double s);

   /**
    * Scales the components of the velocity Jacobian associated with this force
    * effector and adds it to the supplied solve matrix M.
    * 
    * <p>
    * M is guaranteed to be the same matrix supplied in the most recent call to
    * {@link #addSolveBlocks addSolveBlocks}, and so implementations may choose
    * to cache the relevant matrix blocks from that call, instead of retrieving
    * them directly from M.
    * 
    * @param M
    * solve matrix to which scaled velocity Jacobian is to be added
    * @param s
    * scaling factor for velocity Jacobian
    */
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s);

   /**
    * Returns a code indicating the matrix type that results when the Jacobian
    * terms of this force effector are added to the solve matrix. This should be
    * a logical or-ing of either {@link maspack.matrix.Matrix#SYMMETRIC
    * Matrix.SYMMETRIC} or {@link maspack.matrix.Matrix#POSITIVE_DEFINITE
    * Matrix.POSITIVE_DEFINITE}. The former should be set if adding the Jacobian
    * terms preserves symmetry, and the latter should be set if positive
    * definiteness if preserved. Both should be set if there is no Jacobian for
    * this effector (i.e., the Jacobian methods are not implemented). Matrix
    * types from all the force effectors are logically and-ed together to
    * determine the type for the entire solve matrix.
    * 
    * @return solve matrix type resulting from adding Jacobian terms
    */
   public int getJacobianType();
}
