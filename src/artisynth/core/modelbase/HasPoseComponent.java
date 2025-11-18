/**
 * Copyright (c) 2025, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.RigidTransform3d;

/**
 * A model component that has a "pose" describable by a RigidTransform3d.
 */
public interface HasPoseComponent extends HasCoordinateFrame, ModelComponent {

   public RigidTransform3d getPose();

}
