/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

public interface Monitor extends ModelAgent {

   /**
    * Called at the end of a {@code RootModel}'s advance procedure
    *
    * @param t0 time at start of step
    * @param t1 time at end of step
    */
   public void apply (double t0, double t1);

}
