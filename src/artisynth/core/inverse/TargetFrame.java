/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.TransformGeometryContext;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.RigidTransform3d;

public class TargetFrame extends RigidBody {

   public TargetFrame () {
      super.setDynamic (false);
   }

   public TargetFrame (RigidTransform3d X) {
      this();
      setPose(X);
   }
   
   /**
    * Cannot set target frame as dynamic
    */
   public void setDynamic (boolean dynamic) {
      // prevent setting as dynamic
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      // XXX should actually transform target pos/rot; here assuming target==actual
      if (myTarget != null) {
         myTarget.setTargetPos (getPosition ());
         myTarget.setTargetRot (getOrientation ());
      }
   }   

}
