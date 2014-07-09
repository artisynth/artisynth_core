/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.RigidTransform3d;

/**
 * Defines a worker class that tests two bounding volume nodes for intersection.
 */
public interface BVNodeTester {
   
   /**
    * Returns true if node1 and node2 are disjoint. The nodes may be described
    * with respect to different coordinate frames.
    *
    * @param node1 first bounding volume node
    * @param node2 second bounding volume node
    * @param X21 transform from the coordinate frame of node2 to the
    * coordinate frame of node1. If the coordinate frames are the same,
    * this should be set to {@link maspack.matrix.RigidTransform3d#IDENTITY
    * RigidTransform3d.IDENTITY}.
    * @return true if the nodes are disjoint.
    */
   public boolean isDisjoint (BVNode node1, BVNode node2, RigidTransform3d X21);
}
