/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.*;
import maspack.geometry.GeometryTransformer;

/**
 * Reports changes in component geometry.
 */
public class GeometryChangeEvent extends ComponentChangeEvent {
   private GeometryTransformer myTransformer;

   public GeometryChangeEvent (ModelComponent comp, GeometryTransformer gtr) {
      super (Code.GEOMETRY_CHANGED, comp);
      myTransformer = gtr;
   }

   public GeometryTransformer getTransformer() {
      return myTransformer;
   }
}
