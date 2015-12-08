/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.geometry.GeometryTransformer;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;

public class TargetPoint extends Point {

   public TargetPoint () {
   }

   public TargetPoint (Point3d pnt) {
      super (pnt);
      myTarget.setTargetPos (pnt);
   }

   @Override
   public boolean isDynamic () {
      return false;
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      // XXX should actually transform target pos; here assuming target==actual
      if (myTarget != null) {
         myTarget.setTargetPos (getPosition());
      }
   }


}
