/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;

/**
 * Describes a DynamicComponent that can response to collisions. One or
 * more of these associated with a Collidable object provide the underlying 
 * collision response.
 * 
 * @author lloyd
 */
public interface CollidableDynamicComponent extends DynamicComponent {

   /**
    * Computes the values for the block matrix which defines this component's
    * contribution to a contact constraint matrix. The matrix is 1 x n, where
    * <code>n</code> is the component's velocity state size, and the contact is
    * defined by a direction <code>dir</code> and a point <code>cpnt</code>.
    * The computed values should be scaled by a weighting factor
    * <code>w</code>.
    * 
    * @param buf returns the n values for the block matrix
    * @param w weighting factor by which the values should be scaled
    * @param dir contact direction (world coordinates)
    * @param cpnt contact point
    */
   public void setContactConstraint (
      double[] buf, double w, Vector3d dir, ContactPoint cpnt);

   /**
    * Computes the velocity imparted to a contact point by this component's
    * current velocity, multiples it by a weighting factor <code>w</code>,
    * and add it to <code>vel</code>.
    * 
    * @param vel accumulates contact point velocity (world coordinates)
    * @param w weighting factor
    * @param cpnt contact point
    */
   public void addToPointVelocity (
      Vector3d vel, double w, ContactPoint cpnt);

   //public boolean requiresContactVertexInfo();

}
