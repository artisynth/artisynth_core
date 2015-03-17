/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Interface for a class that needs to be notfied about property change events.
 */
public interface PropertyChangeListener {

   public void propertyChanged (PropertyChangeEvent e);

}
