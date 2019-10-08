/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;

/**
 * Indicates a model component that contains one or more HasNumericState components
 * internally, as either child or internal (hidden) components.
 */
public interface HasNumericStateComponents {

   /**
    * Returns the HasNumericState components contained by this component. This will
    * be called by MechModelBase when creating a complete list of all the
    * HasNumericState components in the model.
    * 
    * @param list list to which HasNumericState components should be appended
    */
   public void getNumericStateComponents (List<HasNumericState> list);
}
