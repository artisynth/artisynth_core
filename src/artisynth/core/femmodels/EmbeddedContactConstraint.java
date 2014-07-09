/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.geometry.Vertex3d;
import artisynth.core.mechmodels.DeformableContactConstraintGeneric;

public class EmbeddedContactConstraint extends DeformableContactConstraintGeneric {

   public EmbeddedContactConstraint() {
      super();
   }
   
   public EmbeddedContactConstraint(Vertex3d... vtxs) {
      super(vtxs);
   }
   
}
