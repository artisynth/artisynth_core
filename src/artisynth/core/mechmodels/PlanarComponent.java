/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import artisynth.core.modelbase.ModelComponent;

/**
 * Indicates a component that is constrained to a plane
 */
public interface PlanarComponent extends ModelComponent {
   /**
    * Returns the transform from plane coordinates to world coordinates. The
    * plane is assumed to lie in the x-y plane of the planar coordinate system.
    * 
    * @return plane to world transform
    */
   public RigidTransform3d getPlaneToWorld();
}
