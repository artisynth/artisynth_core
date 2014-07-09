/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.util.*;

/**
 * Indicates a component, other than a MechSystemModel, that requires
 * initialization when its containing model is initialized.
 */
public interface RequiresInitialize {
   
   /** 
    * Called from within the model's {@link Model#initialize(double)
    * initialize()} method.
    *
    * @param t initialization time (seconds)
    */
   public void initialize (double t);
}