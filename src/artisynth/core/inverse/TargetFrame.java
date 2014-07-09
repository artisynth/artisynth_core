/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.RigidTransform3d;
import artisynth.core.mechmodels.Frame;
import artisynth.core.util.TransformableGeometry;

public class TargetFrame extends Frame {

   public TargetFrame () {
   }

   public TargetFrame (RigidTransform3d X) {
      super (X);
   }
   
   @Override
   public boolean isDynamic () {
      return false;
   }

   @Override
   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      super.transformGeometry (X, topObject, flags);
      // XXX should actually transform target pos/rot; here assuming target==actual
      if (myTarget != null) {
         myTarget.setTargetPos (getPosition ());
         myTarget.setTargetRot (getOrientation ());
      }
   }

   @Override
   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }
   

}
