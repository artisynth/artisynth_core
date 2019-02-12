/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import maspack.geometry.GeometryTransformer;
/**
 * Allows a rigid body to have multiple geometries, some used for
 * computing mass/inertia, some for display only, some for collisions
 * 
 * @deprecated the functionality offered is now provided by {@link RigidBody}.
 * @author Antonio
 */
public class RigidCompositeBody extends RigidBody {

   public RigidCompositeBody() {
      this (null);
   }

   public RigidCompositeBody(String name) {
      super(name);
      initializeChildComponents();
   }

   @Override
   public void transformGeometry(
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      // remove dragger flag for children
      flags = flags & ~TransformableGeometry.TG_DRAGGER;
      super.transformGeometry(gtr, context, flags);
   }
}
