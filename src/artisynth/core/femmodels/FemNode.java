/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.mechmodels.Particle;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.util.*;
import java.io.*;

public abstract class FemNode extends Particle {
   public FemNode() {
      super();
      myMass = 0;
      //myEffectiveMass = 0;
   }

   public void addMass (double m) {
      myMass += m;
      //myEffectiveMass += m;
   }

   public FemNode copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemNode node = (FemNode)super.copy (flags, copyMap);
      node.myMass = 0;
      return node;
   }


}
