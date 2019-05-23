/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.TransformGeometryContext;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.Point3d;

public class TargetPoint extends Point {

   public TargetPoint () {
      super.setDynamic (false);
   }

   public TargetPoint (Point3d pnt) {
      super (pnt);
      super.setDynamic (false);
      myTarget.setTargetPos (pnt);
   }
   
   @Override
   protected void setDynamic (boolean dynamic) {
      // prevent setting as dynamic
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
