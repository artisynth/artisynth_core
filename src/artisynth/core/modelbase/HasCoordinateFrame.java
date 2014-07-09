/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.RigidTransform3d;

/**
 * Indicates an object that has a "pose" describable by a RigidTransform3d.
 */
public interface HasCoordinateFrame {

   public void getPose (RigidTransform3d X);

}
