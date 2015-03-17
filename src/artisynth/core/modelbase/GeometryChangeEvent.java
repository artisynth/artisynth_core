/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.*;

/**
 * Reports changes in component geometry.
 */
public class GeometryChangeEvent extends ComponentChangeEvent {
   private AffineTransform3dBase myX;

   public GeometryChangeEvent (ModelComponent comp, AffineTransform3dBase X) {
      super (Code.GEOMETRY_CHANGED, comp);
      myX = X.clone();
   }

   public AffineTransform3dBase getTransform() {
      return myX;
   }
}
