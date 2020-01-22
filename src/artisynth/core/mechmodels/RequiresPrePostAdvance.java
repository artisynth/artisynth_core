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
 * processing before and/or after its containing
 * model is advanced.
 */
public interface RequiresPrePostAdvance {

   /** 
    * Called from within the model's {{@link Model#advance(double,double,int)
    * advance()} method, <i>before</i> position and velocity states are
    * advanced.
    *
    * @param t0 current time (seconds)
    * @param t1 new time to be advanced to (seconds)
    * @param flags flags passed to the model's
    * {@link Model#preadvance(double,double,int)
    * preadvance()} method (reserved for future use).
    */
   public void preadvance (double t0, double t1, int flags);

   /** 
    * Called from within the model's {@link Model#advance(double,double,int)
    * advance()} method, <i>after</i> all position and velocity state has been
    * advanced.
    *
    * @param t0 current time (seconds)
    * @param t1 new time to be advanced to (seconds)
    * @param flags flags passed to the model's
    * {@link Model#advance(double,double,int)
    * advance()} method (reserved for future use).
    */
   public void postadvance (double t0, double t1, int flags);
}
