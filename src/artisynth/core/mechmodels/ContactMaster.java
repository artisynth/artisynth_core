/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.HashSet;

import maspack.matrix.*;

/**
 * Adds blocks to constraint matrices to implement the constraint conditions
 * requires for contact, including both normal and frictional contact
 * conditions. Implementations are assumed to have information about the
 * contact point, weighting factor, and the underlying dynamic components.
 */
public interface ContactMaster {
 
   /**
    * Adds blocks to the constraint matrix GT to implement a 1D constraint in a
    * specified direction. These are used to implement both normal constraints
    * and 1D friction constraints. The block column is specified by {@code bj},
    * while the block row(s) are determined internally by the solve indices of
    * the underlying dynamic components.
    *
    * @param DT constraint matrix
    * @param bj block column index
    * @param scale TODO
    * @param cpnt contact point
    * @param dir friction direction
    */
  public void add1DConstraintBlocks (
      SparseBlockMatrix DT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir);

   /**
    * Adds blocks to the constraint matrix GT to implement a 2D constraint in
    * two specified directions. These are used to implement 2D friction
    * constraints in a plane perpendicular to the normal. The block column is
    * specified by {@code bj}, while the block row(s) are determined internally
    * by the solve indices of the underlying dynamic components.
    *
    * @param DT constraint matrix
    * @param bj block column index
    * @param scale TODO
    * @param cpnt contact point
    * @param dir0 first friction direction
    * @param dir1 second friction direction
    */
   public void add2DConstraintBlocks (
      SparseBlockMatrix DT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir0, Vector3d dir1);
   
   /**
    * Accumulate the velocity at the contact due to the current
    * velocity of the underlying dynamic components.
    *
    * @param vel accumulates velocity
    * @param scale TODO
    * @param cpnt contact point
    */
   public void addRelativeVelocity (
      Vector3d vel, double scale, ContactPoint cpnt);
   
   /**
    * Queries whether at least one of the underlying dynamic components is
    * controllable.
    * 
    * @return {@code true} if at least one dynamic component is
    * controllable
    */
   public boolean isControllable();

   /**
    * Collects all the underlying dynamic ``master'' components. This method
    * returns the number of components that were actually added to {@code
    * masters}, i.e., the number of components that were not already present
    * there.
    *
    * @param masters set to which the dynamic components should be added
    * @param activeOnly restrict collected components to those which are active
    * @return number of components added to masters
    */
   public int collectMasterComponents (
      HashSet<DynamicComponent> masters, boolean activeOnly);
}
