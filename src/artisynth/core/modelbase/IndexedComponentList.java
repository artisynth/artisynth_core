/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Describes anything that allows us to get a component by index.
 */
public interface IndexedComponentList {
 
   public int numComponents();

   public ModelComponent get (int idx);

}
