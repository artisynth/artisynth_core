/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;

import java.io.*;

public abstract class Spring extends ModelComponentBase implements
ForceComponent {
   public Spring (String name) {
      super (name);
   }

   public abstract void applyForces (double t);
}
